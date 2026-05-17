package com.example.gymtimer2.data.mapper

import com.example.gymtimer2.data.entity.ChorusEntity
import com.example.gymtimer2.domain.model.ChorusModel

fun ChorusEntity.toDomain(): ChorusModel {
    return ChorusModel(
        id = id,
        songId = songId,
        name = name,
        startMs = startMs
    )
}

fun ChorusModel.toEntity(): ChorusEntity {
    return ChorusEntity(
        id = id,
        songId = songId,
        name = name,
        startMs = startMs
    )
}

