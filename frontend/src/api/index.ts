import axios from 'axios'
import type {
  LoginRequest,
  SignupRequest,
  AuthResponse,
  User,
  CoinInfo,
  CoinTicker,
  UpdateProfileRequest,  
  SaveApiKeysRequest,   
  TradingSetting,           
  TradingSettingRequest,
  Transaction,              
  CreateTransactionRequest,
  SellTransactionRequest,  
  DashboardStats,           
  PageResponse             
} from '@/types'

// Axios 인스턴스 생성
const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'  // ⭐ 수정: charset 추가
  }
})

// 요청 인터셉터: 토큰 자동 추가
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 응답 인터셉터: 에러 처리
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // 토큰 만료 시 로그아웃
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// 인증 API
export const authApi = {
  // 로그인
  login: (data: LoginRequest) =>
    api.post<AuthResponse>('/auth/login', data),

  // 회원가입
  signup: (data: SignupRequest) =>
    api.post<AuthResponse>('/auth/signup', data),

  // 토큰 검증
  validateToken: () =>
    api.get('/auth/validate')
}

// ✅ 수정 후 (깔끔하게 정리)
export const userApi = {
  getProfile: () =>
    api.get<User>('/user/profile'),

  updateProfile: (data: UpdateProfileRequest) =>
    api.put('/user/profile', data),

  saveApiKeys: (data: SaveApiKeysRequest) =>
    api.post('/user/api-keys', data),

  deleteApiKeys: () =>
    api.delete('/user/api-keys')
}

// 코인 API
export const coinApi = {
  // 활성 코인 목록
  getActiveCoins: () =>
    api.get<CoinInfo[]>('/coins/active'),

  // 현재가 조회
  getCoinPrice: (symbol: string) =>
    api.get<CoinTicker>(`/coins/${symbol}/price`),

  // 여러 코인 현재가
  getMultiplePrices: (symbols: string[]) =>
    api.get<CoinTicker[]>('/coins/prices', {
      params: { symbols: symbols.join(',') }
    }),

  // 계좌 정보
  getAccounts: () =>
    api.get('/coins/accounts')
}

// 시스템 API
export const systemApi = {
  // 헬스체크
  health: () =>
    api.get('/health')
}

// 거래 설정 API
export const tradingApi = {
  getSettings: () => 
    api.get<TradingSetting>('/trading-settings'),
  
  createSettings: (data: TradingSettingRequest) => 
    api.post<TradingSetting>('/trading-settings', data),
  
  updateSettings: (data: TradingSettingRequest) => 
    api.put<TradingSetting>('/trading-settings', data),
  
  deleteSettings: () => 
    api.delete('/trading-settings')
}

// 거래 내역 API
export const transactionApi = {
  // 전체 거래 내역 조회 (페이징)
  getAll: (page = 0, size = 20) =>
    api.get<PageResponse<Transaction>>('/transactions', {
      params: { page, size }
    }),

  // 거래 내역 검색
  search: (params: {
    coinSymbol?: string
    status?: 'HOLDING' | 'SOLD' | 'CANCELLED'
    startDate?: string
    endDate?: string
    page?: number
    size?: number
  }) =>
    api.get<PageResponse<Transaction>>('/transactions/search', { params }),

  // 보유 자산 조회
  getHoldings: () =>
    api.get<Transaction[]>('/transactions/holdings'),

  // 특정 거래 조회
  getOne: (transactionId: number) =>
    api.get<Transaction>(`/transactions/${transactionId}`),

  // 거래 생성 (매수)
  create: (data: CreateTransactionRequest) =>
    api.post('/transactions', data),

  // 거래 수정 (메모)
  update: (transactionId: number, data: { note?: string }) =>
    api.put(`/transactions/${transactionId}`, data),

  // 매도 처리
  sell: (transactionId: number, data: SellTransactionRequest) =>
    api.post(`/transactions/${transactionId}/sell`, data),

  // 대시보드 통계
  getStats: () =>
    api.get<DashboardStats>('/transactions/dashboard-stats')
}

// 봇 API
export const botApi = {
  // 봇 상태 조회
  getStatus: () => api.get('/bot/status'),
  
  // 수동 실행
  execute: () => api.post('/bot/execute'),
  
  // 기술적 지표 조회 (단일)
  getIndicator: (market: string) => api.get(`/bot/indicators/${market}`),
  
  // 기술적 지표 조회 (다중)
  getIndicators: (markets: string[]) => 
    api.get('/bot/indicators', { params: { markets: markets.join(',') } }),
}

// 알림 API
export const notificationApi = {
  // 테스트 알림
  sendTest: () => api.post('/notifications/test'),
  
  // 일일 리포트 미리보기
  getDailyReportPreview: () => api.get('/notifications/daily-report/preview'),
  
  // 일일 리포트 발송
  sendDailyReport: () => api.post('/notifications/daily-report/send'),
  
  // 알림 상태
  getStatus: () => api.get('/notifications/status'),
}

// 백테스트 API
export const backtestApi = {
  // 백테스트 실행
  run: (data: {
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
  }) => api.post('/backtest/run', data),
  
  // 빠른 백테스트
  quick: (coins: string[], days: number = 30, initialBalance: number = 1000000) =>
    api.get('/backtest/quick', { 
      params: { coins: coins.join(','), days, initialBalance } 
    }),
  
  // 백테스트 가능 코인 목록
  getAvailableCoins: () => api.get('/backtest/available-coins'),
  
  // 기본 설정값
  getDefaultSettings: () => api.get('/backtest/default-settings'),
}

export default api