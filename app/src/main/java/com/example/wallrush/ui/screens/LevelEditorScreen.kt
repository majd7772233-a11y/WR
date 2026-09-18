package com.example.wallrush.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.wallrush.domain.editor.CustomLevel
import com.example.wallrush.domain.editor.CustomMapPresets
import com.example.wallrush.domain.engine.PathFinder
import com.example.wallrush.domain.model.*
import com.example.wallrush.domain.npc.NPCPersonality
import com.example.wallrush.ui.localization.AppLanguage
import com.example.wallrush.ui.localization.Strings
import com.example.wallrush.ui.viewmodel.ScreenState
import com.example.wallrush.ui.viewmodel.WallRushViewModel
import java.util.UUID
import kotlin.random.Random

enum class EditorTool {
    OBSTACLE,
    PRESET_WALL_H,
    PRESET_WALL_V,
    P1_START,
    P2_START,
    ERASE
}

enum class BrushShape {
    SINGLE_TILE,
    BLOCK_2X2,
    CROSS_PLUS
}

enum class SymmetryMode {
    NONE,
    ROTATIONAL_180,
    VERTICAL,
    HORIZONTAL,
    QUAD_4WAY
}

enum class ArenaTheme(val displayNameAr: String, val displayNameEn: String, val primaryColor: Color, val secondaryColor: Color) {
    NEON_CYBER("سايبر بانك نيون", "Cyberpunk Neon", Color(0xFF00F0FF), Color(0xFF0C4A6E)),
    MAGMA_FORGE("حمم بركانية", "Magma Forge", Color(0xFFFF4B4B), Color(0xFF7F1D1D)),
    ANCIENT_TEMPLE("معبد أثري ذهبي", "Ancient Temple", Color(0xFFFBBF24), Color(0xFF78350F)),
    GLACIER_FROST("جليد متجمد", "Glacier Frost", Color(0xFF38BDF8), Color(0xFF0369A1)),
    EMERALD_GROVE("واحة الزمرد", "Emerald Grove", Color(0xFF10B981), Color(0xFF064E3B))
}

enum class EditorTab {
    CANVAS,
    RULES_AI,
    PRESETS,
    GENERATOR
}

private data class EditorSnapshot(
    val obstacles: Set<Obstacle>,
    val presetWalls: Set<Wall>,
    val p1Start: Position,
    val p2Start: Position,
    val gridSize: Int
)

@Composable
fun LevelEditorScreen(viewModel: WallRushViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val settings by viewModel.settings.collectAsState()
    val isArabic = settings.language == AppLanguage.ARABIC

    var currentTab by remember { mutableStateOf(EditorTab.CANVAS) }
    var mapName by remember { mutableStateOf(if (isArabic) "مرحلة تكتيكية مخصصة" else "Custom Tactical Arena") }
    var mapDescription by remember { mutableStateOf(if (isArabic) "مرحلة مصممة بواسطة المحترف" else "Player-created custom arena") }
    var gridSize by remember { mutableIntStateOf(9) }
    var selectedTool by remember { mutableStateOf(EditorTool.OBSTACLE) }
    var brushShape by remember { mutableStateOf(BrushShape.SINGLE_TILE) }
    var symmetryMode by remember { mutableStateOf(SymmetryMode.ROTATIONAL_180) }
    var arenaTheme by remember { mutableStateOf(ArenaTheme.NEON_CYBER) }

    var wallsPerPlayer by remember { mutableIntStateOf(10) }
    var timeLimitSeconds by remember { mutableIntStateOf(120) }
    var selectedPersonality by remember { mutableStateOf(NPCPersonality.THE_PREDICTOR) }
    var selectedDifficulty by remember { mutableStateOf(AIDifficulty.EXPERT) }

    var p1Start by remember { mutableStateOf(Position(4, 8)) }
    var p2Start by remember { mutableStateOf(Position(4, 0)) }
    var obstacles by remember { mutableStateOf<Set<Obstacle>>(emptySet()) }
    var presetWalls by remember { mutableStateOf<Set<Wall>>(emptySet()) }

    // Undo / Redo History Stacks
    val undoStack = remember { mutableStateListOf<EditorSnapshot>() }
    val redoStack = remember { mutableStateListOf<EditorSnapshot>() }

    fun pushHistory() {
        undoStack.add(EditorSnapshot(obstacles, presetWalls, p1Start, p2Start, gridSize))
        if (undoStack.size > 30) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    fun applyUndo() {
        if (undoStack.isNotEmpty()) {
            val last = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(EditorSnapshot(obstacles, presetWalls, p1Start, p2Start, gridSize))
            obstacles = last.obstacles
            presetWalls = last.presetWalls
            p1Start = last.p1Start
            p2Start = last.p2Start
            gridSize = last.gridSize
        }
    }

    fun applyRedo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(EditorSnapshot(obstacles, presetWalls, p1Start, p2Start, gridSize))
            obstacles = next.obstacles
            presetWalls = next.presetWalls
            p1Start = next.p1Start
            p2Start = next.p2Start
            gridSize = next.gridSize
        }
    }

    var showImportDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    // Real-time Path & Fairness Validation
    val p1Goal = remember(p1Start, gridSize) { if (p1Start.y > gridSize / 2) 0 else gridSize - 1 }
    val p2Goal = remember(p2Start, gridSize) { if (p2Start.y < gridSize / 2) gridSize - 1 else 0 }

    val p1Distance by remember(p1Start, p1Goal, presetWalls, obstacles, gridSize) {
        derivedStateOf {
            PathFinder.shortestDistanceToGoal(p1Start, p1Goal, presetWalls.toList(), gridSize, obstacles.toList())
        }
    }
    val p2Distance by remember(p2Start, p2Goal, presetWalls, obstacles, gridSize) {
        derivedStateOf {
            PathFinder.shortestDistanceToGoal(p2Start, p2Goal, presetWalls.toList(), gridSize, obstacles.toList())
        }
    }
    val isPathValid = p1Distance >= 0 && p2Distance >= 0 && p1Start != p2Start
    val pathDelta = kotlin.math.abs(p1Distance - p2Distance)

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
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Bar with Undo / Redo and Action Hub
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenState.HOME) },
                        modifier = Modifier.testTag("btn_back_editor")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = if (isArabic) "محرر الخرائط الأسطوري 🛠️" else "Pro Level Studio 🛠️",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = arenaTheme.primaryColor
                        )
                        Text(
                            text = if (isArabic) "صمم وعدّل كل شيء باحترافية كاملة" else "Advanced arena creator & balance suite",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Undo
                    IconButton(
                        onClick = { applyUndo() },
                        enabled = undoStack.isNotEmpty(),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (undoStack.isNotEmpty()) arenaTheme.primaryColor else TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Redo
                    IconButton(
                        onClick = { applyRedo() },
                        enabled = redoStack.isNotEmpty(),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (redoStack.isNotEmpty()) arenaTheme.primaryColor else TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Import JSON
                    IconButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Import", tint = arenaTheme.primaryColor, modifier = Modifier.size(18.dp))
                    }

                    // Export / Share JSON
                    IconButton(
                        onClick = {
                            val lvl = CustomLevel(
                                id = UUID.randomUUID().toString().take(8),
                                name = mapName,
                                description = mapDescription,
                                gridSize = gridSize,
                                p1Start = p1Start,
                                p2Start = p2Start,
                                obstacles = obstacles.toList(),
                                presetWalls = presetWalls.toList(),
                                wallsCount = wallsPerPlayer,
                                timeLimitSeconds = timeLimitSeconds,
                                aiPersonality = selectedPersonality,
                                aiDifficulty = selectedDifficulty
                            )
                            val json = CustomMapPresets.serializeToJson(lvl)
                            clipboardManager.setText(AnnotatedString(json))
                            Toast.makeText(context, if (isArabic) "تم نسخ كود المرحلة للحافظة! 📋" else "Map code copied! 📋", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export", tint = GoldRating, modifier = Modifier.size(18.dp))
                    }

                    // Clear All
                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DangerRed.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = DangerRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Top Mode Navigation Tabs (Canvas / Generator / Rules & AI / Presets)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        Triple(EditorTab.CANVAS, "🗺️", if (isArabic) "الرسم" else "Canvas"),
                        Triple(EditorTab.GENERATOR, "✨", if (isArabic) "التوليد الذكي" else "Generator"),
                        Triple(EditorTab.RULES_AI, "⚙️", if (isArabic) "القواعد والذكاء" else "Rules & AI"),
                        Triple(EditorTab.PRESETS, "🏰", if (isArabic) "النماذج" else "Presets")
                    ).forEach { (tab, icon, label) ->
                        val isSelected = currentTab == tab
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) arenaTheme.primaryColor else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { currentTab = tab }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(icon, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            when (currentTab) {
                EditorTab.CANVAS -> {
                    // Map Name Input
                    OutlinedTextField(
                        value = mapName,
                        onValueChange = { mapName = it },
                        label = { Text(if (isArabic) "اسم المرحلة" else "Arena Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = arenaTheme.primaryColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedLabelColor = arenaTheme.primaryColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Grid Size Selector & Symmetry Selector
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceElevated)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Grid Size
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📐 " + Strings.get("map_size", settings.language),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(5, 7, 9, 11).forEach { size ->
                                    FilterChip(
                                        selected = gridSize == size,
                                        onClick = {
                                            pushHistory()
                                            gridSize = size
                                            p1Start = Position(size / 2, size - 1)
                                            p2Start = Position(size / 2, 0)
                                            obstacles = obstacles.filter { it.x < size && it.y < size }.toSet()
                                            presetWalls = presetWalls.filter { it.x < size - 1 && it.y < size - 1 }.toSet()
                                        },
                                        label = { Text("${size}x${size}", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = arenaTheme.primaryColor,
                                            selectedLabelColor = Color.Black,
                                            containerColor = DeepSlateBackground,
                                            labelColor = TextSecondary
                                        ),
                                        modifier = Modifier.height(30.dp)
                                    )
                                }
                            }
                        }

                        // Symmetry Mode Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🪞 " + if (isArabic) "نمط التماثل" else "Symmetry Mode",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(SymmetryMode.values()) { mode ->
                                    val isSel = symmetryMode == mode
                                    val label = when (mode) {
                                        SymmetryMode.NONE -> if (isArabic) "حر" else "None"
                                        SymmetryMode.ROTATIONAL_180 -> if (isArabic) "دوراني 180°" else "180° Spin"
                                        SymmetryMode.VERTICAL -> if (isArabic) "عمودي" else "Vertical"
                                        SymmetryMode.HORIZONTAL -> if (isArabic) "أفقي" else "Horizontal"
                                        SymmetryMode.QUAD_4WAY -> if (isArabic) "رباعي" else "4-Way"
                                    }
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { symmetryMode = mode },
                                        label = { Text(label, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = GoldRating,
                                            selectedLabelColor = Color.Black,
                                            containerColor = DeepSlateBackground,
                                            labelColor = TextSecondary
                                        ),
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                        }

                        // Theme Customizer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎨 " + if (isArabic) "ثيم الساحة" else "Arena Theme",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(ArenaTheme.values()) { th ->
                                    val isSel = arenaTheme == th
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { arenaTheme = th },
                                        label = { Text(if (isArabic) th.displayNameAr else th.displayNameEn, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = th.primaryColor,
                                            selectedLabelColor = Color.Black,
                                            containerColor = DeepSlateBackground,
                                            labelColor = TextSecondary
                                        ),
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Real-Time Path Safety & Distance Balance Analyzer Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPathValid) {
                            if (pathDelta == 0) Color(0xFF064E3B).copy(alpha = 0.7f) else Color(0xFF78350F).copy(alpha = 0.7f)
                        } else DangerRed.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, if (isPathValid) (if (pathDelta == 0) Color(0xFF10B981) else GoldRating) else DangerRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(if (isPathValid) (if (pathDelta == 0) "⚖️" else "⚡") else "⚠️", fontSize = 18.sp)
                            Column {
                                Text(
                                    text = if (isPathValid) {
                                        if (pathDelta == 0) {
                                            if (isArabic) "توازن مثالي 100% (P1: $p1Distance خطوات • P2: $p2Distance خطوات)"
                                            else "100% Fair Balance (P1: $p1Distance steps • P2: $p2Distance steps)"
                                        } else {
                                            if (isArabic) "مسار سالك مع فارق $pathDelta خطوات (P1: $p1Distance • P2: $p2Distance)"
                                            else "Paths verified safe with $pathDelta step delta (P1: $p1Distance • P2: $p2Distance)"
                                        }
                                    } else {
                                        if (isArabic) "تنبيه: مسار أحد اللاعبين مسدود أو البدايات متطابقة!"
                                        else "Warning: Goal is unreachable or start positions clash!"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Black,
                                    color = if (isPathValid) (if (pathDelta == 0) Color(0xFF34D399) else GoldRating) else DangerRed,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = if (isPathValid) (if (isArabic) "الساحة جاهزة للعب التنافسي المباشر" else "Ready for competitive play") else (if (isArabic) "افتح مساراً للوصول للهدف" else "Clear a path to goal"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Tools Palette & Brush Selector
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceElevated)
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Main Tools
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            listOf(
                                Triple(EditorTool.OBSTACLE, "🪨", if (isArabic) "عائق" else "Pillar"),
                                Triple(EditorTool.PRESET_WALL_H, "🧱H", if (isArabic) "جدار H" else "Wall H"),
                                Triple(EditorTool.PRESET_WALL_V, "🧱V", if (isArabic) "جدار V" else "Wall V"),
                                Triple(EditorTool.P1_START, "🔵", if (isArabic) "P1" else "P1 Start"),
                                Triple(EditorTool.P2_START, "🔴", if (isArabic) "P2" else "P2 Start"),
                                Triple(EditorTool.ERASE, "🧹", if (isArabic) "مسح" else "Erase")
                            ).forEach { (tool, icon, label) ->
                                val isSelected = selectedTool == tool
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) arenaTheme.primaryColor else Color.Transparent,
                                    border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier.clickable { selectedTool = tool }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = icon, fontSize = 15.sp)
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.Black else TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Brush Shapes
                        if (selectedTool == EditorTool.OBSTACLE || selectedTool == EditorTool.ERASE) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isArabic) "حجم الفرشاة: " else "Brush: ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                listOf(
                                    Pair(BrushShape.SINGLE_TILE, if (isArabic) "1x1 خلية" else "1x1 Single"),
                                    Pair(BrushShape.BLOCK_2X2, if (isArabic) "2x2 مربع" else "2x2 Block"),
                                    Pair(BrushShape.CROSS_PLUS, if (isArabic) "+ صليب" else "+ Cross")
                                ).forEach { (shape, label) ->
                                    val isSel = brushShape == shape
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) arenaTheme.primaryColor.copy(alpha = 0.2f) else DeepSlateBackground,
                                        border = BorderStroke(1.dp, if (isSel) arenaTheme.primaryColor else CellBorder),
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .clickable { brushShape = shape }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) arenaTheme.primaryColor else TextSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Interactive Grid Canvas
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = BoardBackground,
                        border = BorderStroke(1.5.dp, arenaTheme.primaryColor.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (y in 0 until gridSize) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    for (x in 0 until gridSize) {
                                        val isP1 = p1Start.x == x && p1Start.y == y
                                        val isP2 = p2Start.x == x && p2Start.y == y
                                        val hasObstacle = obstacles.any { it.x == x && it.y == y }
                                        val hWall = presetWalls.any { it.x == x && it.y == y && it.orientation == WallOrientation.HORIZONTAL }
                                        val vWall = presetWalls.any { it.x == x && it.y == y && it.orientation == WallOrientation.VERTICAL }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(2.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (hasObstacle) Color(0xFF64748B)
                                                    else if (isP1) arenaTheme.primaryColor.copy(alpha = 0.85f)
                                                    else if (isP2) Player2Primary.copy(alpha = 0.85f)
                                                    else SurfaceElevated
                                                )
                                                .border(
                                                    1.dp,
                                                    if (hWall || vWall) GoldRating else Color.White.copy(alpha = 0.08f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .clickable {
                                                    pushHistory()

                                                    // Calculate targets based on BrushShape
                                                    val targetCoords = mutableListOf<Pair<Int, Int>>()
                                                    when (brushShape) {
                                                        BrushShape.SINGLE_TILE -> targetCoords.add(Pair(x, y))
                                                        BrushShape.BLOCK_2X2 -> {
                                                            for (dx in 0..1) {
                                                                for (dy in 0..1) {
                                                                    if (x + dx < gridSize && y + dy < gridSize) {
                                                                        targetCoords.add(Pair(x + dx, y + dy))
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        BrushShape.CROSS_PLUS -> {
                                                            targetCoords.add(Pair(x, y))
                                                            if (x + 1 < gridSize) targetCoords.add(Pair(x + 1, y))
                                                            if (x - 1 >= 0) targetCoords.add(Pair(x - 1, y))
                                                            if (y + 1 < gridSize) targetCoords.add(Pair(x, y + 1))
                                                            if (y - 1 >= 0) targetCoords.add(Pair(x, y - 1))
                                                        }
                                                    }

                                                    fun getSymmetricPositions(cx: Int, cy: Int): List<Pair<Int, Int>> {
                                                        val list = mutableListOf<Pair<Int, Int>>()
                                                        when (symmetryMode) {
                                                            SymmetryMode.NONE -> {}
                                                            SymmetryMode.ROTATIONAL_180 -> {
                                                                list.add(Pair(gridSize - 1 - cx, gridSize - 1 - cy))
                                                            }
                                                            SymmetryMode.VERTICAL -> {
                                                                list.add(Pair(gridSize - 1 - cx, cy))
                                                            }
                                                            SymmetryMode.HORIZONTAL -> {
                                                                list.add(Pair(cx, gridSize - 1 - cy))
                                                            }
                                                            SymmetryMode.QUAD_4WAY -> {
                                                                list.add(Pair(gridSize - 1 - cx, cy))
                                                                list.add(Pair(cx, gridSize - 1 - cy))
                                                                list.add(Pair(gridSize - 1 - cx, gridSize - 1 - cy))
                                                            }
                                                        }
                                                        return list
                                                    }

                                                    when (selectedTool) {
                                                        EditorTool.OBSTACLE -> {
                                                            val allTargets = mutableSetOf<Pair<Int, Int>>()
                                                            targetCoords.forEach { (tx, ty) ->
                                                                allTargets.add(Pair(tx, ty))
                                                                allTargets.addAll(getSymmetricPositions(tx, ty))
                                                            }
                                                            val toAdd = mutableSetOf<Obstacle>()
                                                            val toRemove = mutableSetOf<Obstacle>()
                                                            allTargets.forEach { (tx, ty) ->
                                                                if (Position(tx, ty) != p1Start && Position(tx, ty) != p2Start) {
                                                                    val existing = obstacles.find { it.x == tx && it.y == ty }
                                                                    if (existing != null) toRemove.add(existing)
                                                                    else toAdd.add(Obstacle(tx, ty))
                                                                }
                                                            }
                                                            obstacles = (obstacles - toRemove) + toAdd
                                                        }
                                                        EditorTool.PRESET_WALL_H -> {
                                                            if (x < gridSize - 1 && y < gridSize - 1) {
                                                                val existing = presetWalls.find { it.x == x && it.y == y && it.orientation == WallOrientation.HORIZONTAL }
                                                                if (existing != null) {
                                                                    presetWalls = presetWalls - existing
                                                                    if (symmetryMode == SymmetryMode.ROTATIONAL_180) {
                                                                        val symX = gridSize - 2 - x
                                                                        val symY = gridSize - 2 - y
                                                                        if (symX in 0 until gridSize - 1 && symY in 0 until gridSize - 1) {
                                                                            presetWalls = presetWalls.filterNot { it.x == symX && it.y == symY && it.orientation == WallOrientation.HORIZONTAL }.toSet()
                                                                        }
                                                                    }
                                                                } else {
                                                                    val newWalls = mutableSetOf<Wall>()
                                                                    newWalls.add(Wall(x = x, y = y, orientation = WallOrientation.HORIZONTAL, placedBy = PlayerId.PLAYER_1))
                                                                    if (symmetryMode == SymmetryMode.ROTATIONAL_180) {
                                                                        val symX = gridSize - 2 - x
                                                                        val symY = gridSize - 2 - y
                                                                        if (symX in 0 until gridSize - 1 && symY in 0 until gridSize - 1) {
                                                                            newWalls.add(Wall(x = symX, y = symY, orientation = WallOrientation.HORIZONTAL, placedBy = PlayerId.PLAYER_2))
                                                                        }
                                                                    }
                                                                    presetWalls = presetWalls + newWalls
                                                                }
                                                            }
                                                        }
                                                        EditorTool.PRESET_WALL_V -> {
                                                            if (x < gridSize - 1 && y < gridSize - 1) {
                                                                val existing = presetWalls.find { it.x == x && it.y == y && it.orientation == WallOrientation.VERTICAL }
                                                                if (existing != null) {
                                                                    presetWalls = presetWalls - existing
                                                                    if (symmetryMode == SymmetryMode.ROTATIONAL_180) {
                                                                        val symX = gridSize - 2 - x
                                                                        val symY = gridSize - 2 - y
                                                                        if (symX in 0 until gridSize - 1 && symY in 0 until gridSize - 1) {
                                                                            presetWalls = presetWalls.filterNot { it.x == symX && it.y == symY && it.orientation == WallOrientation.VERTICAL }.toSet()
                                                                        }
                                                                    }
                                                                } else {
                                                                    val newWalls = mutableSetOf<Wall>()
                                                                    newWalls.add(Wall(x = x, y = y, orientation = WallOrientation.VERTICAL, placedBy = PlayerId.PLAYER_1))
                                                                    if (symmetryMode == SymmetryMode.ROTATIONAL_180) {
                                                                        val symX = gridSize - 2 - x
                                                                        val symY = gridSize - 2 - y
                                                                        if (symX in 0 until gridSize - 1 && symY in 0 until gridSize - 1) {
                                                                            newWalls.add(Wall(x = symX, y = symY, orientation = WallOrientation.VERTICAL, placedBy = PlayerId.PLAYER_2))
                                                                        }
                                                                    }
                                                                    presetWalls = presetWalls + newWalls
                                                                }
                                                            }
                                                        }
                                                        EditorTool.P1_START -> {
                                                            if (!hasObstacle && !isP2) {
                                                                p1Start = Position(x, y)
                                                                if (symmetryMode == SymmetryMode.ROTATIONAL_180) {
                                                                    val sym = Position(gridSize - 1 - x, gridSize - 1 - y)
                                                                    if (!obstacles.any { it.x == sym.x && it.y == sym.y }) {
                                                                        p2Start = sym
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        EditorTool.P2_START -> {
                                                            if (!hasObstacle && !isP1) {
                                                                p2Start = Position(x, y)
                                                            }
                                                        }
                                                        EditorTool.ERASE -> {
                                                            val allTargets = mutableSetOf<Pair<Int, Int>>()
                                                            targetCoords.forEach { (tx, ty) ->
                                                                allTargets.add(Pair(tx, ty))
                                                                allTargets.addAll(getSymmetricPositions(tx, ty))
                                                            }
                                                            obstacles = obstacles.filterNot { allTargets.contains(Pair(it.x, it.y)) }.toSet()
                                                            presetWalls = presetWalls.filterNot { allTargets.contains(Pair(it.x, it.y)) }.toSet()
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (hasObstacle) {
                                                Text("🪨", fontSize = 12.sp)
                                            } else if (isP1) {
                                                Text("1", fontWeight = FontWeight.Black, color = Color.Black, fontSize = 14.sp)
                                            } else if (isP2) {
                                                Text("2", fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)
                                            } else if (hWall) {
                                                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(GoldRating))
                                            } else if (vWall) {
                                                Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(GoldRating))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                EditorTab.GENERATOR -> {
                    // Smart Procedural Generators
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isArabic) "اختر نمط توليد تلقائي ذكي بضغطة زر واحدة:" else "Generate intelligent tactical layouts instantly:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        listOf(
                            Triple("🏰 " + if (isArabic) "حصن الألماس التكتيكي" else "Diamond Citadel Fortress", if (isArabic) "حوائط وأعمدة ماسية توفر ممرات اشتباك متوازنة تماماً" else "Symmetrical diamond layout creating dual flanking choke corridors", 1),
                            Triple("🌀 " + if (isArabic) "المتاهة اللولبية المتناظرة" else "Symmetric Spiral Maze", if (isArabic) "ممرات حلزونية تجبر اللاعبين على المناورة والالتفاف" else "Challenging spiral labyrinth requiring precise navigation", 2),
                            Triple("🛡️ " + if (isArabic) "بوابة الحصون المزدوجة" else "Twin Bastion Gateways", if (isArabic) "أعمدة حماية جانبية مع ممر وسط مفتوح وسريع" else "Flanking column fortresses with open high-risk central lane", 3),
                            Triple("⚡ " + if (isArabic) "ممرات الاشتباك الصاعق" else "Blitz Corridor Grid", if (isArabic) "عوائق متفرقة بانتظام لمباريات سريعة جداً" else "Regular obstacle spacing for fast tactical play", 4),
                            Triple("🎲 " + if (isArabic) "توزيع عشوائي متوازن ذكياً" else "Fair Procedural Scatter", if (isArabic) "توزيع عشوائي يضمن مسارات متكافئة 100% بين اللاعبين" else "Guaranteed fair random obstacle scattering", 5)
                        ).forEach { (title, desc, genType) ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SurfaceElevated,
                                border = BorderStroke(1.dp, CellBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        pushHistory()
                                        val newObs = mutableSetOf<Obstacle>()
                                        val newWalls = mutableSetOf<Wall>()
                                        when (genType) {
                                            1 -> { // Diamond Fortress
                                                val mid = gridSize / 2
                                                newObs.add(Obstacle(mid - 1, mid))
                                                newObs.add(Obstacle(mid + 1, mid))
                                                newObs.add(Obstacle(mid, mid - 1))
                                                newObs.add(Obstacle(mid, mid + 1))
                                            }
                                            2 -> { // Spiral Maze
                                                for (i in 1..gridSize - 3) {
                                                    if (i % 2 == 1) {
                                                        newObs.add(Obstacle(i, 2))
                                                        newObs.add(Obstacle(gridSize - 1 - i, gridSize - 3))
                                                    }
                                                }
                                            }
                                            3 -> { // Twin Bastions
                                                newObs.add(Obstacle(1, gridSize / 2))
                                                newObs.add(Obstacle(2, gridSize / 2))
                                                newObs.add(Obstacle(gridSize - 2, gridSize / 2))
                                                newObs.add(Obstacle(gridSize - 3, gridSize / 2))
                                            }
                                            4 -> { // Blitz Grid
                                                for (gx in 2 until gridSize - 2 step 2) {
                                                    for (gy in 2 until gridSize - 2 step 2) {
                                                        newObs.add(Obstacle(gx, gy))
                                                    }
                                                }
                                            }
                                            5 -> { // Fair Random Scatter
                                                val rand = Random.Default
                                                val targetCount = when (gridSize) { 5 -> 2; 7 -> 4; 9 -> 6; else -> 8 }
                                                var tries = 0
                                                while (newObs.size < targetCount && tries < 60) {
                                                    tries++
                                                    val rx = rand.nextInt(gridSize)
                                                    val ry = rand.nextInt(1, gridSize - 1)
                                                    if (rx != p1Start.x || ry != p1Start.y) {
                                                        newObs.add(Obstacle(rx, ry))
                                                        newObs.add(Obstacle(gridSize - 1 - rx, gridSize - 1 - ry))
                                                    }
                                                }
                                            }
                                        }
                                        obstacles = newObs
                                        presetWalls = newWalls
                                        currentTab = EditorTab.CANVAS
                                        Toast.makeText(context, if (isArabic) "تم توليد المرحلة بنجاح! ✨" else "Arena generated! ✨", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = title, fontWeight = FontWeight.Black, color = TextPrimary, fontSize = 14.sp)
                                        Text(text = desc, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = arenaTheme.primaryColor)
                                }
                            }
                        }
                    }
                }

                EditorTab.RULES_AI -> {
                    // Walls count per player
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "🧱 " + if (isArabic) "عدد الجدران لكل لاعب" else "Walls Per Player",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(0, 5, 8, 10, 15, 20).forEach { count ->
                                    FilterChip(
                                        selected = wallsPerPlayer == count,
                                        onClick = { wallsPerPlayer = count },
                                        label = { Text(if (count == 0) (if (isArabic) "بدون" else "None") else "$count") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = arenaTheme.primaryColor,
                                            selectedLabelColor = Color.Black,
                                            containerColor = DeepSlateBackground,
                                            labelColor = TextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Turn Time Limit
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "⏱️ " + if (isArabic) "زمن دورة التفكير" else "Turn Time Limit",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(15, 30, 60, 120, 300, 0).forEach { sec ->
                                    FilterChip(
                                        selected = timeLimitSeconds == sec,
                                        onClick = { timeLimitSeconds = sec },
                                        label = { Text(if (sec == 0) "∞" else "${sec}s") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = arenaTheme.primaryColor,
                                            selectedLabelColor = Color.Black,
                                            containerColor = DeepSlateBackground,
                                            labelColor = TextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // AI Bot Personality Picker
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "🤖 " + if (isArabic) "شخصية الروبوت المنافس" else "AI Opponent Personality",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    NPCPersonality.THE_PREDICTOR,
                                    NPCPersonality.THE_TRICKSTER,
                                    NPCPersonality.THE_RUSHER,
                                    NPCPersonality.THE_DEFENDER
                                ).forEach { p ->
                                    FilterChip(
                                        selected = selectedPersonality == p,
                                        onClick = { selectedPersonality = p },
                                        label = { Text(p.name.replace("THE_", "").lowercase().replaceFirstChar { it.uppercase() }) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF00F0FF),
                                            selectedLabelColor = Color.Black,
                                            containerColor = DeepSlateBackground,
                                            labelColor = TextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // AI Difficulty Picker
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "⚔️ " + if (isArabic) "مستوى صعوبة الذكاء الاصطناعي" else "AI Difficulty Level",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    AIDifficulty.EASY,
                                    AIDifficulty.MEDIUM,
                                    AIDifficulty.HARD,
                                    AIDifficulty.EXPERT,
                                    AIDifficulty.NIGHTMARE
                                ).forEach { diff ->
                                    FilterChip(
                                        selected = selectedDifficulty == diff,
                                        onClick = { selectedDifficulty = diff },
                                        label = { Text(diff.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = GoldRating,
                                            selectedLabelColor = Color.Black,
                                            containerColor = DeepSlateBackground,
                                            labelColor = TextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                EditorTab.PRESETS -> {
                    // Catalog of Preset Maps
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (isArabic) "اختر نموذجاً للتحميل والتعديل المباشر:" else "Select a preset to load and customize:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        CustomMapPresets.PRESET_MAPS.forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SurfaceElevated,
                                border = BorderStroke(1.dp, CellBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        pushHistory()
                                        mapName = preset.name
                                        mapDescription = preset.description
                                        gridSize = preset.gridSize
                                        p1Start = preset.p1Start
                                        p2Start = preset.p2Start
                                        obstacles = preset.obstacles.toSet()
                                        presetWalls = preset.presetWalls.toSet()
                                        wallsPerPlayer = preset.wallsCount
                                        timeLimitSeconds = preset.timeLimitSeconds
                                        currentTab = EditorTab.CANVAS
                                        Toast.makeText(context, if (isArabic) "تم تحميل نموذج: ${preset.name}" else "Loaded: ${preset.name}", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.name,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = preset.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "📐 ${preset.gridSize}x${preset.gridSize} • 🧱 ${preset.wallsCount} • ⏱️ ${preset.timeLimitSeconds}s • 🪨 ${preset.obstacles.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = arenaTheme.primaryColor,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            pushHistory()
                                            mapName = preset.name
                                            mapDescription = preset.description
                                            gridSize = preset.gridSize
                                            p1Start = preset.p1Start
                                            p2Start = preset.p2Start
                                            obstacles = preset.obstacles.toSet()
                                            presetWalls = preset.presetWalls.toSet()
                                            wallsPerPlayer = preset.wallsCount
                                            timeLimitSeconds = preset.timeLimitSeconds
                                            currentTab = EditorTab.CANVAS
                                            Toast.makeText(context, if (isArabic) "تم تحميل نموذج: ${preset.name}" else "Loaded: ${preset.name}", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = arenaTheme.primaryColor, contentColor = Color.Black),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(if (isArabic) "تحميل" else "Load", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Sticky Action Buttons: Save & Live Play Test
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val lvl = CustomLevel(
                            id = UUID.randomUUID().toString().take(8),
                            name = mapName,
                            description = mapDescription,
                            gridSize = gridSize,
                            p1Start = p1Start,
                            p2Start = p2Start,
                            obstacles = obstacles.toList(),
                            presetWalls = presetWalls.toList(),
                            wallsCount = wallsPerPlayer,
                            timeLimitSeconds = timeLimitSeconds,
                            aiPersonality = selectedPersonality,
                            aiDifficulty = selectedDifficulty
                        )
                        viewModel.saveCustomLevel(lvl)
                        Toast.makeText(context, if (isArabic) "تم حفظ المرحلة بنجاح! 💾" else "Level saved! 💾", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                    border = BorderStroke(1.dp, arenaTheme.primaryColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = arenaTheme.primaryColor)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.get("save_level", settings.language), color = arenaTheme.primaryColor, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (p1Start == p2Start) {
                            val msg = if (isArabic) "لا يمكن أن يبدأ اللاعبان في نفس الخلية!" else "Players cannot start on the same cell!"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (obstacles.any { it.x == p1Start.x && it.y == p1Start.y } || obstacles.any { it.x == p2Start.x && it.y == p2Start.y }) {
                            val msg = if (isArabic) "لا يمكن وضع نقطة البداية فوق عائق!" else "Player start cannot be on an obstacle!"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (p1Start.y == p1Goal || p2Start.y == p2Goal) {
                            val msg = if (isArabic) "لا يمكن أن تبدأ في صف الفوز مباشرة!" else "Player cannot start on their winning goal line!"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val p1HasPath = PathFinder.hasPathToGoal(p1Start, p1Goal, presetWalls.toList(), gridSize, obstacles.toList())
                        val p2HasPath = PathFinder.hasPathToGoal(p2Start, p2Goal, presetWalls.toList(), gridSize, obstacles.toList())
                        if (!p1HasPath || !p2HasPath) {
                            val msg = if (isArabic) "أحد اللاعبين محاصر تماماً بالجدران أو العوائق ولا يوجد له مسار للهدف!" else "A player has no reachable path to their goal!"
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        val lvl = CustomLevel(
                            id = UUID.randomUUID().toString().take(8),
                            name = mapName,
                            description = mapDescription,
                            gridSize = gridSize,
                            p1Start = p1Start,
                            p2Start = p2Start,
                            obstacles = obstacles.toList(),
                            presetWalls = presetWalls.toList(),
                            wallsCount = wallsPerPlayer,
                            timeLimitSeconds = timeLimitSeconds,
                            aiPersonality = selectedPersonality,
                            aiDifficulty = selectedDifficulty
                        )
                        viewModel.startCustomLevelMatch(lvl)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = arenaTheme.primaryColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isArabic) "🎮 تجربة المرحلة الآن" else "🎮 Test Play Arena", color = Color.Black, fontWeight = FontWeight.Black)
                }
            }
        }
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(Strings.get("import_level", settings.language)) },
            text = {
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    placeholder = { Text("Paste JSON map code here...") },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = CustomMapPresets.deserializeFromJson(importText.trim())
                        if (parsed != null) {
                            pushHistory()
                            mapName = parsed.name
                            mapDescription = parsed.description
                            gridSize = parsed.gridSize
                            p1Start = parsed.p1Start
                            p2Start = parsed.p2Start
                            obstacles = parsed.obstacles.toSet()
                            presetWalls = parsed.presetWalls.toSet()
                            wallsPerPlayer = parsed.wallsCount
                            timeLimitSeconds = parsed.timeLimitSeconds
                            selectedPersonality = parsed.aiPersonality
                            selectedDifficulty = parsed.aiDifficulty
                            showImportDialog = false
                            currentTab = EditorTab.CANVAS
                            Toast.makeText(context, if (isArabic) "تم استيراد المرحلة بنجاح! 📥" else "Imported successfully! 📥", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, if (isArabic) "كود غير صالح" else "Invalid code", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(Strings.get("cancel", settings.language))
                }
            }
        )
    }

    // Clear All Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(if (isArabic) "مسح كامل الخريطة؟" else "Clear Entire Canvas?") },
            text = { Text(if (isArabic) "هل تريد حقاً إزالة جميع العوائق والجدران المثبتة؟" else "Do you really want to remove all obstacles and walls?") },
            confirmButton = {
                Button(
                    onClick = {
                        pushHistory()
                        obstacles = emptySet()
                        presetWalls = emptySet()
                        showClearDialog = false
                        Toast.makeText(context, if (isArabic) "تم مسح الخريطة بالكامل" else "Canvas cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text(if (isArabic) "مسح الكل" else "Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(Strings.get("cancel", settings.language))
                }
            }
        )
    }
}
