package com.trevit.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * 로그인 세션 토큰. 클라이언트는 이 값을 localStorage에 두고
 * `Authorization: Bearer <token>` 헤더로 보낸다.
 * 로그아웃하면 삭제되고, 만료(기본 30일)되면 인증에 실패한다.
 */
@Entity
@Table(name = "auth_tokens")
class AuthToken(
    @Id
    @Column(length = 64)
    var token: String = "",

    @Column(nullable = false)
    var userId: Long = 0,

    @Column(nullable = false)
    var expiresAt: Instant = Instant.now(),
) {
    fun isExpired(now: Instant = Instant.now()): Boolean = expiresAt.isBefore(now)

    companion object {
        /** 토큰 유효기간 (일) */
        const val TTL_DAYS = 30L
    }
}
