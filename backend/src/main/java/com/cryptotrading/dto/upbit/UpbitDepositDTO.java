package com.cryptotrading.dto.upbit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 업비트 입금 내역 DTO
 * API: GET /v1/deposits
 * 
 * [응답 필드 설명]
 * - type: 입금 유형
 * - uuid: 입금 고유 UUID
 * - currency: 통화 코드 (KRW, BTC 등)
 * - state: 입금 상태 (ACCEPTED: 완료, REJECTED: 거절 등)
 * - amount: 입금 금액
 * - fee: 입금 수수료
 * - transaction_type: 입금 유형 상세 (default: 일반입금, interest: 예치금 이용료)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpbitDepositDTO {

    /** 입금 유형 */
    private String type;

    /** 입금 고유 UUID */
    private String uuid;

    /** 통화 코드 */
    private String currency;

    /** 입금 상태 (ACCEPTED: 완료) */
    private String state;

    /** 입금 생성 시간 */
    @JsonProperty("created_at")
    private String createdAt;

    /** 입금 완료 시간 */
    @JsonProperty("done_at")
    private String doneAt;

    /** 입금 금액 */
    private BigDecimal amount;

    /** 입금 수수료 */
    private BigDecimal fee;

    /** 입금 유형 상세 (default: 일반입금, interest: 예치금 이용료) */
    @JsonProperty("transaction_type")
    private String transactionType;
}