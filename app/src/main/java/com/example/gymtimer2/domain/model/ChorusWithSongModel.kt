package com.example.gymtimer2.domain.model

import java.io.Serializable

data class ChorusWithSongModel(
    val chorus: ChorusModel,
    val song: SongModel
): Serializable
