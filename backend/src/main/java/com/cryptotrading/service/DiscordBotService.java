package com.cryptotrading.service;

import com.cryptotrading.dto.notification.DailyReportDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DiscordBotService {

    @Value("${discord.bot.token:}")
    private String botToken;

    private JDA jda;
    private boolean initialized = false;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @PostConstruct
    public void init() {
        if (botToken == null || botToken.isBlank()) {
            log.warn("Discord Bot Token이 설정되지 않았습니다. DM 알림이 비활성화됩니다.");
            return;
        }

        try {
            jda = JDABuilder.createDefault(botToken)
                    .enableIntents(GatewayIntent.DIRECT_MESSAGES)
                    .setEnableShutdownHook(false) 
                    .build();
            jda.awaitReady();
            initialized = true;
            log.info("Discord Bot 초기화 완료: {}", jda.getSelfUser().getName());
        } catch (Exception e) {
            log.error("Discord Bot 초기화 실패: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        if (jda != null) {
            log.info("Discord Bot 종료 시작...");
            jda.shutdown();
            try {
                // ⭐ 추가: JDA 종료 완료 대기 (최대 5초)
                if (!jda.awaitShutdown(java.time.Duration.ofSeconds(5))) {
                    log.warn("Discord Bot 종료 타임아웃, 강제 종료");
                    jda.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                jda.shutdownNow();
            }
            log.info("Discord Bot 종료 완료");
        }
    }

    /**
     * Bot 활성화 여부
     */
    public boolean isEnabled() {
        return initialized && jda != null;
    }

    /**
     * 테스트 DM 발송
     */
    public boolean sendTestDM(String discordUserId) {
        if (!isEnabled()) {
            log.warn("Discord Bot이 비활성화 상태입니다.");
            return false;
        }

        try {
            User user = jda.retrieveUserById(discordUserId).complete();
            if (user == null) {
                log.error("Discord 사용자를 찾을 수 없습니다: {}", discordUserId);
                return false;
            }

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🔔 연동 테스트 성공!")
                    .setDescription("코인 & 주식 자동매매 시스템과 Discord 연동이 완료되었습니다.\n\n이제부터 코인/주식 매수/매도 알림과 일일 리포트를 DM으로 받으실 수 있습니다.")
                    .setColor(Color.GREEN)
                    .setFooter("코인 & 주식 자동매매 시스템", null)
                    .setTimestamp(LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant());

            user.openPrivateChannel()
                    .flatMap(channel -> channel.sendMessageEmbeds(embed.build()))
                    .complete();

            log.info("테스트 DM 발송 성공: {}", discordUserId);
            return true;

        } catch (Exception e) {
            log.error("테스트 DM 발송 실패: {} - {}", discordUserId, e.getMessage());
            return false;
        }
    }

    // ⭐⭐⭐ 추가: 시스템 알림 DM 발송 (관리자용) ⭐⭐⭐
    /**
     * 시스템 알림 DM 발송 (관리자용)
     */
    public boolean sendSystemAlertDM(String discordUserId, String subject, String message) {
        if (!isEnabled()) {
            log.warn("Discord Bot이 비활성화 상태입니다.");
            return false;
        }

        if (discordUserId == null || discordUserId.isBlank()) {
            log.warn("Discord User ID가 없습니다.");
            return false;
        }

        try {
            User user = jda.retrieveUserById(discordUserId).complete();
            if (user == null) {
                log.error("Discord 사용자를 찾을 수 없습니다: {}", discordUserId);
                return false;
            }

            Color color = determineAlertColor(subject);
            String formattedMessage = message.replace("━", "─").trim();

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(subject)
                    .setDescription(formattedMessage)
                    .setColor(color)
                    .setFooter("코인 & 주식 자동매매 시스템 - 관리자 알림", null)
                    .setTimestamp(LocalDateTime.now().atZone(KST).toInstant());

            user.openPrivateChannel()
                    .flatMap(channel -> channel.sendMessageEmbeds(embed.build()))
                    .complete();

            log.info("시스템 알림 DM 발송 성공: {} - {}", discordUserId, subject);
            return true;

        } catch (Exception e) {
            log.error("시스템 알림 DM 발송 실패: {} - {}", discordUserId, e.getMessage());
            return false;
        }
    }

    private Color determineAlertColor(String subject) {
        if (subject == null) return Color.GRAY;
        
        if (subject.contains("긴급") || subject.contains("🚨") || subject.contains("위험")) {
            return Color.RED;
        } else if (subject.contains("경고") || subject.contains("⚠️")) {
            return Color.ORANGE;
        } else if (subject.contains("시작") || subject.contains("✅")) {
            return Color.GREEN;
        } else if (subject.contains("종료") || subject.contains("🛑")) {
            return Color.DARK_GRAY;
        }
        return Color.BLUE;
    }

    /**
     * Discord Embed에 맞게 메시지 포맷팅
     */
    private String formatForDiscordEmbed(String message) {
        if (message == null) return "";
        
        // Discord Embed은 이미 Markdown을 지원하므로 대부분 그대로 사용
        // 일부 특수문자 이스케이프만 처리
        return message
                .replace("━", "─")  // 일부 문자 호환성
                .trim();
    }

    /**
     * 매수 체결 알림 DM
     */
    @Async
    public void sendBuyNotification(String discordUserId, String coinSymbol, 
                                     String quantity, String price, String totalAmount, 
                                     String reason) {
        if (!isEnabled() || discordUserId == null || discordUserId.isBlank()) {
            return;
        }

        try {
            User user = jda.retrieveUserById(discordUserId).complete();
            if (user == null) return;

            // ⭐⭐⭐ [추가] 사유 텍스트 처리 ⭐⭐⭐
            String reasonText = (reason != null && !reason.isEmpty()) ? reason : "조건 충족";

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("📈 매수 체결")
                    .setColor(Color.BLUE)
                    .addField("종목", coinSymbol, true)
                    .addField("수량", quantity, true)
                    .addField("매수가", price + "원", true)
                    .addField("총 금액", totalAmount + "원", false)
                    .addField("📌 매수 사유", reasonText, false)  // ⭐⭐⭐ [추가] 매수 사유 필드 ⭐⭐⭐
                    .setFooter("코인 & 주식 자동매매 시스템", null)
                    .setTimestamp(LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant());

            user.openPrivateChannel()
                    .flatMap(channel -> channel.sendMessageEmbeds(embed.build()))
                    .queue(
                        success -> log.debug("매수 알림 DM 발송 성공: {}", discordUserId),
                        error -> log.error("매수 알림 DM 발송 실패: {}", error.getMessage())
                    );

        } catch (Exception e) {
            log.error("매수 알림 DM 발송 오류: {}", e.getMessage());
        }
    }

    /**
     * 매도 체결 알림 DM
     */
    @Async
    public void sendSellNotification(String discordUserId, String coinSymbol,
                                      String quantity, String sellPrice, 
                                      String profitLoss, String profitRate, String reason) {
        if (!isEnabled() || discordUserId == null || discordUserId.isBlank()) {
            return;
        }

        try {
            User user = jda.retrieveUserById(discordUserId).complete();
            if (user == null) return;

            boolean isProfit = !profitLoss.startsWith("-");
            Color color = isProfit ? Color.GREEN : Color.RED;
            String emoji = isProfit ? "🟢" : "🔴";

            // ⭐⭐⭐ [추가] 사유 텍스트 처리 ⭐⭐⭐
            String reasonText = (reason != null && !reason.isEmpty()) ? reason : "조건 충족";

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("📉 매도 체결")
                    .setColor(color)
                    .addField("종목", coinSymbol, true)
                    .addField("수량", quantity, true)
                    .addField("매도가", sellPrice + "원", true)
                    .addField(emoji + " 손익", profitLoss + "원 (" + profitRate + "%)", false)
                    .addField("📌 매도 사유", reasonText, false)  // ⭐⭐⭐ [추가] 매도 사유 필드 ⭐⭐⭐
                    .setFooter("코인 & 주식 자동매매 시스템", null)
                    .setTimestamp(LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant());

            user.openPrivateChannel()
                    .flatMap(channel -> channel.sendMessageEmbeds(embed.build()))
                    .queue(
                        success -> log.debug("매도 알림 DM 발송 성공: {}", discordUserId),
                        error -> log.error("매도 알림 DM 발송 실패: {}", error.getMessage())
                    );

        } catch (Exception e) {
            log.error("매도 알림 DM 발송 오류: {}", e.getMessage());
        }
    }

    /**
     * 손절매 알림 DM
     */
    @Async
    public void sendStopLossNotification(String discordUserId, String coinSymbol,
                                          String quantity, String sellPrice,
                                          String lossAmount, String lossRate) {
        if (!isEnabled() || discordUserId == null || discordUserId.isBlank()) {
            return;
        }

        try {
            User user = jda.retrieveUserById(discordUserId).complete();
            if (user == null) return;

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🚨 손절매 실행")
                    .setColor(Color.RED)
                    .setDescription("설정한 손절 기준에 도달하여 자동 매도되었습니다.")
                    .addField("종목", coinSymbol, true)
                    .addField("수량", quantity, true)
                    .addField("매도가", sellPrice + "원", true)
                    .addField("🔴 손실", lossAmount + "원 (" + lossRate + "%)", false)
                    .setFooter("코인 & 주식 자동매매 시스템", null)
                    .setTimestamp(LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant());

            user.openPrivateChannel()
                    .flatMap(channel -> channel.sendMessageEmbeds(embed.build()))
                    .queue();

        } catch (Exception e) {
            log.error("손절매 알림 DM 발송 오류: {}", e.getMessage());
        }
    }

    /**
     * 일일 리포트 DM
     * ⭐⭐⭐ [Day 63 개선] holdingsBreakdown 파라미터 추가 ⭐⭐⭐
     * 왜: 기존엔 "보유 종목 1개 / 총 평가액 0원"처럼 요약만 보여주고 개별 종목은 알 수 없었음.
     *     요약 줄 아래에 종목명(코드)별 평가액을 나열하는 필드를 추가.
     *     holdingsBreakdown이 비어있으면(null/blank) 기존과 동일하게 요약 필드만 표시.
     */
    @Async
    public void sendDailyReportDM(String discordUserId, String titlePrefix, String reportDate,
                                   String realizedProfit, String unrealizedProfit,
                                   String totalProfit, String profitRate,
                                   int holdingCount, String totalHoldingValue,
                                   List<HoldingRow> holdingsBreakdown) {
        if (!isEnabled() || discordUserId == null || discordUserId.isBlank()) {
            return;
        }

        try {
            User user = jda.retrieveUserById(discordUserId).complete();
            if (user == null) return;

            boolean isProfit = !totalProfit.startsWith("-");
            Color color = isProfit ? Color.GREEN : Color.RED;
            String prefix = (titlePrefix == null) ? "" : titlePrefix;

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("📊 " + prefix + "일일 리포트 - " + reportDate)
                    .setColor(color)
                    .addField("💰 실현 손익", realizedProfit + "원", true)
                    .addField("📈 평가 손익", unrealizedProfit + "원", true)
                    // ⭐⭐⭐ [버그 수정] 투명 스페이서로 3칸을 채워 간격을 좁힘 ⭐⭐⭐
                    .addField("\u200B", "\u200B", true)
                    .addField("📋 총 손익", totalProfit + "원 (" + profitRate + "%)", false)
                    .addField("🪙 보유 종목", holdingCount + "개", true)
                    .addField("💎 총 평가액", totalHoldingValue + "원", true)
                    .addField("\u200B", "\u200B", true);

            // ⭐⭐⭐ [개선] 매수/매도 체결 알림과 동일하게 종목별로 "종목/수량/평가액" 필드를 나란히(inline) 추가 ⭐⭐⭐
            // 왜: 위에서 이미 3칸을 채웠으므로 자동으로 새 줄에서 시작됨
            if (holdingsBreakdown != null && !holdingsBreakdown.isEmpty()) {
                for (HoldingRow row : holdingsBreakdown) {
                    embed.addField("📦 종목", row.name(), true);
                    embed.addField("수량", row.quantity(), true);
                    embed.addField("평가액", row.evaluation(), true);
                }
            }

            embed.setFooter("코인 & 주식 자동매매 시스템", null)
                    .setTimestamp(LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant());

            user.openPrivateChannel()
                    .flatMap(channel -> channel.sendMessageEmbeds(embed.build()))
                    .queue(
                        success -> log.info("일일 리포트 DM 발송 성공: {}", discordUserId),
                        error -> log.error("일일 리포트 DM 발송 실패: {}", error.getMessage())
                    );

        } catch (Exception e) {
            log.error("일일 리포트 DM 발송 오류: {}", e.getMessage());
        }
    }

    /**
     * ★ 추가: 기간 리포트 DM (주간/월간/연간 공통)
     * - 기존 sendDailyReportDM과 동일하나 periodLabel로 제목 구분
     */
    public void sendPeriodReportDM(String discordUserId, String periodLabel,
                                    String reportDate, String realizedProfit,
                                    String unrealizedProfit, String totalProfit,
                                    String profitRate, int holdingCount,
                                    String totalHoldingValue) {
        if (!isEnabled() || discordUserId == null || discordUserId.isBlank()) {
            return;
        }
        try {
            User user = jda.retrieveUserById(discordUserId).complete();
            if (user == null) return;

            boolean isProfit = !totalProfit.startsWith("-");
            Color color = isProfit ? Color.GREEN : Color.RED;

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("📊 " + periodLabel + " 리포트 - " + reportDate)
                    .setColor(color)
                    .addField("💰 실현 손익", realizedProfit + "원", true)
                    .addField("📈 평가 손익", unrealizedProfit + "원", true)
                    .addField("📋 총 손익", totalProfit + "원 (" + profitRate + "%)", false)
                    .addField("🪙 보유 종목", holdingCount + "개", true)
                    .addField("💎 총 평가액", totalHoldingValue + "원", true)
                    .setFooter("코인 & 주식 자동매매 시스템", null)
                    .setTimestamp(LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant());

            user.openPrivateChannel()
                    .flatMap(channel -> channel.sendMessageEmbeds(embed.build()))
                    .queue(
                        success -> log.info("{} 리포트 DM 발송 성공: {}", periodLabel, discordUserId),
                        error -> log.error("{} 리포트 DM 발송 실패: {}", periodLabel, error.getMessage())
                    );
        } catch (Exception e) {
            log.error("{} 리포트 DM 발송 오류: {}", periodLabel, e.getMessage());
        }
    }

    /**
     * ⭐⭐⭐ [Day 63 개선] 코인/주식 보유 종목별 평가액을 한 줄씩 나열한 문자열 생성 ⭐⭐⭐
     * 왜: Discord 일일 리포트 Embed에 "종목별 평가액" 필드를 추가하기 위함.
     *     coinSummaries/stockSummaries 중 하나만 넘겨도 되고(둘 다 null 아니면 합쳐서 표시),
     *     주식은 "종목명 (코드)" 형태로, 코인은 기존처럼 심볼 그대로 표시.
     */
    public List<HoldingRow> buildHoldingsBreakdown(List<DailyReportDTO.CoinSummary> coinSummaries,
                                                     List<DailyReportDTO.StockSummary> stockSummaries) {
        List<HoldingRow> rows = new ArrayList<>();

        if (coinSummaries != null) {
            for (DailyReportDTO.CoinSummary c : coinSummaries) {
                BigDecimal evaluation = (c.getCurrentPrice() != null && c.getTotalQuantity() != null)
                        ? c.getCurrentPrice().multiply(c.getTotalQuantity())
                        : BigDecimal.ZERO;
                String qty = c.getTotalQuantity() != null ? c.getTotalQuantity().toPlainString() : "0";
                rows.add(new HoldingRow(c.getCoinSymbol(), qty, String.format("%,.0f원", evaluation)));
            }
        }

        if (stockSummaries != null) {
            for (DailyReportDTO.StockSummary s : stockSummaries) {
                BigDecimal evaluation = (s.getCurrentPrice() != null && s.getTotalQuantity() != null)
                        ? s.getCurrentPrice().multiply(s.getTotalQuantity())
                        : BigDecimal.ZERO;
                String displayName = (s.getStockName() != null && !s.getStockName().equals(s.getStockCode()))
                        ? s.getStockName() + " (" + s.getStockCode() + ")"
                        : s.getStockCode();
                String qty = s.getTotalQuantity() != null ? s.getTotalQuantity().toPlainString() + "주" : "0주";
                rows.add(new HoldingRow(displayName, qty, String.format("%,.0f원", evaluation)));
            }
        }

        return rows;
    }

    /**
     * 종목별 평가액 한 줄 (Discord Embed 필드 3개 - 종목/수량/평가액 - 로 표시됨)
     */
    public record HoldingRow(String name, String quantity, String evaluation) {}
}