package com.example.gymtimer2.timer_overlay_service.components.float_timer

/** Keeps the exercise order fixed for one overlay session. */
class WorkoutSequence(exerciseIds: List<Int>) {
    private val ids = exerciseIds.toList()
    private var index = 0

    val currentId: Int?
        get() = ids.getOrNull(index)

    val hasNext: Boolean
        get() = index < ids.lastIndex

    val nextId: Int?
        get() = ids.getOrNull(index + 1)

    fun advance(): Int? {
        index++
        return currentId
    }
}
