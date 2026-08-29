#!/usr/bin/env bash
# ============================================================================
# 저금통 백엔드 로컬 시연 스크립트
# 사전조건: mvn spring-boot:run 실행
# 이 스크립트는 src/main/resources/data.sql 에 적재된 시연용 데이터 사용.
# 두번째 시도시 rm -rf data/ 후 재기동.
#
# 사용법: ./demo.sh   (jq 설치 권장 - 출력)
# ============================================================================
set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

# jq가 있으면 pretty-print, 없으면 원문 그대로 출력
pp() {
  if command -v jq >/dev/null 2>&1; then
    jq . 2>/dev/null || cat
  else
    cat
  fi
}

step() {
  echo
  echo "----------------------------------------------------------------------"
  echo "▶ $1"
  echo "----------------------------------------------------------------------"
}

req() {
  # req METHOD PATH [BODY]
  local method="$1" path="$2" body="${3:-}"
  echo "$ curl -s -w '\\n[HTTP %{http_code}]\\n' -X $method $BASE_URL$path ${body:+-H 'Content-Type: application/json' -d '$body'}"
  if [ -n "$body" ]; then
    curl -s -w '\n[HTTP %{http_code}]\n' -X "$method" "$BASE_URL$path" \
      -H 'Content-Type: application/json' -d "$body" | pp
  else
    curl -s -w '\n[HTTP %{http_code}]\n' -X "$method" "$BASE_URL$path" | pp
  fi
}

# GNU date(Linux)와 BSD date(macOS) 둘 다 지원
tomorrow() {
  date -d "+1 day" +%F 2>/dev/null || date -v+1d +%F
}

TODAY=$(date +%F)
TOMORROW=$(tomorrow)

echo "=========================================================================="
echo " 저금통 백엔드 데모  (BASE_URL=$BASE_URL, TODAY=$TODAY, TOMORROW=$TOMORROW)"
echo "=========================================================================="

# ── 1.1 신규가입: 실패 케이스 5종 ───────────────────────────────────────────
# (성공 케이스보다 먼저 실행해야 합니다 — 성공 후에는 C0000000001이 이미
#  활성 저금통을 갖게 되어 DUPLICATE_SIGNUP으로 결과가 달라집니다.)

step "1.1-실패① 연령 미달 (C0000000002, 2015년생) → 400 AGE_NOT_ELIGIBLE"
req POST /api/piggybank/signup '{"cusNo":"C0000000002","rtAcno":"110-2233-4455"}'

step "1.1-실패② 이미 저금통 보유 (C0000000003) → 409 DUPLICATE_SIGNUP"
req POST /api/piggybank/signup '{"cusNo":"C0000000003","rtAcno":"110-4455-6677"}'

step "1.1-실패③ 근거계좌가 DDA가 아님 (저금통 계좌 110-9988-7766을 근거계좌로 지정) → 400 NOT_DDA_ACCOUNT"
req POST /api/piggybank/signup '{"cusNo":"C0000000001","rtAcno":"110-9988-7766"}'

step "1.1-실패④ 근거계좌 지급제한 (110-5566-7788, RST_HIS ACTV) → 400 BASE_ACCOUNT_NOT_ACTIVE"
req POST /api/piggybank/signup '{"cusNo":"C0000000001","rtAcno":"110-5566-7788"}'

step "1.1-실패⑤ 모임통장 근거계좌 불가 (110-3344-7788) → 400 GROUP_ACCOUNT_RESTRICTED"
req POST /api/piggybank/signup '{"cusNo":"C0000000001","rtAcno":"110-3344-7788"}'

# ── 1.1 신규가입: 성공 ──────────────────────────────────────────────────────

step "1.1-성공 신규가입 (C0000000001 + 110-2233-4455) → 201 Created"
SIGNUP_RESPONSE=$(curl -s -X POST "$BASE_URL/api/piggybank/signup" \
  -H 'Content-Type: application/json' \
  -d '{"cusNo":"C0000000001","rtAcno":"110-2233-4455"}')
echo "$SIGNUP_RESPONSE" | pp

if command -v jq >/dev/null 2>&1; then
  NEW_PIG=$(echo "$SIGNUP_RESPONSE" | jq -r '.pigAcno')
else
  # jq 없이 pigAcno 값만 대충 추출 (데모 목적)
  NEW_PIG=$(echo "$SIGNUP_RESPONSE" | sed -n 's/.*"pigAcno":"\([^"]*\)".*/\1/p')
fi

if [ -z "${NEW_PIG:-}" ] || [ "$NEW_PIG" = "null" ]; then
  echo
  echo "!! 신규가입에 실패해 새 저금통 계좌번호를 얻지 못했습니다."
  echo "!! (이미 한 번 demo.sh를 실행한 뒤라면 C0000000001이 이미 저금통을 보유 중일 수 있습니다."
  echo "!!  서버를 끄고 'rm -rf data/' 후 재기동해서 처음부터 다시 시도해 보세요.)"
  echo "!! 이후 단계는 새로 만든 저금통 없이는 계속할 수 없어 스크립트를 종료합니다."
  exit 1
fi
echo
echo ">> 새로 생성된 저금통 계좌번호: $NEW_PIG"

step "새 저금통 상태 조회 (잔액 0원이어야 함)"
req GET "/api/piggybank/$NEW_PIG"

# ── 1.3 동전모으기(자동저축) 배치 ───────────────────────────────────────────
# 스냅샷은 "오늘"의 DDA 잔액을 적재하고, 배치는 "내일" 날짜로 실행
# 스냅샷(전일자=오늘)을 사용한다. 오늘 막 가입한 저금통은 오늘 배치 대상에서 제외되므로(당일가입 제외 규칙) 
# 배치 실행일을 하루 뒤로 지정해야 새 저금통이 대상에 포함.

step "1.3-1단계 전일 마감 스냅샷 적재 (date=$TODAY) — 110-2233-4455 잔액 1,325,150원 포함"
req POST "/api/batch/autosave/snapshot?date=$TODAY"

step "1.3-2~9단계 자동저축 배치 실행 (date=$TOMORROW, 전일자=$TODAY 스냅샷 사용)"
req POST "/api/batch/autosave/run?date=$TOMORROW"
echo
echo ">> 1,325,150원의 끝전(1000원 미만) 150원이 저금통으로 자동 이체되어야 합니다."
echo ">> (데이터흐름.pdf 샘플과 동일한 숫자 — sanity-check/BusinessLogicSanityCheck.java 로도 별도 검증됨)"

step "새 저금통의 자동저축 이력 조회 (SUCC, CALC_AMT=150, EXC_AMT=150 이어야 함)"
req GET "/api/piggybank/$NEW_PIG/autosave-history"

step "근거계좌 잔액 확인 (1,325,150 → 1,325,000원이어야 함)"
req GET "/api/accounts/110-2233-4455"

# ── 1.2 저금통 비우기 (기존 시드 데이터의 저금통 사용) ─────────────────────

step "1.2 저금통 비우기 (110-9988-7766, 잔액 45,000원 → 근거계좌 110-4455-6677로 전액 이체)"
req POST /api/piggybank/110-9988-7766/empty

step "비운 후 저금통 상태 확인 (잔액 0원)"
req GET /api/piggybank/110-9988-7766

step "근거계좌 잔액 확인 (2,000,000 → 2,045,000원이어야 함)"
req GET /api/accounts/110-4455-6677

# ── 1.4 해지: 지급제한으로 거부되는 케이스 ──────────────────────────────────

step "지급제한 등록 (110-9988-7766에 질권 설정, 데모용 관리 API)"
req POST "/api/admin/accounts/110-9988-7766/restrictions?rstTypeCd=PLEDGE"

step "1.4-실패 해지 시도 → 지급제한으로 거부 (409 PAYMENT_RESTRICTED)"
req POST /api/piggybank/110-9988-7766/close

# ── 1.4 해지: 성공 케이스 (앞서 만든 새 저금통을 정리) ─────────────────────

step "1.4-성공 새 저금통 해지 ($NEW_PIG, 잔액 150원 → 근거계좌로 반환, 서비스 OFF, 계좌 해지)"
req POST "/api/piggybank/$NEW_PIG/close"

step "해지 후 근거계좌 잔액 확인 (1,325,000 → 1,325,150원으로 원복되어야 함)"
req GET /api/accounts/110-2233-4455

step "근거계좌 자체는 살아있어 같은 계좌로 재가입 가능 (해지 확인용)"
req GET "/api/piggybank/$NEW_PIG"

echo
echo "=========================================================================="
echo " 데모 종료.
echo "=========================================================================="
