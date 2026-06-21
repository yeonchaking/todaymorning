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
            // 활성화·반복 요일을 반영해 등록/취소. 실제 반복은 Receiver가 발생 시점에 재등록.
            alarmScheduler.applyAlarm(settings)
        }
    }

    /** 마스터 스위치 토글: 부분 저장 후 즉시 등록/취소 반영. */
    fun setAlarmEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.saveAlarmEnabled(enabled)
            alarmScheduler.applyAlarm(settings.value.copy(alarmEnabled = enabled))
        }
    }

    /** 반복 요일 저장 후 즉시 등록/취소 반영. */
    fun setRepeatDays(days: Set<Int>) {
        viewModelScope.launch {
            dataStore.saveRepeatDays(days)
            alarmScheduler.applyAlarm(settings.value.copy(repeatDays = days))
        }
    }

    /** 알람음 선택(기본/내장/최근값)만 부분 저장. 알람 재등록 불필요(서비스가 울릴 때 읽음). */
    fun setAlarmSound(soundId: String) {
        viewModelScope.launch {
            dataStore.saveAlarmSound(soundId)
        }
    }

    /** 진동 패턴 선택만 부분 저장. 알람 재등록 불필요(서비스가 울릴 때 읽음). */
    fun setVibrationPattern(patternId: String) {
        viewModelScope.launch {
            dataStore.saveVibrationPattern(patternId)
        }
    }

    /** 휴대폰에서 고른 알람음 → 선택 + "최근 선택한 알람"으로 동시 저장. */
    fun setPickedRingtone(uri: String) {
        viewModelScope.launch {
            dataStore.savePickedRingtone(uri)
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
