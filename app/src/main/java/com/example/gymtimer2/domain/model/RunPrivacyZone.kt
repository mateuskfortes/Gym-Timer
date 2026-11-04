package com.example.gymtimer2.domain.model

data class RunPrivacyZone(
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int
) {
    companion object {
        const val MAX_NAME_LENGTH = 80
        val RADIUS_OPTIONS_METERS = (1..8).map { it * 200 }
    }
}
