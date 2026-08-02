package com.cryptotrading.scheduler;

import com.cryptotrading.service.TradingBotService;
import com.cryptotrading.service.TradingBotService.BotExecutionResult;
import com.cryptotrading.service.DailyReportService;
import com.cryptotrading.service.NotificationService;
import com.cryptotrading.service.EmailService;
import com.cryptotrading.service.DiscordBotService;
import com.cryptotrading.service.NewsCollectorService;      
import com.cryptotrading.service.NewsAnalysisService;       
import com.cryptotrading.entity.User;
import com.cryptotrading.entity.TradingSetting;             
import com.cryptotrading.repository.UserRepository;
import com.cryptotrading.repository.TradingSettingRepository; 
import com.cryptotrading.repository.CoinNewsRepository;
// ⭐⭐⭐ [개선] 코인/주식 활동 여부 판별용 리포지토리 3개 추가 ⭐⭐⭐
import com.cryptotrading.repository.TransactionRepository;
import com.cryptotrading.repository.StockTradingSettingRepository;
import com.cryptotrading.repository.StockTransactionRepository;
import com.cryptotrading.dto.notification.DailyReportDTO;
import com.cryptotrading.service.RiskManagementService;
import com.cryptotrading.service.DailyAssetSnapshotService;  // ⭐⭐⭐ [신규 추가] 일별 자산 스냅샷 서비스 ⭐⭐⭐
import com.cryptotrading.service.StockDailyReportService;    // ⭐⭐⭐ [Day 63 추가] 코인+주식 통합 일일 리포트용 ⭐⭐⭐

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import com.cryptotrading.entity.Transaction;
import com.cryptotrading.entity.TransactionStatus;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradingScheduler {

    private final TradingBotService tradingBotService;
    private final DailyReportService dailyReportService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final DiscordBotService discordBotService;
    private final NewsCollectorService newsCollectorService;
    private final NewsAnalysisService newsAnalysisService;
    private final CoinNewsRepository coinNewsRepository;
    private final TradingSettingRepository tradingSettingRepository;
    // ⭐⭐⭐ [개선] 코인/주식 활동 여부(거래설정 존재 또는 거래내역 존재) 판별용 ⭐⭐⭐
    private final TransactionRepository transactionRepository;
    private final StockTradingSettingRepository stockTradingSettingRepository;
    private final StockTransactionRepository stockTransactionRepository;
    
   // ⭐⭐⭐ [추가] RiskManagementService (총자산 스냅샷 관리) ⭐⭐⭐
    private final RiskManagementService riskManagementService;
    
    // ⭐⭐⭐ [신규 추가] 일별 자산 스냅샷 서비스 ⭐⭐⭐
    private final DailyAssetSnapshotService dailyAssetSnapshotService;
    
    // ⭐⭐⭐ [Day 63 추가] 주식 일일 리포트 서비스 (코인+주식 통합 리포트용) ⭐⭐⭐
    private final StockDailyReportService stockDailyReportService;
    
    // ⭐⭐⭐ [추가] Redis Template (한 번만 선언) ⭐⭐⭐
    private final StringRedisTemplate redisTemplate;
    
    // 업비트 점검 시간 (매일 09:00 ~ 09:10 KST)
    private static final LocalTime MAINTENANCE_START = LocalTime.of(9, 0);
    private static final LocalTime MAINTENANCE_END = LocalTime.of(9, 10);

    // ⭐⭐⭐ [추가] Redis 키 상수 ⭐⭐⭐
    // ⭐⭐⭐ [Day 61.5 수정] 단일 전역 키 → 사용자별 키 패턴 (멀티유저 격리 버그 수정) ⭐⭐⭐
    // 변경 이유: 기존엔 한 사용자가 봇을 OFF하면 모든 사용자의 자동매매가 차단되는
    //           멀티유저 격리 버그가 있었음. 사용자별 독립 키로 변경.
    //           예: trading:bot:enabled:user1, trading:bot:enabled:user2
    private static final String BOT_ENABLED_KEY = "trading:bot:enabled:%s";
    
    // ⭐⭐⭐ [추가] static 참조용 ⭐⭐⭐
    private static StringRedisTemplate staticRedisTemplate;
    
    // ⭐⭐⭐ [추가] 초기화 메서드 ⭐⭐⭐
    @PostConstruct
    public void init() {
        staticRedisTemplate = this.redisTemplate;
        // ⭐⭐⭐ [Day 61.5 수정] 사용자별 키 패턴이라 시작 시점에는 특정 사용자의 봇 상태를 알 수 없음 ⭐⭐⭐
        // 변경 이유: isBotEnabled()는 이제 userId 인자를 받으므로 init() 시점엔 호출 불가.
        //           대신 키 패턴을 안내하는 로그로 대체.
        log.info("⭐ TradingScheduler 초기화 완료 - 사용자별 봇 활성화 키 사용 (trading:bot:enabled:{{userId}})");
    }

    /**
     * ⭐ 봇 활성화 상태 조회 (Redis에서)
     * ⭐⭐⭐ [Day 61.5 수정] userId 인자 추가 - 사용자별 봇 상태 독립 조회 ⭐⭐⭐
     * 변경 이유: 기존 단일 전역 키 → 사용자별 키 패턴으로 변경.
     *           예: trading:bot:enabled:user1 의 값 조회.
     *           키가 없으면 기본값 true (활성화) 반환 → 새 사용자도 자동매매 기본 활성화.
     *
     * @param userId 사용자 ID
     * @return true=활성화, false=비활성화
     */
    public static boolean isBotEnabled(String userId) {
        if (staticRedisTemplate == null) {
            return true; // Redis 연결 전 기본값
        }
        try {
            String key = String.format(BOT_ENABLED_KEY, userId);
            String value = staticRedisTemplate.opsForValue().get(key);
            return value == null || "true".equals(value); // 값이 없으면 기본값 true
        } catch (Exception e) {
            return true; // Redis 오류 시 기본값
        }
    }
    
    /**
     * ⭐ 봇 활성화 상태 설정 (Redis에 저장)
     * ⭐⭐⭐ [Day 61.5 수정] userId 인자 추가 - 사용자별 봇 상태 독립 설정 ⭐⭐⭐
     * 변경 이유: 기존엔 한 사용자가 OFF하면 전체 영향. 이제 자기 키만 변경.
     *
     * @param userId 사용자 ID
     * @param enabled true=활성화, false=비활성화
     */
    public static void setBotEnabled(String userId, boolean enabled) {
        if (staticRedisTemplate != null) {
            try {
                String key = String.format(BOT_ENABLED_KEY, userId);
                staticRedisTemplate.opsForValue().set(key, String.valueOf(enabled));
            } catch (Exception e) {
                // Redis 오류 시 무시
            }
        }
        // ⭐⭐⭐ [Day 61.5 수정] 로그에 userId 포함 - 누가 봇을 켰는지/껐는지 추적 가능 ⭐⭐⭐
        System.out.println("⭐ 자동매매 봇 상태 변경 [" + userId + "]: " + (enabled ? "활성화" : "비활성화"));
    }

    /**
     * 5분마다 자동매매 실행
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")  // 5분마다 (KST 기준)
    public void executeAutoTrading() {
        LocalTime now = LocalTime.now();

        // ⭐⭐⭐ [Day 61.5 수정] 단일 전역 봇 게이트 제거 ⭐⭐⭐
        // 변경 이유: 멀티유저 격리 버그 수정.
        //   - 기존: 한 사용자라도 봇 OFF하면 모든 사용자 매매 차단됨 (BUG)
        //   - 수정: 사용자별 봇 상태 체크는 TradingBotService.executeForUser()에서 수행
        //   - 점검 시간(업비트) 체크는 그대로 유지 - 시스템 전체 차원의 차단이므로
        
        // 업비트 점검 시간 체크
        if (isMaintenanceTime(now)) {
            log.info("업비트 점검 시간 - 자동매매 스킵 ({})", now);
            return;
        }
        
        log.info("========================================");
        log.info("스케줄러 자동매매 시작: {}", LocalDateTime.now());
        log.info("========================================");
        
        try {
            List<BotExecutionResult> results = tradingBotService.executeForAllUsers();
            
            // 실행 결과 요약
            int successCount = 0;
            int skipCount = 0;
            int errorCount = 0;
            int totalBuys = 0;
            int totalSells = 0;
            
            for (BotExecutionResult result : results) {
                switch (result.getStatus()) {
                    case "SUCCESS" -> {
                        successCount++;
                        totalBuys += result.getBuyCount();
                        totalSells += result.getSellCount();
                    }
                    case "SKIP" -> skipCount++;
                    case "ERROR" -> errorCount++;
                }
            }
            
            log.info("========================================");
            log.info("스케줄러 자동매매 완료");
            log.info("처리: {}명 (성공: {}, 스킵: {}, 오류: {})", 
                    results.size(), successCount, skipCount, errorCount);
            log.info("거래: 매수 {}건, 매도 {}건", totalBuys, totalSells);
            log.info("========================================");
            
        } catch (Exception e) {
            log.error("스케줄러 자동매매 오류: {}", e.getMessage(), e);
        }
    }

    /**
     * 매일 새벽 4시에 시스템 점검
     */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")  // 매일 04:00 KST
    public void dailySystemCheck() {
        log.info("========================================");
        log.info("일일 시스템 점검 시작: {}", LocalDateTime.now());
        log.info("========================================");
        
        // 추후 구현: 일일 통계 생성, 알림 발송 등
        
        log.info("일일 시스템 점검 완료");
    }

    /**
     * 업비트 점검 시간 여부 확인
     */
    private boolean isMaintenanceTime(LocalTime time) {
        return !time.isBefore(MAINTENANCE_START) && time.isBefore(MAINTENANCE_END);
    }

    /**
     * 매일 23:50에 일일 리포트 발송
     */
    @Scheduled(cron = "0 50 23 * * *", zone = "Asia/Seoul")
    public void sendDailyReport() {
        log.info("========== 일일 리포트 발송 시작 ==========");

        try {
            List<User> users = userRepository.findByIsActive(true);
       
            for (User user : users) {
                try {
                    // ⭐⭐⭐ [수정] "오늘 안의 거래 이력"도 활동으로 인정하도록 조건 추가 ⭐⭐⭐
                    // 왜: 오늘 매수/매도해서 실적이 있는데도, 마침 오늘 안에 설정 삭제+전량 매도까지
                    //     끝내버리면 "지금 상태" 기준만으로는 활동 없음(false)이 되어 리포트가 아예
                    //     안 갔음. 주간/월간/연간과 동일하게 "그 기간(오늘) 안의 거래 이력"도 함께
                    //     보도록 하여, 오늘 거래했으면 오늘 리포트는 한 번 오고 다음 날부터는
                    //     다시 조용해지도록 함(hasCoinPeriodActivity/hasStockPeriodActivity 재사용).
                    LocalDate today = LocalDate.now();
                    boolean hasCoinActivity = tradingSettingRepository.findByUserId(user.getUserId()).isPresent()
                            || transactionRepository.countByUserIdAndStatus(user.getUserId(), Transaction.TransactionStatus.HOLDING) > 0
                            || hasCoinPeriodActivity(user.getUserId(), today, today);
                    boolean hasStockActivity = stockTradingSettingRepository.existsByUserId(user.getUserId())
                            || stockTransactionRepository.countByUserIdAndStatus(user.getUserId(), TransactionStatus.HOLDING) > 0
                            || hasStockPeriodActivity(user.getUserId(), today, today);

                    if (!hasCoinActivity && !hasStockActivity) {
                        log.debug("사용자 {} 코인/주식 활동 이력 없음 - 일일 리포트 발송 생략", user.getUserId());
                        continue;
                    }

                    DailyReportDTO report = dailyReportService.generateDailyReport(user.getUserId());
                    report.setHasCoinActivity(hasCoinActivity);

                    // ⭐⭐⭐ [Day 63 추가] 주식 리포트 병합 (코인+주식 통합 발송) ⭐⭐⭐
                    // 왜: 별도 발송 채널을 새로 만들지 않고 기존 23:50 스케줄 1건에 합류시키기 위함.
                    //     실패해도 코인 리포트 발송에는 영향 없도록 개별 try-catch로 격리.
                    //     hasStockActivity가 false면 병합 자체를 생략 → stockBuyCount가 null로 남아
                    //     "[주식]" 섹션이 자동으로 숨겨짐(기존 hasStockData 로직 그대로 재사용).
                    if (hasStockActivity) {
                        try {
                            DailyReportDTO stockReport = stockDailyReportService.generateStockDailyReport(user.getUserId());
                            report.setStockBuyCount(stockReport.getStockBuyCount());
                            report.setStockSellCount(stockReport.getStockSellCount());
                            report.setStockTotalBuyAmount(stockReport.getStockTotalBuyAmount());
                            report.setStockTotalSellAmount(stockReport.getStockTotalSellAmount());
                            report.setStockRealizedProfit(stockReport.getStockRealizedProfit());
                            report.setStockUnrealizedProfit(stockReport.getStockUnrealizedProfit());
                            report.setStockTotalProfit(stockReport.getStockTotalProfit());
                            report.setStockProfitRate(stockReport.getStockProfitRate());
                            report.setStockHoldingCount(stockReport.getStockHoldingCount());
                            report.setStockTotalHoldingValue(stockReport.getStockTotalHoldingValue());
                            report.setStockTotalInvestment(stockReport.getStockTotalInvestment());
                            report.setStockSummaries(stockReport.getStockSummaries());
                        } catch (Exception stockEx) {
                            log.warn("사용자 {} 주식 리포트 병합 실패 (코인 리포트는 정상 발송): {}", user.getUserId(), stockEx.getMessage());
                        }
                    }

                    notificationService.sendDailyReport(report);
               
                    // 이메일 알림
                    if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                        emailService.sendDailyReport(user.getEmail(), report);
                    }

                    // Discord DM 발송
                    if (user.getDiscordUserId() != null && !user.getDiscordUserId().isEmpty()) {
                        // ⭐⭐⭐ [수정] 웹훅과 동일한 [코인]/[주식] 섹션 텍스트를 그대로 DM으로 발송 ⭐⭐⭐
                        // 왜: DM 전용 필드 조합 로직(titlePrefix/realized/unrealized 등 삼항연산)이
                        //     더 이상 필요 없음 — 각 섹션이 자기 데이터를 그대로 렌더링하기 때문.
                        discordBotService.sendCategorizedReportDM(
                            user.getDiscordUserId(), "일일", report.getReportDate().toString(),
                            report, user.getUserId()
                        );
                    }
               
                    log.info("사용자 {} 일일 리포트 발송 완료", user.getUserId());
               
                } catch (Exception e) {
                    log.error("사용자 {} 일일 리포트 발송 실패: {}", user.getUserId(), e.getMessage());
                }
            }
       
        } catch (Exception e) {
            log.error("일일 리포트 발송 중 오류: {}", e.getMessage());
        }
   
        log.info("========== 일일 리포트 발송 완료 ==========");
    }

    /**
     * ★ 추가: 매주 일요일 23:50 주간 리포트 발송
     */
    /**
     * ⭐⭐⭐ [수정] 일일 리포트와 동일한 활동 판별 + 코인/주식 통합 로직 적용 ⭐⭐⭐
     * 왜: 기존엔 활성 사용자 전원에게 코인 데이터만 무조건 발송했음(주식 데이터 미반영,
     *     활동 없어도 계속 발송). 일일 리포트(Day 63)와 동일하게 개선.
     */
    @Scheduled(cron = "0 50 23 * * SUN", zone = "Asia/Seoul")
    public void sendWeeklyReport() {
        log.info("========== 주간 리포트 발송 시작 ==========");
        try {
            List<User> users = userRepository.findByIsActive(true);
            for (User user : users) {
                try {
                    LocalDate today = LocalDate.now();
                    LocalDate weekStart = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));

                    // ⭐⭐⭐ [수정] 설정/보유 여부에 "기간 내 거래 이력" 조건 추가 ⭐⭐⭐
                    boolean hasCoinActivity = tradingSettingRepository.findByUserId(user.getUserId()).isPresent()
                            || transactionRepository.countByUserIdAndStatus(user.getUserId(), Transaction.TransactionStatus.HOLDING) > 0
                            || hasCoinPeriodActivity(user.getUserId(), weekStart, today);
                    boolean hasStockActivity = stockTradingSettingRepository.existsByUserId(user.getUserId())
                            || stockTransactionRepository.countByUserIdAndStatus(user.getUserId(), TransactionStatus.HOLDING) > 0
                            || hasStockPeriodActivity(user.getUserId(), weekStart, today);

                    if (!hasCoinActivity && !hasStockActivity) {
                        log.debug("사용자 {} 코인/주식 활동 이력 없음 - 주간 리포트 발송 생략", user.getUserId());
                        continue;
                    }

                    DailyReportDTO report = dailyReportService.generateWeeklyReport(user.getUserId());
                    report.setHasCoinActivity(hasCoinActivity);

                    if (hasStockActivity) {
                        try {
                            DailyReportDTO stockReport = stockDailyReportService.generateStockPeriodReport(user.getUserId(), weekStart, today);
                            report.setStockBuyCount(stockReport.getStockBuyCount());
                            report.setStockSellCount(stockReport.getStockSellCount());
                            report.setStockTotalBuyAmount(stockReport.getStockTotalBuyAmount());
                            report.setStockTotalSellAmount(stockReport.getStockTotalSellAmount());
                            report.setStockRealizedProfit(stockReport.getStockRealizedProfit());
                            report.setStockUnrealizedProfit(stockReport.getStockUnrealizedProfit());
                            report.setStockTotalProfit(stockReport.getStockTotalProfit());
                            report.setStockProfitRate(stockReport.getStockProfitRate());
                            report.setStockHoldingCount(stockReport.getStockHoldingCount());
                            report.setStockTotalHoldingValue(stockReport.getStockTotalHoldingValue());
                            report.setStockTotalInvestment(stockReport.getStockTotalInvestment());
                            report.setStockSummaries(stockReport.getStockSummaries());
                        } catch (Exception stockEx) {
                            log.warn("사용자 {} 주식 주간 리포트 병합 실패 (코인 리포트는 정상 발송): {}", user.getUserId(), stockEx.getMessage());
                        }
                    }

                    // 이메일 (등록된 경우만)
                    if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                        emailService.sendDailyReport(user.getEmail(), report);
                    }
                    // Discord DM (등록된 경우만)
                    if (user.getDiscordUserId() != null && !user.getDiscordUserId().isEmpty()) {
                        // ⭐⭐⭐ [수정] 일일 리포트와 동일하게 sendCategorizedReportDM로 통합 ⭐⭐⭐
                        discordBotService.sendCategorizedReportDM(
                            user.getDiscordUserId(), "주간", report.getReportDate().toString(),
                            report, user.getUserId()
                        );
                    }
                    log.info("사용자 {} 주간 리포트 발송 완료", user.getUserId());
                } catch (Exception e) {
                    log.error("사용자 {} 주간 리포트 발송 실패: {}", user.getUserId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("주간 리포트 발송 중 오류: {}", e.getMessage());
        }
        log.info("========== 주간 리포트 발송 완료 ==========");
    }

    /**
     * ★ 추가: 매월 말일 23:50 월간 리포트 발송
     * - 말일 여부: cron으로 말일 직접 지정 불가 → L(last) 사용
     */
    @Scheduled(cron = "0 50 23 L * *", zone = "Asia/Seoul") 
    public void sendMonthlyReport() {
        log.info("========== 월간 리포트 발송 시작 ==========");
        try {
            List<User> users = userRepository.findByIsActive(true);
            for (User user : users) {
                try {
                    LocalDate today = LocalDate.now();
                    LocalDate monthStart = today.withDayOfMonth(1);
                    LocalDate monthEnd = today.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());

                    boolean hasCoinActivity = tradingSettingRepository.findByUserId(user.getUserId()).isPresent()
                            || transactionRepository.countByUserIdAndStatus(user.getUserId(), Transaction.TransactionStatus.HOLDING) > 0
                            || hasCoinPeriodActivity(user.getUserId(), monthStart, monthEnd);
                    boolean hasStockActivity = stockTradingSettingRepository.existsByUserId(user.getUserId())
                            || stockTransactionRepository.countByUserIdAndStatus(user.getUserId(), TransactionStatus.HOLDING) > 0
                            || hasStockPeriodActivity(user.getUserId(), monthStart, monthEnd);

                    if (!hasCoinActivity && !hasStockActivity) {
                        log.debug("사용자 {} 코인/주식 활동 이력 없음 - 월간 리포트 발송 생략", user.getUserId());
                        continue;
                    }

                    DailyReportDTO report = dailyReportService.generateMonthlyReport(user.getUserId());
                    report.setHasCoinActivity(hasCoinActivity);

                    if (hasStockActivity) {
                        try {
                            DailyReportDTO stockReport = stockDailyReportService.generateStockPeriodReport(user.getUserId(), monthStart, monthEnd);
                            report.setStockBuyCount(stockReport.getStockBuyCount());
                            report.setStockSellCount(stockReport.getStockSellCount());
                            report.setStockTotalBuyAmount(stockReport.getStockTotalBuyAmount());
                            report.setStockTotalSellAmount(stockReport.getStockTotalSellAmount());
                            report.setStockRealizedProfit(stockReport.getStockRealizedProfit());
                            report.setStockUnrealizedProfit(stockReport.getStockUnrealizedProfit());
                            report.setStockTotalProfit(stockReport.getStockTotalProfit());
                            report.setStockProfitRate(stockReport.getStockProfitRate());
                            report.setStockHoldingCount(stockReport.getStockHoldingCount());
                            report.setStockTotalHoldingValue(stockReport.getStockTotalHoldingValue());
                            report.setStockTotalInvestment(stockReport.getStockTotalInvestment());
                            report.setStockSummaries(stockReport.getStockSummaries());
                        } catch (Exception stockEx) {
                            log.warn("사용자 {} 주식 월간 리포트 병합 실패 (코인 리포트는 정상 발송): {}", user.getUserId(), stockEx.getMessage());
                        }
                    }

                    if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                        emailService.sendDailyReport(user.getEmail(), report);
                    }
                    if (user.getDiscordUserId() != null && !user.getDiscordUserId().isEmpty()) {
                        // ⭐⭐⭐ [수정] 일일 리포트와 동일하게 sendCategorizedReportDM로 통합 ⭐⭐⭐
                        discordBotService.sendCategorizedReportDM(
                            user.getDiscordUserId(), "월간", report.getReportDate().toString(),
                            report, user.getUserId()
                        );
                    }
                    log.info("사용자 {} 월간 리포트 발송 완료", user.getUserId());
                } catch (Exception e) {
                    log.error("사용자 {} 월간 리포트 발송 실패: {}", user.getUserId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("월간 리포트 발송 중 오류: {}", e.getMessage());
        }
        log.info("========== 월간 리포트 발송 완료 ==========");
    }

    /**
     * ★ 추가: 매년 12월 31일 23:50 연간 리포트 발송
     */
    @Scheduled(cron = "0 50 23 31 12 *", zone = "Asia/Seoul")
    public void sendYearlyReport() {
        log.info("========== 연간 리포트 발송 시작 ==========");
        try {
            List<User> users = userRepository.findByIsActive(true);
            for (User user : users) {
                try {
                    LocalDate today = LocalDate.now();
                    LocalDate yearStart = today.withDayOfYear(1);
                    LocalDate yearEnd = today.with(java.time.temporal.TemporalAdjusters.lastDayOfYear());

                    boolean hasCoinActivity = tradingSettingRepository.findByUserId(user.getUserId()).isPresent()
                            || transactionRepository.countByUserIdAndStatus(user.getUserId(), Transaction.TransactionStatus.HOLDING) > 0
                            || hasCoinPeriodActivity(user.getUserId(), yearStart, yearEnd);
                    boolean hasStockActivity = stockTradingSettingRepository.existsByUserId(user.getUserId())
                            || stockTransactionRepository.countByUserIdAndStatus(user.getUserId(), TransactionStatus.HOLDING) > 0
                            || hasStockPeriodActivity(user.getUserId(), yearStart, yearEnd);

                    if (!hasCoinActivity && !hasStockActivity) {
                        log.debug("사용자 {} 코인/주식 활동 이력 없음 - 연간 리포트 발송 생략", user.getUserId());
                        continue;
                    }

                    DailyReportDTO report = dailyReportService.generateYearlyReport(user.getUserId());
                    report.setHasCoinActivity(hasCoinActivity);

                    if (hasStockActivity) {
                        try {
                            DailyReportDTO stockReport = stockDailyReportService.generateStockPeriodReport(user.getUserId(), yearStart, yearEnd);
                            report.setStockBuyCount(stockReport.getStockBuyCount());
                            report.setStockSellCount(stockReport.getStockSellCount());
                            report.setStockTotalBuyAmount(stockReport.getStockTotalBuyAmount());
                            report.setStockTotalSellAmount(stockReport.getStockTotalSellAmount());
                            report.setStockRealizedProfit(stockReport.getStockRealizedProfit());
                            report.setStockUnrealizedProfit(stockReport.getStockUnrealizedProfit());
                            report.setStockTotalProfit(stockReport.getStockTotalProfit());
                            report.setStockProfitRate(stockReport.getStockProfitRate());
                            report.setStockHoldingCount(stockReport.getStockHoldingCount());
                            report.setStockTotalHoldingValue(stockReport.getStockTotalHoldingValue());
                            report.setStockTotalInvestment(stockReport.getStockTotalInvestment());
                            report.setStockSummaries(stockReport.getStockSummaries());
                        } catch (Exception stockEx) {
                            log.warn("사용자 {} 주식 연간 리포트 병합 실패 (코인 리포트는 정상 발송): {}", user.getUserId(), stockEx.getMessage());
                        }
                    }

                    if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                        emailService.sendDailyReport(user.getEmail(), report);
                    }
                    if (user.getDiscordUserId() != null && !user.getDiscordUserId().isEmpty()) {
                        // ⭐⭐⭐ [수정] 일일 리포트와 동일하게 sendCategorizedReportDM로 통합 ⭐⭐⭐
                        discordBotService.sendCategorizedReportDM(
                            user.getDiscordUserId(), "연간", report.getReportDate().toString(),
                            report, user.getUserId()
                        );
                    }
                    log.info("사용자 {} 연간 리포트 발송 완료", user.getUserId());
                } catch (Exception e) {
                    log.error("사용자 {} 연간 리포트 발송 실패: {}", user.getUserId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("연간 리포트 발송 중 오류: {}", e.getMessage());
        }
        log.info("========== 연간 리포트 발송 완료 ==========");
    }

     /**
     * ⭐⭐⭐ 신규: 자정에 어제 총자산 스냅샷 캐시 정리 (00:05 KST) ⭐⭐⭐
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    public void clearYesterdaySnapshotCache() {
        log.info("=== 총자산 스냅샷 캐시 정리 시작 (00:05 KST) ===");
        try {
            riskManagementService.clearYesterdayTotalAssetSnapshot();
            riskManagementService.clearDailySellAmountCache();
            log.info("=== 총자산 스냅샷 캐시 정리 완료 ===");
        } catch (Exception e) {
            log.error("총자산 스냅샷 캐시 정리 실패: {}", e.getMessage());
        }
    }

    // ⭐⭐⭐ [신규 추가] 매일 23:59 KST - 일별 자산 스냅샷 생성 ⭐⭐⭐
    // 왜: 대시보드/보유자산 차트에 일별 평가금액+불입금액 데이터 제공
    // 23:50 일일리포트 → 23:59 자산스냅샷 → 00:00 AI가중치초기화 → 00:05 캐시정리 순서
    @Scheduled(cron = "0 59 23 * * *", zone = "Asia/Seoul")
    public void createDailyAssetSnapshot() {
        log.info("====== 일별 자산 스냅샷 생성 시작 (23:59 KST) ======");
        try {
            dailyAssetSnapshotService.createAllUsersSnapshot();
            log.info("✅ 일별 자산 스냅샷 생성 완료");
        } catch (Exception e) {
            log.error("❌ 일별 자산 스냅샷 생성 실패: {}", e.getMessage(), e);
        }
    }
    // ⭐⭐⭐ [신규 추가 끝] ⭐⭐⭐

    // ======================================================
    // AI 뉴스 분석 스케줄러
    // ======================================================

    /**
     * 매일 00:00 KST - AI 가중치 초기화
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void resetAiWeights() {
        log.info("====== AI 가중치 초기화 시작 (매일 00:00 KST) ======");
        try {
            newsAnalysisService.resetAllWeights();
            log.info("✅ AI 가중치 초기화 완료");
        } catch (Exception e) {
            log.error("❌ AI 가중치 초기화 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 3시간마다 - 뉴스 수집 및 AI 분석
     * 실행 시간: 00:00, 03:00, 06:00, 09:00, 12:00, 15:00, 18:00, 21:00 KST
     */
    @Scheduled(cron = "0 0 */3 * * *", zone = "Asia/Seoul")
    public void collectAndAnalyzeNews() {
        log.info("====== 뉴스 수집 및 AI 분석 시작 (3시간마다) ======");
        
        try {
            // 1. 뉴스 수집
            log.info("📰 뉴스 수집 시작...");
            List<String> targetCoins = getActiveUserCoins();
            
            if (targetCoins.isEmpty()) {
                log.info("활성 사용자의 투자 코인이 없습니다. 스킵.");
                return;
            }
            
            // List를 받아서 size() 호출
            List<?> collected = newsCollectorService.collectAllNews(targetCoins);
            int collectedCount = collected.size();
            log.info("📰 뉴스 수집 완료: {}건", collectedCount);
            
            // 2. AI 분석 실행 (각 사용자별로)
            log.info("🤖 AI 뉴스 분석 시작...");
            analyzeNewsForAllUsers();
            
            log.info("====== 뉴스 수집 및 AI 분석 완료 ======");
            
        } catch (Exception e) {
            log.error("❌ 뉴스 수집/분석 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 매일 04:00 KST - 오래된 뉴스 데이터 정리 (시스템 점검 시간)
     */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void cleanupOldNews() {
        log.info("====== 뉴스 데이터 정리 시작 (매일 04:00 KST) ======");
        try {
            int deleted = newsCollectorService.cleanupOldNews();
            log.info("✅ 오래된 뉴스 삭제 완료: {}건", deleted);
        } catch (Exception e) {
            log.error("❌ 뉴스 데이터 정리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 활성 사용자들의 투자 코인 목록 조회
     */
    private List<String> getActiveUserCoins() {
        List<TradingSetting> settings = tradingSettingRepository.findAll();
        return settings.stream()
                .filter(s -> s.getCoinSymbols() != null && !s.getCoinSymbols().isEmpty())
                .flatMap(s -> s.getCoinSymbols().stream())
                .map(String::trim)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 모든 활성 사용자에 대해 AI 분석 실행
     */
    private void analyzeNewsForAllUsers() {
        List<TradingSetting> settings = tradingSettingRepository.findAll();
        
        for (TradingSetting setting : settings) {
            if (setting.getUseAiAnalysis() == null || !setting.getUseAiAnalysis()) {
                continue; // AI 분석 비활성화된 사용자 스킵
            }
            
            String userId = setting.getUser().getUserId();

            for (String coin : setting.getCoinSymbols()) {
                try {
                    newsAnalysisService.analyzeNewsForCoin(userId, coin.trim());
                } catch (Exception e) {
                    log.error("사용자 {} 코인 {} 분석 실패: {}", userId, coin, e.getMessage());
                }
            }
        }
    }

    /**
     * ⭐⭐⭐ [신규] 기간(주간/월간/연간) 내 코인 거래 이력 존재 여부 ⭐⭐⭐
     * 왜: 설정도 삭제하고 전량 매도해서 지금은 활동이 없어도, 해당 기간 안에 실제 거래가
     *     있었다면 그 기간의 리포트는 한 번은 보내주는 게 맞다는 판단. (일일 리포트는
     *     "설정 존재 OR 현재 보유"만으로 계속 판단 — 오늘 하루 안의 활동은 이미 그 기준에
     *     자연스럽게 포함되므로 별도 변경 없음)
     */
    private boolean hasCoinPeriodActivity(String userId, LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
        return !transactionRepository.findByUserIdAndCreatedAtBetween(userId, startDateTime, endDateTime).isEmpty()
                || !transactionRepository.findByUserIdAndSoldAtBetween(userId, startDateTime, endDateTime).isEmpty();
    }

    /**
     * ⭐⭐⭐ [신규] 기간(주간/월간/연간) 내 주식 거래 이력 존재 여부 ⭐⭐⭐
     */
    private boolean hasStockPeriodActivity(String userId, LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
        boolean hasBuys = stockTransactionRepository.findByUserIdAndCreatedAtAfter(userId, startDateTime).stream()
                .anyMatch(t -> !t.getCreatedAt().isAfter(endDateTime));
        boolean hasSells = !stockTransactionRepository.findSoldTransactionsByDateRange(userId, startDateTime, endDateTime).isEmpty();
        return hasBuys || hasSells;
    }
}