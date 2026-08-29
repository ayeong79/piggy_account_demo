package com.kakaobank.piggybank.dto.response;

import java.time.LocalDate;

public record SignupResponse(
        String pigAcno,
        String rtAcno,
        String cusNo,
        LocalDate enrDt
) {
}
