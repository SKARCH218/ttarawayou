package com.mysterytrip.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 토큰 지갑 (테스트용 단일 지갑, id=1).
 * 토큰과 현금은 1:1 (10,000원 = 10,000토큰). 플랜 생성 시 예상 총비용만큼 차감된다.
 */
@Entity
@Table(name = "wallets")
public class Wallet {

    public static final long INITIAL_BALANCE = 500_000L;

    @Id
    private Long id;

    private long balance;

    protected Wallet() {}

    public Wallet(Long id, long balance) {
        this.id = id;
        this.balance = balance;
    }

    public Long getId() { return id; }
    public long getBalance() { return balance; }
    public void setBalance(long balance) { this.balance = balance; }
}
