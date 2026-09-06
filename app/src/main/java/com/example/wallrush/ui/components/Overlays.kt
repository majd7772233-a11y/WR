package com.example.wallrush.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.rotate
import com.example.ui.theme.*
import com.example.wallrush.domain.model.FinishReason
import com.example.wallrush.domain.model.GameMode
import com.example.wallrush.domain.model.GameState
import com.example.wallrush.domain.model.PlayerId
import com.example.wallrush.ui.localization.AppLanguage
import com.example.wallrush.ui.localization.Strings

@Composable
fun VictoryDialog(
    state: GameState,
    language: AppLanguage,
    localPlayerId: PlayerId = PlayerId.PLAYER_1,
    onRematchClicked: () -> Unit,
    onReplayClicked: () -> Unit,
    onHomeClicked: () -> Unit
) {
    val winner = state.winner ?: return
    val isPassAndPlay = state.rules.mode == GameMode.PASS_AND_PLAY
    val isQuadMode = state.rules.mode == GameMode.QUAD_MODE
    val isRaceMode = state.rules.mode == GameMode.RACE_MODE
    val isP1Winner = winner == PlayerId.PLAYER_1
    val isLocalUserWin = (winner == localPlayerId)
    val winningPlayer = state.getPlayer(winner)

    val headerText = if (isPassAndPlay) {
        if (isP1Winner) Strings.get("winner_player1", language) else Strings.get("winner_player2", language)
    } else if (isQuadMode) {
        when (winner) {
            PlayerId.PLAYER_1 -> Strings.get("winner_player1", language)
            PlayerId.PLAYER_2 -> Strings.get("winner_player2", language)
            PlayerId.PLAYER_3 -> Strings.get("winner_player3", language)
            PlayerId.PLAYER_4 -> Strings.get("winner_player4", language)
        }
    } else if (isLocalUserWin) {
        Strings.get("you_win", language)
    } else {
        Strings.get("you_lose", language)
    }

    val accentColor = when (winner) {
        PlayerId.PLAYER_1 -> Player1Primary
        PlayerId.PLAYER_2 -> Player2Primary
        PlayerId.PLAYER_3 -> Player3Primary
        PlayerId.PLAYER_4 -> Player4Primary
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            if (isPassAndPlay || isLocalUserWin || isQuadMode) Color(0xFF0F2A4A) else Color(0xFF3B1219),
            DeepSlateBackground
        )
    )

    val reasonText = when (state.finishReason) {
        FinishReason.GOAL_REACHED -> Strings.get("goal_reached", language)
        FinishReason.TIMEOUT -> Strings.get("time_out", language)
        FinishReason.RESIGNATION -> Strings.get("resigned_reason", language)
        FinishReason.OPPONENT_DISCONNECTED -> Strings.get("disconnected_reason", language)
        else -> ""
    }

    if (isPassAndPlay) {
        LocalSplitVictoryDialog(
            state = state,
            winner = winner,
            language = language,
            reasonText = reasonText,
            onRematchClicked = onRematchClicked,
            onHomeClicked = onHomeClicked
        )
        return
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradientBrush)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Trophy / Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f))
                        .border(2.dp, accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isPassAndPlay || isLocalUserWin) "🏆" else "💥",
                        fontSize = 36.sp
                    )
                }

                // Title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = accentColor,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = winningPlayer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (reasonText.isNotBlank()) {
                        Text(
                            text = reasonText,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Stats row
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = DeepSlateBackground.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.moveCount}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = Strings.get("move_number", language),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.walls.size}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = WallWoodPrimary
                            )
                            Text(
                                text = Strings.get("total_walls", language),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Action buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Rematch
                    Button(
                        onClick = onRematchClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)
                    ) {
                        Icon(imageVector = Icons.Default.Replay, contentDescription = Strings.get("rematch", language))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = Strings.get("rematch", language), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    // Replay
                    OutlinedButton(
                        onClick = onReplayClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, CellBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.PlayCircle, contentDescription = Strings.get("replays", language))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = Strings.get("replays", language), fontWeight = FontWeight.SemiBold)
                    }

                    // Main Menu
                    TextButton(
                        onClick = onHomeClicked,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = Strings.get("menu", language), color = TextSecondary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun LocalSplitVictoryDialog(
    state: GameState,
    winner: PlayerId,
    language: AppLanguage,
    reasonText: String,
    onRematchClicked: () -> Unit,
    onHomeClicked: () -> Unit
) {
    val isP1Winner = winner == PlayerId.PLAYER_1
    val isArabic = language == AppLanguage.ARABIC

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f)
                .border(2.dp, CellBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // TOP HALF: Inverted 180 degrees (facing Player 2 sitting opposite)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .rotate(180f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (!isP1Winner) Brush.verticalGradient(listOf(Player2Primary.copy(alpha = 0.25f), SurfaceCardLight))
                            else Brush.verticalGradient(listOf(Color(0xFF2D1016), SurfaceCardLight))
                        )
                        .border(
                            1.5.dp,
                            if (!isP1Winner) Player2Primary.copy(alpha = 0.7f) else DangerRed.copy(alpha = 0.4f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (!isP1Winner) "🏆" else "💥",
                            fontSize = 32.sp
                        )
                        Text(
                            text = if (!isP1Winner)
                                (if (isArabic) "ألف مبروك! فاز اللاعب الثاني! 🏆" else "VICTORY! PLAYER 2 WINS!")
                            else
                                (if (isArabic) "حظاً أوفر! هزيمة اللاعب الثاني" else "DEFEAT! BETTER LUCK NEXT TIME"),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = if (!isP1Winner) Player2Primary else DangerRed,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (!isP1Winner) "${state.player2.name} 👑" else state.player2.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        if (!isP1Winner && reasonText.isNotBlank()) {
                            Text(
                                text = reasonText,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // CENTER STRIP: Return to Home & Rematch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rematch button
                    OutlinedButton(
                        onClick = onRematchClicked,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Player1Primary)
                    ) {
                        Icon(imageVector = Icons.Default.Replay, contentDescription = null, tint = Player1Primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Strings.get("rematch", language),
                            fontWeight = FontWeight.Bold,
                            color = Player1Primary,
                            fontSize = 12.sp
                        )
                    }

                    // Return to Main Menu button (central user requirement)
                    Button(
                        onClick = onHomeClicked,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.Black)
                    ) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Strings.get("back_to_menu", language),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }

                // BOTTOM HALF: Standard orientation (facing Player 1 sitting normal)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isP1Winner) Brush.verticalGradient(listOf(SurfaceCardLight, Player1Primary.copy(alpha = 0.25f)))
                            else Brush.verticalGradient(listOf(SurfaceCardLight, Color(0xFF2D1016)))
                        )
                        .border(
                            1.5.dp,
                            if (isP1Winner) Player1Primary.copy(alpha = 0.7f) else DangerRed.copy(alpha = 0.4f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isP1Winner) "🏆" else "💥",
                            fontSize = 32.sp
                        )
                        Text(
                            text = if (isP1Winner)
                                (if (isArabic) "ألف مبروك! فاز اللاعب الأول! 🏆" else "VICTORY! PLAYER 1 WINS!")
                            else
                                (if (isArabic) "حظاً أوفر! هزيمة اللاعب الأول" else "DEFEAT! BETTER LUCK NEXT TIME"),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = if (isP1Winner) Player1Primary else DangerRed,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (isP1Winner) "${state.player1.name} 👑" else state.player1.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        if (isP1Winner && reasonText.isNotBlank()) {
                            Text(
                                text = reasonText,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CountdownOverlay(
    secondsRemaining: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSlateBackground.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "countdownPulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (secondsRemaining > 0) "$secondsRemaining" else "GO!",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = if (secondsRemaining > 0) Player1Primary else SuccessGreen,
                fontSize = 80.sp,
                modifier = Modifier.scale(scale)
            )
            Text(
                text = "BLOCK THEIR WAY",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 3.sp
            )
        }
    }
}

@Composable
fun ResignConfirmDialog(
    language: AppLanguage,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Strings.get("resign_confirm_title", language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Text(
                text = Strings.get("resign_confirm_desc", language),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = Strings.get("resign", language), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = Strings.get("cancel", language), color = TextSecondary)
            }
        },
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(20.dp)
    )
}
