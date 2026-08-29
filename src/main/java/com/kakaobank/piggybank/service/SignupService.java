package com.kakaobank.piggybank.service;

import com.kakaobank.piggybank.domain.Account;
import com.kakaobank.piggybank.domain.Customer;
import com.kakaobank.piggybank.domain.DdaAccount;
import com.kakaobank.piggybank.domain.PiggyBank;
import com.kakaobank.piggybank.domain.PiggyBankAccount;
import com.kakaobank.piggybank.domain.PiggyBankDetail;
import com.kakaobank.piggybank.dto.request.SignupRequest;
import com.kakaobank.piggybank.dto.response.SignupResponse;
import com.kakaobank.piggybank.exception.BusinessErrors;
import com.kakaobank.piggybank.repository.AccountRepository;
import com.kakaobank.piggybank.repository.CustomerRepository;
import com.kakaobank.piggybank.repository.PaymentRestrictionRepository;
import com.kakaobank.piggybank.repository.PiggyBankDetailRepository;
import com.kakaobank.piggybank.repository.PiggyBankRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 1.1 신규가입 (플로우차트 코멘트.txt 1.1 신규가입, 1~5단계 + 추가).
 */
@Service
public class SignupService {

    private static final int MIN_AGE = 14;

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PiggyBankRepository piggyBankRepository;
    private final PiggyBankDetailRepository piggyBankDetailRepository;
    private final PaymentRestrictionRepository paymentRestrictionRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final Clock clock;

    public SignupService(CustomerRepository customerRepository,
                          AccountRepository accountRepository,
                          PiggyBankRepository piggyBankRepository,
                          PiggyBankDetailRepository piggyBankDetailRepository,
                          PaymentRestrictionRepository paymentRestrictionRepository,
                          AccountNumberGenerator accountNumberGenerator,
                          Clock clock) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.piggyBankRepository = piggyBankRepository;
        this.piggyBankDetailRepository = piggyBankDetailRepository;
        this.paymentRestrictionRepository = paymentRestrictionRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.clock = clock;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        LocalDate today = LocalDate.now(clock);

        // 1) 연령 검증 — 근거계좌 조회 없이 고객 정보만으로 가장 먼저 체크
        Customer customer = customerRepository.findById(request.cusNo())
                .orElseThrow(() -> BusinessErrors.customerNotFound(request.cusNo()));
        if (!customer.isEligibleAge(today, MIN_AGE)) {
            throw BusinessErrors.ageNotEligible(MIN_AGE);
        }

        // 2) 중복가입 방지 — 활성 저금통 존재 여부 (DB 유니크 제약이 최후 방어선, schema.sql 주석 참고)
        if (piggyBankRepository.existsActiveByCusNo(customer.getCusNo())) {
            throw BusinessErrors.duplicateSignup();
        }

        // 3) 근거계좌 상품검증 — ACCD='DDA'
        Account rtAccount = accountRepository.findByIdForUpdate(request.rtAcno())
                .orElseThrow(() -> BusinessErrors.accountNotFound(request.rtAcno()));
        if (!(rtAccount instanceof DdaAccount)) {
            throw BusinessErrors.notDdaAccount();
        }

        // 추가) 근거계좌 지급제한/휴면/해지 여부 사전 검증 (전제조건이지만 데모에서는 방어적으로 재확인)
        if (!rtAccount.isActive() || rtAccount.isDormant()
                || paymentRestrictionRepository.existsActiveByAcno(rtAccount.getAcno())) {
            throw BusinessErrors.baseAccountNotActive();
        }

        // 4) 모임통장 제외
        if (rtAccount.isGroupAccount()) {
            throw BusinessErrors.groupAccountRestricted();
        }
        // 같은 근거계좌로 이미 활성 저금통이 있는지도 함께 확인 (UX_PIG_MAS_RTACNO_ACTV에 대응)
        if (piggyBankRepository.existsActiveByRtAcno(rtAccount.getAcno())) {
            throw BusinessErrors.duplicateSignup();
        }

        // 5) 저금통 계좌 개설 — ACT_MAS → PIG_MAS → PIG_DTL 순서로 INSERT (한 트랜잭션)
        String pigAcno = accountNumberGenerator.newAccountNumber();

        PiggyBankAccount piggyBankAccount = new PiggyBankAccount(pigAcno, customer.getCusNo(), today);
        accountRepository.save(piggyBankAccount);

        PiggyBank piggyBank = new PiggyBank(pigAcno, rtAccount.getAcno(), customer.getCusNo(), today);
        piggyBankRepository.save(piggyBank);

        PiggyBankDetail detail = PiggyBankDetail.activate(pigAcno, PiggyBankDetail.SVC_COIN, LocalDateTime.now(clock));
        piggyBankDetailRepository.save(detail);

        return new SignupResponse(pigAcno, rtAccount.getAcno(), customer.getCusNo(), today);
    }
}
