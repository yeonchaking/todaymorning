package com.yeon.todaymorning.data.api

import com.yeon.todaymorning.data.api.dto.SubwayArrivalResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface SubwayApiService {
    /**
     * 역 이름으로 실시간 지하철 도착 정보 조회
     * http://swopenapi.seoul.go.kr/api/subway/{key}/json/realtimeStationArrival/0/10/{stationName}
     */
    @GET("{key}/json/realtimeStationArrival/0/10/{stationName}")
    suspend fun getArrivalByStation(
        @Path("key") key: String,
        @Path("stationName") stationName: String
    ): SubwayArrivalResponse
}
