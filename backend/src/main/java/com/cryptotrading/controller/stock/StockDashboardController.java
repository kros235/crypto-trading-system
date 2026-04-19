package com.cryptotrading.controller.stock;

import com.cryptotrading.dto.stock.StockDashboardStatsDTO;
import com.cryptotrading.entity.StockTransaction;
import com.cryptotrading.entity.StockTradingSetting;
import com.cryptotrading.entity.TransactionStatus;
import com.cryptotrading.repository.StockDailySummaryRepository;
import com.cryptotrading.repository.StockTransactionRepository;
import com.cryptotrading.repository.StockTradingSettingRepository;
import com.cryptotrading.service.KisApiService;      // ⭐ [추가]
import com.cryptotrading.entity.StockAssetSnapshot;
import com.cryptotrading.service.StockAssetSnapshotService;
import org.springframework.security.core.Authentication;
import com.cryptotrading.service.StockRiskManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;  // ⭐ WebClient → RestTemplate

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 주식 대시보드 컨트롤러
 * Day 59: Phase 1 DashboardStatsDTO 패턴 재사용
 *
 * [엔드포인트]
 * GET /api/stock/dashboard/stats   - 대시보드 통계 조회
 * GET /api/stock/dashboard/exchange-rate - 환율 정보 (USD/KRW)
 */
@RestController
@RequestMapping("/api/stock/dashboard")
@RequiredArgsConstructor
@Slf4j
public class StockDashboardController {

    private final StockTransactionRepository stockTransactionRepository;
    private final StockTradingSettingRepository stockTradingSettingRepository;
    private final StockDailySummaryRepository stockDailySummaryRepository;
    private final StockRiskManagementService stockRiskManagementService;
     private final KisApiService kisApiService;
    private final StockAssetSnapshotService stockAssetSnapshotService;          // ⭐ [추가]

    /**
     * 주식 대시보드 통계 조회
     * Phase 1 /api/dashboard/stats 와 동일한 역할
     */
    @GetMapping("/stats")
    public ResponseEntity<StockDashboardStatsDTO> getDashboardStats(
            @AuthenticationPrincipal String userId) {

        log.debug("[주식 대시보드] 통계 조회: {}", userId);

        // 1. 보유 중인 거래 목록
        List<StockTransaction> holdings = stockTransactionRepository
                .findByUserIdAndStatus(userId, TransactionStatus.HOLDING);

        // 2. 총 투자금액 / 현재 평가액 계산
        BigDecimal totalHoldingAmount = holdings.stream()
                .map(StockTransaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 현재가는 DB의 soldPrice(보유 중엔 null) 대신 price × quantity로 근사
        // 실시간 평가액은 프론트에서 개별 보유 종목 API로 별도 처리
        BigDecimal totalCurrentValue = totalHoldingAmount; // 기본값 (프론트에서 갱신)
        BigDecimal totalProfitLoss = BigDecimal.ZERO;
        BigDecimal totalProfitLossPct = BigDecimal.ZERO;

        // 3. 오늘 실현 손익 (매도 완료 건)
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<StockTransaction> todaySold = stockTransactionRepository
                .findByUserIdAndStatusAndSoldAtAfter(userId, TransactionStatus.SOLD, todayStart);

        BigDecimal realizedProfitLoss = todaySold.stream()
                .map(t -> t.getProfitLoss() != null ? t.getProfitLoss() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. 오늘 매수 금액/건수
        List<StockTransaction> todayBought = stockTransactionRepository
                .findByUserIdAndCreatedAtAfter(userId, todayStart);
        BigDecimal todayBuyAmount = todayBought.stream()
                .map(StockTransaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. 전체 통계
        long totalBuyCount = stockTransactionRepository.countByUserId(userId);
        long totalSellCount = stockTransactionRepository
                .countByUserIdAndStatus(userId, TransactionStatus.SOLD);

        // 6. 거래 한도
        // ⭐ [수정] findByUserId → findFirstByUserIdOrderByCreatedAtDesc
        // 중복 데이터 존재 시 NonUniqueResultException 방지
        Optional<StockTradingSetting> settingOpt =
                stockTradingSettingRepository.findFirstByUserIdOrderByCreatedAtDesc(userId);
        BigDecimal dailyLimitAmount = settingOpt
                .map(StockTradingSetting::getDailyLimitAmount)
                .orElse(BigDecimal.ZERO);

        BigDecimal usedToday = todayBuyAmount;
        BigDecimal remainingDailyLimit = dailyLimitAmount.subtract(usedToday)
                .max(BigDecimal.ZERO);

        // 7. 보유기간 경고 건수
        int warningCount = 0;
        int urgentCount = 0;
        if (settingOpt.isPresent()) {
            List<StockRiskManagementService.HoldingDaysWarning> warnings =
                    stockRiskManagementService.getHoldingDaysWarnings(userId, settingOpt.get());
            warningCount = (int) warnings.stream().filter(w -> !w.isUrgent()).count();
            urgentCount = (int) warnings.stream().filter(StockRiskManagementService.HoldingDaysWarning::isUrgent).count();
        }

        // 8. 봇/장 상태
        boolean botEnabled = stockRiskManagementService.isStockBotEnabled(userId);
        boolean marketOpen = stockRiskManagementService.isMarketOpen();

        StockDashboardStatsDTO stats = StockDashboardStatsDTO.builder()
                .totalHoldingAmount(totalHoldingAmount)
                .totalCurrentValue(totalCurrentValue)
                .totalProfitLoss(totalProfitLoss)
                .totalProfitLossPct(totalProfitLossPct)
                .realizedProfitLoss(realizedProfitLoss)
                .soldCount(todaySold.size())
                .totalBuyCount(totalBuyCount)
                .totalSellCount(totalSellCount)
                .currentHoldingCount(holdings.size())
                .todayBuyAmount(todayBuyAmount)
                .todaySellAmount(todaySold.stream()
                        .map(StockTransaction::getTotalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .todayBuyCount(todayBought.size())
                .todaySellCount(todaySold.size())
                .dailyLimitAmount(dailyLimitAmount)
                .remainingDailyLimit(remainingDailyLimit)
                .holdingDaysWarningCount(warningCount)
                .holdingDaysUrgentCount(urgentCount)
                .botEnabled(botEnabled)
                .marketOpen(marketOpen)
                .build();

        return ResponseEntity.ok(stats);
    }

    /**
     * USD/KRW 환율 정보 조회
     * Frankfurter API 사용 (무료, 키 불필요)
     * GET /api/stock/dashboard/exchange-rate
     */
    // ⭐ [개선] 환율 캐시 (JVM 레벨 단순 캐시 - 5분 유지)
    // 이유: RestTemplate은 매 요청마다 커넥션 생성 → 짧은 주기 반복 호출 방지
    private volatile double cachedRate = 0;
    private volatile double prevCachedRate = 0;
    private volatile String cachedDate = "";
    private volatile long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L; // 5분


    // ⭐ [추가] KIS 계좌 실제 잔고 조회 (KRW 잔고, 주식 평가액, 총 자산)
    // Phase 1 업비트 실제 잔고 카드와 동일한 역할
    @GetMapping("/account")
    public ResponseEntity<Map<String, Object>> getKisAccount(
            @AuthenticationPrincipal String userId) {
        try {
            com.cryptotrading.dto.kis.KisAccountDTO.BalanceResponse balance =
                    kisApiService.getBalance(userId);

            if (balance == null || !balance.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "krwBalance", 0,
                        "stockEvaluation", 0,
                        "totalAsset", 0,
                        "holdings", java.util.Collections.emptyList()
                ));
            }

            // 예수금 (KRW 잔고)
            java.math.BigDecimal krwBalance = java.math.BigDecimal.ZERO;
            java.math.BigDecimal totalEvaluation = java.math.BigDecimal.ZERO;

            if (balance.getSummary() != null) {
                krwBalance = balance.getSummary().getTotalDepositDecimal();
                try {
                    totalEvaluation = new java.math.BigDecimal(
                            balance.getSummary().getTotalEvaluationAmount() != null
                            ? balance.getSummary().getTotalEvaluationAmount() : "0");
                } catch (Exception ignored) {}
            }

            // 주식 평가액 = 총 평가금액 - 예수금
            java.math.BigDecimal stockEvaluation = totalEvaluation.subtract(krwBalance)
                    .max(java.math.BigDecimal.ZERO);

            // 보유 종목 목록 (파이차트 범례용)
            java.util.List<Map<String, Object>> holdingList = new java.util.ArrayList<>();
            if (balance.getHoldings() != null) {
                for (var h : balance.getHoldings()) {
                    if (h.getHoldingQty() <= 0) continue;
                    Map<String, Object> item = new HashMap<>();
                    item.put("stockCode", h.getStockCode());
                    item.put("stockName", h.getStockName());
                    try {
                        item.put("evaluation", new java.math.BigDecimal(
                                h.getEvaluationAmount() != null ? h.getEvaluationAmount() : "0"));
                    } catch (Exception e) {
                        item.put("evaluation", 0);
                    }
                    holdingList.add(item);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("krwBalance", krwBalance);
            result.put("stockEvaluation", stockEvaluation);
            result.put("totalAsset", totalEvaluation);
            result.put("holdings", holdingList);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.warn("[주식 대시보드] KIS 계좌 잔고 조회 실패: {}", e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("success", false);
            fallback.put("krwBalance", 0);
            fallback.put("stockEvaluation", 0);
            fallback.put("totalAsset", 0);
            fallback.put("holdings", java.util.Collections.emptyList());
            return ResponseEntity.ok(fallback);
        }
    }


    /**
     * USD/KRW 환율 정보 조회
     * ⭐ [변경] Frankfurter API → open.er-api.com (완전 무료, 키 불필요, 방화벽 차단 없음)
     * 엔드포인트: GET /api/stock/dashboard/exchange-rate
     */
    @GetMapping("/exchange-rate")
    public ResponseEntity<Map<String, Object>> getExchangeRate() {
        try {
            // 캐시 유효 시 즉시 반환
            if (cachedRate > 0 && (System.currentTimeMillis() - cacheTimestamp) < CACHE_TTL_MS) {
                return buildExchangeResponse(cachedRate, prevCachedRate, cachedDate, "cache");
            }

            // ⭐ open.er-api.com: 완전 무료, 키 불필요, 1500회/월 제한
            // 대안1: https://open.er-api.com/v6/latest/USD
            // 대안2: https://api.exchangerate-api.com/v4/latest/USD (동일 제공사)
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://open.er-api.com/v6/latest/USD";

            @SuppressWarnings("unchecked")
            Map<String, Object> result = restTemplate.getForObject(url, Map.class);

            if (result == null || !"success".equals(result.get("result"))) {
                throw new RuntimeException("API 응답 실패: " + (result != null ? result.get("result") : "null"));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rates = (Map<String, Object>) result.get("rates");
            double currentRate = ((Number) rates.get("KRW")).doubleValue();
            String date = String.valueOf(result.get("time_last_update_utc"));
            // 날짜 파싱: "Fri, 11 Apr 2026 00:00:01 +0000" → "2026-04-11"
            String shortDate = "";
            try {
                String[] parts = date.split(" ");
                shortDate = parts[3] + "-" + monthToNum(parts[2]) + "-" + parts[1];
            } catch (Exception e) {
                shortDate = LocalDate.now().toString();
            }

            // ⭐ [수정 4] 전일 환율 실제 조회 (Frankfurter API, 날짜별 무료 지원)
            double prevDayRate = fetchPrevDayRate(currentRate);

            prevCachedRate = prevDayRate;
            cachedRate = currentRate;
            cachedDate = shortDate;
            cacheTimestamp = System.currentTimeMillis();

            return buildExchangeResponse(currentRate, prevDayRate, shortDate, "open.er-api.com");

        } catch (Exception e) {
            log.warn("[주식 대시보드] 환율 조회 실패: {}", e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("exchangeRate", null);
            fallback.put("exchangeRateChange", null);
            fallback.put("exchangeRateChangePct", null);
            fallback.put("date", null);
            fallback.put("source", "fallback");
            fallback.put("error", e.getMessage());
            return ResponseEntity.ok(fallback);
        }
    }

    /** 환율 응답 Map 생성 헬퍼 */
    private ResponseEntity<Map<String, Object>> buildExchangeResponse(
            double current, double prev, String date, String source) {
        double change = current - prev;
        double changePct = (prev > 0)
                ? BigDecimal.valueOf(change / prev * 100).setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        Map<String, Object> resp = new HashMap<>();
        resp.put("exchangeRate", current);
        resp.put("exchangeRateChange", BigDecimal.valueOf(change).setScale(2, RoundingMode.HALF_UP).doubleValue());
        resp.put("exchangeRateChangePct", changePct);
        resp.put("date", date);
        resp.put("source", source);
        return ResponseEntity.ok(resp);
    }

    // ⭐ [수정 4] 전일 환율 조회 (Frankfurter API: 무료, 날짜별 지원)
    // open.er-api.com은 날짜별 조회 미지원 → Frankfurter 사용
    private double fetchPrevDayRate(double fallbackRate) {
        try {
            // 어제 날짜 (주말이면 금요일로 자동 처리됨 - Frankfurter가 직전 영업일 반환)
            String yesterday = java.time.LocalDate.now()
                    .minusDays(1).toString(); // yyyy-MM-dd
            String url = "https://api.frankfurter.app/" + yesterday + "?from=USD&to=KRW";
            RestTemplate rt = new RestTemplate();
            @SuppressWarnings("unchecked")
            Map<String, Object> data = rt.getForObject(url, Map.class);
            if (data != null && data.containsKey("rates")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> rates = (Map<String, Object>) data.get("rates");
                Object krw = rates.get("KRW");
                if (krw instanceof Number) {
                    return ((Number) krw).doubleValue();
                }
            }
        } catch (Exception e) {
            log.debug("[환율] 전일 환율 조회 실패 (fallback 사용): {}", e.getMessage());
        }
        return fallbackRate; // 조회 실패 시 change=0으로 안전하게 처리
    }

    
    /** 월 이름 → 숫자 변환 헬퍼 */
    private String monthToNum(String mon) {
        return switch (mon) {
            case "Jan" -> "01"; case "Feb" -> "02"; case "Mar" -> "03";
            case "Apr" -> "04"; case "May" -> "05"; case "Jun" -> "06";
            case "Jul" -> "07"; case "Aug" -> "08"; case "Sep" -> "09";
            case "Oct" -> "10"; case "Nov" -> "11"; default -> "12";
        };
    }

    // ⭐ [수정 Q6] 주식 자산 스냅샷 조회 API (코인의 /profit/snapshots 와 동일 패턴)
    @GetMapping("/stock/profit/snapshots")
    public ResponseEntity<?> getStockSnapshots(
            @RequestParam(defaultValue = "all") String period,
            Authentication authentication) {
        String userId = authentication.getName();
        List<StockAssetSnapshot> snapshots = stockAssetSnapshotService.getSnapshots(userId, period);
        List<Map<String, Object>> result = snapshots.stream().map(s -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("date", s.getDate().toString());
            m.put("evaluationAmount", s.getEvaluationAmount());
            m.put("depositAmount", s.getDepositAmount());
            m.put("profitAmount", s.getProfitAmount());
            m.put("profitRate", s.getProfitRate());
            return m;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ⭐ [수정 Q6] 주식 자산 스냅샷 수동 갱신 API (스냅샷 갱신 버튼용)
    @PostMapping("/stock/profit/snapshot")
    public ResponseEntity<?> createStockSnapshot(Authentication authentication) {
        String userId = authentication.getName();
        stockAssetSnapshotService.createOrUpdateSnapshot(userId);
        return ResponseEntity.ok(Map.of("message", "주식 자산 스냅샷이 갱신되었습니다."));
    }
}