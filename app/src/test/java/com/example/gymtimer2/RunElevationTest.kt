package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.calculateRunElevationProfile
import com.example.gymtimer2.domain.model.estimatedElevationGainMeters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunElevationTest {
    @Test
    fun elevationGainUsesEightMeterThresholdAndCountsEachClimbExtensionOnce() {
        val points = listOf(point(100.0), point(104.0), point(109.0), point(105.0), point(110.0),
            point(108.0), point(118.0))

        assertEquals(18.0, estimatedElevationGainMeters(points)!!, 0.0)
    }

    @Test
    fun elevationGainResetsAfterAnAltitudeJumpOverTenMeters() {
        val points = listOf(point(100.0), point(111.0), point(119.0))

        assertEquals(10.0, estimatedElevationGainMeters(listOf(point(100.0), point(110.0)))!!, 0.0)
        assertEquals(8.0, estimatedElevationGainMeters(points)!!, 0.0)
    }

    @Test
    fun elevationGainUsesAltitudeWithoutVerticalAccuracyAcrossPausedSegments() {
        val points = listOf(point(100.0, accuracy = null), point(108.0, segment = 1, accuracy = null))

        assertEquals(8.0, estimatedElevationGainMeters(points)!!, 0.0)
    }

    @Test
    fun elevationGainReturnsZeroAfterOneUsablePointAndNullWithoutAltitude() {
        assertEquals(0.0, estimatedElevationGainMeters(listOf(point(100.0)))!!, 0.0)
        assertNull(estimatedElevationGainMeters(listOf(point(null))))
    }

    @Test
    fun elevationProfileUsesRouteDistanceAndKeepsPausedSegmentsSeparate() {
        val profile = calculateRunElevationProfile(
            listOf(
                point(100.0, latitude = 0.0),
                point(110.0, latitude = 0.001),
                point(120.0, latitude = 0.002),
                point(130.0, segment = 1, latitude = 0.01),
                point(140.0, segment = 1, latitude = 0.011)
            )
        )

        assertEquals(5, profile.size)
        assertEquals(111.0, profile[1].distanceMeters, 2.0)
        assertEquals(profile[2].distanceMeters, profile[3].distanceMeters, 0.0)
        assertEquals(1, profile[3].segmentIndex)
    }

    private fun point(
        altitude: Double?,
        segment: Int = 0,
        accuracy: Float? = 2f,
        latitude: Double = 0.0
    ) = RunPointModel(
        id = 0, runId = 1, latitude = latitude, longitude = 0.0, recordedAtEpochMs = 0,
        accuracyMeters = 2f, segmentIndex = segment, altitudeMeters = altitude, verticalAccuracyMeters = accuracy
    )
}
