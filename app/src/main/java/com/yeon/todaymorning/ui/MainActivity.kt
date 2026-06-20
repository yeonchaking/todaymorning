package com.yeon.todaymorning.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.yeon.todaymorning.alarm.AlarmScheduler
import com.yeon.todaymorning.data.db.MissionRecord
import com.yeon.todaymorning.domain.model.MissionTransitType
import com.yeon.todaymorning.domain.model.UserSettings
import com.yeon.todaymorning.ui.main.MainViewModel
import com.yeon.todaymorning.ui.main.MissionCalendar
import com.yeon.todaymorning.ui.theme.AppTheme
import com.yeon.todaymorning.ui.theme.TodayCommuteTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_FROM_ALARM = "extra_from_alarm"
    }

    private lateinit var alarmScheduler: AlarmScheduler  // kept for future use
    private var fromAlarm by mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 결과 무시 — UI에서 배너로 처리 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13+ 알림 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        alarmScheduler = AlarmScheduler(this)
        fromAlarm = intent.getBooleanExtra(EXTRA_FROM_ALARM, false)

        // Android 14+: USE_FULL_SCREEN_INTENT 권한 별도 허용 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = getSystemService(NotificationManager::class.java)
            if (!nm.canUseFullScreenIntent()) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }

        enableEdgeToEdge()
        setContent {
            TodayCommuteTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    fromAlarm = fromAlarm,
                    onAlarmConsumed = { fromAlarm = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_FROM_ALARM, false)) {
            fromAlarm = true
        }
    }
}

@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    onStartTimeAttack: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scheduler = remember { AlarmScheduler(context) }
    val showPermissionBanner by remember { mutableStateOf(!scheduler.canScheduleExactAlarms()) }

    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val c = AppTheme.colors

    // TODO(미구현): 알람 ON/OFF는 현재 화면 로컬 상태. DataStore 저장 + 실제 스케줄 해제/등록 연동 필요.
    var alarmOn by rememberSaveable { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(c.appBg)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 22.dp)
    ) {
        // 상단바
        item { TopHeader(onSettings = onNavigateToSettings) }

        // 권한 경고 배너
        if (showPermissionBanner) {
            item {
                PermissionBanner(
                    onOpenSettings = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // 알람 마스터 스위치
        item {
            MasterSwitchCard(
                alarmOn = alarmOn,
                onToggle = { alarmOn = it },
                settings = settings,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 오늘의 미션 카드
        item {
            MissionCardV2(
                settings = settings,
                enabled = alarmOn,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 실시간 도착 CTA (알람 켜졌고 미션 있을 때)
        if (alarmOn && settings.hasMissionTarget) {
            item {
                NextBusCta(
                    onClick = onStartTimeAttack,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // 이번 달 성공 통계(정보성)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🗓️", fontSize = 16.sp)
                Text(
                    text = "이번 달 출근 성공 ",
                    fontSize = 13.5.sp,
                    color = c.onVar
                )
                Text(
                    text = "${uiState.monthSuccessCount}회",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = c.on
                )
            }
        }

        // 달력 (연속성공 헤더 없이 + 범례)
        item {
            MissionCalendar(
                streak = uiState.streak,
                records = uiState.allRecords,
                showStreakHeader = false,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 최근 기록
        if (uiState.recentRecords.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("최근 기록", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.on)
                    val selectedCount = uiState.selectedIds.size
                    when {
                        !uiState.isEditMode ->
                            TextButton(onClick = { viewModel.toggleEditMode() }) { Text("편집") }
                        selectedCount > 0 ->
                            TextButton(onClick = { viewModel.deleteSelected() }) {
                                Text("${selectedCount}개 삭제", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                            }
                        else ->
                            TextButton(onClick = { viewModel.toggleEditMode() }) { Text("취소") }
                    }
                }
            }
            items(uiState.recentRecords, key = { it.id }) { record ->
                MissionRecordItem(
                    record = record,
                    isEditMode = uiState.isEditMode,
                    isSelected = record.id in uiState.selectedIds,
                    onToggleSelect = { viewModel.toggleSelection(record.id) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "아직 기록이 없어요\n알람을 설정하고 첫 출근을 해봐요! 🚌",
                        fontSize = 14.sp,
                        color = c.onVar,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/* ───────────────────────────── 상단바 ───────────────────────────── */

@Composable
private fun TopHeader(onSettings: () -> Unit) {
    val c = AppTheme.colors
    Surface(color = c.surface, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 8.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("오늘도출근", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = c.on)
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = "설정", tint = c.onVar)
            }
        }
    }
}

/* ───────────────────────── 마스터 스위치 ───────────────────────── */

@Composable
private fun MasterSwitchCard(
    alarmOn: Boolean,
    onToggle: (Boolean) -> Unit,
    settings: UserSettings,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    val alarmText = "%02d:%02d".format(settings.alarmHour, settings.alarmMinute)
    AppCard(modifier = modifier, padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(c.primaryCtr),
                contentAlignment = Alignment.Center
            ) { Text("⏰", fontSize = 23.sp) }
            Column(modifier = Modifier.weight(1f)) {
                Text("출근 알람", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.on)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = if (alarmOn) "내일 $alarmText · 평일 반복" else "알람 꺼짐",
                        fontSize = 13.sp,
                        color = c.onVar
                    )
                }
            }
            Switch(checked = alarmOn, onCheckedChange = onToggle)
        }
    }
}

/* ───────────────────────── 오늘의 미션 ───────────────────────── */

@Composable
private fun MissionCardV2(
    settings: UserSettings,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    val today = remember { LocalDate.now() }
    val dateText = remember(today) {
        "${today.monthValue}월 ${today.dayOfMonth}일 " +
            today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)
    }
    val alarmText = "%02d:%02d".format(settings.alarmHour, settings.alarmMinute)
    val targetText = "%02d:%02d".format(settings.targetHour, settings.targetMinute)
    val transitEmoji = when (settings.missionTransitType) {
        MissionTransitType.BUS -> "🚌"
        MissionTransitType.SUBWAY -> "🚇"
        else -> "🚉"
    }

    AppCard(modifier = modifier.then(if (enabled) Modifier else Modifier.alpha(0.5f)), padding = 18.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("오늘의 미션", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.primary)
            Text(dateText, fontSize = 12.sp, color = c.onVar)
        }
        Spacer(Modifier.height(16.dp))

        // 기상 → 탑승
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⏰ 기상", fontSize = 12.sp, color = c.onVar)
                Spacer(Modifier.height(4.dp))
                Text(alarmText, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = c.on)
            }
            Text("→", fontSize = 22.sp, color = c.onVar, modifier = Modifier.padding(bottom = 4.dp))
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🚌 탑승", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.primary)
                Spacer(Modifier.height(4.dp))
                Text(targetText, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = c.primary)
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = c.outlineSoft)
        Spacer(Modifier.height(14.dp))

        // 정류장 + 노선
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("📍", fontSize = 18.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = settings.missionStopName.ifBlank { "출근 경로 미설정" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.on,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (settings.hasMissionTarget) "설정 > 출근 경로에서 변경" else "설정에서 정류장·노선을 골라주세요",
                    fontSize = 12.sp,
                    color = c.onVar
                )
            }
            if (settings.hasMissionTarget) {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(c.primary).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(transitEmoji, fontSize = 14.sp)
                    Text(settings.missionRoutesLabel, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = c.onPrimary, maxLines = 1)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        // 반복 요일 (현재는 평일 고정 표시 — TODO: 실제 요일 설정 연동)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("반복", fontSize = 12.sp, color = c.onVar)
            val days = listOf("월", "화", "수", "목", "금", "토", "일")
            days.forEachIndexed { idx, d ->
                val active = idx <= 4 // 평일
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (active) c.primaryCtr else c.surface2)
                        .then(if (active) Modifier else Modifier.alpha(0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = d,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = if (active) c.onPrimaryCtr else c.onVar
                    )
                }
            }
        }
    }
}

/* ───────────────────────── 도착 CTA ───────────────────────── */

@Composable
private fun NextBusCta(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.successCtr)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(c.success))
        Text(
            text = "실시간 도착 정보 확인하기",
            fontSize = 14.sp,
            color = c.onSuccessCtr,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text("›", fontSize = 18.sp, color = c.onSuccessCtr)
    }
}

/* ───────────────────────── 공통 ───────────────────────── */

@Composable
private fun AppCard(
    modifier: Modifier = Modifier,
    padding: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = AppTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = c.surface,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

@Composable
private fun PermissionBanner(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⚠️ 정확한 알람 권한이 필요해요",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onOpenSettings) { Text("설정에서 허용하기") }
        }
    }
}

@Composable
private fun MissionRecordItem(
    record: MissionRecord,
    isEditMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    val isSuccess = record.isSuccess
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = c.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isEditMode) Modifier.clickable { onToggleSelect() } else Modifier)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isEditMode) {
                Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() }, modifier = Modifier.size(20.dp))
                }
            }
            Text(text = if (isSuccess) "✅" else "❌", fontSize = 20.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(record.date, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.on)
                Text("알람 ${record.alarmTime} · 목표 ${record.targetTime}", fontSize = 12.sp, color = c.onVar)
            }
            Text(
                text = if (isSuccess) "성공" else "실패",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSuccess) c.success else c.danger
            )
        }
    }
}
