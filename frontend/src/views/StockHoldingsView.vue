<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main class="bg-grey-lighten-3">
      <v-container fluid>
        <v-row class="mb-4">
          <v-col>
            <h1 class="text-h4">
              <v-icon class="mr-2">mdi-briefcase-outline</v-icon>
              보유 주식 자산
            </h1>
            <p class="text-subtitle-1 text-grey">기간별/주식별 수익 분석 및 보유 현황을 확인하세요</p>
          </v-col>
        </v-row>

        <!-- ⭐ 코인 HoldingsView.vue 1:1 차용: 상단 영역 - 기간별/주식별 수익 탭 -->
        <v-row>
          <v-col cols="12">
            <v-tabs
              v-model="profitTab"
              color="grey-darken-3"
              class="profit-tabs mb-0"
              height="48"
            >
              <v-tab value="period" class="profit-tab text-body-1 font-weight-bold">
                <v-icon start size="20">mdi-chart-timeline-variant</v-icon>
                기간별 수익
              </v-tab>
              <v-tab value="stock" class="profit-tab text-body-1 font-weight-bold">
                <v-icon start size="20">mdi-chart-line</v-icon>
                주식별 수익
              </v-tab>
            </v-tabs>

            <v-card elevation="2" class="card-no-top-radius">
              <v-window v-model="profitTab">
                <!-- ========== 기간별 수익 탭 ========== -->
                <v-window-item value="period">
                  <v-card-title class="py-3 px-4 bg-indigo-darken-1 text-white d-flex align-center">
                    <v-icon class="mr-2" size="20">mdi-chart-timeline-variant</v-icon>
                    <span class="text-body-1 font-weight-bold">기간별 수익 분석</span>
                    <HelpButton
                      use-dialog
                      :dialog-title="helpContents.periodProfit.title"
                      :dialog-content="helpContents.periodProfit.content"
                    />
                    <v-spacer />
                    <!-- 기간 선택 + 일자 + 조회 (한 줄) -->
                    <div class="d-flex align-center">
                      <v-btn-toggle
                        v-model="selectedPeriod"
                        mandatory
                        density="compact"
                        variant="outlined"
                        divided
                        class="period-toggle-header mr-3"
                      >
                        <v-btn value="today">오늘</v-btn>
                        <v-btn value="month">이번달</v-btn>
                        <v-btn value="year">올해</v-btn>
                        <v-btn value="oneYear">1년</v-btn>
                        <v-btn value="total">누적</v-btn>
                      </v-btn-toggle>

                      <input
                        v-model="customStartDate"
                        type="date"
                        class="custom-date-input"
                      />
                      <span class="text-white mx-1">~</span>
                      <input
                        v-model="customEndDate"
                        type="date"
                        class="custom-date-input"
                      />
                      <v-btn
                        color="white"
                        variant="flat"
                        class="ml-2 text-indigo-darken-2 custom-search-btn"
                        @click="applyCustomPeriod"
                      >
                        조회
                      </v-btn>
                    </div>
                  </v-card-title>

                  <v-card-text class="pa-4">
                    <!-- 수익 요약 카드 -->
                    <v-card variant="outlined" class="mb-4 profit-summary-card">
                      <v-card-text class="pa-4">
                        <v-row align="center">
                          <v-col cols="12" md="4" class="text-center">
                            <div class="text-caption text-grey-darken-1 mb-1">
                              {{ getPeriodLabel(selectedPeriod) }} 수익
                            </div>
                            <div class="text-h4 font-weight-bold text-teal-darken-2">
                              +{{ formatCurrency(0) }}
                            </div>
                            <v-chip color="teal" size="small" variant="flat" class="mt-2">
                              +0.00%
                            </v-chip>
                          </v-col>

                          <v-divider vertical class="d-none d-md-block" />

                          <v-col cols="12" md="8">
                            <v-row dense>
                              <v-col cols="6" sm="3">
                                <div class="text-caption text-grey-darken-1">거래 건수</div>
                                <div class="text-h6 font-weight-medium">0건</div>
                              </v-col>
                              <v-col cols="6" sm="3">
                                <div class="text-caption text-grey-darken-1">익절 / 손절</div>
                                <div class="text-body-1">
                                  <span class="text-teal font-weight-medium">0</span>
                                  <span class="mx-1">/</span>
                                  <span class="text-red font-weight-medium">0</span>
                                </div>
                              </v-col>
                              <v-col cols="6" sm="3">
                                <div class="text-caption text-grey-darken-1">승률</div>
                                <div class="text-h6 font-weight-medium">0.0%</div>
                              </v-col>
                              <v-col cols="6" sm="3">
                                <div class="text-caption text-grey-darken-1">건당 평균</div>
                                <div class="text-body-1 font-weight-medium text-teal">
                                  {{ formatCurrency(0) }}
                                </div>
                              </v-col>
                              <v-col cols="6" sm="3" class="mt-2">
                                <div class="text-caption text-grey-darken-1">최대 수익</div>
                                <div class="text-body-1 text-teal font-weight-medium">{{ formatCurrency(0) }}</div>
                              </v-col>
                              <v-col cols="6" sm="3" class="mt-2">
                                <div class="text-caption text-grey-darken-1">최대 손실</div>
                                <div class="text-body-1 text-red font-weight-medium">{{ formatCurrency(0) }}</div>
                              </v-col>
                              <v-col cols="12" sm="6" class="mt-2">
                                <div class="text-caption text-grey-darken-1">조회 기간</div>
                                <div class="text-body-2">- ~ -</div>
                              </v-col>
                            </v-row>
                          </v-col>
                        </v-row>
                      </v-card-text>
                    </v-card>

                    <!-- 자산 변동 추이 차트 (placeholder) -->
                    <v-card variant="outlined">
                      <v-card-title class="py-2 px-4 d-flex align-center">
                        <v-icon class="mr-2" size="20">mdi-chart-line</v-icon>
                        <span class="text-body-1">자산 변동 추이</span>
                        <v-spacer />
                        <!-- ⭐ 코인 페이지 동일: snapshot-btn 클래스로 높이 통일 -->
                        <v-btn
                          variant="flat"
                          color="grey-lighten-3"
                          class="text-grey-darken-2 mr-2 snapshot-btn"
                          disabled
                        >
                          <v-icon size="16" class="mr-1">mdi-database-refresh</v-icon>
                          스냅샷 갱신
                        </v-btn>
                        <v-btn-toggle
                          v-model="chartViewMode"
                          mandatory
                          density="compact"
                          variant="outlined"
                          divided
                        >
                          <v-btn value="full" size="x-small">
                            <v-icon size="16" class="mr-1">mdi-fit-to-screen</v-icon>
                            전체 보기
                          </v-btn>
                          <v-btn value="scroll" size="x-small">
                            <v-icon size="16" class="mr-1">mdi-arrow-left-right</v-icon>
                            스크롤 보기
                          </v-btn>
                        </v-btn-toggle>
                      </v-card-title>

                      <v-card-text class="pa-4">
                        <div class="text-center py-8 text-grey-darken-2">
                          <v-icon size="48" class="mb-2">mdi-chart-line-variant</v-icon>
                          <div>거래 이력이 없습니다</div>
                          <div class="text-caption text-grey-darken-1 mt-1">
                            ⏳ <strong>Day 63 (StockProfitService)</strong>에서 실제 데이터로 활성화됩니다
                          </div>
                        </div>
                      </v-card-text>
                    </v-card>
                  </v-card-text>
                </v-window-item>

                <!-- ========== 주식별 수익 탭 ========== -->
                <v-window-item value="stock">
                  <v-card-title class="py-3 px-4 bg-teal-darken-1 text-white d-flex align-center">
                    <v-icon class="mr-2" size="20">mdi-chart-line</v-icon>
                    <span class="text-body-1 font-weight-bold">주식별 수익 분석</span>
                    <HelpButton
                      use-dialog
                      :dialog-title="helpContents.stockProfit.title"
                      :dialog-content="helpContents.stockProfit.content"
                    />
                    <v-spacer />
                    <v-btn color="white" variant="text" disabled size="small">
                      <v-icon start>mdi-refresh</v-icon>
                      새로고침
                    </v-btn>
                  </v-card-title>

                  <v-card-text class="pa-0">
                    <v-data-table
                      :headers="stockProfitHeaders"
                      :items="[]"
                      items-per-page="10"
                      class="stock-profit-table"
                    >
                      <template v-slot:no-data>
                        <div class="text-center py-6">
                          <v-icon size="40" color="grey-lighten-1" class="mb-2">mdi-chart-line-variant</v-icon>
                          <div class="text-body-2 text-grey-darken-1 mb-1">분석 데이터가 없습니다</div>
                          <div class="text-caption text-grey-darken-1">
                            ⏳ <strong>Day 63</strong>에서 종목별 수익 분석이 활성화됩니다
                          </div>
                        </div>
                      </template>
                    </v-data-table>
                  </v-card-text>
                </v-window-item>
              </v-window>
            </v-card>
          </v-col>
        </v-row>

        <!-- ========== 하단: 보유 현황 (코인 페이지 1:1) ========== -->
        <v-row class="mt-4">
          <v-col cols="12">
            <v-card elevation="2">
              <v-card-title class="py-3 px-4 bg-blue-grey-darken-1 text-white d-flex align-center">
                <v-icon class="mr-2" size="20">mdi-safe</v-icon>
                <span class="text-body-1 font-weight-bold">보유 현황</span>
                <HelpButton
                  use-dialog
                  :dialog-title="helpContents.holdings.title"
                  :dialog-content="helpContents.holdings.content"
                />
                <v-spacer />
                <v-btn
                  color="white"
                  variant="text"
                  @click="loadHoldings"
                  :loading="loading"
                  size="small"
                >
                  <v-icon start>mdi-refresh</v-icon>
                  새로고침
                </v-btn>
              </v-card-title>

              <v-card-text class="pa-4">
                <!-- 통계 카드 4개 -->
                <v-row class="mb-4">
                  <v-col cols="6" md="3">
                    <v-card variant="outlined" class="text-center pa-3">
                      <div class="text-caption text-grey-darken-1">총 투자금액</div>
                      <div class="text-h6 font-weight-bold mt-1">
                        {{ formatCurrency(stats.totalHoldingAmount) }}
                      </div>
                    </v-card>
                  </v-col>
                  <v-col cols="6" md="3">
                    <v-card variant="outlined" class="text-center pa-3">
                      <div class="text-caption text-grey-darken-1">현재 평가액</div>
                      <div class="text-h6 font-weight-bold mt-1">
                        <span v-if="stats.hasCurrentPrice">{{ formatCurrency(stats.totalCurrentValue) }}</span>
                        <span v-else class="text-grey">-</span>
                      </div>
                    </v-card>
                  </v-col>
                  <v-col cols="6" md="3">
                    <v-card variant="outlined" class="text-center pa-3">
                      <div class="text-caption text-grey-darken-1">평가 손익</div>
                      <div
                        class="text-h6 font-weight-bold mt-1"
                        :class="stats.hasCurrentPrice ? (stats.totalProfitLoss >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2') : 'text-grey'"
                      >
                        <span v-if="stats.hasCurrentPrice">{{ formatCurrency(stats.totalProfitLoss) }}</span>
                        <span v-else>-</span>
                      </div>
                    </v-card>
                  </v-col>
                  <v-col cols="6" md="3">
                    <!-- ⭐ 코인 페이지와 동일: 항상 highlight-card 클래스 적용 -->
                    <v-card variant="outlined" class="text-center pa-3 highlight-card">
                      <div class="text-caption text-grey-darken-1">수익률</div>
                      <div
                        class="text-h6 font-weight-bold mt-1"
                        :class="stats.hasCurrentPrice ? (stats.totalProfitLossPct >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2') : 'text-grey'"
                      >
                        <span v-if="stats.hasCurrentPrice">
                          {{ stats.totalProfitLossPct >= 0 ? '+' : '' }}{{ stats.totalProfitLossPct.toFixed(2) }}%
                        </span>
                        <span v-else>-</span>
                      </div>
                    </v-card>
                  </v-col>
                </v-row>

                <!-- 보유 목록 테이블 (⭐ 보유일 컬럼 제거 - 코인 페이지와 동일하게 10개) -->
                <v-data-table
                  :headers="holdingHeaders"
                  :items="sortedHoldings"
                  :loading="loading"
                  items-per-page="10"
                >
                  <template v-slot:item.transactionId="{ item }">
                    {{ item.transactionId }}
                  </template>

                  <template v-slot:item.stockCode="{ item }">
                    <div class="d-flex flex-column align-center" style="line-height: 1.2;">
                      <strong>{{ item.stockCode }}</strong>
                      <span v-if="item.stockName" class="text-caption text-grey">{{ item.stockName }}</span>
                    </div>
                  </template>

                  <template v-slot:item.quantity="{ item }">
                    {{ item.quantity }}주
                  </template>

                  <template v-slot:item.price="{ item }">
                    {{ formatCurrency(item.price) }}
                  </template>

                  <template v-slot:item.totalAmount="{ item }">
                    {{ formatCurrency(item.totalAmount) }}
                  </template>

                  <template v-slot:item.currentPrice="{ item }">
                    <span v-if="item.currentPrice">{{ formatCurrency(item.currentPrice) }}</span>
                    <span v-else class="text-grey">-</span>
                  </template>

                  <template v-slot:item.currentValue="{ item }">
                    <span v-if="item.currentPrice">{{ formatCurrency(item.currentPrice * item.quantity) }}</span>
                    <span v-else class="text-grey">-</span>
                  </template>

                  <template v-slot:item.profitLoss="{ item }">
                    <span
                      v-if="item.currentProfitLoss !== null && item.currentProfitLoss !== undefined"
                      :class="Number(item.currentProfitLoss) >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2'"
                    >
                      {{ formatCurrency(item.currentProfitLoss) }}
                      <br>
                      <span class="text-caption">({{ Number(item.currentProfitLossPct ?? 0).toFixed(2) }}%)</span>
                    </span>
                    <span v-else class="text-grey">-</span>
                  </template>

                  <template v-slot:item.createdAt="{ item }">
                    {{ formatDateTime(item.createdAt) }}
                  </template>

                  <!-- ⭐ 매도/상세 한 줄 + 폭 통일 (코인 페이지와 동일) -->
                  <template v-slot:item.actions="{ item }">
                    <v-btn color="orange" size="small" @click="openSellDialog(item)">매도</v-btn>
                    <v-btn color="grey" size="small" @click="viewDetail(item)" class="ml-1">상세</v-btn>
                  </template>
                </v-data-table>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <v-snackbar v-model="snackbar.show" :color="snackbar.color" :timeout="3000">
          {{ snackbar.message }}
        </v-snackbar>
      </v-container>
    </v-main>

    <!-- 매도 다이얼로그 (placeholder, Day 63 이후 활성화) -->
    <!-- ⭐⭐⭐ [Day 60 변경] 코인 매도 다이얼로그와 정보량/UI 통일 ⭐⭐⭐ -->
    <v-dialog v-model="sellDialog" max-width="600">
      <v-card>
        <v-card-title class="bg-orange-darken-2 text-white py-3 px-4 d-flex align-center">
          <v-icon class="mr-2">mdi-cash-multiple</v-icon>
          <span>매도 처리</span>
        </v-card-title>
        <v-card-text class="pa-4">
          <v-alert type="info" variant="tonal" density="compact" class="mb-4">
            <span class="text-body-2">주식 수동 매도는 Day 63 이후 거래 내역 페이지에서 처리됩니다.</span>
          </v-alert>
          <div v-if="selectedHolding">
            <p class="mb-2"><strong>종목:</strong> {{ selectedHolding.stockCode }} - {{ selectedHolding.stockName || '-' }}</p>
            <p class="mb-2"><strong>보유 수량:</strong> {{ selectedHolding.quantity }}주</p>
            <p class="mb-2"><strong>매수 평균가:</strong> {{ formatCurrency(selectedHolding.price) }}</p>
            <p class="mb-2"><strong>투자 금액:</strong> {{ formatCurrency(selectedHolding.totalAmount) }}</p>
            <p v-if="selectedHolding.currentPrice" class="mb-2">
              <strong>현재가:</strong> {{ formatCurrency(selectedHolding.currentPrice) }}
            </p>
            <p v-if="selectedHolding.currentProfitLoss !== null && selectedHolding.currentProfitLoss !== undefined" class="mb-2">
              <strong>예상 손익:</strong>
              <span :class="Number(selectedHolding.currentProfitLoss) >= 0 ? 'text-teal-darken-2' : 'text-red-darken-2'" class="ml-2 font-weight-medium">
                {{ formatCurrency(selectedHolding.currentProfitLoss) }}
                ({{ Number(selectedHolding.currentProfitLossPct ?? 0).toFixed(2) }}%)
              </span>
            </p>
            <p v-if="selectedHolding.targetSellPrice" class="mb-2">
              <strong>목표 매도가:</strong>
              <span class="text-teal-darken-2 ml-2">{{ formatCurrency(selectedHolding.targetSellPrice) }}</span>
            </p>
            <p v-if="selectedHolding.stopLossPrice" class="mb-2">
              <strong>손절가:</strong>
              <span class="text-red-darken-2 ml-2">{{ formatCurrency(selectedHolding.stopLossPrice) }}</span>
            </p>
            <v-text-field
              v-model.number="sellPrice"
              label="매도 가격 (Day 63 이후 활성화)"
              type="number"
              variant="outlined"
              density="compact"
              disabled
              class="mt-4"
            />
          </div>
        </v-card-text>
        <v-card-actions class="pa-4 pt-0">
          <v-spacer />
          <v-btn variant="flat" color="grey-lighten-2" @click="sellDialog = false">
            닫기
          </v-btn>
          <v-btn variant="flat" color="orange-darken-2" @click="goToTransactionPage">
            <v-icon start size="16">mdi-arrow-right</v-icon>
            거래 내역으로 이동
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- ⭐⭐⭐ [Day 60 변경] 보유 자산 상세 다이얼로그 - 매도 다이얼로그와 디자인 통일 ⭐⭐⭐ -->
    <v-dialog v-model="detailDialog" max-width="700">
      <v-card v-if="selectedHolding">
        <v-card-title class="bg-blue-grey-darken-1 text-white py-3 px-4 d-flex align-center">
          <v-icon class="mr-2">mdi-information-outline</v-icon>
          <span>보유 자산 상세</span>
          <v-spacer />
          <span class="text-caption">{{ selectedHolding.stockCode }}</span>
        </v-card-title>

        <v-card-text class="pa-4">
          <!-- 종목 정보 영역 -->
          <v-row dense class="mb-2">
            <v-col cols="12">
              <v-card variant="outlined" class="pa-3">
                <div class="text-caption text-grey-darken-1 mb-1">종목</div>
                <div class="text-body-1 font-weight-bold text-grey-darken-4">
                  {{ selectedHolding.stockCode }}
                  <span v-if="selectedHolding.stockName" class="text-body-2 text-grey-darken-2 ml-2">
                    {{ selectedHolding.stockName }}
                  </span>
                </div>
              </v-card>
            </v-col>
          </v-row>

          <!-- 매수 정보 -->
          <v-row dense class="mb-2">
            <v-col cols="6" md="4">
              <div class="text-caption text-grey-darken-1">거래 ID</div>
              <div class="text-body-1 font-weight-medium">{{ selectedHolding.transactionId }}</div>
            </v-col>
            <v-col cols="6" md="4">
              <div class="text-caption text-grey-darken-1">보유 수량</div>
              <div class="text-body-1 font-weight-medium">{{ selectedHolding.quantity }}주</div>
            </v-col>
            <v-col cols="6" md="4">
              <div class="text-caption text-grey-darken-1">매수가</div>
              <div class="text-body-1 font-weight-medium">{{ formatCurrency(selectedHolding.price) }}</div>
            </v-col>
            <v-col cols="6" md="4" class="mt-2">
              <div class="text-caption text-grey-darken-1">투자 금액</div>
              <div class="text-body-1 font-weight-medium">{{ formatCurrency(selectedHolding.totalAmount) }}</div>
            </v-col>
            <v-col v-if="selectedHolding.currentPrice" cols="6" md="4" class="mt-2">
              <div class="text-caption text-grey-darken-1">현재가</div>
              <div class="text-body-1 font-weight-medium">{{ formatCurrency(selectedHolding.currentPrice) }}</div>
            </v-col>
            <v-col v-if="selectedHolding.holdingDays !== null" cols="6" md="4" class="mt-2">
              <div class="text-caption text-grey-darken-1">보유 일수</div>
              <div class="text-body-1 font-weight-medium">{{ selectedHolding.holdingDays }}일</div>
            </v-col>
          </v-row>

          <v-divider class="my-3" />

          <!-- 자동매매 설정 -->
          <v-row dense>
            <v-col v-if="selectedHolding.targetSellPrice" cols="6">
              <div class="text-caption text-grey-darken-1">목표 매도가</div>
              <div class="text-body-1 font-weight-medium text-teal-darken-2">
                {{ formatCurrency(selectedHolding.targetSellPrice) }}
              </div>
            </v-col>
            <v-col v-if="selectedHolding.stopLossPrice" cols="6">
              <div class="text-caption text-grey-darken-1">손절가</div>
              <div class="text-body-1 font-weight-medium text-red-darken-2">
                {{ formatCurrency(selectedHolding.stopLossPrice) }}
              </div>
            </v-col>
            <v-col cols="12" class="mt-2">
              <div class="text-caption text-grey-darken-1">매수 시각</div>
              <div class="text-body-2">{{ formatDateTime(selectedHolding.createdAt) }}</div>
            </v-col>
          </v-row>
        </v-card-text>

        <v-card-actions class="pa-4 pt-0">
          <v-spacer />
          <v-btn variant="flat" color="blue-grey-darken-1" class="text-white" @click="detailDialog = false">
            닫기
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-app>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'
import HelpButton from '@/components/HelpButton.vue'
import { stockTransactionApi } from '@/api/stock'

const router = useRouter()
const sidebarRef = ref()

interface Holding {
  transactionId: number
  stockCode: string
  stockName: string | null
  quantity: number
  price: number
  totalAmount: number
  targetSellPrice: number | null
  stopLossPrice: number | null
  currentPrice: number | null
  currentProfitLoss: number | null
  currentProfitLossPct: number | null
  holdingDays: number | null
  highestPrice: number | null
  exchangeRate: number | null
  createdAt: string | null
}

const profitTab = ref('period')
const selectedPeriod = ref<string>('total')
const customStartDate = ref('')
const customEndDate = ref('')
const chartViewMode = ref<'full' | 'scroll'>('full')

const loading = ref(false)
const holdings = ref<Holding[]>([])
const sellDialog = ref(false)
const detailDialog = ref(false)
const selectedHolding = ref<Holding | null>(null)
const sellPrice = ref(0)              // ⭐ [Day 60 추가] 매도 가격 (Day 63 활성화 예정)
const snackbar = ref({ show: false, message: '', color: 'success' })

const stats = ref({
  totalHoldingAmount: 0,
  totalCurrentValue: 0,
  totalProfitLoss: 0,
  totalProfitLossPct: 0,
  hasCurrentPrice: false
})

// ⭐ 코인 페이지 holdingHeaders 그대로 (보유일 컬럼 제거하여 10개로 통일)
const holdingHeaders = [
  { title: '거래 ID', key: 'transactionId', align: 'center' as const },
  { title: '종목', key: 'stockCode', align: 'center' as const },
  { title: '수량', key: 'quantity', align: 'end' as const },
  { title: '매수가', key: 'price', align: 'end' as const },
  { title: '투자금액', key: 'totalAmount', align: 'end' as const },
  { title: '현재가', key: 'currentPrice', align: 'end' as const },
  { title: '평가액', key: 'currentValue', align: 'end' as const },
  { title: '평가손익', key: 'profitLoss', align: 'end' as const },
  { title: '매수시각', key: 'createdAt', align: 'center' as const },
  { title: '액션', key: 'actions', align: 'center' as const, sortable: false }
]

// 주식별 수익 분석 헤더 (Day 63 활성화 예정)
const stockProfitHeaders = [
  { title: '종목', key: 'stockCode', align: 'start' as const },
  { title: '실현 수익', key: 'totalProfit', align: 'end' as const },
  { title: '수익률', key: 'profitPct', align: 'center' as const },
  { title: '거래', key: 'totalTradeCount', align: 'center' as const },
  { title: '익절/손절 (승률)', key: 'winRate', align: 'center' as const },
  { title: '보유 현황', key: 'currentHoldingCount', align: 'center' as const },
  { title: '', key: 'actions', align: 'center' as const, sortable: false }
]

const helpContents = {
  periodProfit: {
    title: '📅 기간별 수익 분석',
    content: `
      <div class="help-box">
        <p><strong>📖 쉬운 설명:</strong> 선택한 기간 동안의 거래 수익을 분석합니다.</p>
        <p style="margin-top: 8px;"><strong>📆 기간 선택:</strong></p>
        <p>• <strong>오늘</strong>: 당일 00:00~현재까지의 수익</p>
        <p>• <strong>이번달</strong>: 이번 달 1일~현재까지</p>
        <p>• <strong>올해</strong>: 올해 1월 1일~현재까지</p>
        <p>• <strong>1년</strong>: 최근 365일간</p>
        <p>• <strong>누적</strong>: 첫 거래~현재까지 전체</p>
        <p style="margin-top: 8px;"><strong>📊 주요 지표:</strong> 총 수익, 거래 건수, 익절/손절, 승률, 건당 평균, 최대 수익/손실</p>
        <p style="margin-top: 8px;"><strong>⏳ Day 63:</strong> StockProfitService에서 실제 데이터로 활성화됩니다.</p>
      </div>
    `
  },
  stockProfit: {
    title: '📊 주식별 수익 분석',
    content: `
      <div class="help-box">
        <p><strong>📖 쉬운 설명:</strong> 각 종목별로 거래 성과를 분석합니다.</p>
        <p style="margin-top: 8px;"><strong>📊 주요 지표:</strong></p>
        <p>• <strong>실현 수익</strong>: 해당 종목에서 실현된 총 손익</p>
        <p>• <strong>수익률</strong>: 투자 대비 수익 비율</p>
        <p>• <strong>익절/손절</strong>: 수익 거래 / 손실 거래</p>
        <p>• <strong>승률</strong>: 익절 거래 / 전체 거래 × 100%</p>
        <p style="margin-top: 8px;"><strong>⏳ Day 63:</strong> 종목별 수익 통계가 활성화됩니다.</p>
      </div>
    `
  },
  holdings: {
    title: '📦 보유 현황',
    content: `
      <div class="help-box">
        <p><strong>📖 쉬운 설명:</strong> 현재 보유 중인 주식의 평가 손익을 보여줍니다.</p>
        <p style="margin-top: 8px;"><strong>📊 용어 설명:</strong></p>
        <p>• <strong>투자금액</strong>: 매수 시 사용한 금액 (매수가 × 수량 + 수수료)</p>
        <p>• <strong>평가액</strong>: 현재가 × 보유수량</p>
        <p>• <strong>평가 손익</strong>: 평가액 - 투자금액</p>
        <p>• <strong>수익률</strong>: (현재가 - 매수가) / 매수가 × 100%</p>
        <p style="margin-top: 8px;"><strong>⚠️ 주의:</strong> 평가 손익은 미실현 손익입니다. 매도 전까지는 확정되지 않습니다.</p>
        <p style="margin-top: 8px;"><strong>💡 팁:</strong> KIS API 키 미등록 시 현재가/평가액이 "-"로 표시됩니다.</p>
      </div>
    `
  }
}

const sortedHoldings = computed(() => {
  const list = [...holdings.value]
  list.sort((a, b) => (Number(b.currentProfitLoss) || -Infinity) - (Number(a.currentProfitLoss) || -Infinity))
  return list
})

const getPeriodLabel = (period: string) => {
  const labels: Record<string, string> = {
    today: '오늘', month: '이번달', year: '올해', oneYear: '1년', total: '누적', custom: '사용자 지정'
  }
  return labels[period] || period
}

const applyCustomPeriod = () => {
  if (!customStartDate.value || !customEndDate.value) {
    showSnackbar('시작일과 종료일을 입력해주세요', 'warning')
    return
  }
  selectedPeriod.value = 'custom'
  showSnackbar('Day 63에서 사용자 지정 기간 조회가 활성화됩니다.', 'info')
}

const loadHoldings = async () => {
  loading.value = true
  try {
    const response = await stockTransactionApi.getHoldings()
    const list = (response.data as any).data ?? response.data
    holdings.value = (list as Holding[]) || []

    // 통계 계산
    let totalInvestment = 0
    let totalEvaluation = 0
    let totalProfit = 0
    const hasCurrentPrice = holdings.value.some(h => h.currentPrice !== null && h.currentPrice !== undefined)

    holdings.value.forEach(h => {
      totalInvestment += Number(h.totalAmount) || 0
      if (h.currentPrice !== null && h.currentPrice !== undefined) {
        totalEvaluation += Number(h.currentPrice) * Number(h.quantity)
      }
      const profit = Number(h.currentProfitLoss)
      if (!isNaN(profit)) totalProfit += profit
    })

    stats.value = {
      totalHoldingAmount: totalInvestment,
      totalCurrentValue: totalEvaluation,
      totalProfitLoss: totalProfit,
      totalProfitLossPct: totalInvestment > 0 ? (totalProfit / totalInvestment) * 100 : 0,
      hasCurrentPrice
    }
  } catch (error: any) {
    console.error('보유 종목 조회 실패:', error)
    showSnackbar(error.response?.data?.message || '보유 종목 조회에 실패했습니다.', 'error')
    holdings.value = []
  } finally {
    loading.value = false
  }
}

const openSellDialog = (h: Holding) => {
  selectedHolding.value = h
  sellPrice.value = h.currentPrice || h.price   // ⭐ [Day 60 추가]
  sellDialog.value = true
}

const goToTransactionPage = () => {
  if (selectedHolding.value) {
    router.push({
      path: '/stock-transactions',
      query: { stockCode: selectedHolding.value.stockCode, transactionId: String(selectedHolding.value.transactionId) }
    })
  }
  sellDialog.value = false
}

const viewDetail = (h: Holding) => {
  selectedHolding.value = h
  detailDialog.value = true
}

const formatCurrency = (value: number | null | undefined): string => {
  if (value === null || value === undefined) return '₩0'
  const n = Number(value)
  if (isNaN(n)) return '₩0'
  return '₩' + n.toLocaleString('ko-KR', { maximumFractionDigits: 0 })
}

const formatDateTime = (dateString: string | null): string => {
  if (!dateString) return '-'
  try {
    return new Date(dateString).toLocaleString('ko-KR')
  } catch {
    return dateString
  }
}

const showSnackbar = (message: string, color: string) => {
  snackbar.value = { show: true, message, color }
}

onMounted(async () => {
  await loadHoldings()
})
</script>

<style scoped>
/* ⭐⭐⭐ 코인 HoldingsView.vue 스타일 1:1 차용 ⭐⭐⭐ */

/* 배경색 통일 */
.bg-grey-lighten-3 {
  background-color: #EEEEEE !important;
}

/* 탭 스타일 - 하단 줄/배경 완전 제거 */
.profit-tabs {
  border-bottom: none !important;
  background-color: transparent !important;
  flex-grow: 0 !important;
  width: auto !important;
}

.profit-tabs :deep(.v-tabs__container) {
  flex-grow: 0 !important;
}

.profit-tabs :deep(.v-tabs-slider-wrapper),
.profit-tabs :deep(.v-tab__slider) {
  display: none !important;
}

.profit-tabs :deep(.v-slide-group__content) {
  background-color: transparent !important;
}

.profit-tab {
  min-width: 150px;
  max-width: 150px;
  border: 1px solid #CFD8DC;
  border-bottom: 1px solid #CFD8DC;
  margin-right: 4px;
  border-radius: 8px 8px 0 0;
  background-color: #B0BEC5 !important;
  color: #37474F !important;
  flex-grow: 0 !important;
}

.profit-tab.v-tab--selected {
  background-color: #546E7A !important;
  color: white !important;
  border-color: #546E7A;
}

/* 수익 요약 카드 */
.profit-summary-card {
  border: 2px solid #3949AB;
  background: linear-gradient(135deg, #f5f5f5 0%, #ffffff 100%);
}

/* ⭐ 하이라이트 카드 (수익률 - 항상 활성) */
.highlight-card {
  border: 2px solid #333 !important;
  background-color: #E8F5E9 !important;
}

/* 텍스트 컬러 */
.text-teal { color: #009688; }
.text-teal-darken-2 { color: #00796b; }
.text-red { color: #f44336; }
.text-red-darken-2 { color: #d32f2f; }
.text-grey { color: #9e9e9e; }

/* 주식별 수익 테이블 */
.stock-profit-table {
  border-top: 1px solid #e0e0e0;
}

/* 기간 선택 토글 - 헤더 영역 (옅은 회색 배경) */
.period-toggle-header {
  background-color: transparent;
  border-radius: 4px;
  height: 32px;
}

.period-toggle-header .v-btn {
  background-color: #B0BEC5 !important;
  color: #37474F !important;
  min-width: 50px;
  font-size: 12px;
  height: 32px !important;
  padding: 0 10px;
  border: none !important;
}

.period-toggle-header .v-btn--active {
  background-color: #FFC107 !important;
  color: #333 !important;
}

/* 날짜 입력 박스 - 높이 통일 (32px) */
.custom-date-input {
  width: 130px;
  height: 32px;
  padding: 4px 8px;
  font-size: 13px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 4px;
  background-color: rgba(255, 255, 255, 0.95);
  color: #333;
}

.custom-date-input:focus {
  outline: none;
  border-color: #FFC107;
}

/* 조회 버튼 - 높이 통일 (32px) */
.custom-search-btn {
  height: 32px !important;
  min-width: 50px;
  font-size: 13px;
  font-weight: 600;
}

/* 카드 상단 둥근 모서리 제거 (탭과 연결) */
.card-no-top-radius {
  border-top-left-radius: 0 !important;
  border-top-right-radius: 0 !important;
}

/* 스냅샷 갱신 버튼 - 전체보기/스크롤보기 토글과 높이 통일 */
.snapshot-btn {
  height: 34px !important;
  min-height: 34px !important;
  font-size: 0.75rem !important;
  border: 1px solid rgba(0, 0, 0, 0.38) !important;
  border-radius: 4px !important;
}

/* ⭐⭐⭐ [수정] 보유 종목 테이블: 종목명 풀 표시 + 가로 스크롤 제거 ⭐⭐⭐ */
/* 왜: 종목명을 풀로 표시하면서 다른 컬럼을 압축하여 가로 스크롤 방지 */

/* 1. 테이블 전체: 폰트 크기 살짝 축소 (.875rem → 0.85rem) */
:deep(.v-data-table) th,
:deep(.v-data-table) td {
  font-size: 0.85rem !important;
  white-space: nowrap !important;
}

/* 2. 셀 좌우 패딩 축소 (기본 16px → 8px) */
:deep(.v-data-table) th,
:deep(.v-data-table) td {
  padding: 0 8px !important;
}

/* 3. 거래 ID 컬럼: 좁게 (60px) */
:deep(.v-data-table) th:nth-child(1),
:deep(.v-data-table) td:nth-child(1) {
  width: 60px !important;
  min-width: 60px !important;
}

/* 4. 종목 컬럼: 넉넉히 (260px) - 종목명 풀 표시 */
:deep(.v-data-table) th:nth-child(2),
:deep(.v-data-table) td:nth-child(2) {
  min-width: 260px !important;
}

/* 5. 수량 컬럼: 좁게 (50px) */
:deep(.v-data-table) th:nth-child(3),
:deep(.v-data-table) td:nth-child(3) {
  width: 50px !important;
  min-width: 50px !important;
}

/* 6. 매수가/투자금액/현재가/평가액 컬럼: 적정 폭 (각 80px) */
:deep(.v-data-table) th:nth-child(4),
:deep(.v-data-table) td:nth-child(4),
:deep(.v-data-table) th:nth-child(5),
:deep(.v-data-table) td:nth-child(5),
:deep(.v-data-table) th:nth-child(6),
:deep(.v-data-table) td:nth-child(6),
:deep(.v-data-table) th:nth-child(7),
:deep(.v-data-table) td:nth-child(7) {
  width: 85px !important;
  min-width: 85px !important;
}

/* 7. 평가손익 컬럼: 적정 폭 (95px) */
:deep(.v-data-table) th:nth-child(8),
:deep(.v-data-table) td:nth-child(8) {
  width: 95px !important;
  min-width: 95px !important;
}

/* 8. 매수시각 컬럼: 적정 폭 (140px) */
:deep(.v-data-table) th:nth-child(9),
:deep(.v-data-table) td:nth-child(9) {
  width: 140px !important;
  min-width: 140px !important;
  font-size: 0.75rem !important;
}

/* 9. 액션 컬럼: 매도+상세 두 버튼 한 줄 (130px) */
:deep(.v-data-table) th:last-child,
:deep(.v-data-table) td:last-child {
  width: 130px !important;
  min-width: 130px !important;
}

/* 10. 액션 버튼 폭 축소 (기본 ~64px → 50px) */
:deep(.v-data-table) td:last-child .v-btn {
  min-width: 50px !important;
  padding: 0 8px !important;
}

/* 11. 테이블 전체: 가로 스크롤 차단 (안전장치) */
:deep(.v-data-table),
:deep(.v-table__wrapper) {
  overflow-x: hidden !important;
}
</style>