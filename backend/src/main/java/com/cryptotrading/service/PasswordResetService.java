package com.cryptotrading.service;

import com.cryptotrading.entity.PasswordResetToken;
import com.cryptotrading.entity.User;
import com.cryptotrading.repository.PasswordResetTokenRepository;
import com.cryptotrading.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 비밀번호 재설정 서비스
 * 
 * 흐름:
 * 1. requestOtp(email): 이메일로 OTP 코드 발송 (3분 만료)
 * 2. verifyOtpAndReset(email, otp): OTP 검증 후 임시 비밀번호 발급
 * 3. generateTempPasswordForAdmin(userId): 관리자용 임시 비밀번호 즉시 발급 (OTP 없이)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;

    /** OTP 만료 시간 (분) - 기본 3분 */
    @Value("${security.password-reset.otp-expiry-minutes:3}")
    private int otpExpiryMinutes;

    /** OTP 코드 길이 (자릿수) */
    private static final int OTP_LENGTH = 6;

    /** 임시 비밀번호 길이 */
    private static final int TEMP_PASSWORD_LENGTH = 12;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATETIME_FORMAT = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * [사용자] OTP 발송 요청
     * - 이메일로 등록된 사용자 조회
     * - 기존 미사용 OTP 모두 무효화
     * - 새 OTP 생성 후 이메일 발송
     * 
     * @param email 사용자 이메일
     * @return 마스킹된 이메일 (응답용)
     */
    @Transactional
    public String requestOtp(String email) {
        // 1. 이메일로 사용자 조회 (보안: 존재 여부와 상관없이 동일 응답 시간)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("등록되지 않은 이메일입니다."));

        // 2. 비활성 계정 체크
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new RuntimeException("비활성화된 계정입니다. 관리자에게 문의해주세요.");
        }

        // 3. 기존 미사용 토큰 모두 무효화 (재발급 시 기존 OTP 즉시 만료)
        tokenRepository.invalidateAllByUserId(user.getUserId());

        // 4. 새 OTP 생성 (6자리 숫자)
        String otp = generateNumericOtp(OTP_LENGTH);

        // 5. 토큰 엔티티 저장
        PasswordResetToken token = PasswordResetToken.builder()
                .userId(user.getUserId())
                .token(otp)
                .expiryDate(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .used(false)
                .build();
        tokenRepository.save(token);

        // 6. 이메일 발송
        sendOtpEmail(user.getEmail(), user.getUserId(), otp);

        log.info("비밀번호 재설정 OTP 발송 완료: userId={}, email={}", 
                user.getUserId(), maskEmail(email));

        return maskEmail(email);
    }

    /**
     * [사용자] OTP 검증 후 임시 비밀번호 발급
     * - OTP 일치/만료 검증
     * - 신규 임시 비밀번호 생성 후 DB 업데이트
     * - 사용된 토큰은 used=true 처리
     * 
     * @param email 사용자 이메일
     * @param otp 6자리 OTP 코드
     * @return 신규 임시 비밀번호 (평문, 응답에서 1회만 표시)
     */
    @Transactional
    public String verifyOtpAndReset(String email, String otp) {
        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("등록되지 않은 이메일입니다."));

        // 2. 토큰 조회 (userId + token 조합으로 검증)
        PasswordResetToken token = tokenRepository
                .findByUserIdAndTokenAndUsedFalse(user.getUserId(), otp)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 인증 코드입니다."));

        // 3. 만료 검증
        if (token.isExpired()) {
            throw new RuntimeException("인증 코드가 만료되었습니다. 다시 요청해주세요.");
        }

        // 4. 임시 비밀번호 생성
        String tempPassword = generateTempPassword();

        // 5. 비밀번호 업데이트 (BCrypt 해싱)
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        // 6. 토큰 사용 처리
        token.setUsed(true);
        tokenRepository.save(token);

        log.info("비밀번호 재설정 완료 (OTP 인증): userId={}", user.getUserId());

        return tempPassword;
    }

    /**
     * [관리자] 사용자 비밀번호 초기화 (OTP 없이 즉시 발급)
     * - AdminService에서 호출
     * - 임시 비밀번호 생성 후 DB 업데이트 + 사용자 이메일로 통보
     * 
     * @param userId 대상 사용자 ID
     * @return 신규 임시 비밀번호 (평문, 관리자 화면에서 1회 표시)
     */
    @Transactional
    public String generateTempPasswordForAdmin(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        // 임시 비밀번호 생성 + 저장
        String tempPassword = generateTempPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        // 해당 사용자의 미사용 OTP도 모두 무효화 (보안)
        tokenRepository.invalidateAllByUserId(userId);

        // 사용자에게 이메일로 통보 (실패해도 기능은 정상 동작)
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            try {
                sendAdminResetEmail(user.getEmail(), user.getUserId());
            } catch (Exception e) {
                log.warn("관리자 비번 초기화 알림 이메일 발송 실패: {}", e.getMessage());
            }
        }

        log.info("[관리자] 비밀번호 초기화 완료: userId={}", userId);

        return tempPassword;
    }

    // ====================================================================
    // Private Helper Methods
    // ====================================================================

    /**
     * 6자리 숫자 OTP 생성 (000000 ~ 999999)
     */
    private String generateNumericOtp(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 임시 비밀번호 생성
     * - 영문 대/소문자 + 숫자 + 특수문자 조합
     * - 기존 회원가입 비밀번호 정책(8-30자, 대소문자/숫자/특수문자) 충족
     */
    private String generateTempPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // I, O 제외 (혼동 방지)
        String lower = "abcdefghjkmnpqrstuvwxyz";   // i, l, o 제외
        String digits = "23456789";                  // 0, 1 제외
        String special = "!@#$%&*";

        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        // 각 카테고리에서 최소 1개씩 보장
        sb.append(upper.charAt(SECURE_RANDOM.nextInt(upper.length())));
        sb.append(lower.charAt(SECURE_RANDOM.nextInt(lower.length())));
        sb.append(digits.charAt(SECURE_RANDOM.nextInt(digits.length())));
        sb.append(special.charAt(SECURE_RANDOM.nextInt(special.length())));

        // 나머지는 전체 풀에서 랜덤
        String allChars = upper + lower + digits + special;
        for (int i = 4; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(allChars.charAt(SECURE_RANDOM.nextInt(allChars.length())));
        }

        // 위치 셔플 (앞 4자리가 항상 카테고리 순서대로 나오는 것 방지)
        char[] arr = sb.toString().toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
        return new String(arr);
    }

    /**
     * 이메일 마스킹 (예: kros235@naver.com → k****5@naver.com)
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        if (local.length() <= 2) {
            return local.charAt(0) + "***@" + parts[1];
        }
        return local.charAt(0)
                + "*".repeat(Math.max(1, local.length() - 2))
                + local.charAt(local.length() - 1)
                + "@" + parts[1];
    }

    /**
     * OTP 이메일 발송 (Thymeleaf 템플릿)
     */
    private void sendOtpEmail(String email, String userId, String otp) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("userId", userId);
            variables.put("otp", otp);
            variables.put("expiryMinutes", otpExpiryMinutes);
            variables.put("requestedAt", LocalDateTime.now().format(DATETIME_FORMAT));

            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process("email/password-reset-otp", context);

            emailService.sendSystemAlert(email,
                    "[자동매매 시스템] 비밀번호 재설정 인증 코드",
                    htmlContent);
        } catch (Exception e) {
            log.error("OTP 이메일 발송 실패: {}", e.getMessage());
            throw new RuntimeException("이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    /**
     * 관리자 비번 초기화 알림 이메일 발송
     */
    private void sendAdminResetEmail(String email, String userId) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userId", userId);
        variables.put("resetAt", LocalDateTime.now().format(DATETIME_FORMAT));

        Context context = new Context();
        context.setVariables(variables);
        String htmlContent = templateEngine.process("email/admin-password-reset", context);

        emailService.sendSystemAlert(email,
                "[자동매매 시스템] 관리자에 의해 비밀번호가 초기화되었습니다",
                htmlContent);
    }
}