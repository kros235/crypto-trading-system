package com.cryptotrading.entity;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.List;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "discord_user_id", length = 30)
    private String discordUserId;

    @Column(name = "join_date")
    private LocalDateTime joinDate;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 10)
    private UserRole role = UserRole.USER;

    @Column(name = "api_key_encrypted", columnDefinition = "TEXT")
    private String apiKeyEncrypted;

    @Column(name = "secret_key_encrypted", columnDefinition = "TEXT")
    private String secretKeyEncrypted;

    // IP 화이트리스트 필드
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_ips", columnDefinition = "json")
    private List<String> allowedIps;

    // 2FA 관련 필드
    @Column(name = "totp_secret", length = 100)
    private String totpSecret;

    @Builder.Default
    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled = false;

    @Builder.Default
    @Column(name = "ip_whitelist_enabled")
    private Boolean ipWhitelistEnabled = false;

    @PrePersist
    public void prePersist() {
        if (joinDate == null) {
            joinDate = LocalDateTime.now();
        }

        if (twoFactorEnabled == null) {
            twoFactorEnabled = false;
        }

        if (ipWhitelistEnabled == null) {
            ipWhitelistEnabled = false;
        }
    }
}