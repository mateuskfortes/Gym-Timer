package com.example.gymtimer2.domain.model

data class RunMapBounds(
    val southLatitude: Double,
    val westLongitude: Double,
    val northLatitude: Double,
    val eastLongitude: Double
)

data class RunMapPoi(
    val latitude: Double,
    val longitude: Double,
    val name: String?,
    val type: RunMapPoiType,
    val address: String? = null,
    val openingHours: String? = null,
    val phone: String? = null,
    val website: String? = null
)

enum class RunMapPoiType { WATER, TOILETS, SHELTER, BENCH, FOOD, PARK }
