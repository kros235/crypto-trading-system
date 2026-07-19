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

// ⭐⭐⭐ [Day 61 추가] 주식 봇 모니터링 API ⭐⭐⭐
// Phase 1 /api/bot/* 와 동일한 패턴, 주식은 /api/stock/bot/*
export const stockBotApi = {
  // 봇 상태 조회 (봇 활성화/장 운영/긴급정지/카운트다운)
  getStatus: () =>
    api.get('/stock/bot/status'),

  // 봇 활성화
  start: () =>
    api.post('/stock/bot/start'),

  // 봇 비활성화
  stop: () =>
    api.post('/stock/bot/stop'),

  // 수동 매매 실행
  execute: () =>
    api.post('/stock/bot/execute'),

  // 사용자 거래 설정의 모든 종목 기술적 지표 일괄 조회
  getIndicators: () =>
    api.get('/stock/bot/indicators'),

  // 단일 종목 기술적 지표 조회
  getIndicator: (stockCode: string) =>
    api.get(`/stock/bot/indicators/${stockCode}`),

  // 일일 거래 캐시 수동 초기화
  resetDailyCache: () =>
    api.post('/stock/bot/reset-daily-cache'),

// 보유기간 경고 대상 조회 (15일 이상/20일 이상)
  getHoldingWarnings: () =>
    api.get('/stock/bot/holding-warnings'),
}

// ⭐⭐⭐ [Day 62 추가] 주식/ETF 백테스트 API ⭐⭐⭐
// Phase 1 backtestApi(/api/backtest) 구조 재사용
import type { StockBacktestRequest, StockBacktestResult, AvailableStocksResponse } from '@/types/stockBacktest'

export const stockBacktestApi = {
  // 백테스트 실행 (실제 KIS API 기간별 일봉 조회 포함 - 종목/기간에 따라 다소 시간 소요될 수 있음)
  run: (data: StockBacktestRequest) =>
    api.post<StockBacktestResult>('/stock/backtest/run', data, { timeout: 300000 }), // 5분

  // 백테스트 가능 종목 목록 (사용자 등록 활성 종목)
  getAvailableStocks: () =>
    api.get<AvailableStocksResponse>('/stock/backtest/available-stocks'),

  // 기본 설정값
  getDefaultSettings: () =>
    api.get<StockBacktestRequest>('/stock/backtest/default-settings'),
}