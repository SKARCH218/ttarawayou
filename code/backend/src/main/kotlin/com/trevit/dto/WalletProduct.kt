package com.trevit.dto

/**
 * 토큰 구매 상품. 고정환율제라 가격(원)과 토큰 수가 항상 같다 — 보너스 토큰은 없다.
 * 실제 결제(PG) 연동 전이라, 클라이언트가 보낸 금액을 그대로 믿지 않고
 * 서버에 정의된 상품 중에서만 고르게 한다.
 */
enum class WalletProduct(val tokens: Long, val badge: String? = null) {
    TOKEN_30K(30_000),
    TOKEN_50K(50_000, "인기"),
    TOKEN_100K(100_000),
    TOKEN_300K(300_000, "최대"),
    ;

    val id: String get() = name.lowercase()

    companion object {
        fun byId(id: String): WalletProduct? = entries.find { it.id == id }
    }
}
