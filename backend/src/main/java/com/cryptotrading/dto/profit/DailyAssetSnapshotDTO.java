package com.cryptotrading.dto.profit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 일별 자산 스냅샷 응답 DTO
 * 프론트엔드 자산 변동 추이 차트에서 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyAssetSnapshotDTO {

    /** 스냅샷 날짜 (yyyy-MM-dd) */
    private String date;

    /** 평가금액 (KRW잔고 + 코인평가액) */
    private BigDecimal evaluationAmount;

    /** 누적 불입금액 */
    private BigDecimal depositAmount;

    /** 수익 금액 (평가금액 - 불입금액) */
    private BigDecimal profitAmount;

    /** 수익률 (%) */
    private BigDecimal profitRate;

    /** KRW 잔고 */
    private BigDecimal krwBalance;

    /** 코인 평가액 */
    private BigDecimal coinEvaluation;
}