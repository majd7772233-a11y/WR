package com.example.wallrush.domain.challenges

import android.content.Context
import android.content.SharedPreferences
import com.example.wallrush.domain.model.AIDifficulty
import com.example.wallrush.domain.model.DailyChallenge
import com.example.wallrush.domain.model.GameMode
import com.example.wallrush.domain.model.GameState
import com.example.wallrush.domain.model.PlayerId
import java.util.Calendar
import kotlin.math.abs

object DailyChallengeManager {

    private const val PREFS_NAME = "wallrush_daily_challenges"
    private const val KEY_CUSTOM_REROLL_SEED = "custom_reroll_seed_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getTodayDateKey(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return "$year-$month-$day"
    }

    fun getMillisUntilMidnight(): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 24)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return (calendar.timeInMillis - now).coerceAtLeast(0L)
    }

    /**
     * Generates a fully-customized tactical daily challenge where the system chooses
     * all game parameters (Mode, Bot AI, Walls count, Time limit, and Opponent avatar).
     */
    fun createRandomTacticalChallenge(seed: Int): DailyChallenge {
        val absSeed = abs(seed)
        val modeIndex = absSeed % 3
        val mode = when (modeIndex) {
            0 -> GameMode.VS_AI
            1 -> GameMode.RACE_MODE
            else -> GameMode.QUAD_MODE
        }

        val diffIndex = (absSeed / 3) % 3
        val difficulty = when (diffIndex) {
            0 -> AIDifficulty.EASY
            1 -> AIDifficulty.MEDIUM
            else -> AIDifficulty.HARD
        }

        val wallsIndex = (absSeed / 9) % 3
        val walls = when (wallsIndex) {
            0 -> 5
            1 -> 10
            else -> 15
        }

        val timeIndex = (absSeed / 27) % 3
        val timeLimit = when (timeIndex) {
            0 -> 120
            1 -> 180
            else -> 300
        }

        val avatar = (absSeed % 6) + 1
        val opponentName = when (difficulty) {
            AIDifficulty.EASY -> listOf("🤖 Rookie Bot", "🌱 Cadet AI", "🟢 Sparky AI").random()
            AIDifficulty.MEDIUM -> listOf("⚔️ Victor Bot", "🛡️ Titan Guard", "🟡 Sentinel AI").random()
            AIDifficulty.HARD -> listOf("🧠 Grandmaster AI", "⚡ Apex Mind", "🔴 Cyber Warlord").random()
            AIDifficulty.EXPERT -> listOf("🟣 Grand Oracle", "🔮 Cyber Nexus").random()
            AIDifficulty.NIGHTMARE -> listOf("💀 Nightmare Entity", "👁️ Omniscient Core").random()
            AIDifficulty.INSANE -> listOf("⚡ Quantum God AI", "🌌 Infinity Alpha").random()
        }

        val reward = when (difficulty) {
            AIDifficulty.EASY -> 120
            AIDifficulty.MEDIUM -> 180
            AIDifficulty.HARD -> 250
            AIDifficulty.EXPERT -> 320
            AIDifficulty.NIGHTMARE -> 400
            AIDifficulty.INSANE -> 500
        }

        return DailyChallenge(
            id = "daily_random_tactical",
            titleKey = "daily_random_title",
            descKey = when (mode) {
                GameMode.RACE_MODE -> "daily_random_race_desc"
                GameMode.QUAD_MODE -> "daily_random_quad_desc"
                else -> "daily_random_classic_desc"
            },
            icon = when (mode) {
                GameMode.RACE_MODE -> "🏁"
                GameMode.QUAD_MODE -> "🎯"
                else -> "🎲"
            },
            targetCount = 1,
            currentProgress = 0,
            rewardXp = reward,
            isCompleted = false,
            isClaimed = false,
            launchMode = mode,
            launchAiDifficulty = difficulty,
            launchWallsCount = walls,
            launchTimeLimitSeconds = timeLimit,
            opponentName = opponentName,
            opponentAvatar = avatar
        )
    }

    private fun getDailyQuestTemplates(context: Context, dateKey: String): List<DailyChallenge> {
        val prefs = getPrefs(context)
        val rerollSeed = prefs.getInt(KEY_CUSTOM_REROLL_SEED + dateKey, dateKey.hashCode())
        val featuredChallenge = createRandomTacticalChallenge(rerollSeed)

        return listOf(
            featuredChallenge,
            DailyChallenge(
                id = "daily_win",
                titleKey = "daily_win_title",
                descKey = "daily_win_desc",
                icon = "🏆",
                targetCount = 1,
                currentProgress = 0,
                rewardXp = 120,
                isCompleted = false,
                isClaimed = false,
                launchMode = GameMode.VS_AI,
                launchAiDifficulty = AIDifficulty.MEDIUM,
                launchWallsCount = 10,
                launchTimeLimitSeconds = 180,
                opponentName = "🤖 Master AI",
                opponentAvatar = 2
            ),
            DailyChallenge(
                id = "daily_ai",
                titleKey = "daily_ai_title",
                descKey = "daily_ai_desc",
                icon = "🤖",
                targetCount = 1,
                currentProgress = 0,
                rewardXp = 100,
                isCompleted = false,
                isClaimed = false,
                launchMode = GameMode.VS_AI,
                launchAiDifficulty = AIDifficulty.EASY,
                launchWallsCount = 10,
                launchTimeLimitSeconds = 300,
                opponentName = "🤖 Practice Bot",
                opponentAvatar = 1
            ),
            DailyChallenge(
                id = "daily_walls",
                titleKey = "daily_walls_title",
                descKey = "daily_walls_desc",
                icon = "🧱",
                targetCount = 8,
                currentProgress = 0,
                rewardXp = 110,
                isCompleted = false,
                isClaimed = false,
                launchMode = GameMode.VS_AI,
                launchAiDifficulty = AIDifficulty.MEDIUM,
                launchWallsCount = 15,
                launchTimeLimitSeconds = 300,
                opponentName = "🧱 Wall Maestro",
                opponentAvatar = 3
            ),
            DailyChallenge(
                id = "daily_race",
                titleKey = "daily_race_title",
                descKey = "daily_race_desc",
                icon = "🏁",
                targetCount = 1,
                currentProgress = 0,
                rewardXp = 160,
                isCompleted = false,
                isClaimed = false,
                launchMode = GameMode.RACE_MODE,
                launchAiDifficulty = AIDifficulty.MEDIUM,
                launchWallsCount = 10,
                launchTimeLimitSeconds = 180,
                opponentName = "⚡ Speed Phantom",
                opponentAvatar = 4
            ),
            DailyChallenge(
                id = "daily_quad",
                titleKey = "daily_quad_title",
                descKey = "daily_quad_desc",
                icon = "🎯",
                targetCount = 1,
                currentProgress = 0,
                rewardXp = 200,
                isCompleted = false,
                isClaimed = false,
                launchMode = GameMode.QUAD_MODE,
                launchAiDifficulty = AIDifficulty.MEDIUM,
                launchWallsCount = 5,
                launchTimeLimitSeconds = 180,
                opponentName = "🎯 Quad Apex Bot",
                opponentAvatar = 5
            )
        )
    }

    fun loadTodayChallenges(context: Context): List<DailyChallenge> {
        val prefs = getPrefs(context)
        val todayKey = getTodayDateKey()
        val templates = getDailyQuestTemplates(context, todayKey)

        return templates.map { quest ->
            val prog = prefs.getInt("${todayKey}_${quest.id}_prog", 0)
            val claimed = prefs.getBoolean("${todayKey}_${quest.id}_claimed", false)
            val completed = prog >= quest.targetCount
            quest.copy(
                currentProgress = prog.coerceAtMost(quest.targetCount),
                isCompleted = completed,
                isClaimed = claimed
            )
        }
    }

    fun rerollRandomChallenge(context: Context): DailyChallenge {
        val prefs = getPrefs(context)
        val todayKey = getTodayDateKey()
        val newSeed = (System.currentTimeMillis() % 100000).toInt()
        prefs.edit().putInt(KEY_CUSTOM_REROLL_SEED + todayKey, newSeed).apply()
        return createRandomTacticalChallenge(newSeed)
    }

    fun onMatchFinished(
        context: Context,
        finalState: GameState,
        localPlayerId: PlayerId,
        onChallengeCompleted: (DailyChallenge) -> Unit
    ) {
        val prefs = getPrefs(context)
        val todayKey = getTodayDateKey()
        val isWin = finalState.winner == localPlayerId
        val editor = prefs.edit()

        fun increment(id: String, amount: Int = 1) {
            val templates = getDailyQuestTemplates(context, todayKey)
            val quest = templates.find { it.id == id } ?: return
            val key = "${todayKey}_${id}_prog"
            val prev = prefs.getInt(key, 0)
            val updated = prev + amount
            editor.putInt(key, updated)

            if (prev < quest.targetCount && updated >= quest.targetCount) {
                onChallengeCompleted(quest.copy(currentProgress = quest.targetCount, isCompleted = true))
            }
        }

        // 1. Check Win quest
        if (isWin) {
            increment("daily_win")
            increment("daily_random_tactical")
        }

        // 2. Check AI quest
        if (finalState.rules.mode == GameMode.VS_AI) {
            increment("daily_ai")
        }

        // 3. Check Special Mode quest (Race / Quad)
        if (finalState.rules.mode == GameMode.RACE_MODE) {
            increment("daily_race")
        } else if (finalState.rules.mode == GameMode.QUAD_MODE) {
            increment("daily_quad")
        }

        // 4. Walls placed in match by local player
        val wallsUsed = finalState.rules.wallsPerPlayer - finalState.getPlayer(localPlayerId).remainingWalls
        if (wallsUsed > 0) {
            increment("daily_walls", wallsUsed)
        }

        editor.apply()
    }

    fun onWallPlaced(context: Context, onChallengeCompleted: (DailyChallenge) -> Unit) {
        val prefs = getPrefs(context)
        val todayKey = getTodayDateKey()
        val key = "${todayKey}_daily_walls_prog"
        val prev = prefs.getInt(key, 0)
        val updated = prev + 1
        prefs.edit().putInt(key, updated).apply()

        val templates = getDailyQuestTemplates(context, todayKey)
        val quest = templates.find { it.id == "daily_walls" } ?: return
        if (prev < quest.targetCount && updated >= quest.targetCount) {
            onChallengeCompleted(quest.copy(currentProgress = quest.targetCount, isCompleted = true))
        }
    }

    fun claimChallengeReward(context: Context, challengeId: String): Int {
        val prefs = getPrefs(context)
        val todayKey = getTodayDateKey()
        val templates = getDailyQuestTemplates(context, todayKey)
        val quest = templates.find { it.id == challengeId } ?: return 0
        val isClaimed = prefs.getBoolean("${todayKey}_${challengeId}_claimed", false)
        if (isClaimed) return 0

        prefs.edit().putBoolean("${todayKey}_${challengeId}_claimed", true).apply()
        return quest.rewardXp
    }
}

