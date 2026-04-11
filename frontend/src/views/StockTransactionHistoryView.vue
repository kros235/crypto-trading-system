<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main class="bg-grey-lighten-3">
      <v-container fluid>
        <v-row class="mb-4">
          <v-col>
            <h1 class="text-h4">
              <!-- ⭐ 변경: mdi-chart-line → mdi-history (코인 거래 내역과 동일한 아이콘) -->
              <v-icon class="mr-2">mdi-history</v-icon>
              주식 거래 내역
            </h1>
            <p class="text-subtitle-1 text-grey">매수/매도 거래 기록을 확인하세요</p>
          </v-col>
        </v-row>

        <!-- 검색 필터 -->
        <v-row>
          <v-col cols="12">
            <v-card>
              <v-card-title class="d-flex align-center">
                <v-icon class="mr-2">mdi-filter</v-icon>
                검색 필터
                <v-spacer />
                <!-- ⭐ 추가: HelpButton (Phase 1과 동일한 구조) -->
                <HelpButton
                  :use-dialog="true"
                  :dialog-title="helpContents.filter.title"
                  :dialog-content="helpContents.filter.content"
                  color="grey-darken-1"
                />
              </v-card-title>
              <v-card-text>
                <v-row align="center">
                  <v-col cols="12" md="3">
                    <!-- ⭐ 수정: items를 stockOptions 배열로, item-title/item-value 제거 후 title/value 구조로 통일 (Phase 1 coinOptions 방식과 동일) -->
                    <v-select
                      v-model="filters.stockCode"
                      :items="stockOptions"
                      item-title="title"
                      item-value="value"
                      label="종목 선택"
                      clearable
                      density="compact"
                      hide-details
                    />
                  </v-col>
                  <v-col cols="12" md="3">
                    <v-select
                      v-model="filters.status"
                      :items="statusOptions"
                      label="상태"
                      clearable
                      density="compact"
                      hide-details
                    />
                  </v-col>
                  <v-col cols="12" md="2">
                    <v-text-field
                      v-model="filters.startDate"
                      type="date"
                      label="시작일"
                      density="compact"
                      clearable
                      hide-details
                    />
                  </v-col>
                  <v-col cols="12" md="2">
                    <v-text-field
                      v-model="filters.endDate"
                      type="date"
                      label="종료일"
                      density="compact"
                      clearable
                      hide-details
                    />
                  </v-col>
                  <v-col cols="12" md="2">
                    <v-btn
                      color="primary"
                      @click="searchTransactions"
                      :loading="loading"
                      block
                    >
                      <v-icon start>mdi-magnify</v-icon>
                      검색
                    </v-btn>
                  </v-col>
                </v-row>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- 거래 목록 -->
        <v-row class="mt-4">
          <v-col cols="12">
            <v-card>
              <v-card-title class="d-flex align-center">
                <v-icon class="mr-2">mdi-history</v-icon>
                거래 목록
                <v-spacer />
                <!-- ⭐ 수정: variant="outlined" size="small" → variant="flat" + min-width (Phase 1 스타일) -->
                <v-btn
                  color="primary"
                  @click="loadTransactions"
                  :loading="loading"
                  variant="flat"
                  class="mr-2"
                  min-width="135"
                >
                  <v-icon start>mdi-refresh</v-icon>
                  새로고침
                </v-btn>
                <!-- ⭐ 추가: HelpButton -->
                <HelpButton
                  :use-dialog="true"
                  :dialog-title="helpContents.transactionList.title"
                  :dialog-content="helpContents.transactionList.content"
                  color="grey-darken-1"
                />
              </v-card-title>

              <v-card-text>
                <v-data-table
                  :headers="headers"
                  :items="transactions"
                  :loading="loading"
                  :items-per-page="itemsPerPage"
                  hide-default-footer
                >
                  <!-- ⭐ 수정: 거래 ID 컬럼 추가 -->
                  <template v-slot:item.transactionId="{ item }">
                    {{ item.transactionId }}
                  </template>

                  <!-- ⭐ 수정: 종목 표시 (종목코드 + 종목명) -->
                  <template v-slot:item.stockCode="{ item }">
                    <strong>{{ item.stockCode }}</strong>
                    <div class="text-caption text-grey">{{ item.stockName || '' }}</div>
                  </template>

                  <!-- ⭐ 수정: 유형 칩 스타일 Phase 1과 동일 (variant 제거) -->
                  <template v-slot:item.type="{ item }">
                    <v-chip
                      :color="item.type === 'BUY' ? 'blue' : 'orange'"
                      size="small"
                    >
                      {{ item.type === 'BUY' ? '매수' : '매도' }}
                    </v-chip>
                  </template>

                  <!-- ⭐ 수정: 수량 단위 "주" 표시 -->
                  <template v-slot:item.quantity="{ item }">
                    {{ item.quantity?.toLocaleString() }}주
                  </template>

                  <template v-slot:item.price="{ item }">
                    {{ formatCurrency(item.price) }}
                  </template>

                  <template v-slot:item.totalAmount="{ item }">
                    {{ formatCurrency(item.totalAmount) }}
                  </template>

                  <!-- ⭐ 수정: 손익 색상/bold Phase 1과 동일 (text-red=수익, text-blue=손실) -->
                  <template v-slot:item.profitLoss="{ item }">
                    <span
                      v-if="item.profitLoss !== null && item.profitLoss !== undefined"
                      :class="item.profitLoss >= 0 ? 'text-red' : 'text-blue'"
                    >
                      {{ formatCurrency(item.profitLoss) }}
                      ({{ Number(item.profitLossPct).toFixed(2) }}%)
                    </span>
                    <span v-else-if="item.currentProfitLoss !== null && item.currentProfitLoss !== undefined">
                      <span :class="item.currentProfitLoss >= 0 ? 'text-red' : 'text-blue'">
                        {{ formatCurrency(item.currentProfitLoss) }}
                        ({{ Number(item.currentProfitLossPct).toFixed(2) }}%)
                      </span>
                    </span>
                    <span v-else class="text-grey">-</span>
                  </template>

                  <!-- ⭐ 수정: 상태 칩 Phase 1과 동일 (variant 제거, 색상 통일) -->
                  <template v-slot:item.status="{ item }">
                    <v-chip
                      :color="getStatusColor(item.status)"
                      size="small"
                    >
                      {{ getStatusText(item.status) }}
                    </v-chip>
                  </template>

                  <template v-slot:item.createdAt="{ item }">
                    {{ formatDateTime(item.createdAt) }}
                  </template>

                  <!-- ⭐ 수정: 수동매도+연필 → 매도+상세 버튼 (Phase 1과 동일한 구조/스타일) -->
                  <template v-slot:item.actions="{ item }">
                    <div class="d-flex align-center justify-center ga-1" style="min-width: 130px;">
                      <v-btn
                        v-if="item.status === 'HOLDING'"
                        color="orange"
                        size="small"
                        min-width="60"
                        @click="openSellDialog(item)"
                      >
                        매도
                      </v-btn>
                      <div v-else style="width: 60px;"></div>
                      <v-btn
                        color="grey"
                        size="small"
                        min-width="60"
                        @click="viewDetail(item)"
                      >
                        상세
                      </v-btn>
                    </div>
                  </template>
                </v-data-table>

                <v-pagination
                  v-model="currentPage"
                  :length="totalPages"
                  @update:model-value="loadTransactions"
                  class="mt-4"
                />
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </v-main>

    <!-- 매도 다이얼로그 (Phase 1과 동일한 구조) -->
    <v-dialog v-model="sellDialog" max-width="500">
      <v-card>
        <v-card-title>매도 처리</v-card-title>
        <v-card-text>
          <div v-if="selectedTransaction">
            <p><strong>종목:</strong> {{ selectedTransaction.stockCode }} {{ selectedTransaction.stockName }}</p>
            <p><strong>수량:</strong> {{ selectedTransaction.quantity }}주</p>
            <p><strong>매수가:</strong> {{ formatCurrency(selectedTransaction.price) }}</p>
            <p v-if="selectedTransaction.currentPrice">
              <strong>현재가:</strong> {{ formatCurrency(selectedTransaction.currentPrice) }}
            </p>
            <v-text-field
              v-model.number="sellPrice"
              label="매도 가격"
              type="number"
              :rules="[v => v > 0 || '가격은 0보다 커야 합니다']"
              class="mt-4"
            />
          </div>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="sellDialog = false">취소</v-btn>
          <v-btn color="orange" @click="confirmSell" :loading="sellLoading">
            매도 확인
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 상세 다이얼로그 (Phase 1과 동일한 구조, 주식 전용 필드 추가) -->
    <v-dialog v-model="detailDialog" max-width="500">
      <v-card>
        <v-card-title class="bg-primary text-white">
          <v-icon class="mr-2">mdi-file-document-outline</v-icon>
          거래 상세 정보
        </v-card-title>
        <v-card-text class="pa-0">
          <v-table v-if="selectedTransaction" density="comfortable">
            <tbody>
              <tr>
                <td class="font-weight-bold bg-grey-lighten-4" width="35%">거래 ID</td>
                <td>{{ selectedTransaction.transactionId }}</td>
              </tr>
              <tr>
                <td class="font-weight-bold bg-grey-lighten-4">종목코드</td>
                <td><strong>{{ selectedTransaction.stockCode }}</strong></td>
              </tr>
              <tr>
                <td class="font-weight-bold bg-grey-lighten-4">종목명</td>
                <td>{{ selectedTransaction.stockName || '-' }}</td>
              </tr>
              <tr>
                <td class="font-weight-bold bg-grey-lighten-4">거래 유형</td>
                <td>
                  <v-chip :color="selectedTransaction.type === 'BUY' ? 'blue' : 'orange'" size="small">
                    {{ selectedTransaction.type === 'BUY' ? '매수' : '매도' }}
                  </v-chip>
                </td>
              </tr>
              <tr>
                <td class="font-weight-bold bg-grey-lighten-4">수량</td>
                <td>{{ selectedTransaction.quantity }}주</td>
              </tr>
              <tr>
                <td class="font-weight-bold bg-grey-lighten-4">매수가</td>
                <td>{{ formatCurrency(selectedTransaction.price) }}</td>
              </tr>
              <tr>
                <td class="font-weight-bold bg-grey-lighten-4">총 금액</td>
                <td>{{ formatCurrency(selectedTransaction.totalAmount) }}</td>
              </tr>
              <tr>
                <td class="font-weight-bold bg-grey-lighten-4">수수료</td>
                <td>{{ formatCurrency(selectedTransaction.fee) }}</td>
              </tr>
              <tr>
                <td class="font-weight-bold bg-grey-lighten-4">상태</td>
                <td>
                  <v-chip :color="getStatusColor(selectedTransaction.status)" size="small">
                    {{ getStatusText(selectedTransaction.status) }}
                  </v-chip>
                </td>
              </tr>
              <!-- ⭐ 주식 전용: 보유일수 -->
              <tr>
                <td class="font-weight-bold bg-grey-lighten-4">보유일수</td>
                <td>
                  <v-chip
                    :color="selectedTransaction.holdingDays >= 20 ? 'red' : selectedTransaction.holdingDays >= 15 ? 'orange' : 'default'"
                    size="small"
                  >
                    {{ selectedTransaction.holdingDays }}일
                  </v-chip>
                </td>
              </tr>
              <tr>
                <td class="font-weight-bold bg-grey-lighten-4">거래 시각</td>
                <td>{{ formatDateTime(selectedTransaction.createdAt) }}</td>
              </tr>
              <tr v-if="selectedTransaction.soldAt">
                <td class="font-weight-bold bg-grey-lighten-4">매도 시각</td>
                <td>{{ formatDateTime(selectedTransaction.soldAt) }}</td>
              </tr>
              <tr v-if="selectedTransaction.soldPrice">
                <td class="font-weight-bold bg-grey-lighten-4">매도가</td>
                <td>{{ formatCurrency(selectedTransaction.soldPrice) }}</td>
              </tr>
              <tr v-if="selectedTransaction.profitLoss !== null && selectedTransaction.profitLoss !== undefined">
                <td class="font-weight-bold bg-grey-lighten-4">실현 손익</td>
                <td :class="selectedTransaction.profitLoss >= 0 ? 'text-red' : 'text-blue'">
                  {{ formatCurrency(selectedTransaction.profitLoss) }}
                  ({{ Number(selectedTransaction.profitLossPct).toFixed(2) }}%)
                </td>
              </tr>
              <!-- ⭐ 주식 전용: 환율 -->
              <tr v-if="selectedTransaction.exchangeRate">
                <td class="font-weight-bold bg-grey-lighten-4">환율</td>
                <td>{{ selectedTransaction.exchangeRate }}</td>
              </tr>
              <tr v-if="selectedTransaction.note">
                <td class="font-weight-bold bg-grey-lighten-4">📌 거래 사유</td>
                <td style="color: #1976d2; font-size: 13px;">{{ selectedTransaction.note }}</td>
              </tr>
            </tbody>
          </v-table>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="flat" color="primary" @click="detailDialog = false">닫기</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-snackbar v-model="snackbar" :color="snackbarColor" :timeout="3000">
      {{ snackbarMessage }}
    </v-snackbar>
  </v-app>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { stockTransactionApi, stockInfoApi } from '@/api/stock'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'
import HelpButton from '@/components/HelpButton.vue'

const sidebarRef = ref()

// ⭐ 추가: helpContents (Phase 1 구조 재사용, 주식 전용 내용으로 변경)
const helpContents = {
  filter: {
    title: '🔍 검색 필터 안내',
    content: `
      <p class="help-intro">주식 거래 내역을 다양한 조건으로 필터링하여 원하는 기록을 찾을 수 있습니다.</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>종목 선택</strong>
        <span class="help-desc">특정 종목의 거래 내역만 보고 싶을 때 선택합니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>상태</strong>
        <span class="help-desc">보유 중: 아직 매도하지 않은 거래<br/>매도 완료: 이미 매도된 거래<br/>취소됨: 취소된 거래</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>기간 검색</strong>
        <span class="help-desc">시작일과 종료일을 지정하여 특정 기간의 거래만 조회합니다.</span></p>
      <p class="help-note">💡 <strong>Tip:</strong> 필터를 조합하여 사용하면 더 정확한 검색이 가능합니다. 예: "409820 + 보유 중 + 이번 달"</p>
    `
  },
  transactionList: {
    title: '📋 거래 목록 안내',
    content: `
      <p class="help-intro">모든 매수/매도 거래 기록을 확인할 수 있습니다.</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>거래 유형</strong>
        <span class="help-desc">매수(파란색): 주식을 구매한 거래<br/>매도(주황색): 주식을 판매한 거래</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>손익</strong>
        <span class="help-desc">빨간색(+): 수익 발생<br/>파란색(-): 손실 발생<br/>보유 중인 경우 현재가 기준 평가손익 표시</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>상태</strong>
        <span class="help-desc">보유 중(초록): 현재 보유 중인 주식<br/>매도 완료(파랑): 이미 매도된 거래<br/>취소됨(회색): 취소된 거래</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>보유일</strong>
        <span class="help-desc">레버리지 ETF는 장기 보유 시 가치 침식이 발생합니다.<br/>15일 이상: 주황색 경고<br/>20일 이상: 빨간색 위험</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>액션 버튼</strong>
        <span class="help-desc">매도: 보유 중인 주식을 현재가 또는 지정가로 매도<br/>상세: 거래의 세부 정보 확인</span></p>
      <p class="help-note">💡 <strong>Tip:</strong> 테이블 헤더를 클릭하면 해당 컬럼 기준으로 정렬할 수 있습니다.</p>
    `
  }
}

const transactions = ref<any[]>([])
const loading = ref(false)
const sellLoading = ref(false)
const currentPage = ref(1)
const totalPages = ref(1)
const itemsPerPage = ref(20)

const filters = ref({
  stockCode: null as string | null,
  status: null as string | null,
  startDate: null as string | null,
  endDate: null as string | null
})

const sellDialog = ref(false)
const detailDialog = ref(false)
const selectedTransaction = ref<any | null>(null)
const sellPrice = ref(0)

const snackbar = ref(false)
const snackbarMessage = ref('')
const snackbarColor = ref('success')

// ⭐ 수정: 컬럼 순서 변경 (거래ID, 종목, 유형, 수량, 매수가, 총금액, 손익, 상태, 거래시각, 액션)
// ⭐ 수정: 컬럼명 '가격' → '매수가' (주식 거래 맥락에 맞게)
// ⭐ 수정: 보유일 컬럼 헤더 가로 표기로 변경 (align center로 처리)
const headers = [
  { title: '거래 ID', key: 'transactionId', align: 'center' as const },
  { title: '종목', key: 'stockCode', align: 'center' as const },
  { title: '유형', key: 'type', align: 'center' as const },
  { title: '수량', key: 'quantity', align: 'end' as const },
  { title: '매수가', key: 'price', align: 'end' as const },
  { title: '총 금액', key: 'totalAmount', align: 'end' as const },
  { title: '손익', key: 'profitLoss', align: 'end' as const },
  { title: '상태', key: 'status', align: 'center' as const },
  { title: '보유일', key: 'holdingDays', align: 'center' as const },
  { title: '거래 시각', key: 'createdAt', align: 'center' as const },
  { title: '액션', key: 'actions', align: 'center' as const, sortable: false }
]

// ⭐ 수정: stockOptions를 { title, value } 구조로 변경 (Phase 1 coinOptions 방식 동일)
// No data available 원인: 기존 { label, value } 구조가 Vuetify v-select의 기본 item-title과 불일치
const stockOptions = ref<{ title: string; value: string }[]>([])

const statusOptions = [
  { title: '보유 중', value: 'HOLDING' },
  { title: '매도 완료', value: 'SOLD' },
  { title: '취소됨', value: 'CANCELLED' }
]

const loadTransactions = async () => {
  loading.value = true
  try {
    const response = await stockTransactionApi.getAll(currentPage.value - 1, itemsPerPage.value)
    transactions.value = response.data.content
    totalPages.value = response.data.totalPages
  } catch (error: any) {
    showSnackbar(error.response?.data?.message || '거래 내역 조회 실패', 'error')
  } finally {
    loading.value = false
  }
}

const searchTransactions = async () => {
  loading.value = true
  try {
    const params: any = {
      page: currentPage.value - 1,
      size: itemsPerPage.value
    }
    if (filters.value.stockCode) params.stockCode = filters.value.stockCode
    if (filters.value.status) params.status = filters.value.status
    if (filters.value.startDate) params.startDate = new Date(filters.value.startDate).toISOString()
    if (filters.value.endDate) {
      const endDate = new Date(filters.value.endDate)
      endDate.setHours(23, 59, 59, 999)
      params.endDate = endDate.toISOString()
    }
    const response = await stockTransactionApi.search(params)
    transactions.value = response.data.content
    totalPages.value = response.data.totalPages
  } catch (error: any) {
    showSnackbar(error.response?.data?.message || '검색 실패', 'error')
  } finally {
    loading.value = false
  }
}

// ⭐ 변경 후: res.data → res.data.data (ApiResponse 래퍼 구조 대응)
const loadStockOptions = async () => {
  try {
    const res = await stockInfoApi.getActiveStocks()
    const list = res.data.data ?? res.data  // 래퍼 구조 또는 직접 배열 모두 대응
    stockOptions.value = list.map((s: any) => ({
      title: `${s.stockCode} ${s.stockName}`,
      value: s.stockCode
    }))
  } catch (e) {
    // 종목 목록 로드 실패 시 무시
  }
}

const openSellDialog = (transaction: any) => {
  selectedTransaction.value = transaction
  sellPrice.value = transaction.currentPrice || transaction.price
  sellDialog.value = true
}

const confirmSell = async () => {
  if (!selectedTransaction.value || sellPrice.value <= 0) {
    showSnackbar('유효한 매도 가격을 입력해주세요', 'error')
    return
  }
  sellLoading.value = true
  try {
    await stockTransactionApi.sell(selectedTransaction.value.transactionId, sellPrice.value)
    showSnackbar('매도가 완료되었습니다', 'success')
    sellDialog.value = false
    loadTransactions()
  } catch (error: any) {
    showSnackbar(error.response?.data?.message || '매도 처리 실패', 'error')
  } finally {
    sellLoading.value = false
  }
}

const viewDetail = (transaction: any) => {
  selectedTransaction.value = transaction
  detailDialog.value = true
}

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW'
  }).format(value)
}

const formatDateTime = (dateString: string) => {
  return new Date(dateString).toLocaleString('ko-KR')
}

const getStatusColor = (status: string) => {
  switch (status) {
    case 'HOLDING': return 'blue'
    case 'SOLD': return 'green'
    case 'CANCELLED': return 'grey'
    default: return 'grey'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'HOLDING': return '보유 중'
    case 'SOLD': return '매도 완료'
    case 'CANCELLED': return '취소됨'
    default: return status
  }
}

const showSnackbar = (message: string, color: string) => {
  snackbarMessage.value = message
  snackbarColor.value = color
  snackbar.value = true
}

onMounted(() => {
  loadStockOptions()
  loadTransactions()
})
</script>

<style scoped>
.text-red {
  color: #f44336;
  font-weight: bold;
}

.text-blue {
  color: #2196f3;
  font-weight: bold;
}

.text-grey {
  color: #9e9e9e;
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
</style>