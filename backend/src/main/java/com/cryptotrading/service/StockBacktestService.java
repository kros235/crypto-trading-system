package com.cryptotrading.service;

import com.cryptotrading.dto.kis.KisQuoteDTO;
import com.cryptotrading.dto.stock.backtest.StockBacktestRequestDTO;
import com.cryptotrading.dto.stock.backtest.StockBacktestResultDTO;
import com.cryptotrading.dto.stock.backtest.StockBacktestResultDTO.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 주식/ETF 백테스트 서비스
 * Phase 2 Day 62: StockBacktestService + StockBacktestView
 *
 * ⭐⭐⭐ [Day 62 신규] Phase 1 BacktestService 구조 재사용 (재사용률 약 90%) ⭐⭐⭐
 * Phase 1(코인) 대비 변경점:
 * 1. 데이터 소스: UpbitApiService.getDayCandlesWithTo() → KisApiService.getDailyCandlesByDateRange()
 * 2. 수수료율: 0.05% → 0.015% (backtest_master_plan.md 기준, StockSignalDetectorService와 동일)
 * 3. 슬리피지 0.05% 신규 반영 (Phase 1에는 없음, 주식 백테스팅 계획 문서 기준)
 * 4. 매수/매도 수량: BigDecimal → Integer (주식은 1주 단위 정수 거래)
 * 5. 레버리지 ETF decay 방지: 보유기간(maxHoldingDays) 초과 시 강제 매도 (Phase 1에는 없는 조건)
 * 6. 시장 추세 필터(useMarketTrendFilter)는 Day 62 기준 no-op 처리
 *    (코인은 BTC MA20을 프록시로 사용하나, 국내 주식은 대표 지수 프록시 종목 미설정 상태)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockBacktestService {

    private final KisApiService kisApiService;

    // ★ 주식 수수료율: 0.015% (Phase 1 코인: 0.05%) - StockSignalDetectorService와 동일 상수
    private static final BigDecimal FEE_RATE = new BigDecimal("0.00015");
    // ⭐ [Day 62 신규] 슬리피지 0.05% (backtest_master_plan.md Day 53 계획 반영)
    private static final BigDecimal SLIPPAGE_RATE = new BigDecimal("0.0005");
    private static final int SCALE = 8;
    private static final DateTimeFormatter KIS_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 백테스트 실행
     */
    public StockBacktestResultDTO runBacktest(String userId, StockBacktestRequestDTO request) {
        log.info("========== 주식 백테스트 시작 ==========");
        log.info("기간: {} ~ {}", request.getStartDate(), request.getEndDate());
        log.info("종목: {}", request.getStockCodes());
        log.info("초기 자본: {}원", request.getInitialBalance());

        int totalDays = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());

        SimulationState state = new SimulationState(request.getInitialBalance());

        // 종목별 일봉 데이터 조회 (지표 계산용 여유분 50일 포함)
        Map<String, List<KisQuoteDTO.DailyCandle>> candleDataMap = fetchCandleData(
                userId, request.getStockCodes(), request.getStartDate(), request.getEndDate());

        LocalDate currentDate = request.getStartDate();
        while (!currentDate.isAfter(request.getEndDate())) {
            simulateDay(currentDate, candleDataMap, request, state);
            currentDate = currentDate.plusDays(1);
        }

        closeAllPositions(request.getEndDate(), candleDataMap, state, request);

        return buildResult(request, state, totalDays);
    }

    /**
     * 종목별 기간별 일봉 데이터 조회 (KIS API - 최대 약 100건/회 페이징)
     */
    private Map<String, List<KisQuoteDTO.DailyCandle>> fetchCandleData(
            String userId, List<String> stockCodes, LocalDate startDate, LocalDate endDate) {

        Map<String, List<KisQuoteDTO.DailyCandle>> result = new HashMap<>();
        // 지표 계산용 여유분 50일을 더 앞선 시점부터 조회
        LocalDate fetchStart = startDate.minusDays(70);

        for (String stockCode : stockCodes) {
            try {
                List<KisQuoteDTO.DailyCandle> allCandles = new ArrayList<>();

                // ★★★ KIS API 1회 최대 약 100건 제한 대응 - 페이징 처리 (Upbit 페이징과 동일 패턴) ★★★
                LocalDate pageEnd = endDate;
                while (!pageEnd.isBefore(fetchStart)) {
                    LocalDate pageStart = pageEnd.minusDays(95); // 여유 있게 95일씩
                    if (pageStart.isBefore(fetchStart)) {
                        pageStart = fetchStart;
                    }

                    List<KisQuoteDTO.DailyCandle> candles = kisApiService.getDailyCandlesByDateRange(
                            userId, stockCode,
                            pageStart.format(KIS_DATE_FMT),
                            pageEnd.format(KIS_DATE_FMT));

                    if (candles == null || candles.isEmpty()) {
                        break;
                    }

                    allCandles.addAll(candles);

                    // 다음 페이지는 이번 구간의 시작일 하루 전까지
                    pageEnd = pageStart.minusDays(1);

                    // KIS API 호출 제한 방지 (초당 20건 제한)
                    Thread.sleep(150);
                }

                // 날짜순 정렬 (오래된 순), 중복 제거
                Map<String, KisQuoteDTO.DailyCandle> dedup = new LinkedHashMap<>();
                for (KisQuoteDTO.DailyCandle c : allCandles) {
                    if (c.getBusinessDate() != null) {
                        dedup.put(c.getBusinessDate(), c);
                    }
                }
                List<KisQuoteDTO.DailyCandle> sorted = new ArrayList<>(dedup.values());
                sorted.sort(Comparator.comparing(KisQuoteDTO.DailyCandle::getBusinessDate));

                result.put(stockCode, sorted);
                log.info("일봉 데이터 조회: {} - {}개", stockCode, sorted.size());

                Thread.sleep(150);
            } catch (Exception e) {
                log.error("일봉 데이터 조회 실패: {} - {}", stockCode, e.getMessage());
            }
        }

        return result;
    }
    
    /**
     * 일별 시뮬레이션
     */
    private void simulateDay(LocalDate date, Map<String, List<KisQuoteDTO.DailyCandle>> candleDataMap,
            StockBacktestRequestDTO request, SimulationState state) {

        if (state.getCurrentTradeDate() == null || !state.getCurrentTradeDate().equals(date)) {
            state.setCurrentTradeDate(date);
            state.setDailyBuyAmount(BigDecimal.ZERO);
            state.setDailySellRecovery(BigDecimal.ZERO);
            state.setDailyStopTriggered(false);

            BigDecimal holdingValue = calculateHoldingValue(state.getPositions(), candleDataMap, date);
            state.setDailyStartBalance(state.getCashBalance().add(holdingValue));
        }

        // 누적 손실 긴급정지 체크
        if (!state.isCumulativeLossTriggered() && request.getCumulativeLossLimitPct() != null) {
            BigDecimal cumulativeProfitLoss = state.getTrades().stream()
                    .filter(t -> "SELL".equals(t.getType()))
                    .map(BacktestTrade::getProfit)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal cumulativeLossRate = cumulativeProfitLoss
                    .divide(request.getInitialBalance(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

            if (cumulativeLossRate.compareTo(new BigDecimal(request.getCumulativeLossLimitPct())) <= 0) {
                state.setCumulativeLossTriggered(true);
                log.info("누적 손실 긴급정지 발동: {} - 누적손실률 {}%", date, cumulativeLossRate);
            }
        }

        if (state.isCumulativeLossTriggered()) {
            for (String stockCode : request.getStockCodes()) {
                List<KisQuoteDTO.DailyCandle> candles = candleDataMap.get(stockCode);
                KisQuoteDTO.DailyCandle todayCandle = findCandleByDate(candles, date);
                if (todayCandle == null) continue;
                checkSellSignals(stockCode, todayCandle.getClosePriceDecimal(), date, request, state);
            }
            recordDailyBalance(date, candleDataMap, request, state);
            return;
        }

        // 긴급 정지 체크
        if (request.getDailyStopLossPct() > -100) {
            BigDecimal holdingValue = calculateHoldingValue(state.getPositions(), candleDataMap, date);
            BigDecimal currentBalance = state.getCashBalance().add(holdingValue);
            BigDecimal dailyProfitRate = state.getDailyStartBalance().compareTo(BigDecimal.ZERO) > 0
                    ? currentBalance.subtract(state.getDailyStartBalance())
                            .divide(state.getDailyStartBalance(), 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;

            if (dailyProfitRate.compareTo(new BigDecimal(request.getDailyStopLossPct())) <= 0) {
                state.setDailyStopTriggered(true);
                log.info("긴급 정지 발동: {} - 일일 손실률 {}%", date, dailyProfitRate);
            }
        }

        for (String stockCode : request.getStockCodes()) {
            List<KisQuoteDTO.DailyCandle> candles = candleDataMap.get(stockCode);
            KisQuoteDTO.DailyCandle todayCandle = findCandleByDate(candles, date);

            if (todayCandle == null) continue;

            BigDecimal currentPrice = todayCandle.getClosePriceDecimal();

            // 1. 매도 체크 (보유기간 초과 강제 매도 포함)
            checkSellSignals(stockCode, currentPrice, date, request, state);

            // 2. 매수 체크
            if (canBuy(stockCode, currentPrice, request, state, date)) {
                if (checkBuySignal(stockCode, candles, date, request)) {
                    executeBuy(stockCode, currentPrice, date, "매수 신호", request, state);
                }
            }
        }

        recordDailyBalance(date, candleDataMap, request, state);
    }

    /**
     * 매수 신호 체크 (Phase 1과 동일 로직 - 기술적 지표 4조건 중 최소 1개 + MA 하락 필수)
     * ⚠️ useMarketTrendFilter는 Day 62 기준 no-op (대표 지수 프록시 미설정)
     */
    private boolean checkBuySignal(String stockCode, List<KisQuoteDTO.DailyCandle> candles,
            LocalDate date, StockBacktestRequestDTO request) {

        List<KisQuoteDTO.DailyCandle> historicalCandles = getHistoricalCandles(candles, date,
                Math.max(request.getBbPeriod(), request.getRsiPeriod()) + 10);

        if (historicalCandles.size() < request.getBasePeriod()) {
            return false;
        }

        List<BigDecimal> prices = historicalCandles.stream()
                .map(KisQuoteDTO.DailyCandle::getClosePriceDecimal)
                .toList();

        BigDecimal currentPrice = prices.get(0);

        BigDecimal ma = calculateMA(prices, request.getBasePeriod());
        BigDecimal dropRate = calculateDropRate(currentPrice, ma);

        BigDecimal rsi = calculateRSI(prices, request.getRsiPeriod());

        BigDecimal[] bb = calculateBollingerBands(prices, request.getBbPeriod(), request.getBbMultiplier());

        List<BigDecimal> volumes = historicalCandles.stream()
                .map(KisQuoteDTO.DailyCandle::getVolumeDecimal)
                .toList();
        BigDecimal avgVolume = calculateMA(volumes, 20);
        BigDecimal currentVolume = volumes.isEmpty() ? BigDecimal.ZERO : volumes.get(0);
        BigDecimal volumeRatio = avgVolume.compareTo(BigDecimal.ZERO) > 0
                ? currentVolume.divide(avgVolume, 8, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        int conditionsMet = 0;

        if (dropRate.compareTo(request.getBuyThresholdPct()) <= 0) {
            conditionsMet++;
        }

        if (rsi != null && rsi.compareTo(new BigDecimal(request.getRsiBuyThreshold())) <= 0) {
            conditionsMet++;
        }

        if (currentPrice.compareTo(bb[2]) <= 0) {
            conditionsMet++;
        }

        if (volumeRatio.compareTo(new BigDecimal(request.getVolumeThreshold())) >= 0) {
            conditionsMet++;
        }

        return conditionsMet >= 1 && dropRate.compareTo(request.getBuyThresholdPct()) <= 0;
    }

    /**
     * 과거 캔들 데이터 추출 (targetDate 기준 최근 count개, 최신 → 과거 순)
     */
    private List<KisQuoteDTO.DailyCandle> getHistoricalCandles(List<KisQuoteDTO.DailyCandle> candles,
            LocalDate targetDate, int count) {
        List<KisQuoteDTO.DailyCandle> filtered = candles.stream()
                .filter(c -> !parseToLocalDate(c.getBusinessDate()).isAfter(targetDate))
                .toList();

        int size = filtered.size();
        if (size == 0) {
            return new ArrayList<>();
        }

        int fromIndex = Math.max(0, size - count);
        List<KisQuoteDTO.DailyCandle> result = new ArrayList<>(filtered.subList(fromIndex, size));
        Collections.reverse(result);

        return result;
    }

    /**
     * 매도 신호 체크
     * ⭐⭐⭐ [Day 62 신규] 조건 0: 보유기간 초과 강제 매도 (레버리지 ETF decay 방지)
     * - StockSignalDetectorService.detectSellSignal()의 실거래 로직과 동일 우선순위
     */
    private String checkSellSignal(Position position, BigDecimal currentPrice,
            BigDecimal priceChangeRate, LocalDate date, StockBacktestRequestDTO request) {

        // 조건 0: 보유기간 초과 (레버리지 ETF decay 방지) - 최우선 체크
        int holdingDays = (int) ChronoUnit.DAYS.between(position.getBuyDate(), date);
        Integer maxHoldingDays = request.getMaxHoldingDays() != null ? request.getMaxHoldingDays() : 20;
        if (holdingDays >= maxHoldingDays) {
            return String.format("보유기간 초과: %d일 (최대: %d일, 레버리지 decay 방지)", holdingDays, maxHoldingDays);
        }

        // 매도 슬리피지 반영 실제 체결 예상가 (매도는 불리한 방향 = 하락 슬리피지)
        BigDecimal effectiveSellPrice = currentPrice.multiply(BigDecimal.ONE.subtract(SLIPPAGE_RATE));

        BigDecimal sellAmount = new BigDecimal(position.getQuantity()).multiply(effectiveSellPrice);
        BigDecimal sellFee = sellAmount.multiply(FEE_RATE);
        BigDecimal netSellAmount = sellAmount.subtract(sellFee);

        BigDecimal buyAmount = new BigDecimal(position.getQuantity()).multiply(position.getAvgPrice());
        BigDecimal buyFee = buyAmount.multiply(FEE_RATE);
        BigDecimal totalBuyAmount = buyAmount.add(buyFee);

        BigDecimal netProfitRate = totalBuyAmount.compareTo(BigDecimal.ZERO) > 0
                ? netSellAmount.subtract(totalBuyAmount)
                        .divide(totalBuyAmount, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        // 1. 목표 수익률 도달
        if (netProfitRate.compareTo(request.getSellTargetPct()) >= 0) {
            return String.format("목표 수익률 도달: %.2f%% (수수료/슬리피지 반영)", netProfitRate);
        }

        // 2. 손절매
        if (Boolean.TRUE.equals(request.getUseStopLoss()) && priceChangeRate.compareTo(request.getStopLossPct()) <= 0) {
            return String.format("손절매: %.2f%%", priceChangeRate);
        }

        // 3. 트레일링 스톱
        if (Boolean.TRUE.equals(request.getUseTrailingStop()) && position.getHighestPrice() != null) {
            BigDecimal trailingDropRate = position.getHighestPrice().subtract(currentPrice)
                    .divide(position.getHighestPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

            if (trailingDropRate.compareTo(request.getTrailingStopPct()) >= 0
                    && netProfitRate.compareTo(BigDecimal.ZERO) > 0) {
                return String.format("트레일링 스톱: 최고가 대비 -%.2f%%", trailingDropRate);
            }
        }

        return null;
    }

    /**
     * 매수 실행 (수량은 정수, 슬리피지 반영)
     */
    private void executeBuy(String stockCode, BigDecimal price, LocalDate date,
            String signal, StockBacktestRequestDTO request, SimulationState state) {
        boolean useRoundRobin = request.getUseRoundRobin() != null ? request.getUseRoundRobin() : true;

        BigDecimal dailyLimit = request.getDailyTradeLimitPct() != null
                ? request.getInitialBalance()
                        .multiply(new BigDecimal(request.getDailyTradeLimitPct()))
                        .divide(new BigDecimal("100"), SCALE, RoundingMode.DOWN)
                : request.getInitialBalance();

        BigDecimal remainingDailyLimit = dailyLimit.subtract(state.getDailyBuyAmount());
        if (Boolean.TRUE.equals(request.getUseDailyLimitRecovery())) {
            remainingDailyLimit = remainingDailyLimit.add(state.getDailySellRecovery());
            if (remainingDailyLimit.compareTo(dailyLimit) > 0) {
                remainingDailyLimit = dailyLimit;
            }
        }

        BigDecimal buyAmount;
        if (useRoundRobin) {
            BigDecimal maxPerTrade = request.getInitialBalance()
                    .multiply(new BigDecimal(
                        request.getMaxPositionPct() != null ? request.getMaxPositionPct() : 100))
                    .divide(new BigDecimal("100"), SCALE, RoundingMode.DOWN);
            buyAmount = remainingDailyLimit.min(state.getCashBalance()).min(maxPerTrade);
        } else {
            BigDecimal fixedAmount = request.getFixedBuyAmount() != null
                    ? request.getFixedBuyAmount()
                    : new BigDecimal("100000");
            buyAmount = fixedAmount.min(remainingDailyLimit).min(state.getCashBalance());
        }

        // 매수 슬리피지 반영 실제 체결 예상가 (매수는 불리한 방향 = 상승 슬리피지)
        BigDecimal effectiveBuyPrice = price.multiply(BigDecimal.ONE.add(SLIPPAGE_RATE));

        // ⭐⭐⭐ [Day 62] 정수 수량 계산 - 최소 1주 이상 매수 가능해야 실행 ⭐⭐⭐
        int quantity = buyAmount.divide(effectiveBuyPrice, 0, RoundingMode.DOWN).intValue();
        if (quantity < 1) return;

        BigDecimal actualBuyAmount = effectiveBuyPrice.multiply(new BigDecimal(quantity));
        BigDecimal fee = actualBuyAmount.multiply(FEE_RATE);
        BigDecimal totalCost = actualBuyAmount.add(fee);

        if (totalCost.compareTo(state.getCashBalance()) > 0) return;

        Position position = new Position(stockCode, effectiveBuyPrice, quantity, date);
        state.getPositions().add(position);
        state.setCashBalance(state.getCashBalance().subtract(totalCost));

        state.getTrades().add(BacktestTrade.builder()
                .stockCode(stockCode)
                .type("BUY")
                .tradeDate(date)
                .price(effectiveBuyPrice)
                .quantity(quantity)
                .amount(totalCost)
                .signal(signal)
                .build());

        state.setBuyCount(state.getBuyCount() + 1);
        log.debug("매수: {} - {}원 x {}주 ({})", stockCode, effectiveBuyPrice, quantity, signal);

        state.setDailyBuyAmount(state.getDailyBuyAmount().add(totalCost));
    }

    /**
     * 매도 실행
     */
    private void executeSell(Position position, BigDecimal price, LocalDate date,
            String signal, SimulationState state, StockBacktestRequestDTO request) {
        BigDecimal effectiveSellPrice = price.multiply(BigDecimal.ONE.subtract(SLIPPAGE_RATE));

        BigDecimal sellAmount = new BigDecimal(position.getQuantity()).multiply(effectiveSellPrice);
        BigDecimal fee = sellAmount.multiply(FEE_RATE);
        BigDecimal actualAmount = sellAmount.subtract(fee);

        BigDecimal buyAmount = new BigDecimal(position.getQuantity()).multiply(position.getAvgPrice());
        BigDecimal profit = actualAmount.subtract(buyAmount);
        BigDecimal profitRate = buyAmount.compareTo(BigDecimal.ZERO) > 0
                ? profit.divide(buyAmount, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        int holdingDays = (int) ChronoUnit.DAYS.between(position.getBuyDate(), date);

        state.setCashBalance(state.getCashBalance().add(actualAmount));
        state.setTotalProfit(state.getTotalProfit().add(profit));

        if (profit.compareTo(BigDecimal.ZERO) > 0) {
            state.setWinCount(state.getWinCount() + 1);
            state.setTotalWinAmount(state.getTotalWinAmount().add(profit));
        } else {
            state.setLoseCount(state.getLoseCount() + 1);
            state.setTotalLoseAmount(state.getTotalLoseAmount().add(profit.abs()));
        }

        state.getTrades().add(BacktestTrade.builder()
                .stockCode(position.getStockCode())
                .type("SELL")
                .tradeDate(date)
                .price(effectiveSellPrice)
                .quantity(position.getQuantity())
                .amount(sellAmount)
                .profit(profit)
                .profitRate(profitRate)
                .signal(signal)
                .holdingDays(holdingDays)
                .build());

        // 일일 한도 복구
        if (Boolean.TRUE.equals(request.getUseDailyLimitRecovery())
                && request.getDailyTradeLimitPct() != null
                && request.getDailyTradeLimitPct() < 100) {
            BigDecimal effectiveDailyLimit = request.getInitialBalance()
                    .multiply(new BigDecimal(request.getDailyTradeLimitPct()))
                    .divide(new BigDecimal("100"), SCALE, RoundingMode.HALF_UP);

            BigDecimal currentRemaining = effectiveDailyLimit
                    .subtract(state.getDailyBuyAmount())
                    .add(state.getDailySellRecovery());

            BigDecimal maxRecoverable = effectiveDailyLimit.subtract(currentRemaining);
            if (maxRecoverable.compareTo(BigDecimal.ZERO) < 0) {
                maxRecoverable = BigDecimal.ZERO;
            }

            BigDecimal actualRecovery = actualAmount.min(maxRecoverable);
            state.setDailySellRecovery(state.getDailySellRecovery().add(actualRecovery));
        }

        // 연속 손절 카운터 업데이트
        if (signal.contains("손절") || profitRate.compareTo(BigDecimal.ZERO) < 0) {
            int currentCount = state.getConsecutiveStopLossCount().getOrDefault(position.getStockCode(), 0) + 1;
            state.getConsecutiveStopLossCount().put(position.getStockCode(), currentCount);

            int limit = request.getConsecutiveStopLossLimit() != null ? request.getConsecutiveStopLossLimit() : 3;
            if (currentCount >= limit) {
                state.getCoinBuyBlockedUntil().put(position.getStockCode(), date.plusDays(1));
                log.info("연속 손절 한도 도달 (백테스팅): {} - {}까지 매수 금지", position.getStockCode(), date.plusDays(1));
            }
        } else {
            state.getConsecutiveStopLossCount().remove(position.getStockCode());
            state.getCoinBuyBlockedUntil().remove(position.getStockCode());
        }

        state.setSellCount(state.getSellCount() + 1);
        log.debug("매도: {} - {}원 x {}주, 손익: {}원 ({}, 보유 {}일)",
                position.getStockCode(), effectiveSellPrice, position.getQuantity(), profit, signal, holdingDays);
    }

    private void checkSellSignals(String stockCode, BigDecimal currentPrice, LocalDate date,
            StockBacktestRequestDTO request, SimulationState state) {
        List<Position> positionsToSell = new ArrayList<>();

        for (Position position : state.getPositions()) {
            if (!position.getStockCode().equals(stockCode)) continue;

            if (currentPrice.compareTo(position.getHighestPrice()) > 0) {
                position.setHighestPrice(currentPrice);
            }

            BigDecimal profitRate = calculateProfitRate(position.getAvgPrice(), currentPrice);
            String sellSignal = checkSellSignal(position, currentPrice, profitRate, date, request);

            if (sellSignal != null) {
                executeSell(position, currentPrice, date, sellSignal, state, request);
                positionsToSell.add(position);
            }
        }

        state.getPositions().removeAll(positionsToSell);
    }

    private boolean canBuy(String stockCode, BigDecimal currentPrice, StockBacktestRequestDTO request,
        SimulationState state, LocalDate date) {

        if (state.isCumulativeLossTriggered()) return false;

        LocalDate blockedUntil = state.getCoinBuyBlockedUntil().get(stockCode);
        if (blockedUntil != null && !date.isAfter(blockedUntil)) return false;

        if (state.isDailyStopTriggered()) return false;

        // 최소 매수 가능 현금 (1주 이상 매수 가능한지는 executeBuy에서 최종 확인)
        if (state.getCashBalance().compareTo(new BigDecimal("10000")) < 0) return false;

        List<Position> stockHoldings = state.getPositions().stream()
                .filter(p -> p.getStockCode().equals(stockCode))
                .toList();

        if (stockHoldings.size() >= request.getMaxHoldingsPerStock()) return false;

        // 분할 매수(물타기) 간격 조건
        if (!stockHoldings.isEmpty() && currentPrice != null) {
            Position lastPosition = stockHoldings.get(stockHoldings.size() - 1);
            BigDecimal lastBuyPrice = lastPosition.getAvgPrice();

            BigDecimal dropRateFromLastBuy = lastBuyPrice.subtract(currentPrice)
                    .divide(lastBuyPrice, SCALE, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

            BigDecimal requiredDrop = request.getAdditionalDropPct() != null
                    ? request.getAdditionalDropPct()
                    : new BigDecimal("1.0");

            if (dropRateFromLastBuy.compareTo(requiredDrop) < 0) return false;
        }

        boolean useRoundRobin = request.getUseRoundRobin() != null ? request.getUseRoundRobin() : true;

        BigDecimal dailyLimit = request.getDailyTradeLimitPct() != null
                ? request.getInitialBalance()
                        .multiply(new BigDecimal(request.getDailyTradeLimitPct()))
                        .divide(new BigDecimal("100"), SCALE, RoundingMode.DOWN)
                : request.getInitialBalance();

        BigDecimal buyAmount;
        if (useRoundRobin) {
            BigDecimal maxPerTrade = request.getInitialBalance()
                    .multiply(new BigDecimal(
                        request.getMaxPositionPct() != null ? request.getMaxPositionPct() : 100))
                    .divide(new BigDecimal("100"), SCALE, RoundingMode.DOWN);
            buyAmount = dailyLimit.subtract(state.getDailyBuyAmount())
                    .min(state.getCashBalance())
                    .min(maxPerTrade);
        } else {
            BigDecimal fixedAmount = request.getFixedBuyAmount() != null
                    ? request.getFixedBuyAmount()
                    : new BigDecimal("100000");
            buyAmount = fixedAmount.min(state.getCashBalance());
        }

        if (request.getDailyTradeLimitPct() != null) {
            BigDecimal remainingLimit = dailyLimit
                    .subtract(state.getDailyBuyAmount())
                    .add(state.getDailySellRecovery());

            if (remainingLimit.compareTo(dailyLimit) > 0) {
                remainingLimit = dailyLimit;
            }

            if (buyAmount.compareTo(remainingLimit) > 0) return false;
        }

        if (request.getMaxPositionPct() != null && request.getMaxPositionPct() < 100) {
            BigDecimal maxPositionAmount = request.getInitialBalance()
                    .multiply(new BigDecimal(request.getMaxPositionPct()))
                    .divide(new BigDecimal("100"), 0, RoundingMode.DOWN);

            BigDecimal currentStockAmount = state.getPositions().stream()
                    .filter(p -> p.getStockCode().equals(stockCode))
                    .map(p -> new BigDecimal(p.getQuantity()).multiply(p.getAvgPrice()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (currentStockAmount.add(buyAmount).compareTo(maxPositionAmount) > 0) return false;
        }

        // 최소 1주 이상 매수 가능한지 확인 (슬리피지 반영)
        BigDecimal effectiveBuyPrice = currentPrice.multiply(BigDecimal.ONE.add(SLIPPAGE_RATE));
        return buyAmount.divide(effectiveBuyPrice, 0, RoundingMode.DOWN).intValue() >= 1;
    }

    private void recordDailyBalance(LocalDate date, Map<String, List<KisQuoteDTO.DailyCandle>> candleDataMap,
            StockBacktestRequestDTO request, SimulationState state) {
        BigDecimal holdingValue = calculateHoldingValue(state.getPositions(), candleDataMap, date);
        BigDecimal totalBalance = state.getCashBalance().add(holdingValue);

        if (totalBalance.compareTo(state.getPeakBalance()) > 0) {
            state.setPeakBalance(totalBalance);
        }

        if (state.getPeakBalance().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal drawdown = state.getPeakBalance().subtract(totalBalance)
                    .divide(state.getPeakBalance(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            if (drawdown.compareTo(state.getMaxDrawdown()) > 0) {
                state.setMaxDrawdown(drawdown);
            }
        }

        BigDecimal profitRate = totalBalance.subtract(request.getInitialBalance())
                .divide(request.getInitialBalance(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        state.getDailyBalances().add(DailyBalance.builder()
                .date(date)
                .balance(totalBalance)
                .profitRate(profitRate)
                .build());
    }

    private void closeAllPositions(LocalDate date, Map<String, List<KisQuoteDTO.DailyCandle>> candleDataMap,
            SimulationState state, StockBacktestRequestDTO request) {
        for (Position position : new ArrayList<>(state.getPositions())) {
            KisQuoteDTO.DailyCandle candle = findCandleByDate(candleDataMap.get(position.getStockCode()), date);
            if (candle != null) {
                executeSell(position, candle.getClosePriceDecimal(), date, "백테스트 종료", state, request);
            }
        }
        state.getPositions().clear();
    }

    private StockBacktestResultDTO buildResult(StockBacktestRequestDTO request, SimulationState state, int totalDays) {
        BigDecimal finalBalance = state.getCashBalance();
        BigDecimal totalProfitRate = finalBalance.subtract(request.getInitialBalance())
                .divide(request.getInitialBalance(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        int totalTrades = state.getBuyCount() + state.getSellCount();
        BigDecimal winRate = state.getSellCount() > 0
                ? new BigDecimal(state.getWinCount())
                        .divide(new BigDecimal(state.getSellCount()), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        BigDecimal avgProfit = state.getWinCount() > 0
                ? state.getTotalWinAmount().divide(new BigDecimal(state.getWinCount()), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal avgLoss = state.getLoseCount() > 0
                ? state.getTotalLoseAmount().divide(new BigDecimal(state.getLoseCount()), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal profitFactor = state.getTotalLoseAmount().compareTo(BigDecimal.ZERO) > 0
                ? state.getTotalWinAmount().divide(state.getTotalLoseAmount(), 2, RoundingMode.HALF_UP)
                : state.getTotalWinAmount().compareTo(BigDecimal.ZERO) > 0
                        ? new BigDecimal("999")
                        : BigDecimal.ZERO;

        List<StockPerformance> stockPerformances = calculateStockPerformances(state.getTrades(), request.getStockCodes());

        log.info("========== 주식 백테스트 완료 ==========");
        log.info("최종 잔고: {}원", finalBalance);
        log.info("총 수익률: {}%", totalProfitRate);
        log.info("승률: {}%", winRate);

        return StockBacktestResultDTO.builder()
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalDays(totalDays)
                .stockCodes(request.getStockCodes())
                .initialBalance(request.getInitialBalance())
                .finalBalance(finalBalance)
                .totalProfit(state.getTotalProfit())
                .totalProfitRate(totalProfitRate)
                .maxDrawdown(state.getMaxDrawdown())
                .sharpeRatio(calculateSharpeRatio(state.getDailyBalances()))
                .totalTrades(totalTrades)
                .buyCount(state.getBuyCount())
                .sellCount(state.getSellCount())
                .winCount(state.getWinCount())
                .loseCount(state.getLoseCount())
                .winRate(winRate)
                .avgProfit(avgProfit)
                .avgLoss(avgLoss)
                .profitFactor(profitFactor)
                .stockPerformances(stockPerformances)
                .dailyBalances(state.getDailyBalances())
                .trades(state.getTrades())
                .build();
    }

    // ============ 유틸리티 메서드 (Phase 1과 동일 로직) ============

    private KisQuoteDTO.DailyCandle findCandleByDate(List<KisQuoteDTO.DailyCandle> candles, LocalDate date) {
        if (candles == null) return null;
        return candles.stream()
                .filter(c -> c.getBusinessDate() != null && parseToLocalDate(c.getBusinessDate()).equals(date))
                .findFirst()
                .orElse(null);
    }

    private LocalDate parseToLocalDate(String yyyyMMdd) {
        if (yyyyMMdd == null || yyyyMMdd.length() < 8) return null;
        return LocalDate.parse(yyyyMMdd, KIS_DATE_FMT);
    }

    private BigDecimal calculateMA(List<BigDecimal> prices, int period) {
        if (prices.size() < period) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            sum = sum.add(prices.get(i));
        }
        return sum.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDropRate(BigDecimal currentPrice, BigDecimal ma) {
        if (ma.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return currentPrice.subtract(ma)
                .divide(ma, SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private BigDecimal calculateRSI(List<BigDecimal> prices, int period) {
        if (prices.size() < period + 1) return null;

        BigDecimal gainSum = BigDecimal.ZERO;
        BigDecimal lossSum = BigDecimal.ZERO;

        for (int i = 0; i < period; i++) {
            BigDecimal change = prices.get(i).subtract(prices.get(i + 1));
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                gainSum = gainSum.add(change);
            } else {
                lossSum = lossSum.add(change.abs());
            }
        }

        BigDecimal avgGain = gainSum.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
        BigDecimal avgLoss = lossSum.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);

        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal("100");

        BigDecimal rs = avgGain.divide(avgLoss, SCALE, RoundingMode.HALF_UP);
        return new BigDecimal("100").subtract(
                new BigDecimal("100").divide(BigDecimal.ONE.add(rs), SCALE, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal[] calculateBollingerBands(List<BigDecimal> prices, int period, int multiplier) {
        BigDecimal ma = calculateMA(prices, period);

        if (prices.size() < period) {
            return new BigDecimal[] { ma, ma, ma };
        }

        BigDecimal sumSquaredDiff = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            BigDecimal diff = prices.get(i).subtract(ma);
            sumSquaredDiff = sumSquaredDiff.add(diff.multiply(diff));
        }

        BigDecimal variance = sumSquaredDiff.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
        BigDecimal deviation = stdDev.multiply(BigDecimal.valueOf(multiplier));

        return new BigDecimal[] {
                ma.add(deviation).setScale(SCALE, RoundingMode.HALF_UP),
                ma,
                ma.subtract(deviation).setScale(SCALE, RoundingMode.HALF_UP)
        };
    }

    private BigDecimal calculateProfitRate(BigDecimal buyPrice, BigDecimal currentPrice) {
        return currentPrice.subtract(buyPrice)
                .divide(buyPrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private BigDecimal calculateHoldingValue(List<Position> positions,
            Map<String, List<KisQuoteDTO.DailyCandle>> candleDataMap, LocalDate date) {
        BigDecimal total = BigDecimal.ZERO;
        for (Position position : positions) {
            KisQuoteDTO.DailyCandle candle = findCandleByDate(candleDataMap.get(position.getStockCode()), date);
            if (candle != null) {
                total = total.add(new BigDecimal(position.getQuantity()).multiply(candle.getClosePriceDecimal()));
            }
        }
        return total;
    }

    private BigDecimal calculateSharpeRatio(List<DailyBalance> dailyBalances) {
        if (dailyBalances.size() < 2) return BigDecimal.ZERO;

        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < dailyBalances.size(); i++) {
            BigDecimal prevBalance = dailyBalances.get(i - 1).getBalance();
            if (prevBalance.compareTo(BigDecimal.ZERO) == 0) continue;
            BigDecimal dailyReturn = dailyBalances.get(i).getBalance()
                    .subtract(prevBalance)
                    .divide(prevBalance, SCALE, RoundingMode.HALF_UP);
            returns.add(dailyReturn);
        }

        if (returns.isEmpty()) return BigDecimal.ZERO;

        BigDecimal avgReturn = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(returns.size()), SCALE, RoundingMode.HALF_UP);

        BigDecimal variance = returns.stream()
                .map(r -> r.subtract(avgReturn).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(returns.size()), SCALE, RoundingMode.HALF_UP);

        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        if (stdDev.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        // 국내 증시 연간 거래일수(약 252일) 기준 연환산
        return avgReturn.divide(stdDev, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(Math.sqrt(252)));
    }

    private List<StockPerformance> calculateStockPerformances(List<BacktestTrade> trades, List<String> stockCodes) {
        List<StockPerformance> performances = new ArrayList<>();

        for (String code : stockCodes) {
            List<BacktestTrade> stockTrades = trades.stream()
                    .filter(t -> t.getStockCode().equals(code))
                    .collect(Collectors.toList());

            List<BacktestTrade> sells = stockTrades.stream()
                    .filter(t -> "SELL".equals(t.getType()))
                    .collect(Collectors.toList());

            int winCount = (int) sells.stream()
                    .filter(t -> t.getProfit() != null && t.getProfit().compareTo(BigDecimal.ZERO) > 0)
                    .count();
            int loseCount = (int) sells.stream()
                    .filter(t -> t.getProfit() != null && t.getProfit().compareTo(BigDecimal.ZERO) < 0)
                    .count();

            BigDecimal totalProfit = sells.stream()
                    .filter(t -> t.getProfit() != null)
                    .map(BacktestTrade::getProfit)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalAmount = stockTrades.stream()
                    .filter(t -> "BUY".equals(t.getType()))
                    .map(BacktestTrade::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal profitRate = totalAmount.compareTo(BigDecimal.ZERO) > 0
                    ? totalProfit.divide(totalAmount, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;

            BigDecimal avgHoldingDays = sells.stream()
                    .filter(t -> t.getHoldingDays() != null)
                    .map(t -> new BigDecimal(t.getHoldingDays()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (!sells.isEmpty()) {
                avgHoldingDays = avgHoldingDays.divide(new BigDecimal(sells.size()), 1, RoundingMode.HALF_UP);
            }

            performances.add(StockPerformance.builder()
                    .stockCode(code)
                    .tradeCount(stockTrades.size())
                    .winCount(winCount)
                    .loseCount(loseCount)
                    .totalProfit(totalProfit)
                    .profitRate(profitRate)
                    .avgHoldingDays(avgHoldingDays)
                    .build());
        }

        return performances;
    }

    // ============ 내부 클래스 ============

    @lombok.Data
    private static class SimulationState {
        private BigDecimal cashBalance;
        private BigDecimal totalProfit = BigDecimal.ZERO;
        private BigDecimal peakBalance;
        private BigDecimal maxDrawdown = BigDecimal.ZERO;
        private BigDecimal totalWinAmount = BigDecimal.ZERO;
        private BigDecimal totalLoseAmount = BigDecimal.ZERO;
        private int buyCount = 0;
        private int sellCount = 0;
        private int winCount = 0;
        private int loseCount = 0;
        private List<Position> positions = new ArrayList<>();
        private List<DailyBalance> dailyBalances = new ArrayList<>();
        private List<BacktestTrade> trades = new ArrayList<>();

        private BigDecimal dailyBuyAmount = BigDecimal.ZERO;
        private BigDecimal dailySellRecovery = BigDecimal.ZERO;
        private LocalDate currentTradeDate = null;
        private BigDecimal dailyStartBalance = BigDecimal.ZERO;
        private boolean dailyStopTriggered = false;

        private boolean cumulativeLossTriggered = false;
        private Map<String, Integer> consecutiveStopLossCount = new HashMap<>();
        private Map<String, LocalDate> coinBuyBlockedUntil = new HashMap<>(); // 종목별 매수 금지 일자 (필드명은 Phase 1과 통일)

        public SimulationState(BigDecimal initialBalance) {
            this.cashBalance = initialBalance;
            this.peakBalance = initialBalance;
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class Position {
        private String stockCode;
        private BigDecimal avgPrice;
        private int quantity; // ⭐ 주식은 정수 수량
        private LocalDate buyDate;
        private BigDecimal highestPrice;

        public Position(String stockCode, BigDecimal avgPrice, int quantity, LocalDate buyDate) {
            this.stockCode = stockCode;
            this.avgPrice = avgPrice;
            this.quantity = quantity;
            this.buyDate = buyDate;
            this.highestPrice = avgPrice;
        }
    }
}

    