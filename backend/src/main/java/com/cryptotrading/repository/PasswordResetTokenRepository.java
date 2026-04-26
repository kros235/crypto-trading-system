package com.cryptotrading.repository;

import com.cryptotrading.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 비밀번호 재설정 토큰 Repository
 * - OTP 인증 코드 저장/조회/만료 처리 (3분 제한)
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * 토큰 문자열로 조회 (사용자가 이메일에서 받은 OTP 코드 검증용)
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * userId + token 조합으로 조회 (보안 강화: 다른 사용자 토큰 사용 방지)
     */
    Optional<PasswordResetToken> findByUserIdAndTokenAndUsedFalse(String userId, String token);

    /**
     * 사용자별 미사용 토큰 모두 무효화 (새 OTP 발급 시 기존 OTP 일괄 만료)
     */
    @Modifying
    @Query("UPDATE PasswordResetToken p SET p.used = true WHERE p.userId = :userId AND p.used = false")
    void invalidateAllByUserId(@Param("userId") String userId);

    /**
     * 만료된 토큰 일괄 삭제 (스케줄러 또는 사용자 삭제 시 호출)
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * 사용자 삭제 시 관련 토큰 일괄 삭제
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
}