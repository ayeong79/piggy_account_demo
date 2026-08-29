package com.kakaobank.piggybank.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ACT_MAS (계좌 — 입출금계좌/저금통계좌 공통).
 *
 * 물리적으로 ACT_MAS는 단일 테이블이고 ACCD 컬럼('DDA'/'PIG')으로 상품구분이 갈립니다.
 * 클래스 다이어그램에서 합의한 대로, ACT_MAS의 모든 컬럼은 어떤 ACCD든 동일하게 갖는
 * 물리 컬럼이므로 전부 이 추상 클래스(Account)에 두고, {@link DdaAccount}/{@link PiggyBankAccount}는
 * 속성 없이 동작(메서드)만 다릅니다 (Single Table Inheritance).
 */
@Entity
@Table(name = "ACT_MAS")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "ACCD", discriminatorType = DiscriminatorType.STRING, length = 10)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Account {

    @Id
    @Column(name = "ACNO", length = 13)
    private String acno;

    @Column(name = "CUS_NO", length = 20, nullable = false)
    private String cusNo;

    @Column(name = "BAL_AMT", precision = 15, nullable = false)
    private BigDecimal balAmt = BigDecimal.ZERO;

    @Column(name = "CNCL_YN", length = 1, nullable = false)
    private String cnclYn = "N";

    @Column(name = "RST_YN", length = 1, nullable = false)
    private String rstYn = "N";

    @Column(name = "GRP_ACNO_YN", length = 1, nullable = false)
    private String grpAcnoYn = "N";

    @Column(name = "OPN_DT", nullable = false)
    private LocalDate opnDt;

    @Column(name = "CLS_DT")
    private LocalDate clsDt;

    @Column(name = "CNCL_CD", length = 100)
    private String cnclCd;

    @Column(name = "LAST_TRX_DT", nullable = false)
    private LocalDate lastTrxDt;

    @Column(name = "ISTOP_YN", length = 1, nullable = false)
    private String istopYn = "N";

    @Column(name = "DORM_YN", length = 1, nullable = false)
    private String dormYn = "N";

    protected Account(String acno, String cusNo, LocalDate opnDt) {
        this.acno = acno;
        this.cusNo = cusNo;
        this.balAmt = BigDecimal.ZERO;
        this.cnclYn = "N";
        this.rstYn = "N";
        this.grpAcnoYn = "N";
        this.opnDt = opnDt;
        this.lastTrxDt = opnDt;
        this.istopYn = "N";
        this.dormYn = "N";
    }

    // ---- 조회용 상태 판별 메서드 (클래스 다이어그램의 Account 메서드) ----

    public boolean isActive() {
        return "N".equals(cnclYn);
    }

    public boolean isDormant() {
        return "Y".equals(dormYn);
    }

    public boolean isInterestSuspended() {
        return "Y".equals(istopYn);
    }

    public boolean isGroupAccount() {
        return "Y".equals(grpAcnoYn);
    }

    // ---- 상태 변경 (서비스 계층에서만 호출) ----

    public void deposit(BigDecimal amount, LocalDate trxDate) {
        this.balAmt = this.balAmt.add(amount);
        this.lastTrxDt = trxDate;
    }

    public void withdraw(BigDecimal amount, LocalDate trxDate) {
        this.balAmt = this.balAmt.subtract(amount);
        this.lastTrxDt = trxDate;
    }

    public void touchLastTrxDate(LocalDate trxDate) {
        this.lastTrxDt = trxDate;
    }

    public void resumeInterest() {
        this.istopYn = "N";
    }

    public void suspendInterestIfDormant() {
        this.istopYn = "Y";
    }

    public void close(LocalDate closeDate, String reasonCode) {
        this.cnclYn = "Y";
        this.clsDt = closeDate;
        this.cnclCd = reasonCode;
    }

    // ---- 데모/관리자용 시딩 메서드 (AdminController를 통한 테스트 데이터 구성 전용) ----

    public void seedBalance(BigDecimal openingBalance) {
        this.balAmt = openingBalance;
    }

    public void markAsGroupAccount() {
        this.grpAcnoYn = "Y";
    }

    public abstract String accountTypeCode();
}
