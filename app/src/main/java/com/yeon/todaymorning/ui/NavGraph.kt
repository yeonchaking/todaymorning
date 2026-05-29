package com.yeon.todaymorning.ui

import androidx.compose.runtime.Composable
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
    fromAlarm: Boolean = false
) {
    val startDestination = if (fromAlarm) Routes.TIME_ATTACK else Routes.MAIN

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.MAIN) {
            MainScreen(
                fromAlarm = false,
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
