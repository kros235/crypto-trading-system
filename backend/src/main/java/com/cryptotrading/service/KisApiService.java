package com.cryptotrading.service;

import com.cryptotrading.dto.kis.KisAccountDTO;
import com.cryptotrading.dto.kis.KisOrderDTO;
import com.cryptotrading.dto.kis.KisQuoteDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.cryptotrading.dto.stock.StockInfoDTO;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * KIS API 서비스 (시세 조회, 잔고 조회, 주문)
 * Phase 2 Day 50: 시세 조회 기반 구현
 * 
 * KIS API 헤더 구조:
 * - authorization: Bearer {access_token}
 * - appkey: {앱키}
 * - appsecret: {앱시크릿}
 * - tr_id: {거래 ID} (모의/실전별 다름)
 * - custtype: P (개인)
 */
@Service
@Slf4j
public class KisApiService {

    private final WebClient kisWebClient;
    private final KisTokenService kisTokenService;

    @Value("${kis.api.mock-mode:true}")
    private boolean defaultMockMode;

    public KisApiService(
            @Qualifier("kisWebClient") WebClient kisWebClient,
            KisTokenService kisTokenService) {
        this.kisWebClient = kisWebClient;
        this.kisTokenService = kisTokenService;
    }

    // ==========================================
    // 1. 현재가 조회
    // ==========================================

    /**
     * 주식 현재가 조회
     * API: GET /uapi/domestic-stock/v1/quotations/inquire-price
     * tr_id: FHKST01010100
     * 
     * @param userId 사용자 ID
     * @param stockCode 종목코드 (예: "409820")
     * @return 현재가 정보
     */
    public KisQuoteDTO.CurrentPrice getCurrentPrice(String userId, String stockCode) {
        log.debug("[KIS 시세] 현재가 조회 - userId: {}, stockCode: {}", userId, stockCode);

        try {
            KisQuoteDTO.ApiResponse<KisQuoteDTO.CurrentPrice> response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")  // J: 주식
                            .queryParam("FID_INPUT_ISCD", stockCode)
                            .build())
                    .headers(headers -> setCommonHeaders(headers, userId, "FHKST01010100"))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<KisQuoteDTO.ApiResponse<KisQuoteDTO.CurrentPrice>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response != null && response.isSuccess()) {
                log.info("[KIS 시세] 현재가 조회 성공 - {} : {}원",
                        stockCode, response.getOutput().getCurrentPrice());
                return response.getOutput();
            }

            log.warn("[KIS 시세] 현재가 조회 실패 - stockCode: {}, msg: {}",
                    stockCode, response != null ? response.getMessage() : "null");
            return null;

        } catch (WebClientResponseException e) {
            log.error("[KIS 시세] 현재가 API 오류 - stockCode: {}, 상태: {}, 응답: {}",
                    stockCode, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("[KIS 시세] 현재가 조회 예외 - stockCode: {}, 오류: {}",
                    stockCode, e.getMessage());
            return null;
        }
    }

    // ==========================================
    // 2. 일봉 조회
    // ==========================================

    /**
     * 주식 일봉 데이터 조회 (기술적 지표 계산용)
     * API: GET /uapi/domestic-stock/v1/quotations/inquire-daily-price
     * tr_id: FHKST01010400
     * 
     * @param userId 사용자 ID
     * @param stockCode 종목코드
     * @param periodDivCode 기간 구분 (D: 일, W: 주, M: 월)
     * @param count 조회 건수 (최대 100)
     * @return 일봉 데이터 리스트
     */
    public List<KisQuoteDTO.DailyCandle> getDailyCandles(String userId, String stockCode,
                                                          String periodDivCode, int count) {
        log.debug("[KIS 시세] 일봉 조회 - userId: {}, stockCode: {}, count: {}",
                userId, stockCode, count);

        try {
            KisQuoteDTO.ApiResponse<KisQuoteDTO.DailyCandle> response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-daily-price")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", stockCode)
                            .queryParam("FID_PERIOD_DIV_CODE", periodDivCode)
                            .queryParam("FID_ORG_ADJ_PRC", "0")  // 0: 수정주가
                            .build())
                    .headers(headers -> setCommonHeaders(headers, userId, "FHKST01010400"))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<KisQuoteDTO.ApiResponse<KisQuoteDTO.DailyCandle>>() {})
                    .timeout(Duration.ofSeconds(15))
                    .block();

            if (response != null && response.isSuccess() && response.getOutput2() != null) {
                List<KisQuoteDTO.DailyCandle> candles = response.getOutput2();
                // count만큼만 반환
                if (candles.size() > count) {
                    candles = candles.subList(0, count);
                }
                log.info("[KIS 시세] 일봉 조회 성공 - {} : {}건", stockCode, candles.size());
                return candles;
            }

            log.warn("[KIS 시세] 일봉 조회 실패 - stockCode: {}, msg: {}",
                    stockCode, response != null ? response.getMessage() : "null");
            return Collections.emptyList();

        } catch (WebClientResponseException e) {
            log.error("[KIS 시세] 일봉 API 오류 - stockCode: {}, 상태: {}, 응답: {}",
                    stockCode, e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("[KIS 시세] 일봉 조회 예외 - stockCode: {}, 오류: {}",
                    stockCode, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==========================================
    // 3. 잔고 조회 (Day 51 확장 예정)
    // ==========================================

    /**
     * 계좌 잔고 조회
     * API: GET /uapi/domestic-stock/v1/trading/inquire-balance
     * tr_id: VTTC8434R (모의), TTTC8434R (실전)
     * 
     * @param userId 사용자 ID
     * @return 잔고 응답
     */
    public KisAccountDTO.BalanceResponse getBalance(String userId) {
        log.debug("[KIS 잔고] 조회 - userId: {}", userId);

        boolean mockMode = kisTokenService.isMockMode(userId);
        String trId = mockMode ? "VTTC8434R" : "TTTC8434R";
        String accountNo = kisTokenService.getAccountNo(userId);

        if (accountNo == null || accountNo.isEmpty()) {
            log.warn("[KIS 잔고] 계좌번호 미등록 - userId: {}", userId);
            return null;
        }

        // 계좌번호 분리 (앞 8자리 + 뒤 2자리)
        String acntPrefix = accountNo.length() >= 8 ? accountNo.substring(0, 8) : accountNo;
        String acntSuffix = accountNo.length() > 8 ? accountNo.substring(8) : "01";

        try {
            KisAccountDTO.BalanceResponse response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/trading/inquire-balance")
                            .queryParam("CANO", acntPrefix)
                            .queryParam("ACNT_PRDT_CD", acntSuffix)
                            .queryParam("AFHR_FLPR_YN", "N")
                            .queryParam("OFL_YN", "")
                            .queryParam("INQR_DVSN", "02")
                            .queryParam("UNPR_DVSN", "01")
                            .queryParam("FUND_STTL_ICLD_YN", "N")
                            .queryParam("FNCG_AMT_AUTO_RDPT_YN", "N")
                            .queryParam("PRCS_DVSN", "01")
                            .queryParam("CTX_AREA_FK100", "")
                            .queryParam("CTX_AREA_NK100", "")
                            .build())
                    .headers(headers -> setCommonHeaders(headers, userId, trId))
                    .retrieve()
                    .bodyToMono(KisAccountDTO.BalanceResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response != null && response.isSuccess()) {
                log.info("[KIS 잔고] 조회 성공 - userId: {}, 보유종목: {}건",
                        userId, response.getHoldings() != null ? response.getHoldings().size() : 0);
                return response;
            }

            log.warn("[KIS 잔고] 조회 실패 - userId: {}, msg: {}",
                    userId, response != null ? response.getMessage() : "null");
            return null;

        } catch (Exception e) {
            log.error("[KIS 잔고] 조회 예외 - userId: {}, 오류: {}", userId, e.getMessage());
            return null;
        }
    }

    // ==========================================
    // 4. 매수/매도 주문 (Day 51 확장 예정)
    // ==========================================

    /**
     * 매수 주문
     * API: POST /uapi/domestic-stock/v1/trading/order-cash
     * tr_id: VTTC0802U (모의매수), TTTC0802U (실전매수)
     */
    public KisOrderDTO.OrderResponse placeBuyOrder(String userId, KisOrderDTO.OrderRequest request) {
        boolean mockMode = kisTokenService.isMockMode(userId);
        String trId = mockMode ? "VTTC0802U" : "TTTC0802U";
        return placeOrder(userId, request, trId);
    }

    /**
     * 매도 주문
     * API: POST /uapi/domestic-stock/v1/trading/order-cash
     * tr_id: VTTC0801U (모의매도), TTTC0801U (실전매도)
     */
    public KisOrderDTO.OrderResponse placeSellOrder(String userId, KisOrderDTO.OrderRequest request) {
        boolean mockMode = kisTokenService.isMockMode(userId);
        String trId = mockMode ? "VTTC0801U" : "TTTC0801U";
        return placeOrder(userId, request, trId);
    }

    /**
     * 주문 실행 공통 로직
     */
    private KisOrderDTO.OrderResponse placeOrder(String userId,
                                                  KisOrderDTO.OrderRequest request,
                                                  String trId) {
        log.info("[KIS 주문] {} - userId: {}, stockCode: {}, qty: {}, price: {}",
                request.getSide(), userId, request.getStockCode(),
                request.getQuantity(), request.getPrice());

        String accountNo = kisTokenService.getAccountNo(userId);
        String acntPrefix = accountNo.length() >= 8 ? accountNo.substring(0, 8) : accountNo;
        String acntSuffix = accountNo.length() > 8 ? accountNo.substring(8) : "01";

        Map<String, String> body = new HashMap<>();
        body.put("CANO", acntPrefix);
        body.put("ACNT_PRDT_CD", acntSuffix);
        body.put("PDNO", request.getStockCode());
        body.put("ORD_DVSN", request.getOrderType());  // 00: 지정가, 01: 시장가
        body.put("ORD_QTY", String.valueOf(request.getQuantity()));
        body.put("ORD_UNPR", String.valueOf(request.getPrice()));

        try {
            KisOrderDTO.OrderResponse response = kisWebClient.post()
                    .uri("/uapi/domestic-stock/v1/trading/order-cash")
                    .headers(headers -> setCommonHeaders(headers, userId, trId))
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(KisOrderDTO.OrderResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response != null && response.isSuccess()) {
                log.info("[KIS 주문] 성공 - {} {}, 주문번호: {}",
                        request.getSide(), request.getStockCode(),
                        response.getOutput() != null ? response.getOutput().getOrderNumber() : "N/A");
            } else {
                log.warn("[KIS 주문] 실패 - {} {}, msg: {}",
                        request.getSide(), request.getStockCode(),
                        response != null ? response.getMessage() : "null");
            }

            return response;

        } catch (WebClientResponseException e) {
            log.error("[KIS 주문] API 오류 - 상태: {}, 응답: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("[KIS 주문] 예외 - {}", e.getMessage());
            return null;
        }
    }

    // ==========================================
    // 공통 헤더 설정
    // ==========================================

    /**
     * KIS API 공통 헤더 설정
     * - authorization: Bearer 토큰
     * - appkey, appsecret: 사용자별 API 키
     * - tr_id: 거래 ID
     * - custtype: P (개인)
     */
    private void setCommonHeaders(HttpHeaders headers, String userId, String trId) {
        String token = kisTokenService.getAccessToken(userId);
        String appKey = kisTokenService.getAppKey(userId);
        String appSecret = kisTokenService.getAppSecret(userId);

        headers.set("authorization", "Bearer " + token);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", trId);
        headers.set("custtype", "P");  // P: 개인
    }

    // ===================================================================
    // ⭐⭐⭐ Day 51 추가: 주문/잔고/종목검색 API ⭐⭐⭐
    // ===================================================================

    
    /**
     * 종목 검색 (StockInfoService에서 호출)
     * 키워드로 종목명을 검색하여 StockInfoDTO 목록 반환
     * 참고: KIS API에는 종목 검색 전용 API가 없으므로,
     *       로컬 DB 기반 검색 + 수동 종목코드 입력 방식 병행
     */
    public List<StockInfoDTO> searchStocks(String keyword) {
        // KIS API는 종목코드 기반 조회만 지원하므로,
        // 정확한 종목코드가 입력된 경우 해당 종목 정보를 조회
        log.info("종목 검색 - keyword: {}", keyword);
        
        List<StockInfoDTO> results = new java.util.ArrayList<>();
        
        // 종목코드(숫자 6자리)가 입력된 경우 직접 조회
        // 종목코드(숫자 6자리)가 입력된 경우 직접 등록 후보로 반환
        if (keyword.matches("\\d{6}")) {
            try {
                // ⭐ KIS API 호출 없이 종목코드를 등록 후보로 반환
                // 실제 유효성은 사용자가 거래 시 KIS API를 통해 검증됨
                log.info("[KIS] 종목코드 직접 입력: {}", keyword);
                results.add(StockInfoDTO.builder()
                        .stockCode(keyword)
                        .stockName(keyword + " (코드 직접 입력)")
                        .market("KRX")
                        .etfType("NORMAL")
                        .isActive(true)
                        .build());
            } catch (Exception e) {
                log.warn("종목코드 처리 실패: {}", keyword);
            }
        }
        
        return results;
    }    
}