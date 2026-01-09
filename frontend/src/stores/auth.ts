import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, userApi } from '@/api'
import type { User, LoginRequest, SignupRequest } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  // State
  const user = ref<User | null>(null)
  const token = ref<string | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Getters
  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  // Actions
  const login = async (credentials: LoginRequest) => {
    loading.value = true
    error.value = null
    try {
      const response = await authApi.login(credentials)
      const data = response.data
      // 토큰 및 사용자 정보 저장
      token.value = data.token
      localStorage.setItem('token', data.token)
      // 프로필 정보 가져오기
      await fetchProfile()
      return true
    } catch (err: any) {
      console.log('로그인 에러 응답:', err.response)
      console.log('에러 데이터:', err.response?.data)
      console.log('에러 코드:', err.response?.data?.code)
      
      // 2FA 필요 응답 처리
      const errorCode = err.response?.data?.code
      if (errorCode === '2FA_REQUIRED') {
        return '2FA_REQUIRED'
      }
      
      // 에러 메시지 추출 (다양한 응답 구조 지원)
      const errorData = err.response?.data
      const errorObj = errorData?.error  // { code: "A005", message: "...", detail: "..." }

      // detail이 있으면 detail 표시, 없으면 message 표시
      error.value = errorObj?.detail     // "허용되지 않은 IP입니다. 등록된 IP: xxx"
        || errorObj?.message             // "아이디 또는 비밀번호가 일치하지 않습니다."
        || errorData?.message
        || errorData?.error
        || '로그인에 실패했습니다'

      return false
    } finally {
      loading.value = false
    }
  }

  const signup = async (data: SignupRequest) => {
    loading.value = true
    error.value = null

    try {
      const response = await authApi.signup(data)
      const result = response.data

      // 회원가입 후 자동 로그인
      token.value = result.token
      localStorage.setItem('token', result.token)

      await fetchProfile()

      return true
    } catch (err: any) {
      error.value = err.response?.data?.error || '회원가입에 실패했습니다'
      return false
    } finally {
      loading.value = false
    }
  }

  const fetchProfile = async () => {
    try {
      const response = await userApi.getProfile()
      user.value = response.data
      localStorage.setItem('user', JSON.stringify(response.data))
    } catch (err: any) {
      console.error('프로필 조회 실패:', err)
      logout()
    }
  }

  const logout = () => {
    user.value = null
    token.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  const initAuth = () => {
    // 로컬스토리지에서 토큰 및 사용자 정보 복원
    const savedToken = localStorage.getItem('token')
    const savedUser = localStorage.getItem('user')

    if (savedToken && savedUser) {
      token.value = savedToken
      user.value = JSON.parse(savedUser)
    }
  }

  return {
    user,
    token,
    loading,
    error,
    isAuthenticated,
    isAdmin,
    login,
    signup,
    fetchProfile,
    logout,
    initAuth
  }
})