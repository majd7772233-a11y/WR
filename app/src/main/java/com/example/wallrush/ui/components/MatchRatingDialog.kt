package com.example.wallrush.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.wallrush.domain.combo.MatchRating
import com.example.wallrush.domain.progression.LevelInfo
import com.example.wallrush.domain.titles.PlayerTitle
import com.example.wallrush.ui.localization.AppLanguage

@Composable
fun MatchRatingDialog(
    rating: MatchRating,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val isAr = language == AppLanguage.ARABIC

    val gradeColor = when (rating.rankGrade) {
        "S+", "S" -> GoldRating
        "A" -> NeonCyan
        "B" -> Color(0xFFC084FC)
        else -> TextSecondary
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isAr) "تقييم أداء المباراة 🎖️" else "Match Performance 🎖️",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Large Rank Grade & Stars
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(gradeColor.copy(alpha = 0.18f))
                        .border(2.5.dp, gradeColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rating.rankGrade,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = gradeColor
                    )
                }

                // Stars row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        val isFilled = i <= rating.stars
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (isFilled) GoldRating else CellBorder,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (rating.isPerfectRun) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = GoldRating.copy(alpha = 0.2f),
                        modifier = Modifier.border(1.dp, GoldRating, RoundedCornerShape(10.dp))
                    ) {
                        Text(
                            text = if (isAr) "✨ أداء مثالي بدون أي خطأ! ✨" else "✨ PERFECT FLAWLESS RUN! ✨",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldRating,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                // Stats breakdown cards
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCardLight)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RatingStatRow(
                        label = if (isAr) "دقة القرارات" else "Decision Accuracy",
                        value = "${rating.accuracyPercent}%"
                    )
                    RatingStatRow(
                        label = if (isAr) "أعلى كومبو سرعة" else "Max Combo Streak",
                        value = "🔥 ${rating.maxCombo}x"
                    )
                    RatingStatRow(
                        label = if (isAr) "مراوغات قريبة" else "Near Miss Dodges",
                        value = "⚡ ${rating.nearMissesCount + rating.dodgesCount}"
                    )
                    RatingStatRow(
                        label = if (isAr) "نقاط الخبرة الإضافية" else "Bonus XP Earned",
                        value = "+${rating.totalBonusXp} XP",
                        isHighlight = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Player1Primary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isAr) "موافق" else "OK",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        },
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
private fun RatingStatRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = if (isHighlight) FontWeight.Black else FontWeight.Bold,
            color = if (isHighlight) GoldRating else TextPrimary
        )
    }
}

@Composable
fun LevelUpCelebrationDialog(
    levelInfo: LevelInfo,
    newTitle: PlayerTitle?,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val isAr = language == AppLanguage.ARABIC

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "🎉", fontSize = 40.sp)
                Text(
                    text = if (isAr) "ترقية في المستوى!" else "LEVEL UP!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = GoldRating
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isAr) "وصلت إلى المستوى ${levelInfo.level}" else "You reached Level ${levelInfo.level}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )

                Text(
                    text = if (isAr) levelInfo.title else levelInfo.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Player1Primary
                )

                if (newTitle != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceCardLight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GoldRating.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isAr) "🎖️ لقب جديد مفتوح!" else "🎖️ New Title Unlocked!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldRating
                            )
                            Text(
                                text = "${newTitle.icon} ${if (isAr) newTitle.nameAr else newTitle.nameEn}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldRating,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isAr) "موافق" else "OK",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        },
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(22.dp)
    )
}
