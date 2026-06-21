package com.yeon.todaymorning.alarm

import android.content.Context
import android.speech.tts.TextToSpeech
import com.yeon.todaymorning.domain.model.TransitArrival
import com.yeon.todaymorning.domain.model.TransitType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

/**
 * 안드로이드 내장 [TextToSpeech] 엔진 래퍼 — 타임어택 음성 안내 단일 지점.
 *
 * 설계 의도:
 *  - 엔진 init 은 비동기다. 준비 전 호출은 조용히 무시한다(안전 fallback).
 *  - 한국어 미지원/엔진 미설치 기기에서도 앱이 죽지 않도록 모든 실패를 삼킨다.
 *  - 발화 '문장(대본)'은 전부 여기 [sentenceFor] 한 곳에서 만든다. 트리거(언제 읽을지)는
 *    [com.yeon.todaymorning.ui.timeattack.TimeAttackViewModel] 책임.
 *
 * 대본 규칙 (남은시간 접두사 없이 차편 정보만):
 *  - 도착 1분 이내      → "651번 버스가 곧 도착합니다."
 *  - 그 외             → "651번 버스가 3분 후 도착 예정입니다." (arrivalMessage 그대로 사용)
 *  - 지하철            → "2호선 열차가 …"
 *  - 차편 없음          → "도착 정보가 없습니다."
 *
 * 스코프: 일부러 @Singleton 을 붙이지 않는다. TimeAttackViewModel 마다 새 인스턴스를 받아
 * onCleared 에서 [shutdown] 하므로, 화면 재진입 시 새 엔진으로 다시 동작한다.
 */
class TtsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    @Volatile
    private var ready = false

    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val result = runCatching { tts.setLanguage(Locale.KOREAN) }.getOrNull()
            ready = result != null &&
                result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    /**
     * 차편 한 건을 음성으로 읽는다. 엔진 미준비/실패 시 조용히 무시.
     * 한 폴링에 여러 건이 연속 호출될 수 있으므로 QUEUE_ADD로 큐에 쌓아 순차 재생(앞 발화를 끊지 않음).
     */
    fun announce(arrival: TransitArrival?) {
        if (!ready) return
        val text = sentenceFor(arrival)
        runCatching {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "tts_announce_${arrival?.routeName}")
        }
    }

    /** 화면 종료 시 호출해 엔진 자원을 반환한다. */
    fun shutdown() {
        ready = false
        runCatching {
            tts.stop()
            tts.shutdown()
        }
    }

    companion object {
        /** 발화 문장 생성. 단위 테스트 가능하도록 순수 함수로 분리. */
        fun sentenceFor(arrival: TransitArrival?): String {
            if (arrival == null) return "도착 정보가 없습니다."
            val subject = when (arrival.type) {
                TransitType.BUS -> "${arrival.routeName}번 버스"
                TransitType.SUBWAY -> "${arrival.routeName} 열차"
            }
            // subject(버스/열차)는 모두 모음으로 끝나 조사 '가'가 붙는다. 공백 없이 결합.
            // 1분(60초) 이내면 '곧 도착', 그 외엔 도착 시간만 읽는다.
            // arrivalMessage 의 "[3번째 전]" 같은 꼬리표는 음성에서 제거. 예: "5분후[3번째 전]" → "5분후".
            val timePart = arrival.arrivalMessage.substringBefore("[").trim()
            return if (arrival.arrivalSeconds in 0..60) {
                "${subject}가 곧 도착합니다."
            } else {
                "${subject}가 $timePart 도착 예정입니다."
            }
        }
    }
}
