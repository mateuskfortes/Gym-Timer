package com.example.gymtimer2.ui.screens.exercise_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.gymtimer2.data.repository.WorkoutRepository
import com.example.gymtimer2.domain.model.ExerciseModel
import com.example.gymtimer2.domain.model.WorkoutScheduleError
import com.example.gymtimer2.domain.model.WorkoutScheduleException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(SavedStateHandleSaveableApi::class)
class ExerciseListViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    val days = repository.workoutDays
    private val selectedDayId = MutableStateFlow<Long?>(null)
    private val _dayExercises = MutableStateFlow<DayExercisesUiState>(DayExercisesUiState.Idle)
    val dayExercises = _dayExercises.asStateFlow()

    private val _error = MutableStateFlow<WorkoutScheduleError?>(null)
    val error = _error.asStateFlow()

    init {
        viewModelScope.launch {
            selectedDayId.collectLatest { dayId ->
                if (dayId == null) {
                    _dayExercises.value = DayExercisesUiState.Idle
                    return@collectLatest
                }
                _dayExercises.value = DayExercisesUiState.Loading(dayId)
                try {
                    repository.scheduledExercisesForDay(dayId).collect { exercises ->
                        _dayExercises.value = DayExercisesUiState.Loaded(dayId, exercises)
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    _dayExercises.value = DayExercisesUiState.Failed(dayId)
                }
            }
        }
    }

    fun selectDay(dayId: Long?) {
        selectedDayId.value = dayId
    }

    fun clearError() {
        _error.value = null
    }

    fun createDay(name: String, onCreated: (Long) -> Unit) = launchAction {
        onCreated(repository.createDay(name))
    }

    fun renameDay(dayId: Long, name: String) = launchAction {
        repository.renameDay(dayId, name)
    }

    fun deleteDay(dayId: Long) = launchAction {
        repository.deleteDay(dayId)
    }

    fun reorder(dayId: Long, orderedIds: List<Int>) = launchAction {
        repository.reorderExercises(dayId, orderedIds)
    }

    fun deleteExercise(exercise: ExerciseModel) = launchAction {
        repository.deleteExercise(exercise)
    }

    private fun launchAction(block: suspend () -> Unit) = viewModelScope.launch {
        try {
            block()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: WorkoutScheduleException) {
            _error.value = exception.reason
        } catch (_: Exception) {
            _error.value = WorkoutScheduleError.SAVE_FAILED
        }
    }

    companion object {
        fun factory(repository: WorkoutRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    ExerciseListViewModel(
                        repository = repository
                    )
                }
            }
    }
}
