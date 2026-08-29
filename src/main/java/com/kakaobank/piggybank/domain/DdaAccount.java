package com.kakaobank.piggybank.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.time.LocalDate;

/**
 * 입출금이 자유로운 예금 (근거계좌). ACCD = 'DDA'.
 * 저금통 특약 제4조①: 저금통은 실명확인된 DDA 계좌에만 연결할 수 있다.
 */
@Entity
@DiscriminatorValue("DDA")
public class DdaAccount extends Account {

    protected DdaAccount() {
        super();
    }

    public DdaAccount(String acno, String cusNo, LocalDate opnDt) {
        super(acno, cusNo, opnDt);
    }

    @Override
    public String accountTypeCode() {
        return "DDA";
    }
}
