package com.example.gymtimer2.ui.screens.runs.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.domain.model.RunTrainingLogDay
import com.example.gymtimer2.domain.model.RunTrainingLogMetric
import com.example.gymtimer2.domain.model.RunTrainingLogWeek
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
internal fun RunTrainingLogWeekRow(
    week: RunTrainingLogWeek,
    onSelectDay: ((RunTrainingLogDay) -> Unit)? = null,
    metric: RunTrainingLogMetric = RunTrainingLogMetric.DISTANCE,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = weekRangeLabel(week.startEpochMs),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = when (metric) {
                        RunTrainingLogMetric.DISTANCE -> RunFormatting.distanceLabel(week.distanceMeters, fractionDigits = 1)
                        RunTrainingLogMetric.MOVING_TIME -> RunMetrics.formatDuration(week.movingTimeMs)
                    },
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Row(Modifier.fillMaxWidth()) {
                week.days.forEach { day ->
                    RunTrainingLogDayCell(day, onSelectDay, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RunTrainingLogDayCell(
    day: RunTrainingLogDay,
    onSelectDay: ((RunTrainingLogDay) -> Unit)?,
    modifier: Modifier
) {
    val context = LocalContext.current
    val date = Date(day.dateEpochMs)
    val label = date.formatDate("EEEE, d MMMM")
    val description = when {
        day.runs.isEmpty() -> context.getString(R.string.run_training_log_day_empty, label)
        day.runs.size == 1 -> context.getString(R.string.run_training_log_day_one_activity, label)
        else -> context.getString(R.string.run_training_log_day_activities, label, day.runs.size)
    }
    val isToday = day.dateEpochMs == todayStartEpochMs()
    val cellModifier = if (onSelectDay != null && day.runs.isNotEmpty()) {
        modifier.clickable { onSelectDay(day) }
            .semantics { contentDescription = description; role = Role.Button }
    } else {
        modifier.semantics { contentDescription = description }
    }

    Column(
        modifier = cellModifier.padding(horizontal = 1.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(date.formatDate("EEE").replace(".", "").take(3), style = MaterialTheme.typography.labelSmall)
        Surface(
            modifier = Modifier.size(30.dp),
            shape = CircleShape,
            color = if (day.runs.isEmpty()) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.primary,
            border = if (isToday) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = date.formatDate("d"),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.runs.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Text(
            text = day.runs.size.takeIf { it > 1 }?.toString().orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun weekRangeLabel(startEpochMs: Long): String {
    val start = Calendar.getInstance().apply { timeInMillis = startEpochMs }
    val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 6) }
    val sameYear = start.get(Calendar.YEAR) == end.get(Calendar.YEAR)
    val startLabel = Date(startEpochMs).formatDate(if (sameYear) "d MMM" else "d MMM yyyy")
    val endLabel = Date(end.timeInMillis).formatDate("d MMM yyyy")
    return stringResource(R.string.run_training_log_week_range, startLabel, endLabel)
}

private fun Date.formatDate(pattern: String): String =
    SimpleDateFormat(pattern, TRAINING_LOG_LOCALE).format(this)

private fun todayStartEpochMs(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private val TRAINING_LOG_LOCALE = Locale.forLanguageTag("pt-BR")
