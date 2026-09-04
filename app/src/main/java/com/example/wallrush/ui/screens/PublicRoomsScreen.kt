package com.example.wallrush.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.wallrush.domain.model.GameMode
import com.example.wallrush.domain.model.GameRules
import com.example.wallrush.ui.localization.Strings
import com.example.wallrush.ui.viewmodel.PublicRoomItem
import com.example.wallrush.ui.viewmodel.ScreenState
import com.example.wallrush.ui.viewmodel.WallRushViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicRoomsScreen(
    viewModel: WallRushViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val publicRooms by viewModel.publicRooms.collectAsState()
    val showNoInternet by viewModel.showNoInternetDialog.collectAsState()
    val language = settings.language

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedWalls by remember { mutableStateOf(10) }
    var selectedTime by remember { mutableStateOf(300) }

    // No Internet Warning Dialog
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
        topBar = {
            TopAppBar(
                title = { Text(Strings.get("public_rooms", language), color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(ScreenState.HOME) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshPublicRooms() }) {
                        Icon(Icons.Default.Refresh, contentDescription = Strings.get("refresh", language), tint = Player1Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSlateBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Player1Primary,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = Strings.get("create_room", language))
                    Text(Strings.get("create_room", language), fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            if (publicRooms.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Strings.get("no_rooms_available", language),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(publicRooms) { room ->
                    PublicRoomCard(
                        room = room,
                        language = settings.language,
                        onJoin = { viewModel.joinPublicRoom(room) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    text = Strings.get("create_room", language),
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(text = Strings.get("walls_count", language), fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 15).forEach { walls ->
                            FilterChip(
                                selected = selectedWalls == walls,
                                onClick = { selectedWalls = walls },
                                label = { Text("$walls 🧱") }
                            )
                        }
                    }

                    Text(text = Strings.get("time_control", language), fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Pair(180, "3m"), Pair(300, "5m"), Pair(0, "∞")).forEach { (t, label) ->
                            FilterChip(
                                selected = selectedTime == t,
                                onClick = { selectedTime = t },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCreateDialog = false
                        viewModel.startMatch(
                            rules = GameRules(
                                wallsPerPlayer = selectedWalls,
                                timeLimitSeconds = selectedTime,
                                mode = GameMode.PUBLIC_ROOM
                            ),
                            player2Name = "Rival Player",
                            player2IsAI = true
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Player1Primary, contentColor = Color.Black)
                ) {
                    Text(text = Strings.get("create_room", language), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(text = Strings.get("cancel", language), color = TextSecondary)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun PublicRoomCard(
    room: PublicRoomItem,
    language: com.example.wallrush.ui.localization.AppLanguage,
    onJoin: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, CellBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Player1Primary.copy(alpha = 0.15f))
                        .border(1.dp, Player1Primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Public, contentDescription = "Room", tint = Player1Primary)
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = room.hostName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DeepSlateBackground
                        ) {
                            Text(
                                text = room.roomCode,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Player1Primary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "⏱ ${if (room.timeLimitSeconds == 0) "∞" else "${room.timeLimitSeconds / 60}m"} • 🧱 ${room.wallsCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        // Dynamic Ping indicator
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (room.pingMs < 50) SuccessGreen.copy(alpha = 0.2f) else Player1Primary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "📶 ${room.pingMs}ms",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                color = if (room.pingMs < 50) SuccessGreen else Player1Primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = room.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = Player1Dark,
                        fontSize = 10.sp
                    )
                }
            }

            Button(
                onClick = onJoin,
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(text = Strings.get("join_room", language), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
