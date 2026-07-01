package com.yeon.todaymorning.domain.model

/** 미션 대상 대중교통 유형 */
enum class MissionTransitType { NONE, BUS, SUBWAY }

/** 미션 대상 노선 하나 (한 정류장/역에서 여러 개 선택 가능) */
data class MissionRoute(
    val routeId: String,      // 버스: busRouteId / 지하철: 호선 ID
    val routeName: String,    // 표시용: "651" / "2호선"
    val direction: String     // 방면
)

/**
 * 요일 표현은 java.util.Calendar 상수를 그대로 쓴다 (일=1 … 토=7).
 * 기본값은 평일(월~금).
 */
val WEEKDAYS: Set<Int> = setOf(
    java.util.Calendar.MONDAY,
    java.util.Calendar.TUESDAY,
    java.util.Calendar.WEDNESDAY,
    java.util.Calendar.THURSDAY,
    java.util.Calendar.FRIDAY
)
val EVERYDAY: Set<Int> = (java.util.Calendar.SUNDAY..java.util.Calendar.SATURDAY).toSet()
val WEEKEND: Set<Int> = setOf(java.util.Calendar.SATURDAY, java.util.Calendar.SUNDAY)

data class UserSettings(
    val alarmHour: Int = 7,
    val alarmMinute: Int = 0,
    val targetHour: Int = 9,
    val targetMinute: Int = 0,

    // ── 알람 활성화 / 반복 요일 ──────────────────────
    val alarmEnabled: Boolean = true,
    val repeatDays: Set<Int> = WEEKDAYS,   // Calendar 요일값 집합. 빈 집합 = 반복 없음

    // ── 알람음 선택 ───────────────────────────────────
    // "" = 폰 기본 알람음, "builtin:<key>" = 내장 음원, 그 외 = 시스템 ringtone content:// URI
    // (규칙·해석은 com.yeon.todaymorning.alarm.AlarmSounds)
    val alarmSoundId: String = "",
    // 휴대폰에서 마지막으로 고른 시스템 알람음 URI. 현재 선택이 기본/내장이어도
    // "최근 선택한 알람"으로 다이얼로그에 계속 표시하기 위해 별도 보관. "" = 고른 적 없음.
    val lastPickedSoundId: String = "",

    // ── 진동 패턴 선택 ────────────────────────────────
    // VibrationPatterns.PATTERNS 의 id 중 하나. "off" = 진동 없음, 기본값 "basic"(1초 패턴).
    // (규칙·해석은 com.yeon.todaymorning.alarm.VibrationPatterns)
    val vibrationPatternId: String = "basic",

    // ── 음성 안내(TTS) ────────────────────────────────
    // ttsEnabled = 전체 on/off. ttsTimings = 목표 시각까지 남은 '분' 기준 안내 시점 집합.
    // 예: {10,5,3} = 출발 10·5·3분 전에 다음 차편을 음성으로 읽음.
    // (발화 문장·재생은 com.yeon.todaymorning.alarm.TtsManager, 트리거는 TimeAttackViewModel)
    val ttsEnabled: Boolean = true,
    val ttsTimings: Set<Int> = setOf(10, 5, 3),
    // 미션 음성안내가 '열리는' 시점 — 목표 시각까지 남은 분이 이 값 이하가 돼야 발화 시작.
    // (ttsTimings 가 '각 차편 도착 N분 전'이라면, ttsLeadMinutes 는 '미션 전체 음성안내 시작 시점'.)
    val ttsLeadMinutes: Int = 15,

    // ── 플로팅 위젯 ───────────────────────────────────
    // 미션 진행 중 다른 앱 위에 작은 위젯(남은시간+다음버스)을 띄울지. 미션 화면 토글로 제어.
    // 실제 표시는 SYSTEM_ALERT_WINDOW 권한이 있어야 가능(없으면 조용히 미표시).
    val floatingWidgetEnabled: Boolean = true,
    // 위젯 불투명도(%) 30~100. 작을수록 투명. 위젯 전체(배경+글자)에 적용.
    val floatingWidgetOpacity: Int = 90,

    // ── 집 위치 ───────────────────────────────────────
    val homeLat: Double = 0.0,
    val homeLng: Double = 0.0,
    val homeAddress: String = "",

    // ── 회사 위치 ─────────────────────────────────────
    val workLat: Double = 0.0,
    val workLng: Double = 0.0,
    val workAddress: String = "",

    // ── 미션 타겟 (지도에서 첫 정류장/역 + 다수 노선 직접 선택) ──────
    val missionTransitType: MissionTransitType = MissionTransitType.NONE,
    val missionStopId: String = "",            // 버스: arsId / 지하철: 역 이름
    val missionStopName: String = "",          // 표시용: "강남역"
    val missionRoutes: List<MissionRoute> = emptyList(),  // 선택한 노선들 (아무거나 타면 성공)

    // ── 개발자모드 (히든) ─────────────────────────────
    // 메인 화면 타이틀 10연속 탭으로 on/off. 진단용 토스트 등 개발용 기능의 게이트.
    val isDevMode: Boolean = false
) {
    val hasHomeLocation: Boolean get() = homeLat != 0.0 && homeLng != 0.0
    val hasWorkLocation: Boolean get() = workLat != 0.0 && workLng != 0.0
    val hasMissionTarget: Boolean
        get() = missionTransitType != MissionTransitType.NONE &&
                missionStopId.isNotBlank() &&
                missionRoutes.isNotEmpty()

    /** 요약 표시용: "651, 388" */
    val missionRoutesLabel: String get() = missionRoutes.joinToString(", ") { it.routeName }

    /** 알람이 실제로 동작해야 하는가 — 마스터 스위치 ON 이고 반복 요일이 하나라도 있어야 한다. */
    val alarmActive: Boolean get() = alarmEnabled && repeatDays.isNotEmpty()

    /** 요약 표시용: "평일" / "매일" / "주말" / "월·수·금" / "반복 없음" */
    val repeatDaysLabel: String
        get() = when (repeatDays) {
            WEEKDAYS -> "평일"
            EVERYDAY -> "매일"
            WEEKEND -> "주말"
            emptySet<Int>() -> "반복 없음"
            else -> {
                val order = listOf(
                    java.util.Calendar.MONDAY to "월",
                    java.util.Calendar.TUESDAY to "화",
                    java.util.Calendar.WEDNESDAY to "수",
                    java.util.Calendar.THURSDAY to "목",
                    java.util.Calendar.FRIDAY to "금",
                    java.util.Calendar.SATURDAY to "토",
                    java.util.Calendar.SUNDAY to "일"
                )
                order.filter { it.first in repeatDays }.joinToString("·") { it.second }
            }
        }
}
