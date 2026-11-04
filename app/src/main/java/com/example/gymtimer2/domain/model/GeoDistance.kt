package com.example.gymtimer2.domain.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Distância de superfície entre duas coordenadas geográficas, em metros. */
fun distanceMetersBetweenCoordinates(
    latitude1: Double,
    longitude1: Double,
    latitude2: Double,
    longitude2: Double
): Double {
    val latitudeDelta = Math.toRadians(latitude2 - latitude1)
    val longitudeDelta = Math.toRadians(longitude2 - longitude1)
    val firstLatitude = Math.toRadians(latitude1)
    val secondLatitude = Math.toRadians(latitude2)
    val sinLatitude = sin(latitudeDelta / 2)
    val sinLongitude = sin(longitudeDelta / 2)
    val haversine = sinLatitude * sinLatitude +
        cos(firstLatitude) * cos(secondLatitude) * sinLongitude * sinLongitude
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(haversine.coerceIn(0.0, 1.0)))
}

/** Confere a distância do início de uma rota ao usuário antes de iniciar o percurso. */
fun isRunRouteStartFarFromLocation(
    latitude: Double,
    longitude: Double,
    route: List<RoutePoint>
): Boolean {
    if (!hasValidCoordinates(latitude, longitude)) return false
    val start = route.firstOrNull { !it.isFiltered && hasValidCoordinates(it.latitude, it.longitude) } ?: return false
    return distanceMetersBetweenCoordinates(latitude, longitude, start.latitude, start.longitude) >
        FAR_ROUTE_WARNING_DISTANCE_METERS
}

private fun hasValidCoordinates(latitude: Double, longitude: Double): Boolean =
    latitude.isFinite() && latitude in -90.0..90.0 &&
        longitude.isFinite() && longitude in -180.0..180.0

private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val FAR_ROUTE_WARNING_DISTANCE_METERS = 250_000.0
