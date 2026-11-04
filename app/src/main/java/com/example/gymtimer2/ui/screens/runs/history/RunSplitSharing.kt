package com.example.gymtimer2.ui.screens.runs.history

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.gymtimer2.R
import com.example.gymtimer2.data.exporting.RunShareCardStorage
import com.example.gymtimer2.domain.model.RunDistanceUnit
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunSplit
import com.example.gymtimer2.ui.screens.runs.components.RunShareCardColors
import com.example.gymtimer2.ui.screens.runs.components.RunSplitShareCardContent
import com.example.gymtimer2.ui.screens.runs.components.RunSplitShareCardRenderer
import com.example.gymtimer2.ui.screens.runs.components.RunSplitShareCardRow
import com.example.gymtimer2.ui.screens.runs.components.runSplitPaceBarWidthRatios
import com.example.gymtimer2.ui.screens.runs.components.runSplitSharePages
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.screens.runs.format.RunActivityFormatting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun shareRunSplits(
    context: Context,
    run: RunModel,
    splits: List<RunSplit>,
    distanceUnit: RunDistanceUnit,
    colors: RunShareCardColors
) {
    val pages = runSplitSharePages(splits)
    if (pages.isEmpty()) return
    val shareTitle = context.getString(R.string.run_share_splits_title, distanceUnitLabel(context, distanceUnit))
    val fallbackText = formatRunSplitsShareText(context, splits, distanceUnit, shareTitle)
    val imageUris = createShareImages(context, run, pages, distanceUnit, colors)
    val shareIntent = if (imageUris.size == pages.size) {
        createRunImageShareIntent(context, imageUris, context.getString(R.string.run_share_splits_action))
    } else {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, fallbackText)
        }
    }
    context.startActivity(Intent.createChooser(shareIntent, shareTitle))
}

private suspend fun createShareImages(
    context: Context,
    run: RunModel,
    pages: List<List<RunSplit>>,
    distanceUnit: RunDistanceUnit,
    colors: RunShareCardColors
): List<Uri> = withContext(Dispatchers.IO) {
    val storage = RunShareCardStorage(context)
    pages.mapIndexedNotNull { pageIndex, page ->
        val bitmap = RunSplitShareCardRenderer.render(
            createShareCardContent(context, run, page, pageIndex + 1, pages.size, distanceUnit),
            colors
        )
        try {
            storage.save(bitmap)
        } finally {
            bitmap.recycle()
        }
    }
}

private fun createShareCardContent(
    context: Context,
    run: RunModel,
    splits: List<RunSplit>,
    pageNumber: Int,
    pageCount: Int,
    distanceUnit: RunDistanceUnit
): RunSplitShareCardContent {
    val unitLabel = distanceUnitLabel(context, distanceUnit)
    val splitUnitLabel = context.getString(
        if (distanceUnit == RunDistanceUnit.METRIC) R.string.run_unit_kilometers else R.string.run_unit_miles
    )
    val ratios = runSplitPaceBarWidthRatios(splits)
    return RunSplitShareCardContent(
        appName = context.getString(R.string.app_name),
        activityTitle = run.title ?: context.getString(RunActivityFormatting.activityTypeResource(run.activityType)),
        activityDate = RunActivityFormatting.formatDate(run.startedAtEpochMs),
        distanceLabel = context.getString(R.string.run_distance_label),
        distance = RunFormatting.distanceLabel(context, run.distanceMeters, distanceUnit),
        durationLabel = context.getString(R.string.run_time_label),
        duration = RunMetrics.formatDuration(run.performanceDurationMs),
        paceLabel = context.getString(R.string.run_pace_label),
        pace = RunMetrics.formatPace(run.averagePaceSecondsPerKm, distanceUnit),
        splitsTitle = context.getString(R.string.run_splits_title, unitLabel),
        pageLabel = context.getString(R.string.run_share_splits_page, pageNumber, pageCount),
        estimateNote = context.getString(R.string.run_splits_estimate),
        rows = splits.mapIndexed { index, split ->
            RunSplitShareCardRow(
                label = context.getString(R.string.run_split_distance, split.number, splitUnitLabel),
                pace = RunMetrics.formatDuration(split.durationMs),
                barWidthRatio = ratios[index]
            )
        }
    )
}

private fun formatRunSplitsShareText(
    context: Context,
    splits: List<RunSplit>,
    distanceUnit: RunDistanceUnit,
    title: String
): String {
    val unitLabel = distanceUnitLabel(context, distanceUnit)
    val rows = splits.map { split ->
        context.getString(R.string.run_split_share_row, split.number, unitLabel, RunMetrics.formatDuration(split.durationMs))
    }
    return formatRunSplitsShareText(
        title = title,
        rows = rows,
        estimateNote = context.getString(R.string.run_splits_estimate)
    )
}

internal fun formatRunSplitsShareText(title: String, rows: List<String>, estimateNote: String): String =
    buildString {
        appendLine(title)
        rows.forEach { appendLine(it) }
        append(estimateNote)
    }

private fun distanceUnitLabel(context: Context, distanceUnit: RunDistanceUnit): String = context.getString(
    if (distanceUnit == RunDistanceUnit.METRIC) R.string.run_unit_kilometers else R.string.run_unit_miles
)
