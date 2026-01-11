<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main class="bg-grey-lighten-3">
      <v-container fluid>
        <v-row>
          <v-col cols="12">
            <h1 class="text-h4 mb-4">거래 내역</h1>
          </v-col>
        </v-row>

        <v-row>
          <v-col cols="12">
            <v-card>
              <v-card-title>
                <v-icon class="mr-2">mdi-filter</v-icon>
                검색 필터
              </v-card-title>
              <v-card-text>
                <v-row>
                  <v-col cols="12" md="3">
                    <v-select
                      v-model="filters.coinSymbol"
                      :items="coinOptions"
                      label="코인 선택"
                      clearable
                      density="compact"
                    />
                  </v-col>
                  <v-col cols="12" md="3">
                    <v-select
                      v-model="filters.status"
                      :items="statusOptions"
                      label="상태"
                      clearable
                      density="compact"
                    />
                  </v-col>
                  <v-col cols="12" md="2">
                    <v-text-field
                      v-model="filters.startDate"
                      type="date"
                      label="시작일"
                      density="compact"
                      clearable
                    />
                  </v-col>
                  <v-col cols="12" md="2">
                    <v-text-field
                      v-model="filters.endDate"
                      type="date"
                      label="종료일"
                      density="compact"
                      clearable
                    />
                  </v-col>
                  <v-col cols="12" md="2" class="d-flex align-center">
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
              <v-card-title>
                <v-icon class="mr-2">mdi-history</v-icon>
                거래 목록
                <v-spacer />
                <v-btn
                  color="primary"
                  @click="loadTransactions"
                  :loading="loading"
                  size="small"
                >
                  <v-icon start>mdi-refresh</v-icon>
                  새로고침
                </v-btn>
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
                    <v-btn
                      v-if="item.status === 'HOLDING'"
                      color="orange"
                      size="small"
                      @click="openSellDialog(item)"
                    >
                      매도
                    </v-btn>
                    <v-btn
                      color="grey"
                      size="small"
                      @click="viewDetail(item)"
                      class="ml-1"
                    >
                      상세
                    </v-btn>
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

    <v-dialog v-model="detailDialog" max-width="600">
      <v-card>
        <v-card-title>거래 상세 정보</v-card-title>
        <v-card-text>
          <div v-if="selectedTransaction">
            <v-list>
              <v-list-item>
                <v-list-item-title>거래 ID</v-list-item-title>
                <v-list-item-subtitle>{{ selectedTransaction.transactionId }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>코인</v-list-item-title>
                <v-list-item-subtitle>{{ selectedTransaction.coinSymbol }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>거래 유형</v-list-item-title>
                <v-list-item-subtitle>
                  <v-chip :color="selectedTransaction.type === 'BUY' ? 'blue' : 'orange'" size="small">
                    {{ selectedTransaction.type === 'BUY' ? '매수' : '매도' }}
                  </v-chip>
                </v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>수량</v-list-item-title>
                <v-list-item-subtitle>{{ formatNumber(selectedTransaction.quantity, 8) }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>가격</v-list-item-title>
                <v-list-item-subtitle>{{ formatCurrency(selectedTransaction.price) }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>총 금액</v-list-item-title>
                <v-list-item-subtitle>{{ formatCurrency(selectedTransaction.totalAmount) }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>수수료</v-list-item-title>
                <v-list-item-subtitle>{{ formatCurrency(selectedTransaction.fee) }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>상태</v-list-item-title>
                <v-list-item-subtitle>
                  <v-chip :color="getStatusColor(selectedTransaction.status)" size="small">
                    {{ getStatusText(selectedTransaction.status) }}
                  </v-chip>
                </v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>거래 시각</v-list-item-title>
                <v-list-item-subtitle>{{ formatDateTime(selectedTransaction.createdAt) }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item v-if="selectedTransaction.note">
                <v-list-item-title>메모</v-list-item-title>
                <v-list-item-subtitle>{{ selectedTransaction.note }}</v-list-item-subtitle>
              </v-list-item>
            </v-list>
          </div>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="detailDialog = false">닫기</v-btn>
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

const coinStore = useCoinStore()
const sidebarRef = ref()

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
</style>