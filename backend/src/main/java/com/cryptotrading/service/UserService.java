package com.cryptotrading.service;

import com.cryptotrading.dto.UserInfoDTO;
import com.cryptotrading.entity.User;
import com.cryptotrading.repository.UserRepository;
import com.cryptotrading.util.EncryptionUtil;
import com.cryptotrading.service.TotpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionUtil encryptionUtil;
    private final TotpService totpService;

    @Transactional(readOnly = true)
    public UserInfoDTO getUserInfo(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        return UserInfoDTO.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .discordUserId(user.getDiscordUserId())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .joinDate(user.getJoinDate())
                .lastLogin(user.getLastLogin())
                .isActive(user.getIsActive())
                .hasApiKey(user.getApiKeyEncrypted() != null)
                .allowedIps(user.getAllowedIps())
                .ipWhitelistEnabled(user.getAllowedIps() != null && !user.getAllowedIps().isEmpty())    
                .twoFactorEnabled(user.getTwoFactorEnabled() != null && user.getTwoFactorEnabled())           
                .build();
    }

    @Transactional
    public void updateUserInfo(String userId, Map<String, String> updates) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // 이메일 업데이트
        if (updates.containsKey("email")) {
            String newEmail = updates.get("email");
            if (userRepository.existsByEmail(newEmail) && 
                !newEmail.equals(user.getEmail())) {
                throw new RuntimeException("이미 사용 중인 이메일입니다");
            }
            user.setEmail(newEmail);
        }

        // 전화번호 업데이트
        if (updates.containsKey("phone")) {
            user.setPhone(updates.get("phone"));
        }

        // Discord User ID 업데이트
        if (updates.containsKey("discordUserId")) {
            String discordId = updates.get("discordUserId");
            // 빈 문자열이면 null로 저장
            user.setDiscordUserId(discordId != null && !discordId.isBlank() ? discordId : null);
        }

        // 비밀번호 업데이트
        if (updates.containsKey("password")) {
            String newPassword = updates.get("password");
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }

        userRepository.save(user);
        log.info("사용자 정보 업데이트: {}", userId);
    }

    @Transactional
    public void saveApiKeys(String userId, String accessKey, String secretKey) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // API 키 암호화 저장
        user.setApiKeyEncrypted(encryptionUtil.encrypt(accessKey));
        user.setSecretKeyEncrypted(encryptionUtil.encrypt(secretKey));

        userRepository.save(user);
        log.info("API 키 저장 완료: {}", userId);
    }

    @Transactional
    public void deleteApiKeys(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        user.setApiKeyEncrypted(null);
        user.setSecretKeyEncrypted(null);

        userRepository.save(user);
        log.info("API 키 삭제 완료: {}", userId);
    }

    @Transactional(readOnly = true)
    public String[] getDecryptedApiKeys(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        if (user.getApiKeyEncrypted() == null || user.getSecretKeyEncrypted() == null) {
            throw new RuntimeException("등록된 API 키가 없습니다");
        }

        String accessKey = encryptionUtil.decrypt(user.getApiKeyEncrypted());
        String secretKey = encryptionUtil.decrypt(user.getSecretKeyEncrypted());

        return new String[]{accessKey, secretKey};
    }

    // 비밀번호 변경 메서드
    @Transactional
    public void changePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다");
        }
        
        // 새 비밀번호 저장
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.info("비밀번호 변경 완료: {}", userId);
    }

    
    // IP 화이트리스트 관리 메서드 추가

    /**
     * 허용 IP 목록 조회
     */
    @Transactional(readOnly = true)
    public List<String> getAllowedIps(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        return user.getAllowedIps() != null ? user.getAllowedIps() : new ArrayList<>();
    }

    /**
     * 허용 IP 추가
     */
    @Transactional
    public List<String> addAllowedIp(String userId, String ip) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        List<String> allowedIps = user.getAllowedIps();
        if (allowedIps == null) {
            allowedIps = new ArrayList<>();
        }

        // 최대 3개 제한
        if (allowedIps.size() >= 3) {
            throw new RuntimeException("허용 IP는 최대 3개까지 등록할 수 있습니다");
        }

        // 중복 체크
        if (allowedIps.contains(ip)) {
            throw new RuntimeException("이미 등록된 IP입니다");
        }

        // IP 형식 검증
        if (!isValidIp(ip)) {
            throw new RuntimeException("올바른 IP 형식이 아닙니다");
        }

        allowedIps.add(ip);
        user.setAllowedIps(allowedIps);
        userRepository.save(user);

        log.info("IP 추가: userId={}, ip={}", userId, ip);
        return allowedIps;
    }

    /**
     * 허용 IP 삭제
     */
    @Transactional
    public List<String> removeAllowedIp(String userId, String ip) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        List<String> allowedIps = user.getAllowedIps();
        if (allowedIps == null || !allowedIps.contains(ip)) {
            throw new RuntimeException("등록되지 않은 IP입니다");
        }

        allowedIps.remove(ip);
        user.setAllowedIps(allowedIps.isEmpty() ? null : allowedIps);
        userRepository.save(user);

        log.info("IP 삭제: userId={}, ip={}", userId, ip);
        return allowedIps != null ? allowedIps : new ArrayList<>();
    }

    /**
     * IP 화이트리스트 비활성화 (모든 IP 삭제)
     */
    @Transactional
    public void disableIpWhitelist(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        user.setAllowedIps(null);
        userRepository.save(user);

        log.info("IP 화이트리스트 비활성화: userId={}", userId);
    }

    /**
     * IP 검증
     */
    public boolean isIpAllowed(String userId, String clientIp) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        List<String> allowedIps = user.getAllowedIps();
        // 화이트리스트가 비어있으면 모든 IP 허용
        if (allowedIps == null || allowedIps.isEmpty()) {
            return true;
        }

        return allowedIps.contains(clientIp);
    }

    /**
     * IP 형식 검증
     */
    private boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        // IPv4 형식 검증
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ip.matches(ipv4Pattern);
    }

    // 2FA 관련 메서드

    /**
     * 2FA 활성화 상태 확인
     */
    @Transactional(readOnly = true)
    public boolean is2FAEnabled(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && Boolean.TRUE.equals(user.getTwoFactorEnabled());
    }

    /**
     * 2FA 설정 시작 - 비밀키 생성 및 QR 코드 URL 반환
     */
    @Transactional
    public TotpService.TwoFactorSetupResult initiate2FASetup(String userId) {
        return totpService.setup2FA(userId);
    }

    /**
     * 2FA 설정 확인 및 활성화
     */
    @Transactional
    public void confirm2FASetup(String userId, String code) {
        totpService.enable2FA(userId, code);
    }

    /**
     * 2FA 비활성화
     */
    @Transactional
    public void disable2FA(String userId, String code) {
        totpService.disable2FA(userId, code);
    }

}