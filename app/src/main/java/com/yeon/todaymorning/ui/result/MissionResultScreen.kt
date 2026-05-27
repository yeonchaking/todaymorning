package com.yeon.todaymorning.ui.result

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.*
import com.yeon.todaymorning.domain.model.UserLevel
import kotlinx.coroutines.delay

@Composable
fun MissionResultScreen(
    isSuccess: Boolean,
    onNavigateToMain: () -> Unit,
    viewModel: MissionResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 뒤로가기 누르면 TimeAttack으로 돌아가지 않고 Main으로 이동
    BackHandler { onNavigateToMain() }

    // 배경 색상 — 성공: 초록 그라데이션 느낌, 실패: 부드러운 에러 컨테이너
    val bgColor = if (isSuccess)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.errorContainer

    // 카드·텍스트 등장 애니메이션 트리거
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(200L)
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // ── Lottie 애니메이션 ───────────────────────────
            LottieResultAnim(isSuccess = isSuccess)

            // ── 결과 타이틀 ────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 2 }
            ) {
                Text(
                    text = if (isSuccess) "🎉 탑승 성공!" else "😢 다음엔 꼭!",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSuccess)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }

            // ── 레벨 + 스트릭 카드 ─────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 2 }
            ) {
                LevelCard(
                    isSuccess = isSuccess,
                    streak = uiState.streak,
                    level = uiState.level,
                    daysToNext = uiState.daysToNext
                )
            }

            // ── 돌아가기 버튼 ──────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(900))
            ) {
                Button(
                    onClick = onNavigateToMain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSuccess)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "메인으로",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun LottieResultAnim(isSuccess: Boolean) {
    val assetName = if (isSuccess) "lottie_success.json" else "lottie_fail.json"
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(assetName))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        isPlaying = true
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.size(200.dp)
    )
}

@Composable
private fun LevelCard(
    isSuccess: Boolean,
    streak: Int,
    level: UserLevel,
    daysToNext: Int?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 레벨 배지
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = level.emoji, fontSize = 28.sp)
                Text(
                    text = level.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 레벨 설명
            Text(
                text = level.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // 스트릭 표시
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ResultStatItem(
                    emoji = if (streak >= 7) "🔥" else if (streak >= 3) "⭐" else "📅",
                    value = "${streak}일",
                    label = "연속 성공"
                )
                if (daysToNext != null) {
                    ResultStatItem(
                        emoji = "⬆️",
                        value = "${daysToNext}일",
                        label = "다음 레벨까지"
                    )
                } else {
                    ResultStatItem(
                        emoji = "👑",
                        value = "최고",
                        label = "레벨 달성!"
                    )
                }
            }

            // 동기부여 메시지
            if (isSuccess && streak > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = motivationMessage(streak),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultStatItem(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 22.sp)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun motivationMessage(streak: Int): String = when {
    streak >= 60 -> "두 달 연속! 당신은 진짜 출근 레전드예요 👑"
    streak >= 30 -> "한 달 연속! 이 습관, 평생 갑니다 💎"
    streak >= 14 -> "2주 연속! 완전히 습관이 됐어요 🥇"
    streak >= 7  -> "일주일 연속! 이번 주도 완벽했어요 🥈"
    streak >= 3  -> "${streak}일 연속! 좋은 흐름이에요 🥉"
    streak == 1  -> "오늘 첫 성공! 내일도 화이팅 🌱"
    else         -> "잘하고 있어요!"
}
