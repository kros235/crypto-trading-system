자동매매 시스템 프로젝트 지침 (개정판 v3)
- Phase 1: 암호화폐 | Phase 2: 주식/ETF
📋 프로젝트 개요

목적: 개인용 자동매매 시스템 (사용자 5명 규모)
  ★ - Phase 1: 암호화폐 자동매매 (업비트 API)
  ★ - Phase 2: 주식/ETF 자동매매 (한국투자증권 KIS API)
핵심: 안정적인 수익 창출과 최고 수준의 보안
안내 방법:

프로젝트 지침에 명시된대로 제작
개발 환경 구성부터 진행하는 모든 단계를 일단위로 안내
현재까지 진행된 내용을 readme.md 파일로 기록
일별 프로젝트 진행하기 전 readme.md 파일로 진행상황 검토 후 시작

* 서비스 구동 환경 :
차후 해당 프로젝트는, 현재 PC가 아닌 별도의 서버 PC로 구동하거나, 
클라우드에 올려 구동할 예정임.
차후 타서버 이식시에도 절대 경로로 인한 문제가 발생하지 않도록  
디렉토리 지정 작성이 필요한 경우, 상대 경로를 사용하여 문제를 해결하였으면 좋겠음

🎯 거래 전략

📌 Phase 구분

| Phase | 대상 | 거래소/API | 상태 |
|-------|------|------------|------|
| Phase 1 | 암호화폐 (BTC, ETH 등) | 업비트 API | ✅ 완료 (v1.0) |
| Phase 2 | 국내상장 해외ETF | 한국투자증권 KIS API | 🔄 예정 |

※ Phase 1과 Phase 2는 동일한 기술 스택과 거래 전략을 공유하며,
  API 연동 부분만 다르게 구현됨 (코드 재사용률 약 85~90%)

1. 거래 종목 선택

★ [Phase 1 - 암호화폐]
기본: 시가총액 Top 10 코인 (사용자 커스터마이징 가능)
우선순위: BTC, ETH 등 메이저 코인 우선, 알트코인은 제한적 투자

★ [Phase 2 - 주식/ETF] ⭐신규
기본: 국내 상장 해외지수 레버리지 ETF
주요 종목:
  - TIGER 미국나스닥100레버리지(합성) [409820] - 나스닥100 x2, 환노출
  - KODEX 미국나스닥100레버리지(합성H) [409810] - 나스닥100 x2, 환헤지
우선순위: 레버리지 ETF > 일반 ETF > 개별 종목
거래시간: 09:00 ~ 15:30 (KST, 한국 증시)

2. 기준가 산정 방식 (개선된 전략)

기존: 단순 평균가 → 개선: 기술적 지표 조합
기본: 20일 이동평균선 기준
보조지표: RSI(30 이하 매수신호), 볼린저밴드 하단 접촉
거래량: 평균 거래량 대비 150% 이상일 때 매수 활성화
사용자 설정: 7일/14일/30일 기간 선택 가능
AI 뉴스분석 옵션 (Optional):
- API: Groq API (무료, Llama 3.3 70B) - Gemini Rate Limit 문제로 전환
- 실행 주기: 3시간마다 자동 실행 (스케줄러) + 수동 수집 시 즉시 분석
  * 스케줄러: 0, 3, 6, 9, 12, 15, 18, 21시 자동 수집+분석
  * 웹 "뉴스 수집" 버튼: 수집 완료 후 **즉시 AI 분석 실행
- 뉴스 범위: 글로벌 뉴스 위주, KST 기준 당일 발행 뉴스만
- 뉴스 소스: CoinTelegraph, Bitcoin Magazine, Decrypt (무료 RSS 제공, 글로벌 공신력 매체)
  ※ CoinDesk, Reuters는 무료 RSS 미제공으로 제외
- 가중치 적용: 호재/악재 분석 → buyThresholdPct ±0.5% 범위 내 연속적 조정
  * 계산 공식: 가중치(%) = 평균 점수(-1.0~+1.0) × 0.5
  * 예시: 평균 점수 +0.8 → 가중치 +0.4% → 매수 조건 완화
- 초기화: 매일 00:00 KST 가중치 0%로 초기화
- 복합 분석: 호재/악재 동시 존재 시 점수 평균으로 가중치 결정
  * 감성 판단: > +0.2 (호재), -0.2~+0.2 (중립), < -0.2 (악재)
- 알림: 가중치 변경 시 이메일/Discord DM으로 상세 내용 발송
- 데이터 관리: 뉴스 데이터 7일 보관 후 자동 삭제 (매일 04:00 KST 정리)

[Phase 2 - 주식/ETF 전략 조정값]
※ 레버리지 ETF는 암호화폐 대비 변동성이 낮아 임계값 조정 필요

| 항목 | Phase 1 (코인) | Phase 2 (주식) | 비고 |
|------|---------------|---------------|------|
| 매수 기준 하락률 | -6.0% | -3.0% | 변동성 차이 반영 |
| 목표 수익률 | +4.0% | +2.5% | 보수적 설정 |
| 손절매 기준 | -8.0% | -5.0% | 리스크 관리 |
| 트레일링 스톱 | -4.0% | -2.5% | 수익 보존 |
| 거래량 급증 기준 | 140% | 120% | ETF 특성 반영 |
| RSI 매수 신호 | 32 이하 | 35 이하 | 과매도 완화 |
| RSI 매도 신호 | 68 이상 | 65 이상 | 과매수 완화 |

⚠️ 레버리지 ETF 주의사항:
- 장기 보유 시 복리 효과로 인한 가치 침식(decay) 발생 가능
- 권장 보유 기간: 최대 20거래일
- 환노출형(TIGER) vs 환헤지형(KODEX) 선택 고려

3. 매수 조건

기준가 대비 사용자 설정 % 이하 하락 시 매수
종목당 최대 보유 건수 제한 (기본 3건, 사용자 설정 가능)
라운드로빈 방식으로 종목 분산 매수

📌 라운드로빈 매수 방식 상세

[정의]
매수 신호가 발생한 코인들에게 남은 한도를 균등 분배하여 분산 투자하는 방식

[처리 순서]
1단계: 매수 후보 수집
  - 설정된 코인들 중 매수 신호 발생 코인 필터링
  - 리스크 사전 체크 (보유 건수, 비중 제한) 통과 코인만 후보로 선정

2단계: 균등 분배 계산
  - 분배 금액 = 남은 일일 한도 ÷ 매수 후보 수
  - 각 후보별 최대 매수 가능 금액 계산 (비중 제한 반영)

3단계: 최소 금액 체크 및 우선순위 선정
  - 분배 금액 ≥ 최소 매수 금액(5,000원) → 전체 후보 유지
  - 분배 금액 < 최소 매수 금액 → 신호 강도 순 정렬 후 상위 N개만 선정
    * 신호 강도: STRONG(4개 조건 충족) > MEDIUM(3개) > WEAK(2개)
    * 동일 강도 시 이격도(dropRate)가 큰 순서로 우선순위 부여

4단계: 매수 실행
  - 실제 매수 금액 = min(분배 금액, 비중 제한 잔여)
  - 비중 제한으로 남은 금액은 다음 후보에 재분배

[예시: 남은 한도 10만원, ETH/XRP 매수 신호 발생]
- 순차 방식: ETH 10만원 매수, XRP 0원 (한도 소진)
- 라운드로빈: ETH 5만원 + XRP 5만원 (균등 분배)

[예시: 남은 한도 1만원, BTC(MEDIUM)/ETH(STRONG)/SOL(WEAK) 매수 신호 발생]
- 균등 분배 시 3,333원 < 최소 5,000원 → 후보 수 조정 필요
- 매수 가능 코인 수 = 1만원 ÷ 5,000원 = 2개
- 신호 강도 순: ETH(STRONG) > BTC(MEDIUM) > SOL(WEAK)
- 결과: ETH 5,000원 + BTC 5,000원 매수, SOL 제외

4. 매도 조건

목표 수익률 달성 시 자동 매도
추가: 손절매 기능 (-10% 도달 시 강제 매도)
추가: 트레일링 스톱 옵션 (최고가 대비 -5% 하락 시 매도)

5. 리스크 관리

일일 최대 거래 한도 설정
총 투자금 대비 종목별 최대 투자 비율 제한 (20%)
급격한 시장 변동 시 거래 일시 중단 기능
급락장 보호 기능 :
- 시장 추세 필터: BTC 20일선 하회 시 전체 매수 중단 (기본값: OFF)
- 누적 손실률 긴급정지: 초기 자본 대비 누적 손실 도달 시 거래 중단 (기본값: -10%)
- 연속 손절 제한: 동일 코인 연속 손절 시 해당 코인 매수 금지 (기본값: 3회)

💾 데이터베이스 설계 (개선판)
테이블 구조 (sql)
1. users (사용자 테이블)
   - user_id, password_hash, phone, email
   - join_date, last_login, is_active, role(USER/ADMIN)
   - api_key_encrypted, secret_key_encrypted

2. trading_settings (거래 설정 테이블)
   - user_id, coin_symbols[], base_period, buy_threshold_pct
   - sell_target_pct, stop_loss_pct, max_holdings_per_coin
   - daily_limit_amount, use_ai_analysis
   - use_market_trend_filter, cumulative_loss_limit_pct, consecutive_stop_loss_limit**

3. transactions (거래 이력 테이블)
   - transaction_id, user_id, coin_symbol, type(BUY/SELL)
   - quantity, price, fee, total_amount
   - created_at, sold_at, sold_price, profit_loss
   - target_sell_price, stop_loss_price, status

4. coin_info (코인 정보 테이블)  
   - symbol, name_kr, name_en, is_active
   - market_cap_rank, last_updated

5. price_history (가격 이력 테이블)
   - symbol, price, volume, timestamp
   - ma20, rsi, bb_upper, bb_lower

6. daily_summary (일간 집계 테이블)
   - user_id, date, total_profit, profit_rate
   - buy_count, sell_count, total_investment

7. system_logs (시스템 로그 테이블)
   - log_id, user_id, action, message, timestamp, level

[Phase 2 전용 테이블]

8. stock_info (주식/ETF 정보 테이블) [Phase 2]
   - stock_code, stock_name, market(KRX/KOSDAQ)
   - etf_type(LEVERAGE/INVERSE/NORMAL), underlying_index
   - expense_ratio, is_active, last_updated

9. stock_trading_settings (주식 거래 설정 테이블) [Phase 2]
   - user_id, stock_codes[], base_period, buy_threshold_pct
   - sell_target_pct, stop_loss_pct, max_holdings_per_stock
   - daily_limit_amount, max_holding_days
   - kis_app_key_encrypted, kis_app_secret_encrypted

10. stock_transactions (주식 거래 이력 테이블) [Phase 2]
    - transaction_id, user_id, stock_code, type(BUY/SELL)
    - quantity, price, fee, total_amount
    - created_at, sold_at, sold_price, profit_loss
    - exchange_rate (환노출형 ETF용)

인덱스 전략
sql
- transactions: (user_id, created_at), (coin_symbol, status)
- price_history: (symbol, timestamp)
- daily_summary: (user_id, date)

[Phase 2 인덱스]
- stock_transactions: (user_id, created_at), (stock_code, status)
- stock_trading_settings: (user_id)
```

---

### 3️⃣ Phase 2 알림 종류 추가

**현재 알림 종류:**
```
- 매수/매도 체결 즉시 알림
- 일일 수익 리포트 (23:50)
- 시스템 오류, 거래 한도 도달
- 로그인 이상 행위 감지
```

**추가 권장:**
```
- 매수/매도 체결 즉시 알림
- 일일 수익 리포트 (23:50)
- 시스템 오류, 거래 한도 도달
- 로그인 이상 행위 감지

[Phase 2 추가 알림]
- 레버리지 ETF 보유기간 경고 (15일/20일 도달 시)
- 장 시작/마감 알림 (08:50, 15:20)
- 휴장일 전일 알림
```

---

### 4️⃣ 서비스 접속 정보 - Phase 2 추가 (선택)

Phase 2 개발 시 별도 테스트 환경이 필요할 수 있어요:
```
[Phase 2 모의투자 환경]
- KIS 모의투자 포털: https://apiportal.koreainvestment.com
- 모의투자 계좌: 별도 신청 필요
- API 테스트: 실제 시세, 가상 거래
```

---

### 5️⃣ 백테스팅 섹션 신규 추가 (선택)

Phase 2에서 백테스팅이 중요하므로 별도 섹션으로 분리하는 것도 좋아요:
```
📊 백테스팅 시스템

[Phase 1 - 암호화폐]
- 데이터 소스: 업비트 과거 시세 API
- 테스트 기간: 최대 200일
- 수수료 반영: 0.05% (매수/매도 각각)

[Phase 2 - 주식/ETF]
- 데이터 소스: KIS API 일봉 데이터 / Yahoo Finance
- 테스트 기간: 최대 1년
- 수수료 반영: 0.015% (매수/매도 각각)
- 슬리피지: 0.05%

[공통 분석 지표]
- 총 수익률, 승률, MDD
- 샤프 비율, 평균 보유기간
- 월별/분기별 수익률 분포


🔒 보안 강화 방안
API 키 관리

저장: AES-256 암호화하여 DB 저장
환경: 암호화 키는 환경변수로 분리
접근: API 키는 거래 시에만 복호화하여 사용

인증 및 권한

로그인: JWT + 2FA (Google Authenticator)
세션: 30분 자동 만료, 이상 접속 시 강제 로그아웃
통신: HTTPS 필수, API 요청 서명 검증
접근제어: IP 화이트리스트 (최대 3개 IP)

⭐ IP 화이트리스트 Docker 네트워크 설정 

* 문제: Docker Bridge 네트워크에서 모든 클라이언트 IP가 `172.18.0.1` (Gateway IP)로 표시되어 
         IP 화이트리스트가 정상 작동하지 않음

* 원인: Docker iptables NAT로 인해 원본 클라이언트 IP가 손실됨

* 해결: Frontend(Nginx) 컨테이너를 `network_mode: host`로 변경 (운영 환경만)

**환경별 설정 분리**:
| 환경 | Docker Compose | Nginx 설정 | 네트워크 모드 | proxy_pass | IP 표시 |
|------|----------------|------------|--------------|------------|---------|
| * 개발 (Windows/Mac) | docker-compose.yml | nginx.conf | bridge | backend:8080 | Docker IP (172.18.0.1) |
| * 운영 (Linux) | docker-compose.prod.yml | nginx.ssl.conf | host | 127.0.0.1:8080 | 실제 클라이언트 IP ✅ |

**제약사항**:
- Windows Docker Desktop (WSL2)에서 `network_mode: host` 미지원
- 개발 환경에서는 IP 화이트리스트 실제 IP 테스트 불가 (172.18.0.1로 표시)
- 실제 IP 테스트는 운영 서버(Oracle Cloud Linux)에서만 가능

거래 보안

거래 한도: 일일 최대 거래금액 제한
이상 감지: 평소 패턴과 다른 거래 시 알림
긴급 정지: 수동 거래 중단 기능

🏗 기술 스택 (소규모 최적화)
yamlBackend:
  - Java 17 + Spring Boot 3.2.x
  - Spring Security 6 + JWT
  - Spring Data JPA + QueryDSL 5
  - Redis 7.x (캐싱, 세션)

Database:
  - MySQL 8.0 (Master 1대)
  - Redis 7.x

External APIs:
  - 업비트 API (WebSocket + REST) [Phase 1]
  - 한국투자증권 KIS API (REST + WebSocket) [Phase 2] 
  - Groq API (뉴스 분석 - Llama 3.3 70B, 무료)

[Phase 2 - 한국투자증권 KIS API 정보]
- API 방식: REST API + WebSocket (OS 무관, 리눅스 서버 지원)
- API 사용료: 무료
- 거래 수수료: 국내주식 0.0036396%, 해외주식 0.07% (이벤트 적용 시)
- 모의투자: 지원 (5억원 가상자금)
- 개발자 포털: https://apiportal.koreainvestment.com
- GitHub 샘플: https://github.com/koreainvestment/open-trading-api

Frontend:
  - Vue.js 3 + TypeScript
  - Vite (빌드 도구)
  - Chart.js (차트)
  - Vuetify 3 (UI 컴포넌트)
  - Pinia (상태 관리)

Infrastructure:
  - Docker + Docker Compose
  - Nginx (Reverse Proxy)
  - Let's Encrypt (SSL)

Monitoring:
  - Spring Boot Actuator
  - 로그: Logback + 파일 로테이션
📱 알림 시스템
푸시 알림 (단계적 구현)

1단계: 이메일 알림 (SMTP)
2단계: 텔레그램 봇 연동
3단계: Firebase FCM (모바일 앱)

알림 종류

매수/매도 체결 즉시 알림
일일 수익 리포트 (23:50)
시스템 오류, 거래 한도 도달
로그인 이상 행위 감지

🌐 서비스 접속 정보
운영 환경 (Production)

| 서비스 | URL |
|--------|-----|
| Frontend (HTTPS) | https://crypto-trading-prd.duckdns.org |
| Backend API | https://crypto-trading-prd.duckdns.org/api |
| Health Check | https://crypto-trading-prd.duckdns.org/api/health |
| Swagger UI | https://crypto-trading-prd.duckdns.org/swagger-ui/index.html |

- 도메인: DuckDNS 무료 도메인 (crypto-trading-prd.duckdns.org)
- SSL: Let's Encrypt (90일 자동 갱신)
- 서버: Oracle Cloud ARM64 (158.179.161.29)
- Docker: `network_mode: host`로 실제 클라이언트 IP 전달

개발 환경 (Development)

| 서비스 | URL |
|--------|-----|
| Frontend | http://localhost |
| Backend API | http://localhost/api |
| Health Check | http://localhost/api/health |
| Swagger UI | http://localhost/swagger-ui/index.html |
| MySQL | localhost:3306 |
| Redis | localhost:6379 |

⭐ 개발 환경 주의사항:
- Docker Bridge 네트워크 사용으로 IP 화이트리스트 테스트 시 `172.18.0.1`로 표시됨
- 실제 클라이언트 IP 기반 테스트는 운영 환경에서만 가능
- `network_mode: host`는 Linux에서만 완전 지원 (Windows/Mac 미지원)


🌐 웹페이지 구성
📄 공통 페이지
├── 홈페이지 (대시보드)
├── 로그인/2FA 인증
└── 회원가입
👤 사용자 페이지
├── 거래 현황 (실시간)
├── 거래 이력 조회
├── 수익률 차트
├── 설정 관리
│   ├── 코인 거래 설정 [Phase 1]
│   └── 주식 거래 설정 [Phase 2] ⭐신규
├── 보안 설정
├── 코인 뉴스 
├── 보유자산 (기간별/코인별 수익 분석)
└── 주식/ETF 포트폴리오 [Phase 2] ⭐신규
     ├── ETF 보유 현황
     ├── 수익률 분석 (환율 영향 포함)
     └── 보유기간 모니터링 (레버리지 ETF decay 경고)
🔧 관리자 페이지
├── 사용자 관리
├── 시스템 모니터링  
└── 거래 통계

코인 뉴스 페이지 상세              
├── 기능: 수집된 코인 뉴스를 게시판 형식으로 조회
├── 페이징: 페이지당 10/20/50건 선택 가능
├── 필터링: 코인 심볼별 필터 (BTC, ETH, XRP 등)
├── 검색: 제목/내용 키워드 검색
└── 정렬: 최신순/오래된순

보유자산 수익 분석 페이지 상세
├── 투자 수익 확인
│   ├── 오늘 수익 (금액, %)
│   ├── 이번달 수익 (금액, %)
│   └── 올해 수익 (금액, %)
├── 기간별 수익 분석
│   ├── 탭 선택: 오늘 / 이번달 / 올해 / 1년 / 누적
│   ├── 선택 기간 총 수익금액
│   └── 선택 기간 수익률 (%)
└── 코인별 수익 분석
    ├── 보유 코인 목록
    ├── 코인별 수익금액/수익률
    └── 코인별 상세 분석 버튼

=== Phase 1: 암호화폐 자동매매 (완료 ✅) ===

Phase 1-1: 핵심 기능 (4주) ✅
- 업비트 API 연동 및 인증
- 기본 매수/매도 로직
- 데이터베이스 구축
- 보안 로그인 시스템

Phase 1-2: 고도화 (3주) ✅
- Vue.js 대시보드 구축
- 실시간 모니터링
- 이메일 알림 시스템
- 백테스팅 기능

Phase 1-3: 안정화 (2주) ✅
- 예외처리 강화
- 보안 점검 및 개선
- 성능 최적화
- 운영 문서 작성

=== Phase 2: 주식/ETF 자동매매 (예정 🔄) ===

Phase 2-1: 기반 구축 (2주)
- 한국투자증권 KIS API 연동
- 주식용 DB 테이블 생성
- API 인증 및 토큰 관리
- 모의투자 환경 구성

Phase 2-2: 핵심 기능 (3주)
- 주식 매수/매도 로직 (Phase 1 코드 재사용)
- 거래 시간 스케줄러 (09:00~15:30)
- 휴장일 캘린더 처리
- 기술적 지표 계산 (MA, RSI, BB)

Phase 2-3: 고도화 (2주)
- 대시보드 확장 (주식 탭 추가)
- ETF 전용 기능 (보유기간 제한, 환율 표시)
- 백테스팅 기능 확장
- 알림 시스템 통합

Phase 2-4: 안정화 (1주)
- 통합 테스트
- Phase 1/2 전환 기능
- 문서화 및 배포

⚠️ 주요 고려사항
법적/규제

가상자산 관련 세무 처리 방법 숙지
거래 내역 5년 보관 의무

운영 관리

매일 새벽 4시 시스템 점검 시간
주요 업비트 점검 시간 대응 방안
긴급상황 시 수동 개입 절차

백업 전략

DB 일일 백업 (7일 보관)
API 키 및 설정 정보 별도 암호화 보관
거래 로그 월별 아카이빙

Phase 2 (주식/ETF) 추가 고려사항

법적/세금:
- 국내상장 ETF 매매차익: 배당소득세 15.4%
- ISA 계좌 활용 시 절세 가능 (200만원 비과세, 초과분 9.9% 분리과세)
- 해외주식 직접투자 시: 양도소득세 22% (250만원 초과분)

거래 시간:
- 한국 증시: 09:00 ~ 15:30 (정규장)
- 시간외 거래: 08:30~09:00, 15:40~16:00
- 휴장일: 주말, 공휴일, 임시휴장일

레버리지 ETF 리스크:
- 변동성 끌림(Volatility Drag): 횡보장에서 손실 누적
- 장기 보유 비권장: 최대 20거래일 내 청산 권장
- 환노출형 vs 환헤지형 선택에 따른 수익률 차이 (최대 20%p)

API 제한:
- 한국투자증권 KIS API 호출 제한: 초당 20건
- 실시간 시세 WebSocket 연결 수 제한
- 3개월 미거래 시 API 서비스 자동 해지


🚀 일별 진행 계획
프로젝트 진행은 다음과 같이 일별로 체계적으로 진행하며, 매일의 진행사항을 README.md 파일에 기록하여 추적 관리합니다.
Readme.md 파일에 기록되는 Day XX 에서 XX의 값은, 사용자와의 첫 대화에서 사용자가 제공하게 됩니다. 실제 날짜 정보와 관계없이, 사용자가 말하는 작업일자로 기록해주세요.
진행 방식: 각 일차 시작 전 README.md 검토 → 당일 목표 설정 → 프로젝트 지침에 연결된 github 내용 확인을 통한 진척정도 확인 →개발 진행 → 결과 기록 → 다음 일차 준비

[작업 진행시, 프롬프트 안내 방법]
1. 새로운 파일 또는 디렉토리를 생성할 때에는, 
    - 반드시 생성되는 경로를 먼저 별도로 명시한다. 
    - 생성해야하는 파일의 디렉토리가 없을 경우, 디렉토리 생성 명령어를 먼저 알려준다.
    - 이후에 생성되는 파일 내부 내용을 이야기한다.

2. 기존 파일 또는 디렉토리를 수정할 때에는, 
    - 반드시 수정되는 경로를 먼저 별도로 명시한다. 그 이후에 수정되는 내용 강조하여 따로 표기한다.

3. 윈도우 10환경에서 개발 중이며, powershell 을 이용해서 작업 중임.
4. choco 명령어는 사용할 수 없음
5. 지침 말미에 '지금까지 진행한 내용들이, 전체 프로젝트 목표에서 몇 프로 정도 진행된지 알려줘
6. 꼭 필요한 내용이 아니면, 기존 내용을 수정하지 않는다. 
7. 수정된 내용은 반드시 강조표시해주고, 수정되는 위치를 알기 쉽게 앞 뒤내용을 보여준다.
8. 바뀌는 부분은 왜 바뀌어야하는지도 설명한다.