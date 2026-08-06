package com.trevit.dto

object AdminDtos {

    data class DbStatus(val up: Boolean, val latencyMs: Long, val url: String)
    data class LlmStatus(
        val enabled: Boolean,
        val reachable: Boolean,
        val latencyMs: Long,
        val baseUrl: String,
        val model: String,
    )

    /** 외부 연동은 키가 설정돼 있는지만 알려준다 — 실제 호출은 쿼터를 쓰므로 하지 않는다 */
    data class Integrations(
        val mail: Boolean,
        val google: Boolean,
        val tmap: Boolean,
        val odsay: Boolean,
        val dataGoKr: Boolean,
    )

    data class Counts(val users: Long, val authTokens: Long, val pendingEmailVerifications: Long)

    data class AdminStatus(
        val db: DbStatus,
        val llm: LlmStatus,
        val integrations: Integrations,
        val counts: Counts,
        val walletBalance: Long,
        val plansGeneratedSinceStart: Long,
        val uptimeSeconds: Long,
    )

    data class AdminUser(
        val id: Long,
        val email: String,
        val nickname: String,
        val provider: String,
        val createdAt: String,
    )

    data class SetWalletRequest(val balance: Long)
}
