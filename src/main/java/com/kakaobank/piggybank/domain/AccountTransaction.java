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
 * ACT_TRX (계좌 거래내역 — 이중기장).
 * 저금통 특약 제6조①②: 근거계좌로의 이체를 통해서만, 전액만 출금 가능 — 이 규칙 때문에
 * 1.2/1.3/1.4가 전부 "출금 쪽 한 행 + 입금 쪽 한 행"을 같은 TRX_GRP_ID로 남기는
 * 동일한 이중기장 패턴을 재사용한다 ({@link com.kakaobank.piggybank.service.TransferService}).
 */
@Entity
@Table(name = "ACT_TRX")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountTransaction {

    public static final String TYPE_EMPTY = "EMPTY";           // 1.2 비우기
    public static final String TYPE_COIN_SAVE = "COIN_SAVE";   // 1.3 동전모으기
    public static final String TYPE_CLOSE_SETTLE = "CLOSE_SETTLE"; // 1.4 해지 잔액정산
    public static final String TYPE_INTEREST = "INTEREST";     // 이자 지급(자기 계좌, 상대계좌 없음)

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRX_SEQ_ID")
    private Long trxSeqId;

    @Column(name = "ACNO", length = 13, nullable = false)
    private String acno;

    @Column(name = "TRX_DATE", nullable = false)
    private LocalDate trxDate;

    @Column(name = "TRX_TYPE_CD", length = 20, nullable = false)
    private String trxTypeCd;

    /** 출금이면 음수, 입금이면 양수. */
    @Column(name = "TRX_AMT", precision = 15, nullable = false)
    private BigDecimal trxAmt;

    @Column(name = "BF_BAL_AMT", precision = 15, nullable = false)
    private BigDecimal bfBalAmt;

    @Column(name = "AF_BAL_AMT", precision = 15, nullable = false)
    private BigDecimal afBalAmt;

    @Column(name = "CNTP_ACNO", length = 13)
    private String cntpAcno;

    @Column(name = "TRX_GRP_ID")
    private Long trxGrpId;

    public AccountTransaction(String acno, LocalDate trxDate, String trxTypeCd, BigDecimal trxAmt,
                               BigDecimal bfBalAmt, BigDecimal afBalAmt, String cntpAcno) {
        this.acno = acno;
        this.trxDate = trxDate;
        this.trxTypeCd = trxTypeCd;
        this.trxAmt = trxAmt;
        this.bfBalAmt = bfBalAmt;
        this.afBalAmt = afBalAmt;
        this.cntpAcno = cntpAcno;
    }

    public void assignGroupId(Long trxGrpId) {
        this.trxGrpId = trxGrpId;
    }

    public boolean isDebit() {
        return trxAmt.signum() < 0;
    }
}
