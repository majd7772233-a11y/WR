package com.example.wallrush.data.repository

import com.example.wallrush.data.local.AppDatabase
import com.example.wallrush.data.local.MatchRecord
import com.example.wallrush.data.local.PlayerProfile
import com.example.wallrush.domain.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class WallRushRepository(private val database: AppDatabase) {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val rulesAdapter = moshi.adapter(GameRules::class.java)

    val allMatches: Flow<List<MatchRecord>> = database.matchDao().getAllMatches()
    val playerProfileFlow: Flow<PlayerProfile?> = database.profileDao().getProfileFlow()

    suspend fun getOrCreateProfile(): PlayerProfile {
        val existing = database.profileDao().getProfile()
        if (existing != null) return existing

        val randomSuffix = (1000..9999).random()
        val newProfile = PlayerProfile(
            guestId = UUID.randomUUID().toString().take(8),
            username = "Player$randomSuffix",
            avatarId = 0,
            ratingScore = 1200
        )
        database.profileDao().saveProfile(newProfile)
        return newProfile
    }

    suspend fun updateProfile(username: String, avatarId: Int) {
        val current = getOrCreateProfile()
        database.profileDao().saveProfile(current.copy(username = username, avatarId = avatarId))
    }

    suspend fun resetStats() {
        val current = getOrCreateProfile()
        database.profileDao().saveProfile(
            current.copy(
                totalMatches = 0,
                wins = 0,
                losses = 0,
                currentStreak = 0,
                bestStreak = 0,
                ratingScore = 1200,
                wallsPlaced = 0
            )
        )
    }

    suspend fun saveCompletedMatch(
        state: GameState,
        durationSeconds: Long,
        localPlayerId: PlayerId = PlayerId.PLAYER_1
    ): Long {
        val rulesJson = rulesAdapter.toJson(state.rules)

        // Serialize events to simple JSON array format
        val eventsJson = serializeEvents(state.eventHistory)

        val record = MatchRecord(
            matchId = state.matchId,
            gameMode = state.rules.mode,
            player1Name = state.player1.name,
            player2Name = state.player2.name,
            player1Avatar = state.player1.avatarId,
            player2Avatar = state.player2.avatarId,
            winner = state.winner,
            finishReason = state.finishReason,
            moveCount = state.moveCount,
            wallsPlacedCount = state.walls.size,
            durationSeconds = durationSeconds,
            eventsJson = eventsJson,
            rulesJson = rulesJson
        )

        val recordId = database.matchDao().insertMatch(record)

        // Update profile stats for the local user
        val profile = getOrCreateProfile()
        val isWin = state.winner == localPlayerId
        val newWins = if (isWin) profile.wins + 1 else profile.wins
        val newLosses = if (!isWin && state.winner != null) profile.losses + 1 else profile.losses
        val newStreak = if (isWin) profile.currentStreak + 1 else 0
        val newBestStreak = maxOf(profile.bestStreak, newStreak)
        val ratingDelta = if (isWin) 25 else if (state.winner != null) -15 else 0
        val newRating = maxOf(800, profile.ratingScore + ratingDelta)
        val localPlayer = state.getPlayer(localPlayerId)
        val wallsUsedByLocal = state.rules.wallsPerPlayer - localPlayer.remainingWalls

        database.profileDao().saveProfile(
            profile.copy(
                wins = newWins,
                losses = newLosses,
                totalMatches = profile.totalMatches + 1,
                currentStreak = newStreak,
                bestStreak = newBestStreak,
                wallsPlaced = profile.wallsPlaced + wallsUsedByLocal,
                ratingScore = newRating
            )
        )

        return recordId
    }

    suspend fun getMatchRecordById(id: Long): MatchRecord? {
        return database.matchDao().getMatchById(id)
    }

    fun parseRules(rulesJson: String): GameRules {
        return try {
            rulesAdapter.fromJson(rulesJson) ?: GameRules()
        } catch (e: Exception) {
            GameRules()
        }
    }

    fun deserializeEvents(eventsJson: String): List<GameEvent> {
        val list = mutableListOf<GameEvent>()
        try {
            val lines = eventsJson.lines()
            for (line in lines) {
                if (line.isBlank()) continue
                val parts = line.split("|")
                when (parts[0]) {
                    "MOVE" -> {
                        list.add(
                            GameEvent.PawnMoved(
                                player = PlayerId.valueOf(parts[1]),
                                from = Position(parts[2].toInt(), parts[3].toInt()),
                                to = Position(parts[4].toInt(), parts[5].toInt()),
                                moveNumber = parts[6].toInt()
                            )
                        )
                    }
                    "WALL" -> {
                        list.add(
                            GameEvent.WallPlaced(
                                player = PlayerId.valueOf(parts[1]),
                                wall = Wall(
                                    x = parts[2].toInt(),
                                    y = parts[3].toInt(),
                                    orientation = WallOrientation.valueOf(parts[4]),
                                    placedBy = PlayerId.valueOf(parts[1])
                                ),
                                moveNumber = parts[5].toInt()
                            )
                        )
                    }
                    "EMOTE" -> {
                        list.add(
                            GameEvent.EmoteSent(
                                player = PlayerId.valueOf(parts[1]),
                                emoji = parts[2]
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun serializeEvents(events: List<GameEvent>): String {
        val sb = StringBuilder()
        for (event in events) {
            when (event) {
                is GameEvent.PawnMoved -> {
                    sb.append("MOVE|${event.player}|${event.from.x}|${event.from.y}|${event.to.x}|${event.to.y}|${event.moveNumber}\n")
                }
                is GameEvent.WallPlaced -> {
                    sb.append("WALL|${event.player}|${event.wall.x}|${event.wall.y}|${event.wall.orientation}|${event.moveNumber}\n")
                }
                is GameEvent.EmoteSent -> {
                    sb.append("EMOTE|${event.player}|${event.emoji}\n")
                }
            }
        }
        return sb.toString()
    }
}
