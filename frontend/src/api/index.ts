import axios from 'axios'
import type { AxiosError } from 'axios'
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
import type { ApiErrorResponse } from '@/types/error'
import { getErrorMessage } from '@/types/error'  

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
// 응답 인터셉터: 에러 처리 (개선)
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorResponse>) => {
    const status = error.response?.status
    const errorData = error.response?.data
    
    // 네트워크 오류
    if (!error.response) {
      console.error('네트워크 오류:', error.message)
      return Promise.reject({
        code: 'NETWORK_ERROR',
        message: '네트워크 연결을 확인해주세요.',
        originalError: error
      })
    }

    // 로그인/회원가입 API는 원본 에러 유지
    const isLoginRequest = error.config?.url?.includes('/auth/login')
    const isSignupRequest = error.config?.url?.includes('/auth/signup')
    
    if (isLoginRequest || isSignupRequest) {
      // 로그인/회원가입 API는 원본 response 구조 유지 (auth.ts에서 처리)
      return Promise.reject(error)
    }
    
    // 401 Unauthorized - 토큰 만료/무효
    if (status === 401) {
      // 2FA 필요 응답 처리
      if (errorData?.code === '2FA_REQUIRED') {
        return Promise.reject({
          response: {
            status: 401,
            data: errorData
          }
        })
      }
      
      const errorCode = errorData?.error?.code
      // 로그인 API 경로이거나 로그인 실패(A005)인 경우 리다이렉트하지 않음
      const isLoginRequest = error.config?.url?.includes('/auth/login')
      const isSignupRequest = error.config?.url?.includes('/auth/signup')
      
      if (!isLoginRequest && !isSignupRequest && errorCode !== 'A005') {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/login'
      }
    }
    
    // 에러 응답이 새 형식인 경우 (ApiResponse)
    if (errorData?.error) {
      const apiError = errorData.error
      return Promise.reject({
        code: apiError.code,
        message: getErrorMessage(apiError.code, apiError.message),
        detail: apiError.detail,
        fieldErrors: apiError.fieldErrors,
        status: status
      })
    }
    
    // 기존 형식 에러 (하위 호환)
    return Promise.reject({
      code: 'UNKNOWN',
      message: error.response?.data?.message || error.response?.data?.error || '오류가 발생했습니다.',
      status: status,
      originalError: error
    })
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

export const userApi = {
  getProfile: () =>
    api.get<User>('/user/profile'),

  updateProfile: (data: UpdateProfileRequest) =>
    api.put('/user/profile', data),

  saveApiKeys: (data: SaveApiKeysRequest) =>
    api.post('/user/api-keys', data),

  deleteApiKeys: () =>
    api.delete('/user/api-keys'),

  // IP 화이트리스트 API 추가
  getAllowedIps: () => api.get('/user/allowed-ips'),
  addAllowedIp: (ip: string) => api.post('/user/allowed-ips', { ip }),
  removeAllowedIp: (ip: string) => api.delete(`/user/allowed-ips/${ip}`),
  disableIpWhitelist: () => api.delete('/user/allowed-ips'),
  getCurrentIp: () => api.get<{ ip: string }>('/user/current-ip'),

  // 2FA API
  get2FAStatus: () => api.get('/user/2fa/status'),
  setup2FA: () => api.post('/user/2fa/setup'),
  confirm2FA: (code: string) => api.post('/user/2fa/confirm', { code }),
  disable2FA: (code: string) => api.post('/user/2fa/disable', { code }),
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

  // Discord DM API
  // Discord Bot 상태 조회
  getDiscordBotStatus: () => api.get('/notifications/discord/bot-status'),
  
  // Discord DM 테스트 발송
  sendTestDiscordDM: () => api.post('/notifications/discord/test-dm'),

  // Discord DM 알림 테스트 API
  testDailyReportDM: () => api.post('/notifications/discord/test-daily-report'),
  testBuyDM: () => api.post('/notifications/discord/test-buy'),
  testSellDM: () => api.post('/notifications/discord/test-sell'),
  testStopLossDM: () => api.post('/notifications/discord/test-stoploss'),
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
  }) => api.post('/backtest/run', data, { timeout: 300000 }),  // 5분
  
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

// Admin API
export const adminApi = {
  getStats: () => api.get('/admin/stats'),
  getUsers: (params?: { page?: number; size?: number }) => 
    api.get('/admin/users', { params }),
  getUserDetail: (userId: string) => api.get(`/admin/users/${userId}`),
  toggleUserActive: (userId: string) => 
    api.put(`/admin/users/${userId}/toggle-active`),
  changeUserRole: (userId: string, role: string) => 
    api.put(`/admin/users/${userId}/role`, { role }),
  forceLogout: (userId: string) => 
    api.post(`/admin/users/${userId}/logout`),
  getMonitoring: () => api.get('/admin/monitoring'),
  getSlowQueries: () => api.get('/admin/monitoring/slow-queries'),
}

// Email Notification API (추가)
export const emailApi = {
  sendTest: () => api.post('/notifications/email/test'),
  sendDailyReport: () => api.post('/notifications/email/daily-report')
}

// 2FA API
export const twoFactorApi = {
  // 2FA 설정 시작
  setup: () => api.post('/2fa/setup'),
  
  // 2FA 활성화
  enable: (otpCode: string) => api.post('/2fa/enable', { otpCode }),
  
  // 2FA 비활성화
  disable: (otpCode: string) => api.post('/2fa/disable', { otpCode }),
  
  // 2FA 상태 확인
  getStatus: () => api.get('/2fa/status'),
  
  // 2FA 필요 여부 확인 (로그인 전)
  checkRequired: (userId: string) => api.get(`/2fa/required/${userId}`)
}

// 수익 분석 API
export const profitApi = {
  // 기간별 수익 요약 조회
  getSummary: () => api.get('/profit/summary'),
  
  // 특정 기간 수익 상세 조회
  getByPeriod: (period: string) => api.get(`/profit/by-period?period=${period}`),
  
  // 코인별 수익 분석 조회
  getByCoin: () => api.get('/profit/by-coin'),
}

export default api