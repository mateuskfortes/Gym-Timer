package com.example.gymtimer2.ui.screens.runs.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.screens.runs.format.RunActivityFormatting
import com.example.gymtimer2.ui.components.PrimaryButton
import com.example.gymtimer2.ui.screens.runs.components.RunMetricsRow
import com.example.gymtimer2.ui.screens.runs.notifications.openRunNotificationSettings
import com.example.gymtimer2.ui.screens.runs.notifications.rememberRunNotificationsEnabled

/** Destaca a sessão aberta, seu estado e as métricas sem misturá-la às corridas concluídas. */
@Composable
internal fun ActiveRunCard(run: RunModel, onOpen: () -> Unit) {
    val context = LocalContext.current
    val notificationsEnabled = rememberRunNotificationsEnabled()
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    text = RunActivityFormatting.statusLabel(run.status),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            RunMetricsRow(run)
            PrimaryButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.run_open_active_action))
            }
            if (!notificationsEnabled && run.status != RunStatus.INTERRUPTED) {
                Text(stringResource(R.string.run_notifications_disabled))
                TextButton(onClick = { openRunNotificationSettings(context) }) {
                    Text(stringResource(R.string.run_notifications_settings))
                }
            }
        }
    }
}
