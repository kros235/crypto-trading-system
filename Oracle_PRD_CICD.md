* ssh 키 디렉토리 위치 정보  : C:\Projects
* 오라클 클라우드 접속 명령어 : ssh -i ssh-key-2025-12-22.key ubuntu@158.179.161.29
* 프로젝트 디렉토리 이동     : cd ~/crypto-trading-system
git pull origin main
* 기존 운영 컨테이너 다운    : docker compose -f docker-compose.prod.yml --env-file .env.production down
* 새 내용 빌드 후, 컨테이너 기동 : docker compose -f docker-compose.prod.yml --env-file .env.production up -d --build