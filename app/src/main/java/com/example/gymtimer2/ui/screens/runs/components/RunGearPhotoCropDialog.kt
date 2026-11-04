package com.example.gymtimer2.ui.screens.runs.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.gymtimer2.R

private const val GEAR_PHOTO_CROP_WIDTH = 1080
private const val GEAR_PHOTO_CROP_HEIGHT = 720

@Composable
internal fun RunGearPhotoCropDialog(
    sourceUri: Uri,
    onCropped: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    var cropView by remember(sourceUri) { mutableStateOf<CropImageView?>(null) }
    var imageReady by remember(sourceUri) { mutableStateOf(false) }
    var isCropping by remember(sourceUri) { mutableStateOf(false) }
    var hasError by remember(sourceUri) { mutableStateOf(false) }
    val onCroppedState = rememberUpdatedState(onCropped)
    val accentColor = MaterialTheme.colorScheme.primary.toArgb()
    val cropOptions = remember(accentColor) {
        CropImageOptions(
            fixAspectRatio = true,
            aspectRatioX = 3,
            aspectRatioY = 2,
            guidelines = CropImageView.Guidelines.ON,
            multiTouchEnabled = true,
            maxZoom = 4,
            borderLineColor = accentColor,
            borderCornerColor = accentColor,
            guidelinesColor = Color.White.toArgb(),
            backgroundColor = Color.Black.copy(alpha = 0.62f).toArgb()
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.run_gear_photo_crop_cancel)
                        )
                    }
                    Text(
                        stringResource(R.string.run_gear_photo_crop_title),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(
                        enabled = imageReady && !isCropping,
                        onClick = {
                            hasError = false
                            isCropping = true
                            cropView?.croppedImageAsync(
                                Bitmap.CompressFormat.JPEG,
                                90,
                                GEAR_PHOTO_CROP_WIDTH,
                                GEAR_PHOTO_CROP_HEIGHT,
                                CropImageView.RequestSizeOptions.RESIZE_EXACT
                            )
                        }
                    ) {
                        Text(stringResource(R.string.run_gear_photo_crop_save))
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)).background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            CropImageView(context).apply {
                                setImageCropOptions(cropOptions)
                                setOnSetImageUriCompleteListener { _, _, error ->
                                    imageReady = error == null
                                    hasError = error != null
                                }
                                setOnCropImageCompleteListener { _, result ->
                                    isCropping = false
                                    val bitmap = result.bitmap
                                    if (result.isSuccessful && bitmap != null) {
                                        onCroppedState.value(bitmap)
                                    } else {
                                        hasError = true
                                    }
                                }
                                setImageUriAsync(sourceUri)
                                cropView = this
                            }
                        },
                        update = { view ->
                            cropView = view
                            if (view.imageUri != sourceUri) view.setImageUriAsync(sourceUri)
                        }
                    )
                    if (isCropping) CircularProgressIndicator(Modifier.size(40.dp))
                }

                Text(
                    stringResource(
                        if (hasError) R.string.run_gear_photo_crop_error
                        else R.string.run_gear_photo_crop_hint
                    ),
                    color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
