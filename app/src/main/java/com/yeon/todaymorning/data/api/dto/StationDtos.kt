package com.yeon.todaymorning.data.api.dto

import com.google.gson.annotations.SerializedName

// ── 공통 헤더 ──────────────────────────────────────────────
data class StationMsgHeader(
    @SerializedName("headerCd") val headerCd: String? = null,
    @SerializedName("headerMsg") val headerMsg: String? = null,
    @SerializedName("itemCount") val itemCount: String? = null
)

// ── 좌표기반 근접 정류소 목록조회 (getStationByPos) ─────────
data class StationByPosResponse(
    @SerializedName("msgHeader") val msgHeader: StationMsgHeader? = null,
    @SerializedName("msgBody") val msgBody: StationByPosBody? = null
)

data class StationByPosBody(
    @SerializedName("itemList") val itemList: List<StationByPosItem>? = null
)

data class StationByPosItem(
    @SerializedName("stationId") val stationId: String = "",
    @SerializedName("stationNm") val stationNm: String = "",
    @SerializedName("gpsX") val gpsX: String = "",   // 경도(WGS84)
    @SerializedName("gpsY") val gpsY: String = "",   // 위도(WGS84)
    @SerializedName("arsId") val arsId: String = "",
    @SerializedName("dist") val dist: String = ""    // 거리(m)
)

// ── 명칭별 정류소 목록조회 (getStationByName) ──────────────
data class StationByNameResponse(
    @SerializedName("msgHeader") val msgHeader: StationMsgHeader? = null,
    @SerializedName("msgBody") val msgBody: StationByNameBody? = null
)

data class StationByNameBody(
    @SerializedName("itemList") val itemList: List<StationByNameItem>? = null
)

data class StationByNameItem(
    @SerializedName("stId") val stId: String = "",
    @SerializedName("stNm") val stNm: String = "",
    @SerializedName("tmX") val tmX: String = "",     // 경도(WGS84)
    @SerializedName("tmY") val tmY: String = "",     // 위도(WGS84)
    @SerializedName("arsId") val arsId: String = ""
)
