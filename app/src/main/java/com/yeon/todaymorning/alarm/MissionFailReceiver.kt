package com.yeon.todaymorning.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yeon.todaymorning.data.datastore.UserSettingsDataStore
import com.yeon.todaymorning.data.db.AppDatabase
import com.yeon.todaymorning.data.db.MissionRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class MissionFailReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_MISSION_FAIL = "com.yeon.todaymorning.MISSION_FAIL"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MISSION_FAIL) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dataStore = UserSettingsDataStore(context)
                val settings = dataStore.userSettings.first()
                val dao = AppDatabase.getInstance(context).missionDao()
                val now = System.currentTimeMillis()
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)

                // 오늘 이미 성공 기록이 있으면 실패 기록 안 씀
                val existing = dao.getRecordByDate(dateStr)
                if (existing == null || !existing.isSuccess) {
                    dao.insert(
                        MissionRecord(
                            date = dateStr,
                            alarmTime = "%02d:%02d".format(settings.alarmHour, settings.alarmMinute),
                            targetTime = "%02d:%02d".format(settings.targetHour, settings.targetMinute),
                            boardedTime = null,
                            isSuccess = false
                        )
                    )
                }

                // 자동실패 알람도 1회성(setExact)이므로, 오늘분이 발생한 지금
                // 다음(켜진 요일) 목표 시각에 다시 등록해 반복되게 한다.
                // 마스터 스위치 OFF 또는 반복 요일 없음이면 재등록하지 않는다.
                if (settings.alarmActive) {
                    AlarmScheduler(context).scheduleDailyMissionFail(
                        settings.targetHour, settings.targetMinute, settings.repeatDays
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }
}
