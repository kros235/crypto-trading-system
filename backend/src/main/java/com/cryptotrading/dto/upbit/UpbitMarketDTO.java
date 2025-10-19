package com.cryptotrading.dto.upbit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpbitMarketDTO {
    
    @JsonProperty("market")
    private String market; // 마켓 코드 (KRW-BTC)
    
    @JsonProperty("korean_name")
    private String koreanName; // 한글명
    
    @JsonProperty("english_name")
    private String englishName; // 영문명
    
    @JsonProperty("market_warning")
    private String marketWarning; // 유의종목 여부 (NONE, CAUTION)
}