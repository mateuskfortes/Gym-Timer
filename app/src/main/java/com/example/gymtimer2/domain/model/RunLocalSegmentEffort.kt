package com.example.gymtimer2.domain.model

data class RunLocalSegmentEffort(
    val routeId: Long,
    val progressMeters: Double,
    val startedAtDurationMs: Long?,
    val completedAtDurationMs: Long?
) {
    val durationMs: Long?
        get() {
            val startedAt = startedAtDurationMs ?: return null
            return completedAtDurationMs?.minus(startedAt)?.takeIf { it > 0L }
        }
}
