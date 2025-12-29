# 코인 자동매매 시스템

## 📋 프로젝트 개요
- **목적**: 업비트 API를 활용한 개인용 자동매매 시스템
- **규모**: 5명 사용자
- **핵심 가치**: 안정적인 수익 창출 + 최고 수준의 보안
- **개발 기간**: 9주 (Phase 1: 4주, Phase 2: 3주, Phase 3: 2주)

---

## 🏗 기술 스택

### Backend
- Java 17
- Spring Boot 3.2.x
- Spring Security 6
- Spring Data JPA
- MySQL 8.0
- Redis 7.x

### Frontend
- Vue.js 3 (Composition API)
- TypeScript
- Vuetify 3
- Pinia (상태 관리)
- Axios

### Infrastructure
- Docker + Docker Compose
- Nginx (리버스 프록시)

---

## 📅 개발 진행 상황

### ✅ Day 1 (2024-10-11) - 개발환경 구축
**완료 항목:**
- 프로젝트 디렉토리 구조 설계
- Docker Compose 환경 설정 (MySQL, Redis)
- 데이터베이스 스키마 초기 설계 (7개 테이블)
- Git 저장소 설정 및 보안 강화
- 환경변수 템플릿 작성

**결과:**
- 개발 인프라 완성
- 데이터베이스 기본 구조 확립

---

### ✅ Day 2 (2025-10-12) - Spring Boot 프로젝트 초기화
**완료 항목:**
- Spring Boot 3.2.5 프로젝트 생성
- Maven 의존성 설정 (Security, JPA, Redis, JWT, QueryDSL)
- application.yml 설정 (DB, Redis, JWT, Upbit API)
- Spring Security 기본 설정
- HealthController 구현 (시스템 상태 체크)
- Vue.js 프론트엔드 기본 구조 생성
- Docker 이미지 최적화 (Multi-stage build)
- MySQL 초기화 스크립트 완성

**테스트 결과:**
- ✅ MySQL 컨테이너 정상 실행
- ✅ Redis 컨테이너 정상 실행
- ✅ Backend 컨테이너 정상 실행
- ✅ Frontend 컨테이너 정상 실행
- ✅ Health Check API 응답 확인

---

### ✅ Day 3 (2025-10-13) - 사용자 인증 시스템
**완료 항목:**
- JWT 기반 인증 시스템 구현
  - JwtUtil: 토큰 생성/검증
  - JwtAuthenticationFilter: 자동 인증
- 사용자 관리 API
  - 회원가입 (입력값 검증)
  - 로그인 (JWT 발급)
  - 프로필 조회/수정
  - 비밀번호 변경
- API 키 암호화 저장
  - AES-256 암호화
  - 업비트 Access Key/Secret Key 보호
- 전역 예외 처리 핸들러
- CORS 설정

**보안 기능:**
- 비밀번호: BCrypt 해싱 (strength 10)
- API 키: AES-256 암호화
- JWT: HS512 알고리즘, 30분 만료
- 세션: Stateless 관리

**API 엔드포인트:**
- POST /api/auth/signup - 회원가입
- POST /api/auth/login - 로그인
- GET /api/auth/validate - 토큰 검증
- GET /api/user/profile - 프로필 조회
- PUT /api/user/profile - 프로필 수정
- POST /api/user/api-keys - API 키 등록
- DELETE /api/user/api-keys - API 키 삭제

**테스트 완료:**
- ✅ 회원가입 및 토큰 발급
- ✅ 로그인 및 인증
- ✅ 프로필 조회/수정
- ✅ API 키 암호화 저장/삭제

---

### ✅ Day 4 (2025-10-19) - 업비트 API 연동
**완료 항목:**
- UpbitApiService 구현
  - Auth0 JWT로 업비트 API 인증
  - Spring WebClient로 비동기 HTTP 통신
- DTO 클래스 생성
  - UpbitMarketDTO: 마켓 코드
  - UpbitTickerDTO: 현재가 정보
  - UpbitAccountDTO: 계좌 정보
  - UpbitOrderDTO: 주문 정보
- CoinInfo 엔티티 및 Repository
- CoinInfoService: 코인 정보 관리
- 테스트 Controller 생성

**API 테스트:**
- ✅ 마켓 코드 조회 (218개 코인)
- ✅ 현재가 조회 (BTC, ETH)
- ✅ 코인 정보 DB 업데이트
- ✅ 활성 코인 목록 조회

**엔드포인트:**
- GET /api/upbit/test/markets - 마켓 코드
- GET /api/upbit/test/ticker - 현재가
- POST /api/upbit/test/update-coins - DB 업데이트
- GET /api/upbit/test/active-coins - 활성 코인
- GET /api/upbit/test/accounts - 계좌 조회

---

### ✅ Day 5 (2025-10-20) - 거래 설정 및 코인 정보 API
**완료 항목:**
- 거래 설정(Trading Settings) CRUD API
  - TradingSettingDTO (Jakarta Validation)
  - TradingSettingService
  - TradingSettingController
- 코인 정보 조회 API
  - 활성 코인 목록
  - 현재가 조회 (단일/다중)
  - 계좌 정보 조회
- 보안 강화
  - 공개/인증 API 분리
  - 관리자 전용 API (ROLE_ADMIN)

**거래 설정 필드:**
- coinSymbols: 거래할 코인 목록
- basePeriod: 이동평균선 기간 (7~30일)
- buyThresholdPct: 매수 기준 (%)
- sellTargetPct: 목표 수익률 (%)
- stopLossPct: 손절매 기준 (%)
- maxHoldingsPerCoin: 종목당 최대 보유
- dailyLimitAmount: 일일 거래 한도
- useTrailingStop: 트레일링 스톱 사용
- useAiAnalysis: AI 뉴스 분석 사용

**API 엔드포인트:**
| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| GET | /api/trading-settings | ✅ | 설정 조회 |
| POST | /api/trading-settings | ✅ | 설정 생성 |
| PUT | /api/trading-settings | ✅ | 설정 수정 |
| DELETE | /api/trading-settings | ✅ | 설정 삭제 |
| GET | /api/coins/active | ❌ | 활성 코인 목록 |
| GET | /api/coins/{symbol}/price | ❌ | 현재가 조회 |
| GET | /api/coins/prices | ❌ | 다중 현재가 |
| GET | /api/coins/accounts | ✅ | 계좌 정보 |
| POST | /api/coins/update | 🔐 | DB 업데이트 (관리자) |

**테스트 완료:**
- ✅ 거래 설정 생성/조회/수정/삭제
- ✅ 활성 코인 목록 조회
- ✅ 현재가 조회 (단일/다중)
- ✅ 계좌 정보 조회

---

### ✅ Day 6 (2025-10-26) - Vue.js 프론트엔드 핵심 페이지
**완료 항목:**
- 프로젝트 구조 설정
  - TypeScript 타입 시스템
  - Pinia Store (auth, coin)
  - Axios API 클라이언트
  - Vue Router 가드
- 로그인 페이지 (LoginView.vue)
  - 폼 유효성 검증
  - 로딩 상태 표시
  - JWT 토큰 관리
- 회원가입 페이지 (SignupView.vue)
  - 실시간 입력값 검증
  - 비밀번호 강도 체크
  - 전화번호 형식 검증
- 대시보드 (DashboardView.vue)
  - 사용자 정보 카드
  - 시스템 상태 카드
  - 활성 코인 목록 (데이터 테이블)
- 공통 컴포넌트
  - TheHeader: 앱바, 사용자 메뉴
  - TheSidebar: 네비게이션

**Axios 설정:**
- 요청 인터셉터: JWT 토큰 자동 추가
- 응답 인터셉터: 401 에러 시 자동 로그아웃
- API 모듈화: authApi, userApi, coinApi

**라우터 가드:**
- requiresAuth: 인증 필요 페이지 보호
- requiresGuest: 로그인 시 리다이렉션
- 홈(/) → 대시보드 자동 이동

**테스트 계정:**
- ID: admin
- Password: Test1234!@
- Role: ADMIN

---

### ✅ Day 7 (2025-11-09) - 프로필 및 거래 설정 페이지
**완료 항목:**
- 프로필 설정 페이지 (ProfileView.vue)
  - 사용자 정보 조회 (읽기 전용)
  - 이메일/전화번호 수정
  - 폼 유효성 검증
  - 성공/에러 메시지 표시
- 거래 설정 페이지 (TradingSettingsView.vue)
  - 거래 코인 선택 (멀티 셀렉트)
  - 기술적 지표 설정 (이동평균선)
  - 매수 조건 설정
    - 하락률 (음수 입력)
    - 최대 보유 건수
    - 일일 거래 한도
  - 매도 조건 설정
    - 목표 수익률
    - 손절매 기준
    - 트레일링 스톱 (조건부)
  - AI 뉴스 분석 옵션
  - 설정 CRUD 기능
  - 삭제 확인 다이얼로그
- API 클라이언트 확장
  - tradingApi 추가 (CRUD)
  - TypeScript 타입 정의
- 라우터 업데이트
  - 프로필 설정 라우트
  - 거래 설정 라우트
- 사이드바 메뉴 활성화
  - 프로필 설정 메뉴
  - 거래 설정 메뉴

**Validation 규칙:**
- buyThresholdPct: -20 ~ 0 (음수)
- sellTargetPct: 0 ~ 50
- stopLossPct: -30 ~ 0
- basePeriod: 7 ~ 30
- maxHoldingsPerCoin: 최소 1
- dailyLimitAmount: 최소 10,000

**해결한 주요 이슈:**
1. **403 Forbidden 에러**
   - SecurityConfig에 POST /api/trading-settings 명시적 설정
   - JWT 토큰 만료 시간 확인 및 재로그인
2. **400 Bad Request 에러**
   - buyThresholdPct를 음수로 수정 (하락률 의미)
   - Validation 규칙 정확히 구현
3. **프론트엔드 빌드 에러**
   - Vue 템플릿 내 HTML 주석 제거
   - 태그 닫기 오류 수정
4. **Validation 에러**
   - negative 규칙 수정 (0 이하 허용)
   - 타입 변환 로직 강화

**테스트 완료:**
- ✅ 프로필 정보 조회
- ✅ 프로필 정보 수정 (이메일, 전화번호)
- ✅ 거래 설정 생성
- ✅ 거래 설정 조회
- ✅ 거래 설정 수정
- ✅ 거래 설정 삭제
- ✅ Validation 정상 작동
- ✅ 에러 메시지 표시
- ✅ 라우팅 및 네비게이션
- ✅ UI/UX 반응형 디자인

**보류된 테스트:**
- ⏸️ JWT 토큰 만료 (30분 후 테스트 필요)

---

### ✅ Day 8 (2025-11-23) - 거래 내역 및 보유 자산 관리
**완료 항목:**
- 거래 내역 API (Backend)
  - Transaction 엔티티
  - TransactionRepository (복합 조건 검색)
  - TransactionDTO, DashboardStatsDTO
  - TransactionService (CRUD, 통계)
  - TransactionController (REST API)
- 대시보드 통계 API
  - 총 자산 평가액
  - 실시간 수익률 계산
  - 일별 거래 통계
- 거래 내역 페이지 (Frontend)
  - TransactionHistoryView.vue
  - 검색 필터 (코인, 상태, 날짜)
  - 페이지네이션
  - 매도 처리 기능
- 보유 자산 페이지 (Frontend)
  - HoldingsView.vue
  - 실시간 평가손익 표시
  - 통계 요약 카드
  - 매도 처리 기능
- 보안 설정 업데이트
  - SecurityConfig에 거래 API 권한 추가

**API 엔드포인트:**
| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| GET | /api/transactions | ✅ | 전체 거래 내역 (페이징) |
| GET | /api/transactions/search | ✅ | 거래 내역 검색 |
| GET | /api/transactions/holdings | ✅ | 보유 자산 조회 |
| GET | /api/transactions/{id} | ✅ | 특정 거래 상세 |
| POST | /api/transactions | ✅ | 거래 생성 (매수) |
| PUT | /api/transactions/{id} | ✅ | 거래 수정 (메모) |
| POST | /api/transactions/{id}/sell | ✅ | 매도 처리 |
| GET | /api/transactions/dashboard-stats | ✅ | 대시보드 통계 |

**주요 기능:**
- 복합 조건 검색: 코인, 상태, 날짜 범위
- 실시간 현재가 연동 (업비트 API)
- 평가 손익 자동 계산
- 매도 처리 및 실현 손익 기록
- 페이지네이션 (20개/페이지)

**해결한 주요 이슈:**
1. **500 Internal Server Error**
   - TransactionController에서 @AuthenticationPrincipal UserDetails → String으로 타입 변경
   - JwtAuthenticationFilter의 principal 타입과 일치시킴
2. **NullPointerException**
   - UserDetails 캐스팅 오류 해결
   - SecurityContextHolder에서 String(userId) 직접 사용

**테스트 완료:**
- ✅ 거래 내역 조회 (페이징) - 브라우저
- ✅ 복합 조건 검색 (코인, 상태, 날짜) - 브라우저
- ✅ 보유 자산 조회 - 브라우저
- ✅ 대시보드 통계 - 브라우저
- ✅ 실시간 평가손익 계산 - 브라우저
- ✅ 거래 내역 페이지 렌더링 - 브라우저
- ✅ 보유 자산 페이지 렌더링 - 브라우저
- ✅ 거래 생성 (매수) - Postman
- ✅ 거래 수정 (메모) - Postman
- ✅ 매도 처리 - Postman
- ✅ 거래 검색 API - Postman

**참고사항:**
- 프론트엔드 매수/매도/수정 버튼은 미구현 상태
- Postman으로 백엔드 API 기능 검증 완료

---

### ✅ Day 9 (2025-11-29) - 자동매매 봇 핵심 구현
**완료 항목:**
- 기술적 지표 계산 서비스
  - 이동평균선 (MA7, MA14, MA20, MA30)
  - RSI (14일) 계산
  - 볼린저 밴드 (20일, 2 표준편차)
  - 거래량 분석 (평균 대비 비율)
- 거래 신호 감지 서비스
  - 매수 신호: MA 하락률 + RSI + 볼린저밴드 + 거래량 조합
  - 매도 신호: 목표 수익률 도달
  - 손절매 신호: 설정 기준 도달
  - 트레일링 스톱: 최고가 대비 하락률
- 리스크 관리 서비스
  - 일일 거래 한도 체크
  - 종목당 최대 보유 건수 제한
  - 매수 가능 여부 종합 판단
- 자동매매 봇 서비스
  - 사용자별 자동매매 실행
  - 전체 사용자 일괄 실행
  - 최고가 자동 업데이트 (트레일링 스톱용)
- 스케줄러 구현
  - 5분 간격 자동매매 실행
  - 업비트 점검시간 회피 (09:00~09:10)
- 봇 관리 API
  - 수동 실행, 지표 조회, 상태 조회

**API 엔드포인트:**
| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| POST | /api/bot/execute | ✅ | 수동 자동매매 실행 |
| GET | /api/bot/indicators/{market} | ✅ | 특정 코인 기술적 지표 |
| GET | /api/bot/indicators?markets= | ✅ | 다중 코인 지표 조회 |
| GET | /api/bot/status | ✅ | 봇 상태 조회 |

**생성된 파일:**
- `dto/upbit/UpbitCandleDTO.java` - 캔들 데이터 DTO
- `dto/indicator/IndicatorResultDTO.java` - 지표 결과 DTO
- `dto/bot/TradingSignalDTO.java` - 거래 신호 DTO
- `service/TechnicalIndicatorService.java` - 기술적 지표 계산
- `service/SignalDetectorService.java` - 신호 감지
- `service/RiskManagementService.java` - 리스크 관리
- `service/TradingBotService.java` - 자동매매 봇
- `scheduler/TradingScheduler.java` - 스케줄러
- `controller/BotController.java` - 봇 API

**테스트 완료:**
- ✅ 기술적 지표 조회 (KRW-BTC) - Postman
- ✅ 봇 상태 조회 - Postman
- ✅ 수동 자동매매 실행 - Postman
- ✅ 스케줄러 5분 간격 동작 - 로그 확인
- ✅ 거래 설정 페이지 레이아웃 - 브라우저

---

### ✅ Day 10 (2025-12-01) - 알림 시스템 및 대시보드 고도화
**완료 항목:**
- Discord 웹훅 알림 시스템
  - NotificationService: Discord 알림 발송
  - NotificationConfig: 웹훅 설정 관리
  - 매수/매도 체결 시 실시간 알림
  - 손절매 발생 시 경고 알림
- 일일 리포트 시스템
  - DailyReportService: 일일 거래 현황 집계
  - 실현 손익 / 평가 손익 계산
  - 코인별 상세 현황
  - 23:50 자동 리포트 발송 스케줄러
- 알림 API
  - 알림 상태 조회
  - 테스트 알림 발송
  - 일일 리포트 미리보기/발송
- 봇 모니터링 페이지 (Frontend)
  - BotMonitorView.vue
  - 봇 상태 카드 (실행 중/대기 중)
  - 다음 실행 시간 표시
  - 오늘 매수/매도 건수
  - 수동 매매 실행 버튼
  - 기술적 지표 테이블 (RSI, BB, MA, 신호)
  - 30초 자동 새로고침
- 일일 리포트 페이지 (Frontend)
  - DailyReportView.vue
  - 총 손익 요약 카드
  - 손익 상세 (실현/평가/총)
  - Discord 리포트 발송 버튼
  - 코인별 현황 테이블
- 사이드바 메뉴 추가
  - 봇 모니터링 (mdi-robot)
  - 일일 리포트 (mdi-file-chart)
- TypeScript 타입 정의
  - bot.ts: 지표, 신호, 봇 상태, 리포트 타입

**API 엔드포인트:**
| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| GET | /api/notifications/status | ✅ | 알림 상태 조회 |
| POST | /api/notifications/test | ✅ | 테스트 알림 발송 |
| GET | /api/notifications/daily-report/preview | ✅ | 일일 리포트 미리보기 |
| POST | /api/notifications/daily-report/send | ✅ | 일일 리포트 발송 |

**생성된 파일 (Backend):**
- `config/NotificationConfig.java` - Discord 설정
- `dto/notification/NotificationDTO.java` - 알림 DTO
- `dto/notification/DailyReportDTO.java` - 리포트 DTO
- `service/NotificationService.java` - 알림 발송
- `service/DailyReportService.java` - 리포트 생성
- `controller/NotificationController.java` - 알림 API

**생성된 파일 (Frontend):**
- `types/bot.ts` - TypeScript 타입 정의
- `views/BotMonitorView.vue` - 봇 모니터링 페이지
- `views/DailyReportView.vue` - 일일 리포트 페이지

**수정된 파일:**
- `scheduler/TradingScheduler.java` - 23:50 리포트 스케줄러 추가
- `components/TheSidebar.vue` - 메뉴 2개 추가
- `router/index.ts` - 라우트 2개 추가
- `docker-compose.yml` - Discord 환경변수 추가

**환경 설정 (Discord 알림):**
```properties
# .env 파일에 추가
DISCORD_ENABLED=true
DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/your_webhook_url
```

**테스트 완료:**
- ✅ 알림 상태 조회 - Postman
- ✅ 테스트 알림 발송 - Postman
- ✅ 일일 리포트 미리보기 - Postman
- ✅ 일일 리포트 발송 - Postman
- ✅ 기술적 지표 조회 (단일) - Postman
- ✅ 기술적 지표 조회 (다중) - Postman
- ✅ 봇 상태 조회 - Postman
- ✅ 수동 자동매매 실행 - Postman
- ✅ 사이드바 메뉴 표시 - 브라우저
- ✅ 봇 모니터링 페이지 - 브라우저
- ✅ 일일 리포트 페이지 - 브라우저
- ✅ Discord 알림 수신 - Discord

---

**테스트 완료:**
- ✅ 알림 상태 조회 - Postman
- ✅ 테스트 알림 발송 - Postman
...
- ✅ Discord 알림 수신 - Discord

---

### ✅ Day 11 (2025-12-02) - 백테스팅 기능 구현
**완료 항목:**
- 백테스팅 시스템 (Backend)
  - BacktestRequestDTO: 백테스트 요청 파라미터
  - BacktestResultDTO: 결과 데이터 (수익률, 승률, MDD 등)
  - BacktestService: 과거 데이터 기반 시뮬레이션 엔진
  - BacktestController: 백테스트 API
- 백테스팅 로직
  - 이동평균선(MA) 기반 매수 신호 감지
  - RSI 과매도 조건 조합
  - 목표 수익률/손절매/트레일링 스톱 매도
  - 수수료(0.05%) 반영
  - 일별 자산 변동 추적
- 성과 지표 계산
  - 총 수익률, 승률
  - 최대 낙폭(MDD)
  - 샤프 비율
  - 손익비(Profit Factor)
  - 코인별 성과 분석
- 백테스팅 페이지 (Frontend)
  - BacktestView.vue
  - 코인/기간/투자금 설정 폼
  - 고급 설정 (MA기간, 매수/매도 조건)
  - 트레일링 스톱 옵션
  - 결과 요약 카드 (수익, 승률, 거래횟수, MDD)
  - 상세 지표 표시
  - 자산 변동 차트 (v-sparkline)
  - 마우스 호버 시 일별 정보 툴팁
  - 초기 투자금 기준선 (점선)
  - 코인별 성과 테이블
  - 거래 내역 테이블 (페이지네이션)
- 사이드바 메뉴 추가
  - 백테스팅 (mdi-chart-timeline-variant)
- TypeScript 타입 정의
  - backtest.ts: 요청/결과/성과 타입

**API 엔드포인트:**
| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| POST | /api/backtest/run | ✅ | 백테스트 실행 |
| GET | /api/backtest/quick | ✅ | 빠른 백테스트 |
| GET | /api/backtest/available-coins | ✅ | 가능 코인 목록 |
| GET | /api/backtest/default-settings | ✅ | 기본 설정값 |

**생성된 파일 (Backend):**
- `dto/backtest/BacktestRequestDTO.java` - 요청 DTO
- `dto/backtest/BacktestResultDTO.java` - 결과 DTO
- `service/BacktestService.java` - 백테스트 엔진
- `controller/BacktestController.java` - 백테스트 API

**생성된 파일 (Frontend):**
- `types/backtest.ts` - TypeScript 타입
- `views/BacktestView.vue` - 백테스팅 페이지

**수정된 파일:**
- `config/SecurityConfig.java` - 백테스트 API 권한 추가
- `api/index.ts` - backtestApi 추가
- `router/index.ts` - 백테스팅 라우트 추가
- `components/TheSidebar.vue` - 메뉴 추가
- `index.html` - CSP 메타 태그 추가
- `nginx.conf` - CSP 헤더 추가

**해결한 주요 이슈:**
1. **컴파일 오류 (toLocalDate)**
   - UpbitCandleDTO의 candleDateTimeKst가 String 타입
   - parseToLocalDate() 헬퍼 메서드 추가로 해결
2. **@Builder 기본값 무시**
   - @Builder.Default 어노테이션 추가
3. **CSP 차단 (Chart.js eval)**
   - Chart.js → Vuetify v-sparkline으로 대체
   - index.html에 CSP 메타 태그 추가
4. **코인 목록 조회 실패**
   - response.coins → response.data.coins 수정

**테스트 완료:**
- ✅ 로그인 및 토큰 발급 - Postman
- ✅ 가능 코인 목록 조회 - Postman
- ✅ 기본 설정값 조회 - Postman
- ✅ 백테스트 실행 (30일) - Postman
- ✅ 빠른 백테스트 - Postman
- ✅ 백테스팅 페이지 렌더링 - 브라우저
- ✅ 코인 선택 드롭다운 - 브라우저
- ✅ 백테스트 실행 및 결과 표시 - 브라우저
- ✅ 자산 변동 차트 (v-sparkline) - 브라우저
- ✅ 차트 호버 툴팁 - 브라우저
- ✅ 초기 투자금 기준선 - 브라우저
- ✅ 코인별 성과 테이블 - 브라우저
- ✅ 거래 내역 테이블 - 브라우저

---

### ✅ Day 12 (2025-12-03) - 이메일 알림 및 관리자 페이지
**완료 항목:**
- 이메일 알림 시스템 (Backend)
  - EmailService: SMTP 연동 이메일 발송
  - Thymeleaf 이메일 템플릿
  - 테스트 이메일, 일일 리포트 이메일
- 관리자 대시보드 (Backend)
  - AdminService: 시스템 통계, 사용자 관리
  - AdminController: 관리자 전용 API
  - 시스템 현황 (총 사용자, 거래, 수익)
  - 알림 상태 (Discord, Email)
- 관리자 대시보드 (Frontend)
  - AdminDashboardView.vue
  - 시스템 통계 카드 (사용자, 거래, 수익)
  - 알림 상태 칩 (Discord/Email 활성화)
  - 사용자 목록 테이블 (활성/비활성 상태)
- UI/UX 개선
  - 일일 리포트 페이지에 이메일 발송 버튼 추가
  - 봇 모니터링 페이지에 이메일 테스트 버튼 추가
  - 백테스팅 차트에 최고/최저 자산 기준선 추가

**API 엔드포인트:**
| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| GET | /api/admin/stats | 🔐 | 시스템 통계 (관리자) |
| GET | /api/admin/users | 🔐 | 사용자 목록 (관리자) |
| POST | /api/notifications/email/test | ✅ | 테스트 이메일 발송 |
| POST | /api/notifications/email/daily-report | ✅ | 일일 리포트 이메일 |

**생성된 파일 (Backend):**
- `service/EmailService.java` - 이메일 발송 서비스
- `service/AdminService.java` - 관리자 서비스
- `controller/AdminController.java` - 관리자 API
- `dto/admin/SystemStatsDTO.java` - 시스템 통계 DTO
- `dto/admin/AdminUserDTO.java` - 관리자용 사용자 DTO
- `templates/email/test-email.html` - 테스트 이메일 템플릿
- `templates/email/daily-report.html` - 일일 리포트 템플릿

**생성된 파일 (Frontend):**
- `views/AdminDashboardView.vue` - 관리자 대시보드

**수정된 파일:**
- `docker-compose.yml` - 이메일 환경변수 추가
- `api/index.ts` - adminApi, emailApi 추가
- `router/index.ts` - 관리자 라우트 추가
- `components/TheSidebar.vue` - 관리자 메뉴 추가
- `DailyReportView.vue` - 이메일 발송 버튼 추가
- `BotMonitorView.vue` - 이메일 테스트 버튼 추가
- `BacktestView.vue` - 최고/최저 기준선 추가
- `SignupRequest.java` - 전화번호 검증 패턴 수정

**해결한 주요 이슈:**
1. **이메일 발송 실패**
   - docker-compose.yml에 이메일 환경변수 누락 → 추가
2. **일일 리포트 이메일 템플릿 오류**
   - DTO 필드명 불일치 (quantity → totalQuantity) 수정
3. **관리자 API 500 에러**
   - apiClient → api 변수명 수정
   - TransactionStatus enum 타입 사용
4. **사용자 상태 표시 오류**
   - Java boolean isActive → JSON 직렬화 시 active로 변환
   - 프론트엔드 필드명 일치시킴
5. **회원가입 전화번호 검증 실패**
   - 문제: 선택사항인 전화번호를 입력하지 않으면 400 에러 발생
   - 원인: SignupRequest.java의 @Pattern이 빈 문자열 불허
   - 해결: 정규식 `^010-\\d{4}-\\d{4}$` → `^$|^010-\\d{4}-\\d{4}$` 수정
   - 빈 문자열(`^$`) 허용 추가로 선택사항 정상 동작

**환경 설정 (이메일):**
```properties
# .env 파일에 추가
EMAIL_ENABLED=true
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your_email@gmail.com
SMTP_PASSWORD=your_app_password
```

**테스트 완료:**
- ✅ 테스트 이메일 발송 - Postman
- ✅ 일일 리포트 이메일 발송 - Postman
- ✅ 시스템 통계 조회 - Postman
- ✅ 사용자 목록 조회 - Postman
- ✅ 관리자 대시보드 렌더링 - 브라우저
- ✅ 사용자 상태 표시 (활성/비활성) - 브라우저
- ✅ 이메일 발송 버튼 동작 - 브라우저
- ✅ 회원가입 (전화번호 미입력) - 브라우저

---

### ✅ Day 13 (2025-12-04) - 백테스팅 차트 고도화
**완료 항목:**
- 백테스팅 차트 전면 개편
  - v-sparkline → 커스텀 SVG 차트로 변경
  - 데이터 포인트 점 표시 (수익: 녹색, 손실: 빨간색, 보합: 파란색)
  - 기준선 라벨 위치 정확도 개선
  - 차트 높이 증가 (200px → 350px)
- 차트 보기 모드 추가
  - 전체 보기: 모든 데이터를 화면 너비에 맞춤
  - 스크롤 보기: 점 간격 고정, 좌우 스크롤로 상세 확인
  - 60일 이상 데이터일 때만 토글 버튼 표시
- 마우스 호버 인터랙션
  - 호버 시 점 크기 확대
  - 일자별 자산/수익률 툴팁 표시

**수정된 파일:**
- `BacktestView.vue` - 차트 전면 개편
  - 템플릿: SVG 차트, 보기 모드 토글, 스크롤 컨테이너
  - 스크립트: chartViewMode, dynamicSvgWidth, chartPoints 등 computed 추가
  - 스타일: 스크롤 모드 CSS, 차트 높이 증가

**차트 기능:**
| 기능 | 설명 |
|------|------|
| 데이터 포인트 | 각 일자에 점 표시 |
| 점 색상 | 녹색(수익), 빨간색(손실), 파란색(보합) |
| 기준선 | 초기(주황), 최고(녹색), 최저(빨간) 점선 |
| 전체 보기 | 모든 데이터를 화면에 압축 표시 |
| 스크롤 보기 | 점 간격 25px 고정, 좌우 스크롤 |
| 툴팁 | 마우스 호버 시 일자/자산/수익률 표시 |

**테스트 완료:**
- ✅ 차트 렌더링 - 브라우저
- ✅ 데이터 포인트 점 표시 - 브라우저
- ✅ 기준선 라벨 위치 - 브라우저
- ✅ 전체 보기 모드 - 브라우저
- ✅ 스크롤 보기 모드 - 브라우저
- ✅ 마우스 호버 툴팁 - 브라우저

---

### ✅ Day 14 (2025-12-07) - 기술적 지표 설정 UI 추가
**완료 항목:**
- 기술적 지표 사용자 설정 기능 (Backend)
  - TradingSetting Entity 확장 (6개 필드 추가)
  - TradingSettingDTO Validation 추가
  - TradingSettingService 수정
  - BacktestRequestDTO 확장
  - BacktestService 사용자 설정 적용
  - TechnicalIndicatorService 오버로드 메서드 추가
  - SignalDetectorService 사용자 설정 연동
- 데이터베이스 스키마 업데이트
  - trading_settings 테이블에 6개 컬럼 추가
- 거래 설정 페이지 UI 확장 (Frontend)
  - RSI 설정 섹션 (기간, 매수 임계값, 매도 임계값)
  - 볼린저 밴드 설정 섹션 (기간, 표준편차 승수)
  - 거래량 급증 기준 슬라이더
- 백테스팅 페이지 UI 확장 (Frontend)
  - 고급 설정에 기술적 지표 설정 추가
  - RSI, 볼린저 밴드, 거래량 설정 필드
- TypeScript 타입 정의 확장
  - TradingSetting, TradingSettingRequest 인터페이스
  - BacktestRequest 인터페이스

**추가된 설정 필드:**
| 필드 | 설명 | 기본값 | 범위 |
|------|------|--------|------|
| rsiPeriod | RSI 계산 기간 | 14일 | 5~50 |
| rsiBuyThreshold | RSI 매수 신호 (이하) | 30 | 10~50 |
| rsiSellThreshold | RSI 매도 신호 (이상) | 70 | 50~90 |
| bbPeriod | 볼린저 밴드 기간 | 20일 | 10~50 |
| bbMultiplier | 표준편차 승수 | 2배 | 1~4 |
| volumeThreshold | 거래량 급증 기준 | 150% | 100~500 |

**수정된 파일 (Backend):**
- `entity/TradingSetting.java` - 6개 필드 추가
- `dto/TradingSettingDTO.java` - Validation 추가, @Max import
- `service/TradingSettingService.java` - 새 필드 처리
- `dto/backtest/BacktestRequestDTO.java` - 6개 필드 추가
- `controller/BacktestController.java` - 기본값 추가
- `service/TechnicalIndicatorService.java` - 오버로드 메서드
- `service/SignalDetectorService.java` - 사용자 설정 적용
- `service/BacktestService.java` - checkBuySignal, 누락 메서드 추가

**수정된 파일 (Frontend):**
- `views/TradingSettingsView.vue` - 기술적 지표 설정 UI
- `views/BacktestView.vue` - 고급 설정 확장
- `types/index.ts` - TradingSetting 타입 확장
- `types/backtest.ts` - BacktestRequest 타입 확장

**DB 마이그레이션:**
```sql
ALTER TABLE trading_settings 
ADD COLUMN rsi_period INT DEFAULT 14,
ADD COLUMN rsi_buy_threshold INT DEFAULT 30,
ADD COLUMN rsi_sell_threshold INT DEFAULT 70,
ADD COLUMN bb_period INT DEFAULT 20,
ADD COLUMN bb_multiplier INT DEFAULT 2,
ADD COLUMN volume_threshold INT DEFAULT 150;
```

**해결한 주요 이슈:**
1. **@Max import 누락**
   - TradingSettingDTO.java에 `jakarta.validation.constraints.Max` import 추가
2. **BacktestService 메서드 누락**
   - checkSellSignals, canBuy, recordDailyBalance 메서드 추가
   - executeBuy 호출 시 signal 파라미터 추가
3. **Vue 객체 쉼표 누락**
   - settings, defaultSettings, loadSettings, saveSettings에서 쉼표 추가
4. **업비트 API 200개 제한**
   - 백테스트 기간 200일 이내 권장 (API 페이징 미구현)

**테스트 완료:**
- ✅ DB 스키마 업데이트 - MySQL
- ✅ 거래 설정 조회 (새 필드 포함) - Postman
- ✅ 거래 설정 생성/수정 - Postman
- ✅ 백테스트 실행 (느슨한 조건) - Postman
- ✅ 백테스트 기본 설정 조회 - Postman
- ✅ 거래 설정 페이지 UI - 브라우저
- ✅ 백테스팅 페이지 고급 설정 - 브라우저
- ✅ 설정 변경 후 백테스트 실행 - 브라우저

---

### ✅ Day 15 (2025-12-13) - 예외처리 강화 및 에러 핸들링 개선
**완료 항목:**
- 에러 코드 체계화 (Backend)
  - ErrorCode Enum: 9개 카테고리, 30+ 에러 코드 정의
  - 커스텀 예외 클래스 체계 구축
    - BusinessException (기본 비즈니스 예외)
    - EntityNotFoundException (리소스 미발견)
    - DuplicateResourceException (중복 리소스)
    - UnauthorizedException (인증 실패)
    - UpbitApiException (업비트 API 오류)
    - TradingException (거래 관련 오류)
- API 응답 표준화 (Backend)
  - ApiResponse 래퍼 클래스 (success, error, timestamp)
  - ErrorResponse 내부 클래스 (code, message, detail, fieldErrors)
  - PageResponse 페이징 응답 클래스
- GlobalExceptionHandler 고도화 (Backend)
  - 12개 예외 타입별 세분화 처리
  - Validation, 인증, 타입 불일치, JSON 파싱 등
  - 상세 에러 로깅 추가
- Security 인증 예외 핸들러 (Backend)
  - CustomAuthenticationEntryPoint: 401 응답 표준화
  - CustomAccessDeniedHandler: 403 응답 표준화
  - JWT 에러 타입별 코드 분기 (만료/무효/미인증)
- AuthController 예외 처리 개선 (Backend)
  - 로그인/회원가입 에러 응답 표준화
  - 중복 아이디 에러 코드 적용 (U002)
- Frontend 에러 핸들링 개선
  - error.ts: 에러 타입 정의 및 메시지 매핑
  - Axios 인터셉터 고도화 (에러 타입별 처리)
  - 로그인 실패 시 페이지 새로고침 방지
  - useErrorHandler Composable
  - useSnackbar Composable
  - GlobalSnackbar 컴포넌트

**에러 코드 체계:**
| 카테고리 | 코드 범위 | 예시 |
|----------|----------|------|
| Common | C0XX | C001 (잘못된 입력값) |
| Authentication | A0XX | A001 (인증 필요), A005 (로그인 실패) |
| User | U0XX | U002 (중복 아이디) |
| API Key | K0XX | K001 (API 키 미등록) |
| Trading | T0XX | T004 (일일 한도 초과) |
| Transaction | X0XX | X001 (거래 미발견) |
| Upbit API | P0XX | P001 (API 호출 실패) |
| Notification | N0XX | N002 (이메일 발송 실패) |
| Backtest | B0XX | B001 (기간 오류) |

**생성된 파일 (Backend):**
- `exception/ErrorCode.java` - 에러 코드 Enum
- `exception/BusinessException.java` - 기본 비즈니스 예외
- `exception/EntityNotFoundException.java` - 리소스 미발견 예외
- `exception/DuplicateResourceException.java` - 중복 리소스 예외
- `exception/UnauthorizedException.java` - 인증 실패 예외
- `exception/UpbitApiException.java` - 업비트 API 예외
- `exception/TradingException.java` - 거래 관련 예외
- `dto/common/ApiResponse.java` - API 응답 표준 클래스
- `dto/common/PageResponse.java` - 페이징 응답 클래스
- `config/security/CustomAuthenticationEntryPoint.java` - 인증 예외 핸들러
- `config/security/CustomAccessDeniedHandler.java` - 접근 거부 핸들러

**생성된 파일 (Frontend):**
- `types/error.ts` - 에러 타입 및 메시지 매핑
- `composables/useErrorHandler.ts` - 에러 핸들러
- `composables/useSnackbar.ts` - Snackbar 유틸
- `components/GlobalSnackbar.vue` - 전역 알림 컴포넌트

**수정된 파일:**
- `exception/GlobalExceptionHandler.java` - 전면 개편
- `controller/AuthController.java` - 에러 응답 표준화
- `config/SecurityConfig.java` - 예외 핸들러 등록
- `filter/JwtAuthenticationFilter.java` - 에러 메시지 저장
- `api/index.ts` - Axios 인터셉터 개선
- `App.vue` - GlobalSnackbar 추가

**테스트 완료:**
- ✅ Validation 에러 (C001 + fieldErrors) - Postman
- ✅ 로그인 실패 (A005) - Postman
- ✅ 토큰 없이 API 호출 (A001) - Postman
- ✅ 잘못된 토큰 (A002) - Postman
- ✅ 존재하지 않는 거래 조회 - Postman
- ✅ 중복 회원가입 (U002) - Postman
- ✅ 잘못된 HTTP 메서드 (C004) - Postman
- ✅ JSON 파싱 오류 (C001) - Postman
- ✅ 타입 불일치 (C003) - Postman
- ✅ 브라우저 로그인 실패 표시 - 브라우저
- ✅ 거래설정 Validation 표시 - 브라우저

---

### ✅ Day 16 (2025-12-17) - 백테스팅 리스크 관리 기능 추가
**완료 항목:**
- 백테스팅 리스크 관리 기능 (Backend)
  - BacktestRequestDTO 확장 (3개 필드 추가)
  - BacktestService 리스크 관리 로직 구현
  - SimulationState에 일일 상태 추적 필드 추가
- 코인 목록 개선 (Backend)
  - getAvailableCoins 메서드 전면 개편
  - 업비트 전체 KRW 마켓 코인 조회 (150개+)
  - 24시간 거래대금 기준 시가총액 순 정렬
- 백테스팅 UI 확장 (Frontend)
  - 리스크 관리 설정 섹션 추가
  - 코인 선택 v-autocomplete로 변경
  - 시가총액 순위 칩 표시

**추가된 리스크 관리 설정:**
| 필드 | 설명 | 기본값 | 범위 |
|------|------|--------|------|
| dailyTradeLimitPct | 일일 최대 거래금액 (%) | 100 (제한없음) | 10~100 |
| maxPositionPct | 단일 종목 최대 비중 (%) | 100 (제한없음) | 10~100 |
| dailyStopLossPct | 긴급 정지 조건 (%) | -100 (사용안함) | -50~0 |

**리스크 관리 로직:**
| 기능 | 설명 |
|------|------|
| 일일 거래 한도 | 초기 자본 대비 하루 최대 매수 금액 제한 |
| 단일 종목 비중 | 한 코인에 최대 투자 가능 금액 제한 |
| 긴급 정지 | 당일 손실률 도달 시 거래 중단 |

**수정된 파일 (Backend):**
- `dto/backtest/BacktestRequestDTO.java` - 3개 필드 추가
- `service/BacktestService.java` - 리스크 관리 로직, SimulationState 확장
- `controller/BacktestController.java` - UpbitApiService 연동, 코인 목록 개선

**수정된 파일 (Frontend):**
- `types/backtest.ts` - BacktestRequest, AvailableCoin 타입 확장
- `views/BacktestView.vue` - 리스크 관리 UI 섹션 추가

**해결한 주요 이슈:**
1. **UpbitApiService import 누락**
   - BacktestController에 import 추가
2. **canBuy 메서드 파라미터 불일치**
   - 6개 파라미터 → 3개로 간소화 (내부 계산)
3. **Vue 객체 쉼표 누락**
   - volumeThreshold 뒤 쉼표 추가

**테스트 완료:**
- ✅ 백테스트 실행 (리스크 관리 적용) - 브라우저
- ✅ 리스크 관리 UI 표시 - 브라우저
- ✅ 일일 거래 한도 설정 - 브라우저
- ✅ 단일 종목 비중 설정 - 브라우저
- ✅ 긴급 정지 설정 - 브라우저
- ✅ 코인 목록 시가총액 순 정렬 - 브라우저

---

### ✅ Day 17 (2025-12-21) - 성능 최적화 및 보안 강화
**완료 항목:**
- Redis 캐싱 전략 구현 (Backend)
  - CacheService: 범용 캐시 서비스 구현
  - 현재가 캐싱 (TTL: 30초)
  - 활성 코인 목록 캐싱 (TTL: 1시간)
  - 캐시 히트/미스 로깅
- Rate Limiting 구현 (Backend)
  - RateLimitFilter: IP 기반 요청 제한 필터
  - 분당 60회 요청 제한
  - Redis 기반 카운터 관리
  - 429 Too Many Requests 응답 처리
  - X-RateLimit-Limit, X-RateLimit-Remaining 헤더 추가
- 보안 헤더 강화 (Frontend/Nginx)
  - X-Frame-Options: SAMEORIGIN (클릭재킹 방지)
  - X-Content-Type-Options: nosniff (MIME 스니핑 방지)
  - X-XSS-Protection: 1; mode=block (XSS 공격 방지)
  - Referrer-Policy: strict-origin-when-cross-origin (정보 누출 방지)
- DB 인덱스 최적화 확인
  - transactions 테이블: 4개 인덱스 확인
  - trading_settings 테이블: 1개 인덱스 확인
- 백테스팅 기간 확장 (1년 → 3년)
  - BacktestController: 최대 기간 1년 → 3년으로 확장
  - BacktestService: API 호출 딜레이 100ms → 200ms (Rate Limit 방지)
  - BacktestView: 3년 초과 시 alert, 1년 초과 시 confirm 창 표시
  - API 타임아웃: 백테스트 API 10초 → 5분으로 확장
  - 업비트 API 페이징 처리로 장기간 데이터 조회 지원
- UI/UX 개선 (Frontend)
  - 대시보드 레이아웃 전면 개편
    - 사용자 정보 카드 + 우측 영역 높이 통일
    - 4개 통계 카드 (총 손익, 총 평가액, 오늘 매수/매도)
    - 시스템 상태 + 빠른 액세스 2열 배치
    - 빠른 액세스에 백테스팅, 일일 리포트 버튼 추가
  - 코인 상세 다이얼로그 구현
    - 심볼, 시가총액 순위, 상태, 최근 업데이트 표시
    - 주요 코인 설명 (BTC, ETH, XRP, SOL, DOGE, ADA)
    - "이 코인 거래 설정에 추가" 버튼
  - 활성 코인 목록 정렬 개선
    - 시가총액 순위 기준 오름차순 정렬
    - NULL 순위는 마지막으로 배치
  - 관리자 대시보드 카드 높이 통일
  - 봇 모니터링 카드 높이 통일
  - 거래 설정 페이지 개선
    - URL query 파라미터로 코인 자동 추가 기능
    - 기본 선택 코인 비트코인으로 변경
- 데이터베이스 인코딩 개선
  - application.yml에 useUnicode=true 추가
  - 폴리곤 코인 한글명 깨짐 수정 (DB 직접 수정)

**생성된 파일 (Backend):**
- `service/CacheService.java` - Redis 캐시 서비스
- `filter/RateLimitFilter.java` - Rate Limiting 필터

**수정된 파일:**
- `service/CoinInfoService.java` - 캐싱 적용
- `config/SecurityConfig.java` - RateLimitFilter 등록
- `exception/ErrorCode.java` - C006 (Rate Limit 초과) 추가
- `frontend/nginx.conf` - 보안 헤더 추가
- `controller/BacktestController.java` - 기간 제한 3년으로 확장
- `service/BacktestService.java` - API 딜레이 200ms로 증가
- `views/BacktestView.vue` - 기간 체크 로직 및 알림 추가
- `api/index.ts` - 백테스트 API 타임아웃 5분 설정
- `views/DashboardView.vue` - 레이아웃 전면 개편, 코인 상세 다이얼로그
- `views/AdminDashboardView.vue` - 카드 높이 통일 (stats-card 클래스)
- `views/BotMonitorView.vue` - 카드 높이 통일 (bot-stats-card 클래스)
- `views/TradingSettingsView.vue` - query 파라미터 코인 추가 기능
- `application.yml` - useUnicode=true 추가

**테스트 완료:**
- ✅ 활성 코인 캐싱 - Redis CLI
- ✅ 현재가 캐싱 (캐시 히트) - 로그 확인
- ✅ Rate Limit 헤더 - Postman
- ✅ Rate Limit 초과 (429) - PowerShell 스크립트
- ✅ 보안 헤더 적용 - 브라우저
- ✅ DB 인덱스 존재 확인 - MySQL CLI
- ✅ 백테스팅 3년 초과 시 alert 표시 - 브라우저
- ✅ 백테스팅 1~3년 기간 confirm 창 표시 - 브라우저
- ✅ 백테스팅 2년 기간 정상 실행 - 브라우저
- ✅ 대시보드 레이아웃 개선 - 브라우저
- ✅ 코인 상세 다이얼로그 - 브라우저
- ✅ 코인 목록 순위 정렬 - 브라우저
- ✅ 관리자 대시보드 카드 높이 통일 - 브라우저
- ✅ 봇 모니터링 카드 높이 통일 - 브라우저
- ✅ 거래 설정 코인 자동 추가 - 브라우저
- ✅ 폴리곤 한글명 정상 표시 - 브라우저

---

### ✅ Day 18 (2025-12-22) - Discord 개인 DM 알림 시스템
**완료 항목:**
- Discord Bot 생성 및 연동
  - Discord Developer Portal에서 Bot 생성
  - JDA (Java Discord API) 5.0.0-beta.24 라이브러리 연동
  - Bot Token 환경변수 관리
- DiscordBotService 구현
  - Bot 초기화 및 연결 관리
  - 사용자별 DM 발송 기능
  - 매수/매도/손절매/일일 리포트 Embed 메시지
- 사용자별 Discord User ID 관리
  - users 테이블에 discord_user_id 컬럼 추가
  - User Entity, DTO 확장
  - 프로필 API에서 Discord User ID 저장/조회
- 프로필 페이지 UI 개선
  - Discord DM 알림 설정 카드 추가
  - Discord User ID 입력 및 저장
  - 연동 테스트 버튼
  - 알림 유형별 테스트 버튼 (매수/매도/손절/리포트)
  - 기본정보 + (비밀번호 + Discord) 레이아웃 재구성
- 자동매매 DM 연동
  - TradingBotService: 매수/매도 시 개인 DM 발송
  - TradingScheduler: 일일 리포트 개인 DM 발송

**API 엔드포인트:**
| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| GET | /api/notifications/discord/bot-status | ✅ | Discord Bot 상태 조회 |
| POST | /api/notifications/discord/test-dm | ✅ | 연동 테스트 DM 발송 |
| POST | /api/notifications/discord/test-daily-report | ✅ | 일일 리포트 DM 테스트 |
| POST | /api/notifications/discord/test-buy | ✅ | 매수 알림 DM 테스트 |
| POST | /api/notifications/discord/test-sell | ✅ | 매도 알림 DM 테스트 |
| POST | /api/notifications/discord/test-stoploss | ✅ | 손절매 알림 DM 테스트 |

**생성된 파일 (Backend):**
- `service/DiscordBotService.java` - Discord Bot 서비스

**수정된 파일 (Backend):**
- `pom.xml` - JDA 의존성 추가
- `application.yml` - discord.bot.token 설정 추가
- `entity/User.java` - discordUserId 필드 추가
- `dto/UserInfoDTO.java` - discordUserId 필드 추가
- `service/UserService.java` - Discord User ID 저장/조회
- `controller/NotificationController.java` - Discord DM API 추가
- `service/TradingBotService.java` - 매수/매도 DM 발송 연동
- `scheduler/TradingScheduler.java` - 일일 리포트 DM 발송 연동

**수정된 파일 (Frontend):**
- `api/index.ts` - Discord DM API 추가
- `types/index.ts` - User, UpdateProfileRequest 타입 확장
- `views/ProfileView.vue` - Discord 설정 UI 및 레이아웃 개선

**환경 설정:**
```properties
# .env 파일에 추가
DISCORD_BOT_TOKEN=your_discord_bot_token
```

**테스트 완료:**
- ✅ Discord Bot 초기화 - 로그 확인
- ✅ Discord User ID 저장 - 브라우저
- ✅ 연동 테스트 DM 발송 - 브라우저
- ✅ 매수 알림 DM 테스트 - Postman, 브라우저
- ✅ 매도 알림 DM 테스트 - Postman, 브라우저
- ✅ 손절매 알림 DM 테스트 - Postman, 브라우저
- ✅ 일일 리포트 DM 테스트 - Postman, 브라우저
- ✅ 프로필 페이지 레이아웃 - 브라우저

---

### ✅ Day 19 (2025-12-24) - 리스크 관리 설정 동기화 및 UX 개선
**완료 항목:**
- 리스크 관리 설정 동기화 (백테스팅 ↔ 거래설정)
  - 백테스팅에만 있던 리스크 관리 필드를 거래설정에 추가
  - dailyTradeLimitPct: 일일 거래 한도 (%)
  - maxPositionPct: 단일 종목 최대 비중 (%)
  - dailyStopLossPct: 긴급 정지 조건 (%)
- 데이터베이스 스키마 업데이트
  - trading_settings 테이블에 3개 컬럼 추가
- 거래 설정 페이지 UX 개선 (Frontend)
  - 리스크 관리 섹션 UI 추가 (슬라이더)
  - 초기화 확인 다이얼로그 추가
  - 삭제 후 기본값 자동 저장 기능
  - 설정 없을 때 기본값 자동 생성 기능
  - 트레일링 스톱 양수 입력으로 변경 (백테스팅과 통일)
  - 일일 거래 금액 라벨/힌트 명확화
- 리스크 관리 실제 자동매매 적용 (Backend)
  - RiskManagementService 전면 개선
    - checkMaxPosition(): 단일 종목 비중 체크
    - checkDailyStopLoss(): 긴급 정지 조건 체크
    - isEmergencyStopActive(): 긴급 정지 상태 확인
    - calculateEffectiveDailyLimit(): dailyTradeLimitPct 적용
    - getRemainingPositionAmount(): 종목별 남은 투자 가능 금액
  - TransactionRepository 쿼리 추가
    - sumHoldingAmountByCoin(): 종목별 보유 금액 조회
    - sumTodayProfitLoss(): 당일 실현 손익 조회
  - TradingBotService 긴급 정지 체크 추가
    - 자동매매 실행 전 긴급 정지 상태 확인
    - EMERGENCY_STOP 상태 반환

**추가된 설정 필드:**
| 필드 | 설명 | 기본값 | 범위 |
|------|------|--------|------|
| dailyTradeLimitPct | 일일 거래 한도 (%) | 20% | 10~100 |
| maxPositionPct | 단일 종목 최대 비중 (%) | 25% | 10~100 |
| dailyStopLossPct | 긴급 정지 조건 (%) | -5% | -50~0 |

**수정된 파일 (Backend):**
- `entity/TradingSetting.java` - 3개 필드 추가
- `dto/TradingSettingDTO.java` - Validation 추가
- `service/TradingSettingService.java` - 새 필드 처리 (create/update/convert)
- `service/RiskManagementService.java` - ★ 전면 개선 (리스크 체크 로직 추가)
- `service/TradingBotService.java` - 긴급 정지 사전 체크 추가
- `repository/TransactionRepository.java` - 2개 쿼리 메서드 추가

**수정된 파일 (Frontend):**
- `views/TradingSettingsView.vue` - 전면 개선
  - 리스크 관리 UI 섹션 추가
  - resetDialog 상태 및 다이얼로그 추가
  - executeReset() 함수 추가
  - createDefaultSettings() 함수 추가
  - loadSettings() 자동 생성 로직 추가
  - deleteSettings() 기본값 자동 저장 로직 추가
  - 트레일링 스톱 양수 입력/음수 저장 변환
  - formatCurrency() 함수 위치 수정

**DB 마이그레이션:**
```sql
ALTER TABLE trading_settings 
ADD COLUMN daily_trade_limit_pct INT DEFAULT 20,
ADD COLUMN max_position_pct INT DEFAULT 25,
ADD COLUMN daily_stop_loss_pct INT DEFAULT -5;
```

**거래 설정 지표 실제 적용 현황:**
| 지표 | 사용 위치 | 설명 |
|------|----------|------|
| coinSymbols | TradingBotService | 매수 대상 코인 |
| basePeriod | SignalDetectorService | MA 기간 |
| buyThresholdPct | SignalDetectorService | 매수 하락률 |
| sellTargetPct | SignalDetectorService | 목표 수익률 |
| stopLossPct | SignalDetectorService | 손절매 기준 |
| maxHoldingsPerCoin | RiskManagementService | 종목당 최대 보유 |
| dailyLimitAmount | RiskManagementService | 일일 기준 금액 |
| useTrailingStop | SignalDetectorService | 트레일링 스톱 |
| trailingStopPct | SignalDetectorService | 트레일링 비율 |
| rsiPeriod | TechnicalIndicatorService | RSI 기간 |
| rsiBuyThreshold | TechnicalIndicatorService | RSI 매수 임계값 |
| rsiSellThreshold | TechnicalIndicatorService | RSI 매도 임계값 |
| bbPeriod | TechnicalIndicatorService | BB 기간 |
| bbMultiplier | TechnicalIndicatorService | BB 승수 |
| volumeThreshold | TechnicalIndicatorService | 거래량 기준 |
| dailyTradeLimitPct | RiskManagementService | 일일 한도 % |
| maxPositionPct | RiskManagementService | 종목 비중 % |
| dailyStopLossPct | RiskManagementService | 긴급 정지 % |

**테스트 완료:**
- ✅ DB 스키마 업데이트 - MySQL
- ✅ 거래 설정 조회 (새 필드 포함) - Postman
- ✅ 거래 설정 생성/수정 - Postman
- ✅ 리스크 관리 UI 표시 - 브라우저
- ✅ 초기화 다이얼로그 동작 - 브라우저
- ✅ 삭제 후 기본값 자동 저장 - 브라우저
- ✅ 설정 없을 때 자동 생성 - 브라우저
- ✅ 트레일링 스톱 양수 입력/저장 - 브라우저
- ✅ RiskManagementService 메서드 적용 확인 - 코드 검증
- ✅ TradingBotService 긴급 정지 체크 적용 확인 - 코드 검증
- ✅ 백엔드 컴파일 성공 - Docker

---

### ✅ Day 20 (2025-12-25) - 성능 최적화, 보안 강화, API 문서화
**완료 항목:**
- API 응답 시간 로깅 (Backend)
  - RequestLoggingFilter 구현
  - 모든 API 요청의 응답 시간 측정
  - 1초 이상 요청에 [SLOW] 경고 로그 표시
  - 정적 리소스 제외 처리
- 캐시 관리 확장 (Backend)
  - CacheService에 통계 메서드 추가
  - countCacheKeys(): 패턴별 키 개수 조회
  - getAllCacheKeys(): 전체 키 목록
  - evictByPattern(): 패턴별 삭제
  - getCacheStats(): 캐시 통계 (ticker, coins, total)
- 로그인 시도 제한 (Backend)
  - LoginAttemptService 구현
  - Redis 기반 시도 횟수 관리
  - 5회 실패 시 30분 계정 잠금
  - 15분 시도 횟수 만료
  - 관리자 잠금 해제 기능
- Swagger API 문서화 (Backend)
  - springdoc-openapi 2.3.0 연동
  - SwaggerConfig 설정 (JWT Bearer 인증)
  - AuthController에 Swagger 어노테이션 추가
  - API 그룹별 태그 정의
- 비밀번호 변경 API 추가 (Backend)
  - UserController에 PUT /api/user/password 엔드포인트 추가
  - UserService에 changePassword() 메서드 추가
  - 현재 비밀번호 확인 후 변경
- 관리자 API 확장 (Backend)
  - GET /api/admin/cache/stats: 캐시 통계
  - DELETE /api/admin/cache/clear: 캐시 초기화
  - POST /api/admin/users/{userId}/unlock: 계정 잠금 해제
- Nginx Swagger 프록시 설정 (Frontend)
  - /swagger-ui/, /v3/api-docs 등 프록시 추가
  - 정적 파일 규칙에서 swagger 경로 제외
- 기타 수정
  - PasswordResetToken 외래키 제약조건 비활성화
  - ErrorCode에 ACCOUNT_LOCKED, ACCOUNT_DISABLED 추가

**API 엔드포인트:**
| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| PUT | /api/user/password | ✅ | 비밀번호 변경 |
| GET | /api/admin/cache/stats | 🔐 | 캐시 통계 (관리자) |
| DELETE | /api/admin/cache/clear | 🔐 | 캐시 초기화 (관리자) |
| POST | /api/admin/users/{userId}/unlock | 🔐 | 계정 잠금 해제 (관리자) |

**Swagger UI 접속:**
- http://localhost/swagger-ui/index.html (Nginx 경유)
- http://localhost:8080/swagger-ui/index.html (직접 접속)

**생성된 파일 (Backend):**
- `filter/RequestLoggingFilter.java` - API 응답 시간 로깅 필터
- `service/LoginAttemptService.java` - 로그인 시도 제한 서비스
- `config/SwaggerConfig.java` - Swagger/OpenAPI 설정

**수정된 파일 (Backend):**
- `service/CacheService.java` - 캐시 통계 메서드 추가
- `service/AuthService.java` - 로그인 시도 제한 연동
- `service/UserService.java` - changePassword() 메서드 추가
- `controller/UserController.java` - 비밀번호 변경 API 추가
- `controller/AdminController.java` - 캐시/잠금해제 API 추가
- `controller/AuthController.java` - Swagger 어노테이션 추가
- `exception/ErrorCode.java` - ACCOUNT_LOCKED, ACCOUNT_DISABLED 추가
- `config/SecurityConfig.java` - Swagger 경로 허용, 필터 등록
- `entity/PasswordResetToken.java` - 외래키 NO_CONSTRAINT 설정
- `application.yml` - springdoc 설정 추가
- `pom.xml` - springdoc-openapi 의존성 추가

**수정된 파일 (Frontend):**
- `nginx.conf` - Swagger 프록시 설정, 정적 파일 규칙 수정

**해결한 주요 이슈:**
1. **JwtAuthenticationFilter 순서 오류**
   - addFilterBefore에서 커스텀 필터 참조 불가
   - addFilterAfter(SecurityContextHolderFilter) 사용으로 해결
2. **password_reset_tokens 외래키 오류**
   - user_id 컬럼 타입 불일치
   - @ForeignKey(ConstraintMode.NO_CONSTRAINT) 설정으로 해결
3. **Swagger UI 흰 페이지**
   - Nginx 정적 파일 규칙이 swagger 경로 가로챔
   - location 순서 조정 및 정규식 수정으로 해결
4. **ApiResponse 이름 충돌**
   - 프로젝트 DTO vs Swagger 어노테이션
   - 풀 패키지명으로 구분하여 해결

**테스트 완료:**
- ✅ Health Check - PowerShell
- ✅ Swagger UI 접속 - 브라우저
- ✅ 회원가입 - Postman
- ✅ 로그인 & 토큰 발급 - Postman
- ✅ 로그인 5회 실패 → 계정 잠금 - Postman
- ✅ 계정 잠금 해제 (관리자 API) - Postman
- ✅ API 응답 시간 로깅 [SLOW] - Docker 로그
- ✅ 캐시 통계 조회 - Postman
- ✅ 캐시 초기화 - Postman
- ✅ 비밀번호 변경 - Postman
- ✅ 프로필 조회/수정 - Postman
- ✅ 백테스트 API 정상 동작 - Git Bash
- ✅ 웹 UI 전체 확인 - 브라우저

---

### ✅ Day 21 (2025-12-25) - 최종 보안 점검 및 운영 안정성 강화
**완료 항목:**
- DB 자동 백업 시스템
  - backup-db.ps1: Windows PowerShell 백업 스크립트
  - backup-db.sh: Linux/Mac Bash 백업 스크립트
  - restore-db.ps1: 긴급 복원 스크립트
  - 일일 백업 + 7일 보관 + 압축
  - 동적 경로 설정 (이식성 확보)
- Logback 로그 로테이션 강화
  - 일별 로테이션 + 압축 (.gz)
  - 에러 로그 분리 (90일 보관)
  - 거래 로그 분리 (365일 보관)
  - 슬로우 쿼리 로그 분리
- Docker 헬스체크 및 자동 재시작
  - backend 헬스체크 (30초 간격, 60초 시작 대기)
  - mysql, redis 헬스체크
  - 전체 컨테이너 자동 재시작 정책 (restart: unless-stopped)
  - backend_logs 볼륨 마운트
- 스케줄러 시간대 설정
  - 모든 @Scheduled에 zone="Asia/Seoul" 추가
  - 자동매매: 5분마다 (KST)
  - 시스템 점검: 매일 04:00 KST
  - 일일 리포트: 매일 23:50 KST
- OWASP Top 10 보안 체크리스트 검토

**생성된 파일:**
- `scripts/backup-db.ps1` - Windows 백업 스크립트
- `scripts/backup-db.sh` - Linux/Mac 백업 스크립트
- `scripts/restore-db.ps1` - 복원 스크립트
- `backend/src/main/resources/logback-spring.xml` - 로그 설정

**수정된 파일:**
- `backend/Dockerfile` - curl 설치 추가
- `docker-compose.yml` - 헬스체크, 자동 재시작, 볼륨 추가
- `scheduler/TradingScheduler.java` - 시간대 설정 추가
- `.gitignore` - backups/ 제외 추가

**로그 파일 구성:**
| 로그 파일 | 용도 | 보관 기간 |
|----------|------|----------|
| crypto-trading.log | 일반 로그 | 30일 |
| crypto-trading-error.log | 에러 로그 | 90일 |
| crypto-trading-trading.log | 거래 로그 | 365일 |
| crypto-trading-slow-query.log | 슬로우 쿼리 | 30일 |

**테스트 완료:**
- ✅ 컨테이너 상태 확인 (4개 모두 healthy) - PowerShell
- ✅ 헬스체크 동작 확인 - PowerShell
- ✅ Health API 응답 - Postman, 브라우저
- ✅ 로그 파일 생성 확인 - PowerShell
- ✅ 백업 스크립트 실행 - PowerShell
- ✅ 백업 파일 생성 확인 - PowerShell
- ✅ 자동 재시작 테스트 (kill 1 후 복구) - PowerShell
- ✅ 스케줄러 시간대 설정 적용 확인 - Docker


---

### ✅ Day 22 (2025-12-26) - 환경변수 보안 강화 및 운영 안정성
**완료 항목:**
- 환경변수 분리 (개발/운영)
  - .env.development: 개발 환경 설정
  - .env.production: 운영 환경 설정 (강화된 키)
  - .env.example: 템플릿 업데이트 (사용법 안내)
  - .gitignore: 환경 파일 제외 추가
- 업비트 API 재시도 로직 구현
  - UpbitApiService에 Retry 로직 추가
  - 3회 재시도 + 지수 백오프 (500ms → 5s)
  - 5xx 서버 오류, 429 Rate Limit 시 자동 재시도
  - 네트워크 오류 시 자동 재시도
- 운영용 Docker Compose 생성
  - docker-compose.prod.yml 생성
  - 리소스 제한 (MySQL 1G, Redis 512M, Backend 1.5G)
  - JVM 옵션 최적화 (-Xms512m -Xmx1024m)
  - 볼륨 분리 (_prod 접미사)
  - 자동 재시작 정책 (restart: always)
- HTTPS 준비 (템플릿)
  - frontend/nginx.ssl.conf: SSL 설정 템플릿
  - scripts/init-ssl.sh: Let's Encrypt 인증서 발급 스크립트
  - ⏳ 실제 적용은 Day 28로 이동 (도메인 확보 후)

**생성된 파일:**
- `.env.development` - 개발 환경 설정
- `.env.production` - 운영 환경 설정
- `docker-compose.prod.yml` - 운영용 Docker Compose
- `frontend/nginx.ssl.conf` - HTTPS 설정 템플릿
- `scripts/init-ssl.sh` - SSL 인증서 발급 스크립트

**수정된 파일:**
- `backend/src/main/java/com/cryptotrading/service/UpbitApiService.java` - 재시도 로직 추가
- `.env.example` - 환경 분리 안내 추가
- `.gitignore` - 환경 파일 제외 추가

**환경별 Docker 실행:**
```bash
# 개발 환경
docker-compose --env-file .env.development up -d --build

# 운영 환경
docker-compose -f docker-compose.prod.yml --env-file .env.production up -d --build
```

**테스트 완료:**
- ✅ .env.development로 Docker 실행 - PowerShell
- ✅ 컨테이너 상태 확인 (4개 모두 healthy) - PowerShell
- ✅ Health Check API (profile: dev 확인) - PowerShell
- ✅ 로그인 토큰 발급 - Postman
- ✅ 백엔드 로그 정상 확인 - Docker

**Day 28로 이동된 작업:**
- ⏳ HTTPS 실제 적용 (Let's Encrypt 인증서 발급)
- ⏳ Nginx SSL 설정 활성화

---

### ✅ Day 23 (2025-12-26) - 시스템 모니터링 및 알림 고도화
**완료 항목:**
- 시스템 모니터링 대시보드 (Backend)
  - MonitoringService: 시스템 메트릭 수집
  - MonitoringDTO: 모니터링 데이터 DTO
  - JVM 메모리 (Heap/Non-Heap) 사용량
  - DB 커넥션 풀 상태 (Active/Idle/Total)
  - Redis 연결 상태 및 메모리 사용량
  - 시스템 업타임, 스레드 정보, CPU 로드
- 슬로우 쿼리 모니터링
  - 1초 이상 쿼리 기록 및 표시
  - application.yml 슬로우 쿼리 로깅 설정
- 시스템 이상징후 자동 감지 (Backend)
  - MonitoringAlertService: 5분마다 시스템 상태 점검
  - JVM 메모리 80%/90% 경고 알림
  - DB 커넥션 풀 고갈 경고 알림
  - Redis 연결 끊김 경고 알림
  - 시간당 에러 10건 이상 경고 알림
  - 알림 중복 방지 (AtomicBoolean 플래그)
- 서버 시작/종료 Discord 알림 (Backend)
  - StartupNotificationConfig: @PostConstruct, @PreDestroy
  - 서버 시작 시 Discord 알림 발송
  - 서버 종료 시 Discord 알림 발송
- 관리자 멀티채널 알림 시스템 확장 
  - AdminAlertNotificationService: Admin 계정 전용 알림 서비스
  - Admin 프로필에 등록된 이메일로 시스템 알림 발송
  - Admin 프로필에 등록된 Discord User ID로 DM 알림 발송
  - 시스템 모니터링 알림 → Admin 이메일/Discord DM 추가
  - 서버 시작/종료 알림 → Admin 이메일/Discord DM 추가
  - JDA shutdown hook 비활성화로 종료 시 DM 발송 안정화
  - 동기/비동기 메서드 분리 (서버 종료 시 동기 처리)
- 관리자 대시보드 모니터링 UI (Frontend)
  - 시스템 모니터링 섹션 추가
  - JVM Heap 게이지, DB 커넥션 풀 상태
  - Redis 상태 칩, 시스템 정보 카드
  - 전체화면 상세 다이얼로그
  - 30초 자동 새로고침
- 거래 내역 아카이빙 스크립트
  - archive-transactions.ps1 (Windows)
  - archive-transactions.sh (Linux/Mac)
  - 월별 거래 내역 백업 및 압축
- UI 버그 수정 및 개선 :
  - BotMonitorView.vue 버그 수정
    - API 엔드포인트 수정: `/users/me` → `/user/profile`
    - Discord Bot 상태 필드명 수정: `enabled` → `botEnabled`
  - BotMonitorView.vue UI 개선
    - 수동제어 + 이메일 테스트 + 디스코드 DM 테스트 → 한 줄 3등분 배치 (`md="4"`)
    - 버튼 스타일 통일: `variant="outlined"` (테두리 스타일)
    - 이메일 테스트 버튼 색상 통일: `color="teal"`
    - 디스코드 DM 테스트 버튼 색상 통일: `color="deep-purple"`
    - 아이콘 변경: 수동 제어 `mdi-account-cog`, 디스코드 DM 테스트 `mdi-robot`
    - 제목-버튼 간격 증가 (`pt-6`)
  - DailyReportView.vue UI 개선
    - 손익 상세 카드 폰트 크기 증가 (아이콘 `size="28"`, 값 `text-h6`/`text-h5`)
    - 코인별 현황 테이블 폰트 크기 증가 (CSS `:deep(.v-data-table)`)

**API 엔드포인트:**
| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| GET | /api/admin/monitoring | 🔐 | 시스템 모니터링 데이터 (관리자) |

**생성된 파일 (Backend):**
- `service/MonitoringService.java` - 시스템 메트릭 수집
- `service/MonitoringAlertService.java` - 이상징후 감지 및 알림
- `service/AdminAlertNotificationService.java` - Admin 멀티채널 알림 서비스
- `config/StartupNotificationConfig.java` - 서버 시작/종료 알림
- `dto/admin/MonitoringDTO.java` - 모니터링 데이터 DTO

**생성된 파일 (Scripts):**
- `scripts/archive-transactions.ps1` - Windows 아카이빙 스크립트
- `scripts/archive-transactions.sh` - Linux/Mac 아카이빙 스크립트

**수정된 파일 (Backend):**
- `service/NotificationService.java` - sendSystemNotification() 메서드 추가, sendSystemNotificationSync() 동기 메서드 추가
- `service/DiscordBotService.java` - sendSystemAlertDM() 메서드 추가, setEnableShutdownHook(false) 설정
- `service/EmailService.java` - sendSystemAlert() 메서드 추가
- `service/MonitoringAlertService.java` - AdminAlertNotificationService 연동 추가
- `config/StartupNotificationConfig.java` - AdminAlertNotificationService 연동, 동기 발송 처리
- `repository/UserRepository.java` - findByRole(), findByRoleAndIsActive() 메서드 추가
- `controller/AdminController.java` - 모니터링 API 엔드포인트 추가
- `application.yml` - Actuator, 슬로우 쿼리 로깅 설정 추가

**수정된 파일 (Frontend):**
- `views/BotMonitorView.vue` - API 수정, UI 개선
- `views/DailyReportView.vue` - 폰트 크기 개선
- `views/ProfileView.vue` - v-alert 중복 아이콘 제거
- `views/AdminDashboardView.vue` - `import api` 누락 추가

**수정된 파일 (Frontend):**
- `views/AdminDashboardView.vue` - 모니터링 섹션 및 다이얼로그 추가
- `api/index.ts` - getMonitoring() API 추가

**모니터링 알림 임계값:**
| 항목 | 경고 | 위험 |
|------|------|------|
| JVM Heap 사용률 | 80% | 90% |
| DB 활성 커넥션 | 8개 | Max-1개 |
| Redis 연결 | - | 연결 끊김 |
| 시간당 에러 | 10건 | - |

**⭐ 관리자 알림 채널 (신규):**
| 알림 종류 | Discord Webhook | Admin Email | Admin Discord DM |
|----------|-----------------|-------------|------------------|
| 서버 시작 | ✅ | ✅ | ✅ |
| 서버 종료 | ✅ | ✅ | ✅ |
| JVM 메모리 경고 | ✅ | ✅ | ✅ |
| DB 커넥션 경고 | ✅ | ✅ | ✅ |
| Redis 연결 끊김 | ✅ | ✅ | ✅ |
| 에러 다수 발생 | ✅ | ✅ | ✅ |

**추가 수정 사항:**
- 서버 종료 알림 안정화
  - StartupNotificationConfig: CountDownLatch로 알림 완료 대기 (최대 10초)
  - docker-compose.yml: stop_grace_period: 30s 추가
- JDA Discord Bot 종료 순서 보장
  - setEnableShutdownHook(false): JDA 자체 shutdown hook 비활성화
  - Spring @PreDestroy에서 수동 종료 관리
  - 서버 종료 시 Admin DM 발송 후 Bot 종료
- KST 시간대 적용
  - StartupNotificationConfig: ZoneId.of("Asia/Seoul") 적용
  - MonitoringAlertService: 모든 시간 표시에 KST 적용
  - Discord 알림 시간이 한국 시간으로 정확히 표시

**테스트 완료:**
- ✅ Discord 서버 시작 알림 - Discord Webhook
- ✅ Discord 서버 종료 알림 - Discord Webhook
- ✅ Admin Discord DM 서버 시작 알림 - Discord DM
- ✅ Admin Discord DM 서버 종료 알림 - Discord DM
- ✅ Admin 이메일 서버 시작 알림 - Email
- ✅ Admin 이메일 서버 종료 알림 - Email
- ✅ 모니터링 API 응답 - Postman
- ✅ 관리자 대시보드 모니터링 섹션 - 브라우저
- ✅ 전체화면 다이얼로그 - 브라우저
- ✅ 30초 자동 새로고침 - 브라우저
- ✅ 봇 모니터링 프로필 로딩 정상 - 브라우저
- ✅ Discord Bot 상태 표시 정상 - 브라우저
- ✅ 수동제어/이메일/디스코드 카드 3등분 배치 - 브라우저
- ✅ 버튼 스타일 통일 (outlined) - 브라우저
- ✅ 일일 리포트 손익 상세 폰트 크기 - 브라우저
- ✅ 일일 리포트 코인별 현황 테이블 폰트 크기 - 브라우저

---

### ✅ Day 24 (2025-12-29) - AI 뉴스 분석 기반 구축
**완료 항목:**
- DB 스키마 추가
  - coin_news: 수집된 뉴스 저장 테이블
  - coin_news_analysis: 뉴스 분석 결과 테이블
  - 인덱스: coin_symbol, published_at, user_id, analysis_date
- Entity/DTO 생성
  - CoinNews Entity: 뉴스 데이터 엔티티
  - CoinNewsAnalysis Entity: 분석 결과 엔티티 (Sentiment enum 포함)
  - CoinNewsDTO, CoinNewsAnalysisDTO, RssNewsItem DTO
- Repository 구현
  - CoinNewsRepository: 당일 뉴스 조회, 중복 체크, 7일 초과 삭제
  - CoinNewsAnalysisRepository: 사용자별 분석 결과 관리, 가중치 초기화
- 뉴스 수집 서비스 (NewsCollectorService)
  - RSS Feed 파서 구현
  - CoinTelegraph: ✅ 정상 동작 (30건 수신)
  - CoinDesk: ⚠️ RSS 형식 변경으로 파싱 불가 (제외)
  - 코인 키워드 매칭 (BTC, ETH 등 10개 코인)
  - KST 기준 당일 뉴스만 필터링
  - 중복 뉴스 제거 (제목 + 출처 기준)
- 뉴스 API (NewsController)
  - POST /api/news/collect: 수동 뉴스 수집 (관리자)
  - GET /api/news/today/{symbol}: 당일 뉴스 조회
  - DELETE /api/news/cleanup: 7일 초과 데이터 삭제 (관리자)

**생성된 파일:**
- `entity/CoinNews.java` - 뉴스 엔티티
- `entity/CoinNewsAnalysis.java` - 분석 결과 엔티티
- `dto/news/CoinNewsDTO.java` - 뉴스 DTO
- `dto/news/CoinNewsAnalysisDTO.java` - 분석 결과 DTO
- `dto/news/RssNewsItem.java` - RSS 아이템 DTO
- `repository/CoinNewsRepository.java` - 뉴스 Repository
- `repository/CoinNewsAnalysisRepository.java` - 분석 Repository
- `service/NewsCollectorService.java` - 뉴스 수집 서비스
- `controller/NewsController.java` - 뉴스 API

**수정된 파일:**
- `docker/mysql/init.sql` - coin_news, coin_news_analysis 테이블 추가
- `application.yml` - news.collection 설정 추가
- `SecurityConfig.java` - /api/news/today/** permitAll 추가

**API 엔드포인트:**
| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| POST | /api/news/collect | 🔐 Admin | 뉴스 수동 수집 |
| GET | /api/news/today/{symbol} | ❌ | 당일 뉴스 조회 |
| DELETE | /api/news/cleanup | 🔐 Admin | 오래된 뉴스 삭제 |

**테스트 완료:**
- ✅ 테이블 생성 확인 (coin_news, coin_news_analysis) - MySQL
- ✅ 뉴스 수동 수집 (BTC 8건, ETH 2건 = 총 10건) - Postman
- ✅ 당일 뉴스 조회 - Postman
- ✅ 중복 수집 방지 (두 번째 수집 시 0건) - Postman
- ✅ CoinTelegraph RSS 파싱 (30건 수신) - 로그 확인

**알려진 이슈:**
- CoinDesk RSS: 형식 변경으로 파싱 불가 (CoinTelegraph만 사용)
- Docker 로그 한글 인코딩: 깨짐 (기능에 영향 없음)

**수집된 뉴스 예시:**
- "Bitfinex whales go long BTC for 2026" (CoinTelegraph)
- "Bitcoin helps USD's reserve status: Coinbase CEO" (CoinTelegraph)
- "Trend Research lifts ETH holdings to $1.8B" (CoinTelegraph)

---



## 📊 현재 진행 상황
- **전체 진척도**: 약 **94%**
- **Phase 1 (핵심 기능)**: 100% 완료 ✅
- **Phase 2 (고도화)**: 100% 완료 ✅
- **Phase 3 (안정화)**: **70%** 진행 중 🔄
- **Phase 4 (운영 준비)**: **40%** 진행 중 🔄

---

## 🗓️ 프로젝트 일정

| 구분 | 기간 | 상태 |
|------|------|------|
| Phase 1 (핵심 기능) | Day 1~9 | ✅ 완료 |
| Phase 2 (고도화) | Day 10~16 | ✅ 완료 |
| Phase 3 (안정화) | Day 17~21 | ✅ 완료 |
| Phase 4 (운영 준비) | Day 22~28 | 🔄 진행 예정 |
| **v1.0 릴리즈** | **Day 28** | 🎯 목표 |

---

## 🎯 다음 단계 (Day 21~28)

### 📅 상세 일정

### ✅ Day 21 완료 항목 (2025-12-25)
| 작업 | 상태 | 비고 |
|------|------|------|
| DB 자동 백업 | ✅ 완료 | backup-db.ps1, restore-db.ps1 |
| 로그 로테이션 | ✅ 완료 | Logback 일별 로테이션, 에러/거래 로그 분리 |
| 헬스체크 강화 | ✅ 완료 | Docker healthcheck, 자동 재시작 |
| 스케줄러 시간대 | ✅ 완료 | zone="Asia/Seoul" 설정 |


---

### 📅 상세 일정

#### ✅ Day 22: 환경변수 보안 + 운영 안정성 (2025-12-26 완료)
| 시간 | 작업 | 상세 | 상태 |
|------|------|------|------|
| 오후 | 환경변수 보안 | .env.development, .env.production 분리 | ✅ 완료 |
| 오후 | 업비트 API 재시도 | 3회 재시도 + 지수 백오프 | ✅ 완료 |
| 오후 | 운영용 Docker Compose | docker-compose.prod.yml 생성 | ✅ 완료 |
| 오후 | HTTPS 템플릿 준비 | nginx.ssl.conf, init-ssl.sh | ✅ 완료 |
| - | HTTPS 실제 적용 | Let's Encrypt 인증서 발급 | ⏳ Day 28로 이동 |

---

#### ✅ Day 23: 모니터링 (2025-12-26 완료)
| 시간 | 작업 | 상세 | 상태 |
|------|------|------|------|
| 오전 | 시스템 모니터링 대시보드 | JVM, DB, Redis 메트릭 + 관리자 UI | ✅ 완료 |
| 오전 | 서버 시작/종료 알림 | Discord 알림 자동 발송 | ✅ 완료 |
| 오후 | 시스템 이상징후 감지 | 5분마다 자동 점검 + Discord 알림 | ✅ 완료 |
| 오후 | 슬로우 쿼리 모니터링 | 1초 이상 쿼리 로깅 | ✅ 완료 |
| 오후 | 거래 내역 아카이빙 | 월별 백업 스크립트 (ps1, sh) | ✅ 완료 |

---

#### ✅ Day 24: AI 뉴스 분석 (1) - 기반 구축 (2025-12-29 완료)
| 시간 | 작업 | 상세 | 상태 |
|------|------|------|------|
| 오전 | DB 스키마 추가 | coin_news, coin_news_analysis 테이블 | ✅ 완료 |
| 오전 | Entity/DTO 생성 | CoinNews, CoinNewsAnalysis 엔티티 | ✅ 완료 |
| 오후 | 뉴스 수집 서비스 | RSS Feed 파서 (CoinTelegraph) | ✅ 완료 |
| 오후 | 당일 뉴스 필터링 | KST 기준 당일 발행 뉴스만 수집 | ✅ 완료 |

---

#### Day 25: AI 뉴스 분석 (2) - AI 연동
| 시간 | 작업 | 상세 |
|------|------|------|
| 오전 | Google Gemini API 연동 | 무료 API 클라이언트 구현 |
| 오전 | 프롬프트 설계 | 호재/악재 분류, 점수화 (-100~+100) |
| 오후 | 뉴스 분석 서비스 | NewsAnalysisService 구현 |
| 오후 | 점수 → 가중치 변환 | 평균 점수 계산, ±1% 매핑 로직 |

---

#### Day 26: AI 뉴스 분석 (3) - 지표 연동 + 알림
| 시간 | 작업 | 상세 |
|------|------|------|
| 오전 | SignalDetector 연동 | AI 가중치 buyThresholdPct 반영 |
| 오전 | 일일 초기화 스케줄러 | 매일 00:00 KST 가중치 리셋 |
| 오후 | 3시간 분석 스케줄러 | 뉴스 수집 → AI 분석 → 가중치 적용 |
| 오후 | 가중치 변경 알림 | 이메일/Discord DM 상세 내용 발송 |
| 오후 | 뉴스 데이터 정리 스케줄러 | 매일 04:00 KST 7일 초과 데이터 삭제 |
| 오후 | 코인 뉴스 페이지 UI | 게시판 형식 뉴스 조회 (Frontend) |
---

#### Day 27: 추가 기능 + 보안
| 시간 | 작업 | 상세 |
|------|------|------|
| 오전 | 2FA 인증 (Optional) | Google Authenticator 연동 |
| 오전 | IP 화이트리스트 (Optional) | 접속 IP 제한 |
| 오전 | 기간별/코인별 수익 분석 | 보유자산 페이지 내 수익 분석 UI |
| 오후 | 최종 보안 점검 | OWASP Top 10 체크리스트 |
| 오후 | 전체 시스템 테스트 | 통합 테스트, 시나리오 테스트 |

---

#### Day 28: 운영 문서 + HTTPS + v1.0 릴리즈
| 시간 | 작업 | 상세 |
|------|------|------|
| 오전 | 운영 문서 작성 | 아키텍처 다이어그램, 배포 절차서 |
| 오전 | 장애 대응 매뉴얼 | 오류 해결, 재시작 절차, 백업/복원 |
| 오후 | HTTPS 적용 (도메인 확보 시) | Let's Encrypt 인증서 발급, Nginx SSL 활성화 |
| 오후 | README 최종 업데이트 | 완료 항목 정리 |
| 오후 | v1.0 릴리즈 | Git 태깅, 최종 배포 |

---

### 📊 일정 요약

| Day | 주요 작업 | 카테고리 |
|-----|----------|----------|
| 21 | ✅ DB 백업, 로그 로테이션, 헬스체크, 시간대 설정 | ✅ 완료 |
| 22 | ✅ 환경변수 보안, API 재시도, 운영 Docker Compose | ✅ 완료 |
| 23 | ✅ 모니터링 대시보드, 슬로우 쿼리, 서버 알림 | ✅ 완료 |
| 24 | ✅ AI 뉴스 분석 - 기반 구축 | ✅ 완료 |
| 25 | AI 뉴스 분석 - Gemini API 연동 | 🟢 선택 |
| 26 | AI 뉴스 분석 - 지표 연동 + 뉴스 페이지 UI | 🟢 선택 |
| 27 | 2FA, IP 제한, 수익 분석 UI, 보안 점검, 테스트 | 🟢 선택 |
| 28 | 운영 문서 작성, v1.0 릴리즈 | 🔴 필수 |

---

### 🤖 AI 뉴스 분석 기능 상세

#### 개요
- **실행 주기**: 3시간마다 (스케줄러)
- **AI API**: Google Gemini API (무료)
- **분석 대상**: 각 사용자 투자 코인별 글로벌 뉴스
- **뉴스 범위**: KST 기준 당일 발행 뉴스만
- **적용 방식**: 호재/악재 분석 → buyThresholdPct ±0.5% 조정
- **초기화**: 매일 00:00 KST 가중치 0%로 리셋

#### 뉴스 소스 (신뢰도 기준 선별)
| 소스 | 유형 | 신뢰도 | 수집 방식 |
|-----------|---------|------|------|
| CoinTelegraph | https://cointelegraph.com/rss | ✅ 활성 | 월 20M+ 방문 |
| Bitcoin Magazine | https://bitcoinmagazine.com/feed | ✅ 활성 | 월 8M+ 방문 |
| **Decrypt** | https://decrypt.co/feed | ✅ **추가** | 월 15M+ 방문 |
| CoinDesk | - | ❌ 제거 | RSS 형식 변경 |
| Reuters | - | ❌ 제외 | 무료 RSS 미제공 |

**수집 테스트 결과 (2025-12-29)**
- CoinTelegraph: 30건 수신
- Bitcoin Magazine: 10건 수신
- Decrypt: 52건 수신
- 총 92건 수신 → 중복 제외 후 저장

※ 모든 소스는 **무료 RSS**이며 **글로벌 공신력**이 검증된 매체입니다.

#### 처리 흐름
```
1. 뉴스 수집 (투자 코인별)
   - RSS Feed / 검색 API 활용
   - KST 당일 발행 뉴스만 필터링
   - 코인 키워드 매칭 (BTC, 비트코인, ETH 등)
       ↓
2. AI 분석 (Google Gemini API - 무료)
   - 뉴스 제목/본문 분석
   - 호재/악재/중립 분류
   - 영향도 점수 (-100 ~ +100)
       ↓
3. 가중치 계산
   - 복수 뉴스 존재 시 점수 평균 계산
   - 평균 점수 → 가중치 변환 (-0.5%, 0%, +0.5%)
       ↓
4. 지표 조정 반영
   - buyThresholdPct에 가중치 적용
   - 호재: 매수 조건 완화 (+0.5%)
   - 악재: 매수 조건 강화 (-0.5%)
       ↓
5. 결과 저장 & 알림
   - coin_news_analysis 테이블 저장
   - 가중치 변경 시 이메일/Discord DM 발송
   - 알림 내용: 코인, 변경된 지표, 변경 사유(뉴스 요약)
```

#### 지표 조정 기준 
| AI 분석 점수 | 가중치 | buyThresholdPct 변화 | 예시 |
|-------------|--------|---------------------|------|
| +50 ~ +100 | +0.5% (호재) | 조건 완화 | -5% → -4.5% |
| -49 ~ +49 | 0% (중립) | 변동 없음 | -5% 유지 |
| -100 ~ -50 | -0.5% (악재) | 조건 강화 | -5% → -5.5% |

#### 호재/악재 동시 존재 시 처리 
```
예시: BTC 관련 뉴스 3건
- 뉴스 A: +70 (호재)
- 뉴스 B: -30 (약한 악재)
- 뉴스 C: +20 (약한 호재)

평균 점수: (+70 -30 +20) / 3 = +20
→ -49 ~ +49 범위 → 가중치 0% (중립)
```

#### 일일 초기화 로직
- 매일 00:00 KST 스케줄러 실행
- 모든 코인의 AI 가중치 0%로 리셋
- 당일 새로운 뉴스 분석 시작

#### 알림 내용 예시 
```
📰 AI 뉴스 분석 결과 - BTC

🔹 분석 시간: 2025-12-28 15:00 KST
🔹 분석 뉴스: 3건
🔹 평균 점수: +65 (호재)
🔹 가중치 변경: 0% → +1%

📊 지표 변경
- buyThresholdPct: -5% → -4.5%

📰 주요 뉴스 요약
1. [CoinDesk] 비트코인 ETF 순유입 역대 최고 기록
2. [Reuters] 기관 투자자 암호화폐 매수세 증가

#### 데이터 관리
| 항목 | 설정값 | 설명 |
|------|--------|------|
| 뉴스 보관 기간 | 7일 | coin_news 테이블 데이터 |
| 분석 결과 보관 기간 | 7일 | coin_news_analysis 테이블 데이터 |
| 정리 스케줄 | 매일 04:00 KST | 시스템 점검 시간에 실행 |
| 정리 방식 | created_at 기준 7일 초과 데이터 삭제 | 자동 배치 처리 |
```sql
-- 자동 정리 쿼리 (매일 04:00 KST 실행)
DELETE FROM coin_news WHERE created_at < DATE_SUB(NOW(), INTERVAL 7 DAY);
DELETE FROM coin_news_analysis WHERE created_at < DATE_SUB(NOW(), INTERVAL 7 DAY);
```

#### Gemini API 무료 한도
- 분당 15회 요청
- 일일 1,500회 요청
- 3시간마다 5코인 분석 시: 월 ~1,200회 (한도 내)

#### 코인 뉴스 페이지 (Frontend)
수집된 뉴스를 사용자가 조회할 수 있는 게시판 형식의 UI

| 기능 | 설명 |
|------|------|
| 페이징 | 페이지당 10/20/50건 선택 가능 |
| 필터링 | 코인 심볼별 필터 (BTC, ETH, XRP 등) |
| 검색 | 제목/내용 키워드 검색 |
| 정렬 | 최신순/오래된순 |
```

---

## 📋 수정 요약

| 수정 위치 | 변경 내용 |
|-----------|----------|
| Day 26 일정 테이블 | `코인 뉴스 페이지 UI` 행 추가 |
| 일정 요약 테이블 | Day 26 설명에 `뉴스 페이지 UI` 추가 |
| 프로젝트 구조 | `NewsView.vue` 파일 추가 |
| AI 뉴스 분석 상세 | 코인 뉴스 페이지 기능 설명 추가 |

---

## ✅ 프로젝트 지침 확인

프로젝트 지침에는 이미 다음 내용이 포함되어 있습니다:
```
👤 사용자 페이지
├── ...
└── 코인 뉴스 

코인 뉴스 페이지 상세              
├── 기능: 수집된 코인 뉴스를 게시판 형식으로 조회
├── 페이징: 페이지당 10/20/50건 선택 가능
├── 필터링: 코인 심볼별 필터 (BTC, ETH, XRP 등)
├── 검색: 제목/내용 키워드 검색
└── 정렬: 최신순/오래된순

---

### 우선순위:
1. 🔴 **높음**: 운영 문서 작성
2. 🟡 **중간**: 최종 보안 점검
3. 🟢 **낮음**: AI 뉴스 분석 기능 (Optional)

---

## 📋 향후 작업 목록 (Backlog)

### 🔐 보안 강화 (프로덕션 배포 전 필수)

| 작업 | 설명 | 우선순위 | 상태 |
|------|------|----------|------|
| HTTPS 적용 | Let's Encrypt SSL 인증서 설정 | 🔴 필수 | ⏳ Day 28 (도메인 확보 후) |
| Nginx SSL 설정 | HTTPS 리다이렉트, HSTS 헤더 | 🔴 필수 | ✅ 템플릿 준비 완료 |
| 환경변수 보안 | 개발/운영 환경 분리 | 🟡 권장 | ✅ Day 22 완료 |

### 💾 데이터 관리

| 작업 | 설명 | 우선순위 | 예상 시간 |
|------|------|----------|----------|
| DB 자동 백업 | 일일 백업 스크립트 (7일 보관) | 🔴 필수 | ✅ Day 21 완료 |
| 로그 로테이션 | Logback 일별 로테이션 설정 | 🟡 권장 | ✅ Day 21 완료 |
| 거래 내역 아카이빙 | 월별 거래 내역 백업 | 🟢 선택 | ✅ Day 23 완료 |

### 🖥️ 운영 안정성

| 작업 | 설명 | 우선순위 | 예상 시간 |
|------|------|----------|----------|
| 헬스체크 강화 | 컨테이너 자동 재시작 설정 | 🟡 권장 | ✅ Day 21 완료 |
| 스케줄러 시간대 | Asia/Seoul 시간대 설정 | 🟡 권장 | ✅ Day 21 완료 |
| 알림 장애 대응 | Discord/Email 발송 실패 시 대체 로직 | 🟢 선택 | 미완료 |
| 업비트 API 장애 대응 | API 호출 실패 시 재시도 로직 강화 | 🟢 선택 | ✅ Day 22 완료 |

### 📊 모니터링

| 작업 | 설명 | 우선순위 | 상태 |
|------|------|----------|------|
| 시스템 모니터링 대시보드 | JVM, DB, Redis 메트릭 시각화 | 🟢 선택 | ✅ Day 23 완료 |
| 슬로우 쿼리 모니터링 | 1초 이상 쿼리 로깅 | 🟢 선택 | ✅ Day 23 완료 |
| 시스템 이상징후 알림 | 자동 감지 + Discord 알림 | 🟢 선택 | ✅ Day 23 완료 |
| 서버 시작/종료 알림 | Discord 자동 알림 | 🟢 선택 | ✅ Day 23 완료 |

### 🚀 기능 확장 (Optional)

| 작업 | 설명 | 우선순위 | 예상 시간 |
|------|------|----------|----------|
| AI 뉴스 분석 | Google Gemini API (무료), 뉴스 수집, 지표 연동 | 🟢 선택 | 3일 |
| WebSocket 실시간 모니터링 | 실시간 가격/거래 업데이트 | 🟢 선택 | 1~2일 |
| 2FA 인증 | Google Authenticator 연동 | 🟢 선택 | 0.5일 |
| IP 화이트리스트 | 접속 IP 제한 기능 | 🟢 선택 | 2시간 |
| 기간별/코인별 수익 분석 | 보유자산 페이지 내 수익 통계 (오늘/이번달/올해/누적) | 🟢 선택 | 0.5일 |

---

## ⚠️ 실투자 전 필수 확인사항

### 🧪 테스트 단계
```
1단계: 소액 테스트 (1~2일)
├── 일일 한도: 10만원 이하
├── 코인: BTC 1종목만
├── 로그 모니터링 필수 (docker-compose logs -f backend)
└── Discord 알림 정상 수신 확인

2단계: 조건 조정 (3~5일)
├── 백테스팅 결과와 실제 거래 비교 분석
├── 매수 조건 완화/강화 조정
└── 거래 빈도 확인 및 최적화

3단계: 본격 운영
├── 일일 한도 단계적 증액
├── 코인 종목 추가 (최대 3~5개 권장)
└── 일일 리포트 정기 확인
```

### 📋 운영 체크리스트

| 시간 | 체크 항목 |
|------|----------|
| 오전 9시 | 업비트 점검 후 봇 정상 재개 확인 |
| 수시 | Discord DM 알림 수신 확인 |
| 저녁 | 일일 리포트 확인 (23:50 자동 발송) |
| 저녁 | 보유 자산, 거래 내역, 손익 확인 |

### 🚨 긴급 상황 대응

| 상황 | 대응 방법 |
|------|----------|
| 즉시 거래 중단 | 거래 설정 삭제 또는 사용자 비활성화 |
| 수동 매도 필요 | 업비트 앱/웹에서 직접 매도 |
| 시스템 오류 | `docker-compose restart backend` |
| 전체 시스템 중단 | `docker-compose down` |
| 긴급 정지 발동 확인 | `docker-compose logs backend \| grep "긴급 정지"` |

### ⚙️ 권장 초기 설정값

| 설정 | 권장값 | 설명 |
|------|--------|------|
| dailyLimitAmount | 100,000~500,000원 | 일일 기준 금액 (소액 시작) |
| dailyTradeLimitPct | 20~30% | 일일 실제 거래 한도 |
| maxPositionPct | 25~30% | 단일 종목 최대 비중 |
| dailyStopLossPct | -5% | 긴급 정지 조건 |
| maxHoldingsPerCoin | 2~3건 | 종목당 최대 보유 건수 |
| buyThresholdPct | -3~-5% | MA 대비 매수 하락률 |
| sellTargetPct | 2~3% | 목표 수익률 |
| stopLossPct | -5~-10% | 손절매 기준 |

---

## 📊 현재 시스템 완성도

| 항목 | 상태 | 비고 |
|------|------|------|
| 실투자 가능 여부 | ✅ 가능 | 업비트 API 연동 완료 |
| 보안 수준 | ✅ 양호 | HTTPS 적용 시 A등급 |
| 백테스팅 ↔ 실거래 일치 | ✅ 100% | 모든 지표 동일 적용 |
| 운영 안정성 | ✅ 양호 | 자동 재시작, 헬스체크 적용 |
| DB 백업 | ✅ 완료 | 일일 백업, 7일 보관 |
| 로그 관리 | ✅ 완료 | 일별 로테이션, 분리 저장 |
| **종합 완성도** | **97%** | 운영 문서 최종 정리만 남음 |

---

## 🚀 실행 방법

### 1. 환경 설정
```bash
# .env 파일 생성 (루트 디렉토리)
cp .env.example .env

# 필수 환경변수 설정
JWT_SECRET_KEY=your_jwt_secret_key_256bits_or_more
JWT_EXPIRATION=1800000
AES_SECRET_KEY=12345678901234567890123456789012
UPBIT_ACCESS_KEY=your_upbit_access_key
UPBIT_SECRET_KEY=your_upbit_secret_key

# Discord 알림 설정 (선택)
DISCORD_ENABLED=true
DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/your_webhook_url

# Discord Bot DM 알림 설정 (선택)
DISCORD_BOT_TOKEN=your_discord_bot_token
```

### 2. Docker 실행
```bash
# 개발 환경 실행
docker-compose --env-file .env.development up -d --build

# 운영 환경 실행 (프로덕션)
docker-compose -f docker-compose.prod.yml --env-file .env.production up -d --build

# 로그 확인
docker-compose logs -f

# 서비스 중지
docker-compose down
```

### 3. 접속
- **Frontend**: http://localhost
- **Backend API**: http://localhost/api
- **Health Check**: http://localhost/api/health

### 4. 기본 계정
- **관리자**
  - ID: admin
  - Password: Test1234!@

---

## 📁 프로젝트 구조
```
crypto-trading-system/
├── scripts/                      # 운영 스크립트
│   ├── backup-db.ps1             # Windows DB 백업
│   ├── backup-db.sh              # Linux/Mac DB 백업
│   ├── restore-db.ps1            # DB 복원
│   ├── init-ssl.sh               # SSL 인증서 발급 스크립트
│   ├── archive-transactions.ps1  # ⭐ Day 23: Windows 거래 아카이빙
│   └── archive-transactions.sh   # ⭐ Day 23: Linux 거래 아카이빙
├── backups/                      # 백업 저장소
│   └── mysql/                    # MySQL 백업 파일
├── .env.example                  # 환경변수 템플릿
├── .env.development              # 개발 환경 설정 (Git 제외)
├── .env.production               # 운영 환경 설정 (Git 제외)
├── docker-compose.yml            # 개발용 Docker Compose
├── docker-compose.prod.yml       # ⭐ Day 22: 운영용 Docker Compose
├── backend/                      # Spring Boot 백엔드
│   ├── src/main/java/com/cryptotrading/
│   │   ├── controller/           # REST API 컨트롤러
│   │   │   ├── AuthController.java
│   │   │   ├── UserController.java
│   │   │   ├── CoinController.java
│   │   │   ├── TradingSettingController.java
│   │   │   ├── TransactionController.java
│   │   │   ├── BotController.java
│   │   │   ├── NotificationController.java 
│   │   │   ├── BacktestController.java    
│   │   │   ├── AdminController.java
│   │   │   └── NewsController.java               # ⭐ Day 24: 뉴스 API
│   │   ├── service/              # 비즈니스 로직
│   │   │   ├── CacheService.java
│   │   │   ├── TechnicalIndicatorService.java
│   │   │   ├── SignalDetectorService.java
│   │   │   ├── RiskManagementService.java      
│   │   │   ├── TradingBotService.java          
│   │   │   ├── NotificationService.java      
│   │   │   ├── DailyReportService.java      
│   │   │   ├── BacktestService.java    
│   │   │   ├── EmailService.java     
│   │   │   ├── AdminService.java
│   │   │   ├── DiscordBotService.java        
│   │   │   ├── LoginAttemptService.java          # ⭐ Day 20: 로그인 시도 제한
│   │   │   ├── MonitoringService.java            # ⭐ Day 23: 시스템 메트릭 수집
│   │   │   ├── MonitoringAlertService.java       # ⭐ Day 23: 이상징후 감지
│   │   │   ├── AdminAlertNotificationService.java # ⭐ Day 23: Admin 멀티채널 알림
│   │   │   └── NewsCollectorService.java         # ⭐ Day 24: 뉴스 수집 서비스
│   │   ├── config/               # 설정 클래스
│   │   │   ├── SecurityConfig.java
│   │   │   ├── NotificationConfig.java
│   │   │   ├── SwaggerConfig.java                # ⭐ Day 20: Swagger/OpenAPI
│   │   │   ├── StartupNotificationConfig.java    # ⭐ Day 23: 서버 시작/종료 알림
│   │   │   └── security/         # Security 핸들러
│   │   │       ├── CustomAuthenticationEntryPoint.java
│   │   │       └── CustomAccessDeniedHandler.java
│   │   ├── filter/               # 필터
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── RateLimitFilter.java
│   │   │   └── RequestLoggingFilter.java         # 응답 시간 로깅
│   │   ├── exception/            # 예외 처리
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── ErrorCode.java             
│   │   │   ├── BusinessException.java      
│   │   │   ├── EntityNotFoundException.java   
│   │   │   ├── DuplicateResourceException.java 
│   │   │   ├── UnauthorizedException.java 
│   │   │   ├── UpbitApiException.java     
│   │   │   └── TradingException.java       
│   │   ├── scheduler/            # 스케줄러
│   │   │   └── TradingScheduler.java
│   │   ├── repository/           # 데이터 접근 계층
│   │   │   ├── UserRepository.java               # ⭐ Day 23: findByRoleAndIsActive() 추가
│   │   │   ├── TradingSettingRepository.java
│   │   │   ├── TransactionRepository.java
│   │   │   ├── CoinInfoRepository.java
│   │   │   ├── CoinNewsRepository.java           # ⭐ Day 24: 뉴스 Repository
│   │   │   └── CoinNewsAnalysisRepository.java   # ⭐ Day 24: 분석 Repository
│   │   ├── entity/               # JPA 엔티티
│   │   │   ├── User.java
│   │   │   ├── TradingSetting.java
│   │   │   ├── Transaction.java
│   │   │   ├── CoinInfo.java
│   │   │   ├── CoinNews.java                     # ⭐ Day 24: 뉴스 엔티티
│   │   │   └── CoinNewsAnalysis.java             # ⭐ Day 24: 분석 결과 엔티티
│   │   ├── dto/                  # 데이터 전송 객체
│   │   │   ├── common/           # 공통 DTO
│   │   │   │   ├── ApiResponse.java
│   │   │   │   └── PageResponse.java
│   │   │   ├── indicator/        # 기술적 지표 DTO
│   │   │   ├── bot/              # 봇 관련 DTO
│   │   │   ├── upbit/            # 업비트 API DTO
│   │   │   ├── notification/     # 알림 DTO
│   │   │   ├── backtest/         # 백테스트 DTO
│   │   │   ├── admin/            # 관리자 DTO
│   │   │   │   ├── SystemStatsDTO.java
│   │   │   │   ├── AdminUserDTO.java
│   │   │   │   └── MonitoringDTO.java            # ⭐ Day 23: 모니터링 DTO
│   │   │   └── news/             # ⭐ Day 24: 뉴스 DTO
│   │   │       ├── CoinNewsDTO.java              # ⭐ Day 24: 뉴스 DTO
│   │   │       ├── CoinNewsAnalysisDTO.java      # ⭐ Day 24: 분석 결과 DTO
│   │   │       └── RssNewsItem.java              # ⭐ Day 24: RSS 아이템 DTO
│   │   └── util/                 # 유틸리티
│   │       ├── JwtUtil.java
│   │       └── EncryptionUtil.java
│   └── src/main/resources/
│       ├── application.yml
│       ├── logback-spring.xml    # 로그 설정
│       └── templates/email/      # 이메일 템플릿
│   └── Dockerfile                # curl 설치
│
├── frontend/                     # Vue.js 프론트엔드
│   ├── src/
│   │   ├── api/                  # API 클라이언트
│   │   │   └── index.ts
│   │   ├── components/           # 공통 컴포넌트
│   │   │   ├── TheHeader.vue
│   │   │   ├── TheSidebar.vue
│   │   │   └── GlobalSnackbar.vue
│   │   ├── composables/          # Composition API 유틸
│   │   │   ├── useErrorHandler.ts
│   │   │   └── useSnackbar.ts
│   │   ├── views/                # 페이지 컴포넌트
│   │   │   ├── LoginView.vue
│   │   │   ├── SignupView.vue
│   │   │   ├── DashboardView.vue
│   │   │   ├── ProfileView.vue
│   │   │   ├── TradingSettingsView.vue
│   │   │   ├── TransactionHistoryView.vue
│   │   │   ├── HoldingsView.vue
│   │   │   ├── BotMonitorView.vue
│   │   │   ├── DailyReportView.vue
│   │   │   ├── BacktestView.vue
│   │   │   └── AdminDashboardView.vue
│   │   ├── stores/               # Pinia 상태 관리
│   │   │   ├── auth.ts
│   │   │   └── coin.ts
│   │   ├── types/                # TypeScript 타입
│   │   │   ├── index.ts
│   │   │   ├── bot.ts
│   │   │   ├── backtest.ts
│   │   │   └── error.ts
│   │   ├── router/               # Vue Router
│   │   │   └── index.ts
│   │   ├── App.vue
│   │   └── main.ts
│   ├── nginx.conf                # 개발용 Nginx 설정
│   ├── nginx.ssl.conf            # HTTPS용 Nginx 설정 템플릿
│   ├── index.html
│   └── vite.config.ts
│
├── docker/                       # Docker 설정
│   └── mysql/
│       └── init.sql              # DB 초기화 (coin_news, coin_news_analysis 포함)
│
├── docker-compose.yml            # 개발용 (헬스체크, 재시작)
├── docker-compose.prod.yml       # ⭐ Day 22: 운영용 (리소스 제한)
├── .env.example                  # 환경변수 템플릿
├── .env.development              # ⭐ Day 22: 개발 환경 (Git 제외)
├── .env.production               # ⭐ Day 22: 운영 환경 (Git 제외)
├── .gitignore                    # backups/, .env.* 제외
└── README.md
```

---

## 🔐 보안 고려사항

### 환경변수 보호
- `.env` 파일을 Git에 커밋하지 않음
- `.env.example`은 템플릿으로 제공
- 프로덕션에서는 더 강력한 비밀키 사용

### 비밀번호 보안
- BCrypt 해싱 (strength 10)
- 단방향 암호화로 원본 복원 불가능
- 솔트 자동 생성

### API 키 보안
- AES-256 암호화
- 환경변수에서 암호화 키 관리
- 데이터베이스에 암호화된 상태로 저장

### JWT 토큰
- HS512 알고리즘
- 30분 만료 시간
- Stateless 인증

### CORS
- localhost:80, localhost:3000만 허용
- 프로덕션에서는 실제 도메인으로 변경

---

## 🐛 트러블슈팅

### 컨테이너 재시작
```bash
docker-compose restart [service-name]
# 예: docker-compose restart backend
```

### 로그 확인
```bash
# 전체 로그
docker-compose logs

# 특정 서비스 로그
docker-compose logs backend

# 실시간 로그
docker-compose logs -f backend
```

### 데이터베이스 초기화
```bash
# 볼륨 삭제 후 재생성
docker-compose down -v
docker-compose up -d
```

### 포트 충돌
```bash
# 사용 중인 포트 확인
netstat -ano | findstr :80
netstat -ano | findstr :8080
netstat -ano | findstr :3306

# 프로세스 종료 (관리자 권한)
taskkill /PID [PID] /F
```

---

## 📝 라이선스
이 프로젝트는 개인 학습 및 연구 목적으로 제작되었습니다.

---

## 📞 연락처
프로젝트 관련 문의사항이 있으시면 이슈를 등록해주세요.
