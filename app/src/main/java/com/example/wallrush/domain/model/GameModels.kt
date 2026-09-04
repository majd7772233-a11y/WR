package com.example.wallrush.domain.model

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass
import java.util.UUID

@Keep
enum class PlayerId {
    PLAYER_1,
    PLAYER_2
}

@Keep
enum class WallOrientation {
    HORIZONTAL,
    VERTICAL
}

@Keep
@JsonClass(generateAdapter = true)
data class Position(
    val x: Int,
    val y: Int
) {
    fun isWithinBounds(): Boolean = x in 0..8 && y in 0..8
}

@Keep
@JsonClass(generateAdapter = true)
data class Wall(
    val id: String = UUID.randomUUID().toString(),
    val x: Int, // 0..7
    val y: Int, // 0..7
    val orientation: WallOrientation,
    val placedBy: PlayerId
) {
    fun isWithinBounds(): Boolean = x in 0..7 && y in 0..7
}

@Keep
enum class GameMode {
    PASS_AND_PLAY,
    VS_AI,
    QUICK_MATCH,
    PUBLIC_ROOM,
    FRIEND_ROOM
}

@Keep
enum class AIDifficulty {
    EASY,
    MEDIUM,
    HARD
}

@Keep
enum class GameStatus {
    WAITING_FOR_PLAYERS,
    COUNTDOWN,
    IN_PROGRESS,
    FINISHED,
    ABORTED
}

@Keep
enum class FinishReason {
    GOAL_REACHED,
    TIMEOUT,
    RESIGNATION,
    OPPONENT_DISCONNECTED
}

@Keep
@JsonClass(generateAdapter = true)
data class GameRules(
    val wallsPerPlayer: Int = 10,
    val timeLimitSeconds: Int = 300, // 5 min (0 = Infinite)
    val allowUndo: Boolean = false,
    val aiDifficulty: AIDifficulty = AIDifficulty.MEDIUM,
    val mode: GameMode = GameMode.VS_AI
)

@Keep
@JsonClass(generateAdapter = true)
data class PlayerState(
    val id: PlayerId,
    val name: String,
    val avatarId: Int = 0,
    val position: Position,
    val remainingWalls: Int = 10,
    val timeRemainingMillis: Long = 300_000L,
    val isAI: Boolean = false,
    val isHost: Boolean = true,
    val isConnected: Boolean = true
)

@Keep
sealed class GameEvent {
    @JsonClass(generateAdapter = true)
    data class PawnMoved(
        val player: PlayerId,
        val from: Position,
        val to: Position,
        val moveNumber: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : GameEvent()

    @JsonClass(generateAdapter = true)
    data class WallPlaced(
        val player: PlayerId,
        val wall: Wall,
        val moveNumber: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : GameEvent()

    @JsonClass(generateAdapter = true)
    data class EmoteSent(
        val player: PlayerId,
        val emoji: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : GameEvent()
}

@Keep
@JsonClass(generateAdapter = true)
data class GameState(
    val matchId: String = UUID.randomUUID().toString(),
    val roomCode: String = "",
    val rules: GameRules = GameRules(),
    val player1: PlayerState,
    val player2: PlayerState,
    val walls: List<Wall> = emptyList(),
    val currentTurn: PlayerId = PlayerId.PLAYER_1,
    val status: GameStatus = GameStatus.IN_PROGRESS,
    val winner: PlayerId? = null,
    val finishReason: FinishReason? = null,
    val moveCount: Int = 0,
    val eventHistory: List<GameEvent> = emptyList(),
    val countdownSeconds: Int = 0,
    val lastActionTimestamp: Long = System.currentTimeMillis()
) {
    fun getCurrentPlayer(): PlayerState = if (currentTurn == PlayerId.PLAYER_1) player1 else player2
    fun getOpponentPlayer(): PlayerState = if (currentTurn == PlayerId.PLAYER_1) player2 else player1
    fun getPlayer(id: PlayerId): PlayerState = if (id == PlayerId.PLAYER_1) player1 else player2
}
