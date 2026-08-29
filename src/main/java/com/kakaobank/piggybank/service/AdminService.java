package com.kakaobank.piggybank.service;

import com.kakaobank.piggybank.domain.Account;
import com.kakaobank.piggybank.domain.Customer;
import com.kakaobank.piggybank.domain.DdaAccount;
import com.kakaobank.piggybank.domain.PaymentRestriction;
import com.kakaobank.piggybank.dto.request.CreateCustomerRequest;
import com.kakaobank.piggybank.dto.request.CreateDdaAccountRequest;
import com.kakaobank.piggybank.dto.response.AccountView;
import com.kakaobank.piggybank.exception.BusinessErrors;
import com.kakaobank.piggybank.repository.AccountRepository;
import com.kakaobank.piggybank.repository.CustomerRepository;
import com.kakaobank.piggybank.repository.PaymentRestrictionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 시연/테스트를 위해 4개 플로우의 "전제조건"에 해당하는 데이터(고객, 근거계좌, 지급제한)를
 * 만들어주는 관리용 서비스. 실제 운영에서는 계좌개설/질권등록 등 별도 시스템이 담당하는
 * 영역이라 이 프로젝트의 4개 UML 플로우 범위 밖이지만, 로컬 시연을 위해 최소 기능으로 제공.
 */
@Service
public class AdminService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PaymentRestrictionRepository paymentRestrictionRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final Clock clock;

    public AdminService(CustomerRepository customerRepository,
                         AccountRepository accountRepository,
                         PaymentRestrictionRepository paymentRestrictionRepository,
                         AccountNumberGenerator accountNumberGenerator,
                         Clock clock) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.paymentRestrictionRepository = paymentRestrictionRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.clock = clock;
    }

    @Transactional
    public Customer createCustomer(CreateCustomerRequest request) {
        Customer customer = new Customer(request.cusNo(), request.birthDate());
        return customerRepository.save(customer);
    }

    @Transactional
    public AccountView createDdaAccount(CreateDdaAccountRequest request) {
        customerRepository.findById(request.cusNo())
                .orElseThrow(() -> BusinessErrors.customerNotFound(request.cusNo()));

        LocalDate today = LocalDate.now(clock);
        String acno = accountNumberGenerator.newAccountNumber();
        DdaAccount account = new DdaAccount(acno, request.cusNo(), today);
        account.seedBalance(request.initialBalance());
        if (request.groupAccount()) {
            account.markAsGroupAccount();
        }
        accountRepository.save(account);
        return AccountView.from(account);
    }

    @Transactional
    public void registerPaymentRestriction(String acno, String rstTypeCd) {
        accountRepository.findById(acno).orElseThrow(() -> BusinessErrors.accountNotFound(acno));
        paymentRestrictionRepository.save(new PaymentRestriction(acno, rstTypeCd, LocalDate.now(clock)));
    }
}
