package com.example.gymtimer2.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.domain.model.WorkoutDayModel

@Composable
fun WorkoutDaySelector(
    days: List<WorkoutDayModel>,
    selectedDayId: Long?,
    onSelectDay: (Long) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        days.forEach { day ->
            AppFilterChip(
                text = day.name,
                selected = day.id == selectedDayId,
                onClick = { onSelectDay(day.id) }
            )
        }
    }
}
