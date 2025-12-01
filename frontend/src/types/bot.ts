// 기술적 지표 결과
export interface IndicatorResult {
  market: string;
  currentPrice: number;
  ma7: number | null;
  ma14: number | null;
  ma20: number | null;
  ma30: number | null;
  rsi: number | null;
  bbUpper: number | null;
  bbMiddle: number | null;
  bbLower: number | null;
  volumeRatio: number | null;
  calculatedAt: string;
}

// 거래 신호
export interface TradingSignal {
  market: string;
  signalType: 'BUY' | 'SELL' | 'HOLD' | 'STOP_LOSS' | 'TRAILING_STOP';
  strength: 'STRONG' | 'MODERATE' | 'WEAK';
  currentPrice: number;
  targetPrice: number | null;
  stopLossPrice: number | null;
  reason: string;
  conditionsMet: number;
  totalConditions: number;
  detectedAt: string;
}

// 봇 실행 결과
export interface BotExecutionResult {
  userId: string;
  status: 'SUCCESS' | 'SKIP' | 'ERROR';
  message: string | null;
  buyCount: number;
  sellCount: number;
  totalBuyAmount: number;
  totalSellAmount: number;
  totalProfitLoss: number;
  buyDetails: string[];
  sellDetails: string[];
  skipped: string[];
  errors: string[];
}

// 봇 상태
export interface BotStatus {
  isRunning: boolean;
  lastExecutedAt: string | null;
  nextExecutionAt: string | null;
  activeUsers: number;
  todayBuyCount: number;
  todaySellCount: number;
  todayProfitLoss: number;
}

// 일일 리포트
export interface DailyReport {
  userId: string;
  reportDate: string;
  buyCount: number;
  sellCount: number;
  totalBuyAmount: number;
  totalSellAmount: number;
  realizedProfit: number;
  unrealizedProfit: number;
  totalProfit: number;
  profitRate: number;
  holdingCount: number;
  totalHoldingValue: number;
  totalInvestment: number;
  coinSummaries: CoinSummary[];
}

export interface CoinSummary {
  coinSymbol: string;
  holdingCount: number;
  totalQuantity: number;
  averagePrice: number;
  currentPrice: number;
  profitLoss: number;
  profitRate: number;
}

// 알림 상태
export interface NotificationStatus {
  discordEnabled: boolean;
  emailEnabled: boolean;
  telegramEnabled: boolean;
  message: string;
}