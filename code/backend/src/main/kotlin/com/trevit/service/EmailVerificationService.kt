package com.trevit.service

import com.trevit.entity.EmailVerification
import com.trevit.repository.EmailVerificationRepository
import com.trevit.repository.UserRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant

/**
 * 회원가입 메일 인증 — 6자리 코드를 보내고 맞히는지 확인한다.
 * 코드는 해시로만 저장하고, 시도 횟수·재발송 쿨다운으로 무차별 대입과 발송 남용을 막는다.
 */
@Service
class EmailVerificationService(
    private val verifications: EmailVerificationRepository,
    private val users: UserRepository,
    private val mail: MailService,
) {
    private val encoder = BCryptPasswordEncoder()
    private val random = SecureRandom()

    /** 인증코드 발송. 응답에는 남은 시간만 담고 코드는 절대 내려보내지 않는다 */
    @Transactional
    fun sendCode(rawEmail: String): Long {
        val email = normalize(rawEmail)
        require(email.isNotEmpty()) { "이메일을 입력해 주세요." }
        require(EMAIL_REGEX.matches(email)) { "이메일 형식이 올바르지 않아요." }
        require(!users.existsByEmail(email)) { "이미 가입된 이메일이에요." }

        val now = Instant.now()
        val existing = verifications.findById(email).orElse(null)
        if (existing != null) {
            val waited = Duration.between(existing.sentAt, now).seconds
            val cooldown = EmailVerification.RESEND_COOLDOWN_SECONDS
            require(waited >= cooldown) { "잠시 후 다시 시도해 주세요. (${cooldown - waited}초 남음)" }
        }

        val code = (1..6).map { random.nextInt(10) }.joinToString("")
        verifications.save(
            EmailVerification(
                email = email,
                codeHash = encoder.encode(code),
                expiresAt = now.plusSeconds(EmailVerification.CODE_TTL_MINUTES * 60),
                attempts = 0,
                sentAt = now,
                verifiedAt = null,
            )
        )
        mail.sendVerificationCode(email, code, EmailVerification.CODE_TTL_MINUTES)
        return EmailVerification.CODE_TTL_MINUTES * 60
    }

    /**
     * 코드 확인. 맞으면 인증 완료로 표시하고 30분 안에 가입을 마치면 된다.
     *
     * noRollbackFor 가 핵심이다 — 코드가 틀렸을 때 던지는 예외로 트랜잭션이 롤백되면
     * 방금 늘린 attempts 까지 같이 취소돼서 시도 횟수 제한이 아무 의미가 없어진다.
     */
    @Transactional(noRollbackFor = [IllegalArgumentException::class])
    fun verifyCode(rawEmail: String, rawCode: String) {
        val email = normalize(rawEmail)
        val code = rawCode.trim()

        val record = verifications.findById(email).orElse(null)
            ?: throw IllegalArgumentException("먼저 인증번호를 받아 주세요.")
        require(!record.isCodeExpired()) { "인증번호가 만료됐어요. 다시 받아 주세요." }
        require(record.attempts < EmailVerification.MAX_ATTEMPTS) {
            "인증 시도 횟수를 넘겼어요. 인증번호를 다시 받아 주세요."
        }

        if (!encoder.matches(code, record.codeHash)) {
            record.attempts += 1
            verifications.save(record)
            val left = EmailVerification.MAX_ATTEMPTS - record.attempts
            throw IllegalArgumentException(
                if (left > 0) "인증번호가 맞지 않아요. (${left}번 더 시도할 수 있어요)"
                else "인증 시도 횟수를 넘겼어요. 인증번호를 다시 받아 주세요."
            )
        }

        record.verifiedAt = Instant.now()
        verifications.save(record)
    }

    /** 가입 직전 호출 — 인증을 마친 이메일인지 확인하고 기록을 소비한다 */
    @Transactional
    fun consumeVerified(email: String) {
        val record = verifications.findById(email).orElse(null)
            ?: throw IllegalArgumentException("이메일 인증을 먼저 해주세요.")
        require(record.isUsable()) { "이메일 인증이 만료됐어요. 인증번호를 다시 받아 주세요." }
        verifications.delete(record)
    }

    private fun normalize(email: String) = email.trim().lowercase()

    companion object {
        val EMAIL_REGEX = Regex("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")
    }
}
