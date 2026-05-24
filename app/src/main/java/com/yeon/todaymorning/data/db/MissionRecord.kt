package com.yeon.todaymorning.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mission_records")
data class MissionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,           // "2026-04-20" 형식
    val alarmTime: String,      // "07:00"
    val targetTime: String,     // "09:00"
    val boardedTime: String?,   // 탑승 완료 시각 (null = 실패)
    val isSuccess: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)
