package com.yeon.todaymorning.data.api.dto

import com.google.gson.annotations.SerializedName

data class SubwayArrivalResponse(
    @SerializedName("realtimeArrivalList") val realtimeArrivalList: List<SubwayArrivalItem>?,
    @SerializedName("errorMessage") val errorMessage: SubwayErrorMessage?
)

data class SubwayErrorMessage(
    @SerializedName("status") val status: Int?,
    @SerializedName("code") val code: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("link") val link: String?,
    @SerializedName("developerMessage") val developerMessage: String?
)

data class SubwayArrivalItem(
    @SerializedName("barvlDt") val barvlDt: String = "0",       // 도착 예정 시간 (초)
    @SerializedName("arvlMsg2") val arvlMsg2: String = "",       // 도착 메시지 (예: "2분 40초 후")
    @SerializedName("arvlMsg3") val arvlMsg3: String = "",       // 현재 위치 (예: "신도림")
    @SerializedName("trainLineNm") val trainLineNm: String = "", // 행선지 (예: "서울역 방면")
    @SerializedName("subwayId") val subwayId: String = "",       // 호선 ID (예: "1002" = 2호선)
    @SerializedName("updnLine") val updnLine: String = "",       // 상행/하행
    @SerializedName("subwayHeading") val subwayHeading: String = ""
)
