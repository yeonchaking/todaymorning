package com.yeon.todaymorning.ui.main

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeon.todaymorning.alarm.AlarmScheduler
import com.yeon.todaymorning.data.datastore.UserSettingsDataStore
import com.yeon.todaymorning.data.db.MissionRecord
import com.yeon.todaymorning.data.repository.MissionRepository
import com.yeon.todaymorning.data.repository.TransitException
import com.yeon.todaymorning.data.repository.TransitRepository
import com.yeon.todaymorning.data.repository.toUserMessage
import com.yeon.todaymorning.domain.model.MissionTransitType
import com.yeon.todaymorning.domain.model.TransitArrival
import com.yeon.todaymorning.domain.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class MainUiState(
    val streak: Int = 0,
    val recentRecords: List<MissionRecord> = emptyList(),
    val allRecords: List<MissionRecord> = emptyList(),
    val isEditMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    /** 이번 달 출근 성공 횟수 (정보성 통계). */
    val monthSuccessCount: Int = 0
)

/** 실시간 도착정보 다이얼로그 상태. */
data class ArrivalDialogState(
    val isOpen: Boolean = false,
    val isLoading: Boolean = false,
    val arrivals: List<TransitArrival> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MissionRepository,
    private val dataStore: UserSettingsDataStore,
    private val alarmScheduler: AlarmScheduler,
    private val transitRepository: TransitRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    // Lazily: 첫 구독 후 ViewModel 생존 동안 업스트림을 끊지 않는다 → .value 가 항상 DataStore
    // 최신값을 유지하므로, 설정 화면에서 시각을 바꾸고 돌아왔을 때 옛 캐시값이 잠깐 보이지 않는다.
    val settings: StateFlow<UserSettings> = dataStore.userSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = UserSettings()
    )

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // ── 실시간 도착정보 다이얼로그 ──────────────────────
    private val _arrivalDialog = MutableStateFlow(ArrivalDialogState())
    val arrivalDialog: StateFlow<ArrivalDialogState> = _arrivalDialog.asStateFlow()

    /** 다이얼로그 열고 도착정보 조회. (TimeAttackViewModel.fetchArrivals 동일 로직) */
    fun openArrivalDialog() {
        _arrivalDialog.value = ArrivalDialogState(isOpen = true, isLoading = true)
        viewModelScope.launch {
            try {
                val s = dataStore.userSettings.first()
                if (!s.hasMissionTarget) {
                    _arrivalDialog.value = ArrivalDialogState(
                        isOpen = true,
                        errorMessage = "설정된 미션이 없습니다."
                    )
                    return@launch
                }

                val results = mutableListOf<TransitArrival>()
                var endedCount = 0
                var waitingCount = 0
                when (s.missionTransitType) {
                    MissionTransitType.BUS ->
                        s.missionRoutes.forEach { route ->
                            val r = transitRepository.getBusArrivals(s.missionStopId, route.routeId)
                            results += r.arrivals
                            endedCount += r.endedCount
                            waitingCount += r.waitingCount
                        }
                    MissionTransitType.SUBWAY ->
                        s.missionRoutes.forEach { route ->
                            val r = transitRepository.getSubwayArrivals(s.missionStopId, route.routeId)
                            results += r.arrivals
                            endedCount += r.endedCount
                            waitingCount += r.waitingCount
                        }
                    MissionTransitType.NONE -> Unit
                }

                val sorted = results.sortedBy { it.arrivalSeconds }
                // 0건 원인별 안내 — MissionEngine.fetchArrivals 와 동일 기준(2026-07-12).
                _arrivalDialog.value = ArrivalDialogState(
                    isOpen = true,
                    arrivals = sorted,
                    errorMessage = when {
                        sorted.isNotEmpty() -> null
                        endedCount > 0 -> "지금은 운행 시간이 아니에요. (운행 종료 또는 첫차 전)"
                        waitingCount > 0 -> "차량이 아직 출발 전이에요. 잠시 후 다시 확인해 주세요."
                        else -> "지금 도착 예정인 차량이 없어요."
                    }
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: TransitException) {
                _arrivalDialog.value = ArrivalDialogState(isOpen = true, errorMessage = e.userMessage)
            } catch (e: Exception) {
                _arrivalDialog.value = ArrivalDialogState(isOpen = true, errorMessage = e.toUserMessage())
            }
        }
    }

    fun closeArrivalDialog() {
        _arrivalDialog.value = ArrivalDialogState(isOpen = false)
    }

    init {
        observeStats()
        refreshStreak()
    }

    /** 메인 마스터 스위치: 부분 저장 후 등록/취소를 즉시 반영. */
    fun setAlarmEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.saveAlarmEnabled(enabled)
            alarmScheduler.applyAlarm(settings.value.copy(alarmEnabled = enabled))
        }
    }

    /** 타이틀 10연속 탭 트리거로 호출 — 개발자모드 반전 저장 + 안내 토스트. */
    fun toggleDevMode() {
        viewModelScope.launch {
            val newValue = !settings.value.isDevMode
            dataStore.saveDevMode(newValue)
            val msg = if (newValue) "개발자모드가 설정되었습니다" else "개발자모드가 해제되었습니다"
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeStats() {
        viewModelScope.launch {
            repository.getRecentRecords(7).collect { records ->
                _uiState.value = _uiState.value.copy(recentRecords = records)
            }
        }
        // 달력용: 과거 전체 기록 + 이번 달 성공 횟수
        viewModelScope.launch {
            repository.getAllRecords().collect { records ->
                val ym = YearMonth.now().toString() // "2026-06"
                val monthSuccess = records.count { it.isSuccess && it.date.startsWith(ym) }
                _uiState.value = _uiState.value.copy(
                    allRecords = records,
                    monthSuccessCount = monthSuccess
                )
            }
        }
    }

    fun refreshStreak() {
        viewModelScope.launch {
            val streak = repository.getCurrentStreak()
            _uiState.value = _uiState.value.copy(streak = streak)
        }
    }

    /** 편집 모드 토글. 끌 때 선택 초기화. */
    fun toggleEditMode() {
        _uiState.value = _uiState.value.copy(
            isEditMode = !_uiState.value.isEditMode,
            selectedIds = emptySet()
        )
    }

    /** 기록 선택/해제. */
    fun toggleSelection(id: Long) {
        val current = _uiState.value.selectedIds
        _uiState.value = _uiState.value.copy(
            selectedIds = if (id in current) current - id else current + id
        )
    }

    /** 선택한 기록 삭제 후 편집 모드 종료 + streak 재계산. */
    fun deleteSelected() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteRecords(ids)
            _uiState.value = _uiState.value.copy(isEditMode = false, selectedIds = emptySet())
            refreshStreak()
        }
    }
}
