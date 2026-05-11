package com.example.gymtimer2.ui.screens.song_list

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymtimer2.data.repository.SongRepository
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.player.MusicPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MusicListViewModel(
    private val repository: SongRepository,
    private val playerManager: MusicPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SongListUiState())
    val uiState: StateFlow<SongListUiState> = _uiState.asStateFlow()

    fun loadSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }

            runCatching {
                repository.getSongs()
            }.onSuccess { songs ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        songs = songs
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = it.error ?: "Erro ao carregar músicas"
                    )
                }
            }
        }
    }

    fun playSong(song: SongModel) {
        playerManager.play(song.uri)
        _uiState.update { it.copy(playingSongId = song.id) }
    }

    fun stopPlayer() {
        playerManager.stop()
        _uiState.update { it.copy(playingSongId = null) }
    }

    override fun onCleared() {
        playerManager.release()
        super.onCleared()
    }

    fun checkAudioPermission(context: Context): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val hasPermission =  ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        _uiState.update { it.copy(hasPermission = hasPermission) }

        if (hasPermission) loadSongs()

        return hasPermission
    }

    companion object {
        fun factory(
            repository: SongRepository,
            playerManager: MusicPlayerManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MusicListViewModel(repository, playerManager) as T
            }
        }
    }
}