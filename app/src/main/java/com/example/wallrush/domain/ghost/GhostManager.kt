package com.example.wallrush.domain.ghost

import android.content.Context
import android.content.SharedPreferences
import com.example.wallrush.domain.model.Position
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class GhostRecord(
    val matchDurationSeconds: Long,
    val totalMoves: Int,
    val trajectory: List<Position>
)

object GhostManager {

    private const val PREFS_NAME = "wallrush_ghost_record"
    private const val KEY_BEST_DURATION = "best_ghost_duration"
    private const val KEY_BEST_MOVES = "best_ghost_moves"
    private const val KEY_BEST_TRAJECTORY = "best_ghost_trajectory"
    private const val KEY_GHOST_ENABLED = "ghost_enabled"

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, Position::class.java)

    fun isGhostEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_GHOST_ENABLED, true)
    }

    fun setGhostEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_GHOST_ENABLED, enabled).apply()
    }

    fun getBestGhost(context: Context): GhostRecord? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_BEST_TRAJECTORY, null) ?: return null
        val duration = prefs.getLong(KEY_BEST_DURATION, 999L)
        val moves = prefs.getInt(KEY_BEST_MOVES, 999)

        return try {
            val adapter = moshi.adapter<List<Position>>(listType)
            val trajectory = adapter.fromJson(json) ?: return null
            GhostRecord(matchDurationSeconds = duration, totalMoves = moves, trajectory = trajectory)
        } catch (e: Exception) {
            null
        }
    }

    fun saveIfBestGhost(context: Context, durationSeconds: Long, moves: Int, trajectory: List<Position>) {
        if (trajectory.isEmpty()) return
        val currentBest = getBestGhost(context)
        val isBetter = currentBest == null || (moves < currentBest.totalMoves) || (moves == currentBest.totalMoves && durationSeconds < currentBest.matchDurationSeconds)

        if (isBetter) {
            val adapter = moshi.adapter<List<Position>>(listType)
            val json = adapter.toJson(trajectory)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong(KEY_BEST_DURATION, durationSeconds)
                .putInt(KEY_BEST_MOVES, moves)
                .putString(KEY_BEST_TRAJECTORY, json)
                .apply()
        }
    }
}
