package com.kakaobank.piggybank.repository;

import com.kakaobank.piggybank.domain.PiggyBankAutoSaveHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PiggyBankAutoSaveHistoryRepository extends JpaRepository<PiggyBankAutoSaveHistory, Long> {

    List<PiggyBankAutoSaveHistory> findByPigAcnoOrderBySeqIdDesc(String pigAcno);
}
