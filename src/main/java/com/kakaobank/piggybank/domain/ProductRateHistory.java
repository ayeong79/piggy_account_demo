package com.kakaobank.piggybank.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PRD_RATE_HIST (상품별 금리 이력).
 * 1.2/1.3/1.4의 "미정산 이자 반영" 계산에 사용하는 연 금리표.
 */
@Entity
@Table(name = "PRD_RATE_HIST")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductRateHistory {

    @EmbeddedId
    private Id id;

    @Column(name = "RATE_PCT", precision = 5, scale = 2, nullable = false)
    private BigDecimal ratePct;

    public ProductRateHistory(String accd, LocalDate aplyDt, BigDecimal ratePct) {
        this.id = new Id(accd, aplyDt);
        this.ratePct = ratePct;
    }

    public String getAccd() {
        return id.accd;
    }

    public LocalDate getAplyDt() {
        return id.aplyDt;
    }

    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "ACCD", length = 10)
        private String accd;

        @Column(name = "APLY_DT")
        private LocalDate aplyDt;

        public Id(String accd, LocalDate aplyDt) {
            this.accd = accd;
            this.aplyDt = aplyDt;
        }
    }
}
