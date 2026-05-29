package com.yeon.todaymorning

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.kakao.vectormap.KakaoMapSdk
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TodayCommuteApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        // 카카오맵 SDK 초기화 — 네이티브 앱 키는 local.properties → BuildConfig 주입
        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "오늘도출근 알람",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "매일 아침 출근 알람"
                enableLights(true)
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ALARM_CHANNEL_ID = "alarm_channel"
        const val ALARM_NOTIFICATION_ID = 1001
    }
}
