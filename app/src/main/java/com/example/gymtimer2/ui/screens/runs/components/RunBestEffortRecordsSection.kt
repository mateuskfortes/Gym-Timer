@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.ui.screens.runs.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunBenchmarkDistance
import com.example.gymtimer2.domain.model.RunBestEffortRecord
import com.example.gymtimer2.domain.model.RunBestEffortRecords
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.screens.runs.format.RunActivityFormatting

/** Exibe o top 3 vitalício e o top 10 anual de cada distância com uma marca local. */
@Composable
internal fun RunBestEffortRecordsSection(
    records: RunBestEffortRecords,
    onSelectRun: (Long) -> Unit
) {
    val years = records.annual.keys.sortedDescending()
    if (years.isEmpty()) return

    var selectedYear by rememberSaveable(years) { mutableIntStateOf(years.first()) }
    val visibleYear = selectedYear.takeIf(years::contains) ?: years.first()
    var expandedYearMenu by remember { mutableStateOf(false) }
    var expandedDistance by remember(visibleYear) { mutableStateOf<RunBenchmarkDistance?>(null) }
    val yearRecords = records.annual[visibleYear].orEmpty()
    val distances = RunBenchmarkDistance.entries.filter { distance ->
        !records.lifetime[distance].isNullOrEmpty() || !yearRecords[distance].isNullOrEmpty()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.run_best_effort_records_title),
                style = MaterialTheme.typography.titleMedium
            )
            Box {
                TextButton(onClick = { expandedYearMenu = true }) {
                    Text(stringResource(R.string.run_best_effort_year, visibleYear))
                }
                DropdownMenu(expanded = expandedYearMenu, onDismissRequest = { expandedYearMenu = false }) {
                    years.forEach { year ->
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.run_best_effort_year, year)) },
                            onClick = {
                                selectedYear = year
                                expandedYearMenu = false
                            }
                        )
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.run_best_effort_records_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        distances.forEach { distance ->
            val lifetime = records.lifetime[distance].orEmpty()
            val annual = yearRecords[distance].orEmpty()
            val expanded = expandedDistance == distance
            Card(
                onClick = { expandedDistance = if (expanded) null else distance },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = stringResource(RunActivityFormatting.bestEffortDistanceResource(distance)),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(
                                if (expanded) R.string.run_best_effort_collapse_action
                                else R.string.run_best_effort_expand_action
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    SummaryRow(
                        label = stringResource(R.string.run_best_effort_lifetime_label),
                        record = lifetime.firstOrNull()
                    )
                    SummaryRow(
                        label = stringResource(R.string.run_best_effort_year, visibleYear),
                        record = annual.firstOrNull()
                    )
                    if (expanded) {
                        RecordList(
                            title = stringResource(R.string.run_best_effort_lifetime_label),
                            records = lifetime,
                            onSelectRun = onSelectRun
                        )
                        RecordList(
                            title = stringResource(R.string.run_best_effort_year, visibleYear),
                            records = annual,
                            onSelectRun = onSelectRun
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, record: RunBestEffortRecord?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = record?.let { RunMetrics.formatDuration(it.durationMs) }
                ?: stringResource(R.string.run_best_effort_no_time),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun RecordList(
    title: String,
    records: List<RunBestEffortRecord>,
    onSelectRun: (Long) -> Unit
) {
    if (records.isEmpty()) return
    Text(title, style = MaterialTheme.typography.labelLarge)
    records.forEachIndexed { index, record ->
        TextButton(onClick = { onSelectRun(record.runId) }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(
                    R.string.run_best_effort_record_row,
                    index + 1,
                    RunMetrics.formatDuration(record.durationMs),
                    RunActivityFormatting.formatDate(record.startedAtEpochMs)
                ),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
