package com.example.gymtimer2.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.gymtimer2.data.entity.WorkoutDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDayDao {
    @Query("SELECT * FROM workout_days ORDER BY id")
    fun observeDays(): Flow<List<WorkoutDayEntity>>

    @Query("SELECT * FROM workout_days WHERE id = :id")
    suspend fun getById(id: Long): WorkoutDayEntity?

    @Query("SELECT COUNT(*) FROM workout_days")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM workout_days WHERE name = :name COLLATE NOCASE")
    suspend fun countByName(name: String): Int

    @Insert
    suspend fun insert(day: WorkoutDayEntity): Long

    @Update
    suspend fun update(day: WorkoutDayEntity)

    @Delete
    suspend fun delete(day: WorkoutDayEntity)
}
