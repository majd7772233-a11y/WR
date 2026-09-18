package com.example.wallrush.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.wallrush.domain.titles.PlayerTitle
import com.example.wallrush.domain.titles.TitlesManager
import com.example.wallrush.ui.localization.AppLanguage
import com.example.wallrush.ui.localization.Strings
import com.example.wallrush.ui.viewmodel.ScreenState
import com.example.wallrush.ui.viewmodel.WallRushViewModel

@Composable
fun TitlesScreen(viewModel: WallRushViewModel) {
    val settings by viewModel.settings.collectAsState()
    val levelInfo by viewModel.levelInfo.collectAsState()
    val equippedTitle by viewModel.equippedTitle.collectAsState()
    val unlockedTitleIds by viewModel.unlockedTitles.collectAsState()

    val isArabic = settings.language == AppLanguage.ARABIC

    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Unlocked, 2: Locked

    val allTitles = remember { TitlesManager.ALL_TITLES }
    val filteredTitles = remember(selectedTab, unlockedTitleIds) {
        when (selectedTab) {
            1 -> allTitles.filter { it.id in unlockedTitleIds }
            2 -> allTitles.filter { it.id !in unlockedTitleIds }
            else -> allTitles
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSlateBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(ScreenState.HOME) },
                    modifier = Modifier.testTag("btn_back_titles")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Strings.get("titles", settings.language),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Current Level & Equipped Title Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.5.dp, Player1Primary.copy(alpha = 0.5f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${Strings.get("new_level", settings.language)} ${levelInfo.level}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = Player1Primary
                            )
                            val tierTitle = if (isArabic) com.example.wallrush.domain.progression.ProgressionManager.getTierTitleArabic(levelInfo.level) else levelInfo.title
                            Text(
                                text = tierTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }

                        // Equipped Title Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NeonYellow.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, NeonYellow)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = equippedTitle.icon, fontSize = 20.sp)
                                Column {
                                    Text(
                                        text = Strings.get("equipped_title", settings.language),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = if (isArabic) equippedTitle.nameAr else equippedTitle.nameEn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonYellow
                                    )
                                }
                            }
                        }
                    }

                    // XP Progress Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${levelInfo.currentXp} / ${levelInfo.xpForNextLevel} XP",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "${(levelInfo.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Player1Primary
                            )
                        }

                        LinearProgressIndicator(
                            progress = { levelInfo.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Player1Primary,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }

            // Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    if (isArabic) "الكل" else "All",
                    if (isArabic) "المفتوحة" else "Unlocked",
                    if (isArabic) "المقفلة" else "Locked"
                ).forEachIndexed { idx, label ->
                    FilterChip(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        label = { Text(text = label, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Player1Primary,
                            selectedLabelColor = Color.Black,
                            containerColor = SurfaceElevated,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            // Titles List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredTitles, key = { it.id }) { title ->
                    val isUnlocked = title.id in unlockedTitleIds
                    val isEquipped = title.id == equippedTitle.id

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isEquipped) SurfaceElevated.copy(alpha = 0.9f) else SurfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (isEquipped) NeonYellow else if (isUnlocked) Player1Primary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isUnlocked && !isEquipped) {
                                viewModel.equipTitle(title.id)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isUnlocked) Player1Primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUnlocked) {
                                    Text(text = title.icon, fontSize = 24.sp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = TextSecondary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) title.nameAr else title.nameEn,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUnlocked) TextPrimary else TextSecondary
                                )
                                Text(
                                    text = if (isArabic) title.descriptionAr else title.descriptionEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            if (isEquipped) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = NeonYellow.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, NeonYellow)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = NeonYellow,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isArabic) "مجهز" else "Equipped",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonYellow
                                        )
                                    }
                                }
                            } else if (isUnlocked) {
                                Button(
                                    onClick = { viewModel.equipTitle(title.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Player1Primary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = Strings.get("equip", settings.language),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
