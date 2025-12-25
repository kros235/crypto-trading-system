#!/bin/bash
# ============================================================
# MySQL 일일 백업 스크립트 (Linux/Mac)
# 설명: 매일 MySQL 데이터베이스를 백업하고 7일 이상 된 백업 삭제
# 사용법: ./backup-db.sh 또는 cron 등록
# cron 예시: 0 4 * * * /path/to/backup-db.sh >> /path/to/backup.log 2>&1
# ============================================================

set -e

# 설정
PROJECT_DIR="/home/user/crypto-trading-system"  # 실제 경로로 수정
BACKUP_DIR="$PROJECT_DIR/backups/mysql"
CONTAINER_NAME="crypto-mysql"
DB_NAME="crypto_trading"
RETENTION_DAYS=7

# 날짜
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="crypto_trading_$DATE.sql"

# 백업 디렉토리 생성
mkdir -p "$BACKUP_DIR"

# 로그 함수
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

log "========== MySQL 백업 시작 =========="

# Docker 컨테이너 상태 확인
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    log "[ERROR] MySQL 컨테이너가 실행 중이 아닙니다."
    exit 1
fi

# .env 파일에서 비밀번호 읽기
if [ -f "$PROJECT_DIR/.env" ]; then
    source "$PROJECT_DIR/.env"
else
    log "[ERROR] .env 파일을 찾을 수 없습니다."
    exit 1
fi

# MySQL 덤프 실행
log "[INFO] 백업 시작: $BACKUP_FILE"
docker exec "$CONTAINER_NAME" mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" \
    --single-transaction --routines --triggers "$DB_NAME" > "$BACKUP_DIR/$BACKUP_FILE"

if [ $? -eq 0 ]; then
    # 압축
    gzip "$BACKUP_DIR/$BACKUP_FILE"
    COMPRESSED_FILE="${BACKUP_FILE}.gz"
    FILE_SIZE=$(du -h "$BACKUP_DIR/$COMPRESSED_FILE" | cut -f1)
    log "[SUCCESS] 백업 완료: $COMPRESSED_FILE ($FILE_SIZE)"
else
    log "[ERROR] mysqldump 실행 실패"
    exit 1
fi

# 오래된 백업 삭제
log "[INFO] $RETENTION_DAYS 일 이상 된 백업 파일 정리..."
find "$BACKUP_DIR" -name "*.gz" -mtime +$RETENTION_DAYS -delete -print | while read f; do
    log "[INFO] 삭제됨: $f"
done

# 현재 백업 목록
log "[INFO] 현재 백업 목록:"
ls -lh "$BACKUP_DIR"/*.gz 2>/dev/null | awk '{print "  - " $NF " (" $5 ")"}'

log "========== MySQL 백업 완료 =========="