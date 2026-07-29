package com.mysterytrip.config;

import com.mysterytrip.entity.Wallet;
import com.mysterytrip.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 서버 시작 시 테스트 지갑(500,000 토큰)을 생성한다.
 * 장소 데이터는 시드 없이 TMAP POI로 실시간 조회한다 (전국 대응).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final WalletRepository walletRepository;

    public DataSeeder(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public void run(String... args) {
        if (walletRepository.count() == 0) {
            walletRepository.save(new Wallet(1L, Wallet.INITIAL_BALANCE));
            log.info("테스트 지갑 생성: {} 토큰", Wallet.INITIAL_BALANCE);
        }
    }
}
