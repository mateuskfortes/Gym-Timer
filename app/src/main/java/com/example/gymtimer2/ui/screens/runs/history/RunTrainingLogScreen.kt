package com.example.gymtimer2.ui.screens.runs.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunTrainingLogDay
import com.example.gymtimer2.domain.model.RunTrainingLogMetric
import com.example.gymtimer2.domain.model.buildRunTrainingLogWeeks
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.screens.runs.format.RunActivityFormatting
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun RunTrainingLogScreen(
    runs: List<RunModel>,
    onBack: () -> Unit,
    onSelectRun: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val weeks = remember(runs) { buildRunTrainingLogWeeks(runs) }
    var selectedDay by remember { mutableStateOf<RunTrainingLogDay?>(null) }
    var metric by remember { mutableStateOf(RunTrainingLogMetric.DISTANCE) }
    BackHandler(onBack = onBack)

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Text(stringResource(R.string.run_back_action), modifier = Modifier.padding(start = 6.dp))
            }
            Text(
                text = stringResource(R.string.run_training_log_title),
                style = MaterialTheme.typography.titleLarge
            )
        }
        if (weeks.isEmpty()) {
            Text(
                text = stringResource(R.string.run_training_log_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = metric == RunTrainingLogMetric.DISTANCE,
                    onClick = { metric = RunTrainingLogMetric.DISTANCE },
                    label = { Text(stringResource(R.string.run_training_log_metric_distance)) }
                )
                FilterChip(
                    selected = metric == RunTrainingLogMetric.MOVING_TIME,
                    onClick = { metric = RunTrainingLogMetric.MOVING_TIME },
                    label = { Text(stringResource(R.string.run_training_log_metric_time)) }
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(weeks, key = { it.startEpochMs }) { week ->
                    RunTrainingLogWeekRow(
                        week = week,
                        onSelectDay = { day ->
                            if (day.runs.size == 1) onSelectRun(day.runs.first().id)
                            else selectedDay = day
                        },
                        metric = metric
                    )
                }
            }
        }
    }

    selectedDay?.let { day ->
        RunTrainingLogActivityDialog(
            day = day,
            onDismiss = { selectedDay = null },
            onSelectRun = { run ->
                selectedDay = null
                onSelectRun(run.id)
            }
        )
    }
}

@Composable
private fun RunTrainingLogActivityDialog(
    day: RunTrainingLogDay,
    onDismiss: () -> Unit,
    onSelectRun: (RunModel) -> Unit
) {
    val dateLabel = SimpleDateFormat("EEEE, d MMMM", TRAINING_LOG_LOCALE).format(Date(day.dateEpochMs))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.run_training_log_day_dialog_title, dateLabel)) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(day.runs, key = RunModel::id) { run ->
                    TextButton(onClick = { onSelectRun(run) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(RunActivityFormatting.displayTitle(run), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = stringResource(
                                    R.string.run_training_log_activity_summary,
                                    RunActivityFormatting.activityTypeLabel(run.activityType),
                                    RunFormatting.distanceLabel(run.distanceMeters, fractionDigits = 1)
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
        }
    )
}

private val TRAINING_LOG_LOCALE = Locale.forLanguageTag("pt-BR")
