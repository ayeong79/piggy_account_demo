package com.kakaobank.piggybank.dto.response;

import java.time.LocalDate;

public record SnapshotLoadResult(
        LocalDate snapDate,
        int loadedCount
) {
}
