// ===================================================
// Phase 2 Day 62: 주식/ETF 백테스트 타입
// Phase 1 types/backtest.ts (coinSymbols 기준) 재사용 구조
// ===================================================

// 백테스트 요청
export interface StockBacktestRequest {
  stockCodes: string[];
  startDate: string;
  endDate: string;
  initialBalance: number;
  basePeriod?: number;
  buyThresholdPct?: number;
  sellTargetPct?: number;
  stopLossPct?: number;
  maxHoldingsPerStock?: number;
  useTrailingStop?: boolean;
  trailingStopPct?: number;
  rsiPeriod?: number;
  rsiBuyThreshold?: number;
  rsiSellThreshold?: number;
  bbPeriod?: number;
  bbMultiplier?: number;
  volumeThreshold?: number;
  // ⭐ [Day 62 신규] 레버리지 ETF decay 방지: 최대 보유 거래일
  maxHoldingDays?: number;
  // 리스크 관리 설정
  dailyTradeLimitPct?: number;
  maxPositionPct?: number;
  dailyStopLossPct?: number;
  // 급락장 보호 기능 (useMarketTrendFilter는 Day 62 기준 백엔드에서 no-op 처리됨)
  useMarketTrendFilter?: boolean
  cumulativeLossLimitPct?: number
  consecutiveStopLossLimit?: number
  fixedBuyAmount?: number
  useDailyLimitRecovery?: boolean
  useRoundRobin?: boolean
  additionalDropPct?: number
  useStopLoss?: boolean
}

// 백테스트 결과
export interface StockBacktestResult {
  startDate: string;
  endDate: string;
  totalDays: number;
  stockCodes: string[];

  initialBalance: number;
  finalBalance: number;
  totalProfit: number;
  totalProfitRate: number;
  maxDrawdown: number;
  sharpeRatio: number;

  totalTrades: number;
  buyCount: number;
  sellCount: number;
  winCount: number;
  loseCount: number;
  winRate: number;
  avgProfit: number;
  avgLoss: number;
  profitFactor: number;

  stockPerformances: StockPerformance[];
  dailyBalances: DailyBalance[];
  trades: StockBacktestTrade[];
}

export interface StockPerformance {
  stockCode: string;
  tradeCount: number;
  winCount: number;
  loseCount: number;
  totalProfit: number;
  profitRate: number;
  avgHoldingDays?: number;
}

export interface DailyBalance {
  date: string;
  balance: number;
  profitRate: number;
  holdingValue: number;
}

export interface StockBacktestTrade {
  stockCode: string;
  type: 'BUY' | 'SELL';
  tradeDate: string;
  price: number;
  quantity: number;
  amount: number;
  profit?: number;
  profitRate?: number;
  signal: string;
  holdingDays?: number;
}

// 백테스트 가능 종목
export interface AvailableStock {
  stockCode: string;
  stockName: string;
  market: string;
  etfType: string;
  underlyingIndex?: string;
  expenseRatio?: number;
  isActive: boolean;
}

export interface AvailableStocksResponse {
  stocks: AvailableStock[];
  maxPeriodDays: number;
  minInitialBalance: number;
  totalCount: number;
}