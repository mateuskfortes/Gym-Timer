package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunActivityCrop
import com.example.gymtimer2.domain.model.RunActivityType
import com.example.gymtimer2.domain.model.RunLap
import com.example.gymtimer2.domain.model.RunLocalSegmentEffort
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.domain.model.RunWorkoutStep
import com.example.gymtimer2.domain.model.RunWorkoutTrigger
import com.example.gymtimer2.domain.model.calculateRunActivityCrop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunActivityCropTest {
    @Test
    fun croppingKeepsSelectedRouteAndRebasesDerivedActivityData() {
        val points = routePoints()
        val crop = calculateRunActivityCrop(
            run = completedRun(),
            points = points,
            startPointId = 2L,
            endPointId = 3L,
            laps = listOf(
                RunLap(1L, 25_000L, 166.0, 25_000L, 27_000L),
                RunLap(2L, 35_000L, 277.0, 35_000L)
            ),
            workoutSteps = listOf(
                workoutStep(stepIndex = 0, startedAt = 15_000L, startedAtDistance = 55.0),
                workoutStep(
                    stepIndex = 1,
                    startedAt = 25_000L,
                    startedAtDistance = 166.0,
                    completedAt = 30_000L,
                    completedAtDistance = 222.0
                )
            ),
            localSegmentEfforts = listOf(
                RunLocalSegmentEffort(1L, 80.0, 22_000L, 28_000L),
                RunLocalSegmentEffort(2L, 35.0, 35_000L, null)
            )
        )

        assertNotNull(crop)
        assertEquals(listOf(2L, 3L), crop?.points?.map(RunPointModel::id))
        assertEquals(20_000L, crop?.startedAtEpochMs)
        assertEquals(30_000L, crop?.finishedAtEpochMs)
        assertEquals(10_000L, crop?.durationMs)
        assertEquals(10_000L, crop?.elapsedDurationMs)
        assertTrue(crop!!.distanceMeters in 100.0..120.0)
        assertEquals(1, crop.laps.size)
        assertEquals(5_000L, crop.laps.single().durationMs)
        assertEquals(7_000L, crop.laps.single().elapsedDurationMs)
        assertTrue(crop.laps.single().distanceMeters in 45.0..65.0)
        assertEquals(2, crop.workoutSteps.size)
        assertEquals(0L, crop.workoutSteps.first().startedAtDurationMs)
        assertNull(crop.workoutSteps.first().completedAtDurationMs)
        assertEquals(5_000L, crop.workoutSteps.last().startedAtDurationMs)
        assertEquals(10_000L, crop.workoutSteps.last().completedAtDurationMs)
        assertEquals(2_000L, crop.localSegmentEfforts.single().startedAtDurationMs)
        assertEquals(8_000L, crop.localSegmentEfforts.single().completedAtDurationMs)
    }

    @Test
    fun fullRangePreservesOriginalRunMetrics() {
        val crop = calculateRunActivityCrop(
            run = completedRun(),
            points = routePoints(),
            startPointId = 1L,
            endPointId = 4L
        )

        assertNotNull(crop)
        assertEquals(40_000L, crop?.durationMs)
        assertEquals(45_000L, crop?.elapsedDurationMs)
        assertEquals(360.0, crop?.distanceMeters)
        assertEquals(0L, crop?.startedAtEpochMs)
        assertEquals(45_000L, crop?.finishedAtEpochMs)
    }

    @Test
    fun croppingAcrossPausedSegmentsExcludesPauseFromActiveTime() {
        val pausedPoints = routePoints().mapIndexed { index, point ->
            if (index < 2) point.copy(segmentIndex = 0)
            else point.copy(segmentIndex = 1, recordedAtEpochMs = point.recordedAtEpochMs + 60_000L)
        }
        val crop = calculateRunActivityCrop(
            run = completedRun(durationMs = 30_000L, elapsedDurationMs = 90_000L),
            points = pausedPoints,
            startPointId = 1L,
            endPointId = 4L
        )

        assertEquals(30_000L, crop?.durationMs)
        assertEquals(90_000L, crop?.elapsedDurationMs)
    }

    @Test
    fun croppingRebasesLapElapsedTimeAcrossPause() {
        val points = listOf(
            routePoint(1L, 0.0, 10_000L, 10_000L, 100_000L, 0),
            routePoint(2L, 0.001, 20_000L, 20_000L, 110_000L, 0),
            routePoint(3L, 0.0012, 80_000L, 20_000L, 170_000L, 1),
            routePoint(4L, 0.0022, 90_000L, 30_000L, 180_000L, 1)
        )
        val crop = calculateRunActivityCrop(
            run = completedRun(durationMs = 30_000L, elapsedDurationMs = 90_000L).copy(
                distanceMeters = 222.0,
                finishedAtEpochMs = 90_000L
            ),
            points = points,
            startPointId = 2L,
            endPointId = 4L,
            laps = listOf(RunLap(1L, 30_000L, 222.0, 90_000L, 90_000L))
        )

        assertEquals(70_000L, crop?.elapsedDurationMs)
        assertEquals(10_000L, crop?.laps?.single()?.durationMs)
        assertEquals(70_000L, crop?.laps?.single()?.elapsedDurationMs)
    }

    @Test
    fun cropUsesActiveDurationCapturedWithEachPoint() {
        val points = routePoints().mapIndexed { index, point ->
            point.copy(activeDurationMs = listOf(2_000L, 8_000L, 19_000L, 27_000L)[index])
        }
        val crop = calculateRunActivityCrop(completedRun(), points, 2L, 3L)

        assertEquals(11_000L, crop?.durationMs)
    }

    @Test
    fun invalidOrZeroDistanceSelectionIsRejected() {
        val points = routePoints()

        assertNull(calculateRunActivityCrop(completedRun(), points, 2L, 2L))
        assertNull(calculateRunActivityCrop(completedRun(), points, 4L, 1L))
        assertNull(calculateRunActivityCrop(completedRun().copy(status = RunStatus.RUNNING), points, 1L, 4L))
        assertNull(
            calculateRunActivityCrop(
                completedRun(),
                points.map { it.copy(longitude = 0.0) },
                1L,
                3L
            )
        )
    }

    private fun completedRun(
        durationMs: Long = 40_000L,
        elapsedDurationMs: Long? = 45_000L
    ) = RunModel(
        id = 4L,
        startedAtEpochMs = 0L,
        finishedAtEpochMs = 45_000L,
        durationMs = durationMs,
        distanceMeters = 360.0,
        status = RunStatus.COMPLETED,
        currentSegment = 0,
        activityType = RunActivityType.RUN,
        elapsedDurationMs = elapsedDurationMs
    )

    private fun routePoints() = listOf(0.0, 0.001, 0.002, 0.003).mapIndexed { index, longitude ->
        RunPointModel(
            id = index + 1L,
            runId = 4L,
            latitude = 0.0,
            longitude = longitude,
            recordedAtEpochMs = (index + 1L) * 10_000L,
            accuracyMeters = 5f,
            segmentIndex = 0,
            elapsedRealtimeMs = (index + 1L) * 10_000L
        )
    }

    private fun routePoint(
        id: Long,
        longitude: Double,
        recordedAtEpochMs: Long,
        activeDurationMs: Long,
        elapsedRealtimeMs: Long,
        segmentIndex: Int
    ) = RunPointModel(
        id = id,
        runId = 4L,
        latitude = 0.0,
        longitude = longitude,
        recordedAtEpochMs = recordedAtEpochMs,
        accuracyMeters = 5f,
        segmentIndex = segmentIndex,
        activeDurationMs = activeDurationMs,
        elapsedRealtimeMs = elapsedRealtimeMs
    )

    private fun workoutStep(
        stepIndex: Int,
        startedAt: Long,
        startedAtDistance: Double,
        completedAt: Long? = null,
        completedAtDistance: Double? = null
    ) = RunWorkoutStep(
        stepIndex = stepIndex,
        description = "Intervalo ${stepIndex + 1}",
        trigger = RunWorkoutTrigger.DURATION,
        durationTargetMs = 10_000L,
        startedAtDurationMs = startedAt,
        startedAtDistanceMeters = startedAtDistance,
        completedAtDurationMs = completedAt,
        completedAtDistanceMeters = completedAtDistance
    )
}
