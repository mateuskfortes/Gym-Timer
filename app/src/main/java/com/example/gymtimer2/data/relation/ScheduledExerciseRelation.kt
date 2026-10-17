package com.example.gymtimer2.data.relation

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.example.gymtimer2.data.entity.ExerciseEntity

data class ScheduledExerciseRelation(
    @Embedded val exercise: ExerciseEntity,
    @ColumnInfo(name = "schedule_day_id") val dayId: Long,
    @ColumnInfo(name = "schedule_sort_order") val sortOrder: Int
)
