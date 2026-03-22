package com.cryptotrading.scheduler;

import com.cryptotrading.service.StockTradingBotService;
import com.cryptotrading.service.StockTradingBotService.BotExecutionResult;
import com.cryptotrading.service.MarketHolidayService;
import com.cryptotrading.service.StockRiskManagementService;
import com.cryptotrading.service.DiscordBotService;
import com.cryptotrading.service.EmailService;
import com.cryptotrading.entity.StockTradingSetting;
import com.cryptotrading.entity.User;
import com.cryptotrading.repository.UserRepository;
import com.cryptotrading.repository.StockTradingSettingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 주식/ETF 자동매매 스케줄러 (Phase 2)
 *
 * [Phase 1 TradingScheduler와의 차이점]
 * 1. 거래 시간 : 24시간 → 09:00~15:30 KST 정규장만
 * 2. 실행 주기 : 5분마다 → 3분마다 (ETF 유동성 대응)
 * 3. 장 시작 알림 (08:50 KST) 추가
 * 4. 장 마감 알림 + 내일 휴장일 안내 (15:20 KST) 추가
 * 5. 레버리지 ETF 보유기간 경고 알림 (09:05 KST) 추가
 * 6. 장 마감 후 일일 캐시 정리 (15:35 KST) 추가
 *
 * [알림 발송 대상]
 * - stockTradingSettingRepository.findAll() → 주식 거래 설정이 있는 활성 사용자만
 * - Discord DM (discordUserId 등록 시) + 이메일 (email 항상)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StockTradingScheduler {

    private final StockTradingBotService stockTradingBotService;
    private final MarketHolidayService marketHolidayService;
    private final StockRiskManagementService stockRiskManagementService;
    private final DiscordBotService discordBotService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final StockTradingSettingRepository stockTradingSettingRepository;

    // =========================================================
    // 1. 주식 자동매매 메인 스케줄러 (3분 주기)
    // =========================================================

    /**
     * 3분마다 주식 자동매매 실행
     * - isMarketOpen()  : 09:00~15:30 + 휴장일 체크 (StockRiskManagementService)
     * - isStockBotEnabled() : Redis 봇 활성화 체크 (StockRiskManagementService)
     * - 위 두 체크는 StockTradingBotService.executeForUser() 내부에서 수행됨
     * - 거래 미발생 시 DEBUG 레벨 로그 → 장 중 INFO 로그 범람 방지
     */
    @Scheduled(cron = "0 */3 * * * *", zone = "Asia/Seoul")
    public void executeStockAutoTrading() {
        log.debug("[주식 스케줄러] 3분 주기 실행 체크: {}", LocalDateTime.now());

        try {
            List<BotExecutionResult> results = stockTradingBotService.executeForAllUsers();

            if (results.isEmpty()) {
                return;
            }

            int successCount = 0, skipCount = 0, errorCount = 0;
            int totalBuys = 0, totalSells = 0;

            for (BotExecutionResult result : results) {
                switch (result.getStatus()) {
                    case "SUCCESS" -> {
                        successCount++;
                        totalBuys  += result.getBuyCount();
                        totalSells += result.getSellCount();
                    }
                    case "SKIP"  -> skipCount++;
                    case "ERROR" -> errorCount++;
                }
            }

            // 거래 발생 시에만 INFO 로그 출력
            if (totalBuys > 0 || totalSells > 0) {
                log.info("[주식 스케줄러] 거래 완료 - 매수:{}건 매도:{}건 (성공:{}명 스킵:{}명 오류:{}명)",
                        totalBuys, totalSells, successCount, skipCount, errorCount);
            } else {
                log.debug("[주식 스케줄러] 거래 없음 (성공:{}명 스킵:{}명 오류:{}명)",
                        successCount, skipCount, errorCount);
            }

        } catch (Exception e) {
            log.error("[주식 스케줄러] 자동매매 실행 오류: {}", e.getMessage(), e);
        }
    }

    // =========================================================
    // 2. 장 시작 알림 (08:50 KST, 평일)
    // =========================================================

    /**
     * 매일 08:50 KST - 장 시작 10분 전 알림
     * - 오늘이 휴장일이면 알림 발송 안 함
     * - 주식 거래 설정이 있는 활성 사용자에게만 발송
     */
    @Scheduled(cron = "0 50 8 * * MON-FRI", zone = "Asia/Seoul")
    public void sendMarketOpenAlert() {
        LocalDate today = LocalDate.now();

        // 오늘이 휴장일이면 스킵
        if (marketHolidayService.isHoliday(today)) {
            log.info("[주식 스케줄러] 오늘({})은 휴장일 - 장 시작 알림 스킵", today);
            return;
        }

        log.info("[주식 스케줄러] 장 시작 알림 발송 시작 (08:50 KST)");

        String subject = "✅ 장 시작 알림 - 09:00 개장 예정";
        String message = buildMarketOpenMessage(today);
        sendAlertToStockUsers(subject, message);

        log.info("[주식 스케줄러] 장 시작 알림 발송 완료");
    }

    // =========================================================
    // 3. 장 마감 알림 + 휴장일 전일 알림 (15:20 KST, 평일)
    // =========================================================

    /**
     * 매일 15:20 KST - 장 마감 10분 전 알림 + 내일 휴장 여부 안내
     * - 오늘이 휴장일이면 스킵 (정규장 없으므로)
     * - 내일이 휴장일이면 알림 메시지에 추가 안내 포함
     */
    @Scheduled(cron = "0 20 15 * * MON-FRI", zone = "Asia/Seoul")
    public void sendMarketCloseAlert() {
        LocalDate today = LocalDate.now();

        // 오늘이 휴장일이면 스킵
        if (marketHolidayService.isHoliday(today)) {
            log.info("[주식 스케줄러] 오늘({})은 휴장일 - 장 마감 알림 스킵", today);
            return;
        }

        log.info("[주식 스케줄러] 장 마감 알림 발송 시작 (15:20 KST)");

        LocalDate tomorrow = today.plusDays(1);
        boolean tomorrowIsHoliday = !marketHolidayService.isTradingDay(tomorrow);
        LocalDate nextTradingDay  = marketHolidayService.getNextTradingDay(today);

        String subject = tomorrowIsHoliday
                ? "🛑 장 마감 알림 + ⚠️ 내일 휴장"
                : "🛑 장 마감 알림 - 15:30 마감 예정";
        String message = buildMarketCloseMessage(today, tomorrowIsHoliday, nextTradingDay);
        sendAlertToStockUsers(subject, message);

        log.info("[주식 스케줄러] 장 마감 알림 발송 완료 (내일 휴장: {})", tomorrowIsHoliday);
    }

    // =========================================================
    // 4. 레버리지 ETF 보유기간 경고 (09:05 KST, 평일)
    // =========================================================

    /**
     * 평일 09:05 KST - 레버리지 ETF 보유기간 경고
     * - 경고(15일 이상): ⚠️ 알림
     * - 긴급(20일 이상): 🚨 알림 (최대 보유기간 도달)
     *
     * 왜 09:05인가: 장 시작(09:00) 직후, 당일 포지션 정리를 유도하기 위함
     * StockTradingBotService의 강제 매도 로직과는 별개 (알림만 담당)
     */
    @Scheduled(cron = "0 5 9 * * MON-FRI", zone = "Asia/Seoul")
    public void sendHoldingDaysWarning() {
        LocalDate today = LocalDate.now();

        if (marketHolidayService.isHoliday(today)) {
            return;
        }

        log.info("[주식 스케줄러] 보유기간 경고 체크 시작 (09:05 KST)");

        List<StockTradingSetting> settings = stockTradingSettingRepository.findAll();

        for (StockTradingSetting setting : settings) {
            try {
                processHoldingDaysWarningForUser(setting);
            } catch (Exception e) {
                log.error("[주식 스케줄러] 사용자 {} 보유기간 경고 체크 실패: {}",
                        setting.getUserId(), e.getMessage());
            }
        }

        log.info("[주식 스케줄러] 보유기간 경고 체크 완료");
    }

    /**
     * 특정 사용자의 보유 포지션 중 경고 대상 알림 발송
     */
    private void processHoldingDaysWarningForUser(StockTradingSetting setting) {
        String userId = setting.getUserId();
        int maxHoldingDays = setting.getMaxHoldingDays() != null
                ? setting.getMaxHoldingDays()
                : StockRiskManagementService.LEVERAGE_ETF_MAX_DAYS;

        List<StockRiskManagementService.HoldingDaysWarning> warnings =
                stockRiskManagementService.getHoldingDaysWarnings(userId, setting);

        if (warnings.isEmpty()) {
            return;
        }

        Optional<User> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();

        if (!Boolean.TRUE.equals(user.getIsActive())) return;

        for (StockRiskManagementService.HoldingDaysWarning warning : warnings) {
            String subject = warning.isUrgent()
                    ? "🚨 [긴급] 레버리지 ETF 최대 보유기간 도달: " + warning.getStockCode()
                    : "⚠️ 레버리지 ETF 보유기간 경고: " + warning.getStockCode();
            String message = buildHoldingDaysWarningMessage(warning, maxHoldingDays);

            // Discord DM
            if (user.getDiscordUserId() != null && !user.getDiscordUserId().isBlank()) {
                discordBotService.sendSystemAlertDM(user.getDiscordUserId(), subject, message);
            }
            // 이메일
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                emailService.sendSystemAlert(user.getEmail(), subject, message);
            }

            log.info("[주식 스케줄러] 보유기간 경고 발송 - userId:{} stock:{} days:{} urgent:{}",
                    userId, warning.getStockCode(), warning.getHoldingDays(), warning.isUrgent());
        }
    }

    // =========================================================
    // 5. 장 마감 후 일일 캐시 정리 (15:35 KST, 평일)
    // =========================================================

    /**
     * 평일 15:35 KST - 장 마감 후 당일 주식 거래 Redis 캐시 정리
     *
     * 왜 별도 스케줄인가:
     * - Phase 1 코인은 자정(00:05)에 캐시 정리 (24시간 거래 사이클)
     * - Phase 2 주식은 장 마감(15:30) 직후 정리 (당일 거래 사이클 종료)
     * - 두 스케줄이 독립적으로 관리되어야 함
     */
    @Scheduled(cron = "0 35 15 * * MON-FRI", zone = "Asia/Seoul")
    public void clearStockDailyCache() {
        LocalDate today = LocalDate.now();

        if (marketHolidayService.isHoliday(today)) {
            return;
        }

        log.info("[주식 스케줄러] 장 마감 후 일일 캐시 정리 시작 (15:35 KST)");
        try {
            stockRiskManagementService.clearStockDailyCache();
            log.info("[주식 스케줄러] 일일 캐시 정리 완료");
        } catch (Exception e) {
            log.error("[주식 스케줄러] 일일 캐시 정리 실패: {}", e.getMessage(), e);
        }
    }

    // =========================================================
    // 6. 공통 헬퍼 - 주식 사용자 알림 발송
    // =========================================================

    /**
     * 주식 거래 설정이 있는 활성 사용자 전체에게 알림 발송
     * Phase 1 TradingScheduler의 사용자 순회 패턴 재사용
     */
    private void sendAlertToStockUsers(String subject, String message) {
        List<StockTradingSetting> settings = stockTradingSettingRepository.findAll();

        for (StockTradingSetting setting : settings) {
            String userId = setting.getUserId();
            Optional<User> userOpt = userRepository.findByUserId(userId);
            if (userOpt.isEmpty()) continue;

            User user = userOpt.get();
            if (!Boolean.TRUE.equals(user.getIsActive())) continue;

            try {
                // Discord DM (discordUserId 등록된 경우만)
                if (user.getDiscordUserId() != null && !user.getDiscordUserId().isBlank()) {
                    discordBotService.sendSystemAlertDM(user.getDiscordUserId(), subject, message);
                }
                // 이메일 (항상 발송)
                if (user.getEmail() != null && !user.getEmail().isBlank()) {
                    emailService.sendSystemAlert(user.getEmail(), subject, message);
                }
            } catch (Exception e) {
                log.error("[주식 스케줄러] 알림 발송 실패 - userId:{} error:{}", userId, e.getMessage());
            }
        }
    }

    // =========================================================
    // 7. 알림 메시지 빌더
    // =========================================================

    private String buildMarketOpenMessage(LocalDate today) {
        return String.format(
                "📈 오늘(%s) 한국 증시가 09:00에 개장합니다.\n\n" +
                "• 정규장: 09:00 ~ 15:30\n" +
                "• 주식 자동매매 봇이 활성화됩니다.\n" +
                "• 거래 전 설정을 다시 한번 확인해 주세요.",
                today);
    }

    private String buildMarketCloseMessage(LocalDate today, boolean tomorrowIsHoliday,
                                           LocalDate nextTradingDay) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🛑 오늘(%s) 한국 증시가 15:30에 마감됩니다.\n\n", today));
        sb.append("• 주식 자동매매 봇이 비활성 상태로 전환됩니다.\n");
        sb.append("• 미처리 포지션은 다음 거래일 장 시작 후 자동 관리됩니다.\n");

        if (tomorrowIsHoliday) {
            sb.append(String.format(
                    "\n⚠️ 내일은 휴장일입니다!\n" +
                    "• 다음 거래일: %s\n" +
                    "• 레버리지 ETF 장기 보유 시 decay 누적에 주의하세요.",
                    nextTradingDay));
        }

        return sb.toString();
    }

    private String buildHoldingDaysWarningMessage(
            StockRiskManagementService.HoldingDaysWarning warning, int maxHoldingDays) {

        String urgencyText = warning.isUrgent()
                ? "🚨 최대 보유기간에 도달했습니다!"
                : "⚠️ 보유기간 경고 임계값에 도달했습니다.";

        return String.format(
                "%s\n\n" +
                "• 종목: %s\n" +
                "• 현재 보유기간: %d 거래일\n" +
                "• 최대 보유기간: %d 거래일\n" +
                "• 매수일: %s\n\n" +
                "레버리지 ETF 장기 보유 시 변동성 끌림(Volatility Drag)으로\n" +
                "인한 가치 침식이 발생할 수 있습니다.\n" +
                "포지션 정리를 검토해 주세요.",
                urgencyText,
                warning.getStockCode(),
                warning.getHoldingDays(),
                maxHoldingDays,
                warning.getBuyDate());
    }
}