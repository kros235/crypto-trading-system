<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main>
      <v-container fluid class="pa-4">
        <!-- 페이지 제목 (변경 없음) -->
        <div class="d-flex align-center mb-4">
          <v-icon size="28" color="blue" class="mr-2">mdi-chart-line</v-icon>
          <h2 class="text-h5 font-weight-bold">주식 거래 설정</h2>
          <v-chip color="blue" size="small" class="ml-2">Phase 2</v-chip>
          <HelpButton
            :useDialog="true"
            :dialogTitle="helpContents.phaseCompare.title"
            :dialogContent="helpContents.phaseCompare.content"
            color="grey-darken-1"
            class="ml-2"
          />
        </div>
        <p class="text-subtitle-1 text-grey mb-4">주식/ETF 자동매매에 필요한 매매 조건과 리스크 관리를 설정합니다</p>

        <!-- 알림 메시지 (변경 없음) -->
        <v-alert
          v-if="message"
          :type="messageType"
          density="compact"
          closable
          class="mb-4"
          @click:close="message = ''"
        >
          {{ message }}
        </v-alert>

        <v-form ref="formRef" v-model="valid">

          <!-- 1. 거래 종목 선택 (변경 없음) -->
          <v-card class="mb-4" elevation="2">
            <v-card-title class="d-flex align-center">
              <v-icon class="mr-2" color="blue">mdi-format-list-bulleted</v-icon>
              거래 종목 선택
              <v-spacer />
              <HelpButton
                :useDialog="true"
                :dialogTitle="helpContents.stockSelect.title"
                :dialogContent="helpContents.stockSelect.content"
                color="grey-darken-1"
              />
            </v-card-title>
            <v-card-text>
              <v-text-field
                v-model="stockSearchKeyword"
                label="종목코드 입력 (6자리 숫자, 예: 409820)"
                placeholder="409820"
                prepend-inner-icon="mdi-magnify"
                hint="TIGER 나스닥100레버리지: 409820, KODEX 나스닥100레버리지: 409810"
                persistent-hint
                density="compact"
                class="mb-2"
                @keyup.enter="addStockByCode"
              >
                <template v-slot:append>
                  <v-btn
                    color="blue"
                    size="small"
                    variant="tonal"
                    :loading="searchLoading"
                    @click="addStockByCode"
                  >
                    추가
                  </v-btn>
                </template>
              </v-text-field>

              <div class="d-flex flex-wrap gap-2 mb-3">
                <v-chip
                  v-for="preset in presetStocks"
                  :key="preset.code"
                  size="small"
                  :color="settings.stockCodes.includes(preset.code) ? 'blue' : 'grey'"
                  :variant="settings.stockCodes.includes(preset.code) ? 'flat' : 'outlined'"
                  @click="togglePresetStock(preset.code)"
                >
                  {{ preset.name }}
                </v-chip>
              </div>

              <v-alert v-if="settings.stockCodes.length === 0" type="warning" density="compact" class="mb-2">
                거래할 종목을 1개 이상 추가해주세요.
              </v-alert>
              <v-chip-group v-else>
                <v-chip
                  v-for="code in settings.stockCodes"
                  :key="code"
                  closable
                  color="blue"
                  variant="outlined"
                  size="small"
                  @click:close="removeStock(code)"
                >
                  {{ getStockDisplayName(code) }}
                </v-chip>
              </v-chip-group>

              <v-expansion-panels class="mt-3" variant="accordion">
                <v-expansion-panel>
                  <v-expansion-panel-title>
                    <v-icon size="small" class="mr-1">mdi-information</v-icon>
                    주요 레버리지 ETF 종목 정보
                  </v-expansion-panel-title>
                  <v-expansion-panel-text>
                    <v-table density="compact" class="text-body-2">
                      <thead>
                        <tr>
                          <th>종목코드</th>
                          <th>종목명</th>
                          <th>유형</th>
                          <th>기초지수</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="info in etfReferenceList" :key="info.code">
                          <td>{{ info.code }}</td>
                          <td>{{ info.name }}</td>
                          <td>
                            <v-chip size="x-small" :color="info.type === 'LEVERAGE' ? 'red' : info.type === 'INVERSE' ? 'blue' : 'green'" variant="flat">
                              {{ info.typeLabel }}
                            </v-chip>
                          </td>
                          <td>{{ info.index }}</td>
                        </tr>
                      </tbody>
                    </v-table>
                  </v-expansion-panel-text>
                </v-expansion-panel>
              </v-expansion-panels>
            </v-card-text>
          </v-card>

          <v-card class="mb-4" elevation="2">
            <v-card-title class="d-flex align-center">
              <v-icon class="mr-2" color="purple">mdi-chart-areaspline</v-icon>
              기술적 지표 설정
              <!-- ⭐ 삭제: 기존 카드 단위 HelpButton(technicalIndicator) 제거 -->
            </v-card-title>
            <v-card-text>
              <!-- ⭐ 수정: label 속성 제거 → 별도 라벨+도움말 div 추가 -->
              <!-- 변경 이유: label 속성 사용 시 도움말 버튼을 옆에 배치할 수 없음 -->
              <div class="d-flex align-center mb-1">
                <span class="text-subtitle-2 text-grey">이동평균선 기간 (일)</span>
                <HelpButton
                  :useDialog="true"
                  :dialogTitle="helpContents.maSettings.title"
                  :dialogContent="helpContents.maSettings.content"
                  size="x-small"
                  color="grey"
                />
              </div>
              <v-radio-group
                v-model="settings.basePeriod"
                inline
                hide-details
                class="mb-3"
              >
                <v-radio label="7일 이동평균" :value="7" />
                <v-radio label="14일 이동평균" :value="14" />
                <v-radio label="20일 이동평균" :value="20" />
                <v-radio label="30일 이동평균" :value="30" />
              </v-radio-group>
              <p class="text-caption text-grey mb-2">기준가 산정을 위한 이동평균선 기간을 선택하세요</p>

              <!-- ⭐ 수정: RSI 2열 → 3열 + 개별 도움말 -->
              <!-- 변경 이유: RSI 기간/매수/매도를 한눈에 보기 위해 3열, 개별 도움말로 SVG 차트 제공 -->
              <div class="d-flex align-center mb-1 mt-2">
                <span class="text-subtitle-2 text-grey">RSI (상대강도지수)</span>
                <HelpButton
                  :useDialog="true"
                  :dialogTitle="helpContents.rsiSettings.title"
                  :dialogContent="helpContents.rsiSettings.content"
                  size="x-small"
                  color="grey"
                />
              </div>
              <v-row dense>
                <v-col cols="4">
                  <v-text-field
                    v-model.number="settings.rsiPeriod"
                    label="RSI 기간"
                    type="number"
                    density="compact"
                    hint="기본: 14일"
                    persistent-hint
                  />
                </v-col>
                <v-col cols="4">
                  <v-text-field
                    v-model.number="settings.rsiBuyThreshold"
                    label="매수 신호 (이하)"
                    type="number"
                    density="compact"
                    hint="기본: 35"
                    persistent-hint
                  />
                </v-col>
                <v-col cols="4">
                  <v-text-field
                    v-model.number="settings.rsiSellThreshold"
                    label="매도 신호 (이상)"
                    type="number"
                    density="compact"
                    hint="기본: 65"
                    persistent-hint
                  />
                </v-col>
              </v-row>

              <!-- ⭐ 수정: 볼린저밴드 - 개별 도움말 추가 -->
              <!-- 변경 이유: 볼린저밴드 SVG 차트(도로 비유)를 코인 설정에서 그대로 이식 -->
              <div class="d-flex align-center mb-1 mt-2">
                <span class="text-subtitle-2 text-grey">볼린저 밴드 (BB)</span>
                <HelpButton
                  :useDialog="true"
                  :dialogTitle="helpContents.bbSettings.title"
                  :dialogContent="helpContents.bbSettings.content"
                  size="x-small"
                  color="grey"
                />
              </div>
              <v-row dense>
                <v-col cols="6">
                  <v-text-field
                    v-model.number="settings.bbPeriod"
                    label="볼린저밴드 기간"
                    type="number"
                    density="compact"
                    hint="기본: 20일"
                    persistent-hint
                  />
                </v-col>
                <v-col cols="6">
                  <v-text-field
                    v-model.number="settings.bbMultiplier"
                    label="볼린저밴드 승수"
                    type="number"
                    density="compact"
                    hint="기본: 2"
                    persistent-hint
                  />
                </v-col>
              </v-row>

              <!-- ⭐ 수정: 거래량 - 개별 도움말 추가 -->
              <!-- 변경 이유: 거래량 급증 기준(120%)에 대한 가게 비유 설명을 개별 도움말로 제공 -->
              <div class="d-flex align-center mb-1 mt-2">
                <span class="text-subtitle-2 text-grey">거래량 급증 기준</span>
                <HelpButton
                  :useDialog="true"
                  :dialogTitle="helpContents.volumeSettings.title"
                  :dialogContent="helpContents.volumeSettings.content"
                  size="x-small"
                  color="grey"
                />
              </div>
              <v-text-field
                v-model.number="settings.volumeThreshold"
                label="거래량 급증 기준 (%)"
                type="number"
                density="compact"
                hint="기본: 120%"
                persistent-hint
              />
            </v-card-text>
          </v-card>

          <v-card class="mb-4" elevation="2">
            <v-card-title class="d-flex align-center">
              <v-icon class="mr-2" color="green">mdi-swap-vertical</v-icon>
              매수/매도 조건
              <!-- ⭐ 삭제: 기존 카드 단위 HelpButton(buyCondition) 제거 -->
            </v-card-title>
            <v-card-text>
              <!-- ⭐ 수정: 매수 기준 - 개별 도움말 추가 -->
              <div class="d-flex align-center mb-1">
                <span class="text-subtitle-2 text-grey">매수 기준 하락률 (%)</span>
                <HelpButton
                  :useDialog="true"
                  :dialogTitle="helpContents.buyThreshold.title"
                  :dialogContent="helpContents.buyThreshold.content"
                  size="x-small"
                  color="grey"
                />
              </div>
              <v-text-field
                v-model.number="settings.buyThresholdPct"
                type="number"
                density="compact"
                :rules="[rules.required, rules.negative]"
                hint="기본: -3.0% (이동평균선 대비)"
                persistent-hint
                class="mb-2"
              />

              <!-- ⭐ 수정: 목표 수익률 - 개별 도움말 추가 -->
              <div class="d-flex align-center mb-1">
                <span class="text-subtitle-2 text-grey">목표 수익률 (%)</span>
                <HelpButton
                  :useDialog="true"
                  :dialogTitle="helpContents.sellTarget.title"
                  :dialogContent="helpContents.sellTarget.content"
                  size="x-small"
                  color="grey"
                />
              </div>
              <v-text-field
                v-model.number="settings.sellTargetPct"
                type="number"
                density="compact"
                :rules="[rules.required, rules.positive]"
                hint="기본: 2.5%"
                persistent-hint
                class="mb-2"
              />

              <!-- 손절매 ON/OFF + 기준값 (코인 설정과 동일 구조) -->
              <v-row class="mt-1 mb-2" no-gutters>
                <v-col cols="12" md="4">
                  <div class="d-flex align-center">
                    <v-switch
                      v-model="settings.useStopLoss"
                      label="손절매 사용"
                      color="error"
                      hide-details
                      density="compact"
                    />
                    <HelpButton
                      :useDialog="true"
                      :dialogTitle="helpContents.useStopLoss.title"
                      :dialogContent="helpContents.useStopLoss.content"
                      size="x-small"
                      color="grey"
                    />
                  </div>
                  <v-alert
                    v-if="!settings.useStopLoss"
                    type="error"
                    variant="tonal"
                    density="compact"
                    class="mt-2"
                    style="font-size: 12px;"
                  >
                    ⚠️ 손절매 기능을 끄면 하락장에서 큰 손실이 발생할 수 있습니다!
                  </v-alert>
                </v-col>
                <v-col cols="12" md="4" class="pl-md-3">
                  <div class="d-flex align-center mb-1">
                    <span class="text-subtitle-2 text-grey">손절매 기준 (%)</span>
                    <HelpButton
                      :useDialog="true"
                      :dialogTitle="helpContents.stopLoss.title"
                      :dialogContent="helpContents.stopLoss.content"
                      size="x-small"
                      color="grey"
                    />
                  </div>
                  <v-text-field
                    v-model.number="settings.stopLossPct"
                    type="number"
                    density="compact"
                    :rules="settings.useStopLoss ? [rules.required, rules.negative] : []"
                    hint="기본: -5.0%"
                    persistent-hint
                    :disabled="!settings.useStopLoss"
                  />
                </v-col>
              </v-row>

              <!-- ⭐ 수정: 트레일링 스톱 - 개별 도움말 추가 -->
              <div class="d-flex align-center mb-1">
                <span class="text-subtitle-2 text-grey">트레일링 스톱</span>
                <HelpButton
                  :useDialog="true"
                  :dialogTitle="helpContents.trailingStop.title"
                  :dialogContent="helpContents.trailingStop.content"
                  size="x-small"
                  color="grey"
                />
              </div>
              <v-switch
                v-model="settings.useTrailingStop"
                label="트레일링 스톱 사용"
                color="green"
                density="compact"
                hide-details
                class="mb-2"
              />
              <v-text-field
                v-if="settings.useTrailingStop"
                v-model.number="settings.trailingStopPct"
                label="트레일링 스톱 비율 (%)"
                type="number"
                density="compact"
                hint="최고가 대비 하락 시 매도 (기본: -2.5%)"
                persistent-hint
                class="mb-2"
              />

              <!-- ⭐ 수정: 보유 제한 - 개별 도움말 추가 (최대 보유일수 설명 포함) -->
              <!-- 변경 이유: Phase 2 전용 '최대 보유일수'에 대한 변동성 끌림 설명이 필요 -->
              <div class="d-flex align-center mb-1 mt-2">
                <span class="text-subtitle-2 text-grey">보유 제한</span>
                <HelpButton
                  :useDialog="true"
                  :dialogTitle="helpContents.holdingLimit.title"
                  :dialogContent="helpContents.holdingLimit.content"
                  size="x-small"
                  color="grey"
                />
              </div>
              <v-row dense>
                <v-col cols="6">
                  <v-text-field
                    v-model.number="settings.maxHoldingsPerStock"
                    label="종목당 최대 보유 건수"
                    type="number"
                    density="compact"
                    :rules="[rules.required, rules.minOne]"
                    hint="기본: 3건"
                    persistent-hint
                  />
                </v-col>
                <v-col cols="6">
                  <v-text-field
                    v-model.number="settings.maxHoldingDays"
                    label="최대 보유일수"
                    type="number"
                    density="compact"
                    hint="레버리지 decay 방지 (기본: 20일)"
                    persistent-hint
                  />
                </v-col>
              </v-row>
              <!-- ⭐ [신규] 추가 매수 하락률 (코인 설정과 동일) -->
              <div class="d-flex align-center mb-1 mt-3">
                <span class="text-subtitle-2 text-grey">추가 매수 하락률 (%)</span>
                <HelpButton
                  :useDialog="true"
                  :dialogTitle="helpContents.additionalDropPct.title"
                  :dialogContent="helpContents.additionalDropPct.content"
                  size="x-small"
                  color="grey"
                />
              </div>
              <v-text-field
                v-model.number="settings.additionalDropPct"
                type="number"
                density="compact"
                :rules="[v => v > 0 || '양수 입력']"
                hint="이전 매수가 대비 이 값 이상 하락 시 추가 매수 허용 (기본: 1.0%)"
                persistent-hint
                min="0.1"
                max="10"
                step="0.1"
              />
            </v-card-text>
          </v-card>

          <!-- 4. 투자금 및 매수 방식 (변경 없음) -->
          <v-card class="mb-4" elevation="2">
            <v-card-title class="d-flex align-center">
              <v-icon class="mr-2" color="teal">mdi-cash-multiple</v-icon>
              투자금 및 매수 방식
              <v-spacer />
              <HelpButton
                :useDialog="true"
                :dialogTitle="helpContents.investment.title"
                :dialogContent="helpContents.investment.content"
                color="grey-darken-1"
              />
            </v-card-title>
            <v-card-text>
              <v-text-field
                v-model.number="settings.dailyLimitAmount"
                label="일일 거래 한도 (원)"
                type="number"
                density="compact"
                hint="기본: 1,000,000원"
                persistent-hint
                class="mb-2"
              />
              <v-text-field
                v-model.number="settings.fixedBuyAmount"
                label="1회 고정 매수 금액 (원)"
                type="number"
                density="compact"
                hint="기본: 100,000원"
                persistent-hint
                class="mb-2"
              />
              <v-switch
                v-model="settings.useRoundRobin"
                label="라운드로빈 매수 방식"
                color="teal"
                density="compact"
                class="mb-2"
              >
                <template v-slot:append>
                  <v-chip size="x-small" :color="settings.useRoundRobin ? 'teal' : 'grey'" variant="flat">
                    {{ settings.useRoundRobin ? '라운드로빈' : '고정금액' }}
                  </v-chip>
                </template>
              </v-switch>
              <v-switch
                v-model="settings.useDailyLimitRecovery"
                label="일일 한도 회복 사용"
                color="teal"
                density="compact"
                hide-details
              />
            </v-card-text>
          </v-card>

          <v-card class="mb-4" elevation="2">
            <v-card-title class="d-flex align-center">
              <v-icon class="mr-2" color="red">mdi-shield-alert</v-icon>
              리스크 관리
              <!-- ⭐ 삭제: 기존 카드 단위 HelpButton(riskManagement) 제거 -->
            </v-card-title>
            <v-card-text>
              <!-- ⭐ 수정: 일일 거래 한도/종목 비중 - 개별 도움말 추가 -->
              <div class="d-flex align-center mb-1">
                <span class="text-subtitle-2 text-grey">일일 거래 한도 / 종목 비중</span>
                <HelpButton
                  :useDialog="true"
                  :dialogTitle="helpContents.dailyTradeLimit.title"
                  :dialogContent="helpContents.dailyTradeLimit.content"
                  size="x-small"
                  color="grey"
                />
              </div>
              <v-row dense>
                <v-col cols="6">
                  <v-text-field
                    v-model.number="settings.dailyTradeLimitPct"
                    label="일일 거래 한도 (%)"
                    type="number"
                    density="compact"
                    hint="기본: 20%"
                    persistent-hint
                  />
                </v-col>
                <v-col cols="6">
                  <v-text-field
                    v-model.number="settings.maxPositionPct"
                    label="단일 종목 최대 비중 (%)"
                    type="number"
                    density="compact"
                    hint="기본: 25%"
                    persistent-hint
                  />
                </v-col>
              </v-row>

              <!-- ⭐ 수정: 긴급 정지 - 개별 도움말 추가 -->
              <div class="d-flex align-center mb-1 mt-2">
                <span class="text-subtitle-2 text-grey">긴급 정지</span>
                <HelpButton
                  :useDialog="true"
                  :dialogTitle="helpContents.dailyStopLoss.title"
                  :dialogContent="helpContents.dailyStopLoss.content"
                  size="x-small"
                  color="grey"
                />
              </div>
              <v-text-field
                v-model.number="settings.dailyStopLossPct"
                label="긴급 정지 조건 (%)"
                type="number"
                density="compact"
                hint="기본: -5% (일일 누적 손실 시 거래 중단)"
                persistent-hint
                class="mb-2"
              />

              <!-- ⭐ 수정: 급락장 보호 - 개별 도움말 추가 -->
              <div class="d-flex align-center mb-1 mt-2">
                <span class="text-subtitle-2 text-grey">급락장 보호 기능</span>
                <HelpButton
                  :useDialog="true"
                  :dialogTitle="helpContents.crashProtection.title"
                  :dialogContent="helpContents.crashProtection.content"
                  size="x-small"
                  color="grey"
                />
              </div>
              <v-switch
                v-model="settings.useMarketTrendFilter"
                label="시장 추세 필터 사용"
                color="red"
                density="compact"
                hide-details
                class="mb-2"
              />
              <v-row dense>
                <v-col cols="6">
                  <v-text-field
                    v-model.number="settings.cumulativeLossLimitPct"
                    label="누적 손실 한도 (%)"
                    type="number"
                    density="compact"
                    hint="기본: -10%"
                    persistent-hint
                  />
                </v-col>
                <v-col cols="6">
                  <v-text-field
                    v-model.number="settings.consecutiveStopLossLimit"
                    label="연속 손절 제한 (회)"
                    type="number"
                    density="compact"
                    hint="기본: 3회"
                    persistent-hint
                    :disabled="!settings.useStopLoss"
                  />
                </v-col>
              </v-row>
              <!-- ⭐ 손절매 OFF 시 안내 메시지 -->
              <v-alert
                v-if="!settings.useStopLoss"
                type="info"
                variant="tonal"
                density="compact"
                class="mt-2"
                style="font-size: 12px;"
              >
                손절매 기능이 꺼져 있어 연속 손절 제한이 비활성화되었습니다. 이 기능을 사용하려면 매수/매도 조건에서 손절매를 다시 켜주세요.
              </v-alert>
            </v-card-text>
          </v-card>

          <!-- ⭐ [신규] 추가 옵션: AI 뉴스 분석 (코인 설정과 동일 구조) -->
          <v-card class="mb-4" elevation="2">
            <v-card-title class="d-flex align-center">
              <v-icon class="mr-2" color="purple">mdi-robot</v-icon>
              추가 옵션
            </v-card-title>
            <v-card-text>
              <div class="d-flex align-center">
                <v-switch
                  v-model="settings.useAiAnalysis"
                  label="AI 뉴스 분석 사용"
                  color="purple"
                  hide-details
                  density="compact"
                />
                <HelpButton
                  :useDialog="true"
                  dialogTitle="🤖 AI 뉴스 분석 (주식/ETF)"
                  dialogContent="<p>Groq API (Llama 3.3 70B)를 통해 나스닥100 관련 뉴스를 분석합니다.</p><ul style='padding-left:20px; margin-top:8px;'><li>뉴스 소스: MarketWatch, Yahoo Finance (나스닥/기술주 전문)</li><li>분석 주기: 3시간마다 자동 실행</li><li>가중치 적용: 호재/악재 분석 → 매수 조건 ±0.5% 범위 내 자동 조정</li></ul><p style='margin-top:8px; background:#f3e5f5; padding:8px; border-radius:4px;'>💡 나스닥100 지수를 추종하는 ETF(TIGER 409820, KODEX 409810 등) 투자 시 미국 기술주 관련 뉴스 감성을 매수 조건에 반영합니다.</p>"
                  size="x-small"
                  color="grey"
                />
              </div>
              <p class="text-caption text-grey mt-1">
                Groq API (Llama 3.3 70B)를 통한 나스닥100 관련 뉴스 분석으로 매수 조건 가중치를 자동 조정합니다
              </p>
            </v-card-text>
          </v-card>

          <!-- 하단: 저장/초기화/삭제 버튼 (변경 없음) -->
          <div class="mb-4">
            <div class="py-2">
              <div class="d-flex justify-center gap-3">
                <v-btn
                  color="blue"
                  variant="elevated"
                  size="large"
                  :loading="loading"
                  @click="saveSettings"
                  class="d-flex align-center"
                >
                  <v-icon size="20" class="mr-2">mdi-content-save</v-icon>
                  <span>{{ hasExistingSettings ? '설정 수정' : '설정 저장' }}</span>
                </v-btn>
                <v-btn
                  color="amber-darken-2"
                  variant="elevated"
                  size="large"
                  @click="resetDialog = true"
                  class="d-flex align-center"
                >
                  <v-icon size="20" class="mr-2">mdi-refresh</v-icon>
                  <span>기본값 초기화</span>
                </v-btn>
                <v-btn
                  v-if="hasExistingSettings"
                  color="red"
                  variant="elevated"
                  size="large"
                  @click="confirmDelete"
                  class="d-flex align-center"
                >
                  <v-icon size="20" class="mr-2">mdi-delete</v-icon>
                  <span>설정 삭제</span>
                </v-btn>
              </div>
            </div>
          </div>
        </v-form>

        <!-- 다이얼로그 (변경 없음) -->
        <v-dialog v-model="deleteDialog" max-width="400">
          <v-card>
            <v-card-title class="text-h6">설정 삭제 확인</v-card-title>
            <v-card-text>
              주식 거래 설정을 삭제하시겠습니까?<br>
              삭제 후 기본값으로 자동 생성됩니다.
            </v-card-text>
            <v-card-actions>
              <v-spacer />
              <v-btn color="grey" variant="text" @click="deleteDialog = false">취소</v-btn>
              <v-btn color="red" variant="elevated" :loading="deleteLoading" @click="deleteSettings">삭제</v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>

        <v-dialog v-model="resetDialog" max-width="400">
          <v-card>
            <v-card-title class="text-h6">기본값 초기화</v-card-title>
            <v-card-text>
              모든 설정을 주식/ETF 기본값으로 되돌리시겠습니까?<br>
              초기화 후 저장 버튼을 눌러야 적용됩니다.
            </v-card-text>
            <v-card-actions>
              <v-spacer />
              <v-btn color="grey" variant="text" @click="resetDialog = false">취소</v-btn>
              <v-btn color="warning" variant="elevated" @click="executeReset">초기화</v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>

      </v-container>
    </v-main>
  </v-app>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { stockInfoApi, stockSettingApi } from '@/api/stock'
import type { StockInfo } from '@/types/stock'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'
import HelpButton from '@/components/HelpButton.vue'

const sidebarRef = ref()
const formRef = ref()
const valid = ref(false)
const loading = ref(false)
const deleteLoading = ref(false)
const searchLoading = ref(false)
const message = ref('')
const messageType = ref<'success' | 'error' | 'info' | 'warning'>('success')
const deleteDialog = ref(false)
const resetDialog = ref(false)
const hasExistingSettings = ref(false)
const stockSearchKeyword = ref('')
const registeredStocks = ref<StockInfo[]>([])

// ⭐ 수정: helpContents - 카드 단위 6개 → 지표 단위 16개로 분리
// 변경 이유: 코인 거래 설정과 동일한 포맷(glossary-detail, SVG 차트, 비유, 설정값 테이블) 적용
// 삭제된 키: technicalIndicator, buyCondition, riskManagement (개별 도움말로 대체)
// 신규 키: maSettings, rsiSettings, bbSettings, volumeSettings, buyThreshold, sellTarget,
//          stopLoss, trailingStop, holdingLimit, dailyTradeLimit, dailyStopLoss, crashProtection
const helpContents = {
  // ===== phaseCompare (기존 유지) =====
  phaseCompare: {
    title: '📊 Phase 1(코인) vs Phase 2(주식) 기본값 비교',
    content: `
      <p>주식/ETF는 암호화폐 대비 변동성이 낮아 매매 임계값이 다르게 설정됩니다.</p>
      <table style="width:100%; border-collapse:collapse; margin-top:12px;">
        <thead>
          <tr style="background:#f5f5f5;">
            <th style="border:1px solid #ddd; padding:8px; text-align:left;">항목</th>
            <th style="border:1px solid #ddd; padding:8px; text-align:center;">Phase 1 (코인)</th>
            <th style="border:1px solid #ddd; padding:8px; text-align:center;">Phase 2 (주식)</th>
            <th style="border:1px solid #ddd; padding:8px; text-align:left;">비고</th>
          </tr>
        </thead>
        <tbody>
          <tr><td style="border:1px solid #ddd; padding:8px;">매수 기준 하락률</td><td style="border:1px solid #ddd; padding:8px; text-align:center;">-6.0%</td><td style="border:1px solid #ddd; padding:8px; text-align:center; color:#1976D2; font-weight:bold;">-3.0%</td><td style="border:1px solid #ddd; padding:8px;">변동성 차이</td></tr>
          <tr><td style="border:1px solid #ddd; padding:8px;">목표 수익률</td><td style="border:1px solid #ddd; padding:8px; text-align:center;">+4.0%</td><td style="border:1px solid #ddd; padding:8px; text-align:center; color:#1976D2; font-weight:bold;">+2.5%</td><td style="border:1px solid #ddd; padding:8px;">보수적 설정</td></tr>
          <tr><td style="border:1px solid #ddd; padding:8px;">손절매 기준</td><td style="border:1px solid #ddd; padding:8px; text-align:center;">-8.0%</td><td style="border:1px solid #ddd; padding:8px; text-align:center; color:#1976D2; font-weight:bold;">-5.0%</td><td style="border:1px solid #ddd; padding:8px;">리스크 관리</td></tr>
          <tr><td style="border:1px solid #ddd; padding:8px;">트레일링 스톱</td><td style="border:1px solid #ddd; padding:8px; text-align:center;">-4.0%</td><td style="border:1px solid #ddd; padding:8px; text-align:center; color:#1976D2; font-weight:bold;">-2.5%</td><td style="border:1px solid #ddd; padding:8px;">수익 보존</td></tr>
          <tr><td style="border:1px solid #ddd; padding:8px;">RSI 매수/매도</td><td style="border:1px solid #ddd; padding:8px; text-align:center;">32 / 68</td><td style="border:1px solid #ddd; padding:8px; text-align:center; color:#1976D2; font-weight:bold;">35 / 65</td><td style="border:1px solid #ddd; padding:8px;">과매도/과매수 완화</td></tr>
          <tr><td style="border:1px solid #ddd; padding:8px;">거래량 급증 기준</td><td style="border:1px solid #ddd; padding:8px; text-align:center;">140%</td><td style="border:1px solid #ddd; padding:8px; text-align:center; color:#1976D2; font-weight:bold;">120%</td><td style="border:1px solid #ddd; padding:8px;">ETF 특성</td></tr>
          <tr><td style="border:1px solid #ddd; padding:8px;">최대 보유일수</td><td style="border:1px solid #ddd; padding:8px; text-align:center;">없음</td><td style="border:1px solid #ddd; padding:8px; text-align:center; color:#1976D2; font-weight:bold;">20일</td><td style="border:1px solid #ddd; padding:8px;">레버리지 decay 방지</td></tr>
        </tbody>
      </table>
      <p style="margin-top:12px;">⚠️ <strong>레버리지 ETF 주의</strong>: 장기 보유 시 복리 효과로 인한 가치 침식(decay)이 발생할 수 있으므로 최대 20거래일 내 청산을 권장합니다.</p>
    `
  },

  // ===== stockSelect (기존 유지) =====
  stockSelect: {
    title: '📋 거래 종목 선택 안내',
    content: `
      <p>자동매매할 주식/ETF 종목을 선택합니다.</p>
      <p style="margin-top:8px;"><strong>종목 추가 방법:</strong></p>
      <ul style="padding-left:20px; margin-top:4px;">
        <li>종목코드(6자리 숫자)를 직접 입력하여 추가</li>
        <li>빠른 추가 버튼(프리셋)으로 주요 레버리지 ETF 추가</li>
      </ul>
      <p style="margin-top:12px;"><strong>주요 레버리지 ETF:</strong></p>
      <ul style="padding-left:20px; margin-top:4px;">
        <li><strong>TIGER 나스닥100레버리지 (409820)</strong> - 나스닥100 x2, 환노출</li>
        <li><strong>KODEX 나스닥100레버리지 (409810)</strong> - 나스닥100 x2, 환헤지</li>
        <li><strong>KODEX 코스닥150레버리지 (233740)</strong> - 코스닥150 x2</li>
      </ul>
      <p style="margin-top:12px;">💡 <strong>환노출형 vs 환헤지형</strong>: 환노출형은 원달러 환율 변동에 영향을 받고, 환헤지형은 환율 변동 영향을 최소화합니다. 환율 상승기에는 환노출형이, 환율 하락기에는 환헤지형이 유리합니다.</p>
    `
  },

  // ===== ⭐ 신규: 이동평균선 (코인 거래 설정에서 이식 + 주식 예시로 변경) =====
  maSettings: {
    title: '📈 이동평균선 (MA)',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"최근 며칠간의 평균 가격을 선으로 연결한 것. 현재 가격이 비싼지 싼지 판단하는 기준선"</p>
          </div>
        </div>
        <div class="help-example-card mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🛒 마트 비유</span>
          </div>
          <div style="padding-left: 24px; background: #FFF8E1; padding: 16px; border-radius: 8px; border-left: 4px solid #FFA000;">
            삼성전자 주가가 20일 동안 평균 7만원이었어요.<br/>
            오늘 가격이 6만8천원이면? → "평균보다 싸네! 매수 기회?"<br/>
            오늘 가격이 7만2천원이면? → "평균보다 비싸네! 좀 더 지켜볼까?"
          </div>
        </div>
        <div class="help-diagram mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">📊 시각적 설명</span>
          </div>
          <svg width="100%" height="160" viewBox="0 0 500 160" style="background: #FAFAFA; border-radius: 8px; border: 1px solid #E0E0E0;">
            <line x1="30" y1="80" x2="470" y2="80" stroke="#1976D2" stroke-width="2.5"/>
            <text x="380" y="75" font-size="12" fill="#1976D2" font-weight="bold">── 20일 이동평균선</text>
            <polyline points="40,90 80,85 120,70 160,75 200,95 240,110 280,100 320,85 360,70 400,65 440,80" fill="none" stroke="#FF9800" stroke-width="2"/>
            <text x="380" y="55" font-size="12" fill="#FF9800">── 실제 ETF 가격</text>
            <circle cx="240" cy="110" r="8" fill="#43a047"/>
            <text x="200" y="140" font-size="11" fill="#43a047" font-weight="bold">매수 신호! (평균 아래)</text>
            <circle cx="360" cy="70" r="8" fill="#e53935"/>
            <text x="310" y="55" font-size="11" fill="#e53935" font-weight="bold">매도 신호! (평균 위)</text>
          </svg>
        </div>
        <div class="mb-2">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">📋 설정값 안내</span>
          </div>
          <div style="padding-left: 24px;">
            <table style="width: 100%; border-collapse: collapse; border: 1px solid #E0E0E0; font-size: 13px;">
              <thead><tr style="background-color: #ECEFF1;"><th style="padding: 10px 12px; text-align: left; border-bottom: 1px solid #E0E0E0;">설정값</th><th style="padding: 10px 12px; text-align: left; border-bottom: 1px solid #E0E0E0;">의미</th><th style="padding: 10px 12px; text-align: left; border-bottom: 1px solid #E0E0E0;">적합한 사람</th></tr></thead>
              <tbody>
                <tr><td style="padding: 8px 12px; border-bottom: 1px solid #EEE;">7~10일</td><td style="padding: 8px 12px; border-bottom: 1px solid #EEE;">단기 추세</td><td style="padding: 8px 12px; border-bottom: 1px solid #EEE;">자주 거래하고 싶은 사람</td></tr>
                <tr style="background-color: #E3F2FD;"><td style="padding: 8px 12px; border-bottom: 1px solid #EEE; color: #1565C0;"><strong>20일</strong></td><td style="padding: 8px 12px; border-bottom: 1px solid #EEE; color: #1565C0;">중기 추세</td><td style="padding: 8px 12px; border-bottom: 1px solid #EEE; color: #1565C0;">초보자 추천 ✅</td></tr>
                <tr><td style="padding: 8px 12px;">30일</td><td style="padding: 8px 12px;">장기 추세</td><td style="padding: 8px 12px;">느긋한 투자자</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `
  },

  // ===== ⭐ 신규: RSI (코인 거래 설정에서 이식, 주식 기본값 35/65 반영) =====
  rsiSettings: {
    title: '🌡️ RSI (상대강도지수)',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"지금 이 종목이 너무 많이 올랐나? 너무 많이 떨어졌나? 0~100으로 알려주는 온도계"</p>
          </div>
        </div>
        <div class="help-example-card mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🌡️ 체온 비유</span>
          </div>
          <div style="padding-left: 24px; background: #FFF8E1; padding: 16px; border-radius: 8px; border-left: 4px solid #FFA000;">
            사람 체온이 36.5도가 정상이듯, RSI도 50이 중립이에요.<br/><br/>
            🟢 RSI ≤ 35: "과매도! 너무 떨어졌으니 반등할 수 있다!" → 매수 신호<br/>
            🔴 RSI ≥ 65: "과매수! 너무 올랐으니 조정받을 수 있다!" → 매도 신호<br/><br/>
            ※ 주식/ETF는 코인보다 변동성이 낮아 35/65 기준 사용 (코인: 32/68)
          </div>
        </div>
        <div class="help-diagram mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">📊 시각적 설명</span>
          </div>
          <svg width="100%" height="130" viewBox="0 0 500 130" style="background: #FAFAFA; border-radius: 8px; border: 1px solid #E0E0E0;">
            <rect x="30" y="10" width="440" height="20" rx="4" fill="#FFCDD2"/>
            <text x="220" y="24" font-size="11" fill="#c62828" text-anchor="middle">과매수 영역 (RSI 65~100)</text>
            <rect x="30" y="35" width="440" height="50" rx="4" fill="#E8F5E9"/>
            <text x="220" y="64" font-size="12" fill="#2E7D32" text-anchor="middle" font-weight="bold">정상 영역 (RSI 35~65)</text>
            <rect x="30" y="90" width="440" height="20" rx="4" fill="#BBDEFB"/>
            <text x="220" y="104" font-size="11" fill="#1565C0" text-anchor="middle">과매도 영역 (RSI 0~35)</text>
            <line x1="30" y1="35" x2="470" y2="35" stroke="#e53935" stroke-width="1.5" stroke-dasharray="4,3"/>
            <text x="475" y="39" font-size="10" fill="#e53935">65</text>
            <line x1="30" y1="90" x2="470" y2="90" stroke="#1565C0" stroke-width="1.5" stroke-dasharray="4,3"/>
            <text x="475" y="94" font-size="10" fill="#1565C0">35</text>
            <text x="30" y="125" font-size="11" fill="#666">🔵 RSI ≤ 35: 매수 신호</text>
            <text x="300" y="125" font-size="11" fill="#666">🔴 RSI ≥ 65: 매도 신호</text>
          </svg>
        </div>
        <div class="mb-2">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">📋 설정값 안내</span>
          </div>
          <div style="padding-left: 24px;">
            <table style="width: 100%; border-collapse: collapse; border: 1px solid #E0E0E0; font-size: 13px;">
              <thead><tr style="background-color: #ECEFF1;"><th style="padding: 10px 12px; text-align: left; border-bottom: 1px solid #E0E0E0;">설정</th><th style="padding: 10px 12px; text-align: center; border-bottom: 1px solid #E0E0E0;">기간 14일</th><th style="padding: 10px 12px; text-align: center; border-bottom: 1px solid #E0E0E0;">매수 ≤35</th><th style="padding: 10px 12px; text-align: center; border-bottom: 1px solid #E0E0E0;">매도 ≥65</th></tr></thead>
              <tbody>
                <tr><td style="padding: 8px 12px; border-bottom: 1px solid #EEE;">의미</td><td style="padding: 8px 12px; border-bottom: 1px solid #EEE; text-align: center;">표준 설정</td><td style="padding: 8px 12px; border-bottom: 1px solid #EEE; text-align: center;">과매도 진입</td><td style="padding: 8px 12px; border-bottom: 1px solid #EEE; text-align: center;">과매수 전 탈출</td></tr>
                <tr style="background-color: #E3F2FD;"><td style="padding: 8px 12px; color: #1565C0;"><strong>추천</strong></td><td style="padding: 8px 12px; text-align: center; color: #1565C0;">✅</td><td style="padding: 8px 12px; text-align: center; color: #1565C0;">✅</td><td style="padding: 8px 12px; text-align: center; color: #1565C0;">✅</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `
  },

  // ===== ⭐ 신규: 볼린저 밴드 (코인 거래 설정에서 이식, SVG 차트 동일) =====
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
        <div class="help-example-card mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🚗 도로 비유</span>
          </div>
          <div style="padding-left: 24px; background: #FFF8E1; padding: 16px; border-radius: 8px; border-left: 4px solid #FFA000;">
            볼린저 밴드는 도로의 차선과 같습니다.<br/><br/>
            - 가격이 하단 밴드 터치 → "싸졌네? 매수 신호!"<br/>
            - 가격이 상단 밴드 터치 → "비싸졌네? 매도 신호!"<br/><br/>
            📊 <strong>현재 설정:</strong><br/>
            - 기간 20일: 20일 평균 기준<br/>
            - 표준편차 2배: 밴드 폭 결정 (2배가 표준)
          </div>
        </div>
        <div class="help-diagram mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">📊 시각적 설명</span>
          </div>
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
    `
  },

  // ===== ⭐ 신규: 거래량 (코인 거래 설정에서 이식, 120% 기본값 반영) =====
  volumeSettings: {
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
        <div class="help-example-card mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🏪 가게 비유</span>
          </div>
          <div style="padding-left: 24px; background: #FFF8E1; padding: 16px; border-radius: 8px; border-left: 4px solid #FFA000;">
            평소 하루 100명 오는 가게에<br/>
            - 오늘 120명 왔다 → "뭔가 있네! 관심 가져볼까?"<br/>
            - 오늘 200명 왔다 → "대박 터졌다!"<br/>
            - 오늘 80명 왔다 → "오늘은 조용하네..."<br/><br/>
            📊 <strong>ETF 예시:</strong><br/>
            TIGER 나스닥100레버리지 평균 거래량: 500만주/일<br/>
            - 120% 설정 시: 600만주 이상 거래되는 날에만 매수 신호 인정<br/><br/>
            <strong>왜 중요한가?</strong><br/>
            거래량 없이 가격만 움직이면 → 세력의 조작일 수 있음<br/>
            거래량 터지면서 움직이면 → 진짜 시장 반응!<br/><br/>
            ※ ETF는 코인보다 거래량이 안정적이어서 120%로 설정 (코인: 140%)
          </div>
        </div>
      </div>
    `
  },

  // ===== ⭐ 신규: 매수 기준 (코인 거래 설정에서 이식, -3% 기본값 반영) =====
  buyThreshold: {
    title: '🏷️ 매수 기준 하락률',
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
        <div class="help-example-card mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🛒 마트 비유</span>
          </div>
          <div style="padding-left: 24px; background: #FFF8E1; padding: 16px; border-radius: 8px; border-left: 4px solid #FFA000;">
            평소 7만원 하는 삼성전자가<br/>
            - -2% 설정: 68,600원 되면 구매<br/>
            - -3% 설정: 67,900원 되면 구매 (주식 기본값 ✅)<br/>
            - -5% 설정: 66,500원 되면 구매<br/><br/>
            ※ 주식/ETF는 코인보다 변동성이 낮아 -3% 사용 (코인: -6%)
          </div>
        </div>
        <div class="mb-2">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">📋 설정값 안내</span>
          </div>
          <div style="padding-left: 24px;">
            <table style="width: 100%; border-collapse: collapse; border: 1px solid #E0E0E0; font-size: 13px;">
              <thead><tr style="background-color: #ECEFF1;"><th style="padding: 10px 12px; text-align: left; border-bottom: 1px solid #E0E0E0;">설정값</th><th style="padding: 10px 12px; text-align: left; border-bottom: 1px solid #E0E0E0;">거래 빈도</th><th style="padding: 10px 12px; text-align: left; border-bottom: 1px solid #E0E0E0;">적합한 상황</th></tr></thead>
              <tbody>
                <tr><td style="padding: 8px 12px; border-bottom: 1px solid #EEE;">-1~2%</td><td style="padding: 8px 12px; border-bottom: 1px solid #EEE;">많음</td><td style="padding: 8px 12px; border-bottom: 1px solid #EEE;">공격적 투자자</td></tr>
                <tr style="background-color: #E3F2FD;"><td style="padding: 8px 12px; border-bottom: 1px solid #EEE; color: #1565C0;"><strong>-3%</strong></td><td style="padding: 8px 12px; border-bottom: 1px solid #EEE; color: #1565C0;">보통</td><td style="padding: 8px 12px; border-bottom: 1px solid #EEE; color: #1565C0;">초보자 추천 ✅</td></tr>
                <tr><td style="padding: 8px 12px;">-5% 이상</td><td style="padding: 8px 12px;">적음</td><td style="padding: 8px 12px;">신중한 투자자</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `
  },

  // ===== ⭐ 신규: 목표 수익률 (코인 거래 설정에서 이식, 2.5% 기본값 반영) =====
  sellTarget: {
    title: '💰 목표 수익률',
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
        <div class="help-example-card mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🛒 예시</span>
          </div>
          <div style="padding-left: 24px; background: #FFF8E1; padding: 16px; border-radius: 8px; border-left: 4px solid #FFA000;">
            10만원에 산 ETF를<br/>
            - 1.5% 설정: 101,500원에 판매<br/>
            - 2.5% 설정: 102,500원에 판매 (주식 기본값 ✅)<br/>
            - 5% 설정: 105,000원에 판매<br/><br/>
            ※ 주식/ETF는 코인보다 변동폭이 작아 2.5% 사용 (코인: 4%)
          </div>
        </div>
        <div class="mb-2">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">📋 설정값 안내</span>
          </div>
          <div style="padding-left: 24px;">
            <table style="width: 100%; border-collapse: collapse; border: 1px solid #E0E0E0; font-size: 13px;">
              <thead><tr style="background-color: #ECEFF1;"><th style="padding: 10px 12px; text-align: left; border-bottom: 1px solid #E0E0E0;">설정값</th><th style="padding: 10px 12px; text-align: left; border-bottom: 1px solid #E0E0E0;">적합한 상황</th></tr></thead>
              <tbody>
                <tr><td style="padding: 8px 12px; border-bottom: 1px solid #EEE;">1~2%</td><td style="padding: 8px 12px; border-bottom: 1px solid #EEE;">횡보장, 하락장</td></tr>
                <tr style="background-color: #E3F2FD;"><td style="padding: 8px 12px; border-bottom: 1px solid #EEE; color: #1565C0;"><strong>2.5~3%</strong></td><td style="padding: 8px 12px; border-bottom: 1px solid #EEE; color: #1565C0;">초보자 추천 ✅</td></tr>
                <tr><td style="padding: 8px 12px;">5%+</td><td style="padding: 8px 12px;">상승장 (레버리지 ETF)</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `
  },

   // ===== ⭐ 신규: 손절매 ON/OFF 스위치 =====
  useStopLoss: {
    title: '🔘 손절매 사용 여부',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"손절매 기능 자체를 켜고 끌 수 있는 스위치"</p>
          </div>
        </div>
        <div class="help-example-card mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">⚠️ 주의사항</span>
          </div>
          <div style="padding-left: 24px; background: #FFEBEE; padding: 16px; border-radius: 8px; border-left: 4px solid #e53935;">
            <strong>ON (권장)</strong>: 손절매 기준 도달 시 자동으로 손실을 확정하고 매도<br/>
            → 추가 손실 방지, 자금 회수 후 재투자 가능<br/><br/>
            <strong>OFF (비권장)</strong>: 아무리 손실이 나도 자동 매도 안 함<br/>
            → 레버리지 ETF는 큰 손실로 이어질 수 있어 <strong>강력 비권장</strong><br/><br/>
            ※ OFF 시 연속 손절 제한 기능도 함께 비활성화됩니다.
          </div>
        </div>
      </div>
    `
  },

  // ===== ⭐ 신규: 손절매 (코인 거래 설정에서 이식, -5% 기본값 반영) =====
  stopLoss: {
    title: '🛑 손절매 기준',
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
        <div class="help-example-card mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🚪 비상구 비유</span>
          </div>
          <div style="padding-left: 24px; background: #FFEBEE; padding: 16px; border-radius: 8px; border-left: 4px solid #e53935;">
            10만원에 산 ETF가 계속 떨어진다면...<br/>
            - -3% 설정: 97,000원에 손절<br/>
            - -5% 설정: 95,000원에 손절 (주식 기본값 ✅)<br/>
            - -8% 설정: 92,000원에 손절<br/><br/>
            💡 손절 안 하면? 10만원 → 5만원... 50% 손실 회복하려면 100% 올라야 해요!<br/><br/>
            ※ 주식/ETF는 코인보다 변동성이 낮아 -5% 사용 (코인: -8%)
          </div>
        </div>
      </div>
    `
  },

  // ===== ⭐ 신규: 트레일링 스톱 (코인 거래 설정에서 이식, -2.5% 기본값 + SVG 차트) =====
  trailingStop: {
    title: '🎯 트레일링 스톱',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"수익이 나고 있을 때, 최고점에서 얼마나 떨어지면 팔 것인가?"</p>
          </div>
        </div>
        <div class="help-example-card mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🏔️ 등산 비유</span>
          </div>
          <div style="padding-left: 24px; background: #FFF8E1; padding: 16px; border-radius: 8px; border-left: 4px solid #FFA000;">
            ETF 가격이 계속 올라가고 있어요!<br/>
            10만원 → 10만3천원 → 10만5천원 (최고점!)<br/><br/>
            트레일링 스톱 -2.5% 설정이면:<br/>
            최고점(10만5천원) 기준 -2.5% = 102,375원<br/>
            가격이 102,375원 이하로 떨어지면 자동 매도!<br/><br/>
            💡 고정 목표가(+2.5%)보다 더 많은 수익을 챙길 수 있어요!<br/><br/>
            ※ 주식/ETF는 -2.5% 사용 (코인: -4%)
          </div>
        </div>
        <div class="help-diagram mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">📊 시각적 설명</span>
          </div>
          <svg width="100%" height="140" viewBox="0 0 500 140" style="background: #FAFAFA; border-radius: 8px; border: 1px solid #E0E0E0;">
            <polyline points="40,110 100,90 160,70 220,50 280,35 340,30 400,50 440,65" fill="none" stroke="#1976D2" stroke-width="2.5"/>
            <circle cx="340" cy="30" r="6" fill="#FF9800"/>
            <text x="300" y="22" font-size="11" fill="#FF9800" font-weight="bold">최고점!</text>
            <line x1="340" y1="30" x2="340" y2="55" stroke="#FF9800" stroke-width="1" stroke-dasharray="3,2"/>
            <line x1="30" y1="55" x2="470" y2="55" stroke="#e53935" stroke-width="1.5" stroke-dasharray="5,3"/>
            <text x="380" y="50" font-size="10" fill="#e53935">-2.5% 라인</text>
            <circle cx="400" cy="50" r="6" fill="#e53935"/>
            <text x="405" y="70" font-size="11" fill="#e53935" font-weight="bold">매도!</text>
            <rect x="40" y="95" width="12" height="12" rx="2" fill="#43a047"/>
            <text x="57" y="105" font-size="10" fill="#666">수익 확보 구간</text>
          </svg>
        </div>
      </div>
    `
  },

  // ===== ⭐ 신규: 보유 제한 (Phase 2 전용 - 최대 보유일수 + 변동성 끌림 설명) =====
  holdingLimit: {
    title: '📦 보유 제한',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"한 종목을 최대 몇 건, 최대 며칠까지 가지고 있을 것인가?"</p>
          </div>
        </div>
        <div class="help-example-card mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🥚 계란 바구니 비유</span>
          </div>
          <div style="padding-left: 24px; background: #FFF8E1; padding: 16px; border-radius: 8px; border-left: 4px solid #FFA000;">
            <strong>종목당 최대 보유 건수 (기본: 3건)</strong><br/>
            같은 ETF를 여러 번 매수할 수 있지만, 3건까지만! 한 바구니에 계란을 너무 많이 담지 않는 것.<br/><br/>
            <strong>최대 보유일수 (기본: 20일)</strong> ⚠️ Phase 2 전용<br/>
            레버리지 ETF는 장기 보유 시 <strong>변동성 끌림(Volatility Drag)</strong>으로 가치가 침식됩니다.<br/><br/>
            📊 예시: 기초지수가 횡보(0% 수익)해도<br/>
            - 1일차: +2% → -2% = -0.04%<br/>
            - 반복 20일: 약 -0.8% 손실 누적<br/><br/>
            → 20거래일 내 청산을 권장합니다.
          </div>
        </div>
      </div>
    `
  },

  // ===== investment (기존 유지) =====
  investment: {
    title: '💵 투자금 및 매수 방식 안내',
    content: `
      <p>일일 투자 한도와 매수 방식을 설정합니다.</p>
      <p style="margin-top:12px;"><strong>일일 거래 한도</strong></p>
      <p style="padding-left:20px;">하루 동안 사용할 수 있는 최대 매수 금액입니다. 과도한 투자를 방지합니다.</p>
      <p style="margin-top:8px;"><strong>1회 고정 매수 금액</strong></p>
      <p style="padding-left:20px;">고정금액 방식 선택 시, 매 매수마다 이 금액만큼 매수합니다.</p>
      <p style="margin-top:12px;"><strong>매수 방식 비교:</strong></p>
      <table style="width:100%; border-collapse:collapse; margin-top:8px;">
        <tr style="background:#f5f5f5;"><th style="border:1px solid #ddd; padding:6px;">방식</th><th style="border:1px solid #ddd; padding:6px;">설명</th><th style="border:1px solid #ddd; padding:6px;">장점</th></tr>
        <tr><td style="border:1px solid #ddd; padding:6px;">라운드로빈</td><td style="border:1px solid #ddd; padding:6px;">남은 한도를 매수 후보에 균등 분배</td><td style="border:1px solid #ddd; padding:6px;">분산 투자 효과</td></tr>
        <tr><td style="border:1px solid #ddd; padding:6px;">고정금액</td><td style="border:1px solid #ddd; padding:6px;">매 매수마다 동일한 금액 투자</td><td style="border:1px solid #ddd; padding:6px;">예측 가능한 투자</td></tr>
      </table>
      <p style="margin-top:12px;"><strong>일일 한도 회복</strong></p>
      <p style="padding-left:20px;">매도로 현금이 회수되면, 그 금액만큼 일일 한도가 회복되어 재매수가 가능합니다.</p>
    `
  },

  // ===== ⭐ 신규: 일일 거래 한도/종목 비중 (코인 거래 설정에서 이식) =====
  dailyTradeLimit: {
    title: '💰 일일 거래 한도 / 종목 비중',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"하루에 최대 얼마까지만, 한 종목에 최대 얼마까지만 투자할 것인가?"</p>
          </div>
        </div>
        <div class="help-example-card mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">💰 용돈 비유</span>
          </div>
          <div style="padding-left: 24px; background: #FFF8E1; padding: 16px; border-radius: 8px; border-left: 4px solid #FFA000;">
            <strong>일일 거래 한도 (20%)</strong><br/>
            투자금 100만원이면 하루 최대 20만원까지만 매수.<br/>
            폭락장에 한꺼번에 다 쓰는 것을 방지!<br/><br/>
            <strong>단일 종목 최대 비중 (25%)</strong><br/>
            투자금 100만원이면 한 ETF에 최대 25만원까지만.<br/>
            "계란을 한 바구니에 담지 마라!" 원칙.
          </div>
        </div>
      </div>
    `
  },

  // ===== ⭐ 신규: 긴급 정지 (코인 거래 설정에서 이식) =====
  dailyStopLoss: {
    title: '🚨 긴급 정지 (일일 손실률)',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"하루에 일정 금액 이상 잃으면 자동으로 거래를 멈추는 비상 브레이크"</p>
          </div>
        </div>
        <div class="help-example-card mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🚗 자동차 비유</span>
          </div>
          <div style="padding-left: 24px; background: #FFEBEE; padding: 16px; border-radius: 8px; border-left: 4px solid #e53935;">
            고속도로에서 브레이크 없으면 큰 사고!<br/><br/>
            -5% 설정 시: 하루에 투자금의 5% 손실 발생하면<br/>
            → 자동으로 당일 모든 거래 중단!<br/>
            → 다음 날 0시에 자동 해제<br/><br/>
            💡 감정에 휘둘려 연속 매수하는 것을 막아줍니다.
          </div>
        </div>
      </div>
    `
  },

  // ===== ⭐ 신규: 급락장 보호 (코인 거래 설정에서 이식) =====
  crashProtection: {
    title: '🛡️ 급락장 보호 기능',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"시장이 급락할 때 자동으로 매수를 멈추는 안전장치"</p>
          </div>
        </div>
        <div class="help-example-card mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🌧️ 날씨 비유</span>
          </div>
          <div style="padding-left: 24px; background: #FFEBEE; padding: 16px; border-radius: 8px; border-left: 4px solid #e53935;">
            <strong>① 시장 추세 필터</strong><br/>
            KOSPI/기초지수가 20일선 아래면 전체 매수 중단<br/>
            → "비 오는 날엔 우산 없이 나가지 마!"<br/><br/>
            <strong>② 누적 손실 한도 (-10%)</strong><br/>
            초기 자본 대비 누적 -10% 손실 시 거래 완전 중단<br/>
            → "더 이상 잃으면 안 돼! 전원 OFF!"<br/><br/>
            <strong>③ 연속 손절 제한 (3회)</strong><br/>
            같은 종목에서 3번 연속 손절 시 해당 종목 매수 금지<br/>
            → "이 ETF랑은 지금 안 맞아, 잠시 쉬자!"
          </div>
        </div>
      </div>
    `
  },

  // ⭐ [신규] 손절매 ON/OFF 스위치 도움말
  useStopLoss: {
    title: '🔘 손절매 사용 여부',
    content: `
      <div class="glossary-detail pa-3">
        <div class="help-example-card mb-4">
          <div style="padding-left: 24px; background: #FFEBEE; padding: 16px; border-radius: 8px; border-left: 4px solid #e53935;">
            <strong>ON (권장)</strong>: 손절매 기준 도달 시 자동 매도 → 추가 손실 방지<br/><br/>
            <strong>OFF (비권장)</strong>: 아무리 손실이 나도 자동 매도 안 함<br/>
            → 레버리지 ETF는 큰 손실로 이어질 수 있어 <strong>강력 비권장</strong><br/><br/>
            ※ OFF 시 연속 손절 제한 기능도 함께 비활성화됩니다.
          </div>
        </div>
      </div>
    `
  },

  // ⭐ [신규] 추가 매수 하락률 도움말
  additionalDropPct: {
    title: '📉 추가 매수 하락률',
    content: `
      <div class="glossary-detail pa-3">
        <div class="glossary-section mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🔖 쉬운 설명</span>
          </div>
          <div style="padding-left: 24px;">
            <p class="text-body-2 text-grey-darken-3 mb-0">"직전 매수가 대비 얼마나 더 떨어지면 추가 매수를 허용할 것인가?"</p>
          </div>
        </div>
        <div class="help-example-card mb-4">
          <div class="d-flex align-center mb-2">
            <span class="text-subtitle-1 font-weight-bold">🛒 예시</span>
          </div>
          <div style="padding-left: 24px; background: #FFF8E1; padding: 16px; border-radius: 8px; border-left: 4px solid #FFA000;">
            10만원에 TIGER 나스닥100레버리지를 1차 매수했을 때<br/>
            - 1.0% 설정: 99,000원 이하로 떨어지면 2차 매수 허용<br/>
            - 2.0% 설정: 98,000원 이하로 떨어지면 2차 매수 허용<br/><br/>
            💡 너무 낮게 설정하면 작은 변동에도 추가 매수가 발생하고,<br/>
            너무 높게 설정하면 추가 매수 기회를 놓칠 수 있습니다.<br/><br/>
            ※ 주식/ETF 기본값: 1.0% (코인 대비 변동성 낮음)
          </div>
        </div>
        <div class="mb-2">
          <div style="padding-left: 24px;">
            <table style="width: 100%; border-collapse: collapse; border: 1px solid #E0E0E0; font-size: 13px;">
              <thead><tr style="background-color: #ECEFF1;"><th style="padding: 8px 12px; text-align: left; border-bottom: 1px solid #E0E0E0;">설정값</th><th style="padding: 8px 12px; text-align: left; border-bottom: 1px solid #E0E0E0;">특징</th></tr></thead>
              <tbody>
                <tr><td style="padding: 8px 12px; border-bottom: 1px solid #EEE;">0.5%</td><td style="padding: 8px 12px; border-bottom: 1px solid #EEE;">자주 추가 매수 (공격적)</td></tr>
                <tr style="background-color: #E3F2FD;"><td style="padding: 8px 12px; border-bottom: 1px solid #EEE; color: #1565C0;"><strong>1.0%</strong></td><td style="padding: 8px 12px; border-bottom: 1px solid #EEE; color: #1565C0;">기본값 추천 ✅</td></tr>
                <tr><td style="padding: 8px 12px;">2.0% 이상</td><td style="padding: 8px 12px;">신중한 추가 매수</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `
  }
}




const defaultSettings = {
  stockCodes: [] as string[],
  basePeriod: 20,
  buyThresholdPct: -3,
  sellTargetPct: 2.5,
  stopLossPct: -5,
  maxHoldingsPerStock: 3,
  dailyLimitAmount: 1000000,
  useTrailingStop: true,
  trailingStopPct: -2.5,
  rsiPeriod: 14,
  rsiBuyThreshold: 35,
  rsiSellThreshold: 65,
  bbPeriod: 20,
  bbMultiplier: 2,
  volumeThreshold: 120,
  dailyTradeLimitPct: 20,
  maxPositionPct: 25,
  dailyStopLossPct: -5,
  useMarketTrendFilter: false,
  cumulativeLossLimitPct: -10,
  consecutiveStopLossLimit: 3,
  fixedBuyAmount: 100000,
  useDailyLimitRecovery: false,
  useRoundRobin: true,
  maxHoldingDays: 20,
  useStopLoss: true,
  additionalDropPct: 1.0,
  useAiAnalysis: false
}

const settings = ref({ ...defaultSettings })

const presetStocks = [
  { code: '409820', name: 'TIGER 나스닥100레버리지' },
  { code: '409810', name: 'KODEX 나스닥100레버리지' },
  { code: '233740', name: 'KODEX 코스닥150레버리지' },
  { code: '122630', name: 'KODEX 레버리지' },
  { code: '252670', name: 'KODEX 200선물인버스2X' },
]

const etfReferenceList = [
  { code: '409820', name: 'TIGER 미국나스닥100레버리지(합성)', type: 'LEVERAGE', typeLabel: '레버리지', index: 'NASDAQ100 x2 (환노출)' },
  { code: '409810', name: 'KODEX 미국나스닥100레버리지(합성H)', type: 'LEVERAGE', typeLabel: '레버리지', index: 'NASDAQ100 x2 (환헤지)' },
  { code: '233740', name: 'KODEX 코스닥150레버리지', type: 'LEVERAGE', typeLabel: '레버리지', index: '코스닥150 x2' },
  { code: '122630', name: 'KODEX 레버리지', type: 'LEVERAGE', typeLabel: '레버리지', index: 'KOSPI200 x2' },
  { code: '252670', name: 'KODEX 200선물인버스2X', type: 'INVERSE', typeLabel: '인버스', index: 'KOSPI200 x(-2)' },
  { code: '069500', name: 'KODEX 200', type: 'NORMAL', typeLabel: '일반', index: 'KOSPI200' },
]

const rules = {
  required: (v: any) => v !== null && v !== undefined && v !== '' || '필수 입력',
  positive: (v: number) => v > 0 || '0보다 커야 합니다',
  negative: (v: number) => v <= 0 || '0 이하여야 합니다',
  basePeriod: (v: number) => (v >= 5 && v <= 60) || '5~60일 범위',
  minOne: (v: number) => v >= 1 || '최소 1 이상',
}

const getStockDisplayName = (code: string): string => {
  const preset = presetStocks.find(s => s.code === code)
  if (preset) return `${preset.name} (${code})`
  const registered = registeredStocks.value.find(s => s.stockCode === code)
  if (registered) return `${registered.stockName} (${code})`
  return code
}

const addStockByCode = async () => {
  const code = stockSearchKeyword.value.trim()
  if (!code) return
  if (!/^\d{6}$/.test(code)) {
    message.value = '종목코드는 6자리 숫자로 입력해주세요.'
    messageType.value = 'warning'
    return
  }
  if (settings.value.stockCodes.includes(code)) {
    message.value = `${code}는 이미 추가된 종목입니다.`
    messageType.value = 'info'
    return
  }

  searchLoading.value = true
  try {
    const etfInfo = etfReferenceList.find(e => e.code === code)
    const stockName = etfInfo ? etfInfo.name : `종목 ${code}`
    const market = 'KRX'
    const etfType = etfInfo ? etfInfo.type : 'NORMAL'

    try {
      await stockInfoApi.addStock(code, stockName, market, etfType)
    } catch (addError: any) {
      if (addError.response?.status !== 409) {
        console.log('종목 DB 등록 스킵:', addError.response?.data?.message)
      }
    }

    settings.value.stockCodes.push(code)
    stockSearchKeyword.value = ''
    message.value = `${getStockDisplayName(code)} 종목이 추가되었습니다. 저장 버튼을 눌러 적용하세요.`
    messageType.value = 'info'
  } catch (error: any) {
    message.value = error.response?.data?.message || '종목 추가에 실패했습니다.'
    messageType.value = 'error'
  } finally {
    searchLoading.value = false
  }
}

const togglePresetStock = (code: string) => {
  const index = settings.value.stockCodes.indexOf(code)
  if (index >= 0) {
    settings.value.stockCodes.splice(index, 1)
  } else {
    const preset = presetStocks.find(p => p.code === code)
    if (preset) {
      const etfInfo = etfReferenceList.find(e => e.code === code)
      stockInfoApi.addStock(code, preset.name, 'KRX', etfInfo?.type || 'LEVERAGE').catch(() => {})
    }
    settings.value.stockCodes.push(code)
  }
}

const removeStock = (code: string) => {
  const index = settings.value.stockCodes.indexOf(code)
  if (index >= 0) {
    settings.value.stockCodes.splice(index, 1)
  }
}

const buildPayload = () => ({
  stockCodes: settings.value.stockCodes,
  basePeriod: Number(settings.value.basePeriod),
  buyThresholdPct: Number(settings.value.buyThresholdPct),
  sellTargetPct: Number(settings.value.sellTargetPct),
  stopLossPct: Number(settings.value.stopLossPct),
  maxHoldingsPerStock: Number(settings.value.maxHoldingsPerStock),
  dailyLimitAmount: Number(settings.value.dailyLimitAmount),
  useTrailingStop: Boolean(settings.value.useTrailingStop),
  trailingStopPct: -Math.abs(Number(settings.value.trailingStopPct)),
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
  consecutiveStopLossLimit: Number(settings.value.consecutiveStopLossLimit),
  fixedBuyAmount: Number(settings.value.fixedBuyAmount),
  useDailyLimitRecovery: Boolean(settings.value.useDailyLimitRecovery),
  useRoundRobin: Boolean(settings.value.useRoundRobin),
  maxHoldingDays: Number(settings.value.maxHoldingDays),
  useStopLoss: Boolean(settings.value.useStopLoss),
  additionalDropPct: Number(settings.value.additionalDropPct),
  useAiAnalysis: Boolean(settings.value.useAiAnalysis)
})

const loadSettings = async () => {
  try {
    const response = await stockSettingApi.getSettings()
    const rawData = response.data
    console.log('GET /api/stock/settings 전체 응답:', JSON.stringify(rawData))
    const data = rawData?.data !== undefined ? rawData.data : rawData
    console.log('추출된 data:', JSON.stringify(data))
    console.log('data.stockCodes:', data?.stockCodes)

    if (data && data.stockCodes) {
      settings.value = {
        stockCodes: data.stockCodes || [],
        basePeriod: data.basePeriod || 20,
        buyThresholdPct: data.buyThresholdPct || -3,
        sellTargetPct: data.sellTargetPct || 2.5,
        stopLossPct: data.stopLossPct || -5,
        maxHoldingsPerStock: data.maxHoldingsPerStock || 3,
        dailyLimitAmount: data.dailyLimitAmount || 1000000,
        useTrailingStop: data.useTrailingStop ?? true,
        trailingStopPct: data.trailingStopPct || -2.5,
        rsiPeriod: data.rsiPeriod || 14,
        rsiBuyThreshold: data.rsiBuyThreshold || 35,
        rsiSellThreshold: data.rsiSellThreshold || 65,
        bbPeriod: data.bbPeriod || 20,
        bbMultiplier: data.bbMultiplier || 2,
        volumeThreshold: data.volumeThreshold || 120,
        dailyTradeLimitPct: data.dailyTradeLimitPct || 20,
        maxPositionPct: data.maxPositionPct || 25,
        dailyStopLossPct: data.dailyStopLossPct || -5,
        useMarketTrendFilter: data.useMarketTrendFilter ?? false,
        cumulativeLossLimitPct: data.cumulativeLossLimitPct || -10,
        consecutiveStopLossLimit: data.consecutiveStopLossLimit || 3,
        fixedBuyAmount: data.fixedBuyAmount || 100000,
        useDailyLimitRecovery: data.useDailyLimitRecovery ?? false,
        useRoundRobin: data.useRoundRobin ?? true,
        maxHoldingDays: data.maxHoldingDays || 20,
        // ⭐ [신규]
        useStopLoss: data.useStopLoss ?? true,
        additionalDropPct: data.additionalDropPct ?? 1.0,
        useAiAnalysis: data.useAiAnalysis ?? false
      }
      hasExistingSettings.value = true
      message.value = '기존 주식 거래 설정을 불러왔습니다.'
      messageType.value = 'info'
    } else {
      await createDefaultSettings()
    }
  } catch (error: any) {
    console.log('설정 로드 실패, 기본값으로 생성:', error.response?.status)
    await createDefaultSettings()
  }
}

const createDefaultSettings = async () => {
  try {
    settings.value = { ...defaultSettings }
    const payload = buildPayload()
    if (payload.stockCodes.length === 0) {
      payload.stockCodes = ['409820']
      settings.value.stockCodes = ['409820']
    }
    await stockSettingApi.createSettings(payload)
    hasExistingSettings.value = true
    message.value = '기본 주식 거래 설정이 자동으로 생성되었습니다.'
    messageType.value = 'success'
  } catch (createError: any) {
    if (createError.response?.status === 400 || createError.response?.status === 409) {
      hasExistingSettings.value = true
      message.value = '주식 거래 설정을 불러왔습니다.'
      messageType.value = 'info'
    } else {
      hasExistingSettings.value = false
      message.value = '기본 설정 생성에 실패했습니다. 직접 저장해주세요.'
      messageType.value = 'warning'
    }
  }
}

const saveSettings = async () => {
  if (!formRef.value) return
  const { valid: isValid } = await formRef.value.validate()
  if (!isValid) return

  if (settings.value.stockCodes.length === 0) {
    message.value = '거래할 종목을 1개 이상 선택해주세요.'
    messageType.value = 'warning'
    return
  }

  loading.value = true
  message.value = ''

  try {
    const payload = buildPayload()
    console.log('Sending stock settings payload:', payload)

    if (hasExistingSettings.value) {
      await stockSettingApi.updateSettings(payload)
      message.value = '주식 거래 설정이 성공적으로 수정되었습니다.'
    } else {
      try {
        await stockSettingApi.createSettings(payload)
        message.value = '주식 거래 설정이 성공적으로 저장되었습니다.'
      } catch (createError: any) {
        if (createError.response?.status === 500 || createError.response?.status === 409) {
          await stockSettingApi.updateSettings(payload)
          message.value = '주식 거래 설정이 성공적으로 수정되었습니다.'
        } else {
          throw createError
        }
      }
      hasExistingSettings.value = true
    }
    messageType.value = 'success'
  } catch (error: any) {
    message.value = error.response?.data?.error?.message || error.response?.data?.message || '설정 저장에 실패했습니다.'
    messageType.value = 'error'
  } finally {
    loading.value = false
  }
}

const executeReset = () => {
  settings.value = { ...defaultSettings, stockCodes: ['409820'] }
  resetDialog.value = false
  message.value = '설정이 기본값으로 초기화되었습니다. 저장 버튼을 눌러 적용하세요.'
  messageType.value = 'info'
}

const confirmDelete = () => {
  deleteDialog.value = true
}

const deleteSettings = async () => {
  deleteLoading.value = true
  message.value = ''
  try {
    await stockSettingApi.deleteSettings()
    deleteDialog.value = false

    settings.value = { ...defaultSettings }
    hasExistingSettings.value = false
    await createDefaultSettings()
    message.value = '설정이 초기화되고 기본값으로 저장되었습니다.'
    messageType.value = 'success'
  } catch (error: any) {
    console.error('설정 삭제 실패:', error.response?.data)
    message.value = error.response?.data?.error?.message || error.response?.data?.message || '설정 삭제에 실패했습니다.'
    messageType.value = 'error'
  } finally {
    deleteLoading.value = false
  }
}

const loadActiveStocks = async () => {
  try {
    const response = await stockInfoApi.getActiveStocks()
    registeredStocks.value = response.data || []
  } catch (error) {
    console.log('활성 종목 로드 실패 (무시)')
  }
}

onMounted(async () => {
  await loadActiveStocks()
  await loadSettings()
})
</script>

<style scoped>
.gap-2 {
  gap: 8px;
}
.gap-3 {
  gap: 12px;
}
</style>