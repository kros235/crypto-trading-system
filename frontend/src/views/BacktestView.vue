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
              백테스팅
            </h1>
            <p class="text-subtitle-1 text-grey">과거 데이터로 거래 전략의 수익률을 시뮬레이션하세요</p>
          </v-col>
        </v-row>

        <!-- 설정 폼 -->
        <v-row>
          <v-col cols="12" md="4">
            <v-card>
              <v-card-title class="d-flex align-center">
                <v-icon class="mr-2">mdi-cog</v-icon>
                백테스트 설정
                <!-- 도움말 버튼 -->
                <HelpButton 
                  use-dialog 
                  :dialog-title="helpContents.basicSettings.title"
                  :dialog-content="helpContents.basicSettings.content"
                  color="grey-darken-1"
                />
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
                        <!-- 이동평균선 도움말  -->
                        <div class="d-flex align-center mb-1">
                          <span class="text-caption text-grey">이동평균선 기간</span>
                          <HelpButton 
                            :tooltip="'이동평균선 기간 설정'"
                            use-dialog
                            :dialog-title="helpContents.maPeriod.title"
                            :dialog-content="helpContents.maPeriod.content"
                            size="x-small"
                            color="grey"
                          />
                        </div>
                        <v-slider
                          v-model="request.basePeriod"
                          :min="7"
                          :max="30"
                          :step="1"
                          thumb-label
                          hide-details
                          class="mt-2"
                        >
                          <template v-slot:append>
                            <span class="text-body-2">{{ request.basePeriod }}일</span>
                          </template>
                        </v-slider>

                        <!-- 매수 기준 도움말 -->
                        <div class="d-flex align-center mb-1">
                          <span class="text-caption text-grey">매수 기준 (MA 대비 %)</span>
                          <HelpButton 
                            use-dialog
                            :dialog-title="helpContents.buyThreshold.title"
                            :dialog-content="helpContents.buyThreshold.content"
                            size="x-small"
                            color="grey"
                          />
                        </div>
                        <v-slider
                          v-model="request.buyThresholdPct"
                          :min="-20"
                          :max="0"
                          :step="0.5"
                          thumb-label
                          hide-details
                        >
                          <template v-slot:append>
                            <span class="text-body-2">{{ request.buyThresholdPct }}%</span>
                          </template>
                        </v-slider>

                        <div class="d-flex align-center mb-1">
                          <span class="text-caption text-grey">목표 수익률 (%)</span>
                          <HelpButton 
                            use-dialog
                            :dialog-title="helpContents.sellTarget.title"
                            :dialog-content="helpContents.sellTarget.content"
                            size="x-small"
                            color="grey"
                          />
                        </div>
                        <v-slider
                          v-model="request.sellTargetPct"
                          :min="0.5"
                          :max="20"
                          :step="0.5"
                          thumb-label
                          hide-details
                        >
                          <template v-slot:append>
                            <span class="text-body-2">{{ request.sellTargetPct }}%</span>
                          </template>
                        </v-slider>

                        <div class="d-flex align-center mb-1">
                          <span class="text-caption text-grey">손절매 기준 (%)</span>
                          <HelpButton 
                            use-dialog
                            :dialog-title="helpContents.stopLoss.title"
                            :dialog-content="helpContents.stopLoss.content"
                            size="x-small"
                            color="grey"
                          />
                        </div>
                        <v-slider
                          v-model="request.stopLossPct"
                          :min="-30"
                          :max="0"
                          :step="0.5"
                          thumb-label
                          hide-details
                        >
                          <template v-slot:append>
                            <span class="text-body-2">{{ request.stopLossPct }}%</span>
                          </template>
                        </v-slider>

                        <div class="d-flex align-center mb-1">
                          <span class="text-caption text-grey">종목당 최대 보유</span>
                          <HelpButton 
                            use-dialog
                            :dialog-title="helpContents.maxHoldings.title"
                            :dialog-content="helpContents.maxHoldings.content"
                            size="x-small"
                            color="grey"
                          />
                        </div>
                        <v-slider
                          v-model="request.maxHoldingsPerCoin"
                          :min="1"
                          :max="10"
                          :step="1"
                          thumb-label
                          hide-details
                        >
                          <template v-slot:append>
                            <span class="text-body-2">{{ request.maxHoldingsPerCoin }}건</span>
                          </template>
                        </v-slider>

                        <!-- ⭐⭐⭐ 수정: 1회 매수 비율(%) → 1회 고정 매수 금액(원) ⭐⭐⭐ -->
                        <div class="d-flex align-center mb-1 mt-4">
                          <span class="text-caption text-grey">1회 매수 금액 (원)</span>
                          <HelpButton 
                            use-dialog
                            :dialog-title="helpContents.fixedBuyAmount.title"
                            :dialog-content="helpContents.fixedBuyAmount.content"
                            size="x-small"
                            color="grey"
                          />
                        </div>
                        <v-text-field
                          v-model.number="request.fixedBuyAmount"
                          type="number"
                          variant="outlined"
                          density="compact"
                          suffix="원"
                          :rules="[
                            (v: number) => v >= 5000 || '최소 5,000원',
                            (v: number) => v <= 10000000 || '최대 1,000만원'
                          ]"
                          hide-details="auto"
                          class="mb-2"
                        />
                        <div class="text-caption text-grey-darken-1">
                          라운드로빈 OFF 시 각 코인에 {{ formatCurrency(request.fixedBuyAmount) }} 매수
                        </div>                

                        <div class="d-flex align-center">
                          <v-switch
                            v-model="request.useTrailingStop"
                            label="트레일링 스톱 사용"
                            color="primary"
                            hide-details
                          />
                          <HelpButton 
                            use-dialog
                            :dialog-title="helpContents.trailingStop.title"
                            :dialog-content="helpContents.trailingStop.content"
                            size="x-small"
                            color="grey"
                          />
                        </div>

                        <div v-if="request.useTrailingStop" class="d-flex align-center mb-1 mt-2">
                          <span class="text-caption text-grey">트레일링 스톱 (%)</span>
                        </div>
                        <v-slider
                          v-if="request.useTrailingStop"
                          v-model="request.trailingStopPct"
                          :min="1"
                          :max="10"
                          :step="0.5"
                          thumb-label
                          hide-details
                        >
                          <template v-slot:append>
                            <span class="text-body-2">{{ request.trailingStopPct }}%</span>
                          </template>
                        </v-slider>

                        <v-divider class="my-4" />

                        <div class="text-subtitle-2 mb-3 d-flex align-center">
                          <v-icon size="small" class="mr-1">mdi-chart-bell-curve-cumulative</v-icon>
                          기술적 지표 설정
                        </div>

                        <!-- RSI 설정 -->
                        <div class="text-caption text-grey mb-2 d-flex align-center">
                          RSI (상대강도지수)
                          <HelpButton 
                            use-dialog
                            :dialog-title="helpContents.rsiSettings.title"
                            :dialog-content="helpContents.rsiSettings.content"
                            size="x-small"
                            color="grey"
                          />
                        </div>
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
                        <div class="text-caption text-grey mb-2 mt-4 d-flex align-center">
                          볼린저 밴드
                          <HelpButton 
                            use-dialog
                            :dialog-title="helpContents.bbSettings.title"
                            :dialog-content="helpContents.bbSettings.content"
                            size="x-small"
                            color="grey"
                          />
                        </div>
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
                        <div class="text-caption text-grey mb-2 mt-4 d-flex align-center">
                          거래량 급증 기준
                          <HelpButton 
                            use-dialog
                            :dialog-title="helpContents.volumeThreshold.title"
                            :dialog-content="helpContents.volumeThreshold.content"
                            size="x-small"
                            color="grey"
                          />
                        </div>
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
                        
                        <!-- 리스크 관리 설정 -->
                        <div class="text-subtitle-2 mb-3">
                          <v-icon size="small" class="mr-1">mdi-shield-check</v-icon>
                          리스크 관리
                        </div>
                        
                        <!-- 일일 거래 한도 -->
                        <div class="text-caption text-grey mb-2 d-flex align-center">
                          일일 최대 거래금액
                          <HelpButton 
                            use-dialog
                            :dialog-title="helpContents.dailyTradeLimit.title"
                            :dialog-content="helpContents.dailyTradeLimit.content"
                            size="x-small"
                            color="grey"
                          />
                        </div>
                        <v-slider
                          v-model="request.dailyTradeLimitPct"
                          :min="10"
                          :max="100"
                          :step="10"
                          thumb-label
                          hide-details
                        >
                          <template v-slot:append>
                            <span class="text-body-2">
                              {{ request.dailyTradeLimitPct === 100 ? '제한없음' : `${request.dailyTradeLimitPct}%` }}
                            </span>
                          </template>
                        </v-slider>
                        <div class="text-caption text-grey-darken-1 mb-3">
                          초기 자본 대비 하루 최대 매수 금액 ({{ formatCurrency(request.initialBalance * request.dailyTradeLimitPct / 100) }})
                        </div>
                        
                        <!-- ⭐⭐⭐ Day 41 추가: 일일 한도 복구 옵션 ⭐⭐⭐ -->
                        <div class="d-flex align-center mb-1">
                          <v-switch
                            v-model="request.useDailyLimitRecovery"
                            label="매도 시 일일 한도 복구"
                            color="primary"
                            hide-details
                            density="compact"
                          />
                          <HelpButton 
                            use-dialog
                            :dialog-title="helpContents.dailyLimitRecovery.title"
                            :dialog-content="helpContents.dailyLimitRecovery.content"
                            size="x-small"
                            color="grey"
                          />
                        </div>
                        <div class="text-caption text-grey-darken-1 mb-3">
                          ON: 매도 금액만큼 일일 매수 한도가 복구됩니다<br/>(최대 일일 한도까지)
                        </div>
                        
                        <!-- ⭐⭐⭐ 수정: usePerTradeLimit → useRoundRobin (매수 방식 선택) ⭐⭐⭐ -->
                        <div class="d-flex align-center mb-1">
                          <v-switch
                            v-model="request.useRoundRobin"
                            :label="request.useRoundRobin ? '🔄 라운드로빈' : '💵 고정 금액'"
                            color="primary"
                            hide-details
                            density="compact"
                          />
                          <HelpButton 
                            use-dialog
                            :dialog-title="helpContents.useRoundRobin.title"
                            :dialog-content="helpContents.useRoundRobin.content"
                            size="x-small"
                            color="grey"
                          />
                        </div>
                        <div class="text-caption text-grey-darken-1 mb-3">
                          {{ request.useRoundRobin 
                              ? '일일 한도를 매수 신호 수로 균등 분배' 
                              : `각 코인에 ${formatCurrency(request.fixedBuyAmount)} 매수` }}
                        </div>
                        
                        <!-- 단일 종목 비중 제한 -->
                       <div class="text-caption text-grey mb-2 mt-3 d-flex align-center">
                          단일 종목 최대 비중
                          <HelpButton 
                            use-dialog
                            :dialog-title="helpContents.maxPosition.title"
                            :dialog-content="helpContents.maxPosition.content"
                            size="x-small"
                            color="grey"
                          />
                        </div>
                        <v-slider
                          v-model="request.maxPositionPct"
                          :min="10"
                          :max="100"
                          :step="5"
                          thumb-label
                          hide-details
                        >
                          <template v-slot:append>
                            <span class="text-body-2">
                              {{ request.maxPositionPct === 100 ? '제한없음' : `${request.maxPositionPct}%` }}
                            </span>
                          </template>
                        </v-slider>
                        <div class="text-caption text-grey-darken-1 mb-3">
                          한 코인에 최대 투자 가능 금액 ({{ formatCurrency(request.initialBalance * request.maxPositionPct / 100) }})
                        </div>
                        
                        <!-- 긴급 정지 조건 -->
                        <div class="text-caption text-grey mb-2 mt-3 d-flex align-center">
                          긴급 정지 (일일 손실률)
                          <HelpButton 
                            use-dialog
                            :dialog-title="helpContents.dailyStopLoss.title"
                            :dialog-content="helpContents.dailyStopLoss.content"
                            size="x-small"
                            color="grey"
                          />
                        </div>
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
                            <span class="text-body-2">
                              {{ request.dailyStopLossPct <= -50 ? '사용안함' : `${request.dailyStopLossPct}%` }}
                            </span>
                          </template>
                        </v-slider>
                        <div class="text-caption text-grey-darken-1 mb-1">
                          당일 손실이 {{ request.dailyStopLossPct }}% 도달 시 거래 중단
                        </div>

                        <v-divider class="my-4"></v-divider>
                        <div class="text-subtitle-2 font-weight-medium mb-3 d-flex align-center">
                          <v-icon size="small" class="mr-1">mdi-shield-alert</v-icon>
                          급락장 보호 기능
                          <HelpButton 
                            use-dialog
                            :dialog-title="helpContents.crashProtection.title"
                            :dialog-content="helpContents.crashProtection.content"
                            size="x-small"
                            color="grey"
                          />
                        </div>
                        
                        <!-- ★★★ 수정: 시장 추세 필터 - 개별 도움말 버튼 추가 ★★★ -->
                        <div class="d-flex align-center mb-1">
                          <v-switch
                            v-model="request.useMarketTrendFilter"
                            label="시장 추세 필터 (BTC MA20)"
                            hint="BTC가 20일선 하회 시 전체 매수 중단"
                            persistent-hint
                            density="compact"
                            color="primary"
                            hide-details
                          ></v-switch>
                          <HelpButton 
                            use-dialog
                            :dialog-title="helpContents.marketTrendFilter.title"
                            :dialog-content="helpContents.marketTrendFilter.content"
                            size="x-small"
                            color="grey"
                          />
                        </div>
                        <div class="text-caption text-grey-darken-1 mb-3 ml-10">
                          BTC가 20일선 하회 시 전체 매수 중단
                        </div>
                        
                        <!-- ★★★ 수정: 누적 손실 한도 - 개별 도움말 버튼 추가 ★★★ -->
                        <div class="d-flex align-center mb-1">
                          <span class="text-caption text-grey">누적 손실 한도 (%)</span>
                          <HelpButton 
                            use-dialog
                            :dialog-title="helpContents.cumulativeLossLimit.title"
                            :dialog-content="helpContents.cumulativeLossLimit.content"
                            size="x-small"
                            color="grey"
                          />
                        </div>
                        <v-slider
                          v-model="request.cumulativeLossLimitPct"
                          :min="-50"
                          :max="0"
                          :step="5"
                          thumb-label
                          hide-details
                          color="warning"
                          class="mb-3"
                        >
                          <template v-slot:append>
                            <span class="text-body-2">{{ request.cumulativeLossLimitPct }}%</span>
                          </template>
                        </v-slider>
                        
                        <!-- ★★★ 수정: 연속 손절 제한 - 개별 도움말 버튼 추가 ★★★ -->
                        <div class="d-flex align-center mb-1">
                          <span class="text-caption text-grey">연속 손절 제한 (회)</span>
                          <HelpButton 
                            use-dialog
                            :dialog-title="helpContents.consecutiveStopLossLimit.title"
                            :dialog-content="helpContents.consecutiveStopLossLimit.content"
                            size="x-small"
                            color="grey"
                          />
                        </div>
                        <v-slider
                          v-model="request.consecutiveStopLossLimit"
                          :min="1"
                          :max="10"
                          :step="1"
                          thumb-label
                          hide-details
                          color="info"
                        >
                          <template v-slot:append>
                            <span class="text-body-2">{{ request.consecutiveStopLossLimit }}회</span>
                          </template>
                        </v-slider>

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
import HelpButton from '@/components/HelpButton.vue'

// 사이드바 ref
const sidebarRef = ref()

// 도움말 콘텐츠
const helpContents = {
  basicSettings: {
    title: '⚙️ 백테스트 기본 설정',
    content: `
      <p class="help-intro">과거 데이터로 거래 전략을 시뮬레이션하기 위한 기본 설정입니다.</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>거래 코인</strong><br/>
        <span class="help-desc">백테스트할 코인을 선택합니다. 여러 개를 선택하면 분산 투자 효과를 확인할 수 있습니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>테스트 기간</strong><br/>
        <span class="help-desc">시작일~종료일. 최대 3년까지 가능합니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>초기 투자금</strong><br/>
        <span class="help-desc">시뮬레이션 시작 시점의 가상 자본금입니다.</span></p>
      <p class="help-note">💡 <strong>Tip:</strong> 먼저 기본값으로 테스트한 후 고급 설정을 조정해보세요.</p>
    `
  },

  // ★★★ MA (이동평균선) - 도움말 페이지에서 이식 ★★★
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
        
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-color: #37474F; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">🛒 마트 예시</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8; color: #CFD8DC;">
            사과 가격이 매일 바뀐다고 생각해보세요.<br/>
            - 어제: 1,000원<br/>
            - 그제: 1,200원<br/>
            - 3일 전: 800원<br/>
            → <strong style="color: #4CAF50;">3일 이동평균: (1000+1200+800) ÷ 3 = 1,000원</strong><br/><br/>
            <strong style="color: #4CAF50;">20일 이동평균 = 최근 20일간 평균 가격</strong><br/><br/>
            📊 <strong style="color: #4CAF50;">코인 예시:</strong><br/>
            비트코인 20일 이동평균이 1억원인데<br/>
            오늘 가격이 9,400만원이면?<br/>
            → "평균보다 6% 싸네! 살까?"
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
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">설정값</th>
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">의미</th>
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">적합한 사람</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">7~10일</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">단기 추세</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">자주 거래하고 싶은 사람</td>
                </tr>
                <tr style="background-color: #E3F2FD;">
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;"><strong>20일</strong></td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;">중기 추세</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;">초보자 추천 ✅</td>
                </tr>
                <tr>
                  <td style="padding: 8px 12px;">30일</td>
                  <td style="padding: 8px 12px;">장기 추세</td>
                  <td style="padding: 8px 12px;">느긋한 투자자</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `
  },

  // ★★★ 이격도 (매수 기준) - 도움말 페이지에서 이식 ★★★
  buyThreshold: {
    title: '🏷️ 이격도 (매수 기준)',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"평균 가격보다 얼마나 떨어지면 살 것인가?"</p>
          </div>
        </div>
        
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-color: #37474F; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">🏷️ 마트 할인 예시</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8; color: #CFD8DC;">
            평소 10,000원 하는 운동화가 있어요.<br/>
            - -3% 설정: 9,700원 되면 구매<br/>
            - -6% 설정: 9,400원 되면 구매<br/>
            - -10% 설정: 9,000원 되면 구매<br/><br/>
            숫자가 클수록(음수가 클수록)<br/>
            → 더 많이 떨어져야 삼<br/>
            → 거래 횟수 적어짐<br/>
            → 더 신중한 투자<br/><br/>
            📊 <strong style="color: #4CAF50;">코인 예시:</strong><br/>
            비트코인 20일 평균: 1억원<br/>
            - -6% 설정 시: 9,400만원 이하로 떨어지면 매수!
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
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">설정값</th>
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">의미</th>
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">거래 빈도</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">-3%</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">조금만 떨어져도 삼</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">많음 (공격적)</td>
                </tr>
                <tr style="background-color: #E3F2FD;">
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;"><strong>-6%</strong></td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;">적당히 떨어지면 삼</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;">보통 ✅</td>
                </tr>
                <tr>
                  <td style="padding: 8px 12px;">-10%</td>
                  <td style="padding: 8px 12px;">많이 떨어져야 삼</td>
                  <td style="padding: 8px 12px;">적음 (신중)</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `
  },

  // ★★★ 목표 수익률 - 도움말 페이지에서 이식 ★★★
  sellTarget: {
    title: '🤝 목표 수익률',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"얼마 오르면 팔 것인가?"</p>
          </div>
        </div>
        
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-color: #37474F; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">🤝 중고거래 예시</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8; color: #CFD8DC;">
            10,000원에 산 물건을<br/>
            - 3% 설정: 10,300원에 판매<br/>
            - 4% 설정: 10,400원에 판매<br/>
            - 10% 설정: 11,000원에 판매<br/><br/>
            <strong style="color: #4CAF50;">낮게 설정하면:</strong><br/>
            ✅ 자주 수익 실현<br/>
            ❌ 큰 상승 놓칠 수 있음<br/><br/>
            <strong style="color: #4CAF50;">높게 설정하면:</strong><br/>
            ✅ 큰 수익 가능<br/>
            ❌ 목표 도달 못하고 하락할 수 있음<br/><br/>
            📊 <strong style="color: #4CAF50;">코인 예시:</strong><br/>
            9,400만원에 비트코인 샀다면<br/>
            - 4% 설정 시: 9,776만원 되면 자동 매도!
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
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">설정값</th>
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">특징</th>
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">적합한 상황</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">2~3%</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">빠른 수익 실현</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">횡보장, 하락장</td>
                </tr>
                <tr style="background-color: #E3F2FD;">
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;"><strong>4~5%</strong></td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;">균형잡힌 목표</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;">초보자 추천 ✅</td>
                </tr>
                <tr>
                  <td style="padding: 8px 12px;">10%+</td>
                  <td style="padding: 8px 12px;">큰 수익 노림</td>
                  <td style="padding: 8px 12px;">상승장</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="mb-4" style="padding: 16px 0;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">💡 백테스팅 수수료 반영</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8;">
            <strong style="color: #4CAF50;">✅ 백테스팅 결과에도 거래 수수료(0.05%)가 반영됩니다!</strong><br/><br/>
            <strong>적용 방식:</strong><br/>
            - 목표 수익률 도달 판단: 수수료 차감 후 실제 수익률 기준<br/>
            - 손절매/트레일링 스톱: 가격 변동률 기준 (수수료 미반영)<br/><br/>
            <strong>결과 메시지 예시:</strong><br/>
            - "목표 수익률 도달: 4.41% <span style="color: #FF9800;">(수수료 반영)</span>"<br/>
            - "손절매: -10.16%" (가격 기준)
          </div>
        </div>
      </div>
    `
  },

  // ★★★ 손절매 (Stop Loss) - 도움말 페이지에서 이식 ★★★
  stopLoss: {
    title: '🎰 손절매 (Stop Loss)',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"얼마나 손해보면 포기하고 팔 것인가?"</p>
          </div>
        </div>
        
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-color: #37474F; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">🎰 도박 예시</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8; color: #CFD8DC;">
            카지노에서 10만원 들고 갔는데<br/>
            - -5% 설정: 9.5만원 되면 "그만!"<br/>
            - -8% 설정: 9.2만원 되면 "그만!"<br/>
            - -15% 설정: 8.5만원 되면 "그만!"<br/><br/>
            <strong style="color: #4CAF50;">⚠️ 왜 필요한가?</strong><br/>
            손절매 없이 버티면...<br/>
            10만원 → 5만원 → 2만원 → 0원 😭<br/><br/>
            손절매 있으면...<br/>
            10만원 → 9.2만원 → "여기서 멈춤!"<br/>
            → 남은 돈으로 다시 도전 가능<br/><br/>
            📊 <strong style="color: #4CAF50;">코인 예시:</strong><br/>
            9,400만원에 비트코인 샀는데<br/>
            - -8% 설정 시: 8,648만원 되면 자동 손절!<br/>
            - 752,000원 손해로 제한 (전액 손실 방지)
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
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">설정값</th>
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">특징</th>
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">멘탈 요구도</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">-5%</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">빠른 손절</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">약함 (안전)</td>
                </tr>
                <tr style="background-color: #E3F2FD;">
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;"><strong>-8%</strong></td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;">적당한 손절</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;">보통 ✅</td>
                </tr>
                <tr>
                  <td style="padding: 8px 12px;">-15%</td>
                  <td style="padding: 8px 12px;">느린 손절</td>
                  <td style="padding: 8px 12px;">강함 (위험)</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `
  },

  // ⭐⭐⭐ 수정: buyAmountPct → fixedBuyAmount ⭐⭐⭐
  fixedBuyAmount: {
    title: '1회 매수 금액',
    content: `
      <div style="font-size: 14px; line-height: 1.6;">
        <p><strong>1회 매수 금액</strong>은 '고정 금액' 매수 방식에서 각 코인에 매수할 금액입니다.</p>
        
        <p style="margin-top: 8px;"><strong>📌 적용 조건:</strong></p>
        <ul style="margin-left: 16px;">
          <li>매수 방식이 '고정 금액'일 때만 사용</li>
          <li>'라운드로빈' 방식에서는 자동 균등 분배</li>
        </ul>
        
        <p style="margin-top: 8px;"><strong>⚠️ 제한사항:</strong></p>
        <ul style="margin-left: 16px;">
          <li>최소: 5,000원 (업비트 최소 주문금액)</li>
          <li>최대: 1,000만원</li>
        </ul>
        
        <p style="margin-top: 8px;"><strong>💡 백테스팅 예시:</strong></p>
        <span style="background: #f5f5f5; padding: 2px 6px; border-radius: 4px;">
          10,000원 설정 + 3개 코인 매수 신호 → 총 30,000원 매수
        </span>
      </div>
    `
  },

  // ★★★ 종목당 최대 보유 - 도움말 페이지에서 이식 ★★★
  maxHoldings: {
    title: '🛍️ 종목당 최대 보유',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"한 코인을 최대 몇 번까지 나눠서 살 것인가?"</p>
          </div>
        </div>
        
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-color: #37474F; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">🛍️ 쇼핑 예시</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8; color: #CFD8DC;">
            맘에 드는 가방이 있는데 가격이 계속 떨어져요.<br/>
            - 1회 설정: 한 번만 사고 끝<br/>
            - 2회 설정: 더 떨어지면 한 번 더 삼 (2번까지)<br/>
            - 3회 설정: 최대 3번까지 나눠서 삼<br/><br/>
            <strong style="color: #4CAF50;">장점:</strong> 물타기로 평균 단가 낮출 수 있음<br/>
            <strong style="color: #F44336;">단점:</strong> 계속 떨어지면 손실 커짐<br/><br/>
            📊 <strong style="color: #4CAF50;">코인 예시:</strong><br/>
            비트코인을 2번까지 나눠 사기 설정<br/>
            1차 매수: 9,400만원에 50만원어치<br/>
            2차 매수: 9,000만원에 50만원어치 (추가 하락 시)<br/>
            → 평균 단가: 9,200만원
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
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">설정값</th>
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">의미</th>
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">리스크</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">1</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">한 번만 삼</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">낮음 (분산)</td>
                </tr>
                <tr style="background-color: #E3F2FD;">
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;"><strong>2</strong></td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;">두 번까지</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;">보통 ✅</td>
                </tr>
                <tr>
                  <td style="padding: 8px 12px;">3+</td>
                  <td style="padding: 8px 12px;">여러 번</td>
                  <td style="padding: 8px 12px;">높음 (집중)</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `
  },

  // ★★★ 트레일링 스톱 - 도움말 페이지에서 이식 ★★★
  trailingStop: {
    title: '🎯 트레일링 스톱',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"최고점에서 얼마 떨어지면 팔 것인가?"</p>
          </div>
        </div>
        
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-color: #37474F; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">🎯 롤러코스터 예시</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8; color: #CFD8DC;">
            목표 수익률 4%인데, 코인이 10%까지 올랐어요!<br/>
            - 트레일링 OFF: 4%에서 이미 팔았음 (6% 놓침 😢)<br/>
            - 트레일링 ON (4%): 최고점(10%)에서 -4% 떨어진<br/>
            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;6% 수익에서 팔림!<br/><br/>
            📊 <strong style="color: #4CAF50;">실제 예시:</strong><br/>
            9,400만원에 매수 → 목표 4% = 9,776만원<br/><br/>
            <strong style="color: #4CAF50;">[트레일링 OFF]</strong><br/>
            가격: 9,400 → 9,776 → 매도! (4% 수익)<br/>
            이후: 9,900 → 1억 (놓침 😢)<br/><br/>
            <strong style="color: #4CAF50;">[트레일링 ON, 4%]</strong><br/>
            가격: 9,400 → 9,776 → 9,900 → 1억 (최고점)<br/>
            → 9,600만원 (최고점 대비 -4%) → 매도!<br/>
            최종: 약 2% 수익 (200만원)
          </div>
        </div>
        
        <div class="glossary-section mb-4" style="background-color: #FFF3E0; border-radius: 8px; padding: 12px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #E65100;">⚡ 활성화 조건</span>
          </div>
          <div style="padding-left: 24px; color: #424242;">
            트레일링 스톱은 <strong>목표 수익률의 50% 또는 최소 1%</strong> 수익 확보 후 활성화됩니다.<br/><br/>
            <table style="width: 100%; border-collapse: collapse; font-size: 13px;">
              <tr style="background-color: #FFE0B2;">
                <td style="padding: 6px 10px; border: 1px solid #FFCC80;"><strong>목표 수익률</strong></td>
                <td style="padding: 6px 10px; border: 1px solid #FFCC80;"><strong>활성화 시점</strong></td>
              </tr>
              <tr>
                <td style="padding: 6px 10px; border: 1px solid #FFCC80;">4%</td>
                <td style="padding: 6px 10px; border: 1px solid #FFCC80;">2% 수익부터 (4% × 50%)</td>
              </tr>
              <tr>
                <td style="padding: 6px 10px; border: 1px solid #FFCC80;">1%</td>
                <td style="padding: 6px 10px; border: 1px solid #FFCC80;">1% 수익부터 (최소 1%)</td>
              </tr>
              <tr>
                <td style="padding: 6px 10px; border: 1px solid #FFCC80;">10%</td>
                <td style="padding: 6px 10px; border: 1px solid #FFCC80;">5% 수익부터 (10% × 50%)</td>
              </tr>
            </table><br/>
            💡 <strong>왜 필요한가?</strong><br/>백테스팅에서도 실제 거래와 동일한 조건으로 시뮬레이션됩니다.
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
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">설정</th>
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">장점</th>
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">단점</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">OFF</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">확실한 수익 확보</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">추가 상승 놓침</td>
                </tr>
                <tr style="background-color: #E3F2FD;">
                  <td style="padding: 8px 12px; color: #1565C0;"><strong>ON, 4%</strong></td>
                  <td style="padding: 8px 12px; color: #1565C0;">상승장에서 더 벌 수 있음</td>
                  <td style="padding: 8px 12px; color: #1565C0;">하락장에선 효과 적음</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `
  },
  // ★★★ RSI (상대강도지수) - 도움말 페이지에서 이식 ★★★
  rsiSettings: {
    title: '🌡️ RSI (상대강도지수)',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"지금 너무 많이 올랐나? 많이 떨어졌나? 판단 지표"</p>
          </div>
        </div>
        
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-color: #37474F; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">🌡️ 온도계 비유</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8; color: #CFD8DC;">
            RSI = 시장의 "과열/냉각" 온도계<br/><br/>
            <div style="font-family: monospace; background: #37474F; padding: 12px; border-radius: 8px; color: #ECEFF1;">
100 ── 🔥 극도로 과열 (팔아야 할 때)<br/>
 70 ── ⚠️ 과열 (매도 신호) ← 매도 ≥ 68<br/>
 50 ── 😐 보통<br/>
 30 ── ❄️ 냉각 (매수 신호) ← 매수 ≤ 32<br/>
  0 ── 🥶 극도로 냉각 (사야 할 때)
            </div><br/>
            📊 <strong style="color: #4CAF50;">현재 설정:</strong><br/>
            - RSI ≤ 32: "충분히 떨어졌다, 매수!"<br/>
            - RSI ≥ 68: "충분히 올랐다, 매도!"
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
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">설정</th>
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">기간 14일</th>
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">매수 ≤32</th>
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">매도 ≥68</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">의미</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">표준 설정</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">과매도 진입</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE;">과매수 전 탈출</td>
                </tr>
                <tr style="background-color: #E3F2FD;">
                  <td style="padding: 8px 12px; color: #1565C0;">추천</td>
                  <td style="padding: 8px 12px; color: #1565C0;">✅</td>
                  <td style="padding: 8px 12px; color: #1565C0;">✅</td>
                  <td style="padding: 8px 12px; color: #1565C0;">✅</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `
  },

  // ★★★ 볼린저 밴드 (BB) - 도움말 페이지에서 이식 ★★★
  bbSettings: {
    title: '🚗 볼린저 밴드 (BB)',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"가격이 정상 범위 안에 있나 밖에 있나? 판단 도구"</p>
          </div>
        </div>
        
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-color: #37474F; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">🚗 도로 비유</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8; color: #CFD8DC;">
            볼린저 밴드는 도로의 차선과 같습니다.<br/><br/>
            - 가격이 하단 밴드 터치 → "싸졌네? 매수 신호!"<br/>
            - 가격이 상단 밴드 터치 → "비싸졌네? 매도 신호!"<br/><br/>
            📊 <strong style="color: #4CAF50;">현재 설정:</strong><br/>
            - 기간 20일: 20일 평균 기준<br/>
            - 표준편차 2배: 밴드 폭 결정 (2배가 표준)
          </div>
        </div>
        
        <div class="mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">📊 시각적 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <svg width="100%" height="140" viewBox="0 0 500 140" style="background: #FAFAFA; border-radius: 8px; border: 1px solid #E0E0E0;">
              <line x1="30" y1="25" x2="450" y2="25" stroke="#e53935" stroke-width="2" stroke-dasharray="5,3"/>
              <text x="460" y="29" font-size="12" fill="#e53935">상단 밴드 (비싸다!)</text>
              <line x1="30" y1="70" x2="450" y2="70" stroke="#1976D2" stroke-width="2"/>
              <text x="460" y="74" font-size="12" fill="#1976D2">중심선 (평균 가격)</text>
              <line x1="30" y1="115" x2="450" y2="115" stroke="#43a047" stroke-width="2" stroke-dasharray="5,3"/>
              <text x="460" y="119" font-size="12" fill="#43a047">하단 밴드 (싸다!)</text>
              <polyline points="40,70 100,55 160,90 220,108 280,80 340,45 400,65 440,85" fill="none" stroke="#FF9800" stroke-width="2.5"/>
              <circle cx="220" cy="108" r="8" fill="#43a047"/>
              <text x="195" y="135" font-size="11" fill="#43a047" font-weight="bold">매수 신호!</text>
              <circle cx="340" cy="45" r="8" fill="#e53935"/>
              <text x="315" y="35" font-size="11" fill="#e53935" font-weight="bold">매도 신호!</text>
              <circle cx="160" cy="90" r="4" fill="#FF9800"/>
              <text x="135" y="75" font-size="10" fill="#FF9800">🚗</text>
            </svg>
          </div>
        </div>
      </div>
    `
  },

  // ★★★ 거래량 - 도움말 페이지에서 이식 ★★★
  volumeThreshold: {
    title: '🏪 거래량 급증 기준',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"평소보다 거래가 얼마나 활발해야 진짜 신호로 볼 것인가?"</p>
          </div>
        </div>
        
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-color: #37474F; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">🏪 가게 비유</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8; color: #CFD8DC;">
            평소 하루 100명 오는 가게에<br/>
            - 오늘 140명 왔다 → "뭔가 있네! 관심 가져볼까?"<br/>
            - 오늘 200명 왔다 → "대박 터졌다!"<br/>
            - 오늘 80명 왔다 → "오늘은 조용하네..."<br/><br/>
            📊 <strong style="color: #4CAF50;">코인 예시:</strong><br/>
            비트코인 평균 거래량: 1조원/일<br/>
            - 140% 설정 시: 1.4조원 이상 거래되는 날에만 매수 신호 인정<br/>
            - 거래량 적으면 "가짜 신호"일 수 있으니 무시<br/><br/>
            <strong style="color: #4CAF50;">왜 중요한가?</strong><br/>
            거래량 없이 가격만 움직이면 → 세력의 조작일 수 있음<br/>
            거래량 터지면서 움직이면 → 진짜 시장 반응!
          </div>
        </div>
      </div>
    `
  },

  // ★★★ 일일 최대 거래금액 - 도움말 페이지에서 이식 ★★★
  dailyTradeLimit: {
    title: '💰 일일 최대 거래금액',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"하루에 최대 얼마까지만 살 것인가?"</p>
          </div>
        </div>
        
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-color: #37474F; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">💰 용돈 비유</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8; color: #CFD8DC;">
            월급 100만원 받았는데<br/>
            - 100% 설정: 하루에 100만원 다 써도 됨 (위험!)<br/>
            - 20% 설정: 하루에 20만원까지만 씀<br/><br/>
            📊 <strong style="color: #4CAF50;">현재 설정:</strong><br/>
            초기 자본 1,000,000원의 20% = 200,000원<br/>
            → 하루에 최대 20만원어치만 매수 가능<br/><br/>
            <strong style="color: #4CAF50;">왜 필요한가?</strong><br/>
            "오늘 기회다!" 하고 한 번에 다 샀는데<br/>
            다음날 더 떨어지면? 😭 살 돈이 없음!
          </div>
        </div>
      </div>
    `
  },

  // ★★★ 단일 종목 최대 비중 - 도움말 페이지에서 이식 ★★★
  maxPosition: {
    title: '🥚 단일 종목 최대 비중',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"한 코인에 최대 얼마까지 투자할 것인가?"</p>
          </div>
        </div>
        
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-color: #37474F; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">🥚 계란 비유</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8; color: #CFD8DC;">
            "계란을 한 바구니에 담지 마라"<br/><br/>
            100만원이 있을 때:<br/>
            - 100% 설정: 비트코인에 100만원 몰빵 가능<br/>
            - 25% 설정: 비트코인에 최대 25만원까지만!<br/>
            &nbsp;&nbsp;&nbsp;&nbsp;나머지는 다른 코인에 분산<br/><br/>
            📊 <strong style="color: #4CAF50;">현재 설정:</strong><br/>
            1,000,000원 × 25% = 250,000원<br/>
            → 비트코인에 최대 25만원까지만 투자 가능<br/><br/>
            <strong style="color: #4CAF50;">왜 필요한가?</strong><br/>
            비트코인에 100만원 몰빵 → -30% → 30만원 손실<br/>
            4개 코인에 25만원씩 → -30% → 7.5만원 손실
          </div>
        </div>
      </div>
    `
  },

  // ★★★ 긴급 정지 - 일일 손실률 - 도움말 페이지에서 이식 ★★★
  dailyStopLoss: {
    title: '🚨 긴급 정지 (일일 손실률)',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"오늘 손실이 이 정도면 오늘은 거래 중단!"</p>
          </div>
        </div>
        
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-color: #37474F; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">🚨 비상 브레이크 비유</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8; color: #CFD8DC;">
            자동차가 너무 빨리 가면 비상 브레이크!<br/>
            투자도 손실이 너무 커지면 "오늘은 그만!"<br/><br/>
            📊 <strong style="color: #4CAF50;">현재 설정:</strong><br/>
            1,000,000원 × -5% = -50,000원<br/>
            → 오늘 손실이 5만원 넘으면 자동으로 거래 중단<br/><br/>
            <strong style="color: #4CAF50;">왜 필요한가?</strong><br/>
            "오늘 손해 봤으니 더 사서 만회해야지!"<br/>
            → 복수 매매 → 더 큰 손실 😭<br/><br/>
            긴급 정지 있으면:<br/>
            "5만원 잃었으니 오늘은 쉬자"<br/>
            → 냉정해진 후 내일 다시 시작
          </div>
        </div>
      </div>
    `
  },

  // ★★★ 시장 추세 필터 - 분리된 도움말 (급락장 보호에서 분리) ★★★
  marketTrendFilter: {
    title: '🌧️ 시장 추세 필터 (BTC MA20)',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"비트코인이 떨어지고 있으면 아무것도 안 산다"</p>
          </div>
        </div>
        
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-color: #37474F; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">🌧️ 날씨 비유</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8; color: #CFD8DC;">
            비가 오면 소풍 안 가는 것처럼!<br/><br/>
            비트코인 = 코인 시장의 날씨<br/>
            - BTC가 20일 평균 위 = ☀️ 맑음 → 매수 OK<br/>
            - BTC가 20일 평균 아래 = 🌧️ 비 옴 → 매수 중단!<br/><br/>
            📊 <strong style="color: #4CAF50;">예시:</strong><br/>
            BTC 20일 평균: 1억원<br/>
            현재 BTC 가격: 9,500만원 (평균 아래)<br/>
            → 시장 추세 필터 ON이면 모든 매수 중단!<br/><br/>
            <strong style="color: #4CAF50;">왜 필요한가?</strong><br/>
            비트코인이 떨어지면 대부분의 알트코인도 떨어짐<br/>
            하락장에서 매수하면 손실 확률 높음
          </div>
        </div>
      </div>
    `
  },

  // ★★★ 누적 손실 한도 - 분리된 도움말 (급락장 보호에서 분리) ★★★
  cumulativeLossLimit: {
    title: '🛑 누적 손실 한도',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"전체 손실이 이 정도면 완전히 멈춘다"</p>
          </div>
        </div>
        
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-color: #37474F; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">🛑 완전 정지 비유</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8; color: #CFD8DC;">
            일일 긴급정지가 "오늘 그만"이라면<br/>
            누적 긴급정지는 "전체 그만!"<br/><br/>
            📊 <strong style="color: #4CAF50;">현재 설정 (-10%):</strong><br/>
            초기 자본 100만원 × -10% = -10만원<br/>
            → 누적 손실이 10만원 넘으면 모든 거래 완전 중단<br/><br/>
            <strong style="color: #4CAF50;">예시:</strong><br/>
            - 1일차: -3만원<br/>
            - 2일차: -2만원<br/>
            - 3일차: -4만원<br/>
            - 누적: -9만원 → 아직 거래 가능<br/>
            - 4일차: -2만원<br/>
            - 누적: -11만원 → 🛑 완전 중단!<br/><br/>
            <strong style="color: #4CAF50;">왜 필요한가?</strong><br/>
            최악의 경우에도 원금의 90%는 지킴!
          </div>
        </div>
      </div>
    `
  },

  // ★★★ 연속 손절 제한 - 분리된 도움말 (급락장 보호에서 분리) ★★★
  consecutiveStopLossLimit: {
    title: '⏳ 연속 손절 제한',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"같은 코인에서 계속 손해보면 잠시 쉬어간다"</p>
          </div>
        </div>
        
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-color: #37474F; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">⏳ 휴식 비유</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8; color: #CFD8DC;">
            같은 실수를 3번 연속 하면 잠시 쉬어!<br/><br/>
            📊 <strong style="color: #4CAF50;">예시 (3회 설정):</strong><br/>
            비트코인 1차 매수 → 손절 😢<br/>
            비트코인 2차 매수 → 손절 😢<br/>
            비트코인 3차 매수 → 손절 😢<br/>
            → "비트코인 3연속 손절! 잠시 매수 금지!"<br/><br/>
            <strong style="color: #4CAF50;">왜 필요한가?</strong><br/>
            특정 코인이 계속 떨어지는 중인데<br/>
            계속 사면 계속 손해봄<br/>
            → 잠시 쉬면서 추세 바뀔 때까지 기다림
          </div>
        </div>
      </div>
    `
  },

  // ★★★ 급락장 보호 기능 (전체 설명용 - 기존 유지) ★★★
  crashProtection: {
    title: '🛡️ 급락장 보호 기능',
    content: `
      <div class="glossary-detail pa-3">
        <p class="text-body-2 mb-3">급락장에서 손실을 최소화하기 위한 3가지 보호 기능입니다.</p>
        
        <div class="glossary-section mb-3" style="background-color: #E3F2FD; padding: 12px; border-radius: 8px;">
          <strong>🌧️ 시장 추세 필터:</strong> BTC가 20일선 하회 시 전체 매수 중단
        </div>
        
        <div class="glossary-section mb-3" style="background-color: #FFF3E0; padding: 12px; border-radius: 8px;">
          <strong>🛑 누적 손실 한도:</strong> 초기 자본 대비 누적 손실 도달 시 거래 중단
        </div>
        
        <div class="glossary-section" style="background-color: #E8F5E9; padding: 12px; border-radius: 8px;">
          <strong>⏳ 연속 손절 제한:</strong> 동일 코인 연속 손절 시 해당 코인 매수 금지
        </div>
      </div>
    `
  },
  // ★★★ 결과 요약 - 성과 지표 설명 포함 ★★★
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
  dailyLimitRecovery: {
    title: '🔄 일일 한도 복구 옵션',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"매도하면 그 금액만큼 오늘 살 수 있는 한도가 다시 생긴다"</p>
          </div>
        </div>
        
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-color: #37474F; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">⚙️ 동작 방식</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8; color: #CFD8DC;">
            <strong style="color: #F44336;">OFF (기본):</strong> 매도해도 일일 한도 복구 안됨<br/>
            <strong style="color: #4CAF50;">ON:</strong> 매도 금액만큼 한도 복구 (최대 일일 한도까지)
          </div>
        </div>
        
        <div class="mb-4" style="background-color: #E3F2FD; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #1565C0;">📊 예시: 일일 한도 40만원</span>
          </div>
          <div style="padding-left: 12px;">
            <table style="width: 100%; border-collapse: collapse; font-size: 13px;">
              <thead>
                <tr style="background-color: #BBDEFB;">
                  <th style="padding: 8px; border: 1px solid #90CAF9; text-align: center;">단계</th>
                  <th style="padding: 8px; border: 1px solid #90CAF9; text-align: center;">OFF 상태</th>
                  <th style="padding: 8px; border: 1px solid #90CAF9; text-align: center;">ON 상태</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td style="padding: 8px; border: 1px solid #90CAF9;">1. 20만원 매수</td>
                  <td style="padding: 8px; border: 1px solid #90CAF9;">남은 한도: 20만원</td>
                  <td style="padding: 8px; border: 1px solid #90CAF9;">남은 한도: 20만원</td>
                </tr>
                <tr>
                  <td style="padding: 8px; border: 1px solid #90CAF9;">2. 20만원 매도</td>
                  <td style="padding: 8px; border: 1px solid #90CAF9; color: #F44336;">남은 한도: 20만원 ❌</td>
                  <td style="padding: 8px; border: 1px solid #90CAF9; color: #4CAF50;">남은 한도: 40만원 ✅</td>
                </tr>
                <tr>
                  <td style="padding: 8px; border: 1px solid #90CAF9;">3. 20만원 매수</td>
                  <td style="padding: 8px; border: 1px solid #90CAF9; color: #F44336;">남은 한도: 0원</td>
                  <td style="padding: 8px; border: 1px solid #90CAF9; color: #4CAF50;">남은 한도: 20만원</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div style="background-color: #FFF3E0; border-left: 4px solid #FF9800; padding: 12px; border-radius: 4px;">
          <span style="font-size: 16px;">⚠️</span>
          <span class="text-body-2" style="color: #E65100;">
            <strong>주의:</strong> 일일 한도를 초과하는 금액은 복구되지 않습니다.<br/>
            예: 남은 한도 20만원에서 50만원 매도 → 20만원만 복구
          </span>
        </div>
      </div>
    `
  },
  // ⭐⭐⭐ 수정: usePerTradeLimit → useRoundRobin ⭐⭐⭐
  useRoundRobin: {
    title: '매수 방식 선택',
    content: `
      <div style="font-size: 14px; line-height: 1.6;">
        <p><strong>매수 방식</strong>은 백테스팅에서 매수 금액을 결정하는 방법입니다.</p>
        
        <p style="margin-top: 12px;"><strong>🔄 라운드로빈 (ON)</strong></p>
        <ul style="margin-left: 16px;">
          <li>남은 일일 한도를 매수 신호 수로 <strong>균등 분배</strong></li>
          <li>여러 코인에 동시 매수 신호 시 분산 투자</li>
        </ul>
        <p style="background: #e3f2fd; padding: 8px; border-radius: 4px; margin-top: 4px;">
          예: 한도 200,000원 ÷ 4개 신호 = 각 50,000원
        </p>
        
        <p style="margin-top: 12px;"><strong>💵 고정 금액 (OFF)</strong></p>
        <ul style="margin-left: 16px;">
          <li>'1회 매수 금액'에 설정한 금액만큼 <strong>정확히 매수</strong></li>
          <li>일일 한도 내에서 순차적으로 매수</li>
        </ul>
        <p style="background: #fff3e0; padding: 8px; border-radius: 4px; margin-top: 4px;">
          예: 10,000원 설정 → BTC 10,000원, ETH 10,000원...
        </p>
      </div>
    `
  }
}

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
  // RSI 설정
  rsiPeriod: 14,
  rsiBuyThreshold: 32,
  rsiSellThreshold: 68,
  // 볼린저 밴드 설정
  bbPeriod: 20,
  bbMultiplier: 2,
  // 거래량 설정
  volumeThreshold: 140,
  // 리스크 관리 설정
  dailyTradeLimitPct: 20,
  maxPositionPct: 25,
  dailyStopLossPct: -5,
  useMarketTrendFilter: false,
  cumulativeLossLimitPct: -10,
  consecutiveStopLossLimit: 3,
  // ⭐⭐⭐ 수정: buyAmountPct → fixedBuyAmount ⭐⭐⭐
  fixedBuyAmount: 10000,       // 1회 고정 매수 금액 (원)
  useDailyLimitRecovery: false,
  // ⭐⭐⭐ 수정: usePerTradeLimit → useRoundRobin ⭐⭐⭐
  useRoundRobin: true          // 매수 방식: true=라운드로빈, false=고정금액
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

.v-expansion-panel-text .v-slider {
  max-width: calc(100% - 60px);
}

.v-expansion-panel-text .v-slider .v-slider-track {
  width: 100%;
}

</style>