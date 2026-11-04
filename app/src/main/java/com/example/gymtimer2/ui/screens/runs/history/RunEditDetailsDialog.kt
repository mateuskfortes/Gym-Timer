package com.example.gymtimer2.ui.screens.runs.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunSaveDetails
import com.example.gymtimer2.domain.model.RunActivityTag
import com.example.gymtimer2.ui.screens.runs.RunGearCatalog
import com.example.gymtimer2.ui.components.PrimaryButton
import com.example.gymtimer2.ui.screens.runs.active.RunActivityTypeDialog
import com.example.gymtimer2.ui.screens.runs.active.RunActivityTypeSelector
import com.example.gymtimer2.ui.screens.runs.components.RunSaveDetailsFields
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.screens.runs.format.RunActivityFormatting

/** Edita os metadados locais de uma atividade concluída. */
@Composable
internal fun RunEditDetailsDialog(
    run: RunModel,
    onSave: (RunSaveDetails) -> Unit,
    onDismiss: () -> Unit,
    gearCatalog: RunGearCatalog = RunGearCatalog()
) {
    var title by rememberSaveable(run.id) { mutableStateOf(run.title.orEmpty()) }
    var description by rememberSaveable(run.id) { mutableStateOf(run.description.orEmpty()) }
    var privateNotes by rememberSaveable(run.id) { mutableStateOf(run.privateNotes.orEmpty()) }
    var gearName by rememberSaveable(run.id) { mutableStateOf(run.gearName.orEmpty()) }
    var gearId by rememberSaveable(run.id) { mutableStateOf(run.gearId) }
    var isCommute by rememberSaveable(run.id) { mutableStateOf(run.isCommute) }
    var perceivedExertion by rememberSaveable(run.id) { mutableStateOf(run.perceivedExertion) }
    var activityType by rememberSaveable(run.id) { mutableStateOf(run.activityType) }
    var activityTags by rememberSaveable(run.id) { mutableStateOf(RunActivityTag.serialize(run.activityTags)) }
    var isActivityTypePickerOpen by rememberSaveable(run.id) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.run_edit_details_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.run_activity_type_edit_label))
                RunActivityTypeSelector(
                    label = RunActivityFormatting.activityTypeLabel(activityType),
                    onClick = { isActivityTypePickerOpen = true }
                )
                RunSaveDetailsFields(
                    details = RunSaveDetails(
                        title = title,
                        description = description,
                        privateNotes = privateNotes,
                        gearName = gearName,
                        gearId = gearId,
                        isCommute = isCommute,
                        perceivedExertion = perceivedExertion,
                        activityTags = RunActivityTag.parse(activityTags)
                    ),
                    onDetailsChange = { details ->
                        title = details.title.orEmpty()
                        description = details.description.orEmpty()
                        privateNotes = details.privateNotes.orEmpty()
                        gearName = details.gearName.orEmpty()
                        gearId = details.gearId
                        isCommute = details.isCommute
                        perceivedExertion = details.perceivedExertion
                        activityTags = RunActivityTag.serialize(details.activityTags)
                    },
                    gearCatalog = gearCatalog
                )
            }
        },
        confirmButton = {
            PrimaryButton(onClick = {
                onSave(
                    RunSaveDetails(
                        title = title,
                        description = description,
                        privateNotes = privateNotes,
                        gearName = gearName,
                        gearId = gearId,
                        isCommute = isCommute,
                        perceivedExertion = perceivedExertion,
                        activityType = activityType,
                        activityTags = RunActivityTag.parse(activityTags)
                    ).normalized()
                )
            }) {
                Text(stringResource(R.string.run_save_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
        }
    )
    if (isActivityTypePickerOpen) {
        RunActivityTypeDialog(
            selectedActivityType = activityType,
            onSelect = { selectedType ->
                activityType = selectedType
                isActivityTypePickerOpen = false
            },
            onDismiss = { isActivityTypePickerOpen = false }
        )
    }
}
