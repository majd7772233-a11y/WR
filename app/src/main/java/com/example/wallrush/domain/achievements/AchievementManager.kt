package com.example.wallrush.domain.achievements

import android.content.Context
import android.content.SharedPreferences
import com.example.wallrush.data.local.PlayerProfile
import com.example.wallrush.domain.model.*
import com.example.wallrush.domain.npc.NPCPersonality
import com.example.wallrush.domain.rank.RankManager
import com.example.wallrush.domain.rank.RankTier
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
        Achievement("first_win", "ach_first_win_title", "ach_first_win_desc", "🥇", AchievementCategory.COMBAT, 1, 0, false, 50),
        Achievement("wins_5", "ach_wins_5_title", "ach_wins_5_desc", "⚔️", AchievementCategory.COMBAT, 5, 0, false, 100),
        Achievement("wins_10", "ach_wins_10_title", "ach_wins_10_desc", "🗡️", AchievementCategory.COMBAT, 10, 0, false, 150),
        Achievement("wins_20", "ach_wins_20_title", "ach_wins_20_desc", "👑", AchievementCategory.COMBAT, 20, 0, false, 250),
        Achievement("wins_50", "ach_wins_50_title", "ach_wins_50_desc", "💎", AchievementCategory.COMBAT, 50, 0, false, 500),
        Achievement("wins_100", "ach_wins_100_title", "ach_wins_100_desc", "🏆", AchievementCategory.COMBAT, 100, 0, false, 1000),
        Achievement("wins_250", "ach_wins_250_title", "ach_wins_250_desc", "🌟", AchievementCategory.COMBAT, 250, 0, false, 2000),
        Achievement("wins_500", "ach_wins_500_title", "ach_wins_500_desc", "🌌", AchievementCategory.COMBAT, 500, 0, false, 5000),

        Achievement("streak_3", "ach_streak_3_title", "ach_streak_3_desc", "🔥", AchievementCategory.COMBAT, 3, 0, false, 150),
        Achievement("streak_5", "ach_streak_5_title", "ach_streak_5_desc", "⚡", AchievementCategory.COMBAT, 5, 0, false, 300),
        Achievement("streak_7", "ach_streak_7_title", "ach_streak_7_desc", "☄️", AchievementCategory.COMBAT, 7, 0, false, 450),
        Achievement("streak_10", "ach_streak_10_title", "ach_streak_10_desc", "🌟", AchievementCategory.COMBAT, 10, 0, false, 750),
        Achievement("streak_15", "ach_streak_15_title", "ach_streak_15_desc", "💥", AchievementCategory.COMBAT, 15, 0, false, 1200),
        Achievement("streak_20", "ach_streak_20_title", "ach_streak_20_desc", "👑", AchievementCategory.COMBAT, 20, 0, false, 2500),

        Achievement("flawless_victory", "ach_flawless_title", "ach_flawless_desc", "🎯", AchievementCategory.COMBAT, 1, 0, false, 250),
        Achievement("ultra_flawless", "ach_ultra_flawless_title", "ach_ultra_flawless_desc", "🌠", AchievementCategory.COMBAT, 1, 0, false, 500),
        Achievement("comeback_king", "ach_comeback_title", "ach_comeback_desc", "🛡️", AchievementCategory.COMBAT, 1, 0, false, 300),
        Achievement("miracle_turnaround", "ach_miracle_title", "ach_miracle_desc", "✨", AchievementCategory.COMBAT, 1, 0, false, 600),
        Achievement("clutch_survival", "ach_clutch_title", "ach_clutch_desc", "🧗", AchievementCategory.COMBAT, 1, 0, false, 250),

        // ================= AI DIFFICULTIES & PERSONALITIES =================
        Achievement("ai_easy_win", "ach_easy_ai_title", "ach_easy_ai_desc", "🌱", AchievementCategory.COMBAT, 1, 0, false, 60),
        Achievement("ai_medium_win", "ach_med_ai_title", "ach_med_ai_desc", "⚔️", AchievementCategory.COMBAT, 1, 0, false, 100),
        Achievement("ai_hard_win", "ach_hard_ai_title", "ach_hard_ai_desc", "🤖", AchievementCategory.COMBAT, 1, 0, false, 200),
        Achievement("ai_expert_win", "ach_expert_ai_title", "ach_expert_ai_desc", "🟣", AchievementCategory.COMBAT, 1, 0, false, 350),
        Achievement("ai_nightmare_win", "ach_nightmare_ai_title", "ach_nightmare_ai_desc", "💀", AchievementCategory.COMBAT, 1, 0, false, 600),
        Achievement("ai_insane_win", "ach_insane_ai_title", "ach_insane_ai_desc", "⚡", AchievementCategory.COMBAT, 1, 0, false, 1000),
        Achievement("insane_conqueror_3", "ach_insane_3_title", "ach_insane_3_desc", "🔱", AchievementCategory.COMBAT, 3, 0, false, 2000),

        Achievement("defeat_rusher", "ach_rusher_kill_title", "ach_rusher_kill_desc", "⚡", AchievementCategory.COMBAT, 1, 0, false, 180),
        Achievement("defeat_defender", "ach_defender_kill_title", "ach_defender_kill_desc", "🏰", AchievementCategory.COMBAT, 1, 0, false, 180),
        Achievement("defeat_architect", "ach_architect_kill_title", "ach_architect_kill_desc", "🌀", AchievementCategory.COMBAT, 1, 0, false, 200),
        Achievement("defeat_tactician", "ach_tactician_kill_title", "ach_tactician_kill_desc", "♟️", AchievementCategory.COMBAT, 1, 0, false, 250),
        Achievement("defeat_predictor", "ach_predictor_kill_title", "ach_predictor_kill_desc", "🔮", AchievementCategory.COMBAT, 1, 0, false, 300),
        Achievement("defeat_trickster", "ach_trickster_kill_title", "ach_trickster_kill_desc", "🎭", AchievementCategory.COMBAT, 1, 0, false, 250),
        Achievement("defeat_speed_demon", "ach_speed_kill_title", "ach_speed_kill_desc", "🏎️", AchievementCategory.COMBAT, 1, 0, false, 220),
        Achievement("defeat_counter_puncher", "ach_counter_kill_title", "ach_counter_kill_desc", "🎯", AchievementCategory.COMBAT, 1, 0, false, 260),
        Achievement("defeat_merciless", "ach_merciless_kill_title", "ach_merciless_kill_desc", "💀", AchievementCategory.COMBAT, 1, 0, false, 500),
        Achievement("defeat_chaotic", "ach_chaotic_kill_title", "ach_chaotic_kill_desc", "🌪️", AchievementCategory.COMBAT, 1, 0, false, 200),
        Achievement("defeat_all_personalities", "ach_all_personas_title", "ach_all_personas_desc", "👑", AchievementCategory.COMBAT, 10, 0, false, 1500),

        // ================= TACTICS & WALLS =================
        Achievement("walls_10", "ach_walls_10_title", "ach_walls_10_desc", "🧱", AchievementCategory.TACTICS, 10, 0, false, 50),
        Achievement("walls_25", "ach_walls_25_title", "ach_walls_25_desc", "🧱", AchievementCategory.TACTICS, 25, 0, false, 100),
        Achievement("walls_50", "ach_walls_50_title", "ach_walls_50_desc", "🏰", AchievementCategory.TACTICS, 50, 0, false, 180),
        Achievement("walls_100", "ach_walls_100_title", "ach_walls_100_desc", "🏯", AchievementCategory.TACTICS, 100, 0, false, 300),
        Achievement("walls_250", "ach_walls_250_title", "ach_walls_250_desc", "🗿", AchievementCategory.TACTICS, 250, 0, false, 600),
        Achievement("walls_500", "ach_walls_500_title", "ach_walls_500_desc", "⛰️", AchievementCategory.TACTICS, 500, 0, false, 1200),
        Achievement("walls_1000", "ach_walls_1000_title", "ach_walls_1000_desc", "🪐", AchievementCategory.TACTICS, 1000, 0, false, 2500),

        Achievement("minimalist_win", "ach_minimalist_title", "ach_minimalist_desc", "🎯", AchievementCategory.TACTICS, 1, 0, false, 150),
        Achievement("wall_less_victory", "ach_wall_less_title", "ach_wall_less_desc", "👻", AchievementCategory.TACTICS, 1, 0, false, 350),
        Achievement("zero_waste", "ach_zero_waste_title", "ach_zero_waste_desc", "🏹", AchievementCategory.TACTICS, 1, 0, false, 180),
        Achievement("maze_builder", "ach_maze_builder_title", "ach_maze_builder_desc", "🌀", AchievementCategory.TACTICS, 3, 0, false, 200),
        Achievement("iron_curtain", "ach_iron_curtain_title", "ach_iron_curtain_desc", "🚧", AchievementCategory.TACTICS, 1, 0, false, 220),
        Achievement("master_blocker", "ach_master_blocker_title", "ach_master_blocker_desc", "🛑", AchievementCategory.TACTICS, 1, 0, false, 300),
        Achievement("funnel_trap_expert", "ach_funnel_title", "ach_funnel_desc", "🪤", AchievementCategory.TACTICS, 1, 0, false, 350),

        // ================= RANKS & PROGRESSION =================
        Achievement("rank_silver", "ach_rank_silver_title", "ach_rank_silver_desc", "🥈", AchievementCategory.SECRET, 1, 0, false, 200),
        Achievement("rank_gold", "ach_rank_gold_title", "ach_rank_gold_desc", "🥇", AchievementCategory.SECRET, 1, 0, false, 400),
        Achievement("rank_platinum", "ach_rank_platinum_title", "ach_rank_platinum_desc", "💎", AchievementCategory.SECRET, 1, 0, false, 750),
        Achievement("rank_diamond", "ach_rank_diamond_title", "ach_rank_diamond_desc", "🔮", AchievementCategory.SECRET, 1, 0, false, 1200),
        Achievement("rank_master", "ach_rank_master_title", "ach_rank_master_desc", "👑", AchievementCategory.SECRET, 1, 0, false, 2000),
        Achievement("rank_grandmaster", "ach_rank_gm_title", "ach_rank_gm_desc", "🏆", AchievementCategory.SECRET, 1, 0, false, 3500),
        Achievement("rank_apex", "ach_rank_apex_title", "ach_rank_apex_desc", "⚡", AchievementCategory.SECRET, 1, 0, false, 6000),

        Achievement("level_5", "ach_lvl_5_title", "ach_lvl_5_desc", "⭐", AchievementCategory.SECRET, 5, 0, false, 100),
        Achievement("level_10", "ach_lvl_10_title", "ach_lvl_10_desc", "🌟", AchievementCategory.SECRET, 10, 0, false, 250),
        Achievement("level_25", "ach_lvl_25_title", "ach_lvl_25_desc", "🌠", AchievementCategory.SECRET, 25, 0, false, 600),
        Achievement("level_50", "ach_lvl_50_title", "ach_lvl_50_desc", "🌌", AchievementCategory.SECRET, 50, 0, false, 1500),
        Achievement("level_100", "ach_lvl_100_title", "ach_lvl_100_desc", "👑", AchievementCategory.SECRET, 100, 0, false, 5000),

        // ================= SPEED & RACING =================
        Achievement("speed_demon", "ach_race_win_title", "ach_race_win_desc", "🏎️", AchievementCategory.SPEED, 1, 0, false, 100),
        Achievement("race_champion_5", "ach_race_5_title", "ach_race_5_desc", "🏁", AchievementCategory.SPEED, 5, 0, false, 250),
        Achievement("race_master_20", "ach_race_20_title", "ach_race_20_desc", "🚀", AchievementCategory.SPEED, 20, 0, false, 800),
        Achievement("speedy_blitz_win", "ach_blitz_win_title", "ach_blitz_win_desc", "⏱️", AchievementCategory.SPEED, 1, 0, false, 200),
        Achievement("lightning_fast_30s", "ach_lightning_30s_title", "ach_lightning_30s_desc", "⚡", AchievementCategory.SPEED, 1, 0, false, 350),
        Achievement("timed_3m_champion", "ach_timed_3m_title", "ach_timed_3m_desc", "⏳", AchievementCategory.SPEED, 3, 0, false, 220),

        // ================= MULTIPLAYER & P2P =================
        Achievement("multiplayer_p2p", "ach_p2p_title", "ach_p2p_desc", "📡", AchievementCategory.MULTIPLAYER, 1, 0, false, 120),
        Achievement("p2p_victor_3", "ach_p2p_victor_title", "ach_p2p_victor_desc", "🤝", AchievementCategory.MULTIPLAYER, 3, 0, false, 250),
        Achievement("p2p_gladiator_10", "ach_p2p_10_title", "ach_p2p_10_desc", "⚔️", AchievementCategory.MULTIPLAYER, 10, 0, false, 600),
        Achievement("quad_champion", "ach_quad_win_title", "ach_quad_win_desc", "🛡️", AchievementCategory.MULTIPLAYER, 1, 0, false, 200),
        Achievement("quad_master_5", "ach_quad_master_title", "ach_quad_master_desc", "👑", AchievementCategory.MULTIPLAYER, 5, 0, false, 450),
        Achievement("quad_legend_15", "ach_quad_15_title", "ach_quad_15_desc", "🏆", AchievementCategory.MULTIPLAYER, 15, 0, false, 1200),
        Achievement("lobby_gladiator", "ach_lobby_title", "ach_lobby_desc", "🌐", AchievementCategory.MULTIPLAYER, 3, 0, false, 200),
        Achievement("pass_play_master", "ach_pass_play_title", "ach_pass_play_desc", "📱", AchievementCategory.MULTIPLAYER, 3, 0, false, 150),

        // ================= SECRETS & SPECIAL =================
        Achievement("night_owl", "ach_night_owl_title", "ach_night_owl_desc", "🌙", AchievementCategory.SECRET, 1, 0, false, 150),
        Achievement("early_bird", "ach_early_bird_title", "ach_early_bird_desc", "🌅", AchievementCategory.SECRET, 1, 0, false, 150),
        Achievement("fashion_icon", "ach_fashion_title", "ach_fashion_desc", "🎨", AchievementCategory.SECRET, 1, 0, false, 100),
        Achievement("combo_maestro_5", "ach_combo_5_title", "ach_combo_5_desc", "🔥", AchievementCategory.SECRET, 1, 0, false, 250),
        Achievement("near_miss_dodger", "ach_near_miss_title", "ach_near_miss_desc", "🏃", AchievementCategory.SECRET, 3, 0, false, 200),
        Achievement("daily_challenger_5", "ach_daily_5_title", "ach_daily_5_desc", "📅", AchievementCategory.SECRET, 5, 0, false, 350),
        Achievement("secret_bypass_hacker", "ach_bypass_glitch_title", "ach_bypass_glitch_desc", "⚡", AchievementCategory.SECRET, 1, 0, false, 500)
    )

    fun loadAllAchievements(context: Context, profile: PlayerProfile?): List<Achievement> {
        val prefs = getPrefs(context)
        return ACHIEVEMENT_TEMPLATES.map { template ->
            val isUnlocked = prefs.getBoolean(KEY_PREFIX_UNLOCKED + template.id, false)
            val timestamp = prefs.getLong(KEY_PREFIX_TIMESTAMP + template.id, 0L).let { if (it > 0) it else null }

            val calculatedProgress = when (template.id) {
                "first_win", "wins_5", "wins_10", "wins_20", "wins_50", "wins_100", "wins_250", "wins_500" -> profile?.wins ?: 0
                "streak_3", "streak_5", "streak_7", "streak_10", "streak_15", "streak_20" -> profile?.bestStreak ?: 0
                "walls_10", "walls_25", "walls_50", "walls_100", "walls_250", "walls_500", "walls_1000" -> profile?.wallsPlaced ?: 0
                "rank_silver" -> if ((profile?.ratingScore ?: 0) >= 1200) 1 else 0
                "rank_gold" -> if ((profile?.ratingScore ?: 0) >= 1500) 1 else 0
                "rank_platinum" -> if ((profile?.ratingScore ?: 0) >= 1800) 1 else 0
                "rank_diamond" -> if ((profile?.ratingScore ?: 0) >= 2100) 1 else 0
                "rank_master" -> if ((profile?.ratingScore ?: 0) >= 2400) 1 else 0
                "rank_grandmaster" -> if ((profile?.ratingScore ?: 0) >= 2600) 1 else 0
                "rank_apex" -> if ((profile?.ratingScore ?: 0) >= 2900) 1 else 0
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
        aiPersonality: NPCPersonality? = null,
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

        // 1. Time-based secrets (Night Owl & Early Bird)
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (currentHour in 0..5) checkAndUnlock("night_owl")
        if (currentHour in 5..8) checkAndUnlock("early_bird")

        // 2. Pass and Play Match
        if (finalState.rules.mode == GameMode.PASS_AND_PLAY) {
            checkAndUnlock("pass_play_master")
        }

        // 3. Walls Tracking
        val wallsUsedByPlayer = finalState.rules.wallsPerPlayer - finalState.getPlayer(localPlayerId).remainingWalls
        if (wallsUsedByPlayer >= 8) checkAndUnlock("maze_builder")

        if (isWin) {
            // Wins milestones
            val totalWins = profile.wins + 1
            if (totalWins >= 1) checkAndUnlock("first_win")
            if (totalWins >= 5) checkAndUnlock("wins_5")
            if (totalWins >= 10) checkAndUnlock("wins_10")
            if (totalWins >= 20) checkAndUnlock("wins_20")
            if (totalWins >= 50) checkAndUnlock("wins_50")
            if (totalWins >= 100) checkAndUnlock("wins_100")
            if (totalWins >= 250) checkAndUnlock("wins_250")
            if (totalWins >= 500) checkAndUnlock("wins_500")

            // Win streaks
            val currentStreak = profile.currentStreak + 1
            if (currentStreak >= 3) checkAndUnlock("streak_3")
            if (currentStreak >= 5) checkAndUnlock("streak_5")
            if (currentStreak >= 7) checkAndUnlock("streak_7")
            if (currentStreak >= 10) checkAndUnlock("streak_10")
            if (currentStreak >= 15) checkAndUnlock("streak_15")
            if (currentStreak >= 20) checkAndUnlock("streak_20")

            // Minimalist & Wall-less Victories
            if (wallsUsedByPlayer <= 2) checkAndUnlock("minimalist_win")
            if (wallsUsedByPlayer == 0) checkAndUnlock("wall_less_victory")
            if (wallsUsedByPlayer >= finalState.rules.wallsPerPlayer) checkAndUnlock("zero_waste")

            // Fast Moves
            val moveCount = finalState.moveCount
            if (moveCount in 1..16) checkAndUnlock("flawless_victory")
            if (moveCount in 1..12) checkAndUnlock("ultra_flawless")

            // AI difficulty victories
            if (finalState.rules.mode == GameMode.VS_AI) {
                when (finalState.rules.aiDifficulty) {
                    AIDifficulty.EASY -> checkAndUnlock("ai_easy_win")
                    AIDifficulty.MEDIUM -> checkAndUnlock("ai_medium_win")
                    AIDifficulty.HARD -> checkAndUnlock("ai_hard_win")
                    AIDifficulty.EXPERT -> checkAndUnlock("ai_expert_win")
                    AIDifficulty.NIGHTMARE -> checkAndUnlock("ai_nightmare_win")
                    AIDifficulty.INSANE -> {
                        checkAndUnlock("ai_insane_win")
                        checkAndUnlock("insane_conqueror_3")
                    }
                }

                // AI Personality Victories
                if (aiPersonality != null) {
                    when (aiPersonality) {
                        NPCPersonality.THE_RUSHER -> checkAndUnlock("defeat_rusher")
                        NPCPersonality.THE_DEFENDER -> checkAndUnlock("defeat_defender")
                        NPCPersonality.THE_ARCHITECT -> checkAndUnlock("defeat_architect")
                        NPCPersonality.THE_TACTICIAN -> checkAndUnlock("defeat_tactician")
                        NPCPersonality.THE_PREDICTOR -> checkAndUnlock("defeat_predictor")
                        NPCPersonality.THE_TRICKSTER -> checkAndUnlock("defeat_trickster")
                        NPCPersonality.THE_SPEED_DEMON -> checkAndUnlock("defeat_speed_demon")
                        NPCPersonality.THE_COUNTER_PUNCHER -> checkAndUnlock("defeat_counter_puncher")
                        NPCPersonality.THE_MERCILESS -> checkAndUnlock("defeat_merciless")
                        NPCPersonality.THE_CHAOTIC -> checkAndUnlock("defeat_chaotic")
                    }
                    checkAndUnlock("defeat_all_personalities")
                }
            }

            // Special Game Modes
            if (finalState.rules.mode == GameMode.RACE_MODE) {
                checkAndUnlock("speed_demon")
                checkAndUnlock("race_champion_5")
                checkAndUnlock("race_master_20")
            }
            if (finalState.rules.mode == GameMode.QUAD_MODE) {
                checkAndUnlock("quad_champion")
                checkAndUnlock("quad_master_5")
                checkAndUnlock("quad_legend_15")
            }
            if (finalState.rules.mode == GameMode.PUBLIC_ROOM || finalState.rules.mode == GameMode.QUICK_MATCH) {
                checkAndUnlock("lobby_gladiator")
            }
            if (finalState.rules.timeLimitSeconds == 180) {
                checkAndUnlock("timed_3m_champion")
            }
            if (isP2PMatch) {
                checkAndUnlock("p2p_victor_3")
                checkAndUnlock("p2p_gladiator_10")
            }
        }

        if (isP2PMatch || finalState.rules.mode == GameMode.FRIEND_ROOM) {
            checkAndUnlock("multiplayer_p2p")
        }

        editor.apply()
    }

    fun recordLevelOrRank(context: Context, level: Int, rating: Int, onUnlocked: (Achievement) -> Unit) {
        val prefs = getPrefs(context)
        val editor = prefs.edit()

        fun unlock(id: String) {
            val template = ACHIEVEMENT_TEMPLATES.find { it.id == id } ?: return
            if (!prefs.getBoolean(KEY_PREFIX_UNLOCKED + id, false)) {
                editor.putBoolean(KEY_PREFIX_UNLOCKED + id, true)
                editor.putLong(KEY_PREFIX_TIMESTAMP + id, System.currentTimeMillis())
                editor.putInt(KEY_PREFIX_PROGRESS + id, template.targetCount)
                editor.apply()
                onUnlocked(template.copy(isUnlocked = true, currentProgress = template.targetCount))
            }
        }

        if (level >= 5) unlock("level_5")
        if (level >= 10) unlock("level_10")
        if (level >= 25) unlock("level_25")
        if (level >= 50) unlock("level_50")
        if (level >= 100) unlock("level_100")

        if (rating >= 1200) unlock("rank_silver")
        if (rating >= 1500) unlock("rank_gold")
        if (rating >= 1800) unlock("rank_platinum")
        if (rating >= 2100) unlock("rank_diamond")
        if (rating >= 2400) unlock("rank_master")
        if (rating >= 2600) unlock("rank_grandmaster")
        if (rating >= 2900) unlock("rank_apex")
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

    fun recordSecretGlitch(context: Context, onUnlocked: (Achievement) -> Unit) {
        val prefs = getPrefs(context)
        val id = "secret_bypass_hacker"
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
