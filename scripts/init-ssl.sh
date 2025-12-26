#!/bin/bash
# ============================================
# SSL 인증서 발급 스크립트 (Let's Encrypt)
# ============================================
# 생성일: Day 22
# 용도: 도메인 확보 후 SSL 인증서 발급
# 
# 사용 방법:
#   chmod +x scripts/init-ssl.sh
#   ./scripts/init-ssl.sh your-domain.com your-email@example.com
# ============================================

if [ -z "$1" ] || [ -z "$2" ]; then
    echo "사용법: $0 <도메인> <이메일>"
    echo "예시: $0 crypto.example.com admin@example.com"
    exit 1
fi

DOMAIN=$1
EMAIL=$2

echo "=========================================="
echo "SSL 인증서 발급 시작: $DOMAIN"
echo "=========================================="

# 1. Certbot 설치 확인
if ! command -v certbot &> /dev/null; then
    echo "Certbot 설치 중..."
    sudo apt-get update
    sudo apt-get install -y certbot
fi

# 2. SSL 디렉토리 생성
mkdir -p ssl

# 3. 인증서 발급 (Standalone 모드)
echo "인증서 발급 중... (포트 80이 비어있어야 합니다)"
sudo certbot certonly --standalone \
    -d $DOMAIN \
    -d www.$DOMAIN \
    --email $EMAIL \
    --agree-tos \
    --non-interactive

# 4. 인증서 복사
if [ -d "/etc/letsencrypt/live/$DOMAIN" ]; then
    sudo cp /etc/letsencrypt/live/$DOMAIN/fullchain.pem ssl/
    sudo cp /etc/letsencrypt/live/$DOMAIN/privkey.pem ssl/
    sudo chown $USER:$USER ssl/*.pem
    chmod 644 ssl/fullchain.pem
    chmod 600 ssl/privkey.pem
    
    echo "=========================================="
    echo "SSL 인증서 발급 완료!"
    echo "=========================================="
    echo "인증서 위치: ssl/fullchain.pem, ssl/privkey.pem"
    echo ""
    echo "다음 단계:"
    echo "1. docker-compose.prod.yml에서 SSL 볼륨 주석 해제"
    echo "2. frontend/nginx.ssl.conf의 YOUR_DOMAIN.COM을 $DOMAIN으로 변경"
    echo "3. docker-compose -f docker-compose.prod.yml --env-file .env.production up -d"
else
    echo "=========================================="
    echo "SSL 인증서 발급 실패!"
    echo "=========================================="
    echo "도메인 DNS 설정을 확인하세요."
    exit 1
fi