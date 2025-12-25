# ============================================================
# MySQL 백업 복원 스크립트 (Windows PowerShell)
# 사용법: .\restore-db.ps1 -BackupFile "crypto_trading_20241225_040000.sql.gz.zip"
# ============================================================

param(
    [Parameter(Mandatory=$true)]
    [string]$BackupFile
)

# 설정
$PROJECT_DIR = Split-Path -Parent $PSScriptRoot
$BACKUP_DIR = "$PROJECT_DIR\backups\mysql"
$CONTAINER_NAME = "crypto-mysql"
$DB_NAME = "crypto_trading"

$FULL_PATH = "$BACKUP_DIR\$BackupFile"

# 파일 존재 확인
if (!(Test-Path $FULL_PATH)) {
    Write-Host "[ERROR] 백업 파일을 찾을 수 없습니다: $FULL_PATH"
    Write-Host ""
    Write-Host "사용 가능한 백업 파일:"
    Get-ChildItem -Path $BACKUP_DIR -Filter "*.zip" | ForEach-Object { Write-Host "  - $($_.Name)" }
    exit 1
}

# 확인 프롬프트
Write-Host ""
Write-Host "⚠️  경고: 이 작업은 현재 데이터베이스를 덮어씁니다!"
Write-Host "복원할 파일: $BackupFile"
Write-Host ""
$confirm = Read-Host "계속하시겠습니까? (yes 입력)"

if ($confirm -ne "yes") {
    Write-Host "복원이 취소되었습니다."
    exit 0
}

# .env 파일에서 비밀번호 읽기
$envFile = "$PROJECT_DIR\.env"
$envContent = Get-Content $envFile
$MYSQL_ROOT_PASSWORD = ($envContent | Where-Object { $_ -match "^MYSQL_ROOT_PASSWORD=" }) -replace "MYSQL_ROOT_PASSWORD=", ""

# 압축 해제
$TEMP_DIR = "$BACKUP_DIR\temp"
New-Item -ItemType Directory -Path $TEMP_DIR -Force | Out-Null
Expand-Archive -Path $FULL_PATH -DestinationPath $TEMP_DIR -Force
$SQL_FILE = Get-ChildItem -Path $TEMP_DIR -Filter "*.sql" | Select-Object -First 1

Write-Host "[INFO] 복원 시작..."

# 복원 실행
Get-Content $SQL_FILE.FullName | docker exec -i $CONTAINER_NAME mysql -u root -p"$MYSQL_ROOT_PASSWORD" $DB_NAME

if ($LASTEXITCODE -eq 0) {
    Write-Host "[SUCCESS] 데이터베이스 복원 완료!"
} else {
    Write-Host "[ERROR] 복원 실패"
}

# 임시 파일 정리
Remove-Item -Path $TEMP_DIR -Recurse -Force

Write-Host "[INFO] 완료"