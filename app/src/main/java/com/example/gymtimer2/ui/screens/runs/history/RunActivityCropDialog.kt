package com.example.gymtimer2.ui.screens.runs.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunActivityCrop
import com.example.gymtimer2.domain.model.RunCropOutcome
import com.example.gymtimer2.domain.model.RunLap
import com.example.gymtimer2.domain.model.RunLocalSegmentEffort
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunWorkoutStep
import com.example.gymtimer2.domain.model.calculateRunActivityCrop
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun RunActivityCropDialog(
    run: RunModel,
    points: List<RunPointModel>,
    laps: List<RunLap>,
    workoutSteps: List<RunWorkoutStep>,
    localSegmentEfforts: List<RunLocalSegmentEffort>,
    isRouteInUse: Boolean,
    onSave: suspend (Long, Long) -> RunCropOutcome,
    onDismiss: () -> Unit
) {
    var cropStartFraction by rememberSaveable(run.id) { mutableFloatStateOf(0f) }
    var cropEndFraction by rememberSaveable(run.id) { mutableFloatStateOf(1f) }
    var isSaving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<RunCropOutcome?>(null) }
    val scope = rememberCoroutineScope()
    val lastIndex = points.lastIndex
    val startIndex = (cropStartFraction * lastIndex).roundToInt().coerceIn(0, lastIndex)
    val endIndex = (cropEndFraction * lastIndex).roundToInt().coerceIn(0, lastIndex)
    val crop = remember(run, points, laps, workoutSteps, localSegmentEfforts, startIndex, endIndex) {
        calculateRunActivityCrop(
            run = run,
            points = points,
            startPointId = points[startIndex].id,
            endPointId = points[endIndex].id,
            laps = laps,
            workoutSteps = workoutSteps,
            localSegmentEfforts = localSegmentEfforts
        )
    }
    val isFullRouteSelected = startIndex == 0 && endIndex == lastIndex

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(stringResource(R.string.run_crop_title)) },
        text = {
            RunActivityCropContent(
                crop = crop,
                startFraction = cropStartFraction,
                endFraction = cropEndFraction,
                isRouteInUse = isRouteInUse,
                outcome = outcome,
                saveFailed = saveFailed,
                onRangeChange = {
                    cropStartFraction = it.start
                    cropEndFraction = it.endInclusive
                    saveFailed = false
                    outcome = null
                }
            )
        },
        confirmButton = {
            TextButton(
                enabled = crop != null && !isFullRouteSelected && !isRouteInUse && !isSaving,
                onClick = {
                    val selectedCrop = crop ?: return@TextButton
                    scope.launch {
                        isSaving = true
                        saveFailed = false
                        outcome = null
                        val result = try {
                            attemptRunCropSave(selectedCrop, onSave)
                        } finally {
                            isSaving = false
                        }
                        if (result == null) {
                            saveFailed = true
                        } else {
                            outcome = result
                            if (result == RunCropOutcome.SAVED) onDismiss()
                        }
                    }
                }
            ) {
                Text(stringResource(if (isSaving) R.string.run_crop_saving else R.string.run_crop_action))
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
private fun RunActivityCropContent(
    crop: RunActivityCrop?,
    startFraction: Float,
    endFraction: Float,
    isRouteInUse: Boolean,
    outcome: RunCropOutcome?,
    saveFailed: Boolean,
    onRangeChange: (ClosedFloatingPointRange<Float>) -> Unit
) {
    Column(
        Modifier.heightIn(max = MAX_CROP_DIALOG_HEIGHT_DP.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(stringResource(R.string.run_crop_description))
        crop?.let { RunActivityCropPreview(it) }
        Text(
            stringResource(R.string.run_crop_range_label),
            modifier = Modifier.padding(top = 12.dp)
        )
        RangeSlider(
            value = startFraction..endFraction,
            onValueChange = onRangeChange,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )
        RunActivityCropStatus(isRouteInUse, crop, outcome, saveFailed)
        crop?.let { RunActivityCropMetrics(it) }
    }
}

@Composable
private fun RunActivityCropStatus(
    isRouteInUse: Boolean,
    crop: RunActivityCrop?,
    outcome: RunCropOutcome?,
    saveFailed: Boolean
) {
    val message = when {
        isRouteInUse -> R.string.run_crop_route_in_use
        crop == null -> R.string.run_crop_invalid_selection
        outcome?.let { it != RunCropOutcome.SAVED } == true -> outcome.messageResource()
        saveFailed -> R.string.run_crop_failed
        else -> null
    }
    message?.let {
        Text(stringResource(it), color = MaterialTheme.colorScheme.error)
    }
}

private suspend fun attemptRunCropSave(
    crop: RunActivityCrop,
    onSave: suspend (Long, Long) -> RunCropOutcome
): RunCropOutcome? = try {
    onSave(crop.points.first().id, crop.points.last().id)
} catch (error: CancellationException) {
    throw error
} catch (_: Exception) {
    null
}

@Composable
private fun RunActivityCropPreview(crop: RunActivityCrop) {
    RunDetailsMapSection(crop.points, height = CROP_MAP_HEIGHT_DP.dp)
}

@Composable
private fun RunActivityCropMetrics(crop: RunActivityCrop) {
    Text(
        stringResource(
            R.string.run_crop_summary,
            RunFormatting.distanceLabel(crop.distanceMeters),
            RunMetrics.formatDuration(crop.durationMs),
            RunMetrics.formatDuration(crop.elapsedDurationMs)
        ),
        modifier = Modifier.padding(top = 8.dp)
    )
}

private fun RunCropOutcome.messageResource(): Int = when (this) {
    RunCropOutcome.ACTIVE_ROUTE_IN_USE -> R.string.run_crop_route_in_use
    RunCropOutcome.INVALID_SELECTION -> R.string.run_crop_invalid_selection
    RunCropOutcome.RUN_NOT_COMPLETED -> R.string.run_crop_failed
    RunCropOutcome.SAVED -> R.string.run_crop_failed
}

private const val MAX_CROP_DIALOG_HEIGHT_DP = 520
private const val CROP_MAP_HEIGHT_DP = 190
