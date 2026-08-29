package com.kakaobank.piggybank.service;

import com.kakaobank.piggybank.domain.Account;
import com.kakaobank.piggybank.domain.AccountTransaction;
import com.kakaobank.piggybank.domain.PiggyBank;
import com.kakaobank.piggybank.dto.response.EmptyResponse;
import com.kakaobank.piggybank.exception.BusinessErrors;
import com.kakaobank.piggybank.repository.AccountRepository;
import com.kakaobank.piggybank.repository.PaymentRestrictionRepository;
import com.kakaobank.piggybank.repository.PiggyBankRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

/**
 * 1.2 저금통비우기 (플로우차트 코멘트.txt 1.2 저금통비우기, 1~6단계).
 */
@Service
public class EmptyService {

    private final PiggyBankRepository piggyBankRepository;
    private final AccountRepository accountRepository;
    private final PaymentRestrictionRepository paymentRestrictionRepository;
    private final TransferService transferService;
    private final InterestService interestService;
    private final Clock clock;

    public EmptyService(PiggyBankRepository piggyBankRepository,
                         AccountRepository accountRepository,
                         PaymentRestrictionRepository paymentRestrictionRepository,
                         TransferService transferService,
                         InterestService interestService,
                         Clock clock) {
        this.piggyBankRepository = piggyBankRepository;
        this.accountRepository = accountRepository;
        this.paymentRestrictionRepository = paymentRestrictionRepository;
        this.transferService = transferService;
        this.interestService = interestService;
        this.clock = clock;
    }

    @Transactional
    public EmptyResponse empty(String pigAcno) {
        LocalDate today = LocalDate.now(clock);

        // 1) 지급제한 확인 — 가장 먼저 체크해서 불필요한 잠금을 피함
        if (paymentRestrictionRepository.existsActiveByAcno(pigAcno)) {
            throw BusinessErrors.paymentRestricted();
        }

        // 3) FOR UPDATE 잠금 — PIG_MAS·ACT_MAS(양쪽) 행을 잠그고 조회
        PiggyBank piggyBank = piggyBankRepository.findByIdForUpdate(pigAcno)
                .orElseThrow(() -> BusinessErrors.piggyBankNotFound(pigAcno));
        if (!piggyBank.isActive()) {
            throw BusinessErrors.alreadyClosed();
        }
        Account pigAccount = accountRepository.findByIdForUpdate(pigAcno)
                .orElseThrow(() -> BusinessErrors.piggyBankNotFound(pigAcno));
        Account rtAccount = accountRepository.findByIdForUpdate(piggyBank.getRtAcno())
                .orElseThrow(() -> BusinessErrors.accountNotFound(piggyBank.getRtAcno()));

        // 2) 이자지급 재개 — ISTOP_YN='Y'면 비우기 전에 미정산 이자를 일괄정산하고 'N'으로 복귀
        //    (이 순서를 지키지 않으면 미정산 이자가 반영되지 않은 채 잔액만 이체되어 고객이 손해를 봄)
        InterestService.SettlementResult interestResult =
                interestService.settleAndResumeIfSuspended(pigAccount, "PIG", today, today);

        // 4) 이중기장 + 5) 잔액 동기화 — 전액출금만 가능 (저금통 특약 제6조②)
        BigDecimal transferAmount = pigAccount.getBalAmt();
        if (transferAmount.signum() > 0) {
            transferService.transfer(pigAccount, rtAccount, transferAmount,
                    AccountTransaction.TYPE_EMPTY, today);
        } else {
            // 잔액이 0원이어도 "비우기 실행" 자체는 유효한 처리이므로 이체만 생략하고 계속 진행한다.
            pigAccount.touchLastTrxDate(today);
            rtAccount.touchLastTrxDate(today);
        }
        piggyBank.syncBalance(pigAccount.getBalAmt());

        // 6) 최종거래일 갱신은 withdraw/deposit(TransferService) 및 touchLastTrxDate 안에서 이미 처리됨

        return new EmptyResponse(pigAcno, rtAccount.getAcno(), transferAmount,
                interestResult.amount(), pigAccount.getBalAmt(), rtAccount.getBalAmt());
    }
}
