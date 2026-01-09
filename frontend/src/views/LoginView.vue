<template>
  <v-container class="fill-height" fluid>
    <v-row align="center" justify="center">
      <v-col cols="12" sm="8" md="4">
        <v-card class="elevation-12">
          <v-toolbar color="primary" dark flat>
            <v-toolbar-title>코인 자동매매 시스템</v-toolbar-title>
          </v-toolbar>

          <v-card-text>
            <h2 class="text-center mb-4">로그인</h2>

            <v-alert
              v-if="authStore.error"
              type="error"
              dismissible
              class="mb-4"
            >
              {{ authStore.error }}
            </v-alert>

            <v-form ref="formRef" v-model="valid" @submit.prevent="handleLogin">
              <v-text-field
                v-model="loginForm.userId"
                label="사용자 ID"
                prepend-icon="mdi-account"
                :rules="[rules.required]"
                variant="outlined"
                class="mb-2"
              />

              <v-text-field
                v-model="loginForm.password"
                label="비밀번호"
                prepend-icon="mdi-lock"
                :type="showPassword ? 'text' : 'password'"
                :append-icon="showPassword ? 'mdi-eye' : 'mdi-eye-off'"
                @click:append="showPassword = !showPassword"
                :rules="[rules.required]"
                variant="outlined"
                class="mb-2"
              />

              <!-- 2FA OTP 입력 필드 -->
              <v-expand-transition>
                <div v-if="requires2FA">
                  <v-alert type="info" variant="tonal" class="mb-3">
                    2단계 인증이 활성화되어 있습니다.<br>
                    Google Authenticator 앱의 6자리 코드를 입력하세요.
                  </v-alert>
                  <v-text-field
                    v-model="otpCode"
                    label="인증 코드 (6자리)"
                    prepend-icon="mdi-shield-key"
                    maxlength="6"
                    :rules="[rules.required, rules.otpLength]"
                    variant="outlined"
                    class="mb-2"
                    autofocus
                  />
                </div>
              </v-expand-transition>

              <v-btn
                type="submit"
                color="primary"
                block
                size="large"
                :loading="authStore.loading"
                :disabled="!valid"
                class="mt-4"
              >
                로그인
              </v-btn>
            </v-form>
          </v-card-text>

          <v-divider />

          <v-card-actions>
            <v-spacer />
            <v-btn
              text
              color="primary"
              @click="$router.push('/signup')"
            >
              회원가입
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref()
const valid = ref(false)
const showPassword = ref(false)
const requires2FA = ref(false) 
const otpCode = ref('')         

const loginForm = ref({
  userId: '',
  password: ''
})

const rules = {
  required: (value: string) => !!value || '필수 입력 항목입니다',
  otpLength: (value: string) => value.length === 6 || '6자리 코드를 입력하세요'  
}

const handleLogin = async () => {
  if (!formRef.value) return
  const { valid: isValid } = await formRef.value.validate()
  if (!isValid) return

  // 2FA 필요 시 OTP 코드 포함
  const loginData = {
    ...loginForm.value,
    otpCode: requires2FA.value ? otpCode.value : undefined
  }

  const result = await authStore.login(loginData)
  
  // 2FA 필요 응답 처리
  if (result === '2FA_REQUIRED') {
    requires2FA.value = true
    authStore.error = null  // 에러 메시지 숨김
    return
  }
  
  if (result === true) {
    router.push('/dashboard')
  }
}
</script>

<style scoped>
.fill-height {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
</style>