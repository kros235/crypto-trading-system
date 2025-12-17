// 백테스트 요청
export interface BacktestRequest {
  coinSymbols: string[];
  startDate: string;
  endDate: string;
  initialBalance: number;
  basePeriod?: number;
  buyThresholdPct?: number;
  sellTargetPct?: number;
  stopLossPct?: number;
  maxHoldingsPerCoin?: number;
  useTrailingStop?: boolean;
  trailingStopPct?: number;
  rsiPeriod?: number;
  rsiBuyThreshold?: number;
  rsiSellThreshold?: number;
  bbPeriod?: number;
  bbMultiplier?: number;
  volumeThreshold?: number;
  // ★★★ 신규 추가: 리스크 관리 설정 ★★★
  dailyTradeLimitPct?: number;   // 일일 거래 한도 (%)
  maxPositionPct?: number;       // 단일 종목 비중 제한 (%)
  dailyStopLossPct?: number;     // 긴급 정지 조건 (%)
}

// 백테스트 결과
export interface BacktestResult {
  // 기본 정보
  startDate: string;
  endDate: string;
  totalDays: number;
  coinSymbols: string[];
  
  // 수익 요약
  initialBalance: number;
  finalBalance: number;
  totalProfit: number;
  totalProfitRate: number;
  maxDrawdown: number;
  sharpeRatio: number;
  
  // 거래 통계
  totalTrades: number;
  buyCount: number;
  sellCount: number;
  winCount: number;
  loseCount: number;
  winRate: number;
  avgProfit: number;
  avgLoss: number;
  profitFactor: number;
  
  // 코인별 성과
  coinPerformances: CoinPerformance[];
  
  // 일별 자산 변동
  dailyBalances: DailyBalance[];
  
  // 거래 내역
  trades: BacktestTrade[];
}

export interface CoinPerformance {
  coinSymbol: string;
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

export interface BacktestTrade {
  coinSymbol: string;
  type: 'BUY' | 'SELL';
  tradeDate: string;
  price: number;
  quantity: number;
  amount: number;
  profit?: number;
  profitRate?: number;
  signal: string;
}

// 백테스트 가능 코인
export interface AvailableCoin {
  symbol: string;
  name: string;
  rank?: number;                        // ★★★ 신규: 시가총액 순위 ★★★
  accTradePrice24h?: number;      // ★★★ 신규: 24시간 거래대금 ★★★
}

export interface AvailableCoinsResponse {
  coins: AvailableCoin[];
  maxPeriodDays: number;
  minInitialBalance: number;
}