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
        <v-card class="pa-4" elevation="2">
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
        <v-card class="pa-4" elevation="2">
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
        <v-card class="pa-4" elevation="2">
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
        <v-card class="pa-4" elevation="2">
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

    <!-- 알림 설정 상태 -->
    <v-row class="mb-4">
      <v-col cols="12">
        <v-card>
          <v-card-title>📢 알림 설정 상태</v-card-title>
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
  </v-app>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api'

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
  { title: '액션', key: 'actions', sortable: false }
]

const formatKRW = (value: number) => {
  if (!value) return '0원'
  return new Intl.NumberFormat('ko-KR').format(value) + '원'
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('ko-KR')
}

const fetchStats = async () => {
  try {
    const response = await adminApi.getStats()
    stats.value = response.data
  } catch (error) {
    console.error('통계 조회 실패:', error)
  }
}

const fetchUsers = async () => {
  try {
    const response = await adminApi.getUsers()
    users.value = response.data.content || []
  } catch (error) {
    console.error('사용자 목록 조회 실패:', error)
  }
}

const toggleUserActive = async (user: AdminUser) => {
  try {
    await adminApi.toggleUserActive(user.userId)
    await fetchUsers()
  } catch (error) {
    console.error('사용자 상태 변경 실패:', error)
  }
}

const refreshData = async () => {
  loading.value = true
  await Promise.all([fetchStats(), fetchUsers()])
  loading.value = false
}

onMounted(() => {
  refreshData()
})
</script>