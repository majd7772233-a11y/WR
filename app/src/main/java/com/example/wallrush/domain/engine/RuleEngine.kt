package com.example.wallrush.domain.engine

import com.example.wallrush.domain.model.GameMode
import com.example.wallrush.domain.model.GameRules
import com.example.wallrush.domain.model.GameState
import com.example.wallrush.domain.model.PlayerId
import com.example.wallrush.domain.model.Position
import com.example.wallrush.domain.model.Wall
import com.example.wallrush.domain.model.WallOrientation
import kotlin.math.abs

object RuleEngine {

    val PLAYER_1_GOAL_ROW = 0
    val PLAYER_2_GOAL_ROW = 8
    val RACE_GOAL_ROW = 0
    val QUAD_GOAL_CELL = Position(4, 4)

    /**
     * Determines the winning goal row for a player dynamically based on grid size and starting side.
     */
    fun getGoalRow(playerId: PlayerId, rules: GameRules): Int {
        if (rules.mode == GameMode.RACE_MODE) return RACE_GOAL_ROW
        val gridSize = rules.gridSize.coerceAtLeast(3)
        return if (playerId == PlayerId.PLAYER_1) 0 else gridSize - 1
    }

    fun getGoalRow(playerId: PlayerId, mode: GameMode = GameMode.VS_AI): Int {
        if (mode == GameMode.RACE_MODE) return RACE_GOAL_ROW
        return if (playerId == PlayerId.PLAYER_1) PLAYER_1_GOAL_ROW else PLAYER_2_GOAL_ROW
    }

    fun getPlayerGoalRow(state: GameState, playerId: PlayerId): Int {
        if (state.rules.mode == GameMode.RACE_MODE) return RACE_GOAL_ROW
        val player = state.getPlayer(playerId)
        return player.targetGoalRow ?: getGoalRow(playerId, state.rules)
    }

    /**
     * Calculates all legal pawn moves for the specified player in the current game state.
     * Accurately implements standard Quoridor straight jumps, diagonal side jumps, and obstacle blocking.
     */
    fun getLegalMoves(state: GameState, playerId: PlayerId = state.currentTurn): List<Position> {
        val player = state.getPlayer(playerId)
        val currentPos = player.position
        val walls = state.walls
        val gridSize = state.rules.gridSize.coerceAtLeast(3)

        val otherPlayers = when {
            state.rules.mode == GameMode.QUAD_MODE ->
                listOfNotNull(state.player1, state.player2, state.player3, state.player4).filter { it.id != playerId }
            else ->
                listOf(state.getPlayer(if (playerId == PlayerId.PLAYER_1) PlayerId.PLAYER_2 else PlayerId.PLAYER_1))
        }
        val occupiedPositions = otherPlayers.map { it.position }.toSet()
        val obstaclePositions = state.obstacles.map { Position(it.x, it.y) }.toSet()
        val impassablePositions = occupiedPositions + obstaclePositions

        val legalMoves = mutableListOf<Position>()

        // 4 orthogonal directions: Up, Down, Left, Right
        val directions = listOf(
            Pair(0, -1), // Up
            Pair(0, 1),  // Down
            Pair(-1, 0), // Left
            Pair(1, 0)   // Right
        )

        for ((dx, dy) in directions) {
            val targetX = currentPos.x + dx
            val targetY = currentPos.y + dy

            if (targetX !in 0 until gridSize || targetY !in 0 until gridSize) continue

            val targetPos = Position(targetX, targetY)

            // Blocked if an obstacle is placed here
            if (targetPos in obstaclePositions) continue

            // Check if passage to adjacent cell is blocked by wall
            if (PathFinder.isPassageBlocked(currentPos, targetPos, walls)) {
                continue
            }

            // Case A: Adjacent cell is NOT occupied by any player -> Normal single step
            if (targetPos !in occupiedPositions) {
                legalMoves.add(targetPos)
                continue
            }

            // Case B: Adjacent cell IS occupied by an opponent -> Jump mechanics!
            val jumpStraightX = targetPos.x + dx
            val jumpStraightY = targetPos.y + dy
            val straightJumpPos = Position(jumpStraightX, jumpStraightY)

            val canJumpStraight = straightJumpPos.isWithinBounds(gridSize) &&
                    straightJumpPos !in impassablePositions &&
                    !PathFinder.isPassageBlocked(targetPos, straightJumpPos, walls)

            if (canJumpStraight) {
                // Straight jump over opponent
                legalMoves.add(straightJumpPos)
            } else {
                // Straight jump is blocked by a wall, board boundary, or another entity -> diagonal side-steps allowed
                val perpendicularDirs = if (dx == 0) {
                    listOf(Pair(-1, 0), Pair(1, 0))
                } else {
                    listOf(Pair(0, -1), Pair(0, 1))
                }

                for ((pdx, pdy) in perpendicularDirs) {
                    val sideX = targetPos.x + pdx
                    val sideY = targetPos.y + pdy
                    val sidePos = Position(sideX, sideY)

                    if (sidePos.isWithinBounds(gridSize) &&
                        sidePos != currentPos &&
                        sidePos !in impassablePositions &&
                        !PathFinder.isPassageBlocked(targetPos, sidePos, walls)
                    ) {
                        legalMoves.add(sidePos)
                    }
                }
            }
        }

        return legalMoves.distinct()
    }

    /**
     * Verifies if a proposed wall placement is completely legal.
     * Checks boundaries, overlap, intersection, remaining inventory, and ensures players still have a valid path to goal.
     */
    fun isWallPlacementLegal(state: GameState, proposedWall: Wall, playerId: PlayerId = state.currentTurn): Boolean {
        val player = state.getPlayer(playerId)
        if (player.remainingWalls <= 0) return false

        val gridSize = state.rules.gridSize.coerceAtLeast(3)
        val maxWallCoord = gridSize - 1

        // Check slot boundaries
        if (proposedWall.x !in 0 until maxWallCoord || proposedWall.y !in 0 until maxWallCoord) return false

        val currentWalls = state.walls

        // Check conflict with existing walls
        for (wall in currentWalls) {
            // 1. Two walls cannot cross at the same intersection peg (x, y)
            if (wall.x == proposedWall.x && wall.y == proposedWall.y) {
                return false
            }

            // 2. Horizontal walls cannot overlap (share segments)
            if (wall.orientation == WallOrientation.HORIZONTAL &&
                proposedWall.orientation == WallOrientation.HORIZONTAL
            ) {
                if (wall.y == proposedWall.y && abs(wall.x - proposedWall.x) < 2) {
                    return false
                }
            }

            // 3. Vertical walls cannot overlap (share segments)
            if (wall.orientation == WallOrientation.VERTICAL &&
                proposedWall.orientation == WallOrientation.VERTICAL
            ) {
                if (wall.x == proposedWall.x && abs(wall.y - proposedWall.y) < 2) {
                    return false
                }
            }
        }

        // 4. Path availability check: BFS for all players
        val testWalls = currentWalls + proposedWall

        if (state.rules.mode == GameMode.QUAD_MODE) {
            val center = Position(gridSize / 2, gridSize / 2)
            val p1Ok = PathFinder.hasPathToCell(state.player1.position, center, testWalls, gridSize, state.obstacles)
            val p2Ok = PathFinder.hasPathToCell(state.player2.position, center, testWalls, gridSize, state.obstacles)
            val p3Ok = state.player3?.let { PathFinder.hasPathToCell(it.position, center, testWalls, gridSize, state.obstacles) } ?: true
            val p4Ok = state.player4?.let { PathFinder.hasPathToCell(it.position, center, testWalls, gridSize, state.obstacles) } ?: true
            if (!p1Ok || !p2Ok || !p3Ok || !p4Ok) return false
        } else if (state.rules.mode == GameMode.RACE_MODE) {
            val p1Ok = PathFinder.hasPathToGoal(state.player1.position, RACE_GOAL_ROW, testWalls, gridSize, state.obstacles)
            val p2Ok = PathFinder.hasPathToGoal(state.player2.position, RACE_GOAL_ROW, testWalls, gridSize, state.obstacles)
            if (!p1Ok || !p2Ok) return false
        } else {
            val p1Goal = getPlayerGoalRow(state, PlayerId.PLAYER_1)
            val p2Goal = getPlayerGoalRow(state, PlayerId.PLAYER_2)

            val p1HasPath = PathFinder.hasPathToGoal(
                start = state.player1.position,
                targetGoalRow = p1Goal,
                walls = testWalls,
                gridSize = gridSize,
                obstacles = state.obstacles
            )
            if (!p1HasPath) return false

            val p2HasPath = PathFinder.hasPathToGoal(
                start = state.player2.position,
                targetGoalRow = p2Goal,
                walls = testWalls,
                gridSize = gridSize,
                obstacles = state.obstacles
            )
            if (!p2HasPath) return false
        }

        return true
    }

    /**
     * Returns all currently legal wall placements for the given player.
     */
    fun getAllLegalWalls(state: GameState, playerId: PlayerId = state.currentTurn): List<Wall> {
        val player = state.getPlayer(playerId)
        if (player.remainingWalls <= 0) return emptyList()

        val gridSize = state.rules.gridSize.coerceAtLeast(3)
        val maxWallCoord = gridSize - 1
        val legalWalls = mutableListOf<Wall>()

        for (x in 0 until maxWallCoord) {
            for (y in 0 until maxWallCoord) {
                val horizontalWall = Wall(
                    x = x,
                    y = y,
                    orientation = WallOrientation.HORIZONTAL,
                    placedBy = playerId
                )
                if (isWallPlacementLegal(state, horizontalWall, playerId)) {
                    legalWalls.add(horizontalWall)
                }

                val verticalWall = Wall(
                    x = x,
                    y = y,
                    orientation = WallOrientation.VERTICAL,
                    placedBy = playerId
                )
                if (isWallPlacementLegal(state, verticalWall, playerId)) {
                    legalWalls.add(verticalWall)
                }
            }
        }

        return legalWalls
    }

    /**
     * Checks if a player has reached their winning goal row or center cell.
     * Prevents false triggers before any move has been played.
     */
    fun checkWinner(state: GameState): PlayerId? {
        // A game cannot be won with 0 moves!
        if (state.moveCount <= 0) return null

        val gridSize = state.rules.gridSize.coerceAtLeast(3)

        if (state.rules.mode == GameMode.QUAD_MODE) {
            val center = Position(gridSize / 2, gridSize / 2)
            if (state.player1.position == center) return PlayerId.PLAYER_1
            if (state.player2.position == center) return PlayerId.PLAYER_2
            state.player3?.let { if (it.position == center) return PlayerId.PLAYER_3 }
            state.player4?.let { if (it.position == center) return PlayerId.PLAYER_4 }
            return null
        } else if (state.rules.mode == GameMode.RACE_MODE) {
            if (state.player1.position.y == RACE_GOAL_ROW) return PlayerId.PLAYER_1
            if (state.player2.position.y == RACE_GOAL_ROW) return PlayerId.PLAYER_2
            return null
        } else {
            val p1Goal = getPlayerGoalRow(state, PlayerId.PLAYER_1)
            val p2Goal = getPlayerGoalRow(state, PlayerId.PLAYER_2)

            if (state.player1.position.y == p1Goal) return PlayerId.PLAYER_1
            if (state.player2.position.y == p2Goal) return PlayerId.PLAYER_2
            return null
        }
    }
}
