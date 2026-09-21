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
import com.example.wallrush.domain.npc.NPCManager
import com.example.wallrush.domain.npc.NPCPersonality
import com.example.wallrush.domain.npc.NPCProfile
import com.example.wallrush.domain.achievements.AchievementManager
import com.example.wallrush.domain.challenges.DailyChallengeManager
import com.example.wallrush.domain.notifications.SmartNotificationHelper
import com.example.wallrush.domain.ai.PlayerPatternTracker
import com.example.wallrush.domain.progression.ProgressionManager
import com.example.wallrush.domain.progression.LevelInfo
import com.example.wallrush.domain.titles.TitlesManager
import com.example.wallrush.domain.titles.PlayerTitle
import com.example.wallrush.domain.combo.ComboManager
import com.example.wallrush.domain.combo.MatchRating
import com.example.wallrush.domain.powerups.PowerUpManager
import com.example.wallrush.domain.powerups.PowerUpDefinition
import com.example.wallrush.domain.editor.CustomLevel
import com.example.wallrush.domain.editor.CustomMapPresets
import com.example.wallrush.domain.backup.BackupRestoreManager
import com.example.wallrush.domain.ghost.GhostManager
import com.example.wallrush.data.local.CustomLevelEntity
import com.example.wallrush.ui.components.ActiveEmote
import com.example.wallrush.ui.localization.AppLanguage
import com.example.wallrush.ui.localization.Strings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class ScreenState {
    HOME,
    MATCH,
    PUBLIC_ROOMS,
    PLAY_FRIEND,
    TUTORIAL,
    REPLAY,
    LEADERBOARD,
    PROFILE,
    SETTINGS,
    ABOUT,
    ACHIEVEMENTS,
    LEVEL_EDITOR,
    CUSTOM_MAPS,
    TITLES
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
    val mode: GameMode = GameMode.PUBLIC_ROOM,
    val status: String = "Waiting",
    val currentPlayers: Int = 1,
    val maxPlayers: Int = 2,
    val isLiveGame: Boolean = false
)

data class CreatedRoomChallenge(
    val rules: GameRules,
    val roomCode: String,
    val challengerNpc: NPCProfile,
    val durationSeconds: Int = 20,
    val remainingSeconds: Int = 20
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

        SmartNotificationHelper.initChannel(application)
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

    private val _localPlayerId = MutableStateFlow(PlayerId.PLAYER_1)
    val localPlayerId: StateFlow<PlayerId> = _localPlayerId.asStateFlow()

    private var countdownJob: Job? = null

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

    // Active NPC Opponent (for rich personality, custom names, and behavior)
    private val _activeNPC = MutableStateFlow<NPCProfile?>(null)
    val activeNPC: StateFlow<NPCProfile?> = _activeNPC.asStateFlow()

    // Public Rooms List (Dynamic live rooms that periodically refresh and change)
    private val _publicRooms = MutableStateFlow<List<PublicRoomItem>>(emptyList())
    val publicRooms: StateFlow<List<PublicRoomItem>> = _publicRooms.asStateFlow()

    // Room Creation & Waiting Room Challenge State
    private val _createdRoomChallenge = MutableStateFlow<CreatedRoomChallenge?>(null)
    val createdRoomChallenge: StateFlow<CreatedRoomChallenge?> = _createdRoomChallenge.asStateFlow()

    // Offline bypass glitch mode (unlocked by secret long press on Quick Match / Play Online)
    private val _isOfflineBypassGlitchActive = MutableStateFlow(false)
    val isOfflineBypassGlitchActive: StateFlow<Boolean> = _isOfflineBypassGlitchActive.asStateFlow()

    // Achievements & Daily Quests
    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    private val _dailyChallenges = MutableStateFlow<List<DailyChallenge>>(emptyList())
    val dailyChallenges: StateFlow<List<DailyChallenge>> = _dailyChallenges.asStateFlow()

    // In-app sliding notification banner
    private val _inAppNotification = MutableStateFlow<InAppNotification?>(null)
    val inAppNotification: StateFlow<InAppNotification?> = _inAppNotification.asStateFlow()

    // Quick Match Loading Simulation State
    private val _isQuickMatchSearching = MutableStateFlow(false)
    val isQuickMatchSearching: StateFlow<Boolean> = _isQuickMatchSearching.asStateFlow()

    // Replay State
    private val _currentReplayEngine = MutableStateFlow<ReplayEngine?>(null)
    val currentReplayEngine: StateFlow<ReplayEngine?> = _currentReplayEngine.asStateFlow()
    private val _currentReplayStep = MutableStateFlow(0)
    val currentReplayStep: StateFlow<Int> = _currentReplayStep.asStateFlow()
    private val _isReplayPlaying = MutableStateFlow(false)
    val isReplayPlaying: StateFlow<Boolean> = _isReplayPlaying.asStateFlow()
    private val _replaySpeed = MutableStateFlow(1.0f)
    val replaySpeed: StateFlow<Float> = _replaySpeed.asStateFlow()

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

    // Progression & Level System (1-100)
    private val _levelInfo = MutableStateFlow(ProgressionManager.getLevelInfo(ProgressionManager.getTotalXp(application)))
    val levelInfo: StateFlow<LevelInfo> = _levelInfo.asStateFlow()

    private val _levelUpCelebration = MutableStateFlow<Pair<LevelInfo, PlayerTitle?>?>(null)
    val levelUpCelebration: StateFlow<Pair<LevelInfo, PlayerTitle?>?> = _levelUpCelebration.asStateFlow()

    // Titles & Badges
    private val _equippedTitle = MutableStateFlow(TitlesManager.getEquippedTitle(application))
    val equippedTitle: StateFlow<PlayerTitle> = _equippedTitle.asStateFlow()

    private val _unlockedTitles = MutableStateFlow(TitlesManager.getAllUnlockedTitleIds(application))
    val unlockedTitles: StateFlow<Set<String>> = _unlockedTitles.asStateFlow()

    // Combos, Near-Misses, and Move Feedback
    private val _matchRating = MutableStateFlow<MatchRating?>(null)
    val matchRating: StateFlow<MatchRating?> = _matchRating.asStateFlow()

    private val _activeFeedback = MutableStateFlow<String?>(null)
    val activeFeedback: StateFlow<String?> = _activeFeedback.asStateFlow()

    // Time Rewind Glitch visual effect & Charges
    val rewindCharges: StateFlow<Int> = _gameState.map { it?.player1?.rewindCharges ?: 2 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    private val _isGlitchActive = MutableStateFlow(false)
    val isGlitchActive: StateFlow<Boolean> = _isGlitchActive.asStateFlow()

    // Personal Ghost Trajectory
    private val _ghostTrajectory = MutableStateFlow<List<Position>>(GhostManager.getBestGhost(application)?.trajectory ?: emptyList())
    val ghostTrajectory: StateFlow<List<Position>> = _ghostTrajectory.asStateFlow()

    private val _isGhostEnabled = MutableStateFlow(GhostManager.isGhostEnabled(application))
    val isGhostEnabled: StateFlow<Boolean> = _isGhostEnabled.asStateFlow()

    // Custom Level Editor
    private val _customLevels = MutableStateFlow<List<CustomLevel>>(CustomMapPresets.PRESET_MAPS)
    val customLevels: StateFlow<List<CustomLevel>> = _customLevels.asStateFlow()

    private val _currentCustomLevel = MutableStateFlow<CustomLevel?>(null)
    val currentCustomLevel: StateFlow<CustomLevel?> = _currentCustomLevel.asStateFlow()

    // Backup & Restore Message
    private val _backupRestoreMessage = MutableStateFlow<String?>(null)
    val backupRestoreMessage: StateFlow<String?> = _backupRestoreMessage.asStateFlow()

    private var timerJob: Job? = null
    private var aiJob: Job? = null
    private var replayJob: Job? = null
    private var roomsTickerJob: Job? = null
    private var matchStartTime: Long = 0L

    init {
        viewModelScope.launch {
            val profile = repository.getOrCreateProfile()
            _userProfile.value = profile
            refreshAchievements()
            refreshDailyChallenges()
            detectLocalIp()
            generateLivePublicRooms()
            startLiveRoomsTicker()
            loadCustomLevels()
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

    fun navigateBack() {
        navigateTo(ScreenState.HOME)
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
            val lang = _settings.value.language
            AchievementManager.recordProfileCustomization(getApplication()) { ach ->
                SmartNotificationHelper.showAchievementUnlockedNotification(
                    getApplication(),
                    Strings.get(ach.titleKey, lang),
                    Strings.get(ach.descKey, lang),
                    ach.rewardXp
                )
                showInAppNotification(
                    title = "🏆 " + Strings.get(ach.titleKey, lang),
                    message = Strings.get(ach.descKey, lang),
                    icon = ach.icon,
                    type = NotificationType.ACHIEVEMENT
                )
                refreshAchievements()
            }
        }
    }

    fun updateUsername(newName: String) {
        val avatar = _userProfile.value.avatarId
        updateUsername(newName, avatar)
    }

    fun resetStatistics() {
        viewModelScope.launch {
            repository.resetStats()
            val updated = repository.getOrCreateProfile()
            _userProfile.value = updated
        }
    }

    // ==========================================
    // ACHIEVEMENTS & DAILY CHALLENGES MANAGEMENT
    // ==========================================

    fun refreshAchievements() {
        val list = AchievementManager.loadAllAchievements(getApplication(), _userProfile.value)
        _achievements.value = list
    }

    fun refreshDailyChallenges() {
        val list = DailyChallengeManager.loadTodayChallenges(getApplication())
        _dailyChallenges.value = list
    }

    fun launchDailyChallenge(challenge: DailyChallenge) {
        soundManager.playButton()
        val rules = GameRules(
            mode = challenge.launchMode,
            wallsPerPlayer = challenge.launchWallsCount,
            timeLimitSeconds = challenge.launchTimeLimitSeconds,
            aiDifficulty = challenge.launchAiDifficulty
        )

        val opponentName = challenge.opponentName
        val opponentAvatar = challenge.opponentAvatar

        if (challenge.launchMode == GameMode.QUAD_MODE) {
            val opponents = NPCManager.generateLobbyHosts(getApplication(), 3)
            startMatch(
                rules = rules,
                player2Name = opponentName,
                player2Avatar = opponentAvatar,
                player2IsAI = true,
                player3Name = "${opponents[0].countryFlag} ${opponents[0].name}",
                player3Avatar = opponents[0].avatarId,
                player3IsAI = true,
                player4Name = "${opponents[1].countryFlag} ${opponents[1].name}",
                player4Avatar = opponents[1].avatarId,
                player4IsAI = true
            )
        } else {
            startMatch(
                rules = rules,
                player2Name = opponentName,
                player2Avatar = opponentAvatar,
                player2IsAI = true
            )
        }
    }

    fun rerollRandomChallenge() {
        soundManager.playButton()
        DailyChallengeManager.rerollRandomChallenge(getApplication())
        refreshDailyChallenges()
    }

    fun claimDailyChallenge(challengeId: String) {
        val xp = DailyChallengeManager.claimChallengeReward(getApplication(), challengeId)
        if (xp > 0) {
            soundManager.playWin()
            viewModelScope.launch {
                val current = _userProfile.value
                val updated = current.copy(ratingScore = current.ratingScore + xp)
                repository.updateProfile(updated.username, updated.avatarId)
                _userProfile.value = updated
                refreshDailyChallenges()
                showInAppNotification(
                    title = "🎉 +" + xp + " Rating XP!",
                    message = Strings.get("claimed", _settings.value.language),
                    icon = "⭐",
                    type = NotificationType.DAILY_CHALLENGE
                )
            }
        }
    }

    fun activateOfflineBypassGlitch() {
        _isOfflineBypassGlitchActive.value = true
    }

    fun showInAppNotification(title: String, message: String, icon: String, type: NotificationType) {
        val notification = InAppNotification(
            id = System.currentTimeMillis().toString(),
            title = title,
            message = message,
            icon = icon,
            type = type
        )
        _inAppNotification.value = notification
        viewModelScope.launch {
            delay(4000)
            if (_inAppNotification.value?.id == notification.id) {
                _inAppNotification.value = null
            }
        }
    }

    fun dismissInAppNotification() {
        _inAppNotification.value = null
    }

    // ==========================================
    // ONLINE PLAY & LIVE DYNAMIC ROOMS
    // ==========================================

    fun onQuickMatchClicked(targetMode: GameMode = GameMode.QUICK_MATCH) {
        val hasInternet = NetworkHelper.isInternetAvailable(getApplication()) || _isOfflineBypassGlitchActive.value
        if (!hasInternet) {
            _showNoInternetDialog.value = true
            soundManager.playInvalid()
            return
        }

        // Live Matchmaking simulation with varied rules & search loading animation
        viewModelScope.launch {
            soundManager.playButton()
            _isQuickMatchSearching.value = true

            val randomWalls = if (targetMode == GameMode.QUAD_MODE) listOf(5, 8, 10).random() else listOf(8, 10, 12, 15).random()
            val randomTime = listOf(180, 240, 300, 420).random()
            val rules = GameRules(wallsPerPlayer = randomWalls, timeLimitSeconds = randomTime, mode = targetMode)

            val searchDelay = (1400L..2200L).random()
            delay(searchDelay)

            if (targetMode == GameMode.QUAD_MODE) {
                val opponents = NPCManager.generateLobbyHosts(getApplication(), 3)
                val p2 = opponents[0]
                val p3 = opponents[1]
                val p4 = opponents[2]
                _isQuickMatchSearching.value = false
                startMatch(
                    rules = rules,
                    player2Name = "${p2.countryFlag} ${p2.name}",
                    player2Avatar = p2.avatarId,
                    player2IsAI = true,
                    player3Name = "${p3.countryFlag} ${p3.name}",
                    player3Avatar = p3.avatarId,
                    player3IsAI = true,
                    player4Name = "${p4.countryFlag} ${p4.name}",
                    player4Avatar = p4.avatarId,
                    player4IsAI = true
                )
            } else {
                val npc = NPCManager.getRandomOpponent(getApplication())
                NPCManager.recordEncounter(getApplication(), npc.id)
                _activeNPC.value = npc
                _isQuickMatchSearching.value = false

                startMatch(
                    rules = rules,
                    player2Name = "${npc.countryFlag} ${npc.name}",
                    player2Avatar = npc.avatarId,
                    player2IsAI = true,
                    npcProfile = npc
                )
            }
        }
    }

    fun cancelQuickMatchSearch() {
        _isQuickMatchSearching.value = false
    }

    private var challengeTimerJob: Job? = null

    fun createPublicRoomWithWaiting(rules: GameRules) {
        val hasInternet = NetworkHelper.isInternetAvailable(getApplication()) || _isOfflineBypassGlitchActive.value
        if (!hasInternet) {
            _showNoInternetDialog.value = true
            soundManager.playInvalid()
            return
        }

        val code = "#" + (100000..999999).random().toString(16).uppercase()
        val opponent = NPCManager.getRandomOpponent(getApplication())
        val challengeDuration = (12..25).random()

        val challenge = CreatedRoomChallenge(
            rules = rules,
            roomCode = code,
            challengerNpc = opponent,
            durationSeconds = challengeDuration,
            remainingSeconds = challengeDuration
        )
        _createdRoomChallenge.value = challenge
        soundManager.playButton()

        challengeTimerJob?.cancel()
        challengeTimerJob = viewModelScope.launch {
            for (sec in challengeDuration downTo 0) {
                delay(1000)
                val cur = _createdRoomChallenge.value ?: break
                if (sec > 0) {
                    _createdRoomChallenge.value = cur.copy(remainingSeconds = sec - 1)
                } else {
                    // Time expired - pick another opponent or close
                    val nextOpponent = NPCManager.getRandomOpponent(getApplication())
                    _createdRoomChallenge.value = cur.copy(
                        challengerNpc = nextOpponent,
                        durationSeconds = 18,
                        remainingSeconds = 18
                    )
                }
            }
        }
    }

    fun acceptRoomChallenge() {
        val challenge = _createdRoomChallenge.value ?: return
        challengeTimerJob?.cancel()
        _createdRoomChallenge.value = null

        if (challenge.rules.mode == GameMode.QUAD_MODE) {
            val opponents = NPCManager.generateLobbyHosts(getApplication(), 3)
            val p2 = opponents[0]
            val p3 = opponents[1]
            val p4 = opponents[2]
            startMatch(
                rules = challenge.rules,
                player2Name = "${p2.countryFlag} ${p2.name}",
                player2Avatar = p2.avatarId,
                player2IsAI = true,
                player3Name = "${p3.countryFlag} ${p3.name}",
                player3Avatar = p3.avatarId,
                player3IsAI = true,
                player4Name = "${p4.countryFlag} ${p4.name}",
                player4Avatar = p4.avatarId,
                player4IsAI = true
            )
        } else {
            val npc = challenge.challengerNpc
            NPCManager.recordEncounter(getApplication(), npc.id)
            _activeNPC.value = npc

            startMatch(
                rules = challenge.rules,
                player2Name = "${npc.countryFlag} ${npc.name}",
                player2Avatar = npc.avatarId,
                player2IsAI = true,
                npcProfile = npc
            )
        }
    }

    fun rejectRoomChallenge() {
        // Switch to a new challenger waiting
        val current = _createdRoomChallenge.value ?: return
        val newNpc = NPCManager.getRandomOpponent(getApplication())
        _createdRoomChallenge.value = current.copy(
            challengerNpc = newNpc,
            durationSeconds = 15,
            remainingSeconds = 15
        )
        soundManager.playInvalid()
    }

    fun dismissRoomWaiting() {
        challengeTimerJob?.cancel()
        _createdRoomChallenge.value = null
    }

    fun onPlayOnlineClicked() {
        val hasInternet = NetworkHelper.isInternetAvailable(getApplication()) || _isOfflineBypassGlitchActive.value
        if (!hasInternet) {
            _showNoInternetDialog.value = true
            soundManager.playInvalid()
            return
        }
        generateLivePublicRooms()
        navigateTo(ScreenState.PUBLIC_ROOMS)
    }

    fun refreshPublicRooms() {
        val hasInternet = NetworkHelper.isInternetAvailable(getApplication()) || _isOfflineBypassGlitchActive.value
        if (!hasInternet) {
            _showNoInternetDialog.value = true
            soundManager.playInvalid()
            return
        }
        generateLivePublicRooms()
        soundManager.playButton()
    }

    private fun generateLivePublicRooms() {
        val hosts = NPCManager.generateLobbyHosts(getApplication(), 7)
        val roomCodes = listOf("#8A7B2C", "#4F9D1E", "#3M7W8Q", "#9P2K5L", "#6Z4N1T", "#2X8C9V", "#7Y1R4E", "#5T8U2W").shuffled()

        val generated = hosts.mapIndexed { index, host ->
            val mode = when (index % 3) {
                1 -> GameMode.RACE_MODE
                2 -> GameMode.QUAD_MODE
                else -> GameMode.PUBLIC_ROOM
            }
            val walls = if (mode == GameMode.QUAD_MODE) 5 else if (index % 2 == 0) 10 else 15
            val time = when (index % 3) {
                0 -> 180
                1 -> 300
                else -> 0
            }
            val maxP = if (mode == GameMode.QUAD_MODE) 4 else 2
            val isLive = index == 3 || index == 5
            val curP = if (isLive) maxP else 1
            val status = if (isLive) "جولة جارية ⚔️" else if (index == 0) "جاهز للتحدي 🔥" else "بانتظار منافس"
            PublicRoomItem(
                roomCode = roomCodes.getOrElse(index) { "#R${(1000..9999).random()}" },
                hostName = "${host.countryFlag} ${host.name}",
                hostAvatar = host.avatarId,
                timeLimitSeconds = time,
                wallsCount = walls,
                pingMs = (20..58).random(),
                mode = mode,
                status = status,
                currentPlayers = curP,
                maxPlayers = maxP,
                isLiveGame = isLive
            )
        }
        _publicRooms.value = generated
    }

    fun tickLivePublicRooms() {
        val currentList = _publicRooms.value
        if (currentList.isEmpty()) {
            generateLivePublicRooms()
            return
        }
        // Mutate dynamic state to reflect live multiplayer activity every 5 seconds
        val updated = currentList.mapIndexed { index, room ->
            val jitter = (-3..3).random()
            val newPing = (room.pingMs + jitter).coerceIn(18, 85)
            if (room.isLiveGame && (0..2).random() == 0) {
                val newHost = NPCManager.generateLobbyHosts(getApplication(), 1).firstOrNull()
                room.copy(
                    hostName = if (newHost != null) "${newHost.countryFlag} ${newHost.name}" else room.hostName,
                    pingMs = newPing,
                    isLiveGame = false,
                    currentPlayers = 1,
                    status = "غرفة جديدة ✨"
                )
            } else if (!room.isLiveGame && index == 1) {
                room.copy(
                    pingMs = newPing,
                    isLiveGame = true,
                    currentPlayers = room.maxPlayers,
                    status = "جولة جارية ⚔️"
                )
            } else {
                room.copy(pingMs = newPing)
            }
        }.sortedBy { it.isLiveGame }

        _publicRooms.value = updated
    }

    private fun startLiveRoomsTicker() {
        roomsTickerJob?.cancel()
        roomsTickerJob = viewModelScope.launch {
            while (true) {
                delay(5000L) // Exactly 5 seconds update interval
                if (_currentScreen.value == ScreenState.PUBLIC_ROOMS) {
                    tickLivePublicRooms()
                }
            }
        }
    }

    fun joinPublicRoom(room: PublicRoomItem) {
        val hasInternet = NetworkHelper.isInternetAvailable(getApplication()) || _isOfflineBypassGlitchActive.value
        if (!hasInternet) {
            _showNoInternetDialog.value = true
            soundManager.playInvalid()
            return
        }

        if (room.mode == GameMode.QUAD_MODE) {
            val opponents = NPCManager.generateLobbyHosts(getApplication(), 3)
            val p2 = opponents[0]
            val p3 = opponents[1]
            val p4 = opponents[2]
            startMatch(
                rules = GameRules(
                    wallsPerPlayer = room.wallsCount,
                    timeLimitSeconds = room.timeLimitSeconds,
                    mode = GameMode.QUAD_MODE
                ),
                player2Name = "${p2.countryFlag} ${p2.name}",
                player2Avatar = p2.avatarId,
                player2IsAI = true,
                player3Name = "${p3.countryFlag} ${p3.name}",
                player3Avatar = p3.avatarId,
                player3IsAI = true,
                player4Name = "${p4.countryFlag} ${p4.name}",
                player4Avatar = p4.avatarId,
                player4IsAI = true
            )
        } else {
            val npc = NPCManager.getRandomOpponent(getApplication())
            val roomNpc = npc.copy(name = room.hostName.substringAfter(" ").ifBlank { room.hostName }, avatarId = room.hostAvatar)
            _activeNPC.value = roomNpc

            startMatch(
                rules = GameRules(
                    wallsPerPlayer = room.wallsCount,
                    timeLimitSeconds = room.timeLimitSeconds,
                    mode = room.mode
                ),
                player2Name = room.hostName,
                player2Avatar = room.hostAvatar,
                player2IsAI = true,
                npcProfile = roomNpc
            )
        }
    }

    // ==========================================
    // REAL PEER-TO-PEER MULTIPLAYER (PLAY WITH FRIEND)
    // ==========================================

    fun startP2PHostWifi(wallsCount: Int, timeLimit: Int, mode: GameMode = GameMode.FRIEND_ROOM, port: Int = 8888) {
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
                mode = mode,
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

    fun startP2PHostBluetooth(wallsCount: Int, timeLimit: Int, mode: GameMode = GameMode.FRIEND_ROOM) {
        _p2pStatus.value = P2PConnectionStatus.LISTENING_BT
        _p2pDisconnectionMessage.value = null
        p2pConnection?.disconnect()

        p2pConnection = P2PGameConnection(getApplication(), this).apply {
            startBluetoothHost(
                localPlayerName = _userProfile.value.username,
                localAvatar = _userProfile.value.avatarId,
                wallsCount = wallsCount,
                timeLimit = timeLimit,
                mode = mode
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
        countdownJob?.cancel()
        timerJob?.cancel()
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
        timeLimit: Int,
        mode: GameMode
    ) {
        _p2pStatus.value = P2PConnectionStatus.CONNECTED
        isP2PActiveMatch = true
        isP2PHost = isHost

        val rules = GameRules(
            wallsPerPlayer = wallsCount,
            timeLimitSeconds = timeLimit,
            mode = mode
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

        val myId = if (isHost) PlayerId.PLAYER_1 else PlayerId.PLAYER_2
        _localPlayerId.value = myId
        _gameState.value = initial
        _selectedPawn.value = myId
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

    private fun showOpponentEmote(emoji: String) {
        val current = _gameState.value ?: return
        _activeEmote.value = ActiveEmote(PlayerId.PLAYER_2, emoji, current.player2.name)
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
        player2Name: String? = null,
        player2Avatar: Int? = null,
        player2IsAI: Boolean = (rules.mode == GameMode.VS_AI || rules.mode == GameMode.QUICK_MATCH || rules.mode == GameMode.PUBLIC_ROOM || rules.mode == GameMode.QUAD_MODE),
        player3Name: String? = null,
        player3Avatar: Int? = null,
        player3IsAI: Boolean? = null,
        player4Name: String? = null,
        player4Avatar: Int? = null,
        player4IsAI: Boolean? = null,
        npcProfile: NPCProfile? = null
    ) {
        val npc = if (player2IsAI && rules.mode != GameMode.QUAD_MODE) {
            npcProfile ?: NPCManager.getRandomOpponent(getApplication(), rules.aiDifficulty).also {
                NPCManager.recordEncounter(getApplication(), it.id)
            }
        } else {
            null
        }
        _activeNPC.value = npc

        val finalP2Name = player2Name ?: (npc?.name ?: if (rules.mode == GameMode.PASS_AND_PLAY) "Player 2" else if (rules.mode == GameMode.QUAD_MODE) "Player 2 (Red)" else "Guest")
        val finalP2Avatar = player2Avatar ?: (npc?.avatarId ?: 1)

        val finalP3Name = player3Name ?: if (rules.mode == GameMode.QUAD_MODE) "Player 3 (Green)" else "Player 3"
        val finalP3Avatar = player3Avatar ?: 2
        val finalP4Name = player4Name ?: if (rules.mode == GameMode.QUAD_MODE) "Player 4 (Yellow)" else "Player 4"
        val finalP4Avatar = player4Avatar ?: 3
        val finalP3IsAI = player3IsAI ?: player2IsAI
        val finalP4IsAI = player4IsAI ?: player2IsAI

        val p1Name = _userProfile.value.username
        val initial = GameEngine.createInitialState(
            rules = rules,
            player1Name = p1Name,
            player2Name = finalP2Name,
            player3Name = finalP3Name,
            player4Name = finalP4Name,
            player1Avatar = _userProfile.value.avatarId,
            player2Avatar = finalP2Avatar,
            player3Avatar = finalP3Avatar,
            player4Avatar = finalP4Avatar,
            player2IsAI = player2IsAI,
            player3IsAI = finalP3IsAI,
            player4IsAI = finalP4IsAI
        )

        _gameState.value = initial
        _localPlayerId.value = PlayerId.PLAYER_1
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
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
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
                (current.rules.mode == GameMode.PASS_AND_PLAY) ||
                !current.getCurrentPlayer().isAI

        if (!isMyTurn) return

        val legalMoves = RuleEngine.getLegalMoves(current, current.currentTurn)
        if (target in legalMoves) {
            val fromPos = current.getPlayer(current.currentTurn).position
            val updated = GameEngine.makeMove(current, target, current.currentTurn)
            
            // Record pattern tracking and combo evaluation for Player 1
            var stateToEmit = updated
            if (current.currentTurn == PlayerId.PLAYER_1) {
                val closestWallDist = current.walls.minOfOrNull {
                    kotlin.math.abs(it.x - target.x) + kotlin.math.abs(it.y - target.y)
                } ?: 9
                PlayerPatternTracker.recordPlayerMove(getApplication(), fromPos, target, closestWallDist)

                val lastWall = current.walls.lastOrNull()
                val feedback = ComboManager.evaluateMove(
                    from = fromPos,
                    to = target,
                    walls = current.walls,
                    lastWallPlaced = lastWall,
                    currentCombo = current.comboCount,
                    opponentPos = current.player2.position,
                    goalY = current.player1.targetGoalRow ?: 0,
                    gridSize = current.rules.gridSize
                )
                val newMaxCombo = kotlin.math.max(current.maxCombo, feedback.comboCount)
                val nearMisses = current.nearMissCount + (if (feedback.isNearMiss) 1 else 0)
                val currentP1 = updated.player1
                val updatedEnergy = (currentP1.energy + feedback.energyGained).coerceIn(0, 100)
                stateToEmit = updated.copy(
                    player1 = currentP1.copy(energy = updatedEnergy),
                    comboCount = feedback.comboCount,
                    maxCombo = newMaxCombo,
                    nearMissCount = nearMisses
                )
                if (feedback.feedbackMessageEn != null) {
                    val isAr = _settings.value.language == AppLanguage.ARABIC
                    _activeFeedback.value = if (isAr) feedback.feedbackMessageAr else feedback.feedbackMessageEn
                    viewModelScope.launch {
                        delay(1800)
                        _activeFeedback.value = null
                    }
                    if (feedback.isNearMiss) {
                        soundManager.playNearMiss()
                    }
                }
            }

            _gameState.value = stateToEmit
            soundManager.playMove()
            _isWallMode.value = false
            _previewWall.value = null

            // Send to P2P peer if active
            if (isP2PActiveMatch) {
                p2pConnection?.sendMove(target.x, target.y)
            }

            if (stateToEmit.status == GameStatus.FINISHED) {
                handleGameFinished(stateToEmit)
            } else {
                checkTriggerAI(stateToEmit)
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
                (current.rules.mode == GameMode.PASS_AND_PLAY) ||
                !current.getCurrentPlayer().isAI
        if (!isMyTurn) return

        if (RuleEngine.isWallPlacementLegal(current, wall, current.currentTurn)) {
            if (current.currentTurn == PlayerId.PLAYER_1) {
                PlayerPatternTracker.recordPlayerWallPlacement(getApplication(), wall, current.player1.position, current.player2.position)
            }
            val updated = GameEngine.placeWall(current, wall, current.currentTurn)
            _gameState.value = updated
            soundManager.playWallPlace()
            _isWallMode.value = false
            _previewWall.value = null

            DailyChallengeManager.onWallPlaced(getApplication()) { ch ->
                val lang = _settings.value.language
                SmartNotificationHelper.showDailyQuestsNotification(
                    getApplication(),
                    Strings.get(ch.titleKey, lang),
                    Strings.get(ch.descKey, lang)
                )
                showInAppNotification(
                    title = "⚔️ " + Strings.get(ch.titleKey, lang),
                    message = Strings.get(ch.descKey, lang),
                    icon = ch.icon,
                    type = NotificationType.DAILY_CHALLENGE
                )
                refreshDailyChallenges()
            }

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
        val currentPlayer = state.getCurrentPlayer()
        if (currentPlayer.isAI && state.status == GameStatus.IN_PROGRESS) {
            val turnPlayerId = state.currentTurn
            aiJob?.cancel()
            aiJob = viewModelScope.launch {
                val npc = _activeNPC.value
                val delayMs = if (npc != null && state.rules.mode != GameMode.QUAD_MODE) {
                    Random.nextLong(npc.minThinkingDelayMs, npc.maxThinkingDelayMs + 1)
                } else {
                    550L
                }
                delay(delayMs)
                val current = _gameState.value ?: return@launch
                if (current.currentTurn != turnPlayerId || current.status != GameStatus.IN_PROGRESS) return@launch

                val action = if (npc != null && current.rules.mode != GameMode.QUAD_MODE) {
                    AIEngine.decideNPCMove(current, npc.personality, getApplication())
                } else {
                    AIEngine.decideMove(current, current.rules.aiDifficulty, getApplication())
                }

                when (action) {
                    is AIAction.Move -> {
                        val updated = GameEngine.makeMove(current, action.to, turnPlayerId)
                        _gameState.value = updated
                        soundManager.playMove()

                        // Occasional natural human emote & authentic personality reaction
                        if (Random.nextFloat() < 0.18f && npc != null) {
                            val isArabic = _settings.value.language == com.example.wallrush.ui.localization.AppLanguage.ARABIC
                            val reaction = if (Random.nextBoolean()) {
                                npc.personality.emoji
                            } else {
                                npc.personality.getPersonalityReaction(isWallPlaced = false, isWinning = false, isArabic = isArabic)
                            }
                            showOpponentEmote(reaction)
                        } else if (Random.nextFloat() < 0.08f) {
                            showOpponentEmote("😎")
                        }

                        if (updated.status == GameStatus.FINISHED) {
                            handleGameFinished(updated)
                        } else {
                            checkTriggerAI(updated)
                        }
                    }
                    is AIAction.PlaceWall -> {
                        val updated = GameEngine.placeWall(current, action.wall, turnPlayerId)
                        _gameState.value = updated
                        soundManager.playWallPlace()

                        // Occasional natural reaction emote when placing a barrier
                        if (Random.nextFloat() < 0.25f && npc != null) {
                            val isArabic = _settings.value.language == com.example.wallrush.ui.localization.AppLanguage.ARABIC
                            val reaction = if (Random.nextBoolean()) {
                                npc.personality.emoji
                            } else {
                                npc.personality.getPersonalityReaction(isWallPlaced = true, isWinning = false, isArabic = isArabic)
                            }
                            showOpponentEmote(reaction)
                        } else if (Random.nextFloat() < 0.12f) {
                            showOpponentEmote("🧱")
                        }

                        if (updated.status == GameStatus.FINISHED) {
                            handleGameFinished(updated)
                        } else {
                            checkTriggerAI(updated)
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

        val lang = _settings.value.language

        AchievementManager.onMatchFinished(
            context = getApplication(),
            finalState = finalState,
            localPlayerId = localPlayerId,
            profile = _userProfile.value,
            isP2PMatch = isP2PActiveMatch,
            aiPersonality = _activeNPC.value?.personality
        ) { ach ->
            SmartNotificationHelper.showAchievementUnlockedNotification(
                getApplication(),
                Strings.get(ach.titleKey, lang),
                Strings.get(ach.descKey, lang),
                ach.rewardXp
            )
            showInAppNotification(
                title = "🏆 " + Strings.get(ach.titleKey, lang),
                message = Strings.get(ach.descKey, lang),
                icon = ach.icon,
                type = NotificationType.ACHIEVEMENT
            )
            refreshAchievements()
        }

        DailyChallengeManager.onMatchFinished(
            context = getApplication(),
            finalState = finalState,
            localPlayerId = localPlayerId
        ) { ch ->
            SmartNotificationHelper.showDailyQuestsNotification(
                getApplication(),
                Strings.get(ch.titleKey, lang),
                Strings.get(ch.descKey, lang)
            )
            showInAppNotification(
                title = "⚔️ " + Strings.get(ch.titleKey, lang),
                message = Strings.get(ch.descKey, lang),
                icon = ch.icon,
                type = NotificationType.DAILY_CHALLENGE
            )
            refreshDailyChallenges()
        }

        viewModelScope.launch {
            repository.saveCompletedMatch(finalState, duration, localPlayerId)
            _userProfile.value = repository.getOrCreateProfile()
            refreshAchievements()
            refreshDailyChallenges()

            // Rating & Progression calculation
            val rating = ComboManager.calculateMatchRating(finalState, duration, localPlayerId, powerupsUsed = 0)
            _matchRating.value = rating

            val (newLevelInfo, leveledUp) = ProgressionManager.addXp(getApplication(), rating.totalBonusXp)
            _levelInfo.value = newLevelInfo
            if (leveledUp) {
                soundManager.playLevelUp()
                val newTitle = TitlesManager.ALL_TITLES.firstOrNull { it.requiredLevel == newLevelInfo.level }
                _levelUpCelebration.value = Pair(newLevelInfo, newTitle)
            }
            _unlockedTitles.value = TitlesManager.getAllUnlockedTitleIds(getApplication())

            // Save Best Ghost run if victorious
            val playerPawnMoves = finalState.eventHistory
                .filterIsInstance<GameEvent.PawnMoved>()
                .filter { it.player == localPlayerId }
                .map { it.to }
            if (finalState.winner == localPlayerId && playerPawnMoves.isNotEmpty()) {
                GhostManager.saveIfBestGhost(getApplication(), duration, playerPawnMoves.size, playerPawnMoves)
                _ghostTrajectory.value = playerPawnMoves
            }
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
            delay(3200)
            _activeEmote.value = null
        }

        if (isP2PActiveMatch) {
            p2pConnection?.sendEmote(emoji)
        } else if (current.player2.isAI) {
            val npc = _activeNPC.value
            viewModelScope.launch {
                // Human-like thinking delay: 1800ms - 3600ms (never instantly in the same second)
                val delayTime = Random.nextLong(1800L, 3600L)
                delay(delayTime)

                val activeCurrent = _gameState.value ?: return@launch
                if (activeCurrent.status != GameStatus.IN_PROGRESS && activeCurrent.status != GameStatus.FINISHED) return@launch

                val isArabic = npc?.countryFlag in listOf("🇸🇦", "🇪🇬", "🇦🇪", "🇲🇦", "🇩🇿", "🇯🇴", "🇰🇼", "🇶🇦", "🇴🇲", "🇧🇭", "🇮🇶", "🇱🇧", "🇾🇪", "🇸🇾", "🇹🇳", "🇱🇾", "🇸🇩", "🇵🇸") ||
                        (npc?.name?.any { it in '\u0600'..'\u06FF' } == true)

                // User request: "لا اريدهم أن يتكلموا كثير، اغلب ردودهم تكون ايموجيات مفهوم"
                // 80% emojis, 20% short natural human phrase
                val respondWithEmoji = Random.nextFloat() < 0.80f

                val responseContent = if (respondWithEmoji) {
                    val pool = if (isArabic) {
                        listOf("🤝", "😎", "🔥", "😂", "👏", "🫡", "🧱", "⚡", "🎯", "🤯", "✌️")
                    } else {
                        listOf("🤝", "😎", "🔥", "👏", "🫡", "🧱", "⚡", "🎯", "😱", "🏆", "👍")
                    }
                    pool.random()
                } else {
                    val phrases = if (isArabic) {
                        listOf("هلا والله 🤝", "كفو يا بطل 🔥", "حركة قوية!", "بالتوفيق!", "ما شاء الله", "الله يستر هههه", "gg يا غالي")
                    } else {
                        listOf("gg! 🤝", "nice move! 🔥", "gl hf!", "well played!", "close one! 😱", "thanks!", "whoa haha")
                    }
                    phrases.random()
                }

                _activeEmote.value = ActiveEmote(PlayerId.PLAYER_2, responseContent, current.player2.name)
                soundManager.playEmote()
                delay(3200)
                _activeEmote.value = null
            }
        }
    }

    fun startRematch() {
        val current = _gameState.value ?: return
        if (isP2PActiveMatch) {
            p2pConnection?.sendRematch()
            val p1Name = if (isP2PHost) _userProfile.value.username else current.player1.name
            val p1Avatar = if (isP2PHost) _userProfile.value.avatarId else current.player1.avatarId
            val p2Name = if (isP2PHost) current.player2.name else _userProfile.value.username
            val p2Avatar = if (isP2PHost) current.player2.avatarId else _userProfile.value.avatarId

            val initial = GameEngine.createInitialState(
                rules = current.rules,
                player1Name = p1Name,
                player2Name = p2Name,
                player1Avatar = p1Avatar,
                player2Avatar = p2Avatar,
                player2IsAI = false
            )
            _gameState.value = initial
            val myId = if (isP2PHost) PlayerId.PLAYER_1 else PlayerId.PLAYER_2
            _localPlayerId.value = myId
            _selectedPawn.value = myId
            _isWallMode.value = false
            _previewWall.value = null
            matchStartTime = System.currentTimeMillis()
            navigateTo(ScreenState.MATCH)
            if (initial.status == GameStatus.COUNTDOWN) {
                startCountdownFlow()
            } else {
                startTimerLoop()
            }
            return
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

    fun setReplaySpeed(speed: Float) {
        _replaySpeed.value = speed
        if (_isReplayPlaying.value) {
            toggleReplayPlay()
            toggleReplayPlay()
        }
    }

    fun toggleReplayPlay() {
        val next = !_isReplayPlaying.value
        _isReplayPlaying.value = next
        if (next) {
            replayJob?.cancel()
            replayJob = viewModelScope.launch {
                while (_isReplayPlaying.value) {
                    val delayMs = ((1000L / _replaySpeed.value.coerceAtLeast(0.1f))).toLong()
                    delay(delayMs)
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

    // ==========================================
    // POWER-UPS & TIME REWIND
    // ==========================================

    fun usePowerUp(type: PowerUpType) {
        val current = _gameState.value ?: return
        if (current.status != GameStatus.IN_PROGRESS) return
        val localPlayerId = if (isP2PActiveMatch && !isP2PHost) PlayerId.PLAYER_2 else PlayerId.PLAYER_1
        if (current.currentTurn != localPlayerId) return

        if (type == PowerUpType.TIME_REWIND) {
            executeTimeRewind()
            return
        }

        val (newState, message) = PowerUpManager.applyPowerUp(current, localPlayerId, type)
        if (newState != current) {
            _gameState.value = newState
            soundManager.playPowerUp()
            _activeFeedback.value = message
            viewModelScope.launch {
                delay(1800)
                _activeFeedback.value = null
            }
            if (newState.status == GameStatus.FINISHED) {
                handleGameFinished(newState)
            } else if (newState.currentTurn != localPlayerId) {
                checkTriggerAI(newState)
            }
        } else {
            soundManager.playInvalid()
            _activeFeedback.value = message
            viewModelScope.launch {
                delay(1800)
                _activeFeedback.value = null
            }
        }
    }

    fun executeTimeRewind() {
        val current = _gameState.value ?: return
        if (current.status != GameStatus.IN_PROGRESS) return
        if (current.rules.mode != GameMode.VS_AI && current.rules.mode != GameMode.QUICK_MATCH) {
            soundManager.playInvalid()
            return
        }
        val p1 = current.player1
        if (p1.rewindCharges <= 0) {
            soundManager.playInvalid()
            return
        }

        aiJob?.cancel()
        val events = current.eventHistory
        if (events.isEmpty()) {
            soundManager.playInvalid()
            return
        }

        val numToRevert = if (events.size >= 2) 2 else 1
        val remainingEvents = events.dropLast(numToRevert)

        var restoredState = GameEngine.createInitialState(
            rules = current.rules,
            player1Name = current.player1.name,
            player2Name = current.player2.name,
            player1Avatar = current.player1.avatarId,
            player2Avatar = current.player2.avatarId,
            player2IsAI = current.player2.isAI
        ).copy(
            obstacles = current.obstacles
        )

        for (event in remainingEvents) {
            when (event) {
                is GameEvent.PawnMoved -> {
                    restoredState = GameEngine.makeMove(restoredState, event.to, event.player)
                }
                is GameEvent.WallPlaced -> {
                    restoredState = GameEngine.placeWall(restoredState, event.wall, event.player)
                }
                else -> {}
            }
        }

        val updatedP1 = restoredState.player1.copy(
            rewindCharges = p1.rewindCharges - 1,
            energy = p1.energy
        )

        _gameState.value = restoredState.copy(
            player1 = updatedP1,
            currentTurn = PlayerId.PLAYER_1,
            status = GameStatus.IN_PROGRESS
        )

        soundManager.playGlitchRewind()
        _isGlitchActive.value = true
        viewModelScope.launch {
            delay(550)
            _isGlitchActive.value = false
        }
    }

    // ==========================================
    // TITLES SYSTEM
    // ==========================================

    fun equipTitle(titleId: String) {
        TitlesManager.setEquippedTitle(getApplication(), titleId)
        _equippedTitle.value = TitlesManager.getEquippedTitle(getApplication())
        soundManager.playButton()
    }

    fun dismissLevelUp() {
        _levelUpCelebration.value = null
    }

    fun dismissMatchRating() {
        _matchRating.value = null
    }

    fun setGhostEnabled(enabled: Boolean) {
        GhostManager.setGhostEnabled(getApplication(), enabled)
        _isGhostEnabled.value = enabled
    }

    // ==========================================
    // LEVEL EDITOR & CUSTOM MAPS
    // ==========================================

    fun loadCustomLevels() {
        viewModelScope.launch {
            repository.allCustomLevels.collect { entities ->
                val customLoaded = entities.mapNotNull {
                    CustomMapPresets.deserializeFromJson(it.jsonContent)
                }
                _customLevels.value = CustomMapPresets.PRESET_MAPS + customLoaded
            }
        }
    }

    fun saveCustomLevel(level: CustomLevel) {
        viewModelScope.launch {
            val json = CustomMapPresets.serializeToJson(level)
            val entity = CustomLevelEntity(
                id = level.id,
                name = level.name,
                description = level.description,
                gridSize = level.gridSize,
                jsonContent = json
            )
            repository.saveCustomLevel(entity)
            soundManager.playButton()
        }
    }

    fun deleteCustomLevel(levelId: String) {
        viewModelScope.launch {
            repository.deleteCustomLevel(levelId)
            soundManager.playButton()
        }
    }

    fun startCustomLevelMatch(level: CustomLevel) {
        val gSize = level.gridSize.coerceAtLeast(3)
        val p1Goal = if (level.p1Start.y > gSize / 2) 0 else gSize - 1
        val p2Goal = if (level.p2Start.y < gSize / 2) gSize - 1 else 0

        val rules = GameRules(
            wallsPerPlayer = level.wallsCount,
            timeLimitSeconds = level.timeLimitSeconds,
            mode = GameMode.VS_AI,
            aiDifficulty = level.aiDifficulty,
            gridSize = gSize
        )
        val opponent = NPCProfile(
            id = "custom_bot",
            name = "Tactical Bot",
            avatarId = 2,
            personality = level.aiPersonality,
            countryFlag = "🤖",
            countryName = "Cyber Arena",
            rating = 1400,
            minThinkingDelayMs = 800L,
            maxThinkingDelayMs = 1500L
        )
        _activeNPC.value = opponent

        val safeP1Start = if (level.obstacles.any { it.x == level.p1Start.x && it.y == level.p1Start.y }) {
            Position(gSize / 2, gSize - 1)
        } else level.p1Start

        val safeP2Start = if (level.obstacles.any { it.x == level.p2Start.x && it.y == level.p2Start.y } || safeP1Start == level.p2Start) {
            Position(gSize / 2, 0)
        } else level.p2Start

        val initial = GameEngine.createInitialState(
            rules = rules,
            player1Name = _userProfile.value.username,
            player2Name = opponent.name,
            player1Avatar = _userProfile.value.avatarId,
            player2Avatar = opponent.avatarId,
            player2IsAI = true
        ).copy(
            obstacles = level.obstacles,
            player1 = PlayerState(
                id = PlayerId.PLAYER_1,
                name = _userProfile.value.username,
                avatarId = _userProfile.value.avatarId,
                position = safeP1Start,
                remainingWalls = level.wallsCount,
                targetGoalRow = p1Goal
            ),
            player2 = PlayerState(
                id = PlayerId.PLAYER_2,
                name = opponent.name,
                avatarId = opponent.avatarId,
                position = safeP2Start,
                remainingWalls = level.wallsCount,
                isAI = true,
                targetGoalRow = p2Goal
            ),
            moveCount = 0
        )

        // Filter and safely add preset walls without isolating players
        var validWalls = emptyList<Wall>()
        for (w in level.presetWalls) {
            if (w.x in 0 until (gSize - 1) && w.y in 0 until (gSize - 1)) {
                val candidate = validWalls + w
                val p1HasPath = com.example.wallrush.domain.engine.PathFinder.hasPathToGoal(safeP1Start, p1Goal, candidate, gSize, level.obstacles)
                val p2HasPath = com.example.wallrush.domain.engine.PathFinder.hasPathToGoal(safeP2Start, p2Goal, candidate, gSize, level.obstacles)
                if (p1HasPath && p2HasPath) {
                    validWalls = candidate
                }
            }
        }

        val stateWithWalls = initial.copy(walls = validWalls)

        _gameState.value = stateWithWalls
        _localPlayerId.value = PlayerId.PLAYER_1
        _selectedPawn.value = PlayerId.PLAYER_1
        _isWallMode.value = false
        _previewWall.value = null
        matchStartTime = System.currentTimeMillis()
        navigateTo(ScreenState.MATCH)
        startTimerLoop()
    }

    // ==========================================
    // DAILY CHALLENGE DIRECT LAUNCH
    // ==========================================

    fun startDailyChallengeDirectly(challenge: DailyChallenge) {
        val rules = when (challenge.id) {
            "daily_race" -> GameRules(
                mode = GameMode.RACE_MODE,
                aiDifficulty = AIDifficulty.HARD,
                timeLimitSeconds = 60,
                wallsPerPlayer = 8
            )
            "daily_quad" -> GameRules(
                mode = GameMode.QUAD_MODE,
                aiDifficulty = AIDifficulty.HARD,
                timeLimitSeconds = 90,
                wallsPerPlayer = 6
            )
            "daily_walls" -> GameRules(
                mode = GameMode.VS_AI,
                aiDifficulty = AIDifficulty.NORMAL,
                timeLimitSeconds = 180,
                wallsPerPlayer = 12
            )
            "daily_ai" -> GameRules(
                mode = GameMode.VS_AI,
                aiDifficulty = AIDifficulty.EXPERT,
                timeLimitSeconds = 120,
                wallsPerPlayer = 10
            )
            else -> GameRules(
                mode = GameMode.VS_AI,
                aiDifficulty = AIDifficulty.values().random(),
                timeLimitSeconds = listOf(60, 90, 120, 180).random(),
                wallsPerPlayer = (6..12).random()
            )
        }
        val opponent = NPCManager.getRandomOpponent(getApplication(), rules.aiDifficulty)
        startMatch(
            rules = rules,
            player2Name = "${opponent.countryFlag} ${opponent.name}",
            player2Avatar = opponent.avatarId,
            player2IsAI = true,
            npcProfile = opponent
        )
    }

    // ==========================================
    // BACKUP & RESTORE
    // ==========================================

    fun exportBackup(): String {
        return BackupRestoreManager.createBackupString(getApplication(), _userProfile.value)
    }

    fun importBackup(backupContent: String): Boolean {
        val result = BackupRestoreManager.restoreFromBackupString(getApplication(), backupContent)
        val isAr = _settings.value.language == AppLanguage.ARABIC
        return when (result) {
            is com.example.wallrush.domain.backup.RestoreResult.Success -> {
                _userProfile.value = result.profile
                _levelInfo.value = ProgressionManager.getLevelInfo(ProgressionManager.getTotalXp(getApplication()))
                _unlockedTitles.value = TitlesManager.getAllUnlockedTitleIds(getApplication())
                _equippedTitle.value = TitlesManager.getEquippedTitle(getApplication())
                _backupRestoreMessage.value = if (isAr) "تمت استعادة ملف الحفظ بنجاح!" else "Save restored successfully!"
                soundManager.playLevelUp()
                true
            }
            is com.example.wallrush.domain.backup.RestoreResult.Error -> {
                _backupRestoreMessage.value = if (isAr) result.messageAr else result.messageEn
                soundManager.playInvalid()
                false
            }
        }
    }

    fun clearBackupMessage() {
        _backupRestoreMessage.value = null
    }

    fun dismissLevelUpCelebration() {
        _levelUpCelebration.value = null
    }
}
