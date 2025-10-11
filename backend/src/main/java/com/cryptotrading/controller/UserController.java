package com.cryptotrading.controller;

import com.cryptotrading.dto.UserInfoDTO;
import com.cryptotrading.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserInfoDTO> getProfile(Authentication authentication) {
        String userId = authentication.getName();
        UserInfoDTO userInfo = userService.getUserInfo(userId);
        return ResponseEntity.ok(userInfo);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody Map<String, String> updates,
            Authentication authentication
    ) {
        try {
            String userId = authentication.getName();
            userService.updateUserInfo(userId, updates);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "프로필이 성공적으로 업데이트되었습니다");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("프로필 업데이트 실패: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/api-keys")
    public ResponseEntity<?> saveApiKeys(
            @RequestBody Map<String, String> apiKeys,
            Authentication authentication
    ) {
        try {
            String userId = authentication.getName();
            String accessKey = apiKeys.get("accessKey");
            String secretKey = apiKeys.get("secretKey");
            
            userService.saveApiKeys(userId, accessKey, secretKey);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "API 키가 안전하게 저장되었습니다");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("API 키 저장 실패: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/api-keys")
    public ResponseEntity<?> deleteApiKeys(Authentication authentication) {
        try {
            String userId = authentication.getName();
            userService.deleteApiKeys(userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "API 키가 삭제되었습니다");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("API 키 삭제 실패: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}