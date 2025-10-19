package com.cryptotrading.controller;

import com.cryptotrading.dto.upbit.UpbitAccountDTO;
import com.cryptotrading.dto.upbit.UpbitTickerDTO;
import com.cryptotrading.entity.CoinInfo;
import com.cryptotrading.service.CoinInfoService;
import com.cryptotrading.service.UpbitApiService;
import com.cryptotrading.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coins")
@RequiredArgsConstructor
@Slf4j
public class CoinController {

    private final CoinInfoService coinInfoService;
    private final UpbitApiService upbitApiService;
    private final UserService userService;

    @GetMapping("/active")
    public ResponseEntity<List<CoinInfo>> getActiveCoins() {
        List<CoinInfo> coins = coinInfoService.getActiveCoins();
        return ResponseEntity.ok(coins);
    }

    @GetMapping("/{symbol}/price")
    public ResponseEntity<UpbitTickerDTO> getCoinPrice(@PathVariable String symbol) {
        try {
            UpbitTickerDTO ticker = coinInfoService.getCurrentPrice(symbol);
            return ResponseEntity.ok(ticker);
        } catch (RuntimeException e) {
            log.error("현재가 조회 실패: symbol={}, error={}", symbol, e.getMessage());
            throw e;
        }
    }

    @GetMapping("/prices")
    public ResponseEntity<List<UpbitTickerDTO>> getMultipleCoinPrices(
            @RequestParam List<String> symbols
    ) {
        try {
            List<UpbitTickerDTO> tickers = upbitApiService.getTicker(symbols);
            return ResponseEntity.ok(tickers);
        } catch (RuntimeException e) {
            log.error("현재가 조회 실패: symbols={}, error={}", symbols, e.getMessage());
            throw e;
        }
    }

    @GetMapping("/accounts")
    public ResponseEntity<?> getAccounts(Authentication authentication) {
        try {
            String userId = authentication.getName();
        
            // 사용자의 API 키 조회 및 복호화
            String[] apiKeys = userService.getDecryptedApiKeys(userId);
            String accessKey = apiKeys[0];
            String secretKey = apiKeys[1];
            
            // 업비트 계좌 조회 (API 키 전달)
            List<UpbitAccountDTO> accounts = upbitApiService.getAccounts(accessKey, secretKey);
            return ResponseEntity.ok(accounts);
        } catch (RuntimeException e) {
            log.error("계좌 조회 실패: error={}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateCoinInfo(Authentication authentication) {
        try {
            coinInfoService.updateCoinInfo();
            Map<String, String> response = new HashMap<>();
            response.put("message", "코인 정보가 업데이트되었습니다");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("코인 정보 업데이트 실패: {}", e.getMessage());
            throw e;
        }
    }
}