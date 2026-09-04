package com.example.wallrush.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wallrush.audio.SoundManager
import com.example.wallrush.data.local.AppDatabase
import com.example.wallrush.data.local.MatchRecord
import com.example.wallrush.data.local.PlayerProfile
import com.example.wallrush.data.local.SettingsPreferences
import com.example.wallrush.data.network.NetworkHelper
import com.example.wallrush.data.network.P2PGameConnection
import com.example.wallrush.data.network.P2PGameListener
import com.example.wallrush.data.repository.WallRushRepository
import com.example.wallrush.domain.engine.AIAction
import com.example.wallrush.domain.engine.AIEngine
import com.example.wallrush.domain.engine.GameEngine
import com.example.wallrush.domain.engine.ReplayEngine
import com.example.wallrush.domain.engine.RuleEngine
import com.example.wallrush.domain.model.*
import com.example.wallrush.ui.components.ActiveEmote
import com.example.wallrush.ui.localization.AppLanguage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ScreenState {
    HOME,
    MATCH,
    PUBLIC_ROOMS,
    PLAY_FRIEND,
    TUTORIAL,
    REPLAY,
    LEADERBOARD,
    PROFILE
}

enum class P2PConnectionStatus {
    IDLE,
    LISTENING_WIFI,
    CONNECTING_WIFI,
    LISTENING_BT,
    CONNECTING_BT,
    CONNECTED,
    DISCONNECTED
}

data class PublicRoomItem(
    val roomCode: String,
    val hostName: String,
    val hostAvatar: Int,
    val timeLimitSeconds: Int,
    val wallsCount: Int,
    val pingMs: Int,
    val status: String = "Waiting"
)

data class UiSettings(
    val language: AppLanguage = AppLanguage.ARABIC,
    val isSoundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val requireWallConfirm: Boolean = true,
    val showLegalMoves: Boolean = true,
    val theme: com.example.ui.theme.GameTheme = com.example.ui.theme.GameTheme.MainCyberNeon
)

class WallRushViewModel(application: Application) : AndroidViewModel(application), P2PGameListener {

    private val repository: WallRushRepository
    val soundManager: SoundManager
    private val settingsPreferences: SettingsPreferences

    private var p2pConnection: P2PGameConnection? = null

    init {
        val db = AppDatabase.getDatabase(application)
        repository = WallRushRepository(db)
        soundManager = SoundManager(application)
        settingsPreferences = SettingsPreferences(application)

        // Sync sound and vibration manager with saved preferences
        soundManager.isSoundEnabled = settingsPreferences.isSoundEnabled()
        soundManager.isVibrationEnabled = settingsPreferences.isVibrationEnabled()
    }

    private val _currentScreen = MutableStateFlow(ScreenState.HOME)
    val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

    // Persistent Settings
    private val _settings = MutableStateFlow(
        UiSettings(
            language = settingsPreferences.getLanguage(),
            isSoundEnabled = settingsPreferences.isSoundEnabled(),
            isVibrationEnabled = settingsPreferences.isVibrationEnabled(),
            requireWallConfirm = settingsPreferences.isWallConfirmRequired(),
            showLegalMoves = settingsPreferences.isShowLegalMoves(),
            theme = com.example.ui.theme.GameTheme.fromId(settingsPreferences.getThemeId())
        )
    )
    val settings: StateFlow<UiSettings> = _settings.asStateFlow()

    private val _userProfile = MutableStateFlow(PlayerProfile(guestId = "guest01", username = "Player1001"))
    val userProfile: StateFlow<PlayerProfile> = _userProfile.asStateFlow()

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    // Internet connectivity warning dialog
    private val _showNoInternetDialog = MutableStateFlow(false)
    val showNoInternetDialog: StateFlow<Boolean> = _showNoInternetDialog.asStateFlow()

    // In-match UI State
    private val _selectedPawn = MutableStateFlow<PlayerId?>(null)
    val selectedPawn: StateFlow<PlayerId?> = _selectedPawn.asStateFlow()

    private val _isWallMode = MutableStateFlow(false)
    val isWallMode: StateFlow<Boolean> = _isWallMode.asStateFlow()

    private val _wallOrientation = MutableStateFlow(WallOrientation.HORIZONTAL)
    val wallOrientation: StateFlow<WallOrientation> = _wallOrientation.asStateFlow()

    private val _previewWall = MutableStateFlow<Wall?>(null)
    val previewWall: StateFlow<Wall?> = _previewWall.asStateFlow()

    private val _activeEmote = MutableStateFlow<ActiveEmote?>(null)
    val activeEmote: StateFlow<ActiveEmote?> = _activeEmote.asStateFlow()

    private val _showResignDialog = MutableStateFlow(false)
    val showResignDialog: StateFlow<Boolean> = _showResignDialog.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    // Public Rooms List (Dynamic live rooms that periodically refresh and change)
    private val _publicRooms = MutableStateFlow<List<PublicRoomItem>>(emptyList())
    val publicRooms: StateFlow<List<PublicRoomItem>> = _publicRooms.asStateFlow()

    // Replay State
    private val _currentReplayEngine = MutableStateFlow<ReplayEngine?>(null)
    val currentReplayEngine: StateFlow<ReplayEngine?> = _currentReplayEngine.asStateFlow()
    private val _currentReplayStep = MutableStateFlow(0)
    val currentReplayStep: StateFlow<Int> = _currentReplayStep.asStateFlow()
    private val _isReplayPlaying = MutableStateFlow(false)
    val isReplayPlaying: StateFlow<Boolean> = _isReplayPlaying.asStateFlow()

    // P2P / Play with Friend
    private val _p2pStatus = MutableStateFlow(P2PConnectionStatus.IDLE)
    val p2pStatus: StateFlow<P2PConnectionStatus> = _p2pStatus.asStateFlow()

    private val _p2pDisconnectionMessage = MutableStateFlow<String?>(null)
    val p2pDisconnectionMessage: StateFlow<String?> = _p2pDisconnectionMessage.asStateFlow()

    private val _localIpAddress = MutableStateFlow<String>("192.168.43.1")
    val localIpAddress: StateFlow<String> = _localIpAddress.asStateFlow()

    private var isP2PActiveMatch = false
    private var isP2PHost = true

    // Match history from database
    val matchHistory: StateFlow<List<MatchRecord>> = repository.allMatches.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var timerJob: Job? = null
    private var aiJob: Job? = null
    private var replayJob: Job? = null
    private var roomsTickerJob: Job? = null
    private var matchStartTime: Long = 0L

    init {
        viewModelScope.launch {
            val profile = repository.getOrCreateProfile()
            _userProfile.value = profile
            detectLocalIp()
            generateLivePublicRooms()
            startLiveRoomsTicker()
        }
    }

    fun detectLocalIp() {
        val ip = NetworkHelper.getLocalIpAddress() ?: "192.168.43.1"
        _localIpAddress.value = ip
    }

    fun navigateTo(screen: ScreenState) {
        // If navigating away from Friend screen without an active match, cleanup P2P
        if (_currentScreen.value == ScreenState.PLAY_FRIEND && screen != ScreenState.MATCH) {
            cancelP2PConnection()
        }
        _currentScreen.value = screen
        soundManager.playButton()
    }

    // ==========================================
    // PERSISTENT SETTINGS MANAGEMENT
    // ==========================================

    fun setLanguage(lang: AppLanguage) {
        _settings.value = _settings.value.copy(language = lang)
        settingsPreferences.setLanguage(lang)
        soundManager.playButton()
    }

    fun toggleSound() {
        val next = !_settings.value.isSoundEnabled
        _settings.value = _settings.value.copy(isSoundEnabled = next)
        settingsPreferences.setSoundEnabled(next)
        soundManager.isSoundEnabled = next
    }

    fun toggleVibration() {
        val next = !_settings.value.isVibrationEnabled
        _settings.value = _settings.value.copy(isVibrationEnabled = next)
        settingsPreferences.setVibrationEnabled(next)
        soundManager.isVibrationEnabled = next
    }

    fun toggleRequireWallConfirm() {
        val next = !_settings.value.requireWallConfirm
        _settings.value = _settings.value.copy(requireWallConfirm = next)
        settingsPreferences.setWallConfirmRequired(next)
    }

    fun toggleShowLegalMoves() {
        val next = !_settings.value.showLegalMoves
        _settings.value = _settings.value.copy(showLegalMoves = next)
        settingsPreferences.setShowLegalMoves(next)
    }

    fun setTheme(themeId: com.example.ui.theme.GameThemeId) {
        val newTheme = com.example.ui.theme.GameTheme.fromId(themeId)
        _settings.value = _settings.value.copy(theme = newTheme)
        settingsPreferences.setThemeId(themeId)
        soundManager.playButton()
    }

    fun setShowSettingsDialog(show: Boolean) {
        _showSettingsDialog.value = show
    }

    fun dismissNoInternetDialog() {
        _showNoInternetDialog.value = false
    }

    fun updateUsername(newName: String, avatarId: Int) {
        viewModelScope.launch {
            repository.updateProfile(newName, avatarId)
            _userProfile.value = _userProfile.value.copy(username = newName, avatarId = avatarId)
        }
    }

    // ==========================================
    // ONLINE PLAY & LIVE DYNAMIC ROOMS
    // ==========================================

    fun onQuickMatchClicked() {
        val hasInternet = NetworkHelper.isInternetAvailable(getApplication())
        if (!hasInternet) {
            _showNoInternetDialog.value = true
            soundManager.playInvalid()
            return
        }

        // Live Matchmaking simulation with realistic online players
        viewModelScope.launch {
            soundManager.playButton()
            val onlineOpponents = listOf(
                "سلطان_الرياض", "فهد_التكتيكي", "عمر_المحترف", "أحمد_الجدار",
                "GrandMaster_Z", "WallKnight_99", "Sarah_Speed", "Tariq_Pro"
            )
            val chosenOpponent = onlineOpponents.random()
            val rules = GameRules(wallsPerPlayer = 10, timeLimitSeconds = 300, mode = GameMode.QUICK_MATCH)
            startMatch(rules, player2Name = chosenOpponent, player2IsAI = true)
        }
    }

    fun onPlayOnlineClicked() {
        val hasInternet = NetworkHelper.isInternetAvailable(getApplication())
        if (!hasInternet) {
            _showNoInternetDialog.value = true
            soundManager.playInvalid()
            return
        }
        generateLivePublicRooms()
        navigateTo(ScreenState.PUBLIC_ROOMS)
    }

    fun refreshPublicRooms() {
        val hasInternet = NetworkHelper.isInternetAvailable(getApplication())
        if (!hasInternet) {
            _showNoInternetDialog.value = true
            soundManager.playInvalid()
            return
        }
        generateLivePublicRooms()
        soundManager.playButton()
    }

    private fun generateLivePublicRooms() {
        val pool = listOf(
            Triple("سلطان_الرياض", 0, 24),
            Triple("عمر_المحترف", 1, 38),
            Triple("فهد_التكتيكي", 2, 45),
            Triple("GrandMaster_KSA", 3, 29),
            Triple("Ahmed_Apex", 0, 52),
            Triple("CyberPawn", 1, 65),
            Triple("TacticalSamir", 2, 33),
            Triple("WallKnight_88", 3, 41),
            Triple("Zaid_Champion", 0, 28),
            Triple("Rami_Speed", 1, 74)
        )

        val shuffled = pool.shuffled().take(5)
        val roomCodes = listOf("#8A7B2C", "#4F9D1E", "#3M7W8Q", "#9P2K5L", "#6Z4N1T", "#2X8C9V").shuffled()

        val generated = shuffled.mapIndexed { index, item ->
            val walls = if (index % 3 == 0) 15 else 10
            val time = when (index % 3) {
                0 -> 180
                1 -> 300
                else -> 0
            }
            PublicRoomItem(
                roomCode = roomCodes.getOrElse(index) { "#R${(1000..9999).random()}" },
                hostName = item.first,
                hostAvatar = item.second,
                timeLimitSeconds = time,
                wallsCount = walls,
                pingMs = item.third + (0..9).random(),
                status = if (index == 0) "جاهز للتحدي" else "بانتظار لاعب"
            )
        }
        _publicRooms.value = generated
    }

    private fun startLiveRoomsTicker() {
        roomsTickerJob?.cancel()
        roomsTickerJob = viewModelScope.launch {
            while (true) {
                delay(8000) // update dynamic lobby rooms periodically
                if (_currentScreen.value == ScreenState.PUBLIC_ROOMS) {
                    generateLivePublicRooms()
                }
            }
        }
    }

    fun joinPublicRoom(room: PublicRoomItem) {
        val hasInternet = NetworkHelper.isInternetAvailable(getApplication())
        if (!hasInternet) {
            _showNoInternetDialog.value = true
            soundManager.playInvalid()
            return
        }

        startMatch(
            rules = GameRules(
                wallsPerPlayer = room.wallsCount,
                timeLimitSeconds = room.timeLimitSeconds,
                mode = GameMode.PUBLIC_ROOM
            ),
            player2Name = room.hostName,
            player2Avatar = room.hostAvatar,
            player2IsAI = true
        )
    }

    // ==========================================
    // REAL PEER-TO-PEER MULTIPLAYER (PLAY WITH FRIEND)
    // ==========================================

    fun startP2PHostWifi(wallsCount: Int, timeLimit: Int, port: Int = 8888) {
        detectLocalIp()
        _p2pStatus.value = P2PConnectionStatus.LISTENING_WIFI
        _p2pDisconnectionMessage.value = null
        p2pConnection?.disconnect()

        p2pConnection = P2PGameConnection(getApplication(), this).apply {
            startWifiHost(
                localPlayerName = _userProfile.value.username,
                localAvatar = _userProfile.value.avatarId,
                wallsCount = wallsCount,
                timeLimit = timeLimit,
                port = port
            )
        }
    }

    fun connectP2PClientWifi(hostIp: String, port: Int = 8888) {
        _p2pStatus.value = P2PConnectionStatus.CONNECTING_WIFI
        _p2pDisconnectionMessage.value = null
        p2pConnection?.disconnect()

        p2pConnection = P2PGameConnection(getApplication(), this).apply {
            connectToWifiHost(
                hostIp = hostIp.trim(),
                localPlayerName = _userProfile.value.username,
                localAvatar = _userProfile.value.avatarId,
                port = port
            )
        }
    }

    fun startP2PHostBluetooth(wallsCount: Int, timeLimit: Int) {
        _p2pStatus.value = P2PConnectionStatus.LISTENING_BT
        _p2pDisconnectionMessage.value = null
        p2pConnection?.disconnect()

        p2pConnection = P2PGameConnection(getApplication(), this).apply {
            startBluetoothHost(
                localPlayerName = _userProfile.value.username,
                localAvatar = _userProfile.value.avatarId,
                wallsCount = wallsCount,
                timeLimit = timeLimit
            )
        }
    }

    fun connectP2PClientBluetooth(deviceAddress: String) {
        _p2pStatus.value = P2PConnectionStatus.CONNECTING_BT
        _p2pDisconnectionMessage.value = null
        p2pConnection?.disconnect()

        p2pConnection = P2PGameConnection(getApplication(), this).apply {
            connectToBluetoothDevice(
                deviceAddress = deviceAddress,
                localPlayerName = _userProfile.value.username,
                localAvatar = _userProfile.value.avatarId
            )
        }
    }

    fun cancelP2PConnection() {
        p2pConnection?.disconnect()
        p2pConnection = null
        isP2PActiveMatch = false
        _p2pStatus.value = P2PConnectionStatus.IDLE
    }

    fun dismissDisconnectionDialog() {
        _p2pDisconnectionMessage.value = null
        navigateTo(ScreenState.HOME)
    }

    // P2PGameListener Callbacks
    override fun onConnected(
        isHost: Boolean,
        opponentName: String,
        opponentAvatar: Int,
        wallsCount: Int,
        timeLimit: Int
    ) {
        _p2pStatus.value = P2PConnectionStatus.CONNECTED
        isP2PActiveMatch = true
        isP2PHost = isHost

        val rules = GameRules(
            wallsPerPlayer = wallsCount,
            timeLimitSeconds = timeLimit,
            mode = GameMode.FRIEND_ROOM
        )

        val p1Name = if (isHost) _userProfile.value.username else opponentName
        val p1Avatar = if (isHost) _userProfile.value.avatarId else opponentAvatar
        val p2Name = if (isHost) opponentName else _userProfile.value.username
        val p2Avatar = if (isHost) opponentAvatar else _userProfile.value.avatarId

        val initial = GameEngine.createInitialState(
            rules = rules,
            player1Name = p1Name,
            player2Name = p2Name,
            player1Avatar = p1Avatar,
            player2Avatar = p2Avatar,
            player2IsAI = false
        )

        _gameState.value = initial
        _selectedPawn.value = PlayerId.PLAYER_1
        _isWallMode.value = false
        _previewWall.value = null
        matchStartTime = System.currentTimeMillis()

        navigateTo(ScreenState.MATCH)
        startTimerLoop()
    }

    override fun onMoveReceived(targetX: Int, targetY: Int) {
        val current = _gameState.value ?: return
        val opponentId = if (isP2PHost) PlayerId.PLAYER_2 else PlayerId.PLAYER_1
        if (current.currentTurn != opponentId) return

        val target = Position(targetX, targetY)
        val legalMoves = RuleEngine.getLegalMoves(current, opponentId)
        if (target in legalMoves) {
            val updated = GameEngine.makeMove(current, target, opponentId)
            _gameState.value = updated
            soundManager.playMove()
            if (updated.status == GameStatus.FINISHED) {
                handleGameFinished(updated)
            }
        }
    }

    override fun onWallReceived(x: Int, y: Int, isHorizontal: Boolean) {
        val current = _gameState.value ?: return
        val opponentId = if (isP2PHost) PlayerId.PLAYER_2 else PlayerId.PLAYER_1
        if (current.currentTurn != opponentId) return

        val wall = Wall(
            x = x,
            y = y,
            orientation = if (isHorizontal) WallOrientation.HORIZONTAL else WallOrientation.VERTICAL,
            placedBy = opponentId
        )

        if (RuleEngine.isWallPlacementLegal(current, wall, opponentId)) {
            val updated = GameEngine.placeWall(current, wall, opponentId)
            _gameState.value = updated
            soundManager.playWallPlace()
            if (updated.status == GameStatus.FINISHED) {
                handleGameFinished(updated)
            }
        }
    }

    override fun onEmoteReceived(emoji: String) {
        val current = _gameState.value ?: return
        val opponentId = if (isP2PHost) PlayerId.PLAYER_2 else PlayerId.PLAYER_1
        val opponentName = if (isP2PHost) current.player2.name else current.player1.name

        _activeEmote.value = ActiveEmote(opponentId, emoji, opponentName)
        soundManager.playEmote()
        viewModelScope.launch {
            delay(2400)
            _activeEmote.value = null
        }
    }

    override fun onOpponentResigned() {
        val current = _gameState.value ?: return
        val opponentId = if (isP2PHost) PlayerId.PLAYER_2 else PlayerId.PLAYER_1
        val updated = GameEngine.resign(current, opponentId)
        _gameState.value = updated
        handleGameFinished(updated)
    }

    override fun onOpponentRequestedRematch() {
        startRematch()
    }

    override fun onConnectionLost(reason: String) {
        _p2pStatus.value = P2PConnectionStatus.DISCONNECTED
        _p2pDisconnectionMessage.value = reason
        soundManager.playInvalid()

        val current = _gameState.value
        if (current != null && current.status == GameStatus.IN_PROGRESS && isP2PActiveMatch) {
            val localPlayer = if (isP2PHost) PlayerId.PLAYER_1 else PlayerId.PLAYER_2
            val updated = current.copy(
                status = GameStatus.FINISHED,
                winner = localPlayer,
                finishReason = FinishReason.OPPONENT_DISCONNECTED
            )
            _gameState.value = updated
            handleGameFinished(updated)
        }
        isP2PActiveMatch = false
    }

    override fun onError(errorMessage: String) {
        _p2pDisconnectionMessage.value = errorMessage
        _p2pStatus.value = P2PConnectionStatus.DISCONNECTED
        soundManager.playInvalid()
    }

    // ==========================================
    // MATCH CONTROL & GAME ENGINE
    // ==========================================

    fun startMatch(
        rules: GameRules,
        player2Name: String = if (rules.mode == GameMode.VS_AI) "WallBot AI" else "Player 2",
        player2Avatar: Int = if (rules.mode == GameMode.VS_AI) 99 else 1,
        player2IsAI: Boolean = (rules.mode == GameMode.VS_AI)
    ) {
        val p1Name = _userProfile.value.username
        val initial = GameEngine.createInitialState(
            rules = rules,
            player1Name = p1Name,
            player2Name = player2Name,
            player1Avatar = _userProfile.value.avatarId,
            player2Avatar = player2Avatar,
            player2IsAI = player2IsAI
        )

        _gameState.value = initial
        _selectedPawn.value = PlayerId.PLAYER_1
        _isWallMode.value = false
        _previewWall.value = null
        matchStartTime = System.currentTimeMillis()

        navigateTo(ScreenState.MATCH)

        if (initial.status == GameStatus.COUNTDOWN) {
            startCountdownFlow()
        } else {
            startTimerLoop()
        }
    }

    private fun startCountdownFlow() {
        viewModelScope.launch {
            for (sec in 3 downTo 1) {
                _gameState.value = _gameState.value?.copy(countdownSeconds = sec)
                soundManager.playCountdownBeep(isGo = false)
                delay(900)
            }
            _gameState.value = _gameState.value?.copy(countdownSeconds = 0, status = GameStatus.IN_PROGRESS)
            soundManager.playCountdownBeep(isGo = true)
            startTimerLoop()
        }
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _gameState.value ?: break
                if (current.status == GameStatus.IN_PROGRESS) {
                    val updated = GameEngine.tickTimer(current, 1000L)
                    _gameState.value = updated
                    if (updated.status == GameStatus.FINISHED) {
                        handleGameFinished(updated)
                        break
                    }
                } else if (current.status == GameStatus.FINISHED) {
                    break
                }
            }
        }
    }

    fun onCellClicked(target: Position) {
        val current = _gameState.value ?: return
        if (current.status != GameStatus.IN_PROGRESS) return

        // Check local player authority
        val localPlayerId = if (isP2PActiveMatch) {
            if (isP2PHost) PlayerId.PLAYER_1 else PlayerId.PLAYER_2
        } else {
            PlayerId.PLAYER_1
        }

        val isMyTurn = current.currentTurn == localPlayerId ||
                (current.rules.mode == GameMode.PASS_AND_PLAY)

        if (!isMyTurn) return

        val legalMoves = RuleEngine.getLegalMoves(current, current.currentTurn)
        if (target in legalMoves) {
            val updated = GameEngine.makeMove(current, target, current.currentTurn)
            _gameState.value = updated
            soundManager.playMove()
            _isWallMode.value = false
            _previewWall.value = null

            // Send to P2P peer if active
            if (isP2PActiveMatch) {
                p2pConnection?.sendMove(target.x, target.y)
            }

            if (updated.status == GameStatus.FINISHED) {
                handleGameFinished(updated)
            } else {
                checkTriggerAI(updated)
            }
        } else {
            soundManager.playInvalid()
        }
    }

    fun onPawnClicked(playerId: PlayerId) {
        _selectedPawn.value = playerId
    }

    fun toggleWallMode() {
        val current = _gameState.value ?: return
        val player = current.getPlayer(current.currentTurn)
        if (player.remainingWalls <= 0) return

        _isWallMode.value = !_isWallMode.value
        if (_isWallMode.value) {
            val wall = Wall(
                x = 3,
                y = 4,
                orientation = _wallOrientation.value,
                placedBy = current.currentTurn
            )
            _previewWall.value = wall
        } else {
            _previewWall.value = null
        }
        soundManager.playButton()
    }

    fun toggleWallOrientation() {
        val next = if (_wallOrientation.value == WallOrientation.HORIZONTAL)
            WallOrientation.VERTICAL
        else
            WallOrientation.HORIZONTAL
        _wallOrientation.value = next

        _previewWall.value?.let { curr ->
            _previewWall.value = curr.copy(orientation = next)
        }
        soundManager.playButton()
    }

    fun onWallSlotClicked(x: Int, y: Int) {
        val current = _gameState.value ?: return
        val localPlayerId = if (isP2PActiveMatch) {
            if (isP2PHost) PlayerId.PLAYER_1 else PlayerId.PLAYER_2
        } else {
            PlayerId.PLAYER_1
        }

        val isMyTurn = current.currentTurn == localPlayerId ||
                (current.rules.mode == GameMode.PASS_AND_PLAY)
        if (!isMyTurn) return

        val player = current.getPlayer(current.currentTurn)
        if (player.remainingWalls <= 0) return

        val proposed = Wall(
            x = x,
            y = y,
            orientation = _wallOrientation.value,
            placedBy = current.currentTurn
        )

        _previewWall.value = proposed
        _isWallMode.value = true

        if (!_settings.value.requireWallConfirm) {
            confirmWallPlacement()
        }
    }

    fun confirmWallPlacement() {
        val current = _gameState.value ?: return
        val wall = _previewWall.value ?: return

        val localPlayerId = if (isP2PActiveMatch) {
            if (isP2PHost) PlayerId.PLAYER_1 else PlayerId.PLAYER_2
        } else {
            PlayerId.PLAYER_1
        }

        val isMyTurn = current.currentTurn == localPlayerId ||
                (current.rules.mode == GameMode.PASS_AND_PLAY)
        if (!isMyTurn) return

        if (RuleEngine.isWallPlacementLegal(current, wall, current.currentTurn)) {
            val updated = GameEngine.placeWall(current, wall, current.currentTurn)
            _gameState.value = updated
            soundManager.playWallPlace()
            _isWallMode.value = false
            _previewWall.value = null

            // Send to P2P peer if active
            if (isP2PActiveMatch) {
                p2pConnection?.sendWall(wall.x, wall.y, wall.orientation == WallOrientation.HORIZONTAL)
            }

            if (updated.status == GameStatus.FINISHED) {
                handleGameFinished(updated)
            } else {
                checkTriggerAI(updated)
            }
        } else {
            soundManager.playInvalid()
        }
    }

    fun cancelWallPlacement() {
        _isWallMode.value = false
        _previewWall.value = null
        soundManager.playButton()
    }

    private fun checkTriggerAI(state: GameState) {
        if (state.currentTurn == PlayerId.PLAYER_2 && state.player2.isAI && state.status == GameStatus.IN_PROGRESS) {
            aiJob?.cancel()
            aiJob = viewModelScope.launch {
                delay(650)
                val current = _gameState.value ?: return@launch
                if (current.currentTurn != PlayerId.PLAYER_2 || current.status != GameStatus.IN_PROGRESS) return@launch

                val action = AIEngine.decideMove(current, current.rules.aiDifficulty)
                when (action) {
                    is AIAction.Move -> {
                        val updated = GameEngine.makeMove(current, action.to, PlayerId.PLAYER_2)
                        _gameState.value = updated
                        soundManager.playMove()
                        if (updated.status == GameStatus.FINISHED) {
                            handleGameFinished(updated)
                        }
                    }
                    is AIAction.PlaceWall -> {
                        val updated = GameEngine.placeWall(current, action.wall, PlayerId.PLAYER_2)
                        _gameState.value = updated
                        soundManager.playWallPlace()
                        if (updated.status == GameStatus.FINISHED) {
                            handleGameFinished(updated)
                        }
                    }
                }
            }
        }
    }

    private fun handleGameFinished(finalState: GameState) {
        timerJob?.cancel()
        aiJob?.cancel()

        val duration = (System.currentTimeMillis() - matchStartTime) / 1000L
        val localPlayerId = if (isP2PActiveMatch && !isP2PHost) PlayerId.PLAYER_2 else PlayerId.PLAYER_1

        if (finalState.winner == localPlayerId) {
            soundManager.playWin()
        } else {
            soundManager.playLose()
        }

        viewModelScope.launch {
            repository.saveCompletedMatch(finalState, duration)
            _userProfile.value = repository.getOrCreateProfile()
        }
    }

    fun showResignConfirm(show: Boolean) {
        _showResignDialog.value = show
    }

    fun resignMatch() {
        _showResignDialog.value = false
        val current = _gameState.value ?: return

        val localPlayerId = if (isP2PActiveMatch && !isP2PHost) PlayerId.PLAYER_2 else PlayerId.PLAYER_1
        val updated = GameEngine.resign(current, localPlayerId)
        _gameState.value = updated

        if (isP2PActiveMatch) {
            p2pConnection?.sendResign()
        }
        handleGameFinished(updated)
    }

    fun sendEmote(emoji: String) {
        val current = _gameState.value ?: return
        val localPlayerId = if (isP2PActiveMatch && !isP2PHost) PlayerId.PLAYER_2 else PlayerId.PLAYER_1
        val localName = if (localPlayerId == PlayerId.PLAYER_1) current.player1.name else current.player2.name

        val updated = GameEngine.addEmote(current, localPlayerId, emoji)
        _gameState.value = updated
        soundManager.playEmote()

        _activeEmote.value = ActiveEmote(localPlayerId, emoji, localName)
        viewModelScope.launch {
            delay(2400)
            _activeEmote.value = null
        }

        if (isP2PActiveMatch) {
            p2pConnection?.sendEmote(emoji)
        } else if (current.player2.isAI) {
            viewModelScope.launch {
                delay(1200)
                val aiResponses = listOf("🫡", "🤝", "🔥", "😂", "👏")
                val responseEmoji = aiResponses.random()
                _activeEmote.value = ActiveEmote(PlayerId.PLAYER_2, responseEmoji, current.player2.name)
                soundManager.playEmote()
                delay(2400)
                _activeEmote.value = null
            }
        }
    }

    fun startRematch() {
        val current = _gameState.value ?: return
        if (isP2PActiveMatch) {
            p2pConnection?.sendRematch()
        }
        startMatch(current.rules, current.player2.name, current.player2.avatarId, current.player2.isAI)
    }

    // ==========================================
    // REPLAY VIEWER
    // ==========================================

    fun openReplayForCurrentMatch() {
        val current = _gameState.value ?: return
        startReplay(
            rules = current.rules,
            p1Name = current.player1.name,
            p2Name = current.player2.name,
            p1Avatar = current.player1.avatarId,
            p2Avatar = current.player2.avatarId,
            events = current.eventHistory
        )
    }

    fun startReplayFromRecord(record: MatchRecord) {
        val rules = repository.parseRules(record.rulesJson)
        val events = repository.deserializeEvents(record.eventsJson)
        startReplay(
            rules = rules,
            p1Name = record.player1Name,
            p2Name = record.player2Name,
            p1Avatar = record.player1Avatar,
            p2Avatar = record.player2Avatar,
            events = events
        )
    }

    private fun startReplay(
        rules: GameRules,
        p1Name: String,
        p2Name: String,
        p1Avatar: Int,
        p2Avatar: Int,
        events: List<GameEvent>
    ) {
        val engine = ReplayEngine(
            initialRules = rules,
            player1Name = p1Name,
            player2Name = p2Name,
            player1Avatar = p1Avatar,
            player2Avatar = p2Avatar,
            events = events
        )
        _currentReplayEngine.value = engine
        _currentReplayStep.value = 0
        _isReplayPlaying.value = false
        navigateTo(ScreenState.REPLAY)
    }

    fun setReplayStep(step: Int) {
        val engine = _currentReplayEngine.value ?: return
        _currentReplayStep.value = step.coerceIn(0, engine.totalSteps)
        soundManager.playButton()
    }

    fun toggleReplayPlay() {
        val next = !_isReplayPlaying.value
        _isReplayPlaying.value = next
        if (next) {
            replayJob?.cancel()
            replayJob = viewModelScope.launch {
                while (_isReplayPlaying.value) {
                    delay(1000)
                    val engine = _currentReplayEngine.value ?: break
                    if (_currentReplayStep.value < engine.totalSteps) {
                        _currentReplayStep.value += 1
                        soundManager.playMove()
                    } else {
                        _isReplayPlaying.value = false
                        break
                    }
                }
            }
        } else {
            replayJob?.cancel()
        }
    }
}
