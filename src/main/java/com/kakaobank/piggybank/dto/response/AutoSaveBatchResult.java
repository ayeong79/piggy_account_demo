package com.kakaobank.piggybank.dto.response;

import java.time.LocalDate;
import java.util.List;

public record AutoSaveBatchResult(
        LocalDate runDate,
        int totalCount,
        int successCount,
        int skipCount,
        int failCount,
        List<AutoSaveItemResult> items
) {
}
