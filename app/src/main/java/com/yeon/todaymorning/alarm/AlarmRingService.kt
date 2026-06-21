package com.yeon.todaymorning.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.yeon.todaymorning.R
import com.yeon.todaymorning.data.datastore.UserSettingsDataStore
import com.yeon.todaymorning.ui.AlarmRingActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * 알람음을 "소유"하는 포그라운드 서비스.
 *
 * - 알람음을 STREAM_ALARM(USAGE_ALARM)으로 루프 재생 → 무음/진동 모드에서도 알람 볼륨으로 울림
 * - 진동 패턴 반복
 * - 풀스크린 인텐트 알림으로 [AlarmRingActivity]를 잠금화면 위에 띄움
 * - 사용자가 슬라이드로 해제하면 [ACTION_STOP]으로 정지
 *
 * 서비스가 소리를 들고 있으므로, 앱이 강제종료돼도 알림이 남아 다시 진입할 수 있다.
 */
class AlarmRingService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var rawAfd: AssetFileDescriptor? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAlarm()
                return START_NOT_STICKY
            }
            else -> startAlarm()
        }
        return START_STICKY
    }

    private fun startAlarm() {
        startForeground(NOTIFICATION_ID, buildNotification())
        startSound()
        startVibration()
    }

    private fun startSound() {
        if (mediaPlayer != null) return
        // 사용자가 고른 알람음 id 를 읽는다(서비스는 Hilt 미적용 → DataStore 직접 생성).
        val soundId = runCatching {
            runBlocking { UserSettingsDataStore(applicationContext).userSettings.first().alarmSoundId }
        }.getOrDefault(AlarmSounds.DEFAULT_ID)

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                if (!applyDataSource(this, soundId)) {
                    // 선택 음원 적용 실패 → 시스템 기본 알람음으로 폴백
                    applyDefaultDataSource(this)
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "알람음 재생 실패", e)
        }
    }

    /** soundId 규칙(빈값=기본, builtin:=내장, 그 외=시스템 URI)에 맞춰 데이터소스 지정. 성공 여부 반환. */
    private fun applyDataSource(mp: MediaPlayer, soundId: String): Boolean = runCatching {
        when {
            AlarmSounds.isBuiltIn(soundId) -> {
                val builtIn = AlarmSounds.findBuiltIn(soundId) ?: return false
                val afd = resources.openRawResourceFd(builtIn.resId) ?: return false
                rawAfd = afd  // prepare 동안 fd 가 열려 있어야 하므로 정지 시점까지 보관
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                true
            }
            soundId.isNotBlank() -> {
                mp.setDataSource(this, Uri.parse(soundId))
                true
            }
            else -> applyDefaultDataSource(mp)
        }
    }.getOrDefault(false)

    private fun applyDefaultDataSource(mp: MediaPlayer): Boolean = runCatching {
        val uri = AlarmSounds.defaultAlarmUri() ?: return false
        mp.setDataSource(this, uri)
        true
    }.getOrDefault(false)

    private fun startVibration() {
        // 사용자가 고른 진동 패턴 id 를 읽는다(서비스는 Hilt 미적용 → DataStore 직접 생성).
        val patternId = runCatching {
            runBlocking { UserSettingsDataStore(applicationContext).userSettings.first().vibrationPatternId }
        }.getOrDefault(VibrationPatterns.DEFAULT_ID)

        // OFF 면 진동 자체를 시작하지 않는다.
        val pattern = VibrationPatterns.waveformOf(patternId) ?: return

        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator = vib
        // 선택한 패턴을 무한 반복(repeat index 0) 재생
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(pattern, 0)
        }
    }

    private fun stopAlarm() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "알람음 정지 실패", e)
        }
        mediaPlayer = null
        runCatching { rawAfd?.close() }
        rawAfd = null
        vibrator?.cancel()
        vibrator = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }

    private fun buildNotification(): android.app.Notification {
        createChannel()

        val fullScreenIntent = Intent(this, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("오늘도출근 ⏰")
            .setContentText("알람이 울리고 있어요. 탭해서 해제하세요.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "오늘도출근 알람 울림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "알람이 울리는 동안 표시되는 알림"
                setSound(null, null) // 소리는 서비스의 MediaPlayer가 담당
                enableVibration(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "AlarmRingService"
        const val CHANNEL_ID = "alarm_ring_channel"
        const val NOTIFICATION_ID = 1002
        const val ACTION_STOP = "com.yeon.todaymorning.ALARM_RING_STOP"

        fun start(context: Context) {
            val intent = Intent(context, AlarmRingService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AlarmRingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
