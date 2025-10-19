package com.cryptotrading.dto.upbit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpbitAccountDTO {
    
    @JsonProperty("currency")
    private String currency; // 화폐 (KRW, BTC, ETH 등)
    
    @JsonProperty("balance")
    private BigDecimal balance; // 주문가능 금액/수량
    
    @JsonProperty("locked")
    private BigDecimal locked; // 주문 중 묶여있는 금액/수량
    
    @JsonProperty("avg_buy_price")
    private BigDecimal avgBuyPrice; // 매수평균가
    
    @JsonProperty("avg_buy_price_modified")
    private Boolean avgBuyPriceModified; // 매수평균가 수정 여부
    
    @JsonProperty("unit_currency")
    private String unitCurrency; // 평단가 기준 화폐 (KRW)
}