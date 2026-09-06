package com.example.wallrush.domain.engine

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

    fun getGoalRow(playerId: PlayerId, mode: com.example.wallrush.domain.model.GameMode = com.example.wallrush.domain.model.GameMode.VS_AI): Int =
        if (mode == com.example.wallrush.domain.model.GameMode.RACE_MODE) RACE_GOAL_ROW
        else if (playerId == PlayerId.PLAYER_1) PLAYER_1_GOAL_ROW else PLAYER_2_GOAL_ROW

    /**
     * Calculates all legal pawn moves for the specified player in the current game state.
     * Accurately implements standard Quoridor straight jumps and diagonal side jumps.
     */
    fun getLegalMoves(state: GameState, playerId: PlayerId = state.currentTurn): List<Position> {
        val player = state.getPlayer(playerId)
        val currentPos = player.position
        val walls = state.walls

        val otherPlayers = when {
            state.rules.mode == com.example.wallrush.domain.model.GameMode.QUAD_MODE ->
                listOfNotNull(state.player1, state.player2, state.player3, state.player4).filter { it.id != playerId }
            else ->
                listOf(state.getPlayer(if (playerId == PlayerId.PLAYER_1) PlayerId.PLAYER_2 else PlayerId.PLAYER_1))
        }
        val occupiedPositions = otherPlayers.map { it.position }.toSet()

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

            if (targetX !in 0..8 || targetY !in 0..8) continue

            val targetPos = Position(targetX, targetY)

            // Check if passage to adjacent cell is blocked by wall
            if (PathFinder.isPassageBlocked(currentPos, targetPos, walls)) {
                continue
            }

            // Case A: Adjacent cell is NOT occupied by any opponent -> Normal single step
            if (targetPos !in occupiedPositions) {
                legalMoves.add(targetPos)
                continue
            }

            // Case B: Adjacent cell IS occupied by an opponent -> Jump mechanics!
            val jumpStraightX = targetPos.x + dx
            val jumpStraightY = targetPos.y + dy
            val straightJumpPos = Position(jumpStraightX, jumpStraightY)

            val canJumpStraight = straightJumpPos.isWithinBounds() &&
                    straightJumpPos !in occupiedPositions &&
                    !PathFinder.isPassageBlocked(targetPos, straightJumpPos, walls)

            if (canJumpStraight) {
                // Straight jump over opponent
                legalMoves.add(straightJumpPos)
            } else {
                // Straight jump is blocked by a wall, board boundary, or another pawn -> diagonal side-steps allowed
                val perpendicularDirs = if (dx == 0) {
                    // Was moving vertically, check left and right
                    listOf(Pair(-1, 0), Pair(1, 0))
                } else {
                    // Was moving horizontally, check up and down
                    listOf(Pair(0, -1), Pair(0, 1))
                }

                for ((pdx, pdy) in perpendicularDirs) {
                    val sideX = targetPos.x + pdx
                    val sideY = targetPos.y + pdy
                    val sidePos = Position(sideX, sideY)

                    if (sidePos.isWithinBounds() &&
                        sidePos != currentPos &&
                        sidePos !in occupiedPositions &&
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
     * Checks boundaries, overlap, intersection, remaining inventory, and ensures both players still have a valid path to goal.
     */
    fun isWallPlacementLegal(state: GameState, proposedWall: Wall, playerId: PlayerId = state.currentTurn): Boolean {
        val player = state.getPlayer(playerId)
        if (player.remainingWalls <= 0) return false

        // Check slot boundaries: x in 0..7, y in 0..7
        if (proposedWall.x !in 0..7 || proposedWall.y !in 0..7) return false

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

        // 4. Path availability check: BFS for players
        val testWalls = currentWalls + proposedWall

        if (state.rules.mode == com.example.wallrush.domain.model.GameMode.QUAD_MODE) {
            val center = QUAD_GOAL_CELL
            val p1Ok = PathFinder.hasPathToCell(state.player1.position, center, testWalls)
            val p2Ok = PathFinder.hasPathToCell(state.player2.position, center, testWalls)
            val p3Ok = state.player3?.let { PathFinder.hasPathToCell(it.position, center, testWalls) } ?: true
            val p4Ok = state.player4?.let { PathFinder.hasPathToCell(it.position, center, testWalls) } ?: true
            if (!p1Ok || !p2Ok || !p3Ok || !p4Ok) return false
        } else if (state.rules.mode == com.example.wallrush.domain.model.GameMode.RACE_MODE) {
            val p1Ok = PathFinder.hasPathToGoal(state.player1.position, RACE_GOAL_ROW, testWalls)
            val p2Ok = PathFinder.hasPathToGoal(state.player2.position, RACE_GOAL_ROW, testWalls)
            if (!p1Ok || !p2Ok) return false
        } else {
            val p1HasPath = PathFinder.hasPathToGoal(
                start = state.player1.position,
                targetGoalRow = PLAYER_1_GOAL_ROW,
                walls = testWalls
            )
            if (!p1HasPath) return false

            val p2HasPath = PathFinder.hasPathToGoal(
                start = state.player2.position,
                targetGoalRow = PLAYER_2_GOAL_ROW,
                walls = testWalls
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

        val legalWalls = mutableListOf<Wall>()

        for (x in 0..7) {
            for (y in 0..7) {
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
     */
    fun checkWinner(state: GameState): PlayerId? {
        if (state.rules.mode == com.example.wallrush.domain.model.GameMode.QUAD_MODE) {
            val center = QUAD_GOAL_CELL
            if (state.player1.position == center) return PlayerId.PLAYER_1
            if (state.player2.position == center) return PlayerId.PLAYER_2
            state.player3?.let { if (it.position == center) return PlayerId.PLAYER_3 }
            state.player4?.let { if (it.position == center) return PlayerId.PLAYER_4 }
            return null
        } else if (state.rules.mode == com.example.wallrush.domain.model.GameMode.RACE_MODE) {
            if (state.player1.position.y == RACE_GOAL_ROW) return PlayerId.PLAYER_1
            if (state.player2.position.y == RACE_GOAL_ROW) return PlayerId.PLAYER_2
            return null
        } else {
            if (state.player1.position.y == PLAYER_1_GOAL_ROW) return PlayerId.PLAYER_1
            if (state.player2.position.y == PLAYER_2_GOAL_ROW) return PlayerId.PLAYER_2
            return null
        }
    }
}
