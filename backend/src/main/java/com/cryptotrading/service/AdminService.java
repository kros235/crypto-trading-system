package com.cryptotrading.service;

import com.cryptotrading.config.EmailConfig;
import com.cryptotrading.config.NotificationConfig;
import com.cryptotrading.dto.admin.AdminUserDTO;
import com.cryptotrading.dto.admin.SystemStatsDTO;
import com.cryptotrading.entity.Transaction;
import com.cryptotrading.entity.User;
import com.cryptotrading.entity.UserRole;
import com.cryptotrading.repository.TransactionRepository;
import com.cryptotrading.repository.UserRepository;
// ⭐ [추가] 비밀번호 토큰 리포지토리
import com.cryptotrading.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {
    
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationConfig notificationConfig;
    private final EmailConfig emailConfig;
    
    // ⭐⭐⭐ [추가] 비밀번호 초기화 + 사용자 삭제 시 토큰 정리용 ⭐⭐⭐
    private final PasswordResetService passwordResetService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    
    private LocalDateTime lastBotExecution = null;
    private boolean botRunning = false;
    
    /**
     * 전체 사용자 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<AdminUserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::convertToAdminUserDTO);
    }
    
    /**
     * 사용자 상세 조회
     */
    @Transactional(readOnly = true)
    public AdminUserDTO getUserDetail(String odId) {
        User user = userRepository.findByUserId(odId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + odId));
        return convertToAdminUserDTO(user);
    }
    
    /**
     * 사용자 활성화/비활성화
     */
    @Transactional
    public AdminUserDTO toggleUserActive(String odId) {
        User user = userRepository.findByUserId(odId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + odId));
        
        // ★★★ 수정: isActive() → getIsActive() ★★★
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
        
        log.info("사용자 {} 상태 변경: {}", odId, user.getIsActive() ? "활성화" : "비활성화");
        return convertToAdminUserDTO(user);
    }
    
    /**
     * 사용자 역할 변경
     */
    @Transactional
    public AdminUserDTO changeUserRole(String odId, String newRole) {
        User user = userRepository.findByUserId(odId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + odId));
        
        // ★★★ 수정: String → UserRole enum 변환 ★★★
        UserRole role = UserRole.valueOf(newRole);
        user.setRole(role);
        userRepository.save(user);
        
        log.info("사용자 {} 역할 변경: {}", odId, newRole);
        return convertToAdminUserDTO(user);
    }


    // ⭐⭐⭐ [추가] 사용자 비밀번호 초기화 (관리자) ⭐⭐⭐
    /**
     * 관리자가 사용자 비밀번호를 임시 비밀번호로 초기화한다.
     * - 임시 비밀번호는 응답으로 1회 평문 반환 (관리자 화면에서 표시)
     * - 사용자 이메일이 등록되어 있으면 알림 메일 발송
     * - 해당 사용자의 미사용 OTP는 모두 무효화
     *
     * @param targetUserId 대상 사용자 ID
     * @param adminUserId  요청한 관리자 ID (감사 로그용)
     * @return 신규 임시 비밀번호 (평문)
     */
    @Transactional
    public String resetUserPassword(String targetUserId, String adminUserId) {
        // 자기 자신은 이 기능으로 초기화 불가 (관리자는 본인 프로필에서 변경)
        if (targetUserId.equals(adminUserId)) {
            throw new RuntimeException("본인의 비밀번호는 프로필 설정에서 변경해주세요.");
        }

        // 사용자 존재 검증
        User user = userRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + targetUserId));

        // 다른 ADMIN 계정 보호 (다른 관리자의 비번을 임의로 초기화하지 못하도록)
        if (UserRole.ADMIN.equals(user.getRole())) {
            throw new RuntimeException("다른 관리자 계정의 비밀번호는 초기화할 수 없습니다.");
        }

        // 임시 비밀번호 발급 (PasswordResetService에서 BCrypt 해싱 + DB 저장 + 메일 발송)
        String tempPassword = passwordResetService.generateTempPasswordForAdmin(targetUserId);

        log.info("[관리자] 비밀번호 초기화: admin={}, target={}", adminUserId, targetUserId);
        return tempPassword;
    }

    // ⭐⭐⭐ [추가] 사용자 삭제 (관리자) ⭐⭐⭐
    /**
     * 관리자가 사용자를 삭제한다.
     * - 본인/다른 관리자 삭제 방지
     * - 비밀번호 재설정 토큰 cascade 삭제
     * - 그 외 연관 데이터(transactions 등)는 DB의 ON DELETE CASCADE 설정에 위임
     *
     * @param targetUserId 대상 사용자 ID
     * @param adminUserId  요청한 관리자 ID (감사 로그용)
     */
    @Transactional
    public void deleteUser(String targetUserId, String adminUserId) {
        // 자기 자신 삭제 방지
        if (targetUserId.equals(adminUserId)) {
            throw new RuntimeException("본인 계정은 삭제할 수 없습니다.");
        }

        // 사용자 존재 검증
        User user = userRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + targetUserId));

        // 다른 ADMIN 계정 삭제 방지 (관리자 계정 보호)
        if (UserRole.ADMIN.equals(user.getRole())) {
            throw new RuntimeException("관리자 계정은 삭제할 수 없습니다.");
        }

        // 비밀번호 재설정 토큰 사전 정리 (FK 제약은 NO_CONSTRAINT지만, 잔여 데이터 정리 차원)
        try {
            passwordResetTokenRepository.deleteByUserId(targetUserId);
        } catch (Exception e) {
            log.warn("비밀번호 재설정 토큰 정리 실패 (계속 진행): {}", e.getMessage());
        }

        // 사용자 삭제
        // - users 테이블의 user_id를 FK로 참조하는 테이블들은 init.sql에서 ON DELETE CASCADE 설정됨
        //   (예: trading_settings, transactions, coin_news_analysis, password_reset_tokens 등)
        userRepository.delete(user);

        log.info("[관리자] 사용자 삭제 완료: admin={}, target={}", adminUserId, targetUserId);
    }


    
    /**
     * 시스템 통계 조회
     */
    @Transactional(readOnly = true)
    public SystemStatsDTO getSystemStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        
        // 사용자 통계
        List<User> allUsers = userRepository.findAll();
        int totalUsers = allUsers.size();
        // ★★★ 수정: User::isActive → User::getIsActive ★★★
        int activeUsers = (int) allUsers.stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .count();
        int usersWithApiKey = (int) allUsers.stream()
                .filter(u -> u.getApiKeyEncrypted() != null && !u.getApiKeyEncrypted().isEmpty())
                .count();
        
        // 오늘 거래 통계
        List<Transaction> todayTransactions = transactionRepository.findAll().stream()
                .filter(t -> t.getCreatedAt().isAfter(todayStart) && t.getCreatedAt().isBefore(todayEnd))
                .collect(Collectors.toList());
        
        int todayBuyCount = (int) todayTransactions.stream()
                .filter(t -> "BUY".equals(t.getType()))
                .count();
        int todaySellCount = (int) todayTransactions.stream()
                .filter(t -> "SELL".equals(t.getType()))
                .count();
        BigDecimal todayTotalVolume = todayTransactions.stream()
                .map(Transaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 전체 통계
        List<Transaction> allTransactions = transactionRepository.findAll();
        int totalTransactions = allTransactions.size();
        BigDecimal totalInvestment = allTransactions.stream()
                .filter(t -> "BUY".equals(t.getType()))
                .map(Transaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProfit = allTransactions.stream()
                .filter(t -> t.getProfitLoss() != null)
                .map(Transaction::getProfitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 시스템 상태
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double memoryUsage = (double) usedMemory / runtime.maxMemory() * 100;
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        
        return SystemStatsDTO.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .usersWithApiKey(usersWithApiKey)
                .todayBuyCount(todayBuyCount)
                .todaySellCount(todaySellCount)
                .todayTotalVolume(todayTotalVolume)
                .totalTransactions(totalTransactions)
                .totalInvestment(totalInvestment)
                .totalProfit(totalProfit)
                .botRunning(botRunning)
                .lastBotExecution(lastBotExecution)
                .discordEnabled(notificationConfig.isEnabled())
                .emailEnabled(emailConfig.isEnabled())
                .systemStatus("RUNNING")
                .uptime(uptime)
                .memoryUsage(Math.round(memoryUsage * 100.0) / 100.0)
                .cpuUsage(0)
                .build();
    }
    
    /**
     * 봇 실행 상태 업데이트
     */
    public void updateBotStatus(boolean running) {
        this.botRunning = running;
        if (running) {
            this.lastBotExecution = LocalDateTime.now();
        }
    }
    
    /**
     * User -> AdminUserDTO 변환
     */
    private AdminUserDTO convertToAdminUserDTO(User user) {
        int totalTx = transactionRepository.countByUserId(user.getUserId());
        int holdings = transactionRepository.countByUserIdAndStatus(user.getUserId(), Transaction.TransactionStatus.HOLDING);
        
        return AdminUserDTO.builder()
                // ★★★ 수정: getId() 제거 - User 엔티티에 별도 id 없음 ★★★
                .id((long) user.getUserId().hashCode())  // userId의 hashCode 사용
                .userId(user.getUserId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                // ★★★ 수정: isActive() → getIsActive() ★★★
                .isActive(Boolean.TRUE.equals(user.getIsActive()))
                .hasApiKey(user.getApiKeyEncrypted() != null && !user.getApiKeyEncrypted().isEmpty())
                .joinDate(user.getJoinDate())
                .lastLogin(user.getLastLogin())
                .totalTransactions(totalTx)
                .holdingCount(holdings)
                .build();
    }
}