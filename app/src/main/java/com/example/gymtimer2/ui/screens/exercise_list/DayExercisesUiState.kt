package com.example.gymtimer2.ui.screens.exercise_list

import com.example.gymtimer2.domain.model.ScheduledExerciseModel

sealed interface DayExercisesUiState {
    data object Idle : DayExercisesUiState
    data class Loading(val dayId: Long) : DayExercisesUiState
    data class Loaded(val dayId: Long, val exercises: List<ScheduledExerciseModel>) : DayExercisesUiState
    data class Failed(val dayId: Long) : DayExercisesUiState
}
