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

    private fun getAiGoal(state: GameState, id: PlayerId): Int =
        RuleEngine.getPlayerGoalRow(state, id)

    private fun dist(state: GameState, pos: Position, goalRow: Int, walls: List<Wall> = state.walls): Int =
        PathFinder.shortestDistanceToGoal(pos, goalRow, walls, state.rules.gridSize, state.obstacles)

    private fun path(state: GameState, pos: Position, goalRow: Int, walls: List<Wall> = state.walls): List<Position> =
        PathFinder.findShortestPath(pos, goalRow, walls, state.rules.gridSize, state.obstacles)

    private fun distCell(state: GameState, pos: Position, cell: Position, walls: List<Wall> = state.walls): Int =
        PathFinder.shortestDistanceToCell(pos, cell, walls, state.rules.gridSize, state.obstacles)

    private fun pathCell(state: GameState, pos: Position, cell: Position, walls: List<Wall> = state.walls): List<Position> =
        PathFinder.findShortestPathToCell(pos, cell, walls, state.rules.gridSize, state.obstacles)

    fun decideNPCMove(state: GameState, personality: NPCPersonality, context: android.content.Context? = null): AIAction {
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
            NPCPersonality.THE_PREDICTOR -> decidePredictor(state, aiPlayerId, oppPlayerId, context)
            NPCPersonality.THE_TRICKSTER -> decideTrickster(state, aiPlayerId, oppPlayerId)
            NPCPersonality.THE_SPEED_DEMON -> decideSpeedDemon(state, aiPlayerId, oppPlayerId)
            NPCPersonality.THE_MERCILESS -> decideMerciless(state, aiPlayerId, oppPlayerId)
        }
    }

    fun decideMove(state: GameState, difficulty: AIDifficulty = state.rules.aiDifficulty, context: android.content.Context? = null): AIAction {
        val aiPlayerId = state.currentTurn
        if (state.rules.mode == com.example.wallrush.domain.model.GameMode.QUAD_MODE) {
            return decideQuadAI(state, aiPlayerId)
        }

        val oppPlayerId = if (aiPlayerId == PlayerId.PLAYER_1) PlayerId.PLAYER_2 else PlayerId.PLAYER_1

        return when (difficulty) {
            AIDifficulty.EASY -> decideEasy(state, aiPlayerId, oppPlayerId)
            AIDifficulty.MEDIUM -> decideMedium(state, aiPlayerId, oppPlayerId)
            AIDifficulty.HARD -> decideHard(state, aiPlayerId, oppPlayerId)
            AIDifficulty.EXPERT -> decideExpert(state, aiPlayerId, oppPlayerId)
            AIDifficulty.NIGHTMARE -> decideNightmare(state, aiPlayerId, oppPlayerId, context)
            AIDifficulty.INSANE -> decideInsane(state, aiPlayerId, oppPlayerId, context)
        }
    }

    private fun decideQuadAI(state: GameState, aiId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)
        val center = Position(state.rules.gridSize / 2, state.rules.gridSize / 2)

        // 1. Instant win check
        if (center in legalMoves) {
            return AIAction.Move(center)
        }

        // 2. Defensive wall placement if an opponent is closer to the center
        val otherPlayers = state.getAllPlayers().filter { it.id != aiId }
        val myDist = distCell(state, aiPlayer.position, center, state.walls)
        val closestOpp = otherPlayers.minByOrNull { distCell(state, it.position, center, state.walls) }

        if (closestOpp != null && aiPlayer.remainingWalls > 0 && Random.nextFloat() < 0.35f) {
            val oppDist = distCell(state, closestOpp.position, center, state.walls)
            if (oppDist <= myDist && oppDist <= 3) {
                val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
                val blockingWall = legalWalls.shuffled().firstOrNull { wall ->
                    val newOppDist = distCell(state, closestOpp.position, center, state.walls + wall)
                    val newMyDist = distCell(state, aiPlayer.position, center, state.walls + wall)
                    newOppDist > oppDist && newMyDist <= myDist + 1
                }
                if (blockingWall != null) {
                    return AIAction.PlaceWall(blockingWall)
                }
            }
        }

        // 3. Move along the shortest path towards the center cell
        val path = pathCell(state, aiPlayer.position, center, state.walls)
        if (path.size > 1 && path[1] in legalMoves) {
            return AIAction.Move(path[1])
        }

        val bestMove = legalMoves.minByOrNull { distCell(state, it, center, state.walls) }
        return AIAction.Move(bestMove ?: legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    private fun decideRusher(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val aiGoalRow = getAiGoal(state, aiId)
        val oppGoalRow = getAiGoal(state, oppId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        // Instant win check
        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        val oppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls)

        // Rusher ONLY places walls if opponent is 1 or 2 steps from winning
        if (oppDist <= 2 && aiPlayer.remainingWalls > 0) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val oppPath = path(state, oppPlayer.position, oppGoalRow, state.walls)
            val urgentWalls = legalWalls.filter { wall ->
                oppPath.any { kotlin.math.abs(wall.x - it.x) <= 1 && kotlin.math.abs(wall.y - it.y) <= 1 }
            }
            for (w in urgentWalls) {
                val newOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls + w)
                if (newOppDist > oppDist) {
                    return AIAction.PlaceWall(w)
                }
            }
        }

        // Pure sprint along shortest path
        val shortestPath = path(state, aiPlayer.position, aiGoalRow, state.walls)
        if (shortestPath.size > 1 && shortestPath[1] in legalMoves) {
            return AIAction.Move(shortestPath[1])
        }

        val bestMove = legalMoves.minByOrNull { dist(state, it, aiGoalRow, state.walls) }
        return AIAction.Move(bestMove ?: legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    private fun decideArchitect(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val aiGoalRow = getAiGoal(state, aiId)
        val oppGoalRow = getAiGoal(state, oppId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        val currentOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls)
        val currentAiDist = dist(state, aiPlayer.position, aiGoalRow, state.walls)

        // Architect loves placing maze walls early
        if (aiPlayer.remainingWalls > 2 && (state.moveCount < 16 || currentOppDist <= currentAiDist + 1)) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val oppPath = path(state, oppPlayer.position, oppGoalRow, state.walls)

            var bestWall: Wall? = null
            var maxDetour = 0

            val candidates = legalWalls.filter { w ->
                oppPath.any { kotlin.math.abs(w.x - it.x) <= 2 && kotlin.math.abs(w.y - it.y) <= 2 }
            }.take(25)

            for (w in candidates) {
                val test = state.walls + w
                val newOppDist = dist(state, oppPlayer.position, oppGoalRow, test)
                val newAiDist = dist(state, aiPlayer.position, aiGoalRow, test)
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

        val shortestPath = path(state, aiPlayer.position, aiGoalRow, state.walls)
        if (shortestPath.size > 1 && shortestPath[1] in legalMoves) {
            return AIAction.Move(shortestPath[1])
        }

        val bestMove = legalMoves.minByOrNull { dist(state, it, aiGoalRow, state.walls) }
        return AIAction.Move(bestMove ?: legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    private fun decideTactician(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        return decideHard(state, aiId, oppId)
    }

    private fun decideCounterPuncher(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val aiGoalRow = getAiGoal(state, aiId)
        val oppGoalRow = getAiGoal(state, oppId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        val oppCrossedMid = if (oppGoalRow > aiGoalRow) oppPlayer.position.y >= state.rules.gridSize / 2 else oppPlayer.position.y <= state.rules.gridSize / 2
        val currentOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls)

        if (oppCrossedMid && aiPlayer.remainingWalls > 0) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val oppPath = path(state, oppPlayer.position, oppGoalRow, state.walls)

            val blockingWalls = legalWalls.filter { w ->
                oppPath.take(3).any { kotlin.math.abs(w.x - it.x) <= 1 && kotlin.math.abs(w.y - it.y) <= 1 }
            }

            var bestWall: Wall? = null
            var bestPenalty = 0

            for (w in blockingWalls) {
                val newOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls + w)
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

        val shortestPath = path(state, aiPlayer.position, aiGoalRow, state.walls)
        if (shortestPath.size > 1 && shortestPath[1] in legalMoves) {
            return AIAction.Move(shortestPath[1])
        }

        val bestMove = legalMoves.minByOrNull { dist(state, it, aiGoalRow, state.walls) }
        return AIAction.Move(bestMove ?: legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    private fun decideChaotic(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val aiGoalRow = getAiGoal(state, aiId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        // 35% chance to place a wild wall if available
        if (aiPlayer.remainingWalls > 0 && Random.nextFloat() < 0.35f) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            if (legalWalls.isNotEmpty()) {
                val randomWall = legalWalls.random()
                val myDistBefore = dist(state, aiPlayer.position, aiGoalRow, state.walls)
                val myDistAfter = dist(state, aiPlayer.position, aiGoalRow, state.walls + randomWall)
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

        val shortestPath = path(state, aiPlayer.position, aiGoalRow, state.walls)
        if (shortestPath.size > 1 && shortestPath[1] in legalMoves) {
            return AIAction.Move(shortestPath[1])
        }

        val bestMove = legalMoves.minByOrNull { dist(state, it, aiGoalRow, state.walls) }
        return AIAction.Move(bestMove ?: legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    private fun decideDefender(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val aiGoalRow = getAiGoal(state, aiId)
        val oppGoalRow = getAiGoal(state, oppId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        val currentOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls)
        val currentAiDist = dist(state, aiPlayer.position, aiGoalRow, state.walls)

        // Defender blocks opponent if opponent gets closer than AI
        if (aiPlayer.remainingWalls > 0 && currentOppDist <= currentAiDist) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val oppPath = path(state, oppPlayer.position, oppGoalRow, state.walls)
            val tactical = legalWalls.filter { w ->
                oppPath.take(2).any { kotlin.math.abs(w.x - it.x) <= 1 && kotlin.math.abs(w.y - it.y) <= 1 }
            }

            for (w in tactical) {
                val newOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls + w)
                val newAiDist = dist(state, aiPlayer.position, aiGoalRow, state.walls + w)
                if (newOppDist > currentOppDist && newAiDist <= currentAiDist) {
                    return AIAction.PlaceWall(w)
                }
            }
        }

        val shortestPath = path(state, aiPlayer.position, aiGoalRow, state.walls)
        if (shortestPath.size > 1 && shortestPath[1] in legalMoves) {
            return AIAction.Move(shortestPath[1])
        }

        val bestMove = legalMoves.minByOrNull { dist(state, it, aiGoalRow, state.walls) }
        return AIAction.Move(bestMove ?: legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    private fun decideEasy(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val goalRow = getAiGoal(state, aiId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        if (aiPlayer.remainingWalls > 0 && Random.nextFloat() < 0.20f) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            if (legalWalls.isNotEmpty()) {
                return AIAction.PlaceWall(legalWalls.random())
            }
        }

        val shortestPath = path(state, aiPlayer.position, goalRow, state.walls)
        if (shortestPath.size > 1 && shortestPath[1] in legalMoves) {
            return AIAction.Move(shortestPath[1])
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
        val aiGoalRow = getAiGoal(state, aiId)
        val oppGoalRow = getAiGoal(state, oppId)

        val currentAiDist = dist(state, aiPlayer.position, aiGoalRow, state.walls)
        val currentOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        if (aiPlayer.remainingWalls > 0 && (currentOppDist <= currentAiDist || currentOppDist <= 4)) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)

            var bestWall: Wall? = null
            var bestGain = 0

            val priorityWalls = legalWalls.filter { wall ->
                val distToOpp = kotlin.math.abs(wall.x - oppPlayer.position.x) + kotlin.math.abs(wall.y - oppPlayer.position.y)
                distToOpp <= 3
            }

            val wallsToEvaluate = if (priorityWalls.isNotEmpty()) priorityWalls else legalWalls

            for (wall in wallsToEvaluate) {
                val testWalls = state.walls + wall
                val newOppDist = dist(state, oppPlayer.position, oppGoalRow, testWalls)
                val newAiDist = dist(state, aiPlayer.position, aiGoalRow, testWalls)

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

        val shortestPath = path(state, aiPlayer.position, aiGoalRow, state.walls)
        if (shortestPath.size > 1 && shortestPath[1] in legalMoves) {
            return AIAction.Move(shortestPath[1])
        }

        val bestMove = legalMoves.minByOrNull { dist(state, it, aiGoalRow, state.walls) }
        return AIAction.Move(bestMove ?: legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    private fun decideHard(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val aiGoalRow = getAiGoal(state, aiId)
        val oppGoalRow = getAiGoal(state, oppId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)
        val midX = state.rules.gridSize / 2

        // Immediate win check
        for (move in legalMoves) {
            if (move.y == aiGoalRow) {
                return AIAction.Move(move)
            }
        }

        var bestAction: AIAction? = null
        var bestScore = Double.NEGATIVE_INFINITY

        val currentAiDist = dist(state, aiPlayer.position, aiGoalRow, state.walls)
        val currentOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls)

        // 1. Evaluate legal pawn moves
        for (move in legalMoves) {
            val nextAiDist = dist(state, move, aiGoalRow, state.walls)
            val centerBonus = (midX - kotlin.math.abs(move.x - midX)) * 0.5
            val score = (currentOppDist - nextAiDist) * 10.0 + centerBonus
            if (score > bestScore) {
                bestScore = score
                bestAction = AIAction.Move(move)
            }
        }

        // 2. Evaluate tactical walls
        if (aiPlayer.remainingWalls > 0) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val oppPath = path(state, oppPlayer.position, oppGoalRow, state.walls)
            val tacticalWalls = legalWalls.filter { wall ->
                oppPath.any { pos ->
                    kotlin.math.abs(wall.x - pos.x) <= 1 && kotlin.math.abs(wall.y - pos.y) <= 1
                }
            }

            val wallsToCheck = if (tacticalWalls.isNotEmpty()) tacticalWalls else legalWalls.take(30)

            for (wall in wallsToCheck) {
                val testWalls = state.walls + wall
                val newOppDist = dist(state, oppPlayer.position, oppGoalRow, testWalls)
                val newAiDist = dist(state, aiPlayer.position, aiGoalRow, testWalls)

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

    private fun decidePredictor(
        state: GameState,
        aiId: PlayerId,
        oppId: PlayerId,
        context: android.content.Context?
    ): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val aiGoalRow = getAiGoal(state, aiId)
        val oppGoalRow = getAiGoal(state, oppId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        val style = context?.let { com.example.wallrush.domain.ai.PlayerPatternTracker.getPlayerStyle(it) }
        val prefersRight = style?.prefersRightEscape ?: true

        if (aiPlayer.remainingWalls > 0) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val targetedWalls = legalWalls.filter { w ->
                if (prefersRight) {
                    w.x >= oppPlayer.position.x && kotlin.math.abs(w.y - oppPlayer.position.y) <= 2
                } else {
                    w.x <= oppPlayer.position.x && kotlin.math.abs(w.y - oppPlayer.position.y) <= 2
                }
            }

            val currentOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls)
            val bestInterception = targetedWalls.maxByOrNull { w ->
                val newOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls + w)
                newOppDist - currentOppDist
            }

            if (bestInterception != null) {
                val newOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls + bestInterception)
                if (newOppDist > currentOppDist) {
                    return AIAction.PlaceWall(bestInterception)
                }
            }
        }

        return decideHard(state, aiId, oppId)
    }

    private fun decideTrickster(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val aiGoalRow = getAiGoal(state, aiId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        if (aiPlayer.remainingWalls > 1 && Random.nextFloat() < 0.45f) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val corridorWall = legalWalls.firstOrNull { w ->
                kotlin.math.abs(w.x - oppPlayer.position.x) == 1 && kotlin.math.abs(w.y - oppPlayer.position.y) <= 1
            }
            if (corridorWall != null) {
                return AIAction.PlaceWall(corridorWall)
            }
        }

        return decideHard(state, aiId, oppId)
    }

    private fun decideSpeedDemon(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val aiGoalRow = getAiGoal(state, aiId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        val shortestPath = path(state, aiPlayer.position, aiGoalRow, state.walls)
        if (shortestPath.size > 1 && shortestPath[1] in legalMoves) {
            return AIAction.Move(shortestPath[1])
        }

        val bestMove = legalMoves.minByOrNull { dist(state, it, aiGoalRow, state.walls) }
        return AIAction.Move(bestMove ?: legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    private fun decideMerciless(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val aiGoalRow = getAiGoal(state, aiId)
        val oppGoalRow = getAiGoal(state, oppId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        val currentOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls)
        val currentAiDist = dist(state, aiPlayer.position, aiGoalRow, state.walls)

        if (aiPlayer.remainingWalls > 0) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            var bestWall: Wall? = null
            var bestPenalty = 0

            for (w in legalWalls.shuffled().take(40)) {
                val test = state.walls + w
                val newOppDist = dist(state, oppPlayer.position, oppGoalRow, test)
                val newAiDist = dist(state, aiPlayer.position, aiGoalRow, test)
                val penalty = (newOppDist - currentOppDist) * 3 - (newAiDist - currentAiDist)
                if (penalty > bestPenalty && newOppDist > currentOppDist) {
                    bestPenalty = penalty
                    bestWall = w
                }
            }

            if (bestWall != null && bestPenalty >= 2) {
                return AIAction.PlaceWall(bestWall)
            }
        }

        return decideHard(state, aiId, oppId)
    }

    private fun decideExpert(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        return decideHard(state, aiId, oppId)
    }

    private fun decideNightmare(
        state: GameState,
        aiId: PlayerId,
        oppId: PlayerId,
        context: android.content.Context?
    ): AIAction {
        val style = context?.let { com.example.wallrush.domain.ai.PlayerPatternTracker.getPlayerStyle(it) }
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val oppGoalRow = getAiGoal(state, oppId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)
        val aiGoalRow = getAiGoal(state, aiId)

        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        if (aiPlayer.remainingWalls > 0) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val currentOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls)

            val biasX = if (style?.prefersRightEscape == true) 1 else -1
            val prioritizedWalls = legalWalls.filter { w ->
                (w.x - oppPlayer.position.x) * biasX >= 0 && kotlin.math.abs(w.y - oppPlayer.position.y) <= 2
            }

            val candidate = prioritizedWalls.maxByOrNull { w ->
                dist(state, oppPlayer.position, oppGoalRow, state.walls + w) - currentOppDist
            }

            if (candidate != null) {
                val gain = dist(state, oppPlayer.position, oppGoalRow, state.walls + candidate) - currentOppDist
                if (gain >= 2) {
                    return AIAction.PlaceWall(candidate)
                }
            }
        }

        return decideHard(state, aiId, oppId)
    }

    private fun decideInsane(
        state: GameState,
        aiId: PlayerId,
        oppId: PlayerId,
        context: android.content.Context?
    ): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val aiGoalRow = getAiGoal(state, aiId)
        val oppGoalRow = getAiGoal(state, oppId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)
        val midX = state.rules.gridSize / 2

        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        var bestAction: AIAction? = null
        var bestEval = Double.NEGATIVE_INFINITY

        val currentAiDist = dist(state, aiPlayer.position, aiGoalRow, state.walls)
        val currentOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls)

        for (move in legalMoves) {
            val nextAiDist = dist(state, move, aiGoalRow, state.walls)
            val evaluation = (currentOppDist - nextAiDist) * 12.0 + (midX - kotlin.math.abs(move.x - midX))
            if (evaluation > bestEval) {
                bestEval = evaluation
                bestAction = AIAction.Move(move)
            }
        }

        if (aiPlayer.remainingWalls > 0) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val oppPath = path(state, oppPlayer.position, oppGoalRow, state.walls)

            val criticalWalls = legalWalls.filter { w ->
                oppPath.take(4).any { p -> kotlin.math.abs(w.x - p.x) <= 1 && kotlin.math.abs(w.y - p.y) <= 1 }
            }

            for (w in criticalWalls) {
                val test = state.walls + w
                val newOppDist = dist(state, oppPlayer.position, oppGoalRow, test)
                val newAiDist = dist(state, aiPlayer.position, aiGoalRow, test)

                if (newOppDist > currentOppDist) {
                    val oppPenalty = (newOppDist - currentOppDist) * 20.0
                    val selfPenalty = (newAiDist - currentAiDist) * 22.0
                    val eval = (newOppDist - newAiDist) * 15.0 + oppPenalty - selfPenalty + (aiPlayer.remainingWalls * 1.5)
                    if (eval > bestEval) {
                        bestEval = eval
                        bestAction = AIAction.PlaceWall(w)
                    }
                }
            }
        }

        return bestAction ?: AIAction.Move(legalMoves.firstOrNull() ?: aiPlayer.position)
    }
}
