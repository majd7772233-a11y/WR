package com.example.wallrush.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepSlateBackground
import com.example.ui.theme.TextPrimary
import com.example.wallrush.domain.engine.RuleEngine
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

    val current = state ?: return
    val language = settings.language

    // Check if it's currently the local user's turn
    val isLocalTurn = (current.currentTurn == PlayerId.PLAYER_1) ||
            (current.currentTurn == PlayerId.PLAYER_2 && current.rules.mode == com.example.wallrush.domain.model.GameMode.PASS_AND_PLAY)

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
                                com.example.wallrush.domain.model.GameMode.VS_AI -> "VS AI (${current.rules.aiDifficulty.name})"
                                com.example.wallrush.domain.model.GameMode.PASS_AND_PLAY -> Strings.get("pass_and_play", language)
                                com.example.wallrush.domain.model.GameMode.QUICK_MATCH -> Strings.get("quick_match", language)
                                com.example.wallrush.domain.model.GameMode.PUBLIC_ROOM -> "Room ${current.roomCode}"
                                com.example.wallrush.domain.model.GameMode.FRIEND_ROOM -> "Room ${current.roomCode}"
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top: Player 2 Card (Opponent)
                PlayerHeaderCard(
                    player = current.player2,
                    isCurrentTurn = current.currentTurn == PlayerId.PLAYER_2,
                    isTop = true,
                    language = language
                )

                // Center: Game Board Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(vertical = 6.dp),
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
                        theme = settings.theme
                    )

                    // Floating Emote Banner on Board
                    EmoteBanner(
                        activeEmote = activeEmote,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Bottom: Player 1 Card (User) + Match Controls
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlayerHeaderCard(
                        player = current.player1,
                        isCurrentTurn = current.currentTurn == PlayerId.PLAYER_1,
                        isTop = false,
                        language = language
                    )

                    MatchControls(
                        isWallMode = isWallMode,
                        wallOrientation = wallOrientation,
                        previewWall = previewWall,
                        isWallValid = isWallValid,
                        remainingWalls = current.getPlayer(current.currentTurn).remainingWalls,
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

        // Overlay 1: Starting Countdown
        if (current.status == GameStatus.COUNTDOWN) {
            CountdownOverlay(secondsRemaining = current.countdownSeconds)
        }

        // Overlay 2: Victory / Defeat Modal
        if (current.status == GameStatus.FINISHED) {
            VictoryDialog(
                state = current,
                language = language,
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
