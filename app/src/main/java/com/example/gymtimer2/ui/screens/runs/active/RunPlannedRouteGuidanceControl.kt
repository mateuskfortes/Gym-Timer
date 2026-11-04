@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.ui.screens.runs.RunPlannedRouteGuidanceState

@Composable
internal fun RunPlannedRouteGuidanceControl(
    isVisible: Boolean,
    state: RunPlannedRouteGuidanceState,
    onRequest: () -> Unit
) {
    var showConsent by rememberSaveable { mutableStateOf(false) }
    if (!isVisible || state.route != null) return

    if (state.isLoading) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            Text(
                stringResource(R.string.run_route_guidance_loading),
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    } else {
        if (state.hasError) {
            Text(
                stringResource(R.string.run_route_guidance_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(4.dp))
        }
        TextButton(onClick = { showConsent = true }) {
            Text(
                stringResource(
                    if (state.hasError) R.string.run_route_guidance_retry else R.string.run_route_guidance_action
                )
            )
        }
    }

    if (showConsent) {
        AlertDialog(
            onDismissRequest = { showConsent = false },
            title = { Text(stringResource(R.string.run_route_guidance_consent_title)) },
            text = { Text(stringResource(R.string.run_route_guidance_consent_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showConsent = false
                    onRequest()
                }) {
                    Text(stringResource(R.string.run_route_guidance_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConsent = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}
