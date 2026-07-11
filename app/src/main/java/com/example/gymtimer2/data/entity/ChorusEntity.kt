package com.example.gymtimer2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "choruses",
    indices = [Index(value = ["song_id"])],
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["song_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChorusEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "song_id")
    val songId: Long,

    @ColumnInfo(name = "name")
    val name: String = "",

    @ColumnInfo(name = "start_ms")
    val startMs: Long,

    @ColumnInfo(name = "volume")
    val volume: Float? = null
)

