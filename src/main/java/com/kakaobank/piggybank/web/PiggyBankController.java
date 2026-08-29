package com.kakaobank.piggybank.web;

import com.kakaobank.piggybank.domain.PiggyBank;
import com.kakaobank.piggybank.dto.response.CloseResponse;
import com.kakaobank.piggybank.dto.response.EmptyResponse;
import com.kakaobank.piggybank.dto.response.PiggyBankView;
import com.kakaobank.piggybank.exception.BusinessErrors;
import com.kakaobank.piggybank.repository.PiggyBankAutoSaveHistoryRepository;
import com.kakaobank.piggybank.repository.PiggyBankRepository;
import com.kakaobank.piggybank.service.CloseService;
import com.kakaobank.piggybank.service.EmptyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 1.2 저금통비우기 / 1.4 해지 + 저금통 상태 조회. */
@RestController
@RequestMapping("/api/piggybank")
public class PiggyBankController {

    private final EmptyService emptyService;
    private final CloseService closeService;
    private final PiggyBankRepository piggyBankRepository;
    private final PiggyBankAutoSaveHistoryRepository historyRepository;

    public PiggyBankController(EmptyService emptyService, CloseService closeService,
                                PiggyBankRepository piggyBankRepository,
                                PiggyBankAutoSaveHistoryRepository historyRepository) {
        this.emptyService = emptyService;
        this.closeService = closeService;
        this.piggyBankRepository = piggyBankRepository;
        this.historyRepository = historyRepository;
    }

    @GetMapping("/{pigAcno}")
    public PiggyBankView get(@PathVariable String pigAcno) {
        PiggyBank piggyBank = piggyBankRepository.findById(pigAcno)
                .orElseThrow(() -> BusinessErrors.piggyBankNotFound(pigAcno));
        return PiggyBankView.from(piggyBank);
    }

    @PostMapping("/{pigAcno}/empty")
    public EmptyResponse empty(@PathVariable String pigAcno) {
        return emptyService.empty(pigAcno);
    }

    @PostMapping("/{pigAcno}/close")
    public CloseResponse close(@PathVariable String pigAcno) {
        return closeService.close(pigAcno);
    }

    @GetMapping("/{pigAcno}/autosave-history")
    public List<HistoryView> autosaveHistory(@PathVariable String pigAcno) {
        return historyRepository.findByPigAcnoOrderBySeqIdDesc(pigAcno).stream()
                .map(h -> new HistoryView(h.getExcDate(), h.getExcStCd(),
                        h.getSkipRsnCd() != null ? h.getSkipRsnCd() : h.getFailRsnCd(),
                        h.getCalcAmt(), h.getExcAmt()))
                .toList();
    }

    public record HistoryView(java.time.LocalDate excDate, String status, String reasonCode,
                               java.math.BigDecimal calcAmt, java.math.BigDecimal excAmt) {
    }
}
