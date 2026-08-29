package com.kakaobank.piggybank.repository;

import com.kakaobank.piggybank.domain.DdaAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DdaAccountRepository extends JpaRepository<DdaAccount, String> {

    List<DdaAccount> findByCnclYn(String cnclYn);
}
