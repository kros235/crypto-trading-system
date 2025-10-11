package com.cryptotrading.service;

import com.cryptotrading.dto.UserInfoDTO;
import com.cryptotrading.entity.User;
import com.cryptotrading.repository.UserRepository;
import com.cryptotrading.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionUtil encryptionUtil;

    @Transactional(readOnly = true)
    public UserInfoDTO getUserInfo(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        return UserInfoDTO.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .joinDate(user.getJoinDate())
                .lastLogin(user.getLastLogin())
                .isActive(user.getIsActive())
                .hasApiKey(user.getApiKeyEncrypted() != null)
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
}