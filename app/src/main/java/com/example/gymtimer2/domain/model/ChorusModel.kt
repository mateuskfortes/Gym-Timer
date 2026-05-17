package com.example.gymtimer2.domain.model

data class ChorusModel(
    val id: Long = 0,
    val songId: Long,
    val name: String = "",
    val startMs: Long
)

