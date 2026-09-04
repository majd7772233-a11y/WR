package com.example.wallrush.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.wallrush.ui.localization.Strings
import com.example.wallrush.ui.viewmodel.ScreenState
import com.example.wallrush.ui.viewmodel.WallRushViewModel

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val rating: Int,
    val wins: Int,
    val streak: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: WallRushViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val language = settings.language

    val leaderboardList = remember(profile) {
        listOf(
            LeaderboardEntry(1, "GrandMaster_Z", 2480, 312, 14),
            LeaderboardEntry(2, "QuoridorTactician", 2390, 280, 9),
            LeaderboardEntry(3, "WallSniper_99", 2240, 215, 6),
            LeaderboardEntry(4, "CyberPawn", 2110, 194, 5),
            LeaderboardEntry(5, "RushBarricade", 1950, 162, 4),
            LeaderboardEntry(6, profile.username + " (You)", profile.ratingScore, profile.wins, profile.currentStreak),
            LeaderboardEntry(7, "PawnRunner", 1150, 48, 2),
            LeaderboardEntry(8, "ShadowWall", 980, 32, 1)
        ).sortedByDescending { it.rating }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepSlateBackground,
        topBar = {
            TopAppBar(
                title = { Text(Strings.get("leaderboard", language), color = TextPrimary, fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            itemsIndexed(leaderboardList) { index, item ->
                val rank = index + 1
                val isUser = item.name.contains("(You)")
                val rankBadgeColor = when (rank) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> CellBorder
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            if (isUser) 2.dp else 1.dp,
                            if (isUser) Player1Primary else CellBorder,
                            RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isUser) SurfaceCardLight else SurfaceCard
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(rankBadgeColor.copy(alpha = 0.2f))
                                    .border(1.5.dp, rankBadgeColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$rank",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = if (rank <= 3) rankBadgeColor else TextPrimary
                                )
                            }

                            Column {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isUser) FontWeight.Black else FontWeight.Bold,
                                    color = if (isUser) Player1Primary else TextPrimary
                                )
                                Text(
                                    text = "${item.wins} ${Strings.get("wins", language)} • 🔥 ${item.streak} ${Strings.get("streak", language)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Rating
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = GoldRating.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "⭐", fontSize = 12.sp)
                                Text(
                                    text = "${item.rating}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = GoldRating
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
