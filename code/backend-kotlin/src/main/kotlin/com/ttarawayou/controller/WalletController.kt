package com.ttarawayou.controller

import com.ttarawayou.entity.Wallet
import com.ttarawayou.repository.WalletRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/wallet")
class WalletController(private val repository: WalletRepository) {

    private fun wallet(): Wallet = repository.findById(1L)
        .orElseGet { repository.save(Wallet(1L, Wallet.INITIAL_BALANCE)) }

    /** GET /api/wallet — 보유 토큰 조회 */
    @GetMapping
    fun getWallet(): Map<String, Long> = mapOf("balance" to wallet().balance)

    /** POST /api/wallet/reset — 테스트용 토큰 충전 (500,000으로 초기화) */
    @PostMapping("/reset")
    fun reset(): Map<String, Long> {
        val w = wallet()
        w.balance = Wallet.INITIAL_BALANCE
        repository.save(w)
        return mapOf("balance" to w.balance)
    }
}
