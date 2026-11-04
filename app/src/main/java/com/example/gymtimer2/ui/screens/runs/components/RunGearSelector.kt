package com.example.gymtimer2.ui.screens.runs.components

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunGear
import com.example.gymtimer2.domain.model.RunSaveDetails
import com.example.gymtimer2.ui.components.AppTextField
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting

@Composable
internal fun RunGearSelector(
    gearId: Long?,
    gearName: String?,
    gears: List<RunGear>,
    onNameChange: (String) -> Unit,
    onSelect: (RunGear?) -> Unit,
    onCreate: (String, (RunGear) -> Unit) -> Unit,
    onSetDefault: (Long) -> Unit,
    onSetRetired: (Long, Boolean) -> Unit,
    onRename: (Long, String, (Boolean) -> Unit) -> Unit,
    onSetPhoto: (Long, Bitmap) -> Unit,
    onRemovePhoto: (Long) -> Unit,
    onCreatePhotoCapture: ((Uri?) -> Unit) -> Unit,
    onDeletePhotoCapture: (Uri) -> Unit
) {
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    var showRetired by rememberSaveable { mutableStateOf(false) }
    var editingGear by remember { mutableStateOf<RunGear?>(null) }
    var photoGearId by rememberSaveable { mutableStateOf<Long?>(null) }
    var captureUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var cropSourceUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) photoGearId = null else cropSourceUri = uri
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val uri = captureUri
        if (captured && uri != null && photoGearId != null) {
            cropSourceUri = uri
        } else {
            uri?.let(onDeletePhotoCapture)
            captureUri = null
            photoGearId = null
        }
    }
    val dismissCrop = {
        captureUri?.let(onDeletePhotoCapture)
        captureUri = null
        cropSourceUri = null
        photoGearId = null
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        AppTextField(
            value = gearName.orEmpty(),
            onValueChange = { onNameChange(it.take(RunSaveDetails.MAX_GEAR_NAME_LENGTH)) },
            label = { Text(stringResource(R.string.run_gear_label)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            outlined = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                capitalization = KeyboardCapitalization.Words
            )
        )
        TextButton(onClick = { pickerOpen = true }) {
            Text(stringResource(R.string.run_gear_browse))
        }
    }

    RunGearPickerDialog(
        isVisible = pickerOpen,
        gearId = gearId,
        gearName = gearName,
        gears = gears,
        showRetired = showRetired,
        onShowRetiredChange = { showRetired = it },
        onDismiss = { pickerOpen = false },
        onSelect = { gear -> onSelect(gear); pickerOpen = false },
        onCreate = onCreate,
        onSetDefault = onSetDefault,
        onSetRetired = onSetRetired,
        onRename = { editingGear = it },
        onChoosePhoto = { gear ->
            photoGearId = gear.id
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onTakePhoto = { gear ->
            photoGearId = gear.id
            onCreatePhotoCapture { uri ->
                if (uri == null) {
                    photoGearId = null
                } else {
                    captureUri = uri
                    startGearPhotoCapture(uri, camera, onDeletePhotoCapture) {
                        captureUri = null
                        photoGearId = null
                    }
                }
            }
        },
        onRemovePhoto = { onRemovePhoto(it.id) }
    )

    cropSourceUri?.let { uri ->
        RunGearPhotoCropDialog(
            sourceUri = uri,
            onCropped = { bitmap ->
                photoGearId?.let { onSetPhoto(it, bitmap) }
                captureUri?.let(onDeletePhotoCapture)
                captureUri = null
                cropSourceUri = null
                photoGearId = null
            },
            onDismiss = dismissCrop
        )
    }

    editingGear?.let { gear ->
        RunGearRenameDialog(
            gear = gear,
            onRename = onRename,
            onDismiss = { editingGear = null }
        )
    }
}

@Composable
private fun RunGearPickerDialog(
    isVisible: Boolean,
    gearId: Long?,
    gearName: String?,
    gears: List<RunGear>,
    showRetired: Boolean,
    onShowRetiredChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSelect: (RunGear?) -> Unit,
    onCreate: (String, (RunGear) -> Unit) -> Unit,
    onSetDefault: (Long) -> Unit,
    onSetRetired: (Long, Boolean) -> Unit,
    onRename: (RunGear) -> Unit,
    onChoosePhoto: (RunGear) -> Unit,
    onTakePhoto: (RunGear) -> Unit,
    onRemovePhoto: (RunGear) -> Unit
) {
    if (!isVisible) return
    val visibleGears = gears.filter { it.isRetired == showRetired }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.run_gear_select)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = { onShowRetiredChange(!showRetired) }) {
                    Text(stringResource(if (showRetired) R.string.run_gear_show_active else R.string.run_gear_show_retired))
                }
                if (gearId != null || !gearName.isNullOrBlank()) {
                    TextButton(onClick = { onSelect(null) }) {
                        Text(stringResource(R.string.run_gear_none))
                    }
                }
                visibleGears.forEach { gear ->
                    RunGearCatalogItem(
                        gear = gear,
                        onSelect = { onSelect(gear) },
                        onSetDefault = { onSetDefault(gear.id) },
                        onSetRetired = { onSetRetired(gear.id, !gear.isRetired) },
                        onRename = { onRename(gear) },
                        onChoosePhoto = { onChoosePhoto(gear) },
                        onTakePhoto = { onTakePhoto(gear) },
                        onRemovePhoto = { onRemovePhoto(gear) }
                    )
                }
                if (visibleGears.isEmpty()) Text(stringResource(R.string.run_gear_empty))
            }
        },
        confirmButton = {
            TextButton(
                enabled = !gearName.isNullOrBlank(),
                onClick = {
                    onCreate(gearName.orEmpty()) { gear -> onSelect(gear) }
                }
            ) {
                Text(stringResource(R.string.run_gear_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}

private fun startGearPhotoCapture(
    uri: Uri?,
    camera: androidx.activity.result.ActivityResultLauncher<Uri>,
    onDeletePhotoCapture: (Uri) -> Unit,
    onFailure: () -> Unit
) {
    if (uri == null) {
        onFailure()
        return
    }
    try {
        camera.launch(uri)
    } catch (_: ActivityNotFoundException) {
        onDeletePhotoCapture(uri)
        onFailure()
    } catch (_: SecurityException) {
        onDeletePhotoCapture(uri)
        onFailure()
    }
}

@Composable
private fun RunGearCatalogItem(
    gear: RunGear,
    onSelect: () -> Unit,
    onSetDefault: () -> Unit,
    onSetRetired: () -> Unit,
    onRename: () -> Unit,
    onChoosePhoto: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    var photoMenuExpanded by remember { mutableStateOf(false) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                enabled = !gear.isRetired,
                onClick = onSelect,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(gear.name)
                    Text(
                        stringResource(R.string.run_gear_distance, RunFormatting.distanceLabel(gear.distanceMeters)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            gear.photoFileName?.let { fileName ->
                LocalMediaThumbnail(
                    fileName = fileName,
                    contentDescription = stringResource(R.string.run_gear_photo_alt, gear.name),
                    unavailableContentDescription = stringResource(R.string.run_media_unavailable),
                    size = 64.dp,
                    removeContentDescription = stringResource(R.string.run_gear_photo_remove, gear.name),
                    onRemove = onRemovePhoto
                )
            }
            IconButton(onClick = { photoMenuExpanded = true }) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = stringResource(R.string.run_gear_photo_menu, gear.name),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            DropdownMenu(expanded = photoMenuExpanded, onDismissRequest = { photoMenuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.run_gear_photo_gallery)) },
                    onClick = {
                        photoMenuExpanded = false
                        onChoosePhoto()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.run_gear_photo_camera)) },
                    onClick = {
                        photoMenuExpanded = false
                        onTakePhoto()
                    }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!gear.isRetired) {
                TextButton(onClick = onSetDefault) {
                    Text(stringResource(if (gear.isDefault) R.string.run_gear_default else R.string.run_gear_make_default))
                }
            }
            TextButton(onClick = onRename) { Text(stringResource(R.string.run_gear_rename)) }
            TextButton(onClick = onSetRetired) {
                Text(stringResource(if (gear.isRetired) R.string.run_gear_reactivate else R.string.run_gear_retire))
            }
        }
    }
}

@Composable
private fun RunGearRenameDialog(
    gear: RunGear,
    onRename: (Long, String, (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable(gear.id) { mutableStateOf(gear.name) }
    var nameAlreadyUsed by rememberSaveable(gear.id) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.run_gear_rename_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(RunSaveDetails.MAX_GEAR_NAME_LENGTH)
                        nameAlreadyUsed = false
                    },
                    label = { Text(stringResource(R.string.run_gear_label)) },
                    singleLine = true,
                    outlined = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    )
                )
                if (nameAlreadyUsed) Text(stringResource(R.string.run_gear_name_taken))
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                onRename(gear.id, name) { saved ->
                    if (saved) onDismiss() else nameAlreadyUsed = true
                }
            }) {
                Text(stringResource(R.string.run_save_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
        }
    )
}
