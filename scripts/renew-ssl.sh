#!/bin/bash
# ============================================
# SSL 인증서 자동 갱신 스크립트
# ============================================
# ⭐ 수정: 실패 감지 + Discord 알림 + 로그 디렉토리 자동 생성 추가
# 기존 문제: logs/ 디렉토리가 없으면 크론의 리다이렉션(>>)이 실패해
#           스크립트 본문 자체가 실행되지 않음 (수개월간 갱신 미실행의 원인)
# 해결: ① mkdir -p logs 로 디렉토리 사전 생성
#       ② 각 단계마다 exit code 확인
#       ③ 실패 시 Discord Webhook으로 즉시 알림 (백엔드 상태와 무관하게 curl 직접 호출)
# ============================================

cd ~/crypto-trading-system || exit 1

# ⭐ 추가: 로그 디렉토리 없으면 생성 (근본 원인 해결)
mkdir -p logs

# ⭐ 추가: .env.production에서 Discord Webhook URL 로드
if [ -f .env.production ]; then
    DISCORD_WEBHOOK_URL=$(grep -E '^DISCORD_WEBHOOK_URL=' .env.production | cut -d '=' -f2-)
fi

DOMAIN="crypto-trading-prd.duckdns.org"
LOG_PREFIX="[$(date '+%Y-%m-%d %H:%M:%S KST')]"

# ⭐ 추가: Discord 알림 함수
notify_discord() {
    local message="$1"
    local color="$2"   # 성공: 3066993(초록), 실패: 15158332(빨강)

    if [ -z "$DISCORD_WEBHOOK_URL" ]; then
        echo "$LOG_PREFIX Discord Webhook URL 미설정, 알림 스킵"
        return
    fi

    curl -s -H "Content-Type: application/json" \
        -d "{\"embeds\":[{\"description\":\"${message}\",\"color\":${color}}]}" \
        "$DISCORD_WEBHOOK_URL" > /dev/null 2>&1
}

echo "=========================================="
echo "$LOG_PREFIX SSL 인증서 갱신 시작"
echo "=========================================="

# 1. 프론트엔드(Nginx) 컨테이너 중지 (포트 80 확보)
docker compose -f docker-compose.prod.yml --env-file .env.production stop frontend
if [ $? -ne 0 ]; then
    echo "$LOG_PREFIX ❌ 프론트엔드 컨테이너 중지 실패"
    notify_discord "🚨 **SSL 갱신 실패**\n━━━━━━━━━━━━━━━━━━━━\n단계: 컨테이너 중지\n시간: ${LOG_PREFIX}\n서버에 직접 접속하여 확인이 필요합니다." 15158332
    exit 1
fi

# 2. 인증서 갱신
sudo certbot renew --quiet
CERTBOT_EXIT_CODE=$?
if [ $CERTBOT_EXIT_CODE -ne 0 ]; then
    echo "$LOG_PREFIX ❌ certbot renew 실패 (exit code: $CERTBOT_EXIT_CODE)"
    notify_discord "🚨 **SSL 갱신 실패**\n━━━━━━━━━━━━━━━━━━━━\n단계: certbot renew\nExit Code: ${CERTBOT_EXIT_CODE}\n시간: ${LOG_PREFIX}\n서버 로그 확인: /var/log/letsencrypt/letsencrypt.log" 15158332
    # 실패해도 프론트엔드는 반드시 재기동해야 서비스 중단을 막을 수 있음
    docker compose -f docker-compose.prod.yml --env-file .env.production start frontend
    exit 1
fi

# 3. 인증서 파일 복사
sudo cp /etc/letsencrypt/live/${DOMAIN}/fullchain.pem ssl/ && \
sudo cp /etc/letsencrypt/live/${DOMAIN}/privkey.pem ssl/ && \
sudo chown ubuntu:ubuntu ssl/*.pem
if [ $? -ne 0 ]; then
    echo "$LOG_PREFIX ❌ 인증서 파일 복사 실패"
    notify_discord "🚨 **SSL 갱신 실패**\n━━━━━━━━━━━━━━━━━━━━\n단계: 인증서 파일 복사\n시간: ${LOG_PREFIX}" 15158332
    docker compose -f docker-compose.prod.yml --env-file .env.production start frontend
    exit 1
fi

# 4. 컨테이너 재시작
docker compose -f docker-compose.prod.yml --env-file .env.production start frontend
if [ $? -ne 0 ]; then
    echo "$LOG_PREFIX ❌ 프론트엔드 컨테이너 재시작 실패 - 서비스 중단 상태일 수 있음!"
    notify_discord "🚨🚨 **[긴급] 프론트엔드 재기동 실패**\n━━━━━━━━━━━━━━━━━━━━\n인증서는 갱신됐으나 컨테이너 재시작에 실패했습니다.\n서비스가 중단되었을 수 있으니 즉시 확인이 필요합니다!\n시간: ${LOG_PREFIX}" 15158332
    exit 1
fi

# 5. 최종 만료일 확인 후 성공 알림
NEW_EXPIRY=$(sudo certbot certificates -d ${DOMAIN} 2>/dev/null | grep "Expiry Date" | awk -F': ' '{print $2}')
echo "$LOG_PREFIX ✅ SSL 인증서 갱신 완료 (신규 만료일: ${NEW_EXPIRY})"
notify_discord "✅ **SSL 인증서 갱신 성공**\n━━━━━━━━━━━━━━━━━━━━\n도메인: ${DOMAIN}\n신규 만료일: ${NEW_EXPIRY}\n시간: ${LOG_PREFIX}" 3066993

exit 0