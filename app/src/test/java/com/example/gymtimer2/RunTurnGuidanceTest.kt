package com.example.gymtimer2

import com.example.gymtimer2.domain.model.PlannedRoutePoint
import com.example.gymtimer2.domain.model.RunTurnDirection
import com.example.gymtimer2.domain.model.RunTurnGuidance
import com.example.gymtimer2.domain.model.RunTurnInstruction
import com.example.gymtimer2.domain.model.inferRunTurnInstructions
import com.example.gymtimer2.domain.model.nextRunTurnGuidance
import com.example.gymtimer2.domain.model.upcomingRunTurnGuidances
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunTurnGuidanceTest {
    @Test
    fun infersLeftAndRightTurnsFromRouteGeometry() {
        assertEquals(RunTurnDirection.RIGHT, inferRunTurnInstructions(eastThenSouth()).single().direction)
        assertEquals(RunTurnDirection.LEFT, inferRunTurnInstructions(eastThenNorth()).single().direction)
        assertEquals(RunTurnDirection.U_TURN, inferRunTurnInstructions(eastThenWest()).single().direction)
    }

    @Test
    fun ignoresStraightRoutesAndDoesNotJoinSeparateSegments() {
        val straight = listOf(point(0.0, 0.0), point(0.0, 0.001), point(0.0, 0.002))
        val separateSegments = listOf(
            point(0.0, 0.0, 0),
            point(0.0, 0.001, 0),
            point(0.0, 0.0, 1),
            point(0.001, 0.0, 1)
        )

        assertEquals(emptyList<RunTurnInstruction>(), inferRunTurnInstructions(straight))
        assertEquals(emptyList<RunTurnInstruction>(), inferRunTurnInstructions(separateSegments))
    }

    @Test
    fun returnsTheNextTurnAndKeepsItVisibleJustAfterTheTurn() {
        val turns = listOf(
            RunTurnInstruction(RunTurnDirection.LEFT, 100.0),
            RunTurnInstruction(RunTurnDirection.RIGHT, 300.0)
        )

        assertEquals(70.0, nextRunTurnGuidance(turns, 30.0)?.distanceToTurnMeters ?: -1.0, 0.0)
        assertEquals(0.0, nextRunTurnGuidance(turns, 110.0)?.distanceToTurnMeters ?: -1.0, 0.0)
        assertEquals(RunTurnDirection.RIGHT, nextRunTurnGuidance(turns, 130.0)?.direction)
        assertNull(nextRunTurnGuidance(turns, Double.NaN))
    }

    @Test
    fun returnsTheRequestedNumberOfUpcomingTurnsInRouteOrder() {
        val turns = listOf(
            RunTurnInstruction(RunTurnDirection.LEFT, 100.0),
            RunTurnInstruction(RunTurnDirection.RIGHT, 300.0),
            RunTurnInstruction(RunTurnDirection.U_TURN, 500.0)
        )

        assertEquals(
            listOf(RunTurnDirection.RIGHT, RunTurnDirection.U_TURN),
            upcomingRunTurnGuidances(turns, 121.0, 2).map { it.direction }
        )
        assertEquals(2, upcomingRunTurnGuidances(turns, 121.0, Int.MAX_VALUE).size)
        assertEquals(emptyList<RunTurnGuidance>(), upcomingRunTurnGuidances(turns, 121.0, 0))
    }

    private fun eastThenSouth(): List<PlannedRoutePoint> = buildList {
        add(point(0.0, 0.0))
        repeat(10) { add(point(0.0, (it + 1) * 0.0001)) }
        repeat(10) { add(point(-(it + 1) * 0.0001, 0.001)) }
    }

    private fun eastThenNorth(): List<PlannedRoutePoint> = buildList {
        add(point(0.0, 0.0))
        repeat(10) { add(point(0.0, (it + 1) * 0.0001)) }
        repeat(10) { add(point((it + 1) * 0.0001, 0.001)) }
    }

    private fun eastThenWest(): List<PlannedRoutePoint> = buildList {
        add(point(0.0, 0.0))
        repeat(10) { add(point(0.0, (it + 1) * 0.0001)) }
        repeat(10) { add(point(0.0, (10 - it) * 0.0001)) }
    }

    private fun point(latitude: Double, longitude: Double, segment: Int = 0) =
        PlannedRoutePoint(latitude, longitude, segment)
}
