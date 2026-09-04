package com.example.wallrush.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.example.wallrush.ui.localization.AppLanguage
import com.example.wallrush.ui.localization.Strings
import com.example.wallrush.ui.viewmodel.WallRushViewModel

@Composable
fun SettingsDialog(
    viewModel: WallRushViewModel,
    onDismiss: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val language = settings.language

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Strings.get("settings", language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Game Theme Selection
                Text(
                    text = Strings.get("game_theme", language),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(GameTheme.ALL_THEMES) { themeItem ->
                        val isSelected = settings.theme.id == themeItem.id
                        Box(
                            modifier = Modifier
                                .width(125.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) SurfaceCardLight else themeItem.boardSurface)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) themeItem.p1Primary else themeItem.cellBorderColor,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.setTheme(themeItem.id) }
                                .padding(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Color preview dots
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(themeItem.p1Primary)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(themeItem.p2Primary)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(if (themeItem.isWallPlayerSpecific) themeItem.p1WallColor else themeItem.defaultWallColor)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = Strings.get(themeItem.nameKey, language),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) themeItem.p1Primary else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Divider(color = CellBorder)

                // Language selection
                Text(
                    text = Strings.get("language", language),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.language == AppLanguage.ENGLISH,
                        onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                        label = { Text("English") }
                    )
                    FilterChip(
                        selected = settings.language == AppLanguage.ARABIC,
                        onClick = { viewModel.setLanguage(AppLanguage.ARABIC) },
                        label = { Text("العربية (Arabic)") }
                    )
                }

                Divider(color = CellBorder)

                // Sound Effects Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = Strings.get("sound_effects", language), color = TextPrimary)
                    Switch(
                        checked = settings.isSoundEnabled,
                        onCheckedChange = { viewModel.toggleSound() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Player1Primary, checkedTrackColor = Player1Dark)
                    )
                }

                // Vibration Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = Strings.get("vibration", language), color = TextPrimary)
                    Switch(
                        checked = settings.isVibrationEnabled,
                        onCheckedChange = { viewModel.toggleVibration() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Player1Primary, checkedTrackColor = Player1Dark)
                    )
                }

                // Confirm Wall Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = Strings.get("confirm_wall_toggle", language), color = TextPrimary)
                    Switch(
                        checked = settings.requireWallConfirm,
                        onCheckedChange = { viewModel.toggleRequireWallConfirm() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Player1Primary, checkedTrackColor = Player1Dark)
                    )
                }

                // Show Legal Moves Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = Strings.get("show_legal_moves", language), color = TextPrimary)
                    Switch(
                        checked = settings.showLegalMoves,
                        onCheckedChange = { viewModel.toggleShowLegalMoves() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Player1Primary, checkedTrackColor = Player1Dark)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Player1Primary, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = Strings.get("ok", language), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(20.dp)
    )
}
