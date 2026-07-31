#!/bin/bash
# ============================================
# SSL 인증서 만료 임박 감시 스크립트
# ============================================
# 용도: renew-ssl.sh가 어떤 이유로든 조용히 실패했을 경우를 대비한 이중 안전장치.
#       매일 실행하여 만료가 14일 이내로 임박했는데도 갱신되지 않았다면
#       매일 경고 알림을 보낸다.
# ============================================

cd ~/crypto-trading-system || exit 1
mkdir -p logs

if [ -f .env.production ]; then
    DISCORD_WEBHOOK_URL=$(grep -E '^DISCORD_WEBHOOK_URL=' .env.production | cut -d '=' -f2-)
fi

DOMAIN="crypto-trading-prd.duckdns.org"
CERT_FILE="/etc/letsencrypt/live/${DOMAIN}/fullchain.pem"
WARNING_THRESHOLD_DAYS=14
LOG_PREFIX="[$(date '+%Y-%m-%d %H:%M:%S KST')]"

notify_discord() {
    local message="$1"
    local color="$2"
    if [ -z "$DISCORD_WEBHOOK_URL" ]; then
        return
    fi
    curl -s -H "Content-Type: application/json" \
        -d "{\"embeds\":[{\"description\":\"${message}\",\"color\":${color}}]}" \
        "$DISCORD_WEBHOOK_URL" > /dev/null 2>&1
}

if [ ! -f "$CERT_FILE" ]; then
    echo "$LOG_PREFIX ❌ 인증서 파일을 찾을 수 없음: $CERT_FILE"
    notify_discord "🚨 **SSL 인증서 파일 없음**\n경로: ${CERT_FILE}\n시간: ${LOG_PREFIX}" 15158332
    exit 1
fi

# 인증서 만료까지 남은 일수 계산
EXPIRY_EPOCH=$(sudo openssl x509 -enddate -noout -in "$CERT_FILE" | cut -d '=' -f2 | xargs -I{} date -d {} +%s)
NOW_EPOCH=$(date +%s)
DAYS_LEFT=$(( (EXPIRY_EPOCH - NOW_EPOCH) / 86400 ))

echo "$LOG_PREFIX 인증서 만료까지 ${DAYS_LEFT}일 남음"

if [ "$DAYS_LEFT" -le 0 ]; then
    notify_discord "🚨🚨 **[긴급] SSL 인증서 이미 만료됨!**\n━━━━━━━━━━━━━━━━━━━━\n도메인: ${DOMAIN}\n즉시 수동 갱신이 필요합니다.\nssh 접속 후: ./scripts/renew-ssl.sh\n시간: ${LOG_PREFIX}" 15158332
elif [ "$DAYS_LEFT" -le "$WARNING_THRESHOLD_DAYS" ]; then
    notify_discord "⚠️ **SSL 인증서 만료 임박**\n━━━━━━━━━━━━━━━━━━━━\n도메인: ${DOMAIN}\n남은 기간: **${DAYS_LEFT}일**\n자동 갱신(1일/15일 새벽 3시)이 정상 동작하는지 확인이 필요합니다.\n시간: ${LOG_PREFIX}" 16776960
fi

exit 0