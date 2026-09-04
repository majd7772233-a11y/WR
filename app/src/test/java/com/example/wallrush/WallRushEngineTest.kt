package com.example.wallrush

import com.example.wallrush.domain.engine.GameEngine
import com.example.wallrush.domain.engine.PathFinder
import com.example.wallrush.domain.engine.ReplayEngine
import com.example.wallrush.domain.engine.RuleEngine
import com.example.wallrush.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class WallRushEngineTest {

    @Test
    fun testPawnMovesOneSquare() {
        val rules = GameRules()
        val initial = GameEngine.createInitialState(rules).copy(status = GameStatus.IN_PROGRESS)

        val legalMoves = RuleEngine.getLegalMoves(initial, PlayerId.PLAYER_1)
        // Player 1 at (4,8) can move to (3,8), (5,8), (4,7)
        assertTrue(Position(4, 7) in legalMoves)
        assertTrue(Position(3, 8) in legalMoves)
        assertTrue(Position(5, 8) in legalMoves)
        assertEquals(3, legalMoves.size)

        val nextState = GameEngine.makeMove(initial, Position(4, 7), PlayerId.PLAYER_1)
        assertEquals(Position(4, 7), nextState.player1.position)
        assertEquals(PlayerId.PLAYER_2, nextState.currentTurn)
    }

    @Test
    fun testPawnCannotCrossWall() {
        val rules = GameRules()
        var state = GameEngine.createInitialState(rules).copy(status = GameStatus.IN_PROGRESS)

        // Place horizontal wall at (4, 7), which blocks movement between (4,7) & (4,8) and (5,7) & (5,8)
        val wall = Wall(x = 4, y = 7, orientation = WallOrientation.HORIZONTAL, placedBy = PlayerId.PLAYER_1)
        state = GameEngine.placeWall(state, wall, PlayerId.PLAYER_1)

        // Player 1 is at (4,8). Moving to (4,7) must now be blocked
        val p1LegalMoves = RuleEngine.getLegalMoves(state, PlayerId.PLAYER_1)
        assertFalse(Position(4, 7) in p1LegalMoves)
        assertTrue(Position(3, 8) in p1LegalMoves)
        assertTrue(Position(5, 8) in p1LegalMoves)
    }

    @Test
    fun testStraightJumpOverOpponent() {
        val rules = GameRules()
        val initial = GameEngine.createInitialState(rules).copy(
            status = GameStatus.IN_PROGRESS,
            player1 = PlayerState(PlayerId.PLAYER_1, "P1", 0, Position(4, 4), remainingWalls = 10),
            player2 = PlayerState(PlayerId.PLAYER_2, "P2", 1, Position(4, 3), remainingWalls = 10),
            currentTurn = PlayerId.PLAYER_1
        )

        val legalMoves = RuleEngine.getLegalMoves(initial, PlayerId.PLAYER_1)
        // P1 at (4,4), P2 at (4,3). Straight jump should land at (4,2)
        assertTrue(Position(4, 2) in legalMoves)
        assertFalse(Position(4, 3) in legalMoves) // Cannot land directly on opponent's occupied square
    }

    @Test
    fun testDiagonalJumpWhenStraightBlockedByWall() {
        val rules = GameRules()
        val wallBehindP2 = Wall(x = 4, y = 2, orientation = WallOrientation.HORIZONTAL, placedBy = PlayerId.PLAYER_1)
        val initial = GameEngine.createInitialState(rules).copy(
            status = GameStatus.IN_PROGRESS,
            player1 = PlayerState(PlayerId.PLAYER_1, "P1", 0, Position(4, 4), remainingWalls = 10),
            player2 = PlayerState(PlayerId.PLAYER_2, "P2", 1, Position(4, 3), remainingWalls = 10),
            walls = listOf(wallBehindP2),
            currentTurn = PlayerId.PLAYER_1
        )

        val legalMoves = RuleEngine.getLegalMoves(initial, PlayerId.PLAYER_1)
        // Straight jump to (4,2) is blocked by wall at y=2. Diagonal jumps to (3,3) and (5,3) must be allowed
        assertFalse(Position(4, 2) in legalMoves)
        assertTrue(Position(3, 3) in legalMoves)
        assertTrue(Position(5, 3) in legalMoves)
    }

    @Test
    fun testWallCannotBlockAllPaths() {
        val rules = GameRules()
        // Surround player 2 at (4,0) completely
        // Left & Right: x=3, x=4 vertical walls. Bottom: y=0 horizontal wall.
        val wall1 = Wall(x = 3, y = 0, orientation = WallOrientation.VERTICAL, placedBy = PlayerId.PLAYER_1)
        val wall2 = Wall(x = 4, y = 0, orientation = WallOrientation.VERTICAL, placedBy = PlayerId.PLAYER_1)
        val stateWith2Walls = GameEngine.createInitialState(rules).copy(
            status = GameStatus.IN_PROGRESS,
            walls = listOf(wall1, wall2)
        )

        // Trapping horizontal wall across (3,0) and (4,0)
        val trappingWall = Wall(x = 3, y = 0, orientation = WallOrientation.HORIZONTAL, placedBy = PlayerId.PLAYER_1)
        // This wall would trap Player 2 with no exit to row 8
        assertFalse(RuleEngine.isWallPlacementLegal(stateWith2Walls, trappingWall, PlayerId.PLAYER_1))
    }

    @Test
    fun testWinCondition() {
        val rules = GameRules()
        val nearWinning = GameEngine.createInitialState(rules).copy(
            status = GameStatus.IN_PROGRESS,
            player1 = PlayerState(PlayerId.PLAYER_1, "P1", 0, Position(4, 1)),
            player2 = PlayerState(PlayerId.PLAYER_2, "P2", 1, Position(2, 5)),
            currentTurn = PlayerId.PLAYER_1
        )

        val wonState = GameEngine.makeMove(nearWinning, Position(4, 0), PlayerId.PLAYER_1)
        assertEquals(GameStatus.FINISHED, wonState.status)
        assertEquals(PlayerId.PLAYER_1, wonState.winner)
        assertEquals(FinishReason.GOAL_REACHED, wonState.finishReason)
    }

    @Test
    fun testReplayDeterminism() {
        val rules = GameRules()
        var state = GameEngine.createInitialState(rules).copy(status = GameStatus.IN_PROGRESS)

        state = GameEngine.makeMove(state, Position(4, 7), PlayerId.PLAYER_1)
        state = GameEngine.makeMove(state, Position(4, 1), PlayerId.PLAYER_2)
        val wall = Wall(x = 2, y = 2, orientation = WallOrientation.HORIZONTAL, placedBy = PlayerId.PLAYER_1)
        state = GameEngine.placeWall(state, wall, PlayerId.PLAYER_1)

        val replayEngine = ReplayEngine(
            initialRules = rules,
            player1Name = "Player 1",
            player2Name = "Player 2",
            player1Avatar = 0,
            player2Avatar = 1,
            events = state.eventHistory
        )

        assertEquals(3, replayEngine.totalSteps)
        val finalReplayState = replayEngine.getStateAtStep(3)
        assertEquals(state.player1.position, finalReplayState.player1.position)
        assertEquals(state.player2.position, finalReplayState.player2.position)
        assertEquals(1, finalReplayState.walls.size)
    }
}
