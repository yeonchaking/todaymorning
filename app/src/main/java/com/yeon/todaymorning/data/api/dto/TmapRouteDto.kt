package com.yeon.todaymorning.data.api.dto

import com.google.gson.annotations.SerializedName

// ── 요청 ────────────────────────────────────────────────────────
data class TmapRouteRequest(
    val startX: String,
    val startY: String,
    val endX: String,
    val endY: String,
    val count: Int = 5,
    val searchDttm: String,    // yyyyMMddHHmm
    val lang: Int = 0          // 0: 한국어
)

// ── 응답 ────────────────────────────────────────────────────────
data class TmapRouteResponse(
    val metaData: TmapMetaData?
)

data class TmapMetaData(
    val plan: TmapPlan?
)

data class TmapPlan(
    val itineraries: List<TmapItinerary> = emptyList()
)

data class TmapItinerary(
    val totalTime: Int = 0,
    val totalDistance: Int = 0,
    val totalWalkTime: Int = 0,
    val transferCount: Int = 0,
    val fare: TmapFare?,
    val legs: List<TmapLeg> = emptyList()
)

data class TmapFare(
    val regular: TmapFareDetail?
)

data class TmapFareDetail(
    val totalFare: Int = 0
)

data class TmapLeg(
    val mode: String = "",        // "WALK" | "BUS" | "SUBWAY" | "EXPRESSBUS"
    val sectionTime: Int = 0,
    val route: String = "",       // 버스번호("간선:402") or 호선명("수도권2호선")
    @SerializedName("routeId")
    val routeId: String = "",
    val type: Int = 0,            // 노선 종류 코드(버스: 11=간선 등 / 지하철: 호선)
    val routeColor: String = "",  // 노선 색상 hex
    val start: TmapStop?,
    val end: TmapStop?,
    val passStopList: TmapPassStopList?
)

data class TmapStop(
    val name: String = "",
    val lon: String = "",         // 경도(WGS84). 응답에선 숫자지만 Gson이 String으로 흡수
    val lat: String = ""          // 위도(WGS84)
)

data class TmapPassStopList(
    // ⚠️ 실제 응답 필드명은 "stations" (이전 "stationList" 가정은 오류 → 항상 빈 리스트였음)
    @SerializedName("stations")
    val stations: List<TmapStation> = emptyList()
)

data class TmapStation(
    val index: Int = 0,
    val stationName: String = "",
    val stationID: String = "",   // ⚠️ T-map 내부 정류장 ID. 서울버스 arsId 아님(좌표로 별도 변환 필요)
    val lon: String = "",
    val lat: String = ""
)
