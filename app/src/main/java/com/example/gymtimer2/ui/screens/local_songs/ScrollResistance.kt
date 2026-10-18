package com.example.gymtimer2.ui.screens.local_songs

import android.os.SystemClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import kotlin.math.exp

private const val SPEED_THRESHOLD_DP_PER_SECOND = 1_200f
private const val SPEED_RANGE_DP_PER_SECOND = 2_200f
private const val DRAG_SMOOTHING = 0.35f
private const val DRAG_RESET_AFTER_NANOS = 120_000_000L
private const val MIN_EVENT_INTERVAL_NANOS = 1_000_000L

internal fun dampScrollSpeed(speedDpPerSecond: Float): Float {
    val speed = abs(speedDpPerSecond)
    if (speed <= SPEED_THRESHOLD_DP_PER_SECOND) return speedDpPerSecond

    val excess = (speed - SPEED_THRESHOLD_DP_PER_SECOND) / SPEED_RANGE_DP_PER_SECOND
    val dampedSpeed = SPEED_THRESHOLD_DP_PER_SECOND +
        SPEED_RANGE_DP_PER_SECOND * (1f - exp(-excess))
    return dampedSpeed * speedDpPerSecond.signValue()
}

internal class ScrollResistanceConnection(
    private val density: Float
) : NestedScrollConnection {
    private var lastDragTimeNanos = 0L
    private var previousDirection = 0f
    private var smoothedDragSpeedDpPerSecond = 0f

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        if (source != NestedScrollSource.UserInput || available.y == 0f) return Offset.Zero

        val now = SystemClock.elapsedRealtimeNanos()
        val direction = available.y.signValue()
        val elapsedNanos = now - lastDragTimeNanos

        val startsNewGesture =
            lastDragTimeNanos == 0L ||
            elapsedNanos <= 0L ||
            elapsedNanos > DRAG_RESET_AFTER_NANOS ||
            direction != previousDirection
        if (startsNewGesture) {
            smoothedDragSpeedDpPerSecond = 0f
        } else {
            val eventInterval = elapsedNanos.coerceAtLeast(MIN_EVENT_INTERVAL_NANOS)
            val currentSpeed = (abs(available.y) / density) * 1_000_000_000f / eventInterval
            smoothedDragSpeedDpPerSecond = smoothedDragSpeedDpPerSecond * (1f - DRAG_SMOOTHING) +
                currentSpeed * DRAG_SMOOTHING
        }

        lastDragTimeNanos = now
        previousDirection = direction

        val adjustedDelta = if (smoothedDragSpeedDpPerSecond == 0f) {
            available.y
        } else {
            val dampedSpeed = dampScrollSpeed(smoothedDragSpeedDpPerSecond)
            val deltaRatio = (abs(dampedSpeed) / smoothedDragSpeedDpPerSecond).coerceIn(0f, 1f)
            available.y * deltaRatio
        }
        return Offset(x = 0f, y = available.y - adjustedDelta)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        resetDragTracking()
        if (available.y == 0f) return Velocity.Zero

        val speedDpPerSecond = available.y / density
        val adjustedVelocity = dampScrollSpeed(speedDpPerSecond) * density
        return Velocity(x = 0f, y = available.y - adjustedVelocity)
    }

    private fun resetDragTracking() {
        lastDragTimeNanos = 0L
        previousDirection = 0f
        smoothedDragSpeedDpPerSecond = 0f
    }
}

private fun Float.signValue(): Float = when {
    this > 0f -> 1f
    this < 0f -> -1f
    else -> 0f
}
