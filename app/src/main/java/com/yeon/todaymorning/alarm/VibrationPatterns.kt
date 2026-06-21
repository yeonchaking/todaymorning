package com.yeon.todaymorning.alarm

/**
 * 진동 패턴 카탈로그 + 선택값(`vibrationPatternId`) 해석 단일 지점.
 * 구조는 [AlarmSounds]와 동일하게 "레지스트리 → DataStore에 id 저장 → 서비스가 울릴 때 읽음".
 *
 * `vibrationPatternId` 규칙:
 *  - [PatternDef.id] 중 하나. 등록되지 않은(stale) id 는 [find]에서 null → 호출부가 [DEFAULT_ID]로 폴백.
 *  - `"off"`(= [OFF_ID]) 는 "진동 없음". [waveformOf]가 null 을 돌려준다.
 *
 * `waveform` 규칙은 안드로이드 [android.os.VibrationEffect.createWaveform] 과 동일:
 *  - `[0, on, off, on, off, ...]` (밀리초). 첫 0 = 시작 전 대기 0ms.
 *  - 반복 재생은 호출부에서 repeat index 0 으로 지정(알람: 무한 반복, 미리보기: 반복).
 *
 * ── 패턴을 추가하는 방법 ──────────────────────────────
 *  아래 [PATTERNS] 에 [PatternDef] 한 줄만 추가하면 설정 다이얼로그에 자동 노출된다.
 */
data class PatternDef(
    val id: String,
    val label: String,
    /** null = 진동 없음(OFF). 그 외는 createWaveform 용 밀리초 배열. */
    val waveform: LongArray?
)

object VibrationPatterns {

    const val OFF_ID: String = "off"

    /** 키가 없던 기존 사용자·잘못된 id 의 기본값 — 기존 동작(1초 진동 / 1초 멈춤)과 동일. */
    const val DEFAULT_ID: String = "basic"

    /** 진동 패턴 목록. 여기에 [PatternDef] 한 줄만 추가하면 선택지에 자동 노출된다. */
    val PATTERNS: List<PatternDef> = listOf(
        PatternDef(OFF_ID, "진동 없음", null),
        PatternDef("short", "짧게 (0.5초)", longArrayOf(0, 500, 500)),
        PatternDef("basic", "기본 (1초)", longArrayOf(0, 1000, 1000)),
        PatternDef("long", "길게 (5초)", longArrayOf(0, 5000, 1000)),
        PatternDef("double", "두 번씩", longArrayOf(0, 300, 200, 300, 800))
    )

    /** id 에 해당하는 패턴 정의. 없으면 null(= stale id). */
    fun find(id: String): PatternDef? = PATTERNS.find { it.id == id }

    /** OFF 여부. (등록 해제된 stale id 는 기본값으로 폴백되므로 OFF 가 아님) */
    fun isOff(id: String): Boolean = id == OFF_ID

    /**
     * 재생할 waveform 을 돌려준다.
     *  - OFF → null (진동 안 함)
     *  - 유효한 패턴 → 그 waveform
     *  - stale id → 기본 패턴의 waveform 으로 폴백
     */
    fun waveformOf(id: String): LongArray? {
        val def = find(id) ?: find(DEFAULT_ID)
        return def?.waveform
    }

    /** 설정 화면 표시용 라벨. stale id 는 기본 패턴 라벨로 폴백. */
    fun label(id: String): String =
        (find(id) ?: find(DEFAULT_ID))?.label ?: "기본 (1초)"
}
