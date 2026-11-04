package com.example.gymtimer2.ui.screens.runs.map

import android.content.Context
import android.graphics.Color
import com.example.gymtimer2.R
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.HeatmapLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.expressions.Expression.color
import org.maplibre.android.style.expressions.Expression.heatmapDensity
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.interpolate
import org.maplibre.android.style.expressions.Expression.linear
import org.maplibre.android.style.expressions.Expression.stop
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.heatmapColor
import org.maplibre.android.style.layers.PropertyFactory.heatmapIntensity
import org.maplibre.android.style.layers.PropertyFactory.heatmapWeight
import org.maplibre.android.style.layers.PropertyFactory.heatmapOpacity
import org.maplibre.android.style.layers.PropertyFactory.heatmapRadius
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineDasharray
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineOffset
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource

/** Lê o estilo OSM local; os tiles são carregados só para a área visível. */
internal fun readMapStyle(context: Context): String =
    context.resources.openRawResource(R.raw.run_map_style).bufferedReader().use { it.readText() }

/** Adiciona estilos para rotas, inclinação e marcadores sobre o mapa. */
internal fun addRouteLayers(style: Style) {
    style.addSource(GeoJsonSource(WEEKLY_HEATMAP_SOURCE_ID))
    style.addLayer(
        HeatmapLayer(WEEKLY_HEATMAP_LAYER_ID, WEEKLY_HEATMAP_SOURCE_ID).withProperties(
            heatmapColor(
                interpolate(
                    linear(),
                    heatmapDensity(),
                    stop(0.0, color(Color.TRANSPARENT)),
                    stop(0.2, color(Color.rgb(255, 235, 59))),
                    stop(0.55, color(Color.rgb(255, 152, 0))),
                    stop(1.0, color(Color.rgb(244, 67, 54)))
                )
            ),
            heatmapRadius(22f),
            heatmapWeight(get("weight")),
            heatmapIntensity(0.7f),
            heatmapOpacity(0.78f)
        )
    )
    style.addSource(GeoJsonSource(PLANNED_ROUTE_SOURCE_ID))
    style.addLayer(
        LineLayer(PLANNED_ROUTE_LAYER_ID, PLANNED_ROUTE_SOURCE_ID).withProperties(
            lineColor(Color.rgb(66, 133, 244)),
            lineWidth(4f),
            lineCap(ROUNDED_LINE),
            lineJoin(ROUNDED_LINE)
        )
    )
    style.addSource(GeoJsonSource(ROUTE_SOURCE_ID))
    style.addLayer(
        LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
            lineColor(Color.rgb(239, 108, 0)),
            lineWidth(5f),
            lineCap(ROUNDED_LINE),
            lineJoin(ROUNDED_LINE)
        )
    )
    addGradientRouteLayer(style, GRADIENT_DESCENT_SOURCE_ID, GRADIENT_DESCENT_LAYER_ID, Color.rgb(33, 150, 243))
    addGradientRouteLayer(style, GRADIENT_LEVEL_SOURCE_ID, GRADIENT_LEVEL_LAYER_ID, Color.rgb(76, 175, 80))
    addGradientRouteLayer(style, GRADIENT_ASCENT_SOURCE_ID, GRADIENT_ASCENT_LAYER_ID, Color.rgb(244, 67, 54))
    style.addSource(GeoJsonSource(RETURN_ROUTE_SOURCE_ID))
    style.addLayer(
        LineLayer(RETURN_ROUTE_LAYER_ID, RETURN_ROUTE_SOURCE_ID).withProperties(
            lineColor(Color.rgb(103, 58, 183)),
            lineWidth(4f),
            lineOffset(6f),
            lineDasharray(arrayOf(1.5f, 2.5f)),
            lineCap(ROUNDED_LINE),
            lineJoin(ROUNDED_LINE)
        )
    )
    style.addSource(GeoJsonSource(RETURN_START_SOURCE_ID))
    style.addLayer(
        CircleLayer(RETURN_START_LAYER_ID, RETURN_START_SOURCE_ID).withProperties(
            circleColor(Color.rgb(103, 58, 183)),
            circleRadius(8f),
            circleStrokeColor(Color.WHITE),
            circleStrokeWidth(2f)
        )
    )
    style.addSource(GeoJsonSource(LOCATION_SOURCE_ID))
    style.addLayer(
        CircleLayer(LOCATION_LAYER_ID, LOCATION_SOURCE_ID).withProperties(
            circleColor(Color.rgb(239, 108, 0)),
            circleRadius(7f),
            circleStrokeColor(Color.WHITE),
            circleStrokeWidth(2f)
        )
    )
    style.addSource(GeoJsonSource(OPENSTREETMAP_POI_SOURCE_ID))
    style.addLayer(
        CircleLayer(OPENSTREETMAP_POI_LAYER_ID, OPENSTREETMAP_POI_SOURCE_ID).withProperties(
            circleColor(Color.rgb(66, 133, 244)),
            circleRadius(6f),
            circleStrokeColor(Color.WHITE),
            circleStrokeWidth(2f)
        )
    )
}

private fun addGradientRouteLayer(style: Style, sourceId: String, layerId: String, color: Int) {
    style.addSource(GeoJsonSource(sourceId))
    style.addLayer(
        LineLayer(layerId, sourceId).withProperties(
            lineColor(color),
            lineWidth(6f),
            lineCap(ROUNDED_LINE),
            lineJoin(ROUNDED_LINE)
        )
    )
}

internal const val ROUTE_SOURCE_ID = "run-route-source"
internal const val WEEKLY_HEATMAP_SOURCE_ID = "run-weekly-heatmap-source"
private const val ROUTE_LAYER_ID = "run-route-layer"
private const val WEEKLY_HEATMAP_LAYER_ID = "run-weekly-heatmap-layer"
internal const val PLANNED_ROUTE_SOURCE_ID = "run-planned-route-source"
private const val PLANNED_ROUTE_LAYER_ID = "run-planned-route-layer"
internal const val RETURN_ROUTE_SOURCE_ID = "run-return-route-source"
private const val RETURN_ROUTE_LAYER_ID = "run-return-route-layer"
internal const val RETURN_START_SOURCE_ID = "run-return-start-source"
private const val RETURN_START_LAYER_ID = "run-return-start-layer"
internal const val LOCATION_SOURCE_ID = "run-current-location-source"
internal const val OPENSTREETMAP_POI_SOURCE_ID = "run-openstreetmap-poi-source"
internal const val OPENSTREETMAP_POI_LAYER_ID = "run-openstreetmap-poi-layer"
internal const val GRADIENT_DESCENT_SOURCE_ID = "run-gradient-descent-source"
internal const val GRADIENT_LEVEL_SOURCE_ID = "run-gradient-level-source"
internal const val GRADIENT_ASCENT_SOURCE_ID = "run-gradient-ascent-source"
private const val LOCATION_LAYER_ID = "run-current-location-layer"
private const val GRADIENT_DESCENT_LAYER_ID = "run-gradient-descent-layer"
private const val GRADIENT_LEVEL_LAYER_ID = "run-gradient-level-layer"
private const val GRADIENT_ASCENT_LAYER_ID = "run-gradient-ascent-layer"
private const val ROUNDED_LINE = "round"
