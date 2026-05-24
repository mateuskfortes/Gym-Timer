package com.example.gymtimer2.ui.screens.exercise_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.gymtimer2.data.repository.WorkoutRepository
import com.example.gymtimer2.domain.model.ChorusWithSongModel
import com.example.gymtimer2.domain.model.ExerciseModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(SavedStateHandleSaveableApi::class)
class ExerciseListViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    val allExercises = repository.allExercises

    fun deleteExercise(exercise: ExerciseModel) = viewModelScope.launch {
        repository.deleteExercise(exercise)
    }

    fun startExercise(
        exercise: ExerciseModel,
        onOpen: (ExerciseModel, List<ChorusWithSongModel>) -> Unit
    ) {
        viewModelScope.launch {
            val choruses =
                repository.getChorusesWithSongsByExerciseId(exercise.id).first()

            onOpen(exercise, choruses)
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