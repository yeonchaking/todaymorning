package com.yeon.todaymorning.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeon.todaymorning.data.repository.MissionRepository
import com.yeon.todaymorning.domain.model.UserLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultUiState(
    val streak: Int = 0,
    val level: UserLevel = UserLevel.ROOKIE,
    val daysToNext: Int? = null
)

@HiltViewModel
class MissionResultViewModel @Inject constructor(
    private val repository: MissionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    init {
        loadStreak()
    }

    private fun loadStreak() {
        viewModelScope.launch {
            val streak = repository.getCurrentStreak()
            _uiState.value = ResultUiState(
                streak = streak,
                level = UserLevel.fromStreak(streak),
                daysToNext = UserLevel.daysToNextLevel(str