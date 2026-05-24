package com.example.gymtimer2.data.mapper

import com.example.gymtimer2.data.relation.ExerciseWithChorusesRelation
import com.example.gymtimer2.domain.model.ExerciseWithChorusesModel

fun ExerciseWithChorusesRelation.toModel(): ExerciseWithChorusesModel {
    return ExerciseWithChorusesModel(
        exercise = exercise.toModel(),
        choruses = choruses.map { it.toModel() }
    )
}