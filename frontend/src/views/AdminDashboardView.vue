<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />
    <v-main>
      <v-container fluid>
    <!-- Page Title -->
    <v-row class="mb-4">
      <v-col>
        <h1 class="text-h4">🔧 관리자 대시보드</h1>
        <p class="text-subtitle-1 text-grey">시스템 현황 및 사용자 관리</p>
      </v-col>
      <v-col cols="auto">
        <v-btn color="primary" @click="refreshData" :loading="loading">
          <v-icon left>mdi-refresh</v-icon>
          새로고침
        </v-btn>
      </v-col>
    </v-row>

    <!-- System Stats Cards -->
    <v-row class="mb-4">
      <!-- 사용자 통계 -->
      <v-col cols="12" md="3">
        <v-card class="pa-4 stats-card" elevation="2">
          <div class="d-flex align-center">
            <v-avatar color="primary" size="48" class="mr-4">
              <v-icon color="white">mdi-account-group</v-icon>
            </v-avatar>
            <div>
              <p class="text-caption text-grey mb-1">전체 사용자</p>
              <p class="text-h5 font-weight-bold mb-0">{{ stats.totalUsers }}</p>
              <p class="text-caption text-success">활성: {{ stats.activeUsers }}</p>
            </div>
          </div>
        </v-card>
      </v-col>

      <!-- 오늘 거래 -->
      <v-col cols="12" md="3">
        <v-card class="pa-4 stats-card" elevation="2">
          <div class="d-flex align-center">
            <v-avatar color="success" size="48" class="mr-4">
              <v-icon color="white">mdi-swap-horizontal</v-icon>
            </v-avatar>
            <div>
              <p class="text-caption text-grey mb-1">오늘 거래</p>
              <p class="text-h5 font-weight-bold mb-0">
                {{ stats.todayBuyCount + stats.todaySellCount }}건
              </p>
              <p class="text-caption">
                <span class="text-primary">매수 {{ stats.todayBuyCount }}</span> /
                <span class="text-error">매도 {{ stats.todaySellCount }}</span>
              </p>
            </div>
          </div>
        </v-card>
      </v-col>

      <!-- 총 거래액 -->
      <v-col cols="12" md="3">
        <v-card class="pa-4 stats-card" elevation="2">
          <div class="d-flex align-center">
            <v-avatar color="warning" size="48" class="mr-4">
              <v-icon color="white">mdi-currency-krw</v-icon>
            </v-avatar>
            <div>
              <p class="text-caption text-grey mb-1">오늘 거래액</p>
              <p class="text-h5 font-weight-bold mb-0">
                {{ formatKRW(stats.todayTotalVolume) }}
              </p>
            </div>
          </div>
        </v-card>
      </v-col>

      <!-- 시스템 상태 -->
      <v-col cols="12" md="3">
        <v-card class="pa-4 stats-card" elevation="2">
          <div class="d-flex align-center">
            <v-avatar :color="stats.botRunning ? 'success' : 'grey'" size="48" class="mr-4">
              <v-icon color="white">mdi-robot</v-icon>
            </v-avatar>
            <div>
              <p class="text-caption text-grey mb-1">봇 상태</p>
              <p class="text-h5 font-weight-bold mb-0">
                {{ stats.botRunning ? '실행 중' : '대기 중' }}
              </p>
              <p class="text-caption">메모리: {{ stats.memoryUsage }}%</p>
            </div>
          </div>
        </v-card>
      </v-col>
    </v-row>

    <!-- 시스템 모니터링 섹션 -->
    <v-row class="mb-4">
      <v-col cols="12">
        <v-card elevation="2">
          <v-card-title class="d-flex align-center">
            <v-icon class="mr-2">mdi-monitor-dashboard</v-icon>
            시스템 모니터링
            <v-spacer />
            <v-btn
              variant="text"
              size="small"
              @click="fetchMonitoring"
              :loading="monitoringLoading"
            >
              <v-icon>mdi-refresh</v-icon>
            </v-btn>
            <v-btn
              variant="text"
              size="small"
              @click="showMonitoringDialog = true"
              v-if="monitoring"
            >
              <v-icon>mdi-fullscreen</v-icon>
            </v-btn>
          </v-card-title>
          
          <v-card-text v-if="monitoring">
            <v-row>
              <!-- JVM 메모리 -->
              <v-col cols="12" md="3">
                <v-card variant="outlined" class="pa-3">
                  <div class="text-caption text-grey mb-1">JVM Heap 사용량</div>
                  <v-progress-linear
                    :model-value="monitoring.heapUsagePercent"
                    :color="getHeapColor(monitoring.heapUsagePercent)"
                    height="20"
                    rounded
                  >
                    <template v-slot:default>
                      <strong>{{ monitoring.heapUsagePercent.toFixed(1) }}%</strong>
                    </template>
                  </v-progress-linear>
                  <div class="text-caption mt-1">
                    {{ formatBytes(monitoring.heapUsed) }} / {{ formatBytes(monitoring.heapMax) }}
                  </div>
                </v-card>
              </v-col>
              
              <!-- DB 커넥션 풀 -->
              <v-col cols="12" md="3">
                <v-card variant="outlined" class="pa-3">
                  <div class="text-caption text-grey mb-1">DB 커넥션 풀</div>
                  <v-progress-linear
                    :model-value="(monitoring.dbActiveConnections / monitoring.dbMaxConnections) * 100"
                    :color="getDbPoolColor(monitoring.dbActiveConnections, monitoring.dbMaxConnections)"
                    height="20"
                    rounded
                  >
                    <template v-slot:default>
                      <strong>{{ monitoring.dbActiveConnections }} / {{ monitoring.dbMaxConnections }}</strong>
                    </template>
                  </v-progress-linear>
                  <div class="text-caption mt-1">
                    Active: {{ monitoring.dbActiveConnections }} | Idle: {{ monitoring.dbIdleConnections }}
                  </div>
                </v-card>
              </v-col>
              
              <!-- Redis 상태 -->
              <v-col cols="12" md="3">
                <v-card variant="outlined" class="pa-3">
                  <div class="text-caption text-grey mb-1">Redis 상태</div>
                  <div class="d-flex align-center">
                    <v-chip
                      :color="monitoring.redisConnected ? 'success' : 'error'"
                      size="small"
                      class="mr-2"
                    >
                      {{ monitoring.redisConnected ? '연결됨' : '연결 끊김' }}
                    </v-chip>
                    <span class="text-body-2">
                      {{ formatBytes(monitoring.redisUsedMemory) }}
                    </span>
                  </div>
                  <div class="text-caption mt-1">
                    연결 클라이언트: {{ monitoring.redisConnectedClients }}
                  </div>
                </v-card>
              </v-col>
              
              <!-- 업타임 & 스레드 -->
              <v-col cols="12" md="3">
                <v-card variant="outlined" class="pa-3">
                  <div class="text-caption text-grey mb-1">시스템 정보</div>
                  <div class="text-body-2">
                    <v-icon size="small" class="mr-1">mdi-clock-outline</v-icon>
                    업타임: {{ monitoring.uptimeFormatted }}
                  </div>
                  <div class="text-caption mt-1">
                    스레드: {{ monitoring.threadCount }} (Peak: {{ monitoring.peakThreadCount }})
                  </div>
                </v-card>
              </v-col>
            </v-row>
            
            <!-- 슬로우 쿼리 -->
            <v-row class="mt-2" v-if="monitoring.recentSlowQueries && monitoring.recentSlowQueries.length > 0">
              <v-col cols="12">
                <v-alert type="warning" variant="tonal" density="compact">
                  <div class="d-flex align-center">
                    <v-icon class="mr-2">mdi-database-alert</v-icon>
                    <strong>최근 슬로우 쿼리: {{ monitoring.recentSlowQueries.length }}건</strong>
                  </div>
                  <v-expansion-panels class="mt-2" variant="accordion">
                    <v-expansion-panel
                      v-for="(sq, idx) in monitoring.recentSlowQueries.slice(0, 5)"
                      :key="idx"
                    >
                      <v-expansion-panel-title>
                        <span class="text-error font-weight-bold mr-2">{{ sq.executionTimeMs }}ms</span>
                        <span class="text-truncate">{{ sq.query.substring(0, 50) }}...</span>
                      </v-expansion-panel-title>
                      <v-expansion-panel-text>
                        <div class="text-caption mb-1">실행 시간: {{ formatDateTime(sq.executedAt) }}</div>
                        <div class="text-caption mb-2">Source: {{ sq.source }}</div>
                        <code class="text-body-2" style="white-space: pre-wrap; word-break: break-all;">{{ sq.query }}</code>
                      </v-expansion-panel-text>
                    </v-expansion-panel>
                  </v-expansion-panels>
                </v-alert>
              </v-col>
            </v-row>
          </v-card-text>
          
          <v-card-text v-else>
            <v-skeleton-loader type="article" />
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- 알림 설정 상태 -->
    <v-row class="mb-4">
      <v-col cols="12">
        <v-card>
          <v-card-title class="d-flex align-center justify-space-between">
            <span>📢 알림 설정 상태</span>
            <div>
              <v-btn
                color="deep-purple"
                variant="outlined"
                size="small"
                class="mr-2"
                :loading="sendingDiscordTest"
                :disabled="!stats.discordEnabled"
                @click="sendSystemDiscordTest"
              >
                <v-icon left>mdi-discord</v-icon>
                디스코드 Hook 채널 테스트
              </v-btn>
              <v-btn
                color="success"
                variant="outlined"
                size="small"
                :loading="sendingEmailTest"
                :disabled="!stats.emailEnabled"
                @click="sendSystemEmailTest"
              >
                <v-icon left>mdi-email</v-icon>
                이메일 테스트
              </v-btn>
            </div>
          </v-card-title>
          <v-card-text>
            <v-chip :color="stats.discordEnabled ? 'success' : 'grey'" class="mr-2">
              <v-icon left small>mdi-discord</v-icon>
              Discord: {{ stats.discordEnabled ? '활성' : '비활성' }}
            </v-chip>
            <v-chip :color="stats.emailEnabled ? 'success' : 'grey'">
              <v-icon left small>mdi-email</v-icon>
              이메일: {{ stats.emailEnabled ? '활성' : '비활성' }}
            </v-chip>
          </v-card-text>
        </v-card>
  </v-col>
</v-row>

    <!-- Users Table -->
    <v-row>
      <v-col cols="12">
        <v-card>
          <v-card-title class="d-flex justify-space-between align-center">
            <span>👥 사용자 관리</span>
            <v-text-field
              v-model="search"
              append-icon="mdi-magnify"
              label="사용자 검색"
              single-line
              hide-details
              density="compact"
              style="max-width: 300px"
            />
          </v-card-title>
          
          <v-data-table
            :headers="headers"
            :items="users"
            :loading="loading"
            :search="search"
            class="elevation-0"
          >
            <!-- 사용자 ID -->
            <template v-slot:item.userId="{ item }">
              <div class="font-weight-medium">{{ item.userId }}</div>
            </template>

            <!-- 역할 -->
            <template v-slot:item.role="{ item }">
              <v-chip :color="item.role === 'ADMIN' ? 'error' : 'primary'" size="small">
                {{ item.role }}
              </v-chip>
            </template>

            <!-- 상태 -->
            <template v-slot:item.active="{ item }">
              <v-chip :color="item.active ? 'success' : 'grey'" size="small">
                {{ item.active ? '활성' : '비활성' }}
              </v-chip>
            </template>

            <!-- API 키 -->
            <template v-slot:item.hasApiKey="{ item }">
              <v-icon :color="item.hasApiKey ? 'success' : 'grey'">
                {{ item.hasApiKey ? 'mdi-check-circle' : 'mdi-close-circle' }}
              </v-icon>
            </template>

            <!-- 가입일 -->
            <template v-slot:item.joinDate="{ item }">
              {{ formatDate(item.joinDate) }}
            </template>

            <!-- 마지막 로그인 -->
            <template v-slot:item.lastLogin="{ item }">
              {{ formatDateTime(item.lastLogin) }}
            </template>

            <!-- 액션 -->
            <template v-slot:item.actions="{ item }">
              <v-btn
                icon
                size="small"
                :color="item.isActive ? 'warning' : 'success'"
                @click="toggleUserActive(item)"
                :disabled="item.role === 'ADMIN'"
              >
                <v-icon>{{ item.isActive ? 'mdi-account-off' : 'mdi-account-check' }}</v-icon>
              </v-btn>
            </template>
          </v-data-table>
        </v-card>
    </v-col>
    </v-row>
  </v-container>
    </v-main>
    <!-- 모니터링 상세 다이얼로그 -->
    <v-dialog v-model="showMonitoringDialog" max-width="900">
      <v-card v-if="monitoring">
        <v-card-title class="d-flex align-center">
          <v-icon class="mr-2">mdi-monitor-dashboard</v-icon>
          시스템 모니터링 상세
          <v-spacer />
          <v-btn icon variant="text" @click="showMonitoringDialog = false">
            <v-icon>mdi-close</v-icon>
          </v-btn>
        </v-card-title>
        
        <v-card-text>
          <v-row>
            <!-- JVM 메모리 상세 -->
            <v-col cols="12" md="6">
              <v-card variant="outlined" class="pa-4">
                <h4 class="mb-3">💾 JVM 메모리</h4>
                <v-progress-linear
                  :model-value="monitoring.heapUsagePercent"
                  :color="getHeapColor(monitoring.heapUsagePercent)"
                  height="25"
                  rounded
                  class="mb-3"
                >
                  <template v-slot:default>
                    <strong>{{ monitoring.heapUsagePercent.toFixed(1) }}%</strong>
                  </template>
                </v-progress-linear>
                <div class="text-body-2 mb-1">
                  <strong>Heap Used:</strong> {{ formatBytes(monitoring.heapUsed) }}
                </div>
                <div class="text-body-2 mb-1">
                  <strong>Heap Max:</strong> {{ formatBytes(monitoring.heapMax) }}
                </div>
                <div class="text-body-2">
                  <strong>Non-Heap:</strong> {{ formatBytes(monitoring.nonHeapUsed) }}
                </div>
              </v-card>
            </v-col>
            
            <!-- DB 커넥션 상세 -->
            <v-col cols="12" md="6">
              <v-card variant="outlined" class="pa-4">
                <h4 class="mb-3">🗄️ DB 커넥션 풀</h4>
                <v-progress-linear
                  :model-value="(monitoring.dbActiveConnections / monitoring.dbMaxConnections) * 100"
                  :color="getDbPoolColor(monitoring.dbActiveConnections, monitoring.dbMaxConnections)"
                  height="25"
                  rounded
                  class="mb-3"
                >
                  <template v-slot:default>
                    <strong>{{ monitoring.dbActiveConnections }} / {{ monitoring.dbMaxConnections }}</strong>
                  </template>
                </v-progress-linear>
                <div class="text-body-2 mb-1">
                  <strong>Active:</strong> {{ monitoring.dbActiveConnections }}
                </div>
                <div class="text-body-2 mb-1">
                  <strong>Idle:</strong> {{ monitoring.dbIdleConnections }}
                </div>
                <div class="text-body-2">
                  <strong>Total:</strong> {{ monitoring.dbTotalConnections }}
                </div>
              </v-card>
            </v-col>
            
            <!-- Redis 상세 -->
            <v-col cols="12" md="6">
              <v-card variant="outlined" class="pa-4">
                <h4 class="mb-3">🔴 Redis</h4>
                <v-chip
                  :color="monitoring.redisConnected ? 'success' : 'error'"
                  class="mb-3"
                >
                  {{ monitoring.redisConnected ? '✅ 연결됨' : '❌ 연결 끊김' }}
                </v-chip>
                <div class="text-body-2 mb-1">
                  <strong>메모리 사용:</strong> {{ formatBytes(monitoring.redisUsedMemory) }}
                </div>
                <div class="text-body-2">
                  <strong>연결 클라이언트:</strong> {{ monitoring.redisConnectedClients }}
                </div>
              </v-card>
            </v-col>
            
            <!-- 시스템 정보 상세 -->
            <v-col cols="12" md="6">
              <v-card variant="outlined" class="pa-4">
                <h4 class="mb-3">⚙️ 시스템 정보</h4>
                <div class="text-body-2 mb-1">
                  <strong>업타임:</strong> {{ monitoring.uptimeFormatted }}
                </div>
                <div class="text-body-2 mb-1">
                  <strong>CPU 코어:</strong> {{ monitoring.availableProcessors }}개
                </div>
                <div class="text-body-2 mb-1">
                  <strong>시스템 로드:</strong> {{ monitoring.systemLoadAverage.toFixed(2) }}
                </div>
                <div class="text-body-2 mb-1">
                  <strong>스레드:</strong> {{ monitoring.threadCount }} (Peak: {{ monitoring.peakThreadCount }})
                </div>
                <div class="text-body-2">
                  <strong>데몬 스레드:</strong> {{ monitoring.daemonThreadCount }}
                </div>
              </v-card>
            </v-col>
          </v-row>
          
          <!-- 에러 카운트 -->
          <v-row class="mt-2">
            <v-col cols="12">
              <v-alert
                :type="monitoring.recentErrorCount > 0 ? 'warning' : 'success'"
                variant="tonal"
              >
                최근 1시간 에러: <strong>{{ monitoring.recentErrorCount }}건</strong>
              </v-alert>
            </v-col>
          </v-row>
          
          <!-- 수집 시간 -->
          <div class="text-caption text-grey mt-3 text-right">
            마지막 수집: {{ formatDateTime(monitoring.collectedAt) }}
          </div>
        </v-card-text>
      </v-card>
    </v-dialog>
  </v-app>
</template>



<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onUnmounted } from 'vue'
import api, { adminApi } from '@/api'

import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'

interface SystemStats {
  totalUsers: number
  activeUsers: number
  usersWithApiKey: number
  todayBuyCount: number
  todaySellCount: number
  todayTotalVolume: number
  totalTransactions: number
  totalInvestment: number
  totalProfit: number
  botRunning: boolean
  lastBotExecution: string | null
  discordEnabled: boolean
  emailEnabled: boolean
  systemStatus: string
  uptime: number
  memoryUsage: number
  cpuUsage: number
}

interface AdminUser {
  id: number
  userId: string
  email: string
  phone: string
  role: string
  active: boolean
  hasApiKey: boolean
  joinDate: string
  lastLogin: string
  totalTransactions: number
  holdingCount: number
}

interface MonitoringMetrics {
  heapUsed: number
  heapMax: number
  heapUsagePercent: number
  nonHeapUsed: number
  availableProcessors: number
  systemLoadAverage: number
  uptimeSeconds: number
  uptimeFormatted: string
  threadCount: number
  peakThreadCount: number
  daemonThreadCount: number
  dbActiveConnections: number
  dbIdleConnections: number
  dbTotalConnections: number
  dbMaxConnections: number
  redisConnected: boolean
  redisUsedMemory: number
  redisConnectedClients: number
  recentSlowQueries: SlowQueryInfo[]
  apiResponseTimes: Record<string, number>
  recentErrorCount: number
  collectedAt: string
}

interface SlowQueryInfo {
  query: string
  executionTimeMs: number
  executedAt: string
  source: string
}

const sidebarRef = ref()

const loading = ref(false)
const search = ref('')
const stats = ref<SystemStats>({
  totalUsers: 0,
  activeUsers: 0,
  usersWithApiKey: 0,
  todayBuyCount: 0,
  todaySellCount: 0,
  todayTotalVolume: 0,
  totalTransactions: 0,
  totalInvestment: 0,
  totalProfit: 0,
  botRunning: false,
  lastBotExecution: null,
  discordEnabled: false,
  emailEnabled: false,
  systemStatus: 'UNKNOWN',
  uptime: 0,
  memoryUsage: 0,
  cpuUsage: 0
})
const users = ref<AdminUser[]>([])

const headers = [
  { title: '사용자 ID', key: 'userId' },
  { title: '이메일', key: 'email' },
  { title: '역할', key: 'role' },
  { title: '상태', key: 'active' },
  { title: 'API 키', key: 'hasApiKey' },
  { title: '거래 수', key: 'totalTransactions' },
  { title: '보유 건수', key: 'holdingCount' },
  { title: '가입일', key: 'joinDate' },
  { title: '마지막 로그인', key: 'lastLogin' },
  { title: '액션', key: 'actions', sortable: false }
]

// 모니터링 데이터 ref
const monitoring = ref<MonitoringMetrics | null>(null)
const monitoringLoading = ref(false)
const showMonitoringDialog = ref(false)

// 상태 변수 추가
const sendingDiscordTest = ref(false)
const sendingEmailTest = ref(false)

const formatKRW = (value: number) => {
  if (!value) return '0원'
  return new Intl.NumberFormat('ko-KR').format(value) + '원'
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('ko-KR')
}

// ★★★ 수정: 마지막 로그인 시간 - 우상단 현재 시간과 동일한 방식 ★★★
const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return '-'
  
  // toLocaleString('ko-KR') 방식 사용
  const dateOptions: Intl.DateTimeFormatOptions = { 
    year: 'numeric', 
    month: '2-digit', 
    day: '2-digit' 
  }
  const timeOptions: Intl.DateTimeFormatOptions = { 
    hour: '2-digit', 
    minute: '2-digit', 
    second: '2-digit',
    hour12: true
  }
  
  const datePart = date.toLocaleDateString('ko-KR', dateOptions)
  const timePart = date.toLocaleTimeString('ko-KR', timeOptions)
  
  return `${datePart} ${timePart}`
}

const toggleUserActive = async (user: AdminUser) => {
  try {
    await adminApi.toggleUserActive(user.userId)
    await fetchUsers()
  } catch (error) {
    console.error('사용자 상태 변경 실패:', error)
  }
}

// ★★★ 추가: fetchStats 함수 ★★★
const fetchStats = async () => {
  try {
    const response = await adminApi.getStats()
    if (response.data) {
      stats.value = { ...stats.value, ...response.data }
    }
  } catch (error) {
    console.error('통계 조회 실패:', error)
  }
}

// ★★★ 추가: fetchUsers 함수 ★★★
const fetchUsers = async () => {
  try {
    const response = await adminApi.getUsers()
    // API 응답이 배열인지 확인하고 처리
    if (Array.isArray(response.data)) {
      users.value = response.data
    } else if (response.data?.content && Array.isArray(response.data.content)) {
      // 페이징 응답인 경우
      users.value = response.data.content
    } else {
      users.value = []
    }
  } catch (error) {
    console.error('사용자 목록 조회 실패:', error)
    users.value = []
  }
}

const refreshData = async () => {
  loading.value = true
  await Promise.all([fetchStats(), fetchUsers(), fetchMonitoring()])
  loading.value = false
}

// 모니터링 데이터 조회 함수
const fetchMonitoring = async () => {
  monitoringLoading.value = true
  try {
    const response = await adminApi.getMonitoring()
    monitoring.value = response.data
  } catch (error) {
    console.error('모니터링 데이터 조회 실패:', error)
  } finally {
    monitoringLoading.value = false
  }
}

const formatBytes = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const getHeapColor = (percent: number) => {
  if (percent >= 90) return 'error'
  if (percent >= 70) return 'warning'
  return 'success'
}

const getDbPoolColor = (active: number, max: number) => {
  const ratio = active / max
  if (ratio >= 0.9) return 'error'
  if (ratio >= 0.7) return 'warning'
  return 'success'
}

const sendSystemDiscordTest = async () => {
  sendingDiscordTest.value = true
  try {
    await api.post('/notifications/test')
    // 성공 알림 (스낵바 사용 시)
    alert('디스코드 테스트 알림이 발송되었습니다.')
  } catch (error) {
    console.error('디스코드 테스트 실패:', error)
    alert('디스코드 알림 발송에 실패했습니다.')
  } finally {
    sendingDiscordTest.value = false
  }
}

const sendSystemEmailTest = async () => {
  sendingEmailTest.value = true
  try {
    await api.post('/notifications/email/test')
    alert('테스트 이메일이 발송되었습니다.')
  } catch (error) {
    console.error('이메일 테스트 실패:', error)
    alert('이메일 발송에 실패했습니다.')
  } finally {
    sendingEmailTest.value = false
  }
}

// 30초마다 모니터링 자동 새로고침
let monitoringInterval: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  refreshData()
  // 30초마다 모니터링 데이터 갱신
  monitoringInterval = setInterval(fetchMonitoring, 30000)
})

onUnmounted(() => {
  if (monitoringInterval) {
    clearInterval(monitoringInterval)
  }
})
</script>

<style scoped>
/* ✅ 추가: 통계 카드 높이 통일 */
.stats-card {
  height: 100%;
  min-height: 100px;
}

.stats-card .d-flex {
  height: 100%;
}
</style>