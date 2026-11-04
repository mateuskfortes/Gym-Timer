package com.example.gymtimer2

import com.example.gymtimer2.data.exporting.RunExportPrivacy
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunPrivacyZone
import com.example.gymtimer2.domain.model.RunStartEndHideDistance
import com.example.gymtimer2.domain.model.RunStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class RunExportPrivacyTest {
    @Test
    fun removesOnlyPointsWithinTheChosenDistanceOfEachActivityEnd() {
        val visiblePoints = RunExportPrivacy.visiblePoints(
            run = completedRun(RunStartEndHideDistance.METERS_200),
            points = (0..4).map { index -> point(index + 1L, index * 250.0) }
        )

        assertEquals(listOf(2L, 3L, 4L), visiblePoints.map { it.id })
    }

    @Test
    fun removesAllCoordinatesWhenTheWholeRouteIsInsideHiddenEnds() {
        val visiblePoints = RunExportPrivacy.visiblePoints(
            run = completedRun(RunStartEndHideDistance.METERS_200),
            points = (0..2).map { index -> point(index + 1L, index * 100.0) }
        )

        assertEquals(emptyList<RunPointModel>(), visiblePoints)
    }

    @Test
    fun disabledPrivacyKeepsExistingExportInputOrder() {
        val points = listOf(point(2L, 250.0), point(1L, 0.0))

        assertEquals(points, RunExportPrivacy.visiblePoints(completedRun(RunStartEndHideDistance.NONE), points))
    }

    @Test
    fun breaksExportSegmentWhenSparseRouteEdgeCrossesPrivacyZone() {
        val visiblePoints = RunExportPrivacy.visiblePoints(
            run = completedRun(RunStartEndHideDistance.NONE),
            points = listOf(point(1L, -300.0), point(2L, 300.0)),
            privacyZones = listOf(
                RunPrivacyZone(name = "Home", latitude = 0.0, longitude = 0.0, radiusMeters = 200)
            )
        )

        assertEquals(listOf(1L, 2L), visiblePoints.map { it.id })
        assertEquals(listOf(0, 1), visiblePoints.map { it.segmentIndex })
    }

    @Test
    fun removesCoordinatesInsidePrivacyZoneAndBreaksTheRouteAroundIt() {
        val visiblePoints = RunExportPrivacy.visiblePoints(
            run = completedRun(RunStartEndHideDistance.NONE),
            points = listOf(point(1L, -300.0), point(2L, 0.0), point(3L, 300.0)),
            privacyZones = listOf(
                RunPrivacyZone(name = "Home", latitude = 0.0, longitude = 0.0, radiusMeters = 200)
            )
        )

        assertEquals(listOf(1L, 3L), visiblePoints.map { it.id })
        assertEquals(listOf(0, 1), visiblePoints.map { it.segmentIndex })
    }

    private fun completedRun(hideDistance: RunStartEndHideDistance) = RunModel(
        id = 1,
        startedAtEpochMs = 0,
        finishedAtEpochMs = 5_000,
        durationMs = 5_000,
        distanceMeters = 1_000.0,
        status = RunStatus.COMPLETED,
        currentSegment = 0,
        startEndHideDistance = hideDistance
    )

    private fun point(id: Long, distanceFromStartMeters: Double) = RunPointModel(
        id = id,
        runId = 1,
        latitude = 0.0,
        longitude = distanceFromStartMeters / METERS_PER_DEGREE_LONGITUDE,
        recordedAtEpochMs = id * 1_000L,
        accuracyMeters = 5f,
        segmentIndex = 0
    )

    private companion object {
        const val METERS_PER_DEGREE_LONGITUDE = 111_194.9266
    }
}
