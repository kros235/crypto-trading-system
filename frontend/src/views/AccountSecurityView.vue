<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />
    
    <v-main class="bg-grey-lighten-3">
    <v-container fluid>
        <v-row class="mb-4">
          <v-col>
            <h1 class="text-h4">
              <v-icon class="mr-2">mdi-shield-account</v-icon>
              계정 보안
            </h1>
            <p class="text-subtitle-1 text-grey">2단계 인증 및 IP 화이트리스트를 관리하세요</p>
          </v-col>
        </v-row>

        <!-- 2FA 설정 카드 -->
        <v-row>
          <v-col cols="12">
            <v-card>
              <v-card-title class="bg-deep-purple text-white d-flex align-center">
                <v-icon icon="mdi-two-factor-authentication" class="mr-2" />
                2단계 인증 (2FA)
                <v-chip 
                  v-if="twoFactorEnabled" 
                  color="grey-lighten-1" 
                  variant="flat"
                  size="small" 
                  class="ml-2 text-black"
                >
                  활성화
                </v-chip>
                <v-chip 
                  v-else 
                  color="grey-lighten-2" 
                  variant="flat"
                  size="small" 
                  class="ml-2 text-black"
                >
                  비활성화
                </v-chip>
                <v-spacer />
                <HelpButton
                  :use-dialog="true"
                  dialog-title="2단계 인증 (2FA) 안내"
                  :dialog-content="helpContents.twoFactor"
                  color="white"
                />
              </v-card-title>

              <v-card-text class="pt-4">
                <v-alert
                  v-if="twoFactorMessage"
                  :type="twoFactorMessageType"
                  dismissible
                  class="mb-4"
                  @click:close="twoFactorMessage = ''"
                >
                  {{ twoFactorMessage }}
                </v-alert>

                <!-- 2FA 비활성화 상태 -->
                <template v-if="!twoFactorEnabled && !setupMode">
                  <v-alert type="info" variant="tonal" class="mb-4">
                    <p class="text-body-2 mb-1">
                      <strong>2단계 인증</strong>을 활성화하면 로그인 시 Google Authenticator 앱의 인증 코드가 필요합니다.
                    </p>
                    <p class="text-body-2 mb-0">
                      계정 보안을 강화하려면 2FA를 활성화하세요.
                    </p>
                  </v-alert>

                  <v-btn
                    color="deep-purple"
                    :loading="twoFactorLoading"
                    @click="startSetup"
                  >
                    <v-icon left>mdi-shield-plus</v-icon>
                    2FA 활성화
                  </v-btn>
                </template>

                <!-- 2FA 설정 모드 -->
                <template v-if="setupMode">
                  <v-alert type="warning" variant="tonal" class="mb-4">
                    <p class="text-body-2 font-weight-bold mb-2">
                      Google Authenticator 앱에서 아래 QR 코드를 스캔하세요.
                    </p>
                    <p class="text-body-2 mb-0">
                      또는 비밀키를 수동으로 입력하세요.
                    </p>
                  </v-alert>

                  <!-- QR 코드 -->
                  <div class="text-center mb-4">
                    <img 
                      :src="qrCodeDataUrl" 
                      alt="QR Code" 
                      style="max-width: 200px; border: 1px solid #ddd; border-radius: 8px;"
                    />
                  </div>

                  <!-- 비밀키 표시 -->
                  <v-text-field
                    v-model="totpSecret"
                    label="비밀키 (수동 입력용)"
                    readonly
                    variant="outlined"
                    density="compact"
                    class="mb-4"
                    append-icon="mdi-content-copy"
                    @click:append="copySecret"
                  />

                  <!-- 확인 코드 입력 -->
                  <v-text-field
                    v-model="verificationCode"
                    label="인증 코드 (6자리)"
                    placeholder="000000"
                    variant="outlined"
                    density="compact"
                    maxlength="6"
                    class="mb-4"
                    prepend-icon="mdi-numeric"
                    :rules="[v => /^\d{6}$/.test(v) || '6자리 숫자를 입력하세요']"
                  />

                  <div class="d-flex gap-2">
                    <v-btn
                      color="success"
                      :loading="twoFactorLoading"
                      :disabled="verificationCode.length !== 6"
                      @click="confirmSetup"
                    >
                      확인 및 활성화
                    </v-btn>
                    <v-btn
                      color="grey"
                      variant="outlined"
                      @click="cancelSetup"
                    >
                      취소
                    </v-btn>
                  </div>
                </template>

                <!-- 2FA 활성화 상태 -->
                <template v-if="twoFactorEnabled && !setupMode">
                  <v-alert type="success" variant="tonal" class="mb-4">
                    <p class="text-body-2 mb-0">
                      2단계 인증이 활성화되어 있습니다. 로그인 시 Google Authenticator 코드가 필요합니다.
                    </p>
                  </v-alert>

                  <v-text-field
                    v-model="disableCode"
                    label="비활성화하려면 인증 코드 입력"
                    placeholder="000000"
                    variant="outlined"
                    density="compact"
                    maxlength="6"
                    class="mb-4"
                    prepend-icon="mdi-numeric"
                  />

                  <v-btn
                    color="error"
                    variant="outlined"
                    :loading="twoFactorLoading"
                    :disabled="disableCode.length !== 6"
                    @click="disable2FA"
                  >
                    <v-icon left>mdi-shield-off</v-icon>
                    2FA 비활성화
                  </v-btn>
                </template>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- IP 화이트리스트 카드 -->
          <v-col cols="12">
            <v-card>
              <v-card-title class="bg-warning text-white d-flex align-center">
                <v-icon icon="mdi-ip-network" class="mr-2" />
                IP 화이트리스트
                <v-chip 
                  v-if="ipWhitelistEnabled" 
                  color="grey-lighten-1" 
                  variant="flat"
                  size="small" 
                  class="ml-2 text-black"
                >
                  활성화
                </v-chip>
                <v-chip 
                  v-else 
                  color="grey-lighten-2" 
                  variant="flat"
                  size="small" 
                  class="ml-2 text-black"
                >
                  비활성화
                </v-chip>
                <v-spacer />
                <HelpButton
                  :use-dialog="true"
                  dialog-title="IP 화이트리스트 안내"
                  :dialog-content="helpContents.ipWhitelist"
                  color="white"
                />
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

                <!-- IP 추가 폼 -->
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

        <!-- 보안 팁 -->
        <v-row class="mt-4">
          <v-col cols="12">
            <v-card>
              <v-card-title class="bg-teal text-white d-flex align-center">
                <v-icon icon="mdi-shield-check" class="mr-2" />
                보안 권장 사항
                <v-spacer />
                <HelpButton
                  :use-dialog="true"
                  dialog-title="계정 보안 강화 가이드"
                  :dialog-content="helpContents.securityTips"
                  color="white"
                />
              </v-card-title>
              <v-card-text class="pt-4">
                <v-list density="compact" bg-color="transparent">
                  <v-list-item>
                    <template v-slot:prepend>
                      <v-icon :color="twoFactorEnabled ? 'success' : 'grey'">
                        {{ twoFactorEnabled ? 'mdi-check-circle' : 'mdi-circle-outline' }}
                      </v-icon>
                    </template>
                    <v-list-item-title>2단계 인증 활성화</v-list-item-title>
                    <v-list-item-subtitle>Google Authenticator로 계정을 보호하세요</v-list-item-subtitle>
                  </v-list-item>
                  <v-list-item>
                    <template v-slot:prepend>
                      <v-icon :color="ipWhitelistEnabled ? 'success' : 'grey'">
                        {{ ipWhitelistEnabled ? 'mdi-check-circle' : 'mdi-circle-outline' }}
                      </v-icon>
                    </template>
                    <v-list-item-title>IP 화이트리스트 설정</v-list-item-title>
                    <v-list-item-subtitle>신뢰할 수 있는 IP에서만 로그인 허용</v-list-item-subtitle>
                  </v-list-item>
                  <v-list-item>
                    <template v-slot:prepend>
                      <v-icon color="info">mdi-key-variant</v-icon>
                    </template>
                    <v-list-item-title>강력한 비밀번호 사용</v-list-item-title>
                    <v-list-item-subtitle>대소문자, 숫자, 특수문자를 포함한 8자 이상</v-list-item-subtitle>
                  </v-list-item>
                </v-list>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { userApi } from '@/api'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'
import HelpButton from '@/components/HelpButton.vue'
import QRCode from 'qrcode'

const sidebarRef = ref()

// 2FA 상태
const twoFactorEnabled = ref(false)
const twoFactorLoading = ref(false)
const twoFactorMessage = ref('')
const twoFactorMessageType = ref<'success' | 'error' | 'info'>('info')
const setupMode = ref(false)
const totpSecret = ref('')
const qrCodeUrl = ref('')
const qrCodeDataUrl = ref('')
const verificationCode = ref('')
const disableCode = ref('')

// IP 화이트리스트 상태
const ipLoading = ref(false)
const ipMessage = ref('')
const ipMessageType = ref<'success' | 'error' | 'info'>('info')
const allowedIps = ref<string[]>([])
const newIp = ref('')
const currentIp = ref('')
const ipWhitelistEnabled = computed(() => allowedIps.value.length > 0)

// 도움말 내용
const helpContents = {
  twoFactor: `
    <h4>🔐 2단계 인증 (2FA)이란?</h4>
    <div style="padding-left: 24px; margin-bottom: 16px;">
      <p>비밀번호 외에 <strong>추가 인증 수단</strong>을 사용하여 계정을 보호하는 방법입니다.</p>
    </div>
    
    <h4>📱 작동 방식</h4>
    <div style="padding-left: 24px; margin-bottom: 16px;">
      <ol style="margin: 0; padding-left: 20px;">
        <li>로그인 시 아이디/비밀번호 입력</li>
        <li>Google Authenticator 앱에서 <strong>6자리 코드</strong> 확인</li>
        <li>30초마다 새로운 코드가 생성됨</li>
        <li>코드 입력 후 로그인 완료</li>
      </ol>
    </div>
    
    <h4>✅ 활성화 방법</h4>
    <div style="padding-left: 24px; margin-bottom: 16px;">
      <ol style="margin: 0; padding-left: 20px;">
        <li>스마트폰에 <strong>Google Authenticator</strong> 앱 설치</li>
        <li>"2FA 활성화" 버튼 클릭</li>
        <li>앱에서 QR 코드 스캔</li>
        <li>앱에 표시된 6자리 코드 입력</li>
      </ol>
    </div>
    
    <h4>⚠️ 주의사항</h4>
    <div style="padding-left: 24px;">
      <ul style="margin: 0; padding-left: 20px;">
        <li>QR 코드 또는 비밀키를 <strong>안전한 곳에 백업</strong>해두세요</li>
        <li>휴대폰 분실 시 계정 복구가 어려울 수 있습니다</li>
        <li>기기 변경 시 반드시 2FA 재설정 필요</li>
      </ul>
    </div>
  `,
  ipWhitelist: `
    <h4>🌐 IP 화이트리스트란?</h4>
    <div style="padding-left: 24px; margin-bottom: 16px;">
      <p><strong>허용된 IP 주소</strong>에서만 로그인할 수 있도록 제한하는 기능입니다.</p>
    </div>
    
    <h4>🏠 사용 예시</h4>
    <div style="padding-left: 24px; margin-bottom: 16px;">
      <table style="width:100%; border-collapse: collapse; margin: 10px 0;">
        <tr style="background: #f5f5f5;">
          <th style="border: 1px solid #ddd; padding: 8px; text-align: left;">상황</th>
          <th style="border: 1px solid #ddd; padding: 8px; text-align: left;">설명</th>
        </tr>
        <tr>
          <td style="border: 1px solid #ddd; padding: 8px;">집에서만 사용</td>
          <td style="border: 1px solid #ddd; padding: 8px;">집 IP 1개만 등록</td>
        </tr>
        <tr>
          <td style="border: 1px solid #ddd; padding: 8px;">집 + 회사</td>
          <td style="border: 1px solid #ddd; padding: 8px;">집 IP + 회사 IP 등록</td>
        </tr>
        <tr>
          <td style="border: 1px solid #ddd; padding: 8px;">이동 중 사용</td>
          <td style="border: 1px solid #ddd; padding: 8px;">비활성화 권장 (IP 수시 변경)</td>
        </tr>
      </table>
    </div>
    
    <h4>📋 설정 방법</h4>
    <div style="padding-left: 24px; margin-bottom: 16px;">
      <ol style="margin: 0; padding-left: 20px;">
        <li>"현재 IP 추가" 버튼으로 지금 접속 중인 IP 등록</li>
        <li>또는 IP 주소 직접 입력 후 "IP 추가"</li>
        <li>최대 <strong>3개</strong>까지 등록 가능</li>
      </ol>
    </div>
    
    <h4>⚠️ 주의사항</h4>
    <div style="padding-left: 24px; margin-bottom: 16px;">
      <ul style="margin: 0; padding-left: 20px;">
        <li>등록되지 않은 IP에서는 <strong>로그인 불가</strong></li>
        <li>IP가 변경되면 접속이 차단될 수 있음</li>
        <li>유동 IP 사용자는 주의 필요</li>
        <li>모바일 데이터는 IP가 자주 바뀌므로 주의</li>
      </ul>
    </div>
    
    <h4>🔓 비활성화</h4>
    <div style="padding-left: 24px;">
      <p>등록된 IP를 모두 삭제하거나 "화이트리스트 비활성화" 버튼을 클릭하면 <strong>모든 IP에서 로그인</strong>이 가능해집니다.</p>
    </div>
  `,
  securityTips: `
    <h4>🛡️ 계정 보안 강화 가이드</h4>
    <div style="padding-left: 24px; margin-bottom: 16px;">
      <p>자동매매 시스템은 실제 자산을 다루므로 <strong>철저한 보안</strong>이 필요합니다.</p>
    </div>
    
    <h4>✅ 권장 보안 설정</h4>
    <div style="padding-left: 24px; margin-bottom: 16px;">
      <table style="width:100%; border-collapse: collapse; margin: 10px 0;">
        <tr style="background: #f5f5f5;">
          <th style="border: 1px solid #ddd; padding: 8px; text-align: left;">항목</th>
          <th style="border: 1px solid #ddd; padding: 8px; text-align: left;">권장</th>
          <th style="border: 1px solid #ddd; padding: 8px; text-align: left;">효과</th>
        </tr>
        <tr>
          <td style="border: 1px solid #ddd; padding: 8px;">2단계 인증</td>
          <td style="border: 1px solid #ddd; padding: 8px;">🔴 필수</td>
          <td style="border: 1px solid #ddd; padding: 8px;">비밀번호 유출 시에도 계정 보호</td>
        </tr>
        <tr>
          <td style="border: 1px solid #ddd; padding: 8px;">IP 화이트리스트</td>
          <td style="border: 1px solid #ddd; padding: 8px;">🟡 권장</td>
          <td style="border: 1px solid #ddd; padding: 8px;">허용된 위치에서만 접속 가능</td>
        </tr>
        <tr>
          <td style="border: 1px solid #ddd; padding: 8px;">강력한 비밀번호</td>
          <td style="border: 1px solid #ddd; padding: 8px;">🔴 필수</td>
          <td style="border: 1px solid #ddd; padding: 8px;">무차별 대입 공격 방어</td>
        </tr>
      </table>
    </div>
    
    <h4>🔑 강력한 비밀번호 조건</h4>
    <div style="padding-left: 24px; margin-bottom: 16px;">
      <ul style="margin: 0; padding-left: 20px;">
        <li>최소 <strong>8자 이상</strong></li>
        <li><strong>대문자</strong> 포함 (A-Z)</li>
        <li><strong>소문자</strong> 포함 (a-z)</li>
        <li><strong>숫자</strong> 포함 (0-9)</li>
        <li><strong>특수문자</strong> 포함 (!@#$%^&*)</li>
        <li>다른 사이트와 <strong>다른 비밀번호</strong> 사용</li>
      </ul>
    </div>
    
    <h4>🚫 피해야 할 비밀번호</h4>
    <div style="padding-left: 24px;">
      <ul style="margin: 0; padding-left: 20px;">
        <li>생년월일, 전화번호 등 개인정보</li>
        <li>연속된 숫자 (123456)</li>
        <li>키보드 패턴 (qwerty)</li>
        <li>사전에 있는 단어</li>
      </ul>
    </div>
  `
}

// 2FA 메서드
const load2FAStatus = async () => {
  try {
    const response = await userApi.get2FAStatus()
    twoFactorEnabled.value = response.data.enabled
  } catch (error: any) {
    console.error('2FA 상태 조회 실패:', error)
  }
}

const startSetup = async () => {
  twoFactorLoading.value = true
  twoFactorMessage.value = ''

  try {
    const response = await userApi.setup2FA()
    totpSecret.value = response.data.secret
    qrCodeUrl.value = response.data.qrCodeUrl
    
    // QR 코드 이미지 생성
    qrCodeDataUrl.value = await QRCode.toDataURL(qrCodeUrl.value, {
      width: 200,
      margin: 2
    })
    
    setupMode.value = true
    twoFactorMessage.value = 'QR 코드를 스캔하고 인증 코드를 입력하세요'
    twoFactorMessageType.value = 'info'
  } catch (error: any) {
    twoFactorMessage.value = error.response?.data?.error || '2FA 설정 시작 실패'
    twoFactorMessageType.value = 'error'
  } finally {
    twoFactorLoading.value = false
  }
}

const confirmSetup = async () => {
  if (verificationCode.value.length !== 6) {
    twoFactorMessage.value = '6자리 인증 코드를 입력하세요'
    twoFactorMessageType.value = 'error'
    return
  }

  twoFactorLoading.value = true
  twoFactorMessage.value = ''

  try {
    const response = await userApi.confirm2FA(verificationCode.value)
    twoFactorEnabled.value = true
    setupMode.value = false
    verificationCode.value = ''
    totpSecret.value = ''
    qrCodeDataUrl.value = ''
    twoFactorMessage.value = response.data.message || '2FA가 활성화되었습니다'
    twoFactorMessageType.value = 'success'
  } catch (error: any) {
    twoFactorMessage.value = error.response?.data?.error || '2FA 활성화 실패'
    twoFactorMessageType.value = 'error'
  } finally {
    twoFactorLoading.value = false
  }
}

const cancelSetup = () => {
  setupMode.value = false
  verificationCode.value = ''
  totpSecret.value = ''
  qrCodeDataUrl.value = ''
  twoFactorMessage.value = ''
}

const disable2FA = async () => {
  if (disableCode.value.length !== 6) {
    twoFactorMessage.value = '6자리 인증 코드를 입력하세요'
    twoFactorMessageType.value = 'error'
    return
  }

  twoFactorLoading.value = true
  twoFactorMessage.value = ''

  try {
    const response = await userApi.disable2FA(disableCode.value)
    twoFactorEnabled.value = false
    disableCode.value = ''
    twoFactorMessage.value = response.data.message || '2FA가 비활성화되었습니다'
    twoFactorMessageType.value = 'success'
  } catch (error: any) {
    twoFactorMessage.value = error.response?.data?.error || '2FA 비활성화 실패'
    twoFactorMessageType.value = 'error'
  } finally {
    twoFactorLoading.value = false
  }
}

const copySecret = () => {
  navigator.clipboard.writeText(totpSecret.value)
  twoFactorMessage.value = '비밀키가 클립보드에 복사되었습니다'
  twoFactorMessageType.value = 'info'
}

// IP 화이트리스트 메서드
const loadAllowedIps = async () => {
  try {
    const response = await userApi.getAllowedIps()
    allowedIps.value = response.data.allowedIps || []
  } catch (error: any) {
    console.error('IP 목록 로드 실패:', error)
  }
}

const loadCurrentIp = async () => {
  try {
    const response = await userApi.getCurrentIp()
    currentIp.value = response.data.ip
  } catch (error: any) {
    console.error('현재 IP 조회 실패:', error)
  }
}

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

const addCurrentIp = async () => {
  if (!currentIp.value) {
    await loadCurrentIp()
  }
  newIp.value = currentIp.value
  await addIp()
}

const removeIp = async (ip: string) => {
  ipLoading.value = true
  ipMessage.value = ''

  try {
    const response = await userApi.removeAllowedIp(ip)
    allowedIps.value = response.data.allowedIps
    ipMessage.value = 'IP가 삭제되었습니다'
    ipMessageType.value = 'success'
  } catch (error: any) {
    ipMessage.value = error.response?.data?.error || 'IP 삭제 실패'
    ipMessageType.value = 'error'
  } finally {
    ipLoading.value = false
  }
}

const disableIpWhitelist = async () => {
  ipLoading.value = true
  ipMessage.value = ''

  try {
    await userApi.disableIpWhitelist()
    allowedIps.value = []
    ipMessage.value = 'IP 화이트리스트가 비활성화되었습니다'
    ipMessageType.value = 'success'
  } catch (error: any) {
    ipMessage.value = error.response?.data?.error || '비활성화 실패'
    ipMessageType.value = 'error'
  } finally {
    ipLoading.value = false
  }
}

// 컴포넌트 마운트 시 데이터 로드
onMounted(() => {
  load2FAStatus()
  loadAllowedIps()
  loadCurrentIp()
})
</script>

<style scoped>
.gap-2 {
  gap: 8px;
}

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

.security-tips-card {
  background-color: #ECEFF1 !important; /* blue-grey-lighten-4 */
}

.security-list {
  background-color: #CFD8DC !important; /* blue-grey-lighten-3 */
}

.security-list-item {
  border-bottom: 1px solid #B0BEC5; /* blue-grey-lighten-2 */
}

.security-list-item:last-child {
  border-bottom: none;
}
</style>