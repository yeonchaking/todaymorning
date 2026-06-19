package com.yeon.todaymorning.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import com.yeon.todaymorning.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onPickHome: () -> Unit,
    onPickWork: () -> Unit,
    onFindRoute: () -> Unit,          // 출근 경로(버스 선택) 화면
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val savedSettings by viewModel.settings.collectAsState()
    val c = AppTheme.colors

    // rememberSaveable: 버스/위치 선택 화면을 다녀와도 편집 중(저장 전) 시각 보존.
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

    // TODO(미구현): 엄격 모드는 아직 저장/동작 없음. 화면 표시용 로컬 상태.
    var strictMode by rememberSaveable { mutableStateOf(false) }

    var showSavedSnackbar by remember { mutableStateOf(false) }
    var showTimeError by remember { mutableStateOf(false) }
    var comingSoon by remember { mutableStateOf(false) }
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
    LaunchedEffect(comingSoon) {
        if (comingSoon) {
            snackbarHostState.showSnackbar("준비 중인 기능이에요")
            comingSoon = false
        }
    }

    Scaffold(
        containerColor = c.appBg,
        topBar = {
            TopAppBar(
                title = { Text("설정", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = c.header,
                    titleContentColor = c.onHeader,
                    navigationIconContentColor = c.onHeader
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            // ── 알람 그룹 ───────────────────────────────────
            GroupLabel("알람")
            GroupCard {
                SettingRow(
                    emoji = "🔔",
                    title = "알람 시각",
                    value = "%02d:%02d".format(alarmHour, alarmMinute),
                    onClick = { showAlarmPicker = true }
                )
                RowDivider()
                SettingRow(
                    emoji = "🚩",
                    title = "목표 탑승 시각",
                    value = "%02d:%02d".format(targetHour, targetMinute),
                    onClick = { showTargetPicker = true }
                )
                RowDivider()
                SettingRow(
                    emoji = "🎵",
                    title = "알람음",
                    value = "활기찬 아침",
                    onClick = { comingSoon = true }   // TODO(미구현): 알람음 선택
                )
            }

            // ── 출근 경로 그룹 ──────────────────────────────
            GroupLabel("출근 경로")
            GroupCard {
                // 지도 미리보기 (장식)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(c.surface2),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📍", fontSize = 30.sp)
                }
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickableRow { onFindRoute() }
                        .padding(vertical = 12.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    LeadingIcon(emoji = "🚌", bg = c.lilac)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = savedSettings.missionStopName.ifBlank { "출근 경로를 선택하세요" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = c.on
                        )
                        if (savedSettings.hasMissionTarget) {
                            Text(
                                text = "노선 ${savedSettings.missionRoutesLabel}",
                                fontSize = 12.sp,
                                color = c.onVar
                            )
                        }
                    }
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = c.onVar)
                }
            }

            // ── 게임 그룹 ───────────────────────────────────
            GroupLabel("게임")
            GroupCard {
                SettingRow(
                    emoji = "📈",
                    title = "미션 난이도",
                    value = "보통",
                    onClick = { comingSoon = true }   // TODO(미구현): 난이도 선택
                )
                RowDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    LeadingIcon(emoji = "⚡", bg = c.surface2)
                    Text(
                        text = "엄격 모드",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.on,
                        modifier = Modifier.weight(1f)
                    )
                    // TODO(미구현): 토글 상태가 저장/동작에 연결되어 있지 않음.
                    Switch(checked = strictMode, onCheckedChange = { strictMode = it })
                }
            }

            Spacer(Modifier.height(6.dp))

            // ── 저장 ───────────────────────────────────────
            Surface(
                onClick = {
                    val alarmTotal = alarmHour * 60 + alarmMinute
                    val targetTotal = targetHour * 60 + targetMinute
                    if (alarmTotal >= targetTotal) {
                        showTimeError = true
                        return@Surface
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
                shape = RoundedCornerShape(28.dp),
                color = c.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔔", fontSize = 19.sp)
                    Spacer(Modifier.width(9.dp))
                    Text(
                        "저장 및 알람 등록",
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = c.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showAlarmPicker) {
        TimePickerDialog(
            title = "알람 시각 선택",
            initialHour = alarmHour,
            initialMinute = alarmMinute,
            onConfirm = { h, m -> alarmHour = h; alarmMinute = m; showAlarmPicker = false },
            onDismiss = { showAlarmPicker = false }
        )
    }
    if (showTargetPicker) {
        TimePickerDialog(
            title = "목표 탑승 시각 선택",
            initialHour = targetHour,
            initialMinute = targetMinute,
            onConfirm = { h, m -> targetHour = h; targetMinute = m; showTargetPicker = false },
            onDismiss = { showTargetPicker = false }
        )
    }
}

/* ─────────────────────────── 구성 요소 ─────────────────────────── */

@Composable
private fun GroupLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        color = AppTheme.colors.primary,
        modifier = Modifier.padding(start = 6.dp, top = 4.dp)
    )
}

@Composable
private fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = AppTheme.colors.surface,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp), content = content)
    }
}

@Composable
private fun SettingRow(
    emoji: String,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    val c = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickableRow { onClick() }
            .padding(vertical = 12.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        LeadingIcon(emoji = emoji, bg = c.surface2)
        Text(
            text = title,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.on,
            modifier = Modifier.weight(1f)
        )
        Text(text = value, fontSize = 14.sp, color = c.onVar)
        Spacer(Modifier.width(5.dp))
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = c.onVar)
    }
}

@Composable
private fun LeadingIcon(emoji: String, bg: Color) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 19.sp)
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = AppTheme.colors.outline.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 51.dp)
    )
}

/** 행 전체 클릭 모디파이어 (기본 ripple). */
private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)

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
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("확인") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}
