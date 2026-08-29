package com.kakaobank.piggybank.web;

import com.kakaobank.piggybank.domain.Account;
import com.kakaobank.piggybank.domain.Customer;
import com.kakaobank.piggybank.dto.request.CreateCustomerRequest;
import com.kakaobank.piggybank.dto.request.CreateDdaAccountRequest;
import com.kakaobank.piggybank.dto.response.AccountView;
import com.kakaobank.piggybank.exception.BusinessErrors;
import com.kakaobank.piggybank.repository.AccountRepository;
import com.kakaobank.piggybank.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시연용 테스트 데이터 생성 + 계좌 조회.
 * 고객/근거계좌 개설, 지급제한 등록은 이 4개 플로우(1.1~1.4)의 범위 밖이지만
 * 로컬 시연을 위해 최소 기능으로 제공한다 (README "데모 시나리오" 참고).
 */
@RestController
@RequestMapping("/api")
public class AdminController {

    private final AdminService adminService;
    private final AccountRepository accountRepository;

    public AdminController(AdminService adminService, AccountRepository accountRepository) {
        this.adminService = adminService;
        this.accountRepository = accountRepository;
    }

    @PostMapping("/admin/customers")
    public ResponseEntity<Customer> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createCustomer(request));
    }

    @PostMapping("/admin/dda-accounts")
    public ResponseEntity<AccountView> createDdaAccount(@Valid @RequestBody CreateDdaAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createDdaAccount(request));
    }

    @PostMapping("/admin/accounts/{acno}/restrictions")
    public ResponseEntity<Void> registerRestriction(@PathVariable String acno,
                                                      @RequestParam(defaultValue = "PLEDGE") String rstTypeCd) {
        adminService.registerPaymentRestriction(acno, rstTypeCd);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/accounts/{acno}")
    public AccountView getAccount(@PathVariable String acno) {
        Account account = accountRepository.findById(acno)
                .orElseThrow(() -> BusinessErrors.accountNotFound(acno));
        return AccountView.from(account);
    }
}
