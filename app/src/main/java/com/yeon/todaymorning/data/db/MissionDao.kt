package com.yeon.todaymorning.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {
    /** 신규 삽입만. 같은 date가 이미 있으면 -1 반환 (무시). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: MissionRecord): Long

    /** 실패 기록을 성공으로 업데이트. isSuccess=0인 경우에만 동작. */
    @Query("UPDATE mission_records SET isSuccess = 1, boardedTime = :boardedTime WHERE date = :date AND isSuccess = 0")
    suspend fun updateToSuccess(date: String, boardedTime: String?): Int

    @Query("SELECT * FROM mission_records ORDER BY createdAt DESC")
    fun getAllRecords(): Flow<List<MissionRecord>>

    @Query("SELECT * FROM mission_records WHERE date = :date LIMIT 1")
    suspend fun getRecordByDate(date: String): MissionRecord?

    @Query("SELECT COUNT(*) FROM mission_records WHERE isSuccess = 1")
    fun getSuccessCount(): Flow<Int>

    @Query("SELECT * FROM mission_records ORDER BY date DESC LIMIT :limit")
    fun getRecentRecords(limit: Int = 7): Flow<List<MissionRecord>>

    @Query("SELECT COUNT(*) FROM mission_records")
    fun getTotalCount(): Flow<Int>
}
