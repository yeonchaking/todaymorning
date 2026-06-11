package com.yeon.todaymorning.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yeon.todaymorning.data.datastore.UserSettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

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
     */
    private fun fireAlarm(context: Context) {
        AlarmRingService.start(context)
    }

    private fun restoreAlarmAfterBoot(context: Context) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dataStore = UserSettingsDataStore(context)
                val settings = dataStore.userSettings.first()
                val scheduler = AlarmScheduler(context)

                val triggerAt = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, settings.alarmHour)
                    set(Calendar.MINUTE, settings.alarmMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }.timeInMillis

                scheduler.scheduleAt(triggerAt)
            } finally {
                pending.finish()
            }
        }
    }
}
