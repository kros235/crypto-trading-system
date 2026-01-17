# 🔧 장애 대응 매뉴얼

## 프로젝트: 코인 자동매매 시스템 v1.0

---

## 1. 일반적인 문제 해결

### 1.1 서비스 접속 불가

**증상**: https://crypto-trading-prd.duckdns.org 접속 안됨

**원인 파악 순서:**
```bash
# 1. 컨테이너 상태 확인
docker ps -a

# 2. 특정 컨테이너 로그 확인
docker logs crypto-trading-backend-prod
docker logs crypto-trading-frontend-prod

# 3. 네트워크 상태 확인
curl -v https://crypto-trading-prd.duckdns.org
```

**해결 방법:**
```bash
# 컨테이너 재시작
docker compose -f docker-compose.prod.yml restart

# 전체 재배포
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml up -d --build
```

---

### 1.2 데이터베이스 연결 오류

**증상**: "Database connection failed" 또는 Health Check DB: "ERROR"

**원인 파악:**
```bash
# MySQL 컨테이너 상태
docker logs crypto-trading-mysql-prod

# MySQL 연결 테스트
docker exec -it crypto-trading-mysql-prod mysql -u crypto_user -p
```

**해결 방법:**
```bash
# MySQL 컨테이너만 재시작
docker restart crypto-trading-mysql-prod

# 연결 대기 후 백엔드 재시작
sleep 10
docker restart crypto-trading-backend-prod
```

---

### 1.3 Redis 연결 오류

**증상**: "Redis connection failed"

**해결 방법:**
```bash
# Redis 컨테이너 재시작
docker restart crypto-trading-redis-prod

# Redis 연결 테스트
docker exec -it crypto-trading-redis-prod redis-cli ping
```

---

### 1.4 SSL 인증서 만료

**증상**: 브라우저에서 "인증서가 유효하지 않음" 경고

**해결 방법:**
```bash
# 인증서 갱신
./scripts/renew-ssl.sh

# Nginx 재시작
docker restart crypto-trading-frontend-prod
```

---

## 2. 자동매매 관련 문제

### 2.1 봇이 실행되지 않음

**증상**: 봇 상태가 "중지됨"으로 표시

**원인 파악:**
```bash
# 백엔드 로그에서 스케줄러 확인
docker logs crypto-trading-backend-prod | grep -i scheduler
docker logs crypto-trading-backend-prod | grep -i trading
```

**해결 방법:**
1. 웹 UI에서 봇 상태 확인
2. "시작" 버튼 클릭
3. 거래 설정이 저장되어 있는지 확인
4. API 키가 등록되어 있는지 확인

---

### 2.2 업비트 API 오류

**증상**: "Upbit API Error: 401" 또는 "429 Too Many Requests"

**원인:**
- 401: API 키가 잘못되었거나 만료됨
- 429: API 호출 한도 초과

**해결 방법:**

**401 오류:**
1. 업비트에서 새 API 키 발급
2. 프로필 → API 키 재등록

**429 오류:**
1. 잠시 대기 (1-5분)
2. 자동 재시도 로직이 작동함

---

### 2.3 거래 체결 안됨

**증상**: 매수/매도 조건 충족했으나 체결 안됨

**원인 파악:**
```bash
# 거래 로그 확인
docker logs crypto-trading-backend-prod | grep -i order
docker logs crypto-trading-backend-prod | grep -i trade
```

**가능한 원인:**
1. 일일 거래 한도 초과
2. 종목별 최대 보유 건수 초과
3. 잔고 부족
4. 급락장 보호 기능 작동

---

## 3. 알림 관련 문제

### 3.1 Discord DM 안옴

**원인 파악:**
```bash
# Discord Bot 상태 확인
curl https://crypto-trading-prd.duckdns.org/api/notifications/discord/bot-status \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**해결 방법:**
1. Discord Bot Token 확인
2. Discord User ID 정확히 입력했는지 확인
3. Discord 서버에서 DM 허용 설정 확인

---

### 3.2 이메일 안옴

**원인 파악:**
```bash
# 백엔드 로그에서 이메일 확인
docker logs crypto-trading-backend-prod | grep -i email
docker logs crypto-trading-backend-prod | grep -i mail
```

**해결 방법:**
1. SMTP 설정 확인 (.env.production)
2. Gmail 앱 비밀번호 사용 여부 확인
3. 스팸함 확인

---

## 4. 데이터 백업 및 복원

### 4.1 DB 백업
```bash
# 수동 백업 실행
./scripts/backup-db.sh

# 백업 파일 확인
ls -la backups/mysql/
```

### 4.2 DB 복원
```bash
# 특정 백업 파일로 복원
./scripts/restore-db.sh backups/mysql/crypto_trading_20260118_120000.sql

# 주의: 기존 데이터가 덮어써집니다!
```

---

## 5. 성능 문제

### 5.1 응답 속도 느림

**원인 파악:**
```bash
# JVM 메모리 상태 확인
curl https://crypto-trading-prd.duckdns.org/api/admin/monitoring \
  -H "Authorization: Bearer ADMIN_TOKEN"

# 컨테이너 리소스 사용량
docker stats
```

**해결 방법:**
```bash
# 컨테이너 재시작
docker compose -f docker-compose.prod.yml restart backend

# Redis 캐시 초기화 (필요시)
docker exec crypto-trading-redis-prod redis-cli FLUSHALL
```

---

## 6. 긴급 상황 대응

### 6.1 전체 서비스 긴급 중지
```bash
# 모든 컨테이너 즉시 중지
docker compose -f docker-compose.prod.yml stop
```

### 6.2 거래만 긴급 중지

1. 웹 UI 로그인
2. 대시보드 → 봇 상태 → "중지" 클릭
3. 또는 API 호출:
```bash
curl -X POST https://crypto-trading-prd.duckdns.org/api/bot/stop \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 6.3 긴급 복구 절차

1. 서비스 중지
2. 최신 백업 확인
3. 문제 원인 파악
4. DB 복원 (필요시)
5. 서비스 재시작
6. 정상 동작 확인

---

## 7. 연락처

- **시스템 관리자**: [관리자 연락처]
- **Discord 서버**: [서버 링크]
- **이메일**: [관리자 이메일]

---

## 8. 관련 문서

- 배포 절차서: `docs/deployment/DEPLOYMENT_GUIDE.md`
- 아키텍처 문서: `docs/architecture/ARCHITECTURE.md`
- 보안 체크리스트: `docs/security/OWASP_CHECKLIST.md`