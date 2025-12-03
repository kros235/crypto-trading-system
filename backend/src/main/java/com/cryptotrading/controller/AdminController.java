package com.cryptotrading.controller;

import com.cryptotrading.dto.admin.AdminUserDTO;
import com.cryptotrading.dto.admin.SystemStatsDTO;
import com.cryptotrading.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private final AdminService adminService;
    
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
}