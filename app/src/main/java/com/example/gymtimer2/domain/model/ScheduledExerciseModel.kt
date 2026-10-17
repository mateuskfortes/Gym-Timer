package com.example.gymtimer2.domain.model

data class ScheduledExerciseModel(
    val exercise: ExerciseModel,
    val dayId: Long,
    val sortOrder: Int
)
