package com.cryptotrading.dto.upbit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 업비트 출금 내역 DTO
 * API: GET /v1/withdraws
 *
 * [응답 필드 설명]
 * - type: 출금 유형
 * - uuid: 출금 고유 UUID
 * - currency: 통화 코드 (KRW 등)
 * - state: 출금 상태 (DONE: 완료, WAITING: 대기 등)
 * - amount: 출금 금액
 * - fee: 출금 수수료
 * - transaction_type: 출금 유형 상세 (default: 일반출금)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpbitWithdrawDTO {

    /** 출금 유형 */
    private String type;

    /** 출금 고유 UUID */
    private String uuid;

    /** 통화 코드 */
    private String currency;

    /** 출금 상태 (DONE: 완료) */
    private String state;

    /** 출금 생성 시간 */
    @JsonProperty("created_at")
    private String createdAt;

    /** 출금 완료 시간 */
    @JsonProperty("done_at")
    private String doneAt;

    /** 출금 금액 */
    private BigDecimal amount;

    /** 출금 수수료 */
    private BigDecimal fee;

    /** 출금 유형 상세 */
    @JsonProperty("transaction_type")
    private String transactionType;
}