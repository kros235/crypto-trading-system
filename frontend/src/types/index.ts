// 사용자 정보 타입
export interface User {
  userId: string
  email: string
  discordUserId?: string
  phone?: string
  role: string
  joinDate: string
  lastLogin?: string
  isActive: boolean
  hasApiKey: boolean
  // IP 화이트리스트 필드 추가
  allowedIps?: string[]
  ipWhitelistEnabled?: boolean
}

// 로그인 요청 타입
export interface LoginRequest {
  userId: string
  password: string
}

// 회원가입 요청 타입
export interface SignupRequest {
  userId: string
  password: string
  email: string
  phone?: string
}

// 인증 응답 타입
export interface AuthResponse {
  token: string
  userId: string
  email: string
  role: string
  message: string
}

// 코인 정보 타입
export interface CoinInfo {
  symbol: string
  nameKr: string
  nameEn: string
  isActive: boolean
  marketCapRank: number
  lastUpdated: string
}

// 코인 시세 타입
export interface CoinTicker {
  market: string
  tradePrice: number
  changeRate: number
  change: 'RISE' | 'EVEN' | 'FALL'
  accTradePrice24h: number
  highPrice: number
  lowPrice: number
}

// API 에러 타입
export interface ApiError {
  error: string
}

// 프로필 수정 요청 타입
export interface UpdateProfileRequest {
  email?: string
  phone?: string
  password?: string
  discordUserId?: string
}

// API 키 저장 요청 타입
export interface SaveApiKeysRequest {
  accessKey: string
  secretKey: string
}

// 거래 설정 타입
export interface TradingSetting {
  id?: number
  coinSymbols: string[]
  basePeriod: number
  buyThresholdPct: number
  sellTargetPct: number
  stopLossPct: number
  maxHoldingsPerCoin: number
  dailyLimitAmount: number
  useAiAnalysis: boolean
  useTrailingStop: boolean
  trailingStopPct: number
  rsiPeriod: number
  rsiBuyThreshold: number
  rsiSellThreshold: number
  bbPeriod: number
  bbMultiplier: number
  volumeThreshold: number
  useMarketTrendFilter?: boolean
  cumulativeLossLimitPct?: number
  consecutiveStopLossLimit?: number
}

// 거래 설정 요청 타입
export interface TradingSettingRequest {
  coinSymbols: string[]
  basePeriod: number
  buyThresholdPct: number
  sellTargetPct: number
  stopLossPct: number
  maxHoldingsPerCoin: number
  dailyLimitAmount: number
  useAiAnalysis: boolean
  useTrailingStop: boolean
  trailingStopPct: number
  rsiPeriod: number
  rsiBuyThreshold: number
  rsiSellThreshold: number
  bbPeriod: number
  bbMultiplier: number
  volumeThreshold: number
}

// 거래 내역 타입
export interface Transaction {
  transactionId: number
  userId: string
  coinSymbol: string
  type: 'BUY' | 'SELL'
  quantity: number
  price: number
  fee: number
  totalAmount: number
  createdAt: string
  soldAt?: string
  soldPrice?: number
  profitLoss?: number
  profitLossPct?: number
  targetSellPrice?: number
  stopLossPrice?: number
  status: 'HOLDING' | 'SOLD' | 'CANCELLED'
  note?: string
  currentPrice?: number
  currentProfitLoss?: number
  currentProfitLossPct?: number
}

// 거래 생성 요청 타입
export interface CreateTransactionRequest {
  coinSymbol: string
  type: 'BUY' | 'SELL'
  quantity: number
  price: number
  totalAmount: number
  targetSellPrice?: number
  stopLossPrice?: number
  note?: string
}

// 매도 요청 타입
export interface SellTransactionRequest {
  soldPrice: number
}

// 대시보드 통계 타입
export interface DashboardStats {
  totalHoldingAmount: number
  totalCurrentValue: number
  totalProfitLoss: number
  totalProfitLossPct: number
  realizedProfitLoss: number
  soldCount: number
  totalBuyCount: number
  totalSellCount: number
  currentHoldingCount: number
  todayBuyAmount: number
  todaySellAmount: number
  todayBuyCount: number
  todaySellCount: number
  dailyLimitAmount: number
  remainingDailyLimit: number
}

// 페이지 응답 타입
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}