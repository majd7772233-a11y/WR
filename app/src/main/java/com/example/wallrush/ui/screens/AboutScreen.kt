package com.example.wallrush.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.wallrush.ui.localization.Strings
import com.example.wallrush.ui.viewmodel.WallRushViewModel
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(viewModel: WallRushViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val language = settings.language

    // Interactive 3D touch rotation offset
    var dragYaw by remember { mutableFloatStateOf(0f) }
    var dragPitch by remember { mutableFloatStateOf(0f) }

    // Copy notification states
    var phoneCopied by remember { mutableStateOf(false) }
    var emailCopied by remember { mutableStateOf(false) }

    val developerPhone = "+967 735 465 673"
    val developerEmail = "majd7772233@gmail.com"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = Strings.get("about", language),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF070B14)
                )
            )
        },
        containerColor = Color(0xFF070B14)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Cosmic Aurora & Cyber Starfield Background Canvas
            CyberSpaceBackground(modifier = Modifier.fillMaxSize())

            // 2. Main Scrollable Content
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
                ) {
                // Interactive 3D Canvas Showcase
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🎮 INTERACTIVE 3D ENGINE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = Player1Primary.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اسحب بإصبعك لتدوير اللوحة ثلاثية الأبعاد",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Interactive 3D Quoridor Arena Canvas
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF0D1424).copy(alpha = 0.85f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.linearGradient(
                                        listOf(Player1Primary, Color(0xFF8B5CF6), Player2Primary)
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = Player1Primary, spotColor = Player1Glow)
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragYaw += dragAmount.x * 0.015f
                                            dragPitch = (dragPitch + dragAmount.y * 0.01f).coerceIn(-0.6f, 0.6f)
                                        }
                                    )
                                }
                        ) {
                            Interactive3DQuoridorCanvas(
                                externalYaw = dragYaw,
                                externalPitch = dragPitch,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // App Branding Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.9f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Glowing 3D App Icon
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Player1Primary, Color(0xFF0284C7), Color(0xFF0369A1))
                                        )
                                    )
                                    .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                    .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = Player1Primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🧱",
                                    fontSize = 38.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "WallRush",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                color = Color.White
                            )

                            Text(
                                text = "v2.5.0 Quantum Edition",
                                style = MaterialTheme.typography.labelMedium,
                                color = Player1Primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = Strings.get("app_full_desc", language),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Feature Chips Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                FeatureBadge(icon = "⚡", title = "P2P Bluetooth")
                                FeatureBadge(icon = "🌐", title = "Wi-Fi Hotspot")
                                FeatureBadge(icon = "🤖", title = "AlphaBeta AI")
                            }
                        }
                    }
                }

                // Developer Showcase Holographic Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B132B).copy(alpha = 0.95f)),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFC084FC))
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Developer Avatar & Badge
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(Player1Primary, Color(0xFF8B5CF6), Player2Primary, Player1Primary)
                                        )
                                    )
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "Developer",
                                    tint = Player1Primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = Strings.get("developer_label", language),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = Strings.get("developer_name", language),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }

                            HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                            // Contact Action 1: Phone Dialer & Click-to-Call
                            ContactActionTile(
                                icon = Icons.Default.Phone,
                                iconColor = Color(0xFF10B981),
                                title = Strings.get("contact_label", language),
                                value = developerPhone,
                                subtitle = "انقر لفتح لوحة الاتصال ونسخ الرقم",
                                isCopied = phoneCopied,
                                onClick = {
                                    // 1. Copy to clipboard
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Developer Phone", developerPhone)
                                    clipboard.setPrimaryClip(clip)
                                    phoneCopied = true

                                    // 2. Open dialer with prefilled number
                                    try {
                                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${developerPhone.replace(" ", "")}")
                                        }
                                        context.startActivity(dialIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, Strings.get("phone_copied_dialer", language), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            // Contact Action 2: Email Click-to-Copy
                            ContactActionTile(
                                icon = Icons.Default.Email,
                                iconColor = Color(0xFF38BDF8),
                                title = Strings.get("email_label", language),
                                value = developerEmail,
                                subtitle = "انقر لنسخ البريد الإلكتروني للحافظة",
                                isCopied = emailCopied,
                                onClick = {
                                    // Copy to clipboard
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Developer Email", developerEmail)
                                    clipboard.setPrimaryClip(clip)
                                    emailCopied = true
                                    Toast.makeText(context, Strings.get("email_copied", language), Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                // Technology & Engine Specs
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B132B).copy(alpha = 0.7f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🚀 SYSTEM & ENGINE SPECS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Player1Primary,
                                letterSpacing = 1.sp
                            )
                            SpecRow(label = "Graphics Pipeline", value = "Jetpack Compose High-FPS Canvas")
                            SpecRow(label = "Multiplayer Protocol", value = "True RFCOMM Bluetooth & Wi-Fi Sockets")
                            SpecRow(label = "AI Algorithm", value = "Alpha-Beta Pruning + Pathfinding Heuristic")
                            SpecRow(label = "Architecture", value = "Clean Architecture / Reactive Flows")
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun FeatureBadge(icon: String, title: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.8f),
        modifier = Modifier.border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = icon, fontSize = 12.sp)
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ContactActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    subtitle: String,
    isCopied: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(
                1.dp,
                if (isCopied) iconColor else Color(0xFF1E293B),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (isCopied) iconColor.copy(alpha = 0.12f) else Color(0xFF131D33)
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.2f))
                        .border(1.5.dp, iconColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isCopied) "✅ تم النسخ بنجاح!" else subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCopied) iconColor else TextSecondary,
                        fontSize = 10.5.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = if (isCopied) iconColor else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// -----------------------------------------------------------------------------------------
// LIVE 3D INTERACTIVE QUORIDOR ARENA CANVAS
// -----------------------------------------------------------------------------------------

@Composable
private fun Interactive3DQuoridorCanvas(
    externalYaw: Float,
    externalPitch: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "3DCanvasAnim")

    // Continuous smooth rotation angle
    val autoAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "autoAngle"
    )

    // Floating levitation wave
    val levitation by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "levitation"
    )

    // Neon wall pulse glow
    val wallGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wallGlow"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f + 10f

        val totalYaw = autoAngle + externalYaw
        val pitch = (0.75f + externalPitch).coerceIn(0.35f, 1.25f) // tilt angle

        // Draw radial glow center under board
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.35f), Color.Transparent),
                center = Offset(cx, cy),
                radius = w * 0.42f
            ),
            radius = w * 0.42f,
            center = Offset(cx, cy)
        )

        // 3D Isometric projection math
        fun project3D(x: Float, y: Float, z: Float): Offset {
            // Rotate around Z axis (yaw)
            val cosA = cos(totalYaw)
            val sinA = sin(totalYaw)
            val rotX = x * cosA - y * sinA
            val rotY = x * sinA + y * cosA

            // Tilt with pitch
            val projX = cx + rotX
            val projY = cy + (rotY * sin(pitch)) - (z * cos(pitch))
            return Offset(projX, projY)
        }

        // 1. Draw 3D Base Slab Extrusion
        val slabSize = min(w, h) * 0.38f
        val slabDepth = 22f

        val slabCornersTop = listOf(
            project3D(-slabSize, -slabSize, 0f),
            project3D(slabSize, -slabSize, 0f),
            project3D(slabSize, slabSize, 0f),
            project3D(-slabSize, slabSize, 0f)
        )

        val slabCornersBottom = listOf(
            project3D(-slabSize, -slabSize, -slabDepth),
            project3D(slabSize, -slabSize, -slabDepth),
            project3D(slabSize, slabSize, -slabDepth),
            project3D(-slabSize, slabSize, -slabDepth)
        )

        // Draw slab side faces
        for (i in 0..3) {
            val next = (i + 1) % 4
            val sidePath = Path().apply {
                moveTo(slabCornersTop[i].x, slabCornersTop[i].y)
                lineTo(slabCornersTop[next].x, slabCornersTop[next].y)
                lineTo(slabCornersBottom[next].x, slabCornersBottom[next].y)
                lineTo(slabCornersBottom[i].x, slabCornersBottom[i].y)
                close()
            }
            drawPath(
                path = sidePath,
                color = Color(0xFF0F172A).copy(alpha = 0.95f)
            )
            drawPath(
                path = sidePath,
                color = Color(0xFF38BDF8).copy(alpha = 0.3f),
                style = Stroke(1.2f)
            )
        }

        // Draw slab top face
        val topPath = Path().apply {
            moveTo(slabCornersTop[0].x, slabCornersTop[0].y)
            for (i in 1..3) lineTo(slabCornersTop[i].x, slabCornersTop[i].y)
            close()
        }
        drawPath(
            path = topPath,
            brush = Brush.linearGradient(
                listOf(Color(0xFF1E293B), Color(0xFF0B132B))
            )
        )
        drawPath(
            path = topPath,
            color = Color(0xFF38BDF8),
            style = Stroke(2f)
        )

        // 2. Draw 3D Quoridor Grid Cells (5x5 visual model)
        val gridStep = (slabSize * 1.8f) / 5f
        val halfSlab = slabSize * 0.9f
        val cellSize = gridStep * 0.78f

        for (gx in 0 until 5) {
            for (gy in 0 until 5) {
                val ox = -halfSlab + gx * gridStep
                val oy = -halfSlab + gy * gridStep

                val p0 = project3D(ox, oy, 1f)
                val p1 = project3D(ox + cellSize, oy, 1f)
                val p2 = project3D(ox + cellSize, oy + cellSize, 1f)
                val p3 = project3D(ox, oy + cellSize, 1f)

                val cellPath = Path().apply {
                    moveTo(p0.x, p0.y)
                    lineTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p3.x, p3.y)
                    close()
                }

                val isGoal1 = gy == 0
                val isGoal2 = gy == 4
                val cellColor = when {
                    isGoal1 -> Color(0xFF0284C7).copy(alpha = 0.35f)
                    isGoal2 -> Color(0xFFE11D48).copy(alpha = 0.35f)
                    (gx + gy) % 2 == 0 -> Color(0xFF1E293B)
                    else -> Color(0xFF0F172A)
                }

                drawPath(path = cellPath, color = cellColor)
                drawPath(path = cellPath, color = Color(0xFF334155), style = Stroke(1f))
            }
        }

        // 3. Draw 3D Levitation Pawns (P1 Cyan, P2 Crimson)
        // P1 Pawn (Top player heading down)
        val p1Pos = project3D(-gridStep * 0.5f, -gridStep * 1.0f, 15f + levitation)
        val p1Shadow = project3D(-gridStep * 0.5f, -gridStep * 1.0f, 1f)

        // Drop shadow
        drawCircle(
            color = Color(0x66000000),
            radius = 16f,
            center = p1Shadow
        )
        // Pawn Glow
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF38BDF8).copy(alpha = 0.75f), Color.Transparent),
                center = p1Pos,
                radius = 28f
            ),
            radius = 28f,
            center = p1Pos
        )
        // Sphere body
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFF38BDF8), Color(0xFF0284C7)),
                center = Offset(p1Pos.x - 4f, p1Pos.y - 4f),
                radius = 14f
            ),
            radius = 14f,
            center = p1Pos
        )

        // P2 Pawn (Bottom player heading up)
        val p2Pos = project3D(gridStep * 0.5f, gridStep * 1.0f, 15f - levitation)
        val p2Shadow = project3D(gridStep * 0.5f, gridStep * 1.0f, 1f)

        // Drop shadow
        drawCircle(
            color = Color(0x66000000),
            radius = 16f,
            center = p2Shadow
        )
        // Pawn Glow
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFF43F5E).copy(alpha = 0.75f), Color.Transparent),
                center = p2Pos,
                radius = 28f
            ),
            radius = 28f,
            center = p2Pos
        )
        // Sphere body
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFFB7185), Color(0xFFBE123C)),
                center = Offset(p2Pos.x - 4f, p2Pos.y - 4f),
                radius = 14f
            ),
            radius = 14f,
            center = p2Pos
        )

        // 4. Draw 3D Elevated Walls on the board
        fun draw3DWall(wx: Float, wy: Float, isHorizontal: Boolean, color: Color) {
            val wallW = if (isHorizontal) gridStep * 1.7f else 12f
            val wallL = if (isHorizontal) 12f else gridStep * 1.7f
            val wallH = 24f

            val b0 = project3D(wx - wallW / 2f, wy - wallL / 2f, 1f)
            val b1 = project3D(wx + wallW / 2f, wy - wallL / 2f, 1f)
            val b2 = project3D(wx + wallW / 2f, wy + wallL / 2f, 1f)
            val b3 = project3D(wx - wallW / 2f, wy + wallL / 2f, 1f)

            val t0 = project3D(wx - wallW / 2f, wy - wallL / 2f, 1f + wallH)
            val t1 = project3D(wx + wallW / 2f, wy - wallL / 2f, 1f + wallH)
            val t2 = project3D(wx + wallW / 2f, wy + wallL / 2f, 1f + wallH)
            val t3 = project3D(wx - wallW / 2f, wy + wallL / 2f, 1f + wallH)

            // Top face
            val topP = Path().apply {
                moveTo(t0.x, t0.y)
                lineTo(t1.x, t1.y)
                lineTo(t2.x, t2.y)
                lineTo(t3.x, t3.y)
                close()
            }
            drawPath(path = topP, color = color.copy(alpha = wallGlowAlpha))
            drawPath(path = topP, color = Color.White, style = Stroke(1.2f))

            // Side faces
            val s0 = Path().apply {
                moveTo(t0.x, t0.y)
                lineTo(t1.x, t1.y)
                lineTo(b1.x, b1.y)
                lineTo(b0.x, b0.y)
                close()
            }
            drawPath(path = s0, color = color.copy(alpha = 0.75f))
            drawPath(path = s0, color = color, style = Stroke(1f))
        }

        // Place 2 tactical 3D neon walls
        draw3DWall(wx = 0f, wy = -gridStep * 0.3f, isHorizontal = true, color = Color(0xFFF59E0B))
        draw3DWall(wx = -gridStep * 0.8f, wy = gridStep * 0.4f, isHorizontal = false, color = Color(0xFF10B981))
    }
}

// -----------------------------------------------------------------------------------------
// CYBER SPACE COSMIC PARTICLES BACKGROUND
// -----------------------------------------------------------------------------------------

@Composable
private fun CyberSpaceBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "SpaceAnim")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Deep cosmic gradient
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF050811), Color(0xFF0A0F1E), Color(0xFF030712))
            )
        )

        // Radiant Aurora Neon Clouds
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Player1Primary.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(w * 0.25f, h * 0.20f),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(w * 0.25f, h * 0.20f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF8B5CF6).copy(alpha = 0.14f), Color.Transparent),
                center = Offset(w * 0.8f, h * 0.65f),
                radius = w * 0.65f
            ),
            radius = w * 0.65f,
            center = Offset(w * 0.8f, h * 0.65f)
        )

        // Floating Dust Moters & Neon Starfield (40 stars)
        for (i in 0 until 40) {
            val seedX = ((i * 47) % 100) / 100f
            val seedY = ((i * 83) % 100) / 100f
            val starSize = 1.5f + (i % 3) * 1.2f

            val currentY = (seedY + phase * (0.15f + (i % 5) * 0.05f)) % 1f
            val posX = seedX * w
            val posY = currentY * h

            val starAlpha = 0.3f + 0.6f * sin((phase * 2 * Math.PI + i).toFloat()).pow(2)
            val starColor = if (i % 2 == 0) Player1Primary else Color(0xFFA78BFA)

            drawCircle(
                color = starColor.copy(alpha = starAlpha),
                radius = starSize,
                center = Offset(posX, posY)
            )
        }
    }
}
