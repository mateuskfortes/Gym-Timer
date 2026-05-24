package com.example.gymtimer2.data.mapper

import androidx.core.net.toUri
import com.example.gymtimer2.data.entity.SongEntity
import com.example.gymtimer2.domain.model.SongModel

fun SongEntity.toModel(): SongModel {
    return SongModel(
        id = id,
        title = title,
        artist = artist,
        uriString = uri,
        durationMs = durationMs
    )
}

fun SongModel.toEntity(): SongEntity {
    return SongEntity(
        id = id,
        title = title,
        artist = artist,
        uri = uriString,
        durationMs = durationMs
    )
}