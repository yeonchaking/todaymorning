package com.yeon.todaymorning.domain.model

sealed class MissionState {
    object Idle : MissionState()
    object Waiting : MissionState()
    object Active : MissionState()
    object Success : MissionState()
    object Failed : MissionState()
}
