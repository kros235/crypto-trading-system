package com.cryptotrading.service;

import com.cryptotrading.dto.AuthResponse;
import com.cryptotrading.dto.LoginRequest;
import com.cryptotrading.dto.SignupRequest;
import com.cryptotrading.entity.User;
import com.cryptotrading.entity.UserRole;
import com.cryptotrading.repository.UserRepository;
import com.cryptotrading.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;
    private final UserService userService;  // IP 검증용 추가

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        // 중복 체크
        if (userRepository.existsByUserId(request.getUserId())) {
            throw new RuntimeException("이미 존재하는 사용자 ID입니다");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 존재하는 이메일입니다");
        }

        // 사용자 생성
        User user = User.builder()
                .userId(request.getUserId())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .phone(request.getPhone())
                .joinDate(LocalDateTime.now())
                .isActive(true)
                .role(UserRole.USER)
                .build();

        userRepository.save(user);
        log.info("새로운 사용자 가입: {}", user.getUserId());

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(user.getUserId(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .message("회원가입이 완료되었습니다")
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String clientIp) {  // clientIp 파라미터 추가
        String userId = request.getUserId(); 

        // 차단 여부 확인
        if (loginAttemptService.isBlocked(userId)) {
            log.warn("차단된 계정 로그인 시도: {}", userId);
            throw new RuntimeException("계정이 잠금되었습니다. 30분 후 다시 시도해주세요.");
        }

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    // 로그인 실패 기록
                    loginAttemptService.loginFailed(userId);
                    return new RuntimeException("사용자를 찾을 수 없습니다");
                });

        // 계정 활성화 체크
        if (!user.getIsActive()) {
            throw new RuntimeException("비활성화된 계정입니다");
        }

        // IP 화이트리스트 검증
        if (!userService.isIpAllowed(userId, clientIp)) {
            log.warn("허용되지 않은 IP에서 로그인 시도: userId={}, ip={}", userId, clientIp);
            throw new RuntimeException("허용되지 않은 IP입니다. 등록된 IP: " + 
                String.join(", ", user.getAllowedIps()));
        }

        // 비밀번호 검증  // ⭐ [수정] `/`를 `//`로 수정
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // 로그인 실패 기록
            loginAttemptService.loginFailed(userId);
            int remaining = loginAttemptService.getRemainingAttempts(userId);
            log.warn("비밀번호 불일치: {} (남은 시도: {}회)", userId, remaining);
            throw new RuntimeException("비밀번호가 일치하지 않습니다. 남은 시도: " + remaining + "회");
        }

        // 로그인 성공 시 시도 횟수 초기화
        loginAttemptService.loginSucceeded(userId);

        // 마지막 로그인 시간 업데이트
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("사용자 로그인: {}", user.getUserId());

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(user.getUserId(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .message("로그인 성공")
                .build();
    }
}