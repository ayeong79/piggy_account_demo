package com.kakaobank.piggybank.service;

import com.kakaobank.piggybank.domain.Account;
import com.kakaobank.piggybank.domain.AccountTransaction;
import com.kakaobank.piggybank.repository.AccountTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 저금통 특약 제6조①②(근거계좌로의 이체를 통해서만, 전액만 출금 가능)에 따라
 * 1.2 비우기 / 1.3 자동저축 / 1.4 해지가 공통으로 재사용하는 "이중기장 이체" 로직.
 *
 * from 계좌에서 출금 1행, to 계좌에서 입금 1행을 ACT_TRX에 남기고, 두 행을 같은
 * TRX_GRP_ID로 묶는다. 두 계좌의 BAL_AMT/LAST_TRX_DT도 같은 트랜잭션에서 갱신한다
 * (플로우차트 코멘트.txt 1.2의 4~5단계).
 */
@Service
public class TransferService {

    private final AccountTransactionRepository accountTransactionRepository;

    public TransferService(AccountTransactionRepository accountTransactionRepository) {
        this.accountTransactionRepository = accountTransactionRepository;
    }

    public record TransferResult(Long withdrawTrxSeqId, Long depositTrxSeqId) {
    }

    public TransferResult transfer(Account from, Account to, BigDecimal amount, String trxTypeCd, LocalDate trxDate) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("이체 금액은 0보다 커야 합니다: " + amount);
        }

        BigDecimal fromBefore = from.getBalAmt();
        BigDecimal fromAfter = fromBefore.subtract(amount);
        AccountTransaction withdrawal = new AccountTransaction(
                from.getAcno(), trxDate, trxTypeCd, amount.negate(), fromBefore, fromAfter, to.getAcno());
        accountTransactionRepository.save(withdrawal);
        accountTransactionRepository.flush(); // TRX_SEQ_ID 채번 확보
        withdrawal.assignGroupId(withdrawal.getTrxSeqId());

        BigDecimal toBefore = to.getBalAmt();
        BigDecimal toAfter = toBefore.add(amount);
        AccountTransaction deposit = new AccountTransaction(
                to.getAcno(), trxDate, trxTypeCd, amount, toBefore, toAfter, from.getAcno());
        deposit.assignGroupId(withdrawal.getTrxSeqId());
        accountTransactionRepository.save(deposit);
        accountTransactionRepository.flush();

        from.withdraw(amount, trxDate);
        to.deposit(amount, trxDate);

        return new TransferResult(withdrawal.getTrxSeqId(), deposit.getTrxSeqId());
    }

    /** 이자 지급처럼 상대계좌 없이 자기 계좌에만 입금하는 단건 기장 (INTEREST). */
    public Long creditSelf(Account account, BigDecimal amount, String trxTypeCd, LocalDate trxDate) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("입금 금액은 0보다 커야 합니다: " + amount);
        }
        BigDecimal before = account.getBalAmt();
        BigDecimal after = before.add(amount);
        AccountTransaction trx = new AccountTransaction(account.getAcno(), trxDate, trxTypeCd, amount, before, after, null);
        accountTransactionRepository.save(trx);
        accountTransactionRepository.flush();
        account.deposit(amount, trxDate);
        return trx.getTrxSeqId();
    }
}
