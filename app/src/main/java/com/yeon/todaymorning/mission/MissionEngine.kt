package com.yeon.todaymorning.mission

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import com.yeon.todaymorning.alarm.TtsManager
import com.yeon.todaymorning.data.datastore.UserSettingsDataStore
import com.yeon.todaymorning.data.db.MissionRecord
import com.yeon.todaymorning.data.repository.MissionRepository
import com.yeon.todaymorning.data.repository.TransitRepository
import com.yeon.todaymorning.domain.model.MissionState
import com.yeon.todaymorning.domain.model.MissionTransitType
import com.yeon.todaymorning.domain.model.TransitArrival
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 타임어택 미션의 '두뇌' — 도착 폴링·목표 카운트다운·TTS 음성안내·성공/실패 기록을
 * 화면 생명주기와 **독립적으로** 굴린다.
 *
 * 왜 분리했나:
 *  - 과거엔 이 로직이 전부 TimeAttackViewModel 의 viewModelScope 에 있었다. 화면이 꺼지면
 *    (Activity STOP/Doze) 루프가 멈춰 음성안내가 끊기고 도착 리스트도 비워졌다.
 *  - 이제 이 엔진은 @Singleton 이고 [com.yeon.todaymorning.alarm.MissionService](포그라운드
 *    서비스)가 호스팅한다. 서비스가 살아 있는 한 화면과 무관하게 계속 돈다.
 *
 * 화면(ViewModel)은 이 엔진의 StateFlow 를 구독만 하고, 사용자 액션을 forward 한다.
 *
 * 생명주기: [start] 로 시작, 미션 종료(성공/실패/목표시각 경과) 시 [finished] 가 true 가 된다.
 * 서비스는 [finished] 를 보고 [stop] 을 호출해 정리 후 자신을 종료한다.
 */
@Singleton
class MissionEngine @Inject constructor(
    private val transitRepository: TransitRepository,
    private val dataStore: UserSettingsDataStore,
    private val missionRepository: MissionRepository,
    private val ttsProvider: Provider<TtsManager>,
    @ApplicationContext private val appContext: Context
) {
    private val scope = CoroutineScope(SupervisorJob())

    // 차편별 직전 폴링 도착초. key = "노선명|방면|동일그룹순번". 하향 통과 판정용.
    private val lastSecondsByKey = mutableMapOf<String, Int>()
    // 이미 발화한 (차편 key, 안내 시점) 조합. "key@minute". 같은 차편의 같은 시점 중복 방지.
    private val spokenMarks = mutableSetOf<String>()

    @Volatile private var started = false
    private var countdownJob: Job? = null
    private var pollingJob: Job? = null
    private var tts: TtsManager? = null

    private val _arrivals = MutableStateFlow<List<TransitArrival>>(emptyList())
    val arrivals: StateFlow<List<TransitArrival>> = _arrivals.asStateFlow()

    private val _missionState = MutableStateFlow<MissionState>(MissionState.Active)
    val missionState: StateFlow<MissionState> = _missionState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private val _refreshCountdown = MutableStateFlow(10)
    val refreshCountdown: StateFlow<Int> = _refreshCountdown.asStateFlow()

    // 미션이 끝나 더 이상 추적할 필요가 없음(서비스가 자신을 내릴 신호).
    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    /** 미션 추적 시작. 이미 돌고 있으면 무시(중복 진입 방지). */
    @Synchronized
    fun start() {
        if (started) return
        started = true

        // 새 미션 — 상태 초기화.
        lastSecondsByKey.clear()
        spokenMarks.clear()
        _arrivals.value = emptyList()
        _missionState.value = MissionState.Active
        _errorMessage.value = null
        _finished.value = false
        tts = ttsProvider.get()

        startCountdown()
        startPolling()
    }

    private fun startCountdown() {
        countdownJob = scope.launch {
            val s = dataStore.userSettings.first()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, s.targetHour)
                set(Calendar.MINUTE, s.targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            while (true) {
                val now = System.currentTimeMillis()
                val remaining = (target - now) / 1000
                _remainingSeconds.value = remaining

                if (_missionState.value != MissionState.Active) break
                // 목표 시각 경과 — 미션 창이 닫힘. 자동실패 DB 기록은 MissionFailReceiver 가
                // 담당하므로 여기선 추적만 종료한다(중복 기록 방지).
                if (remaining <= 0) {
                    markFinished()
                    break
                }
                delay(1000L)
            }
        }
    }

    private fun startPolling() {
        pollingJob = scope.launch {
            dataStore.userSettings.first()
            fetchArrivals()
            while (_missionState.value == MissionState.Active && !_finished.value) {
                for (i in 10 downTo 1) {
                    _refreshCountdown.value = i
                    delay(1_000L)
                    if (_missionState.value != MissionState.Active || _finished.value) return@launch
                }
                fetchArrivals()
            }
        }
    }

    fun fetchArrivals() {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val s = dataStore.userSettings.first()
                val results = mutableListOf<TransitArrival>()

                if (!s.hasMissionTarget) {
                    _errorMessage.value = "설정에서 출근 경로를 먼저 탐색해 주세요."
                    return@launch
                }

                when (s.missionTransitType) {
                    MissionTransitType.BUS ->
                        s.missionRoutes.forEach { route ->
                            results += transitRepository.getBusArrivals(s.missionStopId, route.routeId)
                        }
                    MissionTransitType.SUBWAY ->
                        s.missionRoutes.forEach { route ->
                            results += transitRepository.getSubwayArrivals(s.missionStopId, route.routeId)
                        }
                    MissionTransitType.NONE -> {
                        _errorMessage.value = "설정에서 출근 경로를 먼저 탐색해 주세요."
                        return@launch
                    }
                }

                _arrivals.value = results.sortedBy { it.arrivalSeconds }
                maybeAnnounce(s)

                if (results.isEmpty()) {
                    _errorMessage.value = "도착 정보가 없습니다. (운행 종료 또는 API 키 미설정)"
                }
            } catch (e: Exception) {
                _errorMessage.value = "네트워크 오류: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 리스트의 '모든 차편' 각각에 대해, 도착초가 설정된 안내 시점(분)을 아래로 통과하는 순간 발화.
     * 단, **미션 음성안내 시작 시점(ttsLeadMinutes) 이전엔 발화하지 않는다** — 목표까지 남은
     * 시간이 ttsLeadMinutes*60 이하가 돼야 음성안내가 '열린다'.
     */
    private fun maybeAnnounce(settings: com.yeon.todaymorning.domain.model.UserSettings) {
        val remaining = _remainingSeconds.value
        val gateOpen = remaining <= settings.ttsLeadMinutes * 60L
        val valid = _arrivals.value.filter { it.arrivalSeconds >= 0 }

        // 디버그: 차편 수·최단 도착초 + TTS설정/엔진상태 + 게이트(15분전) 상태 표시(추후 제거 — TODO).
        showToast(
            "⏱ ${valid.size}대·최단 ${valid.firstOrNull()?.arrivalSeconds ?: "없음"}s · TTS ${if (settings.ttsEnabled) "ON" else "OFF"} · 엔진 ${tts?.initStatus ?: "없음"} · 안내 ${if (gateOpen) "열림" else "닫힘(${remaining / 60}분남음)"}",
            short = true
        )

        // 음성안내 시작 시점 게이트: 목표 lead 분 전이 되기 전엔 침묵.
        if (!gateOpen) return

        val groupCount = mutableMapOf<String, Int>()
        val ttsOn = settings.ttsEnabled && settings.ttsTimings.isNotEmpty()
        var vibrated = false

        for (a in valid) {
            val base = "${a.routeName}|${a.destination}"
            val ord = groupCount.getOrDefault(base, 0)
            groupCount[base] = ord + 1
            val key = "$base|$ord"

            val prev = lastSecondsByKey[key]
            lastSecondsByKey[key] = a.arrivalSeconds

            if (!ttsOn) continue
            if (prev == null) continue

            for (minute in settings.ttsTimings) {
                val mark = "$key@$minute"
                if (mark in spokenMarks) continue
                val threshold = minute * 60
                if (prev > threshold && a.arrivalSeconds <= threshold) {
                    spokenMarks += mark
                    showToast("🔊 ${minute}분 전 안내: ${TtsManager.sentenceFor(a)}")
                    if (!vibrated) { vibrate(); vibrated = true }
                    tts?.announce(a)
                }
            }
        }
    }

    private fun showToast(text: String, short: Boolean = false) {
        val len = if (short) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(appContext, text, len).show()
        }
    }

    private fun vibrate() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val effect = VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1)
            vibrator.vibrate(effect)
        }
    }

    fun onBoardingSuccess() {
        if (_missionState.value != MissionState.Active) return
        scope.launch {
            _missionState.value = MissionState.Success
            val s = dataStore.userSettings.first()
            val now = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            missionRepository.insertTodayResult(
                MissionRecord(
                    date = dateStr,
                    alarmTime = "%02d:%02d".format(s.alarmHour, s.alarmMinute),
                    targetTime = "%02d:%02d".format(s.targetHour, s.targetMinute),
                    boardedTime = timeStr,
                    isSuccess = true
                )
            )
            markFinished()
        }
    }

    fun onMissionFail() {
        if (_missionState.value == MissionState.Success) return
        scope.launch {
            _missionState.value = MissionState.Failed
            val s = dataStore.userSettings.first()
            val now = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
            missionRepository.insertTodayResult(
                MissionRecord(
                    date = dateStr,
                    alarmTime = "%02d:%02d".format(s.alarmHour, s.alarmMinute),
                    targetTime = "%02d:%02d".format(s.targetHour, s.targetMinute),
                    boardedTime = null,
                    isSuccess = false
                )
            )
            markFinished()
        }
    }

    /** 미션 추적 종료 신호. 서비스가 [finished] 를 보고 [stop] 을 호출한다. */
    private fun markFinished() {
        _finished.value = true
    }

    /**
     * 자원 정리. 서비스가 [finished] 관측 후, 또는 서비스 onDestroy 에서 호출한다.
     * 표시용 StateFlow 값은 유지해 화면이 결과 연출을 마칠 수 있게 한다(다음 [start] 에서 초기화).
     */
    @Synchronized
    fun stop() {
        if (!started) return
        started = false
        countdownJob?.cancel(); countdownJob = null
        pollingJob?.cancel(); pollingJob = null
        runCatching { tts?.shutdown() }
        tts = null
    }
}
