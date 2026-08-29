package com.kakaobank.piggybank.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.time.LocalDate;

/**
 * 저금통 계좌 (ACT_MAS 상의 행). ACCD = 'PIG'.
 * {@link PiggyBank}(PIG_MAS)와 PK(ACNO=PIG_ACNO)를 공유하는 합성 관계 — 저금통의
 * "계좌"로서의 측면(잔액/거래/해지 상태)을 담당하고, PIG_MAS는 "상품"으로서의
 * 측면(근거계좌 연결, 서비스 설정)을 담당한다.
 */
@Entity
@DiscriminatorValue("PIG")
public class PiggyBankAccount extends Account {

    protected PiggyBankAccount() {
        super();
    }

    public PiggyBankAccount(String acno, String cusNo, LocalDate opnDt) {
        super(acno, cusNo, opnDt);
    }

    @Override
    public String accountTypeCode() {
        return "PIG";
    }
}
