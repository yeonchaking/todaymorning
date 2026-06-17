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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.yeon.todaymorning.alarm.AlarmScheduler
import com.yeon.todaymorning.data.db.MissionRecord
import com.yeon.todaymorning.domain.model.MissionTransitType
import com.yeon.todaymorning.domain.model.UserSettings
import com.yeon.todaymorning.ui.main.MainViewModel
import com.yeon.todaymorning.ui.main.MissionCalendar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scheduler = remember { AlarmScheduler(context) }
    var showPermissionBanner by remember { mutableStateOf(!scheduler.canScheduleExactAlarms()) }

    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("오늘도출근") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "설정",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 권한 경고 배너
            if (showPermissionBanner) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "⚠️ 정확한 알람 권한이 필요해요",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                }
                            }) { Text("설정에서 허용하기") }
                        }
                    }
                }
            }

            // 오늘의 미션 카드
            item {
                MissionCard(settings = settings)
            }

            // 미션 기록 달력 (연속 성공 일수 + 월별 성공/실패 마킹)
            item {
                MissionCalendar(
                    streak = uiState.streak,
                    records = uiState.allRecords
                )
            }

            // 최근 기록 헤더 + 편집/삭제 버튼
            if (uiState.recentRecords.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "최근 기록",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val selectedCount = uiState.selectedIds.size
                        when {
                            !uiState.isEditMode -> {
                                TextButton(onClick = { viewModel.toggleEditMode() }) {
                                    Text("편집")
                                }
                            }
                            selectedCount > 0 -> {
                                TextButton(onClick = { viewModel.deleteSelected() }) {
                                    Text(
                                        text = "${selectedCount}개 삭제",
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            else -> {
                                TextButton(onClick = { viewModel.toggleEditMode() }) {
                                    Text("취소")
                                }
                            }
                        }
                    }
                }
                items(uiState.recentRecords, key = { it.id }) { record ->
                    MissionRecordItem(
                        record = record,
                        isEditMode = uiState.isEditMode,
                        isSelected = record.id in uiState.selectedIds,
                        onToggleSelect = { viewModel.toggleSelection(record.id) }
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "아직 기록이 없어요\n알람을 설정하고 첫 출근을 해봐요! 🚌",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionCard(settings: UserSettings) {
    val alarmText = "%02d:%02d".format(settings.alarmHour, settings.alarmMinute)
    val targetText = "%02d:%02d".format(settings.targetHour, settings.targetMinute)

    val transitEmoji = when (settings.missionTransitType) {
        MissionTransitType.BUS -> "🚌"
        MissionTransitType.SUBWAY -> "🚇"
        else -> "🚉"
    }
    val transitWord = when (settings.missionTransitType) {
        MissionTransitType.BUS -> "버스"
        MissionTransitType.SUBWAY -> "지하철"
        else -> "대중교통"
    }

    // 1줄: "강남 에서" (에서 작게) / 2줄: "🚌 651, 388 버스 타기!!"
    val boardingText = buildAnnotatedString {
        if (settings.hasMissionTarget) {
            append(settings.missionStopName)
            withStyle(SpanStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)) {
                append(" 에서")
            }
            append("\n")
            append("$transitEmoji ${settings.missionRoutesLabel} $transitWord 타기!!")
        } else {
            append("$transitEmoji 대중교통 타기!!")
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🎯 오늘의 미션",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            // n시에 기상해서
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = alarmText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "에 기상해서",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            // m시까지
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = targetText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "까지",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            // ○○ 에서 / 노선 타기!!
            Text(
                text = boardingText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                lineHeight = 30.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun MissionRecordItem(
    record: MissionRecord,
    isEditMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {}
) {
    val isSuccess = record.isSuccess
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isEditMode) Modifier.clickable { onToggleSelect() } else Modifier
            )
            .background(
                if (isSuccess) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (isEditMode) {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(text = if (isSuccess) "✅" else "❌", style = MaterialTheme.typography.bodyLarge)
            Column {
                Text(
                    text = record.date,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "알람 ${record.alarmTime} · 목표 ${record.targetTime}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = if (isSuccess) record.boardedTime ?: "성공" else "실패",
            style = MaterialTheme.typography.labelMedium,
            color = if (isSuccess) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold
        )
    }
}
