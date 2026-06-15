package com.yeon.todaymorning.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.yeon.todaymorning.ALARM_TRIGGER"
        private const val ALARM_REQUEST_CODE = 0
        private const val MISSION_FAIL_REQUEST_CODE = 100
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAfterSeconds(seconds: Int) {
        val triggerAt = System.currentTimeMillis() + seconds * 1000L
        scheduleAt(triggerAt)
    }

    /**
     * 오늘 hour:minute 시각. 이미 지났으면 내일로 보정한 "다음 발생 시각"(ms).
     * 알람·자동실패 등록 시점 계산을 한 곳으로 모은다.
     */
    fun nextDailyTrigger(hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis

    /**
     * 매일 반복 알람을 다음 발생 시각에 등록한다.
     * setAlarmClock은 1회성이므로, 알람이 울리는 시점(AlarmReceiver)과 부팅 복원 시
     * 이 메서드로 다음날을 다시 등록해야 "매일 반복"이 성립한다.
     */
    fun scheduleDailyAlarm(hour: Int, minute: Int) {
        scheduleAt(nextDailyTrigger(hour, minute))
    }

    /** 매일 반복 자동실패 알람을 다음 발생 시각에 등록한다. */
    fun scheduleDailyMissionFail(hour: Int, minute: Int) {
        scheduleMissionFailAt(nextDailyTrigger(hour, minute))
    }

    /**
     * 목표 시각에 자동 실패 처리 알람 등록.
     * 이미 지난 시각이면 내일로 보정. 사용자가 그 전에 "탑승 완료"를 누르면
     * MissionRepository가 성공 기록을 우선하므로 실패가 덮어쓰지 않는다.
     */
    fun scheduleMissionFailAt(triggerAtMillis: Long) {
        if (!canScheduleExactAlarms()) return
        val intent = Intent(context, MissionFailReceiver::class.java).apply {
            action = MissionFailReceiver.ACTION_MISSION_FAIL
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MISSION_FAIL_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
        )
        Log.d("AlarmScheduler", "자동실패 알람 등록: ${java.util.Date(triggerAtMillis)}")
    }

    fun cancelMissionFail() {
        val intent = Intent(context, MissionFailReceiver::class.java).apply {
            action = MissionFailReceiver.ACTION_MISSION_FAIL
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MISSION_FAIL_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleAt(triggerAtMillis: Long) {
        if (!canScheduleExactAlarms()) {
            Log.w("AlarmScheduler", "정확한 알람 권한 없음 — 스케줄 취소")
            return
        }
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent),
            pendingIntent
        )
        Log.d("AlarmScheduler", "알람 등록 완료: ${java.util.Date(triggerAtMillis)}")
    }

    fun cancel() {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
