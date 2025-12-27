<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main>
      <v-container fluid>
        <!-- 페이지 제목 -->
        <v-row class="mb-4">
          <v-col>
            <h1 class="text-h4">📊 일일 거래 리포트</h1>
            <p class="text-subtitle-1 text-grey">오늘의 거래 현황과 수익률을 확인하세요</p>
          </v-col>
        </v-row>

        <!-- 리포트 로딩/에러 상태 -->
        <v-row v-if="loading">
          <v-col cols="12" class="text-center">
            <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
            <p class="mt-4">리포트를 불러오는 중...</p>
          </v-col>
        </v-row>

        <template v-else-if="report">
          <!-- 요약 카드 -->
          <v-row class="mb-4">
            <v-col cols="12" md="3">
              <v-card 
                class="pa-4" 
                :color="report.totalProfit >= 0 ? 'success' : 'error'" 
                dark
              >
                <div class="d-flex align-center">
                  <v-icon size="48" class="mr-4">
                    {{ report.totalProfit >= 0 ? 'mdi-trending-up' : 'mdi-trending-down' }}
                  </v-icon>
                  <div>
                    <div class="text-h6">총 손익</div>
                    <div class="text-h4">
                      {{ formatCurrency(report.totalProfit) }}
                    </div>
                    <div class="text-subtitle-2">
                      {{ report.profitRate >= 0 ? '+' : '' }}{{ report.profitRate.toFixed(2) }}%
                    </div>
                  </div>
                </div>
              </v-card>
            </v-col>
            
            <v-col cols="12" md="3">
              <v-card class="pa-4" color="primary" dark>
                <div class="d-flex align-center">
                  <v-icon size="48" class="mr-4">mdi-wallet</v-icon>
                  <div>
                    <div class="text-h6">총 평가액</div>
                    <div class="text-h4">{{ formatCurrency(report.totalHoldingValue) }}</div>
                    <div class="text-subtitle-2">
                      투자원금: {{ formatCurrency(report.totalInvestment) }}
                    </div>
                  </div>
                </div>
              </v-card>
            </v-col>
            
            <v-col cols="12" md="3">
              <v-card class="pa-4" color="info" dark>
                <div class="d-flex align-center">
                  <v-icon size="48" class="mr-4">mdi-cart-arrow-down</v-icon>
                  <div>
                    <div class="text-h6">오늘 매수</div>
                    <div class="text-h4">{{ report.buyCount }}건</div>
                    <div class="text-subtitle-2">
                      {{ formatCurrency(report.totalBuyAmount) }}
                    </div>
                  </div>
                </div>
              </v-card>
            </v-col>
            
            <v-col cols="12" md="3">
              <v-card class="pa-4" color="warning" dark>
                <div class="d-flex align-center">
                  <v-icon size="48" class="mr-4">mdi-cart-arrow-up</v-icon>
                  <div>
                    <div class="text-h6">오늘 매도</div>
                    <div class="text-h4">{{ report.sellCount }}건</div>
                    <div class="text-subtitle-2">
                      {{ formatCurrency(report.totalSellAmount) }}
                    </div>
                  </div>
                </div>
              </v-card>
            </v-col>
          </v-row>

          <!-- 손익 상세 -->
          <v-row class="mb-4">
            <v-col cols="12" md="6">
              <v-card class="pa-4 detail-card">
                <v-card-title>
                  <v-icon class="mr-2">mdi-chart-pie</v-icon>
                  손익 상세
                </v-card-title>
                <v-card-text>
                  <v-list>
                    <v-list-item>
                      <template v-slot:prepend>
                        <v-icon color="success" size="28">mdi-check-circle</v-icon>
                      </template>
                      <v-list-item-title class="text-body-1 font-weight-medium">실현 손익</v-list-item-title>
                      <template v-slot:append>
                        <span :class="[report.realizedProfit >= 0 ? 'text-success' : 'text-error', 'text-h6']">
                          {{ report.realizedProfit >= 0 ? '+' : '' }}{{ formatCurrency(report.realizedProfit) }}
                        </span>
                      </template>
                    </v-list-item>
                    
                    <v-list-item>
                      <template v-slot:prepend>
                        <v-icon color="warning" size="28">mdi-clock-outline</v-icon>
                      </template>
                      <v-list-item-title class="text-body-1 font-weight-medium">평가 손익</v-list-item-title>
                      <template v-slot:append>
                        <span :class="[report.unrealizedProfit >= 0 ? 'text-success' : 'text-error', 'text-h6']">
                                  {{ report.unrealizedProfit >= 0 ? '+' : '' }}{{ formatCurrency(report.unrealizedProfit) }}
                        </span>
                      </template>
                    </v-list-item>
          
                    <v-divider class="my-3"></v-divider>
          
                    <v-list-item>
                      <template v-slot:prepend>
                        <v-icon :color="report.totalProfit >= 0 ? 'success' : 'error'" size="28">
                          {{ report.totalProfit >= 0 ? 'mdi-trending-up' : 'mdi-trending-down' }}
                        </v-icon>
                      </template>
                      <v-list-item-title class="text-body-1 font-weight-bold">총 손익</v-list-item-title>
                      <template v-slot:append>
                        <span 
                          :class="[report.totalProfit >= 0 ? 'text-success' : 'text-error', 'font-weight-bold', 'text-h5']"
                        >
                          {{ report.totalProfit >= 0 ? '+' : '' }}{{ formatCurrency(report.totalProfit) }}
                        </span>
                      </template>
                    </v-list-item>
                  </v-list>
                </v-card-text>
              </v-card>
            </v-col>
            
            <v-col cols="12" md="6">
              <v-card class="pa-4 detail-card">
                <v-card-title>
                  <v-icon class="mr-2">mdi-bell</v-icon>
                  알림
                </v-card-title>
                <v-card-text>
                  <v-btn
                    color="primary"
                    block
                    size="large"
                    :loading="sendingReport"
                    @click="sendReport"
                    class="mb-4"
                  >
                    <v-icon left>mdi-send</v-icon>
                    Discord로 리포트 발송
                  </v-btn>

                  <v-btn
                    color="success"
                    block
                    size="large"
                    :loading="sendingEmailReport"
                    @click="sendEmailReport"
                    class="mb-4"
                  >
                    <v-icon left>mdi-email-send</v-icon>
                    이메일로 리포트 발송
                  </v-btn>
                  
                  <v-alert type="info" variant="tonal" class="mt-2">
                    매일 23:50에 자동으로 일일 리포트가 발송됩니다.
                  </v-alert>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>

          <!-- 코인별 현황 -->
          <v-row v-if="report.coinSummaries && report.coinSummaries.length > 0">
            <v-col cols="12">
              <v-card>
                <v-card-title>
                  <v-icon class="mr-2">mdi-format-list-bulleted</v-icon>
                  코인별 현황 ({{ report.holdingCount }}종목)
                </v-card-title>
                <v-card-text>
                  <v-data-table
                    :headers="coinHeaders"
                    :items="report.coinSummaries"
                    class="elevation-1"
                    item-key="coinSymbol"
                  >
                    <!-- 코인 심볼 -->
                    <template v-slot:item.coinSymbol="{ item }">
                      <v-chip color="primary" small>{{ item.coinSymbol }}</v-chip>
                    </template>
                    
                    <!-- 보유 수량 -->
                    <template v-slot:item.totalQuantity="{ item }">
                      {{ formatQuantity(item.totalQuantity) }}
                    </template>
                    
                    <!-- 평균 단가 -->
                    <template v-slot:item.averagePrice="{ item }">
                      {{ formatCurrency(item.averagePrice) }}
                    </template>
                    
                    <!-- 현재가 -->
                    <template v-slot:item.currentPrice="{ item }">
                      {{ formatCurrency(item.currentPrice) }}
                    </template>
                    
                    <!-- 평가 손익 -->
                    <template v-slot:item.profitLoss="{ item }">
                      <span :class="item.profitLoss >= 0 ? 'text-success' : 'text-error'">
                        {{ item.profitLoss >= 0 ? '+' : '' }}{{ formatCurrency(item.profitLoss) }}
                      </span>
                    </template>
                    
                    <!-- 수익률 -->
                    <template v-slot:item.profitRate="{ item }">
                      <v-chip 
                        :color="item.profitRate >= 0 ? 'success' : 'error'" 
                        small 
                        dark
                      >
                        {{ item.profitRate >= 0 ? '+' : '' }}{{ item.profitRate.toFixed(2) }}%
                      </v-chip>
                    </template>
                  </v-data-table>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>

          <!-- 보유 코인 없음 -->
          <v-row v-else>
            <v-col cols="12">
              <v-card class="pa-8 text-center">
                <v-icon size="64" color="grey">mdi-package-variant</v-icon>
                <h3 class="text-h6 mt-4">보유 중인 코인이 없습니다</h3>
                <p class="text-grey">자동매매 봇이 매수 조건에 맞는 코인을 찾으면 자동으로 매수합니다.</p>
              </v-card>
            </v-col>
          </v-row>
        </template>

        <!-- 에러 상태 -->
        <v-row v-else>
          <v-col cols="12">
            <v-alert type="error">
              리포트를 불러오는데 실패했습니다. 다시 시도해주세요.
            </v-alert>
            <v-btn color="primary" @click="fetchReport" class="mt-4">
              다시 시도
            </v-btn>
          </v-col>
        </v-row>

        <!-- 알림 -->
        <v-snackbar v-model="snackbar" :color="snackbarColor" :timeout="3000">
          {{ snackbarMessage }}
        </v-snackbar>
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'
import type { DailyReport } from '@/types/bot'

// 사이드바 Ref
const sidebarRef = ref()

// API 호출 함수
const api = {
  get: async (url: string) => {
    const token = localStorage.getItem('token')
    const response = await fetch(`/api${url}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    return response.json()
  },
  post: async (url: string) => {
    const token = localStorage.getItem('token')
    const response = await fetch(`/api${url}`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` }
    })
    return response.json()
  }
}

// 상태
const loading = ref(false)
const sendingReport = ref(false)
const sendingEmailReport = ref(false)
const report = ref<DailyReport | null>(null)

const snackbar = ref(false)
const snackbarMessage = ref('')
const snackbarColor = ref('success')

// 테이블 헤더
const coinHeaders = [
  { title: '코인', key: 'coinSymbol', sortable: true },
  { title: '보유 건수', key: 'holdingCount', sortable: true },
  { title: '총 수량', key: 'totalQuantity', sortable: true },
  { title: '평균 단가', key: 'averagePrice', sortable: true },
  { title: '현재가', key: 'currentPrice', sortable: true },
  { title: '평가 손익', key: 'profitLoss', sortable: true },
  { title: '수익률', key: 'profitRate', sortable: true },
]

// 메서드
const fetchReport = async () => {
  loading.value = true
  try {
    report.value = await api.get('/notifications/daily-report/preview')
  } catch (error) {
    console.error('리포트 조회 실패:', error)
    report.value = null
  } finally {
    loading.value = false
  }
}

const sendReport = async () => {
  sendingReport.value = true
  try {
    await api.post('/notifications/daily-report/send')
    showSnackbar('일일 리포트가 Discord로 발송되었습니다.', 'success')
  } catch (error) {
    console.error('리포트 발송 실패:', error)
    showSnackbar('리포트 발송에 실패했습니다.', 'error')
  } finally {
    sendingReport.value = false
  }
}

const sendEmailReport = async () => {
  sendingEmailReport.value = true
  try {
    await api.post('/notifications/email/daily-report')
    showSnackbar('일일 리포트가 이메일로 발송되었습니다.', 'success')
  } catch (error) {
    console.error('이메일 발송 실패:', error)
    showSnackbar('이메일 발송에 실패했습니다.', 'error')
  } finally {
    sendingEmailReport.value = false
  }
}

// 유틸리티 함수
const formatCurrency = (value: number) => {
  if (value === null || value === undefined) return '0원'
  return new Intl.NumberFormat('ko-KR').format(Math.round(value)) + '원'
}

const formatQuantity = (value: number) => {
  if (!value) return '0'
  return value.toFixed(8).replace(/\.?0+$/, '')
}

const showSnackbar = (message: string, color: string) => {
  snackbarMessage.value = message
  snackbarColor.value = color
  snackbar.value = true
}

// 마운트 시 데이터 로드
onMounted(() => {
  fetchReport()
})
</script>

<style scoped>

.detail-card {
  height: 100%;
  min-height: 280px;
}

.text-success {
  color: #4CAF50 !important;
}

.text-error {
  color: #F44336 !important;
}


:deep(.v-data-table) {
  font-size: 1rem !important;
}

:deep(.v-data-table th) {
  font-size: 0.95rem !important;
  font-weight: 600 !important;
}

:deep(.v-data-table td) {
  font-size: 1rem !important;
  padding-top: 12px !important;
  padding-bottom: 12px !important;
}

:deep(.v-chip) {
  font-size: 0.9rem !important;
}
</style>