package com.cryptotrading.dto;

import com.cryptotrading.entity.Transaction;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDTO {

    private Long transactionId;

    private String userId;

    @NotBlank(message = "코인 심볼은 필수입니다")
    private String coinSymbol;

    @NotNull(message = "거래 유형은 필수입니다")
    private Transaction.TransactionType type;

    @NotNull(message = "수량은 필수입니다")
    @DecimalMin(value = "0.00000001", message = "수량은 0보다 커야 합니다")
    private BigDecimal quantity;

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

    private Transaction.TransactionStatus status;

    private String note;

    // 현재가 (조회 시 추가 정보)
    private BigDecimal currentPrice;

    // 평가 수익률 (보유 중일 때)
    private BigDecimal currentProfitLoss;
    private BigDecimal currentProfitLossPct;

    // Entity -> DTO 변환
    public static TransactionDTO fromEntity(Transaction transaction) {
        return TransactionDTO.builder()
                .transactionId(transaction.getTransactionId())
                .userId(transaction.getUserId())
                .coinSymbol(transaction.getCoinSymbol())
                .type(transaction.getType())
                .quantity(transaction.getQuantity())
                .price(transaction.getPrice())
                .fee(transaction.getFee())
                .totalAmount(transaction.getTotalAmount())
                .createdAt(transaction.getCreatedAt())
                .soldAt(transaction.getSoldAt())
                .soldPrice(transaction.getSoldPrice())
                .profitLoss(transaction.getProfitLoss())
                .profitLossPct(transaction.getProfitLossPct())
                .targetSellPrice(transaction.getTargetSellPrice())
                .stopLossPrice(transaction.getStopLossPrice())
                .status(transaction.getStatus())
                .note(transaction.getNote())
                .build();
    }

    // DTO -> Entity 변환
    public Transaction toEntity() {
        return Transaction.builder()
                .transactionId(this.transactionId)
                .userId(this.userId)
                .coinSymbol(this.coinSymbol)
                .type(this.type)
                .quantity(this.quantity)
                .price(this.price)
                .fee(this.fee != null ? this.fee : BigDecimal.ZERO)
                .totalAmount(this.totalAmount)
                .createdAt(this.createdAt)
                .soldAt(this.soldAt)
                .soldPrice(this.soldPrice)
                .profitLoss(this.profitLoss)
                .profitLossPct(this.profitLossPct)
                .targetSellPrice(this.targetSellPrice)
                .stopLossPrice(this.stopLossPrice)
                .status(this.status != null ? this.status : Transaction.TransactionStatus.HOLDING)
                .note(this.note)
                .build();
    }
}