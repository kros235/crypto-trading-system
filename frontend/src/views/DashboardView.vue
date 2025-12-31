<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main class="bg-grey-lighten-3">
      <v-container fluid>
        <!-- 페이지 타이틀 -->
        <v-row class="mb-2">
          <v-col cols="12" class="d-flex align-center py-2">
            <h1 class="text-h5">대시보드</h1>
            <v-btn icon variant="text" class="ml-1" @click="refreshAll" :loading="isRefreshing" size="small">
              <v-icon size="20">mdi-refresh</v-icon>
            </v-btn>
            <v-spacer />
            <!-- ★★★ 수정: 실시간 시간 표시 (BOLD체, 검정색) ★★★ -->
            <span class="text-body-1 font-weight-bold text-grey-darken-4">{{ currentTime }}</span>
          </v-col>
        </v-row>

        <!-- ========== 섹션 1: 사용자+통계 + 봇 상태 ========== -->
        <v-row dense>
          <v-col cols="12" md="9">
            <v-card elevation="2" class="fill-height">
              <v-card-text class="pa-3">
                <v-row dense align="center">
                  <!-- ★★★ 수정: 사용자 정보 - 관리자/API 좌우 배치 ★★★ -->
                  <v-col cols="12" sm="4">
                    <div class="d-flex align-center">
                      <v-avatar color="indigo" size="44" class="mr-3">
                        <v-icon color="white" size="24">mdi-account</v-icon>
                      </v-avatar>
                      <div>
                        <!-- ★★★ admin 글자 옆에 관리자/API 등록됨 좌우 배치 ★★★ -->
                        <div class="d-flex align-center">
                          <span class="text-h6 font-weight-bold mr-2">{{ authStore.user?.userId }}</span>
                          <v-chip :color="authStore.user?.role === 'ADMIN' ? 'red-darken-2' : 'indigo'" size="x-small" variant="flat" class="mr-1">
                            {{ authStore.user?.role === 'ADMIN' ? '관리자' : '사용자' }}
                          </v-chip>
                          <v-chip :color="authStore.user?.hasApiKey ? 'teal-darken-1' : 'grey'" size="x-small" variant="flat">
                            {{ authStore.user?.hasApiKey ? '업비트 API 키 등록됨' : 'API 미등록' }}
                          </v-chip>
                        </div>
                        <!-- ★★★ 수정: 디스코드 ID - 로봇 아이콘으로 변경 ★★★ -->
                        <div v-if="authStore.user?.discordUserId" class="text-caption text-grey-darken-1 mt-1">
                          <v-icon size="12" class="mr-1">mdi-robot</v-icon>{{ authStore.user.discordUserId }}
                        </div>
                        <div class="text-caption text-grey-darken-1" :class="{ 'mt-1': !authStore.user?.discordUserId }">
                          <v-icon size="12" class="mr-1">mdi-email-outline</v-icon>{{ authStore.user?.email }}
                        </div>
                        <!-- ★★★ 투자기간 검정 글씨 강조 ★★★ -->
                        <div class="text-caption text-grey-darken-4 font-weight-bold mt-1">
                          <v-icon size="12" class="mr-1">mdi-calendar</v-icon>
                          투자기간: {{ investmentPeriod }}
                        </div>
                      </div>
                    </div>
                  </v-col>

                  <!-- 4개 통계 카드 -->
                  <v-col cols="6" sm="2">
                    <v-card color="teal-darken-1" dark class="pa-2 text-center" elevation="0">
                      <div class="text-caption">총 손익</div>
                      <div class="text-subtitle-1 font-weight-bold">{{ formatCurrency(dashboardStats.totalProfitLoss) }}</div>
                      <div class="text-caption">{{ dashboardStats.totalProfitLossPct >= 0 ? '+' : '' }}{{ dashboardStats.totalProfitLossPct?.toFixed(1) || '0.0' }}%</div>
                    </v-card>
                  </v-col>
                  <v-col cols="6" sm="2">
                    <v-card color="indigo" dark class="pa-2 text-center" elevation="0">
                      <div class="text-caption">총 평가액</div>
                      <div class="text-subtitle-1 font-weight-bold">{{ formatCurrency(dashboardStats.totalEvaluation) }}</div>
                      <div class="text-caption">원금 {{ formatCurrency(dashboardStats.totalInvestment) }}</div>
                    </v-card>
                  </v-col>
                  <v-col cols="6" sm="2">
                    <v-card color="indigo-lighten-1" dark class="pa-2 text-center" elevation="0">
                      <div class="text-caption">오늘 매수</div>
                      <div class="text-subtitle-1 font-weight-bold">{{ dashboardStats.todayBuyCount }}건</div>
                      <div class="text-caption">{{ formatCurrency(dashboardStats.todayBuyAmount) }}</div>
                    </v-card>
                  </v-col>
                  <v-col cols="6" sm="2">
                    <v-card color="amber-darken-2" dark class="pa-2 text-center" elevation="0">
                      <div class="text-caption">오늘 매도</div>
                      <div class="text-subtitle-1 font-weight-bold">{{ dashboardStats.todaySellCount }}건</div>
                      <div class="text-caption">{{ formatCurrency(dashboardStats.todaySellAmount) }}</div>
                    </v-card>
                  </v-col>
                </v-row>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- 봇 상태 카드 -->
          <v-col cols="12" md="3">
            <v-card class="fill-height" elevation="2">
              <v-card-title class="py-2 px-3 bg-indigo-darken-2 text-white d-flex align-center">
                <v-icon class="mr-2" size="20">mdi-robot</v-icon>
                <span class="text-body-2">자동매매</span>
                <v-spacer />
                <!-- ★★★ 수정: 대기중 색상 변경 (매수조건과 동일하게) ★★★ -->
                <v-chip :color="botStatus.isRunning ? 'teal' : 'blue-grey-darken-1'" size="x-small" variant="flat">
                  {{ botStatus.isRunning ? '작동중' : '대기중' }}
                </v-chip>
              </v-card-title>
              <v-card-text class="pa-3">
                <div class="text-body-2 text-grey-darken-3 mb-1"><strong>마지막:</strong> {{ botStatus.lastExecuted || '-' }}</div>
                <div class="text-body-2 text-grey-darken-3"><strong>다음:</strong> {{ botStatus.nextExecution || '-' }}</div>
                <v-chip v-if="botStatus.emergencyStop" color="red" size="x-small" variant="flat" class="mt-1">🚨 긴급정지</v-chip>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- ========== 섹션 2: 업비트 계좌 현황 ========== -->
        <v-row class="mt-3" dense>
          <v-col cols="12">
            <v-card elevation="2">
              <v-card-title class="py-2 px-4 bg-amber-darken-2 text-white d-flex align-center">
                <v-icon class="mr-2" size="20">mdi-bank</v-icon>
                <span class="text-body-1">업비트 실제 잔고</span>
                <v-spacer />
                <v-btn icon size="x-small" variant="text" color="white" @click="fetchUpbitAccount" :loading="loadingAccount">
                  <v-icon size="18">mdi-refresh</v-icon>
                </v-btn>
              </v-card-title>
              <v-card-text class="pa-3">
                <v-row dense>
                  <v-col cols="12" md="4">
                    <v-card variant="outlined" class="pa-3 text-center fill-height">
                      <div class="text-caption text-grey-darken-1 mb-1">KRW 잔고 (매수 대기 자금)</div>
                      <div class="text-h5 font-weight-bold text-amber-darken-3">{{ formatCurrency(upbitAccount.krwBalance) }}</div>
                    </v-card>
                  </v-col>
                  <v-col cols="12" md="4">
                    <v-card variant="outlined" class="pa-3 text-center fill-height">
                      <div class="text-caption text-grey-darken-1 mb-1">코인 평가액</div>
                      <div class="text-h5 font-weight-bold text-indigo-darken-1">{{ formatCurrency(upbitAccount.coinEvaluation) }}</div>
                    </v-card>
                  </v-col>
                  <!-- ★★★ 수정: 총 자산 검정 테두리 추가 ★★★ -->
                  <v-col cols="12" md="4">
                    <v-card variant="outlined" class="pa-3 text-center fill-height total-asset-card">
                      <div class="text-caption text-grey-darken-1 mb-1">총 자산</div>
                      <div class="text-h5 font-weight-bold text-teal-darken-2">{{ formatCurrency(upbitAccount.totalAsset) }}</div>
                    </v-card>
                  </v-col>
                </v-row>
                <div v-if="upbitAccount.holdings.length > 0" class="mt-3">
                  <div class="text-caption text-grey-darken-1 mb-2">보유 코인 ({{ upbitAccount.holdings.length }}종)</div>
                  <v-chip-group>
                    <v-chip
                      v-for="holding in upbitAccount.holdings" :key="holding.currency"
                      :color="holding.profitRate >= 0 ? 'teal' : 'red'" size="small" variant="outlined"
                    >
                      <strong class="mr-1">{{ holding.currency }}</strong>
                      {{ formatCurrency(holding.evaluation) }}
                      <span class="ml-1">({{ holding.profitRate >= 0 ? '+' : '' }}{{ holding.profitRate.toFixed(1) }}%)</span>
                    </v-chip>
                  </v-chip-group>
                </div>
                <div v-else-if="!authStore.user?.hasApiKey" class="text-center py-3">
                  <v-btn size="small" color="amber-darken-2" variant="tonal" @click="$router.push('/profile')">API 키 등록하기</v-btn>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- ========== 섹션 3: 거래설정 + 일일한도 + 매수조건 ========== -->
        <v-row class="mt-3" dense>
          <v-col cols="12" md="4">
            <v-card class="fill-height" elevation="2">
              <v-card-title class="py-2 px-4 bg-indigo-darken-1 text-white">
                <v-icon class="mr-2" size="20">mdi-cog</v-icon>
                <span class="text-body-1">거래 설정</span>
              </v-card-title>
              <v-card-text v-if="tradingSettings" class="pa-3">
                <div class="mb-2">
                  <span class="text-caption text-grey-darken-1">거래 코인</span>
                  <div class="text-body-2 text-grey-darken-4 font-weight-medium">{{ tradingSettings.coinSymbols?.map(s => s.replace('KRW-', '')).join(', ') || '-' }}</div>
                </div>
                <div class="mb-2">
                  <span class="text-caption text-grey-darken-1">매수 조건</span>
                  <div class="text-body-2 text-grey-darken-4 font-weight-medium">MA{{ tradingSettings.basePeriod }} {{ tradingSettings.buyThresholdPct }}% 이하</div>
                </div>
                <div>
                  <span class="text-caption text-grey-darken-1">매도 조건</span>
                  <div class="text-body-2 font-weight-medium">
                    <span class="text-teal-darken-2">익절 +{{ tradingSettings.sellTargetPct }}%</span>
                    <span class="mx-1">/</span>
                    <span class="text-red-darken-2">손절 {{ tradingSettings.stopLossPct }}%</span>
                  </div>
                </div>
                <v-btn size="small" color="indigo" variant="text" class="mt-2 px-0" @click="$router.push('/trading-settings')">설정 변경 →</v-btn>
              </v-card-text>
              <v-card-text v-else class="pa-3 d-flex align-center justify-center" style="min-height: 120px;">
                <!-- ★★★ 수정: 설정 필요 문구 제거, 버튼 강조 및 중앙 배치 ★★★ -->
                <v-btn size="large" color="indigo-darken-1" variant="flat" @click="$router.push('/trading-settings')">
                  <v-icon start>mdi-cog</v-icon>
                  설정하기
                </v-btn>
              </v-card-text>
            </v-card>
          </v-col>

          <v-col cols="12" md="4">
            <v-card class="fill-height" elevation="2">
              <v-card-title class="py-2 px-4 bg-indigo-darken-1 text-white">
                <v-icon class="mr-2" size="20">mdi-chart-donut</v-icon>
                <span class="text-body-1">일일 한도</span>
              </v-card-title>
              <v-card-text class="pa-3">
                <div class="d-flex justify-space-between mb-1">
                  <span class="text-caption text-grey-darken-1">한도</span>
                  <span class="text-body-2 text-grey-darken-4 font-weight-medium">{{ formatCurrency(dailyLimit.totalLimit) }}</span>
                </div>
                <div class="d-flex justify-space-between mb-1">
                  <span class="text-caption text-grey-darken-1">사용</span>
                  <span class="text-body-2 text-grey-darken-4">{{ formatCurrency(dailyLimit.usedAmount) }}</span>
                </div>
                <div class="d-flex justify-space-between mb-2">
                  <span class="text-caption text-grey-darken-1">남은 한도</span>
                  <span class="text-body-2 font-weight-bold text-teal-darken-2">{{ formatCurrency(dailyLimit.remainingAmount) }}</span>
                </div>
                <v-progress-linear
                  :model-value="dailyLimit.usedPercent"
                  :color="dailyLimit.usedPercent > 80 ? 'red' : dailyLimit.usedPercent > 50 ? 'amber-darken-2' : 'teal'"
                  height="18" rounded
                >
                  <span class="text-caption font-weight-bold">{{ dailyLimit.usedPercent.toFixed(0) }}%</span>
                </v-progress-linear>
                <div class="mt-2">
                  <span class="text-caption text-grey-darken-1">종목별 보유</span>
                  <div class="mt-1">
                    <v-chip v-for="(count, symbol) in holdingsPerCoin" :key="symbol" size="x-small" variant="outlined" class="mr-1 mb-1">
                      {{ symbol }}: {{ count }}/{{ tradingSettings?.maxHoldingsPerCoin || 3 }}
                    </v-chip>
                    <span v-if="Object.keys(holdingsPerCoin).length === 0" class="text-caption text-grey-darken-2">없음</span>
                  </div>
                </div>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- ★★★ 수정: 매수 조건 카드 재구성 - 더 알기 쉽게 ★★★ -->
          <v-col cols="12" md="4">
            <v-card class="fill-height" elevation="2">
              <v-card-title class="py-2 px-4 bg-indigo-darken-1 text-white d-flex align-center">
                <v-icon class="mr-2" size="20">mdi-target</v-icon>
                <span class="text-body-1">매수 조건</span>
                <v-spacer />
                <v-btn icon size="x-small" variant="text" color="white" @click="fetchIndicators" :loading="loadingIndicators">
                  <v-icon size="18">mdi-refresh</v-icon>
                </v-btn>
              </v-card-title>
              <v-card-text class="pa-3">
                <!-- ★★★ 롤백: 기존 매수 기준 설명 ★★★ -->
                <div v-if="tradingSettings && coinIndicators.length > 0" class="mb-3 pa-2 bg-indigo-lighten-5 rounded">
                  <div class="text-caption text-indigo-darken-2 font-weight-medium">
                    📌 MA{{ tradingSettings?.basePeriod || 20 }} 대비 {{ tradingSettings?.buyThresholdPct || -6 }}% 이하일 때 매수
                  </div>
                </div>
                <div v-if="coinIndicators.length > 0" class="indicator-list">
                  <div v-for="ind in coinIndicators.slice(0, 4)" :key="ind.symbol" class="py-2 border-b">
                    <!-- ★★★ 수정: 코인명 | 현재가→목표가 | 대기 한줄 표시 ★★★ -->
                    <div class="d-flex align-center justify-space-between mb-1">
                      <span class="text-body-2 text-grey-darken-4 font-weight-bold" style="min-width: 45px;">{{ ind.symbol.replace('KRW-', '') }}</span>
                      <span class="text-caption text-grey-darken-2 flex-grow-1 text-center">{{ formatCompactPrice(ind.currentPrice) }} → <span class="text-indigo-darken-1 font-weight-medium">{{ formatCompactPrice(ind.buyPrice) }}</span></span>
                      <v-chip 
                        :color="ind.canBuy ? 'teal' : 'blue-grey-darken-1'" 
                        size="x-small" 
                        variant="flat"
                        class="font-weight-medium"
                      >
                        {{ ind.canBuy ? '✓ 매수' : '대기' }}
                      </v-chip>
                    </div>
                    <div class="d-flex align-center mb-1">
                      <v-progress-linear
                        :model-value="Math.min(100, Math.max(0, ((ind.dropRate - (tradingSettings?.buyThresholdPct || -6)) / Math.abs(tradingSettings?.buyThresholdPct || -6)) * 100 + 100))"
                        :color="ind.canBuy ? 'teal' : 'indigo-lighten-2'"
                        height="6"
                        rounded
                        class="flex-grow-1"
                      />
                    </div>
                    <div class="d-flex justify-space-between text-caption">
                      <span class="text-grey-darken-2">현재 이격도</span>
                      <span :class="ind.canBuy ? 'text-teal-darken-2 font-weight-bold' : 'text-grey-darken-3'">
                        {{ ind.dropRate?.toFixed(2) }}%
                        <span v-if="!ind.canBuy" class="text-blue-grey-darken-1">({{ Math.abs(ind.remainingDrop)?.toFixed(1) }}% 더 하락 필요)</span>
                      </span>
                    </div>
                  </div>
                  <v-btn v-if="coinIndicators.length > 4" size="small" variant="text" color="indigo" class="mt-1 px-0" @click="showAllIndicators = true">
                    +{{ coinIndicators.length - 4 }}개 더보기
                  </v-btn>
                </div>
                <div v-else class="text-center text-grey-darken-2 py-2">
                  <div class="text-body-2">거래 설정에서 코인을 선택하세요</div>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- ========== 섹션 4: 수익 현황 ========== -->
        <v-row class="mt-3" dense>
          <v-col cols="12">
            <v-card elevation="2">
              <v-card-title class="py-2 px-4 bg-teal-darken-2 text-white">
                <v-icon class="mr-2" size="20">mdi-cash-multiple</v-icon>
                <span class="text-body-1">수익 현황</span>
              </v-card-title>
              <v-card-text class="pa-3">
                <v-row dense>
                  <v-col cols="12" md="4">
                    <v-card variant="outlined" class="pa-3 text-center" height="110">
                      <div class="text-caption text-grey-darken-2">📈 평가 수익 (미실현)</div>
                      <div :class="profitSummary.unrealizedProfit >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2'" class="text-h5 font-weight-bold mt-2">
                        {{ profitSummary.unrealizedProfit >= 0 ? '+' : '' }}{{ formatCurrency(profitSummary.unrealizedProfit) }}
                      </div>
                      <div class="text-caption mt-1" :class="profitSummary.unrealizedProfitPct >= 0 ? 'text-teal-darken-1' : 'text-red-darken-1'">
                        ({{ profitSummary.unrealizedProfitPct >= 0 ? '+' : '' }}{{ profitSummary.unrealizedProfitPct.toFixed(2) }}%)
                      </div>
                    </v-card>
                  </v-col>
                  <v-col cols="12" md="4">
                    <v-card variant="outlined" class="pa-3 text-center" height="110">
                      <div class="text-caption text-grey-darken-2">✅ 실현 수익 (확정)</div>
                      <div :class="profitSummary.realizedProfit >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2'" class="text-h5 font-weight-bold mt-2">
                        {{ profitSummary.realizedProfit >= 0 ? '+' : '' }}{{ formatCurrency(profitSummary.realizedProfit) }}
                      </div>
                      <div class="text-caption mt-1 text-grey-darken-1">&nbsp;</div>
                    </v-card>
                  </v-col>
                  <v-col cols="12" md="4">
                    <v-card variant="outlined" class="pa-3 text-center total-profit-card" height="110">
                      <div class="text-caption text-teal-darken-2 font-weight-medium">💰 누적 총 수익</div>
                      <div :class="profitSummary.totalProfit >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2'" class="text-h5 font-weight-bold mt-2">
                        {{ profitSummary.totalProfit >= 0 ? '+' : '' }}{{ formatCurrency(profitSummary.totalProfit) }}
                      </div>
                      <div class="text-caption mt-1 text-grey-darken-1">&nbsp;</div>
                    </v-card>
                  </v-col>
                </v-row>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- ========== 섹션 5: 자산 변동 추이 ========== -->
        <v-row class="mt-3" dense>
          <v-col cols="12">
            <v-card elevation="2">
              <v-card-title class="py-2 px-4 bg-indigo-darken-2 text-white d-flex align-center">
                <v-icon class="mr-2" size="20">mdi-chart-line</v-icon>
                <span class="text-body-1">자산 변동 추이</span>
                <v-spacer />
                <!-- ★★★ 수정: 기간 버튼 대비 강화 + 사용자 지정 기간 추가 ★★★ -->
                <v-btn-toggle v-model="chartPeriod" density="compact" mandatory variant="outlined" size="small" class="chart-period-toggle">
                  <v-btn value="7" size="x-small" :class="chartPeriod === '7' ? 'active-period' : ''">7일</v-btn>
                  <v-btn value="month" size="x-small" :class="chartPeriod === 'month' ? 'active-period' : ''">이번달</v-btn>
                  <v-btn value="year" size="x-small" :class="chartPeriod === 'year' ? 'active-period' : ''">올해</v-btn>
                  <v-btn value="all" size="x-small" :class="chartPeriod === 'all' ? 'active-period' : ''">전체</v-btn>
                </v-btn-toggle>
                <v-btn size="x-small" variant="text" color="white" class="ml-2" @click="showCustomDateDialog = true">
                  <v-icon size="16">mdi-calendar-range</v-icon>
                </v-btn>
              </v-card-title>
              <v-card-text class="pa-3">
                <div v-if="assetHistory.length > 0" class="chart-container">
                  <div class="chart-wrapper" @mousemove="handleChartHover" @mouseleave="hoveredIndex = -1">
                    <svg class="custom-chart" :viewBox="`0 0 ${svgWidth} ${svgHeight}`" preserveAspectRatio="none">
                      <defs>
                        <linearGradient id="dashboardAreaGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                          <stop offset="0%" style="stop-color:#5C6BC0;stop-opacity:0.5" />
                          <stop offset="100%" style="stop-color:#5C6BC0;stop-opacity:0.1" />
                        </linearGradient>
                      </defs>
                      <path :d="areaPath" fill="url(#dashboardAreaGradient)" />
                      <line :x1="svgPadding" :y1="getYPosition(initialAsset)" :x2="svgWidth - svgPadding" :y2="getYPosition(initialAsset)" stroke="#FFA726" stroke-width="2" stroke-dasharray="5,3" />
                      <path :d="linePath" fill="none" stroke="#3F51B5" stroke-width="2.5" />
                      <circle v-for="(point, index) in chartPoints" :key="index" :cx="point.x" :cy="point.y" :r="hoveredIndex === index ? 7 : 4" :fill="getPointColor(point.balance)" stroke="white" stroke-width="2" class="chart-point" />
                    </svg>
                    <div v-if="hoveredIndex >= 0 && hoveredData" class="chart-tooltip" :style="{ left: tooltipX + 'px' }">
                      <div class="font-weight-bold">{{ hoveredData.date }}</div>
                      <div>{{ formatCurrency(hoveredData.balance) }}</div>
                      <div :class="hoveredData.profitRate >= 0 ? 'text-teal' : 'text-red'">{{ hoveredData.profitRate >= 0 ? '+' : '' }}{{ hoveredData.profitRate.toFixed(2) }}%</div>
                    </div>
                  </div>
                </div>
                <div v-else class="text-center py-6 text-grey-darken-2">
                  <v-icon size="48" class="mb-2" color="grey">mdi-chart-line-variant</v-icon>
                  <div class="text-body-1">거래 내역이 없습니다</div>
                  <div class="text-caption">자동매매가 시작되면 차트가 표시됩니다</div>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- ========== 섹션 6: 코인별 성과 + 최근 거래 + 시스템 알림 ========== -->
        <v-row class="mt-3 mb-4" dense>
          <v-col cols="12" md="4">
            <v-card class="fill-height" elevation="2">
              <v-card-title class="py-2 px-4 bg-indigo-darken-1 text-white">
                <v-icon class="mr-2" size="20">mdi-podium</v-icon>
                <span class="text-body-1">코인별 성과</span>
              </v-card-title>
              <v-card-text class="pa-2">
                <v-list v-if="coinPerformance.length > 0" density="compact">
                  <v-list-item v-for="perf in coinPerformance.slice(0, 5)" :key="perf.symbol" class="px-2 py-1">
                    <div class="d-flex align-center justify-space-between w-100">
                      <div>
                        <span class="text-body-2 font-weight-medium text-grey-darken-4">{{ perf.symbol }}</span>
                        <div class="text-caption text-grey-darken-1">{{ perf.tradeCount }}회 거래</div>
                      </div>
                      <div class="text-end">
                        <div :class="perf.profit >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2'" class="text-body-2 font-weight-medium">
                          {{ perf.profit >= 0 ? '+' : '' }}{{ formatCurrency(perf.profit) }}
                        </div>
                        <v-chip :color="perf.profitRate >= 0 ? 'teal' : 'red'" size="x-small" variant="flat">
                          {{ perf.profitRate >= 0 ? '+' : '' }}{{ perf.profitRate.toFixed(1) }}%
                        </v-chip>
                      </div>
                    </div>
                  </v-list-item>
                </v-list>
                <div v-else class="text-center py-4 text-grey-darken-2 text-body-2">매도 완료된 거래가 없습니다</div>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- ★★★ 수정: 최근 거래 가로 배치 ★★★ -->
          <v-col cols="12" md="4">
            <v-card class="fill-height" elevation="2">
              <v-card-title class="py-2 px-4 bg-indigo-darken-1 text-white d-flex align-center">
                <v-icon class="mr-2" size="20">mdi-history</v-icon>
                <span class="text-body-1">최근 거래</span>
                <v-spacer />
                <v-btn size="x-small" variant="text" color="white" @click="$router.push('/transactions')">전체 →</v-btn>
              </v-card-title>
              <v-card-text class="pa-2">
                <v-list v-if="recentTransactions.length > 0" density="compact">
                  <v-list-item v-for="tx in recentTransactions.slice(0, 5)" :key="tx.id" class="px-2 py-1">
                    <div class="d-flex align-center justify-space-between w-100">
                      <!-- 코인명 -->
                      <div class="d-flex align-center" style="min-width: 70px;">
                        <v-avatar :color="tx.type === 'BUY' ? 'indigo' : 'amber-darken-2'" size="24" class="mr-1">
                          <v-icon size="14" color="white">{{ tx.type === 'BUY' ? 'mdi-arrow-down' : 'mdi-arrow-up' }}</v-icon>
                        </v-avatar>
                        <span class="text-body-2 font-weight-medium text-grey-darken-4">{{ tx.coinSymbol?.replace('KRW-', '') }}</span>
                      </div>
                      <!-- 가격 및 날짜 (가로 배치) -->
                      <div class="text-caption text-grey-darken-2 flex-grow-1 px-2 text-center">
                        {{ formatCurrency(tx.totalAmount) }} · {{ formatDate(tx.createdAt) }}
                      </div>
                      <!-- 매수/매도 -->
                      <v-chip :color="tx.type === 'BUY' ? 'indigo' : 'amber-darken-2'" size="x-small" variant="flat">
                        {{ tx.type === 'BUY' ? '매수' : '매도' }}
                      </v-chip>
                    </div>
                  </v-list-item>
                </v-list>
                <div v-else class="text-center py-4 text-grey-darken-2 text-body-2">거래 내역이 없습니다</div>
              </v-card-text>
            </v-card>
          </v-col>

          <v-col cols="12" md="4">
            <v-card class="fill-height" elevation="2">
              <!-- ★★★ 수정: 시스템 알림 빨간색 배경 ★★★ -->
              <v-card-title class="py-2 px-4 bg-red-darken-1 text-white">
                <v-icon class="mr-2" size="20">mdi-bell</v-icon>
                <span class="text-body-1">시스템 알림</span>
              </v-card-title>
              <v-card-text class="pa-3">
                <!-- ★★★ 수정: 알림 텍스트 검정색 강제 적용 ★★★ -->
                <v-alert v-for="(alert, index) in systemAlerts" :key="index" :type="alert.type" variant="tonal" density="compact" class="mb-2" style="color: #424242 !important;">
                  <span class="font-weight-medium">{{ alert.message }}</span>
                </v-alert>
                <div v-if="systemAlerts.length === 0" class="text-center py-4 text-grey-darken-2">
                  <v-icon size="28" class="mb-1">mdi-check-circle-outline</v-icon>
                  <div class="text-body-2">알림 없음</div>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- 매수조건 전체 보기 다이얼로그 -->
        <v-dialog v-model="showAllIndicators" max-width="550">
          <v-card>
            <v-card-title class="bg-indigo-darken-2 text-white py-2">
              <v-icon class="mr-2" size="20">mdi-target</v-icon>매수 조건 전체
            </v-card-title>
            <v-card-text class="pa-0">
              <v-list density="compact">
                <v-list-item v-for="ind in coinIndicators" :key="ind.symbol" class="py-2">
                  <div class="w-100">
                    <!-- ★★★ 수정: 코인명 | 현재가→목표가 | 대기 한줄 표시 ★★★ -->
                    <div class="d-flex align-center justify-space-between mb-1">
                      <span class="font-weight-bold text-grey-darken-4" style="min-width: 80px;">{{ ind.symbol.replace('KRW-', '') }}</span>
                      <span class="text-caption text-grey-darken-2 flex-grow-1 text-center">{{ formatCompactPrice(ind.currentPrice) }} → <span class="text-indigo-darken-1 font-weight-medium">{{ formatCompactPrice(ind.buyPrice) }}</span></span>
                      <v-chip :color="ind.canBuy ? 'teal' : 'blue-grey-darken-1'" size="small" variant="flat">
                        {{ ind.canBuy ? '✓ 매수' : '대기' }}
                      </v-chip>
                    </div>
                    <div class="d-flex justify-space-between text-caption text-grey-darken-2">
                      <span>MA{{ tradingSettings?.basePeriod || 20 }}: {{ formatCurrency(ind.ma20) }}</span>
                      <span>이격도: {{ ind.dropRate?.toFixed(2) }}%</span>
                    </div>
                  </div>
                </v-list-item>
              </v-list>
            </v-card-text>
            <v-card-actions><v-spacer /><v-btn variant="text" @click="showAllIndicators = false">닫기</v-btn></v-card-actions>
          </v-card>
        </v-dialog>

        <!-- ★★★ 신규: 사용자 지정 기간 다이얼로그 ★★★ -->
        <v-dialog v-model="showCustomDateDialog" max-width="400">
          <v-card>
            <v-card-title class="bg-indigo-darken-2 text-white py-2">
              <v-icon class="mr-2" size="20">mdi-calendar-range</v-icon>기간 선택
            </v-card-title>
            <v-card-text class="pa-4">
              <v-row>
                <v-col cols="6">
                  <v-text-field
                    v-model="customStartDate"
                    label="시작일"
                    type="date"
                    density="compact"
                    variant="outlined"
                  />
                </v-col>
                <v-col cols="6">
                  <v-text-field
                    v-model="customEndDate"
                    label="종료일"
                    type="date"
                    density="compact"
                    variant="outlined"
                  />
                </v-col>
              </v-row>
            </v-card-text>
            <v-card-actions>
              <v-spacer />
              <v-btn variant="text" @click="showCustomDateDialog = false">취소</v-btn>
              <v-btn color="indigo" variant="flat" @click="applyCustomDate">적용</v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>

        <v-snackbar v-model="snackbar.show" :color="snackbar.color" :timeout="3000">{{ snackbar.message }}</v-snackbar>
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'
import { useAuthStore } from '@/stores/auth'
import { coinApi, transactionApi, tradingApi, botApi } from '@/api'

const authStore = useAuthStore()
const sidebarRef = ref()

const loadingAccount = ref(false)
const loadingIndicators = ref(false)
const isRefreshing = ref(false)
const showAllIndicators = ref(false)

// ★★★ 신규: 실시간 시간 표시 ★★★
const currentTime = ref('')
let timeInterval: number | null = null

const updateCurrentTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

// ★★★ 신규: 사용자 지정 기간 ★★★
const showCustomDateDialog = ref(false)
const customStartDate = ref('')
const customEndDate = ref('')

const applyCustomDate = () => {
  chartPeriod.value = 'custom'
  showCustomDateDialog.value = false
  // TODO: customStartDate, customEndDate로 차트 데이터 필터링
  showSnackbar(`${customStartDate.value} ~ ${customEndDate.value} 기간 적용`, 'success')
}

// 투자기간 계산
const investmentPeriod = ref('0일')

const dashboardStats = ref({ totalProfitLoss: 0, totalProfitLossPct: 0, totalEvaluation: 0, totalInvestment: 0, todayBuyCount: 0, todayBuyAmount: 0, todaySellCount: 0, todaySellAmount: 0 })
const upbitAccount = ref({ krwBalance: 0, coinEvaluation: 0, totalAsset: 0, holdings: [] as any[] })
const botStatus = ref({ isRunning: false, lastExecuted: '', nextExecution: '', apiConnected: false, emergencyStop: false })
const tradingSettings = ref<any>(null)
const dailyLimit = ref({ totalLimit: 0, usedAmount: 0, remainingAmount: 0, usedPercent: 0 })
const holdingsPerCoin = ref<Record<string, number>>({})
const coinIndicators = ref<any[]>([])
const profitSummary = ref({ unrealizedProfit: 0, unrealizedProfitPct: 0, realizedProfit: 0, totalProfit: 0 })
const systemAlerts = ref<any[]>([])
const chartPeriod = ref('month')
const assetHistory = ref<any[]>([])
const hoveredIndex = ref(-1)
const tooltipX = ref(0)
const initialAsset = ref(0)
const coinPerformance = ref<any[]>([])
const recentTransactions = ref<any[]>([])
const snackbar = ref({ show: false, message: '', color: 'success' })

const svgWidth = 800, svgHeight = 200, svgPadding = 30

const maxBalance = computed(() => assetHistory.value.length ? Math.max(...assetHistory.value.map(d => d.balance)) : 0)
const minBalance = computed(() => assetHistory.value.length ? Math.min(...assetHistory.value.map(d => d.balance)) : 0)
const chartPoints = computed(() => {
  if (!assetHistory.value.length) return []
  const total = assetHistory.value.length
  return assetHistory.value.map((d, index) => ({ x: getXPosition(index, total), y: getYPosition(d.balance), balance: d.balance }))
})
const linePath = computed(() => chartPoints.value.length ? chartPoints.value.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ') : '')
const areaPath = computed(() => {
  if (!chartPoints.value.length) return ''
  const points = chartPoints.value
  return `M ${points[0].x} ${svgHeight - svgPadding} L ${points.map(p => `${p.x} ${p.y}`).join(' L ')} L ${points[points.length - 1].x} ${svgHeight - svgPadding} Z`
})
const hoveredData = computed(() => hoveredIndex.value >= 0 ? assetHistory.value[hoveredIndex.value] : null)

const formatCurrency = (value: number) => {
  if (value === undefined || value === null) return '₩0'
  return '₩' + value.toLocaleString('ko-KR', { maximumFractionDigits: 0 })
}
// ★★★ 신규: 간결한 가격 포맷 함수 ★★★
const formatCompactPrice = (value: number) => {
  if (value === undefined || value === null) return '-'
  if (value >= 100000000) return (value / 100000000).toFixed(2) + '억'
  if (value >= 10000000) return (value / 10000).toFixed(0) + '만'
  if (value >= 1000000) return (value / 10000).toFixed(0) + '만'
  if (value >= 10000) return (value / 10000).toFixed(1) + '만'
  if (value >= 1000) return value.toLocaleString('ko-KR', { maximumFractionDigits: 0 })
  if (value >= 1) return value.toFixed(0)
  return value.toFixed(4)
}
const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getMonth()+1}/${d.getDate()} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}
const getYPosition = (balance: number) => {
  const range = maxBalance.value - minBalance.value || 1
  return svgPadding + ((maxBalance.value - balance) / range) * (svgHeight - svgPadding * 2)
}
const getXPosition = (index: number, total: number) => total <= 1 ? svgWidth / 2 : svgPadding + (index / (total - 1)) * (svgWidth - svgPadding * 2)
const getPointColor = (balance: number) => {
  if (balance > initialAsset.value * 1.01) return '#26A69A'
  if (balance < initialAsset.value * 0.99) return '#EF5350'
  return '#5C6BC0'
}
const handleChartHover = (event: MouseEvent) => {
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
  const x = event.clientX - rect.left
  const total = assetHistory.value.length
  if (total === 0) return
  const chartWidth = svgWidth - svgPadding * 2
  const index = Math.round(((x - svgPadding) / chartWidth) * (total - 1))
  hoveredIndex.value = Math.max(0, Math.min(total - 1, index))
  tooltipX.value = x
}
const showSnackbar = (message: string, color: string) => { snackbar.value = { show: true, message, color } }

// 투자기간 계산 함수
const calculateInvestmentPeriod = (firstTradeDate: string | null) => {
  if (!firstTradeDate) {
    investmentPeriod.value = '0일'
    return
  }
  const first = new Date(firstTradeDate)
  const now = new Date()
  const diffTime = Math.abs(now.getTime() - first.getTime())
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  
  if (diffDays < 30) {
    investmentPeriod.value = `${diffDays}일`
  } else if (diffDays < 365) {
    const months = Math.floor(diffDays / 30)
    const days = diffDays % 30
    investmentPeriod.value = days > 0 ? `${months}개월 ${days}일` : `${months}개월`
  } else {
    const years = Math.floor(diffDays / 365)
    const months = Math.floor((diffDays % 365) / 30)
    investmentPeriod.value = months > 0 ? `${years}년 ${months}개월` : `${years}년`
  }
}

const fetchDashboardStats = async () => { try { const r = await transactionApi.getDashboardStats(); dashboardStats.value = r.data } catch (e) { console.error(e) } }
const fetchUpbitAccount = async () => {
  if (!authStore.user?.hasApiKey) return
  loadingAccount.value = true
  try {
    const r = await coinApi.getAccounts()
    const accounts = r.data || []
    let krwBalance = 0, coinEvaluation = 0
    const holdings: any[] = []
    for (const acc of accounts) {
      const balance = parseFloat(acc.balance) || 0
      const avgBuyPrice = parseFloat(acc.avg_buy_price || acc.avgBuyPrice) || 0
      if (acc.currency === 'KRW') { krwBalance = balance }
      else if (balance > 0) {
        try {
          const pr = await coinApi.getCoinPrice(`KRW-${acc.currency}`)
          const cp = pr.data?.tradePrice || pr.data?.trade_price || avgBuyPrice
          const ev = balance * cp
          const profitRate = avgBuyPrice > 0 ? ((cp - avgBuyPrice) / avgBuyPrice) * 100 : 0
          coinEvaluation += ev
          holdings.push({ currency: acc.currency, balance, avgBuyPrice, currentPrice: cp, evaluation: ev, profitRate })
        } catch { coinEvaluation += balance * avgBuyPrice; holdings.push({ currency: acc.currency, balance, avgBuyPrice, currentPrice: avgBuyPrice, evaluation: balance * avgBuyPrice, profitRate: 0 }) }
      }
    }
    upbitAccount.value = { krwBalance, coinEvaluation, totalAsset: krwBalance + coinEvaluation, holdings }
  } catch (e) { console.error(e) }
  finally { loadingAccount.value = false }
}
const fetchBotStatus = async () => { try { const r = await botApi.getStatus(); botStatus.value = { isRunning: r.data?.isRunning || false, lastExecuted: r.data?.lastExecutedAt || '', nextExecution: r.data?.nextExecutionAt || '', apiConnected: r.data?.apiConnected ?? true, emergencyStop: r.data?.emergencyStop || false } } catch (e) { console.error(e); botStatus.value.apiConnected = false } }
const fetchTradingSettings = async () => { try { const r = await tradingApi.getSettings(); tradingSettings.value = r.data; if (tradingSettings.value) { const tl = tradingSettings.value.dailyLimitAmount || 0; const ua = dashboardStats.value.todayBuyAmount || 0; dailyLimit.value = { totalLimit: tl, usedAmount: ua, remainingAmount: Math.max(0, tl - ua), usedPercent: tl > 0 ? (ua / tl) * 100 : 0 } } } catch (e) { console.error(e); tradingSettings.value = null } }
const fetchIndicators = async () => {
  if (!tradingSettings.value?.coinSymbols?.length) return
  loadingIndicators.value = true
  try {
    const r = await botApi.getIndicators(tradingSettings.value.coinSymbols)
    const indicators = r.data || []
    const bt = tradingSettings.value.buyThresholdPct || -3
    // ★★★ 수정: 이격도 프론트엔드에서 직접 계산 (백엔드 maDropRate 미제공 문제 해결) ★★★
    coinIndicators.value = indicators.map((ind: any) => {
      const cp = ind.currentPrice || 0
      const ma = ind.ma20 || 0
      // 이격도 = (현재가 - MA20) / MA20 * 100
      const dr = ma > 0 ? ((cp - ma) / ma) * 100 : 0
      return {
        symbol: ind.market || ind.symbol,
        currentPrice: cp,
        ma20: ma,
        buyPrice: ma * (1 + bt / 100),
        dropRate: dr,
        canBuy: dr <= bt,
        remainingDrop: bt - dr
      }
    })
  } catch (e) { console.error(e) }
  finally { loadingIndicators.value = false }
}
const fetchHoldings = async () => { try { const r = await transactionApi.getHoldings(); const h = r.data || []; const pc: Record<string, number> = {}; let up = 0, ti = 0; h.forEach((hh: any) => { const s = hh.coinSymbol?.replace('KRW-', '') || 'X'; pc[s] = (pc[s] || 0) + 1; up += hh.profitLoss || 0; ti += hh.totalAmount || 0 }); holdingsPerCoin.value = pc; profitSummary.value.unrealizedProfit = up; profitSummary.value.unrealizedProfitPct = ti > 0 ? (up / ti) * 100 : 0 } catch (e) { console.error(e) } }
const fetchRecentTransactions = async () => {
  try {
    const r = await transactionApi.getAll({ page: 0, size: 10 }); 
    recentTransactions.value = r.data?.content || []
    
    // 투자기간 계산을 위한 첫 거래일 확인
    if (recentTransactions.value.length > 0) {
      const allTx = await transactionApi.getAll({ page: 0, size: 1000 })
      const sorted = (allTx.data?.content || []).sort((a: any, b: any) => 
        new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
      )
      if (sorted.length > 0) {
        calculateInvestmentPeriod(sorted[0].createdAt)
      }
    }
    
    const ar = await transactionApi.search({ status: 'SOLD' }); const st = ar.data?.content || []
    profitSummary.value.realizedProfit = st.reduce((s: number, t: any) => s + (t.profitLoss || 0), 0)
    profitSummary.value.totalProfit = profitSummary.value.unrealizedProfit + profitSummary.value.realizedProfit
    const pm = new Map<string, any>()
    st.forEach((t: any) => { const s = t.coinSymbol?.replace('KRW-', '') || 'X'; if (!pm.has(s)) pm.set(s, { symbol: s, tradeCount: 0, profit: 0, totalAmount: 0 }); const p = pm.get(s); p.tradeCount++; p.profit += t.profitLoss || 0; p.totalAmount += t.totalAmount || 0 })
    coinPerformance.value = Array.from(pm.values()).map(p => ({ ...p, profitRate: p.totalAmount > 0 ? (p.profit / p.totalAmount) * 100 : 0 }))
  } catch (e) { console.error(e) }
}
const fetchAssetHistory = async () => { assetHistory.value = []; initialAsset.value = tradingSettings.value?.dailyLimitAmount || 1000000 }
const generateSystemAlerts = () => { const a: any[] = []; if (!authStore.user?.hasApiKey) a.push({ type: 'warning', message: 'API 키가 미등록 상태입니다' }); if (!tradingSettings.value) a.push({ type: 'warning', message: '거래 설정을 완료해주세요' }); const n = new Date(); if (n.getHours() === 9 && n.getMinutes() < 10) a.push({ type: 'info', message: '업비트 점검 시간 (09:00~09:10)' }); if (botStatus.value.emergencyStop) a.push({ type: 'error', message: '긴급 정지 발동됨' }); systemAlerts.value = a }
const refreshAll = async () => { isRefreshing.value = true; try { await Promise.all([fetchDashboardStats(), fetchUpbitAccount(), fetchBotStatus(), fetchTradingSettings()]); await Promise.all([fetchIndicators(), fetchHoldings(), fetchRecentTransactions()]); generateSystemAlerts(); showSnackbar('새로고침 완료', 'success') } finally { isRefreshing.value = false } }

let refreshInterval: number | null = null
const startAutoRefresh = () => { refreshInterval = window.setInterval(() => { fetchDashboardStats(); fetchUpbitAccount(); fetchBotStatus() }, 60000) }
const stopAutoRefresh = () => { if (refreshInterval) { clearInterval(refreshInterval); refreshInterval = null } }

onMounted(async () => { 
  // 실시간 시간 업데이트 시작
  updateCurrentTime()
  timeInterval = window.setInterval(updateCurrentTime, 1000)
  
  await Promise.all([fetchDashboardStats(), fetchUpbitAccount(), fetchBotStatus(), fetchTradingSettings()]); 
  await Promise.all([fetchIndicators(), fetchHoldings(), fetchRecentTransactions(), fetchAssetHistory()]); 
  generateSystemAlerts(); 
  startAutoRefresh() 
})
onUnmounted(() => { 
  stopAutoRefresh()
  if (timeInterval) { clearInterval(timeInterval); timeInterval = null }
})
</script>

<style scoped>
.fill-height { height: 100%; }
.w-100 { width: 100%; }
.border-b { border-bottom: 1px solid rgba(0,0,0,0.08); }
.chart-container { position: relative; width: 100%; }
.chart-wrapper { position: relative; width: 100%; height: 200px; }
.custom-chart { width: 100%; height: 100%; }
.chart-point { cursor: pointer; transition: r 0.15s ease; }
.chart-tooltip { position: absolute; top: 8px; transform: translateX(-50%); background: rgba(38,50,56,0.95); color: white; padding: 8px 12px; border-radius: 6px; font-size: 12px; pointer-events: none; z-index: 10; white-space: nowrap; }
.indicator-list { max-height: 220px; overflow-y: auto; }

/* ★★★ 수정: 누적 총 수익 카드 검정 테두리 ★★★ */
.total-profit-card {
  border: 2px solid #333 !important;
  background-color: #E8F5E9 !important;
}

/* ★★★ 신규: 총 자산 카드 검정 테두리 ★★★ */
.total-asset-card {
  border: 2px solid #333 !important;
  background-color: #E8F5E9 !important;
}

/* ★★★ 수정: 차트 기간 버튼 대비 강화 ★★★ */
.chart-period-toggle .v-btn {
  color: rgba(255,255,255,0.7) !important;
  border-color: rgba(255,255,255,0.5) !important;
}
.chart-period-toggle .active-period {
  background-color: white !important;
  color: #3949AB !important;
  font-weight: bold !important;
}
</style>