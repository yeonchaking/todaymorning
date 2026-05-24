package com.yeon.todaymorning.ui

import android.Manifest
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
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.yeon.todaymorning.alarm.AlarmScheduler
import com.yeon.todaymorning.ui.theme.TodayCommuteTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_FROM_ALARM = "extra_from_alarm"
    }

    private lateinit var alarmScheduler: AlarmScheduler  // kept for future use

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
        val fromAlarm = intent.getBooleanExtra(EXTRA_FROM_ALARM, false)

        enableEdgeToEdge()
        setContent {
            TodayCommuteTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController, fromAlarm = fromAlarm)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    fromAlarm: Boolean,
    onNavigateToSettings: () -> Unit
) {
    var alarmFired by remember { mutableStateOf(fromAlarm) }
    val context = LocalContext.current
    val scheduler = remember { AlarmScheduler(context) }
    var showPermissionBanner by remember { mutableStateOf(!scheduler.canScheduleExactAlarms()) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "오늘도출근",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Phase 1 — 알람 테스트",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(40.dp))

            // 권한 없을 때 경고 배너
            if (showPermissionBanner) {
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
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                }
                            }
                        ) {
                            Text("설정에서 허용하기")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (alarmFired) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✅ 알람 수신 성공!",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Button(
                onClick = {
                    if (!scheduler.canScheduleExactAlarms()) {
                        showPermissionBanner = true
                        return@Button
                    }
                    alarmFired = false
                    scheduler.scheduleAfterSeconds(5)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("5초 후 알람 테스트")
            }
        }
    }
}
