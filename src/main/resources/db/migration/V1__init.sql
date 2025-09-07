-- 创建数据库 (若不存在)
CREATE DATABASE IF NOT EXISTS blog_app
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE blog_app;

-- ========== 1. 管理员认证表 ==========
-- 存储 JWT 签发用的公钥标识、SSH 公钥等
CREATE TABLE admin_auth (
                            id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            username VARCHAR(50) NOT NULL UNIQUE COMMENT '管理员用户名 (仅后台用)',
                            password_hash VARCHAR(255) COMMENT '管理员密码哈希 (BCrypt/Argon2)',
                            ssh_public_key TEXT COMMENT 'SSH 公钥，用于 challenge-response 登录',
                            created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== 2. 媒体表 ==========
-- 使用 UUID (BINARY(16)) 作为主键，便于分布式/去重
CREATE TABLE media (
                       id BINARY(16) NOT NULL PRIMARY KEY COMMENT 'UUID 媒体ID',
                       storage_path VARCHAR(1024) NOT NULL COMMENT '文件存储路径或对象存储key',
                       mime_type VARCHAR(255) COMMENT 'MIME类型',
                       size_bytes BIGINT UNSIGNED COMMENT '大小 (字节)',
                       sha256_hash CHAR(64) COMMENT '去重哈希',
                       created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== 3. 帖子表 ==========
-- 博客/动态的主体内容
CREATE TABLE posts (
                       id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                       content LONGTEXT NOT NULL COMMENT '原始文本内容',
                       content_html MEDIUMTEXT COMMENT '预渲染HTML (可选)',
                       visibility ENUM('PUBLIC','UNLISTED','PRIVATE') NOT NULL DEFAULT 'PUBLIC' COMMENT '公开性',
                       is_pinned TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶',
                       reply_to_post_id BIGINT UNSIGNED COMMENT '引用的帖子ID',
                       created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                       updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                       CONSTRAINT fk_posts_reply_to FOREIGN KEY (reply_to_post_id) REFERENCES posts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== 4. 帖子-媒体关系表 ==========
-- 允许一个帖子绑定多个媒体 (按顺序)
CREATE TABLE post_media (
                            post_id BIGINT UNSIGNED NOT NULL,
                            media_id BINARY(16) NOT NULL,
                            position INT NOT NULL DEFAULT 0 COMMENT '顺序',
                            PRIMARY KEY (post_id, media_id),
                            CONSTRAINT fk_pm_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
                            CONSTRAINT fk_pm_media FOREIGN KEY (media_id) REFERENCES media(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 5. 标签表 ==========
CREATE TABLE tags (
                      id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                      name VARCHAR(100) NOT NULL UNIQUE COMMENT '标签名',
                      created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 6. 帖子-标签关系表 ==========
CREATE TABLE post_tags (
                           post_id BIGINT UNSIGNED NOT NULL,
                           tag_id BIGINT UNSIGNED NOT NULL,
                           PRIMARY KEY (post_id, tag_id),
                           CONSTRAINT fk_pt_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
                           CONSTRAINT fk_pt_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 索引优化 ==========
-- 常见查询优化：按时间排序、媒体去重
CREATE INDEX idx_posts_created ON posts (created_at DESC);
CREATE INDEX idx_media_hash ON media (sha256_hash);
CREATE INDEX idx_media_created ON media (created_at DESC);

ALTER TABLE admin_auth
    ADD COLUMN two_factor_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER created_at,
    ADD COLUMN totp_secret_base32 VARCHAR(128) NULL AFTER two_factor_enabled;
ALTER TABLE admin_auth
    ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1 AFTER created_at;
