package com.example.wallrush.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.wallrush.domain.model.PlayerId
import com.example.wallrush.domain.model.Wall
import com.example.wallrush.domain.model.WallOrientation
import com.example.wallrush.ui.localization.AppLanguage
import com.example.wallrush.ui.localization.Strings

@Composable
fun MatchControls(
    isWallMode: Boolean,
    wallOrientation: WallOrientation,
    previewWall: Wall?,
    isWallValid: Boolean,
    remainingWalls: Int,
    isLocalTurn: Boolean,
    language: AppLanguage,
    onToggleWallMode: () -> Unit,
    onToggleOrientation: () -> Unit,
    onConfirmWall: () -> Unit,
    onCancelWall: () -> Unit,
    onResignClicked: () -> Unit,
    onSendEmote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showChatPanel by remember { mutableStateOf(false) }
    var selectedChatTab by remember { mutableStateOf(0) } // 0: Emojis, 1: Quick phrases, 2: Custom text
    var customText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val allEmojis = remember {
        listOf(
            "😂", "🫡", "🤝", "👏", "🔥", "😎", "🧱", "😱", "😈", "⚡",
            "🛡️", "🎯", "🧠", "🤯", "🥳", "🥱", "💀", "🥶", "🤖", "💔",
            "⏳", "👑", "🥊", "🌪️", "🚀", "👀", "✌️", "👍", "🏆", "💥"
        )
    }

    val quickPhrases = remember(language) {
        if (language == AppLanguage.ARABIC) {
            listOf(
                "بالتوفيق يا بطل! 🤝",
                "حركة ذكية وخطيرة! 🧠",
                "لعبة أسطورية! 🔥",
                "كفووو عليك! 👏",
                "أوف الجدار قفلها! 🧱",
                "سرعة غير عادية! ⚡",
                "يا ساتر والله صعبة! 😱",
                "شكراً على اللعبة الجميلة! 🫡",
                "حركة معلم 😎",
                "ركز في القادم! 🎯"
            )
        } else {
            listOf(
                "Good luck, have fun! 🤝",
                "Nice and smart move! 🧠",
                "What an awesome game! 🔥",
                "Well played! 👏",
                "Blocked by a great wall! 🧱",
                "Incredible speed! ⚡",
                "Whoa, that was close! 😱",
                "Thanks for the game! 🫡",
                "Master move right there 😎",
                "Focused and ready! 🎯"
            )
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Expandable Chat & Emoji Box
        AnimatedVisibility(
            visible = showChatPanel,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SurfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CellBorder, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header: Tabs + Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedChatTab == 0,
                                onClick = { selectedChatTab = 0 },
                                label = { Text("😀 ${if (language == AppLanguage.ARABIC) "إيموجي" else "Emojis"}", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Player1Primary.copy(alpha = 0.2f),
                                    selectedLabelColor = Player1Primary
                                )
                            )
                            FilterChip(
                                selected = selectedChatTab == 1,
                                onClick = { selectedChatTab = 1 },
                                label = { Text("💬 ${if (language == AppLanguage.ARABIC) "عبارات" else "Phrases"}", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Player1Primary.copy(alpha = 0.2f),
                                    selectedLabelColor = Player1Primary
                                )
                            )
                            FilterChip(
                                selected = selectedChatTab == 2,
                                onClick = { selectedChatTab = 2 },
                                label = { Text("✏️ ${if (language == AppLanguage.ARABIC) "كتابة" else "Text"}", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Player1Primary.copy(alpha = 0.2f),
                                    selectedLabelColor = Player1Primary
                                )
                            )
                        }

                        IconButton(
                            onClick = { showChatPanel = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Content by Tab
                    when (selectedChatTab) {
                        0 -> {
                            // Emoji Grid (30+ emojis)
                            Box(modifier = Modifier.height(110.dp)) {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 38.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(allEmojis) { emoji ->
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    onSendEmote(emoji)
                                                    showChatPanel = false
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = emoji, fontSize = 22.sp)
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Quick Phrases (Scrollable Pills)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (phrase in quickPhrases) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = DeepSlateBackground,
                                        modifier = Modifier
                                            .border(1.dp, CellBorder, RoundedCornerShape(12.dp))
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                onSendEmote(phrase)
                                                showChatPanel = false
                                            }
                                    ) {
                                        Text(
                                            text = phrase,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Custom Text Input (Arabic & English)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customText,
                                    onValueChange = { if (it.length <= 50) customText = it },
                                    placeholder = {
                                        Text(
                                            text = if (language == AppLanguage.ARABIC) "اكتب رسالة للخصم..." else "Type message to opponent...",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            if (customText.isNotBlank()) {
                                                onSendEmote(customText.trim())
                                                customText = ""
                                                showChatPanel = false
                                                focusManager.clearFocus()
                                            }
                                        }
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Player1Primary,
                                        unfocusedBorderColor = CellBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )

                                Button(
                                    onClick = {
                                        if (customText.isNotBlank()) {
                                            onSendEmote(customText.trim())
                                            customText = ""
                                            showChatPanel = false
                                            focusManager.clearFocus()
                                        }
                                    },
                                    enabled = customText.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Player1Primary,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Primary Control Bar (Compact & Responsive 44dp height)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emote toggle button
            IconButton(
                onClick = { showChatPanel = !showChatPanel },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (showChatPanel) Player1Primary.copy(alpha = 0.2f) else SurfaceCard)
                    .border(1.dp, if (showChatPanel) Player1Primary else CellBorder, RoundedCornerShape(12.dp))
            ) {
                Text(text = "💬", fontSize = 18.sp)
            }

            // Resign button
            IconButton(
                onClick = onResignClicked,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .border(1.dp, CellBorder, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = Strings.get("resign", language),
                    tint = DangerRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Main Action Area: Wall Placement Controls
            if (!isWallMode) {
                Button(
                    onClick = onToggleWallMode,
                    enabled = isLocalTurn && remainingWalls > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WallWoodPrimary,
                        contentColor = Color.Black,
                        disabledContainerColor = SurfaceCardLight,
                        disabledContentColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "🧱", fontSize = 15.sp)
                        Text(
                            text = Strings.get("place_wall", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // In Wall Placement Mode: Orientation Switcher + Confirm/Cancel
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Orientation toggle
                    Button(
                        onClick = onToggleOrientation,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceCardLight,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RotateRight,
                                contentDescription = Strings.get("rotate_wall", language),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (wallOrientation == WallOrientation.HORIZONTAL)
                                    Strings.get("horizontal", language)
                                else
                                    Strings.get("vertical", language),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Cancel Wall
                    IconButton(
                        onClick = onCancelWall,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCardLight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = Strings.get("cancel_wall", language),
                            tint = DangerRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Confirm Wall Button
                    Button(
                        onClick = onConfirmWall,
                        enabled = previewWall != null && isWallValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen,
                            contentColor = Color.Black,
                            disabledContainerColor = SurfaceCardLight,
                            disabledContentColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = Strings.get("confirm_wall", language),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Strings.get("confirm_wall", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LocalPlayerControlPanel(
    playerId: PlayerId,
    playerState: com.example.wallrush.domain.model.PlayerState,
    isCurrentTurn: Boolean,
    isWallMode: Boolean,
    wallOrientation: WallOrientation,
    previewWall: Wall?,
    isWallValid: Boolean,
    language: AppLanguage,
    onToggleWallMode: () -> Unit,
    onToggleOrientation: () -> Unit,
    onConfirmWall: () -> Unit,
    onCancelWall: () -> Unit,
    onResignClicked: () -> Unit,
    onSendEmote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isP1 = playerId == PlayerId.PLAYER_1
    val playerAccent = if (isP1) Player1Primary else Player2Primary
    var showChatPanel by remember { mutableStateOf(false) }
    var selectedChatTab by remember { mutableStateOf(0) }
    var customText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val allEmojis = remember {
        listOf("😂", "🫡", "🤝", "👏", "🔥", "😎", "🧱", "😱", "😈", "⚡", "🎯", "🧠", "✌️", "🏆", "💥")
    }

    val quickPhrases = remember(language) {
        if (language == AppLanguage.ARABIC) {
            listOf("بالتوفيق يا بطل! 🤝", "حركة ذكية! 🧠", "لعبة أسطورية! 🔥", "كفووو! 👏", "أوف الجدار قفلها! 🧱", "حركة معلم 😎")
        } else {
            listOf("Good luck! 🤝", "Smart move! 🧠", "Awesome! 🔥", "Well played! 👏", "Nice wall! 🧱", "Master move! 😎")
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, if (isCurrentTurn) playerAccent.copy(alpha = 0.8f) else CellBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Player Info bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(playerAccent.copy(alpha = 0.2f))
                        .border(1.dp, playerAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = playerState.name,
                        tint = playerAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = playerState.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 12.sp
                )
                if (isCurrentTurn) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(playerAccent)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = Strings.get("your_turn", language),
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            fontSize = 8.sp
                        )
                    }
                }
            }

            // Wall count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(text = "🧱", fontSize = 11.sp)
                Text(
                    text = "${playerState.remainingWalls} ${Strings.get("walls_remaining", language)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = playerAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }

        // Expandable Chat/Emote
        AnimatedVisibility(
            visible = showChatPanel,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DeepSlateBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CellBorder, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = selectedChatTab == 0,
                                onClick = { selectedChatTab = 0 },
                                label = { Text("😀", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = selectedChatTab == 1,
                                onClick = { selectedChatTab = 1 },
                                label = { Text("💬", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = selectedChatTab == 2,
                                onClick = { selectedChatTab = 2 },
                                label = { Text("✏️", fontSize = 11.sp) }
                            )
                        }
                        IconButton(
                            onClick = { showChatPanel = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }

                    when (selectedChatTab) {
                        0 -> {
                            Box(modifier = Modifier.height(76.dp)) {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 32.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(allEmojis) { emoji ->
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    onSendEmote(emoji)
                                                    showChatPanel = false
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = emoji, fontSize = 18.sp)
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (phrase in quickPhrases) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SurfaceCard,
                                        modifier = Modifier
                                            .border(1.dp, CellBorder, RoundedCornerShape(8.dp))
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                onSendEmote(phrase)
                                                showChatPanel = false
                                            }
                                    ) {
                                        Text(
                                            text = phrase,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = customText,
                                    onValueChange = { if (it.length <= 40) customText = it },
                                    placeholder = {
                                        Text(text = if (language == AppLanguage.ARABIC) "اكتب رسالة..." else "Type message...", fontSize = 11.sp, color = TextSecondary)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = {
                                        if (customText.isNotBlank()) {
                                            onSendEmote(customText.trim())
                                            customText = ""
                                            focusManager.clearFocus()
                                            showChatPanel = false
                                        }
                                    }),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = playerAccent,
                                        unfocusedBorderColor = CellBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                                IconButton(
                                    onClick = {
                                        if (customText.isNotBlank()) {
                                            onSendEmote(customText.trim())
                                            customText = ""
                                            focusManager.clearFocus()
                                            showChatPanel = false
                                        }
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(playerAccent)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons Row (Resign + Emote + Wall Controls)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emote toggle button
            IconButton(
                onClick = { showChatPanel = !showChatPanel },
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (showChatPanel) playerAccent.copy(alpha = 0.2f) else DeepSlateBackground)
                    .border(1.dp, if (showChatPanel) playerAccent else CellBorder, RoundedCornerShape(10.dp))
            ) {
                Text(text = "💬", fontSize = 15.sp)
            }

            // Resign button
            IconButton(
                onClick = onResignClicked,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DeepSlateBackground)
                    .border(1.dp, CellBorder, RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = Strings.get("resign", language),
                    tint = DangerRed,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (!isWallMode) {
                // Place Wall button
                Button(
                    onClick = onToggleWallMode,
                    enabled = isCurrentTurn && playerState.remainingWalls > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WallWoodPrimary,
                        contentColor = Color.Black,
                        disabledContainerColor = SurfaceCardLight,
                        disabledContentColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🧱", fontSize = 13.sp)
                        Text(
                            text = Strings.get("place_wall", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                // In Wall Placement Mode: Orientation Switcher + Confirm/Cancel
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Rotate
                    Button(
                        onClick = onToggleOrientation,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepSlateBackground,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (wallOrientation == WallOrientation.HORIZONTAL) Strings.get("horizontal", language) else Strings.get("vertical", language),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Cancel
                    IconButton(
                        onClick = onCancelWall,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DeepSlateBackground)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                    }

                    // Confirm Wall Button
                    Button(
                        onClick = onConfirmWall,
                        enabled = previewWall != null && isWallValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen,
                            contentColor = Color.Black,
                            disabledContainerColor = SurfaceCardLight,
                            disabledContentColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = Strings.get("confirm_wall", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
