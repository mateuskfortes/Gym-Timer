package com.example.gymtimer2.ui.screens.saved_songs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.gymtimer2.data.repository.SavedSongRepository
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.player.MusicPlayerManager
import com.example.gymtimer2.ui.components.music.hasAudioPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SavedSongsViewModel(
    repository: SavedSongRepository,
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
            playerManager.play(song.uri, song.startAtMs.toInt())
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


    companion object {
        fun factory(
            repository: SavedSongRepository,
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

