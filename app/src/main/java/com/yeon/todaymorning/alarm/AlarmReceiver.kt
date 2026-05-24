package com.yeon.todaymorning.alarm

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.yeon.todaymorning.R
import com.yeon.todaymorning.TodayCommuteApp
import com.yeon.todaymorning.data.datastore.UserSettingsDataStore
import com.yeon.todaymorning.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AlarmScheduler.ACTION_ALARM_TRIGGER -> fireAlarmNotification(context)
            Intent.ACTION_BOOT_COMPLETED -> restoreAlarmAfterBoot(context)
        }
    }

    private fun fireAlarmNotification(context: Context) {
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_FROM_ALARM, true)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (!hasNotificationPermission) {
            // 알림 권한 없음 → 직접 Activity 실행 (잠금화면 위로)
            Toast.makeText(context, "알람! 알림 권한을 허용해주세요.", Toast.LENGTH_LONG).show()
            context.startActivity(activityIntent)
            return
        }

        val notification = NotificationCompat.Builder(context, TodayCommuteApp.ALARM_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("오늘도출근 ⏰")
            .setContentText("알람이 울립니다! 탭해서 확인하세요.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // 소리 + 진동 기본값
            .build()

        NotificationManagerCompat.from(context)
            .notify(TodayCommuteApp.ALARM_NOTIFICATION_ID, notification)
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
