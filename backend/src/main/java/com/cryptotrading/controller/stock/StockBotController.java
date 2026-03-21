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
}