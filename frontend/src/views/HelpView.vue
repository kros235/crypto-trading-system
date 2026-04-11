<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main class="bg-grey-lighten-3">
      <v-container fluid>
        <v-row class="mb-4">
          <v-col>
            <div class="d-flex align-center">
              <h1 class="text-h4">
                <v-icon class="mr-2">mdi-help-circle</v-icon>
                도움말
              </h1>
              <HelpButton
                :use-dialog="true"
                :dialog-title="helpContents.helpOverview.title"
                :dialog-content="helpContents.helpOverview.content"
                color="grey-darken-1"
                class="ml-2"
              />
            </div>
            <p class="text-subtitle-1 text-grey">시스템 사용법, 용어 사전, FAQ를 확인하세요</p>
          </v-col>
        </v-row>

        <!-- 탭 메뉴 -->
        <v-row>
          <v-col cols="12">
            <!-- ⭐ 수정: HoldingsView 스타일 탭 적용 -->
            <v-tabs 
              v-model="activeTab" 
              color="grey-darken-3" 
              class="help-tabs mb-0"
              height="48"
            >
              <v-tab value="guide" class="help-tab text-body-1 font-weight-bold">
                <v-icon start size="20">mdi-rocket-launch</v-icon>
                시작 가이드
              </v-tab>
              <v-tab value="glossary" class="help-tab text-body-1 font-weight-bold">
                <v-icon start size="20">mdi-book-alphabet</v-icon>
                용어 사전
              </v-tab>
              <v-tab value="faq" class="help-tab text-body-1 font-weight-bold">
                <v-icon start size="20">mdi-frequently-asked-questions</v-icon>
                FAQ
              </v-tab>
              <v-tab value="troubleshoot" class="help-tab text-body-1 font-weight-bold">
                <v-icon start size="20">mdi-wrench</v-icon>
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
                    <!-- ⭐ 수정: 배경색과 텍스트 색상 대비 강화 -->
                    <v-alert color="blue-lighten-5" border="start" border-color="blue" class="mb-4 guide-alert">
                      <p class="mb-2 text-grey-darken-4">1. 회원가입 페이지에서 계정을 생성합니다.</p>
                      <p class="mb-2 text-grey-darken-4">2. 이메일 주소와 안전한 비밀번호를 입력합니다.</p>
                      <p class="mb-0 text-grey-darken-4">3. 보안 강화를 위해 2FA(2단계 인증) 설정을 권장합니다.</p>
                    </v-alert>

                    <v-divider class="my-4" />

                    <h3 class="text-h6 mb-4">
                      <v-icon class="mr-2" color="indigo">mdi-numeric-2-circle</v-icon>
                      업비트 API 키 발급 및 등록
                    </h3>
                    <!-- ⭐ 수정: 경고 알림 색상 대비 강화 -->
                    <v-alert color="orange-lighten-5" border="start" border-color="orange-darken-2" class="mb-4 guide-alert">
                      <p class="font-weight-bold mb-2 text-orange-darken-4">⚠️ API 키 발급 시 주의사항</p>
                      <p class="mb-2 text-grey-darken-4">• 출금 권한은 <strong class="text-red">절대 부여하지 마세요</strong></p>
                      <p class="mb-2 text-grey-darken-4">• IP 제한을 설정하면 보안이 강화됩니다</p>
                      <p class="mb-0 text-grey-darken-4">• API 키는 타인과 절대 공유하지 마세요.</p>
                    </v-alert>

                    <v-stepper :items="apiKeySteps" alt-labels class="mb-4 api-stepper" color="indigo">
                      <template v-slot:item.1>
                        <v-card flat>
                          <v-card-text class="text-grey-darken-3">
                            <a href="https://upbit.com/mypage/open_api_management" target="_blank" class="text-primary">
                              업비트 Open API 관리 페이지
                            </a>에 접속합니다.
                          </v-card-text>
                        </v-card>
                      </template>
                      <template v-slot:item.2>
                        <v-card flat>
                          <v-card-text class="text-grey-darken-3">
                            자산조회와 주문 권한만 선택하여 API 키를 발급받습니다.
                          </v-card-text>
                        </v-card>
                      </template>
                      <template v-slot:item.3>
                        <v-card flat>
                          <v-card-text class="text-grey-darken-3">
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
                    <!-- ⭐ 수정: 성공 알림 색상 대비 강화 -->
                    <v-alert color="green-lighten-5" border="start" border-color="green" class="mb-4 guide-alert">
                      <p class="mb-2 text-grey-darken-4"><strong class="text-green-darken-3">거래 코인 선택:</strong> 시가총액 상위 코인을 3~5개 선택하세요.</p>
                      <p class="mb-2 text-grey-darken-4"><strong class="text-green-darken-3">매수 조건:</strong> 기본값(-6%)으로 시작하고, 시장 상황에 맞게 조정하세요.</p>
                      <p class="mb-2 text-grey-darken-4"><strong class="text-green-darken-3">매도 조건:</strong> 목표 수익률 3~5%, 손절매 -10% 권장</p>
                      <p class="mb-0 text-grey-darken-4"><strong class="text-green-darken-3">리스크 관리:</strong> 일일 한도와 종목별 비중을 설정하세요.</p>
                    </v-alert>

                    <v-divider class="my-4" />

                    <h3 class="text-h6 mb-4">
                      <v-icon class="mr-2" color="indigo">mdi-numeric-4-circle</v-icon>
                      소액 테스트 권장
                    </h3>
                    <!-- ⭐ 수정: 정보 알림 색상 대비 강화 -->
                    <v-alert color="blue-lighten-5" border="start" border-color="blue" class="guide-alert">
                      <p class="mb-2 text-grey-darken-4">처음에는 <strong class="text-blue-darken-3">10만원 이하</strong>의 소액으로 1~2일간 테스트 운영을 권장합니다.</p>
                      <p class="mb-2 text-grey-darken-4">백테스팅 기능을 활용하여 과거 데이터로 전략을 검증하세요.</p>
                      <p class="mb-0 text-grey-darken-4">시스템이 정상 작동하는 것을 확인한 후 투자금을 늘려가세요.</p>
                    </v-alert>

                    <!-- ⭐ 추가: Phase 2 주식/ETF 안내 섹션 -->
                    <v-divider class="my-4" />

                    <h3 class="text-h6 mb-4">
                      <v-icon class="mr-2" color="orange-darken-2">mdi-numeric-5-circle</v-icon>
                      주식/ETF 자동매매 (Phase 2)
                      <v-chip color="orange-darken-2" size="x-small" variant="flat" class="ml-2 text-white">Phase 2</v-chip>
                    </h3>

                    <v-alert color="orange-lighten-5" border="start" border-color="orange-darken-2" class="mb-4 guide-alert">
                      <p class="font-weight-bold mb-2 text-orange-darken-4">📈 주식/ETF 자동매매 시작하기</p>
                      <p class="mb-2 text-grey-darken-4"><strong class="text-orange-darken-3">KIS API 키 발급:</strong> <a href="https://apiportal.koreainvestment.com" target="_blank" class="text-primary">한국투자증권 개발자 포털</a>에서 API 키를 발급받으세요.</p>
                      <p class="mb-2 text-grey-darken-4"><strong class="text-orange-darken-3">API 키 등록:</strong> 프로필 설정 페이지에서 KIS API 키를 등록하세요.</p>
                      <p class="mb-2 text-grey-darken-4"><strong class="text-orange-darken-3">거래 설정:</strong> 주식 거래 설정 페이지에서 종목과 매매 조건을 설정하세요.</p>
                      <p class="mb-0 text-grey-darken-4"><strong class="text-orange-darken-3">모의투자 권장:</strong> 처음에는 모의투자 모드로 전략을 검증하세요.</p>
                    </v-alert>

                    <v-alert color="red-lighten-5" border="start" border-color="red" class="guide-alert">
                      <p class="font-weight-bold mb-2 text-red-darken-4">⚠️ 레버리지 ETF 주의사항</p>
                      <p class="mb-2 text-grey-darken-4">• 장기 보유 시 <strong class="text-red">복리 효과로 인한 가치 침식(Volatility Drag)</strong>이 발생합니다.</p>
                      <p class="mb-2 text-grey-darken-4">• 권장 보유 기간: <strong>최대 20거래일</strong></p>
                      <p class="mb-2 text-grey-darken-4">• 환노출형(TIGER) vs 환헤지형(KODEX) 선택에 따라 수익률이 최대 20%p 차이날 수 있습니다.</p>
                      <p class="mb-0 text-grey-darken-4">• 거래 시간: 한국 증시 09:00 ~ 15:30 (KST)</p>
                    </v-alert>
                  </v-card-text>
                </v-window-item>

                <!-- ========== 용어 사전 탭 (⭐ 전면 개편) ========== -->
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

                    <!-- ⭐ 수정: 카테고리별 용어 - 상세 설명 포함 -->
                    <v-expansion-panels v-model="glossaryPanel" multiple>
                      <!-- 기술적 지표 -->
                      <v-expansion-panel>
                        <v-expansion-panel-title>
                          <v-icon class="mr-2" color="blue">mdi-chart-line</v-icon>
                          <strong>기술적 지표</strong>
                        </v-expansion-panel-title>
                        <v-expansion-panel-text>
                          <v-expansion-panels variant="accordion">
                            <v-expansion-panel 
                              v-for="term in filteredGlossary('indicator')" 
                              :key="term.term"
                            >
                              <v-expansion-panel-title>
                                <v-icon size="small" color="blue" class="mr-2">mdi-chevron-right</v-icon>
                                <strong>{{ term.term }}</strong>
                              </v-expansion-panel-title>
                              <v-expansion-panel-text>
                                <div class="glossary-detail pa-3">
                                  <div class="glossary-section mb-4">
                                    <div class="d-flex align-center mb-2">
                                      <v-icon size="18" color="grey-darken-1" class="mr-2">mdi-book-open-variant</v-icon>
                                      <span class="text-subtitle-2 font-weight-bold">쉬운 설명:</span>
                                    </div>
                                    <p class="text-body-2 text-grey-darken-3 mb-0">"{{ term.simpleDesc }}"</p>
                                  </div>
                                  
                                  <v-card variant="outlined" class="mb-4 glossary-example-card">
                                    <v-card-text>
                                      <div class="d-flex align-center mb-2">
                                        <v-icon size="18" class="mr-2">{{ term.exampleIcon }}</v-icon>
                                        <span class="text-subtitle-2 font-weight-bold">{{ term.exampleTitle }}:</span>
                                      </div>
                                      <div v-html="term.example" class="glossary-example"></div>
                                    </v-card-text>
                                  </v-card>
                                  
                                  <div v-if="term.hasDiagram" class="mb-4">
                                    <div class="d-flex align-center mb-2">
                                      <v-icon size="18" color="purple" class="mr-2">mdi-chart-box-outline</v-icon>
                                      <span class="text-subtitle-2 font-weight-bold">시각적 설명:</span>
                                    </div>
                                    <div v-html="term.diagram" class="glossary-diagram"></div>
                                  </div>
                                  
                                  <div v-if="term.table">
                                    <div class="d-flex align-center mb-2">
                                      <v-icon size="18" color="teal" class="mr-2">mdi-table</v-icon>
                                      <span class="text-subtitle-2 font-weight-bold">설정값 안내:</span>
                                    </div>
                                    <v-table density="compact" class="glossary-table">
                                      <thead>
                                        <tr>
                                          <th v-for="(header, idx) in term.table.headers" :key="idx">{{ header }}</th>
                                        </tr>
                                      </thead>
                                      <tbody>
                                        <tr v-for="(row, idx) in term.table.rows" :key="idx">
                                          <td v-for="(cell, cidx) in row" :key="cidx" v-html="cell"></td>
                                        </tr>
                                      </tbody>
                                    </v-table>
                                  </div>
                                </div>
                              </v-expansion-panel-text>
                            </v-expansion-panel>
                          </v-expansion-panels>
                        </v-expansion-panel-text>
                      </v-expansion-panel>

                      <!-- 거래 용어 -->
                      <v-expansion-panel>
                        <v-expansion-panel-title>
                          <v-icon class="mr-2" color="green">mdi-swap-horizontal</v-icon>
                          <strong>거래 용어</strong>
                        </v-expansion-panel-title>
                        <v-expansion-panel-text>
                          <v-expansion-panels variant="accordion">
                            <v-expansion-panel 
                              v-for="term in filteredGlossary('trading')" 
                              :key="term.term"
                            >
                              <v-expansion-panel-title>
                                <v-icon size="small" color="green" class="mr-2">mdi-chevron-right</v-icon>
                                <strong>{{ term.term }}</strong>
                              </v-expansion-panel-title>
                              <v-expansion-panel-text>
                                <div class="glossary-detail pa-3">
                                  <div class="glossary-section mb-4">
                                    <div class="d-flex align-center mb-2">
                                      <v-icon size="18" color="grey-darken-1" class="mr-2">mdi-book-open-variant</v-icon>
                                      <span class="text-subtitle-2 font-weight-bold">쉬운 설명:</span>
                                    </div>
                                    <p class="text-body-2 text-grey-darken-3 mb-0">"{{ term.simpleDesc }}"</p>
                                  </div>
                                  
                                  <v-card variant="outlined" class="mb-4 glossary-example-card">
                                    <v-card-text>
                                      <div class="d-flex align-center mb-2">
                                        <v-icon size="18" class="mr-2">{{ term.exampleIcon }}</v-icon>
                                        <span class="text-subtitle-2 font-weight-bold">{{ term.exampleTitle }}:</span>
                                      </div>
                                      <div v-html="term.example" class="glossary-example"></div>
                                    </v-card-text>
                                  </v-card>
                                  
                                  <div v-if="term.table">
                                    <div class="d-flex align-center mb-2">
                                      <v-icon size="18" color="teal" class="mr-2">mdi-table</v-icon>
                                      <span class="text-subtitle-2 font-weight-bold">설정값 안내:</span>
                                    </div>
                                    <v-table density="compact" class="glossary-table">
                                      <thead>
                                        <tr>
                                          <th v-for="(header, idx) in term.table.headers" :key="idx">{{ header }}</th>
                                        </tr>
                                      </thead>
                                      <tbody>
                                        <tr v-for="(row, idx) in term.table.rows" :key="idx">
                                          <td v-for="(cell, cidx) in row" :key="cidx" v-html="cell"></td>
                                        </tr>
                                      </tbody>
                                    </v-table>
                                  </div>
                                </div>
                              </v-expansion-panel-text>
                            </v-expansion-panel>
                          </v-expansion-panels>
                        </v-expansion-panel-text>
                      </v-expansion-panel>

                      <!-- 리스크 관리 -->
                      <v-expansion-panel>
                        <v-expansion-panel-title>
                          <v-icon class="mr-2" color="orange">mdi-shield-alert</v-icon>
                          <strong>리스크 관리</strong>
                        </v-expansion-panel-title>
                        <v-expansion-panel-text>
                          <v-expansion-panels variant="accordion">
                            <v-expansion-panel 
                              v-for="term in filteredGlossary('risk')" 
                              :key="term.term"
                            >
                              <v-expansion-panel-title>
                                <v-icon size="small" color="orange" class="mr-2">mdi-chevron-right</v-icon>
                                <strong>{{ term.term }}</strong>
                              </v-expansion-panel-title>
                              <v-expansion-panel-text>
                                <div class="glossary-detail pa-3">
                                  <div class="glossary-section mb-4">
                                    <div class="d-flex align-center mb-2">
                                      <v-icon size="18" color="grey-darken-1" class="mr-2">mdi-book-open-variant</v-icon>
                                      <span class="text-subtitle-2 font-weight-bold">쉬운 설명:</span>
                                    </div>
                                    <p class="text-body-2 text-grey-darken-3 mb-0">"{{ term.simpleDesc }}"</p>
                                  </div>
                                  
                                  <v-card variant="outlined" class="mb-4 glossary-example-card">
                                    <v-card-text>
                                      <div class="d-flex align-center mb-2">
                                        <v-icon size="18" class="mr-2">{{ term.exampleIcon }}</v-icon>
                                        <span class="text-subtitle-2 font-weight-bold">{{ term.exampleTitle }}:</span>
                                      </div>
                                      <div v-html="term.example" class="glossary-example"></div>
                                    </v-card-text>
                                  </v-card>
                                  
                                  <div v-if="term.table">
                                    <div class="d-flex align-center mb-2">
                                      <v-icon size="18" color="teal" class="mr-2">mdi-table</v-icon>
                                      <span class="text-subtitle-2 font-weight-bold">설정값 안내:</span>
                                    </div>
                                    <v-table density="compact" class="glossary-table">
                                      <thead>
                                        <tr>
                                          <th v-for="(header, idx) in term.table.headers" :key="idx">{{ header }}</th>
                                        </tr>
                                      </thead>
                                      <tbody>
                                        <tr v-for="(row, idx) in term.table.rows" :key="idx">
                                          <td v-for="(cell, cidx) in row" :key="cidx" v-html="cell"></td>
                                        </tr>
                                      </tbody>
                                    </v-table>
                                  </div>
                                </div>
                              </v-expansion-panel-text>
                            </v-expansion-panel>
                          </v-expansion-panels>
                        </v-expansion-panel-text>
                      </v-expansion-panel>

                      <!-- 성과 지표 -->
                      <v-expansion-panel>
                        <v-expansion-panel-title>
                          <v-icon class="mr-2" color="purple">mdi-chart-areaspline</v-icon>
                          <strong>성과 지표</strong>
                        </v-expansion-panel-title>
                        <v-expansion-panel-text>
                          <v-expansion-panels variant="accordion">
                            <v-expansion-panel 
                              v-for="term in filteredGlossary('performance')" 
                              :key="term.term"
                            >
                              <v-expansion-panel-title>
                                <v-icon size="small" color="purple" class="mr-2">mdi-chevron-right</v-icon>
                                <strong>{{ term.term }}</strong>
                              </v-expansion-panel-title>
                              <v-expansion-panel-text>
                                <div class="glossary-detail pa-3">
                                  <div class="glossary-section mb-4">
                                    <div class="d-flex align-center mb-2">
                                      <v-icon size="18" color="grey-darken-1" class="mr-2">mdi-book-open-variant</v-icon>
                                      <span class="text-subtitle-2 font-weight-bold">쉬운 설명:</span>
                                    </div>
                                    <p class="text-body-2 text-grey-darken-3 mb-0">"{{ term.simpleDesc }}"</p>
                                  </div>
                                  
                                  <v-card variant="outlined" class="mb-4 glossary-example-card">
                                    <v-card-text>
                                      <div class="d-flex align-center mb-2">
                                        <v-icon size="18" class="mr-2">{{ term.exampleIcon }}</v-icon>
                                        <span class="text-subtitle-2 font-weight-bold">{{ term.exampleTitle }}:</span>
                                      </div>
                                      <div v-html="term.example" class="glossary-example"></div>
                                    </v-card-text>
                                  </v-card>
                                  
                                  <div v-if="term.table">
                                    <div class="d-flex align-center mb-2">
                                      <v-icon size="18" color="teal" class="mr-2">mdi-table</v-icon>
                                      <span class="text-subtitle-2 font-weight-bold">해석 기준:</span>
                                    </div>
                                    <v-table density="compact" class="glossary-table">
                                      <thead>
                                        <tr>
                                          <th v-for="(header, idx) in term.table.headers" :key="idx">{{ header }}</th>
                                        </tr>
                                      </thead>
                                      <tbody>
                                        <tr v-for="(row, idx) in term.table.rows" :key="idx">
                                          <td v-for="(cell, cidx) in row" :key="cidx" v-html="cell"></td>
                                        </tr>
                                      </tbody>
                                    </v-table>
                                  </div>
                                </div>
                              </v-expansion-panel-text>
                            </v-expansion-panel>
                          </v-expansion-panels>
                        </v-expansion-panel-text>
                      </v-expansion-panel>
                    </v-expansion-panels>
                  </v-card-text>
                </v-window-item>

                <!-- ========== FAQ 탭 ========== -->
                <v-window-item value="faq">
                  <v-card-text class="pa-4">
                    <v-text-field
                      v-model="faqSearch"
                      prepend-inner-icon="mdi-magnify"
                      label="질문 검색"
                      variant="outlined"
                      density="compact"
                      clearable
                      class="mb-4"
                    />

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
                    <h3 class="text-h6 mb-4">
                      <v-icon class="mr-2" color="orange">mdi-alert-circle</v-icon>
                      일반적인 오류 해결
                    </h3>

                    <v-alert 
                      v-for="(issue, index) in commonIssues" 
                      :key="index"
                      color="red-lighten-5" 
                      border="start" 
                      border-color="red"
                      class="mb-3 troubleshoot-alert"
                    >
                      <p class="font-weight-bold mb-1 text-red-darken-3">❌ {{ issue.title }}</p>
                      <p class="text-body-2 mb-1 text-grey-darken-3">{{ issue.description }}</p>
                      <p class="text-body-2 mb-0 text-grey-darken-4"><strong class="text-green-darken-3">해결 방법:</strong> {{ issue.solution }}</p>
                    </v-alert>

                    <v-divider class="my-4" />

                    <h3 class="text-h6 mb-4">
                      <v-icon class="mr-2" color="blue">mdi-api</v-icon>
                      API 연동 문제
                    </h3>

                    <v-alert 
                      v-for="(issue, index) in apiIssues" 
                      :key="'api-' + index"
                      color="orange-lighten-5" 
                      border="start" 
                      border-color="orange-darken-2"
                      class="mb-3 troubleshoot-alert"
                    >
                      <p class="font-weight-bold mb-1 text-orange-darken-4">⚠️ {{ issue.title }}</p>
                      <p class="text-body-2 mb-1 text-grey-darken-3">{{ issue.description }}</p>
                      <p class="text-body-2 mb-0 text-grey-darken-4"><strong class="text-green-darken-3">해결 방법:</strong> {{ issue.solution }}</p>
                    </v-alert>

                    <v-divider class="my-4" />

                    <h3 class="text-h6 mb-4">
                      <v-icon class="mr-2" color="red">mdi-alarm-light</v-icon>
                      긴급 상황 대응
                    </h3>
                    
                    <v-alert color="red-lighten-5" border="start" border-color="red-darken-2" class="mb-3 troubleshoot-alert">
                      <p class="font-weight-bold mb-2 text-red-darken-3">🚨 급락장 발생 시</p>
                      <p class="mb-2 text-body-2 text-grey-darken-4">1. 대시보드에서 봇 "긴급 정지" 버튼 클릭</p>
                      <p class="mb-2 text-body-2 text-grey-darken-4">2. 거래 설정에서 급락장 보호 기능 활성화 확인</p>
                      <p class="mb-0 text-body-2 text-grey-darken-4">3. 필요 시 업비트 앱에서 직접 매도 처리</p>
                    </v-alert>
                    <v-alert color="yellow-lighten-4" border="start" border-color="orange-darken-2" class="troubleshoot-alert">
                      <p class="font-weight-bold mb-2 text-orange-darken-4">🔒 보안 침해 의심 시</p>
                      <p class="mb-2 text-body-2 text-grey-darken-4">1. 즉시 업비트에서 API 키 폐기</p>
                      <p class="mb-2 text-body-2 text-grey-darken-4">2. 비밀번호 변경</p>
                      <p class="mb-0 text-body-2 text-grey-darken-4">3. 2FA 재설정</p>
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
import HelpButton from '@/components/HelpButton.vue'

const sidebarRef = ref()

const helpContents = {
  helpOverview: {
    title: '❓ 도움말 페이지 안내',
    content: `
      <p class="help-intro">시스템 사용에 필요한 모든 정보를 제공하는 페이지입니다.</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>시작 가이드</strong>
        <span class="help-desc">회원가입부터 첫 거래까지의 단계별 안내입니다.<br/>처음 사용하시는 분은 이 탭부터 확인하세요.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>용어 사전</strong>
        <span class="help-desc">기술적 지표, 거래 용어, 리스크 관리, 성과 지표의 상세 설명입니다.<br/>검색 기능으로 원하는 용어를 빠르게 찾을 수 있습니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>FAQ</strong>
        <span class="help-desc">자주 묻는 질문과 답변을 카테고리별로 정리했습니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>문제 해결</strong>
        <span class="help-desc">일반적인 오류, API 연동 문제, 긴급 상황 대응 방법입니다.</span></p>
      <p class="help-note">💡 <strong>Tip:</strong> 각 페이지의 ? 아이콘을 클릭하면 해당 기능의 간단한 도움말을 볼 수 있습니다.</p>
    `
  }
}

const activeTab = ref('guide')
const glossarySearch = ref('')
const faqSearch = ref('')
const glossaryPanel = ref([0])
const faqPanel = ref<number[]>([])

const apiKeySteps = [
  '업비트 접속',
  'API 키 발급',
  '키 등록'
]

interface GlossaryTerm {
  category: string
  term: string
  description: string
  simpleDesc: string
  exampleIcon: string
  exampleTitle: string
  example: string
  hasDiagram?: boolean
  diagram?: string
  table?: {
    headers: string[]
    rows: string[][]
  }
}

const glossaryTerms = ref<GlossaryTerm[]>([
  // ========== 기술적 지표 ==========
  { 
    category: 'indicator', 
    term: 'MA (이동평균선)', 
    description: '일정 기간 동안의 가격 평균값을 연결한 선.',
    simpleDesc: '최근 며칠간의 평균 가격',
    exampleIcon: 'mdi-cart',
    exampleTitle: '🛒 마트 예시',
    example: `사과 가격이 매일 바뀐다고 생각해보세요.<br/>
- 어제: 1,000원<br/>
- 그제: 1,200원<br/>
- 3일 전: 800원<br/>
→ <strong>3일 이동평균: (1000+1200+800) ÷ 3 = 1,000원</strong><br/><br/>
<strong>20일 이동평균 = 최근 20일간 평균 가격</strong><br/><br/>
📊 <strong>코인 예시:</strong><br/>
비트코인 20일 이동평균이 1억원인데<br/>
오늘 가격이 9,400만원이면?<br/>
→ "평균보다 6% 싸네! 살까?"`,
    table: {
      headers: ['설정값', '의미', '적합한 사람'],
      rows: [
        ['7~10일', '단기 추세', '자주 거래하고 싶은 사람'],
        ['<strong>20일</strong>', '중기 추세', '초보자 추천 ✅'],
        ['30일', '장기 추세', '느긋한 투자자']
      ]
    }
  },
  { 
    category: 'indicator', 
    term: 'RSI (상대강도지수)', 
    description: '가격의 상승/하락 강도를 0~100 사이 값으로 표시.',
    simpleDesc: '지금 너무 많이 올랐나? 많이 떨어졌나? 판단 지표',
    exampleIcon: 'mdi-thermometer',
    exampleTitle: '🌡️ 온도계 비유',
    example: `RSI = 시장의 "과열/냉각" 온도계<br/><br/>
<div style="font-family: monospace; background: #37474F; padding: 12px; border-radius: 8px; color: #ECEFF1;">
100 ── 🔥 극도로 과열 (팔아야 할 때)<br/>
 70 ── ⚠️ 과열 (매도 신호) ← 매도 ≥ 68<br/>
 50 ── 😐 보통<br/>
 30 ── ❄️ 냉각 (매수 신호) ← 매수 ≤ 32<br/>
  0 ── 🥶 극도로 냉각 (사야 할 때)
</div><br/>
📊 <strong>현재 설정:</strong><br/>
- RSI ≤ 32: "충분히 떨어졌다, 매수!"<br/>
- RSI ≥ 68: "충분히 올랐다, 매도!"`,
    table: {
      headers: ['설정', '기간 14일', '매수 ≤32', '매도 ≥68'],
      rows: [
        ['의미', '표준 설정', '과매도 진입', '과매수 전 탈출'],
        ['추천', '✅', '✅', '✅']
      ]
    }
  },
  { 
    category: 'indicator', 
    term: '볼린저 밴드 (BB)', 
    description: '이동평균선을 중심으로 표준편차를 이용한 상/하단 밴드.',
    simpleDesc: '가격이 정상 범위 안에 있나 밖에 있나? 판단 도구',
    exampleIcon: 'mdi-car',
    exampleTitle: '🚗 도로 비유',
    example: `볼린저 밴드는 도로의 차선과 같습니다.<br/><br/>
- 가격이 하단 밴드 터치 → "싸졌네? 매수 신호!"<br/>
- 가격이 상단 밴드 터치 → "비싸졌네? 매도 신호!"<br/><br/>
📊 <strong>현재 설정:</strong><br/>
- 기간 20일: 20일 평균 기준<br/>
- 표준편차 2배: 밴드 폭 결정 (2배가 표준)`,
    hasDiagram: true,
    diagram: `<svg width="100%" height="140" viewBox="0 0 500 140" style="background: #FAFAFA; border-radius: 8px; border: 1px solid #E0E0E0;">
      <line x1="30" y1="25" x2="450" y2="25" stroke="#e53935" stroke-width="2" stroke-dasharray="5,3"/>
      <text x="460" y="29" font-size="12" fill="#e53935">상단 밴드 (비싸다!)</text>
      <line x1="30" y1="70" x2="450" y2="70" stroke="#1976D2" stroke-width="2"/>
      <text x="460" y="74" font-size="12" fill="#1976D2">중심선 (평균 가격)</text>
      <line x1="30" y1="115" x2="450" y2="115" stroke="#43a047" stroke-width="2" stroke-dasharray="5,3"/>
      <text x="460" y="119" font-size="12" fill="#43a047">하단 밴드 (싸다!)</text>
      <polyline points="40,70 100,55 160,90 220,108 280,80 340,45 400,65 440,85" fill="none" stroke="#FF9800" stroke-width="2.5"/>
      <circle cx="220" cy="108" r="8" fill="#43a047"/>
      <text x="195" y="135" font-size="11" fill="#43a047" font-weight="bold">매수 신호!</text>
      <circle cx="340" cy="45" r="8" fill="#e53935"/>
      <text x="315" y="35" font-size="11" fill="#e53935" font-weight="bold">매도 신호!</text>
      <circle cx="160" cy="90" r="4" fill="#FF9800"/>
      <text x="135" y="75" font-size="10" fill="#FF9800">🚗</text>
    </svg>`
  },
  { 
    category: 'indicator', 
    term: '거래량', 
    description: '일정 기간 동안 거래된 코인의 총량.',
    simpleDesc: '평소보다 거래가 얼마나 활발해야 진짜 신호로 볼 것인가?',
    exampleIcon: 'mdi-store',
    exampleTitle: '🏪 가게 비유',
    example: `평소 하루 100명 오는 가게에<br/>
- 오늘 140명 왔다 → "뭔가 있네! 관심 가져볼까?"<br/>
- 오늘 200명 왔다 → "대박 터졌다!"<br/>
- 오늘 80명 왔다 → "오늘은 조용하네..."<br/><br/>
📊 <strong>코인 예시:</strong><br/>
비트코인 평균 거래량: 1조원/일<br/>
- 140% 설정 시: 1.4조원 이상 거래되는 날에만 매수 신호 인정<br/>
- 거래량 적으면 "가짜 신호"일 수 있으니 무시<br/><br/>
<strong>왜 중요한가?</strong><br/>
거래량 없이 가격만 움직이면 → 세력의 조작일 수 있음<br/>
거래량 터지면서 움직이면 → 진짜 시장 반응!`
  },
  { 
    category: 'indicator', 
    term: '이격도', 
    description: '현재가가 기준가(이동평균선) 대비 얼마나 떨어져 있는지를 백분율로 표시한 값.',
    simpleDesc: '평균 가격보다 얼마나 떨어지면 살 것인가?',
    exampleIcon: 'mdi-tag-outline',
    exampleTitle: '🏷️ 마트 할인 예시',
    example: `평소 10,000원 하는 운동화가 있어요.<br/>
- -3% 설정: 9,700원 되면 구매<br/>
- -6% 설정: 9,400원 되면 구매<br/>
- -10% 설정: 9,000원 되면 구매<br/><br/>
숫자가 클수록(음수가 클수록)<br/>
→ 더 많이 떨어져야 삼<br/>
→ 거래 횟수 적어짐<br/>
→ 더 신중한 투자<br/><br/>
📊 <strong>코인 예시:</strong><br/>
비트코인 20일 평균: 1억원<br/>
- -6% 설정 시: 9,400만원 이하로 떨어지면 매수!`,
    table: {
      headers: ['설정값', '의미', '거래 빈도'],
      rows: [
        ['-3%', '조금만 떨어져도 삼', '많음 (공격적)'],
        ['<strong>-6%</strong>', '적당히 떨어지면 삼', '보통 ✅'],
        ['-10%', '많이 떨어져야 삼', '적음 (신중)']
      ]
    }
  },
  
  // ========== 거래 용어 ==========
  { 
    category: 'trading', 
    term: '매수', 
    description: '코인을 구매하는 것.',
    simpleDesc: '코인을 사는 것',
    exampleIcon: 'mdi-cart-plus',
    exampleTitle: '🛒 쇼핑 예시',
    example: `마트에서 물건 사는 것과 같아요!<br/><br/>
시스템이 자동으로 매수하는 조건:<br/>
1. 가격이 평균보다 충분히 떨어졌을 때<br/>
2. RSI가 낮을 때 (시장이 냉각되었을 때)<br/>
3. 거래량이 충분할 때<br/><br/>
<strong>예시:</strong><br/>
비트코인이 9,400만원으로 떨어지고<br/>
RSI가 28이고, 거래량이 평소의 150%라면<br/>
→ 시스템이 "지금이 기회!" 하고 자동 매수`
  },
  { 
    category: 'trading', 
    term: '매도', 
    description: '보유한 코인을 판매하는 것.',
    simpleDesc: '코인을 파는 것',
    exampleIcon: 'mdi-cash-register',
    exampleTitle: '💰 판매 예시',
    example: `가지고 있던 물건을 파는 것!<br/><br/>
시스템이 자동으로 매도하는 조건:<br/>
1. <strong>목표 수익률 달성:</strong> 4% 올랐을 때<br/>
2. <strong>손절매:</strong> -8% 떨어졌을 때<br/>
3. <strong>트레일링 스톱:</strong> 최고점에서 -4% 떨어졌을 때<br/><br/>
<strong>예시:</strong><br/>
9,400만원에 산 비트코인이<br/>
- 9,776만원(+4%)이 되면 → 자동 매도 (수익 실현!)<br/>
- 8,648만원(-8%)이 되면 → 자동 매도 (손실 제한!)`
  },
  { 
    category: 'trading', 
    term: '목표 수익률', 
    description: '매수가 대비 이 수익률에 도달하면 자동으로 매도.',
    simpleDesc: '얼마 오르면 팔 것인가?',
    exampleIcon: 'mdi-hand-coin',
    exampleTitle: '🤝 중고거래 예시',
    example: `10,000원에 산 물건을<br/>
- 3% 설정: 10,300원에 판매<br/>
- 4% 설정: 10,400원에 판매<br/>
- 10% 설정: 11,000원에 판매<br/><br/>
<strong>낮게 설정하면:</strong><br/>
✅ 자주 수익 실현<br/>
❌ 큰 상승 놓칠 수 있음<br/><br/>
<strong>높게 설정하면:</strong><br/>
✅ 큰 수익 가능<br/>
❌ 목표 도달 못하고 하락할 수 있음<br/><br/>
📊 <strong>코인 예시:</strong><br/>
9,400만원에 비트코인 샀다면<br/>
- 4% 설정 시: 9,776만원 되면 자동 매도!<br/><br/>
<!-- ★★★ [신규 추가] 수수료 안내 ★★★ -->
💡 <strong style="color: #4CAF50;">수수료 자동 반영!</strong><br/>
업비트 수수료(매수 0.05% + 매도 0.05%)가<br/>
자동으로 계산되어 실제 수익률 기준으로 매도됩니다.<br/>
예: 가격 4.1% 상승 시 → 실제 수익률 약 4.0%`,
    table: {
      headers: ['설정값', '특징', '적합한 상황'],
      rows: [
        ['2~3%', '빠른 수익 실현', '횡보장, 하락장'],
        ['<strong>4~5%</strong>', '균형잡힌 목표', '초보자 추천 ✅'],
        ['10%+', '큰 수익 노림', '상승장']
      ]
    }
  },
  { 
    category: 'trading', 
    term: '손절매 (Stop Loss)', 
    description: '손실을 제한하기 위해 일정 비율 하락 시 강제 매도.',
    simpleDesc: '얼마나 손해보면 포기하고 팔 것인가?',
    exampleIcon: 'mdi-slot-machine',
    exampleTitle: '🎰 도박 예시',
    example: `카지노에서 10만원 들고 갔는데<br/>
- -5% 설정: 9.5만원 되면 "그만!"<br/>
- -8% 설정: 9.2만원 되면 "그만!"<br/>
- -15% 설정: 8.5만원 되면 "그만!"<br/><br/>
<strong>⚠️ 왜 필요한가?</strong><br/>
손절매 없이 버티면...<br/>
10만원 → 5만원 → 2만원 → 0원 😭<br/><br/>
손절매 있으면...<br/>
10만원 → 9.2만원 → "여기서 멈춤!"<br/>
→ 남은 돈으로 다시 도전 가능<br/><br/>
📊 <strong>코인 예시:</strong><br/>
9,400만원에 비트코인 샀는데<br/>
- -8% 설정 시: 8,648만원 되면 자동 손절!<br/>
- 752,000원 손해로 제한 (전액 손실 방지)`,
    table: {
      headers: ['설정값', '특징', '멘탈 요구도'],
      rows: [
        ['-5%', '빠른 손절', '약함 (안전)'],
        ['<strong>-8%</strong>', '적당한 손절', '보통 ✅'],
        ['-15%', '느린 손절', '강함 (위험)']
      ]
    }
  },
   { 
    category: 'trading', 
    term: '거래 수수료', 
    description: '코인 매수/매도 시 거래소에 지불하는 비용.',
    simpleDesc: '거래할 때마다 내는 수수료',
    exampleIcon: 'mdi-percent',
    exampleTitle: '💰 수수료 계산 예시',
    example: `<strong>업비트 수수료:</strong> 매수 0.05% + 매도 0.05%<br/><br/>
<strong>100만원 투자 시:</strong><br/>
1. 매수: 100만원 - 수수료 500원 = 999,500원어치 매수<br/>
2. 가격 4% 상승: 평가금액 1,039,480원<br/>
3. 매도: 1,039,480원 - 수수료 520원 = 1,038,960원<br/>
4. <strong style="color: #4CAF50;">실제 수익: 38,960원 (약 3.9%)</strong><br/><br/>
⚠️ <strong>주의:</strong> 4% 상승해도 수수료 때문에 실제 수익은 약 3.9%!<br/><br/>
💡 <strong style="color: #2196F3;">시스템 자동 반영:</strong><br/>
목표 수익률 달성 여부 판단 시<br/>
수수료가 자동으로 계산됩니다.`,
    table: {
      headers: ['항목', '수수료 반영', '기준'],
      rows: [
        ['<strong>목표 수익률</strong>', '✅ 반영', '실제 수익률'],
        ['<strong>RSI 과매수 매도</strong>', '✅ 반영', '실제 수익률'],
        ['손절매', '❌ 미반영', '가격 변동률'],
        ['트레일링 스톱', '❌ 미반영', '최고가 대비 하락률']
      ]
    }
  },
  { 
    category: 'trading', 
    term: '종목당 최대 보유', 
    description: '한 코인을 최대 몇 번까지 나눠서 살 것인가?',
    simpleDesc: '한 코인을 최대 몇 번까지 나눠서 살 것인가?',
    exampleIcon: 'mdi-shopping',
    exampleTitle: '🛍️ 쇼핑 예시',
    example: `맘에 드는 가방이 있는데 가격이 계속 떨어져요.<br/>
- 1회 설정: 한 번만 사고 끝<br/>
- 2회 설정: 더 떨어지면 한 번 더 삼 (2번까지)<br/>
- 3회 설정: 최대 3번까지 나눠서 삼<br/><br/>
<strong>장점:</strong> 물타기로 평균 단가 낮출 수 있음<br/>
<strong>단점:</strong> 계속 떨어지면 손실 커짐<br/><br/>
📊 <strong>코인 예시:</strong><br/>
비트코인을 2번까지 나눠 사기 설정<br/>
1차 매수: 9,400만원에 50만원어치<br/>
2차 매수: 9,000만원에 50만원어치 (추가 하락 시)<br/>
→ 평균 단가: 9,200만원<br/><br/>
⚠️ <strong>1건 설정 + 소액 매수 시 주의:</strong><br/>
1회 매수 금액이 10,000원 미만이고 종목당 1건만 보유 시,<br/>
하락으로 평가금액이 5,000원 미만이 되면<br/>
합산할 대상이 없어 <strong>손절매가 실행되지 않을 수 있습니다.</strong><br/>
💡 종목당 최대 보유 2건 이상 또는 1회 매수 금액 10,000원 이상 권장`,
    table: {
      headers: ['설정값', '의미', '리스크'],
      rows: [
        ['1', '한 번만 삼', '낮음 (분산)'],
        ['<strong>2</strong>', '두 번까지', '보통 ✅'],
        ['3+', '여러 번', '높음 (집중)']
      ]
    }
  },
  { 
    category: 'trading', 
    term: '트레일링 스톱', 
    description: '최고가 대비 일정 비율 하락 시 매도. 수익을 보호하면서 상승 추세를 최대한 활용.',
    simpleDesc: '최고점에서 얼마 떨어지면 팔 것인가?',
    exampleIcon: 'mdi-target',
    exampleTitle: '🎯 롤러코스터 예시',
    example: `목표 수익률 4%인데, 코인이 10%까지 올랐어요!<br/>
- 트레일링 OFF: 4%에서 이미 팔았음 (6% 놓침 😢)<br/>
- 트레일링 ON (4%): 최고점(10%)에서 -4% 떨어진<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;6% 수익에서 팔림!<br/><br/>
📊 <strong>실제 예시:</strong><br/>
9,400만원에 매수 → 목표 4% = 9,776만원<br/><br/>
<strong>[트레일링 OFF]</strong><br/>
가격: 9,400 → 9,776 → 매도! (4% 수익)<br/>
이후: 9,900 → 1억 (놓침 😢)<br/><br/>
<strong>[트레일링 ON, 4%]</strong><br/>
가격: 9,400 → 9,776 → 9,900 → 1억 (최고점)<br/>
→ 9,600만원 (최고점 대비 -4%) → 매도!<br/>
최종: 약 2% 수익 (200만원)<br/><br/>
<strong style="color: #FF9800;">⚡ 활성화 조건:</strong><br/>
트레일링 스톱은 <strong>목표 수익률의 50% 또는 최소 1%</strong> 수익 확보 후 활성화됩니다.<br/>
- 목표 수익률 4% → 2% 수익부터 트레일링 활성화<br/>
- 목표 수익률 1% → 1% 수익부터 트레일링 활성화<br/>
→ 미미한 수익에서 조기 매도되는 것을 방지!`,
    table: {
      headers: ['설정', '장점', '단점'],
      rows: [
        ['OFF', '확실한 수익 확보', '추가 상승 놓침'],
        ['<strong>ON, 4%</strong>', '상승장에서 더 벌 수 있음', '하락장에선 효과 적음']
      ]
    }
  },
  
  { 
    category: 'trading', 
    term: '1회 매수 금액', 
    description: '고정 금액 매수 방식에서 각 코인에 매수할 금액 (원).',
    simpleDesc: '매수 신호가 발생했을 때, 각 코인에 얼마를 투자할 것인가?',
    exampleIcon: 'mdi-cash-multiple',
    exampleTitle: '💵 설정 예시',
    example: `<strong>고정 금액 매수 방식 (라운드로빈 OFF)</strong>에서<br/>
각 코인에 매수할 금액을 직접 설정합니다.<br/><br/>
📊 <strong>예시: 10,000원 설정 시</strong><br/>
BTC 매수 신호 → <strong>10,000원</strong> 매수<br/>
ETH 매수 신호 → <strong>10,000원</strong> 매수<br/>
XRP 매수 신호 → <strong>10,000원</strong> 매수<br/>
→ 총 30,000원 사용 (일일 한도 내에서)<br/><br/>
<strong>💡 라운드로빈 ON일 때는?</strong><br/>
이 설정값은 무시되고, 남은 일일 한도를<br/>
매수 신호 수로 자동 균등 분배합니다.<br/><br/>
⚠️ <strong>제한사항:</strong><br/>
- 최소: <strong>5,000원</strong> (업비트 최소 주문금액)<br/>
- 최대: <strong>1,000만원</strong><br/><br/>
💡 <strong>10,000원 미만 설정 시 참고:</strong><br/>
하락으로 평가금액이 5,000원 미만이 되면<br/>
업비트에서 개별 매도가 불가합니다.<br/>
이 경우 동일 코인 보유 건을 <strong>자동 합산하여 매도</strong> 처리합니다.<br/>
예) BTC 5,000원 × 2건 → 하락 시 4,000원 × 2건 = 8,000원 합산 매도`,
    table: {
      headers: ['설정값', '특징', '적합한 상황'],
      rows: [
        ['5,000원', '최소 금액', '소규모 테스트'],
        ['<strong>10,000원</strong>', '기본값', '초보자 추천 ✅'],
        ['50,000원+', '적극 투자', '확신 있는 전략']
      ]
    }
  }, 


  // ========== 리스크 관리 ==========
  { 
    category: 'risk', 
    term: '일일 최대 거래금액', 
    description: '하루에 사용할 수 있는 최대 투자 금액.',
    simpleDesc: '하루에 최대 얼마까지만 살 것인가?',
    exampleIcon: 'mdi-piggy-bank',
    exampleTitle: '💰 용돈 비유',
    example: `월급 100만원 받았는데<br/>
- 100% 설정: 하루에 100만원 다 써도 됨 (위험!)<br/>
- 20% 설정: 하루에 20만원까지만 씀<br/><br/>
📊 <strong>현재 설정:</strong><br/>
초기 자본 1,000,000원의 20% = 200,000원<br/>
→ 하루에 최대 20만원어치만 매수 가능<br/><br/>
<strong>왜 필요한가?</strong><br/>
"오늘 기회다!" 하고 한 번에 다 샀는데<br/>
다음날 더 떨어지면? 😭 살 돈이 없음!`
  },
  { 
    category: 'risk', 
    term: '일일 한도 복구', 
    description: '매도 시 해당 금액만큼 일일 매수 한도가 복구됩니다.',
    simpleDesc: '매도하면 그 금액만큼 오늘 살 수 있는 한도가 다시 생긴다',
    exampleIcon: 'mdi-refresh',
    exampleTitle: '🔄 복구 예시',
    example: `<strong>⚙️ 동작 방식:</strong><br/>
<strong style="color: #F44336;">OFF (기본):</strong> 매도해도 일일 한도 복구 안됨<br/>
<strong style="color: #4CAF50;">ON:</strong> 매도 금액만큼 한도 복구 (최대 일일 한도까지)<br/><br/>
📊 <strong>예시: 일일 한도 40만원</strong><br/><br/>
<table style="width: 100%; border-collapse: collapse; font-size: 13px;">
<tr style="background-color: #1565C0;">
<th style="padding: 8px; border: 1px solid #1976D2; color: white;">단계</th>
<th style="padding: 8px; border: 1px solid #1976D2; color: white;">OFF 상태</th>
<th style="padding: 8px; border: 1px solid #1976D2; color: white;">ON 상태</th>
</tr>
<tr>
<td style="padding: 8px; border: 1px solid #E0E0E0;">1. 20만원 매수</td>
<td style="padding: 8px; border: 1px solid #E0E0E0;">남은 한도: 20만원</td>
<td style="padding: 8px; border: 1px solid #E0E0E0;">남은 한도: 20만원</td>
</tr>
<tr>
<td style="padding: 8px; border: 1px solid #E0E0E0;">2. 20만원 매도</td>
<td style="padding: 8px; border: 1px solid #E0E0E0; color: #F44336;">남은 한도: 20만원 ❌</td>
<td style="padding: 8px; border: 1px solid #E0E0E0; color: #4CAF50;">남은 한도: 40만원 ✅</td>
</tr>
<tr>
<td style="padding: 8px; border: 1px solid #E0E0E0;">3. 20만원 매수</td>
<td style="padding: 8px; border: 1px solid #E0E0E0; color: #F44336;">남은 한도: 0원</td>
<td style="padding: 8px; border: 1px solid #E0E0E0; color: #4CAF50;">남은 한도: 20만원</td>
</tr>
</table><br/>
⚠️ <strong>주의:</strong> 일일 한도를 초과하는 금액은 복구되지 않습니다.<br/>
예: 남은 한도 20만원에서 50만원 매도 → 20만원만 복구`
  },
  { 
    category: 'risk', 
    term: '매수 방식 선택', 
    description: '라운드로빈(균등 분배) 또는 고정 금액 매수 방식을 선택합니다.',
    simpleDesc: '여러 코인에 어떻게 나눠서 살 것인가?',
    exampleIcon: 'mdi-scale-balance',
    exampleTitle: '⚖️ 비교 예시',
    example: `<strong>두 가지 매수 방식:</strong><br/><br/>
<strong style="color: #4CAF50;">🔄 라운드로빈 (ON):</strong><br/>
남은 일일 한도를 매수 신호 수로 <strong>균등 분배</strong>하여 매수<br/>
→ 여러 코인에 동시 매수 신호 시 자동 분산 투자<br/><br/>
<strong style="color: #2196F3;">💵 고정 금액 (OFF):</strong><br/>
'1회 매수 금액'에 설정한 금액만큼 <strong>정확히 매수</strong><br/>
→ 일일 한도 내에서 순차적으로 매수<br/><br/>
📊 <strong>예시: 일일 한도 200,000원, 고정 금액 10,000원</strong><br/>
<strong>BTC/ETH/XRP 3개 코인 매수 신호 발생!</strong><br/><br/>
<table style="width: 100%; border-collapse: collapse; font-size: 13px;">
<tr style="background-color: #1565C0;">
<th style="padding: 8px; border: 1px solid #1976D2; color: white;">항목</th>
<th style="padding: 8px; border: 1px solid #1976D2; color: white;">🔄 라운드로빈</th>
<th style="padding: 8px; border: 1px solid #1976D2; color: white;">💵 고정 금액</th>
</tr>
<tr>
<td style="padding: 8px; border: 1px solid #E0E0E0;">매수 금액 계산</td>
<td style="padding: 8px; border: 1px solid #E0E0E0;">200,000 ÷ 3 = 66,666원</td>
<td style="padding: 8px; border: 1px solid #E0E0E0;">고정 10,000원</td>
</tr>
<tr>
<td style="padding: 8px; border: 1px solid #E0E0E0;">BTC 매수</td>
<td style="padding: 8px; border: 1px solid #E0E0E0; color: #4CAF50;"><strong>66,666원</strong></td>
<td style="padding: 8px; border: 1px solid #E0E0E0; color: #2196F3;"><strong>10,000원</strong></td>
</tr>
<tr>
<td style="padding: 8px; border: 1px solid #E0E0E0;">ETH 매수</td>
<td style="padding: 8px; border: 1px solid #E0E0E0; color: #4CAF50;"><strong>66,666원</strong></td>
<td style="padding: 8px; border: 1px solid #E0E0E0; color: #2196F3;"><strong>10,000원</strong></td>
</tr>
<tr>
<td style="padding: 8px; border: 1px solid #E0E0E0;">XRP 매수</td>
<td style="padding: 8px; border: 1px solid #E0E0E0; color: #4CAF50;"><strong>66,666원</strong></td>
<td style="padding: 8px; border: 1px solid #E0E0E0; color: #2196F3;"><strong>10,000원</strong></td>
</tr>
<tr style="background-color: #FFF8E1;">
<td style="padding: 8px; border: 1px solid #E0E0E0; color: #E65100;"><strong>총 사용</strong></td>
<td style="padding: 8px; border: 1px solid #E0E0E0; color: #2E7D32;"><strong>199,998원 (≈100%)</strong></td>
<td style="padding: 8px; border: 1px solid #E0E0E0; color: #1565C0;"><strong>30,000원 (15%)</strong></td>
</tr>
</table><br/>
💡 <strong>언제 사용하나요?</strong><br/>
- <strong style="color: #4CAF50;">🔄 라운드로빈:</strong> 일일 한도를 최대한 활용하고 분산 투자할 때 (추천 ✅)<br/>
- <strong style="color: #2196F3;">💵 고정 금액:</strong> 정해진 금액만 투자하고 나머지를 보존할 때`,
    table: {
      headers: ['방식', '특징', '일일 한도 활용'],
      rows: [
        ['<strong>🔄 라운드로빈</strong>', '균등 분배, 분산 투자', '최대한 활용 ✅'],
        ['<strong>💵 고정 금액</strong>', '정확한 금액 제어', '보수적 활용']
      ]
    }
  },
  { 
    category: 'risk', 
    term: '단일 종목 최대 비중', 
    description: '전체 투자금 대비 한 코인에 투자할 수 있는 최대 비율.',
    simpleDesc: '한 코인에 최대 얼마까지 투자할 것인가?',
    exampleIcon: 'mdi-egg',
    exampleTitle: '🥚 계란 비유',
    example: `"계란을 한 바구니에 담지 마라"<br/><br/>
100만원이 있을 때:<br/>
- 100% 설정: 비트코인에 100만원 몰빵 가능<br/>
- 25% 설정: 비트코인에 최대 25만원까지만!<br/>
&nbsp;&nbsp;&nbsp;&nbsp;나머지는 다른 코인에 분산<br/><br/>
📊 <strong>현재 설정:</strong><br/>
1,000,000원 × 25% = 250,000원<br/>
→ 비트코인에 최대 25만원까지만 투자 가능<br/><br/>
<strong>왜 필요한가?</strong><br/>
비트코인에 100만원 몰빵 → -30% → 30만원 손실<br/>
4개 코인에 25만원씩 → -30% → 7.5만원 손실`
  },
  { 
    category: 'risk', 
    term: '시장 추세 필터', 
    description: 'BTC가 20일 이동평균선 아래일 때 전체 매수를 중단.',
    simpleDesc: '비트코인이 떨어지고 있으면 아무것도 안 산다',
    exampleIcon: 'mdi-weather-cloudy',
    exampleTitle: '🌧️ 날씨 비유',
    example: `비가 오면 소풍 안 가는 것처럼!<br/><br/>
비트코인 = 코인 시장의 날씨<br/>
- BTC가 20일 평균 위 = ☀️ 맑음 → 매수 OK<br/>
- BTC가 20일 평균 아래 = 🌧️ 비 옴 → 매수 중단!<br/><br/>
📊 <strong>예시:</strong><br/>
BTC 20일 평균: 1억원<br/>
현재 BTC 가격: 9,500만원 (평균 아래)<br/>
→ 시장 추세 필터 ON이면 모든 매수 중단!<br/><br/>
<strong>왜 필요한가?</strong><br/>
비트코인이 떨어지면 대부분의 알트코인도 떨어짐<br/>
하락장에서 매수하면 손실 확률 높음`
  },
  { 
    category: 'risk', 
    term: '긴급 정지 - 일일 손실률', 
    description: '오늘 손실이 설정값에 도달하면 오늘 거래 중단.',
    simpleDesc: '오늘 손실이 이 정도면 오늘은 거래 중단!',
    exampleIcon: 'mdi-car-brake-alert',
    exampleTitle: '🚨 비상 브레이크 비유',
    example: `자동차가 너무 빨리 가면 비상 브레이크!<br/>
투자도 손실이 너무 커지면 "오늘은 그만!"<br/><br/>
📊 <strong>현재 설정:</strong><br/>
1,000,000원 × -5% = -50,000원<br/>
→ 오늘 손실이 5만원 넘으면 자동으로 거래 중단<br/><br/>
<strong>왜 필요한가?</strong><br/>
"오늘 손해 봤으니 더 사서 만회해야지!"<br/>
→ 복수 매매 → 더 큰 손실 😭<br/><br/>
긴급 정지 있으면:<br/>
"5만원 잃었으니 오늘은 쉬자"<br/>
→ 냉정해진 후 내일 다시 시작`
  },
  { 
    category: 'risk', 
    term: '연속 손절 제한', 
    description: '동일 코인에서 연속으로 손절이 발생하면 해당 코인 매수를 일시 금지.',
    simpleDesc: '같은 코인에서 계속 손해보면 잠시 쉬어간다',
    exampleIcon: 'mdi-timer-sand',
    exampleTitle: '⏳ 휴식 비유',
    example: `같은 실수를 3번 연속 하면 잠시 쉬어!<br/><br/>
📊 <strong>예시 (3회 설정):</strong><br/>
비트코인 1차 매수 → 손절 😢<br/>
비트코인 2차 매수 → 손절 😢<br/>
비트코인 3차 매수 → 손절 😢<br/>
→ "비트코인 3연속 손절! 잠시 매수 금지!"<br/><br/>
<strong>왜 필요한가?</strong><br/>
특정 코인이 계속 떨어지는 중인데<br/>
계속 사면 계속 손해봄<br/>
→ 잠시 쉬면서 추세 바뀔 때까지 기다림`
  },
  { 
    category: 'risk', 
    term: '누적 손실 긴급정지', 
    description: '초기 자본 대비 누적 손실이 설정값에 도달하면 모든 거래를 중단.',
    simpleDesc: '전체 손실이 이 정도면 완전히 멈춘다',
    exampleIcon: 'mdi-stop-circle',
    exampleTitle: '🛑 완전 정지 비유',
    example: `일일 긴급정지가 "오늘 그만"이라면<br/>
누적 긴급정지는 "전체 그만!"<br/><br/>
📊 <strong>현재 설정 (-10%):</strong><br/>
초기 자본 100만원 × -10% = -10만원<br/>
→ 누적 손실이 10만원 넘으면 모든 거래 완전 중단<br/><br/>
<strong>예시:</strong><br/>
- 1일차: -3만원<br/>
- 2일차: -2만원<br/>
- 3일차: -4만원<br/>
- 누적: -9만원 → 아직 거래 가능<br/>
- 4일차: -2만원<br/>
- 누적: -11만원 → 🛑 완전 중단!<br/><br/>
<strong>왜 필요한가?</strong><br/>
최악의 경우에도 원금의 90%는 지킴!`
  },
  
  // ========== 성과 지표 ==========
  { 
    category: 'performance', 
    term: '수익률', 
    description: '투자 대비 수익의 비율.',
    simpleDesc: '투자한 돈 대비 얼마나 벌었나?',
    exampleIcon: 'mdi-percent',
    exampleTitle: '📊 계산 예시',
    example: `100만원 투자해서 105만원이 됐다면<br/>
수익률 = (105만 - 100만) / 100만 × 100%<br/>
= 5만 / 100만 × 100% = <strong>5%</strong><br/><br/>
<strong>반대로 95만원이 됐다면:</strong><br/>
수익률 = (95만 - 100만) / 100만 × 100%<br/>
= -5만 / 100만 × 100% = <strong>-5%</strong>`
  },
  { 
    category: 'performance', 
    term: '승률', 
    description: '전체 거래 중 수익이 발생한 거래의 비율.',
    simpleDesc: '거래 중 몇 번이나 이겼나?',
    exampleIcon: 'mdi-trophy',
    exampleTitle: '🏆 예시',
    example: `10번 거래했는데<br/>
- 6번 수익, 4번 손실<br/>
→ 승률 = 6/10 × 100% = <strong>60%</strong><br/><br/>
<strong>주의:</strong><br/>
승률이 높아도 손실이 클 수 있음!<br/>
- 9번 +1만원, 1번 -20만원<br/>
→ 승률 90%지만 손실 -11만원 😢`,
    table: {
      headers: ['승률', '평가', '의미'],
      rows: [
        ['70%+', '매우 좋음', '안정적인 전략'],
        ['<strong>50~70%</strong>', '양호', '일반적인 수준'],
        ['50% 미만', '주의', '전략 점검 필요']
      ]
    }
  },
  { 
    category: 'performance', 
    term: 'MDD (최대 낙폭)', 
    description: '특정 기간 동안 고점에서 저점까지의 최대 하락폭.',
    simpleDesc: '최고점에서 최저점까지 얼마나 떨어졌나?',
    exampleIcon: 'mdi-chart-line-variant',
    exampleTitle: '📉 롤러코스터 비유',
    example: `자산이 이렇게 변했다면:<br/>
100만 → 120만 → 90만 → 110만<br/><br/>
최고점: 120만원<br/>
그 후 최저점: 90만원<br/>
MDD = (120만 - 90만) / 120만 × 100% = <strong>25%</strong><br/><br/>
<strong>의미:</strong><br/>
"가장 힘들 때 25% 떨어졌다"<br/>
→ 그 기간 동안 버틸 멘탈이 필요함!`,
    table: {
      headers: ['MDD', '평가', '의미'],
      rows: [
        ['10% 이하', '매우 안정', '초보자 적합'],
        ['<strong>10~20%</strong>', '양호', '일반적인 수준'],
        ['20~30%', '주의', '변동성 큼'],
        ['30%+', '위험', '전략 재검토 필요']
      ]
    }
  },
  { 
    category: 'performance', 
    term: '샤프 비율', 
    description: '위험 대비 수익률. 높을수록 효율적인 전략.',
    simpleDesc: '위험 대비 얼마나 효율적으로 벌었나?',
    exampleIcon: 'mdi-scale-balance',
    exampleTitle: '⚖️ 비교 예시',
    example: `두 전략 비교:<br/><br/>
<strong>전략 A:</strong> 수익 10%, 변동성(위험) 5%<br/>
→ 샤프 = 10/5 = <strong>2.0</strong><br/><br/>
<strong>전략 B:</strong> 수익 10%, 변동성(위험) 20%<br/>
→ 샤프 = 10/20 = <strong>0.5</strong><br/><br/>
같은 10% 수익이지만 A가 훨씬 효율적!<br/>
(적은 위험으로 같은 수익을 얻음)`,
    table: {
      headers: ['샤프 비율', '평가', '의미'],
      rows: [
        ['2.0+', '매우 우수', '뛰어난 전략'],
        ['<strong>1.0~2.0</strong>', '양호', '괜찮은 전략'],
        ['0~1.0', '보통', '개선 여지 있음'],
        ['0 미만', '나쁨', '전략 재검토 필요']
      ]
    }
  },
  { 
    category: 'performance', 
    term: '손익비', 
    description: '평균 수익 대비 평균 손실의 비율.',
    simpleDesc: '이길 때 버는 돈 vs 질 때 잃는 돈',
    exampleIcon: 'mdi-compare',
    exampleTitle: '🎲 도박 비유',
    example: `10번 거래 결과:<br/>
- 이긴 5번: 평균 +3만원<br/>
- 진 5번: 평균 -2만원<br/><br/>
손익비 = 3만 / 2만 = <strong>1.5</strong><br/><br/>
<strong>의미:</strong><br/>
이길 때 3만원 벌고, 질 때 2만원 잃으니<br/>
승률 50%여도 돈을 번다!<br/>
(5×3만 - 5×2만 = +5만원)`,
    table: {
      headers: ['손익비', '평가', '최소 필요 승률'],
      rows: [
        ['3.0+', '매우 우수', '25%만 이겨도 수익'],
        ['<strong>1.5~3.0</strong>', '양호', '40% 이상 필요'],
        ['1.0~1.5', '보통', '50% 이상 필요'],
        ['1.0 미만', '주의', '높은 승률 필요']
      ]
    }
  }
])

const faqList = ref([
  { category: '계정', question: '비밀번호를 잊어버렸어요', answer: '로그인 페이지에서 "비밀번호 찾기"를 클릭하여 등록된 이메일로 재설정 링크를 받을 수 있습니다.' },
  { category: '계정', question: '2FA 인증 코드가 맞지 않아요', answer: '스마트폰의 시간이 정확한지 확인하세요. 자동 시간 설정을 활성화하면 해결되는 경우가 많습니다.' },
  { category: 'API', question: 'API 키 등록이 안 돼요', answer: '업비트에서 발급받은 키가 정확한지 확인하세요. Access Key와 Secret Key를 바꿔서 입력하지 않았는지도 확인하세요.' },
  { category: 'API', question: 'API 권한은 어떻게 설정해야 하나요?', answer: '<strong>자산조회, 주문조회, 주문, 입금조회, 출금조회</strong> 권한을 부여하세요. 입금/출금 조회는 불입금액 자동 계산에 사용됩니다. <span style="color: red;">출금(실행) 권한은 절대 부여하지 마세요!</span>' },
  { category: '거래', question: '자동매매가 작동하지 않아요', answer: '1) API 키가 정상 등록되었는지<br>2) 거래 설정이 완료되었는지<br>3) 봇이 "실행 중" 상태인지 확인하세요.' },
  { category: '거래', question: '매수 조건이 충족되었는데 매수가 안 돼요', answer: '일일 한도 초과, 종목별 최대 보유 건수 초과, 또는 잔고 부족일 수 있습니다.' },
  { category: '거래', question: '급락장 보호 기능은 언제 사용하나요?', answer: '시장 전체가 하락할 때 손실을 줄이기 위한 기능입니다.' },
  { category: '설정', question: '권장 설정값이 있나요?', answer: '초보자는 기본값(매수 -6%, 매도 +5%, 손절 -10%)으로 시작하세요.' },
  { category: '설정', question: '트레일링 스톱이 뭔가요?', answer: '최고가 대비 일정 비율 하락 시 매도하는 기능입니다.' },
  { category: '수익', question: '평가 수익과 실현 수익의 차이는?', answer: '<strong>평가 수익</strong>: 현재 보유 중인 코인의 미실현 손익<br><strong>실현 수익</strong>: 매도 완료된 거래의 확정 손익' },
  { category: '수익', question: '수수료는 얼마나 빠지나요?', answer: '업비트 기준 매수/매도 각 0.05%입니다.' },
  { category: '수익', question: '불입금액은 어떻게 계산되나요?', answer: '자동매매 첫 거래 직전의 업비트 KRW 잔고가 초기 불입금액으로 자동 저장됩니다. 이후 원화 입금/출금 시 매일 스냅샷에 자동 반영됩니다. API 키에 <strong>입금 조회, 출금 조회</strong> 권한이 필요합니다.' },
  { category: '수익', question: '자동매매 시작 전 보유 중인 코인은 불입금액에 포함되나요?', answer: '아니요. 자동매매 시작 시점의 <strong>KRW(원화) 잔고</strong>만 초기 불입금액으로 산정됩니다. 기존에 보유 중이던 코인은 자동매매 시스템이 매수한 것이 아니므로 불입금액에 포함되지 않습니다.' },
  { category: '보안', question: 'API 키가 유출되면 어떻게 하나요?', answer: '즉시 업비트에서 해당 API 키를 폐기하고 새로 발급받으세요.' },
  { category: '보안', question: 'IP 화이트리스트는 꼭 설정해야 하나요?', answer: '권장하지만 필수는 아닙니다. 설정하면 보안이 강화됩니다.' },
  { category: '기타', question: '최소 투자금은 얼마인가요?', answer: '업비트 최소 주문 금액인 <strong>5,000원</strong> 이상이면 가능합니다.' },
  { category: '기타', question: '봇은 언제 실행되나요?', answer: '봇은 <strong>5분 간격</strong>으로 자동 실행됩니다.' },
  // ⭐ 추가: Phase 2 주식/ETF 관련 FAQ
  { category: '거래', question: '주식/ETF 자동매매는 어떻게 시작하나요?', answer: '프로필 설정에서 <strong>KIS API 키</strong>를 등록하고, <strong>주식 거래 설정</strong> 페이지에서 종목과 매매 조건을 설정하면 됩니다. 처음에는 모의투자 모드로 테스트를 권장합니다.' },
  { category: '거래', question: '레버리지 ETF를 장기 보유하면 안 되나요?', answer: '레버리지 ETF는 <strong>일일 수익률의 2배</strong>를 추종하므로, 장기 보유 시 <strong>복리 효과에 의한 가치 침식(Volatility Drag)</strong>이 발생합니다. 횡보장에서 특히 손실이 누적되므로 <strong>최대 20거래일</strong> 내 청산을 권장합니다.' },
  { category: '거래', question: '환노출형과 환헤지형 ETF의 차이는?', answer: '<strong>환노출형(TIGER)</strong>: 원달러 환율 변동에 영향을 받습니다. 환율 상승 시 추가 수익, 하락 시 추가 손실.<br><strong>환헤지형(KODEX)</strong>: 환율 변동 영향을 최소화합니다. 순수 지수 수익률만 추종합니다.<br>환율 상승기에는 환노출형이, 환율 하락기에는 환헤지형이 유리합니다.' },
  { category: 'API', question: 'KIS API 키는 어떻게 발급받나요?', answer: '<a href="https://apiportal.koreainvestment.com" target="_blank">한국투자증권 개발자 포털</a>에 가입 후 앱을 등록하면 APP KEY / APP SECRET을 발급받을 수 있습니다. 모의투자와 실전투자 키가 별도입니다. API 사용료는 <strong>무료</strong>이며, 3개월 미거래 시 자동 해지됩니다.' },
  { category: '설정', question: '주식과 코인 설정값이 다른 이유는?', answer: '주식/ETF는 암호화폐 대비 변동성이 낮습니다. 매수 기준: 코인 -6% → 주식 -3%, 목표 수익률: 코인 +4% → 주식 +2.5%, 손절매: 코인 -8% → 주식 -5%, RSI: 코인 32/68 → 주식 35/65로 조정되어 있습니다.' },
  // ⭐ 추가: 주식 거래 내역 관련 FAQ
  { category: '거래', question: '주식 거래 내역에서 보유일이 주황/빨간색으로 표시돼요', answer: '레버리지 ETF 장기 보유 시 가치 침식(Decay)이 발생하므로 경고를 표시합니다.<br><strong>주황색(15일 이상)</strong>: 청산을 고려할 시점입니다.<br><strong>빨간색(20일 이상)</strong>: 즉시 청산을 권장합니다.<br>수동 매도 버튼으로 직접 매도하거나, 봇이 자동으로 강제 매도합니다.' },
  { category: '거래', question: '주식 거래 내역에서 수동 매도는 어떻게 하나요?', answer: '거래 목록에서 보유 중인 종목의 <strong>매도</strong> 버튼을 클릭하면 매도 가격을 입력할 수 있습니다. 현재가가 조회되면 자동으로 입력됩니다. KIS API 모의투자 모드에서는 실제 주문이 체결되지 않습니다.' },
  { category: '거래', question: '주식 거래 내역 상세 정보에서 무엇을 확인할 수 있나요?', answer: '상세 버튼을 클릭하면 종목코드, 종목명, 수량, 매수가, 총 금액, 수수료, 보유일수, 매도가, 실현손익, 환율(환노출형 ETF), 거래 사유(메모) 등 모든 거래 정보를 확인할 수 있습니다.' }
])

const commonIssues = ref([
  { title: '로그인이 안 돼요', description: '비밀번호가 5회 이상 틀리면 계정이 잠깁니다.', solution: '관리자에게 문의하여 계정 잠금을 해제받으세요.' },
  { title: '페이지가 느리게 로딩돼요', description: '네트워크 상태가 불안정하거나 서버 부하가 높을 수 있습니다.', solution: '잠시 후 다시 시도하거나, 브라우저 캐시를 삭제해보세요.' },
  { title: '데이터가 업데이트되지 않아요', description: '실시간 데이터는 일정 주기로 갱신됩니다.', solution: '새로고침 버튼을 클릭하거나 페이지를 다시 로드하세요.' }
])

const apiIssues = ref([
  { title: 'API 키 등록 실패', description: 'Access Key 또는 Secret Key가 올바르지 않습니다.', solution: '업비트에서 키를 다시 확인하고, 복사 시 공백이 포함되지 않았는지 확인하세요.' },
  { title: '잔고 조회 실패', description: 'API 키에 자산조회 권한이 없을 수 있습니다.', solution: '업비트에서 API 키 권한을 확인하고, 자산조회 권한이 있는지 확인하세요.' },
  { title: '주문 실패', description: '잔고 부족, 최소 주문 금액 미달, 또는 API 권한 문제일 수 있습니다.', solution: '잔고와 최소 주문 금액(5,000원)을 확인하고, API에 주문 권한이 있는지 확인하세요.' },
  // ⭐ 추가: KIS API 관련 문제 해결
  { title: 'KIS API 키 등록 실패', description: 'APP KEY 또는 APP SECRET이 올바르지 않습니다.', solution: '한국투자증권 개발자 포털에서 키를 다시 확인하세요. 모의투자 키와 실전투자 키가 다르므로 모드에 맞는 키를 입력했는지 확인하세요.' },
  { title: 'KIS API 토큰 발급 실패', description: 'API 인증 토큰 발급이 실패했습니다.', solution: 'APP KEY, APP SECRET, 계좌번호가 정확한지 확인하세요. 3개월 미거래 시 API가 자동 해지되므로 개발자 포털에서 상태를 확인하세요.' },
  { title: '장 시간 외 주문 불가', description: '한국 증시 거래 시간(09:00~15:30) 외에는 주문이 불가합니다.', solution: '주식 자동매매는 장 시간 내에만 실행됩니다. 시간외 거래(08:30~09:00, 15:40~16:00)는 현재 미지원입니다.' }
])

const filteredGlossary = (category: string) => {
  return glossaryTerms.value.filter(term => {
    const matchCategory = term.category === category
    const matchSearch = !glossarySearch.value || 
      term.term.toLowerCase().includes(glossarySearch.value.toLowerCase()) ||
      term.description.toLowerCase().includes(glossarySearch.value.toLowerCase()) ||
      term.simpleDesc.toLowerCase().includes(glossarySearch.value.toLowerCase())
    return matchCategory && matchSearch
  })
}

const filteredFaqs = computed(() => {
  if (!faqSearch.value) return faqList.value
  const search = faqSearch.value.toLowerCase()
  return faqList.value.filter(faq => 
    faq.question.toLowerCase().includes(search) ||
    faq.answer.toLowerCase().includes(search) ||
    faq.category.toLowerCase().includes(search)
  )
})

watch(glossarySearch, (newValue) => {
  if (newValue && newValue.length > 0) {
    const panelsToOpen: number[] = []
    const categories = ['indicator', 'trading', 'risk', 'performance']
    
    categories.forEach((category, index) => {
      const hasResults = glossaryTerms.value.some(term => {
        if (term.category !== category) return false
        const search = newValue.toLowerCase()
        return term.term.toLowerCase().includes(search) || 
               term.description.toLowerCase().includes(search) ||
               term.simpleDesc.toLowerCase().includes(search)
      })
      if (hasResults) {
        panelsToOpen.push(index)
      }
    })
    
    glossaryPanel.value = panelsToOpen
  }
})

const getCategoryColor = (category: string) => {
  const colors: Record<string, string> = {
    '계정': 'blue', 'API': 'orange', '거래': 'green',
    '설정': 'purple', '수익': 'teal', '보안': 'red', '기타': 'grey'
  }
  return colors[category] || 'grey'
}
</script>

<style scoped>
.help-tabs {
  border-bottom: none !important;
  background-color: transparent !important;
  flex-grow: 0 !important;
  width: auto !important;
  overflow-x: auto;
}

.help-tabs :deep(.v-tabs__container) {
  flex-grow: 0 !important;
}

.help-tabs :deep(.v-tabs-slider-wrapper),
.help-tabs :deep(.v-tab__slider) {
  display: none !important;
}

.help-tabs :deep(.v-slide-group__content) {
  background-color: transparent !important;
  flex-wrap: nowrap;
}

.help-tab {
  min-width: 130px;
  max-width: 160px;
  border: 1px solid #CFD8DC;
  border-bottom: 1px solid #CFD8DC;
  margin-right: 4px;
  border-radius: 8px 8px 0 0;
  background-color: #B0BEC5 !important;
  color: #37474F !important;
  flex-grow: 0 !important;
}

.help-tab.v-tab--selected {
  background-color: #546E7A !important;
  color: white !important;
  border-color: #546E7A;
}

.card-no-top-radius {
  border-top-left-radius: 0 !important;
  border-top-right-radius: 0 !important;
}

.guide-alert {
  border-radius: 8px;
}

.guide-alert p {
  line-height: 1.7;
}

.troubleshoot-alert {
  border-radius: 8px;
}

.troubleshoot-alert p {
  line-height: 1.6;
}

.glossary-detail {
  background-color: #FAFAFA;
  border-radius: 8px;
}

.glossary-section {
  padding: 12px;
  background-color: white;
  border-radius: 6px;
  border: 1px solid #E0E0E0;
}

.glossary-example-card {
  background-color: #263238 !important;
  border-color: #37474F !important;
}

.glossary-example-card :deep(.v-card-text) {
  color: #ECEFF1;
}

.glossary-example {
  font-family: 'Noto Sans KR', sans-serif;
  line-height: 1.8;
  color: #CFD8DC;
}

.glossary-example strong {
  color: #4CAF50;
}

.glossary-diagram {
  background: white;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #E0E0E0;
}

.glossary-table {
  border: 1px solid #E0E0E0;
  border-radius: 8px;
  overflow: hidden;
}

.glossary-table th {
  background-color: #ECEFF1 !important;
  font-weight: 600;
}

.glossary-table td {
  border-bottom: 1px solid #EEEEEE;
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

@media (max-width: 600px) {
  .help-tab {
    min-width: 100px;
    max-width: 120px;
    padding: 0 12px;
  }
}

/* ⭐ Day 32 추가: Stepper 스타일 개선 */
.api-stepper :deep(.v-stepper-header) {
  background-color: #B0BEC5 !important;
  border-radius: 8px;
  padding: 12px 0;
}

.api-stepper :deep(.v-stepper-item__title) {
  color: #263238 !important;
  font-weight: 600;
}

.api-stepper :deep(.v-stepper-item__subtitle) {
  color: #455A64 !important;
}

.api-stepper :deep(.v-stepper__wrapper) {
  background-color: transparent;
}

.api-stepper :deep(.v-stepper-item__avatar) {
  background-color: #5C6BC0 !important;
  color: white !important;
}

.api-stepper :deep(.v-stepper-item--complete .v-stepper-item__avatar) {
  background-color: #43A047 !important;
}

.api-stepper :deep(.v-stepper-item--selected .v-stepper-item__avatar) {
  background-color: #3949AB !important;
}

.api-stepper :deep(.v-stepper-actions .v-btn) {
  background-color: #5C6BC0 !important;
  color: white !important;
}

.api-stepper :deep(.v-stepper-actions .v-btn:hover) {
  background-color: #3F51B5 !important;
}
:deep(.help-content .help-intro) {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e0e0e0;
  color: #424242;
  font-size: 14px;
}

:deep(.help-content .help-item) {
  margin-bottom: 16px;
  padding-left: 8px;
}

:deep(.help-content .help-bullet) {
  color: #1565C0;
  font-weight: bold;
  margin-right: 6px;
}

:deep(.help-content .help-desc) {
  display: block;
  padding-left: 20px;
  margin-top: 4px;
  color: #616161;
  font-size: 13px;
}

:deep(.help-content .help-note) {
  margin-top: 16px;
  padding: 10px 12px;
  background-color: #FFF8E1;
  border-left: 3px solid #FFA000;
  border-radius: 4px;
  color: #5D4037;
  font-size: 13px;
}
</style>
