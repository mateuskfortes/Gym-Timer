package com.example.gymtimer2.domain.model

import kotlin.math.abs

/** Um trecho contínuo de uma corrida que pode virar um segmento local. */
data class RunLocalSegmentTrack(
    val segmentIndex: Int,
    val points: List<RunPointModel>,
    val cumulativeDistanceMeters: List<Double>
) {
    val distanceMeters: Double
        get() = cumulativeDistanceMeters.lastOrNull() ?: 0.0
}

/** Prepara trechos GPS e seleciona seus limites sem ligar pontos de pausas distintas. */
object RunLocalSegmentSelection {
    /** Agrupa pontos válidos por trecho contínuo e omite percursos curtos demais para competir. */
    fun tracks(points: List<RunPointModel>): List<RunLocalSegmentTrack> = points
        .asSequence()
        .filter { !it.isFiltered && it.hasValidRouteCoordinates() }
        .groupBy(RunPointModel::segmentIndex)
        .toSortedMap()
        .mapNotNull { (segmentIndex, segmentPoints) ->
            val orderedPoints = segmentPoints.sortedBy(RunPointModel::recordedAtEpochMs)
            if (orderedPoints.size < MIN_TRACK_POINTS) return@mapNotNull null
            val cumulative = cumulativeDistances(orderedPoints)
            if (cumulative.last() < MIN_LOCAL_SEGMENT_DISTANCE_METERS) return@mapNotNull null
            RunLocalSegmentTrack(segmentIndex, orderedPoints, cumulative)
        }

    /** Retorna os pontos entre os limites escolhidos, aproximados ao GPS mais próximo. */
    fun pointsBetween(
        track: RunLocalSegmentTrack,
        startDistanceMeters: Double,
        endDistanceMeters: Double
    ): List<RunPointModel> {
        if (!startDistanceMeters.isFinite() || !endDistanceMeters.isFinite()) return emptyList()
        if (startDistanceMeters < 0.0 || endDistanceMeters > track.distanceMeters ||
            endDistanceMeters - startDistanceMeters < MIN_LOCAL_SEGMENT_DISTANCE_METERS
        ) return emptyList()

        val startIndex = nearestDistanceIndex(track.cumulativeDistanceMeters, startDistanceMeters)
        val endIndex = nearestDistanceIndex(track.cumulativeDistanceMeters, endDistanceMeters)
        if (endIndex <= startIndex) return emptyList()

        val selectedPoints = track.points.subList(startIndex, endIndex + 1)
        val selectedDistance = track.cumulativeDistanceMeters[endIndex] -
            track.cumulativeDistanceMeters[startIndex]
        return selectedPoints.takeIf { selectedDistance >= MIN_LOCAL_SEGMENT_DISTANCE_METERS }.orEmpty()
    }

    private fun cumulativeDistances(points: List<RunPointModel>): List<Double> = buildList(points.size) {
        add(0.0)
        points.zipWithNext().forEach { (previous, current) ->
            add(last() + distanceMetersBetweenCoordinates(
                previous.latitude,
                previous.longitude,
                current.latitude,
                current.longitude
            ))
        }
    }

    private fun nearestDistanceIndex(distances: List<Double>, targetDistanceMeters: Double): Int =
        distances.indices.minByOrNull { abs(distances[it] - targetDistanceMeters) } ?: 0

    private fun RunPointModel.hasValidRouteCoordinates(): Boolean =
        latitude.isFinite() && latitude in -90.0..90.0 &&
            longitude.isFinite() && longitude in -180.0..180.0

    private const val MIN_TRACK_POINTS = 2
}
