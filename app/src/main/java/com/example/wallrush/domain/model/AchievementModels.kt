package com.example.wallrush.domain.model

import androidx.annotation.Keep

@Keep
enum class AchievementCategory {
    ALL,
    COMBAT,
    TACTICS,
    SPEED,
    MULTIPLAYER,
    SECRET
}

@Keep
data class Achievement(
    val id: String,
    val titleKey: String,
    val descKey: String,
    val icon: String,
    val category: AchievementCategory,
    val targetCount: Int,
    val currentProgress: Int,
    val isUnlocked: Boolean,
    val rewardXp: Int,
    val unlockedAt: Long? = null
)

@Keep
data class DailyChallenge(
    val id: String,
    val titleKey: String,
    val descKey: String,
    val icon: String,
    val targetCount: Int,
    val currentProgress: Int,
    val rewardXp: Int,
    val isCompleted: Boolean,
    val isClaimed: Boolean,
    val launchMode: GameMode = GameMode.VS_AI,
    val launchAiDifficulty: AIDifficulty = AIDifficulty.MEDIUM,
    val launchWallsCount: Int = 10,
    val launchTimeLimitSeconds: Int = 180,
    val opponentName: String = "AI Challenger",
    val opponentAvatar: Int = 1
)

@Keep
data class InAppNotification(
    val id: String,
    val title: String,
    val message: String,
    val icon: String = "🏆",
    val type: NotificationType = NotificationType.ACHIEVEMENT
)

@Keep
enum class NotificationType {
    ACHIEVEMENT,
    DAILY_CHALLENGE,
    SECRET_GLITCH,
    SYSTEM
}

