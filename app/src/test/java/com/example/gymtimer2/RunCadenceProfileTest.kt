package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.calculateRunCadenceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifica o gráfico de cadência e os intervalos pausados da corrida. */
class RunCadenceProfileTest {
    @Test
    fun profileUsesValidCadenceAndBreaksAtPause() {
        val points = listOf(
            point(1, 0.0, 0L, 0, 170),
            point(2, 0.001, 60_000L, 0, 0),
            point(3, 0.002, 120_000L, 0, 180),
            point(4, 0.5, 3_600_000L, 1, 170),
            point(5, 0.501, 3_660_000L, 1, 190)
        )

        val profile = calculateRunCadenceProfile(points)

        assertEquals(2, profile.size)
        assertEquals(180, profile[0].stepsPerMinute)
        assertEquals(190, profile[1].stepsPerMinute)
        assertEquals(0, profile[0].segmentIndex)
        assertEquals(1, profile[1].segmentIndex)
        assertTrue(profile[1].distanceMeters > profile[0].distanceMeters)
    }

    private fun point(
        id: Int,
        longitude: Double,
        timestampMs: Long,
        segmentIndex: Int,
        cadenceStepsPerMinute: Int
    ) = RunPointModel(
        id = id.toLong(),
        runId = 1L,
        latitude = 0.0,
        longitude = longitude,
        recordedAtEpochMs = timestampMs,
        accuracyMeters = 5f,
        segmentIndex = segmentIndex,
        cadenceStepsPerMinute = cadenceStepsPerMinute,
        elapsedRealtimeMs = timestampMs
    )
}
