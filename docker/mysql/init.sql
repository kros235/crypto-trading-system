-- 코인 자동매매 시스템 데이터베이스 초기화

-- 사용자 테이블
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(50) PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100) NOT NULL UNIQUE,
    join_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    is_active BOOLEAN DEFAULT TRUE,
    role ENUM('USER', 'ADMIN') DEFAULT 'USER',
    api_key_encrypted TEXT,
    secret_key_encrypted TEXT,
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
    buy_threshold_pct DECIMAL(5,2) DEFAULT -5.00 COMMENT '매수 기준 하락률 (%)',
    sell_target_pct DECIMAL(5,2) DEFAULT 3.00 COMMENT '목표 수익률 (%)',
    stop_loss_pct DECIMAL(5,2) DEFAULT -10.00 COMMENT '손절매 기준 (%)',
    max_holdings_per_coin INT DEFAULT 3 COMMENT '종목당 최대 보유 건수',
    daily_limit_amount DECIMAL(15,2) DEFAULT 1000000.00 COMMENT '일일 거래 한도',
    use_ai_analysis BOOLEAN DEFAULT FALSE COMMENT 'AI 뉴스 분석 사용 여부',
    use_trailing_stop BOOLEAN DEFAULT FALSE COMMENT '트레일링 스톱 사용 여부',
    trailing_stop_pct DECIMAL(5,2) DEFAULT -5.00 COMMENT '트레일링 스톱 비율 (%)',
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
-- BCrypt 해시값: $2a$10$... 형태로 저장됨
INSERT INTO users (user_id, password_hash, email, role, is_active) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin@crypto.com', 'ADMIN', TRUE)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 관리자 기본 거래 설정
INSERT INTO trading_settings (user_id, coin_symbols, base_period, buy_threshold_pct, sell_target_pct, stop_loss_pct) VALUES
('admin', '["KRW-BTC", "KRW-ETH"]', 20, -5.00, 3.00, -10.00)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 사용자 권한 설정 (모든 IP에서 접근 가능하도록)
CREATE USER IF NOT EXISTS 'crypto_user'@'%' IDENTIFIED BY 'cryptopass123!';
GRANT ALL PRIVILEGES ON crypto_trading.* TO 'crypto_user'@'%';
FLUSH PRIVILEGES;