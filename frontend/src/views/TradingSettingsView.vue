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
// ⭐ 변경: helpContents 객체를 용어사전 레이아웃(쉬운설명, 비유, 시각적 설명)으로 전면 개편
const helpContents = {
  coinSelection: `
    <div class="help-content">
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="blue" class="mr-2">mdi-lightbulb</v-icon>
          <strong>쉬운 설명</strong>
        </div>
        <p>자동매매 봇이 거래할 코인을 선택합니다. 선택한 코인들만 매수/매도 대상이 됩니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="orange" class="mr-2">mdi-food-apple</v-icon>
          <strong>비유</strong>
        </div>
        <p>마트에서 살 물건 목록을 정하는 것과 같습니다. 목록에 있는 상품만 장바구니에 담습니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="green" class="mr-2">mdi-chart-line</v-icon>
          <strong>권장 설정</strong>
        </div>
        <ul>
          <li><strong>초보자:</strong> BTC, ETH 등 메이저 코인 2~3개</li>
          <li><strong>중급자:</strong> 3~5개 분산 투자</li>
          <li><strong>주의:</strong> 너무 많으면 관리가 어려워집니다</li>
        </ul>
      </div>
    </div>
  `,

  basePeriod: `
    <div class="help-content">
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="blue" class="mr-2">mdi-lightbulb</v-icon>
          <strong>쉬운 설명</strong>
        </div>
        <p>이동평균선(MA)을 계산할 때 사용하는 기간입니다. 최근 N일간의 평균 가격을 구합니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="orange" class="mr-2">mdi-food-apple</v-icon>
          <strong>비유</strong>
        </div>
        <p>최근 20일간 과일 평균 가격을 계산하는 것과 같습니다. 오늘 가격이 평균보다 싸면 "할인 중"이라고 판단합니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="green" class="mr-2">mdi-chart-line</v-icon>
          <strong>시각적 설명</strong>
        </div>
        <div class="text-center my-2 pa-2 bg-grey-lighten-4 rounded">
          <code>7일선: 단기 추세 (민감) ━━━━━</code><br>
          <code>20일선: 중기 추세 (권장) ━━━━━━━━━━</code><br>
          <code>30일선: 장기 추세 (안정) ━━━━━━━━━━━━━━━</code>
        </div>
        <p class="text-caption mt-2">💡 <strong>권장값: 20일</strong> (가장 보편적인 기준)</p>
      </div>
    </div>
  `,

  buyThreshold: `
    <div class="help-content">
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="blue" class="mr-2">mdi-lightbulb</v-icon>
          <strong>쉬운 설명</strong>
        </div>
        <p>이동평균선 대비 현재가가 얼마나 떨어졌을 때 매수할지 설정합니다. -5%면 평균보다 5% 저렴할 때 매수합니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="orange" class="mr-2">mdi-food-apple</v-icon>
          <strong>비유</strong>
        </div>
        <p>평소 1만원 하던 사과가 9,500원(-5%)이 되면 "싸다!"라고 판단하고 사는 것과 같습니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="green" class="mr-2">mdi-chart-line</v-icon>
          <strong>시각적 설명</strong>
        </div>
        <div class="text-center my-2 pa-2 bg-grey-lighten-4 rounded">
          <code>20일 평균: ━━━━━━━━ 100만원</code><br>
          <code>-3% 기준: - - - - - - 97만원 (공격적)</code><br>
          <code>-5% 기준: - - - - - - 95만원 (권장)</code><br>
          <code>-8% 기준: - - - - - - 92만원 (보수적)</code>
        </div>
        <p class="text-caption mt-2">💡 <strong>권장값: -5% ~ -6%</strong></p>
      </div>
    </div>
  `,

  sellTarget: `
    <div class="help-content">
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="blue" class="mr-2">mdi-lightbulb</v-icon>
          <strong>쉬운 설명</strong>
        </div>
        <p>매수가 대비 이 수익률에 도달하면 자동으로 매도합니다. 3%면 100만원 → 103만원이 되면 매도합니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="orange" class="mr-2">mdi-food-apple</v-icon>
          <strong>비유</strong>
        </div>
        <p>1만원에 산 물건이 1만 300원(+3%)이 되면 "이 정도면 됐다!" 하고 파는 것과 같습니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="green" class="mr-2">mdi-chart-line</v-icon>
          <strong>시각적 설명</strong>
        </div>
        <div class="text-center my-2 pa-2 bg-grey-lighten-4 rounded">
          <code>매수가: ━━━━━━━━ 100만원</code><br>
          <code>+2% 목표: 🎯 102만원 (빈번한 거래)</code><br>
          <code>+3% 목표: 🎯 103만원 (권장)</code><br>
          <code>+5% 목표: 🎯 105만원 (여유있는 목표)</code>
        </div>
        <p class="text-caption mt-2">💡 <strong>권장값: 3% ~ 4%</strong></p>
      </div>
    </div>
  `,

  stopLoss: `
    <div class="help-content">
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="blue" class="mr-2">mdi-lightbulb</v-icon>
          <strong>쉬운 설명</strong>
        </div>
        <p>손실이 이 비율에 도달하면 추가 손실을 막기 위해 자동 매도합니다. -8%면 100만원 → 92만원이 되면 손절합니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="orange" class="mr-2">mdi-food-apple</v-icon>
          <strong>비유</strong>
        </div>
        <p>배가 침몰할 때 짐을 버리고 탈출하는 것과 같습니다. 작은 손실로 큰 손실을 막습니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="green" class="mr-2">mdi-chart-line</v-icon>
          <strong>시각적 설명</strong>
        </div>
        <div class="text-center my-2 pa-2 bg-grey-lighten-4 rounded">
          <code>매수가: ━━━━━━━━ 100만원</code><br>
          <code>-5% 손절: 🛑 95만원 (타이트)</code><br>
          <code>-8% 손절: 🛑 92만원 (권장)</code><br>
          <code>-10% 손절: 🛑 90만원 (여유있음)</code>
        </div>
        <p class="text-caption mt-2">💡 <strong>권장값: -8% ~ -10%</strong></p>
      </div>
    </div>
  `,

  trailingStop: `
    <div class="help-content">
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="blue" class="mr-2">mdi-lightbulb</v-icon>
          <strong>쉬운 설명</strong>
        </div>
        <p>최고가 대비 일정 비율 하락하면 매도합니다. 수익을 보호하면서 상승 추세를 최대한 따라갑니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="orange" class="mr-2">mdi-food-apple</v-icon>
          <strong>비유</strong>
        </div>
        <p>등산할 때 정상까지 올라가다가, 내리막이 시작되면 "여기까지만!" 하고 내려오는 것과 같습니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="green" class="mr-2">mdi-chart-line</v-icon>
          <strong>시각적 설명</strong>
        </div>
        <div class="text-center my-2 pa-2 bg-grey-lighten-4 rounded">
          <code>매수: 100만원 → 상승 → 최고가 120만원 📈</code><br>
          <code>트레일링 5%: 120만원 × 0.95 = 114만원</code><br>
          <code>114만원 아래로 떨어지면 → 자동 매도 🔔</code>
        </div>
        <p class="text-caption mt-2">💡 <strong>권장값: 4% ~ 5%</strong></p>
      </div>
    </div>
  `,

  maxHoldings: `
    <div class="help-content">
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="blue" class="mr-2">mdi-lightbulb</v-icon>
          <strong>쉬운 설명</strong>
        </div>
        <p>한 코인에 최대 몇 건까지 분할 매수할지 설정합니다. 3건이면 같은 코인을 3번까지 나눠 살 수 있습니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="orange" class="mr-2">mdi-food-apple</v-icon>
          <strong>비유</strong>
        </div>
        <p>계란을 한 바구니에 3개까지만 담는 것과 같습니다. 분산해서 위험을 줄입니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="green" class="mr-2">mdi-chart-line</v-icon>
          <strong>시각적 설명</strong>
        </div>
        <div class="text-center my-2 pa-2 bg-grey-lighten-4 rounded">
          <code>BTC 1차 매수: 100만원 ━━━━━</code><br>
          <code>BTC 2차 매수: 100만원 ━━━━━</code><br>
          <code>BTC 3차 매수: 100만원 ━━━━━</code><br>
          <code>─────────────────────────</code><br>
          <code>BTC 총 투자: 300만원 (최대 3건)</code>
        </div>
        <p class="text-caption mt-2">💡 <strong>권장값: 2~3건</strong></p>
      </div>
    </div>
  `,

  dailyLimit: `
    <div class="help-content">
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="blue" class="mr-2">mdi-lightbulb</v-icon>
          <strong>쉬운 설명</strong>
        </div>
        <p>하루 거래의 기준이 되는 금액입니다. 이 금액에 일일 거래 한도(%)를 곱한 만큼 실제로 거래합니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="orange" class="mr-2">mdi-food-apple</v-icon>
          <strong>비유</strong>
        </div>
        <p>용돈 통장에 100만원이 있고, 하루에 20%까지만 쓴다고 정하면 하루 최대 20만원까지만 사용하는 것과 같습니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="green" class="mr-2">mdi-chart-line</v-icon>
          <strong>시각적 설명</strong>
        </div>
        <div class="text-center my-2 pa-2 bg-grey-lighten-4 rounded">
          <code>일일 기준 금액: 500,000원</code><br>
          <code>일일 거래 한도: 20%</code><br>
          <code>─────────────────────────</code><br>
          <code>하루 최대 매수: 500,000 × 0.2 = 100,000원</code>
        </div>
        <p class="text-caption mt-2">💡 <strong>권장값: 투자 가능 금액에 맞게 설정</strong></p>
      </div>
    </div>
  `,

  rsiPeriod: `
    <div class="help-content">
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="blue" class="mr-2">mdi-lightbulb</v-icon>
          <strong>쉬운 설명</strong>
        </div>
        <p>RSI(상대강도지수)를 계산할 기간입니다. 가격 변동의 강도를 측정하여 과매수/과매도 상태를 판단합니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="orange" class="mr-2">mdi-food-apple</v-icon>
          <strong>비유</strong>
        </div>
        <p>체온계로 열을 재는 것과 같습니다. RSI가 30 이하면 "너무 차가워졌다(과매도)", 70 이상이면 "너무 뜨거워졌다(과매수)"입니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="green" class="mr-2">mdi-chart-line</v-icon>
          <strong>시각적 설명</strong>
        </div>
        <div class="text-center my-2 pa-2 bg-grey-lighten-4 rounded">
          <code>RSI 70 이상: 🔴 과매수 (매도 신호)</code><br>
          <code>RSI 30~70: ⚪ 중립</code><br>
          <code>RSI 30 이하: 🔵 과매도 (매수 신호)</code>
        </div>
        <p class="text-caption mt-2">💡 <strong>권장값: 14일</strong> (표준 설정)</p>
      </div>
    </div>
  `,

  rsiBuyThreshold: `
    <div class="help-content">
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="blue" class="mr-2">mdi-lightbulb</v-icon>
          <strong>쉬운 설명</strong>
        </div>
        <p>RSI가 이 값 이하일 때 매수 신호로 판단합니다. 30이면 RSI가 30 아래로 떨어지면 "과매도 = 매수 기회"입니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="orange" class="mr-2">mdi-food-apple</v-icon>
          <strong>비유</strong>
        </div>
        <p>체온이 36도 이하면 "추우니까 따뜻한 옷을 입자"라고 판단하는 것과 같습니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="green" class="mr-2">mdi-chart-line</v-icon>
          <strong>시각적 설명</strong>
        </div>
        <div class="text-center my-2 pa-2 bg-grey-lighten-4 rounded">
          <code>RSI 25 이하: 확실한 과매도 (보수적)</code><br>
          <code>RSI 30 이하: 일반적 과매도 (권장)</code><br>
          <code>RSI 35 이하: 약한 과매도 (공격적)</code>
        </div>
        <p class="text-caption mt-2">💡 <strong>권장값: 30</strong></p>
      </div>
    </div>
  `,

  rsiSellThreshold: `
    <div class="help-content">
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="blue" class="mr-2">mdi-lightbulb</v-icon>
          <strong>쉬운 설명</strong>
        </div>
        <p>RSI가 이 값 이상일 때 매도 신호로 판단합니다. 70이면 RSI가 70 위로 올라가면 "과매수 = 매도 기회"입니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="orange" class="mr-2">mdi-food-apple</v-icon>
          <strong>비유</strong>
        </div>
        <p>체온이 38도 이상이면 "열이 나니까 해열제를 먹자"라고 판단하는 것과 같습니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="green" class="mr-2">mdi-chart-line</v-icon>
          <strong>시각적 설명</strong>
        </div>
        <div class="text-center my-2 pa-2 bg-grey-lighten-4 rounded">
          <code>RSI 65 이상: 약한 과매수 (공격적)</code><br>
          <code>RSI 70 이상: 일반적 과매수 (권장)</code><br>
          <code>RSI 75 이상: 확실한 과매수 (보수적)</code>
        </div>
        <p class="text-caption mt-2">💡 <strong>권장값: 70</strong></p>
      </div>
    </div>
  `,

  bbPeriod: `
    <div class="help-content">
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="blue" class="mr-2">mdi-lightbulb</v-icon>
          <strong>쉬운 설명</strong>
        </div>
        <p>볼린저 밴드를 계산할 기간입니다. 가격의 변동성을 측정하여 상단/하단 밴드를 그립니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="orange" class="mr-2">mdi-food-apple</v-icon>
          <strong>비유</strong>
        </div>
        <p>고무줄과 같습니다. 가격이 위아래로 많이 움직이면 고무줄(밴드)이 늘어나고, 조용하면 좁아집니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="green" class="mr-2">mdi-chart-line</v-icon>
          <strong>시각적 설명</strong>
        </div>
        <div class="text-center my-2 pa-2 bg-grey-lighten-4 rounded">
          <code>상단 밴드: ═══════════ (저항선)</code><br>
          <code>중심선(MA): ━━━━━━━━━━━ (평균)</code><br>
          <code>하단 밴드: ═══════════ (지지선)</code><br>
          <code></code><br>
          <code>하단 터치 → 매수 신호 🔵</code><br>
          <code>상단 터치 → 매도 신호 🔴</code>
        </div>
        <p class="text-caption mt-2">💡 <strong>권장값: 20일</strong></p>
      </div>
    </div>
  `,

  bbMultiplier: `
    <div class="help-content">
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="blue" class="mr-2">mdi-lightbulb</v-icon>
          <strong>쉬운 설명</strong>
        </div>
        <p>볼린저 밴드의 폭을 결정하는 표준편차 승수입니다. 높을수록 밴드가 넓어지고 신호가 줄어듭니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="orange" class="mr-2">mdi-food-apple</v-icon>
          <strong>비유</strong>
        </div>
        <p>허용 범위를 정하는 것과 같습니다. 2배면 "보통 범위", 3배면 "넓은 범위"로 판단 기준이 달라집니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="green" class="mr-2">mdi-chart-line</v-icon>
          <strong>시각적 설명</strong>
        </div>
        <div class="text-center my-2 pa-2 bg-grey-lighten-4 rounded">
          <code>1.5배: ┃━━━━━━┃ 좁은 밴드 (신호 많음)</code><br>
          <code>2.0배: ┃━━━━━━━━┃ 표준 (권장)</code><br>
          <code>2.5배: ┃━━━━━━━━━━┃ 넓은 밴드 (신호 적음)</code>
        </div>
        <p class="text-caption mt-2">💡 <strong>권장값: 2배</strong></p>
      </div>
    </div>
  `,

  volumeThreshold: `
    <div class="help-content">
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="blue" class="mr-2">mdi-lightbulb</v-icon>
          <strong>쉬운 설명</strong>
        </div>
        <p>평균 거래량 대비 비율입니다. 150%면 평소보다 1.5배 거래량일 때 "거래량 급증"으로 판단합니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="orange" class="mr-2">mdi-food-apple</v-icon>
          <strong>비유</strong>
        </div>
        <p>평소 10명이 사던 가게에 15명이 몰리면 "뭔가 있다!"고 판단하는 것과 같습니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="green" class="mr-2">mdi-chart-line</v-icon>
          <strong>시각적 설명</strong>
        </div>
        <div class="text-center my-2 pa-2 bg-grey-lighten-4 rounded">
          <code>평균 거래량: ████████ 100%</code><br>
          <code>120% 기준: ██████████ (민감)</code><br>
          <code>150% 기준: ████████████ (권장)</code><br>
          <code>200% 기준: ████████████████ (보수적)</code>
        </div>
        <p class="text-caption mt-2">💡 <strong>권장값: 150%</strong></p>
      </div>
    </div>
  `,

  dailyTradeLimitPct: `
    <div class="help-content">
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="blue" class="mr-2">mdi-lightbulb</v-icon>
          <strong>쉬운 설명</strong>
        </div>
        <p>일일 기준 금액 대비 실제로 거래할 수 있는 비율입니다. 20%면 기준 금액의 20%까지만 하루에 매수합니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="orange" class="mr-2">mdi-food-apple</v-icon>
          <strong>비유</strong>
        </div>
        <p>월급 100만원 중 하루에 20%인 20만원까지만 쓴다고 정하는 것과 같습니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="green" class="mr-2">mdi-chart-line</v-icon>
          <strong>시각적 설명</strong>
        </div>
        <div class="text-center my-2 pa-2 bg-grey-lighten-4 rounded">
          <code>기준 금액 100만원</code><br>
          <code>────────────────────</code><br>
          <code>10%: 하루 최대 10만원 (보수적)</code><br>
          <code>20%: 하루 최대 20만원 (권장)</code><br>
          <code>30%: 하루 최대 30만원 (공격적)</code>
        </div>
        <p class="text-caption mt-2">💡 <strong>권장값: 20%</strong></p>
      </div>
    </div>
  `,

  maxPositionPct: `
    <div class="help-content">
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="blue" class="mr-2">mdi-lightbulb</v-icon>
          <strong>쉬운 설명</strong>
        </div>
        <p>한 코인에 최대 투자할 수 있는 비율입니다. 25%면 전체 투자금의 1/4까지만 한 코인에 투자합니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="orange" class="mr-2">mdi-food-apple</v-icon>
          <strong>비유</strong>
        </div>
        <p>"계란을 한 바구니에 담지 마라"는 격언처럼, 한 곳에 너무 많이 투자하지 않도록 제한합니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="green" class="mr-2">mdi-chart-line</v-icon>
          <strong>시각적 설명</strong>
        </div>
        <div class="text-center my-2 pa-2 bg-grey-lighten-4 rounded">
          <code>전체 투자금: 1,000만원</code><br>
          <code>────────────────────</code><br>
          <code>BTC 최대: 250만원 (25%)</code><br>
          <code>ETH 최대: 250만원 (25%)</code><br>
          <code>기타 분산...</code>
        </div>
        <p class="text-caption mt-2">💡 <strong>권장값: 25%</strong></p>
      </div>
    </div>
  `,

  dailyStopLossPct: `
    <div class="help-content">
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="blue" class="mr-2">mdi-lightbulb</v-icon>
          <strong>쉬운 설명</strong>
        </div>
        <p>당일 누적 손실이 이 비율에 도달하면 그날은 더 이상 거래하지 않습니다. 긴급 정지 기능입니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="orange" class="mr-2">mdi-food-apple</v-icon>
          <strong>비유</strong>
        </div>
        <p>카지노에서 "오늘 5만원 이상 잃으면 집에 간다"고 정하는 것과 같습니다. 큰 손실을 막는 안전장치입니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="green" class="mr-2">mdi-chart-line</v-icon>
          <strong>시각적 설명</strong>
        </div>
        <div class="text-center my-2 pa-2 bg-grey-lighten-4 rounded">
          <code>기준 금액: 100만원</code><br>
          <code>────────────────────</code><br>
          <code>-3%: 3만원 손실 시 정지 (타이트)</code><br>
          <code>-5%: 5만원 손실 시 정지 (권장)</code><br>
          <code>-10%: 10만원 손실 시 정지 (여유)</code>
        </div>
        <p class="text-caption mt-2">💡 <strong>권장값: -5%</strong></p>
      </div>
    </div>
  `,

  marketTrendFilter: `
    <div class="help-content">
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="blue" class="mr-2">mdi-lightbulb</v-icon>
          <strong>쉬운 설명</strong>
        </div>
        <p>BTC가 20일 이동평균선 아래에 있으면 전체 매수를 중단합니다. 시장이 하락 추세일 때 매수를 막습니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="orange" class="mr-2">mdi-food-apple</v-icon>
          <strong>비유</strong>
        </div>
        <p>비가 올 때는 우산을 들고, 날씨가 좋을 때만 외출하는 것과 같습니다. 시장 날씨를 보고 행동합니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="green" class="mr-2">mdi-chart-line</v-icon>
          <strong>시각적 설명</strong>
        </div>
        <div class="text-center my-2 pa-2 bg-grey-lighten-4 rounded">
          <code>BTC가 20일선 위 📈: 매수 허용 ✅</code><br>
          <code>BTC가 20일선 아래 📉: 매수 중단 🛑</code>
        </div>
        <p class="text-caption mt-2">⚠️ <strong>권장: OFF</strong> (상승장 수익 100% 유지)</p>
      </div>
    </div>
  `,

  cumulativeLossLimit: `
    <div class="help-content">
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="blue" class="mr-2">mdi-lightbulb</v-icon>
          <strong>쉬운 설명</strong>
        </div>
        <p>초기 투자금 대비 누적 손실이 이 비율에 도달하면 전체 거래를 중단합니다. 급락장 보호 기능입니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="orange" class="mr-2">mdi-food-apple</v-icon>
          <strong>비유</strong>
        </div>
        <p>원금 100만원으로 시작해서 90만원(-10%)이 되면 "여기서 멈추자"라고 결정하는 것과 같습니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="green" class="mr-2">mdi-chart-line</v-icon>
          <strong>시각적 설명</strong>
        </div>
        <div class="text-center my-2 pa-2 bg-grey-lighten-4 rounded">
          <code>초기 자본: 100만원</code><br>
          <code>────────────────────</code><br>
          <code>-10% 도달 (90만원): 🛑 거래 중단</code><br>
          <code>급락장 손실 55% 감소 효과!</code>
        </div>
        <p class="text-caption mt-2">💡 <strong>권장값: -10%</strong> (최적화 검증 완료)</p>
      </div>
    </div>
  `,

  consecutiveStopLoss: `
    <div class="help-content">
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="blue" class="mr-2">mdi-lightbulb</v-icon>
          <strong>쉬운 설명</strong>
        </div>
        <p>같은 코인에서 연속으로 손절이 발생하면 해당 코인의 매수를 24시간 금지합니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="orange" class="mr-2">mdi-food-apple</v-icon>
          <strong>비유</strong>
        </div>
        <p>같은 가게에서 3번 연속 상한 음식을 샀다면 "이 가게는 당분간 안 가야지"라고 결정하는 것과 같습니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="green" class="mr-2">mdi-chart-line</v-icon>
          <strong>시각적 설명</strong>
        </div>
        <div class="text-center my-2 pa-2 bg-grey-lighten-4 rounded">
          <code>BTC 1차 매수 → 손절 ❌</code><br>
          <code>BTC 2차 매수 → 손절 ❌</code><br>
          <code>BTC 3차 매수 → 손절 ❌</code><br>
          <code>────────────────────</code><br>
          <code>BTC 24시간 매수 금지 🚫</code>
        </div>
        <p class="text-caption mt-2">💡 <strong>권장값: 3회</strong></p>
      </div>
    </div>
  `,

  aiNewsAnalysis: `
    <div class="help-content">
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="blue" class="mr-2">mdi-lightbulb</v-icon>
          <strong>쉬운 설명</strong>
        </div>
        <p>AI가 코인 관련 뉴스를 분석하여 호재/악재를 판단하고, 매수 조건을 자동으로 조정합니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="orange" class="mr-2">mdi-food-apple</v-icon>
          <strong>비유</strong>
        </div>
        <p>날씨 예보를 보고 우산을 준비하는 것과 같습니다. 좋은 뉴스가 많으면 적극적으로, 나쁜 뉴스가 많으면 신중하게 행동합니다.</p>
      </div>
      <v-divider class="my-3"></v-divider>
      <div class="help-section">
        <div class="d-flex align-center mb-2">
          <v-icon color="green" class="mr-2">mdi-chart-line</v-icon>
          <strong>시각적 설명</strong>
        </div>
        <div class="text-center my-2 pa-2 bg-grey-lighten-4 rounded">
          <code>호재 뉴스 📰➡️ 매수 조건 완화 (+0.5%)</code><br>
          <code>악재 뉴스 📰➡️ 매수 조건 강화 (-0.5%)</code><br>
          <code>────────────────────</code><br>
          <code>매일 00:00 KST 가중치 초기화</code>
        </div>
        <p class="text-caption mt-2">💡 3시간마다 자동 분석 (0, 3, 6, 9, 12, 15, 18, 21시)</p>
      </div>
    </div>
  `
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