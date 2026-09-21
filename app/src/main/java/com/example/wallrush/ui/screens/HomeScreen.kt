package com.example.wallrush.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.wallrush.domain.model.AIDifficulty
import com.example.wallrush.domain.model.GameMode
import com.example.wallrush.domain.model.GameRules
import com.example.wallrush.domain.npc.NPCManager
import com.example.wallrush.domain.npc.NPCPersonality
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
    val isQuickMatchSearching by viewModel.isQuickMatchSearching.collectAsState()
    val language = settings.language
    val context = LocalContext.current

    var showQuickMatchDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    var showPassAndPlayDialog by remember { mutableStateOf(false) }

    var selectedAiDifficulty by remember { mutableStateOf(AIDifficulty.MEDIUM) }
    var aiSelectionTab by remember { mutableStateOf(0) } // 0 = Difficulty, 1 = Personality
    var selectedAiPersonality by remember { mutableStateOf(NPCPersonality.THE_RUSHER) }
    var selectedAiMode by remember { mutableStateOf(GameMode.VS_AI) }
    var selectedPassPlayMode by remember { mutableStateOf(GameMode.PASS_AND_PLAY) }
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

    // Quick Match Searching Loading Dialog (1-3 seconds animation)
    if (isQuickMatchSearching) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelQuickMatchSearch() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Player1Primary,
                        strokeWidth = 2.5.dp
                    )
                    Text(
                        text = Strings.get("searching_opponent", language),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = Strings.get("searching_opponent_desc", language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Player1Primary,
                        trackColor = CellBorder
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.cancelQuickMatchSearch() }) {
                    Text(text = Strings.get("cancel", language), color = DangerRed, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // 1. Quick Match Mode Selection Dialog
    if (showQuickMatchDialog) {
        AlertDialog(
            onDismissRequest = { showQuickMatchDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Player1Primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Quick Match",
                            tint = Player1Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = Strings.get("quick_match", language),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = Strings.get("quick_match_sub", language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    // Option 1: 1v1 Classic Quick Match
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                showQuickMatchDialog = false
                                viewModel.onQuickMatchClicked(GameMode.QUICK_MATCH)
                            }
                            .border(1.dp, Player1Primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                        color = Color(0xFF0C4A6E).copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Player1Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Strings.get("classic_mode", language),
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "1 vs 1 • " + Strings.get("quick_match", language),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Option 2: 1v1 Race Quick Match
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                showQuickMatchDialog = false
                                viewModel.onQuickMatchClicked(GameMode.RACE_MODE)
                            }
                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                        color = Color(0xFF451A03).copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Strings.get("race_mode", language),
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = Strings.get("race_mode_sub", language),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Option 3: 4 Players Quad Quick Match
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                showQuickMatchDialog = false
                                viewModel.onQuickMatchClicked(GameMode.QUAD_MODE)
                            }
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                        color = Color(0xFF064E3B).copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Strings.get("quad_mode", language),
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = Strings.get("quad_mode_sub", language),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuickMatchDialog = false }) {
                    Text(Strings.get("cancel", language), color = TextSecondary)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // 2. Play vs AI Configuration Dialog (Mode, Difficulty, Settings)
    if (showAiDialog) {
        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "AI Setup",
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = Strings.get("ai_setup_title", language),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Game Mode selection
                    Text(
                        text = Strings.get("select_game_mode", language),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple(GameMode.VS_AI, Strings.get("classic_mode_short", language), "⚔️"),
                            Triple(GameMode.RACE_MODE, Strings.get("race_mode_short", language), "🏁"),
                            Triple(GameMode.QUAD_MODE, Strings.get("quad_mode_short", language), "🎯")
                        ).forEach { (mode, label, icon) ->
                            FilterChip(
                                selected = selectedAiMode == mode,
                                onClick = { selectedAiMode = mode },
                                label = { Text("$icon $label", fontSize = 11.sp, maxLines = 1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Selection Tabs: Difficulty Levels vs AI Personalities
                    TabRow(
                        selectedTabIndex = aiSelectionTab,
                        containerColor = SurfaceCardLight,
                        contentColor = Color(0xFFA78BFA),
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = aiSelectionTab == 0,
                            onClick = { aiSelectionTab = 0 },
                            text = {
                                Text(
                                    text = Strings.get("ai_difficulties_tab", language),
                                    fontSize = 12.sp,
                                    fontWeight = if (aiSelectionTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = aiSelectionTab == 1,
                            onClick = { aiSelectionTab = 1 },
                            text = {
                                Text(
                                    text = Strings.get("ai_personalities_tab", language),
                                    fontSize = 12.sp,
                                    fontWeight = if (aiSelectionTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }

                    if (aiSelectionTab == 0) {
                        // All 6 Real Difficulty Levels
                        Text(
                            text = Strings.get("select_difficulty", language),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        // 2 rows of 3 chips each
                        val diffs = AIDifficulty.values()
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                diffs.take(3).forEach { diff ->
                                    val isSelected = selectedAiDifficulty == diff
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedAiDifficulty = diff },
                                        label = {
                                            Text(
                                                text = when (diff) {
                                                    AIDifficulty.EASY -> "🟢 " + Strings.get("easy", language)
                                                    AIDifficulty.MEDIUM -> "🟡 " + Strings.get("medium", language)
                                                    AIDifficulty.HARD -> "🔴 " + Strings.get("hard", language)
                                                    else -> diff.name
                                                },
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                diffs.drop(3).forEach { diff ->
                                    val isSelected = selectedAiDifficulty == diff
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedAiDifficulty = diff },
                                        label = {
                                            Text(
                                                text = when (diff) {
                                                    AIDifficulty.EXPERT -> "🟣 " + Strings.get("expert", language)
                                                    AIDifficulty.NIGHTMARE -> "💀 " + Strings.get("nightmare", language)
                                                    AIDifficulty.INSANE -> "🤖 " + Strings.get("insane", language)
                                                    else -> diff.name
                                                },
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        if (selectedAiDifficulty == AIDifficulty.NIGHTMARE || selectedAiDifficulty == AIDifficulty.INSANE) {
                            Surface(
                                color = Color(0xFF7C3AED).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("🧠", fontSize = 16.sp)
                                    Text(
                                        text = if (language == AppLanguage.ARABIC)
                                            "الذكاء الاصطناعي يتعلم أسلوب هروبك ويغلق مسارك المفضل تلقائياً!"
                                        else
                                            "AI actively tracks your movement patterns and cuts off your preferred escape flank!",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    } else {
                        // AI Personalities Selection
                        val personalities = NPCPersonality.values().map { persona ->
                            Triple(
                                persona,
                                "${persona.emoji} " + if (language == AppLanguage.ARABIC) persona.titleAr else persona.titleEn,
                                if (language == AppLanguage.ARABIC) persona.playStyleAr else persona.playStyleEn
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            personalities.forEach { (persona, title, desc) ->
                                val isSelected = selectedAiPersonality == persona
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedAiPersonality = persona }
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.5.dp,
                                            color = if (isSelected) Color(0xFFA78BFA) else CellBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    color = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.2f) else SurfaceCardLight,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedAiPersonality = persona },
                                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFA78BFA))
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isSelected) Color(0xFFA78BFA) else TextPrimary
                                            )
                                            Text(
                                                text = desc,
                                                fontSize = 11.sp,
                                                color = TextSecondary,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Walls Count (Selectable for all modes, default 10)
                    Text(
                        text = Strings.get("walls_count", language),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15).forEach { walls ->
                            FilterChip(
                                selected = selectedWallsCount == walls,
                                onClick = { selectedWallsCount = walls },
                                label = { Text("$walls 🧱", maxLines = 1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Time Control
                    Text(
                        text = Strings.get("time_control", language),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(Pair(180, "3m"), Pair(300, "5m"), Pair(0, "∞")).forEach { (t, label) ->
                            FilterChip(
                                selected = selectedTimeControl == t,
                                onClick = { selectedTimeControl = t },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAiDialog = false
                        if (selectedAiMode == GameMode.QUAD_MODE) {
                            viewModel.startMatch(
                                rules = GameRules(
                                    wallsPerPlayer = selectedWallsCount,
                                    timeLimitSeconds = selectedTimeControl,
                                    aiDifficulty = selectedAiDifficulty,
                                    mode = GameMode.QUAD_MODE
                                ),
                                player2Name = "Bot Alpha (Red)",
                                player2IsAI = true,
                                player3Name = "Bot Beta (Green)",
                                player3IsAI = true,
                                player4Name = "Bot Gamma (Yellow)",
                                player4IsAI = true
                            )
                        } else if (aiSelectionTab == 1) {
                            val npc = NPCManager.getOpponentWithPersonality(context, selectedAiPersonality, selectedAiDifficulty)
                            viewModel.startMatch(
                                rules = GameRules(
                                    wallsPerPlayer = selectedWallsCount,
                                    timeLimitSeconds = selectedTimeControl,
                                    aiDifficulty = npc.toAIDifficulty(),
                                    mode = selectedAiMode
                                ),
                                player2Name = npc.name,
                                player2Avatar = npc.avatarId,
                                player2IsAI = true,
                                npcProfile = npc
                            )
                        } else {
                            viewModel.startMatch(
                                rules = GameRules(
                                    wallsPerPlayer = selectedWallsCount,
                                    timeLimitSeconds = selectedTimeControl,
                                    aiDifficulty = selectedAiDifficulty,
                                    mode = selectedAiMode
                                ),
                                player2Name = if (selectedAiMode == GameMode.RACE_MODE) "AI Racer" else "AI Bot",
                                player2IsAI = true
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B5CF6),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(text = Strings.get("start_game", language), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiDialog = false }) {
                    Text(Strings.get("cancel", language), color = TextSecondary)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // 3. Pass & Play (Same Device) Configuration Dialog
    if (showPassAndPlayDialog) {
        AlertDialog(
            onDismissRequest = { showPassAndPlayDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Player2Primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = "Pass & Play",
                            tint = Player2Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = Strings.get("pass_and_play", language),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = Strings.get("select_game_mode", language),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple(GameMode.PASS_AND_PLAY, Strings.get("classic_mode_short", language), "⚔️"),
                            Triple(GameMode.RACE_MODE, Strings.get("race_mode_short", language), "🏁"),
                            Triple(GameMode.QUAD_MODE, Strings.get("quad_mode_short", language), "🎯")
                        ).forEach { (mode, label, icon) ->
                            FilterChip(
                                selected = selectedPassPlayMode == mode,
                                onClick = { selectedPassPlayMode = mode },
                                label = { Text("$icon $label", fontSize = 11.sp, maxLines = 1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text(
                        text = Strings.get("walls_count", language),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15).forEach { walls ->
                            FilterChip(
                                selected = selectedWallsCount == walls,
                                onClick = { selectedWallsCount = walls },
                                label = { Text("$walls 🧱", maxLines = 1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text(
                        text = Strings.get("time_control", language),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(Pair(180, "3m"), Pair(300, "5m"), Pair(0, "∞")).forEach { (t, label) ->
                            FilterChip(
                                selected = selectedTimeControl == t,
                                onClick = { selectedTimeControl = t },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPassAndPlayDialog = false
                        if (selectedPassPlayMode == GameMode.QUAD_MODE) {
                            viewModel.startMatch(
                                rules = GameRules(
                                    wallsPerPlayer = selectedWallsCount,
                                    timeLimitSeconds = selectedTimeControl,
                                    mode = GameMode.QUAD_MODE
                                ),
                                player2Name = "Player 2 (Red)",
                                player2IsAI = false,
                                player3Name = "Player 3 (Green)",
                                player3IsAI = false,
                                player4Name = "Player 4 (Yellow)",
                                player4IsAI = false
                            )
                        } else {
                            viewModel.startMatch(
                                rules = GameRules(
                                    wallsPerPlayer = selectedWallsCount,
                                    timeLimitSeconds = selectedTimeControl,
                                    mode = selectedPassPlayMode
                                ),
                                player2Name = "Player 2",
                                player2IsAI = false
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Player2Primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(text = Strings.get("start_game", language), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPassAndPlayDialog = false }) {
                    Text(Strings.get("cancel", language), color = TextSecondary)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSlateBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            // Header: Branding
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

                    // About App Button (Restored to top bar)
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.navigateTo(ScreenState.ABOUT) }
                            .border(1.dp, CellBorder, CircleShape),
                        shape = CircleShape,
                        color = SurfaceCard
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = Strings.get("about", language),
                                tint = Player1Primary,
                                modifier = Modifier.size(22.dp)
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

                        // Rank & Rating badge
                        val isArabic = language == AppLanguage.ARABIC
                        val rankInfo = com.example.wallrush.domain.rank.RankManager.getPlayerRankInfo(profile, isArabic)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = rankInfo.tier.primaryColor.copy(alpha = 0.15f),
                            modifier = Modifier.border(1.dp, rankInfo.tier.primaryColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = rankInfo.tier.iconEmoji, fontSize = 13.sp)
                                Text(
                                    text = "${profile.ratingScore} RP",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = rankInfo.tier.primaryColor
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
                    onClick = { showQuickMatchDialog = true },
                    onLongClick = {
                        viewModel.activateOfflineBypassGlitch()
                        showQuickMatchDialog = true
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
                    onClick = { viewModel.onPlayOnlineClicked() },
                    onLongClick = {
                        viewModel.activateOfflineBypassGlitch()
                        viewModel.onPlayOnlineClicked()
                    }
                )
            }

            // 3. Play a Friend (Room Code / P2P)
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
                MenuGameCard(
                    title = Strings.get("play_ai", language),
                    subtitle = Strings.get("play_ai_sub", language),
                    badge = "AI BOT",
                    badgeColor = Color(0xFF8B5CF6),
                    icon = Icons.Default.SmartToy,
                    gradientColors = listOf(Color(0xFF4C1D95), SurfaceCard),
                    onClick = { showAiDialog = true }
                )
            }

            // 5. Pass & Play (Local Multiplayer)
            item {
                MenuGameCard(
                    title = Strings.get("pass_and_play", language),
                    subtitle = Strings.get("pass_and_play_sub", language),
                    badge = "OFFLINE",
                    badgeColor = Player2Primary,
                    icon = Icons.Default.PhoneAndroid,
                    gradientColors = listOf(Color(0xFF831843), SurfaceCard),
                    onClick = { showPassAndPlayDialog = true }
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
                    gradientColors = listOf(Color(0xFF78350F), SurfaceCard),
                    onClick = { viewModel.navigateTo(ScreenState.TUTORIAL) }
                )
            }

            // Note: 7. Custom Maps & Level Editor is temporarily hidden per user preference without deleting underlying code.
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MenuGameCard(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(1.dp, CellBorder, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceCard
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradientColors))
                .padding(horizontal = 14.dp, vertical = 12.dp)
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = 0.2f))
                            .border(1.5.dp, badgeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = badgeColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = badgeColor,
                                    fontSize = 8.5.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Go",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
