@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.ui.screens.runs.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunMapCameraSnapshot
import com.example.gymtimer2.domain.model.RoutePoint
import kotlin.math.hypot
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

/** Aplica as regras da câmera sem acoplar o desenho da rota à navegação do mapa. */
internal class RunMapCameraController(private val centerTolerancePx: Double) {
    private val state = RunMapCameraState()
    private var map: MapLibreMap? = null
    private var userPosition: LatLng? = null
    private var plannedRouteKey: String? = null
    private var plannedRouteVisible = false
    private var appliedBottomPaddingPx: Int? = null
    private var hasRestoredInitialViewport = false

    /** Só mostra o botão quando existe uma posição fora do centro da área útil do mapa. */
    var isRecenterVisible by mutableStateOf(false)
        private set

    private val moveStartedListener = MapLibreMap.OnCameraMoveStartedListener { reason ->
        if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) state.onGestureStarted()
    }
    private val idleListener = MapLibreMap.OnCameraIdleListener {
        val centered = isCenteredOnUser()
        state.onCameraIdle(centered)
        refreshRecenterVisibility(centered)
    }

    /** Registra os listeners uma vez para distinguir gestos de movimentos feitos pelo app. */
    fun attach(map: MapLibreMap) {
        if (this.map === map) return
        detach()
        this.map = map
        map.addOnCameraMoveStartedListener(moveStartedListener)
        map.addOnCameraIdleListener(idleListener)
    }

    /** Atualiza a posição de referência sem mover o mapa quando o usuário o está explorando. */
    fun update(
        points: List<RunPointModel>,
        plannedPoints: List<RoutePoint>,
        plannedRouteKey: String?,
        initialLocation: LatLng?,
        initialCameraSnapshot: RunMapCameraSnapshot?,
        followLatestPoint: Boolean,
        focusLatestPoint: Boolean,
        bottomPaddingPx: Int
    ) {
        val map = map ?: return
        val position = points.lastOrNull { !it.isFiltered }?.let { LatLng(it.latitude, it.longitude) } ?: initialLocation
        val positionChanged = userPosition != position
        userPosition = position
        val plannedRouteChanged = updatePlannedRoute(plannedPoints, plannedRouteKey)
        val savedViewport = initialCameraSnapshot.takeUnless { hasRestoredInitialViewport }
        if (shouldSkipCameraUpdate(position, savedViewport != null)) return

        val paddingChanged = appliedBottomPaddingPx != bottomPaddingPx
        val action = state.nextAction(
            followLatestPoint = followLatestPoint,
            positionChanged = positionChanged || plannedRouteChanged,
            paddingChanged = paddingChanged,
            hasSavedViewport = savedViewport != null
        )
        updateBottomPadding(map, bottomPaddingPx, paddingChanged)
        applyCameraAction(
            map,
            action,
            points,
            plannedPoints,
            focusLatestPoint,
            initialLocation,
            position,
            savedViewport
        )
        refreshRecenterVisibility(isCenteredOnUser())
    }

    private fun updatePlannedRoute(points: List<RoutePoint>, routeKey: String?): Boolean {
        val visible = points.count { !it.isFiltered } >= MIN_ROUTE_POINTS
        val changed = plannedRouteKey != routeKey || plannedRouteVisible != visible
        plannedRouteKey = routeKey
        plannedRouteVisible = visible
        if (changed && !state.isGestureInProgress) state.onPlannedRouteChanged()
        return changed
    }

    private fun shouldSkipCameraUpdate(position: LatLng?, hasSavedViewport: Boolean): Boolean =
        (position == null && !plannedRouteVisible && !hasSavedViewport) || state.isGestureInProgress

    private fun updateBottomPadding(map: MapLibreMap, paddingPx: Int, changed: Boolean) {
        if (!changed) return
        appliedBottomPaddingPx = paddingPx
        map.moveCamera(CameraUpdateFactory.paddingTo(0.0, 0.0, 0.0, paddingPx.toDouble()))
    }

    private fun applyCameraAction(
        map: MapLibreMap,
        action: RunMapCameraState.Action,
        points: List<RunPointModel>,
        plannedPoints: List<RoutePoint>,
        focusLatestPoint: Boolean,
        initialLocation: LatLng?,
        position: LatLng?,
        savedViewport: RunMapCameraSnapshot?
    ) {
        when (action) {
            RunMapCameraState.Action.INITIALIZE -> {
                hasRestoredInitialViewport = true
                state.markInitialized()
                initializeCamera(map, points, plannedPoints, focusLatestPoint, initialLocation)
            }
            RunMapCameraState.Action.RESTORE_SAVED_VIEWPORT -> {
                hasRestoredInitialViewport = true
                val viewport = savedViewport ?: return
                state.restoreViewport(viewport.followsUser)
                val target = if (viewport.followsUser) position else null
                map.moveCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(target ?: LatLng(viewport.latitude, viewport.longitude))
                            .zoom(viewport.zoom)
                            .tilt(viewport.tilt)
                            .bearing(viewport.bearing)
                            .build()
                    )
                )
            }
            RunMapCameraState.Action.CENTER_WITHOUT_ZOOM -> position?.let {
                map.moveCamera(CameraUpdateFactory.newLatLng(it))
            }
            RunMapCameraState.Action.KEEP_VIEWPORT -> Unit
        }
    }

    /** Centraliza na área útil e restaura o zoom adequado para acompanhar a corrida. */
    fun recenter() {
        val map = map ?: return
        val position = userPosition ?: return
        state.onRecenter()
        centerOnRunLocation(map, position)
        refreshRecenterVisibility(isCenteredOnUser())
    }

    fun snapshot(runId: Long): RunMapCameraSnapshot? {
        val cameraPosition = map?.cameraPosition ?: return null
        val target = cameraPosition.target ?: return null
        return RunMapCameraSnapshot(
            runId = runId,
            latitude = target.latitude,
            longitude = target.longitude,
            zoom = cameraPosition.zoom,
            tilt = cameraPosition.tilt,
            bearing = cameraPosition.bearing,
            followsUser = state.isFollowingUser
        ).takeIf(RunMapCameraSnapshot::isValid)
    }

    /** Fits the selected and recorded route, then waits for an explicit recenter action. */
    fun showRouteOverview(
        points: List<RunPointModel>,
        plannedPoints: List<RoutePoint>,
        returnRoutePoints: List<RoutePoint>
    ) {
        val map = map ?: return
        val routePoints = (plannedPoints + points + returnRoutePoints).filterNot(RoutePoint::isFiltered)
            .distinctBy { it.latitude to it.longitude }
        if (routePoints.size < MIN_ROUTE_POINTS) return

        state.onRouteOverview()
        val lastPoint = routePoints.last()
        frameRoute(map, routePoints, LatLng(lastPoint.latitude, lastPoint.longitude))
    }

    /** Remove referências e listeners antes de destruir o MapView. */
    fun detach() {
        map?.removeOnCameraMoveStartedListener(moveStartedListener)
        map?.removeOnCameraIdleListener(idleListener)
        map = null
    }

    /** Exige proximidade visual e geográfica para não esconder o botão na visão global. */
    private fun isCenteredOnUser(): Boolean {
        val map = map ?: return false
        val position = userPosition ?: return false
        val target = map.cameraPosition.target ?: return false
        val userPoint = map.projection.toScreenLocation(position)
        val centerPoint = map.projection.toScreenLocation(target)
        val distancePx = hypot((userPoint.x - centerPoint.x).toDouble(), (userPoint.y - centerPoint.y).toDouble())
        return isRunMapPositionCentered(distancePx, target.distanceTo(position), centerTolerancePx)
    }

    /** Não oferece uma ação de centralização antes de receber uma posição válida. */
    private fun refreshRecenterVisibility(isCentered: Boolean) {
        isRecenterVisible = userPosition != null && !isCentered
    }
}
