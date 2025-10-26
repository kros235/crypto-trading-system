import { defineStore } from 'pinia'
import { ref } from 'vue'
import { coinApi } from '@/api'
import type { CoinInfo, CoinTicker } from '@/types'

export const useCoinStore = defineStore('coin', () => {
  // State
  const coins = ref<CoinInfo[]>([])
  const tickers = ref<Map<string, CoinTicker>>(new Map())
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Actions
  const fetchActiveCoins = async () => {
    loading.value = true
    error.value = null

    try {
      const response = await coinApi.getActiveCoins()
      coins.value = response.data
      return true
    } catch (err: any) {
      error.value = err.response?.data?.error || '코인 목록 조회 실패'
      return false
    } finally {
      loading.value = false
    }
  }

  const fetchCoinPrice = async (symbol: string) => {
    try {
      const response = await coinApi.getCoinPrice(symbol)
      tickers.value.set(symbol, response.data)
      return response.data
    } catch (err: any) {
      console.error(`${symbol} 시세 조회 실패:`, err)
      return null
    }
  }

  const fetchMultiplePrices = async (symbols: string[]) => {
    try {
      const response = await coinApi.getMultiplePrices(symbols)
      response.data.forEach(ticker => {
        tickers.value.set(ticker.market, ticker)
      })
      return response.data
    } catch (err: any) {
      console.error('시세 조회 실패:', err)
      return []
    }
  }

  const getCoinTicker = (symbol: string) => {
    return tickers.value.get(symbol)
  }

  return {
    coins,
    tickers,
    loading,
    error,
    fetchActiveCoins,
    fetchCoinPrice,
    fetchMultiplePrices,
    getCoinTicker
  }
})