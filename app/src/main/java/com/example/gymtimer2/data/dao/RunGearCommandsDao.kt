package com.example.gymtimer2.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gymtimer2.data.entity.RunGearEntity

@Dao
interface RunGearCommandsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(gear: RunGearEntity): Long

    @Query("UPDATE run_gears SET is_default = 0 WHERE is_default = 1")
    suspend fun clearDefault()

    @Query("UPDATE run_gears SET is_default = 0 WHERE id = :gearId")
    suspend fun clearGearDefault(gearId: Long)

    @Query("UPDATE run_gears SET is_default = 1 WHERE id = :gearId")
    suspend fun markDefault(gearId: Long)

    @Query("UPDATE run_gears SET is_retired = :retired WHERE id = :gearId")
    suspend fun updateRetired(gearId: Long, retired: Boolean)

    @Query("UPDATE run_gears SET name = :name WHERE id = :gearId")
    suspend fun updateName(gearId: Long, name: String)

    @Query("UPDATE run_gears SET photo_file_name = :fileName WHERE id = :gearId")
    suspend fun updatePhotoFileName(gearId: Long, fileName: String?): Int
}
