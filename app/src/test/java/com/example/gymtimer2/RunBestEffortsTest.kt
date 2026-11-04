@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunBenchmarkDistance
import com.example.gymtimer2.domain.model.RunBestEfforts
import com.example.gymtimer2.domain.model.RunBestEffortOverride
import com.example.gymtimer2.domain.model.RunBestEffortRecords
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifica o cálculo GPS das melhores marcas locais. */
class RunBestEffortsTest {
    @Test
    fun calculatesCoveredDistancesAndCountsPauseTimeInsideAnEffort() {
        val efforts = RunBestEfforts.calculate(routePoints(monotonicTimes = true))

        assertEquals(
            listOf(RunBenchmarkDistance.FOUR_HUNDRED_METERS, RunBenchmarkDistance.HALF_MILE),
            efforts.map { it.distance }
        )
        assertTrue(efforts.first().durationMs in 35_000L..38_000L)
        assertTrue(efforts.last().durationMs in 140_000L..160_000L)
    }

    @Test
    fun fallsBackToRecordedTimeWhenMonotonicClockResets() {
        val efforts = RunBestEfforts.calculate(routePoints(monotonicTimes = false))

        assertEquals(2, efforts.size)
        assertTrue(efforts.last().durationMs in 140_000L..160_000L)
    }

    @Test
    fun calculatesTopThreeLifetimeAndTopTenForEachYear() {
        val runs = (1L..12L).map { runId ->
            val startedAt = if (runId == 12L) 1_736_899_200_000L else 1_705_320_000_000L
            RunModel(
                id = runId,
                startedAtEpochMs = startedAt,
                finishedAtEpochMs = startedAt + 60_000,
                durationMs = 60_000,
                distanceMeters = 445.0,
                status = RunStatus.COMPLETED,
                currentSegment = 0
            )
        }
        val points = runs.flatMap { run ->
            val durationMs = if (run.id == 12L) 40_000L else 45_000L + run.id * 1_000L
            listOf(
                historicalPoint(run.id * 2, run.id, 0.0, run.startedAtEpochMs, 1_000L),
                historicalPoint(run.id * 2 + 1, run.id, 0.004, run.startedAtEpochMs + durationMs, 1_000L + durationMs)
            )
        }

        val records = RunBestEfforts.calculateRecords(runs, points)
        val fourHundredMeters = RunBenchmarkDistance.FOUR_HUNDRED_METERS

        assertEquals(listOf(12L, 1L, 2L), records.lifetime[fourHundredMeters]?.map { it.runId })
        assertEquals((1L..10L).toList(), records.annual[2024]?.get(fourHundredMeters)?.map { it.runId })
        assertEquals(listOf(12L), records.annual[2025]?.get(fourHundredMeters)?.map { it.runId })
    }

    @Test
    fun correctedAndRemovedEffortsChangeTheLocalRankings() {
        val startedAt = 1_705_320_000_000L
        val runs = (1L..3L).map { runId ->
            RunModel(
                id = runId,
                startedAtEpochMs = startedAt + runId,
                finishedAtEpochMs = startedAt + 60_000,
                durationMs = 60_000,
                distanceMeters = 445.0,
                status = RunStatus.COMPLETED,
                currentSegment = 0
            )
        }
        val points = runs.flatMap { run ->
            listOf(
                historicalPoint(run.id * 2, run.id, 0.0, startedAt, 1_000L),
                historicalPoint(run.id * 2 + 1, run.id, 0.004, startedAt + 46_000, 47_000L)
            )
        }
        val overrides = listOf(
            RunBestEffortOverride(1L, RunBenchmarkDistance.FOUR_HUNDRED_METERS, durationMs = 90_000L),
            RunBestEffortOverride(2L, RunBenchmarkDistance.FOUR_HUNDRED_METERS, isRemoved = true)
        )

        val records = RunBestEfforts.calculateRecords(runs, points, overrides)

        assertEquals(listOf(3L, 1L), records.lifetime[RunBenchmarkDistance.FOUR_HUNDRED_METERS]?.map { it.runId })
    }

    @Test
    fun mergingPerRunRankingsMatchesCalculatingAllRunsTogether() {
        val runs = (1L..12L).map { runId ->
            val startedAt = if (runId == 12L) 1_736_899_200_000L else 1_705_320_000_000L
            RunModel(
                id = runId,
                startedAtEpochMs = startedAt,
                finishedAtEpochMs = startedAt + 60_000,
                durationMs = 60_000,
                distanceMeters = 445.0,
                status = RunStatus.COMPLETED,
                currentSegment = 0
            )
        }
        val pointsByRun = runs.associate { run ->
            val durationMs = if (run.id == 12L) 40_000L else 45_000L + run.id * 1_000L
            run.id to listOf(
                historicalPoint(run.id * 2, run.id, 0.0, run.startedAtEpochMs, 1_000L),
                historicalPoint(run.id * 2 + 1, run.id, 0.004, run.startedAtEpochMs + durationMs, 1_000L + durationMs)
            )
        }
        val overrides = listOf(
            RunBestEffortOverride(2L, RunBenchmarkDistance.FOUR_HUNDRED_METERS, isRemoved = true),
            RunBestEffortOverride(12L, RunBenchmarkDistance.FOUR_HUNDRED_METERS, durationMs = 50_000L)
        )
        val expected = RunBestEfforts.calculateRecords(runs, pointsByRun.values.flatten(), overrides)
        val actual = runs.fold(RunBestEffortRecords()) { records, run ->
            val runRecords = RunBestEfforts.calculateRecords(
                listOf(run),
                pointsByRun.getValue(run.id),
                overrides.filter { it.runId == run.id }
            )
            RunBestEfforts.mergeRecords(records, runRecords)
        }

        assertEquals(expected, actual)
    }

    private fun routePoints(monotonicTimes: Boolean) = listOf(
        point(1, 0.0, 1_000L, 1_000L, 0),
        point(2, 0.004, 41_000L, 41_000L, 0),
        point(3, 0.004, 121_000L, 121_000L, 1),
        point(4, 0.008, 161_000L, if (monotonicTimes) 161_000L else 5_000L, 1)
    )

    private fun point(
        id: Long,
        latitude: Double,
        recordedAtEpochMs: Long,
        elapsedRealtimeMs: Long,
        segmentIndex: Int
    ) = RunPointModel(
        id = id,
        runId = 1L,
        latitude = latitude,
        longitude = 0.0,
        recordedAtEpochMs = recordedAtEpochMs,
        accuracyMeters = 5f,
        segmentIndex = segmentIndex,
        elapsedRealtimeMs = elapsedRealtimeMs
    )

    private fun historicalPoint(
        id: Long,
        runId: Long,
        latitude: Double,
        recordedAtEpochMs: Long,
        elapsedRealtimeMs: Long
    ) = RunPointModel(
        id = id,
        runId = runId,
        latitude = latitude,
        longitude = 0.0,
        recordedAtEpochMs = recordedAtEpochMs,
        accuracyMeters = 5f,
        segmentIndex = 0,
        elapsedRealtimeMs = elapsedRealtimeMs
    )
}
