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
    val successRate: Float = 0f,
    val recentRecords: List<MissionRecord> = emptyList(),
    val totalCount: Int = 0,
    val successCount: Int = 0
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
        viewModelScope.launch {
            repository.getSuccessRate().collect { rate ->
                _uiState.value = _uiState.value.copy(successRate = rate)
            }
        }
        viewModelScope.launch {
            repository.getSuccessCount().collect { count ->
                _uiState.value = _uiState.value.copy(successCount = count)
            }
        }
        viewModelScope.launch {
            repository.getTotalCount().collect { count ->
                _uiState.value = _uiState.value.copy(totalCount = count)
            }
        }
    }

    fun refreshStreak() {
        viewModelScope.launch {
            val streak = repository.getCurrentStreak()
            _uiState.value = _uiState.value.copy(streak = streak)
        }
    }
}
