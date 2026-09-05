package com.example.wallrush.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.wallrush.domain.model.PlayerId
import com.example.wallrush.domain.model.PlayerState
import com.example.wallrush.ui.localization.AppLanguage
import com.example.wallrush.ui.localization.Strings

@Composable
fun PlayerHeaderCard(
    player: PlayerState,
    isCurrentTurn: Boolean,
    isTop: Boolean,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    isLocalUser: Boolean = false,
    theme: GameTheme = GameTheme.MainCyberNeon
) {
    val isP1 = player.id == PlayerId.PLAYER_1
    val accentColor = if (isP1) theme.p1Primary else theme.p2Primary
    val containerColor = if (isCurrentTurn) SurfaceCardLight else SurfaceCard

    val infiniteTransition = rememberInfiniteTransition(label = "turnBorder")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )

    val borderColor = if (isCurrentTurn) accentColor.copy(alpha = borderAlpha) else Color.Transparent

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Avatar + Name + Turn badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f))
                        .border(2.dp, accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (player.isAI) Icons.Default.SmartToy else Icons.Default.Person,
                        contentDescription = player.name,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (isCurrentTurn) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isLocalUser) Strings.get("your_turn", language) else Strings.get("opponents_turn", language),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                    // Wall count indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🧱",
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${player.remainingWalls} ${Strings.get("walls_remaining", language)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Right: Timer Display
            if (player.timeRemainingMillis < Long.MAX_VALUE / 2) {
                val totalSec = (player.timeRemainingMillis / 1000L).toInt()
                val minutes = totalSec / 60
                val seconds = totalSec % 60
                val timeString = String.format("%02d:%02d", minutes, seconds)
                val isUrgent = isCurrentTurn && totalSec <= 30

                val timerColor by animateColorAsState(
                    targetValue = if (isUrgent) DangerRed else TextPrimary,
                    label = "timerColor"
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isUrgent) DangerRed.copy(alpha = 0.2f) else DeepSlateBackground,
                    modifier = Modifier.border(
                        1.dp,
                        if (isUrgent) DangerRed else CellBorder,
                        RoundedCornerShape(10.dp)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassBottom,
                            contentDescription = "Timer",
                            tint = timerColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = timerColor
                        )
                    }
                }
            }
        }
    }
}
