package com.trevit.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

/**
 * 인증 메일 발송. SMTP(spring.mail.host)가 설정돼 있으면 실제로 보내고,
 * 없으면 코드를 서버 로그에 찍어 개발·시연이 막히지 않게 한다.
 * (TMAP·LM Studio 처럼 키가 없으면 폴백하는 이 프로젝트의 방식과 같다)
 */
@Service
class MailService(
    private val senderProvider: ObjectProvider<JavaMailSender>,
    @Value("\${spring.mail.host:}") private val host: String,
    @Value("\${trevit.mail.from:트레빗 <no-reply@trevit.app>}") private val from: String,
) {
    private val log = LoggerFactory.getLogger(MailService::class.java)

    fun configured(): Boolean = host.isNotBlank() && senderProvider.ifAvailable != null

    /**
     * 인증코드 메일.
     * - SMTP 미설정(개발·시연): 코드를 서버 로그에 찍는다
     * - SMTP 설정됨 + 발송 실패: [MailDeliveryException] 을 던진다.
     *   예전엔 실패해도 로그만 남기고 "보냈다"고 응답해서, 메일이 9일간 안 나가는 걸 아무도 몰랐다.
     */
    fun sendVerificationCode(email: String, code: String, ttlMinutes: Long, passwordReset: Boolean = false) {
        val sender = if (host.isNotBlank()) senderProvider.ifAvailable else null
        if (sender == null) {
            logFallback(email, code)
            return
        }
        try {
            sender.send(
                SimpleMailMessage().apply {
                    setFrom(from)
                    setTo(email)
                    subject = if (passwordReset) "[트레빗] 비밀번호 재설정 인증번호 $code" else "[트레빗] 인증번호 $code"
                    text = """
                        안녕하세요, 트레빗입니다.

                        아래 인증번호를 ${if (passwordReset) "비밀번호 찾기" else "회원가입"} 화면에 입력해 주세요.

                            $code

                        인증번호는 ${ttlMinutes}분 뒤에 만료됩니다.
                        본인이 요청하지 않았다면 이 메일은 무시하셔도 됩니다.
                    """.trimIndent()
                }
            )
            log.info("인증코드 메일 발송 완료 — {}", maskEmail(email))
        } catch (e: Exception) {
            // ERROR 로 남겨 로그 모니터링에 걸리게 하고, 사용자에게도 실패를 그대로 알린다
            log.error("인증코드 메일 발송 실패 — {} ({})", maskEmail(email), e.message)
            throw MailDeliveryException(
                "인증 메일을 보내지 못했어요. 잠시 후 다시 시도하거나, 초대코드가 있으면 초대코드로 가입해 주세요.", e,
            )
        }
    }

    private fun logFallback(email: String, code: String) {
        log.warn("[메일 미설정] {} 인증코드: {} — SMTP를 설정하면 실제 메일로 발송됩니다", maskEmail(email), code)
    }

    /** 로그에 이메일 전체를 남기지 않는다 */
    private fun maskEmail(email: String): String {
        val at = email.indexOf('@')
        if (at <= 1) return "***"
        return email.first() + "***" + email.substring(at)
    }
}

/** SMTP 로 메일을 보내지 못함 → 503 + 사용자에게 보여줄 메시지 */
class MailDeliveryException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
