package com.yeon.todaymorning.data.api

import com.yeon.todaymorning.data.api.dto.TmapRouteRequest
import com.yeon.todaymorning.data.api.dto.TmapRouteResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface TmapApiService {
    @POST("transit/routes")
    suspend fun getTransitRoutes(
        @Header("appKey") appKey: String,
        @Body body: TmapRouteRequest
    ): TmapRouteResponse
}
