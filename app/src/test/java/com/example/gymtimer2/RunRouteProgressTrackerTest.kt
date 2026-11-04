package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunRouteProgressTracker
import com.example.gymtimer2.domain.model.RunStreetInstruction
import com.example.gymtimer2.domain.model.nextRunStreetGuidance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunRouteProgressTrackerTest {
    @Test
    fun repeatedCoordinatesKeepTheLaterRouteProgressUntilTheRouteChanges() {
        val route = listOf(
            point(1L, 0.0, 0.0),
            point(2L, 0.0, 0.001),
            point(3L, 0.001, 0.001),
            point(4L, 0.001, 0.0),
            point(5L, 0.0, 0.0)
        )
        val tracker = RunRouteProgressTracker()
        val firstPass = tracker.update(1L, "gpx:1", point(10L, 0.0, 0.0005), route)
        val laterPass = tracker.update(1L, "gpx:1", point(11L, 0.001, 0.0), route)
        val repeatedStart = tracker.update(1L, "gpx:1", point(12L, 0.0, 0.0), route)
        val newRouteStart = tracker.update(1L, "gpx:2", point(13L, 0.0, 0.0), route)

        assertNotNull(firstPass)
        assertNotNull(laterPass)
        assertNotNull(repeatedStart)
        assertNotNull(newRouteStart)
        assertTrue(laterPass!!.progressMeters > firstPass!!.progressMeters)
        assertTrue(repeatedStart!!.progressMeters > laterPass.progressMeters)
        assertTrue(newRouteStart!!.progressMeters < 100.0)
    }

    @Test
    fun restoresProgressFromSavedPointsAfterTrackerRecreation() {
        val route = listOf(
            point(1L, 0.0, 0.0),
            point(2L, 0.0, 0.001),
            point(3L, 0.001, 0.001),
            point(4L, 0.001, 0.0),
            point(5L, 0.0, 0.0)
        )
        val earlierLoopPoints = listOf(
            point(10L, 0.0, 0.0005),
            point(11L, 0.001, 0.0)
        )
        val tracker = RunRouteProgressTracker()

        val restoredPosition = tracker.update(
            activeRunId = 1L,
            selectedRouteKey = "gpx:1",
            point = point(12L, 0.0, 0.0),
            route = route,
            recordedPoints = earlierLoopPoints
        )

        assertTrue(restoredPosition!!.progressMeters > 400.0)
    }

    @Test
    fun repeatedCoordinatesDoNotRepeatPassedStreetInstructions() {
        val route = listOf(
            point(1L, 0.0, 0.0),
            point(2L, 0.0, 0.001),
            point(3L, 0.001, 0.001),
            point(4L, 0.001, 0.0),
            point(5L, 0.0, 0.0),
            point(6L, 0.0, -0.001)
        )
        val instructions = listOf(
            RunStreetInstruction("First turn", 200.0),
            RunStreetInstruction("Next turn", 550.0)
        )
        val tracker = RunRouteProgressTracker()

        tracker.update(1L, "return", point(10L, 0.0, 0.0005), route)
        tracker.update(1L, "return", point(11L, 0.001, 0.0), route)
        val positionAfterCrossing = tracker.update(1L, "return", point(12L, 0.0, 0.0), route)

        val guidance = nextRunStreetGuidance(instructions, positionAfterCrossing!!.progressMeters)

        assertEquals(1, guidance?.instructionIndex)
        assertEquals("Next turn", guidance?.text)
    }

    private fun point(id: Long, latitude: Double, longitude: Double) = RunPointModel(
        id = id,
        runId = 1L,
        latitude = latitude,
        longitude = longitude,
        recordedAtEpochMs = id,
        accuracyMeters = 4f,
        segmentIndex = 0
    )
}
