Write-Host "=== Rate Limit 테스트 시작 ===" -ForegroundColor Green
for ($i = 1; $i -le 70; $i++) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost/api/coins/active" -UseBasicParsing -ErrorAction Stop
        $remaining = $response.Headers["X-RateLimit-Remaining"]
        if ($i -ge 50) { 
            Write-Host "요청 $i : 성공 (남은: $remaining)" -ForegroundColor Green 
        }
    } catch {
        Write-Host "요청 $i : 실패 (429)" -ForegroundColor Red
    }
}
Write-Host "=== 테스트 완료 ===" -ForegroundColor Green