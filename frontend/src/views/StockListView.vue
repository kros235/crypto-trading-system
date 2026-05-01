<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main class="bg-grey-lighten-3">
      <v-container fluid>
        <v-row class="mb-4">
          <v-col>
            <h1 class="text-h4">
              <v-icon class="mr-2">mdi-chart-line</v-icon>
              주식 종목 목록
            </h1>
            <p class="text-subtitle-1 text-grey">거래 가능한 주식/ETF 종목을 확인하세요</p>
          </v-col>
          <v-col cols="auto">
            <v-btn color="indigo-darken-1" @click="refreshAll" :loading="loading || priceLoading" variant="flat">
              <v-icon start>mdi-refresh</v-icon>
              새로고침
            </v-btn>
          </v-col>
        </v-row>

        <!-- 검색/필터 영역 -->
        <v-row class="mb-4">
          <v-col cols="12" md="4">
            <v-text-field
              v-model="search"
              prepend-inner-icon="mdi-magnify"
              label="종목 검색 (코드 또는 이름)"
              clearable
              hide-details
              density="compact"
              variant="outlined"
            />
          </v-col>
          <v-col cols="12" md="3">
            <v-select
              v-model="filterActive"
              :items="activeOptions"
              label="상태 필터"
              hide-details
              density="compact"
              variant="outlined"
            />
          </v-col>
          <v-col cols="12" md="3">
            <v-select
              v-model="filterEtfType"
              :items="etfTypeOptions"
              label="유형 필터"
              hide-details
              density="compact"
              variant="outlined"
            />
          </v-col>
          <v-col cols="12" md="2">
            <v-select
              v-model="sortBy"
              :items="sortOptions"
              label="정렬"
              hide-details
              density="compact"
              variant="outlined"
            />
          </v-col>
        </v-row>

        <!-- 종목 목록 카드 -->
        <v-card elevation="2">
          <v-card-title class="d-flex align-center bg-indigo-darken-2 text-white py-3">
            <v-icon class="mr-2">mdi-format-list-bulleted</v-icon>
            등록 종목 목록
            <v-chip class="ml-2" color="white" variant="outlined" size="small">
              {{ filteredStocks.length }}개
            </v-chip>
            <v-spacer />
            <span v-if="priceLoading" class="text-caption mr-2">
              <v-progress-circular size="16" width="2" indeterminate class="mr-1" />
              시세 조회중...
            </span>
            <v-btn
              color="white"
              variant="outlined"
              size="small"
              class="mr-2"
              @click="openAddDialog"
            >
              <v-icon start size="16">mdi-plus</v-icon>
              종목 추가
            </v-btn>
            <HelpButton
              :use-dialog="true"
              :dialog-title="helpContents.stockList.title"
              :dialog-content="helpContents.stockList.content"
              color="white"
            />
          </v-card-title>

          <v-data-table
            :headers="headers"
            :items="filteredStocks"
            :loading="loading"
            :items-per-page="itemsPerPage"
            v-model:page="currentPage"
            class="elevation-0"
          >
            <!-- 종목 컬럼 -->
            <template v-slot:item.stockCode="{ item }">
              <div class="d-flex align-center">
                <v-avatar size="32" class="mr-2" :color="getEtfTypeColor(item.etfType) + '-lighten-4'">
                  <v-icon size="18" :color="getEtfTypeColor(item.etfType) + '-darken-2'">
                    {{ getEtfTypeIcon(item.etfType) }}
                  </v-icon>
                </v-avatar>
                <div>
                  <div class="font-weight-medium">{{ item.stockCode }}</div>
                  <div class="text-caption text-grey-darken-1">{{ item.stockName }}</div>
                </div>
              </div>
            </template>

            <!-- ETF 유형 -->
            <template v-slot:item.etfType="{ item }">
              <v-chip :color="getEtfTypeColor(item.etfType)" size="small" variant="tonal">
                {{ getEtfTypeLabel(item.etfType) }}
              </v-chip>
            </template>

            <!-- 시장 -->
            <template v-slot:item.market="{ item }">
              <span class="text-body-2">{{ item.market || '-' }}</span>
            </template>

            <!-- 현재가 -->
            <template v-slot:item.currentPrice="{ item }">
              <span v-if="item.currentPrice !== null && item.currentPrice !== undefined" class="font-weight-medium">
                {{ formatCurrency(item.currentPrice) }}
              </span>
              <span v-else class="text-grey-darken-1">-</span>
            </template>

            <!-- 변동률 -->
            <template v-slot:item.changeRate="{ item }">
              <v-chip
                v-if="item.changeRate !== null && item.changeRate !== undefined"
                :color="Number(item.changeRate) >= 0 ? 'red-darken-1' : 'blue-darken-1'"
                variant="flat"
                size="small"
              >
                {{ Number(item.changeRate) >= 0 ? '+' : '' }}{{ Number(item.changeRate).toFixed(2) }}%
              </v-chip>
              <span v-else class="text-grey-darken-1">-</span>
            </template>

            <!-- 거래대금 -->
            <template v-slot:item.accumulatedTradingValue="{ item }">
              <span v-if="item.accumulatedTradingValue !== null && item.accumulatedTradingValue !== undefined">
                {{ formatVolume(item.accumulatedTradingValue) }}
              </span>
              <span v-else class="text-grey-darken-1">-</span>
            </template>

            <!-- 상태 -->
            <template v-slot:item.isActive="{ item }">
              <v-chip :color="item.isActive ? 'teal-darken-1' : 'grey'" size="small" variant="flat">
                {{ item.isActive ? '활성' : '비활성' }}
              </v-chip>
            </template>

            <!-- 액션 -->
            <template v-slot:item.actions="{ item }">
              <v-btn size="small" color="indigo-darken-1" variant="tonal" @click="viewStockDetail(item)">
                상세정보
              </v-btn>
            </template>
          </v-data-table>
        </v-card>

        <!-- 종목 상세 다이얼로그 -->
        <v-dialog v-model="detailDialog" max-width="700">
          <v-card v-if="selectedStock">
            <v-card-title class="d-flex align-center bg-indigo-darken-2 text-white py-3">
              <v-icon class="mr-2">mdi-information-outline</v-icon>
              {{ selectedStock.stockName }} 상세 정보
              <HelpButton
                :use-dialog="true"
                :dialog-title="helpContents.stockDetail.title"
                :dialog-content="helpContents.stockDetail.content"
                color="white"
              />
              <v-spacer />
              <v-btn icon variant="text" @click="detailDialog = false" size="small">
                <v-icon color="white">mdi-close</v-icon>
              </v-btn>
            </v-card-title>

            <v-card-text class="pa-4">
              <v-row>
                <!-- 기본 정보 -->
                <v-col cols="12" md="6">
                  <v-card variant="outlined" class="pa-3 fill-height">
                    <div class="text-overline text-indigo-darken-2 mb-2">기본 정보</div>
                    <div class="mb-2">
                      <span class="text-caption text-grey-darken-1">종목 코드</span>
                      <div class="font-weight-medium text-grey-darken-4">{{ selectedStock.stockCode }}</div>
                    </div>
                    <div class="mb-2">
                      <span class="text-caption text-grey-darken-1">종목명</span>
                      <div class="text-grey-darken-4">{{ selectedStock.stockName }}</div>
                    </div>
                    <div class="mb-2">
                      <span class="text-caption text-grey-darken-1">시장</span>
                      <div class="text-grey-darken-4">{{ selectedStock.market || '-' }}</div>
                    </div>
                    <div class="mb-2">
                      <span class="text-caption text-grey-darken-1">유형</span>
                      <div>
                        <v-chip :color="getEtfTypeColor(selectedStock.etfType)" size="small" variant="tonal">
                          {{ getEtfTypeLabel(selectedStock.etfType) }}
                        </v-chip>
                      </div>
                    </div>
                    <div v-if="selectedStock.underlyingIndex" class="mb-2">
                      <span class="text-caption text-grey-darken-1">기초지수</span>
                      <div class="text-grey-darken-4">{{ selectedStock.underlyingIndex }}</div>
                    </div>
                    <div v-if="selectedStock.expenseRatio">
                      <span class="text-caption text-grey-darken-1">운용보수율</span>
                      <div class="text-grey-darken-4">{{ selectedStock.expenseRatio.toFixed(3) }}%</div>
                    </div>
                  </v-card>
                </v-col>

                <!-- 실시간 시세 -->
                <v-col cols="12" md="6">
                  <v-card variant="outlined" class="pa-3 fill-height">
                    <div class="text-overline text-indigo-darken-2 mb-2">실시간 시세 (KIS API)</div>
                    <div class="mb-2">
                      <span class="text-caption text-grey-darken-1">현재가</span>
                      <div class="text-h6 font-weight-bold text-grey-darken-4">
                        <span v-if="detailLoading"><v-progress-circular size="20" width="2" indeterminate /></span>
                        <span v-else-if="detailPrice?.currentPrice !== null && detailPrice?.currentPrice !== undefined">
                          {{ formatCurrency(detailPrice.currentPrice) }}
                        </span>
                        <span v-else class="text-grey">-</span>
                      </div>
                    </div>
                    <div class="mb-2">
                      <span class="text-caption text-grey-darken-1">전일 대비율</span>
                      <div>
                        <v-chip
                          v-if="detailPrice?.changeRate !== null && detailPrice?.changeRate !== undefined"
                          :color="Number(detailPrice.changeRate) >= 0 ? 'red-darken-1' : 'blue-darken-1'"
                          size="small"
                          variant="flat"
                        >
                          {{ Number(detailPrice.changeRate) >= 0 ? '+' : '' }}{{ Number(detailPrice.changeRate).toFixed(2) }}%
                        </v-chip>
                        <span v-else class="text-grey-darken-2">-</span>
                      </div>
                    </div>
                    <div class="mb-2">
                      <span class="text-caption text-grey-darken-1">시가 / 고가 / 저가</span>
                      <div class="text-body-2 text-grey-darken-4">
                        <span v-if="detailPrice?.openPrice">{{ formatCurrency(detailPrice.openPrice) }}</span>
                        <span v-else>-</span>
                        <span class="mx-1">/</span>
                        <span v-if="detailPrice?.highPrice" class="text-red-darken-1">{{ formatCurrency(detailPrice.highPrice) }}</span>
                        <span v-else>-</span>
                        <span class="mx-1">/</span>
                        <span v-if="detailPrice?.lowPrice" class="text-blue-darken-1">{{ formatCurrency(detailPrice.lowPrice) }}</span>
                        <span v-else>-</span>
                      </div>
                    </div>
                    <div>
                      <span class="text-caption text-grey-darken-1">누적 거래대금</span>
                      <div class="text-grey-darken-4">
                        <span v-if="detailPrice?.accumulatedTradingValue">
                          {{ formatVolume(detailPrice.accumulatedTradingValue) }}
                        </span>
                        <span v-else class="text-grey-darken-2">-</span>
                      </div>
                    </div>
                  </v-card>
                </v-col>

                <!-- 종목 설명 / 주의사항 -->
                <v-col cols="12">
                  <v-card variant="outlined" class="pa-3">
                    <div class="text-overline text-indigo-darken-2 mb-2">
                      <v-icon size="small" class="mr-1">mdi-text-box-outline</v-icon>
                      종목 안내
                    </div>
                    <div class="text-body-2 text-grey-darken-4" style="line-height: 1.7;">
                      {{ getStockDescription(selectedStock) }}
                    </div>
                    <v-alert
                      v-if="selectedStock.etfType === 'LEVERAGE' || selectedStock.etfType === 'INVERSE'"
                      type="warning"
                      variant="tonal"
                      density="compact"
                      class="mt-3"
                    >
                      <strong>레버리지/인버스 ETF 주의:</strong>
                      장기 보유 시 변동성 끌림(Volatility Drag)으로 인한 가치 침식이 발생할 수 있습니다.
                      권장 보유 기간은 <strong>최대 20거래일</strong>입니다.
                    </v-alert>
                  </v-card>
                </v-col>
              </v-row>
            </v-card-text>

            <v-card-actions class="pa-4 pt-0">
              <v-btn
                color="indigo-darken-1"
                variant="flat"
                @click="goToTradingWithStock(selectedStock.stockCode)"
              >
                <v-icon start>mdi-cog</v-icon>
                거래 설정에 추가
              </v-btn>
              <v-btn
                v-if="selectedStock.isActive"
                color="orange-darken-1"
                variant="text"
                @click="toggleActive(selectedStock)"
              >
                비활성화
              </v-btn>
              <v-btn
                v-else
                color="teal-darken-1"
                variant="text"
                @click="toggleActive(selectedStock)"
              >
                활성화
              </v-btn>
              <v-spacer />
              <v-btn color="grey" variant="text" @click="detailDialog = false">닫기</v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>

        <!-- 종목 추가 다이얼로그 -->
        <v-dialog v-model="addDialog" max-width="700">
          <v-card>
            <v-card-title class="d-flex align-center bg-teal-darken-2 text-white py-3">
              <v-icon class="mr-2">mdi-plus-circle</v-icon>
              종목 추가
              <v-spacer />
              <v-btn icon variant="text" @click="addDialog = false" size="small">
                <v-icon color="white">mdi-close</v-icon>
              </v-btn>
            </v-card-title>

            <v-card-text class="pa-4">
              <p class="text-body-2 text-grey-darken-2 mb-3">
                KIS API에서 종목을 검색하여 거래 대상에 추가합니다. 종목코드(6자리)나 종목명으로 검색할 수 있습니다.
              </p>
              <v-text-field
                v-model="addSearchKeyword"
                prepend-inner-icon="mdi-magnify"
                label="종목코드 또는 종목명 입력"
                hide-details
                density="compact"
                variant="outlined"
                @keyup.enter="searchKisStocks"
                class="mb-3"
              />
              <v-btn color="teal-darken-1" variant="flat" block @click="searchKisStocks" :loading="addSearchLoading">
                <v-icon start>mdi-magnify</v-icon>
                KIS 검색
              </v-btn>

              <v-divider class="my-4" />

              <div v-if="addSearchResults.length === 0 && !addSearchLoading && addSearched" class="text-center text-grey-darken-1 py-4">
                검색 결과가 없습니다.
              </div>

              <v-list v-if="addSearchResults.length > 0" lines="two" class="pa-0">
                <v-list-item
                  v-for="result in addSearchResults"
                  :key="result.stockCode"
                  class="border rounded mb-2"
                >
                  <v-list-item-title class="font-weight-medium">
                    {{ result.stockCode }} - {{ result.stockName }}
                  </v-list-item-title>
                  <v-list-item-subtitle>
                    <v-chip :color="getEtfTypeColor(result.etfType)" size="x-small" variant="tonal" class="mr-1">
                      {{ getEtfTypeLabel(result.etfType) }}
                    </v-chip>
                    <span class="text-caption">{{ result.market }}</span>
                  </v-list-item-subtitle>
                  <template v-slot:append>
                    <v-btn color="teal-darken-1" variant="tonal" size="small" @click="addStock(result)" :loading="addLoading">
                      <v-icon start size="16">mdi-plus</v-icon>
                      추가
                    </v-btn>
                  </template>
                </v-list-item>
              </v-list>
            </v-card-text>
          </v-card>
        </v-dialog>

        <v-snackbar v-model="snackbar.show" :color="snackbar.color" :timeout="3000">
          {{ snackbar.message }}
        </v-snackbar>
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'
import HelpButton from '@/components/HelpButton.vue'
import { stockInfoApi } from '@/api/stock'
import type { StockInfo } from '@/types/stock'
import type { StockPrice } from '@/api/stock'

const router = useRouter()
const sidebarRef = ref()

// 종목 정보 + 가격 정보 결합 타입
interface StockWithPrice extends StockInfo {
  currentPrice: number | null
  changeRate: number | null
  accumulatedTradingValue: number | null
}

// 상태
const loading = ref(false)
const priceLoading = ref(false)
const stocks = ref<StockWithPrice[]>([])

// 검색/필터
const search = ref('')
const filterActive = ref('all')
const filterEtfType = ref('all')
const sortBy = ref('code')
const currentPage = ref(1)
const itemsPerPage = ref(20)

// 상세 다이얼로그
const detailDialog = ref(false)
const selectedStock = ref<StockWithPrice | null>(null)
const detailLoading = ref(false)
const detailPrice = ref<StockPrice | null>(null)

// 종목 추가 다이얼로그
const addDialog = ref(false)
const addSearchKeyword = ref('')
const addSearchResults = ref<StockInfo[]>([])
const addSearchLoading = ref(false)
const addSearched = ref(false)
const addLoading = ref(false)

// 스낵바
const snackbar = ref({ show: false, message: '', color: 'success' })

// 옵션 정의
const activeOptions = [
  { title: '전체', value: 'all' },
  { title: '활성만', value: 'active' },
  { title: '비활성만', value: 'inactive' }
]

const etfTypeOptions = [
  { title: '전체', value: 'all' },
  { title: '레버리지', value: 'LEVERAGE' },
  { title: '인버스', value: 'INVERSE' },
  { title: '일반 ETF', value: 'NORMAL' },
  { title: '개별 주식', value: 'STOCK' }
]

const sortOptions = [
  { title: '종목코드', value: 'code' },
  { title: '종목명', value: 'name' },
  { title: '변동률 높은순', value: 'changeRateDesc' },
  { title: '변동률 낮은순', value: 'changeRateAsc' },
  { title: '거래대금순', value: 'volume' }
]

// 테이블 헤더
const headers = [
  { title: '종목', key: 'stockCode', sortable: true, align: 'start' as const },
  { title: '유형', key: 'etfType', sortable: true, align: 'center' as const },
  { title: '시장', key: 'market', sortable: true, align: 'center' as const },
  { title: '현재가', key: 'currentPrice', sortable: true, align: 'end' as const },
  { title: '변동률', key: 'changeRate', sortable: true, align: 'end' as const },
  { title: '거래대금', key: 'accumulatedTradingValue', sortable: true, align: 'end' as const },
  { title: '상태', key: 'isActive', sortable: true, align: 'center' as const },
  { title: '상세정보', key: 'actions', sortable: false, align: 'center' as const }
]

// 도움말 컨텐츠
const helpContents = {
  stockList: {
    title: '📋 주식 종목 목록 안내',
    content: `
      <p class="help-intro">자동매매 대상으로 등록된 주식/ETF 종목을 확인하고 관리하는 페이지입니다.</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>종목 검색</strong>
        <span class="help-desc">종목코드(예: 409820)나 종목명(예: TIGER 나스닥)으로 빠르게 찾을 수 있습니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>유형 필터</strong>
        <span class="help-desc">레버리지/인버스/일반 ETF/개별 주식으로 필터링할 수 있습니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>실시간 시세</strong>
        <span class="help-desc">페이지 진입 시 KIS API로 등록된 종목의 현재가/변동률을 일괄 조회합니다.<br/>KIS API 키가 등록되지 않은 경우 시세가 표시되지 않습니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>종목 추가</strong>
        <span class="help-desc">상단의 "종목 추가" 버튼으로 KIS API에서 검색하여 새 종목을 등록할 수 있습니다.</span></p>
      <p class="help-note">💡 <strong>Tip:</strong> "상세정보" 버튼을 클릭하면 종목의 자세한 시세와 ETF 정보를 확인하고, "거래 설정에 추가" 버튼으로 바로 자동매매 대상에 등록할 수 있습니다.</p>
    `
  },
  stockDetail: {
    title: '🔍 종목 상세 정보',
    content: `
      <p class="help-intro">선택한 종목의 상세 정보와 실시간 시세를 확인할 수 있습니다.</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>기본 정보</strong>
        <span class="help-desc">종목코드, 시장(KRX/KOSDAQ), 유형, 기초지수, 운용보수율 등 종목의 기본 정보입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>실시간 시세</strong>
        <span class="help-desc">한국투자증권 KIS API에서 가져온 현재가, 전일 대비율, 시가/고가/저가, 거래대금입니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>변동률 색상</strong>
        <span class="help-desc">한국 증시 관행에 따라 상승은 빨간색(+), 하락은 파란색(-)으로 표시됩니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>레버리지/인버스 주의사항</strong>
        <span class="help-desc">레버리지·인버스 ETF는 장기 보유 시 복리 효과로 인한 가치 침식(decay)이 발생할 수 있습니다. 권장 보유 기간은 최대 20거래일입니다.</span></p>
      <p class="help-note">💡 <strong>Tip:</strong> "거래 설정에 추가" 버튼으로 자동매매 대상에 즉시 등록할 수 있습니다.</p>
    `
  }
}

// ETF 유형별 표시 헬퍼
const getEtfTypeLabel = (type: string | undefined): string => {
  switch (type) {
    case 'LEVERAGE': return '레버리지'
    case 'INVERSE': return '인버스'
    case 'NORMAL': return '일반 ETF'
    case 'STOCK': return '개별 주식'
    default: return '-'
  }
}

const getEtfTypeColor = (type: string | undefined): string => {
  switch (type) {
    case 'LEVERAGE': return 'red'
    case 'INVERSE': return 'blue'
    case 'NORMAL': return 'green'
    case 'STOCK': return 'indigo'
    default: return 'grey'
  }
}

const getEtfTypeIcon = (type: string | undefined): string => {
  switch (type) {
    case 'LEVERAGE': return 'mdi-rocket-launch'
    case 'INVERSE': return 'mdi-arrow-down-bold'
    case 'NORMAL': return 'mdi-chart-areaspline'
    case 'STOCK': return 'mdi-office-building'
    default: return 'mdi-help-circle'
  }
}

// 필터 + 정렬된 종목 리스트
const filteredStocks = computed(() => {
  let result = [...stocks.value]

  // 검색
  if (search.value) {
    const sl = search.value.toLowerCase()
    result = result.filter(s =>
      s.stockCode.toLowerCase().includes(sl) ||
      (s.stockName && s.stockName.toLowerCase().includes(sl))
    )
  }

  // 활성 상태 필터
  if (filterActive.value === 'active') result = result.filter(s => s.isActive)
  else if (filterActive.value === 'inactive') result = result.filter(s => !s.isActive)

  // 유형 필터
  if (filterEtfType.value !== 'all') {
    result = result.filter(s => s.etfType === filterEtfType.value)
  }

  // 정렬
  switch (sortBy.value) {
    case 'code':
      result.sort((a, b) => a.stockCode.localeCompare(b.stockCode))
      break
    case 'name':
      result.sort((a, b) => (a.stockName || '').localeCompare(b.stockName || ''))
      break
    case 'changeRateDesc':
      result.sort((a, b) => (Number(b.changeRate) || -999) - (Number(a.changeRate) || -999))
      break
    case 'changeRateAsc':
      result.sort((a, b) => (Number(a.changeRate) || 999) - (Number(b.changeRate) || 999))
      break
    case 'volume':
      result.sort((a, b) => (Number(b.accumulatedTradingValue) || 0) - (Number(a.accumulatedTradingValue) || 0))
      break
  }

  return result
})

// 포맷터
const formatCurrency = (value: number | null | undefined): string => {
  if (value === null || value === undefined) return '-'
  const n = Number(value)
  if (isNaN(n)) return '-'
  return '₩' + n.toLocaleString('ko-KR', { maximumFractionDigits: 0 })
}

const formatVolume = (value: number | null | undefined): string => {
  if (value === null || value === undefined) return '-'
  const n = Number(value)
  if (isNaN(n)) return '-'
  if (n >= 1000000000000) return (n / 1000000000000).toFixed(1) + '조'
  if (n >= 100000000) return (n / 100000000).toFixed(1) + '억'
  if (n >= 10000) return (n / 10000).toFixed(1) + '만'
  return '₩' + n.toLocaleString('ko-KR')
}

// 종목 설명
const getStockDescription = (stock: StockInfo): string => {
  const baseInfo = `${stock.stockName}(${stock.stockCode})은(는) ${getEtfTypeLabel(stock.etfType)} 종목입니다.`
  if (stock.underlyingIndex) {
    return `${baseInfo} 기초지수는 ${stock.underlyingIndex}이며, ${stock.market || 'KRX'} 시장에 상장되어 있습니다.`
  }
  return `${baseInfo} ${stock.market || 'KRX'} 시장에 상장되어 있습니다.`
}

// 종목 목록 조회
const fetchStocks = async () => {
  loading.value = true
  try {
    const response = await stockInfoApi.getActiveStocks()
    // ApiResponse 래퍼 또는 직접 배열 모두 대응
    const list = (response.data as any).data ?? response.data
    stocks.value = (list as StockInfo[]).map(s => ({
      ...s,
      currentPrice: null,
      changeRate: null,
      accumulatedTradingValue: null
    }))

    // 가격 일괄 조회 (하이브리드 방식)
    await fetchPrices()
  } catch (error: any) {
    console.error('종목 목록 조회 실패:', error)
    showSnackbar('종목 목록 조회에 실패했습니다.', 'error')
  } finally {
    loading.value = false
  }
}

// 다중 종목 가격 일괄 조회 (Day 60 핵심)
const fetchPrices = async () => {
  if (stocks.value.length === 0) return

  priceLoading.value = true
  try {
    const stockCodes = stocks.value.map(s => s.stockCode)
    const response = await stockInfoApi.getPrices(stockCodes)
    const priceList = (response.data as any).data ?? response.data

    // 종목코드를 키로 매핑
    const priceMap = new Map<string, StockPrice>()
    ;(priceList as StockPrice[]).forEach(p => priceMap.set(p.stockCode, p))

    // stocks 배열에 가격 정보 병합
    stocks.value = stocks.value.map(s => {
      const price = priceMap.get(s.stockCode)
      return {
        ...s,
        currentPrice: price?.currentPrice ?? null,
        changeRate: price?.changeRate ?? null,
        accumulatedTradingValue: price?.accumulatedTradingValue ?? null
      }
    })
  } catch (error: any) {
    console.warn('가격 일괄 조회 실패:', error)
    // 실패해도 종목 목록은 보여줌 (graceful degradation)
  } finally {
    priceLoading.value = false
  }
}

// 새로고침 (목록 + 가격 모두)
const refreshAll = async () => {
  await fetchStocks()
}

// 상세 다이얼로그 열기
const viewStockDetail = async (stock: StockWithPrice) => {
  selectedStock.value = stock
  detailDialog.value = true
  detailPrice.value = {
    stockCode: stock.stockCode,
    currentPrice: stock.currentPrice,
    changeRate: stock.changeRate,
    accumulatedTradingValue: stock.accumulatedTradingValue,
    changeFromPrevDay: null,
    accumulatedVolume: null,
    openPrice: null,
    highPrice: null,
    lowPrice: null,
    prevClosePrice: null
  }

  // 상세 시세 추가 조회 (시가/고가/저가 등)
  detailLoading.value = true
  try {
    const response = await stockInfoApi.getPrices([stock.stockCode])
    const list = (response.data as any).data ?? response.data
    if (list && list.length > 0) {
      detailPrice.value = list[0] as StockPrice
      // 목록 페이지의 가격 정보도 함께 갱신
      stock.currentPrice = detailPrice.value.currentPrice
      stock.changeRate = detailPrice.value.changeRate
      stock.accumulatedTradingValue = detailPrice.value.accumulatedTradingValue
    }
  } catch (e) {
    console.warn('종목 상세 시세 조회 실패:', e)
  } finally {
    detailLoading.value = false
  }
}

// 거래 설정 페이지로 이동 (종목코드 쿼리 전달)
const goToTradingWithStock = (stockCode: string) => {
  router.push({ path: '/stock/settings', query: { addStock: stockCode } })
  detailDialog.value = false
}

// 활성/비활성 토글
const toggleActive = async (stock: StockWithPrice) => {
  try {
    if (stock.isActive) {
      await stockInfoApi.deleteStock(stock.stockCode)
      stock.isActive = false
      showSnackbar(`${stock.stockName} 종목이 비활성화되었습니다.`, 'success')
    } else {
      // 백엔드 PATCH /api/stock/info/{code}/activate 사용
      // stockInfoApi에 activate 메서드가 없으므로 axios 직접 호출
      const api = (await import('@/api')).default
      await api.patch(`/stock/info/${stock.stockCode}/activate`)
      stock.isActive = true
      showSnackbar(`${stock.stockName} 종목이 활성화되었습니다.`, 'success')
    }
    detailDialog.value = false
  } catch (error: any) {
    console.error('종목 상태 변경 실패:', error)
    showSnackbar(error.response?.data?.message || '상태 변경에 실패했습니다.', 'error')
  }
}

// 종목 추가 다이얼로그
const openAddDialog = () => {
  addDialog.value = true
  addSearchKeyword.value = ''
  addSearchResults.value = []
  addSearched.value = false
}

// KIS API 종목 검색
const searchKisStocks = async () => {
  if (!addSearchKeyword.value.trim()) {
    showSnackbar('검색어를 입력해주세요.', 'warning')
    return
  }

  addSearchLoading.value = true
  addSearched.value = false
  try {
    const response = await stockInfoApi.searchStocks(addSearchKeyword.value.trim())
    addSearchResults.value = (response.data as any).data ?? response.data ?? []
    addSearched.value = true
    if (addSearchResults.value.length === 0) {
      showSnackbar('검색 결과가 없습니다.', 'info')
    }
  } catch (error: any) {
    console.error('종목 검색 실패:', error)
    showSnackbar(error.response?.data?.message || '검색에 실패했습니다.', 'error')
    addSearchResults.value = []
    addSearched.value = true
  } finally {
    addSearchLoading.value = false
  }
}

// 종목 추가
const addStock = async (result: StockInfo) => {
  addLoading.value = true
  try {
    await stockInfoApi.addStock(
      result.stockCode,
      result.stockName,
      result.market || 'KRX',
      result.etfType || 'NORMAL'
    )
    showSnackbar(`${result.stockName} 종목이 추가되었습니다.`, 'success')
    addDialog.value = false
    await refreshAll()
  } catch (error: any) {
    console.error('종목 추가 실패:', error)
    showSnackbar(error.response?.data?.message || '종목 추가에 실패했습니다.', 'error')
  } finally {
    addLoading.value = false
  }
}

const showSnackbar = (message: string, color: string) => {
  snackbar.value = { show: true, message, color }
}

// 검색/필터 변경 시 페이지 1로 리셋
watch([search, filterActive, filterEtfType], () => {
  currentPage.value = 1
})

onMounted(async () => {
  await refreshAll()
})
</script>

<style scoped>
.v-data-table { font-size: 0.9rem; }
.fill-height { height: 100%; }

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
</style>