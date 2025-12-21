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
                      label="일일 최대 거래 금액"
                      type="number"
                      :rules="[rules.required, rules.positive]"
                      variant="outlined"
                      suffix="원"
                      hint="하루 최대 거래 가능 금액"
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

                  <v-col cols="12" md="4">
                    <v-checkbox
                      v-model="settings.useTrailingStop"
                      label="트레일링 스톱 사용"
                      color="primary"
                      hint="최고가 대비 일정 비율 하락 시 자동 매도"
                    />
                  </v-col>
                </v-row>

                <!-- 트레일링 스톱 비율 입력 (조건부 렌더링) -->
				<v-row v-if="settings.useTrailingStop">
				  <v-col cols="12" md="4">
				    <v-text-field
				      v-model.number="settings.trailingStopPct"
				      label="트레일링 스톱 비율 (%)"
				      type="number"
				      :rules="[rules.negative]"
				      variant="outlined"
				      suffix="%"
				      hint="최고가 대비 이 값 이하로 하락 시 매도"
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
                    판단 결과는 매수/매도 기준가에 ±2% 반영됩니다.
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
    <v-dialog v-model="deleteDialog" max-width="400">
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

// 활성 코인 목록
const availableCoins = ref<Array<CoinInfo & { displayName: string }>>([])

// 기존 설정 존재 여부
const hasExistingSettings = ref(false)

// 거래 설정 폼 데이터
const settings = ref({
  coinSymbols: [] as string[],
  basePeriod: 20,
  buyThresholdPct: -5,       // ✅ 음수로 변경
  sellTargetPct: 3,
  stopLossPct: -10,
  maxHoldingsPerCoin: 3,
  dailyLimitAmount: 1000000,
  useTrailingStop: false,
  trailingStopPct: -5,  // ← 이 줄 추가
  useAiAnalysis: false,
  rsiPeriod: 14,
  rsiBuyThreshold: 30,
  rsiSellThreshold: 70,
  bbPeriod: 20,
  bbMultiplier: 2,
  volumeThreshold: 150
})

// 기본값 (초기화용)
const defaultSettings = {
  coinSymbols: [],
  basePeriod: 20,
  buyThresholdPct: -5,       // ✅ 음수로 변경
  sellTargetPct: 3,
  stopLossPct: -10,
  maxHoldingsPerCoin: 3,
  dailyLimitAmount: 1000000,
  useTrailingStop: false,
  trailingStopPct: -5,
  useAiAnalysis: false,
  rsiPeriod: 14,
  rsiBuyThreshold: 30,
  rsiSellThreshold: 70,
  bbPeriod: 20,
  bbMultiplier: 2,
  volumeThreshold: 150
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

    if (data) {
      settings.value = {
        coinSymbols: data.coinSymbols || [],
        basePeriod: data.basePeriod || 20,
        buyThresholdPct: data.buyThresholdPct || -5,  // ✅ 음수
        sellTargetPct: data.sellTargetPct || 3,
        stopLossPct: data.stopLossPct || -10,
        maxHoldingsPerCoin: data.maxHoldingsPerCoin || 3,
        dailyLimitAmount: data.dailyLimitAmount || 1000000,
        useTrailingStop: data.useTrailingStop || false,
        trailingStopPct: data.trailingStopPct || -5,  // ← 이 줄 추가
        useAiAnalysis: data.useAiAnalysis || false,
        rsiPeriod: data.rsiPeriod || 14,
        rsiBuyThreshold: data.rsiBuyThreshold || 30,
        rsiSellThreshold: data.rsiSellThreshold || 70,
        bbPeriod: data.bbPeriod || 20,
        bbMultiplier: data.bbMultiplier || 2,
        volumeThreshold: data.volumeThreshold || 150
      }

      hasExistingSettings.value = true
      message.value = '기존 거래 설정을 불러왔습니다'
      messageType.value = 'info'
    }
  } catch (error: any) {
    // 404는 설정이 없는 경우이므로 무시
    if (error.response?.status !== 404) {
      message.value = error.response?.data?.message || '거래 설정을 불러오는데 실패했습니다'
      messageType.value = 'error'
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
      trailingStopPct: Number(settings.value.trailingStopPct),
      useAiAnalysis: Boolean(settings.value.useAiAnalysis),
      rsiPeriod: Number(settings.value.rsiPeriod),
      rsiBuyThreshold: Number(settings.value.rsiBuyThreshold),
      rsiSellThreshold: Number(settings.value.rsiSellThreshold),
      bbPeriod: Number(settings.value.bbPeriod),
      bbMultiplier: Number(settings.value.bbMultiplier),
      volumeThreshold: Number(settings.value.volumeThreshold)
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
  settings.value = { ...defaultSettings }
  if (formRef.value) {
    formRef.value.reset()
  }
  message.value = ''
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

    message.value = '거래 설정이 삭제되었습니다'
    messageType.value = 'success'

    deleteDialog.value = false
    hasExistingSettings.value = false

    // 폼 초기화
    resetForm()
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