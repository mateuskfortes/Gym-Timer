package com.example.gymtimer2.domain.model

/** Equipamento salvo neste dispositivo e distância acumulada nas atividades concluídas. */
data class RunGear(
    val id: Long,
    val name: String,
    val distanceMeters: Double,
    val isDefault: Boolean,
    val isRetired: Boolean = false,
    val photoFileName: String? = null
)
