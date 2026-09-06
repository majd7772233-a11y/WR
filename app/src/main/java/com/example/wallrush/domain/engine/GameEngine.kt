package com.example.wallrush.domain.engine

import com.example.wallrush.domain.model.*
import java.util.UUID

object GameEngine {

    fun createInitialState(
        rules: GameRules,
        player1Name: String = "Player 1",
        player2Name: String = "Player 2",
        player3Name: String = "Player 3",
        player4Name: String = "Player 4",
        player1Avatar: Int = 0,
        player2Avatar: Int = 1,
        player3Avatar: Int = 2,
        player4Avatar: Int = 3,
        player2IsAI: Boolean = (rules.mode == GameMode.VS_AI),
        player3IsAI: Boolean = (rules.mode == GameMode.VS_AI || rules.mode == GameMode.QUAD_MODE),
        player4IsAI: Boolean = (rules.mode == GameMode.VS_AI || rules.mode == GameMode.QUAD_MODE),
        roomCode: String = generateRoomCode()
    ): GameState {
        val initialTime = if (rules.timeLimitSeconds > 0) rules.timeLimitSeconds * 1000L else Long.MAX_VALUE
        val wallsCount = if (rules.mode == GameMode.QUAD_MODE) 5 else rules.wallsPerPlayer

        val p1Start = when (rules.mode) {
            GameMode.RACE_MODE -> Position(3, 8)
            else -> Position(4, 8)
        }

        val p2Start = when (rules.mode) {
            GameMode.RACE_MODE -> Position(5, 8)
            else -> Position(4, 0)
        }

        val p1 = PlayerState(
            id = PlayerId.PLAYER_1,
            name = player1Name,
            avatarId = player1Avatar,
            position = p1Start,
            remainingWalls = wallsCount,
            timeRemainingMillis = initialTime,
            isAI = false,
            isHost = true,
            isConnected = true
        )

        val p2 = PlayerState(
            id = PlayerId.PLAYER_2,
            name = player2Name,
            avatarId = player2Avatar,
            position = p2Start,
            remainingWalls = wallsCount,
            timeRemainingMillis = initialTime,
            isAI = player2IsAI,
            isHost = false,
            isConnected = true
        )

        val p3 = if (rules.mode == GameMode.QUAD_MODE) {
            PlayerState(
                id = PlayerId.PLAYER_3,
                name = player3Name,
                avatarId = player3Avatar,
                position = Position(0, 4), // Left side center
                remainingWalls = wallsCount,
                timeRemainingMillis = initialTime,
                isAI = player3IsAI,
                isHost = false,
                isConnected = true
            )
        } else null

        val p4 = if (rules.mode == GameMode.QUAD_MODE) {
            PlayerState(
                id = PlayerId.PLAYER_4,
                name = player4Name,
                avatarId = player4Avatar,
                position = Position(8, 4), // Right side center
                remainingWalls = wallsCount,
                timeRemainingMillis = initialTime,
                isAI = player4IsAI,
                isHost = false,
                isConnected = true
            )
        } else null

        return GameState(
            matchId = UUID.randomUUID().toString(),
            roomCode = roomCode,
            rules = rules.copy(wallsPerPlayer = wallsCount),
            player1 = p1,
            player2 = p2,
            player3 = p3,
            player4 = p4,
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

    fun getNextTurn(state: GameState, currentTurn: PlayerId): PlayerId {
        return if (state.rules.mode == GameMode.QUAD_MODE) {
            when (currentTurn) {
                PlayerId.PLAYER_1 -> PlayerId.PLAYER_2
                PlayerId.PLAYER_2 -> PlayerId.PLAYER_3
                PlayerId.PLAYER_3 -> PlayerId.PLAYER_4
                PlayerId.PLAYER_4 -> PlayerId.PLAYER_1
            }
        } else {
            if (currentTurn == PlayerId.PLAYER_1) PlayerId.PLAYER_2 else PlayerId.PLAYER_1
        }
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
        val newP3 = if (playerId == PlayerId.PLAYER_3) updatedPlayer else state.player3
        val newP4 = if (playerId == PlayerId.PLAYER_4) updatedPlayer else state.player4

        val nextState = state.copy(
            player1 = newP1,
            player2 = newP2,
            player3 = newP3,
            player4 = newP4,
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
        val nextTurn = getNextTurn(state, playerId)
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
        val newP3 = if (playerId == PlayerId.PLAYER_3) updatedPlayer else state.player3
        val newP4 = if (playerId == PlayerId.PLAYER_4) updatedPlayer else state.player4

        val nextTurn = getNextTurn(state, playerId)

        return state.copy(
            player1 = newP1,
            player2 = newP2,
            player3 = newP3,
            player4 = newP4,
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
        val player = state.getPlayer(currentTurn)
        val newTime = maxOf(0L, player.timeRemainingMillis - elapsedMillis)

        val updatedPlayer = player.copy(timeRemainingMillis = newTime)
        val newP1 = if (currentTurn == PlayerId.PLAYER_1) updatedPlayer else state.player1
        val newP2 = if (currentTurn == PlayerId.PLAYER_2) updatedPlayer else state.player2
        val newP3 = if (currentTurn == PlayerId.PLAYER_3) updatedPlayer else state.player3
        val newP4 = if (currentTurn == PlayerId.PLAYER_4) updatedPlayer else state.player4

        if (newTime <= 0L) {
            // Player timed out
            val winner = when {
                state.rules.mode == GameMode.QUAD_MODE -> getNextTurn(state, currentTurn)
                currentTurn == PlayerId.PLAYER_1 -> PlayerId.PLAYER_2
                else -> PlayerId.PLAYER_1
            }
            return state.copy(
                player1 = newP1,
                player2 = newP2,
                player3 = newP3,
                player4 = newP4,
                status = GameStatus.FINISHED,
                winner = winner,
                finishReason = FinishReason.TIMEOUT
            )
        }

        return state.copy(
            player1 = newP1,
            player2 = newP2,
            player3 = newP3,
            player4 = newP4
        )
    }

    fun resign(state: GameState, resigningPlayer: PlayerId): GameState {
        if (state.status != GameStatus.IN_PROGRESS) return state
        val winner = when {
            state.rules.mode == GameMode.QUAD_MODE -> getNextTurn(state, resigningPlayer)
            resigningPlayer == PlayerId.PLAYER_1 -> PlayerId.PLAYER_2
            else -> PlayerId.PLAYER_1
        }
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
