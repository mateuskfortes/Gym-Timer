package com.example.gymtimer2.ui.screens.local_songs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymtimer2.data.repository.WorkoutRepository
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.player.MusicPlayerManager
import com.example.gymtimer2.util.hasAudioPermission
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocalSongsViewModel(
    private val repository: WorkoutRepository,
    private val playerManager: MusicPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocalSongsUiState())
    val uiState: StateFlow<LocalSongsUiState> = _uiState.asStateFlow()
    private var savedSongsJob: Job? = null

    init {
        viewModelScope.launch {
            playerManager.playbackState
                .map { playback -> playback.uri.takeIf { playback.isPlaying } }
                .distinctUntilChanged()
                .collect { playingUri ->
                    _uiState.update { state ->
                        state.copy(
                            playingSongId = state.songs.firstOrNull { it.uriString == playingUri }?.id
                        )
                    }
                }
        }
    }

    private suspend fun loadSongs(savedSongsIds: List<Long>) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val savedIds = savedSongsIds.toHashSet()
            val songs = withContext(Dispatchers.IO) {
                repository.getSongs().filterNot { it.id in savedIds }
            }
            val playback = playerManager.playbackState.value
            val playingSongId = songs.firstOrNull { song ->
                playback.isPlaying && song.uriString == playback.uri
            }?.id
            _uiState.update {
                it.copy(isLoading = false, songs = songs, playingSongId = playingSongId)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            _uiState.update {
                it.copy(isLoading = false, error = exception.message ?: "Erro ao carregar músicas")
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
        val state = _uiState.value
        if (state.isSaving) return
        val songsToSave = state.songs.filter { it.id in state.selectedSongIds }

        if (songsToSave.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                withContext(Dispatchers.IO) { repository.saveSongs(songsToSave) }
                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        selectedSongIds = emptySet()
                    )
                }
                cbFunc()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = exception.message ?: "Erro ao salvar seleção"
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
            _uiState.update { it.copy(error = null) }
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

        if (hasPermission && savedSongsJob?.isActive != true) {
            savedSongsJob = viewModelScope.launch {
                repository.getSavedSongsIds().collectLatest(::loadSongs)
            }
        } else if (!hasPermission) {
            stopPlayer()
            stopObservingSongs()
        }

        return hasPermission
    }

    fun stopObservingSongs() {
        savedSongsJob?.cancel()
        savedSongsJob = null
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
