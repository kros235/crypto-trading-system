<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main>
      <v-container fluid>
        <!-- 페이지 제목 -->
        <v-row class="mb-4">
          <v-col>
            <h1 class="text-h4">🤖 자동매매 봇 모니터링</h1>
            <p class="text-subtitle-1 text-grey">실시간 봇 상태 및 기술적 지표 모니터링</p>
          </v-col>
        </v-row>

        <!-- 봇 상태 카드 -->
        <v-row class="mb-4">
          <v-col cols="12" md="3">
            <v-card class="pa-4" :color="botRunning ? 'success' : 'grey'" dark>
              <div class="d-flex align-center">
                <v-icon size="48" class="mr-4">
                  {{ botRunning ? 'mdi-robot' : 'mdi-robot-off' }}
                </v-icon>
                <div>
                  <div class="text-h6">봇 상태</div>
                  <div class="text-h4">{{ botRunning ? '실행 중' : '대기 중' }}</div>
                </div>
              </div>
            </v-card>
          </v-col>
          
          <v-col cols="12" md="3">
            <v-card class="pa-4" color="primary" dark>
              <div class="d-flex align-center">
                <v-icon size="48" class="mr-4">mdi-clock-outline</v-icon>
                <div>
                  <div class="text-h6">다음 실행</div>
                  <div class="text-h5">{{ nextExecution }}</div>
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
                  <div class="text-h4">{{ todayStats.buyCount }}건</div>
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
                  <div class="text-h4">{{ todayStats.sellCount }}건</div>
                </div>
              </div>
            </v-card>
          </v-col>
        </v-row>

        <!-- 수동 실행 버튼 -->
        <v-row class="mb-4">
          <v-col cols="12">
            <v-card class="pa-4">
              <v-card-title class="d-flex align-center">
                <v-icon class="mr-2">mdi-play-circle</v-icon>
                수동 제어
              </v-card-title>
              <v-card-text>
                <v-btn 
                  color="primary" 
                  size="large"
                  :loading="executing"
                  :disabled="executing"
                  @click="executeBot"
                  class="mr-4"
                >
                  <v-icon left>mdi-play</v-icon>
                  수동 매매 실행
                </v-btn>
                
                <v-btn 
                  color="secondary" 
                  size="large"
                  :loading="refreshing"
                  @click="refreshIndicators"
                  class="mr-4"
                >
                  <v-icon left>mdi-refresh</v-icon>
                  지표 새로고침
                </v-btn>
                
                <v-btn 
                  color="info" 
                  size="large"
                  :loading="sendingTest"
                  @click="sendTestNotification"
                >
                  <v-icon left>mdi-bell</v-icon>
                  테스트 알림
                </v-btn>
              </v-card-text>
              
              <!-- 실행 결과 표시 -->
              <v-card-text v-if="executionResult">
                <v-alert :type="executionResult.status === 'SUCCESS' ? 'success' : 'warning'" class="mt-4">
                  <div class="font-weight-bold">실행 결과: {{ executionResult.status }}</div>
                  <div v-if="executionResult.message">{{ executionResult.message }}</div>
                  <div>매수: {{ executionResult.buyCount }}건 | 매도: {{ executionResult.sellCount }}건</div>
                </v-alert>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- 기술적 지표 테이블 -->
        <v-row>
          <v-col cols="12">
            <v-card>
              <v-card-title class="d-flex align-center">
                <v-icon class="mr-2">mdi-chart-line</v-icon>
                기술적 지표
                <v-spacer></v-spacer>
                <v-chip color="primary" class="mr-2">
                  마지막 업데이트: {{ lastUpdated }}
                </v-chip>
              </v-card-title>
              
              <v-card-text>
                <v-data-table
                  :headers="indicatorHeaders"
                  :items="indicators"
                  :loading="loading"
                  class="elevation-1"
                  item-key="market"
                >
                  <!-- 코인 심볼 -->
                  <template v-slot:item.market="{ item }">
                    <v-chip color="primary" small>
                      {{ item.market }}
                    </v-chip>
                  </template>
                  
                  <!-- 현재가 -->
                  <template v-slot:item.currentPrice="{ item }">
                    <span class="font-weight-bold">
                      {{ formatPrice(item.currentPrice) }}
                    </span>
                  </template>
                  
                  <!-- MA20 -->
                  <template v-slot:item.ma20="{ item }">
                    {{ item.ma20 ? formatPrice(item.ma20) : '-' }}
                  </template>
                  
                  <!-- RSI -->
                  <template v-slot:item.rsi="{ item }">
                    <v-chip 
                      :color="getRsiColor(item.rsi)" 
                      small
                      dark
                    >
                      {{ item.rsi ? item.rsi.toFixed(2) : '-' }}
                    </v-chip>
                  </template>
                  
                  <!-- 볼린저 위치 -->
                  <template v-slot:item.bbPosition="{ item }">
                    <v-chip 
                      :color="getBbPositionColor(item)" 
                      small
                      dark
                    >
                      {{ getBbPosition(item) }}
                    </v-chip>
                  </template>
                  
                  <!-- 거래량 비율 -->
                  <template v-slot:item.volumeRatio="{ item }">
                    <v-chip 
                      :color="item.volumeRatio >= 1.5 ? 'success' : 'grey'" 
                      small
                      dark
                    >
                      {{ item.volumeRatio ? (item.volumeRatio * 100).toFixed(0) + '%' : '-' }}
                    </v-chip>
                  </template>
                  
                  <!-- MA 대비 -->
                  <template v-slot:item.maPosition="{ item }">
                    <span :class="getMaPositionClass(item)">
                      {{ getMaPosition(item) }}
                    </span>
                  </template>
                  
                  <!-- 매수 신호 -->
                  <template v-slot:item.signal="{ item }">
                    <v-chip 
                      :color="getSignalColor(item)" 
                      small
                      dark
                    >
                      {{ getSignal(item) }}
                    </v-chip>
                  </template>
                </v-data-table>
              </v-card-text>
            </v-card>
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
import { ref, onMounted, computed } from 'vue'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'
import type { IndicatorResult, BotExecutionResult } from '@/types/bot'

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
const executing = ref(false)
const refreshing = ref(false)
const sendingTest = ref(false)
const indicators = ref<IndicatorResult[]>([])
const executionResult = ref<BotExecutionResult | null>(null)
const botRunning = ref(true)
const lastUpdated = ref('-')

const snackbar = ref(false)
const snackbarMessage = ref('')
const snackbarColor = ref('success')

// 오늘 통계
const todayStats = ref({
  buyCount: 0,
  sellCount: 0,
  profitLoss: 0
})

// 다음 실행 시간 계산
const nextExecution = computed(() => {
  const now = new Date()
  const minutes = now.getMinutes()
  const nextMinute = Math.ceil(minutes / 5) * 5
  const next = new Date(now)
  next.setMinutes(nextMinute, 0, 0)
  if (next <= now) {
    next.setMinutes(next.getMinutes() + 5)
  }
  return next.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
})

// 테이블 헤더
const indicatorHeaders = [
  { title: '코인', key: 'market', sortable: true },
  { title: '현재가', key: 'currentPrice', sortable: true },
  { title: 'MA20', key: 'ma20', sortable: true },
  { title: 'RSI', key: 'rsi', sortable: true },
  { title: 'BB 위치', key: 'bbPosition', sortable: false },
  { title: '거래량', key: 'volumeRatio', sortable: true },
  { title: 'MA 대비', key: 'maPosition', sortable: false },
  { title: '신호', key: 'signal', sortable: false },
]

// 메서드
const fetchIndicators = async () => {
  loading.value = true
  try {
    // 기본 코인 목록 (거래 설정에서 가져오거나 기본값 사용)
    const markets = ['KRW-BTC', 'KRW-ETH', 'KRW-XRP', 'KRW-SOL', 'KRW-DOGE']
    const response = await api.get(`/bot/indicators?markets=${markets.join(',')}`)
    indicators.value = response
    lastUpdated.value = new Date().toLocaleTimeString('ko-KR')
  } catch (error) {
    console.error('지표 조회 실패:', error)
    showSnackbar('지표 조회에 실패했습니다.', 'error')
  } finally {
    loading.value = false
  }
}

const executeBot = async () => {
  executing.value = true
  executionResult.value = null
  try {
    const result = await api.post('/bot/execute')
    executionResult.value = result
    showSnackbar(`실행 완료: 매수 ${result.buyCount}건, 매도 ${result.sellCount}건`, 'success')
    
    // 지표 새로고침
    await fetchIndicators()
  } catch (error) {
    console.error('봇 실행 실패:', error)
    showSnackbar('봇 실행에 실패했습니다.', 'error')
  } finally {
    executing.value = false
  }
}

const refreshIndicators = async () => {
  refreshing.value = true
  await fetchIndicators()
  refreshing.value = false
  showSnackbar('지표가 새로고침되었습니다.', 'success')
}

const sendTestNotification = async () => {
  sendingTest.value = true
  try {
    await api.post('/notifications/test')
    showSnackbar('테스트 알림이 발송되었습니다.', 'success')
  } catch (error) {
    console.error('알림 발송 실패:', error)
    showSnackbar('알림 발송에 실패했습니다.', 'error')
  } finally {
    sendingTest.value = false
  }
}

// 유틸리티 함수
const formatPrice = (price: number) => {
  if (!price) return '-'
  return new Intl.NumberFormat('ko-KR').format(price) + '원'
}

const getRsiColor = (rsi: number | null) => {
  if (!rsi) return 'grey'
  if (rsi <= 30) return 'success'  // 과매도 (매수 기회)
  if (rsi >= 70) return 'error'    // 과매수 (매도 기회)
  return 'grey'
}

const getBbPosition = (item: IndicatorResult) => {
  if (!item.currentPrice || !item.bbLower || !item.bbUpper) return '-'
  
  const range = item.bbUpper - item.bbLower
  const position = (item.currentPrice - item.bbLower) / range * 100
  
  if (position <= 20) return '하단'
  if (position >= 80) return '상단'
  return '중간'
}

const getBbPositionColor = (item: IndicatorResult) => {
  const position = getBbPosition(item)
  if (position === '하단') return 'success'
  if (position === '상단') return 'error'
  return 'grey'
}

const getMaPosition = (item: IndicatorResult) => {
  if (!item.currentPrice || !item.ma20) return '-'
  
  const diff = ((item.currentPrice - item.ma20) / item.ma20 * 100).toFixed(2)
  return `${Number(diff) > 0 ? '+' : ''}${diff}%`
}

const getMaPositionClass = (item: IndicatorResult) => {
  if (!item.currentPrice || !item.ma20) return ''
  return item.currentPrice < item.ma20 ? 'text-success' : 'text-error'
}

const getSignal = (item: IndicatorResult) => {
  let score = 0
  
  // RSI 체크
  if (item.rsi && item.rsi <= 30) score++
  
  // BB 하단 체크
  if (item.bbLower && item.currentPrice <= item.bbLower * 1.02) score++
  
  // MA 아래 체크
  if (item.ma20 && item.currentPrice < item.ma20) score++
  
  // 거래량 체크
  if (item.volumeRatio && item.volumeRatio >= 1.5) score++
  
  if (score >= 3) return '강력 매수'
  if (score >= 2) return '매수 검토'
  if (score === 1) return '관망'
  return '대기'
}

const getSignalColor = (item: IndicatorResult) => {
  const signal = getSignal(item)
  if (signal === '강력 매수') return 'success'
  if (signal === '매수 검토') return 'primary'
  if (signal === '관망') return 'warning'
  return 'grey'
}

const showSnackbar = (message: string, color: string) => {
  snackbarMessage.value = message
  snackbarColor.value = color
  snackbar.value = true
}

// 마운트 시 데이터 로드
onMounted(() => {
  fetchIndicators()
  
  // 30초마다 자동 새로고침
  setInterval(fetchIndicators, 30000)
})
</script>

<style scoped>
.text-success {
  color: #4CAF50 !important;
  font-weight: bold;
}

.text-error {
  color: #F44336 !important;
  font-weight: bold;
}
</style>