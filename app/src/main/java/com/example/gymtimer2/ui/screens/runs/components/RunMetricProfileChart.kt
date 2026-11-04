package com.example.gymtimer2.ui.screens.runs.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.example.gymtimer2.domain.model.RunMetricProfilePoint
import kotlin.math.abs

/** Desenha uma métrica por distância e permite selecionar amostras com toque ou arrasto. */
@Composable
internal fun RunMetricProfileChart(
    profile: List<RunMetricProfilePoint>,
    routeDistanceMeters: Double,
    selectedIndex: Int,
    lowerValueAtTop: Boolean,
    description: String,
    onSelectPoint: (Int) -> Unit
) {
    val paceColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val selectionColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(104.dp)
            .pointerInput(profile, routeDistanceMeters) {
                detectTapGestures { position ->
                    onSelectPoint(nearestRunMetricProfileIndex(profile, position.x, size.width, routeDistanceMeters))
                }
            }
            .pointerInput(profile, routeDistanceMeters) {
                detectDragGestures(
                    onDragStart = { position ->
                        onSelectPoint(nearestRunMetricProfileIndex(profile, position.x, size.width, routeDistanceMeters))
                    },
                    onDrag = { change, _ ->
                        onSelectPoint(nearestRunMetricProfileIndex(profile, change.position.x, size.width, routeDistanceMeters))
                        change.consume()
                    }
                )
            }
            .semantics { contentDescription = description }
    ) {
        drawRunMetricProfile(
            profile,
            routeDistanceMeters,
            selectedIndex,
            lowerValueAtTop,
            paceColor,
            gridColor,
            selectionColor
        )
    }
}

private fun nearestRunMetricProfileIndex(
    profile: List<RunMetricProfilePoint>,
    x: Float,
    width: Int,
    routeDistanceMeters: Double
): Int {
    if (profile.isEmpty() || width <= 0) return -1
    val selectedDistance = (x / width).coerceIn(0f, 1f) * routeDistanceMeters
    return profile.indices.minByOrNull { index -> abs(profile[index].distanceMeters - selectedDistance) } ?: -1
}

private fun DrawScope.drawRunMetricProfile(
    profile: List<RunMetricProfilePoint>,
    routeDistanceMeters: Double,
    selectedIndex: Int,
    lowerValueAtTop: Boolean,
    paceColor: Color,
    gridColor: Color,
    selectionColor: Color
) {
    if (size.width <= 0f) return
    if (size.height <= 0f || profile.size < 2) return
    if (!routeDistanceMeters.isFinite() || routeDistanceMeters <= 0.0) return

    val scale = MetricProfileScale(profile, routeDistanceMeters, size.width, size.height, lowerValueAtTop)
    drawMetricProfileGrid(gridColor)
    drawPath(createRunMetricProfilePath(profile, scale), paceColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    drawSelectedMetricPoint(profile.getOrNull(selectedIndex), scale, paceColor, selectionColor)
}

private fun DrawScope.drawMetricProfileGrid(color: Color) {
    for (gridline in 0..3) {
        val y = size.height * gridline / 3f
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
    }
}

private fun createRunMetricProfilePath(profile: List<RunMetricProfilePoint>, scale: MetricProfileScale): Path {
    val routePath = Path()
    var previousSegmentIndex: Int? = null
    profile.forEach { point ->
        val position = scale.position(point)
        if (point.segmentIndex == previousSegmentIndex) routePath.lineTo(position.x, position.y)
        else routePath.moveTo(position.x, position.y)
        previousSegmentIndex = point.segmentIndex
    }
    return routePath
}

private fun DrawScope.drawSelectedMetricPoint(
    point: RunMetricProfilePoint?,
    scale: MetricProfileScale,
    paceColor: Color,
    selectionColor: Color
) {
    if (point == null) return
    val position = scale.position(point)
    drawLine(selectionColor, Offset(position.x, 0f), Offset(position.x, size.height), strokeWidth = 1.dp.toPx())
    drawCircle(paceColor, radius = 4.dp.toPx(), center = position)
}

private class MetricProfileScale(
    profile: List<RunMetricProfilePoint>,
    private val routeDistanceMeters: Double,
    private val width: Float,
    private val height: Float,
    private val lowerValueAtTop: Boolean
) {
    private val minimumValue = profile.minOf(RunMetricProfilePoint::value)
    private val rawValueRange = profile.maxOf(RunMetricProfilePoint::value) - minimumValue
    private val valueRange = rawValueRange.coerceAtLeast(1.0)
    private val isFlat = rawValueRange < 1.0

    fun position(point: RunMetricProfilePoint): Offset {
        val x = width * (point.distanceMeters / routeDistanceMeters).coerceIn(0.0, 1.0).toFloat()
        val valueFraction = (point.value - minimumValue) / valueRange
        val screenFraction = if (isFlat) 0.5 else if (lowerValueAtTop) valueFraction else 1.0 - valueFraction
        val y = height * screenFraction.coerceIn(0.0, 1.0).toFloat()
        return Offset(x, y)
    }
}
