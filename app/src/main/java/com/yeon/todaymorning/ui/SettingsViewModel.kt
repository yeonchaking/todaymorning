package com.yeon.todaymorning.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeon.todaymorning.alarm.AlarmScheduler
import com.yeon.todaymorning.data.datastore.UserSettingsDataStore
import com.yeon.todaymorning.domain.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: UserSettingsDataStore,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    val settings = dataStore.userSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings()
    )

    fun saveSettings(settings: UserSettings) {
        viewModelScope.launch {
            dataStore.saveSettings(settings)
            rescheduleAlarm(settings.alarmHour, settings.alarmMinute)
            rescheduleMissionFail(settings.targetHour, settings.targetMinute)
        }
    }

    private fun nextTriggerMillis(hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // 이미 지난 시각이면 내일로
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis

    private fun rescheduleAlarm(hour: Int, minute: Int) {
        alarmScheduler.scheduleAt(nextTriggerMillis(hour, minute))
    }

    /** 목표 시각에 자동 실패 처리 알람 등록 (탑승 완료를 누르지 않은 경우 대비). */
    private fun rescheduleMissionFail(hour: Int, minute: Int) {
        alarmScheduler.scheduleMissionFailAt(nextTriggerMillis(hour, minute))
    }
}
