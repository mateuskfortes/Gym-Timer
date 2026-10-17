package com.example.gymtimer2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_schedule",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["day_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["day_id", "sort_order"])]
)
data class ExerciseScheduleEntity(
    @PrimaryKey @ColumnInfo(name = "exercise_id") val exerciseId: Int,
    @ColumnInfo(name = "day_id") val dayId: Long,
    @ColumnInfo(name = "sort_order") val sortOrder: Int
)
