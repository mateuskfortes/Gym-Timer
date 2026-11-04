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
import com.example.gymtimer2.domain.model.RunStreetGuidance
import com.example.gymtimer2.domain.model.RunTurnDirection
import com.example.gymtimer2.domain.model.RunTurnGuidance
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting

@Composable
internal fun RunTurnGuidanceCard(guidance: RunTurnGuidance, isNext: Boolean = true) {
    val direction = when (guidance.direction) {
        RunTurnDirection.LEFT -> R.string.run_turn_direction_left
        RunTurnDirection.RIGHT -> R.string.run_turn_direction_right
        RunTurnDirection.U_TURN -> R.string.run_turn_direction_uturn
    }
    RunGuidanceCard(
        label = stringResource(
            if (isNext) R.string.run_turn_next_label else R.string.run_turn_upcoming_label
        ),
        instruction = stringResource(
            R.string.run_turn_guidance,
            stringResource(direction),
            formatGuidanceDistance(guidance.distanceToTurnMeters)
        )
    )
}

@Composable
internal fun RunStreetGuidanceCard(guidance: RunStreetGuidance) {
    RunGuidanceCard(
        label = stringResource(R.string.run_street_next_label),
        instruction = stringResource(
            R.string.run_turn_guidance,
            guidance.text,
            formatGuidanceDistance(guidance.distanceToInstructionMeters)
        )
    )
}

@Composable
private fun RunGuidanceCard(label: String, instruction: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                instruction,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun formatGuidanceDistance(distanceMeters: Double): String {
    return if (distanceMeters <= TURN_NOW_THRESHOLD_METERS) {
        stringResource(R.string.run_turn_distance_now)
    } else {
        stringResource(R.string.run_turn_distance, RunFormatting.adaptiveDistanceLabel(distanceMeters))
    }
}

private const val TURN_NOW_THRESHOLD_METERS = 25.0
