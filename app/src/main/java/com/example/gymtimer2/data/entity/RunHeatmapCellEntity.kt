package com.example.gymtimer2.data.entity

/** Projeção Room com uma célula agregada do histórico de corridas. */
data class RunHeatmapCellEntity(
    val latitude: Double,
    val longitude: Double,
    val visitCount: Long
)
