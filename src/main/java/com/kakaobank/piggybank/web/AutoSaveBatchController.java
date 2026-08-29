package com.kakaobank.piggybank.web;

import com.kakaobank.piggybank.dto.response.AutoSaveBatchResult;
import com.kakaobank.piggybank.dto.response.SnapshotLoadResult;
import com.kakaobank.piggybank.service.AutoSaveBatchService;
import com.kakaobank.piggybank.service.DailySnapshotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 1.3 자동저축(동전모으기) 배치 트리거.
 * 실제 운영에서는 스케줄러가 매일 호출하지만, 로컬 시연에서는 두 단계를 직접 호출한다.
 *   1) POST /api/batch/autosave/snapshot?date=2026-08-27   (전일 마감 스냅샷 적재)
 *   2) POST /api/batch/autosave/run?date=2026-08-28        (실제 자동저축 실행, 전일 스냅샷을 사용)
 */
@RestController
@RequestMapping("/api/batch/autosave")
public class AutoSaveBatchController {

    private final DailySnapshotService dailySnapshotService;
    private final AutoSaveBatchService autoSaveBatchService;
    private final Clock clock;

    public AutoSaveBatchController(DailySnapshotService dailySnapshotService,
                                    AutoSaveBatchService autoSaveBatchService,
                                    Clock clock) {
        this.dailySnapshotService = dailySnapshotService;
        this.autoSaveBatchService = autoSaveBatchService;
        this.clock = clock;
    }

    @PostMapping("/snapshot")
    public SnapshotLoadResult loadSnapshot(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate snapDate = date != null ? date : LocalDate.now(clock).minusDays(1);
        return dailySnapshotService.loadSnapshot(snapDate);
    }

    @PostMapping("/run")
    public AutoSaveBatchResult run(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate runDate = date != null ? date : LocalDate.now(clock);
        return autoSaveBatchService.runBatch(runDate);
    }
}
