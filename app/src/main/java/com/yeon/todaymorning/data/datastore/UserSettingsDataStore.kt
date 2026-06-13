package com.yeon.todaymorning.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yeon.todaymorning.domain.model.MissionTransitType
import com.yeon.todaymorning.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserSettingsDataStore(private val context: Context) {

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
        val MISSION_ROUTE_ID = stringPreferencesKey("mission_route_id")
        val MISSION_ROUTE_NAME = stringPreferencesKey("mission_route_name")
        val MISSION_STOP_NAME = stringPreferencesKey("mission_stop_name")
        val MISSION_DIRECTION = stringPreferencesKey("mission_direction")
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
            missionRouteId = prefs[MISSION_ROUTE_ID] ?: "",
            missionRouteName = prefs[MISSION_ROUTE_NAME] ?: "",
            missionStopName = prefs[MISSION_STOP_NAME] ?: "",
            missionDirection = prefs[MISSION_DIRECTION] ?: ""
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
            prefs[MISSION_ROUTE_ID] = settings.missionRouteId
            prefs[MISSION_ROUTE_NAME] = settings.missionRouteName
            prefs[MISSION_STOP_NAME] = settings.missionStopName
            prefs[MISSION_DIRECTION] = settings.missionDirection
        }
    }
}
