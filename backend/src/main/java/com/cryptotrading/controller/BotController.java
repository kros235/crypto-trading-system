package com.cryptotrading.controller;

import com.cryptotrading.dto.indicator.IndicatorResultDTO;
import com.cryptotrading.service.RiskManagementService;
import com.cryptotrading.service.TechnicalIndicatorService;
import com.cryptotrading.service.TradingBotService;
import com.cryptotrading.service.TradingBotService.BotExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bot")
@RequiredArgsConstructor
@Slf4j
public class BotController {

    private final TradingBotService tradingBotService;
    private final TechnicalIndicatorService indicatorService;
    private final RiskManagementService riskManagementService;

    /**
     * 수동으로 자동매매 실행 (현재 사용자)
     */
    @PostMapping("/execute")
    public ResponseEntity<BotExecutionResult> executeBot(Authentication authentication) {
        String userId = authentication.getName();
        log.info("수동 자동매매 실행 요청: {}", userId);
        
        BotExecutionResult result = tradingBotService.executeForUser(userId);
        return ResponseEntity.ok(result);
    }

    /**
     * 특정 코인의 기술적 지표 조회
     */
    @GetMapping("/indicators/{market}")
    public ResponseEntity<?> getIndicators(@PathVariable String market) {
        log.info("기술적 지표 조회: {}", market);
        
        try {
            IndicatorResultDTO indicators = indicatorService.calculateIndicators(market);
            if (indicators == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "지표 데이터를 계산할 수 없습니다: " + market);
                return ResponseEntity.badRequest().body(error);
            }
            return ResponseEntity.ok(indicators);
        } catch (Exception e) {
            log.error("지표 조회 실패: {} - {}", market, e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 여러 코인의 기술적 지표 조회
     */
    @GetMapping("/indicators")
    public ResponseEntity<List<IndicatorResultDTO>> getMultipleIndicators(
            @RequestParam List<String> markets) {
        log.info("다중 기술적 지표 조회: {}", markets);
        
        List<IndicatorResultDTO> results = markets.stream()
                .map(indicatorService::calculateIndicators)
                .filter(indicator -> indicator != null)
                .toList();
        
        return ResponseEntity.ok(results);
    }

    /**
     * 봇 상태 조회 (리스크 정보 포함)
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBotStatus(Authentication authentication) {
        String userId = authentication.getName();
        
        Map<String, Object> status = new HashMap<>();
        status.put("userId", userId);
        status.put("timestamp", System.currentTimeMillis());
        status.put("botEnabled", true);  // 추후 사용자별 on/off 설정 추가 가능
        
        // 추후 구현: 현재 보유 현황, 오늘 거래 내역 등
        
        return ResponseEntity.ok(status);
    }
}