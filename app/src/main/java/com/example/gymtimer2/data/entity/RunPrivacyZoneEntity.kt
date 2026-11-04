package com.example.gymtimer2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.gymtimer2.domain.model.RunPrivacyZone

@Entity(tableName = "run_privacy_zones")
data class RunPrivacyZoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "radius_meters") val radiusMeters: Int
)

fun RunPrivacyZoneEntity.toModel() = RunPrivacyZone(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters
)

fun RunPrivacyZone.toEntity() = RunPrivacyZoneEntity(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters
)
