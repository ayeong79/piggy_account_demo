package com.kakaobank.piggybank.service;

import com.kakaobank.piggybank.domain.PiggyBank;
import com.kakaobank.piggybank.domain.PiggyBankAutoSaveHistory;
import com.kakaobank.piggybank.domain.PiggyBankDetail;
import com.kakaobank.piggybank.dto.response.AutoSaveBatchResult;
import com.kakaobank.piggybank.dto.response.AutoSaveItemResult;
import com.kakaobank.piggybank.exception.PiggyBankBusinessException;
import com.kakaobank.piggybank.repository.PiggyBankDetailRepository;
import com.kakaobank.piggybank.repository.PiggyBankRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 1.3 자동저축(동전모으기) 배치 오케스트레이터 — 대상 판별(2단계) 후 저금통마다
 * {@link AutoSaveItemProcessor}를 호출한다. 저금통 한 건의 실패가 다른 건 처리를 막지 않도록 
 * 여기서 예외를 잡아 FAIL 이력으로 남긴다 (9단계: 건별 COMMIT).
 */
@Service
public class AutoSaveBatchService {

    private final PiggyBankDetailRepository piggyBankDetailRepository;
    private final PiggyBankRepository piggyBankRepository;
    private final AutoSaveItemProcessor itemProcessor;

    public AutoSaveBatchService(PiggyBankDetailRepository piggyBankDetailRepository,
                                 PiggyBankRepository piggyBankRepository,
                                 AutoSaveItemProcessor itemProcessor) {
        this.piggyBankDetailRepository = piggyBankDetailRepository;
        this.piggyBankRepository = piggyBankRepository;
        this.itemProcessor = itemProcessor;
    }

    public AutoSaveBatchResult runBatch(LocalDate runDate) {
        // 2) 대상 판별 — (PIG_ACNO,SVC_CD='COIN') 최신 설정이 USE_YN='Y'이고
        //    오늘 자정 이전에 신청된 저금통만 포함 (오늘 막 신청한 고객은 다음 배치부터 포함)
        LocalDateTime cutoffExclusive = runDate.atStartOfDay();
        List<String> eligiblePigAcnos = piggyBankDetailRepository.findEligiblePigAcnosForBatch(
                PiggyBankDetail.SVC_COIN, cutoffExclusive);

        List<AutoSaveItemResult> items = new ArrayList<>();
        int success = 0, skip = 0, fail = 0;

        for (String pigAcno : eligiblePigAcnos) {
            PiggyBankAutoSaveHistory history;
            try {
                history = itemProcessor.attemptProcess(pigAcno, runDate);
            } catch (Exception e) {
                String rtAcno = piggyBankRepository.findById(pigAcno).map(PiggyBank::getRtAcno).orElse(null);
                String reasonCode = (e instanceof PiggyBankBusinessException be) ? be.getReasonCode() : "UNEXPECTED_ERROR";
                history = itemProcessor.recordFailure(pigAcno, rtAcno, runDate, reasonCode);
            }

            items.add(new AutoSaveItemResult(pigAcno, history.getExcStCd(),
                    history.getSkipRsnCd() != null ? history.getSkipRsnCd() : history.getFailRsnCd(),
                    history.getCalcAmt(), history.getExcAmt()));

            switch (history.getExcStCd()) {
                case PiggyBankAutoSaveHistory.STATUS_SUCCESS -> success++;
                case PiggyBankAutoSaveHistory.STATUS_SKIP -> skip++;
                default -> fail++;
            }
        }

        return new AutoSaveBatchResult(runDate, eligiblePigAcnos.size(), success, skip, fail, items);
    }
}
