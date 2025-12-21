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

**생성된 파일 (Backend):**
- `service/CacheService.java` - Redis 캐시 서비스
- `filter/RateLimitFilter.java` - Rate Limiting 필터

**수정된 파일:**
- `service/CoinInfoService.java` - 캐싱 적용
- `config/SecurityConfig.java` - RateLimitFilter 등록
- `exception/ErrorCode.java` - C006 (Rate Limit 초과) 추가
- `frontend/nginx.conf` - 보안 헤더 추가

**테스트 완료:**
- ✅ 활성 코인 캐싱 - Redis CLI
- ✅ 현재가 캐싱 (캐시 히트) - 로그 확인
- ✅ Rate Limit 헤더 - Postman
- ✅ Rate Limit 초과 (429) - PowerShell 스크립트
- ✅ 보안 헤더 적용 - 브라우저
- ✅ DB 인덱스 존재 확인 - MySQL CLI

---

## 📊 현재 진행 상황
- **전체 진척도**: 약 92%
- **Phase 1 (핵심 기능)**: 100% 완료 ✅
- **Phase 2 (고도화)**: 95% 완료 ✅
- **Phase 3 (안정화)**: 75% 진행중

---

## 🎯 다음 단계 (Day 18)

### 예정 작업:
1. **운영 문서 작성**
   - API 문서화 (Swagger/OpenAPI)
   - 배포 가이드
   - 운영 매뉴얼

2. **최종 테스트**
   - 전체 기능 통합 테스트
   - 부하 테스트
   - 보안 점검 최종 확인

3. **선택적 기능 (Optional)**
   - WebSocket 실시간 모니터링
   - 텔레그램 봇 알림

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
```

### 2. Docker 실행
```bash
# 전체 서비스 시작
docker-compose up -d --build

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
├── backend/                    # Spring Boot 백엔드
│   ├── src/main/java/com/cryptotrading/
│   │   ├── controller/        # REST API 컨트롤러
│   │   │   ├── AuthController.java
│   │   │   ├── UserController.java
│   │   │   ├── CoinController.java
│   │   │   ├── TradingSettingController.java
│   │   │   ├── TransactionController.java
│   │   │   ├── BotController.java
│   │   │   ├── NotificationController.java 
│   │   │   ├── BacktestController.java    
│   │   │   └── AdminController.java   
│   │   ├── service/           # 비즈니스 로직
│   │   │   ├── CacheService.java             # ⭐ 신규: Redis 캐시
│   │   │   ├── TechnicalIndicatorService.java
│   │   │   ├── SignalDetectorService.java
│   │   │   ├── RiskManagementService.java
│   │   │   ├── TradingBotService.java
│   │   │   ├── NotificationService.java      
│   │   │   ├── DailyReportService.java      
│   │   │   ├── BacktestService.java    
│   │   │   ├── EmailService.java     
│   │   │   └── AdminService.java
│   │   ├── config/            # 설정 클래스
│   │   │   ├── SecurityConfig.java
│   │   │   ├── NotificationConfig.java
│   │   │   └── security/      # Security 핸들러
│   │   │       ├── CustomAuthenticationEntryPoint.java
│   │   │       └── CustomAccessDeniedHandler.java
│   │   ├── filter/            # ⭐ 신규: 필터
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   └── RateLimitFilter.java          # ⭐ 신규: Rate Limiting
│   │   ├── exception/         # ⭐ 확장: 예외 처리
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── ErrorCode.java              # ⭐ 추가
│   │   │   ├── BusinessException.java      # ⭐ 추가
│   │   │   ├── EntityNotFoundException.java    # ⭐ 추가
│   │   │   ├── DuplicateResourceException.java # ⭐ 추가
│   │   │   ├── UnauthorizedException.java  # ⭐ 추가
│   │   │   ├── UpbitApiException.java      # ⭐ 추가
│   │   │   └── TradingException.java       # ⭐ 추가
│   │   ├── scheduler/         # 스케줄러
│   │   │   └── TradingScheduler.java
│   │   ├── repository/        # 데이터 접근 계층
│   │   ├── entity/            # JPA 엔티티
│   │   ├── dto/               # 데이터 전송 객체
│   │   │   ├── common/        # ⭐ 추가: 공통 DTO
│   │   │   │   ├── ApiResponse.java
│   │   │   │   └── PageResponse.java
│   │   │   ├── indicator/     # 기술적 지표 DTO
│   │   │   ├── bot/           # 봇 관련 DTO
│   │   │   ├── upbit/         # 업비트 API DTO
│   │   │   ├── notification/  # 알림 DTO
│   │   │   ├── backtest/      # 백테스트 DTO
│   │   │   └── admin/         # 관리자 DTO
│   │   ├── filter/            # 필터
│   │   │   └── JwtAuthenticationFilter.java
│   │   └── util/              # 유틸리티
│   │       ├── JwtUtil.java
│   │       └── EncryptionUtil.java
│   └── src/main/resources/
│       ├── application.yml
│       └── templates/email/   # 이메일 템플릿
│
├── frontend/                   # Vue.js 프론트엔드
│   ├── src/
│   │   ├── api/               # API 클라이언트
│   │   │   └── index.ts
│   │   ├── components/        # 공통 컴포넌트
│   │   │   ├── TheHeader.vue
│   │   │   ├── TheSidebar.vue
│   │   │   └── GlobalSnackbar.vue    # ⭐ 추가
│   │   ├── composables/       # ⭐ 추가: Composition API 유틸
│   │   │   ├── useErrorHandler.ts
│   │   │   └── useSnackbar.ts
│   │   ├── views/             # 페이지 컴포넌트
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
│   │   ├── stores/            # Pinia 상태 관리
│   │   │   ├── auth.ts
│   │   │   └── coin.ts
│   │   ├── types/             # TypeScript 타입
│   │   │   ├── index.ts
│   │   │   ├── bot.ts
│   │   │   ├── backtest.ts
│   │   │   └── error.ts       # ⭐ 추가
│   │   ├── router/            # Vue Router
│   │   │   └── index.ts
│   │   ├── App.vue
│   │   └── main.ts
│   ├── index.html
│   └── vite.config.ts
│
├── docker-compose.yml
├── .env.example
├── .gitignore
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
