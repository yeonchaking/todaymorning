package com.yeon.todaymorning.data.api.dto

import com.google.gson.annotations.SerializedName

// ── 좌표 → 주소 (coord2address) ─────────────────────────────────
data class Coord2AddressResponse(
    val documents: List<Coord2AddressDocument>
)
data class Coord2AddressDocument(
    val address: KakaoAddress?,
    @SerializedName("road_address") val roadAddress: KakaoRoadAddress?
)
data class KakaoAddress(
    @SerializedName("address_name") val addressName: String
)
data class KakaoRoadAddress(
    @SerializedName("address_name") val addressName: String
)

// ── 주소 검색 (search/address) ───────────────────────────────────
data class AddressSearchResponse(
    val documents: List<AddressDocument>
)
data class AddressDocument(
    @SerializedName("address_name") val addressName: String,
    val x: String,   // 경도 (lng)
    val y: String    // 위도 (lat)
)
