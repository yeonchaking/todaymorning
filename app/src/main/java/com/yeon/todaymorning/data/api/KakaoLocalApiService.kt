package com.yeon.todaymorning.data.api

import com.yeon.todaymorning.data.api.dto.AddressSearchResponse
import com.yeon.todaymorning.data.api.dto.Coord2AddressResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface KakaoLocalApiService {

    /** 좌표 → 주소 (역지오코딩) */
    @GET("v2/local/geo/coord2address.json")
    suspend fun coord2Address(
        @Header("Authorization") auth: String,
        @Query("x") lng: Double,
        @Query("y") lat: Double
    ): Coord2AddressResponse

    /** 주소 검색 */
    @GET("v2/local/search/address.json")
    suspend fun searchAddress(
        @Header("Authorization") auth: String,
        @Query("query") query: String,
        @Query("size") size: Int = 10
    ): AddressSearchResponse
}
