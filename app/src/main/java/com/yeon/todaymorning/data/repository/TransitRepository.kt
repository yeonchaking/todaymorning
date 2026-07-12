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
import kotlinx.coroutines.CancellationException

/**
 * 도착 조회 결과. 도착 예정 차편 리스트에 더해, `toTransitArrival()`이 걸러내던
 * "운행종료"/"출발대기" 메시지의 **개수**를 함께 올린다 — 호출부가 "전부 운행종료라 0건"과
 * "그냥 아직 안 옴(정상 0건)"을 구분해 다른 안내를 띄울 수 있게 하기 위함(2026-07-12).
 */
data class ArrivalResult(
    val arrivals: List<TransitArrival>,
    /** "운행종료" 메시지로 걸러진 차편 수 — 막차 이후/첫차 전 신호. */
    val endedCount: Int = 0,
    /** "출발대기" 메시지로 걸러진 차편 수 — 차량이 기점 출발 전 신호. */
    val waitingCount: Int = 0
)

@Singleton
class TransitRepository @Inject constructor(
    private val busApiService: BusApiService,
    private val subwayApiService: SubwayApiService
) {

    /**
     * 정류장+노선 도착 조회. 실패 시 [TransitException]을 던진다(더 이상 emptyList 로 삼키지 않음).
     * 0건(정상)과 오류를 호출부가 구분할 수 있어야 하기 때문 — 상세는 ApiErrorMapper.kt 주석.
     */
    suspend fun getBusArrivals(arsId: String, busRouteId: String): ArrivalResult {
        val apiKey = BuildConfig.BUS_API_KEY
        if (apiKey.isBlank()) throw TransitException("버스 API 키가 설정되지 않았어요. (개발 설정 확인)")
        if (arsId.isBlank()) return ArrivalResult(emptyList())

        return try {
            val response = busApiService.getArrivalByStationId(arsId, apiKey)
            val items = response.msgBody?.itemList ?: emptyList()

            val arrivals = mutableListOf<TransitArrival>()
            var ended = 0
            var waiting = 0
            items
                .filter { busRouteId.isBlank() || it.busRouteId == busRouteId }
                .forEach { item ->
                    for (msg in listOf(item.arrmsg1, item.arrmsg2)) {
                        when {
                            msg.isBlank() -> Unit
                            msg.contains("운행종료") -> ended++
                            msg.contains("출발대기") -> waiting++
                            else -> arrivals += TransitArrival(
                                type = TransitType.BUS,
                                routeName = item.rtNm,
                                destination = item.adirection,
                                arrivalSeconds = parseArrivalSeconds(msg),
                                arrivalMessage = msg
                            )
                        }
                    }
                }
            ArrivalResult(arrivals, endedCount = ended, waitingCount = waiting)
        } catch (e: CancellationException) {
            throw e
        } catch (e: TransitException) {
            throw e
        } catch (e: Exception) {
            Log.e("TransitRepo", "getBusArrivals 실패 (arsId=$arsId): ${e.message}", e)
            throw TransitException(e.toUserMessage(), e)
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

    /** 지하철 도착 조회. 실패 시 [TransitException] — 버스와 동일한 정책(위 getBusArrivals 참고). */
    suspend fun getSubwayArrivals(stationName: String, lineId: String): ArrivalResult {
        val apiKey = BuildConfig.SUBWAY_API_KEY
        val queryName = normalizeStationName(stationName)
        if (apiKey.isBlank()) throw TransitException("지하철 API 키가 설정되지 않았어요. (개발 설정 확인)")
        if (queryName.isBlank()) return ArrivalResult(emptyList())

        return try {
            val response = subwayApiService.getArrivalByStation(apiKey, queryName)
            val items = response.realtimeArrivalList ?: emptyList()

            var ended = 0
            val arrivals = items
                .filter { lineId.isBlank() || it.subwayId == lineId }
                .take(4)
                .mapNotNull { item ->
                    val msg = item.arvlMsg2.ifBlank { item.arvlMsg3 }
                    if (msg.contains("운행종료")) {
                        ended++
                        return@mapNotNull null
                    }
                    TransitArrival(
                        type = TransitType.SUBWAY,
                        routeName = item.subwayId.toLineName(),
                        destination = item.trainLineNm,
                        arrivalSeconds = item.barvlDt.toIntOrNull() ?: -1,
                        arrivalMessage = msg
                    )
                }
            ArrivalResult(arrivals, endedCount = ended)
        } catch (e: CancellationException) {
            throw e
        } catch (e: TransitException) {
            throw e
        } catch (e: Exception) {
            Log.e("TransitRepo", "getSubwayArrivals 실패 (station=$queryName): ${e.message}", e)
            throw TransitException(e.toUserMessage(), e)
        }
    }

    // "3분후[4번째 전]", "곧 도착" 등 도착 메시지 → 초 단위 파싱
    // (구 toTransitArrival 헬퍼는 "운행종료"/"출발대기"를 조용히 버렸음 — 이제 getBusArrivals
    //  본문에서 개수를 세어 ArrivalResult 로 올린다. 2026-07-12)
    private fun parseArrivalSeconds(msg: String): Int = when {
        msg.contains("곧 도착") || msg.startsWith("도착") -> 0
        msg.contains("분") -> {
            val minutes = msg.substringBefore("분").trim().toIntOrNull() ?: return -1
            minutes * 60
        }
        else -> -1
    }

    /**
     * 서울 지하철 실시간 도착 API는 역명을 "역" 접미사·괄호 표기 없이 받는다.
     * T-map이 주는 start.name이 "강남역", "시청(2호선)", "총신대입구(이수)역" 등으로
     * 올 수 있으므로 조회용 역명으로 정규화한다.
     *  - 괄호 표기 제거: "시청(2호선)" → "시청"
     *  - 끝의 "역" 제거: "강남역" → "강남"  (단, 한 글자만 남는 경우는 보존)
     *  - 공백 정리
     */
    private fun normalizeStationName(raw: String): String {
        var name = raw.trim()
        if (name.isEmpty()) return name
        // 괄호(소·중) 안 부가정보 제거
        name = name.replace(Regex("[(\\[（].*?[)\\]）]"), "").trim()
        // 끝의 "역" 한 글자 제거 (역명이 비어버리는 것 방지)
        if (name.length > 1 && name.endsWith("역")) {
            name = name.dropLast(1)
        }
        return name.trim()
    }

    private fun String.toLineName(): String = when (this) {
        "1001" -> "1호선"; "1002" -> "2호선"; "1003" -> "3호선"; "1004" -> "4호선"
        "1005" -> "5호선"; "1006" -> "6호선"; "1007" -> "7호선"; "1008" -> "8호선"
        "1009" -> "9호선"; "1061" -> "중앙선"; "1063" -> "경의중앙선"
        "1065" -> "공항철도"; "1067" -> "경춘선"; "1075" -> "수인분당선"; "1077" -> "신분당선"
        else -> this
    }
}
