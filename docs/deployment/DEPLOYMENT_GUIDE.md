# 🚀 배포 절차서

## 프로젝트: 코인 자동매매 시스템 v1.0

---

## 1. 사전 요구사항

### 1.1 서버 환경
- OS: Ubuntu 22.04 LTS (ARM64)
- CPU: 4 OCPU 이상
- RAM: 24GB 이상
- Storage: 100GB 이상

### 1.2 필수 소프트웨어
- Docker 24.x
- Docker Compose 2.x
- Git

### 1.3 네트워크
- 공인 IP 또는 도메인
- 방화벽 포트: 22(SSH), 80(HTTP), 443(HTTPS)

---

## 2. 서버 초기 설정

### 2.1 Docker 설치
```bash
# 패키지 업데이트
sudo apt update && sudo apt upgrade -y

# Docker 설치
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Docker Compose 설치
sudo apt install docker-compose-plugin -y

# 사용자 Docker 그룹 추가
sudo usermod -aG docker $USER
newgrp docker

# 설치 확인
docker --version
docker compose version
```

### 2.2 방화벽 설정
```bash
# UFW 설정
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status
```

### 2.3 Oracle Cloud 보안 목록 설정
1. OCI 콘솔 → 네트워킹 → VCN → 보안 목록
2. 수신 규칙 추가:
   - 22/tcp (SSH)
   - 80/tcp (HTTP)
   - 443/tcp (HTTPS)

---

## 3. 소스 코드 배포

### 3.1 Git Clone
```bash
# 프로젝트 클론
cd /home/ubuntu
git clone https://github.com/your-repo/crypto-trading-system.git
cd crypto-trading-system
```

### 3.2 환경변수 설정
```bash
# .env.production 파일 생성
cp .env.example .env.production

# 환경변수 편집
nano .env.production
```

**.env.production 필수 항목:**
```env
# Database
MYSQL_ROOT_PASSWORD=강력한_비밀번호
MYSQL_DATABASE=crypto_trading
MYSQL_USER=crypto_user
MYSQL_PASSWORD=강력한_비밀번호

# JWT
JWT_SECRET=64자_이상의_랜덤_문자열

# AES 암호화
ENCRYPTION_KEY=32자_AES_키

# Upbit API (옵션)
UPBIT_ACCESS_KEY=your_access_key
UPBIT_SECRET_KEY=your_secret_key

# Groq API
GROQ_API_KEY=your_groq_api_key

# Discord Bot
DISCORD_BOT_TOKEN=your_discord_bot_token

# Email SMTP
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=앱_비밀번호

# Domain
DOMAIN=crypto-trading-prd.duckdns.org
```

---

## 4. SSL 인증서 발급

### 4.1 DuckDNS 도메인 설정
```bash
# DuckDNS 토큰으로 IP 등록
curl "https://www.duckdns.org/update?domains=crypto-trading-prd&token=YOUR_TOKEN&ip="
```

### 4.2 Let's Encrypt 인증서 발급
```bash
# SSL 스크립트 실행
chmod +x scripts/init-ssl.sh
./scripts/init-ssl.sh
```

### 4.3 인증서 자동 갱신 설정
```bash
# Cron 설정 스크립트 실행
chmod +x scripts/setup-cron.sh
./scripts/setup-cron.sh
```

---

## 5. Docker 배포

### 5.1 이미지 빌드 및 실행
```bash
# 운영 환경 Docker Compose 실행
docker compose -f docker-compose.prod.yml --env-file .env.production up -d --build

# 로그 확인
docker compose -f docker-compose.prod.yml logs -f
```

### 5.2 컨테이너 상태 확인
```bash
# 컨테이너 상태
docker ps

# 헬스체크 확인
curl https://crypto-trading-prd.duckdns.org/api/health
```

### 5.3 예상 결과
```json
{
  "status": "UP",
  "profile": "prod",
  "database": "OK",
  "redis": "OK",
  "timestamp": "2026-01-18T12:00:00"
}
```

---

## 6. 초기 데이터 설정

### 6.1 관리자 계정 비밀번호 변경

1. https://crypto-trading-prd.duckdns.org 접속
2. admin / Test1234!@ 로 로그인
3. 프로필 → 비밀번호 변경
4. 강력한 비밀번호로 변경

### 6.2 코인 정보 초기화
```bash
# 코인 정보 업데이트 API 호출
curl -X POST https://crypto-trading-prd.duckdns.org/api/admin/update-coins \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 7. 운영 관리

### 7.1 서비스 재시작
```bash
docker compose -f docker-compose.prod.yml restart
```

### 7.2 서비스 중지
```bash
docker compose -f docker-compose.prod.yml down
```

### 7.3 로그 확인
```bash
# 전체 로그
docker compose -f docker-compose.prod.yml logs -f

# 특정 서비스 로그
docker compose -f docker-compose.prod.yml logs -f backend
```

### 7.4 백업 실행
```bash
# DB 백업 실행
./scripts/backup-db.sh
```

---

## 8. 체크리스트

배포 완료 후 아래 항목을 확인하세요:

- [ ] HTTPS 접속 가능
- [ ] Health Check API 응답 정상
- [ ] 관리자 로그인 성공
- [ ] 관리자 비밀번호 변경 완료
- [ ] Discord Bot 온라인
- [ ] 이메일 테스트 발송 성공
- [ ] 2FA 설정 완료 (관리자)
- [ ] IP 화이트리스트 등록

---

## 📞 문제 발생 시

장애 대응 매뉴얼 참조: `docs/operations/TROUBLESHOOTING.md`