package com.yeon.todaymorning.ui.locationpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory

private val DEFAULT_CENTER = LatLng.from(37.5666, 126.9784)

data class LocationResult(
    val lat: Double,
    val lng: Double,
    val address: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    title: String,                          // "집 위치 설정" or "회사 위치 설정"
    initialLat: Double = 0.0,
    initialLng: Double = 0.0,
    initialAddress: String = "",
    onConfirm: (LocationResult) -> Unit,
    onBack: () -> Unit,
    viewModel: LocationPickerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember { MapView(context) }
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }

    // 펜 버튼으로 진입 = 위치를 새로 정하려는 의도 → 진입하면 항상 빈 입력 Dialog를 띄운다.
    // (닫으면 기존 저장 위치의 핀/확인 카드가 뒤에 보인다)
    var showSearchDialog by remember { mutableStateOf(true) }

    // 초기 저장값 세팅 (핀 미리 찍기)
    LaunchedEffect(Unit) {
        if (initialLat != 0.0 && initialLng != 0.0) {
            viewModel.setInitialLocation(initialLat, initialLng, initialAddress)
        }
    }

    // 선택된 위치로만 카메라 이동 — 사용자 드래그/확대는 건드리지 않음
    LaunchedEffect(kakaoMap) {
        val map = kakaoMap ?: return@LaunchedEffect
        viewModel.cameraMoveEvent.collect { move ->
            map.moveCamera(
                CameraUpdateFactory.newCenterPosition(
                    LatLng.from(move.lat, move.lng), move.zoom
                )
            )
        }
    }

    // KakaoMap ready callback
    val readyCallback = remember {
        object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                kakaoMap = map
                val initLat = if (initialLat != 0.0) initialLat else DEFAULT_CENTER.latitude
                val initLng = if (initialLng != 0.0) initialLng else DEFAULT_CENTER.longitude
                map.moveCamera(
                    CameraUpdateFactory.newCenterPosition(LatLng.from(initLat, initLng), 16)
                )
            }
        }
    }

    val lifecycleCallback = remember {
        object : MapLifeCycleCallback() {
            override fun onMapDestroy() {}
            override fun onMapError(e: Exception?) {}
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.pause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.start(lifecycleCallback, readyCallback)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.finish()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── 지도 ───────────────────────────────────────────
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )

            // ── 중앙 핀 (선택된 위치 표시) ─────────────────────
            if (state.hasSelection) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "선택 위치",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center)
                        .offset(y = (-20).dp)  // 핀 아래쪽 끝이 중앙에 오도록
                )
            }

            // ── 하단: "여기가 맞나요?" 확인 카드 ───────────────
            if (state.hasSelection) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "여기가 맞나요?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = state.selectedAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.clearSearch()
                                showSearchDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("재입력")
                        }
                        Button(
                            onClick = {
                                onConfirm(
                                    LocationResult(
                                        lat = state.selectedLat,
                                        lng = state.selectedLng,
                                        address = state.selectedAddress
                                    )
                                )
                            },
                            modifier = Modifier.weight(2f)
                        ) {
                            Text("확인")
                        }
                    }
                }
            } else {
                // 선택 전: 주소 입력 안내 버튼
                Button(
                    onClick = { showSearchDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("주소 검색")
                }
            }
        }
    }

    // ── 주소 입력 Dialog ───────────────────────────────────────
    if (showSearchDialog) {
        AddressSearchDialog(
            query = state.searchQuery,
            results = state.searchResults,
            isSearching = state.isSearching,
            errorMessage = state.errorMessage,
            onQueryChange = viewModel::onSearchQueryChange,
            onSearch = viewModel::searchAddress,
            onSelect = { doc ->
                viewModel.selectSearchResult(doc)
                showSearchDialog = false
            },
            onDismiss = { showSearchDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressSearchDialog(
    query: String,
    results: List<com.yeon.todaymorning.data.api.dto.AddressDocument>,
    isSearching: Boolean,
    errorMessage: String?,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSelect: (com.yeon.todaymorning.data.api.dto.AddressDocument) -> Unit,
    onDismiss: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("주소 검색", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("도로명 또는 지번 주소") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()
                        onSearch()
                    }),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                when {
                    isSearching -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("검색 중...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    results.isNotEmpty() -> {
                        LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                            items(results) { doc ->
                                Text(
                                    text = doc.addressName,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            keyboardController?.hide()
                                            onSelect(doc)
                                        }
                                        .padding(vertical = 12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                    errorMessage != null -> {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("닫기") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        keyboardController?.hide()
                        onSearch()
                    }) { Text("검색") }
                }
            }
        }
    }
}
