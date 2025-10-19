# 코인 자동매매 시스템

## 📋 프로젝트 개요
- **목적**: 업비트 API를 활용한 개인용 자동매매 시스템 (사용자 2명 규모)
- **핵심**: 안정적인 수익 창출과 최고 수준의 보안
- **개발 기간**: 9주 (Phase 1: 4주, Phase 2: 3주, Phase 3: 2주)

## 🏗 기술 스택
- **Backend**: Java 17 + Spring Boot 3.2.x, Spring Security 6, JPA
- **Database**: MySQL 8.0, Redis 7.x
- **Frontend**: Vue.js 3 + TypeScript, Vuetify 3
- **Infrastructure**: Docker + Docker Compose, Nginx

---

## 📅 일별 진행 상황

### Day 1 (2024-10-11) - 개발환경 구축 및 프로젝트 초기화
**목표**: 
- [x] 개발환경 구축 (Java 17, Node.js, Docker)
- [x] 프로젝트 기본 구조 생성
- [x] Git 저장소 초기화
- [x] Docker 환경 설정

**진행 내용**:
- [x] 개발환경 요구사항 정의
- [x] 프로젝트 디렉토리 구조 설계
- [x] Docker 환경 설정 파일 작성
- [x] 데이터베이스 스키마 초기 설계
- [x] Git 저장소 설정 파일 준비

**완료된 작업**:
1. Java 17, Node.js, Docker 설치 가이드 제공
2. 프로젝트 전체 디렉토리 구조 설계
3. Docker Compose 설정 (MySQL 8.0, Redis 7.x)
4. 환경변수 템플릿 (.env.example) 작성
5. 데이터베이스 초기 스키마 설계 (7개 테이블)
6. Git 저장소 설정 (.gitignore, 보안 강화)

**내일 준비사항**:
- 개발환경 설치 완료 확인
- Spring Boot 프로젝트 생성을 위한 Maven 설정 준비

**다음 일정**: Day 2 - Spring Boot 프로젝트 생성 및 기본 설정

---

### Day 2 (2025-10-12) - Spring Boot 프로젝트 생성 및 기본 설정 ✅
**목표**: 
- [x] Spring Boot 프로젝트 생성 (Maven)
- [x] 기본 의존성 설정
- [x] 애플리케이션 설정 파일 구성
- [x] Docker 환경 통합
- [x] 기본 Security 설정
- [x] 헬스체크 API 구현

**진행 내용**:
- [x] backend 디렉토리 및 Maven 프로젝트 구조 생성
- [x] pom.xml 의존성 설정 (Spring Boot 3.2.5, Security, JPA, Redis, JWT 등)
- [x] application.yml 및 application-dev.yml 설정
- [x] 기본 패키지 구조 생성 (controller, service, repository, entity, dto, config 등)
- [x] CryptoTradingApplication 메인 클래스 작성
- [x] Spring Security 기본 설정 (SecurityConfig, WebMvcConfig)
- [x] HealthController 구현 (DB, Redis 연결 상태 체크)
- [x] backend Dockerfile 작성 및 최적화
- [x] frontend 기본 구조 생성 (Vue 3 + TypeScript + Vuetify)
- [x] frontend Dockerfile 및 nginx 설정
- [x] docker-compose.yml 수정 (healthcheck 추가)
- [x] MySQL 초기화 스크립트 완성 (7개 테이블 + 초기 데이터)
- [x] QueryDSL 설정 문제 해결
- [x] MySQL 사용자 권한 문제 해결

**완료된 작업**:
1. **Backend 설정**
   - Spring Boot 3.2.5 프로젝트 생성
   - 주요 의존성: Spring Security 6, JPA, Redis, JWT, QueryDSL, WebSocket
   - application.yml: MySQL, Redis, JWT, Upbit API 설정
   - Security 설정: 공개 엔드포인트 (/api/health, /api/auth/**)
   - CORS 설정: 프론트엔드 연동 준비
   - QueryDSL 설정: Compiler plugin에 통합하여 안정화
   
2. **Frontend 설정**
   - Vue 3 + TypeScript + Vite 프로젝트 구조
   - Vuetify 3 UI 프레임워크 설정
   - Nginx 리버스 프록시 설정 (API, WebSocket)
   - 기본 라우팅 및 상태관리 구조
   
3. **Database 설정**
   - 완전한 스키마 설계 (7개 테이블)
   - 초기 코인 정보 데이터 (Top 10 코인)
   - 기본 관리자 계정 생성 (admin/admin123!)
   - 인덱스 최적화 및 외래키 설정
   - 사용자 권한 설정 완료
   
4. **Docker 환경**
   - Multi-stage 빌드로 이미지 최적화
   - 서비스 간 healthcheck 설정
   - 볼륨 마운트로 데이터 영속성 확보
   - 네트워크 설정으로 서비스 간 통신

**테스트 결과**:
- ✅ MySQL 컨테이너 정상 실행 (포트 3306)
- ✅ Redis 컨테이너 정상 실행 (포트 6379)
- ✅ Backend 컨테이너 정상 실행 (포트 8080)
- ✅ Frontend 컨테이너 정상 실행 (포트 80)
- ✅ Health Check API 응답 확인
```json
  {
    "status": "UP",
    "database": {"status": "UP", "database": "MySQL"},
    "redis": {"status": "UP"}
  }

---

### Day 3 (2025-10-13) - 사용자 인증 시스템 구현 ✅

**목표**: 
- [x] JWT 기반 인증 시스템 완성
- [x] 사용자 관리 기능 구현
- [x] API 키 암호화 저장 기능
- [x] 보안 강화 (입력값 검증, 예외 처리)

**진행 내용**:
- [x] JWT 유틸리티 및 필터 구현
- [x] AuthService, UserService 구현
- [x] AuthController, UserController 구현
- [x] 비밀번호 BCrypt 암호화
- [x] API 키 AES-256 암호화 저장
- [x] 입력값 검증 (Jakarta Validation)
- [x] 전역 예외 처리 핸들러
- [x] CORS 설정 완료

**완료된 작업**:

1. 인증 시스템
   - 회원가입 API: POST /api/auth/signup
     - 사용자 ID, 비밀번호, 이메일, 전화번호 입력 검증
     - 비밀번호 강도 체크 (대소문자, 숫자, 특수문자 포함 8-30자)
     - 전화번호 형식 검증 (010-XXXX-XXXX)
     - BCrypt 알고리즘으로 비밀번호 해싱 (strength 10)
     - 중복 사용자 ID 및 이메일 체크
   - 로그인 API: POST /api/auth/login
     - 사용자 ID와 비밀번호 검증
     - JWT 토큰 발급 (HS512 알고리즘, 30분 만료)
     - 마지막 로그인 시간 자동 기록
     - 비활성화된 계정 로그인 차단
   - 토큰 검증 API: GET /api/auth/validate
     - Bearer 토큰 유효성 검증
     - 토큰 만료 시간 체크

2. 사용자 관리 API
   - 프로필 조회: GET /api/user/profile
     - 사용자 ID, 이메일, 전화번호, 역할, 가입일, 마지막 로그인, 활성 상태 조회
     - API 키 등록 여부만 반환 (실제 키는 노출 안 함)
   - 프로필 수정: PUT /api/user/profile
     - 이메일 변경 (중복 체크 포함)
     - 전화번호 변경
     - 비밀번호 변경 (BCrypt 재해싱)
   - API 키 저장: POST /api/user/api-keys
     - 업비트 Access Key와 Secret Key를 AES-256으로 암호화
     - 암호화된 상태로 데이터베이스에 저장
     - 환경변수의 AES_SECRET_KEY 사용 (32자 고정)
   - API 키 삭제: DELETE /api/user/api-keys
     - 저장된 API 키 완전 삭제

3. 보안 기능 구현
   - EncryptionUtil 클래스
     - AES-256 암호화 알고리즘 사용
     - encrypt() 메서드: 평문을 Base64 인코딩된 암호문으로 변환
     - decrypt() 메서드: 암호문을 평문으로 복호화
     - 업비트 API 키 보호에 사용
   - JwtUtil 클래스
     - JWT 토큰 생성 (HS512 서명 알고리즘)
     - 토큰에서 사용자 ID 추출
     - 토큰에서 사용자 역할(Role) 추출
     - 토큰 만료 여부 확인
     - 토큰 유효성 검증
   - JwtAuthenticationFilter 필터
     - 모든 HTTP 요청에서 Authorization 헤더 확인
     - Bearer 토큰 자동 추출 및 검증
     - 유효한 토큰이면 Spring Security Context에 인증 정보 저장
     - 인증 실패 시 예외 로깅
   - SecurityConfig 설정
     - Spring Security 6 기반 설정
     - CSRF 보호 비활성화 (JWT 사용)
     - Stateless 세션 관리 (서버에 세션 저장 안 함)
     - 공개 엔드포인트 설정: /api/health, /api/auth/** 인증 불필요
     - 나머지 모든 엔드포인트는 인증 필수
     - CORS 설정: localhost:80, localhost:3000 허용
   - GlobalExceptionHandler 클래스
     - @RestControllerAdvice로 전역 예외 처리
     - Validation 예외 처리 (입력값 검증 실패 시)
     - RuntimeException 처리 (비즈니스 로직 오류)
     - 일반 Exception 처리 (예상치 못한 오류)
     - 모든 예외를 JSON 형태로 응답

4. 엔티티 및 DTO 설계
   - User 엔티티
     - 사용자 기본 정보: userId(PK), passwordHash, email, phone
     - 역할 및 상태: role(USER/ADMIN), isActive
     - 타임스탬프: joinDate, lastLogin, createdAt, updatedAt
     - API 키: apiKeyEncrypted, secretKeyEncrypted (AES-256 암호화)
     - BaseEntity 상속으로 생성일/수정일 자동 관리
   - PasswordResetToken 엔티티
     - 비밀번호 재설정용 토큰 관리
     - 만료 시간 및 사용 여부 추적
   - UserInfoDTO
     - 클라이언트에게 전달할 사용자 정보
     - 민감한 정보(password, API 키 실제 값) 제외
     - hasApiKey 필드로 API 키 등록 여부만 전달
   - SignupRequest DTO
     - 회원가입 요청 데이터
     - Jakarta Validation 애노테이션으로 입력값 검증
   - LoginRequest DTO
     - 로그인 요청 데이터
     - 필수 필드 검증
   - AuthResponse DTO
     - 인증 성공 후 응답 데이터
     - JWT 토큰, 사용자 정보, 메시지 포함

5. Repository 계층
   - UserRepository 인터페이스
     - JpaRepository 확장으로 기본 CRUD 자동 제공
     - findByEmail: 이메일로 사용자 조회
     - existsByUserId: 사용자 ID 중복 체크
     - existsByEmail: 이메일 중복 체크

**테스트 결과**:

모든 API 테스트를 성공적으로 완료했습니다.

- 회원가입 테스트
  - testuser 계정 생성 성공
  - JWT 토큰 정상 발급
  - 입력값 검증 정상 작동

- 로그인 테스트
  - admin 계정 (비밀번호: Test1234!@) 로그인 성공
  - JWT 토큰 발급: eyJhbGciOiJIUzUxMiJ9...
  - 역할: ADMIN

- 프로필 조회 테스트
  - 사용자 ID: admin
  - 이메일: admin@crypto.com
  - 역할: ADMIN
  - API 키 등록 여부: False (초기 상태)
  - 가입일: 2025-10-11T09:12:34

- 토큰 검증 테스트
  - Bearer 토큰 검증 성공
  - 응답: "유효한 토큰입니다"

- API 키 저장 테스트
  - Access Key와 Secret Key를 AES-256으로 암호화
  - 데이터베이스에 안전하게 저장
  - 응답: "API 키가 안전하게 저장되었습니다"
  - 프로필 재조회 시 hasApiKey: True로 변경 확인

- 프로필 수정 테스트
  - 전화번호를 010-1234-5678로 변경
  - 응답: "프로필이 성공적으로 업데이트되었습니다"
  - 변경 사항 즉시 반영 확인

- API 키 삭제 테스트
  - API 키 삭제 요청 성공
  - 응답: "API 키가 삭제되었습니다"
  - 프로필 재조회 시 hasApiKey: False로 변경 확인

- Health Check 테스트
  - 시스템 상태: UP
  - 데이터베이스: UP
  - Redis: UP
  - 모든 인프라 정상 작동

**보안 설정**:

환경변수 (.env 파일)에 다음 보안 키를 설정했습니다:

- JWT_SECRET_KEY: 256비트 이상의 무작위 문자열
  - HS512 알고리즘에 사용
  - 최소 32자 이상 권장
  - 예시: your_very_long_jwt_secret_key_at_least_256_bits_long_abcdef123456

- JWT_EXPIRATION: 1800000 (30분, 밀리초 단위)
  - 토큰 만료 시간
  - 보안을 위해 짧게 설정

- AES_SECRET_KEY: 정확히 32자의 문자열
  - AES-256 암호화에 사용
  - 반드시 32자여야 함 (256비트 = 32바이트)
  - 예시: 12345678901234567890123456789012

- 비밀번호 해싱: BCrypt (strength 10)
  - 단방향 해시 함수 사용
  - 원본 비밀번호 복원 불가능
  - 솔트 자동 생성으로 레인보우 테이블 공격 방어

- CORS 설정: localhost:80, localhost:3000만 허용
  - 다른 도메인에서의 API 접근 차단
  - 프로덕션에서는 실제 도메인으로 변경 필요

- 세션 관리: Stateless
  - 서버에 세션 정보 저장 안 함
  - JWT 토큰만으로 인증 처리
  - 수평 확장에 유리

**생성된 파일 목록**:

Controller 계층:
- backend/src/main/java/com/cryptotrading/controller/AuthController.java
  - 회원가입, 로그인, 토큰 검증 API
- backend/src/main/java/com/cryptotrading/controller/UserController.java
  - 프로필 조회/수정, API 키 관리 API
- backend/src/main/java/com/cryptotrading/controller/HealthController.java
  - 시스템 상태 체크 API (기존)

Service 계층:
- backend/src/main/java/com/cryptotrading/service/AuthService.java
  - 회원가입, 로그인 비즈니스 로직
- backend/src/main/java/com/cryptotrading/service/UserService.java
  - 사용자 정보 관리, API 키 암호화/복호화

Repository 계층:
- backend/src/main/java/com/cryptotrading/repository/UserRepository.java
  - 사용자 데이터 접근 인터페이스

Entity 계층:
- backend/src/main/java/com/cryptotrading/entity/User.java
  - 사용자 엔티티
- backend/src/main/java/com/cryptotrading/entity/PasswordResetToken.java
  - 비밀번호 재설정 토큰 엔티티
- backend/src/main/java/com/cryptotrading/entity/BaseEntity.java
  - 공통 엔티티 (생성일/수정일 자동 관리)
- backend/src/main/java/com/cryptotrading/entity/UserRole.java
  - 사용자 역할 Enum (USER, ADMIN)

DTO 계층:
- backend/src/main/java/com/cryptotrading/dto/SignupRequest.java
  - 회원가입 요청 DTO
- backend/src/main/java/com/cryptotrading/dto/LoginRequest.java
  - 로그인 요청 DTO
- backend/src/main/java/com/cryptotrading/dto/AuthResponse.java
  - 인증 응답 DTO
- backend/src/main/java/com/cryptotrading/dto/UserInfoDTO.java
  - 사용자 정보 응답 DTO

Config 계층:
- backend/src/main/java/com/cryptotrading/config/SecurityConfig.java
  - Spring Security 6 설정
- backend/src/main/java/com/cryptotrading/config/WebMvcConfig.java
  - CORS 설정 (기존)

Filter 계층:
- backend/src/main/java/com/cryptotrading/filter/JwtAuthenticationFilter.java
  - JWT 토큰 자동 인증 필터

Util 계층:
- backend/src/main/java/com/cryptotrading/util/JwtUtil.java
  - JWT 토큰 생성/검증 유틸리티
- backend/src/main/java/com/cryptotrading/util/EncryptionUtil.java
  - AES-256 암호화/복호화 유틸리티

Exception 계층:
- backend/src/main/java/com/cryptotrading/exception/GlobalExceptionHandler.java
  - 전역 예외 처리 핸들러

---

## Day 4 (2025-10-19) - 업비트 API 연동

### 완료 작업
- ✅ 업비트 API 클라이언트 서비스 구현
- ✅ DTO 클래스 생성 (UpbitMarketDTO, UpbitTickerDTO, UpbitAccountDTO, UpbitOrderDTO)
- ✅ CoinInfo 엔티티 및 Repository 생성
- ✅ CoinInfoService 구현 (코인 정보 관리)
- ✅ 테스트용 Controller 생성
- ✅ 공개 API 연동 테스트 완료
  - 마켓 코드 조회 (218개 코인)
  - 현재가 조회 (BTC, ETH)
  - 코인 정보 DB 업데이트
  - 활성 코인 목록 조회

### 기술 스택
- Auth0 JWT (업비트 API 인증)
- Spring WebClient (비동기 HTTP 통신)
- Spring Data JPA (코인 정보 관리)

### API 엔드포인트
- GET /api/upbit/test/markets - 마켓 코드 조회
- GET /api/upbit/test/ticker - 현재가 조회
- POST /api/upbit/test/update-coins - 코인 정보 업데이트
- GET /api/upbit/test/active-coins - 활성 코인 조회
- GET /api/upbit/test/accounts - 계좌 조회 (API 키 필요)

### 다음 단계
- WebSocket 실시간 시세 구현
- 거래 전략 로직 개발
- 자동 거래 시스템 구축

---

### Day 5 (2025-10-20) - 거래 설정 관리 및 코인 정보 API 구현 ✅

**목표**: 
- [x] 거래 설정(Trading Settings) CRUD API 구현
- [x] 코인 정보 조회 API 구현
- [x] 현재가 조회 API 구현
- [x] 계좌 정보 조회 API 구현
- [x] Postman을 이용한 전체 API 테스트

**진행 내용**:

1. **거래 설정 관리 API 구현**
   - TradingSettingDTO 생성 (Jakarta Validation으로 입력값 검증)
   - TradingSettingService 구현 (CRUD 비즈니스 로직)
   - TradingSettingController 구현 (REST API 엔드포인트)
   - 사용자별 맞춤 거래 설정 관리 기능

2. **코인 정보 및 시세 API 구현**
   - CoinController 생성
   - 활성 코인 목록 조회 API (공개)
   - 단일/다중 코인 현재가 조회 API (공개)
   - 업비트 계좌 정보 조회 API (인증 필요)

3. **보안 강화**
   - 공개 API와 인증 API 명확히 분리
   - 관리자 전용 API 권한 체크 (ROLE_ADMIN)
   - API 키 복호화 후 업비트 API 호출

**완료된 작업**:

1. **DTO 클래스**
   - `TradingSettingDTO.java`: 거래 설정 데이터 전송 객체
     - 입력값 검증: @NotEmpty, @Min, @DecimalMin, @DecimalMax
     - 필드: coinSymbols, basePeriod, buyThresholdPct, sellTargetPct, stopLossPct, maxHoldingsPerCoin, dailyLimitAmount, useAiAnalysis, useTrailingStop, trailingStopPct

2. **Service 계층**
   - `TradingSettingService.java`: 거래 설정 비즈니스 로직
     - getTradingSetting(): 사용자별 설정 조회
     - createTradingSetting(): 설정 생성 (중복 체크 포함)
     - updateTradingSetting(): 설정 수정
     - deleteTradingSetting(): 설정 삭제
     - convertToDTO(): Entity ↔ DTO 변환

3. **Controller 계층**
   - `TradingSettingController.java`: 거래 설정 REST API
     - GET /api/trading-settings: 조회
     - POST /api/trading-settings: 생성
     - PUT /api/trading-settings: 수정
     - DELETE /api/trading-settings: 삭제
   - `CoinController.java`: 코인 정보 REST API
     - GET /api/coins/active: 활성 코인 목록
     - GET /api/coins/{symbol}/price: 현재가 조회
     - GET /api/coins/prices: 여러 코인 현재가 조회
     - GET /api/coins/accounts: 계좌 정보 조회 (인증)
     - POST /api/coins/update: 코인 정보 업데이트 (관리자)

4. **보안 설정 업데이트**
   - `SecurityConfig.java` 수정
     - 코인 정보 조회 API 공개 설정
     - 계좌 조회 API 인증 필요
     - 관리자 전용 API 권한 설정

**테스트 결과**:

Postman을 이용한 전체 API 테스트 완료:
- ✅ 로그인 성공 (JWT 토큰 발급)
- ✅ 거래 설정 생성 성공
  - 요청: POST /api/trading-settings
  - 응답: 201 Created, 생성된 설정 반환
- ✅ 거래 설정 조회 성공
  - 요청: GET /api/trading-settings
  - 응답: 200 OK, 사용자의 설정 반환
- ✅ 거래 설정 수정 성공
  - 요청: PUT /api/trading-settings
  - 응답: 200 OK, 수정된 설정 반환
- ✅ 활성 코인 목록 조회 성공
  - 요청: GET /api/coins/active
  - 응답: 200 OK, Top 10 코인 목록 반환
- ✅ 비트코인 현재가 조회 성공
  - 요청: GET /api/coins/KRW-BTC/price
  - 응답: 200 OK, 실시간 시세 반환
- ✅ 여러 코인 현재가 조회 성공
  - 요청: GET /api/coins/prices?symbols=KRW-BTC,KRW-ETH,KRW-XRP
  - 응답: 200 OK, 3개 코인 시세 배열 반환
- ✅ 계좌 정보 조회 성공 (API 키 등록 후)
  - 요청: GET /api/coins/accounts
  - 응답: 200 OK, 업비트 계좌 정보 반환
- ✅ 거래 설정 삭제 성공
  - 요청: DELETE /api/trading-settings
  - 응답: 200 OK, 삭제 완료 메시지

**주요 기능 설명**:

1. **거래 설정 관리**
   - 사용자별로 독립적인 거래 설정 관리
   - 거래할 코인 종목 선택 (배열 형태)
   - 기준 기간 설정 (7일~30일 이동평균선)
   - 매수/매도 기준 설정 (%, BigDecimal로 정밀 처리)
   - 손절매 및 트레일링 스톱 옵션
   - AI 뉴스 분석 사용 여부 설정

2. **코인 정보 조회**
   - 업비트에서 제공하는 모든 활성 코인 정보
   - 실시간 현재가 조회 (단일/다중)
   - 시가, 고가, 저가, 거래량 등 상세 정보
   - 24시간 누적 거래대금
   - 52주 최고가/최저가

3. **계좌 연동**
   - 사용자의 API 키를 AES-256 암호화하여 DB 저장
   - 필요 시 복호화하여 업비트 API 호출
   - JWT로 인증된 사용자만 접근 가능
   - KRW 잔액 및 보유 코인 정보 조회

**생성된 파일 목록**:
- `backend/src/main/java/com/cryptotrading/dto/TradingSettingDTO.java`
- `backend/src/main/java/com/cryptotrading/service/TradingSettingService.java`
- `backend/src/main/java/com/cryptotrading/controller/TradingSettingController.java`
- `backend/src/main/java/com/cryptotrading/controller/CoinController.java`

**수정된 파일**:
- `backend/src/main/java/com/cryptotrading/config/SecurityConfig.java`

**API 엔드포인트 정리**:

| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| POST | /api/auth/login | ❌ | 로그인 (JWT 발급) |
| GET | /api/user/profile | ✅ | 사용자 프로필 조회 |
| POST | /api/user/api-keys | ✅ | API 키 등록 |
| GET | /api/trading-settings | ✅ | 거래 설정 조회 |
| POST | /api/trading-settings | ✅ | 거래 설정 생성 |
| PUT | /api/trading-settings | ✅ | 거래 설정 수정 |
| DELETE | /api/trading-settings | ✅ | 거래 설정 삭제 |
| GET | /api/coins/active | ❌ | 활성 코인 목록 |
| GET | /api/coins/{symbol}/price | ❌ | 현재가 조회 |
| GET | /api/coins/prices | ❌ | 여러 코인 현재가 |
| GET | /api/coins/accounts | ✅ | 계좌 정보 조회 |
| POST | /api/coins/update | 🔐 | 코인 정보 업데이트 (관리자) |

**다음 단계 (Day 6 예정)**:
- 가격 이력 수집 및 저장 기능
- 기술적 지표 계산 (이동평균선, RSI, 볼린저밴드)
- WebSocket 실시간 시세 구독
- 가격 데이터 스케줄링 및 자동 수집

**현재 진척도**: 약 30% 완료
- Phase 1 (핵심 기능 4주) 중 1.25주차 완료
- 인프라, 인증, 거래 설정, 코인 정보 API 완성
- 다음 단계: 실시간 데이터 수집 및 기술적 분석

---

## 🎯 전체 개발 계획

### Phase 1: 핵심 기능 (4주)
- Week 1: 개발환경 + Spring Boot 기본 설정
- Week 2: 업비트 API 연동 및 인증 시스템
- Week 3: 데이터베이스 설계 및 매매 로직 구현
- Week 4: 기본 보안 시스템 및 테스트

### Phase 2: 고도화 (3주)
- Week 5-6: Vue.js 프론트엔드 개발
- Week 7: 실시간 모니터링 및 알림 시스템

### Phase 3: 안정화 (2주)
- Week 8: 보안 강화 및 성능 최적화
- Week 9: 테스트 및 배포 준비