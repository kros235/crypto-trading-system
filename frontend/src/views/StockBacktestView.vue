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
                    <v-expansion-panel>
                      <v-expansion-panel-title>
                        <v-icon class="mr-2">mdi-swap-vertical</v-icon>
                        매수/매도 조건
                      </v-expansion-panel-title>
                      <v-expansion-panel-text>
                        <div class="d-flex align-center mb-1">
                          <span class="text-caption text-grey">이동평균선 기간: {{ request.basePeriod }}일</span>
                          <HelpButton use-dialog :dialog-title="helpContents.maPeriod.title" :dialog-content="helpContents.maPeriod.content" size="x-small" color="grey" />
                        </div>
                        <v-slider v-model="request.basePeriod" :min="7" :max="30" :step="1" thumb-label class="mb-2" />

                        <div class="d-flex align-center mb-1">
                          <span class="text-caption text-grey">매수 기준 (MA 대비): {{ request.buyThresholdPct }}%</span>
                          <HelpButton use-dialog :dialog-title="helpContents.buyThreshold.title" :dialog-content="helpContents.buyThreshold.content" size="x-small" color="grey" />
                        </div>
                        <v-slider v-model="request.buyThresholdPct" :min="-20" :max="0" :step="0.5" thumb-label class="mb-2" />

                        <div class="d-flex align-center mb-1">
                          <span class="text-caption text-grey">목표 수익률: {{ request.sellTargetPct }}%</span>
                          <HelpButton use-dialog :dialog-title="helpContents.sellTarget.title" :dialog-content="helpContents.sellTarget.content" size="x-small" color="grey" />
                        </div>
                        <v-slider v-model="request.sellTargetPct" :min="0.5" :max="20" :step="0.1" thumb-label class="mb-2" />

                        <div class="d-flex align-center mb-1">
                          <v-switch v-model="request.useStopLoss" label="손절매 사용" color="primary" density="compact" hide-details />
                          <HelpButton use-dialog :dialog-title="helpContents.useStopLoss.title" :dialog-content="helpContents.useStopLoss.content" size="x-small" color="grey" />
                        </div>
                        <template v-if="request.useStopLoss">
                          <div class="d-flex align-center mb-1">
                            <span class="text-caption text-grey">손절매 기준: {{ request.stopLossPct }}%</span>
                            <HelpButton use-dialog :dialog-title="helpContents.stopLoss.title" :dialog-content="helpContents.stopLoss.content" size="x-small" color="grey" />
                          </div>
                          <v-slider v-model="request.stopLossPct" :min="-30" :max="0" :step="0.5" thumb-label class="mb-2" />
                        </template>

                        <div class="d-flex align-center mb-1">
                          <v-switch v-model="request.useTrailingStop" label="트레일링 스톱 사용" color="primary" density="compact" hide-details />
                          <HelpButton use-dialog :dialog-title="helpContents.trailingStop.title" :dialog-content="helpContents.trailingStop.content" size="x-small" color="grey" />
                        </div>
                        <div v-if="request.useTrailingStop" class="mb-3">
                          <span class="text-caption text-grey">트레일링 스톱: {{ request.trailingStopPct }}%</span>
                          <v-slider v-model="request.trailingStopPct" :min="1" :max="10" :step="0.5" thumb-label />
                        </div>

                        <div class="d-flex align-center mb-1">
                          <span class="text-caption text-grey">종목당 최대 보유: {{ request.maxHoldingsPerStock }}건</span>
                          <HelpButton use-dialog :dialog-title="helpContents.maxHoldings.title" :dialog-content="helpContents.maxHoldings.content" size="x-small" color="grey" />
                        </div>
                        <v-slider v-model="request.maxHoldingsPerStock" :min="1" :max="10" :step="1" thumb-label class="mb-2" />

                        <div class="d-flex align-center mb-1">
                          <span class="text-caption text-grey">추가 매수(물타기) 하락률: -{{ request.additionalDropPct }}%</span>
                          <HelpButton use-dialog :dialog-title="helpContents.additionalDropPct.title" :dialog-content="helpContents.additionalDropPct.content" size="x-small" color="grey" />
                        </div>
                        <v-slider v-model="request.additionalDropPct" :min="0" :max="10" :step="0.5" thumb-label />
                      </v-expansion-panel-text>
                    </v-expansion-panel>

                    <!-- ⭐ 레버리지 ETF decay 방지 (Day 62 신규) -->
                    <v-expansion-panel>
                      <v-expansion-panel-title>
                        <v-icon class="mr-2">mdi-timer-alert-outline</v-icon>
                        레버리지 ETF 보유기간 제한
                      </v-expansion-panel-title>
                      <v-expansion-panel-text>
                        <div class="text-caption text-grey mb-2 d-flex align-center">
                          <v-icon size="small" class="mr-1" color="warning">mdi-alert</v-icon>
                          변동성 끌림(decay) 방지: 보유일 도달 시 강제 매도
                        </div>
                        <div class="d-flex align-center mb-1">
                          <span class="text-caption text-grey">최대 보유 거래일: {{ request.maxHoldingDays }}일</span>
                          <HelpButton use-dialog :dialog-title="helpContents.maxHoldingDays.title" :dialog-content="helpContents.maxHoldingDays.content" size="x-small" color="grey" />
                        </div>
                        <v-slider v-model="request.maxHoldingDays" :min="1" :max="60" :step="1" thumb-label />
                      </v-expansion-panel-text>
                    </v-expansion-panel>

                    <!-- 기술적 지표 -->
                    <v-expansion-panel>
                      <v-expansion-panel-title>
                        <v-icon class="mr-2">mdi-chart-bell-curve-cumulative</v-icon>
                        기술적 지표 설정
                      </v-expansion-panel-title>
                      <v-expansion-panel-text>
                        <div class="d-flex align-center mb-1">
                          <span class="text-subtitle-2">RSI (상대강도지수)</span>
                          <HelpButton use-dialog :dialog-title="helpContents.rsiSettings.title" :dialog-content="helpContents.rsiSettings.content" size="x-small" color="grey" />
                        </div>
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

                        <v-divider class="my-3" />
                        <div class="d-flex align-center mb-1">
                          <span class="text-subtitle-2">볼린저 밴드</span>
                          <HelpButton use-dialog :dialog-title="helpContents.bbSettings.title" :dialog-content="helpContents.bbSettings.content" size="x-small" color="grey" />
                        </div>
                        <div class="mb-3">
                          <span class="text-caption text-grey">볼린저 밴드 기간: {{ request.bbPeriod }}일</span>
                          <v-slider v-model="request.bbPeriod" :min="10" :max="50" :step="1" thumb-label />
                        </div>
                        <div class="mb-3">
                          <span class="text-caption text-grey">볼린저 밴드 승수: {{ request.bbMultiplier }}</span>
                          <v-slider v-model="request.bbMultiplier" :min="1" :max="4" :step="1" thumb-label />
                        </div>

                        <v-divider class="my-3" />
                        <div class="d-flex align-center mb-1">
                          <span class="text-subtitle-2">거래량 급증 기준</span>
                          <HelpButton use-dialog :dialog-title="helpContents.volumeThreshold.title" :dialog-content="helpContents.volumeThreshold.content" size="x-small" color="grey" />
                        </div>
                        <div>
                          <span class="text-caption text-grey">거래량 급증 기준: {{ request.volumeThreshold }}%</span>
                          <v-slider v-model="request.volumeThreshold" :min="100" :max="500" :step="10" thumb-label />
                        </div>
                      </v-expansion-panel-text>
                    </v-expansion-panel>

                    <!-- 매수 방식 -->
                    <v-expansion-panel>
                      <v-expansion-panel-title>
                        <v-icon class="mr-2">mdi-cash-multiple</v-icon>
                        매수 방식
                      </v-expansion-panel-title>
                      <v-expansion-panel-text>
                        <div class="d-flex align-center mb-1">
                          <v-switch
                            v-model="request.useRoundRobin"
                            :label="request.useRoundRobin ? '라운드로빈 (신호 발생 종목 균등 분배)' : '고정 금액 매수'"
                            color="primary" density="compact" hide-details
                          />
                          <HelpButton use-dialog :dialog-title="helpContents.useRoundRobin.title" :dialog-content="helpContents.useRoundRobin.content" size="x-small" color="grey" />
                        </div>
                        <template v-if="!request.useRoundRobin">
                          <div class="d-flex align-center mb-1 mt-2">
                            <span class="text-caption text-grey">1회 매수 금액: {{ formatCurrency(request.fixedBuyAmount) }}</span>
                            <HelpButton use-dialog :dialog-title="helpContents.fixedBuyAmount.title" :dialog-content="helpContents.fixedBuyAmount.content" size="x-small" color="grey" />
                          </div>
                          <v-slider v-model="request.fixedBuyAmount" :min="10000" :max="10000000" :step="10000" thumb-label class="mb-2" />
                        </template>
                        <div class="d-flex align-center">
                          <v-switch v-model="request.useDailyLimitRecovery" label="일일 한도 복구 사용" color="primary" density="compact" hide-details />
                          <HelpButton use-dialog :dialog-title="helpContents.dailyLimitRecovery.title" :dialog-content="helpContents.dailyLimitRecovery.content" size="x-small" color="grey" />
                        </div>
                      </v-expansion-panel-text>
                    </v-expansion-panel>

                    <!-- 리스크 관리 -->
                    <v-expansion-panel>
                      <v-expansion-panel-title>
                        <v-icon class="mr-2">mdi-shield-check</v-icon>
                        리스크 관리
                      </v-expansion-panel-title>
                      <v-expansion-panel-text>
                        <div class="d-flex align-center mb-1">
                          <span class="text-caption text-grey">일일 거래 한도: {{ request.dailyTradeLimitPct }}%
                            ({{ formatCurrency(request.initialBalance * request.dailyTradeLimitPct / 100) }})</span>
                          <HelpButton use-dialog :dialog-title="helpContents.dailyTradeLimit.title" :dialog-content="helpContents.dailyTradeLimit.content" size="x-small" color="grey" />
                        </div>
                        <v-slider v-model="request.dailyTradeLimitPct" :min="10" :max="100" :step="5" thumb-label class="mb-2" />

                        <div class="d-flex align-center mb-1">
                          <span class="text-caption text-grey">단일 종목 비중 제한: {{ request.maxPositionPct }}%
                            ({{ formatCurrency(request.initialBalance * request.maxPositionPct / 100) }})</span>
                          <HelpButton use-dialog :dialog-title="helpContents.maxPosition.title" :dialog-content="helpContents.maxPosition.content" size="x-small" color="grey" />
                        </div>
                        <v-slider v-model="request.maxPositionPct" :min="10" :max="100" :step="5" thumb-label class="mb-2" />

                        <div class="d-flex align-center mb-1">
                          <span class="text-caption text-grey">긴급 정지 기준(일일 손실): {{ request.dailyStopLossPct }}%</span>
                          <HelpButton use-dialog :dialog-title="helpContents.dailyStopLoss.title" :dialog-content="helpContents.dailyStopLoss.content" size="x-small" color="grey" />
                        </div>
                        <v-slider v-model="request.dailyStopLossPct" :min="-50" :max="0" :step="1" thumb-label />

                        <v-divider class="my-3" />
                        <div class="text-subtitle-2 mb-2 d-flex align-center">
                          <v-icon size="small" class="mr-1">mdi-shield-alert</v-icon>
                          급락장 보호 기능
                        </div>
                        <div class="d-flex align-center mb-1">
                          <v-switch
                            v-model="request.useMarketTrendFilter"
                            label="시장 추세 필터 (Day 62 기준 미지원 - KOSPI 연동 예정)"
                            color="primary" density="compact" hide-details disabled
                          />
                          <HelpButton use-dialog :dialog-title="helpContents.marketTrendFilter.title" :dialog-content="helpContents.marketTrendFilter.content" size="x-small" color="grey" />
                        </div>
                        <div class="d-flex align-center mb-1 mt-2">
                          <span class="text-caption text-grey">누적 손실 한도: {{ request.cumulativeLossLimitPct }}%</span>
                          <HelpButton use-dialog :dialog-title="helpContents.cumulativeLossLimit.title" :dialog-content="helpContents.cumulativeLossLimit.content" size="x-small" color="grey" />
                        </div>
                        <v-slider v-model="request.cumulativeLossLimitPct" :min="-50" :max="0" :step="1" thumb-label class="mb-2" />

                        <div class="d-flex align-center mb-1">
                          <span class="text-caption text-grey">연속 손절 제한: {{ request.consecutiveStopLossLimit }}회</span>
                          <HelpButton use-dialog :dialog-title="helpContents.consecutiveStopLossLimit.title" :dialog-content="helpContents.consecutiveStopLossLimit.content" size="x-small" color="grey" />
                        </div>
                        <v-slider v-model="request.consecutiveStopLossLimit" :min="1" :max="10" :step="1" thumb-label />
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
              <!-- 결과 해석 도움말 -->
              <div class="d-flex align-center mb-2">
                <span class="text-h6">📈 백테스트 결과</span>
                <HelpButton 
                  use-dialog 
                  :dialog-title="helpContents.resultSummary.title"
                  :dialog-content="helpContents.resultSummary.content"
                  color="grey-darken-1"
                />
              </div>

              <!-- 요약 카드 (코인 BacktestView.vue와 동일 구조) -->
              <v-row>
                <v-col cols="6" md="3">
                  <v-card :color="result.totalProfit >= 0 ? 'success' : 'error'" variant="tonal" class="summary-card">
                    <v-card-text class="text-center">
                      <div class="text-overline summary-label">총 수익</div>
                      <div class="text-h5 font-weight-bold summary-value">
                        {{ formatCurrency(result.totalProfit) }}
                      </div>
                      <div :class="result.totalProfitRate >= 0 ? 'text-success' : 'text-error'">
                        {{ result.totalProfitRate >= 0 ? '+' : '' }}{{ result.totalProfitRate.toFixed(2) }}%
                      </div>
                    </v-card-text>
                  </v-card>
                </v-col>
                <v-col cols="6" md="3">
                  <v-card variant="tonal" class="summary-card">
                    <v-card-text class="text-center">
                      <div class="text-overline summary-label">승률</div>
                      <div class="text-h5 font-weight-bold summary-value">{{ result.winRate.toFixed(1) }}%</div>
                      <div class="summary-sub">{{ result.winCount }}승 {{ result.loseCount }}패</div>
                    </v-card-text>
                  </v-card>
                </v-col>
                <v-col cols="6" md="3">
                  <v-card variant="tonal" class="summary-card">
                    <v-card-text class="text-center">
                      <div class="text-overline summary-label">총 거래</div>
                      <div class="text-h5 font-weight-bold summary-value">{{ result.totalTrades }}회</div>
                      <div class="summary-sub">매수 {{ result.buyCount }} / 매도 {{ result.sellCount }}</div>
                    </v-card-text>
                  </v-card>
                </v-col>
                <v-col cols="6" md="3">
                  <v-card color="warning" variant="tonal" class="summary-card">
                    <v-card-text class="text-center">
                      <div class="text-overline summary-label">최대 낙폭</div>
                      <div class="text-h5 font-weight-bold summary-value">-{{ result.maxDrawdown.toFixed(2) }}%</div>
                      <div class="summary-sub">MDD</div>
                    </v-card-text>
                  </v-card>
                </v-col>
              </v-row>

              <!-- 상세 지표 (코인 BacktestView.vue와 동일 구조) -->
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
                      <div class="text-overline">테스트 종목</div>
                      <div class="text-body-1">{{ result.stockCodes.length }}개</div>
                    </v-col>
                  </v-row>
                </v-card-text>
              </v-card>

              <!-- 자산 변동 차트 (코인 BacktestView.vue와 동일 구조) -->
              <v-card class="mt-4">
                <v-card-title class="d-flex align-center">
                  <v-icon class="mr-2">mdi-chart-line</v-icon>
                  자산 변동 추이

                  <!-- 보기 모드 토글 (60일 이상일 때만 표시) -->
                  <v-spacer />
                  <v-btn-toggle
                    v-if="result.dailyBalances.length > 60"
                    v-model="chartViewMode"
                    density="compact"
                    mandatory
                    color="primary"
                    class="ml-4 chart-view-toggle"
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
                  <!-- ⭐⭐⭐ [버그 재수정] chart-outer 신규: 스크롤 컨테이너와 라벨을 완전히 분리하는 바깥 래퍼 ⭐⭐⭐ -->
                  <div class="chart-outer">
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
                          <defs>
                            <linearGradient id="stockAreaGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                              <stop offset="0%" style="stop-color:#42A5F5;stop-opacity:0.6" />
                              <stop offset="100%" style="stop-color:#42A5F5;stop-opacity:0.1" />
                            </linearGradient>
                          </defs>

                          <path :d="areaPath" fill="url(#stockAreaGradient)" />

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

                          <path :d="linePath" fill="none" stroke="#1976D2" stroke-width="2.5" />

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

                    <!-- chart-scroll-container 바깥이므로 스크롤과 무관하게 항상 고정된 위치 -->
                    <div class="chart-labels">
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
                  </div>

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

              <!-- 종목별 성과 (코인 BacktestView.vue와 동일 구조) -->
              <v-card class="mt-4">
                <v-card-title>
                  <v-icon class="mr-2">mdi-finance</v-icon>
                  종목별 성과
                </v-card-title>
                <v-card-text>
                  <v-data-table
                    :headers="stockHeaders"
                    :items="result.stockPerformances"
                    density="compact"
                  >
                    <template #item.stockCode="{ item }">
                      <v-chip size="small" color="primary" variant="outlined">
                        {{ stockNameMap[item.stockCode] || item.stockCode }}
                      </v-chip>
                    </template>
                    <template #item.winRate="{ item }">
                      {{ item.winCount }}승 {{ item.loseCount }}패
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
                    <template #item.avgHoldingDays="{ item }">
                      {{ item.avgHoldingDays != null ? item.avgHoldingDays.toFixed(1) + '일' : '-' }}
                    </template>
                  </v-data-table>
                </v-card-text>
              </v-card>

              <!-- 거래 내역 (코인 BacktestView.vue와 동일 구조) -->
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
                    <template #item.stockCode="{ item }">
                      <span class="font-weight-medium">{{ stockNameMap[item.stockCode] || item.stockCode }}</span>
                    </template>
                    <template #item.price="{ item }">
                      {{ formatCurrency(item.price) }}
                    </template>
                    <template #item.amount="{ item }">
                      {{ formatCurrency(item.amount) }}
                    </template>
                    <template #item.profit="{ item }">
                      <span v-if="item.profit != null" :class="item.profit >= 0 ? 'text-success' : 'text-error'">
                        {{ item.profit >= 0 ? '+' : '' }}{{ formatCurrency(item.profit) }}
                      </span>
                      <span v-else>-</span>
                    </template>
                    <template #item.profitRate="{ item }">
                      <span v-if="item.profitRate != null" :class="item.profitRate >= 0 ? 'text-success' : 'text-error'">
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
import HelpButton from '@/components/HelpButton.vue'
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

// ---- 차트 (코인 BacktestView.vue와 동일 구조: 스크롤 모드 지원) ----
const hoveredIndex = ref(-1)
const tooltipX = ref(0)
const svgWidth = 800
const svgHeight = 350
const svgPadding = 40

// ⭐ [UI 통일] 전체보기/스크롤보기 토글 상태
const chartViewMode = ref<'full' | 'scroll'>('full')

const isScrollMode = computed(() => {
  if (!result.value?.dailyBalances?.length) return false
  return chartViewMode.value === 'scroll' && result.value.dailyBalances.length > 60
})

// 스크롤 모드에서 점 간격 (px)
const pointSpacing = 25

// 동적 SVG 너비 (스크롤 모드일 때 데이터 개수만큼 넓어짐)
const dynamicSvgWidth = computed(() => {
  if (!result.value?.dailyBalances?.length) return svgWidth
  if (isScrollMode.value) {
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

// ⭐ [UI 통일] svgWidth 고정값 → dynamicSvgWidth로 변경 (스크롤 모드 대응)
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

// ⭐⭐⭐ [도움말 추가] 코인 BacktestView.vue의 helpContents 구성 방식을 그대로 따르되,
// 수치/예시는 주식·ETF 기준(수수료 0.015%, KIS 1주 단위 등)으로 새로 작성 ⭐⭐⭐
const helpContents = {
  // ---- 매수/매도 조건 카테고리 ----
  maPeriod: {
    title: '📊 MA (이동평균선)',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"최근 며칠간의 평균 가격"</p>
          </div>
        </div>
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">📈 ETF 예시</span>
          </div>
          <div style="padding-left: 24px; line-height: 1.8; color: #CFD8DC;">
            TIGER 나스닥100레버리지 가격이 매일 바뀐다면,<br/>
            - 어제: 12,000원 / 그제: 12,300원 / 3일 전: 11,700원<br/>
            → <strong style="color: #4CAF50;">3일 이동평균: 12,000원</strong><br/><br/>
            <strong style="color: #4CAF50;">20일 이동평균 = 최근 20 거래일 평균 가격</strong><br/><br/>
            오늘 가격이 평균보다 3% 낮다면 "싸졌다"고 판단해 매수를 검토합니다.
          </div>
        </div>
        <div class="mb-2">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">📋 설정값 안내</span>
          </div>
          <div style="padding-left: 24px;">
            <table style="width: 100%; border-collapse: collapse; border: 1px solid #E0E0E0; border-radius: 8px; overflow: hidden; font-size: 13px;">
              <thead>
                <tr style="background-color: #ECEFF1;">
                  <th style="padding: 10px 12px; text-align: left;">설정값</th>
                  <th style="padding: 10px 12px; text-align: left;">의미</th>
                  <th style="padding: 10px 12px; text-align: left;">적합한 사람</th>
                </tr>
              </thead>
              <tbody>
                <tr><td style="padding: 8px 12px;">7~14일</td><td style="padding: 8px 12px;">단기 추세</td><td style="padding: 8px 12px;">자주 거래하고 싶은 사람</td></tr>
                <tr style="background-color: #E3F2FD;">
                  <td style="padding: 8px 12px; color: #1565C0;"><strong>20일</strong></td>
                  <td style="padding: 8px 12px; color: #1565C0;">중기 추세</td>
                  <td style="padding: 8px 12px; color: #1565C0;">초보자 추천 ✅</td>
                </tr>
                <tr><td style="padding: 8px 12px;">30일</td><td style="padding: 8px 12px;">장기 추세</td><td style="padding: 8px 12px;">느긋한 투자자</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `
  },

  buyThreshold: {
    title: '🏷️ 매수 기준 (MA 대비)',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"20일 평균 가격보다 얼마나 떨어지면 살 것인가?"</p>
          </div>
        </div>
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">🏷️ 예시</span>
          </div>
          <div style="padding-left: 24px; line-height: 1.8; color: #CFD8DC;">
            20일 평균 12,000원인 ETF라면<br/>
            - -3% 설정: 11,640원 이하로 떨어지면 매수<br/>
            - -6% 설정: 11,280원 이하로 떨어지면 매수<br/><br/>
            ⚠️ 주식/ETF는 코인보다 하루 변동폭이 작은 편이라(레버리지 ETF 제외),
            기본값을 코인(-6%)보다 완만한 <strong style="color: #4CAF50;">-3%</strong>로 설정했습니다.
          </div>
        </div>
        <div class="mb-2">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">📋 설정값 안내</span>
          </div>
          <div style="padding-left: 24px;">
            <table style="width: 100%; border-collapse: collapse; border: 1px solid #E0E0E0; border-radius: 8px; overflow: hidden; font-size: 13px;">
              <thead><tr style="background-color: #ECEFF1;"><th style="padding: 10px 12px; text-align: left;">설정값</th><th style="padding: 10px 12px; text-align: left;">의미</th><th style="padding: 10px 12px; text-align: left;">거래 빈도</th></tr></thead>
              <tbody>
                <tr><td style="padding: 8px 12px;">-1.5%</td><td style="padding: 8px 12px;">조금만 떨어져도 삼</td><td style="padding: 8px 12px;">많음 (공격적)</td></tr>
                <tr style="background-color: #E3F2FD;"><td style="padding: 8px 12px; color: #1565C0;"><strong>-3%</strong></td><td style="padding: 8px 12px; color: #1565C0;">적당히 떨어지면 삼</td><td style="padding: 8px 12px; color: #1565C0;">보통 ✅</td></tr>
                <tr><td style="padding: 8px 12px;">-6% 이상</td><td style="padding: 8px 12px;">많이 떨어져야 삼</td><td style="padding: 8px 12px;">적음 (신중)</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `
  },

  sellTarget: {
    title: '🤝 목표 수익률',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span></div>
          <div style="padding-left: 24px;"><p class="text-body-2 text-grey-darken-3 mb-0">"얼마 오르면 팔 것인가?"</p></div>
        </div>
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">🤝 예시</span></div>
          <div style="padding-left: 24px; line-height: 1.8; color: #CFD8DC;">
            11,640원에 매수했다면<br/>
            - 2.5% 설정: 11,931원 되면 자동 매도<br/>
            - 5% 설정: 12,222원 되면 자동 매도<br/><br/>
            💡 주식/ETF는 수수료(0.015%)가 코인(0.05%)보다 낮아서,
            상대적으로 낮은 목표 수익률(2.5%)로도 여러 번 거래해서 누적 수익을 노릴 수 있습니다.
          </div>
        </div>
        <div class="mb-2">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">📋 설정값 안내</span></div>
          <div style="padding-left: 24px;">
            <table style="width: 100%; border-collapse: collapse; border: 1px solid #E0E0E0; border-radius: 8px; overflow: hidden; font-size: 13px;">
              <thead><tr style="background-color: #ECEFF1;"><th style="padding: 10px 12px; text-align: left;">설정값</th><th style="padding: 10px 12px; text-align: left;">특징</th></tr></thead>
              <tbody>
                <tr style="background-color: #E3F2FD;"><td style="padding: 8px 12px; color: #1565C0;"><strong>2.5%</strong></td><td style="padding: 8px 12px; color: #1565C0;">자주 수익 실현, 초보자 추천 ✅</td></tr>
                <tr><td style="padding: 8px 12px;">5% 이상</td><td style="padding: 8px 12px;">큰 수익 가능하나 도달 못할 수도 있음</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `
  },

  useStopLoss: {
    title: '🛑 손절매 사용',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span></div>
          <div style="padding-left: 24px;"><p class="text-body-2 text-grey-darken-3 mb-0">"손실이 일정 수준 이상 커지면 자동으로 손절할지 여부"</p></div>
        </div>
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-radius: 8px; padding: 16px;">
          <div style="padding-left: 24px; line-height: 1.8; color: #CFD8DC;">
            레버리지 ETF는 하락장에서 낙폭이 지수보다 2배 크게 나타날 수 있어,
            손절매를 꺼두면 손실이 눈덩이처럼 불어날 수 있습니다.<br/><br/>
            ⚠️ <strong style="color: #FF8A65;">특히 레버리지/인버스 상품은 손절매를 켜두는 것을 강력 권장합니다.</strong>
          </div>
        </div>
      </div>
    `
  },

  stopLoss: {
    title: '🛑 손절매 기준',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span></div>
          <div style="padding-left: 24px;"><p class="text-body-2 text-grey-darken-3 mb-0">"이만큼 손실나면 무조건 판다"</p></div>
        </div>
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-radius: 8px; padding: 16px;">
          <div style="padding-left: 24px; line-height: 1.8; color: #CFD8DC;">
            11,640원에 매수했는데 -5% 설정이면,<br/>
            10,558원까지 떨어지면 손실을 감수하고 즉시 매도합니다.<br/><br/>
            💡 레버리지 ETF는 변동성이 커서 코인 기본값(-8%)보다 타이트한
            <strong style="color: #4CAF50;">-5%</strong>를 기본값으로 설정했습니다.
          </div>
        </div>
      </div>
    `
  },

  trailingStop: {
    title: '📉 트레일링 스톱',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span></div>
          <div style="padding-left: 24px;"><p class="text-body-2 text-grey-darken-3 mb-0">"최고가 대비 일정 % 하락하면 이익을 지키고 판다"</p></div>
        </div>
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-radius: 8px; padding: 16px;">
          <div style="padding-left: 24px; line-height: 1.8; color: #CFD8DC;">
            매수 후 15,000원까지 올랐다가(수익 중),<br/>
            2.5% 설정이면 14,625원으로 떨어지는 순간 매도합니다.<br/><br/>
            💡 목표 수익률에 도달하기 전이라도, 오르던 게 꺾이면 수익을 확정 지어주는 안전장치입니다.
          </div>
        </div>
      </div>
    `
  },

  maxHoldings: {
    title: '📦 종목당 최대 보유',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span></div>
          <div style="padding-left: 24px;"><p class="text-body-2 text-grey-darken-3 mb-0">"한 종목을 몇 번까지 나눠서(분할) 살 것인가?"</p></div>
        </div>
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-radius: 8px; padding: 16px;">
          <div style="padding-left: 24px; line-height: 1.8; color: #CFD8DC;">
            3건으로 설정하면, TIGER 나스닥100레버리지가 계속 하락할 때
            최대 3번까지 분할 매수(물타기)해서 평균 단가를 낮출 수 있습니다.<br/><br/>
            💡 너무 많이 설정하면 한 종목에 자금이 몰려 리스크가 커집니다.
          </div>
        </div>
      </div>
    `
  },

  additionalDropPct: {
    title: '📉 추가 매수(물타기) 하락률',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span></div>
          <div style="padding-left: 24px;"><p class="text-body-2 text-grey-darken-3 mb-0">"직전 매수가 대비 몇 % 더 떨어져야 추가로 살 것인가?"</p></div>
        </div>
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-radius: 8px; padding: 16px;">
          <div style="padding-left: 24px; line-height: 1.8; color: #CFD8DC;">
            직전 매수가가 11,640원이고 1% 설정이면,
            가격이 11,524원 이하로 한 번 더 떨어져야 다음 분할 매수가 실행됩니다.<br/><br/>
            💡 이 값이 너무 작으면 짧은 간격으로 계속 물타기를 하게 되어 자금이 빨리 소진될 수 있습니다.
          </div>
        </div>
      </div>
    `
  },
  // ---- 기술적 지표 설정 카테고리 ----
  rsiSettings: {
    title: '📊 RSI (상대강도지수)',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span></div>
          <div style="padding-left: 24px;"><p class="text-body-2 text-grey-darken-3 mb-0">"최근에 너무 많이 팔렸는지(과매도), 너무 많이 샀는지(과매수)를 0~100 숫자로 나타낸 지표"</p></div>
        </div>
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">📊 예시</span></div>
          <div style="padding-left: 24px; line-height: 1.8; color: #CFD8DC;">
            RSI 35 이하 → "최근 많이 떨어졌다, 반등 가능성" → 매수 신호 후보<br/>
            RSI 65 이상 → "최근 많이 올랐다, 조정 가능성" → 매도 신호 후보<br/><br/>
            💡 코인은 변동성이 커서 임계값을 32/68(더 극단적)로 쓰지만,
            주식/ETF는 상대적으로 완만해서 <strong style="color: #4CAF50;">35/65</strong>로 다소 여유 있게 설정했습니다.
          </div>
        </div>
        <div class="mb-2">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">📋 설정값 안내</span></div>
          <div style="padding-left: 24px;">
            <table style="width: 100%; border-collapse: collapse; border: 1px solid #E0E0E0; border-radius: 8px; overflow: hidden; font-size: 13px;">
              <thead><tr style="background-color: #ECEFF1;"><th style="padding: 10px 12px; text-align: left;">항목</th><th style="padding: 10px 12px; text-align: left;">의미</th></tr></thead>
              <tbody>
                <tr><td style="padding: 8px 12px;">기간</td><td style="padding: 8px 12px;">RSI 계산에 사용할 거래일 수 (기본 14일)</td></tr>
                <tr><td style="padding: 8px 12px;">매수 ≤</td><td style="padding: 8px 12px;">이 값 이하면 매수 신호 조건 중 하나로 인정</td></tr>
                <tr><td style="padding: 8px 12px;">매도 ≥</td><td style="padding: 8px 12px;">이 값 이상이면 매도 신호 조건 중 하나로 인정</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `
  },

  bbSettings: {
    title: '📏 볼린저 밴드',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span></div>
          <div style="padding-left: 24px;"><p class="text-body-2 text-grey-darken-3 mb-0">"평균 가격 위아래로 변동폭만큼 밴드를 그려서, 가격이 밴드 하단에 닿으면 '많이 싸졌다'고 보는 지표"</p></div>
        </div>
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-radius: 8px; padding: 16px;">
          <div style="padding-left: 24px; line-height: 1.8; color: #CFD8DC;">
            20일 평균가 12,000원, 표준편차 2배 밴드라면<br/>
            하단선 아래로 가격이 떨어지면 "평소보다 비정상적으로 쌌다"고 판단해 매수 후보에 넣습니다.
          </div>
        </div>
        <div class="mb-2">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">📋 설정값 안내</span></div>
          <div style="padding-left: 24px;">
            <table style="width: 100%; border-collapse: collapse; border: 1px solid #E0E0E0; border-radius: 8px; overflow: hidden; font-size: 13px;">
              <thead><tr style="background-color: #ECEFF1;"><th style="padding: 10px 12px; text-align: left;">항목</th><th style="padding: 10px 12px; text-align: left;">의미</th></tr></thead>
              <tbody>
                <tr><td style="padding: 8px 12px;">기간</td><td style="padding: 8px 12px;">밴드 계산 기준 거래일 수 (기본 20일)</td></tr>
                <tr><td style="padding: 8px 12px;">표준편차</td><td style="padding: 8px 12px;">밴드 폭 배수. 클수록 밴드가 넓어져 신호가 덜 발생</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `
  },

  volumeThreshold: {
    title: '📢 거래량 급증 기준',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span></div>
          <div style="padding-left: 24px;"><p class="text-body-2 text-grey-darken-3 mb-0">"평소보다 거래량이 얼마나 늘어야 의미 있는 신호로 볼 것인가?"</p></div>
        </div>
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-radius: 8px; padding: 16px;">
          <div style="padding-left: 24px; line-height: 1.8; color: #CFD8DC;">
            120% 설정 시, 오늘 거래량이 평균 거래량의 1.2배 이상이면
            "관심이 몰리고 있다"고 보고 매수 신호 조건 중 하나로 인정합니다.<br/><br/>
            💡 국내 상장 ETF는 코인 대비 거래량 변동폭이 완만한 편이라
            코인 기본값(140%)보다 낮은 <strong style="color: #4CAF50;">120%</strong>를 기본값으로 두었습니다.
          </div>
        </div>
      </div>
    `
  },

  // ---- 레버리지 ETF 보유기간 제한 카테고리 (Day 62 신규, 코인에는 없는 항목) ----
  maxHoldingDays: {
    title: '⏱️ 레버리지 ETF 보유기간 제한',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span></div>
          <div style="padding-left: 24px;"><p class="text-body-2 text-grey-darken-3 mb-0">"레버리지 ETF를 너무 오래 들고 있으면 손해를 볼 수 있어서, 최대 며칠까지만 보유하고 자동으로 파는 안전장치"</p></div>
        </div>
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">⚠️ 왜 필요한가? (변동성 끌림, Volatility Drag)</span></div>
          <div style="padding-left: 24px; line-height: 1.8; color: #CFD8DC;">
            레버리지 ETF는 매일의 등락률을 2배로 따라가도록 설계되어 있습니다.<br/>
            그런데 지수가 하루는 +10%, 다음날 -10%처럼 오르내리기만 반복하면(횡보장),<br/>
            <strong style="color: #FF8A65;">지수 자체는 제자리인데 레버리지 ETF는 오히려 손실이 누적</strong>됩니다.<br/><br/>
            예: 지수 100 → +10%(110) → -10%(99) = 지수는 -1%<br/>
            레버리지 2배: 100 → +20%(120) → -20%(96) = <strong style="color: #F44336;">-4%</strong> (더 큰 손실)<br/><br/>
            그래서 목표 수익률/손절매에 안 걸리더라도, 설정한 보유기간(기본 20거래일)이 지나면
            <strong style="color: #4CAF50;">묻지도 따지지도 않고 강제로 매도</strong>해서 이 손실 누적을 원천 차단합니다.
          </div>
        </div>
        <div class="mb-2">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">📋 설정값 안내</span></div>
          <div style="padding-left: 24px;">
            <table style="width: 100%; border-collapse: collapse; border: 1px solid #E0E0E0; border-radius: 8px; overflow: hidden; font-size: 13px;">
              <thead><tr style="background-color: #ECEFF1;"><th style="padding: 10px 12px; text-align: left;">설정값</th><th style="padding: 10px 12px; text-align: left;">특징</th></tr></thead>
              <tbody>
                <tr><td style="padding: 8px 12px;">5~10일</td><td style="padding: 8px 12px;">매우 보수적, 단기 스윙에 적합</td></tr>
                <tr style="background-color: #E3F2FD;"><td style="padding: 8px 12px; color: #1565C0;"><strong>20일</strong></td><td style="padding: 8px 12px; color: #1565C0;">약 1개월, 권장 기본값 ✅</td></tr>
                <tr><td style="padding: 8px 12px;">30일 이상</td><td style="padding: 8px 12px;">변동성 끌림 리스크 증가</td></tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="glossary-section mb-2" style="margin-top: 12px; padding: 12px; background: #FFF3E0; border-radius: 8px;">
          <p class="mb-0" style="font-size: 13px; color: #E65100;">
            ⚠️ 이 설정은 일반 지수추종 ETF(비레버리지)에는 원래 불필요한 로직입니다.
            현재는 선택한 모든 종목에 동일 적용되지만, 추후 레버리지/인버스 상품에만 선택 적용되도록 개선 예정입니다.
          </p>
        </div>
      </div>
    `
  },
  // ---- 매수 방식 카테고리 ----
  useRoundRobin: {
    title: '🔄 매수 방식 (라운드로빈 vs 고정 금액)',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span></div>
          <div style="padding-left: 24px;"><p class="text-body-2 text-grey-darken-3 mb-0">"매수 신호가 여러 종목에서 동시에 뜨면, 남은 한도를 어떻게 나눠서 쓸 것인가?"</p></div>
        </div>
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-radius: 8px; padding: 16px;">
          <div style="padding-left: 24px; line-height: 1.8; color: #CFD8DC;">
            남은 한도 10만원, TIGER·KODEX 나스닥100레버리지 둘 다 매수 신호 발생 시<br/><br/>
            <strong style="color: #4CAF50;">🔄 라운드로빈:</strong> 5만원씩 균등 분배 (분산 투자)<br/>
            <strong style="color: #4CAF50;">💵 고정 금액:</strong> 신호 순서대로 설정한 고정 금액(예: 10만원)씩 매수, 한도 소진 시 나머지는 다음 기회로
          </div>
        </div>
      </div>
    `
  },

  fixedBuyAmount: {
    title: '💵 1회 매수 금액',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span></div>
          <div style="padding-left: 24px;"><p class="text-body-2 text-grey-darken-3 mb-0">"고정 금액 매수 모드일 때, 매수 신호 1건당 얼마씩 살 것인가?"</p></div>
        </div>
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-radius: 8px; padding: 16px;">
          <div style="padding-left: 24px; line-height: 1.8; color: #CFD8DC;">
            10만원으로 설정하면, 매수 신호가 뜰 때마다 10만원어치씩(가격에 따라 정수 주 단위로 환산) 매수합니다.<br/><br/>
            💡 주식은 1주 단위로만 매수 가능하므로, 종목 가격이 비싸면 설정한 금액보다 적게 매수될 수 있습니다.
          </div>
        </div>
      </div>
    `
  },

  // ---- 리스크 관리 카테고리 ----
  dailyTradeLimit: {
    title: '💰 일일 거래 한도',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span></div>
          <div style="padding-left: 24px;"><p class="text-body-2 text-grey-darken-3 mb-0">"하루에 최대 얼마까지 매수에 쓸 것인가?"</p></div>
        </div>
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-radius: 8px; padding: 16px;">
          <div style="padding-left: 24px; line-height: 1.8; color: #CFD8DC;">
            초기 자본 100만원, 20% 설정이면 하루 최대 20만원까지만 매수에 씁니다.<br/>
            여러 종목이 동시에 신호를 내도 이 한도를 넘지 않습니다.
          </div>
        </div>
      </div>
    `
  },

  dailyLimitRecovery: {
    title: '♻️ 매도 시 일일 한도 복구',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span></div>
          <div style="padding-left: 24px;"><p class="text-body-2 text-grey-darken-3 mb-0">"오늘 판 금액만큼, 오늘 다시 살 수 있는 한도를 되살려줄 것인가?"</p></div>
        </div>
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-radius: 8px; padding: 16px;">
          <div style="padding-left: 24px; line-height: 1.8; color: #CFD8DC;">
            일일 한도 20만원을 다 쓴 상태에서 5만원어치를 매도하면,<br/>
            ON: 한도가 5만원 복구되어 오늘 다시 5만원 매수 가능<br/>
            OFF: 하루 매수 한도는 그대로 소진된 채 유지
          </div>
        </div>
      </div>
    `
  },

  maxPosition: {
    title: '🥚 단일 종목 비중 제한',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span></div>
          <div style="padding-left: 24px;"><p class="text-body-2 text-grey-darken-3 mb-0">"한 종목에 최대 얼마까지 투자할 것인가?"</p></div>
        </div>
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">🥚 계란 비유</span></div>
          <div style="padding-left: 24px; line-height: 1.8; color: #CFD8DC;">
            "계란을 한 바구니에 담지 마라"<br/><br/>
            100만원이 있을 때, 25% 설정이면 TIGER 나스닥100레버리지 한 종목에는
            최대 25만원까지만 투자되고, 나머지는 다른 종목에 분산됩니다.<br/><br/>
            ⚠️ 레버리지 ETF는 변동성이 커서, 한 종목에 몰빵하면 손실도 그만큼 커질 수 있습니다.
          </div>
        </div>
      </div>
    `
  },

  dailyStopLoss: {
    title: '🚨 긴급 정지 (일일 손실률)',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span></div>
          <div style="padding-left: 24px;"><p class="text-body-2 text-grey-darken-3 mb-0">"오늘 하루 손실이 이만큼 나면, 오늘은 더 이상 신규 매수를 하지 않는다"</p></div>
        </div>
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-radius: 8px; padding: 16px;">
          <div style="padding-left: 24px; line-height: 1.8; color: #CFD8DC;">
            -5% 설정 시, 하루 시작 자산 대비 -5% 손실이 나면
            그날은 신규 매수를 중단합니다 (보유 종목의 손절매/트레일링 스톱은 계속 작동).
          </div>
        </div>
      </div>
    `
  },

  marketTrendFilter: {
    title: '📉 시장 추세 필터',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span></div>
          <div style="padding-left: 24px;"><p class="text-body-2 text-grey-darken-3 mb-0">"시장 전체가 하락 추세일 때, 개별 종목 신호와 무관하게 매수를 아예 막는 기능"</p></div>
        </div>
        <div class="glossary-example-card mb-4" style="background-color: #FFF3E0; border-radius: 8px; padding: 16px;">
          <p class="mb-0" style="font-size: 13px; color: #E65100;">
            ⚠️ 코인은 BTC 20일선을 시장 기준으로 사용하지만, 국내 주식은 아직
            대표 지수(KOSPI/KOSDAQ) 프록시 연동이 되어 있지 않아 <strong>Day 62 기준으로는 이 옵션을 켜도
            실제 매수 제한에는 영향을 주지 않습니다.</strong> KOSPI/KOSDAQ 연동 완료 후 정상 작동 예정입니다.
          </p>
        </div>
      </div>
    `
  },

  cumulativeLossLimit: {
    title: '🛑 누적 손실 한도',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span></div>
          <div style="padding-left: 24px;"><p class="text-body-2 text-grey-darken-3 mb-0">"시작 이후 누적 손실이 이 비율에 도달하면, 전체 거래를 완전히 중단한다"</p></div>
        </div>
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-radius: 8px; padding: 16px;">
          <div style="padding-left: 24px; line-height: 1.8; color: #CFD8DC;">
            초기 자본 100만원, -10% 설정이면<br/>
            누적 손실이 -10만원에 도달하는 순간 이후로는 신규 매수를 완전히 중단합니다.<br/><br/>
            💡 하루 단위(긴급 정지)가 아니라 <strong style="color: #F44336;">전체 백테스트 기간 누적</strong> 기준입니다.
          </div>
        </div>
      </div>
    `
  },

  consecutiveStopLossLimit: {
    title: '🔁 연속 손절 제한',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2"><span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span></div>
          <div style="padding-left: 24px;"><p class="text-body-2 text-grey-darken-3 mb-0">"같은 종목에서 손절매가 연속으로 몇 번 발생하면, 그 종목은 잠시 매수를 금지할 것인가?"</p></div>
        </div>
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-radius: 8px; padding: 16px;">
          <div style="padding-left: 24px; line-height: 1.8; color: #CFD8DC;">
            3회 설정 시, TIGER 나스닥100레버리지에서 손절매가 3번 연속 발생하면
            해당 종목은 하루 동안 매수 금지 상태가 됩니다.<br/><br/>
            💡 "물타기를 해도 계속 실패하는 종목"에 계속 자금을 투입하는 걸 막는 안전장치입니다.
          </div>
        </div>
      </div>
    `
  },
  resultSummary: {
    title: '📊 백테스트 결과 해석',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">📈 총 수익/수익률</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-2">"투자한 돈 대비 얼마나 벌었나?"</p>
            <div style="background-color: #263238; border-radius: 8px; padding: 12px; color: #CFD8DC; line-height: 1.6;">
              100만원 투자해서 105만원이 됐다면<br/>
              수익률 = (105만 - 100만) / 100만 × 100% = <strong style="color: #4CAF50;">5%</strong>
            </div>
          </div>
        </div>

        <hr style="border: none; border-top: 1px solid #E0E0E0; margin: 16px 0;" />

        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🏆 승률</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-2">"거래 중 몇 번이나 이겼나?"</p>
            <div style="background-color: #263238; border-radius: 8px; padding: 12px; color: #CFD8DC; line-height: 1.6; margin-bottom: 12px;">
              10번 거래해서 6번 수익, 4번 손실<br/>
              → 승률 = 6/10 × 100% = <strong style="color: #4CAF50;">60%</strong><br/><br/>
              <strong style="color: #FF9800;">주의:</strong> 승률이 높아도 손실이 클 수 있음!
            </div>
            <table style="width: 100%; border-collapse: collapse; border: 1px solid #E0E0E0; border-radius: 8px; overflow: hidden; font-size: 13px;">
              <thead>
                <tr style="background-color: #ECEFF1;">
                  <th style="padding: 8px 12px; text-align: left; font-weight: 600;">승률</th>
                  <th style="padding: 8px 12px; text-align: left; font-weight: 600;">평가</th>
                </tr>
              </thead>
              <tbody>
                <tr><td style="padding: 6px 12px; border-bottom: 1px solid #EEE;">70%+</td><td style="padding: 6px 12px; border-bottom: 1px solid #EEE;">매우 좋음</td></tr>
                <tr style="background-color: #E3F2FD;"><td style="padding: 6px 12px; border-bottom: 1px solid #EEE; color: #1565C0;"><strong>50~70%</strong></td><td style="padding: 6px 12px; border-bottom: 1px solid #EEE; color: #1565C0;">양호 ✅</td></tr>
                <tr><td style="padding: 6px 12px;">50% 미만</td><td style="padding: 6px 12px;">전략 점검 필요</td></tr>
              </tbody>
            </table>
          </div>
        </div>

        <hr style="border: none; border-top: 1px solid #E0E0E0; margin: 16px 0;" />

        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">📉 MDD (최대 낙폭)</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-2">"최고점에서 최저점까지 얼마나 떨어졌나?"</p>
            <div style="background-color: #263238; border-radius: 8px; padding: 12px; color: #CFD8DC; line-height: 1.6; margin-bottom: 12px;">
              자산 변동: 100만 → 120만 → 90만 → 110만<br/>
              MDD = (120만 - 90만) / 120만 × 100% = <strong style="color: #4CAF50;">25%</strong><br/><br/>
              <strong style="color: #4CAF50;">의미:</strong> "가장 힘들 때 25% 떨어졌다"
            </div>
            <table style="width: 100%; border-collapse: collapse; border: 1px solid #E0E0E0; border-radius: 8px; overflow: hidden; font-size: 13px;">
              <thead>
                <tr style="background-color: #ECEFF1;">
                  <th style="padding: 8px 12px; text-align: left; font-weight: 600;">MDD</th>
                  <th style="padding: 8px 12px; text-align: left; font-weight: 600;">평가</th>
                </tr>
              </thead>
              <tbody>
                <tr><td style="padding: 6px 12px; border-bottom: 1px solid #EEE;">10% 이하</td><td style="padding: 6px 12px; border-bottom: 1px solid #EEE;">매우 안정</td></tr>
                <tr style="background-color: #E3F2FD;"><td style="padding: 6px 12px; border-bottom: 1px solid #EEE; color: #1565C0;"><strong>10~20%</strong></td><td style="padding: 6px 12px; border-bottom: 1px solid #EEE; color: #1565C0;">양호 ✅</td></tr>
                <tr><td style="padding: 6px 12px; border-bottom: 1px solid #EEE;">20~30%</td><td style="padding: 6px 12px; border-bottom: 1px solid #EEE;">변동성 큼</td></tr>
                <tr><td style="padding: 6px 12px;">30%+</td><td style="padding: 6px 12px;">전략 재검토 필요</td></tr>
              </tbody>
            </table>
          </div>
        </div>

        <hr style="border: none; border-top: 1px solid #E0E0E0; margin: 16px 0;" />

        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">⚖️ 샤프 비율</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-2">"위험 대비 얼마나 효율적으로 벌었나?"</p>
            <div style="background-color: #263238; border-radius: 8px; padding: 12px; color: #CFD8DC; line-height: 1.6; margin-bottom: 12px;">
              <strong style="color: #4CAF50;">전략 A:</strong> 수익 10%, 변동성(위험) 5% → 샤프 = <strong style="color: #4CAF50;">2.0</strong><br/>
              <strong style="color: #FF9800;">전략 B:</strong> 수익 10%, 변동성(위험) 20% → 샤프 = <strong style="color: #FF9800;">0.5</strong><br/><br/>
              같은 10% 수익이지만 A가 훨씬 효율적!
            </div>
            <table style="width: 100%; border-collapse: collapse; border: 1px solid #E0E0E0; border-radius: 8px; overflow: hidden; font-size: 13px;">
              <thead>
                <tr style="background-color: #ECEFF1;">
                  <th style="padding: 8px 12px; text-align: left; font-weight: 600;">샤프 비율</th>
                  <th style="padding: 8px 12px; text-align: left; font-weight: 600;">평가</th>
                </tr>
              </thead>
              <tbody>
                <tr><td style="padding: 6px 12px; border-bottom: 1px solid #EEE;">2.0+</td><td style="padding: 6px 12px; border-bottom: 1px solid #EEE;">매우 우수</td></tr>
                <tr style="background-color: #E3F2FD;"><td style="padding: 6px 12px; border-bottom: 1px solid #EEE; color: #1565C0;"><strong>1.0~2.0</strong></td><td style="padding: 6px 12px; border-bottom: 1px solid #EEE; color: #1565C0;">양호 ✅</td></tr>
                <tr><td style="padding: 6px 12px; border-bottom: 1px solid #EEE;">0~1.0</td><td style="padding: 6px 12px; border-bottom: 1px solid #EEE;">개선 여지 있음</td></tr>
                <tr><td style="padding: 6px 12px;">0 미만</td><td style="padding: 6px 12px;">전략 재검토 필요</td></tr>
              </tbody>
            </table>
          </div>
        </div>

        <hr style="border: none; border-top: 1px solid #E0E0E0; margin: 16px 0;" />

        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🎲 손익비</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-2">"이길 때 버는 돈 vs 질 때 잃는 돈"</p>
            <div style="background-color: #263238; border-radius: 8px; padding: 12px; color: #CFD8DC; line-height: 1.6; margin-bottom: 12px;">
              10번 거래 결과:<br/>
              - 이긴 5번: 평균 +3만원<br/>
              - 진 5번: 평균 -2만원<br/>
              손익비 = 3만 / 2만 = <strong style="color: #4CAF50;">1.5</strong><br/><br/>
              <strong style="color: #4CAF50;">의미:</strong> 승률 50%여도 돈을 번다!
            </div>
            <table style="width: 100%; border-collapse: collapse; border: 1px solid #E0E0E0; border-radius: 8px; overflow: hidden; font-size: 13px;">
              <thead>
                <tr style="background-color: #ECEFF1;">
                  <th style="padding: 8px 12px; text-align: left; font-weight: 600;">손익비</th>
                  <th style="padding: 8px 12px; text-align: left; font-weight: 600;">평가</th>
                  <th style="padding: 8px 12px; text-align: left; font-weight: 600;">최소 필요 승률</th>
                </tr>
              </thead>
              <tbody>
                <tr><td style="padding: 6px 12px; border-bottom: 1px solid #EEE;">3.0+</td><td style="padding: 6px 12px; border-bottom: 1px solid #EEE;">매우 우수</td><td style="padding: 6px 12px; border-bottom: 1px solid #EEE;">25%만 이겨도 수익</td></tr>
                <tr style="background-color: #E3F2FD;"><td style="padding: 6px 12px; border-bottom: 1px solid #EEE; color: #1565C0;"><strong>1.5~3.0</strong></td><td style="padding: 6px 12px; border-bottom: 1px solid #EEE; color: #1565C0;">양호 ✅</td><td style="padding: 6px 12px; border-bottom: 1px solid #EEE; color: #1565C0;">40% 이상 필요</td></tr>
                <tr><td style="padding: 6px 12px; border-bottom: 1px solid #EEE;">1.0~1.5</td><td style="padding: 6px 12px; border-bottom: 1px solid #EEE;">보통</td><td style="padding: 6px 12px; border-bottom: 1px solid #EEE;">50% 이상 필요</td></tr>
                <tr><td style="padding: 6px 12px;">1.0 미만</td><td style="padding: 6px 12px;">주의</td><td style="padding: 6px 12px;">높은 승률 필요</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `
  },
}
</script>

<style scoped>

/* ⭐⭐⭐ [디테일 수정] 전체보기/스크롤보기 버튼 그룹 테두리 ⭐⭐⭐ */
.chart-view-toggle {
  border: 2px solid #000000 !important;
  border-radius: 8px;
}

/* ⭐⭐⭐ [디테일 수정] 요약 카드 테두리 + 텍스트 대비 개선 ⭐⭐⭐ */
.summary-card {
  border: 2px solid #000000;
}
.summary-label {
  color: #000000 !important;
}
.summary-value {
  color: #000000 !important;
}
.summary-sub {
  color: #000000 !important;
  font-size: 0.75rem;
}

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

/* ⭐⭐⭐ [UI 통일] 코인 BacktestView.vue와 동일한 스크롤 모드 CSS ⭐⭐⭐ */
.chart-scroll-container {
  position: relative;
  width: 100%;
}

/* ⭐⭐⭐ [버그 재수정] 라벨의 위치 기준점(anchor) - 스크롤과 무관하게 고정 ⭐⭐⭐ */
.chart-outer {
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

/* 스크롤 모드에서 차트 래퍼 */
.scroll-mode .chart-wrapper {
  min-width: 100%;
}

.v-expansion-panel-text .v-slider {
  max-width: calc(100% - 60px);
}

.v-expansion-panel-text .v-slider .v-slider-track {
  width: 100%;
}
</style>
