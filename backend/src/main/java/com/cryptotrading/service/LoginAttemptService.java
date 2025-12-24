package com.cryptotrading.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    private static final String LOGIN_ATTEMPT_PREFIX = "login_attempt:";
    private static final String BLOCKED_PREFIX = "login_blocked:";
    private static final int MAX_ATTEMPTS = 5;
    private static final int BLOCK_DURATION_MINUTES = 30;
    private static final int ATTEMPT_EXPIRE_MINUTES = 15;

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 로그인 실패 기록
     */
    public void loginFailed(String username) {
        String key = LOGIN_ATTEMPT_PREFIX + username;
        
        try {
            Long attempts = redisTemplate.opsForValue().increment(key);
            if (attempts == 1) {
                redisTemplate.expire(key, ATTEMPT_EXPIRE_MINUTES, TimeUnit.MINUTES);
            }
            
            log.warn("로그인 실패: {} (시도 횟수: {})", username, attempts);
            
            if (attempts != null && attempts >= MAX_ATTEMPTS) {
                blockUser(username);
            }
        } catch (Exception e) {
            log.error("로그인 시도 기록 실패: {}", e.getMessage());
        }
    }

    /**
     * 로그인 성공 시 시도 횟수 초기화
     */
    public void loginSucceeded(String username) {
        String key = LOGIN_ATTEMPT_PREFIX + username;
        try {
            redisTemplate.delete(key);
            log.debug("로그인 성공, 시도 횟수 초기화: {}", username);
        } catch (Exception e) {
            log.error("로그인 시도 초기화 실패: {}", e.getMessage());
        }
    }

    /**
     * 사용자 차단
     */
    private void blockUser(String username) {
        String blockKey = BLOCKED_PREFIX + username;
        try {
            redisTemplate.opsForValue().set(blockKey, "blocked", BLOCK_DURATION_MINUTES, TimeUnit.MINUTES);
            log.warn("계정 잠금: {} ({}분간)", username, BLOCK_DURATION_MINUTES);
        } catch (Exception e) {
            log.error("계정 잠금 실패: {}", e.getMessage());
        }
    }

    /**
     * 차단 여부 확인
     */
    public boolean isBlocked(String username) {
        String blockKey = BLOCKED_PREFIX + username;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(blockKey));
        } catch (Exception e) {
            log.error("차단 여부 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 남은 시도 횟수 조회
     */
    public int getRemainingAttempts(String username) {
        String key = LOGIN_ATTEMPT_PREFIX + username;
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return MAX_ATTEMPTS;
            }
            int attempts = Integer.parseInt(value);
            return Math.max(0, MAX_ATTEMPTS - attempts);
        } catch (Exception e) {
            log.error("남은 시도 횟수 조회 실패: {}", e.getMessage());
            return MAX_ATTEMPTS;
        }
    }

    /**
     * 차단 해제 (관리자용)
     */
    public void unblockUser(String username) {
        String blockKey = BLOCKED_PREFIX + username;
        String attemptKey = LOGIN_ATTEMPT_PREFIX + username;
        try {
            redisTemplate.delete(blockKey);
            redisTemplate.delete(attemptKey);
            log.info("계정 잠금 해제: {}", username);
        } catch (Exception e) {
            log.error("계정 잠금 해제 실패: {}", e.getMessage());
        }
    }
}