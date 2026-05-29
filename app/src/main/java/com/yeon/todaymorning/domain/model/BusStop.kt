package com.yeon.todaymorning.domain.model

/** 지도/검색에서 다루는 버스 정류장. */
data class BusStop(
    val arsId: String,
    val name: String,
    val lat: Double,   // 위도(WGS84)
    val lng: Double,   // 경도(WGS84)
    val distance: Int? = null // 중심좌표로부터 거리(m), getStationByPos 결과에만 존재
)

/** 특정 정류장을 경유하는 노선(선택 후보). 도착 메시지가 있으면 미리보기로 노출. */
data class BusRouteOption(
    val busRouteId: String,
    val routeName: String,    // 예: "273"
    val direction: String,    // 방면(종점)
    val arrivalMessage: String? = null // 예: "3분 후[2번째 전]", 없으면 null
)
