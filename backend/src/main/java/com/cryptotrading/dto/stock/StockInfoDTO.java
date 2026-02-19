package com.cryptotrading.dto.stock;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockInfoDTO {
    private String stockCode;       // 종목코드 (예: 409820)
    private String stockName;       // 종목명
    private String market;          // 시장 (KRX/KOSDAQ)
    private String etfType;         // ETF 유형 (LEVERAGE/INVERSE/NORMAL/STOCK)
    private String underlyingIndex; // 기초지수
    private Double expenseRatio;    // 운용보수율
    private Boolean isActive;       // 활성화 여부
}