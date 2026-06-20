package com.yeon.todaymorning.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yeon.todaymorning.data.datastore.UserSettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AlarmScheduler.ACTION_ALARM_TRIGGER -> fireAlarm(context)
            Intent.ACTION_BOOT_COMPLETED -> restoreAlarmAfterBoot(context)
        }
    }

    /**
     * 알람음을 소유하는 포그라운드 서비스를 시작한다.
     * 서비스가 풀스크린 인텐트로 [com.yeon.todaymorning.ui.AlarmRingActivity]를
     * 잠금화면 위에 띄우고, 사용자가 슬라이드로 해제할 때까지 알람음·진동을 반복한다.
     *
     * setAlarmClock은 1회성이므로, 여기서 곧바로 다음날 같은 시각에 알람을
     * 다시 등록해야 "매일 반복"이 성립한다. (자동실패 알람은 MissionFailReceiver가
     * 자기 발생 시점에 다음날을 재등록한다 — 같은 PendingIntent를 여기서 덮어쓰면
     * 오늘분 자동실패가 사라지므로 분리한다.)
     */
    private fun fireAlarm(context: Context) {
        AlarmRingService.start(context)

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = UserSettingsDataStore(context).userSettings.first()
                // 마스터 스위치 OFF 또는 반복 요일 없음이면 다음 발생을 재등록하지 않는다.
                if (settings.alarmActive) {
                    AlarmScheduler(context).scheduleDailyAlarm(
                        settings.alarmHour, settings.alarmMinute, settings.repeatDays
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun restoreAlarmAfterBoot(context: Context) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = UserSettingsDataStore(context).userSettings.first()
                // 활성화·요일·취소를 한 번에 반영 (OFF면 내부에서 취소 처리).
                AlarmScheduler(context).applyAlarm(settings)
            } finally {
                pending.finish()
            }
        }
    }
}
