import { defineStore } from 'pinia'
import { ref } from 'vue'
import { tradingApi } from '@/api'
import type { TradingSetting, TradingSettingRequest } from '@/types'

export const useTradingStore = defineStore('trading', () => {
  const settings = ref<TradingSetting | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 거래 설정 조회
  const fetchSettings = async () => {
    loading.value = true
    error.value = null
    try {
      const response = await tradingApi.getSettings()
      settings.value = response.data
      return true
    } catch (err: any) {
      error.value = err.response?.data?.error || '거래 설정 조회에 실패했습니다'
      settings.value = null
      return false
    } finally {
      loading.value = false
    }
  }

  // 거래 설정 생성
  const createSettings = async (data: TradingSettingRequest) => {
    loading.value = true
    error.value = null
    try {
      const response = await tradingApi.createSettings(data)
      settings.value = response.data
      return true
    } catch (err: any) {
      error.value = err.response?.data?.error || '거래 설정 생성에 실패했습니다'
      return false
    } finally {
      loading.value = false
    }
  }

  // 거래 설정 수정
  const updateSettings = async (data: TradingSettingRequest) => {
    loading.value = true
    error.value = null
    try {
      const response = await tradingApi.updateSettings(data)
      settings.value = response.data
      return true
    } catch (err: any) {
      error.value = err.response?.data?.error || '거래 설정 수정에 실패했습니다'
      return false
    } finally {
      loading.value = false
    }
  }

  // 거래 설정 삭제
  const deleteSettings = async () => {
    loading.value = true
    error.value = null
    try {
      await tradingApi.deleteSettings()
      settings.value = null
      return true
    } catch (err: any) {
      error.value = err.response?.data?.error || '거래 설정 삭제에 실패했습니다'
      return false
    } finally {
      loading.value = false
    }
  }

  return {
    settings,
    loading,
    error,
    fetchSettings,
    createSettings,
    updateSettings,
    deleteSettings
  }
})