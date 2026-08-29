package com.kakaobank.piggybank.service;

import com.kakaobank.piggybank.repository.AccountRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 데모용 계좌번호/고객번호 채번기.
 * 실제 DDL에는 채번 규칙이 정의되어 있지 않아(ACNO/CUS_NO는 상위 시스템에서 발급된다고 가정),
 * 시연 편의를 위해 "110-####-####" 형식으로 무작위 채번하고 유일성만 재확인한다.
 */
@Component
public class AccountNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;

    public AccountNumberGenerator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public String newAccountNumber() {
        String candidate;
        int guard = 0;
        do {
            candidate = String.format("110-%04d-%04d", RANDOM.nextInt(10000), RANDOM.nextInt(10000));
            guard++;
        } while (accountRepository.existsById(candidate) && guard < 20);
        return candidate;
    }
}
