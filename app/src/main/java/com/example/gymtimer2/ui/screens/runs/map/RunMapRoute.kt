@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.ui.screens.runs.map

import android.content.Context
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RoutePoint
import com.example.gymtimer2.domain.model.RunGradientDirection
import com.example.gymtimer2.domain.model.RunGradientSegment
import com.example.gymtimer2.domain.model.RunMapPoi
import com.example.gymtimer2.domain.model.RunMapPoiType
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunHeatmapCell
import org.maplibre.android.maps.Style
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/** Atualiza o percurso-alvo, o traçado gravado e o marcador sem mover a câmera manualmente. */
internal fun updateRoute(
    map: MapLibreMap,
    points: List<RunPointModel>,
    plannedPoints: List<RoutePoint>,
    returnRoutePoints: List<RoutePoint>,
    initialLocation: LatLng?
) {
    val style = map.style ?: return
    val plannedRouteSource = style.getSourceAs<GeoJsonSource>(PLANNED_ROUTE_SOURCE_ID) ?: return
    val routeSource = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID) ?: return
    val returnRouteSource = style.getSourceAs<GeoJsonSource>(RETURN_ROUTE_SOURCE_ID) ?: return
    val returnStartSource = style.getSourceAs<GeoJsonSource>(RETURN_START_SOURCE_ID) ?: return
    val locationSource = style.getSourceAs<GeoJsonSource>(LOCATION_SOURCE_ID) ?: return
    routeSource.setGeoJson(createRoute(points.filterNot(RunPointModel::isFiltered)))
    plannedRouteSource.setGeoJson(createRoute(plannedPoints.filterNot(RoutePoint::isFiltered)))
    returnRouteSource.setGeoJson(createRoute(returnRoutePoints))
    returnStartSource.setGeoJson(createReturnStart(returnRoutePoints))
    locationSource.setGeoJson(createCurrentLocation(points, initialLocation))
}

internal fun updateGradientRouteLayers(map: MapLibreMap, segments: List<RunGradientSegment>) {
    map.style?.let { updateGradientRoutes(it, segments) }
}

internal fun updateActivityHeatmap(
    map: MapLibreMap,
    weeklyCells: List<RunHeatmapCell>,
    historyCells: List<RunHeatmapCell>,
    selectedLayers: Set<RunMapLayer>
) {
    val source = map.style?.getSourceAs<GeoJsonSource>(WEEKLY_HEATMAP_SOURCE_ID) ?: return
    source.setGeoJson(createActivityHeatmap(weeklyCells, historyCells, selectedLayers))
}

internal fun updateOpenStreetMapPois(map: MapLibreMap, context: Context, pois: List<RunMapPoi>) {
    val source = map.style?.getSourceAs<GeoJsonSource>(OPENSTREETMAP_POI_SOURCE_ID) ?: return
    val features = pois.map { poi ->
        Feature.fromGeometry(Point.fromLngLat(poi.longitude, poi.latitude)).apply {
            addStringProperty("label", poi.name ?: context.getString(poi.type.labelResource()))
            addStringProperty("type", poi.type.name)
            poi.name?.let { addStringProperty("name", it) }
            poi.address?.let { addStringProperty("address", it) }
            poi.openingHours?.let { addStringProperty("opening_hours", it) }
            poi.phone?.let { addStringProperty("phone", it) }
            poi.website?.let { addStringProperty("website", it) }
        }
    }
    source.setGeoJson(FeatureCollection.fromFeatures(features.toTypedArray()))
}

internal fun Feature.toRunMapPoiOrNull(): RunMapPoi? {
    val point = geometry() as? Point ?: return null
    val typeName = getStringProperty("type")
    val type = RunMapPoiType.entries.firstOrNull { it.name == typeName } ?: return null
    return RunMapPoi(
        latitude = point.latitude(),
        longitude = point.longitude(),
        name = stringPropertyOrNull("name"),
        type = type,
        address = stringPropertyOrNull("address"),
        openingHours = stringPropertyOrNull("opening_hours"),
        phone = stringPropertyOrNull("phone"),
        website = stringPropertyOrNull("website")
    )
}

private fun Feature.stringPropertyOrNull(name: String): String? =
    takeIf { hasProperty(name) }?.getStringProperty(name)?.takeIf(String::isNotBlank)

private fun createActivityHeatmap(
    weeklyCells: List<RunHeatmapCell>,
    historyCells: List<RunHeatmapCell>,
    selectedLayers: Set<RunMapLayer>
): FeatureCollection {
    val features = buildList {
        if (RunMapLayer.WEEKLY_ACTIVITY in selectedLayers) addAll(weeklyCells.toHeatmapFeatures())
        if (RunMapLayer.LOCAL_HISTORY in selectedLayers) addAll(historyCells.toHeatmapFeatures())
    }
    return FeatureCollection.fromFeatures(features.toTypedArray())
}

private fun List<RunHeatmapCell>.toHeatmapFeatures(): List<Feature> {
    val maxVisits = maxOfOrNull(RunHeatmapCell::visitCount)?.coerceAtLeast(1L) ?: return emptyList()
    return map { cell ->
        Feature.fromGeometry(Point.fromLngLat(cell.longitude, cell.latitude)).apply {
            addNumberProperty("weight", cell.visitCount.toDouble() / maxVisits)
        }
    }
}

internal fun RunMapPoiType.labelResource(): Int = when (this) {
    RunMapPoiType.WATER -> R.string.run_map_poi_water
    RunMapPoiType.TOILETS -> R.string.run_map_poi_toilets
    RunMapPoiType.SHELTER -> R.string.run_map_poi_shelter
    RunMapPoiType.BENCH -> R.string.run_map_poi_bench
    RunMapPoiType.FOOD -> R.string.run_map_poi_food
    RunMapPoiType.PARK -> R.string.run_map_poi_park
}

private fun updateGradientRoutes(style: Style, segments: List<RunGradientSegment>) {
    val segmentsByDirection = segments.groupBy(RunGradientSegment::direction)
    updateGradientSource(style, GRADIENT_DESCENT_SOURCE_ID, segmentsByDirection[RunGradientDirection.DESCENT].orEmpty())
    updateGradientSource(style, GRADIENT_LEVEL_SOURCE_ID, segmentsByDirection[RunGradientDirection.LEVEL].orEmpty())
    updateGradientSource(style, GRADIENT_ASCENT_SOURCE_ID, segmentsByDirection[RunGradientDirection.ASCENT].orEmpty())
}

private fun updateGradientSource(
    style: Style,
    sourceId: String,
    segments: List<RunGradientSegment>
) {
    style.getSourceAs<GeoJsonSource>(sourceId)?.setGeoJson(createGradientRoute(segments))
}

private fun createGradientRoute(segments: List<RunGradientSegment>): FeatureCollection {
    val features = segments.map { segment ->
        val coordinates = listOf(
            Point.fromLngLat(segment.start.longitude, segment.start.latitude),
            Point.fromLngLat(segment.end.longitude, segment.end.latitude)
        )
        Feature.fromGeometry(LineString.fromLngLats(coordinates))
    }
    return FeatureCollection.fromFeatures(features.toTypedArray())
}

/** Cria linhas separadas para que pausas não desenhem atalhos falsos. */
private fun createRoute(points: List<RoutePoint>): FeatureCollection {
    val routeFeatures = points.groupBy(RoutePoint::segmentIndex).values.mapNotNull { segment ->
        if (segment.size < MIN_ROUTE_POINTS) return@mapNotNull null
        val coordinates = segment.map { Point.fromLngLat(it.longitude, it.latitude) }
        Feature.fromGeometry(LineString.fromLngLats(coordinates))
    }
    return FeatureCollection.fromFeatures(routeFeatures.toTypedArray())
}

/** Cria o marcador da última posição GPS válida. */
private fun createCurrentLocation(points: List<RunPointModel>, initialLocation: LatLng?): FeatureCollection {
    val target = points.lastOrNull { !it.isFiltered }?.let { LatLng(it.latitude, it.longitude) } ?: initialLocation
        ?: return FeatureCollection.fromFeatures(emptyArray())
    val marker = Feature.fromGeometry(Point.fromLngLat(target.longitude, target.latitude))
    return FeatureCollection.fromFeatures(arrayOf(marker))
}

/** Marca o início registrado, que é o destino ao percorrer a linha ao contrário. */
private fun createReturnStart(points: List<RoutePoint>): FeatureCollection {
    val destination = points.lastOrNull()
        ?: return FeatureCollection.fromFeatures(emptyArray())
    val marker = Feature.fromGeometry(Point.fromLngLat(destination.longitude, destination.latitude))
    return FeatureCollection.fromFeatures(arrayOf(marker))
}
