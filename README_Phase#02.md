# 주식/ETF 자동매매 시스템 (Phase 2)

> ⚠️ **문서 안내**: 이 문서는 Phase 2 초기 설계 시점(Day 38~65 체계)의 계획서입니다.
> 실제 작업은 이후 `Phase2_Implementation_Plan.md`(Day 48~66 체계)를 기준으로 진행되었으며,
> **일자별 상세 진행 상황은 `Phase2_Implementation_Plan.md`가 최신 기준 문서입니다.**
> 이 문서는 아키텍처/DB 설계/API 명세 등 설계 원본 레퍼런스 용도로 유지합니다.

## 📋 프로젝트 개요
- **목적**: 한국투자증권 KIS API를 활용한 개인용 주식/ETF 자동매매 시스템
- **규모**: 5명 사용자
- **핵심 가치**: 안정적인 수익 창출 + 최고 수준의 보안
- **개발 기간**: 8주 (Phase 2-1: 2주, Phase 2-2: 3주, Phase 2-3: 2주, Phase 2-4: 1주)
- **코드 재사용률**: Phase 1 (암호화폐) 대비 약 85~90%

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
- Oracle Cloud ARM64

### External APIs
- **한국투자증권 KIS API** (REST + WebSocket)
- Groq API (뉴스 분석 - Llama 3.3 70B)
- Discord Bot (알림)
- Email SMTP (알림)

---

## 📌 Phase 2 주요 특징

### 거래 대상
| 구분 | 내용 |
|------|------|
| 시장 | 한국 증시 (KRX, KOSDAQ) |
| 주요 종목 | 국내 상장 해외지수 레버리지 ETF |
| 대표 종목 | TIGER 미국나스닥100레버리지(합성) [409820] |
| | KODEX 미국나스닥100레버리지(합성H) [409810] |
| 거래시간 | 정규장 09:00~15:30 (KST) |

### Phase 1과의 차이점
| 항목 | Phase 1 (코인) | Phase 2 (주식) |
|------|---------------|---------------|
| 매수 기준 하락률 | -6.0% | -3.0% |
| 목표 수익률 | +4.0% | +2.5% |
| 손절매 기준 | -8.0% | -5.0% |
| 트레일링 스톱 | -4.0% | -2.5% |
| 거래량 급증 기준 | 140% | 120% |
| RSI 매수 신호 | 32 이하 | 35 이하 |
| RSI 매도 신호 | 68 이상 | 65 이상 |
| 거래 시간 | 24시간 | 09:00~15:30 |
| 휴장일 | 없음 | 주말, 공휴일 |

---

## 🎯 거래 전략

### 1. 기준가 산정 방식
- 기본: 20일 이동평균선 기준
- 보조지표: RSI(35 이하 매수신호), 볼린저밴드 하단 접촉
- 거래량: 평균 거래량 대비 120% 이상일 때 매수 활성화
- 사용자 설정: 7일/14일/30일 기간 선택 가능

### 2. 매수 조건
- 기준가 대비 사용자 설정 % 이하 하락 시 매수
- 종목당 최대 보유 건수 제한 (기본 3건, 사용자 설정 가능)
- 라운드로빈 방식으로 종목 분산 매수

#### 📌 라운드로빈 매수 방식 상세

**[정의]**
매수 신호가 발생한 종목들에게 남은 한도를 균등 분배하여 분산 투자하는 방식
(Phase 1 암호화폐와 동일 로직 재사용)

**[처리 순서]**
```
1단계: 매수 후보 수집
  - 설정된 종목들 중 매수 신호 발생 종목 필터링
  - 리스크 사전 체크 (보유 건수, 비중 제한) 통과 종목만 후보로 선정

2단계: 균등 분배 계산
  - 분배 금액 = 남은 일일 한도 ÷ 매수 후보 수
  - 각 후보별 최대 매수 가능 금액 계산 (비중 제한 반영)

3단계: 최소 금액 체크 및 우선순위 선정
  - 분배 금액 ≥ 최소 매수 금액 → 전체 후보 유지
  - 분배 금액 < 최소 매수 금액 → 신호 강도 순 정렬 후 상위 N개만 선정
    * 신호 강도: STRONG(4개 조건 충족) > MEDIUM(3개) > WEAK(2개)
    * 동일 강도 시 이격도(dropRate)가 큰 순서로 우선순위 부여

4단계: 매수 실행
  - 실제 매수 금액 = min(분배 금액, 비중 제한 잔여)
  - 비중 제한으로 남은 금액은 다음 후보에 재분배
```

**[예시: 남은 한도 100만원, TIGER/KODEX 매수 신호 발생]**
| 방식 | TIGER | KODEX | 특징 |
|------|-------|-------|------|
| 순차 방식 | 100만원 | 0원 | 집중 투자 |
| **라운드로빈** | **50만원** | **50만원** | **분산 투자** |

### 3. 매도 조건
- 목표 수익률 달성 시 자동 매도 (기본 +2.5%)
- 손절매 기능 (-5% 도달 시 강제 매도)
- 트레일링 스톱 옵션 (최고가 대비 -2.5% 하락 시 매도)

### 4. 리스크 관리
- 일일 최대 거래 한도 설정
- 총 투자금 대비 종목별 최대 투자 비율 제한 (20%)
- 급격한 시장 변동 시 거래 일시 중단 기능
- **레버리지 ETF 보유기간 제한**: 최대 20거래일 (decay 방지)

### 5. 레버리지 ETF 주의사항
- 장기 보유 시 복리 효과로 인한 가치 침식(decay) 발생 가능
- 권장 보유 기간: 최대 20거래일
- 환노출형(TIGER) vs 환헤지형(KODEX) 선택 고려

---

## 💾 데이터베이스 설계

### Phase 2 전용 테이블

```sql
-- 1. 주식/ETF 정보 테이블
CREATE TABLE stock_info (
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
CREATE TABLE stock_trading_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL COMMENT '사용자 ID',
    stock_codes JSON NOT NULL COMMENT '거래 종목 코드 목록',
    base_period INT DEFAULT 20 COMMENT '이동평균선 기간',
    buy_threshold_pct DECIMAL(5,2) DEFAULT -3.0 COMMENT '매수 기준 하락률 (%)',
    sell_target_pct DECIMAL(5,2) DEFAULT 2.5 COMMENT '목표 수익률 (%)',
    stop_loss_pct DECIMAL(5,2) DEFAULT -5.0 COMMENT '손절매 기준 (%)',
    max_holdings_per_stock INT DEFAULT 3 COMMENT '종목당 최대 보유 건수',
    daily_limit_amount BIGINT DEFAULT 1000000 COMMENT '일일 거래 한도 (원)',
    max_holding_days INT DEFAULT 20 COMMENT '최대 보유 기간 (거래일)',
    use_trailing_stop BOOLEAN DEFAULT TRUE COMMENT '트레일링 스톱 사용',
    trailing_stop_pct DECIMAL(5,2) DEFAULT -2.5 COMMENT '트레일링 스톱 비율 (%)',
    kis_app_key_encrypted VARCHAR(500) COMMENT 'KIS API App Key (암호화)',
    kis_app_secret_encrypted VARCHAR(500) COMMENT 'KIS API App Secret (암호화)',
    kis_account_no VARCHAR(50) COMMENT 'KIS 계좌번호',
    -- 기술적 지표 설정
    rsi_period INT DEFAULT 14 COMMENT 'RSI 계산 기간',
    rsi_buy_threshold INT DEFAULT 35 COMMENT 'RSI 매수 신호 임계값',
    rsi_sell_threshold INT DEFAULT 65 COMMENT 'RSI 매도 신호 임계값',
    bb_period INT DEFAULT 20 COMMENT '볼린저밴드 기간',
    bb_multiplier INT DEFAULT 2 COMMENT '볼린저밴드 표준편차 승수',
    volume_threshold INT DEFAULT 120 COMMENT '거래량 급증 기준 (%)',
    -- 리스크 관리 설정
    daily_trade_limit_pct INT DEFAULT 20 COMMENT '일일 거래 한도 (%)',
    max_position_pct INT DEFAULT 25 COMMENT '단일 종목 최대 비중 (%)',
    daily_stop_loss_pct INT DEFAULT -5 COMMENT '긴급 정지 조건 (%)',
    -- 시장 보호 설정
    use_market_trend_filter BOOLEAN DEFAULT FALSE COMMENT '시장 추세 필터 사용',
    cumulative_loss_limit_pct INT DEFAULT -10 COMMENT '누적 손실 한도 (%)',
    consecutive_stop_loss_limit INT DEFAULT 3 COMMENT '연속 손절 제한 횟수',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stock_settings_user (user_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 주식 거래 이력 테이블
CREATE TABLE stock_transactions (
    transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL COMMENT '사용자 ID',
    stock_code VARCHAR(20) NOT NULL COMMENT '종목코드',
    type ENUM('BUY', 'SELL') NOT NULL COMMENT '거래 유형',
    quantity INT NOT NULL COMMENT '수량 (주)',
    price DECIMAL(15,2) NOT NULL COMMENT '체결가',
    fee DECIMAL(15,2) DEFAULT 0 COMMENT '수수료',
    total_amount DECIMAL(15,2) NOT NULL COMMENT '총 거래금액',
    exchange_rate DECIMAL(10,4) COMMENT '환율 (환노출형 ETF용)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '매수 시간',
    sold_at TIMESTAMP NULL COMMENT '매도 시간',
    sold_price DECIMAL(15,2) COMMENT '매도가',
    profit_loss DECIMAL(15,2) COMMENT '실현 손익',
    target_sell_price DECIMAL(15,2) COMMENT '목표 매도가',
    stop_loss_price DECIMAL(15,2) COMMENT '손절매가',
    highest_price DECIMAL(15,2) COMMENT '보유 중 최고가 (트레일링 스톱용)',
    holding_days INT DEFAULT 0 COMMENT '보유 기간 (거래일)',
    status ENUM('HOLDING', 'SOLD', 'STOP_LOSS', 'EXPIRED') DEFAULT 'HOLDING',
    memo TEXT COMMENT '메모',
    INDEX idx_stock_tx_user_created (user_id, created_at),
    INDEX idx_stock_tx_stock_status (stock_code, status),
    INDEX idx_stock_tx_status (status),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 주식 가격 이력 테이블
CREATE TABLE stock_price_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL COMMENT '종목코드',
    price DECIMAL(15,2) NOT NULL COMMENT '현재가',
    volume BIGINT NOT NULL COMMENT '거래량',
    timestamp TIMESTAMP NOT NULL COMMENT '시간',
    ma20 DECIMAL(15,2) COMMENT '20일 이동평균',
    rsi DECIMAL(5,2) COMMENT 'RSI 값',
    bb_upper DECIMAL(15,2) COMMENT '볼린저밴드 상단',
    bb_lower DECIMAL(15,2) COMMENT '볼린저밴드 하단',
    INDEX idx_stock_price_symbol_time (stock_code, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 휴장일 캘린더 테이블
CREATE TABLE market_holidays (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    holiday_date DATE NOT NULL COMMENT '휴장일',
    holiday_name VARCHAR(100) COMMENT '휴장 사유',
    year INT NOT NULL COMMENT '연도',
    UNIQUE KEY uk_holiday_date (holiday_date),
    INDEX idx_holiday_year (year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. 주식 일간 집계 테이블
CREATE TABLE stock_daily_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    date DATE NOT NULL,
    total_profit DECIMAL(15,2) DEFAULT 0 COMMENT '총 손익',
    profit_rate DECIMAL(5,2) DEFAULT 0 COMMENT '수익률 (%)',
    buy_count INT DEFAULT 0 COMMENT '매수 건수',
    sell_count INT DEFAULT 0 COMMENT '매도 건수',
    total_investment DECIMAL(15,2) DEFAULT 0 COMMENT '총 투자금액',
    UNIQUE KEY uk_stock_daily_user_date (user_id, date),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 인덱스 전략
```sql
-- stock_transactions 인덱스
CREATE INDEX idx_stock_tx_user_created ON stock_transactions(user_id, created_at);
CREATE INDEX idx_stock_tx_stock_status ON stock_transactions(stock_code, status);

-- stock_price_history 인덱스
CREATE INDEX idx_stock_price_symbol_time ON stock_price_history(stock_code, timestamp);

-- stock_daily_summary 인덱스
CREATE INDEX idx_stock_daily_user_date ON stock_daily_summary(user_id, date);
```

---

## 🌐 한국투자증권 KIS API 정보

### API 개요
| 항목 | 내용 |
|------|------|
| API 방식 | REST API + WebSocket |
| API 사용료 | **무료** |
| 거래 수수료 | 국내주식 0.0036396% |
| 모의투자 | 지원 (5억원 가상자금) |
| 개발자 포털 | https://apiportal.koreainvestment.com |
| GitHub 샘플 | https://github.com/koreainvestment/open-trading-api |

### API 제한
- 호출 제한: **초당 20건**
- 실시간 시세 WebSocket 연결 수 제한
- 3개월 미거래 시 API 서비스 자동 해지

### 주요 API 엔드포인트
| 구분 | 엔드포인트 | 설명 |
|------|----------|------|
| 인증 | /oauth2/tokenP | 접근 토큰 발급 |
| 시세 | /uapi/domestic-stock/v1/quotations/inquire-price | 현재가 조회 |
| 일봉 | /uapi/domestic-stock/v1/quotations/inquire-daily-price | 일봉 데이터 |
| 잔고 | /uapi/domestic-stock/v1/trading/inquire-balance | 계좌 잔고 조회 |
| 매수 | /uapi/domestic-stock/v1/trading/order-cash | 현금 매수 주문 |
| 매도 | /uapi/domestic-stock/v1/trading/order-cash | 현금 매도 주문 |

---

## 📱 알림 시스템

### 알림 종류
| 알림 | 설명 | 발송 시점 |
|------|------|----------|
| 매수/매도 체결 | 거래 체결 즉시 알림 | 실시간 |
| 일일 수익 리포트 | 하루 거래 요약 | 매일 15:35 |
| 시스템 오류 | 거래 실패, API 오류 | 즉시 |
| 거래 한도 도달 | 일일 한도 소진 | 즉시 |
| **보유기간 경고** | 레버리지 ETF 15일/20일 도달 | 해당 시점 |
| **장 시작/마감 알림** | 거래 시간 안내 | 08:50 / 15:20 |
| **휴장일 전일 알림** | 다음날 휴장 안내 | 전일 15:00 |

---

## 🌐 웹페이지 구성

### Phase 2 신규/수정 페이지
```
🌐 웹페이지 구성
├── 📄 공통 페이지
│   ├── 홈페이지 (대시보드) - Phase 1/2 탭 전환
│   ├── 로그인/2FA 인증
│   └── 회원가입
│
├── 👤 사용자 페이지
│   ├── 거래 현황 (실시간) - 주식 탭 추가
│   ├── 거래 이력 조회 - 주식 거래 이력
│   ├── 수익률 차트 - 주식 수익 분석
│   ├── 설정 관리
│   │   ├── 코인 거래 설정 [Phase 1]
│   │   └── ⭐ 주식 거래 설정 [Phase 2]
│   ├── 보안 설정
│   ├── 코인 뉴스 [Phase 1]
│   ├── 보유자산 (기간별/종목별 수익 분석)
│   └── ⭐ 주식/ETF 포트폴리오 [Phase 2]
│       ├── ETF 보유 현황
│       ├── 수익률 분석 (환율 영향 포함)
│       └── 보유기간 모니터링 (레버리지 ETF decay 경고)
│
└── 🔧 관리자 페이지
    ├── 사용자 관리
    ├── 시스템 모니터링
    ├── 거래 통계
    └── ⭐ 휴장일 관리 [Phase 2]
```

---

## 📊 백테스팅 시스템

### Phase 2 백테스팅 설정
| 항목 | 값 |
|------|-----|
| 데이터 소스 | KIS API 일봉 데이터 / Yahoo Finance |
| 테스트 기간 | 최대 1년 |
| 수수료 반영 | 0.015% (매수/매도 각각) |
| 슬리피지 | 0.05% |

### 분석 지표
- 총 수익률, 승률, MDD
- 샤프 비율, 평균 보유기간
- 월별/분기별 수익률 분포

---

## ⚠️ Phase 2 주요 고려사항

### 법적/세금
| 구분 | 내용 |
|------|------|
| 국내상장 ETF 매매차익 | 배당소득세 15.4% |
| ISA 계좌 활용 시 | 200만원 비과세, 초과분 9.9% 분리과세 |
| 해외주식 직접투자 시 | 양도소득세 22% (250만원 초과분) |

### 거래 시간
| 구분 | 시간 (KST) |
|------|----------|
| 정규장 | 09:00 ~ 15:30 |
| 시간외 거래 (동시호가) | 08:30~09:00, 15:40~16:00 |
| 휴장일 | 주말, 공휴일, 임시휴장일 |

### 레버리지 ETF 리스크
- **변동성 끌림(Volatility Drag)**: 횡보장에서 손실 누적
- **장기 보유 비권장**: 최대 20거래일 내 청산 권장
- **환노출형 vs 환헤지형**: 수익률 차이 최대 20%p

---

## 📅 개발 진행 상황

### Phase 2 일정 개요
| 구분 | 기간 | 주요 작업 |
|------|------|----------|
| Phase 2-1 (기반 구축) | Day 38~44 (2주) | KIS API 연동, DB 테이블, 모의투자 |
| Phase 2-2 (핵심 기능) | Day 45~53 (3주) | 매수/매도, 스케줄러, 지표 계산 |
| Phase 2-3 (고도화) | Day 54~60 (2주) | 대시보드, ETF 기능, 백테스팅 |
| Phase 2-4 (안정화) | Day 61~65 (1주) | 통합 테스트, 문서화, 배포 |

---

### 📅 Day 38 - KIS API 연동 기반 구축
**목표**: 한국투자증권 KIS API 연동을 위한 기반 구축

**작업 내용**:
- [ ] KIS API 개발자 포털 계정 생성 및 앱 등록
- [ ] KIS API DTO 클래스 생성
  - KisTokenDTO: 토큰 응답
  - KisQuoteDTO: 시세 정보
  - KisAccountDTO: 계좌 잔고
  - KisOrderDTO: 주문 정보
- [ ] KisApiService 기본 구조 구현
  - 토큰 발급 및 갱신 로직
  - WebClient 설정 (KIS API용)
- [ ] application.yml KIS 설정 추가
- [ ] 환경변수 추가 (.env.development, .env.production)

**생성할 파일**:
```
backend/src/main/java/com/cryptotrading/
├── dto/kis/
│   ├── KisTokenDTO.java
│   ├── KisQuoteDTO.java
│   ├── KisAccountDTO.java
│   └── KisOrderDTO.java
├── service/
│   └── KisApiService.java
└── config/
    └── KisApiConfig.java
```

**API 엔드포인트 (테스트용)**:
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /api/kis/test/token | 토큰 발급 테스트 |
| GET | /api/kis/test/quote/{stockCode} | 시세 조회 테스트 |

---

### 📅 Day 39 - KIS API 시세 조회 구현
**목표**: KIS API를 통한 주식 시세 및 일봉 데이터 조회

**작업 내용**:
- [ ] KisApiService 시세 조회 메서드 구현
  - getCurrentPrice(): 현재가 조회
  - getDailyCandles(): 일봉 데이터 조회
  - getMultiplePrices(): 다중 종목 시세 조회
- [ ] 시세 데이터 캐싱 (Redis)
- [ ] API 호출 재시도 로직 (3회, 지수 백오프)
- [ ] Rate Limit 처리 (초당 20회 제한)

**테스트 항목**:
- [ ] TIGER 나스닥100레버리지 (409820) 시세 조회
- [ ] KODEX 나스닥100레버리지 (409810) 시세 조회
- [ ] 일봉 데이터 200일 조회
- [ ] 캐싱 동작 확인

---

### 📅 Day 40 - 주식 정보 및 DB 테이블 구축
**목표**: Phase 2 전용 DB 테이블 생성 및 주식 정보 관리

**작업 내용**:
- [ ] init.sql에 Phase 2 테이블 추가
  - stock_info (주식/ETF 정보)
  - stock_trading_settings (거래 설정)
  - stock_transactions (거래 이력)
  - stock_price_history (가격 이력)
  - market_holidays (휴장일 캘린더)
  - stock_daily_summary (일간 집계)
- [ ] Entity 클래스 생성
- [ ] Repository 인터페이스 생성
- [ ] 기본 ETF 데이터 초기화 (TIGER, KODEX 등)

**생성할 파일**:
```
backend/src/main/java/com/cryptotrading/
├── entity/
│   ├── StockInfo.java
│   ├── StockTradingSetting.java
│   ├── StockTransaction.java
│   ├── StockPriceHistory.java
│   ├── MarketHoliday.java
│   └── StockDailySummary.java
└── repository/
    ├── StockInfoRepository.java
    ├── StockTradingSettingRepository.java
    ├── StockTransactionRepository.java
    ├── StockPriceHistoryRepository.java
    ├── MarketHolidayRepository.java
    └── StockDailySummaryRepository.java
```

---

### 📅 Day 41 - KIS API 주문 실행 구현
**목표**: KIS API를 통한 매수/매도 주문 실행

**작업 내용**:
- [ ] KisApiService 주문 메서드 구현
  - placeBuyOrder(): 매수 주문
  - placeSellOrder(): 매도 주문
  - getOrderStatus(): 주문 상태 조회
  - cancelOrder(): 주문 취소
- [ ] 계좌 잔고 조회 구현
  - getBalance(): 예수금 조회
  - getHoldings(): 보유 종목 조회
- [ ] 모의투자 환경 설정 및 테스트

**테스트 항목**:
- [ ] 모의투자 매수 주문
- [ ] 모의투자 매도 주문
- [ ] 잔고 조회
- [ ] 보유 종목 조회

---

### 📅 Day 42 - 주식 거래 설정 API 구현
**목표**: 주식 거래 설정 CRUD API 구현

**작업 내용**:
- [ ] StockTradingSettingDTO 생성 (Validation 포함)
- [ ] StockTradingSettingService 구현
- [ ] StockTradingSettingController 구현
- [ ] KIS API 키 암호화 저장 로직
- [ ] SecurityConfig 권한 설정

**API 엔드포인트**:
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/stock/trading-settings | 설정 조회 |
| POST | /api/stock/trading-settings | 설정 생성 |
| PUT | /api/stock/trading-settings | 설정 수정 |
| DELETE | /api/stock/trading-settings | 설정 삭제 |
| POST | /api/stock/api-keys | KIS API 키 등록 |
| DELETE | /api/stock/api-keys | KIS API 키 삭제 |

---

### 📅 Day 43 - 휴장일 캘린더 및 거래 시간 관리
**목표**: 휴장일 관리 및 거래 시간 스케줄러 구현

**작업 내용**:
- [ ] MarketHolidayService 구현
  - 휴장일 CRUD
  - isMarketOpen(): 시장 개장 여부 확인
  - getNextTradingDay(): 다음 거래일 조회
- [ ] 2026년 휴장일 데이터 초기화
- [ ] 거래 시간 스케줄러 구현
  - 장 시작 준비 (08:50)
  - 정규장 거래 (09:00~15:30)
  - 장 마감 처리 (15:35)
- [ ] 휴장일 전일 알림 구현

**생성할 파일**:
```
backend/src/main/java/com/cryptotrading/
├── service/
│   └── MarketHolidayService.java
├── scheduler/
│   └── StockTradingScheduler.java
└── controller/
    └── MarketHolidayController.java (관리자용)
```

---

### 📅 Day 44 - 모의투자 환경 구성 및 통합 테스트
**목표**: 모의투자 환경 전체 통합 테스트

**작업 내용**:
- [ ] 모의투자 계좌 설정 가이드 문서
- [ ] API 연동 전체 흐름 테스트
  - 토큰 발급 → 시세 조회 → 주문 실행 → 체결 확인
- [ ] 에러 핸들링 강화
  - KisApiException 생성
  - 에러 코드 추가 (ErrorCode.java)
- [ ] Phase 2-1 완료 보고서 작성

**테스트 시나리오**:
1. 로그인 → KIS API 키 등록
2. 거래 설정 생성 (TIGER 나스닥100레버리지)
3. 모의투자 매수 주문 실행
4. 잔고 확인
5. 모의투자 매도 주문 실행
6. 거래 이력 확인

---

### 📅 Day 45 - 주식 기술적 지표 계산 서비스
**목표**: Phase 1 코드 재사용하여 주식용 기술적 지표 계산

**작업 내용**:
- [ ] StockTechnicalIndicatorService 구현
  - 이동평균선 (MA7, MA14, MA20, MA30)
  - RSI (14일) 계산
  - 볼린저 밴드 (20일, 2 표준편차)
  - 거래량 분석
- [ ] Phase 1 TechnicalIndicatorService 코드 재사용
- [ ] 주식용 파라미터 기본값 적용 (RSI 35/65 등)

**생성할 파일**:
```
backend/src/main/java/com/cryptotrading/
├── service/
│   └── StockTechnicalIndicatorService.java
└── dto/indicator/
    └── StockIndicatorResultDTO.java
```

---

### 📅 Day 46 - 주식 거래 신호 감지 서비스
**목표**: 매수/매도 신호 감지 로직 구현

**작업 내용**:
- [ ] StockSignalDetectorService 구현
  - 매수 신호: MA 하락률 + RSI + 볼린저밴드 + 거래량 조합
  - 매도 신호: 목표 수익률 도달
  - 손절매 신호: 설정 기준 도달
  - 트레일링 스톱: 최고가 대비 하락률
- [ ] 레버리지 ETF 보유기간 체크 로직 추가
- [ ] Phase 1 SignalDetectorService 코드 재사용

**생성할 파일**:
```
backend/src/main/java/com/cryptotrading/
├── service/
│   └── StockSignalDetectorService.java
└── dto/bot/
    └── StockTradingSignalDTO.java
```

---

### 📅 Day 47 - 주식 리스크 관리 서비스
**목표**: 리스크 관리 로직 구현

**작업 내용**:
- [ ] StockRiskManagementService 구현
  - 일일 거래 한도 체크
  - 종목당 최대 보유 건수 제한
  - 단일 종목 최대 비중 체크
  - 누적 손실률 긴급정지
  - 연속 손절 제한
- [ ] 레버리지 ETF 특화 리스크 관리
  - 보유기간 20일 초과 경고
  - 강제 청산 로직
- [ ] Phase 1 RiskManagementService 코드 재사용

---

### 📅 Day 48 - 주식 자동매매 봇 서비스
**목표**: 자동매매 봇 핵심 로직 구현

**작업 내용**:
- [ ] StockTradingBotService 구현
  - executeAutoTrading(): 자동매매 실행
  - processBuySignals(): 매수 신호 처리
  - processSellSignals(): 매도 신호 처리
  - updateHighestPrice(): 최고가 업데이트
- [ ] 거래 시간 체크 로직 (09:00~15:30)
- [ ] 휴장일 스킵 로직
- [ ] Phase 1 TradingBotService 코드 재사용

**생성할 파일**:
```
backend/src/main/java/com/cryptotrading/
└── service/
    └── StockTradingBotService.java
```

---

### 📅 Day 49 - 주식 거래 스케줄러 구현
**목표**: 정규장 시간에 맞춘 스케줄러 구현

**작업 내용**:
- [ ] StockTradingScheduler 스케줄 설정
  - 08:50: 장 시작 알림
  - 09:00~15:25: 5분 간격 자동매매 실행
  - 15:30: 장 마감 처리
  - 15:35: 일일 리포트 발송
- [ ] 휴장일 스킵 로직
- [ ] 보유기간 업데이트 (매일 장 마감 후)

```java
// 스케줄러 예시
@Scheduled(cron = "0 */5 9-15 * * MON-FRI", zone = "Asia/Seoul")
public void executeStockAutoTrading() {
    if (marketHolidayService.isHoliday(LocalDate.now())) {
        return; // 휴장일 스킵
    }
    // 자동매매 실행
}
```

---

### 📅 Day 50 - 주식 거래 알림 시스템
**목표**: 주식 거래 관련 알림 구현

**작업 내용**:
- [ ] StockNotificationService 구현
  - 매수/매도 체결 알림
  - 보유기간 경고 알림 (15일/20일)
  - 장 시작/마감 알림
  - 휴장일 전일 알림
- [ ] Discord DM 템플릿 (주식용)
- [ ] 이메일 템플릿 (주식용)
- [ ] 일일 리포트 (주식용)

---

### 📅 Day 51 - 주식 거래 내역 API
**목표**: 주식 거래 내역 조회 API 구현

**작업 내용**:
- [ ] StockTransactionDTO 생성
- [ ] StockTransactionService 구현
- [ ] StockTransactionController 구현
- [ ] 복합 조건 검색 (종목, 상태, 날짜 범위)
- [ ] 페이지네이션

**API 엔드포인트**:
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/stock/transactions | 거래 내역 조회 |
| GET | /api/stock/transactions/search | 거래 검색 |
| GET | /api/stock/transactions/holdings | 보유 종목 조회 |
| GET | /api/stock/transactions/{id} | 거래 상세 |
| POST | /api/stock/transactions/{id}/sell | 수동 매도 |

---

### 📅 Day 52 - 주식 봇 모니터링 API
**목표**: 봇 상태 및 기술적 지표 조회 API 구현

**작업 내용**:
- [ ] StockBotController 구현
- [ ] 봇 상태 조회 API
- [ ] 기술적 지표 조회 API
- [ ] 수동 자동매매 실행 API

**API 엔드포인트**:
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/stock/bot/status | 봇 상태 조회 |
| GET | /api/stock/bot/indicators/{stockCode} | 지표 조회 |
| POST | /api/stock/bot/execute | 수동 실행 |

---

### 📅 Day 53 - 주식 백테스팅 서비스
**목표**: 주식용 백테스팅 기능 구현

**작업 내용**:
- [ ] StockBacktestService 구현
  - 과거 데이터 시뮬레이션
  - 수수료 반영 (0.015%)
  - 슬리피지 반영 (0.05%)
- [ ] StockBacktestRequestDTO, StockBacktestResultDTO
- [ ] Phase 1 BacktestService 코드 재사용
- [ ] 레버리지 ETF 보유기간 제한 시뮬레이션

---

### 📅 Day 54 - 주식 거래 설정 페이지 (Frontend)
**목표**: 주식 거래 설정 UI 구현

**작업 내용**:
- [ ] StockTradingSettingsView.vue 생성
  - 거래 종목 선택 (ETF 목록)
  - 기술적 지표 설정
  - 매수/매도 조건 설정
  - 리스크 관리 설정
  - 보유기간 제한 설정
- [ ] KIS API 키 등록 UI
- [ ] 라우터 추가 (/stock/settings)
- [ ] 사이드바 메뉴 추가

---

### 📅 Day 55 - 주식 대시보드 탭 추가 (Frontend)
**목표**: 기존 대시보드에 주식 탭 추가

**작업 내용**:
- [ ] DashboardView.vue 수정
  - Phase 1 (코인) / Phase 2 (주식) 탭 전환
  - 주식용 통계 카드
  - 주식용 봇 상태 카드
  - 주식용 보유 현황
- [ ] API 연동 (주식 데이터)

---

### 📅 Day 56 - 주식 거래 내역 페이지 (Frontend)
**목표**: 주식 거래 내역 조회 UI 구현

**작업 내용**:
- [ ] StockTransactionHistoryView.vue 생성
  - 거래 내역 테이블
  - 검색/필터 기능
  - 수동 매도 버튼
- [ ] StockHoldingsView.vue 생성
  - 보유 종목 현황
  - 평가 손익 표시
  - 보유기간 표시

---

### 📅 Day 57 - 주식 포트폴리오 페이지 (Frontend)
**목표**: ETF 포트폴리오 분석 UI 구현

**작업 내용**:
- [ ] StockPortfolioView.vue 생성
  - ETF 보유 현황
  - 수익률 분석 (환율 영향 포함)
  - 보유기간 모니터링
  - 레버리지 ETF decay 경고
- [ ] 기간별/종목별 수익 분석 탭

---

### 📅 Day 58 - 주식 봇 모니터링 페이지 (Frontend)
**목표**: 주식 봇 모니터링 UI 구현

**작업 내용**:
- [ ] StockBotMonitorView.vue 생성
  - 봇 상태 카드
  - 기술적 지표 테이블
  - 수동 실행 버튼
  - 알림 테스트 버튼
- [ ] 장 운영 시간 표시

---

### 📅 Day 59 - 주식 백테스팅 페이지 (Frontend)
**목표**: 주식 백테스팅 UI 구현

**작업 내용**:
- [ ] StockBacktestView.vue 생성
  - 종목/기간/투자금 설정
  - 고급 설정 (기술적 지표, 리스크 관리)
  - 결과 요약 카드
  - 자산 변동 차트
- [ ] Phase 1 BacktestView.vue 코드 재사용

---

### 📅 Day 60 - 관리자 휴장일 관리 페이지 (Frontend)
**목표**: 휴장일 관리 UI 구현

**작업 내용**:
- [ ] AdminHolidayView.vue 생성
  - 휴장일 목록 (연도별)
  - 휴장일 추가/수정/삭제
  - 달력 뷰
- [ ] 관리자 대시보드에 주식 통계 추가

---

### 📅 Day 61 - Phase 1/2 전환 기능
**목표**: 코인/주식 자동매매 전환 기능 구현

**작업 내용**:
- [ ] 사용자별 활성 Phase 설정
- [ ] 대시보드 Phase 전환 UI
- [ ] 알림 통합 (코인 + 주식)
- [ ] 일일 리포트 통합

---

### 📅 Day 62 - 통합 테스트 (1)
**목표**: 백엔드 통합 테스트

**작업 내용**:
- [ ] KIS API 연동 테스트
- [ ] 자동매매 시나리오 테스트
- [ ] 알림 시스템 테스트
- [ ] 에러 핸들링 테스트

**테스트 시나리오**:
1. 장 시작 전: 봇 대기 상태 확인
2. 장 시작: 알림 발송, 자동매매 시작
3. 매수 신호 발생: 주문 실행, 알림 발송
4. 매도 신호 발생: 주문 실행, 알림 발송
5. 장 마감: 일일 리포트 발송

---

### 📅 Day 63 - 통합 테스트 (2)
**목표**: 프론트엔드 통합 테스트

**작업 내용**:
- [ ] 모든 페이지 렌더링 테스트
- [ ] API 연동 테스트
- [ ] 반응형 디자인 테스트
- [ ] 크로스 브라우저 테스트

---

### 📅 Day 64 - 문서화 및 운영 준비
**목표**: 운영 문서 작성

**작업 내용**:
- [ ] Phase 2 아키텍처 문서 업데이트
- [ ] API 문서 (Swagger)
- [ ] 사용자 가이드
- [ ] 배포 절차서 업데이트
- [ ] 장애 대응 매뉴얼 업데이트

---

### 📅 Day 65 - 최종 점검 및 v2.0 릴리즈
**목표**: 최종 점검 및 릴리즈

**작업 내용**:
- [ ] 보안 점검 (OWASP Top 10)
- [ ] 성능 테스트
- [ ] README.md 최종 업데이트
- [ ] v2.0.0 Git 태깅
- [ ] 운영 서버 배포

---

## 📊 현재 진행 상황

> 최신 상세 진행 상황은 `Phase2_Implementation_Plan.md`(Day 48~66 체계) 기준입니다.

- 전체 진척도: 약 85% (Phase 1 완료, Phase 2 Day 62/66 완료)
- Phase 2-1 (기반 구축): ✅ 완료
- Phase 2-2 (핵심 기능): ✅ 완료 (자동매매, 봇 모니터링, 백테스팅, 코인/주식 UI 통일까지 완료)
- Phase 2-3 (고도화): ⏳ 예정 (수익 분석, 관리자 휴장일 관리)
- Phase 2-4 (안정화): ⏳ 예정 (사이드바 최종 활성화, 통합 테스트, v2.0 릴리즈)

---

## 🗓️ 프로젝트 일정 요약

| 구분 | 기간(설계 시점 기준) | 상태 | 주요 작업 |
|------|------|------|----------|
| Phase 2-1 | Day 38~44 | ✅ 완료 | KIS API 연동, DB 테이블, 모의투자 |
| Phase 2-2 | Day 45~53 | ✅ 완료 | 매수/매도 로직, 스케줄러, 알림, 백테스팅, UI 통일 |
| Phase 2-3 | Day 54~60 | ⏳ 예정 | 수익 분석, 휴장일 관리 |
| Phase 2-4 | Day 61~65 | ⏳ 예정 | 통합 테스트, 문서화, v2.0 배포 |

> 실제 작업 순서/일자는 `Phase2_Implementation_Plan.md`를 참고하세요 (Day 번호 체계가 다릅니다).

---

## 🔧 Phase 1 코드 재사용 목록

| Phase 1 파일 | Phase 2 재사용 | 수정 내용 |
|-------------|---------------|----------|
| TechnicalIndicatorService.java | StockTechnicalIndicatorService.java | 파라미터 기본값 변경 |
| SignalDetectorService.java | StockSignalDetectorService.java | 보유기간 체크 추가 |
| RiskManagementService.java | StockRiskManagementService.java | 거래시간 체크 추가 |
| TradingBotService.java | StockTradingBotService.java | 휴장일 체크 추가 |
| BacktestService.java | StockBacktestService.java | 수수료율 변경 |
| NotificationService.java | 공통 사용 | 주식 알림 템플릿 추가 |
| TradingSettingsView.vue | StockTradingSettingsView.vue | 종목 선택 UI 변경 |
| BacktestView.vue | StockBacktestView.vue | 파라미터 UI 변경 |

---

## 🚀 실행 방법

### 1. 환경 설정
```bash
# .env 파일에 KIS API 키 추가
KIS_APP_KEY=your_kis_app_key
KIS_APP_SECRET=your_kis_app_secret
KIS_ACCOUNT_NO=your_account_number
KIS_MOCK_MODE=true  # 모의투자 모드
```

### 2. Docker 실행
```bash
# 개발 환경
docker-compose --env-file .env.development up -d --build

# 운영 환경
docker-compose -f docker-compose.prod.yml --env-file .env.production up -d --build
```

### 3. 접속
- **Frontend**: https://crypto-trading-prd.duckdns.org
- **Backend API**: https://crypto-trading-prd.duckdns.org/api
- **Swagger UI**: https://crypto-trading-prd.duckdns.org/swagger-ui/index.html

---

## 📝 라이선스
이 프로젝트는 개인 학습 및 연구 목적으로 제작되었습니다.

---

## 📞 연락처
프로젝트 관련 문의사항이 있으시면 이슈를 등록해주세요.