package com.yeon.todaymorning.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeon.todaymorning.data.datastore.UserSettingsDataStore
import com.yeon.todaymorning.data.db.MissionRecord
import com.yeon.todaymorning.data.repository.MissionRepository
import com.yeon.todaymorning.domain.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val streak: Int = 0,
    val recentRecords: List<MissionRecord> = emptyList(),
    val allRecords: List<MissionRecord> = emptyList(),
    val isEditMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
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
        // 달력용: 과거 전체 기록
        viewModelScope.launch {
            repository.getAllRecords().collect { records ->
                _uiState.value = _uiState.value.copy(allRecords = records)
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
