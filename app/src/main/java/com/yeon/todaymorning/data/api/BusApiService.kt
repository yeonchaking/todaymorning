package com.yeon.todaymorning.data.api

import com.yeon.todaymorning.data.api.dto.BusArrivalResponse
import com.yeon.todaymorning.data.api.dto.StationByNameResponse
import com.yeon.todaymorning.data.api.dto.StationByPosResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface BusApiService {
    /**
     * 정류소 고유번호(arsId)로 해당 정류장의 모든 버스 도착 예정 정보 조회.
     * 노선 선택 화면(경유 노선 목록 + 도착시간 미리보기)에도 재사용한다.
     * http://ws.bus.go.kr/api/rest/stationinfo/getStationByUid
     */
    @GET("stationinfo/getStationByUid")
    suspend fun getArrivalByStationId(
        @Query("arsId") arsId: String,
        @Query("serviceKey") serviceKey: String,
        @Query("resultType") resultType: String = "json"
    ): BusArrivalResponse

    /**
     * 좌표 기반 근접 정류소 목록 조회 — 지도 화면의 핀 표시 / 내 주변.
     * http://ws.bus.go.kr/api/rest/stationinfo/getStationByPos
     */
    @GET("stationinfo/getStationByPos")
    suspend fun getStationByPos(
        @Query("tmX") tmX: String,       // 경도(WGS84)
        @Query("tmY") tmY: String,       // 위도(WGS84)
        @Query("radius") radius: String, // 반경(m)
        @Query("serviceKey") serviceKey: String,
        @Query("resultType") resultType: String = "json"
    ): StationByPosResponse

    /**
     * 명칭으로 정류소 검색 — 지도 화면 상단 검색바.
     * http://ws.bus.go.kr/api/rest/stationinfo/getStationByName
     */
    @GET("stationinfo/getStationByName")
    suspend fun getStationByName(
        @Query("stSrch") keyword: String,
        @Query("serviceKey") serviceKey: String,
        @Query("resultType") resultType: String = "json"
    ): StationByNameResponse
}
