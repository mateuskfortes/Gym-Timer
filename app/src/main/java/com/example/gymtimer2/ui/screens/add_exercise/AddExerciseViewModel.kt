package com.example.gymtimer2.ui.screens.add_exercise

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
import com.example.gymtimer2.domain.model.ExerciseModel
import com.example.gymtimer2.domain.model.WeightModel
import com.example.gymtimer2.domain.model.WeightUnit
import kotlinx.coroutines.launch

@OptIn(SavedStateHandleSaveableApi::class)
class AddExerciseViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: WorkoutRepository
) : ViewModel() {

    var name by savedStateHandle.saveable { mutableStateOf("") }
        private set

    var weight by savedStateHandle.saveable { mutableStateOf("") }
        private set

    var restSeconds by savedStateHandle.saveable { mutableStateOf("80") }
        private set

    var weightUnit by savedStateHandle.saveable { mutableStateOf(WeightUnit.TOTAL_KG) }
        private set

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

    fun insertExercise(goBack: () -> Unit = {}) = viewModelScope.launch {
        val trimmedName = name.trim()
        val trimmedWeight = weight.trim()
        val trimmedRest = restSeconds.trim()

        if (trimmedName.isBlank() || trimmedWeight.isBlank() || trimmedRest.isBlank()) {
            return@launch
        }

        val weightValue = trimmedWeight.toIntOrNull() ?: return@launch
        val restMs = trimmedRest.toLongOrNull()?.let { it * 1000 } ?: return@launch

        repository.insertExercise(
            ExerciseModel(
                id = 0,
                name = trimmedName,
                weight = WeightModel(
                    value = weightValue,
                    unit = weightUnit
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
                    AddExerciseViewModel(
                        savedStateHandle = createSavedStateHandle(),
                        repository = repository
                    )
                }
            }
    }
}