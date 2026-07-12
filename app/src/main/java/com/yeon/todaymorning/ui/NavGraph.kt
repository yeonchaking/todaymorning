package com.yeon.todaymorning.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yeon.todaymorning.domain.model.MissionTransitType
import com.yeon.todaymorning.ui.busselect.BusSelectScreen
import com.yeon.todaymorning.ui.locationpicker.LocationPickerScreen
import com.yeon.todaymorning.ui.onboarding.IntroScreen
import com.yeon.todaymorning.ui.onboarding.PermissionOnboardingScreen
import com.yeon.todaymorning.ui.onboarding.allOnboardingPermissionsGranted
import com.yeon.todaymorning.ui.result.MissionResultScreen
import com.yeon.todaymorning.ui.routeselect.RouteSelectScreen
import com.yeon.todaymorning.ui.timeattack.TimeAttackScreen
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object Routes {
    const val INTRO = "intro"            // 첫 실행 1회 앱 소개
    const val ONBOARDING = "onboarding"  // 권한 게이트
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val HOME_PICKER = "home_picker"
    const val WORK_PICKER = "work_picker"
    const val ROUTE_SELECT = "route_select"   // T-map 경로탐색 (보류, 추후 사용)
    const val BUS_SELECT = "bus_select"
    const val TIME_ATTACK = "time_attack"
    const val RESULT = "result/{isSuccess}"

    fun result(isSuccess: Boolean) = "result/$isSuccess"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    fromAlarm: Boolean = false,
    onAlarmConsumed: () -> Unit = {},
    showIntro: Boolean = false
) {
    LaunchedEffect(fromAlarm) {
        if (fromAlarm) {
            navController.navigate(Routes.TIME_ATTACK) {
                popUpTo(Routes.MAIN) { inclusive = false }
                launchSingleTop = true
            }
            onAlarmConsumed()
        }
    }

    // 시작 화면 우선순위 (2026-07-12 소개 온보딩 추가):
    //  1) 알람으로 열림 → MAIN (소개/권한이 미션을 가로막으면 안 됨 — 기존 흐름 유지)
    //  2) 첫 실행(hasSeenIntro=false, MainActivity가 스플래시 중에 읽어 전달) → INTRO
    //  3) 권한 3종(정확한 알람·전체화면 인텐트·알림) 미비 → ONBOARDING (하드 게이트, 매 실행 재평가)
    //  4) 그 외 → MAIN
    val context = LocalContext.current
    val startDestination = remember {
        when {
            fromAlarm -> Routes.MAIN
            showIntro -> Routes.INTRO
            !allOnboardingPermissionsGranted(context) -> Routes.ONBOARDING
            else -> Routes.MAIN
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.INTRO) {
            IntroScreen(
                onFinish = {
                    // 소개 완료 → 권한이 이미 다 있으면 메인, 아니면 권한 게이트로.
                    val next = if (allOnboardingPermissionsGranted(context)) Routes.MAIN else Routes.ONBOARDING
                    navController.navigate(next) {
                        popUpTo(Routes.INTRO) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ONBOARDING) {
            PermissionOnboardingScreen(
                onAllGranted = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onStartTimeAttack = { navController.navigate(Routes.TIME_ATTACK) }
            )
        }

        composable(Routes.SETTINGS) { backStackEntry ->
            val handle = backStackEntry.savedStateHandle

            // 집 위치 결과 수신
            val homeLat by handle.getStateFlow("home_lat", 0.0).collectAsState()
            val homeLng by handle.getStateFlow("home_lng", 0.0).collectAsState()
            val homeAddress by handle.getStateFlow("home_address", "").collectAsState()

            // 회사 위치 결과 수신
            val workLat by handle.getStateFlow("work_lat", 0.0).collectAsState()
            val workLng by handle.getStateFlow("work_lng", 0.0).collectAsState()
            val workAddress by handle.getStateFlow("work_address", "").collectAsState()

            val settingsViewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()

            // 위치 변경 시 자동 저장
            LaunchedEffect(homeAddress) {
                if (homeAddress.isNotBlank()) {
                    // 시각 키를 건드리지 않는 부분 저장 — 편집 중인 시각 보존
                    settingsViewModel.saveHomeLocation(homeLat, homeLng, homeAddress)
                    handle["home_address"] = ""
                }
            }
            LaunchedEffect(workAddress) {
                if (workAddress.isNotBlank()) {
                    settingsViewModel.saveWorkLocation(workLat, workLng, workAddress)
                    handle["work_address"] = ""
                }
            }

            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onPickHome = { navController.navigate(Routes.HOME_PICKER) },
                onPickWork = { navController.navigate(Routes.WORK_PICKER) },
                onFindRoute = { navController.navigate(Routes.BUS_SELECT) },
                viewModel = settingsViewModel
            )
        }

        composable(Routes.HOME_PICKER) {
            val settingsViewModel: SettingsViewModel =
                androidx.hilt.navigation.compose.hiltViewModel(
                    navController.getBackStackEntry(Routes.SETTINGS)
                )
            val currentSettings = settingsViewModel.settings.value

            LocationPickerScreen(
                title = "집 위치 설정",
                initialLat = currentSettings.homeLat,
                initialLng = currentSettings.homeLng,
                initialAddress = currentSettings.homeAddress,
                onConfirm = { result ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("home_lat", result.lat)
                        set("home_lng", result.lng)
                        set("home_address", result.address)
                    }
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.WORK_PICKER) {
            val settingsViewModel: SettingsViewModel =
                androidx.hilt.navigation.compose.hiltViewModel(
                    navController.getBackStackEntry(Routes.SETTINGS)
                )
            val currentSettings = settingsViewModel.settings.value

            LocationPickerScreen(
                title = "회사 위치 설정",
                initialLat = currentSettings.workLat,
                initialLng = currentSettings.workLng,
                initialAddress = currentSettings.workAddress,
                onConfirm = { result ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("work_lat", result.lat)
                        set("work_lng", result.lng)
                        set("work_address", result.address)
                    }
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 첫 대중교통(정류장 + 다수 노선) 직접 선택
        composable(Routes.BUS_SELECT) {
            val settingsViewModel: SettingsViewModel =
                androidx.hilt.navigation.compose.hiltViewModel(
                    navController.getBackStackEntry(Routes.SETTINGS)
                )
            BusSelectScreen(
                onPicked = { result ->
                    // 시각 키를 건드리지 않는 부분 저장 — 편집 중인 시각 보존
                    settingsViewModel.saveMissionTarget(
                        transitType = MissionTransitType.BUS,
                        stopId = result.arsId,
                        stopName = result.stopName,
                        routes = result.routes
                    )
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        // T-map 자동 경로탐색 (보류 — 추후 유료 플랜 시 재연결)
        composable(Routes.ROUTE_SELECT) {
            RouteSelectScreen(
                onRouteSelected = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TIME_ATTACK) {
            TimeAttackScreen(
                onMissionComplete = { isSuccess ->
                    navController.navigate(Routes.result(isSuccess)) {
                        popUpTo(Routes.TIME_ATTACK) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.RESULT,
            arguments = listOf(
                navArgument("isSuccess") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val isSuccess = backStackEntry.arguments?.getBoolean("isSuccess") ?: false
            MissionResultScreen(
                isSuccess = isSuccess,
                onNavigateToMain = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
