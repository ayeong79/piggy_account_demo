package com.kakaobank.piggybank.service;

import com.kakaobank.piggybank.domain.Account;
import com.kakaobank.piggybank.domain.DdaAccount;
import com.kakaobank.piggybank.domain.PiggyBank;
import com.kakaobank.piggybank.dto.response.EmptyResponse;
import com.kakaobank.piggybank.exception.PiggyBankBusinessException;
import com.kakaobank.piggybank.repository.AccountRepository;
import com.kakaobank.piggybank.repository.PiggyBankRepository;
import com.kakaobank.piggybank.support.TestClockConfig;
import com.kakaobank.piggybank.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 1.2 저금통비우기 — 플로우차트 코멘트.txt 1~6단계를 검증한다.
 * 클래스 레벨 @Transactional: 각 테스트가 끝나면 자동 롤백되어 DB가 깨끗한 상태로 유지된다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestClockConfig.class)
@Transactional
class EmptyServiceTest {

    @Autowired
    private EmptyService emptyService;
    @Autowired
    private TestFixtures fixtures;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PiggyBankRepository piggyBankRepository;

    @Test
    void 비우기_성공하면_저금통_잔액_전액이_근거계좌로_이체된다() {
        fixtures.customer("E-CUS-001", LocalDate.of(1990, 1, 1));
        fixtures.ddaAccount("E-ACT-001", "E-CUS-001", BigDecimal.valueOf(1_000_000));
        fixtures.existingPiggyBank("E-PIG-001", "E-ACT-001", "E-CUS-001", BigDecimal.valueOf(45_000));

        EmptyResponse response = emptyService.empty("E-PIG-001");

        assertThat(response.transferredAmount()).isEqualByComparingTo(BigDecimal.valueOf(45_000));
        assertThat(response.pigBalanceAfter()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.rtBalanceAfter()).isEqualByComparingTo(BigDecimal.valueOf(1_045_000));

        PiggyBank piggyBank = piggyBankRepository.findById("E-PIG-001").orElseThrow();
        assertThat(piggyBank.getBalAmt()).isEqualByComparingTo(BigDecimal.ZERO); // PIG_MAS 캐시도 함께 동기화
    }

    @Test
    void 잔액이_0원이어도_비우기_자체는_성공하고_거래일만_갱신된다() {
        fixtures.customer("E-CUS-002", LocalDate.of(1990, 1, 1));
        fixtures.ddaAccount("E-ACT-002", "E-CUS-002", BigDecimal.valueOf(500_000));
        fixtures.existingPiggyBank("E-PIG-002", "E-ACT-002", "E-CUS-002", BigDecimal.ZERO);

        EmptyResponse response = emptyService.empty("E-PIG-002");

        assertThat(response.transferredAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.rtBalanceAfter()).isEqualByComparingTo(BigDecimal.valueOf(500_000));

        Account pigAccount = accountRepository.findById("E-PIG-002").orElseThrow();
        assertThat(pigAccount.getLastTrxDt()).isEqualTo(TestClockConfig.FIXED_TODAY);
    }

    @Test
    void 지급제한이_걸려있으면_비우기가_거부된다() {
        fixtures.customer("E-CUS-003", LocalDate.of(1990, 1, 1));
        fixtures.ddaAccount("E-ACT-003", "E-CUS-003", BigDecimal.valueOf(500_000));
        fixtures.existingPiggyBank("E-PIG-003", "E-ACT-003", "E-CUS-003", BigDecimal.valueOf(20_000));
        fixtures.restriction("E-PIG-003");

        assertThatThrownBy(() -> emptyService.empty("E-PIG-003"))
                .isInstanceOf(PiggyBankBusinessException.class)
                .satisfies(e -> assertThat(((PiggyBankBusinessException) e).getReasonCode()).isEqualTo("PAYMENT_RESTRICTED"));
    }

    @Test
    void 이자지급이_정지된_상태였다면_비우기_전에_미정산_이자를_정산하고_재개한다() {
        fixtures.customer("E-CUS-004", LocalDate.of(1990, 1, 1));
        fixtures.ddaAccount("E-ACT-004", "E-CUS-004", BigDecimal.valueOf(500_000));
        PiggyBank piggyBank = fixtures.existingPiggyBank("E-PIG-004", "E-ACT-004", "E-CUS-004", BigDecimal.valueOf(100_000));

        // ISTOP_YN='Y'로 강제 전환 (5년 이상 무거래로 이자정지된 상태를 흉내)
        Account pigAccount = accountRepository.findById("E-PIG-004").orElseThrow();
        pigAccount.suspendInterestIfDormant();
        accountRepository.save(pigAccount);

        EmptyResponse response = emptyService.empty("E-PIG-004");

        // 개설일(2024-06-01)부터 정산기준일(2026-08-27)까지 기간에 대해 4% 단리로 이자가 발생해야 한다.
        assertThat(response.interestSettled()).isGreaterThan(BigDecimal.ZERO);

        Account after = accountRepository.findById("E-PIG-004").orElseThrow();
        assertThat(after.isInterestSuspended()).isFalse();
        // 비우기는 "이자까지 반영된 잔액"을 전액 이체하므로 정산 후 잔액은 다시 0이어야 한다.
        assertThat(after.getBalAmt()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
