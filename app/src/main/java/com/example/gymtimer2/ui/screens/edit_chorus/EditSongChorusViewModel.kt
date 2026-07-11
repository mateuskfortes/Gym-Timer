package com.example.gymtimer2.ui.screens.edit_chorus

import android.health.connect.datatypes.units.Volume
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymtimer2.data.repository.WorkoutRepository
import com.example.gymtimer2.domain.model.ChorusModel
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.player.MusicPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditSongChorusViewModel(
    private val repository: WorkoutRepository,
    val playerManager: MusicPlayerManager,
    val songToEdit: SongModel
) : ViewModel() {

    private val _choruses = MutableStateFlow<List<ChorusModel>>(emptyList())
    val choruses = _choruses.asStateFlow()

    private val _chorusToEdit = MutableStateFlow<ChorusModel?>(null)
    val chorusToEdit = _chorusToEdit.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getChorusesBySongId(songToEdit.id).collect { choruses ->
                _choruses.value = choruses
            }
        }
    }

    // Playback functions
    fun play(startMs: Int = 0, volume: Float? = null) {
        playerManager.play(songToEdit.uriString, startMs, volume = volume)
    }
    fun stopPlayback() {
        playerManager.stop()
    }
    fun seekTo(positionMs: Int) {
        playerManager.seekTo(positionMs)
    }

    // Chorus management functions
    fun newChorus() {
        _chorusToEdit.value = ChorusModel(
            songId = songToEdit.id,
            name = "",
            startMs = 0,
            volume = playerManager.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        )
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

    // Chorus card management
    fun playChorus(chorus: ChorusModel) {
        play(chorus.startMs.toInt(), volume = chorus.volume)
    }
    fun setChorusToEdit(chorus: ChorusModel?) {
        _chorusToEdit.value = chorus
    }
    fun saveUpdatedChorus(chorus: ChorusModel) {
        saveChorus(
            chorus = chorus,
            songToEdit.durationMs
        ) {
            _chorusToEdit.value = null
        }
    }
    fun deleteChorus(chorus: ChorusModel) {
        playerManager.stop()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteChorus(chorus)
            }
        }
    }

    companion object {
        fun factory(
            repository: WorkoutRepository,
            playerManager: MusicPlayerManager,
            songToEdit: SongModel
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EditSongChorusViewModel(repository, playerManager, songToEdit) as T
            }
        }
    }
}



