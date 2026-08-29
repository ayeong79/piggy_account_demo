package com.kakaobank.piggybank.service;

import com.kakaobank.piggybank.domain.Account;
import com.kakaobank.piggybank.domain.AccountTransaction;
import com.kakaobank.piggybank.domain.PiggyBank;
import com.kakaobank.piggybank.domain.PiggyBankDetail;
import com.kakaobank.piggybank.dto.response.CloseResponse;
import com.kakaobank.piggybank.exception.BusinessErrors;
import com.kakaobank.piggybank.repository.AccountRepository;
import com.kakaobank.piggybank.repository.PaymentRestrictionRepository;
import com.kakaobank.piggybank.repository.PiggyBankDetailRepository;
import com.kakaobank.piggybank.repository.PiggyBankRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 1.4 해지 (플로우차트 코멘트.txt 1.4 해지, 1~6단계 + 추가).
 */
@Service
public class CloseService {

    private final PiggyBankRepository piggyBankRepository;
    private final AccountRepository accountRepository;
    private final PaymentRestrictionRepository paymentRestrictionRepository;
    private final PiggyBankDetailRepository piggyBankDetailRepository;
    private final TransferService transferService;
    private final InterestService interestService;
    private final Clock clock;

    public CloseService(PiggyBankRepository piggyBankRepository,
                         AccountRepository accountRepository,
                         PaymentRestrictionRepository paymentRestrictionRepository,
                         PiggyBankDetailRepository piggyBankDetailRepository,
                         TransferService transferService,
                         InterestService interestService,
                         Clock clock) {
        this.piggyBankRepository = piggyBankRepository;
        this.accountRepository = accountRepository;
        this.paymentRestrictionRepository = paymentRestrictionRepository;
        this.piggyBankDetailRepository = piggyBankDetailRepository;
        this.transferService = transferService;
        this.interestService = interestService;
        this.clock = clock;
    }

    @Transactional
    public CloseResponse close(String pigAcno) {
        LocalDate today = LocalDate.now(clock);

        // 1) 지급제한 확인 — ACTV 행이 있으면 해지 자체를 막음
        if (paymentRestrictionRepository.existsActiveByAcno(pigAcno)) {
            throw BusinessErrors.paymentRestricted();
        }

        PiggyBank piggyBank = piggyBankRepository.findByIdForUpdate(pigAcno)
                .orElseThrow(() -> BusinessErrors.piggyBankNotFound(pigAcno));
        if (!piggyBank.isActive()) {
            throw BusinessErrors.alreadyClosed();
        }
        Account pigAccount = accountRepository.findByIdForUpdate(pigAcno)
                .orElseThrow(() -> BusinessErrors.piggyBankNotFound(pigAcno));
        Account rtAccount = accountRepository.findByIdForUpdate(piggyBank.getRtAcno())
                .orElseThrow(() -> BusinessErrors.accountNotFound(piggyBank.getRtAcno()));

        // 2) 미정산 이자 반영 — 정기결산일과 무관하게, 남아있던 이자를 먼저 원금에 가산
        InterestService.SettlementResult interestResult =
                interestService.settleUpTo(pigAccount, "PIG", today, today);

        // 3) 잔액 정산 — 이자까지 반영된 최종 잔액을 근거계좌로 전액 자동이체 (0원이면 이체 생략)
        BigDecimal settledAmount = pigAccount.getBalAmt();
        if (settledAmount.signum() > 0) {
            transferService.transfer(pigAccount, rtAccount, settledAmount,
                    AccountTransaction.TYPE_CLOSE_SETTLE, today);
        }
        piggyBank.syncBalance(pigAccount.getBalAmt());

        // 4) 서비스 OFF 이력 — 해지 시점에 USE_YN='Y'였던 서비스마다 USE_YN='N' 행 INSERT
        List<String> activeSvcCodes = piggyBankDetailRepository.findCurrentlyUsingServiceCodes(pigAcno);
        LocalDateTime now = LocalDateTime.now(clock);
        for (String svcCd : activeSvcCodes) {
            piggyBankDetailRepository.save(PiggyBankDetail.deactivate(pigAcno, svcCd, now));
        }

        // 5) 계좌·저금통 동시 해지 — 둘 중 하나만 해지되면 데이터 불일치가 생기므로 같은 트랜잭션
        piggyBank.close(today, "CUST_REQ");
        pigAccount.close(today, "CUST_REQ");

        // 6) 최종거래일 갱신 — 해지 처리 자체와 근거계좌 입금 모두 거래이므로 양쪽 갱신
        //    (잔액이 0원이라 이체가 생략된 경우에도 해지 자체는 거래이므로 갱신한다)
        pigAccount.touchLastTrxDate(today);
        rtAccount.touchLastTrxDate(today);

        // 추가) 재가입 가능성 — RT_ACNO는 해지하지 않고 그대로 둔다 (아무 것도 하지 않음)

        return new CloseResponse(pigAcno, rtAccount.getAcno(), interestResult.amount(),
                settledAmount, today, activeSvcCodes.size());
    }
}
