package com.example.gymtimer2.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.gymtimer2.data.entity.ChorusEntity
import com.example.gymtimer2.data.relation.ChorusWithSongRelation
import com.example.gymtimer2.data.relation.ExerciseWithChorusesRelation
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

    @Query("SELECT * FROM choruses ORDER BY song_id, start_ms ASC")
    fun getAllChoruses(): Flow<List<ChorusEntity>>

    @Transaction
    @Query("""
        SELECT c.* FROM choruses c
        INNER JOIN exercise_choruses ec
            ON c.id = ec.chorus_id
        WHERE ec.exercise_id = :exerciseId
    """)
    fun getChorusesWithSongsByExerciseId(exerciseId: Int): Flow<List<ChorusWithSongRelation>>

    @Transaction
    @Query("""
        SELECT * FROM exercises
        WHERE id = :exerciseId
    """)
    fun getExerciseWithChorusesByExerciseId(exerciseId: Int): Flow<ExerciseWithChorusesRelation>

    @Query("DELETE FROM choruses WHERE song_id = :songId")
    suspend fun deleteChorusesBySongId(songId: Long)

}

