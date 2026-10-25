@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.ui.screens.runs.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.gymtimer2.domain.model.RunPointModel
import kotlin.math.hypot
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

/** Aplica as regras da câmera sem acoplar o desenho da rota à navegação do mapa. */
internal class RunMapCameraController(private val centerTolerancePx: Double) {
    private val state = RunMapCameraState()
    private var map: MapLibreMap? = null
    private var userPosition: LatLng? = null
    private var appliedBottomPaddingPx: Int? = null

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
        initialLocation: LatLng?,
        followLatestPoint: Boolean,
        focusLatestPoint: Boolean,
        bottomPaddingPx: Int
    ) {
        val map = map ?: return
        val position = points.lastOrNull()?.let { LatLng(it.latitude, it.longitude) } ?: initialLocation
        val positionChanged = userPosition != position
        userPosition = position
        if (position == null || state.isGestureInProgress) return

        val paddingChanged = appliedBottomPaddingPx != bottomPaddingPx
        val action = state.nextAction(followLatestPoint, positionChanged, paddingChanged)
        if (paddingChanged) {
            appliedBottomPaddingPx = bottomPaddingPx
            map.moveCamera(CameraUpdateFactory.paddingTo(0.0, 0.0, 0.0, bottomPaddingPx.toDouble()))
        }
        when (action) {
            RunMapCameraState.Action.INITIALIZE -> {
                state.markInitialized()
                initializeCamera(map, points, focusLatestPoint, initialLocation)
            }
            RunMapCameraState.Action.CENTER_WITHOUT_ZOOM -> map.moveCamera(CameraUpdateFactory.newLatLng(position))
            RunMapCameraState.Action.KEEP_VIEWPORT -> Unit
        }
        refreshRecenterVisibility(isCenteredOnUser())
    }

    /** Centraliza na área útil e restaura o zoom adequado para acompanhar a corrida. */
    fun recenter() {
        val map = map ?: return
        val position = userPosition ?: return
        state.onRecenter()
        centerOnRunLocation(map, position)
        refreshRecenterVisibility(isCenteredOnUser())
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
