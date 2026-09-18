package com.example.wallrush.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.wallrush.ui.components.GameBoardCanvas
import com.example.wallrush.ui.components.PlayerHeaderCard
import com.example.wallrush.ui.localization.Strings
import com.example.wallrush.ui.viewmodel.ScreenState
import com.example.wallrush.ui.viewmodel.WallRushViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplayScreen(
    viewModel: WallRushViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val replayEngine by viewModel.currentReplayEngine.collectAsState()
    val currentStep by viewModel.currentReplayStep.collectAsState()
    val isPlaying by viewModel.isReplayPlaying.collectAsState()
    val replaySpeed by viewModel.replaySpeed.collectAsState()
    val language = settings.language

    val engine = replayEngine ?: return
    val currentState = engine.getStateAtStep(currentStep)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepSlateBackground,
        topBar = {
            TopAppBar(
                title = { Text(Strings.get("replay_title", language), color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(ScreenState.HOME) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSlateBackground)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 560.dp)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Player 2 Header
            PlayerHeaderCard(
                player = currentState.player2,
                isCurrentTurn = currentState.currentTurn == com.example.wallrush.domain.model.PlayerId.PLAYER_2,
                isTop = true,
                language = language
            )

            // Board Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                GameBoardCanvas(
                    state = currentState,
                    isLocalTurn = false,
                    legalMoves = emptyList(),
                    selectedPawn = null,
                    previewWall = null,
                    isWallValid = true,
                    onCellClicked = {},
                    onPawnClicked = {},
                    onWallSlotClicked = { _, _ -> },
                    theme = settings.theme
                )
            }

            // Player 1 Header
            PlayerHeaderCard(
                player = currentState.player1,
                isCurrentTurn = currentState.currentTurn == com.example.wallrush.domain.model.PlayerId.PLAYER_1,
                isTop = false,
                language = language
            )

            // Replay Playback Controls
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CellBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceCard
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Move counter & slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${Strings.get("move_number", language)} $currentStep / ${engine.totalSteps}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        // Speed selector pills
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val speeds = listOf(0.25f, 0.5f, 1f, 2f, 4f)
                            speeds.forEach { speed ->
                                val isSelected = replaySpeed == speed
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.setReplaySpeed(speed) },
                                    color = if (isSelected) Player1Primary else SurfaceCardLight,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${if (speed == 0.25f) "0.25" else if (speed == 0.5f) "0.5" else speed.toInt().toString()}x",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                        color = if (isSelected) Color.Black else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Slider(
                        value = currentStep.toFloat(),
                        onValueChange = { viewModel.setReplayStep(it.toInt()) },
                        valueRange = 0f..maxOf(1f, engine.totalSteps.toFloat()),
                        steps = if (engine.totalSteps > 1) engine.totalSteps - 1 else 0,
                        colors = SliderDefaults.colors(
                            thumbColor = Player1Primary,
                            activeTrackColor = Player1Primary,
                            inactiveTrackColor = CellBorder
                        )
                    )

                    // Step Buttons: Start, Frame -1, Play/Pause, Frame +1, End
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.setReplayStep(0) },
                            enabled = currentStep > 0
                        ) {
                            Icon(Icons.Default.FirstPage, contentDescription = "Start", tint = TextPrimary)
                        }

                        // Frame -1
                        OutlinedButton(
                            onClick = { viewModel.setReplayStep(currentStep - 1) },
                            enabled = currentStep > 0,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Frame -1", modifier = Modifier.size(16.dp))
                                Text("-1", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        FilledIconButton(
                            onClick = { viewModel.toggleReplayPlay() },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Player1Primary, contentColor = Color.Black)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play"
                            )
                        }

                        // Frame +1
                        OutlinedButton(
                            onClick = { viewModel.setReplayStep(currentStep + 1) },
                            enabled = currentStep < engine.totalSteps,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("+1", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.ChevronRight, contentDescription = "Frame +1", modifier = Modifier.size(16.dp))
                            }
                        }

                        IconButton(
                            onClick = { viewModel.setReplayStep(engine.totalSteps) },
                            enabled = currentStep < engine.totalSteps
                        ) {
                            Icon(Icons.Default.LastPage, contentDescription = "End", tint = TextPrimary)
                        }
                    }
                }
            }
        }
        }
    }
}
