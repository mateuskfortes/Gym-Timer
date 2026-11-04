package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.PlannedRoutePoint
import com.example.gymtimer2.domain.model.distanceFromRunRouteMeters
import com.example.gymtimer2.domain.model.isOffRunRoute
import com.example.gymtimer2.domain.model.isOffRunRoutePosition
import com.example.gymtimer2.domain.model.isRunRouteStartFarFromLocation
import com.example.gymtimer2.domain.model.runRoutePosition
import com.example.gymtimer2.domain.model.RunRouteAlertState
import com.example.gymtimer2.domain.model.hasRecordedBacktrack
import com.example.gymtimer2.domain.model.routeBackToRunStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Confere distância à rota e evita criar ligações entre trechos separados. */
class RunRouteNavigationTest {
    @Test
    fun pointOnSegmentIsNearRouteAndDistantPointRaisesAlert() {
        val route = listOf(point(1L, 0.0), point(2L, 0.001))

        assertTrue(distanceFromRunRouteMeters(point(3L, 0.0005, 0.00002), route)!! < 3.0)
        assertTrue(isOffRunRoute(point(4L, 0.0005, 0.001), route))
        assertFalse(isOffRunRoute(point(5L, 0.0005, 0.00002), route))
    }

    @Test
    fun coordinateBasedAlertMatchesTheSavedPointThreshold() {
        val route = listOf(point(1L, 0.0), point(2L, 0.001))

        assertTrue(isOffRunRoute(0.0005, 0.001, route))
        assertFalse(isOffRunRoute(0.0005, 0.00002, route))
    }

    @Test
    fun projectedRoutePositionUsesTheSameDeviationThreshold() {
        val route = listOf(point(1L, 0.0), point(2L, 0.001))
        val nearPosition = runRoutePosition(0.0005, 0.00002, route)
        val farPosition = runRoutePosition(0.0005, 0.001, route)

        assertFalse(isOffRunRoutePosition(nearPosition))
        assertTrue(isOffRunRoutePosition(farPosition))
    }

    @Test
    fun distantRouteWarningUsesTheFirstAcceptedRoutePoint() {
        val route = listOf(point(1L, 3.0), point(2L, 0.0))

        assertTrue(isRunRouteStartFarFromLocation(0.0, 0.0, route))
    }

    @Test
    fun routeWithinTwoHundredFiftyKilometersDoesNotWarn() {
        val route = listOf(point(1L, 2.0), point(2L, 0.0))

        assertFalse(isRunRouteStartFarFromLocation(0.0, 0.0, route))
    }

    @Test
    fun routeDistanceWarningIgnoresFilteredOrInvalidCoordinates() {
        val route = listOf(
            point(1L, 3.0).copy(isFiltered = true),
            point(2L, 0.0)
        )

        assertFalse(isRunRouteStartFarFromLocation(0.0, 0.0, route))
        assertFalse(isRunRouteStartFarFromLocation(100.0, 0.0, route))
        assertFalse(isRunRouteStartFarFromLocation(0.0, 0.0, emptyList()))
    }

    @Test
    fun offRouteCueRepeatsOnlyAfterReturningToTheRoute() {
        val alertState = RunRouteAlertState()

        assertFalse(alertState.update(false))
        assertTrue(alertState.update(true))
        assertFalse(alertState.update(true))
        assertFalse(alertState.update(false))
        assertTrue(alertState.update(true))
    }

    @Test
    fun muteOnceSuppressesAlertsUntilReturningToTheRoute() {
        val alertState = RunRouteAlertState()

        assertTrue(alertState.update(true))
        alertState.muteUntilRouteReturn()
        assertTrue(alertState.isMutedUntilReturn)
        assertFalse(alertState.update(true))
        assertFalse(alertState.update(false))
        assertFalse(alertState.isMutedUntilReturn)
        assertTrue(alertState.update(true))
    }

    @Test
    fun muteOnceSurvivesRunResumeUntilRouteReturn() {
        val alertState = RunRouteAlertState()

        alertState.update(true)
        alertState.muteUntilRouteReturn()
        alertState.resetForResume()

        assertFalse(alertState.update(true))
        assertTrue(alertState.isMutedUntilReturn)
    }

    @Test
    fun routeSegmentsAreNotConnectedAcrossPauses() {
        val route = listOf(
            point(1L, 0.0, segment = 0),
            point(2L, 0.001, segment = 0),
            point(3L, 0.1, segment = 1),
            point(4L, 0.101, segment = 1)
        )

        assertTrue(distanceFromRunRouteMeters(point(5L, 0.05), route)!! > 5_000.0)
    }

    @Test
    fun returnPathReversesOrderAndKeepsPausedSegmentsSeparate() {
        val route = listOf(
            point(1L, 0.0, segment = 0),
            point(2L, 0.001, segment = 0),
            point(3L, 0.1, segment = 1),
            point(4L, 0.101, segment = 1)
        )

        assertEquals(
            listOf(
                PlannedRoutePoint(0.101, 0.0, 0),
                PlannedRoutePoint(0.1, 0.0, 0),
                PlannedRoutePoint(0.001, 0.0, 1),
                PlannedRoutePoint(0.0, 0.0, 1)
            ),
            routeBackToRunStart(route)
        )
    }

    @Test
    fun returnPathNeedsTwoRecordedPoints() {
        assertTrue(routeBackToRunStart(listOf(point(1L, 0.0))).isEmpty())
    }

    @Test
    fun separateSinglePointSegmentsDoNotCreateAReturnLine() {
        val route = listOf(point(1L, 0.0, segment = 0), point(2L, 0.1, segment = 1))

        assertFalse(hasRecordedBacktrack(route))
        assertTrue(routeBackToRunStart(route).isEmpty())
    }

    @Test
    fun filteredPointsDoNotExtendTheRecordedRoute() {
        val acceptedStart = point(1L, 0.0)
        val filteredJump = point(2L, 0.1).copy(isFiltered = true)

        assertTrue(routeBackToRunStart(listOf(acceptedStart, filteredJump)).isEmpty())
        assertFalse(isOffRunRoute(filteredJump, listOf(acceptedStart, point(3L, 0.001))))
    }

    private fun point(id: Long, latitude: Double, longitude: Double = 0.0, segment: Int = 0) =
        RunPointModel(
            id = id,
            runId = 1L,
            latitude = latitude,
            longitude = longitude,
            recordedAtEpochMs = 1L,
            accuracyMeters = 4f,
            segmentIndex = segment
        )
}
