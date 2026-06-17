package com.yeon.todaymorning.data.repository

import com.yeon.todaymorning.data.db.MissionDao
import com.yeon.todaymorning.data.db.MissionRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MissionRepository @Inject constructor(
    private val dao: MissionDao
) {
    fun getAllRecords(): Flow<List<MissionRecord>> = dao.getAllRecords()

    fun getRecentRecords(limit: Int = 7): Flow<List<MissionRecord>> = dao.getRecentRecords(limit)

    fun getSuccessCount(): Flow<Int> = dao.getSuccessCount()

    fun getTotalCount(): Flow<Int> = dao.getTotalCount()

    suspend fun insert(record: MissionRecord): Long = dao.insert(record)

    suspend fun getRecordByDate(date: String): MissionRecord? = dao.getRecordByDate(date)

    /** 선택한 id들의 기록을 삭제. */
    suspend fun deleteRecords(ids: List<Long>): Int = dao.deleteByIds(ids)

    /**
     * 오늘 미션 결과를 안전하게 기록한다.
     *
     * - 오늘 기록 없음 → insert
     * - 오늘 성공 기록 있음 → 무시 (성공은 변경 불가)
     * - 오늘 실패 기록 있고 새 결과가 성공 → 성공으로 업데이트
     * - 오늘 실패 기록 있고 새 결과도 실패 → 중복 무시
     */
    suspend fun insertTodayResult(record: MissionRecord) {
        val existing = dao.getRecordByDate(record.date)
        when {
            existing == null -> dao.insert(record)
            existing.isSuccess -> { /* 이미 성공 — 아무것도 하지 않음 */ }
            record.isSuccess -> dao.updateToSuccess(record.date, record.boardedTime)
            else -> { /* 실패 중복 — 무시 */ }
        }
    }

    /** 오늘 포함 최근 연속 성공 streak 계산 */
    suspend fun getCurrentStreak(): Int {
        var streak = 0
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        // 오늘부터 하루씩 거슬러 올라가며 성공 기록 확인
        for (i in 0 until 365) {
            val dateStr = fmt.format(cal.time)
            val record = dao.getRecordByDate(dateStr)
            if (record != null && record.isSuccess) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    /** 성공률 계산 (0.0 ~ 1.0) */
    fun getSuccessRate(): Flow<Float> = combine(
        dao.getSuccessCount(),
        dao.getTotalCount()
    ) { success, total ->
        if (total == 0) 0f else success.toFloat() / total
    }
}
