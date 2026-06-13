package com.yeon.todaymorning.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yeon.todaymorning.domain.model.UserSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onPickHome: () -> Unit,
    onPickWork: () -> Unit,
    onFindRoute: () -> Unit,          // Phase 2: 경로 탐색 화면
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val savedSettings by viewModel.settings.collectAsState()

    var alarmHour by remember(savedSettings) { mutableIntStateOf(savedSettings.alarmHour) }
    var alarmMinute by remember(savedSettings) { mutableIntStateOf(savedSettings.alarmMinute) }
    var targetHour by remember(savedSettings) { mutableIntStateOf(savedSettings.targetHour) }
    var targetMinute by remember(savedSettings) { mutableIntStateOf(savedSettings.targetMinute) }

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
            // ── 알람 시각 ──────────────────────────────────────
            SettingsSection(title = "알람 시각") {
                TimePicker(alarmHour, alarmMinute, { alarmHour = it }, { alarmMinute = it })
            }

            // ── 목표 탑승 시각 ─────────────────────────────────
            SettingsSection(title = "목표 탑승 시각") {
                TimePicker(targetHour, targetMinute, { targetHour = it }, { targetMinute = it })
            }

            // ── 집 위치 ────────────────────────────────────────
            SettingsSection(title = "집 위치") {
                LocationCard(
                    icon = "🏠",
                    address = savedSettings.homeAddress,
                    placeholder = "집 위치를 설정해 주세요",
                    onEdit = onPickHome
                )
            }

            // ── 회사 위치 ──────────────────────────────────────
            SettingsSection(title = "회사 위치") {
                LocationCard(
                    icon = "🏢",
                    address = savedSettings.workAddress,
                    placeholder = "회사 위치를 설정해 주세요",
                    onEdit = onPickWork
                )
            }

            // ── 경로 탐색 + 미션 타겟 ─────────────────────────
            SettingsSection(title = "출근 경로") {
                if (savedSettings.hasMissionTarget) {
                    // 미션 타겟 요약 카드
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = when (savedSettings.missionTransitType) {
                                    com.yeon.todaymorning.domain.model.MissionTransitType.BUS -> "🚌 버스"
                                    com.yeon.todaymorning.domain.model.MissionTransitType.SUBWAY -> "🚇 지하철"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = savedSettings.missionRouteName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = buildString {
                                    append(savedSettings.missionStopName)
                                    if (savedSettings.missionDirection.isNotBlank())
                                        append("  ·  방면 ${savedSettings.missionDirection}")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onFindRoute,
                        enabled = savedSettings.hasHomeLocation && savedSettings.hasWorkLocation,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("경로 다시 탐색")
                    }
                } else {
                    Button(
                        onClick = onFindRoute,
                        enabled = savedSettings.hasHomeLocation && savedSettings.hasWorkLocation,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("출근 경로 탐색하기")
                    }
                    if (!savedSettings.hasHomeLocation || !savedSettings.hasWorkLocation) {
                        Text(
                            text = "집과 회사 위치를 먼저 설정해 주세요",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // ── 저장 ───────────────────────────────────────────
            Button(
                onClick = {
                    viewModel.saveSettings(
                        savedSettings.copy(
                            alarmHour = alarmHour,
                            alarmMinute = alarmMinute,
                            targetHour = targetHour,
                            targetMinute = targetMinute
                        )
                    )
                    onNavigateBack()
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
private fun LocationCard(
    icon: String,
    address: String,
    placeholder: String,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = icon, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(10.dp))
            Text(
                text = address.ifBlank { placeholder },
                style = MaterialTheme.typography.bodyMedium,
                color = if (address.isNotBlank()) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (address.isNotBlank()) FontWeight.Medium else FontWeight.Normal
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "위치 변경",
                tint = MaterialTheme.colorScheme.primary
            )
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
        TimeSpinner(value = minute, range = 0..59 step 1, label = "분", onValueChange = onMinuteChange)
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
