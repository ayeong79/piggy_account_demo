-- 테스트에서 공통으로 필요한 최소 데이터만 적재합니다.
-- 고객/계좌 등 시나리오별 데이터는 각 테스트가 TestFixtures로 직접 구성합니다.
INSERT INTO PRD_RATE_HIST (ACCD, APLY_DT, RATE_PCT) VALUES ('PIG', DATE '2024-01-01', 4.00);
INSERT INTO PRD_RATE_HIST (ACCD, APLY_DT, RATE_PCT) VALUES ('DDA', DATE '2024-01-01', 0.10);
