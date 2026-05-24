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
            } finally {
                pending.finish()
            }
        }
    }
}
