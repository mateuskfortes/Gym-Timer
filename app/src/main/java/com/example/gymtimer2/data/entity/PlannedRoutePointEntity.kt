package com.example.gymtimer2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "planned_route_points",
    primaryKeys = ["route_id", "point_index"],
    foreignKeys = [
        ForeignKey(
            entity = PlannedRouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["route_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlannedRoutePointEntity(
    @ColumnInfo(name = "route_id")
    val routeId: Long,
    @ColumnInfo(name = "point_index")
    val pointIndex: Int,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "segment_index")
    val segmentIndex: Int,
    @ColumnInfo(name = "altitude_meters")
    val altitudeMeters: Double? = null
)
