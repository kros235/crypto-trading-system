<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main>
      <v-container fluid>
        <v-row>
          <v-col cols="12">
            <h1 class="text-h4 mb-4">대시보드</h1>
          </v-col>
        </v-row>

        <!-- 새로운 레이아웃: 상단 통계 + 사용자 정보 영역 -->
        <v-row>
          <!-- 왼쪽: 사용자 정보 카드 -->
          <v-col cols="12" md="4">
            <v-card class="user-info-card">
              <v-card-title>
                <v-icon class="mr-2">mdi-account</v-icon>
                사용자 정보
              </v-card-title>
              <v-card-text v-if="authStore.user">
                <div class="mb-2">
                  <strong>ID:</strong> {{ authStore.user.userId }}
                </div>
                <div class="mb-2">
                  <strong>이메일:</strong> {{ authStore.user.email }}
                </div>
                <div class="mb-2">
                  <strong>역할:</strong>
                  <v-chip :color="authStore.user.role === 'ADMIN' ? 'error' : 'primary'" size="small">
                    {{ authStore.user.role }}
                  </v-chip>
                </div>
                <div class="mb-2">
                  <strong>API 키:</strong>
                  <v-chip :color="authStore.user.hasApiKey ? 'success' : 'warning'" size="small">
                    {{ authStore.user.hasApiKey ? '등록됨' : '미등록' }}
                  </v-chip>
                </div>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- 오른쪽: 2x2 그리드 (통계 상단 + 시스템/빠른액세스 하단) -->
          <v-col cols="12" md="8">
            <div class="right-section">
              <!-- 상단: 4개 통계 카드 -->
              <v-row dense class="stats-row">
                <v-col cols="6" sm="3">
                  <v-card color="success" class="pa-3" dark>
                    <div class="d-flex align-center">
                      <v-icon class="mr-2">mdi-trending-up</v-icon>
                      <div>
                        <div class="text-caption">총 손익</div>
                        <div class="text-h6 font-weight-bold">
                          {{ formatCurrency(dashboardStats.totalProfitLoss) }}
                        </div>
                        <div class="text-caption">
                          {{ dashboardStats.totalProfitLossPct >= 0 ? '+' : '' }}{{ dashboardStats.totalProfitLossPct.toFixed(2) }}%
                        </div>
                      </div>
                    </div>
                  </v-card>
                </v-col>

                <v-col cols="6" sm="3">
                  <v-card color="primary" class="pa-3" dark>
                    <div class="d-flex align-center">
                      <v-icon class="mr-2">mdi-wallet</v-icon>
                      <div>
                        <div class="text-caption">총 평가액</div>
                        <div class="text-h6 font-weight-bold">
                          {{ formatCurrency(dashboardStats.totalCurrentValue) }}
                        </div>
                        <div class="text-caption">
                          투자원금: {{ formatCurrency(dashboardStats.totalHoldingAmount) }}
                        </div>
                      </div>
                    </div>
                  </v-card>
                </v-col>

                <v-col cols="6" sm="3">
                  <v-card color="info" class="pa-3" dark>
                    <div class="d-flex align-center">
                      <v-icon class="mr-2">mdi-cart-arrow-down</v-icon>
                      <div>
                        <div class="text-caption">오늘 매수</div>
                        <div class="text-h6 font-weight-bold">
                          {{ dashboardStats.todayBuyCount }}건
                        </div>
                        <div class="text-caption">
                          {{ formatCurrency(dashboardStats.todayBuyAmount) }}
                        </div>
                      </div>
                    </div>
                  </v-card>
                </v-col>

                <v-col cols="6" sm="3">
                  <v-card color="warning" class="pa-3" dark>
                    <div class="d-flex align-center">
                      <v-icon class="mr-2">mdi-cart-arrow-up</v-icon>
                      <div>
                        <div class="text-caption">오늘 매도</div>
                        <div class="text-h6 font-weight-bold">
                          {{ dashboardStats.todaySellCount }}건
                        </div>
                        <div class="text-caption">
                          {{ formatCurrency(dashboardStats.todaySellAmount) }}
                        </div>
                      </div>
                    </div>
                  </v-card>
                </v-col>
              </v-row>

              <!-- 하단: 시스템 상태 + 빠른 액세스 -->
              <div class="bottom-cards-row mt-2">
                <!-- 시스템 상태 카드 -->
                <v-card class="bottom-card">
                  <v-card-title class="py-2">
                    <v-icon class="mr-2" size="small">mdi-server</v-icon>
                    시스템 상태
                  </v-card-title>
                  <v-card-text class="py-2">
                    <div class="mb-1">
                      <strong>상태:</strong>
                      <v-chip color="success" size="small">
                        <v-icon start size="small">mdi-check-circle</v-icon>
                        정상
                      </v-chip>
                    </div>
                    <div>
                      <strong>마지막 로그인:</strong> {{ lastLoginFormatted }}
                    </div>
                  </v-card-text>
                </v-card>

                <!-- 빠른 액세스 카드 -->
                <v-card class="bottom-card">
                  <v-card-title class="py-2">
                    <v-icon class="mr-2" size="small">mdi-lightning-bolt</v-icon>
                    빠른 액세스
                  </v-card-title>
                  <v-card-text class="py-2">
                    <v-row dense>
                      <v-col cols="6">
                        <v-btn
                          block
                          color="primary"
                          size="small"
                          @click="$router.push('/trading-settings')"
                        >
                          <v-icon start size="small">mdi-cog</v-icon>
                          거래 설정
                        </v-btn>
                      </v-col>
                      <v-col cols="6">
                        <v-btn
                          block
                          color="secondary"
                          size="small"
                          @click="$router.push('/profile')"
                        >
                          <v-icon start size="small">mdi-account-cog</v-icon>
                          프로필 설정
                        </v-btn>
                      </v-col>
                      <v-col cols="6">
                        <v-btn
                          block
                          color="teal"
                          size="small"
                          class="mt-1"
                          @click="$router.push('/backtest')"
                        >
                          <v-icon start size="small">mdi-chart-timeline-variant</v-icon>
                          백테스팅
                        </v-btn>
                      </v-col>
                      <v-col cols="6">
                        <v-btn
                          block
                          color="orange"
                          size="small"
                          class="mt-1"
                          @click="$router.push('/daily-report')"
                        >
                          <v-icon start size="small">mdi-file-chart</v-icon>
                          일일 리포트
                        </v-btn>
                      </v-col>
                    </v-row>
                  </v-card-text>
                </v-card>
              </div>
            </div>
          </v-col>
        </v-row>

        <!-- 활성 코인 목록 -->
        <v-row class="mt-4">
          <v-col cols="12">
            <v-card>
              <v-card-title>
                <v-icon class="mr-2">mdi-currency-btc</v-icon>
                활성 코인 목록
                <v-spacer />
                <v-btn
                  color="primary"
                  @click="loadCoins"
                  :loading="coinStore.loading"
                >
                  <v-icon start>mdi-refresh</v-icon>
                  새로고침
                </v-btn>
              </v-card-title>

              <v-card-text>
                <v-data-table
                  :headers="headers"
                  :items="sortedCoins"
                  :loading="coinStore.loading"
                  items-per-page="10"
                >
                  <template v-slot:item.marketCapRank="{ item }">
                    {{ item.marketCapRank || '-' }}
                  </template>
                  
                  <template v-slot:item.nameKr="{ item }">
                    <strong>{{ item.nameKr }}</strong>
                  </template>

                  <template v-slot:item.symbol="{ item }">
                    <v-chip size="small">{{ item.symbol }}</v-chip>
                  </template>

                  <template v-slot:item.isActive="{ item }">
                    <v-chip :color="item.isActive ? 'success' : 'error'" size="small">
                      {{ item.isActive ? '활성' : '비활성' }}
                    </v-chip>
                  </template>

                  <template v-slot:item.actions="{ item }">
                    <v-btn
                      size="small"
                      color="primary"
                      @click="viewCoinDetail(item)"
                    >
                      상세보기
                    </v-btn>
                  </template>
                </v-data-table>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- 코인 상세 정보 다이얼로그 -->
        <v-dialog v-model="coinDetailDialog" max-width="700">
          <v-card>
            <v-card-title class="bg-primary text-white">
              <v-icon class="mr-2">mdi-information</v-icon>
              {{ selectedCoin?.nameKr }} ({{ selectedCoin?.nameEn }})
            </v-card-title>
      
            <v-card-text class="pt-4">
              <v-row>
                <v-col cols="12" md="6">
                  <v-list density="compact">
                    <v-list-item>
                      <v-list-item-title>심볼</v-list-item-title>
                      <v-list-item-subtitle>
                        <v-chip color="primary" size="small">{{ selectedCoin?.symbol }}</v-chip>
                      </v-list-item-subtitle>
                    </v-list-item>
              
                    <v-list-item>
                      <v-list-item-title>시가총액 순위</v-list-item-title>
                      <v-list-item-subtitle>
                        {{ selectedCoin?.marketCapRank ? `#${selectedCoin.marketCapRank}` : '순위 정보 없음' }}
                      </v-list-item-subtitle>
                    </v-list-item>
              
                    <v-list-item>
                      <v-list-item-title>상태</v-list-item-title>
                      <v-list-item-subtitle>
                        <v-chip :color="selectedCoin?.isActive ? 'success' : 'error'" size="small">
                          {{ selectedCoin?.isActive ? '거래 가능' : '거래 불가' }}
                        </v-chip>
                      </v-list-item-subtitle>
                    </v-list-item>
              
                    <v-list-item v-if="selectedCoin?.lastUpdated">
                      <v-list-item-title>최근 업데이트</v-list-item-title>
                      <v-list-item-subtitle>
                        {{ formatDate(selectedCoin.lastUpdated) }}
                      </v-list-item-subtitle>
                    </v-list-item>
                  </v-list>
                </v-col>
          
                <v-col cols="12" md="6">
                  <v-card variant="outlined" class="pa-3">
                    <div class="text-subtitle-2 mb-2">
                      <v-icon size="small" class="mr-1">mdi-information-outline</v-icon>
                      코인 소개
                    </div>
                    <div class="text-body-2" v-if="coinDescription">
                      {{ coinDescription }}
                    </div>
                    <div class="text-body-2 text-grey" v-else>
                      {{ selectedCoin?.nameKr }}({{ selectedCoin?.nameEn }})은(는) 업비트 거래소에서 거래 가능한 암호화폐입니다.
                      <br><br>
                      심볼 {{ selectedCoin?.symbol }}로 거래되며, 
                      {{ selectedCoin?.marketCapRank ? `시가총액 기준 ${selectedCoin.marketCapRank}위에 위치해 있습니다.` : '시가총액 순위 정보는 현재 제공되지 않습니다.' }}
                    </div>
                  </v-card>
            
                  <v-btn
                    block
                    color="primary"
                    variant="outlined"
                    class="mt-3"
                    @click="goToTradingWithCoin(selectedCoin?.symbol)"
                  >
                    <v-icon start>mdi-chart-line</v-icon>
                    이 코인 거래 설정에 추가
                  </v-btn>
                </v-col>
              </v-row>
            </v-card-text>
      
            <v-card-actions>
              <v-spacer />
              <v-btn color="grey" @click="coinDetailDialog = false">닫기</v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useCoinStore } from '@/stores/coin'
import { transactionApi } from '@/api'
import type { DashboardStats } from '@/types'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'

const router = useRouter()
const authStore = useAuthStore()
const coinStore = useCoinStore()

// 사이드바 ref
const sidebarRef = ref()

// 대시보드 통계 데이터
const dashboardStats = ref<DashboardStats>({
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

// 코인 상세 다이얼로그 관련
const coinDetailDialog = ref(false)
const selectedCoin = ref<any>(null)
const coinDescription = ref('')

// 코인 설명 데이터 (주요 코인)
const coinDescriptions: Record<string, string> = {
  'KRW-BTC': '비트코인(Bitcoin)은 2009년 사토시 나카모토가 개발한 최초의 암호화폐입니다. 탈중앙화된 P2P 네트워크를 기반으로 하며, 블록체인 기술의 시초가 되었습니다. 디지털 금으로 불리며 가장 높은 시가총액을 보유하고 있습니다.',
  'KRW-ETH': '이더리움(Ethereum)은 비탈릭 부테린이 개발한 스마트 컨트랙트 플랫폼입니다. 탈중앙화 애플리케이션(DApp)을 개발할 수 있으며, DeFi와 NFT 생태계의 핵심 인프라입니다.',
  'KRW-XRP': '리플(Ripple)은 국제 송금에 특화된 암호화폐입니다. 빠른 거래 속도와 낮은 수수료가 특징이며, 금융 기관과의 파트너십을 통해 실제 결제 시스템에 활용되고 있습니다.',
  'KRW-SOL': '솔라나(Solana)는 고성능 블록체인 플랫폼으로, 초당 수천 건의 트랜잭션을 처리할 수 있습니다. 낮은 수수료와 빠른 속도로 DeFi, NFT 분야에서 주목받고 있습니다.',
  'KRW-DOGE': '도지코인(Dogecoin)은 2013년 밈(meme)에서 시작된 암호화폐입니다. 시바견을 마스코트로 하며, 커뮤니티 중심의 문화와 일론 머스크의 지지로 유명해졌습니다.',
  'KRW-ADA': '카르다노(Cardano)는 학술 연구 기반의 블록체인 플랫폼입니다. 지분 증명(PoS) 합의 알고리즘을 사용하며, 확장성과 지속가능성을 강조합니다.',
}

const headers = [
  { title: '순위', key: 'marketCapRank', align: 'center' as const },
  { title: '한글명', key: 'nameKr' },
  { title: '영문명', key: 'nameEn' },
  { title: '심볼', key: 'symbol' },
  { title: '상태', key: 'isActive', align: 'center' as const },
  { title: '액션', key: 'actions', align: 'center' as const, sortable: false }
]

// 순위 기준 정렬 (NULL은 마지막으로)
const sortedCoins = computed(() => {
  if (!coinStore.coins) return []
  
  return [...coinStore.coins].sort((a, b) => {
    // 둘 다 순위가 있으면 오름차순
    if (a.marketCapRank && b.marketCapRank) {
      return a.marketCapRank - b.marketCapRank
    }
    // a만 순위가 있으면 a가 먼저
    if (a.marketCapRank && !b.marketCapRank) {
      return -1
    }
    // b만 순위가 있으면 b가 먼저
    if (!a.marketCapRank && b.marketCapRank) {
      return 1
    }
    // 둘 다 순위가 없으면 이름순
    return (a.nameKr || '').localeCompare(b.nameKr || '')
  })
})

const lastLoginFormatted = computed(() => {
  if (!authStore.user?.lastLogin) return '정보 없음'
  return new Date(authStore.user.lastLogin).toLocaleString('ko-KR')
})

// 통화 포맷
const formatCurrency = (value: number): string => {
  if (value === null || value === undefined) return '0원'
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
    maximumFractionDigits: 0
  }).format(value)
}

// 날짜 포맷
const formatDate = (dateString: string): string => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('ko-KR')
}

const loadCoins = async () => {
  await coinStore.fetchActiveCoins()
}

// 대시보드 통계 로드
const loadDashboardStats = async () => {
  try {
    const response = await transactionApi.getStats()
    dashboardStats.value = response.data
  } catch (error) {
    console.error('대시보드 통계 로드 실패:', error)
  }
}

// 코인 상세 보기
const viewCoinDetail = (coin: any) => {
  selectedCoin.value = coin
  coinDescription.value = coinDescriptions[coin.symbol] || ''
  coinDetailDialog.value = true
}

// 거래 설정으로 이동 (해당 코인 포함)
const goToTradingWithCoin = (symbol: string | undefined) => {
  if (symbol) {
    router.push({
      path: '/trading-settings',
      query: { addCoin: symbol }
    })
  }
  coinDetailDialog.value = false
}

onMounted(() => {
  loadCoins()
  loadDashboardStats()
})
</script>

<style scoped>
/* 사용자 정보 카드와 우측 영역 높이 맞추기 */
.user-info-card {
  height: 100%;
  min-height: 280px;
}

.right-section {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 280px;
}

.stats-row {
  flex: 0 0 auto;
}

.bottom-cards-row {
  flex: 1;
  display: flex;
  gap: 16px;
}

.bottom-card {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.bottom-card .v-card-text {
  flex: 1;
}
</style>