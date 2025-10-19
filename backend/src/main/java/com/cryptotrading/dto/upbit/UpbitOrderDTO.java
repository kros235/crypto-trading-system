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
public class UpbitOrderDTO {
    
    @JsonProperty("uuid")
    private String uuid; // 주문 UUID
    
    @JsonProperty("side")
    private String side; // 주문 종류 (bid: 매수, ask: 매도)
    
    @JsonProperty("ord_type")
    private String ordType; // 주문 방식 (limit: 지정가, price: 시장가 매수, market: 시장가 매도)
    
    @JsonProperty("price")
    private BigDecimal price; // 주문 가격
    
    @JsonProperty("state")
    private String state; // 주문 상태 (wait, watch, done, cancel)
    
    @JsonProperty("market")
    private String market; // 마켓 ID
    
    @JsonProperty("created_at")
    private String createdAt; // 주문 생성 시간
    
    @JsonProperty("volume")
    private BigDecimal volume; // 사용자가 입력한 주문 수량
    
    @JsonProperty("remaining_volume")
    private BigDecimal remainingVolume; // 체결 후 남은 수량
    
    @JsonProperty("reserved_fee")
    private BigDecimal reservedFee; // 수수료로 예약된 비용
    
    @JsonProperty("remaining_fee")
    private BigDecimal remainingFee; // 남은 수수료
    
    @JsonProperty("paid_fee")
    private BigDecimal paidFee; // 사용된 수수료
    
    @JsonProperty("locked")
    private BigDecimal locked; // 거래에 사용 중인 비용
    
    @JsonProperty("executed_volume")
    private BigDecimal executedVolume; // 체결된 수량
    
    @JsonProperty("trades_count")
    private Integer tradesCount; // 해당 주문에 걸린 체결 수
}