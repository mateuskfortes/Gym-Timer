package com.example.gymtimer2.ui.screens.runs

import com.example.gymtimer2.data.repository.OpenStreetMapPoiRepository
import com.example.gymtimer2.data.repository.isSupportedPoiSearchArea
import com.example.gymtimer2.domain.model.RunMapBounds
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

internal class RunMapPoiController(
    scope: CoroutineScope,
    private val repository: OpenStreetMapPoiRepository
) {
    private val viewport = MutableStateFlow<RunMapBounds?>(null)
    private val mutableState = MutableStateFlow(RunMapPoiUiState())
    private var lastSuccessfulBounds: RunMapBounds? = null
    val state: StateFlow<RunMapPoiUiState> = mutableState.asStateFlow()
    val onViewportChanged: (RunMapBounds?) -> Unit = ::updateViewport

    init {
        scope.launch {
            var lastRequestAtMs = 0L
            viewport.filterNotNull().collect { bounds ->
                delay(REQUEST_DEBOUNCE_MS)
                if (viewport.value != bounds) return@collect
                val remainingInterval = MIN_REQUEST_INTERVAL_MS -
                    (System.currentTimeMillis() - lastRequestAtMs)
                if (bounds != lastSuccessfulBounds && remainingInterval > 0L) delay(remainingInterval)
                if (viewport.value != bounds) return@collect
                mutableState.value = RunMapPoiUiState(isLoading = true)
                lastRequestAtMs = System.currentTimeMillis()
                try {
                    val points = repository.findNearby(bounds)
                    if (viewport.value == bounds) {
                        lastSuccessfulBounds = bounds
                        mutableState.value = RunMapPoiUiState(points = points, hasSearched = true)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    if (viewport.value == bounds) mutableState.value = RunMapPoiUiState(hasError = true)
                }
            }
        }
    }

    private fun updateViewport(bounds: RunMapBounds?) {
        if (bounds == null) {
            viewport.value = null
            mutableState.value = RunMapPoiUiState()
        } else if (!bounds.isSupportedPoiSearchArea()) {
            viewport.value = null
            mutableState.value = RunMapPoiUiState(needsZoom = true)
        } else {
            val previousBounds = lastSuccessfulBounds
            if (previousBounds != null && bounds != previousBounds &&
                !bounds.isMeaningfullyDifferentFrom(previousBounds)
            ) return
            viewport.value = bounds
        }
    }

    private companion object {
        const val REQUEST_DEBOUNCE_MS = 900L
        const val MIN_REQUEST_INTERVAL_MS = 60_000L
    }
}

internal fun RunMapBounds.isMeaningfullyDifferentFrom(other: RunMapBounds): Boolean =
    abs(centerLatitude() - other.centerLatitude()) >= MIN_POI_VIEWPORT_CHANGE ||
        abs(centerLongitude() - other.centerLongitude()) >= MIN_POI_VIEWPORT_CHANGE ||
        hasMeaningfulZoomChange(other)

private fun RunMapBounds.centerLatitude(): Double = (southLatitude + northLatitude) / 2

private fun RunMapBounds.centerLongitude(): Double = (westLongitude + eastLongitude) / 2

private fun RunMapBounds.hasMeaningfulZoomChange(other: RunMapBounds): Boolean {
    val latitudeChange = abs(latitudeSpan() - other.latitudeSpan()) / minOf(latitudeSpan(), other.latitudeSpan())
    val longitudeChange = abs(longitudeSpan() - other.longitudeSpan()) / minOf(longitudeSpan(), other.longitudeSpan())
    return latitudeChange >= MIN_POI_ZOOM_CHANGE_RATIO || longitudeChange >= MIN_POI_ZOOM_CHANGE_RATIO
}

private fun RunMapBounds.latitudeSpan(): Double = northLatitude - southLatitude

private fun RunMapBounds.longitudeSpan(): Double = eastLongitude - westLongitude

private const val MIN_POI_VIEWPORT_CHANGE = 0.01
private const val MIN_POI_ZOOM_CHANGE_RATIO = 0.5
