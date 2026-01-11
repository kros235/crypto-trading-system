/**
 * 수익 분석 관련 타입 정의
 * Day 31: 기간별/코인별 수익 분석 기능
 */

// 기간별 수익 요약
export interface ProfitSummary {
  todayProfit: number
  todayProfitPct: number
  todayTradeCount: number
  
  monthProfit: number
  monthProfitPct: number
  monthTradeCount: number
  
  yearProfit: number
  yearProfitPct: number
  yearTradeCount: number
  
  oneYearProfit: number
  oneYearProfitPct: number
  oneYearTradeCount: number
  
  totalProfit: number
  totalProfitPct: number
  totalTradeCount: number
  
  initialInvestment: number
}

// 일별 수익 (차트용)
export interface DailyProfit {
  date: string
  profit: number
  tradeCount: number
}

// 특정 기간 수익 상세
export interface PeriodProfit {
  period: string
  startDate: string
  endDate: string
  
  totalProfit: number
  profitPct: number
  tradeCount: number
  winCount: number
  loseCount: number
  winRate: number
  
  avgProfit: number
  maxProfit: number
  maxLoss: number
  
  dailyProfits: DailyProfit[]
}

// 코인별 수익 분석
export interface CoinProfit {
  coinSymbol: string
  coinName: string
  
  totalProfit: number
  profitPct: number
  
  totalTradeCount: number
  winCount: number
  loseCount: number
  winRate: number
  
  totalBuyAmount: number
  totalSellAmount: number
  avgBuyPrice: number
  avgSellPrice: number
  
  maxProfit: number
  maxLoss: number
  
  currentHoldingCount: number
  currentHoldingAmount: number
  unrealizedProfit: number
  
  lastTradeAt: string | null
}

// 기간 타입
export type PeriodType = 'today' | 'month' | 'year' | 'oneYear' | 'total'