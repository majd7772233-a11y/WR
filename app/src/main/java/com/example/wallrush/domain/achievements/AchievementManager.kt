package com.example.wallrush.domain.achievements

import android.content.Context
import android.content.SharedPreferences
import com.example.wallrush.data.local.PlayerProfile
import com.example.wallrush.domain.model.*
import java.util.Calendar

object AchievementManager {

    private const val PREFS_NAME = "wallrush_achievements_prefs"
    private const val KEY_PREFIX_UNLOCKED = "ach_unlocked_"
    private const val KEY_PREFIX_PROGRESS = "ach_prog_"
    private const val KEY_PREFIX_TIMESTAMP = "ach_time_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val ACHIEVEMENT_TEMPLATES = listOf(
        // ================= COMBAT & VICTORY =================
        Achievement(
            id = "first_win",
            titleKey = "ach_first_win_title",
            descKey = "ach_first_win_desc",
            icon = "🥇",
            category = AchievementCategory.COMBAT,
            targetCount = 1,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 50
        ),
        Achievement(
            id = "wins_5",
            titleKey = "ach_wins_5_title",
            descKey = "ach_wins_5_desc",
            icon = "⚔️",
            category = AchievementCategory.COMBAT,
            targetCount = 5,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 100
        ),
        Achievement(
            id = "wins_20",
            titleKey = "ach_wins_20_title",
            descKey = "ach_wins_20_desc",
            icon = "👑",
            category = AchievementCategory.COMBAT,
            targetCount = 20,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 250
        ),
        Achievement(
            id = "wins_50",
            titleKey = "ach_wins_50_title",
            descKey = "ach_wins_50_desc",
            icon = "💎",
            category = AchievementCategory.COMBAT,
            targetCount = 50,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 500
        ),
        Achievement(
            id = "wins_100",
            titleKey = "ach_wins_100_title",
            descKey = "ach_wins_100_desc",
            icon = "🏆",
            category = AchievementCategory.COMBAT,
            targetCount = 100,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 1000
        ),
        Achievement(
            id = "streak_3",
            titleKey = "ach_streak_3_title",
            descKey = "ach_streak_3_desc",
            icon = "🔥",
            category = AchievementCategory.COMBAT,
            targetCount = 3,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 150
        ),
        Achievement(
            id = "streak_5",
            titleKey = "ach_streak_5_title",
            descKey = "ach_streak_5_desc",
            icon = "⚡",
            category = AchievementCategory.COMBAT,
            targetCount = 5,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 300
        ),
        Achievement(
            id = "streak_10",
            titleKey = "ach_streak_10_title",
            descKey = "ach_streak_10_desc",
            icon = "🌟",
            category = AchievementCategory.COMBAT,
            targetCount = 10,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 600
        ),
        Achievement(
            id = "ai_easy_win",
            titleKey = "ach_easy_ai_title",
            descKey = "ach_easy_ai_desc",
            icon = "🌱",
            category = AchievementCategory.COMBAT,
            targetCount = 1,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 60
        ),
        Achievement(
            id = "ai_medium_win",
            titleKey = "ach_med_ai_title",
            descKey = "ach_med_ai_desc",
            icon = "⚔️",
            category = AchievementCategory.COMBAT,
            targetCount = 1,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 100
        ),
        Achievement(
            id = "ai_hard_win",
            titleKey = "ach_hard_ai_title",
            descKey = "ach_hard_ai_desc",
            icon = "🤖",
            category = AchievementCategory.COMBAT,
            targetCount = 1,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 200
        ),
        Achievement(
            id = "flawless_victory",
            titleKey = "ach_flawless_title",
            descKey = "ach_flawless_desc",
            icon = "🎯",
            category = AchievementCategory.COMBAT,
            targetCount = 1,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 250
        ),
        Achievement(
            id = "comeback_king",
            titleKey = "ach_comeback_title",
            descKey = "ach_comeback_desc",
            icon = "🛡️",
            category = AchievementCategory.COMBAT,
            targetCount = 1,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 200
        ),

        // ================= TACTICS & WALLS =================
        Achievement(
            id = "walls_10",
            titleKey = "ach_walls_10_title",
            descKey = "ach_walls_10_desc",
            icon = "🧱",
            category = AchievementCategory.TACTICS,
            targetCount = 10,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 50
        ),
        Achievement(
            id = "walls_25",
            titleKey = "ach_walls_25_title",
            descKey = "ach_walls_25_desc",
            icon = "🧱",
            category = AchievementCategory.TACTICS,
            targetCount = 25,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 100
        ),
        Achievement(
            id = "walls_50",
            titleKey = "ach_walls_50_title",
            descKey = "ach_walls_50_desc",
            icon = "🏰",
            category = AchievementCategory.TACTICS,
            targetCount = 50,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 180
        ),
        Achievement(
            id = "walls_100",
            titleKey = "ach_walls_100_title",
            descKey = "ach_walls_100_desc",
            icon = "🏯",
            category = AchievementCategory.TACTICS,
            targetCount = 100,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 300
        ),
        Achievement(
            id = "walls_250",
            titleKey = "ach_walls_250_title",
            descKey = "ach_walls_250_desc",
            icon = "🗿",
            category = AchievementCategory.TACTICS,
            targetCount = 250,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 600
        ),
        Achievement(
            id = "minimalist_win",
            titleKey = "ach_minimalist_title",
            descKey = "ach_minimalist_desc",
            icon = "🎯",
            category = AchievementCategory.TACTICS,
            targetCount = 1,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 150
        ),
        Achievement(
            id = "wall_less_victory",
            titleKey = "ach_wall_less_title",
            descKey = "ach_wall_less_desc",
            icon = "👻",
            category = AchievementCategory.TACTICS,
            targetCount = 1,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 300
        ),
        Achievement(
            id = "zero_waste",
            titleKey = "ach_zero_waste_title",
            descKey = "ach_zero_waste_desc",
            icon = "🏹",
            category = AchievementCategory.TACTICS,
            targetCount = 1,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 160
        ),
        Achievement(
            id = "maze_builder",
            titleKey = "ach_maze_builder_title",
            descKey = "ach_maze_builder_desc",
            icon = "🌀",
            category = AchievementCategory.TACTICS,
            targetCount = 3,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 180
        ),

        // ================= SPEED & RACING =================
        Achievement(
            id = "speed_demon",
            titleKey = "ach_race_win_title",
            descKey = "ach_race_win_desc",
            icon = "🏎️",
            category = AchievementCategory.SPEED,
            targetCount = 1,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 100
        ),
        Achievement(
            id = "race_champion_5",
            titleKey = "ach_race_5_title",
            descKey = "ach_race_5_desc",
            icon = "🏁",
            category = AchievementCategory.SPEED,
            targetCount = 5,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 250
        ),
        Achievement(
            id = "speedy_blitz_win",
            titleKey = "ach_blitz_win_title",
            descKey = "ach_blitz_win_desc",
            icon = "⏱️",
            category = AchievementCategory.SPEED,
            targetCount = 1,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 180
        ),
        Achievement(
            id = "timed_3m_champion",
            titleKey = "ach_timed_3m_title",
            descKey = "ach_timed_3m_desc",
            icon = "⏳",
            category = AchievementCategory.SPEED,
            targetCount = 3,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 220
        ),

        // ================= MULTIPLAYER & P2P =================
        Achievement(
            id = "multiplayer_p2p",
            titleKey = "ach_p2p_title",
            descKey = "ach_p2p_desc",
            icon = "📡",
            category = AchievementCategory.MULTIPLAYER,
            targetCount = 1,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 120
        ),
        Achievement(
            id = "p2p_victor_3",
            titleKey = "ach_p2p_victor_title",
            descKey = "ach_p2p_victor_desc",
            icon = "🤝",
            category = AchievementCategory.MULTIPLAYER,
            targetCount = 3,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 250
        ),
        Achievement(
            id = "quad_champion",
            titleKey = "ach_quad_win_title",
            descKey = "ach_quad_win_desc",
            icon = "🛡️",
            category = AchievementCategory.MULTIPLAYER,
            targetCount = 1,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 200
        ),
        Achievement(
            id = "quad_master_5",
            titleKey = "ach_quad_master_title",
            descKey = "ach_quad_master_desc",
            icon = "👑",
            category = AchievementCategory.MULTIPLAYER,
            targetCount = 5,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 400
        ),
        Achievement(
            id = "lobby_gladiator",
            titleKey = "ach_lobby_title",
            descKey = "ach_lobby_desc",
            icon = "🌐",
            category = AchievementCategory.MULTIPLAYER,
            targetCount = 3,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 200
        ),
        Achievement(
            id = "pass_play_master",
            titleKey = "ach_pass_play_title",
            descKey = "ach_pass_play_desc",
            icon = "📱",
            category = AchievementCategory.MULTIPLAYER,
            targetCount = 3,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 150
        ),

        // ================= SECRETS & SPECIAL =================
        Achievement(
            id = "secret_glitch_master",
            titleKey = "ach_glitch_title",
            descKey = "ach_glitch_desc",
            icon = "🔓",
            category = AchievementCategory.SECRET,
            targetCount = 1,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 350
        ),
        Achievement(
            id = "night_owl",
            titleKey = "ach_night_owl_title",
            descKey = "ach_night_owl_desc",
            icon = "🌙",
            category = AchievementCategory.SECRET,
            targetCount = 1,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 150
        ),
        Achievement(
            id = "fashion_icon",
            titleKey = "ach_fashion_title",
            descKey = "ach_fashion_desc",
            icon = "🎨",
            category = AchievementCategory.SECRET,
            targetCount = 1,
            currentProgress = 0,
            isUnlocked = false,
            rewardXp = 100
        )
    )

    fun loadAllAchievements(context: Context, profile: PlayerProfile?): List<Achievement> {
        val prefs = getPrefs(context)
        return ACHIEVEMENT_TEMPLATES.map { template ->
            val isUnlocked = prefs.getBoolean(KEY_PREFIX_UNLOCKED + template.id, false)
            val timestamp = prefs.getLong(KEY_PREFIX_TIMESTAMP + template.id, 0L).let { if (it > 0) it else null }

            val calculatedProgress = when (template.id) {
                "first_win", "wins_5", "wins_20", "wins_50", "wins_100" -> profile?.wins ?: 0
                "streak_3", "streak_5", "streak_10" -> profile?.bestStreak ?: 0
                "walls_10", "walls_25", "walls_50", "walls_100", "walls_250" -> profile?.wallsPlaced ?: 0
                else -> prefs.getInt(KEY_PREFIX_PROGRESS + template.id, 0)
            }

            val finalUnlocked = isUnlocked || (calculatedProgress >= template.targetCount)
            if (finalUnlocked && !isUnlocked) {
                prefs.edit()
                    .putBoolean(KEY_PREFIX_UNLOCKED + template.id, true)
                    .putLong(KEY_PREFIX_TIMESTAMP + template.id, System.currentTimeMillis())
                    .apply()
            }

            template.copy(
                isUnlocked = finalUnlocked,
                currentProgress = calculatedProgress.coerceAtMost(template.targetCount),
                unlockedAt = timestamp
            )
        }
    }

    fun onMatchFinished(
        context: Context,
        finalState: GameState,
        localPlayerId: PlayerId,
        profile: PlayerProfile,
        isP2PMatch: Boolean,
        onUnlocked: (Achievement) -> Unit
    ) {
        val prefs = getPrefs(context)
        val isWin = finalState.winner == localPlayerId
        val editor = prefs.edit()

        fun checkAndUnlock(id: String, increment: Int = 1) {
            val template = ACHIEVEMENT_TEMPLATES.find { it.id == id } ?: return
            val current = prefs.getInt(KEY_PREFIX_PROGRESS + id, 0) + increment
            editor.putInt(KEY_PREFIX_PROGRESS + id, current)
            val alreadyUnlocked = prefs.getBoolean(KEY_PREFIX_UNLOCKED + id, false)

            if (!alreadyUnlocked && current >= template.targetCount) {
                editor.putBoolean(KEY_PREFIX_UNLOCKED + id, true)
                editor.putLong(KEY_PREFIX_TIMESTAMP + id, System.currentTimeMillis())
                editor.apply()
                onUnlocked(template.copy(isUnlocked = true, currentProgress = template.targetCount))
            }
        }

        // 1. Night Owl Check (Match played between 12 AM and 5 AM)
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (currentHour in 0..5) {
            checkAndUnlock("night_owl")
        }

        // 2. Pass and Play Match
        if (finalState.rules.mode == GameMode.PASS_AND_PLAY) {
            checkAndUnlock("pass_play_master")
        }

        // 3. Walls Tracking
        val wallsUsedByPlayer = finalState.rules.wallsPerPlayer - finalState.getPlayer(localPlayerId).remainingWalls
        if (wallsUsedByPlayer >= 8) {
            checkAndUnlock("maze_builder")
        }

        if (isWin) {
            // Wins & Streaks
            val totalWins = profile.wins + 1
            if (totalWins >= 1) checkAndUnlock("first_win")
            if (totalWins >= 5) checkAndUnlock("wins_5")
            if (totalWins >= 20) checkAndUnlock("wins_20")
            if (totalWins >= 50) checkAndUnlock("wins_50")
            if (totalWins >= 100) checkAndUnlock("wins_100")

            val currentStreak = profile.currentStreak + 1
            if (currentStreak >= 3) checkAndUnlock("streak_3")
            if (currentStreak >= 5) checkAndUnlock("streak_5")
            if (currentStreak >= 10) checkAndUnlock("streak_10")

            // Minimalist / Wall-less Wins
            if (wallsUsedByPlayer <= 2) {
                checkAndUnlock("minimalist_win")
            }
            if (wallsUsedByPlayer == 0) {
                checkAndUnlock("wall_less_victory")
            }
            if (wallsUsedByPlayer >= finalState.rules.wallsPerPlayer) {
                checkAndUnlock("zero_waste")
            }

            // Move counts
            val moveCount = finalState.moveCount
            if (moveCount in 1..16) {
                checkAndUnlock("flawless_victory")
            }

            // AI difficulty victories
            if (finalState.rules.mode == GameMode.VS_AI) {
                when (finalState.rules.aiDifficulty) {
                    AIDifficulty.EASY -> checkAndUnlock("ai_easy_win")
                    AIDifficulty.MEDIUM -> checkAndUnlock("ai_medium_win")
                    AIDifficulty.HARD -> checkAndUnlock("ai_hard_win")
                }
            }

            // Special Modes
            if (finalState.rules.mode == GameMode.RACE_MODE) {
                checkAndUnlock("speed_demon")
                checkAndUnlock("race_champion_5")
            }
            if (finalState.rules.mode == GameMode.QUAD_MODE) {
                checkAndUnlock("quad_champion")
                checkAndUnlock("quad_master_5")
            }
            if (finalState.rules.mode == GameMode.PUBLIC_ROOM || finalState.rules.mode == GameMode.QUICK_MATCH) {
                checkAndUnlock("lobby_gladiator")
            }
            if (finalState.rules.timeLimitSeconds == 180) {
                checkAndUnlock("timed_3m_champion")
            }
            if (isP2PMatch) {
                checkAndUnlock("p2p_victor_3")
            }
        }

        if (isP2PMatch || finalState.rules.mode == GameMode.FRIEND_ROOM) {
            checkAndUnlock("multiplayer_p2p")
        }

        editor.apply()
    }

    fun recordProfileCustomization(context: Context, onUnlocked: (Achievement) -> Unit) {
        val prefs = getPrefs(context)
        val id = "fashion_icon"
        val alreadyUnlocked = prefs.getBoolean(KEY_PREFIX_UNLOCKED + id, false)
        if (!alreadyUnlocked) {
            prefs.edit()
                .putBoolean(KEY_PREFIX_UNLOCKED + id, true)
                .putInt(KEY_PREFIX_PROGRESS + id, 1)
                .putLong(KEY_PREFIX_TIMESTAMP + id, System.currentTimeMillis())
                .apply()
            val template = ACHIEVEMENT_TEMPLATES.find { it.id == id } ?: return
            onUnlocked(template.copy(isUnlocked = true, currentProgress = 1))
        }
    }

    fun unlockSecretGlitch(context: Context, onUnlocked: (Achievement) -> Unit) {
        val prefs = getPrefs(context)
        val id = "secret_glitch_master"
        val alreadyUnlocked = prefs.getBoolean(KEY_PREFIX_UNLOCKED + id, false)
        if (!alreadyUnlocked) {
            prefs.edit()
                .putBoolean(KEY_PREFIX_UNLOCKED + id, true)
                .putInt(KEY_PREFIX_PROGRESS + id, 1)
                .putLong(KEY_PREFIX_TIMESTAMP + id, System.currentTimeMillis())
                .apply()
            val template = ACHIEVEMENT_TEMPLATES.find { it.id == id } ?: return
            onUnlocked(template.copy(isUnlocked = true, currentProgress = 1))
        }
    }
}

