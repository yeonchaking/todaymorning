package com.yeon.todaymorning.ui.locationpicker

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
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
    val keyboardController = LocalSoftwareKeyboardController.current

    val mapView = remember { MapView(context) }
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }

    // 초기 저장값 세팅
    LaunchedEffect(Unit) {
        if (initialLat != 0.0 && initialLng != 0.0) {
            viewModel.setInitialLocation(initialLat, initialLng, initialAddress)
        }
    }

    // 검색 결과 선택 시 지도 이동
    val targetLat = state.lat
    val targetLng = state.lng
    LaunchedEffect(targetLat, targetLng) {
        kakaoMap?.moveCamera(
            CameraUpdateFactory.newCenterPosition(LatLng.from(targetLat, targetLng), 15)
        )
    }

    // 위치 권한 런처
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) {
            moveToCurrentLocation(context, kakaoMap, viewModel)
        }
    }

    // KakaoMap ready callback
    val readyCallback = remember {
        object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                kakaoMap = map

                // 초기 위치 이동
                val initLat = if (initialLat != 0.0) initialLat else DEFAULT_CENTER.latitude
                val initLng = if (initialLng != 0.0) initialLng else DEFAULT_CENTER.longitude
                map.moveCamera(
                    CameraUpdateFactory.newCenterPosition(LatLng.from(initLat, initLng), 15)
                )

                // 카메라 이동 완료 시 역지오코딩
                map.setOnCameraMoveEndListener { _, cameraPosition, _ ->
                    val center = cameraPosition.position
                    viewModel.onCameraIdle(center.latitude, center.longitude)
                }
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

            // ── 중앙 고정 핀 ───────────────────────────────────
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "선택 위치",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Center)
                    .offset(y = (-20).dp)  // 핀 아래쪽 끝이 중앙에 오도록
            )

            // ── 상단: 주소 검색바 ──────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.TopCenter)
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("주소 검색") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()
                        viewModel.searchAddress()
                    }),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // 검색 결과 드롭다운
                if (state.searchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(state.searchResults) { doc ->
                                Text(
                                    text = doc.addressName,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            keyboardController?.hide()
                                            viewModel.selectSearchResult(doc)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            // ── 하단: 현재 주소 + 확정 버튼 ───────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 현재 주소 표시
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.isGeocoding) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("주소 확인 중...", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = state.address.ifBlank { "지도를 움직여 위치를 선택하세요" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (state.address.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                            color = if (state.address.isNotBlank()) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 내 위치로 이동
                    OutlinedButton(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) moveToCurrentLocation(context, kakaoMap, viewModel)
                            else locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📍 내 위치")
                    }

                    // 이 위치로 설정
                    Button(
                        onClick = {
                            onConfirm(
                                LocationResult(
                                    lat = state.lat,
                                    lng = state.lng,
                                    address = state.address
                                )
                            )
                        },
                        enabled = state.address.isNotBlank() && !state.isGeocoding,
                        modifier = Modifier.weight(2f)
                    ) {
                        Text("이 위치로 설정")
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun moveToCurrentLocation(
    context: android.content.Context,
    kakaoMap: KakaoMap?,
    viewModel: LocationPickerViewModel
) {
    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    fusedClient.lastLocation.addOnSuccessListener { location ->
        location ?: return@addOnSuccessListener
        kakaoMap?.moveCamera(
            CameraUpdateFactory.newCenterPosition(
                LatLng.from(location.latitude, location.longitude), 16
            )
        )
        viewModel.onCameraIdle(location.latitude, location.longitude)
    }
}
