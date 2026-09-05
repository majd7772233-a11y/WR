package com.example.wallrush.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.wallrush.domain.model.AIDifficulty
import com.example.wallrush.domain.model.GameMode
import com.example.wallrush.domain.model.GameRules
import com.example.wallrush.ui.localization.AppLanguage
import com.example.wallrush.ui.localization.Strings
import com.example.wallrush.ui.viewmodel.ScreenState
import com.example.wallrush.ui.viewmodel.WallRushViewModel

@Composable
fun HomeScreen(
    viewModel: WallRushViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val showNoInternet by viewModel.showNoInternetDialog.collectAsState()
    val language = settings.language

    var showAiDifficultyPicker by remember { mutableStateOf(false) }
    var selectedAiDifficulty by remember { mutableStateOf(AIDifficulty.MEDIUM) }
    var selectedWallsCount by remember { mutableStateOf(10) }
    var selectedTimeControl by remember { mutableStateOf(300) } // 5 min

    if (showNoInternet) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNoInternetDialog() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = ErrorRed)
                    Text(
                        text = Strings.get("no_internet_title", language),
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed
                    )
                }
            },
            text = {
                Text(
                    text = Strings.get("no_internet_msg", language),
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissNoInternetDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = Player1Primary)
                ) {
                    Text(Strings.get("ok", language), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceCard
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepSlateBackground,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceCard,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { viewModel.navigateTo(ScreenState.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = Strings.get("app_name", language)) },
                    label = { Text(Strings.get("app_name", language), fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Player1Primary,
                        selectedTextColor = Player1Primary,
                        indicatorColor = Player1Primary.copy(alpha = 0.2f),
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(ScreenState.LEADERBOARD) },
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = Strings.get("leaderboard", language)) },
                    label = { Text(Strings.get("leaderboard", language)) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(ScreenState.PROFILE) },
                    icon = { Icon(Icons.Default.Person, contentDescription = Strings.get("profile", language)) },
                    label = { Text(Strings.get("profile", language)) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
            }
        }
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
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
            // Header: Branding + Language Switcher + Settings
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "🧱",
                                fontSize = 28.sp
                            )
                            Text(
                                text = Strings.get("app_name", language),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = Player1Primary,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = Strings.get("tagline", language),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Language toggle button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceCard,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val nextLang = if (language == AppLanguage.ENGLISH) AppLanguage.ARABIC else AppLanguage.ENGLISH
                                    viewModel.setLanguage(nextLang)
                                }
                                .border(1.dp, CellBorder, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (language == AppLanguage.ENGLISH) "🌐 العربية" else "🌐 EN",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        // Settings Icon
                        IconButton(
                            onClick = { viewModel.setShowSettingsDialog(true) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceCard)
                                .border(1.dp, CellBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = Strings.get("settings", language),
                                tint = TextPrimary
                            )
                        }
                    }
                }
            }

            // Quick Profile Stats Header Strip
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { viewModel.navigateTo(ScreenState.PROFILE) }
                        .border(1.dp, CellBorder, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceCard
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Player1Primary.copy(alpha = 0.2f))
                                    .border(1.5.dp, Player1Primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "User",
                                    tint = Player1Primary
                                )
                            }
                            Column {
                                Text(
                                    text = profile.username,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${profile.wins} ${Strings.get("wins", language)} • 🔥 ${profile.currentStreak} ${Strings.get("streak", language)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Rating badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = GoldRating.copy(alpha = 0.15f),
                            modifier = Modifier.border(1.dp, GoldRating.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "⭐", fontSize = 12.sp)
                                Text(
                                    text = "${profile.ratingScore}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = GoldRating
                                )
                            }
                        }
                    }
                }
            }

            // Game Mode Options
            // 1. Quick Match
            item {
                MenuGameCard(
                    title = Strings.get("quick_match", language),
                    subtitle = Strings.get("quick_match_sub", language),
                    badge = "1v1 LIVE",
                    badgeColor = Player1Primary,
                    icon = Icons.Default.Bolt,
                    gradientColors = listOf(Color(0xFF0C4A6E), SurfaceCard),
                    onClick = {
                        viewModel.onQuickMatchClicked()
                    }
                )
            }

            // 2. Play Online (Public Rooms)
            item {
                MenuGameCard(
                    title = Strings.get("play_online", language),
                    subtitle = Strings.get("play_online_sub", language),
                    badge = "LOBBY",
                    badgeColor = Color(0xFF10B981),
                    icon = Icons.Default.Public,
                    gradientColors = listOf(Color(0xFF064E3B), SurfaceCard),
                    onClick = { viewModel.onPlayOnlineClicked() }
                )
            }

            // 3. Play a Friend (Room Code)
            item {
                MenuGameCard(
                    title = Strings.get("play_friend", language),
                    subtitle = Strings.get("play_friend_sub", language),
                    badge = "CODE",
                    badgeColor = WallWoodPrimary,
                    icon = Icons.Default.Group,
                    gradientColors = listOf(Color(0xFF78350F), SurfaceCard),
                    onClick = { viewModel.navigateTo(ScreenState.PLAY_FRIEND) }
                )
            }

            // 4. Play the AI (Practice Mode)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, CellBorder, RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF4C1D95), SurfaceCard))
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.2f))
                                    .border(1.5.dp, Color(0xFF8B5CF6), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = "AI",
                                    tint = Color(0xFFA78BFA)
                                )
                            }
                            Column {
                                Text(
                                    text = Strings.get("play_ai", language),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = Strings.get("play_ai_sub", language),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Play Button
                        Button(
                            onClick = {
                                viewModel.startMatch(
                                    rules = GameRules(
                                        wallsPerPlayer = selectedWallsCount,
                                        timeLimitSeconds = selectedTimeControl,
                                        aiDifficulty = selectedAiDifficulty,
                                        mode = GameMode.VS_AI
                                    ),
                                    player2Name = "WallBot (${selectedAiDifficulty.name})",
                                    player2IsAI = true
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B5CF6),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "PLAY", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Difficulty selector tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AIDifficulty.values().forEach { diff ->
                            val isSelected = selectedAiDifficulty == diff
                            val tabColor = if (isSelected) Color(0xFF8B5CF6) else SurfaceCard
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(tabColor)
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFFA78BFA) else CellBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedAiDifficulty = diff }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (diff) {
                                        AIDifficulty.EASY -> Strings.get("easy", language)
                                        AIDifficulty.MEDIUM -> Strings.get("medium", language)
                                        AIDifficulty.HARD -> Strings.get("hard", language)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 5. Pass & Play (Local 2 Players)
            item {
                MenuGameCard(
                    title = Strings.get("pass_and_play", language),
                    subtitle = Strings.get("pass_and_play_sub", language),
                    badge = "OFFLINE",
                    badgeColor = Player2Primary,
                    icon = Icons.Default.PhoneAndroid,
                    gradientColors = listOf(Color(0xFF831843), SurfaceCard),
                    onClick = {
                        viewModel.startMatch(
                            rules = GameRules(
                                wallsPerPlayer = selectedWallsCount,
                                timeLimitSeconds = selectedTimeControl,
                                mode = GameMode.PASS_AND_PLAY
                            ),
                            player2Name = "Player 2",
                            player2IsAI = false
                        )
                    }
                )
            }

            // 6. How to Play (Interactive Tutorial)
            item {
                MenuGameCard(
                    title = Strings.get("how_to_play", language),
                    subtitle = Strings.get("how_to_play_sub", language),
                    badge = "TUTORIAL",
                    badgeColor = GoldRating,
                    icon = Icons.Default.MenuBook,
                    gradientColors = listOf(Color(0xFF1E293B), SurfaceCard),
                    onClick = { viewModel.navigateTo(ScreenState.TUTORIAL) }
                )
            }
        }
        }
    }
}

@Composable
private fun MenuGameCard(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .border(1.dp, CellBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceCard
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradientColors))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = 0.2f))
                            .border(1.5.dp, badgeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = badgeColor
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = badgeColor,
                                    fontSize = 9.sp
                                )
                            }
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Go",
                    tint = TextSecondary
                )
            }
        }
    }
}
