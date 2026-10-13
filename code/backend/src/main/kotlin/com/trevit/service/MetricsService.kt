package com.trevit.service

import com.trevit.entity.UserEvent
import com.trevit.repository.UserEventRepository
import com.trevit.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

/**
 * 실측 지표: 행동 기록([UserEvent])을 남기고, 퍼널·리텐션·일별 추이를 계산한다.
 *
 * 핵심 지표 정의 (관리자 대시보드에도 같은 설명이 나간다)
 * - 활성 사용자: 그날 가입·로그인·앱 열기·플랜 생성·여정 시작·완주 중 하나라도 한 회원
 * - 퍼널: 가입 → 첫 플랜 생성 → 여정 시작 → 하루 완주 → 재방문(가입 다음 날 이후 다시 활동)
 * - Day-N 리텐션: 가입한 날(한국 시간) 기준 N일째 되는 날 활동한 회원 비율
 *
 * 기록은 부가 기능이라 실패해도 가입·플랜 같은 본 기능을 막지 않는다 (별도 트랜잭션 + 예외 삼킴).
 */
@Service
class MetricsService(
    private val events: UserEventRepository,
    private val users: UserRepository,
    // 메일 설정 여부만 읽는다. 발송 성공·실패 기록은 EmailVerificationService 가 남긴다 (순환 의존 방지)
    private val mail: MailService,
    txManager: PlatformTransactionManager,
) {
    private val log = LoggerFactory.getLogger(MetricsService::class.java)
    private val tx = TransactionTemplate(txManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    /** 회원별로 ACTIVE 를 마지막으로 남긴 날 — 하루 한 번만 남긴다 */
    private val activeLogged = ConcurrentHashMap<Long, LocalDate>()

    fun record(type: String, userId: Long? = null) {
        try {
            tx.executeWithoutResult { events.save(UserEvent(userId = userId, type = type, createdAt = Instant.now())) }
        } catch (e: Exception) {
            log.warn("지표 기록 실패({}): {}", type, e.message)
        }
    }

    /** 로그인 상태로 앱을 쓴 날 (하루 한 번) */
    fun recordActive(userId: Long) {
        val today = LocalDate.now(KST)
        if (activeLogged.put(userId, today) == today) return
        record(UserEvent.ACTIVE, userId)
    }

    // ---------- 대시보드 ----------

    data class Totals(
        val users: Int, val signups7d: Int, val activeToday: Int, val active7d: Int,
        val plans7d: Int, val journeys7d: Int,
    )
    data class Daily(
        val date: String, val signups: Int, val activeUsers: Int, val plans: Int,
        val journeys: Int, val mailSent: Int, val mailFailed: Int,
    )
    data class FunnelStep(val key: String, val label: String, val users: Int, val fromStart: Double?, val fromPrev: Double?)
    data class Cohort(val date: String, val size: Int, val days: List<Double?>)
    data class CurvePoint(val day: Int, val rate: Double?, val eligible: Int)
    data class Retention(val days: List<Int>, val cohorts: List<Cohort>, val curve: List<CurvePoint>, val tracked: Int, val untracked: Int)
    data class MailHealth(val configured: Boolean, val sent7d: Int, val failed7d: Int, val lastSentAt: String?, val lastFailedAt: String?)
    data class Dashboard(
        val generatedAt: String, val trackingSince: String?, val totals: Totals, val daily: List<Daily>,
        val funnel: List<FunnelStep>, val funnelBase: String, val retention: Retention, val mail: MailHealth,
    )

    fun dashboard(now: Instant = Instant.now()): Dashboard {
        val today = now.atZone(KST).toLocalDate()   // 기준 시각에서 날짜를 뽑는다 (테스트·재계산이 실제 날짜에 흔들리지 않게)
        val since = now.minus(Duration.ofDays(WINDOW_DAYS))
        val recent = events.findByCreatedAtAfter(since)
        val allUsers = users.findAll()
        val trackingStart = events.findFirstByOrderByCreatedAtAsc()?.createdAt
        fun day(i: Instant): LocalDate = i.atZone(KST).toLocalDate()

        // 회원별 활동한 날짜
        val activeDays = HashMap<Long, MutableSet<LocalDate>>()
        for (e in recent) {
            val uid = e.userId ?: continue
            if (e.type in UserEvent.ACTIVITY_TYPES) activeDays.getOrPut(uid) { HashSet() } += day(e.createdAt)
        }
        // 가입일도 활동한 날이다 (기록 시작 전에 가입했어도)
        val signupDay = allUsers.mapNotNull { u -> u.id?.let { it to day(u.createdAt) } }.toMap()
        signupDay.forEach { (uid, d) -> activeDays.getOrPut(uid) { HashSet() } += d }
        fun activeOf(uid: Long?): Set<LocalDate> = uid?.let { activeDays[it] }.orEmpty()

        // 일별 추이 (최근 14일)
        val daily = (DAILY_DAYS - 1 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            val dayEvents = recent.filter { day(it.createdAt) == d }
            Daily(
                date = d.toString(),
                signups = signupDay.values.count { it == d },
                activeUsers = activeDays.count { d in it.value },
                plans = dayEvents.count { it.type == UserEvent.PLAN_CREATED },
                journeys = dayEvents.count { it.type == UserEvent.JOURNEY_STARTED },
                mailSent = dayEvents.count { it.type == UserEvent.MAIL_SENT },
                mailFailed = dayEvents.count { it.type == UserEvent.MAIL_FAILED },
            )
        }
        val last7 = daily.takeLast(7)
        val weekStart = today.minusDays(6)
        val totals = Totals(
            users = allUsers.size,
            signups7d = last7.sumOf { it.signups },
            activeToday = activeDays.count { today in it.value },
            active7d = activeDays.count { entry -> entry.value.any { !it.isBefore(weekStart) } },
            plans7d = last7.sumOf { it.plans },
            journeys7d = last7.sumOf { it.journeys },
        )

        // 퍼널 — 행동 기록이 남아 있는 회원(기록 시작 이후 가입)만 센다. 그 전 가입자는 첫 행동을 알 수 없다
        val tracked = allUsers.filter { u -> trackingStart != null && !u.createdAt.isBefore(trackingStart) }
        val base = tracked.ifEmpty { allUsers }
        val baseIds = base.mapNotNull { it.id }.toSet()
        fun usersWith(type: String) = recent.filter { e -> e.type == type && e.userId?.let { it in baseIds } == true }.mapNotNull { it.userId }.toSet()
        val planned = usersWith(UserEvent.PLAN_CREATED)
        val journeyed = usersWith(UserEvent.JOURNEY_STARTED)
        val completed = usersWith(UserEvent.DAY_COMPLETED)
        val returned = baseIds.filter { uid ->
            val first = signupDay[uid] ?: return@filter false
            activeDays[uid].orEmpty().any { it.isAfter(first) }
        }.toSet()
        val steps = listOf(
            "signup" to "가입" to baseIds.size,
            "plan" to "첫 플랜 생성" to planned.size,
            "journey" to "여정 시작" to journeyed.size,
            "complete" to "하루 완주" to completed.size,
            "return" to "재방문 (가입 다음 날 이후)" to returned.size,
        )
        val funnel = steps.mapIndexed { i, (kl, n) ->
            FunnelStep(kl.first, kl.second, n,
                fromStart = rate(n, steps[0].second),
                fromPrev = if (i == 0) null else rate(n, steps[i - 1].second))
        }

        // 리텐션 — 최근 28일 가입 코호트(가입일별), 기록이 있는 회원만
        val cohortUsers = tracked.filter { u -> !day(u.createdAt).isBefore(today.minusDays(COHORT_DAYS - 1)) }
        val cohorts = cohortUsers.groupBy { day(it.createdAt) }.toSortedMap(compareByDescending { it }).map { (d, members) ->
            val age = Duration.between(d.atStartOfDay(KST), today.atStartOfDay(KST)).toDays().toInt()
            Cohort(d.toString(), members.size, RETENTION_DAYS.map { n ->
                if (n > age) null
                else rate(members.count { u -> d.plusDays(n.toLong()) in activeOf(u.id) }, members.size)
            })
        }
        val curve = (0..CURVE_DAYS).map { n ->
            val eligible = cohortUsers.filter { u ->
                !day(u.createdAt).plusDays(n.toLong()).isAfter(today)
            }
            val kept = eligible.count { u -> day(u.createdAt).plusDays(n.toLong()) in activeOf(u.id) }
            CurvePoint(n, rate(kept, eligible.size), eligible.size)
        }

        val week = now.minus(Duration.ofDays(7))
        val mailHealth = MailHealth(
            configured = mail.configured(),
            sent7d = recent.count { it.type == UserEvent.MAIL_SENT && it.createdAt.isAfter(week) },
            failed7d = recent.count { it.type == UserEvent.MAIL_FAILED && it.createdAt.isAfter(week) },
            lastSentAt = events.findFirstByTypeOrderByCreatedAtDesc(UserEvent.MAIL_SENT)?.createdAt?.toString(),
            lastFailedAt = events.findFirstByTypeOrderByCreatedAtDesc(UserEvent.MAIL_FAILED)?.createdAt?.toString(),
        )

        return Dashboard(
            generatedAt = now.toString(),
            trackingSince = trackingStart?.toString(),
            totals = totals,
            daily = daily,
            funnel = funnel,
            funnelBase = if (tracked.isEmpty()) "전체 회원 (행동 기록 시작 이후 가입자가 아직 없음)" else "행동 기록 시작 이후 가입한 회원",
            retention = Retention(RETENTION_DAYS, cohorts, curve, tracked.size, allUsers.size - tracked.size),
            mail = mailHealth,
        )
    }

    private fun rate(n: Int, of: Int): Double? = if (of == 0) null else n.toDouble() / of

    companion object {
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
        private const val WINDOW_DAYS = 60L
        private const val DAILY_DAYS = 14
        private const val COHORT_DAYS = 28L
        private const val CURVE_DAYS = 14
        private val RETENTION_DAYS = listOf(0, 1, 3, 7, 14)
    }
}
