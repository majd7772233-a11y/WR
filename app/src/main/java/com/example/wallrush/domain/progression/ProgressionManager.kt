package com.example.wallrush.domain.progression

import android.content.Context
import android.content.SharedPreferences
import com.example.wallrush.domain.model.AIDifficulty
import kotlin.math.min

data class LevelInfo(
    val level: Int,
    val currentXp: Long,
    val xpForCurrentLevel: Long,
    val xpForNextLevel: Long,
    val progress: Float,
    val title: String
)

object ProgressionManager {

    private const val PREFS_NAME = "wallrush_progression"
    private const val KEY_TOTAL_XP = "total_xp"
    private const val KEY_LEVEL = "current_level"

    const val MAX_LEVEL = 100

    /**
     * Balanced, rewarding XP curve for Levels 1 to 100:
     * Level 1 -> Level 2: 430 XP (requires 3-4 standard wins or 2 S+ wins)
     * Level 2 -> Level 3: 1,020 XP total
     * Level 10 -> ~10,000 XP total
     */
    fun xpRequiredForLevel(level: Int): Long {
        if (level <= 1) return 0L
        val lvl = level - 1
        return (lvl * 350L) + (lvl * lvl * 80L)
    }

    fun calculateLevelFromXp(totalXp: Long): Int {
        var lvl = 1
        while (lvl < MAX_LEVEL && totalXp >= xpRequiredForLevel(lvl + 1)) {
            lvl++
        }
        return min(lvl, MAX_LEVEL)
    }

    fun getLevelInfo(totalXp: Long): LevelInfo {
        val level = calculateLevelFromXp(totalXp)
        val currentLevelBase = xpRequiredForLevel(level)
        val nextLevelTarget = if (level >= MAX_LEVEL) currentLevelBase else xpRequiredForLevel(level + 1)
        val needed = (nextLevelTarget - currentLevelBase).coerceAtLeast(1L)
        val earnedInLevel = (totalXp - currentLevelBase).coerceAtLeast(0L)
        val progress = if (level >= MAX_LEVEL) 1.0f else (earnedInLevel.toFloat() / needed).coerceIn(0f, 1f)

        return LevelInfo(
            level = level,
            currentXp = totalXp,
            xpForCurrentLevel = currentLevelBase,
            xpForNextLevel = nextLevelTarget,
            progress = progress,
            title = getTierTitle(level)
        )
    }

    fun getTierTitle(level: Int): String {
        return when {
            level >= 100 -> "Legendary Champion"
            level >= 90 -> "Grandmaster Rush"
            level >= 80 -> "Labyrinth Overlord"
            level >= 70 -> "Nightmare Conqueror"
            level >= 60 -> "Tactical Architect"
            level >= 50 -> "Untouchable Runner"
            level >= 40 -> "Speed Phantom"
            level >= 30 -> "Wall Master"
            level >= 20 -> "Pathfinder"
            level >= 10 -> "Apprentice Runner"
            else -> "Novice"
        }
    }

    fun getTierTitleArabic(level: Int): String {
        return when {
            level >= 100 -> "البطل الأسطوري"
            level >= 90 -> "الأستاذ الأكبر"
            level >= 80 -> "سيد المتاهات"
            level >= 70 -> "قاهر الكوابيس"
            level >= 60 -> "مهندس التكتيك"
            level >= 50 -> "العدّاء المحصّن"
            level >= 40 -> "شبح السرعة"
            level >= 30 -> "سيد الجدران"
            level >= 20 -> "مستكشف المسارات"
            level >= 10 -> "عدّاء صاعد"
            else -> "مبتدئ"
        }
    }

    fun calculateMatchWinXp(
        difficulty: AIDifficulty,
        moveCount: Int,
        maxCombo: Int,
        nearMisses: Int,
        isPerfectRun: Boolean
    ): Long {
        val baseDifficultyXp = when (difficulty) {
            AIDifficulty.EASY -> 120L
            AIDifficulty.MEDIUM -> 200L
            AIDifficulty.HARD -> 350L
            AIDifficulty.EXPERT -> 500L
            AIDifficulty.NIGHTMARE -> 750L
            AIDifficulty.INSANE -> 1100L
        }

        val comboBonus = maxCombo * 15L
        val nearMissBonus = nearMisses * 35L
        val survivalBonus = (moveCount * 4L).coerceAtMost(200L)
        val perfectBonus = if (isPerfectRun) 300L else 0L

        return baseDifficultyXp + comboBonus + nearMissBonus + survivalBonus + perfectBonus
    }

    fun getTotalXp(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_TOTAL_XP, 0L)
    }

    fun addXp(context: Context, earnedXp: Long): Pair<LevelInfo, Boolean> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val oldTotal = prefs.getLong(KEY_TOTAL_XP, 0L)
        val oldLevel = calculateLevelFromXp(oldTotal)

        val newTotal = oldTotal + earnedXp
        val newLevel = calculateLevelFromXp(newTotal)

        prefs.edit()
            .putLong(KEY_TOTAL_XP, newTotal)
            .putInt(KEY_LEVEL, newLevel)
            .apply()

        val leveledUp = newLevel > oldLevel
        return Pair(getLevelInfo(newTotal), leveledUp)
    }
}
