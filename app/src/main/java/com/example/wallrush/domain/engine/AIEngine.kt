package com.example.wallrush.domain.engine

import com.example.wallrush.domain.model.AIDifficulty
import com.example.wallrush.domain.model.GameState
import com.example.wallrush.domain.model.PlayerId
import com.example.wallrush.domain.model.Position
import com.example.wallrush.domain.model.Wall
import com.example.wallrush.domain.npc.NPCPersonality
import kotlin.random.Random

sealed class AIAction {
    data class Move(val to: Position) : AIAction()
    data class PlaceWall(val wall: Wall) : AIAction()
}

object AIEngine {

    fun decideNPCMove(state: GameState, personality: NPCPersonality): AIAction {
        val aiPlayerId = state.currentTurn
        if (state.rules.mode == com.example.wallrush.domain.model.GameMode.QUAD_MODE) {
            return decideQuadAI(state, aiPlayerId)
        }

        val oppPlayerId = if (aiPlayerId == PlayerId.PLAYER_1) PlayerId.PLAYER_2 else PlayerId.PLAYER_1

        return when (personality) {
            NPCPersonality.THE_RUSHER -> decideRusher(state, aiPlayerId, oppPlayerId)
            NPCPersonality.THE_ARCHITECT -> decideArchitect(state, aiPlayerId, oppPlayerId)
            NPCPersonality.THE_TACTICIAN -> decideTactician(state, aiPlayerId, oppPlayerId)
            NPCPersonality.THE_COUNTER_PUNCHER -> decideCounterPuncher(state, aiPlayerId, oppPlayerId)
            NPCPersonality.THE_CHAOTIC -> decideChaotic(state, aiPlayerId, oppPlayerId)
            NPCPersonality.THE_DEFENDER -> decideDefender(state, aiPlayerId, oppPlayerId)
        }
    }

    fun decideMove(state: GameState, difficulty: AIDifficulty = state.rules.aiDifficulty): AIAction {
        val aiPlayerId = state.currentTurn
        if (state.rules.mode == com.example.wallrush.domain.model.GameMode.QUAD_MODE) {
            return decideQuadAI(state, aiPlayerId)
        }

        val oppPlayerId = if (aiPlayerId == PlayerId.PLAYER_1) PlayerId.PLAYER_2 else PlayerId.PLAYER_1

        return when (difficulty) {
            AIDifficulty.EASY -> decideEasy(state, aiPlayerId, oppPlayerId)
            AIDifficulty.MEDIUM -> decideMedium(state, aiPlayerId, oppPlayerId)
            AIDifficulty.HARD -> decideHard(state, aiPlayerId, oppPlayerId)
        }
    }

    private fun decideQuadAI(state: GameState, aiId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)
        val center = RuleEngine.QUAD_GOAL_CELL

        // 1. Instant win check
        if (center in legalMoves) {
            return AIAction.Move(center)
        }

        // 2. Defensive wall placement if an opponent is closer to the center
        val otherPlayers = state.getAllPlayers().filter { it.id != aiId }
        val myDist = PathFinder.shortestDistanceToCell(aiPlayer.position, center, state.walls)
        val closestOpp = otherPlayers.minByOrNull { PathFinder.shortestDistanceToCell(it.position, center, state.walls) }

        if (closestOpp != null && aiPlayer.remainingWalls > 0 && Random.nextFloat() < 0.35f) {
            val oppDist = PathFinder.shortestDistanceToCell(closestOpp.position, center, state.walls)
            if (oppDist <= myDist && oppDist <= 3) {
                val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
                val blockingWall = legalWalls.shuffled().firstOrNull { wall ->
                    val newOppDist = PathFinder.shortestDistanceToCell(closestOpp.position, center, state.walls + wall)
                    val newMyDist = PathFinder.shortestDistanceToCell(aiPlayer.position, center, state.walls + wall)
                    newOppDist > oppDist && newMyDist <= myDist + 1
                }
                if (blockingWall != null) {
                    return AIAction.PlaceWall(blockingWall)
                }
            }
        }

        // 3. Move along the shortest path towards the center cell
        val path = PathFinder.findShortestPathToCell(aiPlayer.position, center, state.walls)
        if (path.size > 1 && path[1] in legalMoves) {
            return AIAction.Move(path[1])
        }

        val bestMove = legalMoves.minByOrNull { PathFinder.shortestDistanceToCell(it, center, state.walls) }
        return AIAction.Move(bestMove ?: legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    private fun decideRusher(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val aiGoalRow = RuleEngine.getGoalRow(aiId, state.rules.mode)
        val oppGoalRow = RuleEngine.getGoalRow(oppId, state.rules.mode)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        // Instant win check
        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        val oppDist = PathFinder.shortestDistanceToGoal(oppPlayer.position, oppGoalRow, state.walls)

        // Rusher ONLY places walls if opponent is 1 or 2 steps from winning
        if (oppDist <= 2 && aiPlayer.remainingWalls > 0) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val oppPath = PathFinder.findShortestPath(oppPlayer.position, oppGoalRow, state.walls)
            val urgentWalls = legalWalls.filter { wall ->
                oppPath.any { kotlin.math.abs(wall.x - it.x) <= 1 && kotlin.math.abs(wall.y - it.y) <= 1 }
            }
            for (w in urgentWalls) {
                val newOppDist = PathFinder.shortestDistanceToGoal(oppPlayer.position, oppGoalRow, state.walls + w)
                if (newOppDist > oppDist) {
                    return AIAction.PlaceWall(w)
                }
            }
        }

        // Pure sprint along shortest path
        val shortestPath = PathFinder.findShortestPath(aiPlayer.position, aiGoalRow, state.walls)
        if (shortestPath.size > 1 && shortestPath[1] in legalMoves) {
            return AIAction.Move(shortestPath[1])
        }

        val bestMove = legalMoves.minByOrNull { PathFinder.shortestDistanceToGoal(it, aiGoalRow, state.walls) }
        return AIAction.Move(bestMove ?: legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    private fun decideArchitect(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val aiGoalRow = RuleEngine.getGoalRow(aiId, state.rules.mode)
        val oppGoalRow = RuleEngine.getGoalRow(oppId, state.rules.mode)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        val currentOppDist = PathFinder.shortestDistanceToGoal(oppPlayer.position, oppGoalRow, state.walls)
        val currentAiDist = PathFinder.shortestDistanceToGoal(aiPlayer.position, aiGoalRow, state.walls)

        // Architect loves placing maze walls early (while walls > 3 and move count < 18)
        if (aiPlayer.remainingWalls > 2 && (state.moveCount < 16 || currentOppDist <= currentAiDist + 1)) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val oppPath = PathFinder.findShortestPath(oppPlayer.position, oppGoalRow, state.walls)

            var bestWall: Wall? = null
            var maxDetour = 0

            val candidates = legalWalls.filter { w ->
                oppPath.any { kotlin.math.abs(w.x - it.x) <= 2 && kotlin.math.abs(w.y - it.y) <= 2 }
            }.take(25)

            for (w in candidates) {
                val test = state.walls + w
                val newOppDist = PathFinder.shortestDistanceToGoal(oppPlayer.position, oppGoalRow, test)
                val newAiDist = PathFinder.shortestDistanceToGoal(aiPlayer.position, aiGoalRow, test)
                val detour = (newOppDist - currentOppDist) * 2 - (newAiDist - currentAiDist)
                if (detour > maxDetour && newOppDist > currentOppDist) {
                    maxDetour = detour
                    bestWall = w
                }
            }

            if (bestWall != null && maxDetour >= 1) {
                return AIAction.PlaceWall(bestWall)
            }
        }

        val shortestPath = PathFinder.findShortestPath(aiPlayer.position, aiGoalRow, state.walls)
        if (shortestPath.size > 1 && shortestPath[1] in legalMoves) {
            return AIAction.Move(shortestPath[1])
        }

        val bestMove = legalMoves.minByOrNull { PathFinder.shortestDistanceToGoal(it, aiGoalRow, state.walls) }
        return AIAction.Move(bestMove ?: legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    private fun decideTactician(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        return decideHard(state, aiId, oppId)
    }

    private fun decideCounterPuncher(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val aiGoalRow = RuleEngine.getGoalRow(aiId, state.rules.mode)
        val oppGoalRow = RuleEngine.getGoalRow(oppId, state.rules.mode)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        // Opponent crossed midfield? Spring the trap!
        val oppCrossedMid = if (oppGoalRow == 8) oppPlayer.position.y >= 3 else oppPlayer.position.y <= 5
        val currentOppDist = PathFinder.shortestDistanceToGoal(oppPlayer.position, oppGoalRow, state.walls)

        if (oppCrossedMid && aiPlayer.remainingWalls > 0) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val oppPath = PathFinder.findShortestPath(oppPlayer.position, oppGoalRow, state.walls)

            val blockingWalls = legalWalls.filter { w ->
                oppPath.take(3).any { kotlin.math.abs(w.x - it.x) <= 1 && kotlin.math.abs(w.y - it.y) <= 1 }
            }

            var bestWall: Wall? = null
            var bestPenalty = 0

            for (w in blockingWalls) {
                val newOppDist = PathFinder.shortestDistanceToGoal(oppPlayer.position, oppGoalRow, state.walls + w)
                val penalty = newOppDist - currentOppDist
                if (penalty > bestPenalty) {
                    bestPenalty = penalty
                    bestWall = w
                }
            }

            if (bestWall != null && bestPenalty >= 2) {
                return AIAction.PlaceWall(bestWall)
            }
        }

        val shortestPath = PathFinder.findShortestPath(aiPlayer.position, aiGoalRow, state.walls)
        if (shortestPath.size > 1 && shortestPath[1] in legalMoves) {
            return AIAction.Move(shortestPath[1])
        }

        val bestMove = legalMoves.minByOrNull { PathFinder.shortestDistanceToGoal(it, aiGoalRow, state.walls) }
        return AIAction.Move(bestMove ?: legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    private fun decideChaotic(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val aiGoalRow = RuleEngine.getGoalRow(aiId, state.rules.mode)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        // 35% chance to place a wild wall if available
        if (aiPlayer.remainingWalls > 0 && Random.nextFloat() < 0.35f) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            if (legalWalls.isNotEmpty()) {
                val randomWall = legalWalls.random()
                // Ensure it doesn't hurt own path too much
                val myDistBefore = PathFinder.shortestDistanceToGoal(aiPlayer.position, aiGoalRow, state.walls)
                val myDistAfter = PathFinder.shortestDistanceToGoal(aiPlayer.position, aiGoalRow, state.walls + randomWall)
                if (myDistAfter <= myDistBefore + 1) {
                    return AIAction.PlaceWall(randomWall)
                }
            }
        }

        // Sometimes choose a sideways/flanking legal move to confuse opponent
        if (legalMoves.size > 1 && Random.nextFloat() < 0.25f) {
            val flankMove = legalMoves.random()
            return AIAction.Move(flankMove)
        }

        val shortestPath = PathFinder.findShortestPath(aiPlayer.position, aiGoalRow, state.walls)
        if (shortestPath.size > 1 && shortestPath[1] in legalMoves) {
            return AIAction.Move(shortestPath[1])
        }

        val bestMove = legalMoves.minByOrNull { PathFinder.shortestDistanceToGoal(it, aiGoalRow, state.walls) }
        return AIAction.Move(bestMove ?: legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    private fun decideDefender(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val aiGoalRow = RuleEngine.getGoalRow(aiId, state.rules.mode)
        val oppGoalRow = RuleEngine.getGoalRow(oppId, state.rules.mode)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        val currentOppDist = PathFinder.shortestDistanceToGoal(oppPlayer.position, oppGoalRow, state.walls)
        val currentAiDist = PathFinder.shortestDistanceToGoal(aiPlayer.position, aiGoalRow, state.walls)

        // Defender blocks opponent if opponent gets closer than AI
        if (aiPlayer.remainingWalls > 0 && currentOppDist <= currentAiDist) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val oppPath = PathFinder.findShortestPath(oppPlayer.position, oppGoalRow, state.walls)
            val tactical = legalWalls.filter { w ->
                oppPath.take(2).any { kotlin.math.abs(w.x - it.x) <= 1 && kotlin.math.abs(w.y - it.y) <= 1 }
            }

            for (w in tactical) {
                val newOppDist = PathFinder.shortestDistanceToGoal(oppPlayer.position, oppGoalRow, state.walls + w)
                val newAiDist = PathFinder.shortestDistanceToGoal(aiPlayer.position, aiGoalRow, state.walls + w)
                if (newOppDist > currentOppDist && newAiDist <= currentAiDist) {
                    return AIAction.PlaceWall(w)
                }
            }
        }

        val shortestPath = PathFinder.findShortestPath(aiPlayer.position, aiGoalRow, state.walls)
        if (shortestPath.size > 1 && shortestPath[1] in legalMoves) {
            return AIAction.Move(shortestPath[1])
        }

        val bestMove = legalMoves.minByOrNull { PathFinder.shortestDistanceToGoal(it, aiGoalRow, state.walls) }
        return AIAction.Move(bestMove ?: legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    private fun decideEasy(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val goalRow = RuleEngine.getGoalRow(aiId, state.rules.mode)
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
        val aiGoalRow = RuleEngine.getGoalRow(aiId, state.rules.mode)
        val oppGoalRow = RuleEngine.getGoalRow(oppId, state.rules.mode)

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
        val aiGoalRow = RuleEngine.getGoalRow(aiId, state.rules.mode)
        val oppGoalRow = RuleEngine.getGoalRow(oppId, state.rules.mode)

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
