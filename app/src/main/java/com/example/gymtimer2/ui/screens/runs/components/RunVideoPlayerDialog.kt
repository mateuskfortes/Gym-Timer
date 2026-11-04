package com.example.gymtimer2.ui.screens.runs.components

import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import java.io.File

@Composable
internal fun RunVideoPlayerDialog(fileName: String, title: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isSafeVideo = File(fileName).name == fileName && fileName.endsWith(".video")
    val videoFile = File(context.filesDir, "run_photos/$fileName")
    val canPlay = isSafeVideo && videoFile.isFile
    val videoView = remember(fileName) {
        VideoView(context).apply {
            if (canPlay) {
                val controller = MediaController(context)
                controller.setAnchorView(this)
                setMediaController(controller)
                setVideoPath(videoFile.absolutePath)
                setOnPreparedListener { start() }
            }
        }
    }
    DisposableEffect(videoView) {
        onDispose { videoView.stopPlayback() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.run_media_close_player))
                    }
                }
                if (canPlay) {
                    AndroidView(
                        factory = { videoView },
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                    )
                } else {
                    Text(
                        stringResource(R.string.run_media_play_failed),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
