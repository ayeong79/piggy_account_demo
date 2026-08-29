package com.kakaobank.piggybank.repository;

import com.kakaobank.piggybank.domain.DailyBalanceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyBalanceSnapshotRepository extends JpaRepository<DailyBalanceSnapshot, Long> {

    Optional<DailyBalanceSnapshot> findByAcnoAndSnapDate(String acno, LocalDate snapDate);

    List<DailyBalanceSnapshot> findBySnapDate(LocalDate snapDate);

    @Query("select case when count(s) > 0 then true else false end from DailyBalanceSnapshot s " +
           "where s.snapDate = :snapDate")
    boolean existsForDate(@Param("snapDate") LocalDate snapDate);
}
