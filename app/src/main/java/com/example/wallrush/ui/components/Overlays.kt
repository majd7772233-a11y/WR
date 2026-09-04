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
import com.example.ui.theme.*
import com.example.wallrush.domain.model.FinishReason
import com.example.wallrush.domain.model.GameState
import com.example.wallrush.domain.model.PlayerId
import com.example.wallrush.ui.localization.AppLanguage
import com.example.wallrush.ui.localization.Strings

@Composable
fun VictoryDialog(
    state: GameState,
    language: AppLanguage,
    onRematchClicked: () -> Unit,
    onReplayClicked: () -> Unit,
    onHomeClicked: () -> Unit
) {
    val winner = state.winner ?: return
    val isP1Winner = winner == PlayerId.PLAYER_1
    val isLocalUserWin = isP1Winner // Player 1 is primary local player

    val headerText = if (isLocalUserWin) Strings.get("you_win", language) else Strings.get("you_lose", language)
    val accentColor = if (isLocalUserWin) Player1Primary else Player2Primary
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            if (isLocalUserWin) Color(0xFF0F2A4A) else Color(0xFF3B1219),
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
                        text = if (isLocalUserWin) "🏆" else "💥",
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
                        text = if (isP1Winner) state.player1.name else state.player2.name,
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
