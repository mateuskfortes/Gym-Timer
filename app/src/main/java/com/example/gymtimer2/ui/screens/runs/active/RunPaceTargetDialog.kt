package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunDistanceUnit
import com.example.gymtimer2.domain.model.RunPaceTarget
import com.example.gymtimer2.ui.components.AppTextField
import com.example.gymtimer2.ui.components.PrimaryButton
import com.example.gymtimer2.ui.screens.runs.format.LocalRunDistanceUnit
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import kotlin.math.roundToInt

@Composable
internal fun RunPaceTargetDialog(
    visible: Boolean,
    initialTarget: RunPaceTarget?,
    onConfirm: (RunPaceTarget?) -> Unit,
    onDismiss: () -> Unit
) {
    if (visible) {
        val distanceUnit = LocalRunDistanceUnit.current
        var fasterPace by rememberSaveable(initialTarget, distanceUnit) {
            mutableStateOf(initialTarget?.fasterSecondsPerKm?.let { formatPaceInput(it, distanceUnit) }.orEmpty())
        }
        var slowerPace by rememberSaveable(initialTarget, distanceUnit) {
            mutableStateOf(initialTarget?.slowerSecondsPerKm?.let { formatPaceInput(it, distanceUnit) }.orEmpty())
        }
        val target = RunPaceTarget.parse(fasterPace, slowerPace, distanceUnit)
        val hasInput = fasterPace.isNotBlank() || slowerPace.isNotBlank()

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.run_pace_target_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.run_pace_target_description, RunFormatting.distanceUnitName()))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppTextField(
                            value = fasterPace,
                            onValueChange = { fasterPace = it.filterPaceInput() },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.run_pace_target_faster)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            outlined = true
                        )
                        AppTextField(
                            value = slowerPace,
                            onValueChange = { slowerPace = it.filterPaceInput() },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.run_pace_target_slower)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                            outlined = true
                        )
                    }
                    if (hasInput && target == null) {
                        Text(
                            stringResource(R.string.run_pace_target_invalid),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                PrimaryButton(onClick = { onConfirm(target) }, enabled = target != null) {
                    Text(stringResource(R.string.run_pace_target_save))
                }
            },
            dismissButton = {
                Row {
                    if (initialTarget != null) {
                        TextButton(onClick = { onConfirm(null) }) {
                            Text(stringResource(R.string.run_pace_target_clear))
                        }
                    }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
                }
            }
        )
    }
}

private fun String.filterPaceInput(): String = filter { it.isDigit() || it == ':' }.take(5)

private fun formatPaceInput(secondsPerKm: Int, distanceUnit: RunDistanceUnit): String {
    val secondsPerUnit = distanceUnit.paceSecondsPerUnit(secondsPerKm.toDouble()).roundToInt()
    return "%02d:%02d".format(secondsPerUnit / 60, secondsPerUnit % 60)
}
