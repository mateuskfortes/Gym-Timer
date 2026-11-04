package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunActivityType
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.screens.runs.format.RunActivityFormatting

/** Permite escolher o tipo de atividade antes de iniciar a gravação. */
@Composable
internal fun RunActivityTypeDialog(
    selectedActivityType: RunActivityType,
    onSelect: (RunActivityType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.run_activity_type_picker_title)) },
        text = {
            Column {
                RunActivityType.entries.forEach { activityType ->
                    Row(
                        modifier = Modifier.fillMaxWidth().selectable(
                            selected = selectedActivityType == activityType,
                            role = Role.RadioButton,
                            onClick = { onSelect(activityType) }
                        ).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedActivityType == activityType,
                            onClick = null
                        )
                        Text(
                            RunActivityFormatting.activityTypeLabel(activityType),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}
