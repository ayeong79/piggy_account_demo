package com.kakaobank.piggybank.repository;

import com.kakaobank.piggybank.domain.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * ACT_MAS 공통 리포지토리.
 * 이 리포지토리는 DdaAccount/PiggyBankAccount 구분 없이 ACNO로만 조회한다.
 */
public interface AccountRepository extends JpaRepository<Account, String> {

    /** 1.2/1.3/1.4의 "FOR UPDATE 잠금" 단계에 대응 — 비관적 쓰기 락으로 조회. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.acno = :acno")
    Optional<Account> findByIdForUpdate(@Param("acno") String acno);

    @Query("select a from Account a where a.cusNo = :cusNo")
    List<Account> findAllByCusNo(@Param("cusNo") String cusNo);
}
