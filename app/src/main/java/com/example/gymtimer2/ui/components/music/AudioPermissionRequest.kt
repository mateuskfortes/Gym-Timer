package com.example.gymtimer2.ui.components.music

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.gymtimer2.util.hasAudioPermission
import com.example.gymtimer2.util.requiredAudioPermission

@Composable
fun rememberAudioPermissionRequest(
    onPermissionResult: (Boolean) -> Unit = {}
): (onGranted: () -> Unit) -> Unit {
    val context = LocalContext.current
    val currentOnPermissionResult by rememberUpdatedState(onPermissionResult)
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val action = pendingAction
        pendingAction = null
        val hasPermission = granted && hasAudioPermission(context)
        currentOnPermissionResult(hasPermission)
        if (hasPermission) action?.invoke()
    }

    return { action ->
        if (hasAudioPermission(context)) {
            currentOnPermissionResult(true)
            action()
        } else {
            pendingAction = action
            permissionLauncher.launch(requiredAudioPermission())
        }
    }
}
