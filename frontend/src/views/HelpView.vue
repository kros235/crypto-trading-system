<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main class="bg-grey-lighten-3">
      <v-container fluid>
        <v-row class="mb-4">
          <v-col>
            <h1 class="text-h4">
              <v-icon class="mr-2">mdi-help-circle</v-icon>
              도움말
            </h1>
            <p class="text-subtitle-1 text-grey">시스템 사용법, 용어 사전, FAQ를 확인하세요</p>
          </v-col>
        </v-row>

        <!-- 탭 메뉴 -->
        <v-row>
          <v-col cols="12">
            <v-tabs v-model="activeTab" color="indigo-darken-2" class="help-tabs mb-0">
              <v-tab value="guide">
                <v-icon start>mdi-rocket-launch</v-icon>
                시작 가이드
              </v-tab>
              <v-tab value="glossary">
                <v-icon start>mdi-book-alphabet</v-icon>
                용어 사전
              </v-tab>
              <v-tab value="faq">
                <v-icon start>mdi-frequently-asked-questions</v-icon>
                FAQ
              </v-tab>
              <v-tab value="troubleshoot">
                <v-icon start>mdi-wrench</v-icon>
                문제 해결
              </v-tab>
            </v-tabs>

            <v-card elevation="2" class="card-no-top-radius">
              <v-window v-model="activeTab">
                <!-- ========== 시작 가이드 탭 ========== -->
                <v-window-item value="guide">
                  <v-card-text class="pa-4">
                    <h3 class="text-h6 mb-4">
                      <v-icon class="mr-2" color="indigo">mdi-numeric-1-circle</v-icon>
                      회원가입 및 로그인
                    </h3>
                    <v-alert type="info" variant="tonal" class="mb-4">
                      <p class="mb-2">1. 회원가입 페이지에서 계정을 생성합니다.</p>
                      <p class="mb-2">2. 이메일 주소와 안전한 비밀번호를 입력합니다.</p>
                      <p class="mb-0">3. 보안 강화를 위해 2FA(2단계 인증) 설정을 권장합니다.</p>
                    </v-alert>

                    <v-divider class="my-4" />

                    <h3 class="text-h6 mb-4">
                      <v-icon class="mr-2" color="indigo">mdi-numeric-2-circle</v-icon>
                      업비트 API 키 발급 및 등록
                    </h3>
                    <v-alert type="warning" variant="tonal" class="mb-4">
                      <p class="font-weight-bold mb-2">⚠️ API 키 발급 시 주의사항</p>
                      <p class="mb-2">• 출금 권한은 <strong>절대 부여하지 마세요</strong></p>
                      <p class="mb-2">• IP 제한을 설정하면 보안이 강화됩니다</p>
                      <p class="mb-0">• API 키는 타인과 절대 공유하지 마세요</p>
                    </v-alert>
                    <v-stepper :items="apiKeySteps" alt-labels class="mb-4">
                      <template v-slot:item.1>
                        <v-card flat>
                          <v-card-text>
                            <a href="https://upbit.com/mypage/open_api_management" target="_blank" class="text-primary">
                              업비트 Open API 관리 페이지
                            </a>에 접속합니다.
                          </v-card-text>
                        </v-card>
                      </template>
                      <template v-slot:item.2>
                        <v-card flat>
                          <v-card-text>
                            "Open API Key 발급하기" 버튼을 클릭하고, <strong>자산조회</strong>와 <strong>주문</strong> 권한만 선택합니다.
                          </v-card-text>
                        </v-card>
                      </template>
                      <template v-slot:item.3>
                        <v-card flat>
                          <v-card-text>
                            발급된 Access Key와 Secret Key를 <strong>프로필 설정</strong> 페이지에 등록합니다.
                          </v-card-text>
                        </v-card>
                      </template>
                    </v-stepper>

                    <v-divider class="my-4" />

                    <h3 class="text-h6 mb-4">
                      <v-icon class="mr-2" color="indigo">mdi-numeric-3-circle</v-icon>
                      거래 설정
                    </h3>
                    <v-alert type="success" variant="tonal" class="mb-4">
                      <p class="mb-2"><strong>거래 코인 선택:</strong> 시가총액 상위 코인을 3~5개 선택하세요.</p>
                      <p class="mb-2"><strong>매수 조건:</strong> 기본값(-6%)으로 시작하고, 시장 상황에 맞게 조정하세요.</p>
                      <p class="mb-2"><strong>매도 조건:</strong> 목표 수익률 3~5%, 손절매 -10% 권장</p>
                      <p class="mb-0"><strong>리스크 관리:</strong> 일일 한도와 종목별 비중을 설정하세요.</p>
                    </v-alert>

                    <v-divider class="my-4" />

                    <h3 class="text-h6 mb-4">
                      <v-icon class="mr-2" color="indigo">mdi-numeric-4-circle</v-icon>
                      소액 테스트 권장
                    </h3>
                    <v-alert type="info" variant="tonal">
                      <p class="mb-2">처음에는 <strong>10만원 이하</strong>의 소액으로 1~2일간 테스트 운영을 권장합니다.</p>
                      <p class="mb-2">백테스팅 기능을 활용하여 과거 데이터로 전략을 검증하세요.</p>
                      <p class="mb-0">시스템이 정상 작동하는 것을 확인한 후 투자금을 늘려가세요.</p>
                    </v-alert>
                  </v-card-text>
                </v-window-item>

                <!-- ========== 용어 사전 탭 ========== -->
                <v-window-item value="glossary">
                  <v-card-text class="pa-4">
                    <!-- 검색 -->
                    <v-text-field
                      v-model="glossarySearch"
                      prepend-inner-icon="mdi-magnify"
                      label="용어 검색"
                      variant="outlined"
                      density="compact"
                      clearable
                      class="mb-4"
                    />

                    <!-- 카테고리별 용어 -->
                    <v-expansion-panels v-model="glossaryPanel" multiple>
                      <!-- 기술적 지표 -->
                      <v-expansion-panel>
                        <v-expansion-panel-title>
                          <v-icon class="mr-2" color="blue">mdi-chart-line</v-icon>
                          <strong>기술적 지표</strong>
                        </v-expansion-panel-title>
                        <v-expansion-panel-text>
                          <v-list density="compact">
                            <v-list-item v-for="term in filteredGlossary('indicator')" :key="term.term">
                              <template v-slot:prepend>
                                <v-icon size="small" color="blue">mdi-chevron-right</v-icon>
                              </template>
                              <v-list-item-title class="font-weight-bold">{{ term.term }}</v-list-item-title>
                              <v-list-item-subtitle class="text-wrap">{{ term.description }}</v-list-item-subtitle>
                            </v-list-item>
                          </v-list>
                        </v-expansion-panel-text>
                      </v-expansion-panel>

                      <!-- 거래 용어 -->
                      <v-expansion-panel>
                        <v-expansion-panel-title>
                          <v-icon class="mr-2" color="green">mdi-swap-horizontal</v-icon>
                          <strong>거래 용어</strong>
                        </v-expansion-panel-title>
                        <v-expansion-panel-text>
                          <v-list density="compact">
                            <v-list-item v-for="term in filteredGlossary('trading')" :key="term.term">
                              <template v-slot:prepend>
                                <v-icon size="small" color="green">mdi-chevron-right</v-icon>
                              </template>
                              <v-list-item-title class="font-weight-bold">{{ term.term }}</v-list-item-title>
                              <v-list-item-subtitle class="text-wrap">{{ term.description }}</v-list-item-subtitle>
                            </v-list-item>
                          </v-list>
                        </v-expansion-panel-text>
                      </v-expansion-panel>

                      <!-- 리스크 관리 -->
                      <v-expansion-panel>
                        <v-expansion-panel-title>
                          <v-icon class="mr-2" color="orange">mdi-shield-alert</v-icon>
                          <strong>리스크 관리</strong>
                        </v-expansion-panel-title>
                        <v-expansion-panel-text>
                          <v-list density="compact">
                            <v-list-item v-for="term in filteredGlossary('risk')" :key="term.term">
                              <template v-slot:prepend>
                                <v-icon size="small" color="orange">mdi-chevron-right</v-icon>
                              </template>
                              <v-list-item-title class="font-weight-bold">{{ term.term }}</v-list-item-title>
                              <v-list-item-subtitle class="text-wrap">{{ term.description }}</v-list-item-subtitle>
                            </v-list-item>
                          </v-list>
                        </v-expansion-panel-text>
                      </v-expansion-panel>

                      <!-- 성과 지표 -->
                      <v-expansion-panel>
                        <v-expansion-panel-title>
                          <v-icon class="mr-2" color="purple">mdi-chart-areaspline</v-icon>
                          <strong>성과 지표</strong>
                        </v-expansion-panel-title>
                        <v-expansion-panel-text>
                          <v-list density="compact">
                            <v-list-item v-for="term in filteredGlossary('performance')" :key="term.term">
                              <template v-slot:prepend>
                                <v-icon size="small" color="purple">mdi-chevron-right</v-icon>
                              </template>
                              <v-list-item-title class="font-weight-bold">{{ term.term }}</v-list-item-title>
                              <v-list-item-subtitle class="text-wrap">{{ term.description }}</v-list-item-subtitle>
                            </v-list-item>
                          </v-list>
                        </v-expansion-panel-text>
                      </v-expansion-panel>
                    </v-expansion-panels>
                  </v-card-text>
                </v-window-item>

                <!-- ========== FAQ 탭 ========== -->
                <v-window-item value="faq">
                  <v-card-text class="pa-4">
                    <!-- 검색 -->
                    <v-text-field
                      v-model="faqSearch"
                      prepend-inner-icon="mdi-magnify"
                      label="질문 검색"
                      variant="outlined"
                      density="compact"
                      clearable
                      class="mb-4"
                    />

                    <!-- FAQ 목록 -->
                    <v-expansion-panels v-model="faqPanel">
                      <v-expansion-panel 
                        v-for="(faq, index) in filteredFaqs" 
                        :key="index"
                      >
                        <v-expansion-panel-title>
                          <v-chip size="x-small" :color="getCategoryColor(faq.category)" class="mr-2">
                            {{ faq.category }}
                          </v-chip>
                          <strong>{{ faq.question }}</strong>
                        </v-expansion-panel-title>
                        <v-expansion-panel-text>
                          <div v-html="faq.answer" class="faq-answer"></div>
                        </v-expansion-panel-text>
                      </v-expansion-panel>
                    </v-expansion-panels>

                    <div v-if="filteredFaqs.length === 0" class="text-center py-8 text-grey">
                      <v-icon size="48" class="mb-2">mdi-magnify</v-icon>
                      <p>검색 결과가 없습니다.</p>
                    </div>
                  </v-card-text>
                </v-window-item>

                <!-- ========== 문제 해결 탭 ========== -->
                <v-window-item value="troubleshoot">
                  <v-card-text class="pa-4">
                    <!-- 일반적인 오류 -->
                    <h3 class="text-h6 mb-4">
                      <v-icon class="mr-2" color="red">mdi-alert-circle</v-icon>
                      일반적인 오류 해결
                    </h3>
                    <v-alert 
                      v-for="(issue, index) in commonIssues" 
                      :key="index"
                      :type="issue.type"
                      variant="tonal"
                      class="mb-3"
                    >
                      <p class="font-weight-bold mb-2">{{ issue.title }}</p>
                      <p class="mb-2 text-body-2">{{ issue.description }}</p>
                      <p class="mb-0 text-body-2"><strong>해결 방법:</strong> {{ issue.solution }}</p>
                    </v-alert>

                    <v-divider class="my-4" />

                    <!-- API 연동 문제 -->
                    <h3 class="text-h6 mb-4">
                      <v-icon class="mr-2" color="orange">mdi-api</v-icon>
                      API 연동 문제
                    </h3>
                    <v-alert 
                      v-for="(issue, index) in apiIssues" 
                      :key="'api-' + index"
                      type="warning"
                      variant="tonal"
                      class="mb-3"
                    >
                      <p class="font-weight-bold mb-2">{{ issue.title }}</p>
                      <p class="mb-2 text-body-2">{{ issue.description }}</p>
                      <p class="mb-0 text-body-2"><strong>해결 방법:</strong> {{ issue.solution }}</p>
                    </v-alert>

                    <v-divider class="my-4" />

                    <!-- 긴급 상황 대응 -->
                    <h3 class="text-h6 mb-4">
                      <v-icon class="mr-2" color="red-darken-2">mdi-alarm-light</v-icon>
                      긴급 상황 대응
                    </h3>
                    <v-alert type="error" variant="tonal" class="mb-3">
                      <p class="font-weight-bold mb-2">🚨 급락장 발생 시</p>
                      <p class="mb-2 text-body-2">1. 대시보드에서 봇 "긴급 정지" 버튼 클릭</p>
                      <p class="mb-2 text-body-2">2. 거래 설정에서 급락장 보호 기능 활성화 확인</p>
                      <p class="mb-0 text-body-2">3. 필요 시 업비트 앱에서 직접 매도 처리</p>
                    </v-alert>
                    <v-alert type="error" variant="tonal">
                      <p class="font-weight-bold mb-2">🔒 보안 침해 의심 시</p>
                      <p class="mb-2 text-body-2">1. 즉시 업비트에서 API 키 폐기</p>
                      <p class="mb-2 text-body-2">2. 비밀번호 변경</p>
                      <p class="mb-0 text-body-2">3. 2FA 재설정</p>
                    </v-alert>
                  </v-card-text>
                </v-window-item>
              </v-window>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'

const sidebarRef = ref()
const activeTab = ref('guide')
const glossarySearch = ref('')
const faqSearch = ref('')
const glossaryPanel = ref([0])
const faqPanel = ref<number[]>([])

// API 키 발급 단계
const apiKeySteps = [
  '업비트 접속',
  'API 키 발급',
  '키 등록'
]

// 용어 사전 데이터
const glossaryTerms = ref([
  // 기술적 지표
  { category: 'indicator', term: 'MA (이동평균선)', description: '일정 기간 동안의 가격 평균값을 연결한 선. MA20은 20일 이동평균선을 의미합니다.' },
  { category: 'indicator', term: 'RSI (상대강도지수)', description: '가격의 상승/하락 강도를 0~100 사이 값으로 표시. 30 이하면 과매도(매수 신호), 70 이상이면 과매수(매도 신호)로 해석합니다.' },
  { category: 'indicator', term: '볼린저 밴드 (BB)', description: '이동평균선을 중심으로 표준편차를 이용한 상/하단 밴드. 하단 밴드 접촉 시 매수 신호로 활용합니다.' },
  { category: 'indicator', term: '거래량', description: '일정 기간 동안 거래된 코인의 총량. 평균 거래량 대비 150% 이상일 때 의미 있는 움직임으로 판단합니다.' },
  { category: 'indicator', term: '이격도', description: '현재가가 기준가(이동평균선) 대비 얼마나 떨어져 있는지를 백분율로 표시한 값입니다.' },
  
  // 거래 용어
  { category: 'trading', term: '매수', description: '코인을 구매하는 것. 시스템이 설정된 조건 충족 시 자동으로 매수합니다.' },
  { category: 'trading', term: '매도', description: '보유한 코인을 판매하는 것. 목표 수익률 달성 또는 손절 조건 시 자동 매도됩니다.' },
  { category: 'trading', term: '목표 수익률', description: '매수가 대비 이 수익률에 도달하면 자동으로 매도합니다. 예: 5% 설정 시 매수가의 105%에서 매도' },
  { category: 'trading', term: '손절매 (Stop Loss)', description: '손실을 제한하기 위해 일정 비율 하락 시 강제 매도하는 것. 예: -10% 설정 시 매수가의 90%에서 자동 매도' },
  { category: 'trading', term: '트레일링 스톱', description: '최고가 대비 일정 비율 하락 시 매도. 수익을 보호하면서 상승 추세를 최대한 활용하는 전략입니다.' },
  
  // 리스크 관리
  { category: 'risk', term: '일일 한도', description: '하루에 사용할 수 있는 최대 투자 금액. 과도한 투자를 방지합니다.' },
  { category: 'risk', term: '종목별 비중', description: '전체 투자금 대비 한 코인에 투자할 수 있는 최대 비율. 분산 투자를 위해 20~25% 권장' },
  { category: 'risk', term: '시장 추세 필터', description: 'BTC가 20일 이동평균선 아래일 때 전체 매수를 중단하는 기능. 하락장에서 손실을 줄입니다.' },
  { category: 'risk', term: '누적 손실 긴급정지', description: '초기 자본 대비 누적 손실이 설정값에 도달하면 모든 거래를 중단합니다.' },
  { category: 'risk', term: '연속 손절 제한', description: '동일 코인에서 연속으로 손절이 발생하면 해당 코인 매수를 일시 금지합니다.' },
  
  // 성과 지표
  { category: 'performance', term: '수익률', description: '투자 대비 수익의 비율. (현재가치 - 투자금) / 투자금 × 100%' },
  { category: 'performance', term: '승률', description: '전체 거래 중 수익이 발생한 거래의 비율. 총 수익 거래 / 총 거래 × 100%' },
  { category: 'performance', term: 'MDD (최대 낙폭)', description: '특정 기간 동안 고점에서 저점까지의 최대 하락폭. 낮을수록 안정적인 전략입니다.' },
  { category: 'performance', term: '샤프 비율', description: '위험 대비 수익률. 높을수록 리스크 대비 수익이 좋은 전략입니다. 1 이상이면 양호' },
  { category: 'performance', term: '손익비', description: '평균 수익 대비 평균 손실의 비율. 1 이상이면 수익이 손실보다 큰 전략입니다.' }
])

// FAQ 데이터
const faqList = ref([
  {
    category: '계정',
    question: '비밀번호를 잊어버렸어요',
    answer: '로그인 페이지에서 "비밀번호 찾기"를 클릭하여 등록된 이메일로 재설정 링크를 받을 수 있습니다.'
  },
  {
    category: '계정',
    question: '2FA 인증 코드가 맞지 않아요',
    answer: '스마트폰의 시간이 정확한지 확인하세요. 자동 시간 설정을 활성화하면 해결되는 경우가 많습니다.'
  },
  {
    category: 'API',
    question: 'API 키 등록이 안 돼요',
    answer: '업비트에서 발급받은 키가 정확한지 확인하세요. Access Key와 Secret Key를 바꿔서 입력하지 않았는지도 확인하세요.'
  },
  {
    category: 'API',
    question: 'API 권한은 어떻게 설정해야 하나요?',
    answer: '<strong>자산조회</strong>와 <strong>주문</strong> 권한만 부여하세요. <span style="color: red;">출금 권한은 절대 부여하지 마세요!</span>'
  },
  {
    category: '거래',
    question: '자동매매가 작동하지 않아요',
    answer: '1) API 키가 정상 등록되었는지<br>2) 거래 설정이 완료되었는지<br>3) 봇이 "실행 중" 상태인지 확인하세요.'
  },
  {
    category: '거래',
    question: '매수 조건이 충족되었는데 매수가 안 돼요',
    answer: '일일 한도 초과, 종목별 최대 보유 건수 초과, 또는 잔고 부족일 수 있습니다. 대시보드에서 상태를 확인하세요.'
  },
  {
    category: '거래',
    question: '급락장 보호 기능은 언제 사용하나요?',
    answer: '시장 전체가 하락할 때 손실을 줄이기 위한 기능입니다. 시장 추세 필터, 누적 손실 긴급정지, 연속 손절 제한을 조합해 사용하세요.'
  },
  {
    category: '설정',
    question: '권장 설정값이 있나요?',
    answer: '초보자는 기본값(매수 -6%, 매도 +5%, 손절 -10%)으로 시작하세요. 백테스팅으로 검증 후 조정하는 것을 권장합니다.'
  },
  {
    category: '설정',
    question: '트레일링 스톱이 뭔가요?',
    answer: '최고가 대비 일정 비율 하락 시 매도하는 기능입니다. 예: -5% 설정 시, 100원에 매수 → 120원 최고가 도달 → 114원(120×0.95)에서 자동 매도'
  },
  {
    category: '수익',
    question: '평가 수익과 실현 수익의 차이는?',
    answer: '<strong>평가 수익</strong>: 현재 보유 중인 코인의 미실현 손익<br><strong>실현 수익</strong>: 매도 완료된 거래의 확정 손익'
  },
  {
    category: '수익',
    question: '수수료는 얼마나 빠지나요?',
    answer: '업비트 기준 매수/매도 각 0.05%입니다. 시스템에서 수익률 계산 시 수수료가 반영됩니다.'
  },
  {
    category: '보안',
    question: 'API 키가 유출되면 어떻게 하나요?',
    answer: '즉시 업비트에서 해당 API 키를 폐기하고 새로 발급받으세요. 출금 권한이 없었다면 자산 유출 위험은 없습니다.'
  },
  {
    category: '보안',
    question: 'IP 화이트리스트는 꼭 설정해야 하나요?',
    answer: '권장하지만 필수는 아닙니다. 고정 IP가 있다면 설정하여 보안을 강화하세요.'
  },
  {
    category: '기타',
    question: '봇은 24시간 작동하나요?',
    answer: '네, 서버가 운영 중이면 5분마다 자동으로 시장을 분석하고 거래를 실행합니다.'
  },
  {
    category: '기타',
    question: '업비트 점검 시간에는 어떻게 되나요?',
    answer: '업비트 점검 중에는 API 호출이 실패하며, 시스템은 자동으로 재시도합니다. 점검 종료 후 정상 작동합니다.'
  }
])

// 일반적인 오류
const commonIssues = ref([
  {
    type: 'error',
    title: '로그인이 안 돼요',
    description: '비밀번호가 5회 이상 틀리면 계정이 잠깁니다.',
    solution: '관리자에게 문의하여 계정 잠금을 해제받으세요.'
  },
  {
    type: 'warning',
    title: '페이지가 느리게 로딩돼요',
    description: '네트워크 상태가 불안정하거나 서버 부하가 높을 수 있습니다.',
    solution: '잠시 후 다시 시도하거나, 브라우저 캐시를 삭제해보세요.'
  },
  {
    type: 'info',
    title: '데이터가 업데이트되지 않아요',
    description: '실시간 데이터는 일정 주기로 갱신됩니다.',
    solution: '새로고침 버튼을 클릭하거나 페이지를 다시 로드하세요.'
  }
])

// API 연동 문제
const apiIssues = ref([
  {
    title: 'API 키 등록 실패',
    description: 'Access Key 또는 Secret Key가 올바르지 않습니다.',
    solution: '업비트에서 키를 다시 확인하고, 복사 시 공백이 포함되지 않았는지 확인하세요.'
  },
  {
    title: '잔고 조회 실패',
    description: 'API 키에 자산조회 권한이 없을 수 있습니다.',
    solution: '업비트에서 API 키 권한을 확인하고, 자산조회 권한이 있는지 확인하세요.'
  },
  {
    title: '주문 실패',
    description: '잔고 부족, 최소 주문 금액 미달, 또는 API 권한 문제일 수 있습니다.',
    solution: '잔고와 최소 주문 금액(5,000원)을 확인하고, API에 주문 권한이 있는지 확인하세요.'
  }
])

// 용어 필터링
const filteredGlossary = (category: string) => {
  return glossaryTerms.value.filter(term => {
    const matchCategory = term.category === category
    const matchSearch = !glossarySearch.value || 
      term.term.toLowerCase().includes(glossarySearch.value.toLowerCase()) ||
      term.description.toLowerCase().includes(glossarySearch.value.toLowerCase())
    return matchCategory && matchSearch
  })
}

// FAQ 필터링
const filteredFaqs = computed(() => {
  if (!faqSearch.value) return faqList.value
  const search = faqSearch.value.toLowerCase()
  return faqList.value.filter(faq => 
    faq.question.toLowerCase().includes(search) ||
    faq.answer.toLowerCase().includes(search) ||
    faq.category.toLowerCase().includes(search)
  )
})

// 용어 검색 시 관련 패널 자동 열기 
watch(glossarySearch, (newValue) => {
  if (newValue && newValue.length > 0) {
    // 검색어가 있으면 검색 결과가 있는 패널만 열기
    const panelsToOpen: number[] = []
    const categories = ['indicator', 'trading', 'risk', 'performance']
    
    categories.forEach((category, index) => {
      const hasResults = glossaryTerms.value.some(term => {
        if (term.category !== category) return false
        const search = newValue.toLowerCase()
        return term.term.toLowerCase().includes(search) || 
               term.description.toLowerCase().includes(search)
      })
      if (hasResults) {
        panelsToOpen.push(index)
      }
    })
    
    glossaryPanel.value = panelsToOpen
  }
})

// 카테고리 색상
const getCategoryColor = (category: string) => {
  const colors: Record<string, string> = {
    '계정': 'blue',
    'API': 'orange',
    '거래': 'green',
    '설정': 'purple',
    '수익': 'teal',
    '보안': 'red',
    '기타': 'grey'
  }
  return colors[category] || 'grey'
}
</script>

<style scoped>
.help-tabs {
  background-color: #455A64;
  border-radius: 8px 8px 0 0;
}

.help-tabs :deep(.v-tab) {
  color: rgba(255, 255, 255, 0.7) !important;
}

.help-tabs :deep(.v-tab--selected) {
  color: white !important;
  background-color: #37474F;
}

.card-no-top-radius {
  border-top-left-radius: 0 !important;
  border-top-right-radius: 0 !important;
}

.faq-answer {
  line-height: 1.6;
}

.faq-answer strong {
  color: #1565C0;
}

.v-list-item-subtitle {
  white-space: normal !important;
  -webkit-line-clamp: unset !important;
}

/* 모바일 탭 스크롤  */
.help-tabs {
  overflow-x: auto;
}

.help-tabs :deep(.v-slide-group__content) {
  flex-wrap: nowrap;
}

@media (max-width: 600px) {
  .help-tabs :deep(.v-tab) {
    min-width: auto;
    padding: 0 12px;
  }
}
</style>