@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.ui.screens.runs.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunActivityTag
import com.example.gymtimer2.domain.model.RunSaveDetails
import com.example.gymtimer2.domain.model.RunGear
import com.example.gymtimer2.ui.screens.runs.RunGearCatalog
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.screens.runs.format.RunActivityFormatting
import com.example.gymtimer2.ui.components.AppTextField

/** Campos compartilhados ao salvar uma corrida ou editar sua atividade local. */
@Composable
internal fun RunSaveDetailsFields(
    details: RunSaveDetails,
    onDetailsChange: (RunSaveDetails) -> Unit,
    gearCatalog: RunGearCatalog = RunGearCatalog(),
    useDefaultGear: Boolean = false
) {
    ApplyDefaultRunGear(details, onDetailsChange, gearCatalog.items.firstOrNull(RunGear::isDefault), useDefaultGear)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppTextField(
            value = details.title.orEmpty(),
            onValueChange = { onDetailsChange(details.copy(title = it.take(RunSaveDetails.MAX_TITLE_LENGTH))) },
            label = { Text(stringResource(R.string.run_title_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            outlined = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            )
        )
        Text(
            stringResource(
                R.string.run_title_character_count,
                details.title.orEmpty().length,
                RunSaveDetails.MAX_TITLE_LENGTH
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AppTextField(
            value = details.description.orEmpty(),
            onValueChange = { onDetailsChange(details.copy(description = it.take(RunSaveDetails.MAX_DESCRIPTION_LENGTH))) },
            label = { Text(stringResource(R.string.run_description_label)) },
            modifier = Modifier.fillMaxWidth(),
            outlined = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        AppTextField(
            value = details.privateNotes.orEmpty(),
            onValueChange = { onDetailsChange(details.copy(privateNotes = it.take(RunSaveDetails.MAX_PRIVATE_NOTES_LENGTH))) },
            label = { Text(stringResource(R.string.run_private_notes_label)) },
            modifier = Modifier.fillMaxWidth(),
            outlined = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        RunGearFields(details, onDetailsChange, gearCatalog)
        Text(stringResource(R.string.run_perceived_exertion_label), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.run_perceived_exertion_scale),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        for (ratings in (RunSaveDetails.MIN_PERCEIVED_EXERTION..RunSaveDetails.MAX_PERCEIVED_EXERTION).chunked(5)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ratings.forEach { rating ->
                    FilterChip(
                        selected = details.perceivedExertion == rating,
                        onClick = {
                            onDetailsChange(
                                details.copy(perceivedExertion = if (details.perceivedExertion == rating) null else rating)
                            )
                        },
                        label = { Text(rating.toString()) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Text(stringResource(R.string.run_activity_tags_label), style = MaterialTheme.typography.titleSmall)
        for (tags in RunActivityTag.entries.chunked(2)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag ->
                    FilterChip(
                        selected = tag in details.activityTags,
                        onClick = {
                            val selectedTags = details.activityTags
                            onDetailsChange(
                                details.copy(
                                    activityTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
                                )
                            )
                        },
                        label = { Text(RunActivityFormatting.activityTagLabel(tag)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().toggleable(
                value = details.isCommute,
                role = Role.Checkbox,
                onValueChange = { onDetailsChange(details.copy(isCommute = it)) }
            ),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Checkbox(checked = details.isCommute, onCheckedChange = null)
            Text(stringResource(R.string.run_commute_label))
        }
    }
}

@Composable
private fun RunGearFields(
    details: RunSaveDetails,
    onDetailsChange: (RunSaveDetails) -> Unit,
    gearCatalog: RunGearCatalog
) {
    RunGearSelector(
        gearId = details.gearId,
        gearName = details.gearName,
        gears = gearCatalog.items,
        onNameChange = { onDetailsChange(details.copy(gearId = null, gearName = it)) },
        onSelect = { gear -> onDetailsChange(details.copy(gearId = gear?.id, gearName = gear?.name)) },
        onCreate = gearCatalog.create,
        onSetDefault = gearCatalog.setDefault,
        onSetRetired = gearCatalog.setRetired,
        onRename = gearCatalog.rename,
        onSetPhoto = gearCatalog.setPhoto,
        onRemovePhoto = gearCatalog.removePhoto,
        onCreatePhotoCapture = gearCatalog.createPhotoCapture,
        onDeletePhotoCapture = gearCatalog.deletePhotoCapture
    )
}

@Composable
private fun ApplyDefaultRunGear(
    details: RunSaveDetails,
    onDetailsChange: (RunSaveDetails) -> Unit,
    defaultGear: RunGear?,
    enabled: Boolean
) {
    val latestDetails = rememberUpdatedState(details)
    val latestOnDetailsChange = rememberUpdatedState(onDetailsChange)
    LaunchedEffect(defaultGear?.id, enabled) {
        if (!enabled) return@LaunchedEffect
        val gear = defaultGear ?: return@LaunchedEffect
        val current = latestDetails.value
        if (current.gearId != null || current.gearName != null) return@LaunchedEffect
        latestOnDetailsChange.value(current.copy(gearId = gear.id, gearName = gear.name))
    }
}
