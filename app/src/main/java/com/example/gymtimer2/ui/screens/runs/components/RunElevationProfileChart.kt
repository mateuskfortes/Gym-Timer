package com.example.gymtimer2.ui.screens.runs.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunElevationProfilePoint

/** Desenha o perfil da rota e destaca o progresso quando ele está disponível. */
@Composable
internal fun RunElevationProfileChart(
    profile: List<RunElevationProfilePoint>,
    routeDistanceMeters: Double,
    description: String,
    progressMeters: Double? = null,
    height: Dp = 48.dp
) {
    if (profile.size < 2 || !routeDistanceMeters.isFinite() || routeDistanceMeters <= 0.0) return

    val routeColor = if (progressMeters == null) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val progressColor = MaterialTheme.colorScheme.primary
    Canvas(
        Modifier.fillMaxWidth().height(height).semantics { contentDescription = description }
    ) {
        drawRunElevationProfile(profile, routeDistanceMeters, progressMeters, routeColor, progressColor)
    }
}

private fun DrawScope.drawRunElevationProfile(
    profile: List<RunElevationProfilePoint>,
    routeDistanceMeters: Double,
    progressMeters: Double?,
    routeColor: Color,
    progressColor: Color
) {
    if (size.width <= 0f || size.height <= 0f) return
    val minimumElevation = profile.minOf(RunElevationProfilePoint::elevationMeters)
    val elevationRange = (profile.maxOf(RunElevationProfilePoint::elevationMeters) - minimumElevation)
        .coerceAtLeast(1.0)
    val progressLimitX = progressMeters
        ?.takeIf(Double::isFinite)
        ?.let { size.width * (it / routeDistanceMeters).coerceIn(0.0, 1.0).toFloat() }
    val routePath = Path()
    val progressPath = Path()
    var previousSegmentIndex: Int? = null

    profile.forEach { point ->
        val x = size.width * (point.distanceMeters / routeDistanceMeters).coerceIn(0.0, 1.0).toFloat()
        val elevationFraction = ((point.elevationMeters - minimumElevation) / elevationRange).coerceIn(0.0, 1.0)
        val position = Offset(x, size.height * (1.0 - elevationFraction).toFloat())
        val isNewSegment = point.segmentIndex != previousSegmentIndex
        appendElevationProfilePoint(routePath, progressPath, position, isNewSegment, progressLimitX?.let { x <= it } == true)
        previousSegmentIndex = point.segmentIndex
    }

    drawPath(routePath, routeColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    if (progressLimitX != null) {
        drawPath(progressPath, progressColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
    }
}

private fun appendElevationProfilePoint(
    routePath: Path,
    progressPath: Path,
    position: Offset,
    isNewSegment: Boolean,
    isInProgress: Boolean
) {
    if (isNewSegment) routePath.moveTo(position.x, position.y) else routePath.lineTo(position.x, position.y)
    if (isInProgress) {
        if (isNewSegment) progressPath.moveTo(position.x, position.y)
        else progressPath.lineTo(position.x, position.y)
    }
}
