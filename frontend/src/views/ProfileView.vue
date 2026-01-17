<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />
    
    <v-main class="bg-grey-lighten-3">
      <v-container fluid>
        <v-row class="mb-4">
          <v-col>
            <h1 class="text-h4">
              <v-icon class="mr-2">mdi-account-cog</v-icon>
              프로필 설정
            </h1>
            <p class="text-subtitle-1 text-grey">계정 정보 및 알림 설정을 관리하세요</p>
          </v-col>
        </v-row>

        <!-- 기본정보 + (비밀번호 + Discord) 레이아웃  -->
        <v-row class="equal-height-row">
          <!-- 왼쪽: 기본 정보 카드 -->
          <v-col cols="12" md="6" class="d-flex">
            <v-card class="flex-grow-1">
              <v-card-title class="bg-primary text-white d-flex align-center">
                <v-icon icon="mdi-account-circle" class="mr-2" />
                기본 정보
                <v-spacer />
                <HelpButton
  	    :useDialog="true"
                  :dialogTitle="helpContents.profile.title"
                  :dialogContent="helpContents.profile.content"
                  iconColor="white"
                  size="24"
                />
              </v-card-title>

              <v-card-text class="pt-10 pb-5">
                <v-alert
                  v-if="profileMessage"
                  :type="profileMessageType"
                  dismissible
                  class="mb-5"
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
                    class="mb-4"
                  />

                  <v-text-field
                    v-model="profileForm.email"
                    label="이메일"
                    prepend-icon="mdi-email"
                    :rules="[rules.required, rules.email]"
                    variant="outlined"
                    class="mb-4"
                  />

                  <v-text-field
                    v-model="profileForm.phone"
                    label="전화번호"
                    prepend-icon="mdi-phone"
                    :rules="[rules.phone]"
                    placeholder="010-XXXX-XXXX"
                    variant="outlined"
                    class="mb-4"
                  />

                  <v-text-field
                    v-model="profileForm.role"
                    label="역할"
                    prepend-icon="mdi-shield-account"
                    readonly
                    variant="outlined"
                    class="mb-4"
                  />

                  <v-text-field
                    v-model="formattedJoinDate"
                    label="가입일"
                    prepend-icon="mdi-calendar"
                    readonly
                    variant="outlined"
                    class="mb-4"
                  />

                  <v-text-field
                    v-model="formattedLastLogin"
                    label="마지막 로그인"
                    prepend-icon="mdi-clock-outline"
                    readonly
                    variant="outlined"
                    class="mb-6"
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
              <v-card-title class="bg-secondary text-white d-flex align-center">
                <v-icon icon="mdi-lock-reset" class="mr-2" />
                비밀번호 변경
                <v-spacer />
                <HelpButton
                  :useDialog="true"
                  :dialogTitle="helpContents.password.title"
                  :dialogContent="helpContents.password.content"
                  iconColor="white"
                  size="28"
                />
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
              <v-card-title class="bg-deep-purple text-white d-flex align-center">
              <v-icon icon="mdi-robot" class="mr-2" />
              Discord DM 알림 설정
                <v-spacer />
                <HelpButton
                  :useDialog="true"
                  :dialogTitle="helpContents.discord.title"
                  :dialogContent="helpContents.discord.content"
                  iconColor="white"
                  size="28"
                />
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
                    <strong>Discord User ID 확인 방법</strong>
                    <a 
                      href="https://support.discord.com/hc/ko/articles/206346498" 
                      target="_blank" 
                      class="ml-2 text-primary"
                      style="font-size: 12px;"
                    >
                      (공식 가이드 보기 ↗)
                    </a>
                    <ol style="margin: 8px 0 0 0; padding-left: 20px; line-height: 1.8;">
                      <li>Discord 설정(⚙️) 클릭</li>
                      <li>고급 → <strong>개발자 모드</strong> ON</li>
                      <li>자신의 프로필 우클릭</li>
                      <li><strong>ID 복사</strong> 클릭</li>
                    </ol>
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
              <v-card-title class="bg-success text-white d-flex align-center">
                <v-icon icon="mdi-key-variant" class="mr-2" />
                업비트 API 키 설정
                <v-spacer />
                 <HelpButton
                  :useDialog="true"
                  :dialogTitle="helpContents.apiKey.title"
                  :dialogContent="helpContents.apiKey.content"
                  iconColor="white"
                  size="28"
                />
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

                <!-- API 키 발급 가이드 -->
                <v-alert
                  type="info"
                  variant="tonal"
                  class="mt-4"
                  icon="mdi-lightbulb-on"
                >
                  <div class="text-body-2 text-black">
                    <strong>API 키 발급 가이드</strong>
                    <ol class="mt-2 mb-0">
                      <li>
                        <a href="https://upbit.com/mypage/open_api_management" target="_blank" class="text-primary font-weight-bold">
                          업비트 Open API 관리 페이지
                        </a> 접속
                      </li>
                      <li>"Open API 키 발급" 버튼 클릭</li>
                      <li>권한 선택: <strong>자산 조회, 주문 조회, 주문</strong> (출금 권한 제외!)</li>
                      <li>IP 설정 후 발급받은 키를 위 입력란에 붙여넣기</li>
                    </ol>
                  </div>
                </v-alert>

                <v-alert
                  type="warning"
                  class="mt-3"
                  icon="mdi-shield-alert"
                >
                  <div class="text-body-2">
                    <strong>API 키 보안 주의사항</strong>
                    <ul class="mt-2 mb-0">
                      <li>API 키는 AES-256 암호화되어 안전하게 저장됩니다</li>
                      <li>API 키를 타인과 절대 공유하지 마세요</li>
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
import HelpButton from '@/components/HelpButton.vue'
import { notificationApi } from '@/api'



const authStore = useAuthStore()

// ⭐ 도움말 내용 추가
const helpContents = {
  profile: {
    title: '기본 정보 안내',
    content: `
      <p><strong>사용자 ID</strong></p>
      <p style="padding-left: 20px; margin-top: 4px;">로그인에 사용되는 고유 식별자입니다. 변경할 수 없습니다.</p>
      <p><strong>이메일</strong></p>
      <p style="padding-left: 20px; margin-top: 4px;">시스템 알림 및 일일 리포트를 받을 이메일 주소입니다.</p>
      <p><strong>전화번호</strong></p>
      <p style="padding-left: 20px; margin-top: 4px;">선택 사항이며, 010-XXXX-XXXX 형식으로 입력합니다.</p>
      <p style="margin-top: 16px;">💡 이메일 주소를 정확히 입력해야 거래 알림을 받을 수 있습니다.</p>
    `
  },
  password: {
    title: '비밀번호 변경 안내',
    content: `
      <p><strong>비밀번호 요구사항:</strong></p>
      <ul style="padding-left: 40px; margin-top: 8px;">
        <li>8~30자 길이</li>
        <li>대문자 1개 이상</li>
        <li>소문자 1개 이상</li>
        <li>숫자 1개 이상</li>
        <li>특수문자(@$!%*?&) 1개 이상</li>
      </ul>
      <p style="margin-top: 16px;">⚠️ 비밀번호 변경 후 다시 로그인해야 할 수 있습니다.</p>
    `
  },
  discord: {
    title: 'Discord DM 알림 설정 안내',
    content: `
      <p><strong>Discord User ID 확인 방법:</strong></p>
      <ol style="padding-left: 40px; margin-top: 8px;">
        <li>Discord 앱 → 설정(톱니바퀴) 클릭</li>
        <li>고급 → <strong>개발자 모드</strong> 활성화</li>
        <li>자신의 프로필 우클릭 → <strong>ID 복사</strong></li>
      </ol>
      <p style="margin-top: 16px;"><strong>알림 종류:</strong></p>
      <ul style="padding-left: 40px; margin-top: 8px;">
        <li>매수/매도 체결 알림</li>
        <li>손절매 발생 알림</li>
        <li>일일 리포트</li>
        <li>AI 가중치 변경 알림</li>
      </ul>
      <p style="margin-top: 16px;">💡 Bot이 활성화되어 있어야 DM을 받을 수 있습니다.</p>
    `
  },
  apiKey: {
    title: '업비트 API 키 설정 안내',
    content: `
      <p><strong>API 키 발급 방법:</strong></p>
      <ol style="padding-left: 40px; margin-top: 8px;">
        <li><a href="https://upbit.com/mypage/open_api_management" target="_blank" style="color: #1976D2;">업비트 Open API 관리</a> 페이지 접속</li>
        <li>"Open API 키 발급" 클릭</li>
        <li><strong>필수 권한</strong>: 자산 조회, 주문 조회, 주문</li>
        <li><strong>IP 설정</strong>: 서버 IP 입력 또는 "모든 IP 허용"</li>
        <li>발급된 Access Key와 Secret Key 복사</li>
      </ol>
      <p style="margin-top: 16px;">⚠️ <strong>절대 출금 권한은 부여하지 마세요!</strong></p>
      <table style="width: 100%; border-collapse: collapse; margin-top: 12px; font-size: 14px;">
        <tr style="background-color: #E3F2FD;">
          <th style="padding: 8px 12px; border: 1px solid #ddd; text-align: left;">권한</th>
          <th style="padding: 8px 12px; border: 1px solid #ddd; text-align: left;">필요 여부</th>
        </tr>
        <tr>
          <td style="padding: 8px 12px; border: 1px solid #ddd;">자산 조회</td>
          <td style="padding: 8px 12px; border: 1px solid #ddd;">✅ 필수</td>
        </tr>
        <tr>
          <td style="padding: 8px 12px; border: 1px solid #ddd;">주문 조회</td>
          <td style="padding: 8px 12px; border: 1px solid #ddd;">✅ 필수</td>
        </tr>
        <tr>
          <td style="padding: 8px 12px; border: 1px solid #ddd;">주문</td>
          <td style="padding: 8px 12px; border: 1px solid #ddd;">✅ 필수</td>
        </tr>
        <tr>
          <td style="padding: 8px 12px; border: 1px solid #ddd;">출금</td>
          <td style="padding: 8px 12px; border: 1px solid #ddd;">❌ 절대 불가</td>
        </tr>
      </table>
    `
  }
}

// Discord 관련 상태 
const discordLoading = ref(false)
const discordTestLoading = ref(false)
const discordMessage = ref('')
const discordMessageType = ref<'success' | 'error' | 'info'>('info')
const discordBotEnabled = ref(false)
const testDailyReportLoading = ref(false)
const testBuyLoading = ref(false)
const testSellLoading = ref(false)
const testStopLossLoading = ref(false)

// Sidebar Ref 추가
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

/* 도움말 테이블 스타일 */
:deep(.help-table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
}

:deep(.help-table th),
:deep(.help-table td) {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}

:deep(.help-table th) {
  background-color: #f5f5f5;
}
</style>