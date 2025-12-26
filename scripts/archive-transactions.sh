#!/bin/bash
# ============================================
# 거래 내역 월별 아카이빙 스크립트 (Linux/Mac)
# 실행: chmod +x scripts/archive-transactions.sh && ./scripts/archive-transactions.sh
# ============================================

set -e

# 설정
MONTHS_AGO=${1:-3}  # 몇 개월 전 데이터를 아카이빙할지 (기본: 3개월)
DRY_RUN=${2:-false}

# 스크립트 위치 기준으로 프로젝트 루트 찾기
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

BACKUP_DIR="$PROJECT_ROOT/backups/archives"
CONTAINER_NAME="crypto-trading-mysql"
DATABASE="crypto_trading"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# 아카이빙 대상 월 계산
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    TARGET_DATE=$(date -v-${MONTHS_AGO}m +"%Y-%m")
else
    # Linux
    TARGET_DATE=$(date -d "$MONTHS_AGO months ago" +"%Y-%m")
fi

TARGET_YEAR=$(echo $TARGET_DATE | cut -d'-' -f1)
TARGET_MONTH=$(echo $TARGET_DATE | cut -d'-' -f2)
START_DATE="$TARGET_YEAR-$TARGET_MONTH-01"

if [[ "$OSTYPE" == "darwin"* ]]; then
    END_DATE=$(date -j -f "%Y-%m-%d" "$START_DATE" -v+1m +"%Y-%m-%d")
else
    END_DATE=$(date -d "$START_DATE +1 month" +"%Y-%m-%d")
fi

echo "============================================"
echo "  거래 내역 월별 아카이빙"
echo "============================================"
echo "대상 기간: $START_DATE ~ $END_DATE"
echo "백업 디렉토리: $BACKUP_DIR"
echo ""

# 백업 디렉토리 생성
mkdir -p "$BACKUP_DIR"

# Docker 컨테이너 확인
if ! docker ps --filter "name=$CONTAINER_NAME" --format "{{.Status}}" | grep -q "Up"; then
    echo "[ERROR] MySQL 컨테이너가 실행 중이 아닙니다."
    exit 1
fi

# 환경변수 파일에서 MySQL 비밀번호 읽기
ENV_FILE="$PROJECT_ROOT/.env.development"
if [ ! -f "$ENV_FILE" ]; then
    ENV_FILE="$PROJECT_ROOT/.env"
fi

if [ -f "$ENV_FILE" ]; then
    MYSQL_PASSWORD=$(grep "^MYSQL_PASSWORD=" "$ENV_FILE" | cut -d'=' -f2)
fi

if [ -z "$MYSQL_PASSWORD" ]; then
    echo "[ERROR] MySQL 비밀번호를 찾을 수 없습니다."
    exit 1
fi

# 아카이빙 대상 데이터 건수 확인
echo "아카이빙 대상 데이터 확인 중..."
RECORD_COUNT=$(docker exec $CONTAINER_NAME mysql -u crypto_user -p"$MYSQL_PASSWORD" $DATABASE -N -e \
    "SELECT COUNT(*) FROM transactions WHERE created_at >= '$START_DATE' AND created_at < '$END_DATE';" 2>/dev/null)

echo "대상 레코드 수: $RECORD_COUNT 건"

if [ "$RECORD_COUNT" -eq 0 ]; then
    echo "[INFO] 아카이빙할 데이터가 없습니다."
    exit 0
fi

if [ "$DRY_RUN" = "true" ]; then
    echo ""
    echo "[DRY RUN] 실제 아카이빙은 수행하지 않습니다."
    exit 0
fi

# 백업 파일명
BACKUP_FILE="transactions_${TARGET_YEAR}${TARGET_MONTH}_$TIMESTAMP.sql"
BACKUP_PATH="$BACKUP_DIR/$BACKUP_FILE"

# 데이터 백업
echo ""
echo "Step 1: 데이터 백업 중..."

docker exec $CONTAINER_NAME mysqldump -u crypto_user -p"$MYSQL_PASSWORD" $DATABASE transactions \
    --where="created_at >= '$START_DATE' AND created_at < '$END_DATE'" \
    --no-create-info --compact 2>/dev/null > "$BACKUP_PATH"

if [ $? -ne 0 ]; then
    echo "[ERROR] 데이터 백업 실패"
    exit 1
fi

echo "[OK] 백업 완료: $BACKUP_FILE"

# 압축
echo ""
echo "Step 2: 백업 파일 압축 중..."
gzip -f "$BACKUP_PATH"
COMPRESSED_FILE="$BACKUP_PATH.gz"

FILE_SIZE=$(du -h "$COMPRESSED_FILE" | cut -f1)
echo "[OK] 압축 완료: $FILE_SIZE"

# 선택적: 원본 데이터 삭제
echo ""
echo "========================================"
echo "  원본 데이터 삭제 여부 확인"
echo "========================================"
echo "백업이 완료되었습니다."
echo "원본 데이터를 삭제하시겠습니까?"
echo ""
echo "[WARNING] 삭제 후 복구는 백업 파일로만 가능합니다!"
echo ""
read -p "삭제하려면 'DELETE'를 입력하세요 (취소: Enter): " CONFIRMATION

if [ "$CONFIRMATION" = "DELETE" ]; then
    echo ""
    echo "Step 3: 원본 데이터 삭제 중..."
    
    docker exec $CONTAINER_NAME mysql -u crypto_user -p"$MYSQL_PASSWORD" $DATABASE -e \
        "DELETE FROM transactions WHERE created_at >= '$START_DATE' AND created_at < '$END_DATE' AND status = 'SOLD';" 2>/dev/null
    
    if [ $? -eq 0 ]; then
        echo "[OK] 매도 완료(SOLD) 거래 내역 삭제 완료"
    else
        echo "[ERROR] 삭제 실패"
    fi
else
    echo "[INFO] 삭제를 건너뜁니다. 백업 파일만 생성되었습니다."
fi

echo ""
echo "============================================"
echo "  아카이빙 완료!"
echo "============================================"
echo "백업 파일: $COMPRESSED_FILE"
echo ""