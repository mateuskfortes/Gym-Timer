package com.example.gymtimer2.domain.model

data class ExerciseWithChorusesModel (
    val exercise: ExerciseModel,
    val choruses: List<ChorusWithSongModel>
)