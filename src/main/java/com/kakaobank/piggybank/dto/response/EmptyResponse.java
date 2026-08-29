package com.kakaobank.piggybank.dto.response;

import java.math.BigDecimal;

public record EmptyResponse(
        String pigAcno,
        String rtAcno,
        BigDecimal transferredAmount,
        BigDecimal interestSettled,
        BigDecimal pigBalanceAfter,
        BigDecimal rtBalanceAfter
) {
}
