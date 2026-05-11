package com.example.gymtimer2.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri

class MusicPlayerManager(
    private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null

    fun play(uri: Uri, startAtMs: Int = 0) {
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, uri)
            prepare()

            val safeStartAt = minOf(startAtMs, duration)
            seekTo(safeStartAt)
            start()
        }
    }

    fun stop() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}