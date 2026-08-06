package com.trevit.controller

import com.trevit.dto.WalletDtos.ProductResponse
import com.trevit.dto.WalletDtos.PurchaseRequest
import com.trevit.dto.WalletProduct
import com.trevit.entity.Wallet
import com.trevit.repository.WalletRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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

    /** GET /api/wallet/products — 구매 가능한 토큰 상품 (고정환율 1토큰 = 1원) */
    @GetMapping("/products")
    fun products(): List<ProductResponse> =
        WalletProduct.entries.map { ProductResponse(it.id, it.tokens, it.badge) }

    /** POST /api/wallet/purchase — 상품 하나를 구매해 보유 토큰에 더한다 (결제 시뮬레이션) */
    @PostMapping("/purchase")
    fun purchase(@RequestBody req: PurchaseRequest): Map<String, Long> {
        val product = WalletProduct.byId(req.productId)
            ?: throw IllegalArgumentException("존재하지 않는 상품이에요.")
        val w = wallet()
        w.balance += product.tokens
        repository.save(w)
        return mapOf("balance" to w.balance)
    }

    /** POST /api/wallet/reset — 테스트용 토큰 충전 (500,000으로 초기화). UI 에는 더 이상 노출하지 않는다 */
    @PostMapping("/reset")
    fun reset(): Map<String, Long> {
        val w = wallet()
        w.balance = Wallet.INITIAL_BALANCE
        repository.save(w)
        return mapOf("balance" to w.balance)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun badRequest(e: IllegalArgumentException): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "잘못된 요청")))
}
