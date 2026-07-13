package com.example.shikagera1.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DayEntryDao {
    @Query("SELECT * FROM day_entries WHERE dateIso BETWEEN :startIso AND :endIso ORDER BY dateIso DESC")
    fun observeRange(startIso: String, endIso: String): Flow<List<DayEntryEntity>>

    @Query("SELECT * FROM day_entries WHERE dateIso = :dateIso LIMIT 1")
    suspend fun getByDate(dateIso: String): DayEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DayEntryEntity)

    @Query("SELECT * FROM day_entries WHERE dateIso < :beforeIso ORDER BY dateIso ASC")
    suspend fun getBefore(beforeIso: String): List<DayEntryEntity>

    @Query("DELETE FROM day_entries WHERE dateIso < :beforeIso")
    suspend fun deleteBefore(beforeIso: String)
}