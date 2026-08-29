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

import java.time.LocalDate;

/**
 * RST_HIS (지급제한 이력 — 질권/압류 등).
 * 상품설명서: "질권, 압류 등 출금제한 사고신고가 등록된 경우에는 원금 또는 이자 지급이 제한됨".
 */
@Entity
@Table(name = "RST_HIS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentRestriction {

    public static final String STATUS_ACTIVE = "ACTV";
    public static final String STATUS_RELEASED = "RLSE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ_ID")
    private Long seqId;

    @Column(name = "ACNO", length = 13, nullable = false)
    private String acno;

    @Column(name = "RST_TYPE_CD", length = 10, nullable = false)
    private String rstTypeCd;

    @Column(name = "RST_ST_CD", length = 10, nullable = false)
    private String rstStCd = STATUS_ACTIVE;

    @Column(name = "STRT_DATE", nullable = false)
    private LocalDate strtDate;

    @Column(name = "END_DATE")
    private LocalDate endDate;

    public PaymentRestriction(String acno, String rstTypeCd, LocalDate strtDate) {
        this.acno = acno;
        this.rstTypeCd = rstTypeCd;
        this.rstStCd = STATUS_ACTIVE;
        this.strtDate = strtDate;
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(rstStCd);
    }
}
