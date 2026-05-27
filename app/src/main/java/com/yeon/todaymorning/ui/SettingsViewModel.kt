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
        }
    }

    private fun rescheduleAlarm(hour: Int, minute: Int) {
        val triggerAt = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // 이미 지난 시각이면 내일로
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis
        alarmScheduler.scheduleAt(triggerAt)
    }
}
