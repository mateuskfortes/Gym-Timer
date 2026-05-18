package com.example.gymtimer2.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gymtimer2.data.entity.ChorusEntity
import com.example.gymtimer2.data.entity.ExerciseChorusEntity
import com.example.gymtimer2.data.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseChorusDao {

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insert(relation: ExerciseChorusEntity)

	@Delete
	suspend fun delete(relation: ExerciseChorusEntity)

	@Query("DELETE FROM exercise_choruses WHERE exercise_id = :exerciseId AND chorus_id = :chorusId")
	suspend fun deleteByIds(exerciseId: Int, chorusId: Long)

	@Query(
		"""
		SELECT c.*
		FROM choruses c
		INNER JOIN exercise_choruses ec ON ec.chorus_id = c.id
		WHERE ec.exercise_id = :exerciseId
		ORDER BY c.start_ms ASC
		"""
	)
	fun getChorusesByExerciseId(exerciseId: Int): Flow<List<ChorusEntity>>

	@Query(
		"""
		SELECT e.*
		FROM exercises e
		INNER JOIN exercise_choruses ec ON ec.exercise_id = e.id
		WHERE ec.chorus_id = :chorusId
		ORDER BY e.name ASC
		"""
	)
	fun getExercisesByChorusId(chorusId: Long): Flow<List<ExerciseEntity>>

	@Query("DELETE FROM exercise_choruses WHERE exercise_id = :exerciseId")
	suspend fun deleteByExerciseId(exerciseId: Int)

	@Query("DELETE FROM exercise_choruses WHERE chorus_id = :chorusId")
	suspend fun deleteByChorusId(chorusId: Long)
}

