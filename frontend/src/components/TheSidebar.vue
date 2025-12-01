<template>
  <v-navigation-drawer
    v-model="drawer"
    temporary
  >
    <v-list>
      <v-list-item
        prepend-icon="mdi-view-dashboard"
        title="대시보드"
        @click="$router.push('/dashboard')"
      />

      <v-list-item
        prepend-icon="mdi-wallet"
        title="보유 자산"
        @click="$router.push('/holdings')"
      />

      <v-list-item
        prepend-icon="mdi-history"
        title="거래 내역"
        @click="$router.push('/transactions')"
      />

      <v-list-item
        prepend-icon="mdi-cog"
        title="거래 설정"
        @click="$router.push('/trading-settings')"
      />

      <v-divider class="my-2" />

      <!-- ⭐ 추가: 봇 모니터링 메뉴 -->
      <v-list-item
        prepend-icon="mdi-robot"
        title="봇 모니터링"
        @click="$router.push('/bot-monitor')"
      />

      <!-- ⭐ 추가: 일일 리포트 메뉴 -->
      <v-list-item
        prepend-icon="mdi-file-chart"
        title="일일 리포트"
        @click="$router.push('/daily-report')"
      />

      <v-divider class="my-2" />

      <v-list-item
        prepend-icon="mdi-account-cog"
        title="프로필 설정"
        @click="$router.push('/profile')"
      />

      <v-list-item
        v-if="authStore.isAdmin"
        prepend-icon="mdi-shield-crown"
        title="관리자 페이지 (준비중)"
        disabled
      />
    </v-list>
  </v-navigation-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const drawer = ref(false)

defineExpose({ drawer })
</script>