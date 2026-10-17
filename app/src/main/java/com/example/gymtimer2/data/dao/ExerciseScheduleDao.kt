package com.example.gymtimer2.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gymtimer2.data.entity.ExerciseScheduleEntity
import com.example.gymtimer2.data.relation.ScheduledExerciseRelation
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseScheduleDao {
    @Query("""
        SELECT e.*, s.day_id AS schedule_day_id, s.sort_order AS schedule_sort_order
        FROM exercises e INNER JOIN exercise_schedule s ON s.exercise_id = e.id
        ORDER BY s.day_id, s.sort_order, e.id
    """)
    fun observeScheduledExercises(): Flow<List<ScheduledExerciseRelation>>

    @Query("""
        SELECT e.*, s.day_id AS schedule_day_id, s.sort_order AS schedule_sort_order
        FROM exercises e INNER JOIN exercise_schedule s ON s.exercise_id = e.id
        WHERE s.day_id = :dayId
        ORDER BY s.sort_order, e.id
    """)
    fun observeScheduledExercisesForDay(dayId: Long): Flow<List<ScheduledExerciseRelation>>

    @Query("SELECT exercise_id FROM exercise_schedule WHERE day_id = :dayId ORDER BY sort_order, exercise_id")
    suspend fun orderedExerciseIds(dayId: Long): List<Int>

    @Query("SELECT day_id FROM exercise_schedule WHERE exercise_id = :exerciseId")
    suspend fun dayIdForExercise(exerciseId: Int): Long?

    @Query("SELECT COUNT(*) FROM exercise_schedule WHERE day_id = :dayId")
    suspend fun countInDay(dayId: Long): Int

    @Insert
    suspend fun insert(schedule: ExerciseScheduleEntity)

    @Query("UPDATE exercise_schedule SET day_id = :dayId, sort_order = :sortOrder WHERE exercise_id = :exerciseId")
    suspend fun move(exerciseId: Int, dayId: Long, sortOrder: Int)

    @Query("UPDATE exercise_schedule SET sort_order = :sortOrder WHERE exercise_id = :exerciseId")
    suspend fun updateOrder(exerciseId: Int, sortOrder: Int)
}
