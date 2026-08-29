package com.kakaobank.piggybank;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 스프링 컨텍스트(모든 빈, JPA 매핑, schema.sql)가 정상적으로 뜨는지 확인하는 스모크 테스트. */
@SpringBootTest
@ActiveProfiles("test")
class PiggybankApplicationTests {

    @Test
    void contextLoads() {
    }
}
