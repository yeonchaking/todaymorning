package com.yeon.todaymorning.ui.onboarding

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yeon.todaymorning.alarm.AlarmScheduler
import com.yeon.todaymorning.ui.theme.AppTheme

/** 온보딩 한 단계(권한 하나)를 표시하는 데 필요한 정보. */
private data class OnboardingStep(
    val emoji: String,
    val title: String,
    val description: String,
    val granted: Boolean,
    val onRequest: () -> Unit
)

/**
 * 앱 시작 시 권한 게이트.
 *
 * 정확한 알람 · 전체화면 표시(Android 14+) · 알림(Android 13+) 셋 다 **하드 게이트**다.
 * 스킵 버튼이 없다 — 하나라도 허용하지 않으면 다음 단계로 넘어가지 않는다.
 * 이 화면 자체는 NavGraph가 "권한이 하나라도 없을 때"만 시작 화면으로 고른다
 * (앱을 새로 열 때마다 재평가되므로, 나중에 설정에서 권한을 도로 껐다면 다음 실행 때 다시 걸린다).
 *
 * 정확한 알람 · 전체화면 표시는 시스템이 결과 콜백을 주지 않는 "설정 화면 이동" 방식이라,
 * 화면이 ON_RESUME 될 때마다 실제 권한 상태를 다시 읽어 반영한다.
 */
@Composable
fun PermissionOnboardingScreen(onAllGranted: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val c = AppTheme.colors

    var exactAlarmGranted by remember { mutableStateOf(canScheduleExactAlarms(context)) }
    var fullScreenGranted by remember { mutableStateOf(canUseFullScreenIntent(context)) }
    var notificationGranted by remember { mutableStateOf(hasNotificationPermission(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                exactAlarmGranted = canScheduleExactAlarms(context)
                fullScreenGranted = canUseFullScreenIntent(context)
                notificationGranted = hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationGranted = granted || hasNotificationPermission(context) }

    val steps = buildList {
        add(
            OnboardingStep(
                emoji = "⏰",
                title = "정확한 알람",
                description = "이 권한이 없으면 출근 알람이 정확한 시각에 울리지 않아요.",
                granted = exactAlarmGranted,
                onRequest = { openExactAlarmSettings(context) }
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            add(
                OnboardingStep(
                    emoji = "📱",
                    title = "전체화면 표시",
                    description = "알람이 울릴 때 잠금화면 위로 타임어택 화면을 바로 띄우는 데 필요해요.",
                    granted = fullScreenGranted,
                    onRequest = { openFullScreenIntentSettings(context) }
                )
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(
                OnboardingStep(
                    emoji = "🔔",
                    title = "알림",
                    description = "미션 진행 상황과 도착 정보를 알림으로 보여주는 데 필요해요.",
                    granted = notificationGranted,
                    onRequest = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                )
            )
        }
    }

    val currentIndex = steps.indexOfFirst { !it.granted }

    LaunchedEffect(currentIndex) {
        if (currentIndex == -1) onAllGranted()
    }

    // 모든 권한이 이미 허용된 순간 — onAllGranted 호출 후 화면 전환까지 잠깐 빈 화면.
    if (currentIndex == -1) return

    val current = steps[currentIndex]

    Scaffold(containerColor = c.appBg) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${currentIndex + 1} / ${steps.size}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = c.primary
            )
            Spacer(Modifier.height(20.dp))
            Text(current.emoji, fontSize = 48.sp)
            Spacer(Modifier.height(20.dp))
            Text(
                text = "${current.title} 권한이 필요해요",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = c.on,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = current.description,
                fontSize = 14.sp,
                color = c.onVar,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = current.onRequest,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.primary,
                    contentColor = c.onPrimary
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("허용하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "이 권한을 허용해야 다음 단계로 진행돼요",
                fontSize = 12.sp,
                color = c.onVar,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 온보딩이 게이트하는 권한 3종(정확한 알람 · 전체화면 인텐트 · 알림)이 전부 허용됐는지.
 * NavGraph가 시작 화면(온보딩 vs 메인)을 고를 때 재사용한다.
 */
fun allOnboardingPermissionsGranted(context: Context): Boolean =
    canScheduleExactAlarms(context) && canUseFullScreenIntent(context) && hasNotificationPermission(context)

private fun canScheduleExactAlarms(context: Context): Boolean =
    AlarmScheduler(context).canScheduleExactAlarms()

private fun canUseFullScreenIntent(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
    val nm = context.getSystemService(NotificationManager::class.java)
    return nm?.canUseFullScreenIntent() ?: true
}

private fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.startActivity(
            Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:${context.packageName}")
            )
        )
    }
}

private fun openFullScreenIntentSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                Uri.parse("package:${context.packageName}")
            )
        )
    }
}
