package com.example.gymtimer2.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.gymtimer2.data.entity.ChorusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChorusDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chorus: ChorusEntity): Long

    @Update
    suspend fun update(chorus: ChorusEntity)

    @Delete
    suspend fun delete(chorus: ChorusEntity)

    @Query("SELECT * FROM choruses WHERE song_id = :songId ORDER BY start_ms ASC")
    fun getChorusesBySongId(songId: Long): Flow<List<ChorusEntity>>

    @Query("DELETE FROM choruses WHERE song_id = :songId")
    suspend fun deleteChorusesBySongId(songId: Long)

}

