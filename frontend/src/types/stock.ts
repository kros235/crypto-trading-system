// ===================================================
// Phase 2: 주식/ETF 관련 TypeScript 타입 정의
// ===================================================

// 주식/ETF 정보
export interface StockInfo {
  stockCode: string
  stockName: string
  market: string          // KRX, KOSDAQ
  etfType: string         // LEVERAGE, INVERSE, NORMAL, STOCK
  underlyingIndex?: string
  expenseRatio?: number
  isActive: boolean
  lastUpdated?: string
}

// 주식 거래 설정
export interface StockTradingSetting {
  stockCodes: string[]
  basePeriod: number
  buyThresholdPct: number
  sellTargetPct: number
  stopLossPct: number
  maxHoldingsPerStock: number
  dailyLimitAmount: number
  useTrailingStop: boolean
  trailingStopPct: number
  // 기술적 지표
  rsiPeriod: number
  rsiBuyThreshold: number
  rsiSellThreshold: number
  bbPeriod: number
  bbMultiplier: number
  volumeThreshold: number
  // 리스크 관리
  dailyTradeLimitPct: number
  maxPositionPct: number
  dailyStopLossPct: number
  useMarketTrendFilter: boolean
  cumulativeLossLimitPct: number
  consecutiveStopLossLimit: number
  fixedBuyAmount: number
  useDailyLimitRecovery: boolean
  useRoundRobin: boolean
  // Phase 2 전용
  maxHoldingDays: number
  // KIS API 키
  kisAppKey?: string
  kisAppSecret?: string
  kisAccountNo?: string
  kisMockMode?: boolean
  hasKisApiKey?: boolean
}

// 주식 거래 설정 요청
export interface StockTradingSettingRequest {
  stockCodes: string[]
  basePeriod: number
  buyThresholdPct: number
  sellTargetPct: number
  stopLossPct: number
  maxHoldingsPerStock: number
  dailyLimitAmount: number
  useTrailingStop: boolean
  trailingStopPct: number
  rsiPeriod: number
  rsiBuyThreshold: number
  rsiSellThreshold: number
  bbPeriod: number
  bbMultiplier: number
  volumeThreshold: number
  dailyTradeLimitPct: number
  maxPositionPct: number
  dailyStopLossPct: number
  useMarketTrendFilter: boolean
  cumulativeLossLimitPct: number
  consecutiveStopLossLimit: number
  fixedBuyAmount: number
  useDailyLimitRecovery: boolean
  useRoundRobin: boolean
  maxHoldingDays: number
  kisAppKey?: string
  kisAppSecret?: string
  kisAccountNo?: string
  kisMockMode?: boolean
}

// KIS API 키 등록 요청
export interface KisApiKeyRequest {
  appKey: string
  appSecret: string
  accountNo: string
}