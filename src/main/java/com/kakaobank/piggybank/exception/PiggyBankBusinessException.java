package com.kakaobank.piggybank.exception;

import org.springframework.http.HttpStatus;

/**
 * 4개 플로우(1.1~1.4)에서 발생하는 업무 예외 공통 타입.
 * reasonCode는 화면/로그/테스트에서 분기할 수 있는 안정적인 식별자이고,
 * termsReference는 근거 약관 조항(플로우차트 코멘트.txt 기준)을 담아 왜 막혔는지 바로 알 수 있게 한다.
 */
public class PiggyBankBusinessException extends RuntimeException {

    private final String reasonCode;
    private final HttpStatus httpStatus;
    private final String termsReference;

    public PiggyBankBusinessException(String reasonCode, HttpStatus httpStatus, String message, String termsReference) {
        super(message);
        this.reasonCode = reasonCode;
        this.httpStatus = httpStatus;
        this.termsReference = termsReference;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getTermsReference() {
        return termsReference;
    }
}
