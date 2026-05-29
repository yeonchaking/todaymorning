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
    val busStopId: String = "",        // arsId (정류소 고유번호)
    val busStopName: String = "",      // 표시용: "강남역"
    val busRouteId: String = "",
    val busRouteName: String = "",     // 표시용: "273"
    val busDirection: String = "",     // 방면(종점)
    val subwayStationId: String = "",
    val subwayLineId: String = ""
) {
    /** 출근 버스가 등록되어 있는지 */
    val hasBus: Boolean get() = busStopId.isNotBlank() && busRouteId.isNotBlank()
}
