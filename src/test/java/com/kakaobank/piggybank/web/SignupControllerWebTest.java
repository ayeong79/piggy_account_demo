package com.kakaobank.piggybank.web;

import com.kakaobank.piggybank.support.TestClockConfig;
import com.kakaobank.piggybank.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 컨트롤러 계층(HTTP 요청/응답 직렬화, 검증, 예외 매핑)까지 포함한 엔드투엔드 테스트.
 * 서비스 단위 테스트(SignupServiceTest 등)가 업무 로직을, 이 테스트는 REST API 계약을 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
@Transactional
class SignupControllerWebTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TestFixtures fixtures;

    @Test
    void 신규가입_API는_201과_저금통계좌번호를_반환한다() throws Exception {
        fixtures.customer("W-CUS-001", LocalDate.of(1995, 1, 1));
        fixtures.ddaAccount("W-ACT-001", "W-CUS-001", BigDecimal.valueOf(1_000_000));

        mockMvc.perform(post("/api/piggybank/signup")
                        .contentType("application/json")
                        .content("""
                                {"cusNo":"W-CUS-001","rtAcno":"W-ACT-001"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rtAcno").value("W-ACT-001"))
                .andExpect(jsonPath("$.pigAcno").exists());
    }

    @Test
    void 필수값이_비어있으면_400과_reasonCode를_반환한다() throws Exception {
        mockMvc.perform(post("/api/piggybank/signup")
                        .contentType("application/json")
                        .content("""
                                {"cusNo":"","rtAcno":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reasonCode").value("VALIDATION_FAILED"));
    }

    @Test
    void 존재하지_않는_고객이면_404와_reasonCode를_반환한다() throws Exception {
        mockMvc.perform(post("/api/piggybank/signup")
                        .contentType("application/json")
                        .content("""
                                {"cusNo":"NO-SUCH-CUSTOMER","rtAcno":"NO-SUCH-ACCOUNT"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.reasonCode").value("CUSTOMER_NOT_FOUND"));
    }
}
