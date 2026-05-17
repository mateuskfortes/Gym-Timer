package com.example.gymtimer2.ui.components.music

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SongCover(uri: Uri?) {
    val context = LocalContext.current

    val coverBytes = remember(uri) {
        if (uri == null) return@remember null

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    if (coverBytes != null) {
        val bitmap = remember(coverBytes) {
            BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.size)
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Capa da música",
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Crop
            )
            return
        }
    }

    Box(
        modifier = Modifier
            .size(64.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text("♪", fontSize = 48.sp)
    }
}