#!/bin/bash
# Crontab 자동 설정 스크립트
# 신규 서버 배포 시 실행: ./scripts/setup-cron.sh

# ⭐ 추가: logs 디렉토리 사전 생성 (크론 리다이렉션 실패 방지 - 근본 원인 재발 방지)
mkdir -p ~/crypto-trading-system/logs

# SSL 갱신 크론 등록 (매월 1일, 15일 새벽 3시)
(crontab -l 2>/dev/null | grep -v "renew-ssl.sh"; echo "0 3 1,15 * * ~/crypto-trading-system/scripts/renew-ssl.sh >> ~/crypto-trading-system/logs/ssl-renew.log 2>&1") | crontab -

# ⭐ 추가: SSL 만료 임박 감시 크론 등록 (매일 새벽 4시 30분)
(crontab -l 2>/dev/null | grep -v "check-ssl-expiry.sh"; echo "30 4 * * * ~/crypto-trading-system/scripts/check-ssl-expiry.sh >> ~/crypto-trading-system/logs/ssl-check.log 2>&1") | crontab -

echo "✅ Crontab 등록 완료"
crontab -l