package com.example.gymtimer2.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gymtimer2.data.entity.RunMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunMediaDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRunMedia(media: List<RunMediaEntity>): List<Long>

    @Query("SELECT * FROM run_photos WHERE run_id = :runId ORDER BY id")
    fun observeRunMedia(runId: Long): Flow<List<RunMediaEntity>>

    @Query("SELECT * FROM run_photos WHERE run_id = :runId ORDER BY id")
    suspend fun getRunMedia(runId: Long): List<RunMediaEntity>

    @Query("SELECT file_name FROM run_photos WHERE id = :mediaId")
    suspend fun getMediaFileName(mediaId: Long): String?

    @Query("DELETE FROM run_photos WHERE id = :mediaId")
    suspend fun deleteRunMedia(mediaId: Long): Int

    @Query(
        "UPDATE run_photos SET is_featured = CASE WHEN id = :mediaId THEN 1 ELSE 0 END " +
            "WHERE run_id = :runId AND EXISTS " +
            "(SELECT 1 FROM run_photos WHERE id = :mediaId AND run_id = :runId)"
    )
    suspend fun setFeaturedMedia(runId: Long, mediaId: Long)

    @Query("SELECT COUNT(*) FROM run_photos WHERE run_id = :runId")
    suspend fun countRunMedia(runId: Long): Int

}
