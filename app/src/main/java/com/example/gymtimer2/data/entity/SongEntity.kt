package com.example.gymtimer2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity (
    @PrimaryKey
    val id: Long, // id from MediaStore

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "artist")
    val artist: String,

    @ColumnInfo(name = "uri")
    val uri: String,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long? = null,

    @ColumnInfo(name = "album")
    val album: String? = null,


    @ColumnInfo(name = "start_at_ms", defaultValue = "0")
    val startAtMs: Long = 0
)