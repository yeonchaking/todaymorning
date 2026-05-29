package com.yeon.todaymorning.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yeon.todaymorning.domain.model.TransitType
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
        val TRANSIT_TYPE = stringPreferencesKey("transit_type")
        val BUS_STOP_ID = stringPreferencesKey("bus_stop_id")
        val BUS_STOP_NAME = stringPreferencesKey("bus_stop_name")
        val BUS_ROUTE_ID = stringPreferencesKey("bus_route_id")
        val BUS_ROUTE_NAME = stringPreferencesKey("bus_route_name")
        val BUS_DIRECTION = stringPreferencesKey("bus_direction")
        val SUBWAY_STATION_ID = stringPreferencesKey("subway_station_id")
        val SUBWAY_LINE_ID = stringPreferencesKey("subway_line_id")
    }

    val userSettings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            alarmHour = prefs[ALARM_HOUR] ?: 7,
            alarmMinute = prefs[ALARM_MINUTE] ?: 0,
            targetHour = prefs[TARGET_HOUR] ?: 9,
            targetMinute = prefs[TARGET_MINUTE] ?: 0,
            transitType = TransitType.valueOf(prefs[TRANSIT_TYPE] ?: TransitType.BUS.name),
            busStopId = prefs[BUS_STOP_ID] ?: "",
            busStopName = prefs[BUS_STOP_NAME] ?: "",
            busRouteId = prefs[BUS_ROUTE_ID] ?: "",
            busRouteName = prefs[BUS_ROUTE_NAME] ?: "",
            busDirection = prefs[BUS_DIRECTION] ?: "",
            subwayStationId = prefs[SUBWAY_STATION_ID] ?: "",
            subwayLineId = prefs[SUBWAY_LINE_ID] ?: ""
        )
    }

    suspend fun saveSettings(settings: UserSettings) {
        context.dataStore.edit { prefs ->
            prefs[ALARM_HOUR] = settings.alarmHour
            prefs[ALARM_MINUTE] = settings.alarmMinute
            prefs[TARGET_HOUR] = settings.targetHour
            prefs[TARGET_MINUTE] = settings.targetMinute
            prefs[TRANSIT_TYPE] = settings.transitType.name
            prefs[BUS_STOP_ID] = settings.busStopId
            prefs[BUS_STOP_NAME] = settings.busStopName
            prefs[BUS_ROUTE_ID] = settings.busRouteId
            prefs[BUS_ROUTE_NAME] = settings.busRouteName
            prefs[BUS_DIRECTION] = settings.busDirection
            prefs[SUBWAY_STATION_ID] = settings.subwayStationId
            prefs[SUBWAY_LINE_ID] = settings.subwayLineId
        }
    }
}
