package com.yeon.todaymorning.data.repository

import android.util.Log
import com.yeon.todaymorning.BuildConfig
import com.yeon.todaymorning.data.api.BusApiService
import com.yeon.todaymorning.data.api.SubwayApiService
import com.yeon.todaymorning.domain.model.BusRouteOption
import com.yeon.todaymorning.domain.model.BusStop
import com.yeon.todaymorning.domain.model.TransitArrival
import com.yeon.todaymorning.domain.model.TransitType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransitRepository @Inject constructor(
    private val busApiService: BusApiService,
    private val subwayApiService: SubwayApiService
) {

    suspend fun getBusArrivals(arsId: String, busRouteId: String): List<TransitArrival> {
        val apiKey = BuildConfig.BUS_API_KEY
        if (apiKey.isBlank() || arsId.isBlank()) return emptyList()

        return try {
            val response = busApiService.getArrivalByStationId(arsId, apiKey)
            val items = response.msgBody?.itemList ?: emptyList()

            items
                .filter { busRouteId.isBlank() || it.busRouteId == busRouteId }
                .flatMap { item ->
                    listOfNotNull(
                        item.arrmsg1.toTransitArrival(item.rtNm, item.adirection),
                        item.arrmsg2.toTransitArrival(item.rtNm, item.adirection)
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 좌표 주변 정류장 — 지도 핀/내 주변. radius 단위 m. */
    suspend fun nearbyBusStops(lat: Double, lng: Double, radius: Int = 500): List<BusStop> {
        val apiKey = BuildConfig.BUS_API_KEY
        if (apiKey.isBlank()) return emptyList()
        return try {
            val response = busApiService.getStationByPos(
                tmX = lng.toString(), tmY = lat.toString(), radius = radius.toString(), serviceKey = apiKey
            )
            response.msgBody?.itemList.orEmpty()
                .filter { it.arsId.isNotBlank() && it.arsId != "0" }
                .mapNotNull { item ->
                    val y = item.gpsY.toDoubleOrNull() ?: return@mapNotNull null
                    val x = item.gpsX.toDoubleOrNull() ?: return@mapNotNull null
                    BusStop(
                        arsId = item.arsId,
                        name = item.stationNm,
                        lat = y,
                        lng = x,
                        distance = item.dist.toDoubleOrNull()?.toInt()
                    )
                }
                .distinctBy { it.arsId }
                .sortedBy { it.distance ?: Int.MAX_VALUE }
                .also { Log.d("TransitRepo", "nearbyBusStops 결과: ${it.size}개 (lat=$lat, lng=$lng)") }
        } catch (e: Exception) {
            Log.e("TransitRepo", "nearbyBusStops 실패 (lat=$lat,lng=$lng): ${e.message}", e)
            emptyList()
        }
    }

    /** 이름으로 정류장 검색 — 지도 상단 검색바. */
    suspend fun searchBusStops(keyword: String): List<BusStop> {
        val apiKey = BuildConfig.BUS_API_KEY
        if (apiKey.isBlank() || keyword.isBlank()) return emptyList()
        return try {
            val response = busApiService.getStationByName(keyword = keyword, serviceKey = apiKey)
            response.msgBody?.itemList.orEmpty()
                .filter { it.arsId.isNotBlank() && it.arsId != "0" }
                .mapNotNull { item ->
                    val y = item.tmY.toDoubleOrNull() ?: return@mapNotNull null
                    val x = item.tmX.toDoubleOrNull() ?: return@mapNotNull null
                    BusStop(arsId = item.arsId, name = item.stNm, lat = y, lng = x)
                }
                .distinctBy { it.arsId }
        } catch (e: Exception) {
            Log.e("TransitRepo", "searchBusStops 실패 (q=$keyword): ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 정류장 경유 노선 목록 + 도착시간 미리보기.
     * getStationByUid(검증된 엔드포인트)를 재사용해 노선(rtNm/busRouteId/방면)과
     * 첫 도착 메시지를 함께 돌려준다.
     */
    suspend fun getRoutesAtStop(arsId: String): List<BusRouteOption> {
        val apiKey = BuildConfig.BUS_API_KEY
        if (apiKey.isBlank() || arsId.isBlank()) return emptyList()
        return try {
            val response = busApiService.getArrivalByStationId(arsId, apiKey)
            response.msgBody?.itemList.orEmpty()
                .filter { it.busRouteId.isNotBlank() && it.rtNm.isNotBlank() }
                .distinctBy { it.busRouteId }
                .map { item ->
                    BusRouteOption(
                        busRouteId = item.busRouteId,
                        routeName = item.rtNm,
                        direction = item.adirection,
                        arrivalMessage = item.arrmsg1.takeIf { it.isNotBlank() }
                    )
                }
                .sortedBy { it.routeName }
        } catch (e: Exception) {
            Log.e("TransitRepo", "getRoutesAtStop 실패 (arsId=$arsId): ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getSubwayArrivals(stationName: String, lineId: String): List<SubwayArrival> {
        val apiKey = BuildConfig.SUBWAY_API_KEY
        if (apiKey.isBlank() || stationName.isBlank()) return emptyList()

        return try {
            val response = subwayApiService.getArrivalByStation(apiKey, stationName)
            val items = response.realtimeArrivalList ?: emptyList()

            items
                .filter { lineId.isBlank() || it.subwayId == lineId }
                .take(4)
                .map { item ->
                    TransitArrival(
                        type = TransitType.SUBWAY,
                        routeName = item.subwayId.toLineName(),
                        destination = item.trainLineNm,
                        arrivalSeconds = item.barvlDt.toIntOrNull() ?: -1,
                        arrivalMessage = item.arvlMsg2.ifBlank { item.arvlMsg3 }
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // "3분후[4번째 전]", "곧 도착", "운행종료" 등 파싱
    private fun String.toTransitArrival(routeName: String, destination: String): TransitArrival? {
        if (isBlank() || contains("운행종료") || contains("출발대기")) return null
        return TransitArrival(
            type = TransitType.BUS,
            routeName = routeName,
            destination = destination,
            arrivalSeconds = parseArrivalSeconds(this),
            arrivalMessage = this
        )
    }

    private fun parseArrivalSeconds(msg: String): Int = when {
        msg.contains("곧 도착") || msg.startsWith("도착") -> 0
        msg.contains("분") -> {
            val minutes = msg.substringBefore("분").trim().toIntOrNull() ?: return -1
            minutes * 60
        }
        else -> -1
    }

    private fun String.toLineName(): String = when (this) {
        "1001" -> "1호선"; "1002" -> "2호선"; "1003" -> "3호선"; "1004" -> "4호선"
        "1005" -> "5호선"; "1006" -> "6호선"; "1007" -> "7호선"; "1008" -> "8호선"
        "1009" -> "9호선"; "1061" -> "중앙선"; "1063" -> "경의중앙선"
        "1065" -> "공항철도"; "1067" -> "경춘선"; "1075" -> "수인분당선"; "1077" -> "신분당선"
        else -> this
    }
}

// SubwayArrival은 타입 충돌 방지를 위한 별칭 — 실제로는 TransitArrival 반환
private typealias SubwayArrival = TransitArrival
