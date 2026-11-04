package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.calculateRunPaceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifica o pace do gráfico local e as interrupções entre trechos de uma corrida. */
class RunPaceProfileTest {
    @Test
    fun profileUsesAcceptedGpsDistanceAndElapsedTime() {
        val profile = calculateRunPaceProfile(
            listOf(
                point(id = 1, longitude = 0.0, timestampMs = 0L, segmentIndex = 0),
                point(id = 2, longitude = 0.009, timestampMs = 600_000L, segmentIndex = 0)
            )
        )

        assertEquals(1, profile.size)
        assertTrue(profile.single().secondsPerKm in 590.0..610.0)
    }

    @Test
    fun profileDoesNotJoinPausedSegments() {
        val points = listOf(
            point(id = 1, longitude = 0.0, timestampMs = 0L, segmentIndex = 0),
            point(id = 2, longitude = 0.001, timestampMs = 120_000L, segmentIndex = 0),
            point(id = 3, longitude = 0.5, timestampMs = 3_600_000L, segmentIndex = 1),
            point(id = 4, longitude = 0.501, timestampMs = 3_720_000L, segmentIndex = 1)
        )

        val profile = calculateRunPaceProfile(points)

        assertEquals(2, profile.size)
        assertTrue(profile[1].distanceMeters > profile[0].distanceMeters)
        assertEquals(0, profile[0].segmentIndex)
        assertEquals(1, profile[1].segmentIndex)
    }

    @Test
    fun longProfileIsSampledAndRetainsItsFinalDistance() {
        val points = (0..300).map { index ->
            point(
                id = index,
                longitude = index * 0.0001,
                timestampMs = index * 10_000L,
                segmentIndex = 0
            )
        }

        val profile = calculateRunPaceProfile(points)

        assertTrue(profile.size <= 256)
        assertTrue(profile.last().distanceMeters > 3_300.0)
    }

    private fun point(id: Int, longitude: Double, timestampMs: Long, segmentIndex: Int) = RunPointModel(
        id = id.toLong(),
        runId = 1L,
        latitude = 0.0,
        longitude = longitude,
        recordedAtEpochMs = timestampMs,
        accuracyMeters = 5f,
        segmentIndex = segmentIndex,
        elapsedRealtimeMs = timestampMs
    )
}
