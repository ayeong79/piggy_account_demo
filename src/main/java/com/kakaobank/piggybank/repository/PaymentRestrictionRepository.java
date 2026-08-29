package com.kakaobank.piggybank.repository;

import com.kakaobank.piggybank.domain.PaymentRestriction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRestrictionRepository extends JpaRepository<PaymentRestriction, Long> {

    @Query("select case when count(r) > 0 then true else false end from PaymentRestriction r " +
           "where r.acno = :acno and r.rstStCd = 'ACTV'")
    boolean existsActiveByAcno(@Param("acno") String acno);
}
