package com.cryptotrading.controller;

import com.cryptotrading.entity.TradingSetting;
import com.cryptotrading.repository.TradingSettingRepository;
import com.cryptotrading.service.RiskManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
@Slf4j
public class RiskManagementController {

    private final RiskManagementService riskManagementService;
    private final TradingSettingRepository tradingSettingRepository;

    /**
     * 일일 거래 한도 조회 API
     * - totalLimit: 총자산 × dailyTradeLimitPct%
     * - usedAmount: 오늘 매수 사용액
     * - remainingAmount: 남은 한도
     * - usedPercent: 사용률(%)
     */
    @GetMapping("/daily-limit")
    public ResponseEntity<Map<String, Object>> getDailyLimit(Authentication auth) {
        String userId = auth.getName();
        Map<String, Object> result = new HashMap<>();

        try {
            TradingSetting setting = tradingSettingRepository.findByUserId(userId).orElse(null);
            if (setting == null) {
                result.put("totalLimit", 0);
                result.put("usedAmount", 0);
                result.put("remainingAmount", 0);
                result.put("usedPercent", 0);
                result.put("dailyTradeLimitPct", 0);
                return ResponseEntity.ok(result);
            }

            // 실제 일일 한도 (총자산 × dailyTradeLimitPct%)
            BigDecimal effectiveLimit = riskManagementService.calculateEffectiveDailyLimit(userId, setting);
            // 남은 한도
            BigDecimal remaining = riskManagementService.getRemainingDailyLimit(userId, setting);
            // 사용액 = 한도 - 남은 한도
            BigDecimal used = effectiveLimit.subtract(remaining);
            if (used.compareTo(BigDecimal.ZERO) < 0) {
                used = BigDecimal.ZERO;
            }
            // 사용률
            double usedPercent = effectiveLimit.compareTo(BigDecimal.ZERO) > 0
                    ? used.doubleValue() / effectiveLimit.doubleValue() * 100
                    : 0;

            result.put("totalLimit", effectiveLimit.longValue());
            result.put("usedAmount", used.longValue());
            result.put("remainingAmount", remaining.longValue());
            result.put("usedPercent", Math.min(usedPercent, 100));
            result.put("dailyTradeLimitPct", setting.getDailyTradeLimitPct());

            log.debug("일일 한도 조회: userId={}, 한도={}원, 사용={}원, 남은={}원",
                    userId, effectiveLimit, used, remaining);

        } catch (Exception e) {
            log.error("일일 한도 조회 실패: userId={}", userId, e);
            result.put("totalLimit", 0);
            result.put("usedAmount", 0);
            result.put("remainingAmount", 0);
            result.put("usedPercent", 0);
            result.put("error", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }
}