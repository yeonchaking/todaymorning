package com.yeon.todaymorning.domain.model

/**
 * 게이미피케이션 파생 데이터 (홈 화면 변형 B "스트릭 히어로"용).
 *
 * 별도 저장 스키마 없이 기존 미션 기록(성공 수·streak)에서 전부 유도한다.
 *  - 포인트 = 성공 횟수 × [POINTS_PER_SUCCESS]
 *  - 레벨/진행률/다음 레벨까지 포인트 = [LevelSystem]
 *  - 주간 현황·배지 = MainViewModel에서 기록으로 계산
 *
 * TODO(향후): 포인트를 "정시 탑승 보너스" 등으로 가중하거나, 별도 누적 저장으로
 *  전환하려면 여기 규칙만 바꾸면 된다.
 */
const val POINTS_PER_SUCCESS = 200

/** 현재 레벨 상태. */
data class LevelInfo(
    val level: Int,            // 1-based
    val points: Int,
    val progress: Float,       // 0f..1f (현재 레벨 구간 내 진행률)
    val pointsToNext: Int?     // 다음 레벨까지 남은 포인트, 최고 레벨이면 null
)

object LevelSystem {
    /** 각 레벨(인덱스+1)에 도달하기 위한 누적 포인트. */
    private val THRESHOLDS = listOf(0, 400, 1000, 1800, 2800, 4000, 5400, 7000, 9000, 11500)

    fun fromPoints(points: Int): LevelInfo {
        val p = points.coerceAtLeast(0)
        var idx = THRESHOLDS.indexOfLast { p >= it }
        if (idx < 0) idx = 0
        val isMax = idx >= THRESHOLDS.lastIndex
        return if (isMax) {
            LevelInfo(level = idx + 1, points = p, progress = 1f, pointsToNext = null)
        } else {
            val start = THRESHOLDS[idx]
            val next = THRESHOLDS[idx + 1]
            val span = (next - start).coerceAtLeast(1)
            LevelInfo(
                level = idx + 1,
                points = p,
                progress = ((p - start).toFloat() / span).coerceIn(0f, 1f),
                pointsToNext = (next - p).coerceAtLeast(0)
            )
        }
    }
}

/** 주간 트래커의 요일별 상태. */
enum class DayStatus { SUCCESS, FAIL, TODAY, FUTURE, NONE }

/** 획득 배지. unlocked=false면 잠금(회색) 표시. */
data class BadgeUi(
    val key: String,
    val label: String,
    val emoji: String,
    val unlocked: Boolean
)
