// 사용자 정보 타입
export interface User {
  userId: string
  email: string
  phone?: string
  role: string
  joinDate: string
  lastLogin?: string
  isActive: boolean
  hasApiKey: boolean
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