import axios from 'axios'
import type {
  LoginRequest,
  SignupRequest,
  AuthResponse,
  User,
  CoinInfo,
  CoinTicker
} from '@/types'

// Axios 인스턴스 생성
const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
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

// 사용자 API
export const userApi = {
  // 프로필 조회
  getProfile: () =>
    api.get<User>('/user/profile'),

  // 프로필 수정
  updateProfile: (data: Partial<User>) =>
    api.put('/user/profile', data),

  // API 키 저장
  saveApiKeys: (accessKey: string, secretKey: string) =>
    api.post('/user/api-keys', { accessKey, secretKey }),

  // API 키 삭제
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

export default api