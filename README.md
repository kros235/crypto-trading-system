# 코인 자동매매 시스템

## 📋 프로젝트 개요
- **목적**: 업비트 API를 활용한 개인용 자동매매 시스템
- **규모**: 2명 사용자
- **핵심 가치**: 안정적인 수익 창출 + 최고 수준의 보안
- **개발 기간**: 9주 (Phase 1: 4주, Phase 2: 3주, Phase 3: 2주)

---

## 🏗 기술 스택

### Backend
- Java 17
- Spring Boot 3.2.x
- Spring Security 6
- Spring Data JPA
- MySQL 8.0
- Redis 7.x

### Frontend
- Vue.js 3 (Composition API)
- TypeScript
- Vuetify 3
- Pinia (상태 관리)
- Axios

### Infrastructure
- Docker + Docker Compose
- Nginx (리버스 프록시)

---

## 📅 개발 진행 상황

### ✅ Day 1 (2024-10-11) - 개발환경 구축
**완료 항목:**
- 프로젝트 디렉토리 구조 설계
- Docker Compose 환경 설정 (MySQL, Redis)
- 데이터베이스 스키마 초기 설계 (7개 테이블)
- Git 저장소 설정 및 보안 강화
- 환경변수 템플릿 작성

**결과:**
- 개발 인프라 완성
- 데이터베이스 기본 구조 확립

---

### ✅ Day 2 (2025-10-12) - Spring Boot 프로젝트 초기화
**완료 항목:**
- Spring Boot 3.2.5 프로젝트 생성
- Maven 의존성 설정 (Security, JPA, Redis, JWT, QueryDSL)
- application.yml 설정 (DB, Redis, JWT, Upbit API)
- Spring Security 기본 설정
- HealthController 구현 (시스템 상태 체크)
- Vue.js 프론트엔드 기본 구조 생성
- Docker 이미지 최적화 (Multi-stage build)
- MySQL 초기화 스크립트 완성

**테스트 결과:**
- ✅ MySQL 컨테이너 정상 실행
- ✅ Redis 컨테이너 정상 실행
- ✅ Backend 컨테이너 정상 실행
- ✅ Frontend 컨테이너 정상 실행
- ✅ Health Check API 응답 확인

---

### ✅ Day 3 (2025-10-13) - 사용자 인증 시스템
**완료 항목:**
- JWT 기반 인증 시스템 구현
  - JwtUtil: 토큰 생성/검증
  - JwtAuthenticationFilter: 자동 인증
- 사용자 관리 API
  - 회원가입 (입력값 검증)
  - 로그인 (JWT 발급)
  - 프로필 조회/수정
  - 비밀번호 변경
- API 키 암호화 저장
  - AES-256 암호화
  - 업비트 Access Key/Secret Key 보호
- 전역 예외 처리 핸들러
- CORS 설정

**보안 기능:**
- 비밀번호: BCrypt 해싱 (strength 10)
- API 키: AES-256 암호화
- JWT: HS512 알고리즘, 30분 만료
- 세션: Stateless 관리

**API 엔드포인트:**
- POST /api/auth/signup - 회원가입
- POST /api/auth/login - 로그인
- GET /api/auth/validate - 토큰 검증
- GET /api/user/profile - 프로필 조회
- PUT /api/user/profile - 프로필 수정
- POST /api/user/api-keys - API 키 등록
- DELETE /api/user/api-keys - API 키 삭제

**테스트 완료:**
- ✅ 회원가입 및 토큰 발급
- ✅ 로그인 및 인증
- ✅ 프로필 조회/수정
- ✅ API 키 암호화 저장/삭제

---

### ✅ Day 4 (2025-10-19) - 업비트 API 연동
**완료 항목:**
- UpbitApiService 구현
  - Auth0 JWT로 업비트 API 인증
  - Spring WebClient로 비동기 HTTP 통신
- DTO 클래스 생성
  - UpbitMarketDTO: 마켓 코드
  - UpbitTickerDTO: 현재가 정보
  - UpbitAccountDTO: 계좌 정보
  - UpbitOrderDTO: 주문 정보
- CoinInfo 엔티티 및 Repository
- CoinInfoService: 코인 정보 관리
- 테스트 Controller 생성

**API 테스트:**
- ✅ 마켓 코드 조회 (218개 코인)
- ✅ 현재가 조회 (BTC, ETH)
- ✅ 코인 정보 DB 업데이트
- ✅ 활성 코인 목록 조회

**엔드포인트:**
- GET /api/upbit/test/markets - 마켓 코드
- GET /api/upbit/test/ticker - 현재가
- POST /api/upbit/test/update-coins - DB 업데이트
- GET /api/upbit/test/active-coins - 활성 코인
- GET /api/upbit/test/accounts - 계좌 조회

---

### ✅ Day 5 (2025-10-20) - 거래 설정 및 코인 정보 API
**완료 항목:**
- 거래 설정(Trading Settings) CRUD API
  - TradingSettingDTO (Jakarta Validation)
  - TradingSettingService
  - TradingSettingController
- 코인 정보 조회 API
  - 활성 코인 목록
  - 현재가 조회 (단일/다중)
  - 계좌 정보 조회
- 보안 강화
  - 공개/인증 API 분리
  - 관리자 전용 API (ROLE_ADMIN)

**거래 설정 필드:**
- coinSymbols: 거래할 코인 목록
- basePeriod: 이동평균선 기간 (7~30일)
- buyThresholdPct: 매수 기준 (%)
- sellTargetPct: 목표 수익률 (%)
- stopLossPct: 손절매 기준 (%)
- maxHoldingsPerCoin: 종목당 최대 보유
- dailyLimitAmount: 일일 거래 한도
- useTrailingStop: 트레일링 스톱 사용
- useAiAnalysis: AI 뉴스 분석 사용

**API 엔드포인트:**
| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| GET | /api/trading-settings | ✅ | 설정 조회 |
| POST | /api/trading-settings | ✅ | 설정 생성 |
| PUT | /api/trading-settings | ✅ | 설정 수정 |
| DELETE | /api/trading-settings | ✅ | 설정 삭제 |
| GET | /api/coins/active | ❌ | 활성 코인 목록 |
| GET | /api/coins/{symbol}/price | ❌ | 현재가 조회 |
| GET | /api/coins/prices | ❌ | 다중 현재가 |
| GET | /api/coins/accounts | ✅ | 계좌 정보 |
| POST | /api/coins/update | 🔐 | DB 업데이트 (관리자) |

**테스트 완료:**
- ✅ 거래 설정 생성/조회/수정/삭제
- ✅ 활성 코인 목록 조회
- ✅ 현재가 조회 (단일/다중)
- ✅ 계좌 정보 조회

---

### ✅ Day 6 (2025-10-26) - Vue.js 프론트엔드 핵심 페이지
**완료 항목:**
- 프로젝트 구조 설정
  - TypeScript 타입 시스템
  - Pinia Store (auth, coin)
  - Axios API 클라이언트
  - Vue Router 가드
- 로그인 페이지 (LoginView.vue)
  - 폼 유효성 검증
  - 로딩 상태 표시
  - JWT 토큰 관리
- 회원가입 페이지 (SignupView.vue)
  - 실시간 입력값 검증
  - 비밀번호 강도 체크
  - 전화번호 형식 검증
- 대시보드 (DashboardView.vue)
  - 사용자 정보 카드
  - 시스템 상태 카드
  - 활성 코인 목록 (데이터 테이블)
- 공통 컴포넌트
  - TheHeader: 앱바, 사용자 메뉴
  - TheSidebar: 네비게이션

**Axios 설정:**
- 요청 인터셉터: JWT 토큰 자동 추가
- 응답 인터셉터: 401 에러 시 자동 로그아웃
- API 모듈화: authApi, userApi, coinApi

**라우터 가드:**
- requiresAuth: 인증 필요 페이지 보호
- requiresGuest: 로그인 시 리다이렉션
- 홈(/) → 대시보드 자동 이동

**테스트 계정:**
- ID: admin
- Password: Test1234!@
- Role: ADMIN

---

### ✅ Day 7 (2025-11-09) - 프로필 및 거래 설정 페이지
**완료 항목:**
- 프로필 설정 페이지 (ProfileView.vue)
  - 사용자 정보 조회 (읽기 전용)
  - 이메일/전화번호 수정
  - 폼 유효성 검증
  - 성공/에러 메시지 표시
- 거래 설정 페이지 (TradingSettingsView.vue)
  - 거래 코인 선택 (멀티 셀렉트)
  - 기술적 지표 설정 (이동평균선)
  - 매수 조건 설정
    - 하락률 (음수 입력)
    - 최대 보유 건수
    - 일일 거래 한도
  - 매도 조건 설정
    - 목표 수익률
    - 손절매 기준
    - 트레일링 스톱 (조건부)
  - AI 뉴스 분석 옵션
  - 설정 CRUD 기능
  - 삭제 확인 다이얼로그
- API 클라이언트 확장
  - tradingApi 추가 (CRUD)
  - TypeScript 타입 정의
- 라우터 업데이트
  - 프로필 설정 라우트
  - 거래 설정 라우트
- 사이드바 메뉴 활성화
  - 프로필 설정 메뉴
  - 거래 설정 메뉴

**Validation 규칙:**
- buyThresholdPct: -20 ~ 0 (음수)
- sellTargetPct: 0 ~ 50
- stopLossPct: -30 ~ 0
- basePeriod: 7 ~ 30
- maxHoldingsPerCoin: 최소 1
- dailyLimitAmount: 최소 10,000

**해결한 주요 이슈:**
1. **403 Forbidden 에러**
   - SecurityConfig에 POST /api/trading-settings 명시적 설정
   - JWT 토큰 만료 시간 확인 및 재로그인
2. **400 Bad Request 에러**
   - buyThresholdPct를 음수로 수정 (하락률 의미)
   - Validation 규칙 정확히 구현
3. **프론트엔드 빌드 에러**
   - Vue 템플릿 내 HTML 주석 제거
   - 태그 닫기 오류 수정
4. **Validation 에러**
   - negative 규칙 수정 (0 이하 허용)
   - 타입 변환 로직 강화

**테스트 완료:**
- ✅ 프로필 정보 조회
- ✅ 프로필 정보 수정 (이메일, 전화번호)
- ✅ 거래 설정 생성
- ✅ 거래 설정 조회
- ✅ 거래 설정 수정
- ✅ 거래 설정 삭제
- ✅ Validation 정상 작동
- ✅ 에러 메시지 표시
- ✅ 라우팅 및 네비게이션
- ✅ UI/UX 반응형 디자인

**보류된 테스트:**
- ⏸️ JWT 토큰 만료 (30분 후 테스트 필요)

---

## 📊 현재 진행 상황
- **전체 진척도**: 약 45%
- **Phase 1 (핵심 기능)**: 75% 완료
- **Phase 2 (고도화)**: 25% 진행중
- **Phase 3 (안정화)**: 0%

---

## 🎯 다음 단계 (Day 8)

### 예정 작업:
1. **거래 내역 조회 API**
   - TradeHistory 엔티티
   - 거래 이력 CRUD
   - 필터링 및 정렬
2. **보유 자산 조회 API**
   - Holdings 엔티티
   - 실시간 수익률 계산
3. **대시보드 통계 API**
   - 총 자산 평가액
   - 일별 수익률
   - 거래 통계
4. **실시간 가격 업데이트**
   - WebSocket 또는 폴링
   - 프론트엔드 실시간 갱신

---

## 🚀 실행 방법

### 1. 환경 설정
```bash
# .env 파일 생성 (루트 디렉토리)
cp .env.example .env

# 필수 환경변수 설정
JWT_SECRET_KEY=your_jwt_secret_key_256bits_or_more
JWT_EXPIRATION=1800000
AES_SECRET_KEY=12345678901234567890123456789012
UPBIT_ACCESS_KEY=your_upbit_access_key
UPBIT_SECRET_KEY=your_upbit_secret_key
```

### 2. Docker 실행
```bash
# 전체 서비스 시작
docker-compose up -d --build

# 로그 확인
docker-compose logs -f

# 서비스 중지
docker-compose down
```

### 3. 접속
- **Frontend**: http://localhost
- **Backend API**: http://localhost/api
- **Health Check**: http://localhost/api/health

### 4. 기본 계정
- **관리자**
  - ID: admin
  - Password: Test1234!@

---

## 📁 프로젝트 구조
```
crypto-trading-system/
├── backend/                    # Spring Boot 백엔드
│   ├── src/main/java/com/cryptotrading/
│   │   ├── controller/        # REST API 컨트롤러
│   │   ├── service/           # 비즈니스 로직
│   │   ├── repository/        # 데이터 접근 계층
│   │   ├── entity/            # JPA 엔티티
│   │   ├── dto/               # 데이터 전송 객체
│   │   ├── config/            # 설정 클래스
│   │   ├── filter/            # 필터 (JWT 인증)
│   │   ├── util/              # 유틸리티
│   │   └── exception/         # 예외 처리
│   ├── src/main/resources/
│   │   └── application.yml    # 애플리케이션 설정
│   └── pom.xml                # Maven 의존성
│
├── frontend/                   # Vue.js 프론트엔드
│   ├── src/
│   │   ├── api/               # API 클라이언트
│   │   ├── components/        # Vue 컴포넌트
│   │   ├── views/             # 페이지 컴포넌트
│   │   ├── stores/            # Pinia 상태 관리
│   │   ├── router/            # Vue Router
│   │   ├── types/             # TypeScript 타입
│   │   └── App.vue            # 루트 컴포넌트
│   ├── nginx.conf             # Nginx 설정
│   └── package.json           # npm 의존성
│
├── mysql/
│   └── init.sql               # 데이터베이스 초기화
│
├── docker-compose.yml         # Docker 서비스 설정
├── .env                       # 환경변수 (보안)
└── README.md                  # 프로젝트 문서
```

---

## 🔐 보안 고려사항

### 환경변수 보호
- `.env` 파일을 Git에 커밋하지 않음
- `.env.example`은 템플릿으로 제공
- 프로덕션에서는 더 강력한 비밀키 사용

### 비밀번호 보안
- BCrypt 해싱 (strength 10)
- 단방향 암호화로 원본 복원 불가능
- 솔트 자동 생성

### API 키 보안
- AES-256 암호화
- 환경변수에서 암호화 키 관리
- 데이터베이스에 암호화된 상태로 저장

### JWT 토큰
- HS512 알고리즘
- 30분 만료 시간
- Stateless 인증

### CORS
- localhost:80, localhost:3000만 허용
- 프로덕션에서는 실제 도메인으로 변경

---

## 🐛 트러블슈팅

### 컨테이너 재시작
```bash
docker-compose restart [service-name]
# 예: docker-compose restart backend
```

### 로그 확인
```bash
# 전체 로그
docker-compose logs

# 특정 서비스 로그
docker-compose logs backend

# 실시간 로그
docker-compose logs -f backend
```

### 데이터베이스 초기화
```bash
# 볼륨 삭제 후 재생성
docker-compose down -v
docker-compose up -d
```

### 포트 충돌
```bash
# 사용 중인 포트 확인
netstat -ano | findstr :80
netstat -ano | findstr :8080
netstat -ano | findstr :3306

# 프로세스 종료 (관리자 권한)
taskkill /PID [PID] /F
```

---

## 📝 라이선스
이 프로젝트는 개인 학습 및 연구 목적으로 제작되었습니다.

---

## 📞 연락처
프로젝트 관련 문의사항이 있으시면 이슈를 등록해주세요.