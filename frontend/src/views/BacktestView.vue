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
                  <v-autocomplete
                    v-model="request.coinSymbols"
                    :items="availableCoins"
                    item-value="symbol"
                    label="거래 코인"
                    multiple
                    chips
                    closable-chips
                    :rules="[v => v.length > 0 || '최소 1개 코인을 선택하세요']"
                    hint="시가총액 순으로 정렬됩니다"
                    persistent-hint
                  >
                    <template v-slot:item="{ props, item }">
                      <v-list-item v-bind="props">
                        <template v-slot:prepend>
                          <v-chip size="x-small" color="primary" variant="tonal" class="mr-2">
                            {{ item.raw.rank || '-' }}
                          </v-chip>
                        </template>
                        <template v-slot:title>
                          {{ item.raw.name }}
                        </template>
                        <template v-slot:subtitle>
                          {{ item.raw.symbol }}
                        </template>
                      </v-list-item>
                    </template>
                    <template v-slot:chip="{ props, item }">
                      <v-chip v-bind="props" closable>
                        {{ item.raw.name }}
                      </v-chip>
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

                        <v-divider class="my-4" />

                        <div class="text-subtitle-2 mb-3">
                          <v-icon size="small" class="mr-1">mdi-chart-bell-curve-cumulative</v-icon>
                          기술적 지표 설정
                        </div>

                        <!-- RSI 설정 -->
                        <div class="text-caption text-grey mb-2">RSI (상대강도지수)</div>
                        <v-row dense>
                          <v-col cols="4">
                            <v-text-field
                              v-model.number="request.rsiPeriod"
                              label="기간"
                              type="number"
                              density="compact"
                              suffix="일"
                              hide-details
                            />
                          </v-col>
                          <v-col cols="4">
                            <v-text-field
                              v-model.number="request.rsiBuyThreshold"
                              label="매수 ≤"
                              type="number"
                              density="compact"
                              hide-details
                            />
                          </v-col>
                          <v-col cols="4">
                            <v-text-field
                              v-model.number="request.rsiSellThreshold"
                              label="매도 ≥"
                              type="number"
                              density="compact"
                              hide-details
                            />
                          </v-col>
                        </v-row>

                        <!-- 볼린저 밴드 설정 -->
                        <div class="text-caption text-grey mb-2 mt-4">볼린저 밴드</div>
                        <v-row dense>
                          <v-col cols="6">
                            <v-text-field
                              v-model.number="request.bbPeriod"
                              label="기간"
                              type="number"
                              density="compact"
                              suffix="일"
                              hide-details
                            />
                          </v-col>
                          <v-col cols="6">
                            <v-select
                              v-model="request.bbMultiplier"
                              :items="[1, 2, 3, 4]"
                              label="표준편차"
                              density="compact"
                              suffix="배"
                              hide-details
                            />
                          </v-col>
                        </v-row>
                        
                        <!-- 거래량 설정 -->
                        <div class="text-caption text-grey mb-2 mt-4">거래량 급증 기준</div>
                        <v-slider
                          v-model="request.volumeThreshold"
                          :min="100"
                          :max="500"
                          :step="10"
                          thumb-label
                          hide-details
                        >
                          <template v-slot:append>
                            <span class="text-body-2">{{ request.volumeThreshold }}%</span>
                          </template>
                        </v-slider>
                        <v-divider class="my-4" />
                        
                        <!-- ★★★ 신규 추가: 리스크 관리 설정 ★★★ -->
                        <div class="text-subtitle-2 mb-3">
                          <v-icon size="small" class="mr-1">mdi-shield-check</v-icon>
                          리스크 관리
                        </div>
                        
                        <!-- 일일 거래 한도 -->
                        <div class="text-caption text-grey mb-2">일일 최대 거래금액</div>
                        <v-slider
                          v-model="request.dailyTradeLimitPct"
                          :min="10"
                          :max="100"
                          :step="10"
                          thumb-label
                          hide-details
                        >
                          <template v-slot:append>
                            <span class="text-body-2" style="min-width: 80px">
                              {{ request.dailyTradeLimitPct === 100 ? '제한없음' : `${request.dailyTradeLimitPct}%` }}
                            </span>
                          </template>
                        </v-slider>
                        <div class="text-caption text-grey-darken-1 mb-3">
                          초기 자본 대비 하루 최대 매수 금액 ({{ formatCurrency(request.initialBalance * request.dailyTradeLimitPct / 100) }})
                        </div>
                        
                        <!-- 단일 종목 비중 제한 -->
                        <div class="text-caption text-grey mb-2 mt-3">단일 종목 최대 비중</div>
                        <v-slider
                          v-model="request.maxPositionPct"
                          :min="10"
                          :max="100"
                          :step="5"
                          thumb-label
                          hide-details
                        >
                          <template v-slot:append>
                            <span class="text-body-2" style="min-width: 80px">
                              {{ request.maxPositionPct === 100 ? '제한없음' : `${request.maxPositionPct}%` }}
                            </span>
                          </template>
                        </v-slider>
                        <div class="text-caption text-grey-darken-1 mb-3">
                          한 코인에 최대 투자 가능 금액 ({{ formatCurrency(request.initialBalance * request.maxPositionPct / 100) }})
                        </div>
                        
                        <!-- 긴급 정지 조건 -->
                        <div class="text-caption text-grey mb-2 mt-3">긴급 정지 (일일 손실률)</div>
                        <v-slider
                          v-model="request.dailyStopLossPct"
                          :min="-50"
                          :max="0"
                          :step="5"
                          thumb-label
                          hide-details
                          color="error"
                        >
                          <template v-slot:append>
                            <span class="text-body-2" style="min-width: 80px">
                              {{ request.dailyStopLossPct <= -50 ? '사용안함' : `${request.dailyStopLossPct}%` }}
                            </span>
                          </template>
                        </v-slider>
                        <div class="text-caption text-grey-darken-1 mb-1">
                          당일 손실이 {{ request.dailyStopLossPct }}% 도달 시 거래 중단
                        </div>
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
  <v-card-title class="d-flex align-center">
    <v-icon class="mr-2">mdi-chart-line</v-icon>
    자산 변동 추이
    
    <!-- ★★★ 추가: 보기 모드 토글 (60일 이상일 때만 표시) ★★★ -->
    <v-spacer />
    <v-btn-toggle
      v-if="result.dailyBalances.length > 60"
      v-model="chartViewMode"
      density="compact"
      mandatory
      color="primary"
      class="ml-4"
    >
      <v-btn value="full" size="small">
        <v-icon size="small" class="mr-1">mdi-fit-to-screen</v-icon>
        전체 보기
      </v-btn>
      <v-btn value="scroll" size="small">
        <v-icon size="small" class="mr-1">mdi-arrow-left-right</v-icon>
        스크롤 보기
      </v-btn>
    </v-btn-toggle>
  </v-card-title>
  <v-card-text>
    <!-- ★★★ 수정: 스크롤 모드 지원 래퍼 ★★★ -->
    <div 
      class="chart-scroll-container"
      :class="{ 'scroll-mode': isScrollMode }"
    >
      <div 
        class="chart-wrapper"
        :style="{ width: chartWrapperWidth }"
        @mousemove="handleChartHover"
        @mouseleave="hoveredIndex = -1"
      >
        <svg 
          class="custom-chart"
          :viewBox="`0 0 ${dynamicSvgWidth} ${svgHeight}`"
          preserveAspectRatio="none"
        >
          <!-- 그라데이션 정의 -->
          <defs>
            <linearGradient id="areaGradient" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" style="stop-color:#42A5F5;stop-opacity:0.6" />
              <stop offset="100%" style="stop-color:#42A5F5;stop-opacity:0.1" />
            </linearGradient>
          </defs>
          
          <!-- 영역 채우기 -->
          <path :d="areaPath" fill="url(#areaGradient)" />
          
          <!-- 기준선들 -->
          <line 
            :x1="svgPadding" 
            :y1="getYPosition(result.initialBalance)" 
            :x2="dynamicSvgWidth - svgPadding" 
            :y2="getYPosition(result.initialBalance)"
            stroke="#FF9800" 
            stroke-width="2" 
            stroke-dasharray="6,4"
          />
          <line 
            :x1="svgPadding" 
            :y1="getYPosition(maxBalance)" 
            :x2="dynamicSvgWidth - svgPadding" 
            :y2="getYPosition(maxBalance)"
            stroke="#4CAF50" 
            stroke-width="2" 
            stroke-dasharray="6,4"
          />
          <line 
            :x1="svgPadding" 
            :y1="getYPosition(minBalance)" 
            :x2="dynamicSvgWidth - svgPadding" 
            :y2="getYPosition(minBalance)"
            stroke="#F44336" 
            stroke-width="2" 
            stroke-dasharray="6,4"
          />
          
          <!-- 라인 차트 -->
          <path :d="linePath" fill="none" stroke="#1976D2" stroke-width="2.5" />
          
          <!-- 데이터 포인트 -->
          <circle
            v-for="(point, index) in chartPoints"
            :key="index"
            :cx="point.x"
            :cy="point.y"
            :r="hoveredIndex === index ? 8 : pointRadius"
            :fill="getPointColor(index)"
            stroke="white"
            stroke-width="2"
            class="chart-point"
          />
        </svg>
        
        <!-- 기준선 라벨 (스크롤 모드에서는 고정) -->
        <div class="chart-labels" :class="{ 'labels-fixed': isScrollMode }">
          <span class="chart-label label-max" :style="{ top: getLabelPosition(maxBalance) + '%' }">
            최고: {{ formatCurrency(maxBalance) }}
          </span>
          <span class="chart-label label-initial" :style="{ top: getLabelPosition(result.initialBalance) + '%' }">
            초기: {{ formatCurrency(result.initialBalance) }}
          </span>
          <span class="chart-label label-min" :style="{ top: getLabelPosition(minBalance) + '%' }">
            최저: {{ formatCurrency(minBalance) }}
          </span>
        </div>
        
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
    </div>
    
    <!-- 스크롤 힌트 -->
    <div v-if="isScrollMode" class="text-center text-caption text-grey mt-1">
      <v-icon size="small">mdi-gesture-swipe-horizontal</v-icon>
      좌우로 스크롤하여 전체 기간을 확인하세요
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

const chartViewMode = ref<'full' | 'scroll'>('full')

const isScrollMode = computed(() => {
  if (!result.value?.dailyBalances?.length) return false
  return chartViewMode.value === 'scroll' && result.value.dailyBalances.length > 60
})

// 스크롤 모드에서 점 간격 (px)
const pointSpacing = 25

// 동적 SVG 너비
const dynamicSvgWidth = computed(() => {
  if (!result.value?.dailyBalances?.length) return svgWidth
  if (isScrollMode.value) {
    // 스크롤 모드: 데이터 개수 × 점 간격
    return Math.max(svgWidth, result.value.dailyBalances.length * pointSpacing + svgPadding * 2)
  }
  return svgWidth
})

// 차트 래퍼 너비
const chartWrapperWidth = computed(() => {
  if (isScrollMode.value) {
    return dynamicSvgWidth.value + 'px'
  }
  return '100%'
})

// 점 크기 (스크롤 모드에서 더 크게)
const pointRadius = computed(() => {
  return isScrollMode.value ? 6 : 4
})

// 상태
const loading = ref(false)
const availableCoins = ref<AvailableCoin[]>([])
const result = ref<BacktestResult | null>(null)


// 요청 데이터
const request = ref({
  coinSymbols: ['KRW-BTC', 'KRW-ETH', 'KRW-XRP', 'KRW-SOL'],
  startDate: getDateString(-365),
  endDate: getDateString(-1),
  initialBalance: 1000000,
  basePeriod: 20,
  buyThresholdPct: -6,
  sellTargetPct: 4,
  stopLossPct: -8,
  maxHoldingsPerCoin: 2,
  useTrailingStop: true,
  trailingStopPct: 4,
  // ★★★ 신규 추가: RSI 설정 ★★★
  rsiPeriod: 14,
  rsiBuyThreshold: 32,
  rsiSellThreshold: 68,
  // ★★★ 신규 추가: 볼린저 밴드 설정 ★★★
  bbPeriod: 20,
  bbMultiplier: 2,
  // ★★★ 신규 추가: 거래량 설정 ★★★
  volumeThreshold: 140,
  // ★★★ 신규 추가: 리스크 관리 설정 ★★★
  dailyTradeLimitPct: 20,    // 기본값: 제한 없음
  maxPositionPct: 25,        // 기본값: 제한 없음
  dailyStopLossPct: -5      // 기본값: 사용 안함
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



const hoveredData = computed(() => {
  if (hoveredIndex.value < 0 || !result.value?.dailyBalances) return null
  return result.value.dailyBalances[hoveredIndex.value]
})

const svgWidth = 800
const svgHeight = 350
const svgPadding = 40

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
  // Y는 위가 0, 아래가 height
  return svgPadding + ((max - balance) / range) * (svgHeight - svgPadding * 2)
}

const getXPosition = (index: number, total: number) => {
  if (total <= 1) return dynamicSvgWidth.value / 2
  return svgPadding + (index / (total - 1)) * (dynamicSvgWidth.value - svgPadding * 2)
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
  return chartPoints.value
    .map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`)
    .join(' ')
})

const areaPath = computed(() => {
  if (!chartPoints.value.length) return ''
  const points = chartPoints.value
  const firstX = points[0].x
  const lastX = points[points.length - 1].x
  const bottomY = svgHeight - svgPadding
  
  return `
    M ${firstX} ${bottomY}
    L ${points.map(p => `${p.x} ${p.y}`).join(' L ')}
    L ${lastX} ${bottomY}
    Z
  `
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

  // 코인 선택 체크
  if (!request.value.coinSymbols || request.value.coinSymbols.length === 0) {
    showSnackbar('코인을 선택해주세요.', 'warning')
    return
  }
  
  // ★★★ 추가: 기간 3년 초과 체크 ★★★
  const startDate = new Date(request.value.startDate)
  const endDate = new Date(request.value.endDate)
  const diffYears = (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24 * 365)
  
  if (diffYears > 3) {
    alert('백테스트 기간은 최대 3년까지 가능합니다.\n\n선택한 기간: ' + Math.floor(diffYears * 10) / 10 + '년')
    return
  }
  
  // ★★★ 추가: 장기간 테스트 시 안내 ★★★
  if (diffYears > 1) {
    const confirmed = confirm(
      `장기간(${Math.floor(diffYears * 10) / 10}년) 백테스트를 실행합니다.\n` +
      `데이터 조회에 시간이 다소 걸릴 수 있습니다.\n\n계속하시겠습니까?`
    )
    if (!confirmed) {
      return
    }
  }

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
.text-success {
  color: #4CAF50 !important;
  font-weight: bold;
}

.text-error {
  color: #F44336 !important;
  font-weight: bold;
}

.chart-wrapper {
  position: relative;
  cursor: crosshair;
  height: 350px;
}

.custom-chart {
  width: 100%;
  height: 100%;
}

.chart-point {
  transition: r 0.2s ease;
  cursor: pointer;
}

.chart-labels {
  position: absolute;
  top: 0;
  right: 0;
  height: 100%;
  pointer-events: none;
}

.chart-label {
  position: absolute;
  right: 5px;
  font-size: 11px;
  padding: 2px 6px;
  background: white;
  border-radius: 3px;
  font-weight: 500;
  transform: translateY(-50%);
  white-space: nowrap;
}

.label-max {
  color: #4CAF50;
}

.label-initial {
  color: #FF9800;
}

.label-min {
  color: #F44336;
}

.chart-tooltip {
  position: absolute;
  top: 10px;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.85);
  color: white;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 12px;
  pointer-events: none;
  z-index: 10;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(0,0,0,0.3);
}

.chart-scroll-container {
  position: relative;
  width: 100%;
}

.chart-scroll-container.scroll-mode {
  overflow-x: auto;
  overflow-y: hidden;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding-bottom: 8px;
}

.chart-scroll-container.scroll-mode::-webkit-scrollbar {
  height: 8px;
}

.chart-scroll-container.scroll-mode::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.chart-scroll-container.scroll-mode::-webkit-scrollbar-thumb {
  background: #1976D2;
  border-radius: 4px;
}

.chart-scroll-container.scroll-mode::-webkit-scrollbar-thumb:hover {
  background: #1565C0;
}

/* 스크롤 모드에서 라벨 고정 */
.labels-fixed {
  position: fixed !important;
  right: 20px !important;
}

/* 스크롤 모드에서 차트 래퍼 */
.scroll-mode .chart-wrapper {
  min-width: 100%;
}

</style>