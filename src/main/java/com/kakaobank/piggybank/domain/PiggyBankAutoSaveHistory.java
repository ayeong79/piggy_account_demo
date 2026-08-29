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
 * PIG_ATO_EXC_HIST (저금통 자동저축 실행 이력).
 * 1.3 자동저축 배치가 저금통 1건을 처리할 때마다(성공/SKIP/FAIL 모두) 남기는 실행 로그.
 */
@Entity
@Table(name = "PIG_ATO_EXC_HIST")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PiggyBankAutoSaveHistory {

    public static final String STATUS_SUCCESS = "SUCC";
    public static final String STATUS_SKIP = "SKIP";
    public static final String STATUS_FAIL = "FAIL";

    // SKIP 사유코드
    public static final String SKIP_NO_CHANGE = "NO_CHANGE";     // 끝전이 0원
    public static final String SKIP_LIMIT_EXCD = "LIMIT_EXCD";   // 10만원 한도 초과
    public static final String SKIP_RST_ACTIVE = "RST_ACTIVE";   // 근거계좌 지급제한 (신규 추가된 체크)
    public static final String SKIP_BAL_LE_1000 = "BAL_LE_1000"; // 근거계좌 잔액 1,000원 이하

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ_ID")
    private Long seqId;

    @Column(name = "PIG_ACNO", length = 13, nullable = false)
    private String pigAcno;

    @Column(name = "SVC_CD", length = 10, nullable = false)
    private String svcCd;

    @Column(name = "RT_ACNO", length = 13, nullable = false)
    private String rtAcno;

    @Column(name = "EXC_DATE", nullable = false)
    private LocalDate excDate;

    @Column(name = "PREV_BAL_AMT", precision = 15)
    private BigDecimal prevBalAmt;

    @Column(name = "CUR_BAL_AMT", precision = 15)
    private BigDecimal curBalAmt;

    @Column(name = "CALC_AMT", precision = 15, nullable = false)
    private BigDecimal calcAmt = BigDecimal.ZERO;

    @Column(name = "EXC_AMT", precision = 15, nullable = false)
    private BigDecimal excAmt = BigDecimal.ZERO;

    @Column(name = "EXC_ST_CD", length = 10, nullable = false)
    private String excStCd;

    @Column(name = "SKIP_RSN_CD", length = 20)
    private String skipRsnCd;

    @Column(name = "FAIL_RSN_CD", length = 20)
    private String failRsnCd;

    @Column(name = "TRX_SEQ_ID")
    private Long trxSeqId;

    private PiggyBankAutoSaveHistory(String pigAcno, String svcCd, String rtAcno, LocalDate excDate,
                                      BigDecimal prevBalAmt, BigDecimal curBalAmt,
                                      BigDecimal calcAmt, BigDecimal excAmt, String excStCd,
                                      String skipRsnCd, String failRsnCd) {
        this.pigAcno = pigAcno;
        this.svcCd = svcCd;
        this.rtAcno = rtAcno;
        this.excDate = excDate;
        this.prevBalAmt = prevBalAmt;
        this.curBalAmt = curBalAmt;
        this.calcAmt = calcAmt == null ? BigDecimal.ZERO : calcAmt;
        this.excAmt = excAmt == null ? BigDecimal.ZERO : excAmt;
        this.excStCd = excStCd;
        this.skipRsnCd = skipRsnCd;
        this.failRsnCd = failRsnCd;
    }

    public static PiggyBankAutoSaveHistory success(String pigAcno, String svcCd, String rtAcno, LocalDate excDate,
                                                     BigDecimal prevBalAmt, BigDecimal curBalAmt,
                                                     BigDecimal calcAmt, BigDecimal excAmt, Long trxSeqId) {
        PiggyBankAutoSaveHistory h = new PiggyBankAutoSaveHistory(pigAcno, svcCd, rtAcno, excDate,
                prevBalAmt, curBalAmt, calcAmt, excAmt, STATUS_SUCCESS, null, null);
        h.trxSeqId = trxSeqId;
        return h;
    }

    public static PiggyBankAutoSaveHistory skip(String pigAcno, String svcCd, String rtAcno, LocalDate excDate,
                                                  BigDecimal prevBalAmt, BigDecimal curBalAmt,
                                                  BigDecimal calcAmt, String skipReasonCode) {
        return new PiggyBankAutoSaveHistory(pigAcno, svcCd, rtAcno, excDate,
                prevBalAmt, curBalAmt, calcAmt, BigDecimal.ZERO, STATUS_SKIP, skipReasonCode, null);
    }

    public static PiggyBankAutoSaveHistory fail(String pigAcno, String svcCd, String rtAcno, LocalDate excDate,
                                                  String failReasonCode) {
        return new PiggyBankAutoSaveHistory(pigAcno, svcCd, rtAcno, excDate,
                null, null, BigDecimal.ZERO, BigDecimal.ZERO, STATUS_FAIL, null, failReasonCode);
    }
}
