# 저금통 백엔드 — 로컬 시연용

"저금통" 상품의 4개 업무 플로우(1.1 신규가입, 1.2 비우기, 1.3 동전모으기, 1.4 해지)를 제공된 테이블 DDL·ERD·플로우차트·클래스다이어그램을 기반으로 구현한 Spring Boot 백엔드입니다. 

Java/Spring Boot로 전체를 구현하고 JUnit으로 테스트합니다.

## 기술 스택

- Java 17, Spring Boot 3.3.4 (Web, Data JPA, Validation)
- H2 데이터베이스, Oracle 호환 모드(`MODE=Oracle`) 
- Lombok, JUnit 5 + AssertJ + MockMvc (spring-boot-starter-test)
- 빌드: Maven

## 실행 방법

```bash
mvn spring-boot:run
```

- 서버는 `http://localhost:8080` 에서 뜹니다.
- DB는 `./data/piggybank.mv.db` 파일(H2 파일 모드)에 저장되어, 서버를 껐다 켜도 데이터가 유지됩니다. 
  처음 기동 시 `schema.sql`(테이블 생성) + `data.sql`(시연용 샘플 데이터)이 매번 실행됩니다(`spring.sql.init.mode=always`이지만 스키마는 `CREATE TABLE IF NOT EXISTS`, 데이터는 PK 충돌 시 재기동해도 중복 삽입되지 않도록 작성되어 있지 않으므로, 
  **완전히 처음부터 다시 보려면 `rm -rf data/` 후 재기동**하세요).
- H2 콘솔: `http://localhost:8080/h2-console` (JDBC URL은 `application.yml`의 `spring.datasource.url` 값을 그대로 입력, 계정 `sa` / 빈 비밀번호)

## 데모 방법

```bash
mvn spring-boot:run    # 한 터미널에서 서버 기동
./demo.sh              # 다른 터미널에서 실행 
```

`demo.sh`는 `data.sql`의 시연용 데이터를 이용해 4개 플로우를 순서대로 호출합니다:

| 단계 | 호출 | 기대 결과 |
|---|---|---|
| 1.1 실패① | 미성년 고객(C0000000002)으로 가입 | `400 AGE_NOT_ELIGIBLE` |
| 1.1 실패② | 이미 저금통 보유 고객(C0000000003)으로 가입 | `409 DUPLICATE_SIGNUP` |
| 1.1 실패③ | 근거계좌로 저금통(PIG) 계좌 지정 | `400 NOT_DDA_ACCOUNT` |
| 1.1 실패④ | 지급제한 걸린 근거계좌(110-5566-7788) | `400 BASE_ACCOUNT_NOT_ACTIVE` |
| 1.1 실패⑤ | 모임통장 근거계좌(110-3344-7788) | `400 GROUP_ACCOUNT_RESTRICTED` |
| 1.1 성공 | C0000000001 + 110-2233-4455 | `201`, 새 저금통계좌 발급 |
| 1.3 배치 | 스냅샷 적재 → 익일 배치 실행 | 1,325,150원의 끝전 150원이 자동 적립 (`SUCC`) |
| 1.2 | 기존 저금통(110-9988-7766, 45,000원) 비우기 | 전액이 근거계좌로 이체, 잔액 0원 |
| 1.4 실패 | 지급제한 등록 후 해지 시도 | `409 PAYMENT_RESTRICTED` |
| 1.4 성공 | 새로 만든 저금통 해지 | 잔액 반환 + 계좌/저금통 동시 해지 |

각 단계의 HTTP 상태코드와 `reasonCode`는 `GlobalExceptionHandler`가 JSON으로
일관되게 응답합니다(`{"reasonCode": "...", "message": "...", "termsReference": "..."}`).

### 수동 호출

```bash
# 신규가입
curl -X POST localhost:8080/api/piggybank/signup \
  -H 'Content-Type: application/json' \
  -d '{"cusNo":"C0000000001","rtAcno":"110-2233-4455"}'

# 저금통 상태 조회 (위 응답의 pigAcno 사용)
curl localhost:8080/api/piggybank/{pigAcno}

# 비우기 / 해지
curl -X POST localhost:8080/api/piggybank/{pigAcno}/empty
curl -X POST localhost:8080/api/piggybank/{pigAcno}/close

# 동전모으기 배치 (스냅샷 → 실행 순서로)
curl -X POST "localhost:8080/api/batch/autosave/snapshot?date=2026-08-28"
curl -X POST "localhost:8080/api/batch/autosave/run?date=2026-08-29"

# 자동저축 이력 조회
curl localhost:8080/api/piggybank/{pigAcno}/autosave-history
```

## API 목록

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/piggybank/signup` | 1.1 신규가입 |
| GET | `/api/piggybank/{pigAcno}` | 저금통 상태 조회 |
| POST | `/api/piggybank/{pigAcno}/empty` | 1.2 비우기 |
| POST | `/api/piggybank/{pigAcno}/close` | 1.4 해지 |
| GET | `/api/piggybank/{pigAcno}/autosave-history` | 자동저축 이력 조회 |
| POST | `/api/batch/autosave/snapshot?date=` | 1.3-1단계: 전일 잔액 스냅샷 적재 (미지정 시 어제) |
| POST | `/api/batch/autosave/run?date=` | 1.3-2~9단계: 동전모으기 배치 실행 (미지정 시 오늘) |
| GET | `/api/accounts/{acno}` | 계좌 조회 |
| POST | `/api/admin/customers` | (데모용) 고객 생성 |
| POST | `/api/admin/dda-accounts` | (데모용) 입출금계좌 생성 |
| POST | `/api/admin/accounts/{acno}/restrictions?rstTypeCd=` | (데모용) 지급제한 등록 |

Admin API는 4개 UML 다이어그램 범위 밖이지만, 전제조건(고객/근거계좌/지급제한)을 만들 별도 시스템이 없는 로컬 시연 환경을 위해 최소 기능으로 추가했습니다.

## 시드 데이터 (`src/main/resources/data.sql`)

| 종류 | 값 | 용도 |
|---|---|---|
| 고객 | C0000000001 (1998-05-12생) | 정상 가입 가능 |
| 고객 | C0000000002 (2015-01-01생) | 연령 미달 데모 |
| 고객 | C0000000003 (1990-03-03생) | 이미 저금통 보유 |
| 계좌 | 110-2233-4455 (DDA, 1,325,150원) | 정상 근거계좌 |
| 계좌 | 110-3344-7788 (DDA, 모임통장) | 모임통장 제한 데모 |
| 계좌 | 110-4455-6677 (DDA, 2,000,000원) | C0000000003의 근거계좌 |
| 계좌 | 110-9988-7766 (PIG, 45,000원) | C0000000003의 기존 저금통 |
| 계좌 | 110-5566-7788 (DDA, 지급제한 ACTV) | 지급제한 데모 |
| 금리 | PIG 4.00%, DDA 0.10% (2024-01-01부터) | 이자 정산 계산용 |

## 설계에서 단순화한 부분

원본 DDL/플로우를 최대한 충실히 옮기되, 오프라인 환경에서 검증 불가능한 부분은
아래와 같이 안전한 방향으로 단순화하고 코드/주석에 명시했습니다.

1. **이자 계산은 단순 단리**: `InterestService`는 "(잔액 × 연이율 × 경과일수/365)를 원 단위 절사"하는 단순 단리로 계산.
2. **동전모으기 배치는 HTTP 트리거 방식**: 실제 운영에서는 스케줄러(예: Spring  `@Scheduled` 또는 외부 배치 시스템)가 매일 자동 호출하지만, 
   로컬 시연 편의를 위해 `POST /api/batch/autosave/snapshot`, `POST /api/batch/autosave/run`과 같이 직접 호출하는 방식으로 구현했습니다.
3. **계좌번호는 데모용 랜덤 생성**: `AccountNumberGenerator`가 `110-####-####` 형식으로 무작위 채번합니다(중복 시 재시도). 


## 프로젝트 구조

```
piggybank-backend/
├── pom.xml
├── demo.sh                          # 데모 시나리오 curl 스크립트
├── src/main/resources/
│   ├── application.yml
│   ├── schema.sql                   # 원본 Oracle DDL → H2 이식 (주석에 단순화 내역 명시)
│   └── data.sql                     # 시연용 시드 데이터
├── src/main/java/com/kakaobank/piggybank/
│   ├── domain/                      # ACT_MAS(Single Table Inheritance) 등 10개 테이블 엔티티
│   ├── repository/                  # Spring Data JPA 리포지토리
│   ├── service/                     # 1.1~1.4 플로우 + 배치 + 관리용 서비스
│   ├── web/                         # REST 컨트롤러
│   ├── dto/                         # 요청/응답 DTO
│   ├── exception/                   # 사유코드 기반 업무 예외 + 전역 예외 핸들러
│   └── config/                      # Clock 빈 (테스트 가능성을 위한 시간 주입)
└── src/test/java/...                # JUnit 5 + MockMvc 테스트
