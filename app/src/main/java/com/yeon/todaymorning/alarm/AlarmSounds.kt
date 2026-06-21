package com.yeon.todaymorning.alarm

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.annotation.RawRes

/**
 * 알람음 카탈로그 + 선택값(`alarmSoundId`) 해석 단일 지점.
 *
 * `alarmSoundId` 규칙:
 *  - `""`             : 폰 시스템 기본 알람음 (기본값, 기존 동작과 동일)
 *  - `"builtin:<key>"`: 앱 내장 음원 (`res/raw`). [BUILT_INS]에 등록된 것만 유효.
 *  - 그 외 문자열      : 시스템 알람음 선택기로 고른 `content://...` URI
 *
 * ── 내장 음원을 나중에 추가하는 방법 ──────────────────────────────
 *  1) 음원 파일(ogg/mp3 등)을 `app/src/main/res/raw/` 에 넣는다.
 *     (파일명은 소문자·영문·숫자·`_`만. 예: `alarm_morning_bell.ogg`)
 *  2) 아래 [BUILT_INS] 에 한 줄 추가:
 *     `BuiltInSound("morning_bell", "아침 종소리", R.raw.alarm_morning_bell)`
 *  그러면 설정 알람음 선택 다이얼로그에 자동으로 노출된다. 다른 코드 수정 불필요.
 */
data class BuiltInSound(
    val key: String,
    val label: String,
    @RawRes val resId: Int
)

object AlarmSounds {

    /** 폰 시스템 기본 알람음을 의미하는 빈 id. */
    const val DEFAULT_ID: String = ""

    private const val BUILTIN_PREFIX = "builtin:"

    /**
     * 내장 음원 목록. 여기에 [BuiltInSound] 한 줄만 추가하면 선택지에 자동 노출된다.
     * 지금은 비어 있음 — 폰 시스템 음원만으로 동작하며, 음원 추가 준비만 되어 있는 상태.
     */
    val BUILT_INS: List<BuiltInSound> = emptyList()

    fun builtInId(key: String): String = "$BUILTIN_PREFIX$key"
    fun isBuiltIn(id: String): Boolean = id.startsWith(BUILTIN_PREFIX)
    private fun builtInKey(id: String): String = id.removePrefix(BUILTIN_PREFIX)

    /** id 가 내장 음원이면 해당 정의를, 아니면 null. (등록 해제된 stale id 도 null) */
    fun findBuiltIn(id: String): BuiltInSound? =
        if (isBuiltIn(id)) BUILT_INS.find { it.key == builtInKey(id) } else null

    /** 시스템 기본 알람음 URI. 없으면 알림음 → 벨소리 순 폴백. */
    fun defaultAlarmUri(): Uri? =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

    /** 설정 화면 표시용 라벨. */
    fun label(context: Context, id: String): String = when {
        id == DEFAULT_ID -> "기본 알람음"
        isBuiltIn(id) -> findBuiltIn(id)?.label ?: "기본 알람음"
        else -> runCatching {
            RingtoneManager.getRingtone(context, Uri.parse(id))?.getTitle(context)
        }.getOrNull() ?: "사용자 선택음"
    }
}
