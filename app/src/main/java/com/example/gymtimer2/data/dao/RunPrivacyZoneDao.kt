package com.example.gymtimer2.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.gymtimer2.data.entity.RunPrivacyZoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunPrivacyZoneDao {
    @Query("SELECT * FROM run_privacy_zones ORDER BY id")
    fun observeAll(): Flow<List<RunPrivacyZoneEntity>>

    @Query("SELECT * FROM run_privacy_zones ORDER BY id")
    suspend fun getAll(): List<RunPrivacyZoneEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(zone: RunPrivacyZoneEntity): Long

    @Update
    suspend fun update(zone: RunPrivacyZoneEntity): Int

    @Delete
    suspend fun delete(zone: RunPrivacyZoneEntity): Int

    @Query("SELECT * FROM run_privacy_zones WHERE id = :id")
    suspend fun get(id: Long): RunPrivacyZoneEntity?
}
