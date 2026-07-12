package com.yeon.todaymorning.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.yeon.todaymorning.R
// import com.yeon.todaymorning.data.datastore.UserSettingsDataStore  // 1.0 릴리즈: 플로팅 위젯 비활성화
import com.yeon.todaymorning.domain.model.TransitArrival
import com.yeon.todaymorning.mission.MissionEngine
// import com.yeon.todaymorning.mission.MissionOverlay  // 1.0 릴리즈: 플로팅 위젯 비활성화
import com.yeon.todaymorning.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 미션이 진행되는 동안 **화면과 무관하게** [MissionEngine] 을 살려 두는 포그라운드 서비스.
 *
 * - 화면이 꺼지거나 Activity 가 파괴돼도 도착 폴링·카운트다운·TTS 음성안내가 계속 돈다.
 * - 포그라운드 서비스는 알림이 필수 — 상시 알림에 **목표까지 남은시간(실시간 카운트다운)**과
 *   **다음 버스 도착정보**를 띄우고, 탭하면 타임어택 화면으로 복귀한다.
 *   · 남은시간: 시스템 크로노미터(카운트다운)로 표시 → 1초마다 재알림 없이 알아서 줄어든다.
 *   · 다음 버스: 엔진의 도착정보가 갱신될 때(약 10초)마다 알림 내용을 다시 그린다.
 * - 엔진이 미션 종료([MissionEngine.finished])를 알리면 정리 후 스스로 내려간다.
 */
@AndroidEntryPoint
class MissionService : Service() {

    @Inject lateinit var engine: MissionEngine
    // 1.0 릴리즈: 플로팅 위젯 비활성화 — watchWidgetFlag 전용이었음 (P2 재활성화 시 해제)
    // @Inject lateinit var dataStore: UserSettingsDataStore

    private val scope = CoroutineScope(SupervisorJob())
    private var watchJob: Job? = null
    private var uiJob: Job? = null

    // 1.0 릴리즈: 플로팅 위젯 비활성화 (P2 재활성화 시 해제 — Manifest 권한도 함께 복원할 것)
    // private var flagJob: Job? = null
    // private val overlay by lazy { MissionOverlay(this) }
    // @Volatile private var widgetEnabled = true
    // @Volatile private var widgetOpacity = 90

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            shutdown()
            return START_NOT_STICKY
        }
        startForegroundCompat()
        engine.start()
        watchFinished()
        // watchWidgetFlag()  // 1.0 릴리즈: 플로팅 위젯 비활성화
        watchUi()
        return START_STICKY
    }

    private fun watchFinished() {
        if (watchJob != null) return
        watchJob = scope.launch {
            engine.finished.collectLatest { done ->
                if (done) shutdown()
            }
        }
    }

    // 1.0 릴리즈: 플로팅 위젯 비활성화 (P2 재활성화 시 해제)
    // /** 미션 화면 토글(DataStore)을 구독해 위젯 표시 여부를 실시간 반영. */
    // private fun watchWidgetFlag() {
    //     if (flagJob != null) return
    //     flagJob = scope.launch {
    //         dataStore.userSettings.collectLatest {
    //             widgetEnabled = it.floatingWidgetEnabled
    //             widgetOpacity = it.floatingWidgetOpacity
    //         }
    //     }
    // }

    /**
     * 알림 + 플로팅 위젯을 주기적으로 다시 그린다 — 엔진 현재값을 직접 읽어 emission 타이밍에
     * 의존하지 않는다. View(오버레이) 조작은 메인 스레드여야 하므로 Dispatchers.Main 에서 돈다.
     * 알림 남은시간은 크로노미터(시스템)가 담당하므로 알림은 3초마다, 위젯 시:분:초는 1초마다 갱신.
     */
    private fun watchUi() {
        if (uiJob != null) return
        uiJob = scope.launch(Dispatchers.Main) {
            var tick = 0
            while (true) {
                val remaining = engine.remainingSeconds.value
                val arrivals = engine.arrivals.value

                // 1.0 릴리즈: 플로팅 위젯 비활성화 (P2 재활성화 시 해제)
                // val busText = nextBusText(arrivals)
                // // 플로팅 위젯: 토글 ON + 권한 있을 때만 표시.
                // if (widgetEnabled && overlay.canDraw()) {
                //     if (!overlay.isShown) overlay.show()
                //     overlay.update(formatRemaining(remaining), busText, widgetOpacity)
                // } else if (overlay.isShown) {
                //     overlay.hide()
                // }

                // 알림은 3초마다 갱신(과도한 notify 방지).
                if (tick % 3 == 0) {
                    val nm = getSystemService(NotificationManager::class.java)
                    nm.notify(NOTIFICATION_ID, buildNotification(arrivals, remaining))
                }
                tick++
                delay(1000L)
            }
        }
    }

    private fun shutdown() {
        engine.stop()
        watchJob?.cancel(); watchJob = null
        uiJob?.cancel(); uiJob = null
        // flagJob?.cancel(); flagJob = null  // 1.0 릴리즈: 플로팅 위젯 비활성화
        // overlay.hide()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        watchJob?.cancel(); watchJob = null
        uiJob?.cancel(); uiJob = null
        // flagJob?.cancel(); flagJob = null  // 1.0 릴리즈: 플로팅 위젯 비활성화
        // overlay.hide()
    }

    // 1.0 릴리즈: 플로팅 위젯 비활성화 — 위젯 표시 전용 헬퍼 (P2 재활성화 시 해제)
    // /** 남은초 → "12:34" / "1:02:03". 음수면 0 처리. */
    // private fun formatRemaining(remainingSeconds: Long): String {
    //     val s = remainingSeconds.coerceAtLeast(0)
    //     val h = s / 3600
    //     val m = (s % 3600) / 60
    //     val sec = s % 60
    //     return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
    // }

    private fun startForegroundCompat() {
        val notification = buildNotification(emptyList(), engine.remainingSeconds.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(
        arrivals: List<TransitArrival>,
        remainingSeconds: Long
    ): android.app.Notification {
        createChannel()

        // 탭하면 타임어택 화면으로 복귀 (MainActivity 가 EXTRA_FROM_ALARM 으로 라우팅).
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_FROM_ALARM, true)
        }
        val contentIntent = PendingIntent.getActivity(
            this, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val busLines = nextBusLines(arrivals)
        // 접힌 알림: 첫 줄만. 펼친 알림(BigText): 버스 2대를 두 줄로.
        val collapsed = busLines.firstOrNull() ?: "도착 정보를 불러오는 중…"
        val expanded = if (busLines.isEmpty()) "도착 정보를 불러오는 중…" else busLines.joinToString("\n")
        // 크로노미터 카운트다운의 기준 시각 = 지금 + 남은초. 목표가 지났으면 과거가 되어 0 에서 멈춘다.
        val targetAt = System.currentTimeMillis() + remainingSeconds.coerceAtLeast(0) * 1000L

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("출근 미션 진행 중 🚍")
            .setContentText(collapsed)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expanded))
            .setSubText("목표까지")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)

        // 목표 시각 전이면 남은시간을 실시간 카운트다운으로 표시(시스템이 1초마다 갱신).
        if (remainingSeconds > 0) {
            builder.setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(targetAt)
                .setShowWhen(true)
        } else {
            builder.setShowWhen(false)
        }

        return builder.build()
    }

    /** 도착정보 → ["🚌 651 3분후", "🚌 7011 5분후"] (최대 2건). 없으면 빈 리스트. */
    private fun nextBusLines(arrivals: List<TransitArrival>): List<String> {
        return arrivals.filter { it.arrivalSeconds >= 0 }.take(2).map { a ->
            val time = a.arrivalMessage.substringBefore("[").trim().ifBlank { "곧 도착" }
            "🚌 ${a.routeName} $time"
        }
    }

    // 1.0 릴리즈: 플로팅 위젯 비활성화 — 위젯 표시 전용 헬퍼 (P2 재활성화 시 해제)
    // /** 버스 라인들을 줄바꿈으로 합친 표시용 텍스트. 없으면 안내 문구. */
    // private fun nextBusText(arrivals: List<TransitArrival>): String {
    //     val lines = nextBusLines(arrivals)
    //     return if (lines.isEmpty()) "도착 정보를 불러오는 중…" else lines.joinToString("\n")
    // }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "출근 미션 진행",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "미션이 진행되는 동안 표시되는 알림"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "mission_progress_channel"
        const val NOTIFICATION_ID = 1003
        const val ACTION_STOP = "com.yeon.todaymorning.MISSION_SERVICE_STOP"

        fun start(context: Context) {
            val intent = Intent(context, MissionService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, MissionService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }
}
