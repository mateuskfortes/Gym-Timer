package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunStepRateAccumulator
import com.example.gymtimer2.domain.model.RunStepRateReading
import com.example.gymtimer2.domain.model.GpsPoint
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunStepRateSample
import com.example.gymtimer2.domain.model.averageRunCadence
import com.example.gymtimer2.domain.model.averageRunCadenceFromEvents
import com.example.gymtimer2.domain.model.currentRunStepRate
import com.example.gymtimer2.domain.model.shouldCollectRunStepRate
import com.example.gymtimer2.domain.model.withCurrentRunCadence
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifica o cálculo e a validade temporal da cadência do sensor. */
class RunCadenceTest {
    @Test
    fun stepRateCollectionRequiresPreferenceSensorAndPermission() {
        assertTrue(shouldCollectRunStepRate(true, sensorAvailable = true, permissionGranted = true))
        assertFalse(shouldCollectRunStepRate(false, sensorAvailable = true, permissionGranted = true))
        assertFalse(shouldCollectRunStepRate(true, sensorAvailable = false, permissionGranted = true))
        assertFalse(shouldCollectRunStepRate(true, sensorAvailable = true, permissionGranted = false))
    }

    @Test
    fun aggregatesCounterUpdatesIntoTwoSecondSamples() {
        val accumulator = RunStepRateAccumulator()

        accumulator.recordCounterSample(1_000L, 100)
        accumulator.recordCounterSample(2_000L, 104)
        accumulator.recordCounterSample(3_000L, 105)

        assertEquals(RunStepRateSample(150, 5, 10_000L, 2_000L), accumulator.sample(3_000L, 10_000L))
        assertEquals(RunStepRateSample(0, 0, 12_000L, 5_000L), accumulator.sample(8_000L, 12_000L))
    }

    @Test
    fun reportsZeroForCadenceBelowSixtyStepsPerMinute() {
        val accumulator = RunStepRateAccumulator()
        accumulator.recordCounterSample(1_000L, 100)
        accumulator.recordCounterSample(7_000L, 105)

        assertEquals(RunStepRateSample(0, 5, 7_000L, 2_000L), accumulator.sample(7_000L, 7_000L))
    }

    @Test
    fun recoversWhenCumulativeSensorCounterResets() {
        val accumulator = RunStepRateAccumulator()
        accumulator.recordCounterSample(1_000L, 100)
        accumulator.recordCounterSample(2_000L, 105)
        accumulator.sample(2_000L, 2_000L)

        accumulator.recordCounterSample(3_000L, 0)
        accumulator.recordCounterSample(4_000L, 5)

        assertEquals(RunStepRateSample(300, 5, 4_000L, 2_000L), accumulator.sample(4_000L, 4_000L))
    }

    @Test
    fun resetStartsANewExpectedSampleInterval() {
        val accumulator = RunStepRateAccumulator()
        accumulator.sample(1_000L, 1_000L)
        accumulator.reset()

        assertEquals(RunStepRateSample(0, 0, 20_000L, 2_000L), accumulator.sample(20_000L, 20_000L))
    }

    @Test
    fun hidesCadenceWhenItIsStaleOrBelongsToAnotherRun() {
        val reading = RunStepRateReading(runId = 4L, measuredAtElapsedMs = 10_000L, stepsPerMinute = 180)

        assertEquals(180, currentRunStepRate(reading, runId = 4L, nowElapsedMs = 15_000L))
        assertNull(currentRunStepRate(reading, runId = 5L, nowElapsedMs = 12_000L))
        assertNull(currentRunStepRate(reading, runId = 4L, nowElapsedMs = 15_001L))
    }

    @Test
    fun attachesOnlyFreshCadenceFromTheCurrentRunToGpsPoints() {
        val point = GpsPoint(latitude = 0.0, longitude = 0.0, recordedAtEpochMs = 10_000L, accuracyMeters = 5f)
        val reading = RunStepRateReading(runId = 4L, measuredAtElapsedMs = 10_000L, stepsPerMinute = 180)

        assertEquals(180, point.withCurrentRunCadence(reading, runId = 4L, nowElapsedMs = 15_000L).cadenceStepsPerMinute)
        assertNull(point.withCurrentRunCadence(reading, runId = 4L, nowElapsedMs = 15_001L).cadenceStepsPerMinute)
        assertNull(point.withCurrentRunCadence(reading, runId = 5L, nowElapsedMs = 12_000L).cadenceStepsPerMinute)
    }

    @Test
    fun averagesOnlyValidSavedCadenceSamples() {
        val points = listOf(point(160), point(null), point(180), point(400))

        assertEquals(170, averageRunCadence(points))
        assertNull(averageRunCadence(emptyList()))
    }

    @Test
    fun weightsGpsCadenceByElapsedInterval() {
        val points = listOf(
            point(160).copy(elapsedRealtimeMs = 1_000L),
            point(170).copy(elapsedRealtimeMs = 3_000L),
            point(180).copy(elapsedRealtimeMs = 7_000L)
        )

        assertEquals(177, averageRunCadence(points))
    }

    @Test
    fun ignoresPausedTimeWhenWeightingGpsCadence() {
        val points = listOf(
            point(160, segment = 0).copy(elapsedRealtimeMs = 1_000L),
            point(180, segment = 0).copy(elapsedRealtimeMs = 3_000L),
            point(200, segment = 1).copy(elapsedRealtimeMs = 100_000L),
            point(200, segment = 1).copy(elapsedRealtimeMs = 102_000L)
        )

        assertEquals(190, averageRunCadence(points))
    }

    @Test
    fun weightsEventAverageByDurationWhenEverySampleHasAnInterval() {
        val samples = listOf(
            RunStepRateSample(160, 5, 1_000L, 2_000L),
            RunStepRateSample(180, 15, 3_000L, 2_000L)
        )

        assertEquals(170, averageRunCadenceFromEvents(samples))
    }

    @Test
    fun keepsCountWeightedAverageForLegacySamplesWithoutDuration() {
        val samples = listOf(
            RunStepRateSample(160, 5, 1_000L),
            RunStepRateSample(180, 15, 3_000L),
            RunStepRateSample(0, 5, 5_000L),
            RunStepRateSample(400, 3, 7_000L)
        )

        assertEquals(175, averageRunCadenceFromEvents(samples))
        assertNull(averageRunCadenceFromEvents(emptyList()))
    }

    private fun point(cadence: Int?, segment: Int = 0) = RunPointModel(
        id = segment.toLong(),
        runId = 1,
        latitude = 0.0,
        longitude = 0.0,
        recordedAtEpochMs = 0,
        accuracyMeters = 5f,
        segmentIndex = segment,
        cadenceStepsPerMinute = cadence
    )
}
