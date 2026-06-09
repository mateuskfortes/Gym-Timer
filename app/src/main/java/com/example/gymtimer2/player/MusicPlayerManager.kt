package com.example.gymtimer2.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.core.net.toUri
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
    private var delayedPlayerJob: Job? = null
    private var fadeInJob: Job? = null

    private val _playbackState = MutableStateFlow(MusicPlaybackState())
    val playbackState: StateFlow<MusicPlaybackState> = _playbackState.asStateFlow()

    fun play(uri: String, startAtMs: Int = 0, fadeInDurationMs: Int = 0, playForMs: Int? = null) {
        stopInternal()
        fadeInJob?.cancel()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, uri.toUri())
            prepare()

            val safeStartAt = minOf(startAtMs, duration)
            previewEndAtMs = playForMs?.takeIf { it > 0 }?.let { limitMs ->
                (safeStartAt + limitMs).coerceAtMost(duration)
            }
            seekTo(safeStartAt)
            setOnCompletionListener {
                stopInternal()
            }
            // start with volume at 0 for fade-in
            setVolume(0f, 0f)
            start()
        }

        updatePlaybackState(uri, mediaPlayer?.currentPosition ?: 0, mediaPlayer?.duration ?: 0, true)
        startProgressUpdates(uri.toUri())

        // perform fade-in if requested
        if (fadeInDurationMs > 0) {
            fadeInJob = scope.launch {
                val totalMs = fadeInDurationMs.coerceAtLeast(1)
                val stepMs = 50L
                val steps = ((totalMs + stepMs - 1) / stepMs).toInt().coerceAtLeast(1)
                for (i in 0 until steps) {
                    if (!isActive) break
                    val fraction = ((i + 1) * stepMs).toFloat() / totalMs
                    val vol = fraction.coerceIn(0f, 1f)
                    mediaPlayer?.setVolume(vol, vol)
                    delay(stepMs)
                }
                mediaPlayer?.setVolume(1f, 1f)
                fadeInJob = null
            }
        } else {
            mediaPlayer?.setVolume(1f, 1f)
        }
    }

    fun delayedPlay(uri: String, startChorusAtMs: Int = 0, delayMs: Int = 0, playForMs: Int? = null) {
        var startAtMs = startChorusAtMs - delayMs
        var silenceDurationMs: Int = 0

        if (startAtMs < 0) {
            silenceDurationMs = -startAtMs
            startAtMs = 0
        }

        delayedPlayerJob?.cancel()
        delayedPlayerJob = scope.launch {
            delay(silenceDurationMs.toLong())
            play(
                uri = uri,
                startAtMs = startAtMs,
                fadeInDurationMs = startChorusAtMs - startAtMs,
                playForMs = playForMs
            )
        }
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

        fadeInJob?.cancel()
        fadeInJob = null

        mediaPlayer?.release()
        mediaPlayer = null

        delayedPlayerJob?.cancel()

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