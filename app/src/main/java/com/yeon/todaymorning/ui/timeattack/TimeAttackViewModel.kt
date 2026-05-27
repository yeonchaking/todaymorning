package com.yeon.todaymorning.ui.timeattack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeon.todaymorning.data.datastore.UserSettingsDataStore
import com.yeon.todaymorning.data.db.MissionRecord
import com.yeon.todaymorning.data.repository.MissionRepository
import com.yeon.todaymorning.data.repository.TransitRepository
import com.yeon.todaymorning.domain.model.MissionState
import com.yeon.todaymorning.domain.model.TransitArrival
import com.yeon.todaymorning.domain.model.TransitType
import com.yeon.todaymorning.domain.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val missionRepository: MissionRepository
) : ViewModel() {

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

    init {
        startCountdown()
        startPolling()
    }

    private fun startCountdown() {
        viewModelScope.launch {
            while (true) {
                val s = settings.value
                val now = System.currentTimeMillis()
                val target = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, s.targetHour)
                    set(Calendar.MINUTE, s.targetMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val remaining = (target - now) / 1000
                _remainingSeconds.value = remaining

                // 목표 시각 초과 → 자동 실패
                if (remaining <= 0 && _missionState.value == MissionState.Active) {
                    onMissionFail()
                    break
                }
                delay(1000L)
            }
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                fetchArrivals()
                delay(30_000L) // 30초마다 갱신
            }
        }
    }

    fun fetchArrivals() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val s = settings.value
                val results = mutableListOf<TransitArrival>()

                if (s.busStopId.isBlank() && s.subwayStationId.isBlank()) {
                    _errorMessage.value = "설정에서 정류장/역 정보를 먼저 입력해주세요."
                    return@launch
                }

                if (s.transitType == TransitType.BUS || s.transitType == TransitType.BOTH) {
                    results += transitRepository.getBusArrivals(s.busStopId, s.busRouteId)
                }
                if (s.transitType == TransitType.SUBWAY || s.transitType == TransitType.BOTH) {
                    results += transitRepository.getSubwayArrivals(s.subwayStationId, s.subwayLineId)
                }

                _arrivals.value = results.sortedBy { it.arrivalSeconds }

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
