package com.example.gymtimer2.ui.screens.saved_songs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.gymtimer2.data.repository.WorkoutRepository
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.player.MusicPlayerManager
import com.example.gymtimer2.util.hasAudioPermission
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SavedSongsViewModel(
    private val repository: WorkoutRepository,
    private val playerManager: MusicPlayerManager
) : ViewModel() {

    val savedSongs = repository.savedSongs

    private var requestedSongId: Long? = null

    fun playSong(context: Context, song: SongModel) {
        if (!hasAudioPermission(context)) {
            requestedSongId = null
            return
        }

        runCatching {
            playerManager.play(song.uriString)
        }.onSuccess {
            requestedSongId = song.id
        }.onFailure {
            requestedSongId = null
        }
    }

    fun stopPlayer() {
        playerManager.stop()
        requestedSongId = null
    }

    fun seekTo(positionMs: Int) {
        playerManager.seekTo(positionMs)
    }

    fun deleteSong(song: SongModel) {
        if (requestedSongId == song.id) {
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

