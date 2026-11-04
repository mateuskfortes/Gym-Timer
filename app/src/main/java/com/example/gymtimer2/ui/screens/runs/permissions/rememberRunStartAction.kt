package com.example.gymtimer2.ui.screens.runs.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.gymtimer2.R

/** Solicita permissões no início da ação e não pede acesso permanente ao GPS. */
@Composable
internal fun rememberRunStartAction(
    context: Context,
    onStartRun: () -> Unit,
    requestOptionalPermissions: Boolean = true,
    onMessage: (String) -> Unit
): () -> Unit {
    var issue by remember { mutableStateOf<RunStartIssue?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val locationIssue = runLocationPermissionIssue(
            preciseLocationGranted = hasPreciseLocation(context),
            approximateLocationGranted = hasApproximateLocation(context)
        )
        if (locationIssue == null) {
            if (notificationPermissionWasDenied(grants)) {
                onMessage(context.getString(R.string.run_notification_permission_note))
            }
            onStartRun()
        } else {
            issue = locationIssue
        }
    }

    val startRun = {
        if (!isGpsEnabled(context)) {
            issue = RunStartIssue.GPS_DISABLED
        } else {
            val permissions = missingRunPermissions(context, requestOptionalPermissions)
            if (permissions.isEmpty()) onStartRun() else permissionLauncher.launch(permissions)
        }
    }

    issue?.let { currentIssue ->
        AlertDialog(
            onDismissRequest = { issue = null },
            title = { Text(stringResource(currentIssue.titleResource())) },
            text = { Text(stringResource(currentIssue.messageResource())) },
            confirmButton = {
                TextButton(onClick = {
                    issue = null
                    context.startActivity(currentIssue.settingsIntent(context))
                }) {
                    Text(stringResource(currentIssue.actionResource()))
                }
            },
            dismissButton = {
                TextButton(onClick = { issue = null }) {
                    Text(stringResource(R.string.run_start_dismiss))
                }
            }
        )
    }

    return startRun
}

private fun RunStartIssue.titleResource(): Int = when (this) {
    RunStartIssue.GPS_DISABLED -> R.string.run_start_gps_title
    RunStartIssue.APPROXIMATE_LOCATION -> R.string.run_start_approximate_title
    RunStartIssue.LOCATION_DENIED -> R.string.run_start_denied_title
}

private fun RunStartIssue.messageResource(): Int = when (this) {
    RunStartIssue.GPS_DISABLED -> R.string.run_start_gps_message
    RunStartIssue.APPROXIMATE_LOCATION -> R.string.run_start_approximate_message
    RunStartIssue.LOCATION_DENIED -> R.string.run_start_denied_message
}

private fun RunStartIssue.actionResource(): Int = when (this) {
    RunStartIssue.GPS_DISABLED -> R.string.run_start_gps_settings
    RunStartIssue.APPROXIMATE_LOCATION,
    RunStartIssue.LOCATION_DENIED -> R.string.run_start_location_settings
}

private fun RunStartIssue.settingsIntent(context: Context): Intent = when (this) {
    RunStartIssue.GPS_DISABLED -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    RunStartIssue.APPROXIMATE_LOCATION,
    RunStartIssue.LOCATION_DENIED -> Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
}
