package com.example.wallrush.domain.engine

import com.example.wallrush.domain.model.*
import java.util.UUID

object GameEngine {

    fun createInitialState(
        rules: GameRules,
        player1Name: String = "Player 1",
        player2Name: String = "Player 2",
        player1Avatar: Int = 0,
        player2Avatar: Int = 1,
        player2IsAI: Boolean = (rules.mode == GameMode.VS_AI),
        roomCode: String = generateRoomCode()
    ): GameState {
        val initialTime = if (rules.timeLimitSeconds > 0) rules.timeLimitSeconds * 1000L else Long.MAX_VALUE

        val p1 = PlayerState(
            id = PlayerId.PLAYER_1,
            name = player1Name,
            avatarId = player1Avatar,
            position = Position(4, 8), // Bottom row center
            remainingWalls = rules.wallsPerPlayer,
            timeRemainingMillis = initialTime,
            isAI = false,
            isHost = true,
            isConnected = true
        )

        val p2 = PlayerState(
            id = PlayerId.PLAYER_2,
            name = player2Name,
            avatarId = player2Avatar,
            position = Position(4, 0), // Top row center
            remainingWalls = rules.wallsPerPlayer,
            timeRemainingMillis = initialTime,
            isAI = player2IsAI,
            isHost = false,
            isConnected = true
        )

        return GameState(
            matchId = UUID.randomUUID().toString(),
            roomCode = roomCode,
            rules = rules,
            player1 = p1,
            player2 = p2,
            walls = emptyList(),
            currentTurn = PlayerId.PLAYER_1,
            status = if (rules.mode == GameMode.QUICK_MATCH || rules.mode == GameMode.PUBLIC_ROOM || rules.mode == GameMode.FRIEND_ROOM) {
                GameStatus.COUNTDOWN
            } else {
                GameStatus.IN_PROGRESS
            },
            winner = null,
            finishReason = null,
            moveCount = 0,
            eventHistory = emptyList(),
            countdownSeconds = if (rules.mode == GameMode.QUICK_MATCH || rules.mode == GameMode.PUBLIC_ROOM || rules.mode == GameMode.FRIEND_ROOM) 3 else 0,
            lastActionTimestamp = System.currentTimeMillis()
        )
    }

    fun makeMove(state: GameState, target: Position, playerId: PlayerId = state.currentTurn): GameState {
        if (state.status != GameStatus.IN_PROGRESS) return state
        if (state.currentTurn != playerId) return state

        val legalMoves = RuleEngine.getLegalMoves(state, playerId)
        if (target !in legalMoves) return state

        val movingPlayer = state.getPlayer(playerId)
        val updatedPlayer = movingPlayer.copy(position = target)

        val newMoveCount = state.moveCount + 1
        val event = GameEvent.PawnMoved(
            player = playerId,
            from = movingPlayer.position,
            to = target,
            moveNumber = newMoveCount
        )

        val newP1 = if (playerId == PlayerId.PLAYER_1) updatedPlayer else state.player1
        val newP2 = if (playerId == PlayerId.PLAYER_2) updatedPlayer else state.player2

        var nextState = state.copy(
            player1 = newP1,
            player2 = newP2,
            moveCount = newMoveCount,
            eventHistory = state.eventHistory + event,
            lastActionTimestamp = System.currentTimeMillis()
        )

        // Check for winner
        val winner = RuleEngine.checkWinner(nextState)
        if (winner != null) {
            return nextState.copy(
                status = GameStatus.FINISHED,
                winner = winner,
                finishReason = FinishReason.GOAL_REACHED
            )
        }

        // Switch turn
        val nextTurn = if (playerId == PlayerId.PLAYER_1) PlayerId.PLAYER_2 else PlayerId.PLAYER_1
        return nextState.copy(currentTurn = nextTurn)
    }

    fun placeWall(state: GameState, wall: Wall, playerId: PlayerId = state.currentTurn): GameState {
        if (state.status != GameStatus.IN_PROGRESS) return state
        if (state.currentTurn != playerId) return state

        if (!RuleEngine.isWallPlacementLegal(state, wall, playerId)) {
            return state
        }

        val placingPlayer = state.getPlayer(playerId)
        val updatedPlayer = placingPlayer.copy(remainingWalls = placingPlayer.remainingWalls - 1)

        val newMoveCount = state.moveCount + 1
        val event = GameEvent.WallPlaced(
            player = playerId,
            wall = wall,
            moveNumber = newMoveCount
        )

        val newP1 = if (playerId == PlayerId.PLAYER_1) updatedPlayer else state.player1
        val newP2 = if (playerId == PlayerId.PLAYER_2) updatedPlayer else state.player2

        val nextTurn = if (playerId == PlayerId.PLAYER_1) PlayerId.PLAYER_2 else PlayerId.PLAYER_1

        return state.copy(
            player1 = newP1,
            player2 = newP2,
            walls = state.walls + wall,
            currentTurn = nextTurn,
            moveCount = newMoveCount,
            eventHistory = state.eventHistory + event,
            lastActionTimestamp = System.currentTimeMillis()
        )
    }

    fun tickTimer(state: GameState, elapsedMillis: Long = 1000L): GameState {
        if (state.status != GameStatus.IN_PROGRESS) return state
        if (state.rules.timeLimitSeconds <= 0) return state

        val currentTurn = state.currentTurn
        val p1 = state.player1
        val p2 = state.player2

        var newP1Time = p1.timeRemainingMillis
        var newP2Time = p2.timeRemainingMillis

        if (currentTurn == PlayerId.PLAYER_1) {
            newP1Time = maxOf(0L, p1.timeRemainingMillis - elapsedMillis)
            if (newP1Time <= 0L) {
                return state.copy(
                    player1 = p1.copy(timeRemainingMillis = 0L),
                    status = GameStatus.FINISHED,
                    winner = PlayerId.PLAYER_2,
                    finishReason = FinishReason.TIMEOUT
                )
            }
        } else {
            newP2Time = maxOf(0L, p2.timeRemainingMillis - elapsedMillis)
            if (newP2Time <= 0L) {
                return state.copy(
                    player2 = p2.copy(timeRemainingMillis = 0L),
                    status = GameStatus.FINISHED,
                    winner = PlayerId.PLAYER_1,
                    finishReason = FinishReason.TIMEOUT
                )
            }
        }

        return state.copy(
            player1 = p1.copy(timeRemainingMillis = newP1Time),
            player2 = p2.copy(timeRemainingMillis = newP2Time)
        )
    }

    fun resign(state: GameState, resigningPlayer: PlayerId): GameState {
        if (state.status != GameStatus.IN_PROGRESS) return state
        val winner = if (resigningPlayer == PlayerId.PLAYER_1) PlayerId.PLAYER_2 else PlayerId.PLAYER_1
        return state.copy(
            status = GameStatus.FINISHED,
            winner = winner,
            finishReason = FinishReason.RESIGNATION
        )
    }

    fun addEmote(state: GameState, player: PlayerId, emoji: String): GameState {
        val event = GameEvent.EmoteSent(player, emoji)
        return state.copy(eventHistory = state.eventHistory + event)
    }

    fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..6).map { chars.random() }.joinToString("")
        return "#$code"
    }
}
