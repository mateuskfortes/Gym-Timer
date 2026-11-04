package com.example.gymtimer2.ui.screens.runs.history

import android.content.Context
import android.content.Intent
import com.example.gymtimer2.R
import com.example.gymtimer2.data.exporting.RunShareCardStorage
import com.example.gymtimer2.domain.model.RunDistanceUnit
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.ui.screens.runs.components.RunShareCardColors
import com.example.gymtimer2.ui.screens.runs.components.RunSummaryShareCardContent
import com.example.gymtimer2.ui.screens.runs.components.RunSummaryShareCardRenderer
import com.example.gymtimer2.ui.screens.runs.components.RunSummaryShareMetric
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.screens.runs.format.RunActivityFormatting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun shareRunSummary(
    context: Context,
    run: RunModel,
    distanceUnit: RunDistanceUnit,
    colors: RunShareCardColors
) {
    val content = runSummaryShareCardContent(context, run, distanceUnit)
    val title = context.getString(R.string.run_share_summary_title)
    val uri = withContext(Dispatchers.IO) {
        val bitmap = RunSummaryShareCardRenderer.render(content, colors)
        try {
            RunShareCardStorage(context).save(bitmap)
        } finally {
            bitmap.recycle()
        }
    }
    val intent = uri?.let {
        createRunImageShareIntent(context, listOf(it), context.getString(R.string.run_share_summary_action))
    } ?: Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, formatRunSummaryShareText(content))
    }
    context.startActivity(Intent.createChooser(intent, title))
}

internal fun formatRunSummaryShareText(content: RunSummaryShareCardContent): String = buildString {
    appendLine(content.activityTitle)
    appendLine(content.activityDate)
    content.metrics.forEach { metric -> appendLine("${metric.label}: ${metric.value}") }
    append(content.footer)
}

private fun runSummaryShareCardContent(
    context: Context,
    run: RunModel,
    distanceUnit: RunDistanceUnit
) = RunSummaryShareCardContent(
    appName = context.getString(R.string.app_name),
    activityTitle = run.title ?: context.getString(RunActivityFormatting.activityTypeResource(run.activityType)),
    activityDate = RunActivityFormatting.formatDate(run.startedAtEpochMs),
    metrics = listOf(
        RunSummaryShareMetric(
            context.getString(R.string.run_distance_label),
            RunFormatting.distanceLabel(context, run.distanceMeters, distanceUnit)
        ),
        RunSummaryShareMetric(
            context.getString(R.string.run_time_label),
            RunMetrics.formatDuration(run.performanceDurationMs)
        ),
        RunSummaryShareMetric(
            context.getString(R.string.run_pace_label),
            RunMetrics.formatPace(run.averagePaceSecondsPerKm, distanceUnit)
        ),
        RunSummaryShareMetric(
            context.getString(R.string.run_average_speed_label),
            run.averageSpeedKmPerHour?.let { RunFormatting.speedLabel(context, it, distanceUnit) }
                ?: context.getString(R.string.run_metric_unavailable)
        )
    ),
    footer = context.getString(R.string.run_share_summary_footer)
)
