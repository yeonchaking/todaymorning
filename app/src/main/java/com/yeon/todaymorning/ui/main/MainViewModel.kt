package com.yeon.todaymorning.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeon.todaymorning.data.datastore.UserSettingsDataStore
import com.yeon.todaymorning.data.db.MissionRecord
import com.yeon.todaymorning.data.repository.MissionRepository
import com.yeon.todaymorning.domain.model.BadgeUi
import com.yeon.todaymorning.domain.model.DayStatus
import com.yeon.todaymorning.domain.model.LevelInfo
import com.yeon.todaymorning.domain.model.LevelSystem
import com.yeon.todaymorning.domain.model.POINTS_PER_SUCCESS
import com.yeon.todaymorning.domain.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class MainUiState(
    val streak: Int = 0,
    val recentRecords: List<MissionRecord> = emptyList(),
    val allRecords: List<MissionRecord> = emptyList(),
    val isEditMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),

    // ── 게이미피케이션(파생) ──────────────────────────────
    val level: LevelInfo = LevelInfo(level = 1, points = 0, progress = 0f, pointsToNext = 400),
    val weekly: List<DayStatus> = List(7) { DayStatus.NONE }, // 월~일
    val weekSuccess: Int = 0,
    val weekTotal: Int = 0,
    val badges: List<BadgeUi> = emptyList(),
    val todayLabel: String = "",   // "목요일"
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MissionRepository,
    private val dataStore: UserSettingsDataStore
) : ViewModel() {

    val settings: StateFlow<UserSettings> = dataStore.userSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings()
    )

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        observeStats()
        refreshStreak()
    }

    private fun observeStats() {
        viewModelScope.launch {
            repository.getRecentRecords(7).collect { records ->
                _uiState.value = _uiState.value.copy(recentRecords = records)
            }
        }
        // 달력·게이미피케이션용: 과거 전체 기록
        viewModelScope.launch {
            repository.getAllRecords().collect { records ->
                _uiState.value = _uiState.value.copy(allRecords = records)
                recomputeDerived()
            }
        }
    }

    fun refreshStreak() {
        viewModelScope.launch {
            val streak = repository.getCurrentStreak()
            _uiState.value = _uiState.value.copy(streak = streak)
            recomputeDerived()
        }
    }

    /** 기록·streak에서 포인트·레벨·주간현황·배지를 다시 계산한다. */
    private fun recomputeDerived() {
        val s = _uiState.value
        val records = s.allRecords
        val successCount = records.count { it.isSuccess }
        val points = successCount * POINTS_PER_SUCCESS
        val level = LevelSystem.fromPoints(points)

        val today = LocalDate.now()
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        // 날짜 → 성공여부 (같은 날 성공이 하나라도 있으면 성공 우선)
        val resultByDate = HashMap<LocalDate, Boolean>()
        for (r in records) {
            val d = runCatching { LocalDate.parse(r.date) }.getOrNull() ?: continue
            resultByDate[d] = (resultByDate[d] ?: false) || r.isSuccess
        }
        val weekly = (0..6).map { offset ->
            val d = monday.plusDays(offset.toLong())
            when {
                d.isAfter(today) -> DayStatus.FUTURE
                d.isEqual(today) -> DayStatus.TODAY
                resultByDate.containsKey(d) ->
                    if (resultByDate[d] == true) DayStatus.SUCCESS else DayStatus.FAIL
                else -> DayStatus.NONE
            }
        }
        val weekSuccess = weekly.count { it == DayStatus.SUCCESS }
        val weekTotal = weekly.count { it == DayStatus.SUCCESS || it == DayStatus.FAIL }
        val perfectWeek = weekSuccess >= 5

        val badges = listOf(
            BadgeUi("early_bird", "얼리버드", "🌅", successCount >= 1),
            BadgeUi("streak7", "7일 연속", "🔥", s.streak >= 7),
            BadgeUi("perfect_week", "완벽한 주", "🏅", perfectWeek),
            BadgeUi("streak30", "30일", "👑", s.streak >= 30),
        )

        val todayLabel = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)

        _uiState.value = _uiState.value.copy(
            level = level,
            weekly = weekly,
            weekSuccess = weekSuccess,
            weekTotal = weekTotal,
            badges = badges,
            todayLabel = todayLabel,
        )
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
