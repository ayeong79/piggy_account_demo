package com.kakaobank.piggybank.service;

import com.kakaobank.piggybank.domain.Account;
import com.kakaobank.piggybank.domain.InterestPayment;
import com.kakaobank.piggybank.domain.ProductRateHistory;
import com.kakaobank.piggybank.repository.InterestPaymentRepository;
import com.kakaobank.piggybank.repository.ProductRateHistoryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * 입출금이자유로운예금약관 제2조③: "...계좌해지 또는 추가 입출금 거래 발생일에 일괄계산하여 지급".
 *
 * 1.2(비우기 2단계) / 1.3(자동저축 6단계) / 1.4(해지 2단계)가 공통으로 재사용하는
 * "미정산 이자 계산 및 정산" 로직. 
 */
@Service
public class InterestService {

    private final InterestPaymentRepository interestPaymentRepository;
    private final ProductRateHistoryRepository productRateHistoryRepository;
    private final TransferService transferService;

    public InterestService(InterestPaymentRepository interestPaymentRepository,
                            ProductRateHistoryRepository productRateHistoryRepository,
                            TransferService transferService) {
        this.interestPaymentRepository = interestPaymentRepository;
        this.productRateHistoryRepository = productRateHistoryRepository;
        this.transferService = transferService;
    }

    public record SettlementResult(boolean settled, BigDecimal amount) {
        static final SettlementResult NONE = new SettlementResult(false, BigDecimal.ZERO);
    }

    /**
     * account의 마지막 이자지급일(IST_HIS.PAY_DT) 다음날부터 asOfDate 전일까지 이자를 계산해
     * 원금에 가산한다. 계산기간이 없거나(fromDt > toDt) 이자가 0원이면 아무 것도 하지 않는다.
     *
     * @param accd        금리 조회에 사용할 상품구분 ('PIG' 또는 'DDA')
     * @param payDate     실제로 지급 처리되는 날짜(IST_HIS.PAY_DT로 기록됨)
     */
    public SettlementResult settleUpTo(Account account, String accd, LocalDate asOfDate, LocalDate payDate) {
        LocalDate toDt = asOfDate.minusDays(1);
        Optional<InterestPayment> last = interestPaymentRepository.findLastByAcno(account.getAcno());
        LocalDate fromDt = last.map(ip -> ip.getToDt().plusDays(1)).orElse(account.getOpnDt());

        if (fromDt.isAfter(toDt)) {
            return SettlementResult.NONE; // 계산기간 없음
        }

        Optional<ProductRateHistory> rate = productRateHistoryRepository.findApplicableRate(accd, toDt);
        if (rate.isEmpty()) {
            return SettlementResult.NONE; // 금리표 미등록 — 데모에서는 skip
        }

        long days = ChronoUnit.DAYS.between(fromDt, toDt) + 1;
        BigDecimal ratio = rate.get().getRatePct()
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(days))
                .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);
        BigDecimal interest = account.getBalAmt().multiply(ratio).setScale(0, RoundingMode.DOWN); // 원 단위 절사

        if (interest.signum() <= 0) {
            return SettlementResult.NONE;
        }

        InterestPayment ist = new InterestPayment(account.getAcno(), fromDt, toDt, payDate, interest);
        interestPaymentRepository.save(ist);

        Long trxSeqId = transferService.creditSelf(account, interest, "INTEREST", payDate);
        ist.linkTransaction(trxSeqId);

        return new SettlementResult(true, interest);
    }

    /**
     * ISTOP_YN='Y'(이자지급불가) 상태였다면 정산 후 'N'으로 되돌린다.
     * (1.2 2단계 / 1.3 6단계: "이번 적립/거래가 곧 추가 입출금 거래에 해당하므로 이자지급 재개")
     */
    public SettlementResult settleAndResumeIfSuspended(Account account, String accd, LocalDate asOfDate, LocalDate payDate) {
        if (!account.isInterestSuspended()) {
            return SettlementResult.NONE;
        }
        SettlementResult result = settleUpTo(account, accd, asOfDate, payDate);
        account.resumeInterest();
        return result;
    }
}
