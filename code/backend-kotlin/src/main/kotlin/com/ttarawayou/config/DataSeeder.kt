package com.ttarawayou.config

import com.ttarawayou.entity.Wallet
import com.ttarawayou.repository.WalletRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

/**
 * 서버 시작 시 테스트 지갑(500,000 토큰)을 생성한다.
 * 장소 데이터는 TMAP POI 실시간 조회 → 실패 시 충남·대전 시드(SeedPlaceService)로 공급한다.
 */
@Component
class DataSeeder(private val walletRepository: WalletRepository) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(DataSeeder::class.java)

    override fun run(vararg args: String) {
        if (walletRepository.count() == 0L) {
            walletRepository.save(Wallet(1L, Wallet.INITIAL_BALANCE))
            log.info("테스트 지갑 생성: {} 토큰", Wallet.INITIAL_BALANCE)
        }
    }
}
