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
│       │       └── StockSettingController.java              ⭐ (Day 51)
│       ├── dto/                 (30+ DTO 클래스)
│       │   ├── AuthResponse, DashboardStatsDTO, LoginRequest, SignupRequest
│       │   ├── TradingSettingDTO, TransactionDTO, UserInfoDTO
│       │   ├── admin/           (AdminUserDTO, MonitoringDTO, SystemStatsDTO)
│       │   ├── backtest/        (BacktestRequestDTO, BacktestResultDTO)
│       │   ├── bot/             (TradingSignalDTO)
│       │   ├── common/          (ApiResponse, PageResponse)
│       │   ├── indicator/       (IndicatorResultDTO)
│       │   ├── ⭐ kis/           (KisTokenDTO, KisQuoteDTO, KisAccountDTO, KisOrderDTO) (Day 50)
│       │   ├── ⭐ stock/         (StockInfoDTO, StockTradingSettingDTO) (Day 51, Day 52 BigDecimal 타입 수정)
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
│       │   ├── ⭐ StockInfo, StockTradingSetting, StockTransaction (Day 49)
│       │   ├── ⭐ StockPriceHistory, StockDailySummary, MarketHoliday (Day 49)
│       ├── exception/           (7개)
│       ├── filter/              (3개: JWT, RateLimit, RequestLogging)
│       ├── repository/          (15개)
│       ├── scheduler/           (TradingScheduler.java)
│       ├── service/             (25개 서비스)
│       │   ├── ... (기존 23개)
│       │   ├── KisApiService.java                   (Day 50, Day 51 정리)
│       │   ├── KisTokenService.java                 (Day 50)
│       │   ├── StockInfoService.java                ⭐ (Day 51)
│       │   └── StockSettingService.java             ⭐ (Day 51)
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
│       ├── router/index.ts                     (Day 52 /stock-settings 라우트 추가)
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
│           ├── HelpView.vue                    (Day 52 Phase 2 FAQ 추가)
│           ├── HoldingsView.vue
│           ├── LoginView.vue
│           ├── NewsView.vue
│           ├── ProfileView.vue                 (Day 52 KIS API 키 등록 UI 추가)
│           ├── ReleaseNotesView.vue
│           ├── SignupView.vue
│           ├── ⭐ StockTradingSettingsView.vue  (Day 52 신규, 주식 거래 설정)
│           ├── TradingSettingsView.vue
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
| 12 | daily_asset_snapshot | 일별 자산 스냅샷 | 차트용 |

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

주식 거래 (공사중) (v-list-group "stock", 전부 disabled)
├── 대시보드
├── 보유 주식 자산
├── 주식 종목 목록
├── 주식 거래 내역
└── 주식 거래 설정

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
| **53** | ⑥ StockTechnicalIndicatorService + StockSignalDetectorService | 기술지표/신호 감지 (Phase 1 재사용) | ⏳ 예정 |
| **54** | ⑦ StockRiskManagementService + MarketHolidayService | 리스크관리, 휴장일 | ⏳ 예정 |
| **55** | ⑧ StockTradingBotService (자동매매 핵심) | 매수/매도/라운드로빈 | ⏳ 예정 |
| **56** | ⑨ StockTradingScheduler + 장 시작/마감 알림 | 스케줄러, 알림 | ⏳ 예정 |
| **57** | ⑩ StockTransactionService + API + 프론트엔드 | 거래 내역 | ⏳ 예정 |
| **58** | ⑪ StockDashboardView (주식 대시보드 프론트엔드) | 대시보드 | ⏳ 예정 |
| **59** | ⑫ StockHoldingsView + StockListView | 보유자산, 종목목록 | ⏳ 예정 |
| **60** | ⑬ StockBotMonitorView | 봇 모니터링 | ⏳ 예정 |
| **61** | ⑭ StockBacktestService + StockBacktestView | 백테스팅 | ⏳ 예정 |
| **62** | ⑮ StockProfitService + 수익분석 + 일일리포트 | 수익 분석 | ⏳ 예정 |
| **63** | ⑯ AdminHolidayView + 관리자 통합 | 관리자 기능 | ⏳ 예정 |
| **64** | ⑰ 사이드바 활성화 + SecurityConfig 업데이트 + 통합 테스트 | 통합 | ⏳ 예정 |
| **65** | ⑱ 최종 테스트 + 문서화 + v2.0 릴리즈 + (v2.1 리팩토링 계획 수립: com.cryptotrading → com.investment 패키지 리네이밍, controller/service/entity 서브패키지 crypto/stock/common 분리) | 배포 + 리팩토링 계획서 | ⏳ 예정 |

---

# 📊 4부: 전체 프로젝트 진행률

| 구분 | 상태 | 진행률 |
|------|------|--------|
| Phase 1 (암호화폐) | ✅ 완료 | 100% |
| Phase 2-1 (기반 구축) | 🔄 진행 중 | ~60% (Day 48 카테고리 + Day 49 DB/Entity/Repository + Day 50 KIS API 연동 + Day 51 종목/설정 CRUD API + Day 52 주식 거래 설정 프론트엔드 완료) |
| Phase 2-2 (핵심 기능) | ⏳ 예정 | 0% |
| Phase 2-3 (고도화) | ⏳ 예정 | 0% |
| Phase 2-4 (안정화) | ⏳ 예정 | 0% |
| **전체 프로젝트** | - | **~63%** (Phase 1 완료, Phase 2 Day 52 완료) |
