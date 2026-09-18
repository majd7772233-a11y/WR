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
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.example.wallrush.domain.engine.RuleEngine
import com.example.wallrush.domain.model.*

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
    theme: GameTheme = GameTheme.MainCyberNeon,
    isFlipped: Boolean = false,
    ghostTrajectory: List<Position> = emptyList(),
    isGhostEnabled: Boolean = false
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
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val boardDim = if (maxWidth < maxHeight) maxWidth else maxHeight

        Canvas(
            modifier = Modifier
                .size(boardDim)
                .pointerInput(state, isLocalTurn, previewWall, isFlipped) {
                    if (!isLocalTurn) return@pointerInput

                    detectTapGestures { offset ->
                        val gridSize = state.rules.gridSize.coerceAtLeast(3)
                        val maxCoord = gridSize - 1
                        val maxWallCoord = gridSize - 2

                        val w = kotlin.math.min(size.width, size.height).toFloat()
                        val padding = w * 0.032f
                        val playableWidth = w - (padding * 2)
                        val gap = playableWidth * (0.25f / gridSize)
                        val cellSize = (playableWidth - (gap * maxCoord)) / gridSize.toFloat()
                        val step = cellSize + gap

                        val localX = offset.x - padding
                        val localY = offset.y - padding

                        if (localX < 0 || localY < 0 || localX > playableWidth || localY > playableWidth) return@detectTapGestures

                        // Check if tap was near a wall slot / gap
                        var tappedWallSlot = false
                        for (dwx in 0..maxWallCoord) {
                            for (dwy in 0..maxWallCoord) {
                                val pegCenterX = (dwx + 1) * step - (gap / 2f)
                                val pegCenterY = (dwy + 1) * step - (gap / 2f)
                                val distSq = (localX - pegCenterX) * (localX - pegCenterX) + (localY - pegCenterY) * (localY - pegCenterY)
                                val hitRadius = step * 0.48f
                                if (distSq <= hitRadius * hitRadius) {
                                    val modelWx = if (isFlipped) maxWallCoord - dwx else dwx
                                    val modelWy = if (isFlipped) maxWallCoord - dwy else dwy
                                    onWallSlotClicked(modelWx, modelWy)
                                    tappedWallSlot = true
                                    break
                                }
                            }
                            if (tappedWallSlot) break
                        }

                        if (!tappedWallSlot) {
                            // Tap was on a cell
                            val dispCol = (localX / step).toInt().coerceIn(0, maxCoord)
                            val dispRow = (localY / step).toInt().coerceIn(0, maxCoord)
                            val modelCol = if (isFlipped) maxCoord - dispCol else dispCol
                            val modelRow = if (isFlipped) maxCoord - dispRow else dispRow
                            val clickedPos = Position(modelCol, modelRow)

                            if (clickedPos == state.player1.position) {
                                onPawnClicked(PlayerId.PLAYER_1)
                            } else if (clickedPos == state.player2.position) {
                                onPawnClicked(PlayerId.PLAYER_2)
                            } else if (state.player3 != null && clickedPos == state.player3.position) {
                                onPawnClicked(PlayerId.PLAYER_3)
                            } else if (state.player4 != null && clickedPos == state.player4.position) {
                                onPawnClicked(PlayerId.PLAYER_4)
                            } else {
                                onCellClicked(clickedPos)
                            }
                        }
                    }
                }
        ) {
            val gridSize = state.rules.gridSize.coerceAtLeast(3)
            val maxCoord = gridSize - 1
            val maxWallCoord = gridSize - 2

            val w = kotlin.math.min(size.width, size.height)
            val padding = w * 0.032f
            val playableWidth = w - (padding * 2)
            val gap = playableWidth * (0.25f / gridSize)
            val cellSize = (playableWidth - (gap * maxCoord)) / gridSize.toFloat()
            val step = cellSize + gap
            val cellRadius = cellSize * 0.16f
            val boardRadius = w * 0.045f

            // 1. Draw Board Background Container
            val boardBg = if (state.rules.mode == GameMode.RACE_MODE) RaceTrackSurface else theme.boardSurface
            val boardBorder = if (state.rules.mode == GameMode.RACE_MODE) RaceFinishGreen.copy(alpha = 0.7f) else theme.cellBorderColor

            drawRoundRect(
                color = boardBg,
                topLeft = Offset(0f, 0f),
                size = Size(w, w),
                cornerRadius = CornerRadius(boardRadius, boardRadius)
            )
            drawRoundRect(
                color = boardBorder,
                topLeft = Offset(0f, 0f),
                size = Size(w, w),
                cornerRadius = CornerRadius(boardRadius, boardRadius),
                style = Stroke(width = if (state.rules.mode == GameMode.RACE_MODE) 3.5f else 2.5f)
            )

            // 2. Goal Lines indicators
            val isQuadMode = state.rules.mode == GameMode.QUAD_MODE
            val isRaceMode = state.rules.mode == GameMode.RACE_MODE

            val p1GoalColor = theme.p1Primary.copy(alpha = 0.22f)
            val p2GoalColor = theme.p2Primary.copy(alpha = 0.22f)

            // Paint for coordinates
            val textPaint = Paint().apply {
                color = theme.cellBorderColor.copy(alpha = 0.55f).toArgb()
                textSize = cellSize * 0.24f
                isAntiAlias = true
                textAlign = Paint.Align.LEFT
                typeface = Typeface.DEFAULT_BOLD
            }

            // 3. Draw Grid Cells
            val rowLabels = (1..gridSize).map { it.toString() }.let { if (isFlipped) it else it.reversed() }
            val colLabels = (0 until gridSize).map { ('a' + it).toString() }.let { if (isFlipped) it else it.reversed() }

            val p1GoalRow = RuleEngine.getPlayerGoalRow(state, PlayerId.PLAYER_1)
            val p2GoalRow = RuleEngine.getPlayerGoalRow(state, PlayerId.PLAYER_2)

            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    val cellLeft = padding + c * step
                    val cellTop = padding + r * step

                    val modelC = if (isFlipped) maxCoord - c else c
                    val modelR = if (isFlipped) maxCoord - r else r

                    val isP1GoalRow = !isQuadMode && !isRaceMode && (modelR == p1GoalRow)
                    val isP2GoalRow = !isQuadMode && !isRaceMode && (modelR == p2GoalRow)
                    val isQuadCenterCell = isQuadMode && modelR == gridSize / 2 && modelC == gridSize / 2
                    val isRaceFinishCell = isRaceMode && (modelR == RuleEngine.RACE_GOAL_ROW)
                    val isRaceStartCell = isRaceMode && (modelR == maxCoord) && (modelC == (gridSize / 2 - 1) || modelC == (gridSize / 2 + 1))

                    val bgCellColor = when {
                        isQuadCenterCell -> GoldRating.copy(alpha = 0.25f + (glowAlpha * 0.15f))
                        isRaceFinishCell -> if ((r + c) % 2 == 0) SuccessGreen.copy(alpha = 0.48f) else SuccessGreen.copy(alpha = 0.30f)
                        isRaceStartCell -> RaceStartGreen.copy(alpha = 0.22f)
                        isQuadMode && modelR == maxCoord && modelC == gridSize / 2 -> theme.p1Primary.copy(alpha = 0.18f)
                        isQuadMode && modelR == 0 && modelC == gridSize / 2 -> theme.p2Primary.copy(alpha = 0.18f)
                        isQuadMode && modelR == gridSize / 2 && modelC == 0 -> Player3Primary.copy(alpha = 0.18f)
                        isQuadMode && modelR == gridSize / 2 && modelC == maxCoord -> Player4Primary.copy(alpha = 0.18f)
                        isP1GoalRow -> p1GoalColor
                        isP2GoalRow -> p2GoalColor
                        (r + c) % 2 == 0 -> theme.cellColor
                        else -> theme.cellAltColor
                    }

                    drawRoundRect(
                        color = bgCellColor,
                        topLeft = Offset(cellLeft, cellTop),
                        size = Size(cellSize, cellSize),
                        cornerRadius = CornerRadius(cellRadius, cellRadius)
                    )

                    // Cell border
                    val cellBorderColor = when {
                        isQuadCenterCell -> GoldRating
                        isRaceFinishCell -> SuccessGreen
                        isRaceStartCell -> RaceStartGreen.copy(alpha = 0.6f)
                        isP1GoalRow -> theme.p1Primary.copy(alpha = 0.45f)
                        isP2GoalRow -> theme.p2Primary.copy(alpha = 0.45f)
                        else -> theme.cellBorderColor
                    }

                    drawRoundRect(
                        color = cellBorderColor,
                        topLeft = Offset(cellLeft, cellTop),
                        size = Size(cellSize, cellSize),
                        cornerRadius = CornerRadius(cellRadius, cellRadius),
                        style = Stroke(width = if (isQuadCenterCell || isRaceFinishCell) 2.2f else 1.2f)
                    )

                    // Special indicator inside Race Finish cells
                    if (isRaceFinishCell) {
                        val centerCellX = cellLeft + (cellSize / 2f)
                        val centerCellY = cellTop + (cellSize / 2f)
                        drawCircle(
                            color = SuccessGreen.copy(alpha = glowAlpha * 0.7f),
                            radius = cellSize * 0.22f * pulseScale,
                            center = Offset(centerCellX, centerCellY),
                            style = Stroke(width = 1.5f)
                        )
                        drawCircle(
                            color = SuccessGreen,
                            radius = cellSize * 0.12f,
                            center = Offset(centerCellX, centerCellY)
                        )
                    }

                    // Special indicator inside Quad Center cell
                    if (isQuadCenterCell) {
                        val centerCellX = cellLeft + (cellSize / 2f)
                        val centerCellY = cellTop + (cellSize / 2f)
                        drawCircle(
                            color = GoldRating.copy(alpha = glowAlpha),
                            radius = (cellSize * 0.32f) * pulseScale,
                            center = Offset(centerCellX, centerCellY),
                            style = Stroke(width = 2f)
                        )
                        drawCircle(
                            color = GoldRating,
                            radius = cellSize * 0.15f,
                            center = Offset(centerCellX, centerCellY)
                        )
                    }

                    // Draw Obstacle Pillar if present on this cell
                    val hasObstacle = state.obstacles.any { it.x == modelC && it.y == modelR }
                    if (hasObstacle) {
                        drawRoundRect(
                            color = Color(0xFF334155),
                            topLeft = Offset(cellLeft + 4f, cellTop + 4f),
                            size = Size(cellSize - 8f, cellSize - 8f),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = Color(0xFF64748B),
                            topLeft = Offset(cellLeft + 8f, cellTop + 8f),
                            size = Size(cellSize - 16f, cellSize - 16f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }

                    // Draw coordinates on edge cells
                    if (theme.showCoordinates) {
                        if (c == 0) {
                            drawContext.canvas.nativeCanvas.drawText(
                                rowLabels[r],
                                cellLeft + (cellSize * 0.08f),
                                cellTop + (cellSize * 0.30f),
                                textPaint
                            )
                        }
                        if (r == maxCoord) {
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
                    val dispX = if (isFlipped) maxCoord - move.x else move.x
                    val dispY = if (isFlipped) maxCoord - move.y else move.y
                    val moveLeft = padding + dispX * step
                    val moveTop = padding + dispY * step
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

            // Ghost Path Trajectory (Personal Best Run)
            if (isGhostEnabled && ghostTrajectory.isNotEmpty()) {
                for (ghostPos in ghostTrajectory) {
                    val dispX = if (isFlipped) maxCoord - ghostPos.x else ghostPos.x
                    val dispY = if (isFlipped) maxCoord - ghostPos.y else ghostPos.y
                    val centerX = padding + dispX * step + (cellSize / 2f)
                    val centerY = padding + dispY * step + (cellSize / 2f)
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.25f),
                        radius = cellSize * 0.16f,
                        center = Offset(centerX, centerY)
                    )
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.6f),
                        radius = cellSize * 0.06f,
                        center = Offset(centerX, centerY)
                    )
                }
            }

            // 5. Draw Placed Walls
            for (wall in state.walls) {
                val (wColor, wBorder) = when (wall.placedBy) {
                    PlayerId.PLAYER_1 -> Pair(theme.p1WallColor, theme.p1WallBorder)
                    PlayerId.PLAYER_2 -> Pair(theme.p2WallColor, theme.p2WallBorder)
                    PlayerId.PLAYER_3 -> Pair(Player3Primary, Player3Dark)
                    PlayerId.PLAYER_4 -> Pair(Player4Primary, Player4Dark)
                }

                val dispWall = if (isFlipped) wall.copy(x = maxWallCoord - wall.x, y = maxWallCoord - wall.y) else wall

                drawWall(
                    wall = dispWall,
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

                val dispPreview = if (isFlipped) previewWall.copy(x = maxWallCoord - previewWall.x, y = maxWallCoord - previewWall.y) else previewWall

                drawWall(
                    wall = dispPreview,
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
            val p1DispPos = if (isFlipped) Position(maxCoord - state.player1.position.x, maxCoord - state.player1.position.y) else state.player1.position
            drawPawn(
                pos = p1DispPos,
                padding = padding,
                step = step,
                cellSize = cellSize,
                primaryColor = theme.p1Primary,
                darkColor = theme.p1Dark,
                glowColor = theme.p1Glow,
                isSelected = (selectedPawn == PlayerId.PLAYER_1 || (state.currentTurn == PlayerId.PLAYER_1 && isLocalTurn)),
                pulseScale = if (state.currentTurn == PlayerId.PLAYER_1) pulseScale else 1f,
                glowAlpha = glowAlpha,
                shieldActive = state.player1.shieldTurns > 0,
                phaseActive = state.player1.phasePassCharges > 0
            )

            // Player 2 (P2)
            val p2DispPos = if (isFlipped) Position(maxCoord - state.player2.position.x, maxCoord - state.player2.position.y) else state.player2.position
            drawPawn(
                pos = p2DispPos,
                padding = padding,
                step = step,
                cellSize = cellSize,
                primaryColor = theme.p2Primary,
                darkColor = theme.p2Dark,
                glowColor = theme.p2Glow,
                isSelected = (selectedPawn == PlayerId.PLAYER_2 || (state.currentTurn == PlayerId.PLAYER_2 && isLocalTurn)),
                pulseScale = if (state.currentTurn == PlayerId.PLAYER_2) pulseScale else 1f,
                glowAlpha = glowAlpha,
                shieldActive = state.player2.shieldTurns > 0,
                phaseActive = state.player2.phasePassCharges > 0
            )

            // Player 3 (P3)
            state.player3?.let { p3 ->
                val p3DispPos = if (isFlipped) Position(maxCoord - p3.position.x, maxCoord - p3.position.y) else p3.position
                drawPawn(
                    pos = p3DispPos,
                    padding = padding,
                    step = step,
                    cellSize = cellSize,
                    primaryColor = Player3Primary,
                    darkColor = Player3Dark,
                    glowColor = Player3Glow,
                    isSelected = (selectedPawn == PlayerId.PLAYER_3 || (state.currentTurn == PlayerId.PLAYER_3 && isLocalTurn)),
                    pulseScale = if (state.currentTurn == PlayerId.PLAYER_3) pulseScale else 1f,
                    glowAlpha = glowAlpha
                )
            }

            // Player 4 (P4)
            state.player4?.let { p4 ->
                val p4DispPos = if (isFlipped) Position(maxCoord - p4.position.x, maxCoord - p4.position.y) else p4.position
                drawPawn(
                    pos = p4DispPos,
                    padding = padding,
                    step = step,
                    cellSize = cellSize,
                    primaryColor = Player4Primary,
                    darkColor = Player4Dark,
                    glowColor = Player4Glow,
                    isSelected = (selectedPawn == PlayerId.PLAYER_4 || (state.currentTurn == PlayerId.PLAYER_4 && isLocalTurn)),
                    pulseScale = if (state.currentTurn == PlayerId.PLAYER_4) pulseScale else 1f,
                    glowAlpha = glowAlpha
                )
            }
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
    glowAlpha: Float,
    shieldActive: Boolean = false,
    phaseActive: Boolean = false
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

    // Shield Aura if active
    if (shieldActive) {
        drawCircle(
            color = NeonCyan.copy(alpha = glowAlpha),
            radius = pawnRadius * 1.55f * pulseScale,
            center = Offset(centerX, centerY),
            style = Stroke(width = 4f)
        )
    }

    // Phase Aura if active
    if (phaseActive) {
        drawCircle(
            color = Color(0xFFD946EF).copy(alpha = 0.5f),
            radius = pawnRadius * 1.4f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 3f)
        )
    }

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
    val wallThickness = kotlin.math.max(gap * 1.45f, 16f)
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
