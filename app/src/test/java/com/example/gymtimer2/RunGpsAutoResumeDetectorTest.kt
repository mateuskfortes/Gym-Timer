package com.example.gymtimer2

import com.example.gymtimer2.domain.model.GpsPoint
import com.example.gymtimer2.domain.model.RunGpsAutoResumeDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunGpsAutoResumeDetectorTest {
    @Test
    fun resumesAfterThreeAcceptedPointsConfirmSustainedRunningSpeed() {
        val detector = RunGpsAutoResumeDetector()
        detector.reset(point(longitude = 0.0, elapsedMs = 1_000L))

        assertFalse(detector.shouldResume(point(0.00004, 3_000L)))
        assertFalse(detector.shouldResume(point(0.00008, 5_000L)))
        assertFalse(detector.shouldResume(point(0.00012, 7_000L)))
        assertTrue(detector.shouldResume(point(0.00016, 9_000L)))
        assertFalse(detector.shouldResume(point(0.00020, 11_000L)))
    }

    @Test
    fun doesNotResumeForSlowOrInaccurateGpsMovement() {
        val slowDetector = RunGpsAutoResumeDetector()
        slowDetector.reset(point(longitude = 0.0, elapsedMs = 1_000L))
        repeat(6) { index ->
            assertFalse(slowDetector.shouldResume(point(0.000032 * (index + 1), 3_000L + index * 2_000L)))
        }

        val inaccurateDetector = RunGpsAutoResumeDetector()
        inaccurateDetector.reset(point(longitude = 0.0, elapsedMs = 1_000L))
        assertFalse(inaccurateDetector.shouldResume(point(0.00004, 3_000L, accuracyMeters = 151f)))
    }

    @Test
    fun badAccuracyClearsAcceptedPointsBeforeGpsResume() {
        val detector = RunGpsAutoResumeDetector()
        detector.reset(point(longitude = 0.0, elapsedMs = 1_000L))

        assertFalse(detector.shouldResume(point(0.00004, 3_000L)))
        assertFalse(detector.shouldResume(point(0.00008, 5_000L)))
        assertFalse(detector.shouldResume(point(0.00010, 6_000L, accuracyMeters = 151f)))
        assertFalse(detector.shouldResume(point(0.00012, 7_000L)))
        assertFalse(detector.shouldResume(point(0.00016, 9_000L)))
        assertTrue(detector.shouldResume(point(0.00020, 11_000L)))
    }

    @Test
    fun excessiveAccelerationClearsAcceptedPointsBeforeGpsResume() {
        val detector = RunGpsAutoResumeDetector()
        detector.reset(point(longitude = 0.0, elapsedMs = 1_000L))

        assertFalse(detector.shouldResume(point(0.00004, 3_000L)))
        assertFalse(detector.shouldResume(point(0.00008, 5_000L)))
        assertFalse(detector.shouldResume(point(0.00025, 5_500L)))
        assertFalse(detector.shouldResume(point(0.00012, 7_000L)))
        assertFalse(detector.shouldResume(point(0.00016, 9_000L)))
        assertTrue(detector.shouldResume(point(0.00020, 11_000L)))
    }

    private fun point(longitude: Double, elapsedMs: Long, accuracyMeters: Float = 5f) = GpsPoint(
        latitude = 0.0,
        longitude = longitude,
        recordedAtEpochMs = elapsedMs,
        accuracyMeters = accuracyMeters,
        elapsedRealtimeMs = elapsedMs
    )
}
