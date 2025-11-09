import axios from 'axios'
import type {
  LoginRequest,
  SignupRequest,
  AuthResponse,
  User,
  CoinInfo,
  CoinTicker,
  UpdateProfileRequest,      // ✅ 추가
  SaveApiKeysRequest,         // ✅ 추가
  TradingSetting,             // ✅ 추가
  TradingSettingRequest       // ✅ 추가
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

export default api