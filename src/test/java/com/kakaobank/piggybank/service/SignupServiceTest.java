package com.kakaobank.piggybank.service;

import com.kakaobank.piggybank.domain.PiggyBank;
import com.kakaobank.piggybank.dto.request.SignupRequest;
import com.kakaobank.piggybank.dto.response.SignupResponse;
import com.kakaobank.piggybank.exception.PiggyBankBusinessException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 1.1 신규가입 — 플로우차트 코멘트.txt 1~5단계 + 추가(전제조건)를 각각 검증한다.
 * 클래스 레벨 @Transactional: 각 테스트가 끝나면 자동 롤백되어 DB가 깨끗한 상태로 유지된다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestClockConfig.class)
@Transactional
class SignupServiceTest {

    @Autowired
    private SignupService signupService;
    @Autowired
    private TestFixtures fixtures;
    @Autowired
    private PiggyBankRepository piggyBankRepository;
    @Autowired
    private PiggyBankDetailRepository piggyBankDetailRepository;

    @Test
    void 신규가입_성공하면_저금통계좌_저금통_동전모으기설정이_한번에_생성된다() {
        fixtures.customer("SU-CUS-001", LocalDate.of(1998, 5, 12)); // 성인
        fixtures.ddaAccount("SU-ACT-001", "SU-CUS-001", BigDecimal.valueOf(1_000_000));

        SignupResponse response = signupService.signup(new SignupRequest("SU-CUS-001", "SU-ACT-001"));

        assertThat(response.rtAcno()).isEqualTo("SU-ACT-001");
        assertThat(response.cusNo()).isEqualTo("SU-CUS-001");
        assertThat(response.enrDt()).isEqualTo(TestClockConfig.FIXED_TODAY);

        Optional<PiggyBank> saved = piggyBankRepository.findById(response.pigAcno());
        assertThat(saved).isPresent();
        assertThat(saved.get().getBalAmt()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.get().isActive()).isTrue();

        assertThat(piggyBankDetailRepository.findLatest(response.pigAcno(), "COIN"))
                .hasValueSatisfying(d -> assertThat(d.isUsing()).isTrue());
    }

    @Test
    void 만14세_미만이면_연령검증에서_거부된다() {
        fixtures.customer("SU-CUS-002", LocalDate.of(2015, 1, 1)); // 2026-08-28 기준 11세
        fixtures.ddaAccount("SU-ACT-002", "SU-CUS-002", BigDecimal.valueOf(500_000));

        assertThatThrownBy(() -> signupService.signup(new SignupRequest("SU-CUS-002", "SU-ACT-002")))
                .isInstanceOf(PiggyBankBusinessException.class)
                .satisfies(e -> assertThat(((PiggyBankBusinessException) e).getReasonCode()).isEqualTo("AGE_NOT_ELIGIBLE"));
    }

    @Test
    void 이미_활성_저금통을_보유한_고객은_중복가입이_거부된다() {
        fixtures.customer("SU-CUS-003", LocalDate.of(1990, 3, 3));
        fixtures.ddaAccount("SU-ACT-003", "SU-CUS-003", BigDecimal.valueOf(2_000_000));
        fixtures.existingPiggyBank("SU-PIG-003", "SU-ACT-003", "SU-CUS-003", BigDecimal.valueOf(10_000));

        fixtures.ddaAccount("SU-ACT-003B", "SU-CUS-003", BigDecimal.valueOf(500_000));

        assertThatThrownBy(() -> signupService.signup(new SignupRequest("SU-CUS-003", "SU-ACT-003B")))
                .isInstanceOf(PiggyBankBusinessException.class)
                .satisfies(e -> assertThat(((PiggyBankBusinessException) e).getReasonCode()).isEqualTo("DUPLICATE_SIGNUP"));
    }

    @Test
    void 근거계좌가_DDA가_아니면_거부된다() {
        fixtures.customer("SU-CUS-004", LocalDate.of(1990, 1, 1));
        fixtures.ddaAccount("SU-ACT-004", "SU-CUS-004", BigDecimal.valueOf(1_000_000));
        PiggyBank other = fixtures.existingPiggyBank("SU-PIG-004", "SU-ACT-004", "SU-CUS-004", BigDecimal.ZERO);

        fixtures.customer("SU-CUS-004B", LocalDate.of(1990, 1, 1));

        assertThatThrownBy(() -> signupService.signup(new SignupRequest("SU-CUS-004B", other.getPigAcno())))
                .isInstanceOf(PiggyBankBusinessException.class)
                .satisfies(e -> assertThat(((PiggyBankBusinessException) e).getReasonCode()).isEqualTo("NOT_DDA_ACCOUNT"));
    }

    @Test
    void 모임통장_이용중인_근거계좌는_거부된다() {
        fixtures.customer("SU-CUS-005", LocalDate.of(1990, 1, 1));
        fixtures.ddaAccount("SU-ACT-005", "SU-CUS-005", BigDecimal.valueOf(1_000_000), true);

        assertThatThrownBy(() -> signupService.signup(new SignupRequest("SU-CUS-005", "SU-ACT-005")))
                .isInstanceOf(PiggyBankBusinessException.class)
                .satisfies(e -> assertThat(((PiggyBankBusinessException) e).getReasonCode()).isEqualTo("GROUP_ACCOUNT_RESTRICTED"));
    }

    @Test
    void 지급제한이_걸린_근거계좌는_거부된다() {
        fixtures.customer("SU-CUS-006", LocalDate.of(1990, 1, 1));
        fixtures.ddaAccount("SU-ACT-006", "SU-CUS-006", BigDecimal.valueOf(1_000_000));
        fixtures.restriction("SU-ACT-006");

        assertThatThrownBy(() -> signupService.signup(new SignupRequest("SU-CUS-006", "SU-ACT-006")))
                .isInstanceOf(PiggyBankBusinessException.class)
                .satisfies(e -> assertThat(((PiggyBankBusinessException) e).getReasonCode()).isEqualTo("BASE_ACCOUNT_NOT_ACTIVE"));
    }
}
