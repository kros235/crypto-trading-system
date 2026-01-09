package com.cryptotrading.controller;

import com.cryptotrading.dto.UserInfoDTO;
import com.cryptotrading.service.UserService;
import com.cryptotrading.service.TotpService; 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

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

    // IP 화이트리스트 관리 API 추가

    /**
     * 허용 IP 목록 조회
     */
    @GetMapping("/allowed-ips")
    public ResponseEntity<?> getAllowedIps(Authentication authentication) {
        try {
            String userId = authentication.getName();
            List<String> ips = userService.getAllowedIps(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("allowedIps", ips);
            response.put("count", ips.size());
            response.put("maxCount", 3);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("IP 목록 조회 실패: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 허용 IP 추가
     */
    @PostMapping("/allowed-ips")
    public ResponseEntity<?> addAllowedIp(
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        try {
            String userId = authentication.getName();
            String ip = request.get("ip");
            
            if (ip == null || ip.trim().isEmpty()) {
                throw new RuntimeException("IP 주소를 입력해주세요");
            }
            
            List<String> ips = userService.addAllowedIp(userId, ip.trim());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "IP가 추가되었습니다");
            response.put("allowedIps", ips);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("IP 추가 실패: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 허용 IP 삭제
     */
    @DeleteMapping("/allowed-ips/{ip}")
    public ResponseEntity<?> removeAllowedIp(
            @PathVariable String ip,
            Authentication authentication
    ) {
        try {
            String userId = authentication.getName();
            List<String> ips = userService.removeAllowedIp(userId, ip);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "IP가 삭제되었습니다");
            response.put("allowedIps", ips);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("IP 삭제 실패: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * IP 화이트리스트 비활성화
     */
    @DeleteMapping("/allowed-ips")
    public ResponseEntity<?> disableIpWhitelist(Authentication authentication) {
        try {
            String userId = authentication.getName();
            userService.disableIpWhitelist(userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "IP 화이트리스트가 비활성화되었습니다");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("IP 화이트리스트 비활성화 실패: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 현재 접속 IP 조회
     */
    @GetMapping("/current-ip")
    public ResponseEntity<?> getCurrentIp(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        
        Map<String, String> response = new HashMap<>();
        response.put("ip", clientIp);
        return ResponseEntity.ok(response);
    }
 
    // ========================
    // 2FA (Two-Factor Authentication) API
    // ========================

    /**
     * 2FA 상태 조회
     */
    @GetMapping("/2fa/status")
    public ResponseEntity<?> get2FAStatus(Authentication authentication) {
        try {
            String userId = authentication.getName();
            boolean enabled = userService.is2FAEnabled(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("enabled", enabled);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("2FA 상태 조회 실패: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 2FA 설정 시작 - QR 코드 및 비밀키 생성
     */
    @PostMapping("/2fa/setup")
    public ResponseEntity<?> setup2FA(Authentication authentication) {
        try {
            String userId = authentication.getName();
            TotpService.TwoFactorSetupResult result = userService.initiate2FASetup(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("secret", result.secret());
            response.put("qrCodeUrl", result.qrCodeUrl());
            response.put("message", "QR 코드를 스캔하고 인증 코드를 입력하세요");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("2FA 설정 시작 실패: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 2FA 설정 확인 및 활성화
     */
    @PostMapping("/2fa/confirm")
    public ResponseEntity<?> confirm2FA(
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        try {
            String userId = authentication.getName();
            String code = request.get("code");
            
            if (code == null || code.length() != 6) {
                throw new RuntimeException("6자리 인증 코드를 입력해주세요");
            }
            
            userService.confirm2FASetup(userId, code);
            
            Map<String, Object> response = new HashMap<>();
            response.put("enabled", true);
            response.put("message", "2FA가 활성화되었습니다");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("2FA 활성화 실패: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 2FA 비활성화
     */
    @PostMapping("/2fa/disable")
    public ResponseEntity<?> disable2FA(
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        try {
            String userId = authentication.getName();
            String code = request.get("code");
            
            if (code == null || code.length() != 6) {
                throw new RuntimeException("6자리 인증 코드를 입력해주세요");
            }
            
            userService.disable2FA(userId, code);
            
            Map<String, Object> response = new HashMap<>();
            response.put("enabled", false);
            response.put("message", "2FA가 비활성화되었습니다");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("2FA 비활성화 실패: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 클라이언트 IP 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    // 비밀번호 변경 API
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        try {
            String userId = authentication.getName();
            String currentPassword = request.get("currentPassword");
            String newPassword = request.get("newPassword");
            String confirmPassword = request.get("confirmPassword");
            
            // 유효성 검사
            if (currentPassword == null || newPassword == null || confirmPassword == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "모든 필드를 입력해주세요");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (!newPassword.equals(confirmPassword)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "새 비밀번호가 일치하지 않습니다");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (newPassword.length() < 8) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "비밀번호는 8자 이상이어야 합니다");
                return ResponseEntity.badRequest().body(error);
            }
            
            userService.changePassword(userId, currentPassword, newPassword);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "비밀번호가 변경되었습니다");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("비밀번호 변경 실패: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}