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