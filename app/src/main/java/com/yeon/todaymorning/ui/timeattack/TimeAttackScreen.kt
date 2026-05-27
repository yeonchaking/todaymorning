package com.yeon.todaymorning.ui.timeattack

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yeon.todaymorning.domain.model.MissionState
import com.yeon.todaymorning.domain.model.TransitArrival
import com.yeon.todaymorning.domain.model.TransitType
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeAttackScreen(
    onMissionComplete: (isSuccess: Boolean) -> Unit,
    viewModel: TimeAttackViewModel = hiltViewModel()
) {
    val arrivals by viewModel.arrivals.collectAsState()
    val missionState by viewModel.missionState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val settings by viewModel.settings.collectAsState()

    // 미션 종료 시 결과 화면으로 이동
    LaunchedEffect(missionState) {
        if (missionState == MissionState.Success || missionState == MissionState.Failed) {
            kotlinx.coroutines.delay(600L) // 상태 변화를 잠깐 보여주고 이동
            onMissionComplete(missionState == MissionState.Success)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("오늘도출근 🚌") },
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
            // ── 카운트다운 타이머 ───────────────────────────
            item {
                CountdownCard(
                    remainingSeconds = remainingSeconds,
                    targetHour = settings.targetHour,
                    targetMinute = settings.targetMinute,
                    missionState = missionState
                )
            }

            // ── 미션 결과 ──────────────────────────────────
            if (missionState == MissionState.Success || missionState == MissionState.Failed) {
                item {
                    MissionResultCard(missionState = missionState)
                }
            }

            // ── 탑승 완료 버튼 ─────────────────────────────
            if (missionState == MissionState.Active) {
                item {
                    Button(
                        onClick = { viewModel.onBoardingSuccess() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "✅ 탑승 완료!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── 도착 정보 헤더 ─────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "실시간 도착 정보",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = { viewModel.fetchArrivals() }) {
                            Text("새로고침")
                        }
                    }
                }
            }

            // ── 오류 메시지 ────────────────────────────────
            if (errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // ── 도착 카드 목록 ─────────────────────────────
            if (arrivals.isEmpty() && !isLoading && errorMessage == null) {
                item {
                    Text(
                        text = "도착 정보를 불러오는 중...",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(arrivals) { arrival ->
                ArrivalCard(arrival = arrival)
            }
        }
    }
}

@Composable
private fun CountdownCard(
    remainingSeconds: Long,
    targetHour: Int,
    targetMinute: Int,
    missionState: MissionState
) {
    val isOverdue = remainingSeconds < 0
    val absSeconds = abs(remainingSeconds)
    val hours = absSeconds / 3600
    val minutes = (absSeconds % 3600) / 60
    val seconds = absSeconds % 60

    val timeText = if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }

    val containerColor = when {
        missionState == MissionState.Success -> MaterialTheme.colorScheme.primaryContainer
        missionState == MissionState.Failed -> MaterialTheme.colorScheme.errorContainer
        isOverdue -> MaterialTheme.colorScheme.errorContainer
        remainingSeconds < 300 -> MaterialTheme.colorScheme.tertiaryContainer // 5분 미만: 경고
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "목표 탑승 시각 %02d:%02d".format(targetHour, targetMinute),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    missionState == MissionState.Success -> "✅ 탑승 성공!"
                    missionState == MissionState.Failed -> "❌ 시간 초과"
                    isOverdue -> "⚠️ +$timeText 초과"
                    else -> timeText
                },
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (missionState == MissionState.Active && !isOverdue) {
                Text(
                    text = "남은 시간",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MissionResultCard(missionState: MissionState) {
    val isSuccess = missionState == MissionState.Success
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSuccess) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = if (isSuccess) "🎉 오늘도 출근 성공! 잠시 후 돌아갑니다." else "😢 내일은 꼭 성공해요! 잠시 후 돌아갑니다.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun ArrivalCard(arrival: TransitArrival) {
    val isUrgent = arrival.arrivalSeconds in 0..180 // 3분 이내

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUrgent) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 타입 배지
                    Surface(
                        color = if (arrival.type == TransitType.BUS)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = if (arrival.type == TransitType.BUS) "버스" else "지하철",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = arrival.routeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "→ ${arrival.destination}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = arrival.arrivalMessage,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isUrgent) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
                if (isUrgent) {
                    Text(
                        text = "⚡ 빨리!",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
