package com.example.gymtimer2.ui.screens.saved_songs

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.gymtimer2.data.repository.WorkoutRepository
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.player.MusicPlayerManager
import com.example.gymtimer2.ui.components.music.hasAudioPermission
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SavedSongsViewModel(
    private val repository: WorkoutRepository,
    private val playerManager: MusicPlayerManager
) : ViewModel() {

    val savedSongs = repository.savedSongs

    private val _playingSongId = MutableStateFlow<Long?>(null)
    val playingSongId: StateFlow<Long?> = _playingSongId.asStateFlow()

    fun playSong(context: Context, song: SongModel) {
        if (!hasAudioPermission(context)) {
            _playingSongId.value = null
            return
        }

        runCatching {
            playerManager.play(song.uriString)
        }.onSuccess {
            _playingSongId.value = song.id
        }.onFailure {
            _playingSongId.value = null
        }
    }

    fun stopPlayer() {
        playerManager.stop()
        _playingSongId.value = null
    }

    fun seekTo(positionMs: Int) {
        playerManager.seekTo(positionMs)
    }

    fun deleteSong(song: SongModel) {
        if (_playingSongId.value == song.id) {
            stopPlayer()
        }
        viewModelScope.launch {
            repository.deleteSong(song)
        }
    }


    companion object {
        fun factory(
            repository: WorkoutRepository,
            playerManager: MusicPlayerManager
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SavedSongsViewModel(
                    repository = repository,
                    playerManager = playerManager
                )
            }
        }
    }

}

