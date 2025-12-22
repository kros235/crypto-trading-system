package com.cryptotrading.service;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class DiscordBotService {

    @Value("${discord.bot.token:}")
    private String botToken;

    private JDA jda;
    private boolean initialized = false;

    @PostConstruct
    public void init() {
        if (botToken == null || botToken.isBlank()) {
            log.warn("Discord Bot Token이 설정되지 않았습니다. DM 알림이 비활성화됩니다.");
            return;
        }

        try {
            jda = JDABuilder.createDefault(botToken)
                    .enableIntents(GatewayIntent.DIRECT_MESSAGES)
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
            jda.shutdown();
            log.info("Discord Bot 종료");
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
                    .setDescription("코인 자동매매 시스템과 Discord 연동이 완료되었습니다.\n\n이제부터 매수/매도 알림과 일일 리포트를 DM으로 받으실 수 있습니다.")
                    .setColor(Color.GREEN)
                    .setFooter("코인 자동매매 시스템", null)
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

    /**
     * 매수 체결 알림 DM
     */
    @Async
    public void sendBuyNotification(String discordUserId, String coinSymbol, 
                                     String quantity, String price, String totalAmount) {
        if (!isEnabled() || discordUserId == null || discordUserId.isBlank()) {
            return;
        }

        try {
            User user = jda.retrieveUserById(discordUserId).complete();
            if (user == null) return;

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("📈 매수 체결")
                    .setColor(Color.BLUE)
                    .addField("코인", coinSymbol, true)
                    .addField("수량", quantity, true)
                    .addField("매수가", price + "원", true)
                    .addField("총 금액", totalAmount + "원", false)
                    .setFooter("코인 자동매매 시스템", null)
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
                                      String profitLoss, String profitRate) {
        if (!isEnabled() || discordUserId == null || discordUserId.isBlank()) {
            return;
        }

        try {
            User user = jda.retrieveUserById(discordUserId).complete();
            if (user == null) return;

            boolean isProfit = !profitLoss.startsWith("-");
            Color color = isProfit ? Color.GREEN : Color.RED;
            String emoji = isProfit ? "🟢" : "🔴";

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("📉 매도 체결")
                    .setColor(color)
                    .addField("코인", coinSymbol, true)
                    .addField("수량", quantity, true)
                    .addField("매도가", sellPrice + "원", true)
                    .addField(emoji + " 손익", profitLoss + "원 (" + profitRate + "%)", false)
                    .setFooter("코인 자동매매 시스템", null)
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
                    .addField("코인", coinSymbol, true)
                    .addField("수량", quantity, true)
                    .addField("매도가", sellPrice + "원", true)
                    .addField("🔴 손실", lossAmount + "원 (" + lossRate + "%)", false)
                    .setFooter("코인 자동매매 시스템", null)
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
     */
    @Async
    public void sendDailyReportDM(String discordUserId, String reportDate,
                                   String realizedProfit, String unrealizedProfit,
                                   String totalProfit, String profitRate,
                                   int holdingCount, String totalHoldingValue) {
        if (!isEnabled() || discordUserId == null || discordUserId.isBlank()) {
            return;
        }

        try {
            User user = jda.retrieveUserById(discordUserId).complete();
            if (user == null) return;

            boolean isProfit = !totalProfit.startsWith("-");
            Color color = isProfit ? Color.GREEN : Color.RED;

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("📊 일일 리포트 - " + reportDate)
                    .setColor(color)
                    .addField("💰 실현 손익", realizedProfit + "원", true)
                    .addField("📈 평가 손익", unrealizedProfit + "원", true)
                    .addField("📋 총 손익", totalProfit + "원 (" + profitRate + "%)", false)
                    .addField("🪙 보유 종목", holdingCount + "개", true)
                    .addField("💎 총 평가액", totalHoldingValue + "원", true)
                    .setFooter("코인 자동매매 시스템", null)
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
}