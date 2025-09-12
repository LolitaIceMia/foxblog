package com.foxsoftware.foxblog.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "admin_auth",
        indexes = {
                @Index(name = "uk_admin_username", columnList = "username", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 管理员登录名
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    // BCrypt / Argon2 哈希
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    // OpenSSH 公钥 (可选，不影响 TOTP)
    @Lob
    @Column(name = "ssh_public_key", columnDefinition = "TEXT")
    private String sshPublicKey;

    // 创建时间
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private java.time.LocalDateTime createdAt;

    //启用状态
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    // 是否已启用 TOTP 2FA
    @Column(name = "two_factor_enabled", nullable = false)
    private boolean twoFactorEnabled = false;

    // TOTP 密钥（Base32），启用后才写入
    @Column(name = "totp_secret_base32", length = 128)
    private String totpSecretBase32;
}