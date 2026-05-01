package com.cryptotrading.controller.stock;

import com.cryptotrading.dto.common.ApiResponse;
import com.cryptotrading.dto.stock.StockTradingSettingDTO;
import com.cryptotrading.service.StockSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stock/settings")
@RequiredArgsConstructor
public class StockSettingController {

    private final StockSettingService stockSettingService;

    @GetMapping
    public ResponseEntity<ApiResponse<StockTradingSettingDTO>> getSettings(Authentication auth) {
        String userId = auth.getName();
        StockTradingSettingDTO settings = stockSettingService.getSettings(userId);
        if (settings == null) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StockTradingSettingDTO>> createSettings(
            Authentication auth,
            @Valid @RequestBody StockTradingSettingDTO dto) {
        String userId = auth.getName();
        StockTradingSettingDTO created = stockSettingService.createSettings(userId, dto);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<StockTradingSettingDTO>> updateSettings(
            Authentication auth,
            @Valid @RequestBody StockTradingSettingDTO dto) {
        String userId = auth.getName();
        StockTradingSettingDTO updated = stockSettingService.updateSettings(userId, dto);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> deleteSettings(Authentication auth) {
        String userId = auth.getName();
        stockSettingService.deleteSettings(userId);
        return ResponseEntity.ok(ApiResponse.success("주식 거래 설정이 삭제되었습니다."));
    }

    @PostMapping("/kis-api-key")
    public ResponseEntity<ApiResponse<String>> updateKisApiKey(
            Authentication auth,
            @RequestBody Map<String, String> request) {
        String userId = auth.getName();
        stockSettingService.updateKisApiKeys(
                userId,
                request.get("appKey"),
                request.get("appSecret"),
                request.get("accountNo")
        );
        return ResponseEntity.ok(ApiResponse.success("KIS API 키가 등록되었습니다."));
    }

    @DeleteMapping("/kis-api-key")
    public ResponseEntity<ApiResponse<String>> deleteKisApiKey(Authentication auth) {
        String userId = auth.getName();
        stockSettingService.deleteKisApiKeys(userId);
        return ResponseEntity.ok(ApiResponse.success("KIS API 키가 삭제되었습니다."));
    }

    @GetMapping("/kis-api-key/status")
    public ResponseEntity<ApiResponse<Boolean>> hasKisApiKey(Authentication auth) {
        String userId = auth.getName();
        boolean hasKey = stockSettingService.hasKisApiKey(userId);
        return ResponseEntity.ok(ApiResponse.success(hasKey));
    }
}