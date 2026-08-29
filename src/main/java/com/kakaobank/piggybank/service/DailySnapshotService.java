package com.kakaobank.piggybank.service;

import com.kakaobank.piggybank.domain.DailyBalanceSnapshot;
import com.kakaobank.piggybank.domain.DdaAccount;
import com.kakaobank.piggybank.dto.response.SnapshotLoadResult;
import com.kakaobank.piggybank.repository.DailyBalanceSnapshotRepository;
import com.kakaobank.piggybank.repository.DdaAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 1.3 자동저축 1단계 (스냅샷 적재) — 배치 전용 "전일 마감 잔액" 고정 테이블(DLY_BLC_SNAPSHOT)에
 * 현재 모든 활성 DDA(입출금) 계좌의 잔액을 복사해 둔다.
 *
 * 실제 운영에서는 매일 자정 배치로 "오늘 날짜"의 마감 스냅샷을 적재하고, 다음날 자동저축
 * 배치가 그 스냅샷(=전일자)을 읽는다. 이 데모에서는 날짜를 파라미터로 받아 그대로 재현한다.
 */
@Service
public class DailySnapshotService {

    private static final String PROG_ID = "PIG_SNAPSHOT_BATCH";

    private final DdaAccountRepository ddaAccountRepository;
    private final DailyBalanceSnapshotRepository snapshotRepository;

    public DailySnapshotService(DdaAccountRepository ddaAccountRepository,
                                 DailyBalanceSnapshotRepository snapshotRepository) {
        this.ddaAccountRepository = ddaAccountRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional
    public SnapshotLoadResult loadSnapshot(LocalDate snapDate) {
        List<DdaAccount> activeDdaAccounts = ddaAccountRepository.findByCnclYn("N");
        int loaded = 0;
        for (DdaAccount dda : activeDdaAccounts) {
            if (snapshotRepository.findByAcnoAndSnapDate(dda.getAcno(), snapDate).isPresent()) {
                continue; // 이미 적재된 날짜는 건너뜀 (UK_DLY_BLC_SNAPSHOT 보호)
            }
            snapshotRepository.save(new DailyBalanceSnapshot(snapDate, dda.getAcno(), dda.getCusNo(),
                    dda.getBalAmt(), PROG_ID));
            loaded++;
        }
        return new SnapshotLoadResult(snapDate, loaded);
    }
}
