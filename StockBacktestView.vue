<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main class="bg-grey-lighten-3">
      <v-container fluid>
        <v-row class="mb-4">
          <v-col>
            <h1 class="text-h4">
              <v-icon class="mr-2">mdi-chart-timeline-variant</v-icon>
              주식/ETF 백테스팅
            </h1>
            <p class="text-subtitle-1 text-grey">과거 데이터로 주식/ETF 거래 전략의 수익률을 시뮬레이션하세요</p>
          </v-col>
        </v-row>

        <v-row>
          <!-- 설정 폼 -->
          <v-col cols="12" md="4">
            <v-card>
              <v-card-title class="d-flex align-center">
                <v-icon class="mr-2">mdi-cog</v-icon>
                백테스트 설정
              </v-card-title>
              <v-card-text>
                <v-form ref="form" @submit.prevent="runBacktest">
                  <!-- 종목 선택 -->
                  <v-autocomplete
                    v-model="request.stockCodes"
                    :items="availableStocks"
                    item-value="stockCode"
                    label="거래 종목"
                    multiple
                    chips
                    closable-chips
                    :rules="[v => v.length > 0 || '최소 1개 종목을 선택하세요']"
                    hint="레버리지 ETF 우선"
                    persistent-hint
                  >
                    <template v-slot:item="{ props, item }">
                      <v-list-item v-bind="props">
                        <template v-slot:prepend>
                          <v-chip size="x-small" :color="etfTypeColor(item.raw.etfType)" variant="tonal" class="mr-2">
                            {{ etfTypeLabel(item.raw.etfType) }}
                          </v-chip>
                        </template>
                        <template v-slot:title>{{ item.raw.stockName }}</template>
                        <template v-slot:subtitle>{{ item.raw.stockCode }}</template>
                      </v-list-item>
                    </template>
                    <template v-slot:chip="{ props, item }">
                      <v-chip v-bind="props" closable>{{ item.raw.stockName }}</v-chip>
                    </template>
                  </v-autocomplete>

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
                  <div class="text-caption text-grey-darken-1 mb-2">
                    ※ 주식 백테스트는 KIS API 특성상 최대 1년까지 지원합니다.
                  </div>

                  <!-- 초기 자본 -->
                  <v-text-field
                    v-model.number="request.initialBalance"
                    label="초기 투자금 (원)"
                    type="number"
                    :rules="[v => v >= 100000 || '최소 10만원 이상']"
                    suffix="원"
                  />

                  <v-divider class="my-4" />

                  <v-expansion-panels variant="accordion">
                    <!-- 매수/매도 조건 -->
                    <v-expansion-panel title="매수/매도 조건">
                      <template v-slot:text>
                        <div class="mb-3">
                          <span class="text-caption text-grey">이동평균선 기간: {{ request.basePeriod }}일</span>
                          <v-slider v-model="request.basePeriod" :min="7" :max="30" :step="1" thumb-label />
                        </div>
                        <div class="mb-3">
                          <span class="text-caption text-grey">매수 기준 (MA 대비): {{ request.buyThresholdPct }}%</span>
                          <v-slider v-model="request.buyThresholdPct" :min="-20" :max="0" :step="0.5" thumb-label />
                        </div>
                        <div class="mb-3">
                          <span class="text-caption text-grey">목표 수익률: {{ request.sellTargetPct }}%</span>
                          <v-slider v-model="request.sellTargetPct" :min="0.5" :max="20" :step="0.1" thumb-label />
                        </div>
                        <v-switch v-model="request.useStopLoss" label="손절매 사용" color="primary" density="compact" hide-details class="mb-2" />
                        <div v-if="request.useStopLoss" class="mb-3">
                          <span class="text-caption text-grey">손절매 기준: {{ request.stopLossPct }}%</span>
                          <v-slider v-model="request.stopLossPct" :min="-30" :max="0" :step="0.5" thumb-label />
                        </div>
                        <v-switch v-model="request.useTrailingStop" label="트레일링 스톱 사용" color="primary" density="compact" hide-details class="mb-2" />
                        <div v-if="request.useTrailingStop" class="mb-3">
                          <span class="text-caption text-grey">트레일링 스톱: {{ request.trailingStopPct }}%</span>
                          <v-slider v-model="request.trailingStopPct" :min="1" :max="10" :step="0.5" thumb-label />
                        </div>
                        <div class="mb-3">
                          <span class="text-caption text-grey">종목당 최대 보유: {{ request.maxHoldingsPerStock }}건</span>
                          <v-slider v-model="request.maxHoldingsPerStock" :min="1" :max="10" :step="1" thumb-label />
                        </div>
                        <div class="mb-1">
                          <span class="text-caption text-grey">추가 매수(물타기) 하락률: -{{ request.additionalDropPct }}%</span>
                          <v-slider v-model="request.additionalDropPct" :min="0" :max="10" :step="0.5" thumb-label />
                        </div>
                      </template>
                    </v-expansion-panel>

                    <!-- ⭐ 레버리지 ETF decay 방지 (Day 62 신규) -->
                    <v-expansion-panel title="레버리지 ETF 보유기간 제한">
                      <template v-slot:text>
                        <div class="text-caption text-grey mb-2 d-flex align-center">
                          <v-icon size="small" class="mr-1" color="warning">mdi-alert</v-icon>
                          변동성 끌림(decay) 방지: 보유일 도달 시 강제 매도
                        </div>
                        <span class="text-caption text-grey">최대 보유 거래일: {{ request.maxHoldingDays }}일</span>
                        <v-slider v-model="request.maxHoldingDays" :min="1" :max="60" :step="1" thumb-label />
                      </template>
                    </v-expansion-panel>

                    <!-- 기술적 지표 -->
                    <v-expansion-panel title="기술적 지표 설정">
                      <template v-slot:text>
                        <div class="mb-3">
                          <span class="text-caption text-grey">RSI 기간: {{ request.rsiPeriod }}일</span>
                          <v-slider v-model="request.rsiPeriod" :min="5" :max="50" :step="1" thumb-label />
                        </div>
                        <div class="mb-3">
                          <span class="text-caption text-grey">RSI 매수 임계값: {{ request.rsiBuyThreshold }}</span>
                          <v-slider v-model="request.rsiBuyThreshold" :min="10" :max="50" :step="1" thumb-label />
                        </div>
                        <div class="mb-3">
                          <span class="text-caption text-grey">RSI 매도 임계값: {{ request.rsiSellThreshold }}</span>
                          <v-slider v-model="request.rsiSellThreshold" :min="50" :max="90" :step="1" thumb-label />
                        </div>
                        <div class="mb-3">
                          <span class="text-caption text-grey">볼린저 밴드 기간: {{ request.bbPeriod }}일</span>
                          <v-slider v-model="request.bbPeriod" :min="10" :max="50" :step="1" thumb-label />
                        </div>
                        <div class="mb-3">
                          <span class="text-caption text-grey">볼린저 밴드 승수: {{ request.bbMultiplier }}</span>
                          <v-slider v-model="request.bbMultiplier" :min="1" :max="4" :step="1" thumb-label />
                        </div>
                        <div>
                          <span class="text-caption text-grey">거래량 급증 기준: {{ request.volumeThreshold }}%</span>
                          <v-slider v-model="request.volumeThreshold" :min="100" :max="500" :step="10" thumb-label />
                        </div>
                      </template>
                    </v-expansion-panel>

                    <!-- 매수 방식 -->
                    <v-expansion-panel title="매수 방식">
                      <template v-slot:text>
                        <v-switch
                          v-model="request.useRoundRobin"
                          :label="request.useRoundRobin ? '라운드로빈 (신호 발생 종목 균등 분배)' : '고정 금액 매수'"
                          color="primary" density="compact" hide-details class="mb-2"
                        />
                        <div v-if="!request.useRoundRobin" class="mb-2">
                          <span class="text-caption text-grey">1회 매수 금액: {{ formatCurrency(request.fixedBuyAmount) }}</span>
                          <v-slider v-model="request.fixedBuyAmount" :min="10000" :max="10000000" :step="10000" thumb-label />
                        </div>
                        <v-switch v-model="request.useDailyLimitRecovery" label="일일 한도 복구 사용" color="primary" density="compact" hide-details />
                      </template>
                    </v-expansion-panel>

                    <!-- 리스크 관리 -->
                    <v-expansion-panel title="리스크 관리">
                      <template v-slot:text>
                        <div class="mb-3">
                          <span class="text-caption text-grey">일일 거래 한도: {{ request.dailyTradeLimitPct }}%
                            ({{ formatCurrency(request.initialBalance * request.dailyTradeLimitPct / 100) }})</span>
                          <v-slider v-model="request.dailyTradeLimitPct" :min="10" :max="100" :step="5" thumb-label />
                        </div>
                        <div class="mb-3">
                          <span class="text-caption text-grey">단일 종목 비중 제한: {{ request.maxPositionPct }}%
                            ({{ formatCurrency(request.initialBalance * request.maxPositionPct / 100) }})</span>
                          <v-slider v-model="request.maxPositionPct" :min="10" :max="100" :step="5" thumb-label />
                        </div>
                        <div class="mb-3">
                          <span class="text-caption text-grey">긴급 정지 기준(일일 손실): {{ request.dailyStopLossPct }}%</span>
                          <v-slider v-model="request.dailyStopLossPct" :min="-50" :max="0" :step="1" thumb-label />
                        </div>
                        <v-divider class="my-3" />
                        <div class="text-subtitle-2 mb-2 d-flex align-center">
                          <v-icon size="small" class="mr-1">mdi-shield-alert</v-icon>
                          급락장 보호 기능
                        </div>
                        <v-switch
                          v-model="request.useMarketTrendFilter"
                          label="시장 추세 필터 (Day 62 기준 미지원 - KOSPI 연동 예정)"
                          color="primary" density="compact" hide-details disabled class="mb-2"
                        />
                        <div class="mb-3">
                          <span class="text-caption text-grey">누적 손실 한도: {{ request.cumulativeLossLimitPct }}%</span>
                          <v-slider v-model="request.cumulativeLossLimitPct" :min="-50" :max="0" :step="1" thumb-label />
                        </div>
                        <div>
                          <span class="text-caption text-grey">연속 손절 제한: {{ request.consecutiveStopLossLimit }}회</span>
                          <v-slider v-model="request.consecutiveStopLossLimit" :min="1" :max="10" :step="1" thumb-label />
                        </div>
                      </template>
                    </v-expansion-panel>
                  </v-expansion-panels>

                  <v-btn
                    type="submit"
                    color="primary"
                    block
                    size="large"
                    class="mt-4"
                    :loading="loading"
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
            <v-card v-if="loading" class="text-center pa-8">
              <v-progress-circular indeterminate color="primary" size="64" />
              <p class="mt-4 text-h6">백테스트 실행 중...</p>
              <p class="text-grey">KIS API로부터 과거 일봉 데이터를 조회하고 분석하고 있습니다.</p>
            </v-card>

            <v-card v-else-if="!result" class="text-center pa-8">
              <v-icon size="64" color="grey-lighten-1">mdi-chart-line</v-icon>
              <p class="mt-4 text-h6">백테스트 설정을 입력하고 실행하세요</p>
              <p class="text-grey">과거 데이터를 기반으로 주식/ETF 거래 전략을 시뮬레이션합니다.</p>
            </v-card>

            <template v-else>
              <!-- 요약 카드 -->
              <v-row class="mb-2">
                <v-col cols="6" md="3">
                  <v-card :color="result.totalProfit >= 0 ? 'success' : 'error'" variant="tonal">
                    <v-card-text class="text-center">
                      <div class="text-overline">총 수익</div>
                      <div class="text-h5 font-weight-bold">
                        {{ result.totalProfit >= 0 ? '+' : '' }}{{ formatCurrency(result.totalProfit) }}
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
                      <div class="text-caption text-grey">{{ result.winCount }}승 {{ result.loseCount }}패</div>
                    </v-card-text>
                  </v-card>
                </v-col>
                <v-col cols="6" md="3">
                  <v-card variant="tonal">
                    <v-card-text class="text-center">
                      <div class="text-overline">최대 낙폭(MDD)</div>
                      <div class="text-h5 font-weight-bold text-error">{{ result.maxDrawdown.toFixed(2) }}%</div>
                    </v-card-text>
                  </v-card>
                </v-col>
                <v-col cols="6" md="3">
                  <v-card variant="tonal">
                    <v-card-text class="text-center">
                      <div class="text-overline">샤프 비율</div>
                      <div class="text-h5 font-weight-bold">{{ result.sharpeRatio.toFixed(2) }}</div>
                    </v-card-text>
                  </v-card>
                </v-col>
              </v-row>

              <!-- 상세 통계 -->
              <v-card class="mb-4">
                <v-card-text>
                  <v-row>
                    <v-col cols="6" md="3">
                      <div class="text-caption text-grey">초기 자본</div>
                      <div class="text-body-1">{{ formatCurrency(result.initialBalance) }}</div>
                    </v-col>
                    <v-col cols="6" md="3">
                      <div class="text-caption text-grey">최종 잔고</div>
                      <div class="text-body-1">{{ formatCurrency(result.finalBalance) }}</div>
                    </v-col>
                    <v-col cols="6" md="3">
                      <div class="text-caption text-grey">평균 수익</div>
                      <div class="text-body-1 text-success">{{ formatCurrency(result.avgProfit) }}</div>
                    </v-col>
                    <v-col cols="6" md="3">
                      <div class="text-caption text-grey">평균 손실</div>
                      <div class="text-body-1 text-error">-{{ formatCurrency(result.avgLoss) }}</div>
                    </v-col>
                  </v-row>
                  <v-row class="mt-1">
                    <v-col cols="6" md="3">
                      <div class="text-caption text-grey">총 거래</div>
                      <div class="text-body-1">{{ result.totalTrades }}건 (매수 {{ result.buyCount }} / 매도 {{ result.sellCount }})</div>
                    </v-col>
                    <v-col cols="6" md="3">
                      <div class="text-caption text-grey">손익비 (Profit Factor)</div>
                      <div class="text-body-1">{{ result.profitFactor.toFixed(2) }}</div>
                    </v-col>
                    <v-col cols="6" md="3">
                      <div class="text-caption text-grey">테스트 기간</div>
                      <div class="text-body-1">{{ result.totalDays }}일</div>
                    </v-col>
                  </v-row>
                </v-card-text>
              </v-card>
              <!-- 자산 변동 차트 -->
              <v-card class="mb-4">
                <v-card-title class="d-flex align-center">
                  <v-icon class="mr-2">mdi-chart-areaspline</v-icon>
                  자산 변동 추이
                </v-card-title>
                <v-card-text>
                  <div class="chart-wrapper" @mousemove="handleChartHover" @mouseleave="hoveredIndex = -1">
                    <svg :viewBox="`0 0 ${svgWidth} ${svgHeight}`" class="custom-chart" preserveAspectRatio="none">
                      <path :d="areaPath" fill="rgba(25,118,210,0.12)" stroke="none" />
                      <path :d="linePath" fill="none" stroke="#1976D2" stroke-width="2" />
                      <line
                        v-if="hoveredIndex >= 0"
                        :x1="chartPoints[hoveredIndex]?.x" :x2="chartPoints[hoveredIndex]?.x"
                        y1="0" :y2="svgHeight" stroke="#9e9e9e" stroke-dasharray="4" stroke-width="1"
                      />
                      <circle
                        v-for="(p, i) in chartPoints" :key="i"
                        :cx="p.x" :cy="p.y" :r="hoveredIndex === i ? 5 : 2.5"
                        :fill="getPointColor(i)" class="chart-point"
                      />
                    </svg>
                    <div class="chart-labels">
                      <div class="chart-label label-max" :style="{ top: getLabelPosition(maxBalance) + '%' }">
                        최고: {{ formatCurrency(maxBalance) }}
                      </div>
                      <div class="chart-label label-initial" :style="{ top: getLabelPosition(result.initialBalance) + '%' }">
                        초기: {{ formatCurrency(result.initialBalance) }}
                      </div>
                      <div class="chart-label label-min" :style="{ top: getLabelPosition(minBalance) + '%' }">
                        최저: {{ formatCurrency(minBalance) }}
                      </div>
                    </div>
                    <div v-if="hoveredData" class="chart-tooltip" :style="{ left: tooltipX + 'px' }">
                      <div>{{ hoveredData.date }}</div>
                      <div>자산: {{ formatCurrency(hoveredData.balance) }}</div>
                      <div>수익률: {{ hoveredData.profitRate >= 0 ? '+' : '' }}{{ hoveredData.profitRate.toFixed(2) }}%</div>
                    </div>
                  </div>
                </v-card-text>
              </v-card>

              <!-- 종목별 성과 -->
              <v-card class="mb-4">
                <v-card-title class="d-flex align-center">
                  <v-icon class="mr-2">mdi-format-list-bulleted</v-icon>
                  종목별 성과
                </v-card-title>
                <v-data-table
                  :headers="stockHeaders"
                  :items="result.stockPerformances"
                  :items-per-page="10"
                  density="comfortable"
                >
                  <template v-slot:item.stockCode="{ item }">
                    {{ stockNameMap[item.stockCode] || item.stockCode }}
                  </template>
                  <template v-slot:item.winRate="{ item }">
                    {{ item.winCount }}승 {{ item.loseCount }}패
                  </template>
                  <template v-slot:item.totalProfit="{ item }">
                    <span :class="item.totalProfit >= 0 ? 'text-success' : 'text-error'">
                      {{ item.totalProfit >= 0 ? '+' : '' }}{{ formatCurrency(item.totalProfit) }}
                    </span>
                  </template>
                  <template v-slot:item.profitRate="{ item }">
                    <span :class="item.profitRate >= 0 ? 'text-success' : 'text-error'">
                      {{ item.profitRate >= 0 ? '+' : '' }}{{ item.profitRate.toFixed(2) }}%
                    </span>
                  </template>
                  <template v-slot:item.avgHoldingDays="{ item }">
                    {{ item.avgHoldingDays != null ? item.avgHoldingDays.toFixed(1) + '일' : '-' }}
                  </template>
                </v-data-table>
              </v-card>

              <!-- 거래 내역 -->
              <v-card>
                <v-card-title class="d-flex align-center">
                  <v-icon class="mr-2">mdi-history</v-icon>
                  거래 내역 ({{ result.trades.length }}건)
                </v-card-title>
                <v-data-table
                  :headers="tradeHeaders"
                  :items="result.trades"
                  :items-per-page="15"
                  density="comfortable"
                >
                  <template v-slot:item.type="{ item }">
                    <v-chip :color="item.type === 'BUY' ? 'primary' : 'secondary'" size="small">
                      {{ item.type === 'BUY' ? '매수' : '매도' }}
                    </v-chip>
                  </template>
                  <template v-slot:item.stockCode="{ item }">
                    {{ stockNameMap[item.stockCode] || item.stockCode }}
                  </template>
                  <template v-slot:item.price="{ item }">
                    {{ formatCurrency(item.price) }}
                  </template>
                  <template v-slot:item.amount="{ item }">
                    {{ formatCurrency(item.amount) }}
                  </template>
                  <template v-slot:item.profit="{ item }">
                    <span v-if="item.profit != null" :class="item.profit >= 0 ? 'text-success' : 'text-error'">
                      {{ item.profit >= 0 ? '+' : '' }}{{ formatCurrency(item.profit) }}
                    </span>
                    <span v-else>-</span>
                  </template>
                  <template v-slot:item.profitRate="{ item }">
                    <span v-if="item.profitRate != null" :class="item.profitRate >= 0 ? 'text-success' : 'text-error'">
                      {{ item.profitRate >= 0 ? '+' : '' }}{{ item.profitRate.toFixed(2) }}%
                    </span>
                    <span v-else>-</span>
                  </template>
                </v-data-table>
              </v-card>
            </template>
          </v-col>
        </v-row>
      </v-container>
    </v-main>

    <v-snackbar v-model="snackbar.show" :color="snackbar.color" timeout="3000">
      {{ snackbar.message }}
    </v-snackbar>
  </v-app>
</template>

<script setup lang="ts">
// ===================================================
// Phase 2 Day 62: 주식/ETF 백테스트 페이지
// Phase 1 BacktestView.vue 구조 재사용 (코인 → 종목, 정수 수량, decay 방지 패널 추가)
// ===================================================
import { ref, computed, onMounted } from 'vue'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'
import { stockBacktestApi } from '@/api/stock'
import type { StockBacktestResult, AvailableStock } from '@/types/stockBacktest'

const sidebarRef = ref<any>({ drawer: true })
const form = ref()

const loading = ref(false)
const availableStocks = ref<AvailableStock[]>([])
const result = ref<StockBacktestResult | null>(null)

const stockNameMap = computed<Record<string, string>>(() => {
  const map: Record<string, string> = {}
  availableStocks.value.forEach(s => { map[s.stockCode] = s.stockName })
  return map
})

const request = ref({
  stockCodes: ['409820', '409810'],
  startDate: getDateString(-365),
  endDate: getDateString(-1),
  initialBalance: 1000000,
  basePeriod: 20,
  buyThresholdPct: -3,
  sellTargetPct: 2.5,
  useStopLoss: true,
  stopLossPct: -5,
  maxHoldingsPerStock: 3,
  additionalDropPct: 1.0,
  useTrailingStop: true,
  trailingStopPct: 2.5,
  rsiPeriod: 14,
  rsiBuyThreshold: 35,
  rsiSellThreshold: 65,
  bbPeriod: 20,
  bbMultiplier: 2,
  volumeThreshold: 120,
  // ⭐ [Day 62 신규] 레버리지 ETF decay 방지
  maxHoldingDays: 20,
  dailyTradeLimitPct: 20,
  maxPositionPct: 25,
  dailyStopLossPct: -5,
  useMarketTrendFilter: false,
  cumulativeLossLimitPct: -10,
  consecutiveStopLossLimit: 3,
  fixedBuyAmount: 100000,
  useDailyLimitRecovery: false,
  useRoundRobin: true,
})

const snackbar = ref({ show: false, message: '', color: 'success' })

const stockHeaders = [
  { title: '종목', key: 'stockCode' },
  { title: '거래 횟수', key: 'tradeCount' },
  { title: '승/패', key: 'winRate' },
  { title: '총 손익', key: 'totalProfit' },
  { title: '수익률', key: 'profitRate' },
  { title: '평균 보유일', key: 'avgHoldingDays' },
]

const tradeHeaders = [
  { title: '유형', key: 'type', width: '80px' },
  { title: '종목', key: 'stockCode' },
  { title: '일자', key: 'tradeDate', width: '110px' },
  { title: '가격', key: 'price' },
  { title: '수량', key: 'quantity' },
  { title: '금액', key: 'amount' },
  { title: '손익', key: 'profit' },
  { title: '수익률', key: 'profitRate', width: '90px' },
  { title: '신호', key: 'signal' },
]

// ---- 차트 ----
const hoveredIndex = ref(-1)
const tooltipX = ref(0)
const svgWidth = 800
const svgHeight = 350
const svgPadding = 40

const hoveredData = computed(() => {
  if (hoveredIndex.value < 0 || !result.value?.dailyBalances) return null
  return result.value.dailyBalances[hoveredIndex.value]
})

const maxBalance = computed(() => {
  if (!result.value?.dailyBalances?.length) return 0
  return Math.max(...result.value.dailyBalances.map(d => d.balance))
})

const minBalance = computed(() => {
  if (!result.value?.dailyBalances?.length) return 0
  return Math.min(...result.value.dailyBalances.map(d => d.balance))
})

const getYPosition = (balance: number) => {
  if (!result.value?.dailyBalances?.length) return svgHeight / 2
  const max = maxBalance.value
  const min = minBalance.value
  const range = max - min || 1
  return svgPadding + ((max - balance) / range) * (svgHeight - svgPadding * 2)
}

const getXPosition = (index: number, total: number) => {
  if (total <= 1) return svgWidth / 2
  return svgPadding + (index / (total - 1)) * (svgWidth - svgPadding * 2)
}

const chartPoints = computed(() => {
  if (!result.value?.dailyBalances?.length) return []
  const total = result.value.dailyBalances.length
  return result.value.dailyBalances.map((d, index) => ({
    x: getXPosition(index, total),
    y: getYPosition(d.balance),
    balance: d.balance
  }))
})

const linePath = computed(() => {
  if (!chartPoints.value.length) return ''
  return chartPoints.value.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ')
})

const areaPath = computed(() => {
  if (!chartPoints.value.length) return ''
  const points = chartPoints.value
  const firstX = points[0].x
  const lastX = points[points.length - 1].x
  const bottomY = svgHeight - svgPadding
  return `M ${firstX} ${bottomY} L ${points.map(p => `${p.x} ${p.y}`).join(' L ')} L ${lastX} ${bottomY} Z`
})

const getLabelPosition = (balance: number) => {
  if (!result.value?.dailyBalances?.length) return 50
  const max = maxBalance.value
  const min = minBalance.value
  const range = max - min || 1
  const paddingPercent = (svgPadding / svgHeight) * 100
  const usableHeight = 100 - paddingPercent * 2
  return paddingPercent + ((max - balance) / range) * usableHeight
}

const getPointColor = (index: number) => {
  if (!result.value?.dailyBalances?.length) return '#1976D2'
  const balance = result.value.dailyBalances[index].balance
  const initial = result.value.initialBalance
  if (balance > initial * 1.01) return '#4CAF50'
  if (balance < initial * 0.99) return '#F44336'
  return '#1976D2'
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

// ---- ETF 유형 표시 ----
function etfTypeLabel(etfType: string): string {
  switch (etfType) {
    case 'LEVERAGE': return '레버리지'
    case 'INVERSE': return '인버스'
    case 'NORMAL': return 'ETF'
    default: return '주식'
  }
}
function etfTypeColor(etfType: string): string {
  switch (etfType) {
    case 'LEVERAGE': return 'error'
    case 'INVERSE': return 'purple'
    case 'NORMAL': return 'primary'
    default: return 'grey'
  }
}

// ---- 초기화 ----
onMounted(async () => {
  await fetchAvailableStocks()
})

const fetchAvailableStocks = async () => {
  try {
    const response = await stockBacktestApi.getAvailableStocks()
    availableStocks.value = (response.data as any).stocks || []
  } catch (error) {
    console.error('종목 목록 조회 실패:', error)
    availableStocks.value = [
      { stockCode: '409820', stockName: 'TIGER 미국나스닥100레버리지(합성)', market: 'KRX', etfType: 'LEVERAGE', isActive: true },
      { stockCode: '409810', stockName: 'KODEX 미국나스닥100레버리지(합성H)', market: 'KRX', etfType: 'LEVERAGE', isActive: true },
    ]
  }
}

// ---- 백테스트 실행 ----
const runBacktest = async () => {
  if (!request.value.stockCodes || request.value.stockCodes.length === 0) {
    showSnackbar('종목을 선택해주세요.', 'warning')
    return
  }

  const startDate = new Date(request.value.startDate)
  const endDate = new Date(request.value.endDate)
  const diffYears = (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24 * 365)

  if (diffYears > 1) {
    alert('주식 백테스트 기간은 최대 1년까지 가능합니다.\n\n선택한 기간: ' + Math.floor(diffYears * 10) / 10 + '년')
    return
  }

  loading.value = true
  result.value = null

  try {
    const response = await stockBacktestApi.run(request.value as any)
    result.value = response.data
    showSnackbar('백테스트가 완료되었습니다.', 'success')
  } catch (error: any) {
    console.error('백테스트 실패:', error)
    showSnackbar(error.response?.data?.message || '백테스트 실행에 실패했습니다.', 'error')
  } finally {
    loading.value = false
  }
}

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
</script>

<style scoped>
.text-success { color: #4CAF50 !important; font-weight: bold; }
.text-error { color: #F44336 !important; font-weight: bold; }

.chart-wrapper { position: relative; cursor: crosshair; height: 350px; }
.custom-chart { width: 100%; height: 100%; }
.chart-point { transition: r 0.2s ease; cursor: pointer; }

.chart-labels { position: absolute; top: 0; right: 0; height: 100%; pointer-events: none; }
.chart-label {
  position: absolute; right: 5px; font-size: 11px; padding: 2px 6px;
  background: white; border-radius: 3px; font-weight: 500;
  transform: translateY(-50%); white-space: nowrap;
}
.label-max { color: #4CAF50; }
.label-initial { color: #FF9800; }
.label-min { color: #F44336; }

.chart-tooltip {
  position: absolute; top: 10px; transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.85); color: white; padding: 8px 12px;
  border-radius: 6px; font-size: 12px; pointer-events: none; z-index: 10;
  white-space: nowrap; box-shadow: 0 2px 8px rgba(0,0,0,0.3);
}
</style>