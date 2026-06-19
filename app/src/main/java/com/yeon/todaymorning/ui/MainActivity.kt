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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.yeon.todaymorning.domain.model.BadgeUi
import com.yeon.todaymorning.domain.model.DayStatus
import com.yeon.todaymorning.domain.model.LevelInfo
import com.yeon.todaymorning.domain.model.MissionTransitType
import com.yeon.todaymorning.domain.model.UserSettings
import com.yeon.todaymorning.ui.main.MainViewModel
import com.yeon.todaymorning.ui.main.MissionCalendar
import com.yeon.todaymorning.ui.theme.AppTheme
import com.yeon.todaymorning.ui.theme.TodayCommuteTheme
import dagger.hilt.android.AndroidEntryPoint

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
        // 앱이 이미 실행 중일 때 알람이 오면 STATE 업데이트 → recomposition 트리거
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(c.appBg)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(13.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── 그라디언트 히어로 헤더 (스트릭 링 + 레벨/포인트) ──────────
        item {
            StreakHero(
                streak = uiState.streak,
                level = uiState.level,
                onSettings = onNavigateToSettings
            )
        }

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
                    modifier = Modifier.padding(horizontal = 15.dp)
                )
            }
        }

        // 오늘의 출근 카드 + 타임어택 시작
        item {
            TodayCommuteCard(
                settings = settings,
                todayLabel = uiState.todayLabel,
                onStartTimeAttack = onStartTimeAttack,
                modifier = Modifier.padding(horizontal = 15.dp)
            )
        }

        // 이번 주 트래커
        item {
            WeeklyTrackerCard(
                weekly = uiState.weekly,
                weekSuccess = uiState.weekSuccess,
                weekTotal = uiState.weekTotal,
                modifier = Modifier.padding(horizontal = 15.dp)
            )
        }

        // 획득 배지
        item {
            BadgesCard(
                badges = uiState.badges,
                modifier = Modifier.padding(horizontal = 15.dp)
            )
        }

        // 월별 기록 달력 (streak 헤더는 히어로에 있으므로 생략)
        item {
            MissionCalendar(
                streak = uiState.streak,
                records = uiState.allRecords,
                showStreakHeader = false,
                modifier = Modifier.padding(horizontal = 15.dp)
            )
        }

        // 최근 기록 헤더 + 편집/삭제
        if (uiState.recentRecords.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 21.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "최근 기록",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = c.on
                    )
                    val selectedCount = uiState.selectedIds.size
                    when {
                        !uiState.isEditMode ->
                            TextButton(onClick = { viewModel.toggleEditMode() }) { Text("편집") }
                        selectedCount > 0 ->
                            TextButton(onClick = { viewModel.deleteSelected() }) {
                                Text(
                                    text = "${selectedCount}개 삭제",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
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
                    modifier = Modifier.padding(horizontal = 15.dp)
                )
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "아직 기록이 없어요\n알람을 설정하고 첫 출근을 해봐요! 🚌",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onVar,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/* ───────────────────────────── 히어로 ───────────────────────────── */

@Composable
private fun StreakHero(
    streak: Int,
    level: LevelInfo,
    onSettings: () -> Unit
) {
    val c = AppTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
            .background(Brush.linearGradient(c.headerGradient))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 타이틀 줄
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "오늘도출근",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.95f)
                )
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "설정", tint = Color.White)
                }
            }

            // 스트릭 링
            StreakRing(streak = streak, progress = level.progress)

            Text(
                text = "${streak}일 연속 출근 성공!",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            // Lv / 포인트 칩
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroChip(text = "🎖️ Lv.${level.level}")
                HeroChip(text = "%,dP".format(level.points))
            }

            // 진행 바
            Column(
                modifier = Modifier.width(210.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.22f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(level.progress.coerceIn(0f, 1f))
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFD54A))
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = level.pointsToNext?.let { "다음 레벨까지 ${it}P" } ?: "최고 레벨 달성! 👑",
                    fontSize = 10.5.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun StreakRing(streak: Int, progress: Float) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(152.dp)) {
        Canvas(modifier = Modifier.size(152.dp)) {
            val sw = 14.dp.toPx()
            val arcSize = Size(size.width - sw, size.height - sw)
            val topLeft = Offset(sw / 2, sw / 2)
            // 트랙
            drawArc(
                color = Color.White.copy(alpha = 0.22f),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(sw, cap = StrokeCap.Round)
            )
            // 진행
            drawArc(
                color = Color(0xFFFFD54A),
                startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f), useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(sw, cap = StrokeCap.Round)
            )
        }
        Box(
            modifier = Modifier
                .size(124.dp)
                .clip(CircleShape)
                .background(Color(0xFF1B49C0)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🔥", fontSize = 20.sp)
                Text(
                    text = "$streak",
                    fontSize = 46.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "DAYS STREAK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun HeroChip(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 11.dp, vertical = 4.dp)
    )
}

/* ─────────────────────────── 오늘의 출근 ─────────────────────────── */

@Composable
private fun TodayCommuteCard(
    settings: UserSettings,
    todayLabel: String,
    onStartTimeAttack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    val targetText = "%02d:%02d".format(settings.targetHour, settings.targetMinute)
    val transitEmoji = when (settings.missionTransitType) {
        MissionTransitType.BUS -> "🚌"
        MissionTransitType.SUBWAY -> "🚇"
        else -> "🚉"
    }

    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("☀️", fontSize = 18.sp)
                Text("오늘의 출근", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = c.on)
            }
            if (todayLabel.isNotBlank()) {
                Text(todayLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.onVar)
            }
        }

        Spacer(Modifier.height(11.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(targetText, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = c.on)
            Text(" 까지 탑승", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.onVar)
        }

        Spacer(Modifier.height(11.dp))
        if (settings.hasMissionTarget) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(c.lilac)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(transitEmoji, fontSize = 16.sp)
                Text(settings.missionRoutesLabel, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = c.onLilac)
                Text(
                    text = settings.missionStopName,
                    fontSize = 12.sp,
                    color = c.onLilac.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(c.lilac)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text("🚉", fontSize = 16.sp)
                Text("설정에서 출근 경로를 먼저 선택하세요", fontSize = 12.5.sp, color = c.onLilac)
            }
        }

        Spacer(Modifier.height(11.dp))
        Surface(
            onClick = onStartTimeAttack,
            shape = RoundedCornerShape(15.dp),
            color = c.primary,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚡", fontSize = 19.sp)
                Spacer(Modifier.width(8.dp))
                Text("타임어택 시작", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = c.onPrimary)
            }
        }
    }
}

/* ─────────────────────────── 이번 주 ─────────────────────────── */

@Composable
private fun WeeklyTrackerCard(
    weekly: List<DayStatus>,
    weekSuccess: Int,
    weekTotal: Int,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    val labels = listOf("월", "화", "수", "목", "금", "토", "일")

    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("이번 주", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = c.on)
            Text("$weekSuccess/$weekTotal 성공", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.success)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weekly.forEachIndexed { idx, status ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = labels[idx],
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (idx >= 5) Color(0xFFE5746B) else c.onVar
                    )
                    DayDot(status)
                }
            }
        }
    }
}

@Composable
private fun DayDot(status: DayStatus) {
    val c = AppTheme.colors
    when (status) {
        DayStatus.SUCCESS -> Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(c.successCtr),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Check, contentDescription = null, tint = c.success, modifier = Modifier.size(18.dp)) }
        DayStatus.FAIL -> Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(c.dangerCtr),
            contentAlignment = Alignment.Center
        ) { Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.danger) }
        DayStatus.TODAY -> Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).border(2.dp, c.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) { Text("D", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = c.primary) }
        else -> Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(c.surface3),
            contentAlignment = Alignment.Center
        ) { Text("·", fontSize = 12.sp, color = c.onVar) }
    }
}

/* ─────────────────────────── 배지 ─────────────────────────── */

@Composable
private fun BadgesCard(
    badges: List<BadgeUi>,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("획득 배지", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = c.on)
            Text("전체 보기", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.primary)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            badges.forEach { badge -> BadgeChip(badge) }
        }
    }
}

@Composable
private fun BadgeChip(badge: BadgeUi) {
    val c = AppTheme.colors
    val unlockedBrush = Brush.linearGradient(
        when (badge.key) {
            "early_bird" -> listOf(Color(0xFFFF9347), Color(0xFFFF6A3D))
            "streak7" -> listOf(Color(0xFFFFC93C), Color(0xFFFF9A3C))
            "perfect_week" -> listOf(Color(0xFF5A8CFF), Color(0xFF3F5BD0))
            else -> listOf(Color(0xFF9A6CFF), Color(0xFF6A3FD0))
        }
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = if (badge.unlocked) Modifier else Modifier.alpha(0.4f)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .then(
                    if (badge.unlocked) Modifier.background(unlockedBrush)
                    else Modifier.background(c.surface3)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = if (badge.unlocked) badge.emoji else "🔒", fontSize = 24.sp)
        }
        Text(badge.label, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = c.onVar)
    }
}

/* ─────────────────────────── 공통 ─────────────────────────── */

@Composable
private fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = AppTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = c.surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun PermissionBanner(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (isEditMode) Modifier.clickable { onToggleSelect() } else Modifier)
            .background(if (isSuccess) c.surface else c.dangerCtr.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (isEditMode) {
                Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(text = if (isSuccess) "✅" else "❌", fontSize = 18.sp)
            Column {
                Text(text = record.date, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.on)
                Text(
                    text = "알람 ${record.alarmTime} · 목표 ${record.targetTime}",
                    fontSize = 11.sp,
                    color = c.onVar
                )
            }
        }
        Text(
            text = if (isSuccess) record.boardedTime ?: "성공" else "실패",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSuccess) c.success else c.danger
        )
    }
}
