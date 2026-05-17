package com.example.gymtimer2.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import com.example.gymtimer2.domain.model.MusicPlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicPlayerManager(
    private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null
    private var previewEndAtMs: Int? = null

    private val _playbackState = MutableStateFlow(MusicPlaybackState())
    val playbackState: StateFlow<MusicPlaybackState> = _playbackState.asStateFlow()

    fun play(uri: Uri, startAtMs: Int = 0, playForMs: Int? = null) {
        stopInternal()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, uri)
            prepare()

            val safeStartAt = minOf(startAtMs, duration)
            previewEndAtMs = playForMs?.takeIf { it > 0 }?.let { limitMs ->
                (safeStartAt + limitMs).coerceAtMost(duration)
            }
            seekTo(safeStartAt)
            setOnCompletionListener {
                stopInternal()
            }
            start()
        }

        updatePlaybackState(uri.toString(), mediaPlayer?.currentPosition ?: 0, mediaPlayer?.duration ?: 0, true)
        startProgressUpdates(uri)
    }

    fun stop() {
        stopInternal()
    }

    fun seekTo(positionMs: Int) {
        val player = mediaPlayer ?: return
        val duration = runCatching { player.duration }.getOrDefault(0)
        val safePosition = if (duration > 0) {
            positionMs.coerceIn(0, duration)
        } else {
            positionMs.coerceAtLeast(0)
        }

        runCatching {
            player.seekTo(safePosition)
        }.onSuccess {
            updatePlaybackState(
                uri = _playbackState.value.uri,
                currentPositionMs = player.currentPosition,
                durationMs = runCatching { player.duration }.getOrDefault(duration),
                isPlaying = player.isPlaying
            )
        }
    }

    private fun startProgressUpdates(uri: Uri) {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val player = mediaPlayer ?: break
                val endAtMs = previewEndAtMs
                if (endAtMs != null && player.currentPosition >= endAtMs) {
                    stopInternal()
                    break
                }
                updatePlaybackState(
                    uri = uri.toString(),
                    currentPositionMs = player.currentPosition,
                    durationMs = runCatching { player.duration }.getOrDefault(0),
                    isPlaying = player.isPlaying
                )
                delay(250)
            }
        }
    }

    private fun stopInternal() {
        progressJob?.cancel()
        progressJob = null
        previewEndAtMs = null

        mediaPlayer?.release()
        mediaPlayer = null

        _playbackState.value = MusicPlaybackState()
    }

    private fun updatePlaybackState(
        uri: String?,
        currentPositionMs: Int = 0,
        durationMs: Int = 0,
        isPlaying: Boolean = false
    ) {
        _playbackState.update {
            it.copy(
                uri = uri,
                currentPositionMs = currentPositionMs.coerceAtLeast(0),
                durationMs = durationMs.coerceAtLeast(0),
                isPlaying = isPlaying
            )
        }
    }
}