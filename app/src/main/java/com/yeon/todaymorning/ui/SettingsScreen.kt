package com.yeon.todaymorning.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yeon.todaymorning.alarm.AlarmSounds
import com.yeon.todaymorning.alarm.VibrationPatterns
import com.yeon.todaymorning.domain.model.EVERYDAY
import com.yeon.todaymorning.domain.model.MissionTransitType
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
    val context = LocalContext.current

    var alarmHour by rememberSaveable(savedSettings.alarmHour, savedSettings.alarmMinute) { mutableIntStateOf(savedSettings.alarmHour) }
    var alarmMinute by rememberSaveable(savedSettings.alarmHour, savedSettings.alarmMinute) { mutableIntStateOf(savedSettings.alarmMinute) }
    var targetHour by rememberSaveable(savedSettings.targetHour, savedSettings.targetMinute) { mutableIntStateOf(savedSettings.targetHour) }
    var targetMinute by rememberSaveable(savedSettings.targetHour, savedSettings.targetMinute) { mutableIntStateOf(savedSettings.targetMinute) }

    var showAlarmPicker by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }
    var showSoundPicker by remember { mutableStateOf(false) }
    var showVibrationPicker by remember { mutableStateOf(false) }

    // 휴대폰 시스템 알람음 선택기 결과 → content:// URI 저장
    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) viewModel.setPickedRingtone(uri.toString())
            else viewModel.setAlarmSound(AlarmSounds.DEFAULT_ID)  // "기본음" 선택 시
        }
        // 다이얼로그는 닫지 않고 유지 → 방금 고른 "최근 선택한 알람"이 바로 보인다
    }
    fun launchRingtonePicker() {
        val existing = savedSettings.alarmSoundId
            .takeIf { it.isNotBlank() && !AlarmSounds.isBuiltIn(it) }
            ?.let { Uri.parse(it) }
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "알람음 선택")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing)
        }
        ringtonePicker.launch(intent)
    }

    // 음성 안내(TTS) — 저장된 설정에서 파생. 토글 시 즉시 부분 저장(알람음·진동과 동일 UX).
    val ttsOn = savedSettings.ttsEnabled
    val ttsTimings = savedSettings.ttsTimings
    val ttsLead = savedSettings.ttsLeadMinutes
    fun toggleTtsTiming(minute: Int) {
        val next = if (minute in ttsTimings) ttsTimings - minute else ttsTimings + minute
        viewModel.setTtsSettings(ttsOn, next, ttsLead)
    }
    fun setTtsLead(minute: Int) {
        viewModel.setTtsSettings(ttsOn, ttsTimings, minute)
    }

    var showRepeatDialog by remember { mutableStateOf(false) }

    var showTimeError by remember { mutableStateOf(false) }
    // 저장 시 필수 항목(출근 경로) 누락 안내. null = 정상.
    var validationError by remember { mutableStateOf<String?>(null) }
    var comingSoon by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showTimeError) {
        if (showTimeError) { snackbarHostState.showSnackbar("알람 시각은 목표 탑승 시각보다 빨라야 해요"); showTimeError = false }
    }
    LaunchedEffect(validationError) {
        validationError?.let { snackbarHostState.showSnackbar(it); validationError = null }
    }
    LaunchedEffect(comingSoon) {
        if (comingSoon) { snackbarHostState.showSnackbar("준비 중인 기능이에요"); comingSoon = false }
    }

    Scaffold(
        containerColor = c.appBg,
        topBar = {
            TopAppBar(
                title = { Text("미션 설정", fontWeight = FontWeight.ExtraBold) },
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
                NavRow("🎵", "알람음", AlarmSounds.label(context, savedSettings.alarmSoundId), valueMarquee = true) { showSoundPicker = true }
                RowDivider()
                NavRow("📳", "진동", VibrationPatterns.label(savedSettings.vibrationPatternId)) { showVibrationPicker = true }
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
                        Text("출근 경로 선택", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.primary)
                    }
                }
            }

            // ── 음성 안내 ─────────────────────────────────
            SettingsGroup("음성 안내") {
                ToggleRow("🔊", "음성 안내", ttsOn) { viewModel.setTtsSettings(it, ttsTimings, ttsLead) }
                RowDivider()
                Column(modifier = Modifier.padding(vertical = 14.dp, horizontal = 2.dp)) {
                    Text("미션 음성안내 시작", fontSize = 13.sp, color = c.onVar)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "목표 시각 이만큼 전부터 음성안내가 켜져요",
                        fontSize = 11.5.sp, color = c.onVar
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimingChip("10분 전", ttsLead == 10, Modifier.weight(1f)) { setTtsLead(10) }
                        TimingChip("15분 전", ttsLead == 15, Modifier.weight(1f)) { setTtsLead(15) }
                        TimingChip("20분 전", ttsLead == 20, Modifier.weight(1f)) { setTtsLead(20) }
                    }
                }
                RowDivider()
                Column(modifier = Modifier.padding(vertical = 14.dp, horizontal = 2.dp)) {
                    Text("도착 알림 시작", fontSize = 13.sp, color = c.onVar)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimingChip("10분 전", 10 in ttsTimings, Modifier.weight(1f)) { toggleTtsTiming(10) }
                        TimingChip("5분 전", 5 in ttsTimings, Modifier.weight(1f)) { toggleTtsTiming(5) }
                        TimingChip("3분 전", 3 in ttsTimings, Modifier.weight(1f)) { toggleTtsTiming(3) }
                    }
                }
            }

            // ── 저장 ───────────────────────────────────────
            Surface(
                onClick = {
                    val alarmTotal = alarmHour * 60 + alarmMinute
                    val targetTotal = targetHour * 60 + targetMinute
                    if (alarmTotal >= targetTotal) { showTimeError = true; return@Surface }
                    // 출근 경로 검증 — 정류장/역·노선이 없으면 저장 차단하고 무엇이 빈지 안내.
                    if (savedSettings.missionTransitType == MissionTransitType.NONE ||
                        savedSettings.missionStopId.isBlank()
                    ) {
                        validationError = "출근 경로의 정류장·역을 먼저 선택해 주세요"; return@Surface
                    }
                    if (savedSettings.missionRoutes.isEmpty()) {
                        validationError = "출근 경로의 노선을 먼저 선택해 주세요"; return@Surface
                    }
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
                    Text("알람 저장", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = c.onPrimary)
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
    if (showSoundPicker) {
        AlarmSoundDialog(
            currentId = savedSettings.alarmSoundId,
            lastPickedId = savedSettings.lastPickedSoundId,
            onPick = { id -> viewModel.setAlarmSound(id) },
            onPickFromPhone = { launchRingtonePicker() },
            onDismiss = { showSoundPicker = false }
        )
    }
    if (showVibrationPicker) {
        VibrationPatternDialog(
            currentId = savedSettings.vibrationPatternId,
            onConfirm = { id -> viewModel.setVibrationPattern(id); showVibrationPicker = false },
            onDismiss = { showVibrationPicker = false }
        )
    }
}

/* ─────────────────────── 진동 패턴 선택 다이얼로그 ─────────────────────── */

@Composable
private fun VibrationPatternDialog(
    currentId: String,
    onConfirm: (String) -> Unit,    // 확인 눌렀을 때만 최종 저장
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // 임시 선택값 — 확인을 눌러야 commit 된다(요일반복 다이얼로그와 동일).
    var selected by remember { mutableStateOf(currentId) }

    // 미리보기 진동 — 다이얼로그가 닫히면 반드시 정리.
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    fun stopPreview() = runCatching { vibrator.cancel() }
    fun preview(id: String) {
        stopPreview()
        val pattern = VibrationPatterns.find(id)?.waveform ?: return  // OFF 면 진동 안 함
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))  // 다이얼로그 동안 반복
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
        }
    }
    DisposableEffect(Unit) { onDispose { stopPreview() } }

    AlertDialog(
        onDismissRequest = { stopPreview(); onDismiss() },
        confirmButton = {
            TextButton(onClick = { stopPreview(); onConfirm(selected) }) { Text("확인", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = { stopPreview(); onDismiss() }) { Text("취소") }
        },
        title = { Text("진동", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                VibrationPatterns.PATTERNS.forEach { def ->
                    SoundRow(def.label, selected == def.id) {
                        selected = def.id
                        preview(def.id)   // 탭하면 그 진동을 바로 느껴볼 수 있게
                    }
                }
            }
        }
    )
}

/* ─────────────────────── 알람음 선택 다이얼로그 ─────────────────────── */

@Composable
private fun AlarmSoundDialog(
    currentId: String,
    lastPickedId: String,            // 휴대폰에서 마지막으로 고른 알람음 ("최근 선택한 알람"으로 항상 표시)
    onPick: (String) -> Unit,        // 기본/내장 음원 선택 → 즉시 저장
    onPickFromPhone: () -> Unit,     // 휴대폰 시스템 알람음 선택기 실행
    onDismiss: () -> Unit
) {
    val c = AppTheme.colors
    val context = LocalContext.current

    // 미리듣기 플레이어 — 다이얼로그가 닫히면 반드시 정리
    val previewHolder = remember { mutableStateOf<MediaPlayer?>(null) }
    val previewAfd = remember { mutableStateOf<AssetFileDescriptor?>(null) }  // prepare 동안 열려 있어야 함
    fun stopPreview() {
        previewHolder.value?.let { runCatching { it.stop(); it.release() } }
        previewHolder.value = null
        previewAfd.value?.let { runCatching { it.close() } }
        previewAfd.value = null
    }
    fun playPreview(id: String) {
        stopPreview()
        runCatching {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
            )
            val builtIn = AlarmSounds.findBuiltIn(id)
            when {
                builtIn != null -> {
                    val afd = context.resources.openRawResourceFd(builtIn.resId)
                    previewAfd.value = afd  // stopPreview 시점까지 닫지 않는다
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
                id == AlarmSounds.DEFAULT_ID || id.isBlank() ->
                    AlarmSounds.defaultAlarmUri()?.let { mp.setDataSource(context, it) }
                else -> mp.setDataSource(context, Uri.parse(id))
            }
            mp.setOnPreparedListener { it.start() }
            mp.prepareAsync()
            previewHolder.value = mp
        }
    }
    DisposableEffect(Unit) { onDispose { stopPreview() } }

    // 표시할 라디오 행: 기본 + 내장 음원들
    val rows = buildList {
        add(AlarmSounds.DEFAULT_ID to "기본 알람음")
        AlarmSounds.BUILT_INS.forEach { add(AlarmSounds.builtInId(it.key) to it.label) }
    }
    // 휴대폰에서 고른 적이 있으면 그 음원을 "최근 선택한 알람"으로 항상 표시(현재 선택이 아니어도 유지)
    val hasRecent = lastPickedId.isNotBlank()
    val recentTitle = remember(lastPickedId) {
        if (hasRecent) AlarmSounds.label(context, lastPickedId) else ""
    }

    AlertDialog(
        onDismissRequest = { stopPreview(); onDismiss() },
        confirmButton = {
            TextButton(onClick = { stopPreview(); onDismiss() }) { Text("닫기", fontWeight = FontWeight.Bold) }
        },
        title = { Text("알람음", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                rows.forEach { (id, label) ->
                    val selected = currentId == id || (id == AlarmSounds.DEFAULT_ID && currentId.isBlank())
                    SoundRow(label, selected) { onPick(id); playPreview(id) }
                }
                Spacer(Modifier.height(10.dp))
                // "휴대폰 알람음에서 고르기" + "최근 선택한 알람"을 같은 박스에 묶는다.
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .border(1.5.dp, c.primary, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                            .clickable { stopPreview(); onPickFromPhone() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📱 휴대폰 알람음에서 고르기", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.primary)
                    }
                    // 휴대폰에서 고른 알람음은 "최근 선택한 알람"으로 박스 안에 항상 남는다.
                    if (hasRecent) {
                        RowDivider()
                        val recentSelected = currentId == lastPickedId
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onPick(lastPickedId); playPreview(lastPickedId) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = recentSelected,
                                onClick = { onPick(lastPickedId); playPreview(lastPickedId) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("최근 선택한 알람", fontSize = 11.5.sp, color = c.onVar)
                                Text(recentTitle, fontSize = 15.sp, color = c.on)
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun SoundRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(vertical = 6.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, fontSize = 15.sp, color = c.on, modifier = Modifier.weight(1f))
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NavRow(
    emoji: String,
    title: String,
    value: String,
    valueStrong: Boolean = false,
    valueMarquee: Boolean = false,   // 값이 길면 한 줄로 천천히 흐르게(슬라이드). 제목 줄바꿈 방지.
    onClick: () -> Unit
) {
    val c = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 14.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        // 제목은 항상 한 줄 고정 (긴 값에 밀려 줄바꿈되지 않도록)
        Text(title, fontSize = 15.sp, color = c.on, maxLines = 1)
        Text(
            text = value,
            fontSize = if (valueStrong) 16.sp else 14.sp,
            fontWeight = if (valueStrong) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (valueStrong) c.primary else c.onVar,
            maxLines = 1,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f)
                .then(if (valueMarquee) Modifier.basicMarquee() else Modifier)
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

/** Material3 입력형(TimeInput) 시각 선택 다이얼로그 — 시계 다이얼 대신 숫자 직접 입력(2026-07-12 변경) */
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
        text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimeInput(state = state) } },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("확인") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
