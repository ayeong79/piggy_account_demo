import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

/**
 * Maven Central 접근이 막힌 이 샌드박스에서는 실제 Spring Boot 프로젝트를
 * mvn으로 컴파일/실행할 수 없어서, 핵심 계산 로직만 의존성 없는 순수 자바로 옮겨
 * javac/java로 직접 컴파일·실행해 검증한 스크립트입니다.
 *
 * 아래 각 메서드는 실제 서비스 코드(Customer.isEligibleAge, AutoSaveItemProcessor의
 * 3~5단계 계산, InterestService.settleUpTo의 이자 계산식)와 동일한 로직을 그대로
 * 옮긴 것이며, 각 케이스의 기대값은 JUnit 테스트(SignupServiceTest, EmptyServiceTest,
 * AutoSaveBatchServiceTest)에 쓴 것과 동일합니다.
 *
 * 실행: javac BusinessLogicSanityCheck.java && java BusinessLogicSanityCheck
 */
public class BusinessLogicSanityCheck {

    static int checks = 0;
    static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== 1.1 연령 검증 (Customer.isEligibleAge) ===");
        checkAge();

        System.out.println();
        System.out.println("=== 1.3 동전모으기 계산 (AutoSaveItemProcessor 3~5단계) ===");
        checkAutoSaveCalculation();

        System.out.println();
        System.out.println("=== 1.2/1.3/1.4 공통 이자 계산 (InterestService.settleUpTo) ===");
        checkInterestCalculation();

        System.out.println();
        System.out.println("=== 결과: " + checks + "개 검증, 실패 " + failures + "개 ===");
        if (failures > 0) {
            System.exit(1);
        }
    }

    // ---- 1.1 신규가입 1단계: 만 나이 계산 (Customer.isEligibleAge와 동일 로직) ----
    static boolean isEligibleAge(LocalDate birthDate, LocalDate asOfDate, int minAge) {
        return Period.between(birthDate, asOfDate).getYears() >= minAge;
    }

    static void checkAge() {
        LocalDate today = LocalDate.of(2026, 8, 28);
        expect("성인(1998-05-12)은 만14세 이상", isEligibleAge(LocalDate.of(1998, 5, 12), today, 14), true);
        expect("2015-01-01생은 만14세 미만(약 11세)", isEligibleAge(LocalDate.of(2015, 1, 1), today, 14), false);
        // 경계값: 생일 하루 전 = 아직 그 나이가 안 됨
        expect("2012-08-29생은 2026-08-28 기준 만13세(하루 부족)", isEligibleAge(LocalDate.of(2012, 8, 29), today, 14), false);
        // 경계값: 생일 당일 = 그 나이가 됨
        expect("2012-08-28생은 2026-08-28 기준 정확히 만14세", isEligibleAge(LocalDate.of(2012, 8, 28), today, 14), true);
    }

    // ---- 1.3 자동저축 3~5단계: CALC_AMT / EXC_AMT 계산 (AutoSaveItemProcessor와 동일 로직) ----
    static BigDecimal calcAmt(BigDecimal prevBalance) {
        return prevBalance.remainder(BigDecimal.valueOf(1000));
    }

    static BigDecimal excAmt(BigDecimal calcAmt, BigDecimal currentPigBalance) {
        BigDecimal room = BigDecimal.valueOf(100_000).subtract(currentPigBalance);
        return calcAmt.min(room);
    }

    static void checkAutoSaveCalculation() {
        // 데이터흐름.pdf 샘플과 동일: 전일잔액 1,325,150원 -> 끝전 150원
        BigDecimal calc1 = calcAmt(BigDecimal.valueOf(1_325_150));
        expect("1,325,150원의 끝전(CALC_AMT)은 150원", calc1, BigDecimal.valueOf(150));
        expect("저금통 잔액 44,850원일 때 EXC_AMT는 한도 내라 150원 그대로",
                excAmt(calc1, BigDecimal.valueOf(44_850)), BigDecimal.valueOf(150));

        // 한도 초과 케이스: 저금통이 이미 100,000원 -> room=0 -> EXC_AMT=0 (SKIP LIMIT_EXCD)
        BigDecimal calc2 = calcAmt(BigDecimal.valueOf(123_456));
        expect("123,456원의 끝전은 456원", calc2, BigDecimal.valueOf(456));
        expect("저금통이 이미 10만원이면 EXC_AMT는 0 (한도초과 SKIP)",
                excAmt(calc2, BigDecimal.valueOf(100_000)), BigDecimal.ZERO);

        // 끝전 0원 케이스 (SKIP NO_CHANGE)
        expect("500,000원처럼 1000으로 나누어떨어지면 CALC_AMT=0",
                calcAmt(BigDecimal.valueOf(500_000)), BigDecimal.ZERO);

        // 부분 적립 케이스: 끝전(700)이 남은 한도(300)보다 클 때 -> 한도만큼만 적립
        BigDecimal calc3 = calcAmt(BigDecimal.valueOf(999_700)); // 700원 끝전
        expect("999,700원의 끝전은 700원", calc3, BigDecimal.valueOf(700));
        expect("저금통 잔액 99,700원(남은 한도 300원)이면 EXC_AMT는 300원으로 절삭",
                excAmt(calc3, BigDecimal.valueOf(99_700)), BigDecimal.valueOf(300));
    }

    // ---- 1.2/1.3/1.4 공통: 미정산 이자 계산 (InterestService.settleUpTo와 동일 로직) ----
    static BigDecimal interest(BigDecimal balance, BigDecimal ratePct, LocalDate fromDt, LocalDate toDt) {
        long days = ChronoUnit.DAYS.between(fromDt, toDt) + 1;
        BigDecimal ratio = ratePct.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(days))
                .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);
        return balance.multiply(ratio).setScale(0, RoundingMode.DOWN);
    }

    static void checkInterestCalculation() {
        // EmptyServiceTest 시나리오: 개설일 2024-06-01 ~ 정산기준일 2026-08-27, 잔액 100,000원, 금리 4.00%
        BigDecimal amt = interest(BigDecimal.valueOf(100_000), BigDecimal.valueOf(4.00),
                LocalDate.of(2024, 6, 1), LocalDate.of(2026, 8, 27));
        System.out.println("  100,000원 / 연 4.00% / 2024-06-01~2026-08-27 => 이자 " + amt + "원");
        expect("장기간(2년+) 4% 단리 이자는 0원보다 커야 함", amt.compareTo(BigDecimal.ZERO) > 0, true);

        // 계산기간이 없는 경우 (fromDt > toDt) 는 서비스에서 별도 분기하므로 여기서는 일수만 확인
        long zeroDays = ChronoUnit.DAYS.between(LocalDate.of(2026, 8, 28), LocalDate.of(2026, 8, 27));
        expect("fromDt가 toDt보다 하루 늦으면 일수는 음수(-1) -> 서비스가 계산기간 없음으로 스킵",
                zeroDays < 0, true);
    }

    static void expect(String label, Object actual, Object expected) {
        checks++;
        boolean pass = (actual instanceof BigDecimal && expected instanceof BigDecimal)
                ? ((BigDecimal) actual).compareTo((BigDecimal) expected) == 0
                : actual.equals(expected);
        System.out.println((pass ? "  [OK] " : "  [FAIL] ") + label + " -> actual=" + actual + ", expected=" + expected);
        if (!pass) {
            failures++;
        }
    }
}
