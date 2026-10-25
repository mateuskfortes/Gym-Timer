@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.ui.screens.runs.map

import com.example.gymtimer2.domain.model.RunPointModel
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/** Atualiza apenas a rota e o marcador, preservando a câmera durante a navegação manual. */
internal fun updateRoute(
    map: MapLibreMap,
    points: List<RunPointModel>,
    initialLocation: LatLng?
) {
    val style = map.style ?: return
    val routeSource = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID) ?: return
    val locationSource = style.getSourceAs<GeoJsonSource>(LOCATION_SOURCE_ID) ?: return
    routeSource.setGeoJson(createRoute(points))
    locationSource.setGeoJson(createCurrentLocation(points, initialLocation))
}

/** Cria linhas separadas para que pausas não desenhem atalhos falsos. */
private fun createRoute(points: List<RunPointModel>): FeatureCollection {
    val routeFeatures = points.groupBy(RunPointModel::segmentIndex).values.mapNotNull { segment ->
        if (segment.size < MIN_ROUTE_POINTS) return@mapNotNull null
        val coordinates = segment.map { Point.fromLngLat(it.longitude, it.latitude) }
        Feature.fromGeometry(LineString.fromLngLats(coordinates))
    }
    return FeatureCollection.fromFeatures(routeFeatures.toTypedArray())
}

/** Cria o marcador da última posição GPS válida. */
private fun createCurrentLocation(points: List<RunPointModel>, initialLocation: LatLng?): FeatureCollection {
    val target = points.lastOrNull()?.let { LatLng(it.latitude, it.longitude) } ?: initialLocation
        ?: return FeatureCollection.fromFeatures(emptyArray())
    val marker = Feature.fromGeometry(Point.fromLngLat(target.longitude, target.latitude))
    return FeatureCollection.fromFeatures(arrayOf(marker))
}
