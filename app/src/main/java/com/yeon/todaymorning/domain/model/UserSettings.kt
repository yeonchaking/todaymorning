package com.yeon.todaymorning.domain.model

/** 미션 대상 대중교통 유형 (경로 탐색 결과로 자동 설정됨) */
enum class MissionTransitType { NONE, BUS, SUBWAY }

data class UserSettings(
    val alarmHour: Int = 7,
    val alarmMinute: Int = 0,
    val targetHour: Int = 9,
    val targetMinute: Int = 0,

    // ── 집 위치 ───────────────────────────────────────
    val homeLat: Double = 0.0,
    val homeLng: Double = 0.0,
    val homeAddress: String = "",

    // ── 회사 위치 ─────────────────────────────────────
    val workLat: Double = 0.0,
    val workLng: Double = 0.0,
    val workAddress: String = "",

    // ── 미션 타겟 (T-map 경로 탐색 후 자동 설정) ──────
    val missionTransitType: MissionTransitType = MissionTransitType.NONE,
    val missionStopId: String = "",       // 버스: arsId / 지하철: 역 이름
    val missionRouteId: String = "",      // 버스: routeId / 지하철: 호선 ID
    val missionRouteName: String = "",    // 표시용: "273" or "2호선"
    val missionStopName: String = "",     // 표시용: "강남역"
    val missionDirection: String = ""     // 방면
) {
    val hasHomeLocation: Boolean get() = homeLat != 0.0 && homeLng != 0.0
    val hasWorkLocation: Boolean get() = workLat != 0.0 && workLng != 0.0
    val hasMissionTarget: Boolean
        get() = missionTransitType != MissionTransitType.NONE && missionStopId.isNotBlank()
}
