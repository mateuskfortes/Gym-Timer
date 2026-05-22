package com.example.gymtimer2.data.mapper

import androidx.core.net.toUri
import com.example.gymtimer2.data.entity.SongEntity
import com.example.gymtimer2.domain.model.SongModel

fun SongEntity.toDomain(): SongModel {
    return SongModel(
        id = id,
        title = title,
        artist = artist,
        uri = uri.toUri(),
        durationMs = durationMs
    )
}

fun SongModel.toEntity(): SongEntity {
    return SongEntity(
        id = id,
        title = title,
        artist = artist,
        uri = uri.toString(),
        durationMs = durationMs
    )
}