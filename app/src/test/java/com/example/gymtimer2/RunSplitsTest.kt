package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.domain.model.RunDistanceUnit
import com.example.gymtimer2.domain.model.RunSplit
import com.example.gymtimer2.domain.model.calculateRunSplits
import com.example.gymtimer2.domain.model.currentRunSplitPaceSecondsPerKm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifica parciais completas e o pace da parcial que ainda está sendo percorrida. */
class RunSplitsTest {
    @Test
    /** Não inclui o intervalo de pausa entre segmentos no tempo das parciais. */
    fun splitsIgnorePauseBetweenSegments() {
        val points = (0..20).map { index ->
            RunPointModel(
                id = index.toLong(), runId = 1, latitude = 0.0,
                longitude = index * 0.001, recordedAtEpochMs = index * 20_000L + if (index >= 10) 600_000L else 0L,
                accuracyMeters = 5f, segmentIndex = if (index >= 10) 1 else 0
            )
        }
        val splits = calculateRunSplits(points)
        assertEquals(2, splits.size)
        assertTrue(splits.all { it.durationMs in 170_000..190_000 })
    }

    @Test
    /** Não cria uma parcial completa quando a rota tem menos de um quilômetro. */
    fun shortRouteHasNoFullKilometer() {
        val point = RunPointModel(1, 1, 0.0, 0.0, 0, 5f, 0)
        assertTrue(calculateRunSplits(listOf(point)).isEmpty())
    }

    @Test
    fun imperialSplitsUseOneMile() {
        val points = (0..20).map { index ->
            RunPointModel(
                id = index.toLong(), runId = 1, latitude = 0.0,
                longitude = index * 0.001, recordedAtEpochMs = index * 20_000L,
                accuracyMeters = 5f, segmentIndex = 0
            )
        }

        val splits = calculateRunSplits(points, RunDistanceUnit.IMPERIAL)

        assertEquals(1, splits.size)
        assertEquals(1, splits.single().number)
    }

    @Test
    fun filteredPointsDoNotBreakDistanceBetweenAcceptedLocations() {
        val points = listOf(
            point(0, 0.0, 0L),
            point(1, 0.05, 10_000L, isFiltered = true),
            point(2, 0.01, 100_000L)
        )

        assertEquals(1, calculateRunSplits(points).size)
    }

    @Test
    /** Subtrai as parciais completas antes de estimar o pace do quilômetro atual. */
    fun currentSplitPaceUsesActiveTimeAfterCompletedKilometers() {
        val points = (0..150).map { index ->
            RunPointModel(
                id = index.toLong(), runId = 1, latitude = 0.0,
                longitude = Math.toDegrees(index * 10.0 / EARTH_RADIUS_METERS),
                recordedAtEpochMs = if (index < 50) index * 1_000L else (100 - index) * 1_000L,
                accuracyMeters = 5f, segmentIndex = 0,
                elapsedRealtimeMs = index * 1_000L
            )
        }
        val splits = calculateRunSplits(points)
        val run = RunModel(1, 0, null, 160_000L, 1_500.0, RunStatus.RUNNING, 0)

        assertEquals(1, splits.size)
        assertEquals(120.0, currentRunSplitPaceSecondsPerKm(run, splits)!!, 0.1)
        assertEquals(2_400.0, currentRunSplitPaceSecondsPerKm(run.copy(distanceMeters = 1_025.0), splits)!!, 0.1)
    }

    @Test
    fun currentSplitPaceKeepsPreviousSplitThroughFiveSeconds() {
        val run = RunModel(1, 0, null, 245_000L, 1_010.0, RunStatus.RUNNING, 0)

        assertEquals(
            240.0,
            currentRunSplitPaceSecondsPerKm(run, listOf(RunSplit(1, 240_000L)))!!,
            0.1
        )
    }

    @Test
    fun currentSplitPaceUsesCurrentSplitAfterFiveSecondsEvenBelowFiftyMeters() {
        val run = RunModel(1, 0, null, 246_000L, 1_010.0, RunStatus.RUNNING, 0)

        assertEquals(
            600.0,
            currentRunSplitPaceSecondsPerKm(run, listOf(RunSplit(1, 240_000L)))!!,
            0.1
        )
    }

    @Test
    fun currentSplitPaceFallsBackToMostRecentQualifiedSplit() {
        val run = RunModel(1, 0, null, 245_000L, 1_010.0, RunStatus.RUNNING, 0)
        val completedSplits = listOf(
            RunSplit(1, 240_000L),
            RunSplit(2, 3_000L)
        )

        assertEquals(240.0, currentRunSplitPaceSecondsPerKm(run, completedSplits)!!, 0.1)
    }

    @Test
    fun currentSplitPaceIsUnavailableBeforeAnySplitPassesFiveSeconds() {
        val run = RunModel(1, 0, null, 2_000L, 10.0, RunStatus.RUNNING, 0)

        assertNull(currentRunSplitPaceSecondsPerKm(run, emptyList()))
    }

    private companion object {
        const val EARTH_RADIUS_METERS = 6_371_000.0
    }

    private fun point(id: Int, longitude: Double, timeMs: Long, isFiltered: Boolean = false) =
        RunPointModel(
            id = id.toLong(),
            runId = 1,
            latitude = 0.0,
            longitude = longitude,
            recordedAtEpochMs = timeMs,
            accuracyMeters = 5f,
            segmentIndex = 0,
            isFiltered = isFiltered
        )
}
