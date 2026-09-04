package com.example.wallrush.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.example.wallrush.domain.engine.RuleEngine
import com.example.wallrush.domain.model.*
import kotlin.math.min

@Composable
fun GameBoardCanvas(
    state: GameState,
    isLocalTurn: Boolean,
    legalMoves: List<Position>,
    selectedPawn: PlayerId?,
    previewWall: Wall?,
    isWallValid: Boolean,
    onCellClicked: (Position) -> Unit,
    onPawnClicked: (PlayerId) -> Unit,
    onWallSlotClicked: (x: Int, y: Int) -> Unit,
    modifier: Modifier = Modifier,
    theme: GameTheme = GameTheme.MainCyberNeon
) {
    // Pulse animation for turn indicator / legal move dots
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state, isLocalTurn, previewWall) {
                    if (!isLocalTurn) return@pointerInput

                    detectTapGestures { offset ->
                        val w = size.width
                        val padding = w * 0.035f
                        val playableWidth = w - (padding * 2)
                        val gap = playableWidth * 0.024f
                        val cellSize = (playableWidth - (gap * 8)) / 9f
                        val step = cellSize + gap

                        val localX = offset.x - padding
                        val localY = offset.y - padding

                        if (localX < 0 || localY < 0 || localX > playableWidth || localY > playableWidth) return@detectTapGestures

                        // Check if tap was near a wall slot / gap
                        var tappedWallSlot = false
                        for (wx in 0..7) {
                            for (wy in 0..7) {
                                val pegCenterX = (wx + 1) * step - (gap / 2f)
                                val pegCenterY = (wy + 1) * step - (gap / 2f)
                                val distSq = (localX - pegCenterX) * (localX - pegCenterX) + (localY - pegCenterY) * (localY - pegCenterY)
                                val hitRadius = step * 0.45f
                                if (distSq <= hitRadius * hitRadius) {
                                    onWallSlotClicked(wx, wy)
                                    tappedWallSlot = true
                                    break
                                }
                            }
                            if (tappedWallSlot) break
                        }

                        if (!tappedWallSlot) {
                            // Tap was on a cell
                            val col = (localX / step).toInt().coerceIn(0, 8)
                            val row = (localY / step).toInt().coerceIn(0, 8)
                            val clickedPos = Position(col, row)

                            if (clickedPos == state.player1.position) {
                                onPawnClicked(PlayerId.PLAYER_1)
                            } else if (clickedPos == state.player2.position) {
                                onPawnClicked(PlayerId.PLAYER_2)
                            } else {
                                onCellClicked(clickedPos)
                            }
                        }
                    }
                }
        ) {
            val w = size.width
            val padding = w * 0.035f
            val playableWidth = w - (padding * 2)
            val gap = playableWidth * 0.024f
            val cellSize = (playableWidth - (gap * 8)) / 9f
            val step = cellSize + gap

            // 1. Draw Board Background Container
            drawRoundRect(
                color = theme.boardSurface,
                topLeft = Offset(0f, 0f),
                size = Size(w, w),
                cornerRadius = CornerRadius(24f, 24f)
            )
            drawRoundRect(
                color = theme.cellBorderColor,
                topLeft = Offset(0f, 0f),
                size = Size(w, w),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 3f)
            )

            // 2. Goal Lines indicators (Top for P1, Bottom for P2)
            val p1GoalColor = theme.p1Primary.copy(alpha = 0.22f)
            val p2GoalColor = theme.p2Primary.copy(alpha = 0.22f)

            // Paint for coordinates (1..9, i..a)
            val textPaint = Paint().apply {
                color = theme.cellBorderColor.copy(alpha = 0.55f).toArgb()
                textSize = cellSize * 0.26f
                isAntiAlias = true
                textAlign = Paint.Align.LEFT
                typeface = Typeface.DEFAULT_BOLD
            }

            // 3. Draw 81 Cells
            val rowLabels = listOf("9", "8", "7", "6", "5", "4", "3", "2", "1")
            val colLabels = listOf("i", "h", "g", "f", "e", "d", "c", "b", "a")

            for (r in 0..8) {
                for (c in 0..8) {
                    val cellLeft = padding + c * step
                    val cellTop = padding + r * step

                    val isP1GoalRow = (r == 0)
                    val isP2GoalRow = (r == 8)

                    val bgCellColor = when {
                        isP1GoalRow -> p1GoalColor
                        isP2GoalRow -> p2GoalColor
                        (r + c) % 2 == 0 -> theme.cellColor
                        else -> theme.cellAltColor
                    }

                    drawRoundRect(
                        color = bgCellColor,
                        topLeft = Offset(cellLeft, cellTop),
                        size = Size(cellSize, cellSize),
                        cornerRadius = CornerRadius(12f, 12f)
                    )

                    // Cell border
                    drawRoundRect(
                        color = if (isP1GoalRow) theme.p1Primary.copy(alpha = 0.45f)
                        else if (isP2GoalRow) theme.p2Primary.copy(alpha = 0.45f)
                        else theme.cellBorderColor,
                        topLeft = Offset(cellLeft, cellTop),
                        size = Size(cellSize, cellSize),
                        cornerRadius = CornerRadius(12f, 12f),
                        style = Stroke(width = 1.5f)
                    )

                    // Draw coordinates on edge cells
                    if (theme.showCoordinates) {
                        if (c == 0) {
                            // Row label on the left of cell
                            drawContext.canvas.nativeCanvas.drawText(
                                rowLabels[r],
                                cellLeft + (cellSize * 0.08f),
                                cellTop + (cellSize * 0.32f),
                                textPaint
                            )
                        }
                        if (r == 8) {
                            // Column label on the bottom of cell
                            drawContext.canvas.nativeCanvas.drawText(
                                colLabels[c],
                                cellLeft + (cellSize * 0.08f),
                                cellTop + (cellSize * 0.90f),
                                textPaint
                            )
                        }
                    }
                }
            }

            // 4. Draw Legal Move Dots
            if (isLocalTurn) {
                for (move in legalMoves) {
                    val moveLeft = padding + move.x * step
                    val moveTop = padding + move.y * step
                    val centerX = moveLeft + (cellSize / 2f)
                    val centerY = moveTop + (cellSize / 2f)
                    val baseRadius = cellSize * 0.22f

                    // Outer glow
                    drawCircle(
                        color = MoveIndicatorDot.copy(alpha = glowAlpha),
                        radius = baseRadius * pulseScale,
                        center = Offset(centerX, centerY)
                    )
                    // Inner bright dot
                    drawCircle(
                        color = MoveIndicatorDot,
                        radius = baseRadius * 0.7f,
                        center = Offset(centerX, centerY)
                    )
                }
            }

            // 5. Draw Placed Walls
            for (wall in state.walls) {
                val (wColor, wBorder) = if (theme.isWallPlayerSpecific) {
                    if (wall.placedBy == PlayerId.PLAYER_1) {
                        Pair(theme.p1WallColor, theme.p1WallBorder)
                    } else {
                        Pair(theme.p2WallColor, theme.p2WallBorder)
                    }
                } else {
                    Pair(theme.defaultWallColor, theme.defaultWallBorder)
                }

                drawWall(
                    wall = wall,
                    padding = padding,
                    step = step,
                    cellSize = cellSize,
                    gap = gap,
                    color = wColor,
                    borderColor = wBorder,
                    alpha = 1f,
                    isGlow = theme.isGlowEnabled
                )
            }

            // 6. Draw Preview Wall (if currently being positioned)
            if (previewWall != null) {
                val previewColor = if (isWallValid) WallPreviewLegal else WallPreviewIllegal
                val previewBorder = if (isWallValid) Color(0xFF059669) else DangerRed

                drawWall(
                    wall = previewWall,
                    padding = padding,
                    step = step,
                    cellSize = cellSize,
                    gap = gap,
                    color = previewColor,
                    borderColor = previewBorder,
                    alpha = 0.85f,
                    isGlow = false
                )
            }

            // 7. Draw Player Pawns
            // Player 1 (P1)
            drawPawn(
                pos = state.player1.position,
                padding = padding,
                step = step,
                cellSize = cellSize,
                primaryColor = theme.p1Primary,
                darkColor = theme.p1Dark,
                glowColor = theme.p1Glow,
                isSelected = (selectedPawn == PlayerId.PLAYER_1 || (state.currentTurn == PlayerId.PLAYER_1 && isLocalTurn)),
                pulseScale = if (state.currentTurn == PlayerId.PLAYER_1) pulseScale else 1f,
                glowAlpha = glowAlpha
            )

            // Player 2 (P2)
            drawPawn(
                pos = state.player2.position,
                padding = padding,
                step = step,
                cellSize = cellSize,
                primaryColor = theme.p2Primary,
                darkColor = theme.p2Dark,
                glowColor = theme.p2Glow,
                isSelected = (selectedPawn == PlayerId.PLAYER_2 || (state.currentTurn == PlayerId.PLAYER_2 && isLocalTurn)),
                pulseScale = if (state.currentTurn == PlayerId.PLAYER_2) pulseScale else 1f,
                glowAlpha = glowAlpha
            )
        }
    }
}

private fun DrawScope.drawPawn(
    pos: Position,
    padding: Float,
    step: Float,
    cellSize: Float,
    primaryColor: Color,
    darkColor: Color,
    glowColor: Color,
    isSelected: Boolean,
    pulseScale: Float,
    glowAlpha: Float
) {
    val centerX = padding + pos.x * step + (cellSize / 2f)
    val centerY = padding + pos.y * step + (cellSize / 2f)
    val pawnRadius = cellSize * 0.38f

    // Soft drop shadow
    drawCircle(
        color = Color(0x77000000),
        radius = pawnRadius * 1.05f,
        center = Offset(centerX + 3f, centerY + 5f)
    )

    // Active glow if current turn
    if (isSelected) {
        drawCircle(
            color = glowColor.copy(alpha = glowAlpha),
            radius = pawnRadius * 1.35f * pulseScale,
            center = Offset(centerX, centerY)
        )
    }

    // Outer ring
    drawCircle(
        color = darkColor,
        radius = pawnRadius,
        center = Offset(centerX, centerY)
    )

    // Inner bright sphere
    drawCircle(
        color = primaryColor,
        radius = pawnRadius * 0.82f,
        center = Offset(centerX, centerY)
    )

    // Highlight reflection
    drawCircle(
        color = Color.White.copy(alpha = 0.55f),
        radius = pawnRadius * 0.28f,
        center = Offset(centerX - pawnRadius * 0.32f, centerY - pawnRadius * 0.32f)
    )
}

private fun DrawScope.drawWall(
    wall: Wall,
    padding: Float,
    step: Float,
    cellSize: Float,
    gap: Float,
    color: Color,
    borderColor: Color,
    alpha: Float,
    isGlow: Boolean
) {
    val wallThickness = gap * 1.35f
    val wallLength = (cellSize * 2) + gap

    val (topLeft, size) = if (wall.orientation == WallOrientation.HORIZONTAL) {
        val left = padding + (wall.x * step)
        val top = padding + (wall.y * step) + cellSize - ((wallThickness - gap) / 2f)
        Pair(Offset(left, top), Size(wallLength, wallThickness))
    } else {
        val left = padding + (wall.x * step) + cellSize - ((wallThickness - gap) / 2f)
        val top = padding + (wall.y * step)
        Pair(Offset(left, top), Size(wallThickness, wallLength))
    }

    // Neon Outer Glow if enabled
    if (isGlow) {
        drawRoundRect(
            color = color.copy(alpha = 0.45f * alpha),
            topLeft = Offset(topLeft.x - 3f, topLeft.y - 3f),
            size = Size(size.width + 6f, size.height + 6f),
            cornerRadius = CornerRadius(8f, 8f)
        )
    }

    // Shadow
    drawRoundRect(
        color = Color(0x55000000),
        topLeft = Offset(topLeft.x + 2f, topLeft.y + 4f),
        size = size,
        cornerRadius = CornerRadius(6f, 6f)
    )

    // Main wall body
    drawRoundRect(
        color = color.copy(alpha = alpha),
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(6f, 6f)
    )

    // Border
    drawRoundRect(
        color = borderColor.copy(alpha = alpha),
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 2f)
    )
}
