package com.kakaobank.piggybank.repository;

import com.kakaobank.piggybank.domain.InterestPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InterestPaymentRepository extends JpaRepository<InterestPayment, Long> {

    @Query("select i from InterestPayment i where i.acno = :acno order by i.payDt desc, i.seqId desc")
    Optional<InterestPayment> findLastByAcno(@Param("acno") String acno);
}
