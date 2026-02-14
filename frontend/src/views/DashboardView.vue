<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main class="bg-grey-lighten-3">
      <v-container fluid>
        <!-- 시작 가이드 위젯-->
        <OnboardingGuide 
          :has-api-key="hasApiKey"
          :has-settings="hasSettings"
          :has-transactions="hasTransactions"
        />
        
<!-- 페이지 타이틀 -->
        <v-row class="mb-2">
          <v-col cols="12" class="py-2">
            <div class="d-flex align-center">
              <h1 class="text-h4">
                <v-icon class="mr-2">mdi-view-dashboard</v-icon>
                대시보드
              </h1>
              <v-btn icon variant="text" class="ml-1" @click="refreshAll" :loading="isRefreshing" size="small">
                <v-icon size="20">mdi-refresh</v-icon>
              </v-btn>
              <v-spacer />
              <!-- 실시간 시간 표시 (BOLD체, 검정색)  -->
              <span class="text-body-1 font-weight-bold text-grey-darken-4">{{ currentTime }}</span>
            </div>
            <p class="text-subtitle-1 text-grey mt-1">투자 현황과 자동매매 상태를 한눈에 확인하세요</p>
          </v-col>
        </v-row>

        <!-- ========== 섹션 1: 사용자+통계 + 봇 상태 ========== -->
        <v-row dense>
          <v-col cols="12" md="9">
            <v-card elevation="2" class="fill-height">
              <v-card-text class="pa-3">
                <!--  1행: 사용자 정보 + 4개 통계 카드  -->
                <v-row dense align="center">
                  <v-col cols="12" sm="4">
                    <div class="d-flex align-center">
                      <v-avatar color="indigo" size="44" class="mr-3">
                        <v-icon color="white" size="24">mdi-account</v-icon>
                      </v-avatar>
                      <div>
                        <div class="d-flex align-center">
                          <span class="text-h6 font-weight-bold mr-2">{{ authStore.user?.userId }}</span>
                          <v-chip :color="authStore.user?.role === 'ADMIN' ? 'red-darken-2' : 'indigo'" size="x-small" variant="flat" class="mr-1">
                            {{ authStore.user?.role === 'ADMIN' ? '관리자' : '사용자' }}
                          </v-chip>
                          <v-chip :color="authStore.user?.hasApiKey ? 'teal-darken-1' : 'grey'" size="x-small" variant="flat">
                            {{ authStore.user?.hasApiKey ? '업비트 API 키 등록됨' : 'API 미등록' }}
                          </v-chip>
                        </div>
                        <div v-if="authStore.user?.discordUserId" class="text-caption text-grey-darken-1 mt-1">
                          <v-icon size="12" class="mr-1">mdi-robot</v-icon>{{ authStore.user.discordUserId }}
                        </div>
                        <div class="text-caption text-grey-darken-1" :class="{ 'mt-1': !authStore.user?.discordUserId }">
                          <v-icon size="12" class="mr-1">mdi-email-outline</v-icon>{{ authStore.user?.email }}
                        </div>
                        <!-- 수정: 마지막 로그인 2줄 표시 -->
                        <div class="text-caption text-grey-darken-4 font-weight-bold mt-1">
                          <v-icon size="12" class="mr-1">mdi-clock-outline</v-icon>
                          마지막 로그인:
                          <div class="ml-4">{{ formatLastLogin(authStore.user?.lastLogin) }}</div>
                        </div>
                      </div>
                    </div>
                  </v-col>

                  <!-- 4개 통계 카드 -->
                  <v-col cols="6" sm="2">
                    <v-card color="teal-darken-1" dark class="pa-2 text-center" elevation="0">
                      <div class="text-caption">총 평가손익</div>
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
                <!-- 2행: 투자기간(좌) + 시스템 상태(우) - 마지막 로그인과 같은 높이 -->
                <v-row dense>
                  <v-col cols="12" sm="4">
                    <!-- 빈 공간 -->
                  </v-col>
                  <v-col cols="12" sm="8" class="d-flex justify-space-between align-center" style="margin-top: -20px;">
                    <div class="text-caption text-grey-darken-4 font-weight-bold">
                      <v-icon size="12" class="mr-1">mdi-calendar</v-icon>
                      투자기간: {{ investmentPeriod }}
                    </div>
                    <div class="text-caption text-grey-darken-4 font-weight-bold">
                      <v-icon size="12" class="mr-1">mdi-server</v-icon>
                      시스템 상태: <span class="text-teal-darken-2">정상</span>
                    </div>
                  </v-col>
                </v-row>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- 봇 상태 카드 -->
          <v-col cols="12" md="3">
            <v-card class="fill-height" elevation="2">
              <!-- ⭐⭐⭐ [수정] 봇 비활성화 시 회색 배경으로 변경 ⭐⭐⭐ -->
              <v-card-title class="py-2 px-3 text-white d-flex align-center" :class="botEnabled ? 'bg-indigo-darken-2' : 'bg-grey-darken-1'">
                <!-- ⭐⭐⭐ [수정] 봇 비활성화 시 아이콘 변경 ⭐⭐⭐ -->
                <v-icon class="mr-2" size="20">{{ botEnabled ? 'mdi-robot' : 'mdi-robot-off' }}</v-icon>
                <span class="text-body-2">자동매매</span>
                <v-btn icon size="x-small" variant="text" color="white" @click.stop="openHelp('botStatus')" class="ml-1">
                  <v-icon size="14">mdi-help-circle-outline</v-icon>
                </v-btn>
                <v-spacer />
                <v-switch
                  v-model="botEnabled"
                  :loading="botToggleLoading"
                  :disabled="botToggleLoading"
                  color="white"
                  hide-details
                  density="compact"
                  class="mr-2"
                  style="flex: none;"
                  @change="toggleBot"
                />
                <v-chip :color="botEnabled ? (botStatus.isRunning ? 'teal' : 'blue-grey-darken-1') : 'grey-darken-1'" size="x-small" variant="flat">
                  {{ botEnabled ? (botStatus.isRunning ? '실행 중' : '대기 중') : '중지됨' }}
                </v-chip>
              </v-card-title>
              <v-card-text class="pa-3">
                <div class="d-flex justify-space-between align-center text-body-2 text-grey-darken-3 mb-1">
                  <span>마지막 봇 수행 시간</span>
                  <span class="font-weight-medium">{{ formatBotTimeDisplay(botStatus.lastExecuted || botStatus.lastExecutionTime) }}</span>
                </div>
                <div class="d-flex justify-space-between align-center text-body-2 text-grey-darken-3">
                  <span>다음 봇 수행 시간</span>
                  <!-- ⭐⭐⭐ [수정] 봇 비활성화 시 "-" 표시 ⭐⭐⭐ -->
                  <span class="font-weight-medium">{{ botEnabled ? formatBotTimeDisplay(botStatus.nextExecution || botStatus.nextExecutionTime) : '-' }}</span>
                </div>
                <!-- ⭐⭐⭐ [수정] 봇 비활성화 시 "중단 상태입니다" 표시, 활성화 시 카운트다운 ⭐⭐⭐ -->
                <div v-if="!botEnabled" class="text-caption text-orange-darken-2 text-right mt-1 font-weight-medium">
                  (중단 상태입니다)
                </div>
                <div v-else-if="countdownSeconds > 0" class="text-caption text-teal-darken-2 text-right mt-1 font-weight-medium">
                  ({{ Math.floor(countdownSeconds / 60) }}분 {{ countdownSeconds % 60 }}초 후)
                </div>
                <v-chip v-if="botStatus.emergencyStop" color="red" size="x-small" variant="flat" class="mt-1">🚨 긴급정지</v-chip>
                <v-chip v-if="botStatus.isMaintenanceTime" color="orange" size="x-small" variant="flat" class="mt-1">🔧 점검시간</v-chip>
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
                <v-btn icon size="x-small" variant="text" color="white" @click.stop="openHelp('upbitBalance')" class="ml-1">
                  <v-icon size="14">mdi-help-circle-outline</v-icon>
                </v-btn>
                <v-spacer />
                <v-btn icon size="x-small" variant="text" color="white" @click="fetchUpbitAccount" :loading="loadingAccount">
                  <v-icon size="18">mdi-refresh</v-icon>
                </v-btn>
              </v-card-title>
              <v-card-text class="pa-3">
                <v-row dense>
                  <!-- 좌측: 보유자산 포트폴리오 파이차트 -->
                  <v-col cols="12" md="6">
                    <div class="text-body-2 font-weight-bold text-grey-darken-3 mb-2">보유자산 포트폴리오</div>
                    <div v-if="upbitAccount.totalAsset > 0" class="d-flex">
                      <!-- 파이차트 (3D 타원형) -->
                      <div class="portfolio-chart-wrapper" style="position: relative;">
                        <svg viewBox="0 0 260 200" class="portfolio-chart">
                          <defs>
                            <filter id="shadow3d" x="-10%" y="-10%" width="120%" height="130%">
                              <feDropShadow dx="1" dy="3" stdDeviation="3" flood-opacity="0.25"/>
                            </filter>
                          </defs>
                          <!-- 3D 측면 두께 (아래쪽으로 15px) -->
                          <path
                            v-for="(slice, index) in portfolio3dSides"
                            :key="'side-' + index"
                            :d="slice.sidePath"
                            :fill="slice.darkColor"
                            stroke="none"
                          />
                          <!-- 파이 차트 상면 (타원) -->
                          <path
                            v-for="(slice, index) in portfolio3dSlices"
                            :key="'slice-' + index"
                            :d="slice.path"
                            :fill="slice.color"
                            stroke="white"
                            stroke-width="2"
                            filter="url(#shadow3d)"
                            class="pie-slice"
                            @mouseenter="hoveredSlice = index"
                            @mouseleave="hoveredSlice = -1"
                          />
                          <!-- 조각 내 % 텍스트 (10% 이상만) -->
                          <text
                            v-for="(slice, index) in portfolio3dSlices"
                            :key="'pct-' + index"
                            v-show="slice.percent >= 10"
                            :x="slice.labelX"
                            :y="slice.labelY"
                            text-anchor="middle"
                            font-size="12"
                            font-weight="bold"
                            fill="white"
                            stroke="rgba(0,0,0,0.3)"
                            stroke-width="0.5"
                            pointer-events="none"
                          >{{ slice.percent.toFixed(1) }}%</text>
                          <!-- 중앙 도넛 홀 -->
                          <ellipse cx="130" cy="90" rx="45" ry="36" fill="white" />
                          <ellipse cx="130" cy="90" rx="45" ry="36" fill="none" stroke="rgba(0,0,0,0.08)" stroke-width="1" />
                          <text x="130" y="86" text-anchor="middle" font-size="11" fill="#616161" font-weight="500">보유비중</text>
                          <text x="130" y="99" text-anchor="middle" font-size="11" fill="#616161" font-weight="500">(%)</text>
                        </svg>
                        <!-- 호버 툴팁 (10% 미만 조각) -->
                        <div 
                          v-if="hoveredSlice >= 0 && portfolio3dSlices[hoveredSlice]?.percent < 10"
                          class="pie-tooltip"
                          :style="{ 
                            left: portfolio3dSlices[hoveredSlice].labelX + 'px',
                            top: (portfolio3dSlices[hoveredSlice].labelY - 15) + 'px'
                          }"
                        >
                          {{ portfolio3dSlices[hoveredSlice].label }} {{ portfolio3dSlices[hoveredSlice].percent.toFixed(1) }}%
                        </div>
                      </div>
                      <!-- 우측: 범례 + 보유코인 목록 세로 배치 -->
                      <div class="ml-3 d-flex flex-column justify-center" style="min-width: 140px;">
                        <!-- 범례 -->
                        <div 
                          v-for="(item, index) in portfolioLegend" 
                          :key="'legend-' + index"
                          class="d-flex align-center mb-2"
                        >
                          <div class="legend-dot mr-2" :style="{ backgroundColor: item.color }"></div>
                          <span class="text-body-2 font-weight-bold" style="min-width: 40px;">{{ item.label }}</span>
                          <span class="text-body-2 text-grey-darken-1 ml-2">{{ item.percent.toFixed(1) }}%</span>
                        </div>
                        <!-- 구분선 -->
                        <v-divider class="my-2" />
                        <!-- 보유 코인 상세 (세로 배열) -->
                        <div class="text-caption text-grey-darken-1 mb-1">보유 코인 ({{ upbitAccount.holdings.length }}종)</div>
                        <div v-if="upbitAccount.holdings.length > 0">
                          <div 
                            v-for="holding in upbitAccount.holdings" 
                            :key="'detail-' + holding.currency"
                            class="d-flex align-center justify-space-between mb-1"
                          >
                            <span class="text-body-2 font-weight-medium">{{ holding.currency }}</span>
                            <span class="text-body-2" :class="holding.profitRate >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2'">
                              {{ formatCurrency(holding.evaluation) }}
                              <span class="text-caption ml-1">({{ holding.profitRate >= 0 ? '+' : '' }}{{ holding.profitRate.toFixed(1) }}%)</span>
                            </span>
                          </div>
                        </div>
                        <div v-else class="text-caption text-grey-darken-2">없음</div>
                      </div>
                    </div>
                    <div v-else class="d-flex" style="min-height: 200px;">
                      <!-- 좌측: 자산 정보 없음 (정중앙) -->
                      <div class="d-flex flex-column align-center justify-center text-grey-darken-2" style="flex: 1; border-right: 1px solid #e0e0e0;">
                        <v-icon size="32" class="mb-1">mdi-chart-donut</v-icon>
                        <div class="text-caption">자산 정보 없음</div>
                      </div>
                      <!-- 우측: 보유 코인 목록 (상단 정렬) -->
                      <div class="pl-3" style="min-width: 140px;">
                        <div class="text-caption text-grey-darken-1 mb-1">보유 코인 ({{ upbitAccount.holdings.length }}종)</div>
                        <div v-if="upbitAccount.holdings.length > 0">
                          <v-chip
                            v-for="holding in upbitAccount.holdings" :key="'empty-' + holding.currency"
                            :color="holding.profitRate >= 0 ? 'teal' : 'red'" size="small" variant="outlined"
                            class="mb-1 mr-1"
                          >
                            <strong class="mr-1">{{ holding.currency }}</strong>
                            {{ formatCurrency(holding.evaluation) }}
                            <span class="ml-1">({{ holding.profitRate >= 0 ? '+' : '' }}{{ holding.profitRate.toFixed(1) }}%)</span>
                          </v-chip>
                        </div>
                        <div v-else class="text-caption text-grey-darken-2">없음</div>
                      </div>
                    </div>
                  </v-col>

                  <!-- 우측: 잔고 정보 세로 배치 -->
                  <v-col cols="12" md="6">
                    <v-card variant="outlined" class="pa-3 text-center mb-2">
                      <div class="text-caption text-grey-darken-1 mb-1">KRW 잔고 (매수 대기 자금)</div>
                      <div class="text-h5 font-weight-bold text-amber-darken-3">{{ formatCurrency(upbitAccount.krwBalance) }}</div>
                    </v-card>
                    <v-card variant="outlined" class="pa-3 text-center mb-2">
                      <div class="text-caption text-grey-darken-1 mb-1">코인 평가액</div>
                      <div class="text-h5 font-weight-bold text-indigo-darken-1">{{ formatCurrency(upbitAccount.coinEvaluation) }}</div>
                    </v-card>
                    <v-card variant="outlined" class="pa-3 text-center total-asset-card">
                      <div class="text-caption text-grey-darken-1 mb-1">총 자산</div>
                      <div class="text-h5 font-weight-bold text-teal-darken-2">{{ formatCurrency(upbitAccount.totalAsset) }}</div>
                    </v-card>
                    <div v-if="!authStore.user?.hasApiKey" class="text-center py-3">
                      <v-btn size="small" color="amber-darken-2" variant="tonal" @click="$router.push('/profile')">API 키 등록하기</v-btn>
                    </div>
                  </v-col>
                </v-row>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- ========== 섹션 3: 거래설정 + 일일한도 + 매수조건 ========== -->
        <v-row class="mt-3" dense>
          <v-col cols="12" md="4">
            <v-card class="fill-height" elevation="2">
              <v-card-title class="py-2 px-4 bg-indigo-darken-2 text-white d-flex align-center">
                <v-icon class="mr-2" size="20">mdi-cog</v-icon>
                <span class="text-body-1">거래 설정</span>
                <v-btn icon size="x-small" variant="text" color="white" @click.stop="openHelp('tradingSettings')" class="ml-1">
                  <v-icon size="14">mdi-help-circle-outline</v-icon>
                </v-btn>
              </v-card-title>
              <v-card-text v-if="tradingSettings" class="pa-3">
                <div class="mb-2">
                  <span class="text-caption text-grey-darken-1">거래 코인</span>
                  <div class="text-body-2 text-grey-darken-4 font-weight-medium">
                    {{ tradingSettings.coinSymbols?.map(s => s.replace('KRW-', '')).join(', ') || '-' }}
                  </div>
                </div>
                <div class="mb-2">
                  <span class="text-caption text-grey-darken-1">매수 조건</span>
                  <div class="text-body-2 text-grey-darken-4 font-weight-medium">
                    MA{{ tradingSettings.basePeriod }} 대비 {{ tradingSettings.buyThresholdPct }}% 이하
                  </div>
                </div>
                <div class="mb-2">
                  <span class="text-caption text-grey-darken-1">매도 조건</span>
                  <div class="text-body-2 font-weight-medium">
                    <span class="text-teal-darken-2">익절 +{{ tradingSettings.sellTargetPct }}%</span>
                    <span class="mx-1">/</span>
                    <span class="text-red-darken-2">손절 {{ tradingSettings.stopLossPct }}%</span>
                  </div>
                </div>
                <div class="mb-2">
                  <span class="text-caption text-grey-darken-1">종목당 최대 보유</span>
                  <div class="text-body-2 text-grey-darken-4 font-weight-medium">{{ tradingSettings.maxHoldingsPerCoin || 3 }}건</div>
                </div>
                <div class="mb-2">
                  <span class="text-caption text-grey-darken-1">일일 투자 한도</span>
                  <!-- ⭐⭐⭐ 수정: deprecated된 dailyLimitAmount 대신 실제 일일 한도 API 값 표시 ⭐⭐⭐ -->
                  <!-- 수정 이유: dailyLimitAmount는 더 이상 사용하지 않는 deprecated 필드로 -->
                  <!--           DB에 저장된 옛날 값을 보여줌. 실제 한도는 총자산 × dailyTradeLimitPct%임 -->
                  <div class="text-body-2 text-grey-darken-4 font-weight-medium">
                    {{ formatCurrency(dailyLimit.totalLimit) }}
                    <span class="text-caption text-grey-darken-1 ml-1">(총자산의 {{ tradingSettings.dailyTradeLimitPct || 20 }}%)</span>
                  </div>
                </div>
                <div class="mb-2">
                  <span class="text-caption text-grey-darken-1">AI 뉴스 분석</span>
                  <v-chip :color="tradingSettings.useAiAnalysis ? 'teal' : 'grey'" size="x-small" variant="flat" class="ml-2">
                    {{ tradingSettings.useAiAnalysis ? '사용' : '미사용' }}
                  </v-chip>
                </div>
                <v-btn size="small" color="indigo" variant="text" class="mt-1 px-0" @click="$router.push('/trading-settings')">설정 변경 →</v-btn>
              </v-card-text>
              <v-card-text v-else class="pa-0 fill-height">
                <div class="d-flex align-center justify-center" style="height: 220px;">
                  <v-btn size="large" color="indigo-darken-1" variant="flat" @click="$router.push('/trading-settings')">
                    <v-icon start>mdi-cog</v-icon>
                    설정하기
                  </v-btn>
                </div>
              </v-card-text>
            </v-card>
          </v-col>

          <v-col cols="12" md="4">
            <v-card class="fill-height" elevation="2">
              <v-card-title class="py-2 px-4 bg-orange-darken-2 text-white d-flex align-center">
                <v-icon class="mr-2" size="20">mdi-clock-outline</v-icon>
                <span class="text-body-1">일일 한도</span>
                <v-btn icon size="x-small" variant="text" color="white" @click.stop="openHelp('dailyLimit')" class="ml-1">
                  <v-icon size="14">mdi-help-circle-outline</v-icon>
                </v-btn>
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
                    <v-chip-group>
                      <v-chip 
                        v-for="(count, symbol) in holdingsPerCoin" 
                        :key="symbol" 
                        size="small" 
                        variant="outlined"
                        :color="count >= (tradingSettings?.maxHoldingsPerCoin || 3) ? 'red' : 'teal'"
                      >
                        <strong class="mr-1">{{ symbol }}</strong>
                        {{ count }}/{{ tradingSettings?.maxHoldingsPerCoin || 3 }}
                      </v-chip>
                    </v-chip-group>
                    <span v-if="Object.keys(holdingsPerCoin).length === 0" class="text-caption text-grey-darken-2">없음</span>
                  </div>
                </div>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- ★★★ 수정: 매수 조건 카드 재구성 - 더 알기 쉽게 ★★★ -->
          <v-col cols="12" md="4">
            <v-card class="fill-height" elevation="2">
              <v-card-title class="py-2 px-4 bg-teal-darken-2 text-white d-flex align-center">
                <v-icon class="mr-2" size="20">mdi-target</v-icon>
                <span class="text-body-1">매수 조건</span>
                <v-btn icon size="x-small" variant="text" color="white" @click.stop="openHelp('buyCondition')" class="ml-1">
                  <v-icon size="14">mdi-help-circle-outline</v-icon>
                </v-btn>
                <v-spacer />
                <v-btn icon size="x-small" variant="text" color="white" @click="fetchIndicators" :loading="loadingIndicators">
                  <v-icon size="18">mdi-refresh</v-icon>
                </v-btn>
              </v-card-title>
              <v-card-text class="pa-2">
                <!-- ★★★ [추가] 매수 조건 설명 (AI 가중치 반영) ★★★ -->
                <v-alert 
                  v-if="tradingSettings" 
                  type="info" 
                  variant="tonal" 
                  density="compact" 
                  class="mb-2 text-caption"
                  :icon="false"
                >
                  <v-icon size="14" class="mr-1">mdi-information</v-icon>
                  <template v-if="tradingSettings.useAiAnalysis">
                    MA{{ tradingSettings.basePeriod || 20 }} {{ tradingSettings.buyThresholdPct }}% 이하 매수 (AI 뉴스 기반 ±0.5% 조정)
                  </template>
                  <template v-else>
                    MA{{ tradingSettings.basePeriod || 20 }} 대비 {{ tradingSettings.buyThresholdPct }}% 이하일 때 매수
                  </template>
                </v-alert>
      
                <v-list density="compact" class="pa-0">
                  <v-list-item 
                    v-for="coin in coinIndicators" 
                    :key="coin.symbol" 
                    class="px-2 py-1"
                    style="min-height: 48px;"
                  >
                    <div class="d-flex align-center justify-space-between w-100">
                      <div class="flex-grow-1">
                        <div class="d-flex align-center">
                          <span class="text-body-2 font-weight-medium text-grey-darken-4">
                            {{ coin.symbol.replace('KRW-', '') }}
                          </span>
                          <!-- ★★★ [추가] AI 가중치 적용 코인 표시 ★★★ -->
                          <v-chip 
                            v-if="coin.aiWeight && coin.aiWeight !== 0" 
                            size="x-small" 
                            :color="coin.aiWeight > 0 ? 'teal' : 'red'" 
                            variant="outlined"
                            class="ml-1"
                          >
                            AI {{ coin.aiWeight > 0 ? '+' : '' }}{{ coin.aiWeight.toFixed(1) }}%
                          </v-chip>
                        </div>
                        <!-- ★★★ [수정] 가격 정보에 설명 추가 ★★★ -->
                        <div class="text-caption text-grey-darken-1">
                          <span class="font-weight-medium">현재가:</span> {{ formatCompactPrice(coin.currentPrice) }} 
                          <v-icon size="12" class="mx-1">mdi-arrow-right</v-icon>
                          <span class="font-weight-medium">매수가:</span> {{ formatCompactPrice(coin.buyPrice) }}
                          <!-- ★★★ [추가] AI 가중치 적용된 매수가도 표시 ★★★ -->
                          <template v-if="coin.aiAdjustedBuyPrice && coin.aiAdjustedBuyPrice !== coin.buyPrice">
                            <span class="text-teal-darken-2">(AI: {{ formatCompactPrice(coin.aiAdjustedBuyPrice) }})</span>
                          </template>
                        </div>
                        <div class="text-caption" :class="coin.canBuy ? 'text-teal-darken-2' : 'text-grey'">
                          현재 이격도 {{ coin.dropRate?.toFixed(2) || '0.00' }}% 
                          <span v-if="!coin.canBuy">({{ Math.abs(coin.remainingDrop || 0).toFixed(1) }}% 더 하락 필요)</span>
                          <span v-else class="font-weight-bold">(매수 조건 충족!)</span>
                        </div>
                      </div>
                      <div class="d-flex align-center">
                        <!-- ★★★ [수정] 상세 지표 보기 칩 스타일 ★★★ -->
                        <v-chip 
                          size="x-small" 
                          variant="outlined"
                          color="indigo"
                          class="mr-1"
                          style="cursor: pointer;"
                          @click="openIndicatorDetail(coin)"
                        >
                          상세 지표 보기
                        </v-chip>
                        <v-chip 
                          :color="coin.canBuy ? 'teal' : 'blue-grey-darken-1'" 
                          size="x-small" 
                          variant="flat"
                        >
                          {{ coin.canBuy ? '매수가능' : '대기' }}
                        </v-chip>
                      </div>
                    </div>
                  </v-list-item>
                </v-list>
      
                <div v-if="!coinIndicators.length" class="d-flex flex-column align-center justify-center text-grey" style="height: 100%; min-height: 180px;">
                  <v-icon size="32" class="mb-2">mdi-chart-timeline-variant</v-icon>
                  <div class="text-caption">거래 설정을 먼저 완료해주세요</div>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- ========== 섹션 4: 수익 현황 ========== -->
        <v-row class="mt-3" dense>
          <v-col cols="12">
            <v-card elevation="2">
              <v-card-title class="py-2 px-4 bg-teal-darken-2 text-white d-flex align-center">
                <v-icon class="mr-2" size="20">mdi-cash-multiple</v-icon>
                <span class="text-body-1">수익 현황</span>
                <v-btn icon size="x-small" variant="text" color="white" @click.stop="openHelp('profitSummary')" class="ml-1">
                  <v-icon size="14">mdi-help-circle-outline</v-icon>
                </v-btn>
                <!-- ⭐ Day 31 추가: 상세 분석 버튼 -->
                <v-spacer />
                <v-btn 
                  size="small" 
                  variant="flat" 
                  color="amber" 
                  class="text-grey-darken-4"
                  @click="$router.push('/holdings?tab=period')"
                >
                  상세 분석 →
                </v-btn>
              </v-card-title>
              <v-card-text class="pa-3">
                <v-row dense>
                  <!-- ⭐ Day 31 추가: 오늘 수익 카드 -->
                  <v-col cols="12" md="3">
                    <v-card variant="outlined" class="pa-3 text-center" height="110">
                      <div class="text-caption text-grey-darken-2">🌅 오늘 수익</div>
                      <div :class="todayProfit >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2'" class="text-h5 font-weight-bold mt-2">
                        {{ todayProfit >= 0 ? '+' : '' }}{{ formatCurrency(todayProfit) }}
                      </div>
                      <div class="text-caption mt-1" :class="todayProfitPct >= 0 ? 'text-teal-darken-1' : 'text-red-darken-1'">
                        ({{ todayProfitPct >= 0 ? '+' : '' }}{{ todayProfitPct.toFixed(2) }}%)
                      </div>
                    </v-card>
                  </v-col>
                  <v-col cols="12" md="3">
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
                  <v-col cols="12" md="3">
                    <v-card variant="outlined" class="pa-3 text-center" height="110">
                      <div class="text-caption text-grey-darken-2">✅ 실현 수익 (확정)</div>
                      <div :class="profitSummary.realizedProfit >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2'" class="text-h5 font-weight-bold mt-2">
                        {{ profitSummary.realizedProfit >= 0 ? '+' : '' }}{{ formatCurrency(profitSummary.realizedProfit) }}
                      </div>
                      <div class="text-caption mt-1 text-grey-darken-1">&nbsp;</div>
                    </v-card>
                  </v-col>
                  <v-col cols="12" md="3">
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
                <v-btn icon size="x-small" variant="text" color="white" @click.stop="openHelp('assetChart')" class="ml-1">
                  <v-icon size="14">mdi-help-circle-outline</v-icon>
                </v-btn>
                <v-spacer />
                <!-- ★★★ 수정: 기간 버튼 대비 강화 + 사용자 지정 기간 추가 ★★★ -->
                <v-btn-toggle v-model="chartPeriod" density="compact" mandatory variant="outlined" size="small" class="chart-period-toggle">
                  <v-btn value="7" size="x-small" :class="chartPeriod === '7' ? 'active-period' : ''">7일</v-btn>
                  <v-btn value="month" size="x-small" :class="chartPeriod === 'month' ? 'active-period' : ''">이번달</v-btn>
                  <v-btn value="year" size="x-small" :class="chartPeriod === 'year' ? 'active-period' : ''">올해</v-btn>
                  <v-btn value="all" size="x-small" :class="chartPeriod === 'all' ? 'active-period' : ''">전체 투자기간</v-btn>
                </v-btn-toggle>
                <!-- ★★★ [수정] 사용자 지정 기간 입력 필드 - 높이 통일 ★★★ -->
                <div class="d-flex align-center ml-3 chart-control-group">
                  <input 
                    type="date" 
                    v-model="customStartDate" 
                    class="custom-date-input"
                    @change="applyCustomDateRange"
                  />
                  <span class="mx-1 text-white">~</span>
                  <input 
                    type="date" 
                    v-model="customEndDate" 
                    class="custom-date-input"
                    @change="applyCustomDateRange"
                  />
                </div>
                <!-- ★★★ [수정] 전체보기/스크롤보기 토글 - 높이 통일 ★★★ -->
                <div class="d-flex align-center ml-2 chart-control-group">
                  <v-btn 
                    size="x-small" 
                    :color="dashboardChartViewMode === 'full' ? 'amber' : 'grey-lighten-1'"
                    variant="flat"
                    :class="dashboardChartViewMode === 'full' ? 'text-grey-darken-4' : 'text-grey-darken-2'"
                    class="chart-view-btn"
                    @click="dashboardChartViewMode = 'full'"
                  >
                    <v-icon size="14" class="mr-1">mdi-fit-to-screen</v-icon>
                    전체 보기
                  </v-btn>
                  <v-divider vertical class="mx-1 chart-divider" />
                  <v-btn 
                    size="x-small"
                    :color="dashboardChartViewMode === 'scroll' ? 'amber' : 'grey-lighten-1'"
                    variant="flat"
                    :class="dashboardChartViewMode === 'scroll' ? 'text-grey-darken-4' : 'text-grey-darken-2'"
                    class="chart-view-btn"
                    @click="dashboardChartViewMode = 'scroll'"
                  >
                    <v-icon size="14" class="mr-1">mdi-arrow-left-right</v-icon>
                    스크롤 보기
                  </v-btn>
                </div>
              </v-card-title>
                <v-card-text class="pa-3">
                <!-- ★★★ [수정] HoldingsView 스타일 자산 변동 추이 차트 ★★★ -->
                <div v-if="assetHistory.length > 0 || upbitAccount.totalAsset > 0" class="chart-container">
                  <div 
                    class="chart-wrapper-backtest"
                    :class="{ 'scroll-mode': dashboardChartViewMode === 'scroll' }"
                    :style="dashboardChartViewMode === 'scroll' ? { width: dynamicChartWidth + 'px' } : {}"
                    @mousemove="handleChartHover"
                    @mouseleave="hoveredIndex = -1; showEmptyTooltip = false"
                  >
                    <svg 
                      class="custom-chart"
                      :viewBox="`0 0 ${effectiveWidth} ${svgHeightBacktest}`"
                      preserveAspectRatio="none"
                    >
                      <!-- 그라데이션 정의 -->
                      <defs>
                        <linearGradient id="dashboardAreaGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                          <stop offset="0%" style="stop-color:#42A5F5;stop-opacity:0.6" />
                          <stop offset="100%" style="stop-color:#42A5F5;stop-opacity:0.1" />
                        </linearGradient>
                      </defs>
                      
                      <!-- 거래 데이터가 있을 때 -->
                      <!-- ⭐⭐⭐ [변경] 자산 변동 추이 차트 재구성 ⭐⭐⭐ -->
                      <template v-if="assetHistory.length > 0">
                        

                        <!-- 영역 채우기 (평가금액 - 연한 파란색) -->
                        <path :d="areaPathBacktest" fill="url(#dashboardAreaGradient)" />

	          <!-- 불입금액 막대그래프 (주황색) -->
                        <rect
                          v-for="(point, index) in chartPointsBacktest"
                          :key="'bar-' + index"
                          :x="point.x - barWidth / 2"
                          :y="getYPositionBacktest(point.depositAmount)"
                          :width="barWidth"
                          :height="Math.max(0, (svgHeightBacktest - svgPadding) - getYPositionBacktest(point.depositAmount))"
                          fill="#FF9800"
                          :opacity="hoveredIndex === index ? 0.6 : 0.35"
                          rx="1"
                        />

                        <!-- 최고 평가금액 파선 (초록) -->
                        <line
                          :x1="svgPadding" :y1="getYPositionBacktest(maxEvaluation)"
                          :x2="effectiveWidth - (dashboardChartViewMode === 'scroll' ? scrollPaddingRight : svgPaddingRight)" :y2="getYPositionBacktest(maxEvaluation)"
                          stroke="#4CAF50" stroke-width="2" stroke-dasharray="6,4"
                        />
                        <!-- 최저 평가금액 파선 (빨강) -->
                        <line
                          :x1="svgPadding" :y1="getYPositionBacktest(minEvaluation)"
                          :x2="effectiveWidth - (dashboardChartViewMode === 'scroll' ? scrollPaddingRight : svgPaddingRight)" :y2="getYPositionBacktest(minEvaluation)"
                          stroke="#F44336" stroke-width="2" stroke-dasharray="6,4"
                        />

                        <!-- 불입금액 추세선 (주황 파선) -->
                        <path :d="depositLinePathBacktest" fill="none" stroke="#FF9800" stroke-width="2" stroke-dasharray="6,4" />

                        <!-- 평가금액 추세선 (파란 파선) -->
                        <path :d="linePathBacktest" fill="none" stroke="#1976D2" stroke-width="2.5" stroke-dasharray="8,4" />

                        <!-- 평가금액 데이터 포인트 -->
                        <circle
                          v-for="(point, index) in chartPointsBacktest"
                          :key="'eval-' + index"
                          :cx="point.x" :cy="point.y"
                          :r="hoveredIndex === index ? 8 : 4"
                          :fill="getPointColorBacktest(point.evaluationAmount)"
                          stroke="white" stroke-width="2" class="chart-point"
                        />

                        <!-- ⭐⭐⭐ [변경] 불입금액 점: 막대 상단에 위치 ⭐⭐⭐ -->
                        <circle
                          v-for="(point, index) in chartPointsBacktest"
                          :key="'dep-' + index"
                          :cx="point.x" :cy="getYPositionBacktest(point.depositAmount)"
                          :r="hoveredIndex === index ? 6 : 3"
                          fill="#FF9800" stroke="white" stroke-width="1.5" class="chart-point"
                        />
                      </template>
                      
                      <!-- ★★★ [수정] 거래 데이터 없을 때 - effectiveWidth 사용 ★★★ -->
                      <template v-else>
                        <!-- 영역 채우기 (현재 자산 수평선) -->
                        <rect 
                          :x="svgPadding" 
                          :y="svgHeightBacktest / 2 - 2"
                          :width="effectiveWidth - svgPadding - svgPaddingRight"
                          :height="4"
                          fill="url(#dashboardAreaGradient)"
                          opacity="0.3"
                        />
                        
                        <!-- 초기 자산 기준선 (주황색 점선) -->
                        <line 
                          :x1="svgPadding" 
                          :y1="svgHeightBacktest / 2" 
                          :x2="effectiveWidth - svgPaddingRight" 
                          :y2="svgHeightBacktest / 2"
                          stroke="#FF9800" 
                          stroke-width="2" 
                          stroke-dasharray="6,4"
                        />
                        
                        <!-- 현재 자산 라인 (파란색 실선) -->
                        <line 
                          :x1="svgPadding" 
                          :y1="svgHeightBacktest / 2" 
                          :x2="effectiveWidth - svgPaddingRight" 
                          :y2="svgHeightBacktest / 2" 
                          stroke="#1976D2" 
                          stroke-width="2.5"
                        />
  
                        <!-- ★★★ 수정: 기간 내 모든 날짜 점 표시 ★★★ -->
                        <circle
                          v-for="(point, index) in emptyChartPoints"
                          :key="index"
                          :cx="point.x"
                          :cy="point.y"
                          :r="hoveredIndex === index ? 8 : 4"
                          fill="#1976D2"
                          stroke="white"
                          stroke-width="2"
                          class="chart-point"
                        />
                      </template>
                    </svg>
                    
                    <!-- 기준선 라벨 (백테스팅 스타일) - 항상 표시 -->
                    <div class="chart-labels-backtest">
                      <!-- ⭐⭐⭐ [변경] 라벨: 풀 텍스트 + 겹침 오프셋 적용 ⭐⭐⭐ -->
                      <!-- 왜: 금액이 같으면 라벨이 완전히 겹쳐 하나만 보임 -->
                      <template v-if="assetHistory.length > 0">
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
                          차트 바닥 : {{ formatCurrency(minBalanceBacktest) }}
                        </span>
                      </template>
                    </div>
                    
                    <!-- ★★★ [수정] 툴팁 - 점 옆에 표시 (점을 가리지 않도록) ★★★ -->
                    <div 
                      v-if="hoveredIndex >= 0 && hoveredData && assetHistory.length > 0"
                      class="chart-tooltip-backtest"
                      :style="{ 
                        left: (tooltipX > chartWrapperWidth * 0.5 ? tooltipX - 10 : tooltipX + 10) + 'px',
                        top: Math.max(60, Math.min(svgHeightBacktest - 80, tooltipY)) + 'px',
                        transform: tooltipX > chartWrapperWidth * 0.5 ? 'translateX(-100%) translateY(-50%)' : 'translateY(-50%)'
                      }"
                    >
                     <!-- ⭐⭐⭐ [변경] 툴팁 5개 항목으로 확장 ⭐⭐⭐ -->
                      <div class="font-weight-bold mb-1">{{ hoveredData.date || '-' }}</div>
                      <div style="color: #64B5F6;">평가금액: {{ formatCurrency(hoveredData.evaluationAmount || hoveredData.balance) }}</div>
                      <div style="color: #FFB74D;">불입금액: {{ formatCurrency(hoveredData.depositAmount || initialAsset) }}</div>
                      <div :class="(hoveredData.profitRate || 0) >= 0 ? 'text-success' : 'text-error'">
                        수익률: {{ (hoveredData.profitRate || 0) >= 0 ? '+' : '' }}{{ Number(hoveredData.profitRate || 0).toFixed(2) }}%
                      </div>
                      <div :class="(hoveredData.profitAmount || 0) >= 0 ? 'text-success' : 'text-error'">
                        수익금액: {{ (hoveredData.profitAmount || 0) >= 0 ? '+' : '' }}{{ formatCurrency(hoveredData.profitAmount || 0) }}
                      </div>
                    </div>
                    
                    <!-- ★★★ [수정] 거래 이력 없을 때 호버 툴팁 - 점 옆에 표시 ★★★ -->
                    <div 
                      v-if="showEmptyTooltip && assetHistory.length === 0 && emptyHoveredData"
                      class="chart-tooltip-backtest"
                      :style="{ 
                        left: (tooltipX > chartWrapperWidth * 0.5 ? tooltipX - 10 : tooltipX + 10) + 'px',
                        top: tooltipY + 'px',
                        transform: tooltipX > chartWrapperWidth * 0.5 ? 'translateX(-100%) translateY(-50%)' : 'translateY(-50%)'
                      }"
                    >
                      <div class="font-weight-bold">{{ emptyHoveredData.date }}</div>
                      <div>자산: {{ formatCurrency(emptyHoveredData.balance) }}</div>
                      <div class="text-orange">거래 이력 없음</div>
                    </div>
                  </div>
                  
                  <!-- 차트 하단 날짜 표시 -->
                  <div class="chart-dates d-flex justify-space-between mt-2 px-4">
                    <span class="text-caption text-grey-darken-1">
                      {{ displayChartStartDate }}
                      <span v-if="isBeforeFirstTrade" class="text-orange-darken-2 ml-2">
                        (투자 시작일 이전 내역은 조회가 불가능합니다)
                      </span>
                    </span>
                    <span class="text-caption text-grey-darken-1">
                      {{ assetHistory.length > 0 ? chartEndDate : formatTodayDate() }}
                    </span>
                  </div>
                </div>
                
                <!-- 자산 정보 없음 -->
                <div v-else class="text-center py-6 text-grey-darken-2">
                  <v-icon size="48" class="mb-2" color="grey">mdi-chart-line-variant</v-icon>
                  <div class="text-body-1">자산 정보가 없습니다</div>
                  <div class="text-caption">업비트 API 키를 등록하고 거래 설정을 완료해주세요</div>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- ========== 섹션 6: 코인별 성과 + 최근 거래 + 시스템 알림 ========== -->
        <v-row class="mt-3 mb-4" dense>
          <v-col cols="12" md="4">
            <v-card class="fill-height" elevation="2">
              <v-card-title class="py-2 px-4 bg-indigo-darken-1 text-white" style="min-height: 48px;">
                <v-icon class="mr-2" size="20">mdi-podium</v-icon>
                <span class="text-body-1">코인별 성과</span>
                <v-btn icon size="x-small" variant="text" color="white" @click.stop="openHelp('coinPerformance')" class="ml-1">
                  <v-icon size="14">mdi-help-circle-outline</v-icon>
                </v-btn>
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
                <div v-else class="d-flex align-center justify-center text-grey-darken-2 text-body-2" style="height: 100%; min-height: 150px;">매도 완료된 거래가 없습니다</div>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- ★★★ 수정: 최근 거래 가로 배치 ★★★ -->
          <v-col cols="12" md="4">
            <v-card class="fill-height" elevation="2">
              <v-card-title class="py-2 px-4 bg-indigo-darken-1 text-white d-flex align-center" style="min-height: 48px;">
                <v-icon class="mr-2" size="20">mdi-history</v-icon>
                <span class="text-body-1">최근 거래</span>
                <v-btn icon size="x-small" variant="text" color="white" @click.stop="openHelp('recentTransactions')" class="ml-1">
                  <v-icon size="14">mdi-help-circle-outline</v-icon>
                </v-btn>
                <v-spacer />
                <v-btn size="small" variant="flat" color="amber" class="text-grey-darken-4" @click="$router.push('/transactions')">전체 거래 내역 보기 →</v-btn>
              </v-card-title>
              <v-card-text class="pa-2">
                <v-list v-if="recentTransactions.length > 0" density="compact">
                  <v-list-item v-for="tx in recentTransactions.slice(0, 5)" :key="tx.id" class="px-2 py-1">
                    <div class="d-flex align-center justify-space-between w-100">
                      <!-- ★★★ [수정] 코인명 + 매수/매도 아이콘 구분 - 간격 확대 ★★★ -->
                      <div class="d-flex align-center" style="min-width: 85px;">
                        <v-avatar 
                          :color="tx.status === 'SOLD' ? 'amber-darken-2' : 'indigo'" 
                          size="24" 
                          class="mr-3"
                        >
                          <v-icon size="14" color="white">
                            {{ tx.status === 'SOLD' ? 'mdi-arrow-up' : 'mdi-arrow-down' }}
                          </v-icon>
                        </v-avatar>
                        <span class="text-body-2 font-weight-medium text-grey-darken-4">
                          {{ tx.coinSymbol?.replace('KRW-', '') }}
                        </span>
                      </div>
                      <!-- ★★★ [수정] 가격/날짜 + 수익 2줄 배치 ★★★ -->
                      <div class="flex-grow-1 px-2" style="text-align: center;">
                        <div class="text-caption text-grey-darken-2">
                          {{ formatCurrency(tx.totalAmount) }} · {{ formatDate(tx.createdAt) }}
                        </div>
                        <div 
                          v-if="tx.status === 'SOLD' && tx.profitLoss != null" 
                          :class="tx.profitLoss >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2'" 
                          class="text-caption font-weight-medium"
                        >
                          {{ tx.profitLoss >= 0 ? '+' : '' }}{{ formatCurrency(tx.profitLoss) }}
                        </div>
                      </div>
                      <!-- ★★★ [수정] 상태 칩 - 매도완료/매수+보유중 구분 ★★★ -->
                      <div class="d-flex align-center justify-end" style="min-width: 110px;">
                        <!-- 매도 완료: 매도완료만 표시 -->
                        <template v-if="tx.status === 'SOLD'">
                          <v-chip 
                            color="orange" 
                            size="x-small" 
                            variant="flat"
                            style="width: 60px; justify-content: center;"
                          >
                            매도완료
                          </v-chip>
                        </template>
                        <!-- 보유중: 매수 + 보유중 표시 -->
                        <template v-else>
                          <v-chip 
                            color="indigo" 
                            size="x-small" 
                            variant="flat"
                            style="width: 40px; justify-content: center;"
                          >
                            매수
                          </v-chip>
                          <v-chip 
                            color="teal" 
                            size="x-small" 
                            variant="outlined"
                            class="ml-1"
                            style="width: 56px; justify-content: center;"
                          >
                            보유중
                          </v-chip>
                        </template>
                      </div>
                    </div>
                  </v-list-item>
                </v-list>
                <div v-else class="d-flex align-center justify-center text-grey-darken-2 text-body-2" style="height: 100%; min-height: 150px;">거래 내역이 없습니다</div>
              </v-card-text>
            </v-card>
          </v-col>

          <v-col cols="12" md="4">
            <v-card class="fill-height" elevation="2">
              <!-- ★★★ 수정: 시스템 알림 빨간색 배경 ★★★ -->
              <v-card-title class="py-2 px-4 bg-red-darken-2 text-white d-flex align-center">
                <v-icon class="mr-2" size="20">mdi-bell-alert</v-icon>
                <span class="text-body-1">시스템 알림</span>
                <v-btn icon size="x-small" variant="text" color="white" @click.stop="openHelp('systemAlert')" class="ml-1">
                  <v-icon size="14">mdi-help-circle-outline</v-icon>
                </v-btn>
              </v-card-title>
              <v-card-text class="pa-3 d-flex flex-column" style="min-height: 150px;">
                <!-- ⭐ Day 30 개선: 최신 공지사항 한줄 표시 -->
                <div 
                  v-if="latestReleaseNote" 
                  class="d-flex align-center mb-2 pa-2 rounded cursor-pointer bg-blue-lighten-5"
                  @click="$router.push('/release-notes')"
                  style="border-left: 3px solid #1976D2;"
                >
                  <v-icon size="16" color="info" class="mr-2">mdi-information</v-icon>
                  <span class="text-body-2 text-truncate flex-grow-1">
                    <strong>📢</strong> {{ latestReleaseNote.title }}
                  </span>
                  <span class="text-caption text-grey-darken-1 ml-2 text-no-wrap">
                    {{ formatDateShort(latestReleaseNote.createdAt) }}
                  </span>
                  <v-icon size="14" color="grey" class="ml-1">mdi-chevron-right</v-icon>
                </div>
      
                <!-- ★★★ 수정: 알림이 있을 때 아이콘+텍스트 중앙 정렬 ★★★ -->
                <div v-if="systemAlerts.length > 0" class="d-flex flex-column align-center justify-center flex-grow-1">
                  <div v-for="(alert, index) in systemAlerts" :key="index" class="d-flex align-center mb-2">
                    <v-icon :color="alert.type === 'error' ? 'red' : alert.type === 'warning' ? 'orange' : 'info'" class="mr-2">{{ alert.type === 'error' ? 'mdi-alert-circle' : alert.type           === 'warning' ? 'mdi-alert' : 'mdi-information' }}</v-icon>
                    <span class="font-weight-medium text-grey-darken-3">{{ alert.message }}</span>
                  </div>
                </div>
                <div v-else-if="!latestReleaseNote" class="d-flex flex-column align-center justify-center text-grey-darken-2 flex-grow-1">
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
      </v-container>      <v-dialog v-model="showIndicatorDetailDialog" max-width="700">
        <v-card>
          <v-card-title class="bg-teal-darken-2 text-white d-flex align-center">
            <v-icon class="mr-2">mdi-chart-box</v-icon>
            {{ selectedCoinDetail?.symbol?.replace('KRW-', '') || '' }} 매수 조건 상세
            <v-spacer />
            <v-btn icon variant="text" color="white" @click="showIndicatorDetailDialog = false">
              <v-icon>mdi-close</v-icon>
            </v-btn>
          </v-card-title>
          <v-card-text class="pa-4" v-if="selectedCoinDetail">
            <v-table density="compact">
              <thead>
                <tr>
                  <th class="text-left" style="min-width: 140px;">항목</th>
                  <th class="text-right" style="min-width: 100px;">설정값</th>
                  <th class="text-right" style="min-width: 120px;">현재값</th>
                  <th class="text-center" style="min-width: 60px;">상태</th>
                </tr>
              </thead>
              <tbody>
                <!-- MA 기준 하락률 -->
                <tr>
                  <td class="text-no-wrap">
                    <v-icon size="16" class="mr-1">mdi-trending-down</v-icon>
                    MA{{ tradingSettings?.basePeriod || 20 }} 하락률
                  </td>
                  <td class="text-right font-weight-medium text-no-wrap">{{ tradingSettings?.buyThresholdPct || -6 }}% 이하</td>
                  <td class="text-right text-no-wrap" :class="selectedCoinDetail.dropRate <= (tradingSettings?.buyThresholdPct || -6) ? 'text-teal font-weight-bold' : ''">
                    {{ selectedCoinDetail.dropRate?.toFixed(2) }}%
                  </td>
                  <td class="text-center">
                    <v-icon :color="selectedCoinDetail.dropRate <= (tradingSettings?.buyThresholdPct || -6) ? 'teal' : 'grey'" size="20">
                      {{ selectedCoinDetail.dropRate <= (tradingSettings?.buyThresholdPct || -6) ? 'mdi-check-circle' : 'mdi-circle-outline' }}
                    </v-icon>
                  </td>
                </tr>
                
                <!-- AI 가중치 (사용 시) -->
                <tr v-if="tradingSettings?.useAiAnalysis">
                  <td class="text-no-wrap">
                    <v-icon size="16" class="mr-1">mdi-robot</v-icon>
                    AI 뉴스 가중치
                  </td>
                  <td class="text-right font-weight-medium text-no-wrap">±0.5%</td>
                  <td class="text-right text-no-wrap" :class="selectedCoinDetail.aiWeight > 0 ? 'text-teal' : selectedCoinDetail.aiWeight < 0 ? 'text-red' : ''">
                    {{ selectedCoinDetail.aiWeight ? (selectedCoinDetail.aiWeight > 0 ? '+' : '') + selectedCoinDetail.aiWeight.toFixed(2) + '%' : '0%' }}
                  </td>
                  <td class="text-center">
                    <v-chip size="x-small" :color="selectedCoinDetail.aiWeight > 0 ? 'teal' : selectedCoinDetail.aiWeight < 0 ? 'red' : 'grey'" variant="flat">
                      {{ selectedCoinDetail.aiWeight > 0 ? '호재' : selectedCoinDetail.aiWeight < 0 ? '악재' : '중립' }}
                    </v-chip>
                  </td>
                </tr>
                
                <!-- 최종 매수 기준 -->
                <tr v-if="tradingSettings?.useAiAnalysis && selectedCoinDetail.aiWeight">
                  <td class="text-no-wrap">
                    <v-icon size="16" class="mr-1">mdi-target</v-icon>
                    최종 매수 기준
                  </td>
                  <td class="text-right font-weight-medium text-teal text-no-wrap">
                    {{ ((tradingSettings?.buyThresholdPct || -6) + (selectedCoinDetail.aiWeight || 0)).toFixed(2) }}% 이하
                  </td>
                  <td class="text-right">-</td>
                  <td class="text-center">-</td>
                </tr>
                
                <!-- 현재가 -->
                <tr>
                  <td class="text-no-wrap">
                    <v-icon size="16" class="mr-1">mdi-cash</v-icon>
                    현재가
                  </td>
                  <td class="text-right">-</td>
                  <td class="text-right font-weight-bold text-no-wrap">{{ formatCurrency(selectedCoinDetail.currentPrice) }}</td>
                  <td class="text-center">-</td>
                </tr>
                
                <!-- MA20 -->
                <tr>
                  <td class="text-no-wrap">
                    <v-icon size="16" class="mr-1">mdi-chart-line</v-icon>
                    MA{{ tradingSettings?.basePeriod || 20 }}
                  </td>
                  <td class="text-right">-</td>
                  <td class="text-right text-no-wrap">{{ formatCurrency(selectedCoinDetail.ma20) }}</td>
                  <td class="text-center">-</td>
                </tr>
                
                <!-- 목표 매수가 -->
                <tr>
                  <td class="text-no-wrap">
                    <v-icon size="16" class="mr-1">mdi-arrow-down-bold</v-icon>
                    목표 매수가
                  </td>
                  <td class="text-right">-</td>
                  <td class="text-right font-weight-bold text-indigo text-no-wrap">
                    {{ formatCurrency(selectedCoinDetail.aiAdjustedBuyPrice || selectedCoinDetail.buyPrice) }}
                  </td>
                  <td class="text-center">-</td>
                </tr>
                
                <!-- 남은 하락폭 -->
                <tr>
                  <td class="text-no-wrap">
                    <v-icon size="16" class="mr-1">mdi-delta</v-icon>
                    남은 하락폭
                  </td>
                  <td class="text-right">-</td>
                  <td class="text-right text-no-wrap" :class="selectedCoinDetail.canBuy ? 'text-teal font-weight-bold' : 'text-orange'">
                    {{ selectedCoinDetail.canBuy ? '조건 충족!' : Math.abs(selectedCoinDetail.remainingDrop || 0).toFixed(2) + '%' }}
                  </td>
                  <td class="text-center">
                    <v-icon :color="selectedCoinDetail.canBuy ? 'teal' : 'orange'" size="20">
                      {{ selectedCoinDetail.canBuy ? 'mdi-check-circle' : 'mdi-clock-outline' }}
                    </v-icon>
                  </td>
                </tr>
              </tbody>
            </v-table>
            
            <!-- 추가 정보 -->
            <v-divider class="my-3" />
            <div class="text-caption text-grey">
              <v-icon size="14" class="mr-1">mdi-information-outline</v-icon>
              매수 조건: MA{{ tradingSettings?.basePeriod || 20 }} 대비 {{ tradingSettings?.buyThresholdPct || -6 }}% 
              {{ tradingSettings?.useAiAnalysis ? '+ AI 가중치' : '' }} 이하 하락 시 매수 신호 발생
            </div>
          </v-card-text>
        </v-card>
      </v-dialog>
    </v-main>
    <!-- ★★★ [추가] 카드 도움말 다이얼로그 ★★★ -->
    <v-dialog v-model="showHelpDialog" max-width="700">
      <v-card>
        <v-card-title class="bg-indigo-darken-2 text-white d-flex align-center">
          <v-icon class="mr-2">mdi-help-circle</v-icon>
          {{ currentHelp.title }}
          <v-spacer />
          <v-btn icon variant="text" color="white" @click="showHelpDialog = false">
            <v-icon>mdi-close</v-icon>
          </v-btn>
        </v-card-title>
        <v-card-text class="pa-4">
          <div v-html="currentHelp.content" class="help-content"></div>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn color="primary" variant="flat" @click="showHelpDialog = false">확인</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-app>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'
import { useAuthStore } from '@/stores/auth'
import api, { coinApi, transactionApi, tradingApi, botApi, profitApi } from '@/api'  // ⭐ profitApi 추가
import OnboardingGuide from '@/components/OnboardingGuide.vue'

const authStore = useAuthStore()
const sidebarRef = ref()

const loadingAccount = ref(false)
const loadingIndicators = ref(false)
const isRefreshing = ref(false)
const showAllIndicators = ref(false)

// 카드 도움말 시스템
const showHelpDialog = ref(false)
const currentHelp = ref({ title: '', content: '' })

// 온보딩 가이드 상태
const hasApiKey = computed(() => authStore.user?.hasApiKey || false)
const hasSettings = computed(() => tradingSettings.value !== null && tradingSettings.value?.coinSymbols?.length > 0)
const hasTransactions = computed(() => recentTransactions.value?.length > 0)

// 상세 보기 다이얼로그 관련 변수
const showIndicatorDetailDialog = ref(false)
const selectedCoinDetail = ref<any>(null)

// AI 가중치 총합 (표시용)
const totalAiWeight = ref(0)

// 봇 상태 자동 새로고침 타이머
const botStatusTimer = ref<number | null>(null)

// 최신 릴리즈 노트 상태 추가
const latestReleaseNote = ref<any>(null)

// 최신 릴리즈 노트 조회 함수 추가
const fetchLatestReleaseNote = async () => {
  try {
    const response = await api.get('/release-notes/latest')
    latestReleaseNote.value = response.data
  } catch (error) {
    console.error('최신 릴리즈 노트 조회 실패:', error)
  }
}

// 상세 보기 팝업 열기 함수
const openIndicatorDetail = (coin: any) => {
  selectedCoinDetail.value = coin
  showIndicatorDetailDialog.value = true
}

// 실시간 시간 표시 
const currentTime = ref('')
let timeInterval: number | null = null

// 실시간 카운트다운
const countdownSeconds = ref(0)
let countdownInterval: number | null = null

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
  fetchAssetHistory()
  showSnackbar(`${customStartDate.value} ~ ${customEndDate.value} 기간 적용`, 'success')
}

// ★★★ [추가] 날짜 입력 필드 변경 시 자동 적용 ★★★
const applyCustomDateRange = () => {
  if (customStartDate.value && customEndDate.value) {
    chartPeriod.value = 'custom'
    fetchAssetHistory()
  }
}

// 투자기간 계산
const investmentPeriod = ref('0일')

const dashboardStats = ref({ totalProfitLoss: 0, totalProfitLossPct: 0, totalEvaluation: 0, totalInvestment: 0, todayBuyCount: 0, todayBuyAmount: 0, todaySellCount: 0, todaySellAmount: 0 })
const upbitAccount = ref({ krwBalance: 0, coinEvaluation: 0, totalAsset: 0, holdings: [] as any[] })

// ⭐⭐⭐ [신규] 포트폴리오 파이차트 데이터 ⭐⭐⭐
const hoveredSlice = ref(-1)
const portfolioColors = ['#8BC34A', '#5C6BC0', '#AB47BC', '#FF7043', '#26A69A', '#FFA726', '#42A5F5', '#EC407A']

const portfolioLegend = computed(() => {
  const total = upbitAccount.value.totalAsset
  if (total <= 0) return []
  const items: Array<{ label: string, percent: number, color: string, value: number }> = []
  // KRW
  items.push({ label: 'KRW', percent: (upbitAccount.value.krwBalance / total) * 100, color: portfolioColors[0], value: upbitAccount.value.krwBalance })
  // 보유 코인
  upbitAccount.value.holdings.forEach((h: any, i: number) => {
    items.push({ label: h.currency, percent: (h.evaluation / total) * 100, color: portfolioColors[(i + 1) % portfolioColors.length], value: h.evaluation })
  })
  return items
})

// 3D 타원 파이차트 상면
const portfolio3dSlices = computed(() => {
  const items = portfolioLegend.value
  if (!items.length) return []
  const cx = 130, cy = 90, rx = 85, ry = 68
  let startAngle = -90
  return items.map((item) => {
    const angle = (item.percent / 100) * 360
    const endAngle = startAngle + angle
    const startRad = (startAngle * Math.PI) / 180
    const endRad = (endAngle * Math.PI) / 180
    const x1 = cx + rx * Math.cos(startRad)
    const y1 = cy + ry * Math.sin(startRad)
    const x2 = cx + rx * Math.cos(endRad)
    const y2 = cy + ry * Math.sin(endRad)
    const largeArc = angle > 180 ? 1 : 0
    const path = `M ${cx} ${cy} L ${x1} ${y1} A ${rx} ${ry} 0 ${largeArc} 1 ${x2} ${y2} Z`
    // % 라벨 위치 (조각 중앙)
    const midAngle = ((startAngle + endAngle) / 2) * Math.PI / 180
    const labelR = (rx + 45) / 2  // 도넛 중간
    const labelX = cx + labelR * 0.85 * Math.cos(midAngle)
    const labelY = cy + labelR * 0.7 * Math.sin(midAngle)
    startAngle = endAngle
    return { path, color: item.color, label: item.label, percent: item.percent, labelX, labelY }
  })
})

// 3D 측면 (두께) - 아래쪽 반원만 렌더링
const portfolio3dSides = computed(() => {
  const items = portfolioLegend.value
  if (!items.length) return []
  const cx = 130, cy = 90, rx = 85, ry = 68, depth = 15
  let startAngle = -90
  return items.map((item) => {
    const angle = (item.percent / 100) * 360
    const endAngle = startAngle + angle
    // 측면은 0~180도 범위만 보임 (아래쪽)
    const visStart = Math.max(startAngle, 0)
    const visEnd = Math.min(endAngle, 180)
    let sidePath = ''
    if (visStart < visEnd) {
      const s1 = (visStart * Math.PI) / 180
      const s2 = (visEnd * Math.PI) / 180
      const ax1 = cx + rx * Math.cos(s1)
      const ay1 = cy + ry * Math.sin(s1)
      const ax2 = cx + rx * Math.cos(s2)
      const ay2 = cy + ry * Math.sin(s2)
      const largeArc = (visEnd - visStart) > 180 ? 1 : 0
      sidePath = `M ${ax1} ${ay1} A ${rx} ${ry} 0 ${largeArc} 1 ${ax2} ${ay2} L ${ax2} ${ay2 + depth} A ${rx} ${ry} 0 ${largeArc} 0 ${ax1} ${ay1 + depth} Z`
    }
    // 어두운 색상 생성
    const darkColor = darkenColor(item.color, 0.35)
    startAngle = endAngle
    return { sidePath, darkColor }
  }).filter(s => s.sidePath)
})

// 색상 어둡게 만드는 헬퍼
function darkenColor(hex: string, factor: number): string {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgb(${Math.floor(r * (1 - factor))}, ${Math.floor(g * (1 - factor))}, ${Math.floor(b * (1 - factor))})`
}

// botStatus ref 초기값에 새 필드 추가
const botStatus = ref({
  isRunning: false,
  lastExecuted: '',
  nextExecution: '',
  apiConnected: false,
  emergencyStop: false,
  isMaintenanceTime: false,    
  secondsUntilNext: 0          
})
const botEnabled = ref(true)  // 봇 활성화 상태
const botToggleLoading = ref(false)  // 스위치 로딩 상태

const tradingSettings = ref<any>(null)
const dailyLimit = ref({ totalLimit: 0, usedAmount: 0, remainingAmount: 0, usedPercent: 0 })
const holdingsPerCoin = ref<Record<string, number>>({})
const coinIndicators = ref<any[]>([])
const profitSummary = ref({ unrealizedProfit: 0, unrealizedProfitPct: 0, realizedProfit: 0, totalProfit: 0 })
const todayProfit = ref(0)
const todayProfitPct = ref(0)
const systemAlerts = ref<any[]>([])
const chartPeriod = ref('all') 
const assetHistory = ref<any[]>([])
const hoveredIndex = ref(-1)
const tooltipX = ref(0)
const dashboardChartViewMode = ref<'full' | 'scroll'>('full')
const showEmptyTooltip = ref(false)
const chartWrapperWidth = ref(800) // 실제 차트 wrapper 너비
const tooltipY = ref(0) // 툴팁 Y 위치
const pointX = ref(0) // 호버된 점의 실제 X 좌표
const initialAsset = ref(0)
const coinPerformance = ref<any[]>([])
const recentTransactions = ref<any[]>([])
const snackbar = ref({ show: false, message: '', color: 'success' })

const svgWidth = 800, svgHeight = 200, svgPadding = 30
const svgPaddingRight = 120
const scrollPaddingRight = 220

// HoldingsView 스타일 동적 차트 너비 계산
const effectiveWidth = computed(() => dashboardChartViewMode.value === 'scroll' ? dynamicChartWidth.value : svgWidth)

const dynamicChartWidth = computed(() => {
  const pointCount = assetHistory.value.length || emptyPeriodDates.value.length
  return Math.max(svgWidth, pointCount * 25 + svgPadding + scrollPaddingRight)
})

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

// 백테스팅 스타일 차트용 computed 
const chartHeight = 280
const maxBalanceChart = computed(() => assetHistory.value.length ? Math.max(...assetHistory.value.map(d => d.balance), initialAsset.value) : initialAsset.value)
const minBalanceChart = computed(() => assetHistory.value.length ? Math.min(...assetHistory.value.map(d => d.balance), initialAsset.value) : initialAsset.value)

const getYPositionChart = (balance: number) => {
  const range = maxBalanceChart.value - minBalanceChart.value || 1
  return svgPadding + ((maxBalanceChart.value - balance) / range) * (chartHeight - svgPadding * 2)
}

const chartPointsEnhanced = computed(() => {
  if (!assetHistory.value.length) return []
  const total = assetHistory.value.length
  return assetHistory.value.map((d, index) => ({
    x: svgPadding + (index / (total - 1 || 1)) * (svgWidth - svgPadding * 2),
    y: getYPositionChart(d.balance),
    balance: d.balance
  }))
})

const linePathChart = computed(() => {
  if (!chartPointsEnhanced.value.length) return ''
  return chartPointsEnhanced.value.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ')
})

const areaPathChart = computed(() => {
  if (!chartPointsEnhanced.value.length) return ''
  const points = chartPointsEnhanced.value
  return `M ${points[0].x} ${chartHeight - svgPadding} L ${points.map(p => `${p.x} ${p.y}`).join(' L ')} L ${points[points.length - 1].x} ${chartHeight - svgPadding} Z`
})

const chartStartDate = computed(() => {
  if (!assetHistory.value.length) return ''
  return assetHistory.value[0]?.date || ''
})

const chartEndDate = computed(() => {
  if (!assetHistory.value.length) return ''
  return assetHistory.value[assetHistory.value.length - 1]?.date || ''
})

// 기간 선택에 따른 시작일 계산
const chartPeriodStartDate = computed(() => {
  const today = new Date()
  let startDate = new Date()
  
  switch (chartPeriod.value) {
    case '7':
      startDate.setDate(today.getDate() - 7)
      break
    case 'month':
      startDate = new Date(today.getFullYear(), today.getMonth(), 1)
      break
    case 'year':
      startDate = new Date(today.getFullYear(), 0, 1)
      break
    case 'all':
      // 첫 거래일 또는 1년 전
      if (recentTransactions.value.length > 0) {
        const sorted = [...recentTransactions.value].sort((a, b) => 
          new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
        )
        startDate = new Date(sorted[0].createdAt)
      } else {
        startDate.setFullYear(today.getFullYear() - 1)
      }
      break
    case 'custom':
      return customStartDate.value || formatTodayDate()
    default:
      startDate.setDate(today.getDate() - 30)
  }
  
  return `${startDate.getFullYear()}-${String(startDate.getMonth() + 1).padStart(2, '0')}-${String(startDate.getDate()).padStart(2, '0')}`
})

const firstTradeDate = computed(() => {
  if (recentTransactions.value.length === 0) return null
  const sorted = [...recentTransactions.value].sort((a, b) => 
    new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
  )
  return sorted[0]?.createdAt ? new Date(sorted[0].createdAt) : null
})

// 선택한 기간이 첫 투자일 이전인지 확인
const isBeforeFirstTrade = computed(() => {
  if (!firstTradeDate.value) return false
  
  const periodStart = new Date(chartPeriodStartDate.value)
  return periodStart < firstTradeDate.value
})

// 표시할 시작일 (투자 시작일 기준)
const displayChartStartDate = computed(() => {
  if (assetHistory.value.length > 0) {
    return chartStartDate.value
  }
  
  // 빈 기간 데이터가 있으면 그 첫 번째 날짜 사용
  if (emptyPeriodDates.value.length > 0) {
    return emptyPeriodDates.value[0].date
  }
  
  // 첫 투자일이 있으면 사용
  if (firstTradeDate.value) {
    const d = firstTradeDate.value
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  }
  
  return formatTodayDate()
})

// 기간 내 모든 날짜 데이터 생성 (투자 시작일 이후만)
const emptyPeriodDates = computed(() => {
  // 오늘 날짜를 명시적으로 계산 (시간 제거)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  
  // 투자 시작일이 있으면 그 날짜부터, 없으면 오늘만
  let startDate: Date
  if (firstTradeDate.value) {
    // 선택한 기간 시작일과 첫 투자일 중 더 늦은 날짜 사용
    const periodStart = new Date(chartPeriodStartDate.value)
    periodStart.setHours(0, 0, 0, 0)
    
    const firstTrade = new Date(firstTradeDate.value)
    firstTrade.setHours(0, 0, 0, 0)
    
    startDate = periodStart > firstTrade ? new Date(periodStart) : new Date(firstTrade)
  } else {
    // 투자 이력이 없으면 오늘 하루만
    startDate = new Date(today)
  }
  
  const endDate = new Date(today)
  const dates: Array<{ date: string, balance: number, profitRate: number, hasData: boolean }> = []
  
  // 새로운 Date 객체로 복사하여 루프
  const currentDate = new Date(startDate.getTime())
  while (currentDate <= endDate) {
    dates.push({
      date: `${currentDate.getFullYear()}-${String(currentDate.getMonth() + 1).padStart(2, '0')}-${String(currentDate.getDate()).padStart(2, '0')}`,
      balance: upbitAccount.value.totalAsset || initialAsset.value,
      profitRate: 0,
      hasData: false
    })
    currentDate.setDate(currentDate.getDate() + 1)
  }
  
  return dates
})

// 스크롤 모드 지원 - effectiveWidth 사용
const emptyChartPoints = computed(() => {
  if (!emptyPeriodDates.value.length) return []
  const total = emptyPeriodDates.value.length
  const rightPad = dashboardChartViewMode.value === 'scroll' ? scrollPaddingRight : svgPaddingRight
  const chartWidth = effectiveWidth.value - svgPadding - rightPad
  return emptyPeriodDates.value.map((d, index) => ({
    x: svgPadding + (index / (total - 1 || 1)) * chartWidth,
    y: svgHeightBacktest / 2,
    date: d.date,
    balance: d.balance,
    hasData: d.hasData
  }))
})

//  빈 차트 호버 데이터
const emptyHoveredData = computed(() => {
  if (hoveredIndex.value < 0 || !emptyPeriodDates.value.length) return null
  return emptyPeriodDates.value[hoveredIndex.value] || null
})


// 오늘 날짜 포맷 함수
const formatTodayDate = () => {
  const today = new Date()
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
}

// ⭐ Day 31 추가: 오늘 수익 조회
const fetchTodayProfit = async () => {
  try {
    const response = await api.get('/profit/summary')
    const data = response.data?.data || response.data
    todayProfit.value = data?.todayProfit || 0
    todayProfitPct.value = data?.todayProfitPct || 0
  } catch (error) {
    console.error('오늘 수익 조회 실패:', error)
  }
}


// 백테스팅 스타일 차트용 상수 및 computed 
const svgHeightBacktest = 350

// ⭐⭐⭐ [변경] Y축 범위: 평가금액과 불입금액 모두 포함 ⭐⭐⭐
// 왜: 불입금액 막대그래프가 추가되어 Y축에 불입금액도 포함해야 차트가 정확함
const maxBalanceBacktest = computed(() => {
  if (!assetHistory.value.length) return initialAsset.value
  const maxEval = Math.max(...assetHistory.value.map(d => d.evaluationAmount || d.balance))
  const maxDeposit = Math.max(...assetHistory.value.map(d => d.depositAmount || initialAsset.value))
  return Math.max(maxEval, maxDeposit)
})

const minBalanceBacktest = computed(() => {
  if (!assetHistory.value.length) return 0
  const minEval = Math.min(...assetHistory.value.map(d => d.evaluationAmount || d.balance))
  const minDeposit = Math.min(...assetHistory.value.map(d => d.depositAmount || initialAsset.value))
  const minValue = Math.min(minEval, minDeposit)
  if (minValue <= 0) return 0
  return Math.floor(minValue * 0.98)
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

const getYPositionBacktest = (balance: number) => {
  const max = maxBalanceBacktest.value
  const min = minBalanceBacktest.value
  const range = max - min || 1
  return svgPadding + ((max - balance) / range) * (svgHeightBacktest - svgPadding * 2)
}

// 스크롤 모드 지원 - effectiveWidth 사용
// ⭐⭐⭐ [변경] evaluationAmount, depositAmount 포함 ⭐⭐⭐
// 왜: SVG에서 막대그래프(depositAmount)와 데이터포인트(evaluationAmount) 렌더링에 필요
const chartPointsBacktest = computed(() => {
  if (!assetHistory.value.length) return []
  const total = assetHistory.value.length
  const rightPad = dashboardChartViewMode.value === 'scroll' ? scrollPaddingRight : svgPaddingRight
  const chartWidth = effectiveWidth.value - svgPadding - rightPad
  return assetHistory.value.map((d, index) => ({
    x: svgPadding + (index / (total - 1 || 1)) * chartWidth,
    y: getYPositionBacktest(d.evaluationAmount || d.balance),
    balance: d.evaluationAmount || d.balance || 0,
    evaluationAmount: d.evaluationAmount || d.balance || 0,
    depositAmount: d.depositAmount || initialAsset.value
  }))
})

const linePathBacktest = computed(() => {
  if (!chartPointsBacktest.value.length) return ''
  return chartPointsBacktest.value
    .map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`)
    .join(' ')
})

// ⭐⭐⭐ [신규 추가] 불입금액 추세선 path ⭐⭐⭐
// 왜: SVG에서 불입금액 추세선(주황 파선)을 그리기 위한 path 데이터
// ⭐⭐⭐ [변경] 불입금액 추세선도 막대 상단 위치와 일치 ⭐⭐⭐
const depositLinePathBacktest = computed(() => {
  if (!chartPointsBacktest.value.length) return ''
  return chartPointsBacktest.value
    .map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${getYPositionBacktest(p.depositAmount)}`)
    .join(' ')
})

const areaPathBacktest = computed(() => {
  if (!chartPointsBacktest.value.length) return ''
  const points = chartPointsBacktest.value
  const firstX = points[0].x
  const lastX = points[points.length - 1].x
  const bottomY = svgHeightBacktest - svgPadding
  
  return `M ${firstX} ${bottomY} L ${points.map(p => `${p.x} ${p.y}`).join(' L ')} L ${lastX} ${bottomY} Z`
})

// ★★★ [수정] 여유 공간 제거한 실제 값으로 라벨 위치 계산 ★★★
// ⭐⭐⭐ [변경] 불입금액 포함 라벨 위치 계산 ⭐⭐⭐
// 왜: 라벨 4개(최고, 평가, 불입, 최저)가 Y축 범위 내에서 정확한 위치에 표시되어야 함
const getLabelPositionBacktest = (balance: number) => {
  const max = maxBalanceBacktest.value
  const min = minBalanceBacktest.value
  const range = max - min || 1
  const paddingPercent = (svgPadding / svgHeightBacktest) * 100
  const usableHeight = 100 - paddingPercent * 2
  return paddingPercent + ((max - balance) / range) * usableHeight
}

// ⭐⭐⭐ [신규 추가] 라벨 겹침 방지 위치 계산 ⭐⭐⭐
// 왜: 금액이 같거나 근접하면 라벨이 겹쳐서 안 보임. 최소 간격(4%) 보장
const getAdjustedLabelPosition = (type: string) => {
  const positions = [
    { type: 'max', value: maxEvaluation.value, raw: getLabelPositionBacktest(maxEvaluation.value) },
    { type: 'evaluation', value: latestEvaluationAmount.value, raw: getLabelPositionBacktest(latestEvaluationAmount.value) },
    { type: 'deposit', value: latestDepositAmount.value, raw: getLabelPositionBacktest(latestDepositAmount.value) },
    { type: 'min', value: minEvaluation.value, raw: getLabelPositionBacktest(minEvaluation.value) },
    { type: 'floor', value: minBalanceBacktest.value, raw: getLabelPositionBacktest(minBalanceBacktest.value) }
  ]

  // raw 위치 기준 오름차순 정렬 (위→아래)
  positions.sort((a, b) => a.raw - b.raw)

  // 최소 간격 적용 (4%)
  const minGap = 4
  for (let i = 1; i < positions.length; i++) {
    if (positions[i].raw - positions[i - 1].raw < minGap) {
      positions[i].raw = positions[i - 1].raw + minGap
    }
  }

  // 요청된 type의 조정된 위치 반환
  const found = positions.find(p => p.type === type)
  return found ? found.raw : 0
}

// ⭐⭐⭐ [변경] 불입금액 기준으로 색상 판단 ⭐⭐⭐
// 왜: 초기자산이 아닌 실제 불입금액 대비 수익/손실 색상 표시
const getPointColorBacktest = (evaluationAmount: number) => {
  const deposit = latestDepositAmount.value
  if (evaluationAmount > deposit * 1.01) return '#4CAF50'
  if (evaluationAmount < deposit * 0.99) return '#F44336'
  return '#1976D2'
}









const formatCurrency = (value: number) => {
  if (value === undefined || value === null) return '₩0'
  return '₩' + value.toLocaleString('ko-KR', { maximumFractionDigits: 0 })
}
// 간결한 가격 포맷 함수
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

// 짧은 날짜 포맷 (MM/DD HH:mm)
const formatDateShort = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

// 마지막 로그인 포맷 함수 - 우상단 현재 시간과 동일한 방식
const formatLastLogin = (dateStr: string | null | undefined) => {
  if (!dateStr) return '-'
  
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return '-'
  
  // 우상단 시간과 동일한 toLocaleString('ko-KR') 방식 사용
  const dateOptions: Intl.DateTimeFormatOptions = { 
    year: 'numeric', 
    month: '2-digit', 
    day: '2-digit' 
  }
  const timeOptions: Intl.DateTimeFormatOptions = { 
    hour: '2-digit', 
    minute: '2-digit', 
    second: '2-digit',
    hour12: true
  }
  
  const datePart = date.toLocaleDateString('ko-KR', dateOptions)
  const timePart = date.toLocaleTimeString('ko-KR', timeOptions)
  
  return `${datePart} ${timePart}`
}


// 봇 수행 시간 포맷 함수 - 우상단 현재 시간과 동일한 방식
const formatBotTimeDisplay = (dateStr: string | null | undefined) => {
  if (!dateStr || dateStr === '-') return '-'
  
  // 이미 HH:mm 형식인 경우 (예: "12:05") - 오늘 날짜 붙이기
  if (/^\d{2}:\d{2}$/.test(dateStr)) {
    const now = new Date()
    const dateOptions: Intl.DateTimeFormatOptions = { year: 'numeric', month: '2-digit', day: '2-digit' }
    const datePart = now.toLocaleDateString('ko-KR', dateOptions).replace(/\. /g, '-').replace('.', '')
    return `${datePart} ${dateStr}`
  }
  
  // ISO 형식 또는 전체 날짜 형식인 경우
  try {
    const d = new Date(dateStr)
    if (isNaN(d.getTime())) return dateStr
    
    // 우상단 시간과 동일한 toLocaleString('ko-KR') 방식 사용
    const dateOptions: Intl.DateTimeFormatOptions = { year: 'numeric', month: '2-digit', day: '2-digit' }
    const timeOptions: Intl.DateTimeFormatOptions = { hour: '2-digit', minute: '2-digit', hour12: false }
    
    const datePart = d.toLocaleDateString('ko-KR', dateOptions).replace(/\. /g, '-').replace('.', '')
    const timePart = d.toLocaleTimeString('ko-KR', timeOptions)
    
    return `${datePart} ${timePart}`
  } catch {
    return dateStr
  }
}

// 카운트다운 시작 함수
const startCountdown = (seconds: number) => {
  countdownSeconds.value = seconds
  
  if (countdownInterval) {
    clearInterval(countdownInterval)
  }
  
  countdownInterval = window.setInterval(() => {
    if (countdownSeconds.value > 0) {
      countdownSeconds.value--
    } else {
      if (countdownInterval) {
        clearInterval(countdownInterval)
        countdownInterval = null
      }
      // 카운트다운 종료 시 봇 상태 새로고침
      fetchBotStatus()
      fetchIndicators()
    }
  }, 1000)
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
  const wrapper = event.currentTarget as HTMLElement
  const rect = wrapper.getBoundingClientRect()
  
  // 마우스 위치 (wrapper 내 상대 좌표)
  const mouseX = event.clientX - rect.left
  
  // 스크롤 모드 확인
  const isScrollMode = dashboardChartViewMode.value === 'scroll'
  
  // ★★★ [핵심 수정] 스크롤 모드에서의 계산 방식 완전 변경 ★★★
  let targetIndex = 0
  const total = assetHistory.value.length
  
  if (total === 0) {
    const emptyTotal = emptyPeriodDates.value.length
    if (emptyTotal > 0) {
      if (isScrollMode) {
        // 스크롤 모드: SVG의 실제 너비 기준으로 계산
        const svgElement = wrapper.querySelector('svg')
        if (svgElement) {
          const svgRect = svgElement.getBoundingClientRect()
          const svgMouseX = event.clientX - svgRect.left
          const chartWidth = effectiveWidth.value - svgPadding - svgPaddingRight
          const ratio = Math.max(0, Math.min(1, (svgMouseX - svgPadding * (svgRect.width / effectiveWidth.value)) / (chartWidth * (svgRect.width / effectiveWidth.value))))
          targetIndex = Math.round(ratio * (emptyTotal - 1))
        }
      } else {
        const chartWidth = effectiveWidth.value - svgPadding - svgPaddingRight
        const ratio = Math.max(0, Math.min(1, (mouseX - svgPadding) / chartWidth))
        targetIndex = Math.round(ratio * (emptyTotal - 1))
      }
      hoveredIndex.value = Math.max(0, Math.min(emptyTotal - 1, targetIndex))
      tooltipY.value = svgHeightBacktest / 2
    }
    showEmptyTooltip.value = true
    tooltipX.value = mouseX
    chartWrapperWidth.value = rect.width
    return
  }
  
  showEmptyTooltip.value = false
  
  if (isScrollMode) {
    // ★★★ [핵심] 스크롤 모드: 각 점의 화면상 위치와 마우스 위치 비교 ★★★
    const svgElement = wrapper.querySelector('svg')
    if (svgElement) {
      const svgRect = svgElement.getBoundingClientRect()
      const scaleX = svgRect.width / effectiveWidth.value
      
      // 마우스와 가장 가까운 점 찾기
      let minDistance = Infinity
      let closestIndex = 0
      
      chartPointsBacktest.value.forEach((point, index) => {
        // 점의 화면상 X 위치 계산
        const pointScreenX = svgRect.left + (point.x * scaleX) - rect.left
        const distance = Math.abs(mouseX - pointScreenX)
        
        if (distance < minDistance) {
          minDistance = distance
          closestIndex = index
        }
      })
      
      targetIndex = closestIndex
    }
  } else {
    // 전체 보기 모드: 기존 방식
    const rightPad = dashboardChartViewMode.value === 'scroll' ? scrollPaddingRight : svgPaddingRight
    const chartWidth = effectiveWidth.value - svgPadding - rightPad
    const ratio = Math.max(0, Math.min(1, (mouseX - svgPadding * (rect.width / effectiveWidth.value)) / (chartWidth * (rect.width / effectiveWidth.value))))
    targetIndex = Math.round(ratio * (total - 1))
  }
  
  hoveredIndex.value = Math.max(0, Math.min(total - 1, targetIndex))
  
  // 호버된 점의 Y 위치
  if (chartPointsBacktest.value[hoveredIndex.value]) {
    tooltipY.value = chartPointsBacktest.value[hoveredIndex.value].y
  }
  
  // 툴팁 X 위치
  tooltipX.value = mouseX
  chartWrapperWidth.value = rect.width
}

// 점의 실제 위치에 툴팁 표시
const getTooltipPosition = () => {
  return pointX.value
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

const fetchDashboardStats = async () => { 
  try { 
    const r = await transactionApi.getStats()
    const data = r.data
    
    // 평가손익 (미실현) = 총 평가액 - 투자금
    const unrealizedProfit = parseFloat(data.totalProfitLoss) || 0
    const unrealizedProfitPct = parseFloat(data.totalProfitLossPct) || 0
    // 실현손익 (확정)
    const realizedProfit = parseFloat(data.realizedProfitLoss) || 0
    // 총 손익 = 평가손익 + 실현손익
    const totalProfit = unrealizedProfit + realizedProfit
    
    dashboardStats.value = {
      totalProfitLoss: totalProfit,
      totalProfitLossPct: unrealizedProfitPct,
      totalEvaluation: parseFloat(data.totalCurrentValue) || 0,
      totalInvestment: parseFloat(data.totalHoldingAmount) || 0,
      todayBuyCount: data.todayBuyCount || 0,
      todayBuyAmount: parseFloat(data.todayBuyAmount) || 0,
      todaySellCount: data.todaySellCount || 0,
      todaySellAmount: parseFloat(data.todaySellAmount) || 0
    }
    
    // 수익 현황 카드에도 값 반영
    profitSummary.value = {
      ...profitSummary.value,
      unrealizedProfit: unrealizedProfit,
      unrealizedProfitPct: unrealizedProfitPct,
      realizedProfit: realizedProfit,
      totalProfit: totalProfit
    }
  } catch (e) { console.error(e) } 
}

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


const fetchBotStatus = async () => {
  try {
    const r = await botApi.getStatus()
    botStatus.value = {
      isRunning: r.data?.isRunning || false,
      lastExecuted: r.data?.lastExecutionTime || r.data?.lastExecutedAt || '',
      nextExecution: r.data?.nextExecutionTime || r.data?.nextExecutionAt || '',
      apiConnected: r.data?.apiConnected ?? true,
      emergencyStop: r.data?.emergencyStop || false,
      isMaintenanceTime: r.data?.isMaintenanceTime || false,
      secondsUntilNext: r.data?.secondsUntilNextExecution || 0
    }

    botEnabled.value = r.data?.botEnabled ?? true 
    
    // 카운트다운 시작
    const seconds = r.data?.secondsUntilNextExecution || 0
    if (seconds > 0) {
      startCountdown(seconds)
    }
    
    // 기존 타이머 로직 제거 또는 주석 처리
    // if (botStatusTimer.value) { ... }
    
  } catch (e) {
    console.error(e)
    botStatus.value.apiConnected = false
  }
}

// 봇 활성/비활성 토글
const toggleBot = async () => {
  botToggleLoading.value = true
  try {
    // v-model이 이미 변경된 상태이므로, 새 값 기준으로 API 호출 
    if (botEnabled.value) {
      // 새 값이 true = 사용자가 켬 → start 호출
      await botApi.start()
    } else {
      // 새 값이 false = 사용자가 끔 → stop 호출
      await botApi.stop()
    }
    // 상태 재조회
    await fetchBotStatus()
  } catch (error) {
    console.error('봇 상태 변경 실패:', error)
    // ⭐⭐⭐ [추가] 실패 시 원래 값으로 복원 ⭐⭐⭐
    botEnabled.value = !botEnabled.value
  } finally {
    botToggleLoading.value = false
  }
}

const fetchTradingSettings = async () => {
  try {
    const r = await tradingApi.getSettings()
    tradingSettings.value = r.data

    if (tradingSettings.value) {
      // ⭐⭐⭐ [수정] 일일 한도를 백엔드 API에서 조회 (총자산 × dailyTradeLimitPct% 기준) ⭐⭐⭐
      try {
        const limitRes = await api.get('/risk/daily-limit')
        const limitData = limitRes.data
        dailyLimit.value = {
          totalLimit: limitData.totalLimit || 0,
          usedAmount: limitData.usedAmount || 0,
          remainingAmount: limitData.remainingAmount || 0,
          usedPercent: limitData.usedPercent || 0
        }
      } catch (limitErr) {
        console.error('일일 한도 API 조회 실패, 기존 방식 사용:', limitErr)
        // API 실패 시 기존 방식 폴백
        const tl = tradingSettings.value.dailyLimitAmount || 0
        const ua = dashboardStats.value.todayBuyAmount || 0
        dailyLimit.value = {
          totalLimit: tl,
          usedAmount: ua,
          remainingAmount: Math.max(0, tl - ua),
          usedPercent: tl > 0 ? (ua / tl) * 100 : 0
        }
      }
    }
  } catch (e) {
    console.error(e)
    tradingSettings.value = null
  }
}

const fetchIndicators = async () => {
  if (!tradingSettings.value?.coinSymbols?.length) return
  loadingIndicators.value = true
  try {
    const r = await botApi.getIndicators(tradingSettings.value.coinSymbols)
    const indicators = r.data || []
    const bt = tradingSettings.value.buyThresholdPct || -3
    
    // AI 가중치 조회 (사용자가 AI 분석 사용 시)
    let aiWeights: Record<string, number> = {}
    if (tradingSettings.value.useAiAnalysis) {
      try {
        for (const symbol of tradingSettings.value.coinSymbols) {
          const weightRes = await api.get(`/news/analysis/weight/${symbol}`)
          if (weightRes.data?.data?.weightAdjustment) {
            aiWeights[symbol] = parseFloat(weightRes.data.data.weightAdjustment)
          }
        }
        // 총 가중치 평균 계산
        const weights = Object.values(aiWeights)
        totalAiWeight.value = weights.length > 0 ? weights.reduce((a, b) => a + b, 0) / weights.length : 0
      } catch (err) {
        console.warn('AI 가중치 조회 실패:', err)
      }
    }
    
    coinIndicators.value = indicators.map((ind: any) => {
      const cp = ind.currentPrice || 0
      const ma = ind.ma20 || 0
      const dr = ma > 0 ? ((cp - ma) / ma) * 100 : 0
      const symbol = ind.market || ind.symbol
      
      // AI 가중치 적용
      const aiWeight = aiWeights[symbol] || 0
      const adjustedThreshold = bt + aiWeight  // 예: -6 + 0.3 = -5.7 (호재 시 완화)
      const aiAdjustedBuyPrice = ma * (1 + adjustedThreshold / 100)
      
      return {
        symbol: symbol,
        currentPrice: cp,
        ma20: ma,
        buyPrice: ma * (1 + bt / 100),  // 기본 매수가
        aiAdjustedBuyPrice: aiAdjustedBuyPrice,  // AI 적용 매수가
        dropRate: dr,
        canBuy: dr <= adjustedThreshold,  // AI 가중치 적용된 기준으로 판단
        remainingDrop: adjustedThreshold - dr,
        aiWeight: aiWeight  // AI 가중치 저장
      }
    })
  } catch (e) {
    console.error(e)
  } finally {
    loadingIndicators.value = false
  }
}


const fetchHoldings = async () => { try { const r = await transactionApi.getHoldings(); const h = r.data || []; const pc: Record<string, number> = {}; let up = 0, ti = 0; h.forEach((hh: any) => { const s = hh.coinSymbol?.replace('KRW-', '') || 'X'; pc[s] = (pc[s] || 0) + 1; up += hh.profitLoss || 0; ti += hh.totalAmount || 0 }); holdingsPerCoin.value = pc; profitSummary.value.unrealizedProfit = up; profitSummary.value.unrealizedProfitPct = ti > 0 ? (up / ti) * 100 : 0 } catch (e) { console.error(e) } }
const fetchRecentTransactions = async () => {
  try {
    // 모든 거래 조회 - HOLDING + SOLD 모두 포함
    // 1. 전체 거래 조회 (최신순)
    const allRes = await transactionApi.getAll({ page: 0, size: 50, sort: 'createdAt,desc' })
    const allTransactions = allRes.data?.content || []
    
    // 2. 매도 완료된 거래 별도 조회 (혹시 누락된 경우 대비)
    const soldRes = await transactionApi.search({ status: 'SOLD', page: 0, size: 20 })
    const soldTransactions = soldRes.data?.content || []
    
    // 3. 두 목록 합치고 중복 제거
    const combined = [...allTransactions, ...soldTransactions]
    const uniqueMap = new Map()
    combined.forEach(tx => {
      // id 또는 transactionId로 중복 체크
      const key = tx.id || tx.transactionId
      if (key && !uniqueMap.has(key)) {
        uniqueMap.set(key, tx)
      }
    })
    
    // 4. 최신순 정렬 (createdAt 또는 soldAt 기준)
    const uniqueList = Array.from(uniqueMap.values())
    uniqueList.sort((a, b) => {
      const dateA = new Date(a.soldAt || a.createdAt).getTime()
      const dateB = new Date(b.soldAt || b.createdAt).getTime()
      return dateB - dateA  // 최신순
    })
    
    // 5. 상위 10개만 표시
    recentTransactions.value = uniqueList.slice(0, 10)
    
    // 투자기간 계산
    if (uniqueList.length > 0) {
      const sortedByOldest = [...uniqueList].sort((a, b) => 
        new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
      )
      if (sortedByOldest.length > 0) {
        calculateInvestmentPeriod(sortedByOldest[0].createdAt)
      }
    }
    
    // 실현 수익 계산 (매도 완료된 거래만)
    const soldOnly = uniqueList.filter(tx => tx.status === 'SOLD')
    profitSummary.value.realizedProfit = soldOnly.reduce((sum: number, tx: any) => sum + (tx.profitLoss || 0), 0)
    profitSummary.value.totalProfit = profitSummary.value.unrealizedProfit + profitSummary.value.realizedProfit
    
    // 코인별 성과 계산
    const perfMap = new Map<string, any>()
    soldOnly.forEach((tx: any) => {
      const symbol = tx.coinSymbol?.replace('KRW-', '') || 'X'
      if (!perfMap.has(symbol)) {
        perfMap.set(symbol, { symbol, tradeCount: 0, profit: 0, totalAmount: 0 })
      }
      const perf = perfMap.get(symbol)
      perf.tradeCount++
      perf.profit += tx.profitLoss || 0
      perf.totalAmount += tx.totalAmount || 0
    })
    coinPerformance.value = Array.from(perfMap.values()).map(p => ({
      ...p,
      profitRate: p.totalAmount > 0 ? (p.profit / p.totalAmount) * 100 : 0
    }))
    
  } catch (e) {
    console.error('최근 거래 조회 실패:', e)
  }
}
// ⭐⭐⭐ [변경] 백엔드 스냅샷 API 우선 조회 + 기존 로직 폴백 ⭐⭐⭐
// 왜: 매일 23:59에 저장되는 스냅샷 데이터로 정확한 평가금액/불입금액 표시
// 스냅샷이 아직 없으면(배포 초기) 기존 SOLD 거래 기반 계산으로 폴백
const fetchAssetHistory = async () => {
  try {
    initialAsset.value = 1000000

    // ⭐ 1단계: 백엔드 스냅샷 API 우선 조회
    try {
      let snapshotResponse
      if (chartPeriod.value === 'custom' && customStartDate.value && customEndDate.value) {
        snapshotResponse = await profitApi.getAssetSnapshotsByRange(customStartDate.value, customEndDate.value)
      } else {
        snapshotResponse = await profitApi.getAssetSnapshots(chartPeriod.value)
      }

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
        return  // 스냅샷 데이터가 있으면 여기서 종료
      }
    } catch (snapshotError) {
      console.warn('스냅샷 API 조회 실패, 기존 방식으로 폴백:', snapshotError)
    }

    // ⭐ 2단계: 폴백 - 기존 SOLD 거래 기반 계산 (스냅샷 없을 때)
    const response = await transactionApi.search({ status: 'SOLD', page: 0, size: 1000 })
    const transactions = response.data?.content || []
    
    if (transactions.length === 0) {
      assetHistory.value = []
      return
    }
    
    const now = new Date()
    let startDate: Date
    let endDate: Date = now
    
    switch (chartPeriod.value) {
      case '7':
        startDate = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 7)
        break
      case 'month':
        startDate = new Date(now.getFullYear(), now.getMonth(), 1)
        break
      case 'year':
        startDate = new Date(now.getFullYear(), 0, 1)
        break
      case 'custom':
        startDate = customStartDate.value ? new Date(customStartDate.value) : new Date(2020, 0, 1)
        endDate = customEndDate.value ? new Date(customEndDate.value) : now
        break
      case 'all':
      default:
        startDate = new Date(2020, 0, 1)
        break
    }
    
    const filteredTxs = transactions.filter((tx: any) => {
      const txDate = new Date(tx.soldAt || tx.createdAt)
      return txDate >= startDate
    })
    
    if (filteredTxs.length === 0) {
      assetHistory.value = []
      return
    }
    
    const sortedTxs = [...filteredTxs].sort((a: any, b: any) => 
      new Date(a.soldAt || a.createdAt).getTime() - new Date(b.soldAt || b.createdAt).getTime()
    )
    
    const dailyBalanceMap = new Map<string, number>()
    let runningBalance = initialAsset.value
    
    sortedTxs.forEach((tx: any) => {
      const dateKey = new Date(tx.soldAt || tx.createdAt).toISOString().split('T')[0]
      if (tx.profitLoss) {
        runningBalance += tx.profitLoss
      }
      dailyBalanceMap.set(dateKey, runningBalance)
    })
    
    const sortedDates = Array.from(dailyBalanceMap.keys()).sort()
    const firstDate = new Date(sortedDates[0])
    firstDate.setHours(0, 0, 0, 0)
    
    const nowDate = new Date()
    const today = new Date(nowDate.getFullYear(), nowDate.getMonth(), nowDate.getDate())
    
    let lastDate: Date
    if (chartPeriod.value === 'custom' && customEndDate.value) {
      lastDate = new Date(customEndDate.value)
    } else {
      lastDate = new Date(today.getTime())
    }
    
    const allDates: Array<any> = []
    let currentBalance = initialAsset.value
    const currentDate = new Date(firstDate)
    currentDate.setHours(12, 0, 0, 0)
    lastDate.setHours(12, 0, 0, 0)
    
    const lastDateStr = `${lastDate.getFullYear()}-${String(lastDate.getMonth() + 1).padStart(2, '0')}-${String(lastDate.getDate()).padStart(2, '0')}`
    
    while (true) {
      const dateKey = `${currentDate.getFullYear()}-${String(currentDate.getMonth() + 1).padStart(2, '0')}-${String(currentDate.getDate()).padStart(2, '0')}`
      
      if (dailyBalanceMap.has(dateKey)) {
        currentBalance = dailyBalanceMap.get(dateKey)!
      }
      
      // ⭐ 폴백에서도 evaluationAmount/depositAmount 필드 포함
      allDates.push({
        date: dateKey,
        balance: currentBalance,
        evaluationAmount: currentBalance,
        depositAmount: initialAsset.value,
        profitAmount: currentBalance - initialAsset.value,
        profitRate: ((currentBalance - initialAsset.value) / initialAsset.value) * 100
      })
      
      if (dateKey >= lastDateStr) {
        break
      }
      
      currentDate.setDate(currentDate.getDate() + 1)
    }
    
    assetHistory.value = allDates
    
  } catch (e) {
    console.error('자산 이력 생성 실패:', e)
    assetHistory.value = []
  }
}
const generateSystemAlerts = () => { const a: any[] = []; if (!authStore.user?.hasApiKey) a.push({ type: 'warning', message: 'API 키가 미등록 상태입니다' }); if (!tradingSettings.value) a.push({ type: 'warning', message: '거래 설정을 완료해주세요' }); const n = new Date(); if (n.getHours() === 9 && n.getMinutes() < 10) a.push({ type: 'info', message: '업비트 점검 시간 (09:00~09:10)' }); if (botStatus.value.emergencyStop) a.push({ type: 'error', message: '긴급 정지 발동됨' }); systemAlerts.value = a }
const refreshAll = async () => { 
    isRefreshing.value = true; 
    try { 
        await Promise.all([fetchDashboardStats(), fetchUpbitAccount(), fetchBotStatus(), fetchTradingSettings()]); 
        await Promise.all([fetchIndicators(), fetchHoldings(), fetchRecentTransactions()]); 
        generateSystemAlerts(); 
        fetchLatestReleaseNote()
        showSnackbar('새로고침 완료', 'success') 
    } 
    finally { 
        isRefreshing.value = false 
    } 
}

// ★★★ [추가] 기간 변경 시 자산 이력 다시 로드 ★★★
watch(chartPeriod, async () => {
  await fetchAssetHistory()
})

let refreshInterval: number | null = null
const startAutoRefresh = () => { refreshInterval = window.setInterval(() => { fetchDashboardStats(); fetchUpbitAccount(); fetchBotStatus() }, 60000) }
const stopAutoRefresh = () => { if (refreshInterval) { clearInterval(refreshInterval); refreshInterval = null } }

// 카드별 도움말 내용
const cardHelps = {
  userStats: {
    title: '👤 사용자 정보 + 통계',
    content: `
      <p class="help-intro">사용자 계정 정보와 투자 현황 통계를 한눈에 보여줍니다.</p>
      <div style="height: 16px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>총 손익</strong><br/>
        <span class="help-desc">현재 보유 중인 코인의 평가 손익 + 매도 완료된 실현 손익의 합계입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>총 평가액</strong><br/>
        <span class="help-desc">보유 코인의 현재 시세 기준 평가 금액입니다. 원금은 매수 당시 투자한 금액입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>오늘 매수/매도</strong><br/>
        <span class="help-desc">오늘 하루 동안 봇이 자동으로 체결한 매수/매도 건수와 금액입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>투자기간</strong><br/>
        <span class="help-desc">첫 거래일부터 현재까지의 기간입니다.</span></p>
    `
  },
  botStatus: {
    title: '🤖 자동매매 봇 상태',
    content: `
      <p class="help-intro">자동매매 봇의 현재 상태와 실행 스케줄을 보여줍니다.</p>
      <div style="height: 16px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>작동중</strong><br/>
        <span class="help-desc">봇이 5분마다 매수/매도 조건을 자동으로 검사하고 있습니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>대기중</strong><br/>
        <span class="help-desc">봇이 일시 정지 상태입니다. 거래 설정이 없거나 점검 시간일 수 있습니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>마지막/다음 봇 수행 시간</strong><br/>
        <span class="help-desc">봇의 가장 최근 실행 시간과 다음 예정 시간입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>긴급정지</strong><br/>
        <span class="help-desc">당일 손실이 일정 수준 이상이면 자동으로 거래가 중단됩니다.</span></p>
    `
  },
  upbitBalance: {
    title: '🏦 업비트 실제 잔고',
    content: `
      <p class="help-intro">업비트 계좌의 실제 자산 현황을 실시간으로 조회합니다.</p>
      <div style="height: 16px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>보유자산 포트폴리오</strong><br/>
        <span class="help-desc">KRW와 보유 코인의 비중을 파이차트로 시각화합니다.<br/>각 자산의 비율(%)을 한눈에 확인할 수 있습니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>KRW 잔고</strong><br/>
        <span class="help-desc">업비트 계좌의 원화 보유량입니다. 매수 대기 자금으로 사용됩니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>코인 평가액</strong><br/>
        <span class="help-desc">보유 중인 모든 코인의 현재 시세 기준 평가 금액 합계입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>총 자산</strong><br/>
        <span class="help-desc">KRW 잔고 + 코인 평가액의 합계입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>보유 코인</strong><br/>
        <span class="help-desc">현재 보유 중인 코인 종류와 평가액, 수익률을 표시합니다.</span></p>
      <p class="help-note">※ 업비트 API 키가 등록되어 있어야 조회됩니다.</p>
    `
  },
  tradingSettings: {
    title: '⚙️ 거래 설정 요약',
    content: `
      <p class="help-intro">현재 설정된 자동매매 조건을 요약하여 보여줍니다.</p>
      <div style="height: 16px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>거래 코인</strong><br/>
        <span class="help-desc">자동매매 대상으로 설정된 코인 목록입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>매수 조건</strong><br/>
        <span class="help-desc">이동평균선(MA) 대비 하락률과 목표 수익률 설정입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>매도 조건</strong><br/>
        <span class="help-desc">목표 수익률 도달 시 자동 매도, 손절매 기준입니다.</span></p>
      <p class="help-note">설정이 없으면 [설정하기] 버튼을 눌러 거래 조건을 설정해주세요.</p>
    `
  },
  dailyLimit: {
    title: '📊 일일 한도',
    content: `
      <p class="help-intro">오늘 하루 동안의 거래 한도 사용 현황을 보여줍니다.</p>
      <div style="height: 16px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>한도</strong><br/>
        <span class="help-desc">하루 동안 사용할 수 있는 최대 거래 금액입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>사용</strong><br/>
        <span class="help-desc">오늘 이미 매수에 사용한 금액입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>남은 한도</strong><br/>
        <span class="help-desc">오늘 남은 거래 가능 금액입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>종목별 보유</strong><br/>
        <span class="help-desc">각 코인별 현재 보유 건수입니다. 최대 보유 건수에 도달하면 추가 매수가 제한됩니다.</span></p>
    `
  },
  buyCondition: {
    title: '🎯 매수 조건',
    content: `
      <p class="help-intro">각 코인별 매수 조건 충족 여부를 실시간으로 모니터링합니다.</p>
      <div style="height: 16px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>현재가 → 목표가</strong><br/>
        <span class="help-desc">현재 시세와 매수 목표가를 보여줍니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>이격도</strong><br/>
        <span class="help-desc">현재가가 이동평균선(MA) 대비 얼마나 떨어져 있는지 나타냅니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>대기</strong><br/>
        <span class="help-desc">아직 매수 조건에 도달하지 않은 상태입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>매수 가능</strong><br/>
        <span class="help-desc">이격도가 설정된 하락률 이하로 내려가면 매수 신호가 발생합니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>AI 가중치</strong><br/>
        <span class="help-desc">AI 뉴스 분석 사용 시 호재/악재에 따라 ±0.5% 범위에서 매수 조건이 조정됩니다.</span></p>
    `
  },
  profitSummary: {
    title: '💰 수익 현황',
    content: `
      <p class="help-intro">전체 투자 수익을 미실현/실현으로 구분하여 보여줍니다.</p>
      <div style="height: 16px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>평가 수익 (미실현)</strong><br/>
        <span class="help-desc">현재 보유 중인 코인의 매수가 대비 평가 손익입니다. 매도 전까지는 확정되지 않은 수익입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>실현 수익 (확정)</strong><br/>
        <span class="help-desc">매도 완료된 거래의 실제 손익 합계입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>누적 총 수익</strong><br/>
        <span class="help-desc">평가 수익 + 실현 수익의 합계입니다.</span></p>
    `
  },
  assetChart: {
    title: '📈 자산 변동 추이',
    content: `
      <p class="help-intro">기간별 총 자산의 변동 추이를 차트로 보여줍니다.</p>
      <div style="height: 16px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>7일/이번달/올해/전체</strong><br/>
        <span class="help-desc">원하는 기간을 선택하여 조회할 수 있습니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>차트 호버</strong><br/>
        <span class="help-desc">차트의 점을 호버하면 해당 일자의 자산과 수익률을 확인할 수 있습니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>기록 시작일</strong><br/>
        <span class="help-desc">자산 변동 추이는 최초 거래일로부터 기록이 시작됩니다. 거래 이전 기간은 조회되지 않습니다.</span></p>
    `
  },
  coinPerformance: {
    title: '🏆 코인별 성과',
    content: `
      <p class="help-intro">매도 완료된 거래 기준으로 코인별 성과를 보여줍니다.</p>
      <div style="height: 16px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>거래 횟수</strong><br/>
        <span class="help-desc">해당 코인의 총 매도 완료 건수입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>손익</strong><br/>
        <span class="help-desc">해당 코인에서 발생한 실현 손익 합계입니다.</span></p>
    `
  },
  recentTransactions: {
    title: '📋 최근 거래',
    content: `
      <p class="help-intro">가장 최근에 발생한 거래 내역을 보여줍니다.</p>
      <div style="height: 16px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>매수</strong><br/>
        <span class="help-desc">봇이 자동으로 코인을 매수한 내역입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>매도</strong><br/>
        <span class="help-desc">목표 수익률 도달 또는 손절매로 매도된 내역입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>보유중</strong><br/>
        <span class="help-desc">아직 매도되지 않고 보유 중인 상태입니다.</span></p>
      <p class="help-note">[전체 →] 버튼을 누르면 거래 내역 페이지로 이동합니다.</p>
    `
  },
  systemAlert: {
    title: '🚨 시스템 알림',
    content: `
      <p class="help-intro">시스템 상태 및 필요한 조치사항을 알려줍니다.</p>
      <div style="height: 16px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>API 키 미등록</strong><br/>
        <span class="help-desc">업비트 API 키를 등록해야 자동매매가 가능합니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>거래 설정 필요</strong><br/>
        <span class="help-desc">자동매매 조건을 설정해야 봇이 작동합니다.</span></p>
      <p class="help-note">알림이 없으면 시스템이 정상 상태입니다.</p>
    `
  }
}

// 도움말 다이얼로그 열기 함수
const openHelp = (helpKey: string) => {
  const help = cardHelps[helpKey as keyof typeof cardHelps]
  if (help) {
    currentHelp.value = help
    showHelpDialog.value = true
  }
}

// ★★★ [추가] 기간 변경 시 자산 이력 다시 로드 ★★★
watch(chartPeriod, async () => {
  await fetchAssetHistory()
})

// ★★★ [추가] 사용자 지정 기간 적용 시 자산 이력 다시 로드 ★★★
watch([customStartDate, customEndDate], async () => {
  if (chartPeriod.value === 'custom' && customStartDate.value && customEndDate.value) {
    await fetchAssetHistory()
  }
})
onMounted(async () => { 
  // 실시간 시간 업데이트 시작
  updateCurrentTime()
  timeInterval = window.setInterval(updateCurrentTime, 1000)
  
  await Promise.all([fetchDashboardStats(), fetchUpbitAccount(), fetchBotStatus(), fetchTradingSettings()]); 
  await Promise.all([fetchIndicators(), fetchHoldings(), fetchRecentTransactions(), fetchAssetHistory()]); 
  generateSystemAlerts(); 
  startAutoRefresh() 
  fetchLatestReleaseNote()  
  fetchTodayProfit()  
})
onUnmounted(() => { 
  stopAutoRefresh()
  if (botStatusTimer.value) {
    clearTimeout(botStatusTimer.value)
  }
  // ★★★ [추가] 카운트다운 인터벌 정리 ★★★
  if (countdownInterval) {
    clearInterval(countdownInterval)
    countdownInterval = null
  }
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

.chart-labels {
  position: absolute;
  right: 8px;
  top: 0;
  bottom: 30px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  pointer-events: none;
}

.chart-label-max {
  color: #4CAF50;
  font-size: 11px;
  background: rgba(255,255,255,0.9);
  padding: 2px 6px;
  border-radius: 4px;
}

.chart-label-init {
  color: #FFA726;
  font-size: 11px;
  background: rgba(255,255,255,0.9);
  padding: 2px 6px;
  border-radius: 4px;
}

.chart-label-min {
  color: #EF5350;
  font-size: 11px;
  background: rgba(255,255,255,0.9);
  padding: 2px 6px;
  border-radius: 4px;
}

.chart-tooltip-enhanced {
  position: absolute;
  top: 10px;
  transform: translateX(-50%);
  background: rgba(38,50,56,0.95);
  color: white;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 12px;
  pointer-events: none;
  z-index: 10;
  white-space: nowrap;
}

.chart-wrapper-backtest {
  position: relative;
  cursor: crosshair;
  height: 350px;
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

.label-max {
  color: #4CAF50;
}

.label-initial {
  color: #FF9800;
}

.label-min {
  color: #F44336;
}

/* ⭐⭐⭐ [신규 추가] 불입금액/평가금액 라벨 색상 ⭐⭐⭐ */
.label-deposit {
  color: #FF9800;
}

.label-evaluation {
  color: #1976D2;
}

.label-floor {
  color: #9E9E9E;
}

.chart-tooltip-backtest {
  position: absolute;
  /* top은 동적으로 설정됨 */
  background: rgba(0, 0, 0, 0.9);
  color: white;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 12px;
  pointer-events: none;
  z-index: 100;
  white-space: nowrap;
  box-shadow: 0 4px 12px rgba(0,0,0,0.4);
}

.text-success {
  color: #4CAF50 !important;
  font-weight: bold;
}

.text-error {
  color: #F44336 !important;
  font-weight: bold;
}

.help-content p {
  margin-bottom: 12px;
  line-height: 1.6;
}
.help-content p:last-child {
  margin-bottom: 0;
}
.help-content strong {
  color: #1565C0;
}

/* ★★★ [추가] 도움말 콘텐츠 스타일 개선 ★★★ */
.help-content .help-intro {
  margin-bottom: 28px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e0e0e0;
  color: #424242;
  font-size: 14px;
  line-height: 1.6;
}

.help-content .help-item {
  margin-bottom: 16px;
  padding-left: 8px;
}

.help-content .help-bullet {
  color: #1565C0;
  font-weight: bold;
  margin-right: 6px;
}

.help-content .help-desc {
  display: block;
  padding-left: 20px;
  margin-top: 4px;
  color: #616161;
  font-size: 13px;
  line-height: 1.5;
}

.help-content .help-note {
  margin-top: 16px;
  padding: 10px 12px;
  background-color: #FFF8E1;
  border-left: 3px solid #FFA000;
  border-radius: 4px;
  color: #5D4037;
  font-size: 13px;
}

.help-content p:last-child {
  margin-bottom: 0;
}

.help-content strong {
  color: #1565C0;
  font-weight: 600;
}

.chart-dates {
  font-size: 12px;
}

.chart-label.label-initial {
  color: #FF9800;
}

.chart-label.label-max {
  color: #4CAF50;
}

.chart-label.label-min {
  color: #F44336;
}

.chart-container {
  overflow-x: auto;
  overflow-y: hidden;
}

.chart-wrapper-backtest.scroll-mode {
  min-width: 100%;
}

/* ★★★ [추가] 사용자 지정 기간 입력 필드 스타일 ★★★ */
.custom-date-input {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 11px;
  color: #333;
  width: 110px;
  height: 24px;
}

.custom-date-input:focus {
  outline: none;
  border-color: #FFC107;
}

/* ★★★ [추가] 차트 컨트롤 버튼 높이 통일 (7일~전체투자기간 버튼 기준) ★★★ */
.chart-control-group {
  height: 28px;  /* v-btn-toggle과 동일한 높이 */
}

.custom-date-input {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 11px;
  color: #333;
  width: 110px;
  height: 28px;  /* ★★★ [수정] 버튼과 높이 통일 ★★★ */
  line-height: 20px;
}

.chart-view-btn {
  height: 28px !important;  /* ★★★ [추가] 버튼 높이 통일 ★★★ */
  min-height: 28px !important;
}

.chart-divider {
  border-color: rgba(255, 255, 255, 0.3) !important;
  height: 20px !important;
  align-self: center;
}

.chart-period-toggle {
  height: 28px;
}

.chart-period-toggle .v-btn {
  height: 28px !important;
  min-height: 28px !important;
}

/* ⭐⭐⭐ [신규] 포트폴리오 파이차트 스타일 ⭐⭐⭐ */
.portfolio-chart-wrapper {
  width: 260px;
  height: 200px;
  flex-shrink: 0;
}

.portfolio-chart {
  width: 100%;
  height: 100%;
}

.pie-slice {
  transition: opacity 0.2s;
  cursor: pointer;
}

.pie-slice:hover {
  opacity: 0.8;
}

.pie-center-text {
  font-weight: 500;
  pointer-events: none;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  flex-shrink: 0;
}

.pie-tooltip {
  position: absolute;
  background: rgba(0,0,0,0.8);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: bold;
  white-space: nowrap;
  pointer-events: none;
  transform: translate(-50%, -100%);
  z-index: 10;
}
</style>