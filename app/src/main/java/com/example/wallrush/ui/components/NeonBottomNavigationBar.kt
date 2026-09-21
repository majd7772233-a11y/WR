package com.example.wallrush.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.wallrush.ui.localization.AppLanguage
import com.example.wallrush.ui.localization.Strings
import com.example.wallrush.ui.viewmodel.ScreenState

private val NeonCyan = Color(0xFF00F0FF)
private val NeonPurple = Color(0xFF8B5CF6)
private val NeonNavyBg = Color(0xFF090E17).copy(alpha = 0.95f)

data class NavDestination(
    val screen: ScreenState,
    val icon: ImageVector,
    val labelKey: String,
    val fallbackLabelEn: String,
    val fallbackLabelAr: String
)

@Composable
fun NeonBottomNavigationBar(
    currentScreen: ScreenState,
    language: AppLanguage,
    onNavigate: (ScreenState) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember {
        listOf(
            NavDestination(
                screen = ScreenState.HOME,
                icon = Icons.Default.Home,
                labelKey = "app_name",
                fallbackLabelEn = "Home",
                fallbackLabelAr = "الرئيسية"
            ),
            NavDestination(
                screen = ScreenState.PROFILE,
                icon = Icons.Default.Person,
                labelKey = "profile",
                fallbackLabelEn = "Profile",
                fallbackLabelAr = "الملف الشخصي"
            ),
            NavDestination(
                screen = ScreenState.ACHIEVEMENTS,
                icon = Icons.Default.MilitaryTech,
                labelKey = "achievements",
                fallbackLabelEn = "Quests",
                fallbackLabelAr = "المهام"
            ),
            NavDestination(
                screen = ScreenState.SETTINGS,
                icon = Icons.Default.Settings,
                labelKey = "settings",
                fallbackLabelEn = "Settings",
                fallbackLabelAr = "الإعدادات"
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .height(58.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(26.dp),
                    ambientColor = NeonCyan.copy(alpha = 0.35f),
                    spotColor = NeonPurple.copy(alpha = 0.45f)
                ),
            shape = RoundedCornerShape(26.dp),
            color = NeonNavyBg,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        NeonCyan.copy(alpha = 0.6f),
                        NeonPurple.copy(alpha = 0.5f),
                        NeonCyan.copy(alpha = 0.6f)
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentScreen == item.screen
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 0.95f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "iconScale"
                    )
                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) NeonCyan else TextSecondary.copy(alpha = 0.7f),
                        label = "iconTint"
                    )
                    val bgAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 0.18f else 0f,
                        label = "bgAlpha"
                    )

                    val label = if (language == AppLanguage.ARABIC) item.fallbackLabelAr else item.fallbackLabelEn

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(NeonCyan.copy(alpha = bgAlpha))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onNavigate(item.screen)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = label,
                                tint = iconTint,
                                modifier = Modifier
                                    .size(20.dp)
                                    .scale(iconScale)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else TextSecondary.copy(alpha = 0.7f),
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
