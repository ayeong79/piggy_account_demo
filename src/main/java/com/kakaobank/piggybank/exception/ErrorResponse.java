package com.kakaobank.piggybank.exception;

import java.time.Instant;

public record ErrorResponse(
        String reasonCode,
        String message,
        String termsReference,
        Instant timestamp
) {
    public static ErrorResponse of(PiggyBankBusinessException e) {
        return new ErrorResponse(e.getReasonCode(), e.getMessage(), e.getTermsReference(), Instant.now());
    }

    public static ErrorResponse of(String reasonCode, String message) {
        return new ErrorResponse(reasonCode, message, null, Instant.now());
    }
}
