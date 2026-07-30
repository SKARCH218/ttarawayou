package com.trevit.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 토큰 지갑 (테스트용 단일 지갑, id=1).
 * 토큰과 현금은 1:1 (10,000원 = 10,000토큰). 플랜 생성 시 예상 총비용만큼 차감된다.
 */
@Entity
@Table(name = "wallets")
class Wallet(
    @Id
    var id: Long? = null,
    var balance: Long = 0,
) {
    companion object {
        const val INITIAL_BALANCE = 500_000L
    }
}
