package com.trevit.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trevit.app.AppState
import com.trevit.app.R
import com.trevit.app.Screen
import com.trevit.app.won
import com.trevit.shared.DayPlanDto

/**
 * 플랜 결과 — 웹 `plan.html` 을 그대로 옮긴 화면.
 * 심볼 → "플랜 완성" → 부제·설계 주체 → (AI 한마디) → 비용 카드 → 예산 배분 바 → Day 버튼 → 처음부터.
 *
 * 웹과 마찬가지로 장소 이름은 여기서 절대 보여주지 않는다 — 도착해야 공개된다.
 */
@Composable
fun ResultScreen(state: AppState) {
    val plan = state.plan ?: run {
        state.screen = Screen.Setup
        return
    }

    WebScreen {
        // 웹 `.brand-logo` (plan.html 은 심볼만 76px) — 원본 비율 94:58
        Icon(
            painter = painterResource(R.drawable.ic_travit_symbol),
            contentDescription = null,
            tint = BrandMint,
            modifier = Modifier
                .width(76.dp)
                .height(47.dp),
        )
        Spacer(Modifier.height(6.dp))
        GradientTitle("플랜 완성")
        Spacer(Modifier.height(10.dp))
        WebSubtitle("장소는 도착할 때 공개돼요")
        Spacer(Modifier.height(6.dp))
        WebSubtitle(
            if (plan.plannedBy == "AI") "AI가 설계한 플랜" else "알고리즘이 설계한 플랜",
            color = webTextFaint(),
            fontSize = 12.sp,
        )

        // 웹: AI 추천 이유가 있으면 비용 카드 앞에 카드 하나를 더 끼운다
        plan.aiReason?.takeIf { it.isNotBlank() }?.let { reason ->
            Spacer(Modifier.height(16.dp))
            TrevitCard(Modifier.fillMaxWidth()) {
                Text("AI의 한마디", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = webTextChip())
                Spacer(Modifier.height(4.dp))
                Text(reason, fontSize = 13.sp, lineHeight = 21.sp, color = webTextChip())
            }
        }

        // ---- 비용 요약 (웹 `.cost-row` 4줄) ----
        Spacer(Modifier.height(16.dp))
        TrevitCard(Modifier.fillMaxWidth()) {
            CostRow("총 예산", won(plan.budget), webText())
            DashedDivider()
            CostRow("예상 총비용", won(plan.totalCost), WebPurple, valueSize = 23.sp)
            DashedDivider()
            CostRow("남는 예산", won(plan.remainingBudget), WebMint)
            DashedDivider()
            CostRow("남은 토큰", "%,d 토큰".format(plan.tokenBalance), WebOrange)
        }

        // ---- 예산 배분 사용률 (웹 `.bar-group`) ----
        Spacer(Modifier.height(16.dp))
        TrevitCard(Modifier.fillMaxWidth()) {
            Text("예산 배분 사용률", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = webTextLabel())
            val b = plan.breakdown
            // 웹 팔레트: 숙박=퍼플 · 관광=민트 · 식비=오렌지 · 교통=블루
            BudgetBarRow("숙박", b.lodgingSpent, b.lodgingBudget, WebPurple)
            BudgetBarRow("관광", b.attractionSpent, b.attractionBudget, WebMint)
            BudgetBarRow("식비", b.foodSpent, b.foodBudget, WebOrange)
            BudgetBarRow("교통", b.transportSpent, b.transportBudget, WebBlue)
        }

        // ---- Day 버튼 (웹 `.day-list`) ----
        Spacer(Modifier.height(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            plan.dayPlans.forEachIndexed { index, day ->
                DayEntry(state, day, index)
            }
        }

        Spacer(Modifier.height(18.dp))
        GhostButton("← 처음부터 다시", { state.resetPlan() }, Modifier.fillMaxWidth())
    }
}

/**
 * 웹 `.day-btn` 한 줄. 이전 일차를 마쳐야 다음 일차가 열린다.
 * 장소 이름 대신 개수·소요·비용만 노출한다.
 */
@Composable
private fun DayEntry(state: AppState, day: DayPlanDto, index: Int) {
    val completed = index < state.completedDays
    val locked = index > state.completedDays
    val mysterySpots = day.stops.filterIndexed { i, s ->
        i > 0 && !(i == day.stops.lastIndex && s.type == "LODGING")
    }.size
    val totalMinutes = day.legs.sumOf { it.durationMinutes }
    val startAt = day.legs.firstOrNull()?.departAt ?: "09:00"

    DayButton(
        badge = when {
            locked -> "잠김"
            completed -> "완료"
            else -> "D${day.day}"
        },
        title = "Day ${day.day} 여정 ${if (completed) "(완료)" else "따라가기"}",
        info = when {
            completed -> "완료한 여정 · 다시 보기"
            locked -> "Day ${day.day - 1} 완료 후 열려요 · $startAt 시작 예정"
            else -> "$startAt 시작 · 비밀 장소 ${mysterySpots}곳 · 이동 약 ${totalMinutes}분 · ${won(day.dayCost)}"
        },
        locked = locked,
        onClick = { state.screen = Screen.Journey(index) },
    )
}

/** 웹 `.bar-item` — 라벨/사용액 한 줄 + 8dp 바. 웹처럼 0에서 목표까지 차오른다. */
@Composable
private fun BudgetBarRow(label: String, spent: Long, budget: Long, color: Color) {
    val target = if (budget > 0) (spent.toFloat() / budget).coerceIn(0f, 1f) else 0f
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val fraction by animateFloatAsState(
        targetValue = if (started) target else 0f,
        animationSpec = tween(800),
        label = "barFill",
    )

    Spacer(Modifier.height(12.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = webTextMuted())
        Text(
            "${won(spent)} / ${won(budget)}",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = webTextMuted(),
        )
    }
    Spacer(Modifier.height(6.dp))
    BudgetBar(fraction, color)
}

fun formatDistance(meters: Double): String =
    if (meters >= 1000) "%.1fkm".format(meters / 1000) else "${meters.toInt()}m"
