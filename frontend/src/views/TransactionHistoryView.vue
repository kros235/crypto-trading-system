<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main class="bg-grey-lighten-3">
      <v-container fluid>
        <v-row class="mb-4">
          <v-col>
            <h1 class="text-h4">
              <v-icon class="mr-2">mdi-history</v-icon>
              거래 내역
            </h1>
            <p class="text-subtitle-1 text-grey">매수/매도 거래 기록을 확인하세요</p>
          </v-col>
        </v-row>

        <v-row>
          <v-col cols="12">
            <v-card>
              <v-card-title class="d-flex align-center">
                <v-icon class="mr-2">mdi-filter</v-icon>
                검색 필터
                <v-spacer />
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
                    <v-select
                      v-model="filters.coinSymbol"
                      :items="coinOptions"
                      label="코인 선택"
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

        <v-row class="mt-4">
          <v-col cols="12">
            <v-card>
              <v-card-title class="d-flex align-center">
                <v-icon class="mr-2">mdi-history</v-icon>
                거래 목록
                <v-spacer />
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
                  <template v-slot:item.type="{ item }">
                    <v-chip
                      :color="item.type === 'BUY' ? 'blue' : 'orange'"
                      size="small"
                    >
                      {{ item.type === 'BUY' ? '매수' : '매도' }}
                    </v-chip>
                  </template>

                  <template v-slot:item.coinSymbol="{ item }">
                    <strong>{{ item.coinSymbol }}</strong>
                  </template>

                  <template v-slot:item.quantity="{ item }">
                    {{ formatNumber(item.quantity, 8) }}
                  </template>

                  <template v-slot:item.price="{ item }">
                    {{ formatCurrency(item.price) }}
                  </template>

                  <template v-slot:item.totalAmount="{ item }">
                    {{ formatCurrency(item.totalAmount) }}
                  </template>

                  <template v-slot:item.profitLoss="{ item }">
                    <span
                      v-if="item.profitLoss !== null && item.profitLoss !== undefined"
                      :class="item.profitLoss >= 0 ? 'text-red' : 'text-blue'"
                    >
                      {{ formatCurrency(item.profitLoss) }}
                      ({{ item.profitLossPct?.toFixed(2) }}%)
                    </span>
                    <span v-else-if="item.currentProfitLoss !== null && item.currentProfitLoss !== undefined">
                      <span :class="item.currentProfitLoss >= 0 ? 'text-red' : 'text-blue'">
                        {{ formatCurrency(item.currentProfitLoss) }}
                        ({{ item.currentProfitLossPct?.toFixed(2) }}%)
                      </span>
                    </span>
                    <span v-else class="text-grey">-</span>
                  </template>

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

    <v-dialog v-model="sellDialog" max-width="500">
      <v-card>
        <v-card-title>매도 처리</v-card-title>
        <v-card-text>
          <div v-if="selectedTransaction">
            <p><strong>코인:</strong> {{ selectedTransaction.coinSymbol }}</p>
            <p><strong>수량:</strong> {{ formatNumber(selectedTransaction.quantity, 8) }}</p>
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
          <v-btn
            color="orange"
            @click="confirmSell"
            :loading="sellLoading"
          >
            매도 확인
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

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
                <td class="font-weight-bold bg-grey-lighten-4">코인</td>
                <td>{{ selectedTransaction.coinSymbol }}</td>
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
                <td>{{ formatNumber(selectedTransaction.quantity, 8) }}</td>
              </tr>
              <tr>
                <td class="font-weight-bold bg-grey-lighten-4">가격</td>
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
              <tr>
                <td class="font-weight-bold bg-grey-lighten-4">거래 시각</td>
                <td>{{ formatDateTime(selectedTransaction.createdAt) }}</td>
              </tr>
              <tr v-if="selectedTransaction.note">
                <td class="font-weight-bold bg-grey-lighten-4">메모</td>
                <td>{{ selectedTransaction.note }}</td>
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
import { ref, computed, onMounted } from 'vue'
import { transactionApi } from '@/api'
import { useCoinStore } from '@/stores/coin'
import type { Transaction } from '@/types'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'
import HelpButton from '@/components/HelpButton.vue'

const coinStore = useCoinStore()
const sidebarRef = ref()

const helpContents = {
  filter: {
    title: '🔍 검색 필터 안내',
    content: `
      <p class="help-intro">거래 내역을 다양한 조건으로 필터링하여 원하는 기록을 찾을 수 있습니다.</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>코인 선택</strong>
        <span class="help-desc">특정 코인의 거래 내역만 보고 싶을 때 선택합니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>상태</strong>
        <span class="help-desc">보유 중: 아직 매도하지 않은 거래<br/>매도 완료: 이미 매도된 거래<br/>취소됨: 취소된 거래</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>기간 검색</strong>
        <span class="help-desc">시작일과 종료일을 지정하여 특정 기간의 거래만 조회합니다.</span></p>
      <p class="help-note">💡 <strong>Tip:</strong> 필터를 조합하여 사용하면 더 정확한 검색이 가능합니다. 예: "BTC + 보유 중 + 이번 달"</p>
    `
  },
  transactionList: {
    title: '📋 거래 목록 안내',
    content: `
      <p class="help-intro">모든 매수/매도 거래 기록을 확인할 수 있습니다.</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>거래 유형</strong>
        <span class="help-desc">매수(파란색): 코인을 구매한 거래<br/>매도(주황색): 코인을 판매한 거래</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>손익</strong>
        <span class="help-desc">빨간색(+): 수익 발생<br/>파란색(-): 손실 발생<br/>보유 중인 경우 현재가 기준 평가손익 표시</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>상태</strong>
        <span class="help-desc">보유 중(초록): 현재 보유 중인 코인<br/>매도 완료(파랑): 이미 매도된 거래<br/>취소됨(회색): 취소된 거래</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>액션 버튼</strong>
        <span class="help-desc">매도: 보유 중인 코인을 현재가 또는 지정가로 매도<br/>상세: 거래의 세부 정보 확인</span></p>
      <p class="help-note">💡 <strong>Tip:</strong> 테이블 헤더를 클릭하면 해당 컬럼 기준으로 정렬할 수 있습니다.</p>
    `
  }
}

const transactions = ref<Transaction[]>([])
const loading = ref(false)
const sellLoading = ref(false)
const currentPage = ref(1)
const totalPages = ref(1)
const itemsPerPage = ref(20)

const filters = ref({
  coinSymbol: null as string | null,
  status: null as string | null,
  startDate: null as string | null,
  endDate: null as string | null
})

const sellDialog = ref(false)
const detailDialog = ref(false)
const selectedTransaction = ref<Transaction | null>(null)
const sellPrice = ref(0)

const snackbar = ref(false)
const snackbarMessage = ref('')
const snackbarColor = ref('success')

const headers = [
  { title: '거래 ID', key: 'transactionId', align: 'center' },
  { title: '코인', key: 'coinSymbol', align: 'center' },
  { title: '유형', key: 'type', align: 'center' },
  { title: '수량', key: 'quantity', align: 'end' },
  { title: '가격', key: 'price', align: 'end' },
  { title: '총 금액', key: 'totalAmount', align: 'end' },
  { title: '손익', key: 'profitLoss', align: 'end' },
  { title: '상태', key: 'status', align: 'center' },
  { title: '거래 시각', key: 'createdAt', align: 'center' },
  { title: '액션', key: 'actions', align: 'center', sortable: false }
]

const coinOptions = computed(() => {
  return coinStore.coins.map(coin => ({
    title: coin.nameKr,
    value: coin.symbol
  }))
})

const statusOptions = [
  { title: '보유 중', value: 'HOLDING' },
  { title: '매도 완료', value: 'SOLD' },
  { title: '취소됨', value: 'CANCELLED' }
]

const loadTransactions = async () => {
  loading.value = true
  try {
    const response = await transactionApi.getAll(currentPage.value - 1, itemsPerPage.value)
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

    if (filters.value.coinSymbol) params.coinSymbol = filters.value.coinSymbol
    if (filters.value.status) params.status = filters.value.status
    if (filters.value.startDate) params.startDate = new Date(filters.value.startDate).toISOString()
    if (filters.value.endDate) {
      const endDate = new Date(filters.value.endDate)
      endDate.setHours(23, 59, 59, 999)
      params.endDate = endDate.toISOString()
    }

    const response = await transactionApi.search(params)
    transactions.value = response.data.content
    totalPages.value = response.data.totalPages
  } catch (error: any) {
    showSnackbar(error.response?.data?.message || '검색 실패', 'error')
  } finally {
    loading.value = false
  }
}

const openSellDialog = (transaction: Transaction) => {
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
    await transactionApi.sell(selectedTransaction.value.transactionId, {
      soldPrice: sellPrice.value
    })

    showSnackbar('매도가 완료되었습니다', 'success')
    sellDialog.value = false
    loadTransactions()
  } catch (error: any) {
    showSnackbar(error.response?.data?.message || '매도 처리 실패', 'error')
  } finally {
    sellLoading.value = false
  }
}

const viewDetail = (transaction: Transaction) => {
  selectedTransaction.value = transaction
  detailDialog.value = true
}

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW'
  }).format(value)
}

const formatNumber = (value: number, decimals: number) => {
  return value.toFixed(decimals)
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
  coinStore.fetchActiveCoins()
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