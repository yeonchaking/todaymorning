package com.yeon.todaymorning.domain.model

data class TransitArrival(
    val type: TransitType,
    val routeName: String,      // 버스 번호 or 지하철 호선
    val destination: String,    // 방향 (종점 or 방면)
    val arrivalSeconds: Int,    // 도착까지 남은 초
    val arrivalMessage: String  // "3분 후" 등 표시용 문자열
)
