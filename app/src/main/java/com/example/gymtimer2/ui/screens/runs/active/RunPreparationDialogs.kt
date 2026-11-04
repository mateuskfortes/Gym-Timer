package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.res.stringResource
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunWorkoutPlan

@Composable
internal fun RunPreparationDialogs(
    showBatterySaverWarning: Boolean,
    onContinueWithBatterySaver: () -> Unit,
    onDismissBatterySaverWarning: () -> Unit,
    onOpenBatterySaverSettings: () -> Unit,
    showGpsWarning: Boolean,
    onContinueWithoutGps: () -> Unit,
    onDismissGpsWarning: () -> Unit,
    showWorkoutEditor: Boolean,
    workoutPlan: RunWorkoutPlan?,
    onSaveWorkout: (RunWorkoutPlan) -> Unit,
    onDismissWorkoutEditor: () -> Unit,
    routeImportFailed: Boolean,
    onDismissImportError: () -> Unit
) {
    if (showBatterySaverWarning) {
        AlertDialog(
            onDismissRequest = onDismissBatterySaverWarning,
            title = { Text(stringResource(R.string.run_battery_saver_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.run_battery_saver_message))
                    TextButton(onClick = onOpenBatterySaverSettings) {
                        Text(stringResource(R.string.run_battery_saver_settings))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onContinueWithBatterySaver) {
                    Text(stringResource(R.string.run_battery_saver_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissBatterySaverWarning) {
                    Text(stringResource(R.string.run_battery_saver_cancel))
                }
            }
        )
    }
    if (showGpsWarning) {
        AlertDialog(
            onDismissRequest = onDismissGpsWarning,
            title = { Text(stringResource(R.string.run_gps_warning_title)) },
            text = { Text(stringResource(R.string.run_gps_warning_message)) },
            confirmButton = {
                TextButton(onClick = onContinueWithoutGps) {
                    Text(stringResource(R.string.run_gps_warning_continue))
                }
            }
        )
    }
    if (showWorkoutEditor) {
        RunWorkoutEditorDialog(workoutPlan, onSaveWorkout, onDismissWorkoutEditor)
    }
    if (routeImportFailed) {
        AlertDialog(
            onDismissRequest = onDismissImportError,
            title = { Text(stringResource(R.string.run_import_error_title)) },
            text = { Text(stringResource(R.string.run_import_error_message)) },
            confirmButton = {
                TextButton(onClick = onDismissImportError) {
                    Text(stringResource(R.string.run_import_error_close))
                }
            }
        )
    }
}
