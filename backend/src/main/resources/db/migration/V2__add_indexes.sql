-- =====================================================
-- 인덱스 최적화 스크립트 (Day 17)
-- 설명: 자주 사용되는 쿼리 패턴에 맞는 인덱스 추가
-- =====================================================

-- 1. transactions 테이블 인덱스
-- 목적: 사용자별 보유 중인 거래 조회 최적화
CREATE INDEX IF NOT EXISTS idx_transactions_user_status 
    ON transactions(user_id, status);

-- 목적: 사용자별 날짜 범위 조회 최적화 (일일 리포트용)
CREATE INDEX IF NOT EXISTS idx_transactions_user_created 
    ON transactions(user_id, created_at);

-- 목적: 사용자별 매도일 조회 최적화
CREATE INDEX IF NOT EXISTS idx_transactions_user_sold 
    ON transactions(user_id, sold_at);

-- 목적: 코인별 상태 조회 최적화 (통계용)
CREATE INDEX IF NOT EXISTS idx_transactions_coin_status 
    ON transactions(coin_symbol, status);

-- 2. trading_settings 테이블 인덱스
-- 목적: 사용자별 설정 조회 최적화
CREATE INDEX IF NOT EXISTS idx_trading_settings_user 
    ON trading_settings(user_id);

-- 3. system_logs 테이블 인덱스 (존재하는 경우)
-- 목적: 날짜별 로그 조회 최적화
-- CREATE INDEX IF NOT EXISTS idx_system_logs_created 
--     ON system_logs(created_at);

-- 4. 통계 정보 갱신 (MySQL)
ANALYZE TABLE transactions;
ANALYZE TABLE trading_settings;
ANALYZE TABLE users;
ANALYZE TABLE coin_info;