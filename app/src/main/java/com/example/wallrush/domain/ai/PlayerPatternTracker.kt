package com.example.wallrush.domain.ai

import android.content.Context
import android.content.SharedPreferences
import com.example.wallrush.domain.model.Position
import com.example.wallrush.domain.model.Wall
import com.example.wallrush.domain.model.WallOrientation

data class LearnedPlayerStyle(
    val rightwardEscapes: Int = 0,
    val leftwardEscapes: Int = 0,
    val averageTurnWallDistance: Float = 2.0f,
    val turnsSampleCount: Int = 0,
    val offensiveWallCount: Int = 0,
    val defensiveWallCount: Int = 0,
    val totalMovesObserved: Int = 0
) {
    val escapesRightTendency: Float
        get() {
            val total = rightwardEscapes + leftwardEscapes
            return if (total > 0) rightwardEscapes.toFloat() / total else 0.5f
        }

    val prefersRightEscape: Boolean
        get() = escapesRightTendency >= 0.58f

    val prefersLeftEscape: Boolean
        get() = escapesRightTendency <= 0.42f
}

object PlayerPatternTracker {

    private const val PREFS_NAME = "wallrush_player_learning"
    private const val KEY_RIGHT_ESCAPES = "stat_right_escapes"
    private const val KEY_LEFT_ESCAPES = "stat_left_escapes"
    private const val KEY_AVG_DISTANCE = "stat_avg_turn_distance"
    private const val KEY_TURNS_COUNT = "stat_turns_count"
    private const val KEY_OFFENSIVE_WALLS = "stat_offensive_walls"
    private const val KEY_DEFENSIVE_WALLS = "stat_defensive_walls"
    private const val KEY_TOTAL_MOVES = "stat_total_moves"

    @Volatile
    private var cachedStyle: LearnedPlayerStyle? = null

    fun getPlayerStyle(context: Context): LearnedPlayerStyle {
        cachedStyle?.let { return it }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val style = LearnedPlayerStyle(
            rightwardEscapes = prefs.getInt(KEY_RIGHT_ESCAPES, 10),
            leftwardEscapes = prefs.getInt(KEY_LEFT_ESCAPES, 10),
            averageTurnWallDistance = prefs.getFloat(KEY_AVG_DISTANCE, 1.8f),
            turnsSampleCount = prefs.getInt(KEY_TURNS_COUNT, 8),
            offensiveWallCount = prefs.getInt(KEY_OFFENSIVE_WALLS, 5),
            defensiveWallCount = prefs.getInt(KEY_DEFENSIVE_WALLS, 5),
            totalMovesObserved = prefs.getInt(KEY_TOTAL_MOVES, 30)
        )
        cachedStyle = style
        return style
    }

    fun recordPlayerMove(
        context: Context,
        from: Position,
        to: Position,
        closestWallDistance: Int
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val isRightward = to.x > from.x
        val isLeftward = to.x < from.x

        if (isRightward) {
            val cur = prefs.getInt(KEY_RIGHT_ESCAPES, 10)
            editor.putInt(KEY_RIGHT_ESCAPES, cur + 1)
        } else if (isLeftward) {
            val cur = prefs.getInt(KEY_LEFT_ESCAPES, 10)
            editor.putInt(KEY_LEFT_ESCAPES, cur + 1)
        }

        // Record turn distance when changing lateral direction near walls
        if (closestWallDistance in 1..4 && (isRightward || isLeftward)) {
            val currentAvg = prefs.getFloat(KEY_AVG_DISTANCE, 1.8f)
            val currentCount = prefs.getInt(KEY_TURNS_COUNT, 8)
            val newAvg = ((currentAvg * currentCount) + closestWallDistance) / (currentCount + 1)
            editor.putFloat(KEY_AVG_DISTANCE, newAvg)
            editor.putInt(KEY_TURNS_COUNT, currentCount + 1)
        }

        val total = prefs.getInt(KEY_TOTAL_MOVES, 30) + 1
        editor.putInt(KEY_TOTAL_MOVES, total)
        editor.apply()

        cachedStyle = null
    }

    fun recordPlayerWallPlacement(
        context: Context,
        wall: Wall,
        playerPos: Position,
        oppPos: Position
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val distToPlayer = kotlin.math.abs(wall.x - playerPos.x) + kotlin.math.abs(wall.y - playerPos.y)
        val distToOpp = kotlin.math.abs(wall.x - oppPos.x) + kotlin.math.abs(wall.y - oppPos.y)

        if (distToOpp < distToPlayer) {
            val count = prefs.getInt(KEY_OFFENSIVE_WALLS, 5) + 1
            editor.putInt(KEY_OFFENSIVE_WALLS, count)
        } else {
            val count = prefs.getInt(KEY_DEFENSIVE_WALLS, 5) + 1
            editor.putInt(KEY_DEFENSIVE_WALLS, count)
        }
        editor.apply()
        cachedStyle = null
    }
}
