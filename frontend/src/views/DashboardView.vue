<template>
  <v-app>
    <the-header />

    <the-sidebar />

    <v-main>
      <v-container fluid>
        <v-row>
          <v-col cols="12">
            <h1 class="text-h4 mb-4">대시보드</h1>
          </v-col>
        </v-row>

        <!-- 사용자 정보 카드 -->
        <v-row>
          <v-col cols="12" md="4">
            <v-card>
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

          <!-- 시스템 상태 카드 -->
          <v-col cols="12" md="4">
            <v-card>
              <v-card-title>
                <v-icon class="mr-2">mdi-server</v-icon>
                시스템 상태
              </v-card-title>
              <v-card-text>
                <div class="mb-2">
                  <strong>상태:</strong>
                  <v-chip color="success" size="small">
                    <v-icon start>mdi-check-circle</v-icon>
                    정상
                  </v-chip>
                </div>
                <div class="mb-2">
                  <strong>마지막 로그인:</strong> {{ lastLoginFormatted }}
                </div>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- 빠른 액세스 카드 -->
          <v-col cols="12" md="4">
            <v-card>
              <v-card-title>
                <v-icon class="mr-2">mdi-lightning-bolt</v-icon>
                빠른 액세스
              </v-card-title>
              <v-card-text>
                <v-btn
                  block
                  color="primary"
                  class="mb-2"
                  disabled
                >
                  <v-icon start>mdi-cog</v-icon>
                  거래 설정 (준비중)
                </v-btn>
                <v-btn
                  block
                  color="secondary"
                  disabled
                >
                  <v-icon start>mdi-account-cog</v-icon>
                  프로필 설정 (준비중)
                </v-btn>
              </v-card-text>
            </v-card>
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
                  :items="coinStore.coins"
                  :loading="coinStore.loading"
                  items-per-page="10"
                >
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
                      @click="viewCoinDetail(item.symbol)"
                    >
                      상세보기
                    </v-btn>
                  </template>
                </v-data-table>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useCoinStore } from '@/stores/coin'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'

const authStore = useAuthStore()
const coinStore = useCoinStore()

const headers = [
  { title: '순위', key: 'marketCapRank', align: 'center' },
  { title: '한글명', key: 'nameKr' },
  { title: '영문명', key: 'nameEn' },
  { title: '심볼', key: 'symbol' },
  { title: '상태', key: 'isActive', align: 'center' },
  { title: '액션', key: 'actions', align: 'center', sortable: false }
]

const lastLoginFormatted = computed(() => {
  if (!authStore.user?.lastLogin) return '정보 없음'
  return new Date(authStore.user.lastLogin).toLocaleString('ko-KR')
})

const loadCoins = async () => {
  await coinStore.fetchActiveCoins()
}

const viewCoinDetail = (symbol: string) => {
  // TODO: 코인 상세 페이지 구현
  alert(`${symbol} 상세 정보 - 추후 구현 예정`)
}

onMounted(() => {
  loadCoins()
})
</script>