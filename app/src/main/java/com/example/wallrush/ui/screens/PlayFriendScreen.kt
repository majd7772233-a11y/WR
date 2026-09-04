package com.example.wallrush.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.wallrush.data.network.NetworkHelper
import com.example.wallrush.ui.localization.Strings
import com.example.wallrush.ui.viewmodel.P2PConnectionStatus
import com.example.wallrush.ui.viewmodel.ScreenState
import com.example.wallrush.ui.viewmodel.WallRushViewModel

enum class P2PMethodTab {
    WIFI_HOTSPOT,
    BLUETOOTH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayFriendScreen(
    viewModel: WallRushViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val p2pStatus by viewModel.p2pStatus.collectAsState()
    val localIp by viewModel.localIpAddress.collectAsState()
    val disconnectionMsg by viewModel.p2pDisconnectionMessage.collectAsState()
    val language = settings.language

    var selectedTab by remember { mutableStateOf(P2PMethodTab.WIFI_HOTSPOT) }
    var inputHostIp by remember { mutableStateOf(localIp) }
    var selectedWalls by remember { mutableStateOf(10) }
    var selectedTime by remember { mutableStateOf(300) }

    val isBluetoothOn = remember { NetworkHelper.isBluetoothEnabled(context) }

    // Disconnection Dialog
    if (disconnectionMsg != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDisconnectionDialog() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Disconnected", tint = ErrorRed)
                    Text(
                        text = Strings.get("p2p_disconnected_title", language),
                        color = ErrorRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = disconnectionMsg ?: Strings.get("p2p_disconnected_msg", language),
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissDisconnectionDialog() },
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
                title = {
                    Text(
                        text = Strings.get("play_friend", language),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.cancelP2PConnection()
                        viewModel.navigateTo(ScreenState.HOME)
                    }) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Explanation Banner
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Player1Primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCard.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Player1Primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Devices, contentDescription = "Devices", tint = Player1Primary)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Strings.get("p2p_mode_title", language),
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = Strings.get("p2p_mode_desc", language),
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Connection Method Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Wi-Fi / Hotspot Tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                viewModel.cancelP2PConnection()
                                selectedTab = P2PMethodTab.WIFI_HOTSPOT
                            },
                        color = if (selectedTab == P2PMethodTab.WIFI_HOTSPOT) Player1Primary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Wifi,
                                contentDescription = "WiFi",
                                tint = if (selectedTab == P2PMethodTab.WIFI_HOTSPOT) Color.Black else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = Strings.get("p2p_method_wifi", language),
                                color = if (selectedTab == P2PMethodTab.WIFI_HOTSPOT) Color.Black else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Bluetooth Tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                viewModel.cancelP2PConnection()
                                selectedTab = P2PMethodTab.BLUETOOTH
                            },
                        color = if (selectedTab == P2PMethodTab.BLUETOOTH) Player1Primary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Bluetooth,
                                contentDescription = "Bluetooth",
                                tint = if (selectedTab == P2PMethodTab.BLUETOOTH) Color.Black else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = Strings.get("p2p_method_bt", language),
                                color = if (selectedTab == P2PMethodTab.BLUETOOTH) Color.Black else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Tab 1: Wi-Fi & Hotspot Content
            if (selectedTab == P2PMethodTab.WIFI_HOTSPOT) {
                // Section 1: Host a Match
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CellBorder, RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        color = SurfaceCard
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.WifiTethering, contentDescription = "Host", tint = Player1Primary)
                                Text(
                                    text = Strings.get("p2p_host_title", language),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            // Device IP info
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = DeepSlateBackground
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = Strings.get("p2p_my_ip", language),
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = localIp,
                                            color = Player1Primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                    IconButton(onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("IP", localIp))
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy IP", tint = Player1Primary)
                                    }
                                }
                            }

                            // Match configuration: Walls & Time
                            if (p2pStatus != P2PConnectionStatus.LISTENING_WIFI) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedWalls == 10,
                                        onClick = { selectedWalls = 10 },
                                        label = { Text("10 " + Strings.get("walls_remaining", language)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = selectedWalls == 15,
                                        onClick = { selectedWalls = 15 },
                                        label = { Text("15 " + Strings.get("walls_remaining", language)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Button(
                                    onClick = {
                                        viewModel.startP2PHostWifi(selectedWalls, selectedTime)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Player1Primary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = Strings.get("p2p_host_btn", language),
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                // Listening state
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(color = Player1Primary, strokeWidth = 3.dp)
                                    Text(
                                        text = Strings.get("p2p_listening", language),
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.cancelP2PConnection() },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                                    ) {
                                        Text(Strings.get("cancel", language))
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Join Friend as Guest
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CellBorder, RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        color = SurfaceCard
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Login, contentDescription = "Join", tint = Player2Primary)
                                Text(
                                    text = Strings.get("p2p_join_title", language),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            OutlinedTextField(
                                value = inputHostIp,
                                onValueChange = { inputHostIp = it },
                                label = { Text(Strings.get("p2p_enter_ip", language)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Player2Primary,
                                    unfocusedBorderColor = CellBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            if (p2pStatus != P2PConnectionStatus.CONNECTING_WIFI) {
                                Button(
                                    onClick = {
                                        if (inputHostIp.isNotBlank()) {
                                            viewModel.connectP2PClientWifi(inputHostIp)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Player2Primary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = Strings.get("p2p_join_btn", language),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(color = Player2Primary, strokeWidth = 3.dp)
                                    Text(
                                        text = Strings.get("p2p_connecting", language),
                                        color = TextPrimary,
                                        fontSize = 13.sp
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.cancelP2PConnection() },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                                    ) {
                                        Text(Strings.get("cancel", language))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tab 2: Bluetooth Content
            if (selectedTab == P2PMethodTab.BLUETOOTH) {
                if (!isBluetoothOn) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = ErrorRed.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.BluetoothDisabled, contentDescription = null, tint = ErrorRed)
                                Text(
                                    text = Strings.get("p2p_bt_disabled", language),
                                    color = TextPrimary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Section 1: Bluetooth Host
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CellBorder, RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        color = SurfaceCard
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.BluetoothSearching, contentDescription = "Host BT", tint = Player1Primary)
                                Text(
                                    text = Strings.get("p2p_host_title", language),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            if (p2pStatus != P2PConnectionStatus.LISTENING_BT) {
                                Button(
                                    onClick = {
                                        viewModel.startP2PHostBluetooth(selectedWalls, selectedTime)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Player1Primary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Color.Black)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = Strings.get("p2p_host_btn", language),
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(color = Player1Primary, strokeWidth = 3.dp)
                                    Text(
                                        text = Strings.get("p2p_bt_host_listening", language),
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.cancelP2PConnection() },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                                    ) {
                                        Text(Strings.get("cancel", language))
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Paired Bluetooth Devices
                item {
                    val adapter = remember { BluetoothAdapter.getDefaultAdapter() }
                    @SuppressLint("MissingPermission")
                    val pairedDevices: List<BluetoothDevice> = remember(adapter) {
                        try {
                            adapter?.bondedDevices?.toList() ?: emptyList()
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CellBorder, RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        color = SurfaceCard
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.BluetoothConnected, contentDescription = "Paired", tint = Player2Primary)
                                Text(
                                    text = Strings.get("p2p_bt_paired", language),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            if (pairedDevices.isEmpty()) {
                                Text(
                                    text = Strings.get("p2p_bt_no_devices", language),
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            } else {
                                pairedDevices.forEach { device ->
                                    @SuppressLint("MissingPermission")
                                    val devName = try { device.name ?: "Unknown Device" } catch (e: Exception) { "Device" }
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                viewModel.connectP2PClientBluetooth(device.address)
                                            },
                                        color = DeepSlateBackground,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Player2Primary)
                                                Column {
                                                    Text(text = devName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text(text = device.address, color = TextSecondary, fontSize = 11.sp)
                                                }
                                            }
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
