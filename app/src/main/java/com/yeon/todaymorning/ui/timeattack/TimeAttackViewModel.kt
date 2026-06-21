package com.yeon.todaymorning.ui.timeattack

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeon.todaymorning.alarm.TtsManager
import com.yeon.todaymorning.data.datastore.UserSettingsDataStore
import com.yeon.todaymorning.data.db.MissionRecord
import com.yeon.todaymorning.data.repository.MissionRepository
import com.yeon.todaymorning.data.repository.TransitRepository
import com.yeon.todaymorning.domain.model.MissionState
import com.yeon.todaymorning.domain.model.MissionTransitType
import com.yeon.todaymorning.domain.model.TransitArrival
import com.yeon.todaymorning.domain.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TimeAttackViewModel @Inject constructor(
    private val transitRepository: TransitRepository,
    private val dataStore: UserSettingsDataStore,
    private val missionRepository: MissionRepository,
    private val ttsManager: TtsManager,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    // 차편별 직전 폴링 도착초. key = "노선명|방면|동일그룹순번". 하향 통과 판정용.
    private val lastSecondsByKey = mutableMapOf<String, Int>()

    // 이미 발화한 (차편 key, 안내 시점) 조합. "key@minute". 같은 차편의 같은 시점 중복 방지.
    private val spokenMarks = mutableSetOf<String>()

    val settings: StateFlow<UserSettings> = dataStore.userSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings()
    )

    private val _arrivals = MutableStateFlow<List<TransitArrival>>(emptyList())
    val arrivals: StateFlow<List<TransitArrival>> = _arrivals.asStateFlow()

    private val _missionState = MutableStateFlow<MissionState>(MissionState.Active)
    val missionState: StateFlow<MissionState> = _missionState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // 목표 시각까지 남은 초
    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    // 자동 새로고침까지 남은 초 (UI 표시용)
    private val _refreshCountdown = MutableStateFlow(10)
    val refreshCountdown: StateFlow<Int> = _refreshCountdown.asStateFlow()

    init {
        startCountdown()
        startPolling()
    }

    private fun startCountdown() {
        viewModelScope.launch {
            // StateFlow initialValue(기본값)가 아닌 DataStore의 실제 저장값 로드까지 대기
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

                // 미션 종료(성공 or 실패 확정)되면 타이머 중단
                if (_missionState.value != MissionState.Active) break
                delay(1000L)
            }
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            // DataStore 실제 설정값 로드 대기 후 첫 조회
            // (init 시점엔 settings.value가 아직 기본값이라 빈 ID로 조회됨)
            dataStore.userSettings.first()
            fetchArrivals()
            while (true) {
                // 10초 카운트다운 표시 후 갱신
                for (i in 10 downTo 1) {
                    _refreshCountdown.value = i
                    delay(1_000L)
                }
                fetchArrivals()
            }
        }
    }

    fun fetchArrivals() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // settings StateFlow는 진입 직후 아직 기본값(hasMissionTarget=false)일 수 있어
                // '설정 안됨' 오류가 잠깐 떴다 사라진다. DataStore 실제 저장값을 직접 읽어 방지.
                val s = dataStore.userSettings.first()
                val results = mutableListOf<TransitArrival>()

                if (!s.hasMissionTarget) {
                    _errorMessage.value = "설정에서 출근 경로를 먼저 탐색해 주세요."
                    return@launch
                }

                when (s.missionTransitType) {
                    MissionTransitType.BUS ->
                        // 선택한 노선 전부 조회해 합산 (아무거나 타면 성공)
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

                // 도착정보가 갱신될 때마다 음성 안내 시점 통과 여부 확인 (버스 도착 기준).
                maybeAnnounce()

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
     * - 버스 도착 기준. fetchArrivals(10초 폴링)가 도착정보를 갱신할 때마다 호출된다.
     * - 차편 식별: "노선명|방면|동일그룹순번"(1·2번째 도착을 구분). lastSecondsByKey로 직전값 추적.
     * - 하향 통과(직전 도착초 > 임계 >= 현재 도착초)일 때만 → 진입 시점에 이미 임박한 건 안 울림.
     *   도착초가 위로 점프하면(이전 버스 출발로 슬롯 이동) 발화 안 함.
     * - spokenMarks("key@minute")로 같은 차편의 같은 시점 중복 방지.
     */
    private fun maybeAnnounce() {
        val s = settings.value
        // 유효한 차편만(도착초 ≥ 0). -1(정보없음/운행종료)은 제외. 리스트는 이미 오름차순.
        val valid = _arrivals.value.filter { it.arrivalSeconds >= 0 }

        // 디버그: 폴링마다 차편 수·최단 도착초 표시(폴링 동작·값 확인용. 추후 제거).
        showToast(
            "⏱ ${valid.size}대 · 최단 ${valid.firstOrNull()?.arrivalSeconds ?: "없음"}s · TTS ${if (s.ttsEnabled) "ON" else "OFF"}",
            short = true
        )

        // 같은 (노선|방면) 그룹 내 순번 부여 — 1·2번째 도착을 서로 다른 차편으로 추적.
        val groupCount = mutableMapOf<String, Int>()
        val ttsOn = s.ttsEnabled && s.ttsTimings.isNotEmpty()
        var vibrated = false

        for (a in valid) {
            val base = "${a.routeName}|${a.destination}"
            val ord = groupCount.getOrDefault(base, 0)
            groupCount[base] = ord + 1
            val key = "$base|$ord"

            val prev = lastSecondsByKey[key]
            lastSecondsByKey[key] = a.arrivalSeconds  // 추적값은 항상 갱신

            if (!ttsOn) continue
            if (prev == null) continue  // 이 차편 첫 관측 → 기준만 잡고 다음 폴링부터 비교

            for (minute in s.ttsTimings) {
                val mark = "$key@$minute"
                if (mark in spokenMarks) continue
                val threshold = minute * 60
                if (prev > threshold && a.arrivalSeconds <= threshold) {
                    spokenMarks += mark
                    // 디버그: 트리거 확인용 토스트(소리 없어도 이게 뜨면 트리거는 OK).
                    showToast("🔊 ${minute}분 전 안내: ${TtsManager.sentenceFor(a)}")
                    if (!vibrated) { vibrate(); vibrated = true }  // 한 폴링에 여러 건이어도 진동은 1회
                    ttsManager.announce(a)  // 여러 건이면 QUEUE_ADD로 순차 재생
                }
            }
        }
    }

    /** 디버그용 토스트. 어느 스레드에서 호출되든 메인 스레드에서 표시. */
    private fun showToast(text: String, short: Boolean = false) {
        val len = if (short) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(appContext, text, len).show()
        }
    }

    /** 안내 시점 알림용 짧은 진동(두 번 짧게). 진동 불가 기기에선 조용히 무시. */
    private fun vibrate() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            // 0대기 → 200 진동 → 100 멈춤 → 200 진동, 반복 없음(-1).
            val effect = VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1)
            vibrator.vibrate(effect)
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }

    fun onBoardingSuccess() {
        if (_missionState.value != MissionState.Active) return
        viewModelScope.launch {
            _missionState.value = MissionState.Success
            val s = settings.value
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
        }
    }

    fun onMissionFail() {
        if (_missionState.value == MissionState.Success) return
        viewModelScope.launch {
            _missionState.value = MissionState.Failed
            val s = settings.value
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
        }
    }
}
