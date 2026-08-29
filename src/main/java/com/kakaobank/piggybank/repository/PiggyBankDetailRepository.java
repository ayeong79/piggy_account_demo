package com.kakaobank.piggybank.repository;

import com.kakaobank.piggybank.domain.PiggyBankDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * PIG_DTL은 (PIG_ACNO, SVC_CD)별 이력 테이블이라 "현재 설정"은 항상
 * "최신 SEQ_ID 행"으로 조회.
 */
public interface PiggyBankDetailRepository extends JpaRepository<PiggyBankDetail, Long> {

    @Query("select d from PiggyBankDetail d where d.pigAcno = :pigAcno and d.svcCd = :svcCd " +
           "and d.seqId = (select max(d2.seqId) from PiggyBankDetail d2 " +
           "               where d2.pigAcno = :pigAcno and d2.svcCd = :svcCd)")
    Optional<PiggyBankDetail> findLatest(@Param("pigAcno") String pigAcno, @Param("svcCd") String svcCd);

    /** 해지(1.4) 4단계 — 해지 시점에 사용 중(USE_YN='Y')이던 서비스 코드 전체. */
    @Query("select d.svcCd from PiggyBankDetail d where d.pigAcno = :pigAcno and d.useYn = 'Y' " +
           "and d.seqId = (select max(d2.seqId) from PiggyBankDetail d2 " +
           "               where d2.pigAcno = d.pigAcno and d2.svcCd = d.svcCd)")
    List<String> findCurrentlyUsingServiceCodes(@Param("pigAcno") String pigAcno);

    /**
     * 1.3 자동저축 배치 대상 판별 — (PIG_ACNO, SVC_CD='COIN')별 최신 행이 USE_YN='Y'이고
     * "오늘 막 신청"이 아닌(APLY_DTTM이 오늘 자정 이전인) 저금통의 PIG_ACNO 목록.
     */
    @Query("select d.pigAcno from PiggyBankDetail d where d.svcCd = :svcCd and d.useYn = 'Y' " +
           "and d.aplyDttm < :cutoffExclusive " +
           "and d.seqId = (select max(d2.seqId) from PiggyBankDetail d2 " +
           "               where d2.pigAcno = d.pigAcno and d2.svcCd = :svcCd)")
    List<String> findEligiblePigAcnosForBatch(@Param("svcCd") String svcCd,
                                               @Param("cutoffExclusive") LocalDateTime cutoffExclusive);
}
