package com.example.wallrush.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
    var showEmotePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Emote Picker row (animated visibility)
        AnimatedVisibility(
            visible = showEmotePicker,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CellBorder, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val emojis = listOf("😂", "🫡", "🤝", "😡", "🔥", "👏")
                    for (emoji in emojis) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable {
                                    onSendEmote(emoji)
                                    showEmotePicker = false
                                }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 22.sp)
                        }
                    }
                }
            }
        }

        // Primary Control Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emote toggle button
            IconButton(
                onClick = { showEmotePicker = !showEmotePicker },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .border(1.dp, CellBorder, RoundedCornerShape(12.dp))
            ) {
                Text(text = "💬", fontSize = 20.sp)
            }

            // Resign button
            IconButton(
                onClick = onResignClicked,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .border(1.dp, CellBorder, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = Strings.get("resign", language),
                    tint = DangerRed,
                    modifier = Modifier.size(22.dp)
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
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "🧱", fontSize = 16.sp)
                        Text(
                            text = Strings.get("place_wall", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
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
                            .height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RotateRight,
                                contentDescription = Strings.get("rotate_wall", language),
                                modifier = Modifier.size(18.dp)
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
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCardLight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = Strings.get("cancel_wall", language),
                            tint = DangerRed
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
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = Strings.get("confirm_wall", language),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Strings.get("confirm_wall", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
