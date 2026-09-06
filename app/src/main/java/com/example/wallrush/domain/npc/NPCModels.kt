package com.example.wallrush.domain.npc

import androidx.annotation.Keep
import com.example.wallrush.domain.model.AIDifficulty

@Keep
enum class NPCPersonality {
    THE_RUSHER,          // Focuses on pure forward sprint, uses walls only in dire emergencies
    THE_ARCHITECT,       // Builds complex wall mazes early to funnel and trap
    THE_TACTICIAN,       // Calculated shortest-path optimizer, high-IQ blocking
    THE_COUNTER_PUNCHER, // Waits until opponent crosses midfield, then springs wall traps
    THE_CHAOTIC,         // Unpredictable flanking, unusual angles, erratic decisions
    THE_DEFENDER         // Cautious, guards jump corridors, maintains safe escape routes
}

@Keep
data class NPCProfile(
    val id: String,
    val name: String,
    val avatarId: Int,
    val rating: Int,
    val countryFlag: String,
    val countryName: String,
    val personality: NPCPersonality,
    val minThinkingDelayMs: Long,
    val maxThinkingDelayMs: Long,
    var encountersCount: Int = 0,
    val statusBio: String = ""
) {
    fun toAIDifficulty(): AIDifficulty {
        return when {
            rating >= 1900 -> AIDifficulty.HARD
            rating >= 1400 -> AIDifficulty.MEDIUM
            else -> AIDifficulty.EASY
        }
    }
}
