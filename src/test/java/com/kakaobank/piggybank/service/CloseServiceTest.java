package com.kakaobank.piggybank.service;

import com.kakaobank.piggybank.domain.Account;
import com.kakaobank.piggybank.domain.PiggyBank;
import com.kakaobank.piggybank.dto.response.CloseResponse;
import com.kakaobank.piggybank.exception.PiggyBankBusinessException;
import com.kakaobank.piggybank.repository.AccountRepository;
import com.kakaobank.piggybank.repository.PiggyBankDetailRepository;
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
 * 1.4 해지 — 플로우차트 코멘트.txt 1~6단계 + 추가(재가입 가능성)를 검증한다.
 * 클래스 레벨 @Transactional: 각 테스트가 끝나면 자동 롤백되어 DB가 깨끗한 상태로 유지된다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestClockConfig.class)
@Transactional
class CloseServiceTest {

    @Autowired
    private CloseService closeService;
    @Autowired
    private TestFixtures fixtures;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PiggyBankRepository piggyBankRepository;
    @Autowired
    private PiggyBankDetailRepository piggyBankDetailRepository;

    @Test
    void 해지하면_잔액이_근거계좌로_이체되고_계좌와_저금통이_동시에_해지된다() {
        fixtures.customer("C-CUS-001", LocalDate.of(1990, 1, 1));
        fixtures.ddaAccount("C-ACT-001", "C-CUS-001", BigDecimal.valueOf(500_000));
        fixtures.existingPiggyBank("C-PIG-001", "C-ACT-001", "C-CUS-001", BigDecimal.valueOf(30_000));

        CloseResponse response = closeService.close("C-PIG-001");

        assertThat(response.settledAmount()).isEqualByComparingTo(BigDecimal.valueOf(30_000));
        assertThat(response.deactivatedServiceCount()).isEqualTo(1); // COIN 서비스 하나

        PiggyBank piggyBank = piggyBankRepository.findById("C-PIG-001").orElseThrow();
        assertThat(piggyBank.isActive()).isFalse();
        assertThat(piggyBank.getCnclDt()).isEqualTo(TestClockConfig.FIXED_TODAY);

        Account pigAccount = accountRepository.findById("C-PIG-001").orElseThrow();
        assertThat(pigAccount.isActive()).isFalse();

        Account rtAccount = accountRepository.findById("C-ACT-001").orElseThrow();
        assertThat(rtAccount.getBalAmt()).isEqualByComparingTo(BigDecimal.valueOf(530_000));

        assertThat(piggyBankDetailRepository.findLatest("C-PIG-001", "COIN"))
                .hasValueSatisfying(d -> assertThat(d.isUsing()).isFalse()); // 서비스 OFF 이력이 새로 남음
    }

    @Test
    void 잔액이_0원이면_이체는_생략되지만_해지와_거래일_갱신은_수행된다() {
        fixtures.customer("C-CUS-002", LocalDate.of(1990, 1, 1));
        fixtures.ddaAccount("C-ACT-002", "C-CUS-002", BigDecimal.valueOf(500_000));
        fixtures.existingPiggyBank("C-PIG-002", "C-ACT-002", "C-CUS-002", BigDecimal.ZERO);

        CloseResponse response = closeService.close("C-PIG-002");

        assertThat(response.settledAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        Account pigAccount = accountRepository.findById("C-PIG-002").orElseThrow();
        assertThat(pigAccount.isActive()).isFalse();
        assertThat(pigAccount.getLastTrxDt()).isEqualTo(TestClockConfig.FIXED_TODAY);
    }

    @Test
    void 지급제한이_걸려있으면_해지가_거부된다() {
        fixtures.customer("C-CUS-003", LocalDate.of(1990, 1, 1));
        fixtures.ddaAccount("C-ACT-003", "C-CUS-003", BigDecimal.valueOf(500_000));
        fixtures.existingPiggyBank("C-PIG-003", "C-ACT-003", "C-CUS-003", BigDecimal.valueOf(10_000));
        fixtures.restriction("C-PIG-003");

        assertThatThrownBy(() -> closeService.close("C-PIG-003"))
                .isInstanceOf(PiggyBankBusinessException.class)
                .satisfies(e -> assertThat(((PiggyBankBusinessException) e).getReasonCode()).isEqualTo("PAYMENT_RESTRICTED"));
    }

    @Test
    void 이미_해지된_저금통을_다시_해지하면_거부된다() {
        fixtures.customer("C-CUS-004", LocalDate.of(1990, 1, 1));
        fixtures.ddaAccount("C-ACT-004", "C-CUS-004", BigDecimal.valueOf(500_000));
        fixtures.existingPiggyBank("C-PIG-004", "C-ACT-004", "C-CUS-004", BigDecimal.valueOf(5_000));

        closeService.close("C-PIG-004");

        assertThatThrownBy(() -> closeService.close("C-PIG-004"))
                .isInstanceOf(PiggyBankBusinessException.class)
                .satisfies(e -> assertThat(((PiggyBankBusinessException) e).getReasonCode()).isEqualTo("ALREADY_CLOSED"));
    }

    @Test
    void 해지해도_근거계좌는_해지되지_않아_같은_근거계좌로_재가입이_가능하다() {
        fixtures.customer("C-CUS-005", LocalDate.of(1990, 1, 1));
        fixtures.ddaAccount("C-ACT-005", "C-CUS-005", BigDecimal.valueOf(500_000));
        fixtures.existingPiggyBank("C-PIG-005", "C-ACT-005", "C-CUS-005", BigDecimal.valueOf(5_000));

        closeService.close("C-PIG-005");

        Account rtAccount = accountRepository.findById("C-ACT-005").orElseThrow();
        assertThat(rtAccount.isActive()).isTrue(); // 근거계좌 자체는 살아있음
    }
}
