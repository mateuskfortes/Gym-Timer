package com.example.gymtimer2.data.mapper

import com.example.gymtimer2.data.entity.ExerciseEntity
import com.example.gymtimer2.domain.model.ChorusModel
import com.example.gymtimer2.domain.model.ExerciseModel
import com.example.gymtimer2.domain.model.WeightModel
import com.example.gymtimer2.domain.model.WeightUnit

fun ExerciseEntity.toModel(): ExerciseModel {
    return ExerciseModel(
        id = id,
        name = name,
        weight = WeightModel(
            value = weight,
            unit = WeightUnit.fromCode(weightUnitCode)
        ),
        restPeriod = restPeriod,
        chorusDelay = chorusDelay
    )
}

fun ExerciseModel.toEntity(): ExerciseEntity {
    return ExerciseEntity(
        id = id,
        name = name,
        weight = weight.value,
        weightUnitCode = weight.unit.code,
        restPeriod = restPeriod,
        chorusDelay = chorusDelay
    )
}