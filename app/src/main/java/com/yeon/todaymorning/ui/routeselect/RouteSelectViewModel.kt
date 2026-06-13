package com.yeon.todaymorning.ui.routeselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeon.todaymorning.BuildConfig
import com.yeon.todaymorning.data.api.BusApiService
import com.yeon.todaymorning.data.api.TmapApiService
import com.yeon.todaymorning.data.api.dto.TmapItinerary
import com.yeon.todaymorning.data.api.dto.TmapRouteRequest
import com.yeon.todaymorning.data.api.dto.TmapStop
import com.yeon.todaymorning.data.datastore.UserSettingsDataStore
import com.yeon.todaymorning.domain.model.MissionTransitType
import com.yeon.todaymorning.domain.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class RouteOption(
    val index: Int,
    val totalTimeMin: Int,
    val transferCount: Int,
    val fare: Int,
    val firstTransitMode: String,   // "BUS" | "SUBWAY" | "WALK"
    val firstTransitRoute: String,  // "273번" | "2호선"
    val firstStopName: String,      // 탑승 정류장/역
    val direction: String,          // 방면
    // 미션 타겟 저장용
    val missionTransitType: MissionTransitType,
    val missionStopId: String,
    val missionRouteId: String,
    val missionRouteName: String,
    val missionStopName: String,
    val missionDirection: String
)

data class RouteSelectUiState(
    val isLoading: Boolean = false,
    val routes: List<RouteOption> = emptyList(),
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false
)

@HiltViewModel
class RouteSelectViewModel @Inject constructor(
    private val tmapApiService: TmapApiService,
    private val busApiService: BusApiService,
    private val dataStore: UserSettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteSelectUiState())
    val uiState: StateFlow<RouteSelectUiState> = _uiState.asStateFlow()

    init {
        fetchRoutes()
    }

    private fun fetchRoutes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val settings = dataStore.userSettings.first()

                if (!settings.hasHomeLocation || !settings.hasWorkLocation) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "집과 회사 위치를 먼저 설정해 주세요."
                    )
                    return@launch
                }

                val searchDttm = buildSearchDttm(settings)

                val response = tmapApiService.getTransitRoutes(
                    appKey = BuildConfig.TMAP_API_KEY,
                    body = TmapRouteRequest(
                        startX = settings.homeLng.toString(),
                        startY = settings.homeLat.toString(),
                        endX = settings.workLng.toString(),
                        endY = settings.workLat.toString(),
                        count = 5,
                        searchDttm = searchDttm
                    )
                )

                val itineraries = response.metaData?.plan?.itineraries ?: emptyList()
                if (itineraries.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "경로를 찾을 수 없습니다."
                    )
                    return@launch
                }

                val routeOptions = buildList {
                    itineraries.forEachIndexed { idx, itin ->
                        parseItinerary(idx, itin)?.let { add(it) }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    routes = routeOptions
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "경로 탐색 실패: ${e.message}"
                )
            }
        }
    }

    fun selectRoute(route: RouteOption) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val current = dataStore.userSettings.first()
                dataStore.saveSettings(
                    current.copy(
                        missionTransitType = route.missionTransitType,
                        missionStopId = route.missionStopId,
                        missionRouteId = route.missionRouteId,
                        missionRouteName = route.missionRouteName,
                        missionStopName = route.missionStopName,
                        missionDirection = route.missionDirection
                    )
                )
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedSuccessfully = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "저장 실패: ${e.message}"
                )
            }
        }
    }

    fun retry() = fetchRoutes()

    // ── 내부 헬퍼 ────────────────────────────────────────────

    /** 내일 목표 출근 시각으로 searchDttm(yyyyMMddHHmm) 생성 */
    private suspend fun buildSearchDttm(settings: UserSettings): String {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)          // 다음날 (평일 기준)
            set(Calendar.HOUR_OF_DAY, settings.targetHour)
            set(Calendar.MINUTE, settings.targetMinute)
            set(Calendar.SECOND, 0)
        }
        return SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault()).format(cal.time)
    }

    private suspend fun parseItinerary(index: Int, it: TmapItinerary): RouteOption? {
        // 첫 번째 비-도보 구간이 미션 타겟
        val firstTransit = it.legs.firstOrNull { leg ->
            leg.mode != "WALK"
        } ?: return null  // 도보만 있으면 제외

        val (transitType, stopId, routeId, routeName, stopName, direction) =
            when (firstTransit.mode) {
                "BUS", "EXPRESSBUS", "CITYBUS" -> {
                    // T-map의 stationID는 서울버스 arsId가 아니므로,
                    // 탑승 정류장 좌표로 서울버스 API를 조회해 실제 arsId로 변환한다.
                    val arsId = resolveBusArsId(firstTransit.start) ?: ""
                    TransitTarget(
                        type = MissionTransitType.BUS,
                        stopId = arsId,   // 변환 실패 시 빈 문자열 → 아래에서 해당 경로 제외
                        routeId = firstTransit.routeId,
                        routeName = firstTransit.route,
                        stopName = firstTransit.start?.name ?: "",
                        direction = firstTransit.end?.name ?: ""
                    )
                }
                "SUBWAY" -> TransitTarget(
                    type = MissionTransitType.SUBWAY,
                    stopId = firstTransit.start?.name ?: "",   // 지하철은 역 이름으로 조회
                    routeId = firstTransit.routeId,
                    routeName = firstTransit.route,
                    stopName = firstTransit.start?.name ?: "",
                    direction = firstTransit.end?.name ?: ""
                )
                else -> return null  // 알 수 없는 mode
            }

        if (stopId.isBlank()) return null

        val modeLabel = when (firstTransit.mode) {
            "BUS", "EXPRESSBUS", "CITYBUS" -> "버스"
            "SUBWAY" -> "지하철"
            else -> firstTransit.mode
        }

        return RouteOption(
            index = index,
            totalTimeMin = it.totalTime / 60,
            transferCount = it.transferCount,
            fare = it.fare?.regular?.totalFare ?: 0,
            firstTransitMode = modeLabel,
            firstTransitRoute = routeName,
            firstStopName = stopName,
            direction = direction,
            missionTransitType = transitType,
            missionStopId = stopId,
            missionRouteId = routeId,
            missionRouteName = routeName,
            missionStopName = stopName,
            missionDirection = direction
        )
    }

    /**
     * T-map 탑승 정류장 좌표 → 서울버스 API arsId 변환.
     * getStationByPos(좌표 근접 조회)로 가장 가까운 정류장의 arsId를 사용한다.
     * (T-map stationID는 서울버스 arsId와 호환되지 않음)
     */
    private suspend fun resolveBusArsId(stop: TmapStop?): String? {
        val lon = stop?.lon?.takeIf { it.isNotBlank() } ?: return null
        val lat = stop.lat.takeIf { it.isNotBlank() } ?: return null
        return try {
            val resp = busApiService.getStationByPos(
                tmX = lon,
                tmY = lat,
                radius = "200",
                serviceKey = BuildConfig.BUS_API_KEY
            )
            resp.msgBody?.itemList.orEmpty()
                .filter { it.arsId.isNotBlank() && it.arsId != "0" }
                .minByOrNull { it.dist.toIntOrNull() ?: Int.MAX_VALUE }
                ?.arsId
        } catch (e: Exception) {
            null
        }
    }

    private data class TransitTarget(
        val type: MissionTransitType,
        val stopId: String,
        val routeId: String,
        val routeName: String,
        val stopName: String,
        val direction: String
    )
    private operator fun TransitTarget.component1() = type
    private operator fun TransitTarget.component2() = stopId
    private operator fun TransitTarget.component3() = routeId
    private operator fun TransitTarget.component4() = routeName
    private operator fun TransitTarget.component5() = stopName
    private operator fun TransitTarget.component6() = direction
}
