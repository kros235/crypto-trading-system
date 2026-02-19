package com.cryptotrading.dto.kis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * KIS API 계좌 잔고 조회 DTO
 * Phase 2 Day 50: 잔고 조회
 */

@Getter
@Setter
public class KisAccountDTO {

    /**
     * 보유 종목 정보 (output1)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StockHolding {
        /** 종목코드 */
        @JsonProperty("pdno")
        private String stockCode;

        /** 종목명 */
        @JsonProperty("prdt_name")
        private String stockName;

        /** 보유수량 */
        @JsonProperty("hldg_qty")
        private String holdingQuantity;

        /** 매입평균가 */
        @JsonProperty("pchs_avg_pric")
        private String averagePurchasePrice;

        /** 현재가 */
        @JsonProperty("prpr")
        private String currentPrice;

        /** 평가손익금액 */
        @JsonProperty("evlu_pfls_amt")
        private String evaluationProfitLoss;

        /** 평가손익률 */
        @JsonProperty("evlu_pfls_rt")
        private String evaluationProfitLossRate;

        /** 평가금액 */
        @JsonProperty("evlu_amt")
        private String evaluationAmount;

        /** 매입금액 */
        @JsonProperty("pchs_amt")
        private String purchaseAmount;

        /** BigDecimal 변환 헬퍼 */
        public int getHoldingQty() {
            return holdingQuantity != null ? Integer.parseInt(holdingQuantity) : 0;
        }

        public BigDecimal getCurrentPriceDecimal() {
            return currentPrice != null ? new BigDecimal(currentPrice) : BigDecimal.ZERO;
        }

        public BigDecimal getProfitLossDecimal() {
            return evaluationProfitLoss != null ? new BigDecimal(evaluationProfitLoss) : BigDecimal.ZERO;
        }
    }

    /**
     * 계좌 총 정보 (output2)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AccountSummary {
        /** 예수금총금액 */
        @JsonProperty("dnca_tot_amt")
        private String totalDeposit;

        /** 총 평가금액 */
        @JsonProperty("tot_evlu_amt")
        private String totalEvaluationAmount;

        /** 총 매입금액 */
        @JsonProperty("pchs_amt_smtl_amt")
        private String totalPurchaseAmount;

        /** 총 평가손익금액 */
        @JsonProperty("evlu_pfls_smtl_amt")
        private String totalProfitLoss;

        public BigDecimal getTotalDepositDecimal() {
            return totalDeposit != null ? new BigDecimal(totalDeposit) : BigDecimal.ZERO;
        }

        public BigDecimal getTotalProfitLossDecimal() {
            return totalProfitLoss != null ? new BigDecimal(totalProfitLoss) : BigDecimal.ZERO;
        }
    }

    /**
     * 잔고 조회 전체 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BalanceResponse {
        @JsonProperty("rt_cd")
        private String returnCode;

        @JsonProperty("msg_cd")
        private String messageCode;

        @JsonProperty("msg1")
        private String message;

        @JsonProperty("output1")
        private List<StockHolding> holdings;

        @JsonProperty("output2")
        private List<AccountSummary> summaryList;

        public boolean isSuccess() {
            return "0".equals(returnCode);
        }

        public AccountSummary getSummary() {
            return (summaryList != null && !summaryList.isEmpty()) ? summaryList.get(0) : null;
        }
    }
}