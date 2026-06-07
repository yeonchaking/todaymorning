package com.yeon.todaymorning.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yeon.todaymorning.ui.busselect.BusSelectScreen
import com.yeon.todaymorning.ui.result.MissionResultScreen
import com.yeon.todaymorning.ui.timeattack.TimeAttackScreen

object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
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
    // fromAlarm이 true가 될 때마다 타임어택으로 이동, 소비 후 리셋
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
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onPickBus = { navController.navigate(Routes.BUS_SELECT) },
                resultHandle = backStackEntry.savedStateHandle
            )
        }

        composable(Routes.BUS_SELECT) {
            BusSelectScreen(
                onPicked = { result ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("bus_arsId", result.arsId)
                        set("bus_stopName", result.stopName)
                        set("bus_routeName", result.routeName)
                        set("bus_direction", result.direction)
                        set("bus_routeId", result.routeId) // 마지막에 set → 수신 트리거
                    }
                    navController.popBackStack()
                },
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
