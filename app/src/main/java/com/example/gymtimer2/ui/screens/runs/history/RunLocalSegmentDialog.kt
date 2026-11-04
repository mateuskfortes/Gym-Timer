package com.example.gymtimer2.ui.screens.runs.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunLocalSegmentSelection
import com.example.gymtimer2.domain.model.RunLocalSegmentTrack
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.runRouteLengthMeters
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.screens.runs.format.RunActivityFormatting
import com.example.gymtimer2.ui.screens.runs.map.RunMap
import kotlinx.coroutines.launch

@Composable
internal fun RunLocalSegmentDialog(
    run: RunModel,
    tracks: List<RunLocalSegmentTrack>,
    onDismiss: () -> Unit,
    onSave: suspend (Long, String, Int, Double, Double) -> Boolean,
    onSaved: () -> Unit
) {
    val initialName = stringResource(R.string.run_local_segment_create_default_name, RunActivityFormatting.displayTitle(run))
    var name by rememberSaveable(run.id) { mutableStateOf(initialName.take(MAX_SEGMENT_NAME_LENGTH)) }
    var selectedSegmentIndex by rememberSaveable(run.id) {
        mutableIntStateOf(tracks.first().segmentIndex)
    }
    val selectedTrack = tracks.firstOrNull { it.segmentIndex == selectedSegmentIndex } ?: tracks.first()
    var startDistanceMeters by rememberSaveable(run.id, selectedTrack.segmentIndex) {
        mutableFloatStateOf(0f)
    }
    var endDistanceMeters by rememberSaveable(run.id, selectedTrack.segmentIndex) {
        mutableFloatStateOf(selectedTrack.distanceMeters.toFloat())
    }
    var isSaving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val selectedPoints = remember(selectedTrack, startDistanceMeters, endDistanceMeters) {
        RunLocalSegmentSelection.pointsBetween(
            selectedTrack,
            startDistanceMeters.toDouble(),
            endDistanceMeters.toDouble()
        )
    }
    val selectedDistanceMeters = remember(selectedPoints) { runRouteLengthMeters(selectedPoints) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(stringResource(R.string.run_local_segment_dialog_title)) },
        text = {
            Column(
                Modifier.heightIn(max = MAX_DIALOG_HEIGHT_DP.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                TextField(
                    value = name,
                    onValueChange = { name = it.take(MAX_SEGMENT_NAME_LENGTH) },
                    label = { Text(stringResource(R.string.run_local_segment_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                RunLocalSegmentTrackPicker(tracks, selectedTrack) {
                    selectedSegmentIndex = it.segmentIndex
                }
                RunLocalSegmentMapPreview(selectedPoints)
                Text(
                    RunFormatting.distanceLabel(selectedDistanceMeters),
                    modifier = Modifier.padding(top = 8.dp)
                )
                RunLocalSegmentRangeControl(
                    track = selectedTrack,
                    startDistanceMeters = startDistanceMeters,
                    endDistanceMeters = endDistanceMeters,
                    onRangeChanged = { range ->
                        startDistanceMeters = range.start
                        endDistanceMeters = range.endInclusive
                        saveFailed = false
                    }
                )
                RunLocalSegmentValidationMessage(selectedPoints.size, saveFailed)
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSaveLocalSegment(name, isSaving, selectedPoints.size),
                onClick = {
                    scope.launch {
                        isSaving = true
                        saveFailed = false
                        try {
                            val saved = onSave(
                                run.id,
                                name.trim(),
                                selectedTrack.segmentIndex,
                                startDistanceMeters.toDouble(),
                                endDistanceMeters.toDouble()
                            )
                            saveFailed = !saved
                            if (saved) {
                                onSaved()
                                onDismiss()
                            }
                        } finally {
                            isSaving = false
                        }
                    }
                }
            ) {
                Text(stringResource(if (isSaving) R.string.run_local_segment_saving else R.string.run_local_segment_save_action))
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}

@Composable
private fun RunLocalSegmentTrackPicker(
    tracks: List<RunLocalSegmentTrack>,
    selectedTrack: RunLocalSegmentTrack,
    onSelectTrack: (RunLocalSegmentTrack) -> Unit
) {
    if (tracks.size <= 1) return
    var isExpanded by remember { mutableStateOf(false) }
    val selectedTrackNumber = tracks.indexOf(selectedTrack) + 1
    TextButton(onClick = { isExpanded = true }) {
        Text(stringResource(R.string.run_local_segment_track_option, selectedTrackNumber))
    }
    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = { isExpanded = false }
    ) {
        tracks.forEachIndexed { index, track ->
            DropdownMenuItem(
                text = { Text(stringResource(R.string.run_local_segment_track_option, index + 1)) },
                onClick = {
                    onSelectTrack(track)
                    isExpanded = false
                }
            )
        }
    }
}

@Composable
private fun RunLocalSegmentMapPreview(points: List<RunPointModel>) {
    if (points.size < MIN_SEGMENT_POINT_COUNT) return
    RunMap(
        points = points,
        followLatestPoint = false,
        useTextureView = true,
        modifier = Modifier.fillMaxWidth().height(MAP_PREVIEW_HEIGHT_DP.dp)
    )
}

@Composable
private fun RunLocalSegmentRangeControl(
    track: RunLocalSegmentTrack,
    startDistanceMeters: Float,
    endDistanceMeters: Float,
    onRangeChanged: (ClosedFloatingPointRange<Float>) -> Unit
) {
    Text(
        stringResource(R.string.run_local_segment_range_label),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 12.dp)
    )
    RangeSlider(
        value = startDistanceMeters..endDistanceMeters,
        onValueChange = onRangeChanged,
        valueRange = 0f..track.distanceMeters.toFloat()
    )
}

@Composable
private fun RunLocalSegmentValidationMessage(pointCount: Int, saveFailed: Boolean) {
    if (pointCount < MIN_SEGMENT_POINT_COUNT) {
        Text(
            stringResource(R.string.run_local_segment_minimum_distance),
            color = MaterialTheme.colorScheme.error
        )
    }
    if (saveFailed) {
        Text(
            stringResource(R.string.run_local_segment_save_error),
            color = MaterialTheme.colorScheme.error
        )
    }
}

private fun canSaveLocalSegment(name: String, isSaving: Boolean, pointCount: Int): Boolean =
    !isSaving && name.isNotBlank() && pointCount >= MIN_SEGMENT_POINT_COUNT

private const val MIN_SEGMENT_POINT_COUNT = 2
private const val MAX_SEGMENT_NAME_LENGTH = 200
private const val MAP_PREVIEW_HEIGHT_DP = 190
private const val MAX_DIALOG_HEIGHT_DP = 520
