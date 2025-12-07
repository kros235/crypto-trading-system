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
    
    private static final BigDecimal FEE_RATE = new BigDecimal("0.0005");  // 0.05%
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
                request.getEndDate()
        );
        
        // 4. 날짜별 시뮬레이션 실행
        LocalDate currentDate = request.getStartDate();
        while (!currentDate.isAfter(request.getEndDate())) {
            simulateDay(currentDate, candleDataMap, request, state);
            currentDate = currentDate.plusDays(1);
        }
        
        // 5. 남은 포지션 정리 (마지막 날 강제 매도)
        closeAllPositions(request.getEndDate(), candleDataMap, state);
        
        // 6. 결과 계산 및 반환
        return buildResult(request, state, totalDays);
    }

    /**
     * 캔들 데이터 조회
     */
    private Map<String, List<UpbitCandleDTO>> fetchCandleData(
            List<String> coinSymbols, LocalDate startDate, LocalDate endDate) {
        
        Map<String, List<UpbitCandleDTO>> result = new HashMap<>();
        int count = (int) ChronoUnit.DAYS.between(startDate, endDate) + 50; // 지표 계산용 여유분
        
        for (String symbol : coinSymbols) {
            try {
                List<UpbitCandleDTO> candles = upbitApiService.getDayCandles(symbol, count);
                
                // 날짜순 정렬 (오래된 순)
                candles.sort(Comparator.comparing(UpbitCandleDTO::getCandleDateTimeKst));
                
                result.put(symbol, candles);
                log.info("캔들 데이터 조회: {} - {}개", symbol, candles.size());
                
                // API 호출 제한 방지
                Thread.sleep(100);
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
        
        for (String coinSymbol : request.getCoinSymbols()) {
            List<UpbitCandleDTO> candles = candleDataMap.get(coinSymbol);
            UpbitCandleDTO todayCandle = findCandleByDate(candles, date);
            
            if (todayCandle == null) continue;
        
            BigDecimal currentPrice = todayCandle.getTradePrice();
        
            // 1. 매도 체크 (보유 포지션)
            checkSellSignals(coinSymbol, currentPrice, date, request, state);
            
            // 2. 매수 체크
            if (canBuy(coinSymbol, request, state)) {
                // ★★★ 수정: 사용자 설정값으로 매수 신호 체크 ★★★
                if (checkBuySignal(coinSymbol, candles, date, request)) {
                    executeBuy(coinSymbol, currentPrice, date, "매수 신호", request, state);
                }
            }
        }
    
        // 일별 잔고 기록
        recordDailyBalance(date, candleDataMap, request, state);
    }

    /**
     * ★★★ 신규: 매수 신호 체크 (사용자 설정 적용) ★★★
     */
    private boolean checkBuySignal(String coinSymbol, List<UpbitCandleDTO> candles, 
                                    LocalDate date, BacktestRequestDTO request) {
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
     */
    private List<UpbitCandleDTO> getHistoricalCandles(List<UpbitCandleDTO> candles, 
                                                       LocalDate targetDate, int count) {
        return candles.stream()
                .filter(c -> !parseToLocalDate(c.getCandleDateTimeKst()).isAfter(targetDate))
                .limit(count)
                .toList();
    }

    /**
     * 매도 신호 체크
     */
    private String checkSellSignal(Position position, BigDecimal currentPrice,
                                    BigDecimal profitRate, BacktestRequestDTO request) {
        // 1. 목표 수익률 도달
        if (profitRate.compareTo(request.getSellTargetPct()) >= 0) {
            return String.format("목표 수익률 도달: %.2f%%", profitRate);
        }
        
        // 2. 손절매
        if (profitRate.compareTo(request.getStopLossPct()) <= 0) {
            return String.format("손절매: %.2f%%", profitRate);
        }
        
        // 3. 트레일링 스톱
        if (request.getUseTrailingStop() && position.getHighestPrice() != null) {
            BigDecimal trailingDropRate = position.getHighestPrice().subtract(currentPrice)
                    .divide(position.getHighestPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            
            if (trailingDropRate.compareTo(request.getTrailingStopPct()) >= 0 
                    && profitRate.compareTo(BigDecimal.ZERO) > 0) {
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
        // 매수 금액: 초기 자본의 10% 또는 남은 현금
        BigDecimal buyAmount = request.getInitialBalance()
                .multiply(new BigDecimal("0.1"))
                .min(state.getCashBalance());
        
        if (buyAmount.compareTo(new BigDecimal("10000")) < 0) return;  // 최소 1만원
        
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
    }

    /**
     * 매도 실행
     */
    private void executeSell(Position position, BigDecimal price, LocalDate date,
                              String signal, SimulationState state) {
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
        
        state.setSellCount(state.getSellCount() + 1);
        log.debug("매도: {} - {}원 x {}, 손익: {}원 ({})", 
                position.getCoinSymbol(), price, position.getQuantity(), profit, signal);
    }

    /**
     * ★★★ 신규 추가: 매도 신호 체크 및 실행 ★★★
     */
    private void checkSellSignals(String coinSymbol, BigDecimal currentPrice, LocalDate date,
                                   BacktestRequestDTO request, SimulationState state) {
        List<Position> positionsToSell = new ArrayList<>();
        
        for (Position position : state.getPositions()) {
            if (!position.getCoinSymbol().equals(coinSymbol)) continue;
            
            // 최고가 업데이트
            if (currentPrice.compareTo(position.getHighestPrice()) > 0) {
                position.setHighestPrice(currentPrice);
            }
            
            BigDecimal profitRate = calculateProfitRate(position.getAvgPrice(), currentPrice);
            String sellSignal = checkSellSignal(position, currentPrice, profitRate, request);
            
            if (sellSignal != null) {
                executeSell(position, currentPrice, date, sellSignal, state);
                positionsToSell.add(position);
            }
        }
        
        state.getPositions().removeAll(positionsToSell);
    }

    /**
     * ★★★ 신규 추가: 매수 가능 여부 확인 ★★★
     */
    private boolean canBuy(String coinSymbol, BacktestRequestDTO request, SimulationState state) {
        // 현금 잔고 확인
        if (state.getCashBalance().compareTo(new BigDecimal("10000")) < 0) {
            return false;
        }
        
        // 해당 코인 보유 건수 확인
        long holdingCount = state.getPositions().stream()
                .filter(p -> p.getCoinSymbol().equals(coinSymbol))
                .count();
        
        return holdingCount < request.getMaxHoldingsPerCoin();
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
                                    SimulationState state) {
        for (Position position : new ArrayList<>(state.getPositions())) {
            UpbitCandleDTO candle = findCandleByDate(candleDataMap.get(position.getCoinSymbol()), date);
            if (candle != null) {
                executeSell(position, candle.getTradePrice(), date, "백테스트 종료", state);
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
                    ? new BigDecimal("999") : BigDecimal.ZERO;
        
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
        if (candles == null) return null;
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
        if (prices.size() < period) return BigDecimal.ZERO;
    
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
        if (ma.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return currentPrice.subtract(ma)
                .divide(ma, SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * RSI 계산
     */
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
                new BigDecimal("100").divide(BigDecimal.ONE.add(rs), SCALE, RoundingMode.HALF_UP)
        ).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 볼린저 밴드 계산
     */
    private BigDecimal[] calculateBollingerBands(List<BigDecimal> prices, int period, int multiplier) {
        BigDecimal ma = calculateMA(prices, period);
        
        if (prices.size() < period) {
            return new BigDecimal[]{ma, ma, ma};
        }
    
        BigDecimal sumSquaredDiff = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            BigDecimal diff = prices.get(i).subtract(ma);
            sumSquaredDiff = sumSquaredDiff.add(diff.multiply(diff));
        }
    
        BigDecimal variance = sumSquaredDiff.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
        BigDecimal deviation = stdDev.multiply(BigDecimal.valueOf(multiplier));
        
        return new BigDecimal[]{
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
        if (dailyBalances.size() < 2) return BigDecimal.ZERO;
        
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
        
        if (stdDev.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        
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