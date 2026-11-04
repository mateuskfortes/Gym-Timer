package com.example.gymtimer2.ui.screens.runs.history

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.example.gymtimer2.domain.model.MAX_RUN_MEDIA_COUNT
import com.example.gymtimer2.domain.model.RunMedia
import com.example.gymtimer2.ui.screens.runs.components.RunMediaSection

@Composable
internal fun RunDetailsMediaSection(
    media: List<RunMedia>,
    onAdd: (List<Uri>) -> Unit,
    onRemove: (Long) -> Unit,
    onSetFeatured: (Long) -> Unit
) {
    val mediaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia((MAX_RUN_MEDIA_COUNT - media.size).coerceAtLeast(1))
    ) {
        onAdd(it)
    }
    RunMediaSection(
        media = media,
        onAdd = {
            mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        },
        onRemove = onRemove,
        onSetFeatured = onSetFeatured
    )
}
