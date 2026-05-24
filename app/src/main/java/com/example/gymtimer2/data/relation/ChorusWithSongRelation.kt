package com.example.gymtimer2.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.gymtimer2.data.entity.ChorusEntity
import com.example.gymtimer2.data.entity.SongEntity

data class ChorusWithSongRelation(

    @Embedded
    val chorus: ChorusEntity,

    @Relation(
        parentColumn = "song_id",
        entityColumn = "id"
    )
    val song: SongEntity
)