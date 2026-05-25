package com.example.gymtimer2.domain.model

import java.io.Serializable

data class ExerciseModel(
    val id: Int = 0,
    val name: String,
    val weight: WeightModel,
    val restPeriod: Long,
    val chorusDelay: Long
): Serializable