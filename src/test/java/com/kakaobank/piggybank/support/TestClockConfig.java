package com.kakaobank.piggybank.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 테스트를 결정적으로 만들기 위해 "오늘"을 2026-08-28로 고정한다.
 * (연령 판정, 무거래 기간 계산 등 날짜에 민감한 로직을 안정적으로 검증하기 위함)
 */
@TestConfiguration
public class TestClockConfig {

    public static final LocalDate FIXED_TODAY = LocalDate.of(2026, 8, 28);

    @Bean
    @Primary
    public Clock clock() {
        return Clock.fixed(FIXED_TODAY.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul"));
    }
}
