package com.example.wallrush.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
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
import com.example.wallrush.domain.model.PowerUpType
import com.example.wallrush.domain.powerups.PowerUpDefinition
import com.example.wallrush.domain.powerups.PowerUpManager
import com.example.wallrush.ui.localization.AppLanguage

@Composable
fun PowerUpSelectionBar(
    currentEnergy: Int,
    isLocalTurn: Boolean,
    language: AppLanguage,
    isRewindAllowed: Boolean,
    rewindCharges: Int,
    onUsePowerUp: (PowerUpType) -> Unit,
    onRewind: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Player1Primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        color = SurfaceCard,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Energy Gauge + Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(GoldRating.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Energy",
                            tint = GoldRating,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = if (language == AppLanguage.ARABIC) "طاقة القدرات: $currentEnergy / 100" else "Energy: $currentEnergy / 100",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Energy Bar Progress
            LinearProgressIndicator(
                progress = { (currentEnergy / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = GoldRating,
                trackColor = CellBorder,
            )

            // Horizontal Scroll of PowerUps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rewind Button (Special powerup if permitted)
                if (isRewindAllowed) {
                    PowerUpCardItem(
                        icon = "⏳",
                        name = if (language == AppLanguage.ARABIC) "الرجوع بالزمن" else "Rewind",
                        costText = if (language == AppLanguage.ARABIC) "$rewindCharges متبقي" else "$rewindCharges left",
                        canAfford = rewindCharges > 0 && isLocalTurn,
                        onClick = onRewind
                    )
                }

                // Standard Power-Ups
                PowerUpManager.ALL_POWERUPS.forEach { pDef ->
                    val name = if (language == AppLanguage.ARABIC) pDef.nameAr else pDef.nameEn
                    val canAfford = currentEnergy >= pDef.energyCost && isLocalTurn
                    PowerUpCardItem(
                        icon = pDef.icon,
                        name = name,
                        costText = "⚡ ${pDef.energyCost}",
                        canAfford = canAfford,
                        onClick = { onUsePowerUp(pDef.type) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PowerUpCardItem(
    icon: String,
    name: String,
    costText: String,
    canAfford: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(88.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = canAfford) { onClick() }
            .border(
                1.dp,
                if (canAfford) Player1Primary.copy(alpha = 0.5f) else CellBorder,
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (canAfford) SurfaceCardLight else SurfaceCard.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = icon,
                fontSize = 24.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (canAfford) TextPrimary else TextSecondary.copy(alpha = 0.5f),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (canAfford) GoldRating.copy(alpha = 0.2f) else CellBorder.copy(alpha = 0.3f)
            ) {
                Text(
                    text = costText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = if (canAfford) GoldRating else TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}
