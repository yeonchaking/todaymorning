package com.yeon.todaymorning.ui.busselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeon.todaymorning.data.repository.TransitRepository
import com.yeon.todaymorning.domain.model.BusRouteOption
import com.yeon.todaymorning.domain.model.BusStop
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
    val isLoadingStops: Boolean = false,
    val isLoadingRoutes: Boolean = false,
    /** 지도가 이동해야 할 목표 좌표(검색/내주변 결과). null이면 이동 안 함. */
    val moveCameraTo: BusStop? = null
)

/** 선택 완료 결과. */
data class BusSelectResult(
    val arsId: String,
    val stopName: String,
    val routeId: String,
    val routeName: String,
    val direction: String
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
        _uiState.update { it.copy(selectedStop = stop, isLoadingRoutes = true, routesAtStop = emptyList()) }
        viewModelScope.launch {
            val routes = repository.getRoutesAtStop(stop.arsId)
            _uiState.update { it.copy(routesAtStop = routes, isLoadingRoutes = false) }
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedStop = null, routesAtStop = emptyList()) }
    }

    fun buildResult(route: BusRouteOption): BusSelectResult? {
        val stop = _uiState.value.selectedStop ?: return null
        return BusSelectResult(
            arsId = stop.arsId,
            stopName = stop.name,
            routeId = route.busRouteId,
            routeName = route.routeName,
            direction = route.direction
        )
    }
}
