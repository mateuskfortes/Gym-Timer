package com.example.gymtimer2.domain.model

data class MusicPlaybackState(
    val uri: String? = null,
    var currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val isPlaying: Boolean = false
)