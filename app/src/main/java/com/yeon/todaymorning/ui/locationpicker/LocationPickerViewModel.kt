package com.yeon.todaymorning.ui.locationpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeon.todaymorning.BuildConfig
import com.yeon.todaymorning.data.api.KakaoLocalApiService
import com.yeon.todaymorning.data.api.dto.AddressDocument
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 선택된 좌표로 카메라를 이동시키는 일회성 명령 */
data class CameraMove(val lat: Double, val lng: Double, val zoom: Int)

data class LocationPickerUiState(
    // 현재 선택(확정 대기) 위치
    val selectedLat: Double = 0.0,
    val selectedLng: Double = 0.0,
    val selectedAddress: String = "",
    val hasSelection: Boolean = false,
    // 검색
    val searchQuery: String = "",
    val searchResults: List<AddressDocument> = emptyList(),
    val isSearching: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 주소 검색(search/address) → 결과 선택 → 핀 표시 → 확인 방식.
 * 역지오코딩(coord2address)은 사용하지 않는다. 지도를 움직여도 API를 호출하지 않으므로
 * 호출 한도(코드 -10)를 소모하지 않는다.
 */
@HiltViewModel
class LocationPickerViewModel @Inject constructor(
    private val kakaoLocalApiService: KakaoLocalApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationPickerUiState())
    val uiState: StateFlow<LocationPickerUiState> = _uiState.asStateFlow()

    // 선택된 위치로 카메라를 옮기는 일회성 이벤트
    private val _cameraMoveEvent = MutableSharedFlow<CameraMove>(extraBufferCapacity = 1)
    val cameraMoveEvent: SharedFlow<CameraMove> = _cameraMoveEvent.asSharedFlow()

    private val auth get() = "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}"

    /** 기존 저장값이 있을 때 핀을 미리 찍어둔다 (지도 이동은 onMapReady에서 직접 처리) */
    fun setInitialLocation(lat: Double, lng: Double, address: String) {
        _uiState.value = _uiState.value.copy(
            selectedLat = lat,
            selectedLng = lng,
            selectedAddress = address,
            hasSelection = true
            // searchQuery는 비워둬서 재입력 Dialog가 빈 상태로 열리도록 한다
        )
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    /** 주소 검색 (search/address — 역지오코딩과 별도 쿼터) */
    fun searchAddress() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isBlank()) return
        _uiState.value = _uiState.value.copy(
            isSearching = true,
            searchResults = emptyList(),
            errorMessage = null
        )
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

    /** 검색 결과 선택 → 핀 확정 대기 + 카메라 이동 */
    fun selectSearchResult(doc: AddressDocument) {
        val lat = doc.y.toDoubleOrNull() ?: return
        val lng = doc.x.toDoubleOrNull() ?: return
        _uiState.value = _uiState.value.copy(
            selectedLat = lat,
            selectedLng = lng,
            selectedAddress = doc.addressName,
            hasSelection = true,
            searchQuery = doc.addressName,
            searchResults = emptyList(),
            errorMessage = null
        )
        _cameraMoveEvent.tryEmit(CameraMove(lat, lng, 16))
    }

    /** 재입력 시작 — 입력어/검색결과/에러를 모두 비워 Dialog를 빈 상태로 연다 */
    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            searchResults = emptyList(),
            errorMessage = null
        )
    }
}
