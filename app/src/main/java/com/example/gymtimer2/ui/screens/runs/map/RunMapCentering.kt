package com.example.gymtimer2.ui.screens.runs.map

/** Evita considerar posições distantes como centralizadas quando o zoom mostra uma região ampla. */
internal fun isRunMapPositionCentered(distancePx: Double, distanceMeters: Double, tolerancePx: Double): Boolean =
    distancePx in 0.0..tolerancePx && distanceMeters in 0.0..MAX_CENTER_DISTANCE_METERS

private const val MAX_CENTER_DISTANCE_METERS = 25.0
