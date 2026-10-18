package com.example.gymtimer2.timer_overlay_service.components.float_timer

/** Keeps a session-local order and postpones skipped exercises until other exercises finish. */
class WorkoutSequence(exerciseIds: List<Int>) {
    private val ids = exerciseIds.toMutableList()
    private val deferredIds = mutableSetOf<Int>()
    private var index = 0

    val currentId: Int?
        get() = ids.getOrNull(index)

    val hasNext: Boolean
        get() = index < ids.lastIndex

    val canSkipNext: Boolean
        get() {
            val nextIndex = index + 1
            if (nextIndex !in ids.indices || ids[nextIndex] in deferredIds) return false
            return (nextIndex + 1 until ids.size).any { ids[it] !in deferredIds }
        }

    val nextId: Int?
        get() = ids.getOrNull(index + 1)

    fun advance(): Int? {
        index++
        val nextId = currentId
        if (nextId != null) deferredIds.remove(nextId)
        return nextId
    }

    fun skipNext(): Boolean {
        if (!canSkipNext) return false

        val skippedId = ids.removeAt(index + 1)
        ids.add(skippedId)
        deferredIds.add(skippedId)
        return true
    }
}
