package com.yeon.todaymorning.ui.locationpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeon.todaymorning.BuildConfig
import com.yeon.todaymorning.data.api.KakaoLocalApiService
import com.yeon.todaymorning.data.api.dto.AddressDocument
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocationPickerUiState(
    val lat: Double = 37.5666,          // 기본: 서울시청
    val lng: Double = 126.9784,
    val address: String = "",
    val isGeocoding: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<AddressDocument> = emptyList(),
    val isSearching: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class LocationPickerViewModel @Inject constructor(
    private val kakaoLocalApiService: KakaoLocalApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationPickerUiState())
    val uiState: StateFlow<LocationPickerUiState> = _uiState.asStateFlow()

    private val auth get() = "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}"

    /** 카메라 이동 완료 시 호출 — 중앙 좌표로 역지오코딩 */
    fun onCameraIdle(lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(lat = lat, lng = lng, isGeocoding = true)
        viewModelScope.launch {
            try {
                val response = kakaoLocalApiService.coord2Address(auth, lng, lat)
                val doc = response.documents.firstOrNull()
                val addr = doc?.roadAddress?.addressName
                    ?: doc?.address?.addressName
                    ?: "주소를 찾을 수 없습니다"
                _uiState.value = _uiState.value.copy(address = addr, isGeocoding = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    address = "주소 조회 실패",
                    isGeocoding = false
                )
            }
        }
    }

    /** 초기 위치 설정 (기존 저장값이 있을 때 지도 이동용) */
    fun setInitialLocation(lat: Double, lng: Double, address: String) {
        _uiState.value = _uiState.value.copy(lat = lat, lng = lng, address = address)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun searchAddress() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isBlank()) return
        _uiState.value = _uiState.value.copy(isSearching = true, searchResults = emptyList())
        viewModelScope.launch {
            try {
                val response = kakaoLocalApiService.searchAddress(auth, query)
                _uiState.value = _uiState.value.copy(
                    searchResults = response.documents,
                    isSearching = false,
                    errorMessage = if (response.documents.isEmpty()) "검색 결과가 없습니다" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    errorMessage = "검색 실패: ${e.message}"
                )
            }
        }
    }

    /** 검색 결과 선택 → 해당 좌표로 이동 */
    fun selectSearchResult(doc: AddressDocument) {
        val lat = doc.y.toDoubleOrNull() ?: return
        val lng = doc.x.toDoubleOrNull() ?: return
        _uiState.value = _uiState.value.copy(
            lat = lat,
            lng = lng,
            address = doc.addressName,
            searchResults = emptyList(),
            searchQuery = doc.addressName
        )
    }

    fun clearSearchResults() {
        _uiState.value = _uiState.value.copy(searchResults = emptyList())
    }
}
