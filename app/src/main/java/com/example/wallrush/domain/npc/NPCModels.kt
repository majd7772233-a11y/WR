package com.example.wallrush.domain.npc

import androidx.annotation.Keep
import com.example.wallrush.domain.model.AIDifficulty

@Keep
enum class NPCPersonality {
    THE_RUSHER,          // Aggressive / The Aggressor: Pure forward rush, relentless pressure
    THE_DEFENDER,        // The Defender: Cautious, waits for opponent mistakes, fortress builder
    THE_CHAOTIC,         // The Random / Chaotic: Unpredictable moves, sudden off-angle walls
    THE_PREDICTOR,       // The Predictor: Learns player tendencies, anticipates escape routes
    THE_TRICKSTER,       // The Trickster: Deploys decoy openings and bait patterns
    THE_TACTICIAN,       // Tactical: Mathematical shortest-path and corridor optimizer
    THE_SPEED_DEMON,     // Speed Demon: Blitzkrieg fast movements, rapid flanks
    THE_MERCILESS,       // Merciless: Punishes every sub-optimal step with brutal wall traps
    THE_ARCHITECT,       // The Architect: Complex maze architect, funnels into dead ends
    THE_COUNTER_PUNCHER  // Counter-Puncher: Waits for player to cross midfield then strikes
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
            rating >= 2400 -> AIDifficulty.INSANE
            rating >= 2100 -> AIDifficulty.NIGHTMARE
            rating >= 1800 -> AIDifficulty.EXPERT
            rating >= 1500 -> AIDifficulty.HARD
            rating >= 1100 -> AIDifficulty.MEDIUM
            else -> AIDifficulty.EASY
        }
    }
}
