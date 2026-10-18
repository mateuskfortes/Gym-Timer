package com.example.gymtimer2.domain.model

data class SongModel(
    val id: Long,
    val title: String,
    val artist: String,
    val uriString: String,
    val durationMs: Long? = null,
    val albumId: Long? = null
)
