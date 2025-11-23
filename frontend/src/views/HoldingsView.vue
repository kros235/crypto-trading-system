<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main>
      <v-container fluid>
        <v-row>
          <v-col cols="12">
            <h1 class="text-h4 mb-4">보유 자산</h1>
          </v-col>
        </v-row>

        <v-row>
          <v-col cols="12" md="3">
            <v-card>
              <v-card-title>총 투자금액</v-card-title>
              <v-card-text class="text-h5">
                {{ formatCurrency(stats.totalHoldingAmount) }}
              </v-card-text>
            </v-card>
          </v-col>
          <v-col cols="12" md="3">
            <v-card>
              <v-card-title>현재 평가액</v-card-title>
              <v-card-text class="text-h5">
                {{ formatCurrency(stats.totalCurrentValue) }}
              </v-card-text>
            </v-card>
          </v-col>
          <v-col cols="12" md="3">
            <v-card>
              <v-card-title>평가 손익</v-card-title>
              <v-card-text
                class="text-h5"
                :class="stats.totalProfitLoss >= 0 ? 'text-red' : 'text-blue'"
              >
                {{ formatCurrency(stats.totalProfitLoss) }}
              </v-card-text>
            </v-card>
          </v-col>
          <v-col cols="12" md="3">
            <v-card>
              <v-card-title>수익률</v-card-title>
              <v-card-text
                class="text-h5"
                :class="stats.totalProfitLossPct >= 0 ? 'text-red' : 'text-blue'"
              >
                {{ stats.totalProfitLossPct.toFixed(2) }}%
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <v-row class="mt-4">
          <v-col cols="12">
            <v-card>
              <v-card-title>
                <v-icon class="mr-2">mdi-wallet</v-icon>
                보유 목록 ({{ holdings.length }}건)
                <v-spacer />
                <v-btn
                  color="primary"
                  @click="loadHoldings"
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
                  :items="holdings"
                  :loading="loading"
                  items-per-page="10"
                >
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

                  <template v-slot:item.currentPrice="{ item }">
                    <span v-if="item.currentPrice">
                      {{ formatCurrency(item.currentPrice) }}
                    </span>
                    <span v-else class="text-grey">-</span>
                  </template>

                  <template v-slot:item.currentValue="{ item }">
                    <span v-if="item.currentPrice">
                      {{ formatCurrency(item.currentPrice * item.quantity) }}
                    </span>
                    <span v-else class="text-grey">-</span>
                  </template>

                  <template v-slot:item.profitLoss="{ item }">
                    <span
                      v-if="item.currentProfitLoss !== null && item.currentProfitLoss !== undefined"
                      :class="item.currentProfitLoss >= 0 ? 'text-red' : 'text-blue'"
                    >
                      {{ formatCurrency(item.currentProfitLoss) }}
                      <br>
                      ({{ item.currentProfitLossPct?.toFixed(2) }}%)
                    </span>
                    <span v-else class="text-grey">-</span>
                  </template>

                  <template v-slot:item.createdAt="{ item }">
                    {{ formatDateTime(item.createdAt) }}
                  </template>

                  <template v-slot:item.actions="{ item }">
                    <v-btn
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
          <div v-if="selectedHolding">
            <p><strong>코인:</strong> {{ selectedHolding.coinSymbol }}</p>
            <p><strong>보유 수량:</strong> {{ formatNumber(selectedHolding.quantity, 8) }}</p>
            <p><strong>매수 평균가:</strong> {{ formatCurrency(selectedHolding.price) }}</p>
            <p v-if="selectedHolding.currentPrice">
              <strong>현재가:</strong> {{ formatCurrency(selectedHolding.currentPrice) }}
            </p>
            <p v-if="selectedHolding.currentProfitLoss !== null">
              <strong>예상 손익:</strong>
              <span :class="selectedHolding.currentProfitLoss >= 0 ? 'text-red' : 'text-blue'">
                {{ formatCurrency(selectedHolding.currentProfitLoss) }}
                ({{ selectedHolding.currentProfitLossPct?.toFixed(2) }}%)
              </span>
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
        <v-card-title>보유 자산 상세</v-card-title>
        <v-card-text>
          <div v-if="selectedHolding">
            <v-list>
              <v-list-item>
                <v-list-item-title>거래 ID</v-list-item-title>
                <v-list-item-subtitle>{{ selectedHolding.transactionId }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>코인</v-list-item-title>
                <v-list-item-subtitle>{{ selectedHolding.coinSymbol }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>보유 수량</v-list-item-title>
                <v-list-item-subtitle>{{ formatNumber(selectedHolding.quantity, 8) }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>매수 가격</v-list-item-title>
                <v-list-item-subtitle>{{ formatCurrency(selectedHolding.price) }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>투자 금액</v-list-item-title>
                <v-list-item-subtitle>{{ formatCurrency(selectedHolding.totalAmount) }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item v-if="selectedHolding.currentPrice">
                <v-list-item-title>현재가</v-list-item-title>
                <v-list-item-subtitle>{{ formatCurrency(selectedHolding.currentPrice) }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item v-if="selectedHolding.currentPrice">
                <v-list-item-title>현재 평가액</v-list-item-title>
                <v-list-item-subtitle>
                  {{ formatCurrency(selectedHolding.currentPrice * selectedHolding.quantity) }}
                </v-list-item-subtitle>
              </v-list-item>
              <v-list-item v-if="selectedHolding.targetSellPrice">
                <v-list-item-title>목표 매도가</v-list-item-title>
                <v-list-item-subtitle>{{ formatCurrency(selectedHolding.targetSellPrice) }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item v-if="selectedHolding.stopLossPrice">
                <v-list-item-title>손절가</v-list-item-title>
                <v-list-item-subtitle>{{ formatCurrency(selectedHolding.stopLossPrice) }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>매수 시각</v-list-item-title>
                <v-list-item-subtitle>{{ formatDateTime(selectedHolding.createdAt) }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item v-if="selectedHolding.note">
                <v-list-item-title>메모</v-list-item-title>
                <v-list-item-subtitle>{{ selectedHolding.note }}</v-list-item-subtitle>
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
import { ref, onMounted } from 'vue'
import { transactionApi } from '@/api'
import type { Transaction, DashboardStats } from '@/types'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'

const sidebarRef = ref()

const holdings = ref<Transaction[]>([])
const stats = ref<DashboardStats>({
  totalHoldingAmount: 0,
  totalCurrentValue: 0,
  totalProfitLoss: 0,
  totalProfitLossPct: 0,
  realizedProfitLoss: 0,
  soldCount: 0,
  totalBuyCount: 0,
  totalSellCount: 0,
  currentHoldingCount: 0,
  todayBuyAmount: 0,
  todaySellAmount: 0,
  todayBuyCount: 0,
  todaySellCount: 0,
  dailyLimitAmount: 0,
  remainingDailyLimit: 0
})

const loading = ref(false)
const sellLoading = ref(false)

const sellDialog = ref(false)
const detailDialog = ref(false)
const selectedHolding = ref<Transaction | null>(null)
const sellPrice = ref(0)

const snackbar = ref(false)
const snackbarMessage = ref('')
const snackbarColor = ref('success')

const headers = [
  { title: '거래 ID', key: 'transactionId', align: 'center' },
  { title: '코인', key: 'coinSymbol', align: 'center' },
  { title: '수량', key: 'quantity', align: 'end' },
  { title: '매수가', key: 'price', align: 'end' },
  { title: '투자금액', key: 'totalAmount', align: 'end' },
  { title: '현재가', key: 'currentPrice', align: 'end' },
  { title: '평가액', key: 'currentValue', align: 'end' },
  { title: '평가손익', key: 'profitLoss', align: 'end' },
  { title: '매수시각', key: 'createdAt', align: 'center' },
  { title: '액션', key: 'actions', align: 'center', sortable: false }
]

const loadHoldings = async () => {
  loading.value = true
  try {
    const [holdingsResponse, statsResponse] = await Promise.all([
      transactionApi.getHoldings(),
      transactionApi.getStats()
    ])

    holdings.value = holdingsResponse.data
    stats.value = statsResponse.data
  } catch (error: any) {
    showSnackbar(error.response?.data?.message || '보유 자산 조회 실패', 'error')
  } finally {
    loading.value = false
  }
}

const openSellDialog = (holding: Transaction) => {
  selectedHolding.value = holding
  sellPrice.value = holding.currentPrice || holding.price
  sellDialog.value = true
}

const confirmSell = async () => {
  if (!selectedHolding.value || sellPrice.value <= 0) {
    showSnackbar('유효한 매도 가격을 입력해주세요', 'error')
    return
  }

  sellLoading.value = true
  try {
    await transactionApi.sell(selectedHolding.value.transactionId, {
      soldPrice: sellPrice.value
    })

    showSnackbar('매도가 완료되었습니다', 'success')
    sellDialog.value = false
    loadHoldings()
  } catch (error: any) {
    showSnackbar(error.response?.data?.message || '매도 처리 실패', 'error')
  } finally {
    sellLoading.value = false
  }
}

const viewDetail = (holding: Transaction) => {
  selectedHolding.value = holding
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

const showSnackbar = (message: string, color: string) => {
  snackbarMessage.value = message
  snackbarColor.value = color
  snackbar.value = true
}

onMounted(() => {
  loadHoldings()
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