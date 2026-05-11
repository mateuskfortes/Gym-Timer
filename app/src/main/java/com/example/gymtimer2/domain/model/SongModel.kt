package com.example.gymtimer2.domain.model

import android.net.Uri

data class SongModel(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: Uri,
    val startAtMs: Long = 0,
    val coverBytes: ByteArray? = null
)