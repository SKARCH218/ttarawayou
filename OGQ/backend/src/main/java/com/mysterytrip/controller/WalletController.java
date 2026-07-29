package com.mysterytrip.controller;

import com.mysterytrip.entity.Wallet;
import com.mysterytrip.repository.WalletRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletRepository repository;

    public WalletController(WalletRepository repository) {
        this.repository = repository;
    }

    private Wallet wallet() {
        return repository.findById(1L)
                .orElseGet(() -> repository.save(new Wallet(1L, Wallet.INITIAL_BALANCE)));
    }

    /** GET /api/wallet — 보유 토큰 조회 */
    @GetMapping
    public Map<String, Long> getWallet() {
        return Map.of("balance", wallet().getBalance());
    }

    /** POST /api/wallet/reset — 테스트용 토큰 충전 (500,000으로 초기화) */
    @PostMapping("/reset")
    public Map<String, Long> reset() {
        Wallet w = wallet();
        w.setBalance(Wallet.INITIAL_BALANCE);
        repository.save(w);
        return Map.of("balance", w.getBalance());
    }
}
