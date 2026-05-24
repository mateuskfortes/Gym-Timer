package com.example.gymtimer2.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.gymtimer2.data.entity.ChorusEntity
import com.example.gymtimer2.data.entity.ExerciseChorusEntity
import com.example.gymtimer2.data.entity.ExerciseEntity

data class ExerciseWithChorusesRelation (

    @Embedded
    val exercise: ExerciseEntity,

    @Relation(
        entity = ChorusEntity::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = androidx.room.Junction(
            ExerciseChorusEntity::class,
            parentColumn = "exercise_id",
            entityColumn = "chorus_id"
        )
    )
    val choruses: List<ChorusWithSongRelation>
)