package com.example.wallrush.domain.engine

import com.example.wallrush.domain.model.*

class ReplayEngine(
    val initialRules: GameRules,
    val player1Name: String,
    val player2Name: String,
    val player1Avatar: Int,
    val player2Avatar: Int,
    val events: List<GameEvent>
) {
    val states: List<GameState>

    init {
        val initial = GameEngine.createInitialState(
            rules = initialRules,
            player1Name = player1Name,
            player2Name = player2Name,
            player1Avatar = player1Avatar,
            player2Avatar = player2Avatar
        ).copy(status = GameStatus.IN_PROGRESS)

        val list = mutableListOf<GameState>()
        list.add(initial)

        var current = initial
        for (event in events) {
            current = when (event) {
                is GameEvent.PawnMoved -> {
                    GameEngine.makeMove(current, event.to, event.player)
                }
                is GameEvent.WallPlaced -> {
                    GameEngine.placeWall(current, event.wall, event.player)
                }
                is GameEvent.EmoteSent -> {
                    GameEngine.addEmote(current, event.player, event.emoji)
                }
            }
            list.add(current)
        }
        states = list
    }

    val totalSteps: Int get() = states.size - 1

    fun getStateAtStep(step: Int): GameState {
        val clamped = step.coerceIn(0, totalSteps)
        return states[clamped]
    }
}
