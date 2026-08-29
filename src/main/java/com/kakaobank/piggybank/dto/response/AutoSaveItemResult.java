package com.kakaobank.piggybank.dto.response;

import java.math.BigDecimal;

public record AutoSaveItemResult(
        String pigAcno,
        String status,       // SUCC / SKIP / FAIL
        String reasonCode,   // SKIP_RSN_CD 또는 FAIL_RSN_CD (성공이면 null)
        BigDecimal calcAmt,
        BigDecimal excAmt
) {
}
