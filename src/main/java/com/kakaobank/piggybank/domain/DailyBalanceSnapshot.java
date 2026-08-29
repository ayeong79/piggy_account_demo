package com.kakaobank.piggybank.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DLY_BLC_SNAPSHOT (일별 잔액 스냅샷).
 * 저금통 특약 제9조①2: "동전모으기 저축금액은 근거계좌 전일자 최종 잔액의 1천원 미만에 해당하는 금액".
 *
 * ACT_MAS.BAL_AMT(당일 실시간 잔액)와 분리된, 배치 전용 "전일 마감 잔액" 고정 테이블.
 * ACNO를 참조하지만 FK 제약이 선언되어 있지 않다(클래스 다이어그램 상 Account에 대한
 * Dependency(«use») 관계 — 배치가 적재 시점에만 ACT_MAS를 읽어 값을 복사해 둘 뿐,
 * 지속적인 참조 무결성을 강제하지 않는다).
 */
@Entity
@Table(name = "DLY_BLC_SNAPSHOT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyBalanceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ_ID")
    private Long seqId;

    @Column(name = "SNAP_DATE", nullable = false)
    private LocalDate snapDate;

    @Column(name = "ACNO", length = 13, nullable = false)
    private String acno;

    @Column(name = "CUS_NO", length = 20, nullable = false)
    private String cusNo;

    @Column(name = "BAL_AMT", precision = 15, nullable = false)
    private BigDecimal balAmt;

    @Column(name = "PROG_ID", length = 30, nullable = false)
    private String progId;

    public DailyBalanceSnapshot(LocalDate snapDate, String acno, String cusNo, BigDecimal balAmt, String progId) {
        this.snapDate = snapDate;
        this.acno = acno;
        this.cusNo = cusNo;
        this.balAmt = balAmt;
        this.progId = progId;
    }
}
