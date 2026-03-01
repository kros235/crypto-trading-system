### 제목 : [Day 51 릴리즈 노트] - 자산 스냅샷 평가금액 불일치 수정 (캐시 우회) / 파이차트 100% 렌더링 수정 / 단계적 매수 및 손절매 ON·OFF 도입

### 📅 작업일: 2026-03-02

---

### ✨ 신규 기능 (Feature)

#### 작업 1: 단계적 매수 (물타기 간격, Staggered Buy) 도입
- **내용:** 하락장에서 코인을 여러 번 나누어 살 때, 한 번에 예산을 모두 소진하지 않도록 직전 매수가 대비 **'추가 매수 하락률(%)'**을 설정하는 기능을 추가했습니다.
- **상세 적용:**
  - `TradingBotService`: 코인을 이미 보유 중일 경우, 가장 최근 매수가 대비 지정된 하락률(기본 0.5%) 이상 떨어졌을 때만 추가 매수가 승인되도록 방어 로직 추가.
  - 라운드로빈 및 고정금액 방식 모두 적용 (고정금액 방식 시 1사이클에 1번만 매수되도록 제한).
  - `BacktestService`: 백테스팅 시그널 감지 항목에도 동일한 단계적 하락률 매수 로직 반영 완비.
  - `TradingSettingsView` / `BacktestView`: 종목당 보유 건수가 2 이상일 때만 나타나는 직관적인 하락률 조절 슬라이더 및 맞춤형 도움말(물타기 간격 비유) UI 추가.

#### 작업 2: 손절매 ON / OFF (강제 청산 방지) 도입
- **내용:** 일시적인 대폭락장에서 손해를 확정 짓지 않고 반등을 기다리는 일명 '존버 전략'이 가능하도록, 손절매를 아예 끄고 켤 수 있는 스위치 기능을 도입했습니다.
- **상세 적용:**
  - `SignalDetectorService` / `BacktestService`: 매도 시그널 검증 단계에서 `useStopLoss`가 `true`일 때만 손절(SELL_STOP_LOSS) 시그널이 발생하도록 우회(Wrapping) 로직 추가.
  - Frontend: 손절매 OFF 시 자금이 묶일 수 있다는 강력한 에러 경고 문구 표출. 또한 그 하위의 묶음 설정인 '연속 손절 제한(회)' 슬라이더 역시 조작 불가능하도록 시각적 비활성화(`disabled`) 연동 적용.

---

### 🐛 버그 수정

#### 작업 1: 자산변동 추이 차트 평가금액 불일치 (중요)
- **증상:** 대시보드/보유자산 페이지의 자산변동 추이 차트에서 평가금액이 업비트 실제 KRW 잔고(96,634원)와 다르게 표시됨(96,326원, 308원 차이)
- **원인:** `DailyAssetSnapshotService.createDailySnapshot()`이 `RiskManagementService.getDailyTotalAssetSnapshot()`을 호출하는데, 이 메서드는 **일일 한도 계산의 안정성**을 위해 당일 첫 호출 시 업비트 API 결과를 ConcurrentHashMap에 캐시하고 이후 동일 값을 반환함. 따라서 23:59 스냅샷 저장 시 **당일 새벽/아침에 봇이 캐시한 값**이 사용되어, 이후 발생한 거래/입출금/예치금 이용료 등이 반영되지 않음.
- **해결:**
  - `DailyAssetSnapshotService.java`: `createDailySnapshot()`에서 `getDailyTotalAssetSnapshot()` (캐시) → `fetchTotalAssetFromUpbit()` (API 직접 호출)로 변경
  - `RiskManagementService.java`: `fetchTotalAssetFromUpbit()` 접근제어자 `private` → `public` 변경 (외부 서비스에서 캐시 우회 호출 가능하도록)
- **영향범위:**
  - 23:59 스케줄러 스냅샷: 마감 시점 정확한 잔고 기록 ✅
  - 매수/매도 후 즉시 갱신: 체결 직후 정확한 잔고 기록 ✅
  - 수동 스냅샷 생성 API: 호출 시점 정확한 잔고 기록 ✅
  - 일일 한도 계산: 영향 없음 (기존 캐시 로직 그대로 유지)

#### 작업 2: KRW 100% 보유 시 파이차트 상면 미렌더링 (중요)
- **증상:** 업비트 실제 잔고 카드에서 현금(KRW) 100% 보유 시 보유자산 포트폴리오 파이차트의 상면(윗면)이 흰색으로 표시됨. 다른 코인과 함께 보유 대 보유 중일 때는 색상 정상 표시.
- **원인:** SVG Arc 명령은 시작점과 끝점 좌표가 동일하면 경로를 렌더링하지 않음. 단일 항목 100%(360도)일 때 cos(-90°)=cos(270°), sin(-90°)=sin(270°)으로 시작/끝 좌표가 수학적으로 동일해져 Arc가 그려지지 않고, 중앙 도넛 홀의 `fill="white"`만 보이는 현상 발생.
- **해결:** `DashboardView.vue`의 `portfolio3dSlices` computed에서 360도(≥359.99) 항목을 두 개의 반원 Arc로 분할하여 완전한 타원이 렌더링되도록 조건 분기 추가
- **영향범위:**
  - KRW 100% 보유 시 파이차트: 정상 렌더링 ✅
  - 복수 자산 보유 시 파이차트: 영향 없음 (기존 로직 그대로)
  - 3D 측면 렌더링: 영향 없음 (0°~180° 범위 계산으로 100%에서도 정상)

---

### 📁 수정된 파일 목록

**Database**

| 구분 | 파일 | 내용 |
|------|------|------|
| ➕ | `init.sql` | `trading_settings` 테이블에 `additional_drop_pct`(하락률), `use_stop_loss`(손절매 설정) 컬럼 및 기본값 추가 |

**Backend**

| 구분 | 파일 | 내용 |
|------|------|------|
| ➕ | `dto & entity` | `TradingSetting`, `TradingSettingDTO`, `BacktestRequestDTO` 파일들에 상기 2개 신규 필드 추가 및 매핑 동기화 |
| ✏️ | `service/DailyAssetSnapshotService.java` | `createDailySnapshot()`에서 캐시 메서드 대신 `fetchTotalAssetFromUpbit()` 직접 호출로 변경 |
| ✏️ | `service/RiskManagementService.java` | `fetchTotalAssetFromUpbit()` 접근제어자 `private` → `public` 변경 |
| ✏️ | `service/TradingBotService.java` | 매수 시 보유 가능 건수 확인 후, 이전 매수가 대비 하락률을 체크하는 단계적 매수 방어 로직 신설 |
| ✏️ | `service/SignalDetectorService.java` | 손절매(useStopLoss) 여부가 허용일 때만 강제 매도 시그널을 만들도록 분기 적용 |
| ✏️ | `service/BacktestService.java` | 실제 봇 서비스와 동일하게 하락률 매수 텀 확인 및 손절매 선택 여부 백테스팅 적용 |
| ✏️ | `service/TradingSettingService.java` | 신규 스위치 및 변수값이 DB CRUD 시에도 정상 데이터 바인딩되도록 누락분 보완 |

**Frontend**

| 구분 | 파일 | 내용 |
|------|------|------|
| ✏️ | `views/DashboardView.vue` | `portfolio3dSlices` computed에서 100%(≥359.99도) 항목 시 두 개의 반원 Arc로 분할하는 조건 분기 추가 |
| ➕ | `views/TradingSettingsView.vue` | 손절매 스위치(및 비활성화 연동) 추가, 물타기 하락폭(%) 슬라이더 추가 및 시각적 위젯 디자인 반영 |
| ➕ | `views/BacktestView.vue` | 백테스트용 고급 옵션 패널에 상단 위젯 설정 동일 적용 및 페이로드 데이터 동기화 |

---

### 🧪 테스트 검증

| 항목 | 결과 | 비고 |
|------|------|------|
| 로컬 코드 경로 확인 | ✅ 통과 | Postman + Docker 로그에서 `fetchTotalAssetFromUpbit` 직접 호출 확인 (캐시 히트 로그 없음) |
| 로컬 가짜 API 키 환경 | ✅ 통과 | 401 Unauthorized 예외 → catch 블록 정상 처리, 기존 기능 영향 없음 |
| 운영 수동 스냅샷 생성 | ✅ 통과 | 평가금액 ₩96,639 (실제 잔고 + 예수금 이용료 5원 정확 반영) |
| 운영 수익률 검증 | ✅ 통과 | 수익금액 ₩-3,480 / 수익률 -3.48% 정확 |
| 일일 한도 계산 영향 | ✅ 무관 | 기존 `getDailyTotalAssetSnapshot()` 캐시 로직 그대로 유지 |
| 파이차트 KRW 100% 렌더링 | ✅ 통과 | KRW만 보유 시 녹색(#8BC34A) 파이 상면 정상 표시, 100.0% 텍스트 정상 |
| 파이차트 복수 자산 렌더링 | ✅ 통과 | 기존 다중 코인 보유 시 렌더링 영향 없음 확인 |
| API 및 Entity CRUD 매핑 검증 | ✅ 통과 | null 에러 없이 프론트엔드의 신규 옵션값이 안전하게 DB 매핑 및 보존됨 |

---

### ⚙️ 운영 배포 내역

| 항목 | 내용 |
|------|------|
| DB 마이그레이션 | 신규 파라미터 적용용 ALTER TABLE 실행 요망 (`additional_drop_pct`, `use_stop_loss`) |
| 백엔드 재빌드 | `docker compose -f docker-compose.prod.yml up -d --build backend` |
| 배포 후 검증 | 수동 스냅샷 생성 API 호출 → 갱신 평가금액 확인 및 신규 거래 탭 변수 초기화 성공 |

---

### 🌐 서비스 접속 정보

| 서비스 | URL |
|--------|-----|
| Frontend | https://crypto-trading-prd.duckdns.org |
| Backend API | https://crypto-trading-prd.duckdns.org/api |
| Swagger UI | https://crypto-trading-prd.duckdns.org/swagger-ui/index.html |
