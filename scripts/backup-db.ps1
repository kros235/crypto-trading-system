# ============================================================
# MySQL 일일 백업 스크립트 (Windows PowerShell)
# 설명: 매일 MySQL 데이터베이스를 백업하고 7일 이상 된 백업 삭제
# 사용법: .\backup-db.ps1 또는 Windows 작업 스케줄러에 등록
# ============================================================

# 설정
$PROJECT_DIR = Split-Path -Parent $PSScriptRoot
$BACKUP_DIR = "$PROJECT_DIR\backups\mysql"
$CONTAINER_NAME = "crypto-mysql"
$DB_NAME = "crypto_trading"
$RETENTION_DAYS = 7

# 날짜 포맷
$DATE = Get-Date -Format "yyyyMMdd_HHmmss"
$BACKUP_FILE = "crypto_trading_$DATE.sql"

# 백업 디렉토리 생성
if (!(Test-Path $BACKUP_DIR)) {
    New-Item -ItemType Directory -Path $BACKUP_DIR -Force
    Write-Host "[INFO] 백업 디렉토리 생성: $BACKUP_DIR"
}

# 로그 함수
function Write-Log {
    param([string]$Message)
    $Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Write-Host "[$Timestamp] $Message"
    Add-Content -Path "$BACKUP_DIR\backup.log" -Value "[$Timestamp] $Message"
}

Write-Log "========== MySQL 백업 시작 =========="

# Docker 컨테이너 상태 확인
$containerStatus = docker inspect -f '{{.State.Running}}' $CONTAINER_NAME 2>$null
if ($containerStatus -ne "true") {
    Write-Log "[ERROR] MySQL 컨테이너가 실행 중이 아닙니다."
    exit 1
}

# .env 파일에서 비밀번호 읽기
$envFile = "$PROJECT_DIR\.env"
if (Test-Path $envFile) {
    $envContent = Get-Content $envFile
    $MYSQL_ROOT_PASSWORD = ($envContent | Where-Object { $_ -match "^MYSQL_ROOT_PASSWORD=" }) -replace "MYSQL_ROOT_PASSWORD=", ""
} else {
    Write-Log "[ERROR] .env 파일을 찾을 수 없습니다."
    exit 1
}

# MySQL 덤프 실행
Write-Log "[INFO] 백업 시작: $BACKUP_FILE"
try {
    docker exec $CONTAINER_NAME mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers $DB_NAME > "$BACKUP_DIR\$BACKUP_FILE"
    
    if ($LASTEXITCODE -eq 0) {
        # 백업 파일 압축
        $COMPRESSED_FILE = "$BACKUP_FILE.gz"
        Compress-Archive -Path "$BACKUP_DIR\$BACKUP_FILE" -DestinationPath "$BACKUP_DIR\$COMPRESSED_FILE.zip" -Force
        Remove-Item "$BACKUP_DIR\$BACKUP_FILE" -Force
        
        $fileSize = (Get-Item "$BACKUP_DIR\$COMPRESSED_FILE.zip").Length / 1KB
        Write-Log "[SUCCESS] 백업 완료: $COMPRESSED_FILE.zip ($([math]::Round($fileSize, 2)) KB)"
    } else {
        Write-Log "[ERROR] mysqldump 실행 실패"
        exit 1
    }
} catch {
    Write-Log "[ERROR] 백업 중 오류 발생: $_"
    exit 1
}

# 오래된 백업 삭제 (7일 이상)
Write-Log "[INFO] $RETENTION_DAYS 일 이상 된 백업 파일 정리..."
$oldFiles = Get-ChildItem -Path $BACKUP_DIR -Filter "*.zip" | Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-$RETENTION_DAYS) }
foreach ($file in $oldFiles) {
    Remove-Item $file.FullName -Force
    Write-Log "[INFO] 삭제됨: $($file.Name)"
}

# 현재 백업 목록 표시
Write-Log "[INFO] 현재 백업 목록:"
Get-ChildItem -Path $BACKUP_DIR -Filter "*.zip" | Sort-Object LastWriteTime -Descending | ForEach-Object {
    $size = [math]::Round($_.Length / 1KB, 2)
    Write-Log "  - $($_.Name) ($size KB)"
}

Write-Log "========== MySQL 백업 완료 =========="