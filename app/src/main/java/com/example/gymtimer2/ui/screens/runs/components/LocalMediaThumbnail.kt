package com.example.gymtimer2.ui.screens.runs.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun LocalMediaThumbnail(
    fileName: String,
    contentDescription: String,
    unavailableContentDescription: String,
    size: Dp,
    isVideo: Boolean = false,
    videoContentDescription: String? = null,
    onPlay: (() -> Unit)? = null,
    isFeatured: Boolean = false,
    featureContentDescription: String? = null,
    featuredContentDescription: String? = null,
    onSetFeatured: (() -> Unit)? = null,
    removeContentDescription: String? = null,
    onRemove: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val thumbnail by produceState<Bitmap?>(null, fileName, isVideo) {
        value = loadThumbnail(context, fileName, isVideo)
    }
    Box(Modifier.size(size)) {
        LocalMediaThumbnailSurface(thumbnail, contentDescription, unavailableContentDescription)
        VideoPreviewControl(isVideo, onPlay, videoContentDescription)
        FeaturedMediaControl(isFeatured, featuredContentDescription, onSetFeatured, featureContentDescription)
        RemoveMediaControl(onRemove, removeContentDescription)
    }
}

@Composable
private fun LocalMediaThumbnailSurface(
    thumbnail: Bitmap?,
    contentDescription: String,
    unavailableContentDescription: String
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            thumbnail?.let { bitmap ->
                Image(
                    bitmap.asImageBitmap(),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } ?: Icon(
                Icons.Default.BrokenImage,
                contentDescription = unavailableContentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BoxScope.VideoPreviewControl(isVideo: Boolean, onPlay: (() -> Unit)?, contentDescription: String?) {
    if (!isVideo || onPlay == null || contentDescription == null) return
    IconButton(
        onClick = onPlay,
        modifier = Modifier.align(Alignment.Center).size(44.dp)
            .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f), CircleShape)
    ) {
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.inverseOnSurface
        )
    }
}

@Composable
private fun BoxScope.FeaturedMediaControl(
    isFeatured: Boolean,
    featuredContentDescription: String?,
    onSetFeatured: (() -> Unit)?,
    featureContentDescription: String?
) {
    when {
        isFeatured && featuredContentDescription != null -> Icon(
            Icons.Default.Star,
            contentDescription = featuredContentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                .size(24.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape)
        )
        onSetFeatured != null && featureContentDescription != null -> IconButton(
            onClick = onSetFeatured,
            modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                .size(36.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape)
        ) {
            Icon(
                Icons.Outlined.StarBorder,
                contentDescription = featureContentDescription,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BoxScope.RemoveMediaControl(onRemove: (() -> Unit)?, contentDescription: String?) {
    if (onRemove == null || contentDescription == null) return
    IconButton(
        onClick = onRemove,
        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
            .size(36.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape)
    ) {
        Icon(Icons.Default.Close, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurface)
    }
}

private suspend fun loadThumbnail(context: Context, fileName: String, isVideo: Boolean): Bitmap? = withContext(Dispatchers.IO) {
    if (File(fileName).name != fileName) return@withContext null
    val media = File(context.filesDir, "run_photos/$fileName")
    if (isVideo) return@withContext loadVideoThumbnail(media)

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(media.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        null
    } else {
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > 256) sampleSize *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        BitmapFactory.decodeFile(media.path, options)
    }
}

private fun loadVideoThumbnail(file: File): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(file.path)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(-1L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 256, 256)
        } else {
            retriever.getFrameAtTime(-1L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let(::scaleThumbnail)
        }
    } catch (_: Exception) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

private fun scaleThumbnail(bitmap: Bitmap): Bitmap {
    val longestSide = maxOf(bitmap.width, bitmap.height)
    if (longestSide <= 256) return bitmap
    val scale = 256f / longestSide
    val scaled = Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * scale).toInt().coerceAtLeast(1),
        (bitmap.height * scale).toInt().coerceAtLeast(1),
        true
    )
    if (scaled !== bitmap) bitmap.recycle()
    return scaled
}
