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

import java.time.LocalDateTime;

/**
 * PIG_DTL (저금통 부가서비스 설정 이력 — 서비스별 정규화).
 *
 * (PIG_ACNO, SVC_CD)별로 "최신 SEQ_ID 행"이 현재 설정 상태다. UPDATE가 아니라
 * 항상 새 행을 INSERT해서 이력을 남긴다 (1.4 해지 4단계: 서비스 OFF 이력도 이 방식).
 *
 * [모델링 메모] 물리 PK는 (PIG_ACNO, SVC_CD, SEQ_ID) 복합키지만 SEQ_ID가 테이블
 * 전체에서 유일한 IDENTITY이므로 JPA @Id는 SEQ_ID 단일 컬럼으로 둔다.
 */
@Entity
@Table(name = "PIG_DTL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PiggyBankDetail {

    public static final String SVC_COIN = "COIN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ_ID")
    private Long seqId;

    @Column(name = "PIG_ACNO", length = 13, nullable = false)
    private String pigAcno;

    @Column(name = "SVC_CD", length = 10, nullable = false)
    private String svcCd;

    @Column(name = "USE_YN", length = 1, nullable = false)
    private String useYn;

    @Column(name = "APLY_DTTM", nullable = false)
    private LocalDateTime aplyDttm;

    @Column(name = "RG_GB_CD", length = 10)
    private String rgGbCd;

    public PiggyBankDetail(String pigAcno, String svcCd, String useYn, LocalDateTime aplyDttm, String rgGbCd) {
        this.pigAcno = pigAcno;
        this.svcCd = svcCd;
        this.useYn = useYn;
        this.aplyDttm = aplyDttm;
        this.rgGbCd = rgGbCd;
    }

    public boolean isUsing() {
        return "Y".equals(useYn);
    }

    /** 서비스 활성화 (신규가입 시 동전모으기 기본 등록 등). */
    public static PiggyBankDetail activate(String pigAcno, String svcCd, LocalDateTime now) {
        return new PiggyBankDetail(pigAcno, svcCd, "Y", now, "NEW");
    }

    /** 서비스 OFF 이력 (해지 시). */
    public static PiggyBankDetail deactivate(String pigAcno, String svcCd, LocalDateTime now) {
        return new PiggyBankDetail(pigAcno, svcCd, "N", now, "CLOSE");
    }
}
