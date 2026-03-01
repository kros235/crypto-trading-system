-- 코인 자동매매 시스템 데이터베이스 초기화

-- 사용자 테이블
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(50) PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100) NOT NULL UNIQUE,
    discord_user_id VARCHAR(30),
    join_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    is_active BOOLEAN DEFAULT TRUE,
    role ENUM('USER', 'ADMIN') DEFAULT 'USER',
    api_key_encrypted TEXT,
    secret_key_encrypted TEXT,
    allowed_ips JSON DEFAULT NULL COMMENT '허용 IP 목록 (최대 3개)',
    totp_secret VARCHAR(64) DEFAULT NULL COMMENT '2FA TOTP 비밀키',
    totp_enabled BOOLEAN DEFAULT FALSE COMMENT '2FA 활성화 여부',
    ip_whitelist_enabled BOOLEAN DEFAULT FALSE COMMENT 'IP 화이트리스트 활성화 여부',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 거래 설정 테이블
CREATE TABLE IF NOT EXISTS trading_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    coin_symbols JSON NOT NULL,
    base_period INT DEFAULT 20 COMMENT '이동평균선 기간 (7, 14, 20, 30일)',
    buy_threshold_pct DECIMAL(5,2) DEFAULT -6.00 COMMENT '매수 기준 하락률 (%)',
    sell_target_pct DECIMAL(5,2) DEFAULT 4.00 COMMENT '목표 수익률 (%)',
    stop_loss_pct DECIMAL(5,2) DEFAULT -8.00 COMMENT '손절매 기준 (%)',
    max_holdings_per_coin INT DEFAULT 2 COMMENT '종목당 최대 보유 건수',
    daily_limit_amount DECIMAL(15,2) DEFAULT 1000000.00 COMMENT '일일 거래 한도',
    use_ai_analysis BOOLEAN DEFAULT FALSE COMMENT 'AI 뉴스 분석 사용 여부',
    use_trailing_stop BOOLEAN DEFAULT TRUE COMMENT '트레일링 스톱 사용 여부',
    trailing_stop_pct DECIMAL(5,2) DEFAULT -4.00 COMMENT '트레일링 스톱 비율 (%)',
    -- ⭐⭐⭐ Day 14 추가: 기술적 지표 설정 (6개 컬럼) ⭐⭐⭐
    rsi_period INT DEFAULT 14 COMMENT 'RSI 기간',
    rsi_buy_threshold INT DEFAULT 32 COMMENT 'RSI 매수 신호 임계값',
    rsi_sell_threshold INT DEFAULT 68 COMMENT 'RSI 매도 신호 임계값',
    bb_period INT DEFAULT 20 COMMENT '볼린저 밴드 기간',
    bb_multiplier INT DEFAULT 2 COMMENT '볼린저 밴드 승수',
    volume_threshold INT DEFAULT 140 COMMENT '거래량 급증 기준 (%)',
    -- ⭐⭐⭐ Day 19 추가: 리스크 관리 설정 (3개 컬럼) ⭐⭐⭐
    daily_trade_limit_pct INT DEFAULT 20 COMMENT '일일 거래 한도 (%)',
    max_position_pct INT DEFAULT 25 COMMENT '종목당 최대 비중 (%)',
    daily_stop_loss_pct INT DEFAULT -5 COMMENT '긴급 정지 손실률 (%)',
    -- ⭐⭐⭐ Day 29 추가: 급락장 보호 기능 (3개 컬럼) ⭐⭐⭐
    use_market_trend_filter BOOLEAN DEFAULT FALSE COMMENT '시장 추세 필터 사용 (BTC MA20 기준)',
    cumulative_loss_limit_pct INT DEFAULT -10 COMMENT '누적 손실 한도 (%) - 초기 자본 대비',
    consecutive_stop_loss_limit INT DEFAULT 3 COMMENT '연속 손절 제한 횟수',
    fixed_buy_amount DECIMAL(15,2) DEFAULT 10000.00 COMMENT '1회 매수 금액 (원) - 고정 금액 매수 시 사용 (최소 5,000원)',
    use_daily_limit_recovery BOOLEAN DEFAULT FALSE COMMENT '일일 한도 복구 옵션 - 매도 시 한도 복구',
    use_round_robin BOOLEAN DEFAULT TRUE COMMENT '매수 방식 - ON: 라운드로빈 균등분배, OFF: 고정 금액 매수',
    additional_drop_pct DECIMAL(5,2) DEFAULT 1.00 COMMENT '추가 하락시 매수 비율 (%)',
    use_stop_loss TINYINT(1) DEFAULT 1 COMMENT '손절매 사용 여부',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 거래 이력 테이블
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    coin_symbol VARCHAR(20) NOT NULL COMMENT 'KRW-BTC 형식',
    type ENUM('BUY', 'SELL') NOT NULL,
    quantity DECIMAL(20,8) NOT NULL COMMENT '거래 수량',
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
    -- ⭐⭐⭐ 트레일링 스톱용 컬럼 추가 ⭐⭐⭐
    highest_price DECIMAL(20,8) NULL COMMENT '보유 기간 중 최고가 (트레일링 스톱용)',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_symbol_status (user_id, coin_symbol, status),
    INDEX idx_created_at (created_at),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 코인 정보 테이블
CREATE TABLE IF NOT EXISTS coin_info (
    symbol VARCHAR(20) PRIMARY KEY COMMENT 'KRW-BTC 형식',
    name_kr VARCHAR(50) NOT NULL COMMENT '한글명',
    name_en VARCHAR(50) NOT NULL COMMENT '영문명',
    is_active BOOLEAN DEFAULT TRUE COMMENT '거래 가능 여부',
    market_cap_rank INT NULL COMMENT '시가총액 순위',
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_active_rank (is_active, market_cap_rank)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 가격 이력 테이블 (기술적 지표 포함)
CREATE TABLE IF NOT EXISTS price_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    price DECIMAL(15,2) NOT NULL,
    volume DECIMAL(20,8) NOT NULL COMMENT '거래량',
    timestamp TIMESTAMP NOT NULL,
    ma7 DECIMAL(15,2) NULL COMMENT '7일 이동평균',
    ma20 DECIMAL(15,2) NULL COMMENT '20일 이동평균',
    ma30 DECIMAL(15,2) NULL COMMENT '30일 이동평균',
    rsi DECIMAL(5,2) NULL COMMENT 'RSI 지표',
    bb_upper DECIMAL(15,2) NULL COMMENT '볼린저밴드 상단',
    bb_lower DECIMAL(15,2) NULL COMMENT '볼린저밴드 하단',
    FOREIGN KEY (symbol) REFERENCES coin_info(symbol) ON DELETE CASCADE,
    INDEX idx_symbol_timestamp (symbol, timestamp),
    UNIQUE KEY unique_symbol_timestamp (symbol, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 일간 집계 테이블
CREATE TABLE IF NOT EXISTS daily_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    date DATE NOT NULL,
    total_profit DECIMAL(15,2) DEFAULT 0.00 COMMENT '일일 총 손익',
    profit_rate DECIMAL(5,2) DEFAULT 0.00 COMMENT '일일 수익률 (%)',
    buy_count INT DEFAULT 0 COMMENT '매수 건수',
    sell_count INT DEFAULT 0 COMMENT '매도 건수',
    total_investment DECIMAL(15,2) DEFAULT 0.00 COMMENT '총 투자금액',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_date (user_id, date),
    INDEX idx_user_date (user_id, date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 시스템 로그 테이블
CREATE TABLE IF NOT EXISTS system_logs (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NULL,
    action VARCHAR(100) NOT NULL COMMENT '수행 동작',
    message TEXT NOT NULL COMMENT '로그 메시지',
    level ENUM('INFO', 'WARN', 'ERROR', 'DEBUG') DEFAULT 'INFO',
    ip_address VARCHAR(45) NULL COMMENT 'IP 주소',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_timestamp (user_id, timestamp),
    INDEX idx_level_timestamp (level, timestamp),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 초기 코인 정보 데이터 삽입
INSERT INTO coin_info (symbol, name_kr, name_en, market_cap_rank) VALUES
('KRW-BTC', '비트코인', 'Bitcoin', 1),
('KRW-ETH', '이더리움', 'Ethereum', 2),
('KRW-XRP', '리플', 'Ripple', 3),
('KRW-ADA', '에이다', 'Cardano', 4),
('KRW-SOL', '솔라나', 'Solana', 5),
('KRW-DOGE', '도지코인', 'Dogecoin', 6),
('KRW-DOT', '폴카닷', 'Polkadot', 7),
('KRW-MATIC', '폴리곤', 'Polygon', 8),
('KRW-AVAX', '아발란체', 'Avalanche', 9),
('KRW-SHIB', '시바이누', 'Shiba Inu', 10)
ON DUPLICATE KEY UPDATE last_updated = CURRENT_TIMESTAMP;

-- 기본 관리자 계정 생성 (비밀번호: admin123! - 실제 운영 시 반드시 변경)
-- BCrypt 해시값: Python bcrypt로 생성 (rounds=10)
INSERT INTO users (user_id, password_hash, email, role, is_active) VALUES
('admin', '$2b$10$ZoMm7C71INlM.zzPabHnDe5opTA3tGcBzOVPObOaJW5CCXRfIQoSG', 'admin@crypto.com', 'ADMIN', TRUE)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 관리자 기본 거래 설정
INSERT INTO trading_settings (user_id, coin_symbols, base_period, buy_threshold_pct, sell_target_pct, stop_loss_pct) VALUES
('admin', '["KRW-BTC", "KRW-ETH"]', 20, -5.00, 3.00, -10.00)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 사용자 권한 설정 (모든 IP에서 접근 가능하도록)
CREATE USER IF NOT EXISTS 'crypto_user'@'%' IDENTIFIED BY 'cryptopass123!';
GRANT ALL PRIVILEGES ON crypto_trading.* TO 'crypto_user'@'%';
FLUSH PRIVILEGES;

-- =====================================================
-- ⭐⭐⭐ 성능 최적화 인덱스 (Day 17) ⭐⭐⭐
-- =====================================================
-- 참고: MySQL 8.0 초기 버전은 CREATE INDEX IF NOT EXISTS를 지원하지 않음
-- 아래 인덱스들은 테이블 생성 시 이미 유사한 인덱스가 포함되어 있거나,
-- 성능 최적화용으로 필요시 수동으로 추가 가능

-- transactions 테이블: idx_user_symbol_status, idx_created_at, idx_status 이미 존재
-- trading_settings 테이블: idx_user_id 이미 존재

-- ============================================
-- Day 24: AI 뉴스 분석 테이블 (2025-12-29 추가)
-- ============================================

-- 뉴스 테이블 (수집된 뉴스 저장)
CREATE TABLE IF NOT EXISTS coin_news (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coin_symbol VARCHAR(20) NOT NULL COMMENT '코인 심볼 (KRW-BTC 등)',
    title VARCHAR(500) NOT NULL COMMENT '뉴스 제목',
    summary TEXT COMMENT '뉴스 요약/본문',
    source VARCHAR(100) NOT NULL COMMENT '뉴스 출처 (CoinDesk, Reuters 등)',
    source_url VARCHAR(1000) COMMENT '원문 URL',
    published_at TIMESTAMP NOT NULL COMMENT '뉴스 발행 시간',
    collected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '수집 시간',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- ⭐⭐⭐ Day 25 추가: AI 분석 관련 컬럼 (3개) ⭐⭐⭐
    analyzed BOOLEAN DEFAULT FALSE COMMENT '분석 완료 여부',
    analyzed_at TIMESTAMP NULL COMMENT '분석 시간',
    sentiment_score DECIMAL(3,2) NULL COMMENT '감성 점수 (-1.00 ~ +1.00)',
    INDEX idx_coin_news_symbol (coin_symbol),
    INDEX idx_coin_news_published (published_at),
    INDEX idx_coin_news_symbol_published (coin_symbol, published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 뉴스 분석 결과 테이블
CREATE TABLE IF NOT EXISTS coin_news_analysis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL COMMENT '사용자 ID',
    coin_symbol VARCHAR(20) NOT NULL COMMENT '코인 심볼',
    analysis_date DATE NOT NULL COMMENT '분석 일자 (KST 기준)',
    news_count INT DEFAULT 0 COMMENT '분석된 뉴스 건수',
    -- ⭐⭐⭐ 주석 수정: (-100 ~ +100) → (-1.0 ~ +1.0) ⭐⭐⭐
    average_score DECIMAL(6,2) DEFAULT 0 COMMENT '평균 점수 (-1.0 ~ +1.0)',
    weight_adjustment DECIMAL(4,2) DEFAULT 0 COMMENT '가중치 조정값 (-0.5, 0, +0.5)',
    sentiment ENUM('POSITIVE', 'NEGATIVE', 'NEUTRAL') DEFAULT 'NEUTRAL' COMMENT '종합 감성',
    summary TEXT COMMENT '분석 요약 (주요 뉴스 요약)',
    analyzed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '분석 시간',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_coin_date (user_id, coin_symbol, analysis_date),
    INDEX idx_analysis_user (user_id),
    INDEX idx_analysis_date (analysis_date),
    INDEX idx_analysis_coin (coin_symbol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 비밀번호 재설정 토큰 테이블
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL COMMENT '사용자 ID',
    token VARCHAR(255) NOT NULL UNIQUE COMMENT '재설정 토큰',
    expiry_date TIMESTAMP NOT NULL COMMENT '만료 시간',
    used BOOLEAN DEFAULT FALSE COMMENT '사용 여부',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_prt_token (token),
    INDEX idx_prt_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ⭐⭐⭐ Day 30 추가: IP 화이트리스트 테이블 ⭐⭐⭐
CREATE TABLE IF NOT EXISTS ip_whitelist (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    description VARCHAR(100) DEFAULT NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL,
    last_used_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY unique_user_ip (user_id, ip_address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 릴리즈 노트 테이블 (2026-01-08 추가)
CREATE TABLE IF NOT EXISTS release_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '게시글 제목',
    content TEXT NOT NULL COMMENT '게시글 본문',
    category VARCHAR(20) DEFAULT 'GENERAL' COMMENT '카테고리 (COIN: 코인, STOCK: 주식, GENERAL: 공통)',
    author_id VARCHAR(50) NOT NULL COMMENT '작성자 ID',
    author_name VARCHAR(100) NOT NULL COMMENT '작성자 이름',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '작성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '삭제 여부 (soft delete)',
    INDEX idx_release_notes_created_at (created_at DESC),
    INDEX idx_release_notes_is_deleted (is_deleted),
    INDEX idx_release_notes_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 초기 릴리즈 노트 데이터 (샘플)
INSERT INTO release_notes (title, content, category, author_id, author_name) VALUES
('v1.0 Day 30 업데이트 - 릴리즈 노트 기능 추가', 
'■ 릴리즈 노트 게시판 기능 추가\n  - 공지사항 및 업데이트 이력 게시판\n  - 관리자만 작성/수정/삭제 가능\n  - 대시보드 시스템 알림 연동\n\n■ 2FA 인증 (Optional)\n  - Google Authenticator 연동\n\n■ IP 화이트리스트 (Optional)\n  - 접속 IP 제한 기능', 'GENERAL',
'admin', '관리자');

-- ⭐⭐⭐ [신규 추가] 일별 자산 스냅샷 테이블 (매일 23:59 KST 기준) ⭐⭐⭐
-- 용도: 자산 변동 추이 차트에서 사용 (대시보드, 보유자산 페이지)
-- 기존 daily_summary 테이블과 별도 - 평가금액 + 불입금액 추적 전용
CREATE TABLE IF NOT EXISTS daily_asset_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    snapshot_date DATE NOT NULL COMMENT '스냅샷 날짜',
    evaluation_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '평가금액 (KRW잔고 + 코인평가액)',
    deposit_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '누적 불입금액 (입금-출금)',
    krw_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'KRW 잔고',
    coin_evaluation DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '코인 평가액',
    profit_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '수익 금액 (평가금액 - 불입금액)',
    profit_rate DECIMAL(8,4) NOT NULL DEFAULT 0.0000 COMMENT '수익률 (%)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_snapshot_date (user_id, snapshot_date),
    INDEX idx_snapshot_user_date (user_id, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ====================================================
-- ⭐⭐⭐ Phase 2: 주식/ETF 자동매매 테이블 (Day 49 추가) ⭐⭐⭐
-- ====================================================

-- 1. 주식/ETF 정보 테이블
-- 데이터는 사용자가 KIS API 종목 검색을 통해 직접 추가
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
    fixed_buy_amount DECIMAL(15,2) DEFAULT 100000.00 COMMENT '1회 매수 금액',
    use_daily_limit_recovery BOOLEAN DEFAULT FALSE,
    use_round_robin BOOLEAN DEFAULT TRUE,
    -- Phase 2 전용: 레버리지 ETF 관련
    max_holding_days INT DEFAULT 20 COMMENT '최대 보유일수 (레버리지 decay 방지)',
    -- Phase 2 전용: KIS API 키 (사용자별)
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
-- 데이터는 API 자동 수집 + 관리자 수동 등록 방식으로 관리
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