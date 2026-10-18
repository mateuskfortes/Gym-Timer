package com.example.gymtimer2.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioFocusRequest
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri
import com.example.gymtimer2.domain.model.MusicPlaybackState
import com.example.gymtimer2.util.hasAudioPermission
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
    private var isPrepared = false
    val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (isPrepared) {
                    mediaPlayer?.let { player ->
                        runCatching { player.start() }
                            .onSuccess { updatePlaybackFrom(player) }
                            .onFailure { stopInternal() }
                    }
                }
            }
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (isPrepared) {
                    mediaPlayer?.let { player ->
                        runCatching { player.pause() }
                            .onSuccess { updatePlaybackFrom(player) }
                            .onFailure { stopInternal() }
                    }
                }
            }
        }
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null
    private var previewEndAtMs: Int? = null
    private var delayedPlayerJob: Job? = null
    private var fadeInJob: Job? = null
    private var previousVolume: Float? = null

    private val _playbackState = MutableStateFlow(MusicPlaybackState())
    val playbackState: StateFlow<MusicPlaybackState> = _playbackState.asStateFlow()

    fun play(
        uri: String,
        startAtMs: Int = 0,
        fadeInDurationMs: Int = 0,
        volume: Float? = null,
        playForMs: Int? = null
    ) {
        stopInternal()
        if (!hasAudioPermission(context)) return

        val player = runCatching { MediaPlayer() }.getOrNull() ?: run {
            stopInternal()
            return
        }
        mediaPlayer = player

        runCatching {
            player.setOnPreparedListener { preparedPlayer ->
                startPreparedPlayback(
                    preparedPlayer,
                    uri,
                    startAtMs,
                    fadeInDurationMs,
                    volume,
                    playForMs
                )
            }
            player.setOnCompletionListener { completedPlayer ->
                if (mediaPlayer === completedPlayer) stopInternal()
            }
            player.setOnErrorListener { failedPlayer, _, _ ->
                if (mediaPlayer === failedPlayer) stopInternal()
                true
            }
            player.setDataSource(context, uri.toUri())
            player.prepareAsync()
        }.onFailure { stopInternal() }
    }

    private fun startPreparedPlayback(
        player: MediaPlayer,
        uri: String,
        startAtMs: Int,
        fadeInDurationMs: Int,
        volume: Float?,
        playForMs: Int?
    ) {
        if (mediaPlayer !== player) return
        if (!hasAudioPermission(context)) {
            stopInternal()
            return
        }
        if (!runCatching { requestAudioFocus() }.getOrDefault(false)) {
            stopInternal()
            return
        }

        runCatching {
            isPrepared = true
            previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
            val durationMs = player.duration
            val safeStartAtMs = startAtMs.coerceIn(0, durationMs)
            previewEndAtMs = playForMs?.takeIf { it > 0 }?.let { limitMs ->
                (safeStartAtMs + limitMs).coerceAtMost(durationMs)
            }
            player.seekTo(safeStartAtMs)
            player.setVolume(0f, 0f)
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                volume?.toInt() ?: (previousVolume ?: 0f).toInt(),
                0
            )
            player.start()
            updatePlaybackFrom(player, uri)
            startProgressUpdates(uri.toUri())
            startFadeIn(player, fadeInDurationMs)
        }.onFailure { stopInternal() }
    }

    private fun startFadeIn(player: MediaPlayer, durationMs: Int) {
        if (durationMs <= 0) {
            player.setVolume(1f, 1f)
            return
        }

        fadeInJob = scope.launch {
            val stepMs = 50L
            val steps = ((durationMs + stepMs - 1) / stepMs).toInt().coerceAtLeast(1)
            for (step in 1..steps) {
                if (!isActive || mediaPlayer !== player) break
                val volume = (step * stepMs).toFloat() / durationMs
                player.setVolume(volume.coerceIn(0f, 1f), volume.coerceIn(0f, 1f))
                delay(stepMs)
            }
            if (mediaPlayer === player) player.setVolume(1f, 1f)
            fadeInJob = null
        }
    }

    fun delayedPlay(
        uri: String,
        startChorusAtMs: Int = 0,
        delayMs: Int = 0,
        volume: Float? = null,
        playForMs: Int? = null
    ) {
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
                playForMs = playForMs,
                volume = volume
            )
        }
    }

    fun stop() {
        stopInternal()
    }

    fun seekTo(positionMs: Int) {
        if (!isPrepared) return
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

    fun setPlaybackVolume(volume: Float) {
        if (!isPrepared || mediaPlayer == null) return

        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            volume.toInt().coerceIn(0, maxVolume),
            0
        )
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(audioFocusListener, Handler(Looper.getMainLooper()))
                .setAcceptsDelayedFocusGain(false)
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            } else {
                audioManager.abandonAudioFocus(audioFocusListener)
            }
        } catch (_: Exception) {}
    }

    private fun startProgressUpdates(uri: Uri) {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val player = mediaPlayer ?: break
                val progress = runCatching {
                    Triple(player.currentPosition, player.duration, player.isPlaying)
                }.getOrNull()
                if (progress == null) {
                    stopInternal()
                    break
                }
                val endAtMs = previewEndAtMs
                if (endAtMs != null && progress.first >= endAtMs) {
                    stopInternal()
                    break
                }
                updatePlaybackState(
                    uri = uri.toString(),
                    currentPositionMs = progress.first,
                    durationMs = progress.second,
                    isPlaying = progress.third
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

        val player = mediaPlayer
        mediaPlayer = null
        isPrepared = false
        runCatching { player?.release() }

        delayedPlayerJob?.cancel()
        delayedPlayerJob = null

        abandonAudioFocus()

        _playbackState.value = MusicPlaybackState()

        val volumeToRestore = previousVolume
        previousVolume = null
        volumeToRestore?.let { volume ->
            runCatching {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume.toInt(), 0)
            }
        }
    }

    private fun updatePlaybackFrom(player: MediaPlayer, uri: String? = _playbackState.value.uri) {
        updatePlaybackState(
            uri = uri,
            currentPositionMs = runCatching { player.currentPosition }.getOrDefault(0),
            durationMs = runCatching { player.duration }.getOrDefault(0),
            isPlaying = runCatching { player.isPlaying }.getOrDefault(false)
        )
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
