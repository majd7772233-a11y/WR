package com.example.wallrush.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepSlateBackground
import com.example.ui.theme.TextPrimary
import com.example.wallrush.domain.engine.RuleEngine
import com.example.wallrush.domain.model.GameMode
import com.example.wallrush.domain.model.GameStatus
import com.example.wallrush.domain.model.PlayerId
import com.example.wallrush.ui.components.*
import com.example.wallrush.ui.localization.Strings
import com.example.wallrush.ui.viewmodel.ScreenState
import com.example.wallrush.ui.viewmodel.WallRushViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchScreen(
    viewModel: WallRushViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.gameState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isWallMode by viewModel.isWallMode.collectAsState()
    val wallOrientation by viewModel.wallOrientation.collectAsState()
    val previewWall by viewModel.previewWall.collectAsState()
    val selectedPawn by viewModel.selectedPawn.collectAsState()
    val activeEmote by viewModel.activeEmote.collectAsState()
    val showResignDialog by viewModel.showResignDialog.collectAsState()
    val localPlayerId by viewModel.localPlayerId.collectAsState()

    val current = state ?: return
    val language = settings.language

    // Dynamic Board Flipping: Default to flipped for Player 2 so local pawn is at the bottom, or toggleable anytime
    var isFlipped by remember(localPlayerId) {
        mutableStateOf(localPlayerId == PlayerId.PLAYER_2)
    }

    // Determine roles: Host/P1 (Blue) vs Guest/P2 (Red)
    val isUserPlayer1 = (localPlayerId == PlayerId.PLAYER_1)
    val userPlayer = if (isUserPlayer1) current.player1 else current.player2
    val opponentPlayer = if (isUserPlayer1) current.player2 else current.player1
    val isQuadMode = current.rules.mode == GameMode.QUAD_MODE
    val activeQuadPlayer = current.getCurrentPlayer()
    val activeControlPlayer = if (isQuadMode) activeQuadPlayer else userPlayer

    // Check if it's currently the local user's turn
    val isLocalTurn = when (current.rules.mode) {
        GameMode.PASS_AND_PLAY -> true
        GameMode.QUAD_MODE -> !activeQuadPlayer.isAI
        else -> current.currentTurn == localPlayerId
    }

    val legalMoves = if (isLocalTurn && settings.showLegalMoves) {
        RuleEngine.getLegalMoves(current, current.currentTurn)
    } else {
        emptyList()
    }

    val isWallValid = previewWall?.let {
        RuleEngine.isWallPlacementLegal(current, it, current.currentTurn)
    } ?: false

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(settings.theme.boardBackground)
    ) {
        Scaffold(
            containerColor = settings.theme.boardBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (current.rules.mode) {
                                GameMode.VS_AI -> "VS AI (${current.rules.aiDifficulty.name})"
                                GameMode.PASS_AND_PLAY -> Strings.get("pass_and_play", language)
                                GameMode.QUICK_MATCH -> Strings.get("quick_match", language)
                                GameMode.PUBLIC_ROOM -> "Room ${current.roomCode}"
                                GameMode.FRIEND_ROOM -> "Room ${current.roomCode}"
                                GameMode.RACE_MODE -> Strings.get("race_mode", language)
                                GameMode.QUAD_MODE -> Strings.get("quad_mode", language)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.showResignConfirm(true) }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    },
                    actions = {
                        // Flip / Rotate Board Button
                        IconButton(onClick = { isFlipped = !isFlipped }) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = "Rotate Board",
                                tint = TextPrimary
                            )
                        }
                        IconButton(onClick = { viewModel.setShowSettingsDialog(true) }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSlateBackground)
                )
            }
        ) { paddingValues ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val isLandscape = maxWidth > maxHeight && maxWidth >= 600.dp

                if (isLandscape) {
                    // Two-pane layout for tablets in landscape
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Pane: Square Board Canvas perfectly fitted to height
                        Box(
                            modifier = Modifier
                                .weight(1.15f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            GameBoardCanvas(
                                state = current,
                                isLocalTurn = isLocalTurn && current.status == GameStatus.IN_PROGRESS,
                                legalMoves = legalMoves,
                                selectedPawn = selectedPawn,
                                previewWall = previewWall,
                                isWallValid = isWallValid,
                                onCellClicked = { pos -> viewModel.onCellClicked(pos) },
                                onPawnClicked = { pid -> viewModel.onPawnClicked(pid) },
                                onWallSlotClicked = { x, y -> viewModel.onWallSlotClicked(x, y) },
                                theme = settings.theme,
                                isFlipped = isFlipped,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Floating Emote Banner on Board
                            EmoteBanner(
                                activeEmote = activeEmote,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        // Right Pane: Opponent / Quad Strip at top, User & Controls at bottom
                        Column(
                            modifier = Modifier
                                .weight(0.85f)
                                .fillMaxHeight()
                                .widthIn(max = 420.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isQuadMode) {
                                QuadPlayerStrip(
                                    state = current,
                                    language = language,
                                    theme = settings.theme
                                )
                            } else {
                                PlayerHeaderCard(
                                    player = opponentPlayer,
                                    isCurrentTurn = current.currentTurn == opponentPlayer.id,
                                    isTop = true,
                                    language = language,
                                    isLocalUser = false,
                                    theme = settings.theme
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PlayerHeaderCard(
                                    player = activeControlPlayer,
                                    isCurrentTurn = current.currentTurn == activeControlPlayer.id,
                                    isTop = false,
                                    language = language,
                                    isLocalUser = !activeControlPlayer.isAI,
                                    theme = settings.theme
                                )

                                MatchControls(
                                    isWallMode = isWallMode,
                                    wallOrientation = wallOrientation,
                                    previewWall = previewWall,
                                    isWallValid = isWallValid,
                                    remainingWalls = activeControlPlayer.remainingWalls,
                                    isLocalTurn = isLocalTurn && current.status == GameStatus.IN_PROGRESS,
                                    language = language,
                                    onToggleWallMode = { viewModel.toggleWallMode() },
                                    onToggleOrientation = { viewModel.toggleWallOrientation() },
                                    onConfirmWall = { viewModel.confirmWallPlacement() },
                                    onCancelWall = { viewModel.cancelWallPlacement() },
                                    onResignClicked = { viewModel.showResignConfirm(true) },
                                    onSendEmote = { emoji -> viewModel.sendEmote(emoji) }
                                )
                            }
                        }
                    }
                } else if (current.rules.mode == GameMode.PASS_AND_PLAY) {
                    // Local 2-Player Split Screen Layout (Face-to-Face)
                    // Red side (Player 2) facing opposite (rotated 180°), Board in middle, Blue side (Player 1) at bottom
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 540.dp)
                            .align(Alignment.Center)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Player: Player 2 (Red), Inverted 180 degrees
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .rotate(180f)
                        ) {
                            LocalPlayerControlPanel(
                                playerId = PlayerId.PLAYER_2,
                                playerState = current.player2,
                                isCurrentTurn = current.currentTurn == PlayerId.PLAYER_2,
                                isWallMode = isWallMode && current.currentTurn == PlayerId.PLAYER_2,
                                wallOrientation = wallOrientation,
                                previewWall = previewWall,
                                isWallValid = isWallValid,
                                language = language,
                                onToggleWallMode = {
                                    if (current.currentTurn == PlayerId.PLAYER_2) viewModel.toggleWallMode()
                                },
                                onToggleOrientation = {
                                    if (current.currentTurn == PlayerId.PLAYER_2) viewModel.toggleWallOrientation()
                                },
                                onConfirmWall = {
                                    if (current.currentTurn == PlayerId.PLAYER_2) viewModel.confirmWallPlacement()
                                },
                                onCancelWall = {
                                    if (current.currentTurn == PlayerId.PLAYER_2) viewModel.cancelWallPlacement()
                                },
                                onResignClicked = {
                                    viewModel.showResignConfirm(true)
                                },
                                onSendEmote = { emoji ->
                                    viewModel.sendEmote(emoji)
                                }
                            )
                        }

                        // Center: Game Board Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GameBoardCanvas(
                                state = current,
                                isLocalTurn = current.status == GameStatus.IN_PROGRESS,
                                legalMoves = legalMoves,
                                selectedPawn = selectedPawn,
                                previewWall = previewWall,
                                isWallValid = isWallValid,
                                onCellClicked = { pos -> viewModel.onCellClicked(pos) },
                                onPawnClicked = { pid -> viewModel.onPawnClicked(pid) },
                                onWallSlotClicked = { x, y -> viewModel.onWallSlotClicked(x, y) },
                                theme = settings.theme,
                                isFlipped = false,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Floating Emote Banner on Board
                            EmoteBanner(
                                activeEmote = activeEmote,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        // Bottom Player: Player 1 (Blue), Normal 0 degrees
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LocalPlayerControlPanel(
                                playerId = PlayerId.PLAYER_1,
                                playerState = current.player1,
                                isCurrentTurn = current.currentTurn == PlayerId.PLAYER_1,
                                isWallMode = isWallMode && current.currentTurn == PlayerId.PLAYER_1,
                                wallOrientation = wallOrientation,
                                previewWall = previewWall,
                                isWallValid = isWallValid,
                                language = language,
                                onToggleWallMode = {
                                    if (current.currentTurn == PlayerId.PLAYER_1) viewModel.toggleWallMode()
                                },
                                onToggleOrientation = {
                                    if (current.currentTurn == PlayerId.PLAYER_1) viewModel.toggleWallOrientation()
                                },
                                onConfirmWall = {
                                    if (current.currentTurn == PlayerId.PLAYER_1) viewModel.confirmWallPlacement()
                                },
                                onCancelWall = {
                                    if (current.currentTurn == PlayerId.PLAYER_1) viewModel.cancelWallPlacement()
                                },
                                onResignClicked = {
                                    viewModel.showResignConfirm(true)
                                },
                                onSendEmote = { emoji ->
                                    viewModel.sendEmote(emoji)
                                }
                            )
                        }
                    }
                } else {
                    // Standard Portrait / Compact Tablet & Phone
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 540.dp)
                            .align(Alignment.Center)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top: Opponent Card or Quad Strip
                        if (isQuadMode) {
                            QuadPlayerStrip(
                                state = current,
                                language = language,
                                theme = settings.theme
                            )
                        } else {
                            PlayerHeaderCard(
                                player = opponentPlayer,
                                isCurrentTurn = current.currentTurn == opponentPlayer.id,
                                isTop = true,
                                language = language,
                                isLocalUser = false,
                                theme = settings.theme
                            )
                        }

                        // Center: Game Board Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GameBoardCanvas(
                                state = current,
                                isLocalTurn = isLocalTurn && current.status == GameStatus.IN_PROGRESS,
                                legalMoves = legalMoves,
                                selectedPawn = selectedPawn,
                                previewWall = previewWall,
                                isWallValid = isWallValid,
                                onCellClicked = { pos -> viewModel.onCellClicked(pos) },
                                onPawnClicked = { pid -> viewModel.onPawnClicked(pid) },
                                onWallSlotClicked = { x, y -> viewModel.onWallSlotClicked(x, y) },
                                theme = settings.theme,
                                isFlipped = isFlipped,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Floating Emote Banner on Board
                            EmoteBanner(
                                activeEmote = activeEmote,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        // Bottom: User Card + Match Controls
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlayerHeaderCard(
                                player = activeControlPlayer,
                                isCurrentTurn = current.currentTurn == activeControlPlayer.id,
                                isTop = false,
                                language = language,
                                isLocalUser = !activeControlPlayer.isAI,
                                theme = settings.theme
                            )

                            MatchControls(
                                isWallMode = isWallMode,
                                wallOrientation = wallOrientation,
                                previewWall = previewWall,
                                isWallValid = isWallValid,
                                remainingWalls = activeControlPlayer.remainingWalls,
                                isLocalTurn = isLocalTurn && current.status == GameStatus.IN_PROGRESS,
                                language = language,
                                onToggleWallMode = { viewModel.toggleWallMode() },
                                onToggleOrientation = { viewModel.toggleWallOrientation() },
                                onConfirmWall = { viewModel.confirmWallPlacement() },
                                onCancelWall = { viewModel.cancelWallPlacement() },
                                onResignClicked = { viewModel.showResignConfirm(true) },
                                onSendEmote = { emoji -> viewModel.sendEmote(emoji) }
                            )
                        }
                    }
                }
            }
        }

        // Overlay 1: Starting Countdown
        if (current.status == GameStatus.COUNTDOWN) {
            CountdownOverlay(secondsRemaining = current.countdownSeconds)
        }

        // Overlay 2: Victory / Defeat Modal
        if (current.status == GameStatus.FINISHED) {
            VictoryDialog(
                state = current,
                language = language,
                localPlayerId = localPlayerId,
                onRematchClicked = { viewModel.startRematch() },
                onReplayClicked = { viewModel.openReplayForCurrentMatch() },
                onHomeClicked = { viewModel.navigateTo(ScreenState.HOME) }
            )
        }

        // Overlay 3: Resign Confirmation Dialog
        if (showResignDialog) {
            ResignConfirmDialog(
                language = language,
                onConfirm = { viewModel.resignMatch() },
                onDismiss = { viewModel.showResignConfirm(false) }
            )
        }
    }
}
