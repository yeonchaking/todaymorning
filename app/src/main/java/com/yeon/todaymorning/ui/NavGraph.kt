package com.yeon.todaymorning.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.yeon.todaymorning.ui.timeattack.TimeAttackScreen

object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val TIME_ATTACK = "time_attack"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    fromAlarm: Boolean = false
) {
    // 알람으로 열렸으면 타임어택 화면을 시작점으로
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
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.TIME_ATTACK) {
            TimeAttackScreen(
                onMissionDone = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.TIME_ATTACK) { inclusive = true }
                    }
                }
            )
        }
    }
}
