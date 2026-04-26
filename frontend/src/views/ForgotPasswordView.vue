<template>
  <v-container class="fill-height" fluid>
    <v-row align="center" justify="center">
      <v-col cols="12" sm="8" md="5">
        <v-card class="elevation-12">
          <v-toolbar color="primary" dark flat>
            <v-toolbar-title>비밀번호 찾기</v-toolbar-title>
          </v-toolbar>

          <v-card-text>
            <!-- ====== 1단계: 이메일 입력 ====== -->
            <div v-if="step === 'email'">
              <h2 class="text-center mb-4">이메일 인증</h2>
              <p class="text-body-2 text-grey text-center mb-4">
                가입 시 등록한 이메일을 입력하시면<br>
                인증 코드를 발송해드립니다.
              </p>

              <v-alert
                v-if="errorMessage"
                type="error"
                variant="tonal"
                dismissible
                class="mb-4"
                @click:close="errorMessage = ''"
              >{{ errorMessage }}</v-alert>

              <v-form ref="emailFormRef" v-model="emailValid" @submit.prevent="handleRequestOtp">
                <v-text-field
                  v-model="email"
                  label="이메일"
                  type="email"
                  prepend-icon="mdi-email"
                  :rules="[rules.required, rules.email]"
                  variant="outlined"
                  class="mb-2"
                  autofocus
                />

                <v-btn
                  type="submit"
                  color="primary"
                  block
                  size="large"
                  :loading="loading"
                  :disabled="!emailValid"
                  class="mt-4"
                >인증 코드 발송</v-btn>
              </v-form>
            </div>

            <!-- ====== 2단계: OTP 입력 ====== -->
            <div v-else-if="step === 'otp'">
              <h2 class="text-center mb-4">인증 코드 입력</h2>
              <p class="text-body-2 text-center mb-2">
                <strong>{{ maskedEmail }}</strong> 으로<br>
                6자리 인증 코드를 발송했습니다.
              </p>

              <!-- ⭐⭐⭐ 실시간 카운트다운 ⭐⭐⭐ -->
              <div class="text-center mb-4">
                <v-chip
                  :color="remainingSeconds > 60 ? 'success' : (remainingSeconds > 0 ? 'warning' : 'error')"
                  size="large"
                  variant="elevated"
                >
                  <v-icon start>mdi-timer-outline</v-icon>
                  <span v-if="remainingSeconds > 0">유효 시간: {{ formattedTime }}</span>
                  <span v-else>인증 코드가 만료되었습니다</span>
                </v-chip>
              </div>

              <v-alert
                v-if="errorMessage"
                type="error"
                variant="tonal"
                dismissible
                class="mb-4"
                @click:close="errorMessage = ''"
              >{{ errorMessage }}</v-alert>

              <v-form ref="otpFormRef" v-model="otpValid" @submit.prevent="handleVerifyOtp">
                <v-text-field
                  v-model="otpCode"
                  label="인증 코드 (6자리)"
                  prepend-icon="mdi-shield-key"
                  maxlength="6"
                  :rules="[rules.required, rules.otpLength]"
                  variant="outlined"
                  class="mb-2"
                  autofocus
                  :disabled="remainingSeconds <= 0"
                  inputmode="numeric"
                />

                <v-btn
                  type="submit"
                  color="primary"
                  block
                  size="large"
                  :loading="loading"
                  :disabled="!otpValid || remainingSeconds <= 0"
                  class="mt-4"
                >인증 확인</v-btn>

                <v-btn
                  block
                  variant="text"
                  size="small"
                  class="mt-2"
                  :disabled="loading"
                  @click="resendOtp"
                >
                  <v-icon start>mdi-refresh</v-icon>
                  인증 코드 다시 받기
                </v-btn>
              </v-form>
            </div>

            <!-- ====== 3단계: 임시 비밀번호 표시 ====== -->
            <div v-else-if="step === 'success'">
              <h2 class="text-center mb-4">
                <v-icon color="success" size="40">mdi-check-circle</v-icon><br>
                임시 비밀번호 발급 완료
              </h2>

              <v-alert
                type="info"
                variant="tonal"
                class="mb-4"
              >
                <strong>이 화면을 닫으면 임시 비밀번호를 다시 확인할 수 없습니다.</strong><br>
                반드시 메모하거나 복사해주세요.
              </v-alert>

              <div
                class="pa-4 mb-3"
                style="background: #f5f5f5; border: 2px dashed #1976d2; border-radius: 8px; text-align: center;"
              >
                <p class="text-caption text-grey mb-1">임시 비밀번호</p>
                <p
                  class="text-h5 font-weight-bold mb-0"
                  style="font-family: 'Consolas', 'Monaco', monospace; letter-spacing: 2px; color: #1976d2; word-break: break-all;"
                >{{ tempPassword }}</p>
              </div>

              <v-btn
                block
                color="primary"
                variant="outlined"
                prepend-icon="mdi-content-copy"
                class="mb-3"
                @click="copyPassword"
              >클립보드에 복사</v-btn>

              <!-- ⭐ 사용자 요청: 본인이 희망하는 비밀번호로 수정 권고 ⭐ -->
              <v-alert
                type="warning"
                variant="tonal"
                class="mb-3"
              >
                <!-- ⭐⭐⭐ [수정] 가독성 개선: 글자 색 노란색 → 검정색 ⭐⭐⭐ -->
                <p class="font-weight-bold mb-1" style="color: #000;">⚠ 보안 권장 사항</p>
                <p class="text-body-2 mb-0" style="color: #000;">
                  로그인 후 <strong>"프로필 설정 → 비밀번호 변경"</strong> 메뉴에서<br>
                  본인이 희망하는 비밀번호로 <strong>즉시 변경</strong>해주세요.
                </p>
              </v-alert>

              <v-btn
                color="success"
                block
                size="large"
                @click="goToLogin"
              >로그인 화면으로 이동</v-btn>
            </div>
          </v-card-text>

          <v-divider v-if="step !== 'success'" />

          <v-card-actions v-if="step !== 'success'">
            <v-spacer />
            <v-btn
              variant="text"
              color="primary"
              @click="$router.push('/login')"
              :disabled="loading"
            >로그인으로 돌아가기</v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api'

const router = useRouter()

// ==== 단계 관리 ====
type Step = 'email' | 'otp' | 'success'
const step = ref<Step>('email')

// ==== 폼 데이터 ====
const email = ref('')
const otpCode = ref('')
const maskedEmail = ref('')
const tempPassword = ref('')

// ==== 폼 검증 ====
const emailFormRef = ref()
const otpFormRef = ref()
const emailValid = ref(false)
const otpValid = ref(false)

// ==== 상태 ====
const loading = ref(false)
const errorMessage = ref('')

// ==== 실시간 카운트다운 (총 3분 = 180초) ====
const remainingSeconds = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

// MM:SS 포맷
const formattedTime = computed(() => {
  const min = Math.floor(remainingSeconds.value / 60)
  const sec = remainingSeconds.value % 60
  return `${String(min).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
})

// 카운트다운 시작
const startCountdown = (seconds: number) => {
  stopCountdown()
  remainingSeconds.value = seconds
  countdownTimer = setInterval(() => {
    if (remainingSeconds.value > 0) {
      remainingSeconds.value -= 1
    } else {
      stopCountdown()
    }
  }, 1000)
}

// 카운트다운 중단
const stopCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

// 컴포넌트 언마운트 시 타이머 정리 (메모리 누수 방지)
onUnmounted(() => {
  stopCountdown()
})

// ==== 검증 규칙 ====
const rules = {
  required: (v: string) => !!v || '필수 입력 항목입니다',
  email: (v: string) => {
    const pattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return pattern.test(v) || '올바른 이메일 형식이 아닙니다'
  },
  otpLength: (v: string) => /^\d{6}$/.test(v) || '6자리 숫자를 입력하세요'
}

// ==== 1단계: OTP 발송 요청 ====
const handleRequestOtp = async () => {
  if (!emailFormRef.value) return
  const { valid } = await emailFormRef.value.validate()
  if (!valid) return

  loading.value = true
  errorMessage.value = ''

  try {
    const response = await authApi.requestPasswordResetOtp(email.value)
    if (response.data.success) {
      maskedEmail.value = response.data.maskedEmail || email.value
      const expiry = response.data.expiryMinutes || 3
      // 단계 전환 + 카운트다운 시작
      step.value = 'otp'
      startCountdown(expiry * 60)
      otpCode.value = ''
    } else {
      errorMessage.value = response.data.message || '인증 코드 발송에 실패했습니다.'
    }
  } catch (error: any) {
    console.error('OTP 발송 실패:', error)
    errorMessage.value = error?.response?.data?.message
      || error?.message
      || '인증 코드 발송에 실패했습니다.'
  } finally {
    loading.value = false
  }
}

// ==== 2단계: OTP 검증 + 임시 비밀번호 발급 ====
const handleVerifyOtp = async () => {
  if (!otpFormRef.value) return
  const { valid } = await otpFormRef.value.validate()
  if (!valid) return

  if (remainingSeconds.value <= 0) {
    errorMessage.value = '인증 코드가 만료되었습니다. 다시 발급받아주세요.'
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const response = await authApi.verifyPasswordResetOtp(email.value, otpCode.value)
    if (response.data.success && response.data.tempPassword) {
      tempPassword.value = response.data.tempPassword
      stopCountdown()
      step.value = 'success'
    } else {
      errorMessage.value = response.data.message || '인증에 실패했습니다.'
    }
  } catch (error: any) {
    console.error('OTP 검증 실패:', error)
    errorMessage.value = error?.response?.data?.message
      || error?.message
      || '인증에 실패했습니다.'
  } finally {
    loading.value = false
  }
}

// ==== OTP 재발급 ====
const resendOtp = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await authApi.requestPasswordResetOtp(email.value)
    if (response.data.success) {
      const expiry = response.data.expiryMinutes || 3
      startCountdown(expiry * 60)
      otpCode.value = ''
    } else {
      errorMessage.value = response.data.message || '인증 코드 재발송에 실패했습니다.'
    }
  } catch (error: any) {
    console.error('OTP 재발송 실패:', error)
    errorMessage.value = error?.response?.data?.message
      || error?.message
      || '인증 코드 재발송에 실패했습니다.'
  } finally {
    loading.value = false
  }
}

// ==== 클립보드 복사 ====
const copyPassword = async () => {
  try {
    await navigator.clipboard.writeText(tempPassword.value)
    alert('임시 비밀번호가 클립보드에 복사되었습니다.')
  } catch (e) {
    console.error('클립보드 복사 실패:', e)
    alert('자동 복사에 실패했습니다. 임시 비밀번호를 직접 선택해 복사해주세요.')
  }
}

// ==== 로그인 화면 이동 (메모리 정리) ====
const goToLogin = () => {
  tempPassword.value = ''
  email.value = ''
  otpCode.value = ''
  router.push('/login')
}
</script>

<style scoped>
.fill-height {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
</style>