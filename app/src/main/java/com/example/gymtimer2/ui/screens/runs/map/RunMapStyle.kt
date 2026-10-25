package com.example.gymtimer2.ui.screens.runs.map

import android.content.Context
import android.graphics.Color
import com.example.gymtimer2.R
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource

/** Lê o estilo OSM local; os tiles são carregados só para a área visível. */
internal fun readMapStyle(context: Context): String =
    context.resources.openRawResource(R.raw.run_map_style).bufferedReader().use { it.readText() }

/** Adiciona uma linha para a rota e um marcador para a posição mais recente. */
internal fun addRouteLayers(style: Style) {
    style.addSource(GeoJsonSource(ROUTE_SOURCE_ID))
    style.addLayer(
        LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
            lineColor(Color.rgb(239, 108, 0)),
            lineWidth(5f),
            lineCap("round"),
            lineJoin("round")
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
}

internal const val ROUTE_SOURCE_ID = "run-route-source"
private const val ROUTE_LAYER_ID = "run-route-layer"
internal const val LOCATION_SOURCE_ID = "run-current-location-source"
private const val LOCATION_LAYER_ID = "run-current-location-layer"
