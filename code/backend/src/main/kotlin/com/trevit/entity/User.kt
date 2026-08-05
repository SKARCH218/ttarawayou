package com.trevit.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * 회원. 비밀번호는 BCrypt 해시로만 저장하고 평문은 어디에도 남기지 않는다.
 * 이메일은 소문자로 정규화해 저장한다 (대소문자 다른 중복 가입 방지).
 *
 * 가입 경로는 두 가지다.
 *  - LOCAL  : 이메일 인증(6자리 코드)을 마쳐야 가입된다. passwordHash 있음
 *  - GOOGLE : 구글이 인증한 이메일을 그대로 신뢰한다. passwordHash 없음
 */
@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 190)
    var email: String = "",

    /** 소셜 가입 회원은 비밀번호가 없다 */
    var passwordHash: String? = null,

    @Column(nullable = false, unique = true, length = 20)
    var nickname: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var provider: Provider = Provider.LOCAL,

    /** 구글 계정 고유 식별자(sub). 이메일이 바뀌어도 같은 계정을 알아본다 */
    @Column(unique = true, length = 64)
    var googleSub: String? = null,

    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),
) {
    enum class Provider { LOCAL, GOOGLE }
}
