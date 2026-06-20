package com.yeon.todaymorning.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.yeon.todaymorning.domain.model.UserSettings
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
     * 반복 요일을 반영한 다음 발생 시각. hour:minute 기준으로, 오늘 이후(이미 지났으면 내일부터)
     * 최대 7일 안에서 [repeatDays](Calendar 요일값)에 해당하는 첫 날을 찾는다.
     * 켜진 요일이 없으면 null → 호출부에서 등록을 건너뛴다.
     *
     * 요일 게이팅을 "알람이 울린 뒤 차단"이 아니라 등록 시점 계산으로 처리하는 이유:
     * setAlarmClock은 1회성이고 매일 반복은 재등록으로 흉내내므로, 꺼진 요일은
     * 애초에 등록되지 않아야 알람음이 울리지 않는다.
     */
    fun nextDailyTrigger(hour: Int, minute: Int, repeatDays: Set<Int>): Long? {
        if (repeatDays.isEmpty()) return null
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        repeat(7) {
            if (cal.get(Calendar.DAY_OF_WEEK) in repeatDays) return cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
    }

    /**
     * 알람 활성화·반복 요일을 한 번에 반영하는 단일 진입점.
     * 마스터 스위치 OFF 이거나 반복 요일이 없으면 알람·자동실패를 모두 취소한다.
     */
    fun applyAlarm(settings: UserSettings) {
        if (!settings.alarmActive) {
            cancel()
            cancelMissionFail()
            Log.d("AlarmScheduler", "알람 비활성(스위치 OFF 또는 요일 없음) — 전체 취소")
            return
        }
        scheduleDailyAlarm(settings.alarmHour, settings.alarmMinute, settings.repeatDays)
        scheduleDailyMissionFail(settings.targetHour, settings.targetMinute, settings.repeatDays)
    }

    /**
     * 반복 알람을 다음(켜진 요일) 발생 시각에 등록한다.
     * setAlarmClock은 1회성이므로, 알람이 울리는 시점(AlarmReceiver)과 부팅 복원 시
     * 이 메서드로 다음 발생을 다시 등록해야 "반복"이 성립한다.
     */
    fun scheduleDailyAlarm(hour: Int, minute: Int, repeatDays: Set<Int>) {
        val triggerAt = nextDailyTrigger(hour, minute, repeatDays) ?: run {
            cancel()
            return
        }
        scheduleAt(triggerAt)
    }

    /** 반복 자동실패 알람을 다음(켜진 요일) 발생 시각에 등록한다. */
    fun scheduleDailyMissionFail(hour: Int, minute: Int, repeatDays: Set<Int>) {
        val triggerAt = nextDailyTrigger(hour, minute, repeatDays) ?: run {
            cancelMissionFail()
            return
        }
        scheduleMissionFailAt(triggerAt)
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
