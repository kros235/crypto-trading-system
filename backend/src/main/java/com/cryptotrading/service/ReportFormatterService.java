package com.cryptotrading.service;

import com.cryptotrading.dto.notification.DailyReportDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * ⭐⭐⭐ [신규] 코인/주식 리포트 본문(거래요약+손익현황+보유현황+보유종목상세)을 텍스트로 생성하는 공용 서비스 ⭐⭐⭐
 * 왜: 웹훅 채널(NotificationService)과 Discord DM(DiscordBotService)이 각각 다른 로직/포맷으로
 *     리포트를 만들다 보니 내용이 서로 달라지는 문제가 있었음(사용자 피드백).
 *     이 서비스가 만든 텍스트를 두 채널이 동일하게 사용해서 완전히 같은 내용이 오도록 통일함.
 *     보유 종목 상세 표에는 종목코드/심볼만 사용해서(고정폭이라 절대 안 잘림), 긴 종목명은
 *     표 위에 별도 범례 줄로 보여준다.
 */
@Slf4j
@Service
public class ReportFormatterService {

    /**
     * [코인] 섹션 + [주식] 섹션을 순서대로 이어붙인 텍스트를 생성.
     * report.hasCoinActivity가 false면 [코인] 섹션 생략, stockBuyCount가 null이면 [주식] 섹션 생략.
     */
    public String buildCategorySections(DailyReportDTO report) {
        boolean showCoin = report.getHasCoinActivity() == null || Boolean.TRUE.equals(report.getHasCoinActivity());
        boolean showStock = report.getStockBuyCount() != null;

        StringBuilder sb = new StringBuilder();
        if (showCoin) {
            sb.append(buildCoinSection(report));
        }
        if (showCoin && showStock) {
            sb.append("\n━━━━━━━━━━━━━━━━━━━━━━\n\n");
        }
        if (showStock) {
            sb.append(buildStockSection(report));
        }
        return sb.toString();
    }

    private String buildCoinSection(DailyReportDTO report) {
        String profitSign = report.getTotalProfit().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        String emoji = report.getTotalProfit().compareTo(BigDecimal.ZERO) >= 0 ? "📈" : "📉";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
            📊 [코인] 거래 요약
            • 매수: %d건 (%s원)
            • 매도: %d건 (%s원)
            
            %s [코인] 손익 현황
            • 실현 손익: %s%s원
            • 평가 손익: %s%s원
            • 총 손익: %s%s원 (%s%s%%)
            
            📦 [코인] 보유 현황
            • 보유 종목: %d종목
            • 총 평가액: %s원
            • 투자 원금: %s원
            
            """,
            report.getBuyCount(), formatNumber(report.getTotalBuyAmount()),
            report.getSellCount(), formatNumber(report.getTotalSellAmount()),
            emoji,
            profitSign, formatNumber(report.getRealizedProfit()),
            report.getUnrealizedProfit().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "",
            formatNumber(report.getUnrealizedProfit()),
            profitSign, formatNumber(report.getTotalProfit()),
            profitSign, report.getProfitRate().setScale(2, RoundingMode.HALF_UP),
            // ⭐⭐⭐ [버그 수정] 누락됐던 마지막 3개 인자(보유종목/총평가액/투자원금) 추가 ⭐⭐⭐
            report.getHoldingCount(),
            formatNumber(report.getTotalHoldingValue()),
            formatNumber(report.getTotalInvestment())
        ));

        List<DailyReportDTO.CoinSummary> coins = report.getCoinSummaries();
        if (coins != null && !coins.isEmpty()) {
            // ⭐⭐⭐ [수정] 수량 칸 폭 16 → 9로 축소 ⭐⭐⭐
            // 왜: 전체 줄 길이(종목+수량+평가액+손익)가 Discord 카드 폭을 넘어서
            //     수익률이 긴 종목에서 줄바꿈이 발생했음. 대부분의 코인 수량은 짧은 숫자라
            //     9칸이면 충분하고(예: "0.01932539"), SHIB처럼 극단적으로 긴 수량만
            //     그 줄에서 컬럼이 살짝 밀릴 수 있으나 전체 줄바꿈보다는 훨씬 낫다.
            sb.append("📦 [코인] 보유 종목 상세\n```\n");
            sb.append(padRight("종목", 9)).append(padRight("수량", 9)).append(padRight("평가액", 10)).append("손익\n");
            for (DailyReportDTO.CoinSummary c : coins) {
                BigDecimal evaluation = (c.getCurrentPrice() != null && c.getTotalQuantity() != null)
                        ? c.getCurrentPrice().multiply(c.getTotalQuantity()) : BigDecimal.ZERO;
                String qty = c.getTotalQuantity() != null
                        ? c.getTotalQuantity().stripTrailingZeros().toPlainString() : "0";
                BigDecimal pl = c.getProfitLoss() != null ? c.getProfitLoss() : BigDecimal.ZERO;
                BigDecimal rate = c.getProfitRate() != null ? c.getProfitRate() : BigDecimal.ZERO;
                String sign = pl.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                sb.append(padRight(c.getCoinSymbol(), 9))
                  .append(padRight(qty, 9))
                  .append(padRight(String.format("%,.0f원", evaluation), 10))
                  .append(sign).append(String.format("%,.0f", pl)).append("원 (")
                  .append(sign).append(rate.setScale(2, RoundingMode.HALF_UP)).append("%)\n");
            }
            sb.append("```\n");
        }

        return sb.toString();
    }

    private String buildStockSection(DailyReportDTO report) {
        String profitSign = report.getStockTotalProfit().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        String emoji = report.getStockTotalProfit().compareTo(BigDecimal.ZERO) >= 0 ? "📈" : "📉";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
            📊 [주식] 거래 요약
            • 매수: %d건 (%s원)
            • 매도: %d건 (%s원)
            
            %s [주식] 손익 현황
            • 실현 손익: %s%s원
            • 평가 손익: %s%s원
            • 총 손익: %s%s원 (%s%s%%)
            
            📦 [주식] 보유 현황
            • 보유 종목: %d종목
            • 총 평가액: %s원
            • 투자 원금: %s원
            
            """,
            report.getStockBuyCount(), formatNumber(report.getStockTotalBuyAmount()),
            report.getStockSellCount(), formatNumber(report.getStockTotalSellAmount()),
            emoji,
            profitSign, formatNumber(report.getStockRealizedProfit()),
            report.getStockUnrealizedProfit().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "",
            formatNumber(report.getStockUnrealizedProfit()),
            profitSign, formatNumber(report.getStockTotalProfit()),
            profitSign, report.getStockProfitRate().setScale(2, RoundingMode.HALF_UP),
            // ⭐⭐⭐ [버그 수정] 누락됐던 마지막 3개 인자(보유종목/총평가액/투자원금) 추가 ⭐⭐⭐
            report.getStockHoldingCount(),
            formatNumber(report.getStockTotalHoldingValue()),
            formatNumber(report.getStockTotalInvestment())
        ));

        List<DailyReportDTO.StockSummary> stocks = report.getStockSummaries();
        if (stocks != null && !stocks.isEmpty()) {
            sb.append("📦 [주식] 보유 종목 상세\n");
            // ⭐⭐⭐ [핵심] 표에는 종목코드만 쓰고, 긴 종목명은 표 위에 범례로 별도 표시 (잘림 방지) ⭐⭐⭐
            for (DailyReportDTO.StockSummary s : stocks) {
                if (s.getStockName() != null && !s.getStockName().equals(s.getStockCode())) {
                    sb.append("※ ").append(s.getStockCode()).append(" = ").append(s.getStockName()).append("\n");
                }
            }
            sb.append("```\n");
            sb.append(padRight("종목", 8)).append(padRight("수량", 8)).append(padRight("평가액", 10)).append("손익\n");
            for (DailyReportDTO.StockSummary s : stocks) {
                BigDecimal evaluation = (s.getCurrentPrice() != null && s.getTotalQuantity() != null)
                        ? s.getCurrentPrice().multiply(s.getTotalQuantity()) : BigDecimal.ZERO;
                String qty = s.getTotalQuantity() != null
                        ? s.getTotalQuantity().stripTrailingZeros().toPlainString() + "주" : "0주";
                BigDecimal pl = s.getProfitLoss() != null ? s.getProfitLoss() : BigDecimal.ZERO;
                BigDecimal rate = s.getProfitRate() != null ? s.getProfitRate() : BigDecimal.ZERO;
                String sign = pl.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                sb.append(padRight(s.getStockCode(), 8))
                  .append(padRight(qty, 8))
                  .append(padRight(String.format("%,.0f원", evaluation), 10))
                  .append(sign).append(String.format("%,.0f", pl)).append("원 (")
                  .append(sign).append(rate.setScale(2, RoundingMode.HALF_UP)).append("%)\n");
            }
            sb.append("```\n");
        }

        return sb.toString();
    }

    private String formatNumber(BigDecimal number) {
        if (number == null) return "0";
        return String.format("%,.0f", number);
    }

    /** 모노스페이스 폰트 기준 시각적 폭 계산 (한글/한자는 영문 대비 2배 폭) */
    private int visualWidth(String s) {
        int width = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean isWide = (c >= 0xAC00 && c <= 0xD7A3) || (c >= 0x1100 && c <= 0x11FF)
                    || (c >= 0x3130 && c <= 0x318F) || (c >= 0x4E00 && c <= 0x9FFF) || (c >= 0xFF00 && c <= 0xFFEF);
            width += isWide ? 2 : 1;
        }
        return width;
    }

    private String padRight(String s, int width) {
        int visual = visualWidth(s);
        if (visual >= width) return s + " ";
        StringBuilder sb = new StringBuilder(s);
        for (int i = 0; i < width - visual; i++) sb.append(' ');
        return sb.toString();
    }
}