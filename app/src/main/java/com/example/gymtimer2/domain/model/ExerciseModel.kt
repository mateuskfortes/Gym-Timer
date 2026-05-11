package com.example.gymtimer2.domain.model

data class ExerciseModel(
    val id: Int = 0,
    val name: String,
    val weight: WeightModel,
    val restPeriod: Long
)