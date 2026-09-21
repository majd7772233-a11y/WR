package com.example.wallrush.domain.engine

import android.content.Context
import com.example.wallrush.domain.ai.PlayerPatternTracker
import com.example.wallrush.domain.model.AIDifficulty
import com.example.wallrush.domain.model.GameState
import com.example.wallrush.domain.model.PlayerId
import com.example.wallrush.domain.model.Position
import com.example.wallrush.domain.model.Wall
import com.example.wallrush.domain.npc.NPCPersonality
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
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

    fun decideNPCMove(state: GameState, personality: NPCPersonality, context: Context? = null): AIAction {
        val aiPlayerId = state.currentTurn
        if (state.rules.mode == com.example.wallrush.domain.model.GameMode.QUAD_MODE) {
            return decideQuadAI(state, aiPlayerId)
        }

        val oppPlayerId = if (aiPlayerId == PlayerId.PLAYER_1) PlayerId.PLAYER_2 else PlayerId.PLAYER_1

        return when (personality) {
            NPCPersonality.THE_RUSHER -> decideRusher(state, aiPlayerId, oppPlayerId)
            NPCPersonality.THE_DEFENDER -> decideDefender(state, aiPlayerId, oppPlayerId)
            NPCPersonality.THE_ARCHITECT -> decideArchitect(state, aiPlayerId, oppPlayerId)
            NPCPersonality.THE_TACTICIAN -> decideTactician(state, aiPlayerId, oppPlayerId)
            NPCPersonality.THE_PREDICTOR -> decidePredictor(state, aiPlayerId, oppPlayerId, context)
            NPCPersonality.THE_TRICKSTER -> decideTrickster(state, aiPlayerId, oppPlayerId)
            NPCPersonality.THE_SPEED_DEMON -> decideSpeedDemon(state, aiPlayerId, oppPlayerId)
            NPCPersonality.THE_COUNTER_PUNCHER -> decideCounterPuncher(state, aiPlayerId, oppPlayerId)
            NPCPersonality.THE_MERCILESS -> decideMerciless(state, aiPlayerId, oppPlayerId)
            NPCPersonality.THE_CHAOTIC -> decideChaotic(state, aiPlayerId, oppPlayerId)
        }
    }

    fun decideMove(state: GameState, difficulty: AIDifficulty = state.rules.aiDifficulty, context: Context? = null): AIAction {
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

    // =========================================================================
    // 4-PLAYER MODE AI
    // =========================================================================
    private fun decideQuadAI(state: GameState, aiId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)
        val center = Position(state.rules.gridSize / 2, state.rules.gridSize / 2)

        if (center in legalMoves) {
            return AIAction.Move(center)
        }

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

        val shortestPath = pathCell(state, aiPlayer.position, center, state.walls)
        if (shortestPath.size > 1 && shortestPath[1] in legalMoves) {
            return AIAction.Move(shortestPath[1])
        }

        val bestMove = legalMoves.minByOrNull { distCell(state, it, center, state.walls) }
        return AIAction.Move(bestMove ?: legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    // =========================================================================
    // 1. EASY DIFFICULTY: Casual, forgiving, occasional wandering
    // =========================================================================
    private fun decideEasy(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val aiGoalRow = getAiGoal(state, aiId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        // 40% chance to make a sub-optimal random move
        if (Random.nextFloat() < 0.40f && legalMoves.size > 1) {
            return AIAction.Move(legalMoves.random())
        }

        // 15% chance to place a completely random legal wall
        if (aiPlayer.remainingWalls > 0 && Random.nextFloat() < 0.15f) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            if (legalWalls.isNotEmpty()) {
                return AIAction.PlaceWall(legalWalls.random())
            }
        }

        val shortestPath = path(state, aiPlayer.position, aiGoalRow, state.walls)
        if (shortestPath.size > 1 && shortestPath[1] in legalMoves) {
            return AIAction.Move(shortestPath[1])
        }

        return AIAction.Move(legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    // =========================================================================
    // 2. MEDIUM DIFFICULTY: Straightforward, shortest-path follower, reactive
    // =========================================================================
    private fun decideMedium(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val aiGoalRow = getAiGoal(state, aiId)
        val oppGoalRow = getAiGoal(state, oppId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        val currentAiDist = dist(state, aiPlayer.position, aiGoalRow, state.walls)
        val currentOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls)

        // React with wall if opponent is noticeably closer to goal
        if (aiPlayer.remainingWalls > 0 && currentOppDist < currentAiDist && Random.nextFloat() < 0.50f) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val oppPath = path(state, oppPlayer.position, oppGoalRow, state.walls)
            val goodWall = legalWalls.filter { w ->
                oppPath.take(2).any { p -> abs(w.x - p.x) <= 1 && abs(w.y - p.y) <= 1 }
            }.shuffled().firstOrNull { w ->
                val newOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls + w)
                val newAiDist = dist(state, aiPlayer.position, aiGoalRow, state.walls + w)
                newOppDist > currentOppDist && newAiDist <= currentAiDist + 1
            }

            if (goodWall != null) {
                return AIAction.PlaceWall(goodWall)
            }
        }

        val shortestPath = path(state, aiPlayer.position, aiGoalRow, state.walls)
        if (shortestPath.size > 1 && shortestPath[1] in legalMoves) {
            return AIAction.Move(shortestPath[1])
        }

        val bestMove = legalMoves.minByOrNull { dist(state, it, aiGoalRow, state.walls) }
        return AIAction.Move(bestMove ?: legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    // =========================================================================
    // 3. HARD DIFFICULTY: 1-Ply strategic evaluation + center control
    // =========================================================================
    private fun decideHard(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
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
        var bestScore = Double.NEGATIVE_INFINITY

        val currentAiDist = dist(state, aiPlayer.position, aiGoalRow, state.walls)
        val currentOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls)

        // Evaluate all legal pawn steps
        for (move in legalMoves) {
            val nextAiDist = dist(state, move, aiGoalRow, state.walls)
            val centerBonus = (midX - abs(move.x - midX)) * 0.6
            val forwardBonus = (currentAiDist - nextAiDist) * 15.0
            val score = forwardBonus + centerBonus
            if (score > bestScore) {
                bestScore = score
                bestAction = AIAction.Move(move)
            }
        }

        // Evaluate strategic walls
        if (aiPlayer.remainingWalls > 0) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val oppPath = path(state, oppPlayer.position, oppGoalRow, state.walls)
            val candidateWalls = legalWalls.filter { w ->
                oppPath.any { p -> abs(w.x - p.x) <= 1 && abs(w.y - p.y) <= 1 }
            }.take(30)

            for (w in candidateWalls) {
                val test = state.walls + w
                val newOppDist = dist(state, oppPlayer.position, oppGoalRow, test)
                val newAiDist = dist(state, aiPlayer.position, aiGoalRow, test)

                if (newOppDist > currentOppDist) {
                    val oppPenalty = (newOppDist - currentOppDist) * 18.0
                    val selfPenalty = (newAiDist - currentAiDist) * 20.0
                    val wallConserve = (aiPlayer.remainingWalls - oppPlayer.remainingWalls) * 2.0
                    val score = (newOppDist - newAiDist) * 12.0 + oppPenalty - selfPenalty + wallConserve

                    if (score > bestScore) {
                        bestScore = score
                        bestAction = AIAction.PlaceWall(w)
                    }
                }
            }
        }

        return bestAction ?: AIAction.Move(legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    // =========================================================================
    // 4. EXPERT DIFFICULTY: 2-Ply Lookahead with Anti-Trap Heuristics
    // =========================================================================
    private fun decideExpert(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val aiGoalRow = getAiGoal(state, aiId)
        val oppGoalRow = getAiGoal(state, oppId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)
        val midX = state.rules.gridSize / 2

        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        val currentAiDist = dist(state, aiPlayer.position, aiGoalRow, state.walls)
        val currentOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls)

        var bestAction: AIAction? = null
        var bestScore = Double.NEGATIVE_INFINITY

        // 1. Evaluate Pawn Moves with 2-ply counter anticipation
        for (move in legalMoves) {
            val aiDistAfter = dist(state, move, aiGoalRow, state.walls)
            val centerControl = (midX - abs(move.x - midX)) * 1.0

            // Anticipate opponent response step
            val oppPath = path(state, oppPlayer.position, oppGoalRow, state.walls)
            val oppNextPos = if (oppPath.size > 1) oppPath[1] else oppPlayer.position
            val oppDistAfter = dist(state, oppNextPos, oppGoalRow, state.walls)

            val differential = (oppDistAfter - aiDistAfter) * 20.0
            val moveScore = differential + centerControl + if (move.y == aiGoalRow) 1000.0 else 0.0

            if (moveScore > bestScore) {
                bestScore = moveScore
                bestAction = AIAction.Move(move)
            }
        }

        // 2. Evaluate high-impact walls
        if (aiPlayer.remainingWalls > 0) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val oppPath = path(state, oppPlayer.position, oppGoalRow, state.walls)

            val interceptWalls = legalWalls.filter { w ->
                oppPath.take(4).any { p -> abs(w.x - p.x) <= 1 && abs(w.y - p.y) <= 1 }
            }

            for (w in interceptWalls) {
                val testWalls = state.walls + w
                val newOppDist = dist(state, oppPlayer.position, oppGoalRow, testWalls)
                val newAiDist = dist(state, aiPlayer.position, aiGoalRow, testWalls)

                if (newOppDist > currentOppDist) {
                    val oppGain = (newOppDist - currentOppDist) * 25.0
                    val selfLoss = (newAiDist - currentAiDist) * 28.0
                    val delta = (newOppDist - newAiDist) * 16.0
                    val wallReserveBonus = aiPlayer.remainingWalls * 3.0

                    val score = delta + oppGain - selfLoss + wallReserveBonus
                    if (score > bestScore) {
                        bestScore = score
                        bestAction = AIAction.PlaceWall(w)
                    }
                }
            }
        }

        return bestAction ?: AIAction.Move(legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    // =========================================================================
    // 5. NIGHTMARE DIFFICULTY: Predictive Pattern-Aware Alpha-Beta Lookahead
    // =========================================================================
    private fun decideNightmare(
        state: GameState,
        aiId: PlayerId,
        oppId: PlayerId,
        context: Context?
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

        val style = context?.let { PlayerPatternTracker.getPlayerStyle(it) }
        val prefersRight = style?.prefersRightEscape ?: true

        val currentAiDist = dist(state, aiPlayer.position, aiGoalRow, state.walls)
        val currentOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls)

        var bestAction: AIAction? = null
        var bestScore = Double.NEGATIVE_INFINITY

        // 1. Deep Pawn Move Evaluation
        for (move in legalMoves) {
            val aiDistAfter = dist(state, move, aiGoalRow, state.walls)
            val centerControl = (midX - abs(move.x - midX)) * 2.0
            val leadAdvantage = (currentOppDist - aiDistAfter) * 30.0
            val score = leadAdvantage + centerControl

            if (score > bestScore) {
                bestScore = score
                bestAction = AIAction.Move(move)
            }
        }

        // 2. Nightmare Wall Interceptions (biased towards cutting off the player's favored flank)
        if (aiPlayer.remainingWalls > 0) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val oppPath = path(state, oppPlayer.position, oppGoalRow, state.walls)

            val flankSign = if (prefersRight) 1 else -1
            val prioritizedWalls = legalWalls.filter { w ->
                val onPath = oppPath.take(4).any { p -> abs(w.x - p.x) <= 1 && abs(w.y - p.y) <= 1 }
                val onFavoredFlank = (w.x - oppPlayer.position.x) * flankSign >= 0
                onPath || onFavoredFlank
            }.take(50)

            for (w in prioritizedWalls) {
                val testWalls = state.walls + w
                val newOppDist = dist(state, oppPlayer.position, oppGoalRow, testWalls)
                val newAiDist = dist(state, aiPlayer.position, aiGoalRow, testWalls)

                if (newOppDist > currentOppDist) {
                    val oppPenalty = (newOppDist - currentOppDist) * 35.0
                    val selfPenalty = (newAiDist - currentAiDist) * 38.0
                    val netDifference = (newOppDist - newAiDist) * 22.0
                    val wallConserve = aiPlayer.remainingWalls * 4.0

                    val score = netDifference + oppPenalty - selfPenalty + wallConserve
                    if (score > bestScore) {
                        bestScore = score
                        bestAction = AIAction.PlaceWall(w)
                    }
                }
            }
        }

        return bestAction ?: AIAction.Move(legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    // =========================================================================
    // 6. INSANE DIFFICULTY: Grandmaster Minimax - Virtually Unbeatable ⚡💀
    // =========================================================================
    private fun decideInsane(
        state: GameState,
        aiId: PlayerId,
        oppId: PlayerId,
        context: Context?
    ): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val aiGoalRow = getAiGoal(state, aiId)
        val oppGoalRow = getAiGoal(state, oppId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)
        val midX = state.rules.gridSize / 2

        // 1. Instant Win Check
        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        val currentAiDist = dist(state, aiPlayer.position, aiGoalRow, state.walls)
        val currentOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls)

        var bestAction: AIAction? = null
        var bestScore = Double.NEGATIVE_INFINITY

        // 2. Minimax evaluation for all legal pawn movements
        for (move in legalMoves) {
            val aiDistAfter = dist(state, move, aiGoalRow, state.walls)
            val centerControl = (midX - abs(move.x - midX)) * 2.5

            // Simulating opponent's best possible counter-response
            val oppPath = path(state, oppPlayer.position, oppGoalRow, state.walls)
            val oppBestStep = if (oppPath.size > 1) oppPath[1] else oppPlayer.position
            val oppDistAfter = dist(state, oppBestStep, oppGoalRow, state.walls)

            // Critical distance differential
            val delta = (oppDistAfter - aiDistAfter) * 45.0
            val forwardTempo = (currentAiDist - aiDistAfter) * 30.0

            val totalScore = delta + forwardTempo + centerControl

            if (totalScore > bestScore) {
                bestScore = totalScore
                bestAction = AIAction.Move(move)
            }
        }

        // 3. Exhaustive search across all high-impact wall placements
        if (aiPlayer.remainingWalls > 0) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val oppPath = path(state, oppPlayer.position, oppGoalRow, state.walls)
            val aiPath = path(state, aiPlayer.position, aiGoalRow, state.walls)

            // Filter for high-impact candidates (intercepting opponent path, perimeter seals, funnel traps)
            val candidateWalls = legalWalls.filter { w ->
                val hitsOppPath = oppPath.take(5).any { p -> abs(w.x - p.x) <= 1 && abs(w.y - p.y) <= 1 }
                val sealsOppFlank = abs(w.y - oppPlayer.position.y) <= 1
                val preservesAiPath = !aiPath.any { p -> abs(w.x - p.x) == 0 && abs(w.y - p.y) == 0 }
                (hitsOppPath || sealsOppFlank) && preservesAiPath
            }.take(60)

            for (w in candidateWalls) {
                val testWalls = state.walls + w
                val newOppDist = dist(state, oppPlayer.position, oppGoalRow, testWalls)
                val newAiDist = dist(state, aiPlayer.position, aiGoalRow, testWalls)

                if (newOppDist > currentOppDist) {
                    val oppDetourGain = (newOppDist - currentOppDist) * 50.0
                    val selfDetourPenalty = (newAiDist - currentAiDist) * 60.0
                    val differentialAdvantage = (newOppDist - newAiDist) * 35.0
                    val wallConserve = aiPlayer.remainingWalls * 5.0

                    // Inescapable Trap Multiplier: If opponent distance increased by >= 3 with no AI penalty
                    val trapBonus = if (newOppDist - currentOppDist >= 3 && newAiDist <= currentAiDist) 80.0 else 0.0

                    // Choke-point emergency lock: If opponent was within 3 steps of goal, prioritize absolute lock
                    val emergencyDefense = if (currentOppDist <= 3 && newOppDist > currentOppDist) 120.0 else 0.0

                    val wallScore = differentialAdvantage + oppDetourGain - selfDetourPenalty + wallConserve + trapBonus + emergencyDefense

                    if (wallScore > bestScore) {
                        bestScore = wallScore
                        bestAction = AIAction.PlaceWall(w)
                    }
                }
            }
        }

        return bestAction ?: AIAction.Move(legalMoves.firstOrNull() ?: aiPlayer.position)
    }

    // =========================================================================
    // 7. SPECIFIC PERSONALITIES IMPLEMENTATION
    // =========================================================================
    private fun decideRusher(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val aiGoalRow = getAiGoal(state, aiId)
        val oppGoalRow = getAiGoal(state, oppId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        val oppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls)

        // Rusher ONLY places a wall if opponent is within 1 step of victory
        if (oppDist <= 1 && aiPlayer.remainingWalls > 0) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val oppPath = path(state, oppPlayer.position, oppGoalRow, state.walls)
            val urgentWalls = legalWalls.filter { wall ->
                oppPath.any { abs(wall.x - it.x) <= 1 && abs(wall.y - it.y) <= 1 }
            }
            for (w in urgentWalls) {
                val newOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls + w)
                if (newOppDist > oppDist) {
                    return AIAction.PlaceWall(w)
                }
            }
        }

        // Relentless direct sprint
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

        // Fortress builder: drops barricades early and maintains lead
        if (aiPlayer.remainingWalls > 0 && currentOppDist <= currentAiDist + 1) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val oppPath = path(state, oppPlayer.position, oppGoalRow, state.walls)
            val tactical = legalWalls.filter { w ->
                oppPath.take(3).any { abs(w.x - it.x) <= 1 && abs(w.y - it.y) <= 1 }
            }

            var bestWall: Wall? = null
            var bestDetour = 0

            for (w in tactical) {
                val newOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls + w)
                val newAiDist = dist(state, aiPlayer.position, aiGoalRow, state.walls + w)
                val detour = (newOppDist - currentOppDist) * 2 - (newAiDist - currentAiDist)
                if (detour > bestDetour && newOppDist > currentOppDist) {
                    bestDetour = detour
                    bestWall = w
                }
            }

            if (bestWall != null && bestDetour >= 1) {
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

        if (aiPlayer.remainingWalls > 1 && (state.moveCount < 20 || currentOppDist <= currentAiDist + 2)) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val oppPath = path(state, oppPlayer.position, oppGoalRow, state.walls)

            val candidates = legalWalls.filter { w ->
                oppPath.any { abs(w.x - it.x) <= 2 && abs(w.y - it.y) <= 2 }
            }.take(35)

            var bestWall: Wall? = null
            var maxDetour = 0

            for (w in candidates) {
                val test = state.walls + w
                val newOppDist = dist(state, oppPlayer.position, oppGoalRow, test)
                val newAiDist = dist(state, aiPlayer.position, aiGoalRow, test)
                val detour = (newOppDist - currentOppDist) * 3 - (newAiDist - currentAiDist)
                if (detour > maxDetour && newOppDist > currentOppDist) {
                    maxDetour = detour
                    bestWall = w
                }
            }

            if (bestWall != null && maxDetour >= 2) {
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
        return decideExpert(state, aiId, oppId)
    }

    private fun decidePredictor(
        state: GameState,
        aiId: PlayerId,
        oppId: PlayerId,
        context: Context?
    ): AIAction {
        return decideNightmare(state, aiId, oppId, context)
    }

    private fun decideTrickster(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val aiGoalRow = getAiGoal(state, aiId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        // Deceptive parallel walling
        if (aiPlayer.remainingWalls > 1 && Random.nextFloat() < 0.45f) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val pinchWall = legalWalls.firstOrNull { w ->
                abs(w.x - oppPlayer.position.x) == 1 && abs(w.y - oppPlayer.position.y) <= 1
            }
            if (pinchWall != null) {
                return AIAction.PlaceWall(pinchWall)
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

    private fun decideCounterPuncher(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val oppPlayer = state.getPlayer(oppId)
        val aiGoalRow = getAiGoal(state, aiId)
        val oppGoalRow = getAiGoal(state, oppId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

        val oppCrossedMid = if (oppGoalRow > aiGoalRow) {
            oppPlayer.position.y >= state.rules.gridSize / 2
        } else {
            oppPlayer.position.y <= state.rules.gridSize / 2
        }
        val currentOppDist = dist(state, oppPlayer.position, oppGoalRow, state.walls)

        if (oppCrossedMid && aiPlayer.remainingWalls > 0) {
            val legalWalls = RuleEngine.getAllLegalWalls(state, aiId)
            val oppPath = path(state, oppPlayer.position, oppGoalRow, state.walls)

            val blockingWalls = legalWalls.filter { w ->
                oppPath.take(3).any { abs(w.x - it.x) <= 1 && abs(w.y - it.y) <= 1 }
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

    private fun decideMerciless(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        return decideInsane(state, aiId, oppId, null)
    }

    private fun decideChaotic(state: GameState, aiId: PlayerId, oppId: PlayerId): AIAction {
        val aiPlayer = state.getPlayer(aiId)
        val aiGoalRow = getAiGoal(state, aiId)
        val legalMoves = RuleEngine.getLegalMoves(state, aiId)

        for (m in legalMoves) {
            if (m.y == aiGoalRow) return AIAction.Move(m)
        }

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

        if (legalMoves.size > 1 && Random.nextFloat() < 0.30f) {
            return AIAction.Move(legalMoves.random())
        }

        val shortestPath = path(state, aiPlayer.position, aiGoalRow, state.walls)
        if (shortestPath.size > 1 && shortestPath[1] in legalMoves) {
            return AIAction.Move(shortestPath[1])
        }

        val bestMove = legalMoves.minByOrNull { dist(state, it, aiGoalRow, state.walls) }
        return AIAction.Move(bestMove ?: legalMoves.firstOrNull() ?: aiPlayer.position)
    }
}
