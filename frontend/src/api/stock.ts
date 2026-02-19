// ===================================================
// Phase 2: 주식/ETF API 클라이언트
// ===================================================
import api from './index'
import type { StockInfo, StockTradingSetting, StockTradingSettingRequest, KisApiKeyRequest } from '@/types/stock'

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