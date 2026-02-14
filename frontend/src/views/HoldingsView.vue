<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main class="bg-grey-lighten-3">
      <v-container fluid>
        <v-row class="mb-4">
          <v-col>
            <h1 class="text-h4">
              <v-icon class="mr-2">mdi-wallet</v-icon>
              보유 자산
            </h1>
            <p class="text-subtitle-1 text-grey">기간별/코인별 수익 분석 및 보유 현황을 확인하세요</p>
          </v-col>
        </v-row>

        <!-- ⭐ Day 31 개선: 상단 영역 - 기간별/코인별 수익 탭 -->
        <v-row>
          <v-col cols="12">
            <!-- ⭐ 수정: 탭을 카드 바깥으로 분리하여 빈 공간 제거 -->
            <v-tabs 
              v-model="profitTab" 
              color="grey-darken-3" 
              class="profit-tabs mb-0"
              height="48"
            >
              <v-tab value="period" class="profit-tab text-body-1 font-weight-bold">
                <v-icon start size="20">mdi-chart-timeline-variant</v-icon>
                기간별 수익
              </v-tab>
              <v-tab value="coin" class="profit-tab text-body-1 font-weight-bold">
                <v-icon start size="20">mdi-bitcoin</v-icon>
                코인별 수익
              </v-tab>
            </v-tabs>

            <v-card elevation="2" class="card-no-top-radius">
              <v-window v-model="profitTab">
                <!-- ========== 기간별 수익 탭 ========== -->
                <v-window-item value="period">
                  <!-- ⭐ 수정: 탭이 카드 바깥이므로 mt 제거 -->
                  <v-card-title class="py-3 px-4 bg-indigo-darken-1 text-white d-flex align-center">
                    <v-icon class="mr-2" size="20">mdi-chart-timeline-variant</v-icon>
                    <span class="text-body-1 font-weight-bold">기간별 수익 분석</span>
                    <!-- 도움말 버튼  -->
                    <HelpButton 
                      use-dialog 
                      :dialog-title="helpContents.periodProfit.title"
                      :dialog-content="helpContents.periodProfit.content"
                    />
                    
                    <v-spacer />
                    
                    <!-- 기간 선택 버튼 + 일자 선택 (우측 정렬, 1줄) -->
                    <div class="d-flex align-center">
                      <v-btn-toggle 
                        v-model="selectedPeriod" 
                        mandatory 
                        density="compact"
                        variant="outlined"
                        divided
                        class="period-toggle-header mr-3"
                      >
                        <v-btn value="today">오늘</v-btn>
                        <v-btn value="month">이번달</v-btn>
                        <v-btn value="year">올해</v-btn>
                        <v-btn value="oneYear">1년</v-btn>
                        <v-btn value="total">누적</v-btn>
                      </v-btn-toggle>

                      <input
                        v-model="customStartDate"
                        type="date"
                        class="custom-date-input"
                      />
                      <span class="text-white mx-1">~</span>
                      <input
                        v-model="customEndDate"
                        type="date"
                        class="custom-date-input"
                      />
                      <v-btn 
                        color="white" 
                        variant="flat"
                        class="ml-2 text-indigo-darken-2 custom-search-btn"
                        @click="applyCustomPeriod"
                        :loading="loadingProfit"
                      >
                        조회
                      </v-btn>
                    </div>
                  </v-card-title>

                  <v-card-text class="pa-4">

                    <!-- 수익 요약 카드 (큰 카드 1개) -->
                    <v-card variant="outlined" class="mb-4 profit-summary-card">
                      <v-card-text class="pa-4">
                        <v-row align="center">
                          <v-col cols="12" md="4" class="text-center">
                            <div class="text-caption text-grey-darken-1 mb-1">
                              {{ getPeriodLabel(selectedPeriod) }} 수익
                            </div>
                            <div 
                              class="text-h4 font-weight-bold"
                              :class="currentPeriodProfit >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2'"
                            >
                              {{ currentPeriodProfit >= 0 ? '+' : '' }}{{ formatCurrency(currentPeriodProfit) }}
                            </div>
                            <v-chip 
                              :color="currentPeriodProfitPct >= 0 ? 'teal' : 'red'" 
                              size="small" 
                              variant="flat"
                              class="mt-2"
                            >
                              {{ currentPeriodProfitPct >= 0 ? '+' : '' }}{{ currentPeriodProfitPct.toFixed(2) }}%
                            </v-chip>
                          </v-col>

                          <v-divider vertical class="d-none d-md-block" />

                          <v-col cols="12" md="8">
                            <v-row dense>
                              <v-col cols="6" sm="3">
                                <div class="text-caption text-grey-darken-1">거래 건수</div>
                                <div class="text-h6 font-weight-medium">{{ periodDetail?.tradeCount || 0 }}건</div>
                              </v-col>
                              <v-col cols="6" sm="3">
                                <div class="text-caption text-grey-darken-1">익절 / 손절</div>
                                <div class="text-body-1">
                                  <span class="text-teal font-weight-medium">{{ periodDetail?.winCount || 0 }}</span>
                                  <span class="mx-1">/</span>
                                  <span class="text-red font-weight-medium">{{ periodDetail?.loseCount || 0 }}</span>
                                </div>
                              </v-col>
                              <v-col cols="6" sm="3">
                                <div class="text-caption text-grey-darken-1">승률</div>
                                <div class="text-h6 font-weight-medium">{{ periodDetail?.winRate?.toFixed(1) || 0 }}%</div>
                              </v-col>
                              <v-col cols="6" sm="3">
                                <div class="text-caption text-grey-darken-1">건당 평균</div>
                                <div class="text-body-1 font-weight-medium" :class="(periodDetail?.avgProfit || 0) >= 0 ? 'text-teal' : 'text-red'">
                                  {{ formatCurrency(periodDetail?.avgProfit || 0) }}
                                </div>
                              </v-col>
                              <v-col cols="6" sm="3" class="mt-2">
                                <div class="text-caption text-grey-darken-1">최대 수익</div>
                                <div class="text-body-1 text-teal font-weight-medium">{{ formatCurrency(periodDetail?.maxProfit || 0) }}</div>
                              </v-col>
                              <v-col cols="6" sm="3" class="mt-2">
                                <div class="text-caption text-grey-darken-1">최대 손실</div>
                                <div class="text-body-1 text-red font-weight-medium">{{ formatCurrency(periodDetail?.maxLoss || 0) }}</div>
                              </v-col>
                              <v-col cols="12" sm="6" class="mt-2">
                                <div class="text-caption text-grey-darken-1">조회 기간</div>
                                <div class="text-body-2">{{ periodDetail?.startDate || '-' }} ~ {{ periodDetail?.endDate || '-' }}</div>
                              </v-col>
                            </v-row>
                          </v-col>
                        </v-row>
                      </v-card-text>
                    </v-card>

                    <!-- 자산 변동 추이 차트 (백테스팅 스타일) -->
                    <v-card variant="outlined">
                      <v-card-title class="py-2 px-4 d-flex align-center">
                        <v-icon class="mr-2" size="20">mdi-chart-line</v-icon>
                        <span class="text-body-1">자산 변동 추이</span>
                        <v-spacer />
                        <!-- 전체보기/스크롤보기 토글 -->
                        <v-btn-toggle 
                          v-model="chartViewMode" 
                          mandatory 
                          density="compact"
                          variant="outlined"
                          divided
                        >
                          <v-btn value="full" size="x-small">
                            <v-icon size="16" class="mr-1">mdi-fit-to-screen</v-icon>
                            전체 보기
                          </v-btn>
                          <v-btn value="scroll" size="x-small">
                            <v-icon size="16" class="mr-1">mdi-arrow-left-right</v-icon>
                            스크롤 보기
                          </v-btn>
                        </v-btn-toggle>
                      </v-card-title>

                      <v-card-text class="pa-4">
                        <div v-if="assetHistory.length > 0" class="chart-container">
                          <div 
                            class="chart-wrapper-backtest"
                            :class="{ 'scroll-mode': chartViewMode === 'scroll' }"
                            :style="chartViewMode === 'scroll' ? { width: dynamicChartWidth + 'px' } : {}"
                            @mousemove="handleChartHover"
                            @mouseleave="hoveredIndex = -1"
                          >
                            <svg 
                              class="custom-chart"
                              :viewBox="`0 0 ${chartViewMode === 'scroll' ? dynamicChartWidth : svgWidth} ${svgHeight}`"
                              preserveAspectRatio="none"
                            >
                              <!-- 그라데이션 정의 -->
                              <defs>
                                <linearGradient id="profitAreaGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                                  <stop offset="0%" style="stop-color:#42A5F5;stop-opacity:0.6" />
                                  <stop offset="100%" style="stop-color:#42A5F5;stop-opacity:0.1" />
                                </linearGradient>
                              </defs>

                              <!-- 영역 채우기 (평가금액 - 연한 파란색) -->
                              <path :d="areaPath" fill="url(#profitAreaGradient)" />

		  <!-- ⭐⭐⭐ [변경] 자산 변동 추이 차트 재구성 ⭐⭐⭐ -->
                              <!-- 불입금액 막대그래프 (주황색) -->
                              <!-- ⭐⭐⭐ [변경] 막대: 불입금액 위치 위로 고정 높이 표시 ⭐⭐⭐ -->
                              <rect
                                v-for="(point, index) in chartPoints"
                                :key="'bar-' + index"
                                :x="point.x - barWidth / 2"
                                :y="getYPosition(point.depositAmount)"
                                :width="barWidth"
                                :height="Math.max(0, (svgHeight - svgPadding) - getYPosition(point.depositAmount))"
                                fill="#FF9800"
                                :opacity="hoveredIndex === index ? 0.6 : 0.35"
                                rx="1"
                              />

                              <!-- 최고 평가금액 파선 (초록) -->
                              <line
                                :x1="svgPadding" :y1="getYPosition(maxEvaluation)"
                                :x2="effectiveWidth - (chartViewMode === 'scroll' ? scrollPaddingRight : svgPaddingRight)" :y2="getYPosition(maxEvaluation)"
                                stroke="#4CAF50" stroke-width="2" stroke-dasharray="6,4"
                              />
                              <!-- 최저 평가금액 파선 (빨강) -->
                              <line
                                :x1="svgPadding" :y1="getYPosition(minEvaluation)"
                                :x2="effectiveWidth - (chartViewMode === 'scroll' ? scrollPaddingRight : svgPaddingRight)" :y2="getYPosition(minEvaluation)"
                                stroke="#F44336" stroke-width="2" stroke-dasharray="6,4"
                              />

                              <!-- 불입금액 추세선 (주황 파선) -->
                              <path :d="depositLinePath" fill="none" stroke="#FF9800" stroke-width="2" stroke-dasharray="6,4" />

                              <!-- 평가금액 추세선 (파란 파선) -->
                              <path :d="linePath" fill="none" stroke="#1976D2" stroke-width="2.5" stroke-dasharray="8,4" />

                              <!-- 평가금액 데이터 포인트 -->
                              <circle
                                v-for="(point, index) in chartPoints"
                                :key="'eval-' + index"
                                :cx="point.x" :cy="point.y"
                                :r="hoveredIndex === index ? 8 : 4"
                                :fill="getPointColor(point.evaluationAmount)"
                                stroke="white" stroke-width="2" class="chart-point"
                              />

                              <!-- 불입금액 데이터 포인트 (주황 작은 점) -->
                              <circle
                                v-for="(point, index) in chartPoints"
                                :key="'dep-' + index"
                                :cx="point.x" :cy="getYPosition(point.depositAmount)"
                                :r="hoveredIndex === index ? 6 : 3"
                                fill="#FF9800" stroke="white" stroke-width="1.5" class="chart-point"
                              />
                            </svg>

                            <!-- 기준선 라벨 -->
                            <div class="chart-labels-backtest">
                              <span class="chart-label label-max" :style="{ top: getAdjustedLabelPosition('max') + '%' }">
                                최고 평가 금액 : {{ formatCurrency(maxEvaluation) }}
                              </span>
                              <span class="chart-label label-evaluation" :style="{ top: getAdjustedLabelPosition('evaluation') + '%' }">
                                평가금액 추세 : {{ formatCurrency(latestEvaluationAmount) }}
                              </span>
                              <span class="chart-label label-deposit" :style="{ top: getAdjustedLabelPosition('deposit') + '%' }">
                                불입금액 추세 : {{ formatCurrency(latestDepositAmount) }}
                              </span>
                              <span class="chart-label label-min" :style="{ top: getAdjustedLabelPosition('min') + '%' }">
                                최저 평가 금액 : {{ formatCurrency(minEvaluation) }}
                              </span>
                              <span class="chart-label label-floor" :style="{ top: getAdjustedLabelPosition('floor') + '%' }">
                                차트 바닥 : {{ formatCurrency(minBalance) }}
                              </span>
                            </div>

                            <!-- 툴팁 -->
                            <!-- ⭐⭐⭐ [변경] 툴팁: 점 옆에 표시 (점을 가리지 않도록) ⭐⭐⭐ -->
                            <!-- 왜: 기존 translateX(-50%) + top 방식은 점 위를 가림 -->
                            <!--     대시보드와 동일하게 좌/우 자동 전환 방식으로 변경 -->
                            <div 
                              v-if="hoveredIndex >= 0 && hoveredData"
                              class="chart-tooltip-backtest"
                              :style="{ 
                                left: (tooltipX > chartWrapperWidth * 0.5 ? tooltipX - 10 : tooltipX + 10) + 'px',
                                top: Math.max(60, Math.min(svgHeight - 80, tooltipY)) + 'px',
                                transform: tooltipX > chartWrapperWidth * 0.5 ? 'translateX(-100%) translateY(-50%)' : 'translateY(-50%)'
                              }"
                            >
                             <!-- ⭐⭐⭐ [변경] 툴팁 5개 항목으로 확장 ⭐⭐⭐ -->
                              <div class="font-weight-bold mb-1">{{ hoveredData.date }}</div>
                              <div style="color: #64B5F6;">평가금액: {{ formatCurrency(hoveredData.evaluationAmount || hoveredData.balance) }}</div>
                              <div style="color: #FFB74D;">불입금액: {{ formatCurrency(hoveredData.depositAmount || initialAsset) }}</div>
                              <div :class="(hoveredData.profitRate || 0) >= 0 ? 'text-success' : 'text-error'">
                                수익률: {{ (hoveredData.profitRate || 0) >= 0 ? '+' : '' }}{{ Number(hoveredData.profitRate || 0).toFixed(2) }}%
                              </div>
                              <div :class="(hoveredData.profitAmount || 0) >= 0 ? 'text-success' : 'text-error'">
                                수익금액: {{ (hoveredData.profitAmount || 0) >= 0 ? '+' : '' }}{{ formatCurrency(hoveredData.profitAmount || 0) }}
                              </div>
                            </div>
                          </div>

                          <!-- 날짜 표시 -->
                          <div class="d-flex justify-space-between text-caption text-grey mt-2 px-4">
                            <span>{{ assetHistory[0]?.date || '' }}</span>
                            <span>{{ assetHistory[assetHistory.length - 1]?.date || '' }}</span>
                          </div>
                        </div>

                        <div v-else class="text-center py-8 text-grey-darken-2">
                          <v-icon size="48" class="mb-2">mdi-chart-line-variant</v-icon>
                          <div>거래 이력이 없습니다</div>
                        </div>
                      </v-card-text>
                    </v-card>
                  </v-card-text>
                </v-window-item>

                <!-- ========== 코인별 수익 탭 ========== -->
                <v-window-item value="coin">
                  <v-card-title class="py-3 px-4 bg-teal-darken-1 text-white d-flex align-center">
                    <v-icon class="mr-2" size="20">mdi-bitcoin</v-icon>
                    <span class="text-body-1 font-weight-bold">코인별 수익 분석</span>
                    <!-- 도움말 버튼 -->
                    <HelpButton 
                      use-dialog 
                      :dialog-title="helpContents.coinProfit.title"
                      :dialog-content="helpContents.coinProfit.content"
                    />
                    <v-spacer />
                    <v-btn
                      color="white"
                      variant="text"
                      @click="loadCoinProfits"
                      :loading="loadingCoin"
                      size="small"
                    >
                      <v-icon start>mdi-refresh</v-icon>
                      새로고침
                    </v-btn>
                  </v-card-title>

                  <v-card-text class="pa-0">
                    <v-data-table
                      :headers="coinHeaders"
                      :items="coinProfits"
                      :loading="loadingCoin"
                      items-per-page="10"
                      class="coin-profit-table"
                    >
                      <template v-slot:item.coinSymbol="{ item }">
                        <div class="d-flex align-center">
                          <strong>{{ item.coinSymbol.replace('KRW-', '') }}</strong>
                          <span class="text-caption text-grey ml-2">{{ item.coinName }}</span>
                        </div>
                      </template>

                      <template v-slot:item.totalProfit="{ item }">
                        <span 
                          class="font-weight-bold"
                          :class="item.totalProfit >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2'"
                        >
                          {{ formatCurrency(item.totalProfit) }}
                        </span>
                      </template>

                      <template v-slot:item.profitPct="{ item }">
                        <v-chip 
                          :color="item.profitPct >= 0 ? 'teal' : 'red'" 
                          size="small"
                          variant="flat"
                        >
                          {{ item.profitPct >= 0 ? '+' : '' }}{{ item.profitPct?.toFixed(2) }}%
                        </v-chip>
                      </template>

                      <template v-slot:item.winRate="{ item }">
                        <div>
                          <span class="text-teal">{{ item.winCount }}</span>
                          <span class="mx-1">/</span>
                          <span class="text-red">{{ item.loseCount }}</span>
                          <span class="text-caption text-grey ml-1">({{ item.winRate?.toFixed(0) }}%)</span>
                        </div>
                      </template>

                      <template v-slot:item.currentHoldingCount="{ item }">
                        <v-chip 
                          v-if="item.currentHoldingCount > 0"
                          color="indigo" 
                          size="small"
                          variant="outlined"
                        >
                          {{ item.currentHoldingCount }}건 보유중
                        </v-chip>
                        <span v-else class="text-grey">-</span>
                      </template>

                      <template v-slot:item.actions="{ item }">
                        <v-btn
                          color="grey"
                          size="small"
                          @click="openCoinDetailDialog(item)"
                        >
                          상세
                        </v-btn>
                      </template>
                    </v-data-table>
                  </v-card-text>
                </v-window-item>
              </v-window>
            </v-card>
          </v-col>
        </v-row>

         <!-- ========== 하단: 보유 현황 ========== -->
        <v-row class="mt-4">
          <v-col cols="12">
            <v-card elevation="2">
              <v-card-title class="py-3 px-4 bg-blue-grey-darken-1 text-white d-flex align-center">
                <v-icon class="mr-2" size="20">mdi-safe</v-icon>
                <span class="text-body-1 font-weight-bold">보유 현황</span>
                <!-- ★★★ [추가] 도움말 버튼 ★★★ -->
                <HelpButton 
                  use-dialog 
                  :dialog-title="helpContents.holdings.title"
                  :dialog-content="helpContents.holdings.content"
                />
                <v-spacer />
                <v-btn
                  color="white"
                  variant="text"
                  @click="loadHoldings"
                  :loading="loading"
                  size="small"
                >
                  <v-icon start>mdi-refresh</v-icon>
                  새로고침
                </v-btn>
              </v-card-title>

              <!-- 통계 카드 -->
              <v-card-text class="pa-4">
                <v-row class="mb-4">
                  <v-col cols="6" md="3">
                    <v-card variant="outlined" class="text-center pa-3">
                      <div class="text-caption text-grey-darken-1">총 투자금액</div>
                      <div class="text-h6 font-weight-bold mt-1">{{ formatCurrency(stats.totalHoldingAmount) }}</div>
                    </v-card>
                  </v-col>
                  <v-col cols="6" md="3">
                    <v-card variant="outlined" class="text-center pa-3">
                      <div class="text-caption text-grey-darken-1">현재 평가액</div>
                      <div class="text-h6 font-weight-bold mt-1">{{ formatCurrency(stats.totalCurrentValue) }}</div>
                    </v-card>
                  </v-col>
                  <v-col cols="6" md="3">
                    <v-card variant="outlined" class="text-center pa-3">
                      <div class="text-caption text-grey-darken-1">평가 손익</div>
                      <div 
                        class="text-h6 font-weight-bold mt-1"
                        :class="stats.totalProfitLoss >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2'"
                      >
                        {{ formatCurrency(stats.totalProfitLoss) }}
                      </div>
                    </v-card>
                  </v-col>
                  <v-col cols="6" md="3">
                    <v-card variant="outlined" class="text-center pa-3 highlight-card">
                      <div class="text-caption text-grey-darken-1">수익률</div>
                      <div 
                        class="text-h6 font-weight-bold mt-1"
                        :class="stats.totalProfitLossPct >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2'"
                      >
                        {{ stats.totalProfitLossPct >= 0 ? '+' : '' }}{{ stats.totalProfitLossPct.toFixed(2) }}%
                      </div>
                    </v-card>
                  </v-col>
                </v-row>

                <!-- 보유 목록 테이블 -->
                <v-data-table
                  :headers="holdingHeaders"
                  :items="holdings"
                  :loading="loading"
                  items-per-page="10"
                >
                  <template v-slot:item.coinSymbol="{ item }">
                    <strong>{{ item.coinSymbol }}</strong>
                  </template>

                  <template v-slot:item.quantity="{ item }">
                    {{ formatNumber(item.quantity, 8) }}
                  </template>

                  <template v-slot:item.price="{ item }">
                    {{ formatCurrency(item.price) }}
                  </template>

                  <template v-slot:item.totalAmount="{ item }">
                    {{ formatCurrency(item.totalAmount) }}
                  </template>

                  <template v-slot:item.currentPrice="{ item }">
                    <span v-if="item.currentPrice">{{ formatCurrency(item.currentPrice) }}</span>
                    <span v-else class="text-grey">-</span>
                  </template>

                  <template v-slot:item.currentValue="{ item }">
                    <span v-if="item.currentPrice">{{ formatCurrency(item.currentPrice * item.quantity) }}</span>
                    <span v-else class="text-grey">-</span>
                  </template>

                  <template v-slot:item.profitLoss="{ item }">
                    <span
                      v-if="item.currentProfitLoss !== null && item.currentProfitLoss !== undefined"
                      :class="item.currentProfitLoss >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2'"
                    >
                      {{ formatCurrency(item.currentProfitLoss) }}
                      <br>
                      <span class="text-caption">({{ item.currentProfitLossPct?.toFixed(2) }}%)</span>
                    </span>
                    <span v-else class="text-grey">-</span>
                  </template>

                  <template v-slot:item.createdAt="{ item }">
                    {{ formatDateTime(item.createdAt) }}
                  </template>

                  <template v-slot:item.actions="{ item }">
                    <v-btn color="orange" size="small" @click="openSellDialog(item)">매도</v-btn>
                    <v-btn color="grey" size="small" @click="viewDetail(item)" class="ml-1">상세</v-btn>
                  </template>
                </v-data-table>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </v-main>

    <!-- 매도 다이얼로그 -->
    <v-dialog v-model="sellDialog" max-width="500">
      <v-card>
        <v-card-title>매도 처리</v-card-title>
        <v-card-text>
          <div v-if="selectedHolding">
            <p><strong>코인:</strong> {{ selectedHolding.coinSymbol }}</p>
            <p><strong>보유 수량:</strong> {{ formatNumber(selectedHolding.quantity, 8) }}</p>
            <p><strong>매수 평균가:</strong> {{ formatCurrency(selectedHolding.price) }}</p>
            <p v-if="selectedHolding.currentPrice">
              <strong>현재가:</strong> {{ formatCurrency(selectedHolding.currentPrice) }}
            </p>
            <p v-if="selectedHolding.currentProfitLoss !== null">
              <strong>예상 손익:</strong>
              <span :class="selectedHolding.currentProfitLoss >= 0 ? 'text-teal' : 'text-red'">
                {{ formatCurrency(selectedHolding.currentProfitLoss) }}
                ({{ selectedHolding.currentProfitLossPct?.toFixed(2) }}%)
              </span>
            </p>
            <v-text-field
              v-model.number="sellPrice"
              label="매도 가격"
              type="number"
              :rules="[v => v > 0 || '가격은 0보다 커야 합니다']"
              class="mt-4"
            />
          </div>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="sellDialog = false">취소</v-btn>
          <v-btn color="orange" @click="confirmSell" :loading="sellLoading">매도 확인</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 보유 상세 다이얼로그 -->
    <v-dialog v-model="detailDialog" max-width="600">
      <v-card>
        <v-card-title>보유 자산 상세</v-card-title>
        <v-card-text>
          <div v-if="selectedHolding">
            <v-list>
              <v-list-item>
                <v-list-item-title>거래 ID</v-list-item-title>
                <v-list-item-subtitle>{{ selectedHolding.transactionId }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>코인</v-list-item-title>
                <v-list-item-subtitle>{{ selectedHolding.coinSymbol }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>보유 수량</v-list-item-title>
                <v-list-item-subtitle>{{ formatNumber(selectedHolding.quantity, 8) }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>매수 가격</v-list-item-title>
                <v-list-item-subtitle>{{ formatCurrency(selectedHolding.price) }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>투자 금액</v-list-item-title>
                <v-list-item-subtitle>{{ formatCurrency(selectedHolding.totalAmount) }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item v-if="selectedHolding.currentPrice">
                <v-list-item-title>현재가</v-list-item-title>
                <v-list-item-subtitle>{{ formatCurrency(selectedHolding.currentPrice) }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item v-if="selectedHolding.targetSellPrice">
                <v-list-item-title>목표 매도가</v-list-item-title>
                <v-list-item-subtitle>{{ formatCurrency(selectedHolding.targetSellPrice) }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item v-if="selectedHolding.stopLossPrice">
                <v-list-item-title>손절가</v-list-item-title>
                <v-list-item-subtitle>{{ formatCurrency(selectedHolding.stopLossPrice) }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>매수 시각</v-list-item-title>
                <v-list-item-subtitle>{{ formatDateTime(selectedHolding.createdAt) }}</v-list-item-subtitle>
              </v-list-item>
            </v-list>
          </div>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="detailDialog = false">닫기</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 코인별 상세 다이얼로그 -->
    <v-dialog v-model="coinDetailDialog" max-width="700">
      <v-card v-if="selectedCoin">
        <v-card-title class="bg-amber-darken-2 text-white">
          <v-icon class="mr-2">mdi-bitcoin</v-icon>
          {{ selectedCoin.coinSymbol.replace('KRW-', '') }} 수익 상세
          <span class="text-caption ml-2">({{ selectedCoin.coinName }})</span>
        </v-card-title>
        <v-card-text class="pa-4">
          <v-row>
            <v-col cols="6" md="4">
              <div class="text-caption text-grey-darken-1">총 실현 수익</div>
              <div 
                class="text-h6 font-weight-bold"
                :class="selectedCoin.totalProfit >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2'"
              >
                {{ formatCurrency(selectedCoin.totalProfit) }}
              </div>
            </v-col>
            <v-col cols="6" md="4">
              <div class="text-caption text-grey-darken-1">수익률</div>
              <div class="text-h6 font-weight-bold" :class="selectedCoin.profitPct >= 0 ? 'text-teal' : 'text-red'">
                {{ selectedCoin.profitPct >= 0 ? '+' : '' }}{{ selectedCoin.profitPct?.toFixed(2) }}%
              </div>
            </v-col>
            <v-col cols="6" md="4">
              <div class="text-caption text-grey-darken-1">총 거래 건수</div>
              <div class="text-h6 font-weight-bold">{{ selectedCoin.totalTradeCount }}건</div>
            </v-col>
          </v-row>

          <v-divider class="my-4" />

          <v-row>
            <v-col cols="6" md="3">
              <div class="text-caption text-grey-darken-1">익절</div>
              <div class="text-body-1 font-weight-medium text-teal">{{ selectedCoin.winCount }}건</div>
            </v-col>
            <v-col cols="6" md="3">
              <div class="text-caption text-grey-darken-1">손절</div>
              <div class="text-body-1 font-weight-medium text-red">{{ selectedCoin.loseCount }}건</div>
            </v-col>
            <v-col cols="6" md="3">
              <div class="text-caption text-grey-darken-1">승률</div>
              <div class="text-body-1 font-weight-medium">{{ selectedCoin.winRate?.toFixed(1) }}%</div>
            </v-col>
            <v-col cols="6" md="3">
              <div class="text-caption text-grey-darken-1">현재 보유</div>
              <div class="text-body-1 font-weight-medium">{{ selectedCoin.currentHoldingCount }}건</div>
            </v-col>
          </v-row>

          <v-divider class="my-4" />

          <v-row>
            <v-col cols="6">
              <div class="text-caption text-grey-darken-1">총 매수 금액</div>
              <div class="text-body-1">{{ formatCurrency(selectedCoin.totalBuyAmount) }}</div>
            </v-col>
            <v-col cols="6">
              <div class="text-caption text-grey-darken-1">총 매도 금액</div>
              <div class="text-body-1">{{ formatCurrency(selectedCoin.totalSellAmount) }}</div>
            </v-col>
            <v-col cols="6">
              <div class="text-caption text-grey-darken-1">평균 매수가</div>
              <div class="text-body-1">{{ formatCurrency(selectedCoin.avgBuyPrice) }}</div>
            </v-col>
            <v-col cols="6">
              <div class="text-caption text-grey-darken-1">평균 매도가</div>
              <div class="text-body-1">{{ formatCurrency(selectedCoin.avgSellPrice) }}</div>
            </v-col>
            <v-col cols="6">
              <div class="text-caption text-grey-darken-1">최대 수익 거래</div>
              <div class="text-body-1 text-teal font-weight-medium">{{ formatCurrency(selectedCoin.maxProfit) }}</div>
            </v-col>
            <v-col cols="6">
              <div class="text-caption text-grey-darken-1">최대 손실 거래</div>
              <div class="text-body-1 text-red font-weight-medium">{{ formatCurrency(selectedCoin.maxLoss) }}</div>
            </v-col>
          </v-row>

          <div v-if="selectedCoin.lastTradeAt" class="mt-4 text-caption text-grey">
            마지막 거래: {{ formatDateTime(selectedCoin.lastTradeAt) }}
          </div>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn color="amber-darken-2" variant="flat" @click="coinDetailDialog = false">닫기</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-snackbar v-model="snackbar" :color="snackbarColor" :timeout="3000">
      {{ snackbarMessage }}
    </v-snackbar>
  </v-app>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { transactionApi, profitApi } from '@/api'
import type { Transaction, DashboardStats } from '@/types'
import type { ProfitSummary, PeriodProfit, CoinProfit } from '@/types/profit'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'
import HelpButton from '@/components/HelpButton.vue'

const sidebarRef = ref()
// 도움말 콘텐츠
const helpContents = {
  periodProfit: {
    title: '📅 기간별 수익 분석',
    content: `
      <div class="help-box">
        <p><strong>📖 쉬운 설명:</strong> 선택한 기간 동안의 거래 수익을 분석합니다.</p>
        <p style="margin-top: 8px;"><strong>📆 기간 선택:</strong></p>
        <p>• <strong>오늘</strong>: 당일 00:00~현재까지의 수익</p>
        <p>• <strong>이번달</strong>: 이번 달 1일~현재까지</p>
        <p>• <strong>올해</strong>: 올해 1월 1일~현재까지</p>
        <p>• <strong>1년</strong>: 최근 365일간</p>
        <p>• <strong>누적</strong>: 첫 거래~현재까지 전체</p>
        <p style="margin-top: 8px;"><strong>📊 주요 지표:</strong></p>
        <p>• <strong>총 수익</strong>: 해당 기간 실현 손익 합계</p>
        <p>• <strong>거래 건수</strong>: 매도 완료된 거래 수</p>
        <p>• <strong>승률</strong>: 익절 거래 / 전체 거래 × 100%</p>
        <p style="margin-top: 8px;"><strong>💡 팁:</strong> 사용자 지정 기간으로 원하는 기간을 분석할 수 있습니다.</p>
      </div>
    `
  },
  coinProfit: {
    title: '💰 코인별 수익 분석',
    content: `
      <div class="help-box">
        <p><strong>📖 쉬운 설명:</strong> 각 코인별로 거래 성과를 분석합니다.</p>
        <p style="margin-top: 8px;"><strong>📊 주요 지표:</strong></p>
        <p>• <strong>실현 수익</strong>: 해당 코인에서 실현된 총 손익</p>
        <p>• <strong>수익률</strong>: 투자 대비 수익 비율</p>
        <p>• <strong>익절/손절</strong>: 수익 거래 수 / 손실 거래 수</p>
        <p>• <strong>승률</strong>: 익절 거래 / 전체 거래 × 100%</p>
        <p style="margin-top: 8px;"><strong>💡 활용법:</strong></p>
        <p>• 승률이 낮아도 평균 수익이 높으면 좋은 전략입니다.</p>
        <p>• 특정 코인에서 계속 손실이 나면 거래 종목에서 제외를 고려하세요.</p>
        <p>• "상세" 버튼으로 코인별 세부 통계를 확인할 수 있습니다.</p>
      </div>
    `
  },
  holdings: {
    title: '📦 보유 현황',
    content: `
      <div class="help-box">
        <p><strong>📖 쉬운 설명:</strong> 현재 보유 중인 코인의 평가 손익을 보여줍니다.</p>
        <p style="margin-top: 8px;"><strong>📊 용어 설명:</strong></p>
        <p>• <strong>투자금액</strong>: 매수 시 사용한 금액 (매수가 × 수량)</p>
        <p>• <strong>평가액</strong>: 현재가 × 보유수량</p>
        <p>• <strong>평가 손익</strong>: 평가액 - 투자금액</p>
        <p>• <strong>수익률</strong>: (현재가 - 매수가) / 매수가 × 100%</p>
        <p style="margin-top: 8px;"><strong>⚠️ 주의:</strong></p>
        <p>• 평가 손익은 <strong>미실현 손익</strong>입니다.</p>
        <p>• 매도하기 전까지는 확정된 수익/손실이 아닙니다.</p>
        <p>• 현재가는 실시간으로 변동됩니다.</p>
        <p style="margin-top: 8px;"><strong>💡 팁:</strong></p>
        <p>• "매도" 버튼으로 수동 매도가 가능합니다.</p>
        <p>• "상세" 버튼으로 목표가, 손절가 등을 확인할 수 있습니다.</p>
      </div>
    `
  }
}

// 탭 상태
const profitTab = ref('period')

// 기존 보유 현황 상태
const holdings = ref<Transaction[]>([])
const stats = ref<DashboardStats>({
  totalHoldingAmount: 0,
  totalCurrentValue: 0,
  totalProfitLoss: 0,
  totalProfitLossPct: 0,
  realizedProfitLoss: 0,
  soldCount: 0,
  totalBuyCount: 0,
  totalSellCount: 0,
  currentHoldingCount: 0,
  todayBuyAmount: 0,
  todaySellAmount: 0,
  todayBuyCount: 0,
  todaySellCount: 0,
  dailyLimitAmount: 0,
  remainingDailyLimit: 0
})

const loading = ref(false)
const sellLoading = ref(false)
const sellDialog = ref(false)
const detailDialog = ref(false)
const selectedHolding = ref<Transaction | null>(null)
const sellPrice = ref(0)
// 기간별 수익 상태
const loadingProfit = ref(false)
const profitSummary = ref<ProfitSummary>({
  todayProfit: 0, todayProfitPct: 0, todayTradeCount: 0,
  monthProfit: 0, monthProfitPct: 0, monthTradeCount: 0,
  yearProfit: 0, yearProfitPct: 0, yearTradeCount: 0,
  oneYearProfit: 0, oneYearProfitPct: 0, oneYearTradeCount: 0,
  totalProfit: 0, totalProfitPct: 0, totalTradeCount: 0,
  initialInvestment: 0
})
const selectedPeriod = ref<string>('total')
const periodDetail = ref<PeriodProfit | null>(null)

// 사용자 지정 기간
const customStartDate = ref('')
const customEndDate = ref('')

// 차트 관련
const chartViewMode = ref<'full' | 'scroll'>('full')
const assetHistory = ref<any[]>([])
const initialAsset = ref(1000000)
const hoveredIndex = ref(-1)
const tooltipX = ref(0)
const tooltipY = ref(0)
const chartWrapperWidth = ref(800) // ⭐⭐⭐ [추가] 차트 wrapper 너비 (툴팁 좌/우 판단용)

// 차트 상수
const svgWidth = 800
const svgHeight = 350
const svgPadding = 30
// ⭐⭐⭐ [변경] 우측 라벨 영역 여백 확대 (100 → 120) ⭐⭐⭐
// 왜: 대시보드(120)와 동일하게 맞춰 마지막 점이 라벨에 가려지지 않도록 함
const svgPaddingRight = 120

// 코인별 수익 상태
const loadingCoin = ref(false)
const coinProfits = ref<CoinProfit[]>([])
const coinDetailDialog = ref(false)
const selectedCoin = ref<CoinProfit | null>(null)

const snackbar = ref(false)
const snackbarMessage = ref('')
const snackbarColor = ref('success')

// 보유 현황 테이블 헤더
const holdingHeaders = [
  { title: '거래 ID', key: 'transactionId', align: 'center' },
  { title: '코인', key: 'coinSymbol', align: 'center' },
  { title: '수량', key: 'quantity', align: 'end' },
  { title: '매수가', key: 'price', align: 'end' },
  { title: '투자금액', key: 'totalAmount', align: 'end' },
  { title: '현재가', key: 'currentPrice', align: 'end' },
  { title: '평가액', key: 'currentValue', align: 'end' },
  { title: '평가손익', key: 'profitLoss', align: 'end' },
  { title: '매수시각', key: 'createdAt', align: 'center' },
  { title: '액션', key: 'actions', align: 'center', sortable: false }
]

// 코인별 수익 테이블 헤더
const coinHeaders = [
  { title: '코인', key: 'coinSymbol', align: 'start' },
  { title: '실현 수익', key: 'totalProfit', align: 'end' },
  { title: '수익률', key: 'profitPct', align: 'center' },
  { title: '거래', key: 'totalTradeCount', align: 'center' },
  { title: '익절/손절 (승률)', key: 'winRate', align: 'center' },
  { title: '보유 현황', key: 'currentHoldingCount', align: 'center' },
  { title: '', key: 'actions', align: 'center', sortable: false }
]

// 차트 computed
const effectiveWidth = computed(() => chartViewMode.value === 'scroll' ? dynamicChartWidth.value : svgWidth)

const scrollPaddingRight = 220
const dynamicChartWidth = computed(() => {
  const pointCount = assetHistory.value.length
  return Math.max(svgWidth, pointCount * 25 + svgPadding + scrollPaddingRight)
})

// ⭐⭐⭐ [변경] Y축 범위: 평가금액과 불입금액 모두 포함 ⭐⭐⭐
const maxBalance = computed(() => {
  if (!assetHistory.value.length) return initialAsset.value
  const maxEval = Math.max(...assetHistory.value.map(d => d.evaluationAmount || d.balance))
  const maxDeposit = Math.max(...assetHistory.value.map(d => d.depositAmount || initialAsset.value))
  return Math.max(maxEval, maxDeposit)
})

const minBalance = computed(() => {
  if (!assetHistory.value.length) return 0
  const minEval = Math.min(...assetHistory.value.map(d => d.evaluationAmount || d.balance))
  const minDeposit = Math.min(...assetHistory.value.map(d => d.depositAmount || initialAsset.value))
  const minValue = Math.min(minEval, minDeposit)
  if (minValue <= 0) return 0
  return Math.floor(minValue * 0.95)
})

// ⭐⭐⭐ [신규 추가] 스냅샷 기반 차트 computed ⭐⭐⭐
const maxEvaluation = computed(() => {
  if (!assetHistory.value.length) return initialAsset.value
  return Math.max(...assetHistory.value.map(d => d.evaluationAmount || d.balance || 0))
})

const minEvaluation = computed(() => {
  if (!assetHistory.value.length) return initialAsset.value
  return Math.min(...assetHistory.value.map(d => d.evaluationAmount || d.balance || 0))
})

const latestDepositAmount = computed(() => {
  if (!assetHistory.value.length) return initialAsset.value
  const last = assetHistory.value[assetHistory.value.length - 1]
  return last.depositAmount || initialAsset.value
})

const latestEvaluationAmount = computed(() => {
  if (!assetHistory.value.length) return initialAsset.value
  const last = assetHistory.value[assetHistory.value.length - 1]
  return last.evaluationAmount || last.balance || initialAsset.value
})

const barWidth = computed(() => {
  const total = assetHistory.value.length
  if (total <= 1) return 20
  const chartWidth = effectiveWidth.value - svgPadding - svgPaddingRight
  return Math.max(4, Math.min(20, (chartWidth / total) * 0.6))
})

// ⭐⭐⭐ [변경] evaluationAmount, depositAmount 포함 ⭐⭐⭐
const chartPoints = computed(() => {
  if (!assetHistory.value.length) return []
  const total = assetHistory.value.length
  // ⭐⭐⭐ [변경] 스크롤 모드: 우측 라벨 공간을 더 확보하여 점과 라벨 분리 ⭐⭐⭐
  // 왜: 스크롤 모드에서 라벨 텍스트(약200px)가 마지막 점들과 겹치지 않도록
  const rightPad = chartViewMode.value === 'scroll' ? scrollPaddingRight : svgPaddingRight
  const width = effectiveWidth.value - svgPadding - rightPad
  return assetHistory.value.map((d, index) => ({
    x: svgPadding + (index / (total - 1 || 1)) * width,
    y: getYPosition(d.evaluationAmount || d.balance),
    balance: d.evaluationAmount || d.balance || 0,
    evaluationAmount: d.evaluationAmount || d.balance || 0,
    depositAmount: d.depositAmount || initialAsset.value
  }))
})

const linePath = computed(() => {
  if (!chartPoints.value.length) return ''
  return chartPoints.value.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ')
})

// ⭐⭐⭐ [신규 추가] 불입금액 추세선 path ⭐⭐⭐
// ⭐⭐⭐ [변경] 불입금액 추세선도 막대 상단 위치와 일치 ⭐⭐⭐
const depositLinePath = computed(() => {
  if (!chartPoints.value.length) return ''
  return chartPoints.value
    .map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${getYPosition(p.depositAmount)}`)
    .join(' ')
})

const areaPath = computed(() => {
  if (!chartPoints.value.length) return ''
  const points = chartPoints.value
  const bottomY = svgHeight - svgPadding
  return `M ${points[0].x} ${bottomY} L ${points.map(p => `${p.x} ${p.y}`).join(' L ')} L ${points[points.length - 1].x} ${bottomY} Z`
})

const hoveredData = computed(() => hoveredIndex.value >= 0 ? assetHistory.value[hoveredIndex.value] : null)

const currentPeriodProfit = computed(() => {
  switch (selectedPeriod.value) {
    case 'today': return profitSummary.value.todayProfit
    case 'month': return profitSummary.value.monthProfit
    case 'year': return profitSummary.value.yearProfit
    case 'oneYear': return profitSummary.value.oneYearProfit
    case 'total': return profitSummary.value.totalProfit
    default: return periodDetail.value?.totalProfit || 0
  }
})

const currentPeriodProfitPct = computed(() => {
  switch (selectedPeriod.value) {
    case 'today': return profitSummary.value.todayProfitPct
    case 'month': return profitSummary.value.monthProfitPct
    case 'year': return profitSummary.value.yearProfitPct
    case 'oneYear': return profitSummary.value.oneYearProfitPct
    case 'total': return profitSummary.value.totalProfitPct
    default: return periodDetail.value?.profitPct || 0
  }
})

// 함수들
const getYPosition = (balance: number) => {
  const max = maxBalance.value
  const min = minBalance.value
  const range = max - min || 1
  return svgPadding + ((max - balance) / range) * (svgHeight - svgPadding * 2)
}

const getLabelPosition = (balance: number) => {
  const max = maxBalance.value
  const min = minBalance.value
  const range = max - min || 1
  const paddingPercent = (svgPadding / svgHeight) * 100
  const usableHeight = 100 - paddingPercent * 2
  return paddingPercent + ((max - balance) / range) * usableHeight
}

// ⭐⭐⭐ [신규 추가] 라벨 겹침 방지 위치 계산 ⭐⭐⭐
const getAdjustedLabelPosition = (type: string) => {
  const positions = [
    { type: 'max', value: maxEvaluation.value, raw: getLabelPosition(maxEvaluation.value) },
    { type: 'evaluation', value: latestEvaluationAmount.value, raw: getLabelPosition(latestEvaluationAmount.value) },
    { type: 'deposit', value: latestDepositAmount.value, raw: getLabelPosition(latestDepositAmount.value) },
    { type: 'min', value: minEvaluation.value, raw: getLabelPosition(minEvaluation.value) },
    { type: 'floor', value: minBalance.value, raw: getLabelPosition(minBalance.value) }
  ]

  positions.sort((a, b) => a.raw - b.raw)

  const minGap = 4
  for (let i = 1; i < positions.length; i++) {
    if (positions[i].raw - positions[i - 1].raw < minGap) {
      positions[i].raw = positions[i - 1].raw + minGap
    }
  }

  const found = positions.find(p => p.type === type)
  return found ? found.raw : 0
}

// ⭐⭐⭐ [변경] 불입금액 기준으로 색상 판단 ⭐⭐⭐
const getPointColor = (evaluationAmount: number) => {
  const deposit = latestDepositAmount.value
  if (evaluationAmount > deposit * 1.01) return '#4CAF50'
  if (evaluationAmount < deposit * 0.99) return '#F44336'
  return '#1976D2'
}

const handleChartHover = (event: MouseEvent) => {
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  
  const currentWidth = chartViewMode.value === 'scroll' ? dynamicChartWidth.value : svgWidth
  // ⭐⭐⭐ [변경] SVG 좌표 변환: 전체보기 모드에서 wrapper 너비 기준으로 정확히 매핑 ⭐⭐⭐
  // 왜: 기존 Math.floor는 index를 한 칸 앞으로 밀어 호버 위치가 어긋남
  //     Math.round로 변경하여 가장 가까운 점을 정확히 선택
  const svgX = (x / rect.width) * currentWidth
  // ⭐⭐⭐ [변경] 스크롤 모드: chartPoints와 동일한 rightPad 사용 ⭐⭐⭐
  // 왜: chartPoints는 scrollPaddingRight(220)으로 점 위치를 계산하는데
  //     호버는 svgPaddingRight(120)로 계산하면 좌표 불일치 발생
  const rightPad = chartViewMode.value === 'scroll' ? scrollPaddingRight : svgPaddingRight
  const chartWidth = currentWidth - svgPadding - rightPad
  
  const total = assetHistory.value.length
  if (total === 0) return
  
  // ⭐⭐⭐ [변경] Math.floor → Math.round로 변경 ⭐⭐⭐
  // 왜: floor는 소수점 버림으로 항상 왼쪽 점을 선택 → 마우스 위치와 불일치
  //     round는 가장 가까운 점을 선택 → 마우스 위치와 일치
  const ratio = Math.max(0, Math.min(1, (svgX - svgPadding) / chartWidth))
  const index = Math.round(ratio * (total - 1))
  hoveredIndex.value = Math.max(0, Math.min(total - 1, index))
  tooltipX.value = x
  chartWrapperWidth.value = rect.width  // ⭐⭐⭐ [추가] wrapper 너비 저장
  
  if (chartPoints.value[hoveredIndex.value]) {
    tooltipY.value = chartPoints.value[hoveredIndex.value].y * (rect.height / svgHeight)
  }
}

const getPeriodLabel = (period: string) => {
  const labels: Record<string, string> = {
    today: '오늘',
    month: '이번달',
    year: '올해',
    oneYear: '1년',
    total: '누적',
    custom: '사용자 지정'
  }
  return labels[period] || period
}

// 사용자 지정 기간 조회 - 차트도 함께 갱신
const applyCustomPeriod = async () => {
  if (!customStartDate.value || !customEndDate.value) {
    showSnackbar('시작일과 종료일을 입력해주세요', 'warning')
    return
  }
  
  loadingProfit.value = true
  try {
    selectedPeriod.value = 'custom'
    await loadPeriodDetail('custom')
    await loadAssetHistoryByCustomPeriod(customStartDate.value, customEndDate.value)
  } finally {
    loadingProfit.value = false
  }
}

// 사용자 지정 기간 자산 이력 로드
// ⭐⭐⭐ [변경] 백엔드 스냅샷 API 우선 조회 + 기존 로직 폴백 ⭐⭐⭐
const loadAssetHistoryByCustomPeriod = async (startDateStr: string, endDateStr: string) => {
  try {
    // ⭐ 1단계: 백엔드 스냅샷 API 우선 조회
    try {
      const snapshotResponse = await profitApi.getAssetSnapshotsByRange(startDateStr, endDateStr)
      const snapshots = snapshotResponse.data?.data || snapshotResponse.data || []
      if (snapshots.length > 0) {
        assetHistory.value = snapshots.map((s: any) => ({
          date: s.date,
          balance: parseFloat(s.evaluationAmount) || 0,
          evaluationAmount: parseFloat(s.evaluationAmount) || 0,
          depositAmount: parseFloat(s.depositAmount) || 0,
          profitAmount: parseFloat(s.profitAmount) || 0,
          profitRate: parseFloat(s.profitRate) || 0
        }))
        return
      }
    } catch (snapshotError) {
      console.warn('스냅샷 API 조회 실패, 기존 방식으로 폴백:', snapshotError)
    }

    // ⭐ 2단계: 폴백 - 기존 SOLD 거래 기반 계산
    const response = await transactionApi.search({ status: 'SOLD', page: 0, size: 1000 })
    const transactions = response.data?.content || []
    
    if (transactions.length === 0) {
      assetHistory.value = []
      return
    }
    
    const startDate = new Date(startDateStr)
    const endDate = new Date(endDateStr)
    endDate.setHours(23, 59, 59, 999)
    
    const filteredTxs = transactions.filter((tx: any) => {
      const txDate = new Date(tx.soldAt || tx.createdAt)
      return txDate >= startDate && txDate <= endDate
    })
    
    const sortedTxs = [...filteredTxs].sort((a: any, b: any) => 
      new Date(a.soldAt || a.createdAt).getTime() - new Date(b.soldAt || b.createdAt).getTime()
    )
    
    const dailyMap = new Map<string, number>()
    let runningBalance = initialAsset.value
    
    sortedTxs.forEach((tx: any) => {
      const dateKey = new Date(tx.soldAt || tx.createdAt).toISOString().split('T')[0]
      if (tx.profitLoss) {
        runningBalance += tx.profitLoss
      }
      dailyMap.set(dateKey, runningBalance)
    })
    
    // ⭐⭐⭐ [변경] 사용자 지정 시작일~종료일까지 모든 날짜 채우기 ⭐⭐⭐
    const allDates3: Array<any> = []
    let currentBalance3 = initialAsset.value
    const customStart = new Date(startDateStr)
    customStart.setHours(12, 0, 0, 0)
    const customEnd = new Date(endDateStr)
    customEnd.setHours(12, 0, 0, 0)
    const customEndStr2 = `${customEnd.getFullYear()}-${String(customEnd.getMonth() + 1).padStart(2, '0')}-${String(customEnd.getDate()).padStart(2, '0')}`
    const customCurrent = new Date(customStart)

    while (true) {
      const dateKey = `${customCurrent.getFullYear()}-${String(customCurrent.getMonth() + 1).padStart(2, '0')}-${String(customCurrent.getDate()).padStart(2, '0')}`
      if (dailyMap.has(dateKey)) {
        currentBalance3 = dailyMap.get(dateKey)!
      }
      allDates3.push({
        date: dateKey,
        balance: currentBalance3,
        evaluationAmount: currentBalance3,
        depositAmount: initialAsset.value,
        profitAmount: currentBalance3 - initialAsset.value,
        profitRate: ((currentBalance3 - initialAsset.value) / initialAsset.value) * 100
      })
      if (dateKey >= customEndStr2) break
      customCurrent.setDate(customCurrent.getDate() + 1)
    }
    assetHistory.value = allDates3
  } catch (error) {
    console.error('사용자 지정 기간 자산 이력 조회 실패:', error)
    assetHistory.value = []
  }
}

// API 호출 함수들
const loadHoldings = async () => {
  loading.value = true
  try {
    const [holdingsResponse, statsResponse] = await Promise.all([
      transactionApi.getHoldings(),
      transactionApi.getStats()
    ])
    holdings.value = holdingsResponse.data
    stats.value = statsResponse.data
  } catch (error: any) {
    showSnackbar(error.response?.data?.message || '보유 자산 조회 실패', 'error')
  } finally {
    loading.value = false
  }
}

const loadProfitSummary = async () => {
  loadingProfit.value = true
  try {
    const response = await profitApi.getSummary()
    profitSummary.value = response.data?.data || response.data || {}
    
    // 초기 자산 설정
    initialAsset.value = profitSummary.value.initialInvestment || 1000000
    
    // 자산 이력 생성 (전체 기간)
    await loadAssetHistory()
  } catch (error: any) {
    console.error('수익 요약 조회 실패:', error)
    showSnackbar('수익 요약 조회 실패', 'error')
  } finally {
    loadingProfit.value = false
  }
}

const loadPeriodDetail = async (period: string) => {
  try {
    if (period === 'custom') {
      // 사용자 지정 기간은 별도 API 필요 (현재는 미구현)
      showSnackbar('사용자 지정 기간 조회 완료', 'success')
      return
    }
    const response = await profitApi.getByPeriod(period)
    periodDetail.value = response.data?.data || response.data || null
  } catch (error: any) {
    console.error('기간별 수익 조회 실패:', error)
  }
}

// ⭐⭐⭐ [변경] 백엔드 스냅샷 API 우선 조회 + 기존 로직 폴백 ⭐⭐⭐
const loadAssetHistory = async () => {
  try {
    // ⭐ 1단계: 백엔드 스냅샷 API 우선 조회
    try {
      const snapshotResponse = await profitApi.getAssetSnapshots('all')
      const snapshots = snapshotResponse.data?.data || snapshotResponse.data || []
      if (snapshots.length > 0) {
        assetHistory.value = snapshots.map((s: any) => ({
          date: s.date,
          balance: parseFloat(s.evaluationAmount) || 0,
          evaluationAmount: parseFloat(s.evaluationAmount) || 0,
          depositAmount: parseFloat(s.depositAmount) || 0,
          profitAmount: parseFloat(s.profitAmount) || 0,
          profitRate: parseFloat(s.profitRate) || 0
        }))
        return
      }
    } catch (snapshotError) {
      console.warn('스냅샷 API 조회 실패, 기존 방식으로 폴백:', snapshotError)
    }

    // ⭐ 2단계: 폴백 - 기존 SOLD 거래 기반 계산
    const response = await transactionApi.search({ status: 'SOLD', page: 0, size: 1000 })
    const transactions = response.data?.content || []
    
    if (transactions.length === 0) {
      assetHistory.value = []
      return
    }
    
    const sortedTxs = [...transactions].sort((a: any, b: any) => 
      new Date(a.soldAt || a.createdAt).getTime() - new Date(b.soldAt || b.createdAt).getTime()
    )
    
    const dailyMap = new Map<string, number>()
    let runningBalance = initialAsset.value
    
    sortedTxs.forEach((tx: any) => {
      const dateKey = new Date(tx.soldAt || tx.createdAt).toISOString().split('T')[0]
      if (tx.profitLoss) {
        runningBalance += tx.profitLoss
      }
      dailyMap.set(dateKey, runningBalance)
    })
    
    // ⭐⭐⭐ [변경] 첫 거래일~오늘까지 모든 날짜 채우기 ⭐⭐⭐
    // 왜: SOLD 거래일만 표시하면 막대가 2개만 나옴. 모든 날짜를 채워야 함
    const sortedDates = Array.from(dailyMap.keys()).sort()
    const firstDate = new Date(sortedDates[0])
    firstDate.setHours(0, 0, 0, 0)
    const today = new Date()
    today.setHours(12, 0, 0, 0)

    const allDates: Array<any> = []
    let currentBalance = initialAsset.value
    const currentDate = new Date(firstDate)
    currentDate.setHours(12, 0, 0, 0)
    const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

    while (true) {
      const dateKey = `${currentDate.getFullYear()}-${String(currentDate.getMonth() + 1).padStart(2, '0')}-${String(currentDate.getDate()).padStart(2, '0')}`
      if (dailyMap.has(dateKey)) {
        currentBalance = dailyMap.get(dateKey)!
      }
      allDates.push({
        date: dateKey,
        balance: currentBalance,
        evaluationAmount: currentBalance,
        depositAmount: initialAsset.value,
        profitAmount: currentBalance - initialAsset.value,
        profitRate: ((currentBalance - initialAsset.value) / initialAsset.value) * 100
      })
      if (dateKey >= todayStr) break
      currentDate.setDate(currentDate.getDate() + 1)
    }
    assetHistory.value = allDates
  } catch (error) {
    console.error('자산 이력 조회 실패:', error)
    assetHistory.value = []
  }
}

// 기간별 자산 이력 로드 함수
// ⭐⭐⭐ [변경] 백엔드 스냅샷 API 우선 조회 + 기존 로직 폴백 ⭐⭐⭐
const loadAssetHistoryByPeriod = async (period: string) => {
  try {
    // ⭐ 1단계: 백엔드 스냅샷 API 우선 조회 (period 매핑)
    let snapshotPeriod = period
    if (period === 'today') snapshotPeriod = '7'  // today는 7일로 대체
    if (period === 'oneYear') snapshotPeriod = 'year'
    if (period === 'total') snapshotPeriod = 'all'
    
    try {
      const snapshotResponse = await profitApi.getAssetSnapshots(snapshotPeriod)
      const snapshots = snapshotResponse.data?.data || snapshotResponse.data || []
      if (snapshots.length > 0) {
        assetHistory.value = snapshots.map((s: any) => ({
          date: s.date,
          balance: parseFloat(s.evaluationAmount) || 0,
          evaluationAmount: parseFloat(s.evaluationAmount) || 0,
          depositAmount: parseFloat(s.depositAmount) || 0,
          profitAmount: parseFloat(s.profitAmount) || 0,
          profitRate: parseFloat(s.profitRate) || 0
        }))
        return
      }
    } catch (snapshotError) {
      console.warn('스냅샷 API 조회 실패, 기존 방식으로 폴백:', snapshotError)
    }

    // ⭐ 2단계: 폴백 - 기존 SOLD 거래 기반 계산
    const response = await transactionApi.search({ status: 'SOLD', page: 0, size: 1000 })
    const transactions = response.data?.content || []
    
    if (transactions.length === 0) {
      assetHistory.value = []
      return
    }
    
    const now = new Date()
    let startDate: Date
    
    switch (period) {
      case 'today':
        startDate = new Date(now.getFullYear(), now.getMonth(), now.getDate())
        break
      case 'month':
        startDate = new Date(now.getFullYear(), now.getMonth(), 1)
        break
      case 'year':
        startDate = new Date(now.getFullYear(), 0, 1)
        break
      case 'oneYear':
        startDate = new Date(now.getFullYear() - 1, now.getMonth(), now.getDate())
        break
      case 'total':
      default:
        startDate = new Date(2020, 0, 1)
        break
    }
    
    const filteredTxs = transactions.filter((tx: any) => {
      const txDate = new Date(tx.soldAt || tx.createdAt)
      return txDate >= startDate
    })
    
    const sortedTxs = [...filteredTxs].sort((a: any, b: any) => 
      new Date(a.soldAt || a.createdAt).getTime() - new Date(b.soldAt || b.createdAt).getTime()
    )
    
    const dailyMap = new Map<string, number>()
    let runningBalance = initialAsset.value
    
    sortedTxs.forEach((tx: any) => {
      const dateKey = new Date(tx.soldAt || tx.createdAt).toISOString().split('T')[0]
      if (tx.profitLoss) {
        runningBalance += tx.profitLoss
      }
      dailyMap.set(dateKey, runningBalance)
    })
    
    // ⭐⭐⭐ [변경] 기간 시작일~오늘까지 모든 날짜 채우기 ⭐⭐⭐
    const sortedDates2 = Array.from(dailyMap.keys()).sort()
    if (sortedDates2.length === 0) {
      assetHistory.value = []
      return
    }
    const fillStart = new Date(sortedDates2[0])
    fillStart.setHours(12, 0, 0, 0)
    const fillEnd = new Date()
    fillEnd.setHours(12, 0, 0, 0)
    const fillEndStr = `${fillEnd.getFullYear()}-${String(fillEnd.getMonth() + 1).padStart(2, '0')}-${String(fillEnd.getDate()).padStart(2, '0')}`

    const allDates2: Array<any> = []
    let currentBalance2 = initialAsset.value
    const fillCurrent = new Date(fillStart)

    while (true) {
      const dateKey = `${fillCurrent.getFullYear()}-${String(fillCurrent.getMonth() + 1).padStart(2, '0')}-${String(fillCurrent.getDate()).padStart(2, '0')}`
      if (dailyMap.has(dateKey)) {
        currentBalance2 = dailyMap.get(dateKey)!
      }
      allDates2.push({
        date: dateKey,
        balance: currentBalance2,
        evaluationAmount: currentBalance2,
        depositAmount: initialAsset.value,
        profitAmount: currentBalance2 - initialAsset.value,
        profitRate: ((currentBalance2 - initialAsset.value) / initialAsset.value) * 100
      })
      if (dateKey >= fillEndStr) break
      fillCurrent.setDate(fillCurrent.getDate() + 1)
    }
    assetHistory.value = allDates2
  } catch (error) {
    console.error('기간별 자산 이력 조회 실패:', error)
    assetHistory.value = []
  }
}

const loadCoinProfits = async () => {
  loadingCoin.value = true
  try {
    const response = await profitApi.getByCoin()
    coinProfits.value = response.data?.data || response.data || []
  } catch (error: any) {
    console.error('코인별 수익 조회 실패:', error)
    showSnackbar('코인별 수익 조회 실패', 'error')
  } finally {
    loadingCoin.value = false
  }
}

const openSellDialog = (holding: Transaction) => {
  selectedHolding.value = holding
  sellPrice.value = holding.currentPrice || holding.price
  sellDialog.value = true
}

const confirmSell = async () => {
  if (!selectedHolding.value || sellPrice.value <= 0) {
    showSnackbar('유효한 매도 가격을 입력해주세요', 'error')
    return
  }

  sellLoading.value = true
  try {
    await transactionApi.sell(selectedHolding.value.transactionId, { soldPrice: sellPrice.value })
    showSnackbar('매도가 완료되었습니다', 'success')
    sellDialog.value = false
    loadHoldings()
  } catch (error: any) {
    showSnackbar(error.response?.data?.message || '매도 처리 실패', 'error')
  } finally {
    sellLoading.value = false
  }
}

const viewDetail = (holding: Transaction) => {
  selectedHolding.value = holding
  detailDialog.value = true
}

const openCoinDetailDialog = (coin: CoinProfit) => {
  selectedCoin.value = coin
  coinDetailDialog.value = true
}

const formatCurrency = (value: number) => {
  if (value === undefined || value === null) return '₩0'
  return '₩' + value.toLocaleString('ko-KR', { maximumFractionDigits: 0 })
}

const formatNumber = (value: number, decimals: number) => {
  return value?.toFixed(decimals) || '0'
}

const formatDateTime = (dateString: string) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('ko-KR')
}

const showSnackbar = (message: string, color: string) => {
  snackbarMessage.value = message
  snackbarColor.value = color
  snackbar.value = true
}

// 기간 선택 변경 감지 - 차트도 함께 갱신
watch(selectedPeriod, async (newPeriod) => {
  if (newPeriod !== 'custom') {
    await loadPeriodDetail(newPeriod)
    await loadAssetHistoryByPeriod(newPeriod)
  }
})

// 탭 변경 시 데이터 로드
watch(profitTab, (newTab) => {
  if (newTab === 'coin' && coinProfits.value.length === 0) {
    loadCoinProfits()
  }
})

onMounted(() => {
  loadHoldings()
  loadProfitSummary()
  loadPeriodDetail('total')
})
</script>

<style scoped>
/* 배경색 통일 */
.bg-grey-lighten-3 {
  background-color: #EEEEEE !important;
}

/* ⭐ 수정: 탭 스타일 - 하단 줄/배경 완전 제거 */
.profit-tabs {
  border-bottom: none !important;
  background-color: transparent !important;
  flex-grow: 0 !important;
  width: auto !important;
}

.profit-tabs :deep(.v-tabs__container) {
  flex-grow: 0 !important;
}

/* ⭐ 탭 하단 인디케이터(파란 줄) 제거 */
.profit-tabs :deep(.v-tabs-slider-wrapper),
.profit-tabs :deep(.v-tab__slider) {
  display: none !important;
}

/* ⭐ v-card 내부 탭 영역 배경 투명 처리 */
.profit-tabs :deep(.v-slide-group__content) {
  background-color: transparent !important;
}

.profit-tab {
  min-width: 150px;
  max-width: 150px;
  border: 1px solid #CFD8DC;
  border-bottom: 1px solid #CFD8DC;
  margin-right: 4px;
  border-radius: 8px 8px 0 0;
  background-color: #B0BEC5 !important;
  color: #37474F !important;
  flex-grow: 0 !important;
}

.profit-tab.v-tab--selected {
  background-color: #546E7A !important;
  color: white !important;
  border-color: #546E7A;
}

/* 기간 선택 토글 */
.period-toggle .v-btn {
  min-width: 70px;
}

.period-toggle .v-btn--active {
  background-color: #3949AB !important;
  color: white !important;
}

/* 수익 요약 카드 */
.profit-summary-card {
  border: 2px solid #3949AB;
  background: linear-gradient(135deg, #f5f5f5 0%, #ffffff 100%);
}

/* 하이라이트 카드 */
.highlight-card {
  border: 2px solid #333 !important;
  background-color: #E8F5E9 !important;
}

/* 차트 스타일 */
.chart-container {
  position: relative;
  width: 100%;
  overflow-x: auto;
}

.chart-wrapper-backtest {
  position: relative;
  cursor: crosshair;
  height: 350px;
  min-width: 100%;
}

.chart-wrapper-backtest.scroll-mode {
  overflow-x: auto;
}

.custom-chart {
  width: 100%;
  height: 100%;
}

.chart-point {
  cursor: pointer;
  transition: r 0.15s ease;
}

.chart-labels-backtest {
  position: absolute;
  top: 0;
  right: 0;
  height: 100%;
  pointer-events: none;
  z-index: 5;
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

.label-max { color: #4CAF50; }
.label-initial { color: #FF9800; }
.label-min { color: #F44336; }
.label-deposit { color: #FF9800; }
.label-evaluation { color: #1976D2; }

.label-floor { color: #9E9E9E; }

.chart-tooltip-backtest {
  position: absolute;
  background: rgba(0, 0, 0, 0.9);
  color: white;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 12px;
  pointer-events: none;
  z-index: 100;
  white-space: nowrap;
  box-shadow: 0 4px 12px rgba(0,0,0,0.4);
  /* ⭐⭐⭐ [변경] transform 제거 - 인라인 스타일에서 동적으로 설정 ⭐⭐⭐ */
  /* 왜: 인라인 style에서 좌/우 전환용 transform을 동적 지정하므로 CSS 고정값 제거 */
}

.text-success { color: #4CAF50 !important; }
.text-error { color: #F44336 !important; }
.text-teal { color: #009688; }
.text-teal-darken-2 { color: #00796b; }
.text-red { color: #f44336; }
.text-red-darken-2 { color: #d32f2f; }
.text-grey { color: #9e9e9e; }

/* 코인별 수익 테이블 */
.coin-profit-table {
  border-top: 1px solid #e0e0e0;
}

/* 제목 영역 기간 선택 토글 - 미선택 시 옅은 회색 */
.period-toggle-header {
  background-color: transparent;
  border-radius: 4px;
  height: 32px;
}

.period-toggle-header .v-btn {
  background-color: #B0BEC5 !important;
  color: #37474F !important;
  min-width: 50px;
  font-size: 12px;
  height: 32px !important;
  padding: 0 10px;
  border: none !important;
}

.period-toggle-header .v-btn--active {
  background-color: #FFC107 !important;
  color: #333 !important;
}

/* 날짜 입력 박스 크기 조정 - 높이 통일 */
.custom-date-input {
  width: 130px;
  height: 32px;
  padding: 4px 8px;
  font-size: 13px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 4px;
  background-color: rgba(255, 255, 255, 0.95);
  color: #333;
}

.custom-date-input:focus {
  outline: none;
  border-color: #FFC107;
}

/* 조회 버튼 높이 통일 */
.custom-search-btn {
  height: 32px !important;
  min-width: 50px;
  font-size: 13px;
  font-weight: 600;
}

/* 조회 버튼 높이 통일 */
.custom-search-btn {
  height: 32px !important;
  min-width: 50px;
  font-size: 13px;
  font-weight: 600;
}

/* ⭐ 수정: 카드 상단 둥근 모서리 제거 (탭과 연결) */
.card-no-top-radius {
  border-top-left-radius: 0 !important;
  border-top-right-radius: 0 !important;
}
</style>