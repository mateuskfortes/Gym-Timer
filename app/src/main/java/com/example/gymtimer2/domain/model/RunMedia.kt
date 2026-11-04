package com.example.gymtimer2.domain.model

const val MAX_RUN_MEDIA_COUNT = 10

data class RunMedia(
    val id: Long,
    val fileName: String,
    val isFeatured: Boolean = false,
    val isVideo: Boolean = false
)
