package com.example.gymtimer2

import com.example.gymtimer2.domain.model.GpsPoint
import com.example.gymtimer2.domain.model.LocalSegmentStart
import com.example.gymtimer2.domain.model.PlannedRoutePoint
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunLocalSegmentTracker
import com.example.gymtimer2.domain.model.findLocalSegmentApproach
import com.example.gymtimer2.domain.model.findLocalSegmentStart
import com.example.gymtimer2.domain.model.findLocalSegmentStartApproach
import com.example.gymtimer2.domain.model.findLocalSegmentStartApproaches
import com.example.gymtimer2.domain.model.findLocalSegmentStarts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunLocalSegmentTest {
    @Test
    fun startsTracksProgressAndFinishesAlongTheSelectedRoute() {
        val tracker = RunLocalSegmentTracker(route())

        val started = tracker.update(point(latitude = 0.0), durationMs = 0L)
        val progress = tracker.update(point(latitude = 0.001), durationMs = 10_000L)
        val completed = tracker.update(point(latitude = 0.002), durationMs = 20_000L)

        assertNotNull(started)
        assertEquals(0L, started!!.startedAtDurationMs)
        assertTrue(progress!!.progressMeters in 100.0..120.0)
        assertNull(progress.completedAtDurationMs)
        assertEquals(20_000L, completed!!.completedAtDurationMs)
        assertEquals(0L, completed.startedAtDurationMs)
    }

    @Test
    fun doesNotStartAwayFromRouteOrWithLowGpsAccuracy() {
        val tracker = RunLocalSegmentTracker(route())

        assertNull(tracker.update(point(latitude = 0.001, longitude = 0.001), durationMs = 1_000L))
        assertNull(tracker.update(point(latitude = 0.0, accuracyMeters = 80f), durationMs = 1_000L))
    }

    @Test
    fun progressDoesNotMoveBackwardsAfterTheSegmentStarts() {
        val tracker = RunLocalSegmentTracker(route())
        tracker.update(point(latitude = 0.0), durationMs = 0L)
        val previousProgress = tracker.update(point(latitude = 0.001), durationMs = 10_000L)!!.progressMeters

        assertNull(tracker.update(point(latitude = 0.0002), durationMs = 11_000L))
        val forwardProgress = tracker.update(point(latitude = 0.0015), durationMs = 12_000L)!!.progressMeters

        assertFalse(forwardProgress < previousProgress)
    }

    @Test
    fun ignoresFilteredGpsAndVeryShortRoutes() {
        val tracker = RunLocalSegmentTracker(route())
        val filteredPoint = point(latitude = 0.0).copy(isFiltered = true)
        val shortTracker = RunLocalSegmentTracker(
            listOf(PlannedRoutePoint(0.0, 0.0, 0), PlannedRoutePoint(0.0, 0.0005, 0))
        )

        assertNull(tracker.update(filteredPoint, durationMs = 0L))
        assertNull(shortTracker.update(point(latitude = 0.0), durationMs = 0L))
    }

    @Test
    fun reportsApproachOnlyForAccurateGpsNearTheRouteStart() {
        val route = route()
        val distanceMeters = findLocalSegmentApproach(runPoint(latitude = 0.001), route)

        assertNotNull(distanceMeters)
        assertTrue(distanceMeters!!.distanceToStartMeters in 100.0..120.0)
        assertNull(findLocalSegmentApproach(runPoint(latitude = 0.004), route))
        assertNull(
            findLocalSegmentApproach(runPoint(latitude = 0.001, accuracyMeters = 80f), route)
        )
        assertNull(
            findLocalSegmentApproach(runPoint(latitude = 0.001, isFiltered = true), route)
        )
        val shortRoute = listOf(
            PlannedRoutePoint(0.0, 0.0, 0),
            PlannedRoutePoint(0.0005, 0.0, 0)
        )
        assertNull(findLocalSegmentApproach(runPoint(latitude = 0.0006), shortRoute))
    }

    @Test
    fun findsNearestSavedSegmentAtItsStartAndForApproach() {
        val starts = listOf(
            LocalSegmentStart(1L, "Mais perto", 120.0, 0.0002, 0.0),
            LocalSegmentStart(2L, "Mais longe", 200.0, 0.0004, 0.0),
            LocalSegmentStart(3L, "GPX curto", 80.0, 0.0, 0.0)
        )

        assertEquals(1L, findLocalSegmentStart(point(0.0), starts)?.routeId)
        assertEquals(2L, findLocalSegmentStartApproach(runPoint(0.001), starts)?.start?.routeId)
        assertNull(findLocalSegmentStart(point(0.0, accuracyMeters = 80f), starts))
        assertNull(findLocalSegmentStartApproach(runPoint(0.004), starts))
    }

    @Test
    fun findsEveryNearbySegmentInDistanceOrder() {
        val starts = listOf(
            LocalSegmentStart(1L, "Primeiro", 120.0, 0.0002, 0.0),
            LocalSegmentStart(2L, "Segundo", 200.0, 0.0004, 0.0),
            LocalSegmentStart(3L, "Fora do raio", 160.0, 0.0008, 0.0)
        )

        assertEquals(listOf(1L, 2L), findLocalSegmentStarts(point(0.0), starts).map { it.routeId })
    }

    @Test
    fun findsEveryApproachingSegmentInDistanceOrder() {
        val starts = listOf(
            LocalSegmentStart(1L, "Mais perto", 120.0, 0.0015, 0.0),
            LocalSegmentStart(2L, "Mais longe", 200.0, 0.002, 0.0),
            LocalSegmentStart(3L, "Fora do raio", 180.0, 0.003, 0.0)
        )

        assertEquals(
            listOf(1L, 2L),
            findLocalSegmentStartApproaches(runPoint(0.0), starts).map { it.start.routeId }
        )
    }

    private fun route() = listOf(
        PlannedRoutePoint(latitude = 0.0, longitude = 0.0, segmentIndex = 0),
        PlannedRoutePoint(latitude = 0.001, longitude = 0.0, segmentIndex = 0),
        PlannedRoutePoint(latitude = 0.002, longitude = 0.0, segmentIndex = 0)
    )

    private fun point(
        latitude: Double,
        longitude: Double = 0.0,
        accuracyMeters: Float = 5f
    ) = GpsPoint(
        latitude = latitude,
        longitude = longitude,
        recordedAtEpochMs = 0L,
        accuracyMeters = accuracyMeters
    )

    private fun runPoint(
        latitude: Double,
        longitude: Double = 0.0,
        accuracyMeters: Float = 5f,
        isFiltered: Boolean = false
    ) = RunPointModel(
        id = 1L,
        runId = 1L,
        latitude = latitude,
        longitude = longitude,
        recordedAtEpochMs = 0L,
        accuracyMeters = accuracyMeters,
        segmentIndex = 0,
        isFiltered = isFiltered
    )
}
