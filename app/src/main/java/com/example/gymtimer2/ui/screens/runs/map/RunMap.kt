package com.example.gymtimer2.ui.screens.runs.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.gymtimer2.domain.model.RunPointModel
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/**
 * Sincroniza a rota e preserva a navegação manual até a ação de centralizar.
 *
 * A opção useTextureView integra o desenho ao recorte de contêineres com rolagem.
 */
@Composable
fun RunMap(
    points: List<RunPointModel>,
    followLatestPoint: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    bottomPadding: Dp = 0.dp,
    showRecenterControl: Boolean = false,
    useTextureView: Boolean = false
) {
    val bottomPaddingPx = with(LocalDensity.current) { bottomPadding.roundToPx() }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val initialLocation by rememberInitialMapLocation(enabled = points.isEmpty())
    val centerTolerancePx = with(LocalDensity.current) { 16.dp.toPx().toDouble() }
    val cameraController = remember { RunMapCameraController(centerTolerancePx) }
    var readyMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapSize by remember { mutableStateOf(IntSize.Zero) }
    val mapView = remember {
        // A textura participa da composição e do recorte em contêineres com rolagem.
        MapView(context, MapLibreMapOptions.createFromAttributes(context).textureMode(useTextureView)).apply {
            onCreate(null)
        }
    }
    // O efeito observa os dados da composição, inclusive quando chegam após o callback do SDK.
    LaunchedEffect(readyMap, mapSize, points, initialLocation, followLatestPoint, bottomPaddingPx, showRecenterControl) {
        val map = readyMap ?: return@LaunchedEffect
        if (mapSize.width == 0 || mapSize.height == 0) return@LaunchedEffect
        updateRoute(map, points, initialLocation)
        cameraController.update(
            points = points,
            initialLocation = initialLocation,
            followLatestPoint = followLatestPoint,
            focusLatestPoint = showRecenterControl || followLatestPoint,
            bottomPaddingPx = bottomPaddingPx
        )
    }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event -> mapView.handleLifecycleEvent(event) }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            cameraController.detach()
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    Box(modifier = modifier.clip(shape)) {
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        cameraController.attach(map)
                        // O recurso contém JSON; a sobrecarga String interpreta o valor como URI.
                        val styleBuilder = Style.Builder().fromJson(readMapStyle(context))
                        map.setStyle(styleBuilder) { style ->
                            addRouteLayers(style)
                            readyMap = map
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize().onSizeChanged { mapSize = it }
        )
        RunMapOverlays(
            waitingForLocation = points.isEmpty() && initialLocation == null,
            recenterEnabled = showRecenterControl,
            recenterVisible = cameraController.isRecenterVisible,
            bottomPadding = bottomPadding,
            onRecenter = cameraController::recenter
        )
    }
}
