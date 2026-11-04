package com.example.gymtimer2.data.repository

import com.example.gymtimer2.domain.model.PlannedRoutePoint

internal fun decodePolyline6(encoded: String): List<PlannedRoutePoint> {
    require(encoded.isNotEmpty() && encoded.length <= MAX_ENCODED_POLYLINE_LENGTH)
    var index = 0
    var latitude = 0L
    var longitude = 0L
    val points = ArrayList<PlannedRoutePoint>()

    fun readDelta(): Long {
        var value = 0L
        var shift = 0
        while (true) {
            require(index < encoded.length && shift <= MAX_POLYLINE_SHIFT)
            val character = encoded[index++].code - POLYLINE_ASCII_OFFSET
            require(character in 0..POLYLINE_CHARACTER_MAX)
            value = value or ((character and POLYLINE_VALUE_MASK).toLong() shl shift)
            if (character < POLYLINE_CONTINUATION_THRESHOLD) break
            shift += POLYLINE_SHIFT_BITS
        }
        return if (value and 1L == 1L) -((value shr 1) + 1L) else value shr 1
    }

    while (index < encoded.length) {
        latitude += readDelta()
        longitude += readDelta()
        val latitudeDegrees = latitude / POLYLINE6_SCALE
        val longitudeDegrees = longitude / POLYLINE6_SCALE
        require(latitudeDegrees in -90.0..90.0 && longitudeDegrees in -180.0..180.0)
        require(points.size < MAX_DECODED_ROUTE_POINTS)
        points += PlannedRoutePoint(latitudeDegrees, longitudeDegrees, segmentIndex = 0)
    }

    require(points.size >= MIN_ROUTE_POINTS)
    return points
}

private const val POLYLINE6_SCALE = 1_000_000.0
private const val POLYLINE_ASCII_OFFSET = 63
private const val POLYLINE_CHARACTER_MAX = 63
private const val POLYLINE_VALUE_MASK = 0x1f
private const val POLYLINE_CONTINUATION_THRESHOLD = 0x20
private const val POLYLINE_SHIFT_BITS = 5
private const val MAX_POLYLINE_SHIFT = 25
private const val MAX_ENCODED_POLYLINE_LENGTH = 1_000_000
private const val MAX_DECODED_ROUTE_POINTS = 50_000
private const val MIN_ROUTE_POINTS = 2
