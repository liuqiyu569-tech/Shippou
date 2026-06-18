-- ================================================================
-- Docker 容器首启时自动执行（挂载到 /docker-entrypoint-initdb.d/）
-- 用途：干净环境建库 + 建分片表 + 建全局索引表。无数据迁移、无 RENAME
-- ================================================================

CREATE DATABASE IF NOT EXISTS `short_url`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `short_url`;

-- 全局索引表（不分片）：longUrlHash → shortCode，避免分片表广播查重
CREATE TABLE IF NOT EXISTS `t_long_url_index` (
    `long_url_hash` BIGINT      NOT NULL                COMMENT 'FNV-1a 64bit hash',
    `short_code`    VARCHAR(16) NOT NULL                COMMENT '对应短码',
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`long_url_hash`),
    KEY `idx_short_code` (`short_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='长链hash全局索引（不分片）';

-- 分片表 t_short_url_0..3（HASH_MOD by short_code）
CREATE TABLE IF NOT EXISTS `t_short_url_0` (
    `id`            BIGINT        NOT NULL                COMMENT '雪花算法ID',
    `short_code`    VARCHAR(16)   NOT NULL                COMMENT '短链编码（Base62）',
    `long_url`      VARCHAR(2048) NOT NULL                COMMENT '原始长链接',
    `long_url_hash` BIGINT        NOT NULL                COMMENT '长链hash',
    `expire_time`   DATETIME      DEFAULT NULL            COMMENT '过期时间',
    `access_count`  BIGINT        NOT NULL DEFAULT 0      COMMENT '累计访问次数',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_short_code` (`short_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短链分片0';

CREATE TABLE IF NOT EXISTS `t_short_url_1` LIKE `t_short_url_0`;
CREATE TABLE IF NOT EXISTS `t_short_url_2` LIKE `t_short_url_0`;
CREATE TABLE IF NOT EXISTS `t_short_url_3` LIKE `t_short_url_0`;
