package com.kakaobank.piggybank.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Period;

/**
 * CUS_MAS (고객).
 * 1.1 신규가입 1단계(연령 검증)의 기준 테이블.
 */
@Entity
@Table(name = "CUS_MAS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer {

    @Id
    @Column(name = "CUS_NO", length = 20)
    private String cusNo;

    @Column(name = "BIRTH_DATE", nullable = false)
    private LocalDate birthDate;

    @Setter
    @Column(name = "VALD_YN", nullable = false, length = 1)
    private String valdYn = "Y";

    public Customer(String cusNo, LocalDate birthDate) {
        this.cusNo = cusNo;
        this.birthDate = birthDate;
        this.valdYn = "Y";
    }

    /**
     * 저금통 특약 제3조: "가입대상은 만 14세 이상의 실명의 개인".
     * asOfDate 기준 만 나이가 minAge 이상인지 판단한다.
     */
    public boolean isEligibleAge(LocalDate asOfDate, int minAge) {
        return Period.between(birthDate, asOfDate).getYears() >= minAge;
    }

    public boolean isValid() {
        return "Y".equals(valdYn);
    }
}
