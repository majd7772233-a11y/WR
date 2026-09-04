package com.example.wallrush.domain.engine

import com.example.wallrush.domain.model.PlayerId
import com.example.wallrush.domain.model.Position
import com.example.wallrush.domain.model.Wall
import com.example.wallrush.domain.model.WallOrientation
import java.util.ArrayDeque
import kotlin.math.abs

object PathFinder {

    /**
     * Checks if moving directly between two adjacent cells (from -> to) is blocked by any wall.
     */
    fun isPassageBlocked(from: Position, to: Position, walls: List<Wall>): Boolean {
        val dx = to.x - from.x
        val dy = to.y - from.y

        // Moving Vertically: between row y and row y+1
        if (dx == 0 && abs(dy) == 1) {
            val minRow = minOf(from.y, to.y)
            val col = from.x
            // Blocked if a horizontal wall exists at (col, minRow) or (col - 1, minRow)
            return walls.any { wall ->
                wall.orientation == WallOrientation.HORIZONTAL &&
                        wall.y == minRow &&
                        (wall.x == col || wall.x == col - 1)
            }
        }

        // Moving Horizontally: between col x and col x+1
        if (dy == 0 && abs(dx) == 1) {
            val minCol = minOf(from.x, to.x)
            val row = from.y
            // Blocked if a vertical wall exists at (minCol, row) or (minCol, row - 1)
            return walls.any { wall ->
                wall.orientation == WallOrientation.VERTICAL &&
                        wall.x == minCol &&
                        (wall.y == row || wall.y == row - 1)
            }
        }

        return true // Non-adjacent cells are not directly passable
    }

    /**
     * Checks if a path exists from start position to the target goal row.
     * Player 1 target: row 0 (y == 0)
     * Player 2 target: row 8 (y == 8)
     */
    fun hasPathToGoal(
        start: Position,
        targetGoalRow: Int,
        walls: List<Wall>
    ): Boolean {
        if (start.y == targetGoalRow) return true

        val visited = Array(9) { BooleanArray(9) }
        val queue = ArrayDeque<Position>()

        queue.add(start)
        visited[start.y][start.x] = true

        val neighbors = arrayOf(
            Pair(0, -1), // Up
            Pair(0, 1),  // Down
            Pair(-1, 0), // Left
            Pair(1, 0)   // Right
        )

        while (queue.isNotEmpty()) {
            val current = queue.poll() ?: continue

            if (current.y == targetGoalRow) {
                return true
            }

            for ((dx, dy) in neighbors) {
                val nx = current.x + dx
                val ny = current.y + dy

                if (nx in 0..8 && ny in 0..8 && !visited[ny][nx]) {
                    val next = Position(nx, ny)
                    if (!isPassageBlocked(current, next, walls)) {
                        visited[ny][nx] = true
                        queue.add(next)
                    }
                }
            }
        }

        return false
    }

    /**
     * Calculates shortest distance from start position to goal row using BFS.
     * Returns Int.MAX_VALUE / 2 if unreachable.
     */
    fun shortestDistanceToGoal(
        start: Position,
        targetGoalRow: Int,
        walls: List<Wall>
    ): Int {
        if (start.y == targetGoalRow) return 0

        val dist = Array(9) { IntArray(9) { -1 } }
        val queue = ArrayDeque<Position>()

        queue.add(start)
        dist[start.y][start.x] = 0

        val neighbors = arrayOf(
            Pair(0, -1),
            Pair(0, 1),
            Pair(-1, 0),
            Pair(1, 0)
        )

        var minDistance = Int.MAX_VALUE / 2

        while (queue.isNotEmpty()) {
            val current = queue.poll() ?: continue
            val currentDist = dist[current.y][current.x]

            if (current.y == targetGoalRow) {
                minDistance = minOf(minDistance, currentDist)
                continue
            }

            for ((dx, dy) in neighbors) {
                val nx = current.x + dx
                val ny = current.y + dy

                if (nx in 0..8 && ny in 0..8 && dist[ny][nx] == -1) {
                    val next = Position(nx, ny)
                    if (!isPassageBlocked(current, next, walls)) {
                        dist[ny][nx] = currentDist + 1
                        queue.add(next)
                    }
                }
            }
        }

        return minDistance
    }

    /**
     * Reconstructs the shortest path as a list of positions from start to goal.
     */
    fun findShortestPath(
        start: Position,
        targetGoalRow: Int,
        walls: List<Wall>
    ): List<Position> {
        if (start.y == targetGoalRow) return listOf(start)

        val parent = mutableMapOf<Position, Position>()
        val visited = Array(9) { BooleanArray(9) }
        val queue = ArrayDeque<Position>()

        queue.add(start)
        visited[start.y][start.x] = true

        var goalReached: Position? = null

        val neighbors = arrayOf(
            Pair(0, -1),
            Pair(0, 1),
            Pair(-1, 0),
            Pair(1, 0)
        )

        while (queue.isNotEmpty()) {
            val current = queue.poll() ?: continue

            if (current.y == targetGoalRow) {
                goalReached = current
                break
            }

            for ((dx, dy) in neighbors) {
                val nx = current.x + dx
                val ny = current.y + dy

                if (nx in 0..8 && ny in 0..8 && !visited[ny][nx]) {
                    val next = Position(nx, ny)
                    if (!isPassageBlocked(current, next, walls)) {
                        visited[ny][nx] = true
                        parent[next] = current
                        queue.add(next)
                    }
                }
            }
        }

        if (goalReached == null) return emptyList()

        val path = mutableListOf<Position>()
        var curr: Position? = goalReached
        while (curr != null) {
            path.add(0, curr)
            curr = parent[curr]
        }
        return path
    }
}
