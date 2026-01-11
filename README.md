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
  - ⏳ 실제 적용은 Day 29로 이동 (도메인 확보 후)

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

**Day 29로 이동된 작업:**
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
| GET | /api/news/today/{symbol} | ❌ | 당일 뉴스 	조회 |
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

### ✅ Day 25 (2025-12-29) - AI 뉴스 분석 (2) - AI 연동 + 최적화
**완료 항목:**
- AI API 연동 (Gemini → Groq 전환)
  - Google Gemini API: Rate Limit 문제 (15 req/분, 1,500 req/일)
  - Groq API로 전환: 30 req/분, 14,400 req/일 (무료)
  - GeminiApiService 전면 개편 (Groq API 호출)
  - application.yml에 groq 설정 추가
  - .env.development에 GROQ_API_KEY, GROQ_MODEL 추가
- 프롬프트 설계
  - 호재/악재 분류 프롬프트
  - 점수화 (-1.0 ~ +1.0)
  - JSON 배열 응답 형식
- 뉴스 분석 서비스 (NewsAnalysisService)
  - 미분석 뉴스만 조회하여 API 호출 최소화
  - 벌크 분석: N건 뉴스 → 1회 API 호출
  - 분석 결과 캐싱 (새 뉴스 없으면 0회 API 호출)
  - 평균 점수 계산 및 감성 판단 (POSITIVE/NEGATIVE/NEUTRAL)
- DB 스키마 확장
  - coin_news 테이블에 analyzed, analyzed_at, sentiment_score 컬럼 추가
  - 분석 완료 플래그로 중복 분석 방지
- 점수 → 가중치 변환
  - 평균 점수 기반 가중치 계산
  - buyThresholdPct ±0.5% 조정 로직

**생성된 파일:**
- `dto/news/NewsAnalysisResultDTO.java` - 분석 결과 DTO

**수정된 파일:**
- `service/GeminiApiService.java` - ✅ **Groq API로 전면 개편**
  - Gemini API → Groq API 호출로 변경
  - analyzeBulkNews() 벌크 분석 메서드 추가
  - NewsAnalysisResult 내부 클래스 추가
- `service/NewsAnalysisService.java` - ✅ **벌크 분석 + 캐싱 로직 추가**
  - 미분석 뉴스만 조회
  - 벌크 분석 후 개별 뉴스에 결과 저장
  - 캐싱으로 중복 API 호출 방지
- `entity/CoinNews.java` - analyzed, analyzedAt, sentimentScore 필드 추가
- `repository/CoinNewsRepository.java` - 미분석 뉴스 조회 메서드 추가
- `application.yml` - groq API 설정 추가
- `.env.development` - GROQ_API_KEY, GROQ_MODEL 추가
- `docker-compose.yml` - GROQ_API_KEY, GROQ_MODEL 환경변수 추가

**API 엔드포인트:**
| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| POST | /api/news/analyze | ✅ | 뉴스 AI 분석 실행 |

**최적화 효과:**
| 항목 | 기존 (Gemini) | 최적화 후 (Groq) |
|------|---------------|------------------|
| Rate Limit | 15 req/분 | **30 req/분** |
| 일일 한도 | 1,500 req | **14,400 req** |
| 뉴스 N건 분석 | N회 API 호출 | **1회** (벌크) |
| 재분석 (새 뉴스 0건) | N회 API 호출 | **0회** (캐시) |
| 504 타임아웃 | 빈번 발생 | **없음** |
| 분석 소요 시간 | 5분+ | **1초 미만** |

**테스트 완료:**
- ✅ Groq API 초기화 확인 - Docker 로그
- ✅ 벌크 뉴스 분석 (2건 → 1회 API) - Postman
- ✅ 분석 결과 응답 정상 (score, sentiment) - Postman
- ✅ 캐싱 동작 확인 (재요청 시 API 미호출) - Postman
- ✅ analyzed 플래그 업데이트 - MySQL

**DB 마이그레이션:**
```sql
ALTER TABLE coin_news ADD COLUMN analyzed BOOLEAN DEFAULT FALSE;
ALTER TABLE coin_news ADD COLUMN analyzed_at DATETIME NULL;
ALTER TABLE coin_news ADD COLUMN sentiment_score DECIMAL(5,2) NULL;
CREATE INDEX idx_coin_news_analyzed ON coin_news(analyzed, coin_symbol);
```

---

### ✅ Day 26 (2025-12-30) - AI 뉴스 분석 (3) - 지표 연동 + 뉴스 페이지 UI
**완료 항목:**
- SignalDetector AI 가중치 연동
  - buyThresholdPct에 AI 가중치 자동 반영
  - 호재: 매수 조건 완화, 악재: 매수 조건 강화
- 스케줄러 구현 (3개)
  - 00:00 KST: AI 가중치 초기화 (매일 자정 리셋)
  - 매 3시간 (0, 3, 6, 9, 12, 15, 18, 21시): 뉴스 수집 + AI 분석
  - 04:00 KST: 7일 초과 뉴스 데이터 삭제
- 가중치 변경 알림
  - 분석 완료 시 Discord DM + 이메일 자동 발송
  - 미등록 사용자는 알림 SKIP 처리
- 코인 뉴스 페이지 UI (Frontend)
  - NewsView.vue: 게시판 형식 뉴스 조회
  - 페이징 (10/20/50건), 필터링 (코인별), 검색 (키워드)
  - 뉴스 상세 다이얼로그, 외부 링크 열기
  - 사이드바 메뉴 추가 ("코인 뉴스")
- 가중치 변경 테스트 알림 API 추가
  - POST /api/notifications/email/test-weight-change
  - POST /api/notifications/discord/test-weight-change
- 봇 모니터링 페이지 UI 개선
  - 이메일/디스코드 테스트 버튼 2열 배치
  - 가중치 변경 테스트 버튼 추가

**생성된 파일 (Frontend):**
- `views/NewsView.vue` - 코인 뉴스 페이지

**수정된 파일 (Backend):**
- `service/SignalDetectorService.java` - AI 가중치 연동
- `scheduler/TradingScheduler.java` - 스케줄러 3개 추가
- `service/NewsAnalysisService.java` - 가중치 변경 알림 기능
- `controller/NotificationController.java` - 가중치 테스트 API 추가

**수정된 파일 (Frontend):**
- `views/BotMonitorView.vue` - 가중치 테스트 버튼, 2열 배치
- `components/TheSidebar.vue` - 코인 뉴스 메뉴 추가
- `router/index.ts` - /news 라우트 추가

**API 엔드포인트 (신규):**
| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| GET | /api/news/list | ✅ | 뉴스 목록 조회 (페이징) |
| GET | /api/news/{newsId} | ✅ | 뉴스 상세 조회 |
| POST | /api/notifications/email/test-weight-change | ✅ | 가중치 변경 테스트 이메일 |
| POST | /api/notifications/discord/test-weight-change | ✅ | 가중치 변경 테스트 DM |

**스케줄러 실행 시간:**
| 스케줄러 | 실행 시간 (KST) | 설명 |
|----------|----------------|------|
| 가중치 초기화 | 00:00 | 모든 코인 AI 가중치 0%로 리셋 |
| 뉴스 수집/분석 | 매 3시간 | RSS 수집 → AI 분석 → 가중치 적용 |
| 데이터 정리 | 04:00 | 7일 초과 뉴스 삭제 |

**테스트 완료:**
- ✅ 뉴스 수집 API - Postman
- ✅ AI 뉴스 분석 API - Postman
- ✅ AI 가중치 조회 API - Postman
- ✅ 뉴스 목록 조회 (페이징) - Postman
- ✅ 코인 뉴스 페이지 UI - 브라우저
- ✅ 사이드바 메뉴 정상 표시 - 브라우저
- ✅ 3시간 스케줄러 정상 실행 - Docker 로그
- ✅ 가중치 변경 테스트 이메일 - 봇 모니터링
- ✅ 가중치 변경 테스트 DM - 봇 모니터링

---

### ✅ Day 27 (2025-12-30) - Oracle Cloud ARM64 배포 + DB 스키마 검증
**완료 항목:**
- Oracle Cloud 인스턴스 생성 및 설정
  - VM.Standard.A1.Flex (ARM64 아키텍처)
  - 4 OCPU, 24GB RAM, Ubuntu 22.04
  - Public IP: 158.179.161.29
- 서버 초기 설정
  - Docker & Docker Compose 설치
  - UFW 방화벽 설정 (22, 80, 443, 8080 포트)
  - Oracle Cloud 보안 목록 설정 (Ingress Rules)
- Docker 이미지 ARM64 호환성 수정
  - backend/Dockerfile: `eclipse-temurin:17-jdk-alpine` → `eclipse-temurin:17-jdk`
  - backend/Dockerfile: `eclipse-temurin:17-jre-alpine` → `eclipse-temurin:17-jre`
  - frontend/Dockerfile: `node:18-alpine` → `node:18`
  - frontend/Dockerfile: `nginx:alpine` → `nginx:latest`
- MySQL 설정 추가
  - docker/mysql/conf.d/my.cnf 생성 (KST 시간대, utf8mb4 문자셋)
  - init.sql 수정: `CREATE INDEX IF NOT EXISTS` → 주석 처리 (MySQL 8.0 호환)
  - coin_news_analysis 테이블 sentiment 컬럼: `VARCHAR(20)` → `ENUM` 변경
- 환경변수 보안 수정
  - .env.production: `$` 특수문자 제거 (Docker 환경변수 파싱 오류 방지)
  - .env.production: AES_SECRET_KEY 길이 수정 (31자 → 32자, AES-256 필수)
  - .env.production: JWT_SECRET_KEY 길이 수정 (54자 → 64자, HS512 최적화)
  - docker-compose.prod.yml: Groq API 환경변수 추가 (GROQ_API_KEY, GROQ_API_MODEL)
- 프론트엔드 수정
  - TradingSettingsView.vue: AI 뉴스 분석 설명 수정 (`±2%` → `±0.5%`)
- BCrypt 비밀번호 해시 수정
  - Python bcrypt로 admin123! 해시 생성
  - init.sql: admin 계정 password_hash 업데이트 (`$2b$10$...` 형식)
- DB 스키마 교차검증 및 수정
  - JPA Entity vs init.sql 불일치 항목 발견 및 수정
  - trading_settings 테이블: 9개 컬럼 추가
    - Day 14 기술적 지표: rsi_period, rsi_buy_threshold, rsi_sell_threshold, bb_period, bb_multiplier, volume_threshold
    - Day 19 리스크 관리: daily_trade_limit_pct, max_position_pct, daily_stop_loss_pct
  - transactions 테이블: highest_price 컬럼 추가 (트레일링 스톱용)
  - coin_news 테이블: 3개 컬럼 추가 (analyzed, analyzed_at, sentiment_score)
  - coin_news_analysis 테이블: average_score 주석 수정 (-1.0 ~ +1.0)
  - password_reset_tokens 테이블: 신규 추가
- docker-compose.prod.yml 환경변수 동기화
  - GEMINI_API_KEY, GEMINI_API_ENABLED 환경변수 추가
  - GROQ_API_KEY, GROQ_MODEL 환경변수 추가
  - stop_grace_period: 30s 추가 (graceful shutdown)
- docker-compose.yml vs docker-compose.prod.yml 비교 분석
  - 의도적 차이 확인 (운영 전용 설정)
  - 잠재적 문제 식별 (UPBIT API 키, TZ 시간대 등)

**배포 환경**:
| 항목 | 값 |
|------|-----|
| Cloud Provider | Oracle Cloud (Pay-as-you-go) |
| Instance Type | VM.Standard.A1.Flex |
| Architecture | ARM64 (Ampere) |
| CPU | 4 OCPU |
| Memory | 24GB |
| OS | Ubuntu 22.04 LTS |
| Public IP | 158.179.161.29 |

**수정된 파일:**
| 파일 경로 | 수정 내용 |
|----------|----------|
| `backend/Dockerfile` | ARM64 호환 이미지로 변경 (alpine 제거) |
| `frontend/Dockerfile` | ARM64 호환 이미지로 변경 (alpine 제거) |
| `docker/mysql/conf.d/my.cnf` |  MySQL KST 시간대, utf8mb4 설정 |
| `docker/mysql/init.sql` | 누락 컬럼/테이블 추가, INDEX 주석 처리, sentiment ENUM 변경, BCrypt 해시 수정 |
| `.env.production` | `$` 특수문자 제거, **AES_SECRET_KEY 32자**, **JWT_SECRET_KEY 64자** |
| `docker-compose.prod.yml` | AI API 환경변수 추가, stop_grace_period 추가 |
| `frontend/src/views/TradingSettingsView.vue` | AI 뉴스 분석 설명 ±0.5%로 수정 |

**init.sql 수정 상세:**
| 테이블 | 추가된 컬럼/내용 |
|--------|-----------------|
| trading_settings | rsi_period, rsi_buy_threshold, rsi_sell_threshold, bb_period, bb_multiplier, volume_threshold, daily_trade_limit_pct, max_position_pct, daily_stop_loss_pct (9개) |
| transactions | highest_price (1개) |
| coin_news | analyzed, analyzed_at, sentiment_score (3개) |
| coin_news_analysis | average_score 주석 수정 |
| password_reset_tokens | 테이블 신규 추가 |

**해결한 주요 이슈:**
1. **ARM64 이미지 미지원**
   - 문제: `eclipse-temurin:17-jdk-alpine`이 ARM64 미지원
   - 해결: alpine 제거한 일반 이미지로 변경
2. **환경변수 파싱 오류**
   - 문제: 비밀번호 내 `$` 문자가 변수로 인식됨
   - 해결: `$` 문자 제거한 비밀번호 사용
3. **MySQL 8.0 구문 오류**
   - 문제: `CREATE INDEX IF NOT EXISTS` 미지원
   - 해결: 해당 구문 주석 처리
4. **스키마 불일치 (sentiment 컬럼)**
   - 문제: Java Entity는 ENUM, init.sql은 VARCHAR
   - 해결: init.sql에서 ENUM 타입으로 변경
5. **BCrypt 해시 불일치**
   - 문제: 기존 해시값이 admin123!와 불일치
   - 해결: Python bcrypt로 새 해시 생성 후 적용
6. **Groq API 환경변수 누락**
   - 문제: docker-compose.prod.yml에 Groq 환경변수 미전달
   - 해결: GROQ_API_KEY, GROQ_API_MODEL 환경변수 추가
7. **JPA Entity vs init.sql 불일치**
   - 문제: Day 14, 19, 25에 추가된 컬럼들이 init.sql에 누락
   - 해결: 전체 교차검증 후 누락 항목 일괄 추가
8. **AES_SECRET_KEY 길이 불일치**
   - 문제: 31자로 설정되어 API 키 암호화 실패 (400 Bad Request)
   - 해결: 32자로 수정 (AES-256 필수 조건)
9. **JWT_SECRET_KEY 길이 불일치**
   - 문제: 54자로 설정되어 HS512 최적화 미달
   - 해결: 64자로 수정 (512비트 최적)

**배포 명령어:**
```bash
# 서버 접속
ssh -i ~/.ssh/crypto-key ubuntu@158.179.161.29

# 프로젝트 클론 또는 업데이트
cd ~/crypto-trading-system
git pull origin main

# 운영 환경 실행
docker compose -f docker-compose.prod.yml --env-file .env.production up -d --build

# 상태 확인
docker ps
docker logs crypto-backend-prod --tail 50
```

**서비스 접속 정보:**
| 서비스 | URL |
|--------|-----|
| Frontend | http://158.179.161.29 |
| Backend API | http://158.179.161.29:8080/api |
| Health Check | http://158.179.161.29:8080/api/health |

**로그인 계정:**
- 아이디: `admin`
- 비밀번호: `admin123!`

**테스트 완료:**
- ✅ 서버 SSH 접속 - 터미널
- ✅ Docker 설치 및 실행 - 서버
- ✅ 모든 컨테이너 healthy 상태 - 서버
- ✅ MySQL 9개 테이블 생성 - 서버
- ✅ Groq API 초기화 완료 - Docker 로그
- ✅ Discord Bot 연결 완료 - Docker 로그
- ✅ 서버 시작 알림 발송 - Discord
- ✅ Health Check API - curl
- ✅ 로그인 API (admin/admin123!) - curl
- ✅ 브라우저 접속 - 브라우저
- ✅ 브라우저 로그인 - 브라우저
- ✅ init.sql 스키마 검증 완료 - 로컬
- ✅ docker-compose.prod.yml 환경변수 동기화 - 로컬
- ✅ API 키 저장 정상 동작 (AES 암호화) - 브라우저

---

### ✅ Day 28 (2025-12-31) - 대시보드 재구성 및 코인 목록 페이지
**완료 항목:**
- 대시보드 화면 전면 재구성 (DashboardView.vue)
  - 실시간 시간 표시 (우측 상단, BOLD체, 1초 업데이트)
  - 사용자 정보 카드 레이아웃 개선
    - admin 옆에 관리자/API 칩 좌우 배치
    - 디스코드 ID 로봇 아이콘 변경
    - 투자기간 자동 계산 및 강조 표시
  - 4개 통계 카드 한줄 배치 (총 손익, 총 평가액, 오늘 매수/매도)
  - 봇 상태 카드 (대기중 색상 변경)
  - 업비트 실제 잔고 섹션 신규 추가
  - 거래 설정 요약 카드 (미설정 시 버튼 강조)
  - 일일 한도 카드 (진행바 시각화)
  - 매수 조건 카드 완전 재구성
    - 코인명 | 현재가→목표가 | 대기 한줄 표시
    - 진행 바 시각화
    - 이격도(dropRate) 프론트엔드 직접 계산
  - 수익 현황 섹션 (누적 총 수익 검정 테두리 강조)
  - 자산 변동 추이 차트 (기간 버튼 대비 강화, 사용자 지정 기간 추가)
  - 최근 거래 카드 (가로 배치)
  - 시스템 알림 카드 (빨간색 배경)
- 코인 목록 페이지 신규 생성 (CoinListView.vue)
  - 활성 코인 데이터 테이블
  - 검색/필터/정렬 기능
  - 시가총액 순위 기준 기본 정렬
  - 페이지네이션 기반 가격 조회 (429 오류 해결)
  - 벌크 API 호출 (getMultiplePrices)
  - 시세 조회 진행 상황 표시
  - 코인 상세 정보 다이얼로그 (30개 코인 설명)
  - 거래 설정에 추가 버튼
- Backend 수정
  - CoinInfoService bulk API 파라미터 파싱 수정
  - MATIC 코인 자동 비활성화 로직 (@EventListener)
- 시간대 KST 통일 (전 시스템)
  - docker-compose.yml: 모든 컨테이너에 `TZ: Asia/Seoul` 추가
  - docker-compose.prod.yml: 모든 컨테이너에 `TZ: Asia/Seoul` 추가
  - CryptoTradingApplication.java: JVM 시간대 KST 설정 (`@PostConstruct`)
  - backend JAVA_OPTS: `-Duser.timezone=Asia/Seoul` 추가
- AdminDashboardView 오류 수정
  - `fetchStats()` 함수 추가 (누락으로 인한 에러 해결)
  - `fetchUsers()` 함수 추가 (배열/페이징 응답 모두 처리)
  - 사용자 목록 테이블 정상 렌더링
- 프론트엔드 시간 표시 함수 개선
  - `formatLastLogin()`: `toLocaleString('ko-KR')` 방식으로 통일
  - `formatBotTimeDisplay()`: `toLocaleString('ko-KR')` 방식으로 통일
  - `formatDateTime()`: `toLocaleString('ko-KR')` 방식으로 통일
  - 로컬/클라우드 환경 모두 KST 정확히 표시

**생성된 파일 (Frontend):**
- `views/CoinListView.vue` - 코인 목록 페이지

**수정된 파일 (Frontend):**
- `views/DashboardView.vue` - 전면 재구성 (10개 패널)
- `components/TheSidebar.vue` - 코인 목록 메뉴 추가
- `router/index.ts` - /coin-list 라우트 추가

**수정된 파일 (Backend):**
- `service/CoinInfoService.java` - bulk API 파싱 수정, MATIC 비활성화 로직

**수정된 파일:**
| 파일 | 변경 내용 |
|------|----------|
| `docker-compose.yml` | mysql, redis, backend, frontend에 `TZ: Asia/Seoul` 추가 |
| `docker-compose.prod.yml` | redis, frontend에 `TZ: Asia/Seoul` 추가, backend JAVA_OPTS에 시간대 추가 |
| `CryptoTradingApplication.java` | `@PostConstruct`로 JVM 시간대 KST 설정 |
| `AdminDashboardView.vue` | `fetchStats()`, `fetchUsers()` 함수 추가 |
| `DashboardView.vue` | `formatLastLogin()`, `formatBotTimeDisplay()` 개선 |

**대시보드 패널 구성:**
| 패널 | 설명 |
|------|------|
| 사용자+통계 | 사용자 정보 + 4개 통계 카드 한줄 배치 |
| 봇 상태 | 자동매매 상태, 마지막/다음 실행 시간 |
| 업비트 실제 잔고 | KRW 잔고, 코인 평가액, 총 자산 |
| 거래 설정 요약 | 거래 코인, 매수/매도 조건 |
| 일일 한도 | 한도 사용량, 종목별 보유 현황 |
| 매수 조건 | 코인별 현재가→목표가, 이격도 진행바 |
| 수익 현황 | 평가 수익, 실현 수익, 누적 총 수익 |
| 자산 변동 추이 | 기간별 차트, 사용자 지정 기간 |
| 코인별 성과 | 코인별 거래 횟수, 손익 |
| 최근 거래 | 최근 5건 거래 내역 |
| 시스템 알림 | API 키 미등록, 설정 필요 등 경고 |

**해결한 주요 이슈:**
1. **429 Too Many Requests 오류**
   - 문제: 코인 목록 페이지에서 개별 API 호출로 Rate Limit 초과
   - 해결: 벌크 API (getMultiplePrices) 호출로 변경, 페이지네이션 기반 조회
2. **이격도(dropRate) 0.00% 표시 문제**
   - 문제: 백엔드 maDropRate 필드 미제공
   - 해결: 프론트엔드에서 (현재가-MA20)/MA20*100 직접 계산
3. **MATIC/POL 심볼 변경 문제**
   - 문제: 업비트에서 MATIC→POL 심볼 변경
   - 해결: 서버 시작 시 KRW-MATIC 자동 비활성화 (@EventListener)
4. **bulk API 파라미터 파싱 오류**
   - 문제: CoinInfoService에서 symbols 문자열 파싱 실패
   - 해결: Arrays.asList(symbols.split(",")) 변환 로직 추가

**테스트 완료:**
- ✅ 대시보드 전체 패널 렌더링 - 브라우저
- ✅ 실시간 시간 표시 - 브라우저
- ✅ 투자기간 자동 계산 - 브라우저
- ✅ 매수 조건 이격도 계산 - 브라우저
- ✅ 매수 조건 현재가→목표가 표시 - 브라우저
- ✅ 업비트 실제 잔고 조회 - 브라우저
- ✅ 코인 목록 페이지 렌더링 - 브라우저
- ✅ 코인 목록 벌크 가격 조회 - 브라우저
- ✅ 코인 상세 다이얼로그 - 브라우저
- ✅ MATIC 자동 비활성화 - Docker 로그
- ✅ 사이드바 메뉴 정상 표시 - 브라우저
- ✅ Docker 컨테이너 시간대 확인 (date 명령어) - 서버
- ✅ 대시보드 마지막 로그인 시간 정상 표시 - 브라우저
- ✅ 자동매매 봇 수행 시간 정상 표시 - 브라우저
- ✅ 관리자 페이지 사용자 목록 정상 렌더링 - 브라우저
- ✅ 관리자 페이지 마지막 로그인 시간 정상 표시 - 브라우저

★★★ 백테스팅 버그 수정 ★★★**

**문제점:**
- 백테스팅 시 과거 기간 설정해도 현재 날짜 기준 데이터만 조회됨
- 예: 2022-12-01 ~ 2023-01-30 설정 시 거래 0건 발생

**원인:**
- `fetchCandleData()` 메서드에서 `getDayCandles()` 호출 시 현재 날짜 기준 조회
- 업비트 API는 `to` 파라미터 없이 호출하면 오늘부터 역순 조회

**해결:**
- `initialToDate` 변수 추가: 요청 종료일+1일 09:00 기준
- `getDayCandles()` → `getDayCandlesWithTo()` 변경
- 페이징 초기값도 `initialToDate`로 설정

**수정된 파일:**
- `backend/src/main/java/com/cryptotrading/service/BacktestService.java`

**★★★ 백테스팅 전략 검증 결과 ★★★**

| 구간 | 기간 | 시장상황 | 총 수익 | 수익률 | 승률 | MDD | 평가 |
|------|------|----------|---------|--------|------|-----|------|
| 1️⃣ | 2020-10 ~ 2021-04 | 급등장 | +134,393원 | +13.21% | 69.6% | -9.49% | ✅ 양호 |
| 2️⃣ | 2022-04 ~ 2022-07 | 급락장 | -408,243원 | -41.25% | 37.5% | -51.07% | ⚠️ 개선필요 |
| 3️⃣ | 2023-03 ~ 2023-09 | 횡보장 | +85,122원 | +8.27% | 68.8% | -6.55% | ✅ 우수 |
| 4️⃣ | 2024-01 ~ 2024-06 | 변동장 | +103,632원 | +10.13% | 76.1% | -5.39% | ✅ 우수 |
| 5️⃣ | 2024-10 ~ 2024-12 | 최근 | +14,178원 | +1.36% | 54.5% | -1.46% | ✅ 정상 |

**테스트 설정:**
- 코인: BTC, ETH, XRP, SOL (4종)
- 초기 자본: 1,000,000원
- 이동평균선: 20일 / 매수기준: -6% / 목표수익률: 4% / 손절매: -8%
- 트레일링 스톱: 4% / RSI: 14일 (32~68) / 볼린저밴드: 20일, 2배
- 리스크 관리: 일일 최대 20%, 단일 종목 25%, 긴급정지 -5%

**전략 평가:**
| 시장 상황 | 적합도 | 비고 |
|----------|--------|------|
| 횡보장 | ⭐⭐⭐⭐⭐ | 최적의 성과 (손익비 1.5+) |
| 완만한 상승장 | ⭐⭐⭐⭐ | 안정적 수익 (10~13%) |
| 급등장 | ⭐⭐⭐ | 보수적 (시장 대비 수익 제한) |
| 급락장 | ⭐ | 심각한 손실 (추가 보호 필요) |

**발견된 문제점:**
1. 급락장에서 MDD -51% 발생 (긴급정지 -5%가 일일 기준이라 누적 손실 방지 못함)
2. 하락장에서 매수→손절 반복으로 손실 누적 (176회 거래)
3. SOL 전 구간 저조한 성과 (변동성 높은 코인 제외 고려)

---

### ✅ Day 29 (2026-01-04) - 급락장 보호 기능 구현 및 최적화
**완료 항목:**
- 급락장 보호 기능 3종 구현 (Backend)
  - **시장 추세 필터**: BTC 20일 이동평균선 하회 시 전체 매수 중단
  - **누적 손실률 긴급정지**: 초기 자본 대비 누적 손실 도달 시 거래 중단
  - **연속 손절 제한**: 동일 코인 연속 손절 시 해당 코인 24시간 매수 금지
- DB 스키마 확장
  - trading_settings 테이블 3개 컬럼 추가
    - use_market_trend_filter (기본값: FALSE)
    - cumulative_loss_limit_pct (기본값: -10)
    - consecutive_stop_loss_limit (기본값: 3)
- 백테스팅 보호 기능 연동
  - BacktestService 급락장 보호 로직 구현
  - SimulationState 확장 (연속 손절 추적)
  - checkMarketTrend(): BTC MA20 체크
  - isEmergencyStopTriggered(): 누적 손실 체크
  - isConsecutiveStopLossBlocked(): 연속 손절 체크
- 거래 설정 페이지 UI 확장 (Frontend)
  - 급락장 보호 설정 섹션 추가
  - 시장 추세 필터 ON/OFF 스위치
  - 누적 손실 한도 슬라이더 (-5% ~ -30%)
  - 연속 손절 제한 슬라이더 (2회 ~ 10회)
- 백테스팅 페이지 UI 확장 (Frontend)
  - 급락장 보호 설정 섹션 추가
  - 보호 기능별 토글 및 파라미터 입력
- 파라미터 최적화 테스트 (5가지 시장 상황 × 6가지 설정)
  - 보호 OFF vs 시장필터 ON vs 다양한 손실 한도 비교
  - 급등장, 급락장, 횡보장, 상승장, 최근 시장 테스트
- 최적 기본값 도출 및 적용
  - 시장 추세 필터: **OFF** (상승장 수익 100% 유지)
  - 누적 손실 한도: **-10%** (급락장 손실 55% 감소)
  - 연속 손절 제한: **3회** (과매매 방지)
- HTTPS 적용 (DuckDNS + Let's Encrypt)
  - 무료 도메인: `crypto-trading-prd.duckdns.org`
  - Let's Encrypt SSL 인증서 발급
  - nginx.ssl.conf 설정 (HTTP→HTTPS 리다이렉트, 보안 헤더)
  - docker-compose.prod.yml SSL 볼륨 활성화
  - frontend/Dockerfile 443 포트 추가
  - Oracle Cloud 보안 목록 443 포트 개방
- CORS 운영 도메인 추가
  - SecurityConfig.java에 DuckDNS 도메인 허용
- SSL 자동 갱신 설정
  - scripts/renew-ssl.sh: 인증서 갱신 스크립트
  - scripts/setup-cron.sh: Cron 설정 스크립트
  - Cron 등록: 매월 1일, 15일 새벽 3시 자동 갱신
- AI 뉴스 분석 날짜 필터링 버그 수정
  - 문제: 자정 직후 실행 시 검색 범위가 `00:00 ~ 현재시간`으로 1초 미만이라 뉴스 0건 분석
  - 원인: 미분석 뉴스 조회 시 **당일 시간 범위** 기준이라 과거 수집 뉴스 제외됨
  - 해결: **당일 발행 날짜 기준** (`DATE(published_at) = 오늘`)으로 변경
- 뉴스 수집 후 즉시 AI 분석 기능 추가
  - 기존: 웹 "뉴스 수집" 버튼 → 수집만 (분석은 스케줄러 대기)
  - 변경: 웹 "뉴스 수집" 버튼 → **수집 + 즉시 분석** 동시 실행
  - NewsController의 `collectNews()` 메서드에 분석 로직 추가
- CoinNewsRepository 새 메서드 추가
  - `findUnanalyzedNewsByDate()`: 날짜 기준 미분석 뉴스 조회
  - `DATE(published_at) = :targetDate` 조건으로 당일 발행 뉴스만 조회
- 뉴스 수집 시 analyzed 기본값 설정 버그 수정
  - 문제: 새 뉴스 저장 시 analyzed 필드가 NULL로 저장되어 미분석 뉴스 조회 시 누락
  - 원인: NewsCollectorService에서 CoinNews 엔티티 생성 시 analyzed 값 미설정
  - 해결: `.analyzed(false)` 명시적 설정으로 새 뉴스는 항상 미분석 상태로 저장

**추가된 DB 컬럼:**
```sql
ALTER TABLE trading_settings 
ADD COLUMN use_market_trend_filter BOOLEAN DEFAULT FALSE COMMENT '시장 추세 필터 사용 여부',
ADD COLUMN cumulative_loss_limit_pct INT DEFAULT -10 COMMENT '누적 손실 한도 (%)',
ADD COLUMN consecutive_stop_loss_limit INT DEFAULT 3 COMMENT '연속 손절 제한 횟수';
```

**생성된 파일:**
- `scripts/renew-ssl.sh` - SSL 인증서 자동 갱신 스크립트
- `scripts/setup-cron.sh` - Cron 설정 스크립트
- `ssl/fullchain.pem` - SSL 인증서 (Git 제외)
- `ssl/privkey.pem` - SSL 개인키 (Git 제외)

**수정된 파일 (Backend):**
- `entity/TradingSetting.java` - 급락장 보호 필드 3개 추가
- `dto/TradingSettingDTO.java` - 급락장 보호 필드 3개 추가, Validation
- `dto/backtest/BacktestRequestDTO.java` - 급락장 보호 필드 3개 추가
- `service/TradingSettingService.java` - 새 필드 처리 로직
- `service/BacktestService.java` - 급락장 보호 로직 구현
- `service/RiskManagementService.java` - 실거래용 급락장 보호 로직
- `service/TradingBotService.java` - 급락장 보호 기능 연동
- `controller/BacktestController.java` - 기본값 수정
- `frontend/nginx.ssl.conf` - DuckDNS 도메인 적용
- `docker-compose.prod.yml` - SSL 볼륨 주석 해제
- `frontend/Dockerfile` - EXPOSE 80 443
- `backend/src/main/java/com/cryptotrading/config/SecurityConfig.java` - CORS 도메인 추가
- `backend/src/main/java/com/cryptotrading/repository/CoinNewsRepository.java` - 날짜 기준 조회 메서드 추가
- `backend/src/main/java/com/cryptotrading/service/NewsAnalysisService.java` - 날짜 필터링 로직 수정
- `backend/src/main/java/com/cryptotrading/controller/NewsController.java` - 수집 후 즉시 분석 로직 추가
- `backend/src/main/java/com/cryptotrading/service/NewsCollectorService.java` - 뉴스 저장 시 analyzed=false 기본값 설정

**수정된 파일 (Frontend):**
- `views/TradingSettingsView.vue` - 급락장 보호 설정 UI 추가
- `views/BacktestView.vue` - 급락장 보호 설정 UI 추가
- `types/index.ts` - TradingSetting 타입 확장
- `types/backtest.ts` - BacktestRequest 타입 확장

**수정된 파일 (Database):**
- `docker/mysql/init.sql` - trading_settings 테이블 3개 컬럼 추가

**서비스 접속 정보:**
| 서비스 | URL |
|--------|-----|
| Frontend (HTTPS) | https://crypto-trading-prd.duckdns.org |
| Frontend (HTTP→리다이렉트) | http://crypto-trading-prd.duckdns.org |
| Backend API | https://crypto-trading-prd.duckdns.org/api |
| Health Check | https://crypto-trading-prd.duckdns.org/api/health |

**★★★ 백테스팅 파라미터 최적화 결과 ★★★**

**5가지 시장 상황 × 6가지 설정 종합 비교:**

| 시장 | 기간 | 보호OFF | -10%,3회 | -15%,3회 | -20%,5회 | -25%,10회 | 시장필터ON |
|------|------|---------|----------|----------|----------|-----------|------------|
| 급등장 | 2020.10~2021.04 | +13.21% | +13.21% | +13.21% | +13.21% | +13.21% | +3.12% |
| **급락장** | 2022.04~2022.07 | **-41.25%** | **-18.57%** | -25.59% | -28.16% | -32.14% | -4.15% |
| 횡보장 | 2023.03~2023.09 | +8.27% | +8.27% | +8.27% | +8.27% | +8.27% | -1.01% |
| 상승장 | 2024.01~2024.06 | +10.13% | +10.13% | +10.13% | +10.13% | +10.13% | +0.63% |
| 최근 | 2024.10~2024.12 | +1.36% | +1.36% | +1.36% | +1.36% | +1.36% | +1.35% |
| **5개 합산** | - | **-70,918원** | **+152,690원** | +82,726원 | +57,537원 | +18,877원 | +2,098원 |

**🏆 최적 설정 결정:**
시장 추세 필터: OFF
누적 손실 한도: -10%
연속 손절 제한: 3회

**최적 설정 선정 이유:**
| 지표 | 보호 OFF | 최적 설정 (-10%, 3회) | 개선율 |
|------|----------|----------------------|--------|
| 급등장 수익 | +13.21% | +13.21% | **100% 유지** |
| 급락장 손실 | -41.25% | -18.57% | **55% 감소** |
| 급락장 MDD | -51.07% | -24.90% | **51% 감소** |
| 횡보장 수익 | +8.27% | +8.27% | **100% 유지** |
| 상승장 수익 | +10.13% | +10.13% | **100% 유지** |
| 5개 합산 | -70,918원 | +152,690원 | **+223,608원** |

**투자 성향별 권장 설정:**
| 투자 성향 | 시장필터 | 누적손실 | 연속손절 | 급락장 MDD | 상승장 수익 |
|-----------|----------|----------|----------|------------|------------|
| **균형형 (권장)** | OFF | -10% | 3회 | -25% | 100% |
| 중립형 | OFF | -15% | 3회 | -35% | 100% |
| 공격형 | OFF | -25% | 10회 | -46% | 100% |
| 보수형 | ON | -15% | 3회 | -4% | 24% |

**CoinNewsRepository 추가 메서드:**
```java
@Query("SELECT n FROM CoinNews n WHERE n.coinSymbol = :coinSymbol " +
       "AND (n.analyzed = false OR n.analyzed IS NULL) " +
       "AND DATE(n.publishedAt) = :targetDate " +
       "ORDER BY n.publishedAt DESC")
List findUnanalyzedNewsByDate(
    @Param("coinSymbol") String coinSymbol,
    @Param("targetDate") LocalDate targetDate);
```

**뉴스 수집 + 즉시 분석 동작 흐름:**
```
1. 웹 "뉴스 수집" 버튼 클릭
2. RSS Feed 수집 (CoinTelegraph, Bitcoin Magazine, Decrypt)
3. ⭐ 즉시 AI 분석 시작 (각 코인별)
4. Groq API 호출 (벌크 분석)
5. 분석 결과 저장 + 가중치 계산
6. Discord DM + 이메일 알림 발송
7. 완료 응답 반환
```

**변경된 동작 비교:**
| 트리거 | 기존 | 변경 후 |
|--------|------|---------|
| 웹 "뉴스 수집" 버튼 | 수집만 | ⭐ **수집 + 즉시 분석** |
| 스케줄러 (3시간마다) | 수집 + 분석 | 수집 + 분석 (변경 없음) |
| POST /api/news/analyze/{symbol} | 특정 코인 분석 | 특정 코인 분석 (변경 없음) |

**테스트 완료:**
- ✅ 급락장 보호 기능 구현 - Backend
- ✅ DB 스키마 확장 (3개 컬럼 추가) - MySQL
- ✅ 백테스팅 보호 기능 연동 - Backend
- ✅ 거래 설정 UI 급락장 보호 섹션 - 브라우저
- ✅ 백테스팅 UI 급락장 보호 섹션 - 브라우저
- ✅ 5가지 시장 상황 백테스팅 테스트 - 브라우저
- ✅ 6가지 설정 조합 비교 테스트 - 브라우저
- ✅ 최적 기본값 적용 - Backend/Frontend
- ✅ init.sql 스키마 동기화 - MySQL
- ✅ SSL 인증서 발급 - 서버
- ✅ HTTPS 접속 (HTTP/2 200) - curl
- ✅ 보안 헤더 적용 확인 (HSTS, CSP 등) - curl
- ✅ HTTP→HTTPS 리다이렉트 - 브라우저
- ✅ 로그인 정상 작동 - 브라우저
- ✅ 대시보드 접속 - 브라우저
- ✅ 뉴스 수집 API 호출 시 즉시 분석 실행 - Postman
- ✅ BTC 23건 분석 완료, 평균점수 0.29 - 로그 확인
- ✅ ETH 4건 분석 완료, 평균점수 0.60 - 로그 확인
- ✅ 총 27건 뉴스 수집+분석 동시 완료 - Postman
- ✅ 가중치 계산 정상 (BTC: +0.18%) - Postman
- ✅ Discord DM 알림 발송 - Discord
- ✅ Email 알림 발송 - Email
- ✅ 웹 UI 분석완료 상태 즉시 표시 - 브라우저
- ✅ 새 뉴스 저장 시 analyzed=false 설정 확인 - MySQL

---

### ✅ Day 30 (2026-01-08) - 릴리즈 노트 게시판 + 2FA 인증 + 기능 개선
**완료 항목:**
- 릴리즈 노트 게시판 기능 구현
  - DB 스키마: release_notes 테이블 (id, title, content, author_id, author_name, created_at, updated_at, is_deleted)
  - Backend: ReleaseNote Entity, Repository, Service, Controller
  - Frontend: ReleaseNotesView.vue 게시판 페이지
  - 라우팅: /release-notes 경로 추가
  - 사이드바: 릴리즈 노트 메뉴 추가 (mdi-bullhorn 아이콘)
  - 권한: 조회(모든 사용자), 작성/수정/삭제(ADMIN만)
- 대시보드 연동
  - 시스템 알림 카드에 최신 공지사항 표시
  - 클릭 시 /release-notes 페이지로 이동
  - 한줄 간결 표시 (info 아이콘 + 제목 + 날짜)
- 게시판 기능 개선
  - 페이지당 건수 선택 (10/20/50건)
  - 검색 기능 (제목, 작성자, 내용)
  - 글번호 순번화 (삭제와 무관하게 현재 목록 기준)
  - 작성일 한줄 표시 (YYYY-MM-DD HH:mm)
- 코인 뉴스 페이지 개선
  - 하단 Items per page 중복 컨트롤 제거 (hide-default-footer)
  - 뉴스 수집 API 타임아웃 60초로 증가 (외부 RSS 수집 시간 고려)
- IP 화이트리스트 기능 구현
  - DB 스키마: users 테이블에 allowed_ips JSON 컬럼 추가
  - Backend: User 엔티티, UserService, AuthService, AuthController, UserController 수정
  - Frontend: ProfileView.vue IP 화이트리스트 카드 추가
  - 기능: IP 추가/삭제, 현재 IP 조회, 화이트리스트 비활성화
  - 제한: 최대 3개 IP 등록 가능
  - 보안: 등록된 IP에서만 로그인 허용 (비활성화 시 모든 IP 허용)

- 문제 발견: Docker Bridge 네트워크에서 모든 접속이 172.18.0.1 (Gateway IP)로 표시
- 근본 원인: Docker iptables NAT로 인해 원본 클라이언트 IP가 손실됨
- 해결 방안: Frontend(Nginx) 컨테이너를 network_mode: host로 변경
-환경별 설정 분리:
      - 개발 환경 (Windows/Mac): network_mode: host 미지원 → 기존 bridge 방식 유지
      - 운영 환경 (Linux/Oracle Cloud): network_mode: host 적용 → 실제 IP 전달
- 수정 파일:
      - docker-compose.yml (개발용): bridge 네트워크 유지, proxy_pass http://backend:8080
      - docker-compose.prod.yml (운영용): network_mode: "host" 적용
      - frontend/nginx.conf (개발용): proxy_pass http://backend:8080 (기존 유지)
      - frontend/nginx.ssl.conf (운영용): proxy_pass http://127.0.0.1:8080 (⭐ 변경)
- 2FA 인증 (Google Authenticator) 구현
  - Backend: TotpService (TOTP 생성/검증), UserService (2FA 상태 관리)
  - Backend: UserController (2FA 설정 API 4개 엔드포인트)
  - Backend: AuthController (로그인 시 2FA 검증, 2FA_REQUIRED 응답)
  - Frontend: AccountSecurityView.vue (보안 설정 페이지 - QR코드, OTP 입력)
  - Frontend: LoginView.vue (OTP 입력 필드 추가)
  - Frontend: auth.ts (2FA_REQUIRED 응답 처리 로직)
  - Frontend: api/index.ts (401 응답 인터셉터에 2FA_REQUIRED 처리 추가)
  - DB 스키마: users 테이블에 totp_secret, two_factor_enabled 컬럼 추가
  - 기능: 2FA 활성화/비활성화, QR코드 스캔, OTP 코드 검증
  - 라이브러리: dev.samstevens.totp (백엔드), qrcode (프론트엔드)
- 로그인 에러 메시지 상세 표시 개선
  - 문제: IP 화이트리스트 차단 시 "로그인에 실패했습니다"만 표시
  - 원인: api/index.ts 인터셉터에서 401 응답 가공 시 원본 response 손실
  - 해결: 로그인/회원가입 API는 원본 에러 응답 유지하도록 수정
  - 결과: "허용되지 않은 IP입니다. 등록된 IP: xxx" 상세 메시지 표시

** IP 화이트리스트 환경별 설정 상세:**

| 환경 | Docker Compose 파일 | Nginx 설정 | 네트워크 모드 | proxy_pass | IP 표시 |
|------|---------------------|------------|--------------|------------|---------|
| **개발 (Windows/Mac)** | `docker-compose.yml` | `nginx.conf` | `bridge` (기존) | `backend:8080` | Docker IP (172.18.0.1) |
| **운영 (Linux/Oracle)** | `docker-compose.prod.yml` | `nginx.ssl.conf` | `host` | `127.0.0.1:8080` | **실제 클라이언트 IP** ✅ |

** 운영 환경 docker-compose.prod.yml 변경 내용:**
```yaml
  frontend:
    # IP 화이트리스트 실제 IP 전달을 위해 host 네트워크 사용
    # 기존: ports: - "80:80" - "443:443" / networks: - crypto-network-prod
    # 변경: network_mode: "host"
    # 효과: $remote_addr가 실제 클라이언트 IP를 받음 (172.18.0.1 대신)
    network_mode: "host"
    # ports 제거 (host 모드에서는 컨테이너가 직접 호스트 포트 사용)
    # networks 제거 (host 모드에서는 Docker 네트워크 사용 불가)
```

** 운영 환경 nginx.ssl.conf 변경 내용:**
```nginx
# backend:8080 → 127.0.0.1:8080 (host 네트워크 모드)
# 모든 proxy_pass 위치에 적용:
# - /swagger-ui/, /swagger-ui.html, /v3/api-docs, /swagger-resources, /webjars/
# - /api/, /ws/

location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;  # ⭐ 변경
    # ... 기타 설정 동일
}

** 개발 환경 제약사항:**
- Windows Docker Desktop (WSL2)에서 `network_mode: host`가 제대로 동작하지 않음
- 개발 환경에서는 IP 화이트리스트 테스트 시 `172.18.0.1`로 표시됨 (정상)
- 실제 IP 테스트는 운영 서버(Oracle Cloud Linux)에서만 가능


**생성된 파일:**
- `backend/src/main/java/com/cryptotrading/entity/ReleaseNote.java`
- `backend/src/main/java/com/cryptotrading/repository/ReleaseNoteRepository.java`
- `backend/src/main/java/com/cryptotrading/service/ReleaseNoteService.java`
- `backend/src/main/java/com/cryptotrading/controller/ReleaseNoteController.java`
- `backend/src/main/java/com/cryptotrading/dto/releasenote/ReleaseNoteDTO.java`
- `backend/src/main/java/com/cryptotrading/dto/releasenote/ReleaseNoteRequest.java`
- `frontend/src/views/ReleaseNotesView.vue`
- `backend/src/main/java/com/cryptotrading/service/TotpService.java` - TOTP 생성/검증 서비스
- `frontend/src/views/AccountSecurityView.vue` - 보안 설정 페이지

**수정된 파일:**
- `docker/mysql/init.sql` - release_notes 테이블 + 샘플 데이터 추가
- `backend/src/main/java/com/cryptotrading/config/SecurityConfig.java` - API 권한 설정
- `frontend/src/router/index.ts` - /release-notes 라우트 추가
- `frontend/src/components/TheSidebar.vue` - 릴리즈 노트 메뉴 추가
- `frontend/src/views/DashboardView.vue` - 최신 공지 표시 + 한줄 개선
- `frontend/src/views/NewsView.vue` - hide-default-footer 추가, 뉴스 수집 API 타임아웃 60초로 증가
- `docker/mysql/init.sql` - users 테이블 allowed_ips 컬럼 추가
- `backend/src/main/java/com/cryptotrading/entity/User.java` - allowedIps 필드 추가
- `backend/src/main/java/com/cryptotrading/dto/user/UserInfoDTO.java` - allowedIps 필드 추가
- `backend/src/main/java/com/cryptotrading/service/UserService.java` - IP 관리 메서드 추가
- `backend/src/main/java/com/cryptotrading/service/AuthService.java` - IP 검증 로직 추가
- `backend/src/main/java/com/cryptotrading/controller/AuthController.java` - 현재 IP 조회 API
- `backend/src/main/java/com/cryptotrading/controller/UserController.java` - IP 관리 API 4개
- `frontend/src/types/index.ts` - IP 관련 타입 추가
- `frontend/src/api/index.ts - IP 관리 API 함수 추가, 로그인/회원가입 API 401 응답 시 원본 에러 유지
- `frontend/src/views/ProfileView.vue - IP 화이트리스트 카드 UI
- `frontend/src/stores/auth.ts - 에러 메시지 추출 로직 개선 (error.detail 우선)
- `docker-compose.yml`** - 개발용 (bridge 네트워크 유지, ports/networks 유지)
- `docker-compose.prod.yml`** - 운영용 (`network_mode: "host"` 추가, ports/networks 제거)
- `frontend/nginx.conf`** - 개발용 (`proxy_pass http://backend:8080` 유지)
- `frontend/nginx.ssl.conf`** - 운영용 (`proxy_pass http://127.0.0.1:8080`으로 변경)
- `backend/src/main/java/com/cryptotrading/entity/User.java` - totpSecret, twoFactorEnabled 필드 추가
- `backend/src/main/java/com/cryptotrading/dto/user/UserInfoDTO.java` - twoFactorEnabled 필드 추가
- `backend/src/main/java/com/cryptotrading/service/UserService.java` - 2FA 설정/해제 메서드 추가
- `backend/src/main/java/com/cryptotrading/controller/UserController.java` - 2FA API 4개 엔드포인트 추가
- `backend/src/main/java/com/cryptotrading/controller/AuthController.java` - 로그인 시 2FA 검증 로직 추가
- `backend/pom.xml` - dev.samstevens.totp 의존성 추가
- `frontend/src/views/LoginView.vue` - OTP 입력 필드 추가
- `frontend/src/stores/auth.ts` - 2FA_REQUIRED 응답 처리 로직 추가
- `frontend/src/api/index.ts` - 401 응답 인터셉터에 2FA_REQUIRED 처리 추가
- `frontend/src/router/index.ts` - /account-security 라우트 추가
- `frontend/src/components/TheSidebar.vue` - 보안 설정 메뉴 추가
- `frontend/package.json` - qrcode 패키지 추가
- `docker/mysql/init.sql` - users 테이블에 totp_secret, two_factor_enabled 컬럼 추가
- `frontend/src/api/index.ts` - 로그인/회원가입 API 401 응답 시 원본 에러 유지
- `frontend/src/stores/auth.ts` - 에러 메시지 추출 로직 개선 (error.detail 우선)

**API 엔드포인트:**
| Method | Endpoint | 권한 | 설명 |
|--------|----------|------|------|
| GET | /api/release-notes | 인증 사용자 | 목록 조회 (페이징 + 검색) |
| GET | /api/release-notes/{id} | 인증 사용자 | 상세 조회 |
| GET | /api/release-notes/latest | 인증 사용자 | 최신 1건 (대시보드용) |
| POST | /api/release-notes | ADMIN | 글 작성 |
| PUT | /api/release-notes/{id} | ADMIN | 글 수정 |
| DELETE | /api/release-notes/{id} | ADMIN | 글 삭제 (soft delete) |
| GET | /api/auth/current-ip | 인증 사용자 | 현재 접속 IP 조회 |
| GET | /api/users/allowed-ips | 인증 사용자 | 등록된 IP 목록 조회 |
| POST | /api/users/allowed-ips | 인증 사용자 | IP 추가 (최대 3개) |
| DELETE | /api/users/allowed-ips | 인증 사용자 | IP 삭제 |
| DELETE | /api/users/allowed-ips/all | 인증 사용자 | 화이트리스트 비활성화 |
| GET | /api/user/2fa/status | 인증 사용자 | 2FA 활성화 상태 조회 |
| POST | /api/user/2fa/setup | 인증 사용자 | 2FA 설정 시작 (QR코드 URL 반환) |
| POST | /api/user/2fa/confirm | 인증 사용자 | 2FA 활성화 확인 (OTP 검증) |
| POST | /api/user/2fa/disable | 인증 사용자 | 2FA 비활성화 (OTP 검증) |

**운영 서버 배포 참고:**
- 기존 운영 DB에는 release_notes 테이블이 없으므로 수동 생성 필요:
```sql
CREATE TABLE IF NOT EXISTS release_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    author_id VARCHAR(50) NOT NULL,
    author_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    INDEX idx_release_notes_created_at (created_at DESC),
    INDEX idx_release_notes_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

** 운영 서버 배포 명령어 (IP 화이트리스트 적용):**
```bash
# SSH 접속
ssh -i ~/.ssh/crypto-key ubuntu@158.179.161.29

cd ~/crypto-trading-system
git pull origin main

# 재배포 (network_mode: host 적용)
docker compose -f docker-compose.prod.yml --env-file .env.production down
docker compose -f docker-compose.prod.yml --env-file .env.production build frontend
docker compose -f docker-compose.prod.yml --env-file .env.production up -d

# 상태 확인
docker ps
docker logs crypto-frontend-prod
```

** IP 화이트리스트 테스트 절차 (운영 서버):**
1. PC에서 로그인
2. 보안 설정 > IP 화이트리스트 이동
3. 현재 IP 확인 (실제 Public IP 표시 확인 - 예: `121.xxx.xxx.xxx`)
4. 현재 IP 추가
5. 모바일 데이터로 접속 (Wi-Fi 끄고)
6. 로그아웃 후 로그인 시도 → "허용되지 않은 IP입니다" 메시지 확인 ✅

**테스트 완료:**
- ✅ 릴리즈 노트 목록 조회 - 브라우저
- ✅ 게시글 작성 (관리자) - 브라우저
- ✅ 게시글 수정 (관리자) - 브라우저
- ✅ 게시글 삭제 (관리자) - 브라우저
- ✅ 페이징 정상 작동 - 브라우저
- ✅ 검색 기능 (제목/작성자/내용) - 브라우저
- ✅ 페이지당 건수 선택 (10/20/50) - 브라우저
- ✅ 작성일 한줄 표시 - 브라우저
- ✅ 대시보드 최신 공지 표시 - 브라우저
- ✅ 운영 서버 배포 완료 - Oracle Cloud
- ✅ 코인 뉴스 하단 중복 컨트롤 제거 - 브라우저
- ✅ 뉴스 수집 API 타임아웃 증가 (60초) - 브라우저
- ✅ IP 화이트리스트 API 9단계 테스트 - Postman
- ✅ 현재 IP 조회 - Postman
- ✅ IP 추가/삭제 - Postman
- ✅ 중복 IP 추가 에러 처리 - Postman
- ✅ 잘못된 IP 형식 에러 처리 - Postman
- ✅ 화이트리스트 비활성화 - Postman
- ✅ 프로필 페이지 IP 화이트리스트 UI - 브라우저
- ✅ 개발 환경 (Windows) Docker 실행 정상 - 브라우저
- ✅ 개발 환경 IP 표시 (172.18.0.1) - 브라우저 (예상대로 Docker IP 표시)
- ✅ 운영 환경 (Oracle Cloud) 실제 IP 테스트 - 브라우저
- ✅ IP 화이트리스트 차단 시 상세 에러 메시지 표시 ("허용되지 않은 IP입니다. 등록된 IP: xxx") - 브라우저
- ✅ 2FA 상태 조회 API - Postman
- ✅ 2FA 설정 시작 (QR코드 URL 생성) - Postman
- ✅ 2FA 활성화 확인 (OTP 검증) - 브라우저
- ✅ 2FA 활성화 후 로그인 시 OTP 입력 필드 표시 - 브라우저
- ✅ 2FA 로그인 OTP 검증 성공 - 브라우저
- ✅ 2FA 비활성화 (OTP 검증) - 브라우저
- ✅ 보안 설정 페이지 UI 렌더링 - 브라우저
- ✅ IP 화이트리스트 차단 시 상세 에러 메시지 표시 - 브라우저

해결한 주요 이슈 (로그인 에러 메시지 표시):

IP 화이트리스트 차단 시 상세 메시지 미표시 문제

문제: IP 차단 시 "로그인에 실패했습니다"만 표시되고 상세 사유 미표시
원인: api/index.ts 인터셉터에서 401 응답을 가공하면서 원본 response 객체 손실
해결: 로그인/회원가입 API는 인터셉터에서 가공하지 않고 원본 에러 그대로 전달
추가 수정: auth.ts에서 error.detail 필드를 우선적으로 표시하도록 변경
결과: "허용되지 않은 IP입니다. 등록된 IP: 118.235.13.3" 상세 메시지 정상 표시

해결한 주요 이슈 (2FA):
1. 2FA 로그인 시 OTP 입력 필드 미표시 문제
   - 문제: 2FA 활성화 후 로그인 시 OTP 입력 필드가 나타나지 않음
   - 원인: api/index.ts 인터셉터에서 401 응답 시 에러 객체 가공 과정에서 `err.response`가 undefined로 전달됨
   - 해결: 401 응답 처리 시 `2FA_REQUIRED` 코드를 감지하여 `err.response.data` 형태로 명시적 전달
   
2. qrcode 패키지 import 오류
   - 문제: `Cannot find module 'qrcode'` 에러 발생
   - 해결: `npm install qrcode @types/qrcode` 패키지 설치

** 해결한 주요 이슈 (IP 화이트리스트 Docker 네트워크):
1. Docker Bridge 네트워크에서 실제 IP 손실 문제
   - 문제: 모든 접속이 `172.18.0.1` (Docker Gateway IP)로 표시
   - 원인: Docker iptables NAT로 인해 클라이언트 원본 IP가 Gateway IP로 변환됨
   - 해결: 운영 환경에서 `network_mode: host` 적용
   
2. Windows Docker Desktop에서 `network_mode: host` 미지원
   - 문제: 개발 환경에서 `network_mode: host` 적용 시 `http://localhost` 접속 불가
   - 원인: Windows Docker Desktop (WSL2)은 `network_mode: host`를 제한적으로 지원
   - 해결: 개발/운영 환경 분리 - 개발은 bridge 유지, 운영만 host 모드 적용

3. docker-compose.yml frontend 들여쓰기 오류
   - 문제: `frontend:` 서비스가 `services:` 블록 바깥에 위치
   - 원인: 들여쓰기 누락으로 YAML 파싱 오류 발생
   - 해결: `frontend:` 앞에 2칸 들여쓰기 추가

DB 마이그레이션 (2FA):
```sql
-- users 테이블에 2FA 컬럼 추가
ALTER TABLE users 
ADD COLUMN totp_secret VARCHAR(255) NULL COMMENT 'TOTP 비밀키 (암호화)',
ADD COLUMN two_factor_enabled BOOLEAN DEFAULT FALSE COMMENT '2FA 활성화 여부';
```

---

### ✅ Day 31 (2026-01-11) - 기간별/코인별 수익 분석 UI 구현
**완료 항목:**
- 기간별/코인별 수익 분석 Backend API 구현
  - ProfitSummaryDTO: 전체 기간별 수익 요약 (오늘/이번달/올해/1년/누적)
  - PeriodProfitDTO: 특정 기간 수익 상세 + 일별 추이 데이터
  - CoinProfitDTO: 코인별 수익 분석 (거래 통계, 승률, 평균가 등)
  - ProfitService: 수익 분석 비즈니스 로직
  - ProfitController: 수익 분석 REST API 3개 엔드포인트
- HoldingsView 2탭 구조 + 하단 보유현황 분리
  - 탭 1: 기간별 수익 분석 (기간 선택 버튼 + 사용자 지정 기간)
  - 탭 2: 코인별 수익 분석 (테이블 + 상세 다이얼로그)
  - 하단: 보유 현황 (기존 기능 유지, 별도 카드로 분리)
- 자산 변동 추이 차트 구현
  - 백테스팅 스타일 SVG 차트
  - 최고/초기/최저 기준선 표시
  - 전체보기/스크롤보기 토글
  - 마우스 호버 시 툴팁
- UI 스타일 개선
  - 탭 색상: 미선택 #B0BEC5, 선택 #546E7A
  - 기간 버튼: 미선택 #B0BEC5, 선택 #FFC107 (노란색)
  - 탭 하단 인디케이터 제거
  - 탭-카드 자연스럽게 연결 (카드 상단 모서리 제거)
- UI 통일성 개선 (추가 작업)**
  - 대시보드: `text-h5` → `text-h4`, `mdi-view-dashboard` 아이콘 추가
  - 보유 자산: `mdi-wallet` 아이콘 추가, 부제목 추가
  - 거래 내역: `mdi-history` 아이콘 추가, 부제목 추가
  - 거래 설정: `v-container fluid` 적용, `mdi-cog-outline` 아이콘 추가, 부제목 추가
  - 프로필 설정: `v-container fluid` 적용, `mdi-account-cog` 아이콘 추가, 부제목 추가
  - 계정 보안: `v-container fluid` 적용, 부제목 추가
  - 모든 페이지 제목 `text-h4` 크기로 통일
  - 기준 페이지(관리자, 릴리즈 노트, 코인 뉴스, 백테스팅, 일일 리포트, 봇 모니터링, 코인 목록)와 동일한 스타일 적용

**API 엔드포인트:**
| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| GET | /api/profit/summary | ✅ | 전체 기간별 수익 요약 |
| GET | /api/profit/by-period | ✅ | 특정 기간 수익 상세 |
| GET | /api/profit/by-coin | ✅ | 코인별 수익 분석 |

**생성된 파일 (Backend: 5개):**
- `dto/profit/ProfitSummaryDTO.java` - 기간별 수익 요약 DTO
- `dto/profit/PeriodProfitDTO.java` - 특정 기간 수익 상세 DTO
- `dto/profit/CoinProfitDTO.java` - 코인별 수익 분석 DTO
- `service/ProfitService.java` - 수익 분석 비즈니스 로직
- `controller/ProfitController.java` - 수익 분석 REST API

**생성된 파일 (Frontend: 1개):**
- `types/profit.ts` - 수익 분석 TypeScript 타입 정의

**수정된 파일:**
- `repository/CoinInfoRepository.java` - findBySymbol() 메서드 추가
- `repository/TransactionRepository.java` - 기간별 조회 쿼리 2개 추가
- `api/index.ts` - profitApi 객체 추가 (3개 API 함수)
- `views/HoldingsView.vue` - 2탭 구조 + 하단 보유현황 분리, UI 전면 재구성
- `config/SecurityConfig.java` - /api/profit/** 인증 설정 추가
- `views/DashboardView.vue` - 제목 `text-h5` → `text-h4`, `mdi-view-dashboard` 아이콘 추가
- `views/HoldingsView.vue` - `mdi-wallet` 아이콘 추가, 부제목 추가
- `views/TransactionHistoryView.vue` - `mdi-history` 아이콘 추가, 부제목 추가
- `views/TradingSettingsView.vue` - `v-container fluid`, `mdi-cog-outline` 아이콘, 부제목 추가
- `views/ProfileView.vue` - `v-container fluid`, `mdi-account-cog` 아이콘, 부제목 추가
- `views/AccountSecurityView.vue` - `v-container fluid`, 부제목 추가

**by-period API period 파라미터:**
| 값 | 설명 |
|------|------|
| today | 오늘 |
| month | 이번달 (1일~오늘) |
| year | 올해 (1월 1일~오늘) |
| oneYear | 최근 1년 |
| total | 전체 누적 |
| custom | 사용자 지정 (startDate, endDate 파라미터 필요) |

**테스트 완료:**
- ✅ 기간별 수익 요약 API - Postman
- ✅ 특정 기간 수익 상세 API (today/month/year/total) - Postman
- ✅ 코인별 수익 분석 API - Postman
- ✅ HoldingsView 2탭 구조 렌더링 - 브라우저
- ✅ 기간별 수익 분석 탭 - 브라우저
- ✅ 기간 선택 버튼 (오늘/이번달/올해/1년/누적) - 브라우저
- ✅ 사용자 지정 기간 조회 - 브라우저
- ✅ 자산 변동 추이 차트 - 브라우저
- ✅ 전체보기/스크롤보기 토글 - 브라우저
- ✅ 코인별 수익 분석 탭 - 브라우저
- ✅ 코인별 상세 다이얼로그 - 브라우저
- ✅ 하단 보유 현황 카드 - 브라우저
- ✅ 탭 스타일 (색상, 인디케이터 제거) - 브라우저
- ✅ UI 통일성 개선 - 대시보드 제목 아이콘 추가 - 브라우저**
- ✅ UI 통일성 개선 - 보유 자산 제목 아이콘/부제목 추가 - 브라우저
- ✅ UI 통일성 개선 - 거래 내역 제목 아이콘/부제목 추가 - 브라우저
- ✅ UI 통일성 개선 - 거래 설정 fluid/아이콘/부제목 추가 - 브라우저
- ✅ UI 통일성 개선 - 프로필 설정 fluid/아이콘/부제목 추가 - 브라우저
- ✅ UI 통일성 개선 - 계정 보안 fluid/부제목 추가 - 브라우저

---

## 📊 현재 진행 상황
- **전체 진척도**: 약 **100%**
- **Phase 1 (핵심 기능)**: 100% 완료 ✅
- **Phase 2 (고도화)**: 100% 완료 ✅
- **Phase 3 (안정화)**: 100% 완료 ✅
- **Phase 4 (운영 준비)**: **100%** 완료 ✅ (기간별/코인별 수익 분석 UI 완료)

---

## 🗓️ 프로젝트 일정

| 구분 | 기간 | 상태 |
|------|------|------|
| Phase 1 (핵심 기능) | Day 1~9 | ✅ 완료 |
| Phase 2 (고도화) | Day 10~16 | ✅ 완료 |
| Phase 3 (안정화) | Day 17~21 | ✅ 완료 |
| Phase 4 (운영 준비) | Day 22~31 | ✅ 완료 |
| v1.0 릴리즈 | Day 32 | 🎯 목표 |

---

## 🎯 다음 단계 (Day 21~32)

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
| - | HTTPS 실제 적용 | Let's Encrypt 인증서 발급 | ⏳ Day 29로 이동 |

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

#### ✅ Day 25: AI 뉴스 분석 (2) - AI 연동 + 최적화 (2025-12-29 완료)
| 시간 | 작업 | 상세 | 상태 |
|------|------|------|------|
| 오전 | AI API 연동 | Gemini → **Groq API로 전환** (Rate Limit 해결) | ✅ 완료 |
| 오전 | 프롬프트 설계 | 호재/악재 분류, 점수화 (-1.0~+1.0) | ✅ 완료 |
| 오후 | 뉴스 분석 서비스 | NewsAnalysisService 구현 | ✅ 완료 |
| 오후 | 점수 → 가중치 변환 | 평균 점수 계산, ±0.5% 매핑 로직 | ✅ 완료 |
| 오후 | **벌크 분석 최적화** | N건 → 1회 API 호출 | ✅ 완료 |
| 오후 | **캐싱 최적화** | 분석 완료 플래그로 중복 방지 | ✅ 완료 |

---

#### ✅ Day 26: AI 뉴스 분석 (3) - 지표 연동 + 알림
| 시간 | 작업 | 상세 |
|------|------|------|
| 오전 | SignalDetector 연동 | AI 가중치 buyThresholdPct 반영 | ✅ 완료 |
| 오전 | 일일 초기화 스케줄러 | 매일 00:00 KST 가중치 리셋 | ✅ 완료 |
| 오후 | 3시간 분석 스케줄러 | 뉴스 수집 → AI 분석 → 가중치 적용 | ✅ 완료 |
| 오후 | 가중치 변경 알림 | 이메일/Discord DM 상세 내용 발송 | ✅ 완료 |
| 오후 | 뉴스 데이터 정리 스케줄러 | 매일 04:00 KST 7일 초과 데이터 삭제 | ✅ 완료 |
| 오후 | 코인 뉴스 페이지 UI | 게시판 형식 뉴스 조회 (Frontend) | ✅ 완료 |
---

#### ✅ Day 27: Oracle Cloud ARM64 배포 (2025-12-30 완료)
| 시간 | 작업 | 상세 | 상태 |
|------|------|------|------|
| 오전 | Oracle Cloud 인스턴스 생성 | ARM64 4 OCPU, 24GB RAM | ✅ 완료 |
| 오전 | 서버 초기 설정 | Docker, 방화벽, 보안 목록 | ✅ 완료 |
| 오후 | Docker 이미지 ARM64 호환 수정 | Dockerfile alpine 제거 | ✅ 완료 |
| 오후 | MySQL/환경변수/BCrypt 문제 해결 | 스키마 수정, 해시 재생성 | ✅ 완료 |
| 오후 | 배포 및 테스트 | 전체 서비스 정상 작동 확인 | ✅ 완료 |

---

#### ✅ Day 28: 대시보드 재구성 및 코인 목록 페이지 (2025-12-31 완료)
| 시간 | 작업 | 상세 | 상태 |
|------|------|------|------|
| 오전 | 대시보드 전면 재구성 | 10개 패널 구성, 실시간 시간, 매수조건 개선 | ✅ 완료 |
| 오전 | 코인 목록 페이지 생성 | CoinListView.vue 신규, 벌크 API 적용 | ✅ 완료 |
| 오후 | Backend 수정 | bulk API 파싱, MATIC 비활성화 | ✅ 완료 |
| 오후 | UI/UX 최종 개선 | 매수조건 현재가→목표가 표시, 이격도 계산 | ✅ 완료 |
| 오후 | 백테스팅 버그 수정 | 과거 데이터 조회 로직 수정 (endDate 기준) | ✅ 완료 |
| 오후 | 백테스팅 전략 검증 | 5개 시장 구간별 테스트 실행 | ✅ 완료 |

---

#### Day 29: 급락장 보호 기능 + **HTTPS 적용**
| 시간 | 작업 | 상세 | 상태 |
|------|------|------|------|
| 오전 | 급락장 보호 기능 3종 구현 | 시장 추세 필터, 누적 손실 긴급정지, 연속 손절 제한 | ✅ 완료 |
| 오후 | DB 스키마 확장 | trading_settings 테이블 3개 컬럼 추가 | ✅ 완료 |
| 오후 | 백테스팅 보호 기능 연동 | BacktestService 급락장 보호 로직 구현 | ✅ 완료 |
| 오후 | 거래 설정 UI 확장 | 급락장 보호 설정 UI 추가 | ✅ 완료 |
| 오후 | 백테스팅 UI 확장 | 급락장 보호 설정 UI 추가 | ✅ 완료 |
| 오후 | 파라미터 최적화 테스트 | 5가지 시장 상황 × 6가지 설정 종합 비교 | ✅ 완료 |
| 오후 | 최적 기본값 적용 | 시장필터 OFF, 누적손실 -10%, 연속손절 3회 | ✅ 완료 |
| 오후 | HTTPS 적용 | DuckDNS 무료 도메인 + Let's Encrypt SSL | ✅ 완료 |
| 오후 | CORS 설정 수정 | 운영 도메인 허용 추가 | ✅ 완료 |
| 오후 | SSL 자동 갱신 | renew-ssl.sh + Cron 등록 | ✅ 완료 |

★★★ 급락장 보호 기능 상세 (Day 29 구현 완료) ★★★

| 기능 | 설명 | 기본값 | 적용 대상 | 상태 |
|------|------|--------|----------|------|
| 시장 추세 필터 | BTC가 20일 이동평균선 아래면 전체 매수 중단 | **OFF** | 자동매매, 백테스팅 | ✅ 완료 |
| 누적 손실률 긴급정지 | 초기 자본 대비 누적 손실 도달 시 거래 중단 | **-10%** | 자동매매, 백테스팅 | ✅ 완료 |
| 연속 손절 제한 | 동일 코인 연속 손절 시 해당 코인 24시간 매수 금지 | **3회** | 자동매매, 백테스팅 | ✅ 완료 |

**최적 기본값 선정 근거 (백테스팅 검증):**
- 시장필터 OFF: 상승장/횡보장 수익 100% 유지 (시장필터 ON은 76% 손실)
- 누적손실 -10%: 급락장 손실 55% 감소 (-41% → -19%)
- 연속손절 3회: 과매매 방지 + 상승장 수익 유지

---

#### ✅ Day 30: 릴리즈 노트 게시판 + IP 화이트리스트 + 기능 개선 (2026-01-08 완료)
| 시간 | 작업 | 상세 | 상태 |
|------|------|------|------|
| 오전 | 릴리즈 노트 게시판 | 공지사항/업데이트 이력 게시판 UI + API | ✅ 완료 |
| 오전 | DB 스키마 추가 | release_notes 테이블 생성 | ✅ 완료 |
| 오전 | Backend API 구현 | Entity, Repository, Service, Controller | ✅ 완료 |
| 오전 | Frontend 페이지 구현 | ReleaseNotesView.vue + 라우팅 + 사이드바 | ✅ 완료 |
| 오전 | 대시보드 연동 | 시스템 알림 카드에 최신 공지 표시 | ✅ 완료 |
| 오후 | 게시판 기능 개선 | 페이지당 건수 선택, 검색 기능, 글번호 순번화 | ✅ 완료 |
| 오후 | 작성일 표시 개선 | 2줄 → 1줄 표시 (YYYY-MM-DD HH:mm) | ✅ 완료 |
| 오후 | 대시보드 공지 개선 | 한줄 간결한 표시로 변경 | ✅ 완료 |
| 오후 | 코인 뉴스 페이지 개선 | 하단 중복 컨트롤 제거 | ✅ 완료 |
| 오후 | IP 화이트리스트 | 접속 IP 제한 기능 (최대 3개) | ✅ 완료 |
| 오후 | Backend API 구현 | User 엔티티 확장, 5개 API 엔드포인트 | ✅ 완료 |
| 오후 | Frontend UI 구현 | ProfileView.vue IP 화이트리스트 카드 | ✅ 완료 |
| 오후 | 2FA 인증 | Google Authenticator 연동 | ✅ 완료 |

---

#### ✅ Day 31: 기간별/코인별 수익 분석 UI 구현 (2026-01-11 완료)
| 시간 | 작업 | 상세 | 상태 |
|------|------|------|------|
| 오전 | 기간별/코인별 수익 분석 | HoldingsView 2탭 + 하단 보유현황 구조 | ✅ 완료 |
| 오전 | ├ Backend API | 기간별 수익 집계 엔드포인트 (3개) | ✅ 완료 |
| 오전 | ├ Frontend 탭1 | 기간별 수익 분석 (오늘/이번달/올해/1년/누적 + 사용자지정) | ✅ 완료 |
| 오전 | ├ Frontend 탭2 | 코인별 수익 분석 (테이블 + 상세 다이얼로그) | ✅ 완료 |
| 오전 | └ 하단 보유현황 | 보유 현황 (기존 유지, 별도 카드 분리) | ✅ 완료 |
| 오후 | 자산 변동 추이 차트 | 백테스팅 스타일 SVG 차트 + 전체/스크롤 보기 | ✅ 완료 |
| 오후 | UI 스타일 개선 | 탭 색상, 인디케이터 제거, 카드 연결 | ✅ 완료 |
| 오후 | UI 통일성 개선 | 6개 페이지 제목 아이콘/부제목/fluid 통일 | ✅ 완료 |

---

#### Day 32: 최종 보안 점검 + 운영 문서 + v1.0 릴리즈
| 시간 | 작업 | 상세 | 상태 |
|------|------|------|------|
| 오전 | 최종 보안 점검 | OWASP Top 10 체크리스트 | |
| 오전 | 전체 시스템 테스트 | 통합 테스트, 시나리오 테스트 | |
| 오후 | 운영 문서 작성 | 아키텍처 다이어그램, 배포 절차서 | |
| 오후 | 장애 대응 매뉴얼 | 오류 해결, 재시작 절차, 백업/복원 | |
| 오후 | README 최종 업데이트 | 완료 항목 정리 | |
| 오후 | v1.0 릴리즈 | Git 태깅, 최종 배포 | |

---

### 릴리즈 노트 게시판 상세 (Day 30 신규)

#### 개요
프로젝트 진행 이력 및 업데이트 내용을 사용자에게 공지하는 게시판

#### 기능 요구사항

| 구분 | 기능 | 설명 |
|------|------|------|
| **조회** | 목록 조회 | 글번호, 제목, 작성일자, 작성자 표시 |
| **조회** | 상세 조회 | 게시글 본문 내용 열람 |
| **조회** | 페이징 | 페이지당 10/20건 선택 |
| **관리자** | 글 작성 | 제목, 본문 입력 (관리자만) |
| **관리자** | 글 수정 | 기존 게시글 수정 (관리자만) |
| **관리자** | 글 삭제 | 게시글 삭제 (관리자만) |
| **일반 사용자** | 열람만 가능 | 작성/수정/삭제 불가 |

#### 대시보드 연동
- 대시보드 상단 **시스템 알림**에 최신 게시글 한 줄 표시
- 클릭 시 해당 게시글 상세 페이지로 이동

#### DB 테이블 설계
```sql
CREATE TABLE release_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    author_id VARCHAR(50) NOT NULL,
    author_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    INDEX idx_created_at (created_at DESC)
);
```

#### API 엔드포인트
| Method | Endpoint | 권한 | 설명 |
|--------|----------|------|------|
| GET | /api/release-notes | 모든 사용자 | 목록 조회 (페이징) |
| GET | /api/release-notes/{id} | 모든 사용자 | 상세 조회 |
| GET | /api/release-notes/latest | 모든 사용자 | 최신 1건 조회 (대시보드용) |
| POST | /api/release-notes | ADMIN만 | 글 작성 |
| PUT | /api/release-notes/{id} | ADMIN만 | 글 수정 |
| DELETE | /api/release-notes/{id} | ADMIN만 | 글 삭제 (soft delete) |

#### 프론트엔드 페이지
- `ReleaseNotesView.vue` - 게시판 목록/상세 페이지
- `DashboardView.vue` - 공지 카드에 최신 글 표시

#### 게시글 예시 내용
README.md의 일별 작업 내용을 게시글로 작성:
```
[제목] v1.0 Day 29 업데이트 - 급락장 보호 기능 + HTTPS 적용
[본문]
■ 급락장 보호 기능 3종 구현
  - 시장 추세 필터 (BTC 20일선 기준)
  - 누적 손실률 긴급정지 (-10%)
  - 연속 손절 제한 (3회)

■ HTTPS 적용
  - DuckDNS 무료 도메인 연동
  - Let's Encrypt SSL 인증서 발급
  - 자동 갱신 설정 완료

### IP 화이트리스트 상세 (Day 30 신규)

#### 개요
사용자가 지정한 IP 주소에서만 로그인을 허용하는 보안 기능

#### 기능 요구사항

| 구분 | 기능 | 설명 |
|------|------|------|
| **조회** | 현재 IP 조회 | 접속 중인 클라이언트 IP 표시 |
| **조회** | IP 목록 조회 | 등록된 화이트리스트 IP 목록 |
| **등록** | IP 추가 | 최대 3개까지 IP 등록 가능 |
| **등록** | 현재 IP 추가 | 버튼 클릭으로 현재 접속 IP 자동 등록 |
| **삭제** | IP 삭제 | 개별 IP 삭제 |
| **비활성화** | 전체 삭제 | 화이트리스트 비활성화 (모든 IP 허용) |

#### 보안 동작
- **활성화 상태** (1개 이상 IP 등록): 등록된 IP에서만 로그인 가능
- **비활성화 상태** (IP 목록 비어있음): 모든 IP에서 로그인 가능
- **검증 시점**: 로그인 요청 시 IP 검증

#### DB 스키마 변경
```sql
-- users 테이블에 컬럼 추가
ALTER TABLE users ADD COLUMN allowed_ips JSON DEFAULT NULL;

-- 예시 데이터
UPDATE users SET allowed_ips = '["192.168.1.100", "10.0.0.50"]' WHERE user_id = 'admin';
```

#### API 엔드포인트
| Method | Endpoint | 권한 | 설명 |
|--------|----------|------|------|
| GET | /api/auth/current-ip | 인증 사용자 | 현재 접속 IP 조회 |
| GET | /api/users/allowed-ips | 인증 사용자 | 등록된 IP 목록 |
| POST | /api/users/allowed-ips | 인증 사용자 | IP 추가 |
| DELETE | /api/users/allowed-ips | 인증 사용자 | IP 삭제 |
| DELETE | /api/users/allowed-ips/all | 인증 사용자 | 화이트리스트 비활성화 |

#### 프론트엔드 UI
- `ProfileView.vue` - 프로필 설정 페이지 내 IP 화이트리스트 카드
- 현재 IP 표시, IP 입력 필드, 추가/삭제 버튼, 비활성화 버튼
```

### 기간별/코인별 수익 분석 상세 (Day 31 완료)

#### 개요
보유자산 페이지(HoldingsView)를 2탭 + 하단 보유현황 구조로 개선하여 기간별/코인별 수익 분석 기능 제공

#### 구현 결과
- **구조**: 2탭 (기간별/코인별) + 하단 보유현황 분리
- **선정 이유**:
  1. 탭 전환과 보유현황 조회의 독립성 확보
  2. 사용자 동선 개선 (수익 분석 ↔ 보유 현황 동시 확인 가능)
  3. UI/UX 명확한 구분

#### HoldingsView 구조 (완료)
```
보유 자산 페이지
├── [탭 1] 기간별 수익 분석
│   ├── 기간 선택 버튼: 오늘 | 이번달 | 올해 | 1년 | 누적
│   ├── 사용자 지정 기간: 시작일 ~ 종료일 입력
│   ├── 수익 요약 카드
│   │   ├── 선택 기간 수익금액 (큰 글씨)
│   │   ├── 수익률 % 칩
│   │   ├── 거래 건수, 익절/손절, 승률
│   │   ├── 건당 평균 수익, 최대 수익/손실
│   │   └── 조회 기간 표시
│   └── 자산 변동 추이 차트
│       ├── 백테스팅 스타일 SVG 차트
│       ├── 최고(녹색)/초기(주황)/최저(빨간) 기준선
│       ├── 전체보기/스크롤보기 토글
│       └── 마우스 호버 시 툴팁
│
├── [탭 2] 코인별 수익 분석
│   ├── 코인별 수익 테이블
│   │   ├── 코인 심볼/이름
│   │   ├── 실현 수익 (금액)
│   │   ├── 수익률 (%) 칩
│   │   ├── 거래 건수
│   │   ├── 익절/손절 (승률)
│   │   ├── 보유 현황 칩
│   │   └── 상세 버튼
│   └── 코인별 상세 다이얼로그
│       ├── 총 실현 수익, 수익률, 총 거래 건수
│       ├── 익절/손절 건수, 승률, 현재 보유
│       ├── 총 매수/매도 금액, 평균 매수가/매도가
│       ├── 최대 수익/손실 거래
│       └── 마지막 거래 시간
│
└── [하단] 보유 현황 카드
    ├── 통계 카드 4개 (총 투자금액, 현재 평가액, 평가 손익, 수익률)
    └── 보유 자산 테이블 (코인별 수량, 매수가, 현재가, 평가손익)
```

#### Backend API 엔드포인트 (완료)
| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| GET | /api/profit/summary | ✅ | 전체 기간별 수익 요약 (오늘/이번달/올해/1년/누적) |
| GET | /api/profit/by-period?period={period} | ✅ | 특정 기간 수익 상세 + 일별 추이 |
| GET | /api/profit/by-period?period=custom&startDate={}&endDate={} | ✅ | 사용자 지정 기간 |
| GET | /api/profit/by-coin | ✅ | 코인별 수익 집계 |

#### 기간별 수익 요약 API 응답 예시
```json
{
  "success": true,
  "data": {
    "todayProfit": 0,
    "todayProfitPct": 0.0,
    "todayTradeCount": 0,
    "monthProfit": 12980,
    "monthProfitPct": 1.3,
    "monthTradeCount": 2,
    "yearProfit": 12980,
    "yearProfitPct": 1.3,
    "yearTradeCount": 2,
    "oneYearProfit": 12980,
    "oneYearProfitPct": 1.3,
    "oneYearTradeCount": 2,
    "totalProfit": 12980,
    "totalProfitPct": 1.3,
    "totalTradeCount": 2,
    "initialInvestment": 1000000
  }
}
```

#### 특정 기간 수익 API 응답 예시
```json
{
  "success": true,
  "data": {
    "period": "전체",
    "startDate": "2025-11-24",
    "endDate": "2026-01-11",
    "totalProfit": 12980,
    "profitPct": 1.3,
    "tradeCount": 2,
    "winCount": 2,
    "loseCount": 0,
    "winRate": 100.0,
    "avgProfit": 6490,
    "maxProfit": 10980,
    "maxLoss": 2000,
    "dailyProfits": [
      {"date": "2025-11-24", "profit": 2000, "cumulativeProfit": 2000, "tradeCount": 1},
      {"date": "2025-12-02", "profit": 10980, "cumulativeProfit": 12980, "tradeCount": 1}
    ]
  }
}
```

#### 코인별 수익 API 응답 예시
```json
{
  "success": true,
  "data": [
    {
      "coinSymbol": "KRW-ETH",
      "coinName": "이더리움",
      "totalProfit": 10980,
      "profitPct": 36.6,
      "totalTradeCount": 1,
      "winCount": 1,
      "loseCount": 0,
      "winRate": 100.0,
      "totalBuyAmount": 30000,
      "totalSellAmount": 40980,
      "avgBuyPrice": 3000000,
      "avgSellPrice": 4098000,
      "maxProfit": 10980,
      "maxLoss": 10980,
      "currentHoldingCount": 0,
      "currentHoldingAmount": 0,
      "unrealizedProfit": 0,
      "lastTradeAt": "2025-12-02T09:46:10"
    }
  ]
}
```

#### 프론트엔드 수정 파일
| 파일 | 변경 내용 |
|------|----------|
| `HoldingsView.vue` | 2탭 + 하단 보유현황 구조로 전면 개편, 자산 변동 차트 추가 |
| `api/index.ts` | profitApi 객체 추가 (3개 API 함수) |
| `types/profit.ts` | ProfitSummary, PeriodProfit, CoinProfit, DailyProfit 타입 추가 |

#### 기간 계산 기준 (KST)
| 기간 | 시작일 | 종료일 |
|------|--------|--------|
| 오늘 (today) | 오늘 00:00:00 | 현재 시간 |
| 이번달 (month) | 이번달 1일 00:00:00 | 현재 시간 |
| 올해 (year) | 올해 1월 1일 00:00:00 | 현재 시간 |
| 1년 (oneYear) | 1년 전 오늘 | 현재 시간 |
| 누적 (total) | 첫 거래일 | 현재 시간 |
| 사용자 지정 (custom) | startDate 파라미터 | endDate 파라미터 |

#### UI 스타일 정리
| 요소 | 스타일 |
|------|--------|
| 탭 (미선택) | 배경색 #B0BEC5 (옅은 회색) |
| 탭 (선택) | 배경색 #546E7A (진한 회색) |
| 기간 버튼 (미선택) | 배경색 #B0BEC5 |
| 기간 버튼 (선택) | 배경색 #FFC107 (노란색) |
| 탭 인디케이터 | 제거 (display: none) |
| 카드 상단 모서리 | 제거 (탭과 자연스럽게 연결) |

---


### 📊 일정 요약

| Day | 주요 작업 | 카테고리 |
|-----|----------|----------|
| 21 | ✅ DB 백업, 로그 로테이션, 헬스체크, 시간대 설정 | ✅ 완료 |
| 22 | ✅ 환경변수 보안, API 재시도, 운영 Docker Compose | ✅ 완료 |
| 23 | ✅ 모니터링 대시보드, 슬로우 쿼리, 서버 알림 | ✅ 완료 |
| 24 | ✅ AI 뉴스 분석 - 기반 구축 | ✅ 완료 |
| 25 | ✅ AI 뉴스 분석 - Groq API 연동 + 최적화 | ✅ 완료 |
| 26 | ✅ AI 뉴스 분석 - 지표 연동 + 뉴스 페이지 UI | ✅ 완료 |
| 27 | ✅ Oracle Cloud ARM64 배포, DB 스키마 교차검증, docker-compose 동기화, 이슈 해결 | ✅ 완료 |
| 28 | ✅ 대시보드 재구성, 코인 목록 페이지, bulk API 수정, MATIC 비활성화, 시간대 KST 통일, AdminDashboard 오류 수정, 시간 표시 함수 개선 | ✅ 완료 |
| 29 | ✅ 급락장 보호 기능 3종, HTTPS 적용 (DuckDNS + Let's Encrypt), CORS 수정, SSL 자동 갱신 | **✅ 완료** |
| 30 | ✅ 릴리즈 노트 게시판 (CRUD + 검색 + 페이징), IP 화이트리스트, 2FA 인증, 대시보드 연동, 뉴스 페이지 개선 | ✅ 완료 |
| 31 | ✅ HoldingsView 2탭+보유현황 (수익 분석), 자산 변동 차트, UI 스타일 개선, UI 통일성 개선 (6개 페이지) | ✅ 완료 |
| 32 | 최종 보안 점검, 전체 시스템 테스트, 운영 문서 작성, 장애 대응 매뉴얼, v1.0 릴리즈 | 🔴 필수 |

---

### 🤖 AI 뉴스 분석 기능 상세

#### 개요
- **실행 주기**: 3시간마다 (스케줄러)
- **AI API**: ~~Google Gemini API~~ → **Groq API (Llama 3.3 70B)** - 무료, Rate Limit 우수
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
**⭐ 연속적인 값 방식** (-0.5% ~ +0.5% 범위 내 연속적 계산)

#### 지표 조정 기준 (연속적인 값 방식)

AI가 각 뉴스를 분석하여 **-1.0 ~ +1.0** 사이의 점수를 매기고, 평균 점수에 0.5를 곱하여 **-0.5% ~ +0.5%** 범위의 가중치로 변환합니다.

**📌 계산 공식**
```
가중치(%) = 평균 점수 × 0.5
```

**📌 0.5를 곱하는 이유**
- 프로젝트 지침: "buyThresholdPct ±0.5% 조정"
- AI 점수 범위(-1.0 ~ +1.0)를 가중치 범위(-0.5% ~ +0.5%)로 변환
- 뉴스로 인한 매수 조건 변동을 적절한 수준으로 제한

**📌 AI 점수 기준**
| 점수 범위 | 의미 | 예시 뉴스 |
|----------|------|----------|
| +0.7 ~ +1.0 | 매우 호재 | "비트코인 ETF 순유입 역대 최고" |
| +0.3 ~ +0.6 | 약한 호재 | "기관 투자자 매수세 증가" |
| -0.2 ~ +0.2 | 중립 | "비트코인 가격 횡보 지속" |
| -0.6 ~ -0.3 | 약한 악재 | "규제 강화 논의 시작" |
| -1.0 ~ -0.7 | 매우 악재 | "대형 거래소 해킹 사고" |

**📌 감성(Sentiment) 판단 기준**
| 평균 점수 | 감성 | 의미 |
|----------|------|------|
| > +0.2 | POSITIVE | 호재 - 매수 조건 완화 |
| -0.2 ~ +0.2 | NEUTRAL | 중립 - 변동 미미 |
| < -0.2 | NEGATIVE | 악재 - 매수 조건 강화 |

#### 가중치 적용 예시

**📈 예시 1: 호재 뉴스가 많은 날**
```
오늘 BTC 뉴스 3건:
- "비트코인 ETF 승인" → AI 점수: +0.90
- "대형 기관 매수" → AI 점수: +0.80  
- "긍정적 규제 발표" → AI 점수: +0.70

평균 점수: (0.90 + 0.80 + 0.70) ÷ 3 = +0.80
가중치: 0.80 × 0.5 = +0.40%

사용자 설정: buyThresholdPct = -5% (5% 하락 시 매수)
→ 조정 후: -5% + 0.40% = -4.60% (4.6% 하락 시 매수)

효과: 좋은 뉴스가 많으니, 조금만 떨어져도 매수
```

**📉 예시 2: 악재 뉴스가 많은 날**
```
오늘 BTC 뉴스 3건:
- "대형 거래소 해킹" → AI 점수: -0.85
- "각국 규제 강화" → AI 점수: -0.70
- "고래 대량 매도" → AI 점수: -0.65

평균 점수: (-0.85 + -0.70 + -0.65) ÷ 3 = -0.73
가중치: -0.73 × 0.5 = -0.37%

사용자 설정: buyThresholdPct = -5%
→ 조정 후: -5% + (-0.37%) = -5.37% (5.37% 하락 시 매수)

효과: 나쁜 뉴스가 많으니, 더 많이 떨어져야 매수
```

**📊 예시 3: 호재/악재 혼재 시**
```
오늘 BTC 뉴스 3건:
- "ETF 순유입 증가" → AI 점수: +0.70
- "일부 국가 규제 강화" → AI 점수: -0.30
- "기관 관심 증가" → AI 점수: +0.20

평균 점수: (0.70 + -0.30 + 0.20) ÷ 3 = +0.20
가중치: 0.20 × 0.5 = +0.10%

사용자 설정: buyThresholdPct = -5%
→ 조정 후: -5% + 0.10% = -4.90%

효과: 호재/악재가 섞여 있어 거의 변동 없음
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

#### Groq API 무료 한도 (Gemini 대체)
- 분당 30회 요청 (Gemini의 2배)
- 일일 14,400회 요청 (Gemini의 10배)
- 3시간마다 5코인 분석 시: 월 ~1,200회 (한도의 0.3%)
- **벌크 분석 최적화**: N건 뉴스 → 1회 API 호출
- **캐싱 최적화**: 이미 분석된 뉴스는 API 미호출

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
| HTTPS 적용 | Let's Encrypt SSL 인증서 설정 | 🔴 필수 | ✅ Day 29 완료 |
| Nginx SSL 설정 | HTTPS 리다이렉트, HSTS 헤더 | 🔴 필수 | ✅ Day 29 완료 |
| 환경변수 보안 | 개발/운영 환경 분리 | 🟡 권장 | ✅ Day 22 완료 ||

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
| AI 뉴스 분석 | Groq API (무료), 뉴스 수집, 지표 연동 | 🟢 선택 | ✅ Day 24-26 완료 |
| WebSocket 실시간 모니터링 | 실시간 가격/거래 업데이트 | 🟢 선택 | 미구현 |
| 2FA 인증 | Google Authenticator 연동 | 🟢 선택 | ✅ Day 30 완료 |
| IP 화이트리스트 | 접속 IP 제한 기능 | 🟢 선택 | ✅ Day 30 완료 |
| 기간별/코인별 수익 분석 | HoldingsView 2탭+보유현황 구조 | 🔴 필수 | ✅ Day 31 완료 |

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
| useMarketTrendFilter | FALSE | 시장 추세 필터 (권장: OFF) |
| cumulativeLossLimitPct | -10% | 누적 손실 한도 (최적화 결과) |
| consecutiveStopLossLimit | 3회 | 연속 손절 제한 (최적화 결과) |

---

## 📊 현재 시스템 완성도

| 항목 | 상태 | 비고 |
|------|------|------|
| 실투자 가능 여부 | ✅ 가능 | 업비트 API 연동 완료 |
| 보안 수준 | ✅ 양호 | HTTPS 적용 완료, 2FA 지원 |
| 백테스팅 ↔ 실거래 일치 | ✅ 100% | 모든 지표 동일 적용 |
| 운영 안정성 | ✅ 양호 | 자동 재시작, 헬스체크 적용 |
| DB 백업 | ✅ 완료 | 일일 백업, 7일 보관 |
| 로그 관리 | ✅ 완료 | 일별 로테이션, 분리 저장 |
| **종합 완성도** | **99%** | 운영 문서 최종 정리만 남음 |

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
│   ├── renew-ssl.sh              # ⭐ Day 29: SSL 인증서 자동 갱신
│   ├── setup-cron.sh             # ⭐ Day 29: Cron 설정 스크립트
│   ├── archive-transactions.ps1  # ⭐ Day 23: Windows 거래 아카이빙
│   └── archive-transactions.sh   # ⭐ Day 23: Linux 거래 아카이빙
├── backups/                      # 백업 저장소
│   └── mysql/                    # MySQL 백업 파일
├── .env.example                  # 환경변수 템플릿
├── .env.development              # 개발 환경 설정 (Git 제외) ⭐ Day 25: GROQ_API_KEY 추가
├── .env.production               # 운영 환경 설정 (Git 제외)
├── docker-compose.yml            # 개발용 Docker Compose ⭐ Day 25: GROQ 환경변수 추가
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
│   │   │   ├── NewsController.java		# ⭐ Day 24: 뉴스 API
│   │   │   ├── ReleaseNoteController.java	# ⭐ Day 30: 릴리즈 노트 API
│   │   │   └── ProfitController.java		# ⭐ Day 31: 수익 분석 API
│   │   ├── service/              # 비즈니스 로직
│   │   │   ├── AuthService.java
│   │   │   ├── UserService.java
│   │   │   ├── CoinInfoService.java
│   │   │   ├── TradingSettingService.java
│   │   │   ├── TransactionService.java
│   │   │   ├── UpbitApiService.java
│   │   │   ├── CacheService.java
│   │   │   ├── TechnicalIndicatorService.java
│   │   │   ├── SignalDetectorService.java
│   │   │   ├── RiskManagementService.java      
│   │   │   ├── TradingBotService.java          
│   │   │   ├── NotificationService.java      
│   │   │   ├── DailyReportService.java         
│   │   │   ├── DiscordBotService.java          
│   │   │   ├── EmailService.java              
│   │   │   ├── BacktestService.java            
│   │   │   ├── AdminService.java               
│   │   │   ├── MonitoringService.java          # ⭐ Day 23: 시스템 모니터링
│   │   │   ├── MonitoringAlertService.java     # ⭐ Day 23: 이상징후 알림
│   │   │   ├── AdminAlertNotificationService.java  # ⭐ Day 23: Admin 알림
│   │   │   ├── NewsCollectorService.java       # ⭐ Day 24: 뉴스 수집
│   │   │   ├── NewsAnalysisService.java        # ⭐ Day 25: AI 분석
│   │   │   ├── GeminiApiService.java	# ⭐ Day 25: Groq API 연동
│   │   │   ├── ReleaseNoteService.java	# ⭐ Day 30: 릴리즈 노트 서비스
│   │   │   ├── TotpService.java		# ⭐ Day 30: TOTP 생성/검증
│   │   │   └── ProfitService.java		# ⭐ Day 31: 수익 분석 서비스
│   │   ├── repository/           # 데이터 접근
│   │   │   ├── UserRepository.java
│   │   │   ├── TradingSettingRepository.java
│   │   │   ├── TransactionRepository.java
│   │   │   ├── CoinInfoRepository.java
│   │   │   ├── DailySummaryRepository.java
│   │   │   ├── SystemLogRepository.java
│   │   │   ├── CoinNewsRepository.java           # ⭐ Day 24-25: 뉴스 Repository
│   │   │   ├── CoinNewsAnalysisRepository.java   # ⭐ Day 24: 분석 Repository
│   │   │   └── ReleaseNoteRepository.java        # ⭐ Day 30: 릴리즈 노트 Repository
│   │   ├── entity/               # JPA 엔티티
│   │   │   ├── User.java
│   │   │   ├── TradingSetting.java
│   │   │   ├── Transaction.java
│   │   │   ├── CoinInfo.java
│   │   │   ├── DailySummary.java
│   │   │   ├── SystemLog.java
│   │   │   ├── PasswordResetToken.java
│   │   │   ├── CoinNews.java                     # ⭐ Day 24-25: 뉴스 엔티티
│   │   │   ├── CoinNewsAnalysis.java             # ⭐ Day 24: 분석 결과 엔티티
│   │   │   └── ReleaseNote.java                  # ⭐ Day 30: 릴리즈 노트 엔티티
│   │   ├── dto/                  # 데이터 전송 객체
│   │   │   ├── common/           # 공통 DTO
│   │   │   │   ├── ApiResponse.java
│   │   │   │   └── PageResponse.java
│   │   │   ├── indicator/        # 기술적 지표 DTO
│   │   │   ├── bot/              # 봇 관련 DTO
│   │   │   ├── upbit/            # 업비트 API DTO
│   │   │   ├── notification/     # 알림 DTO
│   │   │   ├── TradingSettingDTO.java      # ⭐ Day 29: 급락장 보호 필드 추가
│   │   │   ├── backtest/         # 백테스트 DTO
│   │   │   │   └── BacktestRequestDTO.java # ⭐ Day 29: 급락장 보호 필드 추가
│   │   │   ├── gemini/           # Gemini/Groq API DTO
│   │   │   │   ├── GeminiRequestDTO.java
│   │   │   │   └── GeminiResponseDTO.java
│   │   │   ├── admin/            # 관리자 DTO
│   │   │   │   ├── SystemStatsDTO.java
│   │   │   │   ├── AdminUserDTO.java
│   │   │   │   └── MonitoringDTO.java            # ⭐ Day 23: 모니터링 DTO
│   │   │   ├── news/             # ⭐ Day 24-25: 뉴스 DTO
│   │   │   │   ├── CoinNewsDTO.java
│   │   │   │   ├── CoinNewsAnalysisDTO.java
│   │   │   │   ├── RssNewsItem.java
│   │   │   │   └── NewsAnalysisResultDTO.java
│   │   │   └── releasenote/      # ⭐ Day 30: 릴리즈 노트 DTO
│   │   │   │ ├── ReleaseNoteDTO.java
│   │   │   │ └── ReleaseNoteRequest.java
│   │   │   └── profit/           # ⭐ Day 31: 수익 분석 DTO
│   │   │       ├── ProfitSummaryDTO.java
│   │   │       ├── PeriodProfitDTO.java
│   │   │       └── CoinProfitDTO.java
│   │   ├── config/               # 설정
│   │   │   ├── SecurityConfig.java
│   │   │   ├── WebClientConfig.java
│   │   │   ├── RedisConfig.java
│   │   │   ├── SwaggerConfig.java
│   │   │   ├── CorsConfig.java
│   │   │   ├── EmailConfig.java
│   │   │   ├── NotificationConfig.java
│   │   │   └── StartupNotificationConfig.java    # ⭐ Day 23: 서버 시작/종료 알림
│   │   ├── scheduler/            # 스케줄러
│   │   │   └── TradingScheduler.java             # ⭐ Day 26: 뉴스 분석 스케줄러 추가
│   │   ├── exception/            # 예외 처리
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── EntityNotFoundException.java
│   │   │   └── ErrorCode.java
│   │   ├── security/             # 보안
│   │   │   ├── JwtUtil.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   └── LoginAttemptService.java
│   │   └── util/                 # 유틸리티
│   │       ├── JwtUtil.java
│   │       └── EncryptionUtil.java
│   └── src/main/resources/
│       ├── application.yml                       # ⭐ Day 25: groq API 설정 추가
│       ├── logback-spring.xml    # 로그 설정
│       └── templates/email/      # 이메일 템플릿
│   └── Dockerfile                # ⭐ Day 27: ARM64 호환 이미지 (alpine 제거)
│
├── frontend/                     # Vue.js 프론트엔드
│   ├── src/
│   │   ├── api/                  # API 클라이언트
│   │   │   └── index.ts
│   │   ├── components/           # 공통 컴포넌트
│   │   │   ├── TheHeader.vue
│   │   │   ├── TheSidebar.vue                    # ⭐ Day 30: 릴리즈 노트 메뉴 추가
│   │   │   └── GlobalSnackbar.vue
│   │   ├── composables/          # Composition API 유틸
│   │   │   ├── useErrorHandler.ts
│   │   │   └── useSnackbar.ts
│   │   ├── views/                # 페이지 컴포넌트
│   │   │   ├── LoginView.vue
│   │   │   ├── SignupView.vue
│   │   │   ├── DashboardView.vue                 # ⭐ Day 28: 전면 재구성, ⭐ Day 30: 최신 공지 표시
│   │   │   ├── CoinListView.vue                     # ⭐ Day 28: 코인 목록 페이지 신규
│   │   │   ├── ProfileView.vue                       # ⭐ Day 30: IP 화이트리스트 카드 추가
│   │   │   ├── TradingSettingsView.vue
│   │   │   ├── TransactionHistoryView.vue
│   │   │   ├── HoldingsView.vue                  # ⭐ Day 31: 2탭+보유현황 (기간별/코인별 수익 분석)
│   │   │   ├── BotMonitorView.vue
│   │   │   ├── DailyReportView.vue
│   │   │   ├── BacktestView.vue
│   │   │   ├── NewsView.vue                      # ⭐ Day 26: 코인 뉴스 페이지
│   │   │   ├── AdminDashboardView.vue
│   │   │   ├── ReleaseNotesView.vue               # ⭐ Day 30: 릴리즈 노트 페이지
│   │   │   └── AccountSecurityView.vue           # ⭐ Day 30: 보안 설정 페이지
│   │   ├── stores/               # Pinia 상태 관리
│   │   │   ├── auth.ts
│   │   │   └── coin.ts
│   │   ├── types/                # TypeScript 타입
│   │   │   ├── index.ts
│   │   │   ├── bot.ts
│   │   │   ├── backtest.ts
│   │   │   ├── error.ts
│   │   │   └── profit.ts         # ⭐ Day 31: 수익 분석 타입
│   │   ├── router/               # Vue Router
│   │   │   └── index.ts                          # ⭐ Day 30: /release-notes 라우트 추가
│   │   ├── App.vue
│   │   └── main.ts
│   ├── nginx.conf                # 개발용 Nginx 설정
│   ├── nginx.ssl.conf            # HTTPS용 Nginx 설정 템플릿
│   ├── index.html
│   └── vite.config.ts
│
├── docker/                       # Docker 설정
│   └── mysql/
│       ├── conf.d/               # ⭐ Day 27: MySQL 설정
│       │   └── my.cnf            # ⭐ KST 시간대, utf8mb4 문자셋
│       └── init.sql              # ⭐ Day 30: release_notes 테이블 추가
│
├── ssl/                          # ⭐ Day 29: SSL 인증서 디렉토리
│   ├── fullchain.pem             # Let's Encrypt 인증서
│   └── privkey.pem               # 개인 키
│
├── docker-compose.yml            # 개발용 (헬스체크, 재시작) ⭐ Day 25: GROQ 환경변수
├── docker-compose.prod.yml       # ⭐ Day 22: 운영용 (리소스 제한)
├── .env.example                  # 환경변수 템플릿
├── .env.development              # ⭐ Day 22-25: 개발 환경 (GROQ_API_KEY 추가)
├── .env.production               # ⭐ Day 22: 운영 환경 (Git 제외)
├── .gitignore                    # backups/, .env.*, ssl/ 제외
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
