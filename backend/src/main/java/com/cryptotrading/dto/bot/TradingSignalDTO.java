package com.cryptotrading.dto.bot;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TradingSignalDTO {
    
    public enum SignalType {
        BUY,        // 매수 신호
        SELL,       // 매도 신호 (목표가 도달)
        STOP_LOSS,  // 손절매 신호
        TRAILING_STOP, // 트레일링 스톱 신호
        HOLD        // 대기 (신호 없음)
    }
    
    public enum SignalStrength {
        STRONG,     // 강한 신호 (여러 조건 충족)
        MODERATE,   // 보통 신호 (일부 조건 충족)
        WEAK        // 약한 신호 (최소 조건만 충족)
    }
    
    private String market;
    private SignalType signalType;
    private SignalStrength strength;
    private LocalDateTime detectedAt;
    
    // 가격 정보
    private BigDecimal currentPrice;
    private BigDecimal targetPrice;      // 목표 매도가
    private BigDecimal stopLossPrice;    // 손절가
    
    // 신호 근거
    private String reason;               // 신호 발생 이유
    private int conditionsMet;           // 충족된 조건 수
    private int totalConditions;         // 전체 조건 수
    
    // 추천 거래량
    private BigDecimal suggestedAmount;  // 추천 매수/매도 금액

    // 라운드로빈 우선순위 결정용
    private BigDecimal dropRate;         // 이동평균선 대비 이격도 (%)
}