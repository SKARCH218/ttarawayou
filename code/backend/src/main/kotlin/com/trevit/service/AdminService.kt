package com.trevit.service

import com.trevit.dto.AdminDtos.AdminStatus
import com.trevit.dto.AdminDtos.AdminUser
import com.trevit.dto.AdminDtos.Counts
import com.trevit.dto.AdminDtos.DbStatus
import com.trevit.dto.AdminDtos.Integrations
import com.trevit.dto.AdminDtos.LlmStatus
import com.trevit.entity.Wallet
import com.trevit.repository.AuthTokenRepository
import com.trevit.repository.EmailVerificationRepository
import com.trevit.repository.UserRepository
import com.trevit.repository.WalletRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.net.URI
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import javax.sql.DataSource

/**
 * 관리자 대시보드용 상태 조회 + 유저/지갑 조정.
 * 외부 API(TMAP·ODsay·data.go.kr)는 키 설정 여부만 보여준다 — 실제로 호출하면
 * 쿼터를 쓰게 되므로 여기서는 호출하지 않는다.
 */
@Service
class AdminService(
    private val dataSource: DataSource,
    private val userRepository: UserRepository,
    private val authTokenRepository: AuthTokenRepository,
    private val emailVerificationRepository: EmailVerificationRepository,
    private val walletRepository: WalletRepository,
    @Value("\${lmstudio.base-url}") private val lmBaseUrl: String,
    @Value("\${lmstudio.model}") private val lmModel: String,
    @Value("\${lmstudio.enabled}") private val lmEnabled: Boolean,
    @Value("\${spring.mail.host:}") private val mailHost: String,
    @Value("\${google.client-id:}") private val googleClientId: String,
    @Value("\${tmap.app-key:}") private val tmapKey: String,
    @Value("\${odsay.api-key:}") private val odsayKey: String,
    @Value("\${datago.api-key:}") private val dataGoKrKey: String,
) {
    private val startedAt = Instant.now()
    private val http = RestClient.builder()
        .requestFactory(SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(2000)
            setReadTimeout(3000)
        })
        .build()

    fun status(): AdminStatus {
        val db = checkDb()
        val llm = checkLlm()
        return AdminStatus(
            db = db,
            llm = llm,
            integrations = Integrations(
                mail = mailHost.isNotBlank(),
                google = googleClientId.isNotBlank(),
                tmap = tmapKey.isNotBlank(),
                odsay = odsayKey.isNotBlank(),
                dataGoKr = dataGoKrKey.isNotBlank(),
            ),
            counts = Counts(
                users = userRepository.count(),
                authTokens = authTokenRepository.count(),
                pendingEmailVerifications = emailVerificationRepository.count(),
            ),
            walletBalance = wallet().balance,
            plansGeneratedSinceStart = plansGenerated.get(),
            uptimeSeconds = Instant.now().epochSecond - startedAt.epochSecond,
        )
    }

    fun users(): List<AdminUser> = userRepository.findAll()
        .sortedByDescending { it.id }
        .map { AdminUser(it.id ?: -1, it.email, it.nickname, it.provider.name, it.createdAt.toString()) }

    /** 유저 + 로그인 세션을 함께 지운다 (JPA cascade 를 걸어두지 않아서 직접 정리) */
    fun deleteUser(id: Long) {
        authTokenRepository.deleteByUserId(id)
        userRepository.deleteById(id)
    }

    fun setWallet(balance: Long): Long {
        require(balance >= 0) { "잔액은 0 이상이어야 해요." }
        val w = wallet()
        w.balance = balance
        return walletRepository.save(w).balance
    }

    private fun wallet(): Wallet = walletRepository.findById(1L)
        .orElseGet { walletRepository.save(Wallet(1L, Wallet.INITIAL_BALANCE)) }

    private fun checkDb(): DbStatus {
        val t0 = System.currentTimeMillis()
        val up = try {
            dataSource.connection.use { it.isValid(2) }
        } catch (e: Exception) {
            false
        }
        val url = try {
            dataSource.connection.use { it.metaData.url }
        } catch (e: Exception) {
            "알 수 없음"
        }
        return DbStatus(up = up, latencyMs = System.currentTimeMillis() - t0, url = url)
    }

    private fun checkLlm(): LlmStatus {
        if (!lmEnabled) return LlmStatus(enabled = false, reachable = false, latencyMs = 0, baseUrl = lmBaseUrl, model = lmModel)
        val t0 = System.currentTimeMillis()
        val reachable = try {
            http.get().uri(URI.create(lmBaseUrl.trimEnd('/') + "/models")).retrieve().toBodilessEntity()
            true
        } catch (e: Exception) {
            false
        }
        return LlmStatus(
            enabled = true, reachable = reachable,
            latencyMs = System.currentTimeMillis() - t0,
            baseUrl = lmBaseUrl, model = lmModel,
        )
    }

    companion object {
        /** PlanService가 플랜을 성공적으로 만들 때마다 올린다. 재시작하면 0으로 돌아간다 */
        val plansGenerated = AtomicLong()
    }
}
