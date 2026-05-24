package com.example.gymtimer2.data.mapper

import com.example.gymtimer2.data.relation.ChorusWithSongRelation
import com.example.gymtimer2.domain.model.ChorusWithSongModel

fun ChorusWithSongRelation.toModel(): ChorusWithSongModel {
    return ChorusWithSongModel(
        chorus = chorus.toModel(),
        song = song.toModel()
    )
}