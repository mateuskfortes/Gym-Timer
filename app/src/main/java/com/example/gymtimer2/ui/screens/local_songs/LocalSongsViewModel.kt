package com.example.gymtimer2.ui.screens.local_songs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymtimer2.data.repository.WorkoutRepository
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.player.MusicPlayerManager
import com.example.gymtimer2.util.hasAudioPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocalSongsViewModel(
    private val repository: WorkoutRepository,
    private val playerManager: MusicPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocalSongsUiState())
    val uiState: StateFlow<LocalSongsUiState> = _uiState.asStateFlow()

    fun loadSongs(savedSongsIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }

            runCatching {
                repository.getSongs()
            }.onSuccess { songs ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        songs = songs.filter { song -> song.id !in savedSongsIds }
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "Erro ao carregar músicas"
                    )
                }
            }
        }
    }

    fun toggleSongSelection(songId: Long) {
        _uiState.update { state ->
            val selectedSongIds = state.selectedSongIds.toMutableSet().apply {
                if (contains(songId)) {
                    remove(songId)
                } else {
                    add(songId)
                }
            }

            state.copy(selectedSongIds = selectedSongIds)
        }
    }

    fun saveSelectedSongs(cbFunc: () -> Unit) {
        val songsToSave = _uiState.value.songs.filter { song ->
            song.id in _uiState.value.selectedSongIds
        }

        if (songsToSave.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSaving = true, error = null) }

            runCatching {
                repository.saveSongs(songsToSave)
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        selectedSongIds = emptySet()
                    )
                }
                cbFunc()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = throwable.message ?: "Erro ao salvar seleção"
                    )
                }
            }
        }
    }

    fun playSong(context: Context, song: SongModel) {
        if (!hasAudioPermission(context)) {
            _uiState.update {
                it.copy(error = "Permissão de áudio necessária para reproduzir músicas")
            }
            return
        }

        runCatching {
            playerManager.play(song.uriString)
        }.onSuccess {
            _uiState.update { it.copy(playingSongId = song.id, error = null) }
        }.onFailure { throwable ->
            _uiState.update {
                it.copy(error = throwable.message ?: "Não foi possível reproduzir a música")
            }
        }
    }

    fun stopPlayer() {
        playerManager.stop()
        _uiState.update { it.copy(playingSongId = null) }
    }

    fun seekTo(positionMs: Int) {
        playerManager.seekTo(positionMs)
    }

    fun checkAudioPermission(context: Context): Boolean {
        val hasPermission = hasAudioPermission(context)

        _uiState.update { it.copy(hasPermission = hasPermission) }

        if (hasPermission) {
            viewModelScope.launch {
                repository.getSavedSongsIds().collect {
                    loadSongs(it)
                }
            }
        }

        return hasPermission
    }

    companion object {
        fun factory(
            repository: WorkoutRepository,
            playerManager: MusicPlayerManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LocalSongsViewModel(repository, playerManager) as T
            }
        }
    }
}