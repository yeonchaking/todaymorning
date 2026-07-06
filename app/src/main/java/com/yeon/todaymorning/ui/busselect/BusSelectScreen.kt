package com.yeon.todaymorning.ui.busselect

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.location.LocationServices
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.yeon.todaymorning.R
import com.yeon.todaymorning.domain.model.BusStop
import com.yeon.todaymorning.ui.common.EmptyStateText
import com.yeon.todaymorning.ui.common.SectionLoading

// 기본 카메라: 서울시청
private val DEFAULT_CENTER = LatLng.from(37.5666, 126.9784)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusSelectScreen(
    onPicked: (BusSelectResult) -> Unit,
    onBack: () -> Unit,
    viewModel: BusSelectViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember { MapView(context) }
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var labelStyles by remember { mutableStateOf<LabelStyles?>(null) }

    // 위치 권한 런처
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) moveToMyLocation(context, kakaoMap, viewModel)
    }

    // 지도 진입 시 1회: 현위치로 자동 이동 (권한 있으면 바로, 없으면 요청)
    var didAutoLocate by remember { mutableStateOf(false) }
    LaunchedEffect(kakaoMap) {
        if (kakaoMap == null || didAutoLocate) return@LaunchedEffect
        didAutoLocate = true
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) moveToMyLocation(context, kakaoMap, viewModel)
        else locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // map.start() 콜백 (AndroidView factory에서 1회 호출)
    val readyCallback = remember {
        object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                kakaoMap = map
                // Kakao Map SDK는 벡터 드로어블을 지원하지 않으므로 비트맵으로 변환
                // dp→px 명시 변환 (핀: 14×22dp — 기존 32×44의 절반 + 가로 추가 축소)
                val density = context.resources.displayMetrics.density
                val markerW = (14 * density).toInt()
                val markerH = (22 * density).toInt()
                val drawable = ContextCompat.getDrawable(context, R.drawable.ic_bus_marker)
                val markerBitmap: Bitmap? = drawable?.let { d ->
                    val bmp = Bitmap.createBitmap(markerW, markerH, Bitmap.Config.ARGB_8888)
                    d.setBounds(0, 0, markerW, markerH)
                    d.draw(Canvas(bmp))
                    bmp
                }
                labelStyles = if (markerBitmap != null) {
                    map.labelManager?.addLabelStyles(
                        LabelStyles.from(LabelStyle.from(markerBitmap))
                    )
                } else null
                // 라벨(정류장) 클릭 → 정류장 선택
                map.setOnLabelClickListener { _, _, label ->
                    val arsId = label.tag as? String
                    if (arsId != null) {
                        val all = viewModel.uiState.value.let { it.nearbyStops + it.searchResults }
                        all.firstOrNull { it.arsId == arsId }?.let { viewModel.selectStop(it) }
                    }
                    true
                }
                // 카메라 이동 종료 → 주변 정류장 갱신
                map.setOnCameraMoveEndListener { _, cameraPosition, _ ->
                    val p = cameraPosition.position
                    viewModel.onCameraCenterChanged(p.latitude, p.longitude)
                }
                // 최초 진입: 기본 좌표 주변 로드
                viewModel.onCameraCenterChanged(DEFAULT_CENTER.latitude, DEFAULT_CENTER.longitude)
            }
        }
    }
    val lifeCycleCallback = remember {
        object : MapLifeCycleCallback() {
            override fun onMapDestroy() {}
            override fun onMapError(error: Exception?) {}
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
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.finish()
        }
    }

    // 검색 결과로 카메라 이동
    LaunchedEffect(state.moveCameraTo, kakaoMap) {
        val target = state.moveCameraTo
        val map = kakaoMap
        if (target != null && map != null) {
            map.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(target.lat, target.lng), 16))
            viewModel.selectStop(target)
            viewModel.consumeCameraMove()
        }
    }

    // 정류장 목록 변경 시 라벨 다시 그리기
    val stopsToShow = if (state.isSearchMode) state.searchResults else state.nearbyStops
    LaunchedEffect(stopsToShow, kakaoMap, labelStyles) {
        val map = kakaoMap ?: return@LaunchedEffect
        val styles = labelStyles ?: return@LaunchedEffect
        val layer = map.labelManager?.layer ?: return@LaunchedEffect
        layer.removeAll()
        stopsToShow.forEach { stop ->
            layer.addLabel(
                LabelOptions.from(LatLng.from(stop.lat, stop.lng))
                    .setStyles(styles)
                    .setTag(stop.arsId)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("출근 버스 선택") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 카카오 지도 (앱 내 유일한 MapView 부착 지점)
            AndroidView(
                factory = {
                    mapView.apply { start(lifeCycleCallback, readyCallback) }
                },
                modifier = Modifier.matchParentSize()
            )

            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                // 검색바
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("정류장 이름 검색 (예: 강남역)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.isSearchMode) {
                            TextButton(onClick = viewModel::exitSearch) { Text("주변") }
                        } else {
                            TextButton(onClick = viewModel::search) { Text("검색") }
                        }
                    },
                    singleLine = true,
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = { viewModel.search() }
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // 내 주변 버튼
            FloatingActionButton(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) moveToMyLocation(context, kakaoMap, viewModel)
                    else locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = "내 주변")
            }

            if (state.isLoadingStops) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }

    // 노선 선택 바텀시트
    val selected = state.selectedStop
    if (selected != null) {
        ModalBottomSheet(onDismissRequest = viewModel::clearSelection) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(selected.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "정류소번호 ${selected.arsId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text("경유 노선", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))

                when {
                    state.isLoadingRoutes -> {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            SectionLoading()
                        }
                    }
                    state.routesAtStop.isEmpty() -> {
                        EmptyStateText(
                            text = "이 정류장의 노선 정보를 불러오지 못했어요. 다른 정류장을 선택해 주세요.",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }
                    else -> {
                        Text(
                            "타는 노선을 모두 선택하세요 (여러 개 가능)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                            items(state.routesAtStop, key = { it.busRouteId }) { route ->
                                val checked = route.busRouteId in state.selectedRouteIds
                                ListItem(
                                    headlineContent = { Text("${route.routeName}번") },
                                    supportingContent = {
                                        Text(
                                            buildString {
                                                if (route.direction.isNotBlank()) append("방면 ${route.direction}")
                                                if (!route.arrivalMessage.isNullOrBlank()) {
                                                    if (isNotEmpty()) append("  ·  ")
                                                    append(route.arrivalMessage)
                                                }
                                            }
                                        )
                                    },
                                    leadingContent = {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = { viewModel.toggleRoute(route) }
                                        )
                                    },
                                    modifier = Modifier.clickable { viewModel.toggleRoute(route) }
                                )
                                HorizontalDivider()
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.buildResult()?.let(onPicked) },
                            enabled = state.selectedRouteIds.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (state.selectedRouteIds.isEmpty()) "노선을 선택하세요"
                                else "선택 완료 (${state.selectedRouteIds.size})"
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun moveToMyLocation(
    context: android.content.Context,
    kakaoMap: KakaoMap?,
    viewModel: BusSelectViewModel
) {
    val map = kakaoMap ?: return
    LocationServices.getFusedLocationProviderClient(context).lastLocation
        .addOnSuccessListener { loc ->
            if (loc != null) {
                val here = LatLng.from(loc.latitude, loc.longitude)
                map.moveCamera(CameraUpdateFactory.newCenterPosition(here, 16))
                viewModel.onCameraCenterChanged(loc.latitude, loc.longitude)
            }
        }
}
