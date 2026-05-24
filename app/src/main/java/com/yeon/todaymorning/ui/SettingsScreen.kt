package com.yeon.todaymorning.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yeon.todaymorning.domain.model.TransitType
import com.yeon.todaymorning.domain.model.UserSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
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
    var busRouteId by remember(savedSettings) { mutableStateOf(savedSettings.busRouteId) }
    var subwayStationId by remember(savedSettings) { mutableStateOf(savedSettings.subwayStationId) }
    var subwayLineId by remember(savedSettings) { mutableStateOf(savedSettings.subwayLineId) }

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
            // ── 알람 시각 ──────────────────────────────────
            SettingsSection(title = "알람 시각") {
                TimePicker(
                    hour = alarmHour,
                    minute = alarmMinute,
                    onHourChange = { alarmHour = it },
                    onMinuteChange = { alarmMinute = it }
                )
            }

            // ── 목표 탑승 시각 ─────────────────────────────
            SettingsSection(title = "목표 탑승 시각") {
                TimePicker(
                    hour = targetHour,
                    minute = targetMinute,
                    onHourChange = { targetHour = it },
                    onMinuteChange = { targetMinute = it }
                )
            }

            // ── 대중교통 유형 ──────────────────────────────
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

            // ── 버스 정보 ──────────────────────────────────
            if (transitType == TransitType.BUS || transitType == TransitType.BOTH) {
                SettingsSection(title = "버스 정보") {
                    OutlinedTextField(
                        value = busStopId,
                        onValueChange = { busStopId = it },
                        label = { Text("정류장 ID") },
                        placeholder = { Text("예: 111000001") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = busRouteId,
                        onValueChange = { busRouteId = it },
                        label = { Text("노선 ID") },
                        placeholder = { Text("예: 100100118") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // ── 지하철 정보 ────────────────────────────────
            if (transitType == TransitType.SUBWAY || transitType == TransitType.BOTH) {
                SettingsSection(title = "지하철 정보") {
                    OutlinedTextField(
                        value = subwayStationId,
                        onValueChange = { subwayStationId = it },
                        label = { Text("역 ID") },
                        placeholder = { Text("예: 1001000123") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = subwayLineId,
                        onValueChange = { subwayLineId = it },
                        label = { Text("호선") },
                        placeholder = { Text("예: 1호선") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // ── 저장 버튼 ──────────────────────────────────
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
                            busRouteId = busRouteId,
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
        // 시간
        TimeSpinner(
            value = hour,
            range = 0..23,
            label = "시",
            onValueChange = onHourChange
        )
        Text(
            text = " : ",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        // 분
        TimeSpinner(
            value = minute,
            range = 0..59 step 5,
            label = "분",
            onValueChange = onMinuteChange
        )
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
    val current = values.indexOfFirst { it >= value }.takeIf { it >= 0 } ?: 0

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = {
            val idx = values.indexOf(value)
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
            val idx = values.indexOf(value)
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
