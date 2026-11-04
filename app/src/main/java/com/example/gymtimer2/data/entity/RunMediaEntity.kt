package com.example.gymtimer2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gymtimer2.domain.model.RunMedia

@Entity(
    tableName = "run_photos",
    foreignKeys = [
        ForeignKey(
            entity = RunEntity::class,
            parentColumns = ["id"],
            childColumns = ["run_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["run_id", "file_name"], unique = true)]
)
data class RunMediaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "run_id")
    val runId: Long,
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "is_featured", defaultValue = "0")
    val isFeatured: Boolean = false,
    @ColumnInfo(name = "is_video", defaultValue = "0")
    val isVideo: Boolean = false
)

internal fun RunMediaEntity.toModel() = RunMedia(id, fileName, isFeatured, isVideo)
