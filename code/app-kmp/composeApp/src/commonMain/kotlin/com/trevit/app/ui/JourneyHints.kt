package com.trevit.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trevit.app.map.stopEmoji
import com.trevit.app.map.stopTypeLabel
import com.trevit.app.oneDecimal
import com.trevit.app.won
import com.trevit.shared.StopDto

/** 다음 비밀 장소 힌트 하나 — 구간 진행률이 [at] 이상이면 열린다 */
class JourneyHint(val at: Double, val emoji: String, val text: String)

/**
 * 가까워질수록 구체적으로 — 종류 → 지역(구) → 평점·비용 → 동네·이름 첫 글자.
 * 정체(이름 전체)는 도착했을 때만 공개된다.
 */
fun buildHints(stop: StopDto): List<JourneyHint> {
    val tokens = stop.address.orEmpty().split(" ").filter { it.isNotBlank() }
    val district = tokens.firstOrNull { it.endsWith("구") || it.endsWith("군") }
        ?: tokens.getOrNull(1)?.takeIf { it.endsWith("시") }
    val neighborhood = tokens.firstOrNull { t -> listOf("동", "읍", "면", "리", "가", "로", "길").any { t.endsWith(it) } }
    val name = stop.name.orEmpty().replace(" ", "")

    val hints = ArrayList<JourneyHint>()
    hints += JourneyHint(0.0, stopEmoji(stop.type), "다음은 ${stopTypeLabel(stop.type)}이에요")
    hints += JourneyHint(0.3, "📍", district?.let { "$it 어딘가예요" } ?: "지금 가는 방향 그대로예요")
    val detail = buildList {
        if (stop.rating > 0) add("평점 ${oneDecimal(stop.rating)}")
        if (stop.cost > 0) add("약 ${won(stop.cost)}")
    }
    hints += JourneyHint(0.6, "⭐", detail.joinToString(" · ").ifEmpty { "가 보면 분명 마음에 들 거예요" })
    if (name.isNotEmpty()) {
        val masked = name.first() + "○".repeat((name.length - 1).coerceAtLeast(0))
        val where = neighborhood?.let { "$it · " } ?: ""
        hints += JourneyHint(0.85, "🔤", "$where'$masked' (${name.length}글자)")
    }
    return hints
}

@Composable
fun HintButton(unlocked: Int, total: Int, expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = webSurface(),
        border = BorderStroke(1.dp, if (expanded) WebMint else webBorderStrong()),
        shadowElevation = 2.dp,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("💡", fontSize = 14.sp, fontFamily = TossFaceFontFamily)
            Text(
                " 힌트 $unlocked/$total",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = WebMintDeep,
            )
        }
    }
}

/** 열린 힌트는 내용을, 잠긴 힌트는 몇 m 더 가면 열리는지 보여준다 */
@Composable
fun HintCard(hints: List<JourneyHint>, progress: Double, legLengthMeters: Double, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = webSurface().copy(alpha = 0.97f),
        border = BorderStroke(1.dp, webBorderStrong()),
        shadowElevation = 4.dp,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            hints.forEach { hint ->
                val open = progress >= hint.at
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (open) hint.emoji else "🔒", fontSize = 15.sp, fontFamily = TossFaceFontFamily)
                    androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
                    Text(
                        if (open) hint.text
                        else "${formatDistance((hint.at - progress) * legLengthMeters)} 더 가면 열려요",
                        fontSize = 13.5.sp,
                        fontWeight = if (open) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (open) webText() else webTextDim(),
                    )
                }
            }
        }
    }
}
