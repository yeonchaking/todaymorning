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
import com.yeon.todaymorning.ui.result.MissionResultScreen
import com.yeon.todaymorning.ui.routeselect.RouteSelectScreen
import com.yeon.todaymorning.ui.timeattack.TimeAttackScreen

object Routes {
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
    onAlarmConsumed: () -> Unit = {}
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

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN
    ) {
        composable(Routes.MAIN) {
            MainScreen(
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
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
                    val current = settingsViewModel.settings.value
                    settingsViewModel.saveSettings(
                        current.copy(
                            homeLat = homeLat,
                            homeLng = homeLng,
                            homeAddress = homeAddress
                        )
                    )
                    handle["home_address"] = ""
                }
            }
            LaunchedEffect(workAddress) {
                if (workAddress.isNotBlank()) {
                    val current = settingsViewModel.settings.value
                    settingsViewModel.saveSettings(
                        current.copy(
                            workLat = workLat,
                            workLng = workLng,
                            workAddress = workAddress
                        )
                    )
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
                    val current = settingsViewModel.settings.value
                    settingsViewModel.saveSettings(
                        current.copy(
                            missionTransitType = MissionTransitType.BUS,
                            missionStopId = result.arsId,
                            missionStopName = result.stopName,
                            missionRoutes = result.routes
                        )
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
