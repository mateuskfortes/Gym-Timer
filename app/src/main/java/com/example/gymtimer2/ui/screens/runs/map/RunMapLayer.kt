package com.example.gymtimer2.ui.screens.runs.map

/** Camadas disponíveis no mapa durante a corrida. */
internal enum class RunMapLayer { STANDARD, GRADIENT, WEEKLY_ACTIVITY, LOCAL_HISTORY, OPENSTREETMAP_POI }

internal fun Set<RunMapLayer>.toggle(layer: RunMapLayer): Set<RunMapLayer> = when {
    layer == RunMapLayer.STANDARD -> emptySet()
    layer in this -> this - layer
    else -> this + layer
}

internal fun Set<RunMapLayer>.isSelected(layer: RunMapLayer): Boolean =
    if (layer == RunMapLayer.STANDARD) isEmpty() else layer in this

/** Indica se há dados suficientes para renderizar a camada escolhida. */
internal fun RunMapLayer.isAvailable(
    gradientAvailable: Boolean,
    weeklyActivityAvailable: Boolean,
    localHistoryAvailable: Boolean = false,
    poiLayerEnabled: Boolean = false
): Boolean = when (this) {
    RunMapLayer.STANDARD -> true
    RunMapLayer.GRADIENT -> gradientAvailable
    RunMapLayer.WEEKLY_ACTIVITY -> weeklyActivityAvailable
    RunMapLayer.LOCAL_HISTORY -> localHistoryAvailable
    RunMapLayer.OPENSTREETMAP_POI -> poiLayerEnabled
}
