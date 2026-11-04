@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2

import com.example.gymtimer2.domain.model.GpsPoint
import com.example.gymtimer2.domain.model.RunGpsFilter
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.domain.model.RunActivityTag
import com.example.gymtimer2.domain.model.RunActivityType
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.domain.model.elapsedDurationAt
import com.example.gymtimer2.ui.screens.runs.RunUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifica filtros GPS e formatação sem depender de um dispositivo. */
class RunMetricsTest {
    @Test
    fun activityTagsHaveStableStorageAndIgnoreUnknownValues() {
        val tags = setOf(RunActivityTag.RECOVERY, RunActivityTag.RACE)

        val stored = RunActivityTag.serialize(tags)

        assertEquals("RACE,RECOVERY", stored)
        assertEquals(tags, RunActivityTag.parse("RACE,RECOVERY,UNKNOWN"))
    }

    @Test
    fun raceTagUsesElapsedTimeForSummaryMetrics() {
        val run = RunModel(
            id = 1,
            startedAtEpochMs = 1_000L,
            finishedAtEpochMs = 61_000L,
            durationMs = 50_000L,
            distanceMeters = 100.0,
            status = RunStatus.COMPLETED,
            currentSegment = 0,
            activityType = RunActivityType.RUN,
            activityTags = setOf(RunActivityTag.RACE)
        )

        assertEquals(60_000L, run.performanceDurationMs)
        assertEquals(600.0, run.averagePaceSecondsPerKm!!, 0.0)
        assertEquals(6.0, run.averageSpeedKmPerHour!!, 0.0)
        assertEquals(50_000L, run.copy(activityTags = emptySet()).performanceDurationMs)
    }

    @Test
    fun raceMetricsPreferPersistedMonotonicElapsedDuration() {
        val run = RunModel(
            id = 1,
            startedAtEpochMs = 1_000L,
            finishedAtEpochMs = 3_601_000L,
            durationMs = 50_000L,
            distanceMeters = 100.0,
            status = RunStatus.COMPLETED,
            currentSegment = 0,
            activityTags = setOf(RunActivityTag.RACE),
            elapsedDurationMs = 60_000L
        )

        assertEquals(60_000L, run.performanceDurationMs)
    }

    @Test
    fun elapsedDurationUsesMonotonicClockAndWallClockAfterReboot() {
        val run = RunModel(
            id = 1,
            startedAtEpochMs = 400_000L,
            finishedAtEpochMs = null,
            durationMs = 45_000L,
            distanceMeters = 100.0,
            status = RunStatus.PAUSED,
            currentSegment = 0,
            elapsedDurationMs = 50_000L,
            elapsedRealtimeAnchorMs = 10_000L,
            elapsedEpochAnchorMs = 500_000L
        )

        assertEquals(55_000L, run.elapsedDurationAt(15_000L, 450_000L))
        assertEquals(60_000L, run.elapsedDurationAt(5_000L, 510_000L))
    }

    @Test
    fun averageSpeedUsesActiveTimeAndHandlesMissingTime() {
        val run = RunModel(1, 0, null, 600_000, 1_000.0, RunStatus.RUNNING, 0)
        assertEquals(6.0, run.averageSpeedKmPerHour!!, 0.001)
        assertNull(run.copy(durationMs = 0).averageSpeedKmPerHour)
        assertEquals(0.0, run.copy(distanceMeters = 0.0).averageSpeedKmPerHour!!, 0.0)
    }

    @Test
    /** Converte o pace atual para velocidade sem criar outra fonte de GPS. */
    fun currentSpeedUsesTheCurrentPaceReading() {
        assertEquals(9.0, RunUiState(currentPaceSecondsPerKm = 400.0).currentSpeedKmPerHour!!, 0.0)
        assertNull(RunUiState().currentSpeedKmPerHour)
    }

    @Test
    fun compactRunSummaryUsesCurrentSplitPaceWhileRunning() {
        val run = RunModel(1, 0, null, 600_000, 1_000.0, RunStatus.RUNNING, 0)

        assertEquals(350.0, RunMetrics.compactPaceSecondsPerKm(run, 350.0)!!, 0.0)
    }

    @Test
    fun compactRunSummaryUsesAveragePaceWhilePaused() {
        val run = RunModel(1, 0, null, 600_000, 1_000.0, RunStatus.PAUSED, 0)

        assertEquals(600.0, RunMetrics.compactPaceSecondsPerKm(run, 350.0)!!, 0.0)
    }

    @Test
    fun compactRunSummaryUsesAveragePaceDuringStructuredWorkouts() {
        val run = RunModel(
            id = 1,
            startedAtEpochMs = 0,
            finishedAtEpochMs = null,
            durationMs = 600_000,
            distanceMeters = 1_000.0,
            status = RunStatus.RUNNING,
            currentSegment = 0,
            workoutTitle = "Intervalos"
        )

        assertEquals(600.0, RunMetrics.compactPaceSecondsPerKm(run, 350.0)!!, 0.0)
    }

    @Test
    fun automaticSplitsAreHiddenOnlyWhenStructuredWorkoutIsSelected() {
        val run = RunModel(1, 0, null, 600_000, 1_000.0, RunStatus.RUNNING, 0)

        assertTrue(RunMetrics.shouldShowAutomaticSplits(run))
        assertFalse(RunMetrics.shouldShowAutomaticSplits(run.copy(workoutTitle = "Intervalos")))
    }

    @Test
    /** Confirma que um segmento com precisão válida soma sua distância geográfica. */
    fun acceptedGpsSegmentAddsGeographicDistance() {
        val previous = gpsPoint(longitude = 0.0, recordedAtMs = 1_000L)
        val current = gpsPoint(longitude = 0.0001, recordedAtMs = 6_000L)

        val distance = RunMetrics.acceptedDistanceMeters(previous, current)

        assertTrue(distance != null && distance in 10.0..12.0)
    }

    @Test
    /** Confirma o limite de precisão horizontal observado no Strava. */
    fun acceptsLocationWithinStravaAccuracyLimit() {
        val previous = gpsPoint(longitude = 0.0, recordedAtMs = 1_000L)
        val current = gpsPoint(longitude = 0.0001, recordedAtMs = 6_000L, accuracyMeters = 150f)

        assertTrue(RunMetrics.acceptedDistanceMeters(previous, current) != null)
    }

    @Test
    fun rejectsAccuracyAboveStravaLimit() {
        val previous = gpsPoint(longitude = 0.0, recordedAtMs = 1_000L)
        val current = gpsPoint(longitude = 0.0001, recordedAtMs = 6_000L, accuracyMeters = 151f)

        assertNull(RunMetrics.acceptedDistanceMeters(previous, current))
    }

    @Test
    fun rejectsFilteredPointEvenWhenItsAccuracyIsValid() {
        val previous = gpsPoint(longitude = 0.0, recordedAtMs = 1_000L)
        val current = gpsPoint(longitude = 0.0001, recordedAtMs = 6_000L).copy(isFiltered = true)

        assertNull(RunMetrics.acceptedDistanceMeters(null, current))
        assertNull(RunMetrics.acceptedDistanceMeters(previous, current))
    }

    @Test
    /** Confirma que um salto incompatível com corrida não altera a distância. */
    fun rejectsImpossibleGpsJump() {
        val previous = gpsPoint(longitude = 0.0, recordedAtMs = 1_000L)
        val current = gpsPoint(longitude = 0.001, recordedAtMs = 2_000L)

        assertNull(RunMetrics.acceptedDistanceMeters(previous, current))
    }

    @Test
    fun liveFilterUsesThreeMeterMinimumAndKeepsTheLastAcceptedPoint() {
        val filter = RunGpsFilter()

        assertEquals(0.0, filter.filter(gpsPoint(0.0, 1_000L, elapsedMs = 1_000L)).distanceMeters!!, 0.0)
        assertNull(filter.filter(gpsPoint(0.000018, 2_000L, elapsedMs = 2_000L)).distanceMeters)
        val acceptedDistance = filter.filter(
            gpsPoint(0.000036, 3_000L, elapsedMs = 3_000L)
        ).distanceMeters

        assertTrue(acceptedDistance != null && acceptedDistance in 3.0..5.0)
    }

    @Test
    fun liveFilterRejectsAccelerationSpikeWithoutAdvancingItsState() {
        val filter = RunGpsFilter()
        filter.filter(gpsPoint(0.0, 1_000L, elapsedMs = 1_000L))
        assertTrue(filter.filter(gpsPoint(0.000036, 2_000L, elapsedMs = 2_000L)).distanceMeters != null)

        assertNull(filter.filter(gpsPoint(0.000171, 2_500L, elapsedMs = 2_500L)).distanceMeters)
        assertTrue(filter.filter(gpsPoint(0.000072, 3_000L, elapsedMs = 3_000L)).distanceMeters != null)
    }

    @Test
    fun liveFilterRejectsRepeatedMonotonicTimestamp() {
        val filter = RunGpsFilter()
        filter.filter(gpsPoint(0.0, 1_000L, elapsedMs = 1_000L))

        assertNull(filter.filter(gpsPoint(0.000036, 2_000L, elapsedMs = 1_000L)).distanceMeters)
        assertTrue(filter.filter(gpsPoint(0.000036, 3_000L, elapsedMs = 3_000L)).distanceMeters != null)
    }

    @Test
    fun liveFilterRejectsOutOfOrderSampleAfterSubdistancePoint() {
        val filter = RunGpsFilter()
        filter.filter(gpsPoint(0.0, 1_000L, elapsedMs = 1_000L))
        filter.filter(gpsPoint(0.000036, 2_000L, elapsedMs = 2_000L))
        assertNull(filter.filter(gpsPoint(0.00005, 3_000L, elapsedMs = 3_000L)).distanceMeters)

        assertNull(filter.filter(gpsPoint(0.00009, 2_500L, elapsedMs = 2_500L)).distanceMeters)
        assertTrue(filter.filter(gpsPoint(0.00007, 3_500L, elapsedMs = 3_500L)).distanceMeters != null)
    }

    @Test
    /** Confirma o pace médio e a ausência de pace quando a distância é zero. */
    fun averagePaceUsesActiveDurationAndDistance() {
        val pace = RunMetrics.averagePaceSecondsPerKm(durationMs = 120_000L, distanceMeters = 1_000.0)

        assertEquals(120.0, pace!!, 0.0)
        assertEquals("2:00/km", RunMetrics.formatPace(pace))
        assertNull(RunMetrics.averagePaceSecondsPerKm(durationMs = 10_000L, distanceMeters = 0.0))
    }

    @Test
    /** Confirma os formatos de tempo abaixo e acima de uma hora. */
    fun formatsElapsedTimeWithHoursWhenNeeded() {
        assertEquals("02:05", RunMetrics.formatDuration(125_000L))
        assertEquals("1:02:05", RunMetrics.formatDuration(3_725_000L))
    }

    /** Cria uma posição GPS com valores controlados para cada cenário. */
    private fun gpsPoint(
        longitude: Double,
        recordedAtMs: Long = 1_000L,
        accuracyMeters: Float = 5f,
        elapsedMs: Long? = null
    ) = GpsPoint(
        latitude = 0.0,
        longitude = longitude,
        recordedAtEpochMs = recordedAtMs,
        accuracyMeters = accuracyMeters,
        elapsedRealtimeMs = elapsedMs
    )
}
