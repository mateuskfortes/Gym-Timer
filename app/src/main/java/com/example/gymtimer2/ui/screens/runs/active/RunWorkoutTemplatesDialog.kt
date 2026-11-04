package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunWorkoutTemplate

@Composable
internal fun RunWorkoutTemplatesDialog(
    templates: List<RunWorkoutTemplate>,
    selectedTemplateId: Long?,
    onSelect: (RunWorkoutTemplate?) -> Unit,
    onCreate: () -> Unit,
    onEdit: (RunWorkoutTemplate) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var templateToDelete by remember { mutableStateOf<RunWorkoutTemplate?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.run_workout_templates_title)) },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (templates.isEmpty()) {
                    Text(
                        stringResource(R.string.run_workout_templates_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    Modifier.fillMaxWidth().clickable { onSelect(null) }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedTemplateId == null, onClick = { onSelect(null) })
                    Text(stringResource(R.string.run_workout_templates_none))
                }
                HorizontalDivider()
                templates.forEach { template ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            Modifier.weight(1f).clickable { onSelect(template) }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTemplateId == template.id,
                                onClick = { onSelect(template) }
                            )
                            Text(template.plan.name, maxLines = 1)
                        }
                        IconButton(onClick = { onEdit(template) }) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = stringResource(
                                    R.string.run_workout_template_edit,
                                    template.plan.name
                                )
                            )
                        }
                        IconButton(onClick = { templateToDelete = template }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = stringResource(
                                    R.string.run_workout_template_delete,
                                    template.plan.name
                                )
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCreate) {
                Text(stringResource(R.string.run_workout_templates_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.run_cancel_action))
            }
        }
    )

    templateToDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = { Text(stringResource(R.string.run_workout_template_delete_title)) },
            text = {
                Text(stringResource(R.string.run_workout_template_delete_message, template.plan.name))
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(template.id)
                    templateToDelete = null
                }) {
                    Text(stringResource(R.string.run_workout_template_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) {
                    Text(stringResource(R.string.run_cancel_action))
                }
            }
        )
    }
}
