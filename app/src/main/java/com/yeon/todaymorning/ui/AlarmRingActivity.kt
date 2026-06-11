package com.yeon.todaymorning.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.yeon.todaymorning.alarm.AlarmRingService
import com.yeon.todaymorning.ui.theme.Blue40
import com.yeon.todaymorning.ui.theme.LightBlue40
import com.yeon.todaymorning.ui.theme.TodayCommuteTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 알람이 울리는 동안 잠금화면 위에 뜨는 전용 전체화면 화면.
 * 슬라이드 투 디스미스로 해제하면 알람음을 끄고 타임어택 화면으로 진입한다.
 */
class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge()
        setContent {
            TodayCommuteTheme {
                AlarmRingScreen(onDismiss = ::onDismiss)
            }
        }
    }

    /** 잠금화면 위로 표시 + 화면 켜기 */
    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
                .requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    /** 슬라이드 해제 시: 알람음 정지 → 타임어택 진입 → 알람화면 종료 */
    private fun onDismiss() {
        AlarmRingService.stop(this)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_FROM_ALARM, true)
        }
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        // 뒤로가기로 알람을 끌 수 없게 막음 — 반드시 슬라이드로 해제
    }
}

@Composable
private fun AlarmRingScreen(onDismiss: () -> Unit) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1000)
        }
    }
    val timeFmt = remember { SimpleDateFormat("a h:mm", Locale.KOREAN) }
    val dateFmt = remember { SimpleDateFormat("M월 d일 EEEE", Locale.KOREAN) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Blue40, LightBlue40))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(80.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "⏰",
                    style = MaterialTheme.typography.displayMedium
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = timeFmt.format(now),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.displayLarge
                )
                Text(
                    text = dateFmt.format(now),
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "출근 시간이에요!\n밀어서 미션을 시작하세요",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            SlideToDismiss(
                onDismiss = onDismiss,
                modifier = Modifier.padding(bottom = 48.dp)
            )
        }
    }
}

/** 끝까지 밀면 onDismiss를 호출하는 슬라이드 컨트롤 */
@Composable
private fun SlideToDismiss(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val thumbSize = 64.dp
    val trackPadding = 6.dp
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val trackPaddingPx = with(density) { trackPadding.toPx() }

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var dismissed by remember { mutableStateOf(false) }

    val maxOffset = (trackWidthPx - thumbSizePx - trackPaddingPx * 2).coerceAtLeast(0f)
    val animatedOffset by animateFloatAsState(targetValue = offsetX, label = "thumb")
    val progress = if (maxOffset > 0f) offsetX / maxOffset else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(RoundedCornerShape(38.dp))
            .background(Color.White.copy(alpha = 0.25f))
            .onSizeChanged { trackWidthPx = it.width.toFloat() },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "밀어서 출근 시작  →",
            color = Color.White.copy(alpha = (1f - progress).coerceIn(0f, 1f)),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Center)
        )

        Box(
            modifier = Modifier
                .padding(trackPadding)
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White)
                .pointerInput(maxOffset) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (!dismissed) {
                                if (offsetX > maxOffset * 0.85f) {
                                    dismissed = true
                                    offsetX = maxOffset
                                    onDismiss()
                                } else {
                                    offsetX = 0f
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount).coerceIn(0f, maxOffset)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "→",
                color = Blue40,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}
