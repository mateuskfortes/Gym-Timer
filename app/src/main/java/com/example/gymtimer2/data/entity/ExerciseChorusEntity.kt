package com.example.gymtimer2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "exercise_choruses",
    primaryKeys = ["exercise_id", "chorus_id"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChorusEntity::class,
            parentColumns = ["id"],
            childColumns = ["chorus_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["exercise_id"]),
        Index(value = ["chorus_id"])
    ]
)
data class ExerciseChorusEntity(
    @ColumnInfo(name = "exercise_id")
    val exerciseId: Int,

    @ColumnInfo(name = "chorus_id")
    val chorusId: Long
)

