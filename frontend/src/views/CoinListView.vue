<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main class="bg-grey-lighten-3">
      <v-container fluid>
        <v-row class="mb-4">
          <v-col>
            <h1 class="text-h4">
              <v-icon class="mr-2">mdi-currency-btc</v-icon>
              코인 목록
            </h1>
            <p class="text-subtitle-1 text-grey">거래 가능한 코인 목록을 확인하세요</p>
          </v-col>
          <v-col cols="auto">
            <v-btn color="indigo-darken-1" @click="fetchCoins" :loading="loading" variant="flat">
              <v-icon left>mdi-refresh</v-icon>
              새로고침
            </v-btn>
          </v-col>
        </v-row>

        <v-row class="mb-4">
          <v-col cols="12" md="6">
            <v-text-field v-model="search" prepend-inner-icon="mdi-magnify" label="코인 검색 (심볼 또는 이름)" clearable hide-details density="compact" variant="outlined" />
          </v-col>
          <v-col cols="12" md="3">
            <v-select v-model="filterActive" :items="activeOptions" label="상태 필터" hide-details density="compact" variant="outlined" />
          </v-col>
          <v-col cols="12" md="3">
            <v-select v-model="sortBy" :items="sortOptions" label="정렬" hide-details density="compact" variant="outlined" />
          </v-col>
        </v-row>

        <v-card elevation="2">
          <v-card-title class="d-flex align-center bg-indigo-darken-2 text-white py-3">
            <v-icon class="mr-2">mdi-format-list-bulleted</v-icon>
            활성 코인 목록
            <v-chip class="ml-2" color="white" variant="outlined" size="small">{{ filteredCoins.length }}개</v-chip>
            <v-spacer />
            <!-- ★★★ 수정: 시세 조회 진행 상황 표시 ★★★ -->
            <span v-if="priceLoading" class="text-caption">
              <v-progress-circular size="16" width="2" indeterminate class="mr-1" />
              시세 조회중... ({{ priceLoadedCount }}/{{ priceTargetCount }})
            </span>
          </v-card-title>

          <!-- ★★★ 수정: 페이지 변경 이벤트 추가 ★★★ -->
          <v-data-table 
            :headers="headers" 
            :items="filteredCoins" 
            :loading="loading" 
            :items-per-page="itemsPerPage"
            v-model:page="currentPage"
            @update:page="onPageChange"
            @update:items-per-page="onItemsPerPageChange"
            class="elevation-0"
          >
            <template v-slot:item.symbol="{ item }">
              <div class="d-flex align-center">
                <v-avatar size="32" class="mr-2" color="indigo-lighten-4">
                  <span class="text-caption font-weight-bold text-indigo-darken-2">{{ item.symbol.replace('KRW-', '').substring(0, 2) }}</span>
                </v-avatar>
                <div>
                  <div class="font-weight-medium">{{ item.symbol.replace('KRW-', '') }}</div>
                  <div class="text-caption text-grey-darken-1">{{ item.nameKr || item.symbol }}</div>
                </div>
              </div>
            </template>

            <template v-slot:item.marketCapRank="{ item }">
              <v-chip v-if="item.marketCapRank" size="small" variant="tonal" color="indigo">#{{ item.marketCapRank }}</v-chip>
              <span v-else class="text-grey-darken-1">-</span>
            </template>

            <template v-slot:item.currentPrice="{ item }">
              <span v-if="item.currentPrice" class="font-weight-medium">{{ formatCurrency(item.currentPrice) }}</span>
              <span v-else class="text-grey-darken-1">-</span>
            </template>

            <template v-slot:item.changeRate="{ item }">
              <v-chip v-if="item.changeRate !== undefined && item.changeRate !== null" :color="item.changeRate >= 0 ? 'teal-darken-1' : 'red-darken-1'" variant="flat" size="small">
                {{ item.changeRate >= 0 ? '+' : '' }}{{ item.changeRate?.toFixed(2) }}%
              </v-chip>
              <span v-else class="text-grey-darken-1">-</span>
            </template>

            <template v-slot:item.accTradePrice24h="{ item }">
              <span v-if="item.accTradePrice24h">{{ formatVolume(item.accTradePrice24h) }}</span>
              <span v-else class="text-grey-darken-1">-</span>
            </template>

            <template v-slot:item.isActive="{ item }">
              <v-chip :color="item.isActive ? 'teal-darken-1' : 'grey'" size="small" variant="flat">{{ item.isActive ? '활성' : '비활성' }}</v-chip>
            </template>

            <template v-slot:item.actions="{ item }">
              <v-btn size="small" color="indigo-darken-1" variant="tonal" @click="viewCoinDetail(item)">상세정보</v-btn>
            </template>
          </v-data-table>
        </v-card>

        <!-- 코인 상세 다이얼로그 -->
        <v-dialog v-model="coinDetailDialog" max-width="600">
          <v-card>
            <v-card-title class="d-flex align-center bg-indigo-darken-2 text-white py-3">
              <v-icon class="mr-2">mdi-information-outline</v-icon>
              {{ selectedCoin?.nameKr || selectedCoin?.symbol }} 상세 정보
              <v-spacer />
              <v-btn icon variant="text" @click="coinDetailDialog = false" size="small"><v-icon color="white">mdi-close</v-icon></v-btn>
            </v-card-title>

            <v-card-text class="pa-4">
              <v-row v-if="selectedCoin">
                <v-col cols="12" md="6">
                  <v-card variant="outlined" class="pa-3 fill-height">
                    <div class="text-overline text-indigo-darken-2 mb-2">기본 정보</div>
                    <div class="mb-2"><span class="text-caption text-grey-darken-1">심볼</span><div class="font-weight-medium text-grey-darken-4">{{ selectedCoin.symbol }}</div></div>
                    <div class="mb-2"><span class="text-caption text-grey-darken-1">한글명</span><div class="text-grey-darken-4">{{ selectedCoin.nameKr || '-' }}</div></div>
                    <div class="mb-2"><span class="text-caption text-grey-darken-1">영문명</span><div class="text-grey-darken-4">{{ selectedCoin.nameEn || '-' }}</div></div>
                    <div><span class="text-caption text-grey-darken-1">시가총액 순위</span><div><v-chip v-if="selectedCoin.marketCapRank" size="small" color="indigo" variant="tonal">#{{ selectedCoin.marketCapRank }}</v-chip><span v-else class="text-grey-darken-2">정보 없음</span></div></div>
                  </v-card>
                </v-col>
                <v-col cols="12" md="6">
                  <v-card variant="outlined" class="pa-3 fill-height">
                    <div class="text-overline text-indigo-darken-2 mb-2">실시간 시세</div>
                    <div class="mb-2">
                      <span class="text-caption text-grey-darken-1">현재가</span>
                      <div class="text-h6 font-weight-bold text-grey-darken-4">
                        <span v-if="detailLoading"><v-progress-circular size="20" width="2" indeterminate /></span>
                        <span v-else-if="detailPrice.currentPrice">{{ formatCurrency(detailPrice.currentPrice) }}</span>
                        <span v-else class="text-grey">-</span>
                      </div>
                    </div>
                    <div class="mb-2">
                      <span class="text-caption text-grey-darken-1">24시간 변동률</span>
                      <div>
                        <v-chip v-if="detailPrice.changeRate !== null" :color="detailPrice.changeRate >= 0 ? 'teal-darken-1' : 'red-darken-1'" size="small" variant="flat">{{ detailPrice.changeRate >= 0 ? '+' : '' }}{{ detailPrice.changeRate?.toFixed(2) }}%</v-chip>
                        <span v-else class="text-grey-darken-2">-</span>
                      </div>
                    </div>
                    <div>
                      <span class="text-caption text-grey-darken-1">24시간 거래대금</span>
                      <div class="text-grey-darken-4"><span v-if="detailPrice.accTradePrice24h">{{ formatVolume(detailPrice.accTradePrice24h) }}</span><span v-else class="text-grey-darken-2">-</span></div>
                    </div>
                  </v-card>
                </v-col>
                <v-col cols="12">
                  <v-card variant="outlined" class="pa-3">
                    <div class="text-overline text-indigo-darken-2 mb-2"><v-icon size="small" class="mr-1">mdi-text-box-outline</v-icon>코인 소개</div>
                    <div class="text-body-2 text-grey-darken-4" style="line-height: 1.7;">{{ getCoinDescription(selectedCoin.symbol) }}</div>
                  </v-card>
                </v-col>
              </v-row>
            </v-card-text>

            <v-card-actions class="pa-4 pt-0">
              <v-btn color="indigo-darken-1" variant="flat" @click="goToTradingWithCoin(selectedCoin?.symbol)"><v-icon start>mdi-cog</v-icon>거래 설정에 추가</v-btn>
              <v-spacer />
              <v-btn color="grey" variant="text" @click="coinDetailDialog = false">닫기</v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>

        <v-snackbar v-model="snackbar.show" :color="snackbar.color" :timeout="3000">{{ snackbar.message }}</v-snackbar>
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'
import { coinApi } from '@/api'

const router = useRouter()
const sidebarRef = ref()

const loading = ref(false)
const priceLoading = ref(false)
const priceLoadedCount = ref(0)
const priceTargetCount = ref(0)
const coins = ref<any[]>([])
const search = ref('')
const filterActive = ref('all')
const sortBy = ref('rank')

const coinDetailDialog = ref(false)
const selectedCoin = ref<any>(null)
const detailLoading = ref(false)
const detailPrice = ref({ currentPrice: null as number | null, changeRate: null as number | null, accTradePrice24h: null as number | null })

const coinDescriptions: Record<string, string> = {
  'KRW-BTC': '비트코인(Bitcoin)은 2009년 사토시 나카모토가 개발한 최초의 암호화폐입니다. 탈중앙화된 P2P 네트워크를 기반으로 하며, 블록체인 기술의 시초가 되었습니다.',
  'KRW-ETH': '이더리움(Ethereum)은 비탈릭 부테린이 개발한 스마트 컨트랙트 플랫폼입니다. DeFi와 NFT 생태계의 핵심 인프라입니다.',
  'KRW-XRP': '리플(Ripple)은 국제 송금에 특화된 암호화폐입니다. 빠른 거래 속도와 낮은 수수료가 특징입니다.',
  'KRW-SOL': '솔라나(Solana)는 고성능 블록체인 플랫폼으로, 초당 수천 건의 트랜잭션을 처리할 수 있습니다.',
  'KRW-DOGE': '도지코인(Dogecoin)은 2013년 밈(meme)에서 시작된 암호화폐입니다.',
  'KRW-ADA': '카르다노(Cardano)는 학술 연구 기반의 블록체인 플랫폼입니다.',
  'KRW-AVAX': '아발란체(Avalanche)는 높은 처리량과 빠른 완결성을 제공하는 스마트 컨트랙트 플랫폼입니다.',
  'KRW-DOT': '폴카닷(Polkadot)은 서로 다른 블록체인을 연결하는 멀티체인 프로토콜입니다.',
  'KRW-MATIC': '폴리곤(Polygon)은 이더리움의 확장성 문제를 해결하는 레이어2 솔루션입니다.',
  'KRW-LINK': '체인링크(Chainlink)는 블록체인과 외부 데이터를 연결하는 탈중앙화 오라클 네트워크입니다.',
  'KRW-TRX': '트론(TRON)은 콘텐츠 크리에이터를 위한 탈중앙화 플랫폼입니다.',
  'KRW-ATOM': '코스모스(Cosmos)는 블록체인 인터넷을 목표로 하는 프로젝트입니다.',
  'KRW-UNI': '유니스왑(Uniswap)은 이더리움 기반의 탈중앙화 거래소(DEX)입니다.',
  'KRW-LTC': '라이트코인(Litecoin)은 비트코인을 기반으로 한 암호화폐로, 더 빠른 거래 확인 시간을 제공합니다.',
  'KRW-BCH': '비트코인 캐시(Bitcoin Cash)는 2017년 비트코인에서 하드포크된 암호화폐입니다.',
  'KRW-ETC': '이더리움 클래식(Ethereum Classic)은 원래 이더리움 체인을 유지한 블록체인입니다.',
  'KRW-NEAR': '니어 프로토콜(NEAR Protocol)은 샤딩 기술을 사용하는 고성능 블록체인입니다.',
  'KRW-APT': '앱토스(Aptos)는 Meta 출신 개발자들이 만든 레이어1 블록체인입니다.',
  'KRW-ARB': '아비트럼(Arbitrum)은 이더리움의 레이어2 솔루션입니다.',
  'KRW-SHIB': '시바이누(Shiba Inu)는 도지코인에서 영감을 받은 밈 코인입니다.',
  'KRW-XLM': '스텔라 루멘(Stellar Lumens)은 국경 간 결제에 특화된 블록체인입니다.',
  'KRW-1INCH': '1인치(1inch)는 탈중앙화 거래소 애그리게이터로, 최적의 스왑 경로를 찾아줍니다.',
  'KRW-AAVE': '에이브(Aave)는 탈중앙화 대출 프로토콜입니다.',
  'KRW-SAND': '샌드박스(Sandbox)는 블록체인 기반 메타버스 플랫폼입니다.',
  'KRW-MANA': '디센트럴랜드(Decentraland)는 가상 부동산 메타버스 플랫폼입니다.',
  'KRW-AXS': '엑시인피니티(Axie Infinity)는 블록체인 기반 게임 플랫폼입니다.',
  'KRW-FLOW': '플로우(Flow)는 NBA Top Shot 등 NFT에 특화된 블록체인입니다.',
  'KRW-ENJ': '엔진코인(Enjin)은 게임 아이템 NFT 플랫폼입니다.',
  'KRW-CHZ': '칠리즈(Chiliz)는 스포츠 팬 토큰 플랫폼입니다.',
  'KRW-HBAR': '헤데라(Hedera)는 해시그래프 기술 기반의 분산 원장입니다.',
}

const snackbar = ref({ show: false, message: '', color: 'success' })
const activeOptions = [{ title: '전체', value: 'all' }, { title: '활성만', value: 'active' }, { title: '비활성만', value: 'inactive' }]
const sortOptions = [{ title: '시총순위', value: 'rank' }, { title: '심볼순', value: 'symbol' }, { title: '거래대금순', value: 'volume' }, { title: '변동률순', value: 'change' }]

// ★★★ 추가: 페이지네이션 변수 ★★★
const currentPage = ref(1)
const itemsPerPage = ref(20)

const headers = [
  { title: '코인', key: 'symbol', sortable: true },
  { title: '시총순위', key: 'marketCapRank', sortable: true, align: 'center' },
  { title: '현재가', key: 'currentPrice', sortable: true, align: 'end' },
  { title: '변동률(24h)', key: 'changeRate', sortable: true, align: 'end' },
  { title: '거래대금(24h)', key: 'accTradePrice24h', sortable: true, align: 'end' },
  { title: '상태', key: 'isActive', sortable: true, align: 'center' },
  { title: '상세정보', key: 'actions', sortable: false, align: 'center' }
]

const filteredCoins = computed(() => {
  let result = [...coins.value]
  if (search.value) {
    const sl = search.value.toLowerCase()
    result = result.filter(c => c.symbol.toLowerCase().includes(sl) || (c.nameKr && c.nameKr.toLowerCase().includes(sl)) || (c.nameEn && c.nameEn.toLowerCase().includes(sl)))
  }
  if (filterActive.value === 'active') result = result.filter(c => c.isActive)
  else if (filterActive.value === 'inactive') result = result.filter(c => !c.isActive)
  
  switch (sortBy.value) {
    case 'rank': result.sort((a, b) => { if (a.marketCapRank && b.marketCapRank) return a.marketCapRank - b.marketCapRank; if (a.marketCapRank && !b.marketCapRank) return -1; if (!a.marketCapRank && b.marketCapRank) return 1; return (a.nameKr || a.symbol).localeCompare(b.nameKr || b.symbol) }); break
    case 'volume': result.sort((a, b) => (b.accTradePrice24h || 0) - (a.accTradePrice24h || 0)); break
    case 'change': result.sort((a, b) => (b.changeRate ?? -999) - (a.changeRate ?? -999)); break
    default: result.sort((a, b) => a.symbol.localeCompare(b.symbol))
  }
  return result
})

const formatCurrency = (value: number) => {
  if (!value && value !== 0) return '-'
  if (value >= 1000000) return '₩' + value.toLocaleString('ko-KR', { maximumFractionDigits: 0 })
  if (value >= 100) return '₩' + value.toLocaleString('ko-KR', { maximumFractionDigits: 2 })
  return '₩' + value.toLocaleString('ko-KR', { maximumFractionDigits: 4 })
}
const formatVolume = (value: number) => {
  if (!value) return '-'
  if (value >= 1000000000000) return (value / 1000000000000).toFixed(1) + '조'
  if (value >= 100000000) return (value / 100000000).toFixed(1) + '억'
  if (value >= 10000) return (value / 10000).toFixed(1) + '만'
  return '₩' + value.toLocaleString('ko-KR')
}
const getCoinDescription = (symbol: string) => {
  const desc = coinDescriptions[symbol]
  if (desc) return desc
  const coinName = selectedCoin.value?.nameKr || symbol.replace('KRW-', '')
  return `${coinName}은(는) 업비트 거래소에서 거래 가능한 암호화폐입니다.`
}

// ★★★ 수정: 현재 페이지 코인만 조회 ★★★
const fetchCoins = async () => {
  loading.value = true
  try {
    const response = await coinApi.getActiveCoins()
    const coinList = response.data || []
    
    coins.value = coinList.map((coin: any) => ({
      ...coin,
      currentPrice: null,
      changeRate: null,
      accTradePrice24h: null
    }))

    showSnackbar('코인 목록을 불러왔습니다.', 'success')

    // ★★★ 핵심 수정: 현재 페이지에 표시되는 코인만 조회 ★★★
    await fetchCurrentPagePrices()

  } catch (error) {
    console.error('코인 목록 조회 실패:', error)
    showSnackbar('코인 목록 조회에 실패했습니다.', 'error')
  } finally {
    loading.value = false
  }
}

// ★★★ 수정: 벌크 API로 현재 페이지 코인 가격 조회 (429 오류 해결) ★★★
const fetchCurrentPagePrices = async () => {
  // 현재 정렬된 목록에서 현재 페이지에 해당하는 코인만 추출
  const startIndex = (currentPage.value - 1) * itemsPerPage.value
  const endIndex = startIndex + itemsPerPage.value
  const currentPageCoins = filteredCoins.value.slice(startIndex, endIndex)
  
  // 이미 가격 정보가 있는 코인은 제외
  const coinsToFetch = currentPageCoins.filter(c => c.currentPrice === null)
  
  if (coinsToFetch.length === 0) return
  
  priceTargetCount.value = coinsToFetch.length
  priceLoadedCount.value = 0
  priceLoading.value = true

  try {
    // ★★★ 핵심 변경: 벌크 API 한 번 호출로 모든 코인 가격 조회 ★★★
    const symbols = coinsToFetch.map(c => c.symbol)
    const response = await coinApi.getMultiplePrices(symbols)
    const tickers = response.data || []
    
    // 응답된 ticker 데이터를 각 코인에 매핑
    for (const ticker of tickers) {
      const market = ticker.market || ticker.symbol
      const originalCoin = coins.value.find(c => c.symbol === market)
      if (originalCoin) {
        originalCoin.currentPrice = ticker.tradePrice ?? ticker.trade_price ?? null
        originalCoin.changeRate = (ticker.signedChangeRate ?? ticker.signed_change_rate) !== undefined
          ? (ticker.signedChangeRate ?? ticker.signed_change_rate) * 100 : null
        originalCoin.accTradePrice24h = ticker.accTradePrice24h ?? ticker.acc_trade_price_24h ?? null
      }
      priceLoadedCount.value++
    }
  } catch (e) {
    console.error('벌크 가격 조회 실패:', e)
  }
  
  priceLoading.value = false
}

// ★★★ 신규: 페이지 변경 핸들러 ★★★
const onPageChange = async (page: number) => {
  currentPage.value = page
  await fetchCurrentPagePrices()
}

// ★★★ 수정: 페이지당 항목 수 변경 핸들러 - 추가 정보 로드 ★★★
const onItemsPerPageChange = async (items: number) => {
  itemsPerPage.value = items
  // 항목 수 증가로 인해 새로 표시되는 코인들의 가격 정보 로드
  await fetchCurrentPagePrices()
}

// ★★★ 신규: 정렬/필터 변경 감지하여 가격 재조회 ★★★
watch([sortBy, filterActive, search], async () => {
  // 정렬/필터 변경 시 현재 페이지 코인 가격 조회
  await fetchCurrentPagePrices()
}, { flush: 'post' })

const viewCoinDetail = async (coin: any) => {
  selectedCoin.value = coin
  coinDetailDialog.value = true
  detailPrice.value = { currentPrice: coin.currentPrice || null, changeRate: coin.changeRate ?? null, accTradePrice24h: coin.accTradePrice24h || null }
  
  if (!coin.currentPrice) {
    detailLoading.value = true
    try {
      const response = await coinApi.getCoinPrice(coin.symbol)
      const ticker = response.data
      if (ticker) {
        detailPrice.value = {
          currentPrice: ticker.tradePrice ?? ticker.trade_price ?? null,
          changeRate: (ticker.signedChangeRate ?? ticker.signed_change_rate) !== undefined ? (ticker.signedChangeRate ?? ticker.signed_change_rate) * 100 : null,
          accTradePrice24h: ticker.accTradePrice24h ?? ticker.acc_trade_price_24h ?? null
        }
        // 원본 데이터도 업데이트
        coin.currentPrice = detailPrice.value.currentPrice
        coin.changeRate = detailPrice.value.changeRate
        coin.accTradePrice24h = detailPrice.value.accTradePrice24h
      }
    } catch (e) { console.error('개별 가격 조회 실패:', e) }
    finally { detailLoading.value = false }
  }
}

const goToTradingWithCoin = (symbol: string | undefined) => {
  if (symbol) router.push({ path: '/trading-settings', query: { addCoin: symbol } })
  coinDetailDialog.value = false
}
const showSnackbar = (message: string, color: string) => { snackbar.value = { show: true, message, color } }

onMounted(() => { fetchCoins() })
</script>

<style scoped>
.v-data-table { font-size: 0.9rem; }
.fill-height { height: 100%; }
</style>