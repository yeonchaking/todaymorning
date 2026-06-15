package com.yeon.todaymorning.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
