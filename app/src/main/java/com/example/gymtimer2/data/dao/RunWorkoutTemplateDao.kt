package com.example.gymtimer2.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gymtimer2.data.entity.RunWorkoutTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunWorkoutTemplateDao {
    @Query("SELECT * FROM run_workout_templates ORDER BY id")
    fun observeAll(): Flow<List<RunWorkoutTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(template: RunWorkoutTemplateEntity): Long

    @Query("DELETE FROM run_workout_templates WHERE id = :id")
    suspend fun delete(id: Long)
}
