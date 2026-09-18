package com.example.wallrush.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.wallrush.domain.editor.CustomLevel
import com.example.wallrush.ui.localization.AppLanguage
import com.example.wallrush.ui.localization.Strings
import com.example.wallrush.ui.viewmodel.ScreenState
import com.example.wallrush.ui.viewmodel.WallRushViewModel

@Composable
fun CustomMapsScreen(viewModel: WallRushViewModel) {
    val settings by viewModel.settings.collectAsState()
    val customLevels by viewModel.customLevels.collectAsState()
    val isArabic = settings.language == AppLanguage.ARABIC

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSlateBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenState.HOME) },
                        modifier = Modifier.testTag("btn_back_custom_maps")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Strings.get("custom_maps", settings.language),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Button(
                    onClick = { viewModel.navigateTo(ScreenState.LEVEL_EDITOR) },
                    colors = ButtonDefaults.buttonColors(containerColor = Player1Primary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = Strings.get("create_level", settings.language),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Maps List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(customLevels, key = { it.id }) { level ->
                    CustomMapCard(
                        level = level,
                        isArabic = isArabic,
                        onPlay = { viewModel.startCustomLevelMatch(level) },
                        onDelete = { viewModel.deleteCustomLevel(level.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomMapCard(
    level: CustomLevel,
    isArabic: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SurfaceElevated,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = level.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = level.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NeonCyan.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${level.gridSize}x${level.gridSize}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Details tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.06f)
                ) {
                    Text(
                        text = "🪨 ${level.obstacles.size} ${if (isArabic) "أعمدة" else "Obstacles"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.06f)
                ) {
                    Text(
                        text = "🧱 ${level.wallsCount} ${if (isArabic) "جدران" else "Walls"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.06f)
                ) {
                    Text(
                        text = "🤖 ${level.aiPersonality.name.replace("THE_", "").lowercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // Play & Delete Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!level.id.startsWith("preset_")) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                    }
                }

                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = Player1Primary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isArabic) "بدء اللعب" else "Play", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
