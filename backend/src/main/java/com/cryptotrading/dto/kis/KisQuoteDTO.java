package com.cryptotrading.dto.kis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

/**
 * KIS API 주식 현재가/일봉 조회 DTO
 * Phase 2 Day 50: 시세 조회
 */
public class KisQuoteDTO {

    /**
     * 현재가 조회 응답 (inquire-price)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CurrentPrice {
        /** 현재가 */
        @JsonProperty("stck_prpr")
        private String currentPrice;

        /** 전일 대비 */
        @JsonProperty("prdy_vrss")
        private String changeFromPrevDay;

        /** 전일 대비 부호 (1:상한, 2:상승, 3:보합, 4:하한, 5:하락) */
        @JsonProperty("prdy_vrss_sign")
        private String changeSign;

        /** 전일 대비율 */
        @JsonProperty("prdy_ctrt")
        private String changeRate;

        /** 누적 거래량 */
        @JsonProperty("acml_vol")
        private String accumulatedVolume;

        /** 누적 거래대금 */
        @JsonProperty("acml_tr_pbmn")
        private String accumulatedTradingValue;

        /** 시가 */
        @JsonProperty("stck_oprc")
        private String openPrice;

        /** 고가 */
        @JsonProperty("stck_hgpr")
        private String highPrice;

        /** 저가 */
        @JsonProperty("stck_lwpr")
        private String lowPrice;

        /** 전일 종가 */
        @JsonProperty("stck_sdpr")
        private String prevClosePrice;

        /** BigDecimal 변환 헬퍼 */
        public BigDecimal getCurrentPriceDecimal() {
            return currentPrice != null ? new BigDecimal(currentPrice) : BigDecimal.ZERO;
        }

        public BigDecimal getVolumeDecimal() {
            return accumulatedVolume != null ? new BigDecimal(accumulatedVolume) : BigDecimal.ZERO;
        }
    }

    /**
     * 일봉 조회 응답 (inquire-daily-price)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyCandle {
        /** 영업일자 (yyyyMMdd) */
        @JsonProperty("stck_bsop_date")
        private String businessDate;

        /** 시가 */
        @JsonProperty("stck_oprc")
        private String openPrice;

        /** 고가 */
        @JsonProperty("stck_hgpr")
        private String highPrice;

        /** 저가 */
        @JsonProperty("stck_lwpr")
        private String lowPrice;

        /** 종가 */
        @JsonProperty("stck_clpr")
        private String closePrice;

        /** 거래량 */
        @JsonProperty("acml_vol")
        private String volume;

        /** 거래대금 */
        @JsonProperty("acml_tr_pbmn")
        private String tradingValue;

        /** BigDecimal 변환 헬퍼 */
        public BigDecimal getClosePriceDecimal() {
            return closePrice != null ? new BigDecimal(closePrice) : BigDecimal.ZERO;
        }

        public BigDecimal getVolumeDecimal() {
            return volume != null ? new BigDecimal(volume) : BigDecimal.ZERO;
        }

        public BigDecimal getHighPriceDecimal() {
            return highPrice != null ? new BigDecimal(highPrice) : BigDecimal.ZERO;
        }

        public BigDecimal getLowPriceDecimal() {
            return lowPrice != null ? new BigDecimal(lowPrice) : BigDecimal.ZERO;
        }

        public BigDecimal getOpenPriceDecimal() {
            return openPrice != null ? new BigDecimal(openPrice) : BigDecimal.ZERO;
        }
    }

    /**
     * KIS API 공통 응답 래퍼
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiResponse<T> {
        /** 응답코드 (0: 성공) */
        @JsonProperty("rt_cd")
        private String returnCode;

        /** 응답메시지 코드 */
        @JsonProperty("msg_cd")
        private String messageCode;

        /** 응답메시지 */
        @JsonProperty("msg1")
        private String message;

        /** 응답 본문 */
        @JsonProperty("output")
        private T output;

        /** 응답 본문 (리스트형, 일봉 등) */
        @JsonProperty("output2")
        private java.util.List<T> output2;

        /** 성공 여부 */
        public boolean isSuccess() {
            return "0".equals(returnCode);
        }
    }
}