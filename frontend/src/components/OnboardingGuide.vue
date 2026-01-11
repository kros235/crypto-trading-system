<template>
  <v-card 
    v-if="showGuide" 
    class="onboarding-card mb-4" 
    elevation="3"
    color="blue-grey-lighten-5"
  >
    <v-card-title class="bg-indigo-darken-2 text-white py-3 d-flex align-center">
      <v-icon class="mr-2">mdi-rocket-launch</v-icon>
      <span class="text-body-1 font-weight-bold">시작 가이드</span>
      <v-spacer />
      <v-btn 
        icon 
        variant="text" 
        size="small" 
        color="white" 
        @click="hideTemporarily"
        title="닫기"
      >
        <v-icon size="20">mdi-close</v-icon>
      </v-btn>
    </v-card-title>

    <v-card-text class="pa-4">
      <v-row>
        <!-- Step 1: API 키 등록 -->
        <v-col cols="12" md="4">
          <div 
            class="step-item pa-3 rounded-lg cursor-pointer"
            :class="step1Complete ? 'step-complete' : 'step-pending'"
            @click="goToProfile"
          >
            <div class="d-flex align-center mb-2">
              <v-avatar 
                :color="step1Complete ? 'success' : 'grey-lighten-1'" 
                size="32" 
                class="mr-2"
              >
                <v-icon size="18" :color="step1Complete ? 'white' : 'grey'">
                  {{ step1Complete ? 'mdi-check' : 'mdi-numeric-1-circle' }}
                </v-icon>
              </v-avatar>
              <span class="text-body-1 font-weight-bold">API 키 등록</span>
              <v-chip 
                v-if="step1Complete" 
                size="x-small" 
                color="success" 
                class="ml-2"
              >
                완료
              </v-chip>
              <v-chip 
                v-else 
                size="x-small" 
                color="warning" 
                class="ml-2"
              >
                필요
              </v-chip>
            </div>
            <p class="text-body-2 text-grey-darken-1 mb-2">
              업비트에서 API 키를 발급받아 등록하세요.
            </p>
            <a 
              href="https://upbit.com/mypage/open_api_management" 
              target="_blank" 
              class="text-caption text-primary"
              @click.stop
            >
              <v-icon size="12" class="mr-1">mdi-open-in-new</v-icon>
              업비트 API 발급 페이지
            </a>
          </div>
        </v-col>

        <!-- Step 2: 거래 설정 -->
        <v-col cols="12" md="4">
          <div 
            class="step-item pa-3 rounded-lg cursor-pointer"
            :class="step2Complete ? 'step-complete' : (step1Complete ? 'step-pending' : 'step-locked')"
            @click="goToSettings"
          >
            <div class="d-flex align-center mb-2">
              <v-avatar 
                :color="step2Complete ? 'success' : (step1Complete ? 'grey-lighten-1' : 'grey-lighten-2')" 
                size="32" 
                class="mr-2"
              >
                <v-icon size="18" :color="step2Complete ? 'white' : 'grey'">
                  {{ step2Complete ? 'mdi-check' : 'mdi-numeric-2-circle' }}
                </v-icon>
              </v-avatar>
              <span class="text-body-1 font-weight-bold">거래 설정</span>
              <v-chip 
                v-if="step2Complete" 
                size="x-small" 
                color="success" 
                class="ml-2"
              >
                완료
              </v-chip>
              <v-chip 
                v-else-if="step1Complete" 
                size="x-small" 
                color="info" 
                class="ml-2"
              >
                진행 중
              </v-chip>
              <v-chip 
                v-else 
                size="x-small" 
                color="grey" 
                class="ml-2"
              >
                대기
              </v-chip>
            </div>
            <p class="text-body-2 text-grey-darken-1 mb-0">
              거래할 코인과 매수/매도 조건을 설정하세요.
            </p>
          </div>
        </v-col>

        <!-- Step 3: 소액 테스트 -->
        <v-col cols="12" md="4">
          <div 
            class="step-item pa-3 rounded-lg"
            :class="step3Complete ? 'step-complete' : (step2Complete ? 'step-pending' : 'step-locked')"
          >
            <div class="d-flex align-center mb-2">
              <v-avatar 
                :color="step3Complete ? 'success' : (step2Complete ? 'grey-lighten-1' : 'grey-lighten-2')" 
                size="32" 
                class="mr-2"
              >
                <v-icon size="18" :color="step3Complete ? 'white' : 'grey'">
                  {{ step3Complete ? 'mdi-check' : 'mdi-numeric-3-circle' }}
                </v-icon>
              </v-avatar>
              <span class="text-body-1 font-weight-bold">소액 테스트</span>
              <v-chip 
                v-if="step3Complete" 
                size="x-small" 
                color="success" 
                class="ml-2"
              >
                완료
              </v-chip>
              <v-chip 
                v-else-if="step2Complete" 
                size="x-small" 
                color="info" 
                class="ml-2"
              >
                권장
              </v-chip>
              <v-chip 
                v-else 
                size="x-small" 
                color="grey" 
                class="ml-2"
              >
                대기
              </v-chip>
            </div>
            <p class="text-body-2 text-grey-darken-1 mb-0">
              10만원 이하로 1~2일간 테스트 후 본격 운영하세요.
            </p>
          </div>
        </v-col>
      </v-row>

      <!-- 하단 안내 -->
      <v-divider class="my-3" />
      <div class="d-flex align-center justify-space-between">
        <div class="text-caption text-grey-darken-1">
          <v-icon size="14" class="mr-1">mdi-information-outline</v-icon>
          모든 단계를 완료하면 자동매매를 시작할 수 있습니다.
        </div>
        <v-btn 
          variant="text" 
          size="small" 
          color="grey"
          @click="hideForever"
        >
          다시 보지 않기
        </v-btn>
      </div>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  hasApiKey: boolean
  hasSettings: boolean
  hasTransactions: boolean
}>()

const router = useRouter()
const authStore = useAuthStore()

// 사용자별 고유 키 생성
const userId = computed(() => authStore.user?.userId || 'guest')
const storageKey = computed(() => `onboarding_hidden_${userId.value}`)
const sessionKey = computed(() => `onboarding_hidden_temp_${userId.value}`)

const showGuide = ref(true)

// 사용자별 저장소 키
const getStorageKey = (suffix: string) => {
  const userId = authStore.user?.userId || 'guest'
  return `onboarding_${suffix}_${userId}`
}

// 단계 완료 여부
const step1Complete = computed(() => props.hasApiKey)
const step2Complete = computed(() => props.hasSettings)
const step3Complete = computed(() => props.hasTransactions)

// 모든 단계 완료 여부
const allComplete = computed(() => step1Complete.value && step2Complete.value && step3Complete.value)

onMounted(() => {
  // 사용자별 localStorage 키 사용
  const hidden = localStorage.getItem(storageKey.value)
  const hiddenTemp = sessionStorage.getItem(sessionKey.value)
  
  if (hidden === 'true' || hiddenTemp === 'true') {
    showGuide.value = false
  }
  
  // 모든 단계 완료 시 자동 숨김
  if (allComplete.value) {
    showGuide.value = false
  }
})

// 임시로 숨기기 (세션 동안)
const hideTemporarily = () => {
  showGuide.value = false
  // 사용자별 키 사용
  sessionStorage.setItem(sessionKey.value, 'true')
}

// 영구적으로 숨기기
const hideForever = () => {
  showGuide.value = false
  // 사용자별 키 사용
  localStorage.setItem(storageKey.value, 'true')
}

// 프로필 페이지로 이동
const goToProfile = () => {
  router.push('/profile')
}

// 거래 설정 페이지로 이동
const goToSettings = () => {
  if (step1Complete.value) {
    router.push('/trading-settings')
  }
}

// 외부에서 다시 보기 활성화
const resetGuide = () => {
  // 사용자별 키 사용
  localStorage.removeItem(storageKey.value)
  sessionStorage.removeItem(sessionKey.value)
  showGuide.value = true
}

defineExpose({ resetGuide })
</script>

<style scoped>
.onboarding-card {
  border-left: 4px solid #3F51B5;
}

.step-item {
  transition: all 0.2s ease;
  border: 1px solid #e0e0e0;
}

.step-complete {
  background-color: #E8F5E9;
  border-color: #4CAF50;
}

.step-pending {
  background-color: #FFF8E1;
  border-color: #FFC107;
}

.step-pending:hover {
  background-color: #FFF3C4;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.step-locked {
  background-color: #FAFAFA;
  border-color: #BDBDBD;
  opacity: 0.7;
}

.cursor-pointer {
  cursor: pointer;
}
</style>