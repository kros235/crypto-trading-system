<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main class="bg-grey-lighten-3">
      <v-container fluid>
          <v-row>
            <v-col cols="12">
              <div class="d-flex align-center">
                <h1 class="text-h4">📰 코인 뉴스</h1>
                <HelpButton
                  :use-dialog="true"
                  :dialog-title="helpContents.newsOverview.title"
                  :dialog-content="helpContents.newsOverview.content"
                  color="grey-darken-1"
                  class="ml-2"
                />
              </div>
              <p class="text-subtitle-1 text-grey mb-4">AI가 분석한 코인별 최신 뉴스를 확인하세요</p>
            </v-col>
          </v-row>

        <v-row class="mb-4" align="center">
          <v-col cols="12" md="2">
            <v-select
              v-model="selectedCoin"
              :items="coinOptions"
              label="코인 필터"
              clearable
              density="comfortable"
              hide-details
              variant="outlined"
              bg-color="white"
              @update:model-value="loadNews"
            />
          </v-col>
          <v-col cols="12" md="4">
            <v-text-field
              v-model="searchKeyword"
              label="키워드 검색"
              prepend-inner-icon="mdi-magnify"
              clearable
              density="comfortable"
              hide-details
              variant="outlined"
              bg-color="white"
              @keyup.enter="loadNews"
              @click:clear="clearSearch"
            />
          </v-col>
          <v-col cols="12" md="2">
            <v-select
              v-model="pageSize"
              :items="[10, 20, 50]"
              label="페이지당 건수"
              density="comfortable"
              hide-details
              variant="outlined"
              bg-color="white"
              @update:model-value="loadNews"
            />
          </v-col>
          <v-col cols="12" md="4" class="d-flex justify-end">
            <v-btn 
              color="primary" 
              @click="loadNews" 
              class="mr-2"
              height="48"
            >
              <v-icon start>mdi-refresh</v-icon>
              새로고침
            </v-btn>
            <v-btn 
              color="teal-darken-1" 
              @click="collectNews" 
              :loading="collecting"
              height="48"
            >
              <v-icon start>mdi-download</v-icon>
              뉴스 수집
            </v-btn>
          </v-col>
        </v-row>



        <!-- 뉴스 목록 -->
        <v-row>
          <v-col cols="12">
            <v-card>
              <v-card-title class="d-flex align-center">
                <v-icon class="mr-2">mdi-format-list-bulleted</v-icon>
                뉴스 목록
                <v-chip size="small" color="primary" class="ml-2">{{ totalElements }}건</v-chip>
                <v-spacer />
                <HelpButton
                  :use-dialog="true"
                  :dialog-title="helpContents.aiAnalysis.title"
                  :dialog-content="helpContents.aiAnalysis.content"
                  color="grey-darken-1"
                />
              </v-card-title>
              <v-data-table
                :headers="headers"
                :items="newsList"
                :loading="loading"
                :items-per-page="pageSize"
                hide-default-footer
                class="elevation-1"
                @click:row="openNewsDetail"
              >
                <!-- 코인 심볼 -->
                <template #item.coinSymbol="{ item }">
                  <v-chip size="small" :color="getCoinColor(item.coinSymbol)">
                    {{ formatCoinSymbol(item.coinSymbol) }}
                  </v-chip>
                </template>

                <!-- 제목 -->
                <template #item.title="{ item }">
                  <div class="text-truncate" style="max-width: 400px;">
                    {{ item.title }}
                  </div>
                </template>

                <!-- 출처 -->
                <template #item.source="{ item }">
                  <v-chip size="x-small" variant="outlined">
                    {{ item.source }}
                  </v-chip>
                </template>

                <!-- 발행일 -->
                <template #item.publishedAt="{ item }">
                  {{ formatDate(item.publishedAt) }}
                </template>

                <!-- AI 분석 -->
                <template #item.analyzed="{ item }">
                  <v-chip 
                    size="small" 
                    :color="item.analyzed ? 'success' : 'grey'"
                  >
                    {{ item.analyzed ? '분석완료' : '미분석' }}
                  </v-chip>
                  <span v-if="item.sentimentScore" class="ml-2">
                    ({{ item.sentimentScore > 0 ? '+' : '' }}{{ item.sentimentScore }})
                  </span>
                </template>

                <!-- 링크 -->
                <template #item.actions="{ item }">
                  <v-btn
                    icon
                    size="small"
                    variant="text"
                    :href="item.sourceUrl"
                    target="_blank"
                    @click.stop
                  >
                    <v-icon>mdi-open-in-new</v-icon>
                  </v-btn>
                </template>
              </v-data-table>

              <!-- 페이지네이션 -->
              <v-pagination
                v-model="currentPage"
                :length="totalPages"
                :total-visible="7"
                class="my-4"
                @update:model-value="loadNews"
              />
            </v-card>
          </v-col>
        </v-row>

        <!-- 뉴스 상세 다이얼로그 -->
        <v-dialog v-model="detailDialog" max-width="700">
          <v-card v-if="selectedNews">
            <v-card-title class="d-flex align-center">
              <v-chip size="small" :color="getCoinColor(selectedNews.coinSymbol)" class="mr-2">
                {{ formatCoinSymbol(selectedNews.coinSymbol) }}
              </v-chip>
              <span>뉴스 상세</span>
              <v-spacer />
              <v-btn icon variant="text" @click="detailDialog = false">
                <v-icon>mdi-close</v-icon>
              </v-btn>
            </v-card-title>
            <v-divider />
            <v-card-text>
              <h3 class="text-h6 mb-3">{{ selectedNews.title }}</h3>
              <div class="text-body-2 text-grey mb-3">
                <v-icon size="small" class="mr-1">mdi-newspaper</v-icon>
                {{ selectedNews.source }}
                <span class="mx-2">|</span>
                <v-icon size="small" class="mr-1">mdi-clock-outline</v-icon>
                {{ formatDate(selectedNews.publishedAt) }}
              </div>
              <v-divider class="my-3" />
              <p class="text-body-1" style="white-space: pre-wrap;">
                {{ selectedNews.summary || '요약 내용이 없습니다.' }}
              </p>
              <div v-if="selectedNews.analyzed" class="mt-4">
                <v-chip :color="getSentimentColor(selectedNews.sentimentScore)">
                  AI 분석: {{ getSentimentLabel(selectedNews.sentimentScore) }}
                  ({{ selectedNews.sentimentScore > 0 ? '+' : '' }}{{ selectedNews.sentimentScore }})
                </v-chip>
              </div>
            </v-card-text>
            <v-card-actions>
              <v-spacer />
              <v-btn 
                color="primary" 
                :href="selectedNews.sourceUrl" 
                target="_blank"
              >
                <v-icon start>mdi-open-in-new</v-icon>
                원문 보기
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>

        <!-- 스낵바 -->
        <v-snackbar v-model="snackbar.show" :color="snackbar.color" :timeout="3000">
          {{ snackbar.message }}
        </v-snackbar>
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/api'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'
import HelpButton from '@/components/HelpButton.vue'

// 사이드바 ref
const sidebarRef = ref()

const helpContents = {
  newsOverview: {
    title: '📰 코인 뉴스 안내',
    content: `
      <p class="help-intro">AI가 분석한 코인별 최신 뉴스를 확인할 수 있습니다.</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>뉴스 소스</strong>
        <span class="help-desc">CoinTelegraph, Bitcoin Magazine, Decrypt 등 글로벌 공신력 있는 매체에서 수집됩니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>수집 주기</strong>
        <span class="help-desc">3시간마다 자동 수집됩니다. (0시, 3시, 6시, 9시, 12시, 15시, 18시, 21시)</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>AI 분석</strong>
        <span class="help-desc">Groq AI가 뉴스를 분석하여 호재/악재/중립을 판단하고 점수를 부여합니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>가중치 적용</strong>
        <span class="help-desc">AI 분석 결과는 거래 설정의 매수 기준가에 ±0.5% 범위로 자동 반영됩니다.</span></p>
      <p class="help-note">💡 <strong>Tip:</strong> 뉴스 행을 클릭하면 상세 내용을 확인할 수 있습니다.</p>
    `
  },
  aiAnalysis: {
  title: '🤖 AI 분석 점수 안내',
  content: `
    <p class="help-intro">AI가 뉴스를 분석하여 시장에 미치는 영향을 <strong>-1.0 ~ +1.0</strong> 범위의 점수로 나타냅니다.</p>
    <p class="help-item"><span class="help-bullet">•</span> <strong>+0.2 이상 (호재)</strong>
      <span class="help-desc">시장에 긍정적인 영향을 미칠 것으로 예상되는 뉴스입니다.<br/>예: +0.8, +0.5 등 → 매수 조건이 완화됩니다.</span></p>
    <p class="help-item"><span class="help-bullet">•</span> <strong>-0.2 ~ +0.2 (중립)</strong>
      <span class="help-desc">시장에 큰 영향이 없을 것으로 예상됩니다.<br/>예: +0.1, -0.1 등 → 매수 조건에 변화가 없습니다.</span></p>
    <p class="help-item"><span class="help-bullet">•</span> <strong>-0.2 이하 (악재)</strong>
      <span class="help-desc">시장에 부정적인 영향을 미칠 것으로 예상되는 뉴스입니다.<br/>예: -0.5, -0.9 등 → 매수 조건이 강화됩니다.</span></p>
    <p class="help-item"><span class="help-bullet">•</span> <strong>가중치 계산</strong>
      <span class="help-desc">평균 점수 × 0.5 = 가중치(%)<br/>예: 평균 +0.8 → 가중치 +0.4% → 매수 기준가 완화</span></p>
    <p class="help-note">💡 <strong>Tip:</strong> AI 분석은 참고용이며, 실제 시장 상황과 다를 수 있습니다.<br/><span style="margin-left: 47px;">뉴스 원문을 직접 확인하는 것을 권장합니다.</span></p>
  `
  }
}

// 상태
const loading = ref(false)
const collecting = ref(false)
const newsList = ref<any[]>([])
const selectedCoin = ref<string | null>(null)
const searchKeyword = ref('')
const pageSize = ref(10)
const currentPage = ref(1)
const totalPages = ref(1)
const totalElements = ref(0)

// 다이얼로그
const detailDialog = ref(false)
const selectedNews = ref<any>(null)

// 스낵바
const snackbar = ref({
  show: false,
  message: '',
  color: 'success'
})

// 테이블 헤더
const headers = [
  { title: '코인', key: 'coinSymbol', width: '100px' },
  { title: '제목', key: 'title' },
  { title: '출처', key: 'source', width: '120px' },
  { title: '발행일', key: 'publishedAt', width: '150px' },
  { title: 'AI분석', key: 'analyzed', width: '130px' },
  { title: '', key: 'actions', width: '60px', sortable: false }
]

// 코인 옵션
const coinOptions = [
  { title: '전체', value: null },
  { title: 'BTC (비트코인)', value: 'KRW-BTC' },
  { title: 'ETH (이더리움)', value: 'KRW-ETH' },
  { title: 'XRP (리플)', value: 'KRW-XRP' },
  { title: 'SOL (솔라나)', value: 'KRW-SOL' },
  { title: 'DOGE (도지코인)', value: 'KRW-DOGE' },
  { title: 'ADA (에이다)', value: 'KRW-ADA' },
  { title: 'AVAX (아발란체)', value: 'KRW-AVAX' },
  { title: 'DOT (폴카닷)', value: 'KRW-DOT' },
  { title: 'MATIC (폴리곤)', value: 'KRW-MATIC' },
  { title: 'LINK (체인링크)', value: 'KRW-LINK' }
]

// 뉴스 목록 조회
const loadNews = async () => {
  loading.value = true
  try {
    const params: any = {
      page: currentPage.value - 1,
      size: pageSize.value,
      sortBy: 'publishedAt',
      sortDir: 'desc'
    }
    
    if (selectedCoin.value) {
      params.coinSymbol = selectedCoin.value
    }
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    
    const response = await api.get('/news/list', { params })
    
    if (response.data.success) {
      newsList.value = response.data.data.content || []
      totalPages.value = response.data.data.totalPages || 1
      totalElements.value = response.data.data.totalElements || 0
    }
  } catch (error) {
    console.error('뉴스 조회 실패:', error)
    showSnackbar('뉴스 조회에 실패했습니다.', 'error')
  } finally {
    loading.value = false
  }
}

// 뉴스 수집 (관리자)
const collectNews = async () => {
  collecting.value = true
  try {
    const response = await api.post('/news/collect', {}, { timeout: 60000 })
    if (response.data.success) {
      const count = response.data.data?.length || 0
      if (count > 0) {
        showSnackbar(`${count}건의 새로운 뉴스가 수집되었습니다.`, 'success')
      } else {
        showSnackbar('새로운 뉴스가 없습니다.', 'info')
      }
      loadNews()
    }
  } catch (error) {
    console.error('뉴스 수집 실패:', error)
    showSnackbar('뉴스 수집에 실패했습니다.', 'error')
  } finally {
    collecting.value = false
  }
}

// 뉴스 상세 보기
const openNewsDetail = (event: Event, { item }: any) => {
  selectedNews.value = item
  detailDialog.value = true
}

// 검색 초기화
const clearSearch = () => {
  searchKeyword.value = ''
  loadNews()
}

// 유틸 함수들
const formatCoinSymbol = (symbol: string) => {
  return symbol?.replace('KRW-', '') || symbol
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getCoinColor = (symbol: string) => {
  const colors: Record<string, string> = {
    'KRW-BTC': 'orange',
    'KRW-ETH': 'indigo',
    'KRW-XRP': 'blue',
    'KRW-SOL': 'purple',
    'KRW-DOGE': 'amber',
    'KRW-ADA': 'cyan',
    'KRW-AVAX': 'red',
    'KRW-DOT': 'pink',
    'KRW-MATIC': 'deep-purple',
    'KRW-LINK': 'blue-grey'
  }
  return colors[symbol] || 'grey'
}

const getSentimentColor = (score: number | null) => {
  if (score === null) return 'grey'
  if (score >= 50) return 'success'
  if (score <= -50) return 'error'
  return 'warning'
}

const getSentimentLabel = (score: number | null) => {
  if (score === null) return '미분석'
  if (score >= 50) return '호재'
  if (score <= -50) return '악재'
  return '중립'
}

const showSnackbar = (message: string, color: string) => {
  snackbar.value = { show: true, message, color }
}

// 초기 로드
onMounted(() => {
  loadNews()
})
</script>

<style scoped>
.v-data-table :deep(tr) {
  cursor: pointer;
}
.v-data-table :deep(tr:hover) {
  background-color: rgba(var(--v-theme-primary), 0.05);
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