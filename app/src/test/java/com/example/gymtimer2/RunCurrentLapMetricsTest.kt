package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunLap
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.domain.model.metricsSince
import com.example.gymtimer2.ui.screens.runs.RunLiveSegment
import com.example.gymtimer2.ui.screens.runs.active.RunStatsPage
import com.example.gymtimer2.ui.screens.runs.active.runStatsPageIndexAfterChange
import com.example.gymtimer2.ui.screens.runs.active.runStatsLiveSegmentIds
import com.example.gymtimer2.ui.screens.runs.active.runStatsPages
import com.example.gymtimer2.ui.screens.runs.active.shouldExpandRunSheetForNewSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunCurrentLapMetricsTest {
    @Test
    fun calculatesMetricsSinceTheMostRecentLap() {
        val metrics = RunMetrics.currentLapMetrics(
            activeDurationMs = 3_600_000,
            totalDistanceMeters = 5_000.0,
            completedLaps = listOf(
                RunLap(id = 1, durationMs = 1_000_000, distanceMeters = 1_400.0),
                RunLap(id = 2, durationMs = 2_400_000, distanceMeters = 3_200.0)
            )
        )

        assertEquals(1_200_000L, metrics?.durationMs)
        assertEquals(1_800.0, metrics?.distanceMeters ?: 0.0, 0.001)
        assertEquals(666.667, metrics?.paceSecondsPerKm ?: 0.0, 0.001)
    }

    @Test
    fun measuresCurrentLapFromRunStartBeforeTheFirstManualLap() {
        val metrics = RunMetrics.currentLapMetrics(1_000, 100.0, emptyList())

        assertEquals(1_000L, metrics?.durationMs)
        assertEquals(100.0, metrics?.distanceMeters ?: 0.0, 0.001)
        assertEquals(10.0, metrics?.paceSecondsPerKm ?: 0.0, 0.001)
    }

    @Test
    fun completedLapMetricsAreRelativeToThePreviousLap() {
        val metrics = RunLap(2, 2_400_000, 3_200.0).metricsSince(
            RunLap(1, 1_000_000, 1_400.0)
        )

        assertEquals(1_400_000L, metrics.durationMs)
        assertEquals(1_800.0, metrics.distanceMeters, 0.0)
        assertEquals(777.778, metrics.paceSecondsPerKm!!, 0.001)
    }

    @Test
    fun lapKeepsElapsedTimeIncludingThePause() {
        val metrics = RunLap(
            id = 2,
            durationMs = 2_400_000,
            distanceMeters = 3_200.0,
            elapsedDurationMs = 2_700_000
        ).metricsSince(
            RunLap(id = 1, durationMs = 1_000_000, distanceMeters = 1_400.0, elapsedDurationMs = 1_100_000)
        )

        assertEquals(1_400_000L, metrics.durationMs)
        assertEquals(1_600_000L, metrics.elapsedDurationMs)
    }

    @Test
    fun oldLapWithoutElapsedTimeDoesNotInventElapsedTimeForNewLap() {
        val metrics = RunLap(
            id = 2,
            durationMs = 2_400_000,
            distanceMeters = 3_200.0,
            elapsedDurationMs = 2_700_000
        ).metricsSince(RunLap(id = 1, durationMs = 1_000_000, distanceMeters = 1_400.0))

        assertNull(metrics.elapsedDurationMs)
    }

    @Test
    fun addsWorkoutInstructionsBeforeStatsWhenAWorkoutIsAttached() {
        assertEquals(
            listOf(RunStatsPage.WORKOUT, RunStatsPage.OVERVIEW, RunStatsPage.DETAILS),
            runStatsPages(showLapPage = false, showWorkoutPage = true)
        )
    }

    @Test
    fun includesLapStatsAfterTheOtherPagesOnlyWhenLapsAreAvailable() {
        assertEquals(
            listOf(RunStatsPage.OVERVIEW, RunStatsPage.DETAILS),
            runStatsPages(showLapPage = false, showWorkoutPage = false)
        )
        assertEquals(
            listOf(RunStatsPage.OVERVIEW, RunStatsPage.DETAILS, RunStatsPage.LAP_STATS),
            runStatsPages(showLapPage = true, showWorkoutPage = false)
        )
        assertEquals(
            listOf(RunStatsPage.WORKOUT, RunStatsPage.OVERVIEW, RunStatsPage.DETAILS, RunStatsPage.LAP_STATS),
            runStatsPages(showLapPage = true, showWorkoutPage = true)
        )
    }

    @Test
    fun addsTheLocalSegmentPageWhenSegmentsAreAvailable() {
        assertEquals(
            listOf(RunStatsPage.WORKOUT, RunStatsPage.LIVE_SEGMENTS, RunStatsPage.OVERVIEW, RunStatsPage.DETAILS),
            runStatsPages(showLapPage = false, showWorkoutPage = true, showLiveSegmentsPage = true)
        )
    }

    @Test
    fun keepsTheSelectedPageWhenWorkoutInstructionsLoad() {
        val previousPages = runStatsPages(showLapPage = false, showWorkoutPage = false)
        val pages = runStatsPages(showLapPage = false, showWorkoutPage = true)

        assertEquals(
            pages.indexOf(RunStatsPage.DETAILS),
            runStatsPageIndexAfterChange(previousPages, pages, currentPage = 1)
        )
    }

    @Test
    fun selectsTheLiveSegmentPageWhenANewSegmentStarts() {
        val pages = runStatsPages(showLapPage = false, showWorkoutPage = false, showLiveSegmentsPage = true)

        assertEquals(
            pages.indexOf(RunStatsPage.LIVE_SEGMENTS),
            runStatsPageIndexAfterChange(
                previousPages = runStatsPages(showLapPage = false, showWorkoutPage = false),
                pages = pages,
                currentPage = 1,
                hasNewLiveSegment = true
            )
        )
    }

    @Test
    fun tracksOnlyStartedSegmentsThatAreStillActive() {
        val liveSegment = segment(routeId = 7L)
        val updatedLiveSegment = liveSegment.copy(progressMeters = 40.0, elapsedDurationMs = 12_000L)

        assertEquals(setOf("route:7"), runStatsLiveSegmentIds(listOf(updatedLiveSegment)))
        assertEquals(emptySet<String>(), runStatsLiveSegmentIds(listOf(segment(routeId = 8L, distanceToStartMeters = 40.0))))
        assertEquals(emptySet<String>(), runStatsLiveSegmentIds(listOf(segment(routeId = 9L, isCompleted = true))))
    }

    @Test
    fun expandsTheSheetForANewSegmentUnlessTheMapIsFullscreen() {
        assertTrue(
            shouldExpandRunSheetForNewSegment(emptySet(), setOf("route:1"), false, false)
        )
        assertFalse(
            shouldExpandRunSheetForNewSegment(setOf("route:1"), setOf("route:1"), false, false)
        )
        assertFalse(
            shouldExpandRunSheetForNewSegment(emptySet(), setOf("route:1"), true, false)
        )
        assertFalse(
            shouldExpandRunSheetForNewSegment(emptySet(), setOf("route:1"), false, true)
        )
    }

    private fun segment(
        routeId: Long,
        distanceToStartMeters: Double? = null,
        isCompleted: Boolean = false
    ) = RunLiveSegment(
        name = null,
        progressMeters = 0.0,
        distanceMeters = 500.0,
        elapsedDurationMs = 0L,
        bestDurationMs = null,
        differenceFromBestMs = null,
        isCompleted = isCompleted,
        isNewRecord = false,
        distanceToStartMeters = distanceToStartMeters,
        routeId = routeId
    )
}
