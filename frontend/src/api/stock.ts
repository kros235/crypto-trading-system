// ===================================================
// Phase 2: 주식/ETF API 클라이언트
// ===================================================
import api from './index'
import type { StockInfo, StockTradingSetting, StockTradingSettingRequest, KisApiKeyRequest } from '@/types/stock'

// ⭐⭐⭐ [Day 60 추가] 다중 종목 가격 조회 응답 타입 ⭐⭐⭐
// 백엔드 StockPriceDTO 와 1:1 대응
export interface StockPrice {
  stockCode: string
  currentPrice: number | null
  changeFromPrevDay: number | null
  changeRate: number | null              // 전일 대비율 (%)
  accumulatedVolume: number | null
  accumulatedTradingValue: number | null
  openPrice: number | null
  highPrice: number | null
  lowPrice: number | null
  prevClosePrice: number | null
}

// 주식 정보 API
export const stockInfoApi = {
  // 활성 종목 목록
  getActiveStocks: () =>
    api.get<StockInfo[]>('/stock/info/active'),

  // 종목 검색
  searchStocks: (keyword: string) =>
    api.get<StockInfo[]>('/stock/info/search', { params: { keyword } }),

  // 종목 등록 (DB에 추가)
  addStock: (stockCode: string, stockName: string, market: string, etfType: string) =>
    api.post('/stock/info', { stockCode, stockName, market, etfType }),

  // 종목 삭제 (비활성화)
  deleteStock: (stockCode: string) =>
    api.delete(`/stock/info/${stockCode}`),

  // KIS API로 종목 검색
  searchFromKis: (keyword: string) =>
    api.get<StockInfo[]>('/stock/info/kis-search', { params: { keyword } }),

  // ⭐⭐⭐ [Day 60 추가] 다중 종목 현재가/변동률 일괄 조회 ⭐⭐⭐
  // 사용처: StockListView (종목 목록 페이지), StockHoldingsView 등
  // KIS API 키 미등록 시: 응답은 정상이지만 가격 필드가 null (graceful degradation)
  getPrices: (stockCodes: string[]) =>
    api.post<StockPrice[]>('/stock/info/prices', { stockCodes }),
}

// 주식 거래 설정 API
export const stockSettingApi = {
  // ⭐ 수정: URL을 백엔드 컨트롤러(@RequestMapping("/api/stock/settings"))에 맞춤
  // 기존 /stock/trading-settings → /stock/settings

  // 설정 조회
  getSettings: () =>
    api.get<StockTradingSetting>('/stock/settings'),

  // 설정 생성
  createSettings: (data: StockTradingSettingRequest) =>
    api.post<StockTradingSetting>('/stock/settings', data),

  // 설정 수정
  updateSettings: (data: StockTradingSettingRequest) =>
    api.put<StockTradingSetting>('/stock/settings', data),

  // 설정 삭제
  deleteSettings: () =>
    api.delete('/stock/settings'),

  // ⭐ 수정: KIS API 키 등록 (kis-api-keys → kis-api-key, 백엔드 엔드포인트에 맞춤)
  saveKisApiKeys: (data: KisApiKeyRequest) =>
    api.post('/stock/settings/kis-api-key', data),

  // ⭐ 수정: KIS API 키 삭제 (kis-api-keys → kis-api-key)
  deleteKisApiKeys: () =>
    api.delete('/stock/settings/kis-api-key'),

  // ⭐ 추가: KIS API 키 등록 상태 확인 (백엔드에 이미 구현되어 있음)
  hasKisApiKey: () =>
    api.get<boolean>('/stock/settings/kis-api-key/status'),
}

// ⭐ [Day 58 추가] 주식 거래 내역 API
export const stockTransactionApi = {
  // 전체 조회 (페이징)
  getAll: (page = 0, size = 20) =>
    api.get('/stock/transactions', { params: { page, size } }),

  // 복합 조건 검색
  search: (params: {
    stockCode?: string
    status?: string
    startDate?: string
    endDate?: string
    page?: number
    size?: number
  }) => api.get('/stock/transactions/search', { params }),

  // 보유 중인 주식
  getHoldings: () =>
    api.get('/stock/transactions/holdings'),

  // 상세 조회
  getOne: (transactionId: number) =>
    api.get(`/stock/transactions/${transactionId}`),

  // 메모 수정
  update: (transactionId: number, data: { note: string }) =>
    api.put(`/stock/transactions/${transactionId}`, data),

  // 수동 매도
  sell: (transactionId: number, soldPrice: number) =>
    api.post(`/stock/transactions/${transactionId}/sell`, { soldPrice }),
}

export const stockDashboardApi = {
  getStats: () =>
    api.get('/stock/dashboard/stats'),
  getExchangeRate: () =>
    api.get('/stock/dashboard/exchange-rate'),
  // ⭐ [추가] KIS 계좌 실제 잔고 (KRW잔고, 주식평가액, 총자산)
  getAccount: () =>
    api.get('/stock/dashboard/account'),
}