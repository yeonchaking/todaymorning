package com.yeon.todaymorning.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeon.todaymorning.ui.theme.AppTheme

/**
 * 화면 곳곳에서 제각각이던 로딩/빈상태/에러 표현을 한 곳에 모은 공통 컴포저블.
 * (busselect/routeselect/locationpicker 등에서 MaterialTheme 기본 색·크기를 쓰던 걸
 * AppTheme.colors 기반으로 통일 — TimeAttackScreen/MainActivity의 ArrivalDialog가
 * 이미 쓰고 있던 스타일을 기준으로 맞췄다.)
 */

/** 화면/섹션 전체가 로딩 중일 때 — 큰 스피너(+선택적 라벨). */
@Composable
fun SectionLoading(
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val c = AppTheme.colors
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp, color = c.primary)
        if (label != null) {
            Text(text = label, fontSize = 13.sp, color = c.onVar)
        }
    }
}

/** 한 줄/한 행 안에서 쓰는 작은 인라인 스피너 — 텍스트 옆에 붙는 용도. */
@Composable
fun InlineLoading(
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val c = AppTheme.colors
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = c.primary)
        if (label != null) {
            Spacer(Modifier.width(8.dp))
            Text(text = label, fontSize = 13.sp, color = c.onVar)
        }
    }
}

/** "정보 없음" 류의 빈 상태 문구 — 화면마다 다르던 톤을 하나로. */
@Composable
fun EmptyStateText(
    text: String,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    Text(
        text = text,
        fontSize = 13.5.sp,
        color = c.onVar,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

/** 에러 문구(+선택적 재시도 버튼) — danger 색으로 통일. */
@Composable
fun ErrorStateText(
    text: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    val c = AppTheme.colors
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.5.sp,
            color = c.danger,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            TextButton(onClick = onRetry) { Text("다시 시도", color = c.primary) }
        }
    }
}
