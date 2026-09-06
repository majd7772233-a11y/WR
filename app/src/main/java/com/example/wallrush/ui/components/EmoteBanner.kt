package com.example.wallrush.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CellBorder
import com.example.ui.theme.SurfaceCard
import com.example.wallrush.domain.model.PlayerId

data class ActiveEmote(
    val player: PlayerId,
    val emoji: String,
    val playerName: String
)

@Composable
fun EmoteBanner(
    activeEmote: ActiveEmote?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = activeEmote != null,
        enter = scaleIn(initialScale = 0.8f) + fadeIn(),
        exit = scaleOut(targetScale = 0.8f) + fadeOut(),
        modifier = modifier
    ) {
        if (activeEmote != null) {
            val isPureEmoji = activeEmote.emoji.length <= 6 && activeEmote.emoji.none { it.isLetterOrDigit() }
            val isP1 = activeEmote.player == PlayerId.PLAYER_1
            val accentColor = if (isP1) Color(0xFF00E5FF) else Color(0xFFFF2A85)

            Box(
                modifier = Modifier
                    .shadow(16.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(SurfaceCard.copy(alpha = 0.95f))
                    .border(1.5.dp, accentColor.copy(alpha = 0.8f), RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isPureEmoji) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = activeEmote.emoji, fontSize = 34.sp)
                        Column {
                            Text(
                                text = activeEmote.playerName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }
                } else {
                    // Chat speech bubble layout
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Text(text = "💬", fontSize = 22.sp)
                        Column {
                            Text(
                                text = activeEmote.playerName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = activeEmote.emoji,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
