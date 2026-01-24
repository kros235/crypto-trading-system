<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />
    
    <v-main class="bg-grey-lighten-3">
      <v-container fluid>
        <v-row class="mb-4">
          <v-col>
            <h1 class="text-h4">
              <v-icon class="mr-2">mdi-cog-outline</v-icon>
              거래 설정
            </h1>
            <p class="text-subtitle-1 text-grey">자동매매 전략 및 거래 조건을 설정하세요</p>
          </v-col>
        </v-row>

    <v-row>
      <v-col cols="12">
        <v-card>
          <v-card-title class="bg-primary text-white">
            <v-icon icon="mdi-cog" class="mr-2" />
            자동매매 전략 설정
          </v-card-title>

          <v-card-text class="pt-4">
            <v-alert
              v-if="message"
              :type="messageType"
              dismissible
              class="mb-4"
              @click:close="message = ''"
            >
              {{ message }}
            </v-alert>

            <v-form ref="formRef" v-model="valid">
              <!-- 거래 종목 선택 -->
              <div class="mb-6">
                <h3 class="text-h6 mb-3 d-flex align-center">
                  <v-icon icon="mdi-currency-btc" class="mr-2" />
                  거래 종목 선택
                  <!-- 도움말 버튼 -->
                  <HelpButton 
                    use-dialog 
                    :dialog-title="helpContents.coinSelect.title"
                    :dialog-content="helpContents.coinSelect.content"
                    color="grey-darken-1"
                  />
                </h3>
                <v-autocomplete
                  v-model="settings.coinSymbols"
                  :items="availableCoins"
                  item-title="displayName"
                  item-value="symbol"
                  label="거래할 코인을 선택하세요"
                  multiple
                  chips
                  closable-chips
                  :rules="[rules.required]"
                  variant="outlined"
                  :loading="coinsLoading"
                >
                  <template v-slot:chip="{ props, item }">
                    <v-chip
                      v-bind="props"
                      :prepend-icon="'mdi-currency-' + getCoinIcon(item.raw.symbol)"
                      label
                      class="ma-1"
                    >
                      {{ item.raw.nameKr }} ({{ item.raw.symbol }})
                    </v-chip>
                  </template>
                </v-autocomplete>
                <div class="text-caption text-grey mt-1">
                  * 최소 1개 이상의 코인을 선택해주세요
                </div>
              </div>

              <v-divider class="my-6" />

              <!-- 기술적 지표 설정 -->
              <div class="mb-6">
                <h3 class="text-h6 mb-3 d-flex align-center">
                  <v-icon icon="mdi-chart-line" class="mr-2" />
                  기술적 지표 설정
                  <!-- 도움말 버튼 -->
                  <HelpButton 
                    use-dialog 
                    :dialog-title="helpContents.indicator.title"
                    :dialog-content="helpContents.indicator.content"
                    color="grey-darken-1"
                  />
                </h3>
                <v-radio-group
                  v-model="settings.basePeriod"
                  inline
                  :rules="[rules.required]"
                >
                  <v-radio
                    label="7일 이동평균"
                    :value="7"
                    color="primary"
                  />
                  <v-radio
                    label="14일 이동평균"
                    :value="14"
                    color="primary"
                  />
                  <v-radio
                    label="20일 이동평균"
                    :value="20"
                    color="primary"
                  />
                  <v-radio
                    label="30일 이동평균"
                    :value="30"
                    color="primary"
                  />
                </v-radio-group>
                <div class="text-caption text-grey">
                  * 기준가 산정을 위한 이동평균선 기간을 선택하세요
                </div>
              </div>

              <v-divider class="my-6" />

              <!-- 매수 조건 설정 -->
              <div class="mb-6">
                <h3 class="text-h6 mb-3 d-flex align-center">
                  <v-icon icon="mdi-currency-usd" class="mr-2" />
                  매수 조건 설정
                  <!-- ★★★ [추가] 도움말 버튼 ★★★ -->
                  <HelpButton 
                    use-dialog 
                    :dialog-title="helpContents.buyCondition.title"
                    :dialog-content="helpContents.buyCondition.content"
                    color="grey-darken-1"
                  />
                </h3>

                <v-row>
                  <v-col cols="12" md="4">
                    <v-text-field
                      v-model.number="settings.buyThresholdPct"
                      label="기준가 대비 하락률 (%)"
                      type="number"
                      :rules="[rules.required, rules.negative]"
                      variant="outlined"
                      suffix="%"
                      hint="기준가 대비 이 값 이하로 하락 시 매수 (음수 입력, 예: -5)"
                      persistent-hint
                      step="0.1"
                      min="-20"
                      max="0"
                    />
                  </v-col>

                  <v-col cols="12" md="4">
                    <v-text-field
                      v-model.number="settings.maxHoldingsPerCoin"
                      label="종목당 최대 보유 건수"
                      type="number"
                      :rules="[rules.required, rules.positive]"
                      variant="outlined"
                      suffix="건"
                      hint="한 종목당 최대 보유 가능한 매수 건수"
                    />
                  </v-col>

                  <v-col cols="12" md="4">
                    <v-text-field
                      v-model.number="settings.dailyLimitAmount"
                      label="일일 거래 한도 (기준 금액)"
                      type="number"
                      :rules="[rules.required, rules.positive]"
                      variant="outlined"
                      suffix="원"
                      hint="리스크 관리에서 비율 계산의 기준이 됩니다"
                      persistent-hint
                    />
                  </v-col>
                </v-row>
              </div>

              <v-divider class="my-6" />

              <!-- 매도 조건 설정 -->
              <div class="mb-6">
                <h3 class="text-h6 mb-3 d-flex align-center">
                  <v-icon icon="mdi-cash-register" class="mr-2" />
                  매도 조건 설정
                  <!-- ★★★ [추가] 도움말 버튼 ★★★ -->
                  <HelpButton 
                    use-dialog 
                    :dialog-title="helpContents.sellCondition.title"
                    :dialog-content="helpContents.sellCondition.content"
                    color="grey-darken-1"
                  />
                </h3>

                <v-row>
                  <v-col cols="12" md="4">
                    <v-text-field
                      v-model.number="settings.sellTargetPct"
                      label="목표 수익률 (%)"
                      type="number"
                      :rules="[rules.required, rules.positive]"
                      variant="outlined"
                      suffix="%"
                      hint="매수가 대비 이 값 이상 상승 시 매도"
                    />
                  </v-col>

                  <v-col cols="12" md="4">
                    <v-text-field
                      v-model.number="settings.stopLossPct"
                      label="손절매 기준 (%)"
                      type="number"
                      :rules="[rules.negative]"
                      variant="outlined"
                      suffix="%"
                      hint="매수가 대비 이 값 이하로 하락 시 매도"
                    />
                  </v-col>
                </v-row>
	  <v-row class="mt-2">
                  <v-col cols="12" md="4">
                    <div class="d-flex align-center">
                      <v-checkbox
                        v-model="settings.useTrailingStop"
                        label="트레일링 스톱 사용"
                        color="primary"
                        hide-details
                      />
                      <HelpButton 
                        use-dialog 
                        :dialog-title="helpContents.trailingStop.title"
                        :dialog-content="helpContents.trailingStop.content"
                        color="grey-darken-1"
                      />
                    </div>
                  </v-col>
                  <v-col cols="12" md="4" v-if="settings.useTrailingStop">
                    <v-text-field
                      v-model.number="settings.trailingStopPct"
                      label="트레일링 스톱 비율 (%)"
                      type="number"
                      :rules="[v => v > 0 && v <= 20 || '1~20 사이 양수 입력']"
                      variant="outlined"
                      suffix="%"
                      hint="예: 4 입력 시 최고가 대비 -4% 하락시 매도"
                      persistent-hint
                      min="1"
                      max="20"
                    />
                  </v-col>
                </v-row>
              </div>

              <v-divider class="my-6" />

              <div class="mb-6">
                <h3 class="text-h6 mb-3 d-flex align-center">
                  <v-icon icon="mdi-chart-bell-curve-cumulative" class="mr-2" />
                  기술적 지표 설정
                </h3>

                <!-- RSI 설정 -->
                <div class="d-flex align-center mb-2">
                  <v-icon size="small" class="mr-1">mdi-chart-line</v-icon>
                  <span class="text-subtitle-2 text-grey">RSI (상대강도지수)</span>
                  <HelpButton 
                    use-dialog 
                    :dialog-title="helpContents.rsiIndicator.title"
                    :dialog-content="helpContents.rsiIndicator.content"
                    color="grey-darken-1"
                  />
                </div>
                <v-row>
                  <v-col cols="12" md="4">
                    <v-text-field
                      v-model.number="settings.rsiPeriod"
                      label="RSI 기간 (일)"
                      type="number"
                      :rules="[v => v >= 5 && v <= 50 || '5~50 사이 입력']"
                      hint="기본값: 14일"
                      persistent-hint
                      density="compact"
                    />
                  </v-col>
                  <v-col cols="12" md="4">
                    <v-text-field
                      v-model.number="settings.rsiBuyThreshold"
                      label="매수 신호 (이하)"
                      type="number"
                      :rules="[v => v >= 10 && v <= 50 || '10~50 사이 입력']"
                      hint="기본값: 30 이하"
                      persistent-hint
                      density="compact"
                    />
                  </v-col>
                  <v-col cols="12" md="4">
                    <v-text-field
                      v-model.number="settings.rsiSellThreshold"
                      label="매도 신호 (이상)"
                      type="number"
                      :rules="[v => v >= 50 && v <= 90 || '50~90 사이 입력']"
                      hint="기본값: 70 이상"
                      persistent-hint
                      density="compact"
                    />
                  </v-col>
                </v-row>

                <v-divider class="my-4" />

                <!-- 볼린저 밴드 설정 -->
                <div class="d-flex align-center mb-2">
                  <v-icon size="small" class="mr-1">mdi-chart-bell-curve</v-icon>
                  <span class="text-subtitle-2 text-grey">볼린저 밴드</span>
                  <HelpButton 
                    use-dialog 
                    :dialog-title="helpContents.bbIndicator.title"
                    :dialog-content="helpContents.bbIndicator.content"
                    :dialog-width="700"
                    color="grey-darken-1"
                  />
                </div>
                <v-row>
                  <v-col cols="12" md="6">
                    <v-text-field
                      v-model.number="settings.bbPeriod"
                      label="볼린저 밴드 기간 (일)"
                      type="number"
                      :rules="[v => v >= 10 && v <= 50 || '10~50 사이 입력']"
                      hint="기본값: 20일"
                      persistent-hint
                      density="compact"
                    />
                  </v-col>
                  <v-col cols="12" md="6">
                    <v-select
                      v-model="settings.bbMultiplier"
                      :items="[1, 2, 3, 4]"
                      label="표준편차 승수"
                      hint="기본값: 2배"
                      persistent-hint
                      density="compact"
                    />
                  </v-col>
                </v-row>

                <v-divider class="my-4" />

                <!-- 거래량 설정 -->
                <div class="d-flex align-center mb-2">
                  <v-icon size="small" class="mr-1">mdi-chart-bar</v-icon>
                  <span class="text-subtitle-2 text-grey">거래량 분석</span>
                  <HelpButton 
                    use-dialog 
                    :dialog-title="helpContents.volumeIndicator.title"
                    :dialog-content="helpContents.volumeIndicator.content"
                    color="grey-darken-1"
                  />
                </div>
                <v-row>
                  <v-col cols="12">
                    <v-slider
                      v-model="settings.volumeThreshold"
                      label="거래량 급증 기준 (%)"
                      :min="100"
                      :max="500"
                      :step="10"
                      thumb-label
                      class="mt-2"
                    >
                       <template v-slot:append>
                        <v-chip size="small" color="primary" style="color: #000000 !important;">
                          {{ settings.volumeThreshold }}%
                        </v-chip>
                      </template>
                    </v-slider>
                    <p class="text-caption text-grey">
                      평균 거래량 대비 {{ settings.volumeThreshold }}% 이상일 때 거래량 급증으로 판단합니다.
                    </p>
                  </v-col>
                </v-row>
              </div>

              <v-divider class="my-6" />

              <!-- ★★★ 수정: 리스크 관리 - 기존 도움말 제거, 개별 도움말 버튼 추가 ★★★ -->
              <div class="mb-6">
                <h3 class="text-h6 mb-3 d-flex align-center">
                  <v-icon icon="mdi-shield-alert" class="mr-2" />
                  리스크 관리
                </h3>

                <!-- 일일 최대 거래금액 -->
                <div class="d-flex align-center mb-2">
                  <v-icon size="small" class="mr-1">mdi-cash-multiple</v-icon>
                  <span class="text-subtitle-2 text-grey">일일 최대 거래금액 (초기 자본 대비)</span>
                  <HelpButton 
                    use-dialog 
                    :dialog-title="helpContents.dailyTradeLimit.title"
                    :dialog-content="helpContents.dailyTradeLimit.content"
                    color="grey-darken-1"
                  />
                </div>
                <v-slider
                  v-model="settings.dailyTradeLimitPct"
                  label="일일 최대 거래금액 (%)"
                  :min="10"
                  :max="100"
                  :step="10"
                  thumb-label
                  class="mt-2"
                >
                   <template v-slot:append>
                    <v-chip size="small" color="primary" style="color: #000000 !important;">
                      {{ settings.dailyTradeLimitPct === 100 ? '제한없음' : `${settings.dailyTradeLimitPct}%` }}
                    </v-chip>
                  </template>
                </v-slider>
                <p class="text-caption text-grey">
                  일일 한도 금액 기준으로 하루 최대 매수 가능 금액을 제한합니다 ({{ formatCurrency(settings.dailyLimitAmount * settings.dailyTradeLimitPct / 100) }})
                </p>

                <v-divider class="my-4" />

                <!-- 단일 종목 최대 비중 -->
                <div class="d-flex align-center mb-2">
                  <v-icon size="small" class="mr-1">mdi-chart-pie</v-icon>
                  <span class="text-subtitle-2 text-grey">단일 종목 최대 비중 (총 자본 대비)</span>
                  <HelpButton 
                    use-dialog 
                    :dialog-title="helpContents.maxPosition.title"
                    :dialog-content="helpContents.maxPosition.content"
                    color="grey-darken-1"
                  />
                </div>
                <v-slider
                  v-model="settings.maxPositionPct"
                  label="단일 종목 최대 비중 (%)"
                  :min="10"
                  :max="100"
                  :step="5"
                  thumb-label
                  class="mt-2"
                >
                  <template v-slot:append>
                    <v-chip size="small" color="primary" style="color: #000000 !important;">
                      {{ settings.maxPositionPct === 100 ? '제한없음' : `${settings.maxPositionPct}%` }}
                    </v-chip>
                  </template>
                </v-slider>
                <p class="text-caption text-grey">
                  한 코인에 최대 투자 가능 금액을 제한합니다 ({{ formatCurrency(settings.dailyLimitAmount * settings.maxPositionPct / 100) }})
                </p>

                <v-divider class="my-4" />

                <!-- 긴급 정지 (일일 손실률) -->
                <div class="d-flex align-center mb-2">
                  <v-icon size="small" class="mr-1">mdi-alert-octagon</v-icon>
                  <span class="text-subtitle-2 text-grey">긴급 정지 (일일 손실률)</span>
                  <HelpButton 
                    use-dialog 
                    :dialog-title="helpContents.dailyStopLoss.title"
                    :dialog-content="helpContents.dailyStopLoss.content"
                    color="grey-darken-1"
                  />
                </div>
                <v-slider
                  v-model="settings.dailyStopLossPct"
                  label="긴급 정지 (%)"
                  :min="-50"
                  :max="0"
                  :step="5"
                  thumb-label
                  color="error"
                  class="mt-2"
                >
                  <template v-slot:append>
                    <v-chip size="small" color="error" style="color: #000000 !important;">
                      {{ settings.dailyStopLossPct === 0 ? '사용안함' : `${settings.dailyStopLossPct}%` }}
                    </v-chip>
                  </template>
                </v-slider>
                <p class="text-caption text-grey">
                  당일 손실이 이 값에 도달하면 거래를 자동 중단합니다
                </p>
              </div>

	<!-- 급락장 보호 기능 -->
              <div class="mb-6">
                <h3 class="text-h6 mb-3 d-flex align-center">
                  <v-icon icon="mdi-alert-octagon" class="mr-2" />
                  급락장 보호 기능
                </h3>
          
                <!-- 시장 추세 필터 -->
                <div class="d-flex align-center mb-2">
                  <v-icon size="small" class="mr-1">mdi-weather-cloudy</v-icon>
                  <span class="text-subtitle-2 text-grey">시장 추세 필터</span>
                  <HelpButton 
                    use-dialog 
                    :dialog-title="helpContents.marketTrendFilter.title"
                    :dialog-content="helpContents.marketTrendFilter.content"
                    color="grey-darken-1"
                  />
                </div>
                <v-switch
                  v-model="settings.useMarketTrendFilter"
                  label="시장 추세 필터 사용"
                  hint="BTC가 20일 이동평균선 아래로 하락하면 전체 매수를 중단합니다"
                  persistent-hint
                  color="primary"
                  class="mb-4"
                ></v-switch>

          
                <v-divider class="my-4" />

                <!-- 누적 손실 한도 -->
                <div class="d-flex align-center mb-2">
                  <v-icon size="small" class="mr-1">mdi-stop-circle</v-icon>
                  <span class="text-subtitle-2 text-grey">누적 손실 한도</span>
                  <HelpButton 
                    use-dialog 
                    :dialog-title="helpContents.cumulativeLossLimit.title"
                    :dialog-content="helpContents.cumulativeLossLimit.content"
                    color="grey-darken-1"
                  />
                </div>
                <v-slider
                  v-model="settings.cumulativeLossLimitPct"
                  label="누적 손실 한도 (%)"
                  :min="-50"
                  :max="0"
                  :step="5"
                  thumb-label
                  color="error"
                  class="mt-2"
                >
                  <template v-slot:append>
                    <v-chip size="small" color="error" style="color: #000000 !important;">
                      {{ settings.cumulativeLossLimitPct }}%
                    </v-chip>
                  </template>
                </v-slider>
                <p class="text-caption text-grey">
                  초기 자본 대비 누적 손실이 이 수치에 도달하면 모든 거래를 중단합니다 (현재: {{ settings.cumulativeLossLimitPct }}%)
                </p>

                <v-divider class="my-4" />
          
                <!-- 연속 손절 제한 -->
                <div class="d-flex align-center mb-2">
                  <v-icon size="small" class="mr-1">mdi-timer-sand</v-icon>
                  <span class="text-subtitle-2 text-grey">연속 손절 제한</span>
                  <HelpButton 
                    use-dialog 
                    :dialog-title="helpContents.consecutiveStopLossLimit.title"
                    :dialog-content="helpContents.consecutiveStopLossLimit.content"
                    color="grey-darken-1"
                  />
                </div>
                <v-slider
                  v-model="settings.consecutiveStopLossLimit"
                  label="연속 손절 제한 (회)"
                  :min="1"
                  :max="10"
                  :step="1"
                  thumb-label
                  color="warning"
                  class="mt-2"
                >
                  <template v-slot:append>
                    <v-chip size="small" color="warning" style="color: #000000 !important;">
                      {{ settings.consecutiveStopLossLimit }}회
                    </v-chip>
                  </template>
                </v-slider>
                <p class="text-caption text-grey">
                  동일 코인에서 {{ settings.consecutiveStopLossLimit }}회 연속 손절 시 해당 코인 24시간 매수를 금지합니다
                </p>
              </div>

              <v-divider class="my-6" />

              <div class="mb-6">
                <h3 class="text-h6 mb-3">
                  <v-icon icon="mdi-brain" class="mr-2" />
                  추가 옵션
                </h3>

                <div class="d-flex align-center">
                  <v-checkbox
                    v-model="settings.useAiAnalysis"
                    label="AI 뉴스 분석 사용"
                    color="primary"
                    hide-details
                  />
                  <HelpButton 
                    use-dialog 
                    :dialog-title="helpContents.aiAnalysis.title"
                    :dialog-content="helpContents.aiAnalysis.content"
                    color="grey-darken-1"
                  />
                </div>
                <div class="text-caption text-grey ml-8 mt-1">
                  Groq API (Llama 3.3 70B)를 통한 뉴스 분석으로 매수 조건 가중치 조정에 활용
                </div>
              </div>
              <v-divider class="my-6" />

              <!-- 버튼 -->
              <div class="d-flex gap-3">
                <v-btn
                  color="primary"
                  size="large"
                  :loading="loading"
                  :disabled="!valid"
                  @click="saveSettings"
                >
                  <v-icon icon="mdi-content-save" class="mr-2" />
                  저장
                </v-btn>

                <v-btn
                  color="secondary"
                  size="large"
                  variant="outlined"
                  @click="resetForm"
                >
                  <v-icon icon="mdi-refresh" class="mr-2" />
                  초기화
                </v-btn>

                <v-btn
                  v-if="hasExistingSettings"
                  color="error"
                  size="large"
                  variant="outlined"
                  @click="confirmDelete"
                >
                  <v-icon icon="mdi-delete" class="mr-2" />
                  삭제
                </v-btn>
              </div>
            </v-form>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- 삭제 확인 다이얼로그 -->
    <v-dialog v-model="deleteDialog" max-width="500">
      <v-card>
        <v-card-title class="bg-error text-white">
          <v-icon icon="mdi-alert" class="mr-2" />
          거래 설정 삭제 확인
        </v-card-title>

        <v-card-text class="pt-4">
          <p class="text-body-1">
            정말로 거래 설정을 삭제하시겠습니까?
          </p>
          <p class="text-body-2 text-grey">
            삭제하면 자동매매가 중단되며, 설정을 다시 생성해야 합니다.
          </p>
          <v-alert type="info" density="compact" class="mt-3">
            삭제 후 기본 설정값으로 자동 저장됩니다.
          </v-alert>
        </v-card-text>

        <v-card-actions>
          <v-spacer />
          <v-btn
            color="grey"
            variant="text"
            @click="deleteDialog = false"
          >
            취소
          </v-btn>
          <v-btn
            color="error"
            variant="elevated"
            :loading="deleteLoading"
            @click="deleteSettings"
          >
            삭제
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
    
    <!-- 초기화 확인 다이얼로그 ★★★ -->
    <v-dialog v-model="resetDialog" max-width="500">
      <v-card>
        <v-card-title class="bg-warning">
          <v-icon icon="mdi-refresh" class="mr-2" />
          설정 초기화 확인
        </v-card-title>

        <v-card-text class="pt-4">
          <p class="text-body-1">
            현재 입력된 설정을 기본값으로 초기화하시겠습니까?
          </p>
          <p class="text-body-2 text-grey mt-2">
            기본 코인: BTC, ETH, XRP, SOL<br>
            기본 전략: 백테스팅 최적화 설정
          </p>
          <v-alert type="info" density="compact" class="mt-3">
            초기화 후 저장 버튼을 눌러야 실제로 적용됩니다.
          </v-alert>
        </v-card-text>

        <v-card-actions>
          <v-spacer />
          <v-btn
            color="grey"
            variant="text"
            @click="resetDialog = false"
          >
            취소
          </v-btn>
          <v-btn
            color="warning"
            variant="elevated"
            @click="executeReset"
          >
            초기화
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>


      </v-container>
    </v-main>
  </v-app>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { coinApi, tradingApi } from '@/api'
import type { CoinInfo } from '@/types'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'
import HelpButton from '@/components/HelpButton.vue'

// 사이드바 Ref
const sidebarRef = ref()

// 라우트 객체
const route = useRoute()

// 폼 Ref
const formRef = ref()
const valid = ref(false)

// 로딩 상태
const loading = ref(false)
const deleteLoading = ref(false)
const coinsLoading = ref(false)

// 메시지
const message = ref('')
const messageType = ref<'success' | 'error' | 'info'>('success')

// 삭제 확인 다이얼로그
const deleteDialog = ref(false)

// 초기화 확인 다이얼로그
const resetDialog = ref(false)

// 도움말 콘텐츠 
const helpContents = {
  coinSelect: {
    title: '💰 거래 종목 선택',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"어떤 코인을 자동으로 사고 팔 것인가?"</p>
          </div>
        </div>
        
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-color: #37474F; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">📋 선택 가이드</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8; color: #CFD8DC;">
            <strong style="color: #4CAF50;">추천:</strong> 시가총액 상위 코인 3~5개 선택<br/><br/>
            <strong style="color: #4CAF50;">이유:</strong><br/>
            - 거래량이 많아 주문 체결이 빠름<br/>
            - 급격한 가격 변동 위험이 상대적으로 낮음<br/>
            - 시장 정보가 풍부해 분석이 용이함<br/><br/>
            📊 <strong style="color: #4CAF50;">기본 추천 코인:</strong><br/>
            BTC, ETH, XRP, SOL
          </div>
        </div>
      </div>
    `
  },

  // 기술적 지표 설정 (이동평균선)
  indicator: {
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

  // 매수 조건 설정
  buyCondition: {
    title: '🏷️ 매수 조건 설정',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"언제, 얼마나 살 것인가?"</p>
          </div>
        </div>
        
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-color: #37474F; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">🏷️ 마트 할인 예시</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8; color: #CFD8DC;">
            <strong style="color: #4CAF50;">기준가 대비 하락률 (이격도):</strong><br/>
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
            - -6% 설정 시: 9,400만원 이하로 떨어지면 매수!<br/><br/>
            <strong style="color: #4CAF50;">종목당 최대 보유:</strong><br/>
            맘에 드는 가방이 있는데 가격이 계속 떨어져요.<br/>
            - 1회 설정: 한 번만 사고 끝<br/>
            - 2회 설정: 더 떨어지면 한 번 더 삼 (2번까지)<br/>
            - 3회 설정: 최대 3번까지 나눠서 삼<br/><br/>
            <strong style="color: #4CAF50;">장점:</strong> 물타기로 평균 단가 낮출 수 있음<br/>
            <strong style="color: #F44336;">단점:</strong> 계속 떨어지면 손실 커짐
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
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">항목</th>
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">추천값</th>
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">설명</th>
                </tr>
              </thead>
              <tbody>
                <tr style="background-color: #E3F2FD;">
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;"><strong>하락률</strong></td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;">-6%</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;">초보자 추천 ✅</td>
                </tr>
                <tr style="background-color: #E3F2FD;">
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;"><strong>최대 보유</strong></td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;">2건</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;">적당한 분산 ✅</td>
                </tr>
                <tr>
                  <td style="padding: 8px 12px;">일일 한도</td>
                  <td style="padding: 8px 12px;">100만원</td>
                  <td style="padding: 8px 12px;">리스크 계산 기준</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `
  },

  // 매도 조건 설정 (트레일링 스톱 분리)
  sellCondition: {
    title: '💰 매도 조건 설정',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"언제 팔 것인가? - 수익 실현 vs 손실 제한"</p>
          </div>
        </div>
        
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-color: #37474F; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">🤝 중고거래 예시 (목표 수익률)</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8; color: #CFD8DC;">
            10,000원에 산 물건을<br/>
            - 3% 설정: 10,300원에 판매<br/>
            - 4% 설정: 10,400원에 판매<br/>
            - 10% 설정: 11,000원에 판매<br/><br/>
            <strong style="color: #4CAF50;">낮게 설정하면:</strong> ✅ 자주 수익 실현 / ❌ 큰 상승 놓칠 수 있음<br/>
            <strong style="color: #4CAF50;">높게 설정하면:</strong> ✅ 큰 수익 가능 / ❌ 목표 도달 못하고 하락할 수 있음
          </div>
        </div>
        
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-color: #37474F; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">🎰 도박 예시 (손절매)</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8; color: #CFD8DC;">
            카지노에서 10만원 들고 갔는데<br/>
            - -5% 설정: 9.5만원 되면 "그만!"<br/>
            - -8% 설정: 9.2만원 되면 "그만!"<br/>
            - -15% 설정: 8.5만원 되면 "그만!"<br/><br/>
            <strong style="color: #F44336;">⚠️ 왜 필요한가?</strong><br/>
            손절매 없이 버티면...<br/>
            10만원 → 5만원 → 2만원 → 0원 😭<br/><br/>
            손절매 있으면...<br/>
            10만원 → 9.2만원 → "여기서 멈춤!"<br/>
            → 남은 돈으로 다시 도전 가능
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
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">항목</th>
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">추천값</th>
                  <th style="padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #E0E0E0;">적합한 상황</th>
                </tr>
              </thead>
              <tbody>
                <tr style="background-color: #E3F2FD;">
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;"><strong>목표 수익률</strong></td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;">4~5%</td>
                  <td style="padding: 8px 12px; border-bottom: 1px solid #EEEEEE; color: #1565C0;">초보자 추천 ✅</td>
                </tr>
                <tr style="background-color: #E3F2FD;">
                  <td style="padding: 8px 12px; color: #1565C0;"><strong>손절매</strong></td>
                  <td style="padding: 8px 12px; color: #1565C0;">-8%</td>
                  <td style="padding: 8px 12px; color: #1565C0;">적당한 손절 ✅</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `
  },

  // ★★★ [신규] 트레일링 스톱 - 분리된 도움말 ★★★
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
            💡 <strong>왜 필요한가?</strong><br/>미미한 수익(0.1% 등)에서 조기 매도되는 것을 방지합니다.
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

  // ★★★ [신규] RSI - 분리된 도움말 ★★★
  rsiIndicator: {
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

  // ★★★ [신규] 볼린저 밴드 - 분리된 도움말 ★★★
  bbIndicator: {
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

  // ★★★ [신규] 거래량 분석 - 분리된 도움말 ★★★
  volumeIndicator: {
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

  // ★★★ [신규] 일일 최대 거래금액 - 분리된 도움말 ★★★
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

  // ★★★ [신규] 단일 종목 최대 비중 - 분리된 도움말 ★★★
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

  // ★★★ [신규] 긴급 정지 (일일 손실률) - 분리된 도움말 ★★★
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

  // ★★★ [신규] 누적 손실 한도 - 분리된 도움말 ★★★
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

  // ★★★ [신규] 연속 손절 제한 - 분리된 도움말 ★★★
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

  marketTrendFilter: {
    title: '🌧️ 시장 추세 필터',
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

  aiAnalysis: {
    title: '🤖 AI 분석 점수 안내',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">AI가 뉴스를 분석하여 시장에 미치는 영향을 <span style="color: #F44336;">-1.0</span> ~ <span style="color: #4CAF50;">+1.0</span> 범위의 점수로 나타냅니다.</p>
          </div>
        </div>
        
        <div class="glossary-example-card mb-4" style="background-color: #263238; border-color: #37474F; border-radius: 8px; padding: 16px;">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold" style="color: #ECEFF1;">📊 점수 해석</span>
          </div>
          <div style="padding-left: 24px; font-family: 'Noto Sans KR', sans-serif; line-height: 1.8; color: #CFD8DC;">
            <strong style="color: #4CAF50;">• +0.2 이상 (호재)</strong><br/>
            &nbsp;&nbsp;시장에 긍정적인 영향을 미칠 것으로 예상되는 뉴스입니다.<br/>
            &nbsp;&nbsp;예: +0.8, +0.5 등 → 매수 조건이 완화됩니다.<br/><br/>
            
            <strong style="color: #FF9800;">• -0.2 ~ +0.2 (중립)</strong><br/>
            &nbsp;&nbsp;시장에 큰 영향이 없을 것으로 예상됩니다.<br/>
            &nbsp;&nbsp;예: +0.1, -0.1 등 → 매수 조건에 변화가 없습니다.<br/><br/>
            
            <strong style="color: #F44336;">• -0.2 이하 (악재)</strong><br/>
            &nbsp;&nbsp;시장에 부정적인 영향을 미칠 것으로 예상되는 뉴스입니다.<br/>
            &nbsp;&nbsp;예: -0.5, -0.9 등 → 매수 조건이 강화됩니다.<br/><br/>
            
            <strong style="color: #2196F3;">• 가중치 계산</strong><br/>
            &nbsp;&nbsp;평균 점수 × 0.5 = 가중치(%)<br/>
            &nbsp;&nbsp;예: 평균 +0.8 → 가중치 +0.4% → 매수 기준가 완화
          </div>
        </div>
        
        <div style="margin-top: 16px; padding: 10px 12px; background-color: #FFF8E1; border-left: 3px solid #FFA000; border-radius: 4px;">
          <p style="margin: 0; color: #5D4037; font-size: 13px;">
            💡 <strong>Tip:</strong> AI 분석은 참고용이며, 실제 시장 상황과 다를 수 있습니다.<br/>
            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;뉴스 원문을 직접 확인하는 것을 권장합니다.
          </p>
        </div>
      </div>
    `
  }
}

// 활성 코인 목록
const availableCoins = ref<Array<CoinInfo & { displayName: string }>>([])

// 기존 설정 존재 여부
const hasExistingSettings = ref(false)

// 거래 설정 폼 데이터
const settings = ref({
  coinSymbols: [] as string[],
  basePeriod: 20,
  buyThresholdPct: -6,        
  sellTargetPct: 4,           
  stopLossPct: -8,            
  maxHoldingsPerCoin: 2,      
  dailyLimitAmount: 1000000,
  useTrailingStop: true,      
  trailingStopPct: 4,        
  useAiAnalysis: false,
  rsiPeriod: 14,
  rsiBuyThreshold: 32,        
  rsiSellThreshold: 68,       
  bbPeriod: 20,
  bbMultiplier: 2,
  volumeThreshold: 140,       
  dailyTradeLimitPct: 20,     
  maxPositionPct: 25,         
  dailyStopLossPct: -5,
  useMarketTrendFilter: false,
  cumulativeLossLimitPct: -10,
  consecutiveStopLossLimit: 3      
})

// 기본값 (초기화용)
const defaultSettings = {
  coinSymbols: ['KRW-BTC', 'KRW-ETH', 'KRW-XRP', 'KRW-SOL'],
  basePeriod: 20,
  buyThresholdPct: -6,    
  sellTargetPct: 4,           
  stopLossPct: -8,            
  maxHoldingsPerCoin: 2,  
  dailyLimitAmount: 1000000,
  useTrailingStop: true,      
  trailingStopPct: 4,
  useAiAnalysis: false,
  rsiPeriod: 14,
  rsiBuyThreshold: 32, 
  rsiSellThreshold: 68, 
  bbPeriod: 20,
  bbMultiplier: 2,
  volumeThreshold: 140,       
  dailyTradeLimitPct: 20,
  maxPositionPct: 25,
  dailyStopLossPct: -5,
  useMarketTrendFilter: false,
  cumulativeLossLimitPct: -10,
  consecutiveStopLossLimit: 3
}

// 유효성 검증 규칙
const rules = {
  required: (value: any) => {
    if (Array.isArray(value)) {
      return value.length > 0 || '최소 1개 이상 선택해주세요'
    }
    // 0도 유효한 값으로 인정
    return (value !== null && value !== undefined && value !== '') || '필수 입력 항목입니다'
  },
  positive: (value: any) => {
    const num = Number(value)
    if (isNaN(num)) return '숫자를 입력해주세요'
    return num > 0 || '0보다 큰 값을 입력해주세요'
  },
  negative: (value: any) => {
    const num = Number(value)
    if (isNaN(num)) return '숫자를 입력해주세요'
    return num <= 0 || '0 이하의 값을 입력해주세요'
  },
  negativeOrZero: (value: any) => {
    if (value === null || value === undefined || value === '') return true // 선택 필드
    const num = Number(value)
    if (isNaN(num)) return '숫자를 입력해주세요'
    return num <= 0 || '0 이하의 값을 입력해주세요'
  }
}

// 금액 포맷
const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
    maximumFractionDigits: 0
  }).format(value)
}

// 코인 아이콘 매핑
const getCoinIcon = (symbol: string): string => {
  const iconMap: Record<string, string> = {
    'KRW-BTC': 'btc',
    'KRW-ETH': 'eth',
    'KRW-XRP': 'usd',
    'KRW-SOL': 'usd',
    'KRW-ADA': 'usd',
    'KRW-AVAX': 'usd',
    'KRW-DOGE': 'usd',
    'KRW-DOT': 'usd'
  }
  return iconMap[symbol] || 'usd'
}

// 활성 코인 목록 로드
const loadActiveCoins = async () => {
  coinsLoading.value = true

  try {
    const response = await coinApi.getActiveCoins()
    availableCoins.value = response.data.map((coin: CoinInfo) => ({
      ...coin,
      displayName: `${coin.nameKr} (${coin.symbol})`
    }))
  } catch (error: any) {
    message.value = error.response?.data?.message || '코인 목록을 불러오는데 실패했습니다'
    messageType.value = 'error'
  } finally {
    coinsLoading.value = false
  }
}

// 기존 거래 설정 로드
const loadSettings = async () => {
  try {
    const response = await tradingApi.getSettings()
    const data = response.data

    if (data && data.coinSymbols && data.coinSymbols.length > 0) {
      // ★ 기존 설정이 있는 경우
      settings.value = {
        coinSymbols: data.coinSymbols,
        basePeriod: data.basePeriod || 20,
        buyThresholdPct: data.buyThresholdPct || -6,
        sellTargetPct: data.sellTargetPct || 4,
        stopLossPct: data.stopLossPct || -8,
        maxHoldingsPerCoin: data.maxHoldingsPerCoin || 2,
        dailyLimitAmount: data.dailyLimitAmount || 1000000,
        useTrailingStop: data.useTrailingStop ?? true,
        trailingStopPct: Math.abs(data.trailingStopPct) || 4,
        useAiAnalysis: data.useAiAnalysis || false,
        rsiPeriod: data.rsiPeriod || 14,
        rsiBuyThreshold: data.rsiBuyThreshold || 32,      
        rsiSellThreshold: data.rsiSellThreshold || 68,    
        bbPeriod: data.bbPeriod || 20,
        bbMultiplier: data.bbMultiplier || 2,
        volumeThreshold: data.volumeThreshold || 140,     
        dailyTradeLimitPct: data.dailyTradeLimitPct || 20,
        maxPositionPct: data.maxPositionPct || 25,
        dailyStopLossPct: data.dailyStopLossPct || -5,
        useMarketTrendFilter: data.useMarketTrendFilter ?? false,
        cumulativeLossLimitPct: data.cumulativeLossLimitPct || -10,
        consecutiveStopLossLimit: data.consecutiveStopLossLimit || 3
      }

      hasExistingSettings.value = true
      message.value = '기존 거래 설정을 불러왔습니다'
      messageType.value = 'info'
    } else {
      // ★★★ 수정: 설정이 없으면 기본값으로 자동 생성 ★★★
      await createDefaultSettings()
    }
  } catch (error: any) {
    // ★★★ 수정: 모든 에러 케이스에서 기본값 자동 생성 ★★★
    console.log('설정 로드 실패, 기본값으로 생성:', error.response?.status)
    await createDefaultSettings()
  }
}

const createDefaultSettings = async () => {
  try {
    settings.value = { ...defaultSettings }
    
    const payload = {
      coinSymbols: settings.value.coinSymbols,
      basePeriod: Number(settings.value.basePeriod),
      buyThresholdPct: Number(settings.value.buyThresholdPct),
      sellTargetPct: Number(settings.value.sellTargetPct),
      stopLossPct: Number(settings.value.stopLossPct),
      maxHoldingsPerCoin: Number(settings.value.maxHoldingsPerCoin),
      dailyLimitAmount: Number(settings.value.dailyLimitAmount),
      useTrailingStop: Boolean(settings.value.useTrailingStop),
      trailingStopPct: -Math.abs(Number(settings.value.trailingStopPct)),
      useAiAnalysis: Boolean(settings.value.useAiAnalysis),
      rsiPeriod: Number(settings.value.rsiPeriod),
      rsiBuyThreshold: Number(settings.value.rsiBuyThreshold),
      rsiSellThreshold: Number(settings.value.rsiSellThreshold),
      bbPeriod: Number(settings.value.bbPeriod),
      bbMultiplier: Number(settings.value.bbMultiplier),
      volumeThreshold: Number(settings.value.volumeThreshold),
      dailyTradeLimitPct: Number(settings.value.dailyTradeLimitPct),
      maxPositionPct: Number(settings.value.maxPositionPct),
      dailyStopLossPct: Number(settings.value.dailyStopLossPct),
      useMarketTrendFilter: Boolean(settings.value.useMarketTrendFilter),
      cumulativeLossLimitPct: Number(settings.value.cumulativeLossLimitPct),
      consecutiveStopLossLimit: Number(settings.value.consecutiveStopLossLimit)
    }
    
    await tradingApi.createSettings(payload)
    hasExistingSettings.value = true
    message.value = '기본 거래 설정이 자동으로 생성되었습니다.'
    messageType.value = 'success'
  } catch (createError: any) {
    // 이미 설정이 존재하는 경우 (동시 요청 등)
    if (createError.response?.status === 400 || createError.response?.status === 409) {
      hasExistingSettings.value = true
      message.value = '거래 설정을 불러왔습니다.'
      messageType.value = 'info'
    } else {
      console.error('기본 설정 생성 실패:', createError)
      hasExistingSettings.value = false
      message.value = '기본 설정 생성에 실패했습니다. 직접 저장해주세요.'
      messageType.value = 'warning'
    }
  }
}

// 거래 설정 저장
const saveSettings = async () => {
  if (!formRef.value) return

  const { valid: isValid } = await formRef.value.validate()
  if (!isValid) return

  loading.value = true
  message.value = ''

  try {
    // 데이터 정제 (빈 값 처리, 타입 보장)
    const payload = {
      coinSymbols: settings.value.coinSymbols,
      basePeriod: Number(settings.value.basePeriod),
      buyThresholdPct: Number(settings.value.buyThresholdPct),
      sellTargetPct: Number(settings.value.sellTargetPct),
      stopLossPct: Number(settings.value.stopLossPct),
      maxHoldingsPerCoin: Number(settings.value.maxHoldingsPerCoin),
      dailyLimitAmount: Number(settings.value.dailyLimitAmount),
      useTrailingStop: Boolean(settings.value.useTrailingStop),
      trailingStopPct: -Math.abs(Number(settings.value.trailingStopPct)),
      useAiAnalysis: Boolean(settings.value.useAiAnalysis),
      rsiPeriod: Number(settings.value.rsiPeriod),
      rsiBuyThreshold: Number(settings.value.rsiBuyThreshold),
      rsiSellThreshold: Number(settings.value.rsiSellThreshold),
      bbPeriod: Number(settings.value.bbPeriod),
      bbMultiplier: Number(settings.value.bbMultiplier),
      volumeThreshold: Number(settings.value.volumeThreshold),
      dailyTradeLimitPct: Number(settings.value.dailyTradeLimitPct),
      maxPositionPct: Number(settings.value.maxPositionPct),
      dailyStopLossPct: Number(settings.value.dailyStopLossPct),
      useMarketTrendFilter: Boolean(settings.value.useMarketTrendFilter),
      cumulativeLossLimitPct: Number(settings.value.cumulativeLossLimitPct),
      consecutiveStopLossLimit: Number(settings.value.consecutiveStopLossLimit)
    }

    console.log('Sending payload:', payload) // 디버깅용

    if (hasExistingSettings.value) {
      await tradingApi.updateSettings(payload)
      message.value = '거래 설정이 성공적으로 수정되었습니다'
    } else {
      await tradingApi.createSettings(payload)
      message.value = '거래 설정이 성공적으로 생성되었습니다'
      hasExistingSettings.value = true
    }

    messageType.value = 'success'
    
    // 3초 후 새로고침
    setTimeout(() => {
      location.reload()
    }, 1500)
    
  } catch (error: any) {
    console.error('Save error:', error)
    console.error('Error response:', error.response?.data)
    
    const errorData = error.response?.data
    if (typeof errorData === 'object' && errorData !== null) {
      // Validation 에러 표시
      const errors = Object.entries(errorData)
        .map(([field, msg]) => `${field}: ${msg}`)
        .join('\n')
      message.value = `입력값 오류:\n${errors}`
    } else {
      message.value = errorData || error.message || '거래 설정 저장에 실패했습니다'
    }
    messageType.value = 'error'
  } finally {
    loading.value = false
  }
}

// 폼 초기화
const resetForm = () => {
  resetDialog.value = true
}

// ★★★ 신규 추가: 실제 초기화 실행 ★★★
const executeReset = () => {
  settings.value = { ...defaultSettings }
  if (formRef.value) {
    formRef.value.resetValidation()
  }
  resetDialog.value = false
  message.value = '기본 설정값이 로드되었습니다. 저장 버튼을 눌러 적용하세요.'
  messageType.value = 'info'
}

// 삭제 확인
const confirmDelete = () => {
  deleteDialog.value = true
}

// 거래 설정 삭제
const deleteSettings = async () => {
  deleteLoading.value = true
  message.value = ''

  try {
    await tradingApi.deleteSettings()

    deleteDialog.value = false

    // ★★★ 수정: 삭제 후 기본값으로 자동 저장 ★★★
    settings.value = { ...defaultSettings }
    
    // 기본값으로 새로 생성
    const payload = {
      coinSymbols: settings.value.coinSymbols,
      basePeriod: Number(settings.value.basePeriod),
      buyThresholdPct: Number(settings.value.buyThresholdPct),
      sellTargetPct: Number(settings.value.sellTargetPct),
      stopLossPct: Number(settings.value.stopLossPct),
      maxHoldingsPerCoin: Number(settings.value.maxHoldingsPerCoin),
      dailyLimitAmount: Number(settings.value.dailyLimitAmount),
      useTrailingStop: Boolean(settings.value.useTrailingStop),
      trailingStopPct: -Math.abs(Number(settings.value.trailingStopPct)),
      useAiAnalysis: Boolean(settings.value.useAiAnalysis),
      rsiPeriod: Number(settings.value.rsiPeriod),
      rsiBuyThreshold: Number(settings.value.rsiBuyThreshold),
      rsiSellThreshold: Number(settings.value.rsiSellThreshold),
      bbPeriod: Number(settings.value.bbPeriod),
      bbMultiplier: Number(settings.value.bbMultiplier),
      volumeThreshold: Number(settings.value.volumeThreshold),
      dailyTradeLimitPct: Number(settings.value.dailyTradeLimitPct),
      maxPositionPct: Number(settings.value.maxPositionPct),
      dailyStopLossPct: Number(settings.value.dailyStopLossPct),
      useMarketTrendFilter: Boolean(settings.value.useMarketTrendFilter),
      cumulativeLossLimitPct: Number(settings.value.cumulativeLossLimitPct),
      consecutiveStopLossLimit: Number(settings.value.consecutiveStopLossLimit)
    }
    
    await tradingApi.createSettings(payload)
    hasExistingSettings.value = true
    
    if (formRef.value) {
      formRef.value.resetValidation()
    }
    
    message.value = '거래 설정이 초기화되고 기본값으로 저장되었습니다.'
    messageType.value = 'success'
  } catch (error: any) {
    message.value = error.response?.data?.message || '거래 설정 삭제에 실패했습니다'
    messageType.value = 'error'
  } finally {
    deleteLoading.value = false
  }
}

// 컴포넌트 마운트 시 데이터 로드
onMounted(async () => {
  await loadActiveCoins()
  await loadSettings()
  
  // ✅ 추가: URL query 파라미터로 전달된 코인 자동 추가
  const addCoin = route.query.addCoin as string
  if (addCoin && !settings.value.coinSymbols.includes(addCoin)) {
    settings.value.coinSymbols.push(addCoin)
    message.value = `${addCoin} 코인이 거래 종목에 추가되었습니다. 저장 버튼을 눌러 설정을 저장하세요.`
    messageType.value = 'info'
  }
})
</script>

<style scoped>
.gap-3 {
  gap: 12px;
}
</style>