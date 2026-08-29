package com.kakaobank.piggybank.repository;

import com.kakaobank.piggybank.domain.ProductRateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ProductRateHistoryRepository extends JpaRepository<ProductRateHistory, ProductRateHistory.Id> {

    /** asOfDate 시점에 적용 중인 금리 = APLY_DT <= asOfDate 중 가장 최근 행. */
    @Query("select r from ProductRateHistory r where r.id.accd = :accd and r.id.aplyDt <= :asOfDate " +
           "order by r.id.aplyDt desc")
    java.util.List<ProductRateHistory> findApplicable(@Param("accd") String accd, @Param("asOfDate") LocalDate asOfDate);

    default Optional<ProductRateHistory> findApplicableRate(String accd, LocalDate asOfDate) {
        java.util.List<ProductRateHistory> list = findApplicable(accd, asOfDate);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
