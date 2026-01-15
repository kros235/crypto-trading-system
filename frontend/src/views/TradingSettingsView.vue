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
                    <v-checkbox
                      v-model="settings.useTrailingStop"
                      label="트레일링 스톱 사용"
                      color="primary"
                      hide-details
                    />
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


              <!-- 기술적 지표 설정 -->
              <v-card class="mb-4">
                <v-card-title class="d-flex align-center">
                  <v-icon class="mr-2">mdi-chart-bell-curve-cumulative</v-icon>
                  기술적 지표 설정
                  <!-- 도움말 버튼 -->
                  <HelpButton 
                    use-dialog 
                    :dialog-title="helpContents.technicalIndicator.title"
                    :dialog-content="helpContents.technicalIndicator.content"
                    :dialog-width="700"
                    color="grey-darken-1"
                  />
                </v-card-title>
                <v-card-text>
                  <!-- RSI 설정 -->
                  <div class="text-subtitle-2 text-grey mb-2">
                    <v-icon size="small" class="mr-1">mdi-chart-line</v-icon>
                    RSI (상대강도지수)
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
                  <div class="text-subtitle-2 text-grey mb-2">
                    <v-icon size="small" class="mr-1">mdi-chart-bell-curve</v-icon>
                    볼린저 밴드
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
                  <div class="text-subtitle-2 text-grey mb-2">
                    <v-icon size="small" class="mr-1">mdi-chart-bar</v-icon>
                    거래량 분석
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
                          <v-chip size="small" color="primary">
                            {{ settings.volumeThreshold }}%
                          </v-chip>
                        </template>
                      </v-slider>
                      <p class="text-caption text-grey">
                        평균 거래량 대비 {{ settings.volumeThreshold }}% 이상일 때 거래량 급증으로 판단합니다.
                      </p>
                    </v-col>
                  </v-row>
                </v-card-text>
              </v-card>

              <v-divider class="my-6" />

              <!-- 리스크 관리 설정 -->
              <div class="mb-6">
                <h3 class="text-h6 mb-3 d-flex align-center">
                  <v-icon icon="mdi-shield-alert" class="mr-2" />
                  리스크 관리
                  <!-- 도움말 버튼 -->
                  <HelpButton 
                    use-dialog 
                    :dialog-title="helpContents.riskManagement.title"
                    :dialog-content="helpContents.riskManagement.content"
                    color="grey-darken-1"
                  />
                </h3>

                <!-- 일일 거래 한도 -->
                <div class="text-caption text-grey mb-2">일일 최대 거래금액 (초기 자본 대비)</div>
                <v-slider
                  v-model="settings.dailyTradeLimitPct"
                  :min="10"
                  :max="100"
                  :step="10"
                  thumb-label
                  hide-details
                  class="mb-1"
                >
                  <template v-slot:append>
                    <span class="text-body-2" style="min-width: 80px">
                      {{ settings.dailyTradeLimitPct === 100 ? '제한없음' : `${settings.dailyTradeLimitPct}%` }}
                    </span>
                  </template>
                </v-slider>
                <div class="text-caption text-grey-darken-1 mb-4">
                  일일 한도 금액 기준으로 하루 최대 매수 가능 금액을 제한합니다 ({{ formatCurrency(settings.dailyLimitAmount * settings.dailyTradeLimitPct / 100) }})
                </div>

                <!-- 단일 종목 비중 제한 -->
                <div class="text-caption text-grey mb-2 mt-4">단일 종목 최대 비중 (총 자본 대비)</div>
                <v-slider
                  v-model="settings.maxPositionPct"
                  :min="10"
                  :max="100"
                  :step="5"
                  thumb-label
                  hide-details
                  class="mb-1"
                >
                  <template v-slot:append>
                    <span class="text-body-2" style="min-width: 80px">
                      {{ settings.maxPositionPct === 100 ? '제한없음' : `${settings.maxPositionPct}%` }}
                    </span>
                  </template>
                </v-slider>
                <div class="text-caption text-grey-darken-1 mb-4">
                  한 코인에 최대 투자 가능 금액을 제한합니다 ({{ formatCurrency(settings.dailyLimitAmount * settings.maxPositionPct / 100) }})
                </div>

                <!-- 긴급 정지 조건 -->
                <div class="text-caption text-grey mb-2 mt-4">긴급 정지 (일일 손실률)</div>
                <v-slider
                  v-model="settings.dailyStopLossPct"
                  :min="-50"
                  :max="0"
                  :step="5"
                  thumb-label
                  hide-details
                  color="error"
                  class="mb-1"
                >
                  <template v-slot:append>
                    <span class="text-body-2" style="min-width: 80px">
                      {{ settings.dailyStopLossPct === 0 ? '사용안함' : `${settings.dailyStopLossPct}%` }}
                    </span>
                  </template>
                </v-slider>
                <div class="text-caption text-grey-darken-1 mb-2">
                  당일 손실이 이 값에 도달하면 거래를 자동 중단합니다
                </div>
              </div>

	<!-- 급락장 보호 기능 -->
              <div class="mb-6">
                <h3 class="text-h6 mb-3 d-flex align-center">
                  <v-icon icon="mdi-alert-octagon" class="mr-2" />
                  급락장 보호 기능
                  <!-- 도움말 버튼 -->
                  <HelpButton 
                    use-dialog 
                    :dialog-title="helpContents.crashProtection.title"
                    :dialog-content="helpContents.crashProtection.content"
                    color="grey-darken-1"
                  />
                </h3>
          
          <!-- 시장 추세 필터 -->
          <v-switch
            v-model="settings.useMarketTrendFilter"
            label="시장 추세 필터 사용"
            hint="BTC가 20일 이동평균선 아래로 하락하면 전체 매수를 중단합니다"
            persistent-hint
            color="primary"
            class="mb-4"
          ></v-switch>
          
          <!-- 누적 손실 한도 -->
          <v-slider
            v-model="settings.cumulativeLossLimitPct"
            :min="-50"
            :max="0"
            :step="5"
            label="누적 손실 한도"
            thumb-label="always"
            color="error"
            class="mb-2"
          >
            <template v-slot:thumb-label="{ modelValue }">
              {{ modelValue }}%
            </template>
          </v-slider>
          <div class="text-caption text-grey mb-4">
            초기 자본 대비 누적 손실이 이 수치에 도달하면 모든 거래를 중단합니다 (현재: {{ settings.cumulativeLossLimitPct }}%)
          </div>
          
          <!-- 연속 손절 제한 -->
          <v-slider
            v-model="settings.consecutiveStopLossLimit"
            :min="1"
            :max="10"
            :step="1"
            label="연속 손절 제한"
            thumb-label="always"
            color="warning"
            class="mb-2"
          >
            <template v-slot:thumb-label="{ modelValue }">
              {{ modelValue }}회
            </template>
          </v-slider>
          <div class="text-caption text-grey mb-4">
            동일 코인에서 {{ settings.consecutiveStopLossLimit }}회 연속 손절 시 해당 코인 24시간 매수를 금지합니다
          </div>
              </div>

              <v-divider class="my-6" />

              <!-- 추가 옵션 -->
              <div class="mb-6">
                <h3 class="text-h6 mb-3">
                  <v-icon icon="mdi-brain" class="mr-2" />
                  추가 옵션
                </h3>

                <v-checkbox
                  v-model="settings.useAiAnalysis"
                  label="AI 뉴스 분석 사용"
                  color="primary"
                  hint="ChatGPT/Claude API를 통한 뉴스 분석으로 매수/매도 판단에 활용"
                />

                <v-alert
                  v-if="settings.useAiAnalysis"
                  type="info"
                  class="mt-3"
                  icon="mdi-information"
                >
                  <div class="text-body-2">
                    AI 뉴스 분석은 실시간 뉴스를 분석하여 호재/악재 여부를 판단합니다.
                    판단 결과는 매수 기준가(buyThresholdPct)에 ±0.5% 범위 내에서 조정됩니다.
                  </div>
                </v-alert>
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
  // 기술적 지표 카드 도움말
  technicalIndicator: {
    title: '📊 기술적 지표 설정',
    content: `
      <div class="help-section">
        <h4 style="color: #1565C0; margin-bottom: 12px;">📈 RSI (상대강도지수)</h4>
        <div class="help-box">
          <p><strong>📖 쉬운 설명:</strong> "지금 너무 많이 올랐나? 많이 떨어졌나?" 판단 지표</p>
          <p>0~100 사이 숫자로 표현</p>
          <div class="help-visual" style="background: #263238; padding: 12px; border-radius: 8px; margin: 12px 0; font-family: monospace; color: #fff;">
            <p style="margin: 4px 0;">100 ── 🔥 극도로 과열 (팔아야 할 때)</p>
            <p style="margin: 4px 0;"> 70 ── ⚠️ 과열 (매도 신호) ← <span style="color: #4CAF50;">매도 ≥ 68</span></p>
            <p style="margin: 4px 0;"> 50 ── 😐 보통</p>
            <p style="margin: 4px 0;"> 30 ── ❄️ 냉각 (매수 신호) ← <span style="color: #2196F3;">매수 ≤ 32</span></p>
            <p style="margin: 4px 0;">  0 ── 🥶 극도로 냉각 (사야 할 때)</p>
          </div>
          <p><strong>🎯 현재 설정:</strong></p>
          <p>• RSI ≤ 32: "충분히 떨어졌다, 매수!"</p>
          <p>• RSI ≥ 68: "충분히 올랐다, 매도!"</p>
        </div>
      </div>
      <div class="help-section" style="margin-top: 16px;">
        <h4 style="color: #1565C0; margin-bottom: 12px;">📉 볼린저 밴드</h4>
        <div class="help-box">
          <p><strong>📖 쉬운 설명:</strong> 가격이 움직이는 "정상 범위"를 보여주는 밴드</p>
          <p>• <strong>상단 밴드</strong>: 이 위로 가면 "너무 비싸다"</p>
          <p>• <strong>중심선</strong>: 평균 가격 (이동평균선)</p>
          <p>• <strong>하단 밴드</strong>: 이 아래로 가면 "너무 싸다" → 매수 기회!</p>
          <p style="margin-top: 8px;"><strong>🎯 현재 설정:</strong> 20일 기준, 표준편차 2배</p>
        </div>
      </div>
      <div class="help-section" style="margin-top: 16px;">
        <h4 style="color: #1565C0; margin-bottom: 12px;">📊 거래량 급증 기준</h4>
        <div class="help-box">
          <p><strong>📖 쉬운 설명:</strong> "평소보다 거래가 얼마나 활발해야 진짜 신호로 볼 것인가?"</p>
          <p><strong>🏪 가게 비유:</strong></p>
          <p>• 평소 하루 100명 오는 가게에</p>
          <p>• 오늘 140명 왔다 → "뭔가 있네! 관심 가져볼까?"</p>
          <p>• 오늘 200명 왔다 → "대박 터졌다!"</p>
          <p>• 오늘 80명 왔다 → "오늘은 조용하네..."</p>
          <p style="margin-top: 8px;"><strong>💡 왜 중요한가?</strong></p>
          <p>• 거래량 없이 가격만 움직이면 → 세력의 조작일 수 있음</p>
          <p>• 거래량 터지면서 움직이면 → 진짜 시장 반응!</p>
        </div>
      </div>
    `
  },
  coinSelect: {
    title: '📊 거래 종목 선택',
    content: `
      <p class="help-intro">자동매매 대상이 되는 코인을 선택합니다.</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>권장 코인 수</strong>: 3~5개 (분산 투자 효과)<br/>
        <span class="help-desc">너무 많으면 관리가 어렵고, 너무 적으면 기회가 줄어듭니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>우선순위</strong>: BTC, ETH 등 시가총액 상위 코인 권장<br/>
        <span class="help-desc">변동성이 크거나 거래량이 적은 코인은 리스크가 높습니다.</span></p>
      <p class="help-note">💡 시가총액 순위가 높은 코인일수록 안정적인 거래가 가능합니다.</p>
    `
  },
  indicator: {
    title: '📈 이동평균선 기간 선택',
    content: `
      <p class="help-intro">매수/매도 판단의 기준이 되는 이동평균선 기간을 선택합니다.</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>7일</strong>: 단기 추세, 잦은 거래, 빠른 반응</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>14일</strong>: 단기~중기, 균형잡힌 설정</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>20일</strong>: 중기 추세, 가장 일반적 (권장 ✅)</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>30일</strong>: 장기 추세, 신중한 거래</p>
      <p class="help-note">💡 기본값 20일은 백테스팅으로 검증된 최적의 설정입니다.</p>
    `
  },
  buyCondition: {
    title: '🛒 매수 조건 설정',
    content: `
      <div class="help-section">
        <h4 style="color: #1565C0; margin-bottom: 12px;">1. 매수 기준 - MA 대비 % (현재: -6%)</h4>
        <div class="help-box">
          <p><strong>📖 쉬운 설명:</strong> "평균 가격보다 얼마나 떨어지면 살 것인가?"</p>
          <p><strong>🛒 마트 예시:</strong></p>
          <p>평소 10,000원 하는 운동화가 있어요.</p>
          <p>• -3% 설정: 9,700원 되면 구매</p>
          <p>• -6% 설정: 9,400원 되면 구매</p>
          <p>• -10% 설정: 9,000원 되면 구매</p>
          <p style="margin-top: 8px;"><strong>숫자가 클수록(음수가 클수록)</strong></p>
          <p>→ 더 많이 떨어져야 삼</p>
          <p>→ 거래 횟수 적어짐</p>
          <p>→ 더 신중한 투자</p>
        </div>
        <table style="width: 100%; margin-top: 12px; border-collapse: collapse; font-size: 14px;">
          <tr style="border-bottom: 1px solid #ddd;">
            <th style="text-align: left; padding: 8px;">설정값</th>
            <th style="text-align: left; padding: 8px;">의미</th>
            <th style="text-align: left; padding: 8px;">거래 빈도</th>
          </tr>
          <tr><td style="padding: 8px;">-3%</td><td>조금만 떨어져도 삼</td><td>많음 (공격적)</td></tr>
          <tr style="background: #E8F5E9;"><td style="padding: 8px;"><strong>-6%</strong></td><td>적당히 떨어지면 삼</td><td>보통 ✅</td></tr>
          <tr><td style="padding: 8px;">-10%</td><td>많이 떨어져야 삼</td><td>적음 (신중)</td></tr>
        </table>
      </div>
      <div class="help-section" style="margin-top: 16px;">
        <h4 style="color: #1565C0; margin-bottom: 12px;">2. 종목당 최대 보유 (현재: 2건)</h4>
        <div class="help-box">
          <p><strong>📖 쉬운 설명:</strong> "한 코인을 최대 몇 번까지 나눠서 살 것인가?"</p>
          <p><strong>🛍️ 쇼핑 예시:</strong></p>
          <p>맘에 드는 가방이 있는데 가격이 계속 떨어져요.</p>
          <p>• 1회 설정: 한 번만 사고 끝</p>
          <p>• 2회 설정: 더 떨어지면 한 번 더 삼 (2번까지)</p>
          <p>• 3회 설정: 최대 3번까지 나눠서 삼</p>
          <p style="margin-top: 8px;"><strong>장점:</strong> 물타기로 평균 단가 낮출 수 있음</p>
          <p><strong>단점:</strong> 계속 떨어지면 손실 커짐</p>
        </div>
        <table style="width: 100%; margin-top: 12px; border-collapse: collapse; font-size: 14px;">
          <tr style="border-bottom: 1px solid #ddd;">
            <th style="text-align: left; padding: 8px;">설정값</th>
            <th style="text-align: left; padding: 8px;">의미</th>
            <th style="text-align: left; padding: 8px;">리스크</th>
          </tr>
          <tr><td style="padding: 8px;">1</td><td>한 번만 삼</td><td>낮음 (분산)</td></tr>
          <tr style="background: #E8F5E9;"><td style="padding: 8px;"><strong>2</strong></td><td>두 번까지</td><td>보통 ✅</td></tr>
          <tr><td style="padding: 8px;">3+</td><td>여러 번</td><td>높음 (집중)</td></tr>
        </table>
      </div>
    `
  },
  sellCondition: {
    title: '💰 매도 조건 설정',
    content: `
      <div class="help-section">
        <h4 style="color: #1565C0; margin-bottom: 12px;">1. 목표 수익률 % (현재: 4%)</h4>
        <div class="help-box">
          <p><strong>📖 쉬운 설명:</strong> "얼마 오르면 팔 것인가?"</p>
          <p><strong>🛒 중고거래 예시:</strong></p>
          <p>10,000원에 산 물건을</p>
          <p>• 3% 설정: 10,300원에 판매</p>
          <p>• 4% 설정: 10,400원에 판매</p>
          <p>• 10% 설정: 11,000원에 판매</p>
          <p style="margin-top: 8px;"><strong>낮게 설정하면:</strong></p>
          <p>✅ 자주 수익 실현</p>
          <p>❌ 큰 상승 놓칠 수 있음</p>
          <p style="margin-top: 8px;"><strong>높게 설정하면:</strong></p>
          <p>✅ 큰 수익 가능</p>
          <p>❌ 목표 도달 못하고 하락할 수 있음</p>
        </div>
        <table style="width: 100%; margin-top: 12px; border-collapse: collapse; font-size: 14px;">
          <tr style="border-bottom: 1px solid #ddd;">
            <th style="text-align: left; padding: 8px;">설정값</th>
            <th style="text-align: left; padding: 8px;">특징</th>
            <th style="text-align: left; padding: 8px;">적합한 상황</th>
          </tr>
          <tr><td style="padding: 8px;">2~3%</td><td>빠른 수익 실현</td><td>횡보장, 하락장</td></tr>
          <tr style="background: #E8F5E9;"><td style="padding: 8px;"><strong>4~5%</strong></td><td>균형잡힌 목표</td><td>초보자 추천 ✅</td></tr>
          <tr><td style="padding: 8px;">10%+</td><td>큰 수익 노림</td><td>상승장</td></tr>
        </table>
      </div>
      <div class="help-section" style="margin-top: 16px;">
        <h4 style="color: #1565C0; margin-bottom: 12px;">2. 손절매 기준 % (현재: -8%)</h4>
        <div class="help-box">
          <p><strong>📖 쉬운 설명:</strong> "얼마나 손해보면 포기하고 팔 것인가?"</p>
          <p><strong>🎰 도박 예시:</strong></p>
          <p>카지노에서 10만원 들고 갔는데</p>
          <p>• -5% 설정: 9.5만원 되면 "그만!"</p>
          <p>• -8% 설정: 9.2만원 되면 "그만!"</p>
          <p>• -15% 설정: 8.5만원 되면 "그만!"</p>
          <p style="margin-top: 8px;"><strong>⚠️ 왜 필요한가?</strong></p>
          <p>손절매 없이 버티면...</p>
          <p>10만원 → 5만원 → 2만원 → 0원 😱</p>
          <p style="margin-top: 8px;"><strong>손절매 있으면...</strong></p>
          <p>10만원 → 9.2만원 → "여기서 멈춤!"</p>
          <p>→ 남은 돈으로 다시 도전 가능</p>
        </div>
        <table style="width: 100%; margin-top: 12px; border-collapse: collapse; font-size: 14px;">
          <tr style="border-bottom: 1px solid #ddd;">
            <th style="text-align: left; padding: 8px;">설정값</th>
            <th style="text-align: left; padding: 8px;">특징</th>
            <th style="text-align: left; padding: 8px;">멘탈 요구도</th>
          </tr>
          <tr><td style="padding: 8px;">-5%</td><td>빠른 손절</td><td>약함 (안전)</td></tr>
          <tr style="background: #E8F5E9;"><td style="padding: 8px;"><strong>-8%</strong></td><td>적당한 손절</td><td>보통 ✅</td></tr>
          <tr><td style="padding: 8px;">-15%</td><td>느린 손절</td><td>강함 (위험)</td></tr>
        </table>
      </div>
    `
  },
  riskManagement: {
    title: '🛡️ 리스크 관리',
    content: `
      <div class="help-section">
        <h4 style="color: #1565C0; margin-bottom: 12px;">1. 일일 최대 거래금액 (현재: 20%)</h4>
        <div class="help-box">
          <p><strong>📖 쉬운 설명:</strong> "하루에 최대 얼마까지만 살 것인가?"</p>
          <p><strong>💰 용돈 비유:</strong></p>
          <p>월급 100만원 받았는데</p>
          <p>• 100% 설정: 하루에 100만원 다 써도 됨 (위험!)</p>
          <p>• 20% 설정: 하루에 20만원까지만 씀</p>
          <p style="margin-top: 8px;"><strong>🎯 현재 설정:</strong></p>
          <p>초기 자본 1,000,000원의 20% = 200,000원</p>
          <p>→ 하루에 최대 20만원어치만 매수 가능</p>
          <p style="margin-top: 8px;"><strong>💡 왜 필요한가?</strong></p>
          <p>"오늘 기회다!" 하고 한 번에 다 샀는데</p>
          <p>다음날 더 떨어지면? 😱 살 돈이 없음!</p>
        </div>
      </div>
      <div class="help-section" style="margin-top: 16px;">
        <h4 style="color: #1565C0; margin-bottom: 12px;">2. 단일 종목 최대 비중 (현재: 25%)</h4>
        <div class="help-box">
          <p><strong>📖 쉬운 설명:</strong> "한 코인에 최대 얼마까지 투자할 것인가?"</p>
          <p><strong>🥚 계란 비유:</strong></p>
          <p>"계란을 한 바구니에 담지 마라"</p>
          <p style="margin-top: 8px;"><strong>100만원이 있을 때:</strong></p>
          <p>• 100% 설정: 비트코인에 100만원 몰빵 가능</p>
          <p>• 25% 설정: 비트코인에 최대 25만원까지만! 나머지는 다른 코인에 분산</p>
          <p style="margin-top: 8px;"><strong>💡 왜 필요한가?</strong></p>
          <p>비트코인에 100만원 몰빵 → 비트코인 -30% → 30만원 손실</p>
          <p>4개 코인에 25만원씩 → 비트코인 -30% → 7.5만원 손실</p>
        </div>
      </div>
      <div class="help-section" style="margin-top: 16px;">
        <h4 style="color: #1565C0; margin-bottom: 12px;">3. 긴급 정지 - 일일 손실률 (현재: -5%)</h4>
        <div class="help-box">
          <p><strong>📖 쉬운 설명:</strong> "오늘 손실이 이 정도면 오늘은 거래 중단!"</p>
          <p><strong>🚨 비상 브레이크 비유:</strong></p>
          <p>자동차가 너무 빨리 가면 비상 브레이크!</p>
          <p>투자도 손실이 너무 커지면 "오늘은 그만!"</p>
          <p style="margin-top: 8px;"><strong>🎯 현재 설정:</strong></p>
          <p>1,000,000원 × -5% = -50,000원</p>
          <p>→ 오늘 손실이 5만원 넘으면 자동으로 거래 중단</p>
          <p style="margin-top: 8px;"><strong>💡 왜 필요한가?</strong></p>
          <p>"오늘 손해 봤으니 더 사서 만회해야지!"</p>
          <p>→ 복수 매매 → 더 큰 손실 😱</p>
          <p style="margin-top: 8px;"><strong>긴급 정지 있으면:</strong></p>
          <p>"5만원 잃었으니 오늘은 쉬자"</p>
          <p>→ 냉정해진 후 내일 다시 시작</p>
        </div>
      </div>
    `
  },
  crashProtection: {
    title: '🚨 급락장 보호 기능',
    content: `
      <p class="help-intro">급격한 시장 하락 시 손실을 줄이기 위한 안전장치입니다.</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>시장 추세 필터</strong><br/>
        <span class="help-desc">BTC가 20일선 아래일 때 전체 매수를 중단합니다. (기본: OFF)</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>누적 손실 긴급정지</strong><br/>
        <span class="help-desc">초기 자본 대비 누적 손실이 설정값에 도달하면 거래를 중단합니다. (기본: -10%)</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>연속 손절 제한</strong><br/>
        <span class="help-desc">동일 코인에서 연속 손절 시 해당 코인 매수를 24시간 금지합니다. (기본: 3회)</span></p>
      <p class="help-note">💡 백테스팅 결과, 기본 설정으로 급락장 손실을 55% 줄일 수 있습니다.</p>
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