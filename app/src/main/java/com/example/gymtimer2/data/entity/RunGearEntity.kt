package com.example.gymtimer2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "run_gears", indices = [Index(value = ["name"], unique = true)])
data class RunGearEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    @ColumnInfo(name = "is_default", defaultValue = "0") val isDefault: Boolean = false,
    @ColumnInfo(name = "is_retired", defaultValue = "0") val isRetired: Boolean = false,
    @ColumnInfo(name = "photo_file_name") val photoFileName: String? = null
)

data class RunGearSummaryRow(
    val id: Long,
    val name: String,
    val distanceMeters: Double,
    val isDefault: Boolean,
    val isRetired: Boolean,
    val photoFileName: String?
)
