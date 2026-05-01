package com.cryptotrading.dto.stock;

import com.cryptotrading.dto.kis.KisQuoteDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 주식 종목 가격 정보 DTO (목록 표시용 경량 버전)
 * Phase 2 Day 60
 *
 * 용도: StockListView, StockHoldingsView 등에서 N개 종목의 현재가/변동률을
 *      한 번의 응답으로 받아오기 위한 응답 전용 DTO.
 *
 * KIS API 응답(KisQuoteDTO.CurrentPrice)과의 차이:
 *  - 필드명을 한글 친화적으로 정리 (currentPrice, changeRate 등)
 *  - String → BigDecimal 변환 완료 (프론트에서 numeric 처리 용이)
 *  - 목록 페이지에 필요한 필드만 노출 (필드 수 축소)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPriceDTO {

    /** 종목코드 (예: 409820) */
    private String stockCode;

    /** 현재가 */
    private BigDecimal currentPrice;

    /** 전일 대비 (절대값) */
    private BigDecimal changeFromPrevDay;

    /** 전일 대비율 (%) - 양수: 상승, 음수: 하락 */
    private BigDecimal changeRate;

    /** 누적 거래량 */
    private BigDecimal accumulatedVolume;

    /** 누적 거래대금 */
    private BigDecimal accumulatedTradingValue;

    /** 시가 */
    private BigDecimal openPrice;

    /** 고가 */
    private BigDecimal highPrice;

    /** 저가 */
    private BigDecimal lowPrice;

    /** 전일 종가 */
    private BigDecimal prevClosePrice;

    /**
     * KisQuoteDTO.CurrentPrice → StockPriceDTO 변환
     * KIS API 응답의 String 필드를 BigDecimal로 안전 변환.
     */
    public static StockPriceDTO fromKisQuote(String stockCode, KisQuoteDTO.CurrentPrice quote) {
        if (quote == null) {
            return StockPriceDTO.builder()
                    .stockCode(stockCode)
                    .build();
        }

        return StockPriceDTO.builder()
                .stockCode(stockCode)
                .currentPrice(parseDecimal(quote.getCurrentPrice()))
                .changeFromPrevDay(parseDecimal(quote.getChangeFromPrevDay()))
                .changeRate(parseDecimal(quote.getChangeRate()))
                .accumulatedVolume(parseDecimal(quote.getAccumulatedVolume()))
                .accumulatedTradingValue(parseDecimal(quote.getAccumulatedTradingValue()))
                .openPrice(parseDecimal(quote.getOpenPrice()))
                .highPrice(parseDecimal(quote.getHighPrice()))
                .lowPrice(parseDecimal(quote.getLowPrice()))
                .prevClosePrice(parseDecimal(quote.getPrevClosePrice()))
                .build();
    }

    /** null/빈문자열 안전 변환 */
    private static BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}