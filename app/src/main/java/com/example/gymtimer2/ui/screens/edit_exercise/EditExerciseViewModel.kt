package com.example.gymtimer2.ui.screens.edit_exercise

import androidx.compose.runtime.mutableStateOf
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
import com.example.gymtimer2.domain.model.ExerciseModel
import com.example.gymtimer2.domain.model.WeightModel
import com.example.gymtimer2.domain.model.WeightUnit
import kotlinx.coroutines.launch
import kotlin.properties.Delegates
@OptIn(SavedStateHandleSaveableApi::class)
class EditExerciseViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: WorkoutRepository
) : ViewModel() {

    private var id: Int by Delegates.notNull()

    private var weightUnit: WeightUnit by Delegates.notNull()

    var name by savedStateHandle.saveable { mutableStateOf("") }
        private set

    var weight by savedStateHandle.saveable { mutableStateOf("") }
        private set

    var restSeconds by savedStateHandle.saveable { mutableStateOf("") }
        private set

    fun loadExercise(exercise: ExerciseModel) {
        id = exercise.id
        name = exercise.name
        weight = exercise.weight.value.toString()
        weightUnit = exercise.weight.unit
        restSeconds = (exercise.restPeriod / 1000).toString()
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

    fun updateExercise(goBack: () -> Unit = {}) = viewModelScope.launch {
        val trimmedName = name.trim()
        val trimmedWeight = weight.trim()
        val trimmedRest = restSeconds.trim()

        if (trimmedName.isBlank() || trimmedWeight.isBlank() || restSeconds.isBlank()) {
            return@launch
        }

        val restMs = trimmedRest.toLongOrNull()?.let { it * 1000 } ?: return@launch

        repository.updateExercise(
            ExerciseModel(
                id = id,
                name = trimmedName,
                weight = WeightModel(
                    value = trimmedWeight.toInt(),
                    unit = weightUnit,
                ),
                restPeriod = restMs
            )
        )

        goBack()
    }

    companion object {
        fun factory(repository: WorkoutRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    EditExerciseViewModel(
                        savedStateHandle = createSavedStateHandle(),
                        repository = repository
                    )
                }
            }
    }
}