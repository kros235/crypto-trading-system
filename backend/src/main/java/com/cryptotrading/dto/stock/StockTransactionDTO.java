package com.cryptotrading.dto.stock;

import com.cryptotrading.entity.StockTransaction;
import com.cryptotrading.entity.TransactionStatus;
import com.cryptotrading.entity.TransactionType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주식 거래 이력 DTO
 * Phase 2 Day 58: StockTransaction Entity ↔ API 전송 전용
 *
 * Phase 1 TransactionDTO 와의 차이:
 *  - coinSymbol(String) → stockCode(String) + stockName(String)
 *  - quantity(BigDecimal) → quantity(Integer) [주식은 정수 단위]
 *  - 추가 필드: holdingDays, exchangeRate, highestPrice
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransactionDTO {

    private Long transactionId;
    private String userId;

    @NotBlank(message = "종목 코드는 필수입니다")
    private String stockCode;

    /** 조회 시 종목명 포함 (StockInfoRepository 에서 별도 세팅) */
    private String stockName;

    @NotNull(message = "거래 유형은 필수입니다")
    private TransactionType type;

    @NotNull(message = "수량은 필수입니다")
    @Min(value = 1, message = "수량은 1주 이상이어야 합니다")
    private Integer quantity;

    @NotNull(message = "가격은 필수입니다")
    @DecimalMin(value = "0.01", message = "가격은 0보다 커야 합니다")
    private BigDecimal price;

    private BigDecimal fee;

    @NotNull(message = "총 거래금액은 필수입니다")
    private BigDecimal totalAmount;

    private LocalDateTime createdAt;
    private LocalDateTime soldAt;
    private BigDecimal soldPrice;
    private BigDecimal profitLoss;
    private BigDecimal profitLossPct;
    private BigDecimal targetSellPrice;
    private BigDecimal stopLossPrice;
    private TransactionStatus status;
    private String note;

    /** 보유 기간 중 최고가 (트레일링 스톱용) */
    private BigDecimal highestPrice;

    /** 보유 일수 (거래일 기준) */
    private Integer holdingDays;

    /** 환율 (환노출형 ETF용) */
    private BigDecimal exchangeRate;

    // ──────────────────────────────────────────────────
    // 조회 시 추가 계산 필드 (DB 저장 X)
    // ──────────────────────────────────────────────────

    /** 현재가 (보유 중일 때 KIS API 조회) */
    private BigDecimal currentPrice;

    /** 현재 평가 손익 */
    private BigDecimal currentProfitLoss;

    /** 현재 평가 수익률 (%) */
    private BigDecimal currentProfitLossPct;

    // ──────────────────────────────────────────────────
    // 변환 메서드
    // ──────────────────────────────────────────────────

    /** Entity → DTO (Phase 1 TransactionDTO.fromEntity 패턴 동일) */
    public static StockTransactionDTO fromEntity(StockTransaction entity) {
        return StockTransactionDTO.builder()
                .transactionId(entity.getTransactionId())
                .userId(entity.getUserId())
                .stockCode(entity.getStockCode())
                .type(entity.getType())
                .quantity(entity.getQuantity())
                .price(entity.getPrice())
                .fee(entity.getFee())
                .totalAmount(entity.getTotalAmount())
                .createdAt(entity.getCreatedAt())
                .soldAt(entity.getSoldAt())
                .soldPrice(entity.getSoldPrice())
                .profitLoss(entity.getProfitLoss())
                .profitLossPct(entity.getProfitLossPct())
                .targetSellPrice(entity.getTargetSellPrice())
                .stopLossPrice(entity.getStopLossPrice())
                .status(entity.getStatus())
                .note(entity.getNote())
                .highestPrice(entity.getHighestPrice())
                .holdingDays(entity.getHoldingDays())
                .exchangeRate(entity.getExchangeRate())
                .build();
    }

    /** DTO → Entity (매수 생성 시) */
    public StockTransaction toEntity() {
        return StockTransaction.builder()
                .userId(this.userId)
                .stockCode(this.stockCode)
                .type(this.type)
                .quantity(this.quantity)
                .price(this.price)
                .fee(this.fee != null ? this.fee : BigDecimal.ZERO)
                .totalAmount(this.totalAmount)
                .targetSellPrice(this.targetSellPrice)
                .stopLossPrice(this.stopLossPrice)
                .note(this.note)
                .exchangeRate(this.exchangeRate)
                .status(TransactionStatus.HOLDING)
                .holdingDays(0)
                .build();
    }
}