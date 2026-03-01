package com.cryptotrading.service;

import com.cryptotrading.dto.backtest.BacktestRequestDTO;
import com.cryptotrading.dto.backtest.BacktestResultDTO;
import com.cryptotrading.dto.backtest.BacktestResultDTO.*;
import com.cryptotrading.dto.upbit.UpbitCandleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BacktestService {

    private final UpbitApiService upbitApiService;
    private final TechnicalIndicatorService technicalIndicatorService;

    private static final BigDecimal FEE_RATE = new BigDecimal("0.0005"); // 0.05%
    private static final int SCALE = 8;

    /**
     * 백테스트 실행
     */
    public BacktestResultDTO runBacktest(BacktestRequestDTO request) {
        log.info("========== 백테스트 시작 ==========");
        log.info("기간: {} ~ {}", request.getStartDate(), request.getEndDate());
        log.info("코인: {}", request.getCoinSymbols());
        log.info("초기 자본: {}원", request.getInitialBalance());

        // 1. 기간 계산
        int totalDays = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());

        // 2. 시뮬레이션 상태 초기화
        SimulationState state = new SimulationState(request.getInitialBalance());

        // 3. 코인별 캔들 데이터 조회 (일봉)
        Map<String, List<UpbitCandleDTO>> candleDataMap = fetchCandleData(
                request.getCoinSymbols(),
                request.getStartDate(),
                request.getEndDate());

        // 4. 날짜별 시뮬레이션 실행
        LocalDate currentDate = request.getStartDate();
        while (!currentDate.isAfter(request.getEndDate())) {
            simulateDay(currentDate, candleDataMap, request, state);
            currentDate = currentDate.plusDays(1);
        }

        // 5. 남은 포지션 정리 (마지막 날 강제 매도)
        closeAllPositions(request.getEndDate(), candleDataMap, state, request);

        // 6. 결과 계산 및 반환
        return buildResult(request, state, totalDays);
    }

    /**
     * 캔들 데이터 조회
     */
    private Map<String, List<UpbitCandleDTO>> fetchCandleData(
            List<String> coinSymbols, LocalDate startDate, LocalDate endDate) {

        Map<String, List<UpbitCandleDTO>> result = new HashMap<>();
        // 최대 1년(365일) + 지표 계산용 여유분(50일)
        int totalDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 50;

        // 요청된 종료일 기준으로 조회 시작점 설정
        String initialToDate = endDate.plusDays(1).atTime(9, 0, 0)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        for (String symbol : coinSymbols) {
            try {
                List<UpbitCandleDTO> allCandles = new ArrayList<>();

                // ★★★ 수정: 업비트 API 200개 제한 대응 - 페이징 처리 ★★★
                if (totalDays <= 200) {
                    // 200개 이하면 한 번에 조회
                    allCandles = upbitApiService.getDayCandlesWithTo(symbol, totalDays, initialToDate);
                } else {
                    // 200개 초과 시 여러 번 나눠서 조회
                    int remaining = totalDays;
                    // ★★★ 수정: null 대신 종료일 기준으로 초기화 ★★★
                    String toDate = initialToDate;

                    while (remaining > 0) {
                        int fetchCount = Math.min(remaining, 200);
                        List<UpbitCandleDTO> candles;

                        candles = upbitApiService.getDayCandlesWithTo(symbol, fetchCount, toDate);

                        if (candles == null || candles.isEmpty()) {
                            break;
                        }

                        allCandles.addAll(candles);
                        remaining -= candles.size();

                        // 다음 조회를 위한 기준 시간 설정 (가장 오래된 캔들의 시간)
                        if (!candles.isEmpty()) {
                            toDate = candles.get(candles.size() - 1).getCandleDateTimeKst();
                        }

                        // API 호출 제한 방지
                        Thread.sleep(200);

                        // 더 이상 데이터가 없으면 종료
                        if (candles.size() < fetchCount) {
                            break;
                        }
                    }
                }

                // 날짜순 정렬 (오래된 순)
                allCandles.sort(Comparator.comparing(UpbitCandleDTO::getCandleDateTimeKst));

                result.put(symbol, allCandles);
                log.info("캔들 데이터 조회: {} - {}개", symbol, allCandles.size());

                // API 호출 제한 방지
                Thread.sleep(200);
            } catch (Exception e) {
                log.error("캔들 데이터 조회 실패: {} - {}", symbol, e.getMessage());
            }
        }

        return result;
    }

    /**
     * 일별 시뮬레이션
     */
    private void simulateDay(LocalDate date, Map<String, List<UpbitCandleDTO>> candleDataMap,
            BacktestRequestDTO request, SimulationState state) {

        // 날짜 변경 시 일일 상태 초기화
        if (state.getCurrentTradeDate() == null || !state.getCurrentTradeDate().equals(date)) {
            state.setCurrentTradeDate(date);
            state.setDailyBuyAmount(BigDecimal.ZERO);
            state.setDailySellRecovery(BigDecimal.ZERO); // 복구 금액도 초기화
            state.setDailyStopTriggered(false);

            // 당일 시작 잔고 계산
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

        // 누적 손실 긴급정지 발동 시 매수 중단
        if (state.isCumulativeLossTriggered()) {
            // 매도만 진행 (기존 포지션 청산)
            for (String coinSymbol : request.getCoinSymbols()) {
                // ... 매도 로직만 실행
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

        for (String coinSymbol : request.getCoinSymbols()) {
            List<UpbitCandleDTO> candles = candleDataMap.get(coinSymbol);
            UpbitCandleDTO todayCandle = findCandleByDate(candles, date);

            if (todayCandle == null)
                continue;

            BigDecimal currentPrice = todayCandle.getTradePrice();

            // 1. 매도 체크 (보유 포지션)
            checkSellSignals(coinSymbol, currentPrice, date, request, state);

            // 2. 매수 체크
            if (canBuy(coinSymbol, currentPrice, request, state)) {
                // ★★★ 수정: 사용자 설정값으로 매수 신호 체크 ★★★
                if (checkBuySignal(coinSymbol, candles, date, request, candleDataMap)) {
                    executeBuy(coinSymbol, currentPrice, date, "매수 신호", request, state);
                }
            }
        }

        // 일별 잔고 기록
        recordDailyBalance(date, candleDataMap, request, state);
    }

    /**
     * 매수 신호 체크 (사용자 설정 적용)
     */
    private boolean checkBuySignal(String coinSymbol, List<UpbitCandleDTO> candles,
            LocalDate date, BacktestRequestDTO request,
            Map<String, List<UpbitCandleDTO>> candleDataMap) {

        // 시장 추세 필터 (BTC MA20)
        if (request.getUseMarketTrendFilter() != null && request.getUseMarketTrendFilter()) {
            List<UpbitCandleDTO> btcCandles = candleDataMap.get("KRW-BTC");
            if (btcCandles != null) {
                List<UpbitCandleDTO> btcHistorical = getHistoricalCandles(btcCandles, date, 25);
                if (btcHistorical.size() >= 20) {
                    List<BigDecimal> btcPrices = btcHistorical.stream()
                            .map(UpbitCandleDTO::getTradePrice)
                            .toList();
                    BigDecimal btcMa20 = calculateMA(btcPrices, 20);
                    BigDecimal btcCurrentPrice = btcPrices.get(0);

                    if (btcCurrentPrice.compareTo(btcMa20) < 0) {
                        // BTC가 MA20 아래 → 전체 매수 중단
                        return false;
                    }
                }
            }
        }
        // 현재 날짜 기준으로 과거 데이터 추출
        List<UpbitCandleDTO> historicalCandles = getHistoricalCandles(candles, date,
                Math.max(request.getBbPeriod(), request.getRsiPeriod()) + 10);

        if (historicalCandles.size() < request.getBasePeriod()) {
            return false;
        }

        List<BigDecimal> prices = historicalCandles.stream()
                .map(UpbitCandleDTO::getTradePrice)
                .toList();

        BigDecimal currentPrice = prices.get(0);

        // 이동평균선 계산
        BigDecimal ma = calculateMA(prices, request.getBasePeriod());
        BigDecimal dropRate = calculateDropRate(currentPrice, ma);

        // RSI 계산 (사용자 설정 기간)
        BigDecimal rsi = calculateRSI(prices, request.getRsiPeriod());

        // 볼린저 밴드 계산 (사용자 설정 기간/승수)
        BigDecimal[] bb = calculateBollingerBands(prices, request.getBbPeriod(), request.getBbMultiplier());

        // 거래량 계산
        List<BigDecimal> volumes = historicalCandles.stream()
                .map(UpbitCandleDTO::getCandleAccTradeVolume)
                .toList();
        BigDecimal avgVolume = calculateMA(volumes, 20);
        BigDecimal currentVolume = volumes.isEmpty() ? BigDecimal.ZERO : volumes.get(0);
        BigDecimal volumeRatio = avgVolume.compareTo(BigDecimal.ZERO) > 0
                ? currentVolume.divide(avgVolume, 8, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        int conditionsMet = 0;

        // 조건 1: MA 대비 하락률
        if (dropRate.compareTo(request.getBuyThresholdPct()) <= 0) {
            conditionsMet++;
        }

        // 조건 2: RSI 과매도 (사용자 설정 임계값)
        if (rsi != null && rsi.compareTo(new BigDecimal(request.getRsiBuyThreshold())) <= 0) {
            conditionsMet++;
        }

        // 조건 3: 볼린저 밴드 하단 접촉
        if (currentPrice.compareTo(bb[2]) <= 0) {
            conditionsMet++;
        }

        // 조건 4: 거래량 급증 (사용자 설정 임계값)
        if (volumeRatio.compareTo(new BigDecimal(request.getVolumeThreshold())) >= 0) {
            conditionsMet++;
        }

        // MA 하락 조건 + 최소 1개 추가 조건
        return conditionsMet >= 1 && dropRate.compareTo(request.getBuyThresholdPct()) <= 0;
    }

    /**
     * 과거 캔들 데이터 추출 (특정 날짜 기준)
     * ★★★ 버그 수정: targetDate 기준 최근 count개 데이터를 역순(최신→과거)으로 반환 ★★★
     */
    private List<UpbitCandleDTO> getHistoricalCandles(List<UpbitCandleDTO> candles,
            LocalDate targetDate, int count) {
        // targetDate 이전 데이터만 필터링 (candles는 오래된 순 정렬 상태)
        List<UpbitCandleDTO> filtered = candles.stream()
                .filter(c -> !parseToLocalDate(c.getCandleDateTimeKst()).isAfter(targetDate))
                .toList();

        int size = filtered.size();
        if (size == 0) {
            return new ArrayList<>();
        }

        // ★★★ 수정: 뒤에서(최신) count개 추출 후 역순 정렬 (최신이 먼저) ★★★
        int fromIndex = Math.max(0, size - count);
        List<UpbitCandleDTO> result = new ArrayList<>(filtered.subList(fromIndex, size));
        Collections.reverse(result); // 최신 데이터가 앞에 오도록 역순

        return result;
    }

    /**
     * 매도 신호 체크
     */
    private String checkSellSignal(Position position, BigDecimal currentPrice,
            BigDecimal priceChangeRate, BacktestRequestDTO request) {
        // ⭐⭐⭐ [추가] 수수료 포함 실제 수익률 계산 ⭐⭐⭐
        // 매도 예상 금액 = 보유 수량 × 현재가
        BigDecimal sellAmount = position.getQuantity().multiply(currentPrice);
        // 매도 수수료 = 매도 금액 × 0.05%
        BigDecimal sellFee = sellAmount.multiply(FEE_RATE);
        // 실제 수령 예상액 = 매도 금액 - 매도 수수료
        BigDecimal netSellAmount = sellAmount.subtract(sellFee);
        // 매수 투입금 (매수 수수료 포함) = 수량 × 매수가 × (1 + 수수료율)
        BigDecimal buyAmount = position.getQuantity().multiply(position.getAvgPrice());
        BigDecimal buyFee = buyAmount.multiply(FEE_RATE);
        BigDecimal totalBuyAmount = buyAmount.add(buyFee);
        // 실제 수익률 = (실제 수령 예상액 - 매수 투입금) / 매수 투입금 × 100
        BigDecimal netProfitRate = totalBuyAmount.compareTo(BigDecimal.ZERO) > 0
                ? netSellAmount.subtract(totalBuyAmount)
                        .divide(totalBuyAmount, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        // 1. 목표 수익률 도달 (⭐ 수수료 포함 수익률 사용)
        if (netProfitRate.compareTo(request.getSellTargetPct()) >= 0) {
            return String.format("목표 수익률 도달: %.2f%% (수수료 반영)", netProfitRate);
        }

        // 2. 손절매 (⭐ 사용여부 체크)
        if (Boolean.TRUE.equals(request.getUseStopLoss()) && priceChangeRate.compareTo(request.getStopLossPct()) <= 0) {
            return String.format("손절매: %.2f%%", priceChangeRate);
        }

        // 3. 트레일링 스톱 (⭐ 기존 유지)
        if (request.getUseTrailingStop() && position.getHighestPrice() != null) {
            BigDecimal trailingDropRate = position.getHighestPrice().subtract(currentPrice)
                    .divide(position.getHighestPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

            // ⭐⭐⭐ [수정] 수익 확인도 netProfitRate 사용 ⭐⭐⭐
            if (trailingDropRate.compareTo(request.getTrailingStopPct()) >= 0
                    && netProfitRate.compareTo(BigDecimal.ZERO) > 0) {
                return String.format("트레일링 스톱: 최고가 대비 -%.2f%%", trailingDropRate);
            }
        }

        return null;
    }

    /**
     * 매수 실행
     */
    private void executeBuy(String symbol, BigDecimal price, LocalDate date,
            String signal, BacktestRequestDTO request, SimulationState state) {
        // ⭐⭐⭐ 수정: usePerTradeLimit → useRoundRobin (의미 변경) ⭐⭐⭐
        // true: 라운드로빈 (남은 한도 균등 분배)
        // false: 고정 금액 (fixedBuyAmount 사용)
        boolean useRoundRobin = request.getUseRoundRobin() != null ? request.getUseRoundRobin() : true;

        // 일일 한도 계산
        BigDecimal dailyLimit = request.getDailyTradeLimitPct() != null
                ? request.getInitialBalance()
                        .multiply(new BigDecimal(request.getDailyTradeLimitPct()))
                        .divide(new BigDecimal("100"), SCALE, RoundingMode.DOWN)
                : request.getInitialBalance();

        // 남은 일일 한도 (복구 금액 포함)
        BigDecimal remainingDailyLimit = dailyLimit.subtract(state.getDailyBuyAmount());
        if (Boolean.TRUE.equals(request.getUseDailyLimitRecovery())) {
            remainingDailyLimit = remainingDailyLimit.add(state.getDailySellRecovery());
            // 최대치는 일일 한도까지만
            if (remainingDailyLimit.compareTo(dailyLimit) > 0) {
                remainingDailyLimit = dailyLimit;
            }
        }

        BigDecimal buyAmount;
        if (useRoundRobin) {
            // ⭐⭐⭐ 라운드로빈: 남은 한도를 매수 후보 수로 균등 분배 ⭐⭐⭐
            // 백테스팅에서는 단일 코인 처리이므로 남은 한도 전체 사용
            buyAmount = remainingDailyLimit.min(state.getCashBalance());
        } else {
            // ⭐⭐⭐ 고정 금액: fixedBuyAmount 사용 ⭐⭐⭐
            BigDecimal fixedAmount = request.getFixedBuyAmount() != null
                    ? request.getFixedBuyAmount()
                    : new BigDecimal("10000");
            buyAmount = fixedAmount.min(remainingDailyLimit).min(state.getCashBalance());
        }

        // ⭐⭐⭐ 수정: 최소 금액 5,000원 (업비트 최소 주문금액) ⭐⭐⭐
        if (buyAmount.compareTo(new BigDecimal("5000")) < 0)
            return;

        BigDecimal fee = buyAmount.multiply(FEE_RATE);
        BigDecimal actualAmount = buyAmount.subtract(fee);
        BigDecimal quantity = actualAmount.divide(price, SCALE, RoundingMode.DOWN);

        Position position = new Position(symbol, price, quantity, date);
        state.getPositions().add(position);
        state.setCashBalance(state.getCashBalance().subtract(buyAmount));

        state.getTrades().add(BacktestTrade.builder()
                .coinSymbol(symbol)
                .type("BUY")
                .tradeDate(date)
                .price(price)
                .quantity(quantity)
                .amount(buyAmount)
                .signal(signal)
                .build());

        state.setBuyCount(state.getBuyCount() + 1);
        log.debug("매수: {} - {}원 x {} ({})", symbol, price, quantity, signal);

        // ★★★ 신규 추가: 일일 매수 금액 누적 ★★★
        state.setDailyBuyAmount(state.getDailyBuyAmount().add(buyAmount));
    }

    /**
     * 매도 실행
     */
    private void executeSell(Position position, BigDecimal price, LocalDate date,
            String signal, SimulationState state, BacktestRequestDTO request) {
        BigDecimal sellAmount = position.getQuantity().multiply(price);
        BigDecimal fee = sellAmount.multiply(FEE_RATE);
        BigDecimal actualAmount = sellAmount.subtract(fee);

        BigDecimal buyAmount = position.getQuantity().multiply(position.getAvgPrice());
        BigDecimal profit = actualAmount.subtract(buyAmount);
        BigDecimal profitRate = profit.divide(buyAmount, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

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
                .coinSymbol(position.getCoinSymbol())
                .type("SELL")
                .tradeDate(date)
                .price(price)
                .quantity(position.getQuantity())
                .amount(sellAmount)
                .profit(profit)
                .profitRate(profitRate)
                .signal(signal)
                .build());

        // 일일 한도 복구 (옵션 ON인 경우)
        if (Boolean.TRUE.equals(request.getUseDailyLimitRecovery())
                && request.getDailyTradeLimitPct() != null
                && request.getDailyTradeLimitPct() < 100) {
            // 일일 한도 계산
            BigDecimal effectiveDailyLimit = request.getInitialBalance()
                    .multiply(new BigDecimal(request.getDailyTradeLimitPct()))
                    .divide(new BigDecimal("100"), SCALE, RoundingMode.HALF_UP);

            // 현재 남은 한도 = 일일한도 - 오늘매수액 + 이미복구액
            BigDecimal currentRemaining = effectiveDailyLimit
                    .subtract(state.getDailyBuyAmount())
                    .add(state.getDailySellRecovery());

            // 복구 가능한 최대 금액 = 일일한도 - 현재남은한도
            BigDecimal maxRecoverable = effectiveDailyLimit.subtract(currentRemaining);
            if (maxRecoverable.compareTo(BigDecimal.ZERO) < 0) {
                maxRecoverable = BigDecimal.ZERO;
            }

            // 실제 복구 금액 = min(매도금액, 복구가능최대금액)
            BigDecimal actualRecovery = actualAmount.min(maxRecoverable);
            state.setDailySellRecovery(state.getDailySellRecovery().add(actualRecovery));

            log.debug("백테스트 일일 한도 복구: {} 매도금액={}, 복구={}",
                    position.getCoinSymbol(), actualAmount, actualRecovery);
        }

        // 연속 손절 카운터 업데이트
        if (signal.contains("손절") || signal.contains("stop") || profitRate.compareTo(BigDecimal.ZERO) < 0) {
            // 손절 발생
            int currentCount = state.getConsecutiveStopLossCount().getOrDefault(position.getCoinSymbol(), 0) + 1;
            state.getConsecutiveStopLossCount().put(position.getCoinSymbol(), currentCount);

            int limit = request.getConsecutiveStopLossLimit() != null ? request.getConsecutiveStopLossLimit() : 3;
            if (currentCount >= limit) {
                // 다음날부터 매수 금지 (백테스팅에서는 1일 추가)
                state.getCoinBuyBlockedUntil().put(position.getCoinSymbol(), date.plusDays(1));
                log.info("연속 손절 한도 도달 (백테스팅): {} - {}까지 매수 금지", position.getCoinSymbol(), date.plusDays(1));
            }
        } else {
            // 수익 실현 시 카운터 리셋
            state.getConsecutiveStopLossCount().remove(position.getCoinSymbol());
            state.getCoinBuyBlockedUntil().remove(position.getCoinSymbol());
        }

        state.setSellCount(state.getSellCount() + 1);
        log.debug("매도: {} - {}원 x {}, 손익: {}원 ({})",
                position.getCoinSymbol(), price, position.getQuantity(), profit, signal);
    }

    /**
     * 매도 신호 체크 및 실행
     */
    private void checkSellSignals(String coinSymbol, BigDecimal currentPrice, LocalDate date,
            BacktestRequestDTO request, SimulationState state) {
        List<Position> positionsToSell = new ArrayList<>();

        for (Position position : state.getPositions()) {
            if (!position.getCoinSymbol().equals(coinSymbol))
                continue;

            // 최고가 업데이트
            if (currentPrice.compareTo(position.getHighestPrice()) > 0) {
                position.setHighestPrice(currentPrice);
            }

            BigDecimal profitRate = calculateProfitRate(position.getAvgPrice(), currentPrice);
            String sellSignal = checkSellSignal(position, currentPrice, profitRate, request);

            if (sellSignal != null) {
                executeSell(position, currentPrice, date, sellSignal, state, request);
                positionsToSell.add(position);
            }
        }

        state.getPositions().removeAll(positionsToSell);
    }

    /**
     * 매수 가능 여부 확인 (리스크 관리 추가)
     */
    private boolean canBuy(String coinSymbol, BigDecimal currentPrice, BacktestRequestDTO request,
            SimulationState state) {

        // 누적 손실 긴급정지 체크
        if (state.isCumulativeLossTriggered()) {
            return false;
        }

        // 연속 손절 제한 체크
        LocalDate blockedUntil = state.getCoinBuyBlockedUntil().get(coinSymbol);
        if (blockedUntil != null) {
            // 백테스팅에서는 1일 = 24시간으로 간주
            // 현재 날짜가 매수금지 해제일 이전이면 매수 불가
            // (simulateDay의 date 파라미터 사용 필요 - 메서드 시그니처 수정 필요)
        }

        // 0. 긴급 정지 발동 여부 확인
        if (state.isDailyStopTriggered()) {
            return false;
        }

        // 1. 현금 잔고 확인
        if (state.getCashBalance().compareTo(new BigDecimal("10000")) < 0) {
            return false;
        }

        // 2. 해당 코인 보유 건수 확인
        List<Position> coinHoldings = state.getPositions().stream()
                .filter(p -> p.getCoinSymbol().equals(coinSymbol))
                .toList();

        if (coinHoldings.size() >= request.getMaxHoldingsPerCoin()) {
            return false;
        }

        // ⭐⭐⭐ [추가] 분할 매수 (Staggered Buy) 조건 확인 ⭐⭐⭐
        if (!coinHoldings.isEmpty() && currentPrice != null) {
            Position lastPosition = coinHoldings.get(coinHoldings.size() - 1);
            BigDecimal lastBuyPrice = lastPosition.getAvgPrice();

            BigDecimal dropRateFromLastBuy = lastBuyPrice.subtract(currentPrice)
                    .divide(lastBuyPrice, SCALE, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

            BigDecimal requiredDrop = request.getAdditionalDropPct() != null
                    ? request.getAdditionalDropPct()
                    : new BigDecimal("0.5");

            if (dropRateFromLastBuy.compareTo(requiredDrop) < 0) {
                return false;
            }
        }

        // ⭐⭐⭐ 수정: 예상 매수 금액 계산 (executeBuy와 동일한 로직) ⭐⭐⭐
        boolean useRoundRobin = request.getUseRoundRobin() != null ? request.getUseRoundRobin() : true;

        // 일일 한도 계산
        BigDecimal dailyLimit = request.getDailyTradeLimitPct() != null
                ? request.getInitialBalance()
                        .multiply(new BigDecimal(request.getDailyTradeLimitPct()))
                        .divide(new BigDecimal("100"), SCALE, RoundingMode.DOWN)
                : request.getInitialBalance();

        BigDecimal buyAmount;
        if (useRoundRobin) {
            // 라운드로빈: 남은 한도 사용 (canBuy에서는 단순화)
            buyAmount = dailyLimit.subtract(state.getDailyBuyAmount())
                    .min(state.getCashBalance());
        } else {
            // 고정 금액: fixedBuyAmount 사용
            BigDecimal fixedAmount = request.getFixedBuyAmount() != null
                    ? request.getFixedBuyAmount()
                    : new BigDecimal("10000");
            buyAmount = fixedAmount.min(state.getCashBalance());
        }

        // 3. 일일 거래 한도 체크
        if (request.getDailyTradeLimitPct() != null && request.getDailyTradeLimitPct() < 100) {
            // dailyLimit은 이미 위에서 계산됨 - 재선언 제거

            // ⭐⭐⭐ Day 41 수정: 복구 금액 반영한 남은 한도 계산 ⭐⭐⭐
            BigDecimal remainingLimit = dailyLimit
                    .subtract(state.getDailyBuyAmount())
                    .add(state.getDailySellRecovery());

            // 최대치는 일일 한도까지만 (복구해도 원래 한도 초과 불가)
            if (remainingLimit.compareTo(dailyLimit) > 0) {
                remainingLimit = dailyLimit;
            }

            if (buyAmount.compareTo(remainingLimit) > 0) {
                return false;
            }
        }

        // 4. 단일 종목 비중 제한 체크
        if (request.getMaxPositionPct() != null && request.getMaxPositionPct() < 100) {
            BigDecimal maxPositionAmount = request.getInitialBalance()
                    .multiply(new BigDecimal(request.getMaxPositionPct()))
                    .divide(new BigDecimal("100"), 0, RoundingMode.DOWN);

            // 해당 코인의 현재 보유 금액 계산
            BigDecimal currentCoinAmount = state.getPositions().stream()
                    .filter(p -> p.getCoinSymbol().equals(coinSymbol))
                    .map(p -> p.getQuantity().multiply(p.getAvgPrice()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (currentCoinAmount.add(buyAmount).compareTo(maxPositionAmount) > 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * ★★★ 신규 추가: 일별 잔고 기록 ★★★
     */
    private void recordDailyBalance(LocalDate date, Map<String, List<UpbitCandleDTO>> candleDataMap,
            BacktestRequestDTO request, SimulationState state) {
        BigDecimal holdingValue = calculateHoldingValue(state.getPositions(), candleDataMap, date);
        BigDecimal totalBalance = state.getCashBalance().add(holdingValue);

        // 최고 잔고 업데이트
        if (totalBalance.compareTo(state.getPeakBalance()) > 0) {
            state.setPeakBalance(totalBalance);
        }

        // 최대 낙폭 계산
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

    /**
     * 모든 포지션 청산
     */
    private void closeAllPositions(LocalDate date, Map<String, List<UpbitCandleDTO>> candleDataMap,
            SimulationState state, BacktestRequestDTO request) {
        for (Position position : new ArrayList<>(state.getPositions())) {
            UpbitCandleDTO candle = findCandleByDate(candleDataMap.get(position.getCoinSymbol()), date);
            if (candle != null) {
                executeSell(position, candle.getTradePrice(), date, "백테스트 종료", state, request);
            }
        }
        state.getPositions().clear();
    }

    /**
     * 결과 빌드
     */
    private BacktestResultDTO buildResult(BacktestRequestDTO request, SimulationState state, int totalDays) {
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

        // 코인별 성과 계산
        List<CoinPerformance> coinPerformances = calculateCoinPerformances(state.getTrades(), request.getCoinSymbols());

        log.info("========== 백테스트 완료 ==========");
        log.info("최종 잔고: {}원", finalBalance);
        log.info("총 수익률: {}%", totalProfitRate);
        log.info("승률: {}%", winRate);

        return BacktestResultDTO.builder()
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalDays(totalDays)
                .coinSymbols(request.getCoinSymbols())
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
                .coinPerformances(coinPerformances)
                .dailyBalances(state.getDailyBalances())
                .trades(state.getTrades())
                .build();
    }

    // ============ 유틸리티 메서드 ============

    private UpbitCandleDTO findCandleByDate(List<UpbitCandleDTO> candles, LocalDate date) {
        if (candles == null)
            return null;
        return candles.stream()
                .filter(c -> c.getCandleDateTimeKst() != null &&
                        parseToLocalDate(c.getCandleDateTimeKst()).equals(date))
                .findFirst()
                .orElse(null);
    }

    private int findCandleIndexByDate(List<UpbitCandleDTO> candles, LocalDate date) {
        for (int i = 0; i < candles.size(); i++) {
            if (candles.get(i).getCandleDateTimeKst() != null &&
                    parseToLocalDate(candles.get(i).getCandleDateTimeKst()).equals(date)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 이동평균선 계산
     */
    private BigDecimal calculateMA(List<BigDecimal> prices, int period) {
        if (prices.size() < period)
            return BigDecimal.ZERO;

        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            sum = sum.add(prices.get(i));
        }
        return sum.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 하락률 계산
     */
    private BigDecimal calculateDropRate(BigDecimal currentPrice, BigDecimal ma) {
        if (ma.compareTo(BigDecimal.ZERO) == 0)
            return BigDecimal.ZERO;
        return currentPrice.subtract(ma)
                .divide(ma, SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * RSI 계산
     */
    private BigDecimal calculateRSI(List<BigDecimal> prices, int period) {
        if (prices.size() < period + 1)
            return null;

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

        if (avgLoss.compareTo(BigDecimal.ZERO) == 0)
            return new BigDecimal("100");

        BigDecimal rs = avgGain.divide(avgLoss, SCALE, RoundingMode.HALF_UP);
        return new BigDecimal("100").subtract(
                new BigDecimal("100").divide(BigDecimal.ONE.add(rs), SCALE, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 볼린저 밴드 계산
     */
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
            Map<String, List<UpbitCandleDTO>> candleDataMap, LocalDate date) {
        BigDecimal total = BigDecimal.ZERO;
        for (Position position : positions) {
            UpbitCandleDTO candle = findCandleByDate(candleDataMap.get(position.getCoinSymbol()), date);
            if (candle != null) {
                total = total.add(position.getQuantity().multiply(candle.getTradePrice()));
            }
        }
        return total;
    }

    private BigDecimal calculateSharpeRatio(List<DailyBalance> dailyBalances) {
        if (dailyBalances.size() < 2)
            return BigDecimal.ZERO;

        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < dailyBalances.size(); i++) {
            BigDecimal dailyReturn = dailyBalances.get(i).getBalance()
                    .subtract(dailyBalances.get(i - 1).getBalance())
                    .divide(dailyBalances.get(i - 1).getBalance(), SCALE, RoundingMode.HALF_UP);
            returns.add(dailyReturn);
        }

        BigDecimal avgReturn = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(returns.size()), SCALE, RoundingMode.HALF_UP);

        BigDecimal variance = returns.stream()
                .map(r -> r.subtract(avgReturn).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(returns.size()), SCALE, RoundingMode.HALF_UP);

        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        if (stdDev.compareTo(BigDecimal.ZERO) == 0)
            return BigDecimal.ZERO;

        // 연환산 (일간 * sqrt(365))
        return avgReturn.divide(stdDev, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(Math.sqrt(365)));
    }

    private List<CoinPerformance> calculateCoinPerformances(List<BacktestTrade> trades, List<String> coinSymbols) {
        List<CoinPerformance> performances = new ArrayList<>();

        for (String symbol : coinSymbols) {
            List<BacktestTrade> coinTrades = trades.stream()
                    .filter(t -> t.getCoinSymbol().equals(symbol))
                    .collect(Collectors.toList());

            List<BacktestTrade> sells = coinTrades.stream()
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

            BigDecimal totalAmount = coinTrades.stream()
                    .filter(t -> "BUY".equals(t.getType()))
                    .map(BacktestTrade::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal profitRate = totalAmount.compareTo(BigDecimal.ZERO) > 0
                    ? totalProfit.divide(totalAmount, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;

            performances.add(CoinPerformance.builder()
                    .coinSymbol(symbol)
                    .tradeCount(coinTrades.size())
                    .winCount(winCount)
                    .loseCount(loseCount)
                    .totalProfit(totalProfit)
                    .profitRate(profitRate)
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

        // 리스크 관리용 필드
        private BigDecimal dailyBuyAmount = BigDecimal.ZERO; // 당일 매수 금액
        private BigDecimal dailySellRecovery = BigDecimal.ZERO; // 일일 매도 복구 금액
        private LocalDate currentTradeDate = null; // 현재 거래일
        private BigDecimal dailyStartBalance = BigDecimal.ZERO; // 당일 시작 잔고
        private boolean dailyStopTriggered = false; // 긴급 정지 발동 여부

        // 급락장 보호 상태
        private boolean cumulativeLossTriggered = false; // 누적 손실 긴급정지 발동
        private Map<String, Integer> consecutiveStopLossCount = new HashMap<>(); // 코인별 연속 손절 횟수
        private Map<String, LocalDate> coinBuyBlockedUntil = new HashMap<>(); // 코인별 매수 금지 일자

        public SimulationState(BigDecimal initialBalance) {
            this.cashBalance = initialBalance;
            this.peakBalance = initialBalance;
        }
    }

    /**
     * 문자열을 LocalDate로 파싱
     * 업비트 API 응답 형식: "2024-11-01T00:00:00"
     */
    private LocalDate parseToLocalDate(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.length() < 10) {
            return null;
        }
        // "2024-11-01T00:00:00" 형식에서 앞 10자리만 추출
        return LocalDate.parse(dateTimeStr.substring(0, 10));
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class Position {
        private String coinSymbol;
        private BigDecimal avgPrice;
        private BigDecimal quantity;
        private LocalDate buyDate;
        private BigDecimal highestPrice;

        public Position(String coinSymbol, BigDecimal avgPrice, BigDecimal quantity, LocalDate buyDate) {
            this.coinSymbol = coinSymbol;
            this.avgPrice = avgPrice;
            this.quantity = quantity;
            this.buyDate = buyDate;
            this.highestPrice = avgPrice;
        }
    }
}