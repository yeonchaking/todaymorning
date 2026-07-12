package com.yeon.todaymorning.data.repository

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

/**
 * 공공 API 호출 실패를 "사용자에게 보여줄 문구"로 매핑하는 공통 유틸.
 *
 * 배경(2026-07-12): 그동안 [TransitRepository]의 도착 조회가 모든 예외를 `emptyList()`로
 * 삼켜버려서, 호출부는 "정말 도착 차편이 없음(정상)"과 "네트워크가 죽어서 0건(오류)"을
 * 구분할 수 없었다 — 사용자에겐 전부 "도착 정보가 없습니다"로 보였다.
 * 이제 도착 조회는 실패 시 [TransitException]을 던지고, 호출부(MissionEngine/MainViewModel)가
 * `userMessage`를 그대로 노출한다.
 */

/** 도착정보 조회 실패. [userMessage]는 사용자에게 그대로 보여줄 수 있는 한국어 문구. */
class TransitException(
    val userMessage: String,
    cause: Throwable? = null
) : Exception(userMessage, cause)

/** Throwable → 사용자 안내 문구. */
fun Throwable.toUserMessage(): String = when (this) {
    is TransitException -> userMessage
    is HttpException -> "일시적인 서버 오류예요. 잠시 후 다시 시도해 주세요. (${code()})"
    is UnknownHostException,
    is SocketTimeoutException,
    is IOException -> "네트워크 연결을 확인해 주세요."
    else -> "오류가 발생했어요. (${message ?: "알 수 없는 오류"})"
}
