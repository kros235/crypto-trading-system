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