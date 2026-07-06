package com.yeon.todaymorning.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.yeon.todaymorning.alarm.AlarmScheduler
import com.yeon.todaymorning.data.db.MissionRecord
import com.yeon.todaymorning.domain.model.MissionTransitType
import com.yeon.todaymorning.domain.model.TransitArrival
import com.yeon.todaymorning.domain.model.TransitType
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

    override fun onCreate(savedInstanceState: Bundle?) {
        // 스플래시 화면(Theme.Todaymorning.Splash) 설치 — super.onCreate() 이전에 호출해야 한다.
        // 로고 교체는 res/drawable/splash_icon.xml 한 파일만 바꾸면 됨(테마·코드 변경 불필요).
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // 권한 요청(정확한 알람·전체화면 인텐트·알림)은 전부 PermissionOnboardingScreen이 담당한다.
        // (NavGraph가 시작화면으로 온보딩을 고르는 기준 — allOnboardingPermissionsGranted)
        alarmScheduler = AlarmScheduler(this)
        fromAlarm = intent.getBooleanExtra(EXTRA_FROM_ALARM, false)

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
    val uiState by viewModel.uiState.collectAsState()
    // 생명주기 인지 수집 — 설정 화면에서 시각을 바꾸고 돌아오면 ON_START 에 재수집해 최신값 반영.
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val arrivalDialog by viewModel.arrivalDialog.collectAsState()
    val c = AppTheme.colors

    if (arrivalDialog.isOpen) {
        ArrivalDialog(
            state = arrivalDialog,
            onRefresh = { viewModel.openArrivalDialog() },
            onDismiss = { viewModel.closeArrivalDialog() }
        )
    }

    // 알람 ON/OFF는 DataStore의 alarmEnabled를 단일 출처로 사용. 토글 시 ViewModel이 저장+스케줄 반영.
    val alarmOn = settings.alarmEnabled

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(c.appBg)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 22.dp)
    ) {
        // 상단바
        item { TopHeader(onSettings = onNavigateToSettings, onTitleTapped = { viewModel.toggleDevMode() }) }

        // 알람 마스터 스위치
        item {
            MasterSwitchCard(
                alarmOn = alarmOn,
                onToggle = { viewModel.setAlarmEnabled(it) },
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

        // 실시간 도착 CTA (미션 설정돼 있으면 알람 ON/OFF 무관하게 노출)
        if (settings.hasMissionTarget) {
            item {
                NextBusCta(
                    onClick = { viewModel.openArrivalDialog() },
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

// 히든 개발자모드 트리거에 필요한 연속 탭 횟수.
private const val DEV_MODE_TAP_COUNT = 10

@Composable
private fun TopHeader(onSettings: () -> Unit, onTitleTapped: () -> Unit = {}) {
    val c = AppTheme.colors
    // 타이틀 연속 탭 카운터 — 화면 상태로만 관리(프로세스 재시작 시 리셋), 10회 도달 시 콜백 후 리셋.
    var tapCount by remember { mutableStateOf(0) }
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
            Text(
                "오늘도출근",
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                color = c.on,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    tapCount++
                    if (tapCount >= DEV_MODE_TAP_COUNT) {
                        tapCount = 0
                        onTitleTapped()
                    }
                }
            )
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
                        text = if (alarmOn) "다음 $alarmText · ${settings.repeatDaysLabel} 반복" else "알람 꺼짐",
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
        // 반복 요일 — 설정의 repeatDays를 단일 출처로 표시.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("반복", fontSize = 12.sp, color = c.onVar)
            val days = listOf(
                "월" to java.util.Calendar.MONDAY,
                "화" to java.util.Calendar.TUESDAY,
                "수" to java.util.Calendar.WEDNESDAY,
                "목" to java.util.Calendar.THURSDAY,
                "금" to java.util.Calendar.FRIDAY,
                "토" to java.util.Calendar.SATURDAY,
                "일" to java.util.Calendar.SUNDAY
            )
            days.forEach { (d, cal) ->
                val active = cal in settings.repeatDays
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

/* ──────────────────── 실시간 도착정보 다이얼로그 ──────────────────── */

@Composable
private fun ArrivalDialog(
    state: com.yeon.todaymorning.ui.main.ArrivalDialogState,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val c = AppTheme.colors
    // 여러 건일 때 현재 보고 있는 인덱스. 목록이 바뀌면 0으로.
    var index by remember(state.arrivals) { mutableStateOf(0) }
    val total = state.arrivals.size
    val current = state.arrivals.getOrNull(index)

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = c.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 헤더
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(c.success))
                        Text("실시간 도착", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.on)
                    }
                    Text("✕", fontSize = 18.sp, color = c.onVar, modifier = Modifier.clickable(onClick = onDismiss))
                }

                Spacer(Modifier.height(18.dp))

                when {
                    state.isLoading -> {
                        Box(Modifier.fillMaxWidth().padding(vertical = 36.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                        }
                    }
                    current == null -> {
                        Text(
                            text = state.errorMessage ?: "도착 정보가 없습니다.",
                            fontSize = 13.5.sp,
                            color = c.onVar,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp)
                        )
                    }
                    else -> {
                        // ◀ [도착카드] ▶
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ArrowButton(
                                symbol = "‹",
                                enabled = index > 0,
                                onClick = { if (index > 0) index-- }
                            )
                            ArrivalCardBody(arrival = current, modifier = Modifier.weight(1f))
                            ArrowButton(
                                symbol = "›",
                                enabled = index < total - 1,
                                onClick = { if (index < total - 1) index++ }
                            )
                        }

                        if (total > 1) {
                            Spacer(Modifier.height(14.dp))
                            Text("${index + 1} / $total", fontSize = 13.sp, color = c.onVar, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    text = if (state.isLoading) "갱신 중..." else "🔄 새로고침",
                    fontSize = 13.sp,
                    color = c.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !state.isLoading, onClick = onRefresh)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ArrowButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    val c = AppTheme.colors
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(c.surface2)
            .alpha(if (enabled) 1f else 0.3f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = c.on)
    }
}

@Composable
private fun ArrivalCardBody(arrival: TransitArrival, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val icon = if (arrival.type == TransitType.SUBWAY) "🚇" else "🚌"
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.surface2)
            .padding(vertical = 22.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(icon, fontSize = 22.sp)
            Text(arrival.routeName, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = c.on)
        }
        if (arrival.destination.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                arrival.destination,
                fontSize = 12.5.sp,
                color = c.onVar,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = arrival.arrivalMessage,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (arrival.arrivalSeconds in 0..180) c.danger else c.primary
        )
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
