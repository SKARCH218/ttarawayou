package com.trevit.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trevit.app.AppState
import com.trevit.app.DURATIONS
import com.trevit.app.R
import com.trevit.app.Screen
import com.trevit.app.map.stopTypeLabel
import com.trevit.app.won
import com.trevit.shared.DayPlanDto
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(state: AppState) {
    val plan = state.plan ?: run {
        state.screen = Screen.Home
        return
    }
    var selectedDay by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.region?.let { "$it 여행" } ?: "여행 플랜") },
                navigationIcon = {
                    IconButton(onClick = { state.screen = Screen.Home }) {
                        Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "뒤로")
                    }
                },
                actions = {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                when (plan.plannedBy) {
                                    "AI" -> "AI"
                                    "DEMO" -> "데모"
                                    else -> "자동"
                                },
                            )
                        },
                    )
                    Spacer(Modifier.width(8.dp))
                },
            )
        },
        bottomBar = {
            val allDone = state.completedDays >= plan.dayPlans.size
            val nextDay = min(state.completedDays, plan.dayPlans.size - 1)
            Button(
                onClick = { if (!allDone) state.screen = Screen.Journey(nextDay) },
                enabled = !allDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
            ) {
                Text(
                    when {
                        allDone -> "여행 완료"
                        state.completedDays == 0 -> "여행 시작"
                        else -> "DAY ${state.completedDays + 1} 시작"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            plan.aiReason?.takeIf { it.isNotBlank() }?.let { reason ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "AI 추천 이유",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(reason, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            OutlinedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SummaryItem("예산", won(plan.budget))
                    SummaryItem("총비용", won(plan.totalCost))
                    SummaryItem("잔여", won(plan.remainingBudget))
                }
            }

            SectionLabel("예산 배분")
            val b = plan.breakdown
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BudgetCell("숙박", b.lodgingSpent, b.lodgingBudget, Modifier.weight(1f))
                BudgetCell("관광", b.attractionSpent, b.attractionBudget, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BudgetCell("식비", b.foodSpent, b.foodBudget, Modifier.weight(1f))
                BudgetCell("교통", b.transportSpent, b.transportBudget, Modifier.weight(1f))
            }

            SectionLabel("일정 · ${DURATIONS.getOrElse(plan.days - 1) { "${plan.days}일" }}")
            if (plan.dayPlans.size > 1) {
                TabRow(selectedTabIndex = selectedDay) {
                    plan.dayPlans.forEachIndexed { i, dp ->
                        Tab(
                            selected = selectedDay == i,
                            onClick = { selectedDay = i },
                            text = { Text(if (i < state.completedDays) "DAY ${dp.day} ✓" else "DAY ${dp.day}") },
                        )
                    }
                }
            }
            plan.dayPlans.getOrNull(selectedDay)?.let { dayPlan ->
                DayTimeline(dayPlan, revealed = selectedDay < state.completedDays)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BudgetCell(label: String, spent: Long, budget: Long, modifier: Modifier = Modifier) {
    OutlinedCard(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(won(spent), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "예산 ${won(budget)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DayTimeline(dayPlan: DayPlanDto, revealed: Boolean) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            dayPlan.stops.forEachIndexed { i, stop ->
                val isStart = stop.type == "START" || i == 0
                val show = revealed || isStart
                val arriveAt = dayPlan.legs.getOrNull(i - 1)?.arriveAt

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(if (show) R.drawable.ic_map_pin else R.drawable.ic_lock),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (show) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (show) (stop.name ?: "???") else "??? · ${stopTypeLabel(stop.type)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        arriveAt?.let {
                            Text(
                                "$it 도착",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (stop.cost > 0) {
                        Text(
                            won(stop.cost),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                dayPlan.legs.getOrNull(i)?.let { leg ->
                    Text(
                        buildString {
                            append(if (leg.mode == "TRANSIT") "버스 " else "도보 ")
                            append("${leg.durationMinutes}분 · ${formatDistance(leg.distanceMeters)}")
                            if (leg.fare > 0) append(" · ${won(leg.fare)}")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 32.dp, top = 6.dp, bottom = 6.dp),
                    )
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "DAY ${dayPlan.day}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    won(dayPlan.dayCost),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

fun formatDistance(meters: Double): String =
    if (meters >= 1000) "%.1fkm".format(meters / 1000) else "${meters.toInt()}m"
