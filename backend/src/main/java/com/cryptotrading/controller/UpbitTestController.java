package com.cryptotrading.controller;

import com.cryptotrading.dto.upbit.UpbitAccountDTO;
import com.cryptotrading.dto.upbit.UpbitMarketDTO;
import com.cryptotrading.dto.upbit.UpbitTickerDTO;
import com.cryptotrading.entity.CoinInfo;
import com.cryptotrading.service.CoinInfoService;
import com.cryptotrading.service.UpbitApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/upbit/test")
@RequiredArgsConstructor
public class UpbitTestController {

    private final UpbitApiService upbitApiService;
    private final CoinInfoService coinInfoService;

    /**
     * 마켓 코드 조회 테스트
     */
    @GetMapping("/markets")
    public ResponseEntity<List<UpbitMarketDTO>> testGetMarkets() {
        List<UpbitMarketDTO> markets = upbitApiService.getMarketAll();
        return ResponseEntity.ok(markets);
    }

    /**
     * 현재가 조회 테스트
     */
    @GetMapping("/ticker")
    public ResponseEntity<List<UpbitTickerDTO>> testGetTicker(
            @RequestParam List<String> markets
    ) {
        List<UpbitTickerDTO> tickers = upbitApiService.getTicker(markets);
        return ResponseEntity.ok(tickers);
    }

    /**
     * 계좌 조회 테스트 (API 키 필요)
     */
    @GetMapping("/accounts")
    public ResponseEntity<List<UpbitAccountDTO>> testGetAccounts() {
        List<UpbitAccountDTO> accounts = upbitApiService.getAccounts();
        return ResponseEntity.ok(accounts);
    }

    /**
     * 코인 정보 업데이트 테스트
     */
    @PostMapping("/update-coins")
    public ResponseEntity<?> testUpdateCoins() {
        coinInfoService.updateCoinInfo();
        Map<String, String> response = new HashMap<>();
        response.put("message", "코인 정보 업데이트 완료");
        return ResponseEntity.ok(response);
    }

    /**
     * 활성화된 코인 목록 조회
     */
    @GetMapping("/active-coins")
    public ResponseEntity<List<CoinInfo>> testGetActiveCoins() {
        List<CoinInfo> coins = coinInfoService.getActiveCoins();
        return ResponseEntity.ok(coins);
    }
}