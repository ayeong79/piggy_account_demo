package com.kakaobank.piggybank.repository;

import com.kakaobank.piggybank.domain.PiggyBank;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PiggyBankRepository extends JpaRepository<PiggyBank, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PiggyBank p where p.pigAcno = :pigAcno")
    Optional<PiggyBank> findByIdForUpdate(@Param("pigAcno") String pigAcno);

    /** 저금통 특약 제3조: 1인당 1계좌 — 활성(CNCL_YN='N') 저금통 존재 여부. */
    @Query("select case when count(p) > 0 then true else false end " +
           "from PiggyBank p where p.cusNo = :cusNo and p.cnclYn = 'N'")
    boolean existsActiveByCusNo(@Param("cusNo") String cusNo);

    /** UX_PIG_MAS_RTACNO_ACTV에 대응하는 애플리케이션 레벨 검증 (schema.sql 주석 참고). */
    @Query("select case when count(p) > 0 then true else false end " +
           "from PiggyBank p where p.rtAcno = :rtAcno and p.cnclYn = 'N'")
    boolean existsActiveByRtAcno(@Param("rtAcno") String rtAcno);
}
