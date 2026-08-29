package com.kakaobank.piggybank.support;

import com.kakaobank.piggybank.domain.Customer;
import com.kakaobank.piggybank.domain.DdaAccount;
import com.kakaobank.piggybank.domain.PaymentRestriction;
import com.kakaobank.piggybank.domain.PiggyBank;
import com.kakaobank.piggybank.domain.PiggyBankAccount;
import com.kakaobank.piggybank.domain.PiggyBankDetail;
import com.kakaobank.piggybank.repository.AccountRepository;
import com.kakaobank.piggybank.repository.CustomerRepository;
import com.kakaobank.piggybank.repository.PaymentRestrictionRepository;
import com.kakaobank.piggybank.repository.PiggyBankDetailRepository;
import com.kakaobank.piggybank.repository.PiggyBankRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 테스트 시나리오 데이터를 손쉽게 구성하기 위한 헬퍼. 실제 서비스 로직은 절대 거치지 않고
 * 리포지토리에 직접 원하는 상태를 심는다 (예: 이미 존재하는 저금통, 지급제한 등). */
@Component
public class TestFixtures {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PiggyBankRepository piggyBankRepository;
    private final PiggyBankDetailRepository piggyBankDetailRepository;
    private final PaymentRestrictionRepository paymentRestrictionRepository;

    public TestFixtures(CustomerRepository customerRepository, AccountRepository accountRepository,
                         PiggyBankRepository piggyBankRepository, PiggyBankDetailRepository piggyBankDetailRepository,
                         PaymentRestrictionRepository paymentRestrictionRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.piggyBankRepository = piggyBankRepository;
        this.piggyBankDetailRepository = piggyBankDetailRepository;
        this.paymentRestrictionRepository = paymentRestrictionRepository;
    }

    public Customer customer(String cusNo, LocalDate birthDate) {
        return customerRepository.save(new Customer(cusNo, birthDate));
    }

    public DdaAccount ddaAccount(String acno, String cusNo, BigDecimal balance) {
        DdaAccount a = new DdaAccount(acno, cusNo, LocalDate.of(2024, 1, 10));
        a.seedBalance(balance);
        accountRepository.save(a);
        return a;
    }

    public DdaAccount ddaAccount(String acno, String cusNo, BigDecimal balance, boolean groupAccount) {
        DdaAccount a = new DdaAccount(acno, cusNo, LocalDate.of(2024, 1, 10));
        a.seedBalance(balance);
        if (groupAccount) {
            a.markAsGroupAccount();
        }
        accountRepository.save(a);
        return a;
    }

    /** 이미 개설되어 있는 저금통(ACT_MAS의 PIG 서브타입 + PIG_MAS + PIG_DTL)을 한 번에 만든다. */
    public PiggyBank existingPiggyBank(String pigAcno, String rtAcno, String cusNo, BigDecimal balance) {
        PiggyBankAccount pigAccount = new PiggyBankAccount(pigAcno, cusNo, LocalDate.of(2024, 6, 1));
        pigAccount.seedBalance(balance);
        accountRepository.save(pigAccount);

        PiggyBank piggyBank = new PiggyBank(pigAcno, rtAcno, cusNo, LocalDate.of(2024, 6, 1));
        piggyBank.syncBalance(balance);
        piggyBankRepository.save(piggyBank);

        piggyBankDetailRepository.save(
                PiggyBankDetail.activate(pigAcno, PiggyBankDetail.SVC_COIN, LocalDateTime.of(2024, 6, 1, 9, 0)));

        return piggyBank;
    }

    public void restriction(String acno) {
        paymentRestrictionRepository.save(new PaymentRestriction(acno, "PLEDGE", LocalDate.of(2026, 1, 1)));
    }
}
