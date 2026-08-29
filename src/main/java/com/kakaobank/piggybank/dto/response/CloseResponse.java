package com.kakaobank.piggybank.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CloseResponse(
        String pigAcno,
        String rtAcno,
        BigDecimal interestSettled,
        BigDecimal settledAmount,
        LocalDate closedDate,
        int deactivatedServiceCount
) {
}
