package com.example.gymtimer2.ui.screens.runs.permissions

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.example.gymtimer2.R
import kotlinx.coroutines.launch

/** Solicita permissões no início da ação e não pede acesso permanente ao GPS. */
@Composable
internal fun rememberRunStartAction(
    context: Context,
    onStartRun: () -> Unit,
    onMessage: (String) -> Unit
): () -> Unit {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (hasPreciseLocation(context)) {
            if (notificationPermissionWasDenied(grants)) {
                onMessage(context.getString(R.string.run_notification_permission_note))
            }
            onStartRun()
        } else {
            onMessage(context.getString(R.string.run_location_permission_required))
        }
    }

    return {
        if (!isGpsEnabled(context)) {
            onMessage(context.getString(R.string.run_gps_required))
        } else {
            val permissions = missingRunPermissions(context)
            if (permissions.isEmpty()) onStartRun() else permissionLauncher.launch(permissions)
        }
    }
}
