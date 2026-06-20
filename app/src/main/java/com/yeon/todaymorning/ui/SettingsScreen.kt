package com.yeon.todaymorning.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.yeon.todaymorning.domain.model.EVERYDAY
import com.yeon.todaymorning.domain.model.WEEKDAYS
import com.yeon.todaymorning.domain.model.WEEKEND
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

    var alarmHour by rememberSaveable(savedSettings.alarmHour, savedSettings.alarmMinute) { mutableIntStateOf(savedSettings.alarmHour) }
    var alarmMinute by rememberSaveable(savedSettings.alarmHour, savedSettings.alarmMinute) { mutableIntStateOf(savedSettings.alarmMinute) }
    var targetHour by rememberSaveable(savedSettings.targetHour, savedSettings.targetMinute) { mutableIntStateOf(savedSettings.targetHour) }
    var targetMinute by rememberSaveable(savedSettings.targetHour, savedSettings.targetMinute) { mutableIntStateOf(savedSettings.targetMinute) }

    var showAlarmPicker by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }

    // TODO(미구현): 아래 상태들은 화면 표시용 로컬 상태. 저장/동작 연결 필요.
    var vibrate by rememberSaveable { mutableStateOf(true) }
    var ttsOn by rememberSaveable { mutableStateOf(true) }
    var tts10 by rememberSaveable { mutableStateOf(true) }
    var tts5 by rememberSaveable { mutableStateOf(true) }
    var tts3 by rememberSaveable { mutableStateOf(true) }

    var showRepeatDialog by remember { mutableStateOf(false) }

    var showTimeError by remember { mutableStateOf(false) }
    var comingSoon by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showTimeError) {
        if (showTimeError) { snackbarHostState.showSnackbar("알람 시각은 목표 탑승 시각보다 빨라야 해요"); showTimeError = false }
    }
    LaunchedEffect(comingSoon) {
        if (comingSoon) { snackbarHostState.showSnackbar("준비 중인 기능이에요"); comingSoon = false }
    }

    Scaffold(
        containerColor = c.appBg,
        topBar = {
            TopAppBar(
                title = { Text("설정", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = c.surface,
                    titleContentColor = c.on,
                    navigationIconContentColor = c.on
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ── 알람 ──────────────────────────────────────
            SettingsGroup("알람") {
                NavRow("⏰", "알람 시각", "%02d:%02d".format(alarmHour, alarmMinute), valueStrong = true) { showAlarmPicker = true }
                RowDivider()
                NavRow("🚌", "목표 탑승 시각", "%02d:%02d".format(targetHour, targetMinute), valueStrong = true) { showTargetPicker = true }
                RowDivider()
                NavRow("🔁", "요일 반복", savedSettings.repeatDaysLabel) { showRepeatDialog = true }
                RowDivider()
                NavRow("🎵", "알람음", "기본 알람음") { comingSoon = true }   // TODO(미구현)
                RowDivider()
                ToggleRow("📳", "진동", vibrate) { vibrate = it }          // TODO(미구현): 저장 연동
            }

            // ── 출근 경로 ─────────────────────────────────
            SettingsGroup("출근 경로") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    SquareIcon("🚌", c.primaryCtr)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (savedSettings.hasMissionTarget) "${savedSettings.missionRoutesLabel} 번 버스" else "출근 경로 미설정",
                            fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = c.on
                        )
                        Text(savedSettings.missionStopName.ifBlank { "정류장·노선을 선택하세요" }, fontSize = 12.5.sp, color = c.onVar)
                    }
                }
                RowDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { comingSoon = true } // TODO(미구현): 지하철 직접선택 화면
                        .padding(vertical = 14.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    SquareIcon("🚇", c.surface2)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("지하철 추가", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = c.on)
                        Text("역명 검색으로 노선 추가", fontSize = 12.5.sp, color = c.onVar)
                    }
                    Text("＋", fontSize = 22.sp, color = c.primary)
                }
                RowDivider()
                Box(modifier = Modifier.padding(vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                            .border(1.5.dp, c.primary, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onFindRoute() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("⚙️", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("정류장 · 노선 다시 선택", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.primary)
                    }
                }
            }

            // ── 음성 안내 ─────────────────────────────────
            SettingsGroup("음성 안내") {
                ToggleRow("🔊", "음성 안내 (TTS)", ttsOn) { ttsOn = it }   // TODO(미구현): 실제 TTS 연동
                RowDivider()
                Column(modifier = Modifier.padding(vertical = 14.dp, horizontal = 2.dp)) {
                    Text("안내 시점", fontSize = 13.sp, color = c.onVar)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimingChip("10분 전", tts10, Modifier.weight(1f)) { tts10 = !tts10 }
                        TimingChip("5분 전", tts5, Modifier.weight(1f)) { tts5 = !tts5 }
                        TimingChip("3분 전", tts3, Modifier.weight(1f)) { tts3 = !tts3 }
                    }
                }
            }

            // ── 저장 ───────────────────────────────────────
            Surface(
                onClick = {
                    val alarmTotal = alarmHour * 60 + alarmMinute
                    val targetTotal = targetHour * 60 + targetMinute
                    if (alarmTotal >= targetTotal) { showTimeError = true; return@Surface }
                    viewModel.saveSettings(
                        savedSettings.copy(
                            alarmHour = alarmHour, alarmMinute = alarmMinute,
                            targetHour = targetHour, targetMinute = targetMinute
                        )
                    )
                    onNavigateBack()
                },
                shape = RoundedCornerShape(18.dp),
                color = c.primary,
                modifier = Modifier.fillMaxWidth().height(58.dp)
            ) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text("🔔", fontSize = 19.sp)
                    Spacer(Modifier.width(9.dp))
                    Text("저장 및 알람 등록", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = c.onPrimary)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showAlarmPicker) {
        TimePickerDialog("알람 시각 선택", alarmHour, alarmMinute,
            onConfirm = { h, m -> alarmHour = h; alarmMinute = m; showAlarmPicker = false },
            onDismiss = { showAlarmPicker = false })
    }
    if (showTargetPicker) {
        TimePickerDialog("목표 탑승 시각 선택", targetHour, targetMinute,
            onConfirm = { h, m -> targetHour = h; targetMinute = m; showTargetPicker = false },
            onDismiss = { showTargetPicker = false })
    }
    if (showRepeatDialog) {
        RepeatDaysDialog(
            initial = savedSettings.repeatDays,
            onConfirm = { days -> viewModel.setRepeatDays(days); showRepeatDialog = false },
            onDismiss = { showRepeatDialog = false }
        )
    }
}

/* ─────────────────────── 요일 반복 선택 다이얼로그 ─────────────────────── */

@Composable
private fun RepeatDaysDialog(
    initial: Set<Int>,
    onConfirm: (Set<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    val c = AppTheme.colors
    var selected by remember { mutableStateOf(initial) }

    // 표시 순서: 월~일. 각 칩 = (라벨, Calendar 요일값)
    val days = listOf(
        "월" to java.util.Calendar.MONDAY,
        "화" to java.util.Calendar.TUESDAY,
        "수" to java.util.Calendar.WEDNESDAY,
        "목" to java.util.Calendar.THURSDAY,
        "금" to java.util.Calendar.FRIDAY,
        "토" to java.util.Calendar.SATURDAY,
        "일" to java.util.Calendar.SUNDAY
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text("확인", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
        title = { Text("요일 반복", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                // 프리셋 빠른 선택
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetChip("평일", Modifier.weight(1f)) { selected = WEEKDAYS }
                    PresetChip("매일", Modifier.weight(1f)) { selected = EVERYDAY }
                    PresetChip("주말", Modifier.weight(1f)) { selected = WEEKEND }
                }
                Spacer(Modifier.height(14.dp))
                // 개별 요일 토글
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    days.forEach { (label, cal) ->
                        val active = cal in selected
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) c.primary else c.surface2)
                                .clickable {
                                    selected = if (active) selected - cal else selected + cal
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                color = if (active) c.onPrimary else c.onVar
                            )
                        }
                    }
                }
                if (selected.isEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("요일을 선택하지 않으면 알람이 울리지 않아요", fontSize = 12.sp, color = c.onVar)
                }
            }
        }
    )
}

@Composable
private fun PresetChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = AppTheme.colors
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, c.primary, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.primary)
    }
}

/* ─────────────────────────── 구성 요소 ─────────────────────────── */

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val c = AppTheme.colors
    Column {
        Text(
            text = title,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = c.primary,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )
        Surface(shape = RoundedCornerShape(18.dp), color = c.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp), content = content)
        }
    }
}

@Composable
private fun NavRow(emoji: String, title: String, value: String, valueStrong: Boolean = false, onClick: () -> Unit) {
    val c = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 14.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        Text(title, fontSize = 15.sp, color = c.on, modifier = Modifier.weight(1f))
        Text(
            text = value,
            fontSize = if (valueStrong) 16.sp else 14.sp,
            fontWeight = if (valueStrong) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (valueStrong) c.primary else c.onVar
        )
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = c.onVar)
    }
}

@Composable
private fun ToggleRow(emoji: String, title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        Text(title, fontSize = 15.sp, color = c.on, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SquareIcon(emoji: String, bg: Color) {
    Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(bg), contentAlignment = Alignment.Center) {
        Text(emoji, fontSize = 18.sp)
    }
}

@Composable
private fun TimingChip(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = AppTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (active) c.primaryCtr else c.surface2)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) c.onPrimaryCtr else c.onVar
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = AppTheme.colors.outlineSoft)
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
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = state) } },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("확인") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
