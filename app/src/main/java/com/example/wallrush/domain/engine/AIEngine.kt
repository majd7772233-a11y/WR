package com.example.wallrush.domain.engine

import com.example.wallrush.domain.model.AIDifficulty
import com.example.wallrush.domain.model.GameState
import com.example.wallrush.domain.model.PlayerId
import com.example.wallrush.domain.model.Position
import com.example.wallrush.domain.model.Wall
import kotlin.random.Random

sealed class AIAction {
    data class Move(val to: Position) : AIAction()
    data class PlaceWall(val wall: Wall) : AIAction()
}

object AIEngine {

    fun decideMove(state: GameState, difficulty: AIDifficulty = state.rules.aiDifficulty): AIAction {
        val aiPlayerId = state.currentTurn
        val oppPlayerId = if (aiPlayerId == PlayerId.PLAYER_1) PlayerId.PLAYER_2 else PlayerId.PLAYER_1

        return when (difficulty) {
            AIDifficulty.EASY -> decideEasy(state, aiPlayerId, oppPlayerId)
            AIDifficulty.MEDIUM -> decideMedium(state, aiPlayerId, oppPlayerId)
            AIDifficulty.HARD -> decideHard(state, aiPlayerId, oppPlayerId)
        }
    }

    private fun decideEasy(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val goalRow = RuleEngine.getGoalRow(aiId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        // 20% chance to try a wall if available
        if (aiPlayer.remainingWalls > 0 && Random.nextFloat() < 0.20f) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            if (legalWalls.isNotEmpty()) {
                val candidateWall = legalWalls.random()
                return AIAction.PlaceWall(candidateWall)
            }
        }

        // Shortest path move or random legal move
        val shortestPath = PathFinder.findShortestPath(aiPlayer.position, goalRow, state.walls)
        if (shortestPath.size > 1) {
            val nextStep = shortestPath[1]
            if (nextStep in legalMoves) {
                return AIAction.Move(nextStep)
            }
        }

        return if (legalMoves.isNotEmpty()) {
            AIAction.Move(legalMoves.random())
        } else {
            AIAction.Move(aiPlayer.position)
        }
    }

    private fun decideMedium(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val aiGoalRow = RuleEngine.getGoalRow(aiId)
        val oppGoalRow = RuleEngine.getGoalRow(oppId)

        val currentAiDist = PathFinder.shortestDistanceToGoal(aiPlayer.position, aiGoalRow, state.walls)
        val currentOppDist = PathFinder.shortestDistanceToGoal(oppPlayer.position, oppGoalRow, state.walls)

        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        // If opponent is closer or equal distance and AI has walls, consider tactical wall
        if (aiPlayer.remainingWalls > 0 && (currentOppDist <= currentAiDist || currentOppDist <= 4)) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)

            var bestWall: Wall? = null
            var bestGain = 0

            // Sample or evaluate high-potential walls near the opponent
            val priorityWalls = legalWalls.filter { wall ->
                val distToOpp = kotlin.math.abs(wall.x - oppPlayer.position.x) + kotlin.math.abs(wall.y - oppPlayer.position.y)
                distToOpp <= 3
            }

            val wallsToEvaluate = if (priorityWalls.isNotEmpty()) priorityWalls else legalWalls

            for (wall in wallsToEvaluate) {
                val testWalls = state.walls + wall
                val newOppDist = PathFinder.shortestDistanceToGoal(oppPlayer.position, oppGoalRow, testWalls)
                val newAiDist = PathFinder.shortestDistanceToGoal(aiPlayer.position, aiGoalRow, testWalls)

                val oppDistIncrease = newOppDist - currentOppDist
                val aiDistIncrease = newAiDist - currentAiDist

                val netGain = (oppDistIncrease * 2) - (aiDistIncrease * 3)
                if (oppDistIncrease >= 2 && netGain > bestGain) {
                    bestGain = netGain
                    bestWall = wall
                }
            }

            if (bestWall != null && bestGain >= 2) {
                return AIAction.PlaceWall(bestWall)
            }
        }

        // Follow shortest path
        val shortestPath = PathFinder.findShortestPath(aiPlayer.position, aiGoalRow, state.walls)
        if (shortestPath.size > 1) {
            val nextStep = shortestPath[1]
            if (nextStep in legalMoves) {
                return AIAction.Move(nextStep)
            }
        }

        // Best move towards goal row
        val bestMove = legalMoves.minByOrNull { move ->
            PathFinder.shortestDistanceToGoal(move, aiGoalRow, state.walls)
        }

        return AIAction.Move(bestMove ?: legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    private fun decideHard(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val aiGoalRow = RuleEngine.getGoalRow(aiId)
        val oppGoalRow = RuleEngine.getGoalRow(oppId)

        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        // Immediate win check: if any legal move reaches the goal row, TAKE IT!
        for (move in legalMoves) {
            if (move.y == aiGoalRow) {
                return AIAction.Move(move)
            }
        }

        var bestAction: AIAction? = null
        var bestScore = Double.NEGATIVE_INFINITY

        val currentAiDist = PathFinder.shortestDistanceToGoal(aiPlayer.position, aiGoalRow, state.walls)
        val currentOppDist = PathFinder.shortestDistanceToGoal(oppPlayer.position, oppGoalRow, state.walls)

        // 1. Evaluate legal pawn moves
        for (move in legalMoves) {
            val nextAiDist = PathFinder.shortestDistanceToGoal(move, aiGoalRow, state.walls)
            // Center control bonus
            val centerBonus = (4 - kotlin.math.abs(move.x - 4)) * 0.5
            val score = (currentOppDist - nextAiDist) * 10.0 + centerBonus
            if (score > bestScore) {
                bestScore = score
                bestAction = AIAction.Move(move)
            }
        }

        // 2. Evaluate tactical walls if AI has walls
        if (aiPlayer.remainingWalls > 0) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            // Filter walls near opponent's path for efficiency
            val oppPath = PathFinder.findShortestPath(oppPlayer.position, oppGoalRow, state.walls)
            val tacticalWalls = legalWalls.filter { wall ->
                oppPath.any { pos ->
                    kotlin.math.abs(wall.x - pos.x) <= 1 && kotlin.math.abs(wall.y - pos.y) <= 1
                }
            }

            val wallsToCheck = if (tacticalWalls.isNotEmpty()) tacticalWalls else legalWalls.take(30)

            for (wall in wallsToCheck) {
                val testWalls = state.walls + wall
                val newOppDist = PathFinder.shortestDistanceToGoal(oppPlayer.position, oppGoalRow, testWalls)
                val newAiDist = PathFinder.shortestDistanceToGoal(aiPlayer.position, aiGoalRow, testWalls)

                if (newOppDist > currentOppDist) {
                    val oppPenalty = (newOppDist - currentOppDist) * 14.0
                    val selfPenalty = (newAiDist - currentAiDist) * 18.0
                    val wallConserve = (aiPlayer.remainingWalls - oppPlayer.remainingWalls) * 2.0
                    val score = (newOppDist - newAiDist) * 10.0 + oppPenalty - selfPenalty + wallConserve

                    if (score > bestScore) {
                        bestScore = score
                        bestAction = AIAction.PlaceWall(wall)
                    }
                }
            }
        }

        return bestAction ?: AIAction.Move(legalMoves.firstOrNull() ?: aiPlayer.position)
    }
}
