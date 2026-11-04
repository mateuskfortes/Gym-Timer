package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunGradientDirection
import com.example.gymtimer2.domain.model.RunGradientSegment
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.runGradientSegments
import org.junit.Assert.assertEquals
import org.junit.Test

class RunGradientTest {
    @Test
    fun classifiesReliableRouteSegmentsByElevationChange() {
        val points = listOf(
            point(0.0, 100.0),
            point(0.001, 110.0),
            point(0.002, 110.0),
            point(0.003, 100.0)
        )

        assertEquals(
            listOf(RunGradientDirection.ASCENT, RunGradientDirection.LEVEL, RunGradientDirection.DESCENT),
            runGradientSegments(points).map { it.direction }
        )
    }

    @Test
    fun skipsImpreciseAndShortSegmentsAndKeepsPausesSeparate() {
        val points = listOf(
            point(0.0, 100.0, segment = 0),
            point(0.001, 120.0, segment = 1),
            point(0.002, 140.0, segment = 1, accuracy = 20f),
            point(0.00201, 160.0, segment = 1),
            point(1.0, 100.0, segment = 2),
            point(1.00001, 110.0, segment = 2)
        )

        assertEquals(emptyList<RunGradientSegment>(), runGradientSegments(points))
    }

    private fun point(
        latitude: Double,
        altitude: Double,
        segment: Int = 0,
        accuracy: Float? = 1f
    ) = RunPointModel(
        id = 0,
        runId = 1,
        latitude = latitude,
        longitude = 0.0,
        recordedAtEpochMs = 0,
        accuracyMeters = 2f,
        segmentIndex = segment,
        altitudeMeters = altitude,
        verticalAccuracyMeters = accuracy
    )
}
