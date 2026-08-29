package com.kakaobank.piggybank.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateDdaAccountRequest(
        @NotBlank String cusNo,
        @NotNull @PositiveOrZero BigDecimal initialBalance,
        boolean groupAccount
) {
}
