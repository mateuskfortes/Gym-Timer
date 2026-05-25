package com.example.gymtimer2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.gymtimer2.domain.model.WeightUnit

@Entity(tableName = "exercises")
// @Parcelize
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val weight: Int,
    val weightUnitCode: Int = WeightUnit.TOTAL_KG.code,
    @ColumnInfo(defaultValue = "0")val chorusDelay: Long = 0L,
    @ColumnInfo(defaultValue = "80000") val restPeriod: Long = 80000L
)