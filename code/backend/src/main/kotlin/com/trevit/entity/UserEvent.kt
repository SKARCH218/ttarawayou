package com.trevit.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

/**
 * 사용자 행동 기록 — 퍼널·리텐션 지표의 원천 데이터.
 * 이름·장소 같은 개인정보는 남기지 않고 "누가(회원 번호) 언제 무엇을 했는지"만 남긴다.
 */
@Entity
@Table(
    name = "user_events",
    indexes = [Index(name = "idx_user_events_created", columnList = "createdAt"), Index(name = "idx_user_events_user", columnList = "userId")],
)
class UserEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    /** 회원 번호. 로그인하지 않은 사용·시스템 이벤트(메일 발송 등)는 null */
    var userId: Long? = null,

    @Column(nullable = false, length = 32)
    var type: String = "",

    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),
) {
    companion object {
        // 퍼널 단계
        const val SIGNUP = "SIGNUP"                 // 가입 완료
        const val PLAN_CREATED = "PLAN_CREATED"     // 플랜 생성 성공
        const val JOURNEY_STARTED = "JOURNEY_STARTED" // 여정(지도 따라가기) 시작 — 앱이 보냄
        const val DAY_COMPLETED = "DAY_COMPLETED"   // 하루 일정 완주 — 앱이 보냄

        // 활동 (리텐션 계산용)
        const val LOGIN = "LOGIN"
        const val ACTIVE = "ACTIVE"                 // 로그인 상태로 앱을 연 날 (하루 한 번)
        const val PASSWORD_RESET = "PASSWORD_RESET" // 비밀번호 찾기로 재설정

        // 시스템 상태
        const val MAIL_SENT = "MAIL_SENT"
        const val MAIL_FAILED = "MAIL_FAILED"

        /** 앱이 /api/events 로 보낼 수 있는 이벤트 */
        val CLIENT_TYPES = setOf(JOURNEY_STARTED, DAY_COMPLETED)

        /** 이 이벤트가 있으면 그날 "활동한 사용자"로 센다 */
        val ACTIVITY_TYPES = setOf(SIGNUP, LOGIN, ACTIVE, PLAN_CREATED, JOURNEY_STARTED, DAY_COMPLETED)
    }
}
