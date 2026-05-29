@file:Suppress("unused")

package com.example.gymtimer2.ui.screens.edit_exercise

import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.compose.saveable
import com.example.gymtimer2.data.repository.WorkoutRepository
import com.example.gymtimer2.domain.model.ChorusModel
import com.example.gymtimer2.domain.model.ExerciseModel
import com.example.gymtimer2.domain.model.MusicPlaybackState
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.domain.model.WeightModel
import com.example.gymtimer2.domain.model.WeightUnit
import com.example.gymtimer2.player.MusicPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

@OptIn(SavedStateHandleSaveableApi::class)
class EditExerciseViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: WorkoutRepository,
    private val playerManager: MusicPlayerManager
) : ViewModel() {

    private var id: Int by Delegates.notNull()

    private var weightUnit: WeightUnit by Delegates.notNull()

    private var originalName: String = ""

    private var originalWeight: String = ""

    private var originalRestSeconds: String = ""

    private var originalChorusDelay: String = ""

    private var originalChorusIds: Set<Long> = emptySet()

    var name by savedStateHandle.saveable { mutableStateOf("") }
        private set

    var weight by savedStateHandle.saveable { mutableStateOf("") }
        private set

    var restSeconds by savedStateHandle.saveable { mutableStateOf("") }
        private set

    var chorusDelay by savedStateHandle.saveable { mutableStateOf("") }
        private set

    private val _associatedChoruses = MutableStateFlow<List<Pair<SongModel, ChorusModel>>>(emptyList())
    val associatedChoruses: StateFlow<List<Pair<SongModel, ChorusModel>>> = _associatedChoruses.asStateFlow()

    private val _allSongs = MutableStateFlow<List<SongModel>>(emptyList())
    val allSongs: StateFlow<List<SongModel>> = _allSongs.asStateFlow()

    private val _allChoruses = MutableStateFlow<List<ChorusModel>>(emptyList())

    private val _allSongsWithChoruses = MutableStateFlow<List<Pair<SongModel, List<ChorusModel>>>>(emptyList())
    val allSongsWithChoruses: StateFlow<List<Pair<SongModel, List<ChorusModel>>>> = _allSongsWithChoruses.asStateFlow()

    private val _exerciseChoruses = MutableStateFlow<List<ChorusModel>>(emptyList())

    private val _selectedChorusIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedChorusIds: StateFlow<Set<Long>> = _selectedChorusIds.asStateFlow()

    val playbackState: StateFlow<MusicPlaybackState> = playerManager.playbackState

    init {
        viewModelScope.launch {
            repository.savedSongs.collect { songs ->
                _allSongs.value = songs
                refreshAssociatedChoruses()
                refreshAllSongsWithChoruses()
            }
        }

        viewModelScope.launch {
            repository.getAllChoruses().collect { allChoruses ->
                _allChoruses.value = allChoruses
                refreshAllSongsWithChoruses()
            }
        }
    }

    fun loadExercise(exercise: ExerciseModel) {
        id = exercise.id
        name = exercise.name
        weight = exercise.weight.value.toString()
        weightUnit = exercise.weight.unit
        restSeconds = (exercise.restPeriod / 1000).toString()
        chorusDelay = (exercise.chorusDelay / 1000).toString()
        originalName = name
        originalWeight = weight
        originalRestSeconds = restSeconds
        originalChorusDelay = chorusDelay

        viewModelScope.launch {
            val chorusesFlow = repository.getChorusesByExerciseId(exercise.id)
            originalChorusIds = chorusesFlow.first().map { it.id }.toSet()

            chorusesFlow.collect { choruses ->
                _exerciseChoruses.value = choruses
                refreshAssociatedChoruses()
            }
        }
    }

    fun removeChorusFromExercise(chorusId: Long) = viewModelScope.launch {
        repository.removeChorusFromExercise(id, chorusId)
    }

    fun addMultipleChorusesToExercise(chorusIds: Set<Long>) = viewModelScope.launch {
        val alreadyAdded = _selectedChorusIds.value
        val newChorusIds = chorusIds - alreadyAdded

        newChorusIds.forEach { chorusId ->
            repository.addChorusToExercise(id, chorusId)
        }
    }

    fun discardChanges(goBack: () -> Unit = {}) = viewModelScope.launch {
        stopPlayback()

        val currentChorusIds = _selectedChorusIds.value
        val chorusIdsToRemove = currentChorusIds - originalChorusIds
        val chorusIdsToRestore = originalChorusIds - currentChorusIds

        chorusIdsToRemove.forEach { chorusId ->
            repository.removeChorusFromExercise(id, chorusId)
        }

        chorusIdsToRestore.forEach { chorusId ->
            repository.addChorusToExercise(id, chorusId)
        }

        name = originalName
        weight = originalWeight
        restSeconds = originalRestSeconds
        chorusDelay = originalChorusDelay

        goBack()
    }

    fun playChorus(song: SongModel, chorus: ChorusModel) {
        playerManager.play(song.uriString, chorus.startMs.toInt())
    }

    fun stopPlayback() {
        playerManager.stop()
    }

    private fun refreshAssociatedChoruses() {
        val songs = _allSongs.value
        val choruses = _exerciseChoruses.value

        _associatedChoruses.value = choruses.mapNotNull { chorus ->
            songs.find { it.id == chorus.songId }?.let { song ->
                song to chorus
            }
        }

        _selectedChorusIds.value = choruses.map { it.id }.toSet()
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

    fun onChorusDelayChange(newValue: String) {
        if (newValue.all { it.isDigit() } || newValue.isBlank()) {
            chorusDelay = newValue
        }
    }

    fun updateExercise(goBack: () -> Unit = {}) = viewModelScope.launch {
        val trimmedName = name.trim()
        val trimmedWeight = weight.trim()
        val trimmedRest = restSeconds.trim()

        if (trimmedName.isBlank() || trimmedWeight.isBlank() || restSeconds.isBlank()) {
            return@launch
        }

        val restMs = trimmedRest.toLongOrNull()?.let { it * 1000 } ?: return@launch
        val trimmedChorus = chorusDelay.trim()
        val chorusDelayMs = trimmedChorus.toLongOrNull()?.let { it * 1000 } ?: 0L

        repository.updateExercise(
            ExerciseModel(
                id = id,
                name = trimmedName,
                weight = WeightModel(
                    value = trimmedWeight.toInt(),
                    unit = weightUnit,
                ),
                restPeriod = restMs,
                chorusDelay = chorusDelayMs
            )
        )

        goBack()
    }

    companion object {
        fun factory(
            repository: WorkoutRepository,
            playerManager: MusicPlayerManager
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    EditExerciseViewModel(
                        savedStateHandle = createSavedStateHandle(),
                        repository = repository,
                        playerManager = playerManager
                    )
                }
            }
    }
}