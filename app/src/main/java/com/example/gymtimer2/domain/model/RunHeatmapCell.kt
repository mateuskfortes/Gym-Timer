package com.example.gymtimer2.domain.model

/** Célula do mapa de calor pessoal, com a quantidade de leituras GPS agrupadas. */
data class RunHeatmapCell(
    val latitude: Double,
    val longitude: Double,
    val visitCount: Long
)
