<template>
  <v-app-bar color="primary" prominent>
    <v-app-bar-nav-icon @click="toggleDrawer" />

    <v-toolbar-title>
      <v-icon class="mr-2">mdi-chart-line</v-icon>
      코인 자동매매 시스템
    </v-toolbar-title>

    <v-spacer />

    <v-menu>
      <template v-slot:activator="{ props }">
        <v-btn icon v-bind="props">
          <v-icon>mdi-account-circle</v-icon>
        </v-btn>
      </template>

      <v-list>
        <v-list-item>
          <v-list-item-title>{{ authStore.user?.userId }}</v-list-item-title>
          <v-list-item-subtitle>{{ authStore.user?.email }}</v-list-item-subtitle>
        </v-list-item>

        <v-divider />

        <v-list-item disabled>
          <template v-slot:prepend>
            <v-icon>mdi-account-cog</v-icon>
          </template>
          <v-list-item-title>프로필 설정 (준비중)</v-list-item-title>
        </v-list-item>

        <v-list-item @click="handleLogout">
          <template v-slot:prepend>
            <v-icon>mdi-logout</v-icon>
          </template>
          <v-list-item-title>로그아웃</v-list-item-title>
        </v-list-item>
      </v-list>
    </v-menu>
  </v-app-bar>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const emit = defineEmits(['toggle-drawer'])

const toggleDrawer = () => {
  emit('toggle-drawer')
}

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}
</script>