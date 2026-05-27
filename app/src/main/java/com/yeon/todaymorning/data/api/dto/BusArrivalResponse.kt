package com.yeon.todaymorning.data.api.dto

import com.google.gson.annotations.SerializedName

data class BusArrivalResponse(
    @SerializedName("msgBody") val msgBody: BusMsgBody?,
    @SerializedName("msgHeader") val msgHeader: BusMsgHeader?
)

data class BusMsgHeader(
    @SerializedName("headerCd") val headerCd: String?,
    @SerializedName("headerMsg") val headerMsg: String?,
    @SerializedName("itemCount") val itemCount: String?
)

data class BusMsgBody(
    @SerializedName("itemList") val itemList: List<BusArrivalItem>?
)

data class BusArrivalItem(
    @SerializedName("arsId") val arsId: String = "",
    @SerializedName("busRouteId") val busRouteId: String = "",
    @SerializedName("rtNm") val rtNm: String = "",           // 노선 번호 (예: "273")
    @SerializedName("adirection") val adirection: String = "", // 방향 (종점)
    @SerializedName("arrmsg1") val arrmsg1: String = "",     // 첫 번째 버스 도착 메시지
    @SerializedName("arrmsg2") val arrmsg2: String = "",     // 두 번째 버스 도착 메시지
    @SerializedName("isArrive1") val isArrive1: String = "0",
    @SerializedName("isArrive2") val isArrive2: String = "0"
)
