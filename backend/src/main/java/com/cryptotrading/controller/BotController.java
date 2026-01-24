package com.cryptotrading.controller;

import com.cryptotrading.dto.indicator.IndicatorResultDTO;
import com.cryptotrading.service.RiskManagementService;
import com.cryptotrading.service.TechnicalIndicatorService;
import com.cryptotrading.service.TradingBotService;
import com.cryptotrading.service.TradingBotService.BotExecutionResult;
import com.cryptotrading.scheduler.TradingScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBotStatus(Authentication authentication) {
        String userId = authentication.getName();
        
        Map<String, Object> status = new HashMap<>();
        status.put("userId", userId);
        status.put("timestamp", System.currentTimeMillis());
        status.put("botEnabled", true);
        
        // ★★★ [추가] 다음 실행 시간 계산 (5분 간격 스케줄러 기준) ★★★
        LocalDateTime now = LocalDateTime.now();
    
        // 5분 단위로 다음 실행 시간 계산
        int currentMinute = now.getMinute();
        int nextMinute = ((currentMinute / 5) + 1) * 5;
        
        LocalDateTime nextExecution;
        if (nextMinute >= 60) {
            nextExecution = now.plusHours(1).withMinute(0).withSecond(0).withNano(0);
        } else {
            nextExecution = now.withMinute(nextMinute).withSecond(0).withNano(0);
        }
    
        // 마지막 실행 시간 (가장 최근 5분 단위)
        int lastMinute = (currentMinute / 5) * 5;
        LocalDateTime lastExecution = now.withMinute(lastMinute).withSecond(0).withNano(0);
        if (lastMinute == currentMinute && now.getSecond() == 0) {
            // 정각에 호출된 경우, 이전 주기가 마지막 실행
            lastExecution = lastExecution.minusMinutes(5);
        }
    
        // 업비트 점검시간 (09:00~09:10) 체크
        boolean isMaintenanceTime = now.getHour() == 9 && now.getMinute() < 10;
    
        // 실행 중 여부 (점검시간이 아니면 실행 중으로 간주)
        boolean isRunning = !isMaintenanceTime;
    
        // 포맷팅
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
        boolean isBotActive = TradingScheduler.isBotEnabled() && !isMaintenanceTime;
        status.put("isRunning", isBotActive);
        status.put("botEnabled", TradingScheduler.isBotEnabled()); 

        status.put("lastExecutedAt", lastExecution.format(fullFormatter));
        status.put("nextExecutionAt", nextExecution.format(fullFormatter));
        status.put("nextExecutionTime", nextExecution.format(formatter));  // 시:분만
        status.put("lastExecutionTime", lastExecution.format(formatter));  // 시:분만
        status.put("isMaintenanceTime", isMaintenanceTime);
        status.put("apiConnected", true);
        status.put("emergencyStop", false);
        
        // ★★★ [추가] 다음 실행까지 남은 초 계산 ★★★
        long secondsUntilNext = java.time.Duration.between(now, nextExecution).getSeconds();
        status.put("secondsUntilNextExecution", secondsUntilNext);
        
        return ResponseEntity.ok(status);
    }

    /**
     * ⭐ 자동매매 봇 시작
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startBot() {
        log.info("🟢 자동매매 봇 시작 요청");
        TradingScheduler.setBotEnabled(true);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "자동매매 봇이 시작되었습니다.");
        response.put("botEnabled", true);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * ⭐ 자동매매 봇 중지
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopBot() {
        log.info("🔴 자동매매 봇 중지 요청");
        TradingScheduler.setBotEnabled(false);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "자동매매 봇이 중지되었습니다.");
        response.put("botEnabled", false);
        
        return ResponseEntity.ok(response);
    }
}