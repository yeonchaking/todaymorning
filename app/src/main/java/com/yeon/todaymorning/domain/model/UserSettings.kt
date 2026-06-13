package com.yeon.todaymorning.domain.model

/** 미션 대상 대중교통 유형 */
enum class MissionTransitType { NONE, BUS, SUBWAY }

/** 미션 대상 노선 하나 (한 정류장/역에서 여러 개 선택 가능) */
data class MissionRoute(
    val routeId: String,      // 버스: busRouteId / 지하철: 호선 ID
    val routeName: String,    // 표시용: "651" / "2호선"
    val direction: String     // 방면
)

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

    // ── 미션 타겟 (지도에서 첫 정류장/역 + 다수 노선 직접 선택) ──────
    val missionTransitType: MissionTransitType = MissionTransitType.NONE,
    val missionStopId: String = "",            // 버스: arsId / 지하철: 역 이름
    val missionStopName: String = "",          // 표시용: "강남역"
    val missionRoutes: List<MissionRoute> = emptyList()  // 선택한 노선들 (아무거나 타면 성공)
) {
    val hasHomeLocation: Boolean get() = homeLat != 0.0 && homeLng != 0.0
    val hasWorkLocation: Boolean get() = workLat != 0.0 && workLng != 0.0
    val hasMissionTarget: Boolean
        get() = missionTransitType != MissionTransitType.NONE &&
                missionStopId.isNotBlank() &&
                missionRoutes.isNotEmpty()

    /** 요약 표시용: "651, 388" */
    val missionRoutesLabel: String get() = missionRoutes.joinToString(", ") { it.routeName }
}
