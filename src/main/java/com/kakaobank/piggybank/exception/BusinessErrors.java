package com.kakaobank.piggybank.exception;

import org.springframework.http.HttpStatus;

/**
 * 플로우차트 코멘트.txt에 정리된 검증 단계별 예외를 한 곳에서 관리한다.
 * reasonCode는 테스트/클라이언트가 분기하기 위한 안정적인 문자열이다.
 */
public final class BusinessErrors {

    private BusinessErrors() {
    }

    // ---------------------- 1.1 신규가입 ----------------------

    public static PiggyBankBusinessException customerNotFound(String cusNo) {
        return new PiggyBankBusinessException("CUSTOMER_NOT_FOUND", HttpStatus.NOT_FOUND,
                "고객을 찾을 수 없습니다: " + cusNo, null);
    }

    public static PiggyBankBusinessException ageNotEligible(int minAge) {
        return new PiggyBankBusinessException("AGE_NOT_ELIGIBLE", HttpStatus.BAD_REQUEST,
                "만 " + minAge + "세 이상만 가입할 수 있습니다.", "저금통 특약 제3조: 가입대상은 만 14세 이상의 실명의 개인");
    }

    public static PiggyBankBusinessException duplicateSignup() {
        return new PiggyBankBusinessException("DUPLICATE_SIGNUP", HttpStatus.CONFLICT,
                "이미 활성 저금통을 보유하고 있어 추가로 가입할 수 없습니다.", "저금통 특약 제3조: 1인당 1계좌만 가입이 가능");
    }

    public static PiggyBankBusinessException accountNotFound(String acno) {
        return new PiggyBankBusinessException("ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND,
                "계좌를 찾을 수 없습니다: " + acno, null);
    }

    public static PiggyBankBusinessException notDdaAccount() {
        return new PiggyBankBusinessException("NOT_DDA_ACCOUNT", HttpStatus.BAD_REQUEST,
                "저금통은 입출금이 자유로운예금(DDA) 계좌에만 연결할 수 있습니다.",
                "저금통 특약 제4조①: 실명확인된 입출금이 자유로운예금(근거계좌)을 통해서만 연결 신규가 가능");
    }

    public static PiggyBankBusinessException groupAccountRestricted() {
        return new PiggyBankBusinessException("GROUP_ACCOUNT_RESTRICTED", HttpStatus.BAD_REQUEST,
                "모임통장서비스를 이용 중인 계좌는 근거계좌로 사용할 수 없습니다.",
                "저금통 특약 제4조②: 근거계좌가 모임통장서비스를 이용 중인 경우 신규가 제한");
    }

    public static PiggyBankBusinessException baseAccountNotActive() {
        return new PiggyBankBusinessException("BASE_ACCOUNT_NOT_ACTIVE", HttpStatus.BAD_REQUEST,
                "근거계좌가 해지·휴면 상태이거나 지급제한이 걸려 있어 신규가입을 진행할 수 없습니다.",
                "제4조①의 전제조건: 근거계좌의 지급제한·휴면·해지 여부는 사전 검증되어 활성 상태여야 함");
    }

    // ---------------------- 1.2 / 1.4 공통 ----------------------

    public static PiggyBankBusinessException piggyBankNotFound(String pigAcno) {
        return new PiggyBankBusinessException("PIGGYBANK_NOT_FOUND", HttpStatus.NOT_FOUND,
                "저금통을 찾을 수 없습니다: " + pigAcno, null);
    }

    public static PiggyBankBusinessException paymentRestricted() {
        return new PiggyBankBusinessException("PAYMENT_RESTRICTED", HttpStatus.CONFLICT,
                "지급제한(질권/압류 등)이 걸려 있어 처리할 수 없습니다.",
                "상품설명서: 질권, 압류 등 출금제한 사고신고가 등록된 경우에는 원금 또는 이자 지급이 제한됨");
    }

    public static PiggyBankBusinessException alreadyClosed() {
        return new PiggyBankBusinessException("ALREADY_CLOSED", HttpStatus.CONFLICT,
                "이미 해지된 저금통입니다.", null);
    }

    // ---------------------- 1.3 자동저축 (배치 내부에서 SKIP/FAIL로 기록되며,
    //                          HTTP 예외로 표출되는 것은 배치 자체 조회 실패 케이스뿐) ----------------------

    public static PiggyBankBusinessException snapshotNotLoaded(String date) {
        return new PiggyBankBusinessException("SNAPSHOT_NOT_LOADED", HttpStatus.CONFLICT,
                date + " 자의 잔액 스냅샷이 적재되지 않았습니다. /api/batch/autosave/snapshot 을 먼저 호출하세요.", null);
    }

    // ---------------------- 공통 ----------------------

    public static PiggyBankBusinessException concurrentModification(String detail) {
        return new PiggyBankBusinessException("CONCURRENT_MODIFICATION", HttpStatus.CONFLICT, detail, null);
    }
}
