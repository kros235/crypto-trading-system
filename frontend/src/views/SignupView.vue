<template>
  <v-container class="fill-height" fluid>
    <v-row align="center" justify="center">
      <v-col cols="12" sm="8" md="5">
        <v-card class="elevation-12">
          <v-toolbar color="primary" dark flat>
            <v-toolbar-title>회원가입</v-toolbar-title>
          </v-toolbar>

          <v-card-text>
            <v-alert
              v-if="authStore.error"
              type="error"
              dismissible
              class="mb-4"
            >
              {{ authStore.error }}
            </v-alert>

            <v-form ref="formRef" v-model="valid" @submit.prevent="handleSignup">
              <v-text-field
                v-model="signupForm.userId"
                label="사용자 ID"
                hint="4-20자, 영문/숫자/언더스코어만 가능"
                prepend-icon="mdi-account"
                :rules="[rules.required, rules.userId]"
                variant="outlined"
                class="mb-2"
              />

              <v-text-field
                v-model="signupForm.email"
                label="이메일"
                prepend-icon="mdi-email"
                :rules="[rules.required, rules.email]"
                variant="outlined"
                class="mb-2"
              />

              <v-text-field
                v-model="signupForm.password"
                label="비밀번호"
                hint="8-30자, 대소문자/숫자/특수문자 포함"
                prepend-icon="mdi-lock"
                :type="showPassword ? 'text' : 'password'"
                :append-icon="showPassword ? 'mdi-eye' : 'mdi-eye-off'"
                @click:append="showPassword = !showPassword"
                :rules="[rules.required, rules.password]"
                variant="outlined"
                class="mb-2"
              />

              <v-text-field
                v-model="passwordConfirm"
                label="비밀번호 확인"
                prepend-icon="mdi-lock-check"
                :type="showPasswordConfirm ? 'text' : 'password'"
                :append-icon="showPasswordConfirm ? 'mdi-eye' : 'mdi-eye-off'"
                @click:append="showPasswordConfirm = !showPasswordConfirm"
                :rules="[rules.required, rules.passwordMatch]"
                variant="outlined"
                class="mb-2"
              />

              <v-text-field
                v-model="signupForm.phone"
                label="전화번호 (선택)"
                placeholder="010-XXXX-XXXX"
                prepend-icon="mdi-phone"
                :rules="[rules.phone]"
                variant="outlined"
                class="mb-2"
              />

              <v-btn
                type="submit"
                color="primary"
                block
                size="large"
                :loading="authStore.loading"
                :disabled="!valid"
                class="mt-4"
              >
                가입하기
              </v-btn>
            </v-form>
          </v-card-text>

          <v-divider />

          <v-card-actions>
            <v-spacer />
            <v-btn
              text
              color="primary"
              @click="$router.push('/login')"
            >
              로그인으로 돌아가기
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
const showPasswordConfirm = ref(false)
const passwordConfirm = ref('')

const signupForm = ref({
  userId: '',
  email: '',
  password: '',
  phone: ''
})

const rules = {
  required: (value: string) => !!value || '필수 입력 항목입니다',
  userId: (value: string) => {
    const pattern = /^[a-zA-Z0-9_]{4,20}$/
    return pattern.test(value) || '4-20자의 영문, 숫자, 언더스코어만 가능합니다'
  },
  email: (value: string) => {
    const pattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return pattern.test(value) || '올바른 이메일 형식이 아닙니다'
  },
  password: (value: string) => {
    const pattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#])[A-Za-z\d@$!%*?&#]{8,30}$/
    return pattern.test(value) || '8-30자, 대소문자/숫자/특수문자를 포함해야 합니다'
  },
  passwordMatch: (value: string) => {
    return value === signupForm.value.password || '비밀번호가 일치하지 않습니다'
  },
  phone: (value: string) => {
    if (!value) return true
    const pattern = /^010-\d{4}-\d{4}$/
    return pattern.test(value) || '올바른 전화번호 형식이 아닙니다 (010-XXXX-XXXX)'
  }
}

const handleSignup = async () => {
  if (!formRef.value) return

  const { valid: isValid } = await formRef.value.validate()
  if (!isValid) return

  const success = await authStore.signup(signupForm.value)

  if (success) {
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