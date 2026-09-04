package com.example.wallrush.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.wallrush.data.local.MatchRecord
import com.example.wallrush.ui.localization.Strings
import com.example.wallrush.ui.viewmodel.ScreenState
import com.example.wallrush.ui.viewmodel.WallRushViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: WallRushViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val matchHistory by viewModel.matchHistory.collectAsState()
    val language = settings.language

    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(profile.username) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepSlateBackground,
        topBar = {
            TopAppBar(
                title = { Text(Strings.get("profile", language), color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(ScreenState.HOME) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSlateBackground)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Profile Card Header
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CellBorder, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceCard
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Player1Primary.copy(alpha = 0.2f))
                                .border(2.dp, Player1Primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "User",
                                tint = Player1Primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        if (!isEditingName) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = profile.username,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                IconButton(onClick = { isEditingName = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = editedName,
                                    onValueChange = { editedName = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Button(
                                    onClick = {
                                        if (editedName.isNotBlank()) {
                                            viewModel.updateUsername(editedName, profile.avatarId)
                                            isEditingName = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Player1Primary, contentColor = Color.Black)
                                ) {
                                    Text(Strings.get("save", language))
                                }
                            }
                        }

                        // Rating badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = GoldRating.copy(alpha = 0.15f),
                            modifier = Modifier.border(1.dp, GoldRating.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "⭐", fontSize = 14.sp)
                                Text(
                                    text = "${Strings.get("rating", language)}: ${profile.ratingScore}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = GoldRating
                                )
                            }
                        }
                    }
                }
            }

            // Stats Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBox(
                        title = Strings.get("total_games", language),
                        value = "${profile.totalMatches}",
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        title = Strings.get("wins", language),
                        value = "${profile.wins}",
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        title = Strings.get("losses", language),
                        value = "${profile.losses}",
                        color = DangerRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBox(
                        title = Strings.get("win_rate", language),
                        value = "${profile.winRatePercent}%",
                        color = Player1Primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        title = Strings.get("streak", language),
                        value = "🔥 ${profile.currentStreak}",
                        color = WallWoodPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        title = Strings.get("total_walls", language),
                        value = "🧱 ${profile.wallsPlaced}",
                        color = WallWoodPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Match History Header
            item {
                Text(
                    text = Strings.get("match_history", language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            if (matchHistory.isEmpty()) {
                item {
                    Text(
                        text = "No matches played yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            } else {
                items(matchHistory) { record ->
                    MatchHistoryItem(
                        record = record,
                        language = language,
                        onReplay = { viewModel.startReplayFromRecord(record) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .border(1.dp, CellBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceCard
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MatchHistoryItem(
    record: MatchRecord,
    language: com.example.wallrush.ui.localization.AppLanguage,
    onReplay: () -> Unit
) {
    val isWin = record.winner == com.example.wallrush.domain.model.PlayerId.PLAYER_1
    val resultColor = if (isWin) SuccessGreen else DangerRed
    val resultLabel = if (isWin) "WIN" else "LOSS"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CellBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(resultColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = resultLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = resultColor
                    )
                }

                Column {
                    Text(
                        text = "vs ${record.player2Name}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${record.gameMode.name} • ${record.moveCount} moves • ${record.durationSeconds}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            IconButton(onClick = onReplay) {
                Icon(Icons.Default.PlayCircle, contentDescription = "Replay", tint = Player1Primary)
            }
        }
    }
}
