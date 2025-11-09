package com.cryptotrading.controller;

import com.cryptotrading.dto.TradingSettingDTO;
import com.cryptotrading.service.TradingSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/trading-settings")
@RequiredArgsConstructor
@Slf4j
public class TradingSettingController {

    private final TradingSettingService tradingSettingService;

    @GetMapping
    public ResponseEntity<?> getTradingSetting() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("거래 설정 조회 요청: userId={}", userId);
    
        TradingSettingDTO setting = tradingSettingService.getTradingSetting(userId);
    
        if (setting == null) {
            // 404 반환 (설정 없음)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "거래 설정이 없습니다"));
        }
        
        return ResponseEntity.ok(setting);
    }

    @PostMapping
    public ResponseEntity<TradingSettingDTO> createTradingSetting(
            @Valid @RequestBody TradingSettingDTO dto,
            Authentication authentication
    ) {
        try {
            String userId = authentication.getName();
            TradingSettingDTO created = tradingSettingService.createTradingSetting(userId, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            log.error("거래 설정 생성 실패: {}", e.getMessage());
            throw e;
        }
    }

    @PutMapping
    public ResponseEntity<TradingSettingDTO> updateTradingSetting(
            @Valid @RequestBody TradingSettingDTO dto,
            Authentication authentication
    ) {
        try {
            String userId = authentication.getName();
            TradingSettingDTO updated = tradingSettingService.updateTradingSetting(userId, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.error("거래 설정 수정 실패: {}", e.getMessage());
            throw e;
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteTradingSetting(Authentication authentication) {
        try {
            String userId = authentication.getName();
            tradingSettingService.deleteTradingSetting(userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "거래 설정이 삭제되었습니다");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("거래 설정 삭제 실패: {}", e.getMessage());
            throw e;
        }
    }
}