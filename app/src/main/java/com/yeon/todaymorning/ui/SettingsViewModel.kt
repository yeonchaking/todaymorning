package com.yeon.todaymorning.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeon.todaymorning.alarm.AlarmScheduler
import com.yeon.todaymorning.data.datastore.UserSettingsDataStore
import com.yeon.todaymorning.domain.model.MissionRoute
import com.yeon.todaymorning.domain.model.MissionTransitType
import com.yeon.todaymorning.domain.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
            // 다음 발생 시각 계산·등록은 AlarmScheduler가 담당.
            // 실제 매일 반복은 AlarmReceiver/MissionFailReceiver가 발생 시점에 재등록.
            alarmScheduler.scheduleDailyAlarm(settings.alarmHour, settings.alarmMinute)
            alarmScheduler.scheduleDailyMissionFail(settings.targetHour, settings.targetMinute)
        }
    }

    /**
     * 미션 타겟만 부분 저장. 시각 키를 건드리지 않고 알람도 재등록하지 않으므로,
     * 설정 화면에서 저장 전 편집 중인 시각이 보존된다.
     */
    fun saveMissionTarget(
        transitType: MissionTransitType,
        stopId: String,
        stopName: String,
        routes: List<MissionRoute>
    ) {
        viewModelScope.launch {
            dataStore.saveMissionTarget(transitType, stopId, stopName, routes)
        }
    }

    /** 집 위치만 부분 저장 (시각·알람 미변경). */
    fun saveHomeLocation(lat: Double, lng: Double, address: String) {
        viewModelScope.launch {
            dataStore.saveHomeLocation(lat, lng, address)
        }
    }

    /** 회사 위치만 부분 저장 (시각·알람 미변경). */
    fun saveWorkLocation(lat: Double, lng: Double, address: String) {
        viewModelScope.launch {
            dataStore.saveWorkLocation(lat, lng, address)
        }
    }
}
