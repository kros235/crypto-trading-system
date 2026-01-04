#!/bin/bash
# SSL 인증서 자동 갱신 스크립트
cd ~/crypto-trading-system

# 컨테이너 중지
docker compose -f docker-compose.prod.yml --env-file .env.production stop frontend

# 인증서 갱신
sudo certbot renew --quiet

# 인증서 복사
sudo cp /etc/letsencrypt/live/crypto-trading-prd.duckdns.org/fullchain.pem ssl/
sudo cp /etc/letsencrypt/live/crypto-trading-prd.duckdns.org/privkey.pem ssl/
sudo chown ubuntu:ubuntu ssl/*.pem

# 컨테이너 재시작
docker compose -f docker-compose.prod.yml --env-file .env.production start frontend

echo "[$(date)] SSL 인증서 갱신 완료"