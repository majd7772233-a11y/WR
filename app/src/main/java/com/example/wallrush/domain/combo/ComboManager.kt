package com.example.wallrush.domain.combo

import com.example.wallrush.domain.model.GameState
import com.example.wallrush.domain.model.PlayerId
import com.example.wallrush.domain.model.Position
import com.example.wallrush.domain.model.Wall
import com.example.wallrush.domain.model.WallOrientation
import kotlin.math.abs
import com.example.wallrush.domain.engine.PathFinder

data class MoveFeedback(
    val isNearMiss: Boolean,
    val isDodge: Boolean,
    val comboCount: Int,
    val energyGained: Int,
    val xpBonus: Long,
    val feedbackMessageEn: String?,
    val feedbackMessageAr: String?
)

data class MatchRating(
    val accuracyPercent: Int,
    val dodgesCount: Int,
    val nearMissesCount: Int,
    val powerupsUsedCount: Int,
    val timeSeconds: Long,
    val maxCombo: Int,
    val stars: Int, // 1..5
    val rankGrade: String, // "S+", "S", "A", "B", "C"
    val isPerfectRun: Boolean,
    val totalBonusXp: Long
)

object ComboManager {

    /**
     * Checks if a move is genuinely dangerous/clutch:
     * - Only triggers on genuine tactical accomplishments (escaping tight wall entrapment, leaping over opponent,
     *   flanking immediate enemy barriers, or decisive breakthrough).
     * - Avoids triggering on standard open-field moves or false boundary detections.
     */
    fun evaluateMove(
        from: Position,
        to: Position,
        walls: List<Wall>,
        lastWallPlaced: Wall?,
        currentCombo: Int,
        opponentPos: Position? = null,
        goalY: Int = 0,
        gridSize: Int = 9
    ): MoveFeedback {
        var isNearMiss = false
        var isDodge = false
        var customMsgEn: String? = null
        var customMsgAr: String? = null

        // 1. Check if the player performed a pawn leap over or diagonally around the opponent
        val isOpponentJump = opponentPos != null && (
            (abs(to.x - from.x) == 2 && from.y == to.y) ||
            (abs(to.y - from.y) == 2 && from.x == to.x) ||
            (abs(to.x - from.x) == 1 && abs(to.y - from.y) == 1 && abs(opponentPos.x - from.x) <= 1 && abs(opponentPos.y - from.y) <= 1)
        )

        // 2. Count ONLY actual walls blocking adjacent tiles (ignore open board boundaries)
        val wallsAroundFrom = if (walls.isNotEmpty()) {
            listOf(
                Position(from.x + 1, from.y),
                Position(from.x - 1, from.y),
                Position(from.x, from.y + 1),
                Position(from.x, from.y - 1)
            ).count { neighbor ->
                neighbor.isWithinBounds(gridSize) && PathFinder.isPassageBlocked(from, neighbor, walls)
            }
        } else 0

        // 3. Check proximity to enemy's most recent wall placement
        val isHuggingRecentWall = if (lastWallPlaced != null) {
            val distTo = abs(lastWallPlaced.x - to.x) + abs(lastWallPlaced.y - to.y)
            val distFrom = abs(lastWallPlaced.x - from.x) + abs(lastWallPlaced.y - from.y)
            distTo <= 1 && distFrom <= 2
        } else false

        // 4. Check if move advances toward the goal
        val isAdvancing = if (goalY == 0) to.y < from.y else to.y > from.y

        // Tactical classification
        if (isOpponentJump) {
            isNearMiss = true
            customMsgEn = "⚡ LEAP STRIKE!"
            customMsgAr = "⚡ قفزة مباغتة فوق الخصم!"
        } else if (wallsAroundFrom >= 2 && isAdvancing) {
            // True entrapment escape
            isNearMiss = true
            customMsgEn = "⚡ CLUTCH ESCAPE!"
            customMsgAr = "⚡ إفلات تكتيكي من الحصار!"
        } else if (isHuggingRecentWall && wallsAroundFrom >= 1 && isAdvancing) {
            // Smart wall flank
            isDodge = true
            customMsgEn = "🛡️ TACTICAL FLANK!"
            customMsgAr = "🛡️ التفاف تكتيكي ذكي!"
        }

        val nextCombo = if (isNearMiss || isDodge) currentCombo + 1 else 0

        val energyGained = when {
            isNearMiss -> 25
            isDodge -> 12
            else -> 0
        }

        val xpBonus = when {
            isNearMiss -> 50L + (nextCombo * 15L)
            isDodge -> 25L + (nextCombo * 10L)
            else -> 0L
        }

        val (msgEn, msgAr) = when {
            customMsgEn != null && nextCombo >= 3 -> Pair("🔥 $customMsgEn x$nextCombo!", "🔥 $customMsgAr x$nextCombo!")
            customMsgEn != null -> Pair(customMsgEn, customMsgAr)
            nextCombo >= 4 -> Pair("🔥 COMBO x$nextCombo!", "🔥 كومبو مناورة x$nextCombo!")
            else -> Pair(null, null)
        }

        return MoveFeedback(
            isNearMiss = isNearMiss,
            isDodge = isDodge,
            comboCount = nextCombo,
            energyGained = energyGained,
            xpBonus = xpBonus,
            feedbackMessageEn = msgEn,
            feedbackMessageAr = msgAr
        )
    }

    /**
     * Calculates realistic, authentic end-of-match rating, actual move counts, decision accuracy, and fair XP.
     */
    fun calculateMatchRating(
        state: GameState,
        durationSeconds: Long,
        localPlayerId: PlayerId,
        powerupsUsed: Int = 0
    ): MatchRating {
        val playerPawnMoves = state.eventHistory
            .filterIsInstance<com.example.wallrush.domain.model.GameEvent.PawnMoved>()
            .filter { it.player == localPlayerId }
        val playerWallsPlaced = state.eventHistory
            .filterIsInstance<com.example.wallrush.domain.model.GameEvent.WallPlaced>()
            .filter { it.player == localPlayerId }
        val opponentWallsPlaced = state.eventHistory
            .filterIsInstance<com.example.wallrush.domain.model.GameEvent.WallPlaced>()
            .filter { it.player != localPlayerId }

        val myMoveCount = playerPawnMoves.size.coerceAtLeast(1)
        val nearMisses = state.nearMissCount
        val maxCombo = state.maxCombo
        val isWon = state.winner == localPlayerId

        // Baseline direct distance from row 8 to row 0 is 8 steps
        val baseShortestPath = 8
        // Account for opponent walls forcing detours
        val effectiveShortestPath = (baseShortestPath + (opponentWallsPlaced.size * 0.75f).toInt()).coerceAtLeast(baseShortestPath)
        val accuracy = if (isWon) {
            ((effectiveShortestPath.toFloat() / myMoveCount) * 100).toInt().coerceIn(55, 100)
        } else {
            ((effectiveShortestPath.toFloat() / myMoveCount.coerceAtLeast(12)) * 80).toInt().coerceIn(35, 75)
        }

        val dodgesCount = nearMisses.coerceAtLeast(0)

        val isPerfect = isWon && accuracy >= 88 && myMoveCount <= 14 && durationSeconds <= 120

        val stars = when {
            isPerfect -> 5
            isWon && accuracy >= 78 -> 4
            isWon -> 3
            accuracy >= 65 -> 2
            else -> 1
        }

        val grade = when (stars) {
            5 -> "S+"
            4 -> "S"
            3 -> "A"
            2 -> "B"
            else -> "C"
        }

        // Fair, controlled bonus XP: losses give negligible effort XP, victories award reasonable progress
        val bonusXp = if (isWon) {
            when (stars) {
                5 -> 180L + (maxCombo * 10L)
                4 -> 120L + (maxCombo * 5L)
                else -> 70L
            }
        } else {
            if (stars == 2) 15L else 5L
        }

        return MatchRating(
            accuracyPercent = accuracy,
            dodgesCount = dodgesCount,
            nearMissesCount = nearMisses,
            powerupsUsedCount = powerupsUsed,
            timeSeconds = durationSeconds,
            maxCombo = maxCombo,
            stars = stars,
            rankGrade = grade,
            isPerfectRun = isPerfect,
            totalBonusXp = bonusXp
        )
    }
}
