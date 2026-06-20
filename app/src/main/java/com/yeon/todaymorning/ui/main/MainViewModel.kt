package com.yeon.todaymorning.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeon.todaymorning.alarm.AlarmScheduler
import com.yeon.todaymorning.data.datastore.UserSettingsDataStore
import com.yeon.todaymorning.data.db.MissionRecord
import com.yeon.todaymorning.data.repository.MissionRepository
import com.yeon.todaymorning.data.repository.TransitRepository
import com.yeon.todaymorning.domain.model.MissionTransitType
import com.yeon.todaymorning.domain.model.TransitArrival
import com.yeon.todaymorning.domain.model.UserSettings
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
    private val transitRepository: TransitRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings> = dataStore.userSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
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
                when (s.missionTransitType) {
                    MissionTransitType.BUS ->
                        s.missionRoutes.forEach { route ->
                            results += transitRepository.getBusArrivals(s.missionStopId, route.routeId)
                        }
                    MissionTransitType.SUBWAY ->
                        s.missionRoutes.forEach { route ->
                            results += transitRepository.getSubwayArrivals(s.missionStopId, route.routeId)
                        }
                    MissionTransitType.NONE -> Unit
                }

                val sorted = results.sortedBy { it.arrivalSeconds }
                _arrivalDialog.value = ArrivalDialogState(
                    isOpen = true,
                    arrivals = sorted,
                    errorMessage = if (sorted.isEmpty())
                        "도착 정보가 없습니다." else null
                )
            } catch (e: Exception) {
                _arrivalDialog.value = ArrivalDialogState(
                    isOpen = true,
                    errorMessage = "네트워크 오류: ${e.message}"
                )
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
