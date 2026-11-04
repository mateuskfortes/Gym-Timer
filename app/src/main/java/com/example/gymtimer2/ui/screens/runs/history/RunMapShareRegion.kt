package com.example.gymtimer2.ui.screens.runs.history

import com.example.gymtimer2.data.exporting.RunExportPrivacy
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunPrivacyZone

internal data class RunMapShareRegion(
    val northLatitude: Double,
    val eastLongitude: Double,
    val southLatitude: Double,
    val westLongitude: Double
)

internal fun runMapShareablePoints(
    run: RunModel,
    points: List<RunPointModel>,
    privacyZones: List<RunPrivacyZone>
): List<RunPointModel> = RunExportPrivacy.visiblePoints(run, points, privacyZones)

internal fun shareableRunSegments(points: List<RunPointModel>): List<List<RunPointModel>> =
    points.asSequence()
        .filterNot(RunPointModel::isFiltered)
        .filter(RunPointModel::hasValidShareCoordinate)
        .groupBy(RunPointModel::segmentIndex)
        .values
        .mapNotNull { segment -> segment.takeIf { it.size >= MIN_SHAREABLE_ROUTE_POINTS } }

internal fun runMapShareRegion(points: List<RunPointModel>): RunMapShareRegion? {
    val routePoints = shareableRunSegments(points).flatten()
    if (routePoints.size < MIN_SHAREABLE_ROUTE_POINTS) return null

    val north = routePoints.maxOf(RunPointModel::latitude)
    val south = routePoints.minOf(RunPointModel::latitude)
    val east = routePoints.maxOf(RunPointModel::longitude)
    val west = routePoints.minOf(RunPointModel::longitude)
    val latitudePadding = ((north - south).coerceAtLeast(MIN_REGION_SPAN) - (north - south)) / 2
    val longitudePadding = ((east - west).coerceAtLeast(MIN_REGION_SPAN) - (east - west)) / 2

    return RunMapShareRegion(
        northLatitude = (north + latitudePadding).coerceAtMost(MAX_MAP_LATITUDE),
        eastLongitude = (east + longitudePadding).coerceAtMost(MAX_LONGITUDE),
        southLatitude = (south - latitudePadding).coerceAtLeast(-MAX_MAP_LATITUDE),
        westLongitude = (west - longitudePadding).coerceAtLeast(-MAX_LONGITUDE)
    )
}

private fun RunPointModel.hasValidShareCoordinate(): Boolean =
    latitude.isFinite() && latitude in -MAX_MAP_LATITUDE..MAX_MAP_LATITUDE &&
        longitude.isFinite() && longitude in -MAX_LONGITUDE..MAX_LONGITUDE

private const val MIN_SHAREABLE_ROUTE_POINTS = 2
private const val MIN_REGION_SPAN = 0.001
private const val MAX_MAP_LATITUDE = 85.05112878
private const val MAX_LONGITUDE = 180.0
