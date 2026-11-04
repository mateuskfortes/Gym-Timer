package com.example.gymtimer2

import com.example.gymtimer2.domain.model.PlannedRouteModel
import com.example.gymtimer2.domain.model.RunLocalSegmentEffort
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.ui.screens.runs.RunLiveSegmentPresenter
import com.example.gymtimer2.ui.screens.runs.RunLocalSegmentUiData
import com.example.gymtimer2.ui.screens.runs.RunLiveSegment
import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifica os dados de progresso e comparação dos segmentos locais ao vivo. */
class RunLiveSegmentPresenterTest {
    @Test
    fun presentsActiveEffortWithBestLocalTime() {
        val run = RunModel(
            id = 1L,
            startedAtEpochMs = 1_000L,
            finishedAtEpochMs = null,
            durationMs = 30_000L,
            distanceMeters = 0.0,
            status = RunStatus.RUNNING,
            currentSegment = 0
        )
        val data = RunLocalSegmentUiData(
            efforts = listOf(RunLocalSegmentEffort(2L, 50.0, 10_000L, null)),
            routes = emptyMap(),
            starts = emptyList(),
            completedEfforts = listOf(RunLocalSegmentEffort(2L, 100.0, 0L, 50_000L))
        )

        val result = RunLiveSegmentPresenter.build(
            run = run,
            selectedRoute = emptyList(),
            selectedElevationProfile = emptyList(),
            completedRuns = emptyList(),
            plannedRoutes = listOf(PlannedRouteModel(2L, "Pista", 100.0)),
            segmentData = data,
            latestPoint = null,
            canShowApproach = false
        )

        assertEquals(
            listOf(
                RunLiveSegment(
                    name = "Pista",
                    progressMeters = 50.0,
                    distanceMeters = 100.0,
                    elapsedDurationMs = 20_000L,
                    bestDurationMs = 50_000L,
                    differenceFromBestMs = 5_000L,
                    isCompleted = false,
                    isNewRecord = false,
                    routeId = 2L
                )
            ),
            result
        )
    }
}
