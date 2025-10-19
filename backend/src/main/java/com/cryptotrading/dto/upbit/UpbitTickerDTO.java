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
public class UpbitTickerDTO {
    
    @JsonProperty("market")
    private String market; // 마켓 코드
    
    @JsonProperty("trade_date")
    private String tradeDate; // 최근 거래 일자(UTC)
    
    @JsonProperty("trade_time")
    private String tradeTime; // 최근 거래 시각(UTC)
    
    @JsonProperty("trade_price")
    private BigDecimal tradePrice; // 현재가
    
    @JsonProperty("opening_price")
    private BigDecimal openingPrice; // 시가
    
    @JsonProperty("high_price")
    private BigDecimal highPrice; // 고가
    
    @JsonProperty("low_price")
    private BigDecimal lowPrice; // 저가
    
    @JsonProperty("prev_closing_price")
    private BigDecimal prevClosingPrice; // 전일 종가
    
    @JsonProperty("change")
    private String change; // 변화 (RISE, EVEN, FALL)
    
    @JsonProperty("change_price")
    private BigDecimal changePrice; // 변화액
    
    @JsonProperty("change_rate")
    private BigDecimal changeRate; // 변화율
    
    @JsonProperty("signed_change_price")
    private BigDecimal signedChangePrice; // 부호가 있는 변화액
    
    @JsonProperty("signed_change_rate")
    private BigDecimal signedChangeRate; // 부호가 있는 변화율
    
    @JsonProperty("trade_volume")
    private BigDecimal tradeVolume; // 가장 최근 거래량
    
    @JsonProperty("acc_trade_price")
    private BigDecimal accTradePrice; // 누적 거래대금(UTC 0시 기준)
    
    @JsonProperty("acc_trade_price_24h")
    private BigDecimal accTradePrice24h; // 24시간 누적 거래대금
    
    @JsonProperty("acc_trade_volume")
    private BigDecimal accTradeVolume; // 누적 거래량(UTC 0시 기준)
    
    @JsonProperty("acc_trade_volume_24h")
    private BigDecimal accTradeVolume24h; // 24시간 누적 거래량
    
    @JsonProperty("highest_52_week_price")
    private BigDecimal highest52WeekPrice; // 52주 최고가
    
    @JsonProperty("highest_52_week_date")
    private String highest52WeekDate; // 52주 최고가 달성일
    
    @JsonProperty("lowest_52_week_price")
    private BigDecimal lowest52WeekPrice; // 52주 최저가
    
    @JsonProperty("lowest_52_week_date")
    private String lowest52WeekDate; // 52주 최저가 달성일
    
    @JsonProperty("timestamp")
    private Long timestamp; // 타임스탬프
}