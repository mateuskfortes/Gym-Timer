package com.example.gymtimer2.data.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "planned_routes")
data class PlannedRouteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val distanceMeters: Double,
    val importedAtEpochMs: Long,
    @ColumnInfo(name = "is_favorite", defaultValue = "0")
    val isFavorite: Boolean = false,
    @ColumnInfo(name = "is_local_segment", defaultValue = "0")
    val isLocalSegment: Boolean = false
)
