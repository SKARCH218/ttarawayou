package com.trevit.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * 회원가입 메일 인증. 이메일 하나당 진행 중인 인증은 한 건만 두고 재발송 때 덮어쓴다.
 * 코드는 5분 내에 맞혀야 하고, 맞힌 뒤에는 30분 안에 가입을 마쳐야 한다.
 * 코드 원문은 저장하지 않고 해시만 남긴다.
 */
@Entity
@Table(name = "email_verifications")
class EmailVerification(
    @Id
    @Column(length = 190)
    var email: String = "",

    @Column(nullable = false)
    var codeHash: String = "",

    @Column(nullable = false)
    var expiresAt: Instant = Instant.now(),

    /** 코드 입력 시도 횟수 — 무차별 대입을 막는다 */
    @Column(nullable = false)
    var attempts: Int = 0,

    /** 마지막 발송 시각 — 재발송 쿨다운 판단용 */
    @Column(nullable = false)
    var sentAt: Instant = Instant.now(),

    /** 인증 성공 시각. null이면 아직 인증 전 */
    var verifiedAt: Instant? = null,
) {
    fun isCodeExpired(now: Instant = Instant.now()): Boolean = expiresAt.isBefore(now)

    /** 인증을 마쳤고 아직 가입에 쓸 수 있는 상태인가 */
    fun isUsable(now: Instant = Instant.now()): Boolean {
        val at = verifiedAt ?: return false
        return at.plusSeconds(VERIFIED_TTL_MINUTES * 60).isAfter(now)
    }

    companion object {
        /** 인증코드 유효시간 (분) */
        const val CODE_TTL_MINUTES = 5L

        /** 인증 완료 후 가입까지 허용되는 시간 (분) */
        const val VERIFIED_TTL_MINUTES = 30L

        /** 코드 입력 최대 시도 횟수 */
        const val MAX_ATTEMPTS = 5

        /** 재발송 쿨다운 (초) */
        const val RESEND_COOLDOWN_SECONDS = 60L
    }
}
