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
        prepend-icon="mdi-chart-line"
        title="거래 현황 (준비중)"
        disabled
      />

      <v-list-item
        prepend-icon="mdi-history"
        title="거래 이력 (준비중)"
        disabled
      />

      <v-list-item
        prepend-icon="mdi-cog"
        title="거래 설정 (준비중)"
        disabled
      />

      <v-divider class="my-2" />

      <v-list-item
        prepend-icon="mdi-account-cog"
        title="프로필 설정 (준비중)"
        disabled
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