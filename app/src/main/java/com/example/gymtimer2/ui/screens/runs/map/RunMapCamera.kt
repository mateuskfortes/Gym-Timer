package com.example.gymtimer2.ui.screens.runs.map

import com.example.gymtimer2.domain.model.RunPointModel
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

/** Enquadra a primeira posição ou a rota salva; não é reaplicado nas atualizações GPS. */
internal fun initializeCamera(
    map: MapLibreMap,
    points: List<RunPointModel>,
    followLatestPoint: Boolean,
    initialLocation: LatLng?
) {
    val target = points.lastOrNull()?.let { LatLng(it.latitude, it.longitude) } ?: initialLocation ?: return
    if (followLatestPoint) {
        centerOnRunLocation(map, target)
        return
    }
    val coordinates = points.map { LatLng(it.latitude, it.longitude) }.distinct()
    if (coordinates.size < MIN_ROUTE_POINTS) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, DEFAULT_ZOOM))
        return
    }
    val bounds = coordinates
        .fold(org.maplibre.android.geometry.LatLngBounds.Builder()) { builder, point -> builder.include(point) }
        .build()
    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, CAMERA_PADDING_PX))
    // Percursos muito curtos também precisam mostrar o contexto das ruas ao redor.
    if (map.cameraPosition.zoom > FOLLOW_ZOOM) {
        map.moveCamera(CameraUpdateFactory.zoomTo(FOLLOW_ZOOM))
    }
}

/** Restaura a escala de acompanhamento; o padding mantém a posição acima do painel. */
internal fun centerOnRunLocation(map: MapLibreMap, position: LatLng) {
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, FOLLOW_ZOOM))
}

internal const val MIN_ROUTE_POINTS = 2
private const val DEFAULT_ZOOM = 15.5
private const val FOLLOW_ZOOM = 17.0
private const val CAMERA_PADDING_PX = 72
