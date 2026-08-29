package com.kakaobank.piggybank.repository;

import com.kakaobank.piggybank.domain.AccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {
}
