package com.yeon.todaymorning.ui.busselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeon.todaymorning.data.repository.TransitRepository
import com.yeon.todaymorning.domain.model.BusRouteOption
import com.yeon.todaymorning.domain.model.BusStop
import com.yeon.todaymorning.domain.model.MissionRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 버스 선택 화면 상태. */
data class BusSelectUiState(
    val nearbyStops: List<BusStop> = emptyList(),
    val searchResults: List<BusStop> = emptyList(),
    val searchQuery: String = "",
    val isSearchMode: Boolean = false,
    val selectedStop: BusStop? = null,
    val routesAtStop: List<BusRouteOption> = emptyList(),
    /** 다수 선택된 노선 ID(busRouteId) 집합. */
    val selectedRouteIds: Set<String> = emptySet(),
    val isLoadingStops: Boolean = false,
    val isLoadingRoutes: Boolean = false,
    /** 지도가 이동해야 할 목표 좌표(검색/내주변 결과). null이면 이동 안 함. */
    val moveCameraTo: BusStop? = null
)

/** 선택 완료 결과 — 한 정류장 + 다수 노선. */
data class BusSelectResult(
    val arsId: String,
    val stopName: String,
    val routes: List<MissionRoute>
)

@HiltViewModel
class BusSelectViewModel @Inject constructor(
    private val repository: TransitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusSelectUiState())
    val uiState: StateFlow<BusSelectUiState> = _uiState.asStateFlow()

    private var nearbyJob: Job? = null
    private var searchJob: Job? = null

    /** 카메라 중심이 바뀌면(이동 종료) 주변 정류장 갱신. 디바운스. */
    fun onCameraCenterChanged(lat: Double, lng: Double) {
        if (_uiState.value.isSearchMode) return
        nearbyJob?.cancel()
        nearbyJob = viewModelScope.launch {
            delay(400)
            _uiState.update { it.copy(isLoadingStops = true) }
            val stops = repository.nearbyBusStops(lat, lng, radius = 500)
            _uiState.update { it.copy(nearbyStops = stops, isLoadingStops = false) }
        }
    }

    fun onQueryChange(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
    }

    fun search() {
        val q = _uiState.value.searchQuery.trim()
        if (q.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStops = true, isSearchMode = true) }
            val results = repository.searchBusStops(q)
            _uiState.update {
                it.copy(
                    searchResults = results,
                    isLoadingStops = false,
                    moveCameraTo = results.firstOrNull()
                )
            }
        }
    }

    fun exitSearch() {
        _uiState.update { it.copy(isSearchMode = false, searchResults = emptyList(), searchQuery = "") }
    }

    fun consumeCameraMove() {
        _uiState.update { it.copy(moveCameraTo = null) }
    }

    fun selectStop(stop: BusStop) {
        _uiState.update {
            it.copy(
                selectedStop = stop,
                isLoadingRoutes = true,
                routesAtStop = emptyList(),
                selectedRouteIds = emptySet()   // 새 정류장 선택 시 노선 선택 초기화
            )
        }
        viewModelScope.launch {
            val routes = repository.getRoutesAtStop(stop.arsId)
            _uiState.update { it.copy(routesAtStop = routes, isLoadingRoutes = false) }
        }
    }

    /** 노선 다수 선택 토글. */
    fun toggleRoute(route: BusRouteOption) {
        _uiState.update {
            val ids = it.selectedRouteIds.toMutableSet()
            if (!ids.add(route.busRouteId)) ids.remove(route.busRouteId)
            it.copy(selectedRouteIds = ids)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedStop = null, routesAtStop = emptyList(), selectedRouteIds = emptySet()) }
    }

    /** 선택된 노선들로 결과 생성. 선택이 없으면 null. */
    fun buildResult(): BusSelectResult? {
        val s = _uiState.value
        val stop = s.selectedStop ?: return null
        val routes = s.routesAtStop
            .filter { it.busRouteId in s.selectedRouteIds }
            .map { MissionRoute(routeId = it.busRouteId, routeName = it.routeName, direction = it.direction) }
        if (routes.isEmpty()) return null
        return BusSelectResult(arsId = stop.arsId, stopName = stop.name, routes = routes)
    }
}
