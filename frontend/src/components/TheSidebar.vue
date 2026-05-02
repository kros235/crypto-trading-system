<template>
  <v-navigation-drawer
    v-model="drawer"
    temporary
    width="280"
  >
    <v-list v-model:opened="openedGroups">
      <v-list-group value="coin">
        <template v-slot:activator="{ props }">
          <v-list-item
            v-bind="props"
            prepend-icon="mdi-bitcoin"
            title="코인 거래"
          />
        </template>

        <v-list-item
          prepend-icon="mdi-view-dashboard"
          title="대시보드"
          @click="$router.push('/dashboard')"
        />
        <v-list-item
          prepend-icon="mdi-briefcase-outline"
          title="보유 코인 자산"
          @click="$router.push('/holdings')"
        />
        <v-list-item
          prepend-icon="mdi-currency-btc"
          title="코인 목록"
          @click="$router.push('/coins')"
        />
        <v-list-item
          prepend-icon="mdi-history"
          title="코인 거래 내역"
          @click="$router.push('/transactions')"
        />
        <v-list-item
          prepend-icon="mdi-cog"
          title="코인 거래 설정"
          @click="$router.push('/trading-settings')"
        />
        <v-list-item
          prepend-icon="mdi-robot"
          title="봇 모니터링"
          @click="$router.push('/bot-monitor')"
        />
        <v-list-item
          prepend-icon="mdi-file-chart"
          title="일일 리포트"
          @click="$router.push('/daily-report')"
        />
        <v-list-item
          prepend-icon="mdi-chart-timeline-variant"
          title="백테스팅"
          @click="$router.push('/backtest')"
        />
        <v-list-item
          prepend-icon="mdi-newspaper"
          title="코인 뉴스"
          @click="$router.push('/news')"
        />
      </v-list-group>

      <v-divider class="my-2" />

      <v-list-group value="stock">
        <template v-slot:activator="{ props }">
           <v-list-item
             v-bind="props"
             prepend-icon="mdi-chart-line"
             title="주식 거래 (공사중)"
           />
        </template>

        <v-list-item
          prepend-icon="mdi-view-dashboard-outline"
          title="대시보드"
          @click="$router.push('/stock-dashboard')"
        />
        <!-- ⭐⭐⭐ [Day 60 변경] disabled 제거 + @click 추가 ⭐⭐⭐ -->
        <v-list-item
          prepend-icon="mdi-briefcase-outline"
          title="보유 주식 자산"
          @click="$router.push('/stock-holdings')"
        />
        <!-- ⭐⭐⭐ [Day 60 변경] disabled 제거 + @click 추가 ⭐⭐⭐ -->
        <v-list-item
          prepend-icon="mdi-chart-line"
          title="주식 종목 목록"
          @click="$router.push('/stock-list')"
        />
        <v-list-item
            prepend-icon="mdi-history"
            title="주식 거래 내역"
            to="/stock-transactions"
          />
        <!-- ⭐⭐⭐ 변경: disabled 제거, @click 추가 ⭐⭐⭐ -->
        <v-list-item
          prepend-icon="mdi-cog-outline"
          title="주식 거래 설정"
          @click="$router.push('/stock/settings')"
        />
        <!-- ⭐⭐⭐ [Day 61 추가] 주식 봇 모니터링 메뉴 ⭐⭐⭐ -->
        <v-list-item
          prepend-icon="mdi-robot"
          title="봇 모니터링"
          @click="$router.push('/stock-bot-monitor')"
        />
      </v-list-group>

      <v-divider class="my-2" />

      <v-list-item
        prepend-icon="mdi-account-cog"
        title="프로필 설정"
        @click="$router.push('/profile')"
      />

      <v-list-item
        prepend-icon="mdi-shield-lock"
        title="계정 보안"
        @click="$router.push('/account-security')"
      />

      <v-list-item
        v-if="authStore.isAdmin"
        :to="'/admin'"
        prepend-icon="mdi-shield-crown"
        title="관리자"
      />

      <v-list-item
        prepend-icon="mdi-bullhorn"
        title="릴리즈 노트"
        @click="$router.push('/release-notes')"
      />
     <v-list-item
        prepend-icon="mdi-help-circle"
        title="도움말"
        @click="$router.push('/help')"
      />
    </v-list>
  </v-navigation-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const drawer = ref(false)
const openedGroups = ref(['coin'])  

defineExpose({ drawer })
</script>