# ============================================
# 거래 내역 월별 아카이빙 스크립트 (Windows PowerShell)
# 실행: .\scripts\archive-transactions.ps1
# ============================================

param(
    [int]$MonthsAgo = 3,  # 몇 개월 전 데이터를 아카이빙할지
    [switch]$DryRun       # 실제 실행 없이 테스트
)

# 스크립트 위치 기준으로 프로젝트 루트 찾기
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir

# 설정
$BackupDir = Join-Path $ProjectRoot "backups\archives"
$ContainerName = "crypto-trading-mysql"
$Database = "crypto_trading"
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

# 아카이빙 대상 월 계산
$TargetDate = (Get-Date).AddMonths(-$MonthsAgo)
$TargetYear = $TargetDate.Year
$TargetMonth = $TargetDate.Month
$StartDate = "$TargetYear-$($TargetMonth.ToString('00'))-01"
$EndDate = (Get-Date $StartDate).AddMonths(1).ToString("yyyy-MM-dd")

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  거래 내역 월별 아카이빙" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "대상 기간: $StartDate ~ $EndDate"
Write-Host "백업 디렉토리: $BackupDir"
Write-Host ""

# 백업 디렉토리 생성
if (-not (Test-Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
    Write-Host "[OK] 백업 디렉토리 생성됨" -ForegroundColor Green
}

# Docker 컨테이너 확인
$containerStatus = docker ps --filter "name=$ContainerName" --format "{{.Status}}"
if (-not $containerStatus) {
    Write-Host "[ERROR] MySQL 컨테이너가 실행 중이 아닙니다." -ForegroundColor Red
    exit 1
}

# 환경변수 파일에서 MySQL 비밀번호 읽기
$EnvFile = Join-Path $ProjectRoot ".env.development"
if (-not (Test-Path $EnvFile)) {
    $EnvFile = Join-Path $ProjectRoot ".env"
}

$MysqlPassword = ""
if (Test-Path $EnvFile) {
    $envContent = Get-Content $EnvFile
    foreach ($line in $envContent) {
        if ($line -match "^MYSQL_PASSWORD=(.+)$") {
            $MysqlPassword = $Matches[1]
            break
        }
    }
}

if (-not $MysqlPassword) {
    Write-Host "[ERROR] MySQL 비밀번호를 찾을 수 없습니다." -ForegroundColor Red
    exit 1
}

# 아카이빙 대상 데이터 건수 확인
$CountQuery = @"
SELECT COUNT(*) as cnt FROM transactions 
WHERE created_at >= '$StartDate' AND created_at < '$EndDate';
"@

Write-Host "아카이빙 대상 데이터 확인 중..." -ForegroundColor Yellow
$countResult = docker exec $ContainerName mysql -u crypto_user -p"$MysqlPassword" $Database -N -e "$CountQuery" 2>$null
$recordCount = [int]$countResult.Trim()

Write-Host "대상 레코드 수: $recordCount 건" -ForegroundColor Cyan

if ($recordCount -eq 0) {
    Write-Host "[INFO] 아카이빙할 데이터가 없습니다." -ForegroundColor Yellow
    exit 0
}

if ($DryRun) {
    Write-Host ""
    Write-Host "[DRY RUN] 실제 아카이빙은 수행하지 않습니다." -ForegroundColor Yellow
    exit 0
}

# 백업 파일명
$BackupFile = "transactions_${TargetYear}${TargetMonth.ToString('00')}_$Timestamp.sql"
$BackupPath = Join-Path $BackupDir $BackupFile

# 데이터 백업 (INSERT 문으로 내보내기)
Write-Host ""
Write-Host "Step 1: 데이터 백업 중..." -ForegroundColor Yellow

$ExportQuery = @"
SELECT * FROM transactions 
WHERE created_at >= '$StartDate' AND created_at < '$EndDate';
"@

docker exec $ContainerName mysqldump -u crypto_user -p"$MysqlPassword" $Database transactions `
    --where="created_at >= '$StartDate' AND created_at < '$EndDate'" `
    --no-create-info --compact 2>$null | Out-File -FilePath $BackupPath -Encoding UTF8

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] 데이터 백업 실패" -ForegroundColor Red
    exit 1
}

Write-Host "[OK] 백업 완료: $BackupFile" -ForegroundColor Green

# 압축
Write-Host ""
Write-Host "Step 2: 백업 파일 압축 중..." -ForegroundColor Yellow
$CompressedFile = "$BackupPath.gz"
Compress-Archive -Path $BackupPath -DestinationPath "$BackupPath.zip" -Force
Remove-Item $BackupPath -Force
Rename-Item "$BackupPath.zip" $CompressedFile -Force

$fileSize = (Get-Item $CompressedFile).Length
$fileSizeMB = [math]::Round($fileSize / 1MB, 2)
Write-Host "[OK] 압축 완료: $fileSizeMB MB" -ForegroundColor Green

# 선택적: 원본 데이터 삭제 (주의!)
Write-Host ""
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "  원본 데이터 삭제 여부 확인" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "백업이 완료되었습니다."
Write-Host "원본 데이터를 삭제하시겠습니까?"
Write-Host ""
Write-Host "[WARNING] 삭제 후 복구는 백업 파일로만 가능합니다!" -ForegroundColor Red
Write-Host ""

$confirmation = Read-Host "삭제하려면 'DELETE'를 입력하세요 (취소: Enter)"

if ($confirmation -eq "DELETE") {
    Write-Host ""
    Write-Host "Step 3: 원본 데이터 삭제 중..." -ForegroundColor Yellow
    
    $DeleteQuery = @"
DELETE FROM transactions 
WHERE created_at >= '$StartDate' AND created_at < '$EndDate' 
AND status = 'SOLD';
"@
    
    docker exec $ContainerName mysql -u crypto_user -p"$MysqlPassword" $Database -e "$DeleteQuery" 2>$null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] 매도 완료(SOLD) 거래 내역 삭제 완료" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] 삭제 실패" -ForegroundColor Red
    }
} else {
    Write-Host "[INFO] 삭제를 건너뜁니다. 백업 파일만 생성되었습니다." -ForegroundColor Cyan
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  아카이빙 완료!" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "백업 파일: $CompressedFile"
Write-Host ""