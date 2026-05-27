package com.yeon.todaymorning.data.api

import com.yeon.todaymorning.data.api.dto.BusArrivalResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface BusApiService {
    /**
     * 정류소 고유번호(arsId)로 해당 정류장의 모든 버스 도착 예정 정보 조회
     * http://ws.bus.go.kr/api/rest/stationinfo/getStationByUid?arsId={arsId}&ServiceKey={key}&resultType=json
     */
    @GET("stationinfo/getStationByUid")
    suspend fun getArrivalByStationId(
        @Query("arsId") arsId: String,
        @Query("ServiceKey") serviceKey: String,
        @Query("resultType") resultType: String = "json"
    ): BusArrivalResponse
}
