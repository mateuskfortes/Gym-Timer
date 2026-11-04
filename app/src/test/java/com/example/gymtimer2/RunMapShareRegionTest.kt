package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunPrivacyZone
import com.example.gymtimer2.domain.model.RunStartEndHideDistance
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.ui.screens.runs.history.runMapShareRegion
import com.example.gymtimer2.ui.screens.runs.history.runMapShareablePoints
import com.example.gymtimer2.ui.screens.runs.history.shareableRunSegments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunMapShareRegionTest {
    @Test
    fun regionContainsAcceptedRouteAndIgnoresFilteredOutlier() {
        val region = runMapShareRegion(
            listOf(
                point(0, 10.0, 20.0),
                point(1, 10.02, 20.04),
                point(2, -40.0, -70.0, isFiltered = true)
            )
        )

        assertNotNull(region)
        assertTrue(region!!.northLatitude >= 10.02)
        assertTrue(region.southLatitude <= 10.0)
        assertTrue(region.eastLongitude >= 20.04)
        assertTrue(region.westLongitude <= 20.0)
    }

    @Test
    fun regionExpandsZeroAreaRoutesAndRequiresTwoAcceptedPoints() {
        val sameLocationRoute = runMapShareRegion(listOf(point(0, 10.0, 20.0), point(1, 10.0, 20.0)))

        assertNotNull(sameLocationRoute)
        assertTrue(sameLocationRoute!!.northLatitude > sameLocationRoute.southLatitude)
        assertTrue(sameLocationRoute.eastLongitude > sameLocationRoute.westLongitude)
        assertNull(runMapShareRegion(listOf(point(0, 10.0, 20.0))))
        assertNull(runMapShareRegion(listOf(point(0, 10.0, 20.0, isFiltered = true), point(1, 10.1, 20.1))))
    }

    @Test
    fun shareableSegmentsDoNotJoinAcrossPauses() {
        val segments = shareableRunSegments(
            listOf(
                point(0, 10.0, 20.0, segmentIndex = 0),
                point(1, 10.01, 20.01, segmentIndex = 0),
                point(2, 10.1, 20.1, segmentIndex = 1),
                point(3, 10.11, 20.11, segmentIndex = 1)
            )
        )

        assertEquals(listOf(listOf(0L, 1L), listOf(2L, 3L)), segments.map { segment -> segment.map { it.id } })
    }

    @Test
    fun mapShareUsesPrivacyZonesAndDoesNotJoinAcrossHiddenArea() {
        val visiblePoints = runMapShareablePoints(
            run = completedRun(),
            points = listOf(-900, -600, -300, 0, 300, 600, 900).mapIndexed { index, meters ->
                point(index, 0.0, meters / METERS_PER_DEGREE_LONGITUDE)
            },
            privacyZones = listOf(RunPrivacyZone(name = "Home", latitude = 0.0, longitude = 0.0, radiusMeters = 200))
        )

        assertEquals(
            listOf(listOf(0L, 1L, 2L), listOf(4L, 5L, 6L)),
            shareableRunSegments(visiblePoints).map { segment -> segment.map { it.id } }
        )
    }

    @Test
    fun mapShareHonorsHiddenDistanceAtBothActivityEnds() {
        val visiblePoints = runMapShareablePoints(
            run = completedRun(RunStartEndHideDistance.METERS_200),
            points = (0..4).map { index ->
                point(index, 0.0, index * 250.0 / METERS_PER_DEGREE_LONGITUDE)
            },
            privacyZones = emptyList()
        )

        assertEquals(listOf(1L, 2L, 3L), visiblePoints.map { it.id })
        assertNotNull(runMapShareRegion(visiblePoints))
    }

    private fun point(
        id: Int,
        latitude: Double,
        longitude: Double,
        isFiltered: Boolean = false,
        segmentIndex: Int = 0
    ) =
        RunPointModel(
            id = id.toLong(),
            runId = 1L,
            latitude = latitude,
            longitude = longitude,
            recordedAtEpochMs = id * 1_000L,
            accuracyMeters = 5f,
            segmentIndex = segmentIndex,
            isFiltered = isFiltered
        )

    private fun completedRun(hideDistance: RunStartEndHideDistance = RunStartEndHideDistance.NONE) = RunModel(
        id = 1,
        startedAtEpochMs = 0,
        finishedAtEpochMs = 5_000,
        durationMs = 5_000,
        distanceMeters = 1_000.0,
        status = RunStatus.COMPLETED,
        currentSegment = 0,
        startEndHideDistance = hideDistance
    )

    private companion object {
        const val METERS_PER_DEGREE_LONGITUDE = 111_194.9266
    }
}
