package com.example.gymtimer2.domain.model

enum class WorkoutScheduleError {
    DAY_NOT_FOUND,
    DAY_NAME_REQUIRED,
    DUPLICATE_DAY_NAME,
    LAST_DAY,
    DAY_NOT_EMPTY,
    EXERCISE_NOT_FOUND,
    INVALID_ORDER,
    INVALID_WEIGHT,
    SAVE_FAILED
}

class WorkoutScheduleException(val reason: WorkoutScheduleError) : IllegalArgumentException()
