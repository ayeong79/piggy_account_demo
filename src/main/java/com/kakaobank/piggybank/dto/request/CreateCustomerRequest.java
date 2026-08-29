package com.kakaobank.piggybank.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record CreateCustomerRequest(
        @NotBlank String cusNo,
        @NotNull @Past LocalDate birthDate
) {
}
