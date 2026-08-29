package com.kakaobank.piggybank.service;

import com.kakaobank.piggybank.domain.Account;
import com.kakaobank.piggybank.domain.AccountTransaction;
import com.kakaobank.piggybank.domain.DailyBalanceSnapshot;
import com.kakaobank.piggybank.domain.PiggyBank;
import com.kakaobank.piggybank.domain.PiggyBankAutoSaveHistory;
import com.kakaobank.piggybank.domain.PiggyBankDetail;
import com.kakaobank.piggybank.exception.BusinessErrors;
import com.kakaobank.piggybank.repository.AccountRepository;
import com.kakaobank.piggybank.repository.DailyBalanceSnapshotRepository;
import com.kakaobank.piggybank.repository.PaymentRestrictionRepository;
import com.kakaobank.piggybank.repository.PiggyBankAutoSaveHistoryRepository;
import com.kakaobank.piggybank.repository.PiggyBankRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 1.3 자동저축(동전모으기) — 저금통 1건 처리 (플로우차트 코멘트.txt 1.3의 3~8단계).
 *
 * {@link #attemptProcess}와 {@link #recordFailure}는 각각 REQUIRES_NEW로 별도 트랜잭션에서 실행
 * (9단계: 건별 COMMIT — 특정 저금통에서 예외가 나도 다른 저금통 처리에 영향을 주지않도록 격리). 
 * 
 * 반드시 스프링 프록시를 거쳐 호출되어야 하므로 {@link AutoSaveBatchService}에서
 * 이 빈을 주입받아 호출한다(this.호출 금지).
 */

@Service
public class AutoSaveItemProcessor {

    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);
    private static final BigDecimal LIMIT = BigDecimal.valueOf(100_000);

    private final PiggyBankRepository piggyBankRepository;
    private final AccountRepository accountRepository;
    private final DailyBalanceSnapshotRepository snapshotRepository;
    private final PaymentRestrictionRepository paymentRestrictionRepository;
    private final PiggyBankAutoSaveHistoryRepository historyRepository;
    private final TransferService transferService;
    private final InterestService interestService;

    public AutoSaveItemProcessor(PiggyBankRepository piggyBankRepository,
                                  AccountRepository accountRepository,
                                  DailyBalanceSnapshotRepository snapshotRepository,
                                  PaymentRestrictionRepository paymentRestrictionRepository,
                                  PiggyBankAutoSaveHistoryRepository historyRepository,
                                  TransferService transferService,
                                  InterestService interestService) {
        this.piggyBankRepository = piggyBankRepository;
        this.accountRepository = accountRepository;
        this.snapshotRepository = snapshotRepository;
        this.paymentRestrictionRepository = paymentRestrictionRepository;
        this.historyRepository = historyRepository;
        this.transferService = transferService;
        this.interestService = interestService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PiggyBankAutoSaveHistory attemptProcess(String pigAcno, LocalDate runDate) {
        PiggyBank piggyBank = piggyBankRepository.findByIdForUpdate(pigAcno)
                .orElseThrow(() -> BusinessErrors.piggyBankNotFound(pigAcno));
        String rtAcno = piggyBank.getRtAcno();

        if (!piggyBank.isActive()) {
            return historyRepository.save(PiggyBankAutoSaveHistory.skip(pigAcno, PiggyBankDetail.SVC_COIN, rtAcno,
                    runDate, null, null, BigDecimal.ZERO, "ALREADY_CLOSED"));
        }

        // 1) 스냅샷 조회 — 전일자
        LocalDate snapDate = runDate.minusDays(1);
        DailyBalanceSnapshot snap = snapshotRepository.findByAcnoAndSnapDate(rtAcno, snapDate)
                .orElseThrow(() -> BusinessErrors.snapshotNotLoaded(snapDate.toString()));
        BigDecimal prevBal = snap.getBalAmt();

        // 3) 저축액 계산 — 3-1) CALC_AMT = MOD(전일잔액, 1000)
        BigDecimal calcAmt = prevBal.remainder(THOUSAND);
        if (calcAmt.signum() == 0) {
            return historyRepository.save(PiggyBankAutoSaveHistory.skip(pigAcno, PiggyBankDetail.SVC_COIN, rtAcno,
                    runDate, prevBal, null, calcAmt, PiggyBankAutoSaveHistory.SKIP_NO_CHANGE));
        }

        // 3-2) EXC_AMT = LEAST(CALC_AMT, 10만원 - PIG_MAS.BAL_AMT)
        BigDecimal room = LIMIT.subtract(piggyBank.getBalAmt());
        BigDecimal excAmt = calcAmt.min(room);
        if (excAmt.signum() <= 0) {
            return historyRepository.save(PiggyBankAutoSaveHistory.skip(pigAcno, PiggyBankDetail.SVC_COIN, rtAcno,
                    runDate, prevBal, null, calcAmt, PiggyBankAutoSaveHistory.SKIP_LIMIT_EXCD));
        }

        // 4) 근거계좌 지급제한 확인 — 근거계좌를 잠그기 전 마지막 무잠금 필터
        if (paymentRestrictionRepository.existsActiveByAcno(rtAcno)) {
            return historyRepository.save(PiggyBankAutoSaveHistory.skip(pigAcno, PiggyBankDetail.SVC_COIN, rtAcno,
                    runDate, prevBal, null, calcAmt, PiggyBankAutoSaveHistory.SKIP_RST_ACTIVE));
        }

        // 5) 잔액 1,000원 이하 SKIP — "저축시점" 실시간 잔액을 봐야 하므로 유일하게 FOR UPDATE로 조회
        Account rtAccount = accountRepository.findByIdForUpdate(rtAcno)
                .orElseThrow(() -> BusinessErrors.accountNotFound(rtAcno));
        if (rtAccount.getBalAmt().compareTo(THOUSAND) <= 0) {
            return historyRepository.save(PiggyBankAutoSaveHistory.skip(pigAcno, PiggyBankDetail.SVC_COIN, rtAcno,
                    runDate, prevBal, rtAccount.getBalAmt(), calcAmt, PiggyBankAutoSaveHistory.SKIP_BAL_LE_1000));
        }

        Account pigAccount = accountRepository.findByIdForUpdate(pigAcno)
                .orElseThrow(() -> BusinessErrors.piggyBankNotFound(pigAcno));

        // 6) 이자지급 재개 — 실제로 적립할 금액이 확정된 순간에만 체크
        interestService.settleAndResumeIfSuspended(rtAccount, "DDA", runDate, runDate);

        // 7) 이체 실행 — 거래금액은 CALC_AMT가 아니라 한도 반영이 끝난 EXC_AMT
        TransferService.TransferResult transferResult = transferService.transfer(
                rtAccount, pigAccount, excAmt, AccountTransaction.TYPE_COIN_SAVE, runDate);
        piggyBank.syncBalance(pigAccount.getBalAmt());

        // 8) 최종거래일 갱신은 TransferService 안에서 양쪽 계좌에 이미 반영됨

        return historyRepository.save(PiggyBankAutoSaveHistory.success(pigAcno, PiggyBankDetail.SVC_COIN, rtAcno,
                runDate, prevBal, rtAccount.getBalAmt(), calcAmt, excAmt, transferResult.withdrawTrxSeqId()));
    }

    /** attemptProcess가 예외로 끝났을 때, 별도 트랜잭션에서 FAIL 이력만 확실히 남긴다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PiggyBankAutoSaveHistory recordFailure(String pigAcno, String rtAcno, LocalDate runDate, String reasonCode) {
        return historyRepository.save(PiggyBankAutoSaveHistory.fail(
                pigAcno, PiggyBankDetail.SVC_COIN, rtAcno == null ? "UNKNOWN" : rtAcno, runDate, reasonCode));
    }
}
