package com.example.gymtimer2.domain.model

/** Câmera usada na sessão aberta, restaurada quando a tela da corrida é reaberta. */
data class RunMapCameraSnapshot(
    val runId: Long,
    val latitude: Double,
    val longitude: Double,
    val zoom: Double,
    val tilt: Double,
    val bearing: Double,
    val followsUser: Boolean
) {
    fun isValid(): Boolean = runId > 0 &&
        latitude.isFinite() && latitude in -90.0..90.0 &&
        longitude.isFinite() && longitude in -180.0..180.0 &&
        zoom.isFinite() && zoom in 0.0..22.0 &&
        tilt.isFinite() && tilt in 0.0..90.0 &&
        bearing.isFinite()
}
