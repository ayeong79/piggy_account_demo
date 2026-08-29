# 저금통(피기뱅크) 백엔드 — 로컬 시연용

카카오뱅크 "저금통" 예금 상품의 4개 업무 플로우(1.1 신규가입, 1.2 비우기, 1.3 동전모으기,
1.4 해지)를 제공된 테이블 DDL·ERD·플로우차트·클래스다이어그램을 기반으로 구현한
Spring Boot 백엔드입니다. Java/Spring Boot로 전체를 구현하고 JUnit으로 테스트합니다
(요청에 있던 "fastapi" 언급은 오타로 판단하고 반영하지 않았습니다).

## ⚠️ 먼저 읽어주세요 — 이 환경에서 확인하지 못한 부분

이 프로젝트는 인터넷이 제한된 샌드박스 환경에서 작성되었고, **Maven Central 저장소
접근이 막혀 있어 `mvn compile` / `mvn test` / `mvn spring-boot:run`을 이 환경에서
단 한 번도 실행해보지 못했습니다.** (`repo1.maven.org`, `repo.maven.apache.org` 등
5개 미러 모두 프록시에서 403으로 차단됨을 확인했습니다.) 따라서:

- 코드는 최대한 신중하게 작성했고, 모든 `.java` 파일에 대해 중괄호/괄호 짝이 맞는지
  구조적 검증을 스크립트로 돌려 통과했습니다(62개 파일, 이슈 0건). 하지만 이는
  컴파일 성공을 보장하지 않습니다.
- 핵심 계산 로직(연령 판정, 동전모으기 끝전/한도 계산, 단리 이자 계산)만은
  Spring/JPA 의존성 없이 순수 자바로 별도 추출해 `sanity-check/` 아래에 두고,
  이 환경에서 실제로 `javac`+`java`로 **컴파일·실행까지 완료해 13개 검증이 모두
  통과함을 확인**했습니다 (아래 "핵심 로직 실행 검증" 참고).
- **사용자의 로컬 환경에서 처음 `mvn test` 또는 `mvn spring-boot:run`을 실행하면
  Maven이 의존성을 인터넷에서 내려받습니다(수 분 소요 가능). 컴파일 에러가 나면
  대부분 사소한 import 누락/오타일 가능성이 높으니, 에러 메시지를 알려주시면
  바로 고치겠습니다.**

## 기술 스택

- Java 17, Spring Boot 3.3.4 (Web, Data JPA, Validation)
- H2 데이터베이스, Oracle 호환 모드(`MODE=Oracle`) — 원본 Oracle DDL을 최대한 그대로 이식
- Lombok, JUnit 5 + AssertJ + MockMvc (spring-boot-starter-test)
- 빌드: Maven

## 실행 방법

```bash
mvn spring-boot:run
```

- 서버는 `http://localhost:8080` 에서 뜹니다.
- DB는 `./data/piggybank.mv.db` 파일(H2 파일 모드)에 저장되어, 서버를 껐다 켜도
  데이터가 유지됩니다. 처음 기동 시 `schema.sql`(테이블 생성) + `data.sql`(시연용
  샘플 데이터)이 매번 실행됩니다(`spring.sql.init.mode=always`이지만 스키마는
  `CREATE TABLE IF NOT EXISTS`, 데이터는 PK 충돌 시 재기동해도 중복 삽입되지
  않도록 작성되어 있지 않으므로, **완전히 처음부터 다시 보려면 `rm -rf data/` 후
  재기동**하세요).
- H2 콘솔: `http://localhost:8080/h2-console` (JDBC URL은 `application.yml`의
  `spring.datasource.url` 값을 그대로 입력, 계정 `sa` / 빈 비밀번호)

## 테스트 방법

```bash
mvn test
```

- 전부 H2 인메모리 DB(`jdbc:h2:mem:...`)로 실행되며 실제 파일 DB에는 영향을 주지 않습니다.
- 서비스별 단위 테스트(`SignupServiceTest`, `EmptyServiceTest`, `CloseServiceTest`,
  `AutoSaveBatchServiceTest`)와 컨트롤러 레벨 테스트(`SignupControllerWebTest`),
  스프링 컨텍스트 스모크 테스트(`PiggybankApplicationTests`)로 구성됩니다.
- 테스트 중 "오늘"은 `TestClockConfig`에서 `2026-08-28`로 고정되어 있어(스프링의
  `Clock` 빈을 테스트용으로 교체), 나이 계산·이자 계산 등이 실행 시점과 무관하게
  항상 같은 결과를 냅니다.
- `AutoSaveBatchServiceTest`만 클래스 레벨 `@Transactional`이 없습니다 — 1.3 배치는
  저금통 1건마다 별도 트랜잭션(`REQUIRES_NEW`)으로 커밋되는 구조라, 테스트 트랜잭션을
  롤백해도 그 커밋들은 되돌아가지 않기 때문입니다. 대신 각 테스트가 서로 다른 ID
  prefix(A1~A9)를 써서 데이터가 섞이지 않게 했습니다.

## 핵심 로직 실행 검증 (Maven 없이 javac/java로 확인)

```bash
cd sanity-check
javac BusinessLogicSanityCheck.java && java BusinessLogicSanityCheck
```

이 환경에서 실행한 결과, 13개 검증(연령 경계값 4건, 동전모으기 끝전/한도 계산 7건,
이자 계산 2건)이 모두 `[OK]`로 통과했습니다(`결과: 13개 검증, 실패 0개`). 실제
서비스 코드(`Customer.isEligibleAge`, `AutoSaveItemProcessor`의 3~5단계,
`InterestService.settleUpTo`)와 동일한 계산식을 그대로 옮긴 것입니다.

## 데모 시나리오

```bash
mvn spring-boot:run   # 한 터미널에서 서버 기동
./demo.sh              # 다른 터미널에서 실행 (jq가 있으면 응답이 보기 좋게 출력됨)
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

### 수동으로 하나씩 호출해보고 싶다면

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
| POST | `/api/admin/customers` | (데모용) 고객 생성 |
| POST | `/api/admin/dda-accounts` | (데모용) 입출금계좌 생성 |
| POST | `/api/admin/accounts/{acno}/restrictions?rstTypeCd=` | (데모용) 지급제한 등록 |
| GET | `/api/accounts/{acno}` | 계좌 조회 |

Admin API 4종은 4개 UML 플로우 범위 밖이지만, 전제조건(고객/근거계좌/지급제한)을
만들 별도 시스템이 없는 로컬 시연 환경을 위해 최소 기능으로 추가했습니다.

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

1. **PIG_MAS의 부분 유니크 인덱스 제거**: 원본 DDL의
   `UX_PIG_MAS_RTACNO_ACTV`, `UX_PIG_MAS_CUSNO_ACTV`(둘 다 `CASE WHEN CNCL_YN='N' THEN ... END`
   형태의 조건부 유니크 인덱스)는 H2에서의 동작을 이 환경에서 검증할 수 없어
   `schema.sql`에서 제거했습니다(원본 Oracle 문법은 주석으로 보존). 대신
   `SignupService`에서 비관적 락 + `existsActiveByCusNo`/`existsActiveByRtAcno`
   명시적 조회로 애플리케이션 레벨에서 동일한 제약을 강제합니다. **운영 환경에
   이식할 때는 이 부분 인덱스를 Oracle에 그대로 복원해 DB 레벨 최후 방어선을
   추가하는 것을 권장합니다.**
2. **복합키 테이블의 대리키(surrogate key) 단순화**: `ACT_TRX`, `RST_HIS`,
   `PIG_DTL`, `PIG_ATO_EXC_HIST`, `IST_HIS`, `DLY_BLC_SNAPSHOT` 등 Oracle에서
   복합 PK(예: ACNO+TRX_SEQ_ID)를 쓰는 테이블은 JPA에서 `@EmbeddedId`/`@IdClass`
   대신 IDENTITY로 자동 채번되는 단일 컬럼을 `@Id`로 사용했습니다. 원래의 복합
   유니크 제약은 `UK_*_ACNO_SEQ` 형태로 `schema.sql`에 그대로 보존해 데이터
   무결성은 동일하게 유지됩니다.
3. **이자 계산은 단순 단리**: `InterestService`는 "(잔액 × 연이율 × 경과일수/365)를
   원 단위 절사"하는 단순 단리로 계산합니다. 실제 은행 상품의 이자 계산(일할 방식,
   중도해지이율, 우대금리 등)은 더 복잡할 수 있어 데모 목적의 근사치입니다.
4. **동전모으기 배치는 HTTP 트리거 방식**: 실제 운영에서는 스케줄러(예: Spring
   `@Scheduled` 또는 외부 배치 시스템)가 매일 자동 호출하지만, 로컬 시연 편의를
   위해 `POST /api/batch/autosave/snapshot`, `POST /api/batch/autosave/run`
   두 엔드포인트를 직접 호출하는 방식으로 구현했습니다.
5. **계좌번호는 데모용 랜덤 생성**: `AccountNumberGenerator`가 `110-####-####`
   형식으로 무작위 채번합니다(중복 시 재시도). 실제 계좌번호 채번 규칙(수표번호
   검증 로직 등)은 반영하지 않았습니다.

## 확인하지 못한 참고자료

`데이터흐름.pdf`의 1.4(해지) 샘플 데이터 페이지는 이번 세션에서 직접 열어보지
못했습니다. 다만 플로우차트 코멘트에 1.4의 단계가 문장으로 명확히 기술되어 있어
(지급제한 확인 → 잔액 이체 → 서비스 OFF → 계좌·저금통 동시 해지), 그 내용을
기준으로 `CloseService`를 구현했습니다. 샘플 데이터의 정확한 숫자와 대조가
필요하시면 알려주세요.

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
├── src/test/java/...                # JUnit 5 + MockMvc 테스트
└── sanity-check/
    └── BusinessLogicSanityCheck.java  # Maven 없이 javac/java로 핵심 계산 검증
```
