<template>
  <v-snackbar
    v-model="snackbar.show"
    :color="snackbar.color"
    :timeout="snackbar.timeout"
    location="top"
    multi-line
  >
    <div class="d-flex align-center">
      <v-icon :icon="snackbar.icon" class="mr-2" />
      <div>
        <div class="font-weight-medium">{{ snackbar.title }}</div>
        <div v-if="snackbar.message" class="text-body-2">
          {{ snackbar.message }}
        </div>
      </div>
    </div>
    <template #actions>
      <v-btn variant="text" @click="snackbar.show = false">
        닫기
      </v-btn>
    </template>
  </v-snackbar>
</template>

<script setup lang="ts">
import { reactive, provide } from 'vue'

interface SnackbarState {
  show: boolean
  title: string
  message: string
  color: string
  icon: string
  timeout: number
}

const snackbar = reactive<SnackbarState>({
  show: false,
  title: '',
  message: '',
  color: 'success',
  icon: 'mdi-check-circle',
  timeout: 4000
})

const showSuccess = (title: string, message?: string) => {
  snackbar.show = true
  snackbar.title = title
  snackbar.message = message || ''
  snackbar.color = 'success'
  snackbar.icon = 'mdi-check-circle'
  snackbar.timeout = 4000
}

const showError = (title: string, message?: string) => {
  snackbar.show = true
  snackbar.title = title
  snackbar.message = message || ''
  snackbar.color = 'error'
  snackbar.icon = 'mdi-alert-circle'
  snackbar.timeout = 6000
}

const showWarning = (title: string, message?: string) => {
  snackbar.show = true
  snackbar.title = title
  snackbar.message = message || ''
  snackbar.color = 'warning'
  snackbar.icon = 'mdi-alert'
  snackbar.timeout = 5000
}

const showInfo = (title: string, message?: string) => {
  snackbar.show = true
  snackbar.title = title
  snackbar.message = message || ''
  snackbar.color = 'info'
  snackbar.icon = 'mdi-information'
  snackbar.timeout = 4000
}

// 전역으로 사용할 수 있도록 provide
provide('showSuccess', showSuccess)
provide('showError', showError)
provide('showWarning', showWarning)
provide('showInfo', showInfo)

// 외부에서 사용할 수 있도록 expose
defineExpose({
  showSuccess,
  showError,
  showWarning,
  showInfo
})
</script>