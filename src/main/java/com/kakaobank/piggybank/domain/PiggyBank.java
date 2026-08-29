package com.kakaobank.piggybank.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PIG_MAS (저금통).
 * PK(PIG_ACNO)를 {@link PiggyBankAccount}의 ACNO와 공유하는 1:1 합성 관계.
 */
@Entity
@Table(name = "PIG_MAS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PiggyBank {

    @Id
    @Column(name = "PIG_ACNO", length = 13)
    private String pigAcno;

    @Column(name = "RT_ACNO", length = 13, nullable = false)
    private String rtAcno;

    @Column(name = "CUS_NO", length = 20, nullable = false)
    private String cusNo;

    @Column(name = "BAL_AMT", precision = 15, nullable = false)
    private BigDecimal balAmt = BigDecimal.ZERO;

    @Column(name = "CNCL_YN", length = 1, nullable = false)
    private String cnclYn = "N";

    @Column(name = "ENR_DT", nullable = false)
    private LocalDate enrDt;

    @Column(name = "CNCL_DT")
    private LocalDate cnclDt;

    @Column(name = "CNCL_CD", length = 20)
    private String cnclCd;

    public PiggyBank(String pigAcno, String rtAcno, String cusNo, LocalDate enrDt) {
        this.pigAcno = pigAcno;
        this.rtAcno = rtAcno;
        this.cusNo = cusNo;
        this.balAmt = BigDecimal.ZERO;
        this.cnclYn = "N";
        this.enrDt = enrDt;
    }

    public boolean isActive() {
        return "N".equals(cnclYn);
    }

    /** PIG_MAS.BAL_AMT는 ACT_MAS(저금통 자신).BAL_AMT의 캐시이므로 항상 함께 동기화한다. */
    public void syncBalance(BigDecimal newBalance) {
        this.balAmt = newBalance;
    }

    public void close(LocalDate closeDate, String reasonCode) {
        this.cnclYn = "Y";
        this.cnclDt = closeDate;
        this.cnclCd = reasonCode;
    }

    /** 저금통 특약 제3조: 1인당 1계좌만 가입 가능 — 다른 활성 저금통이 이미 있는지 여부. */
    public boolean hasDuplicateActive(boolean anotherActiveExistsForCustomer) {
        return isActive() && anotherActiveExistsForCustomer;
    }
}
