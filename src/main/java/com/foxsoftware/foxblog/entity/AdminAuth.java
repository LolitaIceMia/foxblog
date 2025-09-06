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

    // OpenSSH 格式公钥 (ssh-ed25519 AAAAC3NzaC1lZDI1NTE5.... 或 ssh-rsa ...)
    @Lob
    @Column(name = "ssh_public_key")
    private String sshPublicKey;

    // 交由数据库生成，实体只读
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private java.time.LocalDateTime createdAt;

    // 可选扩展：启用/停用
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
}