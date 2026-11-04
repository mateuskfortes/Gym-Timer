package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.domain.model.RunLap
import com.example.gymtimer2.domain.model.RunLapMetrics
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.domain.model.RunWorkoutStep
import com.example.gymtimer2.ui.screens.runs.RunLiveSegment
import com.example.gymtimer2.ui.screens.runs.components.RunElevationMetric
import com.example.gymtimer2.ui.screens.runs.components.RunMetric
import com.example.gymtimer2.ui.screens.runs.components.RunMetricsRow
import com.example.gymtimer2.ui.screens.runs.components.RunCurrentLapMetrics
import com.example.gymtimer2.ui.screens.runs.components.RunLocalSegmentCard
import com.example.gymtimer2.ui.screens.runs.components.RunWorkoutSection
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting

/** Navega por instruções do treino, segmentos locais e métricas durante a corrida. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RunStatsPager(
    run: RunModel,
    points: List<RunPointModel>,
    laps: List<RunLap>,
    workoutSteps: List<RunWorkoutStep>,
    liveSegments: List<RunLiveSegment>,
    onAdvanceWorkout: () -> Unit,
    showLapPage: Boolean,
    currentPaceSecondsPerKm: Double?,
    currentCadenceStepsPerMinute: Int?,
    currentSpeedKmPerHour: Double?,
    modifier: Modifier = Modifier
) {
    val showWorkoutPage = workoutSteps.isNotEmpty()
    val showLiveSegmentsPage = liveSegments.isNotEmpty()
    val pages = remember(showLapPage, showWorkoutPage, showLiveSegmentsPage) {
        runStatsPages(showLapPage, showWorkoutPage, showLiveSegmentsPage)
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val previousPages = remember { mutableStateOf(pages) }
    val activeSegmentIds = runStatsLiveSegmentIds(liveSegments)
    val previousActiveSegmentIds = remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(pages, activeSegmentIds) {
        val hasNewLiveSegment = activeSegmentIds.any { it !in previousActiveSegmentIds.value }
        val targetPage = runStatsPageIndexAfterChange(
            previousPages.value,
            pages,
            pagerState.currentPage,
            hasNewLiveSegment
        )
        if (targetPage != pagerState.currentPage) {
            if (hasNewLiveSegment) pagerState.animateScrollToPage(targetPage)
            else pagerState.scrollToPage(targetPage)
        }
        previousPages.value = pages
        previousActiveSegmentIds.value = activeSegmentIds
    }
    val currentLap = RunMetrics.currentLapMetrics(run.durationMs, run.distanceMeters, laps)
    Column(modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().testTag("run-stats-pager"),
            pageSpacing = 8.dp
        ) { pageIndex ->
            RunStatsPageContent(
                page = pages[pageIndex],
                run = run,
                points = points,
                currentLap = currentLap,
                workoutSteps = workoutSteps,
                liveSegments = liveSegments,
                onAdvanceWorkout = onAdvanceWorkout,
                currentPaceSecondsPerKm = currentPaceSecondsPerKm,
                currentCadenceStepsPerMinute = currentCadenceStepsPerMinute,
                currentSpeedKmPerHour = currentSpeedKmPerHour
            )
        }
        Text(
            text = stringResource(R.string.run_stats_page_position, pagerState.currentPage + 1, pages.size),
            modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RunStatsPageContent(
    page: RunStatsPage,
    run: RunModel,
    points: List<RunPointModel>,
    currentLap: RunLapMetrics?,
    workoutSteps: List<RunWorkoutStep>,
    liveSegments: List<RunLiveSegment>,
    onAdvanceWorkout: () -> Unit,
    currentPaceSecondsPerKm: Double?,
    currentCadenceStepsPerMinute: Int?,
    currentSpeedKmPerHour: Double?
) {
    when (page) {
        RunStatsPage.WORKOUT -> RunWorkoutSection(
            title = run.workoutTitle,
            steps = workoutSteps,
            isRunning = run.status == RunStatus.RUNNING,
            currentPaceSecondsPerKm = currentPaceSecondsPerKm,
            onAdvance = onAdvanceWorkout
        )
        RunStatsPage.LIVE_SEGMENTS -> RunStatsLiveSegmentsPage(liveSegments)
        RunStatsPage.OVERVIEW -> RunStatsOverviewPage(run)
        RunStatsPage.DETAILS -> RunStatsDetailsPage(
            run,
            points,
            currentPaceSecondsPerKm,
            currentCadenceStepsPerMinute,
            currentSpeedKmPerHour
        )
        RunStatsPage.LAP_STATS -> currentLap?.let { RunCurrentLapMetrics(it) }
    }
}

@Composable
private fun RunStatsLiveSegmentsPage(segments: List<RunLiveSegment>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.run_stats_live_segments_title),
            style = MaterialTheme.typography.titleSmall
        )
        segments.forEach { RunLocalSegmentCard(it) }
    }
}

@Composable
private fun RunStatsOverviewPage(run: RunModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.run_stats_overview_title), style = MaterialTheme.typography.titleSmall)
        RunMetricsRow(run)
    }
}

@Composable
private fun RunStatsDetailsPage(
    run: RunModel,
    points: List<RunPointModel>,
    currentPaceSecondsPerKm: Double?,
    currentCadenceStepsPerMinute: Int?,
    currentSpeedKmPerHour: Double?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.run_stats_details_title), style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RunMetric(
                label = stringResource(R.string.run_current_pace_label),
                value = RunFormatting.pace(currentPaceSecondsPerKm),
                modifier = Modifier.weight(1f)
            )
            RunMetric(
                label = stringResource(R.string.run_current_cadence_label),
                value = currentCadenceStepsPerMinute?.let { stringResource(R.string.run_cadence_value, it) } ?: "--",
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RunMetric(
                label = stringResource(R.string.run_average_speed_label),
                value = run.averageSpeedKmPerHour?.let { RunFormatting.speedLabel(it) } ?: "--",
                modifier = Modifier.weight(1f)
            )
            RunMetric(
                label = stringResource(R.string.run_current_speed_label),
                value = currentSpeedKmPerHour?.let { RunFormatting.speedLabel(it) } ?: "--",
                modifier = Modifier.weight(1f)
            )
        }
        RunElevationMetric(points)
    }
}

internal enum class RunStatsPage { WORKOUT, LIVE_SEGMENTS, OVERVIEW, DETAILS, LAP_STATS }

internal fun runStatsPages(
    showLapPage: Boolean,
    showWorkoutPage: Boolean,
    showLiveSegmentsPage: Boolean = false
): List<RunStatsPage> = buildList {
    if (showWorkoutPage) add(RunStatsPage.WORKOUT)
    if (showLiveSegmentsPage) add(RunStatsPage.LIVE_SEGMENTS)
    add(RunStatsPage.OVERVIEW)
    add(RunStatsPage.DETAILS)
    if (showLapPage) add(RunStatsPage.LAP_STATS)
}

internal fun runStatsPageIndexAfterChange(
    previousPages: List<RunStatsPage>,
    pages: List<RunStatsPage>,
    currentPage: Int,
    hasNewLiveSegment: Boolean = false
): Int = pages.indexOf(RunStatsPage.LIVE_SEGMENTS).takeIf { hasNewLiveSegment && it >= 0 }
    ?: previousPages.getOrNull(currentPage)?.let(pages::indexOf)?.takeIf { it >= 0 }
    ?: currentPage.coerceAtMost(pages.lastIndex)

/** Identifica tentativas em andamento sem usar o progresso, que muda em cada leitura GPS. */
internal fun runStatsLiveSegmentIds(segments: List<RunLiveSegment>): Set<String> = segments
    .asSequence()
    .filter { it.distanceToStartMeters == null && !it.isCompleted }
    .mapNotNull { segment ->
        segment.routeId?.let { "route:$it" }
            ?: segment.name?.let { "name:$it" }
            ?: segment.distanceMeters.takeIf { it > 0.0 }?.let { "distance:${it.toBits()}" }
    }
    .toSet()

/** Abre o painel ao iniciar uma disputa, mantendo o mapa em tela cheia sob controle do usuário. */
internal fun shouldExpandRunSheetForNewSegment(
    previousSegmentIds: Set<String>,
    activeSegmentIds: Set<String>,
    isMapFullscreen: Boolean,
    isSheetExpanded: Boolean
): Boolean = !isMapFullscreen && !isSheetExpanded && activeSegmentIds.any { it !in previousSegmentIds }
