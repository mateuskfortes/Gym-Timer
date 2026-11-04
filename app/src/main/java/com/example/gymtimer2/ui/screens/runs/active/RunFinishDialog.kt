package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import com.example.gymtimer2.domain.model.RunSaveDetails
import com.example.gymtimer2.domain.model.RunActivityTag
import com.example.gymtimer2.ui.screens.runs.RunGearCatalog
import com.example.gymtimer2.ui.screens.runs.components.RunSaveDetailsFields
import com.example.gymtimer2.ui.components.PrimaryButton

/** Coleta os metadados locais e confirma o descarte sem alterar a sessão ao cancelar. */
@Composable
internal fun RunFinishDialog(
    onConfirm: (RunSaveDetails) -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
    gearCatalog: RunGearCatalog = RunGearCatalog(),
    isEmptyActivity: Boolean = false
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var privateNotes by rememberSaveable { mutableStateOf("") }
    var gearName by rememberSaveable { mutableStateOf("") }
    var gearId by rememberSaveable { mutableStateOf<Long?>(null) }
    var isCommute by rememberSaveable { mutableStateOf(false) }
    var perceivedExertion by rememberSaveable { mutableStateOf<Int?>(null) }
    var activityTags by rememberSaveable { mutableStateOf("") }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    if (confirmDiscard) {
        RunDiscardDialog(onConfirm = onDiscard, onDismiss = { confirmDiscard = false })
        return
    }
    if (isEmptyActivity) {
        RunEmptyActivityDialog(
            onResume = onDismiss,
            onDiscard = { confirmDiscard = true }
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.run_finish_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.run_finish_message))
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
                    gearCatalog = gearCatalog,
                    useDefaultGear = true
                )
                TextButton(
                    onClick = { confirmDiscard = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text(stringResource(R.string.run_discard_action))
                }
            }
        },
        confirmButton = {
            PrimaryButton(onClick = {
                onConfirm(
                    RunSaveDetails(
                        title = title,
                        description = description,
                        privateNotes = privateNotes,
                        gearName = gearName,
                        gearId = gearId,
                        isCommute = isCommute,
                        perceivedExertion = perceivedExertion,
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
}
