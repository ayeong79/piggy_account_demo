package com.kakaobank.piggybank.dto.response;

import com.kakaobank.piggybank.domain.PiggyBank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PiggyBankView(
        String pigAcno,
        String rtAcno,
        String cusNo,
        BigDecimal balAmt,
        boolean active,
        LocalDate enrDt,
        LocalDate cnclDt
) {
    public static PiggyBankView from(PiggyBank p) {
        return new PiggyBankView(p.getPigAcno(), p.getRtAcno(), p.getCusNo(), p.getBalAmt(),
                p.isActive(), p.getEnrDt(), p.getCnclDt());
    }
}
