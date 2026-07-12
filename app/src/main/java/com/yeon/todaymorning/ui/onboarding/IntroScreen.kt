package com.yeon.todaymorning.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeon.todaymorning.data.datastore.UserSettingsDataStore
import com.yeon.todaymorning.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 첫 실행 1회만 보이는 앱 소개 온보딩 (2026-07-12).
 * 흐름: IntroScreen(여기) → PermissionOnboardingScreen(권한 게이트) → 메인.
 * NavGraph 가 DataStore `hasSeenIntro` 를 보고 시작 화면을 고른다 — 알람으로 열린 경우
 * (fromAlarm)는 이 화면을 건너뛴다(미션이 소개 화면에 가로막히면 안 되므로).
 */

private data class IntroPage(
    val emoji: String,
    val accentEmoji: String,
    val title: String,
    val description: String
)

private val pages = listOf(
    IntroPage(
        emoji = "⏰",
        accentEmoji = "🔥",
        title = "매일 아침이 타임어택",
        description = "알람이 울리면 출근 미션 시작!\n목표 시각 안에 버스를 타면 성공이에요"
    ),
    IntroPage(
        emoji = "🚌",
        accentEmoji = "📢",
        title = "실시간 도착 보며 여유있게",
        description = "내 정류장 버스가 언제 오는지 실시간으로,\n도착 10·5·3분 전엔 음성으로 알려드려요"
    ),
    IntroPage(
        emoji = "🗓️",
        accentEmoji = "✅",
        title = "성공이 쌓이면 습관이 돼요",
        description = "출근 기록이 달력에 차곡차곡.\n먼저 출근 경로부터 설정해 볼까요?"
    )
)

@HiltViewModel
class IntroViewModel @Inject constructor(
    private val dataStore: UserSettingsDataStore
) : ViewModel() {
    /** hasSeenIntro 저장 후 콜백 — 저장 완료 전에 화면을 떠나 플래그가 유실되는 것 방지. */
    fun markSeenAnd(onDone: () -> Unit) {
        viewModelScope.launch {
            dataStore.saveHasSeenIntro()
            onDone()
        }
    }
}

@Composable
fun IntroScreen(
    onFinish: () -> Unit,
    viewModel: IntroViewModel = hiltViewModel()
) {
    val c = AppTheme.colors
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Scaffold(containerColor = c.appBg) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── 상단: 건너뛰기 (마지막 장에서는 숨김 — 시작하기로 유도) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { viewModel.markSeenAnd(onFinish) },
                    enabled = !isLast,
                    modifier = Modifier.scale(if (isLast) 0f else 1f)
                ) {
                    Text("건너뛰기", fontSize = 14.sp, color = c.onVar)
                }
            }

            // ── 페이저 ──
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { index ->
                IntroPageContent(page = pages[index])
            }

            // ── 인디케이터 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.forEachIndexed { index, _ ->
                    val selected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (selected) 26.dp else 8.dp,
                        animationSpec = spring(dampingRatio = 0.8f),
                        label = "dotWidth"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (selected) c.primary else c.trackOff,
                        label = "dotColor"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── 하단 버튼: 다음 → 시작하기 ──
            Button(
                onClick = {
                    if (isLast) {
                        viewModel.markSeenAnd(onFinish)
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.primary,
                    contentColor = c.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp)
            ) {
                Text(
                    text = if (isLast) "시작하기" else "다음",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun IntroPageContent(page: IntroPage) {
    val c = AppTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 히어로: 이중 원 + 메인 이모지, 우상단 액센트 이모지 배지
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(176.dp)
                    .clip(CircleShape)
                    .background(c.surface2)
            )
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(c.primaryCtr),
                contentAlignment = Alignment.Center
            ) {
                Text(page.emoji, fontSize = 62.sp)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(c.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(page.accentEmoji, fontSize = 26.sp)
            }
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text = page.title,
            fontSize = 23.sp,
            fontWeight = FontWeight.ExtraBold,
            color = c.on,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = page.description,
            fontSize = 15.sp,
            color = c.onVar,
            textAlign = TextAlign.Center,
            lineHeight = 23.sp
        )
    }
}
