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
import androidx.compose.runtime.saveable.rememberSaveable
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

    // rememberSaveable: 버스/위치 선택 화면으로 이동했다 돌아와도(설정 composable이 dispose됐다
    // 재생성돼도) 편집 중인(아직 저장 전) 시각이 보존된다. 일반 remember는 화면 전환 시 소실됨.
    //
    // key는 저장된 "시각" 값. 버스/위치 부분 저장으로 savedSettings가 재emit돼도 시각 필드는
    // 그대로이므로 key 불변 → 보존된 편집값 복원. 최초 DataStore 로딩·실제 시각 저장 때만 재초기화.
    var alarmHour by rememberSaveable(savedSettings.alarmHour, savedSettings.alarmMinute) {
        mutableIntStateOf(savedSettings.alarmHour)
    }
    var alarmMinute by rememberSaveable(savedSettings.alarmHour, savedSettings.alarmMinute) {
        mutableIntStateOf(savedSettings.alarmMinute)
    }
    var targetHour by rememberSaveable(savedSettings.targetHour, savedSettings.targetMinute) {
        mutableIntStateOf(savedSettings.targetHour)
    }
    var targetMinute by rememberSaveable(savedSettings.targetHour, savedSettings.targetMinute) {
        mutableIntStateOf(savedSettings.targetMinute)
    }

    var showAlarmPicker by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }

    var showSavedSnackbar by remember { mutableStateOf(false) }
    var showTimeError by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showTimeError) {
        if (showTimeError) {
            snackbarHostState.showSnackbar("알람 시각은 목표 탑승 시각보다 빨라야 해요")
            showTimeError = false
        }
    }

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
                TimeDisplayCard(
                    hour = alarmHour,
                    minute = alarmMinute,
                    onClick = { showAlarmPicker = true }
                )
            }

            // ── 목표 탑승 시각 ─────────────────────────────────
            SettingsSection(title = "목표 탑승 시각") {
                TimeDisplayCard(
                    hour = targetHour,
                    minute = targetMinute,
                    onClick = { showTargetPicker = true }
                )
            }

            // ── 집/회사 위치 (경로탐색 보류로 당장 불필요 — 추후 복구) ──────
            /*
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
            */

            // ── 출근 버스 선택 + 미션 타겟 ─────────────────────────
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
                                text = savedSettings.missionRoutesLabel,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = savedSettings.missionStopName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onFindRoute,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("출근 버스 다시 선택")
                    }
                } else {
                    Button(
                        onClick = onFindRoute,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("출근 버스 선택하기")
                    }
                }
            }

            // ── 저장 ───────────────────────────────────────────
            Button(
                onClick = {
                    val alarmTotal = alarmHour * 60 + alarmMinute
                    val targetTotal = targetHour * 60 + targetMinute
                    if (alarmTotal >= targetTotal) {
                        showTimeError = true
                        return@Button
                    }
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

    if (showAlarmPicker) {
        TimePickerDialog(
            title = "알람 시각 선택",
            initialHour = alarmHour,
            initialMinute = alarmMinute,
            onConfirm = { h, m ->
                alarmHour = h
                alarmMinute = m
                showAlarmPicker = false
            },
            onDismiss = { showAlarmPicker = false }
        )
    }

    if (showTargetPicker) {
        TimePickerDialog(
            title = "목표 탑승 시각 선택",
            initialHour = targetHour,
            initialMinute = targetMinute,
            onConfirm = { h, m ->
                targetHour = h
                targetMinute = m
                showTargetPicker = false
            },
            onDismiss = { showTargetPicker = false }
        )
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

/** 탭하면 시계 다이얼로그를 여는 시각 표시 카드 */
@Composable
private fun TimeDisplayCard(
    hour: Int,
    minute: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "⏰", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(12.dp))
            Text(
                text = "%02d : %02d".format(hour, minute),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Material3 시계 TimePicker 다이얼로그 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
