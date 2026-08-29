package com.kakaobank.piggybank.dto.request;

import jakarta.validation.constraints.NotBlank;

/** 1.1 신규가입 요청. */
public record SignupRequest(
        @NotBlank(message = "고객번호는 필수입니다") String cusNo,
        @NotBlank(message = "근거계좌번호는 필수입니다") String rtAcno
) {
}
