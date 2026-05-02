package com.cryptotrading.controller.stock;

import com.cryptotrading.service.StockRiskManagementService;
import com.cryptotrading.service.StockTradingBotService;
import com.cryptotrading.service.StockTradingBotService.BotExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 주식 봇 컨트롤러
 * Phase 1 BotController의 구조를 재사용하여 주식 봇 전용 엔드포인트 제공
 *
 * [엔드포인트 목록]
 * POST /api/stock/bot/execute   - 수동 매매 실행
 * GET  /api/stock/bot/status    - 봇 상태 조회
 * POST /api/stock/bot/start     - 봇 활성화
 * POST /api/stock/bot/stop      - 봇 비활성화
 */
@RestController
@RequestMapping("/api/stock/bot")
@RequiredArgsConstructor
@Slf4j
public class StockBotController {

    private final StockTradingBotService stockTradingBotService;
    private final StockRiskManagementService stockRiskManagementService;
    private final com.cryptotrading.repository.StockTradingSettingRepository stockTradingSettingRepository; // ⭐ [Day 57 추가]
    private final com.cryptotrading.service.StockTechnicalIndicatorService stockTechnicalIndicatorService; // ⭐ [Day 61 추가]

    /**
     * 수동으로 주식 자동매매 1회 실행
     * Phase 1: POST /api/bot/execute 와 동일한 역할
     */
    @PostMapping("/execute")
    public ResponseEntity<BotExecutionResult> executeBot(Authentication authentication) {
        String userId = authentication.getName();
        log.info("[주식봇 컨트롤러] 수동 자동매매 실행 요청: {}", userId);
        BotExecutionResult result = stockTradingBotService.executeForUser(userId);
        return ResponseEntity.ok(result);
    }

    /**
     * 주식 봇 상태 조회
     * Phase 1 /api/bot/status 와 동일한 구조
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBotStatus(Authentication authentication) {
        String userId = authentication.getName();
        Map<String, Object> status = new HashMap<>();

        boolean botEnabled = stockRiskManagementService.isStockBotEnabled(userId);
        boolean marketOpen = stockRiskManagementService.isMarketOpen();
        boolean emergencyStop = stockRiskManagementService.isEmergencyStop(userId);

        status.put("botEnabled", botEnabled);
        status.put("isRunning", botEnabled && marketOpen && !emergencyStop);
        status.put("marketOpen", marketOpen);
        status.put("emergencyStop", emergencyStop);
        status.put("tomorrowHoliday", stockRiskManagementService.isTomorrowHoliday());

        // ⭐ [Day 59 추가] 코인 대시보드와 동일: 마지막/다음 실행시간 + 카운트다운
        // 주식 봇은 3분(180초) 주기 실행 (cron: "0 */3 * * * *")
        java.time.LocalDateTime now = java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        // 다음 3분 경계 계산
        int currentMinute = now.getMinute();
        int nextMinuteBase = (currentMinute / 3 + 1) * 3;
        java.time.LocalDateTime nextExecution = now
                .withMinute(nextMinuteBase % 60)
                .withSecond(0)
                .withNano(0);
        if (nextMinuteBase >= 60) {
            nextExecution = nextExecution.plusHours(1);
        }
        long secondsUntilNext = java.time.Duration.between(now, nextExecution).getSeconds();

        status.put("lastExecutionTime", now.minusMinutes(currentMinute % 3)
                .withSecond(0).withNano(0).toString());
        status.put("nextExecutionTime", nextExecution.toString());
        status.put("secondsUntilNextExecution", secondsUntilNext);

        return ResponseEntity.ok(status);
    }

    /**
     * 주식 봇 활성화
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startBot(Authentication authentication) {
        String userId = authentication.getName();
        log.info("🟢 [주식봇 컨트롤러] 봇 시작 요청: {}", userId);
        stockRiskManagementService.setStockBotEnabled(userId, true);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "주식 자동매매 봇이 시작되었습니다.");
        response.put("botEnabled", true);
        return ResponseEntity.ok(response);
    }

    /**
     * 주식 봇 비활성화
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopBot(Authentication authentication) {
        String userId = authentication.getName();
        log.info("🔴 [주식봇 컨트롤러] 봇 중지 요청: {}", userId);
        stockRiskManagementService.setStockBotEnabled(userId, false);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "주식 자동매매 봇이 중지되었습니다.");
        response.put("botEnabled", false);
        return ResponseEntity.ok(response);
    }

    // ⭐ [Day 57 추가] 테스트/운영 수동 트리거용 --------------------------

    /**
     * ⭐ [Day 57 추가] 일일 거래 캐시 수동 초기화
     * - 스케줄러(15:35 KST)와 동일한 clearStockDailyCache() 호출
     * - 테스트 환경에서 수동 트리거 및 긴급 초기화 용도
     */
    @PostMapping("/reset-daily-cache")
    public ResponseEntity<Map<String, Object>> resetDailyCache(Authentication authentication) {
        String userId = authentication.getName();
        log.info("[주식봇 컨트롤러] 일일 캐시 수동 초기화 요청: {}", userId);
        stockRiskManagementService.clearStockDailyCache();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "일일 거래 캐시가 초기화되었습니다.");
        return ResponseEntity.ok(response);
    }

    /**
     * ⭐ [Day 57 추가] 보유기간 경고 대상 조회
     * - 스케줄러(09:05 KST)의 경고 체크와 동일한 로직 수동 조회
     * - 경고(15일 이상): urgent=false / 긴급(20일 이상): urgent=true
     */
    @GetMapping("/holding-warnings")
    public ResponseEntity<?> getHoldingWarnings(Authentication authentication) {
        String userId = authentication.getName();
        log.info("[주식봇 컨트롤러] 보유기간 경고 조회 요청: {}", userId);

        java.util.Optional<com.cryptotrading.entity.StockTradingSetting> settingOpt =
                stockTradingSettingRepository.findByUserId(userId);

        if (settingOpt.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "주식 거래 설정이 없습니다.");
            return ResponseEntity.badRequest().body(response);
        }

        java.util.List<StockRiskManagementService.HoldingDaysWarning> warnings =
                stockRiskManagementService.getHoldingDaysWarnings(userId, settingOpt.get());

        return ResponseEntity.ok(warnings);
    }

    // ⭐ [Day 61 추가] 기술적 지표 조회 엔드포인트 ----------------------

    /**
     * ⭐ [Day 61 추가] 사용자 거래 설정의 모든 종목 기술적 지표 일괄 조회
     * - Phase 1: GET /api/bot/indicators?markets=KRW-BTC,KRW-ETH 와 유사
     * - 주식은 KIS API 키 인증이 사용자별로 필요하므로 stockCodes를 거래 설정에서 자동 추출
     * - StockBotMonitorView 의 기술적 지표 테이블에서 사용
     */
    @GetMapping("/indicators")
    public ResponseEntity<?> getAllIndicators(Authentication authentication) {
        String userId = authentication.getName();
        log.info("[주식봇 컨트롤러] 전 종목 기술적 지표 조회: {}", userId);

        java.util.Optional<com.cryptotrading.entity.StockTradingSetting> settingOpt =
                stockTradingSettingRepository.findByUserId(userId);

        if (settingOpt.isEmpty()) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }

        java.util.List<String> stockCodes = parseStockCodes(settingOpt.get().getStockCodes());
        if (stockCodes.isEmpty()) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }

        java.util.List<com.cryptotrading.dto.indicator.IndicatorResultDTO> results =
                new java.util.ArrayList<>();
        for (String stockCode : stockCodes) {
            try {
                com.cryptotrading.dto.indicator.IndicatorResultDTO indicator =
                        stockTechnicalIndicatorService.calculateIndicators(userId, stockCode);
                if (indicator != null) {
                    results.add(indicator);
                }
            } catch (Exception e) {
                log.warn("[주식봇 컨트롤러] 지표 조회 실패: {} - {}", stockCode, e.getMessage());
            }
        }

        return ResponseEntity.ok(results);
    }

    /**
     * ⭐ [Day 61 추가] 단일 종목 기술적 지표 조회
     * - Phase 1: GET /api/bot/indicators/{market} 와 동일 패턴
     */
    @GetMapping("/indicators/{stockCode}")
    public ResponseEntity<?> getIndicator(@PathVariable String stockCode,
                                          Authentication authentication) {
        String userId = authentication.getName();
        log.info("[주식봇 컨트롤러] 단일 종목 기술적 지표 조회: {} - {}", userId, stockCode);

        try {
            com.cryptotrading.dto.indicator.IndicatorResultDTO indicator =
                    stockTechnicalIndicatorService.calculateIndicators(userId, stockCode);
            if (indicator == null) {
                java.util.Map<String, String> error = new java.util.HashMap<>();
                error.put("error", "지표 데이터를 계산할 수 없습니다: " + stockCode);
                return ResponseEntity.badRequest().body(error);
            }
            return ResponseEntity.ok(indicator);
        } catch (Exception e) {
            log.error("[주식봇 컨트롤러] 지표 조회 실패: {} - {}", stockCode, e.getMessage());
            java.util.Map<String, String> error = new java.util.HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * ⭐ [Day 61 추가] JSON 형식의 stockCodes 파싱
     * - StockTradingBotService.parseStockCodes() 와 동일 로직
     */
    private java.util.List<String> parseStockCodes(String stockCodesJson) {
        if (stockCodesJson == null || stockCodesJson.isBlank()) {
            return new java.util.ArrayList<>();
        }
        try {
            String cleaned = stockCodesJson.replaceAll("[\\[\\]\"\\s]", "");
            if (cleaned.isEmpty()) return new java.util.ArrayList<>();
            java.util.List<String> result = new java.util.ArrayList<>();
            for (String part : cleaned.split(",")) {
                if (!part.isBlank()) result.add(part.trim());
            }
            return result;
        } catch (Exception e) {
            log.warn("[주식봇 컨트롤러] 종목 코드 파싱 실패: {} - {}", stockCodesJson, e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
}