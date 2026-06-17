package com.yeon.todaymorning.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yeon.todaymorning.data.db.MissionRecord
import java.time.LocalDate
import java.time.YearMonth

/**
 * 미션 기록 달력.
 * - 상단: 현재 연속 성공 일수
 * - 좌우 방향키로 월 이동(과거 데이터 전부 열람 가능)
 * - 성공/실패한 날을 해당 날짜에 마킹
 */
@Composable
fun MissionCalendar(
    streak: Int,
    records: List<MissionRecord>,
    modifier: Modifier = Modifier
) {
    // date("yyyy-MM-dd") -> isSuccess. 같은 날 중복 시 성공 우선.
    val resultByDate = remember(records) {
        val map = HashMap<String, Boolean>()
        records.forEach { r ->
            val prev = map[r.date]
            map[r.date] = (prev == true) || r.isSuccess
        }
        map
    }

    val today = remember { LocalDate.now() }
    var currentMonth by remember { mutableStateOf(YearMonth.from(today)) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {

            // 연속 성공 일수
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (streak >= 7) "🔥" else if (streak >= 3) "⭐" else "📅",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "현재 ${streak}일 연속 성공",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))

            // 월 이동 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "이전 달")
                }
                Text(
                    text = "${currentMonth.year}년 ${currentMonth.monthValue}월",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { currentMonth = currentMonth.plusMonths(1) },
                    // 미래 달로는 이동하지 않음
                    enabled = currentMonth.isBefore(YearMonth.from(today))
                ) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "다음 달")
                }
            }

            Spacer(Modifier.height(8.dp))

            // 요일 헤더 (일~토)
            val weekdays = listOf("일", "월", "화", "수", "목", "금", "토")
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdays.forEachIndexed { idx, w ->
                    Text(
                        text = w,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (idx) {
                            0 -> MaterialTheme.colorScheme.error
                            6 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // 날짜 그리드
            val daysInMonth = currentMonth.lengthOfMonth()
            // 1일의 요일 오프셋 (일요일 시작 기준): SUNDAY=7 -> 0, MONDAY=1 -> 1 ...
            val firstDayOffset = currentMonth.atDay(1).dayOfWeek.value % 7
            val totalCells = firstDayOffset + daysInMonth
            val rows = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - firstDayOffset + 1
                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNum in 1..daysInMonth) {
                                val date = currentMonth.atDay(dayNum)
                                val dateStr = date.toString() // ISO yyyy-MM-dd
                                val result = resultByDate[dateStr]
                                val isToday = date == today
                                DayCell(
                                    day = dayNum,
                                    result = result,
                                    isToday = isToday,
                                    isFuture = date.isAfter(today)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    result: Boolean?,      // true=성공, false=실패, null=기록 없음
    isToday: Boolean,
    isFuture: Boolean
) {
    val successColor = Color(0xFF4CAF50)
    val failColor = MaterialTheme.colorScheme.error

    val bg = when (result) {
        true -> successColor.copy(alpha = 0.18f)
        false -> failColor.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                isToday -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
