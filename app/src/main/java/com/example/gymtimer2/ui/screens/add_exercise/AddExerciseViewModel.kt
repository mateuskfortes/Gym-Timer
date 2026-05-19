package com.example.gymtimer2.ui.screens.add_exercise

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.gymtimer2.data.repository.WorkoutRepository
import com.example.gymtimer2.data.repository.ExerciseChorusRepository
import com.example.gymtimer2.data.repository.SavedSongRepository
import com.example.gymtimer2.data.repository.ChorusRepository
import com.example.gymtimer2.domain.model.ExerciseModel
import com.example.gymtimer2.domain.model.WeightModel
import com.example.gymtimer2.domain.model.WeightUnit
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.domain.model.ChorusModel
import com.example.gymtimer2.player.MusicPlayerManager
import com.example.gymtimer2.ui.components.music.hasAudioPermission
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(SavedStateHandleSaveableApi::class)
class AddExerciseViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: WorkoutRepository,
    private val exerciseChorusRepository: ExerciseChorusRepository,
    private val chorusRepository: ChorusRepository,
    private val savedSongRepository: SavedSongRepository,
    private val playerManager: MusicPlayerManager
) : ViewModel() {

    var name by savedStateHandle.saveable { mutableStateOf("") }
        private set

    var weight by savedStateHandle.saveable { mutableStateOf("") }
        private set

    var restSeconds by savedStateHandle.saveable { mutableStateOf("80") }
        private set

    var weightUnit by savedStateHandle.saveable { mutableStateOf(WeightUnit.TOTAL_KG) }
        private set

    // Songs and choruses state
    private val _allSongs = MutableStateFlow<List<SongModel>>(emptyList())
    val allSongs: StateFlow<List<SongModel>> = _allSongs.asStateFlow()

    private val _allChoruses = MutableStateFlow<List<ChorusModel>>(emptyList())
    val allChoruses: StateFlow<List<ChorusModel>> = _allChoruses.asStateFlow()

    private val _allSongsWithChoruses = MutableStateFlow<List<Pair<SongModel, List<ChorusModel>>>>(emptyList())
    val allSongsWithChoruses: StateFlow<List<Pair<SongModel, List<ChorusModel>>>> = _allSongsWithChoruses.asStateFlow()

    // Chorus selection state for new exercise
    private val _selectedChorusIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedChorusIds: StateFlow<Set<Long>> = _selectedChorusIds.asStateFlow()

    init {
        viewModelScope.launch {
            savedSongRepository.savedSongs.collect { songs ->
                _allSongs.value = songs
                refreshAllSongsWithChoruses()
            }
        }

        viewModelScope.launch {
            chorusRepository.getAllChoruses().collect { allChoruses ->
                _allChoruses.value = allChoruses
                refreshAllSongsWithChoruses()
            }
        }
    }

    private fun refreshAllSongsWithChoruses() {
        val songs = _allSongs.value
        val choruses = _allChoruses.value

        _allSongsWithChoruses.value = songs.map { song ->
            song to choruses.filter { it.songId == song.id }
        }
    }

    fun onNameChange(value: String) {
        name = value
    }

    fun onWeightChange(newValue: String) {
        if (newValue.all { it.isDigit() } || newValue.isBlank()) {
            weight = newValue
        }
    }

    fun onRestSecondsChange(newValue: String) {
        if (newValue.all { it.isDigit() } || newValue.isBlank()) {
            restSeconds = newValue
        }
    }

    fun onWeightUnitChange(newValue: WeightUnit) {
        weightUnit = newValue
    }

    fun toggleChorusSelection(chorusId: Long) {
        val current = _selectedChorusIds.value.toMutableSet()
        if (current.contains(chorusId)) {
            current.remove(chorusId)
        } else {
            current.add(chorusId)
        }
        _selectedChorusIds.value = current
    }

    fun addMultipleChorusesToExercise(chorusIds: Set<Long>) {
        val current = _selectedChorusIds.value.toMutableSet()
        current.addAll(chorusIds)
        _selectedChorusIds.value = current
    }

    fun removeChorusFromExercise(chorusId: Long) {
        val current = _selectedChorusIds.value.toMutableSet()
        current.remove(chorusId)
        _selectedChorusIds.value = current
    }

    fun playChorus(song: SongModel, chorus: ChorusModel) {
        playerManager.play(song.uri, chorus.startMs.toInt())
    }

    fun stopPlayback() {
        playerManager.stop()
    }

    fun insertExercise(goBack: () -> Unit = {}) = viewModelScope.launch {
        val trimmedName = name.trim()
        val trimmedWeight = weight.trim()
        val trimmedRest = restSeconds.trim()

        if (trimmedName.isBlank() || trimmedWeight.isBlank() || trimmedRest.isBlank()) {
            return@launch
        }

        val weightValue = trimmedWeight.toIntOrNull() ?: return@launch
        val restMs = trimmedRest.toLongOrNull()?.let { it * 1000 } ?: return@launch

        val exercise = ExerciseModel(
            id = 0,
            name = trimmedName,
            weight = WeightModel(
                value = weightValue,
                unit = weightUnit
            ),
            restPeriod = restMs
        )

        // Insert exercise and get the auto-generated ID
        val exerciseId = repository.insertExercise(exercise)

        // Save chorus associations if any were selected
        val selectedIds = _selectedChorusIds.value
        if (selectedIds.isNotEmpty()) {
            selectedIds.forEach { chorusId ->
                exerciseChorusRepository.addChorusToExercise(exerciseId.toInt(), chorusId)
            }
        }

        goBack()
    }

    companion object {
        fun factory(
            repository: WorkoutRepository,
            exerciseChorusRepository: ExerciseChorusRepository,
            chorusRepository: ChorusRepository,
            savedSongRepository: SavedSongRepository,
            playerManager: MusicPlayerManager
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    AddExerciseViewModel(
                        savedStateHandle = createSavedStateHandle(),
                        repository = repository,
                        exerciseChorusRepository = exerciseChorusRepository,
                        chorusRepository = chorusRepository,
                        savedSongRepository = savedSongRepository,
                        playerManager = playerManager
                    )
                }
            }
    }

}