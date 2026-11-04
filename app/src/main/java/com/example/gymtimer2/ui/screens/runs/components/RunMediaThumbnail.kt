package com.example.gymtimer2.ui.screens.runs.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunMedia

@Composable
internal fun RunMediaThumbnail(
    media: RunMedia,
    index: Int,
    onRemove: (Long) -> Unit,
    onSetFeatured: (Long) -> Unit
) {
    var isPlayerOpen by remember(media.id) { mutableStateOf(false) }
    LocalMediaThumbnail(
        fileName = media.fileName,
        isVideo = media.isVideo,
        contentDescription = stringResource(
            if (media.isVideo) R.string.run_media_video_alt else R.string.run_media_photo_alt,
            index + 1
        ),
        unavailableContentDescription = stringResource(R.string.run_media_unavailable),
        videoContentDescription = stringResource(R.string.run_media_play, index + 1),
        size = 112.dp,
        isFeatured = media.isFeatured,
        featureContentDescription = stringResource(R.string.run_media_set_featured, index + 1),
        featuredContentDescription = stringResource(R.string.run_media_featured, index + 1),
        onPlay = { isPlayerOpen = true },
        onSetFeatured = { onSetFeatured(media.id) },
        removeContentDescription = stringResource(R.string.run_media_remove, index + 1),
        onRemove = { onRemove(media.id) }
    )
    if (media.isVideo && isPlayerOpen) {
        RunVideoPlayerDialog(
            fileName = media.fileName,
            title = stringResource(R.string.run_media_play, index + 1),
            onDismiss = { isPlayerOpen = false }
        )
    }
}
