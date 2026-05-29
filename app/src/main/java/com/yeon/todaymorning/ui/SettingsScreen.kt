package com.yeon.todaymorning.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import com.yeon.todaymorning.domain.model.TransitType
import com.yeon.todaymorning.domain.model.UserSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onPickBus: () -> Unit,
    resultHandle: SavedStateHandle,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val savedSettings by viewModel.settings.collectAsState()

    // 로컬 편집 상태
    var alarmHour by remember(savedSettings) { mutableIntStateOf(savedSettings.alarmHour) }
    var alarmMinute by remember(savedSettings) { mutableIntStateOf(savedSettings.alarmMinute) }
    var targetHour by remember(savedSettings) { mutableIntStateOf(savedSettings.targetHour) }
    var targetMinute by remember(savedSettings) { mutableIntStateOf(savedSettings.targetMinute) }
    var transitType by remember(savedSettings) { mutableStateOf(savedSettings.transitType) }
    var busStopId by remember(savedSettings) { mutableStateOf(savedSettings.busStopId) }
    var busStopName by remember(savedSettings) { mutableStateOf(savedSettings.busStopName) }
    var busRouteId by remember(savedSettings) { mutableStateOf(savedSettings.busRouteId) }
    var busRouteName by remember(savedSettings) { mutableStateOf(savedSettings.busRouteName) }
    var busDirection by remember(savedSettings) { mutableStateOf(savedSettings.busDirection) }
    var subwayStationId by remember(savedSettings) { mutableStateOf(savedSettings.subwayStationId) }
    var subwayLineId by remember(savedSettings) { mutableStateOf(savedSettings.subwayLineId) }

    // 지도 화면에서 돌아온 버스 선택 결과 수신
    val pickedRouteId by resultHandle.getStateFlow("bus_routeId", "").collectAsState()
    LaunchedEffect(pickedRouteId) {
        if (pickedRouteId.isNotBlank()) {
            busStopId = resultHandle["bus_arsId"] ?: ""
            busStopName = resultHandle["bus_stopName"] ?: ""
            busRouteId = pickedRouteId
            busRouteName = resultHandle["bus_routeName"] ?: ""
            busDirection = resultHandle["bus_direction"] ?: ""
            resultHandle["bus_routeId"] = "" // 소비
        }
    }

    var showSavedSnackbar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showSavedSnackbar) {
        if (showSavedSnackbar) {
            snackbarHostState.showSnackbar("설정이 저장되었습니다")
            showSavedSnackbar = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection(title = "알람 시각") {
                TimePicker(alarmHour, alarmMinute, { alarmHour = it }, { alarmMinute = it })
            }

            SettingsSection(title = "목표 탑승 시각") {
                TimePicker(targetHour, targetMinute, { targetHour = it }, { targetMinute = it })
            }

            SettingsSection(title = "대중교통 유형") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransitType.entries.forEach { type ->
                        FilterChip(
                            selected = transitType == type,
                            onClick = { transitType = type },
                            label = {
                                Text(
                                    when (type) {
                                        TransitType.BUS -> "버스"
                                        TransitType.SUBWAY -> "지하철"
                                        TransitType.BOTH -> "버스+지하철"
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // ── 버스 정보 (지도 선택) ──────────────────────
            if (transitType == TransitType.BUS || transitType == TransitType.BOTH) {
                SettingsSection(title = "출근 버스") {
                    if (busRouteId.isNotBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "${busRouteName}번",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    buildString {
                                        append(busStopName)
                                        if (busDirection.isNotBlank()) append("  ·  방면 $busDirection")
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onPickBus, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Place, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("버스 변경")
                        }
                    } else {
                        Button(onClick = onPickBus, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Place, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("지도에서 버스 선택하기")
                        }
                    }
                }
            }

            // ── 지하철 정보 (v1 미구현 안내) ───────────────
            if (transitType == TransitType.SUBWAY || transitType == TransitType.BOTH) {
                SettingsSection(title = "지하철 정보") {
                    Text(
                        "지하철 선택은 다음 버전에서 지원됩니다. 현재는 버스로 이용해 주세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.saveSettings(
                        UserSettings(
                            alarmHour = alarmHour,
                            alarmMinute = alarmMinute,
                            targetHour = targetHour,
                            targetMinute = targetMinute,
                            transitType = transitType,
                            busStopId = busStopId,
                            busStopName = busStopName,
                            busRouteId = busRouteId,
                            busRouteName = busRouteName,
                            busDirection = busDirection,
                            subwayStationId = subwayStationId,
                            subwayLineId = subwayLineId
                        )
                    )
                    showSavedSnackbar = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("저장 및 알람 등록", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun TimePicker(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        TimeSpinner(value = hour, range = 0..23, label = "시", onValueChange = onHourChange)
        Text(
            text = " : ",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        TimeSpinner(value = minute, range = 0..59 step 5, label = "분", onValueChange = onMinuteChange)
    }
}

@Composable
private fun TimeSpinner(
    value: Int,
    range: IntProgression,
    label: String,
    onValueChange: (Int) -> Unit
) {
    val values = range.toList()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = {
            val idx = values.indexOf(value).takeIf { it >= 0 } ?: 0
            onValueChange(values[(idx + 1) % values.size])
        }) {
            Text("▲", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = "%02d".format(value),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = {
            val idx = values.indexOf(value).takeIf { it >= 0 } ?: 0
            onValueChange(values[(idx - 1 + values.size) % values.size])
        }) {
            Text("▼", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
