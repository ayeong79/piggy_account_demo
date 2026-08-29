package com.kakaobank.piggybank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 서비스 전반에서 "오늘"을 일관되게 다루기 위한 Clock 빈.
 * 테스트에서는 이 빈을 고정된 Clock으로 교체해 특정 날짜 기준 시나리오(예: 만 14세 경계,
 * 5년 무거래 휴면 등)를 결정적으로 재현할 수 있다.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
