package com.yeon.todaymorning.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MissionRecord): Long

    @Query("SELECT * FROM mission_records ORDER BY createdAt DESC")
    fun getAllRecords(): Flow<List<MissionRecord>>

    @Query("SELECT * FROM mission_records WHERE date = :date LIMIT 1")
    suspend fun getRecordByDate(date: String): MissionRecord?

    @Query("SELECT COUNT(*) FROM mission_records WHERE isSuccess = 1")
    fun getSuccessCount(): Flow<Int>
}
