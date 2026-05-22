package com.example.gymtimer2.ui.screens.edit_song_chorus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymtimer2.data.repository.WorkoutRepository
import com.example.gymtimer2.domain.model.ChorusModel
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.player.MusicPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditSongChorusViewModel(
    private val repository: WorkoutRepository,
    private val playerManager: MusicPlayerManager
) : ViewModel() {

    fun choruses(songId: Long): Flow<List<ChorusModel>> = repository.getChorusesBySongId(songId)

    fun createNewChorus(song: SongModel): ChorusModel {
        return ChorusModel(
            songId = song.id,
            name = "",
            startMs = 0
        )
    }

    fun playFullSong(song: SongModel) {
        playerManager.play(song.uri, 0)
    }

    fun playChorus(song: SongModel, chorus: ChorusModel) {
        playerManager.play(song.uri, chorus.startMs.toInt())
    }

    fun playChorusPreview(song: SongModel, startMs: Long) {
        playerManager.play(song.uri, startMs.toInt())
    }

    fun stopPlayback() {
        playerManager.stop()
    }

    fun seekTo(positionMs: Int) {
        playerManager.seekTo(positionMs)
    }

    fun saveChorus(
        chorus: ChorusModel,
        songDurationMs: Long? = null,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val normalizedStart = chorus.startMs.coerceAtLeast(0)

            val safeChorus = chorus.copy(
                name = chorus.name.trim(),
                startMs = normalizedStart
            )

            if (songDurationMs != null && songDurationMs > 0 && safeChorus.startMs > songDurationMs) {
                return@launch
            }

            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    if (safeChorus.id == 0L) {
                        repository.insertChorus(safeChorus)
                    } else {
                        repository.updateChorus(safeChorus)
                    }
                }.isSuccess
            }

            if (saved) {
                onDone()
            }
        }
    }

    fun deleteChorus(chorus: ChorusModel, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val deleted = withContext(Dispatchers.IO) {
                runCatching {
                    repository.deleteChorus(chorus)
                }.isSuccess
            }

            if (deleted) {
                onDone()
            }
        }
    }

    companion object {
        fun factory(
            repository: WorkoutRepository,
            playerManager: MusicPlayerManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EditSongChorusViewModel(repository, playerManager) as T
            }
        }
    }
}



