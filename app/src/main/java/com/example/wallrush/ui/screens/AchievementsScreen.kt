package com.example.wallrush.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.*
import com.example.wallrush.domain.challenges.DailyChallengeManager
import com.example.wallrush.domain.model.Achievement
import com.example.wallrush.domain.model.AchievementCategory
import com.example.wallrush.domain.model.DailyChallenge
import com.example.wallrush.domain.notifications.SmartNotificationHelper
import com.example.wallrush.ui.localization.Strings
import com.example.wallrush.ui.viewmodel.ScreenState
import com.example.wallrush.ui.viewmodel.WallRushViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    viewModel: WallRushViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val language = settings.language

    val achievements by viewModel.achievements.collectAsState()
    val dailyChallenges by viewModel.dailyChallenges.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Daily Challenges, 1: Achievements
    var selectedCategoryFilter by remember { mutableStateOf<AchievementCategory?>(null) }

    // Smart notification status
    var notificationsEnabled by remember {
        mutableStateOf(SmartNotificationHelper.areNotificationsEnabled(context))
    }

    // Permission launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationsEnabled = isGranted
        SmartNotificationHelper.setNotificationsEnabled(context, isGranted)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = Strings.get("achievements_title", language),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        val unlockedCount = achievements.count { it.isUnlocked }
                        Text(
                            text = "$unlockedCount / ${achievements.size} " + Strings.get("unlocked_label", language),
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldRating
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(ScreenState.HOME) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Refresh icon
                    IconButton(onClick = {
                        viewModel.refreshAchievements()
                        viewModel.refreshDailyChallenges()
                        viewModel.soundManager.playButton()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Player1Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BoardSurface)
            )
        },
        containerColor = DeepSlateBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
            ) {
                // Tab Selection (Daily Quests vs Achievements)
                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = SurfaceCard,
                        contentColor = Player1Primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, CellBorder, RoundedCornerShape(16.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                viewModel.soundManager.playButton()
                            },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("⚔️", fontSize = 16.sp)
                                    Text(
                                        text = Strings.get("daily_challenges_title", language),
                                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                viewModel.soundManager.playButton()
                            },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("🏆", fontSize = 16.sp)
                                    Text(
                                        text = Strings.get("achievements_title", language),
                                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        )
                    }
                }

                // TAB 0: DAILY CHALLENGES
                if (selectedTab == 0) {
                    // Smart Notifications Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(Player1Primary.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.NotificationsActive,
                                                contentDescription = null,
                                                tint = Player1Primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = Strings.get("smart_notifications_title", language),
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = Strings.get("smart_notifications_desc", language),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = notificationsEnabled,
                                        onCheckedChange = { enable ->
                                            if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                                    != PackageManager.PERMISSION_GRANTED
                                                ) {
                                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                    return@Switch
                                                }
                                            }
                                            notificationsEnabled = enable
                                            SmartNotificationHelper.setNotificationsEnabled(context, enable)
                                            if (enable) {
                                                SmartNotificationHelper.showNotification(
                                                    context = context,
                                                    id = 999,
                                                    title = "🔔 " + Strings.get("smart_notifications_title", language),
                                                    body = Strings.get("notif_daily_reminder_title", language)
                                                )
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Player1Primary,
                                            checkedTrackColor = Player1Primary.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Reset Timer Badge
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.8f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CellBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("⏳", fontSize = 16.sp)
                                    Text(
                                        text = Strings.get("challenge_reset_timer", language),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                }
                                Text(
                                    text = "00:00 UTC",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldRating
                                )
                            }
                        }
                    }

                    // Daily Challenges Items
                    items(dailyChallenges, key = { it.id }) { challenge ->
                        DailyChallengeCard(
                            challenge = challenge,
                            language = language,
                            onClaim = { viewModel.claimDailyChallenge(challenge.id) },
                            onStartChallenge = { viewModel.launchDailyChallenge(challenge) },
                            onReroll = if (challenge.id == "daily_random_challenge") {
                                { viewModel.rerollRandomChallenge() }
                            } else null
                        )
                    }
                }

                // TAB 1: ALL ACHIEVEMENTS
                if (selectedTab == 1) {
                    // Category Filter Row
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedCategoryFilter == null,
                                onClick = { selectedCategoryFilter = null },
                                label = { Text(Strings.get("filter_all", language), fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = selectedCategoryFilter == AchievementCategory.COMBAT,
                                onClick = { selectedCategoryFilter = AchievementCategory.COMBAT },
                                label = { Text("⚔️ " + Strings.get("filter_combat", language), fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = selectedCategoryFilter == AchievementCategory.TACTICS,
                                onClick = { selectedCategoryFilter = AchievementCategory.TACTICS },
                                label = { Text("🧱 " + Strings.get("filter_tactics", language), fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = selectedCategoryFilter == AchievementCategory.SPEED,
                                onClick = { selectedCategoryFilter = AchievementCategory.SPEED },
                                label = { Text("🏎️ " + Strings.get("race_mode_short", language), fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = selectedCategoryFilter == AchievementCategory.MULTIPLAYER,
                                onClick = { selectedCategoryFilter = AchievementCategory.MULTIPLAYER },
                                label = { Text("📡 " + Strings.get("quad_mode_short", language), fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = selectedCategoryFilter == AchievementCategory.SECRET,
                                onClick = { selectedCategoryFilter = AchievementCategory.SECRET },
                                label = { Text("⚡ " + Strings.get("filter_secret", language), fontSize = 11.sp) }
                            )
                        }
                    }

                    val filteredAchievements = achievements.filter { ach ->
                        selectedCategoryFilter == null || ach.category == selectedCategoryFilter
                    }

                    items(filteredAchievements, key = { it.id }) { achievement ->
                        AchievementCard(
                            achievement = achievement,
                            language = language
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyChallengeCard(
    challenge: DailyChallenge,
    language: com.example.wallrush.ui.localization.AppLanguage,
    onClaim: () -> Unit,
    onStartChallenge: () -> Unit,
    onReroll: (() -> Unit)? = null
) {
    val progressFraction = (challenge.currentProgress.toFloat() / challenge.targetCount.toFloat()).coerceIn(0f, 1f)
    val isRandomChallenge = challenge.id == "daily_random_challenge"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                challenge.isCompleted && !challenge.isClaimed -> Color(0xFF132A3E)
                isRandomChallenge -> Color(0xFF1E1B4B) // Deep indigo accent for random challenge
                else -> SurfaceCard
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            when {
                challenge.isCompleted && !challenge.isClaimed -> Player1Primary
                isRandomChallenge -> Color(0xFF818CF8) // Purple glow for random challenge
                else -> CellBorder
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    challenge.isCompleted -> Player1Primary.copy(alpha = 0.25f)
                                    isRandomChallenge -> Color(0xFF6366F1).copy(alpha = 0.3f)
                                    else -> Color(0xFF0F172A)
                                }
                            )
                            .border(
                                1.5.dp,
                                when {
                                    challenge.isCompleted -> Player1Primary
                                    isRandomChallenge -> Color(0xFF818CF8)
                                    else -> CellBorder
                                },
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = challenge.icon, fontSize = 22.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = Strings.get(challenge.titleKey, language),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isRandomChallenge) {
                                Surface(
                                    color = Color(0xFF6366F1).copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "🎲 RANDOM",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFA5B4FC),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = Strings.get(challenge.descKey, language),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Reward XP Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GoldRating.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+${challenge.rewardXp} XP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldRating
                    )
                }
            }

            // Specs badges for mode, difficulty, walls, time
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode Tag
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, CellBorder)
                ) {
                    Text(
                        text = when (challenge.launchMode) {
                            com.example.wallrush.domain.model.GameMode.VS_AI -> "🎯 " + Strings.get("classic_mode", language)
                            com.example.wallrush.domain.model.GameMode.RACE_MODE -> "🏎️ " + Strings.get("race_mode_title", language)
                            com.example.wallrush.domain.model.GameMode.QUAD_MODE -> "📡 " + Strings.get("quad_mode_title", language)
                            com.example.wallrush.domain.model.GameMode.PASS_AND_PLAY -> "👥 " + Strings.get("pass_and_play", language)
                            com.example.wallrush.domain.model.GameMode.QUICK_MATCH -> "⚡ " + Strings.get("quick_match", language)
                            else -> Strings.get("classic_mode", language)
                        },
                        fontSize = 10.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // AI Difficulty Tag
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, CellBorder)
                ) {
                    Text(
                        text = "🤖 " + when (challenge.launchAiDifficulty) {
                            com.example.wallrush.domain.model.AIDifficulty.EASY -> Strings.get("diff_easy", language)
                            com.example.wallrush.domain.model.AIDifficulty.MEDIUM -> Strings.get("diff_medium", language)
                            com.example.wallrush.domain.model.AIDifficulty.HARD -> Strings.get("diff_hard", language)
                        },
                        fontSize = 10.sp,
                        color = when (challenge.launchAiDifficulty) {
                            com.example.wallrush.domain.model.AIDifficulty.HARD -> Player2Primary
                            com.example.wallrush.domain.model.AIDifficulty.MEDIUM -> GoldRating
                            com.example.wallrush.domain.model.AIDifficulty.EASY -> Player3Primary
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Walls Tag
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, CellBorder)
                ) {
                    Text(
                        text = "🧱 ${challenge.launchWallsCount}",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Opponent Name
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, CellBorder)
                ) {
                    Text(
                        text = "👤 ${challenge.opponentName}",
                        fontSize = 10.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // Progress Bar & Count
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${challenge.currentProgress} / ${challenge.targetCount}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (challenge.isCompleted) Player1Primary else TextSecondary
                    )
                    Text(
                        text = "${(progressFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (challenge.isCompleted) Player1Primary else Player3Primary,
                    trackColor = Color(0xFF0F172A)
                )
            }

            // Action Buttons
            if (challenge.isCompleted && !challenge.isClaimed) {
                Button(
                    onClick = onClaim,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Player1Primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Strings.get("claim_reward", language),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            } else if (challenge.isClaimed) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Player3Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Strings.get("claimed", language),
                        style = MaterialTheme.typography.labelMedium,
                        color = Player3Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // NOT COMPLETED: Give User One-Click Start Button (+ Re-roll if random challenge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onReroll != null) {
                        OutlinedButton(
                            onClick = onReroll,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFA5B4FC)
                            ),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("🎲", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Strings.get("reroll_challenge", language),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = onStartChallenge,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRandomChallenge) Color(0xFF4F46E5) else Player1Primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🚀", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Strings.get("start_challenge_now", language),
                            fontWeight = FontWeight.Bold,
                            color = if (isRandomChallenge) Color.White else Color(0xFF0F172A),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: Achievement,
    language: com.example.wallrush.ui.localization.AppLanguage
) {
    val progressFraction = (achievement.currentProgress.toFloat() / achievement.targetCount.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) Color(0xFF102A24) else SurfaceCard
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (achievement.isUnlocked) Player3Primary else CellBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.isUnlocked) Player3Primary.copy(alpha = 0.25f)
                        else Color(0xFF0F172A)
                    )
                    .border(
                        1.5.dp,
                        if (achievement.isUnlocked) Player3Primary else CellBorder,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = achievement.icon,
                    fontSize = 24.sp
                )
            }

            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = Strings.get(achievement.titleKey, language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (achievement.isUnlocked) Color.White else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // XP Reward Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GoldRating.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+${achievement.rewardXp} XP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = GoldRating,
                            fontSize = 10.sp
                        )
                    }
                }

                Text(
                    text = Strings.get(achievement.descKey, language),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Progress Bar
                if (!achievement.isUnlocked && achievement.targetCount > 1) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${achievement.currentProgress} / ${achievement.targetCount}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Player1Primary,
                        trackColor = Color(0xFF0F172A)
                    )
                }

                // Unlocked status tag
                if (achievement.isUnlocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Player3Primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = Strings.get("unlocked_label", language),
                            style = MaterialTheme.typography.labelSmall,
                            color = Player3Primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }
        }
    }
}
