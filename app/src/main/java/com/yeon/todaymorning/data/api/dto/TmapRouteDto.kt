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
    val route: String = "",       // 버스번호 or 호선명
    @SerializedName("routeId")
    val routeId: String = "",
    val start: TmapStop?,
    val end: TmapStop?,
    val passStopList: TmapPassStopList?
)

data class TmapStop(
    val name: String = "",
    val lon: String = "",
    val lat: String = ""
)

data class TmapPassStopList(
    val stationList: List<TmapStation> = emptyList()
)

data class TmapStation(
    val index: Int = 0,
    val stationName: String = "",
    val stationID: String = "",   // 버스 arsId
    val lon: String = "",
    val lat: String = ""
)
