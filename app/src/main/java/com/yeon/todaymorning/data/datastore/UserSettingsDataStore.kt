package com.yeon.todaymorning.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yeon.todaymorning.domain.model.MissionRoute
import com.yeon.todaymorning.domain.model.MissionTransitType
import com.yeon.todaymorning.domain.model.UserSettings
import com.yeon.todaymorning.domain.model.WEEKDAYS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserSettingsDataStore(private val context: Context) {

    private val gson = Gson()
    private val missionRoutesType = object : TypeToken<List<MissionRoute>>() {}.type

    companion object {
        val ALARM_HOUR = intPreferencesKey("alarm_hour")
        val ALARM_MINUTE = intPreferencesKey("alarm_minute")
        val TARGET_HOUR = intPreferencesKey("target_hour")
        val TARGET_MINUTE = intPreferencesKey("target_minute")

        val ALARM_ENABLED = booleanPreferencesKey("alarm_enabled")
        val REPEAT_DAYS = intPreferencesKey("repeat_days")  // Calendar 요일 비트마스크
        val ALARM_SOUND_ID = stringPreferencesKey("alarm_sound_id")  // "" | "builtin:<key>" | content:// URI
        val LAST_PICKED_SOUND_ID = stringPreferencesKey("last_picked_sound_id")  // 마지막으로 고른 시스템 알람음 URI
        val VIBRATION_PATTERN_ID = stringPreferencesKey("vibration_pattern_id")  // VibrationPatterns id ("off" | "basic" | ...)
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")  // 음성 안내 전체 on/off
        val TTS_TIMINGS = stringPreferencesKey("tts_timings")   // 안내 시점 '분' CSV, 예: "10,5,3"
        val TTS_LEAD_MINUTES = intPreferencesKey("tts_lead_minutes")  // 음성안내 시작 시점(목표 N분 전), 기본 15
        val FLOATING_WIDGET_ENABLED = booleanPreferencesKey("floating_widget_enabled")  // 플로팅 위젯 on/off
        val FLOATING_WIDGET_OPACITY = intPreferencesKey("floating_widget_opacity")  // 위젯 불투명도 % (30~100)

        /** 요일 집합 ↔ 비트마스크 (bit n = Calendar 요일값 n, 일=1 … 토=7). */
        fun encodeDays(days: Set<Int>): Int = days.fold(0) { acc, d -> acc or (1 shl d) }
        fun decodeDays(mask: Int): Set<Int> = (1..7).filter { (mask shr it) and 1 == 1 }.toSet()

        val HOME_LAT = doublePreferencesKey("home_lat")
        val HOME_LNG = doublePreferencesKey("home_lng")
        val HOME_ADDRESS = stringPreferencesKey("home_address")

        val WORK_LAT = doublePreferencesKey("work_lat")
        val WORK_LNG = doublePreferencesKey("work_lng")
        val WORK_ADDRESS = stringPreferencesKey("work_address")

        val MISSION_TRANSIT_TYPE = stringPreferencesKey("mission_transit_type")
        val MISSION_STOP_ID = stringPreferencesKey("mission_stop_id")
        val MISSION_STOP_NAME = stringPreferencesKey("mission_stop_name")
        val MISSION_ROUTES = stringPreferencesKey("mission_routes")  // JSON List<MissionRoute>
    }

    val userSettings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            alarmHour = prefs[ALARM_HOUR] ?: 7,
            alarmMinute = prefs[ALARM_MINUTE] ?: 0,
            targetHour = prefs[TARGET_HOUR] ?: 9,
            targetMinute = prefs[TARGET_MINUTE] ?: 0,
            alarmEnabled = prefs[ALARM_ENABLED] ?: true,
            // 키가 아예 없으면 평일 기본값. 사용자가 '반복 없음'(mask 0)으로 저장한 경우는 그대로 빈 집합 유지.
            repeatDays = prefs[REPEAT_DAYS]?.let { decodeDays(it) } ?: WEEKDAYS,
            alarmSoundId = prefs[ALARM_SOUND_ID] ?: "",
            lastPickedSoundId = prefs[LAST_PICKED_SOUND_ID] ?: "",
            vibrationPatternId = prefs[VIBRATION_PATTERN_ID] ?: "basic",
            ttsEnabled = prefs[TTS_ENABLED] ?: true,
            // 키가 없으면 기본 {10,5,3}. 사용자가 모든 시점을 끈 경우는 빈 문자열 → 빈 집합 유지.
            ttsTimings = prefs[TTS_TIMINGS]?.let { csv ->
                csv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
            } ?: setOf(10, 5, 3),
            ttsLeadMinutes = prefs[TTS_LEAD_MINUTES] ?: 15,
            floatingWidgetEnabled = prefs[FLOATING_WIDGET_ENABLED] ?: true,
            floatingWidgetOpacity = (prefs[FLOATING_WIDGET_OPACITY] ?: 90).coerceIn(30, 100),
            homeLat = prefs[HOME_LAT] ?: 0.0,
            homeLng = prefs[HOME_LNG] ?: 0.0,
            homeAddress = prefs[HOME_ADDRESS] ?: "",
            workLat = prefs[WORK_LAT] ?: 0.0,
            workLng = prefs[WORK_LNG] ?: 0.0,
            workAddress = prefs[WORK_ADDRESS] ?: "",
            missionTransitType = MissionTransitType.valueOf(
                prefs[MISSION_TRANSIT_TYPE] ?: MissionTransitType.NONE.name
            ),
            missionStopId = prefs[MISSION_STOP_ID] ?: "",
            missionStopName = prefs[MISSION_STOP_NAME] ?: "",
            missionRoutes = prefs[MISSION_ROUTES]?.takeIf { it.isNotBlank() }?.let {
                runCatching { gson.fromJson<List<MissionRoute>>(it, missionRoutesType) }.getOrNull()
            } ?: emptyList()
        )
    }

    suspend fun saveSettings(settings: UserSettings) {
        context.dataStore.edit { prefs ->
            prefs[ALARM_HOUR] = settings.alarmHour
            prefs[ALARM_MINUTE] = settings.alarmMinute
            prefs[TARGET_HOUR] = settings.targetHour
            prefs[TARGET_MINUTE] = settings.targetMinute
            prefs[ALARM_ENABLED] = settings.alarmEnabled
            prefs[REPEAT_DAYS] = encodeDays(settings.repeatDays)
            prefs[ALARM_SOUND_ID] = settings.alarmSoundId
            prefs[LAST_PICKED_SOUND_ID] = settings.lastPickedSoundId
            prefs[VIBRATION_PATTERN_ID] = settings.vibrationPatternId
            prefs[TTS_ENABLED] = settings.ttsEnabled
            prefs[TTS_TIMINGS] = settings.ttsTimings.sortedDescending().joinToString(",")
            prefs[TTS_LEAD_MINUTES] = settings.ttsLeadMinutes
            prefs[FLOATING_WIDGET_ENABLED] = settings.floatingWidgetEnabled
            prefs[FLOATING_WIDGET_OPACITY] = settings.floatingWidgetOpacity
            prefs[HOME_LAT] = settings.homeLat
            prefs[HOME_LNG] = settings.homeLng
            prefs[HOME_ADDRESS] = settings.homeAddress
            prefs[WORK_LAT] = settings.workLat
            prefs[WORK_LNG] = settings.workLng
            prefs[WORK_ADDRESS] = settings.workAddress
            prefs[MISSION_TRANSIT_TYPE] = settings.missionTransitType.name
            prefs[MISSION_STOP_ID] = settings.missionStopId
            prefs[MISSION_STOP_NAME] = settings.missionStopName
            prefs[MISSION_ROUTES] = gson.toJson(settings.missionRoutes)
        }
    }

    /**
     * 미션 타겟(정류장/역 + 노선)만 부분 저장.
     * 알람·목표 시각 키는 건드리지 않으므로, 설정 화면에서 저장 버튼을 누르기 전
     * 편집 중인 시각이 이 저장으로 덮어써지지 않는다.
     */
    suspend fun saveMissionTarget(
        transitType: MissionTransitType,
        stopId: String,
        stopName: String,
        routes: List<MissionRoute>
    ) {
        context.dataStore.edit { prefs ->
            prefs[MISSION_TRANSIT_TYPE] = transitType.name
            prefs[MISSION_STOP_ID] = stopId
            prefs[MISSION_STOP_NAME] = stopName
            prefs[MISSION_ROUTES] = gson.toJson(routes)
        }
    }

    /** 마스터 스위치만 부분 저장 (시각·요일 키 미변경). */
    suspend fun saveAlarmEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ALARM_ENABLED] = enabled
        }
    }

    /** 반복 요일만 부분 저장 (시각·활성화 키 미변경). */
    suspend fun saveRepeatDays(days: Set<Int>) {
        context.dataStore.edit { prefs ->
            prefs[REPEAT_DAYS] = encodeDays(days)
        }
    }

    /** 알람음 선택만 부분 저장 (시각·알람 미변경). 알람 재등록 불필요(서비스가 울릴 때 읽음). */
    suspend fun saveAlarmSound(soundId: String) {
        context.dataStore.edit { prefs ->
            prefs[ALARM_SOUND_ID] = soundId
        }
    }

    /** 진동 패턴 선택만 부분 저장 (시각·알람 미변경). 알람 재등록 불필요(서비스가 울릴 때 읽음). */
    suspend fun saveVibrationPattern(patternId: String) {
        context.dataStore.edit { prefs ->
            prefs[VIBRATION_PATTERN_ID] = patternId
        }
    }

    /** 음성 안내(TTS) 설정만 부분 저장 (시각·알람 미변경). 타임어택 화면이 발화 시점에 읽음. */
    suspend fun saveTtsSettings(enabled: Boolean, timings: Set<Int>, leadMinutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[TTS_ENABLED] = enabled
            prefs[TTS_TIMINGS] = timings.sortedDescending().joinToString(",")
            prefs[TTS_LEAD_MINUTES] = leadMinutes
        }
    }

    /** 플로팅 위젯 on/off 만 부분 저장. */
    suspend fun saveFloatingWidget(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[FLOATING_WIDGET_ENABLED] = enabled
        }
    }

    /** 플로팅 위젯 불투명도(%)만 부분 저장. */
    suspend fun saveFloatingWidgetOpacity(opacity: Int) {
        context.dataStore.edit { prefs ->
            prefs[FLOATING_WIDGET_OPACITY] = opacity.coerceIn(30, 100)
        }
    }

    /** 휴대폰에서 고른 시스템 알람음을 선택 + 최근값으로 동시 저장. */
    suspend fun savePickedRingtone(uri: String) {
        context.dataStore.edit { prefs ->
            prefs[ALARM_SOUND_ID] = uri
            prefs[LAST_PICKED_SOUND_ID] = uri
        }
    }

    /** 집 위치만 부분 저장 (시각 키 미변경). */
    suspend fun saveHomeLocation(lat: Double, lng: Double, address: String) {
        context.dataStore.edit { prefs ->
            prefs[HOME_LAT] = lat
            prefs[HOME_LNG] = lng
            prefs[HOME_ADDRESS] = address
        }
    }

    /** 회사 위치만 부분 저장 (시각 키 미변경). */
    suspend fun saveWorkLocation(lat: Double, lng: Double, address: String) {
        context.dataStore.edit { prefs ->
            prefs[WORK_LAT] = lat
            prefs[WORK_LNG] = lng
            prefs[WORK_ADDRESS] = address
        }
    }
}
