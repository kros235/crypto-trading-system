package com.cryptotrading.service;

import com.cryptotrading.entity.User;
import com.cryptotrading.repository.UserRepository;
import com.cryptotrading.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * TOTP (Time-based One-Time Password) 서비스
 * Google Authenticator와 호환되는 2FA 인증 구현
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TotpService {

    private final EncryptionUtil encryptionUtil;
    private final UserRepository userRepository;

    // TOTP 설정
    private static final int SECRET_SIZE = 20; // 160비트
    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP_SECONDS = 30;
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final String ISSUER = "CryptoTrading";

    // Base32 인코딩용 문자
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    /**
     * 새로운 TOTP 비밀키 생성
     * @return Base32로 인코딩된 비밀키
     */
    public String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[SECRET_SIZE];
        random.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /**
     * 비밀키 암호화 (DB 저장용)
     * @param secret 평문 비밀키
     * @return 암호화된 비밀키
     */
    public String encryptSecret(String secret) {
        return encryptionUtil.encrypt(secret);
    }

    /**
     * 비밀키 복호화
     * @param encryptedSecret 암호화된 비밀키
     * @return 평문 비밀키
     */
    public String decryptSecret(String encryptedSecret) {
        return encryptionUtil.decrypt(encryptedSecret);
    }

    /**
     * Google Authenticator용 QR 코드 URL 생성
     * @param secret Base32 인코딩된 비밀키
     * @param userId 사용자 ID
     * @return otpauth:// 형식의 URL
     */
    public String generateQrCodeUrl(String secret, String userId) {
        String encodedIssuer = urlEncode(ISSUER);
        String encodedAccount = urlEncode(userId);
        
        return String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
            encodedIssuer,
            encodedAccount,
            secret,
            encodedIssuer,
            CODE_DIGITS,
            TIME_STEP_SECONDS
        );
    }

    /**
     * TOTP 코드 검증
     * @param secret Base32 인코딩된 비밀키
     * @param code 사용자가 입력한 6자리 코드
     * @return 검증 성공 여부
     */
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.length() != CODE_DIGITS) {
            return false;
        }

        try {
            int inputCode = Integer.parseInt(code);
            long currentTime = System.currentTimeMillis() / 1000;
            long timeStep = currentTime / TIME_STEP_SECONDS;

            // 시간 허용 범위: 현재 ± 1 스텝 (총 90초 범위)
            for (int i = -1; i <= 1; i++) {
                int generatedCode = generateCode(secret, timeStep + i);
                if (generatedCode == inputCode) {
                    log.debug("TOTP 검증 성공: timeStep={}", timeStep + i);
                    return true;
                }
            }
        } catch (NumberFormatException e) {
            log.warn("TOTP 코드 형식 오류: {}", code);
        }

        log.debug("TOTP 검증 실패");
        return false;
    }

    /**
     * 현재 TOTP 코드 생성 (테스트/디버그용)
     * @param secret Base32 인코딩된 비밀키
     * @return 6자리 코드
     */
    public String getCurrentCode(String secret) {
        long currentTime = System.currentTimeMillis() / 1000;
        long timeStep = currentTime / TIME_STEP_SECONDS;
        int code = generateCode(secret, timeStep);
        return String.format("%06d", code);
    }

    /**
     * TOTP 코드 생성 (내부 메서드)
     */
    private int generateCode(String secret, long timeStep) {
        try {
            byte[] key = base32Decode(secret);
            byte[] data = ByteBuffer.allocate(8).putLong(timeStep).array();

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data);

            // Dynamic Truncation
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                       | ((hash[offset + 1] & 0xFF) << 16)
                       | ((hash[offset + 2] & 0xFF) << 8)
                       | (hash[offset + 3] & 0xFF);

            return binary % (int) Math.pow(10, CODE_DIGITS);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("TOTP 코드 생성 실패", e);
            throw new RuntimeException("TOTP 코드 생성 실패", e);
        }
    }

    /**
     * Base32 인코딩
     */
    private String base32Encode(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bufferSize = 0;

        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xFF);
            bufferSize += 8;

            while (bufferSize >= 5) {
                bufferSize -= 5;
                result.append(BASE32_CHARS.charAt((buffer >> bufferSize) & 0x1F));
            }
        }

        if (bufferSize > 0) {
            buffer <<= (5 - bufferSize);
            result.append(BASE32_CHARS.charAt(buffer & 0x1F));
        }

        return result.toString();
    }

    /**
     * Base32 디코딩
     */
    private byte[] base32Decode(String encoded) {
        encoded = encoded.toUpperCase().replaceAll("[^A-Z2-7]", "");
        
        int byteCount = encoded.length() * 5 / 8;
        byte[] result = new byte[byteCount];
        
        int buffer = 0;
        int bufferSize = 0;
        int index = 0;

        for (char c : encoded.toCharArray()) {
            int value = BASE32_CHARS.indexOf(c);
            if (value < 0) continue;

            buffer = (buffer << 5) | value;
            bufferSize += 5;

            if (bufferSize >= 8) {
                bufferSize -= 8;
                result[index++] = (byte) (buffer >> bufferSize);
            }
        }

        return result;
    }

    /**
     * URL 인코딩
     */
    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    // ★★★ 추가: 2FA 관련 메서드 ★★★

    /**
     * 2FA 설정 시작 (비밀키 생성 및 QR 코드 URL 반환)
     */
    @Transactional
    public TwoFactorSetupResult setup2FA(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        if (user.getTwoFactorEnabled() != null && user.getTwoFactorEnabled()) {
            throw new RuntimeException("이미 2FA가 활성화되어 있습니다");
        }

        String secret = generateSecret();
        String qrCodeUrl = generateQrCodeUrl(secret, userId);

        // 암호화하여 임시 저장 (활성화 전까지)
        user.setTotpSecret(encryptSecret(secret));
        userRepository.save(user);

        log.info("2FA 설정 시작: userId={}", userId);
        
        return new TwoFactorSetupResult(secret, qrCodeUrl);
    }

    /**
     * 2FA 활성화 (OTP 코드 검증 후)
     */
    @Transactional
    public void enable2FA(String userId, String otpCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        if (user.getTotpSecret() == null) {
            throw new RuntimeException("2FA 설정을 먼저 진행해주세요");
        }

        String secret = decryptSecret(user.getTotpSecret());
        
        if (!verifyCode(secret, otpCode)) {
            throw new RuntimeException("OTP 코드가 올바르지 않습니다");
        }

        user.setTwoFactorEnabled(true);
        userRepository.save(user);

        log.info("2FA 활성화 완료: userId={}", userId);
    }

    /**
     * 2FA 비활성화
     */
    @Transactional
    public void disable2FA(String userId, String otpCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        if (user.getTwoFactorEnabled() == null || !user.getTwoFactorEnabled()) {
            throw new RuntimeException("2FA가 활성화되어 있지 않습니다");
        }

        String secret = decryptSecret(user.getTotpSecret());
        
        if (!verifyCode(secret, otpCode)) {
            throw new RuntimeException("OTP 코드가 올바르지 않습니다");
        }

        user.setTotpSecret(null);
        user.setTwoFactorEnabled(false);
        userRepository.save(user);

        log.info("2FA 비활성화 완료: userId={}", userId);
    }

    /**
     * 로그인 시 2FA 검증
     */
    public boolean verify2FAForLogin(String userId, String otpCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        if (user.getTwoFactorEnabled() == null || !user.getTwoFactorEnabled()) {
            return true; // 2FA 미사용
        }

        if (user.getTotpSecret() == null) {
            return true;
        }

        String secret = decryptSecret(user.getTotpSecret());
        return verifyCode(secret, otpCode);
    }

    /**
     * 2FA 활성화 여부 확인
     */
    public boolean is2FAEnabled(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getTwoFactorEnabled() != null && user.getTwoFactorEnabled();
    }

    /**
     * 2FA 필요 여부 확인 (로그인 전 체크용)
     */
    public boolean requires2FA(String userId) {
        return is2FAEnabled(userId);
    }

    // 내부 결과 클래스
    public record TwoFactorSetupResult(String secret, String qrCodeUrl) {}
}