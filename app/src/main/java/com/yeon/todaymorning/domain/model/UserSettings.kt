package com.yeon.todaymorning.domain.model

enum class TransitType {
    BUS, SUBWAY, BOTH
}

data class UserSettings(
    val alarmHour: Int = 7,
    val alarmMinute: Int = 0,
    val targetHour: Int = 9,
    val targetMinute: Int = 0,
    val transitType: TransitType = TransitType.BUS,
    val busStopId: String = "",
    val busRouteId: String = "",
    val subwayStationId: String = "",
    val subwayLineId: String = ""
)
