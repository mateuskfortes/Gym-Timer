package com.example.gymtimer2.data.entity

import com.example.gymtimer2.domain.model.PlannedRoutePoint

internal fun PlannedRoutePointEntity.toModel() = PlannedRoutePoint(
    latitude = latitude,
    longitude = longitude,
    segmentIndex = segmentIndex,
    altitudeMeters = altitudeMeters
)
