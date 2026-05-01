# 📋 Phase 2 - 주식/ETF 자동매매 시스템 구현 계획서

## 작성일: Day 48 (Phase 2 시작)
## 문서 목적: GitHub Repo 현황 분석 + Phase 2 상세 설계 + 릴리즈 노트 카테고리 기능

---

# 🔍 1부: 현재 GitHub Repo 코드베이스 분석

## 1.1 프로젝트 구조 현황

```
crypto-trading-system/
├── backend/
│   ├── Dockerfile
│   ├── pom.xml (Java 17 + Spring Boot 3.2.x)
│   └── src/main/java/com/cryptotrading/
│       ├── CryptoTradingApplication.java
│       ├── config/              (7개)
│       │   ├── EmailConfig.java
│       │   ├── KisApiConfig.java                    (Day 50)
│       │   ├── NotificationConfig.java
│       │   ├── SecurityConfig.java
│       │   ├── StartupNotificationConfig.java
│       │   ├── SwaggerConfig.java
│       │   ├── WebMvcConfig.java
│       │   └── security/
│       │       ├── CustomAccessDeniedHandler.java
│       │       └── CustomAuthenticationEntryPoint.java
│       ├── controller/          (14개)
│       │   ├── AdminController.java
│       │   ├── AuthController.java
│       │   ├── BacktestController.java
│       │   ├── BotController.java
│       │   ├── CoinController.java
│       │   ├── HealthController.java
│       │   ├── NewsController.java
│       │   ├── NotificationController.java
│       │   ├── ProfitController.java
│       │   ├── ReleaseNoteController.java
│       │   ├── RiskManagementController.java
│       │   ├── TradingSettingController.java
│       │   ├── TransactionController.java
│       │   ├── TwoFactorController.java
│       │   ├── UpbitTestController.java
│       │   ├── UserController.java
│       │   └── stock/                                       ⭐ (Day 51)
│       │       ├── StockInfoController.java                 ⭐ (Day 51)
│       │       ├── StockSettingController.java              ⭐ (Day 51)
│       │       ├── StockBotController.java                  ⭐ (Day 56, Day 57 엔드포인트 추가)
│       │       ├── StockTransactionController.java          ⭐ (Day 58)
│       │       └── StockDashboardController.java            ⭐ (Day 59)
│       ├── dto/                 (30+ DTO 클래스)
│       │   ├── AuthResponse, DashboardStatsDTO, LoginRequest, SignupRequest
│       │   ├── TradingSettingDTO, TransactionDTO, UserInfoDTO
│       │   ├── admin/           (AdminUserDTO, MonitoringDTO, SystemStatsDTO)
│       │   ├── backtest/        (BacktestRequestDTO, BacktestResultDTO)
│       │   ├── bot/             (TradingSignalDTO)
│       │   ├── common/          (ApiResponse, PageResponse)
│       │   ├── indicator/       (IndicatorResultDTO)
│       │   ├── ⭐ kis/           (KisTokenDTO, KisQuoteDTO, KisAccountDTO, KisOrderDTO) (Day 50)
│       │   ├── ⭐ stock/         (StockInfoDTO, StockTradingSettingDTO, StockTransactionDTO) (Day 51, Day 52 BigDecimal 타입 수정, Day 58 StockTransactionDTO 추가)
│       │   ├── news/            (CoinNewsAnalysisDTO, CoinNewsDTO, GeminiRequestDTO 등)
│       │   ├── notification/    (DailyReportDTO, EmailNotificationDTO, NotificationDTO)
│       │   ├── profit/          (CoinProfitDTO, DailyAssetSnapshotDTO, PeriodProfitDTO 등)
│       │   ├── releasenote/     (ReleaseNoteDTO, ReleaseNoteRequest)
│       │   ├── totp/            (TwoFactorSetupDTO, TwoFactorVerifyRequest)
│       │   └── upbit/           (UpbitAccountDTO, UpbitCandleDTO 등)
│       ├── entity/              (18개)
│       │   ├── BaseEntity, CoinInfo, CoinNews, CoinNewsAnalysis
│       │   ├── DailyAssetSnapshot, LogLevel, PasswordResetToken
│       │   ├── ReleaseNote, SystemLog, TradingSetting
│       │   ├── Transaction, TransactionStatus, TransactionType
│       │   ├── User, UserRole
│       ├── ⭐ StockInfo, StockTradingSetting, StockTransaction (Day 49)
│       ├── ⭐ StockPriceHistory, StockDailySummary, MarketHoliday (Day 49)
│       ├── ⭐ StockAssetSnapshot (Day 59)
│       ├── exception/           (7개)
│       ├── filter/              (3개: JWT, RateLimit, RequestLogging)
│       ├── repository/          (17개, Day 58 StockTransactionRepository 쿼리 4개 추가, Day 59 StockAssetSnapshotRepository 추가)
│       ├── scheduler/
│       │   ├── TradingScheduler.java
│       │   └── StockTradingScheduler.java              ⭐ (Day 57)
│       ├── service/             (31개 서비스)
│       │   ├── ... (기존 23개)
│       │   ├── KisApiService.java                   (Day 50, Day 51 정리)
│       │   ├── KisTokenService.java                 (Day 50)
│       │   ├── StockInfoService.java                ⭐ (Day 51)
│       │   ├── StockSettingService.java             ⭐ (Day 51)
│       │   ├── StockTechnicalIndicatorService.java  ⭐ (Day 53)
│       │   ├── StockSignalDetectorService.java      ⭐ (Day 53)
│       │   ├── StockRiskManagementService.java      ⭐ (Day 54, Day 56 버그수정, Day 57 메서드 추가)
│       │   ├── MarketHolidayService.java            ⭐ (Day 54)
│       │   ├── StockTradingBotService.java          ⭐ (Day 56)
│       │   ├── StockTransactionService.java         ⭐ (Day 58)
│       │   └── StockAssetSnapshotService.java       ⭐ (Day 59)


│       └── util/                (EncryptionUtil, JwtUtil)
│
├── frontend/
│   ├── Dockerfile, package.json, vite.config.ts
│   ├── nginx.conf, nginx.ssl.conf
│   └── src/
│       ├── App.vue, main.ts
│       ├── api/index.ts
│       ├── ⭐ api/stock.ts                    (Day 52, 주식 전용 API 모듈)
│       ├── components/          (GlobalSnackbar, HelpButton, OnboardingGuide, TheHeader, TheSidebar)
│       ├── composables/         (useErrorHandler, useSnackbar)
│       ├── router/index.ts                     (Day 52 /stock-settings 라우트 추가, Day 58 /stock-transactions 라우트 추가, Day 59 /stock-dashboard 라우트 추가)
│       ├── stores/              (auth, coin, trading)
│       ├── types/               (backtest, bot, error, index, profit)
│       ├── ⭐ types/stock.ts                   (Day 52, StockInfo/StockTradingSettings 타입)
│       └── views/               (17개 뷰)
│           ├── AccountSecurityView.vue
│           ├── AdminDashboardView.vue
│           ├── BacktestView.vue
│           ├── BotMonitorView.vue
│           ├── CoinListView.vue
│           ├── DailyReportView.vue
│           ├── DashboardView.vue        (3105줄 - 가장 큰 파일)
│           ├── HelpView.vue                    (Day 52 Phase 2 FAQ 추가, Day 58 주식 거래 내역 FAQ 3개 추가)
│           ├── HoldingsView.vue
│           ├── LoginView.vue
│           ├── NewsView.vue
│           ├── ProfileView.vue                 (Day 52 KIS API 키 등록 UI 추가)
│           ├── ReleaseNotesView.vue
│           ├── SignupView.vue
│           ├── ⭐ StockTradingSettingsView.vue        (Day 52 신규, 주식 거래 설정)
│           ├── ⭐ StockTransactionHistoryView.vue     (Day 58 신규, 주식 거래 내역)
│           ├── ⭐ StockDashboardView.vue              (Day 59 신규, 주식 대시보드)
│           ├── ⭐ StockHoldingsView.vue               (Day 60 신규, 보유 주식 자산 - 코인 페이지와 UI 통일)
│           ├── ⭐ StockListView.vue                   (Day 60 신규, 주식 종목 목록)
│           └── HoldingsView.vue                      (Day 60 매도/상세 다이얼로그 디자인 통일)
│           ├── TradingSettingsView.vue               (Day 58 제목 '거래 내역' → '코인 거래 내역' 수정)
│           └── TransactionHistoryView.vue
│
├── docker/mysql/
│   ├── conf.d/my.cnf
│   └── init.sql                (약 450줄, 19개 테이블)
│
├── docker-compose.yml          (개발용)
├── docker-compose.prod.yml     (운영용)
├── scripts/                    (백업, 아카이빙, SSL 등)
├── docs/                       (아키텍처, 배포, 운영, 보안, 테스트 문서)
├── README_Phase#01.md
└── README_Phase#02.md
```

## 1.2 현재 DB 테이블 현황 (init.sql)

| # | 테이블명 | 목적 | 비고 |
|---|---------|------|------|
| 1 | users | 사용자 정보 | JWT, 2FA, IP 화이트리스트 |
| 2 | trading_settings | 코인 거래 설정 | 25+ 컬럼, 기술지표/리스크관리 |
| 3 | transactions | 코인 거래 이력 | HOLDING/SOLD/CANCELLED |
| 4 | coin_info | 코인 정보 | Top 10 초기화 |
| 5 | price_history | 가격/기술지표 이력 | MA, RSI, BB |
| 6 | daily_summary | 일간 집계 | 매수/매도/수익 |
| 7 | system_logs | 시스템 로그 | INFO/WARN/ERROR/DEBUG |
| 8 | coin_news | 뉴스 수집 | RSS, 감성점수 |
| 9 | coin_news_analysis | AI 뉴스 분석 | Groq API |
| 10 | password_reset_tokens | 비밀번호 재설정 | |
| 11 | release_notes | 릴리즈 노트 | soft delete |
| 12 | daily_asset_snapshot | 일별 자산 스냅샷 | 코인 차트용 |
| 13 | stock_info | 주식/ETF 정보 | Phase 2 (Day 49) |
| 14 | stock_trading_settings | 주식 거래 설정 | Phase 2 (Day 49) |
| 15 | stock_transactions | 주식 거래 이력 | Phase 2 (Day 49) |
| 16 | stock_price_history | 주식 가격 이력 | Phase 2 (Day 49) |
| 17 | market_holidays | 휴장일 캘린더 | Phase 2 (Day 54) |
| 18 | stock_daily_summary | 주식 일간 집계 | Phase 2 (Day 49) |
| 19 | stock_asset_snapshots | 주식 자산 스냅샷 | Phase 2 (Day 59) |

## 1.3 현재 API 엔드포인트 현황

| 영역 | Controller | 주요 엔드포인트 |
|------|-----------|---------------|
| 인증 | AuthController | /api/auth/login, /api/auth/signup |
| 사용자 | UserController | /api/user/profile, /api/user/api-keys |
| 거래설정 | TradingSettingController | /api/trading-settings/** |
| 거래내역 | TransactionController | /api/transactions/** |
| 봇 | BotController | /api/bot/status, /api/bot/execute |
| 백테스팅 | BacktestController | /api/backtest/** |
| 코인정보 | CoinController | /api/coins/** |
| 뉴스 | NewsController | /api/news/** |
| 수익분석 | ProfitController | /api/profit/** |
| 릴리즈노트 | ReleaseNoteController | /api/release-notes/** |
| 관리자 | AdminController | /api/admin/** |
| 알림 | NotificationController | /api/notifications/** |
| 리스크 | RiskManagementController | /api/risk/** |
| 2FA | TwoFactorController | /api/2fa/** |
| ⭐ 주식정보 | StockInfoController | /api/stock/info/**, /api/stock/info/prices (Day 51, Day 60 다중 종목 가격 일괄 조회) |
| ⭐ 주식설정 | StockSettingController | /api/stock/settings/** (Day 51) |
| ⭐ 주식봇 | StockBotController | /api/stock/bot/execute, /api/stock/bot/status, /api/stock/bot/start, /api/stock/bot/stop, /api/stock/bot/reset-daily-cache, /api/stock/bot/holding-warnings (Day 56~57) |
| ⭐ 주식거래내역 | StockTransactionController | /api/stock/transactions/** (Day 58) |
| ⭐ 주식대시보드 | StockDashboardController | /api/stock/dashboard/stats, /api/stock/dashboard/exchange-rate, /api/stock/dashboard/profit/snapshots, /api/stock/dashboard/profit/snapshot (Day 59) |
| 건강체크 | HealthController | /api/health |
| 업비트 | UpbitTestController | /api/upbit/test/** |

## 1.4 프론트엔드 라우트 현황

| 경로 | 뷰 | 인증 | 용도 |
|-----|-----|------|------|
| /dashboard | DashboardView | ✅ | 코인 대시보드 |
| /holdings | HoldingsView | ✅ | 보유 코인 자산 |
| /coins | CoinListView | ✅ | 코인 목록 |
| /transactions | TransactionHistoryView | ✅ | 거래 내역 |
| /trading-settings | TradingSettingsView | ✅ | 거래 설정 |
| /bot-monitor | BotMonitorView | ✅ | 봇 모니터링 |
| /daily-report | DailyReportView | ✅ | 일일 리포트 |
| /backtest | BacktestView | ✅ | 백테스팅 |
| /news | NewsView | ✅ | 코인 뉴스 |
| /release-notes | ReleaseNotesView | ✅ | 릴리즈 노트 |
| /profile | ProfileView | ✅ | 프로필 설정 |
| /account-security | AccountSecurityView | ✅ | 계정 보안 |
| /admin | AdminDashboardView | ✅+ADMIN | 관리자 |
| /help | HelpView | ✅ | 도움말 |
| /login | LoginView | 게스트 | 로그인 |
| /signup | SignupView | 게스트 | 회원가입 |
| /stock-settings | StockTradingSettingsView | ✅ | ⭐ 주식 거래 설정 (Day 52) |
| /stock-transactions | StockTransactionHistoryView | ✅ | ⭐ 주식 거래 내역 (Day 58) |
| /stock-dashboard | StockDashboardView | ✅ | ⭐ 주식 대시보드 (Day 59) |
| /stock-list | StockListView | ✅ | ⭐ 주식 종목 목록 (Day 60) |
| /stock-holdings | StockHoldingsView | ✅ | ⭐ 보유 주식 자산 (Day 60) |

## 1.5 사이드바 메뉴 현황

```
코인 거래 (v-list-group "coin", 기본 펼침)
├── 대시보드
├── 보유 코인 자산
├── 코인 목록
├── 코인 거래 내역
├── 코인 거래 설정
├── 봇 모니터링
├── 일일 리포트
├── 코인 거래 백테스팅
└── 코인 뉴스

주식 거래 (공사중) (v-list-group "stock")
├── 대시보드          ✅ 활성화 (Day 59, /stock-dashboard)
├── 보유 주식 자산    ✅ 활성화 (Day 60, /stock-holdings)
├── 주식 종목 목록    ✅ 활성화 (Day 60, /stock-list)
├── 주식 거래 내역    ✅ 활성화 (Day 58, /stock-transactions)
└── 주식 거래 설정    ✅ 활성화 (Day 52, /stock-settings)

프로필 설정
계정 보안
관리자 (ADMIN만 표시)
릴리즈 노트
도움말
```

## 1.6 Docker 구성 현황

| 서비스 | 개발(docker-compose.yml) | 운영(docker-compose.prod.yml) |
|--------|------------------------|------------------------------|
| MySQL | bridge, 3306 | bridge, 3306, 1G |
| Redis | bridge, 6379 | bridge, 6379, 512M |
| Backend | bridge, 8080 | bridge, 8080, 1.5G |
| Frontend | bridge, 80 | **host mode**, SSL, 256M |

## 1.7 핵심 서비스 분석 (Phase 2 재사용 대상)

| # | Phase 1 서비스 | 역할 | 코드량 | Phase 2 재사용 방식 |
|---|---------------|------|--------|-------------------|
| 1 | TradingBotService | 자동매매 핵심 로직 | 1059줄 | **추상화/인터페이스 분리 또는 주식용 별도 서비스** |
| 2 | SignalDetectorService | 매수/매도 신호 감지 | ~300줄 | 파라미터만 변경하여 재사용 |
| 3 | TechnicalIndicatorService | MA, RSI, BB 계산 | ~200줄 | **그대로 재사용** (범용 수학 로직) |
| 4 | RiskManagementService | 리스크 관리 | ~400줄 | 거래시간 체크 추가 |
| 5 | BacktestService | 백테스팅 | ~600줄 | 수수료율/파라미터 변경 |
| 6 | NotificationService | 알림 발송 | ~300줄 | **공통 사용** + 주식 템플릿 추가 |
| 7 | DiscordBotService | Discord DM | ~300줄 | **공통 사용** |
| 8 | EmailService | 이메일 발송 | ~200줄 | **공통 사용** |
| 9 | ProfitService | 수익 분석 | ~400줄 | 주식용 확장 |
| 10 | TransactionService | 거래 내역 관리 | ~300줄 | 주식용 별도 서비스 |

---

# 🏗 2부: Phase 2 상세 설계

## 2.1 설계 원칙

1. **코드 재사용 극대화**: Phase 1 로직을 최대한 재활용 (85~90%)
2. **기존 코드 최소 수정**: Phase 1 기능에 영향 없도록 별도 패키지/클래스로 분리
3. **공통 로직 공유**: 기술적 지표 계산, 알림, 인증 등은 Phase 1과 공유
4. **DB 테이블 분리**: 주식 전용 테이블 신규 생성 (기존 코인 테이블 수정 없음)
5. **API 네임스페이스 분리**: `/api/stock/**` 으로 분리

## 2.2 DB 스키마 변경 사항

### 2.2.1 신규 테이블 (6개)

```sql
-- ====================================================
-- Phase 2: 주식/ETF 자동매매 테이블 (신규)
-- ====================================================

-- 1. 주식/ETF 정보 테이블
CREATE TABLE IF NOT EXISTS stock_info (
    stock_code VARCHAR(20) PRIMARY KEY COMMENT '종목코드 (예: 409820)',
    stock_name VARCHAR(100) NOT NULL COMMENT '종목명',
    market VARCHAR(20) NOT NULL COMMENT '시장 (KRX/KOSDAQ)',
    etf_type ENUM('LEVERAGE', 'INVERSE', 'NORMAL', 'STOCK') NOT NULL COMMENT 'ETF 유형',
    underlying_index VARCHAR(100) COMMENT '기초지수 (예: NASDAQ100)',
    expense_ratio DECIMAL(5,3) COMMENT '운용보수율 (%)',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성화 여부',
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_stock_info_market (market),
    INDEX idx_stock_info_etf_type (etf_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 주식 거래 설정 테이블
CREATE TABLE IF NOT EXISTS stock_trading_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    stock_codes JSON NOT NULL COMMENT '거래 종목 코드 목록',
    base_period INT DEFAULT 20 COMMENT '이동평균선 기간',
    buy_threshold_pct DECIMAL(5,2) DEFAULT -3.00 COMMENT '매수 기준 하락률 (%)',
    sell_target_pct DECIMAL(5,2) DEFAULT 2.50 COMMENT '목표 수익률 (%)',
    stop_loss_pct DECIMAL(5,2) DEFAULT -5.00 COMMENT '손절매 기준 (%)',
    max_holdings_per_stock INT DEFAULT 3 COMMENT '종목당 최대 보유 건수',
    daily_limit_amount DECIMAL(15,2) DEFAULT 1000000.00 COMMENT '일일 거래 한도',
    use_trailing_stop BOOLEAN DEFAULT TRUE COMMENT '트레일링 스톱 사용',
    trailing_stop_pct DECIMAL(5,2) DEFAULT -2.50 COMMENT '트레일링 스톱 비율',
    -- 기술적 지표 설정 (Phase 1과 다른 기본값)
    rsi_period INT DEFAULT 14,
    rsi_buy_threshold INT DEFAULT 35 COMMENT 'RSI 매수 신호 (코인: 32)',
    rsi_sell_threshold INT DEFAULT 65 COMMENT 'RSI 매도 신호 (코인: 68)',
    bb_period INT DEFAULT 20,
    bb_multiplier INT DEFAULT 2,
    volume_threshold INT DEFAULT 120 COMMENT '거래량 급증 기준 (코인: 140)',
    -- 리스크 관리 설정
    daily_trade_limit_pct INT DEFAULT 20,
    max_position_pct INT DEFAULT 25,
    daily_stop_loss_pct INT DEFAULT -5,
    use_market_trend_filter BOOLEAN DEFAULT FALSE,
    cumulative_loss_limit_pct INT DEFAULT -10,
    consecutive_stop_loss_limit INT DEFAULT 3,
    fixed_buy_amount DECIMAL(15,2) DEFAULT 100000.00 COMMENT '1회 매수 금액 (주식은 최소 단위가 큼)',
    use_daily_limit_recovery BOOLEAN DEFAULT FALSE,
    use_round_robin BOOLEAN DEFAULT TRUE,
    -- ⭐ Phase 2 전용: 레버리지 ETF 관련
    max_holding_days INT DEFAULT 20 COMMENT '최대 보유일수 (레버리지 decay 방지)',
    -- ⭐ Phase 2 전용: KIS API 키 (사용자별)
    kis_app_key_encrypted TEXT COMMENT 'KIS APP KEY (AES-256 암호화)',
    kis_app_secret_encrypted TEXT COMMENT 'KIS APP SECRET (AES-256 암호화)',
    kis_account_no_encrypted TEXT COMMENT 'KIS 계좌번호 (AES-256 암호화)',
    kis_mock_mode BOOLEAN DEFAULT TRUE COMMENT '모의투자 모드',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_stock_settings_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 주식 거래 이력 테이블
CREATE TABLE IF NOT EXISTS stock_transactions (
    transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    stock_code VARCHAR(20) NOT NULL COMMENT '종목코드',
    type ENUM('BUY', 'SELL') NOT NULL,
    quantity INT NOT NULL COMMENT '거래 수량 (주)',
    price DECIMAL(15,2) NOT NULL COMMENT '체결 가격',
    fee DECIMAL(15,2) DEFAULT 0.00 COMMENT '거래 수수료',
    total_amount DECIMAL(15,2) NOT NULL COMMENT '총 거래 금액',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '매수 시각',
    sold_at TIMESTAMP NULL COMMENT '매도 시각',
    sold_price DECIMAL(15,2) NULL COMMENT '매도 가격',
    profit_loss DECIMAL(15,2) NULL COMMENT '손익',
    profit_loss_pct DECIMAL(5,2) NULL COMMENT '수익률 (%)',
    target_sell_price DECIMAL(15,2) NULL COMMENT '목표 매도가',
    stop_loss_price DECIMAL(15,2) NULL COMMENT '손절가',
    status ENUM('HOLDING', 'SOLD', 'CANCELLED') DEFAULT 'HOLDING',
    note TEXT COMMENT '메모',
    highest_price DECIMAL(15,2) NULL COMMENT '보유 기간 중 최고가 (트레일링 스톱용)',
    holding_days INT DEFAULT 0 COMMENT '보유 일수 (거래일 기준)',
    exchange_rate DECIMAL(10,4) NULL COMMENT '환율 (환노출형 ETF용)',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_stock_tx_user_code_status (user_id, stock_code, status),
    INDEX idx_stock_tx_created_at (created_at),
    INDEX idx_stock_tx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 주식 가격 이력 테이블
CREATE TABLE IF NOT EXISTS stock_price_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL,
    price DECIMAL(15,2) NOT NULL COMMENT '종가',
    volume BIGINT NOT NULL COMMENT '거래량',
    timestamp DATE NOT NULL COMMENT '거래일',
    open_price DECIMAL(15,2) NULL COMMENT '시가',
    high_price DECIMAL(15,2) NULL COMMENT '고가',
    low_price DECIMAL(15,2) NULL COMMENT '저가',
    ma7 DECIMAL(15,2) NULL,
    ma20 DECIMAL(15,2) NULL,
    ma30 DECIMAL(15,2) NULL,
    rsi DECIMAL(5,2) NULL,
    bb_upper DECIMAL(15,2) NULL,
    bb_lower DECIMAL(15,2) NULL,
    INDEX idx_stock_price_code_date (stock_code, timestamp),
    UNIQUE KEY unique_stock_code_date (stock_code, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 휴장일 캘린더 테이블
CREATE TABLE IF NOT EXISTS market_holidays (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    holiday_date DATE NOT NULL COMMENT '휴장일',
    holiday_name VARCHAR(100) NOT NULL COMMENT '휴장 사유',
    market VARCHAR(20) DEFAULT 'KRX' COMMENT '시장',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_market_holiday (market, holiday_date),
    INDEX idx_holiday_date (holiday_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. 주식 일간 집계 테이블
CREATE TABLE IF NOT EXISTS stock_daily_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    date DATE NOT NULL,
    total_profit DECIMAL(15,2) DEFAULT 0.00,
    profit_rate DECIMAL(5,2) DEFAULT 0.00,
    buy_count INT DEFAULT 0,
    sell_count INT DEFAULT 0,
    total_investment DECIMAL(15,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY unique_stock_user_date (user_id, date),
    INDEX idx_stock_summary_user_date (user_id, date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2.2.2 기존 테이블 변경 사항

#### ⭐ release_notes 테이블 변경 (카테고리 기능 추가)

**변경 이유**: 릴리즈 노트에 카테고리를 추가하여 코인/주식별로 분류하고, 각 대시보드에서 해당 카테고리의 최신 노트만 표시하기 위함

```sql
-- 기존 release_notes 테이블에 category 컬럼 추가 (ALTER TABLE)
ALTER TABLE release_notes ADD COLUMN category VARCHAR(20) DEFAULT 'GENERAL' 
    COMMENT '카테고리 (COIN: 코인, STOCK: 주식, GENERAL: 공통)' AFTER content;

ALTER TABLE release_notes ADD INDEX idx_release_notes_category (category);

-- 기존 데이터 업데이트 (기존 릴리즈 노트는 COIN 카테고리로)
UPDATE release_notes SET category = 'COIN' WHERE category = 'GENERAL';
```

**변경 전 → 변경 후:**
```
release_notes 테이블:
  ... (기존 컬럼)
  content TEXT NOT NULL COMMENT '게시글 본문',
  ⭐ [추가] category VARCHAR(20) DEFAULT 'GENERAL' COMMENT '카테고리',
  author_id VARCHAR(50) NOT NULL COMMENT '작성자 ID',
  ... (기존 컬럼)
```

### 2.2.3 초기 데이터

~~ETF 초기 데이터 및 휴장일 하드코딩 삭제됨 (Day 49 설계 변경)~~

**변경 사유**: 
- ETF 종목: 사용자가 KIS API 종목 검색을 통해 직접 추가하는 방식으로 변경
- 휴장일: API 자동 수집(매년 1월 1일 스케줄러) + 관리자 수동 등록 방식으로 변경
- init.sql에는 CREATE TABLE만 포함, INSERT 데이터 없음

**데이터 관리 방식**:
| 테이블 | 데이터 입력 방식 | 담당 서비스 | 구현 예정 |
|--------|---------------|------------|----------|
| stock_info | 사용자가 KIS API 종목 검색 → 선택 → 추가 | StockInfoService | Day 51 |
| market_holidays | 공공데이터포털/KRX API 자동 수집 + 관리자 수동 등록 | MarketHolidayService | Day 54 |

---

## 2.3 백엔드 구현 설계

### 2.3.1 신규 생성 파일 목록

```
backend/src/main/java/com/cryptotrading/
├── config/
│   └── ⭐ KisApiConfig.java               (KIS API WebClient 설정)
│
├── controller/
│   ├── ⭐ StockBotController.java          (주식 봇 상태/실행 API)
│   ├── ⭐ StockSettingController.java       (주식 거래 설정 CRUD)
│   ├── ⭐ StockTransactionController.java   (주식 거래 내역 API)
│   ├── ⭐ StockBacktestController.java      (주식 백테스팅 API)
│   ├── ⭐ StockInfoController.java          (주식 종목 정보 API)
│   ├── ⭐ StockProfitController.java        (주식 수익 분석 API)
│   └── ⭐ MarketHolidayController.java      (휴장일 관리 API - ADMIN)
│
├── dto/
│   ├── kis/
│   │   ├── ⭐ KisTokenDTO.java
│   │   ├── ⭐ KisQuoteDTO.java
│   │   ├── ⭐ KisAccountDTO.java
│   │   └── ⭐ KisOrderDTO.java
│   ├── stock/
│   │   ├── ⭐ StockTradingSettingDTO.java
│   │   ├── ⭐ StockTransactionDTO.java
│   │   ├── ⭐ StockDashboardStatsDTO.java
│   │   └── ⭐ StockInfoDTO.java
│   └── backtest/
│       ├── ⭐ StockBacktestRequestDTO.java
│       └── ⭐ StockBacktestResultDTO.java
│
├── entity/
│   ├── ⭐ StockInfo.java
│   ├── ⭐ StockTradingSetting.java
│   ├── ⭐ StockTransaction.java
│   ├── ⭐ StockPriceHistory.java
│   ├── ⭐ MarketHoliday.java
│   └── ⭐ StockDailySummary.java
│
├── repository/
│   ├── ⭐ StockInfoRepository.java
│   ├── ⭐ StockTradingSettingRepository.java
│   ├── ⭐ StockTransactionRepository.java
│   ├── ⭐ StockPriceHistoryRepository.java
│   ├── ⭐ MarketHolidayRepository.java
│   └── ⭐ StockDailySummaryRepository.java
│
├── scheduler/
│   └── ⭐ StockTradingScheduler.java       (주식 전용 스케줄러)
│
└── service/
    ├── ⭐ KisApiService.java               (KIS API 연동)
    ├── ⭐ KisTokenService.java             (토큰 관리/갱신)
    ├── ⭐ StockTradingBotService.java       (주식 자동매매 핵심)
    ├── ⭐ StockSignalDetectorService.java   (주식 매수/매도 신호)
    ├── ⭐ StockRiskManagementService.java   (주식 리스크 관리)
    ├── ⭐ StockBacktestService.java         (주식 백테스팅)
    ├── ⭐ StockTransactionService.java      (주식 거래 내역 관리)
    ├── ⭐ StockProfitService.java           (주식 수익 분석)
    ├── ⭐ StockSettingService.java          (주식 거래 설정 관리)
    ├── ⭐ StockInfoService.java             (주식 정보 관리)
    └── ⭐ MarketHolidayService.java         (휴장일 관리)
```

### 2.3.2 기존 파일 변경 목록 (최소 변경)

| 파일 | 변경 내용 | 변경 이유 |
|------|----------|----------|
| **SecurityConfig.java** | `/api/stock/**` 권한 설정 추가 | 주식 API 보안 설정 |
| **ReleaseNote.java** | `category` 필드 추가 | 카테고리 기능 |
| **ReleaseNoteDTO.java** | `category` 필드 추가 | 카테고리 전달 |
| **ReleaseNoteRequest.java** | `category` 필드 추가 | 카테고리 저장 |
| **ReleaseNoteRepository.java** | 카테고리별 조회 메서드 추가 | 대시보드별 필터링 |
| **ReleaseNoteService.java** | 카테고리 조회 로직 추가 | 카테고리별 최신 1건 |
| **ReleaseNoteController.java** | 카테고리 파라미터 추가 | API 필터링 |
| **application.yml** | KIS API 설정 추가 | API 연동 |
| **docker-compose.yml** | KIS 환경변수 추가 | 컨테이너 설정 |
| **docker-compose.prod.yml** | KIS 환경변수 추가 | 운영 설정 |
| **init.sql** | Phase 2 테이블/데이터 추가 | DB 초기화 |
| **TradingScheduler.java** | 주식 스케줄러 참조 추가 (선택) | 통합 관리 |
| **NotificationService.java** | 주식 알림 템플릿 추가 | 알림 통합 |

### 2.3.3 Phase 1 코드 재사용 상세

| Phase 1 서비스 | 재사용 방식 | Phase 2 변경점 |
|---------------|-----------|---------------|
| **TechnicalIndicatorService** | **100% 재사용** (범용 수학 로직) | import만 하면 됨 |
| **EncryptionUtil** | **100% 재사용** | KIS API 키 암호화에도 동일 사용 |
| **JwtUtil** | **100% 재사용** | 인증 공유 |
| **NotificationService** | **95% 재사용** + 템플릿 추가 | 주식 알림 메시지 포맷 추가 |
| **DiscordBotService** | **100% 재사용** | 메시지 포맷만 다름 |
| **EmailService** | **100% 재사용** | 이메일 템플릿 추가 |
| **RiskManagementService 로직** | **80% 재사용** | 거래시간 체크, 보유일수 체크 추가 |
| **SignalDetectorService 로직** | **80% 재사용** | 파라미터 기본값 변경, 보유기간 체크 |
| **TradingBotService 로직** | **70% 재사용** | 라운드로빈, 분할매수 로직 동일. 주문 API만 KIS로 교체 |
| **BacktestService 로직** | **75% 재사용** | 수수료율, 슬리피지 변경. 시뮬레이션 로직 동일 |

### 2.3.4 주식 스케줄러 설계

```java
// StockTradingScheduler.java
@Scheduled(cron = "0 50 8 * * MON-FRI", zone = "Asia/Seoul")  // 08:50 장 시작 알림
@Scheduled(cron = "0 */5 9-15 * * MON-FRI", zone = "Asia/Seoul")  // 09:00~15:25 5분마다 자동매매
@Scheduled(cron = "0 30 15 * * MON-FRI", zone = "Asia/Seoul")  // 15:30 장 마감 처리
@Scheduled(cron = "0 35 15 * * MON-FRI", zone = "Asia/Seoul")  // 15:35 일일 리포트
@Scheduled(cron = "0 0 0 * * MON-FRI", zone = "Asia/Seoul")    // 00:00 보유일수 업데이트
```

### 2.3.5 KIS API 연동 핵심 설계

```
KIS API 호출 흐름:
1. KisTokenService: OAuth 토큰 발급/갱신 (24시간 유효)
2. KisApiService: REST API 호출
   - 현재가 조회: /uapi/domestic-stock/v1/quotations/inquire-price
   - 일봉 조회: /uapi/domestic-stock/v1/quotations/inquire-daily-price
   - 잔고 조회: /uapi/domestic-stock/v1/trading/inquire-balance
   - 매수 주문: /uapi/domestic-stock/v1/trading/order-cash
   - 매도 주문: /uapi/domestic-stock/v1/trading/order-cash
3. Rate Limit: 초당 20건 제한 (Redis로 관리)
```

---

## 2.4 프론트엔드 구현 설계

### 2.4.1 신규 생성 파일 목록

```
frontend/src/
├── views/
│   ├── ⭐ StockDashboardView.vue           (주식 대시보드)
│   ├── ⭐ StockHoldingsView.vue            (보유 주식 자산)
│   ├── ⭐ StockListView.vue                (주식 종목 목록)
│   ├── ⭐ StockTransactionHistoryView.vue   (주식 거래 내역)
│   ├── ⭐ StockTradingSettingsView.vue      (주식 거래 설정)
│   ├── ⭐ StockBotMonitorView.vue           (주식 봇 모니터링)
│   ├── ⭐ StockBacktestView.vue             (주식 백테스팅)
│   └── ⭐ AdminHolidayView.vue              (관리자 휴장일 관리)
│
├── types/
│   └── ⭐ stock.ts                          (주식 관련 TypeScript 타입)
│
└── stores/
    └── ⭐ stock.ts                          (주식 관련 Pinia 스토어)
```

### 2.4.2 기존 파일 변경 목록

| 파일 | 변경 내용 | 변경 이유 |
|------|----------|----------|
| **router/index.ts** | 주식 관련 라우트 8개 추가 | 페이지 라우팅 |
| **TheSidebar.vue** | 주식 메뉴 활성화 (disabled 해제) + 추가 메뉴 | 네비게이션 |
| **ReleaseNotesView.vue** | 카테고리 필터 드롭다운 추가 | 카테고리 기능 |
| **DashboardView.vue** | 릴리즈 노트 API 호출에 category=COIN 파라미터 추가 | 코인 릴리즈만 표시 |

### 2.4.3 라우트 추가 계획

```typescript
// router/index.ts에 추가될 라우트
{ path: '/stock/dashboard', name: 'stock-dashboard', component: StockDashboardView, meta: { requiresAuth: true } },
{ path: '/stock/holdings', name: 'stock-holdings', component: StockHoldingsView, meta: { requiresAuth: true } },
{ path: '/stock/list', name: 'stock-list', component: StockListView, meta: { requiresAuth: true } },
{ path: '/stock/transactions', name: 'stock-transactions', component: StockTransactionHistoryView, meta: { requiresAuth: true } },
{ path: '/stock/settings', name: 'stock-settings', component: StockTradingSettingsView, meta: { requiresAuth: true } },
{ path: '/stock/bot-monitor', name: 'stock-bot-monitor', component: StockBotMonitorView, meta: { requiresAuth: true } },
{ path: '/stock/backtest', name: 'stock-backtest', component: StockBacktestView, meta: { requiresAuth: true } },
{ path: '/admin/holidays', name: 'admin-holidays', component: AdminHolidayView, meta: { requiresAuth: true, requiresAdmin: true } },
```

### 2.4.4 사이드바 변경 계획

```
주식 거래 (v-list-group "stock")   ← "공사중" 제거, disabled 해제
├── 대시보드                      ← enabled, @click="/stock/dashboard"
├── 보유 주식 자산                 ← enabled, @click="/stock/holdings"
├── 주식 종목 목록                 ← enabled, @click="/stock/list"
├── 주식 거래 내역                 ← enabled, @click="/stock/transactions"
├── 주식 거래 설정                 ← enabled, @click="/stock/settings"
├── ⭐ 봇 모니터링                 ← 신규 추가
└── ⭐ 주식 백테스팅               ← 신규 추가
```

---

## 2.5 릴리즈 노트 카테고리 기능 상세 설계

### 2.5.1 카테고리 정의

| 카테고리 값 | 표시명 | 용도 |
|-----------|--------|------|
| COIN | 코인 | 코인 관련 업데이트 |
| STOCK | 주식 | 주식 관련 업데이트 |
| GENERAL | 공통 | 시스템 전반 공지 |

### 2.5.2 백엔드 변경 상세

#### ReleaseNote.java (Entity)
```java
// 기존:
private String content;
private String authorId;

// 변경 후:
private String content;
⭐ @Column(length = 20)
⭐ @Builder.Default
⭐ private String category = "GENERAL";  // COIN, STOCK, GENERAL
private String authorId;
```

#### ReleaseNoteDTO.java
```java
// 기존:
private String content;
private String authorId;

// 변경 후:
private String content;
⭐ private String category;
private String authorId;
```

#### ReleaseNoteRequest.java
```java
// 기존:
private String content;

// 변경 후:
private String content;
⭐ @Size(max = 20)
⭐ private String category = "GENERAL";
```

#### ReleaseNoteRepository.java (추가 메서드)
```java
// 기존:
Optional<ReleaseNote> findFirstByIsDeletedFalseOrderByCreatedAtDesc();

// ⭐ 추가:
Optional<ReleaseNote> findFirstByCategoryAndIsDeletedFalseOrderByCreatedAtDesc(String category);

Page<ReleaseNote> findByCategoryAndIsDeletedFalseOrderByCreatedAtDesc(String category, Pageable pageable);

@Query("SELECT r FROM ReleaseNote r WHERE r.isDeleted = false " +
       "AND (:category IS NULL OR r.category = :category) " +
       "AND (r.title LIKE %:keyword% OR r.authorName LIKE %:keyword% OR r.content LIKE %:keyword%) " +
       "ORDER BY r.createdAt DESC")
Page<ReleaseNote> searchByKeywordAndCategory(@Param("keyword") String keyword, 
                                              @Param("category") String category, 
                                              Pageable pageable);
```

#### ReleaseNoteController.java (변경)
```java
// 기존: /api/release-notes/latest
// ⭐ 변경: /api/release-notes/latest?category=COIN
@GetMapping("/latest")
public ResponseEntity<ReleaseNoteDTO> getLatestReleaseNote(
        @RequestParam(required = false) String category) {
    // category가 있으면 해당 카테고리 최신 1건, 없으면 전체 최신 1건
}

// 기존: /api/release-notes?page=0&size=10&keyword=xxx
// ⭐ 변경: /api/release-notes?page=0&size=10&keyword=xxx&category=COIN
@GetMapping
public ResponseEntity<Page<ReleaseNoteDTO>> getReleaseNotes(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String category) {  // ⭐ 추가
}
```

### 2.5.3 프론트엔드 변경 상세

#### DashboardView.vue (코인 대시보드)
```javascript
// 기존:
const response = await api.get('/release-notes/latest')

// ⭐ 변경:
const response = await api.get('/release-notes/latest', { params: { category: 'COIN' } })
```
**변경 이유**: 코인 대시보드에서는 코인 카테고리의 가장 최근 릴리즈 노트만 표시

#### StockDashboardView.vue (주식 대시보드 - 신규)
```javascript
// ⭐ 신규: 주식 카테고리의 최신 릴리즈 노트만 조회
const response = await api.get('/release-notes/latest', { params: { category: 'STOCK' } })
```

#### ReleaseNotesView.vue (릴리즈 노트 목록)
```html
<!-- ⭐ 추가: 카테고리 필터 드롭다운 -->
<v-select
  v-model="selectedCategory"
  :items="categoryOptions"
  label="카테고리"
  density="compact"
  hide-details
  variant="outlined"
  bg-color="white"
  clearable
  @update:model-value="loadReleaseNotes"
/>
```
```javascript
// ⭐ 추가:
const selectedCategory = ref('')
const categoryOptions = [
  { title: '전체', value: '' },
  { title: '🟡 코인', value: 'COIN' },
  { title: '🔵 주식', value: 'STOCK' },
  { title: '⚪ 공통', value: 'GENERAL' }
]

// API 호출에 category 파라미터 추가
const response = await api.get('/release-notes', {
  params: {
    page: currentPage.value - 1,
    size: pageSize.value,
    keyword: searchKeyword.value || undefined,
    category: selectedCategory.value || undefined  // ⭐ 추가
  }
})
```

```html
<!-- ⭐ 추가: 작성 폼에 카테고리 선택 -->
<v-select
  v-model="form.category"
  :items="['COIN', 'STOCK', 'GENERAL']"
  label="카테고리"
  :rules="[v => !!v || '카테고리를 선택해주세요']"
/>

<!-- ⭐ 추가: 테이블에 카테고리 표시 컬럼 -->
<template v-slot:item.category="{ item }">
  <v-chip :color="getCategoryColor(item.category)" size="x-small" variant="flat">
    {{ getCategoryLabel(item.category) }}
  </v-chip>
</template>
```

---

## 2.6 application.yml 추가 설정

```yaml
# ⭐ Phase 2: KIS API 설정 (기존 내용 뒤에 추가)
kis:
  api:
    base-url: ${KIS_BASE_URL:https://openapivts.koreainvestment.com:29443}  # 모의투자
    # 실전: https://openapi.koreainvestment.com:9443
    app-key: ${KIS_APP_KEY:}
    app-secret: ${KIS_APP_SECRET:}
    account-no: ${KIS_ACCOUNT_NO:}
    mock-mode: ${KIS_MOCK_MODE:true}
  websocket:
    url: ${KIS_WS_URL:ws://ops.koreainvestment.com:31000}
```

---

## 2.7 Docker 환경변수 추가

```env
# .env.development / .env.production 추가
KIS_APP_KEY=
KIS_APP_SECRET=
KIS_ACCOUNT_NO=
KIS_MOCK_MODE=true
KIS_BASE_URL=https://openapivts.koreainvestment.com:29443
```

---

# 📅 3부: 일별 진행 계획 (수정안)

| Day | 작업 내용 | 핵심 파일 | 상태 |
|-----|----------|----------|------|
| **48** | ① 릴리즈 노트 카테고리 기능 구현 (DB/Entity/DTO/Repository/Service/Controller/Frontend) | release_notes 관련 전체 | ✅ 완료 |
| **49** | ② DB 테이블 6개 생성 + Entity 6개/Repository 6개 구현 + init.sql 업데이트 (ip_whitelist 누락 수정 포함) | init.sql, 6개 Entity, 6개 Repository | ✅ 완료 |
| **50** | ③ KIS API 연동 기반 (토큰 관리, WebClient 설정, 시세/잔고/주문 API) + KIS DTO 4개 + SecurityConfig 주식 API 권한 + Docker 환경변수 | KisApiConfig, KisApiService, KisTokenService, KisTokenDTO, KisQuoteDTO, KisAccountDTO, KisOrderDTO | ✅ 완료 |
| **51** | ④ StockInfoService/Controller + StockSettingService/Controller + DTO 2개 + API 15개 (Postman 17건 테스트 통과) | StockInfoDTO, StockTradingSettingDTO, StockInfoService, StockSettingService, StockInfoController, StockSettingController + KisApiService/KisAccountDTO/StockInfoRepository/SecurityConfig/StockInfoService 수정 | ✅ 완료 |
| **52** | ⑤ 주식 거래 설정 프론트엔드 + KIS API 키 등록 UI + 도움말 개선 | StockTradingSettingsView.vue, api/stock.ts, types/stock.ts, router, TheSidebar, ProfileView, HelpView, StockTradingSettingDTO | ✅ 완료 |
| **53** | ⑥ StockTechnicalIndicatorService + StockSignalDetectorService (KIS API 일봉 기반 기술지표 계산 + 매수/매도 신호 감지 + 레버리지 ETF 보유기간 강제 매도) | 기술지표/신호 감지 (Phase 1 재사용) | ✅ 완료 |
| **54** | ⑦ StockRiskManagementService + MarketHolidayService (거래시간/한도/비중/긴급정지/연속손절/ETF보유기간 + 휴장일 CRUD/거래일계산/@Cacheable) | StockRiskManagementService, MarketHolidayService, StockTransactionRepository(+3), MarketHolidayRepository(+3) | ✅ 완료 |
| **55** | ⑧ 주식 거래 설정 UI/백엔드 동기화 (useStopLoss/additionalDropPct/useAiAnalysis 추가, 이동평균선 Radio 통일, helpContents 16개 도움말, 빌드오류 2건 수정) | init.sql, StockTradingSetting, StockTradingSettingDTO, StockSettingService, StockTradingSettingsView.vue | ✅ 완료 |
| **56** | ⑨ StockTradingBotService (자동매매 핵심) + StockBotController (봇 REST API 4개) + StockRiskManagementService Redis 키 %d→%s 버그수정 | StockTradingBotService, StockBotController, StockRiskManagementService | ✅ 완료 |
| **57** | ⑩ StockTradingScheduler (3분 주기 자동매매 + 장시작/마감 알림 + 보유기간 경고 + 캐시정리) + StockRiskManagementService (getHoldingDaysWarnings/clearStockDailyCache/HoldingDaysWarning DTO 추가) + StockBotController (reset-daily-cache/holding-warnings 엔드포인트 추가) | StockTradingScheduler, StockRiskManagementService, StockBotController | ✅ 완료 |
| **58** | ⑪ StockTransactionDTO + StockTransactionService + StockTransactionController + StockTransactionHistoryView.vue (거래 내역 조회/검색/수동매도/메모수정 + 보유일 경고 색상 + HelpButton + Phase 1 스타일 통일 + 종목드롭다운 bugfix) | 거래 내역 | ✅ 완료 |
| **59** | ⑫ StockDashboardView (주식 대시보드 프론트엔드) + StockAssetSnapshot Entity/Repository/Service + StockDashboardController (통계/환율/스냅샷 API) + 스냅샷 자동화 (23:59 스케줄 / 거래 즉시 갱신 / 수동 갱신) + 스냅샷 API 경로 오류 수정 (클래스 레벨 prefix 중복 해결) **+ [후속] 차트 레이아웃 버그 수정 (.chart-container position:relative/width:100% + SVG 명시적 크기 지정, 개발자 도구 OFF 상태 SVG 늘어남 문제 해결) + 코인 대시보드 차트 바닥 회색 파선 누락 버그 수정 + 두 대시보드 chartPeriod 기본값 'all'→'7' 변경** | 대시보드 + 스냅샷 시스템 | ✅ 완료 |
| **60** | ⑬ StockHoldingsView + StockListView 신규 작성 + 백엔드 다중 종목 가격 일괄 조회 API + 프론트엔드 API 모듈 확장 + 라우터/사이드바 연결 + **코인 보유 자산 페이지와 UI 100% 통일 (탭/요약카드/highlight-card/매도다이얼로그/상세다이얼로그 디자인 시스템 통일)** | StockPriceDTO, StockInfoService(getPricesForStocks), StockInfoController, api/stock.ts(StockPrice/getPrices), StockHoldingsView.vue, StockListView.vue, router/index.ts, TheSidebar.vue, **HoldingsView.vue (매도/상세 다이얼로그 디자인 통일)** | ✅ 완료 |
| **61** | ⑭ StockBotMonitorView | 봇 모니터링 | ⏳ 예정 |
| **62** | ⑮ StockBacktestService + StockBacktestView | 백테스팅 | ⏳ 예정 |
| **63** | ⑯ StockProfitService + 수익분석 + 일일리포트 + **StockHoldingsView 수익분석 영역 활성화** ⭐ | 수익 분석 | ⏳ 예정 |
| **64** | ⑰ AdminHolidayView + 관리자 통합 | 관리자 기능 | ⏳ 예정 |
| **65** | ⑱ 사이드바 활성화 + SecurityConfig 업데이트 + 통합 테스트 | 통합 | ⏳ 예정 |
| **66** | ⑲ 최종 테스트 + 문서화 + v2.0 릴리즈 + (v2.1 리팩토링 계획 수립: com.cryptotrading → com.investment 패키지 리네이밍, controller/service/entity 서브패키지 crypto/stock/common 분리) | 배포 + 리팩토링 계획서 | ⏳ 예정 |

---

---

## 📌 Day 63 상세 작업 내역 (Day 60에서 보류된 항목 포함)

> **Day 60 작업 시 보류된 사항**: StockHoldingsView의 "기간별/주식별 수익 분석" 영역이 Day 63에서 완성됩니다.

### 백엔드 작업
- [ ] `StockProfitService.java` 신규 작성 (Phase 1 ProfitService 패턴 재사용)
  - [ ] `getStockPeriodStats(userId, period)` — 기간별 수익 분석
  - [ ] `getStockStats(userId)` — 종목별 수익 분석
  - [ ] `getStockAssetSnapshots(userId, period)` — 자산 변동 추이 데이터
- [ ] `StockProfitController.java` 신규 작성
  - [ ] `GET /api/stock/profit/period-stats?period=...`
  - [ ] `GET /api/stock/profit/stock-stats`
  - [ ] `GET /api/stock/profit/asset-snapshots`
- [ ] `StockDailyReportService.java` 신규 작성 (일일 리포트 주식 통합)
  - [ ] 23:50 일일 리포트에 주식 거래 요약 포함
  - [ ] 코인 + 주식 통합 리포트 (Discord/이메일)

### 프론트엔드 작업
- [ ] `frontend/src/views/StockHoldingsView.vue` 수정
  - [ ] **현재 placeholder 영역을 실제 기능으로 교체** ⭐
  - [ ] "기간별 수익" 탭 활성화: 오늘/이번달/올해/1년/누적 + 자산 변동 차트
  - [ ] "주식별 수익" 탭 활성화: 종목별 수익 리스트 + 수익률
  - [ ] Phase 1 `HoldingsView.vue` 코드 패턴 차용 (구조 동일)
- [ ] `frontend/src/api/stock.ts` 확장
  - [ ] `stockProfitApi.getPeriodStats()`, `getStockStats()`, `getAssetSnapshots()` 메서드 추가

### 주의사항
- 코인 보유 자산 페이지(`HoldingsView.vue`)와 100% 동일한 UI 구조 유지
- Phase 1 코드 재사용률 80% 이상 목표 (수익률 계산 로직, 차트 컴포넌트, 기간 필터 등)
- 일일 리포트는 코인+주식 통합 형태로 발송 (별도 발송 X)

---

# 🔮 3.5부: v2.0 완료 이후 미래 예정 작업

> **전제 조건**: Day 66 (v2.0 릴리즈) 완료 후 진행
> **목적**: Phase별 Backend 컨테이너 분리 + 패키지 구조 정리 + Phase 3 확장 기반 마련
> **원칙**: 기존 기능 동작에 영향 없는 순차적 리팩토링

---

## 📌 배경 및 필요성

현재(v2.0) 구조는 코인/주식 자동매매 로직이 단일 Spring Boot 애플리케이션(`crypto-backend`)에 공존한다.
Phase 3(달러 거래 등) 추가 시 단일 컨테이너에 계속 누적되면 다음 문제가 발생한다.

- 코인 봇 장애가 주식 봇에 영향 (JVM 공유)
- Phase별 독립 배포/재시작 불가
- 패키지 `com.cryptotrading`가 주식/달러 거래까지 포함하는 혼용 구조
- API 경로 `/api/coins`, `/api/stocks` 혼재 → Nginx 라우팅 분리 기반 없음

---

## 🗂 v2.1: 패키지 구조 리팩토링 (분리 전 전처리)

> **소요 예상**: 2~3일  
> **핵심 원칙**: 기능 변경 없이 파일 이동/이름 변경만 수행

### v2.1 작업 목록

| # | 작업 | 변경 전 | 변경 후 |
|---|------|---------|---------|
| 1 | 패키지 루트 리네이밍 | `com.cryptotrading` | `com.investment` |
| 2 | 서비스 서브패키지 분리 | `service/*.java` (36개 혼재) | `service/crypto/`, `service/stock/`, `service/common/` |
| 3 | 컨트롤러 서브패키지 분리 | `controller/*.java` (이미 `controller/stock/` 존재) | `controller/crypto/`, `controller/stock/`, `controller/common/` |
| 4 | Entity 서브패키지 분리 | `entity/*.java` (18개 혼재) | `entity/crypto/`, `entity/stock/`, `entity/common/` |
| 5 | DTO 서브패키지 정리 | `dto/kis/` → 이동 | `dto/stock/kis/` |

### v2.1 서비스 분류 기준 (GitHub 코드베이스 기준)

**`service/crypto/` (코인 전용)**
```
TradingBotService.java          ← Phase 1 핵심
TradingSettingService.java
SignalDetectorService.java
TechnicalIndicatorService.java  ← 수학 로직, crypto용 파라미터
RiskManagementService.java
BacktestService.java            ← UpbitApiService 의존
UpbitApiService.java
CoinInfoService.java
TransactionService.java
NewsCollectorService.java
NewsAnalysisService.java
GeminiApiService.java
```

**`service/stock/` (주식 전용)**
```
StockTradingBotService.java     ← Phase 2 핵심 (Day 56 완료)
StockSettingService.java
StockSignalDetectorService.java ← Phase 1 SignalDetectorService 재사용 구조
StockTechnicalIndicatorService.java
StockRiskManagementService.java
StockInfoService.java
KisApiService.java
KisTokenService.java
MarketHolidayService.java
```

**`service/common/` (코인/주식 공통)**
```
AuthService.java
UserService.java
AdminService.java
NotificationService.java        ← Discord/Email 공통 발송
EmailService.java
DiscordBotService.java
ProfitService.java              ← v2.1에서 StockProfitService 분리 후 공통 인터페이스화
DailyAssetSnapshotService.java
DailyReportService.java
ReleaseNoteService.java
CacheService.java
LoginAttemptService.java
TotpService.java
MonitoringService.java
MonitoringAlertService.java
AdminAlertNotificationService.java
```

### v2.1 주의사항

- IntelliJ `Refactor → Rename` 기능 사용 시 import 자동 수정됨 (수동 편집 불필요)
- `@SpringBootTest` 기반 통합 테스트 클래스의 패키지 선언도 함께 수정 필요
- `docker-compose.yml`의 빌드 경로는 상대경로(`./backend`) 사용 중이므로 변경 불필요
- `init.sql`은 Java 패키지와 무관하므로 변경 불필요

---

## 🏗 v3.0: Phase별 Backend 컨테이너 분리

> **전제**: v2.1 패키지 리팩토링 완료 후 진행  
> **소요 예상**: 3~4일  
> **목표**: `coin-backend`(8080) / `stock-backend`(8081) 독립 컨테이너

### v3.0 목표 아키텍처

```
[Frontend - Vue 3 · Nginx · host mode]
        ↓ /api/coin/** → :8080
        ↓ /api/stock/** → :8081
        ↓ /api/auth/** → :8080 (Auth 마스터)

[coin-backend :8080]          [stock-backend :8081]
  crypto 패키지 전체             stock 패키지 전체
  common 패키지 포함             common 패키지 복사 or 공유
  Auth/User/Admin 마스터          JWT 검증만 (공유 Secret)
  업비트 API 연동                KIS API 연동
        ↓                              ↓
[MySQL 8.0 · 공유]            [Redis 7.x · 공유]
  스키마는 이미 Phase별 분리       키 prefix: coin: / stock:
```

### v3.0 작업 목록

#### ① Nginx 라우팅 분리 (`nginx.conf` / `nginx.ssl.conf` 수정)

**수정 위치**: `frontend/nginx.conf` 및 `frontend/nginx.ssl.conf`

**변경 이유**: 현재 `/api` 전체를 단일 백엔드로 프록시 중. 컨테이너 분리 후 경로별 라우팅 필요.

```nginx
# 변경 전 (현재)
location /api/ {
    proxy_pass http://backend:8080/api/;
}

# 변경 후 (coin-backend / stock-backend 분리)
location /api/coin/ {
    proxy_pass http://coin-backend:8080/api/coin/;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}

location /api/stock/ {
    proxy_pass http://stock-backend:8081/api/stock/;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}

# Auth/User/Admin/공통은 coin-backend 마스터
location /api/ {
    proxy_pass http://coin-backend:8080/api/;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
```

> **운영 환경(`nginx.ssl.conf`)**: `proxy_pass`의 `http://backend:8080` → `http://127.0.0.1:8080` / `http://127.0.0.1:8081` 으로 변경 (host 네트워크 모드 유지)

#### ② Docker Compose 분리

**수정 위치**: `docker-compose.yml` / `docker-compose.prod.yml`

**변경 이유**: `crypto-backend` 단일 서비스를 `coin-backend` + `stock-backend` 두 서비스로 분리.

```yaml
# 변경 전 (현재 docker-compose.yml)
services:
  backend:
    build: ./backend
    container_name: crypto-backend
    ports:
      - "8080:8080"

# 변경 후
services:
  coin-backend:
    build:
      context: ./backend
      args:
        MODULE: coin          # Dockerfile에서 활성화할 모듈 선택
    container_name: coin-backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=coin
    env_file:
      - .env.development

  stock-backend:
    build:
      context: ./backend
      args:
        MODULE: stock
    container_name: stock-backend
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=stock
      - SERVER_PORT=8081
    env_file:
      - .env.development
    depends_on:
      - mysql
      - redis
```

> **절대경로 사용 금지**: `context: ./backend` 처럼 항상 상대경로 사용 (현재 규칙 유지)

#### ③ Spring Profile 분리 (`application-coin.yml` / `application-stock.yml`)

**신규 생성 위치**: `backend/src/main/resources/`

```
backend/src/main/resources/
├── application.yml           (공통 설정 - 현재 파일 유지)
├── application-coin.yml      ← 신규: 코인봇 전용 스케줄러/설정 활성화
└── application-stock.yml     ← 신규: 주식봇 전용 스케줄러/설정 활성화
```

```yaml
# application-coin.yml
spring:
  application:
    name: coin-trading-backend
scheduling:
  enabled: true
  stock-scheduler: false   # 주식 스케줄러 비활성화
  coin-scheduler: true

# application-stock.yml
spring:
  application:
    name: stock-trading-backend
server:
  port: 8081
scheduling:
  enabled: true
  coin-scheduler: false    # 코인 스케줄러 비활성화
  stock-scheduler: true
```

#### ④ JWT Secret 공유 설정

**변경 이유**: stock-backend에서도 coin-backend가 발급한 JWT를 검증해야 하므로 동일한 Secret 사용.

```yaml
# application.yml (공통, 변경 없음)
jwt:
  secret: ${JWT_SECRET}    # 환경변수로 두 컨테이너에 동일 값 주입
  expiration: 1800000
```

```env
# .env.development / .env.production (기존 파일에 항목 추가)
JWT_SECRET=기존값_그대로_유지
```

> coin-backend와 stock-backend의 `.env` 파일에 동일한 `JWT_SECRET`이 주입되므로 단일 로그인으로 양쪽 접근 가능.

#### ⑤ Redis 키 prefix 컨벤션 정리

**변경 이유**: 공유 Redis에서 코인/주식 캐시 키 충돌 방지.

| 현재 키 패턴 | 변경 후 키 패턴 | 적용 서비스 |
|------------|--------------|-----------|
| `bot:enabled:{userId}` | `coin:bot:enabled:{userId}` | RiskManagementService |
| `daily:limit:{userId}` | `coin:daily:limit:{userId}` | RiskManagementService |
| `stock:bot:enabled:{userId}` | 변경 없음 (이미 prefix 있음) | StockRiskManagementService |
| `stock:daily:{userId}` | 변경 없음 (이미 prefix 있음) | StockRiskManagementService |

> **참고**: GitHub 코드 확인 결과 `StockRiskManagementService`는 이미 `stock:` prefix를 사용 중. 코인 쪽(`RiskManagementService`)만 prefix 추가 필요.

#### ⑥ init.sql 이관 호환성 확인

**변경 없음** - 현재 `docker/mysql/init.sql`은 이미 다음 조건을 만족하므로 신규 서버 이관 시 그대로 사용 가능:

- `CREATE TABLE IF NOT EXISTS` 사용 (멱등성 보장)
- 절대경로 미사용
- Phase 1(코인) + Phase 2(주식) 테이블 모두 포함
- `FOREIGN KEY` 선언 순서 올바름 (참조 대상 테이블이 먼저 생성됨)

> **Phase 3 추가 시**: `init.sql` 하단에 `-- Phase 3: 달러 거래 테이블` 섹션 추가하는 방식으로 확장.

---

## 🌐 Phase 3: 달러(외환) 거래 확장 준비 사항

> **시작 조건**: v3.0 컨테이너 분리 완료 후  
> **목표**: `dollar-backend`(:8082) 컨테이너 추가만으로 Phase 3 온보딩 가능한 구조

### Phase 3 추가 시 작업 범위 (예상)

| 항목 | 내용 | 재사용 가능 여부 |
|------|------|----------------|
| DB 테이블 | `dollar_transactions`, `dollar_trading_settings`, `fx_price_history` | init.sql 하단 추가 |
| Backend | `dollar-backend` 컨테이너 신규 추가 | `stock-backend` 구조 복사 후 FX API 교체 |
| 스케줄러 | 외환시장 거래 시간 기반 (24시간 단, 주말 휴장) | `StockTradingScheduler` 참고 |
| 신호 감지 | MA/RSI/BB 동일 지표 사용 | `StockSignalDetectorService` 재사용 |
| 프론트엔드 | `DollarDashboardView`, `DollarBotMonitorView` 등 | `Stock*View.vue` 복사 후 레이블 수정 |
| Nginx | `/api/dollar/**` → `dollar-backend:8082` 라우팅 추가 | nginx.conf 1개 블록 추가 |
| Docker | `docker-compose.yml`에 `dollar-backend` 서비스 추가 | coin-backend 블록 복사 후 포트 변경 |
| 외환 API 후보 | OANDA API, 한국투자증권 해외주식 API | 별도 검토 필요 |

### Phase 3 신규 생성 예정 파일 목록

```
backend/src/main/resources/application-dollar.yml   ← 신규
backend/src/main/java/com/investment/
└── service/dollar/
    ├── DollarTradingBotService.java    ← StockTradingBotService 구조 재사용
    ├── DollarSignalDetectorService.java
    ├── DollarRiskManagementService.java
    └── FxApiService.java               ← 외환 API 연동 (신규)
frontend/src/views/
    ├── DollarDashboardView.vue         ← StockDashboardView.vue 복사 후 수정
    └── DollarBotMonitorView.vue        ← StockBotMonitorView.vue 복사 후 수정
```

---

## 📋 v2.1 / v3.0 마이그레이션 체크리스트

신규 서버 이관 시 사용하는 체크리스트 (절대경로 없음 보장).

```
[ ] 1. git clone https://github.com/kros235/crypto-trading-system.git
[ ] 2. .env.development / .env.production 파일 환경변수 설정
        (JWT_SECRET, DB_PASSWORD, KIS_APP_KEY 등)
[ ] 3. docker compose -f docker-compose.prod.yml up -d mysql redis
        → MySQL 초기화: docker/mysql/init.sql 자동 실행 확인
[ ] 4. docker compose -f docker-compose.prod.yml up -d coin-backend
[ ] 5. docker compose -f docker-compose.prod.yml up -d stock-backend  (v3.0 이후)
[ ] 6. docker compose -f docker-compose.prod.yml up -d frontend
[ ] 7. DuckDNS IP 업데이트 + Let's Encrypt SSL 재발급
        (scripts/ 내 ssl 관련 스크립트 활용)
[ ] 8. /api/health 헬스체크 확인
[ ] 9. Postman으로 로그인 → 코인봇/주식봇 API 동작 확인
[ ] 10. 관리자 계정으로 휴장일 데이터 초기 등록 (stock_holidays)
```

---

# 📊 4부: 전체 프로젝트 진행률

| 구분 | 상태 | 진행률 |
|------|------|--------|
| Phase 1 (암호화폐) | ✅ 완료 | 100% |
| Phase 2-1 (기반 구축) | ✅ 완료 | ~100% (Day 48~55 완료, 기반 구축 단계 마무리) |
| Phase 2-2 (핵심 기능) | 🔄 진행 중 | ~88% (Day 53~60 완료 **+ Day 60 디자인 시스템 통일**) |
| Phase 2-3 (고도화) | ⏳ 예정 | 0% |
| Phase 2-4 (안정화) | ⏳ 예정 | 0% |
| **전체 프로젝트** | - | **~80%** (Phase 1 완료, Phase 2 Day 60 완료) |
| v2.1 패키지 리팩토링 | ⏳ 예정 (Day 66 이후) | 0% |
| v3.0 Backend 컨테이너 분리 | ⏳ 예정 (v2.1 완료 이후) | 0% |
| Phase 3 달러 거래 | ⏳ 미정 (v3.0 완료 이후) | 0% |