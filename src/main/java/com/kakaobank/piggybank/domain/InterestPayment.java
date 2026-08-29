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
 * IST_HIS (이자지급 이력).
 * 입출금이자유로운예금약관 제2조③: "...계좌해지 또는 추가 입출금 거래 발생일에 일괄계산하여 지급".
 */
@Entity
@Table(name = "IST_HIS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterestPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ_ID")
    private Long seqId;

    @Column(name = "ACNO", length = 13, nullable = false)
    private String acno;

    @Column(name = "FR_DT", nullable = false)
    private LocalDate frDt;

    @Column(name = "TO_DT", nullable = false)
    private LocalDate toDt;

    @Column(name = "PAY_DT", nullable = false)
    private LocalDate payDt;

    @Column(name = "INT_AMT", precision = 15, nullable = false)
    private BigDecimal intAmt;

    @Column(name = "TRX_SEQ_ID")
    private Long trxSeqId;

    public InterestPayment(String acno, LocalDate frDt, LocalDate toDt, LocalDate payDt, BigDecimal intAmt) {
        this.acno = acno;
        this.frDt = frDt;
        this.toDt = toDt;
        this.payDt = payDt;
        this.intAmt = intAmt;
    }

    public void linkTransaction(Long trxSeqId) {
        this.trxSeqId = trxSeqId;
    }

    public boolean isPaid() {
        return trxSeqId != null;
    }
}
