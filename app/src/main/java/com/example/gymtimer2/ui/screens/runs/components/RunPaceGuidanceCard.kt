package com.example.gymtimer2.ui.screens.runs.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.domain.model.RunPaceGuidance
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.domain.model.RunPaceTarget

@Composable
internal fun RunPaceGuidanceCard(target: RunPaceTarget, currentPaceSecondsPerKm: Double?) {
    val range = stringResource(
        R.string.run_pace_target_range,
        RunFormatting.pace(target.fasterSecondsPerKm.toDouble()),
        RunFormatting.pace(target.slowerSecondsPerKm.toDouble())
    )
    val status = when (target.classify(currentPaceSecondsPerKm)) {
        RunPaceGuidance.WAITING -> R.string.run_pace_guidance_waiting
        RunPaceGuidance.FASTER -> R.string.run_pace_guidance_faster
        RunPaceGuidance.ON_TARGET -> R.string.run_pace_guidance_on_target
        RunPaceGuidance.SLOWER -> R.string.run_pace_guidance_slower
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(stringResource(R.string.run_pace_target_label), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text(range, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(status), color = MaterialTheme.colorScheme.primary)
        }
    }
}
