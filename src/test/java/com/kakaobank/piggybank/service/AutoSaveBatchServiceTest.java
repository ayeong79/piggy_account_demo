package com.kakaobank.piggybank.service;

import com.kakaobank.piggybank.domain.Account;
import com.kakaobank.piggybank.domain.DailyBalanceSnapshot;
import com.kakaobank.piggybank.domain.PiggyBank;
import com.kakaobank.piggybank.domain.PiggyBankAutoSaveHistory;
import com.kakaobank.piggybank.domain.PiggyBankDetail;
import com.kakaobank.piggybank.dto.response.AutoSaveBatchResult;
import com.kakaobank.piggybank.repository.AccountRepository;
import com.kakaobank.piggybank.repository.DailyBalanceSnapshotRepository;
import com.kakaobank.piggybank.repository.PiggyBankAutoSaveHistoryRepository;
import com.kakaobank.piggybank.repository.PiggyBankDetailRepository;
import com.kakaobank.piggybank.repository.PiggyBankRepository;
import com.kakaobank.piggybank.support.TestClockConfig;
import com.kakaobank.piggybank.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 1.3 자동저축(동전모으기) — 플로우차트 코멘트.txt 1~9단계를 검증한다.
 *
 * [중요] 이 테스트 클래스는 일부러 클래스 레벨 @Transactional을 쓰지 않는다.
 * AutoSaveItemProcessor의 attemptProcess/recordFailure는 REQUIRES_NEW로 별도
 * 물리 트랜잭션에서 커밋되므로(9단계: 건별 COMMIT), 테스트를 감싸는 트랜잭션을
 * 롤백해도 그 안에서 커밋된 내용은 되돌아가지 않는다 — 오히려 "부모"만 롤백되면
 * 참조 무결성이 깨질 수 있다. 대신 테스트마다 고유한 ID 접두사(A1, A2, ...)를 써서
 * 데이터가 서로 섞이지 않게 한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestClockConfig.class)
class AutoSaveBatchServiceTest {

    private static final LocalDate RUN_DATE = TestClockConfig.FIXED_TODAY;      // 2026-08-28
    private static final LocalDate SNAP_DATE = RUN_DATE.minusDays(1);           // 2026-08-27

    @Autowired
    private AutoSaveBatchService autoSaveBatchService;
    @Autowired
    private DailySnapshotService dailySnapshotService;
    @Autowired
    private TestFixtures fixtures;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PiggyBankRepository piggyBankRepository;
    @Autowired
    private PiggyBankDetailRepository piggyBankDetailRepository;
    @Autowired
    private DailyBalanceSnapshotRepository snapshotRepository;
    @Autowired
    private PiggyBankAutoSaveHistoryRepository historyRepository;

    private void snapshot(String rtAcno, String cusNo, BigDecimal balance) {
        snapshotRepository.save(new DailyBalanceSnapshot(SNAP_DATE, rtAcno, cusNo, balance, "TEST"));
    }

    @Test
    void 끝전이_있으면_한도내에서_저금통으로_적립된다() {
        fixtures.customer("A1-CUS", LocalDate.of(1990, 1, 1));
        fixtures.ddaAccount("A1-ACT", "A1-CUS", BigDecimal.valueOf(1_325_150)); // 오늘 실시간 잔액
        fixtures.existingPiggyBank("A1-PIG", "A1-ACT", "A1-CUS", BigDecimal.valueOf(44_850));
        snapshot("A1-ACT", "A1-CUS", BigDecimal.valueOf(1_325_150)); // 전일 잔액 스냅샷 (끝전 150원)

        AutoSaveBatchResult result = autoSaveBatchService.runBatch(RUN_DATE);

        assertThat(result.successCount()).isGreaterThanOrEqualTo(1);
        PiggyBankAutoSaveHistory history = latest("A1-PIG");
        assertThat(history.getExcStCd()).isEqualTo(PiggyBankAutoSaveHistory.STATUS_SUCCESS);
        assertThat(history.getCalcAmt()).isEqualByComparingTo(BigDecimal.valueOf(150));
        assertThat(history.getExcAmt()).isEqualByComparingTo(BigDecimal.valueOf(150));

        PiggyBank piggyBank = piggyBankRepository.findById("A1-PIG").orElseThrow();
        assertThat(piggyBank.getBalAmt()).isEqualByComparingTo(BigDecimal.valueOf(45_000));
        Account rt = accountRepository.findById("A1-ACT").orElseThrow();
        assertThat(rt.getBalAmt()).isEqualByComparingTo(BigDecimal.valueOf(1_325_000));
    }

    @Test
    void 끝전이_0원이면_NO_CHANGE로_SKIP된다() {
        fixtures.customer("A2-CUS", LocalDate.of(1990, 1, 1));
        fixtures.ddaAccount("A2-ACT", "A2-CUS", BigDecimal.valueOf(500_000));
        fixtures.existingPiggyBank("A2-PIG", "A2-ACT", "A2-CUS", BigDecimal.valueOf(10_000));
        snapshot("A2-ACT", "A2-CUS", BigDecimal.valueOf(500_000)); // 1000으로 나누어떨어짐

        autoSaveBatchService.runBatch(RUN_DATE);

        PiggyBankAutoSaveHistory history = latest("A2-PIG");
        assertThat(history.getExcStCd()).isEqualTo(PiggyBankAutoSaveHistory.STATUS_SKIP);
        assertThat(history.getSkipRsnCd()).isEqualTo(PiggyBankAutoSaveHistory.SKIP_NO_CHANGE);
    }

    @Test
    void 저금통이_10만원_한도에_도달했으면_LIMIT_EXCD로_SKIP된다() {
        fixtures.customer("A3-CUS", LocalDate.of(1990, 1, 1));
        fixtures.ddaAccount("A3-ACT", "A3-CUS", BigDecimal.valueOf(500_000));
        fixtures.existingPiggyBank("A3-PIG", "A3-ACT", "A3-CUS", BigDecimal.valueOf(100_000)); // 이미 한도
        snapshot("A3-ACT", "A3-CUS", BigDecimal.valueOf(123_456)); // 끝전 456원 존재

        autoSaveBatchService.runBatch(RUN_DATE);

        PiggyBankAutoSaveHistory history = latest("A3-PIG");
        assertThat(history.getExcStCd()).isEqualTo(PiggyBankAutoSaveHistory.STATUS_SKIP);
        assertThat(history.getSkipRsnCd()).isEqualTo(PiggyBankAutoSaveHistory.SKIP_LIMIT_EXCD);
        assertThat(history.getCalcAmt()).isEqualByComparingTo(BigDecimal.valueOf(456));
    }

    @Test
    void 근거계좌에_지급제한이_있으면_RST_ACTIVE로_SKIP된다() {
        fixtures.customer("A4-CUS", LocalDate.of(1990, 1, 1));
        fixtures.ddaAccount("A4-ACT", "A4-CUS", BigDecimal.valueOf(500_000));
        fixtures.existingPiggyBank("A4-PIG", "A4-ACT", "A4-CUS", BigDecimal.valueOf(10_000));
        fixtures.restriction("A4-ACT");
        snapshot("A4-ACT", "A4-CUS", BigDecimal.valueOf(500_500));

        autoSaveBatchService.runBatch(RUN_DATE);

        PiggyBankAutoSaveHistory history = latest("A4-PIG");
        assertThat(history.getExcStCd()).isEqualTo(PiggyBankAutoSaveHistory.STATUS_SKIP);
        assertThat(history.getSkipRsnCd()).isEqualTo(PiggyBankAutoSaveHistory.SKIP_RST_ACTIVE);
    }

    @Test
    void 근거계좌_실시간잔액이_1000원_이하이면_BAL_LE_1000으로_SKIP된다() {
        fixtures.customer("A5-CUS", LocalDate.of(1990, 1, 1));
        // 어제는 잔액이 넉넉했지만(끝전 500원), 오늘 실시간 잔액은 1000원 이하로 떨어진 상황을 재현
        fixtures.ddaAccount("A5-ACT", "A5-CUS", BigDecimal.valueOf(500));
        fixtures.existingPiggyBank("A5-PIG", "A5-ACT", "A5-CUS", BigDecimal.valueOf(10_000));
        snapshot("A5-ACT", "A5-CUS", BigDecimal.valueOf(50_500));

        autoSaveBatchService.runBatch(RUN_DATE);

        PiggyBankAutoSaveHistory history = latest("A5-PIG");
        assertThat(history.getExcStCd()).isEqualTo(PiggyBankAutoSaveHistory.STATUS_SKIP);
        assertThat(history.getSkipRsnCd()).isEqualTo(PiggyBankAutoSaveHistory.SKIP_BAL_LE_1000);
    }

    @Test
    void 오늘_막_신청한_저금통은_이번_배치_대상에서_제외된다() {
        fixtures.customer("A6-CUS", LocalDate.of(1990, 1, 1));
        fixtures.ddaAccount("A6-ACT", "A6-CUS", BigDecimal.valueOf(500_000));
        fixtures.existingPiggyBank("A6-PIG", "A6-ACT", "A6-CUS", BigDecimal.valueOf(10_000));
        snapshot("A6-ACT", "A6-CUS", BigDecimal.valueOf(500_500));

        // "오늘" 다시 신청한 것으로 최신 PIG_DTL 행을 갱신 (오늘 자정 이후 시각)
        piggyBankDetailRepository.save(PiggyBankDetail.activate(
                "A6-PIG", PiggyBankDetail.SVC_COIN, RUN_DATE.atTime(9, 0)));

        List<String> eligible = piggyBankDetailRepository.findEligiblePigAcnosForBatch(
                PiggyBankDetail.SVC_COIN, RUN_DATE.atStartOfDay());
        assertThat(eligible).doesNotContain("A6-PIG");

        autoSaveBatchService.runBatch(RUN_DATE);
        assertThat(historyRepository.findByPigAcnoOrderBySeqIdDesc("A6-PIG")).isEmpty();
    }

    @Test
    void 스냅샷_로딩서비스는_모든_활성_DDA계좌의_잔액을_복사한다() {
        fixtures.customer("A7-CUS", LocalDate.of(1990, 1, 1));
        fixtures.ddaAccount("A7-ACT", "A7-CUS", BigDecimal.valueOf(777_777));

        var result = dailySnapshotService.loadSnapshot(SNAP_DATE);

        assertThat(result.loadedCount()).isGreaterThanOrEqualTo(1);
        assertThat(snapshotRepository.findByAcnoAndSnapDate("A7-ACT", SNAP_DATE))
                .hasValueSatisfying(s -> assertThat(s.getBalAmt()).isEqualByComparingTo(BigDecimal.valueOf(777_777)));
    }

    @Test
    void 스냅샷이_없는_저금통이_실패해도_다른_저금통_처리는_계속된다() {
        // A8: 스냅샷이 없어 FAIL로 남아야 하는 케이스
        fixtures.customer("A8-CUS", LocalDate.of(1990, 1, 1));
        fixtures.ddaAccount("A8-ACT", "A8-CUS", BigDecimal.valueOf(500_000));
        fixtures.existingPiggyBank("A8-PIG", "A8-ACT", "A8-CUS", BigDecimal.valueOf(10_000));
        // snapshot(...) 호출을 일부러 생략

        // A9: 정상적으로 성공해야 하는 케이스
        fixtures.customer("A9-CUS", LocalDate.of(1990, 1, 1));
        fixtures.ddaAccount("A9-ACT", "A9-CUS", BigDecimal.valueOf(200_150));
        fixtures.existingPiggyBank("A9-PIG", "A9-ACT", "A9-CUS", BigDecimal.valueOf(1_000));
        snapshot("A9-ACT", "A9-CUS", BigDecimal.valueOf(200_150));

        AutoSaveBatchResult result = autoSaveBatchService.runBatch(RUN_DATE);

        assertThat(latest("A8-PIG").getExcStCd()).isEqualTo(PiggyBankAutoSaveHistory.STATUS_FAIL);
        assertThat(latest("A8-PIG").getFailRsnCd()).isEqualTo("SNAPSHOT_NOT_LOADED");

        assertThat(latest("A9-PIG").getExcStCd()).isEqualTo(PiggyBankAutoSaveHistory.STATUS_SUCCESS);
        assertThat(result.failCount()).isGreaterThanOrEqualTo(1);
        assertThat(result.successCount()).isGreaterThanOrEqualTo(1);
    }

    private PiggyBankAutoSaveHistory latest(String pigAcno) {
        List<PiggyBankAutoSaveHistory> list = historyRepository.findByPigAcnoOrderBySeqIdDesc(pigAcno);
        assertThat(list).as("이력이 존재해야 함: " + pigAcno).isNotEmpty();
        return list.get(0);
    }
}
