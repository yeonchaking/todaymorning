package com.yeon.todaymorning.ui.timeattack

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.yeon.todaymorning.domain.model.MissionState
import com.yeon.todaymorning.domain.model.TransitArrival
import com.yeon.todaymorning.ui.theme.AppTheme
import kotlin.math.abs

private val HeroSuccess = Color(0xFF1B7D33)
private val HeroAmber = Color(0xFFB5740A)
private val HeroDanger = Color(0xFFC5291C)

private data class Signal(val color: Color, val emoji: String, val text: String)

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
    val refreshCountdown by viewModel.refreshCountdown.collectAsState()
    val c = AppTheme.colors

    // 1.0 릴리즈: 플로팅 위젯 비활성화 (P2 재활성화 시 해제 — Manifest 권한도 함께 복원할 것)
    // // 플로팅 위젯: 토글 상태는 settings 에서, 오버레이 권한은 주기적으로 재확인(설정 다녀온 뒤 반영).
    // val context = LocalContext.current
    // var canOverlay by remember { mutableStateOf(android.provider.Settings.canDrawOverlays(context)) }
    // LaunchedEffect(Unit) {
    //     while (true) {
    //         canOverlay = android.provider.Settings.canDrawOverlays(context)
    //         kotlinx.coroutines.delay(1500)
    //     }
    // }

    LaunchedEffect(missionState) {
        if (missionState == MissionState.Success || missionState == MissionState.Failed) {
            kotlinx.coroutines.delay(600L)
            onMissionComplete(missionState == MissionState.Success)
        }
    }

    // 신호등 상태
    val signal = when {
        missionState == MissionState.Success -> Signal(HeroSuccess, "✅", "탑승 성공!")
        missionState == MissionState.Failed -> Signal(HeroDanger, "😢", "미션 실패")
        remainingSeconds <= 0 -> Signal(HeroDanger, "⏰", "시간이 지났어요")
        remainingSeconds < 300 -> Signal(HeroDanger, "🏃", "지금 출발하세요")
        remainingSeconds < 600 -> Signal(HeroAmber, "🚶", "곧 출발하세요")
        else -> Signal(HeroSuccess, "✅", "여유 있어요")
    }

    // 막차: 목표시각 전(잔여시간 내)에 도착하는 버스 중 가장 늦게 오는 것.
    val lastBoardableSeconds = remember(arrivals, remainingSeconds) {
        arrivals.map { it.arrivalSeconds }
            .filter { it in 0..remainingSeconds.toInt() }
            .maxOrNull()
    }

    Column(modifier = Modifier.fillMaxSize().background(c.appBg)) {

        // ── 히어로 (신호등) ──────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(signal.color)
                .statusBarsPadding()
                .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "목표 탑승 %02d:%02d까지".format(settings.targetHour, settings.targetMinute),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.92f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatCountdown(remainingSeconds),
                fontSize = 72.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 18.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(signal.emoji, fontSize = 20.sp)
                Text(signal.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // ── 중간 스크롤 영역 ─────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            // 실시간 도착
            Surface(shape = RoundedCornerShape(20.dp), color = c.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 13.dp, bottom = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("실시간 도착", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.on)
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(c.success))
                                Text("LIVE", fontSize = 12.sp, color = c.onVar)
                            }
                        }
                    }
                    if (arrivals.isEmpty() && errorMessage == null) {
                        Text(
                            text = if (isLoading) "도착 정보를 불러오는 중..." else "도착 정보가 없습니다",
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            textAlign = TextAlign.Center, fontSize = 13.sp, color = c.onVar
                        )
                    }
                    arrivals.forEach { arrival ->
                        ArrivalRow(
                            arrival = arrival,
                            isLast = lastBoardableSeconds != null && arrival.arrivalSeconds == lastBoardableSeconds
                        )
                    }
                }
            }

            // 오류
            if (errorMessage != null) {
                Surface(shape = RoundedCornerShape(14.dp), color = c.dangerCtr, modifier = Modifier.fillMaxWidth()) {
                    Text(errorMessage!!, modifier = Modifier.padding(12.dp), color = c.onDangerCtr, fontSize = 13.sp)
                }
            }

            // 음성 안내 (TTS) — TODO(미구현): 실제 TTS 연동 전 표시용
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.primaryCtr).padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🔊", fontSize = 21.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text("음성 안내 켜짐", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.onPrimaryCtr)
                    Text("10 · 5 · 3분 전 알림", fontSize = 12.5.sp, color = c.onPrimaryCtr.copy(alpha = 0.85f))
                }
            }

            // 1.0 릴리즈: 플로팅 위젯 비활성화 (P2 재활성화 시 해제)
            // // 플로팅 위젯 토글 — 미션 중 다른 앱 위에 작은 위젯 표시 on/off
            // Column(
            //     modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(horizontal = 16.dp, vertical = 11.dp)
            // ) {
            //     Row(
            //         modifier = Modifier.fillMaxWidth(),
            //         verticalAlignment = Alignment.CenterVertically,
            //         horizontalArrangement = Arrangement.spacedBy(12.dp)
            //     ) {
            //         Text("🪟", fontSize = 21.sp)
            //         Column(modifier = Modifier.weight(1f)) {
            //             Text("플로팅 위젯", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.on)
            //             Text(
            //                 text = when {
            //                     !settings.floatingWidgetEnabled -> "꺼짐"
            //                     !canOverlay -> "권한 필요 · 눌러서 허용"
            //                     else -> "화면 위에 미션 표시 중"
            //                 },
            //                 fontSize = 12.5.sp,
            //                 color = if (settings.floatingWidgetEnabled && !canOverlay) c.danger else c.onVar,
            //                 modifier = Modifier.clickable(enabled = settings.floatingWidgetEnabled && !canOverlay) {
            //                     val intent = android.content.Intent(
            //                         android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            //                         android.net.Uri.parse("package:${context.packageName}")
            //                     ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            //                     context.startActivity(intent)
            //                 }
            //             )
            //         }
            //         Switch(
            //             checked = settings.floatingWidgetEnabled,
            //             onCheckedChange = { on ->
            //                 viewModel.setFloatingWidget(on)
            //                 if (on && !canOverlay) {
            //                     val intent = android.content.Intent(
            //                         android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            //                         android.net.Uri.parse("package:${context.packageName}")
            //                     ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            //                     context.startActivity(intent)
            //                 }
            //             }
            //         )
            //     }
            //
            //     // 투명도 슬라이더 — 위젯 켜졌을 때만
            //     if (settings.floatingWidgetEnabled) {
            //         var opacityLocal by remember(settings.floatingWidgetOpacity) {
            //             mutableFloatStateOf(settings.floatingWidgetOpacity.toFloat())
            //         }
            //         Spacer(Modifier.height(6.dp))
            //         Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            //             Text("투명도", fontSize = 12.5.sp, color = c.onVar)
            //             Slider(
            //                 value = opacityLocal,
            //                 onValueChange = { opacityLocal = it },
            //                 onValueChangeFinished = { viewModel.setFloatingWidgetOpacity(opacityLocal.toInt()) },
            //                 valueRange = 30f..100f,
            //                 modifier = Modifier.weight(1f)
            //             )
            //             Text("${opacityLocal.toInt()}%", fontSize = 12.5.sp, color = c.on, fontWeight = FontWeight.Bold)
            //         }
            //     }
            // }

            // 새로고침 행
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { viewModel.fetchArrivals() }.padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔄 ", fontSize = 13.sp)
                Text(
                    text = if (isLoading) "갱신 중..." else "방금 갱신 · ${refreshCountdown}초 후 자동",
                    fontSize = 12.5.sp, color = c.onVar
                )
            }
        }

        // ── 하단 액션 ────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().background(c.appBg).navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)
        ) {
            when {
                missionState == MissionState.Active && remainingSeconds > 0 -> {
                    Surface(
                        onClick = { viewModel.onBoardingSuccess() },
                        shape = RoundedCornerShape(18.dp),
                        color = c.primary,
                        modifier = Modifier.fillMaxWidth().height(60.dp)
                    ) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Text("✅", fontSize = 24.sp)
                            Spacer(Modifier.width(10.dp))
                            Text("탑승 완료", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = c.onPrimary)
                        }
                    }
                }
                missionState == MissionState.Active -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("미션에 성공하셨나요?", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.on)
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { viewModel.onMissionFail() }, modifier = Modifier.weight(1f)) { Text("❌ 실패") }
                            Button(onClick = { viewModel.onBoardingSuccess() }, modifier = Modifier.weight(1f)) { Text("✅ 성공") }
                        }
                    }
                }
                else -> {
                    Text(
                        text = if (missionState == MissionState.Success) "🎉 오늘도 출근 성공! 잠시 후 돌아갑니다." else "😢 내일은 꼭 성공해요! 잠시 후 돌아갑니다.",
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        textAlign = TextAlign.Center, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.on
                    )
                }
            }
        }
    }
}

@Composable
private fun ArrivalRow(arrival: TransitArrival, isLast: Boolean) {
    val c = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(c.surface2).padding(horizontal = 11.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text("🚌", fontSize = 14.sp)
            Text(arrival.routeName, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = c.on)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(arrival.destination, fontSize = 13.5.sp, color = c.on, maxLines = 1)
            if (isLast) {
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(c.amberCtr).padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("⚡", fontSize = 11.sp)
                    Text("막차 · 이 버스까지", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = c.onAmberCtr)
                }
            }
        }
        Text(
            text = arrival.arrivalMessage,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (arrival.arrivalSeconds in 0..180) c.danger else c.on
        )
    }
}

private fun formatCountdown(remainingSeconds: Long): String {
    val s = abs(remainingSeconds)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    val body = if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
    return if (remainingSeconds < 0) "-$body" else body
}
