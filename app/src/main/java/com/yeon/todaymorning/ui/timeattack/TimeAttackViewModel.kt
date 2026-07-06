package com.yeon.todaymorning.ui.timeattack

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeon.todaymorning.alarm.MissionService
import com.yeon.todaymorning.data.datastore.UserSettingsDataStore
import com.yeon.todaymorning.domain.model.MissionState
import com.yeon.todaymorning.domain.model.TransitArrival
import com.yeon.todaymorning.domain.model.UserSettings
import com.yeon.todaymorning.mission.MissionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 타임어택 화면의 얇은 어댑터.
 *
 * 미션 로직(폴링·카운트다운·TTS·기록)은 전부 [MissionEngine] 이 화면과 독립적으로 굴린다.
 * 이 ViewModel 은 (1) 진입 시 [MissionService] 를 띄워 엔진이 백그라운드에서도 살아 있게 하고,
 * (2) 엔진의 StateFlow 를 화면에 그대로 노출하며, (3) 사용자 액션을 엔진으로 forward 한다.
 *
 * 화면을 떠나도 미션은 계속된다 — onCleared 에서 서비스/엔진을 멈추지 않는다.
 * 미션 종료(사용자가 직접 성공/실패를 선택하는 시점)는 엔진이 판단하고 서비스가 스스로 내려간다.
 * 목표 시각이 지나도 그 자체로는 종료되지 않는다 — [MissionEngine] 문서 참고.
 */
@HiltViewModel
class TimeAttackViewModel @Inject constructor(
    private val engine: MissionEngine,
    private val dataStore: UserSettingsDataStore,
    @ApplicationContext appContext: Context
) : ViewModel() {

    val settings: StateFlow<UserSettings> = dataStore.userSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings()
    )

    val arrivals: StateFlow<List<TransitArrival>> = engine.arrivals
    val missionState: StateFlow<MissionState> = engine.missionState
    val isLoading: StateFlow<Boolean> = engine.isLoading
    val errorMessage: StateFlow<String?> = engine.errorMessage
    val remainingSeconds: StateFlow<Long> = engine.remainingSeconds
    val refreshCountdown: StateFlow<Int> = engine.refreshCountdown

    init {
        // 화면 진입 = 미션 시작. 서비스가 엔진을 호스팅해 화면이 꺼져도 계속 돈다.
        // 이미 진행 중이면 start()/엔진 모두 멱등(no-op).
        MissionService.start(appContext)
    }

    fun fetchArrivals() = engine.fetchArrivals()

    fun onBoardingSuccess() = engine.onBoardingSuccess()

    fun onMissionFail() = engine.onMissionFail()

    /** 플로팅 위젯 on/off. 서비스가 DataStore 를 구독하므로 1초 내 반영된다. */
    fun setFloatingWidget(enabled: Boolean) {
        viewModelScope.launch { dataStore.saveFloatingWidget(enabled) }
    }

    /** 플로팅 위젯 불투명도(%) 30~100. 서비스가 DataStore 를 구독해 1초 내 반영. */
    fun setFloatingWidgetOpacity(opacity: Int) {
        viewModelScope.launch { dataStore.saveFloatingWidgetOpacity(opacity) }
    }
}
