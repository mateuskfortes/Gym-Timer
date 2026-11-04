package com.example.gymtimer2.ui.screens.runs.map

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.geometry.LatLng
import com.example.gymtimer2.domain.model.RoutePoint
import com.example.gymtimer2.domain.model.RunGradientSegment
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunHeatmapCell
import com.example.gymtimer2.domain.model.RunMapBounds
import com.example.gymtimer2.domain.model.RunMapPoi
import com.example.gymtimer2.domain.model.RunMapCameraSnapshot
import com.example.gymtimer2.domain.model.runGradientSegments
import com.example.gymtimer2.ui.screens.runs.RunMapPoiUiState
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/** Desenha rotas e conserva o enquadramento escolhido até a pessoa centralizar. */
@Composable
internal fun RunMap(
    points: List<RunPointModel>,
    weeklyHeatmapCells: List<RunHeatmapCell> = emptyList(),
    historyHeatmapCells: List<RunHeatmapCell> = emptyList(),
    followLatestPoint: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    bottomPadding: Dp = 0.dp,
    showRecenterControl: Boolean = false,
    showRouteOverviewControl: Boolean = false,
    useTextureView: Boolean = false,
    onInitialLocationChanged: (LatLng?) -> Unit = {},
    plannedPoints: List<RoutePoint> = emptyList(),
    plannedRouteKey: String? = null,
    returnRoutePoints: List<RoutePoint> = emptyList(),
    poiState: RunMapPoiUiState = RunMapPoiUiState(),
    onPoiViewportChanged: ((RunMapBounds?) -> Unit)? = null,
    onCameraCenterChanged: ((LatLng?) -> Unit)? = null,
    selectedLayers: Set<RunMapLayer>? = null,
    onSelectedLayersChanged: ((Set<RunMapLayer>) -> Unit)? = null,
    initialCameraLocation: LatLng? = null,
    cameraSnapshotRunId: Long? = null,
    initialCameraSnapshot: RunMapCameraSnapshot? = null,
    onCameraSnapshotChanged: ((RunMapCameraSnapshot) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val bottomPaddingPx = with(LocalDensity.current) { bottomPadding.roundToPx() }
    val centerTolerancePx = with(LocalDensity.current) { 16.dp.toPx().toDouble() }
    val initialLocation by rememberInitialMapLocation(enabled = points.isEmpty())
    val cameraLocation = initialCameraLocation ?: initialLocation
    val cameraController = remember { RunMapCameraController(centerTolerancePx) }
    var readyMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedPoi by remember { mutableStateOf<RunMapPoi?>(null) }
    // Preserve the local map selection when GPS points arrive after the map opens.
    var localSelectedMapLayers by rememberSaveable { mutableStateOf(emptySet<RunMapLayer>()) }
    val selectedMapLayers = selectedLayers ?: localSelectedMapLayers
    val updateSelectedMapLayers: (Set<RunMapLayer>) -> Unit = onSelectedLayersChanged
        ?: { localSelectedMapLayers = it }
    val gradientSegments = remember(points) { runGradientSegments(points) }
    val gradientAvailable = gradientSegments.isNotEmpty()
    val weeklyHeatmapAvailable = weeklyHeatmapCells.isNotEmpty()
    val localHistoryAvailable = historyHeatmapCells.isNotEmpty()
    val poiLayerEnabled = onPoiViewportChanged != null
    val mapView = rememberMapView(context, useTextureView)

    LaunchedEffect(selectedMapLayers, gradientAvailable, weeklyHeatmapAvailable, localHistoryAvailable, poiLayerEnabled) {
        val availableLayers = selectedMapLayers.filter { layer ->
            layer.isAvailable(
                gradientAvailable = gradientAvailable,
                weeklyActivityAvailable = weeklyHeatmapAvailable,
                localHistoryAvailable = localHistoryAvailable,
                poiLayerEnabled = poiLayerEnabled
            )
        }.toSet()
        if (availableLayers != selectedMapLayers) updateSelectedMapLayers(availableLayers)
    }
    val poiLayerSelected = RunMapLayer.OPENSTREETMAP_POI in selectedMapLayers
    RunMapPoiViewportEffect(readyMap, mapSize, poiLayerSelected, onPoiViewportChanged)
    RunMapPoiSelectionEffect(readyMap, poiLayerSelected) { selectedPoi = it }
    RunMapGradientEffect(
        readyMap,
        gradientSegments.takeIf { RunMapLayer.GRADIENT in selectedMapLayers }.orEmpty()
    )
    RunMapActivityHeatmapEffect(
        readyMap,
        weeklyHeatmapCells,
        historyHeatmapCells,
        selectedMapLayers
    )
    RunMapPoiEffect(
        readyMap,
        context,
        poiState.points.takeIf { poiLayerSelected }.orEmpty()
    )
    RunMapDataEffect(
        readyMap,
        mapSize,
        points,
        plannedPoints,
        returnRoutePoints,
        plannedRouteKey,
        initialLocation,
        cameraLocation,
        initialCameraSnapshot,
        followLatestPoint,
        bottomPaddingPx,
        showRecenterControl,
        cameraController
    )
    RunMapLocationEffect(initialLocation, onInitialLocationChanged)
    RunMapCameraCenterEffect(readyMap, mapSize, onCameraCenterChanged)
    RunMapLifecycleEffect(
        lifecycle,
        mapView,
        cameraController,
        cameraSnapshotRunId,
        onCameraSnapshotChanged
    )

    Box(modifier = modifier.clip(shape)) {
        RunMapCanvas(mapView, context, cameraController, onSizeChanged = { mapSize = it }) { readyMap = it }
        RunMapOverlays(
            waitingForLocation = points.isEmpty() && cameraLocation == null && initialCameraSnapshot == null,
            recenterEnabled = showRecenterControl,
            recenterVisible = cameraController.isRecenterVisible,
            routeOverviewEnabled = showRouteOverviewControl &&
                hasRouteOverviewPoints(points, plannedPoints, returnRoutePoints),
            gradientAvailable = gradientAvailable,
            weeklyHeatmapAvailable = weeklyHeatmapAvailable,
            localHistoryAvailable = localHistoryAvailable,
            poiLayerEnabled = poiLayerEnabled,
            poiState = poiState,
            selectedMapLayers = selectedMapLayers,
            selectedPoi = selectedPoi,
            bottomPadding = bottomPadding,
            onRecenter = cameraController::recenter,
            onSelectMapLayer = { updateSelectedMapLayers(selectedMapLayers.toggle(it)) },
            onShowRouteOverview = {
                cameraController.showRouteOverview(points, plannedPoints, returnRoutePoints)
            }
        )
    }
}

private fun hasRouteOverviewPoints(
    points: List<RunPointModel>,
    plannedPoints: List<RoutePoint>,
    returnRoutePoints: List<RoutePoint>
): Boolean =
    points.asSequence().filterNot(RunPointModel::isFiltered).take(MIN_ROUTE_POINTS).count() == MIN_ROUTE_POINTS ||
        plannedPoints.asSequence().filterNot(RoutePoint::isFiltered).take(MIN_ROUTE_POINTS).count() == MIN_ROUTE_POINTS ||
        returnRoutePoints.asSequence().filterNot(RoutePoint::isFiltered).take(MIN_ROUTE_POINTS).count() == MIN_ROUTE_POINTS

@Composable
private fun rememberMapView(context: Context, useTextureView: Boolean): MapView = remember {
    MapView(context, MapLibreMapOptions.createFromAttributes(context).textureMode(useTextureView)).apply {
        onCreate(null)
    }
}

@Composable
private fun RunMapDataEffect(
    map: MapLibreMap?,
    mapSize: IntSize,
    points: List<RunPointModel>,
    plannedPoints: List<RoutePoint>,
    returnRoutePoints: List<RoutePoint>,
    plannedRouteKey: String?,
    initialLocation: LatLng?,
    cameraLocation: LatLng?,
    initialCameraSnapshot: RunMapCameraSnapshot?,
    followLatestPoint: Boolean,
    bottomPaddingPx: Int,
    showRecenterControl: Boolean,
    cameraController: RunMapCameraController
) {
    LaunchedEffect(
        map,
        mapSize,
        points,
        plannedPoints,
        returnRoutePoints,
        plannedRouteKey,
        initialLocation,
        cameraLocation,
        initialCameraSnapshot,
        followLatestPoint,
        bottomPaddingPx,
        showRecenterControl
    ) {
        if (map == null || mapSize.width == 0 || mapSize.height == 0) return@LaunchedEffect
        updateRoute(map, points, plannedPoints, returnRoutePoints, initialLocation)
        cameraController.update(
            points = points,
            plannedPoints = plannedPoints,
            plannedRouteKey = plannedRouteKey,
            initialLocation = cameraLocation,
            initialCameraSnapshot = initialCameraSnapshot,
            followLatestPoint = followLatestPoint,
            focusLatestPoint = if (plannedRouteKey != null && !followLatestPoint) false
                else showRecenterControl || followLatestPoint,
            bottomPaddingPx = bottomPaddingPx
        )
    }
}

@Composable
private fun RunMapGradientEffect(map: MapLibreMap?, segments: List<RunGradientSegment>) {
    LaunchedEffect(map, segments) {
        if (map != null) updateGradientRouteLayers(map, segments)
    }
}

@Composable
private fun RunMapActivityHeatmapEffect(
    map: MapLibreMap?,
    weeklyCells: List<RunHeatmapCell>,
    historyCells: List<RunHeatmapCell>,
    selectedLayers: Set<RunMapLayer>
) {
    LaunchedEffect(map, weeklyCells, historyCells, selectedLayers) {
        if (map != null) updateActivityHeatmap(map, weeklyCells, historyCells, selectedLayers)
    }
}

@Composable
private fun RunMapPoiEffect(map: MapLibreMap?, context: Context, points: List<RunMapPoi>) {
    LaunchedEffect(map, points) {
        if (map != null) updateOpenStreetMapPois(map, context, points)
    }
}

@Composable
private fun RunMapPoiViewportEffect(
    map: MapLibreMap?,
    mapSize: IntSize,
    poiLayerSelected: Boolean,
    onViewportChanged: ((RunMapBounds?) -> Unit)?
) {
    val latestCallback by rememberUpdatedState(onViewportChanged)
    DisposableEffect(map, mapSize, poiLayerSelected) {
        if (map == null || !poiLayerSelected || latestCallback == null) {
            latestCallback?.invoke(null)
            onDispose { }
        } else {
            val listener = MapLibreMap.OnCameraIdleListener {
                val bounds = map.projection.visibleRegion.latLngBounds
                latestCallback?.invoke(
                    RunMapBounds(
                        southLatitude = bounds.latitudeSouth,
                        westLongitude = bounds.longitudeWest,
                        northLatitude = bounds.latitudeNorth,
                        eastLongitude = bounds.longitudeEast
                    )
                )
            }
            map.addOnCameraIdleListener(listener)
            listener.onCameraIdle()
            onDispose {
                map.removeOnCameraIdleListener(listener)
                latestCallback?.invoke(null)
            }
        }
    }
}

@Composable
private fun RunMapCameraCenterEffect(
    map: MapLibreMap?,
    mapSize: IntSize,
    onCenterChanged: ((LatLng?) -> Unit)?
) {
    val latestCallback by rememberUpdatedState(onCenterChanged)
    DisposableEffect(map, mapSize) {
        val centerTarget = findRunMapCenterTarget(map, mapSize, latestCallback != null)
        if (centerTarget == null) {
            latestCallback?.invoke(null)
            onDispose { }
        } else {
            val listener = MapLibreMap.OnCameraIdleListener {
                latestCallback?.invoke(centerTarget.cameraPosition.target)
            }
            centerTarget.addOnCameraIdleListener(listener)
            listener.onCameraIdle()
            onDispose {
                centerTarget.removeOnCameraIdleListener(listener)
                latestCallback?.invoke(null)
            }
        }
    }
}

private fun findRunMapCenterTarget(map: MapLibreMap?, mapSize: IntSize, hasCallback: Boolean): MapLibreMap? =
    map?.takeIf { mapSize.width > 0 && mapSize.height > 0 && hasCallback }

@Composable
private fun RunMapPoiSelectionEffect(
    map: MapLibreMap?,
    poiLayerSelected: Boolean,
    onPoiSelected: (RunMapPoi?) -> Unit
) {
    val latestCallback by rememberUpdatedState(onPoiSelected)
    DisposableEffect(map, poiLayerSelected) {
        if (map == null || !poiLayerSelected) {
            latestCallback(null)
            onDispose { }
        } else {
            val listener = MapLibreMap.OnMapClickListener { location ->
                val feature = map.queryRenderedFeatures(
                    map.projection.toScreenLocation(location),
                    OPENSTREETMAP_POI_LAYER_ID
                ).firstOrNull()
                latestCallback(feature?.toRunMapPoiOrNull())
                feature != null
            }
            map.addOnMapClickListener(listener)
            onDispose {
                map.removeOnMapClickListener(listener)
                latestCallback(null)
            }
        }
    }
}

@Composable
private fun RunMapLocationEffect(
    initialLocation: LatLng?,
    onInitialLocationChanged: (LatLng?) -> Unit
) {
    LaunchedEffect(initialLocation) {
        onInitialLocationChanged(initialLocation)
    }
}

@Composable
private fun RunMapLifecycleEffect(
    lifecycle: Lifecycle,
    mapView: MapView,
    cameraController: RunMapCameraController,
    cameraSnapshotRunId: Long?,
    onCameraSnapshotChanged: ((RunMapCameraSnapshot) -> Unit)?
) {
    val latestCallback by rememberUpdatedState(onCameraSnapshotChanged)
    val latestRunId by rememberUpdatedState(cameraSnapshotRunId)
    DisposableEffect(lifecycle, mapView) {
        var lastSavedSnapshot: RunMapCameraSnapshot? = null
        fun saveCameraSnapshot() {
            val snapshot = latestRunId?.let(cameraController::snapshot) ?: return
            if (snapshot == lastSavedSnapshot) return
            lastSavedSnapshot = snapshot
            latestCallback?.invoke(snapshot)
        }
        val observer = LifecycleEventObserver { _, event ->
            mapView.handleLifecycleEvent(event)
            if (event == Lifecycle.Event.ON_STOP) saveCameraSnapshot()
        }
        lifecycle.addObserver(observer)
        onDispose {
            saveCameraSnapshot()
            lifecycle.removeObserver(observer)
            cameraController.detach()
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }
}

@Composable
private fun RunMapCanvas(
    mapView: MapView,
    context: Context,
    cameraController: RunMapCameraController,
    onSizeChanged: (IntSize) -> Unit,
    onReady: (MapLibreMap) -> Unit
) {
    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    cameraController.attach(map)
                    map.setStyle(Style.Builder().fromJson(readMapStyle(context))) { style ->
                        addRouteLayers(style)
                        onReady(map)
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize().onSizeChanged(onSizeChanged)
    )
}
