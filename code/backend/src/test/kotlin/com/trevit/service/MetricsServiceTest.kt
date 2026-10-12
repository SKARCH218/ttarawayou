package com.trevit.service

import com.trevit.entity.User
import com.trevit.entity.UserEvent
import com.trevit.repository.UserEventRepository
import com.trevit.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.transaction.PlatformTransactionManager
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** 퍼널·리텐션 계산: 기록 시작 전 가입자는 빼고, 가입일 기준 N일째 활동을 센다 */
class MetricsServiceTest {

    private val today: LocalDate = LocalDate.of(2026, 10, 12)
    private val now: Instant = at(today, 15)

    private fun at(d: LocalDate, hour: Int): Instant = d.atTime(LocalTime.of(hour, 0)).atZone(MetricsService.KST).toInstant()
    private fun user(id: Long, joined: Instant) = User(id = id, email = "u$id@t.kr", nickname = "u$id", createdAt = joined)
    private fun ev(type: String, uid: Long?, t: Instant) = UserEvent(userId = uid, type = type, createdAt = t)

    @Test
    fun `퍼널과 리텐션을 기록 있는 가입자 기준으로 계산한다`() {
        val d3 = today.minusDays(3)
        val events = listOf(
            ev(UserEvent.SIGNUP, 1, at(d3, 10)),                    // 기록 시작
            ev(UserEvent.PLAN_CREATED, 1, at(d3, 11)),
            ev(UserEvent.JOURNEY_STARTED, 1, at(d3, 12)),
            ev(UserEvent.ACTIVE, 1, at(d3.plusDays(1), 9)),         // 다음 날 재방문 → D1
            ev(UserEvent.SIGNUP, 2, at(today, 9)),
            ev(UserEvent.PLAN_CREATED, 2, at(today, 10)),
            ev(UserEvent.MAIL_FAILED, null, at(today, 8)),
        )
        val users = listOf(
            user(1, at(d3, 10)),
            user(2, at(today, 9)),
            user(3, at(today.minusDays(20), 9)),                    // 기록 시작 전 가입 → 퍼널·리텐션 제외
        )
        val repo = Mockito.mock(UserEventRepository::class.java)
        Mockito.`when`(repo.findByCreatedAtAfter(Mockito.any() ?: Instant.EPOCH)).thenReturn(events)
        Mockito.`when`(repo.findFirstByOrderByCreatedAtAsc()).thenReturn(events.first())
        val userRepo = Mockito.mock(UserRepository::class.java)
        Mockito.`when`(userRepo.findAll()).thenReturn(users)
        val mail = Mockito.mock(MailService::class.java)
        Mockito.`when`(mail.configured()).thenReturn(true)

        val m = MetricsService(repo, userRepo, mail, Mockito.mock(PlatformTransactionManager::class.java)).dashboard(now)

        assertEquals(3, m.totals.users)
        assertEquals(listOf(2, 2, 1, 0, 1), m.funnel.map { it.users }, "가입 → 플랜 → 여정 → 완주 → 재방문")
        assertEquals(1, m.mail.failed7d)

        // 코호트: 오늘 가입(1명, D1 아직 없음), 3일 전 가입(1명, D1 100%, D3 0%)
        val old = m.retention.cohorts.first { it.date == d3.toString() }
        assertEquals(listOf(1.0, 1.0, 0.0, null, null), old.days)
        val fresh = m.retention.cohorts.first { it.date == today.toString() }
        assertEquals(1.0, fresh.days[0])
        assertNull(fresh.days[1])
        assertEquals(2, m.retention.tracked)
        assertEquals(1, m.retention.untracked)
        // 커브 D1: 하루 이상 지난 가입자 1명 중 1명
        assertEquals(1.0, m.retention.curve[1].rate)
        assertEquals(1, m.retention.curve[1].eligible)
    }
}
