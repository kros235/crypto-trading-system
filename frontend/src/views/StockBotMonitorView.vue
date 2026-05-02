<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main class="bg-grey-lighten-3">
      <v-container fluid>
        <!-- 페이지 제목 -->
        <v-row class="mb-2">
          <v-col cols="12">
            <h1 class="text-h4">📈 주식 자동매매 봇 모니터링</h1>
            <p class="text-subtitle-1 text-grey">실시간 주식 봇 상태 및 기술적 지표 모니터링 (장 운영시간 09:00 ~ 15:30 KST)</p>
          </v-col>
        </v-row>

        <!-- ⭐⭐⭐ [Day 61 후속 v3 수정] 안내 alert + 데모 모드 토글을 한 줄에 정렬 ⭐⭐⭐ -->
        <!-- 변경 이유: 제목 영역에 안내가 들어가있어 두 줄로 늘어졌던 문제 → 단일 v-row에서 align-stretch로 동일 높이 유지 -->
        <v-row class="mb-4" align="stretch">
          <v-col cols="12" md="8">
            <v-alert
              type="info"
              variant="tonal"
              density="compact"
              icon="mdi-information"
              class="h-100 d-flex align-center"
            >
              <span class="text-body-2">이 페이지의 봇 ON/OFF는 <strong>주식 자동매매 전용</strong>이며, 코인 자동매매 봇과 독립적으로 동작합니다.</span>
            </v-alert>
          </v-col>
          <v-col cols="12" md="4">
            <v-card variant="tonal" color="amber-darken-2" class="pa-2 pl-4 h-100 demo-mode-card">
              <div class="d-flex align-center justify-space-between h-100">
                <div>
                  <!-- ⭐ [Day 61 v5 수정] 글씨를 검정으로 명시 (tonal variant의 진한 색 글씨가 amber에서 가독성 떨어지는 문제) -->
                  <div class="text-caption lh-1 demo-mode-caption">시연 / 개발용</div>
                  <div class="text-body-2 font-weight-bold demo-mode-title">🎬 데모 모드 (가짜 데이터)</div>
                </div>
                <v-switch
                  v-model="demoMode"
                  color="amber-darken-2"
                  hide-details
                  density="compact"
                  @change="onDemoModeToggle"
                />
              </div>
            </v-card>
          </v-col>
        </v-row>

        <!-- 봇 상태 카드 -->
        <v-row class="mb-4">
          <v-col cols="12" md="3">
            <v-card class="pa-4 bot-stats-card" :color="botEnabled ? (botRunning ? 'success' : 'info') : 'grey-darken-1'" dark>
              <div class="d-flex align-center">
                <v-icon size="48" class="mr-4">
                  {{ botEnabled ? 'mdi-robot' : 'mdi-robot-off' }}
                </v-icon>
                <div class="flex-grow-1">
                  <div class="d-flex align-center justify-space-between">
                    <div class="text-h6">
                      주식 봇 상태
                      <!-- ⭐⭐⭐ [Day 61 후속 추가] 봇 상태 도움말 (코인봇 분리 안내) ⭐⭐⭐ -->
                      <HelpButton
                        :use-dialog="true"
                        :dialog-title="helpContents.botStatus.title"
                        :dialog-content="helpContents.botStatus.content"
                        color="white"
                      />
                    </div>
                    <v-switch
                      v-model="botEnabled"
                      :loading="botToggleLoading"
                      :disabled="botToggleLoading"
                      color="white"
                      hide-details
                      density="compact"
                      @change="toggleBot"
                    />
                  </div>
                  <div class="text-h4">{{ botStatusLabel }}</div>
                </div>
              </div>
            </v-card>
          </v-col>

          <v-col cols="12" md="3">
            <v-card class="pa-4 bot-stats-card bot-schedule-card" color="primary" dark>
              <div class="d-flex align-center">
                <v-icon size="48" class="mr-4">mdi-clock-outline</v-icon>
                <div class="flex-grow-1">
                  <div class="schedule-label-row">마지막 봇 수행 시간</div>
                  <div class="schedule-value-row">{{ formatBotTimeDisplay(lastExecutionTime) }}</div>
                  <div class="schedule-label-row mt-2">다음 봇 수행 시간</div>
                  <div class="schedule-value-row">{{ botEnabled && marketOpen ? formatBotTimeDisplay(nextExecutionTime) : '-' }}</div>
                  <div class="countdown-row">({{ countdownText }})</div>
                </div>
              </div>
            </v-card>
          </v-col>

          <v-col cols="12" md="3">
            <v-card class="pa-4 bot-stats-card" color="info" dark>
              <div class="d-flex align-center">
                <v-icon size="48" class="mr-4">mdi-cart-arrow-down</v-icon>
                <div>
                  <div class="text-h6">오늘 매수</div>
                  <div class="text-h4">{{ todayStats.buyCount }}건</div>
                </div>
              </div>
            </v-card>
          </v-col>

          <v-col cols="12" md="3">
            <v-card class="pa-4 bot-stats-card" color="warning" dark>
              <div class="d-flex align-center">
                <v-icon size="48" class="mr-4">mdi-cart-arrow-up</v-icon>
                <div>
                  <div class="text-h6">오늘 매도</div>
                  <div class="text-h4">{{ todayStats.sellCount }}건</div>
                </div>
              </div>
            </v-card>
          </v-col>
        </v-row>

        <!-- 시장 상태 + 보유기간 경고 (주식 전용 추가) -->
        <v-row class="mb-4">
          <v-col cols="12" md="6">
            <v-card class="pa-4 control-card">
              <v-card-title class="d-flex align-center pb-0">
                <v-icon class="mr-2" color="indigo">mdi-calendar-clock</v-icon>
                시장 상태
                <v-spacer />
                <HelpButton
                  :use-dialog="true"
                  :dialog-title="helpContents.marketStatus.title"
                  :dialog-content="helpContents.marketStatus.content"
                  color="grey-darken-1"
                />
              </v-card-title>
              <v-card-text class="pt-6">
                <div class="status-row">
                  <span class="status-label">장 운영 여부</span>
                  <v-chip :color="marketOpen ? 'success' : 'grey'" size="small" variant="flat">
                    {{ marketOpen ? '🟢 장중' : '🔴 장마감' }}
                  </v-chip>
                </div>
                <div class="status-row">
                  <span class="status-label">긴급 정지</span>
                  <v-chip :color="emergencyStop ? 'error' : 'success'" size="small" variant="flat">
                    {{ emergencyStop ? '⚠️ 작동 중' : '✅ 정상' }}
                  </v-chip>
                </div>
                <div class="status-row">
                  <span class="status-label">내일 휴장</span>
                  <v-chip :color="tomorrowHoliday ? 'warning' : 'grey'" size="small" variant="flat">
                    {{ tomorrowHoliday ? '🗓 휴장 예정' : '정상 운영' }}
                  </v-chip>
                </div>
                <div class="status-row">
                  <span class="status-label">정규장 시간</span>
                  <span class="status-value">09:00 ~ 15:30 (KST)</span>
                </div>
              </v-card-text>
            </v-card>
          </v-col>

          <v-col cols="12" md="6">
            <v-card class="pa-4 control-card">
              <v-card-title class="d-flex align-center pb-0">
                <v-icon class="mr-2" color="orange">mdi-alert-circle-outline</v-icon>
                보유기간 경고
                <v-spacer />
                <HelpButton
                  :use-dialog="true"
                  :dialog-title="helpContents.holdingWarnings.title"
                  :dialog-content="helpContents.holdingWarnings.content"
                  color="grey-darken-1"
                />
              </v-card-title>
              <v-card-text class="pt-6">
                <div v-if="holdingWarnings.length === 0" class="text-grey text-center py-4">
                  <v-icon size="40" color="success">mdi-check-circle-outline</v-icon>
                  <div class="mt-2">현재 보유기간 경고 대상이 없습니다.</div>
                </div>
                <div v-else class="warning-list-wrapper">
                  <!-- ⭐⭐⭐ [Day 61 후속 v3 수정] 보유기간 경고를 종목명/매수일/수량/금액 풍부하게 표시 ⭐⭐⭐ -->
                  <!-- 변경 이유: 동일 종목 다중 보유 시 어떤 매수 건이 경고인지 식별 가능하도록 매수일/수량/금액 병기 -->
                  <div
                    v-for="(w, idx) in holdingWarnings"
                    :key="w.transactionId || `${w.stockCode}-${idx}`"
                    :class="['warning-item', w.urgent ? 'warning-urgent' : 'warning-normal']"
                  >
                    <div class="warning-header">
                      <v-icon :color="w.urgent ? 'error' : 'warning'" size="20" class="mr-2">
                        {{ w.urgent ? 'mdi-alert-octagon' : 'mdi-alert' }}
                      </v-icon>
                      <span class="warning-stock-name">
                        <strong>{{ getStockName(w.stockCode) }}</strong>
                        <span class="text-caption text-grey-darken-1 ml-1">({{ w.stockCode }})</span>
                      </span>
                      <v-chip
                        :color="w.urgent ? 'error' : 'warning'"
                        size="x-small"
                        variant="flat"
                        class="ml-auto"
                      >
                        {{ w.urgent ? '🚨 긴급 청산 권장' : '⚠️ 경고' }}
                      </v-chip>
                    </div>
                    <div class="warning-details">
                      <span class="detail-item">
                        <span class="detail-label">매수일:</span>
                        <span class="detail-value">{{ w.buyDate || '-' }}</span>
                      </span>
                      <span class="detail-divider">|</span>
                      <span class="detail-item">
                        <span class="detail-label">보유기간:</span>
                        <!-- ⭐ [Day 61 v5 수정] 경고(warning) 단계: 노랑색이 흰 배경에서 안 보이는 문제 → 검정 굵게로 변경 -->
                        <!-- 긴급(urgent) 단계: 빨강 굵게 유지 (시각적 경고 강조 필요) -->
                        <span :class="['detail-value', w.urgent ? 'text-error' : 'text-strong-black']">
                          <strong>{{ w.holdingDays }}일</strong>
                        </span>
                      </span>
                      <span class="detail-divider">|</span>
                      <span class="detail-item">
                        <span class="detail-label">수량:</span>
                        <span class="detail-value">{{ w.quantity ? `${w.quantity}주` : '-' }}</span>
                      </span>
                      <span class="detail-divider">|</span>
                      <span class="detail-item">
                        <span class="detail-label">매수금액:</span>
                        <span class="detail-value">{{ formatPrice(w.totalAmount) }}</span>
                        <span v-if="w.buyPrice" class="text-caption text-grey ml-1">
                          (@ {{ formatPrice(w.buyPrice) }})
                        </span>
                      </span>
                    </div>
                  </div>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- 수동 제어 + 알림 테스트 카드 (한 줄 배치) -->
        <v-row class="mb-4">
          <!-- 수동 제어 -->
          <v-col cols="12" md="4">
            <v-card class="pa-4 control-card">
              <v-card-title class="d-flex align-center pb-0">
                <v-icon class="mr-2" color="primary">mdi-account-cog</v-icon>
                수동 제어
                <v-spacer />
                <HelpButton
                  :use-dialog="true"
                  :dialog-title="helpContents.manualControl.title"
                  :dialog-content="helpContents.manualControl.content"
                  :dialog-width="800"
                  color="grey-darken-1"
                />
              </v-card-title>
              <v-card-text class="pt-6">
                <div class="d-flex flex-column gap-3">
                  <v-btn
                    color="primary"
                    variant="outlined"
                    size="large"
                    block
                    :loading="executing"
                    :disabled="executing"
                    @click="executeBot"
                  >
                    <v-icon left>mdi-play</v-icon>
                    수동 매매 실행
                  </v-btn>

                  <v-btn
                    color="grey-darken-2"
                    variant="outlined"
                    size="large"
                    block
                    :loading="refreshing"
                    @click="refreshIndicators"
                  >
                    <v-icon left>mdi-refresh</v-icon>
                    지표 새로고침
                  </v-btn>

                  <v-btn
                    color="orange-darken-2"
                    variant="outlined"
                    size="large"
                    block
                    :loading="resettingCache"
                    @click="resetDailyCache"
                  >
                    <v-icon left>mdi-cached</v-icon>
                    일일 한도 초기화
                  </v-btn>
                </div>

                <!-- 실행 결과 표시 -->
                <v-alert
                  v-if="executionResult"
                  :type="executionResult.status === 'SUCCESS' ? 'success' : (executionResult.status === 'SKIP' ? 'info' : 'error')"
                  variant="tonal"
                  class="mt-4"
                >
                  <div class="font-weight-bold">{{ executionResult.message || executionResult.status }}</div>
                  <div v-if="(executionResult.buyCount || 0) > 0 || (executionResult.sellCount || 0) > 0">
                    매수: {{ executionResult.buyCount || 0 }}건 / 매도: {{ executionResult.sellCount || 0 }}건
                  </div>
                </v-alert>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- 이메일 테스트 발송 -->
          <v-col cols="12" md="4">
            <v-card class="pa-4 control-card">
              <v-card-title class="d-flex align-center pb-0">
                <v-icon class="mr-2" color="success">mdi-email</v-icon>
                이메일 테스트 발송
                <v-spacer />
                <HelpButton
                  :use-dialog="true"
                  :dialog-title="helpContents.emailTest.title"
                  :dialog-content="helpContents.emailTest.content"
                  color="grey-darken-1"
                />
              </v-card-title>
              <v-card-text class="pt-6">
                <v-alert
                  v-if="!userProfile.email"
                  type="warning"
                  variant="tonal"
                  density="compact"
                  class="mb-4"
                >
                  <div class="d-flex align-center" style="flex-wrap: nowrap;">
                    <span class="text-black" style="white-space: nowrap;">이메일 미등록</span>
                    <v-spacer />
                    <v-btn
                      variant="flat"
                      color="primary"
                      size="small"
                      to="/profile"
                    >
                      등록하기
                    </v-btn>
                  </div>
                </v-alert>

                <v-row dense>
                  <v-col cols="6">
                    <v-btn
                      color="teal"
                      variant="outlined"
                      size="large"
                      block
                      :loading="emailTestLoading.buy"
                      :disabled="!userProfile.email"
                      @click="sendEmailTest('buy')"
                    >
                      <v-icon left>mdi-cart-arrow-down</v-icon>
                      매수
                    </v-btn>
                  </v-col>
                  <v-col cols="6">
                    <v-btn
                      color="teal"
                      variant="outlined"
                      size="large"
                      block
                      :loading="emailTestLoading.sell"
                      :disabled="!userProfile.email"
                      @click="sendEmailTest('sell')"
                    >
                      <v-icon left>mdi-cart-arrow-up</v-icon>
                      매도
                    </v-btn>
                  </v-col>
                  <v-col cols="6">
                    <v-btn
                      color="teal"
                      variant="outlined"
                      size="large"
                      block
                      :loading="emailTestLoading.report"
                      :disabled="!userProfile.email"
                      @click="sendEmailTest('report')"
                    >
                      <v-icon left>mdi-file-chart</v-icon>
                      리포트
                    </v-btn>
                  </v-col>
                  <v-col cols="6">
                    <v-btn
                      color="teal"
                      variant="outlined"
                      size="large"
                      block
                      :loading="emailTestLoading.holding"
                      :disabled="!userProfile.email"
                      @click="sendEmailTest('holding')"
                    >
                      <v-icon left>mdi-calendar-alert</v-icon>
                      보유기간 경고
                    </v-btn>
                  </v-col>
                  <!-- ⭐⭐⭐ [Day 61 후속 추가] AI 뉴스 가중치 버튼 (Phase 2 가중치 기능 완성 후 정상 동작) ⭐⭐⭐ -->
                  <v-col cols="6">
                    <v-btn
                      color="teal"
                      variant="outlined"
                      size="large"
                      block
                      :loading="emailTestLoading.weight"
                      :disabled="!userProfile.email"
                      @click="sendEmailTest('weight')"
                    >
                      <v-icon left>mdi-chart-line</v-icon>
                      가중치
                    </v-btn>
                  </v-col>
                </v-row>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- 디스코드 DM 테스트 발송 -->
          <v-col cols="12" md="4">
            <v-card class="pa-4 control-card discord-control-card">
              <v-card-title class="d-flex align-center pb-0">
                <v-icon class="mr-2" color="deep-purple">mdi-robot</v-icon>
                디스코드 DM 테스트
                <!-- ⭐⭐⭐ [Day 61 후속 v3 수정] 헤더에 있던 "Discord 봇 서버" 칩을 카드 우측하단으로 이동 ⭐⭐⭐ -->
                <!-- 변경 이유: 100% 비율에서 카드 좁아질 때 칩이 잘리는 문제 해결 + 헤더가 깔끔해짐 -->
                <v-spacer />
                <HelpButton
                  :use-dialog="true"
                  :dialog-title="helpContents.discordTest.title"
                  :dialog-content="helpContents.discordTest.content"
                  :dialog-width="800"
                  color="grey-darken-1"
                />
              </v-card-title>
              <v-card-text class="pt-6">
                <v-alert
                  v-if="!userProfile.discordUserId"
                  type="warning"
                  variant="tonal"
                  density="compact"
                  class="mb-4"
                >
                  <div class="d-flex align-center" style="flex-wrap: nowrap;">
                    <span class="text-black" style="white-space: nowrap;">Discord ID 미등록</span>
                    <v-spacer />
                    <v-btn
                      variant="flat"
                      color="deep-purple"
                      size="small"
                      to="/profile"
                    >
                      등록하기
                    </v-btn>
                  </div>
                </v-alert>

                <v-row dense>
                  <v-col cols="6">
                    <v-btn
                      color="deep-purple"
                      variant="outlined"
                      size="large"
                      block
                      :loading="discordTestLoading.buy"
                      :disabled="!userProfile.discordUserId || !discordBotEnabled"
                      @click="sendDiscordTest('buy')"
                    >
                      <v-icon left>mdi-cart-arrow-down</v-icon>
                      매수
                    </v-btn>
                  </v-col>
                  <v-col cols="6">
                    <v-btn
                      color="deep-purple"
                      variant="outlined"
                      size="large"
                      block
                      :loading="discordTestLoading.sell"
                      :disabled="!userProfile.discordUserId || !discordBotEnabled"
                      @click="sendDiscordTest('sell')"
                    >
                      <v-icon left>mdi-cart-arrow-up</v-icon>
                      매도
                    </v-btn>
                  </v-col>
                  <v-col cols="6">
                    <v-btn
                      color="deep-purple"
                      variant="outlined"
                      size="large"
                      block
                      :loading="discordTestLoading.stoploss"
                      :disabled="!userProfile.discordUserId || !discordBotEnabled"
                      @click="sendDiscordTest('stoploss')"
                    >
                      <v-icon left>mdi-alert</v-icon>
                      손절매
                    </v-btn>
                  </v-col>
                  <v-col cols="6">
                    <v-btn
                      color="deep-purple"
                      variant="outlined"
                      size="large"
                      block
                      :loading="discordTestLoading.report"
                      :disabled="!userProfile.discordUserId || !discordBotEnabled"
                      @click="sendDiscordTest('report')"
                    >
                      <v-icon left>mdi-file-chart</v-icon>
                      리포트
                    </v-btn>
                  </v-col>
                  <!-- ⭐⭐⭐ [Day 61 후속 추가] AI 뉴스 가중치 버튼 ⭐⭐⭐ -->
                  <v-col cols="6">
                    <v-btn
                      color="deep-purple"
                      variant="outlined"
                      size="large"
                      block
                      :loading="discordTestLoading.weight"
                      :disabled="!userProfile.discordUserId || !discordBotEnabled"
                      @click="sendDiscordTest('weight')"
                    >
                      <v-icon left>mdi-chart-line</v-icon>
                      가중치
                    </v-btn>
                  </v-col>
                </v-row>

                <!-- ⭐⭐⭐ [Day 61 후속 v3 추가] Discord 봇 서버 상태 칩 - 카드 우측 하단 ⭐⭐⭐ -->
                <!-- 변경 이유: 100% 비율 카드 헤더에서 칩이 잘리던 문제 → 카드 하단으로 이동 -->
                <div class="discord-bot-status-row mt-3">
                  <v-chip
                    :color="discordBotEnabled ? 'success' : 'grey'"
                    size="x-small"
                    variant="flat"
                    prepend-icon="mdi-circle-medium"
                  >
                    Discord 봇 서버: {{ discordBotEnabled ? '온라인' : '오프라인' }}
                  </v-chip>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- 기술적 지표 테이블 -->
        <v-row>
          <v-col cols="12">
            <v-card>
              <v-card-title class="d-flex justify-space-between align-center">
                <span>
                  <v-icon class="mr-2">mdi-chart-line</v-icon>
                  기술적 지표
                  <HelpButton
                    :use-dialog="true"
                    :dialog-title="helpContents.indicators.title"
                    :dialog-content="helpContents.indicators.content"
                    color="grey-darken-1"
                  />
                </span>
                <span class="text-caption text-grey">
                  마지막 업데이트: {{ lastUpdated }}
                </span>
              </v-card-title>

              <v-data-table
                :headers="indicatorHeaders"
                :items="indicators"
                :loading="loading"
                class="elevation-0"
                no-data-text="등록된 종목이 없거나 KIS API 키 인증에 실패했습니다."
              >
                <template v-slot:item.stockCode="{ item }">
                  <div class="font-weight-medium">
                    <div>{{ getStockName(item.market) }}</div>
                    <div class="text-caption text-grey">{{ item.market }}</div>
                  </div>
                </template>

                <template v-slot:item.currentPrice="{ item }">
                  {{ formatPrice(item.currentPrice) }}
                </template>

                <template v-slot:item.ma20="{ item }">
                  {{ formatPrice(item.ma20) }}
                </template>

                <template v-slot:item.rsi="{ item }">
                  <!-- ⭐ [Day 61 후속 v3] 회색 칩일 때 검정 글씨로 가독성 확보 -->
                  <v-chip
                    :color="getRsiColor(item.rsi14)"
                    :class="getRsiColor(item.rsi14) === 'grey' ? 'chip-text-black' : ''"
                    size="small"
                  >
                    {{ item.rsi14 ? Number(item.rsi14).toFixed(1) : '-' }}
                  </v-chip>
                </template>

                <template v-slot:item.bbPosition="{ item }">
                  <!-- ⭐ [Day 61 후속 v3] 회색 칩일 때 검정 글씨로 가독성 확보 -->
                  <v-chip
                    :color="getBbPositionColor(item)"
                    :class="getBbPositionColor(item) === 'grey' ? 'chip-text-black' : ''"
                    size="small"
                  >
                    {{ getBbPosition(item) }}
                  </v-chip>
                </template>

                <template v-slot:item.volumeRatio="{ item }">
                  <span :class="Number(item.volumeRatio) >= 1.2 ? 'text-success font-weight-bold' : ''">
                    {{ item.volumeRatio ? `${Number(item.volumeRatio).toFixed(2)}x` : '-' }}
                  </span>
                </template>

                <template v-slot:item.maPosition="{ item }">
                  <span :class="getMaPositionClass(item)">
                    {{ getMaPosition(item) }}
                  </span>
                </template>

                <template v-slot:item.signal="{ item }">
                  <!-- ⭐ [Day 61 후속 v3] 회색 칩(대기 상태)일 때 검정 글씨로 가독성 확보 -->
                  <v-chip
                    :color="getSignalColor(item)"
                    :class="getSignalColor(item) === 'grey' ? 'chip-text-black' : ''"
                    size="small"
                  >
                    {{ getSignal(item) }}
                  </v-chip>
                </template>
              </v-data-table>
            </v-card>
          </v-col>
        </v-row>

      </v-container>
    </v-main>

    <v-snackbar v-model="snackbar" :color="snackbarColor" :timeout="3000">
      {{ snackbarMessage }}
    </v-snackbar>
  </v-app>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'
import HelpButton from '@/components/HelpButton.vue'
import api from '@/api'
import { stockBotApi, stockInfoApi } from '@/api/stock'
import type { StockInfo } from '@/types/stock'

const sidebarRef = ref()

// 기술적 지표 결과 (Phase 1 IndicatorResult와 동일한 구조이지만, market 필드에 stockCode가 들어감)
interface StockIndicatorResult {
  market: string          // 백엔드에서 stockCode가 IndicatorResultDTO.market 필드에 매핑됨
  currentPrice: number
  ma7: number | null
  ma14: number | null
  ma20: number | null
  ma30: number | null
  rsi14: number | null
  bbUpper: number | null
  bbMiddle: number | null
  bbLower: number | null
  avgVolume: number | null
  currentVolume: number | null
  volumeRatio: number | null
  belowMA20?: boolean
  rsiBuySignal?: boolean
  rsiSellSignal?: boolean
  belowBBLower?: boolean
  aboveBBUpper?: boolean
  highVolume?: boolean
  calculatedAt: string
}

interface HoldingWarning {
  stockCode: string
  holdingDays: number
  urgent: boolean
  // ⭐ [Day 61 후속 v3 추가] 동일 종목 다중 보유 식별 + 메시지 보강용 필드
  buyDate?: string         // 매수일 (yyyy-MM-dd)
  transactionId?: number   // 거래 ID (동일 종목 여러 건 식별용)
  quantity?: number        // 매수 수량
  buyPrice?: number        // 매수 단가
  totalAmount?: number     // 매수 총 금액
}

const helpContents = {
  // ⭐⭐⭐ [Day 61 후속 추가] 봇 상태 도움말 (코인봇과 분리 명시) ⭐⭐⭐
  botStatus: {
    title: '🤖 주식 자동매매 봇 상태 안내',
    content: `
      <p class="help-intro">이 페이지의 봇 상태는 <strong>주식 자동매매 봇 전용</strong>입니다.</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>주식봇과 코인봇은 독립적으로 동작</strong>
        <span class="help-desc">주식봇을 OFF 해도 코인봇은 계속 작동하며, 그 반대도 마찬가지입니다.<br/>각 봇은 별도의 설정과 거래 종목을 가집니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>실행 중</strong>
        <span class="help-desc">봇이 활성화되고 장중(09:00~15:30 KST)인 상태입니다. 3분마다 매수/매도 조건을 검사합니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>장마감 대기</strong>
        <span class="help-desc">봇이 활성화되어 있으나 장이 마감된 상태입니다. 다음 영업일 09:00에 자동으로 재개됩니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>긴급정지</strong>
        <span class="help-desc">누적 손실 한도 도달 또는 연속 손절 한도 초과로 봇이 강제 중지된 상태입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>중지됨</strong>
        <span class="help-desc">사용자가 봇 토글을 OFF로 설정한 상태입니다. 자동매매가 발생하지 않습니다.</span></p>
      <p class="help-note">💡 <strong>알림 발송도 분리:</strong> 주식봇이 OFF 상태이면 주식 거래 알림(매수/매도/손절)도 발송되지 않습니다. 단, 이 페이지의 "테스트 발송" 버튼은 봇 상태와 무관하게 동작합니다.</p>
    `
  },
  manualControl: {
    title: '🎮 수동 제어 안내',
    content: `
      <p class="help-intro">주식 자동매매 봇을 수동으로 제어할 수 있습니다.</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>수동 매매 실행</strong>
        <span class="help-desc">
          현재 설정된 거래 조건에 따라 즉시 매매 신호를 확인하고 거래를 실행합니다.<br/>
          정기 스케줄(3분 간격) 외에 즉시 실행하고 싶을 때 사용합니다.
        </span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>지표 새로고침</strong>
        <span class="help-desc">설정된 모든 종목의 기술적 지표(RSI, 볼린저밴드, 이동평균선 등)를 즉시 갱신합니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>일일 한도 초기화</strong>
        <span class="help-desc">
          <strong>오늘 사용한 일일 거래 한도를 0원으로 되돌려 다시 매매할 수 있게 합니다.</strong><br/>
          봇은 일일 거래 한도(예: 100만원)를 넘지 않도록 매일 매수/매도 누적 금액을 Redis 캐시에 저장합니다.<br/>
          이 캐시는 평일 15:35 KST(장 마감 직후)에 스케줄러가 자동으로 초기화하여<br/>
          다음 거래일부터 새로 카운트됩니다.<br/>
          <br/>
          <strong>📌 일일 한도 동작 방식 (예: 한도 100만원 사용자)</strong><br/>
          • 09:30 매수 30만원 실행 → 누적 매수 30만원 (잔여 한도 70만원)<br/>
          • 11:00 매수 50만원 실행 → 누적 매수 80만원 (잔여 한도 20만원)<br/>
          • 14:00 매수 20만원 실행 → 누적 매수 100만원 (한도 도달)<br/>
          • 14:30 봇이 매수 신호 감지 → "일일 한도 초과" 사유로 매수 차단<br/>
          • 15:35 스케줄러 자동 초기화 → 다음 거래일 09:00부터 다시 100만원 한도<br/>
          <br/>
          <strong>💼 실제 운영에서 수동 초기화가 필요한 사례</strong><br/>
          <strong>① 한도 도달 후 추가 매매 기회 활용</strong><br/>
          • 14:00 일일 한도(100만원) 도달 후 시장이 급락하여 좋은 매수 기회 발생<br/>
          • 한도 설정을 일시 상향 조정하고 캐시 초기화 → 추가 매수 진행 가능<br/>
          <br/>
          <strong>② 라운드로빈 분배 재계산</strong><br/>
          • 라운드로빈 매수 시 "남은 한도 ÷ 후보 종목 수" 로 분배됨<br/>
          • 예: 잔여 50만원 / 후보 5종목 → 종목당 10만원<br/>
          • 캐시 초기화 후 → 100만원 / 5종목 → 종목당 20만원으로 재분배<br/>
          <br/>
          <strong>③ 거래 설정 변경 후 새 한도 즉시 반영</strong><br/>
          • 거래 설정에서 일일 한도를 50만원 → 200만원으로 상향 조정<br/>
          • 기존 누적 캐시(예: 50만원 사용)는 그대로라 잔여 150만원만 사용 가능<br/>
          • 캐시 초기화 → 200만원 전체를 새로 사용 가능<br/>
          <br/>
          <strong>④ 캐시 데이터 불일치 복구</strong><br/>
          • Redis 장애로 캐시가 꼬여서 실제 거래 내역과 다르게 표시될 때<br/>
          • 초기화 시 다음 매수 시도 때 DB(stock_transactions)에서 재계산<br/>
          <br/>
          <strong>⑤ 테스트/시연 환경에서의 반복 검증</strong><br/>
          • 모의투자 환경에서 봇 동작 테스트 시 한도를 의도적으로 빠르게 소진<br/>
          • 한도 도달 후 차단 동작 확인 → 캐시 초기화 → 다시 테스트 반복<br/>
          <br/>
          <strong>⚠️ 주의 사항</strong><br/>
          • 실제 거래 내역(DB의 stock_transactions)은 <em>삭제되지 않습니다</em>.<br/>
          • Redis의 누적 카운터(stock:daily_buy:*, stock:daily_sell:*)만 0으로 리셋됩니다.<br/>
          • 캐시 초기화 후 첫 매수 시도 시 DB에서 오늘 거래 내역을 다시 합산하여 캐시 재구축됩니다.<br/>
          • 따라서 이미 체결된 거래의 손익/포지션은 그대로 유지됩니다.
        </span></p>
      <p class="help-note">
        💡 <strong>주의</strong><br/>
        수동 실행은 장 운영 시간(09:00~15:30 KST)에만 실제 매매가 발생합니다.<br/>
        장 마감 후에는 SKIP 처리됩니다.
      </p>
    `
  },
  marketStatus: {
    title: '📊 시장 상태 안내',
    content: `
      <p class="help-intro">한국 증시의 현재 운영 상태와 봇 작동 가능 여부를 표시합니다.</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>장 운영 여부</strong>
        <span class="help-desc">정규장(09:00~15:30 KST) 운영 중인지 표시합니다. 장마감 시 봇은 자동매매를 중단합니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>긴급 정지</strong>
        <span class="help-desc">누적 손실 한도 도달, 연속 손절 한도 초과 등의 사유로 봇이 강제 중지된 상태입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>내일 휴장</strong>
        <span class="help-desc">다음 영업일이 휴장일인 경우 알림을 표시합니다 (주말/공휴일/임시휴장).</span></p>
      <p class="help-note">💡 한국 증시 정규장 시간은 09:00 ~ 15:30 (KST) 입니다.</p>
    `
  },
  holdingWarnings: {
    title: '⚠️ 보유기간 경고 안내',
    content: `
      <p class="help-intro">레버리지 ETF의 변동성 끌림(decay) 위험으로 인해 장기 보유는 권장되지 않습니다.</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>경고 (15일 이상)</strong>
        <span class="help-desc">
          15일 이상 보유 중인 종목입니다.<br/>
          매도를 검토해보세요.
        </span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>긴급 청산 권장 (20일 이상)</strong>
        <span class="help-desc">
          20일 이상 보유 중인 종목입니다.<br/>
          변동성 끌림 효과가 누적되었을 가능성이 있으니 매도를 강력히 권장합니다.
        </span></p>
      <p class="help-note">💡 매일 09:05 KST에 보유기간 경고가 자동으로 점검되어 알림이 발송됩니다.</p>
    `
  },
  indicators: {
    title: '📊 기술적 지표 안내',
    content: `
      <p class="help-intro">각 종목의 기술적 지표와 매매 신호를 실시간으로 확인할 수 있습니다.</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>RSI (상대강도지수)</strong>
        <span class="help-desc">35 이하: 과매도 구간 (매수 신호)<br/>65 이상: 과매수 구간 (매도 신호)<br/>35~65: 중립 구간<br/>※ 코인(30/70) 대비 ETF 변동성 특성을 반영한 임계값</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>볼린저밴드 위치</strong>
        <span class="help-desc">하단 접근: 가격이 밴드 하단에 근접 (매수 신호)<br/>상단 접근: 가격이 밴드 상단에 근접 (매도 신호)<br/>중간: 밴드 중앙 부근</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>거래량 비율</strong>
        <span class="help-desc">1.2x 이상: 평균 대비 거래량 급증 (신호 신뢰도 상승)<br/>※ 코인(1.5x) 대비 ETF 특성에 맞춘 임계값</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>MA 위치</strong>
        <span class="help-desc">20일 이동평균선 대비 현재가 위치. 음수(-)는 MA 아래(매수 검토), 양수(+)는 MA 위</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>신호</strong>
        <span class="help-desc">강력 매수: 3개 이상 조건 충족<br/>매수 검토: 2개 조건 충족<br/>관망: 1개 조건 충족<br/>대기: 조건 미충족</span></p>
      <p class="help-note">💡 <strong>Tip:</strong> 신호는 참고용이며, 실제 매매는 거래 설정의 조건에 따라 장 운영 시간에만 실행됩니다.</p>
    `
  },
  emailTest: {
    title: '📧 이메일 테스트 발송',
    content: `
      <p class="help-intro">각 유형별 알림 이메일이 정상적으로 발송되는지 테스트합니다.</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>매수</strong>
        <span class="help-desc">주식 매수 시 발송되는 알림 이메일 샘플</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>매도</strong>
        <span class="help-desc">주식 매도 시 발송되는 알림 이메일 샘플</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>리포트</strong>
        <span class="help-desc">일일 거래 리포트 이메일 샘플</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>보유기간 경고</strong>
        <span class="help-desc">레버리지 ETF 장기 보유 경고 이메일 샘플</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>가중치</strong>
        <span class="help-desc">AI 뉴스 분석에 따른 가중치 변경 알림 샘플</span></p>
      <p class="help-note">
        💡 <strong>현재 안내</strong><br/>
        주식 전용 알림 템플릿은 Day 63에 정식 추가될 예정이며, 현재는 코인용 템플릿으로 발송됩니다.<br/>
        프로필 설정에서 이메일을 먼저 등록해야 테스트가 가능합니다.
      </p>
    `
  },
  discordTest: {
    title: '💬 디스코드 DM 테스트',
    content: `
      <p class="help-intro">디스코드 봇을 통한 DM 알림이 정상적으로 발송되는지 테스트합니다.</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>"Discord 봇 서버: 온라인" 칩</strong>
        <span class="help-desc">
          서버 측 JDA Discord 봇 자체가 살아있는지를 의미합니다.<br/>
          <em>이 페이지의 "주식 자동매매 봇" 상태와는 별개</em>이며, Discord 봇이 온라인이어야 DM 발송이 가능합니다.
        </span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>매수 / 매도 / 손절매</strong>
        <span class="help-desc">각 거래 시 발송되는 DM 샘플</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>리포트</strong>
        <span class="help-desc">일일 거래 리포트 DM 샘플</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>가중치</strong>
        <span class="help-desc">AI 뉴스 분석에 따른 가중치 변경 알림 DM 샘플</span></p>
      <p class="help-note">
        💡 <strong>현재 안내</strong><br/>
        주식 전용 DM 템플릿은 Day 63에 정식 추가될 예정이며, 현재는 코인용 템플릿으로 발송됩니다.<br/>
        Discord User ID는 개발자 모드를 켜고 본인 프로필에서 "ID 복사"로 확인할 수 있습니다.
      </p>
    `
  }
}

// 상태 변수
const loading = ref(false)
const executing = ref(false)
const refreshing = ref(false)
const resettingCache = ref(false)
const indicators = ref<StockIndicatorResult[]>([])
const lastUpdated = ref('')
const executionResult = ref<any>(null)

// 종목 마스터 (코드 → 종목명 변환)
const stockMasterMap = ref<Map<string, string>>(new Map())

// Snackbar
const snackbar = ref(false)
const snackbarMessage = ref('')
const snackbarColor = ref('success')

// 오늘 통계 (대시보드 stats API에서 가져옴)
const todayStats = ref({
  buyCount: 0,
  sellCount: 0
})

const botRunning = ref(false)
const lastExecutionTime = ref<string | null>(null)
const nextExecutionTime = ref<string | null>(null)
const botEnabled = ref(false)
const botToggleLoading = ref(false)
const marketOpen = ref(false)
const emergencyStop = ref(false)
const tomorrowHoliday = ref(false)
const holdingWarnings = ref<HoldingWarning[]>([])

const countdownSeconds = ref(0)
const countdownText = ref('계산 중...')
let countdownInterval: ReturnType<typeof setInterval> | null = null

// 봇 상태 라벨 (Phase 1과 동일 패턴, 주식은 장운영 여부 반영)
const botStatusLabel = computed(() => {
  if (!botEnabled.value) return '중지됨'
  if (emergencyStop.value) return '긴급정지'
  if (!marketOpen.value) return '장마감 대기'
  return botRunning.value ? '실행 중' : '대기 중'
})

// 사용자 프로필 정보
const userProfile = ref({
  email: '',
  discordUserId: ''
})

// Discord Bot 상태
const discordBotEnabled = ref(false)

// 이메일 테스트 로딩 상태
// ⭐ [Day 61 후속] weight 항목 추가
const emailTestLoading = ref<Record<string, boolean>>({
  buy: false,
  sell: false,
  report: false,
  holding: false,
  weight: false
})

// 디스코드 테스트 로딩 상태
// ⭐ [Day 61 후속] weight 항목 추가
const discordTestLoading = ref<Record<string, boolean>>({
  buy: false,
  sell: false,
  stoploss: false,
  report: false,
  weight: false
})

// ⭐⭐⭐ [Day 61 후속 추가] 데모 모드 (시연/개발용 가짜 데이터) ⭐⭐⭐
// 실제 백엔드 API 호출 없이 가짜 지표/경고 데이터를 보여주는 모드
const demoMode = ref(false)

// 테이블 헤더
const indicatorHeaders = [
  { title: '종목', key: 'stockCode', sortable: true },
  { title: '현재가', key: 'currentPrice', sortable: true },
  { title: 'MA20', key: 'ma20', sortable: true },
  { title: 'RSI', key: 'rsi', sortable: true },
  { title: 'BB 위치', key: 'bbPosition', sortable: false },
  { title: '거래량', key: 'volumeRatio', sortable: true },
  { title: 'MA 대비', key: 'maPosition', sortable: false },
  { title: '신호', key: 'signal', sortable: false },
]

// 종목 마스터 조회 (종목코드 → 종목명 매핑)
const fetchStockMaster = async () => {
  try {
    const res = await stockInfoApi.getActiveStocks()
    const list: StockInfo[] = (res.data || res) as StockInfo[]
    const map = new Map<string, string>()
    list.forEach(s => map.set(s.stockCode, s.stockName))
    stockMasterMap.value = map
  } catch (error) {
    console.error('[주식봇 모니터] 종목 마스터 조회 실패:', error)
  }
}

const getStockName = (stockCode: string): string => {
  return stockMasterMap.value.get(stockCode) || stockCode
}

// 사용자 프로필 조회
const fetchUserProfile = async () => {
  try {
    const response = await api.get('/user/profile')
    const userData = response.data || response
    userProfile.value = {
      email: userData.email || '',
      discordUserId: userData.discordUserId || ''
    }
  } catch (error) {
    console.error('프로필 조회 실패:', error)
    userProfile.value = { email: '', discordUserId: '' }
  }
}

// Discord Bot 상태 조회
const fetchDiscordBotStatus = async () => {
  try {
    const response = await api.get('/notifications/discord/bot-status')
    const statusData = response.data || response
    discordBotEnabled.value = statusData?.botEnabled || false
  } catch (error) {
    console.error('Discord Bot 상태 조회 실패:', error)
    discordBotEnabled.value = false
  }
}

// 봇 상태 조회 (주식 봇 - /api/stock/bot/status)
const fetchBotStatus = async () => {
  try {
    const response = await stockBotApi.getStatus()
    const data = (response.data || response) as any

    botRunning.value = data.isRunning ?? false
    botEnabled.value = data.botEnabled ?? false
    marketOpen.value = data.marketOpen ?? false
    emergencyStop.value = data.emergencyStop ?? false
    tomorrowHoliday.value = data.tomorrowHoliday ?? false
    lastExecutionTime.value = data.lastExecutionTime || null
    nextExecutionTime.value = data.nextExecutionTime || null
    countdownSeconds.value = data.secondsUntilNextExecution || 0

    startCountdown()
  } catch (error) {
    console.error('[주식봇 모니터] 봇 상태 조회 실패:', error)
  }
}

// 봇 활성/비활성 토글
const toggleBot = async () => {
  botToggleLoading.value = true
  try {
    if (botEnabled.value) {
      await stockBotApi.start()
      showSnackbar('주식 자동매매 봇이 시작되었습니다.', 'success')
    } else {
      await stockBotApi.stop()
      showSnackbar('주식 자동매매 봇이 중지되었습니다.', 'warning')
    }
    await fetchBotStatus()
  } catch (error) {
    console.error('[주식봇 모니터] 봇 상태 변경 실패:', error)
    showSnackbar('봇 상태 변경에 실패했습니다.', 'error')
    botEnabled.value = !botEnabled.value // 실패 시 원래 값으로 복원
  } finally {
    botToggleLoading.value = false
  }
}

const startCountdown = () => {
  if (countdownInterval) clearInterval(countdownInterval)

  updateCountdownText()

  countdownInterval = setInterval(() => {
    if (countdownSeconds.value > 0) {
      countdownSeconds.value--
      updateCountdownText()
    } else {
      fetchBotStatus()
    }
  }, 1000)
}

const updateCountdownText = () => {
  const seconds = countdownSeconds.value

  if (!botEnabled.value) {
    countdownText.value = '중단 상태입니다'
    return
  }

  if (!marketOpen.value) {
    countdownText.value = '장마감 (대기)'
    return
  }

  if (seconds <= 0) {
    countdownText.value = '곧 실행'
    return
  }

  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60

  countdownText.value = mins > 0 ? `${mins}분 ${secs}초 후` : `${secs}초 후`
}

// 봇 시간 포맷 (대시보드와 동일 패턴)
const formatBotTimeDisplay = (dateStr: string | null | undefined): string => {
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

// 지표 조회
const fetchIndicators = async () => {
  loading.value = true
  try {
    const response = await stockBotApi.getIndicators()
    const indicatorData = (response.data || response) as any
    indicators.value = Array.isArray(indicatorData) ? indicatorData : []
    lastUpdated.value = new Date().toLocaleTimeString('ko-KR')
  } catch (error) {
    console.error('[주식봇 모니터] 지표 조회 실패:', error)
    showSnackbar('지표 조회에 실패했습니다. KIS API 키 등록 여부를 확인해주세요.', 'error')
    indicators.value = []
  } finally {
    loading.value = false
  }
}

// 보유기간 경고 조회
const fetchHoldingWarnings = async () => {
  try {
    const response = await stockBotApi.getHoldingWarnings()
    const data = (response.data || response) as any
    holdingWarnings.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('[주식봇 모니터] 보유기간 경고 조회 실패:', error)
    holdingWarnings.value = []
  }
}

// ⭐⭐⭐ [Day 61 후속 추가] 데모 모드 토글 핸들러 ⭐⭐⭐
// ON: 가짜 지표 5개 + 보유기간 경고 2개 + 오늘 통계 표시
// OFF: 실제 API 재호출
const onDemoModeToggle = async () => {
  if (demoMode.value) {
    // 가짜 지표 데이터 생성 (TIGER 미국나스닥100레버리지 외 5종목)
    indicators.value = [
      {
        market: '409820',
        currentPrice: 18750,
        ma7: 19120, ma14: 19400, ma20: 19625, ma30: 19850,
        rsi14: 32.5,
        bbUpper: 20850, bbMiddle: 19625, bbLower: 18400,
        avgVolume: 1250000, currentVolume: 1820000,
        volumeRatio: 1.46,
        belowMA20: true, rsiBuySignal: true, rsiSellSignal: false,
        belowBBLower: false, aboveBBUpper: false, highVolume: true,
        calculatedAt: new Date().toISOString()
      },
      {
        market: '409810',
        currentPrice: 14250,
        ma7: 14380, ma14: 14520, ma20: 14680, ma30: 14820,
        rsi14: 41.8,
        bbUpper: 15420, bbMiddle: 14680, bbLower: 13940,
        avgVolume: 980000, currentVolume: 1080000,
        volumeRatio: 1.10,
        belowMA20: true, rsiBuySignal: false, rsiSellSignal: false,
        belowBBLower: false, aboveBBUpper: false, highVolume: false,
        calculatedAt: new Date().toISOString()
      },
      {
        market: '252670',
        currentPrice: 5810,
        ma7: 5750, ma14: 5680, ma20: 5650, ma30: 5620,
        rsi14: 68.2,
        bbUpper: 5920, bbMiddle: 5650, bbLower: 5380,
        avgVolume: 5400000, currentVolume: 6800000,
        volumeRatio: 1.26,
        belowMA20: false, rsiBuySignal: false, rsiSellSignal: true,
        belowBBLower: false, aboveBBUpper: true, highVolume: true,
        calculatedAt: new Date().toISOString()
      },
      {
        market: '233740',
        currentPrice: 12350,
        ma7: 12180, ma14: 12050, ma20: 11950, ma30: 11820,
        rsi14: 55.4,
        bbUpper: 13150, bbMiddle: 11950, bbLower: 10750,
        avgVolume: 2100000, currentVolume: 1950000,
        volumeRatio: 0.93,
        belowMA20: false, rsiBuySignal: false, rsiSellSignal: false,
        belowBBLower: false, aboveBBUpper: false, highVolume: false,
        calculatedAt: new Date().toISOString()
      },
      {
        market: '465580',
        currentPrice: 9820,
        ma7: 10120, ma14: 10380, ma20: 10520, ma30: 10650,
        rsi14: 28.7,
        bbUpper: 11150, bbMiddle: 10520, bbLower: 9890,
        avgVolume: 850000, currentVolume: 1320000,
        volumeRatio: 1.55,
        belowMA20: true, rsiBuySignal: true, rsiSellSignal: false,
        belowBBLower: true, aboveBBUpper: false, highVolume: true,
        calculatedAt: new Date().toISOString()
      }
    ]

    // 가짜 보유기간 경고 (15일 / 21일)
    // ⭐ [Day 61 후속 v3] 동일 종목(409820) 두 건을 매수일/수량/금액 다르게 → 다중 보유 시연
    holdingWarnings.value = [
      {
        stockCode: '409820',
        holdingDays: 21,
        urgent: true,
        buyDate: '2026-04-01',
        transactionId: 1001,
        quantity: 50,
        buyPrice: 19500,
        totalAmount: 975000
      },
      {
        stockCode: '409820',
        holdingDays: 16,
        urgent: false,
        buyDate: '2026-04-08',
        transactionId: 1002,
        quantity: 30,
        buyPrice: 19200,
        totalAmount: 576000
      },
      {
        stockCode: '252670',
        holdingDays: 18,
        urgent: false,
        buyDate: '2026-04-04',
        transactionId: 1003,
        quantity: 200,
        buyPrice: 5900,
        totalAmount: 1180000
      }
    ]

    // 가짜 종목 마스터 (코드 → 종목명 매핑)
    const demoMasterMap = new Map<string, string>([
      ['409820', 'TIGER 미국나스닥100레버리지(합성)'],
      ['409810', 'KODEX 미국나스닥100레버리지(합성H)'],
      ['252670', 'KODEX 200선물인버스2X'],
      ['233740', 'KODEX 코스닥150 레버리지'],
      ['465580', 'TIGER 차이나전기차SOLACTIVE']
    ])
    // 기존 마스터에 없는 데모 종목 추가
    demoMasterMap.forEach((name, code) => {
      if (!stockMasterMap.value.has(code)) {
        stockMasterMap.value.set(code, name)
      }
    })

    // 가짜 오늘 통계
    todayStats.value = { buyCount: 2, sellCount: 1 }

    lastUpdated.value = new Date().toLocaleTimeString('ko-KR') + ' (데모)'
    showSnackbar('🎬 데모 모드 활성화: 가짜 데이터를 표시 중입니다.', 'info')
  } else {
    // 데모 모드 OFF → 실제 데이터 재조회
    await Promise.all([
      fetchIndicators(),
      fetchHoldingWarnings(),
      fetchTodayStats()
    ])
    showSnackbar('실제 데이터로 복귀했습니다.', 'success')
  }
}

// 오늘 거래 통계 조회 (주식 대시보드 stats API 활용)
const fetchTodayStats = async () => {
  try {
    const response = await api.get('/stock/dashboard/stats')
    const data = (response.data || response) as any
    todayStats.value = {
      buyCount: data.todayBuyCount || data.buyCount || 0,
      sellCount: data.todaySellCount || data.sellCount || 0
    }
  } catch (error) {
    console.error('[주식봇 모니터] 오늘 통계 조회 실패:', error)
    todayStats.value = { buyCount: 0, sellCount: 0 }
  }
}

// 봇 실행
const executeBot = async () => {
  executing.value = true
  executionResult.value = null
  try {
    const result = await stockBotApi.execute()
    executionResult.value = (result.data || result) as any
    const buyCount = executionResult.value?.buyCount || 0
    const sellCount = executionResult.value?.sellCount || 0
    showSnackbar(`실행 완료: 매수 ${buyCount}건, 매도 ${sellCount}건`, 'success')
    await Promise.all([fetchIndicators(), fetchTodayStats()])
  } catch (error) {
    console.error('[주식봇 모니터] 봇 실행 실패:', error)
    showSnackbar('봇 실행에 실패했습니다.', 'error')
  } finally {
    executing.value = false
  }
}

// 지표 새로고침
// ⭐ [Day 61 후속 수정] 데모 모드일 때 실제 API 호출 안 하도록 가드
const refreshIndicators = async () => {
  if (demoMode.value) {
    showSnackbar('데모 모드에서는 실제 API를 호출하지 않습니다.', 'info')
    return
  }
  refreshing.value = true
  await fetchIndicators()
  refreshing.value = false
  showSnackbar('지표가 새로고침되었습니다.', 'success')
}

// 일일 한도 초기화 (백엔드 메서드명은 clearStockDailyCache - Redis 캐시 삭제로 결과적으로 한도가 초기화됨)
const resetDailyCache = async () => {
  resettingCache.value = true
  try {
    await stockBotApi.resetDailyCache()
    showSnackbar('일일 거래 한도가 초기화되었습니다.', 'success')
    await fetchTodayStats()
  } catch (error) {
    console.error('[주식봇 모니터] 일일 한도 초기화 실패:', error)
    showSnackbar('한도 초기화에 실패했습니다.', 'error')
  } finally {
    resettingCache.value = false
  }
}

// 이메일 테스트 발송
// ⭐⭐⭐ [Day 61 후속 수정] 폴백 로직 제거 → 처음부터 코인용 엔드포인트로 직접 호출 ⭐⭐⭐
// 이유:
//   - 백엔드에 /notifications/email/test-stock-* 엔드포인트가 아직 미구현 → 500 에러 발생
//   - GlobalExceptionHandler가 NoHandlerFoundException 활성화 안 되어 있어 404 폴백 동작 X
//   - Day 63 작업으로 주식 전용 알림 엔드포인트 추가 예정. 그때 다시 주식용으로 변경
//   - weight 항목은 AI 뉴스 가중치 기능으로, Phase 2 신규 항목 (Phase2_Implementation_Plan에 추가)
const sendEmailTest = async (type: 'buy' | 'sell' | 'report' | 'holding' | 'weight') => {
  emailTestLoading.value[type] = true
  try {
    let endpoint = ''
    let successMsg = ''

    switch (type) {
      case 'buy':
        endpoint = '/notifications/email/test-buy'
        successMsg = '매수 체결 테스트 이메일이 발송되었습니다. (현재는 코인용 템플릿)'
        break
      case 'sell':
        endpoint = '/notifications/email/test-sell'
        successMsg = '매도 체결 테스트 이메일이 발송되었습니다. (현재는 코인용 템플릿)'
        break
      case 'report':
        endpoint = '/notifications/email/daily-report'
        successMsg = '일일 리포트 테스트 이메일이 발송되었습니다. (현재는 코인용 템플릿)'
        break
      case 'holding':
        // 보유기간 경고 이메일은 Day 63에서 정식 추가 예정 → 현재는 매수 테스트로 대체
        endpoint = '/notifications/email/test-buy'
        successMsg = '보유기간 경고 알림 (Day 63에서 정식 추가 예정 - 현재는 임시 발송)'
        break
      case 'weight':
        endpoint = '/notifications/email/test-weight-change'
        successMsg = 'AI 뉴스 가중치 변경 테스트 이메일이 발송되었습니다. (현재는 코인용 템플릿)'
        break
    }

    await api.post(endpoint)
    showSnackbar(successMsg, type === 'holding' ? 'info' : 'success')
  } catch (error) {
    console.error('[주식봇 모니터] 이메일 테스트 발송 실패:', error)
    showSnackbar('이메일 발송에 실패했습니다.', 'error')
  } finally {
    emailTestLoading.value[type] = false
  }
}

// 디스코드 테스트 발송
// ⭐⭐⭐ [Day 61 후속 수정] 폴백 제거 → 처음부터 코인용 엔드포인트로 직접 호출 ⭐⭐⭐
// (이메일과 동일한 사유 - Day 63에서 주식용 정식 추가 예정)
const sendDiscordTest = async (type: 'buy' | 'sell' | 'stoploss' | 'report' | 'weight') => {
  discordTestLoading.value[type] = true
  try {
    let endpoint = ''
    let successMsg = ''

    switch (type) {
      case 'buy':
        endpoint = '/notifications/discord/test-buy'
        successMsg = '매수 알림 테스트 DM이 발송되었습니다. (현재는 코인용 템플릿)'
        break
      case 'sell':
        endpoint = '/notifications/discord/test-sell'
        successMsg = '매도 알림 테스트 DM이 발송되었습니다. (현재는 코인용 템플릿)'
        break
      case 'stoploss':
        endpoint = '/notifications/discord/test-stoploss'
        successMsg = '손절매 알림 테스트 DM이 발송되었습니다. (현재는 코인용 템플릿)'
        break
      case 'report':
        endpoint = '/notifications/discord/test-daily-report'
        successMsg = '일일 리포트 테스트 DM이 발송되었습니다. (현재는 코인용 템플릿)'
        break
      case 'weight':
        endpoint = '/notifications/discord/test-weight-change'
        successMsg = 'AI 뉴스 가중치 변경 테스트 DM이 발송되었습니다. (현재는 코인용 템플릿)'
        break
    }

    await api.post(endpoint)
    showSnackbar(successMsg, 'success')
  } catch (error) {
    console.error('[주식봇 모니터] 디스코드 테스트 발송 실패:', error)
    showSnackbar('디스코드 DM 발송에 실패했습니다.', 'error')
  } finally {
    discordTestLoading.value[type] = false
  }
}

// 유틸리티 함수
const formatPrice = (price: any): string => {
  if (price === null || price === undefined || price === '') return '-'
  const num = Number(price)
  if (isNaN(num) || num === 0) return '-'
  return new Intl.NumberFormat('ko-KR').format(Math.round(num)) + '원'
}

// RSI 색상 - 주식은 35/65 임계값 적용
const getRsiColor = (rsi: any): string => {
  if (rsi === null || rsi === undefined) return 'grey'
  const num = Number(rsi)
  if (isNaN(num)) return 'grey'
  if (num <= 35) return 'success'
  if (num >= 65) return 'error'
  return 'grey'
}

const getBbPosition = (item: StockIndicatorResult): string => {
  if (!item.currentPrice || !item.bbLower || !item.bbUpper) return '-'
  const range = Number(item.bbUpper) - Number(item.bbLower)
  if (range === 0) return '-'
  const position = (Number(item.currentPrice) - Number(item.bbLower)) / range * 100
  if (position <= 20) return '하단'
  if (position >= 80) return '상단'
  return '중간'
}

const getBbPositionColor = (item: StockIndicatorResult): string => {
  const position = getBbPosition(item)
  if (position === '하단') return 'success'
  if (position === '상단') return 'error'
  return 'grey'
}

const getMaPosition = (item: StockIndicatorResult): string => {
  if (!item.currentPrice || !item.ma20) return '-'
  const cur = Number(item.currentPrice)
  const ma = Number(item.ma20)
  if (ma === 0) return '-'
  const diff = ((cur - ma) / ma * 100).toFixed(2)
  return `${Number(diff) > 0 ? '+' : ''}${diff}%`
}

const getMaPositionClass = (item: StockIndicatorResult): string => {
  if (!item.currentPrice || !item.ma20) return ''
  return Number(item.currentPrice) < Number(item.ma20) ? 'text-success' : 'text-error'
}

// 신호 - 주식은 RSI 35, 거래량 1.2x 임계값 적용
const getSignal = (item: StockIndicatorResult): string => {
  let score = 0

  if (item.rsi14 !== null && item.rsi14 !== undefined && Number(item.rsi14) <= 35) score++
  if (item.bbLower && item.currentPrice &&
      Number(item.currentPrice) <= Number(item.bbLower) * 1.02) score++
  if (item.ma20 && item.currentPrice &&
      Number(item.currentPrice) < Number(item.ma20)) score++
  if (item.volumeRatio && Number(item.volumeRatio) >= 1.2) score++

  if (score >= 3) return '강력 매수'
  if (score >= 2) return '매수 검토'
  if (score === 1) return '관망'
  return '대기'
}

const getSignalColor = (item: StockIndicatorResult): string => {
  const signal = getSignal(item)
  if (signal === '강력 매수') return 'success'
  if (signal === '매수 검토') return 'primary'
  if (signal === '관망') return 'warning'
  return 'grey'
}

const showSnackbar = (message: string, color: string) => {
  snackbarMessage.value = message
  snackbarColor.value = color
  snackbar.value = true
}

// 정기 갱신 인터벌
let indicatorsRefreshInterval: ReturnType<typeof setInterval> | null = null
let statusRefreshInterval: ReturnType<typeof setInterval> | null = null

// 마운트 시 데이터 로드
onMounted(async () => {
  await fetchStockMaster()  // 종목 마스터 먼저 로드
  await Promise.all([
    fetchIndicators(),
    fetchUserProfile(),
    fetchDiscordBotStatus(),
    fetchBotStatus(),
    fetchHoldingWarnings(),
    fetchTodayStats()
  ])

  // ⭐ [Day 61 후속 수정] 30초마다 지표 새로고침 (단, 데모 모드 OFF일 때만)
  indicatorsRefreshInterval = setInterval(() => {
    if (!demoMode.value) fetchIndicators()
  }, 30000)
  // ⭐ [Day 61 후속 수정] 5분마다 봇 상태 새로고침 (단, 데모 모드 OFF일 때만 보유경고/오늘통계 갱신)
  statusRefreshInterval = setInterval(() => {
    fetchBotStatus()  // 봇 상태는 항상 갱신 (실제 봇 상태 정확히 표시)
    if (!demoMode.value) {
      fetchHoldingWarnings()
      fetchTodayStats()
    }
  }, 300000)
})

onUnmounted(() => {
  if (countdownInterval) clearInterval(countdownInterval)
  if (indicatorsRefreshInterval) clearInterval(indicatorsRefreshInterval)
  if (statusRefreshInterval) clearInterval(statusRefreshInterval)
})
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

.text-warning {
  color: #FF9800 !important;
  font-weight: bold;
}

/* ⭐ [Day 61 v5 추가] 보유기간 경고 - "경고" 단계의 보유기간 일수 표시용 (검정 굵게) */
/* 노랑색(text-warning)이 흰 배경 + 옅은 노랑 카드에서 가독성 떨어지는 문제 해결 */
.text-strong-black {
  color: #212121 !important;
  font-weight: 700;
}

/* ⭐ [Day 61 v5 추가] 데모 모드 카드 - 검정 글씨 강제 (amber tonal 배경에서 가독성 확보) */
.demo-mode-caption {
  color: #212121 !important;
}

.demo-mode-title {
  color: #212121 !important;
}

.bot-stats-card {
  height: 100%;
  min-height: 120px;
}

.bot-stats-card .d-flex {
  height: 100%;
  align-items: center;
}

.bot-stats-card .text-h4,
.bot-stats-card .text-h5 {
  white-space: nowrap;
}

.gap-2 {
  gap: 8px;
}

.gap-3 {
  gap: 12px;
}

.control-card {
  height: 100%;
  min-height: 280px;
}

/* 시장 상태 행 */
.status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #eee;
}

.status-row:last-child {
  border-bottom: none;
}

.status-label {
  font-size: 14px;
  color: #616161;
  font-weight: 500;
}

.status-value {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

/* 보유기간 경고 - v2 */
.warning-urgent {
  background-color: #FFEBEE;
  border-left: 4px solid #F44336;
  margin-bottom: 4px;
}

.warning-normal {
  background-color: #FFF8E1;
  border-left: 4px solid #FFA000;
  margin-bottom: 4px;
}

/* ⭐ [Day 61 후속 v3] 보유기간 경고 풍부 표시용 스타일 */
.warning-list-wrapper {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 220px;
  overflow-y: auto;
}

.warning-item {
  padding: 10px 12px;
  border-radius: 4px;
}

.warning-header {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}

.warning-stock-name {
  font-size: 14px;
  color: #212121;
}

.warning-details {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  padding-left: 28px;
  color: #424242;
}

.detail-item {
  display: inline-flex;
  align-items: baseline;
  gap: 3px;
}

.detail-label {
  color: #757575;
}

.detail-value {
  color: #212121;
  font-weight: 500;
}

.detail-divider {
  color: #BDBDBD;
  font-size: 11px;
}

.lh-1 {
  line-height: 1.2;
}

/* ⭐ [Day 61 후속 v3] Discord 봇 서버 상태 칩 - 카드 우측 하단 정렬 */
.discord-bot-status-row {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

/* ⭐ [Day 61 후속 v3] 회색 칩일 때 글씨를 검정으로 강제 (가독성 확보) */
/* Vuetify 3의 grey 색상 칩은 기본 글씨가 회색에 가까운 짙은 색이지만, 배경 회색과 명도 차이가 작아 식별 어려움 */
:deep(.v-chip.chip-text-black .v-chip__content) {
  color: #212121 !important;
  font-weight: 600;
}

:deep(.help-content .help-intro) {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e0e0e0;
  color: #424242;
  font-size: 14px;
}

:deep(.help-content .help-item) {
  margin-bottom: 16px;
  padding-left: 8px;
}

:deep(.help-content .help-bullet) {
  color: #1565C0;
  font-weight: bold;
  margin-right: 6px;
}

:deep(.help-content .help-desc) {
  display: block;
  padding-left: 20px;
  margin-top: 4px;
  color: #616161;
  font-size: 13px;
}

:deep(.help-content .help-note) {
  margin-top: 16px;
  padding: 10px 12px;
  background-color: #FFF8E1;
  border-left: 3px solid #FFA000;
  border-radius: 4px;
  color: #5D4037;
  font-size: 13px;
}

.bot-schedule-card .schedule-label-row {
  font-size: 13px;
  opacity: 0.9;
  text-align: left;
}

.bot-schedule-card .schedule-value-row {
  font-size: 16px;
  font-weight: 700;
  text-align: right;
}

.bot-schedule-card .countdown-row {
  font-size: 15px;
  font-weight: 700;
  text-align: right;
  color: #FFD54F;
  margin-top: 2px;
}
</style>