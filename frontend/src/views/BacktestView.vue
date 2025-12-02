<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main>
      <v-container fluid>
        <v-row>
          <v-col cols="12">
            <h1 class="text-h4 mb-4">
              <v-icon class="mr-2">mdi-chart-timeline-variant</v-icon>
              백테스팅
            </h1>
          </v-col>
        </v-row>

        <!-- 설정 폼 -->
        <v-row>
          <v-col cols="12" md="4">
            <v-card>
              <v-card-title>
                <v-icon class="mr-2">mdi-cog</v-icon>
                백테스트 설정
              </v-card-title>
              <v-card-text>
                <v-form ref="form" @submit.prevent="runBacktest">
                  <!-- 코인 선택 -->
                  <v-select
                    v-model="request.coinSymbols"
                    :items="availableCoins"
                    item-title="name"
                    item-value="symbol"
                    label="거래 코인"
                    multiple
                    chips
                    closable-chips
                    :rules="[v => v.length > 0 || '최소 1개 코인을 선택하세요']"
                  />

                  <!-- 기간 선택 -->
                  <v-row>
                    <v-col cols="6">
                      <v-text-field
                        v-model="request.startDate"
                        label="시작일"
                        type="date"
                        :rules="[v => !!v || '시작일을 선택하세요']"
                      />
                    </v-col>
                    <v-col cols="6">
                      <v-text-field
                        v-model="request.endDate"
                        label="종료일"
                        type="date"
                        :rules="[v => !!v || '종료일을 선택하세요']"
                      />
                    </v-col>
                  </v-row>

                  <!-- 초기 자본 -->
                  <v-text-field
                    v-model.number="request.initialBalance"
                    label="초기 투자금 (원)"
                    type="number"
                    :rules="[v => v >= 100000 || '최소 10만원 이상']"
                    suffix="원"
                  />

                  <v-divider class="my-4" />

                  <!-- 고급 설정 -->
                  <v-expansion-panels>
                    <v-expansion-panel>
                      <v-expansion-panel-title>
                        <v-icon class="mr-2">mdi-tune</v-icon>
                        고급 설정
                      </v-expansion-panel-title>
                      <v-expansion-panel-text>
                        <v-slider
                          v-model="request.basePeriod"
                          label="이동평균선 기간"
                          :min="7"
                          :max="30"
                          :step="1"
                          thumb-label
                          class="mt-4"
                        />

                        <v-slider
                          v-model="request.buyThresholdPct"
                          label="매수 기준 (MA 대비 %)"
                          :min="-20"
                          :max="0"
                          :step="0.5"
                          thumb-label
                        />

                        <v-slider
                          v-model="request.sellTargetPct"
                          label="목표 수익률 (%)"
                          :min="0.5"
                          :max="20"
                          :step="0.5"
                          thumb-label
                        />

                        <v-slider
                          v-model="request.stopLossPct"
                          label="손절매 기준 (%)"
                          :min="-30"
                          :max="0"
                          :step="0.5"
                          thumb-label
                        />

                        <v-slider
                          v-model="request.maxHoldingsPerCoin"
                          label="종목당 최대 보유"
                          :min="1"
                          :max="10"
                          :step="1"
                          thumb-label
                        />

                        <v-switch
                          v-model="request.useTrailingStop"
                          label="트레일링 스톱 사용"
                          color="primary"
                        />

                        <v-slider
                          v-if="request.useTrailingStop"
                          v-model="request.trailingStopPct"
                          label="트레일링 스톱 (%)"
                          :min="1"
                          :max="10"
                          :step="0.5"
                          thumb-label
                        />
                      </v-expansion-panel-text>
                    </v-expansion-panel>
                  </v-expansion-panels>

                  <v-btn
                    type="submit"
                    color="primary"
                    block
                    size="large"
                    class="mt-4"
                    :loading="loading"
                    :disabled="loading"
                  >
                    <v-icon class="mr-2">mdi-play</v-icon>
                    백테스트 실행
                  </v-btn>
                </v-form>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- 결과 영역 -->
          <v-col cols="12" md="8">
            <!-- 로딩 -->
            <v-card v-if="loading" class="text-center pa-8">
              <v-progress-circular indeterminate size="64" color="primary" />
              <p class="mt-4 text-h6">백테스트 실행 중...</p>
              <p class="text-grey">과거 데이터를 분석하고 있습니다.</p>
            </v-card>

            <!-- 결과 없음 -->
            <v-card v-else-if="!result" class="text-center pa-8">
              <v-icon size="64" color="grey">mdi-chart-box-outline</v-icon>
              <p class="mt-4 text-h6">백테스트 설정을 입력하고 실행하세요</p>
              <p class="text-grey">과거 데이터를 기반으로 거래 전략을 시뮬레이션합니다.</p>
            </v-card>

            <!-- 결과 표시 -->
            <template v-else>
              <!-- 요약 카드 -->
              <v-row>
                <v-col cols="6" md="3">
                  <v-card :color="result.totalProfit >= 0 ? 'success' : 'error'" variant="tonal">
                    <v-card-text class="text-center">
                      <div class="text-overline">총 수익</div>
                      <div class="text-h5 font-weight-bold">
                        {{ formatCurrency(result.totalProfit) }}
                      </div>
                      <div :class="result.totalProfitRate >= 0 ? 'text-success' : 'text-error'">
                        {{ result.totalProfitRate >= 0 ? '+' : '' }}{{ result.totalProfitRate.toFixed(2) }}%
                      </div>
                    </v-card-text>
                  </v-card>
                </v-col>
                <v-col cols="6" md="3">
                  <v-card variant="tonal">
                    <v-card-text class="text-center">
                      <div class="text-overline">승률</div>
                      <div class="text-h5 font-weight-bold">{{ result.winRate.toFixed(1) }}%</div>
                      <div class="text-grey">{{ result.winCount }}승 {{ result.loseCount }}패</div>
                    </v-card-text>
                  </v-card>
                </v-col>
                <v-col cols="6" md="3">
                  <v-card variant="tonal">
                    <v-card-text class="text-center">
                      <div class="text-overline">총 거래</div>
                      <div class="text-h5 font-weight-bold">{{ result.totalTrades }}회</div>
                      <div class="text-grey">매수 {{ result.buyCount }} / 매도 {{ result.sellCount }}</div>
                    </v-card-text>
                  </v-card>
                </v-col>
                <v-col cols="6" md="3">
                  <v-card color="warning" variant="tonal">
                    <v-card-text class="text-center">
                      <div class="text-overline">최대 낙폭</div>
                      <div class="text-h5 font-weight-bold">-{{ result.maxDrawdown.toFixed(2) }}%</div>
                      <div class="text-grey">MDD</div>
                    </v-card-text>
                  </v-card>
                </v-col>
              </v-row>

              <!-- 상세 지표 -->
              <v-card class="mt-4">
                <v-card-title>
                  <v-icon class="mr-2">mdi-chart-bar</v-icon>
                  상세 지표
                </v-card-title>
                <v-card-text>
                  <v-row>
                    <v-col cols="6" md="3">
                      <div class="text-overline">초기 자본</div>
                      <div class="text-body-1">{{ formatCurrency(result.initialBalance) }}</div>
                    </v-col>
                    <v-col cols="6" md="3">
                      <div class="text-overline">최종 자본</div>
                      <div class="text-body-1">{{ formatCurrency(result.finalBalance) }}</div>
                    </v-col>
                    <v-col cols="6" md="3">
                      <div class="text-overline">평균 수익</div>
                      <div class="text-body-1 text-success">{{ formatCurrency(result.avgProfit) }}</div>
                    </v-col>
                    <v-col cols="6" md="3">
                      <div class="text-overline">평균 손실</div>
                      <div class="text-body-1 text-error">-{{ formatCurrency(result.avgLoss) }}</div>
                    </v-col>
                    <v-col cols="6" md="3">
                      <div class="text-overline">손익비</div>
                      <div class="text-body-1">{{ result.profitFactor.toFixed(2) }}</div>
                    </v-col>
                    <v-col cols="6" md="3">
                      <div class="text-overline">샤프 비율</div>
                      <div class="text-body-1">{{ result.sharpeRatio.toFixed(2) }}</div>
                    </v-col>
                    <v-col cols="6" md="3">
                      <div class="text-overline">테스트 기간</div>
                      <div class="text-body-1">{{ result.totalDays }}일</div>
                    </v-col>
                    <v-col cols="6" md="3">
                      <div class="text-overline">테스트 코인</div>
                      <div class="text-body-1">{{ result.coinSymbols.length }}개</div>
                    </v-col>
                  </v-row>
                </v-card-text>
              </v-card>

 	 <!-- 자산 변동 차트 -->
              <v-card class="mt-4">
                <v-card-title>
                  <v-icon class="mr-2">mdi-chart-line</v-icon>
                  자산 변동 추이
                </v-card-title>
                <v-card-text>
                  <div 
                    class="chart-wrapper"
                    @mousemove="handleChartHover"
                    @mouseleave="hoveredIndex = -1"
                  >
                    <!-- 초기 투자금 기준선 -->
                    <div 
                      class="baseline"
                      :style="{ bottom: baselinePosition + '%' }"
                    >
                      <span class="baseline-label">초기: {{ formatCurrency(result.initialBalance) }}</span>
                    </div>
                    <v-sparkline
                      :model-value="chartBalances"
                      :gradient="['#1976D2', '#42A5F5']"
                      :line-width="2"
                      :padding="16"
                      :height="150"
                      :fill="true"
                      auto-draw
                      smooth
                    />
                    <!-- 툴팁 -->
                    <div 
                      v-if="hoveredIndex >= 0 && hoveredData"
                      class="chart-tooltip"
                      :style="{ left: tooltipX + 'px' }"
                    >
                      <div class="font-weight-bold">{{ hoveredData.date }}</div>
                      <div>자산: {{ formatCurrency(hoveredData.balance) }}</div>
                      <div :class="hoveredData.profitRate >= 0 ? 'text-success' : 'text-error'">
                        수익률: {{ hoveredData.profitRate >= 0 ? '+' : '' }}{{ hoveredData.profitRate.toFixed(2) }}%
                      </div>
                    </div>
                  </div>
                  <div class="d-flex justify-space-between text-caption text-grey mt-2">
                    <span>{{ result.startDate }}</span>
                    <span>{{ result.endDate }}</span>
                  </div>
                </v-card-text>
              </v-card>

              <!-- 코인별 성과 -->
              <v-card class="mt-4">
                <v-card-title>
                  <v-icon class="mr-2">mdi-bitcoin</v-icon>
                  코인별 성과
                </v-card-title>
                <v-card-text>
                  <v-data-table
                    :headers="coinHeaders"
                    :items="result.coinPerformances"
                    density="compact"
                  >
                    <template #item.coinSymbol="{ item }">
                      <v-chip size="small" color="primary" variant="outlined">
                        {{ item.coinSymbol }}
                      </v-chip>
                    </template>
                    <template #item.totalProfit="{ item }">
                      <span :class="item.totalProfit >= 0 ? 'text-success' : 'text-error'">
                        {{ formatCurrency(item.totalProfit) }}
                      </span>
                    </template>
                    <template #item.profitRate="{ item }">
                      <span :class="item.profitRate >= 0 ? 'text-success' : 'text-error'">
                        {{ item.profitRate >= 0 ? '+' : '' }}{{ item.profitRate.toFixed(2) }}%
                      </span>
                    </template>
                    <template #item.winRate="{ item }">
                      {{ item.tradeCount > 0 ? ((item.winCount / (item.winCount + item.loseCount)) * 100).toFixed(1) : 0 }}%
                    </template>
                  </v-data-table>
                </v-card-text>
              </v-card>

              <!-- 거래 내역 -->
              <v-card class="mt-4">
                <v-card-title>
                  <v-icon class="mr-2">mdi-format-list-bulleted</v-icon>
                  거래 내역
                  <v-chip class="ml-2" size="small">{{ result.trades.length }}건</v-chip>
                </v-card-title>
                <v-card-text>
                  <v-data-table
                    :headers="tradeHeaders"
                    :items="result.trades"
                    :items-per-page="10"
                    density="compact"
                  >
                    <template #item.type="{ item }">
                      <v-chip 
                        :color="item.type === 'BUY' ? 'success' : 'error'" 
                        size="small"
                      >
                        {{ item.type === 'BUY' ? '매수' : '매도' }}
                      </v-chip>
                    </template>
                    <template #item.coinSymbol="{ item }">
                      <span class="font-weight-medium">{{ item.coinSymbol }}</span>
                    </template>
                    <template #item.price="{ item }">
                      {{ formatCurrency(item.price) }}
                    </template>
                    <template #item.amount="{ item }">
                      {{ formatCurrency(item.amount) }}
                    </template>
                    <template #item.profit="{ item }">
                      <span v-if="item.profit !== null" :class="item.profit >= 0 ? 'text-success' : 'text-error'">
                        {{ item.profit >= 0 ? '+' : '' }}{{ formatCurrency(item.profit) }}
                      </span>
                      <span v-else>-</span>
                    </template>
                    <template #item.profitRate="{ item }">
                      <span v-if="item.profitRate !== null" :class="item.profitRate >= 0 ? 'text-success' : 'text-error'">
                        {{ item.profitRate >= 0 ? '+' : '' }}{{ item.profitRate.toFixed(2) }}%
                      </span>
                      <span v-else>-</span>
                    </template>
                  </v-data-table>
                </v-card-text>
              </v-card>
            </template>
          </v-col>
        </v-row>
      </v-container>
    </v-main>

    <!-- 스낵바 -->
    <v-snackbar v-model="snackbar.show" :color="snackbar.color" :timeout="3000">
      {{ snackbar.message }}
    </v-snackbar>
  </v-app>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { backtestApi } from '@/api'
import type { BacktestResult, AvailableCoin } from '@/types/backtest'

import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'

// 사이드바 ref
const sidebarRef = ref()

// 차트 호버 상태
const hoveredIndex = ref(-1)
const tooltipX = ref(0)

// 상태
const loading = ref(false)
const availableCoins = ref<AvailableCoin[]>([])
const result = ref<BacktestResult | null>(null)


// 요청 데이터
const request = ref({
  coinSymbols: ['KRW-BTC', 'KRW-ETH'],
  startDate: getDateString(-30),
  endDate: getDateString(-1),
  initialBalance: 1000000,
  basePeriod: 20,
  buyThresholdPct: -3,
  sellTargetPct: 3,
  stopLossPct: -5,
  maxHoldingsPerCoin: 3,
  useTrailingStop: false,
  trailingStopPct: 5,
})

// 스낵바
const snackbar = ref({
  show: false,
  message: '',
  color: 'success',
})

// 테이블 헤더
const coinHeaders = [
  { title: '코인', key: 'coinSymbol' },
  { title: '거래 횟수', key: 'tradeCount' },
  { title: '승/패', key: 'winRate' },
  { title: '총 손익', key: 'totalProfit' },
  { title: '수익률', key: 'profitRate' },
]

const tradeHeaders = [
  { title: '유형', key: 'type', width: '80px' },
  { title: '코인', key: 'coinSymbol' },
  { title: '일자', key: 'tradeDate', width: '110px' },
  { title: '가격', key: 'price' },
  { title: '금액', key: 'amount' },
  { title: '손익', key: 'profit' },
  { title: '수익률', key: 'profitRate', width: '90px' },
  { title: '신호', key: 'signal' },
]

// 차트 데이터
import { computed } from 'vue'

const chartBalances = computed(() => {
  if (!result.value?.dailyBalances) return []
  return result.value.dailyBalances.map(d => d.balance)
})

const hoveredData = computed(() => {
  if (hoveredIndex.value < 0 || !result.value?.dailyBalances) return null
  return result.value.dailyBalances[hoveredIndex.value]
})

const baselinePosition = computed(() => {
  if (!result.value?.dailyBalances?.length) return 50
  const balances = result.value.dailyBalances.map(d => d.balance)
  const min = Math.min(...balances)
  const max = Math.max(...balances)
  if (max === min) return 50
  const initial = result.value.initialBalance
  // 차트 padding 고려 (상하 약 10%)
  const position = ((initial - min) / (max - min)) * 80 + 10
  return Math.max(5, Math.min(95, position))
})

// 초기화
onMounted(async () => {
  await fetchAvailableCoins()
})

// 코인 목록 조회
const fetchAvailableCoins = async () => {
  try {
    const response = await backtestApi.getAvailableCoins()
    availableCoins.value = response.data.coins
  } catch (error) {
    console.error('코인 목록 조회 실패:', error)
    // 기본값 설정
    availableCoins.value = [
      { symbol: 'KRW-BTC', name: '비트코인' },
      { symbol: 'KRW-ETH', name: '이더리움' },
      { symbol: 'KRW-XRP', name: '리플' },
      { symbol: 'KRW-SOL', name: '솔라나' },
      { symbol: 'KRW-DOGE', name: '도지코인' },
    ]
  }
}

// 백테스트 실행
const runBacktest = async () => {
  loading.value = true
  result.value = null

  try {
    const response = await backtestApi.run(request.value)
    result.value = response.data
    showSnackbar('백테스트가 완료되었습니다.', 'success')
  } catch (error: any) {
    console.error('백테스트 실패:', error)
    showSnackbar(error.response?.data?.message || '백테스트 실행에 실패했습니다.', 'error')
  } finally {
    loading.value = false
  }
}


// 유틸리티 함수
function getDateString(daysOffset: number): string {
  const date = new Date()
  date.setDate(date.getDate() + daysOffset)
  return date.toISOString().split('T')[0]
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('ko-KR').format(Math.round(value)) + '원'
}

function showSnackbar(message: string, color: string) {
  snackbar.value = { show: true, message, color }
}

function handleChartHover(event: MouseEvent) {
  if (!result.value?.dailyBalances?.length) return
  
  const target = event.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const x = event.clientX - rect.left
  const width = rect.width
  
  const index = Math.floor((x / width) * result.value.dailyBalances.length)
  hoveredIndex.value = Math.max(0, Math.min(index, result.value.dailyBalances.length - 1))
  tooltipX.value = Math.min(Math.max(x, 60), width - 60)
}

</script>

<style scoped>
.chart-wrapper {
  position: relative;
  cursor: crosshair;
}

.chart-tooltip {
  position: absolute;
  top: 10px;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 12px;
  pointer-events: none;
  z-index: 10;
  white-space: nowrap;
}
.baseline {
  position: absolute;
  left: 16px;
  right: 16px;
  border-top: 2px dashed #FF9800;
  pointer-events: none;
  z-index: 5;
}

.baseline-label {
  position: absolute;
  right: 0;
  top: -18px;
  font-size: 11px;
  color: #FF9800;
  background: white;
  padding: 0 4px;
  font-weight: 500;
}
</style>