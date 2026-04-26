package com.cryptotrading.controller;

import com.cryptotrading.dto.admin.AdminUserDTO;
import com.cryptotrading.dto.admin.SystemStatsDTO;
import com.cryptotrading.service.AdminService;
import com.cryptotrading.service.CacheService;
import com.cryptotrading.service.LoginAttemptService;
import com.cryptotrading.dto.admin.MonitoringDTO;
import com.cryptotrading.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
// ⭐⭐⭐ [추가] Authentication import - 현재 로그인한 관리자 ID 추출용 ⭐⭐⭐
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private final AdminService adminService;
    private final CacheService cacheService;
    private final LoginAttemptService loginAttemptService;
    private final MonitoringService monitoringService;
    
    /**
     * 시스템 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<SystemStatsDTO> getSystemStats() {
        return ResponseEntity.ok(adminService.getSystemStats());
    }
    
    /**
     * 전체 사용자 목록 조회
     */
    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "joinDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }
    
    /**
     * 사용자 상세 조회
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserDTO> getUserDetail(@PathVariable String userId) {
        return ResponseEntity.ok(adminService.getUserDetail(userId));
    }
    
    /**
     * 사용자 활성화/비활성화 토글
     */
    @PutMapping("/users/{userId}/toggle-active")
    public ResponseEntity<AdminUserDTO> toggleUserActive(@PathVariable String userId) {
        return ResponseEntity.ok(adminService.toggleUserActive(userId));
    }
    
    /**
     * 사용자 역할 변경
     */
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<AdminUserDTO> changeUserRole(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {
        
        String newRole = request.get("role");
        return ResponseEntity.ok(adminService.changeUserRole(userId, newRole));
    }
    
    /**
     * 사용자 강제 로그아웃 (세션 무효화)
     */
    @PostMapping("/users/{userId}/logout")
    public ResponseEntity<Map<String, Object>> forceLogout(@PathVariable String userId) {
        // Redis 세션 무효화 로직 (추후 구현)
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "사용자 " + userId + "의 세션이 무효화되었습니다.");
        return ResponseEntity.ok(response);
    }

    // ⭐⭐⭐ [추가] 사용자 비밀번호 초기화 (관리자) ⭐⭐⭐
    /**
     * 관리자가 특정 사용자의 비밀번호를 임시 비밀번호로 초기화한다.
     * - 응답에 임시 비밀번호 평문이 1회 포함됨 (관리자 화면에서 표시 후 즉시 폐기)
     * - 사용자 이메일이 등록되어 있으면 알림 메일 발송
     * - 본인 / 다른 ADMIN 계정은 초기화 불가
     */
    @Operation(summary = "사용자 비밀번호 초기화", description = "임시 비밀번호로 강제 재설정합니다 (관리자 전용)")
    @PutMapping("/users/{userId}/reset-password")
    public ResponseEntity<Map<String, Object>> resetUserPassword(
            @PathVariable String userId,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            String adminUserId = authentication.getName();
            String tempPassword = adminService.resetUserPassword(userId, adminUserId);

            response.put("success", true);
            response.put("message", "비밀번호가 초기화되었습니다.");
            response.put("tempPassword", tempPassword);
            response.put("userId", userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("비밀번호 초기화 실패: userId={}, error={}", userId, e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ⭐⭐⭐ [추가] 사용자 삭제 (관리자) ⭐⭐⭐
    /**
     * 관리자가 사용자를 영구 삭제한다.
     * - 본인 / 다른 ADMIN 계정은 삭제 불가
     * - DB의 ON DELETE CASCADE 정책에 따라 연관 데이터 자동 삭제
     */
    @Operation(summary = "사용자 삭제", description = "사용자 계정을 영구 삭제합니다 (관리자 전용)")
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> deleteUser(
            @PathVariable String userId,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            String adminUserId = authentication.getName();
            adminService.deleteUser(userId, adminUserId);

            response.put("success", true);
            response.put("message", "사용자가 삭제되었습니다.");
            response.put("userId", userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("사용자 삭제 실패: userId={}, error={}", userId, e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 캐시 통계 조회
     */
    @GetMapping("/cache/stats")
    @Operation(summary = "캐시 통계", description = "Redis 캐시 현황을 조회합니다")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        return ResponseEntity.ok(cacheService.getCacheStats());
    }

    /**
     * 캐시 전체 삭제
     */
    @DeleteMapping("/cache/clear")
    @Operation(summary = "캐시 초기화", description = "전체 캐시를 삭제합니다")
    public ResponseEntity<Map<String, String>> clearCache() {
        cacheService.evictByPattern("*");
        return ResponseEntity.ok(Map.of("message", "캐시가 초기화되었습니다"));
    }

    /**
     * 사용자 계정 잠금 해제
     */
    @PostMapping("/users/{userId}/unlock")
    @Operation(summary = "계정 잠금 해제", description = "로그인 실패로 잠긴 계정을 해제합니다")
    public ResponseEntity<Map<String, String>> unlockUser(@PathVariable String userId) {
        loginAttemptService.unblockUser(userId);
        return ResponseEntity.ok(Map.of("message", userId + " 계정 잠금이 해제되었습니다"));
    } 

    // 시스템 모니터링 API
    /**
     * 시스템 모니터링 메트릭 조회
     */
    @GetMapping("/monitoring")
    @Operation(summary = "시스템 모니터링", description = "JVM, DB, Redis 등 시스템 메트릭을 조회합니다")
    public ResponseEntity<MonitoringDTO> getSystemMonitoring() {
        return ResponseEntity.ok(monitoringService.getSystemMetrics());
    }
    
    /**
     * 슬로우 쿼리 목록 조회
     */
    @GetMapping("/monitoring/slow-queries")
    @Operation(summary = "슬로우 쿼리 목록", description = "최근 슬로우 쿼리 목록을 조회합니다")
    public ResponseEntity<List<MonitoringDTO.SlowQueryInfo>> getSlowQueries() {
        MonitoringDTO metrics = monitoringService.getSystemMetrics();
        return ResponseEntity.ok(metrics.getRecentSlowQueries());
    }

}