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
import androidx.compose.ui.text.style.TextOverflow
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
    val accentColor = when (player.id) {
        PlayerId.PLAYER_1 -> theme.p1Primary
        PlayerId.PLAYER_2 -> theme.p2Primary
        PlayerId.PLAYER_3 -> Player3Primary
        PlayerId.PLAYER_4 -> Player4Primary
    }
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
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Avatar + Name + Turn badge + Walls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f))
                        .border(1.5.dp, accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = player.name,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isCurrentTurn) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(accentColor)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (isLocalUser) Strings.get("your_turn", language) else Strings.get("opponents_turn", language),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    fontSize = 8.5.sp
                                )
                            }
                        }
                    }

                    // Wall count indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "🧱",
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${player.remainingWalls} ${Strings.get("walls_remaining", language)}",
                            style = MaterialTheme.typography.labelSmall,
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
                    shape = RoundedCornerShape(8.dp),
                    color = if (isUrgent) DangerRed.copy(alpha = 0.2f) else DeepSlateBackground,
                    modifier = Modifier.border(
                        1.dp,
                        if (isUrgent) DangerRed else CellBorder,
                        RoundedCornerShape(8.dp)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassBottom,
                            contentDescription = "Timer",
                            tint = timerColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = timerColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuadPlayerStrip(
    state: com.example.wallrush.domain.model.GameState,
    language: AppLanguage,
    theme: GameTheme = GameTheme.MainCyberNeon,
    modifier: Modifier = Modifier
) {
    val players = listOfNotNull(state.player1, state.player2, state.player3, state.player4)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        players.forEach { p ->
            val isCurrent = (state.currentTurn == p.id)
            val pColor = when (p.id) {
                PlayerId.PLAYER_1 -> theme.p1Primary
                PlayerId.PLAYER_2 -> theme.p2Primary
                PlayerId.PLAYER_3 -> Player3Primary
                PlayerId.PLAYER_4 -> Player4Primary
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        width = if (isCurrent) 1.8.dp else 1.dp,
                        color = if (isCurrent) pColor else CellBorder.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                shape = RoundedCornerShape(10.dp),
                color = if (isCurrent) pColor.copy(alpha = 0.15f) else SurfaceCard
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(pColor)
                    )
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = p.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) pColor else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "🧱 ${p.remainingWalls}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

