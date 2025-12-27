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
            <v-card class="pa-4 bot-stats-card" :color="botRunning ? 'success' : 'grey'" dark>
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
            <v-card class="pa-4 bot-stats-card" color="primary" dark>
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
            <v-card class="pa-4 bot-stats-card" color="info" dark>
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
            <v-card class="pa-4 bot-stats-card" color="warning" dark>
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

        <!-- 수동 제어 + 테스트 발송 카드 (한 줄 배치) -->
        <v-row class="mb-4">
          <!-- 수동 제어 -->
          <v-col cols="12" md="4">
            <v-card class="pa-4 control-card">
              <v-card-title class="d-flex align-center pb-0">
                <v-icon class="mr-2" color="primary">mdi-account-cog</v-icon>
                수동 제어
              </v-card-title>
              <v-card-text class="pt-6">
                <div class="d-flex flex-column gap-3">
                  <v-btn 
                    color="primary" 
                    variant="outlined"
                    size="large"
                    block
                    :loading="executing"
                    :disabled="executing"
                    @click="executeBot"
                  >
                    <v-icon left>mdi-play</v-icon>
                    수동 매매 실행
                  </v-btn>
                  
                  <v-btn 
                    color="grey-darken-2" 
                    variant="outlined"
                    size="large"
                    block
                    :loading="refreshing"
                    @click="refreshIndicators"
                  >
                    <v-icon left>mdi-refresh</v-icon>
                    지표 새로고침
                  </v-btn>
                </div>
                
                <!-- 실행 결과 표시 (수동 제어 카드 내부로 이동) -->
                <v-alert 
                  v-if="executionResult" 
                  :type="executionResult.status === 'SUCCESS' ? 'success' : 'error'" 
                  variant="tonal"
                  class="mt-4"
                >
                  <div class="font-weight-bold">{{ executionResult.message }}</div>
                  <div v-if="executionResult.buyCount > 0 || executionResult.sellCount > 0">
                    매수: {{ executionResult.buyCount }}건 / 매도: {{ executionResult.sellCount }}건
                  </div>
                </v-alert>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- 이메일 테스트 발송 -->
          <v-col cols="12" md="4">
            <v-card class="pa-4 control-card">
              <v-card-title class="d-flex align-center pb-0">
                <v-icon class="mr-2" color="success">mdi-email</v-icon>
                이메일 테스트 발송
              </v-card-title>
              <v-card-text class="pt-6">
                <v-alert 
                  v-if="!userProfile.email" 
                  type="warning" 
                  variant="tonal" 
                  density="compact"
                  class="mb-4"
                >
                  이메일 미등록
                  <v-btn 
                    variant="flat" 
                    color="primary" 
                    size="small" 
                    to="/profile"
                    class="ml-2"
                  >
                    등록하기
                  </v-btn>
                </v-alert>
                
                <div class="d-flex flex-column gap-3">
                  <v-btn
                    color="teal"
                    variant="outlined"
                    size="large"
                    block
                    :loading="emailTestLoading.buy"
                    :disabled="!userProfile.email"
                    @click="sendEmailTest('buy')"
                  >
                    <v-icon left>mdi-cart-arrow-down</v-icon>
                    매수 체결
                  </v-btn>
  
                  <v-btn
                    color="teal"
                    variant="outlined"
                    size="large"
                    block
                    :loading="emailTestLoading.sell"
                    :disabled="!userProfile.email"
                    @click="sendEmailTest('sell')"
                  >
                    <v-icon left>mdi-cart-arrow-up</v-icon>
                    매도 체결
                  </v-btn>
  
                  <v-btn
                    color="teal"
                    variant="outlined"
                    size="large"
                    block
                    :loading="emailTestLoading.report"
                    :disabled="!userProfile.email"
                    @click="sendEmailTest('report')"
                  >
                    <v-icon left>mdi-file-chart</v-icon>
                    일간 리포트
                  </v-btn>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
          
          <!-- 디스코드 DM 테스트 발송 -->
          <v-col cols="12" md="4">
            <v-card class="pa-4 control-card">
              <v-card-title class="d-flex align-center pb-0">
                <v-icon class="mr-2" color="deep-purple">mdi-robot</v-icon>
                디스코드 DM 테스트
                <v-chip 
                  :color="discordBotEnabled ? 'success' : 'grey'" 
                  size="small" 
                  class="ml-2"
                >
                  {{ discordBotEnabled ? 'Bot 활성화' : 'Bot 비활성화' }}
                </v-chip>
              </v-card-title>
              <v-card-text class="pt-6">
                <v-alert 
                  v-if="!userProfile.discordUserId" 
                  type="warning" 
                  variant="tonal" 
                  density="compact"
                  class="mb-4"
                >
                  Discord ID 미등록
                  <v-btn 
                    variant="flat" 
                    color="deep-purple" 
                    size="small" 
                    to="/profile"
                    class="ml-2"
                  >
                    등록하기
                  </v-btn>
                </v-alert>
                
                <v-row dense>
                  <v-col cols="6">
                    <v-btn
                      color="deep-purple"
                      variant="outlined"
                      size="large"
                      block
                      :loading="discordTestLoading.buy"
                      :disabled="!userProfile.discordUserId || !discordBotEnabled"
                      @click="sendDiscordTest('buy')"
                    >
                      <v-icon left>mdi-cart-arrow-down</v-icon>
                      매수
                    </v-btn>
                  </v-col>
                  <v-col cols="6">
                    <v-btn
                      color="deep-purple"
                      variant="outlined"
                      size="large"
                      block
                      :loading="discordTestLoading.sell"
                      :disabled="!userProfile.discordUserId || !discordBotEnabled"
                      @click="sendDiscordTest('sell')"
                    >
                      <v-icon left>mdi-cart-arrow-up</v-icon>
                      매도
                    </v-btn>
                  </v-col>
                  <v-col cols="6">
                    <v-btn
                      color="deep-purple"
                      variant="outlined"
                      size="large"
                      block
                      :loading="discordTestLoading.stoploss"
                      :disabled="!userProfile.discordUserId || !discordBotEnabled"
                      @click="sendDiscordTest('stoploss')"
                    >
                      <v-icon left>mdi-alert</v-icon>
                      손절매
                    </v-btn>
                  </v-col>
                  <v-col cols="6">
                    <v-btn
                      color="deep-purple"
                      variant="outlined"
                      size="large"
                      block
                      :loading="discordTestLoading.report"
                      :disabled="!userProfile.discordUserId || !discordBotEnabled"
                      @click="sendDiscordTest('report')"
                    >
                      <v-icon left>mdi-file-chart</v-icon>
                      리포트
                    </v-btn>
                  </v-col>
                </v-row>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- 기술적 지표 테이블 -->
        <v-row>
          <v-col cols="12">
            <v-card>
              <v-card-title class="d-flex justify-space-between align-center">
                <span>
                  <v-icon class="mr-2">mdi-chart-line</v-icon>
                  기술적 지표
                </span>
                <span class="text-caption text-grey">
                  마지막 업데이트: {{ lastUpdated }}
                </span>
              </v-card-title>
              
              <v-data-table
                :headers="indicatorHeaders"
                :items="indicators"
                :loading="loading"
                class="elevation-0"
              >
                <template v-slot:item.market="{ item }">
                  <div class="font-weight-medium">{{ item.market }}</div>
                </template>

                <template v-slot:item.currentPrice="{ item }">
                  {{ formatPrice(item.currentPrice) }}
                </template>

                <template v-slot:item.ma20="{ item }">
                  {{ formatPrice(item.ma20) }}
                </template>

                <template v-slot:item.rsi="{ item }">
                  <v-chip :color="getRsiColor(item.rsi)" size="small">
                    {{ item.rsi ? item.rsi.toFixed(1) : '-' }}
                  </v-chip>
                </template>

                <template v-slot:item.bbPosition="{ item }">
                  <v-chip :color="getBbPositionColor(item)" size="small">
                    {{ getBbPosition(item) }}
                  </v-chip>
                </template>

                <template v-slot:item.volumeRatio="{ item }">
                  <span :class="item.volumeRatio >= 1.5 ? 'text-success font-weight-bold' : ''">
                    {{ item.volumeRatio ? `${item.volumeRatio.toFixed(2)}x` : '-' }}
                  </span>
                </template>

                <template v-slot:item.maPosition="{ item }">
                  <span :class="getMaPositionClass(item)">
                    {{ getMaPosition(item) }}
                  </span>
                </template>

                <template v-slot:item.signal="{ item }">
                  <v-chip :color="getSignalColor(item)" size="small">
                    {{ getSignal(item) }}
                  </v-chip>
                </template>
              </v-data-table>
            </v-card>
          </v-col>
        </v-row>

      </v-container>
    </v-main>

    <v-snackbar v-model="snackbar" :color="snackbarColor" :timeout="3000">
      {{ snackbarMessage }}
    </v-snackbar>
  </v-app>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'
import api from '@/api'
import type { IndicatorResult } from '@/types/bot'

const sidebarRef = ref()

// 상태 변수
const loading = ref(false)
const executing = ref(false)
const refreshing = ref(false)
const indicators = ref<IndicatorResult[]>([])
const lastUpdated = ref('')
const executionResult = ref<any>(null)

// Snackbar
const snackbar = ref(false)
const snackbarMessage = ref('')
const snackbarColor = ref('success')

// 오늘 통계
const todayStats = ref({
  buyCount: 0,
  sellCount: 0
})

const botRunning = ref(true)

// 사용자 프로필 정보
const userProfile = ref({
  email: '',
  discordUserId: ''
})

// Discord Bot 상태
const discordBotEnabled = ref(false)

// 이메일 테스트 로딩 상태
const emailTestLoading = ref({
  buy: false,
  sell: false,
  report: false
})

// 디스코드 테스트 로딩 상태
const discordTestLoading = ref({
  buy: false,
  sell: false,
  stoploss: false,
  report: false
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

// 사용자 프로필 조회
const fetchUserProfile = async () => {
  try {
    // ⭐ 수정: /users/me → /user/profile (올바른 엔드포인트)
    const response = await api.get('/user/profile')
    // API 응답 구조에 따라 data 접근 방식 처리
    const userData = response.data || response
    userProfile.value = {
      email: userData.email || '',
      discordUserId: userData.discordUserId || ''
    }
  } catch (error) {
    console.error('프로필 조회 실패:', error)
    // 에러 시에도 기본값 유지
    userProfile.value = {
      email: '',
      discordUserId: ''
    }
  }
}

// Discord Bot 상태 조회
const fetchDiscordBotStatus = async () => {
  try {
    const response = await api.get('/notifications/discord/bot-status')
    // API 응답 구조에 따라 data 접근 방식 처리
    const statusData = response.data || response
    discordBotEnabled.value = statusData?.botEnabled || false
  } catch (error) {
    console.error('Discord Bot 상태 조회 실패:', error)
    discordBotEnabled.value = false
  }
}

// 지표 조회
const fetchIndicators = async () => {
  loading.value = true
  try {
    const markets = ['KRW-BTC', 'KRW-ETH', 'KRW-XRP', 'KRW-SOL', 'KRW-DOGE']
    const response = await api.get(`/bot/indicators?markets=${markets.join(',')}`)
    // API 응답 구조에 따라 data 접근 방식 처리
    const indicatorData = response.data || response
    indicators.value = Array.isArray(indicatorData) ? indicatorData : []
    lastUpdated.value = new Date().toLocaleTimeString('ko-KR')
  } catch (error) {
    console.error('지표 조회 실패:', error)
    showSnackbar('지표 조회에 실패했습니다.', 'error')
    indicators.value = []
  } finally {
    loading.value = false
  }
}

// 봇 실행
const executeBot = async () => {
  executing.value = true
  executionResult.value = null
  try {
    const result = await api.post('/bot/execute')
    executionResult.value = result
    showSnackbar(`실행 완료: 매수 ${result.buyCount}건, 매도 ${result.sellCount}건`, 'success')
    await fetchIndicators()
  } catch (error) {
    console.error('봇 실행 실패:', error)
    showSnackbar('봇 실행에 실패했습니다.', 'error')
  } finally {
    executing.value = false
  }
}

// 지표 새로고침
const refreshIndicators = async () => {
  refreshing.value = true
  await fetchIndicators()
  refreshing.value = false
  showSnackbar('지표가 새로고침되었습니다.', 'success')
}

// 이메일 테스트 발송
const sendEmailTest = async (type: 'buy' | 'sell' | 'report') => {
  emailTestLoading.value[type] = true
  try {
    let endpoint = ''
    let successMsg = ''
    
    switch (type) {
      case 'buy':
        endpoint = '/notifications/email/test-buy'
        successMsg = '매수 체결 테스트 이메일이 발송되었습니다.'
        break
      case 'sell':
        endpoint = '/notifications/email/test-sell'
        successMsg = '매도 체결 테스트 이메일이 발송되었습니다.'
        break
      case 'report':
        endpoint = '/notifications/email/daily-report'
        successMsg = '일간 리포트 테스트 이메일이 발송되었습니다.'
        break
    }
    
    await api.post(endpoint)
    showSnackbar(successMsg, 'success')
  } catch (error) {
    console.error('이메일 테스트 발송 실패:', error)
    showSnackbar('이메일 발송에 실패했습니다.', 'error')
  } finally {
    emailTestLoading.value[type] = false
  }
}

// 디스코드 테스트 발송
const sendDiscordTest = async (type: 'buy' | 'sell' | 'stoploss' | 'report') => {
  discordTestLoading.value[type] = true
  try {
    let endpoint = ''
    let successMsg = ''
    
    switch (type) {
      case 'buy':
        endpoint = '/notifications/discord/test-buy'
        successMsg = '매수 알림 테스트 DM이 발송되었습니다.'
        break
      case 'sell':
        endpoint = '/notifications/discord/test-sell'
        successMsg = '매도 알림 테스트 DM이 발송되었습니다.'
        break
      case 'stoploss':
        endpoint = '/notifications/discord/test-stoploss'
        successMsg = '손절매 알림 테스트 DM이 발송되었습니다.'
        break
      case 'report':
        endpoint = '/notifications/discord/test-daily-report'
        successMsg = '일간 리포트 테스트 DM이 발송되었습니다.'
        break
    }
    
    await api.post(endpoint)
    showSnackbar(successMsg, 'success')
  } catch (error) {
    console.error('디스코드 테스트 발송 실패:', error)
    showSnackbar('디스코드 DM 발송에 실패했습니다.', 'error')
  } finally {
    discordTestLoading.value[type] = false
  }
}

// 유틸리티 함수
const formatPrice = (price: number) => {
  if (!price) return '-'
  return new Intl.NumberFormat('ko-KR').format(price) + '원'
}

const getRsiColor = (rsi: number | null) => {
  if (!rsi) return 'grey'
  if (rsi <= 30) return 'success'
  if (rsi >= 70) return 'error'
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
  
  if (item.rsi && item.rsi <= 30) score++
  if (item.bbLower && item.currentPrice <= item.bbLower * 1.02) score++
  if (item.ma20 && item.currentPrice < item.ma20) score++
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
  fetchUserProfile()  
  fetchDiscordBotStatus() 
  
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

.bot-stats-card {
  height: 100%;
  min-height: 120px;
}

.bot-stats-card .d-flex {
  height: 100%;
  align-items: center;
}

.bot-stats-card .text-h4,
.bot-stats-card .text-h5 {
  white-space: nowrap;
}

.gap-2 {
  gap: 8px;
}

.control-card {
  height: 100%;
  min-height: 280px;
}

.gap-3 {
  gap: 12px;
}
</style>