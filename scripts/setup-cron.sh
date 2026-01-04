#!/bin/bash
# Crontab 자동 설정 스크립트
# 신규 서버 배포 시 실행: ./scripts/setup-cron.sh

# SSL 갱신 크론 등록 (매월 1일, 15일 새벽 3시)
(crontab -l 2>/dev/null | grep -v "renew-ssl.sh"; echo "0 3 1,15 * * ~/crypto-trading-system/scripts/renew-ssl.sh >> ~/crypto-trading-system/logs/ssl-renew.log 2>&1") | crontab -

echo "✅ Crontab 등록 완료"
crontab -l