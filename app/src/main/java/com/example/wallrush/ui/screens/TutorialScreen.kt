package com.example.wallrush.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.wallrush.domain.engine.GameEngine
import com.example.wallrush.domain.engine.RuleEngine
import com.example.wallrush.domain.model.*
import com.example.wallrush.ui.components.GameBoardCanvas
import com.example.wallrush.ui.localization.Strings
import com.example.wallrush.ui.viewmodel.ScreenState
import com.example.wallrush.ui.viewmodel.WallRushViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    viewModel: WallRushViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val language = settings.language

    var currentStepIndex by remember { mutableStateOf(0) }

    // Tutorial state per step
    val tutorialStates = remember {
        listOf(
            // Step 1: Basic movement
            GameState(
                player1 = PlayerState(PlayerId.PLAYER_1, "You", 0, Position(4, 7), remainingWalls = 5),
                player2 = PlayerState(PlayerId.PLAYER_2, "Bot", 1, Position(4, 1), remainingWalls = 5),
                walls = emptyList()
            ),
            // Step 2: Placing a wall
            GameState(
                player1 = PlayerState(PlayerId.PLAYER_1, "You", 0, Position(4, 6), remainingWalls = 5),
                player2 = PlayerState(PlayerId.PLAYER_2, "Bot", 1, Position(4, 2), remainingWalls = 5),
                walls = emptyList()
            ),
            // Step 3: Jumping rival
            GameState(
                player1 = PlayerState(PlayerId.PLAYER_1, "You", 0, Position(4, 4), remainingWalls = 5),
                player2 = PlayerState(PlayerId.PLAYER_2, "Bot", 1, Position(4, 3), remainingWalls = 5),
                walls = emptyList()
            ),
            // Step 4: No trapping rule
            GameState(
                player1 = PlayerState(PlayerId.PLAYER_1, "You", 0, Position(4, 5), remainingWalls = 5),
                player2 = PlayerState(PlayerId.PLAYER_2, "Bot", 1, Position(4, 0), remainingWalls = 5),
                walls = listOf(
                    Wall(x = 3, y = 0, orientation = WallOrientation.VERTICAL, placedBy = PlayerId.PLAYER_1),
                    Wall(x = 4, y = 0, orientation = WallOrientation.VERTICAL, placedBy = PlayerId.PLAYER_1)
                )
            ),
            // Step 5: Victory
            GameState(
                player1 = PlayerState(PlayerId.PLAYER_1, "You", 0, Position(4, 1), remainingWalls = 5),
                player2 = PlayerState(PlayerId.PLAYER_2, "Bot", 1, Position(2, 5), remainingWalls = 5),
                walls = emptyList()
            )
        )
    }

    var localTutState by remember(currentStepIndex) { mutableStateOf(tutorialStates[currentStepIndex]) }
    var stepCompleted by remember(currentStepIndex) { mutableStateOf(false) }

    val stepTitles = listOf(
        Strings.get("tut_step_1_title", language),
        Strings.get("tut_step_2_title", language),
        Strings.get("tut_step_3_title", language),
        Strings.get("tut_step_4_title", language),
        Strings.get("tut_step_5_title", language)
    )

    val stepDescriptions = listOf(
        Strings.get("tut_step_1_desc", language),
        Strings.get("tut_step_2_desc", language),
        Strings.get("tut_step_3_desc", language),
        Strings.get("tut_step_4_desc", language),
        Strings.get("tut_step_5_desc", language)
    )

    val legalMoves = RuleEngine.getLegalMoves(localTutState, PlayerId.PLAYER_1)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepSlateBackground,
        topBar = {
            TopAppBar(
                title = { Text(Strings.get("tutorial_title", language), color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(ScreenState.HOME) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step Card
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
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stepTitles[currentStepIndex],
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Player1Primary
                        )
                        Text(
                            text = "${currentStepIndex + 1}/5",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = stepDescriptions[currentStepIndex],
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Interactive Board
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                GameBoardCanvas(
                    state = localTutState,
                    isLocalTurn = true,
                    legalMoves = legalMoves,
                    selectedPawn = PlayerId.PLAYER_1,
                    previewWall = null,
                    isWallValid = true,
                    onCellClicked = { target ->
                        if (target in legalMoves) {
                            localTutState = GameEngine.makeMove(localTutState, target, PlayerId.PLAYER_1)
                            viewModel.soundManager.playMove()
                            stepCompleted = true
                            if (localTutState.winner == PlayerId.PLAYER_1) {
                                viewModel.soundManager.playWin()
                            }
                        } else {
                            viewModel.soundManager.playInvalid()
                        }
                    },
                    onPawnClicked = {},
                    onWallSlotClicked = { x, y ->
                        if (currentStepIndex == 1) { // Wall placement step
                            val wall = Wall(x = x, y = y, orientation = WallOrientation.HORIZONTAL, placedBy = PlayerId.PLAYER_1)
                            if (RuleEngine.isWallPlacementLegal(localTutState, wall, PlayerId.PLAYER_1)) {
                                localTutState = GameEngine.placeWall(localTutState, wall, PlayerId.PLAYER_1)
                                viewModel.soundManager.playWallPlace()
                                stepCompleted = true
                            }
                        }
                    },
                    theme = settings.theme
                )
            }

            // Bottom Navigation Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        if (currentStepIndex > 0) currentStepIndex -= 1
                    },
                    enabled = currentStepIndex > 0,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Prev")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = Strings.get("step_prev", language))
                }

                if (currentStepIndex < 4) {
                    Button(
                        onClick = { currentStepIndex += 1 },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Player1Primary, contentColor = Color.Black)
                    ) {
                        Text(text = Strings.get("step_next", language), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                    }
                } else {
                    Button(
                        onClick = { viewModel.navigateTo(ScreenState.HOME) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.Black)
                    ) {
                        Text(text = Strings.get("ready", language), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
