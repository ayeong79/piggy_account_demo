package com.kakaobank.piggybank.dto.response;

import com.kakaobank.piggybank.domain.Account;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountView(
        String acno,
        String accd,
        String cusNo,
        BigDecimal balAmt,
        boolean active,
        boolean groupAccount,
        boolean dormant,
        boolean interestSuspended,
        LocalDate opnDt,
        LocalDate lastTrxDt
) {
    public static AccountView from(Account a) {
        return new AccountView(a.getAcno(), a.accountTypeCode(), a.getCusNo(), a.getBalAmt(),
                a.isActive(), a.isGroupAccount(), a.isDormant(), a.isInterestSuspended(),
                a.getOpnDt(), a.getLastTrxDt());
    }
}
