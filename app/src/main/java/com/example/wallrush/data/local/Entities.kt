package com.example.wallrush.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.wallrush.domain.model.FinishReason
import com.example.wallrush.domain.model.GameMode
import com.example.wallrush.domain.model.PlayerId

@Entity(tableName = "match_records")
data class MatchRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val matchId: String,
    val gameMode: GameMode,
    val player1Name: String,
    val player2Name: String,
    val player1Avatar: Int,
    val player2Avatar: Int,
    val winner: PlayerId?,
    val finishReason: FinishReason?,
    val moveCount: Int,
    val wallsPlacedCount: Int,
    val durationSeconds: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val eventsJson: String,
    val rulesJson: String
)

@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey
    val guestId: String,
    val username: String = "Guest",
    val avatarId: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val totalMatches: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val wallsPlaced: Int = 0,
    val ratingScore: Int = 1200
) {
    val winRatePercent: Int get() = if (totalMatches > 0) (wins * 100) / totalMatches else 0
}
