package com.example.gymtimer2.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.gymtimer2.R
import com.example.gymtimer2.ui.screens.runs.tracking.RunCrashRecovery

/** Explica e abre as permissões necessárias para recuperar corridas após falhas. */
@Composable
internal fun RunCrashRecoverySetting() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val supportsBackgroundPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    var permissionGranted by remember {
        mutableStateOf(RunCrashRecovery.hasBackgroundLocationPermission(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = RunCrashRecovery.hasBackgroundLocationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val title = stringResource(R.string.run_crash_recovery_setting)
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f))
            Switch(
                checked = permissionGranted,
                enabled = supportsBackgroundPermission,
                onCheckedChange = { _ ->
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                    )
                },
                modifier = Modifier.semantics { contentDescription = title }
            )
        }
        Text(
            stringResource(R.string.run_crash_recovery_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
