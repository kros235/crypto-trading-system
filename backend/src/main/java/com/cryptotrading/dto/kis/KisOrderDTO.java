package com.cryptotrading.dto.kis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * KIS API 주문 관련 DTO
 * Phase 2 Day 50: 주문 요청/응답
 */
public class KisOrderDTO {

    /**
     * 주문 요청 (매수/매도)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderRequest {
        /** 종목코드 (6자리) */
        private String stockCode;

        /** 주문 수량 */
        private int quantity;

        /** 주문 단가 (0: 시장가) */
        private int price;

        /** 주문 유형 (00: 지정가, 01: 시장가) */
        @Builder.Default
        private String orderType = "01";

        /** 매수/매도 구분 */
        private String side; // BUY or SELL
    }

    /**
     * 주문 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderResponse {
        @JsonProperty("rt_cd")
        private String returnCode;

        @JsonProperty("msg_cd")
        private String messageCode;

        @JsonProperty("msg1")
        private String message;

        @JsonProperty("output")
        private OrderOutput output;

        public boolean isSuccess() {
            return "0".equals(returnCode);
        }
    }

    /**
     * 주문 응답 상세
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderOutput {
        /** 주문번호 */
        @JsonProperty("ODNO")
        private String orderNumber;

        /** 주문시각 */
        @JsonProperty("ORD_TMD")
        private String orderTime;

        /** KRX FRGN 주문번호 */
        @JsonProperty("KRX_FWDG_ORD_ORGNO")
        private String krxOrderOrgNumber;
    }
}