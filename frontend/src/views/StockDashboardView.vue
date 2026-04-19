<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main class="bg-grey-lighten-3">
      <v-container fluid>

        <!-- 페이지 타이틀 -->
        <v-row class="mb-2">
          <v-col cols="12" class="py-2">
            <div class="d-flex align-center">
              <!-- ⭐ 코인 대시보드와 동일: mdi-view-dashboard 아이콘 스타일 유지, 주식은 mdi-chart-line -->
              <h1 class="text-h4">
                <v-icon class="mr-2">mdi-view-dashboard</v-icon>
                주식/ETF 대시보드
              </h1>
              <v-btn icon variant="text" class="ml-1" @click="refreshAll" :loading="isRefreshing" size="small">
                <v-icon size="20">mdi-refresh</v-icon>
              </v-btn>
              <v-spacer />
              <!-- ⭐ [수정] 코인 대시보드와 동일: text-body-1 font-weight-bold text-grey-darken-4 -->
              <span class="text-body-1 font-weight-bold text-grey-darken-4">{{ currentTimeShort }}</span>
            </div>
            <p class="text-subtitle-1 text-grey mt-1">주식/ETF 투자 현황과 자동매매 상태를 한눈에 확인하세요</p>
          </v-col>
        </v-row>

        <!-- ===== 섹션 1: 사용자+통계 + 봇 상태 ===== -->
        <v-row dense>
          <v-col cols="12" md="9">
            <v-card elevation="2" class="fill-height">
              <!-- ⭐ [수정 1] d-flex + justify-center로 세로 중앙 정렬 -->
              <v-card-text class="pa-3 d-flex flex-column justify-center fill-height">
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
                          <v-chip :color="hasKisApiKey ? 'teal-darken-1' : 'grey'" size="x-small" variant="flat">
                            {{ hasKisApiKey ? 'KIS API 등록됨' : 'API 미등록' }}
                          </v-chip>
                        </div>
                        <div class="text-caption text-grey-darken-1 mt-1">
                          <v-icon size="12" class="mr-1">mdi-email-outline</v-icon>{{ authStore.user?.email }}
                        </div>
                        <div class="text-caption text-grey-darken-4 font-weight-bold mt-1">
                          <v-icon size="12" class="mr-1">mdi-clock-outline</v-icon>
                          마지막 로그인:
                          <div class="ml-4">{{ formatLastLogin(authStore.user?.lastLogin) }}</div>
                        </div>
                      </div>
                    </div>
                  </v-col>

                  <!-- 4개 통계 카드 (Phase 1 동일 색상) -->
                  <v-col cols="6" sm="2">
                    <v-card color="teal-darken-1" dark class="pa-2 text-center" elevation="0">
                      <div class="text-caption">오늘 실현 손익</div>
                      <div class="text-subtitle-1 font-weight-bold">{{ formatCurrency(stats.realizedProfitLoss) }}</div>
                      <div class="text-caption">매도 {{ stats.soldCount }}건</div>
                    </v-card>
                  </v-col>
                  <v-col cols="6" sm="2">
                    <v-card color="indigo" dark class="pa-2 text-center" elevation="0">
                      <div class="text-caption">총 투자금액</div>
                      <div class="text-subtitle-1 font-weight-bold">{{ formatCurrency(stats.totalHoldingAmount) }}</div>
                      <div class="text-caption">보유 {{ stats.currentHoldingCount }}건</div>
                    </v-card>
                  </v-col>
                  <v-col cols="6" sm="2">
                    <v-card color="indigo-lighten-1" dark class="pa-2 text-center" elevation="0">
                      <div class="text-caption">오늘 매수</div>
                      <div class="text-subtitle-1 font-weight-bold">{{ stats.todayBuyCount }}건</div>
                      <div class="text-caption">{{ formatCurrency(stats.todayBuyAmount) }}</div>
                    </v-card>
                  </v-col>
                  <v-col cols="6" sm="2">
                    <v-card color="amber-darken-2" dark class="pa-2 text-center" elevation="0">
                      <div class="text-caption">오늘 매도</div>
                      <div class="text-subtitle-1 font-weight-bold">{{ stats.todaySellCount }}건</div>
                      <div class="text-caption">{{ formatCurrency(stats.todaySellAmount) }}</div>
                    </v-card>
                  </v-col>
                </v-row>
                <!-- 2행: 총거래(좌) + 시스템 상태(우) -->
                <v-row dense>
                  <v-col cols="12" sm="4"></v-col>
                  <v-col cols="12" sm="8" class="d-flex justify-space-between align-center" style="margin-top: -20px;">
                    <div class="text-caption text-grey-darken-4 font-weight-bold">
                      <v-icon size="12" class="mr-1">mdi-swap-horizontal</v-icon>
                      총 거래: 매수 {{ stats.totalBuyCount }}건 / 매도 {{ stats.totalSellCount }}건
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

          <!-- ⭐ [수정 8] 봇 상태 카드 - 코인 대시보드와 동일한 스위치+카운트다운 구조 -->
          <v-col cols="12" md="3">
            <v-card class="fill-height" elevation="2">
              <v-card-title
                class="py-2 px-3 text-white d-flex align-center"
                :class="botEnabled ? 'bg-indigo-darken-2' : 'bg-grey-darken-1'"
              >
                <v-icon class="mr-2" size="20">{{ botEnabled ? 'mdi-robot' : 'mdi-robot-off' }}</v-icon>
                <span class="text-body-2">자동매매</span>
                <v-btn icon size="x-small" variant="text" color="white" @click.stop="openHelp('botStatus')" class="ml-1">
                  <v-icon size="14">mdi-help-circle-outline</v-icon>
                </v-btn>
                <v-spacer />
                <!-- ⭐ 스위치 방식 (코인 대시보드 동일) -->
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
                <!-- ⭐ [수정 2] 코인 대시보드와 동일: isRunning=true → '실행 중'(teal), false → '대기 중'(blue-grey) -->
                <!-- ⭐ [수정 1] botEnabled=true면 장마감이어도 "실행 중"으로 표시 -->
                <!-- ⭐ [수정 1] 정지됨일 때 검은 테두리 추가 -->
                <!-- ⭐ [수정 1] 정지됨: 최근 거래 매도완료 칩과 동일 (orange flat) -->
                <v-chip
                  :color="botEnabled ? 'teal' : 'orange'"
                  variant="flat"
                  size="x-small"
                >
                  {{ botEnabled ? '실행 중' : '정지됨' }}
                </v-chip>
              </v-card-title>
              <v-card-text class="pa-3">
                <!-- ⭐ [수정 2] "장 운영" → "오늘 장 운영 상태" -->
                <div class="d-flex justify-space-between align-center text-body-2 text-grey-darken-3 mb-1">
                  <span>오늘 장 운영 상태</span>
                  <v-chip :color="botStatus.marketOpen ? 'teal' : 'grey'" size="x-small" variant="flat">
                    {{ botStatus.marketOpen ? '장중' : '장마감' }}
                  </v-chip>
                </div>
                <!-- ⭐ [수정 1] "내일 휴장" → "내일 장 운영 여부" / "정상" → "장 오픈 예정" -->
                <div class="d-flex justify-space-between align-center text-body-2 text-grey-darken-3 mb-1">
                  <span>내일 장 운영 여부</span>
                  <v-chip :color="tomorrowHoliday ? 'orange' : 'teal'" size="x-small" variant="flat">
                    {{ tomorrowHoliday ? '휴장일' : '장 오픈 예정' }}
                  </v-chip>
                </div>
                <!-- ⭐ 마지막/다음 봇 수행시간 (코인 대시보드 동일) -->
                <div class="d-flex justify-space-between align-center text-body-2 text-grey-darken-3 mb-1">
                  <span>마지막 봇 수행 시간</span>
                  <span class="font-weight-medium">{{ formatBotTimeDisplay(botStatus.lastExecutionTime) }}</span>
                </div>
                <div class="d-flex justify-space-between align-center text-body-2 text-grey-darken-3">
                  <span>다음 봇 수행 시간</span>
                  <span class="font-weight-medium">{{ botEnabled ? formatBotTimeDisplay(botStatus.nextExecutionTime) : '-' }}</span>
                </div>
                <!-- 카운트다운 / 정지 문구 (코인 대시보드 동일) -->
                <div v-if="!botEnabled" class="text-caption text-orange-darken-2 text-right mt-1 font-weight-medium">
                  (중단 상태입니다)
                </div>
                <div v-else-if="countdownSeconds > 0" class="text-caption text-teal-darken-2 text-right mt-1 font-weight-medium">
                  ({{ Math.floor(countdownSeconds / 60) }}분 {{ countdownSeconds % 60 }}초 후)
                </div>
                <v-chip v-if="botStatus.emergencyStop" color="red" size="x-small" variant="flat" class="mt-1">🚨 긴급정지</v-chip>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- ===== 섹션 2: KIS 계좌 현황 + 환율 카드 (같은 줄) ===== -->
        <!-- ⭐ [수정 3] KIS 계좌(cols=8) + 환율(cols=4) 같은 줄 배치 -->
        <v-row class="mt-3" dense>
          <!-- KIS 계좌 현황 (cols=8) -->
          <v-col cols="12" md="8">
            <v-card elevation="2" class="fill-height">
              <v-card-title class="py-2 px-4 bg-amber-darken-2 text-white d-flex align-center">
                <v-icon class="mr-2" size="20">mdi-bank</v-icon>
                <span class="text-body-1">KIS 계좌 현황</span>
                <v-btn icon size="x-small" variant="text" color="white" @click.stop="openHelp('kisAccount')" class="ml-1">
                  <v-icon size="14">mdi-help-circle-outline</v-icon>
                </v-btn>
                <v-spacer />
                <!-- ⭐ [수정 1] 샘플 데이터 보기 토글 (KIS API 미등록 시에만 표시) -->
                <!-- ⭐ [수정 2] 간격 추가 + 배경색으로 구분 -->
                <!-- ⭐ [수정 2] 텍스트↔스위치 간격 확보 + 상하 패딩 축소로 배경 높이 감소 -->
                <div v-if="!hasKisApiKey" class="d-flex align-center mr-2"
                  style="background: rgba(0,0,0,0.20); border-radius: 12px; padding: 1px 6px 1px 10px; gap: 6px;">
                  <span class="text-caption font-weight-bold"
                    style="font-size:11px; color: #fff; letter-spacing: 0.3px; white-space: nowrap;">
                    샘플 보기
                  </span>
                  <v-switch
                    v-model="kisAccountSampleMode"
                    color="white"
                    hide-details
                    density="compact"
                    style="flex: none; margin: 0;"
                  />
                </div>
                <v-btn icon size="x-small" variant="text" color="white" @click="loadKisAccount" :loading="accountLoading">
                  <v-icon size="18">mdi-refresh</v-icon>
                </v-btn>
              </v-card-title>
              <v-card-text class="pa-3">
                <v-row dense>
                  <!-- 좌측: 보유자산 포트폴리오 3D 파이차트 (Phase 1 동일) -->
                  <v-col cols="12" md="6">
                    <!-- ⭐ [수정 1] 샘플 모드 배너 -->
                    <v-alert v-if="kisAccountSampleMode" density="compact" type="info" variant="tonal" class="text-caption pa-1 mb-2">
                      🧪 샘플 데이터 표시 중 (실제 KIS 계좌 아님)
                    </v-alert>
                    <!-- ⭐ [수정 1] kisAccount → displayKisAccount -->
                    <div v-if="displayKisAccount.totalAsset > 0">
                      <div class="d-flex">
                        <!-- ⭐ [수정 3] "보유 종목 구성" → "보유자산 포트폴리오" -->
                        <div class="text-body-2 font-weight-bold text-grey-darken-3 pt-1" style="min-width: 130px; flex-shrink: 0;">보유자산 포트폴리오</div>
                        <div class="portfolio-chart-wrapper" style="position: relative; flex: 1;" ref="kisChartWrapper">
                          <svg viewBox="0 0 300 220" class="portfolio-chart">
                            <defs>
                              <filter id="shadow3d-kis" x="-10%" y="-10%" width="120%" height="130%">
                                <feDropShadow dx="1" dy="3" stdDeviation="3" flood-opacity="0.25"/>
                              </filter>
                            </defs>
                            <!-- 3D 측면 두께 (Phase 1 portfolio3dSides 동일 구조) -->
                            <path
                              v-for="(slice, index) in kisPortfolio3dSides"
                              :key="'kis-side-' + index"
                              :d="slice.sidePath"
                              :fill="slice.darkColor"
                              stroke="none"
                            />
                            <!-- 파이 차트 상면 (Phase 1 portfolio3dSlices 동일 구조) -->
                            <path
                              v-for="(slice, index) in kisPortfolio3dSlices"
                              :key="'kis-slice-' + index"
                              :d="slice.path"
                              :fill="slice.color"
                              stroke="#333333"
                              stroke-width="1.5"
                              filter="url(#shadow3d-kis)"
                              class="pie-slice"
                              @mouseenter="hoveredKisSlice = index"
                              @mouseleave="hoveredKisSlice = -1"
                            />
                            <!-- % 텍스트 -->
                            <text
                              v-for="(slice, index) in kisPortfolio3dSlices"
                              :key="'kis-pct-' + index"
                              :x="slice.labelX"
                              :y="slice.labelY"
                              text-anchor="middle"
                              :font-size="slice.percent >= 20 ? 13 : 10"
                              font-weight="bold"
                              fill="#333333"
                              stroke="none"
                              pointer-events="none"
                            >{{ slice.percent.toFixed(1) }}</text>
                            <!-- 중앙 도넛 홀 (Phase 1 동일) -->
                            <ellipse cx="150" cy="100" rx="50" ry="40" fill="white" />
                            <ellipse cx="150" cy="100" rx="50" ry="40" fill="none" stroke="rgba(0,0,0,0.08)" stroke-width="1" />
                            <text x="150" y="96" text-anchor="middle" font-size="12" fill="#616161" font-weight="500">보유비중</text>
                            <text x="150" y="110" text-anchor="middle" font-size="12" fill="#616161" font-weight="500">(%)</text>
                          </svg>
                          <!-- 호버 툴팁 -->
                          <!-- ⭐ [수정 5] 종목명 함께 표시 -->
                          <div
                            v-if="hoveredKisSlice >= 0 && kisPortfolio3dSlices[hoveredKisSlice]"
                            class="pie-tooltip"
                            style="left: 50%; top: 40%;"
                          >
                            <strong>{{ kisPortfolio3dSlices[hoveredKisSlice].label }}</strong>
                            <template v-if="kisPortfolio3dSlices[hoveredKisSlice].label !== 'KRW'">
                              <br/><span style="font-size:11px; opacity:0.9;">
                                {{ getStockName(kisPortfolio3dSlices[hoveredKisSlice].label) }}
                              </span>
                            </template>
                            <br/>{{ kisPortfolio3dSlices[hoveredKisSlice].percent.toFixed(1) }}%
                          </div>
                        </div>
                      </div>
                      <!-- 하단 범례 (Phase 1 동일) -->
                      <div class="d-flex flex-wrap justify-center" style="gap: 6px; margin-top: -4px;">
                        <div
                          v-for="(item, index) in kisPortfolioLegend"
                          :key="'kis-legend-' + index"
                          class="d-flex align-center"
                        >
                          <div class="legend-dot mr-1" :style="{ backgroundColor: item.color }"></div>
                          <!-- ⭐ [수정 5] 종목코드 + 종목명 표시 (KRW는 코드만) -->
                          <span class="text-caption font-weight-bold">{{ item.label }}</span>
                          <span v-if="item.label !== 'KRW'" class="text-caption text-grey-darken-2 ml-1">
                            {{ getStockName(item.label) !== item.label ? getStockName(item.label) : '' }}
                          </span>
                          <span class="text-caption text-grey-darken-1 ml-1">{{ item.percent.toFixed(1) }}%</span>
                        </div>
                      </div>
                    </div>
                    <div v-else class="d-flex flex-column align-center justify-center text-grey-darken-2 fill-height" style="min-height: 200px;">
                      <v-icon size="32" class="mb-1">mdi-chart-donut</v-icon>
                      <div class="text-caption">자산 정보 없음</div>
                    </div>
                  </v-col>

                  <!-- 우측: KRW잔고 / 주식평가액 / 총자산 (Phase 1 업비트 잔고 카드 동일 구조) -->
                  <!-- ⭐ [수정 3] 총투자금액/오늘실현손익/현재보유건수 → KRW잔고/주식평가액/총자산 -->
                  <v-col cols="12" md="6">
                    <!-- ⭐ [수정 1] kisAccount → displayKisAccount -->
                    <v-card variant="outlined" class="pa-3 text-center mb-2">
                      <div class="text-caption text-grey-darken-1 mb-1">KRW 잔고 (매수 대기 자금)</div>
                      <div class="text-h5 font-weight-bold text-amber-darken-3">{{ formatCurrency(displayKisAccount.krwBalance) }}</div>
                    </v-card>
                    <v-card variant="outlined" class="pa-3 text-center mb-2">
                      <div class="text-caption text-grey-darken-1 mb-1">주식 평가액</div>
                      <div class="text-h5 font-weight-bold text-indigo-darken-1">{{ formatCurrency(displayKisAccount.stockEvaluation) }}</div>
                    </v-card>
                    <v-card variant="outlined" class="pa-3 text-center total-asset-card">
                      <div class="text-caption text-grey-darken-1 mb-1">총 자산</div>
                      <div class="text-h5 font-weight-bold text-teal-darken-2">{{ formatCurrency(displayKisAccount.totalAsset) }}</div>
                    </v-card>
                  </v-col>
                </v-row>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- ⭐ [수정 3] 환율 카드를 KIS 계좌 우측에 배치 (cols=4) -->
          <v-col cols="12" md="4">
            <v-card class="fill-height" elevation="2">
              <v-card-title class="py-2 px-4 bg-teal-darken-2 text-white d-flex align-center">
                <v-icon class="mr-2" size="20">mdi-currency-usd</v-icon>
                <span class="text-body-1">USD / KRW 환율</span>
                <v-btn icon size="x-small" variant="text" color="white" @click.stop="openHelp('exchangeRate')" class="ml-1">
                  <v-icon size="14">mdi-help-circle-outline</v-icon>
                </v-btn>
                <v-spacer />
                <v-btn icon size="x-small" variant="text" color="white" @click="loadExchangeRate" :loading="exchangeLoading">
                  <v-icon size="18">mdi-refresh</v-icon>
                </v-btn>
              </v-card-title>
              <v-card-text class="pa-3">
                <template v-if="exchangeStatus === 'ok' || exchangeStatus === 'fallback-with-data'">
                  <div class="d-flex align-center justify-space-between mb-1">
                    <span class="text-h4 font-weight-bold">₩{{ formatNumber(exchange.exchangeRate) }}</span>
                    <v-chip v-if="exchangeStatus === 'fallback-with-data'" color="warning" size="x-small" variant="tonal">마지막 저장값</v-chip>
                    <!-- ⭐ [수정 3] 날짜 칩 가독성 강화: grey tonal → 검정 텍스트 + 진한 배경 -->
                    <v-chip v-else size="x-small" variant="flat"
                      style="background:#424242; color:#fff; font-weight:600; font-size:12px;">
                      {{ exchange.date }}
                    </v-chip>
                  </div>
                  <!-- ⭐ [수정 4] 전일 대비 등락 표시 -->
                  <div class="d-flex align-center ga-1 mb-1">
                    <v-icon :color="exchangeChangeColor" size="18">
                      {{ (exchange.exchangeRateChange ?? 0) >= 0 ? 'mdi-trending-up' : 'mdi-trending-down' }}
                    </v-icon>
                    <span class="text-body-2 font-weight-medium" :class="`text-${exchangeChangeColor}`">
                      전일 대비
                      {{ (exchange.exchangeRateChange ?? 0) >= 0 ? '+' : '' }}{{ Number(exchange.exchangeRateChange ?? 0).toFixed(2) }}원
                      ({{ (exchange.exchangeRateChangePct ?? 0) >= 0 ? '+' : '' }}{{ Number(exchange.exchangeRateChangePct ?? 0).toFixed(2) }}%)
                    </span>
                  </div>
                  <!-- ⭐ [수정 2] 카운트다운 + 환노출형 안내 같은 줄 배치 -->
                  <v-alert v-if="exchangeStatus === 'fallback-with-data'" density="compact" type="warning" variant="tonal" class="text-caption pa-2 mb-2">
                    환율 정보를 불러올 수 없습니다.<br>
                    <strong>{{ exchangeLastFetchedAt }} 기준</strong> 마지막 저장값을 표시합니다.<br>
                    {{ exchangeRetryCountdown }}초 후 재시도...
                  </v-alert>
                  <div v-else class="d-flex align-center justify-space-between">
                    <span class="text-caption text-grey-darken-1">환노출형 ETF(TIGER 등) 수익에 직접 영향</span>
                    <span v-if="exchangeStatus === 'ok'" class="text-caption text-teal-darken-2 font-weight-medium">
                      ({{ Math.floor(exchangeAutoRefreshSeconds / 60) }}분 {{ String(exchangeAutoRefreshSeconds % 60).padStart(2, '0') }}초 후 갱신)
                    </span>
                  </div>
                </template>
                <template v-else>
                  <div class="text-h6 text-medium-emphasis text-center my-3">— 데이터 없음 —</div>
                  <v-alert density="compact" type="error" variant="tonal" class="text-caption pa-2 mb-2">
                    환율 정보를 불러올 수 없습니다.<br>
                    {{ exchangeRetryCountdown }}초 후 재시도...
                  </v-alert>
                  <div class="text-caption text-grey-darken-1">환노출형 ETF(TIGER 등) 수익에 직접 영향</div>
                </template>
                <!-- ⭐ [수정 3] TIGER 칩: orange outlined → 진한 주황 flat으로 가독성 개선 -->
                <!-- ⭐ [수정 2] TIGER: 긴급(20일+) 동일 크기+색상(error outlined), KODEX: blue outlined, size=small -->
                <div class="d-flex ga-1 flex-wrap mt-2">
                  <v-chip size="small" color="error" variant="outlined">TIGER(환노출) 영향</v-chip>
                  <v-chip size="small" color="blue" variant="outlined">KODEX(환헤지) 무관</v-chip>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- ===== 섹션 3: 거래설정 + 일일한도 + 환율 카드 (Phase 1 거래설정+일일한도+매수조건 대응) ===== -->
        <v-row class="mt-3" dense>
          <!-- 거래 설정 -->
          <v-col cols="12" md="4">
            <v-card class="fill-height" elevation="2">
              <v-card-title class="py-2 px-4 bg-indigo-darken-2 text-white d-flex align-center">
                <v-icon class="mr-2" size="20">mdi-cog</v-icon>
                <span class="text-body-1">거래 설정</span>
                <v-btn icon size="x-small" variant="text" color="white" @click.stop="openHelp('tradingSettings')" class="ml-1">
                  <v-icon size="14">mdi-help-circle-outline</v-icon>
                </v-btn>
              </v-card-title>
              <v-card-text v-if="stockSettings" class="pa-3">
                <div class="mb-2">
                  <span class="text-caption text-grey-darken-1">거래 종목</span>
                  <!-- ⭐ [수정 3] 코드 → 종목명 변환, 종목당 줄바꿈 -->
                  <div v-if="stockSettings.stockCodes?.length">
                    <div
                      v-for="code in stockSettings.stockCodes"
                      :key="code"
                      class="text-body-2 text-grey-darken-4 font-weight-medium"
                      style="line-height: 1.6;"
                    >
                      <span class="text-caption text-grey-darken-2 mr-1">[{{ code }}]</span>{{ getStockName(code) }}
                    </div>
                  </div>
                  <div v-else class="text-body-2 text-grey-darken-4 font-weight-medium">-</div>
                </div>
                <div class="mb-2">
                  <span class="text-caption text-grey-darken-1">매수 조건</span>
                  <div class="text-body-2 text-grey-darken-4 font-weight-medium">
                    <!-- ⭐ [수정 4] Number() 변환으로 BigDecimal 직렬화 문제 해결 -->
                    MA{{ stockSettings.basePeriod }} 대비 {{ Number(stockSettings.buyThresholdPct) }}% 이하
                  </div>
                </div>
                <div class="mb-2">
                  <span class="text-caption text-grey-darken-1">매도 조건</span>
                  <div class="text-body-2 font-weight-medium">
                    <span class="text-teal-darken-2">익절 +{{ Number(stockSettings.sellTargetPct) }}%</span>
                    <span class="mx-1">/</span>
                    <span class="text-red-darken-2">손절 {{ Number(stockSettings.stopLossPct) }}%</span>
                  </div>
                </div>
                <div class="mb-2">
                  <span class="text-caption text-grey-darken-1">종목당 최대 보유</span>
                  <div class="text-body-2 text-grey-darken-4 font-weight-medium">{{ stockSettings.maxHoldingsPerStock || 3 }}건</div>
                </div>
                <div class="mb-2">
                  <span class="text-caption text-grey-darken-1">최대 보유기간</span>
                  <div class="text-body-2 text-grey-darken-4 font-weight-medium">
                    {{ stockSettings.maxHoldingDays || 20 }}거래일
                    <span class="text-caption text-orange-darken-2 ml-1">(레버리지 ETF Decay 방지)</span>
                  </div>
                </div>
                <!-- ⭐ [수정 8] 누락된 거래설정 항목 추가 -->
                <div v-if="stockSettings.trailingStopPct != null" class="mb-2">
                  <span class="text-caption text-grey-darken-1">트레일링 스톱</span>
                  <div class="text-body-2 text-grey-darken-4 font-weight-medium">
                    최고가 대비 {{ Number(stockSettings.trailingStopPct) }}% 하락 시 매도
                  </div>
                </div>
                <div v-if="stockSettings.rsiBuyThreshold != null" class="mb-2">
                  <span class="text-caption text-grey-darken-1">RSI 매수/매도 신호</span>
                  <div class="text-body-2 text-grey-darken-4 font-weight-medium">
                    매수 {{ stockSettings.rsiBuyThreshold || 35 }} 이하
                    <span class="mx-1">/</span>
                    매도 {{ stockSettings.rsiSellThreshold || 65 }} 이상
                  </div>
                </div>
                <!-- ⭐ [수정 Q5] 누락 항목 추가: 거래량, 볼린저밴드, 시장추세, 리스크관리, 라운드로빈 -->
                <div v-if="stockSettings.volumeThreshold != null" class="mb-2">
                  <span class="text-caption text-grey-darken-1">거래량 기준</span>
                  <div class="text-body-2 text-grey-darken-4 font-weight-medium">
                    평균 대비 {{ Number(stockSettings.volumeThreshold) || 120 }}% 이상
                  </div>
                </div>
                <div v-if="stockSettings.bbPeriod != null" class="mb-2">
                  <span class="text-caption text-grey-darken-1">볼린저밴드</span>
                  <div class="text-body-2 text-grey-darken-4 font-weight-medium">
                    {{ stockSettings.bbPeriod || 20 }}일 / {{ Number(stockSettings.bbMultiplier) || 2 }}σ
                  </div>
                </div>
                <div v-if="stockSettings.useMarketTrendFilter != null" class="mb-2">
                  <span class="text-caption text-grey-darken-1">시장 추세 필터</span>
                  <div class="d-flex align-center">
                    <v-chip :color="stockSettings.useMarketTrendFilter ? 'indigo' : 'grey'" size="x-small" variant="flat" class="mr-1">
                      {{ stockSettings.useMarketTrendFilter ? 'ON' : 'OFF' }}
                    </v-chip>
                    <span v-if="stockSettings.useMarketTrendFilter" class="text-caption text-grey-darken-1">지수 MA20 하회 시 매수 중단</span>
                  </div>
                </div>
                <div v-if="stockSettings.cumulativeLossLimitPct != null" class="mb-2">
                  <span class="text-caption text-grey-darken-1">누적 손실 긴급정지</span>
                  <div class="text-body-2 text-red-darken-2 font-weight-medium">
                    {{ stockSettings.cumulativeLossLimitPct }}% 도달 시 거래 중단
                  </div>
                </div>
                <div v-if="stockSettings.consecutiveStopLossLimit != null" class="mb-2">
                  <span class="text-caption text-grey-darken-1">연속 손절 제한</span>
                  <div class="text-body-2 text-grey-darken-4 font-weight-medium">
                    동일 종목 {{ stockSettings.consecutiveStopLossLimit }}회 연속 손절 시 매수 금지
                  </div>
                </div>
                <div v-if="stockSettings.useRoundRobin != null" class="mb-2">
                  <span class="text-caption text-grey-darken-1">라운드로빈 방식</span>
                  <v-chip :color="stockSettings.useRoundRobin ? 'teal' : 'grey'" size="x-small" variant="flat" class="ml-1">
                    {{ stockSettings.useRoundRobin ? '사용' : '미사용' }}
                  </v-chip>
                </div>
                <!-- ⭐ [수정 Q4] 누락 항목 추가: 손절매 ON/OFF, 추가 하락 조건, 일일 한도 비율, 모의투자 -->
                <!-- 이유: StockTradingSetting 엔티티의 useStopLoss, additionalDropPct,
                           dailyTradeLimitPct, kisMockMode 필드가 카드에 미표시 상태 -->
                <div v-if="stockSettings.useStopLoss != null" class="mb-2">
                  <span class="text-caption text-grey-darken-1">손절매</span>
                  <div class="d-flex align-center">
                    <v-chip :color="stockSettings.useStopLoss ? 'red-darken-1' : 'grey'" size="x-small" variant="flat" class="mr-1">
                      {{ stockSettings.useStopLoss ? 'ON' : 'OFF' }}
                    </v-chip>
                    <span v-if="stockSettings.useStopLoss" class="text-body-2 text-grey-darken-4 font-weight-medium">
                      {{ Number(stockSettings.stopLossPct) }}% 도달 시 강제 매도
                    </span>
                  </div>
                </div>
                <div v-if="stockSettings.additionalDropPct != null" class="mb-2">
                  <span class="text-caption text-grey-darken-1">추가 하락 조건</span>
                  <div class="text-body-2 text-grey-darken-4 font-weight-medium">
                    {{ Number(stockSettings.additionalDropPct) }}% 추가 하락 시 분할 매수
                  </div>
                </div>
                <div v-if="stockSettings.dailyTradeLimitPct != null" class="mb-2">
                  <span class="text-caption text-grey-darken-1">일일 투자 한도</span>
                  <div class="text-body-2 text-grey-darken-4 font-weight-medium">
                    총 자산의 {{ stockSettings.dailyTradeLimitPct || 20 }}%
                  </div>
                </div>
                <div v-if="stockSettings.kisMockMode != null" class="mb-2">
                  <span class="text-caption text-grey-darken-1">KIS 거래 모드</span>
                  <v-chip :color="stockSettings.kisMockMode ? 'orange' : 'teal'" size="x-small" variant="flat" class="ml-1">
                    {{ stockSettings.kisMockMode ? '모의투자' : '실계좌' }}
                  </v-chip>
                </div>
                <v-btn size="small" color="indigo" variant="text" class="mt-1 px-0" @click="$router.push('/stock/settings')">설정 변경 →</v-btn>
              </v-card-text>
              <v-card-text v-else class="pa-0 fill-height">
                <div class="d-flex align-center justify-center" style="height: 220px;">
                  <v-btn size="large" color="indigo-darken-1" variant="flat" @click="$router.push('/stock/settings')">
                    <v-icon start>mdi-cog</v-icon>
                    설정하기
                  </v-btn>
                </div>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- 일일 한도 -->
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
                  <span class="text-body-2 text-grey-darken-4 font-weight-medium">{{ formatCurrency(stats.dailyLimitAmount) }}</span>
                </div>
                <div class="d-flex justify-space-between mb-1">
                  <span class="text-caption text-grey-darken-1">사용</span>
                  <span class="text-body-2 text-grey-darken-4">{{ formatCurrency(stats.dailyLimitAmount - stats.remainingDailyLimit) }}</span>
                </div>
                <div class="d-flex justify-space-between mb-2">
                  <span class="text-caption text-grey-darken-1">남은 한도</span>
                  <span class="text-body-2 font-weight-bold text-teal-darken-2">{{ formatCurrency(stats.remainingDailyLimit) }}</span>
                </div>
                <v-progress-linear
                  :model-value="dailyLimitUsedPct"
                  :color="dailyLimitUsedPct > 80 ? 'red' : dailyLimitUsedPct > 50 ? 'amber-darken-2' : 'teal'"
                  height="18" rounded
                >
                  <span class="text-caption font-weight-bold">{{ dailyLimitUsedPct.toFixed(0) }}%</span>
                </v-progress-linear>
                <!-- 종목별 보유 현황 -->
                <div class="mt-2">
                  <span class="text-caption text-grey-darken-1">레버리지 ETF 보유기간 경고</span>
                  <!-- ⭐ [수정 6] 경고 칩: warning outlined(노란색) → amber-darken-3 flat(진한 주황)으로 가독성 개선 -->
                  <!-- ⭐ [수정 5] 경고 칩: flat → outlined, orange-darken-2 (긴급 칩과 동일 스타일, 다른 색) -->
                  <!-- ⭐ [수정 3] 경고 칩: orange-darken-2 → warning(amber) outlined으로 가독성 개선 -->
                  <!-- amber-darken-3은 충분히 진하여 outlined에서도 잘 보임, 긴급(error red)과 명확히 구분 -->
                  <div class="mt-1 d-flex ga-2">
                    <!-- ⭐ [수정 1] color="warning" 제거 → style만으로 진한 주황 직접 지정 (Vuetify amber 오버라이드 방지) -->
                    <v-chip size="small" variant="outlined"
                      style="color: #BF360C; border-color: #BF360C;">
                      경고(15일+) {{ stats.holdingDaysWarningCount }}건
                    </v-chip>
                    <v-chip color="error" size="small" variant="outlined">
                      긴급(20일+) {{ stats.holdingDaysUrgentCount }}건
                    </v-chip>
                  </div>
                </div>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- ⭐ [수정 4] 환율 카드 → 매수 조건 카드로 교체 (Phase 1 buyCondition 카드 동일) -->
          <v-col cols="12" md="4">
            <v-card class="fill-height" elevation="2">
              <v-card-title class="py-2 px-4 bg-teal-darken-2 text-white d-flex align-center">
                <v-icon class="mr-2" size="20">mdi-target</v-icon>
                <span class="text-body-1">매수 조건</span>
                <v-btn icon size="x-small" variant="text" color="white" @click.stop="openHelp('buyCondition')" class="ml-1">
                  <v-icon size="14">mdi-help-circle-outline</v-icon>
                </v-btn>
                <v-spacer />
                <v-btn icon size="x-small" variant="text" color="white" @click="loadStockIndicators" :loading="indicatorsLoading">
                  <v-icon size="18">mdi-refresh</v-icon>
                </v-btn>
              </v-card-title>
              <v-card-text class="pa-2">
                <!-- 매수 조건 설명 (Phase 1 동일) -->
                <v-alert
                  v-if="stockSettings"
                  type="info"
                  variant="tonal"
                  density="compact"
                  class="mb-2 text-caption"
                  :icon="false"
                >
                  <v-icon size="14" class="mr-1">mdi-information</v-icon>
                  <!-- ⭐ [수정 4] Number() 변환 -->
                  MA{{ stockSettings.basePeriod || 20 }} 대비 {{ Number(stockSettings.buyThresholdPct) }}% 이하일 때 매수
                </v-alert>

                <v-list density="compact" class="pa-0">
                  <v-list-item
                    v-for="item in stockIndicators"
                    :key="item.stockCode"
                    class="px-2 py-1"
                    style="min-height: 48px;"
                  >
                    <div class="d-flex align-center justify-space-between w-100">
                      <div class="flex-grow-1">
                        <div class="d-flex align-center flex-wrap">
                          <span class="text-body-2 font-weight-medium text-grey-darken-4 mr-1">
                            {{ getStockName(item.stockCode) }}
                          </span>
                          <span class="text-caption text-grey-darken-2">({{ item.stockCode }})</span>
                        </div>
                        <div class="text-caption text-grey-darken-1">
                          <span class="font-weight-medium">현재가:</span> {{ formatCurrency(item.currentPrice) }}
                          <v-icon size="12" class="mx-1">mdi-arrow-right</v-icon>
                          <span class="font-weight-medium">매수가:</span> {{ formatCurrency(item.buyPrice) }}
                        </div>
                        <div class="text-caption" :class="item.canBuy ? 'text-teal-darken-2' : 'text-grey'">
                          이격도 {{ item.dropRate?.toFixed(2) || '0.00' }}%
                          <span v-if="!item.canBuy">({{ Math.abs(item.remainingDrop || 0).toFixed(1) }}% 더 하락 필요)</span>
                          <span v-else class="font-weight-bold">(매수 조건 충족!)</span>
                        </div>
                      </div>
                      <!-- ⭐ [수정 4] 코인 대시보드와 완전 동일: 가로 배치, outlined indigo + flat blue-grey -->
                      <div class="d-flex align-center">
                        <v-chip
                          size="x-small"
                          variant="outlined"
                          color="indigo"
                          class="mr-1"
                          style="cursor: pointer;"
                          @click="openStockIndicatorDetail(item)"
                        >
                          상세 지표 보기
                        </v-chip>
                        <v-chip
                          :color="item.canBuy ? 'teal' : 'blue-grey-darken-1'"
                          size="x-small"
                          variant="flat"
                        >
                          {{ item.canBuy ? '매수가능' : '대기' }}
                        </v-chip>
                      </div>
                    </div>
                  </v-list-item>
                </v-list>

                <div v-if="!stockIndicators.length" class="d-flex flex-column align-center justify-center text-grey" style="height: 100%; min-height: 180px;">
                  <v-icon size="32" class="mb-2">mdi-chart-timeline-variant</v-icon>
                  <div class="text-caption">거래 설정을 먼저 완료해주세요</div>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- ===== 섹션 4: 수익 현황 (Phase 1 동일 구조) ===== -->
        <v-row class="mt-3" dense>
          <v-col cols="12">
            <v-card elevation="2">
              <v-card-title class="py-2 px-4 bg-teal-darken-2 text-white d-flex align-center">
                <v-icon class="mr-2" size="20">mdi-cash-multiple</v-icon>
                <span class="text-body-1">수익 현황</span>
                <v-btn icon size="x-small" variant="text" color="white" @click.stop="openHelp('profitSummary')" class="ml-1">
                  <v-icon size="14">mdi-help-circle-outline</v-icon>
                </v-btn>
                <v-spacer />
                <!-- ⭐ [수정 5] 보유 주식 자산 페이지 미구현 → 비활성화 처리 (Day 60 구현 예정) -->
                <v-tooltip text="보유 주식 자산 페이지 준비 중입니다" location="bottom">
                  <template #activator="{ props }">
                    <v-btn
                      v-bind="props"
                      size="small" variant="flat" color="amber"
                      class="text-grey-darken-4"
                      disabled
                    >
                      상세 분석 →
                    </v-btn>
                  </template>
                </v-tooltip>
              </v-card-title>
              <v-card-text class="pa-3">
                <!-- ⭐ [수정 5] Phase 1 수익현황과 동일: 오늘수익/미실현/실현/누적총수익 -->
                <v-row dense>
                  <v-col cols="12" md="3">
                    <v-card variant="outlined" class="pa-3 text-center" height="110">
                      <div class="text-caption text-grey-darken-2">🌅 오늘 수익</div>
                      <div :class="stockProfit.todayProfit >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2'" class="text-h5 font-weight-bold mt-2">
                        {{ stockProfit.todayProfit >= 0 ? '+' : '' }}{{ formatCurrency(stockProfit.todayProfit) }}
                      </div>
                      <div class="text-caption mt-1" :class="stockProfit.todayProfitPct >= 0 ? 'text-teal-darken-1' : 'text-red-darken-1'">
                        ({{ stockProfit.todayProfitPct >= 0 ? '+' : '' }}{{ stockProfit.todayProfitPct.toFixed(2) }}%)
                      </div>
                    </v-card>
                  </v-col>
                  <v-col cols="12" md="3">
                    <v-card variant="outlined" class="pa-3 text-center" height="110">
                      <div class="text-caption text-grey-darken-2">📈 평가 수익 (미실현)</div>
                      <div :class="stockProfit.unrealizedProfit >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2'" class="text-h5 font-weight-bold mt-2">
                        {{ stockProfit.unrealizedProfit >= 0 ? '+' : '' }}{{ formatCurrency(stockProfit.unrealizedProfit) }}
                      </div>
                      <div class="text-caption mt-1" :class="stockProfit.unrealizedProfitPct >= 0 ? 'text-teal-darken-1' : 'text-red-darken-1'">
                        ({{ stockProfit.unrealizedProfitPct >= 0 ? '+' : '' }}{{ stockProfit.unrealizedProfitPct.toFixed(2) }}%)
                      </div>
                    </v-card>
                  </v-col>
                  <v-col cols="12" md="3">
                    <v-card variant="outlined" class="pa-3 text-center" height="110">
                      <div class="text-caption text-grey-darken-2">✅ 실현 수익 (확정)</div>
                      <div :class="stockProfit.realizedProfit >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2'" class="text-h5 font-weight-bold mt-2">
                        {{ stockProfit.realizedProfit >= 0 ? '+' : '' }}{{ formatCurrency(stockProfit.realizedProfit) }}
                      </div>
                      <div class="text-caption mt-1 text-grey-darken-1">&nbsp;</div>
                    </v-card>
                  </v-col>
                  <v-col cols="12" md="3">
                    <v-card variant="outlined" class="pa-3 text-center total-profit-card" height="110">
                      <div class="text-caption text-teal-darken-2 font-weight-medium">💰 누적 총 수익</div>
                      <div :class="stockProfit.totalProfit >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2'" class="text-h5 font-weight-bold mt-2">
                        {{ stockProfit.totalProfit >= 0 ? '+' : '' }}{{ formatCurrency(stockProfit.totalProfit) }}
                      </div>
                      <div class="text-caption mt-1 text-grey-darken-1">&nbsp;</div>
                    </v-card>
                  </v-col>
                </v-row>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- ===== 섹션 4-1: 자산 변동 추이 (코인 대시보드 섹션 5 대응) ===== -->
        <v-row class="mt-3" dense>
          <v-col cols="12">
            <v-card elevation="2">
              <!-- ⭐ [수정 6] Phase 1 자산 변동 추이 헤더 옵션 완전 동일 적용 -->
              <!-- ⭐ [수정 7] flex-wrap 제거 + overflow-x: auto로 좁은 화면에서 가로 스크롤 -->
              <v-card-title class="py-2 px-4 bg-indigo-darken-2 text-white d-flex align-center" style="gap:4px; overflow-x:auto; flex-wrap: nowrap; min-height: 52px;">
                <v-icon class="mr-2" size="20">mdi-chart-line</v-icon>
                <span class="text-body-1">자산 변동 추이</span>
                <v-btn icon size="x-small" variant="text" color="white" @click.stop="openHelp('assetChart')" class="ml-1">
                  <v-icon size="14">mdi-help-circle-outline</v-icon>
                </v-btn>
                <v-spacer />
                <!-- 스냅샷 갱신 (Phase 1 동일) -->
                <!-- ⭐ [수정 5] 스냅샷 갱신: grey-lighten-1 → amber (전체 보기 활성 색상과 동일) -->
                <v-btn
                  size="x-small"
                  variant="flat"
                  color="amber"
                  class="text-grey-darken-4 chart-view-btn mr-2"
                  @click="refreshStockChart"
                >
                  <v-icon size="14" class="mr-1">mdi-database-refresh</v-icon>
                  스냅샷 갱신
                </v-btn>
                <!-- 기간 버튼 (Phase 1 동일) -->
                <v-btn-toggle v-model="chartPeriod" density="compact" mandatory variant="outlined" size="small" class="chart-period-toggle">
                  <v-btn value="7" size="x-small" :class="chartPeriod === '7' ? 'active-period' : ''">7일</v-btn>
                  <v-btn value="month" size="x-small" :class="chartPeriod === 'month' ? 'active-period' : ''">이번달</v-btn>
                  <v-btn value="year" size="x-small" :class="chartPeriod === 'year' ? 'active-period' : ''">올해</v-btn>
                  <v-btn value="all" size="x-small" :class="chartPeriod === 'all' ? 'active-period' : ''">전체 투자기간</v-btn>
                </v-btn-toggle>
                <!-- 날짜 직접 입력 (Phase 1 동일) -->
                <div class="d-flex align-center ml-3 chart-control-group">
                  <input type="date" v-model="chartCustomStart" class="custom-date-input" @change="applyStockCustomDate" />
                  <span class="mx-1 text-white">~</span>
                  <input type="date" v-model="chartCustomEnd" class="custom-date-input" @change="applyStockCustomDate" />
                </div>
                <!-- 전체보기/스크롤보기 (Phase 1 동일) -->
                <div class="d-flex align-center ml-2 chart-control-group">
                  <v-btn
                    size="x-small"
                    :color="stockChartViewMode === 'full' ? 'amber' : 'grey-lighten-1'"
                    variant="flat"
                    :class="stockChartViewMode === 'full' ? 'text-grey-darken-4' : 'text-grey-darken-2'"
                    class="chart-view-btn"
                    @click="stockChartViewMode = 'full'"
                  >
                    <v-icon size="14" class="mr-1">mdi-fit-to-screen</v-icon>
                    전체 보기
                  </v-btn>
                  <v-divider vertical class="mx-1 chart-divider" />
                  <v-btn
                    size="x-small"
                    :color="stockChartViewMode === 'scroll' ? 'amber' : 'grey-lighten-1'"
                    variant="flat"
                    :class="stockChartViewMode === 'scroll' ? 'text-grey-darken-4' : 'text-grey-darken-2'"
                    class="chart-view-btn"
                    @click="stockChartViewMode = 'scroll'"
                  >
                    <v-icon size="14" class="mr-1">mdi-arrow-left-right</v-icon>
                    스크롤 보기
                  </v-btn>
                </div>
              </v-card-title>
              <v-card-text class="pa-3">
                <!-- ⭐ [수정 3] 코인 대시보드와 완전 동일한 차트 구조 -->
                <div v-if="assetHistory.length > 0 || stats.totalHoldingAmount > 0" class="chart-container">
                  <div
                    class="chart-wrapper-backtest"
                    :class="{ 'scroll-mode': stockChartViewMode === 'scroll' }"
                    :style="stockChartViewMode === 'scroll' ? { width: dynamicStockChartWidth + 'px' } : {}"
                    @mousemove="handleChartHover"
                    @mouseleave="hoveredIndex = -1; showEmptyTooltip = false"
                    @touchstart.prevent="handleChartTouch"
                    @touchmove.prevent="handleChartTouch"
                    @touchend="hoveredIndex = -1; showEmptyTooltip = false"
                  >
                    <svg
                      class="custom-chart"
                      :viewBox="`0 0 ${effectiveWidth} ${svgHeightBacktest}`"
                      preserveAspectRatio="none"
                    >
                      <defs>
                        <linearGradient id="stockDashboardAreaGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                          <stop offset="0%" style="stop-color:#42A5F5;stop-opacity:0.6" />
                          <stop offset="100%" style="stop-color:#42A5F5;stop-opacity:0.1" />
                        </linearGradient>
                      </defs>

                      <template v-if="assetHistory.length > 0">
                        <!-- 영역 채우기 -->
                        <path :d="areaPathBacktest" fill="url(#stockDashboardAreaGradient)" />
                        <!-- 불입금액 막대그래프 (주황) -->
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
                          :x2="effectiveWidth - (stockChartViewMode === 'scroll' ? scrollPaddingRight : svgPaddingRight)"
                          :y2="getYPositionBacktest(maxEvaluation)"
                          stroke="#4CAF50" stroke-width="2" stroke-dasharray="6,4"
                        />
                        <!-- 최저 평가금액 파선 (빨강) -->
                        <line
                          :x1="svgPadding" :y1="getYPositionBacktest(minEvaluation)"
                          :x2="effectiveWidth - (stockChartViewMode === 'scroll' ? scrollPaddingRight : svgPaddingRight)"
                          :y2="getYPositionBacktest(minEvaluation)"
                          stroke="#F44336" stroke-width="2" stroke-dasharray="6,4"
                        />
                        <!-- 차트 바닥 회색 파선 (minBalanceBacktest 위치) -->
                        <line
                          :x1="svgPadding"
                          :y1="getYPositionBacktest(minBalanceBacktest)"
                          :x2="effectiveWidth - (stockChartViewMode === 'scroll' ? scrollPaddingRight : svgPaddingRight)"
                          :y2="getYPositionBacktest(minBalanceBacktest)"
                          stroke="#9E9E9E" stroke-width="1.5" stroke-dasharray="4,4" opacity="0.6"
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
                        <!-- 불입금액 점 -->
                        <circle
                          v-for="(point, index) in chartPointsBacktest"
                          :key="'dep-' + index"
                          :cx="point.x" :cy="getYPositionBacktest(point.depositAmount)"
                          :r="hoveredIndex === index ? 6 : 3"
                          fill="#FF9800" stroke="white" stroke-width="1.5" class="chart-point"
                        />
                      </template>

                      <template v-else>
                        <rect
                          :x="svgPadding" :y="svgHeightBacktest / 2 - 2"
                          :width="effectiveWidth - svgPadding - svgPaddingRight" :height="4"
                          fill="url(#stockDashboardAreaGradient)" opacity="0.3"
                        />
                        <line
                          :x1="svgPadding" :y1="svgHeightBacktest / 2"
                          :x2="effectiveWidth - svgPaddingRight" :y2="svgHeightBacktest / 2"
                          stroke="#FF9800" stroke-width="2" stroke-dasharray="6,4"
                        />
                        <line
                          :x1="svgPadding" :y1="svgHeightBacktest / 2"
                          :x2="effectiveWidth - svgPaddingRight" :y2="svgHeightBacktest / 2"
                          stroke="#1976D2" stroke-width="2.5"
                        />
                        <circle
                          v-for="(point, index) in emptyChartPoints"
                          :key="index"
                          :cx="point.x" :cy="point.y"
                          :r="hoveredIndex === index ? 8 : 4"
                          fill="#1976D2" stroke="white" stroke-width="2" class="chart-point"
                        />
                      </template>
                    </svg>

                    <!-- 우측 라벨 (코인 동일) -->
                    <div class="chart-labels-backtest">
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

                    <!-- 호버 툴팁 (데이터 있을 때) -->
                    <div
                      v-if="hoveredIndex >= 0 && chartPointsBacktest[hoveredIndex] && assetHistory.length > 0"
                      class="chart-tooltip-backtest"
                      :style="{
                        left: (tooltipX > chartWrapperWidth * 0.5 ? tooltipX - 10 : tooltipX + 10) + 'px',
                        top: Math.max(60, Math.min(svgHeightBacktest - 80, tooltipY)) + 'px',
                        transform: tooltipX > chartWrapperWidth * 0.5 ? 'translateX(-100%) translateY(-50%)' : 'translateY(-50%)'
                      }"
                    >
                      <div class="font-weight-bold mb-1">{{ chartPointsBacktest[hoveredIndex]?.date || '-' }}</div>
                      <div style="color: #64B5F6;">평가금액: {{ formatCurrency(chartPointsBacktest[hoveredIndex]?.evaluationAmount) }}</div>
                      <div style="color: #FFB74D;">불입금액: {{ formatCurrency(chartPointsBacktest[hoveredIndex]?.depositAmount) }}</div>
                      <div :class="(chartPointsBacktest[hoveredIndex]?.profitRate || 0) >= 0 ? 'text-success' : 'text-error'">
                        수익률: {{ (chartPointsBacktest[hoveredIndex]?.profitRate || 0) >= 0 ? '+' : '' }}{{ Number(chartPointsBacktest[hoveredIndex]?.profitRate || 0).toFixed(2) }}%
                      </div>
                      <div :class="(chartPointsBacktest[hoveredIndex]?.profitAmount || 0) >= 0 ? 'text-success' : 'text-error'">
                        수익금액: {{ (chartPointsBacktest[hoveredIndex]?.profitAmount || 0) >= 0 ? '+' : '' }}{{ formatCurrency(chartPointsBacktest[hoveredIndex]?.profitAmount || 0) }}
                      </div>
                    </div>

                    <!-- 호버 툴팁 (데이터 없을 때) -->
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

                  <!-- 하단 날짜 표시 (코인 동일) -->
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

                <div v-else class="text-center py-6 text-grey-darken-2">
                  <v-icon size="48" class="mb-2" color="grey">mdi-chart-line-variant</v-icon>
                  <div class="text-body-1">거래 이력이 없습니다</div>
                  <div class="text-caption">매수 거래가 발생하면 차트가 표시됩니다</div>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- ===== 섹션 5: 종목별 성과 + 최근 거래 + 시스템 알림 (Phase 1 섹션 6 대응) ===== -->
        <v-row class="mt-3 mb-4" dense>
          <!-- 종목별 성과 -->
          <v-col cols="12" md="4">
            <v-card class="fill-height" elevation="2">
              <v-card-title class="py-2 px-4 bg-indigo-darken-1 text-white" style="min-height: 48px;">
                <v-icon class="mr-2" size="20">mdi-podium</v-icon>
                <span class="text-body-1">종목별 성과</span>
                <v-btn icon size="x-small" variant="text" color="white" @click.stop="openHelp('stockPerformance')" class="ml-1">
                  <v-icon size="14">mdi-help-circle-outline</v-icon>
                </v-btn>
              </v-card-title>
              <v-card-text class="pa-2">
                <v-list v-if="stockPerformance.length > 0" density="compact">
                  <!-- ⭐ [수정 1] 코인별 성과와 동일 구조: "409820 TIGER..." 한줄 + "n회 거래" -->
                  <v-list-item v-for="perf in stockPerformance.slice(0, 5)" :key="perf.stockCode" class="px-2 py-1">
                    <div class="d-flex align-center justify-space-between w-100">
                      <div>
                        <span class="text-body-2 font-weight-medium text-grey-darken-4">
                          {{ perf.stockCode }} {{ perf.stockName }}
                        </span>
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
                <div v-else class="d-flex align-center justify-center text-grey-darken-2 text-body-2" style="height: 100%; min-height: 150px;">
                  매도 완료된 거래가 없습니다
                </div>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- 최근 거래 -->
          <v-col cols="12" md="4">
            <v-card class="fill-height" elevation="2">
              <v-card-title class="py-2 px-4 bg-indigo-darken-1 text-white d-flex align-center" style="min-height: 48px;">
                <v-icon class="mr-2" size="20">mdi-history</v-icon>
                <span class="text-body-1">최근 거래</span>
                <v-btn icon size="x-small" variant="text" color="white" @click.stop="openHelp('recentTransactions')" class="ml-1">
                  <v-icon size="14">mdi-help-circle-outline</v-icon>
                </v-btn>
                <v-spacer />
                <v-btn size="small" variant="flat" color="amber" class="text-grey-darken-4" @click="$router.push('/stock-transactions')">
                  전체 거래 내역 보기 →
                </v-btn>
              </v-card-title>
              <v-card-text class="pa-2">
                <v-list v-if="recentTransactions.length > 0" density="compact">
                  <v-list-item v-for="tx in recentTransactions.slice(0, 5)" :key="tx.transactionId" class="px-2 py-1">
                    <div class="d-flex align-center justify-space-between w-100">
                      <!-- ⭐ [수정 2] 화살표: 매도(SOLD)=↑amber, 매수(BUY)=↓indigo (코인 대시보드 동일 기준) -->
                    <div class="d-flex align-center" style="min-width: 85px;">
                        <v-avatar
                          :color="tx.status === 'SOLD' ? 'amber-darken-2' : 'indigo'"
                          size="24" class="mr-3"
                        >
                          <v-icon size="14" color="white">
                            {{ tx.status === 'SOLD' ? 'mdi-arrow-up' : 'mdi-arrow-down' }}
                          </v-icon>
                        </v-avatar>
                        <div>
                          <span class="text-body-2 font-weight-medium text-grey-darken-4">{{ tx.stockCode }}</span>
                          <div class="text-caption text-grey-darken-1">{{ tx.stockName }}</div>
                        </div>
                      </div>
                      <div class="flex-grow-1 px-2 text-center">
                        <!-- ⭐ [수정 2] 날짜 형식: formatDate → formatDateForTx (4/7 20:20 형식) -->
                        <div class="text-caption text-grey-darken-2">
                          {{ formatCurrency(tx.totalAmount) }} · {{ formatDateForTx(tx.createdAt) }}
                        </div>
                        <div
                          v-if="tx.status === 'SOLD' && tx.profitLoss != null"
                          :class="tx.profitLoss >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2'"
                          class="text-caption font-weight-medium"
                        >
                          {{ tx.profitLoss >= 0 ? '+' : '' }}{{ formatCurrency(tx.profitLoss) }}
                        </div>
                      </div>
                      <v-chip
                        :color="tx.status === 'SOLD' ? 'orange' : 'indigo'"
                        size="x-small" variant="flat"
                        style="min-width: 60px; justify-content: center;"
                      >
                        {{ tx.status === 'SOLD' ? '매도완료' : '보유중' }}
                      </v-chip>
                    </div>
                  </v-list-item>
                </v-list>
                <div v-else class="d-flex align-center justify-center text-grey-darken-2 text-body-2" style="height: 100%; min-height: 150px;">
                  거래 내역이 없습니다
                </div>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- 시스템 알림 -->
          <v-col cols="12" md="4">
            <v-card class="fill-height" elevation="2">
              <v-card-title class="py-2 px-4 bg-red-darken-1 text-white d-flex align-center" style="min-height: 48px;">
                <v-icon class="mr-2" size="20">mdi-bell-alert</v-icon>
                <span class="text-body-1">시스템 알림</span>
                <v-btn icon size="x-small" variant="text" color="white" @click.stop="openHelp('systemAlert')" class="ml-1">
                  <v-icon size="14">mdi-help-circle-outline</v-icon>
                </v-btn>
              </v-card-title>
              <!-- ⭐ [수정] 코인 대시보드와 동일: 릴리즈노트 배너 상단 추가 -->
              <!-- ⭐ [수정 4] fill-height로 카드 전체 높이 사용 → 상하 중앙 정렬 보장 -->
              <v-card-text class="pa-3 d-flex flex-column fill-height" style="min-height: 150px;">
                <!-- 최신 주식 릴리즈노트 1건 (코인 대시보드와 동일 스타일) -->
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

                <!-- 시스템 알림 목록 -->
                <div v-if="systemAlerts.length > 0" class="d-flex flex-column align-center justify-center flex-grow-1">
                  <div v-for="(alert, i) in systemAlerts" :key="i" class="d-flex align-center mb-2">
                    <v-icon
                      :color="alert.color === 'error' ? 'red' : alert.color === 'warning' ? 'orange' : 'info'"
                      class="mr-2"
                    >
                      {{ alert.color === 'error' ? 'mdi-alert-circle' : alert.color === 'warning' ? 'mdi-alert' : 'mdi-information' }}
                    </v-icon>
                    <span class="font-weight-medium text-grey-darken-3">{{ alert.message }}</span>
                    <!-- ⭐ [수정 8] 등록하기 버튼: alert.color(노란 warning) → indigo-darken-1로 가독성 개선 -->
                    <v-btn v-if="alert.action" size="x-small" color="indigo-darken-1" variant="flat" class="ml-2 text-white" @click="$router.push(alert.action)">
                      {{ alert.actionLabel }}
                    </v-btn>
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

      </v-container>
    </v-main>

    <!-- ⭐ [수정 7] 매수 조건 상세 지표 다이얼로그 (코인 대시보드 동일 구조) -->
    <v-dialog v-model="showStockIndicatorDialog" max-width="700">
      <v-card>
        <v-card-title class="bg-teal-darken-2 text-white d-flex align-center">
          <v-icon class="mr-2">mdi-chart-box</v-icon>
          {{ selectedStockIndicator ? getStockName(selectedStockIndicator.stockCode) : '' }} 매수 조건 상세
          <v-spacer />
          <v-btn icon variant="text" color="white" @click="showStockIndicatorDialog = false">
            <v-icon>mdi-close</v-icon>
          </v-btn>
        </v-card-title>
        <v-card-text class="pa-4" v-if="selectedStockIndicator">
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
                  MA{{ stockSettings?.basePeriod || 20 }} 하락률
                </td>
                <td class="text-right font-weight-medium text-no-wrap">
                  {{ Number(stockSettings?.buyThresholdPct || -3) }}% 이하
                </td>
                <td class="text-right text-no-wrap"
                  :class="selectedStockIndicator.dropRate <= Number(stockSettings?.buyThresholdPct || -3) ? 'text-teal font-weight-bold' : ''">
                  {{ selectedStockIndicator.dropRate?.toFixed(2) || '0.00' }}%
                </td>
                <td class="text-center">
                  <v-icon
                    :color="selectedStockIndicator.dropRate <= Number(stockSettings?.buyThresholdPct || -3) ? 'teal' : 'grey'"
                    size="20">
                    {{ selectedStockIndicator.dropRate <= Number(stockSettings?.buyThresholdPct || -3) ? 'mdi-check-circle' : 'mdi-circle-outline' }}
                  </v-icon>
                </td>
              </tr>
              <!-- 현재가 -->
              <tr>
                <td class="text-no-wrap">
                  <v-icon size="16" class="mr-1">mdi-cash</v-icon>
                  현재가
                </td>
                <td class="text-right">-</td>
                <td class="text-right font-weight-bold text-no-wrap">
                  {{ formatCurrency(selectedStockIndicator.currentPrice) }}
                </td>
                <td class="text-center">-</td>
              </tr>
              <!-- MA20 -->
              <tr>
                <td class="text-no-wrap">
                  <v-icon size="16" class="mr-1">mdi-chart-line</v-icon>
                  MA{{ stockSettings?.basePeriod || 20 }}
                </td>
                <td class="text-right">-</td>
                <td class="text-right text-no-wrap">
                  {{ formatCurrency(selectedStockIndicator.maValue || 0) }}
                </td>
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
                  {{ formatCurrency(selectedStockIndicator.buyPrice) }}
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
                <td class="text-right text-no-wrap"
                  :class="selectedStockIndicator.canBuy ? 'text-teal font-weight-bold' : 'text-orange'">
                  {{ selectedStockIndicator.canBuy ? '조건 충족!' : Math.abs(selectedStockIndicator.remainingDrop || 0).toFixed(2) + '%' }}
                </td>
                <td class="text-center">
                  <v-icon :color="selectedStockIndicator.canBuy ? 'teal' : 'orange'" size="20">
                    {{ selectedStockIndicator.canBuy ? 'mdi-check-circle' : 'mdi-clock-outline' }}
                  </v-icon>
                </td>
              </tr>
            </tbody>
          </v-table>
          <v-divider class="my-3" />
          <div class="text-caption text-grey">
            <v-icon size="14" class="mr-1">mdi-information-outline</v-icon>
            매수 조건: MA{{ stockSettings?.basePeriod || 20 }} 대비 {{ Number(stockSettings?.buyThresholdPct || -3) }}% 이하 하락 시 매수 신호 발생
          </div>
          <!-- 더미 데이터 안내 -->
          <v-alert v-if="selectedStockIndicator.currentPrice === 0" type="info" variant="tonal" density="compact" class="mt-3 text-caption">
            KIS API 키 등록 및 봇 실행 후 실제 지표가 표시됩니다. 현재는 미리보기 상태입니다.
          </v-alert>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn color="primary" variant="flat" @click="showStockIndicatorDialog = false">확인</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 도움말 다이얼로그 (Phase 1 동일 구조) -->
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'
import { useAuthStore } from '@/stores/auth'
// ⭐ [수정 3] stockInfoApi 추가
import { stockDashboardApi, stockTransactionApi, stockSettingApi, stockInfoApi } from '@/api/stock'
import api from '@/api/index'

const authStore = useAuthStore()
const sidebarRef = ref()
const isRefreshing = ref(false)
const statsLoading = ref(false)
// ⭐ [수정 8] botLoading → botToggleLoading + 봇 관련 state 추가 (Phase 1 패턴)
const botToggleLoading = ref(false)
const botEnabled = ref(false)
const countdownSeconds = ref(0)
let countdownInterval: ReturnType<typeof setInterval> | null = null
const botStatus = ref({
  isRunning: false,
  lastExecutionTime: '',
  nextExecutionTime: '',
  marketOpen: false,
  emergencyStop: false,
  secondsUntilNextExecution: 0,
})
const exchangeLoading = ref(false)
const currentTime = ref('')
const currentTimeShort = ref('')

// ── 도움말 시스템 (Phase 1 동일 패턴) ─────────────────────
const showHelpDialog = ref(false)
const currentHelp = ref({ title: '', content: '' })

// ── 데이터 ─────────────────────────────────────────────────
const stats = ref({
  totalHoldingAmount: 0, totalCurrentValue: 0,
  totalProfitLoss: 0, totalProfitLossPct: 0,
  realizedProfitLoss: 0, soldCount: 0,
  totalBuyCount: 0, totalSellCount: 0, currentHoldingCount: 0,
  todayBuyAmount: 0, todaySellAmount: 0, todayBuyCount: 0, todaySellCount: 0,
  dailyLimitAmount: 0, remainingDailyLimit: 0,
  holdingDaysWarningCount: 0, holdingDaysUrgentCount: 0,
  botEnabled: false, marketOpen: false,
})

const exchange = ref<{
  exchangeRate: number | null
  exchangeRateChange: number | null
  exchangeRateChangePct: number | null
  date: string | null
  source: string
}>({ exchangeRate: null, exchangeRateChange: null, exchangeRateChangePct: null, date: null, source: 'fallback' })

const exchangeLastFetchedAt = ref<string | null>(null)
const exchangeRetryCountdown = ref(30)
let exchangeRetryTimer: ReturnType<typeof setTimeout> | null = null
let exchangeCountdownTimer: ReturnType<typeof setInterval> | null = null
// ⭐ [수정 4] 5분 자동 갱신
const exchangeAutoRefreshSeconds = ref(300) // 5분 카운트다운
let exchangeAutoRefreshTimer: ReturnType<typeof setInterval> | null = null

const holdings = ref<any[]>([])
const recentTransactions = ref<any[]>([])
const stockSettings = ref<any>(null)
// ⭐ [수정 3] 종목코드 → 종목명 매핑 (stock_info 테이블 기반)
const stockNameMap = ref<Record<string, string>>({})
const hasKisApiKey = ref(false)
const tomorrowHoliday = ref(false)

// ⭐ [수정 3] KIS 계좌 실제 잔고
const accountLoading = ref(false)
const kisAccount = ref({ krwBalance: 0, stockEvaluation: 0, totalAsset: 0, holdings: [] as any[] })
const hoveredKisSlice = ref(-1)

// ⭐ [수정 1] 샘플 데이터 모드 (KIS API 키 미등록 시 UI 확인용)
const kisAccountSampleMode = ref(false)

// 샘플 데이터 (로컬 개발 환경용 - 파이차트/잔고 UI 확인)
const KIS_ACCOUNT_SAMPLE = {
  krwBalance: 450000,
  stockEvaluation: 150030,
  totalAsset: 600030,
  holdings: [
    { stockCode: '409820', stockName: 'TIGER 미국나스닥100레버리지(합성)', evaluation: 100020 },
    { stockCode: '409810', stockName: 'KODEX 미국나스닥100레버리지(합성H)', evaluation: 50010 },
  ]
}

// 실제 표시할 계좌 데이터 (샘플 모드 여부에 따라)
const displayKisAccount = computed(() =>
  kisAccountSampleMode.value && kisAccount.value.totalAsset === 0
    ? KIS_ACCOUNT_SAMPLE
    : kisAccount.value
)

// ⭐ [수정 4] 매수 조건 카드
const indicatorsLoading = ref(false)
const stockIndicators = ref<any[]>([])
// ⭐ [수정 7] 상세 지표 팝업
const showStockIndicatorDialog = ref(false)
const selectedStockIndicator = ref<any>(null)

function openStockIndicatorDetail(item: any) {
  selectedStockIndicator.value = item
  showStockIndicatorDialog.value = true
}

// ⭐ [수정 5] 수익 현황 (Phase 1 profitSummary 동일 구조)
const stockProfit = ref({
  todayProfit: 0, todayProfitPct: 0,
  unrealizedProfit: 0, unrealizedProfitPct: 0,
  realizedProfit: 0, totalProfit: 0,
})

// ⭐ [수정 6] 차트 뷰 모드 + 날짜 커스텀
const stockChartViewMode = ref<'full' | 'scroll'>('full')
const chartCustomStart = ref('')
const chartCustomEnd = ref('')

// ⭐ [추가] 릴리즈노트 (코인 대시보드 동일 패턴)
const latestReleaseNote = ref<any>(null)

// ⭐ [수정 3] 코인 대시보드 자산 변동 추이 완전 동일 구현
const chartPeriod = ref('all')  // 기본값 'all' (코인과 동일)
const hoveredIndex = ref(-1)
const tooltipX = ref(0)
const tooltipY = ref(0)
const chartWrapperWidth = ref(800)
const showEmptyTooltip = ref(false)
const initialAsset = ref(0)

// 코인 대시보드 동일 상수
const svgWidth = 800
const svgPadding = 30
const svgPaddingRight = 120
const scrollPaddingRight = 220
const svgHeightBacktest = 350

// 코인 대시보드 동일: assetHistory (스냅샷 기반)
const assetHistory = ref<any[]>([])

// 스크롤/전체보기 모드
// stockChartViewMode 는 이미 선언되어 있으므로 사용

// effectiveWidth 계산 (코인 동일)
const effectiveWidth = computed(() =>
  stockChartViewMode.value === 'scroll' ? dynamicStockChartWidth.value : svgWidth
)
const dynamicStockChartWidth = computed(() => {
  const pointCount = assetHistory.value.length || emptyPeriodDates.value.length
  return Math.max(svgWidth, pointCount * 25 + svgPadding + scrollPaddingRight)
})

// Y축 최대/최소 (코인 동일)
const maxBalanceBacktest = computed(() => {
  if (!assetHistory.value.length) return initialAsset.value
  const maxEval = Math.max(...assetHistory.value.map((d: any) => d.evaluationAmount || d.balance || 0))
  const maxDeposit = Math.max(...assetHistory.value.map((d: any) => d.depositAmount || initialAsset.value))
  return Math.max(maxEval, maxDeposit)
})
const minBalanceBacktest = computed(() => {
  // ⭐ [수정 Q2] 코인 대시보드와 완전 동일한 계산식으로 통일
  // 이유: 0.90 배율이 너무 낮아 차트 하단 여백이 과도하게 생김
  // 코인 대시보드: Math.floor(min(minEval, minDeposit) * 0.98) → 2% 하단 여백
  if (!assetHistory.value.length) return 0
  const minEval = Math.min(...assetHistory.value.map((d: any) => d.evaluationAmount || d.balance || 0))
  const minDeposit = Math.min(...assetHistory.value.map((d: any) => d.depositAmount || initialAsset.value))
  const minValue = Math.min(minEval, minDeposit)
  if (minValue <= 0) return 0
  return Math.floor(minValue * 0.98)
})

const maxEvaluation = computed(() =>
  assetHistory.value.length
    ? Math.max(...assetHistory.value.map((d: any) => d.evaluationAmount || d.balance || 0))
    : initialAsset.value
)
const minEvaluation = computed(() =>
  assetHistory.value.length
    ? Math.min(...assetHistory.value.map((d: any) => d.evaluationAmount || d.balance || 0))
    : initialAsset.value
)
const latestDepositAmount = computed(() => {
  if (!assetHistory.value.length) return initialAsset.value
  return assetHistory.value[assetHistory.value.length - 1].depositAmount || initialAsset.value
})
const latestEvaluationAmount = computed(() => {
  if (!assetHistory.value.length) return initialAsset.value
  const last = assetHistory.value[assetHistory.value.length - 1]
  return last.evaluationAmount || last.balance || initialAsset.value
})
const barWidth = computed(() => {
  const total = assetHistory.value.length
  if (total <= 1) return 20
  const chartW = effectiveWidth.value - svgPadding - svgPaddingRight
  return Math.max(4, Math.min(20, (chartW / total) * 0.6))
})

const getYPositionBacktest = (balance: number) => {
  const max = maxBalanceBacktest.value
  const min = minBalanceBacktest.value
  const range = max - min || 1
  return svgPadding + ((max - balance) / range) * (svgHeightBacktest - svgPadding * 2)
}

const chartPointsBacktest = computed(() => {
  if (!assetHistory.value.length) return []
  const total = assetHistory.value.length
  const rightPad = stockChartViewMode.value === 'scroll' ? scrollPaddingRight : svgPaddingRight
  const chartW = effectiveWidth.value - svgPadding - rightPad
  return assetHistory.value.map((d: any, index: number) => ({
    x: svgPadding + (index / (total - 1 || 1)) * chartW,
    y: getYPositionBacktest(d.evaluationAmount || d.balance),
    balance: d.evaluationAmount || d.balance || 0,
    evaluationAmount: d.evaluationAmount || d.balance || 0,
    depositAmount: d.depositAmount || initialAsset.value,
    date: d.date,
    profitRate: d.profitRate || 0,
    profitAmount: d.profitAmount || 0,
  }))
})

const linePathBacktest = computed(() =>
  chartPointsBacktest.value.length
    ? chartPointsBacktest.value.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ')
    : ''
)
const depositLinePathBacktest = computed(() =>
  chartPointsBacktest.value.length
    ? chartPointsBacktest.value.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${getYPositionBacktest(p.depositAmount)}`).join(' ')
    : ''
)
const areaPathBacktest = computed(() => {
  if (!chartPointsBacktest.value.length) return ''
  const pts = chartPointsBacktest.value
  const bottomY = svgHeightBacktest - svgPadding
  return `M ${pts[0].x} ${bottomY} L ${pts.map(p => `${p.x} ${p.y}`).join(' L ')} L ${pts[pts.length - 1].x} ${bottomY} Z`
})

// 차트 날짜 (코인 동일)
const chartEndDate = computed(() =>
  assetHistory.value.length
    ? assetHistory.value[assetHistory.value.length - 1]?.date || ''
    : ''
)

// 기간 선택 시작일 계산 (코인 동일)
const chartPeriodStartDate = computed(() => {
  const today = new Date()
  let startDate = new Date()
  switch (chartPeriod.value) {
    case '7': startDate.setDate(today.getDate() - 7); break
    case 'month': startDate = new Date(today.getFullYear(), today.getMonth(), 1); break
    case 'year': startDate = new Date(today.getFullYear(), 0, 1); break
    case 'custom': return chartCustomStart.value || formatTodayDate()
    default:
      if (recentTransactions.value.length > 0) {
        const sorted = [...recentTransactions.value].sort((a: any, b: any) =>
          new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
        )
        startDate = new Date(sorted[0].createdAt)
      } else { startDate.setFullYear(today.getFullYear() - 1) }
  }
  return `${startDate.getFullYear()}-${String(startDate.getMonth() + 1).padStart(2, '0')}-${String(startDate.getDate()).padStart(2, '0')}`
})

const firstTradeDate = computed(() => {
  if (!recentTransactions.value.length) return null
  const sorted = [...recentTransactions.value].sort((a: any, b: any) =>
    new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
  )
  return sorted[0]?.createdAt ? new Date(sorted[0].createdAt) : null
})
const isBeforeFirstTrade = computed(() => {
  if (!firstTradeDate.value) return false
  return new Date(chartPeriodStartDate.value) < firstTradeDate.value
})
const displayChartStartDate = computed(() => {
  if (assetHistory.value.length > 0) return assetHistory.value[0]?.date || ''
  if (emptyPeriodDates.value.length > 0) return emptyPeriodDates.value[0].date
  if (firstTradeDate.value) {
    const d = firstTradeDate.value
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  }
  return formatTodayDate()
})

const emptyPeriodDates = computed(() => {
  const today = new Date(); today.setHours(0, 0, 0, 0)
  let startDate: Date
  if (firstTradeDate.value) {
    const ps = new Date(chartPeriodStartDate.value); ps.setHours(0, 0, 0, 0)
    const ft = new Date(firstTradeDate.value); ft.setHours(0, 0, 0, 0)
    startDate = ps > ft ? new Date(ps) : new Date(ft)
  } else { startDate = new Date(today) }
  const dates: Array<{ date: string; balance: number; profitRate: number; hasData: boolean }> = []
  const cur = new Date(startDate.getTime())
  while (cur <= today) {
    dates.push({
      date: `${cur.getFullYear()}-${String(cur.getMonth() + 1).padStart(2, '0')}-${String(cur.getDate()).padStart(2, '0')}`,
      balance: initialAsset.value, profitRate: 0, hasData: false
    })
    cur.setDate(cur.getDate() + 1)
  }
  return dates
})

const emptyChartPoints = computed(() => {
  if (!emptyPeriodDates.value.length) return []
  const total = emptyPeriodDates.value.length
  const rightPad = stockChartViewMode.value === 'scroll' ? scrollPaddingRight : svgPaddingRight
  const cw = effectiveWidth.value - svgPadding - rightPad
  return emptyPeriodDates.value.map((d, i) => ({
    x: svgPadding + (i / (total - 1 || 1)) * cw,
    y: svgHeightBacktest / 2,
    date: d.date, balance: d.balance, hasData: d.hasData
  }))
})
const emptyHoveredData = computed(() =>
  hoveredIndex.value >= 0 && emptyPeriodDates.value.length
    ? emptyPeriodDates.value[hoveredIndex.value] || null
    : null
)

// 라벨 위치 계산 (코인 동일)
const getLabelPositionBacktest = (balance: number) => {
  const max = maxBalanceBacktest.value, min = minBalanceBacktest.value
  const range = max - min || 1
  const paddingPercent = (svgPadding / svgHeightBacktest) * 100
  const usableHeight = 100 - paddingPercent * 2
  return paddingPercent + ((max - balance) / range) * usableHeight
}
const getAdjustedLabelPosition = (type: string) => {
  const positions = [
    { type: 'max', raw: getLabelPositionBacktest(maxEvaluation.value) },
    { type: 'evaluation', raw: getLabelPositionBacktest(latestEvaluationAmount.value) },
    { type: 'deposit', raw: getLabelPositionBacktest(latestDepositAmount.value) },
    { type: 'min', raw: getLabelPositionBacktest(minEvaluation.value) },
    { type: 'floor', raw: getLabelPositionBacktest(minBalanceBacktest.value) },
  ].sort((a, b) => a.raw - b.raw)
  const minGap = 4
  for (let i = 1; i < positions.length; i++) {
    if (positions[i].raw - positions[i - 1].raw < minGap)
      positions[i].raw = positions[i - 1].raw + minGap
  }
  return positions.find(p => p.type === type)?.raw || 0
}
const getPointColorBacktest = (evaluationAmount: number) => {
  const deposit = latestDepositAmount.value
  if (evaluationAmount > deposit * 1.01) return '#4CAF50'
  if (evaluationAmount < deposit * 0.99) return '#F44336'
  return '#1976D2'
}

function formatTodayDate() {
  const t = new Date()
  return `${t.getFullYear()}-${String(t.getMonth() + 1).padStart(2, '0')}-${String(t.getDate()).padStart(2, '0')}`
}

// 코인과 동일한 호버 핸들러
const handleChartHover = (event: MouseEvent) => {
  const wrapper = event.currentTarget as HTMLElement
  const rect = wrapper.getBoundingClientRect()
  const mouseX = event.clientX - rect.left
  const isScrollMode = stockChartViewMode.value === 'scroll'
  let targetIndex = 0
  const total = assetHistory.value.length

  if (total === 0) {
    const emptyTotal = emptyPeriodDates.value.length
    if (emptyTotal > 0) {
      if (isScrollMode) {
        const svgEl = wrapper.querySelector('svg')
        if (svgEl) {
          const svgRect = svgEl.getBoundingClientRect()
          const svgMouseX = event.clientX - svgRect.left
          const chartW = effectiveWidth.value - svgPadding - svgPaddingRight
          const ratio = Math.max(0, Math.min(1, (svgMouseX - svgPadding * (svgRect.width / effectiveWidth.value)) / (chartW * (svgRect.width / effectiveWidth.value))))
          targetIndex = Math.round(ratio * (emptyTotal - 1))
        }
      } else {
        const chartW = effectiveWidth.value - svgPadding - svgPaddingRight
        const ratio = Math.max(0, Math.min(1, (mouseX - svgPadding) / chartW))
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
    const svgEl = wrapper.querySelector('svg')
    if (svgEl) {
      const svgRect = svgEl.getBoundingClientRect()
      const scaleX = svgRect.width / effectiveWidth.value
      let minDist = Infinity, closestIdx = 0
      chartPointsBacktest.value.forEach((point, index) => {
        const screenX = svgRect.left + (point.x * scaleX) - rect.left
        const dist = Math.abs(mouseX - screenX)
        if (dist < minDist) { minDist = dist; closestIdx = index }
      })
      targetIndex = closestIdx
    }
  } else {
    const rightPad = svgPaddingRight
    const chartW = effectiveWidth.value - svgPadding - rightPad
    const ratio = Math.max(0, Math.min(1, (mouseX - svgPadding * (rect.width / effectiveWidth.value)) / (chartW * (rect.width / effectiveWidth.value))))
    targetIndex = Math.round(ratio * (total - 1))
  }

  hoveredIndex.value = Math.max(0, Math.min(total - 1, targetIndex))
  if (chartPointsBacktest.value[hoveredIndex.value])
    tooltipY.value = chartPointsBacktest.value[hoveredIndex.value].y
  tooltipX.value = mouseX
  chartWrapperWidth.value = rect.width
}
const handleChartTouch = (event: TouchEvent) => {
  const touch = event.touches[0]; if (!touch) return
  handleChartHover({ clientX: touch.clientX, clientY: touch.clientY, currentTarget: event.currentTarget, target: event.target } as unknown as MouseEvent)
}

async function fetchAssetHistory() {
  try {
    initialAsset.value = stats.value.dailyLimitAmount || 1000000

    // ⭐ [수정 Q6] 1단계: 주식 스냅샷 API 우선 조회 (코인 대시보드와 동일 패턴)
    // 이유: 스냅샷 기반으로 전환하여 정확한 일별 자산 변동 추이 제공
    try {
      let snapshotRes
      if (chartPeriod.value === 'custom' && chartCustomStart.value && chartCustomEnd.value) {
        snapshotRes = await api.get(`/stock/dashboard/profit/snapshots?period=custom&start=${chartCustomStart.value}&end=${chartCustomEnd.value}`)
      } else {
        snapshotRes = await api.get(`/stock/dashboard/profit/snapshots?period=${chartPeriod.value}`)
      }
      const snapshots = snapshotRes.data?.data ?? snapshotRes.data ?? []
      if (Array.isArray(snapshots) && snapshots.length > 0) {
        assetHistory.value = snapshots.map((s: any) => ({
          date: s.date,
          balance: parseFloat(s.evaluationAmount) || 0,
          evaluationAmount: parseFloat(s.evaluationAmount) || 0,
          depositAmount: parseFloat(s.depositAmount) || 0,
          profitAmount: parseFloat(s.profitAmount) || 0,
          profitRate: parseFloat(s.profitRate) || 0,
        }))
        return // 스냅샷 있으면 여기서 종료
      }
    } catch (snapshotErr) {
      console.warn('[주식 차트] 스냅샷 API 실패, 거래 기반으로 폴백:', snapshotErr)
    }

    // ⭐ [수정 Q6] 2단계: 폴백 - 거래 내역 기반 계산 (스냅샷 없을 때)
    const now = new Date()
    let startDate = new Date()

    const allTxsSorted = [...recentTransactions.value].sort((a: any, b: any) =>
      new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
    )
    const firstTxDate = allTxsSorted.length > 0 ? new Date(allTxsSorted[0].createdAt) : null

    switch (chartPeriod.value) {
      case '7': startDate.setDate(now.getDate() - 7); break
      case 'month': startDate = new Date(now.getFullYear(), now.getMonth(), 1); break
      case 'year': startDate = new Date(now.getFullYear(), 0, 1); break
      case 'custom':
        if (chartCustomStart.value && chartCustomEnd.value) {
          startDate = new Date(chartCustomStart.value)
        }
        break
      default: // 'all'
        startDate = firstTxDate ?? new Date(now.getFullYear() - 1, now.getMonth(), now.getDate())
    }

    const txs = recentTransactions.value.filter((tx: any) =>
      new Date(tx.createdAt) >= startDate
    )

    if (!txs.length) { assetHistory.value = []; return }

    const dailyMap = new Map<string, { evaluation: number; deposit: number }>()
    let runningProfit = 0

    const sorted = [...txs].sort((a: any, b: any) =>
      new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
    )

    sorted.forEach((tx: any) => {
      const dateKey = new Date(tx.createdAt).toISOString().split('T')[0]
      if (tx.status === 'SOLD' && tx.profitLoss != null) {
        runningProfit += Number(tx.profitLoss)
      }
      dailyMap.set(dateKey, {
        evaluation: initialAsset.value + runningProfit,
        deposit: initialAsset.value,
      })
    })

    assetHistory.value = Array.from(dailyMap.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([date, val]) => ({
        date,
        balance: val.evaluation,
        evaluationAmount: val.evaluation,
        depositAmount: initialAsset.value,
        profitAmount: val.evaluation - initialAsset.value,
        profitRate: initialAsset.value > 0 ? ((val.evaluation - initialAsset.value) / initialAsset.value) * 100 : 0,
      }))
  } catch (e) {
    console.error('[주식 차트] 자산 이력 로드 실패', e)
    assetHistory.value = []
  }
}

// ⭐ [수정 6] 보유 종목 파이차트 데이터 (코인 대시보드 portfolio3dSlices 단순화 버전)
const PIE_COLORS = ['#4CAF50','#2196F3','#FF9800','#9C27B0','#F44336','#00BCD4','#FF5722','#607D8B']

// ⭐ [수정 3] KIS 포트폴리오 파이차트 - Phase 1 portfolioLegend/3dSlices/3dSides 완전 동일 구조
const portfolioColors = ['#8BC34A', '#5C6BC0', '#AB47BC', '#FF7043', '#26A69A', '#FFA726', '#42A5F5', '#EC407A']

const kisPortfolioLegend = computed(() => {
  // ⭐ [수정 1] 샘플 모드 반영
  const account = kisAccountSampleMode.value && kisAccount.value.totalAsset === 0
    ? KIS_ACCOUNT_SAMPLE
    : kisAccount.value
  const total = account.totalAsset
  if (total <= 0) return []
  const items: Array<{ label: string; percent: number; color: string; value: number }> = []
  if (account.krwBalance > 0) {
    items.push({ label: 'KRW', percent: (account.krwBalance / total) * 100, color: portfolioColors[0], value: account.krwBalance })
  }
  account.holdings.forEach((h: any, i: number) => {
    const eval_ = Number(h.evaluation || 0)
    if (eval_ > 0) {
      items.push({ label: h.stockCode, percent: (eval_ / total) * 100, color: portfolioColors[(i + 1) % portfolioColors.length], value: eval_ })
    }
  })
  return items
})

// Phase 1 portfolio3dSlices 동일 로직
const kisPortfolio3dSlices = computed(() => {
  const items = kisPortfolioLegend.value
  if (!items.length) return []
  const cx = 150, cy = 100, rx = 95, ry = 76
  let startAngle = -90
  return items.map(item => {
    const angle = (item.percent / 100) * 360
    const endAngle = startAngle + angle
    const startRad = (startAngle * Math.PI) / 180
    const endRad = (endAngle * Math.PI) / 180
    const x1 = cx + rx * Math.cos(startRad)
    const y1 = cy + ry * Math.sin(startRad)
    const x2 = cx + rx * Math.cos(endRad)
    const y2 = cy + ry * Math.sin(endRad)
    const largeArc = angle > 180 ? 1 : 0
    let path: string
    if (angle >= 359.99) {
      const midRad = (startRad + endRad) / 2
      const mx = cx + rx * Math.cos(midRad)
      const my = cy + ry * Math.sin(midRad)
      path = `M ${cx} ${cy} L ${x1} ${y1} A ${rx} ${ry} 0 0 1 ${mx} ${my} A ${rx} ${ry} 0 0 1 ${x2} ${y2} Z`
    } else {
      path = `M ${cx} ${cy} L ${x1} ${y1} A ${rx} ${ry} 0 ${largeArc} 1 ${x2} ${y2} Z`
    }
    const midAngle = ((startAngle + endAngle) / 2) * Math.PI / 180
    const innerR = 50
    const midRx = (rx + innerR) / 2
    const midRy = (ry + (innerR * ry / rx)) / 2
    const labelX = cx + midRx * Math.cos(midAngle)
    const labelY = cy + midRy * Math.sin(midAngle)
    startAngle = endAngle
    return { path, color: item.color, label: item.label, percent: item.percent, labelX, labelY }
  })
})

// Phase 1 portfolio3dSides 동일 로직
const kisPortfolio3dSides = computed(() => {
  const items = kisPortfolioLegend.value
  if (!items.length) return []
  const cx = 150, cy = 100, rx = 95, ry = 76, depth = 15
  let startAngle = -90
  return items.map(item => {
    const angle = (item.percent / 100) * 360
    const endAngle = startAngle + angle
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
    const darkColor = darkenColor(item.color, 0.35)
    startAngle = endAngle
    return { sidePath, darkColor }
  }).filter(s => s.sidePath)
})

function darkenColor(hex: string, factor: number): string {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgb(${Math.floor(r * (1 - factor))}, ${Math.floor(g * (1 - factor))}, ${Math.floor(b * (1 - factor))})`
}

const holdingPieSlices = computed(() => {
  if (!holdings.value.length) return []

  // 종목코드별 투자금 합산
  const codeMap: Record<string, { code: string; name: string; total: number }> = {}
  holdings.value.forEach((h: any) => {
    const key = h.stockCode
    if (!codeMap[key]) codeMap[key] = { code: key, name: h.stockName || key, total: 0 }
    codeMap[key].total += Number(h.totalAmount || 0)
  })

  const items = Object.values(codeMap)
  const grandTotal = items.reduce((s, i) => s + i.total, 0) || 1

  // SVG 파이 슬라이스 생성 (타원형 도넛, cx=150 cy=110 rx=100 ry=80)
  const CX = 150, CY = 110, RX = 100, RY = 80
  let startAngle = -Math.PI / 2
  return items.map((item, idx) => {
    const pct = item.total / grandTotal * 100
    const angle = (item.total / grandTotal) * 2 * Math.PI
    const endAngle = startAngle + angle
    const x1 = CX + RX * Math.cos(startAngle)
    const y1 = CY + RY * Math.sin(startAngle)
    const x2 = CX + RX * Math.cos(endAngle)
    const y2 = CY + RY * Math.sin(endAngle)
    const largeArc = angle > Math.PI ? 1 : 0
    const midAngle = startAngle + angle / 2
    const labelX = CX + (RX * 0.65) * Math.cos(midAngle)
    const labelY = CY + (RY * 0.65) * Math.sin(midAngle)
    const path = items.length === 1
      ? `M${CX},${CY - RY} A${RX},${RY} 0 1 1 ${CX - 0.01},${CY - RY} Z`
      : `M${CX},${CY} L${x1},${y1} A${RX},${RY} 0 ${largeArc} 1 ${x2},${y2} Z`
    startAngle = endAngle
    return {
      path, color: PIE_COLORS[idx % PIE_COLORS.length],
      label: item.code, percent: pct,
      labelX, labelY
    }
  })
})

// 종목별 성과 (매도 완료 거래 기반 계산)
// ⭐ [수정 3] 종목코드 → 종목명 변환 헬퍼
function getStockName(code: string): string {
  return stockNameMap.value[code] || code
}

// 종목별 성과 (매도 완료 거래 기반 계산)
const stockPerformance = computed(() => {
  const perfMap: Record<string, { stockCode: string; stockName: string; profit: number; tradeCount: number }> = {}
  recentTransactions.value
    .filter(tx => tx.status === 'SOLD' && tx.profitLoss != null)
    .forEach(tx => {
      const key = tx.stockCode
      if (!perfMap[key]) perfMap[key] = { stockCode: key, stockName: tx.stockName || key, profit: 0, tradeCount: 0 }
      perfMap[key].profit += Number(tx.profitLoss)
      perfMap[key].tradeCount++
    })
  return Object.values(perfMap)
    .map(p => ({ ...p, profitRate: p.profit / (stats.value.totalHoldingAmount || 1) * 100 }))
    .sort((a, b) => b.profit - a.profit)
})

// 시스템 알림
const systemAlerts = computed(() => {
  const alerts: any[] = []
  if (!hasKisApiKey.value) {
    alerts.push({ color: 'warning', icon: 'mdi-key-alert', message: 'KIS API 키를 등록해주세요', action: '/profile', actionLabel: '등록하기' })
  }
  if (!stockSettings.value) {
    alerts.push({ color: 'warning', icon: 'mdi-cog-alert', message: '주식 거래 설정을 완료해주세요', action: '/stock/settings', actionLabel: '설정하기' })
  }
  if (stats.value.holdingDaysUrgentCount > 0) {
    alerts.push({ color: 'error', icon: 'mdi-alert', message: `레버리지 ETF ${stats.value.holdingDaysUrgentCount}건이 20일 초과 보유 중입니다`, action: '/stock-transactions', actionLabel: '확인하기' })
  }
  if (stats.value.holdingDaysWarningCount > 0) {
    alerts.push({ color: 'warning', icon: 'mdi-clock-alert', message: `레버리지 ETF ${stats.value.holdingDaysWarningCount}건이 15일 이상 보유 중입니다` })
  }
  if (tomorrowHoliday.value) {
    alerts.push({ color: 'info', icon: 'mdi-calendar-alert', message: '내일은 주식 휴장일입니다' })
  }
  return alerts
})

// ── computed ────────────────────────────────────────────────
const dailyLimitUsedPct = computed(() => {
  if (!stats.value.dailyLimitAmount) return 0
  const used = stats.value.dailyLimitAmount - stats.value.remainingDailyLimit
  return Math.min((used / stats.value.dailyLimitAmount) * 100, 100)
})

// ⭐ [수정 3] source 판별: 'frankfurter' → 'open.er-api.com' 또는 'cache'
const exchangeStatus = computed(() => {
  // ⭐ [수정 환율] 백엔드 소스값 포함
  const src = exchange.value.source
  if (src === 'open.er-api.com' || src === 'cache' || src === 'frankfurter') return 'ok'
  if (exchange.value.exchangeRate !== null) return 'fallback-with-data'
  return 'fallback-no-data'
})

const exchangeChangeColor = computed(() => {
  const change = exchange.value.exchangeRateChange
  if (change == null) return 'grey'
  if (change > 0) return 'error'
  if (change < 0) return 'success'
  return 'grey'
})

// ── 유틸 ────────────────────────────────────────────────────
function formatCurrency(val: any): string {
  if (val == null) return '₩0'
  const num = Number(val)
  if (isNaN(num)) return '₩0'
  return '₩' + num.toLocaleString('ko-KR', { maximumFractionDigits: 0 })
}

function formatNumber(val: any): string {
  if (val == null) return '-'
  return Number(val).toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDate(val: any): string {
  if (!val) return '-'
  return new Date(val).toLocaleDateString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function formatLastLogin(val: any): string {
  if (!val) return '-'
  return new Date(val).toLocaleString('ko-KR', { timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function holdingDayColor(days: number | null): string {
  if (days == null) return 'default'
  if (days >= 20) return 'error'
  if (days >= 15) return 'warning'
  return 'success'
}

function updateCurrentTime() {
  const now = new Date()
  // 기존: 전체 날짜+시간 (내부 사용용 유지)
  currentTime.value = now.toLocaleString('ko-KR', {
    timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
  // ⭐ [추가] 코인 대시보드 우상단과 동일한 형식: "오전 02:26:30"
  currentTimeShort.value = now.toLocaleTimeString('ko-KR', {
    timeZone: 'Asia/Seoul', hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

// ── API ─────────────────────────────────────────────────────
async function loadStats() {
  statsLoading.value = true
  try {
    const res = await stockDashboardApi.getStats()
    stats.value = { ...stats.value, ...res.data }
    // ⭐ [수정 1] tomorrowHoliday는 fetchBotStatus()에서만 설정
    // loadStats()에서 덮어쓰면 레이스 컨디션으로 항상 false가 됨
  } catch (e) { console.error('[주식 대시보드] 통계 조회 실패', e) }
  finally { statsLoading.value = false }
}

async function loadHoldings() {
  try {
    const res = await stockTransactionApi.getHoldings()
    holdings.value = res.data || []
  } catch (e) { console.error('[주식 대시보드] 보유 종목 조회 실패', e) }
}

async function loadRecentTransactions() {
  try {
    // ⭐ [수정 Q3] size 20 → 200 확대
    // 이유: fetchAssetHistory()가 recentTransactions를 차트 원본 데이터로 사용.
    //       20건만 가져오면 전체 투자기간 차트에서 데이터가 누락되어 빈 차트가 표시됨.
    const res = await stockTransactionApi.getAll(0, 200)
    recentTransactions.value = res.data?.content || []
  } catch (e) { console.error('[주식 대시보드] 최근 거래 조회 실패', e) }
}

async function loadSettings() {
  try {
    const res = await stockSettingApi.getSettings()
    stockSettings.value = res.data?.data ?? res.data ?? null
  } catch (e) { stockSettings.value = null }
  // ⭐ [수정 3] 종목명 조회 (stock_info API)
  try {
    const infoRes = await stockInfoApi.getActiveStocks()
    const stocks = infoRes.data?.data ?? infoRes.data ?? []
    const map: Record<string, string> = {}
    stocks.forEach((s: any) => { map[s.stockCode] = s.stockName })
    stockNameMap.value = map
  } catch (e) {
    console.warn('[주식 대시보드] 종목명 조회 실패', e)
  }
}

async function loadKisApiStatus() {
  try {
    const res = await stockSettingApi.hasKisApiKey()
    hasKisApiKey.value = res.data === true
  } catch (e) { hasKisApiKey.value = false }
}

// ⭐ [수정 3] KIS 계좌 실제 잔고 조회
async function loadKisAccount() {
  accountLoading.value = true
  try {
    const res = await stockDashboardApi.getAccount()
    kisAccount.value = {
      krwBalance: Number(res.data.krwBalance || 0),
      stockEvaluation: Number(res.data.stockEvaluation || 0),
      totalAsset: Number(res.data.totalAsset || 0),
      holdings: res.data.holdings || [],
    }
  } catch (e) {
    console.error('[주식 대시보드] KIS 계좌 잔고 조회 실패', e)
  } finally {
    accountLoading.value = false
  }
}

// ⭐ [수정 4] 주식 매수 조건 지표 조회
// Phase 1 fetchIndicators 패턴 - StockSignalDetectorService 활용
async function loadStockIndicators() {
  if (!stockSettings.value?.stockCodes?.length) return
  indicatorsLoading.value = true
  try {
    const res = await api.get('/stock/bot/indicators')
    stockIndicators.value = res.data || []
  } catch (e) {
    // 설정된 종목에 대한 간단한 지표 계산
    stockIndicators.value = (stockSettings.value?.stockCodes || []).map((code: string) => ({
      stockCode: code,
      stockName: code,
      currentPrice: 0,
      buyPrice: 0,
      dropRate: 0,
      canBuy: false,
      remainingDrop: stockSettings.value?.buyThresholdPct || 3,
    }))
  } finally {
    indicatorsLoading.value = false
  }
}

// ⭐ [수정 5] 주식 수익 계산 (거래 내역 기반)
function calcStockProfit() {
  const sold = recentTransactions.value.filter((tx: any) => tx.status === 'SOLD' && tx.profitLoss != null)
  const todayStr = new Date().toDateString()
  const todayProfit = sold
    .filter((tx: any) => new Date(tx.soldAt || tx.createdAt).toDateString() === todayStr)
    .reduce((s: number, tx: any) => s + Number(tx.profitLoss), 0)
  const realizedProfit = sold.reduce((s: number, tx: any) => s + Number(tx.profitLoss), 0)
  const totalInvest = stats.value.totalHoldingAmount || 1
  stockProfit.value = {
    todayProfit,
    todayProfitPct: totalInvest > 0 ? (todayProfit / totalInvest) * 100 : 0,
    unrealizedProfit: 0, // KIS 실시간 평가 필요 (Day 63에서 완성)
    unrealizedProfitPct: 0,
    realizedProfit,
    totalProfit: realizedProfit,
  }
}

async function refreshStockChart() {
  // ⭐ [수정 Q6] 스냅샷 갱신 버튼: 백엔드 스냅샷 생성 후 차트 리로드
  // 이유: 코인 대시보드의 refreshSnapshot과 동일한 패턴
  try {
    await api.post('/stock/dashboard/profit/snapshot')
  } catch (e) {
    console.warn('[주식 차트] 스냅샷 갱신 실패, 거래 기반으로 계속:', e)
  }
  await loadRecentTransactions()
  await fetchAssetHistory()
}

// ⭐ [수정 6] 날짜 직접 입력 적용
function applyStockCustomDate() {
  if (chartCustomStart.value && chartCustomEnd.value) {
    chartPeriod.value = 'custom'
  }
}

// ⭐ [추가] 최신 주식 릴리즈노트 조회 (코인: category=COIN, 주식: category=STOCK)
async function fetchLatestReleaseNote() {
  try {
    const response = await api.get('/release-notes/latest?category=STOCK')
    latestReleaseNote.value = response.data
  } catch (e) {
    // 없어도 무방
  }
}

// ⭐ [추가] 날짜 단축 포맷 (코인 대시보드와 동일: "4/12 02:26")
function formatDateShort(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

// ⭐ [수정 환율] CSP 오류 해결: 외부 fetch 완전 제거 → 백엔드 프록시 단일 경로
async function loadExchangeRate() {
  exchangeLoading.value = true
  try {
    const res = await stockDashboardApi.getExchangeRate()
    const data = res.data

    if (data.source !== 'fallback' && data.exchangeRate != null) {
      const prevRate = exchange.value.exchangeRate ?? data.exchangeRate
      const currentRate = Number(data.exchangeRate)
      const change = currentRate - Number(prevRate)
      const changePct = Number(prevRate) > 0 ? Math.round(change / Number(prevRate) * 10000) / 100 : 0

      exchange.value = {
        exchangeRate: currentRate,
        exchangeRateChange: data.exchangeRateChange ?? Math.round(change * 100) / 100,
        exchangeRateChangePct: data.exchangeRateChangePct ?? changePct,
        date: data.date || '',
        source: 'open.er-api.com'
      }
      exchangeLastFetchedAt.value = new Date().toLocaleString('ko-KR', {
        timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit', second: '2-digit'
      })
      stopExchangeRetryTimer()
      // ⭐ [수정 4] 성공 시 5분 카운트다운 시작
      startExchangeAutoRefresh()
    } else {
      exchange.value = { ...exchange.value, source: 'fallback' }
      startExchangeRetryTimer()
    }
  } catch (e) {
    console.warn('[주식 대시보드] 환율 조회 실패:', e)
    exchange.value = { ...exchange.value, source: 'fallback' }
    startExchangeRetryTimer()
  } finally {
    exchangeLoading.value = false
  }
}



function startExchangeRetryTimer() {
  stopExchangeRetryTimer()
  const RETRY_SEC = 30
  exchangeRetryCountdown.value = RETRY_SEC
  exchangeCountdownTimer = setInterval(() => { exchangeRetryCountdown.value -= 1 }, 1_000)
  exchangeRetryTimer = setTimeout(async () => {
    stopExchangeRetryTimer()
    await loadExchangeRate()
  }, RETRY_SEC * 1_000)
}

function stopExchangeRetryTimer() {
  if (exchangeCountdownTimer) { clearInterval(exchangeCountdownTimer); exchangeCountdownTimer = null }
  if (exchangeRetryTimer) { clearTimeout(exchangeRetryTimer); exchangeRetryTimer = null }
  exchangeRetryCountdown.value = 30
}

// ⭐ [수정 4] 5분 자동 갱신 타이머
function startExchangeAutoRefresh() {
  stopExchangeAutoRefresh()
  exchangeAutoRefreshSeconds.value = 300
  exchangeAutoRefreshTimer = setInterval(async () => {
    if (exchangeAutoRefreshSeconds.value > 0) {
      exchangeAutoRefreshSeconds.value--
    } else {
      stopExchangeAutoRefresh()
      await loadExchangeRate()
    }
  }, 1000)
}
function stopExchangeAutoRefresh() {
  if (exchangeAutoRefreshTimer) {
    clearInterval(exchangeAutoRefreshTimer)
    exchangeAutoRefreshTimer = null
  }
  exchangeAutoRefreshSeconds.value = 300
}

// ⭐ [수정 8] fetchBotStatus - Phase 1 패턴 재사용
async function fetchBotStatus() {
  try {
    const r = await api.get('/stock/bot/status')
    botStatus.value = {
      isRunning: r.data?.isRunning || false,
      lastExecutionTime: r.data?.lastExecutionTime || '',
      nextExecutionTime: r.data?.nextExecutionTime || '',
      marketOpen: r.data?.marketOpen || false,
      emergencyStop: r.data?.emergencyStop || false,
      secondsUntilNextExecution: r.data?.secondsUntilNextExecution || 0,
    }
    botEnabled.value = r.data?.botEnabled ?? false
    tomorrowHoliday.value = r.data?.tomorrowHoliday ?? false
    const seconds = r.data?.secondsUntilNextExecution || 0
    if (seconds > 0) startCountdown(seconds)
  } catch (e) {
    console.error('[주식 대시보드] 봇 상태 조회 실패', e)
  }
}

// ⭐ [수정 8] toggleBot - Phase 1 패턴 재사용 (botToggleLoading + botEnabled ref 사용)
async function toggleBot() {
  botToggleLoading.value = true
  try {
    if (botEnabled.value) {
      await api.post('/stock/bot/start')
    } else {
      await api.post('/stock/bot/stop')
    }
    await fetchBotStatus()
  } catch (e) {
    console.error('[주식 대시보드] 봇 상태 변경 실패', e)
    botEnabled.value = !botEnabled.value // 실패 시 원래 값 복원
  } finally {
    botToggleLoading.value = false
  }
}

// ⭐ [수정 8] formatBotTimeDisplay - Phase 1 동일 함수
function formatBotTimeDisplay(dateStr: string | null | undefined) {
  if (!dateStr || dateStr === '-') return '-'
  try {
    const d = new Date(dateStr)
    if (isNaN(d.getTime())) return dateStr
    const dateOpts: Intl.DateTimeFormatOptions = { year: 'numeric', month: '2-digit', day: '2-digit' }
    const timeOpts: Intl.DateTimeFormatOptions = { hour: '2-digit', minute: '2-digit', hour12: false }
    const datePart = d.toLocaleDateString('ko-KR', dateOpts).replace(/\. /g, '-').replace('.', '')
    const timePart = d.toLocaleTimeString('ko-KR', timeOpts)
    return `${datePart} ${timePart}`
  } catch { return dateStr }
}

// ⭐ [수정 8] 카운트다운 - Phase 1 동일
function startCountdown(seconds: number) {
  countdownSeconds.value = seconds
  if (countdownInterval) clearInterval(countdownInterval)
  countdownInterval = setInterval(() => {
    if (countdownSeconds.value > 0) {
      countdownSeconds.value--
    } else {
      if (countdownInterval) { clearInterval(countdownInterval); countdownInterval = null }
      fetchBotStatus()
    }
  }, 1000)
}

// ⭐ [수정 2] 최근 거래 날짜: 코인 대시보드와 동일 "4/7 20:20" 형식
function formatDateForTx(dateStr: any): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

async function refreshAll() {
  isRefreshing.value = true
  await Promise.all([loadStats(), loadHoldings(), loadRecentTransactions()])
  isRefreshing.value = false
}

// ── 도움말 (Phase 1 cardHelps 동일 패턴, 주식 내용으로 교체) ─
const cardHelps = {
  botStatus: {
    title: '🤖 자동매매 봇 상태',
    content: `
      <p class="help-intro">주식 자동매매 봇의 현재 상태와 장 운영 여부를 보여줍니다.</p>
      <div style="height: 16px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>실행 중</strong><br/>
        <span class="help-desc">봇이 활성화되고 장중(09:00~15:30)인 상태입니다. 3분마다 매수/매도 조건을 자동 검사합니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>장마감 대기</strong><br/>
        <span class="help-desc">봇이 활성화되어 있으나 장이 마감된 상태입니다. 다음 장 시작 시 자동 재개됩니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>정지됨</strong><br/>
        <span class="help-desc">봇을 수동으로 정지한 상태입니다. [봇 시작] 버튼으로 재활성화할 수 있습니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>내일 휴장</strong><br/>
        <span class="help-desc">다음 거래일이 공휴일 또는 임시 휴장일인 경우 표시됩니다.</span></p>
      <p class="help-note">※ 거래 시간: 정규장 09:00 ~ 15:30 (KST)</p>
    `
  },
  kisAccount: {
    title: '🏦 KIS 계좌 현황',
    content: `
      <p class="help-intro">한국투자증권 계좌의 주식/ETF 보유 현황을 보여줍니다.</p>
      <div style="height: 16px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>보유 종목 구성</strong><br/>
        <span class="help-desc">현재 자동매매 봇이 매수하여 보유 중인 종목 목록입니다. 보유일은 실제 영업일 기준입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>보유일 색상</strong><br/>
        <span class="help-desc">초록: 정상 / 주황: 15일 이상(경고) / 빨강: 20일 이상(긴급). 레버리지 ETF는 장기 보유 시 Decay 손실이 발생합니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>오늘 실현 손익</strong><br/>
        <span class="help-desc">오늘 매도 완료된 거래에서 확정된 손익 합계입니다.</span></p>
      <p class="help-note">※ KIS API 키가 등록되어 있어야 실시간 조회가 가능합니다.</p>
    `
  },
  tradingSettings: {
    title: '⚙️ 거래 설정 요약',
    content: `
      <p class="help-intro">현재 설정된 주식/ETF 자동매매 조건을 요약하여 보여줍니다.</p>
      <div style="height: 16px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>거래 종목</strong><br/>
        <span class="help-desc">자동매매 대상 종목코드 목록입니다. 예: 409820(TIGER 나스닥100레버리지)</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>매수 조건</strong><br/>
        <span class="help-desc">이동평균선(MA) 대비 설정된 하락률 이하로 내려갔을 때 매수 신호가 발생합니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>매도 조건</strong><br/>
        <span class="help-desc">익절: 목표 수익률 도달 시 자동 매도. 손절: 손실률 도달 시 강제 매도.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>트레일링 스톱</strong><br/>
        <span class="help-desc">보유 기간 중 최고가 대비 설정 % 하락 시 자동 매도하여 수익을 보전합니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>RSI 매수 신호</strong><br/>
        <span class="help-desc">RSI 지표가 설정값 이하일 때 과매도 상태로 판단하여 매수 신호를 강화합니다. 기본값: 35</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>거래량 기준</strong><br/>
        <span class="help-desc">평균 거래량 대비 설정 % 이상일 때만 매수 신호를 활성화합니다. 기본값: 120%</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>최대 보유기간</strong><br/>
        <span class="help-desc">레버리지 ETF의 변동성 끌림(Volatility Drag) 방지를 위해 설정합니다. 기본 20거래일을 권장합니다.</span></p>
      <!-- ⭐ [수정 Q5] 누락 항목 도움말 추가 -->
      <p class="help-item"><span class="help-bullet">•</span> <strong>RSI 매수/매도 신호</strong><br/>
        <span class="help-desc">RSI 지표가 매수 임계값 이하이면 과매도로 판단, 매도 임계값 이상이면 과매수로 판단합니다. 기본: 매수 35 / 매도 65.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>거래량 기준</strong><br/>
        <span class="help-desc">평균 거래량 대비 설정 % 이상일 때만 매수 신호를 활성화합니다. 기본 120%.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>볼린저밴드</strong><br/>
        <span class="help-desc">N일 이동평균 ± Kσ 범위 계산. 하단 접촉 시 매수 신호 강화. 기본: 20일/2σ.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>시장 추세 필터</strong><br/>
        <span class="help-desc">ON 시 지수(ETF 기초지수) MA20 하회 구간에서 매수를 중단합니다. 급락장 보호 기능.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>누적 손실 긴급정지</strong><br/>
        <span class="help-desc">초기 자본 대비 누적 손실이 설정값(기본 -10%)에 도달하면 모든 거래를 자동 중단합니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>연속 손절 제한</strong><br/>
        <span class="help-desc">동일 종목에서 연속 손절이 발생하면 해당 종목의 추가 매수를 일시 중단합니다. 기본 3회.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>라운드로빈 방식</strong><br/>
        <span class="help-desc">여러 종목에 매수 신호 동시 발생 시 균등 분배하여 분산 투자합니다.</span></p>
      <!-- ⭐ [수정 Q4] 누락 도움말 추가 -->
      <p class="help-item"><span class="help-bullet">•</span> <strong>손절매</strong><br/>
        <span class="help-desc">ON 시 손실률이 설정값에 도달하면 강제 매도합니다. OFF 시 보유기간 제한만 적용됩니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>추가 하락 조건</strong><br/>
        <span class="help-desc">최초 매수 후 추가로 설정 % 하락 시 분할 매수합니다. 종목당 최대 보유 건수 내에서 작동합니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>일일 투자 한도</strong><br/>
        <span class="help-desc">하루 동안 사용할 수 있는 최대 투자 금액을 총 자산의 % 비율로 설정합니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>KIS 거래 모드</strong><br/>
        <span class="help-desc">모의투자: 가상 자금으로 실제 시세에 테스트. 실계좌: 실제 자금으로 거래.</span></p>
      <p class="help-note">설정이 없으면 [설정하기] 버튼을 눌러 거래 조건을 설정해주세요.</p>
    `
  },
  dailyLimit: {
    title: '📊 일일 한도',
    content: `
      <p class="help-intro">오늘 하루 동안의 주식 거래 한도 사용 현황을 보여줍니다.</p>
      <div style="height: 16px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>한도</strong><br/>
        <span class="help-desc">하루 동안 매수에 사용할 수 있는 최대 금액입니다. 거래 설정에서 변경 가능합니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>남은 한도</strong><br/>
        <span class="help-desc">오늘 남은 매수 가능 금액입니다. 한도 소진 시 당일 추가 매수가 중단됩니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>레버리지 ETF 보유기간 경고</strong><br/>
        <span class="help-desc">15일 이상 보유 시 경고, 20일 이상 보유 시 긴급 경고가 표시됩니다. Decay 손실 방지를 위해 청산을 검토하세요.</span></p>
    `
  },
  exchangeRate: {
    title: '💱 USD / KRW 환율',
    content: `
      <p class="help-intro">달러-원 환율 정보를 서버를 통해 조회하여 표시합니다.</p>

      <!-- ⭐ [수정 2] 환율 정보 흐름 다이어그램 -->
      <div style="margin: 12px 0; background: #F8F9FA; border-radius: 8px; padding: 8px;">
        <!-- ⭐ [수정 3] Frankfurter 전일 환율 조회 경로 추가 -->
        <svg width="100%" viewBox="0 0 560 280" style="display:block;">
          <defs>
            <marker id="ha" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse">
              <path d="M2 1L8 5L2 9" fill="none" stroke="context-stroke" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </marker>
          </defs>
          <!-- 브라우저 -->
          <rect x="10" y="70" width="100" height="52" rx="8" fill="#E6F1FB" stroke="#378ADD" stroke-width="1"/>
          <text x="60" y="92" text-anchor="middle" font-size="12" font-weight="500" fill="#0C447C">브라우저</text>
          <text x="60" y="108" text-anchor="middle" font-size="11" fill="#185FA5">Vue 앱</text>
          <!-- 화살표 1→2 -->
          <line x1="110" y1="96" x2="168" y2="96" stroke="#378ADD" stroke-width="1.2" marker-end="url(#ha)"/>
          <text x="139" y="88" text-anchor="middle" font-size="10" fill="#5F5E5A">GET /api/</text>
          <text x="139" y="100" text-anchor="middle" font-size="10" fill="#5F5E5A">exchange-rate</text>
          <!-- 백엔드 -->
          <rect x="172" y="50" width="120" height="92" rx="8" fill="#E1F5EE" stroke="#1D9E75" stroke-width="1"/>
          <text x="232" y="73" text-anchor="middle" font-size="12" font-weight="500" fill="#085041">Spring Boot</text>
          <text x="232" y="89" text-anchor="middle" font-size="11" fill="#0F6E56">백엔드 서버</text>
          <rect x="190" y="98" width="84" height="18" rx="4" fill="#9FE1CB"/>
          <text x="232" y="111" text-anchor="middle" font-size="10" fill="#04342C">5분 캐시 적용</text>
          <text x="232" y="133" text-anchor="middle" font-size="10" fill="#0F6E56">전일 환율 별도 조회</text>
          <!-- 화살표 2→3 (오늘 환율) -->
          <line x1="292" y1="82" x2="350" y2="82" stroke="#1D9E75" stroke-width="1.2" marker-end="url(#ha)"/>
          <text x="321" y="74" text-anchor="middle" font-size="10" fill="#5F5E5A">오늘 환율</text>
          <!-- open.er-api.com -->
          <rect x="354" y="50" width="128" height="66" rx="8" fill="#FAEEDA" stroke="#BA7517" stroke-width="1"/>
          <text x="418" y="72" text-anchor="middle" font-size="11" font-weight="500" fill="#412402">open.er-api.com</text>
          <text x="418" y="87" text-anchor="middle" font-size="10" fill="#633806">무료 환율 API</text>
          <text x="418" y="101" text-anchor="middle" font-size="10" fill="#633806">ECB 기준 / 1,500회/월</text>
          <!-- 화살표 2→4 (전일 환율) -->
          <line x1="292" y1="125" x2="350" y2="165" stroke="#534AB7" stroke-width="1.2" stroke-dasharray="4,2" marker-end="url(#ha)"/>
          <text x="315" y="158" text-anchor="middle" font-size="10" fill="#534AB7">전일 환율</text>
          <!-- Frankfurter -->
          <rect x="354" y="148" width="128" height="56" rx="8" fill="#EEEDFE" stroke="#534AB7" stroke-width="1"/>
          <text x="418" y="169" text-anchor="middle" font-size="11" font-weight="500" fill="#26215C">frankfurter.app</text>
          <text x="418" y="184" text-anchor="middle" font-size="10" fill="#3C3489">무료 / 날짜별 조회</text>
          <text x="418" y="198" text-anchor="middle" font-size="10" fill="#3C3489">어제 날짜 KRW 반환</text>
          <!-- 응답 화살표 (아래 루프) -->
          <path d="M418 116 L418 142 L294 142 L294 116" fill="none" stroke="#BA7517" stroke-width="1.1" stroke-dasharray="4,3" marker-end="url(#ha)"/>
          <path d="M418 204 L418 222 L294 222 L294 142" fill="none" stroke="#534AB7" stroke-width="1.1" stroke-dasharray="4,3" marker-end="url(#ha)"/>
          <text x="356" y="235" text-anchor="middle" font-size="10" fill="#534AB7">전일 KRW 값 반환</text>
          <!-- 최종 응답 -->
          <path d="M232 50 L232 28 L60 28 L60 70" fill="none" stroke="#1D9E75" stroke-width="1.1" stroke-dasharray="4,3" marker-end="url(#ha)"/>
          <text x="145" y="20" text-anchor="middle" font-size="10" fill="#5F5E5A">{ exchangeRate, change, date } 반환</text>
          <!-- CSP 차단 -->
          <rect x="10" y="250" width="540" height="22" rx="4" fill="none" stroke="#E24B4A" stroke-width="0.8" stroke-dasharray="3,3"/>
          <text x="280" y="265" text-anchor="middle" font-size="10" fill="#A32D2D">브라우저 직접 외부 API 호출 불가 (Nginx CSP: connect-src 'self') → 서버 프록시 방식 사용</text>
        </svg>
      </div>

      <div style="height: 8px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>데이터 출처</strong><br/>
        <span class="help-desc">open.er-api.com (유럽중앙은행 ECB 기준, 전일 종가). 월 1,500회 무료.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>조회 구조</strong><br/>
        <span class="help-desc">브라우저 → 백엔드(Spring Boot) → open.er-api.com 순서로 서버사이드 프록시. Nginx CSP 정책으로 브라우저에서 외부 직접 호출 불가.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>5분 자동 갱신</strong><br/>
        <span class="help-desc">최초 로드 후 5분마다 자동 갱신. 백엔드 5분 캐시로 API 호출 횟수 최소화.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>전일 대비</strong><br/>
        <span class="help-desc">오늘 환율과 어제 환율(Frankfurter API)의 차이. 빨강=달러 강세(원화 약세), 초록=달러 약세(원화 강세).</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>환노출형 ETF (TIGER 등)</strong><br/>
        <span class="help-desc">환헤지 없이 달러 변동이 수익률에 직접 영향. 달러 강세 시 추가 수익 발생.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>환헤지형 ETF (KODEX H 등)</strong><br/>
        <span class="help-desc">환율 변동 헤지. 기초지수 수익률만 반영, 환율 영향 없음.</span></p>
      <p class="help-note">※ API 조회 실패 시 마지막 저장값을 표시하며 30초마다 재시도합니다.</p>
    `
  },
  profitSummary: {
    title: '💰 수익 현황',
    content: `
      <p class="help-intro">주식/ETF 투자 수익 현황을 한눈에 확인합니다.</p>
      <div style="height: 16px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>오늘 실현 손익</strong><br/>
        <span class="help-desc">오늘 매도 완료된 거래에서 확정된 손익입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>총 투자금액</strong><br/>
        <span class="help-desc">현재 보유 중인 종목의 매수 당시 투자금 합계입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>남은 일일 한도</strong><br/>
        <span class="help-desc">오늘 추가로 매수 가능한 금액입니다.</span></p>
    `
  },
  stockPerformance: {
    title: '🏆 종목별 성과',
    content: `
      <p class="help-intro">매도 완료된 거래 기준으로 종목별 성과를 보여줍니다.</p>
      <div style="height: 16px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>거래 횟수</strong><br/>
        <span class="help-desc">해당 종목의 총 매도 완료 건수입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>손익</strong><br/>
        <span class="help-desc">해당 종목에서 발생한 실현 손익 합계입니다.</span></p>
      <p class="help-note">※ 보유 중인 미실현 손익은 포함되지 않습니다.</p>
    `
  },
  recentTransactions: {
    title: '📋 최근 거래',
    content: `
      <p class="help-intro">가장 최근에 발생한 주식/ETF 거래 내역을 보여줍니다.</p>
      <div style="height: 16px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>매수 (↓)</strong><br/>
        <span class="help-desc">봇이 자동으로 매수한 내역입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>매도완료 (↑)</strong><br/>
        <span class="help-desc">목표 수익률 도달, 손절매, 또는 보유기간 초과로 매도된 내역입니다.</span></p>
      <p class="help-note">[전체 거래 내역 보기 →] 버튼을 누르면 상세 조회 페이지로 이동합니다.</p>
    `
  },
  systemAlert: {
    title: '🚨 시스템 알림',
    content: `
      <p class="help-intro">시스템 상태 및 필요한 조치사항을 알려줍니다.</p>
      <div style="height: 16px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>KIS API 키 미등록</strong><br/>
        <span class="help-desc">KIS API 키를 등록해야 실제 거래가 가능합니다. 프로필 페이지에서 등록하세요.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>거래 설정 필요</strong><br/>
        <span class="help-desc">자동매매 조건을 설정해야 봇이 작동합니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>레버리지 ETF 경고</strong><br/>
        <span class="help-desc">15일 이상 보유 종목은 Decay 손실 방지를 위해 매도를 검토하세요.</span></p>
      <p class="help-note">알림이 없으면 시스템이 정상 상태입니다.</p>
    `
  },

  buyCondition: {
    title: '🎯 매수 조건',
    content: `
      <p class="help-intro">설정된 주식/ETF 종목별 현재 매수 조건 충족 여부를 보여줍니다.</p>
      <div style="height: 16px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>현재가 → 매수가</strong><br/>
        <span class="help-desc">현재 시세와 MA 기준 매수 목표가를 보여줍니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>이격도</strong><br/>
        <span class="help-desc">현재가가 이동평균선(MA) 대비 얼마나 떨어져 있는지 나타냅니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>매수 가능</strong><br/>
        <span class="help-desc">이격도가 설정된 하락률 이하로 내려가면 매수 신호가 발생합니다.</span></p>
    `
  },

  // ⭐ [추가] 자산 변동 추이 도움말 (템플릿에서 openHelp('assetChart') 호출 중)
  assetChart: {
    title: '📈 자산 변동 추이',
    content: `
      <p class="help-intro">거래 내역 기반으로 일별 투자금액 변동을 차트로 보여줍니다.</p>
      <div style="height: 16px;"></div>
      <p class="help-item"><span class="help-bullet">•</span> <strong>7일 / 이번달 / 전체</strong><br/>
        <span class="help-desc">원하는 기간을 선택하여 조회할 수 있습니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>차트 호버</strong><br/>
        <span class="help-desc">차트의 점 위에 마우스를 올리면 해당 날짜의 투자금액을 확인할 수 있습니다.</span></p>
      <p class="help-note">※ 매수 거래가 발생한 날짜부터 차트가 표시됩니다.</p>
    `
  }
}

const openHelp = (helpKey: string) => {
  const help = cardHelps[helpKey as keyof typeof cardHelps]
  if (help) { currentHelp.value = help; showHelpDialog.value = true }
}

// ── 라이프사이클 ────────────────────────────────────────────
let timeInterval: ReturnType<typeof setInterval> | null = null
let autoRefreshTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  updateCurrentTime()
  timeInterval = setInterval(updateCurrentTime, 1000)
  // ⭐ [수정 8] fetchBotStatus 별도 추가
  // ⭐ [수정 3,4,5] KIS 계좌/매수조건/수익 추가
  await Promise.all([loadStats(), loadHoldings(), loadRecentTransactions(), loadSettings(), loadKisApiStatus(), fetchBotStatus(), loadKisAccount()])
  calcStockProfit()
  loadStockIndicators()
  // ⭐ [수정 3] 자산 이력 로드
  fetchAssetHistory()
  await Promise.all([loadExchangeRate(), fetchLatestReleaseNote()])
  autoRefreshTimer = setInterval(async () => {
    await Promise.all([loadStats(), loadHoldings(), fetchBotStatus()])
  }, 30_000)
})

onUnmounted(() => {
  if (timeInterval) clearInterval(timeInterval)
  if (autoRefreshTimer) clearInterval(autoRefreshTimer)
  // ⭐ [수정 8] 카운트다운 타이머 정리
  if (countdownInterval) { clearInterval(countdownInterval); countdownInterval = null }
  stopExchangeRetryTimer()
  // ⭐ [수정 4]
  stopExchangeAutoRefresh()
})
</script>

<style scoped>
.fill-height { height: 100%; }
.w-100 { width: 100%; }
.border-b { border-bottom: 1px solid rgba(0,0,0,0.08); }

/* Phase 1 동일: 누적 총 수익 카드 검정 테두리 */
.total-profit-card {
  border: 2px solid #333 !important;
  background-color: #E8F5E9 !important;
}
.total-asset-card {
  border: 2px solid #333 !important;
  background-color: #E8F5E9 !important;
}

/* Phase 1 동일: 도움말 다이얼로그 스타일 */
.help-content :deep(.help-intro) {
  color: #37474F;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 8px;
}
.help-content :deep(.help-item) {
  margin-bottom: 12px;
  font-size: 14px;
  line-height: 1.6;
}
.help-content :deep(.help-bullet) {
  color: #3949AB;
  font-weight: bold;
  margin-right: 6px;
}
.help-content :deep(.help-desc) {
  color: #546E7A;
  font-size: 13px;
}
.help-content :deep(.help-note) {
  color: #FF6F00;
  font-size: 12px;
  background: #FFF8E1;
  padding: 6px 10px;
  border-radius: 4px;
  margin-top: 8px;
}

/* ⭐ [수정 5/6] 자산 변동 차트 + 파이차트 */
.chart-container { position: relative; width: 100%; }
.chart-wrapper-stock { position: relative; width: 100%; height: 200px; }
.chart-wrapper-stock svg { width: 100%; height: 100%; }
.stock-chart-tooltip {
  position: absolute;
  background: rgba(38,50,56,0.92);
  color: white;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 12px;
  pointer-events: none;
  z-index: 10;
  white-space: nowrap;
  transform: translateX(-50%);
}
.legend-dot {
  width: 10px; height: 10px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}
/* ⭐ [수정 2-2] 차트 기간 버튼 */
.chart-period-toggle .v-btn { color: rgba(255,255,255,0.7) !important; border-color: rgba(255,255,255,0.5) !important; }
.chart-period-toggle .active-period { background-color: white !important; color: #3949AB !important; font-weight: bold !important; }

/* ⭐ [수정 3] Phase 1 파이차트 동일 CSS */
.portfolio-chart {
  width: 100%;
  height: auto;
}
.pie-slice {
  cursor: pointer;
  transition: opacity 0.2s;
}
.pie-tooltip {
  position: absolute;
  background: rgba(38,50,56,0.92);
  color: white;
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 12px;
  pointer-events: none;
  z-index: 20;
  white-space: nowrap;
  transform: translate(-50%, -50%);
}

/* ⭐ [수정 6] Phase 1 차트 컨트롤 CSS */
.chart-control-group {
  height: 28px;
}
.custom-date-input {
  background: rgba(255,255,255,0.9);
  border: 1px solid rgba(255,255,255,0.5);
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 11px;
  color: #333;
  width: 110px;
  height: 28px;
  line-height: 20px;
}
.custom-date-input:focus {
  outline: none;
  border-color: #FFC107;
}
.chart-view-btn { height: 28px !important; min-height: 28px !important; }
.chart-divider { opacity: 0.5; }

/* ⭐ [수정 3] 코인 대시보드 차트와 완전 동일한 CSS */
.chart-wrapper-backtest {
  position: relative;
  cursor: crosshair;
  height: 350px;
}
.chart-wrapper-backtest.scroll-mode { min-width: 100%; }
/* ⭐ [수정 Q1] height: 100% → 350px 고정 (SVG viewBox 높이와 정확히 일치시켜 좌표 오차 제거) */
/* 이유: height: 100%는 부모 컨테이너 높이(350px와 다를 수 있음) 기준이라 CSS %와 SVG 좌표계 불일치 발생 */
.chart-labels-backtest {
  position: absolute; top: 0; right: 0; height: 350px;
  pointer-events: none; z-index: 5;
}
.chart-label {
  position: absolute; right: 5px; font-size: 11px;
  padding: 2px 6px; background: white; border-radius: 3px;
  font-weight: 500; transform: translateY(-50%); white-space: nowrap;
}
.label-max { color: #4CAF50; }
.label-deposit { color: #FF9800; }
.label-evaluation { color: #1976D2; }
.label-floor { color: #9E9E9E; }
.label-min { color: #F44336; }
.chart-tooltip-backtest {
  position: absolute;
  background: rgba(0,0,0,0.9); color: white;
  padding: 8px 12px; border-radius: 6px;
  font-size: 12px; pointer-events: none;
  z-index: 100; white-space: nowrap;
  box-shadow: 0 4px 12px rgba(0,0,0,0.4);
}
.chart-point { cursor: pointer; transition: r 0.15s ease; }
.text-success { color: #4CAF50 !important; font-weight: bold; }
.text-error { color: #F44336 !important; font-weight: bold; }
.chart-dates { font-size: 12px; }

/* ⭐ [수정 7] 자산 변동 추이 헤더 요소 높이 통일 */
.chart-period-toggle,
.chart-period-toggle .v-btn {
  height: 28px !important;
  /* ⭐ [수정] 오타 !importantc → !important */
  min-height: 28px !important;
}
.chart-control-group {
  height: 28px !important;
  display: flex !important;
  align-items: center !important;
}
.custom-date-input {
  height: 28px !important;
  line-height: 24px !important;
}

</style>