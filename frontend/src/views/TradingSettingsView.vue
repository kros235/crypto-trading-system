<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />
    
    <v-main>
      <v-container>
        <v-row>
      <v-col cols="12">
        <h1 class="text-h4 mb-6">거래 설정</h1>
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
                <h3 class="text-h6 mb-3">
                  <v-icon icon="mdi-currency-btc" class="mr-2" />
                  거래 종목 선택
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
                <h3 class="text-h6 mb-3">
                  <v-icon icon="mdi-chart-line" class="mr-2" />
                  기술적 지표 설정
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
                <h3 class="text-h6 mb-3">
                  <v-icon icon="mdi-currency-usd" class="mr-2" />
                  매수 조건 설정
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
                <h3 class="text-h6 mb-3">
                  <v-icon icon="mdi-cash-multiple" class="mr-2" />
                  매도 조건 설정
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


              <!-- ★★★ 신규 추가: 기술적 지표 설정 ★★★ -->
              <v-card class="mb-4">
                <v-card-title class="d-flex align-center">
                  <v-icon class="mr-2">mdi-chart-bell-curve-cumulative</v-icon>
                  기술적 지표 설정
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

              <div class="mb-6">
                <h3 class="text-h6 mb-3">
                  <v-icon icon="mdi-shield-check" class="mr-2" />
                  리스크 관리
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
          <v-divider class="my-4"></v-divider>
          <div class="text-subtitle-1 font-weight-bold mb-3">
            <v-icon class="mr-2">mdi-shield-alert</v-icon>
            급락장 보호 기능
          </div>
          
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