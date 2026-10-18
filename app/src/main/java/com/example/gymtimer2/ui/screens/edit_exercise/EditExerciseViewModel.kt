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
import com.example.gymtimer2.domain.model.ScheduledExerciseModel
import com.example.gymtimer2.domain.model.MusicPlaybackState
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.domain.model.WeightModel
import com.example.gymtimer2.domain.model.WeightUnit
import com.example.gymtimer2.player.MusicPlayerManager
import com.example.gymtimer2.util.secondsToMillisOrNull
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

    private data class ActiveChorusPlayback(
        val chorusId: Long,
        val songUri: String
    )

    val days = repository.workoutDays

    var selectedDayId by savedStateHandle.saveable { mutableStateOf<Long?>(null) }
        private set

    fun selectDay(dayId: Long) {
        selectedDayId = dayId
    }

    private var id: Int by Delegates.notNull()

    private var weightUnit: WeightUnit by Delegates.notNull()

    private var originalName: String = ""

    private var originalWeight: String = ""

    private var originalRestSeconds: String = ""

    private var originalChorusDelay: String = ""

    private var originalChorusIds: Set<Long> = emptySet()

    private var activeChorusPlayback: ActiveChorusPlayback? = null

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

    private val _selectedChorusIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedChorusIds: StateFlow<Set<Long>> = _selectedChorusIds.asStateFlow()

    private val _chorusesLoaded = MutableStateFlow(false)
    val chorusesLoaded: StateFlow<Boolean> = _chorusesLoaded.asStateFlow()

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
                refreshAssociatedChoruses()
                refreshAllSongsWithChoruses()
            }
        }
    }

    fun loadExercise(scheduled: ScheduledExerciseModel) {
        val exercise = scheduled.exercise
        selectedDayId = scheduled.dayId
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
        _chorusesLoaded.value = false

        viewModelScope.launch {
            val chorusesFlow = repository.getChorusesByExerciseId(exercise.id)
            originalChorusIds = chorusesFlow.first().map { it.id }.toSet()
            _selectedChorusIds.value = originalChorusIds
            _chorusesLoaded.value = true
            refreshAssociatedChoruses()
        }
    }

    fun removeChorusFromExercise(chorusId: Long) {
        val activePlayback = activeChorusPlayback
        if (activePlayback?.chorusId == chorusId && playbackState.value.uri == activePlayback.songUri) {
            stopPlayback()
        }

        _selectedChorusIds.value = _selectedChorusIds.value - chorusId
        refreshAssociatedChoruses()
    }

    fun addMultipleChorusesToExercise(chorusIds: Set<Long>) {
        _selectedChorusIds.value = _selectedChorusIds.value + chorusIds
        refreshAssociatedChoruses()
    }

    fun discardChanges(goBack: () -> Unit = {}) = viewModelScope.launch {
        stopPlayback()
        _selectedChorusIds.value = originalChorusIds
        refreshAssociatedChoruses()

        name = originalName
        weight = originalWeight
        restSeconds = originalRestSeconds
        chorusDelay = originalChorusDelay

        goBack()
    }

    fun playChorus(song: SongModel, chorus: ChorusModel) {
        activeChorusPlayback = ActiveChorusPlayback(chorus.id, song.uriString)
        playerManager.play(song.uriString, chorus.startMs.toInt())
    }

    fun stopPlayback() {
        activeChorusPlayback = null
        playerManager.stop()
    }

    private fun refreshAssociatedChoruses() {
        val songs = _allSongs.value
        val selectedChorusIds = _selectedChorusIds.value

        _associatedChoruses.value = _allChoruses.value
            .filter { chorus -> chorus.id in selectedChorusIds }
            .mapNotNull { chorus ->
                songs.find { it.id == chorus.songId }?.let { song -> song to chorus }
            }
            .sortedBy { (_, chorus) -> chorus.startMs }
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
        if (!_chorusesLoaded.value) return@launch

        val trimmedName = name.trim()
        val trimmedWeight = weight.trim()
        val trimmedRest = restSeconds.trim()

        if (trimmedName.isBlank() || trimmedWeight.isBlank() || restSeconds.isBlank()) {
            return@launch
        }

        val parsedWeight = trimmedWeight.toIntOrNull() ?: return@launch
        val restMs = secondsToMillisOrNull(trimmedRest) ?: return@launch
        val trimmedChorus = chorusDelay.trim()
        val chorusDelayMs = secondsToMillisOrNull(trimmedChorus) ?: return@launch

        repository.updateExercise(
            ExerciseModel(
                id = id,
                name = trimmedName,
                weight = WeightModel(
                    value = parsedWeight,
                    unit = weightUnit,
                ),
                restPeriod = restMs,
                chorusDelay = chorusDelayMs
            )
        )
        repository.replaceChorusesForExercise(id, _selectedChorusIds.value)

        selectedDayId?.let { repository.moveExercise(id, it) }

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
