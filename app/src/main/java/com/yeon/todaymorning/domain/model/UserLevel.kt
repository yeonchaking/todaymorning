package com.yeon.todaymorning.domain.model

enum class UserLevel(
    val title: String,
    val emoji: String,
    val minStreak: Int,
    val description: String
) {
    ROOKIE("루키", "🌱", 0, "출근 여정을 시작해봐요"),
    BRONZE("브론즈", "🥉", 3, "3일 연속 성공!"),
    SILVER("실버", "🥈", 7, "7일 연속, 한 주를 완벽하게!"),
    GOLD("골드", "🥇", 14, "2주 연속, 습관이 됐어요!"),
    PLATINUM("플래티넘", "💎", 30, "한 달 연속, 출근 마스터!"),
    DIAMOND("다이아", "👑", 60, "두 달 연속, 전설이에요!");

    companion object {
        fun fromStreak(streak: Int): UserLevel =
            entries.reversed().firstOrNull { streak >= it.minStreak } ?: ROOKIE

        /** 다음 레벨까지 남은 일수 (이미 최고 레벨이면 null) */
        fun daysToNextLevel(streak: Int): Int? {
            val current = fromStreak(streak)
            val next = entries.getOrNull(current.ordinal + 1) ?: return null
            return next.minStreak - streak
        }
    }
}