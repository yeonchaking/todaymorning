package com.yeon.todaymorning.ui.routeselect

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yeon.todaymorning.ui.common.EmptyStateText
import com.yeon.todaymorning.ui.common.ErrorStateText
import com.yeon.todaymorning.ui.common.SectionLoading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteSelectScreen(
    onRouteSelected: () -> Unit,
    onBack: () -> Unit,
    viewModel: RouteSelectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 에러 토스트 표시
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    // 저장 완료 시 자동 뒤로가기
    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onRouteSelected()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("출근 경로 선택") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    SectionLoading(
                        modifier = Modifier.align(Alignment.Center),
                        label = "경로 탐색 중..."
                    )
                }

                uiState.errorMessage != null -> {
                    ErrorStateText(
                        text = uiState.errorMessage!!,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        onRetry = viewModel::retry
                    )
                }

                uiState.routes.isEmpty() -> {
                    EmptyStateText(
                        text = "경로를 찾을 수 없습니다.",
                        modifier = Modifier.align(Alignment.Center).padding(24.dp)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "출근 경로를 선택하면 첫 번째 대중교통이 미션 대상이 됩니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(uiState.routes) { route ->
                            RouteCard(
                                route = route,
                                isSaving = uiState.isSaving,
                                onSelect = { viewModel.selectRoute(route) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteCard(
    route: RouteOption,
    isSaving: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 상단: 소요시간 + 환승 + 요금
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${route.totalTimeMin}분",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (route.transferCount > 0) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("환승 ${route.transferCount}회") }
                        )
                    } else {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("직통") }
                        )
                    }
                    if (route.fare > 0) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("${route.fare}원") }
                        )
                    }
                }
            }

            HorizontalDivider()

            // 미션 타겟 정보
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "🎯 미션 대상",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val modeIcon = if (route.firstTransitMode == "버스") "🚌" else "🚇"
                    Text(
                        text = "$modeIcon ${route.firstTransitRoute}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = route.firstStopName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                if (route.direction.isNotBlank()) {
                    Text(
                        text = "→ ${route.direction} 방면",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 선택 버튼
            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("이 경로로 설정")
                }
            }
        }
    }
}
