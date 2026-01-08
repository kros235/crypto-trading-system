<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />
    
    <v-main>
      <v-container>
        <v-row>
          <v-col cols="12">
            <h1 class="text-h4 mb-6">프로필 설정</h1>
          </v-col>
        </v-row>

        <!-- ★★★ 변경: 기본정보 + (비밀번호 + Discord) 레이아웃 ★★★ -->
        <v-row class="equal-height-row">
          <!-- 왼쪽: 기본 정보 카드 -->
          <v-col cols="12" md="6" class="d-flex">
            <v-card class="flex-grow-1">
              <v-card-title class="bg-primary text-white">
                <v-icon icon="mdi-account-circle" class="mr-2" />
                기본 정보
              </v-card-title>

              <v-card-text class="pt-4">
                <v-alert
                  v-if="profileMessage"
                  :type="profileMessageType"
                  dismissible
                  class="mb-4"
                  @click:close="profileMessage = ''"
                >
                  {{ profileMessage }}
                </v-alert>

                <v-form ref="profileFormRef" v-model="profileValid">
                  <v-text-field
                    v-model="profileForm.userId"
                    label="사용자 ID"
                    prepend-icon="mdi-account"
                    readonly
                    variant="outlined"
                    class="mb-2"
                  />

                  <v-text-field
                    v-model="profileForm.email"
                    label="이메일"
                    prepend-icon="mdi-email"
                    :rules="[rules.required, rules.email]"
                    variant="outlined"
                    class="mb-2"
                  />

                  <v-text-field
                    v-model="profileForm.phone"
                    label="전화번호"
                    prepend-icon="mdi-phone"
                    :rules="[rules.phone]"
                    placeholder="010-XXXX-XXXX"
                    variant="outlined"
                    class="mb-2"
                  />

                  <v-text-field
                    v-model="profileForm.role"
                    label="역할"
                    prepend-icon="mdi-shield-account"
                    readonly
                    variant="outlined"
                    class="mb-2"
                  />

                  <v-text-field
                    v-model="formattedJoinDate"
                    label="가입일"
                    prepend-icon="mdi-calendar"
                    readonly
                    variant="outlined"
                    class="mb-2"
                  />

                  <v-text-field
                    v-model="formattedLastLogin"
                    label="마지막 로그인"
                    prepend-icon="mdi-clock-outline"
                    readonly
                    variant="outlined"
                    class="mb-4"
                  />

                  <v-btn
                    color="primary"
                    block
                    size="large"
                    :loading="profileLoading"
                    :disabled="!profileValid"
                    @click="updateProfile"
                  >
                    프로필 업데이트
                  </v-btn>
                </v-form>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- 오른쪽: 비밀번호 변경 + Discord DM 알림 설정 (세로 배치) -->
          <v-col cols="12" md="6" class="d-flex flex-column">
            <!-- 비밀번호 변경 카드 -->
            <v-card class="mb-4">
              <v-card-title class="bg-secondary text-white">
                <v-icon icon="mdi-lock-reset" class="mr-2" />
                비밀번호 변경
              </v-card-title>

              <v-card-text class="pt-4">
                <v-alert
                  v-if="passwordMessage"
                  :type="passwordMessageType"
                  dismissible
                  class="mb-4"
                  @click:close="passwordMessage = ''"
                >
                  {{ passwordMessage }}
                </v-alert>

                <v-form ref="passwordFormRef" v-model="passwordValid">
                  <v-text-field
                    v-model="passwordForm.currentPassword"
                    label="현재 비밀번호"
                    prepend-icon="mdi-lock"
                    :type="showCurrentPassword ? 'text' : 'password'"
                    :append-icon="showCurrentPassword ? 'mdi-eye' : 'mdi-eye-off'"
                    @click:append="showCurrentPassword = !showCurrentPassword"
                    :rules="[rules.required]"
                    variant="outlined"
                    density="compact"
                    class="mb-2"
                  />

                  <v-text-field
                    v-model="passwordForm.newPassword"
                    label="새 비밀번호"
                    prepend-icon="mdi-lock-plus"
                    :type="showNewPassword ? 'text' : 'password'"
                    :append-icon="showNewPassword ? 'mdi-eye' : 'mdi-eye-off'"
                    @click:append="showNewPassword = !showNewPassword"
                    :rules="[rules.required, rules.password]"
                    hint="8-30자, 대소문자, 숫자, 특수문자 포함"
                    variant="outlined"
                    density="compact"
                    class="mb-2"
                  />

                  <v-text-field
                    v-model="passwordForm.confirmPassword"
                    label="새 비밀번호 확인"
                    prepend-icon="mdi-lock-check"
                    :type="showConfirmPassword ? 'text' : 'password'"
                    :append-icon="showConfirmPassword ? 'mdi-eye' : 'mdi-eye-off'"
                    @click:append="showConfirmPassword = !showConfirmPassword"
                    :rules="[rules.required, rules.passwordMatch]"
                    variant="outlined"
                    density="compact"
                    class="mb-3"
                  />

                  <v-btn
                    color="secondary"
                    block
                    :loading="passwordLoading"
                    :disabled="!passwordValid"
                    @click="changePassword"
                  >
                    비밀번호 변경
                  </v-btn>
                </v-form>
              </v-card-text>
            </v-card>

            <!-- Discord DM 알림 설정 카드 -->
            <v-card class="flex-grow-1">
              <v-card-title class="bg-deep-purple text-white">
                <v-icon icon="mdi-discord" class="mr-2" />
                Discord DM 알림 설정
              </v-card-title>

              <v-card-text class="pt-4">
                <v-alert
                  v-if="discordMessage"
                  :type="discordMessageType"
                  dismissible
                  class="mb-3"
                  @click:close="discordMessage = ''"
                >
                  {{ discordMessage }}
                </v-alert>

                <v-alert type="info" variant="tonal" density="compact" class="mb-3">
                  <div class="text-body-2">
                    <strong>Discord User ID 확인:</strong>
                    설정 → 고급 → 개발자 모드 ON → 프로필 우클릭 → ID 복사
                  </div>
                </v-alert>

                <v-chip 
                  :color="discordBotEnabled ? 'success' : 'grey'" 
                  size="small" 
                  class="mb-3"
                >
                  {{ discordBotEnabled ? 'Bot 활성화됨' : 'Bot 비활성화' }}
                </v-chip>

                <v-text-field
                  v-model="profileForm.discordUserId"
                  label="Discord User ID"
                  prepend-icon="mdi-identifier"
                  :rules="[rules.discordUserId]"
                  placeholder="예: 123456789012345678"
                  hint="17-20자리 숫자"
                  variant="outlined"
                  density="compact"
                  class="mb-3"
                />

                <div class="d-flex gap-2 mb-3">
                  <v-btn
                    color="deep-purple"
                    :loading="discordLoading"
                    @click="saveDiscordUserId"
                  >
                    <v-icon icon="mdi-content-save" class="mr-1" />
                    저장
                  </v-btn>

                  <v-btn
                    color="grey"
                    variant="outlined"
                    :loading="discordTestLoading"
                    :disabled="!profileForm.discordUserId || !discordBotEnabled"
                    @click="testDiscordDM"
                  >
                    <v-icon icon="mdi-send" class="mr-1" />
                    연동 테스트
                  </v-btn>
                </div>

                <v-divider class="mb-3" />

                <v-alert type="info" variant="tonal" density="compact">
                  알림 유형별 테스트는 
                  <router-link to="/bot-monitor" class="text-decoration-underline">봇 모니터링</router-link> 
                  페이지에서 이용하실 수 있습니다.
                </v-alert>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>     

        <!-- 업비트 API 키 설정 카드 -->
        <v-row>
          <v-col cols="12">
            <v-card>
              <v-card-title class="bg-success text-white">
                <v-icon icon="mdi-key-variant" class="mr-2" />
                업비트 API 키 설정
              </v-card-title>

              <v-card-text class="pt-4">
                <v-alert
                  v-if="apiKeyMessage"
                  :type="apiKeyMessageType"
                  dismissible
                  class="mb-4"
                  @click:close="apiKeyMessage = ''"
                >
                  {{ apiKeyMessage }}
                </v-alert>

                <v-alert
                  :type="authStore.user?.hasApiKey ? 'success' : 'warning'"
                  class="mb-4"
                  prominent
                >
                  <div class="d-flex align-center">
                    <div>
                      <div class="text-h6">
                        {{ authStore.user?.hasApiKey ? 'API 키가 등록되어 있습니다' : 'API 키가 등록되지 않았습니다' }}
                      </div>
                      <div class="text-body-2">
                        {{ authStore.user?.hasApiKey ? '자동매매 기능을 사용할 수 있습니다' : '자동매매를 사용하려면 API 키를 등록해주세요' }}
                      </div>
                    </div>
                  </div>
                </v-alert>

                <v-form ref="apiKeyFormRef" v-model="apiKeyValid">
                  <v-text-field
                    v-model="apiKeyForm.accessKey"
                    label="Access Key"
                    prepend-icon="mdi-key"
                    :rules="[rules.required]"
                    variant="outlined"
                    class="mb-2"
                    hint="업비트에서 발급받은 Access Key를 입력하세요"
                  />

                  <v-text-field
                    v-model="apiKeyForm.secretKey"
                    label="Secret Key"
                    prepend-icon="mdi-key-variant"
                    :type="showSecretKey ? 'text' : 'password'"
                    :append-icon="showSecretKey ? 'mdi-eye' : 'mdi-eye-off'"
                    @click:append="showSecretKey = !showSecretKey"
                    :rules="[rules.required]"
                    variant="outlined"
                    class="mb-4"
                    hint="업비트에서 발급받은 Secret Key를 입력하세요"
                  />

                  <div class="d-flex gap-2">
                    <v-btn
                      color="success"
                      :loading="apiKeyLoading"
                      :disabled="!apiKeyValid"
                      @click="saveApiKeys"
                    >
                      <v-icon icon="mdi-content-save" class="mr-2" />
                      API 키 저장
                    </v-btn>

                    <v-btn
                      color="error"
                      :disabled="!authStore.user?.hasApiKey"
                      :loading="apiKeyDeleteLoading"
                      @click="confirmDeleteApiKeys"
                    >
                      <v-icon icon="mdi-delete" class="mr-2" />
                      API 키 삭제
                    </v-btn>
                  </div>
                </v-form>

                <v-alert
                  type="info"
                  class="mt-4"
                  icon="mdi-information"
                >
                  <div class="text-body-2">
                    <strong>⚠️ API 키 보안 주의사항</strong>
                    <ul class="mt-2">
                      <li>API 키는 AES-256 암호화되어 안전하게 저장됩니다</li>
                      <li>API 키를 타인과 절대 공유하지 마세요</li>
                      <li>업비트에서 IP 화이트리스트 설정을 권장합니다</li>
                      <li>출금 권한은 부여하지 마세요 (자산 조회, 주문 권한만 부여)</li>
                    </ul>
                  </div>
                </v-alert>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>   

        <!-- API 키 삭제 확인 다이얼로그 -->
        <v-dialog v-model="deleteDialog" max-width="400">
          <v-card>
            <v-card-title class="bg-error text-white">
              <v-icon icon="mdi-alert" class="mr-2" />
              API 키 삭제 확인
            </v-card-title>

            <v-card-text class="pt-4">
              <p class="text-body-1">
                정말로 API 키를 삭제하시겠습니까?
              </p>
              <p class="text-body-2 text-grey">
                삭제하면 자동매매 기능을 사용할 수 없게 됩니다.
              </p>
            </v-card-text>

            <v-card-actions>
              <v-spacer />
              <v-btn
                color="grey"
                variant="text"
                @click="deleteDialog = false"
              >
                취소
              </v-btn>
              <v-btn
                color="error"
                variant="elevated"
                @click="deleteApiKeys"
              >
                삭제
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>

        <!-- 업비트 API 키 설정 + IP 화이트리스트 카드 (세로 배치) -->
        <v-row>
          <v-col cols="12">
            <!-- IP 화이트리스트 카드 -->
            <v-card>
              <v-card-title class="bg-warning text-white">
                <v-icon icon="mdi-ip-network" class="mr-2" />
                IP 화이트리스트
                <v-chip 
                  v-if="ipWhitelistEnabled" 
                  color="success" 
                  size="small" 
                  class="ml-2"
                >
                  활성화
                </v-chip>
                <v-chip v-else color="grey" size="small" class="ml-2">
                  비활성화
                </v-chip>
              </v-card-title>

              <v-card-text class="pt-4">
                <v-alert
                  v-if="ipMessage"
                  :type="ipMessageType"
                  dismissible
                  class="mb-4"
                  @click:close="ipMessage = ''"
                >
                  {{ ipMessage }}
                </v-alert>

                <!-- ★★★ 변경: 상태 표시 alert 삭제됨 ★★★ -->

                <v-alert type="info" variant="tonal" class="mb-4">
                  <p class="text-body-2 mb-1">
                    <strong>IP 화이트리스트</strong>를 활성화하면 등록된 IP에서만 로그인할 수 있습니다.
                  </p>
                  <p class="text-body-2 mb-0">
                    현재 접속 IP: <strong>{{ currentIp || '확인 중...' }}</strong>
                  </p>
                </v-alert>

                <!-- 등록된 IP 목록 -->
                <div v-if="allowedIps.length > 0" class="mb-4">
                  <p class="text-subtitle-2 mb-2">등록된 IP ({{ allowedIps.length }}/3)</p>
                  <v-chip
                    v-for="ip in allowedIps"
                    :key="ip"
                    closable
                    class="mr-2 mb-2"
                    color="primary"
                    @click:close="removeIp(ip)"
                  >
                    {{ ip }}
                  </v-chip>
                </div>

                <!-- ★★★ 변경: IP 추가 폼 + 비활성화 버튼 한 줄 배치 ★★★ -->
                <div class="ip-add-row">
                  <v-text-field
                    v-if="allowedIps.length < 3"
                    v-model="newIp"
                    label="IP 주소"
                    placeholder="예: 123.456.789.012"
                    prepend-icon="mdi-ip"
                    variant="outlined"
                    density="compact"
                    :disabled="ipLoading"
                    hide-details
                    class="ip-input"
                  />
                  <v-btn
                    v-if="allowedIps.length < 3"
                    color="primary"
                    :loading="ipLoading"
                    @click="addIp"
                  >
                    IP 추가
                  </v-btn>
                  <v-btn
                    v-if="allowedIps.length < 3"
                    color="secondary"
                    variant="outlined"
                    :loading="ipLoading"
                    @click="addCurrentIp"
                  >
                    현재 IP 추가
                  </v-btn>
                  <!-- ★★★ 변경: 비활성화 버튼 위치 이동 및 스타일 변경 ★★★ -->
                  <v-btn
                    v-if="ipWhitelistEnabled"
                    color="error"
                    variant="outlined"
                    :loading="ipLoading"
                    @click="disableIpWhitelist"
                  >
                    화이트리스트 비활성화
                  </v-btn>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

      </v-container>
    </v-main>
  </v-app>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { userApi } from '@/api'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'
import { notificationApi } from '@/api'

const authStore = useAuthStore()

// ★★★ 추가: Discord 관련 상태 ★★★
const discordLoading = ref(false)
const discordTestLoading = ref(false)
const discordMessage = ref('')
const discordMessageType = ref<'success' | 'error' | 'info'>('info')
const discordBotEnabled = ref(false)
const testDailyReportLoading = ref(false)
const testBuyLoading = ref(false)
const testSellLoading = ref(false)
const testStopLossLoading = ref(false)

// IP 화이트리스트 상태 추가
const ipLoading = ref(false)
const ipMessage = ref('')
const ipMessageType = ref<'success' | 'error' | 'info'>('info')
const allowedIps = ref<string[]>([])
const newIp = ref('')
const currentIp = ref('')
const ipWhitelistEnabled = ref(false)

// ⭐ Sidebar Ref 추가
const sidebarRef = ref()

// 폼 Ref
const profileFormRef = ref()
const passwordFormRef = ref()
const apiKeyFormRef = ref()

// 유효성 검증
const profileValid = ref(false)
const passwordValid = ref(false)
const apiKeyValid = ref(false)

// 로딩 상태
const profileLoading = ref(false)
const passwordLoading = ref(false)
const apiKeyLoading = ref(false)
const apiKeyDeleteLoading = ref(false)

// 메시지
const profileMessage = ref('')
const profileMessageType = ref<'success' | 'error'>('success')
const passwordMessage = ref('')
const passwordMessageType = ref<'success' | 'error'>('success')
const apiKeyMessage = ref('')
const apiKeyMessageType = ref<'success' | 'error'>('success')

// 비밀번호 표시 상태
const showCurrentPassword = ref(false)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)
const showSecretKey = ref(false)

// 삭제 확인 다이얼로그
const deleteDialog = ref(false)

// 프로필 폼 데이터
const profileForm = ref({
  userId: '',
  email: '',
  phone: '',
  role: '',
  joinDate: '',
  lastLogin: '',
  discordUserId: ''
})

// 비밀번호 변경 폼 데이터
const passwordForm = ref({
  newPassword: '',
  confirmPassword: ''
})

// API 키 폼 데이터
const apiKeyForm = ref({
  accessKey: '',
  secretKey: ''
})

// 날짜 포맷팅
const formattedJoinDate = computed(() => {
  if (!profileForm.value.joinDate) return ''
  return new Date(profileForm.value.joinDate).toLocaleString('ko-KR')
})

const formattedLastLogin = computed(() => {
  if (!profileForm.value.lastLogin) return ''
  return new Date(profileForm.value.lastLogin).toLocaleString('ko-KR')
})

// 유효성 검증 규칙
const rules = {
  required: (value: string) => !!value || '필수 입력 항목입니다',
  email: (value: string) => {
    const pattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return pattern.test(value) || '올바른 이메일 형식이 아닙니다'
  },
  phone: (value: string) => {
    if (!value) return true // 선택적 필드
    const pattern = /^010-\d{4}-\d{4}$/
    return pattern.test(value) || '올바른 전화번호 형식이 아닙니다 (010-XXXX-XXXX)'
  },
  discordUserId: (value: string) => {
    if (!value) return true
    const pattern = /^\d{17,20}$/
    return pattern.test(value) || 'Discord User ID는 17-20자리 숫자입니다'
  },
  password: (value: string) => {
    const pattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,30}$/
    return pattern.test(value) || '8-30자, 대소문자, 숫자, 특수문자를 포함해야 합니다'
  },
  passwordMatch: (value: string) => {
    return value === passwordForm.value.newPassword || '비밀번호가 일치하지 않습니다'
  }
}

// 프로필 정보 로드
const loadProfile = async () => {
  try {
    const response = await userApi.getProfile()
    const user = response.data

    profileForm.value = {
      userId: user.userId,
      email: user.email,
      phone: user.phone || '',
      role: user.role,
      joinDate: user.joinDate,
      lastLogin: user.lastLogin,
      discordUserId: user.discordUserId || ''
    }

    // authStore도 업데이트
    await authStore.fetchProfile()

    try {
      const botStatus = await notificationApi.getDiscordBotStatus()
      discordBotEnabled.value = botStatus.data.botEnabled
    } catch (e) {
      discordBotEnabled.value = false
    }
  } catch (error: any) {
    profileMessage.value = error.response?.data?.message || '프로필 정보를 불러오는데 실패했습니다'
    profileMessageType.value = 'error'
  }
}

// IP 화이트리스트 메서드 추가

// IP 목록 로드
const loadAllowedIps = async () => {
  try {
    const response = await userApi.getAllowedIps()
    allowedIps.value = response.data.allowedIps || []
    ipWhitelistEnabled.value = allowedIps.value.length > 0
  } catch (error: any) {
    console.error('IP 목록 로드 실패:', error)
  }
}

// 현재 IP 조회
const loadCurrentIp = async () => {
  try {
    const response = await userApi.getCurrentIp()
    currentIp.value = response.data.ip
  } catch (error: any) {
    console.error('현재 IP 조회 실패:', error)
  }
}

// IP 추가
const addIp = async () => {
  if (!newIp.value.trim()) {
    ipMessage.value = 'IP 주소를 입력해주세요'
    ipMessageType.value = 'error'
    return
  }

  ipLoading.value = true
  ipMessage.value = ''

  try {
    const response = await userApi.addAllowedIp(newIp.value.trim())
    allowedIps.value = response.data.allowedIps
    ipWhitelistEnabled.value = true
    newIp.value = ''
    ipMessage.value = 'IP가 추가되었습니다'
    ipMessageType.value = 'success'
  } catch (error: any) {
    ipMessage.value = error.response?.data?.error || 'IP 추가 실패'
    ipMessageType.value = 'error'
  } finally {
    ipLoading.value = false
  }
}

// 현재 IP 추가
const addCurrentIp = async () => {
  if (!currentIp.value) {
    await loadCurrentIp()
  }
  newIp.value = currentIp.value
  await addIp()
}

// IP 삭제
const removeIp = async (ip: string) => {
  ipLoading.value = true
  ipMessage.value = ''

  try {
    const response = await userApi.removeAllowedIp(ip)
    allowedIps.value = response.data.allowedIps
    ipWhitelistEnabled.value = allowedIps.value.length > 0
    ipMessage.value = 'IP가 삭제되었습니다'
    ipMessageType.value = 'success'
  } catch (error: any) {
    ipMessage.value = error.response?.data?.error || 'IP 삭제 실패'
    ipMessageType.value = 'error'
  } finally {
    ipLoading.value = false
  }
}

// IP 화이트리스트 비활성화
const disableIpWhitelist = async () => {
  ipLoading.value = true
  ipMessage.value = ''

  try {
    await userApi.disableIpWhitelist()
    allowedIps.value = []
    ipWhitelistEnabled.value = false
    ipMessage.value = 'IP 화이트리스트가 비활성화되었습니다'
    ipMessageType.value = 'success'
  } catch (error: any) {
    ipMessage.value = error.response?.data?.error || '비활성화 실패'
    ipMessageType.value = 'error'
  } finally {
    ipLoading.value = false
  }
}

// 프로필 업데이트
const updateProfile = async () => {
  if (!profileFormRef.value) return

  const { valid } = await profileFormRef.value.validate()
  if (!valid) return

  profileLoading.value = true
  profileMessage.value = ''

  try {
    await userApi.updateProfile({
      email: profileForm.value.email,
      phone: profileForm.value.phone || undefined
    })

    profileMessage.value = '프로필이 성공적으로 업데이트되었습니다'
    profileMessageType.value = 'success'

    // 프로필 다시 로드
    await loadProfile()
  } catch (error: any) {
    profileMessage.value = error.response?.data?.message || '프로필 업데이트에 실패했습니다'
    profileMessageType.value = 'error'
  } finally {
    profileLoading.value = false
  }
}

// 비밀번호 변경
const changePassword = async () => {
  if (!passwordFormRef.value) return

  const { valid } = await passwordFormRef.value.validate()
  if (!valid) return

  passwordLoading.value = true
  passwordMessage.value = ''

  try {
    await userApi.updateProfile({
      password: passwordForm.value.newPassword
    })

    passwordMessage.value = '비밀번호가 성공적으로 변경되었습니다'
    passwordMessageType.value = 'success'

    // 폼 초기화
    passwordForm.value = {
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
    passwordFormRef.value.reset()
  } catch (error: any) {
    passwordMessage.value = error.response?.data?.message || '비밀번호 변경에 실패했습니다'
    passwordMessageType.value = 'error'
  } finally {
    passwordLoading.value = false
  }
}

// API 키 저장
const saveApiKeys = async () => {
  if (!apiKeyFormRef.value) return

  const { valid } = await apiKeyFormRef.value.validate()
  if (!valid) return

  apiKeyLoading.value = true
  apiKeyMessage.value = ''

  try {
    await userApi.saveApiKeys({
      accessKey: apiKeyForm.value.accessKey,
      secretKey: apiKeyForm.value.secretKey
    })

    apiKeyMessage.value = 'API 키가 안전하게 저장되었습니다'
    apiKeyMessageType.value = 'success'

    // 폼 초기화
    apiKeyForm.value = {
      accessKey: '',
      secretKey: ''
    }
    apiKeyFormRef.value.reset()

    // 프로필 다시 로드 (hasApiKey 업데이트)
    await loadProfile()
  } catch (error: any) {
    apiKeyMessage.value = error.response?.data?.message || 'API 키 저장에 실패했습니다'
    apiKeyMessageType.value = 'error'
  } finally {
    apiKeyLoading.value = false
  }
}

// API 키 삭제 확인
const confirmDeleteApiKeys = () => {
  deleteDialog.value = true
}

// API 키 삭제
const deleteApiKeys = async () => {
  apiKeyDeleteLoading.value = true
  apiKeyMessage.value = ''

  try {
    await userApi.deleteApiKeys()

    apiKeyMessage.value = 'API 키가 삭제되었습니다'
    apiKeyMessageType.value = 'success'

    deleteDialog.value = false

    // 프로필 다시 로드 (hasApiKey 업데이트)
    await loadProfile()
  } catch (error: any) {
    apiKeyMessage.value = error.response?.data?.message || 'API 키 삭제에 실패했습니다'
    apiKeyMessageType.value = 'error'
  } finally {
    apiKeyDeleteLoading.value = false
  }
}

// ★★★ 추가: Discord User ID 저장 ★★★
const saveDiscordUserId = async () => {
  discordLoading.value = true
  discordMessage.value = ''

  try {
    await userApi.updateProfile({
      discordUserId: profileForm.value.discordUserId || ''
    })

    discordMessage.value = 'Discord User ID가 저장되었습니다'
    discordMessageType.value = 'success'
  } catch (error: any) {
    discordMessage.value = error.response?.data?.message || 'Discord User ID 저장에 실패했습니다'
    discordMessageType.value = 'error'
  } finally {
    discordLoading.value = false
  }
}

// ★★★ 추가: Discord DM 테스트 ★★★
const testDiscordDM = async () => {
  if (!profileForm.value.discordUserId) {
    discordMessage.value = 'Discord User ID를 먼저 입력하고 저장해주세요'
    discordMessageType.value = 'error'
    return
  }

  discordTestLoading.value = true
  discordMessage.value = ''

  try {
    const response = await notificationApi.sendTestDiscordDM()
    discordMessage.value = response.data.message
    discordMessageType.value = response.data.success ? 'success' : 'error'
  } catch (error: any) {
    discordMessage.value = error.response?.data?.message || 'DM 테스트에 실패했습니다'
    discordMessageType.value = 'error'
  } finally {
    discordTestLoading.value = false
  }
}

// ★★★ 추가: 일일 리포트 DM 테스트 ★★★
const testDailyReportDM = async () => {
  if (!profileForm.value.discordUserId) {
    discordMessage.value = 'Discord User ID를 먼저 저장해주세요'
    discordMessageType.value = 'error'
    return
  }

  testDailyReportLoading.value = true
  discordMessage.value = ''

  try {
    const response = await notificationApi.testDailyReportDM()
    discordMessage.value = response.data.message
    discordMessageType.value = response.data.success ? 'success' : 'error'
  } catch (error: any) {
    discordMessage.value = error.response?.data?.message || '일일 리포트 DM 테스트 실패'
    discordMessageType.value = 'error'
  } finally {
    testDailyReportLoading.value = false
  }
}

// ★★★ 추가: 매수 알림 DM 테스트 ★★★
const testBuyDM = async () => {
  if (!profileForm.value.discordUserId) {
    discordMessage.value = 'Discord User ID를 먼저 저장해주세요'
    discordMessageType.value = 'error'
    return
  }

  testBuyLoading.value = true
  discordMessage.value = ''

  try {
    const response = await notificationApi.testBuyDM()
    discordMessage.value = response.data.message
    discordMessageType.value = response.data.success ? 'success' : 'error'
  } catch (error: any) {
    discordMessage.value = error.response?.data?.message || '매수 알림 DM 테스트 실패'
    discordMessageType.value = 'error'
  } finally {
    testBuyLoading.value = false
  }
}

// ★★★ 추가: 매도 알림 DM 테스트 ★★★
const testSellDM = async () => {
  if (!profileForm.value.discordUserId) {
    discordMessage.value = 'Discord User ID를 먼저 저장해주세요'
    discordMessageType.value = 'error'
    return
  }

  testSellLoading.value = true
  discordMessage.value = ''

  try {
    const response = await notificationApi.testSellDM()
    discordMessage.value = response.data.message
    discordMessageType.value = response.data.success ? 'success' : 'error'
  } catch (error: any) {
    discordMessage.value = error.response?.data?.message || '매도 알림 DM 테스트 실패'
    discordMessageType.value = 'error'
  } finally {
    testSellLoading.value = false
  }
}

// ★★★ 추가: 손절매 알림 DM 테스트 ★★★
const testStopLossDM = async () => {
  if (!profileForm.value.discordUserId) {
    discordMessage.value = 'Discord User ID를 먼저 저장해주세요'
    discordMessageType.value = 'error'
    return
  }

  testStopLossLoading.value = true
  discordMessage.value = ''

  try {
    const response = await notificationApi.testStopLossDM()
    discordMessage.value = response.data.message
    discordMessageType.value = response.data.success ? 'success' : 'error'
  } catch (error: any) {
    discordMessage.value = error.response?.data?.message || '손절매 알림 DM 테스트 실패'
    discordMessageType.value = 'error'
  } finally {
    testStopLossLoading.value = false
  }
}


// 컴포넌트 마운트 시 프로필 로드
onMounted(() => {
  loadProfile()
  loadAllowedIps()
  loadCurrentIp()
})
</script>

<style scoped>
.gap-2 {
  gap: 8px;
}

/* 높이 맞춤 스타일 */
.equal-height-row {
  align-items: stretch;
}

.equal-height-row > .v-col {
  display: flex;
}

.equal-height-row .v-card {
  width: 100%;
}

/* IP 추가 폼 한 줄 배치 */
.ip-add-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.ip-add-row .ip-input {
  flex: 1;
  min-width: 200px;
  max-width: 400px;
}

@media (max-width: 600px) {
  .ip-add-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .ip-add-row .ip-input {
    max-width: 100%;
  }
  
  .ip-add-row .v-btn {
    width: 100%;
  }
}
</style>